-- Members reporting a problem with equipment or the building, usually from their phone at the
-- wheel (docs/planning/06-wave2-maintenance-plan.md, next slices). A report isn't work done, so
-- it isn't a maintenance record: staff resolve it, and log any repair as a record as usual.

CREATE TABLE problem_report (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- Null for "somewhere else"; kept as a report if the asset stops being tracked
    asset_id    uuid        REFERENCES asset (id) ON DELETE SET NULL,
    description text        NOT NULL CHECK (char_length(description) BETWEEN 1 AND 1000),
    reported_by uuid        NOT NULL REFERENCES account (id),
    reported_at timestamptz NOT NULL,
    resolved_by uuid        REFERENCES account (id),
    resolved_at timestamptz,
    resolution  text        CHECK (char_length(resolution) <= 1000),
    version     bigint      NOT NULL,
    CHECK ((resolved_at IS NULL) = (resolved_by IS NULL))
);

CREATE INDEX problem_report_open_idx ON problem_report (studio_id, reported_at) WHERE resolved_at IS NULL;

GRANT SELECT, INSERT, UPDATE ON problem_report TO app_rw;

ALTER TABLE problem_report ENABLE ROW LEVEL SECURITY;

CREATE POLICY problem_report_in_tenant ON problem_report FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

ALTER TABLE notification DROP CONSTRAINT notification_kind_check;
ALTER TABLE notification ADD CONSTRAINT notification_kind_check
    CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE', 'MAINTENANCE_DUE', 'PROBLEM_REPORTED'));
