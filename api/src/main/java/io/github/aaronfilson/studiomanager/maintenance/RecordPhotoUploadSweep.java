package io.github.aaronfilson.studiomanager.maintenance;

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
 * Removes record photo uploads that were started and never finished, in every studio, with any
 * images that arrived. The same as the piece module's sweep, through its own SECURITY DEFINER
 * function, {@code app.take_abandoned_record_photo_uploads} (ADR 10), as {@link
 * SystemActor#PHOTO_CLEANUP} (ADR 16).
 */
@Component
@ConditionalOnBooleanProperty(
    name = "studiomanager.maintenance.photo-upload-sweep",
    matchIfMissing = true)
class RecordPhotoUploadSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(RecordPhotoUploadSweep.class);

  static final Duration ABANDONED_AFTER = Duration.ofDays(1);

  private final MaintenanceQueries queries;
  private final TransactionOperations transactions;
  private final BlobStore blobs;
  private final SweepRunner sweeps;
  private final Clock clock;

  RecordPhotoUploadSweep(
      MaintenanceQueries queries,
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

  @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "record-photo-uploads";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Removes uploads started more than a day before {@code now}, and returns how many. */
  public int runAt(Instant now) {
    List<MaintenanceQueries.AbandonedPhoto> taken =
        TenantContext.callAs(
            TenantContext.system(SystemActor.PHOTO_CLEANUP),
            () ->
                transactions.execute(tx ->
                    queries.takeAbandonedPhotoUploads(now.minus(ABANDONED_AFTER))));
    if (taken == null || taken.isEmpty()) {
      return 0;
    }
    for (MaintenanceQueries.AbandonedPhoto photo : taken) {
      try {
        blobs.delete(RecordPhotoService.full(photo.studioId(), photo.recordId(), photo.photoId()));
        blobs.delete(
            RecordPhotoService.thumbnail(photo.studioId(), photo.recordId(), photo.photoId()));
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("studioId", photo.studioId())
            .addKeyValue("photoId", photo.photoId())
            .log("Couldn't delete the images of abandoned record photo {}", photo.photoId());
      }
    }
    log.info("Removed {} abandoned record photo uploads", taken.size());
    return taken.size();
  }
}
