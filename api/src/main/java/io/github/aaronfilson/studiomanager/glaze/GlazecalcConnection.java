package io.github.aaronfilson.studiomanager.glaze;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One member's live OAuth connection to Glazecalc in a studio. Access and refresh tokens are stored
 * AES-256-GCM encrypted via {@code SecretCipher} bound to {@code id}.
 */
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "glazecalc_connection")
class GlazecalcConnection {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID accountId;

  private byte[] accessTokenEnc;

  private byte @Nullable [] refreshTokenEnc;

  private @Nullable Instant expiresAt;

  private Instant connectedAt;

  private @Nullable Instant lastSyncedAt;

  private @Nullable String remoteAccountLabel;

  @SuppressWarnings("NullAway.Init")
  protected GlazecalcConnection() {}

  GlazecalcConnection(
      UUID id,
      UUID accountId,
      byte[] accessTokenEnc,
      byte @Nullable [] refreshTokenEnc,
      @Nullable Instant expiresAt,
      Instant connectedAt,
      @Nullable String remoteAccountLabel) {
    this.id = id;
    this.accountId = accountId;
    this.accessTokenEnc = accessTokenEnc.clone();
    this.refreshTokenEnc = refreshTokenEnc == null ? null : refreshTokenEnc.clone();
    this.expiresAt = expiresAt;
    this.connectedAt = connectedAt;
    this.remoteAccountLabel = remoteAccountLabel;
  }

  UUID id() {
    return id;
  }

  UUID accountId() {
    return accountId;
  }

  byte[] accessTokenEnc() {
    return accessTokenEnc.clone();
  }

  byte @Nullable [] refreshTokenEnc() {
    return refreshTokenEnc == null ? null : refreshTokenEnc.clone();
  }

  @Nullable Instant expiresAt() {
    return expiresAt;
  }

  Instant connectedAt() {
    return connectedAt;
  }

  @Nullable Instant lastSyncedAt() {
    return lastSyncedAt;
  }

  @Nullable String remoteAccountLabel() {
    return remoteAccountLabel;
  }

  void updateTokens(
      byte[] newAccessTokenEnc,
      byte @Nullable [] newRefreshTokenEnc,
      @Nullable Instant newExpiresAt,
      Instant now,
      @Nullable String label) {
    this.accessTokenEnc = newAccessTokenEnc.clone();
    if (newRefreshTokenEnc != null) {
      this.refreshTokenEnc = newRefreshTokenEnc.clone();
    }
    this.expiresAt = newExpiresAt;
    this.connectedAt = now;
    if (label != null && !label.isBlank()) {
      this.remoteAccountLabel = label;
    }
  }

  void markSynced(Instant now, @Nullable String label) {
    this.lastSyncedAt = now;
    if (label != null && !label.isBlank()) {
      this.remoteAccountLabel = label;
    }
  }
}
