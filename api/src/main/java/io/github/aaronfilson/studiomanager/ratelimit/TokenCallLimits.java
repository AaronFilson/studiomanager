package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.json.JsonMapper;

/**
 * The rate limit and call log for REST calls made with a personal API token. A refused call gets
 * {@code 429 Too Many Requests} with {@code Retry-After}. Calls to the MCP endpoint are left to the
 * MCP tools, which count only {@code tools/call}. Signed-in use of the web and mobile apps isn't
 * limited.
 */
@Component
class TokenCallLimits implements HandlerInterceptor {

  private static final PathPattern MCP_ROUTE =
      PathPatternParser.defaultInstance.parse("/api/v1/studios/{studioId}/mcp");
  private static final String STARTED = TokenCallLimits.class.getName() + ".started";

  private final RateLimiter limiter;
  private final ApiCallLog callLog;
  private final Clock clock;
  private final JsonMapper json;

  TokenCallLimits(RateLimiter limiter, ApiCallLog callLog, Clock clock, JsonMapper json) {
    this.limiter = limiter;
    this.callLog = callLog;
    this.clock = clock;
    this.json = json;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    Optional<TenantContext> caller = tokenCallerInStudio(request);
    if (caller.isEmpty()) {
      return true;
    }
    UUID studioId = requireStudio(caller.get());
    Instant started = clock.instant();
    Optional<Duration> refusal = limiter.refusal(Channel.TOKEN, caller.get().accountId(), studioId);
    if (refusal.isPresent()) {
      refuse(response, refusal.get());
      callLog.record(
          new ApiCallLog.Call(
              studioId,
              caller.get().accountId(),
              Channel.TOKEN,
              target(request),
              null,
              ApiCallLog.Outcome.LIMITED,
              HttpStatus.TOO_MANY_REQUESTS.value(),
              Duration.ZERO));
      return false;
    }
    request.setAttribute(STARTED, started);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      @Nullable Exception failure) {
    if (!(request.getAttribute(STARTED) instanceof Instant started)) {
      return;
    }
    TenantContext caller = TenantContext.require();
    int status = response.getStatus();
    boolean failed = failure != null || status >= HttpStatus.BAD_REQUEST.value();
    callLog.record(
        new ApiCallLog.Call(
            requireStudio(caller),
            caller.accountId(),
            Channel.TOKEN,
            target(request),
            null,
            failed ? ApiCallLog.Outcome.ERROR : ApiCallLog.Outcome.OK,
            status,
            Duration.between(started, clock.instant())));
  }

  /** The caller, if this request signed in with a token and isn't for the MCP endpoint. */
  private static Optional<TenantContext> tokenCallerInStudio(HttpServletRequest request) {
    if (ApiCallLog.currentTokenId().isEmpty() || isMcp(request)) {
      return Optional.empty();
    }
    // A token only ever works inside its studio (TokenPolicy), so one is bound
    return TenantContext.current().filter(context -> context.studioId() != null);
  }

  private static UUID requireStudio(TenantContext caller) {
    UUID studioId = caller.studioId();
    if (studioId == null) {
      throw new IllegalStateException("A token call is always bound to a studio");
    }
    return studioId;
  }

  private static boolean isMcp(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return MCP_ROUTE.matches(PathContainer.parsePath(path));
  }

  /** The method and route pattern, such as {@code GET /api/v1/studios/{studioId}/pieces}. */
  private static String target(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String route = pattern == null ? request.getRequestURI() : pattern.toString();
    return request.getMethod() + " " + route;
  }

  private void refuse(HttpServletResponse response, Duration wait) throws IOException {
    long seconds = Math.max(1, (wait.toMillis() + 999) / 1000);
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
    problem.setTitle("Too many requests");
    problem.setDetail(
        "This token's calls are over the limit for now. Try again in %d seconds."
            .formatted(seconds));
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    json.writeValue(response.getOutputStream(), problem);
  }
}
