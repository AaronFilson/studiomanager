#!/usr/bin/env bash
# Runs inside WSL; started by scripts/sync-generated.sh.
# Usage: sync-generated-inner.sh [--lockfile]
set -euo pipefail

SRC="${SM_CHECKOUT:?SM_CHECKOUT is required}"

LOCKFILE=0
for arg in "$@"; do
    case "$arg" in
        --lockfile) LOCKFILE=1 ;;
        *)
            echo "Unknown option: $arg" >&2
            echo "Usage: sync-generated-inner.sh [--lockfile]" >&2
            exit 1
            ;;
    esac
done

SHARED=/tmp/sm-web-ci
WORK="$SHARED/checkouts/$(echo "$SRC" | sha1sum | cut -c1-12)-sync"
mkdir -p "$WORK" "$SHARED/pnpm-store"
cleanup() {
    docker run --rm -v "$WORK:/work" node:24 rm -rf /work/src >/dev/null 2>&1 || true
}
trap cleanup EXIT
cleanup
mkdir -p "$WORK/src"

tar -C "$SRC" \
    --exclude=.git \
    --exclude=.eval-sources-cache \
    --exclude=.claude \
    --exclude=.expo \
    --exclude='*/.expo' \
    --exclude=api/src/test/resources/invoices/private \
    --exclude=node_modules \
    --exclude='*/node_modules' \
    --exclude='packages/*/node_modules' \
    --exclude=api/build \
    --exclude=api/.gradle \
    --exclude=auth/build \
    --exclude=auth/.gradle \
    --exclude=web/dist \
    --exclude=mobile/dist \
    --exclude=mobile/android \
    --exclude=mobile/ios \
    -cf - . | tar -C "$WORK/src" -xf -

install_cmd="pnpm install --frozen-lockfile >/dev/null"
if [[ "$LOCKFILE" -eq 1 ]]; then
    install_cmd="pnpm install --no-frozen-lockfile >/dev/null"
fi

docker run --rm -e CI=true \
    -v "$WORK/src:/repo" -v "$SHARED/pnpm-store:/pnpm-store" -w /repo node:24 \
    bash -euo pipefail -c "corepack enable >/dev/null
pnpm config set store-dir /pnpm-store >/dev/null
$install_cmd
pnpm --filter @studiomanager/api-client run generate
if [[ -f mobile/scripts/generate-assets.mjs ]]; then node mobile/scripts/generate-assets.mjs; fi
pnpm --filter @studiomanager/web run build || echo 'WARN: web build failed (routeTree may still be generated)'"

if [[ "$LOCKFILE" -eq 1 ]]; then
    cp "$WORK/src/pnpm-lock.yaml" "$SRC/pnpm-lock.yaml"
fi
rm -rf "$SRC/packages/api-client/src/generated"
cp -R "$WORK/src/packages/api-client/src/generated" "$SRC/packages/api-client/src/generated"
if [[ -f "$WORK/src/web/src/routeTree.gen.ts" ]]; then
    cp "$WORK/src/web/src/routeTree.gen.ts" "$SRC/web/src/routeTree.gen.ts"
fi
if [[ -d "$WORK/src/mobile/assets" ]]; then
    mkdir -p "$SRC/mobile/assets"
    cp -R "$WORK/src/mobile/assets/." "$SRC/mobile/assets/"
fi
echo "== Synced generated artifacts back to $SRC"
