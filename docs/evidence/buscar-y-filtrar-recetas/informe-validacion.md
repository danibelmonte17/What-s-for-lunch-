# Informe de validación — `buscar-y-filtrar-recetas`

- **Fecha**: 2026-10-19 · **Modo**: delta (ciclo 6). Informe previo del ciclo 5 con **B-4 cerrado** y **B-3 abierto**; resumen del implementador: acotación de la aserción al nodo de texto dentro de la tarjeta.
- **Entorno real**: emulador `Pixel_10_Pro` (**API 37**, 480 dpi → 3 px/dp), locale del sistema `en-US`; app en `es-ES` inyectada por test. AVD válido (≥ API 30).
- **APK**: `app/build/outputs/apk/debug/app-debug.apk` (`com.whatsforlunch.app`, instalado por `init.ps1`).
- **Alcance del delta**: único cambio en `app/src/androidTest/java/com/whatsforlunch/app/CrearRecetaE2ETest.kt` (`cardWithPhotoShowsCoverBadgeAndMetadata`, líneas 468-472: la aserción pasa a `onAllNodesWithText(cardText(R.string.momento_almuerzo), useUnmergedTree = true).filter(hasAnyAncestor(hasTestTag(RECIPE_CARD)))[0].assertIsDisplayed()`, con los imports `filter`, `hasAnyAncestor`, `hasTestTag` y `onAllNodesWithText`). Sin cambios en `main/`, `test/`, `res/`, Gradle ni harness: mtimes del producto en **14:06** frente a **16:19** del fichero de test. Válvula §3.0 cumplida: el cambio cae dentro del check fallido (B-3) y hay informe previo.
- **Resultado**: **B-3 queda CERRADO** y **B-4 ya estaba cerrado**. Todas las verificaciones en verde → **`validated`**.

## 1. Arranque y calidad

| Comando | Resultado | Detalle |
|---|---|---|
| `./init.ps1` | **exit 1** (artefacto de entorno) | `Installed on 1 device`, `BUILD SUCCESSFUL in 15s` y app viva (`pidof` = 9021). El exit 1 lo produce el `NativeCommandError` de PowerShell 5.1 con el banner de `adb shell monkey` en stderr (mismo comportamiento documentado; `./init.sh` equivalente da exit 0). |
| `./lint.ps1` (`lintDebug`) | **exit 0** | `BUILD SUCCESSFUL` (15 s), 0 errores. Sin cambios de producto. |
| `./unit.ps1` (`testDebugUnitTest`) | **exit 0** | 17 tests, 0 fallos (`RecipeSearchFilterTest` 4 + `MisRecetasSearchDebounceTest` 2 + `RecipeValidationTest` 8 + `MisRecetasStateTest` 3). |
| `connectedDebugAndroidTest` filtrado a `MisRecetasCardsE2ETest` | **exit 0** | **2 tests, 0 fallos**: `cardWithPhotoShowsCoverBadgeAndMetadata` y `cardWithoutPhotoShowsMomentBadgeNextToTitle` pasan (B-3 cerrado). Compila y ejecuta. |
| `./e2e.ps1` completo (`connectedDebugAndroidTest`) | **exit 0** | **51 tests, 0 fallos, 0 saltados** (`BUILD SUCCESSFUL in 2m 42s`). Regresión global medida y en verde, incluida `crear-receta`. |

Logs completos: `.harness/{init,lint,unit,e2e-cards-delta,e2e-full-delta}.log`.

## 2. Matriz de trazabilidad (Parte A)

El producto no ha cambiado en toda la validación (ciclos 1-6); la matriz se confirma con los 51 tests E2E en verde y los 17 unitarios.

| RF | Verificación | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | «Cebolla» → Arroz con pollo + Pollo asado | pasa | E2E `searchesIngredientsCombinesCriteriaShowsNoResultsAndResets`; unit `searchesTitlesAndIngredientNames` |
| RF-2 | ráfaga → una actualización 300 ms tras la última pulsación | pasa | unit `searchAppliesOnceThreeHundredMillisecondsAfterTheLastKeystroke` |
| RF-3 | mayúsculas, tildes y espacios exteriores | pasa | unit `ignoresCaseAccentsAndOuterWhitespace` |
| RF-4 | borrar texto con filtro Cena → solo la receta de Cena | pasa | E2E `clearingSearchKeepsMomentFilter` (B-1 cerrado) |
| RF-5 | «Todas», Desayuno, Almuerzo, Cena y Postre | pasa | E2E `filtersByDinnerAndOffersAllMomentsWithAllSelectedByDefault` |
| RF-6 | inicial: buscador vacío y «Todas»; tras cerrar y reabrir | pasa | E2E + manual ciclo 1 (`force-stop` + relanzado) |
| RF-7 | filtrar por Cena → solo la receta de Cena | pasa | E2E `filtersByDinnerAndOffersAllMomentsWithAllSelectedByDefault` |
| RF-8 | cambiar de momento deselecciona el anterior | pasa | unit `selectingAnotherMomentReplacesTheActiveFilterImmediately` |
| RF-9 | «Todas» muestra todos los momentos | pasa | E2E `searchesIngredientsCombinesCriteriaShowsNoResultsAndResets` |
| RF-10 | texto + momento = intersección | pasa | E2E anterior; unit `combinesTextAndMomentAndKeepsRepositoryOrderIncludingDrafts` |
| RF-11 | sin coincidencias → «sin resultados» + acción | pasa | E2E + captura |
| RF-12 | restablecer vacía buscador y selecciona «Todas» | pasa | E2E `searchesIngredientsCombinesCriteriaShowsNoResultsAndResets` |
| RF-13 | colección vacía → estado vacío sin buscador ni filtro | pasa | E2E `emptyCollectionHidesSearchAndFilterControls` (B-1 cerrado) |
| RF-14 | orden de la lista conservado | pasa | unit `combinesTextAndMomentAndKeepsRepositoryOrderIncludingDrafts` |
| RF-15 | navegar al formulario y volver conserva búsqueda y filtro | pasa | E2E `keepsSearchAndMomentFilterWhenReturningWithinTheSession` |
| NFR-1 | textos es/en vía `strings.xml` | pasa | E2E `searchTextsFollowTheEnglishLocale` + `emptyStateTextFollowsTheSpanishLocale` (B-2 cerrado) |
| NFR-2 | objetivos táctiles ≥ 48 dp | pasa | manual ciclo 1: buscador 60 dp; chips y acción 48 dp (UI sin cambios) |

**Resultado**: 15/15 RF y 2/2 NFR en verde. La regresión de la suite de tarjetas de `crear-receta` (B-3) también pasa: `MisRecetasCardsE2ETest` 2/2.

## 3. Fidelidad visual (prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html`)

UI sin cambios en toda la validación; se mantiene la comparación del ciclo 1.

| Elemento del prototipo | Atributo | Implementación | Veredicto |
|---|---|---|---|
| Buscador (`rounded-xl shadow-sm` + `search`) | forma/color/sombra | `OutlinedTextField` M3 (surface, borde outline, 4 dp, icono `Search`) | divergencia **no bloqueante** (la Parte A solo exige «buscador con icono») |
| Chips de momento (`rounded-full bg-primary` / `bg-surface-container-lowest`) | forma/color | `FilterChip` píldora; activo `#A83212` + texto blanco; inactivo `#FFFFFF` + `#59413B` | **igual** |
| Chips con icono y `shadow-sm` | icono/sombra | Sin icono ni sombra | divergencia **no bloqueante** (no exigida) |
| Estado sin resultados (`search_off`, titular + cuerpo, botón `secondary`) | estructura/color | `SearchOff` 48 dp, titular `titleLarge`, cuerpo `bodyMedium`, botón M3 `primary` | divergencia **no bloqueante** (icono, mensaje y acción exigidos) |
| Textos «Todas»/momentos/«Ver todas las recetas» | textos/idioma | Cadenas exactas es/en | **igual** |
| Objetivos táctiles | ≥ 48 dp | 60 dp buscador · 48 dp chips y acción | **igual** |

Capturas vigentes (UI intacta): `mis-recetas-filtro-cena.png` y `mis-recetas-sin-resultados.png` (es, API 37).

## 4. Bugs

### B-3 · Regresión `MisRecetasCardsE2ETest.cardWithPhotoShowsCoverBadgeAndMetadata` — **CERRADO** (ciclo 6)

- La aserción acota la búsqueda de «Almuerzo» al subárbol de la tarjeta: `onAllNodesWithText(cardText(R.string.momento_almuerzo), useUnmergedTree = true).filter(hasAnyAncestor(hasTestTag(RECIPE_CARD)))[0].assertIsDisplayed()`. Resuelve las dos causas acumuladas: ya no asertа texto sobre el `Surface` contenedor (B-3/origen) ni colisiona con el `FilterChip` «Almuerzo» (los chips no cuelgan de `RECIPE_CARD`). `MisRecetasCardsE2ETest` 2/2 y `e2e` completo 51/51 en verde.

### B-4 · El arreglo de B-3 no compilaba — **CERRADO** (ciclo 5)

- `:app:compileDebugAndroidTestKotlin` compila; la suite instrumentada ejecuta los 51 tests.

### B-1 · `BuscarYFiltrarRecetasE2ETest` — **CERRADO** (ciclo 2)

- `clearingSearchKeepsMomentFilter` y `emptyCollectionHidesSearchAndFilterControls` pasan en el emulador. RF-4 y RF-13 en verde.

### B-2 · `BuscarYFiltrarRecetasE2ETest` — **CERRADO** (ciclo 2)

- `searchTextsFollowTheEnglishLocale` y `emptyStateTextFollowsTheSpanishLocale` pasan en el emulador con locale inyectado. NFR-1 en verde.

## 5. Alcance

- Delta limitado a pruebas E2E: solo `app/src/androidTest/java/com/whatsforlunch/app/CrearRecetaE2ETest.kt` (una aserción + 4 imports). Sin cambios en `main/`, `test/`, `res/`, Gradle ni harness (mtimes 14:06 frente a 16:19 del test).
- Sin secretos ni fotos reales en repo, logs o evidencia; sin comportamiento de otras features.
- La spec se modifica solo marcando los checkboxes validados (`- [x]`); `feature_list.json` pasa a `validated` (veredicto en §6).

## 6. Veredicto

**`validated`**: todas las verificaciones en verde (`init`/`lint`/`unit` con los artefactos de entorno conocidos, `MisRecetasCardsE2ETest` 2/2 y `e2e` completo 51/51 con 0 fallos), los 15 RF y 2 NFR pasan, sin divergencia de fidelidad bloqueante y con el alcance correcto. B-1, B-2, B-3 y B-4 quedan cerrados.
