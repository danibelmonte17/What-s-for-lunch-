# Escanear la Despensa con IA — informe de implementación

> **Estado**: implementación autoverificada por `feature-implementer` (2026-10-22). **No es aceptación**; queda pendiente la validación independiente con `feature-validator`. La verificación manual con foto real y llamada de pago (verify 3) **no se ha ejecutado** por requerir confirmación humana explícita.

## Alcance implementado

- Captura CameraX de una foto **en memoria** (JPEG, lado largo ≤ 1024 px, calidad 85) desde el visor; la foto nunca se escribe en disco ni en MediaStore (RD06/D6).
- Cliente `OpenAiCompatibleScanImageAnalyzer` sobre `POST <base>/chat/completions`, alias `chat`, payload multimodal OpenAI-compatible (`data:image/jpeg;base64`), `temperature = 0`, prompt en el idioma activo (es/en).
- Parser tolerante a vallas ```` ```json ```` y texto alrededor del primer objeto JSON; respuesta ilegible, HTTP ≠ 2xx o error de red → `Failed`; sin key → `MissingApiKey`.
- Normalización pura en `ScanAnalysis`: D1 (fusión por nombre normalizado sin pisar ediciones), D2 (cantidad no entera/≤ 0 → 0; > 99 → 99; nombres vacíos descartados; duplicados internos fusionados).
- UI: botón «Escanear alimentos» ≥ 48 dp, progreso «Analizando la foto…», avisos de key ausente y fallo con entrada manual; sin HUD ni confianza. Se conservan chips editables, stepper 0..99, marca «Ya está en la Despensa», suma RD13, confirmaciones de salida/re-escaneo y Recetas Compatibles.
- MediaPipe, su asset `.tflite`, sus dependencias y toda la pila de detección en vivo/HUD/confianza retirados.
- `settings_privacy_notice` reescrito: la foto se envía a DevExpert Inference para análisis y no se almacena en el dispositivo.
- Docs actualizados: `docs/domain-model.md` (Escaneo, RD06 reformulada, RD12 derogada), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (`RK02` cerrada, `RK03` reescrita), `DESIGN.md` y notas de revisión en las specs `escanear-despensa-con-camara` y `detectar-alimentos-en-vivo`.

## Gates ejecutados

| Gate | Resultado |
|---|---|
| `init.ps1` (AVD `Pixel_10_Pro` API 37) | **exit 0** — entorno listo, app instalada y en ejecución (`.harness/init-sesion60.log`) |
| `gradlew lintDebug` | **BUILD SUCCESSFUL**, 0 errores |
| `gradlew testDebugUnitTest --rerun-tasks` | **94/94 en 17 suites**, 0 fallos/errores/omitidos |
| `gradlew assembleDebug` | **BUILD SUCCESSFUL** |
| `ScanPantryE2ETest` focalizada | **19/19**, 0 fallos (`aiPhotoShowsChipsWithQuantitiesAndSavesThemToPantry` corregida para leer `EditableText`) |
| Barrido de restos | `MediaPipe`, `mediapipe`, `efficientdet`, `FoodDetector`, `mergeLiveDetections`, `LIVE_SAMPLE_INTERVAL_MS`, `MIN_CONFIDENCE`, `food_label_`, `scanner_confidence`, `scanner_hud_` → **0 hits** en `app/src` y `gradle/` |
| Comprobación manual RD05 (AVD `Pixel_10_Pro` API 37) | App sin `AI_*` + cámara concedida → aviso de API key ausente y alta manual disponible; captura `scanner-degradacion-es.png` |

Unit nuevos o reescritos: `ScanAnalysisTest` 6/6, `ScanAnalysisParserTest` 6/6, `ScannerViewModelTest` 11/11, `ScanDetectionsTest` 8/8, `AiEndpointsTest` 6/6.

## Recorrido de escenarios

- **S1 (verify 1)**: `aiPhotoShowsChipsWithQuantitiesAndSavesThemToPantry` — foto simulada con `{"manzana":3,"nata":1}` → chips Manzana/Nata con cantidades 3/1 → guardar → Despensa con esas cantidades. **E2E en verde**.
- **S2 (verify 2)**: `missingApiKeyShowsNoticeAndKeepsManualFlowAvailable` — aviso de key y alta/guardado manual sin bloqueo. **E2E en verde**.
- **S3 (verify 2)**: `analysisFailureShowsNoticeAndTheNextPhotoCanRetry` — aviso de fallo, lista editable y reintento con otra foto. **E2E en verde**.
- **S4**: `blankNamesAreDiscardedAndInvalidQuantitiesAreNormalized` — nombres vacíos descartados, cantidades 0,5 → 0, 150 → 99, ausente → 0. **E2E en verde**.
- **S5**: `successivePhotosAccumulateWithoutOverwritingEditedChips` — segunda foto no pisa la cantidad editada. **E2E en verde**.
- **S6**: `chipsCanBeRemovedRenamedAndAddedManually`, `saveWithPantryMatchesNeverShowsDecisionDialog`, `saveAppliesAgreedQuantityEdgeCasesWithoutNewRows`. **E2E en verde**.
- **S7**: `rescanningWithUnsavedChipsRequiresConfirmationAndClearsTheTray` y salida con confirmación. **E2E en verde**.
- **S8**: `englishLabelsAndPromptLanguageFollowTheActiveLocale` — textos y prompt en el idioma activo. **E2E en verde**.
- **S9/RD06**: `captureIsNeverWrittenToCacheOrFiles` — sin imágenes nuevas en `cacheDir`/`filesDir`; `CameraXScanImageSourceTest` verifica JPEG en memoria. **E2E en verde**.

## Verificación manual

- **verify 3** (foto real + llamada de pago a DevExpert Inference, es/en): **pendiente** de confirmación humana. No se ha realizado ninguna llamada real.
- **Captura en español de la degradación (RD05/S2)**: `docs/evidence/escanear-despensa-con-ia/scanner-degradacion-es.png`, tomada en el AVD `Pixel_10_Pro` API 37 el 2026-10-22 con la app instalada sin `AI_*` (`local.properties` solo con `sdk.dir`) y permiso de cámara concedido. Tras pulsar «Escanear alimentos» se ve el aviso «Configura tu API key en Ajustes para analizar fotos con IA…» y el alta manual disponible, sin ninguna llamada de red ni coste.
- **Captura de chips de un análisis real**: **pendiente** junto con el verify 3, porque exige una llamada real de pago; los chips propuestos por la IA están cubiertos por la E2E con cliente simulado (`S1`). No se añaden capturas simuladas para no presentarlas como evidencia real.

## Incidencias de entorno

- El primer pase completo de `connectedDebugAndroidTest` falló por una aserción de test propia (`assertTextEquals` sobre un campo editable), corregida.
- Pases posteriores de la suite completa sufrieron los flakes conocidos del AVD: `EditRecipeE2ETest` («Activity never becomes requested state DESTROYED») y `SearchAndFilterRecipesE2ETest` con `INSTRUMENTATION_ABORTED: System has crashed`, ambos ajenos a esta feature. Los tests focalizados de la feature y del test afectado pasan.

---

# Validación independiente (`feature-validator`, 2026-10-22)

> **Veredicto: `accept`.** Feature `escanear-despensa-con-ia` (`passing` → recomendado `accepted`). Sin hallazgos Critical/High/Medium; 4 notas Low no bloqueantes y 1 nota adicional. La verificación 3 (foto real + llamada de pago) queda **declarada pendiente** por falta de autorización humana, como permite el plan de verificación de la spec.

## Checks reranqueados sobre el árbol actual (sin commitear)

| Check | Resultado | Fecha/entorno |
|---|---|---|
| `init.ps1` (wrapper `cmd /c` + `JAVA_HOME` + `PATH` del SDK) | **exit 0** en el tercer intento (dos falsos fallos conocidos de `pidof` tras `installDebug`; `adb shell pidof` manual → `3775` y `topResumedActivity=MainActivity`) | AVD `Pixel_10_Pro` API 37, `emulator-5554`, 2026-10-22 |
| `gradlew lintDebug testDebugUnitTest --rerun-tasks assembleDebug` | **BUILD SUCCESSFUL** (1 m 5 s) | 2026-10-22 19:36 |
| `lintDebug` | **0 errores**, 48 warnings (`lint-results-debug.xml`) | idem |
| `testDebugUnitTest --rerun-tasks` | **94/94 en 17 suites**, 0 fallos/errores/omitidos (recuento desde XML) | idem |
| `assembleDebug` | APK generado (`app-debug.apk`, 70.883.523 B) | idem |
| `connectedDebugAndroidTest` focalizada `SearchAndFilterRecipesE2ETest` | **8/8, 0 fallos** (`BUILD SUCCESSFUL`, 37 s) — cierra como flake ambiental el único fallo del último pase completo | AVD `Pixel_10_Pro` API 37, 2026-10-22 |
| Barrido de restos en `app/src` y `gradle/` (`MediaPipe`, `mediapipe`, `efficientdet`, `FoodDetector`, `mergeLiveDetections`, `LIVE_SAMPLE_INTERVAL_MS`, `MIN_CONFIDENCE`, `food_label_`, `scanner_confidence`, `scanner_hud_`, `LiveDetectionHud`, `liveDetections`, `liveStopped`, `isDetecting`, `scanner_detection_*`, `scanner_live_hint`, `DetectedFood`, `confidence`) | **0 hits** | idem |
| Paridad de cadenas es/en | **188/188**, sin claves huérfanas | idem |
| `local.properties` / secretos | solo `sdk.dir`; `git grep` de claves reales → solo el valor sintético preexistente `sk-persisted` en un unit de Ajustes | idem |

## Evidencia cara inspeccionada (no reranqueada por defecto)

- `ScanPantryE2ETest` focalizada **19/19, 0 fallos** (`.harness/e2e-scan-focused-sesion60.log`, 2026-10-22 18:15, AVD `Pixel_10_Pro` API 37). Fuentes más recientes del árbol: `ScanPantryE2ETest.kt` 18:13 → el pase es **posterior al diff**; el árbol no cambió después (solo docs).
- Suite completa, tres pasadas documentadas en `.harness/`:
  - `e2e-sesion60.log` (5 m 13 s): 112/113, 1 omitido (`SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete`, solo aplica con `AI_*`) y 1 fallo **propio** en `aiPhotoShowsChipsWithQuantitiesAndSavesThemToPantry` (`assertTextEquals` sobre campo editable) → corregido y verificado en el pase focalizado posterior.
  - `e2e-sesion60-retry.log` (16 m 1 s): **113/113**, 1 omitido y 1 fallo ajeno `EditRecipeE2ETest.cardEditActionIsLocalizedInEnglish` («Activity never becomes requested state [DESTROYED]»); focalizada `EditRecipeE2ETest` **14/14** en verde.
  - `e2e-sesion60-retry2.log` (20 m 56 s): abortado en 105/113 con `SearchAndFilterRecipesE2ETest.clearingSearchKeepsMomentFilter` fallido + `INSTRUMENTATION_ABORTED: System has crashed`; **reranqueado por el validador: 8/8 en verde**.
- Conclusión: los tres fallos de suite quedan explicados (uno propio ya corregido, dos flakes del AVD confirmados con reranqueo focalizado); no hay evidencia de regresión de `ScanPantryE2ETest`, `CompatibleRecipesE2ETest` (8/8), `RecipeDetailE2ETest` (7/7) ni del resto de la suite.
- Captura `docs/evidence/escanear-despensa-con-ia/scanner-degradacion-es.png` revisada visualmente: aviso «Configura tu API key en Ajustes para analizar fotos con IA…», obturador «Escanear alimentos», hint de captura, alta manual, sin HUD ni porcentaje — coherente con S2/RD05/D3 y en español.

## Cobertura S1-S9 / D1-D8

- **S1/S2/S3 (verify 1 y 2)**: E2E `aiPhotoShowsChipsWithQuantitiesAndSavesThemToPantry`, `missingApiKeyShowsNoticeAndKeepsManualFlowAvailable`, `analysisFailureShowsNoticeAndTheNextPhotoCanRetry` en verde; el aviso de key ausente además está capturado a mano en el AVD.
- **S4/S5**: `blankNamesAreDiscardedAndInvalidQuantitiesAreNormalized`, `successivePhotosAccumulateWithoutOverwritingEditedChips`, `detectedAndUserCreatedDuplicatesCollapseToOneChip` + `ScanAnalysisTest` 6/6 y `ScanAnalysisParserTest` 6/6.
- **S6/S7**: chips, stepper 0..99, marca «Ya está en la Despensa», RD13 y confirmaciones de salida/re-escaneo cubiertas por E2E y `ScanDetectionsTest` 8/8 / `ScannerViewModelTest` 11/11.
- **S8**: `englishLabelsAndPromptLanguageFollowTheActiveLocale` (textos y `language` del prompt); paridad 188/188.
- **S9/RD06**: `captureIsNeverWrittenToCacheOrFiles` (E2E) + `CameraXScanImageSourceTest` (JPEG en memoria, `ImageProxy.close()` en `finally`, sin `OutputFileOptions` ni ficheros).
- **D1-D8**: verificadas por inspección del código (`ScanAnalysis.mergeProposals`, `normalizedQuantity`, `OpenAiCompatibleScanImageAnalyzer`, `CameraXScanImageSource`) y por los unit/E2E anteriores. `temperature = 0`, alias `chat`, `data:image/jpeg;base64`, sin reintentos automáticos y sin configurar timeouts nuevos (D8).
- **Nota de criterio**: `mergeProposals` **suma** las cantidades de propuestas duplicadas dentro de una misma respuesta (recortando a 99); D1 solo exige «un solo chip» y este matiz no está escrito en `docs/domain-model.md` (ver L2).

## Hallazgos

Sin hallazgos Critical/High/Medium. Notas Low no bloqueantes:

### L1 · Resumen de riesgos de `PROGRESS.md` desactualizado
- Severidad: Low
- Evidencia: `PROGRESS.md` §«Riesgos y preguntas abiertas» sigue diciendo «RK02 (el modelo de detección MediaPipe está por validar: spike antes del Escáner)», mientras `docs/risks-and-open-questions.md` la marca **cerrada** y MediaPipe ya no está en el build.
- Por qué importa: un agente nuevo puede creer que queda un spike de MediaPipe pendiente.
- Cambio requerido: sustituir esa frase por RK03 (retención del proveedor) y S05/S07.
- Verificación: lectura de la cabecera de `PROGRESS.md` y `grep RK02`.

### L2 · Regla de fusión de duplicados internos (suma) no documentada
- Severidad: Low
- Evidencia: `ScanAnalysis.mergeProposals` suma cantidades de propuestas con el mismo nombre normalizado (`ScanAnalysisTest.duplicateProposalsInsideOneResponseFuseAndSumQuantities`); `docs/domain-model.md` solo dice que «se fusionan en un solo chip».
- Por qué importa: la UI no tiene regla de negocio implícita; conviene dejarla escrita para futuras features (Sugerencias).
- Cambio requerido: añadir en la fila de Relaciones/casos límite que los duplicados de una respuesta se fusionan **sumando** cantidades con recorte a 99.
- Verificación: lectura de `docs/domain-model.md`.

### L3 · El aviso de privacidad afirma una política de retención aún no verificada
- Severidad: Low (lo exige la spec; riesgo rastreado en `RK03`)
- Evidencia: `settings_privacy_notice` (es/en) dice «el proveedor no las retiene», mientras `docs/technical-discovery.md` escribe «Proveedor sin retención para entrenamiento (**por verificar**, `RK03`)» y la tarea «Configuración efectiva de “sin retención”» sigue abierta para `sugerir-recetas-con-ia`.
- Por qué importa: es una afirmación RGPD-facing al usuario sin evidencia; debe quedar coherente antes de distribuir.
- Cambio requerido (antes de publicar, no en esta feature): o se verifica la política y se cierra `RK03`, o se suaviza el copy («según la política del proveedor») en es/en y en la spec.
- Verificación: `settings_privacy_notice` vs `RK03` alineados.

### L4 · `OpenAiCompatibleScanImageAnalyzer` sin unit de sus ramas de error
- Severidad: Low
- Evidencia: T1 pide «unit del parser … y de error → `Failed`/`MissingApiKey`»; existen `ScanAnalysisParserTest` y `AiEndpointsTest.chatCompletions`, pero no hay test del cliente (extracción de `choices[0].message.content` string/array, HTTP ≠ 2xx, `IOException`, key ausente). El repo no tiene `MockWebServer`/framework de mocks y `OpenAiCompatibleImageGenerator` tampoco lo tiene (convención existente).
- Por qué importa: la rama de degradación del cliente real solo se cubre por inspección y por verify 3 (pendiente).
- Cambio requerido (futuro, cuando el harness incorpore `MockWebServer`): test del cliente con 200/500/IOException/sin key.
- Verificación: `gradlew testDebugUnitTest` con el nuevo test.

### L5 (nota) · Cableado de `ScannerRoute` no cubierto por E2E
- Severidad: Low
- Evidencia: la E2E monta `ScannerScreen` y pasa `onCapture`/`onConfirmExit` ella misma; `LocalConfiguration.current.locales[0].language` y `if (!viewModel.confirmExit()) onBack()` no se ejercitan en ningún test (limitación histórica del NavHost).
- Impacto: `S8` se valida a nivel de ViewModel/screen, no del Route.

## Seguridad (checklist por feature)

Sin hallazgos. Revisado sobre el diff: (1) **secretos** — `local.properties` solo `sdk.dir`, sin claves reales ni en tests/fixtures/docs; la key se inyecta desde `BuildConfig` (D7-D11, riesgo aceptado `S05`/`RK01`); (2) **auth** — sin cuentas ni roles (`P31`), sin cambios; (3) **input** — JSON construido con `JSONObject` (sin concatenación), parser tolerante acotado al primer objeto, nombres vacíos descartados y cantidades normalizadas en dominio; (4) **exposición/privacy** — la foto se envía a DevExpert Inference (esperado por la spec) como `data:image/jpeg;base64` en el cuerpo HTTPS; sin logs de la imagen, del cuerpo ni de la key; errores → `Failed` sin filtrar detalles; (5) **servicio externo** — endpoint construido desde `AI_BASE_URL` con default DevExpert, timeouts del `OkHttpClient` compartido, sin reintentos automáticos (D5/D8); (6) **dependencias** — se retira `mediapipe-tasks-vision` y el asset de 7,5 MB; se añade `org.json:json 20240303` **solo** en `testImplementation` (justificado, oficial); (7) **boundary server/client** — N/A (app nativa); (8) **ficheros** — la captura nunca toca disco (RD06, verificado por test y por inspección de `CameraXScanImageSource`); (9) **auditabilidad** — MVP local sin telemetría (`S01`), sin cambios.
Riesgo residual declarado: la foto del usuario sale del dispositivo y la política «sin retención» no está verificada (`RK03`); ver L3.

## Documentación y harness

- `docs/domain-model.md`: RD06 reformulada («se envían … y se descartan»), RD12 derogada conservando el ID, fila «Escaneo» y Relaciones sin confianza/HUD, D2 en validaciones. Coherente con el código.
- `docs/technical-discovery.md`: stack, tabla de detección, flujos mermaid, integraciones `/chat/completions`, datos y RGPD actualizados; spikes RK02 y detección en vivo marcados como **retirados** conservando historial.
- `docs/risks-and-open-questions.md`: `RK02` cerrada, `RK03` reescrita (la foto sí sale), tarea del spike cerrada, `RK05` ajustada.
- `DESIGN.md`: fila del Escáner sin HUD/confianza y con obturador/progreso; laguna de material declarada.
- Notas de revisión presentes en `docs/specs/escanear-despensa-con-camara.md` y `docs/specs/detectar-alimentos-en-vivo.md`.
- `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`: correctamente no tocados (la spec lo justifica).
- Presupuesto de evidencia respetado: 1 informe + 1 captura (≤ 3 artefactos), solo de esta feature. `PROGRESS.md` y `feature_list.json` con estado `passing` y evidencia fiel; `docs/progress-history.md` con la entrada de la sesión 60.
- Alcance: sin tocar Room v2, Despensa, Coincidencia/badges, Recetas Compatibles, RD13, máximo 99 ni `/images/generations`; `CompatibleRecipesE2ETest` solo se adapta al nuevo constructor.

## Qué implica el verify 3 pendiente

`accept` **no certifica** el contrato real de DevExpert Inference en `/chat/completions` con imagen (formato multimodal, alias `chat`, contenido de `choices[0].message.content`) ni la calidad de los nombres/cantidades devueltos. Queda como **puerta de release** antes de depender del Escaneo con IA en producción: una foto real en es y otra en en con key real y confirmación humana (regla de `AGENTS.md`), registrando el resultado en este informe; si el proveedor difiere del contrato OpenAI-compatible, se ajusta el parser/cliente como desviación documentada (spec, «Unknowns»). La captura de chips de un análisis real queda pendiente por el mismo motivo.

---

# Corrección post-aceptación: permiso `INTERNET` (2026-10-23)

**Síntoma**: en el dispositivo real V2124 (Android 13, adb inalámbrico) el Escaneo fallaba unos 2 s después del toque con el aviso «No se pudo analizar la foto…». El fallo era demasiado rápido para el timeout de 10 s del `OkHttpClient`.

**Diagnóstico**:

- `CameraXScanImageSourceTest` pasó en el V2124: la captura CameraX funciona en el dispositivo.
- El móvil resolvía y conectaba a `inference.devexpert.io:443` (`nc` → exit 0) y una llamada directa al proveedor devolvía HTTP 200 con JSON válido.
- El logcat del Vivo suprime los logs de la app (ni siquiera aparecen los `Log` del APK recién instalado, aunque el APK sí los contiene), así que se añadió una sonda temporal visible en el aviso. Mostró: `SecurityException: Permission denied (missing INTERNET permission?)`.

**Causa raíz**: `app/src/main/AndroidManifest.xml` declaraba `CAMERA` pero no `INTERNET`. Las E2E usan un analizador simulado y no ejercitan la red real, por lo que el gate de aceptación no lo detectó.

**Arreglo**: añadida `<uses-permission android:name="android.permission.INTERNET" />` y retirada la instrumentación temporal (logs y sonda visible) para dejar el diff en una línea.

**Verificación**:

- `lintDebug`, `testDebugUnitTest` y `assembleDebug`: **BUILD SUCCESSFUL** (2026-10-23).
- APK final instalado en el V2124; `dumpsys package` muestra `android.permission.INTERNET: granted=true`.
- Escaneo real en español con llamada real al proveedor: la bandeja mostró el chip **«chicle», cantidad 1**, sin aviso de fallo.
- Escaneo real en inglés (locale `en-US`, misma foto): la bandeja mostró el chip **«mint», cantidad 1**, con todos los textos en inglés y sin aviso de fallo.

**Conclusión**: el verify 3 queda cubierto en español e inglés en dispositivo real y el bloqueo de red que impedía el Escaneo con IA está resuelto. `feature_list.json` sigue `accepted`.
