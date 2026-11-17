package io.github.aaronfilson.studiomanager.invoice;

import java.util.UUID;
import org.slf4j.Logger;

/**
 * Logging a reader's failure, the same way from the reading job and from "Compare readers": the
 * reader, the invoice, the message staff saw, and the detail and cause, which say why the provider
 * failed without anything the invoice says (docs/planning/24, "The demo this week").
 */
final class InvoiceFailures {

  /** What staff see when a reader fails in a way nobody planned for. */
  static final String UNEXPECTED =
      "The reader failed unexpectedly. Fill this one in by hand, or upload it again later.";

  private InvoiceFailures() {}

  static void log(Logger log, InvoiceReaderKind reader, UUID invoiceId, InvoiceReadException e) {
    log.warn(
        "{} couldn't read invoice {}: {} [{}] ({})",
        reader,
        invoiceId,
        e.failure(),
        e.detail(),
        e.getMessage(),
        e.getCause());
  }

  static void logUnexpected(Logger log, InvoiceReaderKind reader, UUID invoiceId, Exception e) {
    log.error("{} failed unexpectedly reading invoice {}", reader, invoiceId, e);
  }
}
