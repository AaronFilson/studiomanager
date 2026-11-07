package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Line;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
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

  Journal(JournalEntryRepository entries) {
    this.entries = entries;
  }

  /**
   * Writes a balanced entry once per idempotency key, and returns its id either way. Joins the
   * caller's transaction, and writes the entry before returning, so SQL that sums lines next sees
   * it.
   *
   * @throws ConflictException if another transaction posts the same key at the same moment
   */
  UUID post(
      EntryKind kind,
      LocalDate occurredOn,
      String memo,
      @Nullable UUID sourceId,
      String idempotencyKey,
      List<Line> lines) {
    BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() != 0 || lines.isEmpty()) {
      throw new IllegalArgumentException("An entry's lines must sum to zero, not " + total);
    }
    Optional<UUID> posted = entries.findIdByKey(idempotencyKey);
    if (posted.isPresent()) {
      return posted.get();
    }
    JournalEntry entry = new JournalEntry(kind, occurredOn, memo, sourceId, idempotencyKey, lines);
    try {
      entries.saveAndFlush(entry);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw ConflictException.duplicate("Someone else recorded this at the same moment.");
      }
      throw e;
    }
    return entry.id();
  }

  /** Whether an entry with this idempotency key has been posted. */
  boolean isPosted(String idempotencyKey) {
    return entries.existsByIdempotencyKey(idempotencyKey);
  }
}
