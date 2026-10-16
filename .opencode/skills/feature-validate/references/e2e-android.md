# E2E en emulador Android (adb)

Detalle operativo del paso E2E de `feature-validate`. Cárgalo solo cuando la Parte B de la spec incluya comprobaciones E2E en emulador.

## Daemons (reglas anti-bloqueo)

- **Gradle:** usa `--no-daemon` siempre que ejecutes Gradle desde la shell del agente (también en `connectedDebugAndroidTest`): un daemon persistente arrancado ahí hereda el pipe de salida y deja la llamada bloqueada aunque el build haya terminado.
- **adb:** antes de capturar cualquier salida de `adb`/`emulator`, arranca el daemon en segundo plano con los handles cerrados — `cmd /c "adb start-server 1>NUL 2>NUL"` (POSIX: `adb start-server >/dev/null 2>&1 </dev/null`) o `Start-Process <sdk>/platform-tools/adb -ArgumentList start-server -WindowStyle Hidden` — y **no** combines esa primera invocación con `2>&1`. Con el server ya levantado, `adb devices`, `adb install`, `adb shell`, etc. se capturan sin bloqueo.

## Herramientas

Resuelve el SDK desde `local.properties` (`sdk.dir`) o `ANDROID_HOME`; usa `<sdk>/platform-tools/adb` y `<sdk>/emulator/emulator`. No fijes rutas con el nombre de usuario.

## AVD

Un AVD es válido si su API es **30 o superior** (`minSdk 30`). `emulator -list-avds` debe dar **al menos uno** (si hay varios, prefiere el del API mínimo y uno reciente). Si no hay ninguno, para: registra «no verificable», deja el estado en `passing` y entrega en el reporte y `PROGRESS.md` los comandos exactos para crear uno:

```
sdkmanager "system-images;android-<n>;..."
avdmanager create avd ...
```

## Arranque headless

`emulator -avd <nombre> -no-window -no-audio -no-boot-anim`, **en segundo plano** (Windows `Start-Process`, POSIX `&`/`nohup`), nunca en primer plano. Espera con `adb wait-for-device` y sondea `getprop sys.boot_completed` hasta `1`, con timeout. Anota los emuladores que arrancas para matarlos al cerrar.

## Instalación y lanzamiento

`adb install -r app/build/outputs/apk/debug/app-debug.apk` y `adb shell am start -n <paquete>/<actividad>` (de la Parte B; si no constan, resuélvelos con `adb shell cmd package resolve-activity`).

## Evidencia

Captura solo los estados clave (**≤2 PNG por feature**, presupuesto total de evidencia ≤4 artefactos): `adb exec-out screencap -p > docs/evidence/<feature-id>/api<api>-<estado>.png` (API real del AVD). Para flujos largos, `screenrecord` (S06 acepta captura o vídeo). Los PNG se versionan; los vídeos no.

## Fallos

`adb logcat -d` filtrando `FATAL EXCEPTION`/`AndroidRuntime`; comprueba que el proceso sigue vivo tras cada pantalla.

## Interacción

Usa `adb shell input tap/text/swipe` guiándote por el último pantallazo. Lo que dependa de cámara, permisos o hardware no automatizable se marca «paso manual pendiente» y no cuenta como verificación en verde.
