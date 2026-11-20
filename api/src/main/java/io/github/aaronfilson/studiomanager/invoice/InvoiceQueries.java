package io.github.aaronfilson.studiomanager.invoice;

import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKey;
import io.github.aaronfilson.studiomanager.invoice.SupplyMatcher.AliasKind;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The invoice module's SQL, each statement with its reason (ADR 21, rule 4). Vendor aliases and
 * compared drafts have no JPA entity: they are upserted by key or summarized across tenants via
 * {@code SECURITY DEFINER} functions.
 */
@Component
class InvoiceQueries {

  private final JdbcClient jdbc;

  InvoiceQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** One stored compared draft awaiting (or having) a score against staff's confirmation. */
  record StoredComparison(UUID id, InvoiceReaderKind reader, InvoiceDraft draft) {}

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
   * Cross-tenant accuracy summary for each compared reader on confirmed invoices, through {@code
   * app.reader_accuracy_summary}, which refuses anyone not bound as an admin (V45).
   */
  List<PlatformReaderUsage.ReaderAccuracy> readerAccuracySummary() {
    return jdbc.sql(
            "select reader, confirmed_count, exact_matches, matched_fields, total_fields,"
                + " matched_lines, total_lines from app.reader_accuracy_summary()")
        .query((rs, row) ->
            new PlatformReaderUsage.ReaderAccuracy(
                InvoiceReaderKind.valueOf(rs.getString("reader")),
                rs.getLong("confirmed_count"),
                rs.getLong("exact_matches"),
                rs.getLong("matched_fields"),
                rs.getLong("total_fields"),
                rs.getLong("matched_lines"),
                rs.getLong("total_lines")))
        .list();
  }

  /**
   * Keeps one reader's compared draft so it can be scored when staff confirm the invoice. Replaces
   * an earlier comparison for the same invoice and reader.
   */
  UUID saveComparison(
      UUID studioId,
      UUID invoiceId,
      InvoiceReaderKind reader,
      @Nullable String model,
      InvoiceDraft draft,
      long elapsedMs,
      @Nullable BigDecimal costUsd,
      Instant comparedAt) {
    UUID id = Uuids.v7();
    return Objects.requireNonNull(
        jdbc.sql(
                """
                insert into invoice_comparison
                    (id, studio_id, invoice_id, reader, model, draft, elapsed_ms, cost_usd, compared_at)
                values (:id, :studio, :invoice, :reader, :model, cast(:draft as jsonb), :elapsed, :cost, :at)
                on conflict (studio_id, invoice_id, reader) do update
                    set model = excluded.model,
                        draft = excluded.draft,
                        elapsed_ms = excluded.elapsed_ms,
                        cost_usd = excluded.cost_usd,
                        compared_at = excluded.compared_at,
                        scored_at = null,
                        matched_fields = null,
                        total_fields = null,
                        matched_lines = null,
                        total_lines = null,
                        exact_match = null,
                        line_accuracy = null,
                        false_alarms = null,
                        missed = null
                returning id
                """)
            .param("id", id)
            .param("studio", studioId)
            .param("invoice", invoiceId)
            .param("reader", reader.name())
            .param("model", model, Types.VARCHAR)
            .param("draft", InvoiceJson.writeDraft(draft))
            .param("elapsed", elapsedMs)
            .param("cost", costUsd, Types.NUMERIC)
            .param("at", comparedAt.atOffset(ZoneOffset.UTC))
            .query(UUID.class)
            .single());
  }

  /** Every stored compared draft for one invoice in the bound studio. */
  List<StoredComparison> comparisonsFor(UUID invoiceId) {
    return jdbc
        .sql("select id, reader, draft from invoice_comparison where invoice_id = :invoice")
        .param("invoice", invoiceId)
        .query((rs, row) ->
            new StoredComparison(
                rs.getObject("id", UUID.class),
                InvoiceReaderKind.valueOf(rs.getString("reader")),
                InvoiceJson.readDraft(rs.getString("draft"))))
        .list();
  }

  /** Persists how one compared draft scored against staff's confirmed invoice. */
  void saveComparisonScore(
      UUID comparisonId, InvoiceScorer.Score score, InvoiceScorer.Checked checked, Instant at) {
    BigDecimal lineAccuracy =
        BigDecimal.valueOf(score.lineAccuracy()).setScale(4, RoundingMode.HALF_UP);
    jdbc.sql(
            """
            update invoice_comparison
            set scored_at = :at,
                matched_fields = :matchedFields,
                total_fields = :totalFields,
                matched_lines = :matchedLines,
                total_lines = :totalLines,
                exact_match = :exactMatch,
                line_accuracy = :lineAccuracy,
                false_alarms = :falseAlarms,
                missed = :missed
            where id = :id
            """)
        .param("at", at.atOffset(ZoneOffset.UTC))
        .param("matchedFields", score.fieldsRight())
        .param("totalFields", score.fieldsTotal())
        .param("matchedLines", score.linesRight())
        .param("totalLines", score.linesTotal())
        .param("exactMatch", score.needsNoEdits())
        .param("lineAccuracy", lineAccuracy)
        .param("falseAlarms", checked.falseAlarms())
        .param("missed", checked.missed())
        .param("id", comparisonId)
        .update();
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
