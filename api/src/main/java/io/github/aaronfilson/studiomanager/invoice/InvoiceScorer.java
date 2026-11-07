package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Scores a reader's draft against a hand-written expected one (docs/planning/16, "Evaluation").
 * Numbers and dates must match exactly; text is compared normalized (case, punctuation and spacing
 * don't count). A line is right when its quantity, unit price and total all match.
 *
 * <p>Shared by the eval in CI, the live eval report, and lane F1g's "Compare readers".
 */
public final class InvoiceScorer {

  /** The header fields scored on every invoice. */
  static final int HEADER_FIELDS = 7;

  private InvoiceScorer() {}

  /**
   * How one draft did.
   *
   * @param linesTotal the larger of the expected and read line counts, so a missing or extra line
   *     counts against the reader
   * @param needsNoEdits every field and every line right, with nothing missing or extra
   */
  public record Score(
      int fieldsRight, int fieldsTotal, int linesRight, int linesTotal, boolean needsNoEdits) {

    public double fieldAccuracy() {
      return fieldsTotal == 0 ? 1 : (double) fieldsRight / fieldsTotal;
    }

    public double lineAccuracy() {
      return linesTotal == 0 ? 1 : (double) linesRight / linesTotal;
    }
  }

  public static Score score(InvoiceDraft expected, InvoiceDraft read) {
    int right = 0;
    right += sameText(expected.vendorName(), read.vendorName()) ? 1 : 0;
    right += sameText(expected.invoiceNumber(), read.invoiceNumber()) ? 1 : 0;
    right += sameDate(expected.invoiceDate(), read.invoiceDate()) ? 1 : 0;
    right += sameAmount(expected.subtotal(), read.subtotal()) ? 1 : 0;
    right += sameAmount(expected.shipping(), read.shipping()) ? 1 : 0;
    right += sameAmount(expected.tax(), read.tax()) ? 1 : 0;
    right += sameAmount(expected.total(), read.total()) ? 1 : 0;
    int linesTotal = Math.max(expected.lines().size(), read.lines().size());
    int linesRight = 0;
    for (int index = 0; index < Math.min(expected.lines().size(), read.lines().size()); index++) {
      linesRight += sameLine(expected.lines().get(index), read.lines().get(index)) ? 1 : 0;
    }
    return new Score(
        right,
        HEADER_FIELDS,
        linesRight,
        linesTotal,
        right == HEADER_FIELDS && linesRight == linesTotal);
  }

  private static boolean sameLine(InvoiceDraft.Line expected, InvoiceDraft.Line read) {
    return sameAmount(expected.quantity(), read.quantity())
        && sameAmount(expected.unitPrice(), read.unitPrice())
        && sameAmount(expected.lineTotal(), read.lineTotal());
  }

  private static boolean sameText(@Nullable String expected, @Nullable String read) {
    return SupplyMatcher.normalize(expected).equals(SupplyMatcher.normalize(read));
  }

  private static boolean sameDate(@Nullable LocalDate expected, @Nullable LocalDate read) {
    return Objects.equals(expected, read);
  }

  /** Equal in value: 12.5 and 12.50 are the same amount. */
  private static boolean sameAmount(@Nullable BigDecimal expected, @Nullable BigDecimal read) {
    if (expected == null || read == null) {
      return expected == null && read == null;
    }
    return expected.compareTo(read) == 0;
  }
}
