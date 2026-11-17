package io.github.aaronfilson.studiomanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class InvoiceChecksTests {

  @Test
  void aDraftThatAddsUpHasNoProblems() {
    assertThat(InvoiceChecks.problems(clayOrder("100.00", "8.25", "15.00", "123.25"))).isEmpty();
  }

  @Test
  void aCentPerLineOfRoundingIsAllowed() {
    InvoiceDraft draft =
        new InvoiceDraft(
            "Clay Co",
            "1",
            LocalDate.of(2026, 10, 1),
            amount("10.01"),
            null,
            null,
            amount("10.01"),
            List.of(line("A", "1", "5.00", "5.00"), line("B", "1", "5.00", "5.00")));

    assertThat(InvoiceChecks.problems(draft)).isEmpty();
  }

  @Test
  void aTotalThatDoesNotAddUpIsFlaggedNotCorrected() {
    assertThat(InvoiceChecks.problems(clayOrder("100.00", "8.25", "15.00", "125.00")))
        .containsExactly(
            "The subtotal, tax and shipping add up to 123.25. The total is 125.00, 1.75 more:"
                + " check for tax, shipping or a fee that wasn't read.");
  }

  @Test
  void aHandwrittenFormWithOnlyLineAmountsAddsUpWithoutUnitPrices() {
    InvoiceDraft draft =
        new InvoiceDraft(
            "Counter form",
            null,
            null,
            null,
            null,
            null,
            amount("49.50"),
            List.of(line("Fireclay", "100", null, "17.50"), line("Dolomite", "50", null, "32.00")));

    assertThat(InvoiceChecks.problems(draft)).isEmpty();
  }

  @Test
  void aWrittenTotalBelowTheLinesSaysWhatToLookFor() {
    InvoiceDraft draft =
        new InvoiceDraft(
            "Counter form",
            null,
            null,
            null,
            null,
            null,
            amount("20.00"),
            List.of(line("Fireclay", "100", null, "17.50"), line("Dolomite", "50", null, "32.00")));

    assertThat(InvoiceChecks.problems(draft))
        .containsExactly(
            "The lines add up to 49.50. The total is 20.00, 29.50 less: check for a part paid"
                + " separately, a deposit or an unwritten discount.");
  }

  @Test
  void linesWithoutQuantitiesAreOneMessageNamingThem() {
    InvoiceDraft draft =
        new InvoiceDraft(
            "Counter form",
            null,
            null,
            null,
            null,
            null,
            amount("71.00"),
            List.of(
                line("Fireclay", null, null, "17.50"),
                line("Dolomite", null, null, "32.00"),
                line("Talc", "50", null, "21.50")));

    assertThat(InvoiceChecks.problems(draft))
        .containsExactly("Line 1 (Fireclay), line 2 (Dolomite) have no quantity.");
  }

  @Test
  void aLineWhoseArithmeticIsWrongIsNamed() {
    InvoiceDraft draft =
        new InvoiceDraft(
            "Clay Co",
            "1",
            null,
            amount("60.00"),
            null,
            null,
            amount("60.00"),
            List.of(line("B-Mix 5", "2", "25.00", "60.00")));

    assertThat(InvoiceChecks.problems(draft))
        .containsExactly("Line 1: 2 × 25 is 50.00, not 60.00.");
  }

  @Test
  void anEmptyDraftNeedsALook() {
    assertThat(InvoiceChecks.problems(InvoiceDraft.empty()))
        .containsExactly("No lines were read.", "No total was read.");
  }

  private static InvoiceDraft clayOrder(
      String subtotal, String tax, String shipping, String total) {
    return new InvoiceDraft(
        "Clay Co",
        "INV-1",
        LocalDate.of(2026, 10, 1),
        amount(subtotal),
        amount(shipping),
        amount(tax),
        amount(total),
        List.of(line("B-Mix 5, 50 lb", "4", "25.00", "100.00")));
  }

  static InvoiceDraft.Line line(
      String description, @Nullable String quantity, @Nullable String price, String total) {
    return new InvoiceDraft.Line(
        description, null, amount(quantity), "bag", amount(price), amount(total));
  }

  static @Nullable BigDecimal amount(@Nullable String text) {
    return text == null ? null : new BigDecimal(text);
  }
}
