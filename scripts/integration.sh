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

DISTRO="${STUDIOMANAGER_WSL_DISTRO:-Debian}"
if [[ $# -eq 0 ]]; then
    set -- build
fi

# /c/Users/... (Git Bash) → /mnt/c/Users/... (WSL)
drive="$(echo "${REPO_ROOT:1:1}" | tr '[:upper:]' '[:lower:]')"
WSL_REPO="/mnt/$drive${REPO_ROOT:2}"

MSYS_NO_PATHCONV=1 exec wsl.exe -d "$DISTRO" -- bash "$WSL_REPO/scripts/wsl/integration-inner.sh" "$WSL_REPO" "$@"
