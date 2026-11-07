package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The configured rate limits ({@code studiomanager.rate-limit.mcp.*} and {@code .token.*}), which
 * the admin page can override. Any value left out of configuration takes the plan's starting value
 * (docs/planning/20, section 6).
 */
@ConfigurationProperties("studiomanager.rate-limit")
record RateLimitDefaults(@Nullable Channel mcp, @Nullable Channel token) {

  static final Limits MCP_START = new Limits(30, 1, Duration.ofSeconds(10), 500);
  static final Limits TOKEN_START = new Limits(200, 2, Duration.ofSeconds(1), 5_000);

  Limits mcpLimits() {
    return mcp == null ? MCP_START : mcp.over(MCP_START);
  }

  Limits tokenLimits() {
    return token == null ? TOKEN_START : token.over(TOKEN_START);
  }

  /** One channel's configured values; each one left out keeps the starting value. */
  record Channel(
      @Nullable Integer burst,
      @Nullable Integer refillTokens,
      @Nullable Duration refillPeriod,
      @Nullable Integer dailyCap) {

    Limits over(Limits start) {
      return new Limits(
          burst == null ? start.burst() : burst,
          refillTokens == null ? start.refillTokens() : refillTokens,
          refillPeriod == null ? start.refillPeriod() : refillPeriod,
          dailyCap == null ? start.dailyCap() : dailyCap);
    }
  }
}
