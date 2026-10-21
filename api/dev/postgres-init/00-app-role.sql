-- Local and test databases only (compose.yaml, Testcontainers). Runs once, when the container's
-- data directory is first created.
--
-- The application logs in as studiomanager_app, a member of app_rw. Flyway logs in as the
-- container's superuser and owns the schema. On Azure the logins are managed identities instead;
-- see docs/runbooks/first-deploy.md.
CREATE ROLE app_rw NOLOGIN;
CREATE ROLE studiomanager_app LOGIN PASSWORD 'studiomanager_app' IN ROLE app_rw;
