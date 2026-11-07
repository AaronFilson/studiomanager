package io.github.aaronfilson.studiomanager.maintenance;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * Work done on an asset: a task completed, or a repair outside any task. Records are insert-only
 * evidence for an insurer, so the table has no version; {@link Persistable} tells Spring Data a new
 * one apart from a loaded one, so saving it inserts rather than merges.
 */
@Entity
@Table(name = "maintenance_record")
class MaintenanceRecord implements Persistable<UUID> {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id")
  private MaintainedAsset asset;

  /** Null for a repair, or once the task is gone. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private @Nullable MaintenanceTask task;

  private @Nullable String note;

  private @Nullable BigDecimal cost;

  @JdbcTypeCode(SqlTypes.ARRAY)
  private List<String> checked = new ArrayList<>();

  private @Nullable Integer atFiring;

  private UUID doneBy;

  private Instant doneAt;

  @OneToMany(mappedBy = "record", cascade = CascadeType.PERSIST)
  private List<RecordPart> parts = new ArrayList<>();

  @Transient private boolean isNew = true;

  @SuppressWarnings("NullAway.Init")
  protected MaintenanceRecord() {}

  /**
   * @param task the task completed, or null for a repair
   * @param atFiring for a kiln, its total firings now; null for anything else
   */
  MaintenanceRecord(
      MaintainedAsset asset,
      @Nullable MaintenanceTask task,
      @Nullable String note,
      @Nullable BigDecimal cost,
      List<String> checked,
      @Nullable Integer atFiring,
      UUID doneBy,
      Instant doneAt) {
    this.id = Uuids.v7();
    this.asset = asset;
    this.task = task;
    this.note = note;
    this.cost = cost;
    this.checked = new ArrayList<>(checked);
    this.atFiring = atFiring;
    this.doneBy = doneBy;
    this.doneAt = doneAt;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markStored() {
    this.isNew = false;
  }

  UUID id() {
    return id;
  }

  /** Records a spare part drawn from supplies for this work; saved with the record. */
  void addPart(UUID itemId, BigDecimal quantity) {
    parts.add(new RecordPart(this, itemId, quantity));
  }

  /**
   * The record as the API shows it. Lists fetch the task with the record, so reading its title
   * needs no query per row.
   */
  MaintenanceModel.WorkRecord toView(
      @Nullable String doneByName, List<MaintenanceModel.PartUsed> used) {
    MaintenanceTask completed = task;
    return new MaintenanceModel.WorkRecord(
        id,
        asset.id(),
        completed == null ? null : completed.id(),
        completed == null ? null : completed.title(),
        note,
        cost,
        List.copyOf(checked),
        atFiring,
        doneBy,
        doneByName,
        doneAt,
        used);
  }

  /** The parts used, as saved with this record. */
  List<RecordPart> parts() {
    return List.copyOf(parts);
  }

  UUID doneBy() {
    return doneBy;
  }
}
