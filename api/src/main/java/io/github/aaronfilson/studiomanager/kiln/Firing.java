package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceView;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One load of a kiln, from loading to unloading or cancelling. Its status changes are methods here,
 * so the rules for which change is allowed when live with the data.
 */
@Entity
@Table(name = "firing")
@EntityListeners(AuditingEntityListener.class)
class Firing {

  private static final int MAX_NOTES = 1000;

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "kiln_id")
  private Kiln kiln;

  @Enumerated(EnumType.STRING)
  private FiringKind kind;

  private @Nullable String cone;

  @Enumerated(EnumType.STRING)
  private FiringView.Status status;

  private @Nullable String notes;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  private @Nullable Instant startedAt;

  private @Nullable Instant finishedAt;

  @Version private @Nullable Long version;

  @OneToMany(mappedBy = "firing", cascade = CascadeType.PERSIST)
  @OrderBy("key.pieceId")
  private List<FiringPiece> pieces = new ArrayList<>();

  @SuppressWarnings("NullAway.Init")
  protected Firing() {}

  /** A new load of {@code pieceIds}, still loading. */
  Firing(Kiln kiln, FiringKind kind, @Nullable String cone, Collection<UUID> pieceIds) {
    this.id = Uuids.v7();
    this.kiln = kiln;
    this.kind = kind;
    this.cone = cone == null || cone.isBlank() ? null : cone.strip();
    this.status = FiringView.Status.LOADING;
    for (UUID pieceId : pieceIds) {
      pieces.add(new FiringPiece(this, pieceId));
    }
  }

  UUID id() {
    return id;
  }

  Kiln kiln() {
    return kiln;
  }

  FiringKind kind() {
    return kind;
  }

  FiringView.Status status() {
    return status;
  }

  long version() {
    return Objects.requireNonNull(version, "A firing has a version once it's saved");
  }

  @Nullable Instant startedAt() {
    return startedAt;
  }

  @Nullable Instant finishedAt() {
    return finishedAt;
  }

  /** The pieces in the load, ordered by id. Loads its piece rows if they aren't loaded yet. */
  List<UUID> pieceIds() {
    return pieces.stream().map(FiringPiece::pieceId).toList();
  }

  /** Switches the kiln on. Only a load that's still loading can start. */
  void start(Instant now) {
    if (status != FiringView.Status.LOADING) {
      throw new InvalidInputException("Only a load that's still loading can be started.");
    }
    status = FiringView.Status.FIRING;
    startedAt = now;
  }

  /** Unloads a firing in progress. Its pieces leave the load, free to be loaded again later. */
  void complete(@Nullable String unloadNotes, Instant now) {
    if (status != FiringView.Status.FIRING) {
      throw new InvalidInputException("Only a firing in progress can be unloaded.");
    }
    finish(FiringView.Status.COMPLETE, unloadNotes, now);
  }

  /** Abandons a load before or during firing; its pieces go back to the queue unchanged. */
  void cancel(@Nullable String cancelNotes, Instant now) {
    if (!status.open()) {
      throw new InvalidInputException("This firing is already finished.");
    }
    finish(FiringView.Status.CANCELLED, cancelNotes, now);
  }

  private void finish(FiringView.Status finalStatus, @Nullable String finishNotes, Instant now) {
    String cleanedNotes = finishNotes == null || finishNotes.isBlank() ? null : finishNotes.strip();
    if (cleanedNotes != null && cleanedNotes.length() > MAX_NOTES) {
      throw new InvalidInputException("Keep notes under 1,000 characters.");
    }
    status = finalStatus;
    finishedAt = now;
    if (cleanedNotes != null) {
      notes = cleanedNotes;
    }
    pieces.forEach(FiringPiece::close);
  }

  /**
   * The view the API returns.
   *
   * @param pieceCount how many pieces are in the load, counted by the caller so lists stay at a
   *     fixed number of queries
   * @param loaded the pieces themselves, or none for a list
   */
  FiringView toView(int pieceCount, List<PieceView> loaded) {
    return new FiringView(
        id,
        kiln.id(),
        kiln.name(),
        kind,
        cone,
        status,
        notes,
        Objects.requireNonNull(createdAt, "A firing has a creation time once it's saved"),
        startedAt,
        finishedAt,
        pieceCount,
        version(),
        loaded);
  }
}
