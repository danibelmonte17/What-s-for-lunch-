# Feature Implementation Spec: Agrupar y completar la lista

## Source Feature

- `id`: agrupar-y-completar-la-lista
- `area`: funcional
- `depends_on`: anadir-faltantes-a-la-lista (accepted)
- `status`: not_started (a la hora de planificar; la spec no cambia `feature_list.json`)
- `source`: `feature_list.json`

## Goal

La Lista de la Compra deja de ser una lista plana: se **agrupa por Pasillo o por Receta** según el selector «Agrupar por» y cada ítem gana un **checkbox** que lo marca como comprado (`completado`, ya modelado en `ShoppingListItem`/Room v3). Una tarjeta de progreso resume «{completados} de {total} artículos en la cesta» con porcentaje y barra, y una acción «Limpiar comprados» borra en bloque los ítems completados con «Deshacer». El objetivo es comprar por secciones del súper y ver de un vistazo cuánto falta.

## Non-Goals

- **Compartir la lista** (botón «share» del prototipo, p. ej. WhatsApp): fuera de alcance (D3).
- **Voz** (`⚠ S04`), **barra rápida de alta y chips «Sugerencias»** del prototipo (D2 de `anadir-item-a-la-lista`): siguen diferidos.
- **Sustituir el título de la fila completada por «En la cesta»** (D5): la fila conserva su contenido, tachada y atenuada.
- **Persistir el modo de agrupación** elegido (D1): vive en memoria y se reinicia al salir de la pantalla; sin Room ni preferencias nuevas.
- **Ocultar, mover al final o filtrar los ítems completados**: permanecen en su sitio dentro del grupo.
- **Renombrar ítems, conversiones de unidades ni tope 99** (RD18/D7 de `anadir-item-a-la-lista`): intactos.
- **Migración de Room ni cambios en RD01/RD02/RD09/RD10/RD13/RD15/RD18/RD19**: `completed` ya existe en la v3 y las reglas de Faltantes/upsert no se tocan.
- **IA, llamadas al proveedor ni dependencias nuevas**: la feature es solo presentación + estado de completado.

## Job Story

When estoy comprando la lista del súper y quiero recorrerlo por secciones,
I want to ver los ítems agrupados por Pasillo o por Receta e ir marcando lo que ya tengo en la cesta,
so I can comprar solo lo que falta y ver mi progreso de un vistazo.

## Users And Permissions

- Usuario personal (1-5 usuarios, MVP): única interacción; sin roles, sin cuentas, sin permisos Android nuevos.

## Requisitos funcionales

- **RF-AGL1**: tarjeta de progreso con «{completados} de {total} artículos en la cesta», porcentaje redondeado al entero y barra; cuenta **ítems únicos** (los multi-fuente una vez) con independencia del modo.
- **RF-AGL2**: selector «Agrupar por:» con «Pasillo de Super» (por defecto) y «Por Receta»; la elección solo cambia la presentación y se reinicia al salir de la pantalla (D1).
- **RF-AGL3**: modo Pasillo: un grupo por Pasillo con cabecera (nombre + contador de ítems) en el orden de `CONTEXT.md` y «Sin clasificar» al final; solo grupos no vacíos (D8).
- **RF-AGL4**: modo Receta: un grupo por fuente (título de Receta/Propuesta) con cabecera y contador, ítems multi-fuente en **todos** sus grupos, «Sin receta» al final para los ítems sin fuentes y grupos ordenados por título normalizado ascendente (D2/D8).
- **RF-AGL5**: checkbox por fila que alterna `completed` (persiste en Room); la fila completada queda con el checkbox relleno, el título tachado y la fila atenuada (D5); desmarcar revierte el estado y baja el progreso (D4, sin snackbar).
- **RF-AGL6**: contenido de la fila según el modo (D6): en «Pasillo» se oculta la etiqueta de Pasillo y se conserva el badge de Origen; en «Por Receta» se oculta el badge de una sola fuente y se conserva la etiqueta de Pasillo; el badge «Para: N recetas» se conserva en ambos modos.
- **RF-AGL7**: acción «Limpiar comprados» (icono en la top bar, solo con ≥1 completado) que borra los completados y ofrece «Deshacer» restaurándolos todos con su estado y fuentes (D3/D7).
- **RF-AGL8**: intactos: estado vacío (CTA Escáner + «Añadir ítem»), diálogo de alta/edición, borrado con «Deshacer», badges y upsert RD19; editar un ítem no cambia su `completed` (D10).

## Acceptance Scenarios

### Scenario 1: agrupación por defecto por Pasillo

Given la Lista tiene ítems con Pasillo «Verduras & Frescos», uno «Sin clasificar» y uno de una Receta sin Pasillo
When abro la Lista de la Compra
Then los ítems aparecen agrupados por Pasillo con cabecera y contador, «Sin clasificar» al final y el selector en «Pasillo de Super»

### Scenario 2 (verify 1): agrupación por Receta con dos recetas

Given los ítems de las Recetas «Paella» y «Gazpacho» están en la Lista
When elijo «Por Receta»
Then cada ítem queda bajo la cabecera de su Receta y los contadores cuadran

### Scenario 3 (verify 2): la barra refleja el progreso

Given la Lista tiene 4 ítems, ninguno completado
When marco 2 con el checkbox
Then el progreso dice «2 de 4 artículos en la cesta» y la barra muestra el 50 %

### Scenario 4: ítem multi-fuente en ambos grupos

Given «Harina» tiene las fuentes «Paella» y «Gazpacho»
When elijo «Por Receta» y marco «Harina» en el grupo «Paella»
Then «Harina» aparece también en «Gazpacho», allí también marcado, y el progreso la cuenta una sola vez

### Scenario 5: desmarcar revierte el progreso

Given un ítem marcado
When pulso su checkbox para desmarcarlo
Then la fila vuelve a su estado normal y el progreso baja una unidad

### Scenario 6: fila completada

Given un ítem marcado
Then su checkbox aparece relleno, el título tachado y la fila atenuada, con el nombre legible (sin sustituirlo por «En la cesta»)

### Scenario 7: limpiar comprados con «Deshacer»

Given hay 2 ítems marcados y 2 pendientes
When pulso «Limpiar comprados» y luego «Deshacer»
Then desaparecen los 2 marcados y «Deshacer» los restaura con su estado, cantidades y fuentes

### Scenario 8: contenido de fila según el modo

Given un ítem con Pasillo y badge «Para: Paella» y otro multi-fuente
When recorro ambos modos
Then en «Pasillo» las filas llevan badge y sin etiqueta de Pasillo; en «Por Receta» llevan etiqueta de Pasillo y sin badge de una fuente; el multi-fuente conserva «Para: N recetas» en los dos modos

### Scenario 9: completado persistente y modo en memoria

Given marco ítems y elijo «Por Receta»
When salgo de la pantalla y vuelvo a entrar
Then los ítems siguen marcados (Room) y el selector vuelve a «Pasillo de Super» (D1)

### Scenario 10: edición y estado vacío intactos

Given un ítem marcado
When lo edito con el diálogo y guardo
Then conserva su marca; y con la lista vacía se muestra el estado vacío actual sin tarjeta de progreso ni selector

## Repository Research

### Files Inspected

- `feature_list.json` (entrada, steps y verify de la feature; deps `anadir-faltantes-a-la-lista` accepted).
- `docs/specs/anadir-faltantes-a-la-lista.md` y `docs/specs/anadir-item-a-la-lista.md` — lo ya entregado, los non-goals que esta feature levanta (tabs/checkboxes/progreso/«Deshacer» de completado) y la regla multi-grupo («un ítem con N fuentes aparecerá en todos los grupos»).
- `docs/domain-model.md` — ciclo `pendiente` ↔ `completado`, RD09/RD18/RD19 y casos límite vigentes.
- `CONTEXT.md` — Pasillo y su catálogo de 4 valores.
- `DESIGN.md` — fila «Lista de la Compra» (los tabs/checkboxes/progreso se declaran de esta feature) y estados.
- `uxdocs/chefencasa_lista_de_la_compra_sincronizada/code.html` — prototipo: tarjeta de progreso («6 de 14 artículos en la cesta», %, barra `secondary`), toggle «Agrupar por:» («Pasillo de Super»/«Por Receta»), grupos con icono + contador y filas con checkbox (relleno `secondary` al marcar, fila `opacity-60` y título tachado en el JS).
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — lista plana actual (filas con etiqueta de Pasillo + badge de Origen + edición/borrado), `ShoppingListTestTags`, diálogo RD19 y `formatEditableQuantity`.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListViewModel.kt` — `ShoppingListUiState` + borrado/«Deshacer» (`pendingUndo`) y editor.
- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingListRules.kt` — reglas puras (RD18/RD19) donde entrará la agrupación/progreso.
- `app/src/main/java/com/whatsforlunch/app/domain/model/ShoppingListItem.kt`, `domain/model/Aisle.kt` — modelo con `completed` y `Aisle`.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/ShoppingListRepository.kt`, `data/repository/RoomShoppingListRepository.kt`, `data/repository/InMemoryShoppingListRepository.kt`, `data/local/ShoppingListDao.kt`, `data/local/ShoppingListItemEntity.kt` — contrato y persistencia (columna `completed` ya en v3; `INSERT OR REPLACE` del padre cascada a fuentes).
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` — 17 tests previos, `launchShoppingList(viewModel)` con repos InMemory/Room y `E2eDatabase.clear()` en `@Before`.
- `app/src/test/java/.../ShoppingListRulesTest.kt`, `ShoppingListViewModelTest.kt`, `InMemoryShoppingListRepositoryTest.kt`, `ShoppingListManualItemTest.kt` — cobertura unitaria actual.
- `docs/risks-and-open-questions.md` — ninguna abierta que toque esta feature (`⚠ S04` voz, fuera).
- `docs/technical-discovery.md`, `init.ps1`/`init.sh`, `lint.ps1`, `unit.ps1`, `e2e.ps1` — gates disponibles.

### Existing Patterns To Follow

- Reglas de negocio puras en `domain/` (`ShoppingListRules`, sin Android ni UI) y ViewModel que solo orquesta (`AGENTS.md` P25).
- MVVM + StateFlow + Koin; pantalla sin estado con callbacks; `ShoppingListTestTags` para E2E.
- «Deshacer» vía `Snackbar` + `pendingUndo` (borrado de filas); mismo patrón para «Limpiar comprados».
- Textos visibles en `values/strings.xml` + `values-en/strings.xml` con paridad de claves; plurales con `plurals` (`shopping_list_origin_multiple`).
- Iconos: `material-icons-extended` ya está en `app/build.gradle.kts`; targets táctiles de 48 dp.
- Room v3 sin migración: `completed` ya se persiste (`ShoppingListItemEntity`), el mapeo `toDomain`/`toEntity` ya lo lee y escribe.

### Current Gaps

- No hay operación para cambiar `completed`: falta en `ShoppingListRepository`, DAO (hoy solo `INSERT OR REPLACE` + `deleteById`) e InMemory.
- No hay reglas de agrupación ni de progreso; la UI es una lista plana.
- Faltan los textos de progreso, toggle, cabeceras de grupo y «Limpiar comprados» (es/en).

## Technical Approach

**Dominio** — En `ShoppingListRules` (funciones puras, deterministas y testeables en JVM):
- `groupItems(items: List<ShoppingListItem>, mode: ShoppingGroupMode): List<ShoppingGroup>` con `enum class ShoppingGroupMode { BY_AISLE, BY_RECIPE }` y `ShoppingGroup(key, items)` donde `key` identifica el grupo (nombre del enum `Aisle` o `""` para «Sin clasificar»; `sourceKey` de la fuente; `""` para «Sin receta»). Orden (D8): BY_AISLE → los 4 `Aisle` en orden del enum y «Sin clasificar» al final; BY_RECIPE → grupos por título normalizado ascendente (desempate `sourceKey`) y «Sin receta» al final; dentro de cada grupo, orden actual (nombre normalizado, id) con los completados **en su sitio**; solo grupos no vacíos. En BY_RECIPE un ítem con fuentes se replica en cada grupo de sus fuentes (una sola fila lógica; el checkbox de todas sus apariciones refleja el mismo `completed`).
- `progress(items): ShoppingProgress(completed, total, percent)` sobre ítems únicos (`total = items.size`; los multi-fuente cuentan una vez), `percent` = redondeo al entero más próximo de `completed * 100 / total` (0 con lista vacía; la tarjeta no se muestra).

**Datos** — `ShoppingListRepository.setCompleted(id: String, completed: Boolean)`; en Room, `ShoppingListDao` gana `@Query("UPDATE shopping_items SET completed = :completed WHERE id = :id")` (update dirigido: evita el `REINSERT` de fuentes que exige el `INSERT OR REPLACE`); `RoomShoppingListRepository` lo delega con `mutex`; `InMemoryShoppingListRepository` actualiza su `MutableStateFlow`. `restoreItem` («Deshacer») ya restaura el ítem completo con su `completed`; para «Limpiar comprados» el contrato gana `restoreItems(items: List<ShoppingListItem>)` (o se reutiliza `restoreItem` en bucle dentro de una transacción). Sin migración: la columna existe desde la v3.

**ViewModel** — `ShoppingListUiState` gana `mode: ShoppingGroupMode` (en memoria, `MutableStateFlow`, default `BY_AISLE`), `groups: List<ShoppingGroup>` y `progress: ShoppingProgress`; nuevas acciones `setGroupMode(mode)`, `toggleCompleted(id)` y `clearCompleted()` con `pendingUndo` (mismo patrón de snackbar que el borrado, D7). El estado del editor y del borrado actual no cambian. *Refinamiento de implementación (2026-10-27, sin cambio de alcance)*: `pendingUndo` se modela como `PendingUndo(items: List<ShoppingListItem>, bulk: Boolean)` en lugar de `List<ShoppingListItem>`, porque hay dos mensajes de snackbar distintos («Ítem eliminado» del borrado por fila y «Ítems comprados eliminados» de «Limpiar comprados») y dos alcances de deshacer (una fila vs. todos los borrados de la operación masiva); `bulk` permite elegir ambos sin reabrir el diálogo de confirmación descartado en D3.

**UI** — `ShoppingListScreen`:
1. **Tarjeta de progreso** (solo con ≥1 ítem, encima de la lista): fila «{c} de {t} artículos en la cesta» + porcentaje en `secondary` y barra (`LinearProgressIndicator` o `Box` con fracción completada, relleno `secondary` sobre pista `surfaceContainer`), con `semantics` descriptivos.
2. **Selector «Agrupar por:»** dentro de la tarjeta: etiqueta + dos botones tipo segmento («Pasillo de Super»/«Por Receta») en contenedor redondeado `surfaceContainer`, seleccionado en `surfaceContainerLowest` con sombra (como el prototipo).
3. **Lista agrupada** en `LazyColumn`: cabecera de grupo (título + contador «N ítems» en píldora) y filas; la fila gana un **checkbox** de 48 dp a la izquierda (sin marcar: contorno; marcado: relleno `secondary` + check `onSecondary`) y aplica D6 al contenido (RF-AGL6).
4. **Top bar**: acción «Limpiar comprados» (icono `DeleteSweep` de `material-icons-extended`, 48 dp, visible solo con ≥1 completado) junto a «Volver».
5. `ShoppingListRoute` cablea los nuevos callbacks; nuevos test tags (`PROGRESS_*`, `GROUP_MODE_*`, `GROUP_HEADER`, `CHECK(id)`, `CLEAR_COMPLETED`) sin renombrar los existentes.

**Textos** — es/en con paridad: `shopping_list_progress` (plurales por el total), `shopping_list_progress_percent` («%d %%»), `shopping_list_group_by_label` («Agrupar por:»), `shopping_list_group_aisle` («Pasillo de Super»), `shopping_list_group_recipe` («Por Receta»), `shopping_list_group_count` (plurales «1 ítem»/«%d ítems»), `shopping_list_group_no_recipe` («Sin receta»), `shopping_list_clear_completed` («Limpiar comprados»), `shopping_list_items_removed` («Ítems comprados eliminados»).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingListRules.kt` — modify; `groupItems` + `progress`.
- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingGroup.kt` — create; `ShoppingGroupMode`, `ShoppingGroup`, `ShoppingProgress` (o equivalentes en `domain/model/`).
- `app/src/main/java/com/whatsforlunch/app/domain/repository/ShoppingListRepository.kt` — modify; `setCompleted` y `restoreItems`.
- `app/src/main/java/com/whatsforlunch/app/data/local/ShoppingListDao.kt` — modify; `updateCompleted`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomShoppingListRepository.kt` — modify; nuevas operaciones.
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryShoppingListRepository.kt` — modify; nuevas operaciones.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListViewModel.kt` — modify; modo, grupos, progreso, toggle y limpiar.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — modify; tarjeta, selector, cabeceras, checkbox, acción top bar y test tags.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListRoute.kt` — modify; callbacks nuevos.
- `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml` — modify; textos nuevos con paridad.
- `app/src/test/java/com/whatsforlunch/app/ShoppingListRulesTest.kt`, `ui/shoppinglist/ShoppingListViewModelTest.kt`, `InMemoryShoppingListRepositoryTest.kt` — modify; unit de agrupación/progreso/marca.
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` — modify; S1-S10.
- Docs: `docs/domain-model.md`, `DESIGN.md`, `docs/risks-and-open-questions.md` (ver más abajo). `feature_list.json`: solo `status`/`evidence` al cerrar.

## Visual Design Impact

- UI involved: **sí**. Design source: `DESIGN.md` (tokens Warm Culinary Craft) + `uxdocs/chefencasa_lista_de_la_compra_sincronizada/` (inspiración `P16`).
- Screens/states afectados: Lista de la Compra (tarjeta de progreso, selector «Agrupar por», lista agrupada, fila completada, acción «Limpiar comprados»); el estado vacío y el diálogo no cambian.
- Estados nuevos de fila/grupo: checkbox sin marcar/marcar (`secondary`/`onSecondary`), fila completada atenuada con título tachado, cabecera de grupo con contador en píldora.
- New design artifact required: **no**; el prototipo y los tokens cubren los estados (la inconsistencia del prototipo sobre la fila completada se resuelve en D5 a favor de su JS).
- Sin cambios de layout ajenos: Mis Recetas, Escáner, Detalle y Pinche no se tocan.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — sin capas, límites ni dependencias nuevas; MVVM + reglas en `domain/` ya documentados.
- `CONSTRAINTS.md`: **not needed** — sin MUST/MUST NOT nuevos (RD18/RD19 intactos).
- `AGENTS.md`: **not needed** — sin cambios de flujo, comandos ni convenciones.
- `docs/domain-model.md`: **update** — nueva regla **RD20** (agrupación por Pasillo/Receta con multi-fuente en todos sus grupos, ciclo `completado` visible con checkbox y «Limpiar comprados» reversible), casos límite nuevos y retiro de la nota «la agrupación llega con `agrupar-y-completar-la-lista`».
- `DESIGN.md`: **update** — la fila «Lista de la Compra» deja de describirse como «lista plana» y suma progreso, selector, checkboxes y «Limpiar comprados».
- `docs/risks-and-open-questions.md`: **update** — registrar `Q-agrupar-y-completar-la-lista-1..6` (D1-D6, decididas por el usuario el 2026-10-27) al implementar.
- `docs/technical-discovery.md`: **not needed** — sin comandos, integraciones ni datos nuevos.
- `CONTEXT.md`: **not needed** — términos (Pasillo, Origen, Lista de la Compra) ya definidos; los textos nuevos viven en `strings.xml`.

## Decisions

- **D1** (usuario 2026-10-27): el modo por defecto es **«Pasillo de Super»** y la selección vive **en memoria** (se reinicia al salir de la pantalla; sin persistir).
- **D2** (usuario 2026-10-27): en «Por Receta» los ítems sin fuentes van en el grupo **«Sin receta» al final**; los multi-fuente aparecen en todos sus grupos (regla heredada de `anadir-faltantes-a-la-lista`).
- **D3** (usuario 2026-10-27): **«Limpiar comprados» entra en alcance**; «Compartir» por WhatsApp queda fuera.
- **D4** (usuario 2026-10-27): **sin snackbar al completar**; el «Deshacer» de completado es el propio checkbox reversible (ciclo del modelo `pendiente` ↔ `completado`).
- **D5** (usuario 2026-10-27): la fila completada muestra **título tachado + fila atenuada + checkbox relleno** (el JS del prototipo); no se sustituye el título por «En la cesta».
- **D6** (usuario 2026-10-27): contenido de fila **según el modo**: «Pasillo» → sin etiqueta de Pasillo, con badge; «Por Receta» → sin badge de una fuente, con etiqueta de Pasillo; «Para: N recetas» siempre.
- **D7** (derivada): «Limpiar comprados» sigue el patrón de borrado de la pantalla (D7 de `anadir-faltantes-a-la-lista`): acción directa + aviso «Ítems comprados eliminados» con **«Deshacer» que restaura todos** (sin diálogo de confirmación), visible solo con ≥1 completado.
- **D8** (derivada): orden de grupos y de filas fijado en «Technical Approach» (enum del Pasillo + «Sin clasificar»/«Sin receta» al final; solo grupos no vacíos; completados en su sitio).
- **D9** (derivada): el progreso cuenta ítems únicos con independencia del modo y el porcentaje se redondea al entero más próximo; la tarjeta no se muestra con la lista vacía.
- **D10** (derivada): editar un ítem (RD19) no cambia `completed`; `addContributions` sobre un ítem completado lo conserva; «Deshacer» del borrado restaura el estado de completado y sus fuentes.

## Casos límite

| Caso | Comportamiento |
| --- | --- |
| Ítem multi-fuente en «Por Receta» | Aparece en cada grupo de sus fuentes; el checkbox refleja el mismo `completed` en todas sus apariciones y el progreso lo cuenta una vez (D2/D9); el contador «N ítems» de cada cabecera cuenta las filas mostradas en ese grupo |
| «Limpiar comprados» sin completados | Acción oculta (no hay nada que limpiar) |
| «Deshacer» de «Limpiar comprados» | Restaura todos los ítems borrados con su `completed`, cantidades, unidades, Pasillo y fuentes (D7/D10) |
| Marcar todos los ítems | Progreso «N de N…» y 100 %; los grupos siguen completos con las filas atenuadas |
| Ítem completado al que llega una fuente nueva (`addContributions`) | Sigue completado (D10); la cantidad suma según RD18 |
| Editar un ítem marcado (diálogo RD19) | Conserva la marca (D10) |
| Cambiar de tab | Solo cambia la presentación; ni datos ni `completed` se alteran |
| Salir y volver a entrar | Completados persisten (Room v3); el modo vuelve a «Pasillo de Super» (D1) |
| Lista vacía | Estado vacío actual, sin tarjeta de progreso ni selector (RF-AGL8) |
| Ítems sin Pasillo en modo «Pasillo» | Grupo «Sin clasificar» al final (RD19) |
| Fuentes de Propuesta del Pinche | Mismo tratamiento que Recetas: la clave es `sourceKey` (`proposalRecipeId`, RD16) y el título del grupo es `sourceTitle` |

## Implementation Plan

1. **Dominio**: `ShoppingGroupMode`/`ShoppingGroup`/`ShoppingProgress` y `ShoppingListRules.groupItems`/`progress` con tests unitarios de orden, multi-fuente, contadores y porcentaje.
2. **Datos**: `setCompleted`/`restoreItems` en el contrato, DAO (`updateCompleted`), Room e InMemory con tests (marca persistente; restauración en bloque).
3. **ViewModel**: `mode`, `groups`, `progress`, `setGroupMode`, `toggleCompleted`, `clearCompleted` + undo, con tests (incluye: editar no cambia la marca).
4. **UI**: tarjeta de progreso, selector, cabeceras de grupo, checkbox con D5/D6, «Limpiar comprados» en la top bar, textos es/en y test tags.
5. **E2E**: S1-S10 en `ShoppingListE2ETest` y regresión de los 17 previos.
6. **Docs**: `docs/domain-model.md` (RD20), `DESIGN.md`, `docs/risks-and-open-questions.md` (Q-…) y cierre (`PROGRESS.md`, `feature_list.json` solo `status`/`evidence`).

## Implementation Tasks

- [ ] T1: `ShoppingGroupMode`, `ShoppingGroup` y `ShoppingProgress` en `domain/`.
- [ ] T2: `ShoppingListRules.groupItems` (D8, multi-fuente) + tests.
- [ ] T3: `ShoppingListRules.progress` (D9) + tests.
- [ ] T4: `ShoppingListRepository.setCompleted`/`restoreItems` + `ShoppingListDao.updateCompleted` + Room + InMemory + tests.
- [ ] T5: `ShoppingListViewModel`: modo en memoria, `toggleCompleted`, `clearCompleted` con `pendingUndo` de lista + tests.
- [ ] T6: `ShoppingListScreen`/`ShoppingListRoute`: tarjeta de progreso, selector «Agrupar por», cabeceras con contador, checkbox (D5/D6), acción «Limpiar comprados» (D7) y test tags.
- [ ] T7: textos es/en con paridad (incluidos plurales de progreso y contador).
- [ ] T8: E2E S1-S10 + regresión de los tests previos de `ShoppingListE2ETest`.
- [ ] T9: docs (`domain-model.md` RD20, `DESIGN.md`, `risks-and-open-questions.md` Q-1..6).
- [ ] T10: gates completos y cierre (`PROGRESS.md`, `feature_list.json`).

## Verification Plan

- `./init.ps1` (Windows) o `./init.sh` → «Entorno listo», exit 0. El script ejecuta comprobaciones no bloqueantes (gradle, `installDebug`, arranque de la app) y **no** arranca procesos largos; puede imprimir comandos de seguimiento.
- `./lint.ps1` / `./gradlew :app:lintDebug` → **0 errores** y sin warnings nuevos sobre la línea base (49).
- `./unit.ps1` / `./gradlew :app:testDebugUnitTest --rerun-tasks` → todo verde (hoy 35 suites / 219 tests); los nuevos unit de agrupación/progreso/marca entran aquí.
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- Paridad es/en: mismo conjunto de claves en `values/strings.xml` y `values-en/strings.xml`.
- **E2E focalizada** (la feature cambia comportamiento visible de la Lista): `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.ShoppingListE2ETest` → verde con los 17 previos + S1-S10 (verify 1 = S2, verify 2 = S3). Fixtures locales y repos fakes, sin IA ni llamadas de red.
- **E2E batería**: `./e2e.ps1` (`connectedDebugAndroidTest` completo) → sin regresiones sobre los 171 tests de inventario (los flakes conocidos del AVD se documentan y reejecutan como en sesiones previas).
- Manual (apoyo, con `adb`/uiautomator): marcar/desmarcar y ver el progreso cambiar; «Limpiar comprados» + «Deshacer»; recorrer ambos modos con un ítem multi-fuente.

## Evidence To Capture

- `docs/evidence/agrupar-y-completar-la-lista/`: informe + **2 capturas es 1280×2856** (PNG binario): (1) modo «Pasillo de Super» con progreso parcial y una fila completada; (2) modo «Por Receta» con grupo multi-fuente y «Sin receta».
- Salida de los gates (`init`, `lint`, `unit`, `assembleDebug`, paridad) y de la E2E focalizada (nombres de los tests S1-S10) y de la batería (recuento).
- `PROGRESS.md` (Estado verificado + Siguiente paso + entrada en `docs/progress-history.md`) y `evidence` de `feature_list.json`.
- Sin API keys, sin llamadas a la IA ni PII en la evidencia (`⚠ S05`).

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance (sin voz, compartir, chips, ni cambios en RD18/RD19/Room v3).
- [ ] S1-S10 pasan (verify 1 = S2, verify 2 = S3) y los 17 E2E previos siguen en verde.
- [ ] D1-D10 cumplidas (modo en memoria, «Sin receta», «Limpiar comprados» con «Deshacer», checkbox reversible, fila completada D5, contenido por modo D6).
- [ ] Evidencia presente: gates, E2E, 2 capturas es válidas (cabecera PNG, 1280×2856) e informe en `docs/evidence/agrupar-y-completar-la-lista/`.
- [ ] Cobertura E2E añadida/actualizada (o justificado por qué no) para el cambio de comportamiento visible.
- [ ] `feature_list.json` solo cambia `status`/`evidence`; `PROGRESS.md` al día; docs durables actualizadas (`domain-model.md` RD20, `DESIGN.md`, `risks-and-open-questions.md` Q-1..6).
- [ ] Sin comportamiento ajeno ni trabajo de features futuras añadido.
- [ ] Seguridad: sin secretos, sin permisos nuevos, sin red; logcat y evidencia sin datos sensibles.
