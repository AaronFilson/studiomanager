package io.github.aaronfilson.studiomanager.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import org.junit.jupiter.api.Test;

class ImportValuesTests {

    @Test
    void moneyAsPeopleTypeIt() {
        assertThat(ImportValues.money("$1,234.50")).isEqualByComparingTo("1234.50");
        assertThat(ImportValues.money("(12.00)")).isEqualByComparingTo("-12");
        assertThat(ImportValues.money("-$3")).isEqualByComparingTo("-3");
        assertThat(ImportValues.money("USD 7.25")).isEqualByComparingTo("7.25");
        assertThat(ImportValues.money("  ")).isNull();
        assertThatThrownBy(() -> ImportValues.money("12 dollars")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> ImportValues.money("1.2.3")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void sharesAsFractionsOrPercentages() {
        assertThat(ImportValues.share("60%")).isEqualByComparingTo("0.6");
        assertThat(ImportValues.share("0.6")).isEqualByComparingTo("0.6");
        assertThat(ImportValues.share("60")).isEqualByComparingTo("0.6");
        assertThat(ImportValues.share("1")).isEqualByComparingTo(BigDecimal.ONE);
        assertThatThrownBy(() -> ImportValues.share("most")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void slashedDatesFollowTheStudiosCountry() {
        assertThat(ImportValues.date("3/4/2026", "US")).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThat(ImportValues.date("3/4/2026", "GB")).isEqualTo(LocalDate.of(2026, 4, 3));
        assertThat(ImportValues.date("2026-03-04", "GB")).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThatThrownBy(() -> ImportValues.date("2/30/2026", "US")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void emailsAreCheckedAndLowerCased() {
        assertThat(ImportValues.email(" Mo@Example.TEST ")).isEqualTo("mo@example.test");
        assertThatThrownBy(() -> ImportValues.email("mo at example")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void wholeNumbers() {
        assertThat(ImportValues.wholeNumber("1,000")).isEqualTo(1000);
        assertThatThrownBy(() -> ImportValues.wholeNumber("2.5")).isInstanceOf(InvalidInputException.class);
    }

}
