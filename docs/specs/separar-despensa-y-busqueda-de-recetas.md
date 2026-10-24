# Feature Implementation Spec: Separar la Despensa de la búsqueda de recetas

## Source Feature

- `id`: separar-despensa-y-busqueda-de-recetas
- `area`: funcional
- `depends_on`: mostrar-recetas-compatibles, sugerir-recetas-con-ia, escanear-despensa-con-ia (las tres `accepted`)
- `status`: not_started (con spec; planificada el 2026-10-24)
- `source`: `feature_list.json`

## Goal

Separar dos responsabilidades que hoy conviven en el Escáner: el Escáner queda dedicado **solo** a llenar la Despensa (alta manual, cámara, chips, cantidades y guardado), y la consulta «qué cocino con lo que tengo» se muda a una pantalla propia **«Recetas con mi Despensa»** (ruta `pantry_recipes`) que lista las Recetas Compatibles (RD02) y ofrece la sugerencia con IA. La entrada es una acción en la top bar de Mis Recetas.

Es un **cambio de producto sobre features aceptadas**: sustituye RF-MRC5 de `mostrar-recetas-compatibles` (la sección deja de vivir dentro del Escáner) y conserva RF-MRC1 a RF-MRC4 y RF-MRC6 a RF-MRC10 en la nueva pantalla.

## Non-Goals

- Bottom navigation ni cambio de la arquitectura de navegación existente.
- Mover la cámara o el alta manual de Alimentos a la pantalla Despensa (que no cambia).
- Tocar `calculateMatch`, `MatchBadge`, el umbral 85 % (RD02), Room ni el flujo de IA (`ai_suggestions`, cliente, parser).
- Aceptar/descartar Sugerencias (`aceptar-sugerencia-de-receta`).
- Modificar el guardado del Escaneo (RD13), el diálogo de salida, los avisos RD05 ni el máximo 99.

## Job Story

When tengo la Despensa ya llenada y quiero decidir qué cocinar,
I want to consultar en un solo sitio las recetas que encajan con mi Despensa y pedir sugerencias a la IA,
so I can decidir sin tener que pasar por el flujo de escaneo de la Despensa.

## Users And Permissions

- Usuario único sin roles ni autenticación: toda la app es local, sin permisos nuevos ni red salvo la ya existente de IA.

## Requisitos funcionales (prefijo `RF-SDR#`)

- **RF-SDR1**: existe la pantalla «Recetas con mi Despensa» (ruta `pantry_recipes`) con cabecera, vuelta atrás, lista de Recetas Compatibles, estado vacío y el botón «Sugerir receta con IA» (RF-MRC5 sustituida; RF-MRC6 y RF-MRC8 se conservan aquí).
- **RF-SDR2**: la lista usa `compatibleRecipes()` tal cual (RF-MRC1 a RF-MRC4: ≥ 85 % sobre el porcentaje redondeado, Borradores y Publicadas, sin obligatorios excluidos, orden Coincidencia desc → título normalizado asc → id asc) y las mismas tarjetas (badge, miniatura, título, «X de Y ingredientes en la Despensa»).
- **RF-SDR3**: el cálculo sigue siendo reactivo contra la Despensa persistida (RF-MRC9, RF-MRC10): al añadir, editar o eliminar Alimentos o al crear, editar o eliminar Recetas, la lista se actualiza sin reiniciar la app.
- **RF-SDR4**: el botón «Sugerir receta con IA» está siempre visible (también con Recetas compatibles, RF-MRC8) y se **habilita solo con la Despensa persistida no vacía** (D2); habilitado abre `ai_suggestions`, con vuelta atrás a esta pantalla.
- **RF-SDR5**: tocar una tarjeta abre el Detalle (`recipe_detail/{id}`) y al volver se regresa a «Recetas con mi Despensa» (RF-MRC7).
- **RF-SDR6**: la entrada es una acción en la top bar de Mis Recetas (D3); el FAB de cámara y la pantalla Despensa quedan intactos.
- **RF-SDR7**: el Escáner ya no muestra «Recetas Compatibles» ni «Sugerir receta con IA»: desaparecen la sección, sus callbacks, `RecipeRepository` de `ScannerViewModel` y los testTags asociados; conserva alta manual, cámara, chips, cantidades, guardado (RD13), duplicados, avisos y diálogo de salida.
- **RF-SDR8**: los textos viven en `values/strings.xml` y `values-en/strings.xml` con claves `recommendations_*` (renombrado de `scanner_compatible_*`) y paridad exacta es/en, incluidos título de pantalla y accesibilidad de la acción de entrada.

## Acceptance Scenarios

### Scenario 1: Lista ordenada con badges y desglose (verify de `feature_list.json`)

Given «Tortilla de patatas» al 100 %, «Borrador compatible» al 100 % y «Pechuga al horno» al 85 % en Mis Recetas y sus Alimentos en la Despensa
When abro «Recetas con mi Despensa» desde la acción de la top bar de Mis Recetas
Then la pantalla muestra su título, las tarjetas ordenadas (100 % antes que 85 %) con «100% Match»/«85%» y la línea «X de Y ingredientes en la Despensa»

### Scenario 2: La tarjeta abre el Detalle y al volver se regresa a la nueva pantalla

Given la pantalla «Recetas con mi Despensa» con al menos una Receta compatible
When toco su tarjeta
Then se abre el Detalle de esa Receta y, al volver, se regresa a «Recetas con mi Despensa» con la lista intacta

### Scenario 3: Sugerencia con IA habilitada solo con Despensa

Given la Despensa persistida con al menos un Alimento
When pulso «Sugerir receta con IA»
Then se abre la pantalla real de Sugerencias y se puede volver a «Recetas con mi Despensa»

### Scenario 4: Sin Despensa no se puede pedir sugerencia

Given la Despensa vacía
When abro «Recetas con mi Despensa»
Then la lista muestra el estado vacío y el botón «Sugerir receta con IA» aparece deshabilitado y no navega

### Scenario 5: El Escáner ya solo escanea

Given cualquier estado del Escáner (con y sin chips, con cámara y en modo manual)
When recorro la pantalla del Escáner
Then no aparecen «Recetas Compatibles» ni «Sugerir receta con IA» (0 hits de `scanner_compatible_*`) y guardar Alimentos sigue navegando a la Despensa

### Scenario 6: Actualización reactiva sin reiniciar

Given la pantalla «Recetas con mi Despensa» abierta
When añado o elimino un Alimento de la Despensa, o creo, edito o elimino una Receta
Then la lista se recalcula en caliente (aparece/desaparece la Receta afectada) sin reiniciar la app ni reabrir la pantalla

### Scenario 7: Textos en inglés

Given el idioma del sistema en inglés
When abro la nueva pantalla y su acción de entrada en Mis Recetas
Then se ven «Recipes with my Pantry», «Compatible Recipes», «Suggest a recipe with AI» y el resto de textos en inglés

### Scenario 8: Estado de la feature heredada

Given una Receta con 84 % de Coincidencia o una Receta sin ingredientes obligatorios
When abro la nueva pantalla
Then ninguna de las dos aparece (RF-MRC1, RF-MRC4 sin cambios)

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — `CompatibleRecipesSection`, `CompatibleRecipeCard`, `CompatibleRecipeThumbnail`, `rememberBitmap` (solo usado por la miniatura), testTags `ScannerTestTags.COMPATIBLE_*` (líneas ~103-119, 253-260, 477-619) y callbacks `onOpenRecipe`/`onSuggestWithAi`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — constructor con `RecipeRepository`, campo `recipes`, `ScannerUiState.compatibleRecipes` y `updateRecipes`/`updatePantryItems` (líneas 46, 63, 69-71, 310-327). El resto (chips, holder, guardado RD13) queda intacto.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — firma con `onOpenRecipe`/`onSuggestWithAi` que hay que retirar.
- `app/src/main/java/com/whatsforlunch/app/domain/match/CompatibleRecipes.kt` — `compatibleRecipes()`, `MATCH_SUGGESTION_THRESHOLD = 85` y `CompatibleRecipe`: se reutiliza sin tocar.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes` y cableado de `SCAN_PANTRY`/`AI_SUGGESTIONS`; aquí se añade `pantry_recipes`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — top bar con `actions` (`OPEN_PANTRY`, `OPEN_SETTINGS`, iconos `Icons.Outlined.*` de 48 dp con `contentDescription`) y FAB de cámara (`SCAN_PANTRY_FAB`): patrón a imitar para la nueva acción.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — factories Koin de `ScannerViewModel` y de los ViewModels de pantalla; aquí se registran los cambios de firma y el nuevo VM.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt` — la selección de Alimentos sigue leyendo `ScanDraftHolder` + Despensa: no cambia, pero es el destino de RF-SDR4.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — claves `scanner_compatible_*` (5 pares, líneas ~205-210) a renombrar.
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — 8 tests que montan `ScannerScreen` directamente (`launchScanner`) y usan `ScannerTestTags.COMPATIBLE_*`: hay que reapuntarlos a la nueva pantalla.
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — entra en Sugerencias pulsando `ScannerTestTags.COMPATIBLE_SUGGEST_AI` sobre `ScannerScreen` (líneas ~258-285): hay que reapuntarlo.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — monta `ScannerScreen` con `launchScanner`; sirve de regresión de RF-SDR7 (verificar que la sección ya no existe).
- `docs/specs/mostrar-recetas-compatibles.md` — fuente de RF-MRC1 a RF-MRC10 que hay que anotar como sustituida en RF-MRC5.
- `docs/technical-discovery.md` — comandos Gradle (`assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`).
- `DESIGN.md` — dirección de diseño (secciones «Componentes», «Pantallas principales», «Estados a cubrir») para la nueva pantalla.

### Existing Patterns To Follow

- Pantallas en `ui/<pantalla>/` con `*Screen.kt` (UI + `object *TestTags`), `*ViewModel.kt` (StateFlow de un `*UiState`) y `*Route.kt` (puente Koin `koinViewModel()`), como `ui/pantry/` o `ui/aisuggestions/`.
- La regla de negocio vive en `domain/` (`compatibleRecipes()` ya existe en `domain/match/`); la UI no decide.
- Tarjeta con capa clicable de fondo (`matchParentSize` + `clickable` detrás del contenido) para no fusionar la semántica de los hijos (patrón usado por `CompatibleRecipeCard` y las tarjetas de Mis Recetas).
- Textos visibles solo en `strings.xml` es/en con claves idénticas en ambos locales; identificadores de código y testTags en inglés con prefijo por pantalla.
- E2E instrumentada Compose con `createComposeRule()`, repos in-memory (`InMemoryRecipeRepository`/`InMemoryPantryRepository`) y `E2eDatabase.clear()` en `@Before`.

### Current Gaps

- No existe la pantalla ni la ruta `pantry_recipes`: hay que crear `ui/recommendations/` completo.
- No hay un icono de producto definido en `uxdocs` para la entrada en la top bar: se propone `Icons.Outlined.MenuBook` (o el equivalente disponible en la versión de material-icons usada) con `contentDescription` localizado (detalle no bloqueante, D3).

## Technical Approach

1. **Nueva pantalla** en `ui/recommendations/` (nombre del paquete y del VM decidido por la feature: `RecommendationsViewModel`):
   - `RecommendationsViewModel(pantryRepository, recipeRepository)` observa ambos repos y expone `RecommendationsUiState(compatibleRecipes: List<CompatibleRecipe>, hasFoods: Boolean)`; `compatibleRecipes = compatibleRecipes(recipes, pantryItems)` y `hasFoods = pantryItems.isNotEmpty()` (D2). Reactivo igual que hoy (RF-SDR3).
   - `RecommendationsScreen` con `Scaffold` + `TopAppBar` (título `recommendations_title`, `TextButton` «Volver» con tag `recommendations_back`, mismo patrón que el Escáner) y `LazyColumn`: cabecera de sección, tarjetas (reutilizando el contenido de `CompatibleRecipeCard`/`CompatibleRecipeThumbnail` movidos a este fichero o a `ui/components/`), estado vacío y botón «Sugerir receta con IA» habilitado según `hasFoods` (RF-SDR4). TestTags con prefijo `recommendations_` (`recommendations_screen`, `recommendations_title`, `recommendations_card_<id>`, `recommendations_match_<id>`, `recommendations_ingredients_<id>`, `recommendations_empty`, `recommendations_suggest_ai`).
   - `RecommendationsRoute(onBack, onOpenRecipe, onSuggestWithAi, viewModel = koinViewModel())`.
2. **Navegación**: `AppRoutes.PANTRY_RECIPES = "pantry_recipes"`; `MyRecipesRoute` recibe `onOpenPantryRecipes` y lo pasa a `MyRecipesScreen`, que añade un `IconButton` en `actions` antes de Despensa/Ajustes (tag `action_open_pantry_recipes`, icono D3, `contentDescription` `recommendations_open_accessibility`). Desde la nueva pantalla: tarjeta → `recipe_detail/{id}`; sugerencia → `ai_suggestions`; volver → `popBackStack()` (D7).
3. **Limpieza del Escáner** (RF-SDR7): `ScannerViewModel` pierde `RecipeRepository`, `recipes`, `compatibleRecipes` y `updateRecipes`; `updatePantryItems` deja de calcular compatibles. `ScannerScreen` pierde la sección, la tarjeta, la miniatura, `rememberBitmap` (si ningún otro composable lo usa), los tags `COMPATIBLE_*` y los callbacks `onOpenRecipe`/`onSuggestWithAi`; `ScannerRoute` y `AppNavHost` actualizan sus firmas. `AppModule` ajusta la factory.
4. **Textos** (RF-SDR8): renombrar `scanner_compatible_title|ingredients|empty|suggest_ai|card_accessibility` → `recommendations_*` (mismos valores visibles, salvo el título de sección que puede conservar «Recetas Compatibles»/«Compatible Recipes») y añadir `recommendations_title` («Recetas con mi Despensa»/«Recipes with my Pantry») y `recommendations_open_accessibility`. Claves idénticas en `values/` y `values-en/`.
5. **Tests**: unit de `RecommendationsViewModel` (lista, reactividad, `hasFoods`); E2E reapuntados (`CompatibleRecipesE2ETest` → nueva pantalla; `AiSuggestionsE2ETest` → nueva pantalla como origen de la sugerencia) y un caso de regresión que afirme la **ausencia** de la sección en el Escáner.

### Decisiones (para no reabrir el debate en implementación)

- **D1**: pantalla propia con ruta `pantry_recipes`, paquete `ui/recommendations/` y `RecommendationsViewModel` (decisión del usuario 2026-10-24).
- **D2**: `hasFoods` se calcula **solo** sobre la Despensa persistida. Cambia RF-MRC8, que también contaba los chips del Escaneo sin guardar: al no estar la pantalla dentro del Escáner, esos chips no cuentan.
- **D3**: entrada como acción en la top bar de Mis Recetas (icono con `contentDescription` localizado); FAB de cámara intacto. Icono exacto a criterio del implementador dentro de `Icons.Outlined.*` (propuesto `MenuBook`).
- **D4**: reutilizar `compatibleRecipes()`, `CompatibleRecipe` y el contenido visual de la tarjeta sin tocar `calculateMatch` ni `MatchBadge` (RD02, umbral 85 % intactos).
- **D5**: el Escáner conserva íntegros alta manual, cámara, chips, cantidades 0..99, guardado RD13, marcas de duplicado, avisos RD05 y diálogo de salida.
- **D6**: claves de strings y testTags con prefijo `recommendations_`; paridad exacta es/en.
- **D7**: el back stack garantiza Detalle → nueva pantalla → Mis Recetas.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/ui/recommendations/RecommendationsViewModel.kt` — create; estado reactivo (RF-SDR2, RF-SDR3, RF-SDR4).
- `app/src/main/java/com/whatsforlunch/app/ui/recommendations/RecommendationsScreen.kt` — create; pantalla, tarjetas, estado vacío, testTags (RF-SDR1, RF-SDR5).
- `app/src/main/java/com/whatsforlunch/app/ui/recommendations/RecommendationsRoute.kt` — create; puente Koin.
- `app/src/test/java/com/whatsforlunch/app/ui/recommendations/RecommendationsViewModelTest.kt` — create; unit de lista y `hasFoods`.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryRecipesE2ETest.kt` — create (o renombrar `CompatibleRecipesE2ETest.kt`); S1-S8 sobre la nueva pantalla.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modify; ruta `pantry_recipes` y cableado desde Mis Recetas.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesRoute.kt` / `MyRecipesScreen.kt` / `MyRecipesTestTags` — modify; acción de entrada (RF-SDR6).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — modify; sin `RecipeRepository` ni `compatibleRecipes` (RF-SDR7).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` / `ScannerRoute.kt` — modify; sin sección, tarjeta, tags ni callbacks.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; factory de `ScannerViewModel` y registro de `RecommendationsViewModel`.
- `app/src/main/res/values/strings.xml` / `values-en/strings.xml` — modify; renombre `recommendations_*` y textos nuevos (RF-SDR8).
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` / `AiSuggestionsE2ETest.kt` / `ScanPantryE2ETest.kt` / `ScannerViewModelTest.kt` — modify; reapuntar y ajustar firmas.
- `docs/specs/mostrar-recetas-compatibles.md` — modify; nota de que RF-MRC5 queda sustituida por esta spec (sin borrar la historia).
- `DESIGN.md` — modify; la nueva pantalla en «Pantallas principales» y sus estados.

Si el repo cambia de estructura, los caminos son provisionales salvo `ui/`, `domain/` y `data/`, que ya existen.

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` (tokens Warm Culinary Craft ya aplicados en Compose) + reutilización de los componentes visuales existentes de la tarjeta compatible. No hay prototipo `uxdocs` de esta pantalla: se compone con los patrones ya validados (Scaffold + TopAppBar con «Volver», tarjeta `surfaceContainerLowest` con radio `WarmRadius.lg`, botón primario de 52 dp, texto de estado vacío `onSurfaceVariant`).
- Screens or states affected: nueva «Recetas con mi Despensa» (lista, lista con tarjetas, estado vacío, botón de IA habilitado/deshabilitado); Mis Recetas (una acción más en la top bar); Escáner (desaparece la sección inferior).
- New design artifact required: no — no hay cambio de estilo ni de tokens; solo composición de componentes existentes.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe en el repo y esta feature no cambia capas ni dependencias (sigue `ui` → `domain` → `data` con Koin).
- `CONSTRAINTS.md`: not needed — no introduce reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — no cambia el flujo de trabajo, los comandos ni las convenciones.
- Other docs:
  - `docs/specs/mostrar-recetas-compatibles.md` — update; nota de sustitución de RF-MRC5 y de la conservación de RF-MRC1 a RF-MRC4/RF-MRC6 a RF-MRC10 en la nueva pantalla.
  - `docs/domain-model.md` — not needed (RD01/RD02/RD10 intactos; solo se menciona si conviene que la sección ya no vive en el Escáner).
  - `docs/technical-discovery.md` — update solo si cambia algo estructural (no se prevé).
  - `DESIGN.md` — update; nueva pantalla y su entrada.
  - `feature_list.json` y `PROGRESS.md` — update al cerrar la feature (solo `status`/`evidence` y el estado verificado).

## Implementation Plan

1. Crear `RecommendationsViewModel` + unit con `compatibleRecipes()` reutilizado y `hasFoods` sobre la Despensa persistida (RF-SDR2, RF-SDR3, RF-SDR4).
2. Crear `RecommendationsScreen`/`RecommendationsRoute` con cabecera, tarjetas (contenido reutilizado), estado vacío, botón de IA y testTags `recommendations_*` (RF-SDR1, RF-SDR5).
3. Renombrar los textos a `recommendations_*` y añadir los nuevos en es/en (RF-SDR8).
4. Cablear la navegación: acción en la top bar de Mis Recetas → `pantry_recipes` → Detalle / `ai_suggestions` (RF-SDR6, D7).
5. Limpiar el Escáner (ViewModel, Screen, Route, AppModule) sin tocar su resto de comportamiento (RF-SDR7, D5).
6. Reapuntar y ampliar los E2E, actualizar docs y cerrar con los gates.

## Implementation Tasks

- [ ] T1: `RecommendationsViewModel` + `RecommendationsViewModelTest` (lista ordenada, reactividad, `hasFoods` true/false). (RF-SDR2, RF-SDR3, RF-SDR4)
- [ ] T2: `RecommendationsScreen`/`RecommendationsRoute` con tarjetas, estado vacío y testTags. (RF-SDR1, RF-SDR5)
- [ ] T3: textos `recommendations_*` es/en con paridad (título de pantalla, accesibilidad de la acción). (RF-SDR8, S7)
- [ ] T4: ruta `pantry_recipes` + acción en la top bar de Mis Recetas + firmas de `MyRecipesRoute`/`AppNavHost`. (RF-SDR6, S1, S3)
- [ ] T5: limpieza del Escáner (VM/Screen/Route/AppModule) + regresión: el Escáner sin la sección y guardando a la Despensa. (RF-SDR7, S5)
- [ ] T6: E2E reapuntados (`CompatibleRecipesE2ETest` → nueva pantalla; `AiSuggestionsE2ETest` desde la nueva pantalla) y caso de ausencia en el Escáner. (S1-S6, S8)
- [ ] T7: docs (`mostrar-recetas-compatibles.md`, `DESIGN.md`) y evidencia. (RF-SDR1)

## Verification Plan

- `./init.ps1` (Windows) o `./init.sh` — arranque no bloqueante del harness en verde; no arranca servidores ni procesos largos.
- `./gradlew :app:lintDebug` — 0 errores (sin warnings nuevos).
- `./gradlew :app:testDebugUnitTest --rerun-tasks` — en verde, incluido `RecommendationsViewModelTest` y los units existentes de Escáner.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- E2E (comportamiento visible y navegación cambian → cobertura E2E obligatoria):
  - `./gradlew :app:connectedDebugAndroidTest` completa en emulador API 30+ sin regresiones.
  - Focalizada de la nueva pantalla: S1 (orden, badges, desglose), S2 (Detalle y vuelta), S3/S4 (IA habilitada/deshabilitada), S6 (reactividad) y S8 (umbral/exclusión).
  - Focalizada del Escáner: S5 — `COMPATIBLE_*`/`scanner_compatible_*` con 0 hits y guardado → Despensa intacto.
  - Paridad de claves es/en (mismo juego de claves en `values/` y `values-en/`).
- Manual con `adb`/`uiautomator` en el AVD: entrada desde la top bar de Mis Recetas, vuelta atrás correcta (Detalle → nueva pantalla → Mis Recetas) y textos es/en (S7).

## Evidence To Capture

- Informe de implementación en `docs/validations/separar-despensa-y-busqueda-de-recetas.md` con los resultados de cada gate (nombres de tests y recuentos).
- Hasta 2 capturas PNG **binarias y válidas** en español en `docs/evidence/separar-despensa-y-busqueda-de-recetas/` (presupuesto: 1 informe + 2 capturas), por ejemplo `pantry-recipes-lista-es.png` (nueva pantalla con tarjetas) y `pantry-recipes-escaner-sin-seccion-es.png` (Escáner sin la sección). Generarlas con redirección binaria (`cmd /c adb exec-out screencap -p > fichero.png`) y verificar la cabecera PNG `89 50 4E 47 0D 0A 1A 0A`; capturas con datos ficticios, sin API keys reales ni llamadas a la IA.
- `feature_list.json`: resumen en `evidence`; `PROGRESS.md` y `docs/progress-history.md` al día al cerrar la sesión.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (sin bottom navigation, sin tocar `calculateMatch`/`MatchBadge`/umbral/Room/IA, sin cambios en Despensa ni en el guardado del Escaneo).
- [ ] Acceptance scenarios pass (S1-S8).
- [ ] Verification evidence is present (informe + 2 capturas PNG válidas con los nombres previstos).
- [ ] Persistent E2E coverage was added/updated: `CompatibleRecipesE2ETest`/`PantryRecipesE2ETest` y `AiSuggestionsE2ETest` reapuntados, más la ausencia de la sección en el Escáner.
- [ ] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente (solo `status`/`evidence`).
- [ ] No unrelated product behavior or extra feature work was added.
