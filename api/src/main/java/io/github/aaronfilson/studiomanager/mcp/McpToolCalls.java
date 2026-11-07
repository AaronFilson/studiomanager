package io.github.aaronfilson.studiomanager.mcp;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.ApiTokenAuthentication;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;
import io.github.aaronfilson.studiomanager.ratelimit.ApiCallLog;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimiter;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns {@link McpTools} into the SDK's tool specifications. Every call is rate limited (plan 20,
 * section 6) and logged, and a tool's own failure goes back to the model as an {@code isError}
 * result it can read and act on.
 *
 * <p>Tools run on the request thread ({@code immediateExecution}, see {@link McpConfiguration}), so
 * the tenant and the signed-in person are bound here exactly as in a controller.
 */
@Component
class McpToolCalls {

  private final McpTools tools;
  private final RateLimiter limiter;
  private final ApiCallLog callLog;
  private final JsonMapper json;
  private final Clock clock;

  McpToolCalls(
      McpTools tools, RateLimiter limiter, ApiCallLog callLog, JsonMapper json, Clock clock) {
    this.tools = tools;
    this.limiter = limiter;
    this.callLog = callLog;
    this.json = json;
    this.clock = clock;
  }

  List<SyncToolSpecification> specifications() {
    return tools.list().stream().map(this::specification).toList();
  }

  private SyncToolSpecification specification(McpTools.Tool tool) {
    McpSchema.Tool definition =
        McpSchema.Tool.builder(tool.name(), tool.inputSchema())
            .title(tool.title())
            .description(tool.description())
            .annotations(annotations(tool))
            .build();
    return SyncToolSpecification.builder()
        .tool(definition)
        .callHandler(new LimitedToolCall(tool))
        .build();
  }

  /**
   * Hints for the client. No tool is destructive or reaches outside the studio. A read tool changes
   * nothing, so calling it twice is harmless; a propose tool records a suggestion each time.
   */
  private static McpSchema.ToolAnnotations annotations(McpTools.Tool tool) {
    McpSchema.ToolAnnotations.Builder hints =
        McpSchema.ToolAnnotations.builder()
            .title(tool.title())
            .destructiveHint(false)
            .openWorldHint(false);
    return switch (tool.access()) {
      case READ -> hints.readOnlyHint(true).idempotentHint(true).build();
      case PROPOSE -> hints.readOnlyHint(false).idempotentHint(false).build();
    };
  }

  /**
   * Why the caller can't run this tool, or empty if they can. The caller must hold the tool's
   * permission. A token's context holds only what its person holds and its scopes allow, so a
   * propose tool also needs the {@code propose} scope; that case gets its own message.
   */
  private static Optional<String> refusal(McpTools.Tool tool, TenantContext caller) {
    if (caller.holds(tool.permission())) {
      return Optional.empty();
    }
    return switch (tool.access()) {
      case READ -> Optional.of(roleRefusal(tool));
      case PROPOSE -> {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof ApiTokenAuthentication tokenAuthentication
            && !tokenAuthentication.token().scopes().contains(TokenScope.PROPOSE)) {
          yield Optional.of(
              "This token can't propose changes. Ask the person to make a token with the propose scope.");
        }
        yield Optional.of(roleRefusal(tool));
      }
    };
  }

  private static String roleRefusal(McpTools.Tool tool) {
    return "This person can't use %s here: it needs the %s permission."
        .formatted(tool.name(), tool.permission().key());
  }

  /** One tool's handler: the rate limit, the permission, the call and the call log. */
  private final class LimitedToolCall
      implements BiFunction<McpTransportContext, CallToolRequest, CallToolResult> {

    private final McpTools.Tool tool;

    LimitedToolCall(McpTools.Tool tool) {
      this.tool = tool;
    }

    @Override
    public CallToolResult apply(McpTransportContext transportContext, CallToolRequest request) {
      TenantContext caller = TenantContext.require();
      UUID studioId = caller.studioId();
      if (studioId == null) {
        throw new AccessDeniedException("MCP tools run inside a studio.");
      }
      Map<String, Object> arguments = request.arguments() == null ? Map.of() : request.arguments();
      String argumentsJson = json.writeValueAsString(arguments);
      Instant started = clock.instant();
      Optional<Duration> refusal = limiter.refusal(Channel.MCP, caller.accountId(), studioId);
      if (refusal.isPresent()) {
        long seconds = Math.max(1, (refusal.get().toMillis() + 999) / 1000);
        log(caller, studioId, argumentsJson, ApiCallLog.Outcome.LIMITED, started);
        return failure(
            "Too many tool calls for now. Wait %d seconds before calling another tool, and tell the person you're waiting."
                .formatted(seconds));
      }
      Optional<String> permissionRefusal = refusal(tool, caller);
      if (permissionRefusal.isPresent()) {
        log(caller, studioId, argumentsJson, ApiCallLog.Outcome.ERROR, started);
        return failure(permissionRefusal.get());
      }
      try {
        JsonNode argumentsNode = json.valueToTree(arguments);
        Object data = tool.call().apply(argumentsNode);
        // Structured content for clients that read it, the same as text for those that don't
        Map<String, Object> structured = Map.of("result", data);
        CallToolResult result =
            CallToolResult.builder()
                .addTextContent(json.writeValueAsString(structured))
                .structuredContent(structured)
                .isError(false)
                .build();
        log(caller, studioId, argumentsJson, ApiCallLog.Outcome.OK, started);
        return result;
      } catch (InvalidInputException
          | NotFoundException
          | ConflictException
          | AccessDeniedException toolFailure) {
        log(caller, studioId, argumentsJson, ApiCallLog.Outcome.ERROR, started);
        return failure(
            java.util.Objects.requireNonNullElse(
                toolFailure.getMessage(), "The tool couldn't do that."));
      } catch (RuntimeException unexpected) {
        log(caller, studioId, argumentsJson, ApiCallLog.Outcome.ERROR, started);
        throw unexpected;
      }
    }

    private void log(
        TenantContext caller,
        UUID studioId,
        String argumentsJson,
        ApiCallLog.Outcome outcome,
        Instant started) {
      callLog.record(
          new ApiCallLog.Call(
              studioId,
              caller.accountId(),
              Channel.MCP,
              tool.name(),
              argumentsJson,
              outcome,
              null,
              Duration.between(started, clock.instant())));
    }
  }

  private static CallToolResult failure(String message) {
    return CallToolResult.builder().addTextContent(message).isError(true).build();
  }
}
