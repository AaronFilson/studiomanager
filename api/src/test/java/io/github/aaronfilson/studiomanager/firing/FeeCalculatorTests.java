package io.github.aaronfilson.studiomanager.firing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Random;

import io.github.aaronfilson.studiomanager.firing.FeeRule.DimensionRounding;
import io.github.aaronfilson.studiomanager.firing.FeeRule.VolumeUnit;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.Weight;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;
import org.junit.jupiter.api.Test;

class FeeCalculatorTests {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Test
    void roundsEachSideUpToTheNextWholeInchBeforePricing() {
        // A common US policy: "$0.05 per cubic inch, each dimension rounded up to the next inch"
        FeeSchedule schedule = schedule(new FeeRule.PerVolume(new BigDecimal("0.05"), VolumeUnit.CUBIC_INCH,
                DimensionRounding.WHOLE_UNIT), "0");

        FeeQuote quote = FeeCalculator.quote(schedule, box("11.2", "9.5", "7.9", LengthUnit.INCH), USD);

        assertThat(quote.amount().amount()).isEqualByComparingTo("48.00");
        assertThat(quote.breakdown()).isEqualTo("12 × 10 × 8 in → 960 in³ × 0.05 = 48.00");
        assertThat(quote.scheduleVersion()).isEqualTo(3);
        assertThat(quote.taxCategory()).isEqualTo("FIRING_SERVICE");
    }

    @Test
    void roundsInTheRulesUnitEvenWhenThePieceWasMeasuredInAnother() {
        FeeSchedule schedule = schedule(new FeeRule.PerVolume(new BigDecimal("0.05"), VolumeUnit.CUBIC_INCH,
                DimensionRounding.WHOLE_UNIT), "0");

        // 25.4 cm is exactly 10 in, so no side rounds up; 25.5 cm is a hair over and does
        assertThat(FeeCalculator.quote(schedule, box("25.4", "25.4", "25.4", LengthUnit.CENTIMETRE), USD)
                .amount().amount()).isEqualByComparingTo("50.00");
        assertThat(FeeCalculator.quote(schedule, box("25.5", "25.4", "25.4", LengthUnit.CENTIMETRE), USD)
                .amount().amount()).isEqualByComparingTo("55.00");
    }

    @Test
    void pricesPerLitreFromCentimetres() {
        // UK studios often charge by the litre: 20 × 15 × 10 cm = 3 L at £1.20
        FeeSchedule schedule = schedule(new FeeRule.PerVolume(new BigDecimal("1.20"), VolumeUnit.LITRE,
                DimensionRounding.WHOLE_UNIT), "0");

        FeeQuote quote = FeeCalculator.quote(schedule, box("19.1", "14.2", "9.6", LengthUnit.CENTIMETRE), GBP);

        assertThat(quote.amount().amount()).isEqualByComparingTo("3.60");
        assertThat(quote.breakdown()).isEqualTo("20 × 15 × 10 cm → 3 L × 1.2 = 3.60");
    }

    @Test
    void pricesByWeightRoundedUpToTheHalfUnit() {
        FeeSchedule schedule = schedule(new FeeRule.PerWeight(new BigDecimal("4"), WeightUnit.KILOGRAM,
                DimensionRounding.HALF_UNIT), "0");

        FeeQuote quote = FeeCalculator.quote(schedule,
                new PieceSize(null, null, null, Weight.of("1100", WeightUnit.GRAM)), GBP);

        assertThat(quote.amount().amount()).isEqualByComparingTo("6.00");
        assertThat(quote.breakdown()).isEqualTo("1.5 kg × 4 = 6.00");
    }

    @Test
    void neverChargesLessThanTheMinimum() {
        FeeSchedule schedule = schedule(new FeeRule.PerVolume(new BigDecimal("0.05"), VolumeUnit.CUBIC_INCH,
                DimensionRounding.WHOLE_UNIT), "3.00");

        FeeQuote quote = FeeCalculator.quote(schedule, box("2", "2", "2", LengthUnit.INCH), USD);

        assertThat(quote.amount().amount()).isEqualByComparingTo("3.00");
        assertThat(quote.breakdown()).endsWith("= 0.40, raised to the minimum 3.00");
    }

    @Test
    void chargesAFlatFeeWithoutMeasurements() {
        FeeQuote quote = FeeCalculator.quote(schedule(new FeeRule.Flat(new BigDecimal("6.5")), "0"),
                PieceSize.unmeasured(), USD);

        assertThat(quote.amount().amount()).isEqualByComparingTo("6.50");
    }

    @Test
    void usesTheCurrencysOwnMinorUnit() {
        FeeQuote quote = FeeCalculator.quote(schedule(new FeeRule.PerVolume(new BigDecimal("0.85"),
                VolumeUnit.CUBIC_CENTIMETRE, DimensionRounding.NONE), "0"), box("10", "10", "1", LengthUnit.CENTIMETRE), JPY);

        assertThat(quote.amount().amount()).isEqualByComparingTo("85");
        assertThat(quote.amount().amount().scale()).isZero();
    }

    @Test
    void explainsWhatIsMissing() {
        FeeSchedule byVolume = schedule(new FeeRule.PerVolume(BigDecimal.ONE, VolumeUnit.CUBIC_INCH,
                DimensionRounding.NONE), "0");
        FeeSchedule byWeight = schedule(new FeeRule.PerWeight(BigDecimal.ONE, WeightUnit.POUND,
                DimensionRounding.NONE), "0");

        assertThatThrownBy(() -> FeeCalculator.quote(byVolume, PieceSize.unmeasured(), USD))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("three dimensions");
        assertThatThrownBy(() -> FeeCalculator.quote(byWeight, PieceSize.unmeasured(), USD))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("weight");
    }

    @Test
    void roundingUpNeverLowersThePriceAndABiggerPieceNeverCostsLess() {
        // A small property check over random pieces: rounding only adds, and price is monotonic in size
        FeeSchedule rounded = schedule(new FeeRule.PerVolume(new BigDecimal("0.04"), VolumeUnit.CUBIC_INCH,
                DimensionRounding.WHOLE_UNIT), "0");
        FeeSchedule exact = schedule(new FeeRule.PerVolume(new BigDecimal("0.04"), VolumeUnit.CUBIC_INCH,
                DimensionRounding.NONE), "0");
        Random random = new Random(42);
        for (int i = 0; i < 1_000; i++) {
            String l = dimension(random);
            String w = dimension(random);
            String h = dimension(random);
            PieceSize piece = box(l, w, h, LengthUnit.CENTIMETRE);
            PieceSize bigger = box(new BigDecimal(l).add(new BigDecimal("0.5")).toPlainString(), w, h, LengthUnit.CENTIMETRE);

            assertThat(FeeCalculator.quote(rounded, piece, USD).amount())
                    .isGreaterThanOrEqualTo(FeeCalculator.quote(exact, piece, USD).amount());
            assertThat(FeeCalculator.quote(rounded, bigger, USD).amount())
                    .isGreaterThanOrEqualTo(FeeCalculator.quote(rounded, piece, USD).amount());
        }
    }

    private static String dimension(Random random) {
        return BigDecimal.valueOf(1 + random.nextInt(600), 1).toPlainString();
    }

    private static FeeSchedule schedule(FeeRule rule, String minimum) {
        return new FeeSchedule(3, rule, new BigDecimal(minimum), FeeSchedule.DEFAULT_TAX_CATEGORY);
    }

    private static PieceSize box(String length, String width, String height, LengthUnit unit) {
        return new PieceSize(Length.of(length, unit), Length.of(width, unit), Length.of(height, unit), null);
    }

}
