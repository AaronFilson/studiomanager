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

  @Test
  void v2FieldsAreSanitizedAndCapped() {
    InvoiceDraft read =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.ORDER_FORM,
            "Clay Co",
            "1",
            "  PO-99\t",
            LocalDate.of(2026, 10, 1),
            " 10/1 ",
            InvoiceDraft.DateYearSource.FROM_PAGE,
            LocalDate.of(1999, 1, 1),
            amount("50.00"),
            null,
            null,
            amount("50.00"),
            amount("50.005"),
            amount("100000000"),
            List.of(
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 1),
                    amount("50.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.SLIP),
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 1),
                    amount("99999999"),
                    InvoiceDraft.PaymentMethod.CASH,
                    InvoiceDraft.PaymentSource.RECEIPT)),
            List.of("  Struck\u0000 row  ", "   "),
            "x".repeat(400),
            List.of("n1", "n2", "n3", "n4", "n5", "n6"),
            45,
            List.of(
                new InvoiceDraft.Line(
                    "Kiln element",
                    "EL-1",
                    amount("1"),
                    "set",
                    amount("50.00"),
                    amount("50.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(" SN-123 ", ""),
                    true)));

    InvoiceDrafts.Sanitized sanitized = InvoiceDrafts.sanitize(read, TODAY);
    InvoiceDraft cleaned = sanitized.draft();

    assertThat(cleaned.orderNumber()).isEqualTo("PO-99");
    assertThat(cleaned.dueDate()).isNull();
    assertThat(cleaned.amountPaid()).isEqualByComparingTo("50.01");
    assertThat(cleaned.amountDue()).isNull();
    assertThat(cleaned.payments()).hasSize(1);
    assertThat(cleaned.crossedOut()).containsExactly("Struck row");
    assertThat(cleaned.otherPapers()).hasSize(InvoiceDrafts.OTHER_PAPERS_LENGTH);
    assertThat(cleaned.notes()).containsExactly("n1", "n2", "n3", "n4", "n5");
    assertThat(cleaned.rotation()).isNull();
    assertThat(cleaned.lines().getFirst().serialNumbers()).containsExactly("SN-123");
    assertThat(cleaned.lines().getFirst().unclear()).isTrue();
  }
}
