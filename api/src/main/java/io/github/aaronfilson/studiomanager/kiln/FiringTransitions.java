package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceView;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Running a load: starting it, unloading it, or cancelling it.
 *
 * <p>Unloading is one transaction: the pieces move on, the kiln's count goes up, and {@link
 * FiringCompleted} is published so the notify module writes each maker one notice for the whole
 * batch. If any part fails, none of it happens.
 *
 * <p>Each change is flushed before other modules' SQL and the synchronous listeners run, since they
 * read and write through {@code JdbcClient}, which can't see unflushed changes (ADR 21).
 */
@Service
class FiringTransitions {

  private final FiringRepository firings;
  private final FiringService reads;
  private final PieceService pieces;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  FiringTransitions(
      FiringRepository firings,
      FiringService reads,
      PieceService pieces,
      ApplicationEventPublisher events,
      Clock clock) {
    this.firings = firings;
    this.reads = reads;
    this.pieces = pieces;
    this.events = events;
    this.clock = clock;
  }

  /** Marks a load as firing. */
  @Transactional
  public FiringView start(UUID firingId, long expectedVersion) {
    Firing firing = findAtVersion(firingId, expectedVersion);
    Instant now = clock.instant();
    firing.start(now);
    firings.flush();
    Kiln kiln = firing.kiln();
    events.publishEvent(
        new FiringStarted(studioId(), firing.id(), kiln.id(), kiln.name(), firing.kind(), now));
    return reads.view(firing);
  }

  /**
   * Unloads a firing: moves its pieces on, counts the firing, and notifies makers once per batch.
   */
  @Transactional
  public FiringView complete(UUID firingId, @Nullable String notes, long expectedVersion) {
    Firing firing = findAtVersion(firingId, expectedVersion);
    Instant now = clock.instant();
    firing.complete(notes, now);
    Kiln kiln = firing.kiln();
    kiln.countFiring();
    firings.flush();
    FiringKind kind = firing.kind();
    List<PieceView> moved =
        pieces.advanceAll(
            firing.pieceIds(),
            kind.takes(),
            kind.leaves(),
            "%s firing in %s".formatted(describe(kind), kiln.name()));
    events.publishEvent(
        new FiringCompleted(
            studioId(),
            firing.id(),
            kind,
            kiln.id(),
            kiln.name(),
            moved.stream()
                .map(piece ->
                    new FiringCompleted.FiredPiece(
                        piece.id(),
                        piece.number(),
                        piece.makerAccountId(),
                        piece.description(),
                        piece.holdUntil()))
                .toList()));
    events.publishEvent(new FiringEnded(studioId(), firing.id(), kiln.id(), now, true));
    return reads.view(firing);
  }

  /** Abandons a load before or during firing; its pieces go back to the queue unchanged. */
  @Transactional
  public FiringView cancel(UUID firingId, @Nullable String notes, long expectedVersion) {
    Firing firing = findAtVersion(firingId, expectedVersion);
    Instant now = clock.instant();
    firing.cancel(notes, now);
    firings.flush();
    events.publishEvent(new FiringEnded(studioId(), firing.id(), firing.kiln().id(), now, false));
    return reads.view(firing);
  }

  /**
   * Loads a firing and checks it's still the version the client saw. {@code @Version} then catches
   * anyone who changes it between this load and the commit.
   */
  private Firing findAtVersion(UUID firingId, long expectedVersion) {
    Firing firing = reads.find(firingId);
    if (firing.version() != expectedVersion) {
      throw new ConflictException(
          "Someone else changed this firing at the same time. Reload and try again.");
    }
    return firing;
  }

  private static String describe(FiringKind kind) {
    return switch (kind) {
      case BISQUE -> "Bisque";
      case GLAZE -> "Glaze";
    };
  }

  private static UUID studioId() {
    return TenantContext.require().requireStudioId();
  }
}
