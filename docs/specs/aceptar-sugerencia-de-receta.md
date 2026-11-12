# Feature Implementation Spec: Aceptar sugerencia de receta

## Source Feature

- `id`: aceptar-sugerencia-de-receta
- `area`: funcional
- `depends_on`: sugerir-recetas-con-ia (`accepted`)
- `status`: accepted (implementada y validada de forma independiente el 2026-10-24)
- `source`: `feature_list.json`

## Goal

Desde la pantalla **Sugerencias** (ruta `ai_suggestions`), el usuario acepta una propuesta de la IA y la app la guarda como **Receta propia en Mis Recetas con `origen = Sugerencia` (RD07)** y estado **Borrador**, o la descarta sin guardar nada. Una Sugerencia **no se acepta dos veces**: la segunda aceptación no crea otra Receta. Como parte de esta feature, el prompt de Sugerencias se amplía para que la IA **no proponga recetas iguales ni equivalentes a las que ya tenemos**: se le envían las Recetas de Mis Recetas cuya Coincidencia con los Alimentos seleccionados supera el **60 %** (D2).

## Non-Goals

- **Publicar la Receta aceptada**: queda Borrador sin foto; publicar sigue exigiendo `⚠ S07` (título, foto, ≥1 Ingrediente, ≥1 Paso y ninguna fila inválida).
- **Navegar a la Receta creada**: no se abre el Detalle ni se cambia la navegación de `ai_suggestions` (el usuario se queda en la lista).
- **Persistir Sugerencias ni su estado** fuera del destino: siguen solo en memoria (D4 de `sugerir-recetas-con-ia`), sin Room, sin tablas ni migraciones.
- **Cambios en `calculateMatch`, `MatchBadge`, RD01/RD02/RD03/RD10, RD13, el esquema de Room ni la Despensa**.
- **Editar la Receta aceptada desde Sugerencias** (se edita con el formulario existente) ni mostrar un badge de `origen` en la UI (hoy el origen solo es un atributo; si el producto quiere mostrarlo, es otra feature).
- Pinche, Lista de la Compra, Modo Cocina, `/images/generations` y el Escáner.
- Verificación de «sin retención» (`RK03`) y llamadas reales a la IA.

## Job Story

When tengo delante las Sugerencias que ha devuelto la IA,
I want to quedarme con la que más me gusta y descartar el resto sin salir de la lista,
so I can guardarla como Receta propia en Mis Recetas sin duplicados ni pasos de más.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P7`, `P31`): acepta y descarta sus propias Sugerencias. Aceptar es una operación **local** contra Room (sin red); la única llamada de red sigue siendo la de Sugerencias, ya cubierta por `RK03`/`⚠ S05`. Sin permisos Android nuevos.

## Requisitos funcionales

- **RF-ASR1** (D3 de `sugerir-recetas-con-ia`): cada tarjeta de Sugerencia muestra al pie las acciones **«Aceptar»** y **«Descartar»**; tocar el cuerpo de la tarjeta sigue sin hacer nada.
- **RF-ASR2** (D1, RD07): «Aceptar» guarda la propuesta como Receta en Mis Recetas con `origin = RecipeOrigin.SUGGESTION`, estado **`RecipeStatus.DRAFT`** (Borrador), `photoPath = null`, `favorite = false`, e ingredientes y pasos copiados de la propuesta (con sus cantidades, unidades, opcionalidad y temporizadores). La Receta se crea con un **id estable derivado de la Sugerencia** (D3).
- **RF-ASR3** (D3): una Sugerencia no se acepta dos veces. Tras aceptar, la tarjeta queda en estado **«Aceptada»** con «Aceptar» y «Descartar» **deshabilitados**; una segunda aceptación (doble toque, llamada repetida o re-render) reescribe la misma Receta (**upsert por id estable**), nunca crea otra.
- **RF-ASR4** (D4): «Descartar» no guarda nada, marca la tarjeta **«Descartada»** con las acciones deshabilitadas y la tarjeta permanece visible en la lista.
- **RF-ASR5** (D5): al aceptar se muestra un aviso breve **«Guardada en Mis Recetas»** y el usuario se queda en la lista de Sugerencias (puede seguir aceptando y descartando).
- **RF-ASR6** (D2): la petición de sugerencias («Pedir sugerencias» y «Reintentar») incluye en el prompt las Recetas de Mis Recetas cuya Coincidencia con los Alimentos seleccionados supera el **60 %** (título e ingredientes) y la instrucción de no proponer recetas iguales ni equivalentes a ellas.
- **RF-ASR7** (D5/RF-ASR8): al llegar una lista nueva de propuestas (nueva petición o retry), todas empiezan en estado **propuesta**; los estados de la lista anterior no se arrastran.
- **RF-ASR8** (P19): todos los textos nuevos existen en `values/` (es) y `values-en/` (en) con paridad de claves.

## Acceptance Scenarios

### Scenario 1: Aceptar guarda la Sugerencia en Mis Recetas con origen Sugerencia (verify 1 de `feature_list.json`)

Given la pantalla Sugerencias con 2 propuestas listadas (cliente de IA simulado) y la Mis Recetas vacía
When el usuario pulsa «Aceptar» en la primera tarjeta
Then aparece el aviso «Guardada en Mis Recetas», la tarjeta queda marcada «Aceptada» con las acciones deshabilitadas, y Mis Recetas muestra una Receta nueva con el título de la propuesta, badge «Borrador», sus ingredientes y pasos, y `origen = Sugerencia` (RD07) persistido

### Scenario 2: Aceptar dos veces no duplica la Receta (verify 2 de `feature_list.json`)

Given una Sugerencia ya aceptada (tarjeta «Aceptada»)
When se vuelve a invocar su aceptación (doble toque sobre «Aceptar» o segunda aceptación forzada sobre la misma propuesta)
Then Mis Recetas contiene **una sola** Receta para esa Sugerencia: la segunda aceptación reescribe la misma fila (id estable + upsert) y el recuento de Recetas no aumenta

### Scenario 3: Descartar no guarda nada

Given una lista con propuestas en estado propuesta
When el usuario pulsa «Descartar» en una tarjeta
Then la tarjeta queda marcada «Descartada» con las acciones deshabilitadas y sigue visible, y Mis Recetas no cambia (cero Recetas nuevas)

### Scenario 4: El prompt incluye las Recetas existentes > 60 % para no repetirlas

Given Mis Recetas con «Tarta de manzana» (Coincidencia > 60 % con los Alimentos seleccionados) y otra Receta cuya Coincidencia con esos Alimentos es ≤ 60 %
When el usuario pulsa «Pedir sugerencias»
Then la llamada al cliente de IA incluye en el prompt «Tarta de manzana» con sus ingredientes y la instrucción de no proponer recetas iguales ni equivalentes, y **no** incluye la segunda Receta; con la Coincidencia exactamente 60 % tampoco se incluye

### Scenario 5: Aceptar y descartar conviven en la misma lista

Given una lista con 3 propuestas
When el usuario acepta la primera y descarta la segunda
Then la primera está «Aceptada», la segunda «Descartada» y la tercera sigue en estado propuesta con sus acciones operativas; al pedir sugerencias de nuevo, la lista nueva empieza todas en propuesta

### Scenario 6: Textos en inglés

Given el sistema en inglés (`locale` `en-US`)
When se muestra una tarjeta con acciones y se acepta o descarta
Then «Accept» / «Discard» / los estados y el aviso aparecen en inglés, con las mismas dimensiones y comportamiento que en español

## Casos límite

| Caso | Comportamiento |
|---|---|
| Sugerencia sin foto | La Receta se guarda con `photoPath = null` como Borrador; publicar exige foto (`⚠ S07`) |
| Doble toque sobre «Aceptar» | Una sola Receta (id estable + upsert), sin duplicado (S2) |
| Dos propuestas con el mismo título normalizado | No coexisten: `SuggestionRules.normalize` ya las fusiona conservando la primera (D8 de `sugerir-recetas-con-ia`) |
| Nueva petición que devuelve un título ya aceptado | Mismo id estable → upsert, no duplica; además el prompt lo desaconseja (D2) |
| Receta existente con Coincidencia 0 % o sin obligatorios (RD01) | No entra en la lista anti-duplicados del prompt |
| Receta existente con Coincidencia exactamente 60 % | No se envía al prompt (umbral estrictamente `> 60`) |
| Sugerencia aceptada y luego editada por el usuario | Una segunda aceptación de la misma propuesta la reescribe (upsert); en la práctica lo impide el estado «Aceptada» con acciones deshabilitadas |
| Rotación de pantalla | Estados propuesta/aceptada/descartada viven en el ViewModel: se conservan |
| Salir con atrás | Las Sugerencias y sus estados se pierden (solo memoria); las Recetas aceptadas permanecen en Mis Recetas |

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `aceptar-sugerencia-de-receta` (steps, verification, notes) y estado de `sugerir-recetas-con-ia` (`accepted`).
- `docs/domain-model.md` — entidad **Sugerencia** (estados `propuesta/aceptada/descartada`, «se materializa como máximo en una Receta»), **RD07**, «ninguna Sugerencia se acepta dos veces», validaciones D8.
- `docs/specs/sugerir-recetas-con-ia.md` — contrato vigente de la pantalla Sugerencias (D1-D10), que esta feature **no rompe** salvo el prompt (D2 aquí).
- `docs/risks-and-open-questions.md` — `⚠ S07` (publicar), `RK03` (datos enviados al proveedor), `Q-sugerir-recetas-con-ia-*` resueltas.
- `docs/build-brief.md` — línea roja de producto: «al aceptar una [Sugerencia], se guarda en Mis Recetas».
- `CONTEXT.md` — términos Sugerencia / Receta / Borrador / Mis Recetas.
- `DESIGN.md` — componentes (botón primario terracota, botón secundario, ≥48 dp) y estados deshabilitados.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt` — `RecipeSuggestion` (sin `id`), `SuggestionWithMatch`, `SuggestableFood`, `RawRecipeSuggestion`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` — `MAX_SUGGESTIONS`, `normalize()`, `suggestionMatches()` (adapta Alimentos → `PantryItem` para `calculateMatch`).
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeOrigin.kt`, `RecipeStatus.kt`, `RecipeIngredient.kt`, `RecipeStep.kt` — modelo destino; `RecipeOrigin { USER, SUGGESTION }` ya existe (RD07).
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — `saveRecipe(recipe)` (create-or-update), `observeRecipes()`, `getRecipe(id)`, `deleteRecipe(id)`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeEntity.kt`, `RecipeDao.kt`, `RecipeDatabase.kt` — la columna `origin` ya existe (texto); `insertRecipe` es `REPLACE` y `upsertRecipe` es transaccional por id; **no hace falta migración** (DB v2).
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — mappers `origin = origin.name` / `enumOrDefault(…, USER)`; ambos reemplazan por id.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — patrón `buildRecipe` + ids de hijos `"$id-ingredient-$index"` / `"$id-step-$index"`; conserva `origin` al editar.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt` — fases `Selection/Loading/Results/Empty`, avisos `AI_KEY_MISSING/FAILED`, `toggleFood/requestSuggestions/retry`; **sin `RecipeRepository`** y sin estados por propuesta.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsScreen.kt` — `AiSuggestionsTestTags`, `SuggestionCard` inerte (D5), sin acciones ni textos de aceptar/descartar.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt`, `OpenAiCompatibleRecipeSuggester.kt` — `suggest(foods, language): RecipeSuggestionsResult` (`MissingApiKey`/`Failed`/`Success`); prompt `SPANISH_PROMPT`/`ENGLISH_PROMPT`; no conoce Recetas existentes.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `viewModel { AiSuggestionsViewModel(get(), get(), get()) }` (PantryRepository, ScanDraftHolder, RecipeSuggester).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — 14 claves `ai_suggestions_*` con paridad; **ninguna** de aceptar/descartar/estados.
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt`, `.../domain/suggestion/SuggestionRulesTest.kt`, `.../data/ai/RecipeSuggestionParserTest.kt` — unit actuales con `FakeRecipeSuggester` propio por test.
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` (S1-S8), `PantryRecipesE2ETest.kt`, `EditRecipeE2ETest.kt` (ya aserta `origin` persistido), `E2eDatabase.kt` — patrón E2E del repo (`createComposeRule` + fakes).

### Existing Patterns To Follow

- **`RecipeOrigin.USER|SUGGESTION` ya existe de extremo a extremo** (dominio, columna `origin` en `RecipeEntity`, mappers, preservación al editar): no hay que tocar el esquema de Room ni migraciones.
- **`RecipeRepository.saveRecipe` es upsert por id** (`RecipeDao.insertRecipe` REPLACE; `InMemoryRecipeRepository` reemplaza por id): es la base del id estable para «no aceptar dos veces».
- Ids de hijos de receta `"$recipeId-ingredient-$index"` / `"$recipeId-step-$index"` (`CreateRecipeViewModel`).
- Avisos: patrón enum + banner descartable (`AiSuggestionsNotice`, `ScannerNotice`).
- E2E: `createComposeRule()` montando `AiSuggestionsScreen`/ViewModel con `FakeRecipeSuggester`, `InMemoryRecipeRepository`, `InMemoryPantryRepository` y aserciones por `testTag`; `EditRecipeE2ETest` asienta el patrón de verificar `RecipeOrigin.SUGGESTION` en el repositorio.
- Coincidencia reutilizando `calculateMatch` vía la adaptación Alimentos → `PantryItem` de `SuggestionRules.suggestionMatches` (RD01/RD10 sin tocar `RecipeMatch.kt`).

### Current Gaps

- `SuggestionCard` es inerte: sin acciones, sin estados `aceptada/descartada`, sin textos ni testTags asociados.
- `AiSuggestionsViewModel` no recibe `RecipeRepository` (tampoco `AppModule` se lo inyecta).
- `RecipeSuggestion` no tiene identificador: hay que derivar uno estable del título normalizado (`domain/text/NameNormalization`).
- El puerto `RecipeSuggester.suggest(foods, language)` no recibe las Recetas existentes ni el prompt sabe de ellas (RF-ASR6).

## Technical Approach

### Decisiones (para no reabrir el debate en implementación; confirmadas por el usuario el 2026-10-24)

- **D1 — La Receta aceptada se guarda como Borrador** sin foto. `⚠ S07` queda intacta: publicar exige título y foto, ≥1 Ingrediente y ≥1 Paso; el usuario publica después desde el formulario (Q-aceptar-sugerencia-de-receta-1).
- **D2 — Anti-duplicados en el prompt**: se envían al proveedor las Recetas de Mis Recetas (Borradores y Publicadas, cualquier `origen`) cuya Coincidencia con los Alimentos seleccionados es **estrictamente mayor que 60 %** (porcentaje redondeado, misma base que RD02), con su título e ingredientes, pidiendo no proponer recetas iguales ni equivalentes. Es **delta sobre la aceptada `sugerir-recetas-con-ia`** y entra en esta feature (Q-aceptar-sugerencia-de-receta-5).
- **D3 — «No aceptar dos veces» = estado de tarjeta + id estable**: el id de la Receta se deriva de forma determinista de la Sugerencia (`UUID.nameUUIDFromBytes` sobre `"suggestion:" + normalizeName(title)`), de modo que una aceptación repetida hace upsert de la misma fila; la tarjeta pasa a «Aceptada» con acciones deshabilitadas (Q-aceptar-sugerencia-de-receta-2).
- **D4 — Descartar marca la tarjeta «Descartada»** sin retirarla de la lista ni guardar nada (Q-aceptar-sugerencia-de-receta-3).
- **D5 — Aceptar se queda en la lista** con la tarjeta marcada y el aviso breve «Guardada en Mis Recetas» (Q-aceptar-sugerencia-de-receta-4).

### Dominio

- `domain/suggestion/SuggestionAcceptance.kt` (nuevo, `internal`/`object` estilo `SuggestionRules`):
  - `enum class SuggestionState { PROPOSED, ACCEPTED, DISCARDED }`.
  - `fun suggestionRecipeId(title: String): String` — id estable (D3) sobre el título normalizado.
  - `fun suggestionToRecipe(suggestion: RecipeSuggestion): Recipe` — `Recipe(id = suggestionRecipeId(title), …, photoPath = null, favorite = false, status = RecipeStatus.DRAFT, origin = RecipeOrigin.SUGGESTION, ingredients/steps con ids "$id-ingredient-$index" / "$id-step-$index")`.
  - `fun recipesToAvoid(foods: List<SuggestableFood>, recipes: List<Recipe>, minPercent: Int = 60): List<Recipe>` — Coincidencia con `calculateMatch` contra los Alimentos seleccionados (adaptación de `SuggestionRules`), solo con porcentaje **> 60** (RF-ASR6, D2).
- Sin cambios en `RecipeMatch.kt`, `MatchBadge`, `SuggestionRules.normalize` ni el esquema de Room.

### Prompt y puerto

- `data/ai/RecipeSuggester.kt`: `suggest(foods: List<SuggestableFood>, language: String, existingRecipes: List<RecipeAvoidance> = emptyList())` con `data class RecipeAvoidance(val title: String, val ingredientNames: List<String>)`. Firma con parámetro nuevo para no romper llamadas.
- `data/ai/OpenAiCompatibleRecipeSuggester.kt`: los prompts `SPANISH_PROMPT`/`ENGLISH_PROMPT` ganan una sección (solo si `existingRecipes` no está vacía) «No propongas recetas iguales ni equivalentes a estas que ya tengo: *título (ingredientes)*» en el idioma de la app. El JSON compacto de D1 (`t/st/mm/pm/cm/df/bs/kcal/i/p`) y el resto del contrato no cambian.

### ViewModel y UI

- `AiSuggestionsViewModel(pantryRepository, scanDraftHolder, recipeSuggester, recipeRepository)` (4.º parámetro; `AppModule` lo inyecta). `Results` lleva además `states: Map<String, SuggestionState>` (clave = `suggestionRecipeId`). Nuevas acciones `acceptSuggestion(id)` (construye la Receta y `recipeRepository.saveRecipe(...)`, estado → `ACCEPTED`, aviso `SAVED`) y `discardSuggestion(id)` (estado → `DISCARDED`, sin guardar). `requestSuggestions`/`retry` pasan `recipesToAvoid(...)` al sugerer y reinician los estados de la lista nueva (RF-ASR7).
- `AiSuggestionsScreen`/`AiSuggestionsRoute`: `SuggestionCard` gana pie de acciones con **«Aceptar»** (primario) y **«Descartar»** (secundario); en estado aceptada/descartada los botones se deshabilitan y aparece la marca de estado. Callbacks `onAccept(id)`/`onDiscard(id)`. El aviso post-aceptación reutiliza el patrón de banner con un valor nuevo (`SAVED`) y texto `ai_suggestions_accepted_notice`.
- `testTags` nuevos en `AiSuggestionsTestTags`: `suggestionAccept(index)`, `suggestionDiscard(index)`, `suggestionState(index)`.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptance.kt` — crear; estados, id estable, mapeo a Receta y filtro anti-duplicados (D1-D3).
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt` — modificar; nuevo parámetro `existingRecipes` y DTO `RecipeAvoidance`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleRecipeSuggester.kt` — modificar; sección anti-duplicados del prompt es/en (D2).
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt` — modificar; `RecipeRepository`, estados por propuesta, `acceptSuggestion`/`discardSuggestion`, aviso `SAVED`.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsScreen.kt` — modificar; pie de acciones, estados y testTags.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsRoute.kt` — modificar; cablear `onAccept`/`onDiscard`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; 4.º parámetro del `viewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; claves nuevas con paridad.
- `app/src/test/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptanceTest.kt` — crear; id estable, mapeo (origen, DRAFT, ids de hijos, sin foto) y umbral > 60 %.
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt` — modificar; aceptar guarda con `origen = SUGGESTION` y Borrador, aceptar dos veces → 1 Receta, descartar no guarda, estados y aviso.
- `app/src/test/java/com/whatsforlunch/app/data/ai/` — modificar/crear; test del prompt anti-duplicados (función pura de construcción de prompt o justificación si no es viable sin `MockWebServer`).
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — modificar; S1-S6 de esta spec (verify 1 y 2 de `feature_list.json`).
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`, `docs/progress-history.md` — modificar al implementar (ver Durable Documentation Impact).
- `docs/validations/aceptar-sugerencia-de-receta.md` y `docs/evidence/aceptar-sugerencia-de-receta/*.png` — crear al validar (evidencia).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`). No hay prototipo del pie de acciones de Sugerencias (laguna `RK04` resuelta con los componentes existentes, para que el implementer no invente estilo).
- Screens or states affected: **Sugerencias** (tarjeta con pie de acciones y estados «Aceptada»/«Descartada»); Mis Recetas solo recibe la Receta nueva con su badge «Borrador» existente (sin cambios visuales).
- New design artifact required: no — se usan tokens y patrones de tarjeta/botón existentes.

Especificación verificable por `uiautomator` y captura (sin exigir muestreo de píxeles):

- **Pie de la tarjeta**: «Aceptar» botón primario (relleno terracota `#a83212`, texto blanco, radio `0.75 rem`, área táctil ≥ 48 dp) y «Descartar» botón secundario (superficie `#efeeea`, texto `#1b1c1a`, área táctil ≥ 48 dp).
- **Estado aceptada/descartada**: marca de texto `labelSmall` («Aceptada»/«Descartada») junto al título y botones **deshabilitados** distinguibles (patrón del estado deshabilitado del formulario); el cuerpo de la tarjeta sigue sin acción al tocarlo.
- **Aviso post-aceptación**: banner breve «Guardada en Mis Recetas» fuera del contenido principal (patrón de avisos del Escáner/Sugerencias).
- Contenido de la tarjeta sin cambios (título, `MatchBadge`, «X de Y ingredientes disponibles», ingredientes y pasos).

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (dominio en `domain/suggestion/`, puerto en `data/ai/`, UI en `ui/aisuggestions/`, dentro de la convención).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs:
  - `docs/domain-model.md` — **update**: la Sugerencia pasa a `aceptada` creando una Receta **Borrador** con `origen = Sugerencia` e id estable (RD07), `descartada` no guarda nada; matizar «una Sugerencia no se acepta dos veces» (id estable + upsert, tarjeta en estado aceptada).
  - `docs/technical-discovery.md` — **update**: el prompt de Sugerencias incluye las Recetas existentes > 60 % de Coincidencia (qué datos salen del dispositivo).
  - `docs/risks-and-open-questions.md` — **update**: registrar `Q-aceptar-sugerencia-de-receta-1..5` **resueltas por el usuario el 2026-10-24** (estado Borrador, mecanismo anti-duplicados, descarte, quedarse en la lista con aviso, y anti-duplicados en el prompt + alcance) y ampliar `RK03`: además de los Alimentos seleccionados, salen **títulos e ingredientes de Recetas existentes** al proveedor.
  - `PROGRESS.md` / `docs/progress-history.md` — **update** al cerrar la sesión (estado y siguiente paso; entrada de historial).

## Implementation Plan

1. Crear `SuggestionAcceptance.kt` con sus unit tests (id estable, mapeo a Borrador con `origen = SUGGESTION`, filtro > 60 %).
2. Ampliar el puerto `RecipeSuggester` y el prompt de `OpenAiCompatibleRecipeSuggester` (D2) con su test.
3. Ampliar `AiSuggestionsViewModel` (4.º parámetro, estados, `acceptSuggestion`/`discardSuggestion`, aviso `SAVED`) y sus unit tests.
4. Implementar el pie de acciones y los estados en `AiSuggestionsScreen`/`Route`, textos es/en y testTags.
5. Extender `AiSuggestionsE2ETest` con S1-S6 (verify 1 y 2 de `feature_list.json`) y dejar la regresión completa en verde.
6. Actualizar `docs/domain-model.md`, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` (`Q-aceptar-sugerencia-de-receta-1..5`, `RK03`).
7. Capturar la evidencia y registrar el cierre en `PROGRESS.md`/`docs/progress-history.md` (solo `status` y `evidence` en `feature_list.json`).

## Implementation Tasks

- [x] T1: `domain/suggestion/SuggestionAcceptance.kt` + `SuggestionAcceptanceTest.kt` (D1, D3: id estable, mapeo Borrador/`SUGGESTION`, `recipesToAvoid` > 60 %). (RF-ASR2, RF-ASR6, S1, S2, S4)
- [x] T2: `RecipeSuggester.kt` + `OpenAiCompatibleRecipeSuggester.kt` + test del prompt (D2, sección anti-duplicados es/en, JSON compacto intacto). (RF-ASR6, S4)
- [x] T3: `AiSuggestionsViewModel` + `AiSuggestionsViewModelTest` (estados, aceptar guarda, aceptar dos veces → 1 Receta, descartar no guarda, aviso, reinicio de estados). (RF-ASR2, RF-ASR3, RF-ASR4, RF-ASR5, RF-ASR7, S2, S3, S5)
- [x] T4: `AiSuggestionsScreen`/`AiSuggestionsRoute` + textos es/en + `suggestionAccept/suggestionDiscard/suggestionState`. (RF-ASR1, RF-ASR8, S5, S6)
- [x] T5: `AppModule` (4.º parámetro) y suite de `AiSuggestionsE2ETest` con S1-S6 sin romper la regresión.
- [x] T6: gates en verde y docs actualizadas (dominio, technical-discovery, `Q-aceptar-sugerencia-de-receta-1..5`, `RK03`).
- [x] T7: evidencia capturada y `PROGRESS.md`/historial al día.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate de arranque estándar no bloqueante; no arranca procesos duraderos; artefactos conocidos: carrera del `pidof` y exit 1 falso del `adb shell monkey`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `SuggestionAcceptanceTest`, `AiSuggestionsViewModelTest`, `SuggestionRulesTest` y `RecipeMatchTest` (regresión). Reejecución forzada: `gradlew.bat testDebugUnitTest --rerun-tasks`.
- E2E persistente: `./e2e.ps1` / `./e2e.sh` o `gradlew.bat connectedDebugAndroidTest` (AVD `Pixel_10_Pro` API 37; nota: `e2e.ps1` desinstala la app — alternativa usada en otras features) con `AiSuggestionsE2ETest`:
  - **verify 1 (S1)**: `FakeRecipeSuggester` con 2 propuestas → «Aceptar» → aviso «Guardada en Mis Recetas», tarjeta «Aceptada» y Receta visible en Mis Recetas (montada sobre el mismo `InMemoryRecipeRepository`) con badge «Borrador» y `origin = SUGGESTION`.
  - **verify 2 (S2)**: aceptar la misma Sugerencia dos veces → una sola Receta en el repositorio (upsert por id).
  - S3: «Descartar» → tarjeta «Descartada», acciones deshabilitadas, cero Recetas nuevas.
  - S4 (unit + E2E): el prompt enviado incluye solo las Recetas con Coincidencia > 60 % con los Alimentos seleccionados y la instrucción anti-duplicados.
  - S5: estados mixtos en una misma lista y reinicio de estados al pedir de nuevo.
  - S6: textos en inglés (patrón de localización del repo).
  - Regresión completa en verde (suite `connectedDebugAndroidTest`, hoy 131 pruebas con 2 omitidas de `SettingsE2ETest`) sin romper Sugerencias previa (S1-S8), Recetas Compatibles, Escáner, Detalle ni Ajustes.
- Cobertura E2E: **sí se añade** — la feature cambia comportamiento visible y crea datos persistentes (Receta en Mis Recetas).
- `init.sh` / `init.ps1` **no se modifican**: checks no bloqueantes, sin procesos duraderos; los scripts del harness no se tocan.
- **Sin llamadas reales a DevExpert Inference**: toda la verificación usa el cliente simulado; una llamada real es de pago y requiere confirmación humana previa (`AGENTS.md`).
- UI (solo el validador): capturas en español de la tarjeta con acciones/estado y de Mis Recetas con la Receta aceptada + `uiautomator`; sin screenshot testing en el repo (justificado por el precedente de `afinar-fidelidad-visual-del-formulario`).

## Evidence To Capture

- `docs/validations/aceptar-sugerencia-de-receta.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación por escenario S1-S6.
- `docs/evidence/aceptar-sugerencia-de-receta/aceptar-sugerencia-lista-es.png` — tarjetas con pie «Aceptar»/«Descartar» y una en estado «Aceptada» (es).
- `docs/evidence/aceptar-sugerencia-de-receta/aceptar-sugerencia-mis-recetas-es.png` — Mis Recetas con la Receta aceptada como «Borrador» (es).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas PNG binarias válidas, 1280×2856).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (guardar la Sugerencia aceptada como Receta con origen Sugerencia según RD07) | RF-ASR2, RF-ASR5 | S1 | T1, T3, T4 | unit `SuggestionAcceptanceTest` + E2E |
| Step 2 (descartar la Sugerencia sin guardar nada) | RF-ASR4 | S3, S5 | T3, T4 | unit + E2E |
| Verify 1 (E2E: aceptar → aparece en Mis Recetas con origen Sugerencia) | RF-ASR2 | S1 | T5 | `AiSuggestionsE2ETest` |
| Verify 2 (E2E: aceptar dos veces → no se duplica) | RF-ASR3 | S2 | T1, T3, T5 | unit + `AiSuggestionsE2ETest` |
| — (anti-duplicados en el prompt, D2) | RF-ASR6 | S4 | T2 | unit del prompt + E2E |
| — (Borrador sin foto, `⚠ S07` intacta) | RF-ASR2 (D1) | S1 | T1 | unit + E2E |
| — (es/en) | RF-ASR8 | S6 | T4 | E2E de localización |

## Validator Checklist

- [x] La implementación se queda dentro del alcance de esta feature.
- [x] Los escenarios de aceptación pasan (S1-S6) y las decisiones D1-D5 se respetan (en particular: la Receta queda **Borrador** con `origen = SUGGESTION` y **sin** publicarse; el prompt incluye solo Recetas con Coincidencia **> 60 %**).
- [x] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [x] Cobertura E2E persistente añadida/actualizada (`AiSuggestionsE2ETest` S1-S6) y suite completa en verde, sin romper los escenarios previos de Sugerencias ni el resto de pantallas.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente (solo `status` y `evidence` en `feature_list.json`).
- [x] No se añadió comportamiento de producto ni trabajo de otras features (en particular: **sin publicar** la Receta aceptada, sin navegación al Detalle, sin persistencia de Sugerencias ni migraciones de Room, sin badge de `origen` en la UI, sin Pinche/Lista de la Compra/Modo Cocina y sin tocar `calculateMatch`, `MatchBadge` ni RD13).
- [x] Sin llamadas reales de pago a la IA en la verificación (solo cliente simulado) y sin API keys reales en ficheros, logs ni evidencia (`⚠ S05`, `RK01`).
- [x] `docs/risks-and-open-questions.md` registra `Q-aceptar-sugerencia-de-receta-1..5` resueltas y `RK03` ampliado con los datos nuevos que salen del dispositivo (títulos e ingredientes de Recetas existentes).

---

## Enmienda — `unificar-unidades` (2026-11-09)

Decisión posterior del usuario (D1–D5 de `docs/specs/unificar-unidades.md`): la unidad deja de ser texto libre y pasa a **lista cerrada (ud, g, kg, ml, l)**. Al aceptar la Sugerencia, la Receta se guarda con las unidades ya **normalizadas al enum** (D2: la vacía o desconocida → «ud»), garantizado en `SuggestionRules.normalizeOne`; las tarjetas presentan la unidad con `unitLabel` (código → clave `unit_ud`…`unit_l` en es/en; legada → texto tal cual). El resto del flujo de aceptación (id estable, origen, obligatorios D11) queda intacto.

*Enmienda añadida al final por `unificar-unidades`, sin reescribir la historia ni las evidencias históricas de esta spec.*

---

## Enmienda — `redisenar-formulario-de-receta` (2026-11-09)

Decisiones posteriores del usuario (D3/D5/D8 de `docs/specs/redisenar-formulario-de-receta.md`): la Receta aceptada se materializa **sin `cookMinutes` ni opcionalidad** —todos los Ingredientes guardados cuentan y el tiempo es solo el de preparación (la cocción ya no llega de la IA, contrato sin «cm»/«o»)—; su unidad normalizada al enum se mantiene (enmienda de `unificar-unidades`). El resto de esta spec (id estable, upsert sin duplicados, descarte, prompt anti-duplicados) queda intacto.

*Enmienda añadida al final por `redisenar-formulario-de-receta`, sin reescribir la historia ni las evidencias históricas de esta spec.*
