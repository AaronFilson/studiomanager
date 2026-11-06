-- Personal API tokens (docs/planning/17-mcp-and-api-tokens-plan.md). Staff create them to let a
-- script or an AI assistant act as them, in one studio, within the token's scopes. Only a SHA-256
-- of the token is stored; the token itself is shown once.

CREATE TABLE api_token (
    id            uuid        PRIMARY KEY,
    studio_id     uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id    uuid        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    name          text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 80),
    token_hash    bytea       NOT NULL UNIQUE CHECK (length(token_hash) = 32),
    last_four     text        NOT NULL CHECK (char_length(last_four) = 4),
    scopes        text[]      NOT NULL CHECK (cardinality(scopes) > 0),
    created_at    timestamptz NOT NULL,
    expires_at    timestamptz NOT NULL,
    last_used_at  timestamptz,
    revoked_at    timestamptz,
    CHECK (expires_at > created_at)
);
CREATE INDEX api_token_studio_idx ON api_token (studio_id, account_id);

GRANT SELECT, INSERT, UPDATE ON api_token TO app_rw;

ALTER TABLE api_token ENABLE ROW LEVEL SECURITY;
CREATE POLICY api_token_in_tenant ON api_token FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Signing in with a token happens before any studio is bound, so the lookup crosses tenants
-- (ADR 10). It's keyed by the hash of a 256-bit secret, returns only a live token, and notes
-- when it was used, at most once a minute.
CREATE FUNCTION app.use_api_token(hash bytea, now timestamptz)
    RETURNS TABLE (token_id uuid, account_id uuid, studio_id uuid, scopes text[])
    LANGUAGE sql
    VOLATILE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    UPDATE public.api_token t
    SET last_used_at = CASE WHEN t.last_used_at IS NULL OR t.last_used_at < now - interval '1 minute'
                            THEN now ELSE t.last_used_at END
    WHERE t.token_hash = hash AND t.revoked_at IS NULL AND t.expires_at > now
    RETURNING t.id, t.account_id, t.studio_id, t.scopes
$$;
REVOKE ALL ON FUNCTION app.use_api_token(bytea, timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.use_api_token(bytea, timestamptz) TO app_rw;
