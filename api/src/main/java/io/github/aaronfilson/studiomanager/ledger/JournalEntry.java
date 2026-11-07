package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One balanced journal entry and its lines. Written once and never changed: the table allows only
 * inserts, and the database refuses an entry whose lines don't sum to zero at commit.
 */
@Entity
@Table(name = "journal_entry")
@EntityListeners(AuditingEntityListener.class)
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

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @OneToMany(mappedBy = "entry", cascade = CascadeType.PERSIST)
  private List<JournalLine> lines = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected JournalEntry() {}

  /** {@code lines} already sum to zero. */
  JournalEntry(
      EntryKind kind,
      LocalDate occurredOn,
      String memo,
      @Nullable UUID sourceId,
      String idempotencyKey,
      List<Line> lines) {
    this.id = Uuids.v7();
    this.kind = kind;
    this.occurredOn = occurredOn;
    this.memo = memo;
    this.sourceId = sourceId;
    this.idempotencyKey = idempotencyKey;
    for (Line line : lines) {
      this.lines.add(new JournalLine(this, line));
    }
  }

  UUID id() {
    return id;
  }
}
