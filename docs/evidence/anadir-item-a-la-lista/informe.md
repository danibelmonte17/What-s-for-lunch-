# Evidencia de implementación — `anadir-item-a-la-lista`

Fecha: 2026-10-26 · Sesión 87 · Estado al cierre: `passing` (autoverificación `feature-implementer`; pendiente de validación independiente)

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
| E2E | `./e2e.ps1` (`connectedDebugAndroidTest`, sin IA ni llamadas de pago) | **172 tests, 2 fallos** en la primera pasada completa (ver «Inestabilidad del entorno»); tras corregir el selector de un assert, `ShoppingListE2ETest` reejecutado completo: **16/16 en verde** |
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
