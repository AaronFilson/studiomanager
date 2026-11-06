package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceView;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading firings: one load with its pieces, the loads in progress, and a kiln's firing log.
 *
 * <p>Building and running a load are {@link FiringLoads} and {@link FiringTransitions}. Each list
 * here is a fixed number of queries however long it is: the firings with their kilns, then one
 * count of pieces for all of them.
 */
@Service
public class FiringService {

  /** The most firings {@link #listForKiln} returns. */
  static final int FIRINGS_PER_KILN_LIMIT = 500;

  private final FiringRepository firings;
  private final KilnService kilns;
  private final PieceService pieces;

  FiringService(FiringRepository firings, KilnService kilns, PieceService pieces) {
    this.firings = firings;
    this.kilns = kilns;
    this.pieces = pieces;
  }

  /** Every load in progress (loading or firing), across the studio's kilns. */
  @Transactional(readOnly = true)
  public List<FiringView> open() {
    return withPieceCounts(
        firings.findByStatusIn(EnumSet.of(FiringView.Status.LOADING, FiringView.Status.FIRING)));
  }

  /** A kiln's firing log, newest first. */
  @Transactional(readOnly = true)
  public Capped<FiringView> listForKiln(UUID kilnId) {
    kilns.find(kilnId);
    return Capped.fromFetched(
        withPieceCounts(
            firings.findNewestForKiln(kilnId, Limit.of(Capped.fetchSize(FIRINGS_PER_KILN_LIMIT)))),
        FIRINGS_PER_KILN_LIMIT);
  }

  /** One firing, with the pieces in it. */
  @Transactional(readOnly = true)
  public FiringView get(UUID firingId) {
    return view(find(firingId));
  }

  /** For {@link FiringTransitions}, which changes a firing it has loaded. */
  Firing find(UUID firingId) {
    return firings
        .findWithKiln(firingId)
        .orElseThrow(() -> new NotFoundException("No such firing."));
  }

  /** A firing as the API returns it, with its pieces. */
  FiringView view(Firing firing) {
    List<UUID> pieceIds = firing.pieceIds();
    List<PieceView> loaded = pieces.listByIds(pieceIds);
    return firing.toView(pieceIds.size(), loaded);
  }

  private List<FiringView> withPieceCounts(List<Firing> found) {
    if (found.isEmpty()) {
      return List.of();
    }
    Map<UUID, Long> counts =
        firings.countPieces(found.stream().map(Firing::id).toList()).stream()
            .collect(Collectors.toMap(FiringPieceCount::firingId, FiringPieceCount::pieces));
    return found.stream()
        .map(firing ->
            firing.toView(Math.toIntExact(counts.getOrDefault(firing.id(), 0L)), List.of()))
        .toList();
  }
}
