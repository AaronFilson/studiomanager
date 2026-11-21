-- Live Glazecalc OAuth connection per member per studio (ADR 17).
-- Tokens are AES-256-GCM encrypted at rest via SecretCipher bound to the connection id.

CREATE TABLE glazecalc_connection (
    id                    uuid        PRIMARY KEY,
    studio_id             uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id            uuid        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    access_token_enc      bytea       NOT NULL,
    refresh_token_enc     bytea,
    expires_at            timestamptz,
    connected_at          timestamptz NOT NULL,
    last_synced_at        timestamptz,
    remote_account_label  text        CHECK (char_length(remote_account_label) <= 200),
    UNIQUE (studio_id, account_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON glazecalc_connection TO app_rw;

-- Strict tenant + owner RLS: only the member who connected it can read, update, or remove it.
ALTER TABLE glazecalc_connection ENABLE ROW LEVEL SECURITY;
CREATE POLICY glazecalc_connection_owner ON glazecalc_connection FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND account_id = (SELECT app.current_account_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id())
                AND account_id = (SELECT app.current_account_id()));
