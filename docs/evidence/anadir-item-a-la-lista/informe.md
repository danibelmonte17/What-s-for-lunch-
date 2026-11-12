# Evidencia de implementación — `anadir-item-a-la-lista`

Fecha: 2026-10-26 · Sesión 87 · Estado al cierre: `passing` (autoverificación `feature-implementer`; pendiente de validación independiente). Repair del `revise` de la sesión 88 añadido el 2026-10-27 (sesión 89): ver «Repair del `revise`» al final.

## Entorno

- Emulador **Pixel_10_Pro (AVD), Android 17 (API 37)**, conectado por `adb`.
- JDK: Corretto 17.0.20 (`JAVA_HOME` se ejecuta sobreescrito a la raíz `C:\Users\Daniel\.jdks\corretto-17.0.20`; la variable del entorno incluye `\bin` por error y Gradle la rechaza).
- App instalada con `installDebug` para las capturas; locale de la app `es-ES` (`cmd locale set-app-locales`).
- Sin llamadas reales a DevExpert Inference: la feature no usa IA y las E2E usan fixtures locales. Sin coste y sin API keys en ficheros, logs ni evidencia.
- Sin migración de Room: la BD sigue en **v3** (la columna `shopping_items.aisle` ya existía en el esquema v3).

## Gates (comandos y resultados)

| Gate | Comando | Resultado |
|---|---|---|
| Entorno | `./init.ps1` | **Verde** («Entorno listo», app instalada y en ejecución en el AVD; BD v3 abre con datos previos) |
| Lint | `./lint.ps1` (`lintDebug`) | **0 errores / 49 avisos** — igual que la línea base, sin avisos nuevos |
| Unit | `./unit.ps1` (`testDebugUnitTest`) y `./gradlew testDebugUnitTest --rerun-tasks` (punto 3 del plan) | **Verde**: 35 suites, 216 tests, 0 fallos (incluye `ShoppingListManualItemTest` con 7 tests, +2 en `InMemoryShoppingListRepositoryTest` y +2 en `ShoppingListViewModelTest`) |
| E2E | `./e2e.ps1` (`connectedDebugAndroidTest`, sin IA ni llamadas de pago) | **170 tests** (168 ejecutados + 2 omitidos por `Assume`), **2 fallos** en la primera pasada completa (ver «Inestabilidad del entorno»); tras corregir el selector de un assert, `ShoppingListE2ETest` reejecutado completo: **16/16 en verde**. *Cifra corregida el 2026-10-27 (repair sesión 89): la versión anterior de esta fila decía «172 tests», que era el contador de consola de AGP (cuenta dos veces los 2 omitidos; ver L1 en «Repair del `revise`»).* |
| Build | `./gradlew assembleDebug` | **Sin errores** |
| Paridad es/en | recuento de claves `<string>` de `values/` y `values-en/` | **281/281 claves idénticas** (16 nuevas por archivo: diálogo, validaciones, Pasillos y badge) |

Nota de entorno: `init.ps1`/`e2e.ps1` deben ejecutarse **sin canalizar stdout** (falso fallo conocido de `adb shell monkey`). Las propiedades Gradle con `=` deben ir entrecomilladas (`"-Pandroid.testInstrumentationRunnerArguments.class=…"`), o el intérprete las parte.

## Inestabilidad del entorno (documentada, sin relación con la feature)

La primera pasada completa (`./e2e.ps1`, 25 min) y una reejecución mostraron dos fallos **ajenos a la lógica de esta feature**:

1. `AiSuggestionsE2ETest > mixedStatesCoexistAndANewListStartsEverySuggestionAsProposed` — «Activity never becomes requested state [DESTROYED] (last lifecycle transition = PAUSED)» en el **teardown** del `createComposeRule` (cierre del ActivityScenario), no en el cuerpo del test. Esa suite no compone `ShoppingListScreen`. Reejecución aislada: **ver resultado abajo**.
2. Una reejecución posterior abortó con `INSTRUMENTATION_ABORTED: System has crashed` (crash del system_server del AVD a mitad de carrera, minuto 6), tocando al test previo `emptyListShowsTheScanPantryCta`; ese mismo test pasó en la batería completa y en la reejecución limpia siguiente.

Reejecución limpia de `ShoppingListE2ETest` (tras el crash del AVD): **16/16 en verde, 10m07s**. Reejecución aislada de `AiSuggestionsE2ETest#mixedStatesCoexistAndANewListStartsEverySuggestionAsProposed`: **1/1 verde (49 s)** → el fallo 1 queda clasificado como inestabilidad del teardown (`createComposeRule`/`ActivityScenario`), sin relación con esta feature.

## Desglose de escenarios (`ShoppingListE2ETest`, es-ES, fixtures locales)

Ejecución completa de la clase: **16/16 en verde** (10 casos previos sin regresión + S1–S6).

| Escenario | Test | Resultado |
|---|---|---|
| S1 / verify 1 «E2E: añadir un artículo a mano y comprobar que aparece en la Lista de la Compra» | `manualAddShowsTheItemWithAisleLabelAndManualBadge` | Verde: «Pan (2 uds)» con etiqueta «Despensa & Aceites» y badge «Despensa baja» |
| S2 duplicado por nombre normalizado | `manualAddOfAnExistingNameEditsItWithoutCreatingANewRow` | Verde: una sola fila «Harina (500 g)» con «Despensa & Aceites» y badge «Para: Tortilla» (fuentes intactas) |
| S3 edición por fila (nombre inmutable) | `editingAnItemOnlyChangesQuantityUnitAndAisle` | Verde: «Azafrán (1 g)» pasa a «Lácteos» conservando «Para: Paella» y sin tocar nombre/cantidad |
| S4 validación inline (RF-AIL5) | `invalidNameOrQuantityShowsAnErrorAndKeepsTheDialogOpen` | Verde: «Escribe un nombre» y «Escribe una cantidad válida»; diálogo abierto y 0 ítems guardados |
| S5 opcionales por defecto | `manualAddWithoutQuantityOrAisleUsesTheDefaults` | Verde: «Leche» sin cantidad y etiqueta «Sin clasificar» |
| S6 alta desde el estado vacío | `emptyStateAddCtaOpensTheDialogAndSavesTheItem` | Verde: «Añadir ítem» del estado vacío abre el diálogo y guarda |

## Edición sin cascada y persistencia (punto 6 del plan)

`roomRepositoryKeepsItemsAcrossReopenAndRestoresDeletedSources` se extendió con la comprobación sobre **BD v3 previa con un ítem de Faltantes** («Azafrán», fuente «Paella»):

1. La BD v3 con el ítem aportado por Receta **abre sin migración** (sigue en v3).
2. `saveManualItem` sobre ese ítem (cantidad 1→2 g, Pasillo `DAIRY`) **no borra las fuentes ni el badge**: tras cerrar y reabrir la BD (equivalente a reiniciar la app), la fila conserva nombre, `sources = [Paella]` y solo cambia cantidad, unidad y Pasillo.
3. Borrado con «Deshacer» restaura el ítem editado completo (incluidas fuentes y Pasillo).

La escritura Room reescribe solo los ítems que cambian (diff por igualdad) y **reinserta las fuentes en la misma transacción**, de modo que sobreviven al `INSERT OR REPLACE` de Room (que dispararía `ON DELETE CASCADE` de `shopping_item_sources`). Este es el mecanismo por el que la edición manual nunca pierde el Origen.

## Capturas (app instalada, locale es-ES)

| Fichero | Contenido |
|---|---|
| `dialogo-alta-es.png` | Diálogo «Añadir ítem» con nombre, cantidad, unidad y Pasillo «Despensa & Aceites» seleccionado |
| `lista-es.png` | Lista resultante: «Pan (2 uds)» con badge «Despensa baja» + etiqueta «Despensa & Aceites», y «Harina (500 g)» (ítem de Receta editado a mano) con «Despensa & Aceites» y badge «Para: Tortilla» intacto |

Preparación de las capturas (comprobación puntual con `adb`/`uiautomator`, sin IA ni coste): el escenario de partida se preparó ejecutando el test S2 de `ShoppingListE2ETest` (`manualAddOfAnExistingNameEditsItWithoutCreatingANewRow`) con `am instrument` sobre los APKs instalados (sin Gradle), de modo que todo el estado salió de **flujos reales de la app**: la receta «Tortilla» aporta «Harina» a la Lista y el propio flujo la edita con el diálogo real (200→500 g y Pasillo «Despensa & Aceites») conservando el badge «Para: Tortilla». Después, con la app instalada (`installDebug`) y locale `es-ES`, se añadió «Pan (2 uds)» con Pasillo en el diálogo (captura 1) y se capturó la lista resultante (captura 2). Capturas con `adb exec-out screencap -p` y redirección binaria **dentro** de `cmd /c "… > fichero"` (la redirección de PowerShell escribe UTF-16LE y corrompe el PNG); verificada la firma PNG (`89 50 4E 47 0D 0A 1A 0A`) y el tamaño 1280×2856. Nota: la siembra SQL directa no fue posible (el AVD no incluye binario `sqlite3`); `uiautomator dump` es irregular con la ventana del diálogo («null root node»), y las comprobaciones se hicieron con captura de pantalla.

## Alcance

- **Incluido** (RF-AIL1–RF-AIL7): FAB «Añadir ítem» + alta en el estado vacío, diálogo con nombre/cantidad/unidad/Pasillo (5 opciones), validación inline, upsert por nombre normalizado (D3), edición por fila con nombre de solo lectura (D7), etiqueta de Pasillo por fila, badge «Despensa baja» para ítems sin fuentes (RD09/D5), 16 cadenas es/en.
- **Fuera de alcance** (según la spec): voz (D1), barra rápida y chips «Sugerencias» (D2), agrupación, checkboxes y progreso (`agrupar-y-completar-la-lista`), renombrar ítems, conversiones de unidades, tope 99, migración de Room (sigue v3).

---

## Repair del `revise` de la sesión 88 (sesión 89, `feature-implementer`, 2026-10-27)

Estado al cierre: **`passing`** (autoverificación; **no es aceptación** — pendiente de revalidación independiente con `feature-validator`). Aplica el repair brief de `docs/validations/anadir-item-a-la-lista.md` §7 (H1 y L1).

### H1 (High) — prellenado estable del campo Cantidad

Causa: `ShoppingItemDialog` prellenaba Cantidad con `formatQuantity` (formato de **lectura**: agrupación de millares por locale y máximo 3 decimales) y `parseManualQuantity` reinterpreta el texto sin tolerar la agrupación → ida-y-vuelta corrupto (1200 → «1.200» → 1.2; 1234.5 → «1.234,5» → error fantasma «Escribe una cantidad válida»; 0.1234 → «0,123»).

Fix (solo el prellenado, como pide la nota N2):

- Nueva `formatEditableQuantity(quantity, locale)` en `ui/shoppinglist/ShoppingListScreen.kt`: `BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString()` — sin agrupación de millares, sin redondeo y exacta para cualquier `Double` — usando la coma del locale como separador decimal cuando existe (D6 admite coma o punto; cualquier otro caso cae en punto, siempre parseable).
- `ShoppingItemDialog` prellena con `formatEditableQuantity`. **Sin cambios**: `formatQuantity` (filas de la Lista y Detalle de Receta), `parseManualQuantity` (D6) y el formato de lectura de las filas.

### Tests (3 unit + 1 E2E)

- `ShoppingListManualItemTest` (+2): `editableQuantityRoundTripsThroughParsingInEsAndEn` — ida-y-vuelta `parseManualQuantity(formatEditableQuantity(q, locale)) == q` para **1200.0, 1234.5 y 0.1234** con locales **es y en** — y `editableQuantityHasNoThousandsGroupingAndKeepsAllDecimals` («1200», «1234,5», «0,1234» en es; «1234.5», «0.1234» en en).
- `ShoppingListViewModelTest` (+1): `editingKeepsLargeAndPreciseQuantitiesExactly` — edición por id pasando el texto real del diálogo (1200.0 / 1234.5 / 0.1234) conserva el valor exacto en el repositorio (comprobación de edición con cantidad > 999 que pedía el brief, en el test del ViewModel).
- `ShoppingListE2ETest` (+1): `editingKeepsALargeQuantityUnchangedThroughTheDialog` — alta de «Leche» **1500 ml**, edición por fila sin tocar la cantidad y guardado: el diálogo se cierra y la fila conserva la cantidad (regresión de H1 en la UI real; 1/1 verde aislado en 26 s y 17/17 con la clase).

### Punto 2 opcional del brief: no aplicado (justificado)

No se refuerza `parseManualQuantity` eliminando separadores de millares del locale: cambiaría D6 (hoy «1.200» = 1.2, con coma o punto decimales; con tolerancia de millares en es sería 1200) y el prellenado ya no produce agrupación, con lo que el ida-y-vuelta es estable sin tocarlo.

### Gates (AVD `Pixel_10_Pro` API 37, `emulator-5554`, 2026-10-27)

| Gate | Comando | Resultado |
|---|---|---|
| Entorno | `./init.ps1` | Verde («Entorno listo», app instalada y en ejecución) |
| Unit | `./gradlew :app:testDebugUnitTest --rerun-tasks` | **35 suites / 219 tests / 0 fallos** (+3) |
| Lint | `./lint.ps1` (`lintDebug`) | **0 errores / 49 warnings** (línea base intacta) |
| Build | `./gradlew assembleDebug` | Sin errores |
| E2E | `./e2e.ps1` batería completa | **171 tests: 168 en verde, 1 fallo (flake intermitente, ver abajo), 2 omitidos** (`SettingsE2ETest` por `Assume`), 628,2 s; `ShoppingListE2ETest` **17/17** (S1–S6 sin regresión + el test nuevo de H1). XML conservado: `e2e-bateria-2026-10-27.xml` |
| Paridad es/en | recuento de claves `<string>` | **281/281** (sin cadenas nuevas) |

### L1 (Low) — cifra de la batería cuadrada y XML conservado

El inventario de `@Test` de `app/src/androidTest` era **170** en la sesión 87 (164 de la sesión 85 + 6 de S1–S6) y es **171** hoy (+1 por el test E2E de H1). El resumen de consola de AGP cuenta **dos veces** los 2 tests omitidos por `Assume` (`SettingsE2ETest`): en la batería de hoy el XML dice `tests="171"` y la consola imprime «Finished **173** tests» (+2). El mismo +2 explica la cifra «172 tests» de la sesión 87 (170 + 2): era el contador de consola, no el recuento real. **Corrección**: la primera pasada de la sesión 87 ejecutó **170 tests** (168 ejecutados + 2 omitidos); queda corregida en §Gates arriba y en `PROGRESS.md`. El XML de esa pasada no sobrevivió (los resultados de `connected` se regeneran en cada ejecución); el XML de esta batería sí se conserva: `e2e-bateria-2026-10-27.xml` (`tests="171" failures="1" errors="0" skipped="2"`, `time="628.231"`, timestamp `2026-10-27T08:45:22`), con `ShoppingListE2ETest` en 17/17.

### Inestabilidad del entorno en esta pasada (ajena al diff)

La batería falló en `RecipeDetailE2ETest#servingsSelectionIsNotPersistedWhenReopeningDetail` con un fallo **del cuerpo del test** (`RecipeDetailE2ETest.kt:473`): `java.lang.AssertionError: Failed to inject touch input` — el finder no encuentra `recipe_detail_servings_decrease` en el árbol de semantics fusionado («the unmerged tree contains '1' node … Are you missing `useUnmergedNode = true`») — y la reejecución de la clase completa falló en `loweringServingsHalvesIngredientQuantities` con el fallo de **teardown** ya documentado en «Inestabilidad del entorno» («Activity never becomes requested state [DESTROYED]» en `ActivityScenarioRule.after`/`createComposeRule`). **Dos tests distintos con dos firmas distintas**, ambos intermitentes y sin relación con este repair (el diff solo toca el prellenado de `ShoppingItemDialog` y tests de la Lista). Ambos reproducidos **en verde aislados** (1/1 en 22 s y 1/1 en 27 s; la revalidación independiente de la sesión 90 volvió a reproducir `servingsSelection…` en verde aislado, 4,2 s), con el patrón de clasificación ya aceptado en la sesión 88. *(Firma corregida el 2026-10-27 por F1 de la revalidación: la versión anterior de este párrafo atribuía ambos fallos al teardown, en contra del `<failure>` del XML conservado.)*

### Cambios y cierre

- Código: `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` (+21/−1: `formatEditableQuantity` y su uso en el prellenado).
- Tests: `app/src/test/.../ShoppingListManualItemTest.kt` (+2 tests), `app/src/test/.../ui/shoppinglist/ShoppingListViewModelTest.kt` (+1), `app/src/androidTest/.../ShoppingListE2ETest.kt` (+1).
- Evidencia: esta sección + `e2e-bateria-2026-10-27.xml`; capturas sin cambios (el fix no altera la UI visible). Docs durables sin cambios (RD19, D6, `DESIGN.md` intactos). Sin secretos/PII, sin dependencias ni permisos nuevos y sin llamadas a la IA.
- `feature_list.json`: `status` → `passing` con la evidencia del repair (solo `status`/`evidence`). **Siguiente**: revalidación independiente con `feature-validator` (ida-y-vuelta de cantidades ≥ 1000 y >3 decimales, regresión de S1–S6 y cifras de los gates).
