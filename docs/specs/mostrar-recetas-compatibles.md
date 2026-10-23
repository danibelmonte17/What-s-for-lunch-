# Feature Implementation Spec: Mostrar recetas compatibles

## Source Feature

- `id`: mostrar-recetas-compatibles
- `area`: funcional
- `depends_on`: mostrar-badge-de-coincidencia (`accepted`), escanear-despensa-con-camara (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

El Escáner de la Despensa muestra una sección **«Recetas Compatibles»** con las Recetas de Mis Recetas cuya Coincidencia con la Despensa llega al umbral de sugeribilidad de RD02 (≥ 85 %), en lista vertical y **ordenadas por Coincidencia descendente**. Cada tarjeta lleva el badge de Coincidencia («100% Match» / «N%», RD02) reutilizado de `ui/components/MatchBadge`, su miniatura y el desglose «X de Y ingredientes en la Despensa». Al tocar una tarjeta se abre el Detalle de esa Receta. La sección ofrece **siempre visible** el botón **«Sugerir receta con IA»** (destino provisional; la IA real es `sugerir-recetas-con-ia`), **habilitado solo cuando hay al menos un Alimento en el Escaneo o en la Despensa**; cuando ninguna Receta llega al 85 %, muestra además el estado vacío. Cambio posterior a la aceptación (2026-10-23, petición del usuario).

## Non-Goals

- **IA real de sugerencias**: sin llamadas a OpenRouter, sin API key y sin pantallas de Sugerencias. El botón «Sugerir receta con IA» solo abre un destino provisional (mismo patrón que el Modo Cocina de `ver-detalle-de-receta`); la feature real es `sugerir-recetas-con-ia` (con `configurar-acceso-a-ia`).
- **«Empezar a cocinar ahora»** del prototipo: el Modo Cocina es `cocinar-paso-a-paso` y sigue sin empezar.
- **Botón de carrito / «Te falta: …»** del prototipo: añadir faltantes a la Lista de la Compra es `anadir-faltantes-a-la-lista`; la tarjeta no lista nombres de ingredientes faltantes.
- Recalcular ni cambiar la Coincidencia: se reutiliza `calculateMatch` de `domain/match/RecipeMatch.kt` tal cual (RD01, RD10; sin tocar el badge de Mis Recetas ni el del Detalle).
- Filtrar ni ordenar Mis Recetas por Coincidencia (eso ya es alcance de `buscar-y-filtrar-recetas` y `mostrar-badge-de-coincidencia` lo dejó fuera).
- Cambios en la Despensa, en el Escaneo, en el formulario de Recetas, en Room ni en el esquema.
- «Motor SQL Inteligente», iconos `auto_awesome` ni dependencias de iconos nuevas: el copy del prototipo implicaría un motor que la app no tiene.
- Sumar cantidades al guardar (`sumar-cantidades-al-guardar`).

## Job Story

Cuando termino de escanear lo que tengo en la Despensa y quiero decidir qué cocinar,
quiero ver de un vistazo qué Recetas de Mis Recetas puedo hacer ya con lo que tengo,
para elegir una y abrirla sin salir del Escáner.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P31`): ve sus propias Recetas (Borradores y Publicadas, D4) calculadas contra su Despensa. Nada de esta feature exige permisos nuevos: la sección funciona igual con la cámara concedida, denegada o caída (RD05).

## Requisitos funcionales

Prefijo propio `RF-MRC#` para no colisionar con los `RF-MB#` de `mostrar-badge-de-coincidencia.md` ni los `RF-#` de `crear-receta.md`.

- **RF-MRC1** (RD02, D2): el Escáner lista las Recetas cuya Coincidencia es **≥ 85 %** (85 incluido; el 100 % también entra). El umbral se aplica sobre el porcentaje ya redondeado que muestra el badge.
- **RF-MRC2** (D6): el orden es **Coincidencia descendente**; empate → título ascendente (normalizado); segundo empate → `id` ascendente. Orden determinista y verificable.
- **RF-MRC3** (D4): entran **Borradores y Publicadas** por igual: RD02 no filtra por estado y el badge ya se muestra en ambos.
- **RF-MRC4** (D3): una Receta sin ingredientes obligatorios (Coincidencia indefinida, `calculateMatch` → `null`) **nunca** aparece.
- **RF-MRC5** (D5): la sección «Recetas Compatibles» es una **lista vertical** bajo los chips del Escaneo, dentro del scroll de la pantalla del Escáner (como en el prototipo), con cabecera y contenido o estado vacío.
- **RF-MRC6**: cada tarjeta muestra: badge de Coincidencia (`MatchBadge`: «100% Match» o «N%», RD02), miniatura de la foto o placeholder, título (1 línea) y la línea «X de Y ingredientes en la Despensa» (valores de `RecipeMatch.availableRequired` / `totalRequired`).
- **RF-MRC7** (D7): tocar la tarjeta abre el Detalle de esa Receta (`recipe_detail/{id}`), con vuelta atrás al Escáner.
- **RF-MRC8** (D8): la sección muestra **siempre visible** el botón **«Sugerir receta con IA»**, haya o no Recetas compatibles, pero **solo se habilita cuando hay al menos un Alimento en el Escaneo o en la Despensa**; con cero alimentos queda deshabilitado y no navega. Habilitado, abre un destino provisional con título y vuelta atrás (lo sustituye `sugerir-recetas-con-ia`). Sin Recetas compatibles, la sección muestra además el estado vacío con el mismo botón.
- **RF-MRC9**: el cálculo es **reactivo**: al cambiar la Despensa (alta/baja/edición o al guardar un Escaneo) o las Recetas, la sección se actualiza sin acción manual del usuario.
- **RF-MRC10**: el **cálculo de las Recetas compatibles no depende** del estado de la cámara ni de los chips sin guardar: se hace siempre contra la Despensa persistida (D1) y la sección se muestra igual en modo manual (permiso denegado o fallo de cámara). La habilitación del botón «Sugerir receta con IA» sí considera además los Alimentos del Escaneo sin guardar (RF-MRC8).

## Acceptance Scenarios

### Scenario 1: 100 % y 85 % ordenados y etiquetados (verify de `feature_list.json`)

Given una Receta «Tortilla de patatas» con todos sus obligatorios en la Despensa (100 %) y otra «Pechuga al horno» con 17 de 20 obligatorios en la Despensa (85 %)
When el usuario abre el Escáner
Then la lista vertical muestra «Tortilla de patatas» **antes** que «Pechuga al horno», con los badges «100% Match» y «85%» respectivamente (RF-MRC1, RF-MRC2, RF-MRC6)

### Scenario 2: Bajo el umbral no se sugiere

Given una Receta con Coincidencia 84 % (u otra inferior, p. ej. 2 de 4 obligatorios → 50 %)
When se muestra la sección
Then esa Receta no aparece (RF-MRC1)

### Scenario 3: Actualización reactiva al cambiar la Despensa

Given una Receta con Coincidencia 84 % que no aparece
When el usuario añade a la Despensa (por Escaneo o a mano) el Alimento que falta y su Coincidencia llega al 85 %
Then la Receta aparece en la sección sin reiniciar la app ni reabrir el Escáner (RF-MRC9)

### Scenario 4: Estado vacío con «Sugerir receta con IA»

Given que ninguna Receta llega al 85 % (p. ej. sin Recetas) y hay al menos un Alimento en el Escaneo o en la Despensa
When el usuario mira la sección «Recetas Compatibles»
Then se muestra el estado vacío y el botón «Sugerir receta con IA» habilitado; al pulsarlo se abre el destino provisional con su título y se puede volver al Escáner (RF-MRC8)

### Scenario 4b: Sin alimentos no se puede pedir sugerencia

Given el Escaneo y la Despensa sin ningún Alimento
When el usuario mira la sección «Recetas Compatibles»
Then el botón «Sugerir receta con IA» se muestra deshabilitado y no abre el destino provisional (RF-MRC8)

### Scenario 5: La tarjeta abre el Detalle

Given una Receta compatible visible en la sección
When el usuario toca su tarjeta
Then se abre el Detalle de esa Receta; al volver, el Escáner sigue como estaba (RF-MRC7)

### Scenario 6: Borradores y Publicadas

Given un Borrador con Coincidencia 100 % y una Publicada con 85 %
When se muestra la sección
Then aparecen las dos, cada una con su badge (RF-MRC3)

### Scenario 7: Sin obligatorios no entra

Given una Receta cuyos ingredientes son todos opcionales (o no tiene ingredientes)
When se calculan las compatibles
Then esa Receta no aparece nunca, aunque la Despensa esté llena (RF-MRC4)

### Scenario 8: Textos en inglés

When el idioma del sistema es inglés
Then los textos nuevos (cabecera, línea de ingredientes, estado vacío y botón) se muestran en inglés (P19)

### Scenario 9: Sugerencia con IA también con Recetas compatibles

Given una sección «Recetas Compatibles» con al menos una Receta compatible
When el usuario mira la sección
Then las tarjetas compatibles aparecen y el botón «Sugerir receta con IA» sigue visible; al pulsarlo se abre el destino provisional y se puede volver al Escáner (RF-MRC8)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Despensa vacía | Todas las Recetas al 0 % → estado vacío; «Sugerir receta con IA» solo se habilita si el Escaneo tiene algún Alimento (S4, S4b) |
| Sin Recetas en Mis Recetas | Mismo estado vacío con el botón (S4) |
| Sin alimentos (Escaneo y Despensa vacíos) | El botón «Sugerir receta con IA» se muestra deshabilitado y no navega (S4b) |
| 85 % exacto | **Entra** (D2; RF-MRC1) |
| 84 % | **No entra** (S2) |
| Porcentaje no entero que redondea a 85 (11 de 13 → 85) | Entra: el umbral se aplica al porcentaje redondeado que muestra el badge (D2) |
| Empate de Coincidencia | Título ascendente normalizado; si persiste, `id` (D6) |
| Alimento con cantidad 0 («sin cantidad») | Cuenta como disponible: la Coincidencia es de presencia (D1 de `mostrar-badge-de-coincidencia`) |
| Opcionales en la Despensa | No alteran nada (RD10); solo unit, ya cubierto en `RecipeMatchTest` |
| Chips del Escaneo sin guardar | No alteran el cálculo: solo cuenta la Despensa persistida (D1, RF-MRC10), pero habilitan el botón de IA si aún no hay alimentos guardados (RF-MRC8) |
| Receta sin foto | Placeholder `SkilletIcon` en la miniatura |
| Muchas Recetas compatibles | La lista hace scroll con la pantalla; sin límite ni paginación en el MVP |
| Receta editada o eliminada | La sección se recalcula reactivamente (RF-MRC9) |
| Permiso de cámara denegado / fallo de cámara | La sección sigue visible y operativa (RF-MRC10, RD05) |

## Repository Research

### Files Inspected

- `feature_list.json` — `steps`, `verification` y dependencias (`mostrar-badge-de-coincidencia` y `escanear-despensa-con-camara`, ambas `accepted`).
- `docs/domain-model.md` — RD01, RD02 (umbral 85 %, «100% Match»), RD10 y la fórmula de Coincidencia.
- `docs/build-brief.md` — «Escáner … carrusel de Recetas compatibles (100% / 85%)» (vocabulario de producto; la disposición la fija D5).
- `DESIGN.md` — pantalla «Escáner de la Despensa» (visor, HUD, chips, carrusel 100 %/85 %), estados a cubrir, accesibilidad ≥ 48 dp.
- `docs/risks-and-open-questions.md` — sin `⚠` que bloquee; RK04 (el material UX es orientativo) y el patrón de preguntas `Q-…` de las specs.
- `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html` (líneas 139-225) — sección «Recetas Compatibles» del prototipo: cabecera, tarjeta 100 % (píldora «100% COINCIDENCIA», miniatura 96 px, «4 de 4 ingredientes en nevera», CTA «Empezar a cocinar ahora») y tarjeta 85 % («Te falta: Orégano seco», «Ver Receta» + carrito). Ambas apiladas en vertical.
- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` — `RecipeMatch(percentage, availableRequired, totalRequired)`, `isFullMatch`, `calculateMatch()` → `null` sin obligatorios (reutilizar sin tocar).
- `app/src/main/java/com/whatsforlunch/app/ui/components/MatchBadge.kt` — badge compartido con `testTag` por pantalla (D5 de `ver-detalle-de-receta`).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — `ScannerUiState`, hoy solo observa `PantryRepository` (`updatePantryItems`); constructor `ScannerViewModel(PantryRepository, FoodDetector)`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — `ScannerTestTags`, Column con preview + `LazyColumn` (chips, tag `FOOD_LIST`) + `ManualFoodEntry`; **`FOOD_LIST` no se aserta en ningún test** → se puede reorganizar el scroll sin romper la suite.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — wiring de callbacks y ciclo de vida de la cámara.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes.RECIPE_DETAIL` / `recipeDetail(id)` existentes; `COOK_MODE_PROVISIONAL` con `ProvisionalScreen`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — `ProvisionalTestTags` y destino provisional con título + vuelta atrás (Q-arrancar-app-en-mis-recetas-3).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `viewModel { ScannerViewModel(get(), get()) }`.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — `observeRecipes(): Flow<List<Recipe>>` (reactividad gratis).
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt` — `photoPath`, `status`, `ingredients` con `optional`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesViewModel.kt` — patrón `combine(recipes, pantry)` + `matches` (copiar el estilo, no la clase).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `rememberBitmap(recipe.photoPath)` y placeholder `SkilletIcon` cuando `bitmap == null`.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `scanner_*`, `my_recipes_card_match*` y convención es/en.
- `app/src/test/java/com/whatsforlunch/app/ui/scanner/ScannerViewModelTest.kt` — ~12 call sites de `ScannerViewModel(...)` (cambia la firma).
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — helper `launchScanner(...)` (construye `ScannerViewModel(repository, detector)` y monta `ScannerScreen` directamente con repos in-memory; patrón a replicar).
- `app/src/androidTest/java/com/whatsforlunch/app/E2eDatabase.kt` — limpieza de Room para las suites que usan la app real.

### Existing Patterns To Follow

- MVVM + StateFlow; reglas de negocio puras en `domain/` con comentarios que citan `RD##`; Koin inyecta los ViewModels (`AppModule`).
- Funciones de dominio testeadas en unit (`RecipeMatchTest`, `ScanDetectionsTest`); E2E Compose con `InMemoryRecipeRepository` / `InMemoryPantryRepository` y `FakeFoodDetector`/`FakeLiveFrameSource`.
- Componentes compartidos en `ui/components/` con `testTag` aportado por cada pantalla (`MatchBadge`).
- Capa clicable de fondo en las tarjetas (`matchParentSize` + `clickable` detrás del contenido) para no fusionar la semántica de los hijos con `mergeDescendants` (lección de la sesión 48).
- Destinos provisionales con `ProvisionalScreen` + `ProvisionalTestTags` para affordances de features futuras.
- Textos en `values/` y `values-en/`; identificadores de código en inglés (`P19`, `P25`).
- Comandos: scripts del harness `init`/`lint`/`unit`/`e2e` sobre `./gradlew` (`docs/technical-discovery.md`).

### Current Gaps

- `ScannerViewModel` no observa Recetas: hay que añadir `RecipeRepository` al constructor (≈12 call sites en `ScannerViewModelTest` y 1 en `ScanPantryE2ETest.launchScanner`) y a `AppModule`.
- No existe ninguna regla de selección/orden de Recetas compatibles: se crea en `domain/match/` desde cero.
- El Escáner no tiene sección de Recetas ni destino de sugerencias: hay que crear la sección, la tarjeta y la ruta provisional.
- Sin screenshot testing en el repo (el muestreo visual lo ejecuta el validador, precedente `afinar-fidelidad-visual-del-formulario`).

## Technical Approach

- **Regla de dominio** — nuevo `app/src/main/java/com/whatsforlunch/app/domain/match/CompatibleRecipes.kt`:

  ```kotlin
  // Umbral de sugeribilidad de RD02 aplicado al porcentaje redondeado (D2).
  const val MATCH_SUGGESTION_THRESHOLD = 85

  data class CompatibleRecipe(val recipe: Recipe, val match: RecipeMatch)

  // Compatibles según RD02, ordenadas por RF-MRC2. Sin obligatorios → no entra (RF-MRC4).
  fun compatibleRecipes(recipes: List<Recipe>, pantryItems: List<PantryItem>): List<CompatibleRecipe>
  ```

  Implementación: `recipes.mapNotNull { r -> calculateMatch(r, pantryItems)?.let { CompatibleRecipe(r, it) } }` → filtrar `match.percentage >= MATCH_SUGGESTION_THRESHOLD` → `sortedWith(compareByDescending { it.match.percentage }.thenBy { normalizeName(it.recipe.title) }.thenBy { it.recipe.id })`. Sin filtrar por `recipe.status` (D4).
- **ViewModel** — `ScannerViewModel(pantryRepository, recipeRepository, foodDetector)`. En `init`, `combine(recipeRepository.observeRecipes(), pantryRepository.observePantryItems())` actualiza `pantryItems`/`matches` como hoy (el flujo de duplicados no cambia) y además `ScannerUiState` gana `compatibleRecipes: List<CompatibleRecipe> = emptyList()` calculado con la función de dominio (RF-MRC9). El estado de los chips, los diálogos y el guardado quedan intactos.
- **UI** — `ScannerScreen` reorganiza su cuerpo en **un único `LazyColumn`** (preview, aviso e indicador siguen fuera del scroll; `ManualFoodEntry` y `ScannerActions` se quedan fijos): items de chips (tags existentes sin cambios) y, después, la sección «Recetas Compatibles» (`item` de cabecera + tarjetas o `item` de estado vacío). La lista de chips vacía conserva su hint actual. Nuevos callbacks `onOpenRecipe: (String) -> Unit` y `onSuggestWithAi: () -> Unit`.
- **Tarjeta compatible** (`CompatibleRecipeCard`): `Surface` con `surfaceContainerLowest` y `WarmRadius.lg`; fila con miniatura cuadrada 96 dp (`rememberBitmap(recipe.photoPath)`; sin foto → `SkilletIcon` sobre `surfaceContainer`), `MatchBadge` arriba a la izquierda de la fila de cabecera, título 1 línea y línea «X de Y ingredientes en la Despensa»; **capa clicable de fondo** que dispara `onOpenRecipe(recipe.id)` (patrón de la sesión 48).
- **Navegación** — `AppNavHost`: `ScannerRoute` recibe `onOpenRecipe = { navController.navigate(AppRoutes.recipeDetail(it)) }` y `onSuggestWithAi = { navController.navigate(AppRoutes.AI_SUGGESTIONS_PROVISIONAL) }`; nueva ruta `ai_suggestions_provisional` con `ProvisionalScreen(title = stringResource(R.string.ai_suggestions_title), testTag = ProvisionalTestTags.AI_SUGGESTIONS_SCREEN)`.
- **Textos** (`values/strings.xml` + `values-en/strings.xml`): `scanner_compatible_title` («Recetas Compatibles» / «Compatible Recipes»), `scanner_compatible_ingredients` («%1$d de %2$d ingredientes en la Despensa» / «%1$d of %2$d ingredients in the Pantry»), `scanner_compatible_empty` («Aún no hay recetas compatibles con tu Despensa.» / «No recipes match your Pantry yet.»), `scanner_compatible_suggest_ai` y `ai_suggestions_title` («Sugerir receta con IA» / «Suggest a recipe with AI») y `scanner_compatible_card_accessibility` («Receta compatible: %1$s, %2$d%% de coincidencia» / «Compatible recipe: %1$s, %2$d%% match»).
- **Tests** — unit `CompatibleRecipesTest` (umbral, orden y empates, `null` excluido, Borradores/Publicadas, redondeo 11/13 → 85) y E2E `CompatibleRecipesE2ETest` con repos in-memory (patrón de `launchScanner` de `ScanPantryE2ETest`, sin cámara: `permissionGranted = false` es suficiente para la sección, que no depende de ella).

### Decisiones (para no reabrir el debate en implementación)

- **D1 — contra la Despensa persistida**: los chips del Escaneo sin guardar **no** alteran el cálculo (feature_list: «frente a la Despensa»).
- **D2 — umbral sobre el porcentaje redondeado**: ≥ 85 % incluido; 84,6 % redondeado a 85 entra (es el número que muestra el badge).
- **D3 — sin obligatorios**: nunca compatible (`calculateMatch` → `null`).
- **D4 — Borradores y Publicadas** (confirmado por el usuario, 2026-10-22).
- **D5 — lista vertical** de tarjetas bajo los chips (confirmado por el usuario, 2026-10-22): la palabra «carrusel» de `build-brief.md`/`DESIGN.md` es vocabulario de producto, no disposición.
- **D6 — orden determinista**: Coincidencia desc → título ascendente normalizado → `id`.
- **D7 — tocar la tarjeta abre el Detalle** (`recipe_detail/{id}`, confirmado por el usuario, 2026-10-22); sin CTA «Empezar a cocinar» ni botón de carrito (Non-Goals).
- **D8 — «Sugerir receta con IA» siempre visible** (confirmado por el usuario, 2026-10-22; ampliado el mismo día para que también aparezca con Recetas compatibles) → destino provisional (`ProvisionalScreen`); la feature real es `sugerir-recetas-con-ia`. Sin compatibles se añade el estado vacío sobre el mismo botón.
- **D9 — badge compartido**: `MatchBadge` con testTag propio de la pantalla («100% Match» literal, RD02; «N%» en el resto).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/match/CompatibleRecipes.kt` — crear; selección y orden (RF-MRC1 a RF-MRC4).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — modificar; `RecipeRepository`, `compatibleRecipes` en `ScannerUiState` (RF-MRC9).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — modificar; sección, tarjeta, estado vacío, testTags y callbacks (RF-MRC5 a RF-MRC8, RF-MRC10).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — modificar; cableado de `onOpenRecipe` y `onSuggestWithAi`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; callbacks del Escáner y ruta `ai_suggestions_provisional`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — modificar; `ProvisionalTestTags.AI_SUGGESTIONS_SCREEN`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `ScannerViewModel(get(), get(), get())`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; 6 claves nuevas.
- `app/src/test/java/com/whatsforlunch/app/domain/match/CompatibleRecipesTest.kt` — crear; unit de la regla.
- `app/src/test/java/com/whatsforlunch/app/ui/scanner/ScannerViewModelTest.kt` — modificar; firma del constructor (~12 call sites).
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — modificar; firma en `launchScanner` (1 call site).
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — crear; verify y S2-S9.
- `docs/validations/mostrar-recetas-compatibles.md` y `docs/evidence/mostrar-recetas-compatibles/*.png` — crear al validar (evidencia).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`) y la sección «Recetas Compatibles» de `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html` (inspiración, `P16`). El prototipo **no** define el estado vacío de la sección ni el destino de sugerencias → planning gap resuelto aquí (D8) para que el implementer no lo invente.
- Screens or states affected: Escáner de la Despensa (sección nueva bajo los chips; sin cambios en visor, HUD, chips, diálogos ni dock). Nuevo destino provisional «Sugerir receta con IA».
- New design artifact required: no — se usan los tokens y componentes existentes.

Especificación de la sección y la tarjeta (verificable por `uiautomator` y captura; sin exigir muestreo de píxeles):

- **Cabecera**: texto `MaterialTheme.typography.titleMedium` «Recetas Compatibles», `testTag` `scanner_compatible_title`. Sin icono `auto_awesome` ni la etiqueta «Motor SQL Inteligente» del prototipo (Non-Goals).
- **Tarjeta**: `Surface` `surfaceContainerLowest`, radio `WarmRadius.lg`, padding `WarmSpacing.md`, separación `WarmSpacing.md` entre tarjetas y `WarmSpacing.md` a los lados.
  - Miniatura cuadrada **96 dp**, radio `WarmRadius.md`; sin foto → `SkilletIcon` sobre `surfaceContainer`.
  - `MatchBadge` (píldora compartida): 100 % → «100% Match» sobre `secondary`; resto → «N%» sobre `secondaryContainer`.
  - Título 1 línea con elipsis (`titleSmall`); debajo, «X de Y ingredientes en la Despensa» (`labelSmall`, `onSurfaceVariant`).
  - Objetivo táctil: la tarjeta entera ≥ 48 dp de alto; descripción accesible `scanner_compatible_card_accessibility`.
- **Estado vacío**: texto `scanner_compatible_empty` (`bodySmall`, `onSurfaceVariant`) y, siempre visible, botón primario «Sugerir receta con IA» (altura mínima 52 dp, `testTag` `scanner_compatible_suggest_ai`), habilitado solo si hay al menos un Alimento en el Escaneo o en la Despensa. El botón también se muestra debajo de las tarjetas cuando hay Recetas compatibles.
- `testTags` nuevos en `ScannerTestTags`: `COMPATIBLE_SECTION`, `COMPATIBLE_TITLE`, `compatibleCard(id)`, `compatibleMatch(id)`, `compatibleIngredients(id)`, `COMPATIBLE_EMPTY`, `COMPATIBLE_SUGGEST_AI`.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (`domain/match/` y `ui/scanner/` dentro de la convención existente).
- `CONSTRAINTS.md`: not needed — no existe y la feature no introduce reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/risks-and-open-questions.md` — **update**: registrar `Q-mostrar-recetas-compatibles-1..4` (estado de las Recetas, disposición de la lista, acción de la tarjeta y estado vacío con botón de IA) como **resueltas por el usuario el 2026-10-22** (patrón de las `Q-…` existentes). `docs/domain-model.md` — not needed (RD02 ya define el umbral y el badge; D1-D9 son decisiones de esta spec).

## Implementation Plan

1. Crear la regla `domain/match/CompatibleRecipes.kt` con sus unit tests (`CompatibleRecipesTest`).
2. Ampliar `ScannerViewModel` con `RecipeRepository` y `compatibleRecipes` (RF-MRC9, RF-MRC10) y adaptar `AppModule` y los call sites de tests existentes.
3. Implementar la sección y la tarjeta en `ScannerScreen` con textos es/en y testTags (RF-MRC5, RF-MRC6, D9).
4. Cablear la navegación: tap → Detalle y estado vacío → destino provisional (RF-MRC7, RF-MRC8, D8).
5. Crear `CompatibleRecipesE2ETest`, dejar la regresión completa en verde, capturar la evidencia y registrar las `Q-…` en `docs/risks-and-open-questions.md`.

## Implementation Tasks

- [ ] T1: `CompatibleRecipes.kt` + `CompatibleRecipesTest` (umbral 85/84, redondeo 11/13, orden y empates, `null` excluido, Borradores/Publicadas). (RF-MRC1 a RF-MRC4, S1, S2, S6, S7)
- [ ] T2: `ScannerViewModel` + `AppModule` + firma en `ScannerViewModelTest` y `ScanPantryE2ETest`; `compatibleRecipes` reactivo. (RF-MRC9, RF-MRC10, S3)
- [ ] T3: sección y `CompatibleRecipeCard` en `ScannerScreen` con testTags y textos es/en. (RF-MRC5, RF-MRC6, S1, S8)
- [ ] T4: navegación tap → Detalle y botón «Sugerir receta con IA» siempre visible → `ai_suggestions_provisional` (AppNavHost, ProvisionalScreen). (RF-MRC7, RF-MRC8, S4, S5, S9)
- [ ] T5: `CompatibleRecipesE2ETest` (verify + S2-S9) sin romper la suite existente.
- [ ] T6: `init`/`lint`/`unit`/`e2e` en verde, evidencia capturada y `Q-mostrar-recetas-compatibles-1..4` registradas en `docs/risks-and-open-questions.md`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate de arranque estándar no bloqueante; no arranca procesos duraderos; artefactos conocidos: carrera del `pidof` en `init.sh` y exit 1 falso de `init.ps1` por el stderr de `adb shell monkey` — ejecutar con `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& ... powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `CompatibleRecipesTest`, `ScannerViewModelTest` y `RecipeMatchTest`. Para reejecución forzada: `gradlew.bat testDebugUnitTest --rerun-tasks` (`unit.ps1` no reenvía argumentos).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, emulador `Pixel_10_Pro` API 37) con `CompatibleRecipesE2ETest`:
  - verify (S1): Receta A «100% Match» y Receta B «85%» (17 de 20 obligatorios, generados programáticamente) → ambas visibles, A **antes** que B (comparar posición vertical de las tarjetas) y con sus badges y su línea «X de Y ingredientes en la Despensa».
  - S2: una Receta al 84 % (16 de 19) y otra al 50 % no aparecen.
  - S3: reactividad — al entrar un Alimento en la Despensa que empuja una Receta al 85 %, la tarjeta aparece sin relanzar la pantalla.
  - S4: sin compatibles → `scanner_compatible_empty` + botón → destino provisional `provisional_ai_suggestions_screen` con vuelta atrás.
  - S9: con compatibles → tarjetas visibles y el mismo botón sigue disponible → destino provisional con vuelta atrás.
  - S5: tap en la tarjeta → `recipe_detail/{id}` (Detalle real); al volver, el Escáner sigue operativo.
  - S6: un Borrador con 100 % aparece igual que una Publicada.
  - S7: Receta sin obligatorios no aparece (además de unit).
  - S8: en inglés cambian cabecera, línea de ingredientes, estado vacío y botón (patrón de `englishLabelsAndScannerActionsAreLocalized`).
  - Regresión completa en verde (suite `connectedDebugAndroidTest`, hoy 96 tests) sin romper `escanear-despensa-con-camara`, `detectar-alimentos-en-vivo`, `mostrar-badge-de-coincidencia` ni `ver-detalle-de-receta`.
- Cobertura E2E: **sí se añade**, porque la feature cambia comportamiento visible del Escáner y añade una navegación nueva.
- `init.sh` / `init.ps1` **no se modifican**: siguen ejecutando solo los checks de arranque no bloqueantes del estado actual del repo y **no** arrancan procesos duraderos (sin servidores ni emuladores nuevos); pueden imprimir comandos de seguimiento una vez pasados los checks.
- Unit: umbral y orden (D2, D6), `null` excluido (D3), Borradores incluidos (D4), redondeo 11/13 → 85, cocina de empates con título e `id`.
- UI (solo el validador): captura de la sección con tarjetas 100 %/85 % y del estado vacío con el botón (es) + `uiautomator`; sin pipeline de screenshot testing en el repo (justificado por el precedente de `afinar-fidelidad-visual-del-formulario`).
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/mostrar-recetas-compatibles.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación por escenario S1-S9.
- `docs/evidence/mostrar-recetas-compatibles/escaner-compatibles-100-85.png` — sección con la tarjeta «100% Match» delante de la «85%» (es).
- `docs/evidence/mostrar-recetas-compatibles/escaner-compatibles-vacio.png` — estado vacío con el botón «Sugerir receta con IA» (es).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (coincidencia vs Despensa) | RF-MRC1, RF-MRC4, RF-MRC9 | S2, S3, S7 | T1, T2 | unit `CompatibleRecipesTest` |
| Step 2 (lista con badges 100 %/85 % según RD02) | RF-MRC2, RF-MRC5, RF-MRC6 | S1, S6 | T3 | E2E + captura |
| Verify (E2E: 100 % y 85 %, orden y etiquetas) | RF-MRC1, RF-MRC2, RF-MRC6 | S1 | T5 | `CompatibleRecipesE2ETest` |
| — (apertura del Detalle) | RF-MRC7 | S5 | T4 | `CompatibleRecipesE2ETest` |
| — (estado vacío con IA) | RF-MRC8 | S4 | T4 | `CompatibleRecipesE2ETest` |
| — (IA también con compatibles) | RF-MRC8 | S9 | T4 | `CompatibleRecipesE2ETest` |
| — (accesible sin cámara, reactividad) | RF-MRC10, RF-MRC9 | S3, casos límites | T2, T5 | `CompatibleRecipesE2ETest` |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature.
- [ ] Los escenarios de aceptación pasan (S1-S9) y las decisiones D1-D9 se respetan.
- [ ] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente añadida y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular: **sin IA real ni llamadas de red**, sin Lista de la Compra ni ingredientes faltantes, sin Modo Cocina, sin tocar `calculateMatch`, el badge de Mis Recetas/Detalle ni Room).
- [ ] `docs/risks-and-open-questions.md` registra `Q-mostrar-recetas-compatibles-1..4` resueltas.
