package io.github.aaronfilson.studiomanager.piece;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Pieces in the bound studio. Hibernate adds the studio to every query ({@code @TenantId}). */
interface PieceRepository extends JpaRepository<Piece, UUID> {

  /** Newest first, optionally only one stage or one maker. */
  @Query(
      """
      select p from Piece p
      where (:stage is null or p.stage = :stage)
        and (:maker is null or p.makerAccountId = :maker)
      order by p.number desc
      """)
  List<Piece> findNewest(@Nullable PieceStage stage, @Nullable UUID maker, Limit limit);

  @Query("select p from Piece p where p.id in :pieceIds order by p.number")
  List<Piece> findAllInNumberOrder(Collection<UUID> pieceIds);

  /**
   * Locks the piece's row until the transaction ends, so two photo uploads at once can't both take
   * its last place.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Piece p where p.id = :pieceId")
  Optional<Piece> findAndLock(UUID pieceId);

  /** How many pieces are in each stage: one query for the whole studio. */
  @Query(
      """
      select new io.github.aaronfilson.studiomanager.piece.PieceStageCount(p.stage, count(p))
      from Piece p group by p.stage
      """)
  List<PieceStageCount> countByStage();

  @Query(
      """
      select count(p) from Piece p
      where p.stage = io.github.aaronfilson.studiomanager.piece.PieceStage.READY
        and p.holdUntil <= :now
      """)
  long countPastHold(Instant now);
}
