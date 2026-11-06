package io.github.aaronfilson.studiomanager.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Channel;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.EffectiveLimit;
import io.github.aaronfilson.studiomanager.ratelimit.RateLimitSettings.Limits;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimiterTests {

  private static final Limits TWO_A_DAY = new Limits(2, 1, Duration.ofHours(1), 2);
  private static final Limits FOUR_A_DAY = new Limits(4, 1, Duration.ofHours(1), 4);

  /** Settings a test can change, as the admin page would. */
  private static final class SettableSettings implements RateLimitSettings {
    private EffectiveLimit limit = new EffectiveLimit.Limited(TWO_A_DAY, 1);

    @Override
    public EffectiveLimit current(Channel channel, UUID studioId) {
      return limit;
    }
  }

  private final SettableSettings settings = new SettableSettings();
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final RateLimiter limiter = new RateLimiter(settings, meters);
  private final UUID person = UUID.randomUUID();
  private final UUID studio = UUID.randomUUID();

  @Test
  void aBucketRefusesOnceEmptyAndSaysHowLongToWait() {
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio))
        .hasValueSatisfying(wait -> assertThat(wait).isPositive());
    assertThat(meters.counter("studiomanager.rate_limit.refused", "channel", "MCP").count())
        .isEqualTo(1);
  }

  @Test
  void eachPersonStudioAndChannelHasItsOwnBucket() {
    limiter.refusal(Channel.MCP, person, studio);
    limiter.refusal(Channel.MCP, person, studio);

    assertThat(limiter.refusal(Channel.TOKEN, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, UUID.randomUUID(), studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, UUID.randomUUID())).isEmpty();
  }

  @Test
  void aBucketIsRebuiltOnlyWhenTheRevisionChanges() {
    limiter.refusal(Channel.MCP, person, studio);
    limiter.refusal(Channel.MCP, person, studio);

    // Same revision: the old bucket stands, even with other numbers
    settings.limit = new EffectiveLimit.Limited(FOUR_A_DAY, 1);
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isPresent();

    // A new revision: rebuilt with what's been used kept, so two more fit under four
    settings.limit = new EffectiveLimit.Limited(FOUR_A_DAY, 2);
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isPresent();
  }

  @Test
  void anUnlimitedBoostSkipsTheBucketAndDoesNotCount() {
    settings.limit = new EffectiveLimit.Unlimited(Instant.now().plusSeconds(1800));
    for (int call = 0; call < 50; call++) {
      assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    }

    settings.limit = new EffectiveLimit.Limited(TWO_A_DAY, 3);
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isEmpty();
    assertThat(limiter.refusal(Channel.MCP, person, studio)).isPresent();
  }
}
