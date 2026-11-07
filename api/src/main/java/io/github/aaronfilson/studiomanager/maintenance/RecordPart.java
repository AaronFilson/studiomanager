package io.github.aaronfilson.studiomanager.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A spare part used in one record. The supply item belongs to the supplies module, so it is held by
 * id, not mapped (ADR 21). Saved with its record, by cascade.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "maintenance_record_part")
class RecordPart {

  @EmbeddedId private Key key;

  /** Read-only here: {@code record_id} is written through the key. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", insertable = false, updatable = false)
  private MaintenanceRecord record;

  @TenantId private @Nullable UUID studioId;

  private BigDecimal quantity;

  @SuppressWarnings("NullAway.Init")
  protected RecordPart() {}

  RecordPart(MaintenanceRecord record, UUID itemId, BigDecimal quantity) {
    this.key = new Key(record.id(), itemId);
    this.record = record;
    this.quantity = quantity;
  }

  UUID recordId() {
    return key.recordId();
  }

  UUID itemId() {
    return key.itemId();
  }

  BigDecimal quantity() {
    return quantity;
  }

  /** The table's primary key, its columns named outright as {@link #record}'s join column is. */
  @Embeddable
  record Key(@Column(name = "record_id") UUID recordId, @Column(name = "item_id") UUID itemId)
      implements Serializable {}
}
