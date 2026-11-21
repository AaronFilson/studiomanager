-- Adds studio notices to assistant pending actions (docs/planning/17-mcp-and-api-tokens-plan.md,
-- propose_notice). Approving sends the notice to the studio through NotificationService.broadcast
-- as the staff member who approved.

ALTER TABLE pending_action
    ADD COLUMN notice_title text CHECK (char_length(notice_title) BETWEEN 1 AND 120),
    ADD COLUMN notice_body  text CHECK (char_length(notice_body) <= 2000);

-- Replace the unnamed V36 kind/shape checks with explicitly named constraints covering STUDIO_NOTICE.
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = 'public.pending_action'::regclass
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ~ '\ykind\y'
    LOOP
        EXECUTE format('ALTER TABLE public.pending_action DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END
$$;

ALTER TABLE pending_action
    ADD CONSTRAINT pending_action_kind_check
        CHECK (kind IN ('PURCHASE_REQUEST', 'MAINTENANCE_TASK', 'STUDIO_NOTICE')),
    ADD CONSTRAINT pending_action_purchase_request_check
        CHECK (kind <> 'PURCHASE_REQUEST'
               OR (item_id IS NOT NULL AND quantity IS NOT NULL
                   AND asset_id IS NULL AND title IS NULL AND every_days IS NULL AND every_firings IS NULL
                   AND notice_title IS NULL AND notice_body IS NULL)),
    ADD CONSTRAINT pending_action_maintenance_task_check
        CHECK (kind <> 'MAINTENANCE_TASK'
               OR (asset_id IS NOT NULL AND title IS NOT NULL AND (every_days IS NOT NULL OR every_firings IS NOT NULL)
                   AND item_id IS NULL AND quantity IS NULL AND note IS NULL
                   AND notice_title IS NULL AND notice_body IS NULL)),
    ADD CONSTRAINT pending_action_studio_notice_check
        CHECK (kind <> 'STUDIO_NOTICE'
               OR (notice_title IS NOT NULL AND notice_body IS NOT NULL
                   AND item_id IS NULL AND quantity IS NULL AND note IS NULL
                   AND asset_id IS NULL AND title IS NULL AND every_days IS NULL AND every_firings IS NULL));
