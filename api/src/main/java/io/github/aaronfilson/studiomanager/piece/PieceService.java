package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.firing.FeeQuote;
import io.github.aaronfilson.studiomanager.firing.FeeService;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Members' pieces in the bound studio. Staff see and move every piece; members see only their own
 * and can drop off only their own.
 *
 * <p>Other modules call this class; moves are {@link PieceMoves}, and turning pieces into views is
 * {@link PieceViews}. The SQL this module keeps is in {@link PieceQueries} (ADR 21).
 */
@Service
public class PieceService {

  /** The most pieces {@link #list} returns. */
  public static final int LIST_LIMIT = 2000;

  /** The most pieces {@link #waitingIn} returns. */
  public static final int QUEUE_LIMIT = 1000;

  private final PieceRepository pieces;
  private final PieceQueries queries;
  private final PieceTimeline timeline;
  private final PieceViews views;
  private final PieceMoves moves;
  private final FeeService fees;
  private final StudioMembers members;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final StudioPermissions permissions;

  PieceService(
      PieceRepository pieces,
      PieceQueries queries,
      PieceTimeline timeline,
      PieceViews views,
      PieceMoves moves,
      FeeService fees,
      StudioMembers members,
      ApplicationEventPublisher events,
      Clock clock,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.pieces = pieces;
    this.queries = queries;
    this.timeline = timeline;
    this.views = views;
    this.moves = moves;
    this.fees = fees;
    this.members = members;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Takes in a piece: numbers it, prices it with the current fee schedule, and starts its timeline.
   */
  @Transactional
  public PieceView intake(PieceIntake intake) {
    TenantContext caller = TenantContext.require();
    UUID requestedMaker = intake.makerAccountId();
    UUID maker = requestedMaker == null ? caller.accountId() : requestedMaker;
    if (!maker.equals(caller.accountId())) {
      permissions.require(
          Permission.PIECES_INTAKE_FOR_OTHERS, "Only staff can take in a piece for someone else.");
      if (members.roleOf(maker).isEmpty()) {
        throw new InvalidInputException("The maker isn't a member of this studio.");
      }
    }
    FeeQuote fee = fees.quote(intake.size());
    Piece.Description described =
        new Piece.Description(
            clean(intake.description(), 200, "description"),
            clean(intake.clayBody(), 80, "clay body"),
            clean(intake.makerMark(), 80, "maker's mark"));
    Instant now = clock.instant();
    int number = queries.nextNumber(caller.requireStudioId());
    Piece piece = pieces.save(new Piece(number, maker, described, intake.size(), fee, now));
    timeline.record(piece.id(), null, PieceStage.RECEIVED, null, caller.accountId(), now);
    // Listeners write rows that point at the piece, some through SQL (ADR 21)
    pieces.flush();
    events.publishEvent(
        new PieceReceived(piece.id(), number, maker, fee.amount(), fee.taxCategory(), now));
    return views.detailed(piece, timeline.of(piece.id()));
  }

  /** Pieces, newest first, optionally filtered. Members only ever see their own. */
  @Transactional(readOnly = true)
  public Capped<PieceView> list(@Nullable PieceStage stage, @Nullable UUID maker) {
    TenantContext caller = TenantContext.require();
    UUID makerFilter = seesEveryPiece(caller) ? maker : caller.accountId();
    Capped<Piece> found =
        Capped.fromFetched(
            pieces.findNewest(stage, makerFilter, Limit.of(Capped.fetchSize(LIST_LIMIT))),
            LIST_LIMIT);
    return found.withItems(views.listed(found.items()));
  }

  /** One piece and its timeline. A member asking for someone else's piece gets a 404. */
  @Transactional(readOnly = true)
  public PieceView get(UUID pieceId) {
    Piece piece = findVisible(pieceId);
    return views.detailed(piece, timeline.of(pieceId));
  }

  /**
   * Moves a piece to {@code target}, if that move is allowed from where it is now. Reaching READY
   * starts the hold: the piece is held through the end of the studio's local day, hold-days later.
   *
   * @param expectedVersion the version the caller last saw, so two people can't overwrite each
   *     other
   */
  @Transactional
  public PieceView move(
      UUID pieceId, PieceStage target, @Nullable String note, long expectedVersion) {
    TenantContext caller =
        permissions.require(Permission.PIECES_MOVE, "Only staff can move pieces.");
    Piece piece =
        moves.move(pieceId, target, clean(note, 500, "note"), expectedVersion, caller.accountId());
    return views.detailed(piece, timeline.of(pieceId));
  }

  /**
   * Pieces in {@code stage}, longest-waiting first, with when each reached that stage. This is what
   * a kiln queue is built from. Staff only.
   */
  @Transactional(readOnly = true)
  public Capped<WaitingPiece> waitingIn(PieceStage stage) {
    permissions.require(Permission.FIRINGS_MANAGE, "Only staff can see the kiln queue.");
    Capped<PieceQueries.Waiting> waiting =
        Capped.fromFetched(queries.waitingIn(stage, Capped.fetchSize(QUEUE_LIMIT)), QUEUE_LIMIT);
    List<UUID> ids = waiting.items().stream().map(PieceQueries.Waiting::pieceId).toList();
    Map<UUID, PieceView> byId =
        views.listed(pieces.findAllInNumberOrder(ids)).stream()
            .collect(Collectors.toMap(PieceView::id, Function.identity()));
    List<WaitingPiece> result = new ArrayList<>(ids.size());
    for (PieceQueries.Waiting entry : waiting.items()) {
      PieceView piece = byId.get(entry.pieceId());
      // Gone between the two reads only if it was removed meanwhile; pieces are never deleted
      if (piece != null) {
        result.add(new WaitingPiece(piece, entry.since()));
      }
    }
    return waiting.withItems(result);
  }

  /**
   * The pieces with these ids, in number order, as lists show them: their first photo and no
   * timeline. One query for the pieces however many there are, for a kiln load. Staff only.
   */
  @Transactional(readOnly = true)
  public List<PieceView> listByIds(Collection<UUID> pieceIds) {
    permissions.require(Permission.FIRINGS_MANAGE, "Only staff can see what's in a load.");
    if (pieceIds.isEmpty()) {
      return List.of();
    }
    return views.listed(pieces.findAllInNumberOrder(pieceIds));
  }

  /**
   * Moves every piece in {@code pieceIds} from {@code from} to {@code to} after a firing, with one
   * timeline entry each. All or nothing: if any piece is no longer in {@code from}, nothing moves.
   * Returns the pieces as they are afterwards.
   */
  @Transactional
  public List<PieceView> advanceAll(
      Collection<UUID> pieceIds, PieceStage from, PieceStage to, String note) {
    TenantContext caller =
        permissions.require(Permission.PIECES_MOVE, "Only staff can move pieces.");
    return views.listed(moves.advanceAll(pieceIds, from, to, note, caller.accountId()));
  }

  /** How many pieces are in each stage, and how many ready pieces are past their hold. */
  @Transactional(readOnly = true)
  public StageCounts counts() {
    Map<PieceStage, Integer> byStage = new EnumMap<>(PieceStage.class);
    for (PieceStageCount count : pieces.countByStage()) {
      byStage.put(count.stage(), Math.toIntExact(count.pieces()));
    }
    int overdue = Math.toIntExact(pieces.countPastHold(clock.instant()));
    return new StageCounts(byStage, overdue);
  }

  public record StageCounts(Map<PieceStage, Integer> byStage, int overdue) {

    public int in(PieceStage stage) {
      return byStage.getOrDefault(stage, 0);
    }
  }

  /** A piece waiting in a stage, and since when. */
  public record WaitingPiece(PieceView piece, Instant since) {}

  @Transactional(readOnly = true)
  public int holdDays() {
    return moves.holdDays();
  }

  @Transactional
  public int setHoldDays(int days) {
    TenantContext caller =
        permissions.require(
            Permission.PIECES_POLICY_EDIT, "Only staff can change how long pieces are held.");
    if (days < 1 || days > 365) {
      throw new InvalidInputException("Hold pieces for 1 to 365 days.");
    }
    queries.saveHoldDays(caller.requireStudioId(), days, clock.instant());
    return days;
  }

  /** The end of the studio's local day, {@code days} days after {@code readyAt}. */
  static Instant holdEnd(Instant readyAt, int days, ZoneId zone) {
    return PieceMoves.holdEnd(readyAt, days, zone);
  }

  /** A piece the caller may see: staff see every piece, members only their own. */
  Piece findVisible(UUID pieceId) {
    TenantContext caller = TenantContext.require();
    return pieces
        .findById(pieceId)
        .filter(
            found -> seesEveryPiece(caller) || found.makerAccountId().equals(caller.accountId()))
        .orElseThrow(() -> new NotFoundException("No such piece."));
  }

  private static @Nullable String clean(@Nullable String text, int maxLength, String field) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String stripped = text.strip();
    if (stripped.length() > maxLength) {
      throw new InvalidInputException(
          "The %s can be at most %d characters.".formatted(field, maxLength));
    }
    return stripped;
  }

  /**
   * Whether {@code caller} sees everyone's pieces rather than only their own. Whoever may move
   * pieces through the studio sees them all; the catalogue has no separate permission for it.
   */
  private static boolean seesEveryPiece(TenantContext caller) {
    return caller.holds(Permission.PIECES_MOVE);
  }
}
