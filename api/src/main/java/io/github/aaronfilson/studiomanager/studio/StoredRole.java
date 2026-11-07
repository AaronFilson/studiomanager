package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A role in the bound studio (plan 21 §6). Each studio has the three system roles, one per {@link
 * StudioRole}; custom roles come with lane R7. What a role holds is in {@link
 * StoredRolePermission}.
 */
@Entity
@Table(name = "studio_role")
class StoredRole {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String key;

  private String name;

  @Enumerated(EnumType.STRING)
  private StudioRole base;

  @Column(name = "is_system")
  private boolean system;

  private Instant createdAt;

  private Instant updatedAt;

  @SuppressWarnings("NullAway.Init")
  protected StoredRole() {}

  /** The system role for {@code base}: key {@code staff}, name {@code Staff}. */
  static StoredRole system(StudioRole base, Instant now) {
    StoredRole role = new StoredRole();
    role.id = Uuids.v7();
    role.key = base.name().toLowerCase(Locale.ROOT);
    role.name = base.name().charAt(0) + role.key.substring(1);
    role.base = base;
    role.system = true;
    role.createdAt = now;
    role.updatedAt = now;
    return role;
  }

  UUID id() {
    return id;
  }

  String key() {
    return key;
  }

  void touch(Instant now) {
    updatedAt = now;
  }
}
