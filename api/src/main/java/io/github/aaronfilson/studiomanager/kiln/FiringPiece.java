package io.github.aaronfilson.studiomanager.kiln;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One piece in one load. The piece belongs to the piece module, so it is held by id, not mapped
 * (ADR 21: entities never map another module's table).
 *
 * <p>{@code open} copies whether the firing is still loading or firing, so a unique index can stop
 * one piece being in two open loads.
 */
@Entity
@Table(name = "firing_piece")
class FiringPiece {

  @EmbeddedId private Key key;

  /** Read-only here: {@code firing_id} is written through the key. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "firing_id", insertable = false, updatable = false)
  private Firing firing;

  @TenantId private @Nullable UUID studioId;

  private boolean open;

  @SuppressWarnings("NullAway.Init")
  protected FiringPiece() {}

  FiringPiece(Firing firing, UUID pieceId) {
    this.key = new Key(firing.id(), pieceId);
    this.firing = firing;
    this.open = true;
  }

  UUID pieceId() {
    return key.pieceId();
  }

  void close() {
    this.open = false;
  }

  /**
   * The table's primary key. Its columns are named outright, as {@link #firing}'s join column is:
   * Hibernate refuses one column reached by an implicit and an explicit name.
   */
  @Embeddable
  record Key(@Column(name = "firing_id") UUID firingId, @Column(name = "piece_id") UUID pieceId)
      implements Serializable {}
}
