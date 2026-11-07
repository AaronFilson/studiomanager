package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's tax rates; Hibernate adds the studio to every query ({@code @TenantId}). */
interface TaxRateRepository extends JpaRepository<TaxRateEntity, UUID> {

  @Query("select t from TaxRateEntity t order by t.taxCategory, t.effectiveFrom")
  List<TaxRateEntity> findInOrder();

  /** The rate in force on {@code day}, newest first: ask for one. */
  @Query(
      """
      select t.rate from TaxRateEntity t
      where t.taxCategory = :taxCategory and t.effectiveFrom <= :day
      order by t.effectiveFrom desc
      """)
  List<BigDecimal> findRatesInForce(String taxCategory, LocalDate day, Limit limit);
}
