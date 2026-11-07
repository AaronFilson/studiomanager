package io.github.aaronfilson.studiomanager.platform.sweep;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The sweep profile: runs every sweep once, then the application exits (StudioManagerApplication).
 * A Container Apps job starts it every 6 hours, so retries, reminders and cleanups still happen
 * while the API is scaled to zero (ADR 23). If a sweep fails, the exit code is 1, so the job's
 * execution shows as failed.
 */
@Component
@Profile("sweep")
class SweepJob implements ExitCodeGenerator {

  private static final Logger log = LoggerFactory.getLogger(SweepJob.class);

  private final List<Sweep> sweeps;
  private final SweepRunner runner;
  private int exitCode;

  SweepJob(List<Sweep> sweeps, SweepRunner runner) {
    this.sweeps = sweeps;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  void runEverySweepOnce() {
    int failures = 0;
    for (Sweep sweep : sweeps) {
      SweepRunner.Outcome outcome = runner.run(sweep);
      switch (outcome) {
        case RAN -> log.atInfo().addKeyValue("sweep", sweep.name()).log("Ran {}", sweep.name());
        case SKIPPED ->
            log.atInfo()
                .addKeyValue("sweep", sweep.name())
                .log("Skipped {}: a replica is running it", sweep.name());
        case FAILED -> failures++;
      }
    }
    exitCode = failures == 0 ? 0 : 1;
    log.atInfo()
        .addKeyValue("sweeps", sweeps.size())
        .addKeyValue("failures", failures)
        .log("Sweep job finished: {} sweeps, {} failed", sweeps.size(), failures);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}
