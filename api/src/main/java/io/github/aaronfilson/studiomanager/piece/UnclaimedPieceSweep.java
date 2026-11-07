package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Marks ready pieces whose hold has run out as unclaimed, across every studio.
 *
 * <p>No studio is bound here, so row-level security would hide every piece. The work happens in
 * {@code app.mark_unclaimed_pieces}, a SECURITY DEFINER function that can do only this one thing,
 * and only for a job. It runs as {@link SystemActor#UNCLAIMED_PIECES}, which each piece's history
 * names (ADR 16). The API scales to zero, so this runs whenever an instance is up; pieces past
 * their hold are also flagged as overdue when read, so nothing depends on the sweep's timing.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.pieces.unclaimed-sweep", matchIfMissing = true)
class UnclaimedPieceSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(UnclaimedPieceSweep.class);

  private final PieceQueries queries;
  private final TransactionOperations transactions;
  private final SweepRunner sweeps;
  private final Clock clock;

  UnclaimedPieceSweep(
      PieceQueries queries, TransactionOperations transactions, SweepRunner sweeps, Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "unclaimed-pieces";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Marks every piece whose hold ended at or before {@code now}, and returns how many. */
  public int runAt(Instant now) {
    Integer marked =
        TenantContext.callAs(
            TenantContext.system(SystemActor.UNCLAIMED_PIECES),
            () -> transactions.execute(tx -> queries.markUnclaimed(now)));
    int count = marked == null ? 0 : marked;
    if (count > 0) {
      log.info("Marked {} pieces unclaimed", count);
    }
    return count;
  }
}
