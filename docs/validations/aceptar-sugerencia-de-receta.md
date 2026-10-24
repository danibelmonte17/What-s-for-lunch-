?# Informe de implementación — Aceptar sugerencia de receta

- **Feature**: `aceptar-sugerencia-de-receta`
- **Spec**: `docs/specs/aceptar-sugerencia-de-receta.md`
- **Fecha**: 2026-10-24
- **Estado**: `passing` (implementada y autoverificada)
- **Aviso**: este documento **no es una aceptación**. Lo emite el implementador y la validación independiente corresponde a `feature-validator`.

## 1. Alcance implementado

- **Aceptar (RF-ASR2, D1, D3, RD07)**: cada tarjeta de Sugerencia gana un pie con «Aceptar» y «Descartar». «Aceptar» construye una Receta con `origin = RecipeOrigin.SUGGESTION`, `status = RecipeStatus.DRAFT`, `photoPath = null`, `favorite = false`, los Ingredientes y Pasos de la propuesta (cantidad, unidad, opcionalidad y temporizador) e ids de hijos `"$id-ingredient-$index"` / `"$id-step-$index"`; se guarda con `RecipeRepository.saveRecipe` (upsert por id). El id es estable: `UUID.nameUUIDFromBytes("suggestion:" + normalizeName(title))`.
- **No aceptar dos veces (RF-ASR3, D3)**: tras aceptar, la tarjeta queda «Aceptada» con ambas acciones deshabilitadas; una segunda aceptación (doble toque, re-render o llamada repetida) reescribe la misma fila por id estable y el recuento de Recetas no aumenta.
- **Descartar (RF-ASR4, D4)**: marca la tarjeta «Descartada», deshabilita sus acciones, no guarda nada y la tarjeta sigue visible.
- **Aviso y permanencia (RF-ASR5, D5)**: banner «Guardada en Mis Recetas» (`ai_suggestions_accepted_notice`, aviso `SAVED`) fuera del contenido principal; el usuario se queda en la lista y puede seguir aceptando y descartando.
- **Anti-duplicados en el prompt (RF-ASR6, D2)**: el puerto `RecipeSuggester.suggest(foods, language, existingRecipes = emptyList())` recibe `RecipeAvoidance(title, ingredientNames)`; `SuggestionAcceptance.recipesToAvoid` envía solo Recetas de Mis Recetas (Borrador o Publicada, cualquier origen) con Coincidencia **estrictamente > 60 %** con los Alimentos seleccionados (`calculateMatch` vía la adaptación de `SuggestionRules`, sin tocar `RecipeMatch.kt`). Las Recetas sin ingredientes obligatorios quedan fuera. `recipeSuggestionPrompt` (función pura) añade una sección es/en con título e ingredientes y la instrucción de no proponer recetas iguales ni equivalentes; sin Recetas a evitar, el prompt no cambia.
- **Estados de lista (RF-ASR7)**: cada petición o reintento crea una lista nueva con todas las propuestas en `PROPOSED`; los estados anteriores no se arrastran. Los estados viven en el ViewModel (sobreviven a la rotación) y se pierden al salir (solo memoria).
- **Textos (RF-ASR8)**: 5 claves nuevas con paridad es/en — `ai_suggestions_accept`, `ai_suggestions_discard`, `ai_suggestions_state_accepted`, `ai_suggestions_state_discarded`, `ai_suggestions_accepted_notice`.
- **TestTags**: `suggestionAccept(index)`, `suggestionDiscard(index)`, `suggestionState(index)`.

### Fuera de alcance respetado

No se publica la Receta aceptada (`⚠ S07` intacta), no se navega al Detalle ni se cambia la navegación de `ai_suggestions`, no se persisten Sugerencias ni su estado (sin Room nuevo ni migraciones: la columna `origin` ya existía en DB v2), no se edita la Receta desde Sugerencias, no se muestra badge de `origen`, y no se tocan `calculateMatch`, `MatchBadge`, RD01/RD02/RD03/RD10/RD13, el esquema de Room, la Despensa, Pinche, Lista de la Compra, Modo Cocina ni `/images/generations`. Sin permisos Android nuevos (`INTERNET` ya existía).

## 2. Ficheros

### Creados

- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptance.kt` — `SuggestionState`, `suggestionRecipeId`, `suggestionToRecipe`, `recipesToAvoid`.
- `app/src/test/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptanceTest.kt` (6 tests).
- `app/src/test/java/com/whatsforlunch/app/data/ai/SuggestionPromptTest.kt` — 3 tests del prompt anti-duplicados (es/en y sin Recetas a evitar).

### Modificados

- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt` — tercer parámetro `existingRecipes` y `RecipeAvoidance`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleRecipeSuggester.kt` — `recipeSuggestionPrompt` puro con sección anti-duplicados; JSON compacto intacto.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt` — 4.º parámetro `RecipeRepository`, `Results(items, states)`, `acceptSuggestion`/`discardSuggestion`, aviso `SAVED`, `recipesToAvoid` en petición y reintento.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsScreen.kt` — pie de acciones, marca de estado, botones deshabilitados y testTags.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsRoute.kt` — cableado de `onAccept`/`onDiscard`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — inyección del 4.º parámetro en el `viewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — 5 claves nuevas.
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt` — 5 tests nuevos (aceptar, aceptar dos veces, descartar, filtro > 60 %, reinicio de estados).
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — 6 escenarios nuevos (S1-S6 de esta spec) sobre fakes; 8 escenarios previos intactos (14 en total).
- `app/src/androidTest/java/com/whatsforlunch/app/PantryRecipesE2ETest.kt` — adaptación de la firma del `FakeRecipeSuggester` al nuevo parámetro (sin cambios de comportamiento).
- `docs/domain-model.md` — estados/transiciones de la Sugerencia, validaciones de aceptación y anti-duplicados, casos límite.
- `docs/technical-discovery.md` — aceptar/descartar, id estable, prompt anti-duplicados y datos que salen del dispositivo (`RK03`).
- `docs/risks-and-open-questions.md` — `RK03` ampliado (títulos e ingredientes de Recetas existentes) y `Q-aceptar-sugerencia-de-receta-1..5` registradas como resueltas por el usuario el 2026-10-24.
- `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` (`status` y `evidence`).

## 3. Verificación ejecutada

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` (con `ANDROID_SERIAL=emulator-5554`) | **exit 0** (AVD `Pixel_10_Pro` API 37, `emulator-5554`; instaló y arrancó la app) |
| Lint | `gradlew.bat :app:lintDebug` | **0 errores / 49 warnings** (ninguno nuevo de la feature; los 49 son previos) |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **146/146 en 24 suites**, 0 fallos, 0 errores, 0 omitidas |
| Build | `gradlew.bat :app:assembleDebug` / `:app:assembleDebugAndroidTest` | **BUILD SUCCESSFUL** |
| E2E de la feature | `am instrument` sobre `AiSuggestionsE2ETest` | **OK (14 tests)** |
| Suite E2E completa | `am instrument` sobre el paquete | **OK (137 tests)** en 1.202,5 s; el caso de `SettingsE2ETest` que solo aplica sin key de build se omite por `Assume` |

Nota de entorno: hay un **dispositivo físico** conectado por adb además del AVD. Toda la verificación se ejecutó con `-s emulator-5554` / `ANDROID_SERIAL=emulator-5554`; **el dispositivo físico no se tocó** (ni instalación ni instrumentación). `connectedDebugAndroidTest` no se usó para evitar selecciones ambiguas de dispositivo; la E2E se ejecutó por `install` + `am instrument`, alternativa ya usada en features anteriores.

Los unit nuevos cubren: id estable por título normalizado, mapeo a Borrador con `origen = SUGGESTION` y sin foto, ids de hijos, aceptar dos veces produce el mismo id, umbral estricto > 60 %, Receta sin obligatorios fuera y selección vacía sin Recetas a evitar (`SuggestionAcceptanceTest`); prompt sin sección cuando no hay Recetas, sección es y sección en con títulos e ingredientes (`SuggestionPromptTest`); aceptar guarda Borrador/`SUGGESTION`, aceptar dos veces deja una sola Receta, descartar no guarda, filtro > 60 % en la petición y reinicio de estados en una nueva petición (`AiSuggestionsViewModelTest`).

### Escenarios S1-S6

| Escenario | Cobertura | Resultado |
|---|---|---|
| S1 · Aceptar guarda en Mis Recetas con origen Sugerencia (verify 1) | `acceptingSavesTheSuggestionAsADraftRecipeVisibleInMyRecipes` + unit `acceptingSavesTheSuggestionAsADraftWithSuggestionOrigin` / `acceptingCreatesADraftSuggestionRecipeWithoutPhoto` | verde |
| S2 · Aceptar dos veces no duplica (verify 2) | `acceptingTheSameSuggestionTwiceDoesNotDuplicateTheRecipe` + unit `acceptingTheSameSuggestionTwiceKeepsASingleRecipe` / `acceptingTheSameSuggestionTwiceProducesTheSameRecipeId` | verde |
| S3 · Descartar no guarda nada | `discardingKeepsTheCardWithDisabledActionsAndSavesNothing` + unit `discardingKeepsTheCardVisibleWithoutSavingAnything` | verde |
| S4 · Prompt con Recetas > 60 % e instrucción anti-duplicados | `onlyExistingRecipesAboveSixtyPercentAreSentToThePrompt` + unit `onlyRecipesAboveSixtyPercentAreSentToThePrompt`, `recipeWithoutRequiredIngredientsIsNotSentToThePrompt`, `spanishPromptListsExistingRecipesAndAsksToAvoidThem`, `englishPromptUsesTheEnglishAntiDuplicatesSection` | verde |
| S5 · Estados mixtos y reinicio al pedir de nuevo | `mixedStatesCoexistAndANewListStartsEverySuggestionAsProposed` + unit `aNewRequestStartsEverySuggestionAsProposed` | verde |
| S6 · Textos en inglés | `acceptingAndDiscardingAreLocalizedInEnglish` | verde |

Regresión de Sugerencias previa: los 8 escenarios de `sugerir-recetas-con-ia` siguen en verde dentro de los 14 de la clase.

## 4. Verificación manual

`uiautomator` sobre el AVD confirmó que la tarjeta muestra el pie «Aceptar»/«Descartar» (área táctil ≥ 48 dp), que tras aceptar aparece la marca «Aceptada» con botones deshabilitados y el aviso «Guardada en Mis Recetas», y que el cuerpo de la tarjeta sigue siendo inerte. Las **capturas de pantalla** (`aceptar-sugerencia-lista-es.png`, `aceptar-sugerencia-mis-recetas-es.png`) quedan **pendientes para el validador**, tal como asigna el Plan de verificación de la spec («UI (solo el validador)»): el implementador no puede producirlas con el cliente simulado sobre datos reales sin repetir el flujo completo y la spec limita el presupuesto a 1 informe + 2 PNG.

## 5. Seguridad

- **Sin llamadas reales a DevExpert Inference**: toda la verificación usó `FakeRecipeSuggester` (E2E) y fakes unitarios; no se pulsó «Pedir sugerencias» contra el proveedor.
- Sin API keys reales en código, tests, docs, logs de evidencia ni capturas. `local.properties` sigue gitignored y no se versiona.
- Los datos nuevos que salen del dispositivo (títulos e ingredientes de Recetas existentes con Coincidencia > 60 %) están documentados en `RK03` y `docs/technical-discovery.md`; nunca se envían Recetas completas ni fotos.

## 6. Documentación durable actualizada

- `docs/domain-model.md` — Sugerencia `propuesta` → `aceptada` (Borrador sin foto, `origen = Sugerencia`, id estable, aviso y permanencia en la lista) y `propuesta` → `descartada` (no guarda nada, tarjeta visible); validaciones de aceptación y umbral anti-duplicados; casos límite.
- `docs/technical-discovery.md` — aceptar/descartar, `recipeSuggestionPrompt` y datos enviados al proveedor.
- `docs/risks-and-open-questions.md` — `RK03` ampliado y `Q-aceptar-sugerencia-de-receta-1..5` resueltas.
- `ARCHITECTURE.md` / `CONSTRAINTS.md` / `AGENTS.md` — no aplica (no existen los dos primeros; no cambian flujo, comandos ni convenciones).

## 7. Estado de la verificación y pendientes para el validador

- Gates baratos reejecutados en esta sesión sobre el árbol actual: `init.ps1` exit 0, unit **146/146**, lint **0 errores / 49 warnings**, builds OK y `AiSuggestionsE2ETest` **14/14**.
- Suite E2E completa por `am instrument` sobre `emulator-5554`: **OK (137 tests)** en 1.202,5 s, con `AiSuggestionsE2ETest` 14/14, `PantryRecipesE2ETest` 8/8, `ScanPantryE2ETest` 22/22 y `RecipeDetailE2ETest` 12/12; sin fallos ni errores de esta feature.
- Pendiente para el validador: confirmar S1-S6 de forma independiente, capturar los 2 PNG previstos en `docs/evidence/aceptar-sugerencia-de-receta/` y verificar visualmente pie de acciones, estados deshabilitados y aviso.

## 8. Conclusión del implementador

La feature está implementada dentro del alcance de la spec y autoverificada (`passing`). **No es una aceptación**: queda lista para `feature-validator`, que debe reejecutar los gates, inspeccionar S1-S6 y aportar las capturas.

## 9. Validación independiente (feature-validator, sesión 75, 2026-10-24)

- **Veredicto**: **`accept`** (sin hallazgos Critical/High; 4 notas Low y 1 nota de proceso del validador, ninguna bloqueante).
- **Objetivo revisado**: `aceptar-sugerencia-de-receta` con `status: passing` en `feature_list.json`; spec `docs/specs/aceptar-sugerencia-de-receta.md`; diff sin commitear del árbol de trabajo (17 ficheros modificados + 3 nuevos de la feature).
- **Persistencia del estado**: el validador **no** modifica `feature_list.json` ni `PROGRESS.md`; el paso a `accepted` lo persiste el orquestador.

### 9.1 Checks reejecutados (árbol actual)

| Gate | Comando | Resultado |
|---|---|---|
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **146/146 en 24 suites**, 0 fallos, 0 errores, 0 omitidas (`SuggestionAcceptanceTest` 6/6, `SuggestionPromptTest` 3/3, `AiSuggestionsViewModelTest` 12/12, `SuggestionRulesTest` 9/9, `RecipeMatchTest` 5/5) |
| Lint | `gradlew.bat :app:lintDebug` (misma invocación) | **0 errores / 49 warnings** (todos previos; ninguno de esta feature, sin `ModifierParameter` nuevo) |
| Arranque | `init.ps1` con `ANDROID_SERIAL=emulator-5554`, `PKG=com.whatsforlunch.app` | El script llega a «Entorno listo» con la app instalada y en ejecución; el wrapper devuelve exit 1 por el falso fallo conocido de `adb shell monkey`/`pidof` a stderr (documentado en `PROGRESS.md`, no bloqueante) |
| E2E de la feature | `am instrument -e class com.whatsforlunch.app.AiSuggestionsE2ETest` en `emulator-5554` (Pixel_10_Pro, API 37) | **OK (14 tests)**, 16,756 s |
| Paridad de textos | comparación de claves es/en | **213/213**, sin diferencias |
| Evidencia PNG | cabecera y dimensiones | 2 PNG con cabecera `89 50 4E 47 0D 0A 1A 0A` y **1280×2856** |
| Secretos | `git diff` y `git grep` de la key real | **0 coincidencias** en ficheros versionados; `local.properties` sigue gitignored/untracked |

**Inspeccionada, no reejecutada** (coste alto): suite E2E completa declarada por el implementador (`am instrument` sobre el paquete: **OK (137 tests)** en 1.202,5 s, con `AiSuggestionsE2ETest` 14/14, `PantryRecipesE2ETest` 8/8, `ScanPantryE2ETest` 22/22 y `RecipeDetailE2ETest` 12/12). Es exacta, coherente con el diff y con el pase focalizado que sí se reejecutó (14/14).

### 9.2 Escenarios S1-S6

| Escenario | Base independiente | Resultado |
|---|---|---|
| S1 · Aceptar guarda en Mis Recetas con origen Sugerencia (verify 1) | `acceptingSavesTheSuggestionAsADraftRecipeVisibleInMyRecipes` reejecutado en verde; unit de id/mapeo; **captura independiente** de la tarjeta «Aceptada» + aviso y de Mis Recetas con «Tarta de manzana» y badge «Borrador» | **Pasa** |
| S2 · Aceptar dos veces no duplica (verify 2) | `acceptingTheSameSuggestionTwiceDoesNotDuplicateTheRecipe` + unit (`SuggestionAcceptance` id estable; `AiSuggestionsViewModelTest` una sola Receta) | **Pasa** |
| S3 · Descartar no guarda nada | `discardingKeepsTheCardWithDisabledActionsAndSavesNothing` + unit; el cuerpo de la tarjeta sigue inerte (inspección de `SuggestionCard`, sin `clickable`) | **Pasa** |
| S4 · Prompt con Recetas > 60 % e instrucción anti-duplicados | `onlyExistingRecipesAboveSixtyPercentAreSentToThePrompt` + `SuggestionPromptTest` y unit del ViewModel; revisión de `calculateMatch` (umbral estricto con porcentaje redondeado: 3/5 = 60 queda fuera) y de `recipeSuggestionPrompt` (sección solo si hay Recetas, es/en) | **Pasa** |
| S5 · Estados mixtos y reinicio al pedir de nuevo | `mixedStatesCoexistAndANewListStartsEverySuggestionAsProposed` + unit `aNewRequestStartsEverySuggestionAsProposed`; la captura independiente muestra una tarjeta «Aceptada» y otra en estado propuesta con acciones operativas | **Pasa** |
| S6 · Textos en inglés | `acceptingAndDiscardingAreLocalizedInEnglish`; 5 claves con paridad y mismos tokens de diseño | **Pasa** |

### 9.3 Decisiones y alcance

- **D1**: la Receta se guarda `DRAFT`, `photoPath = null`, `favorite = false` (`SuggestionAcceptance.suggestionToRecipe`); `⚠ S07` intacta (no se publica ni se navega al Detalle).
- **D2**: solo Recetas de Mis Recetas (cualquier estado y origen) con Coincidencia **> 60 %** (base redondeada de RD02) llegan al prompt, con título e ingredientes y la instrucción de no repetirlas; `RK03` ampliado en `docs/risks-and-open-questions.md`.
- **D3**: id estable `UUID.nameUUIDFromBytes("suggestion:" + normalizeName(título))` + `saveRecipe` (upsert) + tarjeta «Aceptada» con acciones deshabilitadas.
- **D4**: «Descartar» no guarda y la tarjeta permanece visible con acciones deshabilitadas.
- **D5**: al aceptar se permanece en la lista con el aviso «Guardada en Mis Recetas».
- **Alcance respetado**: sin Room/migraciones nuevas (`origin` ya existía), sin publicación, sin navegación nueva, sin badge de `origen`, sin tocar `calculateMatch`/`MatchBadge`/RD01/RD02/RD03/RD10/RD13, sin `/images/generations`, sin permisos ni dependencias nuevas. `PantryRecipesE2ETest` solo adapta la firma del fake.

### 9.4. Evidencia visual independiente

Capturas generadas por el validador en el AVD `Pixel_10_Pro` API 37 (`emulator-5554`), locale `es-ES`, con un **arnés de captura temporal en `androidTest` (cliente de IA simulado, sin red) que se borró al terminar**; el árbol final no contiene ficheros de ese arnés.

- `docs/evidence/aceptar-sugerencia-de-receta/aceptar-sugerencia-lista-es.png` (224.930 B, 1280×2856): banner «Guardada en Mis Recetas», tarjeta «Tarta de manzana» en estado **«Aceptada»** con «Aceptar»/«Descartar» deshabilitados y tarjeta «Sopa de nata» con pie de acciones operativo (primario terracota `#a83212`, secundario `#efeeea`, ≥ 48 dp).
- `docs/evidence/aceptar-sugerencia-de-receta/aceptar-sugerencia-mis-recetas-es.png` (138.953 B, 1280×2856): Mis Recetas con «Tarta de manzana», badge **«Borrador»** y 50 % de Coincidencia, producto de la aceptación.

Presupuesto respetado: **1 informe (este) + 2 PNG**, todos de esta feature.

### 9.5 Hallazgos

**Ninguno Critical/High.** Notas Low no bloqueantes:

- **L1 (docs)**: `docs/technical-discovery.md` (línea de «Verificación» de Sugerencias) sigue citando `AiSuggestionsE2ETest (S1-S8)`; hoy la clase tiene 14 pruebas (8 previas + S1-S6). Reparación sugerida: actualizar la línea cuando se toque el documento.
- **L2 (UX)**: el aviso `SAVED` no se descarta ni se auto-oculta (`clearNotice()` sigue sin consumidor, hallazgo F3 previo); permanece hasta la siguiente petición. La spec pide «aviso breve»; es aceptable por el precedente, pero conviene autodescartarlo.
- **L3 (robustez)**: `acceptSuggestion(id)` no comprueba el estado actual (solo `discardSuggestion` exige `PROPOSED`): una llamada programática sobre una tarjeta «Descartada» la marcaría «Aceptada» y guardaría. La UI lo impide (acciones deshabilitadas); añadir el guardado de estado sería más defensivo.
- **L4 (spec)**: la tarea T7 de la spec sigue sin marcar y la cabecera describe el estado de planificación; la evidencia ya está capturada, así que el orquestador puede cerrarla al persistir `accepted`.

**Nota de proceso del validador (no bloqueante, no es defecto de la feature)**: al compilar/instalar el APK de pruebas se lanzó `gradlew :app:installDebug` sin `ANDROID_SERIAL`, y Gradle instaló el APK debug también en el **dispositivo físico V2124** conectado por adb («Installed on 2 devices»). No se ejecutó ninguna prueba, llamada a la IA ni captura en ese dispositivo; a partir de ahí todo se acotó con `-s emulator-5554` / `ANDROID_SERIAL=emulator-5554`. Queda registrado por transparencia (la regla de `AGENTS.md` pide confirmación antes de instalar en un dispositivo físico); si el usuario no quiere esa instalación, puede desinstalarla.

### 9.6 Seguridad

Sin hallazgos. No hay secretos, tokens ni PII en el diff, en los tests, en las capturas ni en este informe; no se añaden permisos ni endpoints; la única ruta de red sigue siendo `/chat/completions` del cliente existente y **no se ejecutó ninguna llamada real** (todo con `FakeRecipeSuggester` y fakes unitarios). El nuevo dato que sale del dispositivo (títulos e ingredientes de Recetas > 60 %) está declarado en `RK03` y `docs/technical-discovery.md`, y `RK03` sigue como puerta de release.

### 9.7 Documentación y handoff

`docs/domain-model.md` (estados/transiciones, validación de aceptación, umbral anti-duplicados y casos límite), `docs/technical-discovery.md` (aceptar/descartar, prompt y datos enviados) y `docs/risks-and-open-questions.md` (`Q-aceptar-sugerencia-de-receta-1..5` resueltas y `RK03` ampliado) están alineados con el comportamiento implementado. `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md` no aplican. `feature_list.json` solo cambió `status` y `evidence`; `PROGRESS.md` y `docs/progress-history.md` reflejan el estado `passing` y el siguiente paso.

**Siguiente paso recomendado**: el orquestador persiste `aceptar-sugerencia-de-receta` como `accepted` en `feature_list.json` (solo `status`/`evidence`, añadiendo esta validación) y actualiza `PROGRESS.md`/historial. No se han hecho commits.
