package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueKind;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Prepaid money held for someone: a gift card, a firing card, a class pack, a deposit or credit.
 * What's left on it is a sum of journal lines, so it is read in SQL ({@code LedgerQueries}); the
 * entity is for selling one and for locking it while it's spent.
 */
@Entity
@Table(name = "stored_value")
@EntityListeners(AuditingEntityListener.class)
class StoredValue {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  @Enumerated(EnumType.STRING)
  private StoredValueKind kind;

  /** The member it belongs to; a gift card can be held by anyone with the code. */
  private @Nullable UUID holder;

  private @Nullable String code;

  private @Nullable String label;

  private @Nullable LocalDate expiresOn;

  @CreatedDate private @Nullable Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected StoredValue() {}

  StoredValue(
      StoredValueKind kind,
      @Nullable UUID holder,
      @Nullable String code,
      @Nullable String label,
      @Nullable LocalDate expiresOn) {
    this.id = Uuids.v7();
    this.kind = kind;
    this.holder = holder;
    this.code = code;
    this.label = label;
    this.expiresOn = expiresOn;
  }

  UUID id() {
    return id;
  }
}
