-- The transactional outbox: Spring Modulith's event publication registry.
--
-- An event handled by an @ApplicationModuleListener is written here in the same transaction
-- that published it, then delivered after commit. So a notice email goes out only if the notice
-- was saved, and one that fails (the email service is down) stays here to be retried.
--
-- The structure is Spring Modulith 2.x's for PostgreSQL. The application doesn't create it
-- itself (spring.modulith.events.jdbc.schema-initialization.enabled is off), because its login
-- owns nothing. Events here span studios and are only ever read by the registry, so the table
-- has no studio_id and no row-level security.

CREATE TABLE event_publication (
    id                     uuid        NOT NULL PRIMARY KEY,
    listener_id            text        NOT NULL,
    event_type             text        NOT NULL,
    serialized_event       text        NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamptz
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

GRANT SELECT, INSERT, UPDATE, DELETE ON event_publication TO app_rw;
