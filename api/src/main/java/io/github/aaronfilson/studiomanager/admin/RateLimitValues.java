package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * How the rate limit settings are stored as JSON in {@code platform_setting}: plain numbers and
 * strings, so a row reads the same to a person as to the code.
 */
final class RateLimitValues {

  /** The key of the overridden normal limits. */
  static final String NORMAL_KEY = "rate-limit.normal";

  /** The key of the boost. */
  static final String BOOST_KEY = "rate-limit.boost";

  record StoredLimits(int burst, int refillTokens, long refillPeriodSeconds, int dailyCap) {

    static StoredLimits of(Limits limits) {
      return new StoredLimits(
          limits.burst(),
          limits.refillTokens(),
          limits.refillPeriod().toSeconds(),
          limits.dailyCap());
    }

    Limits limits() {
      return new Limits(burst, refillTokens, Duration.ofSeconds(refillPeriodSeconds), dailyCap);
    }
  }

  /** The normal limits for both channels, overriding the configured ones. */
  record StoredNormal(StoredLimits mcp, StoredLimits token) {}

  record StoredBoost(
      String startsAt, @Nullable String studioId, int unlimitedMinutes, int easingMinutes) {

    static StoredBoost of(RateLimitBoost boost) {
      return new StoredBoost(
          boost.startsAt().toString(),
          boost.studioId() == null ? null : boost.studioId().toString(),
          Math.toIntExact(boost.unlimited().toMinutes()),
          Math.toIntExact(boost.easing().toMinutes()));
    }

    RateLimitBoost boost() {
      return new RateLimitBoost(
          Instant.parse(startsAt),
          studioId == null ? null : UUID.fromString(studioId),
          Duration.ofMinutes(unlimitedMinutes),
          Duration.ofMinutes(easingMinutes));
    }
  }

  private final JsonMapper json;

  RateLimitValues(JsonMapper json) {
    this.json = json;
  }

  String write(Object stored) {
    return json.writeValueAsString(stored);
  }

  /** Reads a stored value, or null if there is none. Throws if it can't be read. */
  <T> @Nullable T read(@Nullable String stored, Class<T> type) throws JacksonException {
    return stored == null ? null : json.readValue(stored, type);
  }
}
