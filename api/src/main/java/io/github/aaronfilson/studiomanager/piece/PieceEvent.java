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
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
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

  /** The personal API token the actor made the change through; null when they used the app. */
  private @Nullable UUID viaTokenId;

  private Instant occurredAt;

  @SuppressWarnings("NullAway.Init")
  protected PieceEvent() {}

  PieceEvent(
      UUID pieceId,
      @Nullable PieceStage fromStage,
      PieceStage toStage,
      @Nullable String note,
      UUID actorAccountId,
      @Nullable UUID viaTokenId,
      Instant occurredAt) {
    this.id = Uuids.v7();
    this.pieceId = pieceId;
    this.fromStage = fromStage;
    this.toStage = toStage;
    this.note = note;
    this.actorAccountId = actorAccountId;
    this.viaTokenId = viaTokenId;
    this.occurredAt = occurredAt;
  }

  @Nullable UUID actorAccountId() {
    return actorAccountId;
  }

  @Nullable UUID viaTokenId() {
    return viaTokenId;
  }

  PieceView.Event toView(@Nullable String actorName, @Nullable String viaTokenName) {
    return new PieceView.Event(
        fromStage, toStage, note, actorAccountId, actorName, viaTokenName, occurredAt);
  }
}
