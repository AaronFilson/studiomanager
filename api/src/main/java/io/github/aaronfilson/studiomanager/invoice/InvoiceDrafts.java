package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The gate between a reader and the database. An invoice is untrusted input sent to a model or a
 * service, so every value it comes back with is checked here: text is cleaned and kept short,
 * amounts are decimals within limits, and dates are sane. A value that fails is dropped (left for
 * staff to fill in) and noted, never guessed.
 */
final class InvoiceDrafts {

  static final int MAX_LINES = 200;
  static final int MAX_PAYMENTS = 20;
  static final int MAX_CROSSED_OUT = 50;
  static final int MAX_NOTES = 5;
  static final int MAX_SERIAL_NUMBERS = 20;
  static final int VENDOR_LENGTH = 120;
  static final int NUMBER_LENGTH = 60;
  static final int DESCRIPTION_LENGTH = 200;
  static final int SKU_LENGTH = 60;
  static final int UNIT_LENGTH = 30;
  static final int OTHER_PAPERS_LENGTH = 300;
  static final int NOTE_LENGTH = 200;

  /** Larger than any studio's supply invoice; anything beyond it is a misread. */
  private static final BigDecimal LARGEST_AMOUNT = new BigDecimal("9999999.99");

  private static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);

  private InvoiceDrafts() {}

  /** A cleaned draft, and what had to be dropped from it. */
  record Sanitized(InvoiceDraft draft, List<String> notes) {}

  static Sanitized sanitize(InvoiceDraft read, LocalDate today) {
    List<String> notes = new ArrayList<>();
    List<InvoiceDraft.Line> lines = new ArrayList<>();
    for (InvoiceDraft.Line line : read.lines()) {
      if (lines.size() == MAX_LINES) {
        notes.add("Only the first %d lines were kept.".formatted(MAX_LINES));
        break;
      }
      String description = text(line.description(), DESCRIPTION_LENGTH);
      if (description == null) {
        notes.add("A line with no description was left out.");
        continue;
      }
      lines.add(
          new InvoiceDraft.Line(
              description,
              text(line.sku(), SKU_LENGTH),
              amount(line.quantity(), 3, "a quantity", notes),
              text(line.unit(), UNIT_LENGTH),
              amount(line.unitPrice(), 4, "a unit price", notes),
              amount(line.lineTotal(), 2, "a line total", notes),
              line.kind(),
              cleanList(line.serialNumbers(), MAX_SERIAL_NUMBERS, SKU_LENGTH),
              line.unclear()));
    }
    List<InvoiceDraft.Payment> payments = new ArrayList<>();
    for (InvoiceDraft.Payment payment : read.payments()) {
      if (payments.size() == MAX_PAYMENTS) {
        break;
      }
      BigDecimal paid = amount(payment.amount(), 2, "a payment amount", notes);
      if (paid == null) {
        continue;
      }
      payments.add(
          new InvoiceDraft.Payment(
              date(payment.date(), today.plusYears(1), "a payment date", notes),
              paid,
              payment.method(),
              payment.source()));
    }
    LocalDate invoiceDate = date(read.invoiceDate(), today.plusYears(1), "the invoice date", notes);
    InvoiceDraft cleaned =
        new InvoiceDraft(
            read.documentType(),
            text(read.vendorName(), VENDOR_LENGTH),
            text(read.invoiceNumber(), NUMBER_LENGTH),
            text(read.orderNumber(), NUMBER_LENGTH),
            invoiceDate,
            text(read.invoiceDatePrinted(), NUMBER_LENGTH),
            invoiceDate == null ? null : read.invoiceDateYear(),
            date(read.dueDate(), today.plusYears(2), "the due date", notes),
            amount(read.subtotal(), 2, "the subtotal", notes),
            amount(read.shipping(), 2, "the shipping", notes),
            amount(read.tax(), 2, "the tax", notes),
            amount(read.total(), 2, "the total", notes),
            amount(read.amountPaid(), 2, "the amount paid", notes),
            amount(read.amountDue(), 2, "the amount due", notes),
            payments,
            cleanList(read.crossedOut(), MAX_CROSSED_OUT, DESCRIPTION_LENGTH),
            text(read.otherPapers(), OTHER_PAPERS_LENGTH),
            cleanList(read.notes(), MAX_NOTES, NOTE_LENGTH),
            rotation(read.rotation()),
            lines);
    return new Sanitized(cleaned, List.copyOf(notes));
  }

  /**
   * Strips control characters and runs of whitespace, and cuts to {@code maxLength}. Blank is null.
   */
  static @Nullable String text(@Nullable String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String cleaned = value.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
    if (cleaned.isEmpty()) {
      return null;
    }
    return cleaned.length() > maxLength ? cleaned.substring(0, maxLength).strip() : cleaned;
  }

  private static List<String> cleanList(List<String> raw, int limit, int maxLength) {
    List<String> cleaned = new ArrayList<>();
    for (String entry : raw) {
      if (cleaned.size() == limit) {
        break;
      }
      String safe = text(entry, maxLength);
      if (safe != null) {
        cleaned.add(safe);
      }
    }
    return List.copyOf(cleaned);
  }

  private static @Nullable Integer rotation(@Nullable Integer value) {
    if (value == null) {
      return null;
    }
    return switch (value) {
      case 0, 90, 180, 270 -> value;
      default -> null;
    };
  }

  private static @Nullable BigDecimal amount(
      @Nullable BigDecimal value, int scale, String what, List<String> notes) {
    if (value == null) {
      return null;
    }
    if (value.abs().compareTo(LARGEST_AMOUNT) > 0) {
      notes.add("%s was too large to be right, so it was left out.".formatted(capitalized(what)));
      return null;
    }
    return value.setScale(scale, RoundingMode.HALF_UP);
  }

  private static @Nullable LocalDate date(
      @Nullable LocalDate value, LocalDate latest, String what, List<String> notes) {
    if (value == null) {
      return null;
    }
    if (value.isBefore(EARLIEST_DATE) || value.isAfter(latest)) {
      notes.add("%s didn't look right, so it was left out.".formatted(capitalized(what)));
      return null;
    }
    return value;
  }

  private static String capitalized(String text) {
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
