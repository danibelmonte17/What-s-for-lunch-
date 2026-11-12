# Feature Implementation Spec: Sugerir recetas con IA

## Source Feature

- `id`: sugerir-recetas-con-ia
- `area`: funcional
- `depends_on`: mostrar-recetas-compatibles (`accepted`), configurar-acceso-a-ia (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

El usuario pide a la IA **Sugerencias** de recetas nuevas hechas con lo que tiene en casa. Desde el botón **«Sugerir receta con IA»** del Escáner (RF-MRC8) se abre la pantalla real de Sugerencias —que sustituye al destino provisional `ai_suggestions_provisional`— y **el usuario elige qué Alimentos quiere enviar** (chips del Escaneo sin guardar y Alimentos de la Despensa persistida, preseleccionados). Al confirmar, la app envía esa selección a DevExpert Inference (`POST /chat/completions`, alias `chat`) pidiendo **hasta 3 recetas completas** en un JSON compacto estructurado (para minimizar tokens de salida) y lista las propuestas como tarjetas con su **Coincidencia calculada en el cliente** (RD01/RD10, `calculateMatch`) y el detalle de ingredientes y pasos.

Solo se pide y se muestra: aceptar o descartar una Sugerencia es `aceptar-sugerencia-de-receta`. Las Sugerencias viven en memoria (sin Room) y, sin API key, la pantalla pide configurarla en Ajustes (S05, `configurar-acceso-a-ia`).

## Non-Goals

- **Aceptar/descartar Sugerencias** (`aceptar-sugerencia-de-receta`): la tarjeta es de solo lectura y su toque es inerte; no hay estados `aceptada`/`descartada`, ni creación de Recetas, ni `origen = Sugerencia` (RD07) ni regla de «no aceptar dos veces».
- **Degradación RD05 completa** (`degradar-ante-fallo-de-ia`): aquí solo hay aviso mínimo de fallo con «Reintentar»; la entrada manual sin bloquear vive en el Escáner y se endurece en esa feature.
- **Persistencia de Sugerencias**: sin tablas nuevas, sin migración de Room y sin historial entre sesiones (D4).
- **Fotos o imágenes de las Sugerencias**: no se llama a `/images/generations`; la propuesta no lleva foto.
- **Pinche, Modo Cocina, Lista de la Compra y faltantes**: nada de esto se toca.
- **Cambios en `calculateMatch`, `MatchBadge`, Recetas Compatibles, RD13, Room ni en el esquema**.
- **Verificación de «sin retención» del proveedor (RK03)**: tarea humana que queda como puerta de release documentada (D6), no como código.
- **Medición de latencia/coste** de DevExpert Inference (tarea aplazada por el usuario) y **reintentos automáticos** de la llamada: una sola llamada por «Pedir sugerencias».

## Job Story

Cuando tengo la Despensa llena (o acabo de escanear) y no sé qué cocinar,
quiero pedirle a la IA recetas nuevas con lo que tengo, eligiendo yo qué alimentos usar,
para ver propuestas con su Coincidencia y decidir cuál me apetece.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P7`, `P31`): pide Sugerencias sobre sus propios Alimentos. Requiere su API key de IA (`⚠ S05`): con key de build (`D7-D11`) o de Ajustes; sin ninguna, la pantalla la pide configurar. Sin permisos Android nuevos (sin cámara, sin micrófono) y sin red salvo la llamada a DevExpert Inference.

## Requisitos funcionales

Prefijo propio `RF-SRC#` para no colisionar con los `RF-MRC#` de `mostrar-recetas-compatibles.md`.

- **RF-SRC1** (D1): el botón «Sugerir receta con IA» del Escáner (RF-MRC8) abre la pantalla real **Sugerencias** (ruta `ai_suggestions`), que sustituye al destino provisional; `ProvisionalScreen` deja de usarse para esto.
- **RF-SRC2** (D3): la pantalla abre en el **paso de selección**: lista de los Alimentos disponibles —chips del Escaneo sin guardar y Alimentos de la Despensa persistida, **un ítem por nombre normalizado**—, cada uno con checkbox, nombre y cantidad, **todos preseleccionados**; se necesita **al menos 1 seleccionado** para habilitar «Pedir sugerencias».
- **RF-SRC3** (D1, D9): al confirmar se envía la selección (nombre + cantidad) a `POST /chat/completions` (alias `chat`) con `response_format: {"type": "json_object"}`, `temperature` 0 y un prompt en el idioma de la app que pide **hasta 3 recetas completas** en el JSON compacto de D1, priorizando los alimentos enviados. Una sola llamada por confirmación.
- **RF-SRC4** (D8): el parseo tolerante y la normalización descartan entradas inválidas sin tumbar la lista; un JSON ilegible o un error de red/HTTP → **fallo** con aviso y «Reintentar» (RD05 mínimo).
- **RF-SRC5** (D2): cada Sugerencia se lista con su **Coincidencia calculada en el cliente** con `calculateMatch` (RD01, RD10) contra los Alimentos seleccionados y el badge compartido `MatchBadge` («100% Match» / «N%», RD02); sin obligatorios → sin badge.
- **RF-SRC6** (D5): la tarjeta muestra título, badge, «X de Y ingredientes disponibles» y el detalle de la propuesta (ingredientes con cantidad/unidad y pasos numerados con temporizador opcional); **tocar la tarjeta no hace nada**.
- **RF-SRC7**: sin API key, tras «Pedir sugerencias» aparece el aviso «Configura tu API key en Ajustes…» con acción **«Abrir Ajustes»** hacia `settings` (S05).
- **RF-SRC8**: estados de **selección, carga, resultados, vacío y error**; la pantalla nunca se congela y el progreso es explícito (`DESIGN.md`, «Estados a cubrir»).
- **RF-SRC9** (P19): todos los textos nuevos existen en `values/` (es) y `values-en/` (en); el texto de la propuesta (título, historia, pasos) se pide en el idioma de la app (D9) y los campos de enumeración usan tokens fijos de máquina (D1).

## Acceptance Scenarios

### Scenario 1: La IA propone recetas y se listan con su Coincidencia (verify 1 de `feature_list.json`)

Given la Despensa con «Manzana» (3) y «Nata» (1) y el cliente de IA simulado que devuelve `{"s":[{"t":"Tarta de manzana", …2 ingredientes obligatorios, 1 de ellos en la selección…}]}` al estilo del JSON de D1
When el usuario abre «Sugerir receta con IA», confirma la selección y pulsa «Pedir sugerencias»
Then aparece la tarjeta «Tarta de manzana» con su badge de Coincidencia (RD01 sobre sus obligatorios), la línea «X de Y ingredientes disponibles» y sus ingredientes y pasos (RF-SRC3, RF-SRC5, RF-SRC6)

### Scenario 2: Sin API key se pide configurarla en Ajustes (verify 2 de `feature_list.json`)

Given la app sin API key (ni de build ni en Ajustes) y el cliente de IA simulado devolviendo `MissingApiKey`
When el usuario confirma la selección y pulsa «Pedir sugerencias»
Then aparece el aviso «Configura tu API key en Ajustes…» con la acción «Abrir Ajustes»; al pulsarla se navega a `settings` (RF-SRC7)

### Scenario 3: Con cero seleccionados no se pide

Given el paso de selección con todos los Alimentos deseleccionados
When el usuario mira la pantalla
Then «Pedir sugerencias» está deshabilitado y no navega (RF-SRC2)

### Scenario 4: Fallo de red o del proveedor

Given la selección confirmada y el cliente de IA simulado en modo fallo (`Failed`)
When se pulsa «Pedir sugerencias»
Then aparece el aviso de fallo con «Reintentar»; un segundo intento con éxito lista las propuestas sin relanzar la pantalla (RF-SRC4)

### Scenario 5: Propuestas inválidas se descartan

Given el cliente simulado devuelve 3 propuestas, una con título vacío y otra sin pasos
When termina el parseo
Then solo se lista la propuesta válida; si ninguna es válida, se muestra el estado vacío con «Reintentar» (RF-SRC4, D8)

### Scenario 6: Textos en inglés

When el idioma del sistema es inglés
Then los textos nuevos (selección, botones, estados, avisos y cabeceras de la tarjeta) se muestran en inglés (P19, RF-SRC9)

### Scenario 7: El flujo completo desde el Escáner

Given el Escáner con los chips «Chicle» y «Mint» sin guardar y la Despensa con «Tomate»
When el usuario pulsa «Sugerir receta con IA»
Then se abre la pantalla de Sugerencias con «Chicle», «Mint» y «Tomate» en la selección, todos preseleccionados (RF-SRC2); al volver con atrás, el Escáner sigue como estaba (chips sin guardar intactos)

### Scenario 8: Los opcionales no alteran la Coincidencia

Given una propuesta con 2 obligatorios (1 en la selección) y 1 opcional que no está
When se calcula su badge
Then muestra «50%» (1 de 2 obligatorios); el opcional no cuenta (RD10)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Chips del Escaneo sin guardar y Despensa vacía | El paso de selección lista los chips (RF-SRC2, RF-MRC8); la app no exige guardarlos antes |
| Nombre repetido entre chip y Despensa | Un solo ítem por nombre normalizado (D3) |
| 0 Alimentos disponibles | No llega a abrirse: el botón del Escáner está deshabilitado (RF-MRC8); si aun así se abre, selección vacía y botón deshabilitado (S3) |
| JSON ilegible o cuerpo nulo | Aviso de fallo + «Reintentar» (S4); nunca excepción a la UI |
| Propuesta con título vacío, sin ingredientes o sin pasos tras normalizar | Se descarta (S5, D8) |
| Dos propuestas con el mismo título normalizado | Se conserva la primera |
| Más de 3 propuestas válidas | Solo se listan 3 (D7) |
| Cantidad de ingrediente no numérica o ≤ 0 | Queda en `1` (el ingrediente nunca se descarta por su cantidad) |
| Momento/dificultad con token desconocido | `LUNCH` / `MEDIUM` (D8) |
| La IA devuelve 0 propuestas válidas | Estado vacío con «Reintentar» (S5) |
| Rotación de pantalla | La selección y los resultados se conservan en memoria del destino (D4) |
| Salir con atrás | Las Sugerencias se pierden (D4, sin persistencia); el Escáner queda intacto |
| Proveedor lento | Estado de carga con progreso; la app nunca se congela (RF-SRC8) |

## Repository Research

### Files Inspected

- `feature_list.json` — `steps`, `verification` y `notes` de `sugerir-recetas-con-ia`; dependencias `accepted`.
- `docs/domain-model.md` — entidad **Sugerencia** (Coincidencia, estados, «se materializa como máximo en una Receta»), RD01/RD02/RD05/RD07/RD10 y validaciones de datos.
- `docs/build-brief.md` — «Sugerencias de recetas por IA — la IA propone Recetas compatibles con lo escaneado»; restricciones RGPD y `⚠ S05`.
- `docs/risks-and-open-questions.md` — `RK03` (tarea «Configuración efectiva de “sin retención”» marcada «antes de `sugerir-recetas-con-ia`»), `RK01`, `S05`; patrón `Q-…`.
- `docs/technical-discovery.md` — DevExpert Inference (`POST /v1/chat/completions` alias `chat`, `POST /v1/images/generations` alias `image`), `D7-D11` (`AI_BASE_URL`/`AI_API_KEY` en `BuildConfig`), comandos y scripts.
- `DESIGN.md` — «Estados a cubrir» (carga/error/vacío), tokens «Warm Culinary Craft», accesibilidad ≥ 48 dp, «IA discreta».
- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanImageAnalyzer.kt` — puerto con `sealed interface ScanAnalysisResult { Success | MissingApiKey | Failed }` (patrón a imitar).
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleScanImageAnalyzer.kt` — payload de `/chat/completions`, prompts `SPANISH_PROMPT`/`ENGLISH_PROMPT`, `temperature = 0`, tolerancia a `content` como texto o partes.
- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanAnalysisParser.kt` — parseo tolerante del primer objeto JSON (vallas ```` ```json ```` y texto alrededor).
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiEndpoints.kt` — `chatCompletions(baseUrl)`; base por defecto `https://inference.devexpert.io/v1`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyProvider.kt` / `PriorityAiKeyProvider.kt` — `apiKey(): String?` (prioridad key de build).
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeIngredient.kt`, `RecipeStep.kt`, `MealMoment.kt`, `Difficulty.kt` (`EASY|MEDIUM|HARD`), `RecipeOrigin.kt` (`USER|SUGGESTION`), `ScanDraftItem` — modelos que la propuesta replica (D1).
- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` — `calculateMatch(recipe, pantryItems): RecipeMatch?` (reutilizar sin tocar, RD01/RD10).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — `ScannerUiState.items: List<ScanDraftItem>`, `ScannerNotice`, mapeo `MissingApiKey → AI_KEY_MISSING` / `Failed → ANALYSIS_FAILED`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — `CompatibleRecipesSection`, botón `COMPATIBLE_SUGGEST_AI` y `ScannerTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — `onSuggestWithAi: () -> Unit` y `language` de la configuración.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes.AI_SUGGESTIONS_PROVISIONAL` («sugerir-recetas-con-ia lo sustituirá», comentario literal) y `AppRoutes.settings`/`SETTINGS`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — `ProvisionalTestTags.AI_SUGGESTIONS_SCREEN`.
- `app/src/main/java/com/whatsforlunch/app/ui/components/MatchBadge.kt` — badge compartido con testTag por pantalla.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin: `single { OkHttpClient() }`, `AiKeyProvider`, `ScanImageAnalyzer`, `viewModel { … }` (`RecipeDetailViewModel` recibe `parameters`).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — única clave previa `ai_suggestions_title`; claves `scanner_*` y patrón es/en.
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — los tests del destino provisional montan `ProvisionalScreen` localmente (habrá que actualizarlos al destino real).
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — patrón `FakeScanImageAnalyzer`, `InMemoryPantryRepository`/`InMemoryRecipeRepository` y montaje local de pantallas con navegación simulada por flags.
- `app/src/test/java/com/whatsforlunch/app/data/ai/ScanAnalysisParserTest.kt`, `ui/scanner/ScannerViewModelTest.kt` — patrones de test unitario (`runTest`, `UnconfinedTestDispatcher`, fakes del puerto).
- `init.ps1`/`init.sh`, `lint.*`, `unit.*`, `e2e.*` — comandos persistentes (`e2e` = `connectedDebugAndroidTest`).

### Existing Patterns To Follow

- Puertos de IA en `data/ai/` con `sealed interface` `Success | MissingApiKey | Failed` y clientes `OpenAiCompatible*` construidos con `OkHttpClient`, `AiKeyProvider` y `baseUrl`.
- Parseo tolerante en `internal object` aparte (`ScanAnalysisParser`) con unit tests propios.
- MVVM + StateFlow; las reglas de negocio viven en `domain/` con comentarios que citan `RD##`; Koin registra puertos y `viewModel`.
- UI en `ui/<feature>/` con `XxxRoute` (Koin + efectos) y `XxxScreen` stateless; objetos `XxxTestTags` en snake_case prefijados por pantalla y helpers `prefix_$id`.
- E2E Compose sin `AppNavHost`: pantallas locales + navegación simulada por flags + fakes del puerto y repos in-memory; localización con `createConfigurationContext`.
- Estados de carga/error/vacío en toda pantalla nueva (`DESIGN.md`); objetivos táctiles ≥ 48 dp.

### Current Gaps

- No existe ningún puerto ni cliente de **sugerencias de recetas** (`data/ai/` solo tiene escaneo e imagen): se crea de cero.
- No hay modelo de dominio para la propuesta de la Sugerencia ni su normalización: se crea en `domain/suggestion/`.
- El destino de sugerencias es un `ProvisionalScreen`: hay que crear la pantalla real y re-cablear `AppRoutes` y `CompatibleRecipesE2ETest`.
- Los chips del Escaneo viven solo en `ScannerViewModel` y no son accesibles desde otro destino: hace falta un compartidor de estado en memoria (D3).
- No hay `MockWebServer` ni tests de cliente HTTP (convención del repo: se testea el puerto con fakes; L4 de `configurar-acceso-a-ia`).
- Sin pantalla de Sugerencias en `uxdocs/`: laguna de diseño (RK04) resuelta en esta spec con los tokens existentes.

## Technical Approach

- **Dominio** — `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt`:

  ```kotlin
  // Propuesta de la IA con forma de Receta (D1); aún no es una Receta (RD07 se aplica al aceptar).
  data class RecipeSuggestion(
      val title: String, val story: String,
      val mealMoment: MealMoment, val prepMinutes: Int, val cookMinutes: Int,
      val difficulty: Difficulty, val baseServings: Int, val caloriesPerServing: Int?,
      val ingredients: List<RecipeIngredient>, val steps: List<RecipeStep>,
  )
  data class SuggestionWithMatch(val suggestion: RecipeSuggestion, val match: RecipeMatch?)
  ```

  y `domain/suggestion/SuggestionRules.kt` con la normalización (D8) y `suggestionMatches(suggestions, availableFoods): List<SuggestionWithMatch>`, que calcula la Coincidencia con `calculateMatch` (RD01/RD10) **sin tocar `RecipeMatch.kt`**: la propuesta se adapta a un `Recipe` transitorio y los Alimentos disponibles a `PantryItem` (solo importa el nombre normalizado). Orden de la lista: el de la respuesta de la IA (D7 recorta a 3).
- **Puerto de IA** — `data/ai/RecipeSuggester.kt` (mismo patrón que `ScanImageAnalyzer`):

  ```kotlin
  sealed interface RecipeSuggestionsResult {
      class Success(val suggestions: List<RecipeSuggestion>) : RecipeSuggestionsResult
      data object MissingApiKey : RecipeSuggestionsResult
      data object Failed : RecipeSuggestionsResult
  }
  interface RecipeSuggester {
      suspend fun suggest(foods: List<SuggestableFood>, language: String): RecipeSuggestionsResult
  }
  ```

  `SuggestableFood(name, quantity)` refleja la selección del usuario. `OpenAiCompatibleRecipeSuggester` hace `POST <base>/chat/completions` con `Authorization: Bearer`, `model = "chat"` (alias estable), `temperature = 0`, `response_format = {"type": "json_object"}` y `messages[0].content` solo texto (prompt). Sin key → `MissingApiKey`; imagen de entrada vacía (0 alimentos), HTTP ≠ 2xx, cuerpo nulo o JSON ilegible → `Failed` (igual que `OpenAiCompatibleScanImageAnalyzer`); `CancellationException` se repropaga; sin reintentos.
- **Parser** — `data/ai/RecipeSuggestionParser.kt` (`internal object`, estilo `ScanAnalysisParser`): extrae el primer objeto JSON (tolerando vallas y texto alrededor) y lee `s` como array de propuestas en el formato compacto de D1. Entradas malformadas se descartan individualmente en la normalización de dominio; si no hay objeto JSON → `Failed`.
- **Prompt** (D1/D9): en el idioma de la app (`SPANISH_PROMPT`/`ENGLISH_PROMPT`, elegido como en `OpenAiCompatibleScanImageAnalyzer`), lista los alimentos seleccionados como `nombre (cantidad)` y pide **hasta 3 recetas** que los aprovechen, devolviendo **SOLO** el JSON compacto con la forma exacta:

  ```json
  {"s":[{"t":"Tarta de manzana","st":"Historia","mm":"LUNCH","pm":10,"cm":20,"df":"EASY","bs":4,"kcal":320,"i":[{"n":"Manzana","q":3,"u":"ud","o":false}],"p":[{"t":"Pela las manzanas…","m":10}]}]}
  ```

  Claves cortas para minimizar tokens de salida: `t` título, `st` historia, `mm` momento (`BREAKFAST|LUNCH|DINNER|DESSERT`), `pm`/`cm` minutos de preparación/cocción, `df` dificultad (`EASY|MEDIUM|HARD`), `bs` Raciones base, `kcal` calorías por porción, `i` ingredientes (`n` nombre, `q` cantidad, `u` unidad, `o` opcional), `p` pasos (`t` texto, `m` temporizador en minutos).
- **Estado compartido del Escaneo** (D3) — `data/scanner/ScanDraftHolder.kt`: contenedor en memoria (`StateFlow<List<ScanDraftItem>>`) que da acceso a los chips sin guardar desde dos destinos. `ScannerViewModel` lo recibe como parámetro con **valor por defecto** (`ScanDraftHolder()`) para no romper los ~12 call sites de tests, y `AppModule` le inyecta el singleton que también usa `AiSuggestionsViewModel`.
- **ViewModel** — `ui/aisuggestions/AiSuggestionsViewModel(pantryRepository, scanDraftHolder, recipeSuggester)`: `uiState: StateFlow<AiSuggestionsUiState>` con fases `Selection(foods, selected, canRequest)`, `Loading`, `Results(items: List<SuggestionWithMatch>)`, `Empty` y `notice: AiSuggestionsNotice?` (`AI_KEY_MISSING`, `FAILED`) siguiendo el patrón enum + banner descartable de `ScannerNotice`. La selección inicial se arma en `init` uniendo chips + Despensa por nombre normalizado (D3); `toggleFood(name)`, `requestSuggestions(language)` (una llamada, mapea `MissingApiKey`/`Failed` a avisos y éxito vacío a `Empty`) y `retry()`.
- **UI** — `ui/aisuggestions/AiSuggestionsScreen` (stateless) + `AiSuggestionsRoute` (Koin + `configuration.locales[0].language`):
  - **Selección**: cabecera `ai_suggestions_selection_title`, filas con checkbox (preseleccionados), nombre y cantidad, y botón primario «Pedir sugerencias» (habilitado con ≥ 1; `testTag` `ai_suggestions_request`).
  - **Carga**: progreso «Pidiendo sugerencias a la IA…» (`ai_suggestions_progress`), sin interacción y sin congelar la app.
  - **Resultados**: lista de tarjetas `SuggestionCard` (`Surface` `surfaceContainerLowest`, `WarmRadius.lg`): título (`titleSmall`), `MatchBadge` con testTag propio, línea «X de Y ingredientes disponibles», cabecera «Ingredientes» con filas «cantidad unidad nombre» (el opcional se marca) y cabecera «Pasos» con numeración y chip de temporizador cuando lo hay. Sin foto (placeholder no procede). Toque inerte (D5).
  - **Vacío** y **error**: mensajes `ai_suggestions_empty` / `ai_suggestions_failed` con «Reintentar»; sin key: aviso `ai_suggestions_key_missing` + «Abrir Ajustes».
  - Objetivos táctiles ≥ 48 dp; descripciones accesibles por tarjeta y por fila de selección.
- **Navegación** — `AppNavHost`: `AppRoutes.AI_SUGGESTIONS = "ai_suggestions"` (se retira `AI_SUGGESTIONS_PROVISIONAL` y su `ProvisionalScreen`); `ScannerRoute.onSuggestWithAi` navega a `ai_suggestions`; la acción «Abrir Ajustes» navega a `AppRoutes.SETTINGS`.
- **Textos** (`values/strings.xml` + `values-en/strings.xml`): se conserva `ai_suggestions_title` y se añaden `ai_suggestions_selection_title`, `ai_suggestions_request`, `ai_suggestions_progress`, `ai_suggestions_empty`, `ai_suggestions_failed`, `ai_suggestions_retry`, `ai_suggestions_key_missing`, `ai_suggestions_open_settings`, `ai_suggestions_card_ingredients` («%1$d de %2$d ingredientes disponibles» / «%1$d of %2$d ingredients available»), `ai_suggestions_card_accessibility`, `ai_suggestions_selection_item_accessibility`, `ai_suggestions_ingredients_header`, `ai_suggestions_steps_header` (es/en, P19).
- **Tests** — unit: `RecipeSuggestionParserTest` (JSON compacto, vallas, tipos, `s` ausente), `SuggestionRulesTest` (normalización D8: títulos vacíos, cantidades inválidas → 1, tokens de momento/dificultad desconocidos, duplicados por título, recorte a 3, RD10 con opcionales) y `AiSuggestionsViewModelTest` (selección mínima, `MissingApiKey`, `Failed` + `retry`, éxito → tarjetas con Coincidencia, éxito vacío → `Empty`). E2E `AiSuggestionsE2ETest` con `FakeRecipeSuggester` (patrón de `ScanPantryE2ETest`) montando `AiSuggestionsScreen`/`ScannerScreen` con navegación simulada.

### Decisiones (para no reabrir el debate en implementación)

- **D1 — Receta completa en JSON compacto estructurado** (confirmado por el usuario, 2026-10-23): la IA devuelve recetas completas (título, historia, momento, tiempos, dificultad, Raciones, calorías, ingredientes con cantidad/unidad/opcional y pasos con temporizador) en un JSON de claves cortas con `response_format: json_object` para minimizar los tokens de salida. Ver forma exacta arriba.
- **D2 — Coincidencia en el cliente con RD01** (confirmado por el usuario, 2026-10-23): `calculateMatch` sobre los obligatorios de la propuesta (flags `o`, defecto obligatorio; RD10). Matiz del planner: la comparación es contra **los Alimentos seleccionados** que se envían (Escaneo + Despensa), no solo la Despensa persistida; si solo contara esta, las sugerencias pedidas con chips sin guardar mostrarían «0 %» aunque la propuesta use esos alimentos.
- **D3 — el usuario elige qué alimentos enviar** (confirmado por el usuario, 2026-10-23): paso de selección previo con checkboxes, un ítem por nombre normalizado (chip y Despensa se fusionan), cantidad visible, **todos preseleccionados**, mínimo 1 y sin alta libre de texto. Sub-decisiones del planner: incluye también los chips del Escaneo sin guardar (el botón del Escáner se habilita con ellos, RF-MRC8) y el envío lleva `nombre (cantidad)`.
- **D4 — solo en memoria** (confirmado por el usuario, 2026-10-23): sin Room ni migración; la selección y las Sugerencias viven mientras dura el destino. «Una Sugerencia no se acepta dos veces» (dominio) lo resuelve `aceptar-sugerencia-de-receta`.
- **D5 — tarjeta con resumen y toque inerte** (confirmado por el usuario, 2026-10-23): la propuesta se lee completa en la tarjeta; tocarla no navega ni acciona nada.
- **D6 — RK03 como puerta de release** (confirmado por el usuario, 2026-10-23): la verificación efectiva de «sin retención» en el panel de DevExpert Inference queda como gate humano antes de distribuir; esta feature no cambia el aviso de privacidad (el copy «sin retención» sin verificar es el L3 de `escanear-despensa-con-ia`).
- **D7 — hasta 3 Sugerencias**: el prompt pide 3 y la lista recorta a 3; sin paginación.
- **D8 — normalización de la respuesta**: título vacío → propuesta descartada; sin ingredientes o sin pasos tras limpiar → descartada; duplicado por título normalizado → se conserva la primera; cantidad de ingrediente no numérica o ≤ 0 → `1` (el ingrediente nunca se descarta por su cantidad); unidad → texto recortado (puede ser vacío); `o` distinto de booleano → `false`; `mm`/`df` desconocidos → `LUNCH`/`MEDIUM`; `pm`/`cm` no numéricos o < 0 → `0`; `bs` fuera de 1..12 → `4` (RD03); `kcal` no numérica o ≤ 0 → `null`; `m` de paso no numérico o < 0 → `null`.
- **D9 — idioma**: prompt y textos de la propuesta en el idioma de la app (como `OpenAiCompatibleScanImageAnalyzer`); los campos de enumeración viajan como tokens fijos de máquina (`BREAKFAST`…, `EASY`…).
- **D10 — una sola llamada por «Pedir sugerencias»**: sin reintentos automáticos ni caché; «Reintentar» es una acción del usuario.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/RecipeSuggestion.kt` — crear; modelo de propuesta y `SuggestionWithMatch`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` — crear; normalización (D8) y Coincidencia (D2, reutilizando `calculateMatch`).
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt` — crear; puerto + `RecipeSuggestionsResult` + `SuggestableFood`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParser.kt` — crear; parseo tolerante del JSON compacto.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleRecipeSuggester.kt` — crear; cliente `/chat/completions` (alias `chat`, `response_format`).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanDraftHolder.kt` — crear; chips del Escaneo compartidos en memoria (D3).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — modificar; `items` delegados al holder (parámetro con valor por defecto; comportamiento intacto).
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt`, `AiSuggestionsScreen.kt`, `AiSuggestionsRoute.kt` — crear; estados, UI y wiring.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `ScanDraftHolder` singleton, `RecipeSuggester`, `AiSuggestionsViewModel`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; ruta `ai_suggestions` y retirada del destino provisional de sugerencias.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/ProvisionalScreen.kt` — modificar; retirar `ProvisionalTestTags.AI_SUGGESTIONS_SCREEN` al actualizar su único uso de test.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; 13 claves nuevas (más `ai_suggestions_title`, existente).
- `app/src/test/java/com/whatsforlunch/app/data/ai/RecipeSuggestionParserTest.kt` — crear.
- `app/src/test/java/com/whatsforlunch/app/domain/suggestion/SuggestionRulesTest.kt` — crear.
- `app/src/test/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModelTest.kt` — crear.
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — crear; verify 1 y 2 de `feature_list.json` + S3-S8.
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — modificar; los2 tests del destino provisional pasan a montar la pantalla real de Sugerencias (o a asertar solo el callback) y se actualizan sus nombres.
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`, `docs/progress-history.md` — modificar al implementar (ver Durable Documentation Impact).
- `docs/validations/sugerir-recetas-con-ia.md` y `docs/evidence/sugerir-recetas-con-ia/*.png` — crear al validar (evidencia).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`). **No hay prototipo de Sugerencias en `uxdocs/`** (solo se atisba «Sugerencias:» en la lista de la compra): laguna RK04 resuelta aquí con los tokens y componentes existentes, para que el implementer no invente estilo.
- Screens or states affected: nueva pantalla **Sugerencias** (selección → carga → resultados → vacío/error) alcanzada desde el botón del Escáner; el Escáner solo cambia el destino de su callback (sin cambios visibles en él).
- New design artifact required: no — se usan tokens, `MatchBadge` y patrones de tarjeta existentes.

Especificación verificable por `uiautomator` y captura (sin exigir muestreo de píxeles):

- **Selección**: cabecera `titleMedium` + filas de `Surface` con `Checkbox` (48 dp), nombre (`bodyMedium`) y cantidad (`labelSmall`, `onSurfaceVariant`); botón primario «Pedir sugerencias» (altura mínima 52 dp), deshabilitado con 0 seleccionados.
- **Tarjeta de Sugerencia**: `Surface` `surfaceContainerLowest`, `WarmRadius.lg`, `WarmSpacing.md`; título `titleSmall` (2 líneas máx.), `MatchBadge` (100 % → «100% Match» sobre `secondary`; resto «N%» sobre `secondaryContainer`; sin obligatorios → sin badge), línea `labelSmall` «X de Y ingredientes disponibles», lista de ingredientes «cantidad unidad nombre» («(opcional)» en los opcionales) y pasos numerados con chip de temporizador; sin foto ni icono decorativo.
- **Estados**: progreso con texto; vacío y error con texto `bodySmall` + botón «Reintentar»; aviso de key con texto + botón «Abrir Ajustes»; avisos fuera del contenido principal (patrón del Escáner).
- `testTags` nuevos en `AiSuggestionsTestTags`: `SCREEN`, `SELECTION_LIST`, `selectionItem(name)`, `selectionToggle(name)`, `REQUEST`, `PROGRESS`, `RESULT_LIST`, `suggestionCard(index)`, `SUGGESTION_TITLE`, `suggestionMatch(index)`, `SUGGESTION_INGREDIENTS`, `SUGGESTION_STEPS`, `EMPTY`, `NOTICE`, `RETRY`, `OPEN_SETTINGS`.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (un puerto más en `data/ai/`, UI en `ui/aisuggestions/`, dentro de la convención).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs:
  - `docs/domain-model.md` — **update**: refinar la entidad **Sugerencia** (se pide con Alimentos seleccionados; receta completa; se muestra con su Coincidencia RD01; solo en memoria) y añadir a «Validaciones de datos»/«Casos límite» las reglas de normalización D8.
  - `docs/technical-discovery.md` — **update**: nuevo cliente `OpenAiCompatibleRecipeSuggester` (`POST /chat/completions`, alias `chat`, `response_format: json_object`, JSON compacto) en «Integraciones externas» y su consumo en «Flujos principales».
  - `docs/risks-and-open-questions.md` — **update**: registrar `Q-sugerir-recetas-con-ia-1..6` **resueltas por el usuario el 2026-10-23** (contenido de la Sugerencia, Coincidencia en el cliente, elección de alimentos enviados, solo en memoria, tarjeta con toque inerte y RK03 como puerta de release) y actualizar la tarea «Configuración efectiva de “sin retención”» de `RK03` de «antes de `sugerir-recetas-con-ia`» a **puerta de release antes de distribuir** (D6).
  - `PROGRESS.md` / `docs/progress-history.md` — **update** al cerrar la sesión (estado y siguiente paso; entrada de historial).

## Implementation Plan

1. Crear el dominio (`RecipeSuggestion`, `SuggestionRules`) con sus unit tests de normalización y Coincidencia (D2, D8).
2. Crear el puerto `RecipeSuggester`, el parser y el cliente `OpenAiCompatibleRecipeSuggester` con su unit test de parser.
3. Crear `ScanDraftHolder` y adaptar `ScannerViewModel` (sin cambios de comportamiento) y `AppModule`.
4. Implementar `AiSuggestionsViewModel` con sus unit tests (fases, avisos y retry).
5. Implementar `AiSuggestionsScreen`/`AiSuggestionsRoute`, textos es/en, testTags y la navegación (`ai_suggestions` sustituye al destino provisional).
6. Crear `AiSuggestionsE2ETest`, actualizar `CompatibleRecipesE2ETest`, dejar la regresión completa en verde y capturar la evidencia.
7. Actualizar `docs/domain-model.md`, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` (Q-… y RK03).

## Implementation Tasks

- [ ] T1: `domain/suggestion/RecipeSuggestion.kt` + `SuggestionRules.kt` + `SuggestionRulesTest` (D2, D8, RD01/RD10, recorte a 3). (RF-SRC4, RF-SRC5, S1, S5, S8)
- [ ] T2: `RecipeSuggester.kt` + `RecipeSuggestionParser.kt` + `OpenAiCompatibleRecipeSuggester.kt` + `RecipeSuggestionParserTest`. (RF-SRC3, D1)
- [ ] T3: `ScanDraftHolder.kt` + `ScannerViewModel` + `AppModule` (chips compartidos; suite del Escáner intacta). (RF-SRC2, D3, S7)
- [ ] T4: `AiSuggestionsViewModel` + `AiSuggestionsViewModelTest` (selección mínima, avisos, retry, éxito/vacío). (RF-SRC2, RF-SRC4, RF-SRC7, S2-S5)
- [ ] T5: `AiSuggestionsScreen`/`AiSuggestionsRoute` + textos es/en + `AiSuggestionsTestTags` + navegación `ai_suggestions` y «Abrir Ajustes». (RF-SRC1, RF-SRC6, RF-SRC8, RF-SRC9, S6, S7)
- [ ] T6: `AiSuggestionsE2ETest` (verify 1 y 2 + S3-S8) y `CompatibleRecipesE2ETest` actualizado al destino real, sin romper la suite.
- [ ] T7: gates en verde, docs actualizadas (dominio, technical-discovery, `Q-sugerir-recetas-con-ia-1..6`, RK03) y evidencia capturada.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate de arranque estándar no bloqueante; no arranca procesos duraderos; artefactos conocidos: carrera del `pidof` y exit 1 falso por el stderr de `adb shell monkey` — ejecutar con el wrapper `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& ..."` de `PROGRESS.md`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `RecipeSuggestionParserTest`, `SuggestionRulesTest`, `AiSuggestionsViewModelTest`, `ScannerViewModelTest` y `RecipeMatchTest` (regresión). Reejecución forzada: `gradlew.bat testDebugUnitTest --rerun-tasks` (`unit.ps1` no reenvía argumentos).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, AVD `Pixel_10_Pro` API 37) con `AiSuggestionsE2ETest`:
  - **verify 1 (S1)**: `FakeRecipeSuggester` devuelve el JSON de D1 con 2 propuestas → «Pedir sugerencias» lista las2 tarjetas con título, badge de Coincidencia (RD01 calculado en cliente), «X de Y ingredientes disponibles», ingredientes y pasos.
  - **verify 2 (S2)**: fake `MissingApiKey` → aviso «Configura tu API key en Ajustes…» con «Abrir Ajustes» que abre el destino `settings` (simulado con flag, patrón del repo).
  - S3: deseleccionar todos los alimentos → «Pedir sugerencias» deshabilitado.
  - S4: fake `Failed` → aviso + «Reintentar» → segundo intento en éxito lista resultados.
  - S5: propuestas inválidas descartadas (y estado vacío con «Reintentar» si no queda ninguna).
  - S6: en inglés cambian selección, botones, estados, avisos y cabeceras (patrón de `englishLabelsAndScannerActionsAreLocalized`).
  - S7: desde `ScannerScreen`, el botón `COMPATIBLE_SUGGEST_AI` abre la selección con los chips del Escaneo y los Alimentos de la Despensa preseleccionados; al volver, el Escáner conserva los chips.
  - S8 (unit + E2E): los opcionales de la propuesta no alteran la Coincidencia (RD10).
  - Regresión completa en verde (suite `connectedDebugAndroidTest`, hoy 115 pruebas con 2 omitidas de `SettingsE2ETest`) sin romper el Escáner, Recetas Compatibles, Detalle ni Ajustes.
- Cobertura E2E: **sí se añade** — la feature cambia comportamiento visible (destino real del botón) y añade un flujo con llamada simulada al proveedor.
- `init.sh` / `init.ps1` **no se modifican**: checks de arranque no bloqueantes, sin procesos duraderos; los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.
- **Sin llamadas reales a DevExpert Inference**: la verificación usa exclusivamente el cliente simulado; una llamada real es de pago y requiere confirmación humana previa (`AGENTS.md`). La llamada real queda como verificación manual opcional del validador, nunca automática.
- UI (solo el validador): capturas en español de la selección y de los resultados + `uiautomator`; sin screenshot testing en el repo (justificado por el precedente de `afinar-fidelidad-visual-del-formulario`).

## Evidence To Capture

- `docs/validations/sugerir-recetas-con-ia.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación por escenario S1-S8.
- `docs/evidence/sugerir-recetas-con-ia/sugerencias-seleccion-es.png` — paso de selección con chips + Despensa preseleccionados y «Pedir sugerencias» (es).
- `docs/evidence/sugerir-recetas-con-ia/sugerencias-resultados-es.png` — tarjetas de propuestas con badges de Coincidencia, ingredientes y pasos (es).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (enviar el contenido de la Despensa al proveedor compatible con OpenAI) | RF-SRC2, RF-SRC3 | S3, S7 | T2, T3, T4 | unit `AiSuggestionsViewModelTest` + E2E |
| Step 2 (mostrar las Sugerencias como recetas propuestas con su coincidencia) | RF-SRC5, RF-SRC6 | S1, S8 | T1, T5 | unit `SuggestionRulesTest` + E2E + captura |
| Verify 1 (E2E: cliente simulado → se listan recetas propuestas) | RF-SRC3, RF-SRC5 | S1 | T6 | `AiSuggestionsE2ETest` |
| Verify 2 (E2E: sin API key → se pide configurarla en Ajustes) | RF-SRC7 | S2 | T4, T6 | `AiSuggestionsE2ETest` |
| — (destino real del botón del Escáner) | RF-SRC1 | S7 | T5 | `AiSuggestionsE2ETest` + `CompatibleRecipesE2ETest` |
| — (JSON compacto que minimiza tokens) | RF-SRC3 | S1, S5 | T2 | unit `RecipeSuggestionParserTest` |
| — (fallo y reintentos, RD05 mínimo) | RF-SRC4, RF-SRC8 | S4, S5 | T4 | E2E |
| — (es/en) | RF-SRC9 | S6 | T5 | E2E de localización |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature.
- [ ] Los escenarios de aceptación pasan (S1-S8) y las decisiones D1-D10 se respetan (en particular D2: Coincidencia en el cliente contra los Alimentos seleccionados; D5: toque inerte).
- [ ] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente añadida (`AiSuggestionsE2ETest`) y suite completa en verde, sin romper el Escáner ni Recetas Compatibles.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente (solo `status` y `evidence` en `feature_list.json`).
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular: **sin aceptar/descartar Sugerencias ni `origen = Sugerencia`**, sin persistencia de Sugerencias ni migraciones de Room, sin `/images/generations`, sin Pinche/Lista de la Compra/Modo Cocina y sin tocar `calculateMatch`, `MatchBadge` ni RD13).
- [ ] Sin llamadas reales de pago a la IA en la verificación (solo cliente simulado) y sin API keys reales en ficheros, logs ni evidencia (`⚠ S05`, `RK01`).
- [ ] `docs/risks-and-open-questions.md` registra `Q-sugerir-recetas-con-ia-1..6` resueltas y la tarea de `RK03` actualizada a puerta de release (D6).

---

## Enmienda — `unificar-unidades` (2026-11-09)

Decisión posterior del usuario (D1–D5 de `docs/specs/unificar-unidades.md`): la unidad deja de ser texto libre y pasa a **lista cerrada (ud, g, kg, ml, l)**. Al normalizar la respuesta de la IA, `SuggestionRules.normalizeOne` normaliza la unidad al enum (**D2**: la vacía o desconocida queda en «ud»); el contrato JSON compacto **no cambia de claves** (solo cambia el resultado normalizado de `u`). Las tarjetas presentan la unidad con `unitLabel` (código → clave `unit_ud`…`unit_l` en es/en; legada → texto tal cual). Lo demás (`calculateMatch`, `MatchBadge`, RD13) queda intacto.

*Enmienda añadida al final por `unificar-unidades`, sin reescribir la historia ni las evidencias históricas de esta spec.*

---

## Enmienda — `redisenar-formulario-de-receta` (2026-11-09)

Decisiones posteriores del usuario (D3/D5/D8 de `docs/specs/redisenar-formulario-de-receta.md`): el contrato JSON de la IA **pierde «cm» (cocción) y «o» (opcionalidad)** —los parsers las ignoran si llegan como claves desconocidas—; las tarjetas dejan de mostrar la píldora «Opcional» y la **Coincidencia cuenta todos los Ingredientes** (RD10 enmendada: sin obligatorios/opcionales). El resto de esta spec (selección de Alimentos, contrato con `t/st/mm/pm/df/bs/kcal/i/p`, normalización, MatchBadge) queda intacto.

*Enmienda añadida al final por `redisenar-formulario-de-receta`, sin reescribir la historia ni las evidencias históricas de esta spec.*
