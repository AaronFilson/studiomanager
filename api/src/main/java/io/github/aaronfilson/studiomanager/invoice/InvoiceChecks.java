package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Whether a draft adds up (docs/planning/16, "Checks"; reworked in docs/planning/24). A line is
 * whole with a quantity and an amount: many invoices, handwritten counter forms above all, print no
 * unit price, and that isn't a problem. Where a unit price is printed, quantity × price must make
 * the amount. The lines must add up to the subtotal within a cent per line, and the subtotal (or
 * the lines) plus tax and shipping to the total within a cent. A draft that doesn't add up needs a
 * look; nothing is silently corrected.
 *
 * <p>Each kind of problem is one message, naming the lines it's about, so a form of twelve lines
 * without unit prices gets no message, and one of twelve lines without quantities gets one.
 */
final class InvoiceChecks {

  private static final BigDecimal CENT = new BigDecimal("0.01");

  /** How many lines a message names before it says "and N more". */
  private static final int NAMED_LINES = 4;

  private InvoiceChecks() {}

  /** What doesn't add up, in words for staff; empty when everything does. */
  static List<String> problems(InvoiceDraft draft) {
    List<String> problems = new ArrayList<>();
    if (draft.lines().isEmpty()) {
      problems.add("No lines were read.");
    }
    if (draft.total() == null) {
      problems.add("No total was read.");
    }
    List<String> noQuantity = new ArrayList<>();
    List<String> noAmount = new ArrayList<>();
    for (int index = 0; index < draft.lines().size(); index++) {
      InvoiceDraft.Line line = draft.lines().get(index);
      String name = "line %d (%s)".formatted(index + 1, line.description());
      if (line.quantity() == null) {
        noQuantity.add(name);
      }
      if (line.lineTotal() == null) {
        noAmount.add(name);
      }
      if (line.quantity() == null || line.unitPrice() == null || line.lineTotal() == null) {
        continue;
      }
      BigDecimal extended =
          line.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP);
      if (differsBy(extended, line.lineTotal(), CENT)) {
        problems.add(
            "Line %d: %s × %s is %s, not %s."
                .formatted(
                    index + 1,
                    line.quantity().stripTrailingZeros().toPlainString(),
                    line.unitPrice().stripTrailingZeros().toPlainString(),
                    extended.toPlainString(),
                    line.lineTotal().toPlainString()));
      }
    }
    if (!noQuantity.isEmpty()) {
      problems.add(lines(noQuantity, "has no quantity", "have no quantity"));
    }
    if (!noAmount.isEmpty()) {
      problems.add(lines(noAmount, "has no amount", "have no amount"));
    }
    BigDecimal linesSum = sumOfLineTotals(draft);
    if (draft.subtotal() != null && linesSum != null) {
      BigDecimal tolerance = CENT.multiply(BigDecimal.valueOf(Math.max(1, draft.lines().size())));
      if (differsBy(linesSum, draft.subtotal(), tolerance)) {
        problems.add(
            "The lines add up to %s, but the subtotal is %s."
                .formatted(linesSum.toPlainString(), draft.subtotal().toPlainString()));
      }
    }
    BigDecimal beforeExtras = draft.subtotal() != null ? draft.subtotal() : linesSum;
    if (draft.total() != null && beforeExtras != null) {
      BigDecimal expected = beforeExtras.add(orZero(draft.tax())).add(orZero(draft.shipping()));
      if (differsBy(expected, draft.total(), CENT)) {
        problems.add(totalProblem(draft, expected, draft.total()));
      }
    }
    return List.copyOf(problems);
  }

  /**
   * The total that doesn't match, said so staff can tell what to look for: what was added, how far
   * the written total is from it, and the usual reasons when it's less (a second payment, a
   * deposit, a discount nobody wrote down).
   */
  private static String totalProblem(InvoiceDraft draft, BigDecimal expected, BigDecimal total) {
    String added =
        draft.subtotal() != null
            ? extras(draft, "The subtotal")
            : extras(draft, draft.lines().size() == 1 ? "The line" : "The lines");
    String verb = added.contains(" and ") || added.startsWith("The lines") ? "add" : "adds";
    BigDecimal gap = expected.subtract(total);
    if (gap.signum() > 0) {
      return ("%s %s up to %s. The total is %s, %s less: check for a part paid separately, a"
              + " deposit or an unwritten discount.")
          .formatted(
              added, verb, expected.toPlainString(), total.toPlainString(), gap.toPlainString());
    }
    return ("%s %s up to %s. The total is %s, %s more: check for tax, shipping or a fee that"
            + " wasn't read.")
        .formatted(
            added,
            verb,
            expected.toPlainString(),
            total.toPlainString(),
            gap.negate().toPlainString());
  }

  private static String extras(InvoiceDraft draft, String start) {
    boolean tax = draft.tax() != null;
    boolean shipping = draft.shipping() != null;
    if (tax && shipping) {
      return start + ", tax and shipping";
    }
    if (tax) {
      return start + " and tax";
    }
    if (shipping) {
      return start + " and shipping";
    }
    return start;
  }

  private static String lines(List<String> names, String one, String many) {
    String named = String.join(", ", names.subList(0, Math.min(NAMED_LINES, names.size())));
    String more =
        names.size() > NAMED_LINES ? " and %d more".formatted(names.size() - NAMED_LINES) : "";
    String sentence =
        names.size() == 1
            ? "%s %s.".formatted(named, one)
            : "%s%s %s.".formatted(named, more, many);
    return Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
  }

  /** The sum of the lines' totals, or null if any line has none. */
  private static @Nullable BigDecimal sumOfLineTotals(InvoiceDraft draft) {
    if (draft.lines().isEmpty()
        || draft.lines().stream().map(InvoiceDraft.Line::lineTotal).anyMatch(Objects::isNull)) {
      return null;
    }
    return draft.lines().stream()
        .map(line -> Objects.requireNonNull(line.lineTotal()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static boolean differsBy(BigDecimal left, BigDecimal right, BigDecimal tolerance) {
    return left.subtract(right).abs().compareTo(tolerance) > 0;
  }

  private static BigDecimal orZero(@Nullable BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
