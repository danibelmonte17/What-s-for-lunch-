# Feature Implementation Spec: Rellenar Mi perfil conversando con el Pinche

## Source Feature

- `id`: rellenar-perfil-con-el-pinche
- `area`: funcional
- `depends_on`: `crear-mi-perfil` (accepted), `resolver-dudas-con-pinche` (accepted)
- `status`: not_started (a la hora de planificar; 2026-11-09)
- `source`: `feature_list.json`

## Goal

Cuando el Pinche va a proponer una receta y faltan partes del perfil, en lugar de bloquear con un mensaje estático las pide en el propio chat —una pregunta a la vez y solo por lo que falta— y va guardando cada respuesta en «Mi perfil» con una única función de escritura `save_profile`, mostrando un resumen con lo realmente guardado. En cuanto el perfil queda completo continúa con lo que el usuario pedía y propone la receta respetándolo. Las dudas de cocina que no llevan propuesta se siguen respondiendo sin perfil. Si la IA no está disponible, se conserva el mensaje estático de partes pendientes con «Abrir Mi perfil».

## Non-Goals

- Ampliar el consumo del perfil a Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias (sigue siendo solo el Pinche).
- Añadir `get_profile` u otras tools: el perfil viaja en el system prompt y la **única** tool nueva es `save_profile`, acotada al perfil.
- Confirmación previa del guardado ni edición del perfil desde el chat (D2: guardado directo + resumen; editar sigue siendo en «Mi perfil»).
- Reescribir la spec ni la evidencia histórica de `crear-mi-perfil`: su comportamiento RD26/D8 queda enmendado desde esta spec y desde las `notes` de `feature_list.json`.
- Cambios de esquema de Room, de la pantalla «Mi perfil» ni de sus reglas (`UserProfileRules` solo se amplía, no se cambia).
- Traducir las chips del usuario, voz, cuentas, sincronización ni exportación del perfil.
- Verificación con el modelo real de pago: todo con cliente simulado; una pasada real solo sería un verify manual opcional con confirmación humana previa.

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

## Technical Approach

1. **Dominio** (`domain/profile/`): data class `ProfileUpdate(allergies: List<String>?, allergiesNone: Boolean?, intolerances: List<String>?, intolerancesNone: Boolean?, dietType: DietType?)` (campos omitidos = sin tocar) y `UserProfileRules.applyUpdate(profile, update): ProfileUpdateOutcome` — `Saved(updated, savedParts)` / `Invalid(code)` — que compone las reglas existentes: por sección, `*_none = true` exige chips ausentes/vacías y usa `setNone` (limpia); si llegan chips, se fija la lista completa tras validar cada una (`normalizeChip` → `invalid_chip`) y sin duplicados por sección ignorando mayúsculas (`duplicate_chip`); `diet_type` ya mapeado al enum (`unknown_diet_type` si no); al menos un campo (`invalid_arguments`). `savedParts` lista las partes escritas con sus valores finales y `pendingParts(updated)` da lo que falta.
2. **Protocolo** (`PincheChatProtocol`): tercera tool `save_profile` («write the user's Mi perfil sections from what the user just said; only call it when the user states their data») con JSON Schema de los 5 campos opcionales (`additionalProperties: false`), `parseSaveProfileArgs(json): ProfileUpdate?` con el vocabulario cerrado es/en de `DietType` (omnívora/omnivore, pescetariana/pescetarian, vegetariana/vegetarian, vegana/vegan, cetogénica/ketogenic — mismo vocabulario que `renderProfilePrompt`), `saveProfileToolResult(savedParts, pending)` → `{"saved": {...}, "pending": ["ALLERGIES", ...]}` y `saveProfileToolError(code)` → `{"error": code}`. El resultado lleva lo pendiente para que el modelo sepa qué preguntar después.
3. **Cliente** (`OpenAiCompatiblePincheChatClient`): gana `UserProfileRepository`. Al ejecutar `save_profile` lee el perfil vigente (una vez por turno, con caché actualizada tras cada guardado, patrón de `pantrySnapshot`), aplica `applyUpdate`, persiste con `save()` y devuelve el resultado; los errores del outcome viajan como `{"error": …}` sin persistir. `PincheReplyResult.Success` gana `profileSaved: List<SavedProfilePart>?` (partes escritas en el turno, valores finales acumulados de todas las invocaciones). **`MAX_HTTP_CALLS` 3 → 5** (D3; actualiza el comentario D6 y `RK07`).
4. **Prompt** (assets): `pinche_system_{es,en}.txt` pasa a describir **tres** funciones (las dos de solo lectura + `save_profile` de escritura acotada al perfil). `pinche_profile_{es,en}.txt` gana las reglas de entrevista (D1/D6): si una parte está «sin declarar»/«sin elegir» y vas a proponer una receta, **no propongas todavía**: pregunta por **una sola** parte pendiente por mensaje, guarda con `save_profile` lo que el usuario diga (chips en el idioma de la conversación, sin traducir; tipo de alimentación de la lista fija) y, cuando no quede nada pendiente, continúa con la petición original y propón. Las dudas de cocina sin propuesta se responden sin entrevistar.
5. **ViewModel/UI** (`ui/pinche/`): se retira el gate de `runRequest` (siempre que haya texto se llama al cliente). En el éxito: (a) si `profileSaved != null`, mensaje del asistente tipo `profileSaved` (nuevo campo de `PincheMessage`, texto en blanco, patrón `profileRequest`) con el resumen «He guardado en “Mi perfil”: …» compuesto en la UI con las etiquetas `my_profile_*` + valores literales + «ninguna»; (b) **guarda D1**: si la respuesta lleva Propuesta y `UserProfileRules.pendingParts(currentProfile())` no está vacío, **no se pinta la Ficha** y se añade el mensaje estático de partes pendientes con «Abrir Mi perfil» (texto de la respuesta sí se conserva); (c) si falta key o falla la llamada con partes pendientes, además del aviso de siempre se añade ese mensaje estático (degradación, S5). `PincheScreen` gana `ProfileSavedContent` (mismo patrón que `ProfileRequestContent`).
6. **Casos límite**: varios `save_profile` en un turno → se acumulan y sale un único resumen; `save_profile` sobre una sección ya completa la sustituye (permite correcciones); el usuario que evade preguntas nunca ve una Propuesta (la guarda es determinista) y puede completar por chat o en «Abrir Mi perfil»; conversación y resúmenes siguen solo en memoria (al cerrar la app se pierde la entrevista, pero lo guardado persiste en Room).

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

## Visual Design Impact

- UI involved: sí (solo la conversación del Pinche). Design source: `DESIGN.md` + patrón de `ProfileRequestContent` (mensaje localizado con acción).
- Screens or states affected: **Pinche** — nueva burbuja del asistente «He guardado en "Mi perfil": …» (lista de partes con etiquetas `my_profile_*`), mensaje de partes pendientes reutilizado como guarda y como degradación; el resto del chat, la Ficha y sus acciones sin cambios. «Mi perfil» sin cambios.
- New design artifact required: no — se reutilizan burbuja, `Button` de acción y etiquetas existentes.

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

## Implementation Plan

1. Dominio: `ProfileUpdate` + `UserProfileRules.applyUpdate` con tests puros.
2. Protocolo: tool `save_profile` (JSON, parseo de argumentos con vocabulario es/en, resultados `saved`/`pending`/`error`) + `PincheChatProtocolTest`.
3. Cliente: ejecución y persistencia con `UserProfileRepository`, `profileSaved` en el resultado y `MAX_HTTP_CALLS = 5` + tests del executor con `InMemoryUserProfileRepository`.
4. Prompts: `pinche_system_*` (tres funciones) y `pinche_profile_*` (reglas de entrevista) + `PromptAssetsTest`.
5. ViewModel/UI: retirar el gate, resumen «He guardado…», guarda de Propuesta y degradación; strings es/en; `PincheViewModelTest` reescrito.
6. E2E: `PincheE2ETest` con los escenarios nuevos, `MyProfileE2ETest` (verify 4) enmendado, gates completos y docs durables al día.

## Implementation Tasks

- [x] T1: `ProfileUpdate`/`ProfileUpdateOutcome` y `UserProfileRules.applyUpdate` (validación `invalid_chip`/`duplicate_chip`/`unknown_diet_type`/`invalid_arguments`, merge de campos omitidos) con `UserProfileRulesTest` ampliado.
- [x] T2: `PincheChatProtocol` con `save_profile` (toolsJson, parseo de argumentos —incluido extraer los argumentos del `tool_call`— y builders `saved`/`pending`/`error`) con `PincheChatProtocolTest` ampliado.
- [x] T3: `OpenAiCompatiblePincheChatClient` ejecuta `save_profile` (caché por turno, persistencia, errores sin persistir), `Success.profileSaved` y `MAX_HTTP_CALLS = 5`; tests del executor con repo InMemory.
- [x] T4: prompts `pinche_system_*`/`pinche_profile_*` actualizados (tres funciones + entrevista D1/D4/D6) y `PromptAssetsTest`.
- [x] T5: `PincheViewModel` sin gate previo + `PincheMessage.profileSaved` + guarda de Propuesta + degradación; `PincheScreen.ProfileSavedContent`; strings `pinche_profile_saved*` es/en con paridad; `PincheViewModelTest` reescrito.
- [x] T6: E2E — `PincheE2ETest` (S1–S5) y `MyProfileE2ETest` (verify 4 enmendado); batería sin regresión.
- [x] T7: docs durables (`build-brief.md` S08, `domain-model.md` RD26/RD27, `risks-and-open-questions.md`, `technical-discovery.md`, `CONTEXT.md`, `DESIGN.md`).

## Verification Plan

- `./init.sh` / `./init.ps1` — entorno listo (checks no bloqueantes; sin servidores largos; el falso negativo de `monkey`/`pidof` se verifica aparte con `adb shell pidof`; con varios AVDs usar `ANDROID_SERIAL`).
- `./gradlew lintDebug` — 0 errores y sin warnings nuevos sobre la línea base.
- `./gradlew testDebugUnitTest` — todo en verde, incluidos `UserProfileRulesTest`, `PincheChatProtocolTest`, `PincheViewModelTest` y `PromptAssetsTest` ampliados.
- `./gradlew assembleDebug` y `compileDebugAndroidTestKotlin` — OK.
- Paridad es/en de `strings.xml` — mismas claves en `values` y `values-en` (hoy 338 + 5 plurales).
- E2E focalizada (`connectedDebugAndroidTest`, fixtures locales y `FakePincheChatClient`; **sin llamadas de pago**): `PincheE2ETest` con S1 (entrevista de una en una → resumen «He guardado…» → Ficha al completar, y «Mi perfil» muestra lo guardado), S2 (duda sin perfil responde sin entrevista), S3 (perfil a medio completar: solo pide lo pendiente, sin perder lo guardado), S4 (Propuesta con pendientes → sin Ficha + mensaje con «Abrir Mi perfil» que navega), S5 (sin key → aviso + mensaje con «Abrir Mi perfil») + `MyProfileE2ETest` (verify 4 enmendado) + regresión `ShoppingListE2ETest`/`BottomBarE2ETest`.
- Unit de `applyUpdate`/protocolo: chip vacía o duplicada y tipo desconocido → `{"error": …}` sin persistir; éxito → `{"saved", "pending"}` correcto y `pendingParts` vacío al completar (verify 4 de `feature_list.json`).
- Batería E2E completa (`./e2e.sh` / `e2e.ps1`) sin regresión sobre el inventario actual.
- Comprobación manual con `adb`/uiautomator: resumen del chat coincide con lo que muestra «Mi perfil» tras la entrevista (datos ficticios: «Frutos secos», «Diabetes», «Vegana»).
- Opcional (solo con confirmación humana y llamada de pago): una entrevista real con el alias `chat` comprueba que el modelo pregunta de una en una y llama a `save_profile`; no bloquea la feature.

## Evidence To Capture

- Salidas de `lintDebug`, `testDebugUnitTest`, `assembleDebug` y recuento de paridad es/en en `docs/evidence/rellenar-perfil-con-el-pinche/`.
- XML/resultado de la focalizada `PincheE2ETest` (S1–S5) y de la batería, con recuento por clase.
- 2–3 capturas es-ES 1280×2856: entrevista con la pregunta del Pinche y el resumen «He guardado…», «Mi perfil» con lo guardado y la degradación con «Abrir Mi perfil».
- Resumen en `docs/evidence/rellenar-perfil-con-el-pinche/informe.md` y `evidence` de `feature_list.json`.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (única tool nueva `save_profile` acotada al perfil; sin tocar Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias; sin permisos ni dependencias nuevas; Room sin cambios de esquema).
- [ ] Acceptance scenarios pass (S1–S7, verify 1–4 de `feature_list.json`).
- [ ] Verification evidence is present (`docs/evidence/rellenar-perfil-con-el-pinche/` con gates, E2E, capturas y paridad).
- [ ] Persistent E2E coverage added/updated (`PincheE2ETest` S1–S5 con cliente simulado; `MyProfileE2ETest` verify 4 enmendado) o justificación si no procede.
- [ ] `feature_list.json` y `PROGRESS.md` updated correctly (solo `status`/`evidence` en la lista).
- [ ] No unrelated product behavior or extra feature work was added.
- [ ] Seguridad/privacidad: sin secretos ni PII reales en diff/evidencia; evidencia solo con datos ficticios; `RK03`/`S08`/`RK07` actualizados; guardado validado por `UserProfileRules` (sin datos de salud inventados persistidos).

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
