# Informe de implementación: `separar-despensa-y-busqueda-de-recetas`

- **Fecha:** 2026-10-24
- **Implementador:** `feature-implementer` (subagente)
- **Spec:** `docs/specs/separar-despensa-y-busqueda-de-recetas.md`
- **Estado declarado:** `passing` en `feature_list.json` (**no es aceptación**; pendiente `feature-validator`)
- **Árbol:** cambios sin commitear sobre `develop` (`c304cda`)
- **Entorno:** AVD `Pixel_10_Pro` (API 37, `emulator-5554`), JDK `C:\Users\Daniel\.jdks\corretto-17.0.20`. Nunca el dispositivo físico.

---

## 1. Alcance implementado

La consulta «qué cocino con lo que tengo» deja de vivir dentro del Escáner y pasa a una pantalla propia **«Recetas con mi Despensa»** (ruta `pantry_recipes`, paquete `ui/recommendations/`), con entrada desde la top bar de Mis Recetas. El Escáner queda dedicado solo a llenar la Despensa.

- **Nueva pantalla** (`RecommendationsScreen`/`RecommendationsViewModel`/`RecommendationsRoute`): `Scaffold` + `TopAppBar` con «Volver», `LazyColumn` con la cabecera de sección, las tarjetas compatibles (badge `MatchBadge`, miniatura, título y «X de Y ingredientes en la Despensa»), estado vacío y botón «Sugerir receta con IA» habilitado solo con Despensa persistida no vacía (`hasFoods`, D2). TestTags `recommendations_*`.
- **Estado reactivo**: `combine` de `RecipeRepository.observeRecipes()` y `PantryRepository.observeItems()` → `compatibleRecipes(recipes, pantryItems)` (RF-SDR2/RF-SDR3) y `hasFoods = pantryItems.isNotEmpty()` (RF-SDR4). No se tocó `calculateMatch`, `MatchBadge`, el umbral 85 % ni Room.
- **Navegación**: `AppRoutes.PANTRY_RECIPES = "pantry_recipes"`; acción `Icons.AutoMirrored.Outlined.MenuBook` (`action_open_pantry_recipes`, `contentDescription` localizado) en la top bar de Mis Recetas antes de Despensa/Ajustes; tarjeta → `recipe_detail/{id}`; sugerencia → `ai_suggestions`; volver → `popBackStack()` (D7). FAB de cámara y pantalla Despensa intactos.
- **Limpieza del Escáner** (RF-SDR7): `ScannerViewModel` pierde `RecipeRepository`, `recipes`, `compatibleRecipes` y `updateRecipes`; `ScannerScreen` pierde sección, tarjeta, miniatura, `rememberBitmap`, tags `COMPATIBLE_*` y callbacks; `ScannerRoute` y `AppNavHost` ajustan firmas; `AppModule` actualiza las factories. Alta manual, cámara, chips, cantidades 0..99, guardado RD13, marcas de duplicado, avisos y diálogo de salida intactos.
- **Textos** (RF-SDR8): `scanner_compatible_*` → `recommendations_*` + `recommendations_title` y `recommendations_open_accessibility`, con paridad es/en.

Fuera de alcance respetado: sin bottom navigation, sin cambios en Despensa, guardado del Escaneo, IA/Room, `calculateMatch` ni `MatchBadge`.

---

## 2. Checks ejecutados

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `.\init.ps1` | **exit 0**; verificó e instaló/arrancó la app en el AVD API 37 |
| Unit | `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **132 tests, 22 suites, 0 fallos, 0 errores, 0 omitidas** (recuento desde `app/build/test-results/testDebugUnitTest/TEST-*.xml`) |
| Lint | `.\gradlew.bat :app:lintDebug` | **0 errores / 49 warnings** (`lint-results-debug.txt`: `0 errors, 49 warnings`); sin warnings nuevos de la feature (el único de `MyRecipesScreen.kt:158` es previo) |
| Build | `.\gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** |
| Paridad de textos | Comparación de claves `values/strings.xml` vs `values-en/strings.xml` | **paridad exacta** de las 7 claves `recommendations_*` en ambos locales |
| E2E focalizada | `:app:connectedDebugAndroidTest` con `PantryRecipesE2ETest`, `AiSuggestionsE2ETest`, `ScanPantryE2ETest` | **38/38** (PantryRecipes 8/8, AiSuggestions 8/8, ScanPantry 22/22) |
| E2E completa | `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | **BUILD SUCCESSFUL** en 7 m 47 s; **131 pruebas, 0 fallos, 0 errores y 2 omitidas** (`SettingsE2ETest` por `Assume`, sin key de build) |
| Verificación manual | `adb` + `uiautomator dump` sobre la app real | Ver §3 |

El comando combinado `:app:testDebugUnitTest --rerun-tasks :app:lintDebug :app:assembleDebug` terminó en BUILD SUCCESSFUL.

### Tests E2E de la feature (`PantryRecipesE2ETest`, 8/8)

`recommendationsListShowsOrderedCardsWithBadgesAndIngredientBreakdown` (S1), `recipesBelowThresholdAndWithoutRequiredIngredientsAreNotListed` (S8), `listUpdatesReactivelyWhenPantryAndRecipesChange` (S6), `aiSuggestionIsDisabledWhenPantryIsEmpty` (S4), `aiSuggestionOpensSuggestionsAndBackReturnsToRecommendations` (S3), `aiSuggestionStaysAvailableWhenCompatibleRecipesExist` (RF-MRC8), `tappingRecommendationOpensDetailAndBackReturnsToList` (S2), `recommendationsScreenIsLocalizedInEnglish` (S7).

Regresión del Escáner (RF-SDR7): `ScanPantryE2ETest.scannerNoLongerShowsCompatibleRecipesOrTheAiSuggestionButton` (0 hits de la sección y del botón de IA, guardado → Despensa intacto).

### Unit de la feature (`RecommendationsViewModelTest`, 5/5)

`listsOnlyCompatibleRecipesInDomainOrder`, `emptyOrIncompletePantryYieldsNoRecommendations`, `stateRecalculatesWhenPantryChangesWithoutRecreatingTheViewModel`, `stateRecalculatesWhenRecipesChangeAndTracksEmptyPantry`, `hasFoodsFollowsThePersistedPantryOnly`.

---

## 3. Verificación manual en el AVD

Datos ficticios sembrados en la base Room local de la app (3 Recetas: «Tortilla de patatas» 100 %, «Borrador compatible» 100 %, «Pechuga al horno» 86 %; 8 Alimentos en la Despensa). Sin API keys reales y sin ninguna llamada a DevExpert Inference.

- **Entrada (RF-SDR6/S1)**: desde Mis Recetas, la acción de la top bar (`contentDescription` «Recetas con mi Despensa») abre la pantalla. Se ven el título «Recetas con mi Despensa», la cabecera «Recetas Compatibles», las tarjetas ordenadas 100 % → 100 % → 86 % con sus badges y las líneas `2 de 2`, `3 de 3` y `6 de 7 ingredientes en la Despensa`, y el botón «Sugerir receta con IA».
- **Detalle y vuelta (S2/D7)**: tap en «Tortilla de patatas» abre el Detalle; `VOLVER`/atrás regresa a «Recetas con mi Despensa» con la lista intacta.
- **Sugerencias (S3, RF-4)**: con Despensa no vacía, «Sugerir receta con IA» abre la pantalla de Sugerencias (lista de alimentos seleccionables, sin llamada a la IA); atrás regresa a la nueva pantalla y otro atrás a Mis Recetas (back stack Detalle → nueva pantalla → Mis Recetas).
- **Escáner (S5/RF-SDR7)**: el FAB de cámara abre «Escáner de la Despensa»; con un Alimento añadido a mano, la pantalla solo muestra alta manual, cámara, el chip con cantidad y «Guardar en la Despensa»: **no aparecen «Recetas Compatibles» ni «Sugerir receta con IA»**.
- **Inglés (S7)**: con locale de app `en-US` se leen «Recipes with my Pantry», «Compatible Recipes», «Suggest a recipe with AI», «Back» y los textos de tarjeta en inglés. Locale restaurado a español después.

---

## 4. Evidencia

- `docs/evidence/separar-despensa-y-busqueda-de-recetas/pantry-recipes-lista-es.png` (140.075 B, cabecera PNG `89 50 4E 47 0D 0A 1A 0A`): nueva pantalla con las 3 tarjetas, badges 100 %/86 % y botón de IA.
- `docs/evidence/separar-despensa-y-busqueda-de-recetas/pantry-recipes-escaner-sin-seccion-es.png` (115.438 B, misma cabecera PNG): Escáner con un chip y **sin** la sección de compatibles ni el botón de IA.
- Capturas generadas con redirección binaria (`cmd /c "adb exec-out screencap -p > fichero.png"`) y verificadas byte a byte. Presupuesto: 1 informe + 2 PNG.

---

## 5. Documentación actualizada

- `docs/specs/mostrar-recetas-compatibles.md`: nota de sustitución de RF-MRC5 por esta feature y conservación de RF-MRC1 a RF-MRC4/RF-MRC6 a RF-MRC10 en la nueva pantalla.
- `DESIGN.md`: nueva fila «Recetas con mi Despensa» en «Pantallas principales», ajuste de la fila del Escáner (ya sin compatibles) y estados de la pantalla.
- `feature_list.json` (solo `status`/`evidence`), `PROGRESS.md` y `docs/progress-history.md`.

`docs/domain-model.md` no requería cambios (RD01/RD02/RD10 intactos); `docs/technical-discovery.md` tampoco (sin cambios estructurales); `CONSTRAINTS.md`/`AGENTS.md` sin reglas nuevas.

---

## 6. Desviaciones y notas

- El informe cita los recuentos reales del XML de resultados (131 E2E completas, 2 omitidas por `Assume`), que sustituyen a los recuentos preliminares tomados de la salida de Gradle.
- `PantryRecipesE2ETest` sustituye a `CompatibleRecipesE2ETest` (eliminado), y `AiSuggestionsE2ETest` monta ahora `RecommendationsScreen` como origen de la sugerencia.
- Para la evidencia manual se sembraron Recetas/Alimentos ficticios directamente en la base Room del dispositivo (volcado, inserción y restauración con la app detenida), evitando cualquier llamada de pago a la IA.
- No se hicieron commits ni pushes.

## 7. Pendiente

Validación independiente con `feature-validator` (accept/revise/block). El implementador no declara aceptación.


---

## 8. Validación independiente (sesión 72, `feature-validator`, 2026-10-24)

- **Veredicto: `accept`**
- `feature id`: `separar-despensa-y-busqueda-de-recetas`
- `spec`: `docs/specs/separar-despensa-y-busqueda-de-recetas.md`
- `rama`: `feature/separar-despensa-y-busqueda-de-recetas` con los cambios sin commitear
- `entorno`: Windows, JDK `C:\Users\Daniel\.jdks\corretto-17.0.20`, AVD `Pixel_10_Pro` (API 37, `emulator-5554`). Nunca el dispositivo físico. Sin commits ni pushes; no se modificó código de implementación.

### 8.1 Checks baratos reejecutados sobre el árbol actual

| Check | Resultado |
|---|---|
| `./init.ps1` | **exit 0** (`--version`, `installDebug` en `emulator-5554`, `monkey` LAUNCHER y `pidof` con la app viva) |
| `:app:lintDebug` | **0 errores / 49 warnings**; ningún warning en `ui/recommendations/` ni en los ficheros tocados (el `ModifierParameter` de `MyRecipesScreen.kt:158` y los de `KeystoreAiKeyStore`/`CameraXScanImageSource` son previos) |
| `:app:testDebugUnitTest --rerun-tasks` | **132/132 en 22 suites**, 0 fallos, 0 errores, 0 omitidas (recuento desde `TEST-*.xml`); `RecommendationsViewModelTest` 5/5 |
| `:app:assembleDebug` | **BUILD SUCCESSFUL** (comando combinado lint + unit + assemble: 48 s) |
| Paridad de textos | `values/strings.xml` 208 claves = `values-en/strings.xml` 208 claves; 7 claves `recommendations_*` en ambos locales; ninguna `scanner_compatible_*` |
| Restos del Escáner | `git grep scanner_compatible\|COMPATIBLE_` en `app/src/main` = **0 hits**; solo quedan aserciones literales de ausencia en `ScanPantryE2ETest` y referencias históricas en docs |
| Evidencia PNG | 2 ficheros, cabecera `89 50 4E 47 0D 0A 1A 0A`, 140.075 B y 115.438 B, abiertos y revisados visualmente (es, datos ficticios) |

### 8.2 Evidencia cara inspeccionada (no reejecutada)

- XML de `connectedDebugAndroidTest`: **131 pruebas, 0 fallos, 0 errores y 2 omitidas** (`SettingsE2ETest`, por `Assume` sin key de build), 454,589 s, AVD `Pixel_10_Pro` API 37, `timestamp 2026-10-24T08:30:39Z`.
- Desglose relevante: `PantryRecipesE2ETest` **8/8** (S1, S2, S3, S4, S6, S7, S8 y RF-MRC8), `AiSuggestionsE2ETest` **8/8** (reapuntado a la nueva pantalla como origen) y `ScanPantryE2ETest` **22/22** (incluye `scannerNoLongerShowsCompatibleRecipesOrTheAiSuggestionButton`).
- La evidencia es **posterior al diff**: los E2E terminaron a las 10:30 (local) y las últimas ediciones de código/test son de 10:15-10:17; no hay cambios posteriores en el árbol. Es coherente con la suite actual, así que no se reejecutó.

### 8.3 Verificación manual independiente (`adb`/`uiautomator`, datos ficticios del AVD, sin llamadas a la IA)

- **S1 / RF-SDR6**: en Mis Recetas, acción de top bar con `contentDescription` «Recetas con mi Despensa» (nodo `[872,216][944,288]`); al tocarla se abre la pantalla. Se ven «Recetas con mi Despensa», «Recetas Compatibles» y las tarjetas ordenadas **Borrador compatible 100 %**, **Tortilla de patatas 100 %**, **Pechuga al horno 86 %** con `2 de 2`, `3 de 3` y `6 de 7 ingredientes en la Despensa`.
- **S2 / D7**: tap en «Tortilla de patatas» → Detalle real (título, tiempo total, 320 kcal, ración base 4, «Empezar Modo Cocina»); `atrás` → «Recetas con mi Despensa» con la lista intacta; `atrás` → Mis Recetas.
- **S3 / RF-SDR4**: con Despensa no vacía, «Sugerir receta con IA» habilitado abre la pantalla real `ai_suggestions` con los 8 Alimentos del AVD preseleccionados; `Volver` regresa a «Recetas con mi Despensa». Sin llamada a la IA.
- **S5 / RF-SDR7**: FAB «Escanear la Despensa» → Escáner; **0 hits** de «Recetas Compatibles»/«Sugerir receta con IA»; alta manual de «Manzana», chip con cantidad y aviso de duplicado intactos; «Guardar en la Despensa» navega a la Despensa y el Alimento aparece.
- **S7 / RF-SDR8**: con locale de app `en-US` se leen «Recipes with my Pantry» (título y `contentDescription` de la acción), «Compatible Recipes», «2 of 2 ingredients in the Pantry», «Suggest a recipe with AI» y «Back». Locale restaurado a `es-ES`.
- **S4 y S6/S8**: no repetidos a mano (S4 exige vaciar la Despensa sembrada); quedan cubiertos por `aiSuggestionIsDisabledWhenPantryIsEmpty`, `listUpdatesReactivelyWhenPantryAndRecipesChange`, `recipesBelowThresholdAndWithoutRequiredIngredientsAreNotListed` y el unit de `hasFoods`.
- **Nota de entorno**: las pruebas manuales guardaron un Alimento ficticio «Manzana» (sin cantidad) en la Despensa del AVD; el intento de borrarlo desde la pantalla Despensa no surtió efecto (se confirmó el diálogo y el ítem seguía al reabrir). No afecta al repositorio, pero queda en el dispositivo.

### 8.4 Requisitos y alcance

RF-SDR1 a RF-SDR8 cumplidos, con RF-MRC8 conservada (botón visible y habilitado también con compatibles) y D2 aplicada (`hasFoods` solo con Despensa persistida). Non-goals respetados: sin bottom navigation, sin cambios en Despensa ni en el guardado del Escaneo (RD13), sin tocar `calculateMatch`, `MatchBadge`, el umbral 85 %, Room ni el flujo de IA; `git diff` no toca `domain/`, `data/` ni `build.gradle.kts`. El dominio sigue siendo la única fuente de la regla (`compatibleRecipes()` reutilizado) y la UI solo presenta estado.

### 8.5 Hallazgos

Sin hallazgos Critical, High ni Medium. Notas Low no bloqueantes:

- **L1 — Cobertura E2E de la entrada y la ruta**: `PantryRecipesE2ETest` monta `RecommendationsScreen` directamente y no ejercita la acción `action_open_pantry_recipes` ni el registro de `pantry_recipes` en `AppNavHost`; la entrada y el back stack Detalle → pantalla → Mis Recetas se verificaron a mano (la spec lo asigna a la comprobación manual). Recomendación futura: un E2E sobre `MyRecipesScreen`/NavHost que pulse la acción y afirme la pantalla destino.
- **L2 — S6 sin la rama "editar"**: la reactividad se cubre al añadir/eliminar Alimentos y crear/eliminar Recetas; la edición no tiene caso propio (sí emite cambio de lista). Cobertura razonable para el alcance.
- **L3 — Numeración de `PROGRESS.md`**: la sección «Siguiente paso» mezcla 1-3 con líneas históricas numeradas 2/1/1; cosmético, previo en parte, útil de ordenar en el cierre.
- **L4 — Estado vacío transitorio**: `RecommendationsUiState` arranca vacío sin flag de carga, así que puede verse un instante el estado vacío antes de la primera emisión (mismo patrón que la pantalla predecesora aceptada). Impacto visual mínimo.
- **L5 — Chips del Escaneo en Sugerencias**: `AiSuggestionsViewModel` no cambia y sigue leyendo `ScanDraftHolder` + Despensa; abierto desde la nueva pantalla, unos chips sin guardar de una visita previa al Escáner pueden aparecer preseleccionados aunque el gating (`hasFoods`) solo cuente la Despensa persistida. Es el comportamiento que la spec decide conservar; se anota como posible pulido de UX.

### 8.6 Seguridad

Checklist específica de la feature sin hallazgos: sin secretos ni API keys en el diff, capturas o evidencia; sin permisos, dependencias ni llamadas de red nuevas; sin superficie de entrada nueva (no hay campos de usuario en la pantalla); las miniaturas se leen de la ruta interna de `PhotoStore` ya existente; sin logs nuevos. No se ejecutó ninguna llamada a DevExpert Inference durante la validación. `RK03`/`S05` siguen como puertas de release sin cambios.

### 8.7 Documentación y harness

`DESIGN.md` (pantalla nueva, Escáner sin compatibles y estados), `docs/specs/mostrar-recetas-compatibles.md` (nota de sustitución de RF-MRC5 sin borrar historia), `feature_list.json` (solo `status`/`evidence`), `PROGRESS.md` y `docs/progress-history.md` al día. `docs/domain-model.md`, `docs/technical-discovery.md`, `AGENTS.md` y `CONSTRAINTS.md` correctamente no tocados. Presupuesto de evidencia respetado (1 informe + 2 PNG). Sin procesos ni ficheros temporales nuevos en el repositorio; `.harness/` conserva solo logs previos.

### 8.8 Recomendación de estado

**`accept`**: el orquestador puede persistir `separar-despensa-y-busqueda-de-recetas` → `accepted` en `feature_list.json`, con un resumen de evidencia que cite este apartado §8 (gates reejecutados, E2E 131/0/0/2 inspeccionada, comprobación manual independiente S1/S2/S3/S5/S7 y 0 hallazgos bloqueantes). Las notas L1-L5 quedan como deuda opcional.
