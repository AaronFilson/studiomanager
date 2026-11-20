package io.github.aaronfilson.studiomanager.mcp;

import io.github.aaronfilson.studiomanager.platform.security.McpOAuthProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * RFC 9728 OAuth 2.0 Protected Resource Metadata endpoints for MCP OAuth discovery (plan 17 step 5,
 * plan 18 step 3, ADR 26). Returns 404 when {@code studiomanager.mcp.oauth.issuer} is not
 * configured.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
class McpOAuthMetadataConfiguration {

  private static final String WELL_KNOWN_ROOT = "/.well-known/oauth-protected-resource";
  private static final String WELL_KNOWN_STUDIO_MCP =
      "/.well-known/oauth-protected-resource/api/v1/studios/{studioId}/mcp";

  @Bean
  RouterFunction<ServerResponse> mcpOAuthMetadataRoutes(McpOAuthProperties mcpOAuth) {
    return RouterFunctions.route()
        .GET(WELL_KNOWN_ROOT, request -> rootMetadata(request, mcpOAuth))
        .GET(WELL_KNOWN_STUDIO_MCP, request -> studioMetadata(request, mcpOAuth))
        .build();
  }

  private static ServerResponse rootMetadata(ServerRequest request, McpOAuthProperties mcpOAuth) {
    if (!mcpOAuth.enabled()) {
      return ServerResponse.notFound().build();
    }
    String origin = McpOAuthProperties.requestOrigin(request.servletRequest());
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(metadata(mcpOAuth.rootResource(origin), mcpOAuth));
  }

  private static ServerResponse studioMetadata(ServerRequest request, McpOAuthProperties mcpOAuth) {
    if (!mcpOAuth.enabled()) {
      return ServerResponse.notFound().build();
    }
    UUID studioId;
    try {
      studioId = UUID.fromString(request.pathVariable("studioId"));
    } catch (IllegalArgumentException notAUuid) {
      return ServerResponse.badRequest().build();
    }
    String origin = McpOAuthProperties.requestOrigin(request.servletRequest());
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(metadata(mcpOAuth.resourceForStudio(studioId, origin), mcpOAuth));
  }

  private static Map<String, Object> metadata(String resource, McpOAuthProperties mcpOAuth) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("resource", resource);
    body.put("authorization_servers", List.of(mcpOAuth.normalizedIssuer()));
    body.put("scopes_supported", McpOAuthProperties.SUPPORTED_SCOPES);
    body.put("bearer_methods_supported", List.of("header"));
    return body;
  }
}
