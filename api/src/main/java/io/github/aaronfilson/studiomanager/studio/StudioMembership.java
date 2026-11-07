package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.MemberRole;
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
 * <p>The role is a {@link StoredRole} row ({@code roleId}, lane R7), system or custom; {@code role}
 * is always that row's base, kept for everything still keyed on it ({@code V41__custom_roles.sql}
 * keeps the two in step in the database too).
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

  private UUID roleId;

  private Instant createdAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected StudioMembership() {}

  StudioMembership(UUID id, UUID studioId, UUID accountId, StoredRole role, Instant createdAt) {
    this.id = id;
    this.studioId = studioId;
    this.accountId = accountId;
    this.role = role.base();
    this.roleId = role.id();
    this.createdAt = createdAt;
  }

  UUID accountId() {
    return accountId;
  }

  StudioRole role() {
    return role;
  }

  UUID roleId() {
    return roleId;
  }

  MemberRole memberRole() {
    return new MemberRole(role, roleId);
  }

  Instant createdAt() {
    return createdAt;
  }

  /** Puts the member in {@code newRole}, at its base. */
  void changeRole(StoredRole newRole) {
    this.role = newRole.base();
    this.roleId = newRole.id();
  }
}
