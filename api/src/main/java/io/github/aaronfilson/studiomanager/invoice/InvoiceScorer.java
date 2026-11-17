package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Scores a reader's draft against a hand-written expected one (docs/planning/16, "Evaluation").
 * Numbers and dates must match exactly; text is compared normalized (case, punctuation and spacing
 * don't count). A line is right when its quantity, unit price and total all match; lines are
 * aligned in order first, so one missed line costs one line.
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
    int linesRight = alignedLinesRight(expected.lines(), read.lines());
    return new Score(
        right,
        HEADER_FIELDS,
        linesRight,
        linesTotal,
        right == HEADER_FIELDS && linesRight == linesTotal);
  }

  /**
   * How many lines are right once the two lists are aligned in order: the longest run of matching
   * lines that keeps both orders. Comparing line N with line N would mark every line after a missed
   * or extra one wrong (docs/planning/24, "Scoring changes").
   */
  private static int alignedLinesRight(
      List<InvoiceDraft.Line> expected, List<InvoiceDraft.Line> read) {
    int[][] best = new int[expected.size() + 1][read.size() + 1];
    for (int e = expected.size() - 1; e >= 0; e--) {
      for (int r = read.size() - 1; r >= 0; r--) {
        best[e][r] =
            sameLine(expected.get(e), read.get(r))
                ? best[e + 1][r + 1] + 1
                : Math.max(best[e + 1][r], best[e][r + 1]);
      }
    }
    return best[0][0];
  }

  /**
   * How the checks did on one draft: what staff are told, scored, not just what the reader said
   * (docs/planning/24, "Scoring changes" 2). The expected draft's own problems are real (a total
   * that covers only part of a paid-in-two order), so they never count as false alarms.
   *
   * @param falseAlarms messages raised on a draft that was read right, beyond the expected draft's
   * @param missed a draft that needs edits, with nothing raised to say so
   */
  public record Checked(int falseAlarms, boolean missed) {}

  public static Checked checks(InvoiceDraft expected, InvoiceDraft read, Score score) {
    List<String> raised = InvoiceChecks.problems(read);
    if (score.needsNoEdits()) {
      List<String> real = InvoiceChecks.problems(expected);
      return new Checked(
          (int) raised.stream().filter(problem -> !real.contains(problem)).count(), false);
    }
    return new Checked(0, raised.isEmpty());
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
