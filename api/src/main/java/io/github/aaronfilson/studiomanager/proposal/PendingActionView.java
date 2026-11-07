package io.github.aaronfilson.studiomanager.proposal;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A change an assistant proposed, and what became of it.
 *
 * @param reason why the assistant suggests it, in its own words: untrusted text, shown as is
 * @param subjectName the item's or asset's name when it was proposed
 * @param purchaseRequest present exactly when the kind is {@link Kind#PURCHASE_REQUEST}
 * @param maintenanceTask present exactly when the kind is {@link Kind#MAINTENANCE_TASK}
 * @param proposedViaToken the personal API token the assistant used, if it used one
 * @param resultId what approving made: the purchase request's or the task's id
 */
public record PendingActionView(
    UUID id,
    Kind kind,
    Status status,
    @Nullable String reason,
    String subjectName,
    @Nullable PurchaseRequestDraft purchaseRequest,
    @Nullable MaintenanceTaskDraft maintenanceTask,
    UUID proposedBy,
    @Nullable String proposedByName,
    @Nullable UUID proposedViaToken,
    @Nullable String proposedViaTokenName,
    Instant proposedAt,
    @Nullable UUID decidedBy,
    @Nullable String decidedByName,
    @Nullable Instant decidedAt,
    @Nullable String decisionNote,
    @Nullable UUID resultId,
    long version) {

  /** What an assistant can propose. */
  public enum Kind {
    PURCHASE_REQUEST,
    MAINTENANCE_TASK
  }

  /** Waiting for staff, or decided. */
  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }
}
