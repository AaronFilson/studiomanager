package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Clears out webhook events, deliveries and attempts older than {@link #KEEP}, in every studio,
 * through {@code app.purge_webhook_history} (ADR 10), as the webhook sweep's system actor.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.webhooks.sweep", matchIfMissing = true)
class WebhookHistoryPurge implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(WebhookHistoryPurge.class);

  static final Duration KEEP = Duration.ofDays(30);

  private final WebhookQueries queries;
  private final TransactionOperations transactions;
  private final SweepRunner sweeps;
  private final Clock clock;

  WebhookHistoryPurge(
      WebhookQueries queries, TransactionOperations transactions, SweepRunner sweeps, Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT15M", fixedDelayString = "PT24H")
  void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "webhook-history-purge";
  }

  @Override
  public void sweep() {
    Integer purged =
        TenantContext.callAs(
            TenantContext.system(WebhookSweep.ACTOR),
            () -> transactions.execute(tx -> queries.purgeHistory(clock.instant().minus(KEEP))));
    if (purged != null && purged > 0) {
      log.info("Purged {} webhook events older than {} days", purged, KEEP.toDays());
    }
  }
}
