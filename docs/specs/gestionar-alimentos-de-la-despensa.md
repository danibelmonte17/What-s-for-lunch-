# Feature Implementation Spec: Gestionar alimentos de la Despensa

## Source Feature

- `id`: gestionar-alimentos-de-la-despensa
- `area`: funcional
- `depends_on`: arrancar-app-en-mis-recetas (`accepted`)
- `status`: `accepted` a fecha de esta revisión (el cambio de la revisión devuelve la feature a `in_progress` hasta revalidarla)
- `source`: `feature_list.json`

> **Revisión 2026-10-21 (delta sobre la cantidad máxima, decisión del usuario):** la cantidad del Alimento pasa a tener un máximo de **99** (rango entero **0..99**; **0 = sin cantidad**) en toda la app; `PantryItemValidation.MAX_QUANTITY` pasa de 999 a 99 y arrastra el stepper de la Despensa («+» deshabilitado en 99), las guardias de los repositorios y los tests de límites (Q-escanear-despensa-con-camara-4). Sin migración de esquema Room: los valores ya guardados por encima de 99 se recortan con la guardia de dominio. Tras implementarlo, la feature debe pasar de nuevo por `feature-validator` antes de volver a `accepted`.

> **Revisión 2026-10-20 (delta sobre la feature ya aceptada):** la cantidad del Alimento deja de escribirse con un campo de texto (número decimal) y pasa a elegirse con un stepper «− número +» de **siempre enteros** (rango 0..99; **0 = sin cantidad**), sin ningún campo de texto para la cantidad. Esta revisión sustituye el comportamiento de la Q-gestionar-alimentos-de-la-despensa-4 («decimal opcional por texto»). El delta de implementación vive en «Implementation Tasks» (R1-R6); tras implementarlo, la feature debe pasar de nuevo por `feature-validator` antes de volver a `accepted`.

## Goal

El usuario mantiene el listado de la **Despensa** («todos los alimentos del hogar en un único listado», `CONTEXT.md`): una pantalla «Despensa» nueva con alta, edición y baja de **Alimentos**, persistida en Room para que sobreviva a los reinicios de la app. Sin desglose por ubicaciones físicas (RD11). Es la base de datos sobre la que después calcularán la Coincidencia (`mostrar-badge-de-coincidencia`) y que llenará el Escáner (`escanear-despensa-con-camara`); esta feature no toca nada de eso.

Decisiones de UX confirmadas por el usuario el 2026-10-20 (`docs/risks-and-open-questions.md`, Q-gestionar-alimentos-de-la-despensa-1..4): pantalla «Despensa» nueva con ruta propia y acceso desde una acción en la barra superior de Mis Recetas; borrado con diálogo de confirmación con el nombre del Alimento (mismo patrón que `eliminar-receta`); duplicado por nombre normalizado → aviso y no se crea fila nueva; cantidad = número **entero** opcional sin unidad seleccionado con un stepper «− número +» (0..99, 0 = sin cantidad), decisión revisada el mismo día 2026-10-20 que sustituye a la Q-…-4 original (decimal escrito a mano).

## Non-Goals

- Escaneo con cámara, detección MediaPipe y chips de detección: `escanear-despensa-con-camara`.
- Cálculo ni badge de Coincidencia (RD01/RD02/RD10): `mostrar-badge-de-coincidencia`.
- Desglose por ubicaciones físicas (nevera/vinagrera/especiero): prohibido por RD11 (`P37`).
- Unidades de medida, caducidades, precios o stock mínimo: el Alimento solo tiene nombre y cantidad opcional.
- Decimales o fracciones en la cantidad: prohibidos por decisión de 2026-10-20; la cantidad es siempre un entero entre 0 y 99 y no admite entrada de texto (ni teclado, ni escritura directa, ni coma decimal).
- Búsqueda, filtro, ordenación configurable, selección múltiple o borrado masivo del listado.
- Varias Despensas, compartición o sincronización (`P7`, `P31`).
- Cambiar la navegación existente: la ruta del Escáner (`escanear_despensa`) y el estado vacío de Mis Recetas no se tocan; solo se añade una acción en la barra superior de Mis Recetas.
- Modificar la regla de fusión de duplicados del Escaneo («se fusionan en un solo Alimento», `docs/domain-model.md`): esta feature solo define el alta/edición manual, que deja el mismo resultado (un Alimento por nombre normalizado) mediante aviso.

## Job Story

Cuando quiero tener al día los alimentos que hay en casa,
quiero añadir, corregir y quitar Alimentos de la Despensa desde una lista única,
para que la app sepa lo que tengo disponible.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P31`): gestiona sus Alimentos. Nada de esta feature exige permisos adicionales.

## Acceptance Scenarios

### Scenario 1: Añadir «Tomate fresco» (verify 1 de `feature_list.json`)

Given la Despensa abierta (vacía o con otros Alimentos)
When el usuario pulsa «Añadir alimento», escribe «Tomate fresco» y guarda
Then el listado de la Despensa muestra «Tomate fresco»

### Scenario 2: Eliminar un Alimento (verify 2 de `feature_list.json`)

Given un Alimento existente en la Despensa
When el usuario pulsa «Eliminar», confirma en el diálogo y el borrado se aplica
Then el Alimento desaparece del listado de la Despensa

### Scenario 3: Cancelar el borrado

Given el diálogo de confirmación de borrado abierto con el nombre del Alimento
When el usuario pulsa «Cancelar» o descarta el diálogo (atrás o toque fuera)
Then el diálogo se cierra y el Alimento sigue en la Despensa

### Scenario 4: Editar un Alimento

Given un Alimento con nombre «Tomate fresco» y sin cantidad
When el usuario lo edita a nombre «Tomate maduro» y cantidad 3 (tres toques en «+» del stepper)
Then el listado muestra «Tomate maduro» con su cantidad, sin crear un segundo Alimento

### Scenario 5: Duplicado por nombre normalizado (Q-…-3)

Given un Alimento «Tomate fresco» en la Despensa
When el usuario añade o renombra otro a «  TOMATE  fresco » (mayúsculas, espacios redundantes y tildes; minúsculas, colapso de espacios, sin tildes y trim)
Then no se crea un segundo Alimento y el formulario muestra el aviso «Este alimento ya está en la Despensa»

### Scenario 6: Nombre obligatorio

Given el formulario de alta/edición abierto
When el campo nombre está vacío o solo tiene espacios
Then la acción de guardar está deshabilitada (o, tras intentarlo, no se guarda nada)

### Scenario 7: Cantidad por stepper, solo enteros (revisión 2026-10-20; sustituye a la Q-…-4)

Given el formulario de alta/edición con la cantidad en 0
When el usuario pulsa «+» o «−» del stepper de cantidad
Then la cantidad cambia de 1 en 1 en valores enteros entre 0 y 99; «−» está deshabilitado en 0 y «+» está deshabilitado en 99; no existe ningún campo de texto para la cantidad; al guardar con 0 el Alimento se guarda sin cantidad y la tarjeta no muestra línea de cantidad

### Scenario 8: Despensa vacía

Given la Despensa sin ningún Alimento
When el usuario la abre
Then se muestra el estado vacío con título, mensaje y la acción «Añadir alimento»

### Scenario 9: Persistencia en Room (step 2 de `feature_list.json`)

Given al menos un Alimento guardado en la Despensa
When la app se reinicia (actividad recreada / proceso nuevo)
Then los Alimentos siguen en el listado con sus cantidades

### Scenario 10: Textos es/en y objetivos táctiles

Given la app en español o en inglés
When se abre la Despensa y sus diálogos
Then todos los textos visibles cambian con el locale y las acciones tienen área táctil ≥ 48 dp

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `gestionar-alimentos-de-la-despensa` (steps, verification, notes con RD11); `arrancar-app-en-mis-recetas` en `accepted`.
- `CONTEXT.md` — términos «Despensa», «Alimento»; «Despensa = un único listado»; «nevera/inventario» rechazados.
- `docs/build-brief.md` — MVP sin backend, datos locales; «Desglose por ubicaciones físicas» fuera (RD11/P37).
- `docs/domain-model.md` — Alimento (nombre, cantidad opcional), RD11, validaciones («Nombre … no vacío; los duplicados (mismo nombre normalizado) se fusionan»).
- `docs/technical-discovery.md` — Room en uso (2.8.5 + KSP 2.3.12), MVVM + StateFlow, Koin, capas `ui`/`domain`/`data`, comandos Gradle.
- `docs/risks-and-open-questions.md` — RK04 (lagunas visuales), Q-… resueltas como patrón de registro; Q-gestionar-alimentos-de-la-despensa-1..4 (resueltas con el usuario el 2026-10-20; volcadas al «Apéndice · Registro de preguntas resueltas»).
- `DESIGN.md`, `uxdocs/DESIGN.md` — tokens «Warm Culinary Craft»; ninguna pantalla Despensa en los prototipos (laguna tipo RK04).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes` (`mis_recetas`, `crear_receta?recipeId=`, `escanear_despensa`), destino provisional del Escáner.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — patrón de TopAppBar con «Volver» (`action_back`).
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt`, `MisRecetasRoute.kt`, `MisRecetasViewModel.kt` — `Scaffold` + `TopAppBar` sin `actions` hoy; patrón de diálogo (`AlertDialog` de borrado con «Cancelar»/«Eliminar» tinte error), snackbar de error, `MisRecetasTestTags`.
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeIngredient.kt` — modelos de dominio con `id: String`.
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/RecipeSearchFilter.kt` — normalización de nombres (trim, lowercase `Locale.ROOT`, NFD sin `\p{Mn}`).
- `app/src/main/java/com/whatsforlunch/app/domain/crearreceta/RecipeValidation.kt` — patrón de validación pura en `domain/`.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — contrato en `domain/repository/` sobre `Flow`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt`, `RecipeDao.kt`, `RecipeEntity.kt`, `IngredientEntity.kt` — esquema Room actual (`whatsforlunch.db`, **version 1**, `exportSchema = false`, enums como texto).
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — mapeo entity↔domain y doble en memoria para tests.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin: `Room.databaseBuilder(..., "whatsforlunch.db")`, DAO, repositorio, `viewModel {}`.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — convención de claves por pantalla (`mis_recetas_*`, `crear_receta_*`) y acciones (`action_*`).
- `app/src/androidTest/java/com/whatsforlunch/app/E2eDatabase.kt`, `EliminarRecetaE2ETest.kt`, `BuscarYFiltrarRecetasE2ETest.kt` — E2E Compose con `testTag`, `E2eDatabase.clear()` sobre la DB real.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `material-icons-extended` y `kotlinx-coroutines-test` disponibles.
- (revisión 2026-10-20) `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — diálogo de alta/edición con `OutlinedTextField` de cantidad (`KeyboardType.Decimal`), testTags `QUANTITY_FIELD`/`QUANTITY_ERROR`, helper `Double.toDisplayString()` y tarjeta que muestra «Cantidad: N»; es el campo de texto a sustituir por el stepper.
- (revisión) `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryViewModel.kt` — `saveItem(id, name, quantityText: String)` con `PantrySaveError.InvalidQuantity` (se simplifica al entero).
- (revisión) `app/src/main/java/com/whatsforlunch/app/domain/pantry/PantryItemValidation.kt` — `parseQuantity`/`isQuantityValid` sobre texto (se sustituyen por rango entero).
- (revisión) `app/src/main/java/com/whatsforlunch/app/domain/model/PantryItem.kt`, `data/local/PantryItemEntity.kt`, `data/local/PantryItemDao.kt`, `data/repository/RoomPantryRepository.kt`, `data/repository/InMemoryPantryRepository.kt` — `quantity: Double?` en dominio, entidad (`REAL?`) y repositorios con `require` de no negativo.
- (revisión) `app/src/test/java/com/whatsforlunch/app/domain/pantry/PantryItemValidationTest.kt`, `app/src/test/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepositoryTest.kt`, `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt`, `PantryLocalizationE2ETest.kt` — tests que hoy escriben la cantidad como texto («3.5», «-1») y hay que reescribir.
- (revisión) `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `pantry_quantity_label`, `pantry_quantity_value` y `pantry_quantity_error`.

### Existing Patterns To Follow

- MVVM + StateFlow: `*Route` (cablea ViewModel) → `*Screen` (sin estado) → `*ViewModel` con `uiState`; reglas de negocio en `domain/` sin Compose; Koin `viewModel {}`.
- Room: entidades en `data/local/`, DAO con `Flow`, repositorio `Room*Repository` con mapeo a `domain/model`, doble `InMemory*Repository` para unit.
- testTags centralizados en un objeto por pantalla (`MisRecetasTestTags`); diálogos con tags propios; nombres accesibles vía `contentDescription`.
- Diálogo de confirmación de borrado con nombre del elemento, «Cancelar»/«Eliminar» (tinte `error`) y descarte = cancelar (patrón de `eliminar-receta`).
- Validaciones puras en `domain/` (patrón `RecipeValidation`); normalización de nombres como en `RecipeSearchFilter`.
- Cadenas con prefijo de pantalla, duplicadas en `values/` y `values-en/`; identificadores de código en inglés (`P19`, `P25`).
- E2E sobre la app real con `E2eDatabase.clear()` en `@Before` y locale inyectado para NFR de idioma.

### Current Gaps

- No existe nada de la Despensa: ni modelo, ni tabla, ni repositorio, ni pantalla, ni ruta.
- `RecipeDatabase` está en **version 1** ya instalada en el emulador con datos de Recetas: añadir la tabla exige subir a version 2 con `Migration` explícita; **la migración destructiva está prohibida** (borraría las Recetas del usuario).
- Ningún prototipo de `uxdocs/` cubre la pantalla Despensa (laguna tipo `RK04`): el diseño se resuelve con los tokens de `DESIGN.md` y el copy lo propone la spec (Q-arrancar-app-en-mis-recetas-5).
- `MisRecetasScreen` no tiene `actions` en su `TopAppBar` (hay que añadir la entrada a la Despensa).

### Current Gaps (revisión 2026-10-20)

- La cantidad se implementó como campo de texto decimal: hay que sustituirlo por el stepper sin dejar restos (`KeyboardOptions`/`KeyboardType`, `parseQuantity`, `pantry_quantity_error`, `Double.toDisplayString()`).
- `PantryItem.quantity` es `Double?` en dominio y entidad: el entero vive en dominio y la columna `REAL?` de Room se conserva para no migrar (enfoque técnico 2).
- `PantrySaveError.InvalidQuantity` y su error inline desaparecen de la UI; los E2E que hoy escriben «3.5»/«-1» en el campo de cantidad deben reescribirse contra el stepper.

## Technical Approach

1. **Dominio** (`domain/`): modelo `PantryItem` (= Alimento; `id: String`, `name: String`, `quantity: Int?`; `null` = sin cantidad) en `domain/model/`. Contrato `PantryRepository` en `domain/repository/`: `observePantryItems(): Flow<List<PantryItem>>`, `savePantryItem(item): PantrySaveResult` (`Saved` | `DuplicateName`; cubre alta y edición), `deletePantryItem(id)`. Validación pura en `domain/pantry/PantryItemValidation`: nombre no vacío tras `trim`; cantidad `null` o entero en rango con `MIN_QUANTITY = 0` y `MAX_QUANTITY = 99` (`isQuantityValid(quantity: Int)`); se eliminan `parseQuantity` e `isQuantityValid(String)` (ya no hay texto que parsear). Normalización compartida con búsqueda: trim, `lowercase(Locale.ROOT)`, NFD sin marcas y espacios consecutivos reducidos a uno (necesario para que «TOMATE  fresco» coincida con «Tomate fresco»); el helper se reutiliza desde `RecipeSearchFilter` y sus 19 unit deben seguir en verde.
2. **Datos (Room)**: sin cambios de esquema en la revisión: la tabla sigue en **versión 2** y la columna `quantity REAL?` se conserva (una migración 2→3 solo para cambiar el tipo de columna añadiría riesgo sin beneficio de producto). `PantryItemEntity` mantiene `quantity: Double?`; el mapeo `Int?` ↔ `Double?` vive en los repositorios: al escribir `?.toDouble()`, al leer `?.roundToInt()?.coerceIn(MIN_QUANTITY, MAX_QUANTITY)` como protector ante decimales heredados de la versión con campo de texto (p. ej. 3.5 → 4). El resto sin cambios: `PantryItemEntity` (tabla `pantry_items`: `id TEXT PK`, `name TEXT`, `normalizedName TEXT`, `quantity REAL?`) con índice **único** sobre `normalizedName`, `PantryItemDao` (`ORDER BY name COLLATE NOCASE ASC`, `insert` ABORT, `update`, `deleteById`, `findByNormalizedName`), `MIGRATION_1_2` intacta (sin `fallbackToDestructiveMigration`) y `RoomPantryRepository` con control de duplicados.
3. **Doble en memoria**: `InMemoryPantryRepository` con `MutableStateFlow` para unit (mismas reglas de duplicado y no-op al borrar id inexistente).
4. **ViewModel/UI**: `ui/pantry/` con `PantryRoute` (Koin `koinViewModel()`), `PantryViewModel` (expone `uiState` con los Alimentos + flags de error de guardado, y acciones `add`, `update`, `delete`) y `PantryScreen`. La pantalla: `Scaffold` con `TopAppBar` título «Despensa» y acción «Volver» (patrón `ProvisionalScreen`), lista de tarjetas (nombre + «Cantidad: N» solo cuando el Alimento tiene cantidad) con acciones «Editar» y «Eliminar» por fila, `ExtendedFloatingActionButton` «Añadir alimento» y estado vacío con CTA. Alta/edición en un `AlertDialog`: el campo nombre se queda como `OutlinedTextField`; la cantidad pasa a ser un **stepper** en fila (etiqueta «Cantidad (opcional)», botón «−» `Icons.Outlined.Remove` con testTag `pantry_quantity_decrement`, valor centrado con testTag `pantry_quantity_value`, botón «+» `Icons.Outlined.Add` con testTag `pantry_quantity_increment`; la fila entera con testTag `pantry_quantity_stepper`). Valor inicial 0 en alta; en edición la cantidad del Alimento o 0 si no tiene. «−» deshabilitado en 0, «+» deshabilitado en 99; **sin campo de texto ni teclado para la cantidad**. Al guardar, 0 se persiste como `null` (sin cantidad). `PantryViewModel.saveItem(id: String?, name: String, quantity: Int)` sin parseo de texto y sin `PantrySaveError.InvalidQuantity` (el stepper no puede producir valores inválidos; el `require` de rango en los repositorios queda como guardia de dominio). Duplicado → error inline bajo el nombre y el diálogo sigue abierto; guardado deshabilitado con nombre vacío. Baja en el `AlertDialog` de confirmación de `eliminar-receta` (nombre del Alimento, «Cancelar»/«Eliminar» tinte `error`, descarte = cancelar, borrado definitivo sin papelera).
5. **Navegación**: `AppRoutes.PANTRY = "pantry"` + `composable` con `PantryRoute(onBack = popBackStack)`; entrada nueva en `MisRecetasScreen`: `IconButton` en `actions` del `TopAppBar` (visible tanto en estado vacío como con Recetas) con icono `Icons.Outlined.Inventory2`, `contentDescription` «Despensa»/«Pantry» y `testTag(action_open_pantry)`; `MisRecetasRoute` y `AppNavHost` cablean `onOpenPantry`.
6. **Recursos**: claves `pantry_*` en `values/strings.xml` y `values-en/strings.xml` (NFR idioma): título «Despensa»/«Pantry», «Añadir alimento»/«Add food», «Editar alimento»/«Edit food», «Guardar cambios»/«Save changes», «Nombre»/«Name», «Cantidad (opcional)»/“Quantity (optional)” (etiqueta del stepper), «Cantidad: %1$s»/“Quantity: %1$s”, «Eliminar alimento»/«Delete food», mensaje «¿Eliminar «{nombre}»? Esta acción es definitiva y no se puede deshacer.», «Este alimento ya está en la Despensa.»/“This food is already in your Pantry.”, estado vacío («Tu Despensa está vacía»/“Your Pantry is empty” + «Añade los alimentos que tienes en casa.»/“Add the foods you have at home.”), más `action_back` reutilizado; **nuevas** `pantry_quantity_decrease` «Disminuir cantidad»/“Decrease quantity” y `pantry_quantity_increase` «Aumentar cantidad»/“Increase quantity” (`contentDescription` de los botones del stepper); **eliminada** `pantry_quantity_error` (la cantidad ya no puede ser inválida). Identificadores nuevos de código en inglés; los identificadores españoles existentes no se tocan (son de `revisar-identificadores-en-ingles`).
7. **Tests**: unit (`app/src/test`) sobre `InMemoryPantryRepository` + `PantryItemValidation` + normalización (alta, edición, duplicado, borrado, id inexistente, nombre vacío y cantidad entera: 0 = sin cantidad, límites 0/99, fuera de rango rechazado en el `require` de los repositorios). E2E `PantryManagementE2ETest` (`app/src/androidTest`) sobre la **DB real** con `E2eDatabase.clear()` (la persistencia es parte del alcance), navegando desde Mis Recetas a la Despensa y usando el stepper (toques en «+»/«−», sin `performTextInput` en la cantidad).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/PantryItem.kt` — crear; modelo Alimento.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt` — crear; contrato + `PantrySaveResult`.
- `app/src/main/java/com/whatsforlunch/app/domain/pantry/PantryItemValidation.kt` — crear; validaciones puras (nombre, cantidad).
- `app/src/main/java/com/whatsforlunch/app/domain/text/NameNormalization.kt` (o equivalente) — crear; normalización compartida (ver enfoque 1).
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/RecipeSearchFilter.kt` — modificar solo si se extrae la normalización; comportamiento intacto.
- `app/src/main/java/com/whatsforlunch/app/data/local/PantryItemEntity.kt` — crear; entidad `pantry_items` + índice único.
- `app/src/main/java/com/whatsforlunch/app/data/local/PantryItemDao.kt` — crear; CRUD + observación.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — modificar; entidad nueva, version 2 y `MIGRATION_1_2` (sin migración destructiva).
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt` — crear; implementación Room con control de duplicados.
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepository.kt` — crear; doble para unit.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `addMigrations(MIGRATION_1_2)`, DAO, repositorio y `PantryViewModel`.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt`, `PantryRoute.kt`, `PantryViewModel.kt` — crear; pantalla, cableado y estado.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; ruta `pantry` y wiring.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt`, `MisRecetasRoute.kt` — modificar; acción `action_open_pantry` en la barra superior (ambos estados) y callback `onOpenPantry`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; cadenas `pantry_*`.
- `app/src/test/java/com/whatsforlunch/app/domain/pantry/PantryItemValidationTest.kt`, `app/src/test/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepositoryTest.kt` — crear; unit (nombres a criterio del implementador, en inglés).
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt` — crear; E2E de S1-S9 (S10 con locale `en-US`).
- `docs/domain-model.md` — aplicado («Validaciones de datos»: el duplicado en alta/edición manual se rechaza con aviso, Q-…-3).
- `docs/risks-and-open-questions.md` — aplicado (Q-gestionar-alimentos-de-la-despensa-1..4 en el «Apéndice · Registro de preguntas resueltas»).

### Expected File Changes (delta de la revisión 2026-10-20)

- `app/src/main/java/com/whatsforlunch/app/domain/model/PantryItem.kt` — modificar; `quantity: Int?` (`null` = sin cantidad).
- `app/src/main/java/com/whatsforlunch/app/domain/pantry/PantryItemValidation.kt` — modificar; `MIN_QUANTITY`/`MAX_QUANTITY` (0/99) e `isQuantityValid(quantity: Int)`; eliminar `parseQuantity` e `isQuantityValid(String)`.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryViewModel.kt` — modificar; `saveItem(id, name, quantity: Int)`, quitar `PantrySaveError.InvalidQuantity` y su manejo.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — modificar; stepper «− valor +» sustituyendo al `OutlinedTextField` de cantidad; nuevos testTags `pantry_quantity_stepper`/`_value`/`_increment`/`_decrement`; quitar `QUANTITY_FIELD`, `QUANTITY_ERROR`, `KeyboardOptions`/`KeyboardType` y `Double.toDisplayString()`; tarjeta sin línea de cantidad cuando `quantity == null`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt`, `InMemoryPantryRepository.kt` — modificar; `require` de rango entero (0..99) y mapeo `Int?` ↔ `Double?` en Room (con redondeo al leer).
- `app/src/main/java/com/whatsforlunch/app/data/local/PantryItemEntity.kt`, `PantryItemDao.kt`, `RecipeDatabase.kt` — sin cambios de esquema (se confirma `quantity REAL?`, versión 2 y `MIGRATION_1_2` intacta).
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; quitar `pantry_quantity_error`, añadir `pantry_quantity_decrease`/`pantry_quantity_increase`.
- `app/src/test/java/com/whatsforlunch/app/domain/pantry/PantryItemValidationTest.kt`, `app/src/test/java/com/whatsforlunch/app/data/repository/InMemoryPantryRepositoryTest.kt` — modificar; casos enteros (0 = sin cantidad, límites 0/99).
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt` — modificar; S4 con toques del stepper y S7 acotada a los límites (sin texto).
- `app/src/androidTest/java/com/whatsforlunch/app/PantryLocalizationE2ETest.kt` — modificar; etiquetas y `contentDescription` es/en del stepper, área táctil ≥ 48 dp.
- `docs/domain-model.md` — modificar; «Validaciones de datos»: la cantidad del Alimento es un entero 0..99 y 0 = sin cantidad (antes: «cantidad (opcional)» sin tipo).
- `docs/risks-and-open-questions.md` — modificar; anotar en Q-gestionar-alimentos-de-la-despensa-4 que el 2026-10-20 se sustituyó el decimal por texto por un stepper entero 0..99 (0 = sin cantidad).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); **sin prototipo** para la Despensa (laguna tipo `RK04`): el copy y la composición los propone esta spec y se validan visualmente (Q-arrancar-app-en-mis-recetas-5).
- Screens or states affected: «Despensa» (nueva: lista, vacío, diálogos de alta/edición y de borrado, error de duplicado, y el stepper de cantidad con sus estados deshabilitados en 0 y 99); Mis Recetas (solo la nueva acción de la barra superior, sin tocar su contenido).
- New design artifact required: no — se resuelve con componentes M3 y los tokens existentes.

Criterios visuales (verificables con captura + `uiautomator`, como en features anteriores):

- Superficies y color por roles de `uxdocs/DESIGN.md`: fondo/superficie `#FBF9F5`, tarjetas `surface-container-lowest` (#FFFFFF) con radio `rounded.lg` (1 rem) y sombra cálida `shadow-sm` ámbar-carbón; texto `on-surface` #1B1C1A y secundario `on-surface-variant` #59413B; acciones primarias en `primary` #A83212 con texto #FFFFFF; el error en `error` #BA1A1A (solo el «Eliminar» del diálogo).
- Campos del formulario con el estilo de formulario ya validado en `afinar-fidelidad-visual-del-formulario`: relleno `surface-container-highest` (#E4E2DE), anillo de foco terracota, estado deshabilitado distinguible.
- Objetivos táctiles ≥ 48 dp en todas las acciones (incluidas las «Editar»/«Eliminar» por fila y la acción de la barra superior de Mis Recetas); botón primario estándar con altura mínima 52 dp (la excepción compacta 36/48 dp del pie de tarjeta de Mis Recetas no se traslada aquí).
- Stepper de cantidad (revisión 2026-10-20): fila con etiqueta «Cantidad (opcional)», botones «−»/«+» (`IconButton` con área táctil ≥ 48 dp) y valor entero centrado; estado deshabilitado distinguible («−» en 0, «+» en 99) con `IconButton` deshabilitado de Material (tono atenuado); sin teclado ni foco de texto; color del valor `on-surface`, coherente con el estilo de formulario relleno ya validado.
- Iconos de `material-icons-extended`: `Icons.Outlined.Inventory2` (entrada en Mis Recetas), `Icons.Outlined.Add`, `Icons.Outlined.Edit`, `Icons.Outlined.Delete`, y un icono de estado vacío (p. ej. `Icons.Outlined.Kitchen`) dentro de un círculo de 56 dp sobre `surface-container` #EFEEEA, al estilo del estado «sin resultados» de Mis Recetas.
- Copy propuesto (es/en) en «Enfoque técnico» punto 6; la revisión visual puede ajustarlo sin cambiar comportamiento.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas `ui`/`domain`/`data` no cambian (solo se añade un agregado PantryItem con el patrón ya usado por Recetas).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD11 ya vive en `docs/domain-model.md`).
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — update (además del aviso de duplicados ya aplicado, la revisión de 2026-10-20 cambia la cantidad del Alimento a entero 0..99 con 0 = sin cantidad); `docs/risks-and-open-questions.md` — update (además del registro Q-…-1..4 ya aplicado, anotar la revisión de la Q-…-4 al stepper entero).

Las preguntas Q-gestionar-alimentos-de-la-despensa-1..4 se resolvieron con el usuario el 2026-10-20 y su registro completo (pregunta, origen y estado) vive en `docs/risks-and-open-questions.md` («Apéndice · Registro de preguntas resueltas»).

## Implementation Plan

1. Dominio: `PantryItem`, `PantryRepository`/`PantrySaveResult`, `PantryItemValidation` y normalización compartida, con unit.
2. Room: entidad + DAO + migración 1→2, `RoomPantryRepository` e `InMemoryPantryRepository`, wiring Koin; unit del doble en memoria.
3. Pantalla «Despensa» (`PantryViewModel`/`PantryScreen`/`PantryRoute`): lista, alta/edición en diálogo, borrado con confirmación, estado vacío, cadenas es/en y testTags.
4. Navegación: ruta `pantry` y acción `action_open_pantry` en la barra superior de Mis Recetas.
5. Tests E2E, harness completo, evidencia y cierre en `PROGRESS.md`/`feature_list.json` (`passing`).

La revisión 2026-10-20 se ejecuta como delta sobre lo ya entregado: los pasos 1-5 anteriores ya están hechos; el trabajo pendiente son las tareas R1-R6 (cantidad por stepper entero), en orden dominio → datos/repositorios → UI → recursos → tests → cierre.

## Implementation Tasks

- [x] T1: `PantryItem`, `PantryRepository`, `PantryItemValidation` y normalización con unit (nombre vacío, cantidad, duplicados). (S4-S7)
- [x] T2: `PantryItemEntity` + `PantryItemDao` + `RecipeDatabase` v2 con `MIGRATION_1_2` sin destruir datos. (S9)
- [x] T3: `RoomPantryRepository` + `InMemoryPantryRepository` + Koin, con unit del doble (alta, edición, duplicado, borrado, id inexistente). (S1-S7)
- [x] T4: `PantryViewModel`/`PantryScreen`/`PantryRoute`: lista, diálogos de alta/edición y borrado, estado vacío, errores inline, cadenas es/en y testTags. (S1-S8, S10)
- [x] T5: Ruta `pantry` en `AppNavHost` y acción `action_open_pantry` en la barra de Mis Recetas (ambos estados). (S1, S8)
- [x] T6: `PantryManagementE2ETest` con los escenarios de «Enfoque técnico» (S1-S10) sin regresiones. (S1-S10)
- [x] T7: `./init.ps1`, `./lint.ps1`, `./unit.ps1`, `./e2e.ps1` en verde y evidencia capturada; feature en `passing`.

### Delta de la revisión 2026-10-20 (stepper de cantidad)

- [x] R1: dominio — `PantryItem.quantity: Int?` (`null` = sin cantidad), `PantryItemValidation` con `MIN_QUANTITY`/`MAX_QUANTITY` (0/99) e `isQuantityValid(Int)`; eliminar `parseQuantity`/`isQuantityValid(String)`; unit actualizadas. (S4, S7)
- [x] R2: datos/repositorios — `require` de rango entero en `RoomPantryRepository` e `InMemoryPantryRepository`; mapeo `Int?` ↔ `Double?` en Room con redondeo al leer; **sin** migración nueva (esquema v2 intacto). (S7, S9)
- [x] R3: UI — `PantryViewModel.saveItem(id, name, quantity: Int)` sin `InvalidQuantity`; stepper «− valor +» en el diálogo de alta/edición (inicial 0; «−» deshabilitado en 0, «+» en 99; 0 se guarda como `null`); tarjeta sin línea de cantidad cuando no hay cantidad; testTags y `contentDescription` nuevos; quitar el campo de texto y sus restos. (S4, S7, S9)
- [x] R4: recursos — quitar `pantry_quantity_error`; añadir `pantry_quantity_decrease`/`pantry_quantity_increase` en `values/` y `values-en/`. (S10)
- [x] R5: tests — unit de validación/repositorio en entero; `PantryManagementE2ETest` S4 con toques del stepper y S7 de límites (0 y 99, sin campo de texto, 0 = sin cantidad); `PantryLocalizationE2ETest` con el stepper es/en y ≥ 48 dp. (S4, S7, S10)
- [x] R6: harness completo en verde, evidencia renovada, `docs/domain-model.md` y `docs/risks-and-open-questions.md` actualizados; la feature queda en `passing` y el cierre a `accepted` lo hace `feature-validator`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; **no arranca procesos duraderos** y no se modifica). Artefactos conocidos: carrera del `pidof` en `init.sh` (pasa al reintentar) y exit 1 falso de `init.ps1` con PowerShell 5.1 (ejecutar con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; unit incluye los tests de validación/repositorio en memoria actualizados a cantidad entera (0 = sin cantidad, límites 0/99) y conserva el resto.
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en emulador API 30+ con `PantryManagementE2ETest`:
  - verify 1 (S1): añadir «Tomate fresco» → aparece en la Despensa.
  - verify 2 (S2): eliminar un Alimento → desaparece del listado.
  - S3 cancelar borrado («Cancelar» y descarte con atrás); S4 editar nombre y cantidad con el stepper (tres toques en «+» → «Cantidad: 3»); S5 duplicado («  TOMATE  fresco ») con aviso y sin fila nueva; S6 nombre vacío → guardado deshabilitado; S7 límites del stepper: «−» deshabilitado en 0, «+» deshabilitado en 99, sin campo de texto para la cantidad y guardar con 0 = sin cantidad en la tarjeta; S8 estado vacío con CTA; S9 persistencia tras recrear la actividad (`ActivityScenario.recreate()` o relanzamiento) sobre la DB real; S10 textos con locale `en-US` y área táctil ≥ 48 dp (incluidos los botones del stepper).
  - Sin restos del campo de texto (revisión): `grep` en `app/src` sin `pantry_quantity_error`, `parseQuantity`, `KeyboardType` en `ui/pantry` ni `performTextInput` sobre la cantidad en los E2E.
  - Regresión completa en verde sin romper `AppLaunchE2ETest`, `CrearRecetaE2ETest`, `EditarRecetaE2ETest`, `EliminarRecetaE2ETest`, `BuscarYFiltrarRecetasE2ETest` ni `MisRecetasLocalizationE2ETest`.
- Migración 1→2 (manual, validador): instalar el build actual (DB v1 con una Receta), crear una Receta, instalar encima el build nuevo (`adb install -r`) y comprobar que la Receta sigue en Mis Recetas y la tabla `pantry_items` funciona; **prohibido** `fallbackToDestructiveMigration`. (Los E2E validan el esquema v2 sobre instalación limpia; `e2e.ps1` desinstala la app al terminar.)
- UI (solo el validador): hasta 2 capturas en emulador API 30+ (lista con Alimentos y estado vacío o diálogo) + comprobación de tokens (`DESIGN.md`) y `uiautomator` (áreas táctiles ≥ 48 dp, nombres accesibles «Despensa», «Editar …», «Eliminar …»); sin prototipo que comparar, la referencia son los tokens (laguna `RK04`).
- Cobertura E2E: sí se añade, porque la feature introduce pantalla, flujos CRUD y persistencia nuevos; `unit` no puede cubrir Room (sin Robolectric), por eso S9 va en E2E sobre la DB real.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/gestionar-alimentos-de-la-despensa.md` — informe con `lint`/`unit`/`e2e` (nombres de test), recorrido de S1-S10 y comprobación de la migración 1→2; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-con-alimentos.png` — lista (es) con «Tomate fresco» y otro Alimento mostrando «Cantidad: 3» entera (renovar tras la revisión).
- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-dialogo-stepper.png` — diálogo de edición (es) con el stepper «− 3 +» y el botón «−» habilitado (nueva captura de la revisión).
- La captura de estado vacío existente sigue siendo válida (parte no afectada por la revisión).
- Presupuesto ≤ 4 artefactos y solo de esta feature (1 informe + 2 capturas + opcionalmente el log de `e2e` referenciado).

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature (sin Escáner, sin Coincidencia, sin ubicaciones físicas, sin decimales ni texto en la cantidad).
- [ ] Los escenarios de aceptación pasan (S1-S10, con S7 = límites del stepper) y los dos `verify` de `feature_list.json` se ejecutan en el emulador.
- [ ] No queda rastro del campo de texto de cantidad (`pantry_quantity_error`, `parseQuantity`, `KeyboardType` en `ui/pantry`, `performTextInput` sobre la cantidad).
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature (capturas renovadas con la cantidad entera y el stepper).
- [ ] Sin migración Room nueva: esquema v2 intacto y `MIGRATION_1_2` sin `fallbackToDestructiveMigration`; los Alimentos decimales heredados se muestran redondeados al entero.
- [ ] Cobertura E2E actualizada y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature solo vuelve a `accepted` tras esta validación.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features.

---

**Enmienda `redisenar-vista-de-la-despensa` (2026-11-11, patrón completo)**: la tarjeta de Alimento de la Despensa adopta el rediseño de esa feature (RD28): miniatura con icono, píldora de frescura, cantidad, línea de caducidad, stepper −/+ inline que guarda al instante (0..99) y menú ⋮ con «Editar»/«Eliminar» **en lugar de los iconos de editar/eliminar por fila**; el Alimento gana **fecha de caducidad opcional** (Room v8, `MIGRATION_7_8`) y el diálogo de alta/edición conserva el nombre y la cantidad 0..99 y añade la fila de fecha con DatePicker. El stepper del diálogo, la validación de nombre/cantidad, el rechazo de duplicados (Q-gestionar-alimentos-de-la-despensa-3) y la confirmación de borrado quedan intactos; la cantidad sigue sin unidad (RD13).
