# Informe de cierre — `controlar-cocina-con-voz` (repair brief del `revise`, sesiones 102c, 2026-11-06)

Cierra los hallazgos F6–F9 (+ F4) de `docs/validations/controlar-cocina-con-voz.md` §9 sobre el change set de `feature/controlar-cocina-con-voz`. Dispositivo físico **V2124 (Android 13, SDK 33)** por adb; APKs debug + test instaladas con autorización del usuario para esta reparación.

## 1. Resumen por hallazgo

| Hallazgo | Estado | Resultado |
|---|---|---|
| F6 (High) batería E2E sin ejecutar + cifras infladas | cerrado | Batería completa **213 tests ejecutados** (208 verdes + 3 fallos + 2 omitidos) en el V2124 + focalizada 25/25 + reejecución aislada del flaky; XML/HTML conservados en `docs/evidence/controlar-cocina-con-voz/` |
| F7 (Medium) evidencia de cierre incompleta | cerrado | Este informe + checklist manual con resultados (abajo) + 2 capturas es + salidas lint/unit/assemble en `gates-2026-11-06/` |
| F8 (Medium) spec desincronizada con el contrato F2 | cerrado | Spec sincronizada (`VoiceSourceEvent.LanguageUnavailable`, `VoiceNotice.LANGUAGE_NOT_DOWNLOADED`, ERROR 13 vs 3 fallos, strings, Casos límite). Nota: el sub-item «voz en pausa» no corresponde al texto actual (ver §5) |
| F9 (Low) iconos deprecados | cerrado parcialmente + documentado | `VolumeUp` → `Icons.AutoMirrored.Outlined.VolumeUp` (deprecado, verificado con `javap`). `Mic` **no** está deprecado y `AutoMirrored.Outlined.Mic` **no existe** en esta versión de material-icons (ver §4) |
| F4 wording de cifras | cerrado | Inventario 213 `@Test` (24 clases) vs ejecuciones etiquetadas: focalizada 25/25 = 10+7+8 y batería Modo Cocina 27/27 = 7+8+12 (`RecipeDetailE2ETest` incluido) |

## 2. E2E (F6)

Ejecutadas sobre el V2124 el 2026-11-06 tras aplicar F8/F9/F4 (change set actual):

- **Focalizada voz 25/25** (1 m 14 s): `CookModeVoiceE2ETest` 10/10 + `CookModeE2ETest` 7/7 + `CookModeTimerE2ETest` 8/8. Informes XML/HTML con nombres de test en `e2e-focused-2026-11-06/`.
- **Batería completa 213 tests**: **208 verdes**, **3 fallos todos en `PantryManagementE2ETest`** (2 × «Failed to inject touch input» + 1 × `ComposeTimeoutException`) y **2 omitidos** de `SettingsE2ETest` (`Assume`, conocidos). Informes en `e2e-full-2026-11-06/`.
- **Reejecución aislada de `PantryManagementE2ETest`: 5/5 verde (48 s)** — informes en `e2e-pantry-rerun-2026-11-06/`. Clasificación: **flaky del entorno, no regresión** (mismas firmas que en sesiones previas, ver `docs/progress-history.md`; la clase pasa en solitario y sus 3 fallos no tocan código de voz).
- Artefactos del dispositivo (logcats, `test-result.pb`, `profiling/*.pb` con el número de serie en el nombre) **podados**; se conservan HTML por clase, `index.html`, css/js y `xml/TEST-*.xml` (verificado que no contiene identificadores del dispositivo).

Cifras cuadradas (F4): 213 `@Test` es el **inventario** (26 clases de test; 24 ficheros en `androidTest`); las ejecuciones previas de la voz eran focalizada 25/25 (= 10+7+8) y batería Modo Cocina 27/27 (= 7+8+12, con `RecipeDetailE2ETest` 12/12). Con esta reparación, además, la **batería completa 213 ya está ejecutada** sobre el change set.

## 3. Checklist manual (F7) — instrumentado por adb en el V2124

Receta de prueba «Prueba voz» sembrada por la UI real (Paso 1 «Sofreir la cebolla» con temporizador de 1 min, Paso 2 «Batir los huevos»). Estímulos = renders del TTS del propio dispositivo (es, ritmo 0,7) reproducidos desde los altavoces del PC a ~30 cm. Protocolo por repetición: **render doble** (mismo WAV 2 veces, ~3 s de separación) y ~7 s de espera tras navegación/toggle (ver §4).

| Ítem | Resultado | Evidencia |
|---|---|---|
| M5-a «Siguiente» avanza de Paso | **3/3** | Paso 1 → Paso 2 en cada repetición (dumps `m5a1*/m5a2/m5a3`) |
| M5-b «Repetir» no cambia de Paso y relocuta | **3/3** | Paso 2 fijo en las 3 reps + `AudioPlaybackConfiguration piid:11551 AudioTrack state:started USAGE_MEDIA uid 10182` (= `com.google.android.tts`, la relocución) |
| M5-c «Pausar tiempo» pausa la cuenta | **4/5 intentos** (3 pausas exigidas superadas) | «Pausado» 00:52 / 00:27 / 00:19 / 00:11; 1 intento sin reconocer (quedó «En marcha» 00:39) — fallo aislado de reconocimiento, anotado |
| M4 alarma con prioridad sobre TTS | ✓ | «Escuchar» pulsado al llegar el reloj a 00:00 (colisión locución/fin): UI «¡Tiempo cumplido!» + «Detener»; `dumpsys audio` con `piid:12927 u/pid:10202 (app) state:started usage=USAGE_ALARM` por altavoz, `STREAM_ALARM` sin silenciar (6/7) y **TTS (uid 10182) sin reproducción activa**; «Detener» → «Listo» 01:00 |
| M2 rotación sin crash ni relocución | ✓ | `user_rotation` 0→1→0: «Paso 1 de 2» + «Voz activa» preservados, sin crash y sin `AudioPlaybackConfiguration state:started` tras la rotación |
| M2 segundo plano sin crash ni relocución | ✓ | HOME 3 s + regreso: estado preservado (mismo Paso, voz activa), sin crash ni TTS |
| M1 revocación de permiso | ✓ | `pm revoke` → reintento de «Voz» → diálogo del sistema → «NO PERMITIR» → chip «Voz» apagado + aviso «Sin permiso de micrófono. Toca «Voz» para volver a intentarlo.» + botones operativos. **Captura 2** `voz-permiso-denegado-es.png` |
| M1 reintento tras conceder | ✓ | `pm grant` → «Voz» → «Voz activa» + «Comandos de voz activos» |
| M3 micrófono apagado al apagar «Voz» | ✓ | `RecordActivityMonitor`: con voz activa `riid 13351; active? true` (sesión `VOICE_RECOGNITION`, uid 10182 = `com.google.android.tts`); tras apagar «Voz», **sin sesiones activas** |
| M3 micrófono apagado al salir («Volver») | ✓ | Voz reactivada (`riid 13455; active? true`, sesión 13697) → «Volver» → Detalle («Empezar Modo Cocina») con **sin sesiones activas** |
| Voz activa visible (captura) | ✓ | **Captura 1** `voz-activa-comandos-es.png`: chip «Voz activa» + barra «Comandos de voz activos» |

Capturas (PNG verificado por cabecera `89 50 4E 47`): `voz-activa-comandos-es.png` (175 KB) y `voz-permiso-denegado-es.png` (174 KB), en esta carpeta.

Alcance del manual: **comandos solo en español a ~30 cm**, decisión del usuario para esta reparación; el inglés queda cubierto por el T0 («next»/«repeat»/«pause timer» 10/10, 10/10, 9/10 online) y por la E2E `englishAppUsesEnglishCommandsAndVisibleTexts`.

## 4. Notas metodológicas (hallazgos de la reparación)

1. **El volumen maestro del PC estaba a 0**: los primeros estímulos no salían por los altavoces (Windows emite por el monitor LG/S/PDIF). Tras subirlo vía `IAudioEndpointVolume`, el reconocimiento funcionó. No es un fallo de la app.
2. **Protocolo de render doble**: los renders cortos (~1,2 s) caen con frecuencia en las ventanas del endpointing del `SpeechRecognizer`; el mismo WAV reproducido 2 veces con ~3 s de separación se reconoce de forma fiable. Además, tras navegar o tocar «Voz» hay que esperar ~7 s porque la locución de la app (TTS) solapa el estímulo.
3. **F9 ground truth**: sobre `material-icons-extended-release-api.jar`, `javap` confirma que `Icons.Outlined.VolumeUp` lleva `@Deprecated` («Use the AutoMirrored version at Icons.AutoMirrored.Outlined.VolumeUp») y que `Icons.Outlined.Mic` **no** está deprecado. `androidx/compose/material/icons/automirrored/outlined/` tiene 138 iconos y **no incluye `Mic`** (sí `VolumeUp/Down/Mute/Off`): usar `Icons.AutoMirrored.Outlined.Mic` rompe la compilación. Por tanto `Mic` queda como `Icons.Outlined.Mic` (sin deprecar, no hay alternativa espejada) y solo `VolumeUp` se migra a `AutoMirrored`. Lint sin advertencias de iconos.
4. **F8 sub-item «voz en pausa»**: el hallazgo cita la «línea 52 (Scenario 6)» de la spec; el texto actual de la spec no contiene «voz en pausa» en ningún punto (Scenario 6, línea 72: «"Escuchar" funciona con la voz apagada»; la línea 52 es el Then del Scenario 2). El sub-item no corresponde al texto actual y no requiere cambio; el resto de F8 sí se aplicó.
5. **`pm revoke` mata el proceso de la app** en este dispositivo (Android 13); al relanzar, la voz arranca apagada («voz solo en memoria», coherente con la spec). La prueba de revocación se hizo sobre ese flujo real (reintento con diálogo del sistema → denegación → aviso in-app).
6. **Flaky conocido**: `PantryManagementE2ETest` falla de forma intermitente en baterías largas («Failed to inject touch input» / timeouts de Compose) y pasa en solitario; verificado de nuevo en esta reparación (3 fallos en 213 → 5/5 en aislado).

## 5. Gates finales (change set completo, 2026-11-06)

- `lintDebug` (`--rerun-tasks`): BUILD SUCCESSFUL, 0 errores (49 warnings de baseline, ninguno nuevo; ninguno de iconos).
- `testDebugUnitTest` (`--rerun-tasks`): BUILD SUCCESSFUL, 40 suites / 291 tests / 0 fallos / 0 omitidos.
- `assembleDebug`: BUILD SUCCESSFUL.

Salidas completas en `gates-2026-11-06/` (`lint-rerun.log`, `unit-rerun.log`, `unit-test-names.txt`, `assemble.log`).
