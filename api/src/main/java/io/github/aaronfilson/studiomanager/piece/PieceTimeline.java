package io.github.aaronfilson.studiomanager.piece;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Each piece's append-only timeline: the one place its entries are written and read. (The hourly
 * {@code app.mark_unclaimed_pieces} also writes entries, inside the database.)
 *
 * <p>Entries are persisted, not saved: they have no version, so Spring Data's {@code save} would
 * look each one up before inserting it.
 */
@Component
class PieceTimeline {

  private final PieceEventRepository events;
  private final EntityManager entityManager;

  PieceTimeline(PieceEventRepository events, EntityManager entityManager) {
    this.events = events;
    this.entityManager = entityManager;
  }

  /** Records one move of one piece. */
  void record(
      UUID pieceId,
      @Nullable PieceStage from,
      PieceStage to,
      @Nullable String note,
      UUID actorAccountId,
      Instant at) {
    entityManager.persist(new PieceEvent(pieceId, from, to, note, actorAccountId, at));
  }

  /** Records the same move for many pieces, written as a batch of inserts. */
  void recordAll(
      Collection<UUID> pieceIds,
      PieceStage from,
      PieceStage to,
      @Nullable String note,
      UUID actorAccountId,
      Instant at) {
    for (UUID pieceId : pieceIds) {
      record(pieceId, from, to, note, actorAccountId, at);
    }
  }

  /** A piece's timeline, oldest first. */
  List<PieceEvent> of(UUID pieceId) {
    return events.findTimeline(pieceId);
  }
}
