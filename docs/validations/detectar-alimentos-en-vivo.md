# Informe de validación — Detectar alimentos en vivo

- **Feature**: `detectar-alimentos-en-vivo`
- **Spec**: `docs/specs/detectar-alimentos-en-vivo.md`
- **Rama/commit**: `feature/detectar-alimentos-en-vivo` @ `8be1a10` (repair del `revise` de la sesión 50; árbol limpio)
- **Validador**: `feature-validator` (revalidación tras repair; sesión 52, 2026-10-22)
- **Veredicto**: **accept** → `accepted`

## Resumen

La revalidación confirma que los 3 findings del `revise` (sesión 50) están resueltos y que la
implementación cumple la spec: detección continua sobre el visor con acumulación sin repetidos
(D1/D3/D6/D8), degradación RD05 alcanzable desde la UI (incluido el reinicio con lista vacía, D5),
ningún fotograma persistido (RD06) y el flujo de captura congelada retirado por completo (D7).

## Checks reranqueados por el validador (tier barato)

| Check | Resultado |
| --- | --- |
| `init.ps1` | exit 0 (reintento tras el falso fallo conocido de `pidof` en el arranque en frío) |
| `gradlew lintDebug` | BUILD SUCCESSFUL, **0 errores** (solo warnings preexistentes de deprecación) |
| `gradlew testDebugUnitTest --rerun-tasks` | **61/61, 0 fallos** (11 suites desde XML; `ScanDetectionsTest` 13/13, `ScannerViewModelTest` 9/9) |
| `gradlew connectedDebugAndroidTest -P...class=MediaPipeFoodDetectorTest` (focalizado, reranqueo propio) | **1/1** en verde; testcase `time=1.466 s` (arranque en frío + 5 muestras) — corrobora el orden de magnitud de la latencia D2 |

Entorno: AVD `Pixel_10_Pro` API 37, JDK 17 (`corretto-17.0.20`).

## Evidencia cara inspeccionada (no reranqueada por defecto)

- **`connectedDebugAndroidTest` 96/96, 0 fallos, 0 errores, 0 omitidos** — XML en
  `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`
  (timestamp 2026-10-21T22:39:20Z ≈ 00:39 h local del 22/10, duración 831 s ≈ 14 m). Contiene el
  **nuevo** E2E `detectorFailureWithEmptyListCanBeRestartedWithRescan`, así que el pase se ejecutó
  sobre el código reparado y es coherente con el diff actual (`8be1a10`, árbol limpio):
  - `ScanPantryE2ETest` **17/17** (los dos `verify` intactos: `liveFramesShowFoodOnceWithBestConfidenceAndHudLabel`
    y `manualFoodCanBeAddedWithQuantityAndSaved`; RD06 `liveFramesAreNeverWrittenToCacheOrFiles`;
    S6 `deniedPermissionContinuesWithManualEntry`; S7 doble: `detectorFailureShowsNoticeAndKeepsManualFlowAvailable`
    + reanudación; S9 doble: `exitConfirmation…` y `rescanningWithUnsavedChips…`; S10 localización y 48 dp).
  - `CameraXLiveFrameSourceTest` 1/1 (JPEG en memoria, sin ficheros); `MediaPipeFoodDetectorTest` 1/1 (Lite2 real).
  - Regresión íntegra: AppLaunch, Create/Edit/Delete Recipe, SearchAndFilter, PantryManagement,
    PantryLocalization, MatchBadge, RecipeDetail, MyRecipesCards/Localization, etc.
- **Latencia D2**: la cifra medida por el implementador
  (**estado estacionario avg 106,2 ms, máx 123 ms; arranque en frío 889 ms**) está registrada en
  `docs/technical-discovery.md` y el instrumento (`println` sello D2 en `MediaPipeFoodDetectorTest`,
  sin aserción de timing) es permanente y reranqueable. El XML no captura el stdout, por lo que el
  validador lo corroboró con su reranqueo focalizado (testcase 1,466 s = frío + 5 muestras ≈ 889 + 5×106 ms). ≤ 800 ms ✓.
- **Capturas** (`docs/evidence/detectar-alimentos-en-vivo/`, presupuesto 2 PNG respetado), revisadas
  visualmente por el validador: `scanner-en-vivo-es.png` (visor en vivo con escena virtual, indicador
  «Detectando alimentos…» en marcha, hint `scanner_live_hint` en español, alta manual) y
  `scanner-permiso-denegado-es.png` (degradación RD05 completa: placeholder «No se pudo abrir la
  cámara», aviso «Permiso de cámara denegado…» y alta manual sin bloquear).

## Reproducción de la spec (S1-S11, D1-D8, RD05/RD06)

- **S1-S5, S9, S10** — cubiertas por `ScanPantryE2ETest` (fakes por secuencia) y unit
  (`mergeLiveDetections`: sin repetidos, confianza máxima, umbral 0,60, no pisar ediciones; ViewModel:
  muestreo 300 ms, conflación, D8).
- **S6** — E2E `deniedPermissionContinuesWithManualEntry` + captura de degradación RD05 en español.
- **S7/D5 (M1 del revise)** — corregido y verificado: `ScannerUiState.liveStopped` (true solo en la
  rama `Failed`, false cuando el bucle arranca de verdad); «Re-escanear» visible con lista vacía
  mientras `liveStopped` (condición única en `ScannerActions`, tag `RESCAN` sin duplicar);
  `scanner_live_hint` oculto mientras el muestreo está parado; `requestRescan()` con lista vacía
  limpia y reinicia **sin diálogo y sin reintentos automáticos**; E2E
  `detectorFailureWithEmptyListCanBeRestartedWithRescan` (fallo → tap → `detector.calls ≥ 2`,
  «Plátano» 90 %, `liveStopped` false) presente en el pase 96/96.
- **S8/RD06** — `CameraXLiveFrameSource`: JPEG solo en memoria (`ByteArrayOutputStream`), sin
  `OutputFileOptions`, `bitmap.recycle()` y `ImageProxy.close()` en `finally`; verificado en código y
  por `liveFramesAreNeverWrittenToCacheOrFiles` + `CameraXLiveFrameSourceTest`; Room sin cambios en
  el diff (sin tabla de Escaneos).
- **S11/D1/D2** — `LIVE_SAMPLE_INTERVAL_MS = 300` + doble conflación (`KEEP_ONLY_LATEST` +
  `conflate()`); latencia medida ≤ 800 ms con holgura; visor fluido (> 1 min sin crash, verificación
  manual del implementador con captura).
- **D7** — obturador, `capture()`, `ImageCapture`, `scanner_take_photo` y `scanner_no_detections`
  retirados; «Re-escanear» = vaciar y reiniciar con el diálogo S9 intacto.
- **Non-goals respetados**: sin `sumar-cantidades-al-guardar` (diálogo «Mantener/Actualizar»
  intacto), sin carrusel/Sugerencias/Lista de la Compra, modelo/vocabulario/umbral RD12 intactos,
  `PantryRepository` y esquema Room v2 sin tocar.

## Revisión de calidad y riesgo (rubrica)

- **Correctness**: S1-S11 cumplidas o con evidencia sólida; D1-D8 respetadas.
- **Verificación**: comandos y recuentos exactos registrados y reranqueables; el repair se verificó
  con gates completos antes de volver a `passing`.
- **E2E**: cobertura persistente adaptada al flujo en vivo (17 casos) + adaptadores reales testados aparte.
- **Alcance**: diff acotado al Escáner (puerto/adaptador, dominio, ViewModel, UI, strings, tests, docs);
  sin trabajo de features vecinas; `uxdocs/`, `.opencode/` y scripts del harness intactos.
- **Arquitectura**: puertos en `data/` fakeables, reglas puras en `domain/`, UI sin reglas de negocio,
  patrón MVVM + StateFlow + Koin conservado; identificadores en inglés (P19/P25).
- **Mantenibilidad**: cambios pequeños y documentados; estado `liveStopped` con KDoc; tests por secuencia deterministas.

## Seguridad (checklist por feature)

**Sin hallazgos.** Sin secretos/PII en el diff; sin red, permisos (solo `CAMERA` ya declarada fuera
del diff) ni dependencias nuevas; RD06 sólido (frames solo en memoria, nunca a disco); sin tabla de
Escaneos ni fuga de datos; capturas solo con escena virtual/datos ficticios. S05 no aplica (sin IA).

## Docs duraderas y handoff

- `docs/domain-model.md` (Escaneo en vivo, confianza máxima, ediciones intactas, D8) y
  `docs/technical-discovery.md` (muestreo, conflación, modo IMAGE, cifra D2 medida, `liveStopped`)
  actualizados como exige la spec; `AGENTS.md` sin tocar; `ARCHITECTURE.md`/`CONSTRAINTS.md`
  inexistentes (justificado en la spec).
- `feature_list.json` y `PROGRESS.md` reflejan el estado real tras la aceptación.

## Findings bloqueantes

Ninguno. **0 Critical, 0 High, 0 Medium.**

### Notas no bloqueantes (follow-up)

1. **(RK04, heredada)**: las cajas del HUD se dibujan en coordenadas del fotograma analizado sin
   compensar el letterbox de `FIT_CENTER`; solo observable con comida real (el emulador no la
   inyecta). Laguna ya documentada por la spec; no bloquea.
2. El locale por aplicación fijado con `set-app-locales --locales es-ES` no se limpió con
   `set-app-locales` sin args en la sesión 51 (a diferencia de lo anotado en `PROGRESS.md` para
   API 37); matiz de entorno ya recogido en `PROGRESS.md`.

## Persistencia de la aceptación

- `feature_list.json`: `detectar-alimentos-en-vivo` → `accepted` (**13 de 28**), con el resumen en
  `evidence`.
- `PROGRESS.md` y `docs/progress-history.md` actualizados.
