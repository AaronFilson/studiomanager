package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Posts to the studio's journal: every money event is one balanced entry.
 *
 * <p>Two rules keep it honest. The database refuses an entry whose lines don't sum to zero (a
 * deferred constraint trigger), and each entry carries an idempotency key, so an event delivered
 * twice or a payout file imported twice posts once.
 */
@Component
class Journal {

  private final JournalEntryRepository entries;
  private final JournalLineRepository lines;
  private final LedgerQueries queries;

  Journal(JournalEntryRepository entries, JournalLineRepository lines, LedgerQueries queries) {
    this.entries = entries;
    this.lines = lines;
    this.queries = queries;
  }

  /**
   * Writes a balanced entry once per idempotency key, and returns its id either way, even when
   * another transaction posts the same key at the same moment. Joins the caller's transaction, and
   * writes everything before returning, so SQL that sums lines next sees it.
   */
  UUID post(
      EntryKind kind,
      LocalDate occurredOn,
      String memo,
      @Nullable UUID sourceId,
      String idempotencyKey,
      List<Line> entryLines) {
    BigDecimal total =
        entryLines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() != 0 || entryLines.isEmpty()) {
      throw new IllegalArgumentException("An entry's lines must sum to zero, not " + total);
    }
    // The header is SQL: an idempotent insert, race-free in one statement
    entries.flush();
    Optional<UUID> inserted = queries.insertEntry(kind, occurredOn, memo, sourceId, idempotencyKey);
    if (inserted.isEmpty()) {
      return queries.entryIdFor(idempotencyKey);
    }
    UUID entryId = inserted.get();
    JournalEntry entry = entries.getReferenceById(entryId);
    for (Line line : entryLines) {
      lines.save(new JournalLine(entry, line));
    }
    lines.flush();
    return entryId;
  }

  /** Whether an entry with this idempotency key has been posted. */
  boolean isPosted(String idempotencyKey) {
    return entries.existsByIdempotencyKey(idempotencyKey);
  }
}
