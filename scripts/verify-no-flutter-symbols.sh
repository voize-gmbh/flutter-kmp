#!/bin/bash
#
# Guards the "opaque cinterop" approach (KT-81937).
#
# The generated iOS Kotlin/Native framework must carry NO undefined Flutter ObjC class
# symbols (_OBJC_CLASS_$_Flutter…). Those are provided by the host app at runtime, so the
# framework must not reference them at link time — otherwise linking breaks on recent Xcode
# and non-Flutter consumers (e.g. MAUI) crash at startup.
#
# This is the regression canary for two failure modes:
#   1. Someone reverts the opaque (`@class`) forward declarations in the cinterop stub.
#   2. A future Kotlin/Native version changes how forward-declared classes surface
#      (the `objcnames.classes` package), so the generator stops producing opaque refs.
#
# Run in CI after building the example.
set -euo pipefail

BASEDIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASEDIR/example"

echo "Building example iOS framework (iosSimulatorArm64)..."
./gradlew linkPodReleaseFrameworkIosSimulatorArm64 -q

FW="$(find build/bin/iosSimulatorArm64 -name flutterkmpexample -type f | head -1)"
if [ -z "$FW" ]; then
  echo "❌ Could not find built flutterkmpexample framework binary."
  exit 1
fi

echo "Inspecting undefined symbols in: $FW"
SYMS="$(nm -u "$FW" | grep -i Flutter || true)"

if [ -n "$SYMS" ]; then
  echo "❌ Undefined Flutter symbols found — opaque-cinterop regression (KT-81937):"
  echo "$SYMS"
  echo ""
  echo "The framework must not reference concrete Flutter ObjC classes at link time."
  echo "Check the cinterop stub (flutter-kmp/src/nativeInterop/cinterop/Flutter/FlutterPlugin.h)"
  echo "and that the generator references them via the 'objcnames.classes' package."
  exit 1
fi

echo "✅ No undefined Flutter symbols — opaque cinterop intact."
