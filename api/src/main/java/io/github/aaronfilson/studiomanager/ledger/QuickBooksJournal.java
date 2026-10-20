package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;

/**
 * Turns one day's summary into the body of a QuickBooks Online {@code JournalEntry} create
 * request. Pure, so the mapping is tested without Intuit.
 *
 * <p>Why a daily journal entry rather than a SalesReceipt per sale: Intuit's free App Partner tier
 * meters reads (500,000 a month, pooled across every connected company) but not most writes, and
 * the most common complaint about existing connectors is per-transaction posting flooding the books.
 * One write per studio per day, keyed by {@code DocNumber} so a retry can't duplicate it, stays far
 * inside the budget. The HTTP side (OAuth connection, token refresh, posting) needs Intuit app
 * credentials and lives behind {@link AccountingSync}.
 */
public final class QuickBooksJournal {

    private QuickBooksJournal() {
    }

    /**
     * @param accountRefs the studio's QuickBooks account id for each export account name
     *     ({@link JournalExport#accountName}); every name in the day must be mapped
     */
    public static Map<String, Object> entryFor(LocalDate day, List<SummaryLine> lines, Map<String, String> accountRefs) {
        List<Map<String, Object>> qboLines = new ArrayList<>();
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (SummaryLine line : lines) {
            if (!line.date().equals(day)) {
                throw new IllegalArgumentException("Line for " + line.date() + " in the journal for " + day);
            }
            BigDecimal net = line.debits().subtract(line.credits());
            if (net.signum() == 0) {
                continue;
            }
            String name = JournalExport.accountName(line);
            String ref = accountRefs.get(name);
            if (ref == null) {
                throw new IllegalStateException("No QuickBooks account is mapped for '" + name + "'");
            }
            boolean debit = net.signum() > 0;
            BigDecimal amount = net.abs();
            if (debit) {
                debits = debits.add(amount);
            } else {
                credits = credits.add(amount);
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("PostingType", debit ? "Debit" : "Credit");
            detail.put("AccountRef", Map.of("value", ref, "name", name));
            Map<String, Object> qboLine = new LinkedHashMap<>();
            qboLine.put("DetailType", "JournalEntryLineDetail");
            qboLine.put("Amount", amount);
            qboLine.put("Description", name);
            qboLine.put("JournalEntryLineDetail", detail);
            qboLines.add(qboLine);
        }
        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException("The day doesn't balance: debits " + debits + ", credits " + credits);
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("TxnDate", day.toString());
        // QuickBooks keeps DocNumber unique per journal, so a repeated post of the same day is refused
        entry.put("DocNumber", "SM-" + day);
        entry.put("PrivateNote", "Studio Manager daily summary");
        entry.put("Line", qboLines);
        return entry;
    }

}
