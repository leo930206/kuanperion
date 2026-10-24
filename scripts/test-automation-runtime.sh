#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
GSON="$HOME/Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar"
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 21 -encoding UTF-8 -cp "$GSON" -d "$TMP" $(find "$ROOT/src/main/java/dev/kuan/kuanperion/automation" -name '*.java') "$ROOT/src/test/java/dev/kuan/kuanperion/automation/AutomationRuntimeTest.java"
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationRuntimeTest
