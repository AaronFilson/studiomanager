package io.github.aaronfilson.studiomanager.auth;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The bridge's settings.
 *
 * @param issuer this bridge's public origin, which is the {@code iss} of every token it signs
 * @param entraClientId the client ID Entra presents at the token endpoint
 * @param entraClientSecret the client secret Entra presents (client_secret_post)
 * @param entraRedirectUris Entra's federation callbacks, matched exactly
 * @param signingKeys PKCS#8 PEM RSA private keys; the first signs, the rest are only published
 * @param githubEmailsUri GitHub's list of the user's email addresses
 * @param authorizeRequestsPerMinute how many sign-ins this replica starts per minute
 * @param mcp optional OAuth 2.1 authorization server settings for Studio Manager's MCP resource
 */
@ConfigurationProperties("studiomanager.bridge")
public record BridgeProperties(
    URI issuer,
    String entraClientId,
    String entraClientSecret,
    List<URI> entraRedirectUris,
    String signingKeys,
    URI githubEmailsUri,
    int authorizeRequestsPerMinute,
    @Nullable McpProperties mcp) {

  public McpProperties mcpSettings() {
    return mcp == null ? McpProperties.disabled() : mcp;
  }

  /**
   * Optional MCP OAuth 2.1 authorization server settings (plan 17 step 5, plan 18 step 3, ADR 26).
   *
   * @param enabled whether MCP OAuth (dynamic client registration, consent, RFC 8707 tokens) is on
   * @param allowedResources allowed RFC 8707 resource URIs or API origins for MCP access tokens
   * @param scopes MCP scopes clients may request (default {@code read}, {@code propose})
   * @param registerRequestsPerMinute global rate limit on {@code POST /oauth2/register} per replica
   * @param registerRequestsPerMinutePerIp per-IP rate limit on {@code POST /oauth2/register} per
   *     replica
   * @param accessTokenTimeToLive lifetime of minted MCP access tokens (default 1 hour)
   * @param preRegisteredClientId optional pre-registered public PKCE MCP client ID
   * @param preRegisteredRedirectUris redirect URIs for the optional pre-registered MCP client
   */
  public record McpProperties(
      boolean enabled,
      @Nullable List<URI> allowedResources,
      @Nullable List<String> scopes,
      int registerRequestsPerMinute,
      int registerRequestsPerMinutePerIp,
      @Nullable Duration accessTokenTimeToLive,
      @Nullable String preRegisteredClientId,
      @Nullable List<URI> preRegisteredRedirectUris) {

    static final List<String> DEFAULT_SCOPES = List.of("read", "propose");
    static final int DEFAULT_REGISTER_REQUESTS_PER_MINUTE = 20;
    static final int DEFAULT_REGISTER_REQUESTS_PER_MINUTE_PER_IP = 5;
    static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofHours(1);

    public McpProperties {
      if (enabled) {
        if (allowedResources == null || allowedResources.isEmpty()) {
          throw new IllegalStateException(
              "studiomanager.bridge.mcp.allowed-resources must be configured when MCP OAuth is enabled.");
        }
        for (URI resource : allowedResources) {
          if (resource == null
              || !resource.isAbsolute()
              || resource.getRawQuery() != null
              || resource.getRawFragment() != null
              || resource.getRawUserInfo() != null
              || resource.getHost() == null
              || resource.getHost().isBlank()
              || (!"https".equalsIgnoreCase(resource.getScheme())
                  && !"http".equalsIgnoreCase(resource.getScheme()))) {
            throw new IllegalStateException(
                "studiomanager.bridge.mcp.allowed-resources entries must be clean absolute http(s) URIs.");
          }
        }
      }
    }

    public List<URI> effectiveAllowedResources() {
      return allowedResources == null ? List.of() : List.copyOf(allowedResources);
    }

    public List<String> effectiveScopes() {
      if (scopes == null || scopes.isEmpty()) {
        return DEFAULT_SCOPES;
      }
      List<String> cleaned =
          scopes.stream().map(String::strip).filter(value -> !value.isEmpty()).toList();
      return cleaned.isEmpty() ? DEFAULT_SCOPES : cleaned;
    }

    public int effectiveRegisterRequestsPerMinute() {
      return registerRequestsPerMinute > 0
          ? registerRequestsPerMinute
          : DEFAULT_REGISTER_REQUESTS_PER_MINUTE;
    }

    public int effectiveRegisterRequestsPerMinutePerIp() {
      int perIp =
          registerRequestsPerMinutePerIp > 0
              ? registerRequestsPerMinutePerIp
              : DEFAULT_REGISTER_REQUESTS_PER_MINUTE_PER_IP;
      return Math.min(perIp, effectiveRegisterRequestsPerMinute());
    }

    public Duration effectiveAccessTokenTimeToLive() {
      return accessTokenTimeToLive == null ? DEFAULT_ACCESS_TOKEN_TTL : accessTokenTimeToLive;
    }

    public List<URI> effectivePreRegisteredRedirectUris() {
      return preRegisteredRedirectUris == null ? List.of() : List.copyOf(preRegisteredRedirectUris);
    }

    static McpProperties disabled() {
      return new McpProperties(
          false,
          List.of(),
          DEFAULT_SCOPES,
          DEFAULT_REGISTER_REQUESTS_PER_MINUTE,
          DEFAULT_REGISTER_REQUESTS_PER_MINUTE_PER_IP,
          DEFAULT_ACCESS_TOKEN_TTL,
          null,
          List.of());
    }
  }
}
