package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Retries deliveries that are due, in every studio. {@link WebhookHistoryPurge} clears out old
 * history.
 *
 * <p>Finding them crosses tenants, so {@code app.due_webhook_deliveries} (ADR 10) returns only ids,
 * and only to a job. Each attempt then runs inside its studio, under row-level security, as {@link
 * SystemActor#WEBHOOK_SWEEP} (ADR 16), so it keeps working after the endpoint's creator leaves.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.webhooks.sweep", matchIfMissing = true)
class WebhookSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(WebhookSweep.class);

  static final SystemActor ACTOR = SystemActor.WEBHOOK_SWEEP;
  static final int BATCH = 200;

  private final WebhookQueries queries;
  private final TransactionOperations transactions;
  private final WebhookSender sender;
  private final SweepRunner sweeps;
  private final Clock clock;

  WebhookSweep(
      WebhookQueries queries,
      TransactionOperations transactions,
      WebhookSender sender,
      SweepRunner sweeps,
      Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.sender = sender;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1M")
  void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "webhook-deliveries";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Attempts every delivery due at {@code now}, and returns how many arrived. */
  int runAt(Instant now) {
    List<Due> due =
        TenantContext.callAs(
            TenantContext.system(ACTOR),
            () -> transactions.execute(tx -> queries.dueDeliveries(now, BATCH)));
    int arrived = 0;
    for (Due delivery : due == null ? List.<Due>of() : due) {
      TenantContext as = TenantContext.system(ACTOR, delivery.studioId());
      try {
        if (sender.attempt(as, delivery.deliveryId())) {
          arrived++;
        }
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("deliveryId", delivery.deliveryId())
            .addKeyValue("studioId", delivery.studioId())
            .log("Webhook delivery {} failed", delivery.deliveryId());
      }
    }
    return arrived;
  }

  record Due(UUID deliveryId, UUID studioId) {}
}
