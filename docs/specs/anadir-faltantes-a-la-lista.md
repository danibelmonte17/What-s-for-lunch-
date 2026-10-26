# Feature Implementation Spec: Añadir faltantes a la Lista de la Compra

## Source Feature

- `id`: anadir-faltantes-a-la-lista
- `area`: funcional
- `depends_on`: crear-receta (`accepted`), gestionar-alimentos-de-la-despensa (`accepted`)
- `status`: `accepted`
- `source`: `feature_list.json`

## Goal

Cuando el usuario ve una Receta, una tarjeta de «Recetas con mi Despensa» o la Ficha de una Propuesta del Pinche, la app detecta sus **Faltantes** (ingredientes obligatorios ausentes de la Despensa, `CONTEXT.md`) y los convierte con un toque en **ÍtemLista** de la Lista de la Compra conservando su Origen según **RD09**. La Lista de la Compra existe a partir de aquí: pantalla con los ítems, su badge de Origen («Para: {Receta}» o «Para: N recetas»), borrado con «Deshacer» y estado vacío. Cada ítem se crea **una sola vez por nombre normalizado** y las cantidades de cada fuente se suman **una sola vez** (repetir la acción es un no-op): es la materialización de los pasos «Detectar los ingredientes obligatorios ausentes al ver una receta o tras un escaneo» y «Crear los ítems de la Lista de la Compra con su Origen según RD09».

## Non-Goals

- **Agrupación por Pasillo/Receta, checkboxes, barra de progreso y «Deshacer» de completado**: es `agrupar-y-completar-la-lista`. La pantalla queda en lista plana.
- **Alta manual o por voz, elección de Pasillo y el badge «Despensa baja»**: es `anadir-item-a-la-lista`. Los ítems de esta feature nacen sin Pasillo («Sin clasificar») y con Origen «Para: {Receta}».
- **El ciclo de vida del ítem (pendiente → comprado)**: la columna `completed` existe pero siempre es `false`; su UI llega con `agrupar-y-completar-la-lista`.
- **Notificaciones del sistema** (la «notificación interactiva» del PRD): las entradas son in-app.
- **Conversiones de unidades ni tope 99**: solo se suman unidades iguales; el tope 99 es de Alimentos (`docs/domain-model.md`), no de ítems de compra.
- **Tocar reglas existentes**: `calculateMatch`/`RecipeMatch`, RD01/RD02/RD10/RD13, `SuggestionRules`, `SuggestionAcceptance`, `ServingsScaling` ni el esquema de Recetas/Despensa (solo se añaden tablas nuevas).
- Sin IA ni llamadas a la API (tampoco `/images/generations`).

## Job Story

When voy a comprar lo que me falta para cocinar una receta,
I want to añadir de un toque los ingredientes que no tengo a la Lista de la Compra,
so I can comprar solo lo que falta sin anotarlo a mano ni olvidarlo.

## Users And Permissions

- Usuario único sin cuentas ni roles: misma app local que el resto de features. Puede añadir Faltantes desde el Detalle, las tarjetas de «Recetas con mi Despensa» y la Ficha del Pinche, abrir la Lista, borrar ítems con «Deshacer» y vaciar la Lista ítem a ítem.

## Requisitos funcionales

- **RF-AFL1**: en el Detalle de Receta, el bloque de Ingredientes muestra el banner «¡Tienes el X% de ingredientes en casa!» (X = Coincidencia, RD01); con al menos un Faltante aparece la acción «Añadir a la lista».
- **RF-AFL2**: «Añadir a la lista» crea los ítems de los Faltantes de la Receta con la cantidad **visible en el checklist** (escalada según RD03) y el Origen «Para: {Receta}» (RD09), y muestra el aviso «Añadido a la Lista de la Compra».
- **RF-AFL3**: cada tarjeta de «Recetas con mi Despensa» con Faltantes muestra la línea «Te falta: …» y una acción de carrito que añade esos Faltantes con sus cantidades base.
- **RF-AFL4**: la Ficha del Pinche con Faltantes (snapshot RD15) muestra «Añadir faltantes a la Lista de la Compra»; al pulsarlo queda «Añadido a la Lista de la Compra» deshabilitado (estado en memoria de la Ficha, como «Guardada en Mis Recetas»).
- **RF-AFL5**: un ÍtemLista por nombre normalizado; cada fuente (Receta o Propuesta del Pinche) suma su cantidad **una sola vez** y repetir la acción desde la misma fuente no cambia nada; el Origen conserva todas las fuentes («Para: {Receta}» con una, «Para: N recetas» con varias).
- **RF-AFL6**: la pantalla «Lista de la Compra» lista los ítems («{nombre} ({cantidad} {unidad})») con su badge de Origen, permite borrarlos con aviso «Ítem eliminado» + «Deshacer», y muestra el estado vacío con CTA «Escanear la Despensa».
- **RF-AFL7**: la Lista y sus fuentes persisten en Room (una fila por nombre normalizado) y sobreviven a reiniciar la app; la pantalla se abre desde la barra superior de Mis Recetas.
- **RF-AFL8**: todos los textos nuevos tienen paridad es/en en `values/` y `values-en/`.

## Acceptance Scenarios

### Scenario 1: Detalle con faltantes → Lista con Origen (verify 1)

Given la Despensa tiene «Arroz» y «Nata», y la Receta «Paella» pide «Arroz, Azafrán, Mariscos» (todos obligatorios)
When abro el Detalle de «Paella» y pulso «Añadir a la lista»
Then veo el banner «¡Tienes el 33% de ingredientes en casa!» y, al abrir la Lista de la Compra, «Azafrán» y «Mariscos» están con su badge «Para: Paella».

### Scenario 2: repetir la acción no duplica ni dobla (verify 2)

Given ya añadí los Faltantes de «Paella» y la Lista tiene «Azafrán (1 g)» y «Mariscos (300 g)»
When pulso «Añadir a la lista» otra vez sobre el mismo Detalle
Then la Lista sigue teniendo los mismos ítems, con las mismas cantidades (sin filas nuevas y sin doblar la suma).

### Scenario 3: dos Recetas suman y el badge cuenta fuentes

Given «Tortilla» pide «Harina (200 g)» y «Galletas» pide «Harina (500 g)» y ninguna está en la Despensa
When añado los Faltantes de «Tortilla» y luego los de «Galletas»
Then hay un solo ítem «Harina (700 g)» con badge «Para: 2 recetas» (los títulos en su descripción accesible).

### Scenario 4: tarjeta de «Recetas con mi Despensa» («tras un escaneo»)

Given la Despensa cubre 5 de 6 obligatorios de «Pechuga Gratinada» (falta «Orégano seco»)
When abro «Recetas con mi Despensa» y pulso el carrito de su tarjeta
Then la línea «Te falta: Orégano seco» desaparece del cómputo y en la Lista aparece «Orégano seco (1 bote)» con badge «Para: Pechuga Gratinada».

### Scenario 5: Ficha del Pinche

Given la Ficha de la Propuesta «Crema de calabaza» tiene Faltantes según su snapshot
When pulso «Añadir faltantes a la Lista de la Compra»
Then sus ítems aparecen en la Lista con badge «Para: Crema de calabaza», el botón queda «Añadido a la Lista de la Compra» deshabilitado, y si guardo la Propuesta y repito desde el Detalle del Borrador no se duplica nada.

### Scenario 6: borrado con «Deshacer»

Given la Lista contiene «Azafrán (1 g)» con badge «Para: Paella»
When pulso el icono de borrado del ítem y luego «Deshacer» en el aviso
Then el ítem desaparece y vuelve con su cantidad, su unidad y su Origen intactos.

### Scenario 7: Lista vacía

Given la Lista de la Compra está vacía
When la abro desde la barra superior de Mis Recetas
Then veo el estado vacío con el CTA «Escanear la Despensa» y este abre el Escáner.

### Scenario 8: sin Faltantes no hay acción

Given la Despensa contiene todos los obligatorios de «Arroz con leche» (100%)
When abro su Detalle y su tarjeta en «Recetas con mi Despensa»
Then el banner muestra «¡Tienes el 100% de ingredientes en casa!» y no aparecen «Añadir a la lista» ni la línea «Te falta: …».

## Casos límite

| Caso | Comportamiento |
|---|---|
| Receta sin ingredientes obligatorios (Coincidencia `null`) | Sin banner ni «Añadir a la lista» (como el badge de Coincidencia) |
| Despensa vacía | Todos los obligatorios son Faltantes (como RD15) |
| Nombre duplicado dentro de la misma Receta | La primera aparición aporta la cantidad; las demás se ignoran (mismo criterio que RD15) |
| Ingrediente con cantidad 0 | Ítem sin cantidad visible (solo el nombre) |
| Unidades distintas entre fuentes (200 g vs 2 uds) | Se conserva la cantidad/unidad del ítem sin conversiones; la fuente se registra igualmente para no doblar después |
| Propuesta del Pinche guardada en Mis Recetas | Misma clave de fuente (RD16): Ficha y Detalle del Borrador aportan una sola vez |
| Ítem borrado y vuelto a añadir | Se recrea con su cantidad (el id depende del nombre normalizado) |
| «Deshacer» tras expirar el aviso | No se puede restaurar; se vuelve a añadir desde la Receta |
| Raciones escaladas en el Detalle | La aportación usa las cantidades visibles (RD03) |
| Cantidades superiores a 99 | Sin tope: el máximo de 99 aplica a Alimentos, no a ítems de compra |
| Dos Recetas con el mismo título | Fuentes distintas (por id): suman por separado y el badge dice «Para: 2 recetas» |

## Repository Research

### Files Inspected

- `feature_list.json` (entrada de la feature y de `anadir-item-a-la-lista`/`agrupar-y-completar-la-lista`), `docs/domain-model.md` (RD09, RD15, ÍtemLista, casos límite), `CONTEXT.md` (Faltante, Origen, Pasillo, Lista de la Compra), `docs/build-brief.md`, `docs/risks-and-open-questions.md` (`Q-resolver-dudas-con-pinche-3/4`), `DESIGN.md` (filas «Lista de la Compra» y Ficha del Pinche).
- `uxdocs/chefencasa_lista_de_la_compra_sincronizada/code.html` (badges «Para: …»/«Despensa baja», «Deshacer»), `uxdocs/chefencasa_detalle_de_receta/code.html` (banner «¡Tienes el 100% de ingredientes en casa!», botón «Lista de compra»), `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html` («Te falta: …» + `add_shopping_cart`).
- `app/src/main/java/com/whatsforlunch/app/data/local/`: `RecipeDatabase.kt` (v2, `MIGRATION_1_2`), `PantryItemEntity.kt`, `PantryItemDao.kt`, `IngredientEntity.kt` (FK `CASCADE`), `RecipeWithDetails.kt` (`@Relation`).
- `data/repository/RoomPantryRepository.kt` (`Mutex`, mapeo a dominio), `domain/repository/PantryRepository.kt`, `domain/model/PantryItem.kt`, `domain/model/RecipeIngredient.kt`, `domain/match/RecipeMatch.kt`, `domain/pinche/PincheProposalRules.kt` (`missingRequired`, `proposalRecipeId`).
- `ui/recipedetail/RecipeDetailViewModel.kt` y `RecipeDetailScreen.kt` (banner y botón a insertar), `ui/pinche/PincheScreen.kt` (`ProposalCard`, `PincheTestTags.SAVE/SAVED_LABEL`), `ui/recommendations/RecommendationsScreen.kt` (`RecommendationsTestTags`, tarjeta compatible), `ui/nav/AppNavHost.kt` (`AppRoutes`), `di/AppModule.kt`, `ui/myrecipes/MyRecipesScreen.kt` (acciones de la top bar), `res/values/strings.xml`.
- `app/src/androidTest/java/com/whatsforlunch/app/E2eDatabase.kt`, `e2e.ps1`, `unit.ps1`, `init.ps1`.

### Patterns Found

- Room: entidades planas con `@PrimaryKey` String, índice único por `normalizedName` (`PantryItemEntity`), FK `onDelete = CASCADE` + índice (`IngredientEntity`), `@Relation` para vistas compuestas (`RecipeWithDetails`), migraciones con SQL en `RecipeDatabase.Companion` y versionado manual.
- Reglas de negocio puras en `domain/` sin Android (p. ej. `PincheProposalRules.missingRequired` ya calcula Faltantes por `normalizeName`, sin duplicados y en orden); ids estables con `UUID.nameUUIDFromBytes`.
- Repositorios con interfaz en `domain/repository/`, implementación Room con `Mutex`, y doble `InMemory…Repository` para tests unitarios.
- UI: MVVM + `StateFlow`, `*TestTags`, textos vía `stringResource` con paridad es/en, avisos transitorios (notice) ya usados por Sugerencias y Pinche.

### Gaps / Unknowns

- No existen tablas ni repositorios de la Lista de la Compra: hay que crear el modelo completo y la migración 2→3.
- No hay patrón de «Deshacer» en la app: el prototipo lo resuelve con un aviso con acción; se implementará con `Snackbar` + acción (testeable por testTag).
- La top bar de Mis Recetas ya tiene 4 acciones; una quinta (Lista de la Compra) puede quedar densa (ver Desconocidos).

## Technical Approach

Flujo único de dominio con tres superficies de entrada. `ShoppingListRules` (puro) calcula los Faltantes y fusiona aportaciones; el repositorio Room persiste ítems + fuentes en dos tablas; la pantalla Lista solo observa, borra y deshace.

**Dominio** — `domain/model/ShoppingListItem.kt`: `ShoppingListItem(id, name, quantity: Double?, unit, aisle: String?, completed: Boolean, sources: List<ShoppingItemSource>)` y `ShoppingItemSource(sourceKey, sourceTitle)`. `domain/shoppinglist/ShoppingListRules.kt` (puro, sin Android):
- `missingRequiredIngredients(ingredients, pantryItems): List<RecipeIngredient>`: obligatorios ausentes por `normalizeName`, sin duplicados (primera aparición) y en orden de la receta.
- `itemIdFor(name) = UUID.nameUUIDFromBytes("shopping:" + normalizeName(name))`.
- `addContributions(existing, sourceKey, sourceTitle, ingredients)`: crea el ítem (o lo reutiliza por nombre normalizado); si `sourceKey` ya aportó a ese ítem es un no-op; si es nueva, suma la cantidad cuando la unidad normalizada coincide y registra la fuente. `PincheProposalRules.missingRequired` pasa a delegar aquí (comportamiento intacto; `PincheProposalRulesTest` sigue verde).

**Datos** — `ShoppingListItemEntity` (tabla `shopping_items`: `id` String PK, `name`, `normalizedName` con índice único, `quantity REAL NULL`, `unit`, `aisle NULL`, `completed INTEGER NOT NULL DEFAULT 0`) y `ShoppingItemSourceEntity` (tabla `shopping_item_sources`: FK `shopping_items.id` `CASCADE`, `sourceKey`, `sourceTitle`, índice único `(itemId, sourceKey)`) con `ShoppingItemWithSources` (`@Relation`). `ShoppingListDao` (observe con relación, findByNormalizedName, insert/update/delete) y `MIGRATION_2_3` creando ambas tablas con sus índices; `RecipeDatabase` pasa a `version = 3`. `RoomShoppingListRepository` (con `Mutex` y transacción, como `RoomPantryRepository`) + `InMemoryShoppingListRepository` para tests.

**Repositorio** — `domain/repository/ShoppingListRepository.kt`: `observeItems(): Flow<List<ShoppingListItem>>`, `addMissing(sourceKey, sourceTitle, ingredients: List<RecipeIngredient>)`, `deleteItem(id)`, `restoreItem(item)` (para «Deshacer»).

**Pantalla** — `ui/shoppinglist/` con `ShoppingListRoute`, `ShoppingListScreen` y `ShoppingListViewModel`; ruta `AppRoutes.SHOPPING_LIST = "shopping_list"` y acción de la top bar de Mis Recetas (icono `ShoppingCart`, `action_open_shopping_list`). Filas ordenadas por nombre normalizado (como Despensa), formato «{nombre} ({cantidad} {unidad})» con el formateo locale de cantidades ya usado en el Detalle; badge de Origen; `IconButton` de borrado (48 dp). Estado vacío con CTA «Escanear la Despensa» → `SCAN_PANTRY`.

**Superficies de alta** — `RecipeDetailViewModel` (+ `ShoppingListRepository`, expone los Faltantes del Detalle y `addMissingToShoppingList()` con las cantidades visibles del checklist); `RecommendationsViewModel` (+ nombres de Faltantes por tarjeta y `addMissingToShoppingList(recipe)` con cantidades base); `PincheViewModel` (+ `addMissingToShoppingList()` sobre el snapshot RD15 con cantidades base, y estado por Ficha «Añadido a la Lista de la Compra»).

### Decisiones

- **D1** (usuario 2026-10-26): las tres superficies entran en esta feature — Detalle (banner + «Añadir a la lista»), tarjetas de «Recetas con mi Despensa» («Te falta: …» + carrito, el non-goal diferido de `mostrar-recetas-compatibles`, = «tras un escaneo») y Ficha del Pinche (`Q-resolver-dudas-con-pinche-3/4`, `DESIGN.md`).
- **D2** (usuario 2026-10-26): un ÍtemLista por nombre normalizado y **sumar cantidades**, pero cada fuente suma una sola vez: repetir la acción sobre la misma Receta/Propuesta es un no-op (verify 2) y fuentes distintas acumulan (200 g + 500 g = 700 g).
- **D3** (usuario 2026-10-26): la pantalla es lista plana + borrado + estado vacío; agrupación/checkboxes/progreso quedan en `agrupar-y-completar-la-lista` y el alta manual/voz en `anadir-item-a-la-lista`.
- **D4** (usuario 2026-10-26): los ítems se crean **sin Pasillo** («Sin clasificar»); asignar Pasillo es del alta manual.
- **D5** (usuario 2026-10-26): repetir la acción es idempotente (no duplica filas ni dobla la suma).
- **D6** (usuario 2026-10-26): badge «Para: {Receta}» con una fuente y «Para: N recetas» con varias (títulos en la descripción accesible); «Despensa baja» queda para `anadir-item-a-la-lista`.
- **D7** (usuario 2026-10-26): borrado directo con aviso «Ítem eliminado» + «Deshacer» (patrón del prototipo), sin diálogo de confirmación.
- **D8**: el Detalle aporta las cantidades **visibles** en el checklist (escaladas, RD03); las tarjetas y la Ficha, las cantidades base.
- **D9**: clave de fuente = `recipe.id`; para una Propuesta del Pinche = `PincheProposalRules.proposalRecipeId(título)` (RD16), de modo que Ficha y Detalle del Borrador guardado comparten fuente y no suman dos veces.
- **D10**: `completed` (false) y `aisle` (null) se modelan desde el inicio (la entidad coincide con «ÍtemLista» del modelo de dominio y evita una migración futura); las cantidades de compra no tienen tope 99.

### Desconocidos

- **Densidad de la top bar de Mis Recetas** (5 acciones): se mantiene el patrón de acción en la barra (como Despensa). Si la validación la considera inviable, la alternativa mínima es una acción dentro de la pantalla Despensa.
- **Aviso de éxito en el Detalle**: esa pantalla no tiene notice hoy; se usará `Snackbar` con testTag propio (patrón Material3 ya presente en la app).
- **Multi-fuente y agrupación futura por Receta**: un ítem con N fuentes aparecerá en todos los grupos de `agrupar-y-completar-la-lista`; fuera de esta feature, que lo resuelva esa spec.

## Expected File Changes

**Nuevos**
- `app/src/main/java/com/whatsforlunch/app/domain/model/ShoppingListItem.kt`
- `app/src/main/java/com/whatsforlunch/app/domain/shoppinglist/ShoppingListRules.kt`
- `app/src/main/java/com/whatsforlunch/app/domain/repository/ShoppingListRepository.kt`
- `app/src/main/java/com/whatsforlunch/app/data/local/ShoppingListItemEntity.kt` (incluye `ShoppingItemSourceEntity` y `ShoppingItemWithSources`)
- `app/src/main/java/com/whatsforlunch/app/data/local/ShoppingListDao.kt`
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomShoppingListRepository.kt`
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryShoppingListRepository.kt`
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` (incluye `ShoppingListTestTags`)
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListViewModel.kt`
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListRoute.kt`
- `app/src/test/java/com/whatsforlunch/app/ShoppingListRulesTest.kt`
- `app/src/test/java/com/whatsforlunch/app/ShoppingListViewModelTest.kt`
- `app/src/test/java/com/whatsforlunch/app/InMemoryShoppingListRepositoryTest.kt`
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt`

**Modificados**
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` (v3, `MIGRATION_2_3`, `shoppingListDao()`)
- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheProposalRules.kt` (`missingRequired` delega en `ShoppingListRules`)
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` y `RecipeDetailViewModel.kt` (banner, «Añadir a la lista», aviso)
- `app/src/main/java/com/whatsforlunch/app/ui/recommendations/RecommendationsScreen.kt` y `RecommendationsViewModel.kt` («Te falta: …», carrito)
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` y `PincheViewModel.kt` (botón de la Ficha)
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` y `MyRecipesRoute.kt` (acción de la Lista)
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` (ruta `shopping_list`)
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` (DAO, repositorio, ViewModel)
- `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml` (paridad es/en)

## Visual Design Impact

Sin cambios de sistema visual (tokens, paleta y tipografía intactos). Adaptaciones sobre `Warm Culinary Craft`:

- **Detalle de Receta**: banner bajo la cabecera «Ingredientes» — texto «¡Tienes el X% de ingredientes en casa!» en `secondary`, con icono `verified` al 100% y de aviso por debajo; botón de texto «Añadir a la lista» (icono `playlist_add`) a la derecha de la cabecera, solo con Faltantes.
- **Tarjeta compatible**: línea «Te falta: {nombres}» en `on-surface-variant` y botón de carrito (`add_shopping_cart`) de 48 dp junto al badge de Coincidencia.
- **Ficha del Pinche**: botón secundario «Añadir faltantes a la Lista de la Compra» al lado de «Guardar en Mis Recetas»; tras pulsarlo, label «Añadido a la Lista de la Compra» deshabilitado (mismo tratamiento que «Guardada en Mis Recetas»).
- **Lista de la Compra** (prototipo `chefencasa_lista_de_la_compra_sincronizada/`, que inspira la pantalla): título «Lista de la Compra» y «Volver»; filas con nombre + cantidad en paréntesis y badge de Origen en pastilla `primary` (icono `skillet`; «Despensa baja» no aparece en esta feature); borrado con icono `delete`; aviso «Ítem eliminado» con «Deshacer»; estado vacío con título, texto de apoyo y CTA «Escanear la Despensa».

**Contrato visual verificable**: con 1 receta añadida, la Lista muestra exactamente 2 filas (los 2 Faltantes del fixture) con «Para: {Receta}»; con 2 Recetas que comparten «Harina», una única fila «Harina (700 g)» con «Para: 2 recetas».

## Durable Documentation Impact

- **Update** `docs/domain-model.md`: «ÍtemLista» gana `unidad` y sus fuentes; nueva **RD18**: «Al añadir Faltantes a la Lista de la Compra se crea un ÍtemLista por nombre normalizado; cada fuente suma su cantidad una sola vez (unidades iguales), repetir la acción desde la misma fuente no cambia nada y el Origen conserva todas las fuentes («Para: {Receta}» / «Para: N recetas»)». Casos límite de la Lista ya documentados; se añade el de unidades distintas.
- **Update** `docs/risks-and-open-questions.md`: registrar `Q-anadir-faltantes-a-la-lista-1..7` (superficies, cantidad/duplicados, alcance de pantalla, Pasillo, idempotencia, badge multi-fuente, borrado con «Deshacer») como resueltas por el usuario el 2026-10-26, con origen «planificación de la spec».
- **Update** `DESIGN.md`: actualizar la fila «Lista de la Compra» (primera versión: lista plana con badges, borrado y estado vacío) y la nota de la Ficha del Pinche (el botón «Añadir faltantes a la Lista de la Compra» ya aparece).
- **Update** `docs/technical-discovery.md`: Room v3 con `shopping_items` y `shopping_item_sources`; comandos sin cambios.
- **Update** `PROGRESS.md` + `docs/progress-history.md`: al cerrar la implementación (labor del harness, no de esta planificación).
- **Not needed** `CONTEXT.md`: Faltante, Origen, ÍtemLista y Lista de la Compra ya están definidos y esta feature no cambia su significado.
- **Not needed** `AGENTS.md`: estructura, comandos, convenciones y términos intactos.
- **Not needed** `docs/build-brief.md`: alcance MVP sin cambios (la Lista ya figura en él).

## Implementation Plan

1. **Dominio puro**: modelo `ShoppingListItem`/`ShoppingItemSource`, `ShoppingListRules` (Faltantes + fusión idempotente) y delegación de `PincheProposalRules.missingRequired`; tests unitarios de las reglas primero (T1–T2).
2. **Persistencia**: entidades, DAO, `MIGRATION_2_3`, repositorio Room + InMemory y su test; comprobar la migración con una BD v2 de prueba (T3–T4).
3. **Pantalla Lista**: ViewModel, Screen con badges/borrado/«Deshacer»/estado vacío, ruta, acción en la top bar de Mis Recetas y textos es/en (T5–T7).
4. **Superficies de alta**: Detalle (banner + botón), tarjetas de «Recetas con mi Despensa», Ficha del Pinche; tests unitarios de ViewModels y `ShoppingListE2ETest` (T8–T10).

Cada fase deja `lint` y `unit` en verde antes de pasar a la siguiente.

## Implementation Tasks

- **T1**: `ShoppingListItem.kt` y `ShoppingListRules.kt` (Faltantes, `itemIdFor`, `addContributions`).
- **T2**: `ShoppingListRulesTest` (obligatorios, orden, sin duplicados, suma por fuente, idempotencia, unidades distintas, cantidad 0) y delegación de `PincheProposalRules.missingRequired`.
- **T3**: `ShoppingListItemEntity.kt`, `ShoppingItemSourceEntity`, `ShoppingItemWithSources`, `ShoppingListDao` y `MIGRATION_2_3` (v3).
- **T4**: `ShoppingListRepository`, `RoomShoppingListRepository` (Mutex + transacción) e `InMemoryShoppingListRepository` + `InMemoryShoppingListRepositoryTest`.
- **T5**: `ShoppingListViewModel` + `ShoppingListScreen` (lista, badge de Origen, borrado + «Deshacer», estado vacío) + `ShoppingListRoute` y ruta `shopping_list`.
- **T6**: acción `action_open_shopping_list` en la top bar de Mis Recetas y navegación hasta el Escáner desde el CTA vacío.
- **T7**: textos es/en y testTags (`ShoppingListTestTags`).
- **T8**: Detalle (banner «¡Tienes el X%…», «Añadir a la lista», aviso) con cantidades visibles.
- **T9**: tarjetas de «Recetas con mi Despensa» («Te falta: …» + carrito) y Ficha del Pinche («Añadir faltantes…» → «Añadido…» deshabilitado).
- **T10**: `ShoppingListViewModelTest`, ajustes en `RecipeDetailViewModelTest`/`PincheViewModelTest` y `ShoppingListE2ETest` (S1–S8).

## Verification Plan

1. `./init.ps1` (o `./init.sh`): entorno y arranque en verde antes de empezar y al cerrar.
2. `./lint.ps1` (`lintDebug`): 0 errores.
3. `./unit.ps1` (`testDebugUnitTest --rerun-tasks`): todas las suites en verde, incluidos los tests nuevos; `PincheProposalRulesTest` sin cambios sigue pasando.
4. `./e2e.ps1` (`connectedDebugAndroidTest` sobre el emulador, fixtures locales con `E2eDatabase.clear()`, `FakePincheChatClient` para la Ficha): **S1** = verify 1 del `feature_list` (Detalle con ausentes → ítems en la Lista con badge de origen); **S2** = verify 2 (repetir sin duplicar ni doblar); S3–S8 según los escenarios; sin llamadas a la API de IA (feature sin IA, no requiere confirmación humana).
5. `assembleDebug` sin errores.
6. **Migración 2→3**: con datos de prueba (Recetas + Despensa previas), comprobar al arrancar que las tablas antiguas conservan sus datos y que `shopping_items`/`shopping_item_sources` existen (captura o log en el informe).
7. `init.sh` / `init.ps1`: **sin cambios** (siguen ejecutando sus comprobaciones de arranque no bloqueantes; esta feature no añade servicios, modos ni dependencias nuevas).

## Evidence To Capture

- `docs/evidence/anadir-faltantes-a-la-lista/informe.md`: entorno, comandos con resultado (init, lint, unit, assemble, e2e), desglose S1–S8 y nota de la migración 2→3.
- 2 capturas: (1) Detalle con banner y «Añadir a la lista»; (2) Lista de la Compra con ítems y badges de Origen.
- Resumen en el `evidence` de `feature_list.json` (lo único que se toca de ese archivo) y entrada en `docs/progress-history.md`.

## Trazabilidad

| Fuente (`feature_list.json`) | Spec |
|---|---|
| «Detectar los ingredientes obligatorios ausentes al ver una receta o tras un escaneo» | RF-AFL1, RF-AFL3, RF-AFL4 · S1, S4, S5 |
| «Crear los ítems de la Lista de la Compra con su Origen según RD09» | RF-AFL2, RF-AFL5, RF-AFL6, RF-AFL7 · S1, S3, S6, S7 |
| verify 1 «E2E: abrir una receta con ingredientes ausentes y comprobar que sus ítems aparecen en la Lista con el badge de origen» | S1 |
| verify 2 «E2E: repetir la acción y comprobar que no se duplican los ítems» | S2 |
| notes «Cada ítem conserva su Origen (RD09)» | RF-AFL5, D6 · S1, S3 |

## Validator Checklist

- [ ] ¿Los dos `verification` de `feature_list.json` están ejecutados como S1 y S2 en el emulador con resultado legible?
- [ ] ¿Repetir la acción es idempotente (sin filas nuevas **y sin doblar la suma**) y dos Recetas distintas sí suman?
- [ ] ¿El Origen se conserva según RD09 («Para: {Receta}» / «Para: N recetas» con títulos accesibles)?
- [ ] ¿Los Faltantes son solo obligatorios ausentes por `normalizeName`, en orden y sin duplicados (RD15 sin romperse)?
- [ ] ¿El Detalle aporta cantidades visibles (RD03) y las tarjetas/Ficha las base?
- [ ] ¿Nada de `agrupar-y-completar-la-lista` (tabs, checkboxes, progreso) ni de `anadir-item-a-la-lista` (alta manual/voz, Pasillo, «Despensa baja») se ha colado?
- [ ] ¿La migración 2→3 conserva las Recetas y la Despensa previas?
- [ ] ¿Paridad es/en de todos los textos y contratos estables (testTags, rutas) sin romper los E2E existentes?
- [ ] ¿`init`, `lint` y `unit` en verde, `assembleDebug` sin errores y evidencia + `feature_list.json` actualizados?
