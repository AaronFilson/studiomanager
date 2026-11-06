package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Limits lifted for a while: no limits at first, then 8×, 4× and 2× the normal limits, a third of
 * the easing time each, then normal again. It lasts at most {@link #LONGEST}, so a forgotten boost
 * ends on its own.
 *
 * @param studioId the one studio it lifts, or {@code null} for every studio
 */
record RateLimitBoost(
    Instant startsAt, @Nullable UUID studioId, Duration unlimited, Duration easing) {

  static final Duration LONGEST = Duration.ofHours(4);

  /** The easing steps' multipliers, in order. */
  static final List<Integer> EASING_MULTIPLIERS = List.of(8, 4, 2);

  RateLimitBoost {
    Objects.requireNonNull(startsAt, "startsAt");
    if (unlimited.isNegative() || easing.isNegative()) {
      throw new IllegalArgumentException("A boost's phases can't be negative");
    }
    Duration total = unlimited.plus(easing);
    if (total.isZero() || total.compareTo(LONGEST) > 0) {
      throw new IllegalArgumentException("A boost lasts more than nothing and at most 4 hours");
    }
  }

  Instant endsAt() {
    return startsAt.plus(unlimited).plus(easing);
  }

  boolean isOverAt(Instant now) {
    return !now.isBefore(endsAt());
  }

  boolean lifts(UUID studio) {
    return studioId == null || studioId.equals(studio);
  }

  /** The boost's steps, in order: the unlimited one (if any), then each easing step. */
  List<Step> steps() {
    List<Step> steps = new ArrayList<>();
    Instant easingStarts = startsAt.plus(unlimited);
    if (unlimited.isPositive()) {
      steps.add(new Step.Unlimited(startsAt, easingStarts));
    }
    if (easing.isPositive()) {
      int count = EASING_MULTIPLIERS.size();
      for (int index = 0; index < count; index++) {
        Instant from = easingStarts.plus(easing.multipliedBy(index).dividedBy(count));
        Instant until = easingStarts.plus(easing.multipliedBy(index + 1L).dividedBy(count));
        steps.add(new Step.Easing(from, until, EASING_MULTIPLIERS.get(index), index + 1));
      }
    }
    return steps;
  }

  /** The step in force at {@code now}, if the boost is under way. */
  @Nullable Step stepAt(Instant now) {
    for (Step step : steps()) {
      if (!now.isBefore(step.startsAt()) && now.isBefore(step.endsAt())) {
        return step;
      }
    }
    return null;
  }

  /** One stretch of a boost, and what it does to the normal limits. */
  sealed interface Step {

    Instant startsAt();

    Instant endsAt();

    /**
     * The limit this step puts in force. {@code revision} identifies the normal limits and the
     * boost; each step adds its own number to it, below 16.
     */
    EffectiveLimit limit(Limits normal, long revision);

    /** No limits at all until {@code endsAt}. */
    record Unlimited(Instant startsAt, Instant endsAt) implements Step {

      @Override
      public EffectiveLimit limit(Limits normal, long revision) {
        return new EffectiveLimit.Unlimited(endsAt);
      }
    }

    /**
     * The normal limits times {@code multiplier}. {@code number} counts the easing steps from 1.
     */
    record Easing(Instant startsAt, Instant endsAt, int multiplier, int number) implements Step {

      @Override
      public EffectiveLimit limit(Limits normal, long revision) {
        Limits eased =
            new Limits(
                Math.multiplyExact(normal.burst(), multiplier),
                Math.multiplyExact(normal.refillTokens(), multiplier),
                normal.refillPeriod(),
                Math.multiplyExact(normal.dailyCap(), multiplier));
        return new EffectiveLimit.Limited(eased, revision + number);
      }
    }
  }
}
