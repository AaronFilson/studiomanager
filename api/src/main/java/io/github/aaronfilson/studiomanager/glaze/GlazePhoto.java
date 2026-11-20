package io.github.aaronfilson.studiomanager.glaze;

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

/** A test-tile photo attached to a glaze recipe (ADR 12 direct-to-blob upload). */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glaze_photo")
class GlazePhoto {

  enum Status {
    PENDING,
    READY
  }

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID recipeId;

  @Enumerated(EnumType.STRING)
  private Status status;

  private @Nullable Integer byteSize;

  private UUID uploadedBy;

  private Instant createdAt;

  private @Nullable Instant readyAt;

  @SuppressWarnings("NullAway.Init")
  protected GlazePhoto() {}

  GlazePhoto(UUID recipeId, UUID uploadedBy, Instant createdAt) {
    this.id = Uuids.v7();
    this.recipeId = recipeId;
    this.status = Status.PENDING;
    this.uploadedBy = uploadedBy;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID recipeId() {
    return recipeId;
  }

  boolean isReady() {
    return status == Status.READY;
  }

  void markReady(int checkedByteSize, Instant now) {
    this.status = Status.READY;
    this.byteSize = checkedByteSize;
    this.readyAt = now;
  }

  UUID uploadedBy() {
    return uploadedBy;
  }

  Instant readyAt() {
    return Objects.requireNonNull(readyAt, "Only a finished photo has a ready time");
  }
}
