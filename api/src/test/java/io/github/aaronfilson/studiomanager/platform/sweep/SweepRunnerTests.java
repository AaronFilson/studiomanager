package io.github.aaronfilson.studiomanager.platform.sweep;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner.Outcome;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** One copy of a sweep at a time, across replicas and the sweep job, under an advisory lock. */
@IntegrationTest
class SweepRunnerTests {

  @Autowired SweepRunner runner;

  @Autowired MeterRegistry meters;

  @Test
  void runsTheSweepAndTimesIt() {
    AtomicInteger passes = new AtomicInteger();
    Sweep counting = sweep(uniqueName(), passes::incrementAndGet);

    assertThat(runner.run(counting)).isEqualTo(Outcome.RAN);
    assertThat(passes).hasValue(1);
    assertThat(
            meters
                .find(SweepRunner.OBSERVATION)
                .tag("sweep", counting.name())
                .tag("outcome", "ran")
                .timer())
        .isNotNull()
        .satisfies(timer -> assertThat(timer.count()).isEqualTo(1));
  }

  @Test
  void aSecondCopyIsSkippedWhileTheFirstRuns() throws Exception {
    String sharedName = uniqueName();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    AtomicInteger passes = new AtomicInteger();
    Sweep blocking =
        sweep(
            sharedName,
            () -> {
              passes.incrementAndGet();
              firstStarted.countDown();
              awaitQuietly(releaseFirst);
            });
    CompletableFuture<Outcome> first = CompletableFuture.supplyAsync(() -> runner.run(blocking));
    assertThat(firstStarted.await(30, TimeUnit.SECONDS)).isTrue();

    // Another replica, or the sweep job, reaches for the same sweep: it does nothing
    Outcome second = runner.run(sweep(sharedName, passes::incrementAndGet));
    releaseFirst.countDown();

    assertThat(second).isEqualTo(Outcome.SKIPPED);
    assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(Outcome.RAN);
    assertThat(passes).hasValue(1);
    // Once the first has finished, the lock is free again
    assertThat(runner.run(sweep(sharedName, passes::incrementAndGet))).isEqualTo(Outcome.RAN);
    assertThat(passes).hasValue(2);
  }

  @Test
  void aFailureIsReportedAndFreesTheLock() {
    String name = uniqueName();
    Sweep failing =
        sweep(
            name,
            () -> {
              throw new IllegalStateException("storage is down");
            });
    assertThat(runner.run(failing)).isEqualTo(Outcome.FAILED);
    assertThat(runner.run(sweep(name, () -> {}))).isEqualTo(Outcome.RAN);
  }

  @Test
  void theOutboxLagIsMeasured() {
    assertThat(meters.find("studiomanager.outbox.incomplete").gauge())
        .isNotNull()
        .satisfies(gauge -> assertThat(gauge.value()).isGreaterThanOrEqualTo(0));
    assertThat(meters.find("studiomanager.outbox.oldest.age").gauge())
        .isNotNull()
        .satisfies(gauge -> assertThat(gauge.value()).isGreaterThanOrEqualTo(0));
  }

  private static String uniqueName() {
    return "test-" + UUID.randomUUID();
  }

  private static Sweep sweep(String sweepName, Runnable work) {
    return new Sweep() {
      @Override
      public String name() {
        return sweepName;
      }

      @Override
      public void sweep() {
        work.run();
      }
    };
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
