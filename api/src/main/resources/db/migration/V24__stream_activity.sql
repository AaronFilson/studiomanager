-- Weekly activity, built by a consumer of the Kafka event stream (docs/planning/11-kafka-stream-plan.md).
-- Delivery is at least once, so each event's id is recorded in the same transaction as its totals,
-- and an event seen before changes nothing.

CREATE TABLE stream_event_seen (
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    event_id   uuid        NOT NULL,
    seen_at    timestamptz NOT NULL,
    PRIMARY KEY (studio_id, event_id)
);

CREATE TABLE weekly_activity (
    studio_id   uuid           NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- Monday, in the studio's time zone
    week_start  date           NOT NULL,
    metric      text           NOT NULL CHECK (metric IN ('FIRINGS', 'PIECES_FIRED', 'MEMBERS_JOINED', 'FEES')),
    -- The kiln for FIRINGS, the currency for FEES, '' otherwise
    dimension   text           NOT NULL DEFAULT '',
    count       integer        NOT NULL DEFAULT 0,
    amount      numeric(14, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (studio_id, week_start, metric, dimension)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON stream_event_seen, weekly_activity TO app_rw;

ALTER TABLE stream_event_seen ENABLE ROW LEVEL SECURITY;
CREATE POLICY stream_event_seen_in_tenant ON stream_event_seen FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
ALTER TABLE weekly_activity ENABLE ROW LEVEL SECURITY;
CREATE POLICY weekly_activity_in_tenant ON weekly_activity FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
