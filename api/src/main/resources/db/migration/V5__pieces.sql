-- Members' pieces, from drop-off to pickup, with an append-only timeline.
--
-- Measurements keep the value and unit they were entered in, so they can be shown and audited
-- exactly as taken; code converts them. A piece's fee is a snapshot taken at intake: the amount,
-- the schedule version, the tax category and a human-readable breakdown.

CREATE TABLE piece_policy (
    studio_id  uuid        PRIMARY KEY REFERENCES studio (id) ON DELETE CASCADE,
    hold_days  integer     NOT NULL CHECK (hold_days BETWEEN 1 AND 365),
    updated_at timestamptz NOT NULL
);

-- Each studio numbers its pieces 1, 2, 3, ... for labels and conversation ("piece 42").
CREATE TABLE piece_counter (
    studio_id   uuid    PRIMARY KEY REFERENCES studio (id) ON DELETE CASCADE,
    last_number integer NOT NULL
);

CREATE TABLE piece (
    id                   uuid          PRIMARY KEY,
    studio_id            uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    number               integer       NOT NULL,
    maker_account_id     uuid          NOT NULL REFERENCES account (id),
    description          text          CHECK (char_length(description) <= 200),
    clay_body            text          CHECK (char_length(clay_body) <= 80),
    maker_mark           text          CHECK (char_length(maker_mark) <= 80),
    length_value         numeric(9,2),
    width_value          numeric(9,2),
    height_value         numeric(9,2),
    length_unit          text          CHECK (length_unit IN ('MILLIMETRE', 'CENTIMETRE', 'INCH')),
    weight_value         numeric(9,3),
    weight_unit          text          CHECK (weight_unit IN ('GRAM', 'KILOGRAM', 'POUND', 'OUNCE')),
    stage                text          NOT NULL CHECK (stage IN ('RECEIVED', 'BISQUE_FIRED', 'GLAZED', 'READY',
                                                                 'PICKED_UP', 'UNCLAIMED', 'DISPOSED')),
    fee_amount           numeric(12,2) NOT NULL CHECK (fee_amount >= 0),
    fee_currency         text          NOT NULL CHECK (fee_currency ~ '^[A-Z]{3}$'),
    fee_schedule_version integer,
    fee_tax_category     text,
    fee_breakdown        text          NOT NULL,
    received_at          timestamptz   NOT NULL,
    ready_at             timestamptz,
    hold_until           timestamptz,
    created_by           uuid          NOT NULL REFERENCES account (id),
    version              bigint        NOT NULL,
    CONSTRAINT piece_studio_number_key UNIQUE (studio_id, number),
    CONSTRAINT piece_dimensions_complete CHECK (
        (length_value IS NULL AND width_value IS NULL AND height_value IS NULL AND length_unit IS NULL)
        OR (length_value > 0 AND width_value > 0 AND height_value > 0 AND length_unit IS NOT NULL)),
    CONSTRAINT piece_weight_complete CHECK ((weight_value IS NULL) = (weight_unit IS NULL) AND
                                            (weight_value IS NULL OR weight_value > 0)),
    CONSTRAINT piece_ready_has_hold CHECK (stage NOT IN ('READY', 'UNCLAIMED') OR hold_until IS NOT NULL)
);

CREATE INDEX piece_studio_stage_idx ON piece (studio_id, stage);
CREATE INDEX piece_maker_idx ON piece (studio_id, maker_account_id);
CREATE INDEX piece_hold_until_idx ON piece (hold_until) WHERE stage = 'READY';

CREATE TABLE piece_event (
    id               uuid        PRIMARY KEY,
    studio_id        uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    piece_id         uuid        NOT NULL REFERENCES piece (id) ON DELETE CASCADE,
    from_stage       text,
    to_stage         text        NOT NULL,
    note             text        CHECK (char_length(note) <= 500),
    -- Null when the system made the change (for example, a hold period running out)
    actor_account_id uuid        REFERENCES account (id),
    occurred_at      timestamptz NOT NULL
);

CREATE INDEX piece_event_piece_idx ON piece_event (piece_id, occurred_at);

GRANT SELECT, INSERT, UPDATE ON piece_policy, piece_counter, piece TO app_rw;
GRANT SELECT, INSERT ON piece_event TO app_rw;

ALTER TABLE piece_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE piece_counter ENABLE ROW LEVEL SECURITY;
ALTER TABLE piece ENABLE ROW LEVEL SECURITY;
ALTER TABLE piece_event ENABLE ROW LEVEL SECURITY;

CREATE POLICY piece_policy_in_tenant ON piece_policy
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

CREATE POLICY piece_counter_in_tenant ON piece_counter
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

CREATE POLICY piece_in_tenant ON piece
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

CREATE POLICY piece_event_in_tenant ON piece_event
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Moves every READY piece whose hold has run out to UNCLAIMED, in every studio, and records why.
-- Runs on a schedule with no tenant bound, so it is SECURITY DEFINER; it changes nothing else.
CREATE FUNCTION app.mark_unclaimed_pieces(p_now timestamptz)
    RETURNS integer
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    v_count integer;
BEGIN
    WITH expired AS (
        UPDATE public.piece
        SET stage = 'UNCLAIMED', version = version + 1
        WHERE stage = 'READY' AND hold_until <= p_now
        RETURNING id, studio_id
    )
    INSERT INTO public.piece_event (id, studio_id, piece_id, from_stage, to_stage, note, actor_account_id, occurred_at)
    SELECT uuidv7(), e.studio_id, e.id, 'READY', 'UNCLAIMED', 'Hold period ended', NULL, p_now
    FROM expired e;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END
$$;

REVOKE ALL ON FUNCTION app.mark_unclaimed_pieces(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.mark_unclaimed_pieces(timestamptz) TO app_rw;
