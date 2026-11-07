package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * An invite into the bound studio. Only the hash of its code is kept. Accepting one goes through
 * {@code app.accept_invite}, because the invitee isn't a member yet (ADR 10).
 *
 * <p>The table has no version column: an invite is created, resent or withdrawn, never edited.
 */
@Entity
@Table(name = "studio_invite")
class StudioInvite {

  static final Duration LIFETIME = Duration.ofDays(14);

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private byte[] codeHash;

  @Enumerated(EnumType.STRING)
  private StudioRole role;

  private UUID createdBy;

  private Instant createdAt;

  private Instant expiresAt;

  private @Nullable UUID acceptedBy;

  private @Nullable Instant acceptedAt;

  private @Nullable String email;

  private @Nullable Instant lastSentAt;

  @SuppressWarnings("NullAway.Init")
  protected StudioInvite() {}

  /** A new invite, open for {@link #LIFETIME} from {@code now}; {@code email} null for a link. */
  StudioInvite(
      String code, StudioRole role, UUID createdBy, @Nullable String email, Instant now) {
    this.id = Uuids.v7();
    this.codeHash = InviteCodes.sha256(code);
    this.role = role;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.expiresAt = now.plus(LIFETIME);
    this.email = email;
  }

  UUID id() {
    return id;
  }

  StudioRole role() {
    return role;
  }

  @Nullable String email() {
    return email;
  }

  Instant expiresAt() {
    return expiresAt;
  }

  /** Replaces the code with a new one, open for a fresh {@link #LIFETIME}, as its email goes out. */
  void resend(String code, Instant now) {
    this.codeHash = InviteCodes.sha256(code);
    this.expiresAt = now.plus(LIFETIME);
    this.lastSentAt = now;
  }

  OpenInviteView toOpenView() {
    return new OpenInviteView(id, email, role, createdAt, expiresAt, lastSentAt);
  }
}
