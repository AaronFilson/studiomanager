package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.Money;
import io.github.aaronfilson.studiomanager.platform.measure.Length;
import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;

/**
 * Prices a piece. Pure: the same schedule and measurements always give the same quote.
 *
 * <p>The order matters and is what studios publish. Each dimension is converted to the rule's
 * unit and rounded up there ("round each side up to the next whole inch"), then multiplied by
 * the rate, then raised to the minimum, and only then rounded to the currency's minor unit.
 * Rounding in millimetres instead would change real prices.
 */
public final class FeeCalculator {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private FeeCalculator() {
    }

    public static FeeQuote quote(FeeSchedule schedule, PieceSize size, Currency currency) {
        Priced priced = switch (schedule.rule()) {
            case FeeRule.PerVolume rule -> perVolume(rule, size);
            case FeeRule.PerWeight rule -> perWeight(rule, size);
            case FeeRule.Flat rule -> new Priced(rule.rate(), "flat " + plain(rule.rate()));
        };
        Money computed = Money.rounded(priced.amount(), currency);
        Money minimum = Money.rounded(schedule.minimum(), currency);
        String breakdown = priced.working() + " = " + computed.amount().toPlainString();
        Money amount = computed;
        if (computed.compareTo(minimum) < 0) {
            amount = minimum;
            breakdown += ", raised to the minimum " + minimum.amount().toPlainString();
        }
        return new FeeQuote(amount, schedule.version(), schedule.taxCategory(), breakdown);
    }

    private static Priced perVolume(FeeRule.PerVolume rule, PieceSize size) {
        if (!size.hasDimensions()) {
            throw new InvalidInputException("This studio prices by volume, so the piece needs its three dimensions.");
        }
        LengthUnit unit = rule.unit().lengthUnit;
        BigDecimal length = roundUp(dimension(size.length(), unit), rule.rounding());
        BigDecimal width = roundUp(dimension(size.width(), unit), rule.rounding());
        BigDecimal height = roundUp(dimension(size.height(), unit), rule.rounding());
        BigDecimal volume = length.multiply(width).multiply(height)
                .divide(rule.unit().cubicLengthUnitsPerUnit);
        String working = "%s × %s × %s %s → %s %s × %s".formatted(plain(length), plain(width), plain(height),
                unit.symbol(), plain(volume), rule.unit().symbol, plain(rule.rate()));
        return new Priced(volume.multiply(rule.rate()), working);
    }

    private static Priced perWeight(FeeRule.PerWeight rule, PieceSize size) {
        if (size.weight() == null) {
            throw new InvalidInputException("This studio prices by weight, so the piece needs its weight.");
        }
        BigDecimal weight = roundUp(size.weight().in(rule.unit()), rule.rounding());
        String working = "%s %s × %s".formatted(plain(weight), rule.unit().symbol(), plain(rule.rate()));
        return new Priced(weight.multiply(rule.rate()), working);
    }

    private static BigDecimal dimension(Length length, LengthUnit unit) {
        return length.in(unit);
    }

    static BigDecimal roundUp(BigDecimal value, FeeRule.DimensionRounding rounding) {
        return switch (rounding) {
            // Still bounded, so a conversion like 25.4 mm → 1.0000…01 in can't leak into prices
            case NONE -> value.setScale(4, RoundingMode.HALF_UP);
            case WHOLE_UNIT -> value.setScale(0, RoundingMode.CEILING);
            case HALF_UNIT -> value.multiply(TWO).setScale(0, RoundingMode.CEILING).divide(TWO);
        };
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record Priced(BigDecimal amount, String working) {
    }

}
