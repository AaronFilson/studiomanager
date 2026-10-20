package io.github.aaronfilson.studiomanager.platform.measure;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** A weight as it was measured: the value and the unit it was entered in. See {@link Length}. */
public record Weight(BigDecimal value, WeightUnit unit) {

    public Weight {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("A weight must be positive");
        }
    }

    public static Weight of(String value, WeightUnit unit) {
        return new Weight(new BigDecimal(value), unit);
    }

    /** This weight in {@code target} units, unrounded (to 34 significant digits). */
    public BigDecimal in(WeightUnit target) {
        if (target == unit) {
            return value;
        }
        return value.multiply(unit.grams()).divide(target.grams(), MathContext.DECIMAL128);
    }

    @Override
    public String toString() {
        return value.stripTrailingZeros().toPlainString() + " " + unit.symbol();
    }

}
