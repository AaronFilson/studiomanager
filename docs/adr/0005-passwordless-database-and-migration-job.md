# 5. Passwordless database access, with migrations in a separate job

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Database passwords in app settings or Key Vault must be rotated, can leak, and are usually
over-privileged: the same login that serves requests can also drop tables. Running migrations at
application startup also means the serving identity needs DDL rights, and a bad migration takes
the app down while it runs.

## Decision

- **No database passwords on Azure.** Postgres Flexible Server has Microsoft Entra authentication
  on and password authentication off. The API connects as its user-assigned managed identity,
  through the `azure-identity-extensions` JDBC plugin, which trades the identity for a short-lived
  Entra token on each new connection (`application-azure.yaml`).
- **Two identities, two privilege levels.**
  - The **API** identity is a member of `app_rw`: data access only, always subject to RLS
    ([ADR 3](0003-tenant-isolation-with-row-level-security.md)).
  - The **migrator** identity is the server's Entra admin and owns the schema.
- **Migrations run as a Container Apps Job** from the *same image*, using the
  `azure,migrate` profiles. These profiles run Flyway with no web server, then exit. The deploy
  workflow runs the job and waits for it to succeed before rolling out the new API revision. An
  Azure-only Flyway `afterMigrate` callback grants `app_rw` to the API's principal.
- **One manual step per environment:** creating the API identity's Postgres principal with
  `pgaadauth_create_principal`, which has to run as an Entra admin in the `postgres` database
  ([runbook](../runbooks/first-deploy.md)).
- **Locally and in tests,** the same split uses two password logins: `studiomanager` owns the
  schema, and `studiomanager_app` is a member of `app_rw` (`api/dev/postgres-init`).

## Consequences

- There's no credential to rotate or leak, and the serving identity can't run DDL.
- A failed migration stops the deploy before any traffic reaches new code.
- Migrations must stay backward compatible with the running revision (expand, then contract),
  because the old revision serves traffic while the job runs.

## In an interview

"The API has no database password: it gets a token from its managed identity, and that identity
can only read and write rows. Migrations run as a separate job, with a separate identity that owns
the schema, from the same image, and they gate the rollout."
