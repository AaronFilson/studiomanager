package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Which reader reads the bound studio's invoices: the first, in the studio's order, that says it
 * can read for the studio. The order is the admin's for the studio, or Claude first and then the
 * platform's readers when the admin hasn't set one. Nobody falls back to anybody: if that reader
 * fails, the invoice is filled in by hand (docs/planning/20, "Invoice readers").
 *
 * <p>The order is decided here, and nowhere else.
 */
@Component
class InvoiceReaders {

  private final Map<InvoiceReaderKind, InvoiceReader> readers;
  private final PlatformReaderRules rules;

  InvoiceReaders(List<InvoiceReader> readers, PlatformReaderRules rules) {
    Map<InvoiceReaderKind, InvoiceReader> byKind = new EnumMap<>(InvoiceReaderKind.class);
    readers.forEach(reader -> byKind.put(reader.kind(), reader));
    this.readers = byKind;
    this.rules = rules;
  }

  /** The reader for the bound studio's next invoice, if any can read for it. */
  Optional<InvoiceReader> forBoundStudio() {
    return inOrder().stream().filter(InvoiceReader::canReadForBoundStudio).findFirst();
  }

  /** Every reader that can read for the bound studio now, in its order: for "Compare readers". */
  List<InvoiceReader> allForBoundStudio() {
    return inOrder().stream().filter(InvoiceReader::canReadForBoundStudio).toList();
  }

  private List<InvoiceReader> inOrder() {
    UUID studioId = TenantContext.require().requireStudioId();
    return rules.current().order(studioId).stream()
        .distinct()
        .flatMap(kind -> Optional.ofNullable(readers.get(kind)).stream())
        .toList();
  }
}
