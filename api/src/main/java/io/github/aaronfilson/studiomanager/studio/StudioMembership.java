package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One person's membership, and role, in one studio.
 *
 * <p>No {@code @TenantId}: "my studios" reads a person's memberships in every studio with none
 * bound (ADR 21). So queries in one studio name the studio themselves.
 */
@Entity
@Table(name = "studio_membership")
class StudioMembership {

  @Id private UUID id;

  private UUID studioId;

  private UUID accountId;

  @Enumerated(EnumType.STRING)
  private StudioRole role;

  private Instant createdAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected StudioMembership() {}

  StudioMembership(UUID id, UUID studioId, UUID accountId, StudioRole role, Instant createdAt) {
    this.id = id;
    this.studioId = studioId;
    this.accountId = accountId;
    this.role = role;
    this.createdAt = createdAt;
  }

  UUID accountId() {
    return accountId;
  }

  StudioRole role() {
    return role;
  }

  Instant createdAt() {
    return createdAt;
  }
}
