package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.storage.UploadedImages;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos of pieces. The API never handles the image bytes: it hands out signed URLs that upload
 * straight to Blob Storage, then checks what arrived before showing it.
 *
 * <p>Anyone who can see a piece can add and remove its photos: staff for any piece, members for
 * their own. Seeing a piece is checked by {@link PieceService#findVisible}, which hides other
 * members' pieces behind a 404.
 */
@Service
public class PiecePhotoService {

  static final int MAX_PHOTOS = 6;
  static final int MAX_PHOTO_BYTES = UploadedImages.MAX_PHOTO_BYTES;
  static final int MAX_THUMBNAIL_BYTES = UploadedImages.MAX_THUMBNAIL_BYTES;
  static final Duration UPLOAD_WINDOW = UploadedImages.UPLOAD_WINDOW;

  private final PiecePhotoRepository photos;
  private final PieceRepository pieceRows;
  private final PieceService pieces;
  private final BlobStore blobs;
  private final EntityManager entityManager;
  private final Clock clock;

  PiecePhotoService(
      PiecePhotoRepository photos,
      PieceRepository pieceRows,
      PieceService pieces,
      BlobStore blobs,
      EntityManager entityManager,
      Clock clock) {
    this.photos = photos;
    this.pieceRows = pieceRows;
    this.pieces = pieces;
    this.blobs = blobs;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  /**
   * Starts a photo: records it as pending and returns where to upload its two images. Uploads
   * started in the last 15 minutes count toward the limit, so a burst can't overshoot it.
   */
  @Transactional
  public PhotoUpload startUpload(UUID pieceId) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.requireStudioId();
    pieces.findVisible(pieceId);
    lockPiece(pieceId);
    Instant now = clock.instant();
    if (photos.countTaken(pieceId, now.minus(UPLOAD_WINDOW)) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    // Persisted, not saved: a photo has no version, so save would look it up first
    PiecePhoto photo = new PiecePhoto(pieceId, caller.accountId(), now);
    entityManager.persist(photo);
    Instant expiresAt = now.plus(UPLOAD_WINDOW);
    return new PhotoUpload(
        photo.id(),
        blobs.uploadUrl(PhotoBlobs.full(studioId, pieceId, photo.id()), expiresAt),
        blobs.uploadUrl(PhotoBlobs.thumbnail(studioId, pieceId, photo.id()), expiresAt),
        expiresAt,
        MAX_PHOTO_BYTES,
        MAX_THUMBNAIL_BYTES);
  }

  /**
   * Finishes a photo once both images are uploaded: checks they are JPEGs within the limits and
   * shows the photo on the piece. Finishing a photo twice changes nothing.
   */
  @Transactional
  public PieceView complete(UUID pieceId, UUID photoId) {
    UUID studioId = TenantContext.require().requireStudioId();
    pieces.findVisible(pieceId);
    lockPiece(pieceId);
    PiecePhoto photo = find(pieceId, photoId);
    if (photo.isReady()) {
      return pieces.get(pieceId);
    }
    long size =
        UploadedImages.check(
            blobs, PhotoBlobs.full(studioId, pieceId, photoId), MAX_PHOTO_BYTES, "photo");
    UploadedImages.check(
        blobs, PhotoBlobs.thumbnail(studioId, pieceId, photoId), MAX_THUMBNAIL_BYTES, "thumbnail");
    if (photos.countReady(pieceId) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    photo.markReady(Math.toIntExact(size), clock.instant());
    photos.flush();
    return pieces.get(pieceId);
  }

  /** Removes a photo, finished or not, and its images. */
  @Transactional
  public void delete(UUID pieceId, UUID photoId) {
    UUID studioId = TenantContext.require().requireStudioId();
    pieces.findVisible(pieceId);
    photos.delete(find(pieceId, photoId));
    // Written before the images go: if storage fails, the transaction rolls the row back and the
    // delete can be retried
    photos.flush();
    blobs.delete(PhotoBlobs.full(studioId, pieceId, photoId));
    blobs.delete(PhotoBlobs.thumbnail(studioId, pieceId, photoId));
  }

  /** Where to upload a new photo's two images, and the limits they must meet. */
  public record PhotoUpload(
      UUID photoId,
      URI photoUploadUrl,
      URI thumbnailUploadUrl,
      Instant expiresAt,
      int maxPhotoBytes,
      int maxThumbnailBytes) {}

  private PiecePhoto find(UUID pieceId, UUID photoId) {
    return photos
        .findByIdAndPieceId(photoId, pieceId)
        .orElseThrow(() -> new NotFoundException("No such photo."));
  }

  /**
   * Serializes photo changes on one piece, so two uploads at once can't both take the last place.
   */
  private void lockPiece(UUID pieceId) {
    pieceRows.findAndLock(pieceId).orElseThrow(() -> new NotFoundException("No such piece."));
  }

  private static String tooMany() {
    return "A piece can have at most %d photos. Remove one to add another.".formatted(MAX_PHOTOS);
  }
}
