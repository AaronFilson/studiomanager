package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One balanced journal entry's header. It is written by an idempotent SQL insert ({@link
 * LedgerQueries#insertEntry}); this entity reads it and is what its {@link JournalLine}s point at.
 * Never changed: the table allows only inserts.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "journal_entry")
class JournalEntry {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  /** The studio's local date the money moved, which is what bookkeeping is reported by. */
  private LocalDate occurredOn;

  @Enumerated(EnumType.STRING)
  private EntryKind kind;

  private String memo;

  /** What caused it: a piece, a sale, a payout. */
  private @Nullable UUID sourceId;

  /** So a retried event or import posts once. */
  private String idempotencyKey;

  private @Nullable UUID createdBy;

  private Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected JournalEntry() {}
}
