package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What a reader made of an invoice, in the same shape whichever reader it was. Any field it
 * couldn't read is null. Readers hand this over unchecked; {@link InvoiceDrafts#sanitize} is the
 * gate.
 */
public record InvoiceDraft(
    @Nullable String vendorName,
    @Nullable String invoiceNumber,
    @Nullable LocalDate invoiceDate,
    @Nullable BigDecimal subtotal,
    @Nullable BigDecimal shipping,
    @Nullable BigDecimal tax,
    @Nullable BigDecimal total,
    List<Line> lines) {

  public InvoiceDraft {
    lines = List.copyOf(lines);
  }

  /** One line of the invoice. */
  public record Line(
      String description,
      @Nullable String sku,
      @Nullable BigDecimal quantity,
      @Nullable String unit,
      @Nullable BigDecimal unitPrice,
      @Nullable BigDecimal lineTotal) {}

  /** A draft with nothing in it, for staff to type in next to the image. */
  public static InvoiceDraft empty() {
    return new InvoiceDraft(null, null, null, null, null, null, null, List.of());
  }
}
