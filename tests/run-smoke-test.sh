#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
kotlinc app/src/main/java/com/oyayasu/evoluir/Progress.kt tests/ProgressSmokeTest.kt -include-runtime -d "$tmp/test.jar"
java -jar "$tmp/test.jar"
