package io.github.aaronfilson.studiomanager.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The settings' copy, its refresh, and the revisions that tell a bucket to rebuild. */
class DatabaseRateLimitSettingsTests {

  private static final Instant START = Instant.parse("2026-11-06T15:00:00Z");
  private static final UUID STUDIO = UUID.randomUUID();

  private final SettableClock clock = new SettableClock(START);
  private final PlatformSettings stored = mock(PlatformSettings.class);
  private final DatabaseRateLimitSettings settings =
      new DatabaseRateLimitSettings(
          stored, new RateLimitDefaults(null, null), JsonMapper.builder().build(), clock);

  @Test
  void withNothingStoredTheConfiguredDefaultsApply() {
    when(stored.readAll()).thenReturn(Map.of());

    assertThat(settings.current(Channel.MCP, STUDIO))
        .isEqualTo(new EffectiveLimit.Limited(RateLimitDefaults.MCP_START, 0));
    assertThat(settings.current(Channel.TOKEN, STUDIO))
        .isEqualTo(new EffectiveLimit.Limited(RateLimitDefaults.TOKEN_START, 0));
  }

  @Test
  void theSettingsAreReadAgainOnlyOnceTheCopyIsFifteenSecondsOld() {
    when(stored.readAll()).thenReturn(Map.of());

    settings.current(Channel.MCP, STUDIO);
    clock.advance(Duration.ofSeconds(14));
    settings.current(Channel.MCP, STUDIO);
    verify(stored, times(1)).readAll();

    clock.advance(Duration.ofSeconds(1));
    settings.current(Channel.MCP, STUDIO);
    verify(stored, times(2)).readAll();
  }

  @Test
  void anOverrideAppliesAndChangesTheRevisionButAReReadOfTheSameDoesNot() {
    when(stored.readAll())
        .thenReturn(
            Map.of(
                RateLimitValues.NORMAL_KEY,
                new PlatformSettings.Setting(
                    RateLimitValues.NORMAL_KEY,
                    """
                    {"mcp": {"burst": 50, "refillTokens": 2, "refillPeriodSeconds": 10, "dailyCap": 800},
                     "token": {"burst": 300, "refillTokens": 3, "refillPeriodSeconds": 1, "dailyCap": 9000}}
                    """,
                    3)));

    EffectiveLimit first = settings.current(Channel.MCP, STUDIO);
    clock.advance(Duration.ofMinutes(1));
    EffectiveLimit again = settings.current(Channel.MCP, STUDIO);

    assertThat(first)
        .isEqualTo(
            new EffectiveLimit.Limited(new Limits(50, 2, Duration.ofSeconds(10), 800), 3L << 32));
    assertThat(again).isEqualTo(first);
  }

  @Test
  void aBoostForOneStudioLeavesTheOthersAlone() {
    when(stored.readAll())
        .thenReturn(
            Map.of(
                RateLimitValues.BOOST_KEY,
                new PlatformSettings.Setting(
                    RateLimitValues.BOOST_KEY,
                    """
                    {"startsAt": "%s", "studioId": "%s", "unlimitedMinutes": 30, "easingMinutes": 60}
                    """
                        .formatted(START, STUDIO),
                    2)));

    assertThat(settings.current(Channel.TOKEN, STUDIO))
        .isEqualTo(new EffectiveLimit.Unlimited(START.plus(Duration.ofMinutes(30))));
    assertThat(settings.current(Channel.TOKEN, UUID.randomUUID()))
        .isEqualTo(new EffectiveLimit.Limited(RateLimitDefaults.TOKEN_START, 2L << 4));

    clock.advance(Duration.ofMinutes(35));
    assertThat(settings.current(Channel.MCP, STUDIO))
        .isEqualTo(
            new EffectiveLimit.Limited(new Limits(240, 8, Duration.ofSeconds(10), 4_000), 33));
  }

  @Test
  void anUnreadableSettingFallsBackToTheDefaults() {
    when(stored.readAll())
        .thenReturn(
            Map.of(
                RateLimitValues.NORMAL_KEY,
                new PlatformSettings.Setting(RateLimitValues.NORMAL_KEY, "{\"mcp\": 7}", 1)));

    assertThat(settings.current(Channel.MCP, STUDIO))
        .isEqualTo(new EffectiveLimit.Limited(RateLimitDefaults.MCP_START, 1L << 32));
  }

  /** A clock the test moves by hand. */
  private static final class SettableClock extends Clock {

    private Instant now;

    SettableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
