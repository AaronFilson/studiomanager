#!/usr/bin/env bash
# Regenerates JS artifacts inside the Linux node:24 Docker container (same environment as CI)
# and copies pnpm-lock.yaml, packages/api-client/src/generated and web/src/routeTree.gen.ts
# back into the checkout. Useful on Windows when pnpm/corepack is managed outside WSL bash.
source "$(dirname "$0")/lib.sh"

DISTRO="${STUDIOMANAGER_WSL_DISTRO:-Debian}"
WSL_REPO="$(wsl_repo_path)"

MSYS_NO_PATHCONV=1 exec wsl.exe -d "$DISTRO" -- env SM_CHECKOUT="$WSL_REPO" \
    bash "$WSL_REPO/scripts/wsl/sync-generated-inner.sh" "$@"
