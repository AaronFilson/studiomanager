package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.admin.RateLimitValues.StoredBoost;
import io.github.aaronfilson.studiomanager.admin.RateLimitValues.StoredLimits;
import io.github.aaronfilson.studiomanager.admin.RateLimitValues.StoredNormal;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * What the admin page does with the rate limits: override the configured ones, go back to them, and
 * schedule or end a boost. Every change is saved through {@link PlatformSettings}, so the database
 * checks the caller is a platform admin and keeps the history.
 */
@Service
class RateLimitAdmin {

  /** How far ahead a boost can be scheduled. */
  static final Duration FURTHEST_START = Duration.ofDays(30);

  /** The normal limits for both channels. */
  record ChannelLimits(Limits mcp, Limits token) {}

  /**
   * The limits as the admin page shows them.
   *
   * @param override the saved override, if the configured defaults aren't in force
   * @param normal what applies outside a boost
   * @param boost the boost, if one is scheduled or under way
   */
  record Overview(
      Instant now,
      ChannelLimits defaults,
      @Nullable ChannelLimits override,
      ChannelLimits normal,
      @Nullable RateLimitBoost boost,
      @Nullable String boostStudioName) {}

  private final DatabaseRateLimitSettings limits;
  private final PlatformSettings settings;
  private final Clock clock;

  RateLimitAdmin(DatabaseRateLimitSettings limits, PlatformSettings settings, Clock clock) {
    this.limits = limits;
    this.settings = settings;
    this.clock = clock;
  }

  Overview overview() {
    Instant now = clock.instant();
    return overview(limits.fresh(now), now);
  }

  Overview setNormal(ChannelLimits normal) {
    check(normal.mcp(), "MCP");
    check(normal.token(), "Token");
    StoredNormal stored =
        new StoredNormal(StoredLimits.of(normal.mcp()), StoredLimits.of(normal.token()));
    settings.save(RateLimitValues.NORMAL_KEY, limits.values().write(stored));
    return afterSave();
  }

  /** Back to the configured defaults. Nothing is saved if they're already in force. */
  Overview resetNormal() {
    if (limits.reread().override() != null) {
      settings.save(RateLimitValues.NORMAL_KEY, null);
    }
    return afterSave();
  }

  Overview scheduleBoost(
      @Nullable Instant startsAt,
      @Nullable UUID studioId,
      int unlimitedMinutes,
      int easingMinutes) {
    Instant now = clock.instant();
    if (unlimitedMinutes < 0 || easingMinutes < 0) {
      throw new InvalidInputException("A boost's minutes can't be negative.");
    }
    Duration unlimited = Duration.ofMinutes(unlimitedMinutes);
    Duration easing = Duration.ofMinutes(easingMinutes);
    Duration total = unlimited.plus(easing);
    if (total.isZero() || total.compareTo(RateLimitBoost.LONGEST) > 0) {
      throw new InvalidInputException("A boost lasts at least a minute and at most 4 hours.");
    }
    if (startsAt != null && startsAt.isAfter(now.plus(FURTHEST_START))) {
      throw new InvalidInputException("A boost can be scheduled at most 30 days ahead.");
    }
    Instant start = startsAt == null || startsAt.isBefore(now) ? now : startsAt;
    RateLimitBoost boost = new RateLimitBoost(start, studioId, unlimited, easing);
    settings.save(RateLimitValues.BOOST_KEY, limits.values().write(StoredBoost.of(boost)));
    return afterSave();
  }

  /** Normal limits at once. Nothing is saved if no boost is scheduled or under way. */
  Overview endBoost() {
    RateLimitBoost boost = limits.reread().boost();
    if (boost != null && !boost.isOverAt(clock.instant())) {
      settings.save(RateLimitValues.BOOST_KEY, null);
    }
    return afterSave();
  }

  /** The studios a boost can be scoped to. */
  List<PlatformSettings.StudioChoice> studios() {
    return settings.studios();
  }

  List<PlatformSettings.Change> history(int limit) {
    return settings.history(limit);
  }

  private Overview afterSave() {
    return overview(limits.reread(), clock.instant());
  }

  private Overview overview(DatabaseRateLimitSettings.Snapshot snapshot, Instant now) {
    RateLimitDefaults defaults = limits.defaults();
    StoredNormal override = snapshot.override();
    RateLimitBoost boost = snapshot.boost();
    RateLimitBoost current = boost == null || boost.isOverAt(now) ? null : boost;
    return new Overview(
        now,
        new ChannelLimits(defaults.mcpLimits(), defaults.tokenLimits()),
        override == null
            ? null
            : new ChannelLimits(override.mcp().limits(), override.token().limits()),
        new ChannelLimits(snapshot.mcp(), snapshot.token()),
        current,
        current == null ? null : studioName(current.studioId()));
  }

  private @Nullable String studioName(@Nullable UUID studioId) {
    if (studioId == null) {
      return null;
    }
    return settings.studios().stream()
        .filter(studio -> studio.studioId().equals(studioId))
        .map(PlatformSettings.StudioChoice::studioName)
        .findFirst()
        .orElse(null);
  }

  private static void check(Limits limits, String channel) {
    if (limits.burst() < 1 || limits.burst() > 100_000) {
      throw new InvalidInputException(channel + " burst is between 1 and 100,000.");
    }
    if (limits.refillTokens() < 1 || limits.refillTokens() > 100_000) {
      throw new InvalidInputException(channel + " refill is between 1 and 100,000 calls.");
    }
    long periodSeconds = limits.refillPeriod().toSeconds();
    if (periodSeconds < 1 || periodSeconds > 3_600) {
      throw new InvalidInputException(channel + " refill period is between 1 second and 1 hour.");
    }
    if (limits.dailyCap() < 1 || limits.dailyCap() > 10_000_000) {
      throw new InvalidInputException(channel + " daily cap is between 1 and 10,000,000.");
    }
  }
}
