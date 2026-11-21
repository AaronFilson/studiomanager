#!/usr/bin/env bash
# Regenerates JS artifacts inside the Linux node:24 Docker container (same environment as CI)
# and copies pnpm-lock.yaml (with --lockfile), packages/api-client/src/generated,
# web/src/routeTree.gen.ts, and mobile/assets back into the checkout. Useful on Windows when
# pnpm/corepack is managed outside WSL bash.
source "$(dirname "$0")/lib.sh"

if ! command -v wsl.exe >/dev/null 2>&1; then
    echo "scripts/sync-generated.sh requires WSL (wsl.exe not found on PATH)." >&2
    echo "On Linux/macOS, run pnpm directly via scripts/js.sh instead." >&2
    exit 1
fi

DISTRO="${STUDIOMANAGER_WSL_DISTRO:-Debian}"
WSL_REPO="$(wsl_repo_path)"

MSYS_NO_PATHCONV=1 exec wsl.exe -d "$DISTRO" -- env SM_CHECKOUT="$WSL_REPO" \
    bash "$WSL_REPO/scripts/wsl/sync-generated-inner.sh" "$@"
