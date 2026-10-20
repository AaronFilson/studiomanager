-- Tenancy foundation: accounts, studios (the tenants), memberships, and row-level security.
--
-- Isolation model: one shared schema, a studio_id on every studio-owned row, and PostgreSQL
-- row-level security (RLS) beneath the application's own checks. The application connects as
-- a member of app_rw, which owns nothing, so the policies always apply to it. Flyway runs as
-- the owner and is not subject to them.
--
-- The policies read two settings that TenantAwareJpaTransactionManager sets at the start of
-- every transaction, and that vanish when it ends:
--   app.account_id  the signed-in account
--   app.studio_id   the studio the request acts in, if any
--
-- Each migration grants app_rw exactly what it needs on the tables it creates.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_rw') THEN
        CREATE ROLE app_rw NOLOGIN;
    END IF;
END
$$;

CREATE SCHEMA app;
GRANT USAGE ON SCHEMA app TO app_rw;

CREATE FUNCTION app.current_account_id() RETURNS uuid
    LANGUAGE sql STABLE PARALLEL SAFE
    RETURN nullif(current_setting('app.account_id', true), '')::uuid;

CREATE FUNCTION app.current_studio_id() RETURNS uuid
    LANGUAGE sql STABLE PARALLEL SAFE
    RETURN nullif(current_setting('app.studio_id', true), '')::uuid;

-- The identity directory: one row per person, whatever studios they belong to. It holds only
-- what the identity provider asserts and is reached by issuer and subject, so it has no RLS.
CREATE TABLE account (
    id           uuid        PRIMARY KEY,
    issuer       text        NOT NULL,
    subject      text        NOT NULL,
    display_name text,
    email        text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT account_issuer_subject_key UNIQUE (issuer, subject)
);

CREATE TABLE studio (
    id          uuid        PRIMARY KEY,
    name        text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    unit_system text        NOT NULL CHECK (unit_system IN ('US_CUSTOMARY', 'METRIC')),
    time_zone   text        NOT NULL,
    created_at  timestamptz NOT NULL,
    version     bigint      NOT NULL
);

CREATE TABLE studio_membership (
    id         uuid        PRIMARY KEY,
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id uuid        NOT NULL REFERENCES account (id),
    role       text        NOT NULL CHECK (role IN ('OWNER', 'STAFF', 'MEMBER')),
    created_at timestamptz NOT NULL,
    version    bigint      NOT NULL,
    CONSTRAINT studio_membership_studio_account_key UNIQUE (studio_id, account_id)
);

CREATE INDEX studio_membership_account_id_idx ON studio_membership (account_id);

GRANT SELECT, INSERT, UPDATE ON account TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON studio, studio_membership TO app_rw;

ALTER TABLE studio ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_membership ENABLE ROW LEVEL SECURITY;

-- The functions are wrapped in (SELECT ...) so PostgreSQL evaluates them once per statement
-- rather than once per row.

-- Inside a studio: that studio's row and memberships, for reading and writing.
CREATE POLICY studio_in_tenant ON studio
    FOR ALL TO app_rw
    USING (id = (SELECT app.current_studio_id()))
    WITH CHECK (id = (SELECT app.current_studio_id()));

CREATE POLICY membership_in_tenant ON studio_membership
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Outside any one studio (the "my studios" list): people can read their own memberships and
-- the studios those point to. Read-only, because every write happens inside a studio.
CREATE POLICY membership_own_read ON studio_membership
    FOR SELECT TO app_rw
    USING (account_id = (SELECT app.current_account_id()));

CREATE POLICY studio_member_read ON studio
    FOR SELECT TO app_rw
    USING (EXISTS (
        SELECT 1
        FROM studio_membership m
        WHERE m.studio_id = studio.id
          AND m.account_id = (SELECT app.current_account_id())
    ));
