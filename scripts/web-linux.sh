#!/usr/bin/env bash
# Runs the CI "Web, mobile and API client" job on Linux: in a node:24 container, under Docker in
# WSL. Linux differs from Windows in ways that only show in CI (case-sensitive paths, for one).
#
# What runs is the tracked files with any uncommitted changes to them (untracked files are left
# out, as they would be in CI), copied to /tmp/sm-web-ci in the WSL distribution. Arguments are a
# pnpm command to run instead of the CI steps.
#   scripts/web-linux.sh
#   scripts/web-linux.sh --filter @studiomanager/web exec vitest run src/test/pieces.test.tsx
source "$(dirname "$0")/lib.sh"

DISTRO="${STUDIOMANAGER_WSL_DISTRO:-Debian}"

drive="$(echo "${REPO_ROOT:1:1}" | tr '[:upper:]' '[:lower:]')"
WSL_REPO="/mnt/$drive${REPO_ROOT:2}"

# `git stash create` makes a commit of the working tree without touching it; empty when clean
tree="$(git -C "$REPO_ROOT" stash create)"
git -C "$REPO_ROOT" archive --format=tar "${tree:-HEAD}" \
    | MSYS_NO_PATHCONV=1 wsl.exe -d "$DISTRO" -- bash "$WSL_REPO/scripts/wsl/web-linux-inner.sh" "$@"
