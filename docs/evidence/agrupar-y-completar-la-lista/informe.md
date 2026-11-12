# Evidencia de implementación — `agrupar-y-completar-la-lista`

Fecha: 2026-10-27 · Estado al cierre: `passing` (autoverificación `feature-implementer`; pendiente de validación independiente).

## Entorno

- Emulador **Pixel_9_Pro (AVD), Android 17 (API 37)** (`emulator-5554`), conectado por `adb`.
- JDK: Corretto 17.0.20 (`JAVA_HOME` se ejecuta sobreescrito a la raíz `C:\Users\Daniel\.jdks\corretto-17.0.20`; la variable del entorno incluye `\bin` por error y Gradle la rechaza).
- Las E2E y las capturas componen la app con locale **es-ES** explícito (mismo mecanismo que la suite existente: `createConfigurationContext` con `Locale("es", "ES")`).
- Sin llamadas reales a DevExpert Inference: la feature no usa IA y las E2E usan fixtures locales. Sin coste y sin API keys en ficheros, logs ni evidencia.
- Sin migración de Room: la BD sigue en **v3** — la columna `shopping_items.completed` ya existía en el esquema v3 (`MIGRATION_2_3`) y esta feature solo añade el `UPDATE` dirigido de `completed` (RD20).

## Gates (comandos y resultados)

| Gate | Comando | Resultado |
|---|---|---|
| Lint | `./lint.ps1` (`lintDebug`) | **0 errores / 49 avisos** — igual que la línea base, sin avisos nuevos |
| Unit | `./unit.ps1` (`testDebugUnitTest`) | **232 tests, 0 fallos** (219 de línea base + 13 nuevos: 7 `ShoppingListRulesTest`, 4 `ShoppingListViewModelTest`, 2 `InMemoryShoppingListRepositoryTest`) |
| Build | `./gradlew assembleDebug` | **Sin errores** (`EXIT=0`) |
| E2E enfocada | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.ShoppingListE2ETest"` | **27/27 en verde** (17 previos sin regresión + S1–S10), 0 omitidos |
| E2E batería | `./e2e.ps1` (`connectedDebugAndroidTest`, sin IA ni llamadas de pago) | **181 tests, 0 fallos, 2 omitidos** (`SettingsE2ETest`, `Assume` preexistentes) |
| Paridad es/en | recuento de claves `<string>`/`<plurals>` de `values/` y `values-en/` | **291/291 claves idénticas** (9 nuevas por archivo: 7 `<string>` + 2 `<plurals>`) |

Nota de entorno: las propiedades Gradle con `=` deben ir entrecomilladas (`"-Pandroid.testInstrumentationRunnerArguments.class=…"`), o el intérprete las parte y Gradle las toma como tareas. El contador de consola de AGP cuenta dos veces los tests omitidos por `Assume` (en la batería imprime «183/181»; el XML marca `tests="181"`, `skipped="2"`), patrón ya documentado en la evidencia de `anadir-item-a-la-lista`.

## Desglose de escenarios (`ShoppingListE2ETest`, es-ES, fixtures locales)

Ejecución completa de la clase: **27/27 en verde** (17 casos previos —4 de ellos adaptados por D6— + S1–S10).

| Escenario | Test | Resultado |
|---|---|---|
| S1 agrupación por defecto por Pasillo | `itemsAreGroupedByAisleByDefaultWithUncategorizedLast` | Verde: «Pasillo de Super» seleccionado, cabeceras «Verduras & Frescos»/«Lácteos»/«Sin clasificar» con contadores «1 ítem»/«1 ítem»/«2 ítems», «Sin clasificar» la última y orden del catálogo de Pasillos |
| S2 «Por Receta» con dos Recetas | `itemsFromTwoRecipesAreGroupedUnderTheirRecipe` | Verde: cada ítem bajo la cabecera de su Receta (Gazpacho y Paella, «2 ítems» cada una) y orden de filas por nombre normalizado |
| S3 progreso parcial | `theProgressBarReflectsHalfOfTheItemsCompleted` | Verde: «0 de 4…»/0 % → marcar 2 → «2 de 4 artículos en la cesta»/50 % con barra `ProgressBarRangeInfo(0.5f, 0f..1f)` |
| S4 multi-fuente contado una vez | `multiSourceItemsAppearInEveryGroupAndCountOnce` | Verde: «Harina» en «Gazpacho» y «Paella» (2 filas), marcar una copia marca las dos y el progreso dice «1 de 1 artículo en la cesta»/100 % |
| S5 desmarcar revierte | `uncheckingAnItemRevertsTheProgress` | Verde: el checkbox vuelve a `off`, la fila conserva «Pan (1 ud)» y el progreso baja a «0 de 2 artículos en la cesta» |
| S6 fila completada conserva contenido | `completedRowsKeepTheirContentAndShowTheClearAction` | Verde: «Pan (2 uds)» se conserva (tachado/atenuado en captura), 0 apariciones de «En la cesta» y «Limpiar comprados» solo con ≥1 completado |
| S7 «Limpiar comprados» + «Deshacer» | `clearCompletedRemovesThemAndUndoRestoresThemAll` | Verde: snackbar «Ítems comprados eliminados», quedan 2 filas, «Deshacer» restaura las 4 con marcas, cantidades y badges «Para: Paella» (4 apariciones) |
| S8 contenido de fila por modo (D6) | `rowContentChangesWithTheGroupingMode` | Verde: «Pasillo» sin etiqueta de Pasillo y con badges; «Por Receta» con etiqueta «Lácteos» y sin badge de fuente única; «Para: 2 recetas» en los dos modos (1 y 2 apariciones) |
| S9 persistencia y reinicio de modo | `completedItemsSurviveTheRoundTripAndTheModeResets` | Verde: tras salir y volver a entrar (Room real vía app completa), el checkbox sigue marcado y el selector vuelve a «Pasillo de Super» (D1) |
| S10 edición conserva la marca y lista vacía | `editingKeepsTheCompletedStateAndTheEmptyListHasNoProgressCard` | Verde: editar cantidad conserva el checkbox y el progreso «1 de 1 artículo en la cesta»; al borrar el último ítem desaparecen tarjeta, selector y cabeceras |

Adaptaciones legítimas de tests previos (D6 cambió el contenido literal de fila según el modo, sin cambiar el comportamiento verificado):

- `manualAddShowsTheItemWithAisleLabelAndManualBadge` y `manualAddWithoutQuantityOrAisleUsesTheDefaults`: la etiqueta de Pasillo de la fila se verifica en «Por Receta» (mismo assert de etiqueta y badge).
- `manualAddOfAnExistingNameEditsItWithoutCreatingANewRow`: el Pasillo se verifica en la cabecera del grupo («Despensa & Aceites») en modo «Pasillo», que conserva el badge «Para: Tortilla».
- `editingAnItemOnlyChangesQuantityUnitAndAisle`: el cambio de Pasillo se observa en la cabecera del grupo «Lácteos».

Tests unitarios nuevos (13): reglas de agrupación/progreso (`ShoppingListRulesTest`, +7, incluye RD20 y multi-fuente), estado del ViewModel con `PendingUndo` (`ShoppingListViewModelTest`, +4: modo en memoria, progreso derivado, `setCompleted`/`clearCompleted` con deshacer) y repositorio en memoria (`InMemoryShoppingListRepositoryTest`, +2: `setCompleted`/`restoreItems` con fuentes).

## Capturas (app real, locale es-ES)

| Fichero | Contenido |
|---|---|
| `lista-pasillo-es.png` | Modo «Pasillo de Super» con progreso parcial («2 de 3 artículos en la cesta», 67 % y barra) y dos filas completadas («Leche (1l)», «Harina (700 g)»: título tachado, fila atenuada, checkbox relleno); cabeceras «Lácteos», «Despensa & Aceites» y «Sin clasificar» con contador; badges conservados («Despensa baja», «Para: 2 recetas»); acción «Limpiar comprados» visible en la barra superior |
| `lista-receta-es.png` | Modo «Por Receta» con grupo multi-fuente («Harina» en «Gazpacho» y en «Paella», badge «Para: 2 recetas» en ambas copias y checkbox reflejando el mismo estado) y grupo «Sin receta» al final («Leche», «Toallitas») con etiqueta de Pasillo en cada fila |

Procedimiento de captura (reproducible, sin IA ni coste): test instrumentado **temporal** (`ShoppingEvidenceCaptureTest`, eliminado tras las capturas) que siembra el estado por Room con el repositorio real de Koin, compone la app completa (`WhatsForLunchApp()`) con locale es-ES, marca los checkboxes y cambia el modo con la UI real (composeRule), y guarda PNGs de pantalla completa con `uiAutomation.takeScreenshot()`. Los PNG se escriben en **MediaStore** (`Pictures/`) porque el task `connectedDebugAndroidTest` **desinstala el paquete al terminar** (borrando `filesDir` interno); MediaStore sobrevive y se recupera con `adb pull`. Verificación de cada PNG: firma `89 50 4E 47 0D 0A 1A 0A` y dimensiones **1280×2856**. Nota: la redirección `>` de PowerShell escribe UTF-16LE y corrompe binarios; para binarios se usa `adb pull` o redirección dentro de `cmd /c`. El estado capturado son 3 ítems («Leche» Lácteos completada, «Harina» multi-fuente `paella`+`gazpacho` con Pasillo «Despensa & Aceites» completada, «Toallitas» sin Pasillo pendiente), sin datos personales ni secretos.

## Alcance

- **Incluido** (RF-AGL1–RF-AGL8): agrupación por Pasillo de Super / Receta (D1, en memoria y reiniciable), ítems multi-fuente en todos sus grupos y «Sin receta» al final (D2), checkbox por fila con estado compartido entre copias, tarjeta de progreso con «N de M artículos en la cesta» y porcentaje (D9, RD20), fila completada con tachado + atenuación sin sustituir el nombre (D5), contenido de fila según el modo (D6), «Limpiar comprados» con «Deshacer» (D3/D7), preservación de `completed` ante edición, `addContributions` y deshacer (D10), 9 cadenas es/en.
- **Fuera de alcance** (según la spec): compartir por WhatsApp (D3), snackbar al completar (D4, el checkbox es el deshacer), confirmación previa a limpiar, voz, chips de sugerencia, renombrar ítems, conversiones de unidad, orden por «más reciente», persistencia del modo de agrupación (D1), migración de Room (sigue v3).

## Refinamiento sobre la spec (documentado, sin cambiar decisiones)

La spec modela el buffer de deshacer como `List<ShoppingListItem>`. La implementación usa `PendingUndo(items: List<ShoppingListItem>, bulk: Boolean)` porque hay **dos** mensajes de snackbar distintos («Ítem eliminado» del borrado por fila y «Ítems comprados eliminados» de «Limpiar comprados») y el borrado por fila deshace **uno** mientras que el masivo deshace **todos** los borrados juntos; el flag `bulk` permite elegir mensaje y alcance sin reabrir el diálogo de confirmación descartado (D3). Mismo alcance funcional que la spec; actualización de la spec registrada en su sección de notas.
