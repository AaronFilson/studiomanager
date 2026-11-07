package io.github.aaronfilson.studiomanager.piece;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Pieces' timelines. Written and read only through {@link PieceTimeline}. */
interface PieceEventRepository extends JpaRepository<PieceEvent, UUID> {

  @Query("select e from PieceEvent e where e.pieceId = :pieceId order by e.occurredAt, e.id")
  List<PieceEvent> findTimeline(UUID pieceId);
}
