-- Photos on maintenance records: the cracked element, the cleaned trap, the new relay in place
-- (docs/planning/06-wave2-maintenance-plan.md, next slices). They upload the same way as piece
-- photos (ADR 12): straight to Blob Storage with signed URLs, then checked by the API.
--
-- Blob names come from ids, under their own prefix:
-- maintenance/{studio}/{record}/{photo}/full.jpg and thumb.jpg.

CREATE TABLE record_photo (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    record_id   uuid        NOT NULL REFERENCES maintenance_record (id) ON DELETE CASCADE,
    status      text        NOT NULL CHECK (status IN ('PENDING', 'READY')),
    byte_size   integer     CHECK (byte_size > 0),
    uploaded_by uuid        NOT NULL REFERENCES account (id),
    created_at  timestamptz NOT NULL,
    ready_at    timestamptz,
    CHECK ((status = 'READY') = (ready_at IS NOT NULL AND byte_size IS NOT NULL))
);

CREATE INDEX record_photo_record_idx ON record_photo (record_id, created_at);
CREATE INDEX record_photo_pending_idx ON record_photo (created_at) WHERE status = 'PENDING';

GRANT SELECT, INSERT, UPDATE, DELETE ON record_photo TO app_rw;

ALTER TABLE record_photo ENABLE ROW LEVEL SECURITY;

CREATE POLICY record_photo_in_tenant ON record_photo FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- Like app.take_abandoned_photo_uploads (V13), for record photos: removes uploads started before
-- p_before and never finished, in every studio, and returns them so their blobs can be deleted.
CREATE FUNCTION app.take_abandoned_record_photo_uploads(p_before timestamptz)
    RETURNS TABLE (studio_id uuid, record_id uuid, photo_id uuid)
    LANGUAGE sql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    DELETE FROM public.record_photo p
    WHERE p.status = 'PENDING' AND p.created_at < p_before
    RETURNING p.studio_id, p.record_id, p.id
$$;

REVOKE ALL ON FUNCTION app.take_abandoned_record_photo_uploads(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.take_abandoned_record_photo_uploads(timestamptz) TO app_rw;
