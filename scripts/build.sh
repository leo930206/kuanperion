#!/usr/bin/env bash
set -euo pipefail

VERSION="2.0.14"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PRISM="$HOME/Library/Application Support/PrismLauncher"
MC_JAR="$PRISM/libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar"
LOADER_JAR="$PRISM/libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar"
FABRIC_API="$PRISM/instances/Hypixel SkyBlock/minecraft/mods/fabric-api-0.161.0+26.2.jar"
JAVAC="$PRISM/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin/javac"
JAR="$PRISM/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin/jar"

TMP="$ROOT/build/tmp/fabric-api"
CLASSES="$ROOT/build/classes"
OUT="$ROOT/build/libs/kuanperion-$VERSION.jar"

rm -rf "$ROOT/build"
mkdir -p "$TMP" "$CLASSES" "$ROOT/build/libs"
(cd "$TMP" && jar xf "$FABRIC_API" META-INF/jars)

CP="$MC_JAR:$LOADER_JAR:$PRISM/libraries/org/lwjgl/lwjgl-glfw/3.4.1/lwjgl-glfw-3.4.1.jar"
while IFS= read -r dep; do CP="$CP:$dep"; done < <(find "$ROOT/src/main/resources/META-INF/jars" -name '*.jar' -type f)
while IFS= read -r dep; do CP="$CP:$dep"; done < <(find "$PRISM/libraries" -name '*.jar' -type f)
while IFS= read -r dep; do CP="$CP:$dep"; done < <(find "$TMP/META-INF/jars" -name '*.jar' -type f)

"$JAVAC" --release 25 -encoding UTF-8 -cp "$CP" -d "$CLASSES" $(find "$ROOT/src/main/java" -name '*.java')
cp -R "$ROOT/src/main/resources/." "$CLASSES/"
mkdir -p "$CLASSES/META-INF/licenses"
cp "$ROOT/LICENSE" "$CLASSES/META-INF/licenses/Kuanperion-GPL-3.0.txt"
cp "$ROOT/THIRD_PARTY_NOTICES.md" "$CLASSES/META-INF/licenses/THIRD_PARTY_NOTICES.md"
"$JAR" --create --file "$OUT" -C "$CLASSES" .
echo "$OUT"
