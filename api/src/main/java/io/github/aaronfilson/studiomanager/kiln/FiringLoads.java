package io.github.aaronfilson.studiomanager.kiln;

import io.github.aaronfilson.studiomanager.piece.PieceService;
import io.github.aaronfilson.studiomanager.piece.PieceService.WaitingPiece;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.UniqueViolations;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A kiln's queue, and building a load from it. */
@Service
class FiringLoads {

  private final FiringRepository firings;
  private final KilnService kilns;
  private final FiringService reads;
  private final PieceService pieces;

  FiringLoads(
      FiringRepository firings, KilnService kilns, FiringService reads, PieceService pieces) {
    this.firings = firings;
    this.kilns = kilns;
    this.reads = reads;
    this.pieces = pieces;
  }

  /**
   * The queue for {@code kind} firings in {@code kilnId}: every waiting piece not already in a
   * load, longest-waiting first, marked by whether the kiln's per-member cap lets it into the next
   * load.
   */
  @Transactional(readOnly = true)
  public Capped<KilnQueue.Entry<WaitingPiece>> queue(UUID kilnId, FiringKind kind) {
    return queue(kilns.find(kilnId), kind);
  }

  /**
   * Starts a load with the given pieces. They must be waiting for this kind of firing and within
   * the cap.
   */
  @Transactional
  public FiringView load(UUID kilnId, FiringKind kind, @Nullable String cone, List<UUID> pieceIds) {
    Kiln kiln = kilns.find(kilnId);
    String downReason = kiln.downReason();
    if (downReason != null) {
      throw new InvalidInputException(
          "%s is out of service: %s".formatted(kiln.name(), downReason));
    }
    Set<UUID> chosen = new LinkedHashSet<>(pieceIds);
    if (chosen.isEmpty()) {
      throw new InvalidInputException("Choose at least one piece to load.");
    }
    Map<UUID, KilnQueue.Entry<WaitingPiece>> queued =
        queue(kiln, kind).items().stream()
            .collect(Collectors.toMap(entry -> entry.piece().piece().id(), Function.identity()));
    if (!queued.keySet().containsAll(chosen)) {
      throw new InvalidInputException(
          "A piece you chose isn't waiting for a %s firing, or is already loaded."
              .formatted(kind.name().toLowerCase(Locale.ROOT)));
    }
    Integer cap = kiln.maxPiecesPerMember();
    if (cap != null) {
      int perMemberCap = cap;
      Map<UUID, Long> perMaker =
          chosen.stream()
              .collect(
                  Collectors.groupingBy(
                      pieceId -> makerOf(queued, pieceId), Collectors.counting()));
      if (perMaker.values().stream().anyMatch(count -> count > perMemberCap)) {
        throw new InvalidInputException(
            "This kiln takes at most %d pieces per member in one load.".formatted(perMemberCap));
      }
    }
    Firing firing = new Firing(kiln, kind, cone, chosen);
    try {
      firings.saveAndFlush(firing);
    } catch (DataIntegrityViolationException e) {
      if (UniqueViolations.isUniqueViolation(e)) {
        throw new ConflictException(
            "That kiln already has a load in progress, or a piece was just loaded elsewhere.");
      }
      throw e;
    }
    return reads.view(firing);
  }

  private Capped<KilnQueue.Entry<WaitingPiece>> queue(Kiln kiln, FiringKind kind) {
    Set<UUID> loaded = new HashSet<>(firings.findPiecesInOpenLoads());
    Capped<WaitingPiece> waitingInStage = pieces.waitingIn(kind.takes());
    List<WaitingPiece> waiting =
        waitingInStage.items().stream()
            .filter(candidate -> !loaded.contains(candidate.piece().id()))
            .toList();
    return waitingInStage.withItems(
        KilnQueue.plan(
            waiting, candidate -> candidate.piece().makerAccountId(), kiln.maxPiecesPerMember()));
  }

  private static UUID makerOf(Map<UUID, KilnQueue.Entry<WaitingPiece>> queued, UUID pieceId) {
    KilnQueue.Entry<WaitingPiece> entry = queued.get(pieceId);
    if (entry == null) {
      throw new IllegalStateException("Every chosen piece was checked to be in the queue");
    }
    return entry.piece().piece().makerAccountId();
  }
}
