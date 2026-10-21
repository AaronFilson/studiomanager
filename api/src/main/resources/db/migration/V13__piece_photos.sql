-- Photos of pieces. The images live in Blob Storage, uploaded straight from the browser or phone
-- with a short-lived SAS URL; this table records which photos a piece has and whether each
-- upload was checked and finished (docs/adr/0012-direct-to-blob-photo-uploads.md).
--
-- Each photo is two blobs, a full image and a thumbnail, both JPEG made by the client. Their
-- names come from the ids ({studio}/{piece}/{photo}/full.jpg and thumb.jpg), so nothing here
-- can point a studio at another studio's blob.

CREATE TABLE piece_photo (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    piece_id    uuid        NOT NULL REFERENCES piece (id) ON DELETE CASCADE,
    -- PENDING from handing out the upload URLs until the API has checked both blobs
    status      text        NOT NULL CHECK (status IN ('PENDING', 'READY')),
    byte_size   integer     CHECK (byte_size > 0),
    uploaded_by uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    ready_at    timestamptz,
    CHECK ((status = 'READY') = (ready_at IS NOT NULL AND byte_size IS NOT NULL))
);

CREATE INDEX piece_photo_piece_idx ON piece_photo (piece_id, created_at);
CREATE INDEX piece_photo_pending_idx ON piece_photo (created_at) WHERE status = 'PENDING';

GRANT SELECT, INSERT, UPDATE, DELETE ON piece_photo TO app_rw;

ALTER TABLE piece_photo ENABLE ROW LEVEL SECURITY;

CREATE POLICY piece_photo_in_tenant ON piece_photo
    FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id()))
    WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Removes uploads that were started before p_before and never finished, in every studio, and
-- returns them so the caller can delete their blobs. Runs on a schedule with no tenant bound,
-- so it is SECURITY DEFINER (ADR 10); it touches nothing but pending photos.
CREATE FUNCTION app.take_abandoned_photo_uploads(p_before timestamptz)
    RETURNS TABLE (studio_id uuid, piece_id uuid, photo_id uuid)
    LANGUAGE sql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    DELETE FROM public.piece_photo p
    WHERE p.status = 'PENDING' AND p.created_at < p_before
    RETURNING p.studio_id, p.piece_id, p.id
$$;

REVOKE ALL ON FUNCTION app.take_abandoned_photo_uploads(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.take_abandoned_photo_uploads(timestamptz) TO app_rw;
