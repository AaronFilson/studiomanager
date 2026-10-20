-- Every studio gets a country and a currency. Sales tax, the money ledger, payout
-- reconciliation and the choice of accounting integration all depend on them
-- (docs/planning/02-wave1-plan.md, decisions of 2026-10-20).
--
-- Only local development databases have studios when this migration runs, so they are
-- backfilled as US / USD and the defaults are dropped again.

ALTER TABLE studio
    ADD COLUMN country  text NOT NULL DEFAULT 'US' CHECK (country ~ '^[A-Z]{2}$'),
    ADD COLUMN currency text NOT NULL DEFAULT 'USD' CHECK (currency ~ '^[A-Z]{3}$');

ALTER TABLE studio
    ALTER COLUMN country DROP DEFAULT,
    ALTER COLUMN currency DROP DEFAULT;
