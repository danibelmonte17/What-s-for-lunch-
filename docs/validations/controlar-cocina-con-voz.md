# Validación independiente — `controlar-cocina-con-voz`

**Fecha:** 2026-11-04
**Validador:** agente `feature-validator` (sesión independiente)
**Veredicto: `revise`** (no `accept`; no `block` — ver §6)
**Spec:** `docs/specs/controlar-cocina-con-voz.md` (S1-S9, D1-D15, T0-T10)
**Árbol:** rama `feature/controlar-cocina-con-voz`, HEAD `0fd7290`, sin commitear

---

## 1. Checks re-ejecutados vs. solo inspeccionados

**Re-ejecutados (tier barato + excepción del tier caro):**

| Check | Comando | Resultado |
|---|---|---|
| Lint | `gradlew :app:lintDebug` | **0 errores / 49 warnings** (línea base; 0 en `voice/`; `tools:ignore="Typos"` justificado) |
| Unit | `gradlew :app:testDebugUnitTest --rerun-tasks` | **40 suites / 291 tests / 0 fallos / 0 omitidos** (incluye `VoiceCommandParserTest` 7/7 y +14 voz en `CookModeViewModelTest`) |
| Build | `gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| Paridad es/en | script de claves | **326/326** (321 strings + 5 plurals) |
| **E2E focalizada** (excepción) | `connectedDebugAndroidTest` con `CookModeVoiceE2ETest,CookModeE2ETest,CookModeTimerE2ETest` | **25/25, 0 fallos** (1m15s, `Pixel_8_Pro` AVD) |

**Solo inspeccionados:**
- Spike T0 (`docs/evidence/controlar-cocina-con-voz/t0-voice-spike.md`): evidencia coherente y bien documentada.
- Batería E2E completa: NO re-ejecutada (los 9 tests spike la harían fallar; ver F3).
- Comprobación manual real: sin evidencia (T9 sin ejecutar).
- Evidencia visual (2 PNG): no existe.

## 2. Cumplimiento de la spec

**S1-S9 conformes** verificado por inspección del diff + E2E con fakes:
- S1: `next` avanza y locuta ✅
- S2: activación → chip «Voz activa» + barra + locución «Paso N. {texto}» + «Temporizador de N minutos» ✅
- S3: `repetir` re-locuta sin avanzar; frases no exactas ignoradas ✅
- S4: `pausar tiempo` → `StepTimerRules.pause`, nunca reanuda ✅
- S5: último paso → `CookModeEvent.Finish` → `onFinish`, voz y escucha detenidas ✅
- S6: «Escuchar» sin micrófono ni escucha ✅
- S7: `RECORD_AUDIO` solo al activar, denegación → aviso + reintento ✅
- S8: es/en con vocabulario del idioma activo ✅
- S9: sin TTS/sin reconocimiento → avisos, sin crash ✅

**D1-D15 respetadas**: parser puro en `domain/voice`, puertos en `data/voice`, Route con permiso/locale/lifecycle, `SharedFlow<Finish>`, `QUEUE_FLUSH`, prioridad de alarma, memoria-only, Koin `factory`.

**Non-goals**: sin dictado en Pinche/Lista, sin wake word/background, solo 3 comandos exactos, sin fuzzy/partiales, sin Room/preferencias, sin dependencias nuevas.

**Cambios de fichero**: todos los previstos presentes. Dos desviaciones justificadas: `RecipeDetailE2ETest.kt` (doble `ActivityResultRegistryOwner`) y ficheros spike de T0 (F3).

**Tareas T1-T8**: completadas. **T0**: parcial (F1). **T9**: parcial (F4). **T10**: hecho, con violación de campo (F5).

## 3. Hallazgos

### F1 — T0 (bloqueante por spec) no completado — verificación requerida ausente
- **Severidad: High**
- **Evidencia**: `t0-voice-spike.md`: en-US 5/5 (sin alcanzar 10 reps), es-ES 1/3 con red (muestra contaminada por `RECOGNIZER_BUSY`), sin autoactivación, sin diálogo de permisos (pre-concesión `pm grant`).
- **Por qué importa**: T0 existe para no certificar una feature de voz cuya precisión real es desconocida; el E2E con fakes (D15) no prueba precisión.
- **Cambio requerido**: completar T0 en dispositivo físico con motor offline es-ES (10 reps por comando es/en ≥8/10, TTS audible, 10 locuciones sin autoactivación, diálogo de permiso). Si no se alcanza → `blocked` + `⚠ S04`.
- **Verificación tras fix**: evidencia con dispositivo/versión/repeticiones/aciertos.

### F2 — Decisión de producto abierta sobre `EXTRA_PREFER_OFFLINE=true` y motor offline es-ES
- **Severidad: Medium**
- **Evidencia**: con `EXTRA_PREFER_OFFLINE=true` (producción) y sin motor offline es, `ERROR 13` → `Unavailable` → voz apagada. Solo funciona con red.
- **Por qué importa**: en español (idioma principal) la voz quedaría inutilizable sin paquete offline. AGENTS.md manda preguntar en temas `⚠`.
- **Cambio requerido**: decisión del usuario: (a) offline-first + aviso de idioma a descargar, o (b) fallback a red con `ERROR 13` (cambio de contrato + spec).

### F3 — Tests spike rompen la batería E2E y el inventario no reconcilia
- **Severidad: Medium**
- **Evidencia**: `T0VoiceSpikeTest.kt` (7 `@Test`) y `T0PermissionSpikeTest.kt` (2 `@Test`) en `androidTest`, marcados «temporal, no se commitea». `e2e.ps1` ejecuta `connectedDebugAndroidTest` sin filtro → batería rota. Inventario reclamado 213 vs real 222 (con spike).
- **Cambio requerido**: retirar los ficheros spike (o `@Ignore`/guarda por argumento) y cuadrar cifras en PROGRESS/historial/evidencia.

### F4 — Evidencia de cierre incompleta
- **Severidad: Medium**
- **Evidencia**: `docs/evidence/controlar-cocina-con-voz/` solo tiene `t0-voice-spike.md`. Faltan: 2 PNG es 1280×2856, batería completa sin regresiones, manual real (permiso, rotación, micrófono apagado al salir, alarma vs TTS, comandos es/en), informe con gates/E2E.
- **Cambio requerido**: ejecutar y conservar los artefactos; redactar informe.

### F5 — `notes` de `feature_list.json` modificado fuera de lo permitido
- **Severidad: Low**
- **Evidencia**: `notes` ampliado más allá del texto original. AGENTS.md §3: solo `status`/`evidence` editables.
- **Cambio requerido**: restaurar `notes` a «Voz con SpeechRecognizer/TextToSpeech nativos (⚠ S04)».

**Notas Low no bloqueantes:**
- N1: `Icons.Outlined.VolumeUp` deprecado → usar `Icons.AutoMirrored.Outlined.VolumeUp`.
- N2: `AndroidStepSpeaker.prepare` retiene motor con init fallido hasta la segunda llamada (sin impacto práctico).
- N3: desviación con `RecipeDetailE2ETest.kt` legítima y documentada.

## 4. Seguridad (checklist de features)

**Sin hallazgos bloqueantes.** Verificado:
- Sin secretos ni claves en diff ni evidencia (sin WAVs ni `inject_audio.py` en el repo).
- Sin persistencia de audio/transcripciones ni `Log` en `data/voice`.
- Transcripts tratados por parser exacto puro sin sinks peligrosos.
- `RECORD_AUDIO` runtime solo al activar «Voz»; `uses-feature microphone required=false`.
- `<queries>` de reconocimiento/TTS declarados.
- Sin dependencias nuevas (gradle intacto).
- Red del servicio de voz declarada en aviso de privacidad es/en (D14).
- Micrófono cortado en `onHostStopped`/`onVoiceDisabled`/`onCleared` — sin escucha en background.

## 5. Documentación y harness

Docs durables **al día y sin contradicciones**: RD24 + 10 casos límite, DESIGN.md, technical-discovery.md, ⚠ S04 actualizado, CONTEXT.md glosario, notas en specs vecinas, PROGRESS.md, historial. `AGENTS.md` intacto. Único defecto: F5.

## 6. Justificación: `revise` y no `block`

- T0 es un spike de validación, no de implementación: el código está completo, es seguro y cumple S1-S9/D1-D15 (verificado independientemente).
- T0 está **incompleto, no fallado**: los datos parciales muestran el pipeline funcionando (en-US 5/5 offline, es-ES con red, TTS OK); el 1/3 de es-ES está contaminado por `RECOGNIZER_BUSY` del entorno.
- `accept` está **vetado** por el Validator Checklist de la spec mientras T0 no tenga evidencia real (≥8/10 por comando, sin autoactivación).
- **Escalamiento obligatorio**: sin dispositivo físico o con T0 fallido → `blocked` + `⚠ S04`.

## 7. Implementation Repair Brief

```md
## Implementation Repair Brief

Goal: cerrar el spike T0 bloqueante y la evidencia de cierre de `controlar-cocina-con-voz`
para poder someterla de nuevo a validación (aceptación vetada hasta entonces).

Do:
1. Pedir confirmación al usuario para usar un dispositivo físico Android (AGENTS §5) y
   registrar la decisión abierta de F2: ¿offline-first con aviso de idioma a descargar
   (mantener `EXTRA_PREFER_OFFLINE=true`) o fallback a red con `ERROR 13` (cambio de
   contrato + spec)? No implementar nada de F2 sin respuesta.
2. Completar T0 en el físico con motor offline es-ES descargado (Google Speech Services →
   Idiomas → Español), protocolo de la spec: 10 repeticiones por comando es/en con la
   configuración de producción, ≥ 8/10 por comando; TTS audible es/en; 10 locuciones de
   anuncio mientras se escucha (autoactivación = 0); diálogo de permiso con
   concesión/denegación/reintento manual. Actualizar
   `docs/evidence/controlar-cocina-con-voz/t0-voice-spike.md` con dispositivo, versión,
   repeticiones y aciertos. Si no se alcanza el umbral o hay autoactivación sin mitigar →
   NO seguir: feature `blocked` + `⚠ S04` actualizado (fin del brief por esa vía).
3. Retirar `T0VoiceSpikeTest.kt` y `T0PermissionSpikeTest.kt` de `app/src/androidTest`
   (son "temporal, no se commitea"; si la sesión de T0 en físico aún los necesita,
   ejecutarlos antes de borrarlos). Tras ello el inventario debe ser 213 `@Test`.
4. Ejecutar la batería E2E completa (`./e2e.ps1`) sin regresiones sobre 213 `@Test`
   (conservar el XML en evidencia) y la comprobación manual real de la Verification Plan
   (permiso, revocación, rotación/segundo plano, micrófono apagado al salir, alarma vs
   TTS, comandos es/en a ~30 cm).
5. Capturar las 2 PNG es 1280×2856 (voz activa con barra de comandos; aviso de permiso
   denegado o voz en inglés) y redactar el informe de
   `docs/evidence/controlar-cocina-con-voz/` con comandos, fechas, conteos y nombres de
   tests. Reconciliar las cifras «213 @Test»/«batería 27/27» de PROGRESS/historial
   (reales: 222 con los spike / 25 de Modo Cocina en el árbol actual).
6. Restaurar `notes` de `feature_list.json` a «Voz con SpeechRecognizer/TextToSpeech
   nativos (⚠ S04)» (solo `status`/`evidence` son editables) y actualizar `evidence`.

Do not:
- Cambiar la semántica de comandos, el parser, la prioridad de alarma ni el alcance
  (sin dictado en Pinche/Lista, sin wake word, sin background, sin Room/IA/prompts).
- Resolver F2 por tu cuenta si el usuario no ha decidido (⚠ S04).
- Ejecutar nada en dispositivo físico sin confirmación humana explícita.

Run:
- `gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug` (0 errores, 49
  warnings línea base, unit 291/0 o +N según cambios de F2)
- paridad es/en (326/326 + claves nuevas si F2 las introduce)
- `gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=
  com.whatsforlunch.app.CookModeVoiceE2ETest,com.whatsforlunch.app.CookModeE2ETest,
  com.whatsforlunch.app.CookModeTimerE2ETest` (25/25 o +N)
- `./e2e.ps1` (213 `@Test`, sin regresiones)

Update:
- `docs/evidence/controlar-cocina-con-voz/` (t0-voice-spike.md + informe + 2 PNG)
- `docs/risks-and-open-questions.md` (⚠ S04 + decisión de F2)
- `PROGRESS.md` + `docs/progress-history.md` (cifras reconciliadas)
- `feature_list.json` (solo `status`/`evidence`; `notes` restaurado)
- spec `docs/specs/controlar-cocina-con-voz.md` solo si F2 cambia el contrato
```

## 8. Estado recomendado

- `feature_list.json`: mantener `status: in_progress`. `notes` restaurado (F5 aplicado por orquestador).
- Tras repair brief + T0 completo ≥8/10 sin autoactivación → revalidación → `accept` → `accepted`.
- Sin dispositivo físico o T0 fallido → `blocked` + `⚠ S04`.

---

## 9. Revalidación tras completar T0 (2026-11-06, `feature-validator` con agente): `revise`

**Contexto**: el repair brief de §7 se aplicó — T0 completado y **SUPERADO** el 2026-11-06 en el dispositivo físico V2124 (evidencia: `docs/evidence/controlar-cocina-con-voz/t0-voice-spike.md` + 3 PNG), decisión F2 implementada, spike temporal retirado sin commitear y `⚠ S04` resuelto. El validador corrió en una sesión independiente, **sin editar ficheros**.

**Veredicto: `revise`** (sin Critical; 1 High, 2 Medium, 1 Low). La feature vuelve a `in_progress`.

### Checks ejecutados por el validador

| Check | Método | Resultado |
|-------|--------|-----------|
| `lintDebug` | re-ejecutado (`--rerun-tasks`) | 0 errores / 49 warnings (baseline; sin novedades de voz) |
| `testDebugUnitTest` | re-ejecutado (`--rerun-tasks`) | 40 suites / 291 tests / 0 fallos / 0 omitidos |
| `assembleDebug` | re-ejecutado | OK |
| Paridad es/en | inventario de claves | 322 strings + 5 plurals = 327 por idioma ✅ |
| Inventario E2E | contado en `androidTest` | 213 `@Test` (24 clases); focalizada voz = 25 |
| Spike T0 físico | inspeccionado (evidencia + 3 PNG) | es 10/10-10/10-9/10; en 10/10-10/10-9/10 ≥ umbral; autoactivación 0/10; TTS audible; permisos ✅ |
| E2E focalizada 25/25 y batería Modo Cocina 27/27 | inspeccionado (logs de sesión; sin XML conservado) | creíble pero incompleto → F6/F7 |
| Batería E2E completa | no ejecutada para este change set | → F6 |
| Manual de uso | sin evidencia | → F7 |

Seguridad: **sin hallazgos** (sin audio/transcripciones persistidas, sin secretos/PII en el diff, `RECORD_AUDIO` mínimo y diferido, micrófono `required="false"`, privacidad actualizada, `<queries>` acotado).

### Hallazgos

- **F6 (High)** — La batería E2E completa requerida por la spec no se ejecutó sobre este change set y la evidencia lo sobredimensiona: `feature_list.json.evidence` y `PROGRESS.md` afirman «E2E 213 `@Test` (sin regresiones)», pero **213 es el inventario de tests, no una ejecución**. Lo realmente ejecutado: focalizada 25/25 (10 `CookModeVoiceE2ETest` + 7 `CookModeE2ETest` + 8 `CookModeTimerE2ETest`) y batería Modo Cocina 27/27 (7+8+12 = `RecipeDetailE2ETest`); ninguna conserva XML/HTML en `docs/evidence/`.
- **F7 (Medium)** — Evidencia de cierre incompleta (residual de F4 y del «Evidence To Capture» de la spec): faltan (a) captura es de la UI con voz activa + barra de comandos; (b) captura es de un aviso (permiso denegado o motor no descargado) — `t0-perm-denied-state.png` muestra los ajustes TTS del sistema, no el estado denegado de la app; (c) checklist del manual con resultados (revocación en Ajustes, rotación/segundo plano, micrófono apagado al salir, alarma vs TTS, comandos a ~30 cm); (d) salidas de lint/unit/E2E con nombres de tests; (e) informe de cierre que referencie T0 + E2E + manual.
- **F8 (Medium)** — Spec desincronizada tras el cambio F2 (el repair brief exigía actualizarla si cambia el contrato; cambió): la spec no refleja `VoiceSourceEvent.LanguageUnavailable` ni `VoiceNotice.LANGUAGE_NOT_DOWNLOADED` (ERROR 13 → aviso accionable, string `cook_mode_voice_language_not_downloaded`; 3 fallos → `COMMANDS_UNAVAILABLE`). Línea 131 enumera solo `{ PERMISSION_DENIED, VOICE_UNAVAILABLE, COMMANDS_UNAVAILABLE }`; la lista de strings (línea 155) omite la clave nueva; Technical Approach y D2 dicen «idioma no soportado o 3 fallos → `Unavailable`»; la línea 52 (Scenario 6) dice «voz en pausa»; «Casos límite» sin fila de motor de idioma.
- **F9 (Low)** — Icono deprecado heredado (N1 del informe previo): `CookModeScreen.kt` usa `Icons.Outlined.Mic`/`Icons.Outlined.VolumeUp` en lugar de `Icons.AutoMirrored.Outlined.*`; warnings de lint. Sin impacto funcional.

### Notas

- **N1**: la medición T0 de precisión en-US es **online** (decisión del usuario registrada) pese a `EXTRA_PREFER_OFFLINE=true`; motor en-US no descargado (ERROR 13 = caso F2). Acotado y documentado.
- **N2**: cifras reconciliadas por inventario: 213 `@Test` = cifra de PROGRESS; «27/27» = 7+8+12 (falta etiquetar el 12/12 como `RecipeDetailE2ETest` en PROGRESS/historial); «25/25» = 10+7+8 (focalizada voz). Todo coherente.
- **N3**: `t0-perm-dialog-request.png` capta el diálogo sobre los ajustes TTS del sistema (fondo inesperado); cosmético.
- **N4**: `AndroidStepSpeaker.prepare` con init fallido retiene el motor fallido hasta la segunda llamada (sin `releaseEngine()` en ese path); sin impacto funcional conocido.
- **N5**: los checkboxes T0–T10 del «Validator Checklist» de la spec quedan cubiertos por esta validación (la spec no se edita en validación).

### Implementation Repair Brief

1. **Ejecutar y conservar la verificación E2E completa** sobre el árbol final (213 `@Test`): focalizada `CookModeVoiceE2ETest`+`CookModeE2ETest`+`CookModeTimerE2ETest` + batería completa (`./e2e.ps1`/`./e2e.sh`) sin regresiones; copiar las salidas XML/HTML con nombres de tests a `docs/evidence/controlar-cocina-con-voz/`. Si algo preexistente falla (p. ej. `PantryManagementE2ETest`, flaky conocido), repetirlo y documentar la causa.
2. **Completar la evidencia de cierre** (F7): informe en `docs/evidence/controlar-cocina-con-voz/` que referencie T0 + E2E + manual; 2 capturas es (voz activa con barra de comandos; aviso de permiso denegado/motor no descargado); checklist del manual con resultados.
3. **Sincronizar la spec con el contrato F2** (F8): `VoiceSourceEvent.LanguageUnavailable`, `VoiceNotice.LANGUAGE_NOT_DOWNLOADED`, string `cook_mode_voice_language_not_downloaded`, ERROR 13 → `LANGUAGE_NOT_DOWNLOADED` vs 3 fallos → `COMMANDS_UNAVAILABLE`, fila en «Casos límite», y ajustar D2, Technical Approach, línea 52 y la lista de strings.
4. **Cuadrar el wording de cifras**: distinguir inventario (213 `@Test`) de ejecuciones (25/25 = 10+7+8; 27/27 = 7+8+12 con `RecipeDetailE2ETest` etiquetado) en `feature_list.json.evidence`, `PROGRESS.md` e historial.
5. **(Opcional, F9)** `Icons.AutoMirrored.Outlined.Mic/VolumeUp` en `CookModeScreen`.

No tocar: semántica de comandos ni de alarma, alcance (sin parciales/fuzzy/persistencia) ni los contratos ya verificados.

### Estado

- `feature_list.json`: `status` → `in_progress` con la referencia a esta validación en `evidence`.
- Tras el repair + revalidación acotada → `accept` → `accepted`.

## 10. Revalidación tras el repair brief (sesión 102d, 2026-11-06, `feature-validator` con agente): `accept`

**Contexto**: revalidación acotada tras el `revise` de §9. El repair brief (F6–F9+F4) se aplicó en la sesión 102c sobre el V2124 físico (commits `1ea78b9` fix + `a651db8` docs; `353d390..HEAD`), con informe en `docs/evidence/controlar-cocina-con-voz/informe-de-cierre-2026-11-06.md`. El validador corrió en sesión independiente, sin editar ficheros.

**Veredicto: `accept`** (sin Critical/High/Medium; 2 Low nuevos de wording de cifras, corregidos al persistir). Estado recomendado: `accepted`.

### Checks ejecutados por el validador

| Check | Método | Resultado |
|-------|--------|-----------|
| `lintDebug --rerun-tasks` | re-ejecutado | exit 0; 0 errores / 49 warnings (baseline; sin warnings de iconos) |
| `testDebugUnitTest --rerun-tasks` | re-ejecutado | exit 0; 40 suites / 291 tests / 0 fallos / 0 omitidos (reproduce `unit-test-names.txt`) |
| `assembleDebug` | re-ejecutado | BUILD SUCCESSFUL |
| E2E focalizada 25/25 | inspeccionado (XML/HTML conservados) | `e2e-focused-2026-11-06`: `tests=25 failures=0 skipped=0`, `19:34:21`, device `V2124 - 13`; 10 `CookModeVoiceE2ETest` + 7 `CookModeE2ETest` + 8 `CookModeTimerE2ETest` |
| E2E batería completa 213 | inspeccionado | `e2e-full-2026-11-06`: `tests=213 failures=3 errors=0 skipped=2` (19:41:14); 3 fallos solo en `PantryManagementE2ETest` (2 × «Failed to inject touch input» + 1 × `ComposeTimeoutException`), 2 omitidos `SettingsE2ETest`/`Assume` |
| Re-ejecución del flaky | inspeccionado | `e2e-pantry-rerun-2026-11-06`: 5/5 verde (19:42:17) |
| Checklist manual + 2 capturas | inspeccionado | informe §3 con resultados por ítem (adb); PNG con cabecera `89 50 4E 47` válida |
| Comprobación `verification` | inspeccionado | `nextTranscriptAdvancesTheStepAndAnnouncesIt` verde en focalizada y batería + manual M5-a 3/3 |

Seguridad: **sin hallazgos** (sin secretos/PII/identificadores del dispositivo en repo ni evidencia; sin audio/transcripciones persistidos; diff acotado a evidencia/docs + una línea de icono; semántica de comandos/alarma y alcance intactos: sin parciales/fuzzy/persistencia).

### Cierre del repair brief (§9)

- **F6 (High) — cerrado**: batería completa 213 ejecutada sobre el change set en el V2124 (2026-11-06) con XML/HTML con nombres de test conservados en `e2e-focused-`/`e2e-full-`/`e2e-pantry-rerun-2026-11-06/`; flaky `PantryManagementE2ETest` documentado y re-ejecutado aislado 5/5; «213 @Test» ya presentado como inventario, no como ejecución.
- **F7 (Medium) — cerrado**: `informe-de-cierre-2026-11-06.md` (T0+E2E+manual) + checklist manual instrumentado por adb con resultado por ítem (M5-a 3/3, M5-b 3/3, M5-c 4/5 con fallo aislado anotado, M4 alarma `USAGE_ALARM` con TTS parado, M2 rotación/segundo plano, M1 revocación→aviso→reintento, M3 micrófono apagado al desactivar y al salir) + 2 capturas es (`voz-activa-comandos-es.png`, `voz-permiso-denegado-es.png`) + salidas de gates en `gates-2026-11-06/`.
- **F8 (Medium) — cerrado**: spec sincronizada con el contrato F2 (`VoiceSourceEvent.LanguageUnavailable`, `VoiceNotice.LANGUAGE_NOT_DOWNLOADED`, string `cook_mode_voice_language_not_downloaded`, ERROR 13 → `LanguageUnavailable` vs 3 fallos → `Unavailable`, fila en «Casos límite», Technical Approach/strings) y verificada contra código y paridad es/en. El sub-item «voz en pausa» se confirmó **no correspondiente** al texto de la spec (ni actual ni en `353d390`); la nota del informe §4.4 es correcta.
- **F9 (Low) — cerrado**: `VolumeUp` → `Icons.AutoMirrored.Outlined.VolumeUp` (deprecado, ground truth con `javap`); `Mic` se queda en `Icons.Outlined.Mic` (no deprecado; `AutoMirrored.Outlined.Mic` no existe entre los 138 iconos espejados) — documentado y sin warnings de lint.
- **F4 — cerrado**: 25/25 = 10+7+8 y 27/27 = 7+8+12 con `RecipeDetailE2ETest` 12/12 etiquetado (coherentes en informe/PROGRESS/historial/`feature_list.json` y contra los XML); 213 = inventario. Residuales menores → F10/F11.

### Hallazgos nuevos

- **F10 (Low)** — el desglose de la batería no reconcilia: se declara «210 verdes, 3 fallos, 2 omitidos» (= 215) pero el XML conservado declara `tests=213 failures=3 skipped=2` → **208 verdes + 3 fallos + 2 omitidos = 213**. Corregido al persistir en `informe-de-cierre-2026-11-06.md` §1/§2, cabecera de `PROGRESS.md`, §102c del historial y `evidence` de `feature_list.json`.
- **F11 (Low)** — «24 clases» es el número de ficheros `.kt` de `androidTest`, no de clases de test (el run ejecutó **26 clases**; 22 ficheros con `@Test` + fakes/soporte = 24 ficheros). Corregido al persistir a «213 `@Test` (26 clases de test; 24 ficheros en `androidTest`)» en los mismos cuatro artefactos.

### Notas

- **N6**: capturas 1080×2408 (nativa del V2124) frente a los «1280×2856» de la spec (figura del AVD); cabecera válida y estados correctos. La resolución de la spec es entorno-dependiente; candidata a suavizarse en un mantenimiento futuro, sin rehacer evidencia.
- **N7**: «1 m 14 s»/«48 s» del informe son wall-clock de comando (con instalación); los `time` del XML son 45,85 s/28,83 s de ejecución. Relojes distintos, no contradicción.
- **N8**: los logs de `gates-2026-11-06/` están en UTF-16 (git los trata como binarios); legibles y correctos.
- **N9**: los XML/HTML conservados no contienen el número de serie ni identificadores del dispositivo (solo `device = "V2124 - 13"`); artefactos con serie ya podados por el implementador.

### Estado

- `feature_list.json`: `status` → `accepted`, con F10/F11 corregidos en el wording de `evidence` al persistir (igual que PROGRESS/historial/informe de cierre).
- Feature cerrada: el contrato de voz (comandos exactos, avisos, permiso diferido, sin persistencia, prioridad de alarma) queda validado con T0 físico + E2E conservada + manual instrumentado.
