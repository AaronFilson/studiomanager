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

  @Test
  void packingListsGetNoMoneyChecks() {
    InvoiceDraft packingList =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.PACKING_LIST,
            "Clay Works",
            null,
            "SO-1",
            LocalDate.of(2026, 10, 23),
            null,
            InvoiceDraft.DateYearSource.PRINTED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            0,
            List.of(
                new InvoiceDraft.Line(
                    "B-Mix 5",
                    null,
                    amount("10"),
                    "bag",
                    null,
                    null,
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false)));

    assertThat(InvoiceChecks.problems(packingList)).isEmpty();
  }

  @Test
  void receiptTotalsCheckAgainstWhatWasPaid() {
    InvoiceDraft receipt =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.RECEIPT,
            "Milltown Mart",
            "R-1",
            null,
            LocalDate.of(2026, 10, 21),
            null,
            InvoiceDraft.DateYearSource.PRINTED,
            null,
            amount("50.00"),
            null,
            amount("4.50"),
            amount("50.00"),
            amount("54.50"),
            BigDecimal.ZERO,
            List.of(
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 21),
                    new BigDecimal("54.50"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.RECEIPT)),
            List.of(),
            null,
            List.of(),
            0,
            List.of(line("Sponges", "10", "5.00", "50.00")));

    assertThat(InvoiceChecks.problems(receipt)).isEmpty();
    assertThat(receipt.payableAmount()).isEqualByComparingTo("54.50");
  }

  @Test
  void splitPaymentsCoveringLinesAndTaxExplainInsteadOfRaisingAFalseAlarm() {
    InvoiceDraft split =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.ORDER_FORM,
            "Counter Supply",
            null,
            "CTR-2",
            LocalDate.of(2026, 10, 18),
            "10/18",
            InvoiceDraft.DateYearSource.FROM_PAGE,
            null,
            amount("100.00"),
            null,
            amount("8.00"),
            amount("60.00"),
            amount("108.00"),
            BigDecimal.ZERO,
            List.of(
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 18),
                    new BigDecimal("60.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.SLIP),
                new InvoiceDraft.Payment(
                    LocalDate.of(2026, 10, 19),
                    new BigDecimal("48.00"),
                    InvoiceDraft.PaymentMethod.CARD,
                    InvoiceDraft.PaymentSource.RECEIPT)),
            List.of(),
            null,
            List.of(),
            90,
            List.of(line("Fireclay", "100", null, "100.00")));

    assertThat(InvoiceChecks.problems(split)).isEmpty();
    assertThat(split.payableAmount()).isEqualByComparingTo("108.00");
    assertThat(InvoiceChecks.explanations(split))
        .singleElement()
        .asString()
        .contains("60.00 on 2026-10-18", "48.00 on 2026-10-19", "108.00", "written total is 60.00");
  }

  @Test
  void nonStockLinesDoNotRequireAQuantity() {
    InvoiceDraft draft =
        new InvoiceDraft(
            InvoiceDraft.DocumentType.INVOICE,
            "Clay Co",
            "INV-2",
            null,
            LocalDate.of(2026, 10, 1),
            null,
            InvoiceDraft.DateYearSource.PRINTED,
            null,
            amount("115.00"),
            null,
            null,
            amount("115.00"),
            null,
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            0,
            List.of(
                new InvoiceDraft.Line(
                    "B-Mix 5",
                    null,
                    amount("4"),
                    "bag",
                    amount("25.00"),
                    amount("100.00"),
                    InvoiceDraft.LineKind.ITEM,
                    List.of(),
                    false),
                new InvoiceDraft.Line(
                    "Freight",
                    null,
                    null,
                    null,
                    null,
                    amount("20.00"),
                    InvoiceDraft.LineKind.SHIPPING,
                    List.of(),
                    false),
                new InvoiceDraft.Line(
                    "Member discount",
                    null,
                    null,
                    null,
                    null,
                    amount("-5.00"),
                    InvoiceDraft.LineKind.DISCOUNT,
                    List.of(),
                    false)));

    assertThat(InvoiceChecks.problems(draft)).isEmpty();
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
