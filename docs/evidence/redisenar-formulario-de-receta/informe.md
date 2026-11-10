# Evidencia — `redisenar-formulario-de-receta` (implementación, sesión 123, 2026-11-09)

Spec: `docs/specs/redisenar-formulario-de-receta.md` (S1–S6 con verify 1–4 de `feature_list.json`; decisiones D1–D8 del usuario + derivadas D9–D12). Emulador usado: AVD `Pixel_10_Pro_MoverOpciones` (API 37, `emulator-5556`). El `emulator-5554` (Pixel_10_Pro) murió a mitad de sesión y se reemplazó por este, libre.

## Qué se implementó

- **Modelo/Room v7 (D3/D5/D6/D9/D10)**: `Recipe.cookMinutes` y `RecipeIngredient.optional` eliminados; `MIGRATION_6_7` recrea `recipes` con `prepMinutes := MIN(preparación + cocción, 1439)` e `ingredients` sin `optional`, y normaliza `steps.timerMinutes = 0` a `NULL`. Registrada en `AppModule` y en todos los tests que construyen la BD.
- **Dominio**: `calculateMatch` cuenta todos los Ingredientes (0 Ingredientes → `null`, D12); `missingRequiredIngredients` sin filtro de opcionalidad; `RecipeValidation` acota duraciones a 0..1439 (temporizador: `null` o 1..1439).
- **Contrato de IA (D8)**: `RECIPE_CONTRACT` sin `cm` ni `o`; parser y normalización sin esos campos (claves desconocidas ignoradas).
- **Formulario (D1/D2/D7)**: «Detalles de cocina» en dos bloques —fila con reloj de Tiempo de preparación (`ui/components/DurationPickerDialog.kt`, TimeInput M3 en modo entrada HH:MM 24 h) y stepper de Raciones, Nivel en horizontal debajo—; fila de Ingrediente sin casilla «Opcional»; fila de Paso con texto en caja blanca (`whiteFieldColors()`), temporizador por el mismo reloj y control «Sin temporizador» (los Pasos nuevos nacen sin temporizador).
- **Ripple visual (D3/D5)**: Detalle sin desglose de cocción ni píldora «Opcional»; `MyRecipesScreen`, `PincheScreen` y `AiSuggestionsScreen` sin cocción ni píldoras.
- **Strings**: 8 claves retiradas y 4 nuevas (`create_recipe_prep_time_label`, `create_recipe_step_no_timer`, `time_picker_confirm`, `time_picker_cancel`) en es/en; paridad **345/345** (340 `string` + 5 `plurals`, diff 0).

## Gates

- `init.ps1`: «Entorno listo» (app en ejecución; sin el falso negativo de `monkey`).
- `lintDebug`: **0 errores / 48 warnings** (idéntico a la línea base; una warning nueva por `recipe_detail_ingredient_optional` sin uso se resolvió retirando la clave en ambos locales).
- `testDebugUnitTest`: **336 tests / 0 fallos** (base 305 + fixtures y tests al día; incluye `RecipeValidationTest` ampliado con `MAX_DURATION_MINUTES` y `isValidTimerMinutes`).
- `assembleDebug` + `compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL.

## E2E (fixtures locales y clientes de IA simulados; sin llamadas de pago)

Focalizada sobre las clases tocadas, en verde:

- `CreateRecipeE2ETest` **25/25** (incluye `prepTimeClockAndStepTimerSurviveSavingAndCookFieldIsGone` = verify 1 + verify 3: reloj en modo entrada teclea 1 h 05 → `prepMinutes = 65`, timers `5`/`null` guardados, «Sin temporizador» limpia, ningún campo de cocción).
- `RecipeMigrationTest` **4/4** (incluido `migrationFromV6FoldsCookMinutesAndDropsOptionalColumn`: suma 30+45=75, tope 1439, columna `cookMinutes`/`optional` ausentes, timer 0 → NULL, round-trip).
- `EditRecipeE2ETest` + `EditRecipeRouteE2ETest` + `RecipeDetailE2ETest` + `MatchBadgeE2ETest`: **32/32** (precarga `00:10`/`00:05`/«Sin temporizador», sin campo de cocción ni casilla «Opcional»; Detalle sin desglose ni píldora; badge sobre todos los Ingredientes).
- `AiSuggestionsE2ETest` **17/17** (incluido `allIngredientsCountForTheMatchAndNoOptionalPillIsShown`: 33% = 1 de 3, sin píldora «Opcional»).
- `PantryRecipesE2ETest` **8/8** (Recetas sin Ingredientes fuera de la lista).
- `ShoppingListE2ETest` + `PincheE2ETest` **40/40** (Faltantes = todos los ausentes).

Regresión de las zonas no tocadas (Bloque A y Bloque B): ver resultado abajo.

Flakes ambientales del AVD documentados (fallo con mensaje vacío o `ComposeTimeoutException`, familias conocidas de sesiones anteriores; re-ejecutados en verde o verificados como preexistentes):
- `CreateRecipeE2ETest.removingIngredientRowClearsItsStepLinks` (1.er pase) → pase completo posterior 25/25.
- `ShoppingListE2ETest.manualAddOfAnExistingNameEditsItWithoutCreatingANewRow` → reejecución 40/40.
- `BottomBarE2ETest` (dos firmas distintas: `expected DESTROYED but was STARTED` —carrera del back de salida ya conocida— y fallo vacío de teardown) → re-ejecuciones dedicadas.
- **N4 (preexistente, NO regresión)**: `CookModeTimerE2ETest.resetRestoresTheCurrentMetaAndLeavesItRunning` falla con `ComposeTimeoutException` en el `waitUntil` de la línea 182. Verificado con experimento `git stash` (sin cambios de la feature): el mismo test falla en el árbol preexistente de la rama, así que es un fallo preexistente/entorno, fuera del alcance de `redisenar-formulario-de-receta`. Anotado para el validador.

## Comprobación manual (adb + capturas)

Las 3 capturas es-ES de `docs/evidence/redisenar-formulario-de-receta/` (PNG completos de pantalla, `adb exec-out screencap`) cubren la comprobación manual:

1. `redisenar-formulario-detalles-es.png` — «Detalles de cocina» en dos bloques con el reloj de Tiempo de preparación **abierto en modo entrada** (01:05 tecleado, teclado numérico).
2. `redisenar-paso-temporizador-es.png` — filas de Paso con la caja de texto en blanco, temporizador fijado «00:05» y control «Sin temporizador» (el Paso 2 nace sin temporizador).
3. `redisenar-detalle-sin-opcional-es.png` — Detalle con «65 min · Tiempo total» sin desglose de cocción.

Ningún campo de cocción ni casilla «Opcional» en ninguna pantalla (además afirmado en E2E por tag).

## Observaciones (no bloqueantes)

- **N1 (Low)**: las etiquetas «Hour»/«Minute» del modo entrada del reloj las pinta Material 3 y salen en inglés (recursos internos de la librería sin traducir al español); los botones «Cancelar»/«Aceptar» y el resto de la UI sí están localizados. Fuera del alcance de `strings.xml` de la app.
- **N2 (Low)**: el botón «Sin temporizador» del Paso 2 coincide en texto con el valor del disparador cuando no hay temporizador (por diseño, D7); se distinguen por testTag.
- **N3 (operativo)**: un run interrumpido dejó un `.lck` de UTP en `app/build/outputs/androidTest-results` y un lanzador UTP zombi que bloquearon ejecuciones posteriores (se limpiaron; si se repite: borrar la carpeta del AVD y `Stop-Process` del lanzador `com.google.testing.platform.launcher`). Además, el AVD `Pixel_10_Pro_MoverOpciones` degradó su `system_server` (`Can't find service: package` durante la instalación de APK, paquetes invisibles en `pm list`) y se recuperó con `adb reboot` + espera de boot completo.

## Regresión completa (batería)

**Cerrada el 2026-11-10 en el AVD libre `Pixel_8_Pro` (`emulator-5554`, API 37) con `ANDROID_SERIAL`** (la sesión 123 la dejó pendiente; su AVD ya no estaba):

- **Batería 1** (`e2e-battery-run1-2026-11-10.log`, 12 m 40 s): **246 tests = inventario `@Test`** (25 ficheros) → 243 verdes + 1 omitido (`SettingsE2ETest`/`Assume`) + 3 fallos:
  1. **Fallo real** — `MyRecipesCardsE2ETest#cardWithPhotoShowsCoverBadgeAndMetadata` y `#cardWithoutPhotoShowsMomentBadgeNextToTitle`: el fixture ya no llevaba `cookMinutes` (prep = 10) pero las aserciones seguían esperando «25 min» (prep 10 + cook 15 antiguos). La clase vive en el mismo fichero `CreateRecipeE2ETest.kt` y no entró en la focalizada de la sesión 123. **Corregido a `10 min`** según D11 (la tarjeta muestra solo la preparación).
  2. `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures` — flake conocido documentado en sesiones anteriores.
- **Verificación aislada tras la corrección** (`e2e-cards-fix-and-flake-isolated-2026-11-10.log`): **3/3 en verde** (las dos tarjetas + el flake).
- **Batería final** (`e2e-battery-final-2026-11-10.log`, 13 m 49 s): **246 = 244 verdes + 1 omitido (`Assume`) + 1 fallo** — el mismo flake de `Settings`, **verificado en verde aislado en este mismo árbol**; 0 errores y 0 regresiones. La consola AGP imprime «Finished 247» por el doble conteo conocido; el recuento real es 246.

**Gates re-ejecutados el 2026-11-10 sobre el árbol final**: `lintDebug` **0 errores / 48 warnings** (idéntico a la línea base), `testDebugUnitTest` **45 suites / 336 tests / 0 fallos**, `assembleDebug` + `compileDebugAndroidTestKotlin` BUILD SUCCESSFUL, paridad **340 `string` + 5 `plurals` = 345/345 con diff 0** (`lint-results-debug-2026-11-10.txt`).
