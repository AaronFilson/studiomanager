package io.github.aaronfilson.studiomanager.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The one shape Claude may answer in (schema v2, docs/planning/24). The SDK turns this record into
 * the JSON schema for structured output, so the model can't answer anything else.
 *
 * <p>Every value is a string (or list of strings / booleans), with "" for anything the document
 * doesn't show: amounts stay exact decimals instead of passing through a binary double, and nothing
 * is trusted until {@link #toDraft} and then {@link InvoiceDrafts#sanitize} check it. All v2 fields
 * are nullable so v1 recorded eval answers continue to deserialize and score unchanged.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ClaudeInvoiceAnswer(
    @JsonPropertyDescription(
            "One of INVOICE, ORDER_FORM, RECEIPT, PACKING_LIST, CREDIT_MEMO, STATEMENT, QUOTE,"
                + " OTHER, or \"\"")
        @Nullable String documentType,
    @JsonPropertyDescription("The supplier's name as printed, or \"\"") @Nullable String vendorName,
    @JsonPropertyDescription("The invoice number, or \"\"") @Nullable String invoiceNumber,
    @JsonPropertyDescription("The order or PO number, or \"\"") @Nullable String orderNumber,
    @JsonPropertyDescription("The invoice date as YYYY-MM-DD, or \"\"")
        @Nullable String invoiceDate,
    @JsonPropertyDescription("The date exactly as printed on the page, or \"\"")
        @Nullable String invoiceDatePrinted,
    @JsonPropertyDescription(
            "Where the year on invoiceDate came from: PRINTED, FROM_PAGE, ASSUMED, or \"\"")
        @Nullable String invoiceDateYear,
    @JsonPropertyDescription("The due date as YYYY-MM-DD, or \"\"") @Nullable String dueDate,
    @JsonPropertyDescription(
            "The subtotal before tax and shipping, as a plain decimal like 1234.50, or \"\"")
        @Nullable String subtotal,
    @JsonPropertyDescription("Shipping or freight beside the totals, as a plain decimal, or \"\"")
        @Nullable String shipping,
    @JsonPropertyDescription("Sales tax or VAT, as a plain decimal, or \"\"") @Nullable String tax,
    @JsonPropertyDescription("The total printed on the document, as a plain decimal, or \"\"")
        @Nullable String total,
    @JsonPropertyDescription("Amount already paid as printed, as a plain decimal, or \"\"")
        @Nullable String amountPaid,
    @JsonPropertyDescription("Balance or amount due as printed, as a plain decimal, or \"\"")
        @Nullable String amountDue,
    @JsonPropertyDescription(
            "Payments shown on the document or attached receipts/slips; never card digits")
        @Nullable List<Payment> payments,
    @JsonPropertyDescription("Text of any rows crossed out or struck through and left out of lines")
        @Nullable List<String> crossedOut,
    @JsonPropertyDescription(
            "What other papers appear in the photo in a few words, or \"\"; never customer details")
        @Nullable String otherPapers,
    @JsonPropertyDescription(
            "Up to 5 short notes for staff about anything unusual; never customer names, addresses,"
                + " phone numbers, emails or card digits")
        @Nullable List<String> notes,
    @JsonPropertyDescription(
            "Clockwise rotation in degrees needed to make the main document upright: 0, 90, 180 or"
                + " 270, or \"\"")
        @Nullable String rotation,
    @JsonPropertyDescription("Every line item, in the order printed") @Nullable List<Line> lines) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Payment(
      @JsonPropertyDescription("Payment date as YYYY-MM-DD, or \"\"") @Nullable String date,
      @JsonPropertyDescription("Payment amount as a plain decimal") @Nullable String amount,
      @JsonPropertyDescription("One of CARD, CASH, CHECK, ACCOUNT, or \"\"; never card digits")
          @Nullable String method,
      @JsonPropertyDescription("Where this payment was read: INVOICE, RECEIPT, SLIP, or \"\"")
          @Nullable String source) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Line(
      @JsonPropertyDescription("What the line is for, as printed") @Nullable String description,
      @JsonPropertyDescription("The supplier's item code or SKU, or \"\"") @Nullable String sku,
      @JsonPropertyDescription("The quantity, as a plain decimal, or \"\"")
          @Nullable String quantity,
      @JsonPropertyDescription("The unit, such as bag, lb or each, or \"\"") @Nullable String unit,
      @JsonPropertyDescription("The price of one unit, as a plain decimal, or \"\"")
          @Nullable String unitPrice,
      @JsonPropertyDescription("The line's total, as a plain decimal, or \"\"")
          @Nullable String lineTotal,
      @JsonPropertyDescription(
              "One of ITEM, SHIPPING, FEE, DISCOUNT, DEPOSIT, OTHER, or \"\"")
          @Nullable String kind,
      @JsonPropertyDescription("Equipment serial numbers printed on this line, if any")
          @Nullable List<String> serialNumbers,
      @JsonPropertyDescription("True when the handwriting or print on this line was hard to read")
          @Nullable Boolean unclear) {}

  /**
   * A plain decimal: digits, an optional point, and a sign. No thousands separators or currency.
   */
  private static final Pattern DECIMAL = Pattern.compile("-?\\d{1,10}(\\.\\d{1,6})?");

  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  /** The answer as a draft. Anything that isn't a well-formed decimal, date or enum becomes null. */
  InvoiceDraft toDraft() {
    List<Line> answered = lines == null ? List.of() : lines;
    List<Payment> answeredPayments = payments == null ? List.of() : payments;
    return new InvoiceDraft(
        documentType(documentType),
        vendorName,
        invoiceNumber,
        orderNumber,
        date(invoiceDate),
        invoiceDatePrinted,
        dateYearSource(invoiceDateYear),
        date(dueDate),
        decimal(subtotal),
        decimal(shipping),
        decimal(tax),
        decimal(total),
        decimal(amountPaid),
        decimal(amountDue),
        answeredPayments.stream()
            .flatMap(p -> {
              BigDecimal amount = decimal(p.amount());
              if (amount == null) {
                return java.util.stream.Stream.empty();
              }
              return java.util.stream.Stream.of(
                  new InvoiceDraft.Payment(
                      date(p.date()),
                      amount,
                      paymentMethod(p.method()),
                      paymentSource(p.source())));
            })
            .toList(),
        nonNullStrings(crossedOut),
        otherPapers,
        nonNullStrings(notes),
        rotation(rotation),
        answered.stream()
            .map(line ->
                new InvoiceDraft.Line(
                    line.description() == null ? "" : line.description(),
                    line.sku(),
                    decimal(line.quantity()),
                    line.unit(),
                    decimal(line.unitPrice()),
                    decimal(line.lineTotal()),
                    lineKind(line.kind()),
                    nonNullStrings(line.serialNumbers()),
                    Boolean.TRUE.equals(line.unclear())))
            .toList());
  }

  /** Serializes a draft back into the eval {@code expected.json} / answer shape. */
  static ClaudeInvoiceAnswer fromDraft(InvoiceDraft draft) {
    return new ClaudeInvoiceAnswer(
        draft.documentType() == null ? "" : draft.documentType().name(),
        orEmpty(draft.vendorName()),
        orEmpty(draft.invoiceNumber()),
        orEmpty(draft.orderNumber()),
        draft.invoiceDate() == null ? "" : draft.invoiceDate().toString(),
        orEmpty(draft.invoiceDatePrinted()),
        draft.invoiceDateYear() == null ? "" : draft.invoiceDateYear().name(),
        draft.dueDate() == null ? "" : draft.dueDate().toString(),
        plain(draft.subtotal()),
        plain(draft.shipping()),
        plain(draft.tax()),
        plain(draft.total()),
        plain(draft.amountPaid()),
        plain(draft.amountDue()),
        draft.payments().stream()
            .map(p ->
                new Payment(
                    p.date() == null ? "" : p.date().toString(),
                    p.amount().toPlainString(),
                    p.method() == null ? "" : p.method().name(),
                    p.source() == null ? "" : p.source().name()))
            .toList(),
        draft.crossedOut(),
        orEmpty(draft.otherPapers()),
        draft.notes(),
        draft.rotation() == null ? "" : String.valueOf(draft.rotation()),
        draft.lines().stream()
            .map(line ->
                new Line(
                    line.description(),
                    orEmpty(line.sku()),
                    plain(line.quantity()),
                    orEmpty(line.unit()),
                    plain(line.unitPrice()),
                    plain(line.lineTotal()),
                    line.kind() == null ? "" : line.kind().name(),
                    line.serialNumbers(),
                    line.unclear()))
            .toList());
  }

  static @Nullable BigDecimal decimal(@Nullable String text) {
    if (text == null) {
      return null;
    }
    String trimmed = text.strip();
    return DECIMAL.matcher(trimmed).matches() ? new BigDecimal(trimmed) : null;
  }

  static @Nullable LocalDate date(@Nullable String text) {
    if (text == null) {
      return null;
    }
    String trimmed = text.strip();
    if (!ISO_DATE.matcher(trimmed).matches()) {
      return null;
    }
    try {
      return LocalDate.parse(trimmed);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  static InvoiceDraft.@Nullable DocumentType documentType(@Nullable String text) {
    return parseEnum(text, InvoiceDraft.DocumentType.class);
  }

  static InvoiceDraft.@Nullable DateYearSource dateYearSource(@Nullable String text) {
    return parseEnum(text, InvoiceDraft.DateYearSource.class);
  }

  static InvoiceDraft.@Nullable LineKind lineKind(@Nullable String text) {
    return parseEnum(text, InvoiceDraft.LineKind.class);
  }

  static InvoiceDraft.@Nullable PaymentMethod paymentMethod(@Nullable String text) {
    return parseEnum(text, InvoiceDraft.PaymentMethod.class);
  }

  static InvoiceDraft.@Nullable PaymentSource paymentSource(@Nullable String text) {
    return parseEnum(text, InvoiceDraft.PaymentSource.class);
  }

  static @Nullable Integer rotation(@Nullable String text) {
    if (text == null) {
      return null;
    }
    return switch (text.strip()) {
      case "0" -> 0;
      case "90" -> 90;
      case "180" -> 180;
      case "270" -> 270;
      default -> null;
    };
  }

  private static <E extends Enum<E>> @Nullable E parseEnum(
      @Nullable String text, Class<E> enumType) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(enumType, text.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static List<String> nonNullStrings(@Nullable List<String> raw) {
    return raw == null ? List.of() : raw.stream().filter(Objects::nonNull).toList();
  }

  private static String orEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }

  private static String plain(@Nullable BigDecimal value) {
    return value == null ? "" : value.toPlainString();
  }
}
