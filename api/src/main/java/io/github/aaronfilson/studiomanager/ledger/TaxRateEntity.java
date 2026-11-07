package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.TaxRate;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

/**
 * A tax category's rate from a date onwards. Read here; setting one is an upsert in {@link
 * LedgerQueries}. Named for its table; {@link TaxRate} is the view of it.
 */
// Hibernate reads and writes the mapped fields by reflection; some have no Java reader
@SuppressWarnings("UnusedVariable")
@Entity
@Table(name = "tax_rate")
class TaxRateEntity {

  @Id private UUID id;

  @TenantId private @Nullable UUID studioId;

  private String taxCategory;

  /** A fraction: 0.0825 is 8.25%. */
  private BigDecimal rate;

  private LocalDate effectiveFrom;

  @SuppressWarnings("NullAway.Init")
  protected TaxRateEntity() {}

  TaxRate toView() {
    return new TaxRate(id, taxCategory, rate, effectiveFrom);
  }
}
