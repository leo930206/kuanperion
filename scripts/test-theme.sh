#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 21 -encoding UTF-8 -d "$TMP" "$ROOT/src/main/java/dev/kuan/kuanperion/ui/theme/SkyThemePalette.java" "$ROOT/src/test/java/dev/kuan/kuanperion/ui/theme/SkyThemePaletteTest.java"
"$JDK/bin/java" -cp "$TMP" dev.kuan.kuanperion.ui.theme.SkyThemePaletteTest
