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
 * StudioRole}, and any custom roles its owners make (lane R7), each at a base of STAFF or MEMBER.
 * What a role holds is in {@link StoredRolePermission}.
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
    String key = base.name().toLowerCase(Locale.ROOT);
    return new StoredRole(key, base.name().charAt(0) + key.substring(1), base, true, now);
  }

  /** A custom role; the caller has checked its base and that its key and name are free. */
  static StoredRole custom(String key, String name, StudioRole base, Instant now) {
    return new StoredRole(key, name, base, false, now);
  }

  private StoredRole(String key, String name, StudioRole base, boolean system, Instant now) {
    this.id = Uuids.v7();
    this.key = key;
    this.name = name;
    this.base = base;
    this.system = system;
    this.createdAt = now;
    this.updatedAt = now;
  }

  UUID id() {
    return id;
  }

  String key() {
    return key;
  }

  String name() {
    return name;
  }

  StudioRole base() {
    return base;
  }

  boolean isSystem() {
    return system;
  }

  void rename(String newName, Instant now) {
    name = newName;
    updatedAt = now;
  }

  void touch(Instant now) {
    updatedAt = now;
  }
}
