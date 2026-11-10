# Feature Implementation Spec: Ver detalle de receta

## Source Feature

- `id`: ver-detalle-de-receta
- `area`: funcional
- `depends_on`: crear-receta (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

El usuario abre la ficha completa de una Receta desde Mis Recetas y decide si cocinarla: hero con foto, badge de Coincidencia reutilizado (RD01/RD02/RD10) y Momento culinario; métricas de tiempo total, dificultad, calorías por porción y Raciones base; checklist de ingredientes con obligatorios y opcionales; pasos resumidos con su temporizador opcional; y una acción destacada que arranca el Modo Cocina (hoy, su destino provisional hasta `cocinar-paso-a-paso`). Es la pantalla «Detalle» de las ocho previstas (`docs/technical-discovery.md`) y la bisagra entre el catálogo y cocinar.

## Non-Goals

- **Escalado de Raciones (RD03) y selector `[-] N pers [+]`**: es `escalar-raciones`; aquí solo métrica estática de Raciones base y el hueco documentado en el layout (ver D2).
- **Pantalla real de Modo Cocina** (paso a paso, avance, temporizador, voz): es `cocinar-paso-a-paso`; aquí solo la CTA y su destino provisional (ver D4).
- **Añadir faltantes a la Lista de la Compra** (botón «Añadir a la lista», banner «¡Tienes el X% de ingredientes en casa!», ítems con Origen RD09): es `anadir-faltantes-a-la-lista`; no se toca la Lista de la Compra.
- **Disponibilidad por ingrediente en la Despensa** (checks verdes «En despensa» del prototipo): fuera de esta feature; el checklist marca preparación manual del usuario (D3), no la Despensa.
- **Utensilios**: **omitidos en el MVP** por decisión del usuario (2026-10-20, `Q-ver-detalle-de-receta-1`): el prototipo y `docs/build-brief.md` los incluyen, pero no existen en el modelo ni en el formulario aceptado; la sección no se implementa (si se quiere, será una feature futura que amplíe modelo + formulario).
- Valoraciones y opiniones («4.9 (38 opiniones)»), badge «IA», compartir/duplicar/imprimir, menú de cabecera y toggle de favorito (el favorito ya se gestiona en el formulario); precios («Coste aprox.») por `P12`/`⚠ S02`.
- Títulos de Paso (el modelo solo tiene `text`); editar/eliminar desde el Detalle (ya viven en la tarjeta de Mis Recetas); cambios en el formulario, la Despensa o el Escáner.

## Job Story

Cuando tengo varias Recetas guardadas y quiero decidir qué cocinar hoy,
quiero ver la ficha completa de una Receta (tiempo, dificultad, calorías, ingredientes y pasos) con su Coincidencia con la Despensa,
para prepararla con todo a la vista o arrancar el Modo Cocina.

## Users And Permissions

- **Usuario** (único rol del MVP, `P31`): abre el Detalle de sus propias Recetas (Borradores y Publicadas). Sin permisos adicionales.

## Requisitos funcionales

Prefijo propio `RF-DR#` (los `RF-#` de `crear-receta.md` y `RF-MB#` de `mostrar-badge-de-coincidencia.md` se citan sin renumerar).

- **RF-DR1**: al tocar el cuerpo de una tarjeta de Mis Recetas se abre el Detalle de esa Receta; «Editar» y «Eliminar» conservan su acción actual. «Volver» regresa a Mis Recetas con búsqueda y filtro conservados (Q-buscar-y-filtrar-recetas-5).
- **RF-DR2** (RD01/RD02/RD10): el hero muestra el badge de Coincidencia reutilizado de `mostrar-badge-de-coincidencia` (mismo componente, ver D5), recalculado de forma reactiva ante cambios de la Despensa; Receta sin obligatorios → sin badge (RF-MB6).
- **RF-DR3**: el hero muestra la foto (`photoPath`) con el badge de Momento culinario superpuesto y el badge de Coincidencia (ubicación dual como en la tarjeta, D6 de `mostrar-badge-de-coincidencia`); debajo, el título y la historia si no está vacía.
- **RF-DR4**: bloque de métricas 2×2: **Tiempo total** (`prepMinutes + cookMinutes`, con desglose «Nm preparación · Nm cocción»), **Dificultad** (Fácil/Media/Difícil), **Calorías por porción** (`caloriesPerServing`; `«—»` si es `null`) y **Raciones base** (`baseServings`). Sin precios en ninguna celda (`P12`, `⚠ S02`).
- **RF-DR5**: checklist de ingredientes: primero los **obligatorios** y luego los **opcionales** con píldora «Opcional»; cada fila tiene checkbox de marcado local (D3) y «cantidad unidad nombre»; cabecera con contador «N de M» marcados.
- **RF-DR6**: pasos resumidos: tarjeta numerada por Paso (orden de la lista) con su texto completo y chip «N min» cuando `timerMinutes != null`; cabecera con el número de pasos.
- **RF-DR7**: CTA destacada fija abajo «Empezar Modo Cocina» (botón primario, altura mínima 52 dp, `DESIGN.md`) que navega al destino provisional de Modo Cocina con vuelta atrás (patrón Q-arrancar-app-en-mis-recetas-3); `cocinar-paso-a-paso` la sustituirá (D4).
- **RF-DR8**: si el id no existe (Receta borrada o id inválido), la pantalla muestra el estado «Receta no encontrada» con acción de vuelta en lugar de una ficha vacía.

## Acceptance Scenarios

### Scenario 1: Ficha completa (verify 1 de `feature_list.json`)

Given una Receta Publicada con foto, 3 ingredientes (2 obligatorios, 1 opcional) y 2 Pasos (uno con temporizador)
When el usuario la abre desde Mis Recetas
Then se ven la foto y el título, las métricas (tiempo total con desglose, dificultad, calorías, raciones), el checklist con obligatorios y el opcional marcado, y los 2 pasos con su chip de temporizador (RF-DR3-DR6)

### Scenario 2: Apertura y vuelta (RF-DR1)

Given Mis Recetas con búsqueda o filtro activos
When el usuario toca el cuerpo de una tarjeta y luego pulsa «Volver»
Then se abre el Detalle de esa Receta y, al volver, la lista conserva la búsqueda y el filtro

### Scenario 3: Badge de Coincidencia reactivo (RD01/RD02/RD10)

Given una Receta con 2 obligatorios y solo uno en la Despensa
When el usuario abre el Detalle
Then el hero muestra el badge «50%» y, si se añade el otro Alimento, pasa a «100% Match» sin salir de la pantalla (RF-DR2)

### Scenario 4: Borrador incompleto

Given un Borrador sin foto, sin historia, sin calorías y sin ingredientes
When el usuario abre el Detalle
Then la foto es un placeholder, la historia no ocupa espacio, la celda de calorías muestra «—» y el checklist muestra su estado vacío «Sin ingredientes» (RF-DR3-DR5)

### Scenario 5: CTA hacia Modo Cocina (RF-DR7)

Given cualquier Receta abierta en el Detalle
When el usuario pulsa «Empezar Modo Cocina»
Then se abre el destino provisional de Modo Cocina (título y «Volver») hasta que exista `cocinar-paso-a-paso`

### Scenario 6: Receta inexistente (RF-DR8)

Given un id de Receta que no existe
When se abre el Detalle con ese id
Then se muestra «Receta no encontrada» con acción de vuelta

### Scenario 7: Checklist de marcado local (RF-DR5)

Given el Detalle de una Receta con ingredientes
When el usuario marca y desmarca filas del checklist
Then el contador «N de M» se actualiza; al salir y volver a entrar, todas las filas están sin marcar (D3)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Receta sin ingredientes obligatorios (todos opcionales) | Sin badge de Coincidencia (RF-MB6); el checklist lista los opcionales |
| Borrador sin Pasos | Sección de pasos con estado vacío «Sin pasos» |
| Paso sin temporizador | Sin chip de duración |
| `caloriesPerServing = null` | Celda de calorías con «—» (RF-DR4) |
| Historia vacía | No se muestra el párrafo (RF-DR3) |
| Receta sin foto | Placeholder con el mismo tono de superficie que la tarjeta sin foto; badges junto al título |
| Cantidades decimales (0,5 kg) | Formato del idioma activo, sin ceros sobrantes (D7) |
| Despensa vacía | Badge «0%» en el hero; nada más cambia |
| Tap en «Editar»/«Eliminar» de la tarjeta | No abre el Detalle: acciones intactas (RF-DR1) |

## Repository Research

### Files Inspected

- `feature_list.json` — `steps`/`verification` de `ver-detalle-de-receta` y de `escalar-raciones`, `cocinar-paso-a-paso`, `anadir-faltantes-a-la-lista` (sus límites).
- `docs/build-brief.md` — línea del Detalle de Receta (métricas, escalador, checklist, utensilios, pasos, CTA) y exclusiones (`P12`: sin precios).
- `docs/domain-model.md` — atributos de Receta/IngredienteReceta/Paso; RD01/RD02/RD03/RD10; tiempo total = preparación + cocción.
- `CONTEXT.md` — glosario (Receta, Modo Cocina, Raciones, Coincidencia, Momento culinario).
- `docs/technical-discovery.md` — 8 pantallas (Detalle incluida), capas `ui`/`domain`/`data`, comandos Gradle.
- `docs/risks-and-open-questions.md` — `⚠ S02` (sin precios), `⚠ S07`, `RK04`, registro `Q-…`.
- `DESIGN.md` y `uxdocs/chefencasa_detalle_de_receta/code.html` (solo lectura) — hero, métricas 2×2, escalador, checklist con contador, «Utensilios necesarios», pasos con chips de tiempo, CTA fija «Empezar Modo Cocina».
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes` (MY_RECIPES, CREATE_RECIPE, SCAN_PANTRY, PANTRY); **no existe ruta de Detalle**.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — destino provisional con título y vuelta (`ProvisionalTestTags.FORM_SCREEN`/`SCANNER_SCREEN`).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `RecipeCard` (sin acción de apertura: solo `onEdit`/`onDelete`), `MatchBadge` **privado**, `rememberBitmap`, `MyRecipesTestTags`, píldoras `PillShape`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesViewModel.kt`, `MyRecipesRoute.kt` — `MyRecipesUiState` con `matches: Map<String, RecipeMatch>`; no hay callback de apertura.
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt` — `prepMinutes`, `cookMinutes`, `difficulty`, `baseServings`, `caloriesPerServing: Int?`, `photoPath`, `story`, `ingredients`, `steps`. **Sin utensilios** (U1, resuelta: omisión en el MVP).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeIngredient.kt` (`optional`), `RecipeStep.kt` (`text`, `timerMinutes`), `Difficulty.kt`, `MealMoment.kt`.
- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` — `calculateMatch(recipe, pantryItems)` reutilizable (RD01/RD10).
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — `getRecipe(id): Recipe?` ya existe; `PantryRepository.observePantryItems()`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDao.kt`, `RecipeWithDetails.kt`, `RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — lectura por id sin cambios necesarios.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `viewModel { … }` de Koin.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — convención es/en; claves `my_recipes_*`, `difficulty_*`, `meal_moment_*`.
- `app/src/androidTest/java/com/whatsforlunch/app/MatchBadgeE2ETest.kt` (repos in-memory + contexto es), `MyRecipesLocalizationE2ETest.kt` (`WhatsForLunchApp` + navegación a provisionales), `E2eDatabase.kt`.
- Scripts `init`/`lint`/`unit`/`e2e` (.sh/.ps1) — envoltorios de `./gradlew` (ver «Verification Plan»).

### Existing Patterns To Follow

- MVVM + StateFlow, reglas en `domain/`, Koin en `di/AppModule.kt`; pantallas sin estado + `*Route` (como `PantryRoute`, `CreateRecipeRoute`).
- Navegación por callbacks desde la pantalla hasta `AppNavHost` (`onEditRecipe`, `onOpenPantry`); destinos provisionales con `ProvisionalScreen` (Q-arrancar-app-en-mis-recetas-3).
- Píldoras `Surface(shape = PillShape)` + `labelSmall` bold; `testTag` en objetos `*TestTags`; textos solo vía `strings.xml` es/en.
- E2E: `createComposeRule` + `InMemoryRecipeRepository`/`InMemoryPantryRepository` sembrados, contexto español forzado y `waitUntil`; navegación completa con `WhatsForLunchApp`.

### Current Gaps

- No existe pantalla, ruta, ViewModel ni `testTag` de Detalle: se crea de cero.
- `RecipeCard` no abre nada al tocarla: hay que añadir la acción de apertura sin romper «Editar»/«Eliminar».
- `MatchBadge` es privado de `MyRecipesScreen.kt`: hay que extraerlo a componente compartido conservando su `testTag` (los tests existentes no deben cambiar).
- **Utensilios inexistentes en modelo y formulario** (U1): resuelta por el usuario (2026-10-20) como **omisión en el MVP** — el step 1 se implementa sin la cláusula «con los utensilios».

## Technical Approach

- **Navegación** — `AppRoutes.RECIPE_DETAIL = "recipe_detail/{recipeId}"` con `fun recipeDetail(recipeId: String)`; `MyRecipesRoute`/`MyRecipesScreen` reciben `onOpenRecipe: (String) -> Unit = {}` (valor por defecto: no rompe call sites de tests) invocado desde el cuerpo de la tarjeta; `AppNavHost` enlaza a `RecipeDetailRoute(onBack = { popBackStack() }, onStartCooking = { navigate al provisional COOK_MODE })`.
- **Pantalla** — nuevo paquete `ui/recipedetail/`: `RecipeDetailViewModel(recipeRepository, pantryRepository)` con `RecipeDetailUiState` (loading / notFound / detail(recipe, match)) usando `repository.getRecipe(id)` y `combine` con `pantryRepository.observePantryItems()` + `calculateMatch` (RF-DR2, RF-DR8); `RecipeDetailRoute` con `koinViewModel()`; `RecipeDetailScreen` con `RecipeDetailTestTags` (`recipe_detail_screen`, `recipe_detail_hero`, `recipe_detail_match`, `recipe_detail_metrics`, `recipe_detail_total_time`, `recipe_detail_difficulty`, `recipe_detail_calories`, `recipe_detail_servings`, `recipe_detail_ingredients`, `recipe_detail_ingredient_row`, `recipe_detail_ingredient_check`, `recipe_detail_ingredients_count`, `recipe_detail_steps`, `recipe_detail_step_row`, `recipe_detail_step_timer`, `recipe_detail_start_cooking`, `recipe_detail_not_found`).
- **Badge compartido (D5)** — extraer `MatchBadge` a `ui/components/MatchBadge.kt` con parámetro `testTag`; la tarjeta sigue usando `MyRecipesTestTags.RECIPE_MATCH` (regresión intacta) y el hero usa `RecipeDetailTestTags.RECIPE_MATCH`.
- **Checklist** — marcado en estado de UI (`rememberSaveable` por id de Ingrediente) sin persistencia (D3); obligatorios primero, luego opcionales (D6); contador «N de M».
- **CTA Modo Cocina (D4)** — botón primario fijo sobre el scroll; navega a `ProvisionalScreen` con `ProvisionalTestTags.COOK_MODE_SCREEN` nueva constante y título de `strings.xml` («Modo Cocina»); siempre habilitada (el gating por pasos lo decide `cocinar-paso-a-paso`).
- **Textos** — `values/strings.xml` + `values-en/strings.xml`: claves `recipe_detail_*` (título, métricas, «Opcional», «N de M», «Sin ingredientes», «Sin pasos», «—» no necesita clave, «Empezar Modo Cocina», «Receta no encontrada», subtítulos de tiempo).
- **Tests** — unit sobre el ViewModel/formato (notFound, match, total = prep+cook, formato de cantidad D7) y `RecipeDetailE2ETest` con repos in-memory (patrón `MatchBadgeE2ETest`).

### Decisiones (para no reabrir el debate en implementación)

- **D1 — apertura por tap en el cuerpo de la tarjeta** (con `testTag` propio); «Editar» y «Eliminar» no cambian. Es la única entrada al Detalle en esta feature.
- **D2 — métricas 2×2**: la celda «Coste aprox.» del prototipo se sustituye por **Raciones base** (`⚠ S02`); el bloque «Raciones a preparar» con `[-] N pers [+]` queda **reservado para `escalar-raciones`** entre métricas y checklist (hoy no se dibuja placeholder: la métrica estática ya informa).
- **D3 — checklist de marcado local**: el checkbox marca «preparado» por el usuario, solo en memoria de la pantalla (se reinicia al salir); sin persistencia ni reglas de negocio (la UI no toca `domain/`).
- **D4 — CTA habilitada hacia destino provisional** (patrón Q-arrancar-app-en-mis-recetas-3), en vez de deshabilitada: la acción existe y es verificable hoy; `cocinar-paso-a-paso` la convierte en real.
- **D5 — badge reutilizado**, no reimplementado: componente único extraído con la misma semántica que `mostrar-badge-de-coincidencia` (RD01/RD02/RD10, RF-MB6).
- **D6 — orden del checklist**: obligatorios en su orden de modelo y luego opcionales en el suyo, cada opcional con píldora «Opcional».
- **D7 — formato de cantidad**: separador decimal del idioma activo, sin ceros sobrantes (0,5 / 0.5).

### Unknowns

- **U1 — utensilios · RESUELTA** (`Q-ver-detalle-de-receta-1`, decidida por el usuario el 2026-10-20): **omisión en el MVP**. La sección «Utensilios necesarios» no se implementa en el Detalle ni en esta feature; el resto del step 1 se implementa íntegro. Si se quieren utensilios, deberá ser una feature futura que amplíe el modelo (`Recipe` + Room + migración) y el formulario de crear/editar, con su propia spec.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — crear; pantalla + `RecipeDetailTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailRoute.kt` — crear; wiring MVVM.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModel.kt` — crear; `RecipeDetailUiState` (RF-DR2, RF-DR8).
- `app/src/main/java/com/whatsforlunch/app/ui/components/MatchBadge.kt` — crear; badge compartido (D5).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — modificar; usa el `MatchBadge` compartido, `onOpenRecipe` en el tap de la tarjeta (D1).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesRoute.kt` — modificar; pasa `onOpenRecipe`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; ruta `recipe_detail/{recipeId}` + destino provisional de Modo Cocina.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — modificar; constante `COOK_MODE_SCREEN`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `viewModel { RecipeDetailViewModel(get(), get()) }`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; claves `recipe_detail_*`.
- `app/src/test/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModelTest.kt` — crear; unit (RF-DR4, RF-DR8, match).
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeDetailE2ETest.kt` — crear; verify 1 y S2-S6.
- `docs/validations/ver-detalle-de-receta.md` y `docs/evidence/ver-detalle-de-receta/*.png` — crear al validar (evidencia).
- `docs/risks-and-open-questions.md` — `Q-ver-detalle-de-receta-1` (U1) ya registrada como resuelta el 2026-10-20 (omisión en el MVP); sin cambios al ejecutar la spec.

Ninguna migración de Room ni cambio de entidades: se leen datos que ya existen (U1 resuelta: sin utensilios).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens YAML de `uxdocs/DESIGN.md` (`P17`) e `uxdocs/chefencasa_detalle_de_receta/code.html` como inspiración (`P16`).
- Screens or states affected: nueva pantalla Detalle; Mis Recetas gana la apertura por tap; nuevo destino provisional Modo Cocina.
- New design artifact required: no — el prototipo cubre la vista; las lagunas se resuelven aquí (D2, D4); U1 (utensilios) quedó resuelta por omisión en el MVP.

Contrato visual (verificable por `uiautomator` y captura):

- Hero: foto ~272 dp de alto, radio `rounded.xl` (1.5 rem) y sombra cálida; badge de Momento culinario superpuesto arriba-izquierda y badge de Coincidencia arriba-derecha (patrón de la tarjeta); sin foto, placeholder de superficie y badges junto al título. Debajo: título (`headlineLarge`) e historia (`bodyMedium`, `onSurfaceVariant`).
- Métricas 2×2 de tarjetas `surfaceContainerLowest` con icono (timer / equalizer / local_fire_department / group), etiqueta en versalitas y valor (`headlineSmall`); sublabel del tiempo «Nm preparación · Nm cocción»; sin celda de precios (`P12`, `⚠ S02`).
- Checklist: filas `surfaceContainerLowest` con checkbox (5×5, `accentSecondary`), texto «cantidad unidad nombre» y píldora «Opcional» (gris, `labelSmall`) en los opcionales; contador «N de M» en `secondaryContainer` como en el prototipo.
- Pasos: tarjetas con círculo numerado `primary` (24 dp), texto del Paso (`bodyMedium`) y chip de temporizador `tertiary` («N min») si existe.
- CTA fija abajo sobre `surface/90`: botón primario terracota `#a83212`, texto blanco, altura mínima 52 dp, icono de sartén, etiqueta «Empezar Modo Cocina»; área táctil ≥ 48 dp.
- Estados cubiertos (laguna `RK04`): vacío de ingredientes/pasos, not-found (RF-DR8) y placeholder sin foto. Accesibilidad: contraste AA, targets ≥ 48 dp.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y no cambian capas, dominio ni dependencias (pantalla nueva dentro de `ui/`, lectura por `RecipeRepository` existente).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/risks-and-open-questions.md` — done (2026-10-20): `Q-ver-detalle-de-receta-1` registrada como resuelta (omisión de utensilios en el MVP). `docs/domain-model.md` — not needed (no hay reglas nuevas; D2-D7 son decisiones de esta spec; sin ampliación de modelo).

## Implementation Plan

1. Extraer `MatchBadge` a `ui/components/` (D5) sin tocar su comportamiento y añadir la apertura por tap en la tarjeta (RF-DR1) con su ruta en `AppNavHost`.
2. Crear `RecipeDetailViewModel` + `RecipeDetailRoute` + `RecipeDetailScreen` (hero, métricas, checklist, pasos, estados vacíos/not-found) con textos es/en y `RecipeDetailTestTags`.
3. Añadir la CTA «Empezar Modo Cocina» hacia el destino provisional (RF-DR7, D4).
4. Unit tests del ViewModel/formato y `RecipeDetailE2ETest` (verify 1 + S2-S6) sin romper la suite existente.
5. Ejecutar el harness completo, capturar evidencia y dejar la feature en `passing` para `feature-validator`.

## Implementation Tasks

- [ ] T1: `MatchBadge` compartido + `onOpenRecipe` en la tarjeta y ruta `recipe_detail/{recipeId}` (RF-DR1, D5). (S2)
- [ ] T2: `RecipeDetailViewModel` (`getRecipe` + `calculateMatch` reactivo, notFound) + unit tests. (RF-DR2, RF-DR4, RF-DR8)
- [ ] T3: `RecipeDetailScreen` (hero, métricas 2×2, checklist con contador y opcionales, pasos con chip de temporizador, estados vacíos) con `RecipeDetailTestTags` y textos es/en. (RF-DR3-DR6, S1, S4, S7)
- [ ] T4: CTA «Empezar Modo Cocina» + `ProvisionalTestTags.COOK_MODE_SCREEN` (RF-DR7, D4). (S5)
- [ ] T5: `RecipeDetailE2ETest` (verify 1; S2 ida/vuelta con búsqueda conservada; S3 badge reactivo; S5 CTA; S6 not-found) y regresión completa en verde.
- [ ] T6: `init`/`lint`/`unit`/`e2e` en verde y evidencia capturada (el muestreo visual lo ejecuta el validador).

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; no arranca procesos duraderos). Notas de entorno (`PROGRESS.md`): ejecutar en PowerShell 5.1 con `cmd /c "set PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%LOCALAPPDATA%\Android\Sdk\emulator;%PATH%&& set PKG=com.whatsforlunch.app&& powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"` (falso fallo por stderr de `adb shell monkey`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `RecipeDetailViewModelTest` y los suites existentes (33 tests hoy). Para forzar: `gradlew.bat testDebugUnitTest --rerun-tasks` (`unit.ps1` no reenvía argumentos).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, emulador `Pixel_10_Pro` API 37) con `RecipeDetailE2ETest`:
  - verify 1 (S1): Receta con foto, 2 obligatorios + 1 opcional y 2 Pasos (uno con temporizador) → métricas (tiempo total con desglose, dificultad, calorías, raciones), checklist con «Opcional» y pasos con chip «N min».
  - S2: tap en la tarjeta → Detalle → «Volver» conservando búsqueda/filtro; S3: badge «50%» → «100% Match» al cambiar la Despensa; S4: borrador incompleto («—», «Sin ingredientes»); S5: CTA → `ProvisionalTestTags.COOK_MODE_SCREEN` con vuelta; S6: id inexistente → «Receta no encontrada».
  - Regresión completa en verde (suite hoy 70 tests) sin romper `crear-receta`, `editar-receta`, `eliminar-receta`, `buscar-y-filtrar-recetas`, `mostrar-badge-de-coincidencia` ni `gestionar-alimentos-de-la-despensa`.
- Notas de entorno: `e2e.ps1` desinstala la app al terminar (reinstalar con `./gradlew installDebug` para comprobación manual); suites largas pueden abortar (`INSTRUMENTATION_ABORTED`) → reintentar tras reiniciar el AVD; capturas en español con `cmd locale set-app-locales com.whatsforlunch.app` (sin `--locales`); fotos de fixture como en `MatchBadgeE2ETest.writePhoto()` (el picker de MediaStore falla: incidencia conocida de `PROGRESS.md`).
- UI (solo el validador): capturas en es + `uiautomator` (posición, textos, `testTag`); sin pipeline de screenshot testing en el repo (justificado por `afinar-fidelidad-visual-del-formulario`).
- Cobertura E2E: sí se añade, porque la feature añade pantalla y flujo de navegación visibles.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/ver-detalle-de-receta.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación S1-S7 con la omisión de utensilios (U1) declarada; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/ver-detalle-de-receta/detalle-receta-completa.png` — ficha completa en es (hero con badge, métricas, checklist, pasos).
- `docs/evidence/ver-detalle-de-receta/detalle-receta-borrador.png` — borrador incompleto (placeholder sin foto, «—» en calorías, «Sin ingredientes»).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (hero + métricas; utensilios omitidos por U1) | RF-DR2, RF-DR3, RF-DR4 | S1, S3, S4 | T2, T3 | E2E + captura |
| Step 2 (checklist + pasos resumidos) | RF-DR5, RF-DR6 | S1, S4, S7 | T3 | E2E + unit |
| Step 3 (acción destacada Modo Cocina) | RF-DR7 | S5 | T4 | `RecipeDetailE2ETest` |
| Verify 1 (E2E: métricas, ingredientes, pasos) | RF-DR3-DR6 | S1 | T5 | `RecipeDetailE2ETest` |
| — (apertura y vuelta) | RF-DR1, RF-DR8 | S2, S6 | T1, T2 | E2E + unit |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (sin escalado RD03, sin Modo Cocina real, sin Lista de la Compra, sin utensilios: U1 resuelta como omisión en el MVP).
- [ ] Los escenarios de aceptación pasan (S1-S7) y las decisiones D1-D7 se respetan.
- [ ] U1 (utensilios) está resuelta como omisión (usuario, 2026-10-20): la sección «Utensilios necesarios» NO aparece en el Detalle y el informe de validación lo declara.
- [ ] El badge de Coincidencia es el componente reutilizado (RD01/RD02/RD10), no una reimplementación; la regresión de `mostrar-badge-de-coincidencia` pasa.
- [ ] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente añadida y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular: sin selector de raciones, sin «Añadir a la lista», sin pantalla de Modo Cocina definitiva).

---

## Enmienda — `unificar-unidades` (2026-11-09)

Decisión posterior del usuario (D1–D5 de `docs/specs/unificar-unidades.md`): la unidad deja de ser texto libre y pasa a **lista cerrada (ud, g, kg, ml, l)**. El checklist del Detalle compone la unidad con `unitLabel` (código del enum → clave `unit_ud`…`unit_l` en es/en; unidad legada → texto tal cual; vacía → se omite, RD19). Sin conversiones ni migración de unidades y sin cambios en el escalado RD03 ni en el resto del Detalle.

*Enmienda añadida al final por `unificar-unidades`, sin reescribir la historia ni las evidencias históricas de esta spec.*

---

## Enmienda — `redisenar-formulario-de-receta` (2026-11-09)

Decisiones posteriores del usuario (D3/D5 de `docs/specs/redisenar-formulario-de-receta.md`): el **desglose «N min preparación · N min cocción» desaparece** (RF-DR4) —la métrica de Tiempo muestra solo la preparación con el formato «N min» actual— y el checklist pierde la **píldora «Opcional»** (RF-DR5) y el orden obligatorios-primero: los Ingredientes salen en su orden de modelo, todos iguales. El resto de esta spec (hero, métricas, checklist con contador, pasos resumidos y CTA de Modo Cocina) queda intacto.

*Enmienda añadida al final por `redisenar-formulario-de-receta`, sin reescribir la historia ni las evidencias históricas de esta spec.*
