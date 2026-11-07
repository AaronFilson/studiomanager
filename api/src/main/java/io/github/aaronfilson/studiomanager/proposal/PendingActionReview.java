package io.github.aaronfilson.studiomanager.proposal;

import io.github.aaronfilson.studiomanager.apitoken.ApiTokens;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceModel.TaskSettings;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceService;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.proposal.PendingActionView.Status;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "Assistant suggestions" queue: staff see what's waiting and approve or reject it.
 *
 * <p>Approving runs the owning module's normal service call, in this transaction, as the person who
 * approved. If that call refuses (the item was archived, or the asset is gone), nothing is saved
 * and the suggestion stays pending, for staff to reject.
 */
@Service
class PendingActionReview {

  /** The most suggestions the queue shows at once. */
  static final int QUEUE_LIMIT = 100;

  private final PendingActionRepository pendingActions;
  private final SupplyService supplies;
  private final MaintenanceService maintenance;
  private final AccountDirectory accounts;
  private final ApiTokens tokens;
  private final Clock clock;

  PendingActionReview(
      PendingActionRepository pendingActions,
      SupplyService supplies,
      MaintenanceService maintenance,
      AccountDirectory accounts,
      ApiTokens tokens,
      Clock clock) {
    this.pendingActions = pendingActions;
    this.supplies = supplies;
    this.maintenance = maintenance;
    this.accounts = accounts;
    this.tokens = tokens;
    this.clock = clock;
  }

  /** Suggestions waiting for a decision, oldest first. */
  @Transactional(readOnly = true)
  public Capped<PendingActionView> waiting() {
    requireStaff();
    List<PendingAction> fetched =
        pendingActions.findByStatusOrderByCreatedAtAscIdAsc(
            Status.PENDING, Limit.of(Capped.fetchSize(QUEUE_LIMIT)));
    Capped<PendingAction> capped = Capped.fromFetched(fetched, QUEUE_LIMIT);
    return capped.withItems(views(capped.items()));
  }

  /** Approves a suggestion: makes the change it describes, as the caller. */
  @Transactional
  public PendingActionView approve(UUID actionId, long expectedVersion) {
    TenantContext caller = requireStaff();
    PendingAction action = findAtVersion(actionId, expectedVersion);
    // Before making anything, so a decided suggestion is never applied twice
    action.requirePending();
    UUID madeId =
        switch (action.kind()) {
          case PURCHASE_REQUEST -> {
            PurchaseRequestDraft draft = action.requirePurchaseRequest();
            yield supplies.request(draft.itemId(), draft.quantity(), draft.note()).id();
          }
          case MAINTENANCE_TASK -> {
            MaintenanceTaskDraft draft = action.requireMaintenanceTask();
            yield maintenance
                .createTask(
                    draft.assetId(),
                    new TaskSettings(
                        draft.title(),
                        List.of(),
                        draft.everyDays(),
                        draft.everyFirings(),
                        null,
                        null))
                .id();
          }
        };
    action.approve(caller.accountId(), clock.instant(), madeId);
    pendingActions.flush();
    return views(List.of(action)).getFirst();
  }

  /** Turns a suggestion down, with an optional note why. Nothing else changes. */
  @Transactional
  public PendingActionView reject(UUID actionId, long expectedVersion, @Nullable String note) {
    TenantContext caller = requireStaff();
    PendingAction action = findAtVersion(actionId, expectedVersion);
    action.reject(caller.accountId(), clock.instant(), note);
    pendingActions.flush();
    return views(List.of(action)).getFirst();
  }

  /** Views with the people's and tokens' names, looked up once for the whole list. */
  private List<PendingActionView> views(List<PendingAction> actions) {
    Set<UUID> people = new HashSet<>();
    Set<UUID> tokenIds = new HashSet<>();
    for (PendingAction action : actions) {
      people.add(action.proposedBy());
      UUID decider = action.decidedBy();
      if (decider != null) {
        people.add(decider);
      }
      UUID token = action.proposedViaToken();
      if (token != null) {
        tokenIds.add(token);
      }
    }
    Map<UUID, String> names = accounts.displayNames(people);
    Map<UUID, String> tokenNames = tokens.names(tokenIds);
    return actions.stream().map(action -> action.toView(names, tokenNames)).toList();
  }

  private PendingAction findAtVersion(UUID actionId, long expectedVersion) {
    PendingAction action =
        pendingActions
            .findById(actionId)
            .orElseThrow(() -> new NotFoundException("No such suggestion."));
    if (action.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else decided this suggestion at the same time. Reload and try again.");
    }
    return action;
  }

  private static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    StudioRole role = caller.role();
    if (role == null || !role.includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff review assistant suggestions.");
    }
    return caller;
  }
}
