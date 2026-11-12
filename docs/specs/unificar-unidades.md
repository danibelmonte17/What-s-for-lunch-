# Feature Implementation Spec: Unificar las unidades de Ingredientes e ítems con un enum

## Source Feature

- `id`: unificar-unidades
- `area`: funcional
- `depends_on`: crear-receta, editar-receta, ver-detalle-de-receta, cocinar-paso-a-paso, anadir-faltantes-a-la-lista, anadir-item-a-la-lista, sugerir-recetas-con-ia, aceptar-sugerencia-de-receta, resolver-dudas-con-pinche (todas `accepted`)
- `status`: not_started (al planificar, 2026-11-09)
- `source`: `feature_list.json` (entrada creada al partir `redisenar-formulario-de-receta`, 2026-11-09)

## Goal

La unidad deja de ser texto libre y pasa a ser una lista cerrada **(ud, g, kg, ml, l)** compartida por toda la app: se elige con un desplegable en la fila de Ingrediente del formulario y en el diálogo de ítem de la Lista de la Compra («ud» por defecto en filas nuevas), se muestra con los mismos textos es/en en el Detalle, las tarjetas, el Modo Cocina, las Sugerencias y el Pinche, y la unidad que llega de la IA se normaliza al enum. Las unidades ya guardadas fuera de la lista no se migran: se muestran como texto y, al editar, aparecen como opción extra del desplegable.

## Non-Goals

- No conversiones entre unidades ni recálculo ni migración de unidades existentes (D5).
- No tocar `cookMinutes`, el tiempo de cocción ni la opcionalidad de los Ingredientes: van en `redisenar-formulario-de-receta`, que depende de esta feature.
- No cambiar la validación de guardado (`⚠ S07`), la Coincidencia (RD01/RD10), los Faltantes (RD15) ni el escalado de Raciones (RD03).
- Sin cambios en Room ni migraciones: la unidad sigue persistiéndose como texto (`ingredients.unit`, `shopping_items.unit`).
- Sin permisos, dependencias ni llamadas de IA nuevas; el contrato JSON compacto no cambia de claves (solo el resultado normalizado de `u`).
- No endurecer `ShoppingListRules`: los valores legados siguen siendo válidos; el vocabulario cerrado se garantiza en origen (desplegable y normalización de IA).

## Job Story

When creo una Receta o añado un ítem a la Lista de la Compra,
I want to elegir la unidad de una lista cerrada y verla siempre igual en toda la app,
so I can evitar unidades inventadas o inconsistentes sin perder las que ya tengo guardadas.

## Users And Permissions

- Usuario único (MVP de uso personal, sin cuentas): misma funcionalidad en es/en; sin permisos nuevos.

## Decisiones

- **D1 (usuario, 2026-11-09) — unidad obligatoria de lista cerrada**: el desplegable contiene exactamente **ud, g, kg, ml, l**, sin entrada vacía; las filas nuevas nacen con **«ud»** (fila de Ingrediente y diálogo de ítem de la Lista). Una unidad legada (fuera del enum, incluida la vacía) se muestra al editar como **opción extra** del desplegable y se conserva salvo que el usuario elija otra.
- **D2 (usuario, 2026-11-09) — unidad de la IA normalizada al enum**: al normalizar propuestas (Sugerencias y Propuestas del Pinche), la unidad vacía o fuera de la lista cae a **«ud»**; lo ya guardado no se toca.
- **D3 (derivada) — textos del enum**: los códigos visibles son los mismos en ambos idiomas («ud», «g», «kg», «ml», «l»), cada uno como clave es/en (`unit_ud` … `unit_l`) para mantener la paridad 1:1 (`P19`/`P25`).
- **D4 (derivada) — regla en dominio, desplegable en UI**: el parseo/normalización vive en `domain/model/IngredientUnit.kt` (funciones puras, `P25`) y el desplegable es un composable compartido en `ui/components/`; la presentación de la unidad (código → texto; legado → texto tal cual) es una función pura reutilizada por todas las superficies.
- **D5 (derivada) — legado sin migrar**: las unidades ya guardadas («cucharadas», vacías…) siguen viviendo en Room como texto; el caso «ítem sin unidad» de RD19 se conserva solo para esos ítems legados.

## Acceptance Scenarios

### Scenario 1: unidades del enum en el formulario, el Detalle y la Lista (verify 1)

Given abro el formulario de Receta
When añado tres Ingredientes con unidades «g», «ml» y «ud» elegidas con el desplegable y guardo
Then al reabrir en edición, en el Detalle y —al añadir los Faltantes— en la Lista de la Compra se muestran las mismas unidades con los textos del enum.

### Scenario 2: la unidad legada se conserva como texto (verify 1)

Given existe una Receta (o un ítem de la Lista) guardada con la unidad legada «cucharadas»
When la leo en el Detalle o en la Lista y después la edito
Then la unidad se muestra como el texto «cucharadas» y, en el desplegable de edición, aparece como opción extra seleccionada; si no elijo otra, al guardar se conserva igual.

### Scenario 3: desplegable en el diálogo de la Lista (verify 2)

Given la Lista de la Compra
When pulso «Añadir ítem»
Then el diálogo muestra el desplegable de unidad con los cinco valores y «ud» preseleccionado; al editar un ítem legado, su unidad aparece como opción extra.

### Scenario 4: normalización de la unidad de IA (verify 3)

Given el cliente de IA simulado devuelve propuestas con unidades «gramos», desconocidas y vacías
When acepto las propuestas (Sugerencias o Ficha del Pinche)
Then los Ingredientes guardados quedan con unidades del enum («ud» para lo desconocido y lo vacío) y se muestran con sus textos.

### Scenario 5: textos es/en

Given cambio el idioma de la interfaz a inglés
Then las unidades se muestran con sus textos en inglés (mismos códigos, D3) y no queda ninguna unidad hardcodeada fuera de `strings.xml`.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `redisenar-formulario-de-receta` (líneas 957–989) **partida el 2026-11-09** en `unificar-unidades` + `redisenar-formulario-de-receta` por decisión del usuario; esta spec cubre la primera.
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeIngredient.kt` — `unit: String` («unidad en texto libre»); vocabularios de enum puros viven en `domain/model/` (`Difficulty.kt`, `Aisle.kt`).
- `app/src/main/java/com/whatsforlunch/app/domain/model/ShoppingListItem.kt` y `domain/shoppinglist/ShoppingListRules.kt` (grep) — unidad como texto libre recortado (RD19); validación manual sin regla de unidad.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — `IngredientRow` (821–901): `OutlinedTextField` de unidad (862–873) con tag `CreateRecipeTestTags.ingredientUnit(index)`; `CreateRecipeActions.onIngredientUnitChange`. (El resto del formulario lo rediseña `redisenar-formulario-de-receta`.)
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — `IngredientFieldUiState.unit`, creación de filas y `formatQuantity` (438).
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — `ShoppingItemDialog` (728–800): `OutlinedTextField` de unidad con tag `ShoppingListTestTags.UNIT_FIELD` (111) y `shopping_list_unit_label`; filas con `formatQuantity` (323).
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — checklist (803: `formatQuantity` + unidad; `formatQuantity` interno reutilizado por `ShoppingListScreen`).
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt`, `ui/aisuggestions/AiSuggestionsScreen.kt` y `ui/pinche/PincheScreen.kt` (localizados con búsqueda) — superficies que muestran cantidad + unidad (drawer del Paso activo, tarjetas de Sugerencias y Ficha del Pinche).
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt` (`RawRecipeIngredient.unit`) y `SuggestionRules.kt` — `normalizeOne` (74–109; línea 86: `unit = ingredient.unit?.trim().orEmpty()`).
- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheProposalRules.kt` — mapeo de la Propuesta a `RecipeIngredient`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParser.kt` (campo `u`) y `data/ai/RecipeContract.kt` — contrato compacto `i:[{n,q,u,o}]` compartido por Sugerencias y Pinche.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — `create_recipe_ingredient_unit`, `shopping_list_unit_label`; línea base **323/323 claves + 5/5 plurales** (sesión 117).
- `app/src/androidTest/.../` — `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `ShoppingListE2ETest` (a ajustar) y `RecipeDetailE2ETest`, `CookModeE2ETest`, `AiSuggestionsE2ETest`, `PincheE2ETest` (regresión); inventario línea base **224 `@Test`** en 25 ficheros/27 clases.
- `docs/domain-model.md` (RD18/RD19/RD21 y casos límite de unidades), `DESIGN.md` (fila «Formulario de Receta», línea 164), `docs/technical-discovery.md` (comandos, 226–233), `docs/risks-and-open-questions.md` (sin `⚠` que toque esta feature).

### Existing Patterns To Follow

- Enums de vocabulario puros en `domain/model/` (`Difficulty`, `MealMoment`, `Aisle`) con etiquetas en `strings.xml`; para la unidad, que se compone en ~6 superficies, un único mapping compartido (D4).
- No hay ningún `ExposedDropdownMenuBox` en el proyecto hoy: se crea uno compartido con la estética de los campos existentes (`FieldShape`, `warmFieldColors`, mín. 48 dp).
- Textos visibles solo en `strings.xml` es/en con paridad 1:1 (`P19`/`P25`); testTags estables (`ingredientUnit(index)` y `UNIT_FIELD` se conservan en el campo del desplegable).
- E2E Compose con `testTag` + `ComposeTestRule`, fixtures locales y clientes de IA simulados; unit JVM en `app/src/test/`.
- Comandos: `./init.ps1`, `./gradlew lintDebug`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew connectedDebugAndroidTest` (focalizado) y `./e2e.ps1` (batería).

### Current Gaps

- Falta el desplegable compartido y el enum; no hay infraestructura, dependencias ni migraciones que crear.

## Technical Approach

1. **Enum de dominio** `domain/model/IngredientUnit.kt`: `UD, G, KG, ML, L` con `code` («ud»…«l») y funciones puras `fromCode(text): IngredientUnit?` (trim + minúsculas) y `normalize(text): String` (código del enum o «ud» para vacío/desconocido, D2). `RecipeIngredient.unit` y `ShoppingListItem.unit` **siguen siendo `String`** (sin migración): el enum es el vocabulario canónico, no el tipo de almacenamiento.
2. **Presentación compartida** `ui/components/UnitDropdown.kt`: (a) composable `UnitDropdown` (`ExposedDropdownMenuBox` con los 5 valores, mín. 48 dp, tag por parámetro) y (b) `unitLabel(raw)` que mapea código → clave es/en y deja lo legado como texto (D4/S2). Filas nuevas: «ud» (D1); al editar, un valor fuera del enum (incluido el vacío, con etiqueta «(sin unidad)») se añade como **opción extra seleccionada** que solo existe para conservar ese valor legado (D1/D5).
3. **Formulario y Lista**: `IngredientRow` y `ShoppingItemDialog` sustituyen su `OutlinedTextField` de unidad por `UnitDropdown` conservando los tags `ingredientUnit(index)` y `UNIT_FIELD`; `CreateRecipeViewModel` inicializa «ud» en filas nuevas; las filas de la Lista componen la unidad con `unitLabel`.
4. **IA (D2)**: en `SuggestionRules.normalizeOne` y el mapeo de `PincheProposalRules`, `unit = IngredientUnit.normalize(...)` (vacía/desconocida → «ud»); `RecipeSuggestionParser` y `RECIPE_CONTRACT` no cambian de claves. Lo ya guardado no se toca.
5. **Superficies de lectura**: Detalle (checklist), Modo Cocina (drawer), Sugerencias (tarjetas), Pinche (Ficha) y filas de la Lista pasan a componer la unidad con `unitLabel` (código → texto; legado → texto tal cual).
6. **Tests**: unit de `IngredientUnit` (códigos, mayúsculas/espacios, legados, vacío) y de las reglas afectadas (`SuggestionRules`/`PincheProposalRules` con unidades normalizadas); E2E: ajustar `CreateRecipeE2ETest`/`EditRecipeE2ETest`/`ShoppingListE2ETest` a los desplegables (con caso de unidad legada) y mantener en verde la regresión de Detalle/Modo Cocina/Sugerencias/Pinche.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/IngredientUnit.kt` — create: enum + `fromCode`/`normalize` puros (D2/D4).
- `app/src/main/java/com/whatsforlunch/app/ui/components/UnitDropdown.kt` — create: desplegable compartido + `unitLabel` + `optionTag` (D1/D4).
- `app/src/main/java/com/whatsforlunch/app/ui/theme/Field.kt` — create: `FieldShape` y `warmFieldColors()` movidos desde `CreateRecipeScreen.kt` para compartirlos con el desplegable (hallazgo de implementación).
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — modify: `IngredientRow` con `UnitDropdown` (tag `ingredientUnit(index)` conservado) y `ingredientOptionLabel` con `unitLabel`.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — modify: «ud» por defecto en filas nuevas.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — modify: `ShoppingItemDialog` con `UnitDropdown` («ud» por defecto, legado como opción extra) y filas con `unitLabel`.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — modify: checklist con `unitLabel` (`ingredientLine` gana el parámetro `unit`).
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt` — modify: drawer con `unitLabel`.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsScreen.kt` — modify: ingredientes con `unitLabel`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — modify: Ficha con `unitLabel`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` y `domain/pinche/PincheProposalRules.kt` — modify: normalizar la unidad al enum (D2).
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — modify: claves `unit_ud`…`unit_l` y la etiqueta del legado vacío; sin retirar existentes.
- `app/src/test/java/com/whatsforlunch/app/IngredientUnitTest.kt` — create; `SuggestionRulesTest`/`PincheProposalRulesTest` (modify).
- `app/src/androidTest/java/com/whatsforlunch/app/CreateRecipeE2ETest.kt`, `EditRecipeE2ETest.kt`, `ShoppingListE2ETest.kt`, `AiSuggestionsE2ETest.kt` — modify: desplegables, legado y normalización de IA.
- `docs/domain-model.md`, `DESIGN.md`, `docs/technical-discovery.md`, `CONTEXT.md` — update (ver Durable Documentation Impact).
- Specs de las features aceptadas afectadas — update: nota de enmienda al final (patrón completo, 2026-11-09), sin reescribir historia ni evidencias.
- `feature_list.json` — modify (solo `status`/`evidence` al cerrar; `notes` solo con mantenimiento autorizado).

## Hallazgos de implementación

1. `FieldShape` y `warmFieldColors()` vivían como `private` en `CreateRecipeScreen.kt`; se movieron a `ui/theme/Field.kt` para que el desplegable comparta la estética de los campos del formulario sin duplicarlos. El diálogo de la Lista pasa `shape`/`colors` por parámetro con los valores M3 por defecto, para no romper la estética de sus campos vecinos.
2. `unitLabel` es `@Composable` (resuelve `unit_ud`…`unit_l` con `stringResource`) en lugar de una función pura: los textos visibles deben vivir en `strings.xml` (S5/P19/P25), y la presentación sigue siendo una única función compartida por todas las superficies (D4).
3. Con D1, las filas nuevas llevan «ud» y eso cambia aserciones E2E existentes: la fila sin cantidad de la Lista muestra «{nombre} (ud)» (S5 de `anadir-item-a-la-lista`) y las etiquetas del diálogo de vínculos del Paso pasan de «Patatas (700)» a «Patatas (700 ud)». Las unidades legadas ya presentes en fixtures («g», «uds», «bote», «cucharadas») no cambian.
4. El tag `UNIT_FIELD` y `ingredientUnit(index)` siguen en el campo (ahora de solo lectura): `assertTextContains` sigue encontrando el valor y los `performTextReplacement` sobre la unidad se sustituyeron por interacción con el desplegable.
5. La primera batería completa destapó un **defecto preexistente ajeno a esta feature** (autorización del usuario para repararlo): `PantryManagementE2ETest#scannerSaveReturnsToThePantryWithTheSavedFoods` pulsa el «Volver» de la Despensa que `mover-opciones-a-bottom-bar` retiró (RF-MOB5) —el test nació en la rama paralela `mover-escaneo-a-la-despensa` sin ese cambio y la fusión de las dos ramas lo dejó imposible— y no se había detectado porque desde esa fusión solo se corrieron `compile`/`lint`/`unit`/`assemble`. Reparado con la misma autorización: la vuelta a Mis Recetas se hace por la pestaña de la barra inferior y se añade la aserción de que la Despensa no queda duplicada; clase completa en verde (9/9).

## Visual Design Impact

- UI involved: yes.
- Design source: `DESIGN.md` (fila «Formulario de Receta»: «gestor de ingredientes con unidades») y patrones del tema (`WarmSpacing`, `FieldShape`, `warmFieldColors`).
- Screens or states affected: formulario de Receta (solo fila de Ingrediente), diálogo de ítem de la Lista, filas de la Lista, Detalle (checklist), Modo Cocina (drawer), Sugerencias (tarjetas) y Pinche (Ficha).
- New design artifact required: no — el desplegable usa `ExposedDropdownMenuBox` de Material 3 con la estética de los campos actuales y objetivos ≥ 48 dp.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — sin capas ni fronteras nuevas (enum en `domain/model/`, composable en `ui/components/`, patrón existente).
- `CONSTRAINTS.md`: not needed — sin reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — flujo de trabajo y comandos intactos.
- `DESIGN.md`: update — el gestor de ingredientes y el diálogo de la Lista pasan a unidad de lista cerrada por desplegable.
- `docs/domain-model.md`: update — RD19 (unidad de lista cerrada, «ud» por defecto; legados como texto y «sin unidad» solo legado), RD21 (retirar «unidad u opcionalidad» → solo la unidad; la opcionalidad la retira `redisenar-formulario-de-receta`) y casos límite de unidades.
- `docs/technical-discovery.md`: update — nota del enum `IngredientUnit` (reglas puras; sin Room ni migraciones).
- `CONTEXT.md`: update — glosario «Ingrediente»: unidad de lista cerrada (la opcionalidad la retira la otra feature).
- Specs de crear-receta, editar-receta, ver-detalle-de-receta, cocinar-paso-a-paso, anadir-faltantes-a-la-lista, anadir-item-a-la-lista, sugerir-recetas-con-ia, aceptar-sugerencia-de-receta y resolver-dudas-con-pinche: update — nota de enmienda al final (patrón completo, decisión del usuario 2026-11-09), sin reescribir su historia.

## Implementation Plan

1. Enum + reglas: `IngredientUnit` con `fromCode`/`normalize` y tests unitarios.
2. UI compartida: `UnitDropdown` + `unitLabel`; su uso en la fila de Ingrediente y en el diálogo de la Lista (D1).
3. IA: normalización de la unidad en `SuggestionRules`/`PincheProposalRules` (D2) + tests.
4. Superficies de lectura: Detalle, Modo Cocina, Sugerencias, Pinche y filas de la Lista con `unitLabel` (S2).
5. Cierre: textos es/en con paridad, gates, E2E focalizada y batería, docs durables + enmiendas en specs aceptadas y `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [x] Crear `IngredientUnit` (ud, g, kg, ml, l) con `fromCode` y `normalize` (vacío/desconocido → «ud») en `domain/model/`.
- [x] Tests unitarios del enum (códigos, mayúsculas/espacios, legados, vacío).
- [x] Crear `ui/components/UnitDropdown.kt` (desplegable + `unitLabel`) con los cinco valores, opción extra para valores legados y objetivos ≥ 48 dp.
- [x] `IngredientRow`: `UnitDropdown` con «ud» por defecto en filas nuevas (tag `ingredientUnit(index)` conservado).
- [x] `ShoppingItemDialog`: `UnitDropdown` («ud» por defecto; legado como opción extra, tag `UNIT_FIELD` conservado) y filas de la Lista con `unitLabel`.
- [x] Normalizar la unidad de la IA en `SuggestionRules` y `PincheProposalRules` + tests.
- [x] `unitLabel` en Detalle (checklist), Modo Cocina (drawer), Sugerencias (tarjetas) y Pinche (Ficha).
- [x] Claves `unit_*` (y etiqueta del legado vacío) es/en con paridad 1:1.
- [x] E2E: ajustar `CreateRecipeE2ETest`/`EditRecipeE2ETest`/`ShoppingListE2ETest` (desplegables + legado + normalización con cliente simulado).
- [x] Actualizar docs durables y añadir notas de enmienda en las specs aceptadas afectadas.
- [x] Ejecutar el plan de verificación y capturar la evidencia.

## Verification Plan

- `./init.ps1` — entorno listo, app instalada y en ejecución (no arranca servicios de larga duración).
- `./gradlew lintDebug` — 0 errores (línea base: 0 errores / 48 warnings).
- `./gradlew testDebugUnitTest` — 0 fallos (línea base: 42 suites / 305 tests).
- `./gradlew assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismas claves en ambos `strings.xml` (línea base 323 + 5 plurales; + las claves `unit_*`).
- E2E focalizada (`./gradlew connectedDebugAndroidTest` con las clases afectadas): `CreateRecipeE2ETest`, `EditRecipeE2ETest` y `ShoppingListE2ETest` en verde con los desplegables (verify 1 = S1/S2, verify 2 = S3, verify 3 = S4 con `FakeAiClient`), más `RecipeDetailE2ETest`, `CookModeE2ETest`, `AiSuggestionsE2ETest` y `PincheE2ETest` sin regresión (S2).
- E2E batería completa (`./e2e.ps1`, ~15 min): sin regresiones sobre la línea base (224 tests / 0 fallos / 1 omitido por `Assume`); reconciliar el inventario `@Test` con los XML.
- Unit (verify 4): `IngredientUnitTest` (parseo del enum y caída a texto de los legados) y reglas de IA con `normalize`.
- Manual/adb con uiautomator (S5): comprobar los textos de unidad con la app en español e inglés.

## Evidence To Capture

- Salidas de lint/unit/assemble con recuentos en `docs/evidence/unificar-unidades/`.
- Resultado de la E2E focalizada y de la batería (recuento por clase y XML), reconciliado con el inventario `@Test`.
- 2 capturas es 1280×2856: (1) fila de Ingrediente con el desplegable abierto; (2) Detalle o diálogo de la Lista con unidades del enum y una legada.
- Comprobación manual de S5 (es/en) anotada en el informe.
- Resumen en `evidence` de `feature_list.json` y entrada en `docs/progress-history.md` al cerrar.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (solo unidades; sin `cookMinutes` ni opcionalidad — van en `redisenar-formulario-de-receta`).
- [ ] Acceptance scenarios pass (S1–S5; verify 1 = S1/S2, verify 2 = S3, verify 3 = S4, verify 4 = parseo).
- [ ] Verification evidence is present (`docs/evidence/unificar-unidades/`).
- [ ] Persistent E2E coverage was added/updated: desplegables en `CreateRecipeE2ETest`/`EditRecipeE2ETest`/`ShoppingListE2ETest` + legado + normalización de IA.
- [ ] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente (solo `status`/`evidence` en la lista).
- [ ] No unrelated product behavior or extra feature work was added (sin migraciones de Room, sin cambios en Coincidencia/Faltantes/`⚠ S07`).
