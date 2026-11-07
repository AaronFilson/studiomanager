package io.github.aaronfilson.studiomanager.invoice;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Which reader reads the bound studio's invoices. Today: the first reader, in {@link
 * InvoiceReaderKind} order, that says it can read for the studio, so Claude when the studio has a
 * working key. Nobody falls back to anybody: if that reader fails, the invoice is filled in by hand
 * (docs/planning/20, "Invoice readers").
 *
 * <p>Lane F1g adds the platform's readers and the admin's per-studio order; the order is decided
 * here, and nowhere else.
 */
@Component
class InvoiceReaders {

  private final List<InvoiceReader> readers;

  InvoiceReaders(List<InvoiceReader> readers) {
    this.readers = readers.stream().sorted(Comparator.comparing(InvoiceReader::kind)).toList();
  }

  /** The reader for the bound studio's next invoice, if any can read for it. */
  Optional<InvoiceReader> forBoundStudio() {
    return readers.stream().filter(InvoiceReader::canReadForBoundStudio).findFirst();
  }
}
