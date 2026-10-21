package io.github.aaronfilson.studiomanager.firing;

import java.math.BigDecimal;
import java.util.Objects;

import io.github.aaronfilson.studiomanager.platform.measure.LengthUnit;
import io.github.aaronfilson.studiomanager.platform.measure.WeightUnit;

/**
 * How a studio prices one piece. Rates are in the studio's currency, per one unit of the rule.
 * The three kinds cover what the 84-studio census found: per cubic inch in the US, per litre or
 * kilogram in the UK and Australia, and flat per-piece fees.
 */
public sealed interface FeeRule {

    BigDecimal rate();

    /** Priced on the piece's bounding box, each dimension rounded up first in the rule's own unit. */
    record PerVolume(BigDecimal rate, VolumeUnit unit, DimensionRounding rounding) implements FeeRule {
        public PerVolume {
            requireRate(rate);
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(rounding, "rounding");
        }
    }

    /** Priced on weight, rounded up in the rule's own unit. */
    record PerWeight(BigDecimal rate, WeightUnit unit, DimensionRounding rounding) implements FeeRule {
        public PerWeight {
            requireRate(rate);
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(rounding, "rounding");
            if (unit != WeightUnit.POUND && unit != WeightUnit.KILOGRAM) {
                throw new IllegalArgumentException("Weight rates are per pound or per kilogram");
            }
        }
    }

    /** The same price for every piece. */
    record Flat(BigDecimal rate) implements FeeRule {
        public Flat {
            requireRate(rate);
        }
    }

    private static void requireRate(BigDecimal rate) {
        Objects.requireNonNull(rate, "rate");
        if (rate.signum() < 0) {
            throw new IllegalArgumentException("A rate can't be negative");
        }
    }

    /** The unit a volume rate is quoted in, and the length unit its dimensions are rounded in. */
    enum VolumeUnit {

        CUBIC_INCH("in³", LengthUnit.INCH, BigDecimal.ONE),
        CUBIC_CENTIMETRE("cm³", LengthUnit.CENTIMETRE, BigDecimal.ONE),
        // Dimensions in centimetres; 1,000 cm³ to the litre
        LITRE("L", LengthUnit.CENTIMETRE, new BigDecimal("1000"));

        final String symbol;
        final LengthUnit lengthUnit;
        final BigDecimal cubicLengthUnitsPerUnit;

        VolumeUnit(String symbol, LengthUnit lengthUnit, BigDecimal cubicLengthUnitsPerUnit) {
            this.symbol = symbol;
            this.lengthUnit = lengthUnit;
            this.cubicLengthUnitsPerUnit = cubicLengthUnitsPerUnit;
        }

    }

    /** How a measured dimension (or weight) is rounded before pricing. Always upwards. */
    enum DimensionRounding {
        NONE,
        HALF_UNIT,
        WHOLE_UNIT
    }

}
