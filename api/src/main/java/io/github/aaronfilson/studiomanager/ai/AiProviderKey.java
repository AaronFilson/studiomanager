package io.github.aaronfilson.studiomanager.ai;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.jspecify.annotations.Nullable;

/**
 * The studio's own AI key, one per studio. It is written only by {@link AiKeyQueries#save}, an
 * upsert; here it is read, checked and removed.
 *
 * <p>The studio's id is the primary key, so it is the {@code @Id} rather than a {@code @TenantId}
 * (Hibernate can't make the id the tenant). It is looked up by the bound studio's id, and row-level
 * security still applies underneath (ADR 21).
 */
@Entity
@Table(name = "ai_provider_key")
@DynamicUpdate
class AiProviderKey {

  @Id private UUID studioId;

  @Enumerated(EnumType.STRING)
  private AiKeys.Provider provider;

  private byte[] keyCiphertext;

  private String lastFour;

  @Enumerated(EnumType.STRING)
  private AiKeys.Status status;

  private @Nullable String statusReason;

  private @Nullable Instant checkedAt;

  private boolean allowGoogleFallback;

  private Instant setAt;

  @SuppressWarnings("NullAway.Init")
  protected AiProviderKey() {}

  byte[] keyCiphertext() {
    return keyCiphertext;
  }

  /** Records what a check with the provider found. */
  void checked(AiKeys.Status found, @Nullable String reason, Instant now) {
    this.status = found;
    this.statusReason = reason;
    this.checkedAt = now;
  }

  AiKeys.KeyStatus toStatus() {
    return new AiKeys.KeyStatus(
        provider, lastFour, status, statusReason, checkedAt, allowGoogleFallback, setAt);
  }
}
