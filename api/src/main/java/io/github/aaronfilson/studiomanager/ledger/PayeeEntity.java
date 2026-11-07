package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeChanges;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Column;
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
 * Someone the studio pays: an artist on consignment, an instructor, a guild. Named for its table;
 * {@link Payee} is the view of it, with what they're owed. The table has no version column, and the
 * contract sends none, so the last save wins, as before.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "payee")
@EntityListeners(AuditingEntityListener.class)
class PayeeEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String name;

  private @Nullable String email;

  /** A member, who can then be paid in studio credit. */
  private @Nullable UUID accountId;

  @Column(name = "files_1099")
  private boolean files1099;

  @Column(name = "w9_received_on")
  private @Nullable LocalDate w9ReceivedOn;

  private @Nullable BigDecimal consignorShare;

  @Enumerated(EnumType.STRING)
  private DiscountsFrom discountsFrom;

  private @Nullable Integer consignmentDays;

  private @Nullable Instant archivedAt;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected PayeeEntity() {}

  /** {@code changes} are already checked, and their text trimmed. */
  PayeeEntity(PayeeChanges changes) {
    this.id = Uuids.v7();
    this.name = changes.name();
    this.discountsFrom = changes.discountsFrom();
    copy(changes);
  }

  /**
   * Takes on new details. Archiving keeps the date it was first archived; restoring clears it.
   * {@code changes} are already checked, and their text trimmed.
   */
  void change(PayeeChanges changes, Instant now) {
    copy(changes);
    if (!changes.archived()) {
      this.archivedAt = null;
    } else if (archivedAt == null) {
      this.archivedAt = now;
    }
  }

  private void copy(PayeeChanges changes) {
    this.name = changes.name();
    this.email = changes.email();
    this.accountId = changes.accountId();
    this.files1099 = changes.files1099();
    this.w9ReceivedOn = changes.w9ReceivedOn();
    this.consignorShare = changes.consignorShare();
    this.discountsFrom = changes.discountsFrom();
    this.consignmentDays = changes.consignmentDays();
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  @Nullable UUID accountId() {
    return accountId;
  }

  @Nullable BigDecimal consignorShare() {
    return consignorShare;
  }

  DiscountsFrom discountsFrom() {
    return discountsFrom;
  }

  @Nullable Integer consignmentDays() {
    return consignmentDays;
  }

  boolean archived() {
    return archivedAt != null;
  }

  Payee toView(@Nullable String accountName, BigDecimal owed) {
    return new Payee(
        id,
        name,
        email,
        accountId,
        accountName,
        files1099,
        w9ReceivedOn,
        consignorShare,
        discountsFrom,
        consignmentDays,
        archived(),
        owed);
  }
}
