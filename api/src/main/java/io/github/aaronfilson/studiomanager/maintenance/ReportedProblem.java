package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/** Something a member noticed, open until staff resolve it. */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "problem_report")
class ReportedProblem {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  /** Null for "somewhere else", or once the asset is gone. */
  private @Nullable UUID assetId;

  private String description;

  private UUID reportedBy;

  private Instant reportedAt;

  private @Nullable UUID resolvedBy;

  private @Nullable Instant resolvedAt;

  private @Nullable String resolution;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected ReportedProblem() {}

  ReportedProblem(@Nullable UUID assetId, String description, UUID reportedBy, Instant reportedAt) {
    this.id = Uuids.v7();
    this.assetId = assetId;
    this.description = description;
    this.reportedBy = reportedBy;
    this.reportedAt = reportedAt;
  }

  UUID id() {
    return id;
  }

  UUID reportedBy() {
    return reportedBy;
  }

  @Nullable UUID resolvedBy() {
    return resolvedBy;
  }

  long version() {
    return Objects.requireNonNull(version, "A report has a version once it's saved");
  }

  /**
   * Closes the report. A report already resolved is a conflict, as a stale version is: someone else
   * got there first.
   */
  void resolve(UUID by, Instant at, @Nullable String cleanedResolution) {
    if (resolvedAt != null) {
      throw MaintenanceText.staleVersionOfReport();
    }
    this.resolvedBy = by;
    this.resolvedAt = at;
    this.resolution = cleanedResolution;
  }

  /**
   * @param assetName the asset's name now, or null when it's untracked or there's none
   */
  MaintenanceModel.ProblemReport toView(
      @Nullable String assetName,
      @Nullable String reportedByName,
      @Nullable String resolvedByName) {
    return new MaintenanceModel.ProblemReport(
        id,
        assetId,
        assetName,
        description,
        reportedBy,
        reportedByName,
        reportedAt,
        resolvedBy,
        resolvedByName,
        resolvedAt,
        resolution,
        version());
  }

  @Nullable UUID assetId() {
    return assetId;
  }
}
