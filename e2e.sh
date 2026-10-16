#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# JAVA_HOME: usa el JBR de Android Studio si no está definido
if [ -z "${JAVA_HOME:-}" ]; then
  for jbr in "$HOME/android-studio/jbr" "/opt/android-studio/jbr" "/usr/local/android-studio/jbr" "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    if [ -d "$jbr" ]; then export JAVA_HOME="$jbr"; break; fi
  done
fi

if [ ! -f "$ROOT/gradlew" ]; then
  echo "ERROR: no hay proyecto Android todavia (falta gradlew)." >&2
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb no esta en el PATH." >&2
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: no hay emulador ni dispositivo conectado para los tests instrumentados." >&2
  exit 1
fi

echo "==> Tests de extremo a extremo"
./gradlew connectedDebugAndroidTest
