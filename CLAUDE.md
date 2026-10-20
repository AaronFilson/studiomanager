# Working in this repo

## Run builds and tests only through `scripts/`

The owner's settings block reads outside the repo. Any command that names a JDK, corepack or
another tool outside the repo stops for an approval prompt. The scripts below run those tools
from one fixed path inside the repo, so use them, exactly as written, from the repo root:

| Task | Command |
|---|---|
| Everything (API, generated client, web) | `bash scripts/check.sh` |
| API build and tests | `bash scripts/api.sh` |
| Any Gradle task | `bash scripts/api.sh compileJava` |
| pnpm | `bash scripts/js.sh -r run test`, `bash scripts/js.sh --filter @studiomanager/api-client run generate` |

- Don't set `JAVA_HOME`, call `corepack` or call `gradlew` directly, and don't `cd` in a
  compound command.
- Don't read or write outside the repo, including temp and scratch folders. Write notes and
  research into the repo (for example `docs/planning/`).
- Subagents follow the same rules. A research subagent uses only the web and file tools: no
  shell, no agents of its own.

Integration tests need Docker, which is not available on this Windows machine. They are
skipped locally and run in CI.

## Where things are

- Plans and research: `docs/planning/`. The current plan is `02-wave1-plan.md`, revised from
  `reports/Pottery studio manager role.md`.
- Decisions: `docs/adr/`.
- The API contract: `api/src/main/openapi/studiomanager.yaml`. After changing it, run the
  client generate command above and commit the generated files.
