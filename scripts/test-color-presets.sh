#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
GSON="$HOME/Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar"
if [ ! -f "$GSON" ]; then GSON=$(find "$HOME/Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson" -name 'gson-*.jar' -type f | head -1); fi
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 25 -encoding UTF-8 -cp "$GSON" -d "$TMP" "$ROOT/src/main/java/dev/kuan/kuanperion/ui/theme/SkyThemePalette.java" "$ROOT/src/main/java/dev/kuan/kuanperion/ui/theme/ColorPresetStore.java" "$ROOT/src/test/java/dev/kuan/kuanperion/ui/theme/ColorPresetStoreTest.java"
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.ui.theme.ColorPresetStoreTest "$ROOT/src/main/resources/assets/kuanperion/themes/sky-blossom.json"
