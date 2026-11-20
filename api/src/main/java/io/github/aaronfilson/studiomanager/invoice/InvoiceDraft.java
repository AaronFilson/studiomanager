package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What a reader made of an invoice (schema v2, docs/planning/24), in the same shape whichever
 * reader it was. Any field it couldn't read is null (or an empty list). Readers hand this over
 * unchecked; {@link InvoiceDrafts#sanitize} is the gate.
 */
public record InvoiceDraft(
    @Nullable DocumentType documentType,
    @Nullable String vendorName,
    @Nullable String invoiceNumber,
    @Nullable String orderNumber,
    @Nullable LocalDate invoiceDate,
    @Nullable String invoiceDatePrinted,
    @Nullable DateYearSource invoiceDateYear,
    @Nullable LocalDate dueDate,
    @Nullable BigDecimal subtotal,
    @Nullable BigDecimal shipping,
    @Nullable BigDecimal tax,
    @Nullable BigDecimal total,
    @Nullable BigDecimal amountPaid,
    @Nullable BigDecimal amountDue,
    List<Payment> payments,
    List<String> crossedOut,
    @Nullable String otherPapers,
    List<String> notes,
    @Nullable Integer rotation,
    List<Line> lines) {

  public InvoiceDraft {
    payments = List.copyOf(payments);
    crossedOut = List.copyOf(crossedOut);
    notes = List.copyOf(notes);
    lines = List.copyOf(lines);
  }

  /** V1-shape constructor for callers and tests that only carry the core fields. */
  public InvoiceDraft(
      @Nullable String vendorName,
      @Nullable String invoiceNumber,
      @Nullable LocalDate invoiceDate,
      @Nullable BigDecimal subtotal,
      @Nullable BigDecimal shipping,
      @Nullable BigDecimal tax,
      @Nullable BigDecimal total,
      List<Line> lines) {
    this(
        null,
        vendorName,
        invoiceNumber,
        null,
        invoiceDate,
        null,
        null,
        null,
        subtotal,
        shipping,
        tax,
        total,
        null,
        null,
        List.of(),
        List.of(),
        null,
        List.of(),
        null,
        lines);
  }

  /** What kind of supplier document the photo or PDF is. */
  public enum DocumentType {
    INVOICE,
    ORDER_FORM,
    RECEIPT,
    PACKING_LIST,
    CREDIT_MEMO,
    STATEMENT,
    QUOTE,
    OTHER
  }

  /** Where the year on {@code invoiceDate} came from. */
  public enum DateYearSource {
    PRINTED,
    FROM_PAGE,
    ASSUMED
  }

  /** What kind of row a line is; non-stock rows default to unmatched. */
  public enum LineKind {
    ITEM,
    SHIPPING,
    FEE,
    DISCOUNT,
    DEPOSIT,
    OTHER;

    public boolean isStockCandidate() {
      return this == ITEM || this == OTHER;
    }
  }

  /** How a payment was made. Never card digits. */
  public enum PaymentMethod {
    CARD,
    CASH,
    CHECK,
    ACCOUNT
  }

  /** Which paper in the photo the payment came from. */
  public enum PaymentSource {
    INVOICE,
    RECEIPT,
    SLIP
  }

  /** One payment read from the document or an attached receipt or card slip. */
  public record Payment(
      @Nullable LocalDate date,
      BigDecimal amount,
      @Nullable PaymentMethod method,
      @Nullable PaymentSource source) {}

  /** One line of the invoice. */
  public record Line(
      String description,
      @Nullable String sku,
      @Nullable BigDecimal quantity,
      @Nullable String unit,
      @Nullable BigDecimal unitPrice,
      @Nullable BigDecimal lineTotal,
      @Nullable LineKind kind,
      List<String> serialNumbers,
      boolean unclear) {

    public Line {
      serialNumbers = List.copyOf(serialNumbers);
    }

    /** V1-shape constructor for callers and tests without v2 line fields. */
    public Line(
        String description,
        @Nullable String sku,
        @Nullable BigDecimal quantity,
        @Nullable String unit,
        @Nullable BigDecimal unitPrice,
        @Nullable BigDecimal lineTotal) {
      this(description, sku, quantity, unit, unitPrice, lineTotal, null, List.of(), false);
    }

    /** The kind, defaulting to {@link LineKind#ITEM} when a v1 draft left it out. */
    public LineKind effectiveKind() {
      return kind == null ? LineKind.ITEM : kind;
    }
  }

  /**
   * What is payable: defaults to the sum of payments when payments cover lines and tax, otherwise
   * the written total.
   */
  public @Nullable BigDecimal payableAmount() {
    return InvoiceChecks.payableAmount(this);
  }

  /** A draft with nothing in it, for staff to type in next to the image. */
  public static InvoiceDraft empty() {
    return new InvoiceDraft(null, null, null, null, null, null, null, List.of());
  }
}
