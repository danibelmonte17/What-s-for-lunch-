---
feature: revisar-identificadores-en-ingles
category: tecnica
depends_on: [editar-receta]
status: ready
source: feature_list.json
---

# Spec: Revisar los identificadores en inglés

# PARTE A · CONTRATO

## Contexto y objetivo

La convención del repo (`AGENTS.md` §4, `P19`/`P25`: «documentación, specs y textos de producto en español; código, identificadores y mensajes técnicos en inglés») se cumple en la mayor parte del código (`domain/model/`, `data/`, `ui/theme/`, `ui/nav/` salvo rutas), pero no en la capa de UI ni en sus tests: `ui/misrecetas/`, `ui/crearreceta/`, `domain/misrecetas/`, `domain/crearreceta/`, las clases `MisRecetas*` y `CrearReceta*`, las funciones `crearReceta()`/`editarReceta()`, las rutas `"mis_recetas"`/`"crear_receta"`/`"escanear_despensa"`, los valores de `testTag` (`"mis_recetas_*"`, `"crear_receta_*"`), las claves internas de `SystemPhotoAcquirer`, casi un centenar de claves de recurso (`mis_recetas_*`, `crear_receta_*`, `momento_*`, `dificultad_*`) y las clases de test `MisRecetas*`, `CrearReceta*`, `EditarReceta*`, `EliminarReceta*` y `BuscarYFiltrarRecetas*` siguen nombrados en español.

Esta feature audita `app/src` (main, test y androidTest), renombra esos identificadores y ficheros a inglés y verifica que no cambia ni el comportamiento ni el esquema de Room. Es un refactor de pura nomenclatura: los textos visibles de `strings.xml`, los comentarios del código y la documentación siguen en español.

## Actores y permisos

- **Agente implementador**: renombra identificadores en `app/src/**` (los tres source sets) y sus referencias; no cambia comportamiento, copy ni documentación salvo la aclaración de `CONTEXT.md` prevista en §Impacto en documentación duradera.
- **Usuario** (único rol del MVP): no hay cambio visible; la app se comporta exactamente igual.

## Historia

Cuando mantengo el código de la app,
quiero que sus identificadores estén en inglés,
para poder trabajar según la convención del repo (`P25`) sin mezclar idiomas en el código.

## Requisitos funcionales

- RF-1: EL SISTEMA documenta un inventario de los identificadores en español de `app/src` (main, test y androidTest) — clases, interfaces, objetos, funciones, métodos, propiedades, variables, constantes, claves de recurso, valores de `testTag`, rutas de navegación, claves internas, paquetes y ficheros — con su traducción inglesa (auditoría del `step` 1; la tabla de §Enfoque técnico es el contrato para los clusters ya localizados).
- RF-2: CUANDO se renombra un identificador, EL SISTEMA actualiza todas sus referencias — imports, usos en código, referencias `R.string` y tests — en los tres source sets, sin romper la compilación.
- RF-3: EL SISTEMA nombra en inglés las claves de recurso de `values/strings.xml` y `values-en/strings.xml` (`mis_recetas_*` → `my_recipes_*`, `crear_receta_*` → `create_recipe_*`, `momento_*` → `meal_moment_*`, `dificultad_*` → `difficulty_*`), manteniendo pares de clave idénticos en ambos ficheros.
- RF-4: EL SISTEMA traduce a inglés los valores de `testTag`, las rutas de navegación y las claves internas de estado (`SavedStateRegistry` de `SystemPhotoAcquirer`), y actualiza los tests que los usan en el mismo cambio.
- RF-5: EL SISTEMA nombra en inglés los paquetes y los ficheros de `app/src` (main y sus espejos en test y androidTest); cada fichero acompañe a su clase principal.
- RF-6: EL SISTEMA conserva en español los valores de los textos visibles de `values/strings.xml` (y en inglés los de `values-en/strings.xml`), los comentarios del código y la documentación.
- RF-7: EL SISTEMA conserva intacto el esquema de Room: nombres de tablas, columnas, claves foráneas, consultas de los DAO y persistencia de enums no cambian (el contenido de `data/local/` ya está en inglés y no se toca).
- RF-8: EL SISTEMA conserva el comportamiento de la app: `lint`, `unit` y `e2e` pasan tras el renombrado con los mismos recuentos de la sesión 41 (unit 19/19, e2e 61/61) y `assembleDebug` sin errores.

## Requisitos no funcionales

- NFR-1: Documentación y specs en español, con los identificadores de código citados en inglés (`P25`).
- NFR-2: Equivalencia única de nomenclatura: cada término de `CONTEXT.md` tiene un único nombre en el código (tabla de §Enfoque técnico), de modo que «Mis Recetas» sea siempre `MyRecipes` y no convivan `MyRecipes` y `MisRecetas`.

## Casos límite

- Identificadores mixtos (`MisRecetasScreen`, `BuscarYFiltrarRecetasE2ETest`) → traducción completa (`MyRecipesScreen`, `SearchAndFilterRecipesE2ETest`), no solo el trozo español → RF-2, NFR-2.
- Claves `<string name="…">` → son identificadores (constantes `R.string`), no textos: se traducen en los dos `strings.xml` y en todas las referencias; los **valores** (textos es/en) no cambian → RF-3, RF-6.
- Valores de `testTag`, de rutas (`"mis_recetas"`, `"crear_receta?recipeId={recipeId}"`, `"escanear_despensa"`) y de claves internas (`crear_receta_pick_gallery`, `crear_receta_request_camera`, `crear_receta_take_photo`) → son literales pero identificadores internos, invisibles para el usuario: se traducen y los tests se actualizan en el mismo cambio → RF-4.
- Datos de prueba (títulos de receta, nombres de Ingrediente, textos de fixtures) → contenido visible, no identificador: no se traducen → RF-6.
- Nombres ya en inglés por más que su semántica venga del glosario (`RecipeSearchFilter`, `RecipeValidation`, `MealMoment`, `RecipeStatus.DRAFT`, `MisRecetasState.Empty`, `CrearRecetaNotice.AI_KEY_MISSING`, `E2eDatabase`, `AppLaunchE2ETest`…) → no se renombran; solo se les actualiza el import si cambia el paquete → RF-2, RF-8.
- Paquetes españoles (`ui.misrecetas`, `ui.crearreceta`, `domain.misrecetas`, `domain.crearreceta`) → se mueven de directorio en los tres source sets; los paquetes van en minúsculas concatenadas, como hoy (`myrecipes`, `createrecipe`) → RF-5.
- Comentarios del código y documentación con términos del glosario en español («Mis Recetas», «Borrador»…) → intactos: `CONTEXT.md` manda en docs, textos de UI y comentarios → RF-6.
- Evidencia histórica (`docs/evidence/**`) y specs cerradas que citan nombres antiguos (`CrearRecetaE2ETest`, `MisRecetasCardsE2ETest`…) → registro histórico: no se reescriben → RF-6.
- Room: si el inventario localizara algún identificador español en una propiedad anotada por Room (no es el caso hoy), el nombre de columna se fijaría con `@ColumnInfo(name = …)` para conservar el esquema; en cualquier caso `data/local/` y el SQL de los DAO no cambian → RF-7.
- Identificador de producto y textos (`app_name`, «What's for Lunch?») → intactos → RF-6.

## Fuera de alcance

- Traducir comentarios o documentación al inglés; cambiar el copy es/en validado; añadir idiomas.
- Cambiar el esquema de Room, crear migraciones ni tocar datos persistidos.
- Cambios de comportamiento, de UI, de navegación o de tests más allá de la nomenclatura (el diff debe ser renombres, movimientos de paquete y referencias).
- Refactors de estilo, arquitectura, rendimiento o formato; renombrar identificadores ya en inglés por preferencia.
- Reescribir la evidencia histórica (`docs/evidence/**`) ni las specs de otras features (`docs/specs/**` ajenas).
- Añadir una regla automatizada (detekt / custom lint check) que impida reintroducir identificadores en español: queda como posible feature futura; aquí solo se audita y se renombra.
- `uxdocs/`, `DESIGN.md`, `.opencode/`, los scripts del harness (`init`/`lint`/`unit`/`e2e`) y `gradle/`: no contienen identificadores afectados y no se tocan.

## Criterios de finalización

- verify 1 (revisión): la auditoría de `app/src` no deja clases, métodos, variables, constantes, paquetes ni ficheros con nombres en español fuera de los textos visibles y los comentarios (comandos del §Plan de verificación en 0 resultados en posiciones de identificador).
- verify 2 (harness): `init`/`lint`/`unit`/`e2e` en verde con los recuentos de la sesión 41 (unit 19/19, e2e 61/61), `assembleDebug` sin errores y el comportamiento ya validado intacto (RF-7, RF-8).
- Evidencia ≤ 4 artefactos y solo de esta feature.

## Dudas abiertas

Ninguna abierta. Tres decisiones asumidas sin consulta (registro conservado, en la línea de `editar-receta`):

- Q-revisar-identificadores-en-ingles-1 [decisión asumida]: ¿las claves de `strings.xml`, los valores de `testTag`/rutas y las claves internas entran en «identificadores»? — Sí: `verify` 1 habla de «constantes» y solo exceptúa «los textos visibles y los comentarios»; los valores de testTag ya se habían tratado como identificadores de código. Si el usuario prefiere conservar las claves de recurso en español, será una reducción de alcance explícita que habrá que registrar antes de implementar (RF-3).
- Q-revisar-identificadores-en-ingles-2 [decisión asumida]: conflicto aparente entre `CONTEXT.md` («los términos se usan tal cual en … el código») y `P25`/`AGENTS.md` §4 («código, identificadores … en inglés») → mandan `P25` y `AGENTS.md` para los identificadores; el glosario se usa tal cual en documentación, textos de UI y comentarios, y el código usa la equivalencia inglesa de §Enfoque técnico (NFR-2). Se aclara en `CONTEXT.md`.
- Q-revisar-identificadores-en-ingles-3 [decisión asumida]: nombres objetivo de paquetes y clases → equivalencia literal del glosario (`MyRecipes`, `CreateRecipe`, `Pantry`…), sin reorganizar la estructura de carpetas.

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `step` 1 (auditar y listar) | verify 1 (inventario revisado) | inventario en el informe |
| RF-2 | `step` 2 (renombrar y actualizar referencias) | verify 1 + compilación | informe + grep |
| RF-3 | `step` 2 + notas (claves de recurso) | verify 1 + `lint` sin `MissingTranslation` | informe |
| RF-4 | `step` 2 (testTags y tests afectados) | verify 1 + E2E que usan los testTag | informe |
| RF-5 | `step` 2 (paquetes y ficheros) | verify 1 (sin directorios ni ficheros en español) | informe |
| RF-6 | `step` 3 (mantener en español textos, comentarios y docs) | verify 1 (valores de `strings.xml` y comentarios intactos) | informe |
| RF-7 | `step` 4 (conservar el esquema de Room) | verify 2 (E2E con Room real) + diff sin cambios en `data/local/` | informe |
| RF-8 | `verification` 2 (harness verde y comportamiento intacto) | verify 2 | salidas de `lint`/`unit`/`e2e` |

## Registro de iteraciones

- Redacción inicial (2026-10-20): decisiones Q-1 a Q-3 asumidas y registradas en §Dudas abiertas; tabla de renombres fijada en §Enfoque técnico como contrato para el implementador.

# PARTE B · PLAN

## Investigación del repositorio

### Archivos inspeccionados

- `feature_list.json` — entrada de la feature (`area: tecnica`, `depends_on: [editar-receta]` en `accepted`).
- `AGENTS.md`, `CONTEXT.md`, `docs/build-brief.md`, `docs/technical-discovery.md` — convención `P19`/`P25`, glosario y comandos del harness.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/` (`MisRecetasScreen.kt` con `MisRecetasTestTags`, `MisRecetasViewModel.kt` con `MisRecetasUiState`, `MisRecetasRoute.kt`) y `ui/crearreceta/` (`CrearRecetaScreen.kt` con `CrearRecetaTestTags`, `CrearRecetaViewModel.kt` con `CrearRecetaUiState`/`CrearRecetaNotice`, `CrearRecetaRoute.kt` con `toActions`) — el grueso de los identificadores en español.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `MIS_RECETAS`/`CREAR_RECETA`/`ESCANEAR_DESPENSA`, valores de ruta `"mis_recetas"`/`"crear_receta?recipeId={recipeId}"`/`"escanear_despensa"` y funciones `crearReceta()`/`editarReceta(id)`; `ProvisionalScreen.kt` ya en inglés.
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/` (`MisRecetasState.kt`: `MisRecetasState`, `misRecetasStateOf`; `RecipeSearchFilter.kt` ya en inglés) y `domain/crearreceta/` (`RecipeValidation.kt` ya en inglés) — solo el paquete y `MisRecetasState` son españoles.
- `app/src/main/java/com/whatsforlunch/app/domain/model/` — modelo y enums ya en inglés (`Recipe`, `MealMoment`, `Difficulty`, `RecipeStatus`, `RecipeOrigin`, `RecipeIngredient`, `RecipeStep`): no se tocan.
- `app/src/main/java/com/whatsforlunch/app/data/local/` — esquema Room ya en inglés: tablas `recipes`/`ingredients`/`steps`, columnas derivadas de propiedades inglesas (`recipeId`, `prepMinutes`…), DAO con SQL en inglés (`RecipeDao.kt`): no se toca (RF-7).
- `app/src/main/java/com/whatsforlunch/app/data/photo/SystemPhotoAcquirer.kt` — claves internas `crear_receta_pick_gallery|request_camera|take_photo`.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — 95 claves; en español: `mis_recetas_*`, `crear_receta_*`, `momento_desayuno|almuerzo|cena|postre`, `dificultad_facil|media|dificil`; ya en inglés: `app_name`, `action_*`. Valores es/en validados (no cambian).
- `app/src/test/java/com/whatsforlunch/app/domain/MisRecetasStateTest.kt`, `.../ui/misrecetas/MisRecetasSearchDebounceTest.kt`, `RecipeSearchFilterTest.kt`, `RecipeValidationTest.kt`, `data/repository/InMemoryRecipeRepositoryDeleteTest.kt` — unit (19 tests, sesión 41).
- `app/src/androidTest/java/com/whatsforlunch/app/` — `AppLaunchE2ETest.kt`, `CrearRecetaE2ETest.kt` (contiene también `CrearRecetaAppE2ETest` y `MisRecetasCardsE2ETest`), `EditarRecetaE2ETest.kt` (también `EditarRecetaRouteE2ETest` y `EditarRecetaAppE2ETest`), `EliminarRecetaE2ETest.kt`, `BuscarYFiltrarRecetasE2ETest.kt`, `MisRecetasLocalizationE2ETest.kt`, `E2eDatabase.kt`: usan `MisRecetasTestTags.*`/`CrearRecetaTestTags.*` por constante (el cambio de valores de tag queda contenido), `R.string.*` y helpers que devuelven `MisRecetasViewModel`/`CrearRecetaViewModel`.
- `e2e.ps1`/`init.ps1` (y `.sh`) — los scripts del harness no citan clases de test ni identificadores de la app: el renombrado no los rompe.
- `docs/domain-model.md` — no cita identificadores Kotlin; sin cambios.
- `PROGRESS.md` — recuentos de referencia de la sesión 41: unit **19/19**, e2e **61/61** (AVD `Pixel_10_Pro`, API 37).

### Patrones existentes a seguir

- Convención `P25`/`AGENTS.md` §4: identificadores y mensajes técnicos en inglés; documentación y textos de producto en español; copy es/en en `strings.xml`.
- testTags centralizados en objetos `*TestTags` y referenciados por constante en los tests; renombrar sus valores no exige buscar literales sueltos (salvo los que la auditoría confirme).
- El compilador como verificador del renombrado: referencia rota = error de compilación; `lint`/`unit`/`e2e` como red de seguridad de comportamiento.
- Estructura de spec con frontmatter + `PARTE A`/`PARTE B` (precedentes `editar-receta.md`, `sincronizar-spec-de-crear-receta.md`).

### Carencias actuales

- No existe la auditoría completa: §Enfoque técnico recoge los clusters localizados en esta investigación; el implementador debe completar el inventario con un barrido de los tres source sets (T1) porque puede haber identificadores sueltos no cubiertos por los patrones de búsqueda usados aquí.
- No hay regla automatizada (detekt / custom lint) que impida volver a introducir identificadores en español; la verificación es la revisión manual con grep del verify 1 (añadirla queda fuera de alcance).

## Enfoque técnico

Refactor de nomenclatura en tres pasos, con el compilador y la suite como verificación de que nada más cambia:

1. **Auditar (T1)**: barrer `app/src/main`, `app/src/test` y `app/src/androidTest` en busca de identificadores españoles (clases, funciones, métodos, propiedades, variables, constantes, claves de recurso, valores de tag/ruta/clave interna, paquetes y ficheros) y completar la tabla de renombres (inventario = evidencia de RF-1). Los positivos en comentarios y literales de texto se descartan a mano.
2. **Renombrar (T2-T4)**: aplicar los renombres con refactor del IDE (o `git mv` + edición de imports) para que las referencias, los imports y los tests se actualicen en bloque; los tests unitarios y E2E se renombran con sus ficheros y sus métodos con trozos españoles.
3. **Verificar (T5-T8)**: comprobar que solo cambió nomenclatura (`git diff`), que los textos de `strings.xml` y los comentarios siguen en español, que `data/local/` no aparece en el diff (RF-7) y ejecutar el harness completo.

**Equivalencias del glosario (`CONTEXT.md` → identificador inglés, NFR-2)**: Mis Recetas → `MyRecipes` · Receta → `Recipe` · Crear receta → `CreateRecipe` · Editar receta → `EditRecipe` · Eliminar receta → `DeleteRecipe` · Borrador → `Draft` · Publicada → `Published` · Despensa → `Pantry` · Escanear/Escáner → `Scan`/`Scanner` · Lista de la Compra → `ShoppingList` · Sugerencia → `Suggestion` · Coincidencia → `Match` · Momento culinario → `MealMoment` · Raciones → `Servings` · Ingrediente → `Ingredient` · Paso → `Step` · Foto → `Photo` · Favorito → `Favorite` · Historia → `Story`.

**Paquetes y ficheros (RF-5)** — en main y en sus espejos de test/androidTest: `ui.misrecetas` → `ui.myrecipes` · `ui.crearreceta` → `ui.createrecipe` · `domain.misrecetas` → `domain.myrecipes` · `domain.crearreceta` → `domain.createrecipe` (minúsculas concatenadas, como hoy).

**Clases y funciones (RF-2)** — contrato del inventario conocido (T1 lo completa):

| Actual (fichero) | Objetivo (fichero) |
|---|---|
| `MisRecetasScreen.kt`: `MisRecetasScreen`, `EmptyMisRecetas`, `MisRecetasTestTags` (+ `filterChip`, `filterChipIcon`) | `MyRecipesScreen.kt`: `MyRecipesScreen`, `EmptyMyRecipes`, `MyRecipesTestTags` |
| `MisRecetasRoute.kt`: `MisRecetasRoute` | `MyRecipesRoute.kt`: `MyRecipesRoute` |
| `MisRecetasViewModel.kt`: `MisRecetasViewModel`, `MisRecetasUiState` | `MyRecipesViewModel.kt`: `MyRecipesViewModel`, `MyRecipesUiState` |
| `domain/misrecetas/MisRecetasState.kt`: `MisRecetasState`, `misRecetasStateOf` | `domain/myrecipes/MyRecipesState.kt`: `MyRecipesState`, `myRecipesStateOf` |
| `CrearRecetaScreen.kt`: `CrearRecetaScreen`, `CrearRecetaActions`, `CrearRecetaTestTags`, ext. `CrearRecetaNotice.messageRes` | `CreateRecipeScreen.kt`: `CreateRecipeScreen`, `CreateRecipeActions`, `CreateRecipeTestTags`, `CreateRecipeNotice.messageRes` |
| `CrearRecetaRoute.kt`: `CrearRecetaRoute`, `CrearRecetaViewModel.toActions` | `CreateRecipeRoute.kt`: `CreateRecipeRoute`, `CreateRecipeViewModel.toActions` |
| `CrearRecetaViewModel.kt`: `CrearRecetaViewModel`, `CrearRecetaUiState`, `CrearRecetaNotice` | `CreateRecipeViewModel.kt`: `CreateRecipeViewModel`, `CreateRecipeUiState`, `CreateRecipeNotice` |
| `domain/misrecetas/RecipeSearchFilter.kt`, `domain/crearreceta/RecipeValidation.kt` | solo cambio de paquete (nombres ya en inglés) |
| `AppNavHost.kt`: `crearReceta()`, `editarReceta(id)`, `MIS_RECETAS`, `CREAR_RECETA`, `ESCANEAR_DESPENSA` | `createRecipe()`, `editRecipe(id)`, `MY_RECIPES`, `CREATE_RECIPE`, `SCAN_PANTRY` |

**Valores de identificador interno (RF-3, RF-4)**:

- Rutas de navegación: `"mis_recetas"` → `"my_recipes"`, `"crear_receta?recipeId={recipeId}"` → `"create_recipe?recipeId={recipeId}"`, `"escanear_despensa"` → `"scan_pantry"` (sin datos persistidos que dependan de ellas; el estado de navegación solo vive en memoria/saved state efímero).
- Valores de `testTag`: prefijos `mis_recetas_*` → `my_recipes_*` y `crear_receta_*` → `create_recipe_*` (nombres de constante ya en inglés; `MisRecetasTestTags`/`CrearRecetaTestTags` → `MyRecipesTestTags`/`CreateRecipeTestTags`).
- `SystemPhotoAcquirer`: `crear_receta_pick_gallery|request_camera|take_photo` → `create_recipe_pick_gallery|request_camera|take_photo`.
- Claves de recurso (los dos `strings.xml` con la misma clave y todas las referencias `R.string`, también en tests): `mis_recetas_*` → `my_recipes_*`, `crear_receta_*` → `create_recipe_*`, `momento_desayuno|almuerzo|cena|postre` → `meal_moment_breakfast|lunch|dinner|dessert`, `dificultad_facil|media|dificil` → `difficulty_easy|medium|hard`; `app_name` y `action_*` sin cambios. **Los valores de los textos es/en no cambian** (RF-6).

**Tests (RF-2, RF-4)**: ficheros y clases — `MisRecetasStateTest` → `MyRecipesStateTest`, `MisRecetasSearchDebounceTest` → `MyRecipesSearchDebounceTest`, `MisRecetasLocalizationE2ETest` → `MyRecipesLocalizationE2ETest`, `CrearRecetaE2ETest`/`CrearRecetaAppE2ETest`/`MisRecetasCardsE2ETest` → `CreateRecipeE2ETest`/`CreateRecipeAppE2ETest`/`MyRecipesCardsE2ETest`, `BuscarYFiltrarRecetasE2ETest` → `SearchAndFilterRecipesE2ETest`, `EditarRecetaE2ETest`/`EditarRecetaRouteE2ETest`/`EditarRecetaAppE2ETest` → `EditRecipeE2ETest`/`EditRecipeRouteE2ETest`/`EditRecipeAppE2ETest`, `EliminarRecetaE2ETest` → `DeleteRecipeE2ETest`; métodos con español conocidos: `appOpensInMisRecetasWithEmptyState` → `appOpensInMyRecipesWithEmptyState`, `savedDraftAppearsInMisRecetas` → `savedDraftAppearsInMyRecipes`, `editedTitleAppearsInMisRecetas` → `editedTitleAppearsInMyRecipes` (T1 completa el resto). Sin cambios: `AppLaunchE2ETest`, `E2eDatabase`, `RecipeSearchFilterTest`, `RecipeValidationTest`, `InMemoryRecipeRepositoryDeleteTest`.

## Archivos esperados

- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/`, `ui/createrecipe/`, `domain/myrecipes/`, `domain/createrecipe/` — renombrar (movidos desde `misrecetas`/`crearreceta`); contenido modificado solo en nombres.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt`, `data/photo/SystemPhotoAcquirer.kt`, `di/AppModule.kt` — modificar; rutas, claves internas y referencias Koin de las clases renombradas.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — modificar; solo los atributos `name` (valores intactos).
- `app/src/test/java/com/whatsforlunch/app/**`, `app/src/androidTest/java/com/whatsforlunch/app/**` — renombrar ficheros/clases/métodos y actualizar referencias (imports, `R.string`, testTags).
- `app/src/main/java/com/whatsforlunch/app/data/local/**` — **sin cambios** (RF-7); su aparición en el diff es un fallo de alcance.
- `CONTEXT.md` — modificar (aclaración, ver §Impacto en documentación duradera).
- `docs/validations/revisar-identificadores-en-ingles.md` — crear al validar (evidencia).

## Impacto visual

- UI involved: no — refactor de nomenclatura sin cambios visibles: ni copy, ni composición, ni navegación (los `testTag` y las rutas son internos).
- Design source: no aplica; `DESIGN.md` y `uxdocs/` no se tocan.
- Screens or states affected: ninguna (las pantallas Mis Recetas y Formulario de Receta se ven y se comportan igual).
- New design artifact required: no.

## Impacto en documentación duradera

- `ARCHITECTURE.md` / `CONSTRAINTS.md`: not needed — no existen; las capas `ui`/`domain`/`data` y sus dependencias no cambian.
- `AGENTS.md`: not needed — ya fija «código, identificadores y mensajes técnicos en inglés»; el flujo y los comandos no cambian.
- `CONTEXT.md`: **update** (una aclaración breve): el preámbulo dice que los términos del glosario «se usan tal cual en … el código», lo que choca con `P25`; se precisa que se usan tal cual en documentación, textos de UI y comentarios, y que los identificadores del código usan la equivalencia inglesa (referencia a esta spec o tabla resumida). Sin cambiar ningún término del glosario.
- Otros: `docs/domain-model.md` — not needed (no cita identificadores Kotlin; reglas `RD##` intactas). `docs/technical-discovery.md` — not needed (stack y comandos sin cambios). `docs/evidence/**` y specs de otras features — not needed: registro histórico que no se reescribe (Fuera de alcance).

## Plan y tareas

- [x] T1: inventario de la auditoría de `app/src` (main, test, androidTest) con la tabla de traducciones completa (amplía la de §Enfoque técnico). (RF-1)
- [x] T2: renombrar clases, funciones, métodos, propiedades, variables y constantes (incluidas las claves de `strings.xml` en `values/` y `values-en/` con sus referencias `R.string`). (RF-2, RF-3)
- [x] T3: traducir valores de `testTag`, rutas y claves internas y actualizar los tests que los usan. (RF-4)
- [x] T4: mover paquetes y renombrar ficheros en los tres source sets. (RF-5)
- [x] T5: comprobar que los textos visibles es/en, los comentarios y la documentación siguen en español. (RF-6)
- [x] T6: comprobar el diff: solo nomenclatura y referencias; `data/local/` intacto (esquema de Room). (RF-7, RF-8)
- [x] T7: aclaración de `CONTEXT.md` según §Impacto en documentación duradera. (NFR-1, NFR-2)
- [x] T8: harness completo verde (`init`/`lint`/`unit`/`e2e` + `assembleDebug`) y evidencia capturada. (RF-8)

## Plan de verificación

- verify 1 (revisión) — `app/src` sin identificadores en español fuera de textos visibles y comentarios. Comandos de barrido (equivalentes admitidos) y revisión manual de cada resultado (los comentarios y los literales de texto en español dan positivo y se descartan):
  - `git grep -inE '(class|object|interface|fun|val|var|enum class|package)[[:space:]]+[A-Za-z_]*(receta|recet|borrador|despensa|escaneo|escaner|escanear|compra|pinche|sugerencia|coincidencia|ingrediente|pasos|raciones|dificultad|momento|foto|favorito|titulo|historia|caloria|preparacion|coccion|guardar|eliminar|editar|buscar|filtrar|cocina|aviso|vacio)' -- 'app/src'` → sin identificadores españoles entre los resultados.
  - `git grep -nE 'name="[a-z0-9_]*(receta|momento|dificultad)' -- 'app/src/main/res'` → 0 resultados (claves de recurso).
  - `git grep -nE '"(mis_recetas|crear_receta|escanear_despensa|momento_|dificultad_|crear_receta_)' -- 'app/src'` → 0 resultados (valores de tag/ruta/clave interna).
  - `git ls-files app/src | grep -iE '(receta|misrecetas|crearreceta|editar|eliminar|buscar|momento|dificultad)'` → sin ficheros ni directorios en español.
  - `values/strings.xml` y `values-en/strings.xml`: mismas claves que antes traducidas, **valores byte a byte intactos** (diff solo en atributos `name`).
- verify 2 (harness y comportamiento intacto) — sobre emulador API 30+ (`Pixel_10_Pro`, API 37 según `PROGRESS.md`):
  - `./init.ps1` / `./init.sh` → exit 0 (gate de arranque estándar no bloqueante; no se modifica y no arranca procesos duraderos; artefactos conocidos: carrera del `pidof` en `init.sh` y falso exit 1 por stderr de `adb shell monkey` en PowerShell 5.1 → ejecutar con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
  - `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0, `testDebugUnitTest` **19/19** (los mismos tests de la sesión 41 con sus nombres nuevos) y `lintDebug` sin errores ni `MissingTranslation` por las claves renombradas.
  - `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) → exit 0, **61/61** con los nombres de clase nuevos; la suite existente cubre crear/editar/eliminar/buscar/arrancar y la localización es/en, por lo que su verde tras el renombrado es exactamente la comprobación de que el comportamiento no cambió (incluido el esquema de Room, RF-7).
  - `./gradlew assembleDebug` → sin errores.
  - Diff de alcance: `git diff --name-status` (y `git status`) solo muestra renombres/movimientos, cambios de referencias y la aclaración de `CONTEXT.md`; nada en `data/local/`, `gradle/`, scripts del harness ni `uxdocs/`.
- E2E persistente: **no se añade ni se modifica cobertura funcional** — la feature no cambia comportamiento observable, y la suite existente (61 tests instrumentados) se ejecuta sin excepción como verificación de regresión; renombrar clases/métodos de test es parte del alcance (RF-2, RF-4), no cobertura nueva.

## Evidencia a capturar

- `docs/validations/revisar-identificadores-en-ingles.md` (1 artefacto; sin capturas: no hay UI) con: el inventario completo de la auditoría (tabla de renombres), la verificación de verify 1 (salidas de los comandos de grep), las salidas de `init`/`lint`/`unit` (19/19)/`e2e` (61/61)/`assembleDebug`, la comprobación de que `data/local/` y los valores de `strings.xml` no cambian, y la verificación por RF de la tabla de Trazabilidad. Se resume en `evidence` de `feature_list.json` al aceptar. Presupuesto ≤ 4 artefactos y solo de esta feature.

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec (solo nomenclatura y referencias).
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] verify 1: la auditoría deja `app/src` sin identificadores en español fuera de textos visibles y comentarios (comprobación propia con los comandos del Plan de verificación, no la autopercepción del implementador).
- [x] verify 2: `init`/`lint`/`unit` (19/19)/`e2e` (61/61) en verde y `assembleDebug` sin errores.
- [x] Esquema de Room intacto: `data/local/` sin cambios en el diff y la suite E2E (que usa Room real) pasa.
- [x] Textos visibles es/en, comentarios y documentación siguen en español; `CONTEXT.md` solo lleva la aclaración prevista.
- [x] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [x] UI: no aplica — sin cambios visibles (comprobable por la ausencia de cambios en `res/values*` salvo atributos `name`).
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento, cobertura funcional nueva ni trabajo de otras features.
