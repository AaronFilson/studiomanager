package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.ProblemType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers 401 and 403 with RFC 9457 problem details, like every other API error. The bearer token
 * handlers it wraps set the status and the RFC 6750 {@code WWW-Authenticate} header.
 */
final class ProblemJsonSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  /**
   * Matches any {@code resource_metadata="..."} parameter added by Spring Security 7's {@link
   * BearerTokenAuthenticationEntryPoint}, so we control RFC 9728 advertising explicitly (ADR 26).
   */
  private static final Pattern RESOURCE_METADATA_PARAM =
      Pattern.compile("(?:,\\s*)?resource_metadata=\"[^\"]*\"");

  private final AuthenticationEntryPoint bearerEntryPoint =
      new BearerTokenAuthenticationEntryPoint();
  private final AccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();
  private final JsonMapper json;
  private final McpOAuthProperties mcpOAuth;

  ProblemJsonSecurityHandler(JsonMapper json, McpOAuthProperties mcpOAuth) {
    this.json = json;
    this.mcpOAuth = mcpOAuth;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException, ServletException {
    bearerEntryPoint.commence(request, response, ex);
    addMcpResourceMetadataHeader(request, response);
    write(request, response, ProblemType.UNAUTHENTICATED, "A valid access token is required.");
  }

  /**
   * Per RFC 9728 §5.1 and the MCP Authorization spec, unauthenticated or rejected requests to an
   * MCP endpoint advertise {@code resource_metadata} in {@code WWW-Authenticate} when MCP OAuth is
   * enabled, and never advertise a disabled or wrong-origin metadata URL.
   */
  private void addMcpResourceMetadataHeader(
      HttpServletRequest request, HttpServletResponse response) {
    String cleaned = stripResourceMetadata(response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    if (cleaned != null) {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, cleaned);
    }
    if (!mcpOAuth.enabled()) {
      return;
    }
    McpOAuthAuthenticationFilter.mcpStudioId(request)
        .ifPresent(studioId -> {
          String metadataUrl =
              mcpOAuth.metadataUrlForStudio(studioId, McpOAuthProperties.requestOrigin(request));
          String existing = response.getHeader(HttpHeaders.WWW_AUTHENTICATE);
          String header;
          if (existing == null || existing.isBlank() || "Bearer".equals(existing.strip())) {
            header = "Bearer resource_metadata=\"%s\"".formatted(metadataUrl);
          } else {
            header = "%s, resource_metadata=\"%s\"".formatted(existing, metadataUrl);
          }
          response.setHeader(HttpHeaders.WWW_AUTHENTICATE, header);
        });
  }

  private static @Nullable String stripResourceMetadata(@Nullable String header) {
    if (header == null) {
      return null;
    }
    String without = RESOURCE_METADATA_PARAM.matcher(header).replaceAll("").strip();
    if (without.startsWith("Bearer ,")) {
      without = "Bearer %s".formatted(without.substring("Bearer ,".length()).strip());
    } else if (without.startsWith("Bearer,")) {
      without = "Bearer %s".formatted(without.substring("Bearer,".length()).strip());
    }
    return without.isEmpty() ? "Bearer" : without;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException, ServletException {
    bearerAccessDenied.handle(request, response, ex);
    // Ours always carry a reason; Spring's own may not
    write(
        request,
        response,
        ProblemType.FORBIDDEN,
        Objects.requireNonNullElse(ex.getMessage(), "You don't have access to this."));
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      ProblemType problemType,
      String detail)
      throws IOException {
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", problemType.uri().toString());
    problem.put("title", problemType.title());
    problem.put("status", status.value());
    problem.put("detail", detail);
    problem.put("instance", request.getRequestURI());
    problem.put(ProblemType.CODE_PROPERTY, problemType.code());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    json.writeValue(response.getOutputStream(), problem);
  }
}
