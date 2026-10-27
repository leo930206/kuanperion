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
python3 "$ROOT/scripts/test-farming-start-tool.py" "$JAVA_HOME"
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

python3 "$ROOT/scripts/test-settings-automation.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-sethome-key.py" "$JAVA_HOME"

"$ROOT/scripts/test-automation-preferences.sh"

python3 "$ROOT/scripts/test-settings-command.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-mining-selection.py" "$JAVA_HOME"

"$ROOT/scripts/test-automation-runtime.sh"

python3 "$ROOT/scripts/test-automation-client-bridge.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-trade-icon.py"

python3 "$ROOT/scripts/test-farming-handoff-wiring.py"

python3 "$ROOT/scripts/test-auto-sell-wiring.py"

python3 "$ROOT/scripts/test-market-boundaries.py"

python3 "$ROOT/scripts/test-junk-wiring.py"

python3 "$ROOT/scripts/test-pest-boundaries.py"

python3 "$ROOT/scripts/test-position-boundaries.py"
python3 "$ROOT/scripts/test-pest-travel-wiring.py"
python3 "$ROOT/scripts/test-cursor-boundaries.py"

python3 "$ROOT/scripts/test-greenhouse-boundaries.py"

python3 "$ROOT/scripts/test-flight-core.py"

python3 "$ROOT/scripts/test-world-input-priority.py"

python3 "$ROOT/scripts/test-greenhouse-driver.py"

python3 "$ROOT/scripts/test-composter-driver.py"
python3 "$ROOT/scripts/test-maintenance-input-priority.py"

python3 "$ROOT/scripts/test-jerry-home-handoff.py"

python3 "$ROOT/scripts/test-visitor-boundaries.py"

python3 "$ROOT/scripts/test-jerry-global-stop.py"

python3 "$ROOT/scripts/test-trap-close-boundaries.py"

python3 "$ROOT/scripts/test-visitor-data-demand.py"

python3 "$ROOT/scripts/test-supercraft-boundaries.py"

python3 "$ROOT/scripts/test-supercraft-native.py"

python3 "$ROOT/scripts/test-completion-boundaries.py"
python3 "$ROOT/scripts/test-completion-input.py"

python3 "$ROOT/scripts/test-release-audit.py"
python3 "$ROOT/scripts/audit-release.py"

python3 "$ROOT/scripts/test-pet-boundaries.py"

python3 "$ROOT/scripts/test-supercraft-service-wiring.py"

python3 "$ROOT/scripts/test-pest-bait-boundaries.py"

python3 "$ROOT/scripts/test-settings-task-coexistence.py"

python3 "$ROOT/scripts/test-trade-manual-takeover.py"

python3 "$ROOT/scripts/test-applied-inventory-hooks.py" "$JAVA_HOME"

python3 "$ROOT/scripts/test-book-carried-native.py" "$JAVA_HOME"
