#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/build.sh"
java -jar "$ROOT/build/spain-reverse-geocoder.jar" lookup \
  --lat 40.4167 \
  --lon -3.70325
