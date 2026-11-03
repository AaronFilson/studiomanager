-- Outbound webhooks (docs/planning/10-webhooks-plan.md): a studio's staff point an HTTPS URL, such as
-- a Zapier catch hook, at the events they want. Each event is written in the transaction that caused
-- it, then signed and POSTed to every endpoint subscribed to it, retrying with backoff.

CREATE TABLE webhook_endpoint (
    id                     uuid        PRIMARY KEY,
    studio_id              uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    url                    text        NOT NULL CHECK (length(url) BETWEEN 8 AND 2000),
    description            text        CHECK (length(description) <= 200),
    event_types            text[]      NOT NULL CHECK (cardinality(event_types) > 0),
    enabled                boolean     NOT NULL DEFAULT true,
    -- The signing secret, AES-GCM encrypted (nonce || ciphertext). Never stored in the clear.
    secret                 bytea       NOT NULL,
    -- After a rotation the old secret still signs, alongside the new one, until this time
    previous_secret        bytea,
    previous_secret_until  timestamptz,
    consecutive_failures   integer     NOT NULL DEFAULT 0,
    failing_since          timestamptz,
    disabled_reason        text,
    created_by             uuid        NOT NULL REFERENCES account (id),
    created_at             timestamptz NOT NULL
);
CREATE INDEX webhook_endpoint_studio_idx ON webhook_endpoint (studio_id);

-- One row per thing that happened. The body is stored exactly as it is signed and sent.
CREATE TABLE webhook_event (
    id          uuid        PRIMARY KEY,
    studio_id   uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    type        text        NOT NULL,
    -- Such as "pieces.ready:<firing id>". A listener that runs twice adds nothing the second time.
    dedupe_key  text        NOT NULL,
    body        text        NOT NULL,
    created_at  timestamptz NOT NULL,
    UNIQUE (studio_id, dedupe_key)
);

CREATE TABLE webhook_delivery (
    id               uuid        PRIMARY KEY,
    studio_id        uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    endpoint_id      uuid        NOT NULL REFERENCES webhook_endpoint (id) ON DELETE CASCADE,
    event_id         uuid        NOT NULL REFERENCES webhook_event (id) ON DELETE CASCADE,
    status           text        NOT NULL CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    attempts         integer     NOT NULL DEFAULT 0,
    next_attempt_at  timestamptz,
    -- Whoever is sending holds it until then, so two instances never send the same attempt
    claimed_until    timestamptz,
    delivered_at     timestamptz,
    created_at       timestamptz NOT NULL
);
CREATE INDEX webhook_delivery_due_idx ON webhook_delivery (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX webhook_delivery_endpoint_idx ON webhook_delivery (endpoint_id, created_at DESC);

CREATE TABLE webhook_attempt (
    id                uuid        PRIMARY KEY,
    studio_id         uuid        NOT NULL REFERENCES studio (id) ON DELETE CASCADE,
    delivery_id       uuid        NOT NULL REFERENCES webhook_delivery (id) ON DELETE CASCADE,
    attempted_at      timestamptz NOT NULL,
    status_code       integer,
    error             text,
    duration_ms       integer     NOT NULL,
    response_excerpt  text
);
CREATE INDEX webhook_attempt_delivery_idx ON webhook_attempt (delivery_id, attempted_at);

GRANT SELECT, INSERT, UPDATE, DELETE ON webhook_endpoint, webhook_event, webhook_delivery, webhook_attempt TO app_rw;

ALTER TABLE webhook_endpoint ENABLE ROW LEVEL SECURITY;
CREATE POLICY webhook_endpoint_in_tenant ON webhook_endpoint FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
ALTER TABLE webhook_event ENABLE ROW LEVEL SECURITY;
CREATE POLICY webhook_event_in_tenant ON webhook_event FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
ALTER TABLE webhook_delivery ENABLE ROW LEVEL SECURITY;
CREATE POLICY webhook_delivery_in_tenant ON webhook_delivery FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));
ALTER TABLE webhook_attempt ENABLE ROW LEVEL SECURITY;
CREATE POLICY webhook_attempt_in_tenant ON webhook_attempt FOR ALL TO app_rw
    USING (studio_id = (SELECT app.current_studio_id())) WITH CHECK (studio_id = (SELECT app.current_studio_id()));

-- The deliveries due for an attempt, in every studio, for the retry sweep (ADR 10). Returns only
-- ids, and the account that made each endpoint, to act as. The sweep claims and sends each one
-- inside its studio, under row-level security.
CREATE FUNCTION app.due_webhook_deliveries(now timestamptz, max_rows integer)
    RETURNS TABLE (delivery_id uuid, studio_id uuid, account_id uuid)
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    SELECT d.id, d.studio_id, e.created_by
    FROM public.webhook_delivery d
    JOIN public.webhook_endpoint e ON e.id = d.endpoint_id
    WHERE d.status = 'PENDING' AND e.enabled AND d.next_attempt_at <= now
      AND (d.claimed_until IS NULL OR d.claimed_until < now)
    ORDER BY d.next_attempt_at
    LIMIT max_rows
$$;
REVOKE ALL ON FUNCTION app.due_webhook_deliveries(timestamptz, integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.due_webhook_deliveries(timestamptz, integer) TO app_rw;

-- Deletes webhook events, and with them their deliveries and attempts, from before that time, in
-- every studio. It touches nothing else.
CREATE FUNCTION app.purge_webhook_history(before timestamptz)
    RETURNS integer
    LANGUAGE sql
    SECURITY DEFINER
    SET search_path = pg_catalog, pg_temp
AS $$
    WITH gone AS (DELETE FROM public.webhook_event WHERE created_at < before RETURNING 1)
    SELECT count(*)::integer FROM gone
$$;
REVOKE ALL ON FUNCTION app.purge_webhook_history(timestamptz) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.purge_webhook_history(timestamptz) TO app_rw;

ALTER TABLE notification DROP CONSTRAINT notification_kind_check;
ALTER TABLE notification ADD CONSTRAINT notification_kind_check
    CHECK (kind IN ('PIECES_READY', 'STUDIO_NOTICE', 'MAINTENANCE_DUE', 'PROBLEM_REPORTED', 'SHIFT_COVER',
                    'WEBHOOK_DISABLED'));
