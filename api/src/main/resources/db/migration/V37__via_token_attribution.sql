-- "Via token" attribution (docs/planning/17-mcp-and-api-tokens-plan.md, step 2). When a person
-- makes a change through one of their personal API tokens, by REST or an MCP tool, the history
-- keeps the person as the actor and also notes the token, so it reads "Olive via Sync script".
--
-- The app writes these histories itself, so the token id comes from the request, not a setting.
-- Tokens are revoked, never deleted, so a revoked token still names the change it made.

ALTER TABLE piece_event ADD COLUMN via_token_id uuid REFERENCES api_token (id);
ALTER TABLE maintenance_record ADD COLUMN via_token_id uuid REFERENCES api_token (id);
