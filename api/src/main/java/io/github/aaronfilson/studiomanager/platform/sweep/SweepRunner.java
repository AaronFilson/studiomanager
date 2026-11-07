package io.github.aaronfilson.studiomanager.platform.sweep;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs a {@link Sweep} once, unless another replica or the sweep job is already running it.
 *
 * <p>The guard is {@code pg_try_advisory_xact_lock}, keyed on the sweep's name, taken in a
 * transaction on a connection of its own and held until the sweep finishes. The sweep's own work
 * uses other connections and transactions as usual, so a sweep that commits studio by studio still
 * does. If the process dies, Postgres ends the transaction and frees the lock; nothing needs
 * cleaning up (ADR 23).
 *
 * <p>Each run is an {@link Observation} named {@code studiomanager.sweep}, tagged with the sweep
 * and its outcome, which gives a timer and, where tracing is on, a span.
 */
@Component
public class SweepRunner {

  private static final Logger log = LoggerFactory.getLogger(SweepRunner.class);

  static final String OBSERVATION = "studiomanager.sweep";

  /** What happened to one run. */
  public enum Outcome {
    /** It ran to the end. */
    RAN,
    /** Another copy held the lock, so this one did nothing. */
    SKIPPED,
    /** It threw, or the lock couldn't be taken. Logged with the stack trace. */
    FAILED;

    String tag() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private final DataSource dataSource;
  private final ObservationRegistry observations;

  SweepRunner(DataSource dataSource, ObservationRegistry observations) {
    this.dataSource = dataSource;
    this.observations = observations;
  }

  public Outcome run(Sweep sweep) {
    Observation observation =
        Observation.createNotStarted(OBSERVATION, observations)
            .lowCardinalityKeyValue("sweep", sweep.name());
    // observe() starts it, makes it current for the sweep's own spans, and stops it
    return observation.observe(() -> runLocked(sweep, observation));
  }

  private Outcome runLocked(Sweep sweep, Observation observation) {
    try (Connection lockConnection = dataSource.getConnection()) {
      lockConnection.setAutoCommit(false);
      try {
        Outcome outcome = runIfLockFree(lockConnection, sweep, observation);
        observation.lowCardinalityKeyValue("outcome", outcome.tag());
        return outcome;
      } finally {
        // Ending the transaction releases the advisory lock
        lockConnection.rollback();
      }
    } catch (SQLException e) {
      observation.error(e);
      observation.lowCardinalityKeyValue("outcome", Outcome.FAILED.tag());
      log.atWarn()
          .setCause(e)
          .addKeyValue("sweep", sweep.name())
          .log("Couldn't take the lock for sweep {}", sweep.name());
      return Outcome.FAILED;
    }
  }

  private Outcome runIfLockFree(Connection lockConnection, Sweep sweep, Observation observation)
      throws SQLException {
    if (!tryLock(lockConnection, sweep.name())) {
      log.atDebug()
          .addKeyValue("sweep", sweep.name())
          .log("Sweep {} is already running elsewhere; skipped", sweep.name());
      return Outcome.SKIPPED;
    }
    try {
      sweep.sweep();
      return Outcome.RAN;
    } catch (RuntimeException e) {
      observation.error(e);
      log.atWarn()
          .setCause(e)
          .addKeyValue("sweep", sweep.name())
          .log("Sweep {} failed", sweep.name());
      return Outcome.FAILED;
    }
  }

  private static boolean tryLock(Connection lockConnection, String sweepName) throws SQLException {
    try (PreparedStatement statement =
        lockConnection.prepareStatement("select pg_try_advisory_xact_lock(hashtext(?))")) {
      statement.setString(1, "studiomanager.sweep." + sweepName);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }
}
