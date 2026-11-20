package io.github.aaronfilson.studiomanager.platform.security;

import io.github.aaronfilson.studiomanager.platform.ProblemType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
   * enabled.
   */
  private void addMcpResourceMetadataHeader(
      HttpServletRequest request, HttpServletResponse response) {
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
            header = "Bearer resource_metadata=\"" + metadataUrl + "\"";
          } else {
            header = existing + ", resource_metadata=\"" + metadataUrl + "\"";
          }
          response.setHeader(HttpHeaders.WWW_AUTHENTICATE, header);
        });
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
