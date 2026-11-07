package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.EarningKind;
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

/** A share or fee a payee earned, with the journal entry that posted it. Written once. */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "payee_earning")
@EntityListeners(AuditingEntityListener.class)
class PayeeEarning {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID payeeId;

  @Enumerated(EnumType.STRING)
  private EarningKind kind;

  private String description;

  private LocalDate earnedOn;

  /** For a share: what the class took, and the payee's fraction of it. */
  private @Nullable BigDecimal gross;

  private @Nullable BigDecimal share;

  private BigDecimal amount;

  private UUID entryId;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected PayeeEarning() {}

  PayeeEarning(
      UUID id,
      UUID payeeId,
      EarningKind kind,
      String description,
      LocalDate earnedOn,
      @Nullable BigDecimal gross,
      @Nullable BigDecimal share,
      BigDecimal amount,
      UUID entryId) {
    this.id = id;
    this.payeeId = payeeId;
    this.kind = kind;
    this.description = description;
    this.earnedOn = earnedOn;
    this.gross = gross;
    this.share = share;
    this.amount = amount;
    this.entryId = entryId;
  }
}
