-- Each download of the studio's export is logged with its account (plan 21 §7: every owner-only
-- action records who took it). It goes in the access history, so co-owners see it on the roles
-- page and through the access.changed webhook and stream event.
ALTER TABLE role_change_log DROP CONSTRAINT role_change_log_kind_check;
ALTER TABLE role_change_log ADD CONSTRAINT role_change_log_kind_check
    CHECK (kind IN ('ROLE_PERMISSIONS', 'MEMBER_DUTIES', 'MEMBER_ROLE', 'BILLING_CONTACT',
                    'ROLE_CREATED', 'ROLE_RENAMED', 'ROLE_DELETED', 'STUDIO_EXPORTED'));
