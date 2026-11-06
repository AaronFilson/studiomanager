package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The default rate limits, from {@code studiomanager.rate-limit.mcp.*} and {@code
 * studiomanager.rate-limit.token.*}. The admin page can override them while the app runs.
 *
 * @param mcp MCP tool calls, however the caller signed in
 * @param token REST calls made with a personal API token
 */
@ConfigurationProperties("studiomanager.rate-limit")
public record RateLimitProperties(Limits mcp, Limits token) {

  public RateLimitProperties {
    requireSensible("mcp", mcp);
    requireSensible("token", token);
  }

  private static void requireSensible(String channelName, Limits limits) {
    Objects.requireNonNull(limits, "studiomanager.rate-limit." + channelName);
    if (limits.burst() < 1
        || limits.refillTokens() < 1
        || limits.dailyCap() < 1
        || limits.refillPeriod() == null
        || limits.refillPeriod().isNegative()
        || limits.refillPeriod().isZero()) {
      throw new IllegalArgumentException(
          "studiomanager.rate-limit.%s needs a burst, refill and daily cap of at least 1, and a refill period"
              .formatted(channelName));
    }
  }
}
