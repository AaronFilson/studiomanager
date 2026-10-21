package io.github.aaronfilson.studiomanager.ledger;

import java.time.LocalDate;
import java.util.List;

import io.github.aaronfilson.studiomanager.ledger.LedgerModel.SummaryLine;

/**
 * Posts a day's summary to an external accounting system. QuickBooks Online first (the only
 * ledger pottery sources named), Xero later for Australia and New Zealand.
 *
 * <p>Implementations must be write-mostly and idempotent per day: read only to set up account
 * mappings, never poll, and let a retried post be refused rather than duplicated.
 */
public interface AccountingSync {

    /** The system's name, for logs and the settings page. */
    String name();

    void postDay(LocalDate day, List<SummaryLine> lines);

}
