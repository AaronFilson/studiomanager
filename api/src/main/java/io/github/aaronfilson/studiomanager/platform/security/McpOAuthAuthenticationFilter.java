package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenAuthenticationFilter;
import io.github.aaronfilson.studiomanager.platform.tenancy.McpOAuthAuthentication;
import io.github.aaronfilson.studiomanager.platform.tenancy.McpOAuthAuthentication.ResolvedMcpToken;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authenticates OAuth 2.1 access tokens issued by the MCP authorization server (plan 17 step 5,
 * plan 18 step 3, ADR 26).
 *
 * <p>Tokens from this issuer are accepted <em>only</em> on {@code /api/v1/studios/{studioId}/mcp}.
 * On any other route they are refused with 401 before reaching Entra's JWT decoder. Regular Entra
 * JWTs and {@code smpat_} personal API tokens bypass this filter untouched.
 */
final class McpOAuthAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";
  static final PathPattern MCP_ENDPOINT =
      PathPatternParser.defaultInstance.parse("/api/v1/studios/{studioId}/mcp");

  private final McpOAuthProperties properties;
  private final @Nullable JwtDecoder mcpDecoder;
  private final AuthenticationEntryPoint entryPoint;
  private final JsonMapper json;

  McpOAuthAuthenticationFilter(
      McpOAuthProperties properties, AuthenticationEntryPoint entryPoint, JsonMapper json) {
    this.properties = properties;
    this.mcpDecoder = properties.enabled() ? buildDecoder(properties) : null;
    this.entryPoint = entryPoint;
    this.json = json;
  }

  /**
   * True when MCP OAuth is enabled and the request carries a JWT whose unverified {@code iss} claim
   * names the configured MCP authorization server. Used strictly to route verification to this
   * filter and skip Entra's bearer resolver; the signature and issuer are always verified
   * cryptographically before trusting any claim.
   */
  boolean isMcpOAuthToken(HttpServletRequest request) {
    if (!properties.enabled() || ApiTokenAuthenticationFilter.isApiToken(request)) {
      return false;
    }
    String token = bearerToken(request);
    if (token == null) {
      return false;
    }
    String issuer = peekUnverifiedIssuer(token);
    return issuer != null && properties.normalizedIssuer().equals(issuer);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!isMcpOAuthToken(request)) {
      chain.doFilter(request, response);
      return;
    }
    if (mcpStudioId(request).isEmpty()) {
      refuse(request, response, "MCP OAuth tokens are accepted only on the MCP endpoint.");
      return;
    }
    String bearer = bearerToken(request);
    if (bearer == null || mcpDecoder == null) {
      refuse(request, response, "A valid MCP access token is required.");
      return;
    }
    Jwt jwt;
    try {
      jwt = mcpDecoder.decode(bearer);
    } catch (JwtException invalid) {
      refuse(request, response, "This MCP access token isn't valid.");
      return;
    }

    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      refuse(request, response, "This MCP access token has no subject.");
      return;
    }
    UUID tokenStudioId = parseStudioId(jwt.getClaimAsString("studio_id"));
    if (tokenStudioId == null) {
      refuse(request, response, "This MCP access token has no valid studio_id.");
      return;
    }
    if (!properties.acceptsAudience(jwt.getAudience(), tokenStudioId)) {
      refuse(request, response, "This MCP access token wasn't issued for this MCP resource.");
      return;
    }
    Set<TokenScope> scopes = parseScopes(jwt);
    String issuer = jwt.getClaimAsString("iss");
    if (issuer == null || !properties.normalizedIssuer().equals(issuer)) {
      refuse(request, response, "This MCP access token has an unexpected issuer.");
      return;
    }
    ResolvedMcpToken resolved =
        new ResolvedMcpToken(
            issuer, subject, tokenStudioId, scopes, jwt.getClaimAsString("client_id"));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new McpOAuthAuthentication(resolved));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void refuse(HttpServletRequest request, HttpServletResponse response, String reason)
      throws IOException, ServletException {
    SecurityContextHolder.clearContext();
    entryPoint.commence(request, response, new InvalidBearerTokenException(reason));
  }

  /** Studio UUID from {@code /api/v1/studios/{studioId}/mcp}, or empty for any other path. */
  static Optional<UUID> mcpStudioId(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    PathPattern.PathMatchInfo match = MCP_ENDPOINT.matchAndExtract(PathContainer.parsePath(path));
    if (match == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(parseStudioId(match.getUriVariables().get("studioId")));
  }

  private static JwtDecoder buildDecoder(McpOAuthProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(properties.effectiveJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.normalizedIssuer()))));
    return decoder;
  }

  private static @Nullable String bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER)) {
      return null;
    }
    String token = header.substring(BEARER.length()).strip();
    return token.isEmpty() ? null : token;
  }

  private @Nullable String peekUnverifiedIssuer(String token) {
    int firstDot = token.indexOf('.');
    if (firstDot <= 0) {
      return null;
    }
    int secondDot = token.indexOf('.', firstDot + 1);
    if (secondDot <= firstDot + 1 || token.indexOf('.', secondDot + 1) >= 0) {
      return null;
    }
    String payload = token.substring(firstDot + 1, secondDot);
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(payload);
      JsonNode claims = json.readTree(decoded);
      JsonNode iss = claims.path("iss");
      return iss.isString() ? iss.asString() : null;
    } catch (IllegalArgumentException | JacksonException malformed) {
      return null;
    }
  }

  private static @Nullable UUID parseStudioId(@Nullable String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(raw.strip());
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  private static Set<TokenScope> parseScopes(Jwt jwt) {
    EnumSet<TokenScope> parsed = EnumSet.noneOf(TokenScope.class);
    Object raw = jwt.getClaims().get("scope");
    if (raw == null) {
      raw = jwt.getClaims().get("scp");
    }
    if (raw instanceof String scopeText) {
      for (String part : scopeText.strip().split("\\s+", -1)) {
        if (!part.isEmpty()) {
          TokenScope.fromWireName(part).ifPresent(parsed::add);
        }
      }
    } else if (raw instanceof Collection<?> scopeList) {
      for (Object item : scopeList) {
        if (item instanceof String part) {
          TokenScope.fromWireName(part).ifPresent(parsed::add);
        }
      }
    }
    return Collections.unmodifiableSet(parsed);
  }
}
