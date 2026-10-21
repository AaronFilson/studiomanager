package io.github.aaronfilson.studiomanager.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvTests {

    @Test
    void quotesWhatNeedsQuotingAndLeavesTheRestAlone() {
        String csv = new Csv().row(List.of("plain", "a, b", "say \"hi\"", "two\nlines")).toString();

        assertThat(csv).isEqualTo("plain,\"a, b\",\"say \"\"hi\"\"\",\"two\nlines\"\r\n");
    }

    @Test
    void defusesTextThatASpreadsheetWouldRunAsAFormula() {
        assertThat(Csv.text("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
        assertThat(Csv.text("+1")).isEqualTo("'+1");
        assertThat(Csv.text("-2")).isEqualTo("'-2");
        assertThat(Csv.text("@cmd")).isEqualTo("'@cmd");
        assertThat(Csv.text("=HYPERLINK(\"x\",\"y\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"");
    }

    @Test
    void writesNumbersAsNumbersAndNullsAsEmpty() {
        String csv = new Csv().row(Arrays.asList(new BigDecimal("-12.50"), -3, null, "")).toString();

        assertThat(csv).isEqualTo("-12.50,-3,,\r\n");
    }

}
