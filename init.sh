#!/usr/bin/env bash
set -euo pipefail

# Rutas absolutas resueltas desde el propio script
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
mkdir -p "$ROOT/.harness"

# JAVA_HOME: usa el JBR de Android Studio si no está definido
if [ -z "${JAVA_HOME:-}" ]; then
  for jbr in "$HOME/android-studio/jbr" "/opt/android-studio/jbr" "/usr/local/android-studio/jbr" "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    if [ -d "$jbr" ]; then export JAVA_HOME="$jbr"; break; fi
  done
fi

if [ ! -f "$ROOT/gradlew" ]; then
  echo "ERROR: no hay proyecto Android todavia (falta gradlew). Ejecuta init cuando exista el scaffold." >&2
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb no esta en el PATH." >&2
  exit 1
fi

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK_DIR" ] && [ -f "$ROOT/local.properties" ]; then
  SDK_DIR="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | tr -d '\r' || true)"
fi

echo "==> Instalando dependencias"
./gradlew --version

echo "==> Preparando entorno"
if [ -n "$SDK_DIR" ] && [ ! -f "$ROOT/local.properties" ]; then
  printf 'sdk.dir=%s\n' "$SDK_DIR" > "$ROOT/local.properties"
fi
if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: no hay emulador ni dispositivo conectado (adb get-state)." >&2
  exit 1
fi

echo "==> Arrancando la app"
./gradlew installDebug

APK="$(ls "$ROOT"/app/build/outputs/apk/debug/*.apk 2>/dev/null | head -n 1 || true)"
if [ -z "$APK" ]; then
  echo "ERROR: no se genero el APK en app/build/outputs/apk/debug." >&2
  exit 1
fi

# El identificador del paquete se lee del APK generado; se puede forzar con la variable de entorno PKG
PKG="${PKG:-}"
if [ -z "$PKG" ]; then
  if command -v aapt >/dev/null 2>&1; then
    PKG="$(aapt dump badging "$APK" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n 1 || true)"
  fi
  if [ -z "$PKG" ] && [ -n "$SDK_DIR" ]; then
    AAPT2="$(ls "$SDK_DIR"/build-tools/*/aapt2 2>/dev/null | sort | tail -n 1 || true)"
    if [ -n "$AAPT2" ]; then
      PKG="$("$AAPT2" dump badging "$APK" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n 1 || true)"
    fi
  fi
fi
if [ -z "$PKG" ]; then
  echo "ERROR: no se pudo determinar el paquete de la app. Define la variable de entorno PKG con su applicationId y vuelve a ejecutar." >&2
  exit 1
fi

adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

echo "==> Verificando"
if ! adb shell pm path "$PKG" >/dev/null 2>&1; then
  echo "ERROR: la app no quedo instalada en el dispositivo." >&2
  exit 1
fi
PID="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "ERROR: la app no esta en ejecucion tras el arranque." >&2
  exit 1
fi

echo "==> Entorno listo"
echo "App $PKG en ejecucion (pid $PID) sobre el dispositivo de adb"
exit 0
