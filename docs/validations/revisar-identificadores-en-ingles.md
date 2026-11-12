# Autoverificación: revisar identificadores en inglés

Feature: `revisar-identificadores-en-ingles`  
Spec: `docs/specs/revisar-identificadores-en-ingles.md`  
Entorno: Windows PowerShell 5.1, `Pixel_10_Pro` (API 37), JDK 17.

## Inventario de auditoría (T1)

Se inspeccionaron `app/src/main`, `app/src/test` y `app/src/androidTest`: declaraciones Kotlin, rutas/imports, tags, claves `SavedStateRegistry`, nombres de recursos y rutas de ficheros. Los positivos de comentarios y contenido visible se conservaron en español; los literales ingleses de fixtures y las APIs ya inglesas no requerían cambios.

| Área / identificador encontrado | Equivalencia aplicada |
|---|---|
| Paquetes `ui.misrecetas`, `domain.misrecetas` | `ui.myrecipes`, `domain.myrecipes` |
| Paquetes `ui.crearreceta`, `domain.crearreceta` | `ui.createrecipe`, `domain.createrecipe` |
| `MisRecetasScreen`, `EmptyMisRecetas`, `MisRecetasTestTags`, `MisRecetasRoute`, `MisRecetasViewModel`, `MisRecetasUiState` | `MyRecipesScreen`, `EmptyMyRecipes`, `MyRecipesTestTags`, `MyRecipesRoute`, `MyRecipesViewModel`, `MyRecipesUiState` |
| `MisRecetasState`, `misRecetasStateOf` | `MyRecipesState`, `myRecipesStateOf` |
| `CrearRecetaScreen`, `CrearRecetaActions`, `CrearRecetaTestTags`, `CrearRecetaRoute`, `CrearRecetaViewModel`, `CrearRecetaUiState`, `CrearRecetaNotice`, `CrearRecetaViewModel.toActions` | `CreateRecipeScreen`, `CreateRecipeActions`, `CreateRecipeTestTags`, `CreateRecipeRoute`, `CreateRecipeViewModel`, `CreateRecipeUiState`, `CreateRecipeNotice`, `CreateRecipeViewModel.toActions` |
| `AppRoutes.MIS_RECETAS`, `CREAR_RECETA`, `ESCANEAR_DESPENSA`, `crearReceta()`, `editarReceta(id)` | `MY_RECIPES`, `CREATE_RECIPE`, `SCAN_PANTRY`, `createRecipe()`, `editRecipe(id)` |
| `MisRecetasStateTest`, `MisRecetasSearchDebounceTest`, `MisRecetasLocalizationE2ETest`, `CrearRecetaE2ETest`, `CrearRecetaAppE2ETest`, `MisRecetasCardsE2ETest`, `BuscarYFiltrarRecetasE2ETest`, `EditarRecetaE2ETest`, `EditarRecetaRouteE2ETest`, `EditarRecetaAppE2ETest`, `EliminarRecetaE2ETest` | `MyRecipesStateTest`, `MyRecipesSearchDebounceTest`, `MyRecipesLocalizationE2ETest`, `CreateRecipeE2ETest`, `CreateRecipeAppE2ETest`, `MyRecipesCardsE2ETest`, `SearchAndFilterRecipesE2ETest`, `EditRecipeE2ETest`, `EditRecipeRouteE2ETest`, `EditRecipeAppE2ETest`, `DeleteRecipeE2ETest` |
| Métodos `appOpensInMisRecetasWithEmptyState`, `savedDraftAppearsInMisRecetas`, `editedTitleAppearsInMisRecetas` | `appOpensInMyRecipesWithEmptyState`, `savedDraftAppearsInMyRecipes`, `editedTitleAppearsInMyRecipes` |
| Tags `mis_recetas_*`, `crear_receta_*` | `my_recipes_*`, `create_recipe_*` |
| Rutas `mis_recetas`, `crear_receta?recipeId={recipeId}`, `escanear_despensa` | `my_recipes`, `create_recipe?recipeId={recipeId}`, `scan_pantry` |
| Claves internas `crear_receta_pick_gallery`, `crear_receta_request_camera`, `crear_receta_take_photo` | `create_recipe_pick_gallery`, `create_recipe_request_camera`, `create_recipe_take_photo` |
| 22 claves `mis_recetas_*` en cada locale | 22 claves `my_recipes_*` en cada locale |
| 61 claves `crear_receta_*` en cada locale | 61 claves `create_recipe_*` en cada locale |
| `momento_desayuno`, `momento_almuerzo`, `momento_cena`, `momento_postre` | `meal_moment_breakfast`, `meal_moment_lunch`, `meal_moment_dinner`, `meal_moment_dessert` |
| `dificultad_facil`, `dificultad_media`, `dificultad_dificil` | `difficulty_easy`, `difficulty_medium`, `difficulty_hard` |
| `RecipeSearchFilter`, `RecipeValidation`, `MealMoment`, `Difficulty`, `RecipeStatus`, `AppLaunchE2ETest`, `E2eDatabase`, demás APIs/modelos y variables inspeccionados | Ya estaban en inglés; no se renombraron. `filterChip`/`filterChipIcon` también eran ingleses. |

Los nombres de ficheros de producción y tests acompañan a sus clases renombradas; también se movieron los directorios de paquetes aplicables en los source sets presentes. No se detectaron otras declaraciones Kotlin de identificadores en español. Los comentarios, literales visibles de pruebas y copy se excluyeron deliberadamente del renombrado.

## Verify 1: barridos y salidas

Se ejecutaron los comandos de la spec sobre `app/src` después del cambio:

1. `git grep -inE '(class|object|interface|fun|val|var|enum class|package)[[:space:]]+[A-Za-z_]*(receta|recet|borrador|despensa|escaneo|escaner|escanear|compra|pinche|sugerencia|coincidencia|ingrediente|pasos|raciones|dificultad|momento|foto|favorito|titulo|historia|caloria|preparacion|coccion|guardar|eliminar|editar|buscar|filtrar|cocina|aviso|vacio)' -- 'app/src'`

   ```text
   app/src/main/java/com/whatsforlunch/app/data/local/IngredientEntity.kt:21:data class IngredientEntity(
   ```

   Único positivo: `IngredientEntity` usa el sustantivo inglés `Ingredient`; falso positivo de la subcadena `ingrediente` del patrón español, revisado manualmente. No queda identificador español en declaraciones.

2. `git grep -nE 'name="[a-z0-9_]*(receta|momento|dificultad)' -- 'app/src/main/res'`

   ```text
   (sin resultados; git grep exit 1 = no hubo coincidencias)
   ```

3. `git grep -nE '"(mis_recetas|crear_receta|escanear_despensa|momento_|dificultad_|crear_receta_)' -- 'app/src'`

   ```text
   (sin resultados; git grep exit 1 = no hubo coincidencias)
   ```

4. `git ls-files app/src | grep -iE '(receta|misrecetas|crearreceta|editar|eliminar|buscar|momento|dificultad)'` (equivalente ejecutado en PowerShell con `Select-String`):

   ```text
   (sin resultados)
   ```

Las coincidencias restantes de `MisRecetas`/`CrearReceta` que aparecen al hacer búsqueda amplia están solo en comentarios españoles, que la spec exige conservar.

## Conservación del copy y del esquema

- Ambos `strings.xml` contienen **95 claves** y tienen conjuntos de claves idénticos. Se renombraron 90 claves por locale (22 `my_recipes_*`, 61 `create_recipe_*`, 4 `meal_moment_*`, 3 `difficulty_*`); las otras cinco (`app_name` y cuatro `action_*`) permanecen.
- Comparación contra `HEAD`: se extrajeron los valores `<string>` por posición y se compararon sus bytes UTF-8 antes/después, ignorando únicamente el nombre de clave. Resultado: valores `values/strings.xml` **byte a byte intactos (95/95)** y valores `values-en/strings.xml` **byte a byte intactos (95/95)**. Por tanto, el copy español e inglés no cambió.
- `git diff --name-status -- app/src/main/java/com/whatsforlunch/app/data/local` no devuelve cambios; el esquema/SQL Room no se tocó. La suite instrumentada con Room real finalizó 61/61.
- No se detectó cambio de comportamiento/UI: diff limitado a nomenclatura, referencias y movimiento de paquetes/ficheros. Comentarios y documentación existentes permanecen en español. `CONTEXT.md` solo añade la aclaración de la equivalencia de identificadores pedida por la spec.

## Verify 2: harness

Los scripts se invocaron con el wrapper indicado para PowerShell 5.1. Salidas observadas:

| Comando | Resultado |
|---|---|
| `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init-final.log 2>&1"` | exit **0**; `BUILD SUCCESSFUL`; app instalada y viva en `Pixel_10_Pro`, «Entorno listo». |
| `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File lint.ps1 > .harness\lint-implementer.log 2>&1"` | exit **0**; `lintDebug` **BUILD SUCCESSFUL**, cero errores/MissingTranslation. Dos advertencias existentes deprecadas de Koin. |
| `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File unit.ps1 > .harness\unit-implementer.log 2>&1"` | exit **0**; `testDebugUnitTest` **19 tests, 0 fallos** (XML JUnit: 19/19). |
| `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File e2e.ps1 > .harness\e2e-implementer.log 2>&1"` | exit **0**; `connectedDebugAndroidTest` **61 tests, 0 fallos, 0 saltados** en `Pixel_10_Pro` API 37. |
| `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& gradlew.bat assembleDebug > .harness\assemble-implementer.log 2>&1"` | exit **0**; `assembleDebug` **BUILD SUCCESSFUL**. |

La primera invocación de `gradlew.bat assembleDebug` desde una shell sin `JAVA_HOME` terminó antes de arrancar Gradle con el mensaje de entorno «JAVA_HOME is not set»; se repitió fijando el JDK 17 ya instalado y el build pasó. No es un fallo de compilación ni se modificó configuración del repo.

## Trazabilidad

| Requisito | Resultado / evidencia |
|---|---|
| RF-1 | Cumplido: inventario de T1 incluido arriba; barridos realizados y falsos positivos descartados manualmente. |
| RF-2 | Cumplido: símbolos, imports, usos y tests actualizados; `lint`, compilación, unit y E2E pasan. |
| RF-3 | Cumplido: 90 claves traducidas por locale; 95/95 claves emparejadas y lint sin `MissingTranslation`. |
| RF-4 | Cumplido: tags, rutas y tres claves internas traducidas; pruebas instrumentadas 61/61. |
| RF-5 | Cumplido: paquetes y ficheros renombrados con `git mv`; barrido de rutas sin resultados. |
| RF-6 | Cumplido: comentarios/documentación y valores es/en preservados; evidencia de comparación byte a byte arriba. |
| RF-7 | Cumplido: `data/local/` ausente del diff y E2E con Room real 61/61. |
| RF-8 | Cumplido: init/lint/unit 19/19/e2e 61/61/assembleDebug en verde. |
| NFR-1 | Cumplido: aclaración documental en español y referencias Kotlin citadas en inglés. |
| NFR-2 | Cumplido: los nombres de código y paquetes siguen una única equivalencia inglesa; no quedan nombres antiguos en declaraciones o rutas. |

Estado de implementación: **lista para validación independiente**. No se declara aceptación.

---

# Validación independiente: revisar identificadores en inglés

- **Fecha**: 2026-10-20 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: `accept`
- **Spec**: `docs/specs/revisar-identificadores-en-ingles.md` · **Estado revisado**: `passing` en `feature_list.json`.
- **Entorno**: `Pixel_10_Pro` (API 37), JDK 17, `adb` de `%LOCALAPPDATA%\Android\Sdk`, Windows PowerShell 5.1.
- **Diff revisado**: renombres de paquetes, clases y ficheros en `app/src/{main,test,androidTest}`, `AppNavHost.kt`, `SystemPhotoAcquirer.kt`, `AppModule.kt`, `strings.xml` es/en, `CONTEXT.md` y los docs de estado. Sin cambios en `app/src/main/java/com/whatsforlunch/app/data/local/**` (esquema Room), `uxdocs/`, `DESIGN.md`, `.opencode/`, `gradle/` ni en los scripts del harness.

## Comprobaciones del validador

| Comprobación | Resultado |
|---|---|
| Prueba mecánica de «solo nomenclatura» (normalizar cada fichero Kotlin de `app/src` con el mapa de identificadores de la spec y compararlo byte a byte contra `HEAD`) | **18 de 20 ficheros idénticos** tras normalizar; los 2 restantes difieren solo en un comentario que citaba el nombre antiguo de otra clase de test (hallazgo **L1**, resuelto en el commit de cierre) |
| Barridos propios de verify 1 (declaraciones, claves de recurso, literales de ruta/tag/clave interna, rutas de fichero, referencias `R.string` antiguas) | **0 identificadores en español**; único positivo `IngredientEntity`, revisado como falso positivo (`Ingredient` en inglés) |
| `strings.xml` es/en (blob de `HEAD` extraído por redirección cruda de `cmd` + lectura UTF-8 explícita) | **95/95 valores idénticos byte a byte** en cada locale; conjuntos de claves es/en idénticos (90 renombradas + `app_name` y 4 `action_*` intactas); sin `MissingTranslation` |
| Esquema de Room | `git diff` y `git status` sobre `data/local/` vacíos; E2E con Room real 61/61 |
| `init.ps1` | exit **0**; app instalada y viva en `Pixel_10_Pro` |
| `lint.ps1` (`lintDebug`) | exit **0**; BUILD SUCCESSFUL |
| `unit` con `--rerun` forzado (sin caché) | exit **0**; **19/19** con los nombres nuevos (`MyRecipesStateTest`, `MyRecipesSearchDebounceTest`) |
| `e2e.ps1` (`connectedDebugAndroidTest`) | exit **0**; **61/61**, 0 fallos, 0 saltados. El primer pase abortó por una instrumentación externa (`PantryManagementE2ETest`, de otro worktree) ejecutándose en el AVD compartido; el reintento completo en verde es la evidencia válida (artefacto conocido de la sesión 41) |
| `gradlew assembleDebug` | exit **0** |

## Gate de cierre sobre el árbol definitivo

Tras resolver el hallazgo L1 (dos comentarios), el orquestador repitió el gate completo sobre el árbol que se iba a commitear:

| Comando | Resultado |
|---|---|
| `lint.ps1` (`lintDebug`) | exit **0**; BUILD SUCCESSFUL |
| `unit.ps1` (`testDebugUnitTest`) | exit **0**; **19/19**, 0 fallos (XML: `MyRecipesStateTest` 3, `RecipeSearchFilterTest` 4, `RecipeValidationTest` 8, `InMemoryRecipeRepositoryDeleteTest` 2, `MyRecipesSearchDebounceTest` 2) |
| `e2e.ps1` (`connectedDebugAndroidTest`) | exit **0**; **61/61**, 0 fallos, 0 saltados (14 m 9 s) |

**Incidencia del primer pase de este gate**: la primera ejecución de `e2e.ps1` sobre el árbol definitivo falló en `EditRecipeE2ETest.headerSaveIsAbsentWhilePublishedSaveIsDisabled` con `java.lang.AssertionError: Activity never becomes requested state "[DESTROYED]" (last lifecycle transition = "PAUSED")`: un fallo de ciclo de vida del emulador, no de aserción de la app. La repetición completa del suite quedó en **61/61** sin fallos, lo que confirma el diagnóstico de flake. Además, desde la validación del validador el único cambio de código eran dos líneas de comentario, incapaces de alterar el comportamiento. Se deja registrado por transparencia.

## Hallazgos (ninguno bloqueante)

- **L1 (Low) — resuelto en el commit de cierre**: `CreateRecipeE2ETest.kt` y `EditRecipeE2ETest.kt` citaban en un comentario clases ya renombradas (`MisRecetasLocalizationE2ETest`, `CrearRecetaE2ETest`). Las citas pasan a `MyRecipesLocalizationE2ETest` y `CreateRecipeE2ETest`; el comentario sigue en español (RF-6) y solo cambia el símbolo citado.
- **L2 (Low, sin cambio por decisión de alcance)**: literales de datos de prueba en español: directorios temporales `"crear-receta-e2e"` / `"editar-receta-e2e"`, ficheros de fixture `"mis-recetas-card-photo.png"` / `"editar-borrador.png"` e ids `"receta-editada-1"` / `"receta-que-no-existe"`. La spec exceptúa explícitamente los datos de prueba (caso límite «Datos de prueba → contenido visible, no identificador: no se traducen»), así que quedan **registrados aquí como datos de fixture aceptados** y no se modifican: no son identificadores y no afectan a ninguna verificación.
- **Nota de entorno**: el AVD `Pixel_10_Pro` se comparte con el worktree hermano `gestionar-alimentos-de-la-despensa`; un `e2e` concurrente puede abortar el pase. Se repitió completo en verde.

## Seguridad

Sin hallazgos. El diff no introduce credenciales, tokens ni PII; no cambia autenticación, red, permisos ni superficie de datos; `SystemPhotoAcquirer` solo renombra claves internas efímeras de `SavedStateRegistry`; las rutas de navegación son efímeras; esquema y SQL de Room intactos.

## Veredicto

**`accept`** — el diff es un renombrado puro (prueba de normalización idéntica en 18/20 ficheros Kotlin), el copy es/en y el esquema de Room están intactos, el harness está en verde (unit 19/19, e2e 61/61) y los dos hallazgos Low no bloquean. Recomendación al orquestador: `status` → `accepted` en `feature_list.json` resumiendo esta evidencia.
