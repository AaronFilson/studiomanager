package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Live Glazecalc integration: share-link imports ({@code GET /api/shared/recipe}) and per-member
 * PKCE OAuth connection + sync ({@code /oauth/authorize}, {@code /oauth/token}, {@code
 * GET /api/export}).
 */
@Service
public class GlazecalcService {

  private static final Duration STATE_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);
  private static final Pattern SAFE_SECRET = Pattern.compile("^[A-Za-z0-9._~+/=-]{4,256}$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final GlazecalcProperties properties;
  private final GlazecalcHttpClient http;
  private final SecretCipher cipher;
  private final GlazecalcConnectionRepository connections;
  private final GlazeImportService importer;
  private final EntityManager entityManager;
  private final JsonMapper json;
  private final Clock clock;

  GlazecalcService(
      GlazecalcProperties properties,
      GlazecalcHttpClient http,
      GlazecalcConfiguration.GlazecalcTokenCipher tokenCipher,
      GlazecalcConnectionRepository connections,
      GlazeImportService importer,
      EntityManager entityManager,
      JsonMapper json,
      Clock clock) {
    this.properties = properties;
    this.http = http;
    this.cipher = tokenCipher.cipher();
    this.connections = connections;
    this.importer = importer;
    this.entityManager = entityManager;
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

  /**
   * Fetches a recipe from Glazecalc using a share URL ({@code https://glazecalcapp.com/shared#...})
   * or raw share secret, then previews or imports it as a member recipe.
   */
  @Transactional
  public GlazeImportService.Result importShare(String shareUrlOrSecret, boolean dryRun) {
    String secret = extractShareSecret(shareUrlOrSecret);
    URI endpoint = URI.create(properties.resolvedBaseUrl() + "/api/shared/recipe");
    GlazecalcHttpClient.Response response;
    try {
      response =
          http.get(
              endpoint,
              Map.of("Accept", "application/json", "X-Share-Secret", secret));
    } catch (GlazecalcHttpClient.UnsafeTargetException e) {
      throw new InvalidInputException(
          e.getMessage() == null ? "Refused Glazecalc address." : e.getMessage());
    } catch (IOException e) {
      throw new InvalidInputException(
          "Couldn't reach Glazecalc to load that share link: " + e.getMessage());
    }
    if (response.statusCode() == 404 || response.statusCode() == 401 || response.statusCode() == 403) {
      throw new InvalidInputException("Glazecalc didn't recognize that share link or it has expired.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new InvalidInputException(
          "Glazecalc returned HTTP %d when loading that share link.".formatted(response.statusCode()));
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
        .orElseGet(
            () ->
                new Status(
                    properties.oauthConfigured(),
                    false,
                    properties.resolvedBaseUrl(),
                    null,
                    null,
                    null,
                    null));
  }

  public ConnectStart startConnect(@Nullable String requestedRedirectUri) {
    TenantContext caller = TenantContext.require();
    if (!properties.oauthConfigured()) {
      throw new InvalidInputException(
          "Glazecalc OAuth is not configured on this server. Upload an export JSON or paste a share link instead.");
    }
    String redirectUri = resolveRedirectUri(requestedRedirectUri);
    String codeVerifier = randomUrlSafe(32);
    String codeChallenge = pkceS256(codeVerifier);
    OAuthState payload =
        new OAuthState(
            caller.requireStudioId(),
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
    return new ConnectStart(URI.create(properties.resolvedBaseUrl() + "/oauth/authorize?" + query), state);
  }

  @Transactional
  public Status completeCallback(
      String code, String state, @Nullable String requestedRedirectUri) {
    TenantContext caller = TenantContext.require();
    if (!properties.oauthConfigured()) {
      throw new InvalidInputException("Glazecalc OAuth is not configured on this server.");
    }
    if (code == null || code.isBlank()) {
      throw new InvalidInputException("Authorization code is required.");
    }
    OAuthState parsedState = decryptAndCheckState(caller, state);
    String redirectUri =
        requestedRedirectUri != null && !requestedRedirectUri.isBlank()
            ? requestedRedirectUri.strip()
            : parsedState.redirectUri();

    Map<String, String> form = new LinkedHashMap<>();
    form.put("grant_type", "authorization_code");
    form.put("code", code.strip());
    form.put("redirect_uri", redirectUri);
    form.put("client_id", properties.clientId() == null ? "" : properties.clientId().strip());
    form.put("code_verifier", parsedState.codeVerifier());
    if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
      form.put("client_secret", properties.clientSecret().strip());
    }

    TokenResponse tokens = exchangeToken(form);
    Instant now = clock.instant();
    Instant expiresAt =
        tokens.expiresInSeconds() == null ? null : now.plusSeconds(tokens.expiresInSeconds());

    GlazecalcConnection connection = connections.findByAccountId(caller.accountId()).orElse(null);
    if (connection == null) {
      UUID id = Uuids.v7();
      connection =
          new GlazecalcConnection(
              id,
              caller.accountId(),
              cipher.encrypt(id, tokens.accessToken()),
              tokens.refreshToken() == null ? null : cipher.encrypt(id, tokens.refreshToken()),
              expiresAt,
              now,
              tokens.accountLabel());
      entityManager.persist(connection);
    } else {
      connection.updateTokens(
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
  }

  @Transactional
  public GlazeImportService.Result sync(boolean dryRun) {
    TenantContext caller = TenantContext.require();
    GlazecalcConnection connection =
        connections
            .findByAccountId(caller.accountId())
            .orElseThrow(
                () -> new NotFoundException("Connect your Glazecalc account before syncing."));

    String accessToken = resolveFreshAccessToken(connection);
    URI endpoint = URI.create(properties.resolvedBaseUrl() + "/api/export");
    GlazecalcHttpClient.Response response;
    try {
      response =
          http.get(
              endpoint,
              Map.of("Accept", "application/json", "Authorization", "Bearer " + accessToken));
    } catch (GlazecalcHttpClient.UnsafeTargetException e) {
      throw new InvalidInputException(
          e.getMessage() == null ? "Refused Glazecalc address." : e.getMessage());
    } catch (IOException e) {
      throw new InvalidInputException("Couldn't reach Glazecalc to sync recipes: " + e.getMessage());
    }

    if (response.statusCode() == 401) {
      throw new InvalidInputException(
          "Your Glazecalc session expired. Reconnect your Glazecalc account and try again.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new InvalidInputException(
          "Glazecalc returned HTTP %d when exporting your recipes.".formatted(response.statusCode()));
    }

    GlazeImportService.Result result = importer.importJson(response.body(), dryRun);
    if (!dryRun && result.committed()) {
      connection.markSynced(clock.instant(), result.remoteAccountLabel());
      connections.flush();
    }
    return result;
  }

  @Transactional
  public void disconnect() {
    TenantContext caller = TenantContext.require();
    connections.findByAccountId(caller.accountId()).ifPresent(connections::delete);
  }

  private String resolveFreshAccessToken(GlazecalcConnection connection) {
    Instant now = clock.instant();
    Instant expiresAt = connection.expiresAt();
    byte @Nullable [] refreshEnc = connection.refreshTokenEnc();
    if (expiresAt != null
        && refreshEnc != null
        && !expiresAt.isAfter(now.plus(REFRESH_BUFFER))
        && properties.oauthConfigured()) {
      String refreshToken = cipher.decrypt(connection.id(), refreshEnc);
      Map<String, String> form = new LinkedHashMap<>();
      form.put("grant_type", "refresh_token");
      form.put("refresh_token", refreshToken);
      form.put("client_id", properties.clientId() == null ? "" : properties.clientId().strip());
      if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
        form.put("client_secret", properties.clientSecret().strip());
      }
      TokenResponse refreshed = exchangeToken(form);
      Instant nextExpires =
          refreshed.expiresInSeconds() == null
              ? null
              : now.plusSeconds(refreshed.expiresInSeconds());
      connection.updateTokens(
          cipher.encrypt(connection.id(), refreshed.accessToken()),
          refreshed.refreshToken() == null
              ? null
              : cipher.encrypt(connection.id(), refreshed.refreshToken()),
          nextExpires,
          now,
          refreshed.accountLabel());
      connections.flush();
      return refreshed.accessToken();
    }
    return cipher.decrypt(connection.id(), connection.accessTokenEnc());
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
      throw new InvalidInputException(
          e.getMessage() == null ? "Refused Glazecalc address." : e.getMessage());
    } catch (IOException e) {
      throw new InvalidInputException(
          "Couldn't contact Glazecalc to complete sign-in: " + e.getMessage());
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

  private OAuthState decryptAndCheckState(TenantContext caller, String encodedState) {
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
      return state;
    } catch (IllegalArgumentException | IllegalStateException | JacksonException e) {
      throw new InvalidInputException("OAuth state is invalid or expired. Start again.");
    }
  }

  /**
   * Validates a Glazecalc share URL against the allowlisted host or accepts a raw share secret.
   */
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

  private String resolveRedirectUri(@Nullable String requested) {
    if (requested != null && !requested.isBlank()) {
      return requested.strip();
    }
    if (properties.redirectUri() != null && !properties.redirectUri().isBlank()) {
      return properties.redirectUri().strip();
    }
    throw new InvalidInputException(
        "No OAuth redirect URI configured for Glazecalc (set studiomanager.glazecalc.redirect-uri).");
  }

  private Status toStatus(GlazecalcConnection connection) {
    return new Status(
        properties.oauthConfigured(),
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
