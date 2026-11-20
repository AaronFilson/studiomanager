package io.github.aaronfilson.studiomanager.glaze;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.storage.UploadedImages;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test-tile photos on glaze recipes, uploaded straight to Blob Storage with SAS URLs (ADR 12).
 * Staff manage photos on studio glazes; members manage photos on their own recipes.
 */
@Service
public class GlazePhotoService {

  static final int MAX_PHOTOS = 6;

  private final GlazePhotoRepository photos;
  private final GlazeBook book;
  private final GlazeQueries queries;
  private final BlobStore blobs;
  private final EntityManager entityManager;
  private final Clock clock;

  GlazePhotoService(
      GlazePhotoRepository photos,
      GlazeBook book,
      GlazeQueries queries,
      BlobStore blobs,
      EntityManager entityManager,
      Clock clock) {
    this.photos = photos;
    this.book = book;
    this.queries = queries;
    this.blobs = blobs;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  public record PhotoUpload(
      UUID photoId,
      URI photoUploadUrl,
      URI thumbnailUploadUrl,
      Instant expiresAt,
      int maxPhotoBytes,
      int maxThumbnailBytes) {}

  @Transactional
  public PhotoUpload startUpload(UUID glazeId) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.requireStudioId();
    GlazeRecipe recipe = book.find(glazeId);
    GlazeEdits.requireCanEdit(caller, recipe);
    queries.lockRecipe(glazeId);
    Instant now = clock.instant();
    if (photos.countTaken(glazeId, now.minus(UploadedImages.UPLOAD_WINDOW)) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    GlazePhoto photo = new GlazePhoto(glazeId, caller.accountId(), now);
    entityManager.persist(photo);
    Instant expiresAt = now.plus(UploadedImages.UPLOAD_WINDOW);
    return new PhotoUpload(
        photo.id(),
        blobs.uploadUrl(full(studioId, glazeId, photo.id()), expiresAt),
        blobs.uploadUrl(thumbnail(studioId, glazeId, photo.id()), expiresAt),
        expiresAt,
        UploadedImages.MAX_PHOTO_BYTES,
        UploadedImages.MAX_THUMBNAIL_BYTES);
  }

  @Transactional
  public GlazeBook.Recipe complete(UUID glazeId, UUID photoId) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.requireStudioId();
    GlazeRecipe recipe = book.find(glazeId);
    GlazeEdits.requireCanEdit(caller, recipe);
    queries.lockRecipe(glazeId);
    GlazePhoto photo = find(glazeId, photoId);
    if (photo.isReady()) {
      return book.view(recipe, caller);
    }
    long size =
        UploadedImages.check(
            blobs, full(studioId, glazeId, photoId), UploadedImages.MAX_PHOTO_BYTES, "photo");
    UploadedImages.check(
        blobs,
        thumbnail(studioId, glazeId, photoId),
        UploadedImages.MAX_THUMBNAIL_BYTES,
        "thumbnail");
    if (photos.countReady(glazeId) >= MAX_PHOTOS) {
      throw new ConflictException(tooMany());
    }
    photo.markReady(Math.toIntExact(size), clock.instant());
    photos.flush();
    return book.view(recipe, caller);
  }

  @Transactional
  public void delete(UUID glazeId, UUID photoId) {
    TenantContext caller = TenantContext.require();
    UUID studioId = caller.requireStudioId();
    GlazeRecipe recipe = book.find(glazeId);
    GlazeEdits.requireCanEdit(caller, recipe);
    photos.delete(find(glazeId, photoId));
    photos.flush();
    blobs.delete(full(studioId, glazeId, photoId));
    blobs.delete(thumbnail(studioId, glazeId, photoId));
  }

  static String full(UUID studioId, UUID recipeId, UUID photoId) {
    return "glazes/%s/%s/%s/full.jpg".formatted(studioId, recipeId, photoId);
  }

  static String thumbnail(UUID studioId, UUID recipeId, UUID photoId) {
    return "glazes/%s/%s/%s/thumb.jpg".formatted(studioId, recipeId, photoId);
  }

  private GlazePhoto find(UUID glazeId, UUID photoId) {
    return photos
        .findByIdAndRecipeId(photoId, glazeId)
        .orElseThrow(() -> new NotFoundException("No such photo."));
  }

  private static String tooMany() {
    return "A glaze recipe can have at most %d photos. Remove one to add another."
        .formatted(MAX_PHOTOS);
  }
}
