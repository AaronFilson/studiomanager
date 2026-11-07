package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKind;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.MatchSource;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel.MovementKind;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff's side of the inbox: correct the draft next to the image, pick items, and confirm. Nothing
 * reaches stock until a person confirms (docs/planning/16, "Safety").
 */
@Service
class InvoiceReview {

  private final InvoiceRepository invoices;
  private final InvoiceMatching matching;
  private final InvoiceQueries queries;
  private final SupplyService supplies;
  private final Clock clock;

  InvoiceReview(
      InvoiceRepository invoices,
      InvoiceMatching matching,
      InvoiceQueries queries,
      SupplyService supplies,
      Clock clock) {
    this.invoices = invoices;
    this.matching = matching;
    this.queries = queries;
    this.supplies = supplies;
    this.clock = clock;
  }

  /** A line as staff left it: the line, and the item it receives into (null for none). */
  record ReviewedLine(InvoiceDraft.Line line, @Nullable UUID supplyItemId) {}

  /**
   * Replaces the draft with staff's version. A line keeps how it was matched when staff left its
   * item alone; an item staff picked is marked as theirs.
   */
  @Transactional
  InvoiceView edit(
      UUID studioId,
      UUID invoiceId,
      InvoiceDraft header,
      List<ReviewedLine> reviewed,
      long expectedVersion) {
    InvoiceInbox.requireBound(studioId);
    // SQL first, before the entity changes (ADR 21, rule 5)
    Set<UUID> known = matching.itemIds();
    Invoice invoice = findAtVersion(invoiceId, expectedVersion);
    InvoiceDrafts.Sanitized sanitized =
        InvoiceDrafts.sanitize(
            new InvoiceDraft(
                header.vendorName(),
                header.invoiceNumber(),
                header.invoiceDate(),
                header.subtotal(),
                header.shipping(),
                header.tax(),
                header.total(),
                reviewed.stream().map(ReviewedLine::line).toList()),
            LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    if (sanitized.draft().lines().size() != reviewed.size()) {
      throw new InvalidInputException("Every line needs a description.");
    }
    List<InvoiceLine> before = invoice.lines();
    List<Invoice.LineMatch> matches = new ArrayList<>();
    for (int index = 0; index < reviewed.size(); index++) {
      UUID itemId = reviewed.get(index).supplyItemId();
      if (itemId == null) {
        matches.add(Invoice.LineMatch.none());
        continue;
      }
      if (!known.contains(itemId)) {
        throw new InvalidInputException(
            "Line %d's item isn't one of the studio's supplies.".formatted(index + 1));
      }
      MatchSource kept =
          index < before.size() && itemId.equals(before.get(index).supplyItemId())
              ? before.get(index).matchSource()
              : null;
      matches.add(new Invoice.LineMatch(itemId, kept == null ? MatchSource.STAFF : kept));
    }
    invoice.edited(sanitized.draft(), matches);
    invoices.flush();
    return invoice.toView(InvoiceChecks.problems(invoice.draft()));
  }

  /**
   * Receives each matched line's quantity into stock, remembers what the vendor's lines meant for
   * next time, and marks the invoice confirmed. All in one transaction.
   */
  @Transactional
  InvoiceView confirm(UUID studioId, UUID invoiceId, long expectedVersion) {
    InvoiceInbox.requireBound(studioId);
    TenantContext caller = TenantContext.require();
    Invoice invoice = findAtVersion(invoiceId, expectedVersion);
    List<InvoiceLine> lines = invoice.lines();
    for (int index = 0; index < lines.size(); index++) {
      InvoiceLine line = lines.get(index);
      BigDecimal quantity = line.quantity();
      if (line.supplyItemId() != null && (quantity == null || quantity.signum() <= 0)) {
        throw new InvalidInputException(
            "Line %d needs a quantity above zero to receive.".formatted(index + 1));
      }
    }
    Instant now = clock.instant();
    invoice.confirmed(caller.accountId(), now);
    // Supplies records the stock with SQL in this transaction, so write the invoice first
    invoices.flush();
    String note = receivedNote(invoice);
    String vendorKey = SupplyMatcher.normalize(invoice.vendorName());
    for (InvoiceLine line : lines) {
      UUID itemId = line.supplyItemId();
      if (itemId == null) {
        continue;
      }
      supplies.recordMovement(
          itemId, MovementKind.RECEIVED, Objects.requireNonNull(line.quantity()), note);
      if (!vendorKey.isEmpty()) {
        learn(studioId, vendorKey, AliasKind.SKU, line.sku(), itemId, caller.accountId(), now);
        learn(
            studioId,
            vendorKey,
            AliasKind.DESCRIPTION,
            line.description(),
            itemId,
            caller.accountId(),
            now);
      }
    }
    return invoice.toView(InvoiceChecks.problems(invoice.draft()));
  }

  private void learn(
      UUID studioId,
      String vendorKey,
      AliasKind kind,
      @Nullable String text,
      UUID itemId,
      UUID accountId,
      Instant at) {
    String lineKey = SupplyMatcher.normalize(text);
    if (!lineKey.isEmpty()) {
      queries.learnAlias(studioId, new AliasKey(vendorKey, kind, lineKey), itemId, accountId, at);
    }
  }

  private static String receivedNote(Invoice invoice) {
    String vendor = invoice.vendorName();
    return vendor == null ? "Received from an invoice" : "Received from an invoice from " + vendor;
  }

  private Invoice findAtVersion(UUID invoiceId, long expectedVersion) {
    Invoice invoice =
        invoices
            .findWithLines(invoiceId)
            .orElseThrow(() -> new NotFoundException("No such invoice."));
    if (invoice.version() != expectedVersion) {
      throw ConflictException.staleVersion(
          "Someone else changed this invoice at the same time. Reload and try again.");
    }
    return invoice;
  }
}
