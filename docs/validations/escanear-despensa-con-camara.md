# Validación independiente: `escanear-despensa-con-camara`

- **Veredicto: `accept`**
- **Fecha**: 2026-10-21 · **Rol**: `feature-validator` (independiente del `feature-implementer`)
- **Feature**: `escanear-despensa-con-camara` (`funcional`) · estado revisado `passing`
- **Spec**: `docs/specs/escanear-despensa-con-camara.md` (con los deltas del 2026-10-21: D2/D3/D5 y D6 —cantidad máxima 99 en toda la app—, que incorpora Q-escanear-despensa-con-camara-4)
- **Objeto revisado**: árbol de trabajo **sin commitear** en `.worktrees/escanear-despensa-con-camara`, diff completo contra `origin/develop` (`336d0ea`): 23 ficheros modificados + 18 nuevos (más este informe).
- **Entorno**: AVD `Pixel_10_Pro` API 37, JDK 17 (`C:\Users\Daniel\.jdks\corretto-17.0.20`), PowerShell 5.1 lanzado con el wrapper `cmd /c "set JAVA_HOME=...&& ... powershell -File <script>.ps1 > .harness\<log>"` descrito en `PROGRESS.md`.

El informe sustituye al de autoverificación del implementador (que se conserva resumido y corregido al final, en «Autoverificación del implementador»), conforme al requisito de actualizar —no duplicar— `docs/validations/<feature-id>.md`.

## 1. Resumen del veredicto

La feature cumple la spec: sustituye el destino provisional `scan_pantry` por un Escáner real con visor CameraX, inferencia MediaPipe on-device (EfficientDet-Lite2, umbral 0.60, solo diez clases alimentarias COCO), chips editables con stepper entero **0..99** (0 = sin cantidad, sin campo de texto), resolución de coincidencias con la Despensa **preguntando en pantalla** (`Mantener cantidad`/`Actualizar cantidad`, secuencial, sin filas parciales al descartar), confirmación al salir/re-escanear con chips pendientes, degradaciones RD05 y no persistencia de la captura (RD06). El delta del máximo 99 está aplicado de forma global (`PantryItemValidation.MAX_QUANTITY = 99`, sin rastros de 999 en `app/src`, sin migración Room).

Verificado por el validador: los dos `verification` de `feature_list.json` se reejecutaron en el emulador real (focalizadas) y se reprodujeron a mano con `adb`/`uiautomator` en locale es. Los gates baratos (`init`, `lint`, `unit`, `assemble`) se reranearon en el árbol actual; la regresión E2E completa se inspeccionó (evidencia exacta y posterior al último cambio funcional, salvo las dos clases E2E afectadas por el delta, que se reejecutaron aquí).

Sin hallazgos críticos ni altos. Cinco observaciones menores (todas `Low`), ninguna bloqueante.

## 2. Gates ejecutados por el validador

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `init.ps1` (wrapper `cmd /c`) | **exit 0**; `installDebug` + app arrancada en `Pixel_10_Pro` API 37 (`App ... en ejecucion`) |
| Estático | `lint.ps1` → `lintDebug` | **exit 0**; `BUILD SUCCESSFUL`; informe `lint-results-debug.xml`: **0 errores / 37 warnings** (1 warning nuevo del Escáner: `scanner_add_food_confirm` sin uso — ver H1) |
| Unit | `gradlew.bat testDebugUnitTest --rerun-tasks` | **exit 0**; **50/50 tests, 0 fallos, 0 omitidos** (10 clases; `ScanDetectionsTest` 9/9 y `ScannerViewModelTest` 8/8 incluidos) |
| Build | `gradlew.bat assembleDebug` | **exit 0**; `BUILD SUCCESSFUL` |
| E2E focalizada (delta + verify 1 y 2) | `connectedDebugAndroidTest -P...class=ScanPantryE2ETest,PantryManagementE2ETest` | **20/20 tests, 0 fallos, 0 omitidos** (7m45s) en `Pixel_10_Pro` API 37 |
| E2E focalizada (navegación real al Escáner) | `connectedDebugAndroidTest -P...class=MyRecipesLocalizationE2ETest` | **6/6 tests, 0 fallos** (1m12s); `scanPantryOpens...` exige que el visor CameraX real se monte (`CAMERA_PREVIEW`) y que `Volver` regrese a Mis Recetas |
| Higiene del diff | `git diff --check 336d0ea` · `ConvertFrom-Json feature_list.json` · paridad de claves `values/` vs `values-en/` | **OK**: sin errores de whitespace; JSON válido (26 features); **169/169** claves en ambos locales |
| Asset | `Get-FileHash` del modelo | `efficientdet_lite2_detection_metadata_1.tflite` = `6FD32C84…BD52` y `banana.jpg` = `E133F498…1706`, **coinciden** con `docs/technical-discovery.md` |

### Evidencia inspeccionada (no reejecutada, por coste)

| Comprobación | Evidencia | Motivo de no reejecutar |
|---|---|---|
| Regresión E2E completa | **87/87, 0 fallos/omitidos** (17m10s) en `Pixel_10_Pro` API 37, `.harness/e2e-final-script.log` (sesión 49) | Suite larga; el único cambio posterior (`MAX_QUANTITY` 999→99) solo afecta a las dos clases E2E que sí se reejecutaron aquí |
| `MediaPipeFoodDetectorTest` (adaptador real Lite2) | **1/1** API 37 (`.harness/mediapipe-food-test-pixel10.log`, 23s) y **1/1** en API 30 x86 | Inferencia real ya cubierta; el asset y su hash se verificaron directamente |
| `CameraXScanImageSourceTest` | **1/1** API 37 (`.harness/camerax-capture-test.log`): JPEG en memoria, SOI válido, sin archivos en `cacheDir`/`filesDir` | Idéntico criterio; además la captura real se comprobó a mano (visor CameraX activo en vivo) |

## 3. Trazabilidad spec → implementación → prueba

| Escenario | Dónde vive | Prueba / comprobación |
|---|---|---|
| S1 detección con chips, confianza y HUD (verify 1) | `ScanDetections`, `ScannerViewModel.detect`, `CapturedImageHud` | `ScanPantryE2ETest.injectedCaptureShowsFoodChipConfidenceAndHudLabel` (chip «Plátano · 84%», HUD por tag, `84%` en el chip) — **reejecutado ✓**; `ScanDetectionsTest.filtersBelowChosenThresholdAndBlankNames` (0.599 fuera, 0.60 dentro) |
| S2 confirmar y guardar con cantidades (verify 2) | `ScannerViewModel.persistItems`, `ScanDetections.toPantryItem` | `savePersistsChosenQuantityAndShowsPantryScreen` (3 incrementos → `PantryScreen` «Cantidad: 3», `quantity == 3`, imagen liberada) — **reejecutado ✓**; comprobación manual adb: guardar chip «Tomate Fresco» 3 → Despensa muestra «Tomate Fresco / Cantidad: 3» |
| S3 quitar/renombrar/añadir + stepper 0..99 | `ScanDetections.mergeDraftItems/rename/updateQuantity`, `ScannerFoodCard` | `chipsCanBeRemovedRenamedAndAddedManually`, `quantityStepperHasBoundsAndNoEditableQuantityField` (0 → 99, «−» off en 0, «+» off en 99, sin `SetText`), `quantityControlsHaveAtLeast48DpTargets…`; `ScanDetectionsTest.updateQuantityClampsToIntegerBounds` (99/0) |
| S4 duplicados en la bandeja | `toDraftItems`, `mergeDraftItems` | `detectedAndUserCreatedDuplicatesCollapseToOneChip`, `ScanDetectionsTest` (nombre legible, confianza máxima, gana el chip editado) |
| S5 permiso denegado → manual | `ScannerRoute`, `onCameraPermissionDenied` | `deniedPermissionContinuesWithManualEntry` (aviso + sin obturador + alta manual + Guardar habilitado) |
| S6 sin detecciones | `detect` → `NO_FOODS_DETECTED` | `emptyDetectionShowsManualEntryAndCanSave` |
| S7 fallo de inferencia | `detect` (captura de excepción/`LinkageError`) | `detectorFailureShowsNoticeAndKeepsManualFlowAvailable`, `ScannerViewModelTest.emptyAndFailedInferenceDegradeToManualEntry` |
| S8 la captura no persiste (RD06) | `CameraXScanImageSource` (sin `OutputFileOptions`), estado en memoria, `CapturedImageHud` recicla el bitmap | `capturedImageIsNeverWrittenToCacheOrFiles` (cache/files antes == después), `CameraXScanImageSourceTest`; barrido propio: `data/scanner/` sin `OutputFileOptions`/`FileOutputStream`/`createTempFile` |
| S9 es/en y ≥ 48 dp | `strings.xml` + `values-en` (169/169), tags y `contentDescription` | `englishLabelsAndScannerActionsAreLocalized` («Pantry Scanner», «Banana · 84%», «Decrease/Increase quantity»), `PantryLocalization` intacta; `MyRecipesLocalizationE2ETest` 6/6 — **reejecutado ✓**; a mano en es: «Escáner de la Despensa», «Tomar foto», «Cantidad», «Guardar en la Despensa» |
| S10 salir/re-escanear con chips | `requestExit/confirmExit/requestRescan`, `ExitConfirmationDialog` | `exitConfirmationCancelKeepsChipsAndConfirmLeaves`, `rescanningWithUnsavedChipsRequiresConfirmation`, `ScannerViewModelTest.exitAndRescanAskBeforeDiscardingManualChips` |
| S11 coincidencia con la Despensa | `matchesWithPantry`, `queueNewAutomaticMatches`, `DuplicateResolutionDialog` | `pantryMatchOpensDecisionDialogAndKeepOrUpdateReusesExistingRow` (diálogo automático, texto con cantidad existente, cambiar decisión tocando la marca, fila única con `id` existente y cantidad `null` al actualizar 0) — **reejecutado ✓**; a mano: diálogo «Alimento ya en la Despensa / «Tomate Fresco» ya está en la Despensa (Cantidad: 3). ¿Qué cantidad conservamos?» y marca «Ya está en la Despensa» tras descartar |
| S12 varias coincidencias y aborto | `saveToPantry`, `resolveDuplicate`, `dismissDuplicateResolution` | `multiplePendingMatchesAskSequentiallyAndDismissAbortsWholeSave` (una a una, nada escrito con pendientes, descarte deja bandeja y decisiones intactas), `ScannerViewModelTest.savingWithPendingMatchesWritesNothingUntilEveryDialogIsResolved` |
| D6 máximo 99 en toda la app | `PantryItemValidation.MAX_QUANTITY = 99`; UI y repositorios derivan de la constante | unit 50/50 (límites 0/99 y rechazo en 100) + `PantryManagementE2ETest` (99 incrementos, «+» off) **reejecutado ✓**; `grep 999|1000` en `app/src` sin resultados; Room v2 intacto (`RecipeDatabase`/`PantryItemEntity`/`PantryItemDao` fuera del diff) |
| T0/RK02 (modelo y umbral) | `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` | Decisión explícita del usuario (Lite2 + 0.60), hash del asset verificado por el validador, vocabulario COCO de diez clases filtrado en `MediaPipeFoodDetector` |

Alcance respetado: no hay carrusel de Recetas compatibles, ni Sugerencias, ni Lista de la Compra, ni unidades/decimales, ni suma de cantidades al resolver coincidencias; `PantryRepository` solo añade el enum de resolución y no se tocaron esquema Room ni las pantallas Despensa/Mis Recetas (más allá de derivar de la constante).

## 4. Evidencia visual

Capturas nuevas (2, en español, dentro del presupuesto y solo de esta feature), tomadas por el validador en la app real instalada (`adb exec-out screencap`) sobre `Pixel_10_Pro` API 37:

- `docs/evidence/escanear-despensa-con-camara/escaner-chip-stepper-es.png` — Escáner con visor CameraX en vivo, obturador «Tomar foto», chip «Tomate Fresco» con stepper en **3** («−/valor/+»), alta manual y acción primaria «Guardar en la Despensa» (terracota `primary` `#A83212` sobre superficie `#FBF9F5`).
- `docs/evidence/escanear-despensa-con-camara/escaner-coincidencia-despensa-es.png` — diálogo de coincidencia «Alimento ya en la Despensa» con nombre y «Cantidad: 3» y los botones «Mantener cantidad»/«Actualizar cantidad».

Limitación honesta: el emulador no permite inyectar de forma determinista una imagen de comida por la cámara real, así que las píldoras HUD «nombre · confianza» no aparecen en las capturas; su contenido y su tag están cubiertos por `injectedCaptureShowsFoodChipConfidenceAndHudLabel` y por `MediaPipeFoodDetectorTest` con la fixture real. Consecuencia: no se realizó muestreo de píxeles del HUD; los roles de color del resto de la pantalla coinciden con `DESIGN.md`/`uxdocs/DESIGN.md` (superficie, `primary`, `secondary` para la marca de coincidencia y `error` en «Salir»).

## 5. Hallazgos (ninguno bloqueante)

### H1 · Cadena sin uso `scanner_add_food_confirm`
- Severity: Low
- Evidence: `app/src/main/res/values/strings.xml` y `values-en/strings.xml`; warning de lint `UnusedResources` en `app/build/reports/lint-results-debug.xml` (1 de los 37 warnings, único del Escáner).
- Why it matters: ruido de lint y una cadena es/en que puede desincronizarse sin que nadie la use.
- Required change: eliminar la clave de ambos locales (o usarla si se pretendía un botón «Añadir» en el diálogo de alta).
- Suggested implementation: 1) borrar `scanner_add_food_confirm` de `values/` y `values-en/`; 2) `lintDebug`.
- Verification after fix: `lintDebug` sin el warning `UnusedResources` de esa clave; paridad de claves 168/168.

### H2 · Guardado no atómico ante fallo del repositorio
- Severity: Low
- Evidence: `ScannerViewModel.persistItems` guarda chip a chip; si un chip posterior devuelve `PantrySaveResult.DuplicateName` (o lanza), los anteriores ya quedaron escritos y el estado conserva la bandeja (`ScannerNotice.DUPLICATE_NAME`/`SAVE_FAILED`).
- Why it matters: S12 exige «sin filas parciales» solo al descartar un diálogo (eso está cubierto); ante una carrera real de nombres la Despensa puede quedar parcialmente actualizada. Riesgo de producto bajo (un solo usuario, `P31`) pero conviene dejarlo anotado.
- Required change: ninguna para aceptar; si se aborda, guardar los chips pendientes en una sola transacción o registrar qué ids se escribieron para reintentar de forma idempotente.
- Verification after fix: unit nuevo con repositorio que falla en el segundo chip → la Despensa no queda a medias o el reintento no duplica.

### H3 · `PantryDuplicateResolution` vive en el contrato del repositorio
- Severity: Low
- Evidence: `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt` declara el enum, que es una decisión del Escaneo, no del repositorio.
- Why it matters: mezcla de conceptos en la capa de dominio; coste futuro de lectura.
- Required change: opcional, moverlo a `domain/scanner/` o a un fichero propio del dominio del Escaneo.
- Verification after fix: `testDebugUnitTest` y E2E focalizada siguen en verde.

### H4 · Restos que el orquestador no debe incluir en el commit de la feature
- Severity: Low (higiene de repositorio, no del comportamiento)
- Evidence: en el mismo árbol sin commitear aparecen `.opencode/agent/implementer.md`, `planner.md` y `validator.md` (solo cambio del campo `model:` de cada wrapper) y, en `docs/risks-and-open-questions.md`, la fila `Q-ver-detalle-de-receta-1`, que pertenece a la spec de `ver-detalle-de-receta`.
- Why it matters: `AGENTS.md` §5 prohíbe modificar `.opencode/`; estas ediciones son configuración de la sesión de orquestación, no trabajo de la feature, y deben quedar fuera del commit (o en un cambio propio y explícito).
- Required change: `git checkout -- .opencode/agent` antes de commitear esta feature y mover la fila de `Q-ver-detalle-de-receta-1` al cambio de su feature.
- Verification after fix: `git status` del commit de la feature sin rutas de `.opencode/`.

### H5 · Informes previos con cifras desfasadas tras el delta de 99
- Severity: Low
- Evidence: la autoverificación (sección 6) decía «stepper entero 0..999»; `docs/validations/gestionar-alimentos-de-la-despensa.md` mantiene cifras de 999 como evidencia de una aceptación anterior al delta.
- Why it matters: un lector futuro podría creer que el máximo sigue siendo 999.
- Required change: aquí queda corregido; el informe de `gestionar-alimentos-de-la-despensa` debe actualizarse en **su** revalidación pendiente (ya anunciada en su spec y en `PROGRESS.md`).
- Verification after fix: `grep 999 docs/validations/` solo devuelve referencias históricas marcadas como tales.

## 6. Autoverificación del implementador (histórica, corregida)

Resumen del informe original del `feature-implementer`, con las dos cifras corregidas al límite vigente (99) tras el delta del 2026-10-21:

- **Decisión RK02**: EfficientDet-Lite2 (`efficientdet_lite2_detection_metadata_1.tflite`, 7.557.887 bytes, SHA-256 `6FD32C84AB1EB0F7E7F3A7A20A20D7DF1530DAA8378728F7C79571096286BD52`), umbral 0.60, MediaPipe `tasks-vision:0.10.35`, CameraX 1.5.3, diez clases COCO alimentarias en `food_label_*`.
- **Implementación**: dominio puro (`DetectedFood`, `Scan`, `ScanDraftItem`, `ScanDetections`), adaptadores CameraX (JPEG en memoria, `ImageProxy` siempre cerrado, sin `OutputFileOptions`) y MediaPipe on-device, `ScannerRoute/Screen/ViewModel`, navegación `scan_pantry` → `ScannerRoute`, es/en y actualización de `MyRecipesLocalizationE2ETest`.
- **Gates declarados**: `init.ps1` exit 0, `lintDebug` 0 errores, `testDebugUnitTest --rerun-tasks` 50/50, `assembleDebug` OK, `connectedDebugAndroidTest` 87/87 (0 fallos/omitidos) en `Pixel_10_Pro` API 37, `ScanPantryE2ETest` 15/15, `MediaPipeFoodDetectorTest` 1/1 (API 30 y 37), `CameraXScanImageSourceTest` 1/1. **Corrección**: donde el original decía «stepper 0..999» debe leerse **0..99** («−» deshabilitado en 0, «+» en 99).

## 7. Seguridad (revisión acotada a la feature)

Revisado contra `references/security-checklist.md` sobre el diff:

- **Secretos/configuración**: no hay claves, tokens ni `.env` en el diff; no hay llamadas de red en esta feature (inferencia on-device, `P23`).
- **Privacidad/RGPD**: la captura vive solo en memoria (`ByteArray` en `ScannerUiState`), se libera al guardar/re-escanear/salir y el `Bitmap` se recicla; `CameraXScanImageSource` no usa `OutputFileOptions` y el E2E compara `cacheDir`/`filesDir` antes y después sin cambios. No hay tabla ni entidad de Escaneos.
- **Entrada**: nombres de alimento se recortan y normalizan; cantidades acotadas a 0..99 en dominio (`ScanDetections.updateQuantity`, repositorios con `require`) y sin campo de texto libre para la cantidad; el detector filtra el vocabulario (descarta personas/objetos).
- **Permisos**: `CAMERA` en runtime con degradación a modo manual (RD05); el manifest ya declaraba el permiso, sin cambios.
- **Exposición**: no se registran datos en logs ni se sirven archivos; errores del detector se convierten en aviso sin detalles internos.
- **Conclusión**: sin hallazgos de seguridad en el ámbito de la feature. Nota futura (fuera de alcance): la API key de IA sigue siendo el riesgo RK01 de `configurar-acceso-a-ia`.

## 8. Documentación duradera y estado del harness

- `Durable Documentation Impact` de la spec cumplido: `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md` no requerían cambios (y `AGENTS.md` sigue siendo un router breve); se actualizaron `docs/technical-discovery.md` (Lite2, umbral, hashes, CameraX/MediaPipe, fixtures), `docs/risks-and-open-questions.md` (RK02 aceptado; Q-escanear 1..4 resueltas) y `docs/domain-model.md` (RD12, cantidad 0..99, política de fusión y coincidencias preguntadas en pantalla). Sin contradicciones detectadas entre esas docs y el código.
- `docs/specs/gestionar-alimentos-de-la-despensa.md` refleja el máximo 99 y deja constancia de que necesita revalidación (correcto: su informe de aceptación es anterior a D6).
- `PROGRESS.md` (Estado verificado + Siguiente paso) y `docs/progress-history.md` (sesiones 47-50) están al día; `feature_list.json` es JSON válido y la feature sigue en `passing` a la espera de persistir este veredicto. Los scripts del harness no se han tocado.
- Higiene: `.harness/` conserva registros de arranque y de verificación; el dispositivo quedó limpio (datos de la app borrados, locale por app restaurado, `/sdcard/ui.xml` eliminado). Sin commit, sin push.

## 9. Acción recomendada al orquestador

1. Pasar `escanear-despensa-con-camara` a **`accepted`** en `feature_list.json` y resumir en `evidence`, por ejemplo:
   > «Aceptación independiente (feature-validator, 2026-10-21): accept — spec con deltas D2/D3/D5/D6 (máximo 99) cumplida; gates reejecutados en Pixel_10_Pro API 37: init exit 0, lintDebug 0 errores (37 warnings, 1 nuevo), testDebugUnitTest --rerun-tasks 50/50, assembleDebug OK, connectedDebugAndroidTest focalizada ScanPantryE2ETest+PantryManagementE2ETest 20/20 y MyRecipesLocalizationE2ETest 6/6 (regresión completa 87/87 inspeccionada); verify 1 y 2 reproducidos en emulador y a mano con adb/uiautomator (chip 3 → Despensa «Cantidad: 3»; diálogo de coincidencia con «Mantener/Actualizar»); RD06 y RD05 verificados; 2 capturas nuevas en docs/evidence/escanear-despensa-con-camara/; informe en docs/validations/escanear-despensa-con-camara.md; 5 hallazgos Low no bloqueantes».
2. Antes del commit: excluir `.opencode/agent/*.md` y la fila `Q-ver-detalle-de-receta-1` del cambio de esta feature (H4).
3. Abrir la revalidación de `gestionar-alimentos-de-la-despensa` por el spec revisado (99) y actualizar su informe (H5).