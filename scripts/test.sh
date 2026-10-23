#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -d "$TMP" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/GardenTabNotifications.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/GardenTabNotificationsTest.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/FarmingToolGuard.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/SkyBlockLocationBridge.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/SkyBlockLocationBridgeTest.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/GuiPauseGuard.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/GuiPauseGuardTest.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/FarmingToolGuardTest.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/NotificationType.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/NotificationChannels.java" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/ScreenNotificationQueue.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/NotificationScreenDurationTest.java"
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.FarmingToolGuardTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.GuiPauseGuardTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.SkyBlockLocationBridgeTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.GardenTabNotificationsTest
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.NotificationScreenDurationTest
python3 "$ROOT/scripts/test-abnormal-handling.py" "$JAVA_HOME"
python3 "$ROOT/scripts/test-tab-notifications.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-mining-attack-lock.py"
python3 "$ROOT/scripts/test-mining-priority.py" "$JAVA_HOME"
python3 "$ROOT/scripts/test-jerry-detection.py" "$JAVA_HOME"
python3 "$ROOT/scripts/test-notification-settings.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-farming-stall.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-pumpkin-melon-start.py" "$JAVA_HOME"

"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -d "$TMP" \
    "$ROOT/src/main/java/dev/kuan/kuanperion/JerryFarmingPauseMarkers.java" \
    "$ROOT/src/test/java/dev/kuan/kuanperion/JerryFarmingPauseMarkersTest.java"
"$JAVA_HOME/bin/java" -cp "$TMP" dev.kuan.kuanperion.JerryFarmingPauseMarkersTest
python3 "$ROOT/scripts/test-jerry-marker-rendering.py" "$JAVA_HOME"

"$ROOT/scripts/test-theme.sh"
"$ROOT/scripts/test-hotkeys.sh"

"$ROOT/scripts/test-color-presets.sh"
