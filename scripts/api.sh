#!/usr/bin/env bash
# Runs the API's Gradle build with a JDK 25. Arguments go to Gradle; the default is `build`.
#   scripts/api.sh                 compile, test and package
#   scripts/api.sh compileJava     regenerate the server interfaces and compile
source "$(dirname "$0")/lib.sh"

use_jdk_25
cd "$REPO_ROOT/api"
if [[ $# -eq 0 ]]; then
    set -- build
fi
exec ./gradlew --console=plain "$@"
