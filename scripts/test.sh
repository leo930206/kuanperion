#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -d "$TMP" \
    "$ROOT/src/main/java/dev/kuan/skyblockassistant/FarmingToolGuard.java" \
    "$ROOT/src/test/java/dev/kuan/skyblockassistant/FarmingToolGuardTest.java"
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.skyblockassistant.FarmingToolGuardTest
