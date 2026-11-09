# Evidencia de implementación — `mover-escaneo-a-la-despensa`

Fecha de la verificación: 2026-11-08/09.
Entorno: emulador `Pixel_10_Pro` (AVD, API 37, 1280×2856, 480 dpi), `emulator-5554`, rama `feature/mover-escaneo-a-la-despensa`.
Este informe recoge la evidencia de la autoverificación del implementador. La validación independiente corresponde a `feature-validator` y su informe se guardará en `docs/validations/mover-escaneo-a-la-despensa.md`; esta carpeta queda como insumo para esa validación.

## 1. Alcance implementado

Decisiones cerradas en la spec (`docs/specs/mover-escaneo-a-la-despensa.md`, D1–D6):

- **D1 — FABs apilados en la Despensa**: `Column` en el slot `floatingActionButton` de `PantryScreen` con «Escanear alimentos» (`PantryTestTags.SCAN_FOOD`, icono `Icons.Outlined.PhotoCamera`) **encima** y «Añadir alimento» (`ADD_ITEM`) **debajo**.
- **D2 — Lista vacía sin escáner**: `ShoppingListScreen` retira `EMPTY_CTA` «Escanear la Despensa» y el parámetro `onScanPantry` (pantalla, ruta y `AppNavHost`); «Añadir ítem» (`EMPTY_ADD_CTA`) queda como único `Button` primario; título y mensaje intactos.
- **D3 — Despensa vacía con dos CTAs**: `SCAN_FOOD_CTA` «Escanear alimentos» (primario, primero) y `EMPTY_ADD_CTA` «Añadir alimento» (`OutlinedButton`) debajo.
- **D4 — Clave nueva `pantry_scan_food`** es/en y tags `SCAN_FOOD`/`SCAN_FOOD_CTA`/`EMPTY_ADD_CTA`; `action_scan_pantry` se conserva para Mis Recetas.
- **D5 — `launchSingleTop = true`** en `ScannerRoute.onSaved` (navegación a `PANTRY` con `popUpTo(SCAN_PANTRY) { inclusive = true }`), para no duplicar la Despensa en el back stack al escanear desde ella.
- **D6 — Coordinación con `mover-opciones-a-bottom-bar`**: no se implementa aquí; el cambio solo afecta a FABs/estados vacíos y es compatible con esa bottom bar.

Intocables verificados: accesos al Escáner de Mis Recetas (`SCAN_PANTRY_FAB` y CTA del estado vacío), Escáner mismo, dominio/Room y top bar de Mis Recetas.

## 2. Gates estáticos y unitarios

Salidas conservadas en esta carpeta (`gates re-ejecutados 2026-11-09`):

- `lintDebug` (`gate-lintDebug.txt`): **0 errores / 49 warnings** (línea base, sin regresión; recuento desde `app/build/reports/lint-results-debug.xml`).
- `testDebugUnitTest` (`gate-unit.txt`): **42 suites / 305 tests / 0 fallos / 0 errores** (recuento desde `app/build/test-results/testDebugUnitTest/*.xml`).
- `assembleDebug` (`gate-assembleDebug.txt`): **BUILD SUCCESSFUL**, sin errores.
- Paridad es/en: **323/323 claves + 5/5 plurales** (`values/strings.xml` frente a `values-en/`); nueva línea base = 322 + `pantry_scan_food` (D4).

## 3. E2E focalizada (verify 1 = S1 y verify 2 = S2)

`./gradlew connectedDebugAndroidTest` con `ShoppingListE2ETest` + `PantryManagementE2ETest` + `ScanPantryE2ETest` (2026-11-09): **61 tests / 0 fallos / 0 errores / 0 omitidos**, BUILD SUCCESSFUL en 4m 54s. XML: `e2e-focalizada.xml`.

- `ShoppingListE2ETest` 27 (S7 reescrita: Lista vacía sin ningún acceso al Escáner → verify 1).
- `PantryManagementE2ETest` 9 (5 previos + 4 nuevos: S2 con FAB «Escanear alimentos», S4 estado vacío con dos CTAs, duplicado normalizado y vuelta desde el Escáner).
- `ScanPantryE2ETest` 25 (regresión: la vuelta a la Despensa tras guardar sigue cubierta por su flujo).

### Ejecución focalizada previa descartada (locale, no regresión)

El primer intento de hoy falló en 4 tests de `PantryManagementE2ETest` con firma de **idioma**: los tests esperan cadenas en inglés («Edit Tomate fresco», «Delete…», «Back») y la app mostraba español, porque la verificación manual previa había cambiado el idioma interno de la app (selector de Ajustes, persistido en `AppLocaleController`). Tras `pm clear` + reinstalación (locale vuelve al del sistema, `en-US`), la misma selección de tests pasa **61/61**. Es un artefacto del entorno de pruebas, no del código.

## 4. Batería E2E completa

Ejecución por bloques sobre `emulator-5554` (misma línea base que `./e2e.ps1`; los bloques se ejecutaron por separado para aislar un periodo de saturación del host):

- Inventario `@Test` en `app/src/androidTest`: **224** en 25 ficheros (27 clases; `CreateRecipeE2ETest.kt` 26 = 23 + `CreateRecipeAppE2ETest` 1 + `MyRecipesCardsE2ETest` 2; `EditRecipeE2ETest.kt` 14 = 12 + `EditRecipeRouteE2ETest` 1 + `EditRecipeAppE2ETest` 1). Línea base 220 + 4 tests nuevos de `PantryManagementE2ETest`.
- Resultado reconciliado desde los XML (`reconcile.ps1`): **224 tests / 0 fallos / 0 errores / 1 omitido**.

| Bloque (XML) | Tests | Fallos | Omitidos | Clases |
|---|---|---|---|---|
| `e2e-bloque1-pantry.xml` | 44 | 0 | 0 | PantryManagement 9, PantryLocalization 2, PantryRecipes 8, ScanPantry 25 |
| `e2e-bloque2-shoppinglist.xml` | 40 | 0 | 0 | ShoppingList 27, MatchBadge 2, Pinche 11 |
| `e2e-bloque3-localizacion.xml` | 35 | 0 | 1 | AiSuggestions 16, LanguageSwitch 8, MyRecipesLocalization 6, Settings 5 |
| `e2e-bloque4-cookmode.xml` | 25 | 0 | 0 | CookMode 7, CookModeTimer 8, CookModeVoice 10 |
| `e2e-bloque5-recetas.xml` | 44 | 0 | 0 | CreateRecipe 23, EditRecipe 12, DeleteRecipe 9 |
| `e2e-bloque5b-recetas-extra.xml` | 5 | 0 | 0 | CreateRecipeApp 1, MyRecipesCards 2, EditRecipeRoute 1, EditRecipeApp 1 |
| `e2e-bloque6-resto.xml` | 31 | 0 | 0 | RecipeDetail 12, SearchAndFilter 12, AppLaunch 1, CameraX 1, Keystore 3, RecipeMigration 2 |
| **Total** | **224** | **0** | **1** | 27 clases |

- El **1 omitido** es `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (`assumeTrue(buildKeyActive)`), omitido porque este workspace no tiene `AI_API_KEY` en `local.properties` (la clave nunca se escribe en el repo). La línea base de la spec (2 omitidos) correspondía a un build **con** clave: entonces los dos `assumeFalse(buildKeyActive)` se omiten y ese `assumeTrue` corre. Misma familia de `Assume`, contada según la configuración del build; sin regresión.
- **Flake de B4**: el primer bloque de Modo Cocina (`CookModeTimerE2ETest.resetRestoresTheCurrentMetaAndLeavesItRunning`, `ComposeTimeoutException` por saturación de CPU del host con Android Studio y sesiones concurrentes) se reintentó sin tocar el test y pasó **25/25** en 8,5 s.

## 5. Comprobación manual de verify 3 (orden de elementos y textos es/en)

Recorrido con `adb` + `uiautomator dump` y capturas, con la app en español (selector interno) y en inglés (locale del sistema):

| Comprobación | es | en | Veredicto |
|---|---|---|---|
| Lista vacía: solo «Añadir ítem»/«Add item» como CTA primario, sin «Escanear la Despensa» ni otro acceso al Escáner (D2) | ✓ (`verify3-lista-compra.xml`, captura) | ✓ | OK |
| Despensa vacía: CTA «Escanear alimentos»/«Scan foods» primero (y=1678) y «Añadir alimento»/«Add food» debajo (y=1858) (D3) | ✓ (`despensa-vacia-es.png`) | ✓ | OK |
| Despensa con alimento: FAB «Escanear alimentos» **encima** (bounds `[588,2376]`) y FAB «Añadir alimento» **debajo** (bounds `[671,2568]`) (D1) | ✓ (`despensa-con-alimento-es.png`, `verify3-despensa-fabs.xml`) | ✓ | OK |
| Todos los textos nuevos con paridad es/en (clave `pantry_scan_food`) | ✓ | ✓ | OK |

Notas de método:

- Se dio de alta un alimento («Arroz») a mano para ver la Despensa no vacía; el diálogo se reposiciona al escribir, por lo que el botón se localizó por sus `bounds` en cada paso (sin coordenadas fijas).
- En el dump, los FABs aparecen como `android.widget.Button` con sus bounds en el orden correcto pero con `text` vacío (uiautomator no expone el texto de los `ExtendedFloatingActionButton` de Compose en este AVD); el texto visible se verifica en la captura.
- Incidentes de entorno resueltos durante la verificación: (a) otra sesión instaló su build (con bottom bar) en `emulator-5554`; se reinstaló `app-debug.apk` de esta worktree y se confirmó la top bar propia. (b) El emulador se reinició a mitad del recorrido; se relanzó (sin `-wipe-data`, los datos persistieron) y se reinstaló la app.

## 6. Evidencia visual

Tres capturas PNG 1280×2856 en esta carpeta:

- `lista-compra-vacia-es.png` — Lista de la Compra vacía con solo «Añadir ítem» (D2).
- `despensa-vacia-es.png` — Despensa vacía con los dos CTAs, «Escanear alimentos» primero (D3).
- `despensa-con-alimento-es.png` — Despensa con «Arroz» y los dos FABs apilados, «Escanear alimentos» encima (D1).

Dump adicional: `verify3-despensa-fabs.xml` (árbol con los bounds de ambos FABs) y `verify3-lista-compra.xml`.

## 7. Docs durables al día

- `DESIGN.md` (tabla de pantallas: estado vacío de la Lista y FABs de la Despensa) y `docs/domain-model.md` (casuística de la Lista vacía, sin CTA de escaneo) actualizados en la implementación.
