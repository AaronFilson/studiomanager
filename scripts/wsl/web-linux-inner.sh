#!/usr/bin/env bash
# Runs inside WSL; started by scripts/web-linux.sh, which pipes the source tree in as a tar.
# Usage: web-linux-inner.sh [pnpm args...]
set -euo pipefail

WORK=/tmp/sm-web-ci
mkdir -p "$WORK"
# The last run's container wrote as root, so only a container can clear it
docker run --rm -v "$WORK:/work" node:24 rm -rf /work/src
mkdir -p "$WORK/src" "$WORK/pnpm-store"
tar -C "$WORK/src" -xf -

if [[ $# -gt 0 ]]; then
    steps="pnpm $(printf '%q ' "$@")"
else
    # The same steps as the web job in .github/workflows/ci.yml
    steps='pnpm --filter @studiomanager/api-client run generate
pnpm -r run typecheck
pnpm -r run lint
pnpm -r run test
pnpm -r run build'
fi

# corepack enable is fine here: it changes only the throwaway container
docker run --rm -e CI=true \
    -v "$WORK/src:/repo" -v "$WORK/pnpm-store:/pnpm-store" -w /repo node:24 \
    bash -euo pipefail -c "corepack enable >/dev/null
pnpm config set store-dir /pnpm-store >/dev/null
pnpm install --frozen-lockfile >/dev/null
$steps"
