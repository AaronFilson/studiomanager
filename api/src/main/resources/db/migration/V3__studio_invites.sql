-- Invitations into a studio. A staff member creates one, and the invitee accepts it by code.
--
-- Only a SHA-256 hash of each code is stored, so a leaked table can't be used to join studios.
-- The invitee isn't a member yet, so row-level security hides the invite from them. Accepting
-- therefore goes through app.accept_invite: a SECURITY DEFINER function that runs as the
-- schema owner and does exactly one thing, under a pinned search_path.

CREATE TABLE studio_invite (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    code_hash   bytea       NOT NULL UNIQUE,
    role        text        NOT NULL CHECK (role IN ('OWNER', 'STAFF', 'MEMBER')),
    created_by  uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    expires_at  timestamptz NOT NULL,
    accepted_by uuid        REFERENCES account (id),
    accepted_at timestamptz,
    CHECK ((accepted_by IS NULL) = (accepted_at IS NULL))
);

CREATE INDEX studio_invite_studio_id_idx ON studio_invite (studio_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON studio_invite TO app_rw;

ALTER TABLE studio_invite ENABLE ROW LEVEL SECURITY;

CREATE POLICY invite_in_tenant ON studio_invite
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Accepts an open, unexpired invite for the signed-in account (app.account_id) and returns the
-- studio and the resulting role. Returns no row when the code is unknown, used or expired. Someone
-- who is already a member keeps their current role.
CREATE FUNCTION app.accept_invite(p_code text)
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

    INSERT INTO public.studio_membership (id, studio_id, account_id, role, created_at, version)
    VALUES (uuidv7(),v_invite.studio_id, v_account, v_invite.role, now(), 0)
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

REVOKE ALL ON FUNCTION app.accept_invite(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.accept_invite(text) TO app_rw;
