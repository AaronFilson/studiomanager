package io.github.aaronfilson.studiomanager.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class MoneyTests {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void roundsHalfUpToTheMinorUnit() {
        assertThat(Money.rounded(new BigDecimal("2.345"), USD).amount()).isEqualByComparingTo("2.35");
        assertThat(Money.rounded(new BigDecimal("2.344"), USD).amount()).isEqualByComparingTo("2.34");
    }

    @Test
    void refusesToSilentlyDropPrecision() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.005"), USD)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void neverMixesCurrencies() {
        Money dollars = Money.zero(USD);
        Money pounds = Money.zero(Currency.getInstance("GBP"));

        assertThatThrownBy(() -> dollars.plus(pounds)).isInstanceOf(IllegalArgumentException.class);
    }

}
