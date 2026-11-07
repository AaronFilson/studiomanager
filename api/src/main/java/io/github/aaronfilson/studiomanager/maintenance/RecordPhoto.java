package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A photo on a maintenance record: pending while its images upload, then ready. The table has no
 * version (a photo only ever goes from pending to ready, under the record's lock), so {@link
 * Persistable} tells Spring Data a new one apart from a loaded one.
 */
@Entity
@Table(name = "record_photo")
@EntityListeners(AuditingEntityListener.class)
class RecordPhoto implements Persistable<UUID> {

  enum Status {
    PENDING,
    READY
  }

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID recordId;

  @Enumerated(EnumType.STRING)
  private Status status;

  private @Nullable Integer byteSize;

  private UUID uploadedBy;

  @CreatedDate private @Nullable Instant createdAt;

  private @Nullable Instant readyAt;

  @Transient private boolean isNew = true;

  @SuppressWarnings("NullAway.Init")
  protected RecordPhoto() {}

  RecordPhoto(UUID recordId, UUID uploadedBy) {
    this.id = Uuids.v7();
    this.recordId = recordId;
    this.status = Status.PENDING;
    this.uploadedBy = uploadedBy;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markStored() {
    this.isNew = false;
  }

  UUID recordId() {
    return recordId;
  }

  boolean isReady() {
    return status == Status.READY;
  }

  UUID uploadedBy() {
    return uploadedBy;
  }

  Instant readyAt() {
    return Objects.requireNonNull(readyAt, "A ready photo has a time it became ready");
  }

  /** Shows the photo, once both its images have been checked. */
  void markReady(long size, Instant now) {
    this.status = Status.READY;
    this.byteSize = Math.toIntExact(size);
    this.readyAt = now;
  }
}
