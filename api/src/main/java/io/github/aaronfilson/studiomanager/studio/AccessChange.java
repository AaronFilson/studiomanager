package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One row of the bound studio's access log ({@code role_change_log}): written once, never edited.
 */
@Entity
@Table(name = "role_change_log")
class AccessChange {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID actorAccountId;

  private @Nullable UUID viaTokenId;

  @Enumerated(EnumType.STRING)
  private AccessChangeLog.Kind kind;

  private String target;

  private String beforeValue;

  private String afterValue;

  private Instant at;

  @SuppressWarnings("NullAway.Init")
  protected AccessChange() {}

  AccessChange(
      UUID actorAccountId,
      @Nullable UUID viaTokenId,
      AccessChangeLog.Kind kind,
      String target,
      String beforeValue,
      String afterValue,
      Instant at) {
    this.id = Uuids.v7();
    this.actorAccountId = actorAccountId;
    this.viaTokenId = viaTokenId;
    this.kind = kind;
    this.target = target;
    this.beforeValue = beforeValue;
    this.afterValue = afterValue;
    this.at = at;
  }

  UUID id() {
    return id;
  }

  UUID actorAccountId() {
    return actorAccountId;
  }

  @Nullable UUID viaTokenId() {
    return viaTokenId;
  }

  AccessChangeLog.Kind kind() {
    return kind;
  }

  String target() {
    return target;
  }

  String beforeValue() {
    return beforeValue;
  }

  String afterValue() {
    return afterValue;
  }

  Instant at() {
    return at;
  }
}
