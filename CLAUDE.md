# Working in this repo

## Run builds and tests only through `scripts/`

The owner's settings block reads outside the repo. Any command that names a JDK, corepack or
another tool outside the repo stops for an approval prompt. The scripts below run those tools
from one fixed path inside the repo, so use them, exactly as written, from the repo root:

| Task | Command |
|---|---|
| Everything (API, generated client, web) | `bash scripts/check.sh` |
| API build and tests | `bash scripts/api.sh` |
| API with the Docker integration tests (runs in WSL) | `bash scripts/integration.sh test` |
| The CI web job on Linux (a node:24 container under WSL Docker) | `bash scripts/web-linux.sh` |
| Any Gradle task | `bash scripts/api.sh compileJava` |
| pnpm | `bash scripts/js.sh -r run test`, `bash scripts/js.sh --filter @studiomanager/api-client run generate` |

- **The shell is only for `bash scripts/...` and `git`.** Each command is a single command,
  run from the repo root, with no `cd`, no `&&` chains and no pipes into other tools.
- **Read, search and list files with the built-in tools** (Read, Grep, Glob), never with `cat`,
  `head`, `sed`, `find` or `ls`. Edit files with Edit or Write, never with `sed` or redirection.
- Don't set `JAVA_HOME`, and don't call `corepack` or `gradlew` directly.
- Don't read or write outside the repo, including temp and scratch folders. Write notes and
  research into the repo (for example `docs/planning/`).
- Subagents follow the same rules. A research subagent uses only the web and file tools: no
  shell, no agents of its own.

Integration tests need Docker, which only runs in WSL on this machine. `scripts/api.sh` skips
them; `scripts/integration.sh` runs the full build inside WSL and prints a per-class test count,
so skipped tests are visible. CI runs them too.

## Where things are

- Plans and research: `docs/planning/`. The current plan is `02-wave1-plan.md`, revised from
  `reports/Pottery studio manager role.md`.
- Decisions: `docs/adr/`.
- The API contract: `api/src/main/openapi/studiomanager.yaml`. After changing it, run the
  client generate command above and commit the generated files.
