package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.Size;

/**
 * Property-based tests with shrinking (Plan 15) verifying double-entry invariants across journal
 * exports and QuickBooks journal mappings, including supplier bills ({@code SUPPLIES_EXPENSE} /
 * {@code ACCOUNTS_PAYABLE}).
 */
class LedgerJournalPropertyTests {

  private static final LocalDate DAY = LocalDate.of(2026, 11, 21);

  @Property
  void balancedDailySummariesAlwaysProduceBalancedQuickBooksAndCsvJournals(
      @ForAll @Size(min = 1, max = 10)
          List<@BigRange(min = "0.01", max = "9999.99") BigDecimal> rawAmounts,
      @ForAll LedgerAccount debitAccount,
      @ForAll LedgerAccount creditAccount) {
    BigDecimal total =
        rawAmounts.stream()
            .map(a -> a.setScale(2, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<SummaryLine> lines =
        debitAccount == creditAccount
            ? List.of(
                new SummaryLine(DAY, LedgerAccount.SUPPLIES_EXPENSE, null, total, BigDecimal.ZERO),
                new SummaryLine(DAY, LedgerAccount.ACCOUNTS_PAYABLE, null, BigDecimal.ZERO, total))
            : List.of(
                new SummaryLine(DAY, debitAccount, null, total, BigDecimal.ZERO),
                new SummaryLine(DAY, creditAccount, null, BigDecimal.ZERO, total));

    Map<String, String> refs =
        lines.stream()
            .map(JournalExport::accountName)
            .distinct()
            .collect(Collectors.toMap(name -> name, name -> "qbo-" + Math.abs(name.hashCode())));

    Map<String, Object> qbo = QuickBooksJournal.entryFor(DAY, lines, refs);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> qboLines = (List<Map<String, Object>>) qbo.get("Line");
    BigDecimal debits = BigDecimal.ZERO;
    BigDecimal credits = BigDecimal.ZERO;
    for (Map<String, Object> line : qboLines) {
      BigDecimal amount = (BigDecimal) line.get("Amount");
      @SuppressWarnings("unchecked")
      Map<String, Object> detail = (Map<String, Object>) line.get("JournalEntryLineDetail");
      if ("Debit".equals(detail.get("PostingType"))) {
        debits = debits.add(amount);
      } else {
        credits = credits.add(amount);
      }
    }
    assertThat(debits).isEqualByComparingTo(credits);
    assertThat(debits).isEqualByComparingTo(total);

    String csv = JournalExport.csv(lines);
    assertThat(csv).startsWith(JournalExport.HEADER).contains(total.toPlainString());
  }
}
