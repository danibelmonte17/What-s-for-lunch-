# Informe de implementación — Degradar ante fallo de IA

- **Feature**: `degradar-ante-fallo-de-ia`
- **Spec**: `docs/specs/degradar-ante-fallo-de-ia.md`
- **Fecha**: 2026-10-26
- **Estado**: `accepted` (implementación autoverificada y aceptada por validación independiente el 2026-10-26)
- **Aviso**: las secciones 1-6 son el informe del implementador; la sección 7 recoge la validación independiente de `feature-validator`, que es la que sustenta la aceptación.

## 1. Alcance implementado

- **Clasificación compartida (RF-DIA2, D2/D3)**: `data/ai/AiFailure.kt` define `AiFailureCause { NO_CONNECTION, PROVIDER_ERROR, INVALID_RESPONSE }` y `AiFailure.fromException` (todo `IOException`, incluido timeout, → `NO_CONNECTION`; cualquier otra excepción capturada → `INVALID_RESPONSE`). `MissingApiKey` se mantiene como caso propio, comprobado **antes** de la red.
- **Puertos con causa**: `ScanAnalysisResult.Failed(cause)`, `RecipeSuggestionsResult.Failed(cause)`, `ImageGenerationResult.Failed(cause)` y `PhotoResult.GenerationFailed(cause)`. Los tres clientes `OpenAiCompatible*` mapean HTTP ≠ 2xx → `PROVIDER_ERROR`; cuerpo/`choices`/JSON/imagen no utilizables, entrada vacía o petición sin alimentos → `INVALID_RESPONSE`; `IOException` → `NO_CONNECTION`; `CancellationException` se repropaga. `SystemPhotoAcquirer` propaga la causa del generador.
- **Tres superficies (RF-DIA1, RF-DIA3)**: `ScannerViewModel`, `AiSuggestionsViewModel` y `CreateRecipeViewModel` mapean cada causa a un aviso es/en propio; los avisos genéricos (`ANALYSIS_FAILED`, `FAILED`, `AI_GENERATION_FAILED`) quedan como respaldo de errores fuera del contrato.
- **Avisos descartables (RF-DIA4, D4)**: Escáner y formulario conservan su descarte actual; Sugerencias cablea `clearNotice()` con un `IconButton` de 48 dp (`AiSuggestionsTestTags.NOTICE_DISMISS`). Los avisos no bloquean ni dejan estados de carga colgados.
- **Degradación sin pérdida (RF-DIA5, RF-DIA6)**: el Escáner pliega el visor, mantiene los chips y permite alta manual; Sugerencias restaura la selección y ofrece «Reintentar»; el formulario mantiene galería y cámara y sigue editable/guardable. Sin `ConnectivityManager`, sin permisos nuevos, sin reintentos automáticos y sin «Re-escanear» (D3/D4).
- **Textos (RF-DIA7, P19)**: 9 claves nuevas por idioma (3 causas × 3 superficies) en `values/` y `values-en/`.

### Fuera de alcance respetado

No se tocan prompts, endpoints, payloads, parseo, RD13, Room, RD06, `/images/generations` (salvo propagar la causa del fallo), ni la navegación. No hay llamadas reales a DevExpert Inference ni API keys reales en ficheros, logs o evidencia (`⚠ S05`, `RK01`).

## 2. Ficheros

### Creados

- `app/src/main/java/com/whatsforlunch/app/data/ai/AiFailure.kt` — `AiFailureCause` y clasificador `AiFailure`.
- `app/src/test/java/com/whatsforlunch/app/data/ai/AiFailureTest.kt` — 2 tests (IOException/timeout → sin conexión; resto → respuesta no válida).

### Modificados

- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanImageAnalyzer.kt`, `RecipeSuggester.kt`, `ImageGenerator.kt` — resultados con causa.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleScanImageAnalyzer.kt`, `OpenAiCompatibleRecipeSuggester.kt`, `OpenAiCompatibleImageGenerator.kt` — clasificación de HTTP, cuerpo, JSON e `IOException`.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoAcquirer.kt`, `SystemPhotoAcquirer.kt` — `GenerationFailed(cause)` y propagación.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` / `ScannerScreen.kt` — 3 avisos por causa.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt`, `AiSuggestionsScreen.kt`, `AiSuggestionsRoute.kt` — 3 avisos por causa, descarte de 48 dp y `clearNotice()`.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` / `CreateRecipeScreen.kt` — 3 avisos por causa.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — 9 claves nuevas por idioma.
- `app/src/test/java/com/whatsforlunch/app/ui/scanner/ScannerViewModelTest.kt` — causa por caso y mapeo de las tres causas.
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt` — causa, mapeo de las tres causas y `clearNotice()`.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — S1 (`failureNoticeAllowsManualEntryAndSaving`), S2, S3, S7.
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — S5 (causa + reintento + descarte), S7 y montajes directos.
- `app/src/androidTest/java/com/whatsforlunch/app/CreateRecipeE2ETest.kt` — S6 (causa + descarte + galería/cámara + guardado) y S3.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryRecipesE2ETest.kt` — montaje de Sugerencias y fake con causa.
- `docs/domain-model.md` — RD05 ampliada, escenario 4 y casos límite.
- `docs/technical-discovery.md` — clasificación de fallos y `Failed(cause)`.
- `docs/risks-and-open-questions.md` — `Q-degradar-ante-fallo-de-ia-1..4` resueltas.
- `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` (`status` y `evidence`).

## 3. Verificación ejecutada

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` | **exit 0** (AVD `Pixel_10_Pro` API 37, `emulator-5554`) |
| Lint | `gradlew.bat :app:lintDebug` | **0 errores** (warnings preexistentes) |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **150/150 en 25 suites**, 0 fallos, 0 errores, 0 omitidas |
| Build | `gradlew.bat :app:assembleDebug` / `:app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** |
| E2E de la feature (focalizada) | `connectedDebugAndroidTest` sobre `ScanPantryE2ETest`, `AiSuggestionsE2ETest`, `CreateRecipeE2ETest`, `PantryRecipesE2ETest` | **68/69** en el primer pase; el único fallo era una aserción del propio test (`performScrollTo` sobre el dock no desplazable) corregida; el test reejecutado en solitario pasa |
| Suite E2E completa | `connectedDebugAndroidTest` | **OK (143 tests: 141 ejecutados + 2 `SettingsE2ETest` omitidos por `Assume`, 0 fallos)**, 4m56s |

Notas de entorno: `JAVA_HOME` estaba definido en una ruta inválida (`C:\Users\Daniel\.jdks\corretto-17.0.20\bin`); los comandos se ejecutaron con `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. El primer pase focalizado dio 68/69: el único fallo era una aserción del propio test (`performScrollTo` sobre el botón del dock, que no está en un contenedor desplazable), corregida y reejecutada en solitario en verde; la suite completa posterior confirmó el conjunto. Toda la verificación se hizo en el AVD `emulator-5554`; el dispositivo físico no se tocó.

## 4. Cobertura de escenarios

| Escenario | Cobertura | Test / evidencia |
|---|---|---|
| S1 Fallo de red en Escáner + alta manual + guardado + descarte + reaparición | E2E | `ScanPantryE2ETest.failureNoticeAllowsManualEntryAndSaving` (verify de `feature_list.json`) |
| S2 Proveedor no disponible | E2E + unit | `ScanPantryE2ETest.providerFailureShowsNoticeAndTheNextPhotoCanRetry`, `AiSuggestionsE2ETest.failureNoticeCanBeDismissed`, mapeos en unit |
| S3 Respuesta no válida | E2E + unit | `ScanPantryE2ETest.invalidResponseShowsNoticeAndKeepsManualEntry`, `CreateRecipeE2ETest.invalidAiResponseShowsItsOwnNoticeAndKeepsManualPhotoOptions` |
| S4 Sin API key (regresión) | E2E | `ScanPantryE2ETest.missingApiKeyShowsNoticeAndKeepsManualFlowAvailable` (sin cambios) |
| S5 Reintento en Sugerencias | E2E + unit | `AiSuggestionsE2ETest.failureShowsNoticeAndRetryListsSuggestions`, `failureNoticeCanBeDismissed`; `AiSuggestionsViewModelTest.eachFailureCauseMapsToItsOwnSuggestionsNoticeAndCanBeCleared` |
| S6 Formulario usable tras fallar la generación | E2E | `CreateRecipeE2ETest.generationFailureShowsNoticeAndKeepsManualOptions` |
| S7 Textos en inglés | E2E + paridad | `ScanPantryE2ETest.failureNoticeIsLocalizedInEnglish`, `AiSuggestionsE2ETest.failureNoticeIsLocalizedInEnglish`; 9 claves con paridad es/en |
| D3 sin `ConnectivityManager`; D4 sin reintentos ni «Re-escanear» | Inspección | barrido en `app/src/main` |
| `MissingApiKey` antes de la red | Inspección + tests existentes | los tres clientes comprueban la key antes de construir la petición |

## 5. Evidencia visual

- `docs/evidence/degradar-ante-fallo-de-ia/escaner-fallo-sin-conexion-es.png` (1280×2856, 130.474 B) — aviso «Sin conexión…» tras una foto, con «Nombre del alimento»/«Añadir alimento» y el visor plegado.
- `docs/evidence/degradar-ante-fallo-de-ia/formulario-fallo-sin-conexion-es.png` (1280×2856, 249.958 B) — aviso «Sin conexión…» tras «Generar con IA», con «Galería», «Cámara» y el formulario editables.

Ambas se obtuvieron con un build temporal cuyo `AI_BASE_URL` apuntaba a `http://127.0.0.1:9/v1` (conexión rechazada) y una `AI_API_KEY` sintética `sk-test-*`, así que **no hubo ninguna llamada real ni coste**. La suite E2E se había ejecutado antes, con el APK normal y la configuración real de `local.properties`; el build temporal se usó solo para las capturas. Después se restauró `local.properties` desde una copia fuera del repositorio, se eliminó la copia y `init.ps1` reconstruyó e instaló el APK normal (exit 0).

## 6. Riesgos y notas

- Sin hallazgos conocidos de seguridad. Las capturas usan datos ficticios y una key sintética que no se versiona; `local.properties` quedó restaurado y gitignored.
- La suite completa terminó sin fallos ni flakes en esta pasada (143 tests: 141 ejecutados + 2 omitidos por `Assume`). La focalizada inicial requirió corregir una aserción del test, no código de producción.
- `init.ps1` se ejecutó al final con el APK normal y terminó en «Entorno listo».

## 7. Validación independiente y aceptación

- **Validador**: subagente `validator` (rol `feature-validator`), independiente del implementador.
- **Fecha**: 2026-10-26.
- **Veredicto**: **`accept`** → estado recomendado y persistido: **`accepted`**.
- **Hallazgos bloqueantes**: ninguno (0 Critical, 0 High, 0 Medium). Solo notas Low L1-L6, ninguna impide la aceptación.

### Checks reejecutados por el validador (árbol actual, sin commitear)

| Check | Resultado |
|---|---|
| `gradlew.bat :app:lintDebug :app:testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL in 42s**; lint **0 errores / 49 warnings** (0 en ficheros cambiados) |
| Unit | **150/150 en 25 suites**, 0 fallos/errores/omitidos; `AiFailureTest` 2/2, `ScannerViewModelTest` 14/14, `AiSuggestionsViewModelTest` 13/13 |
| `:app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** |

Los checks caros no se reejecutaron (emulador apagado, política de la skill): se inspeccionó la evidencia registrada.

### Evidencia cara inspeccionada

- XML de la suite completa: **143 tests, 141 ejecutados + 2 `SettingsE2ETest` omitidos por `Assume`, 0 fallos**, 276,496 s. El recuento de L1 quedó corregido en este informe, `PROGRESS.md`, historial y `feature_list.json`.
- 2 PNG válidos (1280×2856) y coherencia con el aviso sin conexión y la salida manual.
- `git diff` solo con los ficheros previstos; `feature_list.json` solo cambió `status` y `evidence`; paridad es/en **222/222**; manifest sin permisos nuevos; sin `ConnectivityManager` ni «Re-escanear» (D3/D4); `MissingApiKey` antes de la red; logcat sin endpoint de IA ni API key.
- S1-S7 y D1-D4 cubiertos; cobertura E2E persistente actualizada (los E2E tocados incluyen `PantryRecipesE2ETest`, nombre real del fichero que la spec citaba como `CompatibleRecipesE2ETest`).

### Notas Low (no bloqueantes)

- **L1 — recuento E2E**: corregido a 143 tests totales = 141 ejecutados + 2 omitidos.
- **L2 — `OpenAiCompatibleImageGenerator`**: no captura una `Exception` genérica; las rutas conocidas quedan clasificadas. Se acepta como deuda menor, sin cambio de alcance en esta validación.
- **L3 — formulario**: no hay E2E específico de `PROVIDER_ERROR` ni del aviso en inglés en esa superficie; el mapeo está en el ViewModel y la rama de respuesta no válida sí tiene E2E.
- **L4 — 48 dp**: el tamaño del cierre de Sugerencias se verificó por inspección, no por medición E2E.
- **L5 — metadatos de la spec**: actualizados en esta aceptación (estado, T1-T6 y checklist).
- **L6 — contexto de evidencia**: aclarado que la suite E2E usó el APK/configuración normal y el build temporal se limitó a las capturas.

### Seguridad y docs

Sin llamadas reales ni coste; sin API keys reales en ficheros, logs ni evidencia; `local.properties` restaurado y gitignored. `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (`Q-degradar-ante-fallo-de-ia-1..4`) y el harness quedaron coherentes con el código. **Aceptación persistida por el orquestador**; el validador no modificó archivos.
