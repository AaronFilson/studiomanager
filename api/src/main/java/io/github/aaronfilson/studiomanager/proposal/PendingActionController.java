package io.github.aaronfilson.studiomanager.proposal;

import io.github.aaronfilson.studiomanager.contract.DecidePendingActionRequestDto;
import io.github.aaronfilson.studiomanager.contract.PendingActionDto;
import io.github.aaronfilson.studiomanager.contract.PendingActionKindDto;
import io.github.aaronfilson.studiomanager.contract.PendingActionListDto;
import io.github.aaronfilson.studiomanager.contract.PendingActionStatusDto;
import io.github.aaronfilson.studiomanager.contract.PendingActionsApi;
import io.github.aaronfilson.studiomanager.contract.ProposedMaintenanceTaskDto;
import io.github.aaronfilson.studiomanager.contract.ProposedPurchaseRequestDto;
import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** The "Assistant suggestions" queue. Never reachable with an API token (TokenPolicy). */
@RestController
class PendingActionController implements PendingActionsApi {

  private final PendingActionReview review;

  PendingActionController(PendingActionReview review) {
    this.review = review;
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<PendingActionListDto> listPendingActions(UUID studioId) {
    requireBoundTo(studioId);
    Capped<PendingActionDto> waiting = review.waiting().map(PendingActionController::toDto);
    return ResponseEntity.ok(
        new PendingActionListDto(waiting.items(), waiting.truncated(), waiting.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
  public ResponseEntity<PendingActionDto> decidePendingAction(
      UUID studioId, UUID actionId, DecidePendingActionRequestDto request) {
    requireBoundTo(studioId);
    long version = request.getExpectedVersion();
    PendingActionView decided =
        switch (request.getDecision()) {
          case APPROVED -> review.approve(actionId, version);
          case REJECTED -> review.reject(actionId, version, request.getNote());
        };
    return ResponseEntity.ok(toDto(decided));
  }

  /**
   * The studio in the path is the one the request is bound to. {@code TenantContextFilter} already
   * binds it from the path; this says so where the path is read.
   */
  private static void requireBoundTo(UUID studioId) {
    if (!studioId.equals(TenantContext.require().studioId())) {
      throw new AccessDeniedException("This request isn't bound to that studio.");
    }
  }

  private static PendingActionDto toDto(PendingActionView action) {
    PendingActionDto dto =
        new PendingActionDto()
            .id(action.id())
            .kind(PendingActionKindDto.fromValue(action.kind().name()))
            .status(PendingActionStatusDto.fromValue(action.status().name()))
            .reason(action.reason())
            .subjectName(action.subjectName())
            .proposedBy(action.proposedBy())
            .proposedByName(action.proposedByName())
            .proposedViaTokenId(action.proposedViaToken())
            .proposedViaTokenName(action.proposedViaTokenName())
            .proposedAt(utc(action.proposedAt()))
            .decidedByName(action.decidedByName())
            .decidedAt(utcOrNull(action.decidedAt()))
            .decisionNote(action.decisionNote())
            .resultId(action.resultId())
            .version(action.version());
    PurchaseRequestDraft purchase = action.purchaseRequest();
    if (purchase != null) {
      dto.purchaseRequest(
          new ProposedPurchaseRequestDto()
              .itemId(purchase.itemId())
              .quantity(purchase.quantity().stripTrailingZeros().toPlainString())
              .note(purchase.note()));
    }
    MaintenanceTaskDraft task = action.maintenanceTask();
    if (task != null) {
      dto.maintenanceTask(
          new ProposedMaintenanceTaskDto()
              .assetId(task.assetId())
              .title(task.title())
              .everyDays(task.everyDays())
              .everyFirings(task.everyFirings()));
    }
    return dto;
  }

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  private static @Nullable OffsetDateTime utcOrNull(@Nullable Instant instant) {
    return instant == null ? null : utc(instant);
  }
}
