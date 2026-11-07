package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.ClearingBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.MemberBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.Provider;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueBalance;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.StoredValueKind;
import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The ledger's SQL, each statement with its reason (ADR 21). Postings are entities; balances,
 * statements and the export are sums over journal lines, in one round trip with no entity to load.
 * In a write transaction, flush first: these can't see entity changes Hibernate hasn't written.
 */
@Component
class LedgerQueries {

  private final JdbcClient jdbc;

  LedgerQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Report: everyone who owes something or is in credit, largest first. Names not filled in. */
  List<MemberBalance> memberBalances() {
    return jdbc.sql(
            """
            select member_id, sum(amount) as owed from journal_line
            where account = 'MEMBER_RECEIVABLE' group by member_id having sum(amount) <> 0
            order by sum(amount) desc
            """)
        .query((rs, row) ->
            new MemberBalance(rs.getObject("member_id", UUID.class), null, rs.getBigDecimal("owed")))
        .list();
  }

  /** Total: what one member owes now. */
  BigDecimal owedBy(UUID memberId) {
    return jdbc.sql(
            """
            select coalesce(sum(amount), 0) from journal_line where account = 'MEMBER_RECEIVABLE' and member_id = :id
            """)
        .param("id", memberId)
        .query(BigDecimal.class)
        .single();
  }

  /** Balances: one stored value with what's left on it. Holder's name not filled in. */
  List<StoredValueBalance> storedValueById(UUID storedValueId) {
    return storedValues("where s.id = :id", Map.of("id", storedValueId), 1);
  }

  /** Balances: a member's stored value, newest first, at most {@code fetchSize}. */
  List<StoredValueBalance> storedValuesHeldBy(UUID holder, int fetchSize) {
    return storedValues("where s.holder = :holder", Map.of("holder", holder), fetchSize);
  }

  /** Balances: the gift card with this code, if any. */
  List<StoredValueBalance> giftCardByCode(String code) {
    return storedValues("where s.code = :code", Map.of("code", code), 1);
  }

  /** Balances: every stored value with something left, newest first, at most {@code fetchSize}. */
  List<StoredValueBalance> storedValuesWithSomethingLeft(int fetchSize) {
    return storedValues("where coalesce(used.total, 0) <> 0", Map.of(), fetchSize);
  }

  /**
   * Report: the stored-value liability by kind, split into what was there before {@code from} and,
   * from then to {@code to}, by entry kind. Amounts are credits as positive.
   */
  List<StoredValueMovementRow> storedValueMovements(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            select s.kind, e.kind as entry_kind, e.occurred_on < :from as before, -sum(l.amount) as amount
            from journal_line l
            join journal_entry e on e.id = l.entry_id
            join stored_value s on s.id = l.stored_value
            where l.account = 'STORED_VALUE_LIABILITY' and e.occurred_on <= :to
            -- By position: the two :from placeholders are separate parameters to Postgres
            group by 1, 2, 3
            """)
        .param("from", from)
        .param("to", to)
        .query((rs, row) ->
            new StoredValueMovementRow(
                StoredValueKind.valueOf(rs.getString("kind")),
                EntryKind.valueOf(rs.getString("entry_kind")),
                rs.getBoolean("before"),
                rs.getBigDecimal("amount")))
        .list();
  }

  /** Report: money each processor has taken for the studio but not yet paid out. */
  List<ClearingBalance> clearing() {
    return jdbc.sql(
            """
            select provider, sum(amount) as awaiting from journal_line
            where account = 'PROCESSOR_CLEARING' group by provider having sum(amount) <> 0 order by provider
            """)
        .query((rs, row) ->
            new ClearingBalance(
                Provider.valueOf(rs.getString("provider")), rs.getBigDecimal("awaiting")))
        .list();
  }

  /** Report: totals per day and account (and tax category) over a range, for the export. */
  List<SummaryLine> dailySummary(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            select e.occurred_on, l.account, l.tax_category,
                   coalesce(sum(l.amount) filter (where l.amount > 0), 0) as debits,
                   coalesce(-sum(l.amount) filter (where l.amount < 0), 0) as credits
            from journal_line l join journal_entry e on e.id = l.entry_id
            where e.occurred_on between :from and :to
            group by e.occurred_on, l.account, l.tax_category
            order by e.occurred_on, l.account, l.tax_category nulls first
            """)
        .param("from", from)
        .param("to", to)
        .query((rs, row) ->
            new SummaryLine(
                rs.getObject("occurred_on", LocalDate.class),
                LedgerAccount.valueOf(rs.getString("account")),
                rs.getString("tax_category"),
                rs.getBigDecimal("debits"),
                rs.getBigDecimal("credits")))
        .list();
  }

  /**
   * Upsert: sets a category's rate from a date, replacing the rate already set for that date.
   * Race-free in one statement.
   */
  void setTaxRate(UUID studioId, String taxCategory, BigDecimal rate, LocalDate effectiveFrom) {
    jdbc.sql(
            """
            insert into tax_rate (id, studio_id, tax_category, rate, effective_from)
            values (:id, :studioId, :category, :rate, :from)
            on conflict (studio_id, tax_category, effective_from) do update set rate = excluded.rate
            """)
        .param("id", Uuids.v7())
        .param("studioId", studioId)
        .param("category", taxCategory)
        .param("rate", rate)
        .param("from", effectiveFrom)
        .update();
  }

  /**
   * Balances, and Postgres-only: each stored value with what's left, summed by a {@code lateral}
   * join. {@code where} is one of the fixed clauses above, never caller text.
   */
  private List<StoredValueBalance> storedValues(
      String where, Map<String, Object> params, int fetchSize) {
    var spec =
        jdbc.sql(
                """
                select s.id, s.kind, s.holder, s.code, s.label, s.expires_on,
                       coalesce(-used.total, 0) as remaining
                from stored_value s
                left join lateral (select sum(l.amount) as total from journal_line l
                                   where l.stored_value = s.id) used on true
                %s order by s.created_at desc limit :fetchSize
                """
                    .formatted(where))
            .param("fetchSize", fetchSize);
    for (var param : params.entrySet()) {
      spec = spec.param(param.getKey(), param.getValue());
    }
    return spec.query((rs, row) -> storedValue(rs)).list();
  }

  private static StoredValueBalance storedValue(ResultSet rs) throws SQLException {
    return new StoredValueBalance(
        rs.getObject("id", UUID.class),
        StoredValueKind.valueOf(rs.getString("kind")),
        rs.getObject("holder", UUID.class),
        null,
        rs.getString("code"),
        rs.getString("label"),
        rs.getObject("expires_on", LocalDate.class),
        rs.getBigDecimal("remaining"));
  }

  /** One group of {@link #storedValueMovements}. */
  record StoredValueMovementRow(
      StoredValueKind kind, EntryKind entryKind, boolean beforeRange, BigDecimal amount) {}
}
