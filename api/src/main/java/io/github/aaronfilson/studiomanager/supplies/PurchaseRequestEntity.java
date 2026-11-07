package io.github.aaronfilson.studiomanager.supplies;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.RequestStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.access.AccessDeniedException;

/**
 * A request to buy more of an item, from requested through the owner's decision to ordered and
 * received. Named for its table; {@link SupplyModel.PurchaseRequest} is the view the API returns.
 *
 * <p>Who asked and when are filled by auditing ({@code @CreatedBy}, {@code @CreatedDate}).
 */
@Entity
@Table(name = "purchase_request")
@EntityListeners(AuditingEntityListener.class)
class PurchaseRequestEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id")
  private SupplyItem item;

  private BigDecimal quantity;

  private @Nullable String note;

  @Enumerated(EnumType.STRING)
  private RequestStatus status;

  @CreatedBy private @Nullable UUID requestedBy;

  /** The personal API token the request was made through; null when it was made in the app. */
  private @Nullable UUID requestedViaTokenId;

  @CreatedDate private @Nullable Instant requestedAt;

  private @Nullable UUID decidedBy;

  /** The personal API token the decision was made through; null when it was made in the app. */
  private @Nullable UUID decidedViaTokenId;

  private @Nullable Instant decidedAt;

  @Version private @Nullable Long version;

  @SuppressWarnings("NullAway.Init")
  protected PurchaseRequestEntity() {}

  PurchaseRequestEntity(
      SupplyItem item,
      BigDecimal quantity,
      @Nullable String note,
      @Nullable UUID requestedViaTokenId) {
    this.id = Uuids.v7();
    this.item = item;
    this.quantity = quantity;
    this.note = note;
    this.requestedViaTokenId = requestedViaTokenId;
    this.status = RequestStatus.REQUESTED;
  }

  UUID id() {
    return id;
  }

  SupplyItem item() {
    return item;
  }

  BigDecimal quantity() {
    return quantity;
  }

  long version() {
    return Objects.requireNonNull(version, "A purchase request has a version once it's saved");
  }

  UUID requestedBy() {
    return Objects.requireNonNull(requestedBy, "A purchase request has a requester once saved");
  }

  @Nullable UUID decidedBy() {
    return decidedBy;
  }

  @Nullable UUID requestedViaTokenId() {
    return requestedViaTokenId;
  }

  @Nullable UUID decidedViaTokenId() {
    return decidedViaTokenId;
  }

  /**
   * Moves the request on. The owner approves or rejects a new request; staff mark an approved one
   * ordered, and an approved or ordered one received.
   *
   * @param byOwner whether the person moving it is the studio's owner
   * @param viaTokenId the personal API token the move comes through, if any
   */
  void moveTo(
      RequestStatus to, boolean byOwner, UUID byAccountId, @Nullable UUID viaTokenId, Instant now) {
    boolean allowed =
        switch (to) {
          case APPROVED -> ownerDecides(byOwner);
          case REJECTED -> ownerDecides(byOwner);
          case ORDERED -> status == RequestStatus.APPROVED;
          case RECEIVED -> status == RequestStatus.APPROVED || status == RequestStatus.ORDERED;
          case REQUESTED -> false;
        };
    if (!allowed) {
      throw new InvalidInputException(
          "A %s request can't become %s.".formatted(lowerCase(status), lowerCase(to)));
    }
    boolean isDecision =
        switch (to) {
          case APPROVED -> true;
          case REJECTED -> true;
          case REQUESTED -> false;
          case ORDERED -> false;
          case RECEIVED -> false;
        };
    if (isDecision) {
      this.decidedBy = byAccountId;
      this.decidedViaTokenId = viaTokenId;
      this.decidedAt = now;
    }
    this.status = to;
  }

  /** Only the owner approves or rejects, and only a request still waiting for a decision. */
  private boolean ownerDecides(boolean byOwner) {
    if (!byOwner) {
      throw new AccessDeniedException("Only the owner approves purchases.");
    }
    return status == RequestStatus.REQUESTED;
  }

  /** As the API shows it, with the names looked up by the caller. */
  SupplyModel.PurchaseRequest toView(
      @Nullable String requestedByName,
      @Nullable String requestedViaToken,
      @Nullable String decidedByName,
      @Nullable String decidedViaToken) {
    return new SupplyModel.PurchaseRequest(
        id,
        item.id(),
        item.name(),
        quantity,
        note,
        status,
        requestedBy(),
        requestedByName,
        requestedViaToken,
        Objects.requireNonNull(requestedAt, "A purchase request has a time once it's saved"),
        decidedByName,
        decidedViaToken,
        version());
  }

  private static String lowerCase(RequestStatus status) {
    return status.name().toLowerCase(Locale.ROOT);
  }
}
