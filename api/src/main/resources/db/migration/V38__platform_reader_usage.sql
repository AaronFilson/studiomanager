-- How many pages the platform-paid invoice readers (Google Document AI, Azure AI Document
-- Intelligence) have read since a moment, across every studio (docs/planning/20, "Invoice readers").
--
-- invoice_read is per studio under row-level security, but a platform-wide monthly cap needs the
-- total for all of them, and the admin page shows each studio's use. Both cross tenants (ADR 10),
-- so they are functions that run as the owner and return only counts.

-- The platform-wide total per reader. Any caller may ask: it names no studio, and the reading job,
-- bound to one studio, checks the platform cap with it before each read.
CREATE FUNCTION app.platform_reader_pages(since timestamptz)
    RETURNS TABLE (reader text, pages bigint)
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    SELECT r.reader, coalesce(sum(r.pages), 0)::bigint
    FROM public.invoice_read r
    WHERE r.created_at >= since
      AND r.reader IN ('GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE')
    GROUP BY r.reader
$$;
REVOKE ALL ON FUNCTION app.platform_reader_pages(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.platform_reader_pages(timestamptz) TO app_rw;

-- Each studio's pages, reads and estimated spend per platform-paid reader, for the admin page.
-- Refuses anyone not bound as a platform admin.
CREATE FUNCTION app.platform_reader_usage_by_studio(since timestamptz)
    RETURNS TABLE (studio_id uuid, reader text, reads bigint, pages bigint, cost_usd numeric)
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF NOT app.is_platform_admin() THEN
        RAISE EXCEPTION 'Only a platform admin can see every studio''s reader use' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN QUERY
        SELECT r.studio_id, r.reader, count(*)::bigint, coalesce(sum(r.pages), 0)::bigint,
               coalesce(sum(r.cost_usd), 0)::numeric
        FROM public.invoice_read r
        WHERE r.created_at >= since
          AND r.reader IN ('GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE')
        GROUP BY r.studio_id, r.reader
        ORDER BY r.studio_id, r.reader;
END
$$;
REVOKE ALL ON FUNCTION app.platform_reader_usage_by_studio(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.platform_reader_usage_by_studio(timestamptz) TO app_rw;
