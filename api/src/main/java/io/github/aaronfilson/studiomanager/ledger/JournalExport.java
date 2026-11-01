package io.github.aaronfilson.studiomanager.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;

/**
 * The daily summary as a journal CSV: one balanced journal per day, one row per account. Both
 * QuickBooks Online and Xero import journals in this shape; the account names are mapped once on
 * the first import. It is the ledger-neutral path that works before (or instead of) a live sync.
 */
public final class JournalExport {

    static final String HEADER = "Journal No.,Journal Date,Account Name,Debits,Credits,Description";

    private JournalExport() {
    }

    public static String csv(List<SummaryLine> summary) {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (Map.Entry<LocalDate, List<SummaryLine>> day : byDay(summary).entrySet()) {
            String journal = "SM-" + day.getKey();
            for (SummaryLine line : day.getValue()) {
                BigDecimal net = line.debits().subtract(line.credits());
                if (net.signum() == 0) {
                    continue;
                }
                csv.append(journal).append(',')
                        .append(day.getKey()).append(',')
                        .append(field(accountName(line))).append(',')
                        .append(net.signum() > 0 ? net.toPlainString() : "").append(',')
                        .append(net.signum() < 0 ? net.negate().toPlainString() : "").append(',')
                        .append(field("Studio Manager daily summary")).append("\r\n");
            }
        }
        return csv.toString();
    }

    /** "Revenue: firing (FIRING_SERVICE)": the account, split by tax category where there is one. */
    static String accountName(SummaryLine line) {
        String name = switch (line.account()) {
            case BANK -> "Bank";
            case PROCESSOR_CLEARING -> "Payments clearing";
            case MEMBER_RECEIVABLE -> "Member balances";
            case STORED_VALUE_LIABILITY -> "Gift cards and prepaid balances";
            case SALES_TAX_PAYABLE -> "Sales tax payable";
            case PAYEES_PAYABLE -> "Owed to artists and instructors";
            case REVENUE_FIRING -> "Revenue: firing";
            case REVENUE_CLASSES -> "Revenue: classes";
            case REVENUE_MEMBERSHIPS -> "Revenue: memberships";
            case REVENUE_RETAIL -> "Revenue: retail";
            case REVENUE_EVENTS -> "Revenue: events";
            case REVENUE_BREAKAGE -> "Revenue: unused balances";
            case REVENUE_COMMISSIONS -> "Revenue: consignment commissions";
            case REVENUE_OTHER -> "Revenue: other";
            case REFUNDS -> "Refunds";
            case PROCESSOR_FEES -> "Payment processing fees";
            case CONTRACT_SERVICES -> "Contract instructors and services";
            case OPENING_BALANCE_EQUITY -> "Opening balance equity";
        };
        return line.taxCategory() == null ? name : name + " (" + line.taxCategory() + ")";
    }

    static Map<LocalDate, List<SummaryLine>> byDay(List<SummaryLine> summary) {
        Map<LocalDate, List<SummaryLine>> days = new LinkedHashMap<>();
        for (SummaryLine line : summary) {
            days.computeIfAbsent(line.date(), d -> new java.util.ArrayList<>()).add(line);
        }
        return days;
    }

    private static String field(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

}
