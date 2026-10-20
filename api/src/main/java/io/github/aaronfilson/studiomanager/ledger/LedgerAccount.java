package io.github.aaronfilson.studiomanager.ledger;

/**
 * The studio's chart of accounts. Fixed, so every studio's books have the same shape and an
 * export maps cleanly onto QuickBooks or Xero accounts.
 */
public enum LedgerAccount {

    /** Money in the studio's bank account. */
    BANK(Type.ASSET),
    /** Money a processor (or the cash box) holds for the studio, not yet deposited. */
    PROCESSOR_CLEARING(Type.ASSET),
    /** What members owe, per member. */
    MEMBER_RECEIVABLE(Type.ASSET),
    /** Prepaid money held for people: gift cards, firing cards, class packs, deposits. */
    STORED_VALUE_LIABILITY(Type.LIABILITY),
    SALES_TAX_PAYABLE(Type.LIABILITY),
    REVENUE_FIRING(Type.REVENUE),
    REVENUE_CLASSES(Type.REVENUE),
    REVENUE_MEMBERSHIPS(Type.REVENUE),
    REVENUE_RETAIL(Type.REVENUE),
    REVENUE_EVENTS(Type.REVENUE),
    /** Stored value that expired unused. */
    REVENUE_BREAKAGE(Type.REVENUE),
    REVENUE_OTHER(Type.REVENUE),
    /** Contra-revenue: money given back. */
    REFUNDS(Type.REVENUE),
    PROCESSOR_FEES(Type.EXPENSE);

    private final Type type;

    LedgerAccount(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public enum Type {
        ASSET,
        LIABILITY,
        REVENUE,
        EXPENSE
    }

}
