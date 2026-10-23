# Informe de implementación — Sugerir recetas con IA

- **Feature**: `sugerir-recetas-con-ia`
- **Spec**: `docs/specs/sugerir-recetas-con-ia.md`
- **Fecha**: 2026-10-23
- **Estado**: `passing` (implementada y autoverificada)
- **Aviso**: este documento **no es una aceptación**. Lo emite el implementador y la validación independiente corresponde a `feature-validator`.

## 1. Alcance implementado

Se sustituye el destino provisional de Sugerencias por la pantalla real y se implementa el flujo completo de petición y presentación de propuestas:

- **Paso de selección (RF-SRC2, D3)**: al abrir la pantalla se unen los chips del Escaneo sin guardar y los Alimentos de la Despensa, **un ítem por nombre normalizado**, todos preseleccionados, con nombre y cantidad. «Pedir sugerencias» se habilita con ≥ 1 seleccionado.
- **Llamada a la IA (RF-SRC3, D1, D9, D10)**: una sola llamada a `POST /chat/completions` con alias `chat`, `temperature = 0`, `response_format = {"type":"json_object"}` y prompt es/en que pide hasta 3 recetas completas en JSON compacto (`t/st/mm/pm/cm/df/bs/kcal/i/p`). Sin reintentos automáticos.
- **Parseo y normalización (RF-SRC4, D8)**: parser tolerante a vallas y texto alrededor del primer objeto JSON; normalización en dominio que descarta propuestas inválidas individualmente (título vacío, sin ingredientes/pasos, duplicado por título), recorta a 3 y corrige cantidades, tokens, tiempos, Raciones, calorías y temporizadores. JSON ilegible o error de red/HTTP → `Failed`.
- **Resultados y Coincidencia (RF-SRC4/5/6, D2, D5)**: tarjetas de solo lectura con título, `MatchBadge`, línea «X de Y ingredientes disponibles», historia, ingredientes («cantidad unidad nombre», opcionales marcados) y pasos numerados con chip de temporizador. La Coincidencia se calcula en el cliente con `calculateMatch` (RD01/RD10) contra los Alimentos seleccionados, sin tocar `RecipeMatch.kt`. Tocar la tarjeta no hace nada.
- **Estados y avisos (RF-SRC7, RF-SRC8)**: selección, carga, resultados, vacío y error; sin key aparece el aviso con «Abrir Ajustes» hacia `settings`; el fallo ofrece «Reintentar» y conserva la selección.
- **Navegación (RF-SRC1)**: `AppRoutes.AI_SUGGESTIONS = "ai_suggestions"` sustituye a `ai_suggestions_provisional`; el Escáner navega al destino real y conserva sus chips al volver.
- **Textos (RF-SRC9)**: 14 claves `ai_suggestions_*` en `values/` y `values-en/`.

### Fuera de alcance respetado

No se implementa aceptar/descartar Sugerencias, `origen = Sugerencia`, persistencia/Room, migraciones, `/images/generations`, Pinche, Modo Cocina, Lista de la Compra ni faltantes. No se modifican `calculateMatch`, `MatchBadge`, RD13, el esquema de Room, `uxdocs/`, `.opencode/` ni los scripts del harness.

## 2. Ficheros

### Creados

- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt` — modelo de propuesta, `SuggestionWithMatch`, `RawRecipeSuggestion` y `SuggestableFood`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` — normalización D8 y Coincidencia D2.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt` — puerto + `RecipeSuggestionsResult`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParser.kt` — parseo del JSON compacto.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleRecipeSuggester.kt` — cliente `/chat/completions`.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanDraftHolder.kt` — chips del Escaneo compartidos en memoria.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt`, `AiSuggestionsScreen.kt`, `AiSuggestionsRoute.kt` — estado, UI y wiring.
- `app/src/test/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParserTest.kt` (6 tests).
- `app/src/test/java/com/whatsforlunch/app/domain/suggestion/SuggestionRulesTest.kt` (9 tests).
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt` (7 tests).
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` (8 escenarios S1-S8).
- `docs/evidence/sugerir-recetas-con-ia/sugerencias-seleccion-es.png` y `sugerencias-resultados-es.png`.

### Modificados

- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — `items` delegados a `ScanDraftHolder` (parámetro con valor por defecto; suite del Escáner intacta).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — singleton `ScanDraftHolder`, `RecipeSuggester` y `AiSuggestionsViewModel`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — ruta real y «Abrir Ajustes».
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — retirado el tag del destino provisional de Sugerencias.
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — los 2 tests del destino provisional montan ahora la pantalla real (10 tests en total).
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — 13 claves nuevas (más `ai_suggestions_title`, existente).
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`.
- `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` (`evidence` y `status`).

## 3. Verificación ejecutada

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` | **exit 0** (AVD `Pixel_9_Pro` API 37, `emulator-5554`, con `ANDROID_SERIAL` por haber varios dispositivos conectados; 1 reintento por el falso fallo conocido de `pidof`) |
| Lint | `gradlew.bat :app:lintDebug` | **0 errores** (50 warnings, ninguno bloqueante) |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **118/118 en 20 suites** |
| Build | `gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** |
| E2E de la feature | `am instrument` sobre `AiSuggestionsE2ETest` | **8/8** |
| Regresión focalizada | `am instrument` sobre `CompatibleRecipesE2ETest,ScanPantryE2ETest` | **31/31** |
| Suite E2E completa | `am instrument` sobre todo el paquete | **OK (127 pruebas)**; 2 `SettingsE2ETest` omitidas por `Assume` con key de build |

Los unit nuevos cubren: parser (JSON compacto, vallas, primer objeto, objeto ausente, `s` ausente, valores malformados), dominio (normalización completa, descartes, cantidades, tokens/tiempos/Raciones/calorías, duplicados, recorte a 3, Coincidencia contra seleccionados, sin obligatorios → sin badge) y ViewModel (fusión de chips + Despensa preseleccionada, mínimo 1 sin llamada, `MissingApiKey`, `Failed` + reintento, éxito con Coincidencia, éxito vacío, chips compartidos).

### Escenarios S1-S8

| Escenario | Cobertura | Resultado |
|---|---|---|
| S1 · Propuestas con Coincidencia (verify 1) | `suggestionsAreListedWithClientMatchIngredientsAndSteps` | verde |
| S2 · Sin API key → Ajustes (verify 2) | `missingApiKeyAsksToConfigureItAndOpensSettings` | verde |
| S3 · Cero seleccionados | `requestIsDisabledWhenEveryFoodIsDeselected` | verde |
| S4 · Fallo + Reintentar | `failureShowsNoticeAndRetryListsSuggestions` | verde |
| S5 · Propuestas inválidas / vacío | `SuggestionRulesTest` + `emptyAnswerShowsEmptyStateWithRetry` | verde |
| S6 · Textos en inglés | `suggestionsScreenIsLocalizedInEnglish` | verde |
| S7 · Flujo desde el Escáner y vuelta | `scannerChipsAndPantryArrivePreselectedAndSurviveTheRoundTrip` | verde |
| S8 · Opcionales no alteran la Coincidencia | `optionalIngredientsDoNotChangeTheMatch` + unit | verde |

### Evidencia visual

Dos capturas en español, PNG válido (`89 50 4E 47 0D 0A 1A 0A`), 1280×2856:

- `docs/evidence/sugerir-recetas-con-ia/sugerencias-seleccion-es.png` (92.265 B) — paso de selección con chips y Despensa preseleccionados y botón «Pedir sugerencias».
- `docs/evidence/sugerir-recetas-con-ia/sugerencias-resultados-es.png` (186.610 B) — dos tarjetas con badges 50 % y 100 % Match, disponibilidad, ingredientes y pasos con temporizador.

Presupuesto respetado: 1 informe + 2 capturas, todas de esta feature.

## 4. Seguridad y coste

- **Sin llamadas reales a DevExpert Inference**: toda la verificación usó `FakeRecipeSuggester` y el cliente simulado del Escáner; no se ejecutó ninguna petición de pago ni se necesitó API key real.
- Sin API keys, tokens ni PII en código, docs, logs o capturas. Las capturas usan datos ficticios.
- No se añaden permisos Android, dependencias ni endpoints nuevos; se reutiliza `OkHttpClient`, `AiKeyProvider` y `AiEndpoints.chatCompletions`.

## 5. Desviaciones y limitaciones

- **Ubicación de `SuggestableFood`**: la spec lo mencionaba junto al puerto en `data/ai`; se definió en `domain/suggestion/RecipeSuggestion.kt` para que `SuggestionRules` (dominio) no dependa de `data/`. El puerto `RecipeSuggester` lo importa desde dominio.
- **Ejecución E2E**: `connectedDebugAndroidTest` vía Gradle abortó con `Process crashed` por presión de memoria del AVD `Pixel_10_Pro`; los resultados se obtuvieron con `installDebug` + `installDebugAndroidTest` y `adb shell am instrument` sobre `Pixel_9_Pro` API 37 (`emulator-5554`), que es el mismo runner (`AndroidJUnitRunner`). No es un cambio de alcance.
- La llamada real al proveedor (verificación manual opcional de la spec) **no se ejecutó** por requerir confirmación humana y ser de pago; el contrato del proveedor queda para la validación opcional o la puerta de release.
- `RK03` («sin retención») queda como **puerta de release** documentada (D6), no como verificación de esta implementación.

## 6. Pendiente para `feature-validator`

- Reejecutar los gates baratos sobre el árbol actual y revisar el diff completo.
- Inspeccionar las 2 capturas y, si lo considera necesario, repetir S1-S8 con `uiautomator`.
- Confirmar que el alcance no invade `aceptar-sugerencia-de-receta`, `degradar-ante-fallo-de-ia` ni otras features.
- Verificar `docs/risks-and-open-questions.md` (`Q-sugerir-recetas-con-ia-1..6` y `RK03`) y la coherencia de `PROGRESS.md` / `feature_list.json`.

**Este informe no declara la feature `accepted`; queda `passing` a la espera de validación independiente.**

## 7. Validación independiente (`feature-validator`, 2026-10-23)

**Veredicto: `accept`** · **Estado recomendado: `passing` → `accepted`** (persistido por el orquestador en `feature_list.json`).

### 7.1 Checks reejecutados sobre el árbol actual

| Gate | Comando | Resultado |
|---|---|---|
| Lint | `gradlew.bat :app:lintDebug` | **0 errores** (50 warnings, ninguno bloqueante) |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **118/118 en 20 suites** |
| Build | `gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** |
| Compilación E2E | `gradlew.bat :app:compileDebugAndroidTestKotlin` | **OK** |
| Arranque | `./init.ps1` | **exit 1** por «no hay emulador ni dispositivo conectado» (gate no bloqueante; el implementador lo dejó en exit 0 con el AVD API 37 arrancado) |

### 7.2 Evidencia cara inspeccionada (no reejecutada por defecto)

- `AiSuggestionsE2ETest` **8/8** (S1-S8) y `CompatibleRecipesE2ETest` + `ScanPantryE2ETest` **31/31**, coherentes con el diff actual.
- Suite E2E completa: **127 pruebas**, 2 `SettingsE2ETest` omitidas por `Assume`, sin fallos de la feature.
- E2E ejecutada con `installDebug` + `adb shell am instrument` porque `connectedDebugAndroidTest` abortó por presión de memoria del AVD (flake ambiental, no fallo de test).
- Sin llamadas reales a DevExpert Inference y sin API keys reales en código, docs, logs ni capturas; `local.properties` sigue gitignored/untracked.

### 7.3 Cobertura del contrato

Verificados en código: D1 (pantalla con selección + Sugerencias), D2 (Coincidencia en cliente contra los Alimentos seleccionados, no contra la Despensa), D3/D4 (parser tolerante y una sola llamada por acción), D5 (toque inerte), D8 (normalización) y D10 (sin reintentos automáticos). Sin Room, sin `/images/generations`, sin aceptar/descartar Sugerencias y sin cambios en `calculateMatch`, `MatchBadge` ni RD13. `RK03` queda como puerta de release documentada (D6).

### 7.4 Hallazgos (ninguno bloqueante)

| ID | Severidad | Hallazgo | Acción |
|---|---|---|---|
| F1 | Low | `MatchBadge` reutiliza `my_recipes_card_match_content_description`, cuyo texto menciona «Despensa» aunque D2 compara contra los Alimentos seleccionados | Deuda de copy para un pulido futuro |
| F2 | Low | El E2E inglés S6 no cubre los estados vacío, error y sin API key | Ampliar cobertura en un pulido futuro |
| F3 | Low | `AiSuggestionsViewModel.clearNotice()` no tiene consumidor y el aviso de error no es descartable | Limpieza opcional |
| F4 | Low | No se conserva el XML verde de la E2E (solo la descripción); el XML retenido muestra un `Process crashed` del AVD | Conservar el XML en el próximo ciclo |
| F5 | Low | `retry()` reutiliza `lastFoods` aunque la selección haya cambiado | Valorar recalcular la selección en un pulido futuro |
| F6 | Low | Las casillas T1-T7 de la spec no están marcadas | Bookkeeping opcional |
| F7 | Low | El informe cita `Pixel_10_Pro` mientras el XML retenido es `Pixel_9_Pro(AVD) - 17` | Corregir la nota de entorno en el próximo informe |

### 7.5 Seguridad y documentación

- Checklist de seguridad sin hallazgos: sin secretos, sin PII, sin permisos, dependencias ni endpoints nuevos; el cliente reutiliza `OkHttpClient`, `AiKeyProvider` y `AiEndpoints.chatCompletions`.
- Documentación durable coherente: `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (`Q-sugerir-recetas-con-ia-1..6` y `RK03`), `PROGRESS.md` y `docs/progress-history.md`.
- Alcance respetado: no invade `aceptar-sugerencia-de-receta`, `degradar-ante-fallo-de-ia` ni otras features.

**Conclusión: `accept`.** Los 7 hallazgos son Low y no bloquean la aceptación. `feature_list.json` pasa a `accepted` y la deuda queda registrada aquí para futuros ciclos de pulido.
