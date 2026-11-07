package io.github.aaronfilson.studiomanager.webhook;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One event on its way to one endpoint. Created here; after that, claiming it and recording each
 * attempt is {@link WebhookQueries}' job, an atomic test-and-set that is safe across replicas.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "webhook_delivery")
class WebhookDelivery {

  enum Status {
    PENDING,
    DELIVERED,
    FAILED
  }

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID endpointId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private WebhookEvent event;

  /** Read-only here: {@code event_id} is written through {@link #event}. */
  @Column(name = "event_id", insertable = false, updatable = false)
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  private Status status;

  private int attempts;

  private @Nullable Instant nextAttemptAt;

  private @Nullable Instant deliveredAt;

  private Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected WebhookDelivery() {}

  /**
   * A delivery whose first attempt is due at {@code firstAttemptAt}. {@code event} may be a
   * reference that isn't loaded; its id is {@code eventId}.
   */
  WebhookDelivery(
      UUID endpointId,
      UUID eventId,
      WebhookEvent event,
      Instant createdAt,
      Instant firstAttemptAt) {
    this.id = Uuids.v7();
    this.endpointId = endpointId;
    this.event = event;
    this.eventId = eventId;
    this.status = Status.PENDING;
    this.createdAt = createdAt;
    this.nextAttemptAt = firstAttemptAt;
  }

  UUID id() {
    return id;
  }

  UUID endpointId() {
    return endpointId;
  }

  /** The event's id, without loading the event. */
  UUID eventId() {
    return eventId;
  }

  /** The delivery as its log shows it. Load it with its event fetched, so this adds no query. */
  WebhookEndpoints.DeliveryView toView(List<WebhookEndpoints.AttemptView> attemptLog) {
    return new WebhookEndpoints.DeliveryView(
        id,
        WebhookEvents.wireId(eventId),
        event.type(),
        status.name(),
        attempts,
        createdAt,
        nextAttemptAt,
        deliveredAt,
        attemptLog);
  }
}
