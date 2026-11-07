package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * An opening balance held for someone invited by email, posted once they join. The invite is the
 * studio module's, so it is held by id (ADR 21).
 */
@Entity
@Table(name = "pending_opening_balance")
@EntityListeners(AuditingEntityListener.class)
class PendingOpeningBalance {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private UUID inviteId;

  /** Positive: they owe the studio. Negative: they're in credit. */
  private BigDecimal amount;

  private LocalDate asOf;

  /** The entry that posted it, once they've joined. */
  private @Nullable UUID entryId;

  @CreatedBy private @Nullable UUID createdBy;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected PendingOpeningBalance() {}

  PendingOpeningBalance(UUID inviteId, BigDecimal amount, LocalDate asOf) {
    this.id = Uuids.v7();
    this.inviteId = inviteId;
    this.amount = amount;
    this.asOf = asOf;
  }

  BigDecimal amount() {
    return amount;
  }

  LocalDate asOf() {
    return asOf;
  }

  void posted(UUID entry) {
    this.entryId = entry;
  }
}
