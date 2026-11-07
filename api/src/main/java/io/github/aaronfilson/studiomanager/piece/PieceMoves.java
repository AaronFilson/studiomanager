package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moving pieces from stage to stage: one piece by hand, or a whole load after a firing.
 *
 * <p>Each move is written to the piece's timeline ({@link PieceTimeline}) and flushed before
 * returning, since the caller may run SQL or publish events next (ADR 21).
 */
@Service
class PieceMoves {

  static final int DEFAULT_HOLD_DAYS = 30;

  private final PieceRepository pieces;
  private final PieceQueries queries;
  private final PieceTimeline timeline;
  private final StudioService studios;
  private final EntityManager entityManager;
  private final Clock clock;

  PieceMoves(
      PieceRepository pieces,
      PieceQueries queries,
      PieceTimeline timeline,
      StudioService studios,
      EntityManager entityManager,
      Clock clock) {
    this.pieces = pieces;
    this.queries = queries;
    this.timeline = timeline;
    this.studios = studios;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  /** Moves one piece, if the client saw its current version and the move is allowed. */
  @Transactional
  Piece move(
      UUID pieceId, PieceStage target, @Nullable String note, long expectedVersion, UUID actor) {
    Piece piece =
        pieces.findById(pieceId).orElseThrow(() -> new NotFoundException("No such piece."));
    piece.checkMove(target);
    if (piece.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this piece at the same time. Reload and try again.");
    }
    PieceStage from = piece.stage();
    Instant now = clock.instant();
    piece.moveTo(target, now, target == PieceStage.READY ? holdEnd(now) : null);
    timeline.record(pieceId, from, target, note, actor, now);
    pieces.flush();
    return piece;
  }

  /**
   * Moves every piece in {@code pieceIds} from {@code from} to {@code to} in one statement, with
   * one timeline entry each. All or nothing: if any piece is no longer in {@code from}, nothing
   * moves.
   */
  @Transactional
  List<Piece> advanceAll(
      Collection<UUID> pieceIds, PieceStage from, PieceStage to, String note, UUID actor) {
    if (!from.canMoveTo(to)) {
      throw new IllegalArgumentException("Pieces can't go from %s to %s".formatted(from, to));
    }
    Instant now = clock.instant();
    // The update is SQL, so anything Hibernate holds must be written first
    pieces.flush();
    int moved =
        queries.advance(pieceIds, from, to, now, to == PieceStage.READY ? holdEnd(now) : null);
    if (moved != pieceIds.size()) {
      throw ConflictException.staleVersion(
          "Some of these pieces were moved by someone else. Reload and try again.");
    }
    timeline.recordAll(pieceIds, from, to, note, actor, now);
    pieces.flush();
    List<Piece> afterwards = pieces.findAllInNumberOrder(pieceIds);
    // A piece this transaction had already loaded still shows its old stage: read it again
    for (Piece piece : afterwards) {
      if (piece.stage() != to) {
        entityManager.refresh(piece);
      }
    }
    return afterwards;
  }

  int holdDays() {
    return queries.holdDays().orElse(DEFAULT_HOLD_DAYS);
  }

  private Instant holdEnd(Instant readyAt) {
    return holdEnd(readyAt, holdDays(), studios.current().timeZone());
  }

  /** The end of the studio's local day, {@code days} days after {@code readyAt}. */
  static Instant holdEnd(Instant readyAt, int days, ZoneId zone) {
    LocalDate lastDay = readyAt.atZone(zone).toLocalDate().plusDays(days);
    return lastDay.plusDays(1).atStartOfDay(zone).toInstant();
  }
}
