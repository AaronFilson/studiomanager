package io.github.aaronfilson.studiomanager.proposal;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.proposal.PendingActionView.Kind;
import io.github.aaronfilson.studiomanager.proposal.PendingActionView.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A change an assistant proposed, waiting for staff or decided. Hibernate fills {@code studio_id}
 * ({@code TenantId}), and auditing fills who proposed it and when (ADR 21).
 *
 * <p>The person who proposed it is the token's owner; the token itself is noted alongside, so the
 * queue can say "Olive, through 'Claude desktop'". Whoever approves is the actor of the change it
 * makes.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "pending_action")
@EntityListeners(AuditingEntityListener.class)
class PendingAction {

  static final int MAX_TEXT = 500;

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private Kind kind;

  @Enumerated(EnumType.STRING)
  private Status status;

  private @Nullable String reason;

  private String subjectName;

  @Embedded private @Nullable PurchaseRequestDraft purchaseRequest;

  @Embedded private @Nullable MaintenanceTaskDraft maintenanceTask;

  @CreatedBy
  @Column(name = "proposed_by")
  private @Nullable UUID proposedBy;

  @Column(name = "proposed_via_token")
  private @Nullable UUID proposedViaToken;

  @CreatedDate private @Nullable Instant createdAt;

  private @Nullable UUID decidedBy;

  private @Nullable Instant decidedAt;

  private @Nullable String decisionNote;

  private @Nullable UUID resultId;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected PendingAction() {}

  private PendingAction(
      Kind kind,
      @Nullable String reason,
      String subjectName,
      @Nullable PurchaseRequestDraft purchaseRequest,
      @Nullable MaintenanceTaskDraft maintenanceTask,
      @Nullable UUID proposedViaToken) {
    this.id = Uuids.v7();
    this.kind = kind;
    this.status = Status.PENDING;
    this.reason = cleanText(reason, "reason");
    this.subjectName = subjectName;
    this.purchaseRequest = purchaseRequest;
    this.maintenanceTask = maintenanceTask;
    this.proposedViaToken = proposedViaToken;
  }

  static PendingAction purchaseRequest(
      PurchaseRequestDraft draft,
      String itemName,
      @Nullable String reason,
      @Nullable UUID proposedViaToken) {
    return new PendingAction(
        Kind.PURCHASE_REQUEST, reason, itemName, draft, null, proposedViaToken);
  }

  static PendingAction maintenanceTask(
      MaintenanceTaskDraft draft,
      String assetName,
      @Nullable String reason,
      @Nullable UUID proposedViaToken) {
    return new PendingAction(
        Kind.MAINTENANCE_TASK, reason, assetName, null, draft, proposedViaToken);
  }

  UUID id() {
    return id;
  }

  Kind kind() {
    return kind;
  }

  long version() {
    return Objects.requireNonNull(version, "A pending action has a version once it's saved");
  }

  UUID proposedBy() {
    return Objects.requireNonNull(proposedBy, "A pending action has a proposer once it's saved");
  }

  @Nullable UUID proposedViaToken() {
    return proposedViaToken;
  }

  @Nullable UUID decidedBy() {
    return decidedBy;
  }

  /** The draft of a purchase-request proposal. */
  PurchaseRequestDraft requirePurchaseRequest() {
    return Objects.requireNonNull(purchaseRequest, "A purchase-request proposal has its draft");
  }

  /** The draft of a maintenance-task proposal. */
  MaintenanceTaskDraft requireMaintenanceTask() {
    return Objects.requireNonNull(maintenanceTask, "A maintenance-task proposal has its draft");
  }

  /** Records that {@code approver} approved it, and what that made. */
  void approve(UUID approver, Instant at, UUID madeId) {
    requirePending();
    this.status = Status.APPROVED;
    this.decidedBy = approver;
    this.decidedAt = at;
    this.resultId = madeId;
  }

  /** Records that {@code rejecter} turned it down, with an optional note why. */
  void reject(UUID rejecter, Instant at, @Nullable String note) {
    requirePending();
    this.status = Status.REJECTED;
    this.decidedBy = rejecter;
    this.decidedAt = at;
    this.decisionNote = cleanText(note, "note");
  }

  PendingActionView toView(Map<UUID, String> accountNames, Map<UUID, String> tokenNames) {
    UUID proposer = proposedBy();
    UUID token = proposedViaToken;
    UUID decider = decidedBy;
    return new PendingActionView(
        id,
        kind,
        status,
        reason,
        subjectName,
        purchaseRequest,
        maintenanceTask,
        proposer,
        accountNames.get(proposer),
        token,
        token == null ? null : tokenNames.get(token),
        Objects.requireNonNull(createdAt, "A pending action has a time once it's saved"),
        decider,
        decider == null ? null : accountNames.get(decider),
        decidedAt,
        decisionNote,
        resultId,
        version());
  }

  /** Throws unless it's still waiting for a decision. */
  void requirePending() {
    if (status != Status.PENDING) {
      throw new InvalidInputException(
          "This suggestion was already %s."
              .formatted(status.name().toLowerCase(java.util.Locale.ROOT)));
    }
  }

  private static @Nullable String cleanText(@Nullable String text, String field) {
    return cleanText(text, field, MAX_TEXT);
  }

  /** Blank becomes null; anything longer than {@code max} characters is refused. */
  static @Nullable String cleanText(@Nullable String text, String field, int max) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String cleaned = text.strip();
    if (cleaned.length() > max) {
      throw new InvalidInputException("Keep the %s to %d characters.".formatted(field, max));
    }
    return cleaned;
  }
}
