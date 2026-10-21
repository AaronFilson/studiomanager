-- Who does what (duties), who may use what (certifications), the studio calendar (resources
-- and bookings), and the staffing roster (shifts and credited hours).
--
-- The research behind this: 17 of 84 studio sites gate equipment on an orientation, and none of
-- 16 labour programmes (monitors, work exchange, co-op hours) used any tool to schedule or credit
-- the hours.

-- Duties layered on top of a member's role: a technician, a studio monitor, a work-exchange
-- member, a co-op's volunteer officer, an instructor.
CREATE TABLE member_duty (
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id uuid        NOT NULL REFERENCES account (id),
    duty       text        NOT NULL CHECK (duty IN ('TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER', 'INSTRUCTOR')),
    granted_at timestamptz NOT NULL,
    PRIMARY KEY (studio_id, account_id, duty)
);

-- Kinds of sign-off a studio gives, such as "Kiln loading" or "Wheel room orientation".
CREATE TABLE certification_type (
    id           uuid    PRIMARY KEY,
    studio_id    uuid    NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    name         text    NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    -- Null means it never expires; otherwise it must be renewed this many months after granting
    valid_months integer CHECK (valid_months BETWEEN 1 AND 120),
    CONSTRAINT certification_type_studio_name_key UNIQUE (studio_id, name)
);

-- The latest grant of each certification to each member. Granting again renews it.
CREATE TABLE certification (
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    type_id    uuid        NOT NULL REFERENCES certification_type (id) ON DELETE CASCADE,
    account_id uuid        NOT NULL REFERENCES account (id),
    granted_by uuid        NOT NULL REFERENCES account (id),
    granted_at timestamptz NOT NULL,
    expires_at timestamptz,
    PRIMARY KEY (type_id, account_id)
);

-- Things that can be booked: each kiln, the wheel room (with a number of seats), the whole studio.
CREATE TABLE resource (
    id                     uuid    PRIMARY KEY,
    studio_id              uuid    NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    name                   text    NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    kind                   text    NOT NULL CHECK (kind IN ('KILN', 'WHEEL_ROOM', 'GLAZE_ROOM', 'STUDIO', 'EQUIPMENT', 'OTHER')),
    -- How many members can reserve it at once (8 wheels, say). Blocking bookings take all of it.
    capacity               integer NOT NULL CHECK (capacity BETWEEN 1 AND 200),
    required_certification uuid    REFERENCES certification_type (id) ON DELETE SET NULL,
    version                bigint  NOT NULL,
    CONSTRAINT resource_studio_name_key UNIQUE (studio_id, name)
);

CREATE TABLE booking (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    resource_id uuid        NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    -- RESERVATION takes one seat; every other kind blocks the whole resource
    kind        text        NOT NULL CHECK (kind IN ('RESERVATION', 'FIRING', 'MAINTENANCE', 'CLASS', 'EVENT', 'RENTAL', 'CLOSURE')),
    title       text        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 120),
    starts_at   timestamptz NOT NULL,
    ends_at     timestamptz NOT NULL,
    account_id  uuid        REFERENCES account (id),
    created_by  uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    CHECK (ends_at > starts_at),
    CHECK (ends_at - starts_at <= interval '14 days')
);

CREATE INDEX booking_resource_time_idx ON booking (resource_id, starts_at, ends_at);
CREATE INDEX booking_studio_time_idx ON booking (studio_id, starts_at);

-- A slot someone must cover, such as "Monitor, Tuesday 6–9pm".
CREATE TABLE shift (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    title       text        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 120),
    duty        text        CHECK (duty IN ('TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER', 'INSTRUCTOR')),
    starts_at   timestamptz NOT NULL,
    ends_at     timestamptz NOT NULL,
    account_id  uuid        REFERENCES account (id),
    -- Set when staff confirm it was worked; the hours are then credited
    worked_at   timestamptz,
    created_by  uuid        NOT NULL REFERENCES account (id),
    version     bigint      NOT NULL,
    CHECK (ends_at > starts_at),
    CHECK (ends_at - starts_at <= interval '24 hours'),
    CHECK (worked_at IS NULL OR account_id IS NOT NULL)
);

CREATE INDEX shift_studio_time_idx ON shift (studio_id, starts_at);

-- Credited hours, for work-exchange trades, co-op hour requirements and volunteer records.
CREATE TABLE hours_entry (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id  uuid        NOT NULL REFERENCES account (id),
    shift_id    uuid        UNIQUE REFERENCES shift (id) ON DELETE SET NULL,
    minutes     integer     NOT NULL CHECK (minutes BETWEEN -6000 AND 6000 AND minutes <> 0),
    note        text        CHECK (char_length(note) <= 200),
    credited_by uuid        NOT NULL REFERENCES account (id),
    credited_at timestamptz NOT NULL
);

CREATE INDEX hours_entry_account_idx ON hours_entry (studio_id, account_id, credited_at);

GRANT SELECT, INSERT, DELETE ON member_duty TO app_rw;
GRANT SELECT, INSERT, UPDATE ON certification_type TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON certification TO app_rw;
GRANT SELECT, INSERT, UPDATE ON resource TO app_rw;
GRANT SELECT, INSERT, DELETE ON booking TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON shift TO app_rw;
GRANT SELECT, INSERT ON hours_entry TO app_rw;

ALTER TABLE member_duty ENABLE ROW LEVEL SECURITY;
ALTER TABLE certification_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE certification ENABLE ROW LEVEL SECURITY;
ALTER TABLE resource ENABLE ROW LEVEL SECURITY;
ALTER TABLE booking ENABLE ROW LEVEL SECURITY;
ALTER TABLE shift ENABLE ROW LEVEL SECURITY;
ALTER TABLE hours_entry ENABLE ROW LEVEL SECURITY;

CREATE POLICY member_duty_in_tenant ON member_duty FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY certification_type_in_tenant ON certification_type FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY certification_in_tenant ON certification FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY resource_in_tenant ON resource FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY booking_in_tenant ON booking FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY shift_in_tenant ON shift FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY hours_entry_in_tenant ON hours_entry FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
