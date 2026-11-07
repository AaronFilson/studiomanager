package io.github.aaronfilson.studiomanager.schedule;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.Resource;
import io.github.aaronfilson.studiomanager.schedule.ScheduleModel.ResourceKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * Something on the calendar that can be booked: a kiln, the wheel room with its seats, the whole
 * studio. The certification type belongs to the access module and the asset to maintenance, so both
 * are held by id, not mapped (ADR 21).
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "resource")
class BookableResource {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String name;

  @Enumerated(EnumType.STRING)
  private ResourceKind kind;

  private int capacity;

  private @Nullable UUID requiredCertification;

  private @Nullable UUID assetId;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected BookableResource() {}

  BookableResource(
      String name,
      ResourceKind kind,
      int capacity,
      @Nullable UUID requiredCertification,
      @Nullable UUID assetId) {
    this.id = Uuids.v7();
    this.name = name;
    this.kind = kind;
    this.capacity = capacity;
    this.requiredCertification = requiredCertification;
    this.assetId = assetId;
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  int capacity() {
    return capacity;
  }

  @Nullable UUID requiredCertification() {
    return requiredCertification;
  }

  @Nullable UUID assetId() {
    return assetId;
  }

  long version() {
    return Objects.requireNonNull(version, "A resource has a version once it's saved");
  }

  /**
   * @param certificationName the required certification's name, looked up by the caller
   * @param downReason why its asset is out of service; null when it's in service or there's none
   */
  Resource toView(@Nullable String certificationName, @Nullable String downReason) {
    return new Resource(
        id,
        name,
        kind,
        capacity,
        requiredCertification,
        certificationName,
        assetId,
        downReason,
        version());
  }
}
