package io.github.aaronfilson.studiomanager.platform.measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MeasureTests {

    @Test
    void convertsLengthsExactly() {
        assertThat(Length.of("10", LengthUnit.INCH).in(LengthUnit.CENTIMETRE)).isEqualByComparingTo("25.4");
        assertThat(Length.of("254", LengthUnit.MILLIMETRE).in(LengthUnit.INCH)).isEqualByComparingTo("10");
    }

    @Test
    void convertsWeightsExactly() {
        assertThat(Weight.of("1", WeightUnit.POUND).in(WeightUnit.GRAM)).isEqualByComparingTo("453.59237");
        assertThat(Weight.of("16", WeightUnit.OUNCE).in(WeightUnit.POUND)).isEqualByComparingTo("1");
    }

    @Test
    void keepsTheValueAsEntered() {
        Length length = Length.of("12.50", LengthUnit.INCH);

        assertThat(length.in(LengthUnit.INCH)).isSameAs(length.value());
        assertThat(length).hasToString("12.5 in");
    }

    @Test
    void refusesZeroAndNegativeMeasurements() {
        assertThatThrownBy(() -> Length.of("0", LengthUnit.CENTIMETRE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Weight.of("-1", WeightUnit.KILOGRAM)).isInstanceOf(IllegalArgumentException.class);
    }

}
