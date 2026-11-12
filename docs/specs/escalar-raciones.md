# Feature Implementation Spec: Escalar raciones

## Source Feature

- `id`: escalar-raciones
- `area`: funcional
- `depends_on`: ver-detalle-de-receta (`accepted`, sesión 48)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

En el Detalle de Receta, el usuario ajusta las **Raciones a preparar** con un stepper `[-] N pers. [+]` (entre 1 y 12) y ve cómo las cantidades del checklist de ingredientes se recalculan en vivo según **RD03** (`cantidad = cantidad base × raciones / raciones base`), sin editar ni persistir nada. Es el bloque «Raciones a preparar» que `ver-detalle-de-receta` reservó entre las métricas y el checklist (D2 de esa spec) y la materialización del escalador lineal de `docs/domain-model.md`.

## Non-Goals

- **Persistir la selección de raciones** (ni Room, ni preferencias, ni al navegar): es estado de la pantalla del Detalle, como el marcado del checklist (D3 de `ver-detalle-de-receta`). `baseServings` y las cantidades base de `RecipeIngredient` quedan intactos en el modelo.
- **Escalar otra cosa que no sean cantidades de ingredientes**: pasos, tiempos, temporizadores, calorías por porción, la métrica «Raciones base» del grid 2×2 y el badge de Coincidencia (RD01/RD10: solo presencia en la Despensa) no cambian.
- **Consumir las raciones en el Modo Cocina** (`cocinar-paso-a-paso`) ni en la Lista de la Compra (`anadir-faltantes-a-la-lista`): la CTA «Empezar Modo Cocina» sigue yendo al destino provisional sin parámetros.
- Cambiar el formulario de crear/editar (`crear-receta`, `editar-receta`), la Despensa, el Escáner, el esquema Room ni `RecipeValidation` de publicación.
- Cambiar la fórmula ni los límites de RD03 (lineal, 1-12) y ningún tipo de escalado no lineal («medio plato», redondeo a unidades comestibles, etc.).

## Job Story

Cuando voy a cocinar para más (o menos) gente de la que dice la Receta,
quiero ajustar las Raciones a preparar desde la ficha de la Receta,
para ver las cantidades de los ingredientes recalculadas al momento sin tener que editar la Receta ni hacer cuentas.

## Users And Permissions

- **Usuario** (único rol del MVP, `P31`): ajusta las raciones en el Detalle de sus propias Recetas (Borradores y Publicadas). Sin permisos adicionales; no hay datos nuevos que proteger.

## Requisitos funcionales

Prefijo propio `RF-ER#` (RD03 y los `RF-DR#` de `ver-detalle-de-receta.md` se citan sin renumerar).

- **RF-ER1**: el Detalle muestra, entre las métricas 2×2 y el checklist, el bloque «Raciones a preparar» con el stepper `[-] N pers. [+]`, su subtítulo y la nota «Cantidades actualizadas en vivo según la selección.» (D1).
- **RF-ER2** (RD03): al cambiar las raciones, la cantidad visible de cada ingrediente pasa a ser `cantidad base × (raciones / raciones base)`, redondeada a **máximo 1 decimal** (`Q-escalar-raciones-1`, decisión del usuario 2026-10-23; D2); nombre, unidad, opcionalidad, orden y contador «N de M» no cambian.
- **RF-ER3**: las raciones admitidas son enteras de **1 a 12** (RD03); el stepper arranca en `baseServings`, «−» queda deshabilitado en 1 y «+» deshabilitado en 12 (D4), y el valor nunca sale del intervalo (verify 2 de `feature_list.json`).
- **RF-ER4**: el escalado es solo de visualización: al salir del Detalle y volver a entrar, el stepper vuelve a `baseServings` y el checklist muestra las cantidades base (nada se persiste; D3).
- **RF-ER5**: los ingredientes sin cantidad (`quantity = 0`, «sin cantidad») siguen sin cantidad tras escalar; las cantidades escaladas usan el formato del idioma activo sin ceros sobrantes (D7 de `ver-detalle-de-receta`).
- **RF-ER6**: el bloque de raciones se muestra también en Recetas sin ingredientes y en Borradores incompletos (el checklist conserva su estado vacío «Sin ingredientes»).

## Acceptance Scenarios

### Scenario 1: Escalado a la mitad (verify 1 de `feature_list.json`)

Given una Receta Publicada con Raciones base 4 e ingredientes «200 g Harina» y «2 Huevos»
When el usuario pulsa «−» hasta dejar las raciones en 2
Then el checklist muestra «100 g Harina» y «1 Huevos» (cantidades reducidas a la mitad) y el stepper indica 2 (RF-ER1, RF-ER2)

### Scenario 2: Cantidades con decimales (RD03, `Q-escalar-raciones-1`)

Given la misma Receta con «1 Huevo» y base 4
When el usuario sube las raciones a 3
Then la cantidad mostrada es «0,8» en español («0.8» en inglés): escalado lineal 0,75 redondeado a máximo 1 decimal (RF-ER2)

### Scenario 3: Límites 1-12 (verify 2 de `feature_list.json`)

Given cualquier Receta en el Detalle
When el usuario baja a 1 ración y sube a 12
Then en 1 el botón «−» está deshabilitado y no decrementa más; en 12 el botón «+» está deshabilitado y no incrementa más (RF-ER3); unitariamente, `setServings(0)` deja 1 y `setServings(13)` deja 12

### Scenario 4: Solo visualización (RF-ER4, RD03)

Given una Receta con raciones escaladas a 2 y una fila del checklist marcada
When el usuario sale del Detalle y vuelve a abrir la misma Receta
Then el stepper muestra las Raciones base, las cantidades son las base y las marcas del checklist se han reiniciado (como hasta ahora); el badge de Coincidencia no ha variado durante el escalado

### Scenario 5: Receta sin ingredientes (RF-ER6)

Given un Borrador sin ingredientes
When el usuario abre el Detalle
Then el bloque «Raciones a preparar» se muestra con el stepper operativo (arrancado en `baseServings`) y el checklist conserva «Sin ingredientes»

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| `baseServings = 1` | El stepper arranca en 1 y «−» nace deshabilitado |
| `baseServings = 12` | El stepper arranca en 12 y «+» nace deshabilitado |
| Escalado exacto (200 g, 4 → 2) | «100 g» sin decimal sobrante (formato D7) |
| Escalado inexacto (1 huevo, 4 → 3) | «0,8» en es / «0.8» en en (máximo 1 decimal) |
| Ingrediente sin cantidad (`quantity = 0`) | Sigue sin cantidad visible tras escalar |
| Ingrediente opcional | Se escala igual que el resto (RD10 solo excluye los opcionales de la Coincidencia) |
| Ingrediente marcado en el checklist | La marca se conserva al cambiar raciones: la identidad de fila (D3) no depende de la cantidad escalada (D5) |
| Raciones = Raciones base | Las cantidades se muestran tal cual (sin recalcular visiblemente) |
| Rotación de pantalla | La selección de raciones sobrevive (estado en el ViewModel, D3); no sobrevive a salir de la pantalla |
| Decimales largos (100 g, 3 → 4 = 133,33…) | «133,3» (redondeo a 1 decimal, `Q-escalar-raciones-1`) |

## Repository Research

### Files Inspected

- `feature_list.json` — `escalar-raciones` (steps: selector 1-12 y RD03; verify 1 E2E + verify 2 unit) y los límites de `cocinar-paso-a-paso` / `anadir-faltantes-a-la-lista`.
- `docs/domain-model.md` — RD03 (fórmula lineal, 1-12), cálculo de ejemplo (base 4 → 2: 200 g → 100 g), validación «Raciones: entero entre 1 y 12», caso límite «Raciones fuera de límite».
- `CONTEXT.md` — glosario: «Raciones: número de comensales de una Receta; ajusta las cantidades (entre 1 y 12)».
- `docs/specs/ver-detalle-de-receta.md` — D2 reserva este bloque entre métricas y checklist; D7 (formato de cantidad); alcances que esta feature desbloquea.
- `docs/validations/ver-detalle-de-receta.md` — declara «Sin selector `[-] N pers [+]` (RD03 fuera de alcance)»: esta feature lo implementa.
- `uxdocs/chefencasa_detalle_de_receta/code.html` (solo lectura) — bloque «Servings Interactive Scaler» (título, subtítulo «Cantidades adaptadas a tu mesa», stepper `remove` / número + «pers.» / `add`, nota «Cantidades actualizadas en vivo según la selección.») y su `adjustServings(delta)` (`Math.max(1, Math.min(12, …))`, `scaled = base × servings / baseServings`, `toFixed(1)`).
- `DESIGN.md` — el escalador forma parte del rol del Detalle; el prototipo es inspiración (`P16`), no contrato (`P17` usa los tokens YAML).
- `docs/risks-and-open-questions.md` — `⚠ S02` (sin precios, no afecta), RK04 (el material UX no cubre estados: se propone copy aquí), registro `Q-…`; no hay ninguna Q previa de `escalar-raciones`.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — `RecipeDetailTestTags` (`SERVINGS` ocupa hoy la métrica «Raciones base»), `MetricsGrid`, `IngredientChecklist`/`IngredientRow` (clave D3 `id.ifBlank { "$name-$quantity-$unit" }`), `formatQuantity` (D7), `ingredientLine`.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModel.kt` — `RecipeDetailUiState.Detail(recipe, match)` con `combine` de receta + Despensa; sin estado de raciones.
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt` (`baseServings: Int = 4`), `RecipeIngredient.kt` (`quantity: Double`).
- `app/src/main/java/com/whatsforlunch/app/domain/createrecipe/RecipeValidation.kt` — `MIN_BASE_SERVINGS = 1`, `MAX_BASE_SERVINGS = 12`, `isValidServings`: los límites 1-12 **ya existen en dominio** y se reutilizan.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` y `data/repository/RoomRecipeRepository.kt` — los ingredientes siempre reciben `id` (`"$id-ingredient-$index"` / entidad Room), pero la clave de fila tiene fallback por cantidad: riesgo para el checklist al escalar (ver D5).
- `app/src/main/res/values/strings.xml` — claves `recipe_detail_*` existentes («Raciones base» = `recipe_detail_metric_servings`). `values-en/strings.xml` no inspeccionado en detalle: la convención de paridad es/en es conocida y la verifican los validadores.
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeDetailE2ETest.kt` — 7 tests (S1-S7 de su spec), helpers `launchDetail`/`writePhoto`/`spanishContext`; `app/src/test/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModelTest.kt` — patrón de unit del ViewModel.
- Scripts `init`/`lint`/`unit`/`e2e` (.sh/.ps1) — envoltorios de `./gradlew` (ver «Verification Plan»).

### Existing Patterns To Follow

- MVVM + StateFlow con las reglas de negocio en `domain/` (la UI no contiene reglas: `AGENTS.md`); Koin en `di/AppModule.kt`; pantallas sin estado + `*Route`.
- Stepper con límites y botones deshabilitados en los extremos: el de la Despensa (`gestionar-alimentos-de-la-despensa`, 0..99, «−» deshabilitado en 0 y «+» en el máximo) es el patrón visual y de accesibilidad (targets ≥ 48 dp).
- Píldoras/bloques `Surface` con `CardShape`/`PillShape`, `testTag` en objetos `*TestTags`, textos solo vía `strings.xml` es/en con paridad de claves.
- Unit tests de reglas en `app/src/test/.../domain/...` (p. ej. `RecipeMatchTest`, `PantryItemValidationTest`); E2E con `createComposeRule`, repos in-memory o Room de prueba, contexto español y `waitUntil`.

### Current Gaps

- No existe el bloque de raciones: `MetricsGrid` termina y empieza `IngredientChecklist` (hueco reservado por D2 de `ver-detalle-de-receta`).
- RD03 no existe como código: no hay función de escalado en `domain/` (solo la fórmula documentada).
- `RecipeDetailUiState.Detail` no tiene estado de raciones ni expone cantidades calculadas.
- El stepper de la Despensa no es componente compartido: se reimplementa el bloque siguiendo su patrón (no se extrae a `ui/components/`; fuera de alcance).

## Technical Approach

- **Dominio** — nuevo `app/src/main/java/com/whatsforlunch/app/domain/servings/ServingsScaling.kt` (identificadores en inglés, `P19`/`P25`): `fun clampServings(servings: Int): Int` y `fun scaleIngredientQuantity(baseQuantity: Double, baseServings: Int, servings: Int): Double` que implementa RD03 con redondeo a 1 decimal (`(x * 10).roundToInt() / 10.0`). Los límites se toman de `RecipeValidation.MIN_BASE_SERVINGS`/`MAX_BASE_SERVINGS` (sin duplicar constantes); `baseServings` fuera de 1-12 (dato no alcanzable con `crear-receta`) devuelve la cantidad base sin escalar.
- **ViewModel** — `RecipeDetailViewModel` gana `servings: StateFlow<Int>` (inicial: `clampServings(recipe.baseServings)`) y `fun setServings(value: Int)` (clamped 1-12). `RecipeDetailUiState.Detail` pasa a `Detail(recipe, match, servings, ingredientRows)`, donde `ingredientRows` son copias escaladas de los ingredientes (`quantity` recalculada) que conservan `id`, `name`, `unit` y `optional`; la UI solo formatea.
- **Checklist (D5)** — `IngredientChecklist` sigue derivando la identidad de fila de las cantidades **base** (`RecipeIngredient.key()`), de modo que las marcas del checklist no se pierden ni se reinician al escalar; si hiciera falta tocar `key()`, su fallback no debe depender de la cantidad escalada.
- **UI** — bloque `ServingsScaler` en `RecipeDetailScreen` entre `MetricsGrid` e `IngredientChecklist`: tarjeta `surfaceContainerLowest` con título (`headlineSmall`), subtítulo (`bodySmall`, `onSurfaceVariant`), stepper en contenedor `surfaceContainerLow` con «−» y «+» (iconos `remove`/`add`, área táctil ≥ 48 dp, estado deshabilitado visible en 1 y 12), valor en `primary` bold + «pers.» y nota final «Cantidades actualizadas en vivo según la selección.» con icono. La métrica «Raciones base» del grid no se toca.
- **Textos** — `values/strings.xml` + `values-en/strings.xml`: `recipe_detail_servings_scaler_title`, `recipe_detail_servings_scaler_subtitle`, `recipe_detail_servings_scaler_note`, `recipe_detail_servings_scaler_unit` («pers.»/«servings»), `recipe_detail_servings_decrease` («Disminuir raciones») y `recipe_detail_servings_increase` («Aumentar raciones») como `contentDescription` de los botones. El copy exacto es propuesta de la spec (laguna RK04, patrón `Q-arrancar-app-en-mis-recetas-5`) y se afina en la revisión visual del validador.
- **Tests** — unit de dominio (`ServingsScalingTest`: fórmula, redondeo 1 decimal, clamp 1-12, `quantity = 0`, `baseServings` inválido) y ampliación de `RecipeDetailViewModelTest` (arranque en base, `setServings` clamp, filas escaladas con identidad estable); E2E ampliando `RecipeDetailE2ETest` (verify 1 + S2-S5) sin romper la suite.

### Decisiones (para no reabrir el debate en implementación)

- **D1 — bloque entre métricas y checklist**, con el copy del prototipo (`uxdocs/…/code.html`): título «Raciones a preparar», subtítulo «Cantidades adaptadas a tu mesa», stepper `[-] N pers. [+]` y nota «Cantidades actualizadas en vivo según la selección.». Sin entrada de texto, desplegable ni slider.
- **D2 - redondeo a máximo 1 decimal** (`Q-escalar-raciones-1`, decisión del usuario 2026-10-23): como el `toFixed(1)` del prototipo. Los enteros pierden el decimal al formatear (D7 de `ver-detalle-de-receta`): 200 g → «200», 0,75 → «0,8» en es («0.8» en en). Solo se redondea la cantidad **escalada**: con Raciones iguales a las base se muestra la cantidad guardada con su precisión original (0,25 l sigue «0,25 l»).
- **D3 — estado de la selección en el ViewModel**: arranca en `baseServings`, sobrevive a la rotación y se reinicia al salir de la pantalla. Sin persistencia ni efectos sobre el modelo.
- **D4 — límites con botones deshabilitados** (patrón del stepper de la Despensa), no solo clamp silencioso: en 1 «−» y en 12 «+» se ven deshabilitados; el clamp del dominio es la red de seguridad que cubre el verify 2.
- **D5 — identidad de fila estable**: las marcas del checklist se conservan al escalar; la cantidad escalada nunca entra en la clave de la fila.
- **D6 — el escalado afecta solo a cantidades visibles de ingredientes**: nada más en la ficha cambia (ni badge, ni «N de M», ni calorías, ni tiempos, ni «Raciones base»).

### Unknowns

- Ninguna bloqueante. El copy del bloque es propuesta validable visualmente (RK04) y la integración futura de las raciones con el Modo Cocina queda para `cocinar-paso-a-paso` (fuera de esta spec).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/servings/ServingsScaling.kt` — crear; regla RD03 (fórmula + redondeo + clamp).
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModel.kt` — modificar; `servings`, `setServings` y `Detail(recipe, match, servings, ingredientRows)` escalado.
- `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt` — modificar; bloque «Raciones a preparar» (D1), render de cantidades escaladas, `testTag`s nuevos (`recipe_detail_servings_scaler`, `recipe_detail_servings_decrease`, `recipe_detail_servings_value`, `recipe_detail_servings_increase`) y conservación de la clave de fila (D5).
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; claves `recipe_detail_servings_*` con paridad es/en.
- `app/src/test/java/com/whatsforlunch/app/domain/servings/ServingsScalingTest.kt` — crear; unit de RD03 (verify 2).
- `app/src/test/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailViewModelTest.kt` — modificar; arranque en base, clamp y filas escaladas.
- `app/src/androidTest/java/com/whatsforlunch/app/RecipeDetailE2ETest.kt` — modificar; verify 1 + S2-S5 sin romper los 7 tests existentes.
- `docs/domain-model.md` — update; añadir a RD03 el redondeo de visualización a máximo 1 decimal (`Q-escalar-raciones-1`).
- `docs/risks-and-open-questions.md` — update; registrar `Q-escalar-raciones-1` como resuelta (decisión del usuario 2026-10-23).
- `docs/validations/escalar-raciones.md` y `docs/evidence/escalar-raciones/*.png` — crear al validar (evidencia).

Sin cambios en Room (sin migración), en `AppNavHost`, en `di/AppModule` más allá del `viewModel` existente si la firma del ViewModel no cambia su construcción, ni en los scripts del harness.

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens YAML de `uxdocs/DESIGN.md` (`P17`) e `uxdocs/chefencasa_detalle_de_receta/code.html` como inspiración (`P16`).
- Screens or states affected: Detalle de Receta (nuevo bloque entre métricas y checklist; estados de botón deshabilitado en los límites).
- New design artifact required: no — el prototipo dibuja el bloque; las lagunas (estados deshabilitados, copy en inglés) se resuelven aquí (D1, D4).

Contrato visual (verificable por `uiautomator` y captura):

- Bloque: tarjeta `surfaceContainerLowest` con radio `rounded.xl` y sombra cálida, alineada al margen horizontal de las métricas; fila superior con título «Raciones a preparar» (`headlineSmall`, `onSurface`) y subtítulo «Cantidades adaptadas a tu mesa» (`bodySmall`, `onSurfaceVariant`) a la izquierda y el stepper a la derecha.
- Stepper: contenedor `surfaceContainerLow` redondeado; botones «−» y «+» de 48 dp de área táctil (icono 20 dp, `surfaceContainerLowest`, `onSurface`), valor en `primary` bold con «pers.» debajo en `labelSmall`/`onSurfaceVariant`; deshabilitado = tinte atenuado distinguible (patrón del stepper de la Despensa).
- Nota inferior: icono + «Cantidades actualizadas en vivo según la selección.» en `labelSmall`, color `secondary`.
- Estados cubiertos (laguna `RK04`): límites 1 y 12 con botón deshabilitado, Receta sin ingredientes, cantidades sin cantidad y decimales (0,8). Accesibilidad: contraste AA, targets ≥ 48 dp, `contentDescription` en ambos botones.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y no cambian capas, dominio ya existente (`domain/` gana un módulo de reglas) ni dependencias.
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD03 ya existe).
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- `docs/domain-model.md`: update — RD03 incorpora el redondeo de visualización a máximo 1 decimal (`Q-escalar-raciones-1`); el resto del modelo no cambia.
- `docs/risks-and-open-questions.md`: update — registrar `Q-escalar-raciones-1` (resuelta el 2026-10-23 por el usuario: máximo 1 decimal).
- `docs/technical-discovery.md`: not needed — sin pantallas nuevas (el Detalle ya está documentado), sin comandos ni dependencias nuevas.

## Implementation Plan

1. Crear `ServingsScaling.kt` en `domain/servings/` con RD03 (fórmula, redondeo 1 decimal, clamp 1-12 reutilizando `RecipeValidation`) y sus unit tests.
2. Ampliar `RecipeDetailViewModel` con `servings`/`setServings` y el `Detail` expuesto con filas escaladas e identidad de fila estable (D5); ampliar `RecipeDetailViewModelTest`.
3. Dibujar el bloque «Raciones a preparar» en `RecipeDetailScreen` (D1/D4) con sus `testTag`s, textos es/en y render de cantidades escaladas con `formatQuantity` (D7).
4. Ampliar `RecipeDetailE2ETest` (verify 1 + S2-S5) y dejar la regresión completa en verde.
5. Ejecutar el harness completo, capturar la evidencia y dejar la feature en `passing` para `feature-validator` (actualizando también `docs/domain-model.md` y `docs/risks-and-open-questions.md`).

## Implementation Tasks

- [ ] T1: `domain/servings/ServingsScaling.kt` + `ServingsScalingTest` (fórmula RD03, redondeo 1 decimal, clamp 1-12, `quantity = 0`, `baseServings` inválido). (RF-ER2, RF-ER3, RF-ER5, verify 2)
- [ ] T2: `RecipeDetailViewModel` con `servings`/`setServings` y filas escaladas (identidad estable) + ampliación de `RecipeDetailViewModelTest`. (RF-ER2, RF-ER3, RF-ER4, D3, D5)
- [ ] T3: bloque «Raciones a preparar» en `RecipeDetailScreen` (stepper con deshabilitados en 1 y 12, subtítulo, nota) + textos es/en con paridad + `testTag`s. (RF-ER1, RF-ER3, RF-ER6, D1, D4, S5)
- [ ] T4: `RecipeDetailE2ETest` ampliado: verify 1 (S1), decimales (S2), límites (S3), vuelta a base (S4), sin ingredientes (S5); regresión completa en verde. (S1-S5)
- [ ] T5: `docs/domain-model.md` (RD03 + redondeo) y `docs/risks-and-open-questions.md` (`Q-escalar-raciones-1`) actualizados.
- [ ] T6: `init`/`lint`/`unit`/`e2e` en verde y evidencia capturada (el muestreo visual lo ejecuta el validador).

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; no arranca procesos duraderos). Notas de entorno (`PROGRESS.md`): en PowerShell 5.1, `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& set PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%LOCALAPPDATA%\Android\Sdk\emulator;%PATH%&& set PKG=com.whatsforlunch.app&& powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`; el falso fallo de `pidof` tras un `installDebug` inmediato se resuelve reintentando.
- `./lint.ps1` → exit 0 (0 errores). `./unit.ps1` → exit 0; para forzar: `gradlew.bat testDebugUnitTest --rerun-tasks` (`unit.ps1` no reenvía argumentos; suite hoy **96 tests / 17 suites**). Deben entrar `ServingsScalingTest` y la ampliación de `RecipeDetailViewModelTest`, incluido el **verify 2** («el escalado no permite valores fuera de 1-12»: `setServings(0)` → 1, `setServings(13)` → 12 y fórmula sin efecto fuera de rango).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, emulador `Pixel_10_Pro` API 37; suite hoy **115 tests**) con `RecipeDetailE2ETest` ampliado:
  - verify 1 (S1): Receta base 4 con «200 g Harina» y «2 Huevos» → bajar a 2 muestra «100 g Harina» y «1 Huevos».
  - S2: «1 Huevo» base 4 → 3 raciones muestra «0,8» (es); S3: «−» deshabilitado en 1 y «+» deshabilitado en 12; S4: salir y volver → raciones base y cantidades base; S5: Borrador sin ingredientes → bloque visible y «Sin ingredientes».
  - Regresión completa en verde sin romper `ver-detalle-de-receta` (los 7 tests existentes), `crear-receta`, `editar-receta`, `mostrar-badge-de-coincidencia` ni el resto de la suite.
- Notas de entorno: `e2e.ps1` desinstala la app al terminar (reinstalar con `./gradlew installDebug`); suites largas pueden abortar (`INSTRUMENTATION_ABORTED`) → reintentar tras reiniciar el AVD; capturas en español con `cmd locale set-app-locales com.whatsforlunch.app --locales es-ES`; fotos de fixture como `RecipeDetailE2ETest.writePhoto()` (el picker de MediaStore falla: incidencia conocida).
- UI (solo el validador): capturas en es + `uiautomator` (textos, `testTag`, estado deshabilitado distinguible en 1 y 12); sin pipeline de screenshot testing en el repo (justificado por `afinar-fidelidad-visual-del-formulario`).
- Cobertura E2E: sí se añade/amplía, porque la feature cambia comportamiento visible del Detalle.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan. `./init.sh` ejecuta los checks no bloqueantes y no arranca nada duradero (regla del harness).

## Evidence To Capture

- `docs/validations/escalar-raciones.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y la verificación S1-S5 + verify 1/2; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/escalar-raciones/escalar-raciones-4-a-2-es.png` — Detalle con raciones en 2 y cantidades a la mitad (verify 1).
- `docs/evidence/escalar-raciones/escalar-raciones-limite-12-es.png` — límite superior con «+» deshabilitado y cantidad escalada (S3).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (selector de raciones 1-12) | RF-ER1, RF-ER3, RF-ER6 | S3, S5 | T3 | E2E + unit (clamp) |
| Step 2 (recalcular cantidades según RD03) | RF-ER2, RF-ER4, RF-ER5 | S1, S2, S4 | T1, T2 | unit + E2E |
| Verify 1 (E2E: 4 → 2 reduce a la mitad) | RF-ER2 | S1 | T4 | `RecipeDetailE2ETest` |
| Verify 2 (Unit: sin valores fuera de 1-12) | RF-ER3 | S3 | T1, T2 | `ServingsScalingTest` + `RecipeDetailViewModelTest` |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (solo cantidades visibles de ingredientes: sin persistencia, sin tocar el modelo, sin Modo Cocina, sin Lista de la Compra, sin badge ni métricas).
- [ ] Los escenarios de aceptación pasan (S1-S5) y las decisiones D1-D6 se respetan.
- [ ] RD03 se cumple con el redondeo acordado (`Q-escalar-raciones-1`, máximo 1 decimal) y los límites 1-12 (verify 2 en verde).
- [ ] Las marcas del checklist se conservan al cambiar raciones y el resto de la ficha (badge, «N de M», calorías, tiempos, «Raciones base») no cambia.
- [ ] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente ampliada y suite completa en verde (incluidos los 7 tests existentes de `RecipeDetailE2ETest`).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; `docs/domain-model.md` (RD03 + redondeo) y `docs/risks-and-open-questions.md` (`Q-escalar-raciones-1`) al día.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular: sin «Añadir a la lista», sin pantalla de Modo Cocina definitiva y sin cambios en el formulario).
