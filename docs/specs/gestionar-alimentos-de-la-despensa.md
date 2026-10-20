# Feature Implementation Spec: Gestionar alimentos de la Despensa

## Source Feature

- `id`: gestionar-alimentos-de-la-despensa
- `area`: funcional
- `depends_on`: arrancar-app-en-mis-recetas (`accepted`)
- `status`: `in_progress`
- `source`: `feature_list.json`

## Goal

El usuario mantiene el listado de la **Despensa** («todos los alimentos del hogar en un único listado», `CONTEXT.md`): una pantalla «Despensa» nueva con alta, edición y baja de **Alimentos**, persistida en Room para que sobreviva a los reinicios de la app. Sin desglose por ubicaciones físicas (RD11). Es la base de datos sobre la que después calcularán la Coincidencia (`mostrar-badge-de-coincidencia`) y que llenará el Escáner (`escanear-despensa-con-camara`); esta feature no toca nada de eso.

Decisiones de UX confirmadas por el usuario el 2026-10-20 (`docs/risks-and-open-questions.md`, Q-gestionar-alimentos-de-la-despensa-1..4): pantalla «Despensa» nueva con ruta propia y acceso desde una acción en la barra superior de Mis Recetas; borrado con diálogo de confirmación con el nombre del Alimento (mismo patrón que `eliminar-receta`); duplicado por nombre normalizado → aviso y no se crea fila nueva; cantidad = número decimal opcional sin unidad (≥ 0).

## Non-Goals

- Escaneo con cámara, detección MediaPipe y chips de detección: `escanear-despensa-con-camara`.
- Cálculo ni badge de Coincidencia (RD01/RD02/RD10): `mostrar-badge-de-coincidencia`.
- Desglose por ubicaciones físicas (nevera/vinagrera/especiero): prohibido por RD11 (`P37`).
- Unidades de medida, caducidades, precios o stock mínimo: el Alimento solo tiene nombre y cantidad opcional.
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
When el usuario lo edita a nombre «Tomate maduro» y cantidad 3
Then el listado muestra «Tomate maduro» con su cantidad, sin crear un segundo Alimento

### Scenario 5: Duplicado por nombre normalizado (Q-…-3)

Given un Alimento «Tomate fresco» en la Despensa
When el usuario añade o renombra otro a «  TOMATE  fresco » (mayúsculas, espacios redundantes y tildes; minúsculas, colapso de espacios, sin tildes y trim)
Then no se crea un segundo Alimento y el formulario muestra el aviso «Este alimento ya está en la Despensa»

### Scenario 6: Nombre obligatorio

Given el formulario de alta/edición abierto
When el campo nombre está vacío o solo tiene espacios
Then la acción de guardar está deshabilitada (o, tras intentarlo, no se guarda nada)

### Scenario 7: Cantidad opcional y válida (Q-…-4)

Given el formulario de alta/edición
When el usuario deja la cantidad vacía se guarda sin cantidad; cuando escribe un número decimal ≥ 0 se guarda como cantidad; cuando escribe un valor no numérico o negativo
Then el formulario muestra un error de cantidad y no guarda hasta corregirlo

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

## Technical Approach

1. **Dominio** (`domain/`): modelo `PantryItem` (= Alimento; `id: String`, `name: String`, `quantity: Double?`) en `domain/model/`. Contrato `PantryRepository` en `domain/repository/`: `observePantryItems(): Flow<List<PantryItem>>`, `savePantryItem(item): PantrySaveResult` (`Saved` | `DuplicateName`; cubre alta y edición), `deletePantryItem(id)`. Validación pura en `domain/pantry/PantryItemValidation`: nombre no vacío tras `trim`, cantidad `null` o `>= 0`. Normalización compartida con búsqueda: trim, `lowercase(Locale.ROOT)`, NFD sin marcas y espacios consecutivos reducidos a uno (necesario para que «TOMATE  fresco» coincida con «Tomate fresco»); el helper se reutiliza desde `RecipeSearchFilter` y sus 19 unit deben seguir en verde.
2. **Datos (Room)**: `PantryItemEntity` (tabla `pantry_items`: `id TEXT PK`, `name TEXT`, `normalizedName TEXT`, `quantity REAL?`) con índice **único** sobre `normalizedName` como salvaguarda de la regla «un Alimento por nombre normalizado». `PantryItemDao`: observar todos (`ORDER BY name COLLATE NOCASE ASC`), `insert` (ABORT), `update`, `deleteById`, `findByNormalizedName`. `RecipeDatabase` pasa a **version 2** con la entidad nueva y `MIGRATION_1_2` (`CREATE TABLE pantry_items (...)` + `CREATE UNIQUE INDEX ...`; sin `fallbackToDestructiveMigration`). `RoomPantryRepository` comprueba duplicados antes de insertar/actualizar (excluyendo el propio id en edición) y traduce el conflicto del índice único a `DuplicateName`.
3. **Doble en memoria**: `InMemoryPantryRepository` con `MutableStateFlow` para unit (mismas reglas de duplicado y no-op al borrar id inexistente).
4. **ViewModel/UI**: `ui/pantry/` con `PantryRoute` (Koin `koinViewModel()`), `PantryViewModel` (expone `uiState` con los Alimentos + flags de error de guardado, y acciones `add`, `update`, `delete`) y `PantryScreen`. La pantalla: `Scaffold` con `TopAppBar` título «Despensa» y acción «Volver» (patrón `ProvisionalScreen`), lista de tarjetas (nombre + cantidad) con acciones «Editar» y «Eliminar» por fila, `ExtendedFloatingActionButton` «Añadir alimento» y estado vacío con CTA. Alta/edición en un `AlertDialog` con dos campos rellenos (nombre, cantidad) y «Cancelar»/«Añadir» (alta) o «Guardar cambios» (edición); duplicado → error inline bajo el nombre y el diálogo sigue abierto; guardado deshabilitado con nombre vacío. Baja en el `AlertDialog` de confirmación de `eliminar-receta` (nombre del Alimento, «Cancelar»/«Eliminar» tinte `error`, descarte = cancelar, borrado definitivo sin papelera).
5. **Navegación**: `AppRoutes.PANTRY = "pantry"` + `composable` con `PantryRoute(onBack = popBackStack)`; entrada nueva en `MisRecetasScreen`: `IconButton` en `actions` del `TopAppBar` (visible tanto en estado vacío como con Recetas) con icono `Icons.Outlined.Inventory2`, `contentDescription` «Despensa»/«Pantry» y `testTag(action_open_pantry)`; `MisRecetasRoute` y `AppNavHost` cablean `onOpenPantry`.
6. **Recursos**: claves `pantry_*` en `values/strings.xml` y `values-en/strings.xml` (NFR idioma): título «Despensa»/«Pantry», «Añadir alimento»/«Add food», «Editar alimento»/«Edit food», «Guardar cambios»/«Save changes», «Nombre»/«Name», «Cantidad (opcional)»/«Quantity (optional)», «Eliminar alimento»/«Delete food», mensaje «¿Eliminar «{nombre}»? Esta acción es definitiva y no se puede deshacer.», «Este alimento ya está en la Despensa.»/“This food is already in your Pantry.”, «Cantidad no válida.»/“Invalid quantity.”, estado vacío («Tu Despensa está vacía»/“Your Pantry is empty” + «Añade los alimentos que tienes en casa.»/“Add the foods you have at home.”), más `action_back` reutilizado. Identificadores nuevos de código en inglés; los identificadores españoles existentes no se tocan (son de `revisar-identificadores-en-ingles`).
7. **Tests**: unit (`app/src/test`) sobre `InMemoryPantryRepository` + `PantryItemValidation` + normalización (alta, edición, duplicado, borrado, id inexistente, nombre vacío, cantidad). E2E `PantryManagementE2ETest` (`app/src/androidTest`) sobre la **DB real** con `E2eDatabase.clear()` (la persistencia es parte del alcance), navegando desde Mis Recetas a la Despensa.

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

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); **sin prototipo** para la Despensa (laguna tipo `RK04`): el copy y la composición los propone esta spec y se validan visualmente (Q-arrancar-app-en-mis-recetas-5).
- Screens or states affected: «Despensa» (nueva: lista, vacío, diálogos de alta/edición y de borrado, error de duplicado y de cantidad); Mis Recetas (solo la nueva acción de la barra superior, sin tocar su contenido).
- New design artifact required: no — se resuelve con componentes M3 y los tokens existentes.

Criterios visuales (verificables con captura + `uiautomator`, como en features anteriores):

- Superficies y color por roles de `uxdocs/DESIGN.md`: fondo/superficie `#FBF9F5`, tarjetas `surface-container-lowest` (#FFFFFF) con radio `rounded.lg` (1 rem) y sombra cálida `shadow-sm` ámbar-carbón; texto `on-surface` #1B1C1A y secundario `on-surface-variant` #59413B; acciones primarias en `primary` #A83212 con texto #FFFFFF; el error en `error` #BA1A1A (solo el «Eliminar» del diálogo).
- Campos del formulario con el estilo de formulario ya validado en `afinar-fidelidad-visual-del-formulario`: relleno `surface-container-highest` (#E4E2DE), anillo de foco terracota, estado deshabilitado distinguible.
- Objetivos táctiles ≥ 48 dp en todas las acciones (incluidas las «Editar»/«Eliminar» por fila y la acción de la barra superior de Mis Recetas); botón primario estándar con altura mínima 52 dp (la excepción compacta 36/48 dp del pie de tarjeta de Mis Recetas no se traslada aquí).
- Iconos de `material-icons-extended`: `Icons.Outlined.Inventory2` (entrada en Mis Recetas), `Icons.Outlined.Add`, `Icons.Outlined.Edit`, `Icons.Outlined.Delete`, y un icono de estado vacío (p. ej. `Icons.Outlined.Kitchen`) dentro de un círculo de 56 dp sobre `surface-container` #EFEEEA, al estilo del estado «sin resultados» de Mis Recetas.
- Copy propuesto (es/en) en «Enfoque técnico» punto 6; la revisión visual puede ajustarlo sin cambiar comportamiento.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas `ui`/`domain`/`data` no cambian (solo se añade un agregado PantryItem con el patrón ya usado por Recetas).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD11 ya vive en `docs/domain-model.md`).
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — aplicado (la línea de duplicados de «Validaciones de datos» refleja el aviso en alta/edición manual, Q-…-3); `docs/risks-and-open-questions.md` — aplicado (las filas Q-gestionar-alimentos-de-la-despensa-1..4 viven en el «Apéndice · Registro de preguntas resueltas», al que se añadieron el 2026-10-20 tras la aprobación de la spec).

Las preguntas Q-gestionar-alimentos-de-la-despensa-1..4 se resolvieron con el usuario el 2026-10-20 y su registro completo (pregunta, origen y estado) vive en `docs/risks-and-open-questions.md` («Apéndice · Registro de preguntas resueltas»).

## Implementation Plan

1. Dominio: `PantryItem`, `PantryRepository`/`PantrySaveResult`, `PantryItemValidation` y normalización compartida, con unit.
2. Room: entidad + DAO + migración 1→2, `RoomPantryRepository` e `InMemoryPantryRepository`, wiring Koin; unit del doble en memoria.
3. Pantalla «Despensa» (`PantryViewModel`/`PantryScreen`/`PantryRoute`): lista, alta/edición en diálogo, borrado con confirmación, estado vacío, cadenas es/en y testTags.
4. Navegación: ruta `pantry` y acción `action_open_pantry` en la barra superior de Mis Recetas.
5. Tests E2E, harness completo, evidencia y cierre en `PROGRESS.md`/`feature_list.json` (`passing`).

## Implementation Tasks

- [x] T1: `PantryItem`, `PantryRepository`, `PantryItemValidation` y normalización con unit (nombre vacío, cantidad, duplicados). (S4-S7)
- [x] T2: `PantryItemEntity` + `PantryItemDao` + `RecipeDatabase` v2 con `MIGRATION_1_2` sin destruir datos. (S9)
- [x] T3: `RoomPantryRepository` + `InMemoryPantryRepository` + Koin, con unit del doble (alta, edición, duplicado, borrado, id inexistente). (S1-S7)
- [x] T4: `PantryViewModel`/`PantryScreen`/`PantryRoute`: lista, diálogos de alta/edición y borrado, estado vacío, errores inline, cadenas es/en y testTags. (S1-S8, S10)
- [x] T5: Ruta `pantry` en `AppNavHost` y acción `action_open_pantry` en la barra de Mis Recetas (ambos estados). (S1, S8)
- [x] T6: `PantryManagementE2ETest` con los escenarios de «Enfoque técnico» (S1-S10) sin regresiones. (S1-S10)
- [x] T7: `./init.ps1`, `./lint.ps1`, `./unit.ps1`, `./e2e.ps1` en verde y evidencia capturada; feature en `passing`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; **no arranca procesos duraderos** y no se modifica). Artefactos conocidos: carrera del `pidof` en `init.sh` (pasa al reintentar) y exit 1 falso de `init.ps1` con PowerShell 5.1 (ejecutar con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; unit incluye los tests nuevos de validación/repositorio en memoria y conserva los 19 existentes.
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en emulador API 30+ con `PantryManagementE2ETest`:
  - verify 1 (S1): añadir «Tomate fresco» → aparece en la Despensa.
  - verify 2 (S2): eliminar un Alimento → desaparece del listado.
  - S3 cancelar borrado («Cancelar» y descarte con atrás); S4 editar nombre/cantidad; S5 duplicado («  TOMATE  fresco ») con aviso y sin fila nueva; S6 nombre vacío → guardado deshabilitado; S7 cantidad vacía/negativa; S8 estado vacío con CTA; S9 persistencia tras recrear la actividad (`ActivityScenario.recreate()` o relanzamiento) sobre la DB real; S10 textos con locale `en-US` y área táctil ≥ 48 dp.
  - Regresión completa en verde sin romper `AppLaunchE2ETest`, `CrearRecetaE2ETest`, `EditarRecetaE2ETest`, `EliminarRecetaE2ETest`, `BuscarYFiltrarRecetasE2ETest` ni `MisRecetasLocalizationE2ETest`.
- Migración 1→2 (manual, validador): instalar el build actual (DB v1 con una Receta), crear una Receta, instalar encima el build nuevo (`adb install -r`) y comprobar que la Receta sigue en Mis Recetas y la tabla `pantry_items` funciona; **prohibido** `fallbackToDestructiveMigration`. (Los E2E validan el esquema v2 sobre instalación limpia; `e2e.ps1` desinstala la app al terminar.)
- UI (solo el validador): hasta 2 capturas en emulador API 30+ (lista con Alimentos y estado vacío o diálogo) + comprobación de tokens (`DESIGN.md`) y `uiautomator` (áreas táctiles ≥ 48 dp, nombres accesibles «Despensa», «Editar …», «Eliminar …»); sin prototipo que comparar, la referencia son los tokens (laguna `RK04`).
- Cobertura E2E: sí se añade, porque la feature introduce pantalla, flujos CRUD y persistencia nuevos; `unit` no puede cubrir Room (sin Robolectric), por eso S9 va en E2E sobre la DB real.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/gestionar-alimentos-de-la-despensa.md` — informe con `lint`/`unit`/`e2e` (nombres de test), recorrido de S1-S10 y comprobación de la migración 1→2; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-con-alimentos.png` — lista con «Tomate fresco» y otro Alimento (es).
- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-estado-vacio.png` — estado vacío con su CTA (es).
- Presupuesto ≤ 4 artefactos y solo de esta feature (1 informe + 2 capturas + opcionalmente el log de `e2e` referenciado).

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature (sin Escáner, sin Coincidencia, sin ubicaciones físicas).
- [ ] Los escenarios de aceptación pasan (S1-S10) y los dos `verify` de `feature_list.json` se ejecutan en el emulador.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Migración Room 1→2 comprobada sin pérdida de Recetas (sin migración destructiva).
- [ ] Cobertura E2E persistente añadida y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features.
