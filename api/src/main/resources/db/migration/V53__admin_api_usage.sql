-- Admin API & MCP usage overview (docs/planning/20-pre-production-cleanup-plan.md, section 7).
--
-- api_call_log and api_token are under studio row-level security. These SECURITY DEFINER functions
-- aggregate calls, rate-limit refusals, errors and distinct active hours across studios for the
-- platform admin page, refusing anyone not bound as a platform admin (ADR 10, ADR 24).

CREATE FUNCTION app.admin_api_usage_by_studio(since_hour timestamptz, since_day timestamptz)
    RETURNS TABLE (
        studio_id     uuid,
        studio_name   text,
        channel       text,
        calls_hour    bigint,
        refusals_hour bigint,
        errors_hour   bigint,
        calls_day     bigint,
        refusals_day  bigint,
        errors_day    bigint
    )
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF NOT app.is_platform_admin() THEN
        RAISE EXCEPTION 'Only a platform admin can see API usage across studios'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN QUERY
        SELECT l.studio_id,
               s.name,
               l.channel,
               count(*) FILTER (WHERE l.created_at >= since_hour)::bigint,
               count(*) FILTER (WHERE l.created_at >= since_hour AND l.outcome = 'LIMITED')::bigint,
               count(*) FILTER (WHERE l.created_at >= since_hour AND l.outcome = 'ERROR')::bigint,
               count(*)::bigint,
               count(*) FILTER (WHERE l.outcome = 'LIMITED')::bigint,
               count(*) FILTER (WHERE l.outcome = 'ERROR')::bigint
        FROM public.api_call_log l
        JOIN public.studio s ON s.id = l.studio_id
        WHERE l.created_at >= since_day
        GROUP BY l.studio_id, s.name, l.channel
        ORDER BY count(*) DESC, s.name ASC, l.channel ASC;
END
$$;
REVOKE ALL ON FUNCTION app.admin_api_usage_by_studio(timestamptz, timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.admin_api_usage_by_studio(timestamptz, timestamptz) TO app_rw;

CREATE FUNCTION app.admin_api_usage_by_token(since_hour timestamptz, since_day timestamptz)
    RETURNS TABLE (
        token_id         uuid,
        token_name       text,
        studio_id        uuid,
        studio_name      text,
        calls_hour       bigint,
        refusals_hour    bigint,
        errors_hour      bigint,
        calls_day        bigint,
        refusals_day     bigint,
        errors_day       bigint,
        active_hours_day bigint
    )
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF NOT app.is_platform_admin() THEN
        RAISE EXCEPTION 'Only a platform admin can see token usage across studios'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN QUERY
        SELECT l.token_id,
               t.name,
               l.studio_id,
               s.name,
               count(*) FILTER (WHERE l.created_at >= since_hour)::bigint,
               count(*) FILTER (WHERE l.created_at >= since_hour AND l.outcome = 'LIMITED')::bigint,
               count(*) FILTER (WHERE l.created_at >= since_hour AND l.outcome = 'ERROR')::bigint,
               count(*)::bigint,
               count(*) FILTER (WHERE l.outcome = 'LIMITED')::bigint,
               count(*) FILTER (WHERE l.outcome = 'ERROR')::bigint,
               count(DISTINCT date_trunc('hour', l.created_at))::bigint
        FROM public.api_call_log l
        JOIN public.api_token t ON t.id = l.token_id
        JOIN public.studio s ON s.id = l.studio_id
        WHERE l.created_at >= since_day
          AND l.token_id IS NOT NULL
        GROUP BY l.token_id, t.name, l.studio_id, s.name
        ORDER BY count(*) DESC, s.name ASC, t.name ASC;
END
$$;
REVOKE ALL ON FUNCTION app.admin_api_usage_by_token(timestamptz, timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.admin_api_usage_by_token(timestamptz, timestamptz) TO app_rw;
