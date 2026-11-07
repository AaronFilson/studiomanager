-- Several co-owners per studio (docs/planning/21-studio-rbac-plan.md, lane R8; ADR 25).
--
-- A studio always has at least one owner. The API refuses to demote the last one ("Add another
-- owner first") and locks the owner rows while it decides, so two owners demoting each other at
-- once can't leave none. The trigger below is the backstop: a deferred constraint trigger that
-- refuses, at commit, any transaction leaving a studio with no OWNER membership, whatever made the
-- change.
--
-- V14's single-owner lookup is already gone: V25 replaced app.studios_with_maintenance_to_remind()
-- with one that returns studio ids only, and the reminder goes to everyone holding
-- maintenance.manage, which is every owner. No SQL reads "the" owner any more; the reminder test
-- covers a two-owner studio.

-- The studio's one billing contact (plan 21 §7): an account that must be an owner. Null when the
-- owner who held it stopped being one, until the remaining owners pick another.
ALTER TABLE studio ADD COLUMN billing_contact_account_id uuid REFERENCES account (id);

-- Existing studios: their earliest owner, the one V14 used to pick.
UPDATE studio s
SET billing_contact_account_id = (
    SELECT m.account_id
    FROM studio_membership m
    WHERE m.studio_id = s.id AND m.role = 'OWNER'
    ORDER BY m.created_at, m.account_id
    LIMIT 1);

-- Owner changes and the billing contact are recorded in the access log too.
ALTER TABLE role_change_log DROP CONSTRAINT role_change_log_kind_check;
ALTER TABLE role_change_log ADD CONSTRAINT role_change_log_kind_check
    CHECK (kind IN ('ROLE_PERMISSIONS', 'MEMBER_DUTIES', 'MEMBER_ROLE', 'BILLING_CONTACT'));

-- Owners are told when an owner is added or removed, or when the billing contact needs choosing.
ALTER TABLE notification DROP CONSTRAINT notification_kind_check;
ALTER TABLE notification ADD CONSTRAINT notification_kind_check
    CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE', 'MAINTENANCE_DUE', 'PROBLEM_REPORTED', 'SHIFT_COVER',
                    'WEBHOOK_DISABLED', 'OWNERS_CHANGED'));

-- At commit: the studio still has an owner after a membership change, and its billing contact, if any, is one of them. A
-- studio that was itself deleted takes its memberships with it and is skipped. SECURITY DEFINER so
-- it sees every membership whatever the caller's binding (ADR 10).
CREATE FUNCTION app.check_studio_owners()
    RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    v_studio  uuid;
    v_contact uuid;
BEGIN
    IF TG_TABLE_NAME = 'studio' THEN
        v_studio := NEW.id;
    ELSE
        v_studio := OLD.studio_id;
    END IF;

    SELECT s.billing_contact_account_id INTO v_contact FROM public.studio s WHERE s.id = v_studio;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    -- Only a membership change can take away an owner. (A studio row is checked for its contact
    -- alone, so one made before its first membership commits, as tests do, isn't refused.)
    IF TG_TABLE_NAME = 'studio_membership' AND NOT EXISTS (
        SELECT 1 FROM public.studio_membership m WHERE m.studio_id = v_studio AND m.role = 'OWNER'
    ) THEN
        RAISE EXCEPTION 'A studio must keep at least one owner. Add another owner first.'
            USING ERRCODE = 'check_violation';
    END IF;

    IF v_contact IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM public.studio_membership m
        WHERE m.studio_id = v_studio AND m.account_id = v_contact AND m.role = 'OWNER'
    ) THEN
        RAISE EXCEPTION 'The billing contact must be an owner of the studio.'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END
$$;
REVOKE ALL ON FUNCTION app.check_studio_owners() FROM PUBLIC;

-- Inserting a membership can't remove an owner, so only changes and deletes are checked. A new
-- studio's first owner is inserted after the studio, which the deferral allows.
CREATE CONSTRAINT TRIGGER studio_membership_keeps_an_owner
    AFTER UPDATE OF role, studio_id OR DELETE ON studio_membership
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION app.check_studio_owners();

CREATE CONSTRAINT TRIGGER studio_billing_contact_is_an_owner
    AFTER INSERT OR UPDATE OF billing_contact_account_id ON studio
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION app.check_studio_owners();
