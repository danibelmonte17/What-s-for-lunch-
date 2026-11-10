# Informe de implementación — `unificar-unidades` (2026-11-09)

> Autoverificación del implementador (`feature-implementer`, sesión 119). **No es aceptación**: la validación independiente la realiza `feature-validator`.

## Qué se implementó (según `docs/specs/unificar-unidades.md`, D1–D5)

- **Dominio**: `domain/model/IngredientUnit.kt` — enum cerrado `UD/G/KG/ML/L` con `code` y funciones puras `fromCode` (trim + minúsculas; `null` para vacío/legado) y `normalize` (vacío o desconocido → «ud», D2). `RecipeIngredient.unit` y `ShoppingListItem.unit` siguen siendo `String`: sin Room ni migraciones (D5).
- **UI compartida**: `ui/components/UnitDropdown.kt` — `UnitDropdown` (`ExposedDropdownMenuBox` con `menuAnchor(MenuAnchorType.PrimaryNotEditable)`, campo de solo lectura `singleLine` ≥ 48 dp, menú con `widthIn(min = 160.dp)`, `testTag` por parámetro y `"<tag>_option_<valor>"` por opción) con los cinco valores; la unidad legada —incluida la vacía, rotulada «(sin unidad)»/«(no unit)»— entra como **opción extra seleccionada** (D1). `unitLabel(raw)` presenta la unidad (código → clave `unit_ud`…`unit_l` es/en; legada → texto tal cual; vacía → se omite en línea) y `optionTag` da tags estables a las opciones. `FieldShape` y `warmFieldColors()` se movieron a `ui/theme/Field.kt` para compartirlos con el formulario.
- **Formulario**: `IngredientRow` con `UnitDropdown` (tag `ingredientUnit(index)` conservado; pesos de la fila name 1.2 / unidad 1.1 para que la etiqueta y los valores legados quepan en una línea), «ud» por defecto en filas nuevas (`CreateRecipeViewModel.onAddIngredient`) y `ingredientOptionLabel` compuesto con `unitLabel`.
- **Lista de la Compra**: `ShoppingItemDialog` con `UnitDropdown` (tag `UNIT_FIELD` conservado, «ud» preseleccionado en ítems nuevos, legado como opción extra; forma/colores M3 por parámetro para no romper la estética de sus campos vecinos) y `ShoppingItemRow` compuesto con `unitLabel`.
- **Superficies de lectura con `unitLabel`**: checklist del Detalle (`ingredientLine` gana el parámetro `unit`), drawer del Modo Cocina, tarjetas de Sugerencias, Ficha del Pinche, filas de la Lista y diálogo de vínculos del formulario.
- **IA (D2)**: `SuggestionRules.normalizeOne` y `PincheProposalRules.proposalToRecipe` normalizan la unidad al enum; el contrato JSON compacto no cambia de claves.
- **Textos**: 6 claves nuevas `unit_ud`…`unit_l` + `unit_none` en `values/` y `values-en/` (sin retirar existentes).

## Gates baratos

| Check | Comando | Resultado |
| --- | --- | --- |
| Entorno | `./init.ps1` | «Entorno listo», app instalada y en ejecución (exit 1 = falso negativo conocido de `monkey`; verificado con `pidof`) |
| Lint | `./gradlew lintDebug` | **0 errores / 48 warnings** (= línea base; ninguna incidencia en los ficheros del diff) |
| Unit | `./gradlew testDebugUnitTest` | **43 suites / 313 tests / 0 fallos** (línea base 42/305 + `IngredientUnitTest` 6 + 1 en `SuggestionRulesTest` + 1 en `PincheProposalRulesTest`) |
| Build | `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| Paridad es/en | recuento de claves | **326/326 `string` + 5/5 `plurals`, diff = 0** (6 claves nuevas `unit_*` en ambos) |

## E2E

### Inventario

- **235 `@Test` en 24 ficheros** (árbol final). En HEAD antes de la feature: 231; las 4 nuevas pruebas son `unitDropdownSavesClosedListUnitsWithUdAsTheNewRowDefault` (CreateRecipe), `legacyUnitIsShownAsTextAndOfferedAsAnExtraOption` (EditRecipe), `unitDropdownOffersTheClosedListAndKeepsLegacyUnitsAsExtraOption` (ShoppingList) y `acceptingNormalizesAiUnitsToTheClosedEnum` (AiSuggestions). Nota: la cifra «224» que constaba como línea base en la spec procede de la sesión 114 y no cuadra con el árbol actual (231 en HEAD); se deja constancia de la discrepancia.

### Focalizada (2026-11-09, `emulator-5554`/`emulator-5558`)

- `CreateRecipeE2ETest` + `EditRecipeE2ETest` + `EditRecipeRouteE2ETest` + `ShoppingListE2ETest` + `AiSuggestionsE2ETest`: **83 tests** → 82 verdes + 1 fallo por aserción afectada por D1 («Pan»/«Leche» sin cantidad ahora muestran «(ud)»), corregido en el test y re-ejecutado en verde (log `e2e-focused-83-2026-11-09.log`, tras la corrección las 83 en la batería).
- Regresión `ShoppingListE2ETest` + `RecipeDetailE2ETest` + `CookModeE2ETest` + `PincheE2ETest`: la pasada **se abortó por un crash del AVD** (`Expected 58, received 48` / `INSTRUMENTATION_ABORTED`) tras completar 49: **47/49 verdes**; los 2 fallos de `PincheE2ETest` (`pincheIsLocalizedInEnglish` — teardown `Activity never becomes DESTROYED` — y `emptyPantryShowsEveryRequiredIngredientAsMissing` — `System has crashed`) son ambientales del AVD y **pasaron 2/2 al reejecutarlos aislados** (`e2e-pinche-isolated-rerun-2026-11-09.log`). Los 9 tests no llegados a ejecutar se cubren íntegros en la batería final.

### Batería completa (`./e2e.ps1`)

- Corrida en el AVD libre `Pixel_8_Pro` (`emulator-5558`, API 37) con `ANDROID_SERIAL` para no pisar los emuladores de la sesión paralela (ver Hallazgos).
- **Batería final sobre el árbol final (2026-11-09, 13 m 28 s)**: **235 tests** (=`inventario @Test`, reconciliado) → **233 verdes + 1 omitido** (`SettingsE2ETest`/`Assume` sin `AI_API_KEY`) **+ 1 fallo**: `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures`, el **flake conocido** que **pasó aislado 1/1 en una re-ejecución posterior sobre este mismo árbol** (log `e2e-settings-flake-isolated-final-tree-2026-11-09.log`; el log previo `e2e-settings-flake-isolated-2026-11-09.log` es del pase posterior a la batería 2). 0 errores, 0 regresiones de esta feature (los 4 tests nuevos y todas las clases afectadas en verde). La consola AGP imprime «Finished 236» por el doble conteo conocido; el recuento real es 235.
- Tres pasadas previas documentadas en `.harness`/evidencia: la 1 (83 focalizada, 1 aserción D1 corregida), la de regresión (con los 2 fallos ambientales de Pinche, verificados 2/2 aislados) y la batería 2 (mismo resultado que la final); los logs están en `docs/evidence/unificar-unidades/`.
- **Nota**: el XML `TEST-…xml` de la batería se sobrescribió con la reejecución aislada del flake (mismo artefacto `app/build/outputs/…`); la evidencia durable de la batería es `e2e-battery-final-2026-11-09.log` (contiene «Starting 235», el único `FAILED` nombrado y el resumen).

## Verificación manual / S5 (es/en)

- **S5 (códigos iguales en ambos idiomas, sin unidades hardcodeadas fuera de `strings.xml`)**:
  - Barrido de fuentes `main`: **0 literales** de unidad («ud»/«kg»/«ml»/«(sin unidad)»/«(no unit)») fuera de `IngredientUnit.kt` (que son los códigos canónicos, no textos visibles) y fuera de comentarios; las **6 referencias** `R.string.unit_*` viven solo en `UnitDropdown.kt` (`unitLabel` + `labelRes` + `unit_none`).
  - Captura `03-formulario-desplegable-unidades-en.png`: la app en inglés muestra los mismos códigos (`ud`, `g`, `kg`, `ml`, `l`) y rotula la legada vacía **«(no unit)»**; la captura `01-…-es.png` muestra **«(sin unidad)»** con el resto de la UI en español.
  - El cambio real de idioma en la app real está cubierto por `LanguageSwitchE2ETest` (8/8 en la batería).

## Evidencia visual (1280×2856, es)

1. `01-formulario-desplegable-unidades-es.png` — formulario en edición con las unidades del enum en las filas y el **desplegable abierto** en la fila legada vacía (6 opciones: `ud`, `g`, `kg`, `ml`, `l`, «(sin unidad)»).
2. `02-detalle-unidades-enum-y-legada-es.png` — checklist del Detalle con las cinco líneas: «200 g Harina», «150 ml Leche», «2 ud Huevo», «**1 cucharadas Azúcar**» (legada) y «1 Sal» (legada sin unidad, se omite).
3. `03-formulario-desplegable-unidades-en.png` — mismo desplegable en inglés (S5).

Capturadas con `adb exec-out screencap` sobre el AVD libre con `wm size 1280x2856` (override temporal, restablecido después) mientras un test temporal (patrón de la sesión 111, **eliminado tras extraerlas**) sostenía el estado.

## Hallazgos

1. **UI (captura 1)**: sin `singleLine`, el valor largo («(sin unidad)») se partía en varias líneas y estiraba el campo ~250 dp; el menú heredaba el ancho estrecho del ancla y envolvía las opciones. Corregido con `singleLine` + `widthIn(min = 160.dp)` en el menú; de paso se amplió el peso de la unidad en la fila (0.8 → 1.1, nombre 1.5 → 1.2) para que la etiqueta «Unidad» y los valores legados quepan en una línea. Re-encuadre verificado en las capturas.
2. **Defecto preexistente reparado (autorización del usuario, 2026-11-09)**: `PantryManagementE2ETest#scannerSaveReturnsToThePantryWithTheSavedFoods` estaba roto desde la fusión de las ramas `mover-opciones-a-bottom-bar` (quitó el «Volver» de la Despensa, RF-MOB5) y `mover-escaneo-a-la-despensa` (añadió un test que lo pulsa); no se había detectado porque desde esa fusión solo se corrieron `compile`/`lint`/`unit`/`assemble`. Reparado: la vuelta a Mis Recetas se hace por la pestaña de la barra inferior y se añade la aserción de Despensa no duplicada; clase completa **9/9**.
3. **Flake conocido**: `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures` (`ComposeTimeoutException` 5 s) — familia histórica documentada en `PROGRESS.md`, fuera del diff, **pasa aislado** (1/1 re-ejecutado en verde).
4. **Coordinación de emuladores**: hay una sesión paralela activa en el worktree `feature-crear-mi-perfil` usando emuladores compartidos; a propuesta del usuario, todas las ejecuciones de esta sesión se apuntaron a un AVD libre propio (`Pixel_8_Pro`, `emulator-5558`) con `ANDROID_SERIAL`, lo que también evitó los crashes cruzados (`System has crashed`, `Can't find service: package`) de los primeros pases.
5. **`am instrument` directo** no funciona en este entorno (imprime la ayuda de `am`); se usó Gradle `connectedDebugAndroidTest` con `-Pandroid.testInstrumentationRunnerArguments.class=…#metodo` para los pases de un solo caso (ya funcionaba así en sesiones previas).

## Archivos tocados

Ver `git status`/`git diff --stat`: 3 creados en `main` (`IngredientUnit.kt`, `UnitDropdown.kt`, `ui/theme/Field.kt`), 1 creado en test (`IngredientUnitTest.kt`), 13 modificados en `main` (pantallas, reglas de IA, strings es/en), 5 en tests unit/E2E, docs durables (`domain-model.md`, `DESIGN.md`, `CONTEXT.md`, `technical-discovery.md`), la spec con hallazgos y 9 specs aceptadas con nota de enmienda al final.
