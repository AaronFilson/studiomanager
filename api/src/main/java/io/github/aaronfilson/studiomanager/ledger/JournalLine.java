package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.PaymentChannel;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One line of a journal entry: debits positive, credits negative. The sub-ledger columns say whose
 * receivable, which stored-value balance, which processor and which payee. They stay ids: balances
 * are sums in SQL, never walked through entities.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "journal_line")
class JournalLine {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entry_id")
  private JournalEntry entry;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private LedgerAccount account;

  private BigDecimal amount;

  private @Nullable UUID memberId;

  @Column(name = "stored_value")
  private @Nullable UUID storedValueId;

  @Enumerated(EnumType.STRING)
  private @Nullable PaymentChannel provider;

  private @Nullable String taxCategory;

  private @Nullable UUID payeeId;

  @SuppressWarnings("NullAway.Init")
  protected JournalLine() {}

  JournalLine(JournalEntry entry, Line line) {
    this.id = Uuids.v7();
    this.entry = entry;
    this.account = line.account();
    this.amount = line.amount();
    this.memberId = line.member();
    this.storedValueId = line.storedValue();
    this.provider = line.provider();
    this.taxCategory = line.taxCategory();
    this.payeeId = line.payee();
  }
}
