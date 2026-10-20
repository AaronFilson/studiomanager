-- Kilns, firings, the pieces in each firing, kiln maintenance, and members' notifications.
--
-- A firing is a load: built from queued pieces (LOADING), fired (FIRING), then unloaded
-- (COMPLETE), which moves every piece on and counts the firing against the kiln's maintenance
-- threshold. A kiln can be marked down, with a reason, so nobody loads it.

CREATE TABLE kiln (
    id                         uuid        PRIMARY KEY,
    studio_id                  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    name                       text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    site                       text        CHECK (char_length(site) <= 60),
    kind                       text        NOT NULL CHECK (kind IN ('ELECTRIC', 'GAS', 'WOOD', 'RAKU', 'OTHER')),
    max_pieces_per_member      integer     CHECK (max_pieces_per_member BETWEEN 1 AND 500),
    maintenance_every_firings  integer     CHECK (maintenance_every_firings BETWEEN 1 AND 10000),
    firings_since_maintenance  integer     NOT NULL DEFAULT 0 CHECK (firings_since_maintenance >= 0),
    total_firings              integer     NOT NULL DEFAULT 0 CHECK (total_firings >= 0),
    down_reason                text        CHECK (char_length(down_reason) <= 200),
    created_at                 timestamptz NOT NULL,
    version                    bigint      NOT NULL,
    CONSTRAINT kiln_studio_name_key UNIQUE (studio_id, name)
);

CREATE TABLE firing (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    kiln_id     uuid        NOT NULL REFERENCES kiln (id) ON DELETE CASCADE,
    kind        text        NOT NULL CHECK (kind IN ('BISQUE', 'GLAZE')),
    cone        text        CHECK (char_length(cone) <= 10),
    status      text        NOT NULL CHECK (status IN ('LOADING', 'FIRING', 'COMPLETE', 'CANCELLED')),
    notes       text        CHECK (char_length(notes) <= 1000),
    created_by  uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    started_at  timestamptz,
    finished_at timestamptz,
    version     bigint      NOT NULL
);

CREATE INDEX firing_kiln_idx ON firing (kiln_id, created_at DESC);

-- At most one open firing per kiln: you can't load a kiln that is already loading or firing
CREATE UNIQUE INDEX firing_one_open_per_kiln ON firing (kiln_id) WHERE status IN ('LOADING', 'FIRING');

CREATE TABLE firing_piece (
    firing_id uuid NOT NULL REFERENCES firing (id) ON DELETE CASCADE,
    piece_id  uuid NOT NULL REFERENCES piece (id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    -- Copied from the firing so the index below can stop one piece being in two open loads
    open      boolean NOT NULL,
    PRIMARY KEY (firing_id, piece_id)
);

CREATE UNIQUE INDEX firing_piece_one_open_load ON firing_piece (piece_id) WHERE open;

CREATE TABLE kiln_maintenance (
    id           uuid          PRIMARY KEY,
    studio_id    uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    kiln_id      uuid          NOT NULL REFERENCES kiln (id) ON DELETE CASCADE,
    note         text          NOT NULL CHECK (char_length(note) BETWEEN 1 AND 500),
    cost         numeric(12,2) CHECK (cost >= 0),
    -- The kiln's total firings when the work was done, so "every N firings" can be audited
    at_firing    integer       NOT NULL,
    performed_by uuid          NOT NULL REFERENCES account (id),
    performed_at timestamptz   NOT NULL
);

CREATE INDEX kiln_maintenance_kiln_idx ON kiln_maintenance (kiln_id, performed_at DESC);

CREATE TABLE notification (
    id         uuid        PRIMARY KEY,
    studio_id  uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    account_id uuid        NOT NULL REFERENCES account (id),
    kind       text        NOT NULL CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE')),
    title      text        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 120),
    body       text        NOT NULL CHECK (char_length(body) <= 2000),
    created_at timestamptz NOT NULL,
    read_at    timestamptz
);

CREATE INDEX notification_account_idx ON notification (studio_id, account_id, created_at DESC);

GRANT SELECT, INSERT, UPDATE, DELETE ON kiln TO app_rw;
GRANT SELECT, INSERT, UPDATE ON firing, firing_piece TO app_rw;
GRANT DELETE ON firing_piece TO app_rw;
GRANT SELECT, INSERT ON kiln_maintenance TO app_rw;
GRANT SELECT, INSERT, UPDATE ON notification TO app_rw;

ALTER TABLE kiln ENABLE ROW LEVEL SECURITY;
ALTER TABLE firing ENABLE ROW LEVEL SECURITY;
ALTER TABLE firing_piece ENABLE ROW LEVEL SECURITY;
ALTER TABLE kiln_maintenance ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification ENABLE ROW LEVEL SECURITY;

CREATE POLICY kiln_in_tenant ON kiln FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY firing_in_tenant ON firing FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY firing_piece_in_tenant ON firing_piece FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY kiln_maintenance_in_tenant ON kiln_maintenance FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
CREATE POLICY notification_in_tenant ON notification FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
