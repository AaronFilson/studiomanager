-- Stored permission sets (docs/planning/21-studio-rbac-plan.md, lane R4; ADR 25).
--
-- Each studio has its three system roles, and one editable permission set per role. The catalogue
-- itself is code (Permission.java): these rows only say which of its keys a role holds.
--
-- What is stored, and what isn't:
--   * OWNER holds every permission, always, in code. Its role row exists, but it has no permission
--     rows and can't be given any.
--   * The self-service floor (money.own.read) is held by every role, in code, and isn't stored.
--   * Owner-only permissions can never be stored for any role (plan 21, decision 5): the check on
--     studio_role_permission refuses them.
--   * Duties grant fixed permissions in code (decision 6), not here.
--
-- studio_membership keeps its role column. Until custom roles (R7) a membership's role row is
-- found by (studio_id, base), so there is no role_id on it yet. The last-owner guard is R8's.

CREATE TABLE studio_role (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- 'owner', 'staff' or 'member' for the system roles; a slug for R7's custom roles
    key         text        NOT NULL CHECK (key ~ '^[a-z][a-z0-9_]{0,39}$'),
    name        text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    -- The rank a role sits at: who may assign it, and the ceiling for jobs
    base        text        NOT NULL CHECK (base IN ('OWNER', 'STAFF', 'MEMBER')),
    is_system   boolean     NOT NULL,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    UNIQUE (studio_id, key),
    -- The target of studio_role_permission's foreign key, so a permission row can't name another
    -- studio's role
    UNIQUE (studio_id, id)
);
-- One system role per base in a studio
CREATE UNIQUE INDEX studio_role_system_base_idx ON studio_role (studio_id, base) WHERE is_system;

GRANT SELECT, INSERT, UPDATE ON studio_role TO app_rw;

ALTER TABLE studio_role ENABLE ROW LEVEL SECURITY;
CREATE POLICY studio_role_in_tenant ON studio_role FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

CREATE TABLE studio_role_permission (
    studio_id   uuid NOT NULL,
    role_id     uuid NOT NULL,
    -- A Permission key. A key the running code doesn't know is ignored, never granted
    permission  text NOT NULL CHECK (permission ~ '^[a-z_]+(\.[a-z_]+)+$')
        -- Owner-only (decision 5) and the floor are never stored
        CHECK (permission NOT IN ('studio.export', 'studio.ai_key.manage', 'studio.roles.manage',
                                  'api_tokens.manage_all', 'supplies.purchase.approve',
                                  'owners.manage', 'money.own.read')),
    PRIMARY KEY (studio_id, role_id, permission),
    FOREIGN KEY (studio_id, role_id) REFERENCES studio_role (studio_id, id) ON DELETE CASCADE
);

-- Rows are added and removed, never changed
GRANT SELECT, INSERT, DELETE ON studio_role_permission TO app_rw;

ALTER TABLE studio_role_permission ENABLE ROW LEVEL SECURITY;
CREATE POLICY studio_role_permission_in_tenant ON studio_role_permission FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Every change to who may do what (plan 21 §7), written in the same transaction as the change.
-- Kept for the life of the studio; the app can add rows but never change or remove them.
CREATE TABLE role_change_log (
    id                uuid        PRIMARY KEY,
    studio_id         uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    actor_account_id  uuid        NOT NULL REFERENCES account (id),
    via_token_id      uuid        REFERENCES api_token (id) ON DELETE SET NULL,
    kind              text        NOT NULL CHECK (kind IN ('ROLE_PERMISSIONS', 'MEMBER_DUTIES')),
    -- What changed: a role's key, or a member's account id
    target            text        NOT NULL CHECK (char_length(target) BETWEEN 1 AND 60),
    -- Sorted, comma-separated permission keys or duty names
    before_value      text        NOT NULL CHECK (char_length(before_value) <= 4000),
    after_value       text        NOT NULL CHECK (char_length(after_value) <= 4000),
    at                timestamptz NOT NULL
);
CREATE INDEX role_change_log_studio_idx ON role_change_log (studio_id, at);

GRANT SELECT, INSERT ON role_change_log TO app_rw;

ALTER TABLE role_change_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY role_change_log_in_tenant ON role_change_log FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Seed every existing studio with exactly the code defaults (Permission.defaultsFor), so nothing
-- changes on day one. New studios get the same from StudioService. RolePermissionMigrationTests
-- checks these lists against the code.
INSERT INTO studio_role (id, studio_id, key, name, base, is_system, created_at, updated_at)
SELECT gen_random_uuid(), s.id, r.key, r.name, r.base, true, now(), now()
FROM studio s
CROSS JOIN (VALUES ('owner', 'Owner', 'OWNER'),
                   ('staff', 'Staff', 'STAFF'),
                   ('member', 'Member', 'MEMBER')) AS r (key, name, base);

INSERT INTO studio_role_permission (studio_id, role_id, permission)
SELECT r.studio_id, r.id, p.permission
FROM studio_role r
JOIN (VALUES
    -- MEMBER and above
    ('studio.read', 'MEMBER'), ('pieces.intake_own', 'MEMBER'), ('pieces.read', 'MEMBER'),
    ('kilns.read', 'MEMBER'), ('fees.read', 'MEMBER'), ('maintenance.read', 'MEMBER'),
    ('maintenance.report_problem', 'MEMBER'), ('schedule.read', 'MEMBER'),
    ('schedule.book_own', 'MEMBER'), ('schedule.shifts.take_open', 'MEMBER'),
    ('supplies.read', 'MEMBER'), ('glazes.read', 'MEMBER'), ('glazes.own.edit', 'MEMBER'),
    ('notices.read', 'MEMBER'),
    -- STAFF
    ('studio.settings.edit', 'STAFF'), ('members.read', 'STAFF'), ('members.invite', 'STAFF'),
    ('members.role.assign', 'STAFF'), ('members.remove', 'STAFF'),
    ('members.duties.manage', 'STAFF'), ('members.certify', 'STAFF'),
    ('pieces.intake_for_others', 'STAFF'), ('pieces.move', 'STAFF'),
    ('pieces.policy.edit', 'STAFF'), ('kilns.manage', 'STAFF'), ('firings.manage', 'STAFF'),
    ('fees.edit', 'STAFF'), ('maintenance.manage', 'STAFF'),
    ('maintenance.complete_any', 'STAFF'), ('maintenance.reports.resolve', 'STAFF'),
    ('schedule.manage', 'STAFF'), ('schedule.hours.manage', 'STAFF'),
    ('schedule.book_for_others', 'STAFF'), ('supplies.manage', 'STAFF'),
    ('supplies.sell', 'STAFF'), ('supplies.purchase.request', 'STAFF'),
    ('invoices.manage', 'STAFF'), ('glazes.studio.edit', 'STAFF'), ('money.read', 'STAFF'),
    ('money.take_payment', 'STAFF'), ('money.stored_value.manage', 'STAFF'),
    ('money.refund_and_adjust', 'STAFF'), ('money.payouts', 'STAFF'),
    ('money.payables.manage', 'STAFF'), ('money.tax_rates.edit', 'STAFF'),
    ('notices.send', 'STAFF'), ('today.read', 'STAFF'), ('activity.read', 'STAFF'),
    ('imports.run', 'STAFF'), ('webhooks.manage', 'STAFF'), ('api_tokens.own', 'STAFF'),
    ('mcp.use', 'STAFF'), ('proposals.create', 'STAFF'), ('proposals.review', 'STAFF')
) AS p (permission, minimum)
  ON r.base = 'STAFF' OR (r.base = 'MEMBER' AND p.minimum = 'MEMBER');
