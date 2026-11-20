#!/usr/bin/env bash
# Runs inside WSL; started by scripts/sync-generated.sh.
# Usage: sync-generated-inner.sh [--lockfile]
set -euo pipefail

SRC="${SM_CHECKOUT:?SM_CHECKOUT is required}"
SHARED=/tmp/sm-web-ci
WORK="$SHARED/checkouts/$(echo "$SRC" | sha1sum | cut -c1-12)-sync"
mkdir -p "$WORK" "$SHARED/pnpm-store"
docker run --rm -v "$WORK:/work" node:24 rm -rf /work/src
mkdir -p "$WORK/src"

tar -C "$SRC" \
    --exclude=.git \
    --exclude=node_modules \
    --exclude='*/node_modules' \
    --exclude='packages/*/node_modules' \
    --exclude=api/build \
    --exclude=api/.gradle \
    --exclude=auth/build \
    --exclude=web/dist \
    --exclude=mobile/dist \
    -cf - . | tar -C "$WORK/src" -xf -

install_cmd="pnpm install --frozen-lockfile >/dev/null"
if [[ "${1:-}" == "--lockfile" ]]; then
    install_cmd="pnpm install --no-frozen-lockfile >/dev/null"
fi

docker run --rm -e CI=true \
    -v "$WORK/src:/repo" -v "$SHARED/pnpm-store:/pnpm-store" -w /repo node:24 \
    bash -euo pipefail -c "corepack enable >/dev/null
pnpm config set store-dir /pnpm-store >/dev/null
$install_cmd
pnpm --filter @studiomanager/api-client run generate
pnpm --filter @studiomanager/web run build"

if [[ "${1:-}" == "--lockfile" ]]; then
    cp "$WORK/src/pnpm-lock.yaml" "$SRC/pnpm-lock.yaml"
fi
rm -rf "$SRC/packages/api-client/src/generated"
cp -R "$WORK/src/packages/api-client/src/generated" "$SRC/packages/api-client/src/generated"
if [[ -f "$WORK/src/web/src/routeTree.gen.ts" ]]; then
    cp "$WORK/src/web/src/routeTree.gen.ts" "$SRC/web/src/routeTree.gen.ts"
fi
echo "== Synced generated artifacts back to $SRC"
