-- The glaze book (docs/planning/14-glaze-book-plan.md). A recipe belongs either to the studio (a
-- shared bucket glaze, edited by staff) or to one member (private unless they share it).

CREATE TABLE glaze_recipe (
    id                  uuid          PRIMARY KEY,
    studio_id           uuid          NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    owner_kind          text          NOT NULL CHECK (owner_kind IN ('STUDIO', 'MEMBER')),
    owner_account_id    uuid          REFERENCES account (id),
    name                text          NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    notes               text          CHECK (char_length(notes) <= 4000),
    cone                text          CHECK (char_length(cone) <= 10),
    atmosphere          text          CHECK (atmosphere IN ('OXIDATION', 'REDUCTION', 'NEUTRAL')),
    food_surface        boolean       NOT NULL DEFAULT false,
    target_sg           numeric(4, 3) CHECK (target_sg BETWEEN 1 AND 2.5),
    shared_with_studio  boolean       NOT NULL DEFAULT false,
    -- Where it came from, so a re-import finds its earlier copy instead of adding another
    source              text          NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'GLAZECALC', 'GLAZE_VAULT')),
    source_ref          text          CHECK (char_length(source_ref) <= 200),
    source_rev          integer,
    source_updated_at   timestamptz,
    promoted_from       uuid          REFERENCES glaze_recipe (id) ON DELETE SET NULL,
    version             integer       NOT NULL DEFAULT 1,
    created_by          uuid          NOT NULL REFERENCES account (id),
    created_at          timestamptz   NOT NULL,
    updated_by          uuid          NOT NULL REFERENCES account (id),
    updated_at          timestamptz   NOT NULL,
    -- A studio glaze has no owner; a member recipe always has one
    CHECK ((owner_kind = 'STUDIO') = (owner_account_id IS NULL)),
    -- Only a member's own recipe can be shared; a studio glaze is everyone's already
    CHECK (owner_kind = 'MEMBER' OR NOT shared_with_studio)
);
CREATE INDEX glaze_recipe_studio_idx ON glaze_recipe (studio_id, owner_kind, name);
CREATE UNIQUE INDEX glaze_recipe_source_idx ON glaze_recipe (studio_id, owner_account_id, source, source_ref)
    WHERE source_ref IS NOT NULL;

CREATE TABLE glaze_line (
    id           uuid           PRIMARY KEY,
    studio_id    uuid           NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    recipe_id    uuid           NOT NULL REFERENCES glaze_recipe (id) ON DELETE CASCADE,
    position     integer        NOT NULL CHECK (position >= 0),
    kind         text           NOT NULL CHECK (kind IN ('BASE', 'ADDITION')),
    material     text           NOT NULL CHECK (char_length(material) BETWEEN 1 AND 80),
    amount       numeric(10, 4) NOT NULL CHECK (amount > 0),
    -- The amount exactly as the source wrote it, so "12.50 parts" isn't silently normalized
    amount_text  text           CHECK (char_length(amount_text) <= 30),
    unit         text           NOT NULL CHECK (unit IN ('PARTS', 'PERCENT', 'GRAMS')),
    -- Oxide analysis and LOI copied from an import, kept as sent and never edited
    chemistry    jsonb,
    UNIQUE (recipe_id, position)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON glaze_recipe, glaze_line TO app_rw;

-- Tenant RLS, plus an owner rule: a member's recipe is visible to the studio only if shared, and
-- only its owner may change it. Studio glazes are writable here; the app allows only staff.
ALTER TABLE glaze_recipe ENABLE ROW LEVEL SECURITY;
CREATE POLICY glaze_recipe_read ON glaze_recipe FOR SELECT TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND (owner_kind = 'STUDIO' OR shared_with_studio
                OR owner_account_id = (SELECT app.current_account_id())));
CREATE POLICY glaze_recipe_insert ON glaze_recipe FOR INSERT TO app_rw
    WITH CHECK (studio_id = (SELECT app.current_studio_id())
                AND (owner_kind = 'STUDIO' OR owner_account_id = (SELECT app.current_account_id())));
CREATE POLICY glaze_recipe_update ON glaze_recipe FOR UPDATE TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND (owner_kind = 'STUDIO' OR owner_account_id = (SELECT app.current_account_id())))
    WITH CHECK (studio_id = (SELECT app.current_studio_id())
                AND (owner_kind = 'STUDIO' OR owner_account_id = (SELECT app.current_account_id())));
CREATE POLICY glaze_recipe_delete ON glaze_recipe FOR DELETE TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND (owner_kind = 'STUDIO' OR owner_account_id = (SELECT app.current_account_id())));

-- Lines follow their recipe: readable when it is, writable when it's yours or the studio's
ALTER TABLE glaze_line ENABLE ROW LEVEL SECURITY;
CREATE POLICY glaze_line_read ON glaze_line FOR SELECT TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id));
CREATE POLICY glaze_line_write ON glaze_line FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id
                       AND (r.owner_kind = 'STUDIO' OR r.owner_account_id = (SELECT app.current_account_id()))))
    WITH CHECK (studio_id = (SELECT app.current_studio_id())
                AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id
                            AND (r.owner_kind = 'STUDIO' OR r.owner_account_id = (SELECT app.current_account_id()))));
