-- Reminders for maintenance falling due (docs/planning/06-wave2-maintenance-plan.md, next slices).
--
-- Each task is reminded about once per cycle: when it first shows as due soon or due after it was
-- last done. The cycle is identified by last_done_at, so completing the task starts a new one.

ALTER TABLE maintenance_task ADD COLUMN reminded_for timestamptz;

ALTER TABLE notification DROP CONSTRAINT notification_kind_check;
ALTER TABLE notification ADD CONSTRAINT notification_kind_check
    CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE', 'MAINTENANCE_DUE'));

-- The studios that have a task not yet reminded about in its current cycle, each with an owner to
-- act as. The reminder sweep runs with no tenant bound, so this is SECURITY DEFINER (ADR 10); it
-- returns only ids, and the sweep does the rest inside each studio, under row-level security.
CREATE FUNCTION app.studios_with_maintenance_to_remind()
    RETURNS TABLE (studio_id uuid, owner_id uuid)
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    SELECT DISTINCT ON (t.studio_id) t.studio_id, m.account_id
    FROM public.maintenance_task t
    JOIN public.asset a ON a.id = t.asset_id
    JOIN public.studio_membership m ON m.studio_id = t.studio_id AND m.role = 'OWNER'
    WHERE NOT t.archived AND NOT a.archived AND t.reminded_for IS DISTINCT FROM t.last_done_at
    ORDER BY t.studio_id, m.created_at
$$;

REVOKE ALL ON FUNCTION app.studios_with_maintenance_to_remind() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.studios_with_maintenance_to_remind() TO app_rw;
