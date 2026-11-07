# Feature Implementation Spec: Eliminar el estado de Borrador y añadir filtro de favoritos

## Source Feature

- `id`: eliminar-estado-de-borrador-y-filtrar-favoritos
- `area`: funcional
- `depends_on`: crear-receta, editar-receta, buscar-y-filtrar-recetas, aceptar-sugerencia-de-receta, resolver-dudas-con-pinche (todas `accepted`)
- `status`: not_started (al planificar)
- `source`: `feature_list.json` (entrada creada en la sesión 109, 2026-11-07)

## Goal

Las Recetas dejan de tener estado Borrador/Publicada. Toda Receta guardada manualmente desde el formulario (alta y edición) exige los obligatorios —título, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila inválida (`⚠ S07` revisado)— con un único botón de guardado, y los distintivos «Borrador»/«Publicada» desaparecen por completo de la interfaz. La foto es siempre opcional (D11): las Recetas creadas desde una Sugerencia de IA o una Propuesta del Pinche siguen guardándose sin foto (D1) y cualquier Receta puede editarse y guardarse sin foto. Mis Recetas añade el chip «Favoritas» en su fila de filtros, combinable con la búsqueda y los filtros de momento de comida.

## Non-Goals

- No se añade marcado de favoritos en tarjetas de Mis Recetas ni en el Detalle: «Marcar en Mis Favoritas» sigue siendo solo un control del formulario (ya existente).
- No se toca el Detalle de Receta (D7): ya muestra estados vacíos para Recetas sin foto; solo se eliminan referencias al estado si las hubiera (hoy no hay ninguna en `RecipeDetailScreen`).
- No se cambian los flujos de IA: prompts, Sugerencias y Propuestas del Pinche siguen igual salvo que la Receta destino ya no lleva estado (D1 intacta: siguen sin foto).
- No se persisten los filtros ni la selección del chip «Favoritas» entre sesiones (sigue el patrón actual del filtro de momento).
- No se reescriben las specs ni las evidencias históricas de las features aceptadas; sus `notes` ya llevan la enmienda (sesión 109).
- No se toca la Despensa, la Lista de la Compra ni la navegación (otras features).

## Job Story

When guardo o edito Recetas y organizo mi biblioteca,
I want to guardar cada Receta lista para cocinar con un único botón y filtrar mis favoritas sin perder mis búsquedas,
so I can distinguir de un vistazo lo que ya está completo y encontrar rápido mis Recetas preferidas.

## Users And Permissions

- Usuario único sin roles: mismo comportamiento en alta, edición, Sugerencias y Pinche. Sin permisos ni autenticación nuevos.

## Requisitos funcionales

- RF-EBF1: El modelo de Receta no tiene estado. Se elimina `RecipeStatus` y el campo `Recipe.status`; la persistencia elimina la columna `recipes.status` (Room v5, `MIGRATION_4_5`, D5).
- RF-EBF2: El formulario (alta y edición) tiene un único botón de guardado: «Guardar»/«Save» en el alta y «Guardar cambios»/«Save changes» en edición (D6). Se retiran «Borrador» y «Publicar Receta».
- RF-EBF3: El guardado manual exige los obligatorios de `⚠ S07`: título, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila inválida. Si faltan requisitos, no se guarda y se muestra la lista de requisitos incumplidos junto al dock (hoy `PublishErrors`), con cabecera adaptada a «No se puede guardar todavía:».
- RF-EBF4: La foto es siempre opcional (D11): no figura entre los obligatorios ni en el alta ni en la edición, y nunca bloquea el guardado.
- RF-EBF5: Aceptar una Sugerencia o guardar una Propuesta del Pinche crea la Receta directamente, sin foto (D1) y sin estado.
- RF-EBF6: Los distintivos «Borrador»/«Publicada» desaparecen por completo (D2): píldora de tarjeta en Mis Recetas, píldora de cabecera del formulario y sus textos es/en.
- RF-EBF7: Mis Recetas añade el chip «Favoritas» en la fila de filtros (D3), combinable por AND con la búsqueda y el filtro de momento; al activarlo solo se muestran las Recetas con `favorite = true`.
- RF-EBF8: El estado «sin resultados» y «Ver todas las recetas» cubren el chip: restablecer limpia búsqueda, momento y «Favoritas» a la vez.

## Acceptance Scenarios

### Scenario 1: alta completa con el único botón (verify 1)

Given el formulario de alta con título, foto, 1 Ingrediente válido y 1 Paso válido
When pulso «Guardar»
Then la Receta se guarda y aparece en Mis Recetas sin ningún distintivo de estado

### Scenario 2: alta bloqueada sin obligatorios

Given el formulario de alta sin título o sin Ingredientes/Pasos válidos
When intento guardar
Then no se guarda nada y se lista el requisito incumplido («Falta el título», etc.) junto al botón, que está deshabilitado

### Scenario 3: edición exige obligatorios (verify 2)

Given una Receta guardada que abro en edición
When quito sus datos obligatorios o dejo filas inválidas
Then «Guardar cambios» queda bloqueado con la lista de requisitos y no se guarda nada

### Scenario 4: edición de Receta sin foto (verify 3, D11)

Given una Receta creada desde una Sugerencia o Propuesta del Pinche, sin foto, abierta en edición
When modifico su título o sus pasos y pulso «Guardar cambios»
Then se guarda sin foto y sin distintivo de estado

### Scenario 5: Sugerencia y Propuesta del Pinche (verify 3)

Given una Sugerencia aceptada o una Propuesta del Pinche guardada
When abro Mis Recetas
Then la Receta aparece sin foto y sin distintivo de estado

### Scenario 6: filtro Favoritas combinable (verify 4)

Given al menos una Receta marcada como favorita en el formulario
When activo el chip «Favoritas»
Then solo aparecen las favoritas; al combinarlo con la búsqueda y el momento se cruzan los criterios; sin coincidencias se muestra «No hay recetas que coincidan» y «Ver todas las recetas» restablece búsqueda, momento y Favoritas

### Scenario 7: sin textos de estado y paridad (verify 5)

Given la app en español y en inglés
When recorro Mis Recetas y el formulario
Then no aparece ningún texto «Borrador» ni «Publicada» y los textos nuevos existen en ambos locales

## Casos límite

- Receta con foto a la que se le quita la foto en edición: se guarda sin foto (D11: la foto nunca bloquea el guardado).
- Borradores legados sin foto creados con la validación antigua (solo exigía título): al editarlos se guardan sin foto (D11); en Mis Recetas se muestran sin distintivo desde el primer arranque tras la migración.
- Valores legados de `status` en la BD: la migración v5 recrea la tabla `recipes` sin la columna; no queda fallback ni valor residual (D5).
- Receta favorita sin foto (Sugerencia/Pinche + «Marcar en Mis Favoritas» en edición): el chip «Favoritas» la muestra con los mismos estados vacíos de foto que hoy.
- «Todas» solo resetea el filtro de momento: con «Todas» + «Favoritas» activos se ven las favoritas de todos los momentos.
- Una Receta sin Ingredientes o Pasos válidos no puede guardarse manualmente, pero las creadas por IA (D1) siguen existiendo incompletas: no se migra ni se borra ninguna.

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt` — `status: RecipeStatus = RecipeStatus.DRAFT` a eliminar (ya tiene `favorite: Boolean`).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStatus.kt` — enum `DRAFT`/`PUBLISHED` a eliminar.
- `app/src/main/java/com/whatsforlunch/app/domain/createrecipe/RecipeValidation.kt` — `validateForDraft` (solo título) y `validateForPublish` (obligatorios de S07) + `RecipeRequirement.messageRes()`.
- `app/src/main/java/com/whatsforlunch/app/domain/myrecipes/RecipeSearchFilter.kt` y `MyRecipesState.kt` — `filterRecipes(recipes, query, moment)` a ampliar con favoritos; `Empty/WithRecipes/NoResults`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptance.kt` y `domain/pinche/PincheProposalRules.kt` — crean la Receta con `photoPath = null, favorite = false, status = DRAFT`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeEntity.kt` — columna `status: String` NOT NULL; `RecipeDatabase.kt` (v4, `MIGRATION_1_2..3_4`); `RecipeDao.kt`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — mapeo `status.name` / `enumOrDefault(..., RecipeStatus.DRAFT)`.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeScreen.kt` — dock con «Borrador» (`create_recipe_save_draft`) + «Publicar Receta» (`create_recipe_publish`) o «Guardar cambios» según `editingStatus`; `StatusBadge` de cabecera (tag `create_recipe_status_badge`); `PublishErrors`; `FavoriteSection` («Marcar en Mis Favoritas»).
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` — `onSaveDraft`/`onPublish`/`onSaveChanges`, `canPublish`, `publishErrors`, `editingStatus`, `buildRecipe(state, status)`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `StatusPill` (tag `my_recipes_card_status`) en `RecipeCard`; `SearchAndFilterControls` (chip «Todas» + 4 momentos); `NoResults` con reset.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesViewModel.kt` — `searchQuery` (debounce 300 ms), `mealMomentFilter`, `onResetFilters`.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — 325 strings + 5 plurals por locale (paridad 330/330); claves `my_recipes_status_*`, `create_recipe_save_draft`, `create_recipe_publish*`.
- Tests: `RecipeValidationTest`, `CreateRecipeViewModelTest`, `RecipeSearchFilterTest`, `MyRecipesStateTest`, `MyRecipesSearchDebounceTest`, `SuggestionAcceptanceTest`, `PincheProposalRulesTest`, `AiSuggestionsViewModelTest`, `PincheViewModelTest`, `RecipeMigrationTest`, y E2E `CreateRecipeE2ETest.kt` (incluye `CreateRecipeAppE2ETest` y `MyRecipesCardsE2ETest`), `EditRecipeE2ETest.kt`, `SearchAndFilterRecipesE2ETest.kt`, `AiSuggestionsE2ETest.kt`, `PincheE2ETest.kt`, `RecipeDetailE2ETest.kt` (`incompleteDraftShowsEmptyStates`).
- `docs/domain-model.md` (ciclo de vida Borrador→Publicada, obligatorios, RD07/RD16), `docs/risks-and-open-questions.md` (fila `⚠ S07`), `CONTEXT.md` (glosario «Borrador», «Propuesta del Pinche»).

### Patterns Found

- Reglas puras en `domain/` con tests unitarios en `app/src/test`; UI sin reglas de negocio (patrón del repo, reglas en `docs/domain-model.md`).
- Migraciones Room en el `companion object` de `RecipeDatabase` con tests en `RecipeMigrationTest` (patrón de recrear/backfill en `MIGRATION_3_4`).
- Claves de strings snake_case por pantalla (`my_recipes_*`, `create_recipe_*`), texto de producto en es (`values/`) y en (`values-en/`); paridad por recuento de claves (sin test automático).
- testTags centralizados (`CreateRecipeTestTags`, `MyRecipesTestTags`) y E2E por pantalla en `app/src/androidTest`.
- Comandos: `./init.ps1` (arranque), `.\lint.ps1`, `.\unit.ps1`, `.\e2e.ps1` (`connectedDebugAndroidTest`), `.\gradlew.bat assembleDebug`.

### Current Gaps / Unknowns

- No hay test automático de paridad de claves es/en: la verificación es por recuento (convención del repo).
- `DROP COLUMN` de SQLite no es fiable en todas las versiones de Android: la migración v5 debe recrear la tabla `recipes` (patrón seguro).
- Desconocido menor: si el formulario permite hoy quitar una foto ya seleccionada (no inspeccionado a fondo); con D11 (foto siempre opcional) el caso ya no bloquea el guardado.

## Technical Approach

### Decisiones

- D1 (usuario, 2026-11-07): aceptar una Sugerencia o guardar una Propuesta del Pinche sigue creando la Receta sin foto (excepción a los obligatorios, que solo se exigen en el formulario).
- D2 (usuario, 2026-11-07): los distintivos «Borrador»/«Publicada» desaparecen por completo, sin sustituirlos por un «Publicada» fijo.
- D3 (usuario, 2026-11-07): el filtro es un chip «Favoritas» combinable con búsqueda y filtros.
- D4 (usuario, 2026-11-07; **superada por D11**): al editar una Receta nacida sin foto se puede guardar sin foto; la foto solo era obligatoria en el alta y en la edición de Recetas que ya tenían foto al abrir el formulario.
- D5 (usuario, 2026-11-07): la columna `status` se elimina con `MIGRATION_4_5` (Room v5) recreando la tabla `recipes`; sin columna residual ni fallback.
- D6 (usuario, 2026-11-07): botón único «Guardar»/«Save» en el alta y «Guardar cambios»/«Save changes» en edición.
- D7 (usuario, 2026-11-07): el Detalle no cambia.
- D8 (derivada): el chip «Favoritas» va al final de la fila de filtros, tras los chips de momento, con selección independiente (no radio) y testTag `my_recipes_filter_favorites`.
- D9 (derivada; **ajustada por D11**): `RecipeValidation` unifica sus validaciones en `validateForSave(recipe)` (sin parámetro de foto, D11); se elimina `validateForDraft`.
- D10 (derivada): las claves `create_recipe_publish*` se renombran a `create_recipe_save*` (ya no existe «publicar»); los testTags `create_recipe_save_draft`/`create_recipe_publish` se sustituyen por `create_recipe_save` (compartido por alta y edición) y se retiran `create_recipe_status_badge` y `my_recipes_card_status`.
- D11 (usuario, 2026-11-07): **la foto es siempre opcional**, en el alta y en la edición (supera D4). El hueco de foto del formulario mantiene «Sin foto (opcional)»/«No photo (optional)», que ahora es correcto en todo contexto; la foto desaparece de los obligatorios de `⚠ S07`, se retira el requisito `PHOTO` de `RecipeRequirement` y su cadena `create_recipe_save_error_photo`.

### Desconocidos

- Ninguno bloqueante. El nombre de las claves renombradas y el orden del chip son ajustables por el implementador si los tests lo exigen, sin cambiar el comportamiento.

### Hallazgos de implementación (2026-11-07)

- **`MyRecipesViewModel` necesita una clase intermedia para el `combine`**: con búsqueda, momento y favoritas, el `combine` de los tres `StateFlow` (más los de repositorio) supera las 5 aridades cómodas; los criterios de filtro viajan en un `FilterState` privado y `filterRecipes` conserva su firma con `favoritesOnly = false` por defecto (compatibilidad con llamadas existentes).
- **`RecipeValidation.validateForSave(recipe)` sin parámetro de foto (D11)**: al confirmarse que la foto es siempre opcional se retiró el parámetro `photoRequired` y el flag `photoRequiredOnLoad` del ViewModel (la variante con parámetro llegó a existir en la implementación inicial con D4 y se simplificó en la misma sesión); `CreateRecipeViewModel` recalcula `saveErrors` con la firma única.
- **D11 (2026-11-07, supera D4) confirmada por el usuario durante la implementación**: la foto deja de ser obligatoria en el formulario, lo que hace coherente el texto preexistente del hueco de foto («Sin foto (opcional)»); se retiran `RecipeRequirement.PHOTO` y `create_recipe_save_error_photo` (es/en) y se ajustan los tests que esperaban «Falta la foto».
- **Los tests E2E de la app real inyectan `ImageGenerator` falso vía Koin** (`GlobalContext.get().loadModules(listOf(module { single<ImageGenerator> { FakeGeneratedImageGenerator() } }), allowOverride = true)`, disponible en Koin 4.1.1): «Generar con IA» produce bytes de prueba sin pantallas del sistema ni `espresso-intents` (que no es dependencia del repo). La foto satisface el obligatorio porque `SystemPhotoAcquirer.generate` la persiste al aceptarla.
- **`RecipeMigrationTest` encadena `MIGRATION_3_4` + `MIGRATION_4_5`** en el test v3→v5 (la cadena completa) y añade un test v4→v5 con esquema legado v4 propio (`createLegacyV4Database`) que verifica la ausencia de la columna `status`, la conservación de `favorite` y el ciclo lectura/escritura tras migrar. Todo test que construya la BD Room debe registrar `MIGRATION_4_5` (4 sitios en `ShoppingListE2ETest`).
- **Algunos ficheros de test tenían comentarios con codificación doble (mojibake) en disco**: las ediciones con el editor sobre texto mojibake no casan; se han reescrito los ficheros afectados en UTF-8 limpio (`CreateRecipeE2ETest`, `EditRecipeE2ETest`) o editado con anclas ASCII.

## Expected File Changes

- `domain/model/Recipe.kt` — modify; eliminar `status`.
- `domain/model/RecipeStatus.kt` — delete; el enum desaparece (D5).
- `domain/createrecipe/RecipeValidation.kt` — modify; `validateForSave(recipe)` sustituye a `validateForDraft`/`validateForPublish` (sin foto entre los obligatorios, D11).
- `domain/suggestion/SuggestionAcceptance.kt`, `domain/pinche/PincheProposalRules.kt` — modify; dejan de fijar `status`.
- `domain/myrecipes/RecipeSearchFilter.kt` — modify; `filterRecipes(..., favoritesOnly: Boolean)`.
- `data/local/RecipeEntity.kt` — modify; quitar columna `status`.
- `data/local/RecipeDatabase.kt` — modify; `version = 5` + `MIGRATION_4_5` (recrear `recipes` sin `status`, copiando el resto de columnas incluida `favorite`).
- `data/repository/RoomRecipeRepository.kt` — modify; quitar mapeo de `status`.
- `ui/createrecipe/CreateRecipeViewModel.kt` — modify; `onSave()` único, `canSave`/`saveErrors`, foto siempre opcional (D11), sin `editingStatus`.
- `ui/createrecipe/CreateRecipeScreen.kt` — modify; dock con un botón, sin `StatusBadge`, `SaveErrors` (cabecera «No se puede guardar todavía:»), renombrado de tags.
- `ui/myrecipes/MyRecipesScreen.kt` — modify; quitar `StatusPill`, chip «Favoritas» (D8), reset ampliado.
- `ui/myrecipes/MyRecipesViewModel.kt` — modify; `favoritesOnly` + handler y `onResetFilters` ampliado.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — modify; bajas (`my_recipes_status_draft/published`, `create_recipe_save_draft`, `create_recipe_publish`, `create_recipe_save_error_photo`), renombrados (`create_recipe_publish_*` → `create_recipe_save_*`), altas (`create_recipe_save`, `my_recipes_filter_favorites` y su label de accesibilidad si aplica); paridad es/en.
- Tests unitarios: `RecipeValidationTest`, `CreateRecipeViewModelTest`, `RecipeSearchFilterTest`, `MyRecipesStateTest`, `MyRecipesSearchDebounceTest`, `SuggestionAcceptanceTest`, `PincheProposalRulesTest`, `AiSuggestionsViewModelTest`, `PincheViewModelTest`, `CompatibleRecipesTest` — modify; adaptar a la ausencia de estado y al chip.
- `app/src/androidTest/.../RecipeMigrationTest.kt` — modify; nuevo test v4→v5 y encadenar `MIGRATION_4_5` donde se construyen esquemas legados (también el helper de `ShoppingListE2ETest.kt`).
- E2E: `CreateRecipeE2ETest.kt` (incl. `CreateRecipeAppE2ETest`, `MyRecipesCardsE2ETest`), `EditRecipeE2ETest.kt`, `SearchAndFilterRecipesE2ETest.kt`, `AiSuggestionsE2ETest.kt`, `PincheE2ETest.kt` — modify; quitar aserciones de `RecipeStatus` y tests de borrador/publicar, adaptar a los botones/tags nuevos y añadir casos del chip «Favoritas».
- `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `CONTEXT.md`, `DESIGN.md` — modify; ver Durable Documentation Impact.

## Visual Design Impact

- UI involved: sí.
- Design source: `../../../../DESIGN.md`.
- Screens or states affected: Mis Recetas (tarjetas sin píldora de estado; fila de filtros con chip «Favoritas»; estado «sin resultados» con reset ampliado), formulario de Receta (dock con un solo botón; cabecera sin píldora de estado; lista de requisitos junto al dock).
- New design artifact required: no — se usan los patrones existentes (chip seleccionable como los de momento, `FilledTonalButton`/`Button` de Material3, tarjeta de requisitos ya existente). El chip «Favoritas» usa el mismo estilo que `MomentFilterChip` pero con selección tipo toggle y, si `DESIGN.md` define icono para favorito, el de `Icons.Filled.Favorite` ya usado en el formulario.

## Durable Documentation Impact

- `ARCHITECTURE.md` / `CONSTRAINTS.md`: not needed — no existen en el repo y este cambio no establece límites arquitectónicos nuevos (sigue el patrón domain/data/ui existente).
- `AGENTS.md`: not needed — no cambia el flujo de trabajo del agente, los comandos ni las convenciones.
- `docs/domain-model.md`: update — quitar el ciclo de vida Borrador→Publicada, la regla de obligatorios pasa a «guardado manual» (revisión de `⚠ S07`, sin foto entre los obligatorios, D11), actualizar RD07/RD16 y los casos límite de Sugerencia/Pinche (ya no «Borrador»).
- `docs/risks-and-open-questions.md`: update — revisar la fila `⚠ S07` (los obligatorios se exigen en todo guardado manual, sin foto, D11) y añadir las preguntas resueltas `Q-eliminar-estado-de-borrador-y-filtrar-favoritos-1..5` (D4-D7, D11).
- `CONTEXT.md`: update — retirar o reescribir la entrada «Borrador» del glosario y el «(Borrador)» de «Propuesta del Pinche»; si procede, añadir «Favorita» como término.
- `DESIGN.md`: update — quitar los distintivos de estado de las pantallas documentadas y anotar el chip «Favoritas» en la fila de filtros.

## Implementation Plan

1. Dominio: eliminar `RecipeStatus` y `Recipe.status`; unificar `RecipeValidation.validateForSave(recipe)` (D9, D11); ajustar `SuggestionAcceptance`/`PincheProposalRules` (D1/RF-EBF5) y `RecipeSearchFilter.filterRecipes` con `favoritesOnly`; actualizar tests unitarios de dominio.
2. Persistencia: quitar `RecipeEntity.status`, escribir `MIGRATION_4_5` (recrear `recipes` sin `status`), subir `RecipeDatabase` a v5 y limpiar `RoomRecipeRepository`; ampliar `RecipeMigrationTest` (v4→v5 conservando datos y `favorite`).
3. Formulario: `CreateRecipeViewModel.onSave()` único con `canSave`/`saveErrors` y foto siempre opcional (D11); `CreateRecipeScreen` con dock de un botón (D6), sin `StatusBadge`, `SaveErrors` renombrado; strings y tags (D10).
4. Mis Recetas: quitar `StatusPill` (D2), añadir chip «Favoritas» (D8) con `favoritesOnly` en el ViewModel y reset ampliado (RF-EBF8); strings.
5. E2E: actualizar los seis ficheros afectados (quitar aserciones de estado y tests de borrador/publicar; nuevos casos de alta bloqueada/guardado único, edición sin foto D11 y chip Favoritas combinado).
6. Cierre: docs durables, paridad es/en por recuento, gates completos y evidencia.

## Implementation Tasks

- [ ] Eliminar `RecipeStatus.kt` y el campo `status` de `Recipe.kt`; ajustar todos sus usos.
- [ ] Reescribir `RecipeValidation` (`validateForSave` sin foto entre los obligatorios, D11) y sus tests (`RecipeValidationTest`).
- [ ] Actualizar `SuggestionAcceptance`/`PincheProposalRules` y sus tests: Receta sin estado y sin foto (D1).
- [ ] Ampliar `RecipeSearchFilter.filterRecipes` con `favoritesOnly` y sus tests.
- [ ] Quitar `RecipeEntity.status`; `MIGRATION_4_5` recreando `recipes`; Room v5; `RoomRecipeRepository` limpio; test de migración v4→v5 y encadenamiento en helpers legados.
- [ ] `CreateRecipeViewModel`: `onSave()` único, `canSave`, `saveErrors`, foto siempre opcional (D11); actualizar `CreateRecipeViewModelTest`.
- [ ] `CreateRecipeScreen`: dock con «Guardar»/«Guardar cambios» (D6), retirar `StatusBadge` y botones «Borrador»/«Publicar Receta», renombrar tags y `SaveErrors` (D10).
- [ ] Strings es/en: bajas, renombrados y altas (D10, `my_recipes_filter_favorites`); paridad por recuento.
- [ ] `MyRecipesScreen`/`MyRecipesViewModel`: retirar `StatusPill`, chip «Favoritas» (D8) y reset ampliado; tests de ViewModel/filtros.
- [ ] E2E: `CreateRecipeE2ETest.kt`, `EditRecipeE2ETest.kt`, `SearchAndFilterRecipesE2ETest.kt`, `AiSuggestionsE2ETest.kt`, `PincheE2ETest.kt`, `RecipeMigrationTest.kt` al día con los escenarios de la spec.
- [ ] Docs durables (`docs/domain-model.md`, `docs/risks-and-open-questions.md`, `CONTEXT.md`, `DESIGN.md`) y gates finales.

## Verification Plan

- `./init.ps1` (o `./init.sh`): instala y arranca la app en el emulador; no debe lanzar procesos de larga duración extra (comportamiento actual del script).
- `.\lint.ps1` (`gradlew lintDebug`): 0 errores y sin warnings por encima de la línea base de 49.
- `.\unit.ps1` (`gradlew testDebugUnitTest`): todos los tests unitarios en verde, incluidos los de `RecipeValidation`, filtros con `favoritesOnly` y migración.
- `.\gradlew.bat assembleDebug`: BUILD SUCCESSFUL.
- Paridad es/en: recuento de claves de `values/strings.xml` vs `values-en/strings.xml` idéntico (convención del repo).
- E2E focalizada con `.\gradlew.bat connectedDebugAndroidTest` (emulador disponible): `CreateRecipeE2ETest` (S1, S2), `EditRecipeE2ETest` (S3, S4), `MyRecipesCardsE2ETest` (sin distintivos), `AiSuggestionsE2ETest` y `PincheE2ETest` (S5), `SearchAndFilterRecipesE2ETest` (S6) y `RecipeMigrationTest` (v4→v5).
- Batería completa `.\e2e.ps1`: sin regresiones (se esperan reescrituras de tests de borrador/publicar; los counts deben cuadrar con el inventario `@Test`).
- Cobertura E2E: obligatoria porque la feature cambia comportamiento visible de guardado y filtros; los tests se actualizan y añaden en los ficheros existentes por pantalla (patrón del repo).

## Evidence To Capture

- Salidas de `lint.ps1`, `unit.ps1` (suites/tests/0 fallos), `assembleDebug` y de la batería E2E (conteos por clase, con XML conservado en `docs/evidence/eliminar-estado-de-borrador-y-filtrar-favoritos/`).
- Capturas es 1280×2856: Mis Recetas con el chip «Favoritas» activo y sin distintivos de estado; formulario con el único botón de guardado y la lista de requisitos al bloquearse.
- Recuento de paridad de claves es/en tras la refactorización de strings.
- Resultado del test de migración v4→v5.
- Resumen en `evidence` de `feature_list.json` y entrada en `docs/progress-history.md` al cerrar.

## Trazabilidad

| RF | Escenario | Verificación |
| --- | --- | --- |
| RF-EBF1 | Casos límite (migración) | Unit `RecipeMigrationTest` v4→v5 |
| RF-EBF2/3 | S1, S2 | E2E `CreateRecipeE2ETest`, unit `RecipeValidationTest` |
| RF-EBF4 | S3, S4 | E2E `EditRecipeE2ETest`, unit `CreateRecipeViewModelTest` |
| RF-EBF5 | S5 | E2E `AiSuggestionsE2ETest`, `PincheE2ETest` |
| RF-EBF6 | S1, S7 | E2E `MyRecipesCardsE2ETest`, búsqueda de textos ausentes |
| RF-EBF7/8 | S6 | E2E `SearchAndFilterRecipesE2ETest`, unit `RecipeSearchFilterTest` |

## Validator Checklist

- [ ] Implementation stays within this feature's scope.
- [ ] Acceptance scenarios pass.
- [ ] Verification evidence is present.
- [ ] Persistent E2E coverage was added/updated (obligatorio: cambia comportamiento visible de guardado y filtros).
- [ ] `feature_list.json` and `../../../../PROGRESS.md` were updated correctly.
- [ ] No unrelated product behavior or extra feature work was added.
