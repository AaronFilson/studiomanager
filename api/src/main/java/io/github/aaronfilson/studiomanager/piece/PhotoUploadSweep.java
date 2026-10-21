package io.github.aaronfilson.studiomanager.piece;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Removes photo uploads that were started and never finished, across every studio, with any
 * images that did arrive.
 *
 * <p>Like {@link UnclaimedPieceSweep}, it runs with no tenant bound, so the rows are taken by
 * {@code app.take_abandoned_photo_uploads}, a SECURITY DEFINER function. The images are deleted
 * after that commits; one that fails to delete is only an orphaned blob, logged and left.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.pieces.photo-upload-sweep", matchIfMissing = true)
class PhotoUploadSweep {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadSweep.class);

    static final Duration ABANDONED_AFTER = Duration.ofDays(1);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final BlobStore blobs;

    PhotoUploadSweep(JdbcClient jdbc, TransactionOperations transactions, BlobStore blobs) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.blobs = blobs;
    }

    @Scheduled(initialDelayString = "PT3M", fixedDelayString = "PT1H")
    public void run() {
        runAt(Instant.now());
    }

    /** Removes uploads started more than a day before {@code now}, and returns how many. */
    public int runAt(Instant now) {
        List<Abandoned> taken = transactions.execute(tx -> jdbc
                .sql("select studio_id, piece_id, photo_id from app.take_abandoned_photo_uploads(:before)")
                .param("before", now.minus(ABANDONED_AFTER).atOffset(ZoneOffset.UTC))
                .query((rs, row) -> new Abandoned(rs.getObject("studio_id", UUID.class),
                        rs.getObject("piece_id", UUID.class), rs.getObject("photo_id", UUID.class)))
                .list());
        if (taken == null || taken.isEmpty()) {
            return 0;
        }
        for (Abandoned photo : taken) {
            try {
                blobs.delete(PhotoBlobs.full(photo.studioId(), photo.pieceId(), photo.photoId()));
                blobs.delete(PhotoBlobs.thumbnail(photo.studioId(), photo.pieceId(), photo.photoId()));
            } catch (RuntimeException e) {
                log.warn("Couldn't delete the images of abandoned photo {}: {}", photo.photoId(), e.getMessage());
            }
        }
        log.info("Removed {} abandoned photo uploads", taken.size());
        return taken.size();
    }

    private record Abandoned(UUID studioId, UUID pieceId, UUID photoId) {
    }

}
