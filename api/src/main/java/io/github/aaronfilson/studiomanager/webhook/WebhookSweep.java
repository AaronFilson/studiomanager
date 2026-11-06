package io.github.aaronfilson.studiomanager.webhook;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Retries deliveries that are due, in every studio, and clears out old history.
 *
 * <p>Finding them crosses tenants, so {@code app.due_webhook_deliveries} (ADR 10) returns only
 * ids, and only to a job. Each attempt then runs inside its studio, under row-level security, as
 * {@link SystemActor#WEBHOOK_SWEEP} (ADR 16), so it keeps working after the endpoint's creator leaves.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.webhooks.sweep", matchIfMissing = true)
class WebhookSweep {

    private static final Logger log = LoggerFactory.getLogger(WebhookSweep.class);

    static final SystemActor ACTOR = SystemActor.WEBHOOK_SWEEP;
    static final int BATCH = 200;
    static final Duration KEEP = Duration.ofDays(30);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final WebhookSender sender;

    WebhookSweep(JdbcClient jdbc, TransactionOperations transactions, WebhookSender sender) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.sender = sender;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1M")
    void run() {
        runAt(Instant.now());
    }

    /** Attempts every delivery due at {@code now}, and returns how many arrived. */
    int runAt(Instant now) {
        List<Due> due = TenantContext.callAs(TenantContext.system(ACTOR), () -> transactions.execute(tx -> jdbc
                .sql("select delivery_id, studio_id from app.due_webhook_deliveries(:now, :max)")
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("max", BATCH)
                .query(Due.class).list()));
        int arrived = 0;
        for (Due delivery : due == null ? List.<Due>of() : due) {
            TenantContext as = TenantContext.system(ACTOR, delivery.studioId());
            try {
                if (sender.attempt(as, delivery.deliveryId())) {
                    arrived++;
                }
            } catch (RuntimeException e) {
                log.warn("Webhook delivery {} in studio {} failed: {}", delivery.deliveryId(), delivery.studioId(),
                        e.toString());
            }
        }
        return arrived;
    }

    @Scheduled(initialDelayString = "PT15M", fixedDelayString = "PT24H")
    void purge() {
        Integer purged = TenantContext.callAs(TenantContext.system(ACTOR), () -> transactions.execute(tx -> jdbc
                .sql("select app.purge_webhook_history(:before)")
                .param("before", Instant.now().minus(KEEP).atOffset(ZoneOffset.UTC))
                .query(Integer.class).single()));
        if (purged != null && purged > 0) {
            log.info("Purged {} webhook events older than {} days", purged, KEEP.toDays());
        }
    }

    record Due(UUID deliveryId, UUID studioId) {
    }

}
