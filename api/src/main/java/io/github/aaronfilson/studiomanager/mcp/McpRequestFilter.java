package io.github.aaronfilson.studiomanager.mcp;

import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioAuthorization;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * What stays ours in front of the SDK's transport. A router function can't take
 * {@code @PreAuthorize}, so this checks the caller holds {@code mcp.use} in the studio the URL
 * names. It also checks {@code MCP-Protocol-Version}, which the transport doesn't, and answers
 * anything but POST with a 405 that names POST in {@code Allow}.
 */
@Component
class McpRequestFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

  static final List<String> PROTOCOL_VERSIONS = List.of("2025-11-25", "2025-06-18", "2025-03-26");
  private static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

  private final StudioAuthorization studioAuthz;

  McpRequestFilter(StudioAuthorization studioAuthz) {
    this.studioAuthz = studioAuthz;
  }

  @Override
  public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next)
      throws Exception {
    UUID studioId;
    try {
      studioId = UUID.fromString(request.pathVariable("studioId"));
    } catch (IllegalArgumentException notAnId) {
      return ServerResponse.badRequest().build();
    }
    // The tools act on whatever studio is bound, so make sure it's the one in the URL, and that
    // the caller holds mcp.use there (a token, only with the read scope). The tenant filter
    // already refuses anyone who isn't a member.
    boolean boundHere = studioId.equals(TenantContext.require().studioId());
    if (!boundHere || !studioAuthz.can(studioId, Permission.MCP_USE)) {
      throw new AccessDeniedException("Your role in this studio doesn't include the MCP server.");
    }
    // No server-to-client stream: a stateless server answers each POST and keeps nothing open
    if (!HttpMethod.POST.equals(request.method())) {
      return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
          .header(HttpHeaders.ALLOW, HttpMethod.POST.name())
          .build();
    }
    String protocolVersion = request.headers().firstHeader(PROTOCOL_VERSION_HEADER);
    if (protocolVersion != null && !PROTOCOL_VERSIONS.contains(protocolVersion)) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("jsonrpc", "2.0");
      error.put("id", null);
      error.put(
          "error",
          Map.of(
              "code",
              -32600,
              "message",
              "Unsupported MCP-Protocol-Version. This server speaks "
                  + String.join(", ", PROTOCOL_VERSIONS)));
      return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(error);
    }
    return next.handle(request);
  }
}
