#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -d "$TMP" \
    "$ROOT/src/main/java/dev/kuan/skyblockassistant/GardenTabNotifications.java" \
    "$ROOT/src/test/java/dev/kuan/skyblockassistant/GardenTabNotificationsTest.java" \
    "$ROOT/src/main/java/dev/kuan/skyblockassistant/FarmingToolGuard.java" \
    "$ROOT/src/main/java/dev/kuan/skyblockassistant/SkyBlockLocationBridge.java" \
    "$ROOT/src/test/java/dev/kuan/skyblockassistant/SkyBlockLocationBridgeTest.java" \
    "$ROOT/src/main/java/dev/kuan/skyblockassistant/GuiPauseGuard.java" \
    "$ROOT/src/test/java/dev/kuan/skyblockassistant/GuiPauseGuardTest.java" \
    "$ROOT/src/test/java/dev/kuan/skyblockassistant/FarmingToolGuardTest.java"
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.skyblockassistant.FarmingToolGuardTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.skyblockassistant.GuiPauseGuardTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.skyblockassistant.SkyBlockLocationBridgeTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.skyblockassistant.GardenTabNotificationsTest
python3 "$ROOT/scripts/test-abnormal-handling.py" "$JAVA_HOME"
python3 "$ROOT/scripts/test-tab-notifications.py" "$JAVA_HOME"
