package io.github.aaronfilson.studiomanager.webhook;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One try at a delivery, for the delivery log. Written by {@link WebhookQueries#recordAttempt}, in
 * the same step that releases the delivery's claim.
 */
@Entity
@Immutable
@Table(name = "webhook_attempt")
class WebhookAttempt {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID deliveryId;

  private Instant attemptedAt;

  private @Nullable Integer statusCode;

  private @Nullable String error;

  private int durationMs;

  private @Nullable String responseExcerpt;

  @SuppressWarnings("NullAway.Init")
  protected WebhookAttempt() {}

  UUID deliveryId() {
    return deliveryId;
  }

  WebhookEndpoints.AttemptView toView() {
    return new WebhookEndpoints.AttemptView(
        attemptedAt, statusCode, error, durationMs, responseExcerpt);
  }
}
