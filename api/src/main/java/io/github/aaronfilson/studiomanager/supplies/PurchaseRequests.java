package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.PurchaseRequest;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asking for more of an item, and carrying the request through: the owner approves or rejects it,
 * then staff mark it ordered and received, which puts the stock on the shelf. Public: an approved
 * assistant suggestion (the proposal module) asks through it.
 */
@Service
public class PurchaseRequests {

  private final PurchaseRequestRepository requestRepository;
  private final SupplyItemRepository itemRepository;
  private final StockMovements stock;
  private final SupplyService supplies;
  private final Clock clock;
  private final StudioPermissions permissions;

  PurchaseRequests(
      PurchaseRequestRepository requestRepository,
      SupplyItemRepository itemRepository,
      StockMovements stock,
      SupplyService supplies,
      Clock clock,
      StudioPermissions permissions) {
    this.permissions = permissions;
    this.requestRepository = requestRepository;
    this.itemRepository = itemRepository;
    this.stock = stock;
    this.supplies = supplies;
    this.clock = clock;
  }

  /** Asks for more of an item. Without a quantity, the item's usual reorder quantity. */
  @Transactional
  public PurchaseRequest request(
      UUID itemId, @Nullable BigDecimal quantity, @Nullable String note) {
    TenantContext caller =
        permissions.require(Permission.SUPPLIES_PURCHASE_REQUEST, SupplyService.ONLY_STAFF);
    SupplyItem item =
        itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("No such item."));
    BigDecimal amount =
        quantity != null ? SupplyService.positive(quantity) : item.reorderQuantity();
    if (amount == null) {
      throw new InvalidInputException("How many? This item has no usual reorder quantity.");
    }
    PurchaseRequestEntity request =
        requestRepository.saveAndFlush(
            new PurchaseRequestEntity(
                item, amount, SupplyService.clean(note), caller.viaTokenId()));
    return supplies.view(request);
  }

  /**
   * Moves a request on, if it's still the version the client saw. {@code @Version} then catches
   * anyone who changes it between this load and the commit.
   */
  @Transactional
  public PurchaseRequest decide(UUID requestId, RequestStatus to, long expectedVersion) {
    TenantContext caller =
        permissions.require(Permission.SUPPLIES_PURCHASE_REQUEST, SupplyService.ONLY_STAFF);
    PurchaseRequestEntity request =
        requestRepository
            .findWithItem(requestId)
            .orElseThrow(() -> new NotFoundException("No such purchase request."));
    if (request.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this request at the same time. Reload and try again.");
    }
    request.moveTo(
        to,
        caller.holds(Permission.SUPPLIES_PURCHASE_APPROVE),
        caller.accountId(),
        caller.viaTokenId(),
        clock.instant());
    requestRepository.flush();
    if (to == RequestStatus.RECEIVED) {
      stock.adjust(
          request.item().id(), MovementKind.RECEIVED, request.quantity(), "Purchase received");
    }
    return supplies.view(request);
  }

  /**
   * An item's stock came in another way, on a confirmed supplier invoice, which has already
   * recorded it. The item's oldest request on its way (ordered before approved) is marked received
   * without moving any stock, so marking it received later can't count the delivery twice, and the
   * item no longer shows as on order. The caller checked its own permission to receive stock.
   *
   * @return the request it settled, if the item had one on its way
   */
  @Transactional
  public Optional<PurchaseRequest> settleOnWay(UUID itemId) {
    TenantContext caller = TenantContext.require();
    List<PurchaseRequestEntity> onWay =
        requestRepository.findForItemByStatusIn(
            itemId, List.of(RequestStatus.ORDERED, RequestStatus.APPROVED));
    Optional<PurchaseRequestEntity> oldest =
        onWay.stream()
            .filter(request -> request.status() == RequestStatus.ORDERED)
            .findFirst()
            .or(() -> onWay.stream().findFirst());
    oldest.ifPresent(request ->
        request.moveTo(
            RequestStatus.RECEIVED,
            false,
            caller.accountId(),
            caller.viaTokenId(),
            clock.instant()));
    requestRepository.flush();
    return oldest.map(supplies::view);
  }
}
