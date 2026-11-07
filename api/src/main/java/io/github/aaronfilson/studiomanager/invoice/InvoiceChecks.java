package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Whether a draft adds up (docs/planning/16, "Checks"). The lines must add up to the subtotal
 * within a cent per line, and the subtotal plus tax and shipping to the total within a cent. A
 * draft that doesn't add up needs a look; nothing is silently corrected.
 */
final class InvoiceChecks {

  private static final BigDecimal CENT = new BigDecimal("0.01");

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
    for (int index = 0; index < draft.lines().size(); index++) {
      InvoiceDraft.Line line = draft.lines().get(index);
      if (line.quantity() == null || line.unitPrice() == null || line.lineTotal() == null) {
        problems.add("Line %d is missing its quantity, unit price or total.".formatted(index + 1));
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
        problems.add(
            "The subtotal, tax and shipping add up to %s, but the total is %s."
                .formatted(expected.toPlainString(), draft.total().toPlainString()));
      }
    }
    return List.copyOf(problems);
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
