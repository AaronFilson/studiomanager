-- Platform settings, changed live from the admin page (docs/planning/20, section 7; ADR 24).
--
-- A setting is a key and a JSON value. Configuration supplies each default; a row overrides it,
-- and a row whose value is null means "back to the default". Rows are never deleted, so a key's
-- version only ever goes up, and a replica can tell a change from its version alone.
--
-- Only a platform admin may write. TenantAwareJpaTransactionManager sets app.platform_admin to
-- 'true' with each transaction of a request that carries the Platform.Admin app role, the same
-- way it sets app.principal_kind (ADR 16). Everyone may read the settings, because every rate
-- limit check reads them; only admins read the history.

CREATE FUNCTION app.is_platform_admin() RETURNS boolean
    LANGUAGE sql STABLE PARALLEL SAFE
    RETURN coalesce(current_setting('app.platform_admin', true), '') = 'true';

CREATE TABLE platform_setting (
    key        text        PRIMARY KEY CHECK (key ~ '^[a-z][a-z0-9.-]{0,79}$'),
    -- null: no override, the configured default applies
    value      jsonb,
    version    bigint      NOT NULL CHECK (version >= 1),
    changed_by uuid        NOT NULL REFERENCES account (id),
    changed_at timestamptz NOT NULL
);

-- Every change, written by a trigger so no write can skip it. Append-only: the app may only read.
CREATE TABLE platform_setting_change (
    id         uuid        PRIMARY KEY,
    key        text        NOT NULL REFERENCES platform_setting (key),
    version    bigint      NOT NULL,
    old_value  jsonb,
    new_value  jsonb,
    changed_by uuid        NOT NULL REFERENCES account (id),
    changed_at timestamptz NOT NULL,
    CONSTRAINT platform_setting_change_key_version_key UNIQUE (key, version)
);

CREATE INDEX platform_setting_change_changed_at_idx ON platform_setting_change (changed_at DESC);

GRANT SELECT, INSERT, UPDATE ON platform_setting TO app_rw;
GRANT SELECT ON platform_setting_change TO app_rw;

ALTER TABLE platform_setting ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform_setting_change ENABLE ROW LEVEL SECURITY;

CREATE POLICY platform_setting_read ON platform_setting
    FOR SELECT TO app_rw
    USING (true);

-- A write needs the admin binding, and names the account that made it.
CREATE POLICY platform_setting_admin_insert ON platform_setting
    FOR INSERT TO app_rw
    WITH CHECK ((SELECT app.is_platform_admin()) AND changed_by = (SELECT app.current_account_id()));

CREATE POLICY platform_setting_admin_update ON platform_setting
    FOR UPDATE TO app_rw
    USING ((SELECT app.is_platform_admin()))
    WITH CHECK ((SELECT app.is_platform_admin()) AND changed_by = (SELECT app.current_account_id()));

CREATE POLICY platform_setting_change_admin_read ON platform_setting_change
    FOR SELECT TO app_rw
    USING ((SELECT app.is_platform_admin()));

-- Runs as the owner, so the app needs no insert grant on the history and can't forge an entry.
CREATE FUNCTION app.record_platform_setting_change() RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.version <> OLD.version + 1 THEN
        RAISE EXCEPTION 'A setting''s version goes up by one with each change' USING ERRCODE = 'check_violation';
    END IF;
    IF TG_OP = 'INSERT' AND NEW.version <> 1 THEN
        RAISE EXCEPTION 'A new setting starts at version 1' USING ERRCODE = 'check_violation';
    END IF;
    INSERT INTO public.platform_setting_change (id, key, version, old_value, new_value, changed_by, changed_at)
    VALUES (uuidv7(), NEW.key, NEW.version,
            CASE WHEN TG_OP = 'UPDATE' THEN OLD.value END,
            NEW.value, NEW.changed_by, NEW.changed_at);
    RETURN NEW;
END
$$;
REVOKE ALL ON FUNCTION app.record_platform_setting_change() FROM PUBLIC;

CREATE TRIGGER platform_setting_history
    AFTER INSERT OR UPDATE ON platform_setting
    FOR EACH ROW EXECUTE FUNCTION app.record_platform_setting_change();
