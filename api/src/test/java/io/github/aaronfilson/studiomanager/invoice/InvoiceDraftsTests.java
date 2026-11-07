package io.github.aaronfilson.studiomanager.invoice;

import static io.github.aaronfilson.studiomanager.invoice.InvoiceChecksTests.amount;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What comes back from a reader is untrusted: the gate keeps only sane values. */
class InvoiceDraftsTests {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

  @Test
  void textIsCleanedAndCut() {
    InvoiceDraft read =
        new InvoiceDraft(
            "  Clay\tCo\u0000 ",
            "x".repeat(100),
            null,
            null,
            null,
            null,
            null,
            List.of(new InvoiceDraft.Line("   ", null, null, null, null, null)));

    InvoiceDrafts.Sanitized sanitized = InvoiceDrafts.sanitize(read, TODAY);

    assertThat(sanitized.draft().vendorName()).isEqualTo("Clay Co");
    assertThat(sanitized.draft().invoiceNumber()).hasSize(InvoiceDrafts.NUMBER_LENGTH);
    assertThat(sanitized.draft().lines()).isEmpty();
    assertThat(sanitized.notes()).containsExactly("A line with no description was left out.");
  }

  @Test
  void implausibleAmountsAndDatesAreDroppedNotGuessed() {
    InvoiceDraft read =
        new InvoiceDraft(
            "Clay Co",
            "1",
            LocalDate.of(1999, 12, 31),
            amount("12.345"),
            null,
            null,
            amount("100000000"),
            List.of());

    InvoiceDrafts.Sanitized sanitized = InvoiceDrafts.sanitize(read, TODAY);

    assertThat(sanitized.draft().invoiceDate()).isNull();
    assertThat(sanitized.draft().subtotal()).isEqualByComparingTo("12.35");
    assertThat(sanitized.draft().total()).isNull();
    assertThat(sanitized.notes())
        .containsExactly(
            "The invoice date didn't look right, so it was left out.",
            "The total was too large to be right, so it was left out.");
  }

  @Test
  void claudesAnswerParsesOnlyPlainDecimalsAndIsoDates() {
    assertThat(ClaudeInvoiceAnswer.decimal("1,234.50")).isNull();
    assertThat(ClaudeInvoiceAnswer.decimal("$12")).isNull();
    assertThat(ClaudeInvoiceAnswer.decimal(" -3.50 ")).isEqualByComparingTo("-3.5");
    assertThat(ClaudeInvoiceAnswer.date("2026-02-30")).isNull();
    assertThat(ClaudeInvoiceAnswer.date("10/01/2026")).isNull();
    assertThat(ClaudeInvoiceAnswer.date("2026-10-01")).isEqualTo(LocalDate.of(2026, 10, 1));
  }
}
