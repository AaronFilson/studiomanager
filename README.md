# studiomanager

Studio manager: software to run your pottery studio better, so you can focus on what matters.

A free studio manager that actually knows what a kiln is: piece tracking from greenware to pickup,
firing fees, kiln loads, and supplies. See the [market survey](docs/planning/01-market-survey.md) and
the [Wave 1 plan](docs/planning/02-wave1-plan.md).

**Status:** milestone M0 (the skeleton). A signed-in user can create a studio and open its
"hello studio" page, with tenant isolation enforced down to the database. Nothing is deployed yet;
the [first-deploy runbook](docs/runbooks/first-deploy.md) takes an empty Azure subscription to a
running environment.

## Repository layout

| Path | What |
|---|---|
| [`api/`](api) | Spring Boot 4.1 on Java 25: a modular monolith (Spring Modulith), PostgreSQL 18 with row-level security, Flyway |
| [`api/src/main/openapi/studiomanager.yaml`](api/src/main/openapi/studiomanager.yaml) | The HTTP contract. Server interfaces and the TypeScript client are generated from it |
| [`packages/api-client/`](packages/api-client) | Generated TypeScript client and TanStack Query options, shared by web and (from M1) mobile |
| [`web/`](web) | React 19, Vite 8, TanStack Router and Query, MSAL for Entra External ID |
| [`infra/`](infra) | Terraform: bootstrap, identity, and the `dev` environment on Azure Container Apps |
| [`.github/workflows/`](.github/workflows) | CI (build, test, drift and Terraform checks, plan on PRs) and deploy-on-merge, keyless via OIDC |
| [`docs/adr/`](docs/adr) | Architecture decision records |
| [`docs/runbooks/`](docs/runbooks) | Operational checklists |

## Architecture in brief

- **One deployable, many modules.** Module boundaries are verified by a test
  ([ADR 2](docs/adr/0002-modular-monolith.md)).
- **Tenant isolation in two layers.** Application checks come first, and PostgreSQL row-level
  security sits underneath them. The tenant is bound per transaction from a Java 25
  `ScopedValue`, and the app refuses to start if its database login could bypass RLS
  ([ADR 3](docs/adr/0003-tenant-isolation-with-row-level-security.md)).
- **OpenAPI-first.** Contract drift fails the build ([ADR 4](docs/adr/0004-openapi-first-contract.md)).
- **No secrets.** The API gets passwordless Postgres access through its managed identity, with
  migrations in a separate job under a separate identity
  ([ADR 5](docs/adr/0005-passwordless-database-and-migration-job.md)). GitHub Actions signs in to
  Azure through OIDC federation ([ADR 6](docs/adr/0006-terraform-and-keyless-ci.md)).
- **Identity** comes from Microsoft Entra External ID; the API is a plain OAuth resource server
  ([ADR 7](docs/adr/0007-identity-with-entra-external-id.md)).
- **Cold starts.** Scale to zero, with a JVM AOT cache trained in the image build
  ([ADR 8](docs/adr/0008-jvm-aot-cache-for-cold-starts.md)).

## Local development

You need JDK 25, Node 24, and Docker (for the database and the integration tests).

```sh
docker compose up -d                       # PostgreSQL 18 with the app's database roles

cd api
export STUDIOMANAGER_AUTH_ISSUER_URI=https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0
export STUDIOMANAGER_AUTH_AUDIENCE=<api-app-client-id>
./gradlew bootRun                          # http://localhost:8080; migrates on startup

cd ..
corepack pnpm@12.14.0 install
cp web/.env.example web/.env.local         # fill in the Entra values
corepack pnpm@12.14.0 --filter @studiomanager/web dev   # http://localhost:5173, proxies /api
```

Signing in needs an Entra External ID tenant and the two app registrations. Steps 1–4 of the
[runbook](docs/runbooks/first-deploy.md) set them up; the free tier is enough.

### Tests

```sh
bash scripts/check.sh                  # everything: API build and tests, generated client, web
bash scripts/api.sh                    # API: unit, architecture and Testcontainers integration tests
bash scripts/js.sh -r run test         # API client and web (Vitest, Testing Library, MSW)
```

The scripts find a JDK 25 (`STUDIOMANAGER_JDK`, a `JAVA_HOME` that is already 25 or later, or
IntelliJ's bundled JBR) and run the pinned pnpm through corepack.

Integration tests need Docker. Where Docker is missing they're skipped, except in CI, where a
missing Docker fails the build. If Docker runs only inside WSL, run the Gradle build from WSL.

### Changing the API

1. Edit the contract in `api/src/main/openapi/studiomanager.yaml`.
2. Implement it: `./gradlew compileJava` regenerates the server interfaces, and the compiler
   shows what's missing.
3. Regenerate the client with `corepack pnpm@12.14.0 --filter @studiomanager/api-client run generate`,
   and commit the result.
4. For a new studio-owned table, the migration needs a `studio_id`, `ENABLE ROW LEVEL SECURITY`,
   policies, and explicit grants to `app_rw`. Copy the pattern in
   `V1__tenancy_foundation.sql`.

## License

[MIT](LICENSE)
