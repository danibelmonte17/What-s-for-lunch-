# Validación independiente - `vincular-pasos-e-ingredientes`

- **Veredicto**: **`accept`** (2 hallazgos **Low** no bloqueantes, corregidos por el orquestador tras el veredicto)
- **Spec**: `docs/specs/vincular-pasos-e-ingredientes.md` (RF-VPI1-5, S1-S6, D1-D7, T1-T9, checklist del validador)
- **Rama**: `feature/cocinar-paso-a-paso`, diff **sin commitear** (revisto con `git status`/`git diff`; validador sin commits ni push)
- **Implementación**: sesión 94 (`feature-implementer`, 2026-10-27), autoverificada como `passing`
- **Validación**: sesión del validador 2026-10-27, AVD `emulator-5554` (Pixel_10_Pro, API 37, locale es-ES), Windows/PowerShell 5.1, JDK corretto-17.0.20

## 1. Objetivo de revisión

Cada **Paso** de una Receta declara los **Ingredientes** que usa: vínculo **N:M opcional** (`RecipeStep.ingredientIds`), id estable de Ingrediente (`RecipeIngredient.id` persistido en `ingredients.domainId` con backfill `{recipeId}-ingredient-{position}`) y vínculos en `steps.ingredientIds` (CSV) en Room **v4** (`MIGRATION_3_4`). UI: control «Ingredientes de este paso» por fila de Paso + `StepIngredientsDialog` (casillas; «Aceptar» confirma por diff, «Cancelar» no toca la Receta); limpieza de vínculos al borrar una fila de Ingrediente. Sin cambios en `RecipeValidation`, prompts de IA, Modo Cocina, reordenación, Despensa ni Lista de la Compra.

Estado reclamado en `feature_list.json`: `passing` con `evidence` nueva. La validación independiente juzga contra la spec, el diff y evidencia de runtime, sin confiar en la auto-percepción del implementador.

## 2. Checks re-ejecutados vs. solo inspección

### Tier barato - RE-EJECUTADO por el validador y reproducido

| Gate | Comando | Resultado del validador |
|---|---|---|
| Entorno | `./init.ps1` (sin redirección) | verde: «Entorno listo», app `com.whatsforlunch.app` en ejecución en `emulator-5554` |
| Lint | `./gradlew :app:lintDebug --rerun-tasks` | **0 errores / 49 warnings** (línea base 49; XML `lint-results-debug.xml`: 49 `Warning`, 0 `Error`; ninguna warning apunta al código nuevo del diff) |
| Unit | `./gradlew :app:testDebugUnitTest --rerun-tasks` | **36 suites / 239 tests / 0 fallos / 0 omitidos** (inventario XML reconciliado: 35/232 de base + 1 suite `CreateRecipeViewModelTest` con 7 tests) |
| Compilación | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| Paridad es/en | script propio sobre `strings.xml` + `plurals` | **297/297** claves idénticas (291 de base + 6 nuevas) |

### Tier caro - RE-EJECUTADO por el validador (AVD `emulator-5554`, 2026-10-27)

El validador re-ejecutó la E2E focalizada y la migración (no solo inspección) por ser la feature de persistencia/Room más arriesgada del inventario y para cerrar la duda sobre la validación de esquema de Room tras `MIGRATION_3_4`:

- **Pasada 1** — `connectedDebugAndroidTest` con `class=RecipeMigrationTest,CreateRecipeE2ETest,EditRecipeE2ETest,ShoppingListE2ETest#migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables` (42 tests):
  - `RecipeMigrationTest.migrationFromV3KeepsRecipeDataAndBackfillsStableIngredientIds` **1/1 PASSED** (verify 3 = S4: backfill `mig-1-ingredient-{position}`, datos intactos, vínculos vacíos, y round-trip `saveRecipe`/`getRecipe` con vínculos sobre la base migrada real).
  - `CreateRecipeE2ETest` **24/24 PASSED**, incluidos los 4 nuevos: `stepIngredientsDialogAssignsIngredientsToStep` (S1), `removingIngredientRowClearsItsStepLinks` (S3/verify 2), `stepIngredientsControlIsHiddenWithoutIngredientRows` (D3), `stepIngredientsTextsAreLocalizedInEnglish` (S6) + 20 tests previos sin regresión.
  - `EditRecipeE2ETest` alcanzó 6/16 antes de un `INSTRUMENTATION_ABORTED: System has crashed` (flake conocido del AVD); `editingKeepsStepIngredientLinks` (S2/verify 1) **PASSED**. Dos tests previos fallaron con firma de flake: `headerSaveIsAbsentWhilePublishedSaveIsDisabled` (teardown: `ActivityScenarioRule.after -> close -> "Activity never becomes [DESTROYED]"`, el flake de teardown ya documentado en `PROGRESS.md`) y `saveChangesKeepsPublishedStatusAndOrigin` (sin traza de aserción, 2m41s; inconsistencia XML/HTML por el abort). El AVD quedó wedged («Can't find service: package») y se reinició (`adb emu kill` + arranque frío de `Pixel_10_Pro`).
  - **Pasada 2** (tras reiniciar el AVD) — `class=EditRecipeE2ETest,ShoppingListE2ETest#migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables`: **17/17 PASSED / 0 fallos / 0 omitidos** (XML verificado). Incluye los 10 tests de `EditRecipeE2ETest` que no llegaron a ejecutarse en la pasada 1, `recipeWithoutLinksCanBeLinkedWithoutNewRequirements` (S5), la re-eejecución en verde de los 2 tests flakeados (`headerSaveIsAbsentWhilePublishedSaveIsDisabled`, `saveChangesKeepsPublishedStatusAndOrigin`) y `migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables` (**la regresión real de la batería queda verificada**: los 4 builders de `ShoppingListE2ETest` con `MIGRATION_3_4` cierran bien la cadena 2→4).

**Total instrumentado re-ejecutado por el validador**: 42 tests en dos pasadas, **0 fallos reales** (los 2 fallos confirmados como flakes del AVD por re-ejecución en verde). Cobertura independiente de S1-S6 y D1-D7 completa.

### Solo INSPECCIÓN (evidencia del implementador, consistente)

- **Batería completa `./e2e.ps1`**: 188 tests / 0 fallos / 2 omitidos (`SettingsE2ETest` por `Assume`, preexistentes). Corroboración estática independiente: **188 `@Test` en `app/src/androidTest`** (= 181 de la sesión 92 + 7 nuevos: 4 + 2 E2E + 1 migración). La primera pasada del implementador (4 fallos: 1 regresión real de la cadena de migraciones en `ShoppingListE2ETest`, reparada y verificada por el validador arriba, + 3 flakes de inyección de toques en `PantryManagementE2ETest` reejecutados 32/32) es coherente con los patrones de flake conocidos.
- **Evidencia**: `docs/evidence/vincular-pasos-e-ingredientes/` (informe + 2 PNG) verificada byte a byte por el validador: cabecera `89 50 4E 47 0D 0A 1A 0A` y dimensiones **1280x2856** en ambas (158 062 y 198 532 bytes), más inspección visual (ver F1).

## 3. Hallazgos por severidad

### F1: el informe de evidencia describe las capturas con detalles inexactos

- **Severidad**: Low (no bloqueante)
- **Evidencia**: `docs/evidence/vincular-pasos-e-ingredientes/informe.md` § «Capturas» vs. inspección visual de los PNG. (a) La captura 01 se describe como «diálogo ... desplegado y **«Patatas» marcada**», pero en la imagen las dos casillas están **sin marcar** (el diálogo está abierto con «Patatas (700)» y «Huevo (2)» listados, que es lo que exige la spec: «selección de ingredientes de un paso desplegada»). (b) La captura 02 se describe con «Paso 1 con «1 ingrediente» **y Paso 2 con «Sin ingredientes»**», pero solo el control del Paso 1 («1 ingrediente») es visible; el control del Paso 2 queda por debajo del borde de la captura.
- **Por qué importa**: la evidencia duradera debe describir exactamente lo que muestran las imágenes; una descripción sobredimensionada puede inducir a error en sesiones futuras (el comportamiento en sí está verificado por los tests instrumentados, no por las capturas).
- **Cambio requerido (asumido por el orquestador tras el veredicto)**: ajustar las descripciones del informe a lo real y/o **regenerar la captura 01** con la casilla «Patatas» efectivamente marcada (posible desfase entre el estado de composición y el frame pintado al capturar); si se regenera, mantener cabecera PNG y 1280x2856 y actualizar los bytes/tamaños citados en el informe.
- **Verificación tras la corrección**: releer el informe frente a las imágenes; `89 50 4E 47 0D 0A 1A 0A` y 1280x2856 en las capturas finales.

### F2: recuento de claves nuevas incorrecto en el informe y en `PROGRESS.md`

- **Severidad**: Low (no bloqueante)
- **Evidencia**: `informe.md` § «Gates» dice «**7 nuevas**: control, diálogo y resumen» y `PROGRESS.md`/`docs/progress-history.md` (sesión 94) dicen «7 strings es/en»; el diff añade **6 claves** (5 `string` + 1 `plurals`: `create_recipe_step_ingredients`, `_none`, `_empty`, `_accept`, `_cancel` y `create_recipe_step_ingredients_count`), coherente con 291 + 6 = **297** (paridad verificada por el validador: 297/297).
- **Por qué importa**: cifras durables que no cuadran con el diff dificultan la auditoría futura.
- **Cambio requerido (asumido por el orquestador tras el veredicto)**: «7» → «6 claves (5 strings + 1 plurals)» en el informe y en las entradas de `PROGRESS.md`/`docs/progress-history.md` de la sesión 94.
- **Verificación tras la corrección**: 291 + 6 = 297 citado de forma consistente.

### Notas (no bloqueantes, sin cambio requerido)

- **N1 - Flakes del AVD durante la validación**: la pasada 1 murió con `INSTRUMENTATION_ABORTED: System has crashed` tras un teardown de `ActivityScenarioRule` lento (2m41s) y dejó el AVD wedged («Can't find service: package»). Patrón ya documentado en `PROGRESS.md` («Incidencias abiertas del entorno»); resuelto reiniciando el AVD y re-ejecutando (17/17 verde). No es un defecto de la feature.
- **N2 - Desviaciones benignas de «Expected File Changes»**: `CreateRecipeViewModelTest.kt` es fichero **nuevo** (la spec decía «modify»; no existía previamente: las 7 unit de la feature son toda la clase) y `ShoppingListE2ETest.kt` se modifica **sin estar en la lista** (registra `MIGRATION_3_4` en sus 4 builders de Room, consecuencia necesaria de Room v4 y del mismo tipo que el registro en `AppModule` que sí pide la spec; la necesidad la detectó la propia batería del implementador y la ha re-verificado el validador). Ambas desviaciones están justificadas y documentadas.
- **N3 - Workaround de localización del diálogo (S6)**: el contenido del `Dialog` de Material3 resuelve strings con los recursos de la Activity (no el `LocalContext` inyectado); el test EN muta `Resources.updateConfiguration` (API deprecada, `@Suppress("DEPRECATION")`) y lo restaura en `finally`. Es solo código de test, está documentado en el informe como micro-decisión y pasa; frágil si cambia el orden/paralelismo de tests, aceptable hoy.
- **N4 - E2E del formulario con repositorio en memoria**: `CreateRecipeE2ETest`/`EditRecipeE2ETest` usan `InMemoryRecipeRepository`; el round-trip de persistencia **Room** de los vínculos lo cubre `RecipeMigrationTest` (`saveRecipe`/`getRecipe` sobre base migrada real). Cobertura conjunta adecuada para RF-VPI3/RF-VPI4.
- **N5 - `init.ps1` bajo redirección `2>&1` en PowerShell 5.1**: el stderr de `adb shell monkey` («args: [...]») se convierte en `NativeCommandError` por `$ErrorActionPreference = "Stop"` y el script sale con exit 1 aunque todo esté verde. Ejecutado sin redirección reproduce «Entorno listo». Es un script del harness (solo lectura); se deja constancia operativa para futuros validadores.

## 4. Corrección esperada (F1/F2, asumida por el orquestador)

Veredicto `accept`; las correcciones de F1 y F2 son documentales y las aplicará el orquestador justo después de este veredicto (informe de evidencia + entradas de sesión 94 en `PROGRESS.md`/`docs/progress-history.md`, y captura 01 regenerada si procede). No hay repair brief: no se requiere tocar código de la app.

## 5. Seguridad (aplicando `security-checklist.md` acotado al diff)

- **Secretos/configuración**: sin API keys, tokens, credenciales ni PII en el diff ni en la evidencia (fixtures «Tortilla de patatas»/«Paella»). `local.properties`/keystore intactos.
- **Autenticación/autorización**: N/A (app personal local, sin roles ni cuentas).
- **Validación de entrada/inyección**: los ids que viajan al CSV son UUIDs o `{recipeId}-ingredient-{n}` (sin comas, comentado en `StepEntity`); el parseo descarta vacíos y huérfanos; SQL vía Room parametrizado y DDL estático de migración (sin concatenación de entrada del usuario).
- **Exposición de datos**: todo local (Room/filesDir); sin red ni logs sensibles.
- **Servicios externos/IA**: prompts y clientes de IA **sin cambios** (non-goal D4 cumplido).
- **Dependencias/herramientas**: sin cambios en `build.gradle.kts` ni dependencias nuevas.
- **Permisos**: `AndroidManifest` sin cambios (sin permisos nuevos).
- **Resultado**: **sin hallazgos de seguridad**. La migración v3→v4 no pierde datos (verificado en runtime: `RecipeMigrationTest`).

## 6. Docs durables / harness

- `docs/domain-model.md`: **RD21** + ficha «Paso» + relación N:M + casos límite nuevos + nota de implementación — exactamente lo que pide la spec; coherente con el código.
- `docs/technical-discovery.md`: Room **v4**, `MIGRATION_3_4` con su SQL y el comportamiento de `RoomRecipeRepository` — coherente con el código.
- `DESIGN.md`: fila «Formulario de Receta» con «Ingredientes de este paso» + diálogo de casillas; la UI sigue los tokens (control `surfaceContainerLowest`, objetivos tácticos ≥ 48 dp, patrón de diálogos del formulario) y `uxdocs/` no se ha tocado.
- `CONTEXT.md`: término **Paso** con sus Ingredientes vinculados (opcionales).
- `docs/risks-and-open-questions.md`: `Q-vincular-pasos-e-ingredientes-1..3` (D1-D3) registradas.
- `AGENTS.md`: sin cambios (correcto: sin cambios de flujo/comandos). `ARCHITECTURE.md`/`CONSTRAINTS.md`: no aplican (no existen; la spec declara «not needed»).
- `feature_list.json`: JSON válido; entrada `vincular-pasos-e-ingredientes` con `status: "passing"` y `evidence` (2 ítems) y `depends_on` de `cocinar-paso-a-paso` ampliado — el mantenimiento (entrada nueva + dependencia) está autorizado por D1 y documentado en la sesión 93. Nota de auditoría: los cambios de las sesiones 93 y 94 están mezclados en el working tree (todo sin commitear), por lo que no es posible separar por git qué campos tocó cada sesión; el contenido de la entrada es coherente con la spec y con el cierre declarado (solo `status`/`evidence` en el cierre), sin indicios de mutación no autorizada.
- `PROGRESS.md` + `docs/progress-history.md`: entradas de sesiones 93/94 presentes y coherentes (salvo la cifra «7 strings» de F2).
- Evidencia en `docs/evidence/vincular-pasos-e-ingredientes/` (informe + 2 capturas) y este informe en `docs/validations/`. Sin restos del test temporal de captura (`StepLinksEvidenceTest` no existe en el árbol; grep limpio).

## 7. Follow-up requerido

1. **Orquestador (tras el veredicto)**: aplicar F1 y F2 (descripciones de capturas + «7» → «6»; regenerar captura 01 si procede) y persistir el acceptance (`feature_list.json` → `accepted` + evidencia de esta validación).
2. **Opcional (mantenimiento futuro)**: la captura 02 podría recortarse/scrollear para que se vea también el control del Paso 2 (solo estético; la spec solo exige «la misma receta reabierta en edición con los vínculos conservados», que se ve).
3. **Siguiente feature**: al persistirse el acceptance, planificar `cocinar-paso-a-paso` con `feature-spec` (decisiones de usuario en la entrada de sesión 93 del historial).

## 8. Validator Checklist de la spec (punto por punto)

- [x] **Alcance**: sin Modo Cocina, sin prompts ni contrato JSON de IA, `RecipeValidation` intacto, sin reordenación, Despensa/Lista/IA sin cambios de comportamiento (el único toque fuera de la lista prevista es el registro de `MIGRATION_3_4` en `ShoppingListE2ETest`, justificado — N2).
- [x] **S1-S6 pasan** (verify 1 = S2, verify 2 = S3, verify 3 = S4): re-ejecutados por el validador en el AVD — S1/S3/D3/S6 en `CreateRecipeE2ETest` (24/24), S2/S5 en `EditRecipeE2ETest` (16/16), S4 en `RecipeMigrationTest` (1/1) — y los E2E previos del formulario siguen en verde (20 + 10 tests de regresión sin fallos).
- [x] **D1-D7 cumplidas**: N:M (unit + E2E), asignación opcional sin cambios en `RecipeValidation` (unit `publishValidationDoesNotRequireStepLinks` + E2E S5), IA sin vínculos (sin cambios en prompts/reglas; unit + E2E S5), id estable + backfill (`RecipeMigrationTest`), limpieza al borrar (unit + E2E S3), orden de ingredientes (unit `togglingStepIngredients...` + `orderStepLinks`).
- [x] **Evidencia presente**: gates, migración, E2E y batería documentados con comandos y conteos (todos reconciliados por el validador); 2 capturas PNG es **1280x2856** con cabecera válida e informe (con las inexactitudes menores de F1/F2 que corrige el orquestador).
- [x] **Cobertura E2E** del cambio de comportamiento visible del formulario: 6 tests nuevos E2E (S1/S2/S3/S5/S6/D3) ejecutados en verde.
- [x] **`feature_list.json`** solo `status`/`evidence` en el cierre (mantenimiento de la entrada/dependencia autorizado por D1 — ver nota de auditoría en §6); `PROGRESS.md` al día; docs durables actualizadas (RD21, Room v4, DESIGN, CONTEXT, Q-1..3).
- [x] **Sin comportamiento ajeno** ni trabajo de `cocinar-paso-a-paso` añadido (cero pantallas/navegación/drawer nuevos).
- [x] **Seguridad**: sin secretos, sin permisos nuevos, sin red; migración que no pierde datos (verificado en runtime).

---

**Veredicto final: `accept`** — la implementación cumple la spec (RF-VPI1-5, S1-S6, D1-D7), la verificación es reproducible (tier barato re-ejecutado íntegro + tier caro re-ejecutado en el AVD con 0 fallos reales), los non-goals están respetados, la seguridad no arroja hallazgos y las docs durables/harness están coherentes (salvo F1/F2, Low, corrección documental asumida por el orquestador).
