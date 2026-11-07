package io.github.aaronfilson.studiomanager.piece;

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
 * Removes photo uploads that were started and never finished, across every studio, with any images
 * that did arrive.
 *
 * <p>Like {@link UnclaimedPieceSweep}, it runs with no studio bound, so the rows are taken by
 * {@code app.take_abandoned_photo_uploads}, a SECURITY DEFINER function that only a job may call;
 * it runs as {@link SystemActor#PHOTO_CLEANUP} (ADR 16). The images are deleted after that commits;
 * one that fails to delete is only an orphaned blob, logged and left.
 */
@Component
@ConditionalOnBooleanProperty(
    name = "studiomanager.pieces.photo-upload-sweep",
    matchIfMissing = true)
class PhotoUploadSweep implements Sweep {

  private static final Logger log = LoggerFactory.getLogger(PhotoUploadSweep.class);

  static final Duration ABANDONED_AFTER = Duration.ofDays(1);

  private final PieceQueries queries;
  private final TransactionOperations transactions;
  private final BlobStore blobs;
  private final SweepRunner sweeps;
  private final Clock clock;

  PhotoUploadSweep(
      PieceQueries queries,
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

  @Scheduled(initialDelayString = "PT3M", fixedDelayString = "PT1H")
  public void run() {
    sweeps.run(this);
  }

  @Override
  public String name() {
    return "piece-photo-uploads";
  }

  @Override
  public void sweep() {
    runAt(clock.instant());
  }

  /** Removes uploads started more than a day before {@code now}, and returns how many. */
  public int runAt(Instant now) {
    List<PieceQueries.AbandonedPhoto> taken =
        TenantContext.callAs(
            TenantContext.system(SystemActor.PHOTO_CLEANUP),
            () ->
                transactions.execute(
                    tx -> queries.takeAbandonedPhotoUploads(now.minus(ABANDONED_AFTER))));
    if (taken == null || taken.isEmpty()) {
      return 0;
    }
    for (PieceQueries.AbandonedPhoto photo : taken) {
      try {
        blobs.delete(PhotoBlobs.full(photo.studioId(), photo.pieceId(), photo.photoId()));
        blobs.delete(PhotoBlobs.thumbnail(photo.studioId(), photo.pieceId(), photo.photoId()));
      } catch (RuntimeException e) {
        log.atWarn()
            .setCause(e)
            .addKeyValue("studioId", photo.studioId())
            .addKeyValue("photoId", photo.photoId())
            .log("Couldn't delete the images of abandoned photo {}", photo.photoId());
      }
    }
    log.info("Removed {} abandoned photo uploads", taken.size());
    return taken.size();
  }
}
