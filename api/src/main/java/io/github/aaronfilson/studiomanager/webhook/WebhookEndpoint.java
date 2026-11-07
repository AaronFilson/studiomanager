package io.github.aaronfilson.studiomanager.webhook;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Where a studio's webhooks go. Hibernate fills {@code studio_id} and auditing fills {@code
 * created_by} and {@code created_at} (ADR 21).
 *
 * <p>The table has no version column. Staff changes and the sender's bookkeeping touch different
 * columns ({@link WebhookQueries} counts failures in SQL), so updates write only the columns that
 * changed ({@code DynamicUpdate}) and neither overwrites the other.
 */
@Entity
@Table(name = "webhook_endpoint")
@EntityListeners(AuditingEntityListener.class)
@DynamicUpdate
class WebhookEndpoint {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String url;

  private @Nullable String description;

  private String[] eventTypes;

  private boolean enabled;

  /** The signing secret, AES-GCM encrypted. Never stored in the clear. */
  private byte[] secret;

  private byte @Nullable [] previousSecret;

  private @Nullable Instant previousSecretUntil;

  private int consecutiveFailures;

  private @Nullable Instant failingSince;

  private @Nullable String disabledReason;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected WebhookEndpoint() {}

  /**
   * A new, enabled endpoint. The id is chosen before the secret is encrypted, since the secret is
   * bound to it.
   */
  WebhookEndpoint(
      UUID id, String url, @Nullable String description, List<String> eventTypes, byte[] secret) {
    this.id = id;
    this.url = url;
    this.description = description;
    this.eventTypes = eventTypes.toArray(String[]::new);
    this.enabled = true;
    this.secret = secret;
  }

  UUID id() {
    return id;
  }

  String url() {
    return url;
  }

  @Nullable String description() {
    return description;
  }

  List<String> eventTypes() {
    return List.of(eventTypes);
  }

  boolean enabled() {
    return enabled;
  }

  byte[] secret() {
    return secret;
  }

  /** Whether it should get events of this type: on, and subscribed to it. */
  boolean wants(WebhookEventType type) {
    return enabled && Arrays.asList(eventTypes).contains(type.wireName());
  }

  /** Changes what's given. Turning it back on clears its failures and why it was turned off. */
  void change(
      String newUrl,
      @Nullable String newDescription,
      List<String> newEventTypes,
      boolean nowEnabled) {
    boolean reenabled = nowEnabled && !enabled;
    this.url = newUrl;
    this.description = newDescription;
    this.eventTypes = newEventTypes.toArray(String[]::new);
    this.enabled = nowEnabled;
    if (reenabled) {
      this.consecutiveFailures = 0;
      this.failingSince = null;
    }
    if (nowEnabled) {
      this.disabledReason = null;
    }
  }

  /** A new secret; the old one keeps signing alongside it until {@code previousValidUntil}. */
  void rotateSecret(byte[] newSecret, Instant previousValidUntil) {
    this.previousSecret = secret;
    this.previousSecretUntil = previousValidUntil;
    this.secret = newSecret;
  }

  WebhookEndpoints.EndpointView toView(Instant now) {
    Instant previousUntil = previousSecretUntil;
    return new WebhookEndpoints.EndpointView(
        id,
        url,
        description,
        Arrays.stream(eventTypes)
            .map(WebhookEventType::fromWireName)
            .flatMap(Optional::stream)
            .toList(),
        enabled,
        disabledReason,
        consecutiveFailures,
        previousUntil != null && previousUntil.isAfter(now) ? previousUntil : null,
        Objects.requireNonNull(createdAt, "An endpoint has a creation time once it's saved"));
  }
}
