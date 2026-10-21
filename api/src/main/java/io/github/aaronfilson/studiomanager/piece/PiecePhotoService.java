package io.github.aaronfilson.studiomanager.piece;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos of pieces. The API never handles the image bytes: it hands out signed URLs that upload
 * straight to Blob Storage, then checks what arrived before showing it.
 *
 * <p>Anyone who can see a piece can add and remove its photos: staff for any piece, members for
 * their own. Seeing a piece is checked by {@link PieceService#get}, which hides other members'
 * pieces behind a 404.
 */
@Service
public class PiecePhotoService {

    static final int MAX_PHOTOS = 6;
    static final int MAX_PHOTO_BYTES = 8 * 1024 * 1024;
    static final int MAX_THUMBNAIL_BYTES = 512 * 1024;
    static final Duration UPLOAD_WINDOW = Duration.ofMinutes(15);

    /** Every JPEG starts with a start-of-image marker followed by another marker. */
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    private final JdbcClient jdbc;
    private final PieceService pieces;
    private final BlobStore blobs;

    PiecePhotoService(JdbcClient jdbc, PieceService pieces, BlobStore blobs) {
        this.jdbc = jdbc;
        this.pieces = pieces;
        this.blobs = blobs;
    }

    /**
     * Starts a photo: records it as pending and returns where to upload its two images. Uploads
     * started in the last 15 minutes count toward the limit, so a burst can't overshoot it.
     */
    @Transactional
    public PhotoUpload startUpload(UUID pieceId) {
        TenantContext caller = TenantContext.require();
        pieces.get(pieceId);
        lockPiece(pieceId);
        Instant now = now();
        int taken = jdbc.sql("""
                select count(*) from piece_photo
                where piece_id = :pieceId and (status = 'READY' or created_at > :windowStart)
                """)
                .param("pieceId", pieceId)
                .param("windowStart", now.minus(UPLOAD_WINDOW).atOffset(ZoneOffset.UTC))
                .query(Integer.class).single();
        if (taken >= MAX_PHOTOS) {
            throw new ConflictException(tooMany());
        }
        UUID photoId = Uuids.v7();
        jdbc.sql("""
                insert into piece_photo (id, studio_id, piece_id, status, uploaded_by, created_at)
                values (:id, :studioId, :pieceId, 'PENDING', :uploadedBy, :now)
                """)
                .param("id", photoId)
                .param("studioId", caller.studioId())
                .param("pieceId", pieceId)
                .param("uploadedBy", caller.accountId())
                .param("now", now.atOffset(ZoneOffset.UTC))
                .update();
        Instant expiresAt = now.plus(UPLOAD_WINDOW);
        return new PhotoUpload(photoId,
                blobs.uploadUrl(PhotoBlobs.full(caller.studioId(), pieceId, photoId), expiresAt),
                blobs.uploadUrl(PhotoBlobs.thumbnail(caller.studioId(), pieceId, photoId), expiresAt),
                expiresAt, MAX_PHOTO_BYTES, MAX_THUMBNAIL_BYTES);
    }

    /**
     * Finishes a photo once both images are uploaded: checks they are JPEGs within the limits and
     * shows the photo on the piece. Finishing a photo twice changes nothing.
     */
    @Transactional
    public PieceView complete(UUID pieceId, UUID photoId) {
        UUID studioId = TenantContext.require().studioId();
        pieces.get(pieceId);
        lockPiece(pieceId);
        String status = jdbc.sql("select status from piece_photo where id = :id and piece_id = :pieceId")
                .param("id", photoId)
                .param("pieceId", pieceId)
                .query(String.class).optional()
                .orElseThrow(() -> new NotFoundException("No such photo."));
        if (status.equals("READY")) {
            return pieces.get(pieceId);
        }
        long size = check(PhotoBlobs.full(studioId, pieceId, photoId), MAX_PHOTO_BYTES, "photo");
        check(PhotoBlobs.thumbnail(studioId, pieceId, photoId), MAX_THUMBNAIL_BYTES, "thumbnail");
        int ready = jdbc.sql("select count(*) from piece_photo where piece_id = :pieceId and status = 'READY'")
                .param("pieceId", pieceId)
                .query(Integer.class).single();
        if (ready >= MAX_PHOTOS) {
            throw new ConflictException(tooMany());
        }
        jdbc.sql("""
                update piece_photo set status = 'READY', byte_size = :size, ready_at = :now
                where id = :id and status = 'PENDING'
                """)
                .param("size", size)
                .param("now", now().atOffset(ZoneOffset.UTC))
                .param("id", photoId)
                .update();
        return pieces.get(pieceId);
    }

    /** Removes a photo, finished or not, and its images. */
    @Transactional
    public void delete(UUID pieceId, UUID photoId) {
        UUID studioId = TenantContext.require().studioId();
        pieces.get(pieceId);
        int deleted = jdbc.sql("delete from piece_photo where id = :id and piece_id = :pieceId")
                .param("id", photoId)
                .param("pieceId", pieceId)
                .update();
        if (deleted == 0) {
            throw new NotFoundException("No such photo.");
        }
        // Inside the transaction: if storage fails, the row comes back and the delete can be retried
        blobs.delete(PhotoBlobs.full(studioId, pieceId, photoId));
        blobs.delete(PhotoBlobs.thumbnail(studioId, pieceId, photoId));
    }

    /** Where to upload a new photo's two images, and the limits they must meet. */
    public record PhotoUpload(UUID photoId, URI photoUploadUrl, URI thumbnailUploadUrl, Instant expiresAt,
            int maxPhotoBytes, int maxThumbnailBytes) {
    }

    private long check(String blob, int maxBytes, String what) {
        BlobStore.BlobInfo info = blobs.info(blob)
                .orElseThrow(() -> new InvalidInputException("The %s hasn't been uploaded yet.".formatted(what)));
        if (info.size() > maxBytes) {
            throw new InvalidInputException("The %s is larger than %d KB. Shrink it and try again."
                    .formatted(what, maxBytes / 1024));
        }
        if (info.size() < JPEG_MAGIC.length || !Arrays.equals(blobs.head(blob, JPEG_MAGIC.length), JPEG_MAGIC)) {
            throw new InvalidInputException("The %s isn't a JPEG image.".formatted(what));
        }
        return info.size();
    }

    /** Serializes photo changes on one piece, so two uploads at once can't both take the last place. */
    private void lockPiece(UUID pieceId) {
        jdbc.sql("select id from piece where id = :id for update")
                .param("id", pieceId)
                .query(UUID.class).single();
    }

    private static String tooMany() {
        return "A piece can have at most %d photos. Remove one to add another.".formatted(MAX_PHOTOS);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
