package io.github.aaronfilson.studiomanager.mcp;

import io.github.aaronfilson.studiomanager.firing.FeeService;
import io.github.aaronfilson.studiomanager.glaze.GlazeBook;
import io.github.aaronfilson.studiomanager.kiln.KilnService;
import io.github.aaronfilson.studiomanager.kiln.KilnView;
import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.ratelimit.ApiCallLog;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimiter;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.studio.StudioView;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * MCP Resources exposed alongside tools (docs/planning/17-mcp-and-api-tokens-plan.md, Part B):
 *
 * <ul>
 *   <li>{@code studio://policies} — studio settings, piece pickup hold period, firing-fee schedule,
 *       and kiln queue limits.
 *   <li>{@code studio://glazes/{id}} — one glaze recipe (studio glaze, caller's own, or shared).
 * </ul>
 *
 * <p>Every resource read runs on the calling servlet thread ({@code immediateExecution}), checks
 * the same studio permissions as the REST API, enforces the MCP rate limit, and logs to {@link
 * ApiCallLog}.
 */
@Component
class McpResources {

  static final String POLICIES_URI = "studio://policies";
  static final String GLAZE_PREFIX = "studio://glazes/";
  static final String GLAZE_TEMPLATE = "studio://glazes/{id}";
  private static final String JSON_MIME = "application/json";

  private final StudioService studios;
  private final PieceService pieces;
  private final FeeService fees;
  private final KilnService kilns;
  private final GlazeBook glazes;
  private final RateLimiter limiter;
  private final ApiCallLog callLog;
  private final JsonMapper json;
  private final Clock clock;

  McpResources(
      StudioService studios,
      PieceService pieces,
      FeeService fees,
      KilnService kilns,
      GlazeBook glazes,
      RateLimiter limiter,
      ApiCallLog callLog,
      JsonMapper json,
      Clock clock) {
    this.studios = studios;
    this.pieces = pieces;
    this.fees = fees;
    this.kilns = kilns;
    this.glazes = glazes;
    this.limiter = limiter;
    this.callLog = callLog;
    this.json = json;
    this.clock = clock;
  }

  List<SyncResourceSpecification> staticResources() {
    McpSchema.Resource policies =
        McpSchema.Resource.builder(POLICIES_URI, "studio_policies")
            .title("Studio policies")
            .description(
                "Studio settings, pickup hold days, current firing-fee schedule, and kiln queue caps.")
            .mimeType(JSON_MIME)
            .build();
    return List.of(
        new SyncResourceSpecification(
            policies,
            (ctx, request) ->
                read(
                    POLICIES_URI,
                    "resource:studio://policies",
                    Permission.STUDIO_READ,
                    this::policiesPayload)));
  }

  List<SyncResourceTemplateSpecification> resourceTemplates() {
    McpSchema.ResourceTemplate glazeTemplate =
        McpSchema.ResourceTemplate.builder(GLAZE_TEMPLATE, "studio_glaze")
            .title("Studio glaze recipe")
            .description(
                "One glaze recipe (ingredients, cone, atmosphere, target SG, notes) by id.")
            .mimeType(JSON_MIME)
            .build();
    return List.of(
        new SyncResourceTemplateSpecification(
            glazeTemplate,
            (ctx, request) -> {
              String uri = request.uri();
              UUID glazeId = parseGlazeId(uri);
              return read(
                  uri,
                  "resource:studio://glazes/{id}",
                  Permission.GLAZES_READ,
                  () -> glazePayload(glazeId));
            }));
  }

  private Map<String, Object> policiesPayload() {
    TenantContext caller = TenantContext.require();
    StudioView studio = studios.current();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("studioId", studio.id());
    payload.put("name", studio.name());
    payload.put("unitSystem", studio.unitSystem());
    payload.put("timeZone", studio.timeZone().getId());
    payload.put("country", studio.country());
    payload.put("currency", studio.currency().getCurrencyCode());
    payload.put("pickupHoldDays", pieces.holdDays());
    if (caller.holds(Permission.FEES_READ)) {
      payload.put("feeSchedule", fees.current());
    }
    if (caller.holds(Permission.KILNS_READ)) {
      payload.put("kilns", kilns.list().stream().map(McpResources::kilnPolicySummary).toList());
    }
    return payload;
  }

  private static Map<String, Object> kilnPolicySummary(KilnView kiln) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("id", kiln.id());
    summary.put("name", kiln.name());
    summary.put("kind", kiln.kind());
    summary.put("available", kiln.available());
    summary.put("maxPiecesPerMember", kiln.maxPiecesPerMember());
    return summary;
  }

  private GlazeBook.Recipe glazePayload(UUID glazeId) {
    return glazes.get(glazeId);
  }

  private ReadResourceResult read(
      String uri, String logTarget, Permission permission, Supplier<Object> loader) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.studioId();
    if (studioId == null) {
      throw new AccessDeniedException("MCP resources run inside a studio.");
    }
    Instant started = clock.instant();
    Optional<Duration> refusal = limiter.refusal(Channel.MCP, caller.accountId(), studioId);
    if (refusal.isPresent()) {
      long seconds = Math.max(1, (refusal.get().toMillis() + 999) / 1000);
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.LIMITED, started);
      throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
          .message("Too many MCP calls for now. Wait %d seconds and try again.".formatted(seconds))
          .build();
    }
    if (!caller.holds(permission)) {
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.ERROR, started);
      throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
          .message(
              "This person can't read %s here: it needs the %s permission."
                  .formatted(uri, permission.key()))
          .build();
    }
    try {
      String body = json.writeValueAsString(loader.get());
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.OK, started);
      TextResourceContents contents =
          TextResourceContents.builder(uri, body).mimeType(JSON_MIME).build();
      return ReadResourceResult.builder(List.of(contents)).build();
    } catch (NotFoundException missing) {
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.ERROR, started);
      throw McpError.RESOURCE_NOT_FOUND.apply(uri);
    } catch (AccessDeniedException denied) {
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.ERROR, started);
      throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
          .message(
              java.util.Objects.requireNonNullElse(
                  denied.getMessage(), "You don't have access to that resource."))
          .build();
    } catch (RuntimeException unexpected) {
      log(caller, studioId, logTarget, uri, ApiCallLog.Outcome.ERROR, started);
      throw unexpected;
    }
  }

  private static UUID parseGlazeId(String uri) {
    if (!uri.startsWith(GLAZE_PREFIX)) {
      throw McpError.RESOURCE_NOT_FOUND.apply(uri);
    }
    String rawId = uri.substring(GLAZE_PREFIX.length());
    try {
      return UUID.fromString(rawId);
    } catch (IllegalArgumentException badId) {
      throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
          .message("Glaze resource URI must end with a glaze UUID: " + GLAZE_TEMPLATE)
          .build();
    }
  }

  private void log(
      TenantContext caller,
      UUID studioId,
      String target,
      String uri,
      ApiCallLog.Outcome outcome,
      Instant started) {
    callLog.record(
        new ApiCallLog.Call(
            studioId,
            caller.accountId(),
            Channel.MCP,
            target,
            json.writeValueAsString(Map.of("uri", uri)),
            outcome,
            null,
            Duration.between(started, clock.instant())));
  }
}
