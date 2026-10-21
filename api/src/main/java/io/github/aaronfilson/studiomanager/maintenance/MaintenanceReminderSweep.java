package io.github.aaronfilson.studiomanager.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Reminds people of maintenance coming due, in every studio.
 *
 * <p>Finding the studios crosses tenants, so {@code app.studios_with_maintenance_to_remind}, a
 * SECURITY DEFINER function (ADR 10), returns only their ids and an owner of each. The reminding
 * itself runs inside each studio as that owner, under row-level security like any request, one
 * transaction per studio so a failure in one doesn't hold back the others. Each task is reminded
 * once per cycle, so running hourly costs nothing extra.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.maintenance.reminder-sweep", matchIfMissing = true)
class MaintenanceReminderSweep {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceReminderSweep.class);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final MaintenanceService maintenance;

    MaintenanceReminderSweep(JdbcClient jdbc, TransactionOperations transactions, MaintenanceService maintenance) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.maintenance = maintenance;
    }

    @Scheduled(initialDelayString = "PT4M", fixedDelayString = "PT1H")
    public void run() {
        runAt(Instant.now());
    }

    /** Reminds about every task due or due soon at {@code now}, and returns how many. */
    public int runAt(Instant now) {
        List<StudioOwner> studios = transactions.execute(tx -> jdbc
                .sql("select studio_id, owner_id from app.studios_with_maintenance_to_remind()")
                .query((rs, row) -> new StudioOwner(rs.getObject("studio_id", UUID.class),
                        rs.getObject("owner_id", UUID.class)))
                .list());
        int reminded = 0;
        for (StudioOwner studio : studios == null ? List.<StudioOwner>of() : studios) {
            TenantContext owner = TenantContext.forAccount(studio.ownerId()).inStudio(studio.studioId(), StudioRole.OWNER);
            try {
                reminded += TenantContext.callAs(owner, () -> maintenance.remindDue(now));
            } catch (RuntimeException e) {
                log.warn("Couldn't send maintenance reminders for studio {}: {}", studio.studioId(), e.getMessage());
            }
        }
        if (reminded > 0) {
            log.info("Sent reminders for {} maintenance tasks", reminded);
        }
        return reminded;
    }

    private record StudioOwner(UUID studioId, UUID ownerId) {
    }

}
