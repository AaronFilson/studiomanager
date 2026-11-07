package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.access.Duty;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A recurring job on one asset, such as "Vacuum the elements every 50 firings". Each task keeps its
 * own clock: when it was last done, and the kiln's firing count then.
 *
 * <p>{@code reminded_for} is claimed by one atomic statement in {@link MaintenanceQueries}, so it
 * is mapped read-only: a flush of this entity never writes it back.
 */
@Entity
@Table(name = "maintenance_task")
@EntityListeners(AuditingEntityListener.class)
class MaintenanceTask {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id")
  private MaintainedAsset asset;

  private String title;

  @JdbcTypeCode(SqlTypes.ARRAY)
  private List<String> checklist = new ArrayList<>();

  private @Nullable Integer everyDays;

  private @Nullable Integer everyFirings;

  @Enumerated(EnumType.STRING)
  private @Nullable Duty assignedDuty;

  private @Nullable UUID assignedTo;

  private Instant lastDoneAt;

  private int lastDoneFirings;

  @Column(insertable = false, updatable = false)
  private @Nullable Instant remindedFor;

  private boolean archived;

  @CreatedDate private @Nullable Instant createdAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected MaintenanceTask() {}

  /** A new task whose clock starts now, at the kiln's current count (0 for anything else). */
  MaintenanceTask(MaintainedAsset asset, TaskSettings settings, Instant now, int firingsNow) {
    this.id = Uuids.v7();
    this.asset = asset;
    this.title = settings.title();
    change(settings);
    this.lastDoneAt = now;
    this.lastDoneFirings = firingsNow;
  }

  UUID id() {
    return id;
  }

  MaintainedAsset asset() {
    return asset;
  }

  String title() {
    return title;
  }

  List<String> checklist() {
    return List.copyOf(checklist);
  }

  @Nullable Integer everyDays() {
    return everyDays;
  }

  @Nullable Integer everyFirings() {
    return everyFirings;
  }

  @Nullable Duty assignedDuty() {
    return assignedDuty;
  }

  @Nullable UUID assignedTo() {
    return assignedTo;
  }

  Instant lastDoneAt() {
    return lastDoneAt;
  }

  int lastDoneFirings() {
    return lastDoneFirings;
  }

  /** Whether a reminder has gone out for the cycle that began when it was last done. */
  boolean remindedThisCycle() {
    return lastDoneAt.equals(remindedFor);
  }

  long version() {
    return Objects.requireNonNull(version, "A task has a version once it's saved");
  }

  /** Takes new settings, already validated. The clock carries on from when it was last done. */
  void change(TaskSettings settings) {
    this.title = settings.title();
    this.checklist = new ArrayList<>(settings.checklist());
    this.everyDays = settings.everyDays();
    this.everyFirings = settings.everyFirings();
    this.assignedDuty = settings.assignedDuty();
    this.assignedTo = settings.assignedTo();
  }

  /** Restarts this task's clock, and only this task's. */
  void markDone(Instant now, int firingsNow) {
    this.lastDoneAt = now;
    this.lastDoneFirings = firingsNow;
  }

  void archive() {
    this.archived = true;
  }
}
