# Shared setup for the scripts in this folder. Sourced, not run.
#
# The scripts exist so that tools (and AI agents) can run the build through one fixed path
# inside the repo, instead of commands that name a JDK or package manager somewhere else on
# the machine. Override the JDK with STUDIOMANAGER_JDK if yours lives elsewhere.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PNPM_VERSION="12.14.0"

# Picks a JDK 25: STUDIOMANAGER_JDK, then a JAVA_HOME that already is 25+, then IntelliJ's bundled JBR.
use_jdk_25() {
    local candidate
    for candidate in "${STUDIOMANAGER_JDK:-}" "${JAVA_HOME:-}" /d/IntelliJ\ IDEA\ */jbr "/c/Program Files/JetBrains/"*/jbr; do
        [[ -n "$candidate" && -x "$candidate/bin/java" ]] || continue
        local major
        major="$("$candidate/bin/java" -XshowSettings:properties -version 2>&1 \
            | sed -n 's/^ *java\.specification\.version = \([0-9]*\).*/\1/p')"
        if [[ -n "$major" && "$major" -ge 25 ]]; then
            export JAVA_HOME="$candidate"
            return 0
        fi
    done
    echo "No JDK 25 found. Set STUDIOMANAGER_JDK to one." >&2
    return 1
}

pnpm() {
    (cd "$REPO_ROOT" && corepack "pnpm@$PNPM_VERSION" "$@")
}
