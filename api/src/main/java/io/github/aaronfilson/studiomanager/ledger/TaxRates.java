package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.TaxRate;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sales tax rates by category and date, and the tax on an amount. */
@Service
class TaxRates {

  private final TaxRateRepository rates;
  private final LedgerQueries queries;

  TaxRates(TaxRateRepository rates, LedgerQueries queries) {
    this.rates = rates;
    this.queries = queries;
  }

  @Transactional(readOnly = true)
  public List<TaxRate> taxRates() {
    return rates.findInOrder().stream().map(TaxRateEntity::toView).toList();
  }

  /**
   * Sets the rate for a tax category from a date onwards. Rules change by date: Washington began
   * taxing group pottery classes on 2025-10-01, then exempted nonprofits' from 2026-07-01.
   */
  @Transactional
  public TaxRate setTaxRate(String taxCategory, BigDecimal rate, LocalDate effectiveFrom) {
    TenantContext caller = LedgerService.requireStaff();
    String category = taxCategory == null ? "" : taxCategory.strip();
    if (category.isEmpty() || category.length() > 40) {
      throw new InvalidInputException("Name the tax category in 1 to 40 characters.");
    }
    if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
      throw new InvalidInputException("A rate is a fraction from 0 up to 1 (0.0825 is 8.25%).");
    }
    queries.setTaxRate(caller.requireStudioId(), category, rate, effectiveFrom);
    return taxRates().stream()
        .filter(t -> t.taxCategory().equals(category) && t.effectiveFrom().equals(effectiveFrom))
        .findFirst()
        .orElseThrow();
  }

  /** The tax on {@code amount} at the category's rate on {@code day}, in the currency's units. */
  BigDecimal taxOn(BigDecimal amount, String taxCategory, LocalDate day, Currency currency) {
    BigDecimal rate =
        rates.findRatesInForce(taxCategory, day, Limit.of(1)).stream()
            .findFirst()
            .orElse(BigDecimal.ZERO);
    return amount
        .multiply(rate)
        .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
  }
}
