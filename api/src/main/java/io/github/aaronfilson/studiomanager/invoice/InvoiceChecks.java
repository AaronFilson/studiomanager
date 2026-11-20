package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.InvoiceDraft.DocumentType;
import io.github.aaronfilson.studiomanager.invoice.InvoiceDraft.Payment;
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
 * the lines) plus tax and shipping to the total within a cent.
 *
 * <p>Schema v2 rules:
 *
 * <ul>
 *   <li>Packing lists ({@link DocumentType#PACKING_LIST}) get no money checks.
 *   <li>Receipts ({@link DocumentType#RECEIPT}) check against what was paid (sum of payments, else
 *       amountPaid, else total).
 *   <li>When payments sum to lines + tax (+ shipping), the gap against the written total is
 *       explained clearly in {@link #explanations} instead of raising a false-alarm problem.
 *   <li>Non-stock lines (shipping, fee, discount, deposit) do not require a quantity.
 * </ul>
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
    if (draft.documentType() == DocumentType.PACKING_LIST) {
      checkQuantitiesOnly(draft, problems);
      return List.copyOf(problems);
    }

    boolean isReceipt = draft.documentType() == DocumentType.RECEIPT;
    BigDecimal linesSum = sumOfLineTotals(draft);
    BigDecimal beforeExtras = draft.subtotal() != null ? draft.subtotal() : linesSum;
    BigDecimal expected =
        beforeExtras == null
            ? null
            : beforeExtras.add(orZero(draft.tax())).add(orZero(draft.shipping()));
    BigDecimal paidSum = sumOfPayments(draft.payments());
    boolean paymentsCoverExpected =
        paidSum != null && expected != null && !differsBy(paidSum, expected, CENT);

    if (isReceipt) {
      BigDecimal receiptPaid = receiptPaidTotal(draft, paidSum);
      if (receiptPaid == null) {
        problems.add("No total was read.");
      }
    } else if (draft.total() == null && !paymentsCoverExpected) {
      problems.add("No total was read.");
    }

    List<String> noQuantity = new ArrayList<>();
    List<String> noAmount = new ArrayList<>();
    for (int index = 0; index < draft.lines().size(); index++) {
      InvoiceDraft.Line line = draft.lines().get(index);
      String name = "line %d (%s)".formatted(index + 1, line.description());
      if (line.effectiveKind().isStockCandidate() && line.quantity() == null) {
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
    if (draft.subtotal() != null && linesSum != null) {
      BigDecimal tolerance = CENT.multiply(BigDecimal.valueOf(Math.max(1, draft.lines().size())));
      if (differsBy(linesSum, draft.subtotal(), tolerance)) {
        problems.add(
            "The lines add up to %s, but the subtotal is %s."
                .formatted(linesSum.toPlainString(), draft.subtotal().toPlainString()));
      }
    }
    if (expected != null) {
      if (isReceipt) {
        BigDecimal receiptPaid = receiptPaidTotal(draft, paidSum);
        if (receiptPaid != null && differsBy(expected, receiptPaid, CENT)) {
          problems.add(totalProblem(draft, expected, receiptPaid));
        }
      } else if (!paymentsCoverExpected
          && draft.total() != null
          && differsBy(expected, draft.total(), CENT)) {
        problems.add(totalProblem(draft, expected, draft.total()));
      }
    }
    return List.copyOf(problems);
  }

  /**
   * Non-problem explanations for staff (for example, split payments that add up to lines + tax even
   * when the written total only shows one payment).
   */
  static List<String> explanations(InvoiceDraft draft) {
    if (draft.documentType() == DocumentType.PACKING_LIST || draft.payments().isEmpty()) {
      return List.of();
    }
    BigDecimal paidSum = sumOfPayments(draft.payments());
    if (paidSum == null) {
      return List.of();
    }
    BigDecimal linesSum = sumOfLineTotals(draft);
    BigDecimal beforeExtras = draft.subtotal() != null ? draft.subtotal() : linesSum;
    if (beforeExtras == null) {
      return List.of();
    }
    BigDecimal expected = beforeExtras.add(orZero(draft.tax())).add(orZero(draft.shipping()));
    if (differsBy(paidSum, expected, CENT)) {
      return List.of();
    }
    boolean totalDiffers = draft.total() == null || differsBy(expected, draft.total(), CENT);
    if (!totalDiffers && draft.payments().size() <= 1) {
      return List.of();
    }
    String parts = formatPayments(draft.payments());
    String equation =
        draft.payments().size() > 1
            ? "%s = %s"
                .formatted(
                    String.join(
                        " + ",
                        draft.payments().stream()
                            .map(p -> p.amount().setScale(2, RoundingMode.HALF_UP).toPlainString())
                            .toList()),
                    paidSum.toPlainString())
            : paidSum.toPlainString();
    String target =
        draft.subtotal() != null
            ? extras(draft, "subtotal")
            : extras(draft, draft.lines().size() == 1 ? "line" : "lines");
    if (draft.total() != null && differsBy(expected, draft.total(), CENT)) {
      return List.of(
          "Paid %s: %s matches %s (written total is %s)."
              .formatted(parts, equation, target, draft.total().toPlainString()));
    }
    return List.of("Paid %s: %s matches %s.".formatted(parts, equation, target));
  }

  /**
   * What is payable: defaults to the sum of payments when payments cover lines and tax (+ shipping),
   * otherwise the written total (or what was paid on a receipt).
   */
  static @Nullable BigDecimal payableAmount(InvoiceDraft draft) {
    if (draft.documentType() == DocumentType.PACKING_LIST) {
      return draft.total();
    }
    BigDecimal paidSum = sumOfPayments(draft.payments());
    if (paidSum != null) {
      BigDecimal linesSum = sumOfLineTotals(draft);
      BigDecimal beforeExtras = draft.subtotal() != null ? draft.subtotal() : linesSum;
      if (beforeExtras != null) {
        BigDecimal expected = beforeExtras.add(orZero(draft.tax())).add(orZero(draft.shipping()));
        if (!differsBy(paidSum, expected, CENT)) {
          return paidSum;
        }
      }
      if (draft.documentType() == DocumentType.RECEIPT || draft.total() == null) {
        return paidSum;
      }
    }
    if (draft.documentType() == DocumentType.RECEIPT && draft.amountPaid() != null) {
      return draft.amountPaid();
    }
    return draft.total();
  }

  private static void checkQuantitiesOnly(InvoiceDraft draft, List<String> problems) {
    List<String> noQuantity = new ArrayList<>();
    for (int index = 0; index < draft.lines().size(); index++) {
      InvoiceDraft.Line line = draft.lines().get(index);
      if (line.effectiveKind().isStockCandidate() && line.quantity() == null) {
        noQuantity.add("line %d (%s)".formatted(index + 1, line.description()));
      }
    }
    if (!noQuantity.isEmpty()) {
      problems.add(lines(noQuantity, "has no quantity", "have no quantity"));
    }
  }

  private static @Nullable BigDecimal receiptPaidTotal(
      InvoiceDraft draft, @Nullable BigDecimal paidSum) {
    if (paidSum != null) {
      return paidSum;
    }
    return draft.amountPaid() != null ? draft.amountPaid() : draft.total();
  }

  private static @Nullable BigDecimal sumOfPayments(List<Payment> payments) {
    if (payments.isEmpty()) {
      return null;
    }
    return payments.stream()
        .map(Payment::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static String formatPayments(List<Payment> payments) {
    List<String> items =
        payments.stream()
            .map(p -> {
              String amount = p.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
              return p.date() == null ? amount : amount + " on " + p.date();
            })
            .toList();
    if (items.size() == 1) {
      return items.getFirst();
    }
    if (items.size() == 2) {
      return items.get(0) + " and " + items.get(1);
    }
    return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.getLast();
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
