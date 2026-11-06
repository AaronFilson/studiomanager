-- A system principal for background work (docs/planning/12-system-principal-plan.md, ADR 16).
--
-- Jobs used to act as a person: the studio owner, whoever made a webhook endpoint, or a made-up
-- all-zero account. Now each job has its own account row, marked SYSTEM, that no token can sign
-- in as and that can't join a studio. Jobs stay under the same row-level security as requests.
--
-- TenantAwareJpaTransactionManager also sets app.principal_kind ('person' or 'system') with each
-- transaction. The cross-tenant functions that only jobs call check it, so a request can't.

ALTER TABLE account ADD COLUMN kind text NOT NULL DEFAULT 'PERSON' CHECK (kind IN ('PERSON', 'SYSTEM'));

-- One row per job. The ids are fixed: SystemActor in platform.tenancy holds the same ones.
INSERT INTO account (id, issuer, subject, display_name, kind) VALUES
    ('5157e000-0000-4000-8000-000000000001', 'urn:studiomanager:system', 'activity-consumer',
     'System: activity consumer', 'SYSTEM'),
    ('5157e000-0000-4000-8000-000000000002', 'urn:studiomanager:system', 'maintenance-reminders',
     'System: maintenance reminders', 'SYSTEM'),
    ('5157e000-0000-4000-8000-000000000003', 'urn:studiomanager:system', 'webhook-sweep',
     'System: webhook sweep', 'SYSTEM');

-- System accounts are never members of a studio: a job reaches a studio by being bound to it, not
-- by belonging to it.
CREATE FUNCTION app.refuse_system_membership() RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.account WHERE id = NEW.account_id AND kind = 'SYSTEM') THEN
        RAISE EXCEPTION 'A system account can''t be a member of a studio' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER studio_membership_not_system
    BEFORE INSERT OR UPDATE OF account_id ON studio_membership
    FOR EACH ROW EXECUTE FUNCTION app.refuse_system_membership();

CREATE FUNCTION app.current_principal_kind() RETURNS text
    LANGUAGE sql STABLE PARALLEL SAFE
    RETURN nullif(current_setting('app.principal_kind', true), '');

-- Raises unless a system principal is bound. Called first by each job-only function below.
CREATE FUNCTION app.require_system_principal() RETURNS void
    LANGUAGE plpgsql STABLE
AS $$
BEGIN
    IF app.current_principal_kind() IS DISTINCT FROM 'system' THEN
        RAISE EXCEPTION 'Only a background job can call this' USING ERRCODE = 'insufficient_privilege';
    END IF;
END
$$;

-- The maintenance sweep no longer needs an owner to act as, so this returns only studio ids. A
-- studio whose owner left is still reminded.
DROP FUNCTION app.studios_with_maintenance_to_remind();
CREATE FUNCTION app.studios_with_maintenance_to_remind()
    RETURNS TABLE (studio_id uuid)
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    PERFORM app.require_system_principal();
    RETURN QUERY
        SELECT DISTINCT t.studio_id
        FROM public.maintenance_task t
        JOIN public.asset a ON a.id = t.asset_id
        WHERE NOT t.archived AND NOT a.archived AND t.reminded_for IS DISTINCT FROM t.last_done_at;
END
$$;
REVOKE ALL ON FUNCTION app.studios_with_maintenance_to_remind() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.studios_with_maintenance_to_remind() TO app_rw;

-- The webhook sweep no longer acts as the endpoint's creator, so this returns only ids.
DROP FUNCTION app.due_webhook_deliveries(timestamptz, integer);
CREATE FUNCTION app.due_webhook_deliveries(now timestamptz, max_rows integer)
    RETURNS TABLE (delivery_id uuid, studio_id uuid)
    LANGUAGE plpgsql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    PERFORM app.require_system_principal();
    RETURN QUERY
        SELECT d.id, d.studio_id
        FROM public.webhook_delivery d
        JOIN public.webhook_endpoint e ON e.id = d.endpoint_id
        WHERE d.status = 'PENDING' AND e.enabled AND d.next_attempt_at <= now
          AND (d.claimed_until IS NULL OR d.claimed_until < now)
        ORDER BY d.next_attempt_at
        LIMIT max_rows;
END
$$;
REVOKE ALL ON FUNCTION app.due_webhook_deliveries(timestamptz, integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.due_webhook_deliveries(timestamptz, integer) TO app_rw;

CREATE OR REPLACE FUNCTION app.purge_webhook_history(before timestamptz)
    RETURNS integer
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    gone integer;
BEGIN
    PERFORM app.require_system_principal();
    DELETE FROM public.webhook_event WHERE created_at < before;
    GET DIAGNOSTICS gone = ROW_COUNT;
    RETURN gone;
END
$$;

CREATE OR REPLACE FUNCTION app.purge_stream_event_seen(before timestamptz)
    RETURNS integer
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    gone integer;
BEGIN
    PERFORM app.require_system_principal();
    DELETE FROM public.stream_event_seen WHERE seen_at < before;
    GET DIAGNOSTICS gone = ROW_COUNT;
    RETURN gone;
END
$$;
