package io.github.aaronfilson.studiomanager.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;
import org.junit.jupiter.api.Test;

class AccountingExportTests {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    // A day with a $10 firing fee plus 8.25% tax, half of it paid by card
    private final List<SummaryLine> day = List.of(
            line(LedgerAccount.MEMBER_RECEIVABLE, null, "10.83", "5.00"),
            line(LedgerAccount.PROCESSOR_CLEARING, null, "5.00", "0"),
            line(LedgerAccount.REVENUE_FIRING, "FIRING_SERVICE", "0", "10.00"),
            line(LedgerAccount.SALES_TAX_PAYABLE, "FIRING_SERVICE", "0", "0.83"));

    @Test
    void writesOneBalancedJournalPerDay() {
        String csv = JournalExport.csv(day);

        assertThat(csv.split("\r\n")).containsExactly(
                JournalExport.HEADER,
                "SM-2026-10-20,2026-10-20,Member balances,5.83,,Studio Manager daily summary",
                "SM-2026-10-20,2026-10-20,Payments clearing,5.00,,Studio Manager daily summary",
                "SM-2026-10-20,2026-10-20,Revenue: firing (FIRING_SERVICE),,10.00,Studio Manager daily summary",
                "SM-2026-10-20,2026-10-20,Sales tax payable (FIRING_SERVICE),,0.83,Studio Manager daily summary");
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsADayToAQuickBooksJournalEntry() {
        Map<String, String> refs = Map.of(
                "Member balances", "84", "Payments clearing", "35",
                "Revenue: firing (FIRING_SERVICE)", "79", "Sales tax payable (FIRING_SERVICE)", "41");

        Map<String, Object> entry = QuickBooksJournal.entryFor(DAY, day, refs);

        assertThat(entry).containsEntry("TxnDate", "2026-10-20").containsEntry("DocNumber", "SM-2026-10-20");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) entry.get("Line");
        assertThat(lines).hasSize(4);
        assertThat(lines.getFirst()).containsEntry("Amount", new BigDecimal("5.83"));
        Map<String, Object> detail = (Map<String, Object>) lines.getFirst().get("JournalEntryLineDetail");
        assertThat(detail).containsEntry("PostingType", "Debit");
        assertThat((Map<String, Object>) detail.get("AccountRef")).containsEntry("value", "84");
        assertThat(lines.get(2).get("JournalEntryLineDetail")).asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.MAP).containsEntry("PostingType", "Credit");
    }

    @Test
    void refusesAnUnmappedAccountOrAnUnbalancedDay() {
        assertThatThrownBy(() -> QuickBooksJournal.entryFor(DAY, day, Map.of()))
                .hasMessageContaining("No QuickBooks account is mapped for 'Member balances'");
        List<SummaryLine> unbalanced = List.of(line(LedgerAccount.BANK, null, "1.00", "0"));
        assertThatThrownBy(() -> QuickBooksJournal.entryFor(DAY, unbalanced, Map.of("Bank", "1")))
                .hasMessageContaining("doesn't balance");
    }

    private static SummaryLine line(LedgerAccount account, String taxCategory, String debits, String credits) {
        return new SummaryLine(DAY, account, taxCategory, new BigDecimal(debits), new BigDecimal(credits));
    }

}
