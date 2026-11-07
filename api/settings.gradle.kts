rootProject.name = "studiomanager-api"

// The GitHub sign-in bridge (ADR 20) builds with the API's wrapper and version catalog, so
// scripts/api.sh and CI build and test it too. It lives in ../auth because it's its own
// deployable. The API's image is built from api/ alone, where ../auth doesn't exist, so the
// include is skipped there.
val bridgeDirectory = file("../auth")
if (bridgeDirectory.isDirectory) {
    include("auth")
    project(":auth").projectDir = bridgeDirectory
}
