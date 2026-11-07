package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.AssetKind;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Something the studio maintains: a wheel, the pug mill, the building, or a kiln. A kiln's asset
 * has no name or down state of its own; both come from the kiln, which belongs to the kiln module
 * and is held here by id only (ADR 21).
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "asset")
@EntityListeners(AuditingEntityListener.class)
class MaintainedAsset {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private AssetKind kind;

  private @Nullable UUID kilnId;

  private @Nullable String name;

  private @Nullable String location;

  private @Nullable String notes;

  private @Nullable String downReason;

  private boolean archived;

  @CreatedDate private @Nullable Instant createdAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected MaintainedAsset() {}

  private MaintainedAsset(
      AssetKind kind,
      @Nullable UUID kilnId,
      @Nullable String name,
      @Nullable String location,
      @Nullable String notes) {
    this.id = Uuids.v7();
    this.kind = kind;
    this.kilnId = kilnId;
    this.name = name;
    this.location = location;
    this.notes = notes;
  }

  /** Anything but a kiln, with its own name. */
  static MaintainedAsset equipment(
      AssetKind kind, String name, @Nullable String location, @Nullable String notes) {
    return new MaintainedAsset(kind, null, name, location, notes);
  }

  /** The asset every kiln has from when it's added. */
  static MaintainedAsset ofKiln(UUID kilnId) {
    return new MaintainedAsset(AssetKind.KILN, kilnId, null, null, null);
  }

  UUID id() {
    return id;
  }

  AssetKind kind() {
    return kind;
  }

  /** The kiln this asset is; null for anything else. */
  @Nullable UUID kilnId() {
    return kilnId;
  }

  boolean isKiln() {
    return kilnId != null;
  }

  /** Its own name; null for a kiln, which takes the kiln's. */
  @Nullable String name() {
    return name;
  }

  @Nullable String location() {
    return location;
  }

  @Nullable String notes() {
    return notes;
  }

  /** Why it's out of service, for anything but a kiln; null when it's in service. */
  @Nullable String downReason() {
    return downReason;
  }

  long version() {
    return Objects.requireNonNull(version, "An asset has a version once it's saved");
  }

  /** Changes the location and notes, and the name when one is given. */
  void describe(@Nullable String newName, @Nullable String newLocation, @Nullable String newNotes) {
    if (newName != null) {
      this.name = newName;
    }
    this.location = newLocation;
    this.notes = newNotes;
  }

  void setDownReason(@Nullable String reason) {
    this.downReason = reason;
  }

  /** Stops tracking it. Its records are kept. */
  void archive() {
    this.archived = true;
  }
}
