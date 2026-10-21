#!/usr/bin/env bash
# The full local check: API build and tests, the generated client, then web, mobile and client
# typecheck, lint and tests. Integration tests are skipped where Docker is missing.
source "$(dirname "$0")/lib.sh"

echo "== API"
"$REPO_ROOT/scripts/api.sh" build

echo "== API client (regenerate; fails if the client on disk was stale)"
generated="$REPO_ROOT/packages/api-client/src/generated"
fingerprint() { find "$generated" -type f -print0 | sort -z | xargs -0 sha256sum; }
before="$(fingerprint)"
pnpm --filter @studiomanager/api-client run generate
if [[ "$(fingerprint)" != "$before" ]]; then
    echo "The generated API client was out of date and has been regenerated. Review and commit it, then rerun." >&2
    exit 1
fi

echo "== Web, mobile and client"
pnpm -r run typecheck
pnpm -r run lint
pnpm -r run test

echo "== All checks passed"
