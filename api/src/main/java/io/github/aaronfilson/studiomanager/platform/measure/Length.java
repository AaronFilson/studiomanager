package io.github.aaronfilson.studiomanager.platform.measure;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * A length as it was measured: the value and the unit it was entered in. Conversions are
 * computed from that, never stored, so going back and forth between units cannot drift.
 */
public record Length(BigDecimal value, LengthUnit unit) {

    public Length {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("A length must be positive");
        }
    }

    public static Length of(String value, LengthUnit unit) {
        return new Length(new BigDecimal(value), unit);
    }

    /** This length in {@code target} units, unrounded (to 34 significant digits). */
    public BigDecimal in(LengthUnit target) {
        if (target == unit) {
            return value;
        }
        return value.multiply(unit.millimetres()).divide(target.millimetres(), MathContext.DECIMAL128);
    }

    @Override
    public String toString() {
        return value.stripTrailingZeros().toPlainString() + " " + unit.symbol();
    }

}
