# Feature Implementation Spec: Cocinar paso a paso

## Source Feature

- `id`: cocinar-paso-a-paso
- `area`: funcional
- `depends_on`: ver-detalle-de-receta (accepted), vincular-pasos-e-ingredientes (accepted)
- `status`: not_started (a la hora de planificar; la spec no cambia `feature_list.json`)
- `source`: `feature_list.json` (decisiones del usuario registradas en `docs/progress-history.md`, sesión 93, 2026-10-27)

## Goal

Sustituir el destino provisional de Modo Cocina por la **pantalla real «Modo Cocina»**: guía de la Receta **paso a paso**, con alto contraste y tipografía grande (AAA, cuerpo ≥ 18 px, legible a 1,5 m), barra de avance **«Paso X de Y»**, y el **drawer «Ingredientes para este paso»** que lista los ingredientes **vinculados del paso activo** (RD21) con las cantidades **escaladas a las Raciones heredadas del Detalle** (RD03). El usuario navega con «Paso Anterior»/«Siguiente» y, en el último paso, **«Terminar»** le devuelve al Detalle.

La CTA del Detalle «Empezar Modo Cocina» pasa a exigir **≥ 1 Paso** (deshabilitada con 0 pasos), cerrando el hueco que `ver-detalle-de-receta` dejó a esta feature (D4 de esa spec).

## Non-Goals

- **Temporizador** (cuenta atrás, alarma, `+1 min`, temporizador circular): es `gestionar-temporizador-de-paso`. Aquí solo el dato pasivo «⏱ N min» (D6), **ya sustituido por el widget del temporizador de esa feature** (D5 de `gestionar-temporizador-de-paso`; cambio de comportamiento aceptado).
- **Voz** (locución de pasos, «Escuchar», comandos, micrófono): es `controlar-cocina-con-voz` (`⚠ S04`). Sin `SpeechRecognizer`/`TextToSpeech` ni permisos nuevos.
- **Editar Recetas ni vínculos** (RD21): el Modo Cocina solo lee; las asignaciones se hacen en el formulario.
- **Cambios de Room, validaciones (`RecipeValidation`) o prompts de IA**: ninguno; Room sigue en **v4**.
- **Persistencia del progreso de cocción**: el paso activo vive solo en memoria.
- **Stepper de Raciones en el Modo Cocina**: para cambiar Raciones se vuelve al Detalle (D8).
- **Casillas/marcado de ingredientes en el drawer**: lista de solo lectura (D5). Sin precios (`P12`, `⚠ S02`) ni altas a la Lista de la Compra desde el drawer.

## Job Story

When estoy cocinando una receta con las manos ocupadas o mojadas,
I want to ver cada paso a pantalla completa, grande y con lo que necesito a mano,
so I can seguir la receta a distancia sin perderme ni tener que leer el móvil de cerca.

## Users And Permissions

- Usuario personal (1-5 usuarios, MVP): única interacción; sin roles, sin cuentas, sin permisos Android nuevos, sin red ni IA.

## Requisitos funcionales

- **RF-CPP1**: pantalla real «Modo Cocina» (ruta `cook_mode/{recipeId}?servings={servings}`) que sustituye al destino provisional. Top bar con «Volver» y el título «Modo Cocina» (`cook_mode_title`), título de la Receta, barra de avance «Paso X de Y» con progreso visual y el texto del **Paso activo** en tipografía grande (≥ 18 px, contraste AAA).
- **RF-CPP2**: drawer **colapsable** «Ingredientes para este paso» con los ingredientes **vinculados del paso activo** (RD21) en el orden de ingredientes de la Receta, cada uno con su **cantidad escalada** (RD03) en formato del Detalle; **lista de solo lectura** (D5). Un Paso sin vínculos muestra **«Sin ingredientes»** (D1/D4).
- **RF-CPP3**: navegación por Pasos con «Paso Anterior» (deshabilitado en el Paso 1) y botón primario «Siguiente»; en el **último Paso** el botón pasa a **«Terminar»** y vuelve al Detalle (D2). El Paso activo vive en memoria: sobrevive a la rotación y se reinicia al reabrir el Modo Cocina (D7).
- **RF-CPP4**: **herencia de Raciones**: el Modo Cocina recibe las Raciones elegidas en el Detalle y muestra las cantidades escaladas igual que su checklist (RD03, mismo redondeo); sin stepper (D8). Sin argumento de Raciones, se usan las base.
- **RF-CPP5**: la CTA «Empezar Modo Cocina» del Detalle queda **deshabilitada con 0 Pasos** (D3; cambia RF-DR7 de `ver-detalle-de-receta`, que era siempre habilitada) y con ≥ 1 Paso abre el Modo Cocina real con el id de la Receta y las Raciones actuales.
- **RF-CPP6**: si el Paso tiene `timerMinutes`, muestra el hint pasivo **«⏱ N min»** (D6); sin cuenta atrás, alarma ni controles de tiempo. **Cambiada por `gestionar-temporizador-de-paso`** (D5 de esa spec): el hint quedó sustituido por su widget de temporizador con cuenta atrás y alarma.

## Acceptance Scenarios

### Scenario 1 (verify 1): iniciar el Modo Cocina muestra el Paso 1 con sus ingredientes

Given una Receta con 2 Pasos, el Paso 1 con «Patatas» vinculada (700 g en 4 Raciones base) y Raciones a preparar = 2 en el Detalle
When pulso «Empezar Modo Cocina»
Then se abre «Modo Cocina» con «Paso 1 de 2», el texto del Paso 1 y el drawer con «Patatas» y su cantidad escalada (350 g)

### Scenario 2 (verify 2): avanzar de paso actualiza la barra de avance

Given el Modo Cocina en el Paso 1 de 2
When pulso «Siguiente»
Then la barra de avance muestra «Paso 2 de 2» y el botón primario pasa a «Terminar»

### Scenario 3: «Terminar» vuelve al Detalle

Given el Modo Cocina en el último Paso
When pulso «Terminar»
Then vuelvo al Detalle de la misma Receta con las Raciones que había elegido

### Scenario 4: drawer sin vínculos

Given un Paso sin ingredientes vinculados (Receta de IA, legada o sin asignar)
Then el drawer muestra «Sin ingredientes» y nunca «todos» los ingredientes de la Receta

### Scenario 5: la CTA se deshabilita con 0 Pasos

Given un Borrador sin Pasos abierto en el Detalle
When lo consulto
Then «Empezar Modo Cocina» está deshabilitada y no navega

### Scenario 6: salir a mitad de la cocción

Given el Modo Cocina en un Paso intermedio
When pulso «Volver» o el atrás del sistema
Then vuelvo al Detalle sin confirmación; al reabrir el Modo Cocina empieza en el Paso 1

### Scenario 7: textos es/en

Given el idioma de la app en español o en inglés
Then los textos del Modo Cocina, el drawer y los botones cambian con el idioma (paridad de claves)

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `cocinar-paso-a-paso` (steps/verification) y sus dependencias `accepted`.
- `docs/progress-history.md` (sesión 93, 2026-10-27) — decisiones del usuario para esta feature (drawer, Raciones, «Terminar», CTA con 0 pasos).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes.COOK_MODE_PROVISIONAL` → `ProvisionalScreen(cook_mode_title)`; `onStartCooking` navega hoy sin parámetros.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — `ProvisionalTestTags.COOK_MODE_SCREEN`, que usan los E2E y que esta feature retira.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — `StartCookingBar` (CTA primaria 52 dp, siempre habilitada), `RecipeDetailTestTags.START_COOKING`, `formatQuantity(quantity, locale)` (formato D7 del Detalle).
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModel.kt` — `Detail(recipe, match, servings, ingredientRows…)` con escalado RD03 vía `ServingsScaling`; `requestedServings` vive en memoria.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailRoute.kt` — `onStartCooking: () -> Unit` sin argumentos (habrá que pasar recipeId + servings).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStep.kt`, `RecipeIngredient.kt`, `Recipe.kt` — `ingredientIds` e id estable de Ingrediente (RD21), `baseServings`, `timerMinutes`.
- `app/src/main/java/com/whatsforlunch/app/domain/servings/ServingsScaling.kt` — `clampServings` (1-12) y `scaleIngredientQuantity` (redondeo 1 decimal salvo raciones = base).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — patrón `viewModel { parameters -> … }` para ViewModels con `recipeId`.
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeDetailE2ETest.kt` — `startCookingCtaOpensProvisionalCookModeAndBack` (S5, usa `ProvisionalTestTags.COOK_MODE_SCREEN` y hoy siembra una Receta **sin Pasos**) e `incompleteDraftShowsEmptyStates`; ambos se adaptan.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — `cook_mode_title`, `recipe_detail_start_cooking`; paridad actual 297/297.
- `docs/domain-model.md` (RD03, RD21), `DESIGN.md` (fila «Modo Cocina», AAA, `body-lg`, targets ≥ 48 dp), `uxdocs/chefencasa_modo_cocina_paso_a_paso/` (inspiración `P16`), `docs/build-brief.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md` (comandos/gates).

### Existing Patterns To Follow

- MVVM + StateFlow + Koin (`viewModel { parameters -> … }`), `*Route` sin estado → `*Screen` con callbacks; reglas de negocio puras en `domain/` (P25: la UI no contiene reglas).
- Navegación por rutas con argumentos (`recipe_detail/{recipeId}`), vuelta con `popBackStack` (patrón `Q-arrancar-app-en-mis-recetas-3`).
- Escalado y formato de cantidades reutilizados: `ServingsScaling` (RD03) y `formatQuantity` (D7 de `ver-detalle-de-receta`).
- Test tags por pantalla en objetos `*TestTags`; textos visibles en `values/` + `values-en/` con paridad de claves.
- Tests: unit JVM en `app/src/test` (reglas + ViewModel con InMemory repos), E2E en `app/src/androidTest` con fixtures locales, sin IA ni red.

### Current Gaps

- No existe la pantalla de Modo Cocina: solo el destino provisional vacío.
- La CTA del Detalle no recibe el id de la Receta ni las Raciones y no se deshabilita nunca.
- No hay ViewModel, reglas ni textos del Modo Cocina.

## Technical Approach

**Regla pura (D9)** — `domain/cookmode/CookModeRules.kt` con `stepIngredientRows(recipe, stepIndex, servings): List<RecipeIngredient>`: filtra los ingredientes de la Receta cuyo `id` está en `steps[stepIndex].ingredientIds`, **en el orden de ingredientes de la Receta** (D7 de RD21), y devuelve copias con la cantidad escalada por `ServingsScaling.scaleIngredientQuantity`. Lista vacía = «Sin ingredientes» (la UI decide el copy). Sin dependencias de Android.

**ViewModel** — `ui/cookmode/CookModeViewModel(recipeRepository, recipeId, servings)` con estado sellado `Loading | NotFound | Cooking(recipe, stepIndex, totalSteps, ingredientRows)`; `servings` llega por parámetros de Koin (argumento de ruta) y se recorta con `clampServings`; `next()`/`previous()` mueven el índice dentro de `0..totalSteps-1` y el índice vive en `MutableStateFlow` (memoria: sobrevive rotación, se reinicia al crear el ViewModel). No escribe en ningún repositorio.

**UI** — `ui/cookmode/CookModeScreen.kt` + `CookModeRoute.kt` sobre `Scaffold`: top bar «Volver» + «Modo Cocina»; título de la Receta; barra de avance (`LinearProgressIndicator` + texto «Paso X de Y»); tarjeta del Paso con rótulo «PASO X», texto en `body-lg` (≥ 18 px) y hint «⏱ N min» si `timerMinutes != null` (D6); drawer colapsable «Ingredientes para este paso» (filas de solo lectura «nombre (cantidad unidad)», estado «Sin ingredientes») y dock inferior fijo con «Paso Anterior» (área ≥ 48 dp, deshabilitado en el Paso 1) y botón primario (≥ 52 dp) «Siguiente» → «Terminar» en el último Paso. Contraste AAA y targets ≥ 48 dp (`DESIGN.md`). `CookModeTestTags` para E2E.

**Navegación y Detalle** — `AppRoutes.COOK_MODE = "cook_mode/{recipeId}?servings={servings}"` con `cookMode(recipeId, servings)`; `RecipeDetailRoute.onStartCooking` pasa a `onStartCooking(recipeId, servings)` (el `Detail` del ViewModel ya tiene ambos). Se retira el composable provisional de Modo Cocina y `ProvisionalTestTags.COOK_MODE_SCREEN`; si `ProvisionalScreen` queda sin consumidores, se retira también (verificar con búsqueda antes). `StartCookingBar` gana `enabled = recipe.steps.isNotEmpty()` (RF-CPP5). Registro de `CookModeViewModel` en `AppModule`.

**Sin cambios** en Room (v4), `RecipeValidation`, prompts, Despensa, Lista de la Compra ni IA.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/cookmode/CookModeRules.kt` — create; regla pura de filas del paso.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModel.kt` — create; estado y navegación por pasos.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt` — create; UI + `CookModeTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeRoute.kt` — create; unión con Koin.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modify; ruta `cook_mode/{recipeId}?servings={servings}` y retirada del destino provisional.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — modify/delete; retirar `COOK_MODE_SCREEN` (y el fichero entero si queda sin usos).
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailRoute.kt`, `RecipeDetailScreen.kt`, `RecipeDetailViewModel.kt` — modify; callback `onStartCooking(recipeId, servings)` y CTA deshabilitada con 0 Pasos.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; `viewModel { parameters -> CookModeViewModel(...) }`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify; textos del Modo Cocina con paridad.
- `app/src/test/java/com/whatsforlunch/app/domain/cookmode/CookModeRulesTest.kt` — create.
- `app/src/test/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModelTest.kt` — create.
- `app/src/androidTest/java/com/whatsforlunch/app/CookModeE2ETest.kt` — create; S1-S7.
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeDetailE2ETest.kt` — modify; adaptar S5 a la pantalla real (y su Receta con ≥ 1 Paso) y el borrador sin Pasos a la CTA deshabilitada.
- Docs: `docs/domain-model.md`, `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (ver más abajo). `feature_list.json`: al cerrar, solo `status`/`evidence`.

## Visual Design Impact

- UI involved: **sí**. Design source: `DESIGN.md` (tokens Warm Culinary Craft) + `uxdocs/chefencasa_modo_cocina_paso_a_paso/` (inspiración `P16`; mandan los tokens, `P17`).
- Screens/states afectados: nueva pantalla «Modo Cocina» (paso 1, paso intermedio, último Paso con «Terminar», drawer desplegado/colapsado, «Sin ingredientes», hint «⏱ N min», «Paso Anterior» deshabilitado) y Detalle (CTA deshabilitada con 0 Pasos).
- Del prototipo se toma la composición (barra «Paso X de Y», tarjeta del paso, drawer, dock inferior); **no** se implementan el bloque de temporizador, «Voz Activa», «Escuchar» ni «Consejo del Chef» (temporizador/voz = features aparte; el consejo no está en el modelo).
- New design artifact required: **no**; se compone con tokens y componentes existentes.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** (no existe y no hace falta crearlo): sin capas ni dependencias nuevas.
- `CONSTRAINTS.md`: **not needed** (no existe y no hace falta crearlo): sin MUST/MUST NOT nuevos.
- `AGENTS.md`: **not needed** — sin cambios de flujo, comandos ni convenciones.
- `docs/domain-model.md`: **update** — nueva regla **RD22** (Modo Cocina: herencia de Raciones, drawer con los vínculos del Paso activo, navegación y «Terminar», CTA con ≥ 1 Paso, progreso solo en memoria) y casos límite nuevos.
- `DESIGN.md`: **update** — la fila «Modo Cocina» pasa a describir su composición real (sin temporizador ni voz todavía).
- `docs/technical-discovery.md`: **update** — la pantalla real de Modo Cocina sustituye al destino provisional (sin cambios de Room ni de comandos).
- `docs/risks-and-open-questions.md`: **update** — registrar `Q-cocinar-paso-a-paso-1..3` (D4-D6 de esta spec) al implementar.
- `CONTEXT.md`: **not needed** — «Modo Cocina» ya está definido; temporizadores y voz siguen siendo parte de su definición de producto aunque lleguen en features posteriores.

## Decisions

- **D1** (usuario 2026-10-27, sesión 93): el drawer muestra los **ingredientes vinculados del Paso activo** (los que aporta RD21), no «todos» los de la Receta.
- **D2** (usuario 2026-10-27): en el último Paso el botón pasa a **«Terminar»** y vuelve al Detalle.
- **D3** (usuario 2026-10-27): la CTA «Empezar Modo Cocina» se **deshabilita con 0 Pasos**; cambia el comportamiento aceptado de `ver-detalle-de-receta` (RF-DR7, D4) y se adaptan sus E2E.
- **D4** (usuario 2026-10-27; matizado 2026-10-28): el Modo Cocina **hereda las Raciones** elegidas en el Detalle (cantidades escaladas, RD03); un Paso sin vínculos muestra **«Sin ingredientes»** (sin fallback a todos los ingredientes).
- **D5** (usuario 2026-10-28): el drawer es **lista de solo lectura** (nombre + cantidad escalada), sin casillas ni contador «N de M».
- **D6** (usuario 2026-10-28): el Paso muestra su temporizador solo como **dato pasivo «⏱ N min»**; cuenta atrás, alarma y `+1 min` son de `gestionar-temporizador-de-paso`. *(Sustituido después por el widget de `gestionar-temporizador-de-paso`, D5 de esa spec.)*
- **D7** (derivada): el Paso activo vive **solo en memoria** (sobrevive a la rotación, se reinicia al reabrir); salir con «Volver»/atrás no pide confirmación.
- **D8** (derivada): sin stepper de Raciones en el Modo Cocina; para cambiar Raciones se vuelve al Detalle.
- **D9** (derivada/técnica): la regla de filas del Paso es pura en `domain/` (`CookModeRules`); la UI no contiene reglas de negocio (P25).

## Casos límite

| Caso | Comportamiento |
| --- | --- |
| Receta sin Pasos | CTA «Empezar Modo Cocina» deshabilitada; el Modo Cocina no existe para ella (D3) |
| Paso sin ingredientes vinculados | Drawer con «Sin ingredientes» (D1/D4) |
| Receta de IA o legada sin vínculos | Todos sus Pasos muestran «Sin ingredientes»; se vinculan editando (RD21) |
| Raciones = Raciones base | Cantidades con su precisión original (RD03) |
| Raciones heredadas fuera de 1-12 | Se recortan con `clampServings` (RD03) |
| Borrador con ≥ 1 Paso | Se puede cocinar igual (sin cambios de validación ni de estado) |
| Paso 1 | «Paso Anterior» deshabilitado |
| Último Paso | Botón primario «Terminar» → Detalle (D2) |
| Salir a mitad («Volver»/atrás) | Vuelve al Detalle sin confirmación; al reabrir, Paso 1 (D7) |
| Rotación de la pantalla | El Paso activo se conserva (ViewModel) |
| Paso con `timerMinutes` | Hint pasivo «⏱ N min»; sin cuenta atrás ni alarma (D6) |
| Paso con `timerMinutes` nulo | Sin hint de tiempo |
| Referencias huérfanas en `ingredientIds` | Ya las descarta RD21 al cargar; el drawer solo lista ids válidos |
| Ingredientes vinculados repetidos | Imposible por construcción (lista de ids únicos en el diálogo del formulario) |

## Implementation Plan

1. **Dominio**: `CookModeRules.stepIngredientRows` (vínculos del Paso → filas en orden de la Receta con cantidad escalada RD03) + unit.
2. **ViewModel**: `CookModeViewModel` (carga por id, índice de Paso en memoria, `next`/`previous` con límites, `NotFound`) + unit.
3. **UI**: `CookModeScreen`/`CookModeRoute` (barra «Paso X de Y» + progreso, tarjeta del Paso con texto grande y hint «⏱ N min», drawer colapsable de solo lectura, dock «Paso Anterior»/«Siguiente»/«Terminar»), test tags y textos es/en.
4. **Navegación y Detalle**: ruta `cook_mode/{recipeId}?servings={servings}`, callback con parámetros, CTA deshabilitada con 0 Pasos, retirada del destino provisional y registro Koin.
5. **E2E**: `CookModeE2ETest` (S1-S7) y adaptación de `RecipeDetailE2ETest` (S5 y borrador sin Pasos).
6. **Docs y cierre**: `domain-model.md` (RD22), `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (Q-…) y `PROGRESS.md`/`feature_list.json`.

## Implementation Tasks

- [ ] T1: `CookModeRules` + `CookModeRulesTest` (orden de la Receta, escalado RD03, lista vacía = «Sin ingredientes»).
- [ ] T2: `CookModeViewModel` + `CookModeViewModelTest` (Paso 1 al entrar, `next`/`previous` con clamp, último Paso, `NotFound`, servings por defecto = base).
- [ ] T3: `CookModeScreen`/`CookModeRoute` + `CookModeTestTags` + textos es/en con paridad.
- [ ] T4: navegación (`AppRoutes.COOK_MODE` con `recipeId`/`servings`), registro Koin y retirada del destino provisional (`ProvisionalTestTags.COOK_MODE_SCREEN`).
- [ ] T5: Detalle: `onStartCooking(recipeId, servings)` y CTA deshabilitada con 0 Pasos.
- [ ] T6: E2E `CookModeE2ETest` (S1-S7, verify 1 = S1, verify 2 = S2) + adaptar `RecipeDetailE2ETest` sin regresión.
- [ ] T7: docs (`domain-model.md` RD22 + casos límite, `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` Q-1..3).
- [ ] T8: gates completos y cierre (`PROGRESS.md`, `feature_list.json` solo `status`/`evidence`).

## Verification Plan

- `./init.ps1` (Windows) o `./init.sh` — «Entorno listo», exit 0. El script ejecuta comprobaciones no bloqueantes (gradle, `installDebug`, arranque de la app) y **no** arranca procesos largos; puede imprimir comandos de seguimiento.
- `./lint.ps1` / `./gradlew :app:lintDebug` — **0 errores** y sin warnings nuevos sobre la línea base (49).
- `./unit.ps1` / `./gradlew :app:testDebugUnitTest --rerun-tasks` — todo verde (hoy 36 suites / 239 tests); entran `CookModeRulesTest` y `CookModeViewModelTest`.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismo conjunto de claves en `values/strings.xml` y `values-en/strings.xml` (hoy 297/297).
- **E2E focalizada** (la feature cambia navegación y comportamiento visible): `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.CookModeE2ETest,com.whatsforlunch.app.RecipeDetailE2ETest` — verde con S1-S7 (verify 1 = S1, verify 2 = S2) y la adaptación del Detalle sin regresión. Fixtures locales, sin IA ni red. La cobertura E2E persistente es obligatoria aquí: la feature cambia rutas, el flujo Detalle → Modo Cocina y la CTA.
- **E2E batería**: `./e2e.ps1` (`connectedDebugAndroidTest` completo) — sin regresiones sobre los 188 tests de inventario (los flakes conocidos del AVD se documentan y reejecutan como en sesiones previas).
- Manual (apoyo, con `adb`/uiautomator): receta con 2 Pasos y vínculos, Raciones 2 sobre base 4 → drawer con cantidades escaladas (S1); «Siguiente» y «Terminar» (S2/S3); borrador sin Pasos con CTA deshabilitada (S5).

## Evidence To Capture

- `docs/evidence/cocinar-paso-a-paso/`: informe + **2 capturas es 1280×2856** (PNG binario): (1) Modo Cocina en el Paso 1 con el drawer desplegado y cantidades escaladas; (2) el último Paso con el botón «Terminar».
- Salida de los gates (`init`, `lint`, `unit`, `assembleDebug`, paridad), de la E2E focalizada (nombres de los tests) y recuento de la batería.
- `PROGRESS.md` (Estado verificado + Siguiente paso + entrada en `docs/progress-history.md`) y `evidence` de `feature_list.json`.
- Sin API keys, sin llamadas a la IA ni PII en la evidencia (`⚠ S05`).

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance (sin temporizador funcional, sin voz, sin permisos, sin Room/IA/prompts).
- [ ] S1-S7 pasan (verify 1 = S1, verify 2 = S2) y los E2E previos siguen en verde, incluida la adaptación de `ver-detalle-de-receta`.
- [ ] D1-D9 cumplidas (drawer solo con vínculos del Paso activo, «Sin ingredientes», solo lectura, Raciones heredadas, «Terminar», CTA con 0 Pasos, progreso en memoria, hint «⏱ N min»).
- [ ] Evidencia presente: gates, E2E focalizada y batería, 2 capturas es válidas (cabecera PNG, 1280×2856) e informe en `docs/evidence/cocinar-paso-a-paso/`.
- [ ] Cobertura E2E añadida para el cambio de comportamiento visible (nueva pantalla + CTA deshabilitada).
- [ ] `feature_list.json` solo cambia `status`/`evidence`; `PROGRESS.md` al día; docs durables actualizadas (RD22, `DESIGN.md`, `docs/technical-discovery.md`, `Q-cocinar-paso-a-paso-1..3`).
- [ ] Sin comportamiento ajeno ni trabajo de `gestionar-temporizador-de-paso` ni `controlar-cocina-con-voz` añadido.
- [ ] Seguridad: sin secretos, sin permisos nuevos, sin red; sin datos nuevos persistidos.
