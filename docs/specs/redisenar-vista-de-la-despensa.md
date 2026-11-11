# Feature Implementation Spec: Rediseñar la vista de la Despensa

## Source Feature

- `id`: redisenar-vista-de-la-despensa
- `area`: funcional
- `depends_on`: gestionar-alimentos-de-la-despensa, mover-escaneo-a-la-despensa (ambas `accepted`)
- `status`: not_started (planificación, sesión 128, 2026-11-11; decisiones del usuario 2026-11-10)
- `source`: `feature_list.json`

## Goal

La pantalla Despensa adopta el diseño de `uxdocs/chefencasa_despensa/` (`code.html`, `code_empty.html`, `screen.png`, `screen_empty.png`): cabecera de sección con subtítulo y badge de recuento, tarjeta de resumen con recuento, capacidad estimada y Alimentos por caducar, consejo de cocina, buscador con filtrado en vivo, tarjetas de Alimento con miniatura, píldora de frescura, cantidad, línea de caducidad, stepper −/+ inline y menú ⋮ (editar/eliminar), y los dos estados vacíos del prototipo. Para ello el Alimento gana una **fecha de caducidad opcional** (revisa `P5` de `docs/build-brief.md`, que deja de estar «fuera del MVP») con reglas de frescura en `domain/` (RD28) y Room **v8** (`MIGRATION_7_8`).

## Non-Goals

- Pestañas/zonas Nevera/Despensa/Congelador del prototipo (RD11 y `P37` intactos; sin campo de ubicación).
- Unidades ni decimales en la cantidad del Alimento (entero 0..99 sin unidad, RD13 intacto; las unidades del prototipo no se adoptan).
- Bottom bar del prototipo (se conservan las 5 pestañas), cabecera de marca ChefEnCasa/avatar y «Historial de compras» (no existe en la app).
- Estado vacío de filtrado de Mis Recetas (`chefencasa_mis_recetas_y_crud`).
- Taxonomía de iconos por alimento ni fotos en la Despensa (D12).
- Cambios en Escáner, Coincidencia, Sugerencias, Lista de la Compra o en el cálculo de la Coincidencia.

## Job Story

Cuando abro la Despensa,
quiero ver de un vistazo qué tengo, qué está a punto de caducar y ajustar cantidades rápido,
para cocinar antes de que se estropee la comida y no duplicar al comprar.

## Users And Permissions

- Usuario único (app de uso personal, 1-5 usuarios): acceso a la Despensa desde la pestaña de la barra inferior; sin «Volver» (RF-MOB5); todos los controles ≥ 48 dp; textos es/en (`P19`).

## Decisions

Del usuario (2026-11-10, registradas en `feature_list.json`): **D1** sin pestañas de zona; **D2** caducidad completa adoptada (revisa `P5`); **D3** cantidad entera 0..99 sin unidad; **D4** se conservan los dos FABs y la tarjeta de resumen no repite botones (stepper inline y menú ⋮ sí se adoptan); **D5** cabecera solo de la vista, sin marca ni avatar; **D6→D7** el buscador entra con filtrado en vivo (para que el estado «sin resultados» sea alcanzable). Derivadas en esta spec:

- **D8** — frescura: píldora «Consumir pronto» con ≤ 2 días restantes (hoy, mañana y fecha pasada incluidas; pasada → texto «Caducada»); «En buen estado» con > 2 días o sin fecha. Umbral fijo en `FreshnessRules`.
- **D9** — «N frescos» = total de Alimentos (como el prototipo, donde «25 frescos» = «25 Alimentos»); «N por caducar» = Alimentos «Consumir pronto», píldora oculta con 0.
- **D10** — capacidad estimada = `round(min(100, N / 40 * 100))` con capacidad de referencia 40 Alimentos (la cifra 68 % del prototipo es ilustrativa).
- **D11** — consejo de cocina determinista (sin IA): solo si hay ≥ 1 «Consumir pronto»; lista hasta 2 nombres (orden alfabético normalizado) y «y» entre ellos; con más de 2, «Tienes N alimentos listos para consumir pronto».
- **D12** — miniatura de la tarjeta: icono genérico `Kitchen` en contenedor redondeado (sin taxonomía de iconos ni fotos).
- **D13** — Alimento sin fecha: píldora «En buen estado» y texto «Larga duración» (como los ítems «Larga duración» del prototipo).
- **D14** — cantidad 0/null se muestra como «Sin cantidad» en la tarjeta (la regla RD13 sigue vigente).
- **D15** — el stepper inline guarda al instante (cada toque persiste, sin diálogo); en 0 se deshabilita «−» y en 99 «+»; el «¿Añadir a lista de la compra?» del prototipo queda fuera de alcance.
- **D16** — CTAs del estado vacío según el prototipo: «Añadir Alimento» (primario) primero y «Escanear la despensa» (tonal) segundo (enmienda de mover-escaneo-a-la-despensa D3).
- **D17** — el título de la top bar pasa de «Despensa» a «Mi Despensa» (clave `pantry_title` reutilizada; «Pantry» → «My Pantry» en inglés).
- **D18** — la búsqueda filtra por nombre normalizado «contiene», sin debounce (la lista es pequeña); con Despensa vacía siempre prevalece «Tu despensa está vacía».

## Acceptance Scenarios

### Scenario 1: Cabecera, resumen y consejo (verify 1)

Given una Despensa con Alimentos, incluidos con y sin fecha de caducidad,
When se abre la pestaña Despensa,
Then la top bar muestra «Mi Despensa», el contenido muestra el subtítulo «Ingredientes registrados en casa» con el badge «N frescos», la tarjeta de resumen muestra «N Alimentos», «Capacidad estimada al N %» y —solo si hay ≥ 1 Alimento «Consumir pronto»— «N por caducar», y el consejo de cocina aparece con los nombres de los Alimentos «Consumir pronto»; los Alimentos con ≤ 2 días muestran «Consumir pronto» y los demás «En buen estado».

### Scenario 2: Stepper inline y menú ⋮ (verify 2)

Given una Despensa con un Alimento de cantidad 5,
When se pulsa «+» dos veces en la tarjeta,
Then la cantidad pasa a 7 y queda guardada al momento (visible tras reabrir la pantalla); en 99 «+» está deshabilitado y en 0 «−»; al abrir el menú ⋮, «Editar» abre el diálogo con nombre, cantidad y fecha, y «Eliminar» muestra la confirmación existente y borra el Alimento.

### Scenario 3: Buscador y «sin resultados» (verify 3)

Given una Despensa con Alimentos «Tomate fresco» y «Harina»,
When se escribe «toma» en el buscador,
Then solo se lista «Tomate fresco» con filtrado en vivo; al escribir un término sin coincidencias, se muestra «No se encontraron ingredientes» con su mensaje y el botón «+ Registrar nuevo alimento» que abre el diálogo de alta; al limpiar la búsqueda, vuelve la lista completa.

### Scenario 4: Despensa vacía (verify 4)

Given una Despensa sin Alimentos,
When se abre la pestaña Despensa,
Then se muestra «Tu despensa está vacía» con su descripción, los CTA «Añadir Alimento» (primario) y «Escanear la despensa» (tonal) en ese orden (D16) y los dos FABs apilados; la cabecera de sección se conserva con el subtítulo y el badge «0 frescos» (D9; el prototipo dice «0 alimentos») y, como en `code_empty.html`, no hay buscador, tarjeta de resumen ni consejo.

### Scenario 5: Caducidad persistente (verify 5)

Given la Despensa vacía,
When se añade «Aguacates» con fecha de caducidad y se guarda,
Then el Alimento aparece con su fecha, la píldora y el texto de caducidad correspondientes; al reabrir la app, la fecha sobrevive; al editar y quitar la fecha, el Alimento queda «En buen estado» / «Larga duración».

### Scenario 6: FABs y textos es/en (verify 6)

Given cualquier estado de la Despensa,
When se comprueba la pantalla en español y en inglés,
Then los dos FABs («Escanear alimentos» encima, «Añadir alimento» abajo) siguen presentes y operativos, y todos los textos nuevos existen en ambos idiomas con paridad de claves.

### Scenario 7: Reglas de frescura (verify 7, unit)

Given fechas de caducidad a 3, 2, 1, 0 y −1 días y una sin fecha,
When se evalúan con `FreshnessRules`,
Then la clasificación es «En buen estado», «Consumir pronto», «Consumir pronto», «Consumir pronto» y «Consumir pronto», los textos de caducidad son «Caduca en 3 días», «Caduca en 2 días», «Caduca mañana», «Caduca hoy» y «Caducada», sin fecha → «Larga duración», y capacidad 25 → 63 %.

### Scenario 8: Migración Room v7 → v8 (verify 8)

Given una base v7 con Alimentos (con y sin cantidad),
When se migra a v8,
Then todos los Alimentos siguen con nombre y cantidad intactos y `expiryDate` NULL; guardar y reabrir conserva la nueva fecha.

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — pantalla actual: TopAppBar «Despensa», dos FABs circulares apilados (escaneo arriba), lista con tarjetas (nombre + «Cantidad: N» + iconos editar/eliminar), diálogo alta/edición (nombre + stepper 0..99), confirmación de borrado y estado vacío; `PantryTestTags` completo.
- `.../ui/pantry/PantryViewModel.kt` — `PantryUiState(items, saveError, saveCompleted)`, `saveItem(id, name, quantity)` y `deleteItem(id)`; sin búsqueda.
- `.../ui/pantry/PantryRoute.kt` — enlaza `onScanFood` y los callbacks del ViewModel.
- `.../domain/model/PantryItem.kt` — `PantryItem(id, name, quantity: Int?)` (0 = sin cantidad, no persistido).
- `.../domain/pantry/PantryItemValidation.kt` — nombre no vacío; cantidad 0..99.
- `.../domain/myrecipes/RecipeSearchFilter.kt` + `app/src/test/.../domain/RecipeSearchFilterTest.kt` — patrón del repo para filtrar por nombre normalizado (`filterRecipes`, «contiene»); a imitar en la Despensa.
- `.../domain/repository/PantryRepository.kt` — puerto `observePantryItems`/`savePantryItem`/`deletePantryItem`.
- `.../data/local/PantryItemEntity.kt`, `PantryItemDao.kt` — tabla `pantry_items(id, name, normalizedName unique, quantity REAL)`.
- `.../data/local/RecipeDatabase.kt` — **version = 7**, `MIGRATION_6_7` vigente; `exportSchema = false`.
- `.../data/repository/RoomPantryRepository.kt`, `InMemoryPantryRepository.kt` — guardado con mutex, duplicado por nombre normalizado, `quantity?.toDouble()` y redondeo al leer.
- `app/src/main/res/values/strings.xml` (+ `values-en/`) — claves `pantry_*` (título «Despensa»/«Pantry», vacío, diálogo, errores).
- `app/src/androidTest/.../PantryManagementE2ETest.kt` — S1-S9 (vacío, alta/edición/persistencia, duplicado, stepper 0..99, borrado, CTAs vacío, FAB apilado, Escáner→Despensa).
- `app/src/androidTest/.../PantryLocalizationE2ETest.kt` — es/en del título, CTAs y steppers con ≥48 dp.
- `uxdocs/chefencasa_despensa/code.html` y `code_empty.html` — prototipo (tokens Tailwind = tokens de `uxdocs/DESIGN.md`); `DESIGN.md` (front-matter y componentes); `DESIGN.md` raíz (fila «Despensa» L158 y estados L171); `docs/domain-model.md` (RD11, RD13, Alimento, validaciones); `docs/build-brief.md` (P5 L53); `docs/technical-discovery.md` (comandos).

### Existing Patterns To Follow

- Píldoras/badges como en `ui/components/MatchBadge.kt` (Surface con forma redonda, colores por rol M3, testTag aportado por la pantalla).
- Reglas de negocio en `domain/` con tests JVM (`P25`); UI sin lógica de negocio. El filtrado de búsqueda ya tiene patrón: función pura en dominio (`filterRecipes` en `domain/myrecipes/RecipeSearchFilter.kt`) aplicada por el ViewModel y cubierta por su test JVM.
- Diálogos `AlertDialog` + `testTag` y stepper con `IconButton` ≥48 dp (patrón del diálogo actual y de la Lista de la Compra).
- Migraciones Room en `RecipeDatabase` companion (`MIGRATION_X_Y`) con test en `RecipeMigrationTest`.
- Paridad es/en de claves y plurals; textos visibles solo en `strings.xml`.

### Current Gaps

- `PantryItem` no tiene fecha de caducidad ni reglas de frescura; no existen buscador, resumen, consejo ni estados «sin resultados» en la Despensa.
- No hay `PantryViewModelTest`; la Despensa se cubre con E2E y tests de repositorio/validación.
- La línea `P5` de `docs/build-brief.md` declara la caducidad «fuera del MVP» y hay que reconciliarla.

## Technical Approach

1. **Modelo y Room**: `PantryItem.expiryDate: LocalDate?` y `PantryItemEntity.expiryDate: Long?` (epochDay). Room v8 con `MIGRATION_7_8` = `ALTER TABLE pantry_items ADD COLUMN expiryDate INTEGER` (columna nullable; sin recrear tabla). `PantryItemDao.update` incluye `expiryDate`; `RoomPantryRepository`/`InMemoryPantryRepository` lo persisten/propagan. Cantidad sin cambios (Int 0..99, null al 0).
2. **Dominio (RD28)**: nuevo `domain/pantry/FreshnessRules.kt` puro: `Freshness { GOOD, EXPIRING_SOON }`, `freshness(expiry, today)`, `daysRemaining`, `pantryCapacityPercent(items)` (D10), `expiringCount(items)` y `expiringNames(items, limit)` (D11). `today` lo aporta la UI/ViewModel (inyectable en tests).
3. **ViewModel**: `PantryUiState` gana `searchQuery` y `visibleItems` manteniendo `items` para contadores; la regla de filtrado es una función pura `filterPantryItems(items, query)` en `domain/pantry/PantrySearchFilter.kt` —«contiene» sobre `normalizeName`, D18— siguiendo el patrón de `domain/myrecipes/RecipeSearchFilter.kt` (la UI no contiene reglas de negocio, `P25`); `saveItem` acepta `expiryDate`; nuevo `setQuantity(id, quantity)` que valida 0..99 con `PantryItemValidation` y guarda al instante (D15).
4. **UI (PantryScreen)**: cabecera de sección (subtítulo + badge «N frescos»), tarjeta de resumen (icono `Kitchen` en círculo, «N Alimentos», capacidad, «N por caducar»), banda de consejo (`tertiaryContainer`/`onTertiaryContainer`, icono `Eco`), campo de búsqueda con icono `Search` + limpiar, y tarjeta de Alimento: miniatura 56 dp con icono `Kitchen`, nombre, píldora de frescura (`errorContainer`/`onErrorContainer` = «Consumir pronto», `secondaryContainer`/`onSecondaryContainer` = «En buen estado»), cantidad en `primary` en negrita (o «Sin cantidad», D14), texto de caducidad, stepper −/+ inline (guardado inmediato, deshabilitado en límites) y `DropdownMenu` ⋮ con «Editar» (diálogo) y «Eliminar» (confirmación existente, etiquetas `editItem(id)`/`deleteItem(id)` reutilizadas). Alta/edición con fila «Fecha de caducidad (opcional)» («Sin fecha de caducidad» o fecha) y `DatePickerDialog` de Material 3. Estados vacíos nuevos: «Tu despensa está vacía» (icono grande, descripción nueva, CTAs en orden D16) y «No se encontraron ingredientes» con «+ Registrar nuevo alimento». Sin buscador/resumen/consejo con Despensa vacía (D18). FABs intactos (D4); objetivos táctiles ≥48 dp.

### Casos límite

- Fecha pasada → «Consumir pronto» + «Caducada» (D8); fecha = hoy → «Caduca hoy»; mañana → «Caduca mañana».
- Sin fecha → «En buen estado» + «Larga duración» (D13); Alimento sin cantidad → «Sin cantidad» (D14).
- Stepper en límites 0/99 con botones deshabilitados; toques rápidos no deben dejar cantidades fuera de rango (guardado en el ViewModel con validación).
- Duplicado por nombre normalizado: sigue rechazándose en el diálogo (Q-…-3); el stepper inline no cambia nombres.
- Búsqueda sin resultados con Despensa no vacía → «No se encontraron ingredientes»; con Despensa vacía → «Tu despensa está vacía» aunque haya texto en la búsqueda.
- «N por caducar» y consejo ocultos con 0; «N frescos» = total (D9).
- Migración con Alimentos legados sin fecha → `expiryDate` NULL y «Larga duración».

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/PantryItem.kt` — modify; añade `expiryDate: LocalDate?`.
- `app/src/main/java/com/whatsforlunch/app/domain/pantry/FreshnessRules.kt` — create; reglas de frescura/capacidad/consejo (RD28).
- `app/src/main/java/com/whatsforlunch/app/domain/pantry/PantrySearchFilter.kt` — create; `filterPantryItems` (D18, patrón de `RecipeSearchFilter.kt`).
- `app/src/test/java/com/whatsforlunch/app/domain/pantry/PantrySearchFilterTest.kt` — create; filtrado «contiene», query vacía/en blanco, mayúsculas y acentos.
- `app/src/main/java/com/whatsforlunch/app/data/local/PantryItemEntity.kt` — modify; columna `expiryDate: Long?`.
- `app/src/main/java/com/whatsforlunch/app/data/local/PantryItemDao.kt` — modify; `update` con `expiryDate`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — modify; `version = 8` y `MIGRATION_7_8`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt` — modify; mapeo de `expiryDate`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepository.kt` — no changes needed; `expiryDate` se propaga solo con `PantryItem.copy` (round-trip cubierto en su test).
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryViewModel.kt` — modify; `searchQuery`/`visibleItems`, `expiryDate` en `saveItem`, `setQuantity`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppBottomBar.kt` — modify; la pestaña PANTRY usa la clave corta nueva `pantry_tab_label` (patrón `my_recipes_tab_label`) para que D17 («Mi Despensa» en `pantry_title`) no altere la barra inferior (alcance «sin bottom bar»; hallazgo de implementación, 2026-11-11).
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — modify; rediseño completo, `PantryTestTags` nuevos.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryRoute.kt` — modify; cablea `onQuantityChange`.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — modify; textos nuevos y `pantry_title`/`pantry_tab_label`/`pantry_empty_title`/`pantry_empty_message` (D17/D16); paridad es/en + plurals de contadores.
- `app/src/test/java/com/whatsforlunch/app/domain/pantry/FreshnessRulesTest.kt` — create; cubre D8-D11, D13 y escenarios de verify 7.
- `app/src/test/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepositoryTest.kt` — modify; `expiryDate` en el round-trip.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt` — modify; adaptar (menú ⋮, CTAs vacío D16) y añadir S10-S14 (resumen/consejo, stepper inline, búsqueda/sin resultados, caducidad).
- `app/src/androidTest/java/com/whatsforlunch/app/PantryLocalizationE2ETest.kt` — modify; título «My Pantry», textos nuevos y ≥48 dp de stepper inline/menú.
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeMigrationTest.kt` — modify; caso v7→v8.
- `docs/domain-model.md` — modify; RD28, concepto Alimento con fecha, validaciones y casos límite.
- `DESIGN.md` — modify; fila «Despensa» (L158) y estado vacío (L171).
- `docs/build-brief.md` — modify; `P5` deja de estar «fuera del MVP».
- `docs/technical-discovery.md` — modify; Room v8 + `MIGRATION_7_8`.
- `docs/risks-and-open-questions.md` — modify; `Q-redisenar-vista-de-la-despensa-1` con las decisiones D8-D18.
- `CONTEXT.md` — modify; glosario «Despensa y compra»: términos de frescura/caducidad (D2/D8/D9/D13) y Alimento con fecha de caducidad opcional.
- `docs/specs/gestionar-alimentos-de-la-despensa.md` y `docs/specs/mover-escaneo-a-la-despensa.md` — modify; nota de enmienda al final (patrón completo) y `notes` en `feature_list.json`.
- `uxdocs/` — **not touched** (solo lectura; `P16`/`P17`).

## Visual Design Impact

- UI involved: sí. Design source: `uxdocs/chefencasa_despensa/` (`code.html`, `code_empty.html`) + tokens de `uxdocs/DESIGN.md` y `DESIGN.md` (fila «Despensa»).
- Screens or states affected: **Despensa** — cabecera de sección, tarjeta de resumen, consejo, buscador, tarjeta de Alimento (miniatura, píldoras, stepper inline, menú ⋮), diálogo de alta/edición con fecha de caducidad, estados vacíos «Tu despensa está vacía» y «No se encontraron ingredientes». FABs y barra inferior intactos.
- New design artifact required: no (prototipo ya existe en `uxdocs/`; los colores siguen los roles M3 ya usados: `primaryFixed` para el icono del resumen, `errorContainer`/`secondaryContainer` para píldoras, `tertiaryContainer` para el consejo, `surfaceContainerHigh` para «N por caducar»).

## Durable Documentation Impact

- `docs/domain-model.md`: **update** — RD28 (reglas de frescura, D8-D11/D13), Alimento con `fecha de caducidad opcional`, validaciones y casos límite.
- `DESIGN.md`: **update** — fila «Despensa» (L158) y estado vacío (L171) con el rediseño.
- `docs/build-brief.md`: **update** — `P5` (L53) pasa a alcance de esta feature.
- `docs/technical-discovery.md`: **update** — Room v8 y `MIGRATION_7_8`.
- `docs/risks-and-open-questions.md`: **update** — `Q-redisenar-vista-de-la-despensa-1`.
- `CONTEXT.md`: **update** — el glosario («Despensa y compra») gana los términos de frescura/caducidad («Consumir pronto», «En buen estado», «Larga duración», «N por caducar», «Capacidad estimada») y el **Alimento** se define con fecha de caducidad opcional; los términos existentes no cambian (la regla del propio glosario es que sus términos se usan tal cual en docs, UI y código).
- `ARCHITECTURE.md` / `CONSTRAINTS.md` / `AGENTS.md`: not needed (sin capas ni reglas MUST nuevas).

## Implementation Plan

1. Modelo y datos: `PantryItem.expiryDate`, entidad/DAO, `MIGRATION_7_8` (v8) y repositorios Room/en memoria con su test de migración.
2. Dominio: `FreshnessRules` (D8-D11, D13) con `FreshnessRulesTest`.
3. ViewModel: búsqueda (`visibleItems`), `expiryDate` en `saveItem` y `setQuantity` al instante (D15).
4. UI: cabecera, resumen, consejo, buscador, tarjeta rediseñada, menú ⋮, diálogo con fecha (DatePicker) y estados vacíos; textos es/en y testTags.
5. Tests y docs: E2E adaptados/nuevos, localización, docs durables y enmiendas en las specs aceptadas afectadas.

## Implementation Tasks

- [ ] T1: `PantryItem.expiryDate` + entidad/DAO + `MIGRATION_7_8` (v8) + repositorios + `RecipeMigrationTest` v7→v8. (S8)
- [ ] T2: `FreshnessRules` + `FreshnessRulesTest` (clasificación, textos, capacidad, nombres del consejo). (S7)
- [ ] T3: `PantrySearchFilter` + `PantrySearchFilterTest` y `PantryViewModel`: `searchQuery`/`visibleItems`, `saveItem` con fecha, `setQuantity` con `PantryItemValidation`. (S1, S2, S3)
- [ ] T4: `PantryScreen`: cabecera, tarjeta de resumen y consejo. (S1)
- [ ] T5: `PantryScreen`: buscador + estados «No se encontraron ingredientes» y «Tu despensa está vacía» (D16). (S3, S4)
- [ ] T6: `PantryScreen`: tarjeta rediseñada (miniatura, píldoras, cantidad, caducidad, stepper inline, menú ⋮). (S2)
- [ ] T7: Diálogo de alta/edición con fecha de caducidad (DatePicker M3 + «Sin fecha de caducidad»). (S5)
- [ ] T8: strings es/en + plurals + testTags; paridad de claves. (S6)
- [ ] T9: E2E: adaptar `PantryManagementE2ETest`/`PantryLocalizationE2ETest` y añadir S10-S14; `InMemoryPantryRepositoryTest`. (S1-S6)
- [ ] T10: docs durables (RD28, DESIGN, build-brief, technical-discovery, risks, CONTEXT) + enmiendas en specs aceptadas y `notes` de `feature_list.json`.

## Verification Plan

- `./init.ps1` (Windows) / `./init.sh` (Linux/macOS) — «Entorno listo» (checks no bloqueantes; no debe arrancar procesos largos).
- `./gradlew lintDebug` — 0 errores (sin warnings nuevos).
- `./gradlew testDebugUnitTest` — `FreshnessRulesTest`, `PantrySearchFilterTest`, `InMemoryPantryRepositoryTest` y suite completa en verde.
- `./gradlew assembleDebug` — BUILD SUCCESSFUL.
- `./gradlew connectedDebugAndroidTest` focalizada en `PantryManagementE2ETest` + `PantryLocalizationE2ETest` + `RecipeMigrationTest` (verify 1-6 y 8) y batería completa sin regresión con `./e2e.ps1` (Windows) / `./e2e.sh` (Linux/macOS), los scripts E2E persistentes del harness que exige `AGENTS.md` para la aceptación (wrappers de `connectedDebugAndroidTest`, `docs/technical-discovery.md` §Comandos).
- Manual adb/uiautomator en emulador API 30+ (es y en): resumen/consejo/contador, stepper inline con persistencia, búsqueda con resultados y «sin resultados», estado vacío con CTAs en orden D16, alta con fecha y persistencia tras reiniciar; capturas PNG 1280×2856.

## Evidence To Capture

- Salidas de `init.ps1`, `lintDebug`, `testDebugUnitTest`, `assembleDebug` y focalizada/batería E2E (XML) en `docs/evidence/redisenar-vista-de-la-despensa/`.
- 2-3 capturas PNG es (resumen+consejo, tarjeta con stepper/menú y fecha, estado vacío) y 1 en.
- Recuento de paridad es/en y de `@Test`; migración v7→v8 desde `RecipeMigrationTest`.
- Evidencia en `feature_list.json` (`evidence`) y entrada en `PROGRESS.md`/`docs/progress-history.md` al cerrar.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance de esta spec (sin zonas, sin unidades, sin bottom bar, sin taxonomía de iconos).
- [ ] Escenarios S1-S8 pasan (E2E + unit + migración).
- [ ] Evidencia presente (gates, E2E, capturas, paridad).
- [ ] Cobertura E2E añadida/actualizada para el flujo observable (Despensa) o justificación explícita.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente (solo `status`/`evidence` en la lista, más `notes` de enmienda autorizados).
- [ ] Sin comportamiento de producto ni features extra añadidos.
