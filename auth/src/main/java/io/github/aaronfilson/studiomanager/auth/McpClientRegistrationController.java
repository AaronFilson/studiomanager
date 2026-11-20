package io.github.aaronfilson.studiomanager.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RFC 7591 Dynamic Client Registration for MCP OAuth 2.1 public PKCE clients ({@code POST
 * /oauth2/register}). Only active when {@code studiomanager.bridge.mcp.enabled} is true; returns
 * 404 when disabled. Rate-limited by {@link RegisterRateLimit}.
 */
@RestController
class McpClientRegistrationController {

  private static final int MAX_REDIRECT_URIS = 5;
  private static final int MAX_CLIENT_NAME_LENGTH = 120;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final BridgeProperties properties;
  private final RegisteredClientRepository clients;
  private final Clock clock;

  McpClientRegistrationController(
      BridgeProperties properties, RegisteredClientRepository clients, Clock clock) {
    this.properties = properties;
    this.clients = clients;
    this.clock = clock;
  }

  record RegistrationRequest(
      @JsonProperty("client_name") @Nullable String clientName,
      @JsonProperty("redirect_uris") @Nullable List<String> redirectUris,
      @JsonProperty("token_endpoint_auth_method") @Nullable String tokenEndpointAuthMethod,
      @JsonProperty("grant_types") @Nullable List<String> grantTypes,
      @JsonProperty("response_types") @Nullable List<String> responseTypes,
      @JsonProperty("scope") @Nullable String scope) {}

  @PostMapping(
      path = RegisterRateLimit.REGISTER_PATH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Map<String, Object>> register(@RequestBody RegistrationRequest request) {
    BridgeProperties.McpProperties mcp = properties.mcpSettings();
    if (!mcp.enabled()) {
      return ResponseEntity.notFound().build();
    }
    List<String> redirectUris = request.redirectUris();
    if (redirectUris == null || redirectUris.isEmpty() || redirectUris.size() > MAX_REDIRECT_URIS) {
      return error(
          "invalid_redirect_uri",
          "Provide between 1 and " + MAX_REDIRECT_URIS + " valid redirect_uris.");
    }
    List<String> cleanedRedirects =
        redirectUris.stream().map(uri -> uri == null ? "" : uri.strip()).distinct().toList();
    for (String uri : cleanedRedirects) {
      if (!McpResources.isValidRedirectUri(uri)) {
        return error(
            "invalid_redirect_uri",
            "Redirect URIs must be https or loopback http (localhost, 127.0.0.1, [::1]) with no fragment.");
      }
    }
    String authMethod = request.tokenEndpointAuthMethod();
    if (authMethod != null && !authMethod.isBlank() && !"none".equals(authMethod)) {
      return error(
          "invalid_client_metadata",
          "MCP dynamic clients must be public PKCE clients (token_endpoint_auth_method: none).");
    }
    List<String> grantTypes = request.grantTypes();
    if (grantTypes != null
        && !grantTypes.isEmpty()
        && (!grantTypes.contains("authorization_code")
            || grantTypes.stream().anyMatch(type -> !"authorization_code".equals(type)))) {
      return error(
          "invalid_client_metadata", "Only the authorization_code grant_type is supported.");
    }
    List<String> responseTypes = request.responseTypes();
    if (responseTypes != null
        && !responseTypes.isEmpty()
        && (!responseTypes.contains("code")
            || responseTypes.stream().anyMatch(type -> !"code".equals(type)))) {
      return error("invalid_client_metadata", "Only the code response_type is supported.");
    }
    Set<String> allowedScopes = Set.copyOf(mcp.effectiveScopes());
    Set<String> grantedScopes = parseScopes(request.scope(), allowedScopes);
    if (grantedScopes == null) {
      return error(
          "invalid_client_metadata",
          "Requested scopes must be within: " + String.join(" ", mcp.effectiveScopes()));
    }
    String clientName = sanitizeName(request.clientName());
    Instant issuedAt = clock.instant();
    String clientId = newClientId();
    RegisteredClient.Builder builder =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientIdIssuedAt(issuedAt)
            .clientName(clientName)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)
                    .requireAuthorizationConsent(true)
                    .build())
            .tokenSettings(
                TokenSettings.builder()
                    .authorizationCodeTimeToLive(BridgeSecurity.AUTHORIZATION_CODE_LIFETIME)
                    .accessTokenTimeToLive(mcp.effectiveAccessTokenTimeToLive())
                    .build());
    for (String redirect : cleanedRedirects) {
      builder.redirectUri(redirect);
    }
    for (String scope : grantedScopes) {
      builder.scope(scope);
    }
    clients.save(builder.build());

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("client_id", clientId);
    response.put("client_name", clientName);
    response.put("client_id_issued_at", issuedAt.getEpochSecond());
    response.put("redirect_uris", cleanedRedirects);
    response.put("grant_types", List.of("authorization_code"));
    response.put("response_types", List.of("code"));
    response.put("token_endpoint_auth_method", "none");
    response.put("scope", String.join(" ", grantedScopes));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private static @Nullable Set<String> parseScopes(
      @Nullable String rawScope, Set<String> allowedScopes) {
    if (rawScope == null || rawScope.isBlank()) {
      return new LinkedHashSet<>(allowedScopes);
    }
    Set<String> requested = new LinkedHashSet<>();
    for (String part : Arrays.asList(rawScope.strip().split("\\s+", -1))) {
      if (part.isEmpty()) {
        continue;
      }
      if (!allowedScopes.contains(part)) {
        return null;
      }
      requested.add(part);
    }
    return requested.isEmpty() ? null : requested;
  }

  private static String sanitizeName(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return "MCP Client";
    }
    String clean = raw.replaceAll("[\\p{Cntrl}]", "").strip();
    if (clean.isEmpty()) {
      return "MCP Client";
    }
    return clean.length() <= MAX_CLIENT_NAME_LENGTH
        ? clean
        : clean.substring(0, MAX_CLIENT_NAME_LENGTH);
  }

  private static String newClientId() {
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    return "mcp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static ResponseEntity<Map<String, Object>> error(String code, String description) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", code);
    body.put("error_description", description);
    return ResponseEntity.badRequest().body(body);
  }
}
