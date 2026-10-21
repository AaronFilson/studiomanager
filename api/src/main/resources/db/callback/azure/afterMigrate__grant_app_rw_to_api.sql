-- Azure only (application-migrate.yaml). Grants the data role to the API's managed identity
-- once that identity's Postgres principal exists. The principal is created once per
-- environment; see docs/runbooks/first-deploy.md. Safe to run after every migration.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = '${apiPrincipal}') THEN
        EXECUTE format('GRANT app_rw TO %I', '${apiPrincipal}');
    END IF;
END
$$;
