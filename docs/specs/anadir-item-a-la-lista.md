# Feature Implementation Spec: Añadir ítem a la Lista de la Compra

## Source Feature

- `id`: anadir-item-a-la-lista
- `area`: funcional
- `depends_on`: anadir-faltantes-a-la-lista (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

El usuario añade a mano artículos a la Lista de la Compra que no salen de ninguna Receta («Pan», «Papel de cocina») o que quiere comprar sin receta asociada, con su **cantidad** y su **Pasillo** (`CONTEXT.md`), para tener la compra completa en un solo sitio antes de ir al súper. El alta vive en la propia pantalla «Lista de la Compra» con el patrón de la Despensa (FAB + icono de edición por fila + diálogo único) y materializa el step «Permitir el alta manual de artículos con cantidad y Pasillo» y su verify 1.

El alta es un **upsert por nombre normalizado**: si el artículo ya existe (p. ej. «Harina» nacida como Faltante de una Receta), guardar lo edita en lugar de duplicarlo, y el Pasillo elegido queda visible en la fila. El ítem manual conserva su Origen según RD09 con el badge **«Despensa baja»**. El step de **alta por voz queda diferido** por decisión del usuario (D1): esta spec no incluye micrófono, permisos ni `SpeechRecognizer`.

## Non-Goals

- **Dictado por voz** (step «Permitir el alta por dictado de voz según S04», `⚠ S04`): **diferido por decisión del usuario el 2026-10-26** (D1). Sin micrófono, sin `RECORD_AUDIO`, sin `SpeechRecognizer` (mismo criterio que el Pinche, `Q-resolver-dudas-con-pinche-6`); pendiente del spike de precisión de voz y de una futura feature.
- **Agrupación por Pasillo/Receta, tabs, checkboxes y barra de progreso**: es `agrupar-y-completar-la-lista`; la pantalla sigue en lista plana.
- **Barra rápida del prototipo y chips «Sugerencias»** («+ Ajo morado»): la barra de solo nombre no entra (D2) y los chips exigirían IA, fuera de alcance (sin llamadas a la API).
- **Renombrar ítems**: la identidad es el nombre normalizado; el nombre es de solo lectura al editar (D7).
- **Conversiones de unidades ni tope 99**: las cantidades de compra no se recortan (RD18).
- **Notificaciones del sistema, compra online y sincronización**: la Lista es local y manual.
- **Tocar reglas existentes**: RD01/RD02/RD10/RD13/RD15/RD18 (`ShoppingListRules.addContributions`, `missingRequiredIngredients`), `calculateMatch`, ni el esquema de Room (sin migración).

## Job Story

When quiero comprar productos que no salen de ninguna receta o que me faltan en la Despensa,
I want to añadirlos a mano a la Lista de la Compra con su cantidad y su Pasillo,
so I can llevar la lista completa al súper, ordenada por donde piso, sin apuntarlo en otro sitio.

## Users And Permissions

- Usuario único sin cuentas ni roles: misma app local que el resto de features. Puede abrir la Lista desde la barra superior de Mis Recetas, añadir ítems con el diálogo, editarlos (cantidad, unidad y Pasillo), borrarlos con «Deshacer» y vaciar la Lista ítem a ítem.

## Requisitos funcionales

- **RF-AIL1**: la pantalla «Lista de la Compra» ofrece la acción «Añadir ítem» (FAB extendido, patrón de la Despensa) y también en el estado vacío junto a «Escanear la Despensa»; ambas abren el diálogo de alta.
- **RF-AIL2**: el diálogo tiene nombre (obligatorio), cantidad (opcional), unidad (opcional) y Pasillo (opcional, 5 opciones: «Sin clasificar» + los 4 de `CONTEXT.md`); guardar crea el ÍtemLista con badge «Despensa baja» (RD09) y Pasillo elegido.
- **RF-AIL3**: guardar con un nombre normalizado que ya existe **edita** el ítem existente (cantidad, unidad y Pasillo reemplazados por los del diálogo; nombre conservado) sin crear fila nueva; sus fuentes y su badge se conservan.
- **RF-AIL4**: cada fila muestra su Pasillo como texto secundario («Sin clasificar» si no tiene) además de su línea «{nombre} ({cantidad} {unidad})» y su badge de Origen; un icono de edición abre el diálogo precargado con el nombre de solo lectura.
- **RF-AIL5**: validación en el diálogo: nombre no vacío tras normalizar; cantidad opcional decimal mayor que 0 (acepta coma o punto; vacío = sin cantidad); el error se muestra sin cerrar el diálogo y no guarda nada.
- **RF-AIL6**: los ítems y sus cambios persisten en Room (sin migración: la columna `aisle` existe desde la v3) y sobreviven a reiniciar la app.
- **RF-AIL7**: todos los textos nuevos tienen paridad es/en en `values/` y `values-en/`.

## Acceptance Scenarios

### Scenario 1: alta manual con cantidad y Pasillo (verify 1)

Given la Lista de la Compra está abierta
When pulso «Añadir ítem», escribo «Pan», cantidad «2», unidad «uds», elijo «Despensa & Aceites» y guardo
Then la Lista muestra la fila «Pan (2 uds)» con la etiqueta «Despensa & Aceites» y el badge «Despensa baja».

### Scenario 2: un duplicado fusiona y edita (sin filas nuevas)

Given «Harina (200 g)» está en la Lista como Faltante de «Tortilla» (badge «Para: Tortilla», «Sin clasificar»)
When añado a mano «harina» con «500 g» y Pasillo «Despensa & Aceites»
Then no se crea fila nueva: «Harina» queda con «500 g» y la etiqueta «Despensa & Aceites», conservando su badge «Para: Tortilla».

### Scenario 3: edición por icono de fila

Given la Lista tiene «Azafrán (1 g)» con badge «Para: Paella»
When pulso su icono de edición, cambio el Pasillo a «Lácteos» y guardo
Then la fila muestra la etiqueta «Lácteos» y la cantidad, el nombre y el badge quedan intactos (el nombre no es editable).

### Scenario 4: nombre obligatorio

Given el diálogo de alta está abierto
When pulso «Guardar» con el nombre vacío o solo espacios
Then veo el error «Escribe un nombre», el diálogo sigue abierto y no se guarda nada.

### Scenario 5: cantidad y Pasillo opcionales

Given el diálogo de alta está abierto
When guardo «Leche» sin cantidad y sin elegir Pasillo
Then la fila aparece como «Leche» (sin paréntesis) con la etiqueta «Sin clasificar».

### Scenario 6: el estado vacío también añade

Given la Lista de la Compra está vacía
When pulso «Añadir ítem» del estado vacío y guardo «Pan»
Then el diálogo se abre y, tras guardar, la Lista ya no está vacía y muestra la fila.

## Casos límite

| Caso | Comportamiento |
|---|---|
| Nombre con distinta caja o espacios («harina » vs «Harina») | Mismo nombre normalizado: se edita el ítem existente (se conserva su nombre guardado) |
| Renombrar un ítem | No se puede: el nombre es de solo lectura en edición (D7); borrar y volver a añadir crea el nuevo |
| Cantidad no numérica, 0 o negativa | Error de validación en el diálogo; no se guarda |
| Cantidad decimal («0,5» o «0.5») | Admitida; se guarda como `Double` y se formatea con el locale de la app |
| Unidad vacía | Ítem sin unidad: la fila es «{nombre} ({cantidad})» |
| Cantidad vacía | Ítem sin cantidad: la fila es solo «{nombre}» |
| Editar un ítem con fuentes («Para: {Receta}») | Las fuentes y el badge no cambian; solo cantidad, unidad y Pasillo |
| Ítem manual que luego recibe Faltantes de una Receta | `addContributions` (RD18) reutiliza la fila, suma la cantidad de la fuente nueva (si la unidad coincide) y la registra: el badge pasa a «Para: {Receta}» y «Despensa baja» desaparece |
| Borrar y volver a añadir a mano | Se recrea con su cantidad (el id depende del nombre normalizado) |
| «Deshacer» tras expirar el aviso | No se puede restaurar; se vuelve a añadir a mano |
| Cantidades superiores a 99 | Sin tope (RD18): el máximo de 99 es de Alimentos, no de ítems de compra |

## Repository Research

### Files Inspected

- `feature_list.json` (entrada de la feature, steps y verify), `PROGRESS.md`, `CONTEXT.md` (Pasillo, Origen, «Despensa baja»), `docs/domain-model.md` (RD09, RD18, ÍtemLista, validaciones y casos límite), `docs/build-brief.md`, `docs/risks-and-open-questions.md` (`⚠ S04`, `Q-anadir-faltantes-a-la-lista-4`, `Q-resolver-dudas-con-pinche-6`), `docs/technical-discovery.md` (fila de voz), `DESIGN.md` (filas «Lista de la Compra» y estados).
- `uxdocs/chefencasa_lista_de_la_compra_sincronizada/code.html` (barra rápida + mic + «Añadir», secciones por Pasillo, agrupación y progreso: referencia de contenido, no de alcance).
- `docs/specs/anadir-faltantes-a-la-lista.md` (spec aceptada previa: modelo, RD18 y non-goals que esta feature levanta).
- `app/src/main/java/com/whatsforlunch/app/domain/model/ShoppingListItem.kt` (`aisle: String?` ya modelado), `domain/shoppinglist/ShoppingListRules.kt` (`itemIdFor`, `addContributions`), `domain/repository/ShoppingListRepository.kt` (`observeItems`, `addMissing`, `deleteItem`, `restoreItem`).
- `data/local/ShoppingListItemEntity.kt` (`shopping_items.aisle`, índice único `normalizedName`; `shopping_item_sources` con cascada), `data/local/ShoppingListDao.kt` (`findByNormalizedName`, `findById`, `insertItem` REPLACE, `insertSources`, `deleteById`), `data/repository/RoomShoppingListRepository.kt` e `InMemoryShoppingListRepository.kt`.
- `ui/shoppinglist/ShoppingListScreen.kt` (`ShoppingListTestTags`, fila con badge y borrado, estado vacío con CTA), `ShoppingListViewModel.kt`, `ShoppingListRoute.kt`.
- `ui/pantry/PantryScreen.kt` y `PantryViewModel.kt` (patrón a imitar: FAB extendido «Añadir alimento», icono de edición por fila, `PantryItemDialog` con `saveItem(id?, name, quantity)`, duplicado rechazado con aviso), `domain/model/MealMoment.kt` (enum de dominio + etiquetas `@StringRes` es/en), `res/values/strings.xml` y `res/values-en/strings.xml` (claves `shopping_list_*`).

### Patterns Found

- Enums de dominio con identificadores en inglés (`MealMoment { BREAKFAST, … }`) y etiquetas visibles en `strings.xml` con paridad es/en (`meal_moment_*`).
- Diálogo de alta/edición única con `itemToEdit` + `onSave(id?, …)` (upsert) y validación visible en el propio diálogo, como en la Despensa.
- Reglas puras en `domain/` sin Android; ids estables con `UUID.nameUUIDFromBytes` (`ShoppingListRules.itemIdFor`); repositorios con interfaz en `domain/repository/`, implementación Room con `Mutex` + transacción e `InMemory` para tests.
- Cuidado conocido: `insertItem` usa `OnConflictStrategy.REPLACE`, que dispara el borrado en cascada de `shopping_item_sources`; al reescribir un ítem hay que reinsertar sus fuentes (o usar un `UPDATE` de solo las columnas editadas).

### Gaps / Unknowns

- No existe alta manual ni edición de ítems: `ShoppingListRepository` solo expone `addMissing`, `deleteItem` y `restoreItem`.
- El campo `aisle` es `String?` y hoy siempre `null`: hay que decidir su codificación (enum de dominio, ver Enfoque) sin migrar la tabla.
- La voz (micrófono, permisos) no existe en la app y queda fuera de esta spec (D1).

## Technical Approach

**Dominio** — enum `domain/model/Aisle.kt`: `Aisle { PRODUCE, MEAT_FISH, DAIRY, PANTRY_OILS }` («Verduras & Frescos», «Carnes & Pescados», «Lácteos», «Despensa & Aceites» de `CONTEXT.md`), con etiquetas `@StringRes` en la UI como `MealMoment`; el Pasillo ausente es `null` y se muestra como «Sin clasificar». `ShoppingListItem.aisle` pasa de `String?` a `Aisle?`. `ShoppingListRules` gana la construcción/validación del alta manual (nombre normalizado no vacío, cantidad opcional `Double?` > 0, unidad recortada) reutilizando `itemIdFor`.

**Datos** — sin migración: `shopping_items.aisle` sigue siendo `TEXT NULL` y almacena el nombre del enum (`PRODUCE`, …) o `null`; el mapeo vive en los repositorios (`toDomain`/`toEntity`). `ShoppingListRepository` gana `saveManualItem(id: String?, name: String, quantity: Double?, unit: String, aisle: Aisle?)`: si hay `id`, edita ese ítem; si no, busca por nombre normalizado y edita ese (conservando su nombre) o crea uno nuevo con `itemIdFor(name)`, `completed = false` y **sin fuentes**. La edición usa un `UPDATE` de solo `name/quantity/unit/aisle` (o reinserta fuentes como hace `addContributions`) para no disparar la cascada. `RoomShoppingListRepository` e `InMemoryShoppingListRepository` lo implementan dentro de su `Mutex`/transacción habitual.

**ViewModel y pantalla** — `ShoppingListViewModel` expone el estado del diálogo (`editing: ShoppingListItem?` + `showEditor`) y `saveManualItem(…)` delegando en el repositorio; la validación de nombre/cantidad vive en el diálogo (como `PantryItemDialog`). `ShoppingListScreen` añade: FAB extendido «Añadir ítem» (icono `add`), icono de edición por fila (48 dp) que precarga el diálogo con el nombre de solo lectura, etiqueta de Pasillo como texto secundario de la fila, diálogo `ShoppingItemDialog` (nombre, cantidad decimal, unidad, 5 `FilterChip` de Pasillo, «Cancelar»/«Guardar») y un botón «Añadir ítem» en el estado vacío junto al CTA «Escanear la Despensa». El badge de Origen existente gana la variante **«Despensa baja»** cuando el ítem no tiene fuentes (RD09, D5). TestTags nuevos en `ShoppingListTestTags` (`ADD_ITEM`, `EDIT_DIALOG`, `ITEM_AISLE`, campos y acciones del diálogo) sin renombrar los existentes.

### Decisiones

- **D1** (usuario 2026-10-26): **la voz se difiere**. El step «Permitir el alta por dictado de voz según S04» queda fuera de esta spec (sin micrófono ni permisos nuevos); se retomará cuando se resuelva el spike de precisión de `SpeechRecognizer` (`docs/risks-and-open-questions.md`), junto al resto de la voz del producto.
- **D2** (usuario 2026-10-26): el alta es un **diálogo completo** (nombre, cantidad, unidad opcional y Pasillo) con el patrón de la Despensa (FAB + edición por fila + diálogo único); la barra rápida de solo nombre del prototipo no entra.
- **D3** (usuario 2026-10-26): un duplicado por nombre normalizado **fusiona y edita**: guardar reemplaza cantidad, unidad y Pasillo del ítem existente (nombre conservado) sin crear fila nueva; las fuentes y el badge no se tocan.
- **D4** (usuario 2026-10-26): el **Pasillo es opcional** («Sin clasificar» por defecto, como nacen los Faltantes) y **se muestra en la fila** como texto secundario; catálogo fijo de 4 valores (`CONTEXT.md`), sin alta de categorías nuevas.
- **D5** (derivada de RD09 y del non-goal de `anadir-faltantes-a-la-lista`): el badge **«Despensa baja»** corresponde a los ítems **sin fuentes** (alta manual); si una Receta aporta después al mismo ítem, el badge pasa a «Para: {Receta}» (RD18 intacta) y la edición manual no cambia fuentes ni badge.
- **D6** (derivada de RD18 y del modelo): cantidad opcional decimal > 0 sin tope 99 (acepta coma o punto), unidad opcional de texto libre; sin cantidad la fila es solo el nombre.
- **D7** (derivada): **sin renombrar** ítems; la identidad es el nombre normalizado (`itemIdFor`) y el nombre es de solo lectura al editar.

### Desconocidos

- **Traducción de los Pasillos en `values-en`** (p. ej. «Carnes & Pescados» → «Meat & Fish»): la fija el implementer siguiendo el glosario de `CONTEXT.md` y la revisa el validador (`RK04`).
- **D3 × RD18**: editar la cantidad de un ítem con fuentes reemplaza el total visible; las aportaciones posteriores de fuentes nuevas siguen sumando sobre el valor editado (sin conversiones). Comportamiento aceptado, documentado en casos límite.

## Expected File Changes

**Nuevos**
- `app/src/main/java/com/whatsforlunch/app/domain/model/Aisle.kt`
- `app/src/test/java/com/whatsforlunch/app/ShoppingListManualItemTest.kt` (reglas del alta manual)

**Modificados**
- `app/src/main/java/com/whatsforlunch/app/domain/model/ShoppingListItem.kt` (`aisle: Aisle?`)
- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingListRules.kt` (construcción/validación del alta manual)
- `app/src/main/java/com/whatsforlunch/app/domain/repository/ShoppingListRepository.kt` (`saveManualItem`)
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomShoppingListRepository.kt` e `InMemoryShoppingListRepository.kt` (upsert por nombre normalizado + mapeo del enum)
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` (FAB, diálogo, edición, etiqueta de Pasillo, estado vacío, badge «Despensa baja»), `ShoppingListViewModel.kt`, `ShoppingListRoute.kt`
- `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml` (paridad es/en)
- `app/src/test/java/com/whatsforlunch/app/ShoppingListRulesTest.kt`, `ShoppingListViewModelTest.kt`, `InMemoryShoppingListRepositoryTest.kt`
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` (S1–S6 sin romper los 10 casos previos)

`data/local/` **sin cambios** (sin migración; `aisle` ya existe en la v3) y `di/AppModule.kt` solo si cambia el cableado del ViewModel.

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` + inspiración `uxdocs/chefencasa_lista_de_la_compra_sincronizada/`.
- Pantallas/estados afectados: «Lista de la Compra» (FAB, fila con etiqueta de Pasillo, diálogo de alta/edición, estado vacío con dos acciones).
- Nuevo artefacto de diseño: no — se aplican los tokens `Warm Culinary Craft` existentes.
- **Diálogo** (patrón `PantryItemDialog`): título «Añadir ítem» / «Editar ítem»; campo Nombre (de solo lectura en edición), campo Cantidad decimal (vacío = sin cantidad), campo Unidad opcional, fila de 5 `FilterChip` de Pasillo («Sin clasificar» + 4; seleccionable uno) y acciones «Cancelar»/«Guardar». Sin stepper (las cantidades son decimales) y sin tope 99 (RD18).
- **Fila**: línea existente «{nombre} ({cantidad} {unidad})» + badge de Origen («Despensa baja» para ítems sin fuentes) + etiqueta de Pasillo en texto secundario (`on-surface-variant`) + icono de edición y icono de borrado de 48 dp.
- **Estado vacío**: conserva «Escanear la Despensa» y añade «Añadir ítem» debajo.
- **Sin** micrófono, barra rápida, chips «Sugerencias», agrupación ni progreso (Non-Goals).
- **Contrato visual verificable**: tras S1, la Lista muestra «Pan (2 uds)» con etiqueta «Despensa & Aceites» y badge «Despensa baja»; tras S2, una sola fila «Harina (500 g)» con «Despensa & Aceites» y badge «Para: Tortilla».

## Durable Documentation Impact

- **Update** `docs/domain-model.md`: nueva **RD19** («El alta manual crea o edita un ÍtemLista por nombre normalizado: cantidad, unidad y Pasillo se reemplazan por los del diálogo, el nombre no se edita, el Pasillo es opcional entre los 4 de `CONTEXT.md` + «Sin clasificar» y el Origen es «Despensa baja» mientras el ítem no tenga fuentes»); actualizar Validaciones (nombre no vacío, cantidad opcional decimal > 0), los casos límites nuevos (duplicado manual, edición con fuentes, manual → aporte de Receta) y el escenario 3.
- **Update** `docs/risks-and-open-questions.md`: registrar `Q-anadir-item-a-la-lista-1..4` (voz diferida; diálogo completo; fusión y edición de duplicados; Pasillo opcional con etiqueta + «Despensa baja») como resueltas por el usuario el 2026-10-26 (origen «planificación de la spec»), y anotar en el spike de `SpeechRecognizer` que cubre también el dictado de la Lista (aún diferido).
- **Update** `DESIGN.md`: fila «Lista de la Compra» (alta manual con diálogo, edición por fila, etiqueta de Pasillo y badge «Despensa baja»; **voz diferida**, no «alta manual/voz») y estado vacío con «Escanear la Despensa» + «Añadir ítem».
- **Not needed** `CONTEXT.md`: Pasillo, Origen («Despensa baja») y Lista de la Compra ya están definidos y esta feature no cambia su significado.
- **Not needed** `AGENTS.md` ni `docs/build-brief.md`: estructura, comandos, convenciones y alcance MVP intactos (la voz seguía programada para más tarde).
- **Not needed** `docs/technical-discovery.md`: sin stack nuevo (sin voz, sin permisos, sin migración) y comandos sin cambios.
- **Not needed** `ARCHITECTURE.md` / `CONSTRAINTS.md`: no existen en este repo (las fronteras durables viven en `docs/domain-model.md`, `docs/technical-discovery.md` y `AGENTS.md`) y esta feature no crea dominios, capas ni reglas MUST nuevas.
- `PROGRESS.md` + `docs/progress-history.md`: al cerrar la implementación (labor del harness, no de esta planificación).

## Implementation Plan

1. **Dominio puro**: enum `Aisle`, `ShoppingListItem.aisle: Aisle?`, reglas de construcción/validación del alta manual en `ShoppingListRules` y sus tests (T1–T2).
2. **Persistencia**: `saveManualItem` en la interfaz y en ambos repositorios (upsert por nombre normalizado, mapeo del enum, sin cascada) con tests (T3).
3. **Pantalla**: ViewModel con estado de diálogo, `ShoppingItemDialog`, FAB, icono de edición, etiqueta de Pasillo, badge «Despensa baja» y estado vacío; textos es/en y testTags (T4–T6).
4. **E2E y cierre**: `ShoppingListE2ETest` con S1–S6 y regresión de los casos previos; gates y evidencia (T7).

Cada fase deja `lint` y `unit` en verde antes de pasar a la siguiente.

## Implementation Tasks

- **T1**: `Aisle.kt` y cambio de `ShoppingListItem.aisle` a `Aisle?` (ajustar mapeos existentes).
- **T2**: `ShoppingListRules` del alta manual (nombre normalizado no vacío, cantidad opcional > 0, `itemIdFor`) + `ShoppingListManualItemTest`.
- **T3**: `ShoppingListRepository.saveManualItem` + implementaciones Room/InMemory (upsert, `UPDATE` sin cascada, mapeo del enum) + tests de repositorio.
- **T4**: `ShoppingListViewModel` (estado del diálogo, `saveManualItem`) y `ShoppingListScreen` (FAB, diálogo con validación y FilterChip de Pasillo, edición por fila, etiqueta de Pasillo, badge «Despensa baja», estado vacío).
- **T5**: `ShoppingListRoute`/cableado y textos es/en con paridad (`shopping_list_add_item`, `shopping_list_edit_item`, campos, errores, `aisle_*`, «Sin clasificar», «Despensa baja»).
- **T6**: testTags nuevos en `ShoppingListTestTags` sin renombrar los existentes; `ShoppingListViewModelTest` e `InMemoryShoppingListRepositoryTest` al día.
- **T7**: `ShoppingListE2ETest` S1–S6 (verify 1 = S1) y comprobación de que los 10 casos previos siguen en verde.

## Verification Plan

1. `./init.ps1` (o `./init.sh`): entorno y arranque en verde antes de empezar y al cerrar; **sin cambios en el script** (comprobaciones no bloqueantes; esta feature no añade servicios, modos ni permisos).
2. `./lint.ps1` (`lintDebug`): 0 errores y sin warnings nuevos sobre el baseline.
3. `./unit.ps1` (`testDebugUnitTest --rerun-tasks`): todas las suites en verde, incluidas `ShoppingListManualItemTest` y los ajustes de ViewModel/repositorio.
4. `./e2e.ps1` (`connectedDebugAndroidTest` sobre el emulador, fixtures locales con `E2eDatabase.clear()`, **sin IA y sin llamadas de pago** — no requiere confirmación humana): **S1** = verify 1 del `feature_list` (alta manual → la ítem aparece en la Lista); **S2–S6** según los escenarios; los 10 E2E previos de `ShoppingListE2ETest` en verde.
5. `assembleDebug` sin errores.
6. **Sin migración**: con una BD v3 previa (ítems de Faltantes), comprobar que se abre, que editar no borra las fuentes ni el badge y que todo sobrevive a reiniciar la app (captura o log en el informe).
7. **Paridad de claves es/en** de `values/` y `values-en/` tras añadir los textos nuevos.

## Evidence To Capture

- `docs/evidence/anadir-item-a-la-lista/informe.md`: entorno, comandos con resultado (init, lint, unit, assemble, e2e), desglose S1–S6 y nota de edición sin cascada/reinicio.
- 2 capturas en español: (1) diálogo de alta con cantidad y Pasillo; (2) Lista con ítems manuales (badge «Despensa baja» + etiqueta de Pasillo) y un ítem de Receta editado.
- Resumen en el `evidence` de `feature_list.json` (lo único que se toca de ese archivo) y entrada en `docs/progress-history.md`. Informe del validador en `docs/validations/anadir-item-a-la-lista.md`.

## Trazabilidad

| Fuente (`feature_list.json`) | Spec |
|---|---|
| «Permitir el alta manual de artículos con cantidad y Pasillo» | RF-AIL1–RF-AIL7 · S1–S6 |
| «Permitir el alta por dictado de voz según S04» | **Diferida** por decisión del usuario (2026-10-26): Non-Goals, D1, `Q-anadir-item-a-la-lista-1` |
| verify 1 «E2E: añadir un artículo a mano y comprobar que aparece en la Lista de la Compra» | S1 |
| notes «Alta por voz según ⚠ S04» | Diferida (D1); el spike de `SpeechRecognizer` queda como puerta previa |

## Validator Checklist

- [ ] ¿El `verification` de `feature_list.json` está ejecutado como S1 en el emulador con resultado legible?
- [ ] ¿El alta crea ítems con cantidad, unidad y Pasillo, badge «Despensa baja» y «Sin clasificar» por defecto (S1, S5, RF-AIL2)?
- [ ] ¿Un duplicado por nombre normalizado edita sin crear fila nueva y conserva fuentes y badge (S2, S3, RF-AIL3)?
- [ ] ¿La fila muestra el Pasillo y el icono de edición abre el diálogo con el nombre de solo lectura (S3, RF-AIL4)?
- [ ] ¿La validación exige nombre y admite cantidad opcional decimal > 0 sin tope 99 (S4, S5, RF-AIL5, RD18)?
- [ ] ¿La voz sigue diferida (sin micrófono, `RECORD_AUDIO` ni `SpeechRecognizer`) y no se ha colado nada de `agrupar-y-completar-la-lista` (tabs, checkboxes, progreso)?
- [ ] ¿RD18 intacta (idempotencia y suma por fuente de los Faltantes) y Room sin migración (v3, `aisle` solo se usa)?
- [ ] ¿Paridad es/en, testTags/contratos estables y los 10 E2E previos de la Lista en verde?
- [ ] ¿`init`, `lint` y `unit` en verde, `assembleDebug` sin errores y evidencia + `feature_list.json` actualizados?
