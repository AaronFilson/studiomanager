package io.github.aaronfilson.studiomanager.platform;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Clears out completed event publications after {@link #KEEP_COMPLETED}, so "did that email go
 * out?" has an answer for a month and the table doesn't grow forever.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.outbox.retry", matchIfMissing = true)
class OutboxPurge implements Sweep {

  static final Duration KEEP_COMPLETED = Duration.ofDays(30);

  private final CompletedEventPublications completed;
  private final SweepRunner sweeps;

  OutboxPurge(CompletedEventPublications completed, SweepRunner sweeps) {
    this.completed = completed;
    this.sweeps = sweeps;
  }

  @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT24H")
  void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "outbox-purge";
  }

  @Override
  public void sweep() {
    completed.deletePublicationsOlderThan(KEEP_COMPLETED);
  }
}
