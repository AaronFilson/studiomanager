package io.github.aaronfilson.studiomanager.maintenance;

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
 * Removes record photo uploads that were started and never finished, in every studio, with any
 * images that arrived. The same as the piece module's sweep, through its own SECURITY DEFINER
 * function, {@code app.take_abandoned_record_photo_uploads} (ADR 10).
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.maintenance.photo-upload-sweep", matchIfMissing = true)
class RecordPhotoUploadSweep {

    private static final Logger log = LoggerFactory.getLogger(RecordPhotoUploadSweep.class);

    static final Duration ABANDONED_AFTER = Duration.ofDays(1);

    private final JdbcClient jdbc;
    private final TransactionOperations transactions;
    private final BlobStore blobs;

    RecordPhotoUploadSweep(JdbcClient jdbc, TransactionOperations transactions, BlobStore blobs) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.blobs = blobs;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void run() {
        runAt(Instant.now());
    }

    /** Removes uploads started more than a day before {@code now}, and returns how many. */
    public int runAt(Instant now) {
        List<Abandoned> taken = transactions.execute(tx -> jdbc
                .sql("select studio_id, record_id, photo_id from app.take_abandoned_record_photo_uploads(:before)")
                .param("before", now.minus(ABANDONED_AFTER).atOffset(ZoneOffset.UTC))
                .query((rs, row) -> new Abandoned(rs.getObject("studio_id", UUID.class),
                        rs.getObject("record_id", UUID.class), rs.getObject("photo_id", UUID.class)))
                .list());
        if (taken == null || taken.isEmpty()) {
            return 0;
        }
        for (Abandoned photo : taken) {
            try {
                blobs.delete(RecordPhotoService.full(photo.studioId(), photo.recordId(), photo.photoId()));
                blobs.delete(RecordPhotoService.thumbnail(photo.studioId(), photo.recordId(), photo.photoId()));
            } catch (RuntimeException e) {
                log.warn("Couldn't delete the images of abandoned record photo {}: {}", photo.photoId(), e.getMessage());
            }
        }
        log.info("Removed {} abandoned record photo uploads", taken.size());
        return taken.size();
    }

    private record Abandoned(UUID studioId, UUID recordId, UUID photoId) {
    }

}
