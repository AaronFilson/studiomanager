package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PaymentMethod;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Money sent to a payee, with the journal entry that posted it. Written once. */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "payee_payment")
@EntityListeners(AuditingEntityListener.class)
class PayeePayment {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID payeeId;

  private LocalDate paidOn;

  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  private PaymentMethod method;

  /** Such as a cheque number. */
  private @Nullable String reference;

  private UUID entryId;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected PayeePayment() {}

  PayeePayment(
      UUID id,
      UUID payeeId,
      LocalDate paidOn,
      BigDecimal amount,
      PaymentMethod method,
      @Nullable String reference,
      UUID entryId) {
    this.id = id;
    this.payeeId = payeeId;
    this.paidOn = paidOn;
    this.amount = amount;
    this.method = method;
    this.reference = reference;
    this.entryId = entryId;
  }
}
