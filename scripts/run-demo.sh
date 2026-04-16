#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/build.sh"
java -jar "$ROOT/build/spain-reverse-geocoder.jar" demo
