#!/usr/bin/env bash
# Runs the API build with its Testcontainers integration tests inside WSL, where Docker is.
#
# Copies api/ to /tmp/sm-verify in the WSL distribution, so the Windows checkout and the WSL
# home stay untouched. On first use it downloads Eclipse Temurin 25 there and checks it
# against the published SHA-256 (see scripts/wsl/integration-inner.sh). Arguments go to
# Gradle; the default is `build`.
#   scripts/integration.sh
#   scripts/integration.sh test --tests '*PieceApiTests'
source "$(dirname "$0")/lib.sh"

if ! command -v wsl.exe >/dev/null 2>&1; then
    echo "scripts/integration.sh requires WSL (wsl.exe not found on PATH)." >&2
    echo "On Linux/macOS with Docker installed locally, run bash scripts/api.sh instead." >&2
    exit 1
fi

DISTRO="${STUDIOMANAGER_WSL_DISTRO:-Debian}"
if [[ $# -eq 0 ]]; then
    set -- build
fi

WSL_REPO="$(wsl_repo_path)"

MSYS_NO_PATHCONV=1 exec wsl.exe -d "$DISTRO" -- bash "$WSL_REPO/scripts/wsl/integration-inner.sh" "$WSL_REPO" "$@"
