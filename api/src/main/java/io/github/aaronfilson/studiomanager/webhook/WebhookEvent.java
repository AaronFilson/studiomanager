package io.github.aaronfilson.studiomanager.webhook;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * Something that happened, as receivers are sent it. Read here for the delivery log; written only
 * by {@link WebhookQueries#insertEvent}, whose {@code on conflict do nothing} is what makes a
 * listener that runs twice add nothing.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Immutable
@Table(name = "webhook_event")
class WebhookEvent {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String type;

  @SuppressWarnings("NullAway.Init")
  protected WebhookEvent() {}

  UUID id() {
    return id;
  }

  WebhookEventType type() {
    return WebhookEventType.fromWireName(type)
        .orElseThrow(() -> new IllegalStateException("A stored event has a known type"));
  }
}
