# Informe de validación — `mostrar-badge-de-coincidencia`

- **Fecha**: 2026-10-20 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: `revise` en el ciclo 1 (§5) → **`accept`** en el ciclo 2 de revalidación (§9)
- **Spec**: `docs/specs/mostrar-badge-de-coincidencia.md`.
- **Entorno**: AVD `Pixel_10_Pro` API 37 (density 480 → 3 px/dp), `adb`/`emulator` de `%LOCALAPPDATA%\Android\Sdk`, JDK 17 (`C:\Program Files\Android\Android Studio\jbr`).
- **Diff revisado**: `domain/match/RecipeMatch.kt` (nuevo), `MyRecipesViewModel.kt`, `MyRecipesScreen.kt`, `AppModule.kt`, `values/strings.xml`, `values-en/strings.xml`, `RecipeMatchTest.kt` (nuevo), `MatchBadgeE2ETest.kt` (nuevo), firma del constructor en `MyRecipesSearchDebounceTest`, `SearchAndFilterRecipesE2ETest` y `DeleteRecipeE2ETest`, y `feature_list.json` (`passing` + evidence). Coincide con `Expected File Changes` de la spec; sin cambios en Gradle, harness, `uxdocs/`, `domain-model.md` ni esquema Room.

## 1. Gate reranqueado (todo sobre el árbol actual, sin commitear)

| Comando | Resultado | Detalle |
|---|---|---|
| `./init.ps1` | **exit 1** (artefacto conocido) | `installDebug` BUILD SUCCESSFUL + `monkey` inyecta el launch; el check posterior «la app no esta en ejecucion» es la carrera de `pidof` ya documentada: verificado a mano `adb shell pidof com.whatsforlunch.app` → 25776, app viva. |
| `./lint.ps1` (`lintDebug`) | **exit 0** | BUILD SUCCESSFUL, 0 errores (`.harness/validator-lint.log`). |
| `testDebugUnitTest --rerun-tasks` | **exit 0** | **32/32, 0 fallos** (recuento desde los XML de `app/build/test-results`; `.harness/validator-unit-rerun.log`). Incluye `RecipeMatchTest` 4/4 y `MyRecipesSearchDebounceTest` 2/2 con la firma nueva. |
| `connectedDebugAndroidTest` (suite completa) | **exit 0** | **69/69, 0 fallos, 0 omitidos, 5 m 22 s** en `Pixel_10_Pro(AVD) - 17` (`.harness/validator-e2e-full.log`). Incluye `MatchBadgeE2ETest` y toda la regresión (`crear-receta`, `editar-receta`, `eliminar-receta`, `buscar-y-filtrar-recetas`, `gestionar-alimentos-de-la-despensa`). |

## 2. Ítems de verificación de `feature_list.json` (ejecutados en el emulador)

| Ítem | Resultado | Evidencia |
|---|---|---|
| **Verify 1 — E2E: mitad de obligatorios en la Despensa ⇒ porcentaje en la tarjeta** | **pasa** | `MatchBadgeE2ETest.showsRequiredIngredientPercentageAndReactsToPantryChanges` (en la suite 69/69) + comprobación manual con la app real: BD Room sembrada vía `run-as` (Receta «Pollo con arroz»: obligatorios Pollo/Arroz, opcional Limón; Despensa solo «Pollo») → tarjeta con badge **«50%»**, `content-desc` «Coincidencia con la Despensa: 50%», una sola ocurrencia (captura `docs/evidence/mostrar-badge-de-coincidencia/mis-recetas-badge-50.png`). |
| **Verify 2 — Unit: los opcionales no alteran el porcentaje** | **pasa** | `RecipeMatchTest.calculatesPresenceOnlyFromRequiredIngredients`: 2 obligatorios + opcional «Limón» con la Despensa conteniendo «Pollo» y «Limón» → 50 % (el opcional ni entra en numerador ni en denominador). Rerun independiente 4/4 en verde. |

## 3. Escenarios S1-S7

| Escenario | Resultado | Evidencia |
|---|---|---|
| S1 mitad → «50%» | pasa | Verify 1 (E2E + manual/captura) |
| S2 todos los obligatorios → «100% Match» | pasa | E2E (tras añadir «Arroz»: 2 nodos «100% Match») + manual: Despensa con Pollo y Arroz → badge «100% Match» (captura `mis-recetas-badge-100-match.png`, una sola ocurrencia) |
| S3 opcional no altera | pasa | Verify 2 (unit) + E2E (borrar «Limón» de la Despensa no cambia el 100 %) |
| S4 Despensa vacía → «0%» | pasa | unit `emptyPantryIsZeroAndAllRequiredItemsIsFullMatch` |
| S5 reactividad sin reiniciar | pasa | E2E: la Despensa cambia en caliente y el badge se actualiza en la misma composición (`viewModel.uiState.value.matches…`); el VM combina `observeRecipes()` + `observePantryItems()` |
| S6 sin obligatorios → sin badge | pasa | unit `recipeWithoutRequiredIngredientsHasNoMatch` + E2E (`assertCountEquals(2)` con 3 recetas) |
| S7 nombre normalizado | pasa | unit `roundsHalfUpAndRecognizesNormalizedNames` (« TOMÁte fresco » vs «tomate fresco») + E2E (receta «Tomate» con «tomate fresco» en Despensa → «100% Match») |
| Redondeo 2/3 → 67 % (D2) | pasa | unit `roundsHalfUpAndRecognizesNormalizedNames` |

## 4. Comprobación visual (locale por aplicación `es-ES`; capturas 1280×2856)

Capturas nuevas (presupuesto respetado: 1 informe + 2 capturas):

- `docs/evidence/mostrar-badge-de-coincidencia/mis-recetas-badge-50.png` — tarjeta «Pollo con arroz» con badge «50%» inline en la fila del título antes de la píldora «Borrador».
- `docs/evidence/mostrar-badge-de-coincidencia/mis-recetas-badge-100-match.png` — misma tarjeta con Despensa completa: píldora «100% Match» (fondo verde) antes de «Borrador».

`uiautomator dump` (es):

- Sin foto: badge «50%» bounds `[827,728][957,781]` (≈43×18 dp), `content-desc` «Coincidencia con la Despensa: 50%», tras el título y antes de la píldora de estado → D6 (inline sin foto) ✓, RF-MB4 ✓.
- «100% Match» bounds `[694,764][957,817]`, mismo `content-desc` con 100 %, una sola ocurrencia por tarjeta.

Muestreo de píxeles (3 px/dp; la spec no lo exigía, se hace como refuerzo):

| Rol | Esperado | Muestreado |
|---|---|---|
| Badge 0–99 % fondo `secondaryContainer` | #82F5C1 | `82F5C1` (688/735 px) |
| Badge 0–99 % texto `onSecondaryContainer` | #00714E | `00714E` (548 px) |
| Badge 100 % fondo `secondary` | #006C4A | `006C4A` (599 px) |
| Badge 100 % texto `onSecondary` | #FFFFFF | `FFFFFF` (1474 px) |

## 5. Hallazgos

### Finding 1: el badge aparece dos veces en las tarjetas con foto (D6 incumplido)

- **Severity**: High
- **Evidence**: `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — el badge superpuesto va dentro del `Box` de la portada (líneas 688-695), pero el badge inline de la fila del título (línea 719, `match?.let { MatchBadge(match = it) }`) **no** está guardado por `if (bitmap == null)` como sí lo está `MomentBadge` (línea 703). Demostrado en el emulador con la app real (BD sembrada con `photoPath` y un PNG en `files/`): el `uiautomator dump` de una única tarjeta contiene **2** nodos con `content-desc` «Coincidencia con la Despensa: 100% Match»: `[921,720][1184,773]` (superpuesto arriba-derecha sobre la portada `[48,672][1232,1200]`) y `[694,1256][957,1309]` (inline en la fila del título). No lo captan los tests: `MatchBadgeE2ETest` usa recetas sin foto y `cardWithPhotoShowsCoverBadgeAndMetadata` usa una receta sin ingredientes.
- **Why it matters**: la spec (D6, «dobre ubicación espejo de `MomentBadge`») define ubicaciones **exclusivas**: portada arriba-derecha con foto, fila del título sin foto. Además, toda Receta Publicada exige foto (`RecipeValidation.PHOTO`), así que la tarjeta más habitual del producto muestra el badge duplicado; y dos nodos con el mismo `testTag`/`content-desc` en la misma tarjeta provocarán ambigüedades (`onNodeWithTag` falla por múltiples matcheos) en futuros E2E.
- **Required change**: el badge inline solo debe componerse cuando `bitmap == null`.
- **Suggested implementation**:
  1. En `RecipeCard`, envolver la línea 719 en `if (bitmap == null) { match?.let { MatchBadge(match = it) } }` (mismo patrón que `MomentBadge`, línea 703), dejando el `Spacer` condicional igual.
  2. Añadir un caso E2E de tarjeta **con foto** (reutilizar el helper `writePhoto()` de `CreateRecipeE2ETest`) que afirme `assertCountEquals(1)` sobre `MyRecipesTestTags.RECIPE_MATCH` dentro de la tarjeta.
  3. No tocar el badge superpuesto ni el cálculo de `calculateMatch`.
- **Verification after fix**: `./gradlew connectedDebugAndroidTest` completo en verde; nuevo E2E de tarjeta con foto con un único nodo de badge; dump manual con foto ⇒ 1 ocurrencia de «Coincidencia con la Despensa» por tarjeta.

### Finding 2: el unit «cantidad 0 cuenta (D1)» no asserta el caso obligatorio

- **Severity**: Low
- **Evidence**: `RecipeMatchTest.calculatesPresenceOnlyFromRequiredIngredients` pone `quantity = 0` solo en «Limón», que es opcional; ningún test afirma que un **obligatorio** con cantidad 0 cuente como disponible (el Verification Plan de la spec lo lista explícitamente: «cantidad 0 cuenta (D1)»).
- **Why it matters**: el comportamiento hoy es correcto porque `calculateMatch` nunca lee `quantity`, pero la decisión D1 queda sin red de seguridad: un refactor futuro que filtre por `quantity > 0` rompería D1 en silencio.
- **Required change**: añadir un caso unit que afirme que un obligatorio con cantidad 0 cuenta como disponible.
- **Suggested implementation**: en `RecipeMatchTest`, ingrediente obligatorio «Pollo» + `item("Pollo", quantity = 0)` en la Despensa → `percentage == 100` / `availableRequired == 1`.
- **Verification after fix**: `./gradlew testDebugUnitTest` en verde con el test nuevo.

## 6. Seguridad

Checklist por feature **sin hallazgos**: el diff no introduce secretos, credenciales, red, permisos ni ficheros nuevos; el cálculo es local, puro y sin I/O (`domain/match/`); no toca la API key (`UnconfiguredAiKeyProvider` intacto); el DAO sigue usando solo `@Query` parametrizadas de Room; las capturas y la BD de pruebas contienen solo datos ficticios sembrados por el validador (el dispositivo quedó limpio con `pm clear com.whatsforlunch.app` y el locale de aplicación revocado).

## 7. Alcance, arquitectura, docs y traspaso

- **Alcance**: correcto. Sin umbral 85 %, sin ordenación por Coincidencia, sin carrusel del Escáner, sin desglose de ingredientes, sin cambios en Despensa/Crear Receta/Lista de la Compra. Solo se tocaron los ficheros de `Expected File Changes` (+ este informe y las 2 capturas, dentro del presupuesto de 3 artefactos).
- **Arquitectura**: correcto. Regla de negocio pura en `domain/match/` citando `RD01/RD02/RD10` y `RF-MB*`, sin dependencias de UI; el VM solo orquesta flows (reactividad por `combine` de `observeRecipes()` + `observePantryItems()`); la UI no tiene reglas de negocio; textos es/en en `strings.xml` con claves `my_recipes_card_match*`; identificadores en inglés.
- **Docs duraderos**: correcto según `Durable Documentation Impact`: sin cambios necesarios en `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`/`domain-model.md` (RD01/RD02/RD10 ya existían). El `Validator Checklist` de la spec queda sin marcar a propósito: con veredicto `revise`, D6 y la actualización de `PROGRESS.md` aún no se cumplen.
- **Traspaso**: `feature_list.json` queda como lo dejó el implementer (`passing` + evidence); el traspaso de estado lo gestiona el orquestador. `PROGRESS.md` necesita actualizarse al cerrar (o reabrir) la feature.

## 8. Veredicto

**`revise`** — la feature está muy cerca: los dos ítems de verificación pasan (ejecutados por el validador en el emulador), S1-S7 en verde, tokens visuales exactos por muestreo de píxeles, suite completa 69/69 y sin hallazgos de seguridad. La aceptación la frena el **Finding 1** (badge duplicado en toda tarjeta con foto, incumplimiento de D6 y caso habitual del producto); el Finding 2 es menor y puede resolverse en el mismo pase. Al corregir el guard de la línea 719 y añadir el E2E de tarjeta con foto, la feature queda lista para revalidación (previsible `accept`).

## Implementation Repair Brief

Goal: eliminar el badge duplicado en las tarjetas con foto y cerrar la red de seguridad de D1.

Do:
1. En `MyRecipesScreen.kt` (`RecipeCard`), hacer que el badge inline solo se componga sin foto: `if (bitmap == null) { match?.let { MatchBadge(match = it) } }` (línea ~719, mismo patrón que `MomentBadge`, línea 703).
2. Añadir en `MatchBadgeE2ETest` (o reutilizando `writePhoto()` de `CreateRecipeE2ETest`) un caso de tarjeta **con foto** que afirme `onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).assertCountEquals(1)`.
3. En `RecipeMatchTest`, añadir un test que afirme que un ingrediente **obligatorio** con cantidad 0 cuenta como disponible (D1).

Do not:
- Tocar `domain/match/RecipeMatch.kt` (el cálculo es correcto), el badge superpuesto, `feature_list.json`, scripts del harness ni otras features.
- Añadir umbral 85 %, ordenación ni desglose.

Run:
- `./gradlew testDebugUnitTest` (33+ tests en verde)
- `./gradlew connectedDebugAndroidTest` (regresión completa, hoy 69 tests + el nuevo)

Update:
- `feature_list.json` → mantener/enumerar la evidence nueva y volver a `passing` para revalidación (lo hace el orquestador/implementer)
- `PROGRESS.md` + `docs/progress-history.md` con el estado real

---

## 9. Ciclo 2 — revalidación tras reparación (2026-10-20): veredicto `accept`

El implementer reparó los dos hallazgos del ciclo 1 y devolvió la feature a `passing`. Este segundo ciclo la revalida de forma independiente sobre el árbol actual (sin commitear).

### 9.1 Verificación de las reparaciones en el diff

| Hallazgo del ciclo 1 | Estado | Evidencia en el código |
|---|---|---|
| **Finding 1 (High): badge duplicado en tarjetas con foto** | **corregido** | `MyRecipesScreen.kt` (`RecipeCard`): el badge inline solo se compone tras el guard `if (bitmap == null) { match?.let { MatchBadge(match = it) } }` (líneas 719-721, mismo patrón que `MomentBadge` en 703-707); el badge superpuesto sigue solo dentro del `Box` de portada bajo `if (bitmap != null)` (líneas 688-695). Ubicaciones otra vez exclusivas ⇒ D6 cumplido. |
| **Finding 2 (Low): unit D1 sin caso obligatorio** | **corregido** | `RecipeMatchTest.requiredPantryItemWithZeroQuantityCountsAsAvailable` (líneas 50-57): obligatorio «Pollo» + `item("Pollo", quantity = 0)` en la Despensa → `percentage == 100` y `availableRequired == 1`. |

Cobertura E2E añadida según el brief: `MatchBadgeE2ETest.recipeCardWithPhotoShowsExactlyOneMatchBadge` escribe un PNG real (`writePhoto()` en `cacheDir`), siembra una receta con `photoPath` y un obligatorio disponible, espera a que existan el nodo `RECIPE_PHOTO` y el nodo `RECIPE_MATCH`, y afirma `onAllNodesWithTag(RECIPE_MATCH).assertCountEquals(1)` más `assertIsDisplayed()`. Es exactamente el caso que al ciclo 1 se le escapaba (recetas sin foto).

### 9.2 Gates reranqueados por el validador (ciclo 2, sobre el árbol actual)

| Comando | Resultado | Detalle |
|---|---|---|
| `./lint.ps1` (`lintDebug`) | **exit 0** | BUILD SUCCESSFUL, 0 errores (`.harness/validator2-lint.log`). |
| `gradlew testDebugUnitTest --rerun-tasks` | **exit 0** | **33/33, 0 fallos** contados desde los XML de `app/build/test-results`; `RecipeMatchTest` 5/5 con el test D1 nuevo (`.harness/validator2-unit-rerun.log`). Nota: `unit.ps1` no reenvía argumentos extra a Gradle, así que el rerun se lanzó directamente con `gradlew.bat` (JDK 17 fijado). |
| E2E focalizada `MatchBadgeE2ETest` (`connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=…MatchBadgeE2ETest`) | **exit 0** | **2/2** en `Pixel_10_Pro(AVD) - 17` (`.harness/validator2-e2e-focused.log`): incluye el caso de tarjeta con foto con badge único. |
| `./e2e.ps1` (`connectedDebugAndroidTest` suite completa) | **exit 0** | **70/70, 0 fallos, 0 omitidos, 10 m 48 s** en `Pixel_10_Pro(AVD) - 17` (`.harness/validator2-e2e-full.log`). 69 del ciclo 1 + el E2E de tarjeta con foto; sin regresión en `crear-receta`, `editar-receta`, `eliminar-receta`, `buscar-y-filtrar-recetas` ni `gestionar-alimentos-de-la-despensa`. |

El ciclo 1 se mantiene vigente tal cual: S1-S7 (§3), Verify 1 y Verify 2 (§2), tokens visuales y capturas (§4) y seguridad (§6); el diff del ciclo 2 solo añade el guard `bitmap == null` y los dos tests, sin tocar `MatchBadge`, `calculateMatch` ni el badge superpuesto, así que la comprobación visual y el muestreo de píxeles del ciclo 1 siguen siendo válidos.

### 9.3 Veredicto del ciclo 2

**`accept`** — los dos hallazgos quedan cerrados con evidencia en el emulador (E2E de tarjeta con foto con un único nodo de badge + unit D1 en verde), los gates baratos reranqueados en verde (lint 0 errores, unit 33/33 con rerun), E2E focalizada 2/2 y suite completa 70/70 en `Pixel_10_Pro` API 37. Sin hallazgos nuevos de seguridad, alcance, arquitectura ni documentación. Estado recomendado para el orquestador: `accepted` en `feature_list.json` (evidencia resumida del ciclo 2) y actualización de `PROGRESS.md` + `docs/progress-history.md`.

Checklist de la spec (§ Validator Checklist), marcado al cerrar:

- [x] La implementación se queda dentro del alcance de esta feature.
- [x] Los escenarios de aceptación pasan (S1-S7) y las decisiones D1-D6 se respetan (D6 cerrado con este ciclo).
- [x] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [x] Cobertura E2E persistente añadida (3 tests en `MatchBadgeE2ETest`) y suite completa en verde (70/70).
- [x] `feature_list.json` (`passing` + evidence) y `PROGRESS.md` reflejan el estado real (el traspaso a `accepted` y la nota de PROGRESS los hace el orquestador).
- [x] No se añadió comportamiento de producto ni trabajo de otras features (sin umbral 85 %, sin ordenación, sin carrusel del Escáner).

Nota de traspaso: el fix del ciclo 2 no altera esquema, dependencias ni scripts del harness; `./e2e.ps1` desinstala la app al terminar (reinstalar con `./gradlew installDebug` para comprobaciones manuales con `adb`).
