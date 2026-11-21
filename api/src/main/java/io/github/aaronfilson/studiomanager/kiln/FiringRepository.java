package io.github.aaronfilson.studiomanager.kiln;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Firings, always fetched with their kiln, since every view shows the kiln's name. Hibernate adds
 * the studio to every query ({@code @TenantId}).
 */
interface FiringRepository extends JpaRepository<Firing, UUID> {

  @Query("select f from Firing f join fetch f.kiln where f.id = :firingId")
  Optional<Firing> findWithKiln(UUID firingId);

  @Query("select f from Firing f join fetch f.kiln k where f.status in :statuses order by k.name")
  List<Firing> findByStatusIn(Collection<FiringView.Status> statuses);

  @Query(
      """
      select f from Firing f join fetch f.kiln k
      where k.id = :kilnId
      order by coalesce(f.startedAt, f.finishedAt, f.createdAt) desc, f.createdAt desc, f.id desc
      """)
  List<Firing> findNewestForKiln(UUID kilnId, Limit limit);

  /**
   * Lightweight firing metadata overlapping {@code [windowStart, windowEnd]} on a kiln, without
   * hydrating {@code temperature_curve} JSONB payloads.
   */
  @Query(
      """
      select new io.github.aaronfilson.studiomanager.kiln.FiringOverlapCandidate(
          f.id, f.kind, f.cone, f.status, f.createdAt, f.startedAt, f.finishedAt, f.peakTemp)
      from Firing f
      where f.kiln.id = :kilnId
        and f.status in :statuses
        and coalesce(f.finishedAt, f.startedAt, f.createdAt) >= :windowStart
        and coalesce(f.startedAt, f.createdAt) <= :windowEnd
      order by coalesce(f.startedAt, f.finishedAt, f.createdAt) desc, f.id desc
      """)
  List<FiringOverlapCandidate> findOverlapCandidates(
      UUID kilnId,
      Collection<FiringView.Status> statuses,
      Instant windowStart,
      Instant windowEnd,
      Limit limit);

  /**
   * Whether a non-cancelled firing on this kiln already matches this imported log's fingerprint.
   */
  @Query(
      """
      select (count(f) > 0) from Firing f
      where f.kiln.id = :kilnId
        and f.status <> :cancelled
        and f.startedAt = :startedAt
        and f.finishedAt = :finishedAt
        and f.peakTemp = :peakTemp
        and f.logSource = :logSource
      """)
  boolean existsDuplicateLog(
      UUID kilnId,
      Instant startedAt,
      Instant finishedAt,
      BigDecimal peakTemp,
      KilnLogSource logSource,
      FiringView.Status cancelled);

  /** How many pieces each of these firings holds: one query for a whole list. */
  @Query(
      """
      select new io.github.aaronfilson.studiomanager.kiln.FiringPieceCount(
          fp.key.firingId, count(fp))
      from FiringPiece fp
      where fp.key.firingId in :firingIds
      group by fp.key.firingId
      """)
  List<FiringPieceCount> countPieces(Collection<UUID> firingIds);

  /** Every piece in a load that is still loading or firing, in any of the studio's kilns. */
  @Query("select fp.key.pieceId from FiringPiece fp where fp.open = true")
  List<UUID> findPiecesInOpenLoads();
}
