#!/usr/bin/env bash
# Sparse-clones pinned public invoice eval sources into a git-ignored cache folder
# (.eval-sources-cache/) and prints attribution/license details from
# api/src/test/resources/invoices/eval/sources.json (docs/planning/24).
#
# Usage:
#   bash scripts/eval-sources.sh --list
#   bash scripts/eval-sources.sh fetch [source-id]
#   bash scripts/eval-sources.sh fetch --dry-run [source-id]
source "$(dirname "$0")/lib.sh"

MANIFEST="$REPO_ROOT/api/src/test/resources/invoices/eval/sources.json"
CACHE_DIR="$REPO_ROOT/.eval-sources-cache"

usage() {
    cat <<'EOF'
Usage:
  bash scripts/eval-sources.sh --list
  bash scripts/eval-sources.sh fetch [--dry-run] [source-id]

Reads api/src/test/resources/invoices/eval/sources.json and sparse-clones pinned
public eval repositories into .eval-sources-cache/ (git-ignored).
EOF
}

if [[ ! -f "$MANIFEST" ]]; then
    echo "Missing manifest: $MANIFEST" >&2
    exit 1
fi

parse_manifest() {
    if command -v node >/dev/null 2>&1; then
        node - "$MANIFEST" "$1" "${2:-}" <<'NODE'
const fs = require('node:fs');
const [manifestPath, mode, filterId] = process.argv.slice(2);
const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
const sources = filterId
  ? manifest.sources.filter((s) => s.id === filterId)
  : manifest.sources;
if (filterId && sources.length === 0) {
  console.error(`Unknown source id: ${filterId}`);
  process.exit(1);
}
if (mode === 'list') {
  for (const s of sources) {
    console.log(`${s.id}\t${s.split}\t${s.fetch}\t${s.license}\t${s.repo}`);
  }
} else if (mode === 'tsv') {
  for (const s of sources) {
    console.log([s.id, s.repo, s.commit, s.fetch, s.license, s.paths.join(',')].join('\t'));
  }
}
NODE
    else
        echo "Node.js is required to parse $MANIFEST" >&2
        exit 1
    fi
}

CMD="${1:-}"
case "$CMD" in
    --list|list)
        parse_manifest list "${2:-}"
        ;;
    fetch)
        shift
        DRY_RUN=0
        if [[ "${1:-}" == "--dry-run" ]]; then
            DRY_RUN=1
            shift
        fi
        FILTER_ID="${1:-}"
        rows="$(parse_manifest tsv "$FILTER_ID")"
        mkdir -p "$CACHE_DIR"
        while IFS=$'\t' read -r id repo commit fetch_kind license paths_csv; do
            [[ -n "$id" ]] || continue
            target="$CACHE_DIR/$id"
            echo "== $id ($license) from $repo [$commit]"
            if [[ "$fetch_kind" == "git+lfs" ]]; then
                echo "   note: uses git-lfs; install git-lfs before pulling full image assets"
            fi
            if [[ "$DRY_RUN" -eq 1 ]]; then
                echo "   [dry-run] would sparse-clone into $target with paths: $paths_csv"
                continue
            fi
            if [[ ! -d "$target/.git" ]]; then
                git clone --filter=blob:none --no-checkout "$repo" "$target"
            else
                git -C "$target" fetch --depth=1 origin "$commit"
            fi
            git -C "$target" sparse-checkout init --no-cone
            IFS=',' read -r -a sparse_paths <<< "$paths_csv"
            git -C "$target" sparse-checkout set "${sparse_paths[@]}"
            if [[ "$commit" == "HEAD" && -f "$target/.git/FETCH_HEAD" ]]; then
                git -C "$target" checkout FETCH_HEAD
            else
                git -C "$target" checkout "$commit"
            fi
        done <<< "$rows"
        echo "Done. Review licenses and strip any customer details before copying into api/src/test/resources/invoices/eval/."
        ;;
    -h|--help|"")
        usage
        ;;
    *)
        echo "Unknown command: $CMD" >&2
        usage >&2
        exit 1
        ;;
esac
