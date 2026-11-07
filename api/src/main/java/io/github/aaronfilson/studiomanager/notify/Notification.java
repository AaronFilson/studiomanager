package io.github.aaronfilson.studiomanager.notify;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/** A message to one member in one studio. Hibernate fills {@code studio_id} (ADR 21). */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "notification")
class Notification {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID accountId;

  @Enumerated(EnumType.STRING)
  private NotificationView.Kind kind;

  private String title;

  private String body;

  private Instant createdAt;

  private @Nullable Instant readAt;

  @SuppressWarnings("NullAway.Init")
  protected Notification() {}

  Notification(
      UUID accountId, NotificationView.Kind kind, String title, String body, Instant createdAt) {
    this.id = Uuids.v7();
    this.accountId = accountId;
    this.kind = kind;
    this.title = title;
    this.body = body;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID accountId() {
    return accountId;
  }

  /** Marks it read, unless it already was: the first time it was read is kept. */
  void markRead(Instant now) {
    if (readAt == null) {
      readAt = now;
    }
  }

  NotificationView toView() {
    return new NotificationView(id, kind, title, body, createdAt, readAt);
  }
}
