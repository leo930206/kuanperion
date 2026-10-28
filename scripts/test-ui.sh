#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
PRISM="$HOME/Library/Application Support/PrismLauncher"
CP="$ROOT/build/classes:$PRISM/libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar"
while IFS= read -r dep; do CP="$CP:$dep"; done < <(find "$PRISM/libraries" "$ROOT/src/main/resources/META-INF/jars" "$ROOT/build/tmp/fabric-api" -name '*.jar' -type f)
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 25 -encoding UTF-8 -cp "$CP" -d "$TMP" "$ROOT/src/test/java/dev/kuan/kuanperion/ui/UiModelTest.java" "$ROOT/src/test/java/dev/kuan/kuanperion/ui/GroupHeaderLayoutTest.java"
"$JDK/bin/java" -Djava.awt.headless=true -cp "$TMP:$CP" dev.kuan.kuanperion.ui.UiModelTest

"$JDK/bin/java" -Djava.awt.headless=true -cp "$TMP:$CP" dev.kuan.kuanperion.ui.GroupHeaderLayoutTest

python3 "$ROOT/scripts/test-chat-style.py" "$JDK" "$CP"

python3 "$ROOT/scripts/test-ui-smoke-exit.py"

python3 "$ROOT/scripts/test-ui-smoke-trace.py"
