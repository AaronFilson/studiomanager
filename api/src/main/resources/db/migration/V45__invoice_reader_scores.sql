-- Keep compared drafts from Compare readers and score each against what staff confirm
-- (docs/planning/24-invoice-reading-upgrade-plan.md, "Keep score after confirming").

CREATE TABLE invoice_comparison (
    id             uuid          PRIMARY KEY,
    studio_id      uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    invoice_id     uuid          NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    reader         text          NOT NULL CHECK (reader IN ('CLAUDE', 'GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE')),
    model          text          CHECK (char_length(model) <= 80),
    draft          jsonb         NOT NULL,
    elapsed_ms     bigint        NOT NULL CHECK (elapsed_ms >= 0),
    cost_usd       numeric(10,6) CHECK (cost_usd >= 0),
    compared_at    timestamptz   NOT NULL,
    scored_at      timestamptz,
    matched_fields integer       CHECK (matched_fields >= 0),
    total_fields   integer       CHECK (total_fields >= 0),
    matched_lines  integer       CHECK (matched_lines >= 0),
    total_lines    integer       CHECK (total_lines >= 0),
    exact_match    boolean,
    line_accuracy  numeric(5,4)  CHECK (line_accuracy BETWEEN 0 AND 1),
    false_alarms   integer       CHECK (false_alarms >= 0),
    missed         boolean,
    CONSTRAINT invoice_comparison_invoice_reader_key UNIQUE (studio_id, invoice_id, reader)
);

CREATE INDEX invoice_comparison_invoice_idx ON invoice_comparison (invoice_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON invoice_comparison TO app_rw;

ALTER TABLE invoice_comparison ENABLE ROW LEVEL SECURITY;

CREATE POLICY invoice_comparison_in_tenant ON invoice_comparison FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Cross-tenant reader accuracy summary for the platform admin page (ADR 10, V38 pattern).
-- Refuses anyone not bound as a platform admin.
CREATE FUNCTION app.reader_accuracy_summary()
    RETURNS TABLE (
        reader          text,
        confirmed_count bigint,
        exact_matches   bigint,
        matched_fields  bigint,
        total_fields    bigint,
        matched_lines   bigint,
        total_lines     bigint
    )
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF NOT app.is_platform_admin() THEN
        RAISE EXCEPTION 'Only a platform admin can see reader accuracy across studios' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN QUERY
        SELECT c.reader,
               count(*)::bigint,
               count(*) FILTER (WHERE c.exact_match)::bigint,
               coalesce(sum(c.matched_fields), 0)::bigint,
               coalesce(sum(c.total_fields), 0)::bigint,
               coalesce(sum(c.matched_lines), 0)::bigint,
               coalesce(sum(c.total_lines), 0)::bigint
        FROM public.invoice_comparison c
        WHERE c.scored_at IS NOT NULL
        GROUP BY c.reader
        ORDER BY c.reader;
END
$$;
REVOKE ALL ON FUNCTION app.reader_accuracy_summary() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.reader_accuracy_summary() TO app_rw;
