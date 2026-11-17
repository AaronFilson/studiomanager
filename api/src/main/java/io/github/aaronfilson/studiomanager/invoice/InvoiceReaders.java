package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.util.ArrayList;
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
public class InvoiceReaders {

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

  /**
   * Every reader, the studio's order first and then any it leaves out, for "Compare readers", which
   * shows a reader that can't read with the reason why rather than leaving it out.
   */
  List<InvoiceReader> everyForBoundStudio() {
    List<InvoiceReader> ordered = new ArrayList<>(inOrder());
    readers.values().stream().filter(reader -> !ordered.contains(reader)).forEach(ordered::add);
    return List.copyOf(ordered);
  }

  /** Whether each reader has what it needs on this server, for the admin page. */
  public Map<InvoiceReaderKind, Boolean> setUp() {
    Map<InvoiceReaderKind, Boolean> setUp = new EnumMap<>(InvoiceReaderKind.class);
    readers.forEach((kind, reader) -> setUp.put(kind, reader.setUp()));
    return setUp;
  }

  private List<InvoiceReader> inOrder() {
    UUID studioId = TenantContext.require().requireStudioId();
    return rules.current().order(studioId).stream()
        .distinct()
        .flatMap(kind -> Optional.ofNullable(readers.get(kind)).stream())
        .toList();
  }
}
