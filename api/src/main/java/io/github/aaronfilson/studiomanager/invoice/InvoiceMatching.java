package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.Candidate;
import io.github.aaronfilson.studiomanager.supplies.SupplyModel;
import io.github.aaronfilson.studiomanager.supplies.SupplyService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Loads what {@link SupplyMatcher} needs for one draft: the studio's supply items in one call to
 * the supplies module, and the vendor's aliases in one query. Call it before changing any entity in
 * the transaction (ADR 21, rule 5): both run SQL.
 */
@Component
class InvoiceMatching {

  private final SupplyService supplies;
  private final InvoiceQueries queries;

  InvoiceMatching(SupplyService supplies, InvoiceQueries queries) {
    this.supplies = supplies;
    this.queries = queries;
  }

  /** A match for each line, in order. */
  List<Invoice.LineMatch> matchAll(InvoiceDraft draft) {
    List<Candidate> items =
        supplies.items().stream().map(item -> new Candidate(item.id(), item.name())).toList();
    String vendorKey = SupplyMatcher.normalize(draft.vendorName());
    Map<AliasKey, UUID> aliases = vendorKey.isEmpty() ? Map.of() : queries.aliasesFor(vendorKey);
    return draft.lines().stream()
        .map(line ->
            SupplyMatcher.match(draft.vendorName(), line, aliases, items)
                .map(match -> new Invoice.LineMatch(match.itemId(), match.source()))
                .orElse(Invoice.LineMatch.none()))
        .toList();
  }

  /** The ids of the bound studio's current supply items, to check a choice staff made. */
  Set<UUID> itemIds() {
    return supplies.items().stream().map(SupplyModel.Item::id).collect(Collectors.toSet());
  }
}
