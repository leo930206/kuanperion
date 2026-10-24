#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK="$HOME/Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home"
GSON="$HOME/Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar"
TMP="$(mktemp -d)";trap 'rm -rf "$TMP"' EXIT
"$JDK/bin/javac" --release 21 -encoding UTF-8 -cp "$GSON" -d "$TMP" $(find "$ROOT/src/main/java/dev/kuan/kuanperion/automation" -name '*.java') $(find "$ROOT/src/test/java/dev/kuan/kuanperion/automation" -name "*.java")
"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationRuntimeTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationSafetyTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutoSellTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutoSellServiceTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BazaarMarketTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.HypixelMarketClientTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.AutomationMenuTransitionTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BazaarPurchaseLedgerTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.BookCombinePlannerTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkTaskTest

"$JDK/bin/java" -cp "$TMP:$GSON" dev.kuan.kuanperion.automation.JunkServiceTest
