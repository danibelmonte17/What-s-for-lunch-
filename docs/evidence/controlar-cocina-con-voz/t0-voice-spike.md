# Spike T0 — Precisión de voz (controlar-cocina-con-voz)

**Fecha:** 2026-11-06 (sesión definitiva, dispositivo físico)
**Dispositivo:** V2124, Android 13 (SDK 33), build `TP1A.220624.014 release-keys`, conexión adb WiFi
**Sistema operativo host:** Windows 11
**Versión APK:** build `debug` sin commitear (branch `feature/controlar-cocina-con-voz`, HEAD `0fd7290`)

## Método

### Estímulos (decisión del usuario)

- **Solo estímulos sintéticos**: la voz humana real se descartó expresamente para esta medición.
- Los estímulos se **reproducen por el altavoz del propio dispositivo** mientras `SpeechRecognizer` escucha por el micrófono, en sesiones consecutivas con la misma forma que el adaptador de producción (`AndroidVoiceCommandSource`).
- **Render**: TTS Google del propio dispositivo (voces genéricas `es-ES-language` / `en-US-language` de `com.google.android.tts`), texto = frase canónica del comando, **ritmo 0,7** (`setSpeechRate`). Justificación del ritmo lento en *Limitaciones*.
- 10 WAV por comando («Siguiente», «Repetir», «Pausar tiempo» / «Next», «Repeat», «Pause timer»), más 10 locuciones de anuncio por idioma para autoactivación.
- Los renders SAPI del PC (Microsoft Helena/Zira) se usaron en fases de diagnóstico y se descartaron para la batería final (ver *Limitaciones*).

### Reconocimiento de voz

- **API:** `android.speech.SpeechRecognizer` con el intent de producción: `LANGUAGE_MODEL_FREE_FORM`, `EXTRA_LANGUAGE` (es-ES/en-US), `EXTRA_MAX_RESULTS=1`, `EXTRA_PARTIAL_RESULTS=false`.
- **Modo:** español con `EXTRA_PREFER_OFFLINE=true` (configuración de producción; motor offline es-ES disponible en este dispositivo). Inglés con `EXTRA_PREFER_OFFLINE=false` (online): el motor offline en-US **no está descargado** (ver *Estado de motores* y caso F2).
- **Evaluación:** cada transcripción final se evalúa con el parser real del dominio `VoiceCommandParser.parse()` (frases exactas, D7) y se compara con el comando esperado.
- **Condiciones:** volumen de medios al máximo, pantalla despierta (`screen_off_timeout` ampliado), `RECORD_AUDIO` concedido al proceso de la app.

### TTS audible

- `TextToSpeech.speak()` con las locuciones de Paso en es/en, callback `onDone` y **captura simultánea del micrófono** (`AudioRecord`) como evidencia instrumental de la salida por altavoz.

### Diálogo de permisos

- Petición `RECORD_AUDIO` sobre `MainActivity` con la misma API que usa `CookModeRoute` (`ActivityResultContracts.RequestPermission`), interactuando con el diálogo del sistema vía adb (`uiautomator dump` + `input tap`).

## Resultados

### Precisión de reconocimiento (umbral ≥ 8/10 por comando)

| Comando | Idioma | Modo | Repeticiones | Aciertos | Precisión | ¿Pasa? |
|---------|--------|------|-------------|----------|-----------|--------|
| «Siguiente» | es-ES | offline | 10 | 10 | 100% | ✅ |
| «Repetir» | es-ES | offline | 10 | 10 | 100% | ✅ |
| «Pausar tiempo» | es-ES | offline | 10 | 9 | 90% | ✅ |
| «Next» | en-US | online | 10 | 10 | 100% | ✅ |
| «Repeat» | en-US | online | 10 | 10 | 100% | ✅ |
| «Pause timer» | en-US | online | 10 | 9 | 90% | ✅ |

Fallos aislados (aceptables bajo el umbral): es «Pausar tiempo» rep5 → transcripción `tiempo` (falso negativo); en «Pause timer» rep6 → transcripción `Alzheimer`. El resto de transcripciones finales coinciden literalmente con la frase canónica.

### Sin autoactivación (10 locuciones de anuncio por idioma)

| Idioma | Locuciones | Autoactivaciones | Detalle |
|--------|-----------|------------------|---------|
| es-ES | 10 | **0** ✅ | Transcripciones tipo `paso uno`, `paso uno corta las patatas temporizador de 5 minutos` — ninguna parsea como comando |
| en-US | 10 | **0** ✅ | Transcripciones tipo `step one dice the potatoes timer for 5 minutes` — ninguna parsea como comando (2/10 sin transcripción) |

### TTS audible (es/en)

| Idioma | `onDone` | Energía capturada por micrófono durante la locución | ¿Pasa? |
|--------|----------|-----------------------------------------------------|--------|
| es-ES | ✅ | ✅ (múltiples tramos con RMS alto durante toda la locución) | ✅ |
| en-US | ✅ | ✅ (múltiples tramos con RMS alto durante toda la locución) | ✅ |

### Diálogo de permiso (concesión / denegación / reintento)

| Paso | Comprobación | Resultado |
|------|-------------|-----------|
| 1. Petición sin permiso | Aparece el diálogo del sistema «¿Permitir que What's for Lunch? grabe audio?» con las 3 opciones (Android 13) | ✅ [`t0-perm-dialog-request.png`](t0-perm-dialog-request.png) |
| 2. Denegación («NO PERMITIR») | `RECORD_AUDIO: granted=false` (flag `USER_SET`); el estado interno del test pasa a denegado | ✅ [`t0-perm-denied-state.png`](t0-perm-denied-state.png) |
| 3. Reintento (segunda petición) | El diálogo vuelve a mostrarse tras la denegación (sin «no volver a preguntar» bloqueante) | ✅ [`t0-perm-dialog-retry.png`](t0-perm-dialog-retry.png) |
| 4. Concesión («MIENTRAS SE USA LA APLICACIÓN») | `RECORD_AUDIO: granted=true` | ✅ |

### Estado de motores de reconocimiento en el dispositivo

| Motor | Estado | Evidencia |
|-------|--------|-----------|
| Offline es-ES | ✅ Disponible | La batería es completa con `EXTRA_PREFER_OFFLINE=true` |
| Offline en-US | ❌ No descargado | `ERROR 13` (LANGUAGE_UNAVAILABLE) en 10/10 con `EXTRA_PREFER_OFFLINE=true` → **caso F2**: la app muestra el aviso `LANGUAGE_NOT_DOWNLOADED` (decisión F2 ya implementada) |
| Online en-US | ✅ Disponible | Batería en medida con `EXTRA_PREFER_OFFLINE=false` (aprobado por el usuario para esta medición) |

## Limitaciones y hallazgos de los estímulos sintéticos

1. **Renders SAPI del PC rechazados para «Repetir» (es):** el motor offline es-ES devuelve `ERROR 7` sistemático (0/23) con los renders SAPI de «Repetir», con `onBeginningOfSpeech` pero sin parciales; el mismo motor reconoce renders del TTS del dispositivo al 100%. Los renders SAPI de «Siguiente»/«Pausar tiempo» sí funcionan (10/10). Se usan renders del TTS del dispositivo para toda la batería final.
2. **El endpointing corta los renders a velocidad normal:** con renders a ritmo 1,0, frases cortas («Pause timer», «Next») finalizan prematuramente tras el primer fragmento (parcial `pause` → `ERROR 7`) con tasas ~0/10. A ritmo 0,7 la misma frase finaliza correctamente (3/3 y 10/10). Consecuencia metodológica: **todos los estímulos de la batería final van a ritmo 0,7**, salvo «Repeat» en-US a 0,85 (a 0,7 el render lento provoca la transcripción duplicada `repeat repeat` en 2/10; a 0,85 es 10/10).
3. **`ERROR 9` (RECOGNIZER_BUSY) = permiso ausente en el proceso de la app:** la instrumentación corre en el proceso `com.whatsforlunch.app`; sin `RECORD_AUDIO` concedido a ese proceso, `SpeechRecognizer` falla con error 9 antes de escuchar. Requiere `pm grant com.whatsforlunch.app android.permission.RECORD_AUDIO` tras reinstalar.
4. **Throttling del servicio online:** sesiones online muy rápidas y consecutivas degradan el servicio (fallos instantáneos `ERROR 7` sin `onBeginningOfSpeech`), recuperables con descanso. La batería final se midió con el servicio operativo (los controles en/es alternados confirmaron la salud del servicio durante la tirada).
5. **Pantalla apagada = micrófono bloqueado:** en Android 12+ el recognizer falla si el dispositivo duerme; se fijó `screen_off_timeout=1800000` y se mantuvo la pantalla despierta durante las tiradas.

## Evidencia disponible

- 3 capturas PNG del flujo de permisos en esta misma carpeta (único material gráfico; **no se commitean grabaciones de audio**).
- Logs de logcat con tag `T0Voice`: `ACC rep/ACC_SUMMARY` (precisión, con transcripción final y parcial por repetición), `AUTO ann/AUTO_SUMMARY` (autoactivación), `TTS_DONE/TTS_CAPTURE` (TTS audible + energía), `PERM tick/PERM_DONE` (flujo de permisos).
- WAVs de estímulos y capturas PCM en `$env:TEMP\wfl-t0\` (temporales, no se commitean).
- Test instrumental `T0PhysicalSpikeTest.kt` (temporal, **no commiteado**).

## Conclusión

**T0 SUPERADO en dispositivo físico.** Se cumple el contrato de T0:

- ✅ Precisión ≥ 8/10 en los 3 comandos × 2 idiomas (mínimo observado 9/10).
- ✅ TTS audible en es/en (callback + energía capturada por micrófono).
- ✅ Sin autoactivación en 10 locuciones por idioma (0/10 y 0/10).
- ✅ Diálogo de permiso con concesión, denegación y reintento verificados sobre la UI real.

Observaciones para la implementación:

- La medición en inglés es **online** porque el motor offline en-US no está descargado en el dispositivo (caso F2, aviso `LANGUAGE_NOT_DOWNLOADED` ya implementado). Con el motor offline descargado habría que repetir la medición en-US en modo offline.
- Los estímulos sintéticos requieren ritmo lento (0,7–0,85) para no perder frases cortas por el endpointing; con voz humana este efecto no tiene por qué darse, pero queda registrado por si la precisión real en manual difiere.

## Intento previo en emulador (2026-11-04) — incompleto

Sesión anterior en AVD `Pixel_8_Pro` (android-37) con inyección de audio por gRPC `injectAudio` y estímulos SAPI: se validó el pipeline (en-US 5/5, es-ES 1/3 con red) pero `injectAudio` hacía caer el emulador y no se completaron las repeticiones ni la autoactivación; el motor offline es-ES no existía en el AVD. La recomendación de esa sesión (pasar a dispositivo físico) es la ejecutada aquí.
