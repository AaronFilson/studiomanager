package io.github.aaronfilson.studiomanager.proposal;

import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.Asset;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.Item;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records what an assistant proposes, for staff to approve or reject in the app. Nothing here
 * changes the studio: the proposal is checked now, so an obviously wrong one fails back to the
 * assistant, and checked again by the normal service call when someone approves it.
 */
@Service
public class Proposals {

  private static final int MAX_EVERY_DAYS = 3650;
  private static final int MAX_EVERY_FIRINGS = 10_000;

  /** A purchase request's note, as the Supplies page allows. */
  public static final int MAX_PURCHASE_NOTE = 200;

  private final PendingActionRepository pendingActions;
  private final SupplyService supplies;
  private final MaintenanceService maintenance;
  private final Clock clock;

  Proposals(
      PendingActionRepository pendingActions,
      SupplyService supplies,
      MaintenanceService maintenance,
      Clock clock) {
    this.pendingActions = pendingActions;
    this.supplies = supplies;
    this.maintenance = maintenance;
    this.clock = clock;
  }

  /**
   * Proposes asking for more of a supply item. Without a quantity, the item's usual reorder
   * quantity, fixed now so staff see the number they're approving.
   */
  @Transactional
  public PendingActionView proposePurchaseRequest(
      UUID itemId, @Nullable BigDecimal quantity, @Nullable String note, @Nullable String reason) {
    requireStaff();
    Item item = supplies.item(itemId);
    BigDecimal amount = quantity != null ? quantity : item.reorderQuantity();
    if (amount == null) {
      throw new InvalidInputException(
          "How many? This item has no usual reorder quantity, so give a quantity.");
    }
    if (amount.signum() <= 0) {
      throw new InvalidInputException("The quantity must be more than zero.");
    }
    PurchaseRequestDraft draft =
        new PurchaseRequestDraft(
            itemId, amount, PendingAction.cleanText(note, "note", MAX_PURCHASE_NOTE));
    return save(PendingAction.purchaseRequest(draft, item.name(), reason, proposingToken()));
  }

  /**
   * Proposes a recurring maintenance task on an asset, due every so many days, firings, or both.
   */
  @Transactional
  public PendingActionView proposeMaintenanceTask(
      UUID assetId,
      String title,
      @Nullable Integer everyDays,
      @Nullable Integer everyFirings,
      @Nullable String reason) {
    requireStaff();
    Asset asset = maintenance.asset(assetId, clock.instant());
    String cleanTitle = title.strip();
    if (cleanTitle.isEmpty() || cleanTitle.length() > 120) {
      throw new InvalidInputException("A task needs a title of 1 to 120 characters.");
    }
    if (everyDays == null && everyFirings == null) {
      throw new InvalidInputException("Say how often: every so many days, firings, or both.");
    }
    if (everyDays != null && (everyDays < 1 || everyDays > MAX_EVERY_DAYS)) {
      throw new InvalidInputException("Every 1 to %d days.".formatted(MAX_EVERY_DAYS));
    }
    if (everyFirings != null && (everyFirings < 1 || everyFirings > MAX_EVERY_FIRINGS)) {
      throw new InvalidInputException("Every 1 to %d firings.".formatted(MAX_EVERY_FIRINGS));
    }
    if (everyFirings != null && asset.kilnId() == null) {
      throw new InvalidInputException("Only kilns count firings. Use days for this one.");
    }
    MaintenanceTaskDraft draft =
        new MaintenanceTaskDraft(assetId, cleanTitle, everyDays, everyFirings);
    return save(PendingAction.maintenanceTask(draft, asset.name(), reason, proposingToken()));
  }

  private PendingActionView save(PendingAction action) {
    pendingActions.saveAndFlush(action);
    // The assistant learns only that it's proposed; names are for the review queue
    return action.toView(Map.of(), Map.of());
  }

  /** The personal API token this request signed in with, if any. */
  private static @Nullable UUID proposingToken() {
    return TenantContext.require().viaTokenId();
  }

  private static void requireStaff() {
    StudioRole role = TenantContext.require().role();
    if (role == null || !role.includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff can propose changes.");
    }
  }
}
