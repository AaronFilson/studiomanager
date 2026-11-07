package io.github.aaronfilson.studiomanager.invoice;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The one shape Claude may answer in. The SDK turns this record into the JSON schema for structured
 * output, so the model can't answer anything else.
 *
 * <p>Every field is a string, with "" for anything the invoice doesn't show: amounts stay exact
 * decimals instead of passing through a binary double, and nothing is trusted until {@link
 * #toDraft} and then {@link InvoiceDrafts#sanitize} check it. The components are nullable because a
 * recorded answer read back for the eval could leave one out.
 */
record ClaudeInvoiceAnswer(
    @JsonPropertyDescription("The supplier's name as printed, or \"\"") @Nullable String vendorName,
    @JsonPropertyDescription("The invoice number, or \"\"") @Nullable String invoiceNumber,
    @JsonPropertyDescription("The invoice date as YYYY-MM-DD, or \"\"")
        @Nullable String invoiceDate,
    @JsonPropertyDescription(
            "The subtotal before tax and shipping, as a plain decimal like 1234.50, or \"\"")
        @Nullable String subtotal,
    @JsonPropertyDescription("Shipping or freight, as a plain decimal, or \"\"")
        @Nullable String shipping,
    @JsonPropertyDescription("Sales tax or VAT, as a plain decimal, or \"\"") @Nullable String tax,
    @JsonPropertyDescription("The amount due, as a plain decimal, or \"\"") @Nullable String total,
    @JsonPropertyDescription("Every line item, in the order printed") @Nullable List<Line> lines) {

  record Line(
      @JsonPropertyDescription("What the line is for, as printed") @Nullable String description,
      @JsonPropertyDescription("The supplier's item code or SKU, or \"\"") @Nullable String sku,
      @JsonPropertyDescription("The quantity, as a plain decimal, or \"\"")
          @Nullable String quantity,
      @JsonPropertyDescription("The unit, such as bag, lb or each, or \"\"") @Nullable String unit,
      @JsonPropertyDescription("The price of one unit, as a plain decimal, or \"\"")
          @Nullable String unitPrice,
      @JsonPropertyDescription("The line's total, as a plain decimal, or \"\"")
          @Nullable String lineTotal) {}

  /**
   * A plain decimal: digits, an optional point, and a sign. No thousands separators or currency.
   */
  private static final Pattern DECIMAL = Pattern.compile("-?\\d{1,10}(\\.\\d{1,6})?");

  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  /** The answer as a draft. Anything that isn't a well-formed decimal or date becomes null. */
  InvoiceDraft toDraft() {
    List<Line> answered = lines == null ? List.of() : lines;
    return new InvoiceDraft(
        vendorName,
        invoiceNumber,
        date(invoiceDate),
        decimal(subtotal),
        decimal(shipping),
        decimal(tax),
        decimal(total),
        answered.stream()
            .map(line ->
                new InvoiceDraft.Line(
                    line.description() == null ? "" : line.description(),
                    line.sku(),
                    decimal(line.quantity()),
                    line.unit(),
                    decimal(line.unitPrice()),
                    decimal(line.lineTotal())))
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
}
