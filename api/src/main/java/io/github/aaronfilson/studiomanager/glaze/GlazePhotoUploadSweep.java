package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.sweep.Sweep;
import io.github.aaronfilson.studiomanager.platform.sweep.SweepRunner;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Removes glaze test-tile photo uploads that were started and never finished, across every studio,
 * acting as {@link SystemActor#PHOTO_CLEANUP} (ADR 12 & 16).
 */
@Component
@ConditionalOnBooleanProperty(
    name = "studiomanager.glazes.photo-upload-sweep",
    matchIfMissing = true)
class GlazePhotoUploadSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(GlazePhotoUploadSweep.class);

  static final Duration ABANDONED_AFTER = Duration.ofDays(1);

  private final GlazeQueries queries;
  private final TransactionOperations transactions;
  private final BlobStore blobs;
  private final SweepRunner sweeps;
  private final Clock clock;

  GlazePhotoUploadSweep(
      GlazeQueries queries,
      TransactionOperations transactions,
      BlobStore blobs,
      SweepRunner sweeps,
      Clock clock) {
    this.queries = queries;
    this.transactions = transactions;
    this.blobs = blobs;
    this.sweeps = sweeps;
    this.clock = clock;
  }

  @Scheduled(initialDelayString = "PT7M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "glaze-photo-uploads";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Removes abandoned uploads started more than a day before {@code now}, and returns how many. */
  public int runAt(Instant now) {
    List<GlazeQueries.AbandonedPhoto> taken =
        TenantContext.callAs(
            TenantContext.system(SystemActor.PHOTO_CLEANUP),
            () ->
                transactions.execute(
                    tx -> queries.takeAbandonedPhotoUploads(now.minus(ABANDONED_AFTER))));
    if (taken == null || taken.isEmpty()) {
      return 0;
    }
    for (GlazeQueries.AbandonedPhoto photo : taken) {
      try {
        blobs.delete(GlazePhotoService.full(photo.studioId(), photo.recipeId(), photo.photoId()));
        blobs.delete(
            GlazePhotoService.thumbnail(photo.studioId(), photo.recipeId(), photo.photoId()));
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("studioId", photo.studioId())
            .addKeyValue("photoId", photo.photoId())
            .log("Couldn't delete the images of abandoned glaze photo {}", photo.photoId());
      }
    }
    log.info("Removed {} abandoned glaze photo uploads", taken.size());
    return taken.size();
  }
}
