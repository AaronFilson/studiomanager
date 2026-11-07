package io.github.aaronfilson.studiomanager.maintenance;

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
class MaintenanceReminderSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceReminderSweep.class);

  static final SystemActor ACTOR = SystemActor.MAINTENANCE_REMINDERS;

  private final MaintenanceQueries queries;
  private final TransactionOperations transactions;
  private final MaintenanceReminders reminders;
  private final SweepRunner sweeps;
  private final Clock clock;

  MaintenanceReminderSweep(
      MaintenanceQueries queries,
      TransactionOperations transactions,
      MaintenanceReminders reminders,
      SweepRunner sweeps,
      Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.reminders = reminders;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT4M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "maintenance-reminders";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Reminds about every task due or due soon at {@code now}, and returns how many. */
  public int runAt(Instant now) {
    List<UUID> studios =
        TenantContext.callAs(
            TenantContext.system(ACTOR),
            () -> transactions.execute(tx -> queries.studiosWithMaintenanceToRemind()));
    int reminded = 0;
    for (UUID studioId : studios == null ? List.<UUID>of() : studios) {
      try {
        reminded +=
            TenantContext.callAs(
                TenantContext.system(ACTOR, studioId), () -> reminders.remindDue(now));
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("studioId", studioId)
            .log("Couldn't send maintenance reminders for studio {}", studioId);
      }
    }
    if (reminded > 0) {
      log.info("Sent reminders for {} maintenance tasks", reminded);
    }
    return reminded;
  }
}
