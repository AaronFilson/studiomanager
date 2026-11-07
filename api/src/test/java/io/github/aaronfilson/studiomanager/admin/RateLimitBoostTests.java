package io.github.aaronfilson.studiomanager.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimitBoostTests {

  private static final Instant START = Instant.parse("2026-11-06T15:00:00Z");
  private static final Limits NORMAL = new Limits(30, 1, Duration.ofSeconds(10), 500);

  private final RateLimitBoost demo =
      new RateLimitBoost(START, null, Duration.ofMinutes(30), Duration.ofMinutes(60));

  @Test
  void theDemoPresetIsUnlimitedThenEasesDownInThreeSteps() {
    assertThat(demo.stepAt(START.minusSeconds(1))).isNull();
    assertThat(demo.stepAt(START.plusSeconds(60)).limit(NORMAL, 0))
        .isEqualTo(new EffectiveLimit.Unlimited(START.plus(Duration.ofMinutes(30))));
    assertThat(demo.stepAt(START.plus(Duration.ofMinutes(31))).limit(NORMAL, 0))
        .isEqualTo(
            new EffectiveLimit.Limited(new Limits(240, 8, Duration.ofSeconds(10), 4_000), 1));
    assertThat(demo.stepAt(START.plus(Duration.ofMinutes(55))).limit(NORMAL, 0))
        .isEqualTo(
            new EffectiveLimit.Limited(new Limits(120, 4, Duration.ofSeconds(10), 2_000), 2));
    assertThat(demo.stepAt(START.plus(Duration.ofMinutes(89))).limit(NORMAL, 0))
        .isEqualTo(new EffectiveLimit.Limited(new Limits(60, 2, Duration.ofSeconds(10), 1_000), 3));
    assertThat(demo.stepAt(START.plus(Duration.ofMinutes(90)))).isNull();
    assertThat(demo.isOverAt(START.plus(Duration.ofMinutes(90)))).isTrue();
  }

  @Test
  void theStepsCoverTheBoostWithNoGaps() {
    var steps = demo.steps();

    assertThat(steps).hasSize(4);
    assertThat(steps.getFirst().startsAt()).isEqualTo(START);
    for (int index = 1; index < steps.size(); index++) {
      assertThat(steps.get(index).startsAt()).isEqualTo(steps.get(index - 1).endsAt());
    }
    assertThat(steps.getLast().endsAt()).isEqualTo(demo.endsAt());
  }

  @Test
  void aBoostWithoutEasingIsOnlyUnlimited() {
    RateLimitBoost noEasing =
        new RateLimitBoost(START, null, Duration.ofMinutes(15), Duration.ZERO);

    assertThat(noEasing.steps()).singleElement().isInstanceOf(RateLimitBoost.Step.Unlimited.class);
    assertThat(noEasing.endsAt()).isEqualTo(START.plus(Duration.ofMinutes(15)));
  }

  @Test
  void aBoostForOneStudioLiftsOnlyThatStudio() {
    UUID demoStudio = UUID.randomUUID();
    RateLimitBoost oneStudio =
        new RateLimitBoost(START, demoStudio, Duration.ofMinutes(30), Duration.ZERO);

    assertThat(oneStudio.lifts(demoStudio)).isTrue();
    assertThat(oneStudio.lifts(UUID.randomUUID())).isFalse();
    assertThat(demo.lifts(UUID.randomUUID())).isTrue();
  }

  @Test
  void aBoostAlwaysEndsWithinFourHours() {
    assertThatThrownBy(
            () -> new RateLimitBoost(START, null, Duration.ofMinutes(200), Duration.ofMinutes(41)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RateLimitBoost(START, null, Duration.ZERO, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            new RateLimitBoost(START, null, Duration.ofMinutes(180), Duration.ofMinutes(60))
                .endsAt())
        .isEqualTo(START.plus(Duration.ofHours(4)));
  }
}
