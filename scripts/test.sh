#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT/build-test"
MAIN_CLASSES="$BUILD_DIR/main"
TEST_CLASSES="$BUILD_DIR/test"
rm -rf "$BUILD_DIR"
mkdir -p "$MAIN_CLASSES" "$TEST_CLASSES"
find "$ROOT/src/main/java" -name "*.java" > "$BUILD_DIR/main-sources.txt"
javac -source 1.8 -target 1.8 -d "$MAIN_CLASSES" @"$BUILD_DIR/main-sources.txt"
cp -R "$ROOT/src/main/resources/." "$MAIN_CLASSES/"
find "$ROOT/src/test/java" -name "*.java" > "$BUILD_DIR/test-sources.txt"
javac -source 1.8 -target 1.8 -cp "$MAIN_CLASSES" -d "$TEST_CLASSES" @"$BUILD_DIR/test-sources.txt"
cd "$ROOT"
java -cp "$MAIN_CLASSES:$TEST_CLASSES" com.futesat.spaingeo.SelfTest
