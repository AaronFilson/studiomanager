-- The CSV importer (docs/planning/08-wave2-queue.md, item 2): a studio moves its member list,
-- what each member owes and its consigned stock in from a spreadsheet on day one.
--
-- A balance carried in posts to the journal like everything else, against opening balance
-- equity. Someone imported by email isn't a member until they accept their invite, and a member
-- balance needs an account, so their balance waits here until they join.

CREATE TABLE pending_opening_balance (
    id         uuid          PRIMARY KEY,
    studio_id  uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- Revoking the invite drops the balance with it
    invite_id  uuid          NOT NULL UNIQUE REFERENCES studio_invite (id) ON DELETE CASCADE,
    -- Positive: they owe the studio. Negative: the studio owes them (they're in credit).
    amount     numeric(12,2) NOT NULL CHECK (amount <> 0),
    as_of      date          NOT NULL,
    entry_id   uuid          REFERENCES journal_entry (id),
    created_by uuid          NOT NULL REFERENCES account (id),
    created_at timestamptz   NOT NULL
);

GRANT SELECT, INSERT, UPDATE ON pending_opening_balance TO app_rw;
ALTER TABLE pending_opening_balance ENABLE ROW LEVEL SECURITY;
CREATE POLICY pending_opening_balance_in_tenant ON pending_opening_balance FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- A consignment period: how long the studio keeps unsold work before it goes back to the artist.
-- Each item takes its return date from the payee when it comes in, as it does its share.
ALTER TABLE payee ADD COLUMN consignment_days integer CHECK (consignment_days BETWEEN 1 AND 3650);
ALTER TABLE consignment_item ADD COLUMN return_by date CHECK (return_by >= received_on);
CREATE INDEX consignment_item_return_by_idx ON consignment_item (studio_id, return_by) WHERE return_by IS NOT NULL;

ALTER TABLE journal_line DROP CONSTRAINT journal_line_account_check;
ALTER TABLE journal_line ADD CONSTRAINT journal_line_account_check CHECK (account IN (
    'BANK', 'PROCESSOR_CLEARING', 'MEMBER_RECEIVABLE', 'STORED_VALUE_LIABILITY', 'SALES_TAX_PAYABLE', 'PAYEES_PAYABLE',
    'REVENUE_FIRING', 'REVENUE_CLASSES', 'REVENUE_MEMBERSHIPS', 'REVENUE_RETAIL', 'REVENUE_EVENTS', 'REVENUE_BREAKAGE',
    'REVENUE_COMMISSIONS', 'REVENUE_OTHER', 'REFUNDS', 'PROCESSOR_FEES', 'CONTRACT_SERVICES', 'OPENING_BALANCE_EQUITY'));

ALTER TABLE journal_entry DROP CONSTRAINT journal_entry_kind_check;
ALTER TABLE journal_entry ADD CONSTRAINT journal_entry_kind_check CHECK (kind IN (
    'PIECE_FEE', 'RETAIL_SALE', 'PAYMENT', 'STORED_VALUE_SALE', 'STORED_VALUE_USE', 'STORED_VALUE_EXPIRY', 'PAYOUT',
    'REFUND', 'ADJUSTMENT', 'CONSIGNMENT_SALE', 'CONSIGNMENT_REFUND', 'PAYEE_EARNING', 'PAYEE_PAYMENT',
    'OPENING_BALANCE'));
