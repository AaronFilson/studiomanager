package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One entry in a piece's timeline: a move from one stage to another, who made it and when. The
 * timeline is append-only, so the entity is immutable. {@link PieceTimeline} writes and reads it.
 */
@Entity
@Immutable
@Table(name = "piece_event")
class PieceEvent {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID pieceId;

  @Enumerated(EnumType.STRING)
  private @Nullable PieceStage fromStage;

  @Enumerated(EnumType.STRING)
  private PieceStage toStage;

  private @Nullable String note;

  /** Null when the system made the change, such as a hold running out. */
  private @Nullable UUID actorAccountId;

  private Instant occurredAt;

  @SuppressWarnings("NullAway.Init")
  protected PieceEvent() {}

  PieceEvent(
      UUID pieceId,
      @Nullable PieceStage fromStage,
      PieceStage toStage,
      @Nullable String note,
      UUID actorAccountId,
      Instant occurredAt) {
    this.id = Uuids.v7();
    this.pieceId = pieceId;
    this.fromStage = fromStage;
    this.toStage = toStage;
    this.note = note;
    this.actorAccountId = actorAccountId;
    this.occurredAt = occurredAt;
  }

  @Nullable UUID actorAccountId() {
    return actorAccountId;
  }

  PieceView.Event toView(@Nullable String actorName) {
    return new PieceView.Event(fromStage, toStage, note, actorAccountId, actorName, occurredAt);
  }
}
