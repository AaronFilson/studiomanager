package io.github.aaronfilson.studiomanager.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The rate limits in force right now. Configuration supplies the defaults; the admin page can
 * override them and schedule a boost.
 */
public interface RateLimitSettings {

  /** The limit that applies to one channel in one studio at this moment. */
  EffectiveLimit current(Channel channel, UUID studioId);

  /** Where a call came in. */
  enum Channel {
    MCP,
    TOKEN
  }

  /** Burst size, refill rate and daily cap for one bucket. */
  record Limits(int burst, int refillTokens, Duration refillPeriod, int dailyCap) {}

  /** Either no limit until a time, or a set of limits. */
  sealed interface EffectiveLimit {

    /** No limit, and calls don't count toward the daily cap, until {@code until}. */
    record Unlimited(Instant until) implements EffectiveLimit {}

    /**
     * Limits in force. {@code revision} changes whenever the limits change, so a bucket is rebuilt
     * only then.
     */
    record Limited(Limits limits, long revision) implements EffectiveLimit {}
  }
}
