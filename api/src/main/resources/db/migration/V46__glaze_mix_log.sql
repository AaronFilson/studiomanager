-- Studio glaze mix log and specific gravity readings (docs/planning/14-glaze-book-plan.md, step 3).
-- Only studio glazes have mixes; member recipes stay private drafts unless promoted.

CREATE TABLE glaze_mix (
    id            uuid           PRIMARY KEY,
    studio_id     uuid           NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    recipe_id     uuid           NOT NULL REFERENCES glaze_recipe (id) ON DELETE CASCADE,
    mixed_by      uuid           NOT NULL REFERENCES account (id),
    mixed_at      timestamptz    NOT NULL,
    dry_weight_g  numeric(12, 2) NOT NULL CHECK (dry_weight_g > 0),
    water_g       numeric(12, 2) CHECK (water_g IS NULL OR water_g >= 0),
    notes         text           CHECK (char_length(notes) <= 2000)
);
CREATE INDEX glaze_mix_recipe_idx ON glaze_mix (recipe_id, mixed_at DESC, id DESC);

CREATE TABLE glaze_mix_reading (
    id                uuid          PRIMARY KEY,
    studio_id         uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    mix_id            uuid          NOT NULL REFERENCES glaze_mix (id) ON DELETE CASCADE,
    read_by           uuid          NOT NULL REFERENCES account (id),
    read_at           timestamptz   NOT NULL,
    specific_gravity  numeric(4, 3) NOT NULL CHECK (specific_gravity BETWEEN 1 AND 2.5),
    note              text          CHECK (char_length(note) <= 500)
);
CREATE INDEX glaze_mix_reading_mix_idx ON glaze_mix_reading (mix_id, read_at ASC, id ASC);

GRANT SELECT, INSERT, UPDATE, DELETE ON glaze_mix, glaze_mix_reading TO app_rw;

ALTER TABLE glaze_mix ENABLE ROW LEVEL SECURITY;
CREATE POLICY glaze_mix_in_tenant ON glaze_mix FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

ALTER TABLE glaze_mix_reading ENABLE ROW LEVEL SECURITY;
CREATE POLICY glaze_mix_reading_in_tenant ON glaze_mix_reading FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));
