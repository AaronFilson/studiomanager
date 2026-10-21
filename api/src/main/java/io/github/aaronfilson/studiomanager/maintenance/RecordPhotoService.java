package io.github.aaronfilson.studiomanager.maintenance;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.storage.UploadedImages;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos on maintenance records, uploaded straight to storage like piece photos (ADR 12). Records
 * are insert-only, and so are their photos: they're evidence for an insurer, so there's no delete.
 * Staff add them, from the asset's history.
 */
@Service
public class RecordPhotoService {

    static final int MAX_PHOTOS = 4;

    private final JdbcClient jdbc;
    private final BlobStore blobs;

    RecordPhotoService(JdbcClient jdbc, BlobStore blobs) {
        this.jdbc = jdbc;
        this.blobs = blobs;
    }

    /** Starts a photo on a record and returns where to upload its two images. */
    @Transactional
    public Upload startUpload(UUID recordId) {
        TenantContext caller = TenantContext.require();
        lockRecord(recordId);
        Instant now = now();
        int taken = jdbc.sql("""
                select count(*) from record_photo
                where record_id = :recordId and (status = 'READY' or created_at > :windowStart)
                """)
                .param("recordId", recordId)
                .param("windowStart", utc(now.minus(UploadedImages.UPLOAD_WINDOW)))
                .query(Integer.class).single();
        if (taken >= MAX_PHOTOS) {
            throw new ConflictException(tooMany());
        }
        UUID photoId = Uuids.v7();
        jdbc.sql("""
                insert into record_photo (id, studio_id, record_id, status, uploaded_by, created_at)
                values (:id, :studioId, :recordId, 'PENDING', :by, :now)
                """)
                .param("id", photoId)
                .param("studioId", caller.studioId())
                .param("recordId", recordId)
                .param("by", caller.accountId())
                .param("now", utc(now))
                .update();
        Instant expiresAt = now.plus(UploadedImages.UPLOAD_WINDOW);
        return new Upload(photoId, blobs.uploadUrl(full(caller.studioId(), recordId, photoId), expiresAt),
                blobs.uploadUrl(thumbnail(caller.studioId(), recordId, photoId), expiresAt), expiresAt,
                UploadedImages.MAX_PHOTO_BYTES, UploadedImages.MAX_THUMBNAIL_BYTES);
    }

    /** Checks both images arrived as JPEGs within the limits, and shows the photo. Idempotent. */
    @Transactional
    public void complete(UUID recordId, UUID photoId) {
        UUID studioId = TenantContext.require().studioId();
        lockRecord(recordId);
        String status = jdbc.sql("select status from record_photo where id = :id and record_id = :recordId")
                .param("id", photoId)
                .param("recordId", recordId)
                .query(String.class).optional()
                .orElseThrow(() -> new NotFoundException("No such photo."));
        if (status.equals("READY")) {
            return;
        }
        long size = UploadedImages.check(blobs, full(studioId, recordId, photoId), UploadedImages.MAX_PHOTO_BYTES,
                "photo");
        UploadedImages.check(blobs, thumbnail(studioId, recordId, photoId), UploadedImages.MAX_THUMBNAIL_BYTES,
                "thumbnail");
        int ready = jdbc.sql("select count(*) from record_photo where record_id = :recordId and status = 'READY'")
                .param("recordId", recordId)
                .query(Integer.class).single();
        if (ready >= MAX_PHOTOS) {
            throw new ConflictException(tooMany());
        }
        jdbc.sql("update record_photo set status = 'READY', byte_size = :size, ready_at = :now where id = :id")
                .param("size", size)
                .param("now", utc(now()))
                .param("id", photoId)
                .update();
    }

    /** The finished photos of each of an asset's records, oldest first, with read URLs. */
    @Transactional(readOnly = true)
    public Map<UUID, List<Photo>> photosOf(UUID assetId) {
        UUID studioId = TenantContext.require().studioId();
        return jdbc.sql("""
                select p.id, p.record_id, p.uploaded_by, p.ready_at
                from record_photo p join maintenance_record r on r.id = p.record_id
                where r.asset_id = :assetId and p.status = 'READY'
                order by p.created_at
                """)
                .param("assetId", assetId)
                .query((rs, n) -> {
                    UUID recordId = rs.getObject("record_id", UUID.class);
                    UUID photoId = rs.getObject("id", UUID.class);
                    return Map.entry(recordId, new Photo(photoId,
                            blobs.readUrl(full(studioId, recordId, photoId), UploadedImages.CONTENT_TYPE),
                            blobs.readUrl(thumbnail(studioId, recordId, photoId), UploadedImages.CONTENT_TYPE),
                            rs.getObject("uploaded_by", UUID.class),
                            rs.getObject("ready_at", OffsetDateTime.class).toInstant()));
                })
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public record Upload(UUID photoId, URI photoUploadUrl, URI thumbnailUploadUrl, Instant expiresAt,
            int maxPhotoBytes, int maxThumbnailBytes) {
    }

    public record Photo(UUID id, URI url, URI thumbnailUrl, UUID uploadedBy, Instant uploadedAt) {
    }

    // Names come from ids, never from a client, under a prefix of their own
    static String full(UUID studioId, UUID recordId, UUID photoId) {
        return "maintenance/%s/%s/%s/full.jpg".formatted(studioId, recordId, photoId);
    }

    static String thumbnail(UUID studioId, UUID recordId, UUID photoId) {
        return "maintenance/%s/%s/%s/thumb.jpg".formatted(studioId, recordId, photoId);
    }

    /**
     * Finds the record in this studio, or not at all, and serializes photo changes on it. Records
     * are insert-only, so the app can't take a row lock on one (FOR UPDATE needs UPDATE); a
     * transaction-scoped advisory lock on its id does the same job.
     */
    private void lockRecord(UUID recordId) {
        jdbc.sql("select id from maintenance_record where id = :id")
                .param("id", recordId)
                .query(UUID.class).optional()
                .orElseThrow(() -> new NotFoundException("No such record."));
        jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:id, 0))")
                .param("id", recordId.toString())
                .query((rs, n) -> 0).single();
    }

    private static String tooMany() {
        return "A record can have at most %d photos.".formatted(MAX_PHOTOS);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
