#!/usr/bin/env bash
# Runs inside WSL; started by scripts/integration.sh. Usage: integration-inner.sh <repo> <gradle args...>
set -euo pipefail

SRC="$1"
shift
WORK=/tmp/sm-verify
JDK="$WORK/jdk-25"
mkdir -p "$WORK"

if [[ ! -x "$JDK/bin/java" ]]; then
    echo "Downloading Temurin 25 into $JDK"
    # The release metadata names the archive and its SHA-256
    assets="$(curl -fsSL "https://api.adoptium.net/v3/assets/latest/25/hotspot?os=linux&architecture=x64&image_type=jdk&vendor=eclipse")"
    package="$(echo "$assets" | tr -d '\n ' | grep -o '"package":{[^}]*}' | head -n 1)"
    link="$(echo "$package" | grep -o '"link":"[^"]*"' | cut -d'"' -f4)"
    expected="$(echo "$package" | grep -o '"checksum":"[0-9a-f]*"' | cut -d'"' -f4)"
    if [[ -z "$link" || ${#expected} -ne 64 ]]; then
        echo "Couldn't read the Temurin 25 release metadata" >&2
        exit 1
    fi
    curl -fsSL -o "$WORK/jdk.tar.gz" "$link"
    echo "$expected  $WORK/jdk.tar.gz" | sha256sum -c -
    rm -rf "$JDK"
    mkdir -p "$JDK"
    tar -xzf "$WORK/jdk.tar.gz" -C "$JDK" --strip-components=1
    rm "$WORK/jdk.tar.gz"
fi

# A fresh copy each run (tar, because rsync isn't installed everywhere)
rm -rf "$WORK/api"
tar -C "$SRC" --exclude=api/build --exclude=api/.gradle -cf - api | tar -C "$WORK" -xf -
cd "$WORK/api"
# Files checked out on Windows may lack the executable bit
chmod +x gradlew
export JAVA_HOME="$JDK" GRADLE_USER_HOME="$WORK/gradle-home" CI=true
status=0
./gradlew --console=plain "$@" || status=$?

# A one-line count per test class, so a run that skipped the Docker tests is obvious
if compgen -G "build/test-results/test/*.xml" > /dev/null; then
    echo "== Test results"
    for report in build/test-results/test/*.xml; do
        head -n 2 "$report" | grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
            | sed -E 's/testsuite name="io\.github\.aaronfilson\.studiomanager\.([^"]*)" /\1 /'
    done
    # For each failure: the test, the root cause, and the application's own stack frames
    if grep -l '<failure' build/test-results/test/*.xml > /dev/null 2>&1; then
        echo "== Failures"
        for report in $(grep -l '<failure' build/test-results/test/*.xml); do
            sed -E 's/&#10;/\n/g; s/&#9;//g; s/&quot;/"/g' "$report" \
                | grep -E '^(<testcase name=|.*<failure message=|\s*Caused by|\s*at io\.github\.aaronfilson)' \
                | grep -v -E 'Tests\.java|<testcase name="[^"]*" classname="[^"]*" time="[^"]*"/>' \
                | cut -c1-300 | head -n 25
            # The root cause's own frames, where a library threw
            sed -E 's/&#10;/\n/g; s/&#9;//g; s/&quot;/"/g' "$report" \
                | awk '/^Caused by/ { n = NR } { line[NR] = $0 } END { for (i = n; i < n + 15 && i <= NR; i++) print line[i] }' \
                | cut -c1-300
        done
    fi
fi
exit "$status"
