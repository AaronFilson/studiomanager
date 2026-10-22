-- Payables: what the studio owes the people it pays (docs/planning/08-wave2-queue.md, item 1).
--
-- The research found payables, not sales sync, to be the biggest gap in a studio's books:
-- consignors paid monthly from a spreadsheet, instructors' class shares re-keyed from a PDF, and
-- nothing recording W-9 status or whether a payment was sent. Everything here posts to the same
-- journal (ADR 9): a payee's balance is the sum of their PAYEES_PAYABLE lines.

-- Anyone the studio pays: an artist on consignment, an instructor, a guild.
CREATE TABLE payee (
    id               uuid         PRIMARY KEY,
    studio_id        uuid         NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    name             text         NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    email            text         CHECK (char_length(email) <= 254),
    -- A member, who can then be paid in studio credit
    account_id       uuid         REFERENCES account (id),
    -- Whether the studio files a 1099-NEC for them. The tax id itself is never stored here.
    files_1099       boolean      NOT NULL,
    w9_received_on   date,
    -- Consignment terms, copied onto each item taken in
    consignor_share  numeric(5,4) CHECK (consignor_share > 0 AND consignor_share <= 1),
    discounts_from   text         NOT NULL CHECK (discounts_from IN ('SHARED', 'STUDIO')),
    archived_at      timestamptz,
    created_at       timestamptz  NOT NULL,
    CONSTRAINT payee_account_key UNIQUE (studio_id, account_id)
);

-- Work taken in to sell for an artist.
CREATE TABLE consignment_item (
    id              uuid          PRIMARY KEY,
    studio_id       uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    payee_id        uuid          NOT NULL REFERENCES payee (id),
    description     text          NOT NULL CHECK (char_length(description) BETWEEN 1 AND 120),
    retail_price    numeric(12,2) NOT NULL CHECK (retail_price > 0),
    consignor_share numeric(5,4)  NOT NULL CHECK (consignor_share > 0 AND consignor_share <= 1),
    -- Who bears a discount: split by the shares, or all from the studio's commission
    discounts_from  text          NOT NULL CHECK (discounts_from IN ('SHARED', 'STUDIO')),
    quantity        integer       NOT NULL CHECK (quantity > 0),
    sold            integer       NOT NULL DEFAULT 0 CHECK (sold >= 0),
    returned        integer       NOT NULL DEFAULT 0 CHECK (returned >= 0),
    received_on     date          NOT NULL,
    created_at      timestamptz   NOT NULL,
    CONSTRAINT consignment_item_counts CHECK (sold + returned <= quantity)
);

CREATE INDEX consignment_item_payee_idx ON consignment_item (studio_id, payee_id);

CREATE TABLE consignment_sale (
    id               uuid          PRIMARY KEY,
    studio_id        uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    item_id          uuid          NOT NULL REFERENCES consignment_item (id),
    quantity         integer       NOT NULL CHECK (quantity > 0),
    -- What the buyer paid before tax, after any discount
    price            numeric(12,2) NOT NULL CHECK (price > 0),
    consignor_amount numeric(12,2) NOT NULL CHECK (consignor_amount >= 0 AND consignor_amount <= price),
    -- Sales tax charged on top, kept so a refund reverses exactly what was posted
    tax              numeric(12,2) NOT NULL CHECK (tax >= 0),
    provider         text          NOT NULL CHECK (provider IN ('STRIPE', 'SQUARE', 'SHOPIFY', 'PAYPAL', 'CASH', 'OTHER')),
    sold_on          date          NOT NULL,
    entry_id         uuid          NOT NULL REFERENCES journal_entry (id),
    refunded_on      date,
    refund_entry_id  uuid          REFERENCES journal_entry (id),
    created_at       timestamptz   NOT NULL,
    CONSTRAINT consignment_sale_refund_pair CHECK ((refunded_on IS NULL) = (refund_entry_id IS NULL))
);

CREATE INDEX consignment_sale_item_idx ON consignment_sale (item_id);

-- A share or fee a payee has earned, such as an instructor's 70% of a class, or an adjustment.
CREATE TABLE payee_earning (
    id          uuid          PRIMARY KEY,
    studio_id   uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    payee_id    uuid          NOT NULL REFERENCES payee (id),
    kind        text          NOT NULL CHECK (kind IN ('CLASS_SHARE', 'SERVICE', 'ADJUSTMENT')),
    description text          NOT NULL CHECK (char_length(description) BETWEEN 1 AND 120),
    earned_on   date          NOT NULL,
    -- For a share: what the class took and the payee's fraction of it
    gross       numeric(12,2) CHECK (gross > 0),
    share       numeric(5,4)  CHECK (share > 0 AND share <= 1),
    amount      numeric(12,2) NOT NULL CHECK (amount <> 0),
    entry_id    uuid          NOT NULL REFERENCES journal_entry (id),
    created_by  uuid          REFERENCES account (id),
    created_at  timestamptz   NOT NULL,
    CONSTRAINT payee_earning_share_pair CHECK ((gross IS NULL) = (share IS NULL)),
    CONSTRAINT payee_earning_only_adjustments_negative CHECK (amount > 0 OR kind = 'ADJUSTMENT')
);

CREATE INDEX payee_earning_payee_idx ON payee_earning (studio_id, payee_id);

-- Money sent to a payee. The method decides whether it counts towards a 1099-NEC: card and
-- payment-network payments are reported by the network on a 1099-K instead.
CREATE TABLE payee_payment (
    id          uuid          PRIMARY KEY,
    studio_id   uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    payee_id    uuid          NOT NULL REFERENCES payee (id),
    paid_on     date          NOT NULL,
    amount      numeric(12,2) NOT NULL CHECK (amount > 0),
    method      text          NOT NULL CHECK (method IN ('CHECK', 'BANK_TRANSFER', 'CASH', 'PAYPAL', 'STRIPE', 'STUDIO_CREDIT')),
    reference   text          CHECK (char_length(reference) <= 80),
    entry_id    uuid          NOT NULL REFERENCES journal_entry (id),
    created_by  uuid          REFERENCES account (id),
    created_at  timestamptz   NOT NULL
);

CREATE INDEX payee_payment_payee_idx ON payee_payment (studio_id, payee_id, paid_on);

-- The journal learns the payee sub-ledger, three accounts and four kinds of entry
ALTER TABLE journal_line ADD COLUMN payee_id uuid REFERENCES payee (id);
CREATE INDEX journal_line_payee_idx ON journal_line (studio_id, account, payee_id) WHERE payee_id IS NOT NULL;

ALTER TABLE journal_line DROP CONSTRAINT journal_line_account_check;
ALTER TABLE journal_line ADD CONSTRAINT journal_line_account_check CHECK (account IN (
    'BANK', 'PROCESSOR_CLEARING', 'MEMBER_RECEIVABLE', 'STORED_VALUE_LIABILITY', 'SALES_TAX_PAYABLE', 'PAYEES_PAYABLE',
    'REVENUE_FIRING', 'REVENUE_CLASSES', 'REVENUE_MEMBERSHIPS', 'REVENUE_RETAIL', 'REVENUE_EVENTS', 'REVENUE_BREAKAGE',
    'REVENUE_COMMISSIONS', 'REVENUE_OTHER', 'REFUNDS', 'PROCESSOR_FEES', 'CONTRACT_SERVICES'));

-- A payee's line always names the payee
ALTER TABLE journal_line ADD CONSTRAINT journal_line_payable_names_payee
    CHECK ((account = 'PAYEES_PAYABLE') = (payee_id IS NOT NULL));

ALTER TABLE journal_entry DROP CONSTRAINT journal_entry_kind_check;
ALTER TABLE journal_entry ADD CONSTRAINT journal_entry_kind_check CHECK (kind IN (
    'PIECE_FEE', 'RETAIL_SALE', 'PAYMENT', 'STORED_VALUE_SALE', 'STORED_VALUE_USE', 'STORED_VALUE_EXPIRY', 'PAYOUT',
    'REFUND', 'ADJUSTMENT', 'CONSIGNMENT_SALE', 'CONSIGNMENT_REFUND', 'PAYEE_EARNING', 'PAYEE_PAYMENT'));

GRANT SELECT, INSERT, UPDATE ON payee, consignment_item, consignment_sale TO app_rw;
GRANT SELECT, INSERT ON payee_earning, payee_payment TO app_rw;

ALTER TABLE payee ENABLE ROW LEVEL SECURITY;
ALTER TABLE consignment_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE consignment_sale ENABLE ROW LEVEL SECURITY;
ALTER TABLE payee_earning ENABLE ROW LEVEL SECURITY;
ALTER TABLE payee_payment ENABLE ROW LEVEL SECURITY;

CREATE POLICY payee_in_tenant ON payee FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY consignment_item_in_tenant ON consignment_item FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY consignment_sale_in_tenant ON consignment_sale FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY payee_earning_in_tenant ON payee_earning FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY payee_payment_in_tenant ON payee_payment FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
