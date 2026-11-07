-- The call log for MCP tool calls and API token calls, and what plan 17 step 1 left over: where a
-- token was last used from, and expiry after 90 days unused (docs/planning/20, section 6).

-- One row per MCP tool call (however the caller signed in) and per REST call made with a token.
-- Append-only: the app may insert and read, never change a row. Old rows go through the purge below.
CREATE TABLE api_call_log (
    id           uuid        PRIMARY KEY,
    studio_id    uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id   uuid        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    token_id     uuid        REFERENCES api_token (id) ON DELETE CASCADE,
    channel      text        NOT NULL CHECK (channel IN ('MCP', 'TOKEN')),
    target       text        NOT NULL CHECK (char_length(target) BETWEEN 1 AND 200),
    arguments    text        CHECK (char_length(arguments) <= 2048),
    outcome      text        NOT NULL CHECK (outcome IN ('OK', 'ERROR', 'LIMITED')),
    status       integer,
    duration_ms  integer     NOT NULL CHECK (duration_ms >= 0),
    created_at   timestamptz NOT NULL
);
CREATE INDEX api_call_log_studio_idx ON api_call_log (studio_id, created_at);
CREATE INDEX api_call_log_token_idx ON api_call_log (token_id, created_at) WHERE token_id IS NOT NULL;
CREATE INDEX api_call_log_created_idx ON api_call_log (created_at);

GRANT SELECT, INSERT ON api_call_log TO app_rw;

ALTER TABLE api_call_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY api_call_log_in_tenant ON api_call_log FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- The job that keeps the call log to 90 days
INSERT INTO account (id, issuer, subject, display_name, kind) VALUES
    ('5157e000-0000-4000-8000-000000000006', 'urn:studiomanager:system', 'call-log-purge',
     'System: call log purge', 'SYSTEM');

CREATE FUNCTION app.purge_api_call_log(before timestamptz)
    RETURNS integer
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    gone integer;
BEGIN
    PERFORM app.require_system_principal();
    DELETE FROM public.api_call_log WHERE created_at < before;
    GET DIAGNOSTICS gone = ROW_COUNT;
    RETURN gone;
END
$$;
REVOKE ALL ON FUNCTION app.purge_api_call_log(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.purge_api_call_log(timestamptz) TO app_rw;

-- Where a token was last used from, noted with its last use
ALTER TABLE api_token ADD COLUMN last_used_from text CHECK (char_length(last_used_from) <= 64);

-- A token now also stops working once it has gone 90 days without use, counting from its creation
-- if it was never used
DROP FUNCTION app.use_api_token(bytea, timestamptz);
CREATE FUNCTION app.use_api_token(hash bytea, now timestamptz, from_address text)
    RETURNS TABLE (token_id uuid, account_id uuid, studio_id uuid, scopes text[])
    LANGUAGE sql
    VOLATILE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    UPDATE public.api_token t
    SET last_used_at = CASE WHEN t.last_used_at IS NULL OR t.last_used_at < now - interval '1 minute'
                            THEN now ELSE t.last_used_at END,
        last_used_from = CASE WHEN t.last_used_at IS NULL OR t.last_used_at < now - interval '1 minute'
                              THEN left(from_address, 64) ELSE t.last_used_from END
    WHERE t.token_hash = hash
      AND t.revoked_at IS NULL
      AND t.expires_at > now
      AND coalesce(t.last_used_at, t.created_at) > now - interval '90 days'
    RETURNING t.id, t.account_id, t.studio_id, t.scopes
$$;
REVOKE ALL ON FUNCTION app.use_api_token(bytea, timestamptz, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.use_api_token(bytea, timestamptz, text) TO app_rw;
