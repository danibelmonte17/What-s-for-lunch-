# Feature Implementation Spec: Resolver dudas con el Pinche

## Source Feature

- `id`: resolver-dudas-con-pinche
- `area`: funcional
- `depends_on`: configurar-acceso-a-ia (`accepted`), aceptar-sugerencia-de-receta (`accepted`)
- `status`: `not_started` (con spec)
- `source`: `feature_list.json`

> **Orden de trabajo (excepción aprobada por el usuario, 2026-10-26).** Por orden del backlog tocaría `degradar-ante-fallo-de-ia`; el usuario elige el Pinche. No se reordena `feature_list.json`: solo se actualizan `steps`, `verification` y `notes` de esta feature con permiso explícito, y la constancia queda en `PROGRESS.md`.

## Goal

El usuario abre el **Pinche** desde Mis Recetas y **conversa por texto** para resolver dudas de cocina («¿por qué se me corta la mayonesa?», «¿qué ceno con lo que tengo?») o pedir una receta concreta. El Pinche **no depende de la Despensa** (RD14): puede proponer platos con **Faltantes**. Para no volcar contexto en cada prompt, la app le expone **dos funciones de solo lectura** (`get_pantry` y `get_my_recipes`) y el modelo decide cuándo invocarlas (RD17); con esos datos responde **texto conversacional + como máximo una Propuesta del Pinche**, mostrada en una **Ficha** con ingredientes, pasos y Faltantes.

Desde la Ficha, «Guardar en Mis Recetas» crea un **Borrador** con `origen = Sugerencia` e id estable (RD16); guardar dos veces reescribe la misma Receta. La conversación y las Propuestas viven **solo en memoria**, la voz queda diferida y los Faltantes **solo se muestran**.

## Non-Goals

- **Voz y micrófono** (`⚠ S04`): sin `SpeechRecognizer`, sin permiso `RECORD_AUDIO`, sin dictado. Solo teclado.
- **Añadir Faltantes a la Lista de la Compra** (`anadir-faltantes-a-la-lista`): la Ficha no tiene acción de compra; la Lista no existe aún. Tampoco se descuentan Alimentos de la Despensa.
- **Persistencia**: sin Room, sin migración, sin historial entre sesiones, sin caché de respuestas (RD17). Al salir del Pinche se pierde la conversación.
- **Abrir la Propuesta en Detalle o Modo Cocina**: no hay navegación desde la Ficha; el Modo Cocina aún no existe.
- **Coincidencia, `MatchBadge`, RD01/RD02/RD10/RD13 y Sugerencias**: no se tocan ni se muestran badges en el Pinche (RD14).
- **Más de una receta por respuesta**, streaming, regeneración parcial, edición de mensajes, adjuntar fotos, respuestas en markdown, `/images/generations`, selección de modelo o de proveedor.
- **Funciones de escritura** o cualquier otra tool además de las dos de lectura (RD17).
- **Retención del proveedor (RK03)**: puerta humana de release; aquí solo se documenta el dato que sale del dispositivo.
- **Implementar sin verificar `S08`**: ver «Prerrequisito bloqueante».

## Job Story

Cuando estoy cocinando o planeando la comida y me surge una duda (o quiero una receta que no tengo),
quiero preguntárselo al Pinche por texto y recibir una respuesta con una receta completa si procede,
para resolverlo sin salir de la app y guardar la propuesta en Mis Recetas si me convence.

## Users And Permissions

- **Usuario** (único rol del MVP, `P7`, `P31`): conversa con sus propios datos. Requiere API key (`⚠ S05`): la de build o la de Ajustes; sin ninguna, el Pinche pide configurarla. Sin permisos Android nuevos y sin red salvo DevExpert Inference.

## Requisitos funcionales

Prefijo propio `RF-PCH#` (no colisiona con `RF-SRC#` de Sugerencias).

- **RF-PCH1**: acción de la top bar de **Mis Recetas** (icono de chat, `action_open_pinche`) abre la ruta `pinche`; vuelta atrás con la flecha y con el gesto del sistema.
- **RF-PCH2**: la pantalla muestra un saludo inicial (solo UI, no se envía al modelo), la lista de mensajes, tres chips de consulta rápida (solo con la conversación vacía) y el compositor de texto.
- **RF-PCH3**: enviar un mensaje añade la burbuja del usuario y hace **una tanda** al proveedor con la conversación (máximo los últimos 20 mensajes) y `tools` con `get_pantry` y `get_my_recipes`, `tool_choice: "auto"`, alias `chat`, `temperature` 0 y **sin `response_format`** (D5).
- **RF-PCH4**: la app ejecuta en el dispositivo las `tool_calls` que devuelva el modelo y le devuelve los resultados como mensajes `role = tool`, hasta un máximo de **3 llamadas HTTP por turno** (D6); si la tercera respuesta sigue pidiendo tools, el turno es `Failed`.
- **RF-PCH5**: `get_pantry` devuelve nombre y cantidad de la Despensa; `get_my_recipes` devuelve título, Momento culinario e ingredientes de Mis Recetas, **sin** pasos, foto, estado ni cantidades (Q-14, RD17). Topes: 100 Alimentos y 50 Recetas (con 20 nombres de ingredientes por Receta).
- **RF-PCH6**: la respuesta final se parte en texto conversacional y, si existe, **una** Propuesta del Pinche en el contrato JSON compacto de Sugerencias; se normaliza con `SuggestionRules` y se recorta a 1 (D7). Sin Propuesta válida, se muestra solo el texto.
- **RF-PCH7**: la Ficha muestra título, Momento, tiempos, Raciones, ingredientes (opcionales marcados), pasos numerados con temporizador y **Faltantes** (solo obligatorios ausentes por nombre normalizado, RD15/D8). Sin Faltantes, la sección no aparece. La Ficha **no** tiene acción de Lista de la Compra.
- **RF-PCH8**: «Guardar en Mis Recetas» guarda la Propuesta como **Borrador sin foto**, `origen = Sugerencia`, id `UUID.nameUUIDFromBytes("pinche:" + normalizeName(título))` y upsert (RD16); la Ficha pasa a «Guardada» deshabilitada y se permanece en el chat. Un segundo guardado no duplica.
- **RF-PCH9**: fallos visibles con acción: sin key → aviso + «Abrir Ajustes»; error de proveedor/red/parseo/topes → aviso + «Reintentar» (reenvía el último mensaje sin duplicar la burbuja); guardado fallido → aviso + botón de guardar reactivado.
- **RF-PCH10**: estados de conversación vacía, enviando (indicador de progreso y compositor deshabilitado), respuesta, error y sin key; la app nunca se congela (`DESIGN.md`).
- **RF-PCH11** (`P19`): textos nuevos en `values/` (es) y `values-en/` (en); prompt y respuesta en el idioma de la app, con tokens de máquina para `mm`/`df`.

## Acceptance Scenarios

### Scenario 1: Duda de cocina respondida (verify 1)

Given la Despensa con «Tomate» (3) y «Arroz» (1), una Receta «Arroz con tomate» y el cliente simulado que responde texto sin Propuesta
When el usuario envía «¿Cómo evito que el arroz se pegue?»
Then aparece su burbuja, el progreso, y la respuesta del Pinche en una burbuja de asistente; no se muestra Ficha (RF-PCH4, RF-PCH6)

### Scenario 2: El modelo consulta la Despensa antes de proponer (verify 1)

Given el cliente simulado que responde primero con `tool_calls: [get_pantry]` y luego con texto + una Propuesta
When el usuario envía «Dame una receta con lo que tengo»
Then la app ejecuta `get_pantry` y le devuelve nombre y cantidad; la respuesta final muestra el texto y la Ficha con la Propuesta (RF-PCH5, RF-PCH6)

### Scenario 3: Ficha con Faltantes y guardado (verify 2)

Given la Despensa con «Tomate» y una Propuesta con «Tomate» (obligatorio, presente), «Harina» (obligatorio, ausente) y «Albahaca» (opcional, ausente)
When el usuario pulsa «Guardar en Mis Recetas»
Then la Ficha muestra «Harina» como único Faltante (RD15), la Receta aparece en Mis Recetas como **Borrador sin foto** con `origen = Sugerencia`, la Ficha queda «Guardada» y un segundo toque no duplica (RF-PCH7, RF-PCH8)

### Scenario 4: El Pinche propone sin Despensa (RD14)

Given la Despensa vacía y una Propuesta con dos obligatorios
When llega la respuesta
Then la Ficha se muestra igualmente con los dos obligatorios como Faltantes y «Guardar» habilitado (RD14, RF-PCH7)

### Scenario 5: Propuesta inválida

Given el cliente simulado devuelve texto y un JSON de Propuesta sin pasos
When se normaliza la respuesta
Then se muestra solo el texto, sin Ficha ni acción de guardar (RF-PCH6)

### Scenario 6: Sin API key (verify 3)

Given la app sin API key y el cliente simulado devolviendo `MissingApiKey`
When el usuario envía un mensaje
Then aparece el aviso con «Abrir Ajustes» y al pulsarlo se navega a `settings` (RF-PCH9)

### Scenario 7: Fallo del proveedor y reintento (verify 3)

Given el cliente simulado en modo `Failed`
When el usuario envía un mensaje y luego pulsa «Reintentar» con éxito
Then el aviso desaparece, la burbuja del usuario no se duplica y aparece la respuesta (RF-PCH9)

### Scenario 8: Entrada desde Mis Recetas y vuelta

Given Mis Recetas abierta
When el usuario pulsa la acción del Pinche y después vuelve atrás
Then se abre el Pinche con el saludo y, al volver, Mis Recetas conserva búsqueda y filtro (RF-PCH1, RF-PCH2)

### Scenario 9: Guardado fallido

Given el repositorio simulado que lanza un error al guardar
When el usuario pulsa «Guardar en Mis Recetas»
Then aparece el aviso de fallo de guardado y el botón vuelve a estar habilitado (RF-PCH9)

### Scenario 10: Textos en inglés

When el idioma del sistema es inglés
Then saludo, chips, compositor, avisos, cabeceras de la Ficha y botones se muestran en inglés (P19, RF-PCH11)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Mensaje vacío o solo espacios | «Enviar» deshabilitado; no se llama al modelo (RF-PCH2) |
| Mensaje > 500 caracteres | Se recorta a 500 al enviar (sin aviso bloqueante) |
| Envío mientras se espera respuesta | Ignorado; el compositor está deshabilitado (RF-PCH10) |
| El modelo pide una tool desconocida | Se le devuelve `{"error":"unknown_function"}` y se sigue el turno dentro del presupuesto (D6) |
| La tool lanza una excepción | Se le devuelve `{"error":"unavailable"}`; el turno no se cae (RD17) |
| Se alcanzan 3 llamadas y aún hay `tool_calls` | Turno `Failed` con «Reintentar» (D6, RK07) |
| `tool_calls` con argumentos no JSON | Se ignoran los argumentos (las tools no tienen parámetros) y se ejecuta la función (D4) |
| HTTP 400/401/403/5xx, cuerpo nulo, sin `choices` | `Failed` con «Reintentar»; se registra el código HTTP, nunca el cuerpo (RF-PCH9) |
| `content` con dos bloques `receta` | Se usa el primero y se descarta el resto (D7) |
| Propuesta con título repetido de una Receta existente | Se guarda una Receta **nueva** con id de Pinche; no se fusiona (RD16) |
| Guardar dos veces la misma Propuesta (mismo mensaje o dos respuestas iguales) | Upsert: una sola Receta (RD16) |
| Salir del Pinche o rotar | La conversación vive en el ViewModel: sobrevive a rotación, se pierde al salir (D2) |
| Despensa cambiada tras recibir la Ficha | Los Faltantes de esa Ficha no se recalculan (snapshot al recibir, D8) |
| Despensa con > 100 Alimentos / Mis Recetas > 50 | Se envían los primeros 100/50 (orden de repositorio) y se registra el recorte (D11) |

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `resolver-dudas-con-pinche` (`steps`/`verification` obsoletos: voz y card ejecutable).
- `CONTEXT.md`, `docs/build-brief.md`, `docs/domain-model.md` (RD14-RD17, entidad/estados/casos límite ya escritos en el descubrimiento), `docs/risks-and-open-questions.md` (`S08`, `RK07`, `RK03`, `Q-…-1..16`), `docs/technical-discovery.md` (§ «Pinche con function calling»), `DESIGN.md`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes`, `MyRecipesRoute(...)` y patrón de rutas sin argumentos.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` / `MyRecipesRoute.kt` — `TopAppBar` con 3 `IconButton` y `MyRecipesTestTags`; callbacks con valor por defecto.
- `app/src/main/java/com/whatsforlunch/app/ui/aisuggestions/AiSuggestionsViewModel.kt` — patrón de fases, avisos enum, llamada única con `try/catch` que repropaga `CancellationException` y guarda con `recipeRepository.saveRecipe`.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionRules.kt` — `normalize` (máx. 3), `MAX_SUGGESTIONS`, valores por defecto y normalización de la Propuesta.
- `app/src/main/java/com/whatsforlunch/app/domain/suggestion/SuggestionAcceptance.kt` — mapeo propuesta→`Recipe` Borrador/`SUGGESTION`, ids de hijos y patrón de id estable.
- `app/src/main/java/com/whatsforlunch/app/data/ai/RecipeSuggester.kt`, `RecipeSuggestionParser.kt`, `OpenAiCompatibleRecipeSuggester.kt` — puerto con `Success|MissingApiKey|Failed`, cliente OkHttp, `messageContent`, logging seguro, prompt puro es/en.
- `app/src/main/java/com/whatsforlunch/app/domain/model/{Recipe,RecipeIngredient,RecipeStep,PantryItem}.kt` — campos y valores por defecto.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/{RecipeRepository,PantryRepository}.kt` — `observeRecipes()`, `saveRecipe()`, `observePantryItems()`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin, `OkHttpClient` (10/30/60/90 s), `RecipeSuggester`, `ScanDraftHolder`.
- `app/src/main/java/com/whatsforlunch/app/domain/text/*` (`normalizeName`) — normalización compartida.
- `app/src/androidTest/java/com/whatsforlunch/app/AiSuggestionsE2ETest.kt` — patrón E2E con fakes, `InMemory*Repository`, montaje local y aserciones por `testTag`.
- `docs/specs/sugerir-recetas-con-ia.md`, `docs/specs/aceptar-sugerencia-de-receta.md` — contrato JSON compacto, normalización D8 y guardado por upsert.
- `docs/validations/aceptar-sugerencia-de-receta.md` — evidencia y notas Low vigentes.

### Existing Patterns To Follow

- Puertos de IA en `data/ai/` con `sealed interface` (`Success | MissingApiKey | Failed`) y clientes `OpenAiCompatible*` con `OkHttpClient`, `AiKeyProvider` y `baseUrl`; logging de código HTTP/duración/motivo sin key, cuerpo ni datos personales.
- MVVM + `StateFlow`; reglas puras en `domain/` con comentarios que citan `RD##`; Koin registra puertos y `viewModel`.
- UI en `ui/<feature>/`: `XxxRoute` (Koin + configuración de idioma) y `XxxScreen` stateless; `XxxTestTags` en snake_case con helpers `prefijo_$id`.
- E2E Compose sin `AppNavHost`: pantalla local + navegación simulada por flags + fakes; localización con `createConfigurationContext`.
- Estados vacío/carga/error y objetivos táctiles ≥ 48 dp (`DESIGN.md`).

### Current Gaps

- No existe nada del Pinche: ni ruta, ni pantalla, ni puerto, ni tools, ni tests. Todo se crea.
- `SuggestionRules.normalize` corta a 3: el Pinche debe recortar a 1 sin cambiar ese contrato.
- No hay `MockWebServer` (convención del repo): el bucle de tools se prueba con funciones puras + fake del puerto.
- **`S08` sin verificar**: no se sabe si el alias `chat` admite `tools`/`tool_calls` (ver prerrequisito).

## Technical Approach

- **Prerrequisito bloqueante `T0`** (`S08`, `RK07`): antes de implementar, una llamada real autorizada y de pago debe confirmar `tools` + `tool_calls` con el alias `chat`. Si el proveedor los rechaza, **parar**: registrar el resultado en `docs/risks-and-open-questions.md` y no implementar (no hay plan B de inyectar Despensa/Recetas en el prompt, RD17).
- **Dominio** — `domain/pinche/PincheConversation.kt`: `PincheAuthor { USER, ASSISTANT }`, `PincheTurn(author, text)`, `PincheProposalState { PROPOSED, SAVED }`, `PincheProposal(recipe: RecipeSuggestion, missingIngredients: List<String>)` y `PincheReply(text: String, proposal: RecipeSuggestion?)`.
- **Reglas** — `domain/pinche/PincheProposalRules.kt` (puro, sin Android):
  - `missingRequired(proposal, pantryItems): List<String>` → nombres de ingredientes **no opcionales** cuyo `normalizeName` no está en la Despensa, sin duplicados y en orden de la receta (RD15).
  - `proposalRecipeId(title) = UUID.nameUUIDFromBytes("pinche:${normalizeName(title)}")` (RD16).
  - `proposalToRecipe(proposal): Recipe` → `DRAFT`, `photoPath = null`, `favorite = false`, `origin = SUGGESTION`, ids `"$id-ingredient-$i"`/`"$id-step-$i"` (mismo patrón que `SuggestionAcceptance`, sin reutilizar su prefijo `suggestion:` ni modificar ese objeto).
- **Protocolo (puro y testeable)** — `data/ai/PincheChatProtocol.kt` (`internal object`):
  - `toolsJson()` con las dos definiciones (parámetros `{"type":"object","properties":{}}` y `additionalProperties:false`).
  - `toolNamesByCallId(choiceJson)` → `tool_calls[].id` + `function.name`.
  - `pantryToolResult(items)` / `recipesToolResult(recipes)` → JSON compacto (`{"items":[{"name","quantity"}]}` / `{"recipes":[{"title","moment","ingredients":[...]}]}`) con los topes de D11.
  - `parseReply(content)` → `PincheReply`: busca el bloque cercado `receta` y parsea su JSON con `RecipeSuggestionParser.parse` (contrato `{"s":[{…}]}`) + `SuggestionRules.normalize(...).firstOrNull()`; fallback: primer objeto JSON del contenido; el texto es todo lo anterior al bloque, sin fences, `trim()`. Sin bloque ni JSON → solo texto.
- **Puerto** — `data/ai/PincheChatClient.kt`:
  ```kotlin
  sealed interface PincheReplyResult {
      data class Success(val reply: PincheReply) : PincheReplyResult
      data object MissingApiKey : PincheReplyResult
      data object Failed : PincheReplyResult
  }
  interface PincheChatClient {
      suspend fun reply(turns: List<PincheTurn>, language: String): PincheReplyResult
  }
  ```
  `OpenAiCompatiblePincheChatClient(client, aiKeyProvider, pantryRepository, recipeRepository, baseUrl)` ejecuta el bucle: construye `messages` (system prompt + turnos), `tools`, `tool_choice = "auto"`, `temperature = 0`, `model = "chat"`, **sin `response_format`**; si la respuesta trae `tool_calls`, ejecuta las funciones (snapshot `observePantryItems().first()` / `observeRecipes().first()` en la primera invocación de cada tool del turno), añade `assistant(tool_calls)` + `tool` por `tool_call_id` y repite. **Máximo 3 llamadas HTTP por turno** (D6); si la tercera respuesta aún trae `tool_calls` → `Failed`. Sin key antes de la primera llamada → `MissingApiKey`. HTTP ≠ 2xx, cuerpo nulo, sin `choices`, sin `content` ni `tool_calls`, o tope superado → `Failed`. `CancellationException` se repropaga; sin reintentos. Logging `PincheChat`: código HTTP, duración, número de tools ejecutadas y motivo del fallo; nunca key, mensajes, Despensa ni Recetas.
  - **Prompt de sistema** (es/en, función pura `pincheSystemPrompt(language)`): rol de cocinero; responder dudas; usar `get_pantry`/`get_my_recipes` **solo si hace falta**; no inventar lo que devuelven; proponer **como máximo una** receta; para proponer, cerrar con un bloque cercado `receta` + `{"s":[{…}]}` con el contrato compacto (`t/st/mm/pm/cm/df/bs/kcal/i/p`), sin markdown fuera del bloque; responder en el idioma de la app.
- **ViewModel** — `ui/pinche/PincheViewModel(pantryRepository, recipeRepository, pincheChatClient)`:
  - `PincheUiState(messages, input, isSending, notice, lastUserText)`; `PincheNotice { AI_KEY_MISSING, FAILED, SAVE_FAILED }`; `PincheMessage(id, author, text, proposal: PincheProposal?, proposalState)` con id `UUID.randomUUID()` (solo memoria).
  - `onInputChange(text)` recorta a 500; `send()` ignora texto en blanco o envío en curso, añade la burbuja, guarda `lastUserText`, pasa a `isSending` y llama al puerto con los últimos 20 turnos (`USER`/`ASSISTANT`; el saludo y las Fichas no viajan como turnos). Al recibir `Success`, calcula Faltantes con **snapshot** `observePantryItems().first()` (D8) y añade la burbuja del asistente; `Success` sin texto y sin Propuesta → `Failed`.
  - `retry()` reenvía `lastUserText` sin añadir otra burbuja; `saveProposal(messageId)` marca `SAVED` antes del `saveRecipe` y revierte a `PROPOSED` + `SAVE_FAILED` si lanza; `clearNotice()`.
  - Mapea `MissingApiKey → AI_KEY_MISSING`, `Failed → FAILED`, excepciones del puerto → `FAILED` (repropagando `CancellationException`).
- **UI** — `ui/pinche/PincheScreen` (stateless) + `PincheRoute` (`configuration.locales[0].language`):
  - `Scaffold` + `TopAppBar` «Pinche» con flecha de vuelta; `LazyColumn` de burbujas (usuario a la derecha con `primaryContainer`; asistente a la izquierda con `surfaceContainerLowest`, `WarmRadius.lg`), saludo estático si no hay mensajes y chips de consulta rápida (`AssistChip`, altura táctil ≥ 48 dp) que envían su texto.
  - **Ficha** (`ProposalCard` dentro de la burbuja del asistente): cabecera «Propuesta del Pinche», título, Momento/tiempo/Raciones, «Ingredientes» (opcionales marcados), «Pasos» numerados con chip de temporizador y «Te faltan» (solo si hay Faltantes). Pie: `Button` «Guardar en Mis Recetas» → «Guardada en Mis Recetas» deshabilitado. **Sin** botón de Lista de la Compra ni de Modo Cocina.
  - Compositor: `OutlinedTextField` de una línea con contador implícito y botón «Enviar» (deshabilitado con texto en blanco o `isSending`); `imeAction = Send`. En `isSending`: fila «El Pinche está pensando…» con `LinearProgressIndicator` y compositor deshabilitado.
  - Avisos fuera de la lista (patrón del Escáner): `FAILED` + «Reintentar», `AI_KEY_MISSING` + «Abrir Ajustes», `SAVE_FAILED` + texto; descartables con la ✕.
- **Navegación y entrada** — `AppRoutes.PINCHE = "pinche"`; `composable` con `PincheRoute(onBack, onOpenSettings)`; `MyRecipesScreen`/`MyRecipesRoute` ganan `onOpenPinche: () -> Unit = {}` y un `IconButton` de chat como **primera** acción de la top bar (`Icons.AutoMirrored.Outlined.Chat`, testTag `action_open_pinche`, `contentDescription` «Abrir el Pinche»); `AppNavHost` navega a `pinche` y «Abrir Ajustes» a `settings`.
- **Textos** (`values/strings.xml`, `values-en/strings.xml`): `pinche_title`, `pinche_open_accessibility`, `pinche_greeting`, `pinche_quick_1..3`, `pinche_input_hint`, `pinche_send`, `pinche_thinking`, `pinche_failed`, `pinche_retry`, `pinche_key_missing`, `pinche_open_settings`, `pinche_proposal_header` («Propuesta del Pinche»), `pinche_proposal_ingredients`, `pinche_proposal_steps`, `pinche_proposal_missing`, `pinche_proposal_optional`, `pinche_save`, `pinche_saved`, `pinche_save_failed`, `pinche_timer_minutes`, `pinche_notice_dismiss`, `pinche_message_accessibility` (es/en, P19). Cadenas con placeholders para tiempos/Raciones reutilizan las claves de Detalle cuando encajen.
- **Tests** — unit: `PincheChatProtocolTest` (tools JSON, `tool_calls`, resultados con topes, `parseReply` con/sin bloque, fallback y bloque inválido), `PincheProposalRulesTest` (Faltantes solo obligatorios, normalización de nombres, Despensa vacía, id estable, Borrador/`SUGGESTION`/sin foto, guardar dos veces = mismo id), `PincheViewModelTest` (envío, tool-turno, `MissingApiKey`, `Failed` + `retry` sin duplicar, Ficha con Faltantes, guardado y guardado fallido, texto en blanco, recorte a 500). E2E `PincheE2ETest` con `FakePincheChatClient` (S1-S10 y los verify).

### Decisiones (para no reabrir el debate en implementación)

- **D1 — se amplía el Pinche existente** (2026-10-26): no se crea un chatbot nuevo ni otra pantalla; entrada por la top bar de Mis Recetas.
- **D2 — solo memoria**: conversación, Propuestas y estado `SAVED` viven en el ViewModel; sin Room ni historial (RD17, Q-5).
- **D3 — solo texto**: sin micrófono ni dictado; la voz depende de `⚠ S04` (Q-6).
- **D4 — dos tools de solo lectura** (Q-8, Q-9, Q-14): `get_pantry` (nombre/cantidad) y `get_my_recipes` (título, momento, ingredientes); ninguna escritura.
- **D5 — sobre de la respuesta (cierra `Q-resolver-dudas-con-pinche-16`)**: sin `response_format` cuando hay `tools`; el prompt exige texto y, si procede, un bloque cercado `receta` con `{"s":[{…}]}`. El parser separa texto y Propuesta; si el modelo ignora el marcador, el fallback busca el primer objeto JSON. Un JSON inválido deja solo el texto (sin fallo de turno).
- **D6 — presupuesto por turno**: máximo **3 llamadas HTTP** (inicial + 2 rondas de tools), sin reintentos automáticos ni caché; superarlo es `Failed` (`RK07`). «Reintentar» es una acción del usuario y reenvía el último mensaje.
- **D7 — como máximo una Propuesta**: se reutiliza `SuggestionRules.normalize` (D8 de Sugerencias) y se toma la primera; los opcionales no son Faltantes (RD10).
- **D8 — Faltantes**: solo obligatorios ausentes por `normalizeName`; se calculan una vez al recibir la respuesta y no se recalculan si cambia la Despensa (RD15).
- **D9 — guardado**: Borrador sin foto, `origen = SUGGESTION`, id `pinche:` + título normalizado y upsert; sin origen nuevo (RD16).
- **D10 — fallos visibles**: `MissingApiKey`, `Failed` y `SAVE_FAILED` tienen aviso y acción; nunca se inyecta contexto por otra vía (RD17).
- **D11 — límites de contexto**: últimos 20 mensajes, 100 Alimentos, 50 Recetas, 20 ingredientes por Receta, mensaje de usuario ≤ 500 caracteres.
- **D12 — idioma**: prompt de sistema es/en y respuesta en el idioma de la app; `mm`/`df` viajan como tokens de máquina (D9 de Sugerencias).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheConversation.kt` — crear; turnos, Propuesta, estados y resultado de la respuesta.
- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheProposalRules.kt` — crear; Faltantes (RD15), id estable y mapeo a Borrador (RD16).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — crear; puerto + `PincheReplyResult`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — crear; tools, `tool_calls`, resultados y parser de la respuesta (D5, D11).
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — crear; bucle de tool calling con tope y logging seguro.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt`, `PincheScreen.kt`, `PincheRoute.kt` — crear; estado, compositor, burbujas, Ficha y wiring.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; `AppRoutes.PINCHE`, `composable` y cableado de Mis Recetas/Ajustes.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — modificar; `onOpenPinche`, acción de top bar y `MyRecipesTestTags.OPEN_PINCHE`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesRoute.kt` — modificar; nuevo callback con valor por defecto.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `PincheChatClient` y `PincheViewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; claves `pinche_*` es/en.
- `app/src/test/java/com/whatsforlunch/app/data/ai/PincheChatProtocolTest.kt` — crear.
- `app/src/test/java/com/whatsforlunch/app/domain/pinche/PincheProposalRulesTest.kt` — crear.
- `app/src/test/java/com/whatsforlunch/app/ui/pinche/PincheViewModelTest.kt` — crear.
- `app/src/androidTest/java/com/whatsforlunch/app/PincheE2ETest.kt` — crear; verify 1-3, S1-S10 y regresión de entrada.
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`, `docs/progress-history.md` — modificar al implementar (ver abajo).
- `docs/validations/resolver-dudas-con-pinche.md` y `docs/evidence/resolver-dudas-con-pinche/*.png` — crear al validar.

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` (fila y estados del Pinche ya descritos) + tokens «Warm Culinary Craft» de `uxdocs/DESIGN.md`. No hay prototipo de chat en `uxdocs/`: esta spec fija la estructura para que el implementer no invente estilo.
- Screens or states affected: nueva pantalla **Pinche** (vacía con saludo y chips, enviando, con respuesta, con Ficha, avisos y sin key); Mis Recetas solo gana un icono en la top bar.
- New design artifact required: no — burbujas y tarjetas con los tokens y componentes existentes.

Detalle verificable con `uiautomator` y captura:

- **Top bar del Pinche**: título `titleLarge`, flecha de vuelta de 48 dp, fondo `surface`.
- **Burbujas**: usuario `bodyMedium` sobre `primaryContainer`/`onPrimaryContainer` con esquinas `WarmRadius.md` y margen `WarmSpacing.md`; asistente sobre `surfaceContainerLowest` con sombra cálida (`warmShadow`), texto `bodyMedium` y máximo ancho 85 %.
- **Ficha**: `WarmRadius.lg`, cabecera «Propuesta del Pinche» en `labelSmall` sobre `secondaryContainer`, título `titleSmall`, metadatos `labelSmall`, ingredientes «cantidad unidad nombre» y pasos numerados con chip de temporizador; «Te faltan» en `errorContainer` con texto `onErrorContainer`; botón de guardar altura mínima 52 dp; estado guardado con `ButtonDefaults` deshabilitado.
- **Chips**: `AssistChip`/`Surface` con 48 dp de área táctil, solo con conversación vacía.
- **Compositor**: campo con `WarmSpacing.md`, botón de envío de 48 dp con `contentDescription`; en carga, progreso lineal + texto `labelSmall`.
- **Avisos**: banner fuera de la lista, texto `bodySmall` + acción y ✕ de descarte; sin tapar el compositor.
- **testTags** en `PincheTestTags`: `SCREEN`, `MESSAGE_LIST`, `userMessage(index)`, `assistantMessage(index)`, `QUICK_SUGGESTION`, `COMPOSER`, `SEND`, `PROGRESS`, `NOTICE`, `RETRY`, `OPEN_SETTINGS`, `proposalCard(index)`, `PROPOSAL_TITLE`, `PROPOSAL_INGREDIENTS`, `PROPOSAL_STEPS`, `PROPOSAL_MISSING`, `SAVE`, `SAVED_LABEL`.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (un puerto más en `data/ai/`, dominio en `domain/pinche/`, UI en `ui/pinche/`).
- `CONSTRAINTS.md`: not needed — no existe; RD14-RD17 ya viven en `docs/domain-model.md`.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs:
  - `docs/domain-model.md` — **update al implementar**: confirmar entidad/estados de la Propuesta y RD14-RD17 con lo implementado (snapshot de Faltantes, tope de tools, sobre de respuesta).
  - `docs/technical-discovery.md` — **update**: cerrar `Q-16` (bloque `receta` sin `response_format`, tope de 3 llamadas), documentar topes de contexto y registrar la latencia/coste medidos si `T0` los aporta.
  - `docs/risks-and-open-questions.md` — **update**: resultado de `S08` (verificado o fallido), cierre de `Q-resolver-dudas-con-pinche-16` y matices de `RK07`/`RK03`.
  - `PROGRESS.md` / `docs/progress-history.md` — **update** al cerrar la sesión.

## Implementation Plan

1. Ejecutar `T0` (llamada real autorizada) y dejar constancia del resultado; sin `tools` confirmados, parar.
2. Dominio: `PincheConversation` + `PincheProposalRules` con sus unit tests (RD15, RD16, D7, D8).
3. Protocolo y parser: `PincheChatProtocol` con unit tests (tools, `tool_calls`, resultados con topes, `parseReply` y fallback).
4. Cliente: `PincheChatClient` + `OpenAiCompatiblePincheChatClient` con bucle, tope de 3 llamadas, mapeo de fallos y logging seguro; registrar en `AppModule`.
5. `PincheViewModel` con sus unit tests (envío, tool-turno, avisos, retry, Ficha, guardado y guardado fallido).
6. UI y navegación: `PincheScreen`/`PincheRoute`, textos es/en, testTags, acción en Mis Recetas y ruta `pinche`.
7. E2E `PincheE2ETest`, regresión completa, docs y evidencia.

## Implementation Tasks

- [x] T0: verificar `tools`/`tool_calls` en el alias `chat` con una llamada real autorizada (pago) y documentar el resultado en `docs/risks-and-open-questions.md` (`S08`, `RK07`). **Bloqueante.**
- [x] T1: `domain/pinche/PincheConversation.kt` + `PincheProposalRules.kt` + `PincheProposalRulesTest`. (RF-PCH7, RF-PCH8, S3, S4, D7-D9)
- [x] T2: `PincheChatProtocol.kt` + `PincheChatProtocolTest` (tools, resultados, topes, `parseReply` con bloque/fallback/JSON inválido). (RF-PCH5, RF-PCH6, S2, S5, D5, D11)
- [x] T3: `PincheChatClient.kt` + `OpenAiCompatiblePincheChatClient.kt` + `AppModule` (bucle, 3 llamadas, `MissingApiKey`/`Failed`, logging). (RF-PCH4, RF-PCH9, S2, S7, D6, D10)
- [x] T4: `PincheViewModel` + `PincheViewModelTest` (turnos, Ficha con Faltantes, guardado, retry sin duplicar, texto en blanco, recorte a 500). (RF-PCH2, RF-PCH7, RF-PCH8, RF-PCH9, S1-S9)
- [x] T5: `PincheScreen`/`PincheRoute` + textos es/en + `PincheTestTags` + acción `action_open_pinche` + ruta `pinche`. (RF-PCH1, RF-PCH2, RF-PCH10, RF-PCH11, S8, S10)
- [x] T6: `PincheE2ETest` (verify 1-3, S1-S10) y regresión de Mis Recetas/NavHost sin romper la suite. (11/11 en la suite de 148)
- [x] T7: gates en verde, docs actualizadas (`Q-16`, `S08`, `RK07`, `RK03`) y evidencia capturada. (173/173 unit, lint 0 errores, `assembleDebug` OK, E2E 148/0/0/1)

## Verification Plan

- `./init.ps1` / `./init.sh` → gate no bloqueante (no arranca procesos duraderos; el falso fallo conocido de `pidof`/`adb shell monkey` se documenta como en sesiones previas).
- `./lint.ps1` / `./unit.ps1` → exit 0; `unit` incluye `PincheChatProtocolTest`, `PincheProposalRulesTest`, `PincheViewModelTest` y la regresión (Sugerencias, Recetas compatibles, Escáner, Detalle). Reejecución forzada: `gradlew.bat testDebugUnitTest --rerun-tasks`.
- `assembleDebug` sin errores.
- **E2E persistente** `./e2e.ps1` (`connectedDebugAndroidTest`, AVD) con `PincheE2ETest`:
  - **verify 1**: `FakePincheChatClient` responde primero `tool_calls` de `get_pantry` y luego texto + Propuesta → se muestra la respuesta con su Ficha (ingredientes, pasos y Faltantes).
  - **verify 2**: guardar la Ficha → la Receta aparece en Mis Recetas como Borrador; guardar dos veces no duplica.
  - **verify 3**: `MissingApiKey` → aviso + «Abrir Ajustes» navega a `settings`; `Failed` → aviso + «Reintentar» con éxito sin duplicar la burbuja.
  - S1: duda sin Propuesta (solo burbuja de texto).
  - S4/S5: Despensa vacía → todos los obligatorios como Faltantes; Propuesta inválida → solo texto.
  - S8 (entrada): desde `MyRecipesScreen`, `action_open_pinche` abre el Pinche y «volver» regresa conservando búsqueda y filtro.
  - S9: fallo de guardado → aviso y botón habilitado.
  - S10: en inglés cambian saludo, chips, compositor, avisos, Ficha y botones (patrón de `AiSuggestionsE2ETest`).
  - Regresión completa de la suite en verde, sin romper Mis Recetas, Sugerencias, Recetas con mi Despensa, Escáner, Detalle ni Ajustes.
- `init.sh`/`init.ps1` **no se modifican**; los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.
- **Sin llamadas reales a DevExpert Inference en la verificación automática**: todo usa `FakePincheChatClient` y fakes. `T0` es la única llamada real, manual, autorizada y previa; el validador no la repite.
- UI (solo el validador): capturas en español del chat con Ficha y de Mis Recetas con la Receta guardada, más `uiautomator`; sin screenshot testing (precedente del repo).

## Evidence To Capture

- `docs/validations/resolver-dudas-con-pinche.md` — resultado de `T0`, `lint`/`unit`/`e2e` con nombres de test y verificación de S1-S10.
- `docs/evidence/resolver-dudas-con-pinche/pinche-ficha-es.png` — chat con respuesta, Ficha, Faltantes y botón de guardar (es).
- `docs/evidence/resolver-dudas-con-pinche/pinche-guardada-es.png` — Mis Recetas con la Receta guardada como Borrador (es).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Prerrequisito bloqueante (`S08`)

`T0` exige una clave real y una llamada de pago con autorización humana explícita (`AGENTS.md`). Hasta que se complete:

- La feature **no pasa de `not_started`/spec**: no se implementa el cliente ni se simula como si `tools` funcionara.
- Si el proveedor no admite `tools`, el resultado se registra en `docs/risks-and-open-questions.md` y se pregunta al usuario por el camino de producto (cambio de proveedor o reapertura de RD17). **No** se implementa un plan B que inyecte la Despensa o Mis Recetas en el prompt.

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (conversación con chips de sugerencias) | RF-PCH1, RF-PCH2 | S1, S8 | T4, T5 | unit `PincheViewModelTest` + E2E |
| Step 1 (function calling de solo lectura) | RF-PCH4, RF-PCH5 | S2 | T2, T3 | unit `PincheChatProtocolTest` + E2E |
| Step 2 (Ficha con ingredientes, pasos y Faltantes) | RF-PCH6, RF-PCH7 | S2, S3, S4, S5 | T1, T2, T4, T5 | unit + E2E + captura |
| Step 3 (guardar como Borrador) | RF-PCH8 | S3, S9 | T1, T4, T6 | unit + E2E + captura |
| Verify 1 (respuesta con su Ficha) | RF-PCH4, RF-PCH6, RF-PCH7 | S1, S2 | T6 | `PincheE2ETest` |
| Verify 2 (guardar sin duplicar) | RF-PCH8 | S3 | T6 | `PincheE2ETest` |
| Verify 3 (sin key / fallo con «Reintentar») | RF-PCH9 | S6, S7 | T4, T6 | `PincheE2ETest` |
| — (RD14: propone sin Despensa) | RD14 | S4 | T1, T4 | unit + E2E |
| — (es/en) | RF-PCH11 | S10 | T5 | E2E de localización |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature.
- [ ] Los escenarios S1-S10 pasan y las decisiones D1-D12 se respetan (en especial D4: solo dos tools de lectura; D5: una sola Propuesta y sin `response_format`; D6: 3 llamadas máximo; D8: Faltantes solo obligatorios; D9: Borrador con `origen = Sugerencia` e id `pinche:`).
- [ ] `T0` está documentado con el resultado real de `tools` (no simulado) y no se repitió ninguna llamada de pago en la verificación.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente añadida (`PincheE2ETest`) y suite completa en verde, sin romper Mis Recetas, Sugerencias, Recetas con mi Despensa, Escáner, Detalle ni Ajustes.
- [ ] `feature_list.json` (solo `status`/`evidence`) y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió trabajo de otras features: sin Lista de la Compra ni Faltantes→ÍtemLista, sin Modo Cocina, sin voz/micrófono, sin persistencia ni migraciones de Room, sin `/images/generations`, sin tocar `calculateMatch`, `MatchBadge`, RD01/RD02/RD10/RD13 ni `SuggestionAcceptance`/`SuggestionRules`.
- [ ] Sin llamadas reales de pago en la verificación automática (solo `FakePincheChatClient` y fakes) y sin API keys reales en ficheros, logs ni evidencia (`⚠ S05`, `RK01`); `RK03` actualizado con los datos que salen del dispositivo.
- [ ] `docs/risks-and-open-questions.md` registra el cierre de `Q-resolver-dudas-con-pinche-16` y el resultado de `S08`.
