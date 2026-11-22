-- Supplies inbox operational hygiene and optional ledger payable recording
-- (docs/planning/16-invoice-ocr-plan.md, "Safety" and step 4).
--
-- 1. System actor and SECURITY DEFINER functions to purge raw reader answers after 90 days and
--    remove abandoned UPLOADING rows (and their blobs).
-- 2. Optional double-entry supplier bill posting on confirmation (Debit SUPPLIES_EXPENSE,
--    Credit ACCOUNTS_PAYABLE) linked from invoice.ledger_entry_id.

INSERT INTO account (id, issuer, subject, display_name, kind) VALUES
    ('5157e000-0000-4000-8000-000000000008', 'urn:studiomanager:system', 'invoice-cleanup',
     'System: invoice cleanup', 'SYSTEM');

ALTER TABLE invoice
    ADD COLUMN ledger_entry_id uuid REFERENCES journal_entry (id);

CREATE INDEX invoice_uploading_idx ON invoice (created_at) WHERE status = 'UPLOADING';
CREATE INDEX invoice_raw_created_idx ON invoice (created_at) WHERE raw IS NOT NULL;

CREATE FUNCTION app.purge_invoice_raw_answers(p_before timestamptz)
    RETURNS integer
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    purged integer;
BEGIN
    PERFORM app.require_system_principal();
    UPDATE public.invoice
    SET raw = NULL
    WHERE raw IS NOT NULL
      AND created_at < p_before;
    GET DIAGNOSTICS purged = ROW_COUNT;
    RETURN purged;
END
$$;
REVOKE ALL ON FUNCTION app.purge_invoice_raw_answers(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.purge_invoice_raw_answers(timestamptz) TO app_rw;

CREATE FUNCTION app.take_abandoned_invoice_uploads(p_before timestamptz)
    RETURNS TABLE (studio_id uuid, invoice_id uuid, file_type text)
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    PERFORM app.require_system_principal();
    RETURN QUERY
        DELETE FROM public.invoice i
        WHERE i.status = 'UPLOADING'
          AND i.created_at < p_before
        RETURNING i.studio_id, i.id, i.file_type;
END
$$;
REVOKE ALL ON FUNCTION app.take_abandoned_invoice_uploads(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.take_abandoned_invoice_uploads(timestamptz) TO app_rw;

ALTER TABLE journal_line DROP CONSTRAINT journal_line_account_check;
ALTER TABLE journal_line ADD CONSTRAINT journal_line_account_check CHECK (account IN (
    'BANK', 'PROCESSOR_CLEARING', 'MEMBER_RECEIVABLE', 'STORED_VALUE_LIABILITY', 'SALES_TAX_PAYABLE', 'PAYEES_PAYABLE',
    'ACCOUNTS_PAYABLE', 'REVENUE_FIRING', 'REVENUE_CLASSES', 'REVENUE_MEMBERSHIPS', 'REVENUE_RETAIL', 'REVENUE_EVENTS',
    'REVENUE_BREAKAGE', 'REVENUE_COMMISSIONS', 'REVENUE_OTHER', 'REFUNDS', 'PROCESSOR_FEES', 'CONTRACT_SERVICES',
    'SUPPLIES_EXPENSE', 'OPENING_BALANCE_EQUITY'));

ALTER TABLE journal_entry DROP CONSTRAINT journal_entry_kind_check;
ALTER TABLE journal_entry ADD CONSTRAINT journal_entry_kind_check CHECK (kind IN (
    'PIECE_FEE', 'RETAIL_SALE', 'PAYMENT', 'STORED_VALUE_SALE', 'STORED_VALUE_USE', 'STORED_VALUE_EXPIRY', 'PAYOUT',
    'REFUND', 'ADJUSTMENT', 'CONSIGNMENT_SALE', 'CONSIGNMENT_REFUND', 'PAYEE_EARNING', 'PAYEE_PAYMENT',
    'OPENING_BALANCE', 'SUPPLIER_BILL'));
