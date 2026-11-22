package io.github.aaronfilson.studiomanager.firing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.firing.FeeRule.DimensionRounding;
import io.github.aaronfilson.studiomanager.firing.FeeRule.VolumeUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import java.math.BigDecimal;
import java.util.Currency;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.BigRange;

/**
 * Property-based tests with automatic shrinking (Plan 15 &amp; Plan 19) for the firing-fee engine:
 * rounding up never lowers a fee, bigger or heavier pieces never cost less, and the minimum floor
 * and currency minor-unit scale always hold.
 */
class FeeCalculatorPropertyTests {

  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency JPY = Currency.getInstance("JPY");

  @Property
  void volumeRoundingUpNeverLowersPriceAndIsMonotonic(
      @ForAll @BigRange(min = "0.5", max = "60.0") BigDecimal length,
      @ForAll @BigRange(min = "0.5", max = "60.0") BigDecimal width,
      @ForAll @BigRange(min = "0.5", max = "60.0") BigDecimal height,
      @ForAll @BigRange(min = "0.01", max = "5.00") BigDecimal delta,
      @ForAll @BigRange(min = "0.01", max = "2.50") BigDecimal rate,
      @ForAll @BigRange(min = "0.00", max = "25.00") BigDecimal minimum,
      @ForAll LengthUnit measuredUnit,
      @ForAll VolumeUnit ruleUnit) {
    FeeSchedule exact =
        schedule(new FeeRule.PerVolume(rate, ruleUnit, DimensionRounding.NONE), minimum);
    FeeSchedule half =
        schedule(new FeeRule.PerVolume(rate, ruleUnit, DimensionRounding.HALF_UNIT), minimum);
    FeeSchedule whole =
        schedule(new FeeRule.PerVolume(rate, ruleUnit, DimensionRounding.WHOLE_UNIT), minimum);

    PieceSize piece = box(length, width, height, measuredUnit);
    PieceSize bigger = box(length.add(delta), width, height, measuredUnit);

    FeeQuote qExact = FeeCalculator.quote(exact, piece, USD);
    FeeQuote qHalf = FeeCalculator.quote(half, piece, USD);
    FeeQuote qWhole = FeeCalculator.quote(whole, piece, USD);
    FeeQuote qBigger = FeeCalculator.quote(whole, bigger, USD);

    assertThat(qExact.amount().amount()).isGreaterThanOrEqualTo(minimum);
    assertThat(qHalf.amount()).isGreaterThanOrEqualTo(qExact.amount());
    assertThat(qWhole.amount()).isGreaterThanOrEqualTo(qHalf.amount());
    assertThat(qBigger.amount()).isGreaterThanOrEqualTo(qWhole.amount());
    assertThat(qWhole.amount().amount().scale()).isEqualTo(2);
  }

  @Property
  void weightPricingIsMonotonicAndRespectsCurrencyScale(
      @ForAll @BigRange(min = "0.1", max = "50.0") BigDecimal weight,
      @ForAll @BigRange(min = "0.1", max = "10.0") BigDecimal extra,
      @ForAll @BigRange(min = "0.50", max = "20.00") BigDecimal rate,
      @ForAll WeightUnit measuredUnit,
      @ForAll("weightRuleUnits") WeightUnit ruleUnit) {
    FeeSchedule schedule =
        schedule(
            new FeeRule.PerWeight(rate, ruleUnit, DimensionRounding.HALF_UNIT), BigDecimal.ZERO);

    PieceSize lighter =
        new PieceSize(null, null, null, Weight.of(weight.toPlainString(), measuredUnit));
    PieceSize heavier =
        new PieceSize(null, null, null, Weight.of(weight.add(extra).toPlainString(), measuredUnit));

    FeeQuote qLighterUsd = FeeCalculator.quote(schedule, lighter, USD);
    FeeQuote qHeavierUsd = FeeCalculator.quote(schedule, heavier, USD);
    FeeQuote qLighterJpy = FeeCalculator.quote(schedule, lighter, JPY);

    assertThat(qHeavierUsd.amount()).isGreaterThanOrEqualTo(qLighterUsd.amount());
    assertThat(qLighterUsd.amount().amount().scale()).isEqualTo(2);
    assertThat(qLighterJpy.amount().amount().scale()).isZero();
  }

  @Provide
  Arbitrary<WeightUnit> weightRuleUnits() {
    return Arbitraries.of(WeightUnit.POUND, WeightUnit.KILOGRAM);
  }

  private static FeeSchedule schedule(FeeRule rule, BigDecimal minimum) {
    return new FeeSchedule(1, rule, minimum, FeeSchedule.DEFAULT_TAX_CATEGORY);
  }

  private static PieceSize box(
      BigDecimal length, BigDecimal width, BigDecimal height, LengthUnit unit) {
    return new PieceSize(
        Length.of(length.toPlainString(), unit),
        Length.of(width.toPlainString(), unit),
        Length.of(height.toPlainString(), unit),
        null);
  }
}
