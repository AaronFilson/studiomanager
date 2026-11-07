package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.apitoken.ApiTokens;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Movement;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.PurchaseRequest;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clay and supplies in the bound studio: what other modules read, and the one change they make
 * (maintenance uses parts). Stock on hand is the sum of movements, never a stored number someone
 * can forget to update.
 *
 * <p>The other changes are in this package, one service per use case: {@link SupplyItems}, {@link
 * StockMovements}, {@link SupplySales} and {@link PurchaseRequests}. Each list here is a fixed
 * number of queries however long it is.
 */
@Service
public class SupplyService {

  /** The most movements {@link #movements} returns. */
  static final int STOCK_HISTORY_LIMIT = 1000;

  /** Requests whose stock is on its way: approved or ordered, and not yet received. */
  static final Set<RequestStatus> ON_ORDER =
      EnumSet.of(RequestStatus.APPROVED, RequestStatus.ORDERED);

  private static final Set<RequestStatus> OPEN =
      EnumSet.of(RequestStatus.REQUESTED, RequestStatus.APPROVED, RequestStatus.ORDERED);

  private final SupplyItemRepository itemRepository;
  private final StockMovementRepository movementRepository;
  private final PurchaseRequestRepository requestRepository;
  private final StockMovements stock;
  private final AccountDirectory accounts;
  private final ApiTokens tokens;
  private final StudioPermissions permissions;

  SupplyService(
      SupplyItemRepository itemRepository,
      StockMovementRepository movementRepository,
      PurchaseRequestRepository requestRepository,
      StockMovements stock,
      AccountDirectory accounts,
      ApiTokens tokens,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.itemRepository = itemRepository;
    this.movementRepository = movementRepository;
    this.requestRepository = requestRepository;
    this.stock = stock;
    this.accounts = accounts;
    this.tokens = tokens;
  }

  /**
   * Every item, low stock first. Members see the list too, so they know what's in and what to use
   * instead.
   */
  @Transactional(readOnly = true)
  public List<Item> items() {
    return itemRepository.findActiveWithStock(ON_ORDER).stream()
        .map(SupplyItemStock::toView)
        .sorted(Comparator.comparing(Item::low).reversed())
        .toList();
  }

  @Transactional(readOnly = true)
  public Item item(UUID id) {
    return itemRepository
        .findWithStock(id, ON_ORDER)
        .map(SupplyItemStock::toView)
        .orElseThrow(() -> new NotFoundException("No such item."));
  }

  /** Items at or below their par level with nothing already on order. */
  @Transactional(readOnly = true)
  public List<Item> needingOrder() {
    return items().stream().filter(Item::needsOrdering).toList();
  }

  /**
   * Records stock coming in, being used, or a count. For a count, {@code quantity} is what is on
   * the shelf, and the movement records the difference from the books.
   */
  @Transactional
  public Item recordMovement(
      UUID itemId, MovementKind kind, BigDecimal quantity, @Nullable String note) {
    permissions.require(Permission.SUPPLIES_MANAGE, ONLY_STAFF);
    stock.adjust(itemId, kind, quantity, note);
    return item(itemId);
  }

  /** An item's movements, newest first, with the names of members who bought. */
  @Transactional(readOnly = true)
  public Capped<Movement> movements(UUID itemId) {
    permissions.require(Permission.SUPPLIES_MANAGE, ONLY_STAFF);
    if (!itemRepository.existsById(itemId)) {
      throw new NotFoundException("No such item.");
    }
    List<StockMovement> fetched =
        movementRepository.findByItemIdOrderByCreatedAtDescIdDesc(
            itemId, Limit.of(Capped.fetchSize(STOCK_HISTORY_LIMIT)));
    Map<UUID, String> names =
        accounts.displayNames(
            fetched.stream().map(StockMovement::memberId).filter(Objects::nonNull).toList());
    return Capped.fromFetched(
        fetched.stream()
            .map(movement -> movement.toView(nameOf(movement.memberId(), names)))
            .toList(),
        STOCK_HISTORY_LIMIT);
  }

  /** Open requests (requested, approved, ordered), oldest first. */
  @Transactional(readOnly = true)
  public List<PurchaseRequest> openRequests() {
    permissions.require(Permission.SUPPLIES_PURCHASE_REQUEST, ONLY_STAFF);
    return views(requestRepository.findWithItemByStatusIn(OPEN));
  }

  /** One request as the API returns it, for {@link PurchaseRequests}. */
  PurchaseRequest view(PurchaseRequestEntity request) {
    return views(List.of(request)).getFirst();
  }

  private List<PurchaseRequest> views(List<PurchaseRequestEntity> requests) {
    List<UUID> people = new ArrayList<>();
    List<UUID> viaTokens = new ArrayList<>();
    for (PurchaseRequestEntity request : requests) {
      people.add(request.requestedBy());
      UUID decider = request.decidedBy();
      if (decider != null) {
        people.add(decider);
      }
      UUID requestedVia = request.requestedViaTokenId();
      if (requestedVia != null) {
        viaTokens.add(requestedVia);
      }
      UUID decidedVia = request.decidedViaTokenId();
      if (decidedVia != null) {
        viaTokens.add(decidedVia);
      }
    }
    Map<UUID, String> names = accounts.displayNames(people);
    Map<UUID, String> tokenNames = tokens.names(viaTokens);
    return requests.stream()
        .map(request ->
            request.toView(
                names.get(request.requestedBy()),
                nameOf(request.requestedViaTokenId(), tokenNames),
                nameOf(request.decidedBy(), names),
                nameOf(request.decidedViaTokenId(), tokenNames)))
        .toList();
  }

  /** The name of an account or API token, looked up by id; null when there's no id. */
  private static @Nullable String nameOf(@Nullable UUID id, Map<UUID, String> names) {
    return id == null ? null : names.get(id);
  }

  // ---- Shared by this package's services

  /** Why a change only staff may make was refused. */
  static final String ONLY_STAFF = "Only staff can do this.";

  static BigDecimal positive(BigDecimal quantity) {
    if (quantity.signum() <= 0) {
      throw new InvalidInputException("The quantity must be more than zero.");
    }
    return quantity;
  }

  /** Trimmed, cut to 200 characters, and null if blank. */
  static @Nullable String clean(@Nullable String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String stripped = text.strip();
    return stripped.length() > 200 ? stripped.substring(0, 200) : stripped;
  }
}
