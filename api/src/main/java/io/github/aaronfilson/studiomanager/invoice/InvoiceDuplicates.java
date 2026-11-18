package io.github.aaronfilson.studiomanager.invoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The same invoice uploaded again: same supplier and invoice number as one already confirmed.
 * Confirming it would receive its stock a second time, so the review says so and confirming
 * refuses. An invoice without a number (a handwritten counter form) is never a duplicate.
 */
@Component
class InvoiceDuplicates {

  private final InvoiceRepository invoices;

  InvoiceDuplicates(InvoiceRepository invoices) {
    this.invoices = invoices;
  }

  /** The confirmed invoice this one repeats, if any. Needs a transaction. */
  Optional<Invoice> confirmedCopyOf(Invoice invoice) {
    String number = invoice.invoiceNumber();
    if (number == null || number.isBlank()) {
      return Optional.empty();
    }
    String vendor = SupplyMatcher.normalize(invoice.vendorName());
    return invoices
        .findOthersNumbered(invoice.id(), number.strip(), InvoiceStatus.CONFIRMED)
        .stream()
        .filter(other -> SupplyMatcher.normalize(other.vendorName()).equals(vendor))
        .findFirst();
  }

  /** What staff are told about a repeat; says nothing for an invoice that isn't one. */
  Optional<String> problem(Invoice invoice) {
    return confirmedCopyOf(invoice).map(InvoiceDuplicates::message);
  }

  /** The checks' problems, then the repeat, if it is one. */
  List<String> problems(Invoice invoice) {
    List<String> problems = new ArrayList<>(InvoiceChecks.problems(invoice.draft()));
    problem(invoice).ifPresent(problems::add);
    return problems;
  }

  static String message(Invoice confirmed) {
    String vendor = confirmed.vendorName();
    return "Invoice %s%s is already confirmed and its stock received: confirming this copy would"
            .formatted(confirmed.invoiceNumber(), vendor == null ? "" : " from " + vendor)
        + " receive it twice. Discard this one.";
  }
}
