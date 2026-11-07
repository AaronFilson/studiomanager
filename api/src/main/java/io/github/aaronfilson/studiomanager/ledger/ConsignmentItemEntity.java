package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.platform.Uuids;
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
 * Work taken in to sell for an artist, with the terms copied from the payee when it came in. Named
 * for its table; {@link ConsignmentItem} is the view of it.
 */
@Entity
@Table(name = "consignment_item")
@EntityListeners(AuditingEntityListener.class)
class ConsignmentItemEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payee_id")
  private PayeeEntity payee;

  private String description;

  private BigDecimal retailPrice;

  private BigDecimal consignorShare;

  @Enumerated(EnumType.STRING)
  private DiscountsFrom discountsFrom;

  private int quantity;

  private int sold;

  private int returned;

  private LocalDate receivedOn;

  /** When unsold work goes back to the artist; null for no limit. */
  private @Nullable LocalDate returnBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected ConsignmentItemEntity() {}

  ConsignmentItemEntity(
      PayeeEntity payee,
      String description,
      BigDecimal retailPrice,
      BigDecimal consignorShare,
      int quantity,
      LocalDate receivedOn,
      @Nullable LocalDate returnBy) {
    this.id = Uuids.v7();
    this.payee = payee;
    this.description = description;
    // As the columns hold them, so the view reads the same as one loaded later
    this.retailPrice = Numeric.ofScale(retailPrice, 2);
    this.consignorShare = Numeric.ofScale(consignorShare, 4);
    this.discountsFrom = payee.discountsFrom();
    this.quantity = quantity;
    this.receivedOn = receivedOn;
    this.returnBy = returnBy;
  }

  UUID id() {
    return id;
  }

  PayeeEntity payee() {
    return payee;
  }

  String description() {
    return description;
  }

  int inStock() {
    return quantity - sold - returned;
  }

  void sell(int count) {
    this.sold += count;
  }

  /** Puts refunded work back on the shelf. */
  void unsell(int count) {
    this.sold -= count;
  }

  void giveBack(int count) {
    this.returned += count;
  }

  /** {@code today} is the studio's, for whether the work is past its return date. */
  ConsignmentItem toView(LocalDate today) {
    LocalDate due = returnBy;
    boolean dueBack = due != null && due.isBefore(today) && sold + returned < quantity;
    return new ConsignmentItem(
        id,
        payee.id(),
        payee.name(),
        description,
        retailPrice,
        consignorShare,
        discountsFrom,
        quantity,
        sold,
        returned,
        receivedOn,
        returnBy,
        dueBack);
  }
}
