-- The studio's own AI key, for reading invoices (docs/planning/16-invoice-ocr-plan.md). One per
-- studio, set by the owner. The key is AES-GCM encrypted with the studio id as associated data,
-- so a row copied into another studio won't decrypt. It's never returned or logged.

CREATE TABLE ai_provider_key (
    studio_id              uuid        PRIMARY KEY REFERENCES studio (id) ON DELETE CASCADE,
    provider               text        NOT NULL CHECK (provider IN ('ANTHROPIC')),
    key_ciphertext         bytea       NOT NULL,
    last_four              text        NOT NULL CHECK (char_length(last_four) = 4),
    status                 text        NOT NULL CHECK (status IN ('UNTESTED', 'WORKING', 'REJECTED')),
    status_reason          text        CHECK (char_length(status_reason) <= 300),
    checked_at             timestamptz,
    -- Whether a failed read may fall back to the platform's Google Document AI (plan 16, step 3)
    allow_google_fallback  boolean     NOT NULL DEFAULT false,
    set_by                 uuid        NOT NULL REFERENCES account (id),
    set_at                 timestamptz NOT NULL
);

GRANT SELECT, INSERT, UPDATE, DELETE ON ai_provider_key TO app_rw;

ALTER TABLE ai_provider_key ENABLE ROW LEVEL SECURITY;
CREATE POLICY ai_provider_key_in_tenant ON ai_provider_key FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
