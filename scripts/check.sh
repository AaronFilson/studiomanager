#!/usr/bin/env bash
# The full local check: API build and tests, the generated client, then web and client
# typecheck, lint and tests. Integration tests are skipped where Docker is missing.
source "$(dirname "$0")/lib.sh"

echo "== API"
"$REPO_ROOT/scripts/api.sh" build

echo "== API client (regenerate; fails if the committed client is stale)"
pnpm --filter @studiomanager/api-client run generate
git -C "$REPO_ROOT" diff --exit-code --stat -- packages/api-client/src/generated

echo "== Web and client"
pnpm -r run typecheck
pnpm -r run lint
pnpm -r run test

echo "== All checks passed"
