package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.admin.RateLimitValues.StoredBoost;
import io.github.aaronfilson.studiomanager.admin.RateLimitValues.StoredNormal;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The rate limits in force: configured defaults, overridden and boosted from the admin page.
 *
 * <p>Each replica keeps a copy of the settings and reads them again on the first call after the
 * copy is {@link #REFRESH} old, and at once after it saves a change itself. Nothing runs on a
 * timer, so an idle app does no work, and a change reaches every replica within about 15 seconds.
 *
 * <p>The revision of a {@link EffectiveLimit.Limited} limit is built from the stored versions,
 * which only go up: the override's version in the high 32 bits, the boost's above the low 4, and
 * the easing step in those 4. A re-read that finds nothing new gives the same revision, so buckets
 * are rebuilt only when their limits really change.
 */
@Component
@Primary
@EnableConfigurationProperties(RateLimitDefaults.class)
public class DatabaseRateLimitSettings implements RateLimitSettings {

  static final Duration REFRESH = Duration.ofSeconds(15);

  private static final Logger log = LoggerFactory.getLogger(DatabaseRateLimitSettings.class);

  /** What was read, and when. */
  record Snapshot(
      Limits mcp,
      Limits token,
      @Nullable StoredNormal override,
      long normalVersion,
      @Nullable RateLimitBoost boost,
      long boostVersion,
      Instant readAt) {

    Limits normal(Channel channel) {
      return switch (channel) {
        case MCP -> mcp;
        case TOKEN -> token;
      };
    }

    long revision() {
      return (normalVersion << 32) | (boostVersion << 4);
    }
  }

  private final PlatformSettings settings;
  private final RateLimitDefaults defaults;
  private final RateLimitValues values;
  private final Clock clock;
  private final ReentrantLock reading = new ReentrantLock();
  private volatile @Nullable Snapshot snapshot;

  DatabaseRateLimitSettings(
      PlatformSettings settings, RateLimitDefaults defaults, JsonMapper json, Clock clock) {
    this.settings = settings;
    this.defaults = defaults;
    this.values = new RateLimitValues(json);
    this.clock = clock;
  }

  @Override
  public EffectiveLimit current(Channel channel, UUID studioId) {
    Instant now = clock.instant();
    Snapshot held = fresh(now);
    Limits normal = held.normal(channel);
    RateLimitBoost boost = held.boost();
    RateLimitBoost.Step step = boost != null && boost.lifts(studioId) ? boost.stepAt(now) : null;
    return step == null
        ? new EffectiveLimit.Limited(normal, held.revision())
        : step.limit(normal, held.revision());
  }

  /** The settings as last read, reading them again if the copy is too old. */
  Snapshot fresh(Instant now) {
    Snapshot held = snapshot;
    if (held != null && now.isBefore(held.readAt().plus(REFRESH))) {
      return held;
    }
    reading.lock();
    try {
      // Another call may have read them while this one waited
      held = snapshot;
      if (held != null && now.isBefore(held.readAt().plus(REFRESH))) {
        return held;
      }
      Snapshot read = read(now);
      snapshot = read;
      return read;
    } finally {
      reading.unlock();
    }
  }

  /** Reads the settings again now: after this replica saved a change. */
  Snapshot reread() {
    Instant now = clock.instant();
    reading.lock();
    try {
      Snapshot read = read(now);
      snapshot = read;
      return read;
    } finally {
      reading.unlock();
    }
  }

  RateLimitValues values() {
    return values;
  }

  RateLimitDefaults defaults() {
    return defaults;
  }

  private Snapshot read(Instant now) {
    Map<String, PlatformSettings.Setting> stored = settings.readAll();
    PlatformSettings.Setting normalRow = stored.get(RateLimitValues.NORMAL_KEY);
    PlatformSettings.Setting boostRow = stored.get(RateLimitValues.BOOST_KEY);
    StoredNormal override = readOrNull(normalRow, StoredNormal.class);
    StoredBoost storedBoost = readOrNull(boostRow, StoredBoost.class);
    return new Snapshot(
        override == null ? defaults.mcpLimits() : override.mcp().limits(),
        override == null ? defaults.tokenLimits() : override.token().limits(),
        override,
        normalRow == null ? 0 : normalRow.version(),
        storedBoost == null ? null : storedBoost.boost(),
        boostRow == null ? 0 : boostRow.version(),
        now);
  }

  /** A stored value, or null. One that can't be read is logged and left out, not fatal. */
  private <T> @Nullable T readOrNull(PlatformSettings.@Nullable Setting row, Class<T> type) {
    if (row == null) {
      return null;
    }
    try {
      return values.read(row.json(), type);
    } catch (JacksonException | IllegalArgumentException unreadable) {
      log.warn("Ignoring the setting {} (version {})", row.key(), row.version(), unreadable);
      return null;
    }
  }
}
