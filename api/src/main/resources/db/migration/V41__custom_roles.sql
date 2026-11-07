-- Custom roles (docs/planning/21-studio-rbac-plan.md, lane R7; ADR 25).
--
-- An owner makes named roles such as "Kiln tech" or "Board", each with a base (STAFF or MEMBER,
-- never OWNER) and its own permission set, and puts members in them. The base is what the rest of
-- the system sees: TenantContext.role, jobs, and anything still keyed on studio_membership.role.
--
-- A membership now points at its role row (role_id). The role column stays, always equal to that
-- row's base, so V40's last-owner trigger and every existing query keep working:
--   * a BEFORE trigger keeps the two in step: setting role_id sets role to its base; changing role
--     alone moves role_id to that base's system role; an insert without role_id gets the system
--     role for its role.
--   * app.accept_invite passes the invite's role_id, so an invite can name a custom role.
--
-- A role in use can't be deleted: the API refuses with "give them another role first", and the
-- foreign key below refuses at commit whatever made the change (deferred, so deleting a whole
-- studio, which takes its roles and memberships together, still works).

-- A custom role sits at STAFF or MEMBER. Only the system OWNER role is OWNER-based (decision 5).
ALTER TABLE studio_role ADD CONSTRAINT studio_role_custom_not_owner CHECK (is_system OR base <> 'OWNER');
-- Names are unique in a studio, ignoring case, so a member list and an invite name one role
CREATE UNIQUE INDEX studio_role_name_idx ON studio_role (studio_id, lower(name));
-- Custom roles can be deleted (system roles never are; the API refuses)
GRANT DELETE ON studio_role TO app_rw;

ALTER TABLE studio_membership ADD COLUMN role_id uuid;

UPDATE studio_membership m
SET role_id = r.id
FROM studio_role r
WHERE r.studio_id = m.studio_id AND r.is_system AND r.base = m.role;

ALTER TABLE studio_membership ALTER COLUMN role_id SET NOT NULL;
ALTER TABLE studio_membership ADD CONSTRAINT studio_membership_role_fk
    FOREIGN KEY (studio_id, role_id) REFERENCES studio_role (studio_id, id)
    DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX studio_membership_role_idx ON studio_membership (studio_id, role_id);

-- An invite may name a role row, a custom role; without one it is for the system role of its
-- role column. Deleting a custom role withdraws its open invites first (API); an accepted invite
-- keeps its base.
ALTER TABLE studio_invite ADD COLUMN role_id uuid;
ALTER TABLE studio_invite ADD CONSTRAINT studio_invite_role_fk
    FOREIGN KEY (studio_id, role_id) REFERENCES studio_role (studio_id, id) ON DELETE SET NULL (role_id);

-- Making, renaming and deleting a role are logged too.
ALTER TABLE role_change_log DROP CONSTRAINT role_change_log_kind_check;
ALTER TABLE role_change_log ADD CONSTRAINT role_change_log_kind_check
    CHECK (kind IN ('ROLE_PERMISSIONS', 'MEMBER_DUTIES', 'MEMBER_ROLE', 'BILLING_CONTACT',
                    'ROLE_CREATED', 'ROLE_RENAMED', 'ROLE_DELETED'));

-- Keeps studio_membership.role equal to the base of its role row. SECURITY DEFINER so it finds the
-- studio's roles whatever the caller's binding (accepting an invite runs outside any studio). It
-- never raises: a row it can't match is left for the foreign key, NOT NULL or RLS to refuse.
CREATE FUNCTION app.membership_role_in_step()
    RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    v_base text;
BEGIN
    -- The role column changed on its own (as R8's owner changes and older code do): follow it
    IF TG_OP = 'UPDATE' AND NEW.role_id IS NOT DISTINCT FROM OLD.role_id
            AND NEW.role IS DISTINCT FROM OLD.role THEN
        NEW.role_id := NULL;
    END IF;

    IF NEW.role_id IS NULL THEN
        SELECT r.id INTO NEW.role_id
        FROM public.studio_role r
        WHERE r.studio_id = NEW.studio_id AND r.is_system AND r.base = NEW.role;
    ELSE
        SELECT r.base INTO v_base
        FROM public.studio_role r
        WHERE r.studio_id = NEW.studio_id AND r.id = NEW.role_id;
        IF FOUND THEN
            NEW.role := v_base;
        END IF;
    END IF;
    RETURN NEW;
END
$$;
REVOKE ALL ON FUNCTION app.membership_role_in_step() FROM PUBLIC;

CREATE TRIGGER studio_membership_role_in_step
    BEFORE INSERT OR UPDATE OF role, role_id, studio_id ON studio_membership
    FOR EACH ROW EXECUTE FUNCTION app.membership_role_in_step();

-- V40's owner check, now also after a change to role_id alone
DROP TRIGGER studio_membership_keeps_an_owner ON studio_membership;
CREATE CONSTRAINT TRIGGER studio_membership_keeps_an_owner
    AFTER UPDATE OF role, role_id, studio_id OR DELETE ON studio_membership
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION app.check_studio_owners();

-- As V3's, but the membership takes the invite's role row, so a custom role carries over. Someone
-- who is already a member keeps their current role.
CREATE OR REPLACE FUNCTION app.accept_invite(p_code text)
    RETURNS TABLE (studio_id uuid, studio_name text, role text)
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    v_account uuid := app.current_account_id();
    v_invite  public.studio_invite%ROWTYPE;
BEGIN
    IF v_account IS NULL THEN
        RAISE EXCEPTION 'No account is bound to this transaction' USING ERRCODE = '42501';
    END IF;

    SELECT * INTO v_invite
    FROM public.studio_invite i
    WHERE i.code_hash = sha256(convert_to(p_code, 'UTF8'))
    FOR UPDATE;

    IF NOT FOUND OR v_invite.accepted_at IS NOT NULL OR v_invite.expires_at <= now() THEN
        RETURN;
    END IF;

    -- role_id null means the system role for v_invite.role (the trigger above finds it)
    INSERT INTO public.studio_membership (id, studio_id, account_id, role, role_id, created_at, version)
    VALUES (uuidv7(), v_invite.studio_id, v_account, v_invite.role, v_invite.role_id, now(), 0)
    ON CONFLICT ON CONSTRAINT studio_membership_studio_account_key DO NOTHING;

    UPDATE public.studio_invite
    SET accepted_by = v_account, accepted_at = now()
    WHERE id = v_invite.id;

    RETURN QUERY
        SELECT s.id, s.name, m.role
        FROM public.studio s
        JOIN public.studio_membership m ON m.studio_id = s.id AND m.account_id = v_account
        WHERE s.id = v_invite.studio_id;
END
$$;
