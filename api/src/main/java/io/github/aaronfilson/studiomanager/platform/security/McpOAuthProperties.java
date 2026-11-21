package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional Phase 2 MCP OAuth 2.1 configuration (plan 17 step 5, plan 18 step 3, ADR 26). Blank
 * {@code issuer} leaves MCP OAuth disabled so {@code /.well-known/oauth-protected-resource} answers
 * 404 and only Entra tokens and personal API tokens work.
 *
 * @param issuer the trusted MCP authorization server issuer ({@code iss})
 * @param jwkSetUri JWK Set endpoint for verifying RS256 MCP access tokens; defaults to {@code
 *     <issuer>/oauth2/jwks}
 * @param audience RFC 8707 resource identifier expected in the {@code aud} claim (for example
 *     {@code https://api.studiomanager.example/mcp}); required whenever {@code issuer} is set
 */
@ConfigurationProperties("studiomanager.mcp.oauth")
public record McpOAuthProperties(
    @Nullable String issuer, @Nullable String jwkSetUri, @Nullable String audience) {

  /** The scopes advertised in RFC 9728 Protected Resource Metadata for MCP. */
  public static final List<String> SUPPORTED_SCOPES =
      List.of(TokenScope.READ.wireName(), TokenScope.PROPOSE.wireName());

  public McpOAuthProperties {
    if (issuer != null && !issuer.isBlank()) {
      if (audience == null || audience.isBlank()) {
        throw new IllegalStateException(
            "studiomanager.mcp.oauth.audience must be configured when studiomanager.mcp.oauth.issuer is set.");
      }
      if (!isValidAudienceUri(audience.strip())) {
        throw new IllegalStateException(
            "studiomanager.mcp.oauth.audience must be a clean absolute http or https URI.");
      }
    }
  }

  public boolean enabled() {
    return issuer != null && !issuer.isBlank();
  }

  public String normalizedIssuer() {
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalStateException("studiomanager.mcp.oauth.issuer is not configured.");
    }
    return issuer.strip();
  }

  public String effectiveJwkSetUri() {
    if (jwkSetUri != null && !jwkSetUri.isBlank()) {
      return jwkSetUri.strip();
    }
    return stripTrailingSlashes(normalizedIssuer()) + "/oauth2/jwks";
  }

  public Optional<String> configuredAudience() {
    if (audience == null || audience.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(audience.strip());
  }

  /** Origin ({@code scheme://host[:port]}) of {@code audience} when it is an http(s) URI. */
  public Optional<String> resourceOrigin() {
    return configuredAudience().flatMap(McpOAuthProperties::originOf);
  }

  public String rootResource() {
    return configuredAudience()
        .orElseThrow(
            () -> new IllegalStateException("studiomanager.mcp.oauth.audience is not configured."));
  }

  public String resourceForStudio(UUID studioId) {
    return requiredOrigin() + "/api/v1/studios/" + studioId + "/mcp";
  }

  public String metadataUrlForStudio(UUID studioId) {
    return requiredOrigin()
        + "/.well-known/oauth-protected-resource/api/v1/studios/"
        + studioId
        + "/mcp";
  }

  /**
   * True when the token's {@code aud} claim matches either the configured base MCP resource
   * identifier or the per-studio MCP resource URL for {@code tokenStudioId}. Never accepts an
   * unrelated audience or another studio's MCP URL, and never trusts the request {@code Host}
   * header.
   */
  public boolean acceptsAudience(@Nullable Collection<String> tokenAudiences, UUID tokenStudioId) {
    if (tokenAudiences == null || tokenAudiences.isEmpty()) {
      return false;
    }
    Optional<String> configured = configuredAudience();
    Optional<String> origin = resourceOrigin();
    if (configured.isEmpty() || origin.isEmpty()) {
      return false;
    }
    String studioResource = origin.get() + "/api/v1/studios/" + tokenStudioId + "/mcp";
    return tokenAudiences.contains(studioResource) || tokenAudiences.contains(configured.get());
  }

  private String requiredOrigin() {
    return resourceOrigin()
        .orElseThrow(() ->
            new IllegalStateException(
                "studiomanager.mcp.oauth.audience must be a valid absolute http or https URI."));
  }

  private static boolean isValidAudienceUri(String raw) {
    URI uri;
    try {
      uri = URI.create(raw);
    } catch (IllegalArgumentException invalid) {
      return false;
    }
    if (!uri.isAbsolute()
        || uri.getRawFragment() != null
        || uri.getRawQuery() != null
        || uri.getRawUserInfo() != null) {
      return false;
    }
    return originOf(raw).isPresent();
  }

  private static Optional<String> originOf(String raw) {
    URI uri;
    try {
      uri = URI.create(raw);
    } catch (IllegalArgumentException invalid) {
      return Optional.empty();
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || host.isBlank()) {
      return Optional.empty();
    }
    String lowerScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"https".equals(lowerScheme) && !"http".equals(lowerScheme)) {
      return Optional.empty();
    }
    String lowerHost = host.toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    boolean defaultPort =
        port < 0
            || ("https".equals(lowerScheme) && port == 443)
            || ("http".equals(lowerScheme) && port == 80);
    String origin =
        defaultPort
            ? lowerScheme + "://" + lowerHost
            : lowerScheme + "://" + lowerHost + ":" + port;
    return Optional.of(origin);
  }

  private static String stripTrailingSlashes(String value) {
    String trimmed = value.strip();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
