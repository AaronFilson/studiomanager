package io.github.aaronfilson.studiomanager.access;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * The latest grant of one certification to one member. Granting again renews it, which is an upsert
 * in {@link AccessQueries}; this entity reads and removes grants. Named for its table; {@link
 * AccessService.Certification} is the view of it.
 */
@Entity
@Table(name = "certification")
class CertificationEntity {

  @EmbeddedId private Key key;

  @TenantId private @Nullable UUID studioId;

  private UUID grantedBy;

  private Instant grantedAt;

  private @Nullable Instant expiresAt;

  @SuppressWarnings("NullAway.Init")
  protected CertificationEntity() {}

  @Embeddable
  record Key(@Column(name = "type_id") UUID typeId, @Column(name = "account_id") UUID accountId)
      implements Serializable {}
}
