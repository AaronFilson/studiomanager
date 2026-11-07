package io.github.aaronfilson.studiomanager.studio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One permission one role holds in the bound studio, by its {@code Permission} key. The table's key
 * also holds {@code studio_id}; Hibernate's leaves it out, since {@code @TenantId} already limits
 * every query to one studio. Added and removed, never updated.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "studio_role_permission")
class StoredRolePermission {

  @EmbeddedId private Key key;

  @TenantId private @Nullable UUID studioId;

  @SuppressWarnings("NullAway.Init")
  protected StoredRolePermission() {}

  StoredRolePermission(UUID roleId, String permission) {
    this.key = new Key(roleId, permission);
  }

  String permission() {
    return key.permission();
  }

  @Embeddable
  record Key(@Column(name = "role_id") UUID roleId, @Column(name = "permission") String permission)
      implements Serializable {}
}
