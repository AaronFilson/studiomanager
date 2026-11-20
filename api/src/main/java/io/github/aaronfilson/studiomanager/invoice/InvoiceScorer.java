package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.InvoiceDraft.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Scores a reader's draft against an expected (or staff-confirmed) one (docs/planning/16,
 * "Evaluation"; extended for schema v2 in docs/planning/24). Numbers and dates must match exactly;
 * text is compared normalized (case, punctuation and spacing don't count). A line is right when its
 * quantity, unit price, total and (when expected sets it) kind all match; lines are aligned in
 * order first, so one missed line costs one line.
 *
 * <p>V2 header fields ({@code documentType}, {@code invoiceDateYear}, {@code payments}) are scored
 * whenever the expected draft specifies them, keeping full backward compatibility with v1 eval
 * cases.
 */
public final class InvoiceScorer {

  /** The core v1 header fields scored on every invoice. */
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
    int total = HEADER_FIELDS;
    right += sameText(expected.vendorName(), read.vendorName()) ? 1 : 0;
    right += sameText(expected.invoiceNumber(), read.invoiceNumber()) ? 1 : 0;
    right += sameDate(expected.invoiceDate(), read.invoiceDate()) ? 1 : 0;
    right += sameAmount(expected.subtotal(), read.subtotal()) ? 1 : 0;
    right += sameAmount(expected.shipping(), read.shipping()) ? 1 : 0;
    right += sameAmount(expected.tax(), read.tax()) ? 1 : 0;
    right += sameAmount(expected.total(), read.total()) ? 1 : 0;

    if (expected.documentType() != null) {
      total++;
      right += expected.documentType() == read.documentType() ? 1 : 0;
    }
    if (expected.invoiceDateYear() != null) {
      total++;
      right += expected.invoiceDateYear() == read.invoiceDateYear() ? 1 : 0;
    }
    if (!expected.payments().isEmpty()) {
      total++;
      right += samePayments(expected.payments(), read.payments()) ? 1 : 0;
    }

    int linesTotal = Math.max(expected.lines().size(), read.lines().size());
    int linesRight = alignedLinesRight(expected.lines(), read.lines());
    return new Score(
        right, total, linesRight, linesTotal, right == total && linesRight == linesTotal);
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
    if (!sameAmount(expected.quantity(), read.quantity())
        || !sameAmount(expected.unitPrice(), read.unitPrice())
        || !sameAmount(expected.lineTotal(), read.lineTotal())) {
      return false;
    }
    if (expected.kind() != null && expected.kind() != read.effectiveKind()) {
      return false;
    }
    if (expected.lineTotal() == null && expected.unitPrice() == null) {
      return sameText(expected.description(), read.description());
    }
    return true;
  }

  private static boolean samePayments(List<Payment> expected, List<Payment> read) {
    if (expected.size() != read.size()) {
      return false;
    }
    List<Payment> remaining = new ArrayList<>(read);
    for (Payment target : expected) {
      int found = -1;
      for (int i = 0; i < remaining.size(); i++) {
        Payment candidate = remaining.get(i);
        if (samePayment(target, candidate)) {
          found = i;
          break;
        }
      }
      if (found < 0) {
        return false;
      }
      remaining.remove(found);
    }
    return true;
  }

  private static boolean samePayment(Payment expected, Payment read) {
    return sameDate(expected.date(), read.date())
        && sameAmount(expected.amount(), read.amount())
        && Objects.equals(expected.method(), read.method())
        && Objects.equals(expected.source(), read.source());
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
