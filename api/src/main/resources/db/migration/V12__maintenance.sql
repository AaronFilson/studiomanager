-- Maintenance of equipment and the building: assets, the recurring tasks on them, and a record of
-- the work done (docs/planning/06-wave2-maintenance-plan.md).
--
-- This replaces the kiln's single "every N firings" threshold and its log. Each kiln becomes an
-- asset, its threshold becomes a task, and its log becomes records. Completing a task restarts
-- only that task's clock, where logging anything used to zero the kiln's whole count.

CREATE TABLE asset (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    kind        text        NOT NULL CHECK (kind IN ('KILN', 'WHEEL', 'PUG_MILL', 'SLAB_ROLLER', 'EXTRUDER', 'CLAY_TRAP',
                                                     'SPRAY_BOOTH', 'VENTILATION', 'BUILDING', 'SAFETY', 'OTHER')),
    -- A kiln asset takes its name and down state from the kiln, so it has neither of its own
    kiln_id     uuid        UNIQUE REFERENCES kiln (id) ON DELETE CASCADE,
    name        text        CHECK (char_length(name) BETWEEN 1 AND 80),
    location    text        CHECK (char_length(location) <= 80),
    notes       text        CHECK (char_length(notes) <= 1000),
    down_reason text        CHECK (char_length(down_reason) <= 200),
    archived    boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL,
    version     bigint      NOT NULL,
    CHECK ((kiln_id IS NOT NULL) = (kind = 'KILN')),
    CHECK ((kiln_id IS NULL) = (name IS NOT NULL)),
    CHECK (kiln_id IS NULL OR down_reason IS NULL)
);

CREATE UNIQUE INDEX asset_studio_name_key ON asset (studio_id, lower(name)) WHERE name IS NOT NULL AND NOT archived;

CREATE TABLE maintenance_task (
    id                uuid        PRIMARY KEY,
    studio_id         uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    asset_id          uuid        NOT NULL REFERENCES asset (id) ON DELETE CASCADE,
    title             text        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 120),
    -- Lines to tick, such as "Vacuum the element grooves"
    checklist         text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(checklist) <= 30),
    every_days        integer     CHECK (every_days BETWEEN 1 AND 3650),
    every_firings     integer     CHECK (every_firings BETWEEN 1 AND 10000),
    assigned_duty     text        CHECK (assigned_duty IN ('TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER',
                                                           'INSTRUCTOR')),
    assigned_to       uuid        REFERENCES account (id),
    -- The clock: when it was last done (or set up), and the kiln's total firings then
    last_done_at      timestamptz NOT NULL,
    last_done_firings integer     NOT NULL DEFAULT 0 CHECK (last_done_firings >= 0),
    archived          boolean     NOT NULL DEFAULT false,
    created_at        timestamptz NOT NULL,
    version           bigint      NOT NULL,
    CHECK (every_days IS NOT NULL OR every_firings IS NOT NULL),
    CHECK (assigned_duty IS NULL OR assigned_to IS NULL)
);

CREATE INDEX maintenance_task_asset_idx ON maintenance_task (asset_id) WHERE NOT archived;

CREATE TABLE maintenance_record (
    id        uuid          PRIMARY KEY,
    studio_id uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    asset_id  uuid          NOT NULL REFERENCES asset (id) ON DELETE CASCADE,
    -- Null for a repair outside any task
    task_id   uuid          REFERENCES maintenance_task (id) ON DELETE SET NULL,
    note      text          CHECK (char_length(note) <= 1000),
    cost      numeric(12,2) CHECK (cost >= 0),
    checked   text[]        NOT NULL DEFAULT '{}',
    -- For kilns: total firings when the work was done, so "every N firings" can be audited
    at_firing integer,
    done_by   uuid          NOT NULL REFERENCES account (id),
    done_at   timestamptz   NOT NULL
);

CREATE INDEX maintenance_record_asset_idx ON maintenance_record (asset_id, done_at DESC);

-- --- Moving the kilns over -------------------------------------------------------------------

INSERT INTO asset (id, studio_id, kind, kiln_id, created_at, version)
SELECT uuidv7(), k.studio_id, 'KILN', k.id, k.created_at, 0
FROM kiln k;

-- The old threshold becomes a task whose clock stands where the kiln's counter stood
INSERT INTO maintenance_task (id, studio_id, asset_id, title, every_firings, last_done_at, last_done_firings,
                              created_at, version)
SELECT uuidv7(), k.studio_id, a.id, 'Kiln maintenance', k.maintenance_every_firings,
       coalesce((SELECT max(m.performed_at) FROM kiln_maintenance m WHERE m.kiln_id = k.id), k.created_at),
       k.total_firings - k.firings_since_maintenance, k.created_at, 0
FROM kiln k
JOIN asset a ON a.kiln_id = k.id
WHERE k.maintenance_every_firings IS NOT NULL;

INSERT INTO maintenance_record (id, studio_id, asset_id, task_id, note, cost, at_firing, done_by, done_at)
SELECT m.id, m.studio_id, a.id, t.id, m.note, m.cost, m.at_firing, m.performed_by, m.performed_at
FROM kiln_maintenance m
JOIN asset a ON a.kiln_id = m.kiln_id
LEFT JOIN maintenance_task t ON t.asset_id = a.id;

DROP TABLE kiln_maintenance;
ALTER TABLE kiln DROP COLUMN maintenance_every_firings, DROP COLUMN firings_since_maintenance;

-- --- Access ------------------------------------------------------------------------------------

GRANT SELECT, INSERT, UPDATE ON asset, maintenance_task TO app_rw;
GRANT SELECT, INSERT ON maintenance_record TO app_rw;

ALTER TABLE asset ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_task ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_record ENABLE ROW LEVEL SECURITY;

CREATE POLICY asset_in_tenant ON asset FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY maintenance_task_in_tenant ON maintenance_task FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY maintenance_record_in_tenant ON maintenance_record FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
