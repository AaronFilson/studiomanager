package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.EntryKind;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.NecLine;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.StatementLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Payables' SQL: every statement here is a report or a total over the journal (ADR 21), summed in
 * one round trip with no entity to load. In a write transaction, flush first: these can't see
 * entity changes Hibernate hasn't written.
 */
@Component
class PayablesQueries {

  private final JdbcClient jdbc;

  PayablesQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * What the studio owes each of these payees now, from their {@code PAYEES_PAYABLE} lines. Payees
   * with no lines are left out: they're owed nothing.
   */
  Map<UUID, BigDecimal> owed(Collection<UUID> payeeIds) {
    Map<UUID, BigDecimal> owed = new HashMap<>();
    if (payeeIds.isEmpty()) {
      return owed;
    }
    jdbc.sql(
            """
            select payee_id, -sum(amount) as owed from journal_line
            where account = 'PAYEES_PAYABLE' and payee_id in (:payeeIds)
            group by payee_id
            """)
        .param("payeeIds", List.copyOf(payeeIds))
        .query(rs -> {
          owed.put(rs.getObject("payee_id", UUID.class), rs.getBigDecimal("owed"));
        });
    return owed;
  }

  /** What a payee was owed at the start of {@code from}. */
  BigDecimal owedBefore(UUID payeeId, LocalDate from) {
    return jdbc.sql(
            """
            select coalesce(-sum(l.amount), 0) from journal_line l join journal_entry e on e.id = l.entry_id
            where l.account = 'PAYEES_PAYABLE' and l.payee_id = :payee and e.occurred_on < :from
            """)
        .param("payee", payeeId)
        .param("from", from)
        .query(BigDecimal.class)
        .single();
  }

  /** Each entry that moved what a payee is owed over [{@code from}, {@code to}], in order. */
  List<StatementLine> statementLines(UUID payeeId, LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            select e.occurred_on, e.kind, e.memo, -sum(l.amount) as amount
            from journal_line l join journal_entry e on e.id = l.entry_id
            where l.account = 'PAYEES_PAYABLE' and l.payee_id = :payee and e.occurred_on between :from and :to
            group by e.id, e.occurred_on, e.kind, e.memo, e.created_at
            order by e.occurred_on, e.created_at
            """)
        .param("payee", payeeId)
        .param("from", from)
        .param("to", to)
        .query((rs, row) ->
            new StatementLine(
                rs.getObject("occurred_on", LocalDate.class),
                EntryKind.valueOf(rs.getString("kind")),
                rs.getString("memo"),
                rs.getBigDecimal("amount")))
        .list();
  }

  /**
   * Payments in [{@code start}, {@code end}) to each payee the studio files a 1099-NEC for, split
   * by whether the method is {@code reportable}.
   */
  List<NecLine> necLines(
      LocalDate start, LocalDate end, List<String> reportable, BigDecimal threshold) {
    return jdbc.sql(
            """
            select p.id, p.name, p.email, p.w9_received_on,
                   coalesce(sum(m.amount) filter (where m.method in (:reportable)), 0) as reportable,
                   coalesce(sum(m.amount) filter (where m.method not in (:reportable)), 0) as networks
            from payee p join payee_payment m on m.payee_id = p.id
            where p.files_1099 and m.paid_on >= :start and m.paid_on < :end
            group by p.id, p.name, p.email, p.w9_received_on
            order by p.name
            """)
        .param("reportable", reportable)
        .param("start", start)
        .param("end", end)
        .query((rs, row) -> {
          BigDecimal total = rs.getBigDecimal("reportable");
          return new NecLine(
              rs.getObject("id", UUID.class),
              rs.getString("name"),
              rs.getString("email"),
              rs.getObject("w9_received_on", LocalDate.class),
              total,
              rs.getBigDecimal("networks"),
              total.compareTo(threshold) >= 0);
        })
        .list();
  }
}
