-- The money ledger: a double-entry journal per studio.
--
-- Every financial event (a piece's fee, a payment at the desk, a gift card sold or used, a
-- processor payout) is one journal entry whose lines sum to zero. A deferred constraint trigger
-- checks that at commit, so no code path can write an unbalanced entry. Balances (what a member
-- owes, what is left on a firing card, what a processor still owes the studio) are sums of lines.
--
-- Amounts are signed: debits positive, credits negative.

CREATE TABLE tax_rate (
    id             uuid         PRIMARY KEY,
    studio_id      uuid         NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    tax_category   text         NOT NULL CHECK (char_length(tax_category) BETWEEN 1 AND 40),
    -- A fraction: 0.0825 is 8.25%
    rate           numeric(7,6) NOT NULL CHECK (rate >= 0 AND rate < 1),
    effective_from date         NOT NULL,
    CONSTRAINT tax_rate_category_date_key UNIQUE (studio_id, tax_category, effective_from)
);

-- Prepaid money held for someone: a gift card, a firing card, a class pack, a deposit, or credit.
CREATE TABLE stored_value (
    id         uuid        PRIMARY KEY,
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    kind       text        NOT NULL CHECK (kind IN ('GIFT_CARD', 'FIRING_CARD', 'CLASS_PACK', 'DEPOSIT', 'CREDIT')),
    -- The member it belongs to; gift cards can be held by anyone with the code
    holder     uuid        REFERENCES account (id),
    code       text        CHECK (char_length(code) BETWEEN 4 AND 40),
    label      text        CHECK (char_length(label) <= 80),
    expires_on date,
    created_at timestamptz NOT NULL,
    CONSTRAINT stored_value_code_key UNIQUE (studio_id, code)
);

CREATE TABLE journal_entry (
    id              uuid        PRIMARY KEY,
    studio_id       uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- The studio's local date the money moved, which is what bookkeeping is reported by
    occurred_on     date        NOT NULL,
    kind            text        NOT NULL CHECK (kind IN ('PIECE_FEE', 'PAYMENT', 'STORED_VALUE_SALE', 'STORED_VALUE_USE',
                                                         'STORED_VALUE_EXPIRY', 'PAYOUT', 'REFUND', 'ADJUSTMENT')),
    memo            text        NOT NULL CHECK (char_length(memo) BETWEEN 1 AND 200),
    -- What caused it (a piece, a payout...), and a key so a retried import or event posts once
    source_id       uuid,
    idempotency_key text        NOT NULL CHECK (char_length(idempotency_key) BETWEEN 1 AND 120),
    created_by      uuid        REFERENCES account (id),
    created_at      timestamptz NOT NULL,
    CONSTRAINT journal_entry_idempotency_key UNIQUE (studio_id, idempotency_key)
);

CREATE INDEX journal_entry_studio_date_idx ON journal_entry (studio_id, occurred_on);

CREATE TABLE journal_line (
    id           uuid          PRIMARY KEY,
    entry_id     uuid          NOT NULL REFERENCES journal_entry (id) ON DELETE CASCADE,
    studio_id    uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account      text          NOT NULL CHECK (account IN (
                                   'BANK', 'PROCESSOR_CLEARING', 'MEMBER_RECEIVABLE', 'STORED_VALUE_LIABILITY',
                                   'SALES_TAX_PAYABLE', 'REVENUE_FIRING', 'REVENUE_CLASSES', 'REVENUE_MEMBERSHIPS',
                                   'REVENUE_RETAIL', 'REVENUE_EVENTS', 'REVENUE_BREAKAGE', 'REVENUE_OTHER',
                                   'REFUNDS', 'PROCESSOR_FEES')),
    amount       numeric(14,2) NOT NULL CHECK (amount <> 0),
    -- Sub-ledgers: whose receivable, which stored-value balance, which processor
    member_id    uuid          REFERENCES account (id),
    stored_value uuid          REFERENCES stored_value (id),
    provider     text          CHECK (provider IN ('STRIPE', 'SQUARE', 'SHOPIFY', 'PAYPAL', 'CASH', 'OTHER')),
    tax_category text
);

CREATE INDEX journal_line_entry_idx ON journal_line (entry_id);
CREATE INDEX journal_line_member_idx ON journal_line (studio_id, account, member_id);
CREATE INDEX journal_line_stored_value_idx ON journal_line (stored_value) WHERE stored_value IS NOT NULL;

-- Every entry's lines must sum to zero by the end of the transaction that writes them
CREATE FUNCTION app.check_journal_entry_balances() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    v_total numeric;
BEGIN
    SELECT coalesce(sum(amount), 0) INTO v_total FROM public.journal_line WHERE entry_id = NEW.entry_id;
    IF v_total <> 0 THEN
        RAISE EXCEPTION 'Journal entry % does not balance (off by %)', NEW.entry_id, v_total
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END
$$;

CREATE CONSTRAINT TRIGGER journal_entry_balances
    AFTER INSERT OR UPDATE ON journal_line
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION app.check_journal_entry_balances();

-- A processor's deposit into the studio's bank, imported from its report or API.
CREATE TABLE payout (
    id          uuid          PRIMARY KEY,
    studio_id   uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    provider    text          NOT NULL CHECK (provider IN ('STRIPE', 'SQUARE', 'SHOPIFY', 'PAYPAL', 'OTHER')),
    external_id text          NOT NULL CHECK (char_length(external_id) BETWEEN 1 AND 120),
    arrival_on  date          NOT NULL,
    gross       numeric(14,2) NOT NULL,
    refunds     numeric(14,2) NOT NULL CHECK (refunds >= 0),
    fees        numeric(14,2) NOT NULL CHECK (fees >= 0),
    adjustments numeric(14,2) NOT NULL,
    net         numeric(14,2) NOT NULL,
    currency    text          NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    entry_id    uuid          NOT NULL REFERENCES journal_entry (id),
    imported_at timestamptz   NOT NULL,
    CONSTRAINT payout_provider_external_key UNIQUE (studio_id, provider, external_id),
    -- The processor's own arithmetic: what landed is sales less refunds and fees, plus adjustments
    CONSTRAINT payout_adds_up CHECK (net = gross - refunds - fees + adjustments)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tax_rate TO app_rw;
GRANT SELECT, INSERT, UPDATE ON stored_value TO app_rw;
GRANT SELECT, INSERT ON journal_entry, journal_line, payout TO app_rw;

ALTER TABLE tax_rate ENABLE ROW LEVEL SECURITY;
ALTER TABLE stored_value ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE payout ENABLE ROW LEVEL SECURITY;

CREATE POLICY tax_rate_in_tenant ON tax_rate FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY stored_value_in_tenant ON stored_value FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY journal_entry_in_tenant ON journal_entry FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY journal_line_in_tenant ON journal_line FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY payout_in_tenant ON payout FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
