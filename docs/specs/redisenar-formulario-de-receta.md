# Feature Implementation Spec: Rediseñar la vista Crear receta

## Source Feature

- `id`: redisenar-formulario-de-receta
- `area`: funcional
- `depends_on`: `unificar-unidades` (accepted), `crear-receta`, `editar-receta`, `ver-detalle-de-receta`, `mostrar-badge-de-coincidencia`, `anadir-faltantes-a-la-lista`, `sugerir-recetas-con-ia`, `aceptar-sugerencia-de-receta`, `resolver-dudas-con-pinche` (todas accepted)
- `status`: not_started (a la hora de planificar; 2026-11-09)
- `source`: `feature_list.json`

## Goal

El formulario de Receta queda como el usuario lo quiere: «Detalles de cocina» se rehace en dos bloques horizontales —una fila con el reloj de **Tiempo de preparación** (TimePicker de Material 3 en modo entrada, duración HH:MM tecleada) y el contador de **Raciones** (− valor +), y debajo el **Nivel** con sus chips en horizontal—, y **desaparece el tiempo de cocción**. Se elimina en profundidad `cookMinutes` (modelo, Room con migración que suma cocción a preparación, Detalle, tarjetas, Pinche/Sugerencias y parsers de IA) y el **concepto de ingrediente opcional** (checkbox, campo `optional` y Room con migración): todos los Ingredientes cuentan en la Coincidencia y en «Añadir faltantes». En Pasos, el texto se escribe en una caja con fondo blanco y el temporizador se fija con el mismo reloj, con un control «Sin temporizador».

## Non-Goals

- Unidades de Ingrediente/ítem ni sus desplegables (cerrados en `unificar-unidades`); solo se retira el checkbox «Opcional» de la fila.
- Modo Cocina salvo la pérdida de la distinción obligatorio/opcional: widget del temporizador (`gestionar-temporizador-de-paso`), voz (`controlar-cocina-con-voz`), escalado RD03 y vínculos RD21 intactos.
- Cambiar las reglas de guardado de `⚠ S07` (título, ≥1 Ingrediente y ≥1 Paso válidos, ninguna fila inválida; foto siempre opcional D11) ni el dock de guardado único.
- Conversiones entre unidades de tiempo, segundos, cronómetro ni relojes de duración superiores a 23:59 h.
- Reescribir la spec ni la evidencia histórica de las features aceptadas afectadas: se enmiendan con nota al final (`patrón completo` autorizado) y desde sus `notes` en `feature_list.json`.
- Traducciones, favoritos, Room salvo la migración descrita, y cualquier llamada real a la IA (todo con clientes simulados).

## Job Story

When quiero apuntar una receta y solo me importa cuánto tardo en prepararla,
I want to fijar el tiempo con un reloj claro y que todos mis ingredientes cuenten para la Despensa y la compra,
so I can guardarla rápido sin campos de cocción ni marcas de opcionales.

## Users And Permissions

- **Usuario** (único rol, uso personal 1-5 usuarios, sin cuentas `P7`/`P31`): crea y edita Recetas en el dispositivo; sin permisos Android ni dependencias nuevas.

## Acceptance Scenarios

### Scenario 1: Tiempo de preparación con reloj y sin cocción

Given el formulario de alta con «Detalles de cocina» en dos bloques
When el usuario abre el reloj de Tiempo de preparación en modo entrada, teclea 1 h 05 min, guarda y reabre la Receta en edición
Then el tiempo persiste (65 min, mostrados como 01:05 en el reloj) y en ningún punto del formulario aparece un campo de cocción (verify 1).

### Scenario 2: Sin casilla de opcional; todo cuenta

Given la fila de Ingrediente ya sin checkbox «Opcional»
When el usuario guarda una Receta con dos Ingredientes y ninguno está en la Despensa
Then la Coincidencia cuenta todos los Ingredientes y «Añadir faltantes» ofrece los dos (verify 2).

### Scenario 3: Pasos con caja blanca y temporizador por reloj

Given la sección de Pasos con el texto en caja de fondo blanco
When el usuario escribe el Paso y fija el temporizador con el mismo reloj (o pulsa «Sin temporizador»), guarda y reabre
Then el texto y el temporizador sobreviven al guardar y reabrir; los Pasos nuevos nacen «Sin temporizador» (verify 3).

### Scenario 4: Dominio y contrato de IA sin opcionalidad ni cocción (unit)

Given las reglas de dominio y el parser de IA
When se calcula la Coincidencia y se parsea una respuesta de la IA
Then `calculateMatch` cuenta todos los Ingredientes (sin filtro de opcionalidad) y los parsers ya no reciben `cookMinutes` ni opcionalidad (verify 4).

### Scenario 5: Migración de datos legados

Given una base v6 con una Receta con preparación 30 y cocción 45, un Ingrediente marcado opcional y un Paso con temporizador 0
When migra a v7
Then la Receta queda con preparación 75 (D6), el Ingrediente es obligatorio como todos y el Paso queda «Sin temporizador» (D9/D10), sin perder el resto de datos (test de migración).

### Scenario 6: Detalle y tarjetas sin cocción ni «Opcional»

Given una Receta guardada tras el rediseño
When se abre su Detalle, sus tarjetas (Mis Recetas, Pinche) y las Sugerencias
Then el tiempo se muestra con el formato actual («N min») sin desglose de cocción y no aparece ninguna píldora «Opcional» (complemento de verify 1-3; textos es/en con paridad).

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt` — `prepMinutes`/`cookMinutes` (L10-11).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeIngredient.kt` — `optional: Boolean` (L13).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStep.kt` — `timerMinutes: Int?` (L11).
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeEntity.kt`, `IngredientEntity.kt`, `StepEntity.kt`, `RecipeDatabase.kt` — esquema v6 (`cookMinutes` en `recipes`, `optional` en `ingredients`) y `MIGRATION_1_2..5_6`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` (L63-68) — registro de migraciones.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — mapeadores `toDomain`/`toEntity` (L54-134).
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — `MetricsGrid`/`TimeMetricBlock` (prep+cook, L556-632), `ServingsMetricBlock`, `DifficultyMetricBlock`, `IngredientRow` con checkbox `ingredientOptional(index)` (L875-882), `StepRow` con campo numérico `stepTimer(index)` (L929-1032), `CreateRecipeTestTags`, `SaveErrors`/`ActionDock`.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — `CreateRecipeUiState.prepMinutes/cookMinutes: String`, `IngredientFieldUiState.optional`, `StepFieldUiState.timerMinutes: String`, handlers `onPrepMinutesChange`/`onCookMinutesChange`/`onIngredientOptionalChange`/`onStepTimerChange`, `buildRecipe` (L310) y `preloadedState` (L357).
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeRoute.kt` — `toActions()` (L50).
- `app/src/main/java/com/whatsforlunch/app/domain/createrecipe/RecipeValidation.kt` — `RecipeRequirement`, `validateForSave`, `isValidMinutes`/`isValidTimerMinutes`.
- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` — `calculateMatch` con `filterNot { it.optional }` (L19) y `null` sin obligatorios.
- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingListRules.kt` — `missingRequiredIngredients` con `.filter { !it.optional }` (L35).
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` / `RecipeDetailViewModel.kt` — desglose `recipe_detail_metric_time_breakdown` (L506-511), checklist con `sortedBy { it.optional }` y píldora «Opcional» (L689, L777-794), `key()` con `optional` (L821).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `MetadataRow` con `prepMinutes + cookMinutes` (L924).
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` (L508-511, L601-614) y `PincheViewModel.kt` (L170) — metadatos, píldora «Opcional» y filtro de Faltantes.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsScreen.kt` (L560-573) — píldora «Opcional».
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeContract.kt` — claves `"cm"` y `"o"` en `RECIPE_CONTRACT`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParser.kt` (L30, L47), `domain/suggestion/RecipeSuggestion.kt`, `SuggestionRules.kt`, `SuggestionAcceptance.kt`, `domain/pinche/PincheProposalRules.kt` — `cookMinutes`/`optional` en el modelo crudo y normalización.
- `app/src/main/assets/prompts/` — `suggestion_*`/`pinche_system_*` insertan `{RECIPE_CONTRACT}`; no contienen literales `cm`/`o`.
- Tests afectados (unit/androidTest): `CreateRecipeViewModelTest`, `RecipeValidationTest`, `RecipeMatchTest`, `CompatibleRecipesTest`, `ShoppingListRulesTest`, `ShoppingListFlowTest`, `SuggestionRulesTest`, `SuggestionAcceptanceTest`, `RecipeSuggestionParserTest`, `PincheProposalRulesTest`, `PincheViewModelTest`, `AiSuggestionsViewModelTest`, `RecipeDetailViewModelTest`, `CookModeRulesTest` y `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `RecipeDetailE2ETest`, `AiSuggestionsE2ETest`, `MatchBadgeE2ETest`, `PantryRecipesE2ETest`, `ShoppingListE2ETest` (fixture `createV2Schema`), `RecipeMigrationTest` (patrón de migración a replicar).
- Docs: `docs/domain-model.md` (RD01/RD02/RD03/RD10/RD15/RD21, validaciones y casos límite), `DESIGN.md` (filas «Formulario de Receta» L164 y Detalle L162), `docs/build-brief.md:37,40`, `docs/technical-discovery.md` (Room y comandos), `CONTEXT.md` (glosario «Ingrediente», «Coincidencia», «Faltante»), `docs/risks-and-open-questions.md` (`⚠ S07`).

### Existing Patterns To Follow

- Reglas puras en `domain/<feature>/` con IDs `RD##`; la UI no contiene reglas de negocio (P19/P25).
- Migraciones Room como `MIGRATION_4_5` (recrear tabla con `CREATE TABLE ... SELECT`, drop/rename) registradas en `AppModule`; todo test que construya la BD debe registrar todas las migraciones (`RecipeMigrationTest` con schemas legados embebidos + round-trip con `RoomRecipeRepository`).
- Campos de formulario con `FieldShape`/`warmFieldColors()` (`ui/theme/Field.kt`) y testTags estables en `*TestTags`; strings en `values`/`values-en` con paridad exacta.
- El contrato de IA vive en `RecipeContract.kt` y los prompts lo insertan con `{RECIPE_CONTRACT}`: cambiar el contrato es un cambio de Kotlin, no de prompts.
- Enmiendas a features aceptadas: nota al final de su spec + `notes` de `feature_list.json` (patrón completo, autorizado en la sesión 118); no se reescribe su evidencia.

### Current Gaps

- No existe `TimePicker` ni `TimeInput` de Material 3 en el proyecto; las APIs de M3 para hora son experimentales (`@OptIn(ExperimentalMaterial3Api::class)`). El Compose BOM `2025.09.01` ya las incluye.
- La representación del estado del formulario es `String` de minutos; el reloj trabaja con horas/minutos y hay que reformar `prepMinutes` y el temporizador del Paso en el UI state.
- `RecipeMigrationTest` cubre hasta v5→v6; falta el patrón de test v6→v7.

## Technical Approach

1. **Modelo y Room v7 (D3/D5/D6)**: eliminar `Recipe.cookMinutes` y `RecipeIngredient.optional`; `RecipeDatabase` a `version = 7` con `MIGRATION_6_7` que recrea `recipes` (sin `cookMinutes`, con `prepMinutes = MIN(prepMinutes + cookMinutes, 1439)`, D6/D9) e `ingredients` (sin `optional`; los opcionales legados pasan a obligatorios), copiando el resto de columnas; `steps.timerMinutes` con `0 → NULL` (D10) en la misma pasada. Registrar `MIGRATION_6_7` en `AppModule` y en cada test que construya la BD. Mappers de `RoomRecipeRepository` al día.
2. **Dominio**: `RecipeMatch.calculateMatch` cuenta **todos** los Ingredientes (sin `filterNot`); con 0 Ingredientes devuelve `null` (sin badge, como hoy las recetas «solo opcionales», D12). `ShoppingListRules.missingRequiredIngredients` y el filtro de `PincheViewModel` dejan de excluir nada (Faltante = cualquier Ingrediente ausente, RD15 enmendada). `RecipeValidation.isValidTimerMinutes` pasa a `null || 1..1439`; `isValidMinutes` se retira si queda sin uso.
3. **Contrato de IA (D8)**: `RECIPE_CONTRACT` pierde `"cm"` y `"o"`; `RecipeSuggestionParser` deja de leerlos (claves desconocidas se ignoran, tolerancia actual), `RecipeSuggestion`/`RawRecipeSuggestion`/`RawRecipeIngredient` pierden esos campos y `SuggestionRules`/`SuggestionAcceptance`/`PincheProposalRules.proposalToRecipe` dejan de copiarlos. `SuggestionRules` deja de enviar tiempo de cocción en el prompt anti-duplicados si lo menciona. Prompts sin cambios (solo insertan el contrato).
4. **Formulario «Detalles de cocina» (D1/D2)**: `MetricsGrid` pasa a dos bloques —`Row`: reloj de Tiempo de preparación (control que muestra `HH:MM` y abre el diálogo M3 `TimePicker`/`TimeInput` **forzado a modo entrada**, `is24Hour = true`, Cancelar/Aceptar, tag `create_recipe_prep_minutes`) + `ServingsMetricBlock` (stepper − valor +, tags actuales); debajo, `DifficultyMetricBlock` con chips en horizontal (tags actuales). `COOK_MINUTES_FIELD` desaparece. UI state: `prepMinutes: Int` (minutos 0..1439; 00:00 = 0 min, D7) formateado `HH:MM` solo para presentación.
5. **Fila de Paso (D7)**: `StepRow` con el texto en caja de **fondo blanco** (se sustituye `warmFieldColors()` por contenedor blanco en esa caja, conservando `FieldShape`) y, debajo, el control del temporizador: botón que muestra `HH:MM` o «Sin temporizador» y abre el mismo reloj (tag `stepTimer(index)`), junto al control «Sin temporizador» (≥ 48 dp, tag `create_recipe_step_no_timer_$index`) que limpia a `null`. Los Pasos nuevos nacen sin temporizador; `StepFieldUiState.timerMinutes` pasa a `Int?` con el mismo formato `HH:MM` solo en presentación.
6. **Ripple visual**: el Detalle muestra el tiempo con `my_recipes_card_time` («N min») = `prepMinutes` y **sin** sublabel de desglose; `MyRecipesScreen.MetadataRow` y la metadatalínea del Pinche usan `prepMinutes` directo; desaparecen las píldoras «Opcional» del Detalle (`sortedBy { it.optional }` y `key()` sin `optional`), de las Sugerencias y del Pinche. `recipe_detail_metric_time_breakdown`, `create_recipe_cook_short_label`, `create_recipe_ingredient_optional`, `recipe_detail_ingredient_optional` y `pinche_proposal_optional` se retiran de ambos locales; claves nuevas: `create_recipe_prep_time_label` («Tiempo de preparación»/«Preparation time»), `create_recipe_step_no_timer` («Sin temporizador»/«No timer») y las de los botones del diálogo de reloj («Aceptar»/«Cancelar»); paridad exacta en es/en.
7. **Casos límite**: suma legada > 23:59 h → recortada a 1439 min en la migración (D9); temporizador legado 0 o vacío → «Sin temporizador» (D10); edición de Receta legada con tiempo > 23:59 → imposible tras el recorte de la migración; el reloj no acepta más de 23:59 ni menos de 00:00; borrar todos los Ingredientes de una Receta deja `calculateMatch` en `null` (sin badge) sin romper nada.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeIngredient.kt` — modify; sin `cookMinutes` ni `optional`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeEntity.kt`, `IngredientEntity.kt` — modify; columnas retiradas.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — modify; `version = 7` + `MIGRATION_6_7`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; registrar `MIGRATION_6_7`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — modify; mapeadores.
- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt`, `domain/shoppinglist/ShoppingListRules.kt`, `domain/createrecipe/RecipeValidation.kt` — modify; sin filtro de opcionalidad; validación del temporizador.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeContract.kt`, `RecipeSuggestionParser.kt` — modify; contrato sin `cm`/`o`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt`, `SuggestionRules.kt`, `SuggestionAcceptance.kt`, `domain/pinche/PincheProposalRules.kt` — modify; sin `cookMinutes`/`optional`.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt`, `CreateRecipeViewModel.kt`, `CreateRecipeRoute.kt` — modify; dos bloques, reloj de preparación, fila de Paso con caja blanca y temporizador por reloj, handlers al día.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt`, `RecipeDetailViewModel.kt`, `ui/myrecipes/MyRecipesScreen.kt`, `ui/pinche/PincheScreen.kt`, `ui/pinche/PincheViewModel.kt`, `ui/aisuggestions/AiSuggestionsScreen.kt` — modify; tiempo sin desglose y sin píldoras «Opcional».
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify; claves retiradas y nuevas; paridad exacta.
- Tests unit: los listados en Repository Research (fixtures sin `cookMinutes`/`optional`, asserts al día) + `domain/match/RecipeMatchTest` (todo cuenta; 0 ingredientes → null) + `RecipeSuggestionParserTest` (contrato sin `cm`/`o`).
- Tests instrumentados: `RecipeMigrationTest` (nuevo `migrationFromV6…` replicando el patrón: suma D6, columnas ausentes, `timerMinutes 0 → NULL`, round-trip), `CreateRecipeE2ETest` (S1/S3 con el reloj en modo entrada y «Sin temporizador»), `EditRecipeE2ETest` (round-trip del tiempo y del temporizador), `RecipeDetailE2ETest` (sin desglose ni «Opcional»), `AiSuggestionsE2ETest`/`MatchBadgeE2ETest`/`PantryRecipesE2ETest` (casos «solo opcionales» → «sin ingredientes»/todos cuentan), `ShoppingListE2ETest` (fixtures `createV2Schema` con columnas legadas + migración).

## Visual Design Impact

- UI involved: sí (formulario de Receta, Detalle, tarjetas). Design source: `DESIGN.md` (filas «Formulario de Receta» y Detalle) + `uxdocs/chefencasa_registrar_y_editar_receta/` como inspiración (`P16`).
- Screens or states affected: **Formulario de Receta** («Detalles de cocina» en dos bloques con reloj de Tiempo de preparación y sin Cocción; fila de Ingrediente sin checkbox; fila de Paso con caja blanca, reloj de temporizador y «Sin temporizador»), **Detalle** (métrica de tiempo sin desglose; checklist sin píldora «Opcional»), **Mis Recetas/Pinche/Sugerencias** (metadatos de tiempo sin cocción; sin píldoras «Opcional»).
- New design artifact required: no — se reutilizan `MetricBlock`, `StepperButton`, `SelectionChip`, `FieldShape` y el patrón de diálogo de la app; el `TimePicker`/`TimeInput` de Material 3 aporta su propio estilo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no cambian dominios, capas ni dependencias.
- `CONSTRAINTS.md`: not needed — sin reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — no cambia el flujo de trabajo ni los comandos.
- Other docs:
  - `docs/domain-model.md` — **update**: RD01/RD10 (la Coincidencia cuenta todos los Ingredientes; RD10 se reescribe o se retira), RD15 (Faltante = cualquier Ingrediente ausente), RD21 (sin mención de opcionalidad), tiempos (preparación como duración HH:MM sin cocción), validaciones y casos límite al día.
  - `DESIGN.md` — **update**: filas «Formulario de Receta» (reloj, dos bloques, sin opcionalidad, Paso con caja blanca y «Sin temporizador») y Detalle (sin desglose ni píldora).
  - `docs/build-brief.md` — **update** (líneas 37 y 40): quitar «obligatorios/opcionales» y «opcionalidad», y el tiempo de cocción del Detalle/Formulario.
  - `docs/technical-discovery.md` — **update**: Room v7 + `MIGRATION_6_7` (D6/D9/D10) y contrato de IA sin `cm`/`o` (D8).
  - `CONTEXT.md` — **update**: glosario «Ingrediente» (sin opcionalidad), «Coincidencia» (sobre todos los Ingredientes) y «Faltante» (ya no «obligatorio»).
  - `docs/risks-and-open-questions.md` — **update**: `⚠ S07` sin cambios de regla pero sin mención de opcionalidad si la tuviera; decisiones D1-D8 registradas como `Q-redisenar-formulario-de-receta-1..N` si procede.
  - Specs aceptadas afectadas: **nota de enmienda al final** (patrón completo) en `crear-receta`, `editar-receta`, `ver-detalle-de-receta`, `mostrar-badge-de-coincidencia`, `anadir-faltantes-a-la-lista`, `sugerir-recetas-con-ia`, `aceptar-sugerencia-de-receta`, `resolver-dudas-con-pinche` y `afinar-fidelidad-visual-del-formulario`; sus `notes` en `feature_list.json` quedan al día con la misma enmienda (patrón autorizado). No se reescribe su evidencia histórica.

## Implementation Plan

1. Modelo + Room v7 (`MIGRATION_6_7`, D6/D9/D10) con mapeadores y `RecipeMigrationTest` v6→v7.
2. Dominio: `calculateMatch`, Faltantes y `RecipeValidation` sin opcionalidad + tests unit.
3. Contrato de IA y normalización sin `cm`/`o` + tests de parser/reglas.
4. Formulario: «Detalles de cocina» en dos bloques con reloj de preparación, fila de Paso con caja blanca y temporizador por reloj; strings es/en; `CreateRecipeViewModelTest`/`CreateRecipeE2ETest`/`EditRecipeE2ETest` al día.
5. Ripple visual (Detalle, tarjetas, Sugerencias, Pinche) + tests E2E afectados (incluidos los casos «solo opcionales»).
6. Docs durables + notas de enmienda en specs aceptadas; gates completos y evidencia.

## Implementation Tasks

- [x] T1: `Recipe`/`RecipeIngredient`/`RecipeEntity`/`IngredientEntity` sin `cookMinutes`/`optional`; `RecipeDatabase` v7 + `MIGRATION_6_7` (suma D6 con tope 1439, `timerMinutes 0 → NULL`); `AppModule` y mapeadores; `RecipeMigrationTest.migrationFromV6…` con round-trip.
- [x] T2: `RecipeMatch.calculateMatch` (todos; 0 → `null`), `ShoppingListRules.missingRequiredIngredients` y `PincheViewModel` sin filtro; `RecipeValidation.isValidTimerMinutes` (null/1..1439); unit `RecipeMatchTest`/`CompatibleRecipesTest`/`ShoppingListRulesTest` al día.
- [x] T3: `RecipeContract` sin `cm`/`o`; parser, `RecipeSuggestion`/`RawRecipe*`, `SuggestionRules`/`SuggestionAcceptance`/`PincheProposalRules` al día; unit de parser y reglas.
- [x] T4: «Detalles de cocina» en dos bloques con reloj M3 modo entrada (00:00–23:59) y stepper de Raciones; fila de Paso con caja blanca, reloj de temporizador y «Sin temporizador»; UI state/ViewModel/Actions; strings es/en con paridad; `CreateRecipeViewModelTest`.
- [x] T5: ripple visual (Detalle sin desglose ni píldora, `MyRecipesScreen`, `PincheScreen`, `AiSuggestionsScreen`) y `key()`/orden del checklist sin `optional`.
- [x] T6: E2E — `CreateRecipeE2ETest` (verify 1 y 3), `EditRecipeE2ETest`, `RecipeDetailE2ETest`, `AiSuggestionsE2ETest`/`MatchBadgeE2ETest`/`PantryRecipesE2ETest` (verify 2), `ShoppingListE2ETest` (fixtures/migración); batería sin regresión.
- [x] T7: docs durables (domain-model, DESIGN, build-brief, technical-discovery, CONTEXT, risks) + notas de enmienda (patrón completo) en las 9 specs aceptadas y en `notes` de `feature_list.json`.

## Hallazgos de implementación

- Reloj compartido `ui/components/DurationPickerDialog.kt` (`AlertDialog` + `TimeInput` de Material 3 fijo en modo entrada, `is24Hour = true`; APIs experimentales con `@OptIn(ExperimentalMaterial3Api::class)`) y `whiteFieldColors()` en `ui/theme/Field.kt` para la caja blanca del Paso. `formatDurationMinutes` presenta la duración como `HH:MM`.
- El contrato de IA se define solo en `RecipeContract.kt`: los prompts no cambian (siguen insertando `{RECIPE_CONTRACT}`) y el parser ignora `cm`/`o` si llegan como claves desconocidas.
- En E2E, el tecleo del reloj se hace con `hasSetTextAction() and hasAnyAncestor(hasTestTag(DIALOG))` sobre los dos campos del modo entrada (horas, minutos) y «Aceptar».
- Cobertura del round-trip del temporizador repartida en dos tests (el harness del formulario no remonta la pantalla en un mismo test): `CreateRecipeE2ETest.prepTimeClockAndStepTimerSurviveSavingAndCookFieldIsGone` afirma el modelo guardado (prep 65 min; timers `5`/`null`) y `EditRecipeE2ETest.editingPreloadsRecipeFieldsAndSingleSaveDock` la precarga al reabrir (`00:10`, `00:05`, «Sin temporizador» y sin campo de cocción).

## Verification Plan

- `./init.ps1` — «Entorno listo» (checks no bloqueantes; sin servidores largos; el falso negativo de `monkey`/`pidof` se verifica aparte con `adb shell pidof`; con varios AVDs usar `ANDROID_SERIAL`).
- `./gradlew lintDebug` — 0 errores y sin warnings nuevos sobre la línea base (hoy 48).
- `./gradlew testDebugUnitTest` — todo en verde, con `RecipeMigrationTest` (incluido v6→v7), `RecipeMatchTest`, `CreateRecipeViewModelTest`, `RecipeSuggestionParserTest` y `SuggestionRulesTest` al día (verify 4 unit).
- `./gradlew assembleDebug` y `compileDebugAndroidTestKotlin` — OK.
- Paridad es/en de `strings.xml` — mismas claves en `values` y `values-en` (hoy 344 + 5 plurales; el recuento cambia con las claves retiradas/nuevas).
- E2E focalizada (`connectedDebugAndroidTest`, fixtures locales y clientes de IA simulados; **sin llamadas de pago**): `CreateRecipeE2ETest` con verify 1 (reloj en modo entrada teclea 1 h 05 → guarda y reabre → 01:05 persistido; ningún campo de cocción) y verify 3 (caja blanca del Paso, temporizador fijado con el reloj y «Sin temporizador» tras guardar/reabrir), `EditRecipeE2ETest` (round-trip), `RecipeDetailE2ETest` (sin desglose ni «Opcional»), `AiSuggestionsE2ETest`/`MatchBadgeE2ETest` (verify 2: todos los Ingredientes cuentan en la Coincidencia) + `ShoppingListE2ETest` (verify 2: «Añadir faltantes» ofrece todos) + regresión `PincheE2ETest`/`PantryRecipesE2ETest`.
- Unit de dominio: `calculateMatch` sin filtro de opcionalidad y `null` con 0 Ingredientes; parsers sin `cookMinutes` ni `optional` (verify 4).
- Batería E2E completa (`./e2e.ps1`) sin regresión sobre el inventario actual (los flakes conocidos de `SettingsE2ETest`/`PantryManagementE2ETest` se re-ejecutan en aislamiento y se documentan).
- Comprobación manual con `adb`/uiautomator: «Detalles de cocina» en dos bloques (fila Tiempo+Raciones, Nivel debajo), reloj en modo entrada, caja blanca del Paso y control «Sin temporizador»; nada de cocción ni casillas de opcional en el formulario.
- Migración: `RecipeMigrationTest` comprueba una base v6 real con datos legados (suma D6, opcionales → obligatorios, temporizador 0 → null) sin pérdida del resto.

## Evidence To Capture

- Salidas de `lintDebug`, `testDebugUnitTest`, `assembleDebug`, paridad es/en y resultado del test de migración en `docs/evidence/redisenar-formulario-de-receta/`.
- XML/resultado de la focalizada y de la batería, con recuento por clase.
- 2-3 capturas es-ES 1280×2856: «Detalles de cocina» en dos bloques con el reloj en modo entrada, fila de Paso con temporizador fijado y «Sin temporizador», y Detalle sin desglose ni píldora «Opcional».
- Resumen en `docs/evidence/redisenar-formulario-de-receta/informe.md` y `evidence` de `feature_list.json`.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (sin tocar unidades, Modo Cocina salvo la pérdida de opcionalidad, `⚠ S07`, escalado RD03 ni vínculos RD21; una sola migración Room; sin permisos ni dependencias nuevas).
- [ ] Acceptance scenarios pass (S1-S6; verify 1-4 de `feature_list.json`).
- [ ] Verification evidence is present (`docs/evidence/redisenar-formulario-de-receta/` con gates, migración, E2E y capturas).
- [ ] Persistent E2E coverage added/updated (`CreateRecipeE2ETest`/`EditRecipeE2ETest`/`RecipeDetailE2ETest`/`AiSuggestionsE2ETest`/`MatchBadgeE2ETest`/`ShoppingListE2ETest` + `RecipeMigrationTest`) o justificación si no procede.
- [ ] `feature_list.json` y `PROGRESS.md` updated correctly (solo `status`/`evidence` en la lista, más las `notes` de enmienda ya autorizadas).
- [ ] No unrelated product behavior or extra feature work was added (contrato de IA solo pierde `cm`/`o`; sin llamadas reales a la IA).
- [ ] Enmiendas aplicadas según patrón completo (nota al final de las 9 specs aceptadas afectadas; evidencia histórica intacta).

## Decisiones del usuario (2026-11-08/09)

- **D1 — Reloj = TimePicker de Material 3 en modo entrada** (teclado horas:minutos), usado igual en Tiempo de preparación y en el temporizador de Pasos.
- **D2 — «Detalles de cocina» en dos bloques**: fila Tiempo de preparación + Raciones (− valor +) y Nivel en horizontal debajo.
- **D3 — Tiempo de cocción eliminado en profundidad**: modelo, Room con migración, Detalle, tarjetas, IA.
- **D5 — Se elimina el concepto de ingrediente opcional completo** (el checkbox de la fila era «Opcional», no un check de preselección).
- **D6 — Al migrar, el tiempo de cocción se suma al de preparación** (`prepMinutes := preparación + cocción`) para conservar el tiempo total.
- **D7 — El reloj es duración HH:MM en 24 h** (00:00–23:59; 00:00 = 0 min en preparación) y cada Paso lleva un control «Sin temporizador» (los Pasos nuevos nacen sin temporizador).
- **D8 — El contrato de IA pierde «cm» y «o»**: los parsers ya no reciben `cookMinutes` ni opcionalidad.

Derivadas: **D9** Room `version = 7` con `MIGRATION_6_7` (receta legada con preparación + cocción > 23:59 h se recorta a 1439 min: el reloj no representa más); **D10** `timerMinutes` 0 o vacío = «Sin temporizador» (`null`; los 0 legados se normalizan en la migración); **D11** la duración se teclea HH:MM pero se sigue mostrando como «N min» (`my_recipes_card_time`) en Detalle, tarjetas y chips de temporizador, sin cambios de formato; **D12** `calculateMatch` con 0 Ingredientes sigue devolviendo `null` (sin badge), como hoy las recetas «solo opcionales».
