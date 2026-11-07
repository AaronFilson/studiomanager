package io.github.aaronfilson.studiomanager.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An invoice as the API shows it.
 *
 * @param problems what doesn't add up, from {@link InvoiceChecks}
 */
record InvoiceView(
    UUID id,
    InvoiceStatus status,
    InvoiceFileType fileType,
    @Nullable InvoiceReaderKind reader,
    @Nullable String readNote,
    InvoiceDraft draft,
    List<Line> lines,
    List<String> problems,
    UUID uploadedBy,
    Instant uploadedAt,
    @Nullable UUID confirmedBy,
    @Nullable Instant confirmedAt,
    long version) {

  /** An invoice as the inbox lists it, without its lines. */
  record Summary(
      UUID id,
      InvoiceStatus status,
      InvoiceFileType fileType,
      @Nullable String vendorName,
      @Nullable String invoiceNumber,
      @Nullable LocalDate invoiceDate,
      @Nullable BigDecimal total,
      Instant uploadedAt) {}

  /** A line with the supply item it receives into, if any. */
  record Line(
      InvoiceDraft.Line line,
      @Nullable UUID supplyItemId,
      SupplyMatcher.@Nullable MatchSource matchSource) {}
}
