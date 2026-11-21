package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.crypto.SecretCipher;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Live Glazecalc integration: share-link imports ({@code GET /api/shared/recipe}) and per-member
 * PKCE OAuth connection + sync ({@code /oauth/authorize}, {@code /oauth/token}, {@code GET
 * /api/export}). Outbound HTTP calls always run outside database transactions.
 */
@Service
public class GlazecalcService {

  private static final Logger log = LoggerFactory.getLogger(GlazecalcService.class);
  private static final Duration STATE_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);
  private static final int MAX_CONSUMED_STATES = 10_000;
  private static final Pattern SAFE_SECRET = Pattern.compile("^[A-Za-z0-9._~+/=-]{4,256}$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final GlazecalcProperties properties;
  private final GlazecalcHttpClient http;
  private final SecretCipher cipher;
  private final boolean tokenKeyConfigured;
  private final Set<String> allowedRedirectOrigins;
  private final GlazecalcConnectionRepository connections;
  private final GlazeImportService importer;
  private final EntityManager entityManager;
  private final TransactionOperations transactions;
  private final JsonMapper json;
  private final Clock clock;

  private final Object consumedStatesLock = new Object();
  private final Map<String, Instant> consumedStates = new LinkedHashMap<>();

  GlazecalcService(
      GlazecalcProperties properties,
      GlazecalcHttpClient http,
      GlazecalcConfiguration.GlazecalcTokenCipher tokenCipher,
      GlazecalcConnectionRepository connections,
      GlazeImportService importer,
      EntityManager entityManager,
      TransactionOperations transactions,
      JsonMapper json,
      Clock clock,
      @Value("${studiomanager.mail.web-origin:}") String webOrigin,
      @Value("${studiomanager.cors.allowed-origins:}") List<String> corsAllowedOrigins) {
    this.properties = properties;
    this.http = http;
    this.cipher = tokenCipher.cipher();
    this.tokenKeyConfigured = tokenCipher.keyConfigured();
    this.allowedRedirectOrigins =
        collectAllowedOrigins(properties.redirectUri(), webOrigin, corsAllowedOrigins);
    this.connections = connections;
    this.importer = importer;
    this.entityManager = entityManager;
    this.transactions = transactions;
    this.json = json;
    this.clock = clock;
  }

  public record Status(
      boolean configured,
      boolean connected,
      String baseUrl,
      @Nullable Instant connectedAt,
      @Nullable Instant lastSyncedAt,
      @Nullable Instant expiresAt,
      @Nullable String remoteAccountLabel) {}

  public record ConnectStart(URI authorizationUrl, String state) {}

  record OAuthState(
      UUID studioId, UUID accountId, String codeVerifier, String redirectUri, Instant expiresAt) {}

  private record ParsedState(OAuthState state, String digest) {}

  private static final class ConnectionSnapshot {
    private final UUID connectionId;
    private final byte[] accessTokenEnc;
    private final byte @Nullable [] refreshTokenEnc;
    private final @Nullable Instant expiresAt;

    private ConnectionSnapshot(
        UUID connectionId,
        byte[] accessTokenEnc,
        byte @Nullable [] refreshTokenEnc,
        @Nullable Instant expiresAt) {
      this.connectionId = connectionId;
      this.accessTokenEnc = accessTokenEnc;
      this.refreshTokenEnc = refreshTokenEnc;
      this.expiresAt = expiresAt;
    }

    UUID connectionId() {
      return connectionId;
    }

    byte[] accessTokenEnc() {
      return accessTokenEnc;
    }

    byte @Nullable [] refreshTokenEnc() {
      return refreshTokenEnc;
    }

    @Nullable Instant expiresAt() {
      return expiresAt;
    }
  }

  /**
   * Fetches a recipe from Glazecalc using a share URL ({@code https://glazecalcapp.com/shared#...})
   * or raw share secret, then previews or imports it as a member recipe.
   */
  public GlazeImportService.Result importShare(String shareUrlOrSecret, boolean dryRun) {
    String secret = extractShareSecret(shareUrlOrSecret);
    URI endpoint = URI.create(properties.resolvedBaseUrl() + "/api/shared/recipe");
    GlazecalcHttpClient.Response response;
    try {
      response = http.get(endpoint, Map.of("Accept", "application/json", "X-Share-Secret", secret));
    } catch (GlazecalcHttpClient.UnsafeTargetException e) {
      log.warn("Refused Glazecalc share target: {}", e.getMessage());
      throw new InvalidInputException("Couldn't reach Glazecalc at the configured address.");
    } catch (IOException e) {
      log.warn("Couldn't reach Glazecalc to load share link", e);
      throw new InvalidInputException(
          "Couldn't reach Glazecalc to load that share link. Try again shortly.");
    }
    if (response.statusCode() == 404
        || response.statusCode() == 401
        || response.statusCode() == 403) {
      throw new InvalidInputException(
          "Glazecalc didn't recognize that share link or it has expired.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new InvalidInputException(
          "Glazecalc returned HTTP %d when loading that share link."
              .formatted(response.statusCode()));
    }

    JsonNode body;
    try {
      body = json.readTree(response.body());
    } catch (JacksonException e) {
      throw new InvalidInputException("Glazecalc returned an unreadable share response.");
    }
    if (body == null || !body.isObject()) {
      throw new InvalidInputException("Glazecalc returned an empty share response.");
    }

    // Already a full glazecalc-export envelope? Pass straight through.
    if (GlazeImportParser.FORMAT_GLAZECALC.equals(body.path("format").asString(""))) {
      return importer.importNode(body, dryRun);
    }

    JsonNode recipeNode =
        body.has("recipe") && body.get("recipe").isObject() ? body.get("recipe") : body;
    ObjectNode mutableRecipe = (ObjectNode) recipeNode.deepCopy();
    if (!mutableRecipe.has("_id") || mutableRecipe.path("_id").asString("").isBlank()) {
      mutableRecipe.put("_id", "share:" + shortHash(secret));
    }
    ArrayNode recipesArray = json.createArrayNode();
    recipesArray.add(mutableRecipe);
    ObjectNode recordsNode = json.createObjectNode();
    recordsNode.set("recipes", recipesArray);
    ObjectNode envelope = json.createObjectNode();
    envelope.put("format", GlazeImportParser.FORMAT_GLAZECALC);
    envelope.put("version", 1);
    envelope.set("records", recordsNode);
    return importer.importNode(envelope, dryRun);
  }

  @Transactional(readOnly = true)
  public Status status() {
    TenantContext caller = TenantContext.require();
    return connections
        .findByAccountId(caller.accountId())
        .map(this::toStatus)
        .orElseGet(() ->
            new Status(oauthReady(), false, properties.resolvedBaseUrl(), null, null, null, null));
  }

  public ConnectStart startConnect(@Nullable String requestedRedirectUri) {
    TenantContext caller = TenantContext.require();
    requireOAuthConfigured(
        "Glazecalc OAuth is not configured on this server. Upload an export JSON or paste a share link instead.");
    UUID studioId = caller.requireStudioId();
    String redirectUri = resolveRedirectUri(studioId, requestedRedirectUri);
    String codeVerifier = randomUrlSafe(32);
    String codeChallenge = pkceS256(codeVerifier);
    OAuthState payload =
        new OAuthState(
            studioId,
            caller.accountId(),
            codeVerifier,
            redirectUri,
            clock.instant().plus(STATE_TTL));
    String state =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(cipher.encrypt(caller.accountId(), json.writeValueAsString(payload)));

    String clientId = properties.clientId() == null ? "" : properties.clientId().strip();
    String query =
        "response_type=code"
            + "&client_id="
            + urlEncode(clientId)
            + "&redirect_uri="
            + urlEncode(redirectUri)
            + "&scope="
            + urlEncode("recipes:read")
            + "&state="
            + urlEncode(state)
            + "&code_challenge="
            + urlEncode(codeChallenge)
            + "&code_challenge_method=S256";
    return new ConnectStart(
        URI.create(properties.resolvedBaseUrl() + "/oauth/authorize?" + query), state);
  }

  public Status completeCallback(String code, String state, @Nullable String requestedRedirectUri) {
    TenantContext caller = TenantContext.require();
    requireOAuthConfigured("Glazecalc OAuth is not configured on this server.");
    if (code == null || code.isBlank()) {
      throw new InvalidInputException("Authorization code is required.");
    }
    ParsedState parsed = decryptAndCheckState(caller, state);
    OAuthState parsedState = parsed.state();
    if (requestedRedirectUri != null
        && !requestedRedirectUri.isBlank()
        && !requestedRedirectUri.strip().equals(parsedState.redirectUri())) {
      throw new InvalidInputException("OAuth redirect URI doesn't match the sign-in request.");
    }
    consumeState(parsed.digest(), parsedState.expiresAt());

    Map<String, String> form = new LinkedHashMap<>();
    form.put("grant_type", "authorization_code");
    form.put("code", code.strip());
    form.put("redirect_uri", parsedState.redirectUri());
    form.put("client_id", properties.clientId() == null ? "" : properties.clientId().strip());
    form.put("code_verifier", parsedState.codeVerifier());
    if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
      form.put("client_secret", properties.clientSecret().strip());
    }

    TokenResponse tokens = exchangeToken(form);
    Instant now = clock.instant();
    Instant expiresAt =
        tokens.expiresInSeconds() == null ? null : now.plusSeconds(tokens.expiresInSeconds());
    return persistReconnectedConnection(caller.accountId(), tokens, expiresAt, now);
  }

  public GlazeImportService.Result sync(boolean dryRun) {
    TenantContext caller = TenantContext.require();
    ConnectionSnapshot snapshot =
        transactions.execute(tx ->
            connections
                .findByAccountId(caller.accountId())
                .map(c ->
                    new ConnectionSnapshot(
                        c.id(), c.accessTokenEnc(), c.refreshTokenEnc(), c.expiresAt()))
                .orElse(null));
    if (snapshot == null) {
      throw new NotFoundException("Connect your Glazecalc account before syncing.");
    }

    String accessToken = resolveFreshAccessToken(caller.accountId(), snapshot);
    URI endpoint = URI.create(properties.resolvedBaseUrl() + "/api/export");
    GlazecalcHttpClient.Response response;
    try {
      response =
          http.get(
              endpoint,
              Map.of("Accept", "application/json", "Authorization", "Bearer " + accessToken));
    } catch (GlazecalcHttpClient.UnsafeTargetException e) {
      log.warn("Refused Glazecalc export target: {}", e.getMessage());
      throw new InvalidInputException("Couldn't reach Glazecalc at the configured address.");
    } catch (IOException e) {
      log.warn("Couldn't reach Glazecalc to sync recipes", e);
      throw new InvalidInputException(
          "Couldn't reach Glazecalc to sync recipes. Try again shortly.");
    }

    if (response.statusCode() == 401) {
      throw new InvalidInputException(
          "Your Glazecalc session expired. Reconnect your Glazecalc account and try again.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new InvalidInputException(
          "Glazecalc returned HTTP %d when exporting your recipes."
              .formatted(response.statusCode()));
    }

    GlazeImportService.Result result = importer.importJson(response.body(), dryRun);
    if (!dryRun && result.committed()) {
      Instant syncedAt = clock.instant();
      transactions.executeWithoutResult(tx ->
          connections
              .findByAccountId(caller.accountId())
              .ifPresent(conn -> {
                conn.markSynced(syncedAt, result.remoteAccountLabel());
                connections.flush();
              }));
    }
    return result;
  }

  @Transactional
  public void disconnect() {
    TenantContext caller = TenantContext.require();
    connections.findByAccountId(caller.accountId()).ifPresent(connections::delete);
  }

  private boolean oauthReady() {
    return properties.oauthConfigured() && tokenKeyConfigured;
  }

  private void requireOAuthConfigured(String message) {
    if (!oauthReady()) {
      throw new InvalidInputException(message);
    }
  }

  private Status persistReconnectedConnection(
      UUID accountId, TokenResponse tokens, @Nullable Instant expiresAt, Instant now) {
    try {
      return saveReconnectedConnection(accountId, tokens, expiresAt, now);
    } catch (DataIntegrityViolationException ex) {
      if (!UniqueViolations.isUniqueViolation(ex)) {
        throw ex;
      }
      // Concurrent first-time connect won the insert; reload and update in a fresh transaction
      try {
        return saveReconnectedConnection(accountId, tokens, expiresAt, now);
      } catch (DataIntegrityViolationException retryEx) {
        if (UniqueViolations.isUniqueViolation(retryEx)) {
          throw ConflictException.duplicate(
              "Your Glazecalc connection was updated at the same time. Try again.");
        }
        throw retryEx;
      }
    }
  }

  private Status saveReconnectedConnection(
      UUID accountId, TokenResponse tokens, @Nullable Instant expiresAt, Instant now) {
    Status saved =
        transactions.execute(tx -> {
          GlazecalcConnection connection = connections.findByAccountId(accountId).orElse(null);
          if (connection == null) {
            UUID id = Uuids.v7();
            connection =
                new GlazecalcConnection(
                    id,
                    accountId,
                    cipher.encrypt(id, tokens.accessToken()),
                    tokens.refreshToken() == null
                        ? null
                        : cipher.encrypt(id, tokens.refreshToken()),
                    expiresAt,
                    now,
                    tokens.accountLabel());
            entityManager.persist(connection);
          } else {
            connection.reconnect(
                cipher.encrypt(connection.id(), tokens.accessToken()),
                tokens.refreshToken() == null
                    ? null
                    : cipher.encrypt(connection.id(), tokens.refreshToken()),
                expiresAt,
                now,
                tokens.accountLabel());
          }
          entityManager.flush();
          return toStatus(connection);
        });
    return Objects.requireNonNull(saved, "Glazecalc connection transaction returned null");
  }

  private String resolveFreshAccessToken(UUID accountId, ConnectionSnapshot snapshot) {
    Instant now = clock.instant();
    Instant expiresAt = snapshot.expiresAt();
    byte @Nullable [] refreshEnc = snapshot.refreshTokenEnc();
    if (expiresAt != null
        && refreshEnc != null
        && !expiresAt.isAfter(now.plus(REFRESH_BUFFER))
        && oauthReady()) {
      String refreshToken = cipher.decrypt(snapshot.connectionId(), refreshEnc);
      Map<String, String> form = new LinkedHashMap<>();
      form.put("grant_type", "refresh_token");
      form.put("refresh_token", refreshToken);
      form.put("client_id", properties.clientId() == null ? "" : properties.clientId().strip());
      if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
        form.put("client_secret", properties.clientSecret().strip());
      }
      TokenResponse refreshed = exchangeToken(form);
      Instant refreshedAt = clock.instant();
      Instant nextExpires =
          refreshed.expiresInSeconds() == null
              ? null
              : refreshedAt.plusSeconds(refreshed.expiresInSeconds());
      // Commit rotated tokens immediately so a later /api/export failure never rolls them back
      transactions.executeWithoutResult(tx ->
          connections
              .findByAccountId(accountId)
              .ifPresent(conn -> {
                conn.refreshTokens(
                    cipher.encrypt(conn.id(), refreshed.accessToken()),
                    refreshed.refreshToken() == null
                        ? null
                        : cipher.encrypt(conn.id(), refreshed.refreshToken()),
                    nextExpires,
                    refreshed.accountLabel());
                connections.flush();
              }));
      return refreshed.accessToken();
    }
    return cipher.decrypt(snapshot.connectionId(), snapshot.accessTokenEnc());
  }

  private record TokenResponse(
      String accessToken,
      @Nullable String refreshToken,
      @Nullable Long expiresInSeconds,
      @Nullable String accountLabel) {}

  private TokenResponse exchangeToken(Map<String, String> form) {
    URI tokenEndpoint = URI.create(properties.resolvedBaseUrl() + "/oauth/token");
    GlazecalcHttpClient.Response response;
    try {
      response = http.postForm(tokenEndpoint, form);
    } catch (GlazecalcHttpClient.UnsafeTargetException e) {
      log.warn("Refused Glazecalc token target: {}", e.getMessage());
      throw new InvalidInputException("Couldn't reach Glazecalc at the configured address.");
    } catch (IOException e) {
      log.warn("Couldn't contact Glazecalc to complete sign-in", e);
      throw new InvalidInputException(
          "Couldn't contact Glazecalc to complete sign-in. Try again shortly.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new InvalidInputException(
          "Glazecalc rejected the OAuth token exchange (HTTP %d)."
              .formatted(response.statusCode()));
    }
    JsonNode body;
    try {
      body = json.readTree(response.body());
    } catch (JacksonException e) {
      throw new InvalidInputException("Glazecalc returned an unreadable token response.");
    }
    String accessToken = body.path("access_token").asString("").strip();
    if (accessToken.isEmpty()) {
      throw new InvalidInputException("Glazecalc did not return an access_token.");
    }
    String refreshToken = body.path("refresh_token").asString("").strip();
    Long expiresIn =
        body.has("expires_in") && body.get("expires_in").isNumber()
            ? body.get("expires_in").longValue()
            : null;
    String label = extractLabel(body);
    return new TokenResponse(
        accessToken, refreshToken.isEmpty() ? null : refreshToken, expiresIn, label);
  }

  private static @Nullable String extractLabel(JsonNode body) {
    for (String field : List.of("account_label", "email", "username")) {
      String value = body.path(field).asString("").strip();
      if (!value.isEmpty()) {
        return value.length() > 200 ? value.substring(0, 200) : value;
      }
    }
    JsonNode user = body.get("user");
    if (user != null && user.isObject()) {
      String email = user.path("email").asString("").strip();
      if (!email.isEmpty()) {
        return email.length() > 200 ? email.substring(0, 200) : email;
      }
    }
    return null;
  }

  private ParsedState decryptAndCheckState(TenantContext caller, String encodedState) {
    if (encodedState == null || encodedState.isBlank()) {
      throw new InvalidInputException("OAuth state is missing.");
    }
    try {
      byte[] sealed = Base64.getUrlDecoder().decode(encodedState.strip());
      String raw = cipher.decrypt(caller.accountId(), sealed);
      OAuthState state = json.readValue(raw, OAuthState.class);
      if (!caller.requireStudioId().equals(state.studioId())
          || !caller.accountId().equals(state.accountId())) {
        throw new InvalidInputException("OAuth state doesn't match this studio or member.");
      }
      if (state.expiresAt().isBefore(clock.instant())) {
        throw new InvalidInputException("That Glazecalc sign-in expired. Start again.");
      }
      return new ParsedState(state, sha256Hex(sealed));
    } catch (IllegalArgumentException | IllegalStateException | JacksonException e) {
      throw new InvalidInputException("OAuth state is invalid or expired. Start again.");
    }
  }

  private void consumeState(String stateDigest, Instant expiresAt) {
    Instant now = clock.instant();
    synchronized (consumedStatesLock) {
      consumedStates.values().removeIf(exp -> exp.isBefore(now));
      if (consumedStates.containsKey(stateDigest)) {
        throw new InvalidInputException(
            "That Glazecalc sign-in has already been used. Start again.");
      }
      while (consumedStates.size() >= MAX_CONSUMED_STATES) {
        var iterator = consumedStates.entrySet().iterator();
        if (iterator.hasNext()) {
          iterator.next();
          iterator.remove();
        } else {
          break;
        }
      }
      consumedStates.put(stateDigest, expiresAt);
    }
  }

  /** Validates a Glazecalc share URL against the allowlisted host or accepts a raw share secret. */
  String extractShareSecret(String rawInput) {
    if (rawInput == null || rawInput.isBlank()) {
      throw new InvalidInputException("Paste a Glazecalc share link or secret.");
    }
    String trimmed = rawInput.strip();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
      URI uri;
      try {
        uri = new URI(trimmed);
      } catch (URISyntaxException e) {
        throw new InvalidInputException("That isn't a valid share URL.");
      }
      URI allowedBase = URI.create(properties.resolvedBaseUrl());
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      String allowedHost =
          allowedBase.getHost() == null ? "" : allowedBase.getHost().toLowerCase(Locale.ROOT);
      boolean hostMatches =
          host.equals(allowedHost)
              || (allowedHost.equals("glazecalcapp.com") && host.equals("www.glazecalcapp.com"));
      if (!hostMatches) {
        throw new InvalidInputException(
            "Only share links from %s are allowed.".formatted(allowedHost));
      }
      if (!"https".equalsIgnoreCase(uri.getScheme())
          && !"http".equalsIgnoreCase(allowedBase.getScheme())) {
        throw new InvalidInputException("Glazecalc share links must use https://.");
      }
      String secret = uri.getFragment();
      if (secret == null || secret.isBlank()) {
        secret = queryParam(uri.getRawQuery(), "secret");
      }
      if ((secret == null || secret.isBlank())
          && uri.getPath() != null
          && uri.getPath().startsWith("/shared/")) {
        secret = uri.getPath().substring("/shared/".length());
      }
      if (secret == null || !SAFE_SECRET.matcher(secret.strip()).matches()) {
        throw new InvalidInputException(
            "Couldn't find a valid share secret in that URL (expected /shared#<secret>).");
      }
      return secret.strip();
    }
    if (!SAFE_SECRET.matcher(trimmed).matches()) {
      throw new InvalidInputException("That doesn't look like a valid Glazecalc share secret.");
    }
    return trimmed;
  }

  private String resolveRedirectUri(UUID studioId, @Nullable String requested) {
    String configured =
        properties.redirectUri() == null || properties.redirectUri().isBlank()
            ? null
            : properties.redirectUri().strip();
    if (requested == null || requested.isBlank()) {
      if (configured != null) {
        return configured;
      }
      throw new InvalidInputException(
          "No OAuth redirect URI configured for Glazecalc (set studiomanager.glazecalc.redirect-uri).");
    }
    String candidate = requested.strip();
    if (configured != null && candidate.equals(configured)) {
      return configured;
    }
    URI uri;
    try {
      uri = new URI(candidate);
    } catch (URISyntaxException e) {
      throw new InvalidInputException("Invalid Glazecalc OAuth redirect URI.");
    }
    if (!uri.isAbsolute()
        || uri.getRawUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new InvalidInputException("Invalid Glazecalc OAuth redirect URI.");
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    if (host.isBlank()) {
      throw new InvalidInputException("Invalid Glazecalc OAuth redirect URI.");
    }
    boolean schemeAllowed =
        "https".equals(scheme)
            || (properties.allowPrivateNetworks()
                && "http".equals(scheme)
                && ("localhost".equals(host) || "127.0.0.1".equals(host)));
    if (!schemeAllowed) {
      throw new InvalidInputException("Glazecalc OAuth redirect URI must use https://.");
    }
    String candidateOrigin = normalizeOrigin(uri);
    if (candidateOrigin == null || !allowedRedirectOrigins.contains(candidateOrigin)) {
      throw new InvalidInputException("Glazecalc OAuth redirect URI origin is not allowed.");
    }
    String expectedPath = "/studios/" + studioId + "/glazes";
    if (!expectedPath.equals(uri.getRawPath())) {
      throw new InvalidInputException(
          "Glazecalc OAuth redirect URI must point to this studio's glazes page.");
    }
    return candidate;
  }

  private static Set<String> collectAllowedOrigins(
      @Nullable String configuredRedirectUri, String webOrigin, List<String> corsAllowedOrigins) {
    Set<String> origins = new LinkedHashSet<>();
    addNormalizedOrigin(origins, configuredRedirectUri);
    addNormalizedOrigin(origins, webOrigin);
    for (String origin : corsAllowedOrigins) {
      addNormalizedOrigin(origins, origin);
    }
    return Set.copyOf(origins);
  }

  private static void addNormalizedOrigin(Set<String> target, @Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return;
    }
    try {
      String normalized = normalizeOrigin(URI.create(raw.strip()));
      if (normalized != null) {
        target.add(normalized);
      }
    } catch (IllegalArgumentException ignored) {
      // Ignore malformed origin entries
    }
  }

  private static @Nullable String normalizeOrigin(URI uri) {
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || host.isBlank()) {
      return null;
    }
    String lowerScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"https".equals(lowerScheme) && !"http".equals(lowerScheme)) {
      return null;
    }
    String lowerHost = host.toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    boolean defaultPort =
        port < 0
            || ("https".equals(lowerScheme) && port == 443)
            || ("http".equals(lowerScheme) && port == 80);
    return defaultPort
        ? lowerScheme + "://" + lowerHost
        : lowerScheme + "://" + lowerHost + ":" + port;
  }

  private Status toStatus(GlazecalcConnection connection) {
    return new Status(
        oauthReady(),
        true,
        properties.resolvedBaseUrl(),
        connection.connectedAt(),
        connection.lastSyncedAt(),
        connection.expiresAt(),
        connection.remoteAccountLabel());
  }

  private static @Nullable String queryParam(@Nullable String rawQuery, String name) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return null;
    }
    for (String pair : rawQuery.split("&", -1)) {
      int eq = pair.indexOf('=');
      if (eq > 0 && pair.substring(0, eq).equals(name)) {
        return pair.substring(eq + 1);
      }
    }
    return null;
  }

  private static String randomUrlSafe(int bytes) {
    byte[] buf = new byte[bytes];
    RANDOM.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private static String pkceS256(String verifier) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String sha256Hex(byte[] data) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
      return HexFormat.of().formatHex(digest);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String shortHash(String secret) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 12);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
