package io.github.aaronfilson.studiomanager.stream;

import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code stream_event_seen} from growing forever: once a day it forgets ids older than {@link
 * ActivityProjection#REMEMBER_SEEN}, which is longer than the topic keeps events, so a replay can't
 * count anything twice. Runs whether streaming is on or not, so turning it off still clears what's
 * left.
 */
@Component
class SeenEventPurge implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(SeenEventPurge.class);

  private final ActivityProjection projection;
  private final SweepRunner sweeps;

  SeenEventPurge(ActivityProjection projection, SweepRunner sweeps) {
    this.projection = projection;
    this.sweeps = sweeps;
  }

  @Scheduled(initialDelayString = "PT15M", fixedDelayString = "PT24H")
  void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "stream-seen-event-purge";
  }

  @Override
  public void sweep() {
    // Crosses studios, so it runs as the consumer's system account outside any one studio
    int forgotten =
        TenantContext.callAs(
            TenantContext.system(SystemActor.ACTIVITY_CONSUMER),
            () ->
                projection.forgetSeenBefore(Instant.now().minus(ActivityProjection.REMEMBER_SEEN)));
    if (forgotten > 0) {
      log.info(
          "Forgot {} stream event ids older than {}", forgotten, ActivityProjection.REMEMBER_SEEN);
    }
  }
}
