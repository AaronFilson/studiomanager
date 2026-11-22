-- Links external OAuth identities (such as the MCP OAuth 2.1 authorization server) to an existing
-- Studio Manager account (docs/planning/17-mcp-and-api-tokens-plan.md, step 5; ADR 26).
--
-- Linking requires proving live possession of both the primary Entra token and a signed JWT from
-- the trusted MCP authorization server, so an unverified subject claim can never be attached.

CREATE TABLE account_identity (
    id          uuid        PRIMARY KEY,
    account_id  uuid        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    issuer      text        NOT NULL CHECK (char_length(issuer) BETWEEN 1 AND 300
                                            AND issuer <> 'urn:studiomanager:system'),
    subject     text        NOT NULL CHECK (char_length(subject) BETWEEN 1 AND 300),
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT account_identity_issuer_subject_key UNIQUE (issuer, subject)
);

CREATE INDEX account_identity_account_idx ON account_identity (account_id);

GRANT SELECT, INSERT, DELETE ON account_identity TO app_rw;
