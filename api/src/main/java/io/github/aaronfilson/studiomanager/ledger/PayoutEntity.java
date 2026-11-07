package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.PaymentChannel;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Payout;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
 * A processor's deposit into the studio's bank, with the journal entry that posted it. Written
 * once. Named for its table; {@link Payout} is the view of it.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "payout")
@EntityListeners(AuditingEntityListener.class)
class PayoutEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private PaymentChannel provider;

  private String externalId;

  private LocalDate arrivalOn;

  private BigDecimal gross;

  private BigDecimal refunds;

  private BigDecimal fees;

  private BigDecimal adjustments;

  private BigDecimal net;

  private String currency;

  private UUID entryId;

  @CreatedDate private @Nullable Instant importedAt;

  @SuppressWarnings("NullAway.Init")
  protected PayoutEntity() {}

  PayoutEntity(
      PaymentChannel provider,
      String externalId,
      LocalDate arrivalOn,
      BigDecimal gross,
      BigDecimal refunds,
      BigDecimal fees,
      BigDecimal adjustments,
      BigDecimal net,
      String currency,
      UUID entryId) {
    this.id = Uuids.v7();
    this.provider = provider;
    this.externalId = externalId;
    this.arrivalOn = arrivalOn;
    // As the columns hold them, so the view reads the same as one loaded later
    this.gross = Numeric.ofScale(gross, 2);
    this.refunds = Numeric.ofScale(refunds, 2);
    this.fees = Numeric.ofScale(fees, 2);
    this.adjustments = Numeric.ofScale(adjustments, 2);
    this.net = Numeric.ofScale(net, 2);
    this.currency = currency;
    this.entryId = entryId;
  }

  Payout toView() {
    return new Payout(
        id, provider, externalId, arrivalOn, gross, refunds, fees, adjustments, net, currency);
  }
}
