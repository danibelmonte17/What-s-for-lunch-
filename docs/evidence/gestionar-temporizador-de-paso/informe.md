# Evidencia — Gestionar temporizador de paso

- **Feature**: `gestionar-temporizador-de-paso` (`docs/specs/gestionar-temporizador-de-paso.md`)
- **Fecha**: 2026-10-29 · rama `feature/gestionar-temporizador-de-paso` · emulador `emulator-5554` (Pixel_10_Pro, API 37, 1280×2856, locale es-ES)
- **Alcance**: temporizador del Paso activo del Modo Cocina (RF-GTP1..RF-GTP6, RD23): cuenta atrás manual, pausa/reanudación, reinicio a la meta vigente, «+1 min» y alarma sonora en bucle con el tono de alarma del sistema; vive **solo en memoria** (D1) y se descarta al cambiar de Paso o salir. Sin notificaciones, servicios, `AlarmManager`, permisos nuevos, assets ni vibración (D1/D4).

## Gates

| Gate | Comando | Resultado |
|---|---|---|
| Entorno | `./init.ps1` | instala (`installDebug`) y arranca la app; **sale 1** porque aborta en `adb shell monkey` (escribe su salida en stderr y PowerShell la trata como error con `$ErrorActionPreference='Stop'`), antes de «==> Verificando». Comprobado aparte: `adb shell pidof com.whatsforlunch.app` → `24187` y `MainActivity` en primer plano |
| Lint | `lintDebug` | **0 errores, 49 warnings** (baseline 49: sin warnings nuevos) |
| Unit | `testDebugUnitTest --rerun-tasks` | **39 suites / 270 tests / 0 fallos** (baseline 38/251: +1 suite `StepTimerTest` con 9 tests y +10 tests en `CookModeViewModelTest`) |
| Compilación | `assembleDebug` | OK |
| Paridad es/en | `values/strings.xml` vs `values-en/` | **316/316** claves idénticas (305 − `cook_mode_step_timer_hint` + 12 claves `cook_mode_timer_*`) |
| E2E focalizada | `CookModeTimerE2ETest` | **8/8** verde (S1–S8; verify 1 = S1, verify 2 = S2) sobre el build final |
| E2E regresión | `CookModeE2ETest` | **7/7** verde (la aserción del hint pasivo «⏱ N min» pasa a comprobar el widget del temporizador) |
| Batería E2E completa | `./e2e.ps1` | **203 tests (201 ejecutados + 2 omitidos por `Assume` en `SettingsE2ETest`)**: primera pasada con **3 flakes del AVD** en `PantryManagementE2ETest` —2 × «Failed to inject touch input» al buscar «Editar/Eliminar Tomate fresco» y 1 `ComposeTimeoutException` de 5 s en el aviso de duplicado—, re-ejecutada aislada **5/5 verde** (XML en `e2e-pantry-rerun-2026-10-29.xml`); `CookModeTimerE2ETest` **8/8** y `CookModeE2ETest` **7/7** pasan dentro de la batería. El contador de consola de AGP muestra «205» por la doble cuenta de los 2 omitidos (convención de la sesión 89) |

El XML de la batería completa se sobrescribió al re-ejecutar la clase aislada (los resultados de `connected` se regeneran); el recuento **203 = 201 + 2** queda en este informe y en `PROGRESS.md`, y el XML conservado es el del re-run **5/5** de `PantryManagementE2ETest`.

## Escenarios verificados

### Unit

- `StepTimerTest` (9 tests, máquina pura `domain/cookmode/StepTimerRules`): estado inicial «Listo» con su meta, arranque manual, pausa/reanudación, `+1 min` (suma 60 s a meta y cuenta; con la cuenta detenida no la arranca), `Reiniciar` a la meta vigente (original + ampliaciones), tick con decremento de un segundo y llegada a cero con transición a «Tiempo cumplido», `Detener` (`stopAlarm`).
- `CookModeViewModelTest` (10 tests nuevos, tick con tiempo virtual `StandardTestDispatcher` + `FakeTimerAlarm`): arranque manual sin autoinicio (D3), decremento de 1 s por tick, pausa que congela y reanudación que continúa, `+1 min` sin interrumpir, `Reiniciar` con la meta vigente, alarma al llegar a cero y silencio con `+1 min` (S1/S2), `Detener` a «Listo» con la meta vigente, descarte al cambiar de Paso (S6), descarte y silencio al limpiar el ViewModel (salir del Modo Cocina) y Paso sin `timerMinutes` sin widget ni eventos (RF-GTP6).

### E2E focalizada (`CookModeTimerE2ETest`, 8/8)

- **S1** `alarmRingsWhenTheCountdownReachesZero` (verify 1): Paso con meta 0 min → «Iniciar» lleva la cuenta a 00:00, píldora «¡Tiempo cumplido!», reloj 00:00 y botones «+1 min»/«Detener»; el sonido se comprueba en el nivel 3 (abajo).
- **S2** `addMinuteSilencesTheAlarmAndExtendsTheCountdown` (verify 2): con la alarma sonando «+1 min» la silencia, fija la meta en 1 min y la cuenta sigue decreciendo (00:5x).
- **S3** `startingIsManualAndTheCountdownDecrements`: al entrar el temporizador está «Listo» con 05:00 (nunca arranca solo); «Iniciar» lo pone «En marcha» y el reloj baja de 05:00.
- **S4** `pausingFreezesTheCountdownAndResumingContinues`: «Pausar» congela el reloj (verificado esperando 2 s) y «Reanudar» retoma desde el mismo valor.
- **S5** `resetRestoresTheCurrentMetaAndLeavesItRunning`: `+1 min` amplía a 6 min y «Reiniciar» restablece la cuenta a 06:0x dejándola «En marcha».
- **S6** `changingStepDiscardsTheTimerAndTheAlarm`: con la alarma sonando, «Siguiente» entra «Listo» con la meta del nuevo Paso y «Volver» al anterior también «Listo» con 00:00, sin alarma heredada.
- **S7** `stepWithoutTimerShowsNoWidget`: un Paso sin `timerMinutes` no muestra el widget (RF-GTP6).
- **S8** `timerTextsChangeWithTheLanguage`: título, estados, meta y botones del widget cambian al inglés (paridad es/en).

### Alarma en tres niveles (D4/D8)

1. **Unit**: `FakeTimerAlarm` cuenta `start`/`stop` y verifica que suena al entrar en «Tiempo cumplido» y se silencia con `+1 min`, «Detener», cambio de Paso y al salir del Modo Cocina.
2. **UI (E2E)**: S1/S2/S6 comprueban el estado visible de la alarma (píldora «¡Tiempo cumplido!», 00:00 y controles «+1 min»/«Detener»).
3. **Audio real (build final)**: `adb shell dumpsys audio` muestreado cada 200 ms durante una re-ejecución focalizada de S1 (`alarmRingsWhenTheCountdownReachesZero`, 1/1 verde, 2026-10-29) registra un reproductor de la app con el stream de alarma y su reproducción efectiva (recorte; el dispositivo marca GMT, 1 h por detrás de la hora local):

```
10-29 10:34:54:518 new player piid:85303 uid/pid:10253/25361 package:com.whatsforlunch.app type:android.media.MediaPlayer
10-29 10:34:54:531 player piid:85303 new AudioAttributes: usage=USAGE_ALARM content=CONTENT_TYPE_SONIFICATION flags=0x800(FLAG_MUTE_HAPTIC)
10-29 10:34:54:554 player piid:85303 event:started
10-29 10:34:54:768 releasing player piid:85303, uid:10253
```

Cada ciclo va precedido por la sonda interna de `Ringtone` (`USAGE_NOTIFICATION_RINGTONE`, piid 85295 en este pase), liberada de inmediato. La pasada original del 28-10 (17:08 local) registró **tres ciclos** equivalentes (piid 85111/85127/85143), uno por test que llega a cero. En el flujo automatizado la alarma se corta a los pocos cientos de milisegundos porque el propio test pulsa «+1 min»/«Siguiente» o limpia el ViewModel; en uso real suena en bucle hasta «Detener» (D8), porque `SystemRingtoneTimerAlarm` fija `isLooping = true`. Recorte completo en `audio-s1-2026-10-29.txt` (resultado del pase en `e2e-s1-audio-2026-10-29.xml`: `tests=1 failures=0`).

## Capturas (2, es, PNG 1280×2856)

1. `gestionar-temporizador-de-paso-01-en-marcha.png` — RF-GTP1/RF-GTP2/D2/D3: Paso 2 de 3 con el widget «Temporizador» **«En marcha»**, reloj **04:59** (la cuenta decrece desde 05:00), anillo de progreso, «Meta: 5 min» y controles «Pausar», «Reiniciar» y «+1 min».
2. `gestionar-temporizador-de-paso-02-tiempo-cumplido.png` — RF-GTP4/RF-GTP5/D2/D8: al llegar a cero, píldora **«¡Tiempo cumplido!»**, reloj **00:00** y controles «+1 min» (primario) y «Detener»; es la pantalla en la que suena la alarma (nivel 3 arriba).

Las capturas se tomaron con `adb exec-out screencap -p` (redirección binaria, firma PNG verificada y tamaño exacto 1280×2856) durante la ejecución de `CookModeTimerE2ETest`, sin tests ni código temporales.

## Reproducción manual (validador)

1. Instalar y abrir la app (`./init.ps1` o `adb install -r app/build/outputs/apk/debug/app-debug.apk`).
2. Abrir una Receta con un Paso que tenga temporizador, «Empezar Modo Cocina» y pulsar **«Iniciar»**.
3. Con una meta corta (o «+1 min» hasta agotarla) esperar a 00:00: debe verse «¡Tiempo cumplido!» y **oírse el tono de alarma del sistema en bucle**; «Detener» lo silencia y vuelve a «Listo» con la meta vigente, «+1 min» lo silencia y reanuda la cuenta.
4. Mientras suena: `adb shell dumpsys audio | Select-String "USAGE_ALARM"` (o `adb shell dumpsys audio` completo) muestra el reproductor de `com.whatsforlunch.app` con `usage=USAGE_ALARM`.
5. Cambiar de Paso o salir con «Volver» con la alarma sonando: se silencia y la cuenta se descarta.

## Notas de implementación

- **Máquina pura sin dependencias de Android** (`domain/cookmode/StepTimer.kt`): las transiciones viven en `StepTimerRules` (P25); el tick es un `delay(1000)` del ViewModel, virtualizable en los tests.
- **`AudioAttributes` en lugar de `audioStreamType`**: `Ringtone.setAudioStreamType` ya no está en los stubs de compileSdk 35; el stream de alarma se fija con `AudioAttributes(USAGE_ALARM + CONTENT_TYPE_SONIFICATION)`, equivalente y moderno.
- **Pantalla encendida** (D9): `keepScreenOn` mientras el Modo Cocina está visible, restaurado al salir.
- **Defecto visual detectado y corregido durante la captura**: con tres botones por fila en ~426 dp, «Reanudar» partía en dos líneas; los controles del temporizador pasan a `contentPadding` horizontal reducido (los targets siguen en ≥ 48 dp). Las capturas son del build final con el arreglo.
- **Sin cambios de datos ni de red**: no hay migración, ni permisos nuevos, ni dependencias nuevas, ni llamadas a la IA.
