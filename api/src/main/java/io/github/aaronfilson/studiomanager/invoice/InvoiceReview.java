package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKind;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.MatchSource;
import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.supplies.PurchaseRequests;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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
  private final PurchaseRequests requests;
  private final InvoiceDuplicates duplicates;
  private final EntityManager entityManager;
  private final Clock clock;

  InvoiceReview(
      InvoiceRepository invoices,
      InvoiceMatching matching,
      InvoiceQueries queries,
      SupplyService supplies,
      PurchaseRequests requests,
      InvoiceDuplicates duplicates,
      EntityManager entityManager,
      Clock clock) {
    this.invoices = invoices;
    this.matching = matching;
    this.queries = queries;
    this.supplies = supplies;
    this.requests = requests;
    this.duplicates = duplicates;
    this.entityManager = entityManager;
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
    Integer rotation = header.rotation();
    if (rotation != null) {
      int degrees = rotation;
      if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
        throw new InvalidInputException("Rotation must be 0, 90, 180, or 270 degrees.");
      }
    }
    InvoiceDrafts.Sanitized sanitized =
        InvoiceDrafts.sanitize(
            new InvoiceDraft(
                header.documentType(),
                header.vendorName(),
                header.invoiceNumber(),
                header.orderNumber(),
                header.invoiceDate(),
                header.invoiceDatePrinted(),
                header.invoiceDateYear(),
                header.dueDate(),
                header.subtotal(),
                header.shipping(),
                header.tax(),
                header.total(),
                header.amountPaid(),
                header.amountDue(),
                header.payments(),
                header.crossedOut(),
                header.otherPapers(),
                header.notes(),
                rotation,
                reviewed.stream().map(ReviewedLine::line).toList()),
            LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    if (sanitized.draft().lines().size() != reviewed.size()) {
      throw new InvalidInputException("Every line needs a description.");
    }
    if (!sanitized.notes().isEmpty()) {
      throw new InvalidInputException(
          "Check the invoice values: " + String.join(" ", sanitized.notes()));
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
    long versionBefore = invoice.version();
    invoice.edited(sanitized.draft(), matches);
    invoices.flush();
    if (invoice.version() == versionBefore) {
      // Line-only edits don't dirty any column on invoice itself; force a synchronous version
      // increment so invoice.version() matches the committed row and stale callers receive 409.
      entityManager.lock(invoice, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }
    return invoice.toView(duplicates.problems(invoice));
  }

  /**
   * Receives each matched line's quantity into stock, records the last unit purchase price on each
   * matched item, settles each item's request already on its way, remembers what the vendor's lines
   * meant for next time, scores any stored Compare Readers drafts against the confirmed invoice,
   * and marks the invoice confirmed. All in one transaction. A repeat of a confirmed invoice is
   * refused.
   */
  @Transactional
  InvoiceView confirm(UUID studioId, UUID invoiceId, long expectedVersion) {
    InvoiceInbox.requireBound(studioId);
    TenantContext caller = TenantContext.require();
    Invoice invoice = findAtVersion(invoiceId, expectedVersion);
    duplicates
        .confirmedCopyOf(invoice)
        .ifPresent(confirmed -> {
          throw new ConflictException(InvoiceDuplicates.message(confirmed));
        });
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
    LocalDate invoiceDate = invoice.invoiceDate();
    Instant purchasedAt =
        invoiceDate != null ? invoiceDate.atStartOfDay(ZoneOffset.UTC).toInstant() : now;
    invoice.confirmed(caller.accountId(), now);
    // Supplies records the stock with SQL in this transaction, so write the invoice first
    invoices.flush();
    String note = receivedNote(invoice);
    String vendorKey = SupplyMatcher.normalize(invoice.vendorName());
    // Group matched lines by supply item in deterministic UUID order to prevent AB-BA deadlocks
    // when two invoices touching the same supply items are confirmed concurrently.
    Map<UUID, List<InvoiceLine>> byItem = new TreeMap<>();
    for (InvoiceLine line : lines) {
      UUID itemId = line.supplyItemId();
      if (itemId != null) {
        byItem.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(line);
      }
    }
    for (Map.Entry<UUID, List<InvoiceLine>> entry : byItem.entrySet()) {
      UUID itemId = entry.getKey();
      List<InvoiceLine> itemLines = entry.getValue();
      BigDecimal totalQuantity = BigDecimal.ZERO;
      for (InvoiceLine line : itemLines) {
        totalQuantity = totalQuantity.add(Objects.requireNonNull(line.quantity()));
      }
      BigDecimal unitPrice = weightedPurchaseUnitPrice(itemLines);
      supplies.receiveFromInvoice(itemId, totalQuantity, note, unitPrice, purchasedAt);
      // The delivery an open request was waiting for: received here, so not again by hand
      requests.settleOnWay(itemId);
      if (!vendorKey.isEmpty()) {
        for (InvoiceLine line : itemLines) {
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
    }
    InvoiceDraft confirmedDraft = invoice.draft();
    for (InvoiceQueries.StoredComparison compared : queries.comparisonsFor(invoiceId)) {
      InvoiceScorer.Score score = InvoiceScorer.score(confirmedDraft, compared.draft());
      InvoiceScorer.Checked checked = InvoiceScorer.checks(confirmedDraft, compared.draft(), score);
      queries.saveComparisonScore(compared.id(), score, checked, now);
    }
    return invoice.toView(InvoiceChecks.problems(confirmedDraft));
  }

  /**
   * Computes the quantity-weighted average unit purchase price across positive-priced lines for one
   * supply item. Zero-priced promo/bonus lines are ignored when any positive-priced line exists;
   * {@code 0.0000} is recorded only when every line for the item is explicitly {@code $0.00}.
   */
  private static @Nullable BigDecimal weightedPurchaseUnitPrice(List<InvoiceLine> itemLines) {
    BigDecimal positiveCost = BigDecimal.ZERO;
    BigDecimal positiveQuantity = BigDecimal.ZERO;
    boolean allExplicitZero = !itemLines.isEmpty();
    for (InvoiceLine line : itemLines) {
      BigDecimal quantity = Objects.requireNonNull(line.quantity());
      BigDecimal unitPrice = line.unitPrice();
      BigDecimal lineTotal = line.lineTotal();
      if (unitPrice != null) {
        if (unitPrice.signum() > 0) {
          positiveCost = positiveCost.add(unitPrice.multiply(quantity));
          positiveQuantity = positiveQuantity.add(quantity);
          allExplicitZero = false;
        } else if (unitPrice.signum() < 0) {
          allExplicitZero = false;
        }
      } else if (lineTotal != null) {
        if (lineTotal.signum() > 0) {
          positiveCost = positiveCost.add(lineTotal);
          positiveQuantity = positiveQuantity.add(quantity);
          allExplicitZero = false;
        } else if (lineTotal.signum() < 0) {
          allExplicitZero = false;
        }
      } else {
        allExplicitZero = false;
      }
    }
    if (positiveQuantity.signum() > 0) {
      return positiveCost.divide(positiveQuantity, 4, RoundingMode.HALF_UP);
    }
    if (allExplicitZero) {
      return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
    return null;
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
