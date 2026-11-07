package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A photo of a piece. PENDING from handing out the upload URLs until both images are checked, then
 * READY. The images themselves are in Blob Storage, named from the ids ({@link PhotoBlobs}).
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "piece_photo")
class PiecePhoto {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID pieceId;

  @Enumerated(EnumType.STRING)
  private PhotoStatus status;

  private @Nullable Integer byteSize;

  private UUID uploadedBy;

  /** When the upload started, from the application's clock: the upload window counts from it. */
  private Instant createdAt;

  private @Nullable Instant readyAt;

  @SuppressWarnings("NullAway.Init")
  protected PiecePhoto() {}

  PiecePhoto(UUID pieceId, UUID uploadedBy, Instant createdAt) {
    this.id = Uuids.v7();
    this.pieceId = pieceId;
    this.status = PhotoStatus.PENDING;
    this.uploadedBy = uploadedBy;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID pieceId() {
    return pieceId;
  }

  boolean isReady() {
    return status == PhotoStatus.READY;
  }

  /** Shows the photo on its piece, once its images have been checked. */
  void markReady(int checkedByteSize, Instant now) {
    this.status = PhotoStatus.READY;
    this.byteSize = checkedByteSize;
    this.readyAt = now;
  }

  UUID uploadedBy() {
    return uploadedBy;
  }

  Instant readyAt() {
    return Objects.requireNonNull(readyAt, "Only a finished photo is shown");
  }
}
