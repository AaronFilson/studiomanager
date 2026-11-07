package io.github.aaronfilson.studiomanager.ratelimit;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rate limits for MCP tool calls and API token calls (plan 20, section 6).
 *
 * <p>One bucket per person, per studio, per channel: all of a person's tokens share it, so a second
 * token doesn't double the allowance. Each bucket has two windows, a burst that refills steadily
 * and a daily cap. Buckets live in memory on each replica, so with two replicas the real ceiling is
 * up to twice the setting.
 *
 * <p>A bucket is rebuilt only when the effective limit's revision changes, and skipped altogether
 * while the limit is lifted. Calls while it's lifted don't count toward the daily cap.
 */
@Component
public class RateLimiter {

  private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
  private static final Duration DAY = Duration.ofDays(1);

  private record BucketKey(Channel channel, UUID accountId, UUID studioId) {}

  private record TrackedBucket(Bucket bucket, long revision) {}

  private final RateLimitSettings settings;
  private final MeterRegistry meters;
  private final ConcurrentMap<BucketKey, TrackedBucket> buckets = new ConcurrentHashMap<>();

  RateLimiter(RateLimitSettings settings, MeterRegistry meters) {
    this.settings = settings;
    this.meters = meters;
  }

  /**
   * Takes one call from the person's bucket. Empty if the call may go ahead; otherwise how long to
   * wait before trying again.
   */
  public Optional<Duration> refusal(Channel channel, UUID accountId, UUID studioId) {
    BucketKey bucketKey = new BucketKey(channel, accountId, studioId);
    Optional<Duration> refusal =
        switch (settings.current(channel, studioId)) {
          case EffectiveLimit.Unlimited unlimited -> {
            log.trace("{} is unlimited until {}, not counted", bucketKey, unlimited.until());
            yield Optional.empty();
          }
          case EffectiveLimit.Limited limited -> take(bucketKey, limited);
        };
    if (refusal.isPresent()) {
      meters.counter("studiomanager.rate_limit.refused", "channel", channel.name()).increment();
    }
    return refusal;
  }

  private Optional<Duration> take(BucketKey bucketKey, EffectiveLimit.Limited limited) {
    TrackedBucket tracked =
        buckets.compute(bucketKey, (key, existing) -> upToDate(key, existing, limited));
    ConsumptionProbe probe = tracked.bucket().tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return Optional.empty();
    }
    return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
  }

  private static TrackedBucket upToDate(
      BucketKey bucketKey, @Nullable TrackedBucket existing, EffectiveLimit.Limited limited) {
    if (existing == null) {
      return new TrackedBucket(
          Bucket.builder()
              .addLimit(burst(limited.limits()))
              .addLimit(daily(limited.limits()))
              .build(),
          limited.revision());
    }
    if (existing.revision() == limited.revision()) {
      return existing;
    }
    // The limits changed: a saved setting, or the next step of a boost. ADDITIVE keeps what's been
    // used: a raised limit adds the difference, and a lowered one only trims what's over it.
    log.debug("Limits for {} changed to {}", bucketKey, limited.limits());
    existing
        .bucket()
        .replaceConfiguration(configuration(limited.limits()), TokensInheritanceStrategy.ADDITIVE);
    return new TrackedBucket(existing.bucket(), limited.revision());
  }

  private static BucketConfiguration configuration(Limits limits) {
    return BucketConfiguration.builder().addLimit(burst(limits)).addLimit(daily(limits)).build();
  }

  private static Bandwidth burst(Limits limits) {
    return Bandwidth.builder()
        .capacity(limits.burst())
        .refillGreedy(limits.refillTokens(), limits.refillPeriod())
        .id("burst")
        .build();
  }

  private static Bandwidth daily(Limits limits) {
    return Bandwidth.builder()
        .capacity(limits.dailyCap())
        .refillIntervally(limits.dailyCap(), DAY)
        .id("daily")
        .build();
  }
}
