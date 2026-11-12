# Feature Implementation Spec: Degradar ante fallo de IA

## Source Feature

- `id`: degradar-ante-fallo-de-ia
- `area`: tecnica
- `depends_on`: sugerir-recetas-con-ia (`accepted`)
- `status`: `accepted`
- `source`: `feature_list.json`

## Goal

Endurecer y unificar el **Modo de degradación (RD05)** de la app: cuando falla una llamada a la IA, la app debe **decir qué ha fallado** (sin conexión, proveedor no disponible o respuesta no válida) y **seguir siendo usable a mano** sin bloquear el flujo. Hoy los tres flujos de IA —Escáner, Sugerencias y «Generar foto con IA» del formulario— degradan, pero todos colapsan cualquier error en un `Failed` genérico y el comportamiento de los avisos no es homogéneo (el de Sugerencias no se puede descartar y `clearNotice()` no tiene consumidor). Esta feature introduce una clasificación compartida en `data/ai/`, avisos claros y descartables en las tres superficies y conserva la entrada manual: chips en el Escáner y galería/cámara en el formulario. En Sugerencias la degradación sigue siendo el aviso mínimo con «Reintentar» (decisión ya registrada en `docs/domain-model.md`).

## Non-Goals

- **Reintroducir «Re-escanear»** en el Escáner (retirado por `Q-escanear-despensa-con-ia-5`) ni **reintentos automáticos** o bucles de petición: cada reintento es una acción explícita del usuario.
- **Pre-chequeo de conectividad** con `ConnectivityManager` o permisos nuevos: la causa «sin conexión» se deduce del `IOException` de la llamada (D3).
- **Entrada manual dentro de Sugerencias**: esa pantalla no tiene chips; su degradación es el aviso + «Reintentar» y volver al Escáner. Los chips manuales viven en el Escáner (RD05 completa).
- **Cambiar prompts, modelos, endpoints, payloads, `temperature`, parseo tolerante ni la normalización de respuestas**; solo se clasifica el resultado de error y se propaga la causa.
- **Aceptar/descartar Sugerencias** (`aceptar-sugerencia-de-receta`), persistencia, Room, RD13, `calculateMatch`/`MatchBadge`, RD06 (foto) ni el flujo de captura CameraX.
- **Cambiar el aviso de privacidad** ni cerrar `RK03` (puerta de release) ni hacer **llamadas reales de pago**: toda la verificación usa el cliente simulado.

## Job Story

Cuando la IA no responde porque no tengo red, el proveedor falla o devuelve algo ilegible,
quiero que la app me lo diga con un aviso claro y descartable,
para entender qué ha pasado y seguir trabajando a mano (chips en el Escáner, galería/cámara en el formulario) sin quedarme bloqueado.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P7`, `P31`): usa los tres flujos con su API key (`⚠ S05`). Al degradar no se piden permisos nuevos: cámara solo en el Escáner y el formulario, como hoy. Sin llamadas reales fuera de la que el usuario lance con su key.

## Requisitos funcionales

- **RF-DIA1** (D1): las tres superficies de IA del MVP aplican el mismo criterio de degradación: **Escáner** (`ScannerScreen`/`ScannerViewModel`), **Sugerencias** (`AiSuggestionsScreen`/`AiSuggestionsViewModel`) y **«Generar foto con IA» del formulario** (`CreateRecipeScreen`/`CreateRecipeViewModel`).
- **RF-DIA2** (D2): `data/ai/` clasifica el fallo en tres causas —`NO_CONNECTION`, `PROVIDER_ERROR`, `INVALID_RESPONSE`— y las propaga en los resultados de los puertos. `MissingApiKey` se mantiene como caso propio y previo a la red.
- **RF-DIA3**: cada causa muestra en pantalla un **aviso claro y distinto** (es/en), manteniendo el aviso genérico actual como respaldo de errores inesperados.
- **RF-DIA4** (D4): los avisos de fallo de IA de las tres superficies son **descartables** con un control de 48 dp. En Sugerencias se cablea el `clearNotice()` ya existente (hoy sin consumidor, hallazgo F3 de `sugerir-recetas-con-ia`).
- **RF-DIA5**: la degradación **no bloquea ni pierde trabajo**: el Escáner conserva y permite añadir/editar chips manualmente; el formulario conserva galería y cámara; Sugerencias restaura la selección y ofrece «Reintentar». Nunca queda un estado de carga colgado.
- **RF-DIA6**: el fallo no altera el trabajo previo: los chips existentes y sus cantidades no se tocan (RD13 intacto) y el formulario sigue editable.
- **RF-DIA7** (P19): todos los textos nuevos existen en `values/` (es) y `values-en/` (en) con paridad de claves.

## Acceptance Scenarios

### Scenario 1: Fallo de red en el Escáner y alta manual (verify de `feature_list.json`)

Given el Escáner desplegado y el cliente de IA simulado devolviendo `Failed(NO_CONNECTION)`
When el usuario hace una foto
Then aparece el aviso de «Sin conexión», el panel se pliega, el aviso se descarta sin dejar estados colgados (y reaparece con su causa en un nuevo fallo) y el usuario puede **añadir un Alimento manualmente** y guardarlo en la Despensa

### Scenario 2: Proveedor no disponible

Given el cliente simulado devolviendo `Failed(PROVIDER_ERROR)` (HTTP ≠ 2xx)
When el usuario hace una foto (Escáner), pide sugerencias (Sugerencias) o genera una foto (formulario)
Then cada pantalla muestra su aviso de «el proveedor de IA no está disponible» (sin confundirlo con falta de red) y conserva sus salidas manuales

### Scenario 3: Respuesta no válida

Given el cliente simulado devolviendo `Failed(INVALID_RESPONSE)` (cuerpo nulo, JSON ilegible o forma inesperada)
When termina la llamada
Then aparece el aviso de «respuesta no válida»; el Escáner mantiene los chips y el alta manual, el formulario mantiene galería/cámara y Sugerencias mantiene la selección con «Reintentar»

### Scenario 4: Sin API key (regresión)

Given la app sin key y el cliente devolviendo `MissingApiKey`
When el usuario intenta cualquiera de los tres flujos
Then se mantiene el aviso actual de configurar la key (`S05`) y la entrada manual; no se hace ninguna petición de red

### Scenario 5: Reintento explícito en Sugerencias

Given un fallo de IA en Sugerencias y la selección restaurada
When el usuario pulsa «Reintentar» con el cliente ya en modo éxito
Then se listan las propuestas sin relanzar la pantalla y sin perder la selección (comportamiento actual, ahora con aviso por causa y descartable)

### Scenario 6: Formulario usable tras fallar «Generar foto con IA»

Given el formulario de receta con título e historia y la generación de foto fallando
When el usuario pulsa «Generar con IA»
Then aparece el aviso de la causa, se puede descartar y el usuario sigue pudiendo elegir foto de galería o cámara y guardar/publicar la receta

### Scenario 7: Textos en inglés

When el idioma del sistema es inglés
Then los avisos de las tres causas se muestran en inglés en las tres superficies (P19)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| `UnknownHostException`, `ConnectException`, `SocketTimeoutException` u otro `IOException` | `NO_CONNECTION` (incluye timeout, D3) |
| HTTP 401/403/429/5xx o cualquier no-2xx | `PROVIDER_ERROR` |
| Cuerpo nulo, `choices`/`content` ausente, JSON ilegible, imagen no decodificable, imagen vacía o petición sin alimentos | `INVALID_RESPONSE` |
| Excepción no `IOException` fuera del contrato del puerto | `INVALID_RESPONSE` (respaldo defensivo); `CancellationException` se repropaga sin aviso |
| Sin red y sin API key a la vez | Gana `MissingApiKey`: el cliente comprueba la key antes de la red (sin llamada) |
| Respuesta válida con 0 alimentos (Escáner) | Éxito vacío → aviso `NO_DETECTIONS` actual, no aviso de fallo |
| Respuesta válida con 0 propuestas (Sugerencias) | Estado `Empty` con «Reintentar», no aviso de fallo |
| Aviso descartado / rotación de pantalla | Descartado permanece oculto; Escáner y Sugerencias conservan aviso y estado en su ViewModel y el formulario conserva su estado |
| Fallo tras varias fotos/chips sin guardar | Los chips existentes y sus cantidades no se tocan (RD13 intacto) |

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `degradar-ante-fallo-de-ia` (`area: tecnica`, dependencia `sugerir-recetas-con-ia` `accepted`).
- `docs/domain-model.md` — RD05 y la fila «Fallo de la IA en Sugerencias → aviso mínimo con Reintentar; la degradación manual completa vive en el Escáner».
- `docs/build-brief.md`, `CONTEXT.md`, `DESIGN.md`, `docs/risks-and-open-questions.md` — **Modo de degradación**; estado de error con aviso + manual y 48 dp; `S05`/`RK01`/`RK03`; patrón `Q-…` para las decisiones.
- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanImageAnalyzer.kt`, `RecipeSuggester.kt`, `ImageGenerator.kt` — los tres resultados `Success | MissingApiKey | Failed`.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoAcquirer.kt` — `PhotoResult.GenerationFailed` (objeto) colapsa el fallo de imagen.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleScanImageAnalyzer.kt`, `OpenAiCompatibleRecipeSuggester.kt`, `OpenAiCompatibleImageGenerator.kt` — HTTP, `IOException` y parseo; hoy todo cae en `Failed`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt`, `ScannerScreen.kt` — `ScannerNotice` con `ANALYSIS_FAILED`/`AI_KEY_MISSING`, `NoticeBanner` descartable y alta manual por chips siempre disponible.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt`, `AiSuggestionsScreen.kt`, `AiSuggestionsRoute.kt` — `AiSuggestionsNotice.FAILED` con «Reintentar», `clearNotice()` sin consumidor ni cableado.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt`, `CreateRecipeScreen.kt` — `AI_KEY_MISSING`/`AI_GENERATION_FAILED`, `onDismissNotice`, galería/cámara disponibles.
- Tests: `ScannerViewModelTest`, `AiSuggestionsViewModelTest`, `ScanAnalysisParserTest`, `RecipeSuggestionParserTest`; E2E `ScanPantryE2ETest` (`analysisFailureShowsNoticeAndTheNextPhotoCanRetry`, `missingApiKeyShowsNoticeAndKeepsManualFlowAvailable`), `AiSuggestionsE2ETest` (`failureShowsNoticeAndRetryListsSuggestions`), `CreateRecipeE2ETest` (`generationFailureShowsNoticeAndKeepsManualOptions`) y `PantryRecipesE2ETest` (fakes con `Failed`).

### Existing Patterns To Follow

- Puertos de IA en `data/ai/` con `sealed interface Success | MissingApiKey | Failed`, clientes `OpenAiCompatible*` y fakes en tests.
- MVVM + StateFlow: el ViewModel mapea el resultado del puerto a un `enum` de aviso; la pantalla es stateless y el Route cablea las acciones.
- Aviso como `Surface` `tertiaryContainer` fuera del contenido, con cierre de 48 dp (Escáner) y botones ≥ 48 dp (Sugerencias/formulario).
- E2E Compose sin `NavHost` con fakes del puerto y navegación simulada; unit con `runTest`/`UnconfinedTestDispatcher`; paridad es/en de `strings.xml` verificada en cada feature.

### Current Gaps

- No existe una clasificación de causas: los tres clientes devuelven un único `Failed` y no hay strings por causa.
- El aviso de Sugerencias no se puede descartar y `AiSuggestionsViewModel.clearNotice()` no tiene consumidor.
- La cobertura E2E del fallo existe, pero no asierta la causa ni el descarte del aviso.

## Technical Approach

1. **Clasificación compartida** — `app/src/main/java/com/whatsforlunch/app/data/ai/AiFailure.kt`:

   ```kotlin
   enum class AiFailureCause { NO_CONNECTION, PROVIDER_ERROR, INVALID_RESPONSE }

   internal object AiFailure {
       // Solo para excepciones capturadas; CancellationException se repropaga antes.
       fun fromException(exception: Throwable): AiFailureCause =
           if (exception is java.io.IOException) AiFailureCause.NO_CONNECTION else AiFailureCause.INVALID_RESPONSE
   }
   ```

   HTTP no-2xx → `PROVIDER_ERROR`; cuerpo/contenido/JSON/imagen no utilizables o entrada vacía → `INVALID_RESPONSE`.

2. **Resultados con causa** — cambiar `data object Failed` por `data class Failed(val cause: AiFailureCause)` en `ScanAnalysisResult`, `RecipeSuggestionsResult` e `ImageGenerationResult`, y `PhotoResult.GenerationFailed` por `data class GenerationFailed(val cause: AiFailureCause)`. `MissingApiKey` no cambia.

3. **Clientes** — en los tres `OpenAiCompatible*` y en `SystemPhotoAcquirer.generate`: comprobar la key antes de la red; mapear `!isSuccessful → Failed(PROVIDER_ERROR)`; cuerpo/parseo nulo → `Failed(INVALID_RESPONSE)`; `catch (IOException) → Failed(AiFailure.fromException(e))`; cualquier otra excepción → `Failed(AiFailure.fromException(e))`; `CancellationException` se repropaga. Los `catch` defensivos de `ScannerViewModel.captureAndAnalyze` y `AiSuggestionsViewModel.runRequest` clasifican igual.

4. **ViewModels** — un `when` que mapea causa → aviso:

   | Causa | Escáner (`ScannerNotice`) | Sugerencias (`AiSuggestionsNotice`) | Formulario (`CreateRecipeNotice`) |
   |---|---|---|---|
   | `NO_CONNECTION` | `ANALYSIS_NO_CONNECTION` | `FAILED_NO_CONNECTION` | `AI_GENERATION_FAILED_NO_CONNECTION` |
   | `PROVIDER_ERROR` | `ANALYSIS_PROVIDER_ERROR` | `FAILED_PROVIDER_ERROR` | `AI_GENERATION_FAILED_PROVIDER_ERROR` |
   | `INVALID_RESPONSE` | `ANALYSIS_INVALID_RESPONSE` | `FAILED_INVALID_RESPONSE` | `AI_GENERATION_FAILED_INVALID_RESPONSE` |

   Se conservan `ANALYSIS_FAILED`, `FAILED` y `AI_GENERATION_FAILED` como respaldo para errores fuera del contrato (p. ej. fallo de captura local en el Escáner).

5. **UI** — `ScannerScreen`/`CreateRecipeScreen` solo amplían su `when` de mensajes (el banner ya es descartable). `AiSuggestionsScreen` añade `onClearNotice: () -> Unit` a su firma, lo cablea en `AiSuggestionsRoute` (`viewModel::clearNotice`) y añade un `IconButton` de cierre de 48 dp con `stringResource(R.string.action_dismiss)` en `NoticeBanner`; también hay que actualizar los montajes directos en `AiSuggestionsE2ETest` y `PantryRecipesE2ETest`.

6. **Textos** — claves nuevas en es/en (P19):
   - `scanner_analysis_failed_no_connection`, `scanner_analysis_failed_provider`, `scanner_analysis_failed_invalid_response`.
   - `ai_suggestions_failed_no_connection`, `ai_suggestions_failed_provider`, `ai_suggestions_failed_invalid_response`.
   - `create_recipe_notice_ai_failed_no_connection`, `create_recipe_notice_ai_failed_provider`, `create_recipe_notice_ai_failed_invalid_response`.
   - Copy orientativo (es): «Sin conexión. Comprueba tu red y vuelve a intentarlo; también puedes continuar manualmente.», «El proveedor de IA no está disponible ahora mismo. Inténtalo más tarde o continúa manualmente.», «La IA devolvió una respuesta no válida. Puedes continuar manualmente.». En el formulario, «continuar manualmente» = galería o cámara; en Sugerencias, «Reintentar».

7. **Tests** — `AiFailureTest` (pura: subclases de `IOException` → `NO_CONNECTION`; no-`IOException` → `INVALID_RESPONSE`); actualizar `ScannerViewModelTest` y `AiSuggestionsViewModelTest` a `Failed(causa)` y asertar el mapeo a avisos; actualizar los fakes de `ScanPantryE2ETest`, `AiSuggestionsE2ETest`, `PantryRecipesE2ETest` y `CreateRecipeE2ETest`.

### Decisiones (confirmadas por el usuario, 2026-10-23)

- **D1** las tres superficies comparten patrón (los chips manuales siguen solo en el Escáner). **D2** avisos por causa (sin conexión / proveedor / respuesta no válida). **D3** detección solo tras el fallo, sin `ConnectivityManager`; `IOException` = sin conexión. **D4** acciones actuales + avisos descartables (Sugerencias consume `clearNotice`); sin «Re-escanear».

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/data/ai/AiFailure.kt` — crear; causa compartida y clasificador.
- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanImageAnalyzer.kt`, `RecipeSuggester.kt`, `ImageGenerator.kt` — modificar; `Failed(cause)`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleScanImageAnalyzer.kt`, `OpenAiCompatibleRecipeSuggester.kt`, `OpenAiCompatibleImageGenerator.kt` — modificar; clasificación de errores.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoAcquirer.kt`, `SystemPhotoAcquirer.kt` — modificar; `GenerationFailed(cause)` y mapeo desde `ImageGenerationResult`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/*`, `ui/aisuggestions/*`, `ui/createrecipe/*` — modificar; avisos por causa y descarte en Sugerencias.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; 9 claves nuevas.
- `app/src/test/.../data/ai/AiFailureTest.kt` — crear; clasificador. `ui/scanner/ScannerViewModelTest.kt` y `ui/aisuggestions/AiSuggestionsViewModelTest.kt` — modificar; causas y avisos.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt`, `AiSuggestionsE2ETest.kt`, `CreateRecipeE2ETest.kt`, `PantryRecipesE2ETest.kt` — modificar; fakes con causa, aviso por causa y descarte.
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`, `docs/progress-history.md`, `docs/validations/degradar-ante-fallo-de-ia.md` y `docs/evidence/degradar-ante-fallo-de-ia/*.png` — al implementar/validar.

## Visual Design Impact

- UI involved: yes.
- Design source: `DESIGN.md` (`tertiaryContainer` para avisos, `WarmSpacing`/`WarmRadius`, «Error: fallo de IA/red → degradación con entrada manual y aviso»).
- Screens or states affected: aviso de error del **Escáner**, de **Sugerencias** y del **formulario**; sin pantallas ni navegación nuevas.
- New design artifact required: no — se reutiliza el patrón de banner del Escáner; el único cambio estructural es el botón de cierre (48 dp) en el aviso de Sugerencias.
- Estados verificables: mensaje distinto por causa, aviso descartable, aviso fuera del contenido principal y controles ≥ 48 dp.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y no cambian capas ni dependencias (una utilidad más en `data/ai/`).
- `CONSTRAINTS.md`: not needed — no existe y no se introduce una regla MUST/MUST NOT nueva; RD05 ya está en el dominio.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs:
  - `docs/domain-model.md` — **update**: ampliar RD05 con las tres causas y la degradación por superficie (Escáner manual; Sugerencias aviso+Reintentar; formulario galería/cámara) y ajustar la fila de «Casos límite».
  - `docs/technical-discovery.md` — **update**: documentar `AiFailureCause` y la clasificación (`IOException` → red; HTTP ≠ 2xx → proveedor; parseo → respuesta) en «Integraciones externas».
  - `docs/risks-and-open-questions.md` — **update**: registrar `Q-degradar-ante-fallo-de-ia-1..4` resueltas por el usuario el 2026-10-23 (superficies, avisos por causa, detección tras el fallo, acciones actuales + descartables).
  - `PROGRESS.md` / `docs/progress-history.md` — **update** al cerrar la sesión.

## Implementation Plan

1. Crear `AiFailure.kt` + `AiFailureTest` y cambiar los resultados de los tres puertos y `PhotoResult` a causas; adaptar clientes y `SystemPhotoAcquirer`.
2. Mapear causas a avisos en los tres ViewModels, añadir las 9 cadenas es/en y actualizar las pantallas (banner por causa y cierre en Sugerencias) y sus Routes.
3. Actualizar unit y E2E (causas, descarte y entrada manual) y dejar la regresión completa en verde.
4. Actualizar `domain-model`, `technical-discovery` y `risks-and-open-questions`; capturar evidencia.

## Implementation Tasks

- [x] T1: `AiFailure.kt` + `AiFailureTest` (D2, D3). (RF-DIA2)
- [x] T2: `ScanAnalysisResult`, `RecipeSuggestionsResult`, `ImageGenerationResult` y `PhotoResult` con causa; adaptar los tres clientes y `SystemPhotoAcquirer`. (RF-DIA2, S1-S3, S6)
- [x] T3: mapeo causa→aviso en `ScannerViewModel`, `AiSuggestionsViewModel` y `CreateRecipeViewModel`; cadenas es/en. (RF-DIA3, RF-DIA7, S2-S4, S7)
- [x] T4: `ScannerScreen`, `AiSuggestionsScreen` + `Route` (cierre de 48 dp) y `CreateRecipeScreen`; actualizar montajes directos en tests. (RF-DIA4, RF-DIA6, S1, S5, S6)
- [x] T5: unit (`ScannerViewModelTest`, `AiSuggestionsViewModelTest`) y E2E (`ScanPantryE2ETest`, `AiSuggestionsE2ETest`, `CreateRecipeE2ETest`, `PantryRecipesE2ETest`) con causas, descarte y alta manual. (S1-S7)
- [x] T6: gates en verde, docs actualizadas y evidencia capturada. (RF-DIA1)

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate no bloqueante; no arranca procesos duraderos; no se modifican los scripts del harness).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; reejecución forzada con `gradlew.bat testDebugUnitTest --rerun-tasks` (unit actual 118/118 en 20 suites + `AiFailureTest`).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, AVD API 37) con:
  - **verify de `feature_list.json` (S1)**: `ScanPantryE2ETest.failureNoticeAllowsManualEntryAndSaving` — fake `Failed(NO_CONNECTION)` → aviso de sin conexión, descarte disponible y alta manual de un Alimento que se guarda.
  - S2/S3: casos de `PROVIDER_ERROR` e `INVALID_RESPONSE` en el Escáner, con aviso correcto y manual operativo.
  - S4: `missingApiKeyShowsNoticeAndKeepsManualFlowAvailable` sin regresión.
  - S5: `AiSuggestionsE2ETest.failureShowsNoticeAndRetryListsSuggestions` actualizado a causa + descarte.
  - S6: `CreateRecipeE2ETest.generationFailureShowsNoticeAndKeepsManualOptions` actualizado a causa + descarte + galería/cámara.
  - S7: localización es/en de los avisos nuevos.
  - Regresión completa en verde (suite actual 127 pruebas, 2 `SettingsE2ETest` omitidas por `Assume`), sin tocar Escáner, Sugerencias, Compatibles, Detalle ni Ajustes.
- UI (validador): `uiautomator` + capturas en español de los avisos por causa y del alta manual; sin screenshot testing en el repo.
- **Sin llamadas reales** a DevExpert Inference (solo fakes) y sin API keys reales en ficheros, logs ni evidencia (`⚠ S05`, `RK01`).

## Evidence To Capture

- `docs/validations/degradar-ante-fallo-de-ia.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación S1-S7.
- `docs/evidence/degradar-ante-fallo-de-ia/` — hasta 2 capturas en español: aviso de fallo de IA con el alta manual disponible en el Escáner y aviso de fallo en Sugerencias/formulario con su acción. Presupuesto ≤ 3 artefactos (1 informe + 2 capturas).
- Salida de `AiFailureTest`, `ScannerViewModelTest`, `AiSuggestionsViewModelTest` y de los E2E actualizados.

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (detectar fallo de red o proveedor y avisar claro) | RF-DIA2, RF-DIA3 | S1-S4 | T1-T3 | unit `AiFailureTest` + E2E |
| Step 2 (entrada manual por chips sin bloquear, RD05) | RF-DIA5, RF-DIA6 | S1, S6 | T4, T5 | E2E del Escáner y del formulario |
| Verify 1 (cliente simulado en fallo → aviso + ingrediente manual) | RF-DIA3, RF-DIA5 | S1 | T3-T5 | `ScanPantryE2ETest.failureNoticeAllowsManualEntryAndSaving` |
| — (avisos descartables y consistentes) | RF-DIA4 | S5, S6 | T4 | E2E de Sugerencias y formulario |
| — (es/en) | RF-DIA7 | S7 | T3, T4 | E2E de localización |

## Validator Checklist

- [x] La implementación se queda dentro del alcance: solo clasificación de fallos, avisos y descarte; sin tocar prompts, endpoints, payloads, parseo, RD13, Room ni RD06.
- [x] Los escenarios S1-S7 pasan y las decisiones D1-D4 se respetan (en particular D3: sin `ConnectivityManager`; D4: sin «Re-escanear»).
- [x] `MissingApiKey` sigue comprobándose antes de la red y sin llamada real.
- [x] `AiSuggestionsViewModel.clearNotice()` tiene consumidor y los avisos de las tres superficies son descartables con controles ≥ 48 dp.
- [x] Cobertura E2E persistente actualizada (causas, descarte y alta manual) y suite completa en verde.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente (solo `status` y `evidence` en la lista).
- [x] `docs/domain-model.md`, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` actualizados (`Q-degradar-ante-fallo-de-ia-1..4`).
- [x] Sin llamadas reales de pago ni API keys reales en ficheros, logs o evidencia (`⚠ S05`, `RK01`, `RK03` como puerta de release).
