-- Pending actions (docs/planning/17-mcp-and-api-tokens-plan.md, step 4). An assistant connected
-- over MCP proposes a change; it waits here until a staff member approves or rejects it in the app.
-- Approving runs the normal service call as the person who approved, so all the usual checks apply.
--
-- Each kind's fields are their own typed columns rather than a JSON payload: the database checks
-- them, and Hibernate maps them without a JSON format mapper. A new kind adds its columns and a
-- line in the kind check below.

CREATE TABLE pending_action (
    id                 uuid        PRIMARY KEY,
    studio_id          uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    kind               text        NOT NULL CHECK (kind IN ('PURCHASE_REQUEST', 'MAINTENANCE_TASK')),
    status             text        NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- Why the assistant suggests it, in its own words: shown to staff as plain text
    reason             text        CHECK (char_length(reason) <= 500),
    -- The item's or asset's name when it was proposed, so the review queue reads without a lookup
    -- per row
    subject_name       text        NOT NULL CHECK (char_length(subject_name) BETWEEN 1 AND 120),

    -- PURCHASE_REQUEST: ask for more of a supply item
    item_id            uuid        REFERENCES supply_item (id) ON DELETE CASCADE,
    quantity           numeric(12, 3) CHECK (quantity > 0),
    note               text        CHECK (char_length(note) <= 200),

    -- MAINTENANCE_TASK: a new recurring task on an asset
    asset_id           uuid        REFERENCES asset (id) ON DELETE CASCADE,
    title              text        CHECK (char_length(title) BETWEEN 1 AND 120),
    every_days         integer     CHECK (every_days BETWEEN 1 AND 3650),
    every_firings      integer     CHECK (every_firings BETWEEN 1 AND 10000),

    -- Who proposed it, and through which token. The person is the token's owner; the token is
    -- null when the assistant signed in some other way
    proposed_by        uuid        NOT NULL REFERENCES account (id),
    proposed_via_token uuid        REFERENCES api_token (id) ON DELETE SET NULL,
    created_at         timestamptz NOT NULL,

    -- Who decided, and what approving made (the purchase request's or the task's id)
    decided_by         uuid        REFERENCES account (id),
    decided_at         timestamptz,
    decision_note      text        CHECK (char_length(decision_note) <= 500),
    result_id          uuid,
    version            bigint      NOT NULL,

    CHECK (kind <> 'PURCHASE_REQUEST'
           OR (item_id IS NOT NULL AND quantity IS NOT NULL AND asset_id IS NULL AND title IS NULL
               AND every_days IS NULL AND every_firings IS NULL)),
    CHECK (kind <> 'MAINTENANCE_TASK'
           OR (asset_id IS NOT NULL AND title IS NOT NULL AND item_id IS NULL AND quantity IS NULL
               AND note IS NULL AND (every_days IS NOT NULL OR every_firings IS NOT NULL))),
    CHECK ((status = 'PENDING') = (decided_by IS NULL)),
    CHECK ((status = 'PENDING') = (decided_at IS NULL)),
    CHECK (status = 'APPROVED' OR result_id IS NULL)
);
CREATE INDEX pending_action_open_idx ON pending_action (studio_id, created_at) WHERE status = 'PENDING';

GRANT SELECT, INSERT, UPDATE ON pending_action TO app_rw;

ALTER TABLE pending_action ENABLE ROW LEVEL SECURITY;
CREATE POLICY pending_action_in_tenant ON pending_action FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
