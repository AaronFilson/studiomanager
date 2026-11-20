-- Test-tile photos on glaze recipes (docs/planning/14-glaze-book-plan.md, step 5; ADR 12).
-- Direct-to-blob uploads under prefix glazes/{studio}/{recipe}/{photo}/full.jpg and thumb.jpg.

CREATE TABLE glaze_photo (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    recipe_id   uuid        NOT NULL REFERENCES glaze_recipe (id) ON DELETE CASCADE,
    status      text        NOT NULL CHECK (status IN ('PENDING', 'READY')),
    byte_size   integer     CHECK (byte_size > 0),
    uploaded_by uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    ready_at    timestamptz,
    CHECK ((status = 'READY') = (ready_at IS NOT NULL AND byte_size IS NOT NULL))
);

CREATE INDEX glaze_photo_recipe_idx ON glaze_photo (recipe_id, created_at);
CREATE INDEX glaze_photo_pending_idx ON glaze_photo (created_at) WHERE status = 'PENDING';

GRANT SELECT, INSERT, UPDATE, DELETE ON glaze_photo TO app_rw;

-- Photos follow their recipe's tenant + owner rules: readable when the recipe is, writable only
-- for studio glazes (checked in app for staff) or the member's own recipe.
ALTER TABLE glaze_photo ENABLE ROW LEVEL SECURITY;
CREATE POLICY glaze_photo_read ON glaze_photo FOR SELECT TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id));
CREATE POLICY glaze_photo_write ON glaze_photo FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())
           AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id
                       AND (r.owner_kind = 'STUDIO' OR r.owner_account_id = (SELECT app.current_account_id()))))
    WITH CHECK (studio_id = (SELECT app.current_studio_id())
                AND EXISTS (SELECT 1 FROM glaze_recipe r WHERE r.id = recipe_id
                            AND (r.owner_kind = 'STUDIO' OR r.owner_account_id = (SELECT app.current_account_id()))));

-- Removes abandoned glaze photo uploads started before p_before across every studio (ADR 10 & 16).
CREATE FUNCTION app.take_abandoned_glaze_photo_uploads(p_before timestamptz)
    RETURNS TABLE (studio_id uuid, recipe_id uuid, photo_id uuid)
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    PERFORM app.require_system_principal();
    RETURN QUERY
        DELETE FROM public.glaze_photo p
        WHERE p.status = 'PENDING' AND p.created_at < p_before
        RETURNING p.studio_id, p.recipe_id, p.id;
END
$$;

REVOKE ALL ON FUNCTION app.take_abandoned_glaze_photo_uploads(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.take_abandoned_glaze_photo_uploads(timestamptz) TO app_rw;
