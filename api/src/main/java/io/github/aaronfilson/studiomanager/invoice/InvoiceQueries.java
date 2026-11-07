package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKind;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The invoice module's SQL, each statement with its reason (ADR 21, rule 4). Vendor aliases have no
 * entity: they're only ever upserted and looked up by key.
 */
@Component
class InvoiceQueries {

  private final JdbcClient jdbc;

  InvoiceQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Every alias the bound studio has learned for one vendor (row-level security adds the studio).
   */
  Map<AliasKey, UUID> aliasesFor(String vendorKey) {
    // A plain lookup by key on a table with no entity; it runs before the job changes any entity
    return jdbc
        .sql("select kind, line_key, supply_item_id from vendor_alias where vendor_key = :vendor")
        .param("vendor", vendorKey)
        .query((rs, row) ->
            Map.entry(
                new AliasKey(
                    vendorKey, AliasKind.valueOf(rs.getString("kind")), rs.getString("line_key")),
                rs.getObject("supply_item_id", UUID.class)))
        .list()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Pages each platform-paid reader has read since a moment, for every studio together. Crosses
   * tenants (ADR 10), so it goes through {@code app.platform_reader_pages}, which returns only
   * totals (V38).
   */
  Map<InvoiceReaderKind, Long> platformPagesSince(Instant since) {
    return jdbc
        .sql("select reader, pages from app.platform_reader_pages(:since)")
        .param("since", since.atOffset(ZoneOffset.UTC))
        .query((rs, row) ->
            Map.entry(InvoiceReaderKind.valueOf(rs.getString("reader")), rs.getLong("pages")))
        .list()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Each studio's use of each platform-paid reader since a moment, for the admin page, through
   * {@code app.platform_reader_usage_by_studio}, which refuses anyone not bound as an admin (V38).
   */
  List<PlatformReaderUsage.StudioUse> usageByStudioSince(Instant since) {
    return jdbc.sql(
            "select studio_id, reader, reads, pages, cost_usd"
                + " from app.platform_reader_usage_by_studio(:since)")
        .param("since", since.atOffset(ZoneOffset.UTC))
        .query((rs, row) ->
            new PlatformReaderUsage.StudioUse(
                rs.getObject("studio_id", UUID.class),
                InvoiceReaderKind.valueOf(rs.getString("reader")),
                rs.getLong("reads"),
                rs.getLong("pages"),
                rs.getBigDecimal("cost_usd")))
        .list();
  }

  /**
   * Remembers that this vendor's line means this item, replacing what was remembered before.
   * Counters and upserts (ADR 21): {@code on conflict} is race-free when two invoices from the same
   * vendor are confirmed at once.
   */
  void learnAlias(UUID studioId, AliasKey key, UUID supplyItemId, UUID accountId, Instant at) {
    jdbc.sql(
            """
            insert into vendor_alias (id, studio_id, vendor_key, kind, line_key, supply_item_id, learned_by, learned_at)
            values (:id, :studio, :vendor, :kind, :line, :item, :by, :at)
            on conflict (studio_id, vendor_key, kind, line_key) do update
                set supply_item_id = excluded.supply_item_id, learned_by = excluded.learned_by,
                    learned_at = excluded.learned_at
            """)
        .param("id", Uuids.v7())
        .param("studio", studioId)
        .param("vendor", key.vendorKey())
        .param("kind", key.kind().name())
        .param("line", key.lineKey())
        .param("item", supplyItemId)
        .param("by", accountId)
        .param("at", at.atOffset(ZoneOffset.UTC))
        .update();
  }
}
