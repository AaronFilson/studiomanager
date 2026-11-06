package io.github.aaronfilson.studiomanager.glaze;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;

/**
 * Scales a recipe to a dry batch. Ported from glaze-vault's {@code scaleBatch}, with units added.
 * <ul>
 * <li>The base is the sum of the base lines (of every line, if all are additions).</li>
 * <li>A base line, or an addition in parts, weighs {@code amount / base × size}.</li>
 * <li>A percent addition weighs {@code amount / 100 × size}, on top of the batch.</li>
 * <li>A grams addition is that many grams, whatever the size.</li>
 * </ul>
 * The math runs in grams at full precision; only the result is rounded, per unit.
 */
public final class BatchCalculator {

    private static final MathContext MATH = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final BigDecimal MAX_GRAMS = new BigDecimal("1000000");

    public enum WeightUnit {
        GRAM("1", 1), KILOGRAM("1000", 3), POUND("453.59237", 3), OUNCE("28.349523125", 2);

        final BigDecimal grams;
        final int decimals;

        WeightUnit(String grams, int decimals) {
            this.grams = new BigDecimal(grams);
            this.decimals = decimals;
        }

        BigDecimal toGrams(BigDecimal amount) {
            return amount.multiply(grams, MATH);
        }

        BigDecimal fromGrams(BigDecimal amount) {
            return amount.divide(grams, MATH);
        }
    }

    public record Weighed(GlazeBook.Line line, BigDecimal weight) {
    }

    /** Each line's weight and the total, in the batch's unit, rounded for the scale. */
    public record Batch(BigDecimal size, WeightUnit unit, List<Weighed> lines, BigDecimal total) {
    }

    private BatchCalculator() {
    }

    public static Batch scale(List<GlazeBook.Line> lines, BigDecimal size, WeightUnit unit) {
        if (size.signum() <= 0) {
            throw new InvalidInputException("The batch size must be more than 0.");
        }
        BigDecimal sizeGrams = unit.toGrams(size);
        if (sizeGrams.compareTo(MAX_GRAMS) > 0) {
            throw new InvalidInputException("That's more than a tonne. Mix it in smaller batches.");
        }
        List<BigDecimal> grams = exact(lines, sizeGrams);
        BigDecimal total = grams.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Weighed> weighed = new java.util.ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            weighed.add(new Weighed(lines.get(i), round(unit.fromGrams(grams.get(i)), unit)));
        }
        return new Batch(size, unit, List.copyOf(weighed), round(unit.fromGrams(total), unit));
    }

    /** Each line's weight in grams, unrounded. Visible for the tests. */
    static List<BigDecimal> exact(List<GlazeBook.Line> lines, BigDecimal sizeGrams) {
        BigDecimal base = lines.stream().filter(l -> l.kind() == GlazeBook.LineKind.BASE)
                .map(GlazeBook.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (base.signum() == 0) {
            base = lines.stream().map(GlazeBook.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal perPart = sizeGrams.divide(base, MATH);
        return lines.stream().map(line -> switch (line.unit()) {
            case PARTS -> line.amount().multiply(perPart, MATH);
            case PERCENT -> line.amount().multiply(sizeGrams, MATH).divide(HUNDRED, MATH);
            case GRAMS -> line.amount();
        }).toList();
    }

    private static BigDecimal round(BigDecimal value, WeightUnit unit) {
        return value.setScale(unit.decimals, RoundingMode.HALF_UP);
    }

}
