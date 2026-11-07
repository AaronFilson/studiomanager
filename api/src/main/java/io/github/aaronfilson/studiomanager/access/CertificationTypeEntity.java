package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A kind of sign-off the bound studio gives, such as "Kiln loading". Named for its table; {@link
 * AccessService.CertificationType} is the view of it.
 */
@Entity
@Table(name = "certification_type")
class CertificationTypeEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String name;

  /** Months it lasts after granting; null if it never expires. */
  private @Nullable Integer validMonths;

  @SuppressWarnings("NullAway.Init")
  protected CertificationTypeEntity() {}

  CertificationTypeEntity(String name, @Nullable Integer validMonths) {
    this.id = Uuids.v7();
    this.name = name;
    this.validMonths = validMonths;
  }

  AccessService.CertificationType toView() {
    return new AccessService.CertificationType(id, name, validMonths);
  }
}
