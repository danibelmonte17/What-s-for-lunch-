# Feature Implementation Spec: Rellenar Mi perfil conversando con el Pinche

## Source Feature

- `id`: rellenar-perfil-con-el-pinche
- `area`: funcional
- `depends_on`: `crear-mi-perfil` (accepted), `resolver-dudas-con-pinche` (accepted)
- `status`: not_started (a la hora de planificar; 2026-11-09); **`accepted`** al cierre de la sesión 122
- `source`: `feature_list.json`
- **enmienda 2026-11-10 (esta)**: respuesta del LLM en **streaming** + **auto-scroll** al mensaje nuevo; la feature vuelve a `in_progress` durante la implementación de la enmienda. Decisiones **D9–D13** al final del documento.

## Goal

Cuando el Pinche va a proponer una receta y faltan partes del perfil, en lugar de bloquear con un mensaje estático las pide en el propio chat —una pregunta a la vez y solo por lo que falta— y va guardando cada respuesta en «Mi perfil» con una única función de escritura `save_profile`, mostrando un resumen con lo realmente guardado. En cuanto el perfil queda completo continúa con lo que el usuario pedía y propone la receta respetándolo. Las dudas de cocina que no llevan propuesta se siguen respondiendo sin perfil. Si la IA no está disponible, se conserva el mensaje estático de partes pendientes con «Abrir Mi perfil». **La enmienda (2026-11-10) añade además**: la respuesta del asistente se procesa en **streaming** (el texto va apareciendo en la burbuja y nunca se pinta el JSON de la Propuesta) y la lista **baja siempre al último mensaje** cuando llega uno nuevo o un fragmento.

## Non-Goals

- Ampliar el consumo del perfil a Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias (sigue siendo solo el Pinche).
- Añadir `get_profile` u otras tools: el perfil viaja en el system prompt y la **única** tool nueva es `save_profile`, acotada al perfil.
- Confirmación previa del guardado ni edición del perfil desde el chat (D2: guardado directo + resumen; editar sigue siendo en «Mi perfil»).
- Reescribir la spec ni la evidencia histórica de `crear-mi-perfil`: su comportamiento RD26/D8 queda enmendado desde esta spec y desde las `notes` de `feature_list.json`.
- Cambios de esquema de Room, de la pantalla «Mi perfil» ni de sus reglas (`UserProfileRules` solo se amplía, no se cambia).
- Traducir las chips del usuario, voz, cuentas, sincronización ni exportación del perfil.
- Verificación con el modelo real de pago: todo con cliente simulado; una pasada real solo sería un verify manual opcional con confirmación humana previa.
- **Enmienda (2026-11-10)**: streaming o cambios en Sugerencias, Escáner, «Generar foto» ni el resto de superficies de IA (el streaming es solo del Pinche); dependencias nuevas (sin MockWebServer ni librería SSE); cambiar `MAX_HTTP_CALLS`, el contrato de tools, RD05/RD17 o el aviso de fallo (sigue siendo «Reintentar»); pintar en el chat el JSON de la Propuesta; verify con llamada de pago al proveedor (solo mock local).

## Job Story

When quiero que el Pinche me recomiende algo y mi perfil está incompleto,
I want to que me pregunte por lo que falta en el propio chat y guarde mis respuestas,
so I can recibir la recomendación sin abrir «Mi perfil» ni repetir mis restricciones.

## Users And Permissions

- **Usuario** (único rol, propietario del dispositivo; sin cuentas, `P7`/`P31`): responde en el chat y sus respuestas de datos de salud se guardan en «Mi perfil» en local. Esas respuestas viajan al proveedor de IA (`RK03`): evidencia solo con datos ficticios; sin permisos Android ni dependencias nuevas.

## Acceptance Scenarios

### Scenario 1: Entrevista de una en una antes de proponer

Given el perfil vacío (las tres partes pendientes) y el usuario pide una receta al Pinche
When el Pinche va a proponer
Then no muestra Ficha: pregunta solo por la primera parte pendiente; a cada respuesta del usuario la guarda, muestra el resumen «He guardado…» con lo persistido, pregunta la parte siguiente que siga pendiente y, cuando no queda nada pendiente, propone la receta respetando el perfil (verify 1).

### Scenario 2: Una duda de cocina sin perfil no se bloquea

Given el perfil vacío
When el usuario envía una duda de cocina que no pide receta
Then el Pinche responde con normalidad, sin entrevista y sin mensaje de petición (D1; complemento de verify 1).

### Scenario 3: Perfil a medio completar

Given el perfil con Alergias ya guardada («Frutos secos») y pendientes Intolerancias y Tipo de alimentación
When el usuario pide una receta
Then el Pinche solo pregunta por lo que sigue pendiente, las respuestas ya guardadas no se pierden ni se piden de nuevo y la propuesta final las respeta (verify 2).

### Scenario 4: Nunca se propone con el perfil incompleto (guarda)

Given el perfil incompleto
When llega una respuesta del modelo con Propuesta
Then la Ficha no se muestra y en su lugar aparece el mensaje de las partes pendientes con «Abrir Mi perfil»; la acción abre la pestaña del perfil (guarda D1; complemento de verify 1).

### Scenario 5: Degradación sin IA

Given el perfil incompleto y sin API key (o con fallo del proveedor)
When el usuario envía cualquier mensaje
Then aparece el aviso de siempre («Abrir Ajustes»/«Reintentar») y el mensaje de las partes pendientes con «Abrir Mi perfil», que completa el perfil desde esa pantalla (verify 3).

### Scenario 6: Guardado fiel y validado

Given las respuestas del usuario contienen una chip vacía, una duplicada o un tipo de alimentación desconocido
When el modelo invoca `save_profile`
Then la llamada se rechaza sin persistir nada y devuelve el error al modelo para que corrija; con datos válidos el resumen del chat muestra exactamente lo persistido en Room (verify 4, unit).

### Scenario 7: Textos es/en y chips literales

Given la app en inglés y el usuario responde la entrevista en inglés
Then los textos nuevos existen en es y en y las chips se guardan literalmente en el idioma de la conversación, sin traducir (paridad de claves; complemento de verify 4).

### Scenario 8: La respuesta del asistente llega en streaming (enmienda)

Given el usuario envía un mensaje y el proveedor responde con `stream: true`
When llegan los fragmentos (`choices[0].delta.content`)
Then la burbuja del asistente aparece ya con el primer fragmento y va creciendo a medida que llegan, **sin** pintar nunca el bloque ```` ```receta ```` ni el JSON de la Propuesta (se cortan con `streamingText`), y al terminar se sustituye por el mensaje final de siempre (texto + Ficha + resumen «He guardado…» antes de la respuesta, D2 intacto).

### Scenario 9: Fallo o corte del stream, y respuesta sin SSE (enmienda)

Given una respuesta en streaming que se corta a mitad (o una respuesta que llega como JSON en lugar de `text/event-stream`)
When se detecta el corte / el cuerpo sin SSE
Then **se conserva** el texto ya recibido como burbuja del asistente (D11) y aparece el aviso de siempre con «Reintentar» (RD05); si el cuerpo no es SSE se parsea como hasta hoy y el resultado es idéntico al flujo sin streaming (fallback D12), sin doble aviso ni mensajes vacíos.

### Scenario 10: La lista baja al último mensaje (enmienda)

Given una conversación larga donde el usuario ha hecho scroll hacia arriba
When llega un mensaje nuevo o un fragmento de streaming
Then la lista **siempre** vuelve al último mensaje (D10): `animateScrollToItem` al llegar un mensaje nuevo y `scrollToItem` con cada fragmento; tocar la pestaña activa para ir arriba sigue funcionando (RF-MOB6) mientras no haya texto llegando.

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — `runRequest()` con el gate RD26 (líneas 200–218, punto que se retira), `PincheMessage.profileRequest`, `currentProfile()`, `onSuccess()`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — `ProfileRequestContent`/`pendingLabel` (líneas 287–318, patrón del mensaje localizado con acción) y las burbujas (cuando `profileRequest`/`text`, líneas 242–268).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — puerto `reply(turns, language, profile)` y `PincheReplyResult` (aquí se añade lo guardado por turno).
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — bucle de tool calls con snapshots por turno (líneas 84–170), `MAX_HTTP_CALLS = 3` (línea 207), `renderProfilePrompt` en el system prompt (líneas 68–69).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — `toolsJson()` (las dos tools de solo lectura, líneas 34–64), `toolCalls()` (ignora argumentos, líneas 75–84) y builders de resultado.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PromptSource.kt` — `renderProfilePrompt`/`dietTypeName` (vocabulario es/en del enum, líneas 76–112).
- `app/src/main/assets/prompts/pinche_system_es.txt` — «Tienes dos funciones de solo lectura…» (hay que actualizarlo) y `pinche_profile_es.txt` — bloque de perfil con `{DIET_TYPE}`/`{ALLERGIES}`/`{INTOLERANCES}` («sin declarar»/«sin elegir» ya marcan lo pendiente).
- `app/src/main/java/com/whatsforlunch/app/domain/profile/UserProfileRules.kt` — `addChip`/`setNone`/`setDietType`, `pendingParts`, `isComplete`, `PendingProfilePart`.
- `app/src/main/java/com/whatsforlunch/app/domain/model/UserProfile.kt`, `DietType.kt` — modelo y enum cerrado (OMNIVORE…KETOGENIC).
- `app/src/main/res/values/strings.xml` / `values-en/strings.xml` — `pinche_profile_request`/`pinche_profile_open` (359–360) y etiquetas `my_profile_*` (363–373) reutilizables para el resumen.
- Tests: `ui/pinche/PincheViewModelTest.kt` (los tests del gate RD26 cambian de comportamiento), `data/ai/PincheChatProtocolTest.kt`, `data/ai/PromptAssetsTest.kt`/`PinchePromptsTest.kt`, `androidTest/.../PincheE2ETest.kt` y `ShoppingListFlowTest.kt` (`FakePincheChatClient` con cola de `PincheReplyResult`).
- Docs: `docs/specs/crear-mi-perfil.md` (D5/D6/D8 y su verify 4, que esta feature enmienda), `docs/domain-model.md` (RD25/RD26), `docs/build-brief.md:39,75` (línea roja `⚠ S08`), `docs/risks-and-open-questions.md` (`S08`, `RK03`, `RK07`), `docs/technical-discovery.md` (contrato de tools y topes).

### Existing Patterns To Follow

- Reglas puras en `domain/<feature>/` con IDs `RD##` en `docs/domain-model.md`; la UI no contiene reglas de negocio (P19/P25).
- Tools de IA declaradas y parseadas en `PincheChatProtocol` (objeto puro, testeable en JVM); el cliente solo orquesta y persiste vía puertos `domain/repository/*`.
- Mensajes de asistente con contenido localizado y sin texto (`PincheMessage.profileRequest`) que **nunca viajan al modelo** (el filtro `text.isNotBlank()` de `runRequest`); patrón a reutilizar para el resumen «He guardado…» y la guarda de la Propuesta.
- Textos en `values`/`values-en` con paridad exacta; prompts en `assets/prompts/*_{es,en}.txt`; fakes de `PincheChatClient` en unit y E2E.

### Current Gaps

- `PincheChatProtocol.toolCalls()` **ignora los argumentos** (las tools actuales no tienen): `save_profile` necesita parsearlos.
- `PincheReplyResult.Success` no transporta qué se guardó; el ViewModel no tiene forma de pintar un resumen fiel.
- El gate RD26 vive en `runRequest` y sus tests lo afirman: hay que retirarlo y reescribir esos tests.

### Archivos inspeccionados para la enmienda (2026-11-10)

- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — puerto único `reply(turns, language, profile): PincheReplyResult`; aquí entra el `onContent`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — `buildBody` (líneas 221–229, sin `stream`), petición `client.newCall(request).execute()` y lectura del cuerpo (107–130), bucle de rondas con tools y tope `MAX_HTTP_CALLS = 5` (99–186). Es el único punto de red del Pinche.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — `toolCalls(message)` (129–138: exige `tool_calls[].id` + `function.name`) y `parseReply`/`findRecipeBlock`/`firstJsonObject` (276–305: la Propuesta vive **dentro del contenido**, cercada ```` ```receta ```` o como primer objeto JSON).
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — `PincheUiState` (60–66), `runRequest` (209–237, llama a `reply`), `onSuccess` (265–307, ordena resumen antes de la respuesta) y `onUnavailable` (244–259).
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — `LazyColumn` de mensajes con `itemsIndexed(..., key = message.id)` (192–283), `PincheTestTags` (69–97), scroll-to-top por `scrollToTopTick` (121–127) y `ThinkingRow` (381–397). **El chat no hace auto-scroll** (hallazgo de la sesión 121: hay que arrastrar el `LazyColumn`).
- `app/build.gradle.kts` — `implementation(libs.okhttp)` (línea 103); en `testImplementation` solo `coroutines-test` y `org.json`: **no hay MockWebServer** ni librería SSE.
- Fakes del puerto (han de ampliar la firma): `androidTest/.../PincheE2ETest.kt:875`, `MyProfileE2ETest.kt:305`, `ShoppingListE2ETest.kt:1398`, `test/.../ShoppingListFlowTest.kt:133` y `test/.../PincheViewModelTest.kt:574`.
- `docs/technical-discovery.md` (contrato del Pinche y topes), `docs/domain-model.md:75` (RD17), `docs/risks-and-open-questions.md` (`RK07`), `DESIGN.md` (fila del Pinche).

### Gaps de la enmienda

- **No se ha verificado que el alias `chat` de DevExpert admita `stream: true`** → riesgo cubierto por el fallback (D12) y pendiente de confirmar con el proveedor; el verify real queda fuera (D13).
- No existe soporte de streaming en ninguna superficie ni parser SSE en el repo: hay que crearlo **puro** (sin dependencias nuevas) para poder testearlo en JVM.
- No hay infraestructura de scroll automático: el `LazyListState` solo tiene el trigger de RF-MOB6.

## Technical Approach

1. **Dominio** (`domain/profile/`): data class `ProfileUpdate(allergies: List<String>?, allergiesNone: Boolean?, intolerances: List<String>?, intolerancesNone: Boolean?, dietType: DietType?)` (campos omitidos = sin tocar) y `UserProfileRules.applyUpdate(profile, update): ProfileUpdateOutcome` — `Saved(updated, savedParts)` / `Invalid(code)` — que compone las reglas existentes: por sección, `*_none = true` exige chips ausentes/vacías y usa `setNone` (limpia); si llegan chips, se fija la lista completa tras validar cada una (`normalizeChip` → `invalid_chip`) y sin duplicados por sección ignorando mayúsculas (`duplicate_chip`); `diet_type` ya mapeado al enum (`unknown_diet_type` si no); al menos un campo (`invalid_arguments`). `savedParts` lista las partes escritas con sus valores finales y `pendingParts(updated)` da lo que falta.
2. **Protocolo** (`PincheChatProtocol`): tercera tool `save_profile` («write the user's Mi perfil sections from what the user just said; only call it when the user states their data») con JSON Schema de los 5 campos opcionales (`additionalProperties: false`), `parseSaveProfileArgs(json): ProfileUpdate?` con el vocabulario cerrado es/en de `DietType` (omnívora/omnivore, pescetariana/pescetarian, vegetariana/vegetarian, vegana/vegan, cetogénica/ketogenic — mismo vocabulario que `renderProfilePrompt`), `saveProfileToolResult(savedParts, pending)` → `{"saved": {...}, "pending": ["ALLERGIES", ...]}` y `saveProfileToolError(code)` → `{"error": code}`. El resultado lleva lo pendiente para que el modelo sepa qué preguntar después.
3. **Cliente** (`OpenAiCompatiblePincheChatClient`): gana `UserProfileRepository`. Al ejecutar `save_profile` lee el perfil vigente (una vez por turno, con caché actualizada tras cada guardado, patrón de `pantrySnapshot`), aplica `applyUpdate`, persiste con `save()` y devuelve el resultado; los errores del outcome viajan como `{"error": …}` sin persistir. `PincheReplyResult.Success` gana `profileSaved: List<SavedProfilePart>?` (partes escritas en el turno, valores finales acumulados de todas las invocaciones). **`MAX_HTTP_CALLS` 3 → 5** (D3; actualiza el comentario D6 y `RK07`).
4. **Prompt** (assets): `pinche_system_{es,en}.txt` pasa a describir **tres** funciones (las dos de solo lectura + `save_profile` de escritura acotada al perfil). `pinche_profile_{es,en}.txt` gana las reglas de entrevista (D1/D6): si una parte está «sin declarar»/«sin elegir» y vas a proponer una receta, **no propongas todavía**: pregunta por **una sola** parte pendiente por mensaje, guarda con `save_profile` lo que el usuario diga (chips en el idioma de la conversación, sin traducir; tipo de alimentación de la lista fija) y, cuando no quede nada pendiente, continúa con la petición original y propón. Las dudas de cocina sin propuesta se responden sin entrevistar.
5. **ViewModel/UI** (`ui/pinche/`): se retira el gate de `runRequest` (siempre que haya texto se llama al cliente). En el éxito: (a) si `profileSaved != null`, mensaje del asistente tipo `profileSaved` (nuevo campo de `PincheMessage`, texto en blanco, patrón `profileRequest`) con el resumen «He guardado en “Mi perfil”: …» compuesto en la UI con las etiquetas `my_profile_*` + valores literales + «ninguna»; (b) **guarda D1**: si la respuesta lleva Propuesta y `UserProfileRules.pendingParts(currentProfile())` no está vacío, **no se pinta la Ficha** y se añade el mensaje estático de partes pendientes con «Abrir Mi perfil» (texto de la respuesta sí se conserva); (c) si falta key o falla la llamada con partes pendientes, además del aviso de siempre se añade ese mensaje estático (degradación, S5). `PincheScreen` gana `ProfileSavedContent` (mismo patrón que `ProfileRequestContent`).
6. **Casos límite**: varios `save_profile` en un turno → se acumulan y sale un único resumen; `save_profile` sobre una sección ya completa la sustituye (permite correcciones); el usuario que evade preguntas nunca ve una Propuesta (la guarda es determinista) y puede completar por chat o en «Abrir Mi perfil»; conversación y resúmenes siguen solo en memoria (al cerrar la app se pierde la entrevista, pero lo guardado persiste en Room).

### Enmienda: streaming y auto-scroll (2026-11-10)

7. **Puerto** (`PincheChatClient.kt`): `reply(turns, language, profile, onContent: (String) -> Unit = {})`. El valor por defecto se declara **en la interfaz**; las implementaciones solo añaden el parámetro (los 5 fakes citados arriba). El cliente real emite el texto visible de la ronda en curso; los fakes pueden emitir o no (los tests de streaming sí).
8. **Cliente** (`OpenAiCompatiblePincheChatClient.kt`): `buildBody` añade `"stream": true`. Si `Content-Type` no es `text/event-stream`, se lee el cuerpo completo y se procesa **como hoy** (fallback D12). Si es SSE, se leen líneas con `response.body.source().readUtf8Line()`: `data: {…}` → `PincheSseParser` (objeto **puro**, nuevo) acumula `choices[0].delta.content` y los fragmentos de `choices[0].delta.tool_calls` **por `index`** (`id`, `function.name` y `function.arguments` concatenado); `data: [DONE]` → fin. El parser devuelve el `message` con la forma que ya espera `PincheChatProtocol.toolCalls` (`tool_calls[].id` + `function.{name,arguments}`) y `messageContent`, de modo que **el bucle de rondas, el tope de 5 llamadas, `executeSaveProfile` y `parseReply` no cambian**. Por cada fragmento se emite `onContent(PincheChatProtocol.streamingText(contentAcumulado))` y **al empezar cada ronda se emite `onContent("")`** (el texto visible se reinicia, como hoy, que solo pinta la última ronda). La lectura se hace en `Dispatchers.IO` con la `Call` registrada para cancelarla si se cancela el coroutine (`invokeOnCompletion { call.cancel() }`), de modo que no quede ninguna lectura colgada; una `IOException` con el coroutine ya cancelado se propaga como `CancellationException`.
9. **Protocolo** (`PincheChatProtocol.kt`): `streamingText(content: String): String` — corta en el inicio del bloque ```` ```receta ```` si existe o, si no, en el primer `{` seguido de comillas (inicio de objeto JSON, **equilibrado o a medio formar**, para ocultar el JSON a medida que llega); sin ese candidato, devuelve el contenido entero. Nunca debe devolver JSON de la Propuesta. **Hallazgo de implementación**: se descarta cortar en el primer `{` en equilibrio genérico, porque un `{…}` en prosa (p. ej. «ajusta {el} formato») truncaría el preview sin ser JSON; un `{"` en prosa sí se corta y `parseReply` puede devolverlo después como texto (caso límite aceptado).
10. **ViewModel** (`PincheViewModel.kt`): `PincheUiState.streamingText: String? = null` (hilo IO → `MutableStateFlow.update`). `runRequest` pasa un `onContent` que actualiza el estado y **limpia** el streaming al terminar (éxito, fallo o key ausente). En el **éxito** no cambia nada: se retira el parcial y se añaden los mensajes de siempre (resumen «He guardado…» **antes** de la respuesta, D2 intacto). En el **fallo** se conserva el parcial (D11): si `streamingText` no está en blanco se añade como burbuja del asistente (con el texto ya cortado por `streamingText`, sin JSON) **y después** el mensaje estático de partes pendientes si procede; el aviso y «Reintentar» siguen igual (RD05). `retry()` limpia el parcial antes de reenviar.
11. **UI** (`PincheScreen.kt`): `PincheUiState.streamingText != null` pinta una burbuja transitoria del asistente tras el último mensaje (mismo `Surface`/`BubbleShape` que las demás, tag nuevo `PincheTestTags.STREAMING = "pinche_streaming_message"`); `ThinkingRow` se muestra mientras `isSending` **y** aún no haya fragmento. Scroll (D10, siempre al final): `LaunchedEffect(messages.size)` → `animateScrollToItem` al llegar un mensaje nuevo y `LaunchedEffect(streamingText)` → `scrollToItem` (instantáneo, para no encolar animaciones) con cada fragmento; el objetivo es el último índice de mensajes (o el de la burbuja transitoria si existe) y no se hace nada con la lista vacía (saludo). El trigger de `scrollToTopTick` (RF-MOB6) se conserva; borde aceptado: con texto llegando, tocar la pestaña activa para ir arriba volverá a bajar en el siguiente fragmento.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/profile/ProfileUpdate.kt` — create; `ProfileUpdate`, `SavedProfilePart` y `ProfileUpdateOutcome`.
- `app/src/main/java/com/whatsforlunch/app/domain/profile/UserProfileRules.kt` — modify; `applyUpdate` (sin tocar las reglas existentes).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — modify; tool `save_profile`, parseo de argumentos (amplía `toolCalls` a `toolCalls(message, argumentsById)` o función nueva `toolCallArguments`) y builders de resultado.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — modify; `PincheReplyResult.Success.profileSaved`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — modify; `UserProfileRepository`, ejecución de `save_profile` con caché por turno, `MAX_HTTP_CALLS = 5`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PromptSource.kt` — modify; el vocabulario es/en del tipo de alimentación pasa a fuente única (`DIET_TYPE_VOCABULARY`/`DIET_TYPE_ENUM_VALUES`) compartida por el bloque de perfil y la tool (añadido al listar tras la validación, hallazgo F3).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; `UserProfileRepository` como 6.º argumento de `OpenAiCompatiblePincheChatClient` (añadido al listar tras la validación, hallazgo F3).
- `app/src/main/assets/prompts/pinche_system_es.txt`, `pinche_system_en.txt` — modify; tres funciones.
- `app/src/main/assets/prompts/pinche_profile_es.txt`, `pinche_profile_en.txt` — modify; reglas de entrevista.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — modify; sin gate previo, mensaje `profileSaved`, guarda de Propuesta y degradación con petición estática.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — modify; `ProfileSavedContent`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify; `pinche_profile_saved` («He guardado en "Mi perfil": %1$s») y `pinche_profile_saved_none` («ninguna»/«none»); paridad exacta.
- Tests unit: `domain/profile/UserProfileRulesTest.kt` (ampliar), `data/ai/PincheChatProtocolTest.kt` (ampliar), `ui/pinche/PincheViewModelTest.kt` (reescribir los del gate RD26: guarda, resumen, degradación), `data/ai/PromptAssetsTest.kt` (reglas de entrevista en es/en).
- Tests instrumentados: `androidTest/.../PincheE2ETest.kt` (S1–S5 nuevas con `FakePincheChatClient` que devuelve `profileSaved` y persiste en el repositorio real de Room), `MyProfileE2ETest.kt` (verify 4 enmendado: ya no hay bloqueo sin IA).

**Delta de la enmienda (2026-11-10)**:

- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — modify; `onContent: (String) -> Unit = {}` en `reply`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheSseParser.kt` — create; parser SSE puro (líneas `data:`/`[DONE]`, acumulación de `content` y `tool_calls` por `index`, `message` reconstruido). *Provisional*: si se prefiere, puede vivir como función interna del cliente, pero debe quedar testeable en JVM.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — modify; `stream: true`, rama SSE + fallback no-SSE, emisión por fragmento y reset por ronda, cancelación de la `Call`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — modify; `streamingText`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — modify; `PincheUiState.streamingText`, emisión desde `runRequest`, congelado del parcial en fallo, limpieza en éxito/fallo/retry.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — modify; burbuja transitoria + `PincheTestTags.STREAMING` + scroll automático; `ThinkingRow` hasta el primer fragmento.
- Fakes (solo firma): `PincheE2ETest.kt`, `MyProfileE2ETest.kt`, `ShoppingListE2ETest.kt`, `ShoppingListFlowTest.kt`, `PincheViewModelTest.kt`.
- Tests unit: `data/ai/PincheChatProtocolTest.kt` (ampliar: `streamingText`), `data/ai/PincheSseParserTest.kt` (create), `ui/pinche/PincheViewModelTest.kt` (ampliar: streaming → final, orden del resumen, parcial conservado, limpieza en retry).
- Tests instrumentados: `androidTest/.../PincheE2ETest.kt` (nuevo S6: burbuja que crece y último mensaje visible sin scroll manual).
- `app/src/main/res/values*/strings.xml` — **sin cambios** (no hay claves nuevas: se reutiliza `pinche_thinking` y las burbujas existentes) → la paridad es/en queda en las mismas claves.

## Visual Design Impact

- UI involved: sí (solo la conversación del Pinche). Design source: `DESIGN.md` + patrón de `ProfileRequestContent` (mensaje localizado con acción).
- Screens or states affected: **Pinche** — nueva burbuja del asistente «He guardado en "Mi perfil": …» (lista de partes con etiquetas `my_profile_*`), mensaje de partes pendientes reutilizado como guarda y como degradación; el resto del chat, la Ficha y sus acciones sin cambios. «Mi perfil» sin cambios.
- New design artifact required: no — se reutilizan burbuja, `Button` de acción y etiquetas existentes.
- **Enmienda (2026-11-10)**: la burbuja en streaming reutiliza el estilo de burbuja del asistente (sin Ficha ni resumen hasta el final) y la lista pasa a seguir el final automáticamente. Sin artefacto de diseño nuevo; `DESIGN.md` solo recibe la nota en la fila del Pinche.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no cambian dominios, capas ni dependencias (una tool nueva dentro del patrón existente).
- `CONSTRAINTS.md`: not needed — sin reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — no cambia el flujo de trabajo ni los comandos.
- Other docs:
  - `docs/build-brief.md` — **update** (línea roja `⚠ S08`, líneas 39 y 75): la app expone dos funciones de solo lectura **y una función de escritura `save_profile` acotada al perfil**; si el proveedor no admite tools, la app falla visible sin cambiar datos en silencio (igual que hoy).
  - `docs/domain-model.md` — **update**: RD26 reescrita (sin gate previo; entrevista del Pinche al proponer con perfil incompleto; guarda determinista de Propuestas con partes pendientes) y RD27 nueva (`save_profile`: guardado directo validado por `UserProfileRules`, resumen fiel en el chat, resultado con lo guardado y lo pendiente). Casos límite al día.
  - `docs/risks-and-open-questions.md` — **update**: `S08` (function calling incluye una tool de escritura), `RK03` (el perfil puede rellenarse desde el chat: las respuestas del usuario son datos de salud que viajan al proveedor y el modelo los escribe en el dispositivo), `RK07` (presupuesto 3 → 5 llamadas por turno) y `Q-rellenar-perfil-con-el-pinche-1..N` (decisiones D1–D4).
  - `docs/technical-discovery.md` — **update**: contrato de 3 tools, tope 5, `save_profile` en el bloque de datos que salen del dispositivo.
  - `CONTEXT.md` — **update**: glosario «Mi perfil» (el Pinche lo rellena por chat antes de proponer) y, si procede, «Pinche».
  - `DESIGN.md` — **update**: burbuja «He guardado…» en la fila del Pinche.
  - `docs/specs/crear-mi-perfil.md`: **not needed** — no se reescribe (enmienda declarada aquí y en `notes` de `feature_list.json`).
  - `docs/technical-discovery.md` — **update** (enmienda): contrato del Pinche con **`stream: true` + SSE**, fallback cuando el proveedor no devuelve `text/event-stream`, parser puro sin dependencias nuevas y `MAX_HTTP_CALLS = 5` intacto; nota del auto-scroll en la fila del chat.
  - `DESIGN.md` — **update** (enmienda, mínimo): fila del Pinche con la burbuja en streaming y el scroll automático al último mensaje.
  - `docs/domain-model.md` — **not needed** (enmienda): RD17 (solo lectura, sin persistencia, fallo con «Reintentar») no cambia; el streaming es presentación, no una regla de dominio.

## Implementation Plan

1. Dominio: `ProfileUpdate` + `UserProfileRules.applyUpdate` con tests puros.
2. Protocolo: tool `save_profile` (JSON, parseo de argumentos con vocabulario es/en, resultados `saved`/`pending`/`error`) + `PincheChatProtocolTest`.
3. Cliente: ejecución y persistencia con `UserProfileRepository`, `profileSaved` en el resultado y `MAX_HTTP_CALLS = 5` + tests del executor con `InMemoryUserProfileRepository`.
4. Prompts: `pinche_system_*` (tres funciones) y `pinche_profile_*` (reglas de entrevista) + `PromptAssetsTest`.
5. ViewModel/UI: retirar el gate, resumen «He guardado…», guarda de Propuesta y degradación; strings es/en; `PincheViewModelTest` reescrito.
6. E2E: `PincheE2ETest` con los escenarios nuevos, `MyProfileE2ETest` (verify 4) enmendado, gates completos y docs durables al día.

**Plan de la enmienda (2026-11-10)**:

7. Protocolo y parser puros: `PincheChatProtocol.streamingText` + `PincheSseParser` con sus tests JVM (sin dependencias nuevas).
8. Puerto y cliente: `onContent` en `reply`, `stream: true` con rama SSE y fallback no-SSE, emisión por fragmento con reset por ronda y cancelación de la `Call`; actualiza los 5 fakes.
9. ViewModel y UI: `streamingText` en el estado, burbuja transitoria, congelado del parcial en fallo, scroll automático y `ThinkingRow`; tests unit + E2E S6; gates completos, docs durables y evidencia.

## Implementation Tasks

- [x] T1: `ProfileUpdate`/`ProfileUpdateOutcome` y `UserProfileRules.applyUpdate` (validación `invalid_chip`/`duplicate_chip`/`unknown_diet_type`/`invalid_arguments`, merge de campos omitidos) con `UserProfileRulesTest` ampliado.
- [x] T2: `PincheChatProtocol` con `save_profile` (toolsJson, parseo de argumentos —incluido extraer los argumentos del `tool_call`— y builders `saved`/`pending`/`error`) con `PincheChatProtocolTest` ampliado.
- [x] T3: `OpenAiCompatiblePincheChatClient` ejecuta `save_profile` (caché por turno, persistencia, errores sin persistir), `Success.profileSaved` y `MAX_HTTP_CALLS = 5`; tests del executor con repo InMemory.
- [x] T4: prompts `pinche_system_*`/`pinche_profile_*` actualizados (tres funciones + entrevista D1/D4/D6) y `PromptAssetsTest`.
- [x] T5: `PincheViewModel` sin gate previo + `PincheMessage.profileSaved` + guarda de Propuesta + degradación; `PincheScreen.ProfileSavedContent`; strings `pinche_profile_saved*` es/en con paridad; `PincheViewModelTest` reescrito.
- [x] T6: E2E — `PincheE2ETest` (S1–S5) y `MyProfileE2ETest` (verify 4 enmendado); batería sin regresión.
- [x] T7: docs durables (`build-brief.md` S08, `domain-model.md` RD26/RD27, `risks-and-open-questions.md`, `technical-discovery.md`, `CONTEXT.md`, `DESIGN.md`).

**Tareas de la enmienda (2026-11-10, completadas)**:

- [x] T8: `PincheChatProtocol.streamingText` + `PincheSseParser` (acumulación de `content` y `tool_calls` por `index`, `[DONE]`, JSON ilegible → `invalid` y sin mensaje) con `PincheChatProtocolTest` y `PincheSseParserTest`.
- [x] T9: `PincheChatClient.reply(..., onContent)` + `OpenAiCompatiblePincheChatClient` con `stream: true`, rama SSE, fallback no-SSE, reset por ronda y cancelación de la `Call`; firma de los 5 fakes.
- [x] T10: `PincheUiState.streamingText`, burbuja transitoria (`PincheTestTags.STREAMING`), congelado del parcial en fallo (D11), limpieza en éxito/fallo/retry y scroll siempre al final (D10); `PincheViewModelTest` ampliado.
- [x] T11: E2E S6 (`PincheE2ETest`) + gates completos (init/lint/unit/assemble/paridad) + docs durables (`technical-discovery.md`, `DESIGN.md`) + evidencia. **Verify manual con mock SSE local: no ejecutado** (decisión del usuario 2026-11-10: saltarlo y anotar el hueco — ver «Hallazgos de implementación»).

## Verification Plan

- `./init.sh` / `./init.ps1` — entorno listo (checks no bloqueantes; sin servidores largos; el falso negativo de `monkey`/`pidof` se verifica aparte con `adb shell pidof`; con varios AVDs usar `ANDROID_SERIAL`).
- `./gradlew lintDebug` — 0 errores y sin warnings nuevos sobre la línea base.
- `./gradlew testDebugUnitTest` — todo en verde, incluidos `UserProfileRulesTest`, `PincheChatProtocolTest`, `PincheViewModelTest` y `PromptAssetsTest` ampliados.
- `./gradlew assembleDebug` y `compileDebugAndroidTestKotlin` — OK.
- Paridad es/en de `strings.xml` — mismas claves en `values` y `values-en` (línea base actual 340 + 5 plurales).
- E2E focalizada (`connectedDebugAndroidTest`, fixtures locales y `FakePincheChatClient`; **sin llamadas de pago**): `PincheE2ETest` con S1 (entrevista de una en una → resumen «He guardado…» → Ficha al completar, y «Mi perfil» muestra lo guardado), S2 (duda sin perfil responde sin entrevista), S3 (perfil a medio completar: solo pide lo pendiente, sin perder lo guardado), S4 (Propuesta con pendientes → sin Ficha + mensaje con «Abrir Mi perfil» que navega), S5 (sin key → aviso + mensaje con «Abrir Mi perfil») + `MyProfileE2ETest` (verify 4 enmendado) + regresión `ShoppingListE2ETest`/`BottomBarE2ETest`.
- Unit de `applyUpdate`/protocolo: chip vacía o duplicada y tipo desconocido → `{"error": …}` sin persistir; éxito → `{"saved", "pending"}` correcto y `pendingParts` vacío al completar (verify 4 de `feature_list.json`).
- Batería E2E completa (`./e2e.sh` / `e2e.ps1`) sin regresión sobre el inventario actual.
- Comprobación manual con `adb`/uiautomator: resumen del chat coincide con lo que muestra «Mi perfil» tras la entrevista (datos ficticios: «Frutos secos», «Diabetes», «Vegana»).
- Opcional (solo con confirmación humana y llamada de pago): una entrevista real con el alias `chat` comprueba que el modelo pregunta de una en una y llama a `save_profile`; no bloquea la feature.

**Verificación de la enmienda (2026-11-10)**:

- `./gradlew testDebugUnitTest` incluye: `PincheSseParserTest` (fragmentos de `content` sueltos y concatenados, `tool_calls` llegando en varios `delta` con `index`, cierre con `[DONE]`, JSON ilegible → `invalid` y sin mensaje) y `PincheChatProtocolTest.streamingText` (texto plano íntegro, corte en ```` ```receta ````, corte en `{"` —JSON equilibrado o a medio formar—, y `{…}` en prosa sin comillas que **no** se corta).
- `PincheViewModelTest`: fake que emite varios fragmentos → `streamingText` se actualiza en orden; éxito → el parcial desaparece, el resumen «He guardado…» queda **antes** de la respuesta y la Ficha se pinta; fallo a mitad → **se conserva** el parcial como burbuja del asistente + aviso «Reintentar»; `retry()` limpia el parcial.
- E2E focalizada `PincheE2ETest` (S6, cliente simulado que emite fragmentos): la burbuja `pinche_streaming_message` aparece con el primer fragmento, crece y se sustituye por la respuesta final; el último mensaje queda visible **sin** arrastrar el `LazyColumn`. Regresión de S1–S5 (ninguna debe cambiar) y `MyProfileE2ETest`.
- Verify manual con **mock SSE local** (`mock_pinche_server.py` de la sesión 121 ampliado a `stream: true`, `AI_BASE_URL` transitoria revertida después): burbuja creciendo, sustitución al final, scroll al llegar un mensaje y corte simulado conservando el parcial. **Sin llamadas de pago** (D13); queda anotado como pendiente confirmar que el alias `chat` admite `stream` (el fallback lo cubre).
- Paridad es/en sin cambios de claves (mismo recuento que la línea base 340/340 + 5/5) y `lintDebug` sin warnings nuevos.

## Evidence To Capture

- Salidas de `lintDebug`, `testDebugUnitTest`, `assembleDebug` y recuento de paridad es/en en `docs/evidence/rellenar-perfil-con-el-pinche/`.
- XML/resultado de la focalizada `PincheE2ETest` (S1–S5) y de la batería, con recuento por clase.
- 2–3 capturas es-ES 1280×2856: entrevista con la pregunta del Pinche y el resumen «He guardado…», «Mi perfil» con lo guardado y la degradación con «Abrir Mi perfil».
- Resumen en `docs/evidence/rellenar-perfil-con-el-pinche/informe.md` y `evidence` de `feature_list.json`.

**Evidencia de la enmienda**: salidas de los gates re-ejecutados, XML del S6 focalizado, y 1–2 capturas es-ES 1280×2856 con el mock SSE local (burbuja a medio llegar y respuesta final con la lista abajo al último mensaje); nota del fallback/pendiente del proveedor en el informe.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (única tool nueva `save_profile` acotada al perfil; sin tocar Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias; sin permisos ni dependencias nuevas; Room sin cambios de esquema).
- [ ] Acceptance scenarios pass (S1–S10, verify 1–4 de `feature_list.json`).
- [ ] Verification evidence is present (`docs/evidence/rellenar-perfil-con-el-pinche/` con gates, E2E, capturas y paridad).
- [ ] Persistent E2E coverage added/updated (`PincheE2ETest` S1–S6 con cliente simulado; `MyProfileE2ETest` verify 4 enmendado) o justificación si no procede.
- [ ] `feature_list.json` y `PROGRESS.md` updated correctly (solo `status`/`evidence` en la lista).
- [ ] No unrelated product behavior or extra feature work was added.
- [ ] Seguridad/privacidad: sin secretos ni PII reales en diff/evidencia; evidencia solo con datos ficticios; `RK03`/`S08`/`RK07` actualizados; guardado validado por `UserProfileRules` (sin datos de salud inventados persistidos).

**Checklist de la enmienda (2026-11-10)**:

- [ ] Scenarios 8–10 pasan (streaming visible sin JSON de la Propuesta, fallo/corte con parcial conservado y fallback no-SSE, scroll siempre al último mensaje).
- [ ] Sin dependencias nuevas (`build.gradle.kts` intacto), sin claves nuevas de `strings.xml` (paridad sin cambios) y sin cambios en `MAX_HTTP_CALLS`, tools, RD05/RD17 ni Room.
- [ ] El resto de superficies de IA (Escáner, Sugerencias, imagen) sigue sin streaming.
- [ ] Evidencia de la enmienda presente (gates, XML del S6, capturas con mock local) y sin llamadas de pago.
- [ ] `feature_list.json` solo con `status`/`evidence` y `PROGRESS.md` al día.

## Decisiones del usuario (2026-11-09)

- **D1 — La entrevista solo se activa al proponer receta**: las dudas de cocina se responden sin perfil; la puerta se muda del gate de `runRequest` al system prompt, con guarda determinista en la app (S4: nunca se pinta una Propuesta con partes pendientes).
- **D2 — Guardado directo con resumen visible**: `save_profile` persiste al invocarse y el resumen «He guardado…» lo pinta la app con lo realmente guardado (sin confirmación previa).
- **D3 — Tope de llamadas HTTP por turno 3 → 5** para la entrevista multi-turno.
- **D4 — Chips literales en el idioma de la conversación** (es/en), sin traducir al idioma de la interfaz.

Derivadas: **D5** `save_profile` es la única tool de escritura y actualiza solo los campos que recibe (permitiendo correcciones); **D6** las reglas de entrevista viven en el bloque de perfil del system prompt y el resultado del tool incluye lo pendiente; **D7** los mensajes estáticos `profileRequest` pasan a ser solo guarda y degradación (la entrevista viaja como mensajes normales); **D8** el tipo de alimentación se parsea con el vocabulario cerrado es/en de `DietType` y lo desconocido se rechaza al modelo.

## Hallazgos de implementación (2026-11-09)

- **Parseo de argumentos**: para respetar la firma `parseSaveProfileArgs(json): ProfileUpdate?` y a la vez distinguir `unknown_diet_type` de `invalid_arguments`, el parseo interno devuelve `ParsedArgs(update, error)` y se exponen dos helpers: `parseSaveProfileArgs` (`null` = no usable) y `saveProfileArgsError` (código preciso cuando el primero da `null`). El cliente los combina para `saveProfileToolError`.
- **Resultado de `save_profile`**: `saved.diet_type` se devuelve con el **nombre del enum** (`VEGAN`), lenguaje neutro coherente con `pending: ["ALLERGIES", …]`; las secciones se devuelven con los nombres de sus argumentos (`allergies`, `allergies_none`, …) y sus **valores finales**.
- **Tests del executor**: el executor es `internal fun executeSaveProfile(arguments, state)` sobre `PincheToolTurnState` (cachés por turno + partes acumuladas) y se prueba en el fichero nuevo `app/src/test/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClientTest.kt` con `InMemoryUserProfileRepository` (sin red).
- **E2E**: el `FakePincheChatClient` de `PincheE2ETest` acepta un `UserProfileRepository` opcional y, cuando la respuesta declara `profileSaved`, persiste con `UserProfileRules.applyUpdate` (simula el executor real); el verify 4 de `MyProfileE2ETest` sustituye el cliente de Koin con `loadModules(..., allowOverride = true)` por uno en modo «sin key» (patrón ya usado por `CreateRecipeAppE2ETest` con `ImageGenerator`), de modo que **ninguna E2E usa el cliente real ni hace llamadas de red**.
- **TestTag nuevo**: `PincheTestTags.PROFILE_SAVED` (`pinche_profile_saved`) para el resumen; las E2E sincronizan con el estado del ViewModel y usan `performScrollToIndex` porque el chat no hace auto-scroll.

## Decisiones de la enmienda (2026-11-10)

- **D9 — Respuesta del LLM en streaming**: el cuerpo de `/chat/completions` se pide con `stream: true` y se procesa como SSE; el texto del asistente se muestra **a medida que llega**, cortado para nunca pintar el JSON de la Propuesta. El contrato de tools y el tope de llamadas no cambian.
- **D10 — La lista siempre baja al último mensaje**: cualquier mensaje nuevo o fragmento de streaming lleva el `LazyColumn` al final, aunque el usuario esté leyendo arriba. Tocar la pestaña activa sigue subiendo (RF-MOB6) mientras no haya texto llegando (borde aceptado: durante el streaming vuelve a bajar en el siguiente fragmento).
- **D11 — Si el stream se corta a mitad, se conserva el texto parcial**: se congela como burbuja del asistente (ya cortado, sin JSON) y se muestra el aviso de fallo con «Reintentar» (RD05); en un reintento ese texto viaja como turno del asistente en el contexto.
- **D12 — Fallback sin SSE**: si la respuesta no es `text/event-stream` se procesa el cuerpo completo como hasta hoy; la app no depende de que el alias `chat` admita `stream`.
- **D13 — Verificación solo con mock local**: sin llamadas de pago; confirmar con el proveedor que `stream: true` está soportado queda como pendiente anotado en `docs/technical-discovery.md` (lo cubre D12).

Derivadas: **D14** el texto visible por ronda se reinicia al empezar una ronda nueva (equivalente al comportamiento actual, que solo pinta la última ronda); **D15** el puerto gana `onContent` con valor por defecto para no romper los fakes más allá de la firma; **D16** sin dependencias nuevas: el parser SSE es código propio testeable en JVM (no hay MockWebServer en el proyecto).

## Hallazgos de implementación de la enmienda (2026-11-10)

- **Corte del preview (`streamingText`)**: se descartó cortar en el primer `{` en equilibrio genérico (era el planteamiento de la spec) porque `{…}` en prosa truncaba el preview sin ser JSON; se corta solo en ```` ```receta ```` o en el primer `{` **seguido de comillas**. La spec quedó ajustada en el punto 9 del *Technical Approach* y en su plan de verificación.
- **Semántica de `onContent`**: recibe el **texto visible acumulado de la ronda** (recortado con `streamingText`), no un delta. Los fakes de test acumulan los deltas y los recortan con `trim()` (el cliente real aplica además el corte de `streamingText`); los tests unit asientan el contrato de «texto visible acumulado».
- **Fakes**: los 5 solo añaden el parámetro a la firma (el defecto vive en la interfaz). En `PincheE2ETest` los fragmentos se emiten **solo en la última tanda** (la que trae la respuesta final), o los escenarios previos los reproducirían.
- **E2E determinista con gates**: el `Surface` de la burbuja **no fusiona** el `Text` (hay que asertar con `onNodeWithText`, no con `assertTextContains` sobre el tag) y el simulador se detiene con dos `CompletableDeferred` —antes del último fragmento y antes del resultado— para poder asertar la burbuja sin carreras; `waitUntilDisplayed()` reintenta `assertIsDisplayed()` porque el scroll automático sigue animándose cuando llega el mensaje.
- **Verify manual con mock SSE: no ejecutado (hueco documentado, decisión del usuario 2026-11-10)**. Levantar el mock exige cambiar `AI_BASE_URL`/`AI_API_KEY` en `local.properties` (fichero con la API key real; `AGENTS.md §5` prohíbe tocar ficheros con secretos sin autorización) más `usesCleartextTraffic` transitorio. **Consecuencia**: la lectura SSE real por HTTP (`readStream` en `OpenAiCompatiblePincheChatClient`) **no está probada de extremo a extremo**; los unit cubren `PincheSseParser` y `streamingText`, y el ramo **sin SSE (fallback D12) está implementado y revisado por inspección, sin test que lo ejecute** — recomendación **L1** del validador: test JVM con interceptor OkHttp falso que cubra SSE, no-SSE e `IOException` (sin dependencias, secretos ni emulador). Pendiente: repetir el verify con mock local cuando el usuario lo autorice.
- **Batería E2E**: 245 tests, **2 omitidos** (`SettingsE2ETest`/`Assume`) y **1 fallo flake ajeno** —`ShoppingListE2ETest.twoRecipesAddUpAndTheBadgeCountsSources`, `ComposeNotIdleException` (60 s) en una batería lenta de 59 min—; rerun aislado **1/1 verde** y clase completa **27/27 verde** tras reiniciar el AVD (el primer rerun se cortó porque el emulador se cayó, como en sesiones anteriores). El XML de la batería se sobrescribió con los reruns; el registro completo está en el log conservado en la evidencia.
