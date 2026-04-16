#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_FILE="$BUILD_DIR/spain-reverse-geocoder.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

find "$ROOT/src/main/java" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -source 1.8 -target 1.8 -d "$CLASSES_DIR" @"$BUILD_DIR/sources.txt"

if [ -d "$ROOT/src/main/resources" ]; then
  cp -R "$ROOT/src/main/resources/." "$CLASSES_DIR/"
fi

cat > "$BUILD_DIR/manifest.txt" <<MANIFEST
Main-Class: com.futesat.spaingeo.cli.Main
MANIFEST

jar cfm "$JAR_FILE" "$BUILD_DIR/manifest.txt" -C "$CLASSES_DIR" .

echo "Built: $JAR_FILE"
