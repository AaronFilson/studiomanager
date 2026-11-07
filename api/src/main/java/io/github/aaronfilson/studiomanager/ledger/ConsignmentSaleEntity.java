package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentSale;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A sale of consigned work, with the journal entry that posted it and, once refunded, the one that
 * reversed it. Named for its table; {@link ConsignmentSale} is the view of it.
 */
@Entity
@Table(name = "consignment_sale")
@EntityListeners(AuditingEntityListener.class)
class ConsignmentSaleEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id")
  private ConsignmentItemEntity item;

  private int quantity;

  /** What the buyer paid before tax, after any discount. */
  private BigDecimal price;

  private BigDecimal consignorAmount;

  private BigDecimal tax;

  @Enumerated(EnumType.STRING)
  private Provider provider;

  private LocalDate soldOn;

  private UUID entryId;

  private @Nullable LocalDate refundedOn;

  private @Nullable UUID refundEntryId;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected ConsignmentSaleEntity() {}

  ConsignmentSaleEntity(
      UUID id,
      ConsignmentItemEntity item,
      int quantity,
      BigDecimal price,
      BigDecimal consignorAmount,
      BigDecimal tax,
      Provider provider,
      LocalDate soldOn,
      UUID entryId) {
    this.id = id;
    this.item = item;
    this.quantity = quantity;
    // As the columns hold them, so the view reads the same as one loaded later
    this.price = Numeric.ofScale(price, 2);
    this.consignorAmount = Numeric.ofScale(consignorAmount, 2);
    this.tax = Numeric.ofScale(tax, 2);
    this.provider = provider;
    this.soldOn = soldOn;
    this.entryId = entryId;
  }

  ConsignmentItemEntity item() {
    return item;
  }

  int quantity() {
    return quantity;
  }

  BigDecimal price() {
    return price;
  }

  BigDecimal consignorAmount() {
    return consignorAmount;
  }

  BigDecimal tax() {
    return tax;
  }

  Provider provider() {
    return provider;
  }

  boolean refunded() {
    return refundedOn != null;
  }

  void refund(LocalDate day, UUID refundEntry) {
    this.refundedOn = day;
    this.refundEntryId = refundEntry;
  }

  ConsignmentSale toView() {
    return new ConsignmentSale(
        id,
        item.id(),
        item.description(),
        quantity,
        price,
        consignorAmount,
        tax,
        provider,
        soldOn,
        refundedOn);
  }
}
