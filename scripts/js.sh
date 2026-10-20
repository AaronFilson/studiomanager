#!/usr/bin/env bash
# Runs the pinned pnpm from the repo root through corepack. Arguments go to pnpm.
#   scripts/js.sh install
#   scripts/js.sh -r run test
#   scripts/js.sh --filter @studiomanager/api-client run generate
source "$(dirname "$0")/lib.sh"

pnpm "$@"
