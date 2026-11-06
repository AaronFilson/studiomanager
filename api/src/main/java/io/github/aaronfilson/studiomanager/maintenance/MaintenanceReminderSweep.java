package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
 * SECURITY DEFINER function (ADR 10) that only a job may call, returns only their ids. The
 * reminding itself runs inside each studio as {@link SystemActor#MAINTENANCE_REMINDERS} (ADR 16),
 * under row-level security like any request, so it works whether or not the studio has an owner,
 * one transaction per studio so a failure in one doesn't hold back the others. Each task is
 * reminded once per cycle, so running hourly costs nothing extra.
 */
@Component
@ConditionalOnBooleanProperty(
    name = "studiomanager.maintenance.reminder-sweep",
    matchIfMissing = true)
class MaintenanceReminderSweep {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceReminderSweep.class);

  static final SystemActor ACTOR = SystemActor.MAINTENANCE_REMINDERS;

  private final JdbcClient jdbc;
  private final TransactionOperations transactions;
  private final MaintenanceService maintenance;

  MaintenanceReminderSweep(
      JdbcClient jdbc, TransactionOperations transactions, MaintenanceService maintenance) {
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
    List<UUID> studios =
        TenantContext.callAs(
            TenantContext.system(ACTOR),
            () ->
                transactions.execute(tx ->
                    jdbc.sql("select studio_id from app.studios_with_maintenance_to_remind()")
                        .query(UUID.class)
                        .list()));
    int reminded = 0;
    for (UUID studioId : studios == null ? List.<UUID>of() : studios) {
      try {
        reminded +=
            TenantContext.callAs(
                TenantContext.system(ACTOR, studioId), () -> maintenance.remindDue(now));
      } catch (RuntimeException e) {
        log.warn("Couldn't send maintenance reminders for studio {}: {}", studioId, e.getMessage());
      }
    }
    if (reminded > 0) {
      log.info("Sent reminders for {} maintenance tasks", reminded);
    }
    return reminded;
  }
}
