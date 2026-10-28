# Feature Implementation Spec: Vincular pasos e ingredientes

## Source Feature

- `id`: vincular-pasos-e-ingredientes
- `area`: funcional
- `depends_on`: crear-receta (accepted)
- `status`: not_started (a la hora de planificar; la spec no cambia `feature_list.json`)
- `source`: `feature_list.json` (entrada creada por mantenimiento autorizado por el usuario el 2026-10-27 al partir `cocinar-paso-a-paso`)

## Goal

Que cada **Paso** de una Receta pueda declarar **qué ingredientes usa**: la vinculación es **N:M** (un ingrediente puede usarse en varios pasos) y **opcional**, se asigna en el formulario de la Receta y se guarda con ella. Este vínculo es el prerrequisito del drawer «Ingredientes para este paso» de `cocinar-paso-a-paso`; aquí solo se modela, se edita y se persiste.

Para que el vínculo sobreviva a los guardados, esta feature da además a los Ingredientes un **id estable** (hoy el formulario deriva el id de la posición y Room regenera las filas en cada guardado, de modo que ningún id es persistente).

## Non-Goals

- **Modo Cocina** y su drawer: son `cocinar-paso-a-paso`, que depende de esta feature. Nada de pantallas nuevas ni navegación.
- **Cambios en prompts o en el contrato JSON de la IA** (`RecipeContract`, Sugerencias, Pinche): las recetas creadas por IA nacen **sin vínculos** y el usuario puede asignarlos editando (D4).
- **Validaciones nuevas**: publicar ni guardar borrador siguen sin exigir vínculos (`RecipeValidation` intacto, D3).
- **Vincular por nombre ni inferencia automática** de ingredientes en el texto de los pasos: solo asignación explícita del usuario.
- **Reordenar ingredientes o pasos**: el formulario no lo permite hoy y no se añade.
- **Ids estables de los Pasos**: nada los referencia; `RecipeStep.id` sigue como está (D5).
- **Detalle de Receta, Detalle del checklist, Escáner, Despensa, Lista de la Compra, IA y Room de otras tablas**: sin cambios.

## Job Story

When estoy definiendo la elaboración de mi receta en el formulario,
I want to marcar qué ingredientes usa cada paso,
so I can que al cocinar cada paso muestre solo lo que necesito tener a mano.

## Users And Permissions

- Usuario personal (1-5 usuarios, MVP): única interacción; sin roles, sin cuentas, sin permisos Android nuevos.

## Requisitos funcionales

- **RF-VPI1**: cada Paso expone `ingredientIds`, una lista **opcional** de ids de los Ingredientes de su Receta (N:M: un Ingrediente puede estar en varios Pasos; un Paso, en varios Ingredientes). El orden de la lista sigue el orden de ingredientes de la Receta (D2/D7).
- **RF-VPI2**: el formulario, en cada fila de Paso, ofrece «Ingredientes de este paso»: abre un diálogo con casillas que lista los ingredientes de la Receta en orden (nombre + cantidad/unidad) y guarda la selección del Paso. El control solo se muestra con ≥1 fila de ingrediente (D3).
- **RF-VPI3**: los vínculos se conservan al guardar (Borrador y Publicar) y al reabrir en edición; quitar un Ingrediente elimina sus vínculos de todos los Pasos y renombrarlo o cambiar su cantidad/unidad/opcionalidad los conserva (D6).
- **RF-VPI4**: los Ingredientes tienen un **id estable** persistido en Room (`ingredients.domainId`), con backfill determinista en la migración; los vínculos se persisten en `steps.ingredientIds` (lista separada por comas) dentro de la misma transacción de guardado de la Receta (D5).
- **RF-VPI5**: una Receta sin vínculos (creada por IA o legada de la v3) es válida en todos los puntos: sus Pasos se muestran sin ingredientes y se pueden vincular editando (D4).

## Acceptance Scenarios

### Scenario 1: asignar ingredientes a un paso

Given una Receta en el formulario con los ingredientes «Patatas» y «Huevo» y dos Pasos
When abro «Ingredientes de este paso» del Paso 1 y marco «Patatas»
Then el Paso 1 queda con 1 ingrediente y el Paso 2 sigue sin ninguno

### Scenario 2 (verify 1): los vínculos sobreviven al guardar y editar

Given el Scenario 1 guardado como Borrador o Publicada
When reabro la Receta en edición
Then el Paso 1 conserva «Patatas» marcada y el Paso 2 sigue sin ingredientes

### Scenario 3 (verify 2): borrar un ingrediente limpia sus vínculos

Given «Patatas» está vinculada a los Pasos 1 y 3 y «Huevo» al Paso 2
When elimino la fila de «Patatas»
Then los Pasos 1 y 3 quedan sin «Patatas» y el Paso 2 conserva «Huevo»; guardar sigue siendo válido con Pasos sin vínculos

### Scenario 4 (verify 3): migración v3→v4 sin pérdida

Given una base v3 con una Receta, sus Ingredientes y sus Pasos
When la app abre la base con `MIGRATION_3_4`
Then la Receta, sus ingredientes y sus pasos siguen intactos, cada ingrediente tiene `domainId` (`{recipeId}-ingredient-{position}`) y todos los Pasos quedan sin vínculos

### Scenario 5: receta sin vínculos se puede vincular

Given una Receta creada por la IA (Sugerencia o Propuesta del Pinche) con pasos sin vínculos
When la edito, vinculo un ingrediente a un paso y guardo
Then el vínculo queda guardado y publicar no exige nada más

### Scenario 6: textos es/en

Given el idioma de la app en español o en inglés
Then los textos del control y del diálogo cambian con el idioma (paridad de claves)

## Repository Research

### Files Inspected

- `feature_list.json` — entrada de `cocinar-paso-a-paso` («drawer de ingredientes del paso activo») y decisiones del usuario 2026-10-27 que parten la feature en dos.
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStep.kt`, `RecipeIngredient.kt`, `Recipe.kt` — hoy `RecipeStep(id, text, timerMinutes)` sin vínculos e `RecipeIngredient.id` sin semántica estable.
- `app/src/main/java/com/whatsforlunch/app/data/local/IngredientEntity.kt`, `StepEntity.kt`, `RecipeWithDetails.kt`, `RecipeDao.kt`, `RecipeDatabase.kt` — Room v3; `upsertRecipe` borra y reinserta ingredientes/pasos en cada guardado (los `id` autoincrementales cambian), y `MIGRATION_1_2`/`MIGRATION_2_3` son el patrón de migración.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — `toDomain` devuelve `id = entity.id.toString()` (fila, inestable) y `toEntity` descarta los ids de dominio.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — `IngredientFieldUiState`/`StepFieldUiState` solo tienen `key: Long` de UI; `buildRecipe` deriva los ids de dominio de la posición (`"$id-ingredient-$index"`), y `onRemoveIngredient`/`onRemoveStep` solo filtran filas.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — `StepRow` (texto + temporizador) y `CreateRecipeTestTags` (`stepText(index)`, `stepTimer(index)`…).
- `app/src/main/java/com/whatsforlunch/app/domain/createrecipe/RecipeValidation.kt` — requisitos de Publicar (título, foto, ≥1 ingrediente, ≥1 paso, filas válidas); nada sobre vínculos.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` y `domain/pinche/PincheProposalRules.kt` — crean `RecipeIngredient`/`RecipeStep` con id en blanco para las recetas de IA.
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` — patrón del test de migración (`migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables`, DB de prueba con `addMigrations`).
- `app/src/androidTest/java/com/whatsforlunch/app/CreateRecipeE2ETest.kt`, `EditRecipeE2ETest.kt` — E2E del formulario (alta y edición) donde entra la cobertura nueva.
- `docs/domain-model.md`, `CONTEXT.md`, `DESIGN.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md` — reglas RD1-RD20, términos (Ingrediente, Paso), fila «Formulario de Receta» y comandos/gates.

### Existing Patterns To Follow

- MVVM + StateFlow + Koin; pantalla sin estado con callbacks; reglas puras en `domain/` y validaciones centralizadas en `RecipeValidation` (P25).
- `RoomRecipeRepository` mapea entidades↔dominio en ficheros privados; las escrituras de una Receta van en una transacción (`RecipeDao.upsertRecipe`).
- Migraciones de Room declaradas en `RecipeDatabase` y registradas en `AppModule` (`addMigrations`); versiones documentadas en `docs/technical-discovery.md`.
- Textos visibles en `values/strings.xml` + `values-en/strings.xml` con paridad de claves; test tags por pantalla en objetos `*TestTags`.
- Tests: unit JVM en `app/src/test`, instrumentados/E2E en `app/src/androidTest` con fixtures locales y repos InMemory/Room.

### Current Gaps

- No existe ningún mecanismo de vínculo Paso↔Ingrediente en modelo, formulario ni Room.
- Los ids de Ingrediente no son estables (posición en el formulario, fila en Room) y no se persisten como tales: hay que añadir `domainId` + migración.
- Faltan los textos es/en del selector y del diálogo.

## Technical Approach

**Dominio** — `RecipeStep` gana `ingredientIds: List<String> = emptyList()` (ids de `RecipeIngredient.id`; opcional, N:M, en orden de ingredientes de la Receta). Sin reglas nuevas en `domain/`: la validación se mantiene en `RecipeValidation` sin cambios (D3).

**Ids estables (D5)** — `IngredientEntity` gana `domainId TEXT NOT NULL` y `toDomain` pasa a devolver `id = domainId`. `MIGRATION_3_4` añade la columna con backfill determinista `UPDATE ingredients SET domainId = recipeId || '-ingredient-' || position`, igual a la convención que ya usaba `buildRecipe`. El formulario conserva el id de cada fila: `IngredientFieldUiState`/`StepFieldUiState` ganan `id: String` (el cargado de la Receta, o `UUID.randomUUID()` al añadir la fila) y `buildRecipe` usa ese id en lugar del índice. `RoomRecipeRepository.saveRecipe` genera `"$recipeId-ingredient-$index"` para los ids en blanco (recetas de IA) al persistir. Los ids de Paso no se persisten (no los referencia nada).

**Persistencia** — `StepEntity` gana `ingredientIds TEXT NOT NULL` (ids separados por comas; los ids —UUID o `{recipeId}-ingredient-{n}`— no contienen comas). `MIGRATION_3_4`: `ALTER TABLE ingredients ADD COLUMN domainId TEXT NOT NULL DEFAULT ''`, el `UPDATE` de backfill y `ALTER TABLE steps ADD COLUMN ingredientIds TEXT NOT NULL DEFAULT ''` (las entidades **no** declaran `defaultValue` para que Room no compare defaults). Al cargar, el mapper descarta referencias huérfanas (ids que no existen entre los ingredientes de la Receta). Todo se reescribe en la transacción actual de `upsertRecipe`; sin tablas nuevas ni cambios en `RecipeEntity`/`PantryItemEntity`/Lista de la Compra.

**Formulario** — `StepFieldUiState.ingredientIds: List<String>` + `onStepIngredientToggle(stepKey, ingredientId)`. En `StepRow`, debajo del temporizador, un control «Ingredientes de este paso» (texto resumen: «Sin ingredientes» o «N ingredientes») que abre `StepIngredientsDialog` con un `Checkbox` por ingrediente de la Receta en orden («nombre (cantidad unidad)»), botones Aceptar/Cancelar y estado vacío «Añade ingredientes primero» si no hay filas. El control se oculta con 0 ingredientes. `onRemoveIngredient(key)` elimina el id de la fila borrada de todos los `steps[].ingredientIds`; borrar un paso solo borra el paso; renombrar o editar un ingrediente no toca los vínculos (son por id).

**AI y el resto** — `SuggestionRules`, `SuggestionAcceptance`, `PincheProposalRules` y el parser de Sugerencias siguen creando pasos con `ingredientIds` vacío (parámetro por defecto): sin cambios de código ni de prompts (D4).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStep.kt` — modify; `ingredientIds` y comentario de semántica.
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeIngredient.kt` — modify; comentario del id estable (sin cambio de campos).
- `app/src/main/java/com/whatsforlunch/app/data/local/IngredientEntity.kt` — modify; `domainId`.
- `app/src/main/java/com/whatsforlunch/app/data/local/StepEntity.kt` — modify; `ingredientIds` (CSV).
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — modify; versión 4 y `MIGRATION_3_4`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; registrar `MIGRATION_3_4`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — modify; mapeo de `domainId`/CSV, generación de ids en blanco y descarte de huérfanos.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — modify; ids por fila, `ingredientIds` por paso, toggle y limpieza al borrar filas.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — modify; control por paso, `StepIngredientsDialog` y test tags.
- `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml` — modify; textos nuevos con paridad.
- `app/src/test/java/com/whatsforlunch/app/CreateRecipeViewModelTest.kt` — modify; unit de vínculos (asignar, quitar ingrediente, round-trip de ids).
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeMigrationTest.kt` — create; migración v3→v4 (patrón de `ShoppingListE2ETest.migrationFromV2…`).
- `app/src/androidTest/java/com/whatsforlunch/app/CreateRecipeE2ETest.kt`, `EditRecipeE2ETest.kt` — modify; S1, S2, S3, S5.
- Docs: `docs/domain-model.md`, `DESIGN.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`, `CONTEXT.md` (ver más abajo). `feature_list.json`: entrada nueva + dependencia de `cocinar-paso-a-paso` (mantenimiento autorizado); al cerrar, solo `status`/`evidence`.

## Visual Design Impact

- UI involved: **sí** (solo el formulario de Receta). Design source: `DESIGN.md` (tokens Warm Culinary Craft) + `uxdocs/chefencasa_registrar_y_editar_receta/` (inspiración `P16`).
- Screens/states afectados: Formulario de Receta, sección «Pasos de Elaboración»: nuevo control por fila de Paso y diálogo de selección con casillas (superficie `surface-container-lowest`, objetivo táctil ≥ 48 dp, patrón de diálogos ya usados en el formulario).
- Estados nuevos: resumen del paso («Sin ingredientes»/«N ingredientes»), fila de opción marcada/desmarcada y vacío del diálogo.
- New design artifact required: **no**; se compone con los componentes y tokens existentes.
- Sin cambios visuales en Mis Recetas, Detalle, Escáner, Pinche ni Lista de la Compra.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** (no existe y no hace falta crearlo): sin capas ni dependencias nuevas.
- `CONSTRAINTS.md`: **not needed** (no existe y no hace falta crearlo): sin MUST/MUST NOT nuevos.
- `AGENTS.md`: **not needed** — sin cambios de flujo, comandos ni convenciones.
- `docs/domain-model.md`: **update** — nueva regla **RD21** (vínculo Paso↔Ingrediente N:M opcional e ids estables de Ingrediente), ajuste de la ficha «Paso» y casos límite nuevos.
- `docs/technical-discovery.md`: **update** — Room pasa a v4 con `MIGRATION_3_4` (columnas nuevas en `ingredients`/`steps`).
- `DESIGN.md`: **update** — la fila «Formulario de Receta» suma la asignación de ingredientes por Paso.
- `docs/risks-and-open-questions.md`: **update** — registrar `Q-vincular-pasos-e-ingredientes-1..3` (D1-D3) al implementar.
- `CONTEXT.md`: **update** — la definición de **Paso** suma sus ingredientes vinculados (opcionales).

## Decisions

- **D1** (usuario 2026-10-27): el alcance se **parte en dos features** — esta (`vincular-pasos-e-ingredientes`) es prerrequisito de `cocinar-paso-a-paso`, que conserva su «drawer de ingredientes del paso activo». `feature_list.json` se mantiene por autorización expresa (entrada nueva + dependencia).
- **D2** (usuario 2026-10-27): el vínculo es **N:M** — cada Paso declara sus ingredientes y un Ingrediente puede usarse en varios Pasos (a diferencia del prototipo, que etiqueta «(Paso N)» único). UI por paso con multi-selección.
- **D3** (derivada): la asignación es **opcional** y no cambia `RecipeValidation`; el control de cada paso se oculta cuando la Receta no tiene ingredientes.
- **D4** (derivada): las recetas creadas por IA (Sugerencias, Propuesta del Pinche) y las legadas nacen **sin vínculos**, editables a mano; **sin cambios** en prompts ni en `RecipeContract`.
- **D5** (derivada/técnica): id estable de Ingrediente persistido (`ingredients.domainId`, backfill `{recipeId}-ingredient-{position}`); vínculos en `steps.ingredientIds` (CSV); referencias huérfanas descartadas al cargar; ids de Paso sin cambios.
- **D6** (derivada): quitar un Ingrediente elimina sus vínculos de todos los Pasos; quitar un Paso solo borra el Paso; renombrar o editar un Ingrediente conserva sus vínculos.
- **D7** (derivada): dentro de un Paso, los vínculos se guardan y muestran en el orden de ingredientes de la Receta; el diálogo lista siempre los ingredientes en ese orden.

## Casos límite

| Caso | Comportamiento |
| --- | --- |
| Paso sin ingredientes vinculados | Válido en todo momento (D3); el resumen dice «Sin ingredientes» |
| Receta sin ingredientes | El control por paso no se muestra (D3) |
| Borrar un ingrediente vinculado | Sus vínculos desaparecen de todos los pasos (D6) |
| Borrar un paso | Solo desaparece ese paso y sus vínculos (D6) |
| Renombrar/cambiar cantidad, unidad u opcionalidad | Vínculos intactos (son por id, D6) |
| Receta de IA o legada (v3) | Pasos sin vínculos; se pueden asignar editando (D4/D5) |
| Referencia huérfana al cargar (id inexistente) | Se descarta sin error (D5) |
| Guardar dos veces la misma receta | ids y vínculos estables; sin filas duplicadas (`upsertRecipe` idempotente) |
| Escalar raciones | Sin efecto: los vínculos no contienen cantidades |
| Ids de IA en blanco al persistir | `RoomRecipeRepository` genera `{recipeId}-ingredient-{position}` (D5) |

## Implementation Plan

1. **Dominio**: `RecipeStep.ingredientIds` + comentarios de semántica de id; unit de la estructura.
2. **Room**: `IngredientEntity.domainId`, `StepEntity.ingredientIds`, versión 4, `MIGRATION_3_4` y mapeo (`RoomRecipeRepository`: ids en blanco, CSV, huérfanos); test de migración v3→v4.
3. **Formulario (ViewModel)**: ids por fila, `ingredientIds` por paso, `onStepIngredientToggle` y limpieza al borrar filas; unit de round-trip.
4. **Formulario (UI)**: control por paso, diálogo con casillas, textos es/en y test tags.
5. **E2E**: S1-S3 y S5 en `CreateRecipeE2ETest`/`EditRecipeE2ETest`, con regresión de los tests previos.
6. **Docs y cierre**: `domain-model.md` (RD21), `technical-discovery.md` (Room v4), `DESIGN.md`, `CONTEXT.md`, `risks-and-open-questions.md` (Q-…) y `PROGRESS.md`/`feature_list.json`.

## Implementation Tasks

- [x] T1: `RecipeStep.ingredientIds` (N:M opcional) + tests de dominio.
- [x] T2: `IngredientEntity.domainId` + `StepEntity.ingredientIds` + `MIGRATION_3_4` + registro en `AppModule`.
- [x] T3: mapeo en `RoomRecipeRepository` (ids en blanco → generados, CSV, huérfanos descartados) + test de migración `RecipeMigrationTest`.
- [x] T4: `CreateRecipeViewModel`: ids por fila, toggle de vínculos, limpieza al borrar ingrediente/paso + unit (S3).
- [x] T5: `CreateRecipeScreen`: control «Ingredientes de este paso», `StepIngredientsDialog`, test tags.
- [x] T6: textos es/en con paridad.
- [x] T7: E2E S1 (alta), S2 (round-trip de edición), S5 (receta sin vínculos) + regresión.
- [x] T8: docs (`domain-model.md` RD21, `technical-discovery.md`, `DESIGN.md`, `CONTEXT.md`, `risks-and-open-questions.md` Q-1..3).
- [x] T9: gates completos y cierre (`PROGRESS.md`, `feature_list.json` solo `status`/`evidence`).

## Verification Plan

- `./init.ps1` (Windows) o `./init.sh` — «Entorno listo», exit 0. El script ejecuta comprobaciones no bloqueantes (gradle, `installDebug`, arranque de la app) y **no** arranca procesos largos; puede imprimir comandos de seguimiento.
- `./lint.ps1` / `./gradlew :app:lintDebug` — **0 errores** y sin warnings nuevos sobre la línea base (49).
- `./unit.ps1` / `./gradlew :app:testDebugUnitTest --rerun-tasks` — todo verde (hoy 35 suites / 232 tests); entran aquí los unit de vínculos y de ids estables.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismo conjunto de claves en `values/strings.xml` y `values-en/strings.xml` (hoy 291/291).
- **Migración**: `RecipeMigrationTest` (instrumentado) — base v3 de prueba con Receta/ingredientes/pasos → abrir con `MIGRATION_3_4` → datos intactos, `domainId` backfilled, `ingredientIds` vacíos (verify 3 = S4).
- **E2E focalizada** (la feature cambia el formulario visible): `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.CreateRecipeE2ETest,com.whatsforlunch.app.EditRecipeE2ETest` — verde con S1/S2/S3/S5 (verify 1 = S2, verify 2 = S3) y los tests previos sin regresión. Fixtures locales, sin IA ni red.
- **E2E batería**: `./e2e.ps1` (`connectedDebugAndroidTest` completo) — sin regresiones sobre los 181 tests de inventario (los flakes conocidos del AVD se documentan y reejecutan como en sesiones previas).
- Manual (apoyo, con `adb`/uiautomator): vincular dos pasos, guardar, reabrir y comprobar la selección; borrar un ingrediente y ver cómo se limpia.

## Evidence To Capture

- `docs/evidence/vincular-pasos-e-ingredientes/`: informe + **2 capturas es 1280×2856** (PNG binario): (1) formulario con la selección de ingredientes de un paso desplegada; (2) la misma receta reabierta en edición con los vínculos conservados.
- Salida de los gates (`init`, `lint`, `unit`, `assembleDebug`, paridad), del test de migración y de la E2E focalizada (nombres de los tests) y recuento de la batería.
- `PROGRESS.md` (Estado verificado + Siguiente paso + entrada en `docs/progress-history.md`) y `evidence` de `feature_list.json`.
- Sin API keys, sin llamadas a la IA ni PII en la evidencia (`⚠ S05`).

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance (sin Modo Cocina, prompts, validaciones nuevas, reordenación ni cambios en Despensa/Lista/IA).
- [ ] S1-S6 pasan (verify 1 = S2, verify 2 = S3, verify 3 = S4) y los E2E previos del formulario siguen en verde.
- [ ] D1-D7 cumplidas (N:M, asignación opcional, IA sin vínculos, id estable + backfill, limpieza al borrar, orden de ingredientes).
- [ ] Evidencia presente: gates, migración, E2E, 2 capturas es válidas (cabecera PNG, 1280×2856) e informe en `docs/evidence/vincular-pasos-e-ingredientes/`.
- [ ] Cobertura E2E añadida/actualizada para el cambio de comportamiento visible del formulario.
- [ ] `feature_list.json` solo cambia `status`/`evidence` (la entrada nueva y la dependencia ya están hechas con autorización); `PROGRESS.md` al día; docs durables actualizadas (RD21, Room v4, DESIGN, CONTEXT, Q-1..3).
- [ ] Sin comportamiento ajeno ni trabajo de `cocinar-paso-a-paso` a añadido.
- [ ] Seguridad: sin secretos, sin permisos nuevos, sin red; migración que no pierde datos de recetas existentes.
