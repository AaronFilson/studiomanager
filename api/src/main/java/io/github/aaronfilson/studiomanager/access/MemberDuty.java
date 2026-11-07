package io.github.aaronfilson.studiomanager.access;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * One duty held by one member of the bound studio. The table's key also holds {@code studio_id};
 * Hibernate's key leaves it out, since {@code @TenantId} already limits every query to one studio.
 * Never updated, only granted and taken away (the table allows no update).
 */
@Entity
@Table(name = "member_duty")
class MemberDuty {

  @EmbeddedId private Key key;

  @TenantId private @Nullable UUID studioId;

  private Instant grantedAt;

  @SuppressWarnings("NullAway.Init")
  protected MemberDuty() {}

  MemberDuty(UUID accountId, Duty duty, Instant grantedAt) {
    this.key = new Key(accountId, duty);
    this.grantedAt = grantedAt;
  }

  UUID accountId() {
    return key.accountId();
  }

  Duty duty() {
    return key.duty();
  }

  @Embeddable
  record Key(
      @Column(name = "account_id") UUID accountId,
      @Column(name = "duty") @Enumerated(EnumType.STRING) Duty duty)
      implements Serializable {}
}
