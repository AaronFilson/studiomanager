package io.github.aaronfilson.studiomanager.piece;

import java.time.Instant;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Marks ready pieces whose hold has run out as unclaimed, across every studio.
 *
 * <p>No tenant is bound here, so row-level security would hide every piece. The work happens in
 * {@code app.mark_unclaimed_pieces}, a SECURITY DEFINER function that can do only this one thing.
 * The API scales to zero, so this runs whenever an instance is up; pieces past their hold are
 * also flagged as overdue when read, so nothing depends on the sweep's timing.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.pieces.unclaimed-sweep", matchIfMissing = true)
class UnclaimedPieceSweep {

    private static final Logger log = LoggerFactory.getLogger(UnclaimedPieceSweep.class);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;

    UnclaimedPieceSweep(JdbcClient jdbc, TransactionOperations transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    public void run() {
        runAt(Instant.now());
    }

    /** Marks every piece whose hold ended at or before {@code now}, and returns how many. */
    public int runAt(Instant now) {
        Integer marked = transactions.execute(tx -> jdbc.sql("select app.mark_unclaimed_pieces(:now)")
                .param("now", now.atOffset(ZoneOffset.UTC))
                .query(Integer.class).single());
        int count = marked == null ? 0 : marked;
        if (count > 0) {
            log.info("Marked {} pieces unclaimed", count);
        }
        return count;
    }

}
