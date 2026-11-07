package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.storage.UploadedImages;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  private final RecordPhotoRepository photos;
  private final MaintenanceRecordRepository records;
  private final MaintenanceQueries queries;
  private final BlobStore blobs;
  private final Clock clock;

  RecordPhotoService(
      RecordPhotoRepository photos,
      MaintenanceRecordRepository records,
      MaintenanceQueries queries,
      BlobStore blobs,
      Clock clock) {
    this.photos = photos;
    this.records = records;
    this.queries = queries;
    this.blobs = blobs;
    this.clock = clock;
  }

  /** Starts a photo on a record and returns where to upload its two images. */
  @Transactional
  public Upload startUpload(UUID recordId) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.requireStudioId();
    lockRecord(recordId);
    Instant now = clock.instant();
    if (photos.countTaken(recordId, now.minus(UploadedImages.UPLOAD_WINDOW)) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    RecordPhoto photo = new RecordPhoto(recordId, caller.accountId());
    photos.saveAndFlush(photo);
    UUID photoId = photo.getId();
    Instant expiresAt = now.plus(UploadedImages.UPLOAD_WINDOW);
    return new Upload(
        photoId,
        blobs.uploadUrl(full(studioId, recordId, photoId), expiresAt),
        blobs.uploadUrl(thumbnail(studioId, recordId, photoId), expiresAt),
        expiresAt,
        UploadedImages.MAX_PHOTO_BYTES,
        UploadedImages.MAX_THUMBNAIL_BYTES);
  }

  /** Checks both images arrived as JPEGs within the limits, and shows the photo. Idempotent. */
  @Transactional
  public void complete(UUID recordId, UUID photoId) {
    UUID studioId = TenantContext.require().requireStudioId();
    lockRecord(recordId);
    RecordPhoto photo =
        photos
            .findByIdAndRecordId(photoId, recordId)
            .orElseThrow(() -> new NotFoundException("No such photo."));
    if (photo.isReady()) {
      return;
    }
    long size =
        UploadedImages.check(
            blobs, full(studioId, recordId, photoId), UploadedImages.MAX_PHOTO_BYTES, "photo");
    UploadedImages.check(
        blobs,
        thumbnail(studioId, recordId, photoId),
        UploadedImages.MAX_THUMBNAIL_BYTES,
        "thumbnail");
    if (photos.countReady(recordId) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    photo.markReady(size, clock.instant());
    photos.flush();
  }

  /** The finished photos of each of an asset's records, oldest first, with read URLs. */
  @Transactional(readOnly = true)
  public Map<UUID, List<Photo>> photosOf(UUID assetId) {
    UUID studioId = TenantContext.require().requireStudioId();
    Map<UUID, List<Photo>> byRecord = new HashMap<>();
    for (RecordPhoto photo : photos.findReadyForAsset(assetId)) {
      UUID recordId = photo.recordId();
      UUID photoId = photo.getId();
      byRecord
          .computeIfAbsent(recordId, id -> new ArrayList<>())
          .add(
              new Photo(
                  photoId,
                  blobs.readUrl(full(studioId, recordId, photoId), UploadedImages.CONTENT_TYPE),
                  blobs.readUrl(
                      thumbnail(studioId, recordId, photoId), UploadedImages.CONTENT_TYPE),
                  photo.uploadedBy(),
                  photo.readyAt()));
    }
    return byRecord;
  }

  public record Upload(
      UUID photoId,
      URI photoUploadUrl,
      URI thumbnailUploadUrl,
      Instant expiresAt,
      int maxPhotoBytes,
      int maxThumbnailBytes) {}

  public record Photo(UUID id, URI url, URI thumbnailUrl, UUID uploadedBy, Instant uploadedAt) {}

  // Names come from ids, never from a client, under a prefix of their own
  static String full(UUID studioId, UUID recordId, UUID photoId) {
    return "maintenance/%s/%s/%s/full.jpg".formatted(studioId, recordId, photoId);
  }

  static String thumbnail(UUID studioId, UUID recordId, UUID photoId) {
    return "maintenance/%s/%s/%s/thumb.jpg".formatted(studioId, recordId, photoId);
  }

  /** Finds the record in this studio, or not at all, and serializes photo changes on it. */
  private void lockRecord(UUID recordId) {
    if (!records.existsById(recordId)) {
      throw new NotFoundException("No such record.");
    }
    queries.lockRecord(recordId);
  }

  private static String tooMany() {
    return "A record can have at most %d photos.".formatted(MAX_PHOTOS);
  }
}
