package io.github.aaronfilson.studiomanager.platform;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries failed event publications, such as an email the provider refused. {@link OutboxPurge}
 * clears out old completed ones.
 *
 * <p>A failure waits at least {@link #MIN_AGE} before its next attempt, and gives up after {@link
 * #MAX_ATTEMPTS}, staying in {@code event_publication} as {@code FAILED} for someone to look at.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.outbox.retry", matchIfMissing = true)
class OutboxRetry implements Sweep {

  static final Duration MIN_AGE = Duration.ofMinutes(5);
  static final int MAX_ATTEMPTS = 8;

  private final FailedEventPublications failed;
  private final SweepRunner sweeps;

  OutboxRetry(FailedEventPublications failed, SweepRunner sweeps) {
    this.failed = failed;
    this.sweeps = sweeps;
  }

  @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
  void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "outbox-retry";
  }

  @Override
  public void sweep() {
    failed.resubmit(
        ResubmissionOptions.defaults()
            .withMinAge(MIN_AGE)
            .withFilter(publication -> publication.getCompletionAttempts() < MAX_ATTEMPTS));
  }
}
