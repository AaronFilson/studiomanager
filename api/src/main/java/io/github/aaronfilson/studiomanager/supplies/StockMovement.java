package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One change to an item's stock, signed: received is positive, sold and used negative, a count's
 * correction either way. Movements are only ever added (the table grants no update), so the entity
 * is immutable. The item is held by id: nothing here needs the item loaded.
 */
@Entity
@Immutable
@Table(name = "stock_movement")
@EntityListeners(AuditingEntityListener.class)
class StockMovement {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID itemId;

  @Enumerated(EnumType.STRING)
  private MovementKind kind;

  private BigDecimal quantity;

  private @Nullable UUID memberId;

  private @Nullable String note;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected StockMovement() {}

  /**
   * @param signedQuantity the change to stock on hand, never zero
   * @param memberId who bought it, for a sale; otherwise null
   */
  StockMovement(
      UUID itemId,
      MovementKind kind,
      BigDecimal signedQuantity,
      @Nullable UUID memberId,
      @Nullable String note) {
    this.id = Uuids.v7();
    this.itemId = itemId;
    this.kind = kind;
    this.quantity = signedQuantity;
    this.memberId = memberId;
    this.note = note;
  }

  UUID id() {
    return id;
  }

  /** The signed change to stock on hand. */
  BigDecimal quantity() {
    return quantity;
  }

  @Nullable UUID memberId() {
    return memberId;
  }

  /** As the API shows it, with the member's name looked up by the caller. */
  SupplyModel.Movement toView(@Nullable String memberName) {
    return new SupplyModel.Movement(
        id,
        kind,
        quantity,
        memberId,
        memberName,
        note,
        Objects.requireNonNull(createdAt, "A movement has a creation time once it's saved"));
  }
}
