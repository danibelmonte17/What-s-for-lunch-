# Evidencia — Vincular pasos e ingredientes

- **Feature**: `vincular-pasos-e-ingredientes` (`docs/specs/vincular-pasos-e-ingredientes.md`)
- **Fecha**: 2026-10-27 · rama `feature/cocinar-paso-a-paso` · emulador `emulator-5554` (Pixel_10_Pro, API 37, locale es-ES)
- **Alcance**: vínculo **N:M opcional** Paso↔Ingrediente (RF-VPI1..5); id estable de Ingrediente (D5); sin cambios en `RecipeValidation`, prompts, Modo Cocina, reordenación, Despensa, Lista ni IA (non-goals respetados).

## Gates

| Gate | Comando | Resultado |
|---|---|---|
| Entorno | `./init.ps1` | verde («Entorno listo») |
| Lint | `lintDebug` | **0 errores, 49 warnings** (baseline 49: sin warnings nuevos) |
| Unit | `testDebugUnitTest --rerun-tasks` | **36 suites / 239 tests / 0 fallos** (baseline 35/232: +1 suite `CreateRecipeViewModelTest`, +7 tests) |
| Compilación | `assembleDebug` | OK |
| Paridad es/en | `values/strings.xml` vs `values-en/` | **297/297** claves idénticas (6 nuevas: 5 strings + 1 plurals del control y el diálogo) |
| Migración v3→v4 | `RecipeMigrationTest` | **1/1** (backfill `domainId = {recipeId}-ingredient-{position}` y `steps.ingredientIds` vacíos) |
| E2E focalizada | `CreateRecipeE2ETest` + `EditRecipeE2ETest` | **40/40** verde |
| Batería E2E completa | `./e2e.ps1` | **188 tests / sin regresión** (2 omitidos por `Assume` en `SettingsE2ETest`): primera pasada con 4 fallos — 1 regresión real (los builders de Room de `ShoppingListE2ETest` no registraban `MIGRATION_3_4` y la cadena 2→4 quedaba rota; corregido) y 3 flakes del AVD en `PantryManagementE2ETest` («Failed to inject touch input»); reejecución de ambas clases **32/32 verde** |

### Incidencia resuelta durante los gates

La primera pasada de la batería detectó que `ShoppingListE2ETest.migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables` exigía una migración 2→4 inexistente: sus 4 builders de Room solo registraban `MIGRATION_1_2` y `MIGRATION_2_3`. Se les añadió `MIGRATION_3_4` (mismo patrón que `AppModule` y `RecipeMigrationTest`) y la clase quedó en verde. Los 3 fallos de `PantryManagementE2ETest` eran flakes del emulador (inyección de toques); en la reejecución pasaron sin cambios.

## Escenarios verificados

### Unit (`CreateRecipeViewModelTest`, 7 tests)
- `togglingStepIngredientsAssignsRemovesAndKeepsIngredientOrder`: marcar/desmarcar y orden D7 (los vínculos se listan en el orden de ingredientes de la Receta).
- `removingIngredientClearsItsLinksFromEveryStep` (S3/D6): borrar una fila de Ingrediente limpia sus vínculos de todos los Pasos.
- `editingIngredientKeepsItsStepLinks` (D6): renombrar/cambiar cantidad, unidad u opcionalidad conserva los vínculos.
- `savedDraftKeepsStableIdsAndStepLinksWhenReopened` (D4/D5): ids estables y vínculos tras guardar y reabrir.
- `recipesWithoutStepLinksAreValidAndCanGainLinks` (D3): receta sin vínculos válida; se vincula editando.
- `publishValidationDoesNotRequireStepLinks` (D3): `RecipeValidation` no exige vínculos.
- `newIngredientRowsGetUniqueStableIds` (D5): ids únicos por fila.

### E2E focalizada (40/40, `CreateRecipeE2ETest` + `EditRecipeE2ETest`)
- **S1** `stepIngredientsDialogAssignsIngredientsToStep`: alta con «Patatas»/«Huevo» y 2 pasos → diálogo del Paso 1 → marcar «Patatas» → «Aceptar» → resumen «1 ingrediente»; «Cancelar» no toca la Receta.
- **S2** `editingKeepsStepIngredientLinks` (verify 1): guardar → reabrir en edición → resúmenes intactos; quitar una casilla se guarda.
- **S3** `removingIngredientRowClearsItsStepLinks` (verify 2): borrar la fila «Patatas» → ambos pasos «Sin ingredientes»; el otro Ingrediente se conserva.
- **S5** `recipeWithoutLinksCanBeLinkedWithoutNewRequirements`: receta sin vínculos válida para Borrador y Publicar; se vincula editando sin requisitos nuevos.
- **S6** `stepIngredientsTextsAreLocalizedInEnglish`: textos del control y el diálogo en inglés.
- **D3** `stepIngredientsControlIsHiddenWithoutIngredientRows`: sin filas de Ingrediente el control no se muestra.
- Resto de la batería de ambas clases (RF-32..RF-46 etc.): sin regresión.

### Migración (verify 3)
- `RecipeMigrationTest` v3→v4: la base v3 con recetas existentes abre como v4 con ids `domainId` con backfill, vínculos vacíos y datos intactos.

## Capturas (2, es, PNG 1280×2856)

1. `vincular-pasos-e-ingredientes-01-dialogo.png` — formulario de la Receta («Tortilla de patatas», ingredientes «Patatas» (700) y «Huevo» (2), 2 Pasos) con el diálogo **«Ingredientes de este paso»** del Paso 1 desplegado y **«Patatas (700)» marcada** («Huevo (2)» sin marcar).
2. `vincular-pasos-e-ingredientes-02-edicion.png` — la **misma Receta guardada como Borrador y reabierta en edición** con los vínculos conservados: el Paso 1 («Pochar la cebolla») muestra «1 ingrediente» en su control «Ingredientes de este paso»; el Paso 2 («Batir el huevo») queda justo debajo, con su resumen «Sin ingredientes» fuera del encuadre.

Generadas con un test instrumentado temporal (`StepLinksEvidenceTest`, eliminado tras la captura) que ejecuta el round-trip real crear → vincular → guardar → reabrir y captura la pantalla con `uiAutomation.takeScreenshot()` → MediaStore (sobrevive a la desinstalación tras `connectedDebugAndroidTest`) → `adb pull`. **Verificación byte a byte**: cabecera PNG `89 50 4E 47 0D 0A 1A 0A` y dimensiones 1280×2856 en ambas (159 002 y 198 532 bytes). La captura 01 se regeneró tras la validación (hallazgo F1) con la casilla ya marcada (`assertIsOn` + espera de render antes de capturar). Sin API keys, sin llamadas a IA y sin PII.

## Micro-decisiones de implementación

1. **Confirmación del diálogo** (D1): la selección es local hasta pulsar **«Aceptar»**, que confirma con un commit por diff (`onStepIngredientToggle`); **«Cancelar» no toca la Receta** (se descarta la selección pendiente).
2. **Formato y resumen**: la opción del diálogo se rotula «nombre (cantidad unidad)»; el resumen del control usa plurals (`create_recipe_step_ingredients_count`) con «Sin ingredientes» para 0.
3. **S6 (localización del diálogo)**: el contenido de `Dialog` de Material3 resuelve los strings con los recursos de la **Activity**, no con el `LocalContext` inyectado en tests (demostrado con volcado de semántica: mostraba «Aceptar»/«Cancelar» en es con el contexto en inglés). El test EN muta `Resources.updateConfiguration(en)` sobre los recursos de la Activity resumed (lookup dentro de `runOnMainSync`) y los restaura en `finally`; `@Suppress("DEPRECATION")`.
4. **Referencias huérfanas** (D7): al cargar se descartan sin error los ids de Ingrediente inexistentes; los vínculos se reescriben en la misma transacción de `upsertRecipe`.
