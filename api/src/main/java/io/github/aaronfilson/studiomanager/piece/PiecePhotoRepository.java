package io.github.aaronfilson.studiomanager.piece;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Photos of pieces. A list's first photo per piece is {@link PieceQueries#firstReadyPhotos}, which
 * needs Postgres's {@code distinct on}.
 */
interface PiecePhotoRepository extends JpaRepository<PiecePhoto, UUID> {

  Optional<PiecePhoto> findByIdAndPieceId(UUID id, UUID pieceId);

  /** A piece's finished photos, oldest first. */
  @Query(
      """
      select p from PiecePhoto p
      where p.pieceId = :pieceId
        and p.status = io.github.aaronfilson.studiomanager.piece.PhotoStatus.READY
      order by p.createdAt, p.id
      """)
  List<PiecePhoto> findReady(UUID pieceId);

  /** Finished photos, and uploads started since {@code windowStart} that may still finish. */
  @Query(
      """
      select count(p) from PiecePhoto p
      where p.pieceId = :pieceId
        and (p.status = io.github.aaronfilson.studiomanager.piece.PhotoStatus.READY
             or p.createdAt > :windowStart)
      """)
  long countTaken(UUID pieceId, Instant windowStart);

  @Query(
      """
      select count(p) from PiecePhoto p
      where p.pieceId = :pieceId
        and p.status = io.github.aaronfilson.studiomanager.piece.PhotoStatus.READY
      """)
  long countReady(UUID pieceId);
}
