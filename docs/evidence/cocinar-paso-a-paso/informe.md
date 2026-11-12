# Evidencia — Cocinar paso a paso

- **Feature**: `cocinar-paso-a-paso` (`docs/specs/cocinar-paso-a-paso.md`)
- **Fecha**: 2026-10-28 · rama `feature/cocinar-paso-a-paso` · emulador `emulator-5554` (Pixel_10_Pro, API 37, locale es-ES)
- **Alcance**: Modo Cocina paso a paso (RF-CPP1..6, RD22): avance de Pasos, drawer de ingredientes del Paso con cantidades escaladas, herencia de Raciones y «Terminar»; sin temporizador funcional, sin voz, sin persistencia del progreso y sin stepper de Raciones en el Modo Cocina (non-goals respetados).

## Gates

| Gate | Comando | Resultado |
|---|---|---|
| Entorno | `./init.ps1` | verde («Entorno listo») |
| Lint | `lintDebug` | **0 errores, 49 warnings** (baseline 49: sin warnings nuevos) |
| Unit | `testDebugUnitTest --rerun-tasks` | **38 suites / 251 tests / 0 fallos** (baseline 36/239: +2 suites `CookModeRulesTest` y `CookModeViewModelTest`, +12 tests) |
| Compilación | `assembleDebug` | OK |
| Paridad es/en | `values/strings.xml` vs `values-en/` | **301/301 strings + 4/4 plurals** (305 claves idénticas, sin huecos; 8 strings `cook_mode_*` nuevos en ambos idiomas; `cook_mode_title` ya existía) |
| E2E focalizada | `CookModeE2ETest` + `RecipeDetailE2ETest` | **19/19** verde (S1-S7 + la adaptación del Detalle sin regresión) |
| Batería E2E completa | `./e2e.ps1` | **195 tests (193 ejecutados + 2 omitidos por `Assume`) / sin regresión** (la consola de AGP muestra 197 por doble cuenta de los omitidos —convención corregida en la sesión 89—): primera pasada con 4 fallos intermitentes del AVD ajenos a la feature (3 en `PantryManagementE2ETest` con «Failed to inject touch input» y timeout, 1 en `SettingsE2ETest` con «Activity never becomes DESTROYED»); reejecución de ambas clases **10/10 verde**. 2 omitidos por `Assume` en `SettingsE2ETest`, iguales que en la baseline. La focalizada de la feature (19/19) y las suites de CookMode/Detalle están en verde. |

## Escenarios verificados

### Unit (`CookModeRulesTest` 6 tests, `CookModeViewModelTest` 6 tests)

- `CookModeRulesTest` (RD22/D7): las filas del Paso respetan el **orden de ingredientes de la Receta**, escalan la cantidad con `ServingsScaling` (RD03), un Paso sin vínculos da lista vacía (→ «Sin ingredientes»), los ids huérfanos o en blanco se descartan y los ingredientes no vinculados al Paso no se listan.
- `CookModeViewModelTest`: `NotFound` con id inexistente y con Receta sin Pasos (defensivo, D3), primera ración con y sin argumento de Raciones, recorte `clampServings` fuera de 1-12 (RF-CPP4) y `next`/`previous` con límites en el primer y el último Paso (RF-CPP3).

### E2E focalizada (19/19)

- **S1** `startingCookingModeShowsFirstStepWithScaledIngredients` (verify 1): desde el Detalle con Raciones 2, «Empezar Modo Cocina» abre el Paso 1 de 2 con «350 g Patatas» (700 g base escalado) y «Paso Anterior» deshabilitado.
- **S2** `advancingStepUpdatesProgressAndShowsFinishButton` (verify 2): «Siguiente» lleva al Paso 2 de 2, la barra de avance se completa, se muestra «⏱ 5 min» (RF-CPP6) y el botón pasa a «Terminar».
- **S3** `finishingReturnsToDetailKeepingTheChosenServings`: «Terminar» vuelve al Detalle conservando las Raciones elegidas (RF-CPP3/RF-CPP4).
- **S4** `stepWithoutLinksShowsNoIngredientsMessage`: un Paso sin vínculos muestra «Sin ingredientes» sin fallback al resto de ingredientes (D1/D4).
- **S5** `detailCtaIsDisabledWithoutSteps`: un Borrador sin Pasos tiene «Empezar Modo Cocina» deshabilitada (RF-CPP5, D3).
- **S6** `leavingMidCookRestartsAtFirstStepWhenReopening`: salir con «Volver» a mitad y reabrir reinicia en el Paso 1 (D7).
- **S7** `cookModeTextsChangeWithTheLanguage`: los textos cambian al inglés (S7 de la spec).
- `RecipeDetailE2ETest` (12): la CTA abre el Modo Cocina y vuelve al Detalle, el estado vacío de Pasos la deshabilita y el resto de la batería del Detalle (raciones, checklist, métricas) sigue sin regresión.

### Migración / datos

Sin cambios de esquema: no hay migración nueva (los vínculos Paso↔Ingrediente ya existían desde `vincular-pasos-e-ingredientes`).

## Capturas (4, es, PNG 1280×2856)

1. `cocinar-paso-a-paso-01-paso1-drawer-cantidades.png` — S1/RF-CPP2/RF-CPP4: «Paso 1 de 2», tarjeta «PASO 1» con hint «⏱ 10 min», drawer «Ingredientes para este paso» desplegado con «350 g Patatas» (700 g base escalado a 2 raciones, RD03) y dock con «Paso Anterior» deshabilitado.
2. `cocinar-paso-a-paso-02-ultimo-paso-terminar.png` — S2/RF-CPP3/RF-CPP6: último Paso («Paso 2 de 2», barra completa), «⏱ 5 min», drawer con «4 uds Huevos» y botón primario «Terminar».
3. `cocinar-paso-a-paso-03-detalle-raciones-conservadas.png` — S3/RF-CPP4: el Detalle tras «Terminar» conserva «Raciones a preparar: 2 pers.».
4. `cocinar-paso-a-paso-04-detalle-sin-pasos-cta-deshabilitada.png` — S5/RF-CPP5: Borrador sin Pasos con «Empezar Modo Cocina» deshabilitada.

Las capturas se tomaron con `adb shell screencap` sobre el emulador (resolución exacta 1280×2856), sincronizadas con un test instrumentado temporal (`ScreenshotCaptureTest`, eliminado tras la captura) que dejaba cada pantalla en su estado y confirmaba la toma mediante un fichero `ack`; la batería E2E persistente es `CookModeE2ETest`.

## Micro-decisiones de implementación

- **Rutas**: `cook_mode/{recipeId}?servings={servings}` con `servings` como `NavType.StringType` nullable (patrón de `create_recipe?recipeId=`) parseado con `toIntOrNull()`: ausente → Raciones base; fuera de 1-12 → `clampServings` (RF-CPP4).
- **`NotFound` defensivo**: `CookModeViewModel` mapea a `NotFound` tanto el id inexistente como la Receta sin Pasos (D3: «el Modo Cocina no existe para ella»), con test unitario propio.
- **Reutilización de formato**: `ingredientLine` del Detalle pasa a `internal` (`formatQuantity` ya lo era) para que el drawer use exactamente el mismo formato D7 sin duplicar copy; el escalado va por `ServingsScaling` (RD03) desde la regla pura `CookModeRules` (D9).
- **Retirada del destino provisional**: se elimina `ProvisionalScreen.kt` (sus destinos `FORM_SCREEN`/`SCANNER_SCREEN` no tenían consumidores); `COOK_MODE_SCREEN` pasa a ser la ruta real en `AppNavHost`.
- **Adaptación de consumidores del Detalle**: el callback pasa a `onStartCooking(recipeId, servings)` y actualiza `RecipeDetailE2ETest` y `PantryRecipesE2ETest` (la E2E de la lista de la compra monta el Detalle).
- **UI sin reglas de negocio**: el Modo Cocina solo presenta el estado del ViewModel (P25); las filas del drawer las calcula `CookModeRules.stepIngredientRows` en `domain/cookmode/`.
