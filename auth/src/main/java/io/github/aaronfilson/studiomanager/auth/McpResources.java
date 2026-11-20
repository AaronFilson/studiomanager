package io.github.aaronfilson.studiomanager.auth;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * RFC 8707 resource indicators, studio ID extraction, and OAuth 2.1 redirect URI validation for MCP
 * clients (ADR 26).
 */
final class McpResources {

  static final String RESOURCE_PARAMETER = "resource";
  static final String STUDIO_ID_PARAMETER = "studio_id";
  static final String INVALID_TARGET = "invalid_target";

  private static final Pattern STUDIO_MCP_PATH =
      Pattern.compile("^/api/v1/studios/([0-9a-fA-F-]{36})/mcp$");

  private McpResources() {}

  /** A validated RFC 8707 MCP resource URI and the studio it targets. */
  record Target(URI resource, UUID studioId) {}

  /**
   * Validates the RFC 8707 {@code resource} parameter and resolves the {@code studio_id} it targets
   * (either embedded in {@code /api/v1/studios/{studioId}/mcp} or passed as {@code studio_id}).
   */
  static Target validateAndResolve(
      @Nullable String rawResource, @Nullable String rawStudioId, List<URI> allowedResources) {
    if (rawResource == null || rawResource.isBlank()) {
      throw invalidTarget("The resource parameter is required for MCP authorization.");
    }
    URI resource = parseResourceUri(rawResource.strip());
    if (!allowedResources.isEmpty() && !isAllowedResource(resource, allowedResources)) {
      throw invalidTarget("The requested resource is not allowed by this authorization server.");
    }
    Optional<UUID> fromPath = extractStudioId(resource);
    Optional<UUID> fromParam = parseStudioId(rawStudioId);
    if (fromPath.isPresent() && fromParam.isPresent() && !fromPath.get().equals(fromParam.get())) {
      throw invalidTarget("The studio_id parameter doesn't match the studio in the resource URL.");
    }
    UUID studioId =
        fromPath
            .or(() -> fromParam)
            .orElseThrow(
                () ->
                    invalidTarget(
                        "A studio_id parameter or per-studio MCP resource URL is required."));
    return new Target(resource, studioId);
  }

  /** Extracts the studio UUID from {@code /api/v1/studios/{studioId}/mcp}, if present. */
  static Optional<UUID> extractStudioId(URI resource) {
    String path = resource.getPath();
    if (path == null) {
      return Optional.empty();
    }
    Matcher matcher = STUDIO_MCP_PATH.matcher(path);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(matcher.group(1)));
    } catch (IllegalArgumentException notAUuid) {
      return Optional.empty();
    }
  }

  /**
   * Validates a redirect URI for a dynamically registered public PKCE MCP client: {@code https}
   * with a host, or {@code http} only on loopback ({@code localhost}, {@code 127.0.0.1}, {@code
   * [::1]}). Fragments and userinfo are always rejected (OAuth 2.1 / RFC 8252).
   */
  static boolean isValidRedirectUri(@Nullable String raw) {
    if (raw == null || raw.isBlank() || raw.length() > 2048) {
      return false;
    }
    URI uri;
    try {
      uri = URI.create(raw);
    } catch (IllegalArgumentException invalid) {
      return false;
    }
    if (!uri.isAbsolute() || uri.getRawFragment() != null || uri.getRawUserInfo() != null) {
      return false;
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || host.isBlank()) {
      return false;
    }
    String lowerScheme = scheme.toLowerCase(Locale.ROOT);
    if ("https".equals(lowerScheme)) {
      return true;
    }
    return "http".equals(lowerScheme) && isLoopbackHost(host);
  }

  private static boolean isLoopbackHost(String host) {
    String lower = host.toLowerCase(Locale.ROOT);
    return "localhost".equals(lower)
        || "127.0.0.1".equals(lower)
        || "[::1]".equals(lower)
        || "::1".equals(lower);
  }

  private static URI parseResourceUri(String raw) {
    URI uri;
    try {
      uri = URI.create(raw);
    } catch (IllegalArgumentException invalid) {
      throw invalidTarget("The resource parameter must be an absolute URI.");
    }
    if (!uri.isAbsolute()
        || uri.getRawFragment() != null
        || uri.getRawQuery() != null
        || uri.getRawUserInfo() != null) {
      throw invalidTarget("The resource parameter must be a clean absolute URI without query or fragment.");
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null || host.isBlank()) {
      throw invalidTarget("The resource parameter must be an http or https URI with a host.");
    }
    String lowerScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"https".equals(lowerScheme) && !"http".equals(lowerScheme)) {
      throw invalidTarget("The resource parameter must use http or https.");
    }
    return uri;
  }

  private static boolean isAllowedResource(URI candidate, List<URI> allowedResources) {
    String normalizedCandidate = normalizeWithoutTrailingSlash(candidate);
    String candidateOrigin = originOf(candidate);
    boolean isStudioMcpPath = extractStudioId(candidate).isPresent();
    for (URI allowed : allowedResources) {
      if (normalizedCandidate.equals(normalizeWithoutTrailingSlash(allowed))) {
        return true;
      }
      if (isStudioMcpPath && candidateOrigin.equals(originOf(allowed))) {
        return true;
      }
    }
    return false;
  }

  private static Optional<UUID> parseStudioId(@Nullable String rawStudioId) {
    if (rawStudioId == null || rawStudioId.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(rawStudioId.strip()));
    } catch (IllegalArgumentException notAUuid) {
      throw invalidTarget("The studio_id parameter must be a valid UUID.");
    }
  }

  private static String originOf(URI uri) {
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    boolean defaultPort =
        port < 0 || ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
    return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
  }

  private static String normalizeWithoutTrailingSlash(URI uri) {
    String path = uri.getPath() == null ? "" : uri.getPath();
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return originOf(uri) + path;
  }

  static OAuth2AuthenticationException invalidTarget(String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(INVALID_TARGET, description, null));
  }
}
