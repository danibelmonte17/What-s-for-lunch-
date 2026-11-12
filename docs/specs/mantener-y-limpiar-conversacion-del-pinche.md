# Feature Implementation Spec: Mantener la conversación con el Pinche y poder limpiarla

## Source Feature

- `id`: mantener-y-limpiar-conversacion-del-pinche
- `area`: funcional
- `depends_on`: resolver-dudas-con-pinche, rellenar-perfil-con-el-pinche (ambas `accepted`)
- `status`: not_started
- `source`: `feature_list.json`

## Goal

La conversación con el Pinche sobrevive a la navegación mientras la app está abierta: cambiar de pestaña o abrir otra pantalla y volver no la borra (sigue solo en memoria, sin persistencia en disco). Además, la top bar de la pantalla Pinche gana un botón que limpia la conversación actual y devuelve la pantalla a su estado inicial para empezar otra vez.

## Non-Goals

- Persistir la conversación entre sesiones de la app (sigue fuera del MVP, `docs/build-brief.md:60`) ni en Room ni en ficheros.
- Dictado por voz en el Pinche (`⚠ S04`, diferido).
- Cambiar el contrato del Pinche con el proveedor (tools, prompts, `save_profile`, topes de llamadas): intacto.
- Tocar «Mi perfil», sus reglas (`UserProfileRules`, RD25-RD27) ni la lógica de entrevista del perfil.
- Historial de conversaciones múltiples: hay una sola conversación viva; el botón la limpia, no crea archivos de chat.

## Job Story

When estoy conversando con el Pinche y navego por la app, o quiero empezar de cero,
I want to que la conversación me acompañe al cambiar de pestaña y volver, y poder limpiarla desde la top bar,
so I can no perder el hilo de lo que ya pregunté y reiniciar cuando quiera sin cerrar la app.

## Users And Permissions

- Usuario único sin cuentas ni sesión (uso personal, 1-5 usuarios): toda la funcionalidad es local; sin permisos nuevos, sin red salvo la llamada de IA ya existente.

## Decisiones

- **D1** (usuario, 2026-11-11, en `feature_list.json`): la conversación se mantiene **mientras dura la ejecución de la app**, en memoria, sin persistencia entre sesiones.
- **D2** (usuario, 2026-11-11, en `feature_list.json`): un botón en la top bar de Pinche limpia la conversación actual y empieza una nueva desde el estado inicial.
- **D3** (usuario, 2026-11-11): el botón es un **`IconButton` con icono** (papelera/limpiar) en la top bar, con **content-description localizada** es/en. Al ser solo de icono, su «texto» accesible es la content-description (el `verification` de `feature_list.json` se cubre comprobándola en ambos idiomas).
- **D4** (usuario, 2026-11-11): **limpieza directa**, sin diálogo de confirmación y sin «Deshacer».
- **D5** (usuario, 2026-11-11): con una **petición en curso (streaming)**, el botón está activo y al pulsarlo **se cancela la petición y se limpia**; los fragmentos o resultados que lleguen tarde se descartan.
- **D6** (usuario, 2026-11-11): la limpieza **no toca «Mi perfil»**: lo guardado se conserva y una conversación nueva retoma la entrevista solo por las partes que sigan pendientes (la completitud se deriva del perfil persistido, RD26).
- **D7** (usuario, 2026-11-11): el botón se muestra **solo cuando hay conversación** (algo que limpiar); con la conversación vacía (saludo + chips) no aparece.
- **D8** (derivada): el estado de la conversación se ancla a la **Activity** — `PincheViewModel` obtenido con `koinViewModel(viewModelStoreOwner = <actividad>)` en `PincheRoute`, no al `NavBackStackEntry` — para que sobreviva a `openTab` (que destruye la entrada `pinche` del back stack). Vive mientras el proceso de la app; al cerrarla se pierde, coherente con D1. Se descarta un holder singleton en Koin (más piezas, mismo resultado observable para el usuario). **Hallazgo de implementación**: `LocalActivity.current` deriva su valor de `LocalContext` y devuelve `null` en composiciones cuyo contexto no es la Activity (patrón `showApp` de los E2E, que provee un contexto de app) — en ese caso se cae al owner por defecto (`koinViewModel()` sin argumentos), conservando el comportamiento anterior; la app real siempre resuelve la Activity.
- **D9** (derivada): `clearConversation()` cancela el `Job` de la petición en curso, incrementa un contador de **generación** de la conversación y resetea el estado a `PincheUiState()` inicial (mensajes, `input`, `isSending`, `notice`, `lastUserText` y `streamingText`). Los mensajes con Propuesta guardada o «Añadido a la Lista» se borran de la vista; la Receta ya guardada en Mis Recetas y los ítems de la Lista de la Compra **no se tocan** (son persistidos fuera del chat).
- **D10** (derivada): **guardia de generación** en `runRequest`: el callback `onContent` (hilo IO) y los handlers `onSuccess`/`onUnavailable` solo escriben estado si su generación sigue vigente; tras un limpiado ningún fragmento ni resultado tardío pinta nada (complementa la cancelación del `Job`, que ya cancela la `Call` de OkHttp).
- **D11** (derivada): icono `Icons.Outlined.DeleteSweep` (mismo de «Limpiar comprados» de la Lista), testTag `PincheTestTags.CLEAR_CONVERSATION = "pinche_clear_conversation"` y clave `pinche_clear_conversation` («Limpiar conversación» / "Clear conversation") — patrón de `ShoppingListScreen` (`CLEAR_COMPLETED`).

## Acceptance Scenarios

### Scenario 1: La conversación sobrevive a la navegación (verify 1)

Given una conversación en la pantalla Pinche con mensajes del usuario y del asistente (y, si procede, una Ficha)
When cambio a otra pestaña de la barra inferior —incluido «Mi perfil» desde «Abrir Mi perfil»— y vuelvo a «Pinche»; o abro un destino apilado (Ajustes) y vuelvo
Then la conversación sigue intacta (mensajes, Fichas y sus estados) y puedo continuarla desde donde estaba

### Scenario 2: Limpiar devuelve al estado inicial y se puede conversar de nuevo (verify 2)

Given una conversación con varios mensajes y el botón visible en la top bar
When pulso el botón
Then la pantalla vuelve al estado inicial (saludo + chips de sugerencia, campo vacío, sin avisos ni streaming) y al enviar un mensaje nuevo el Pinche responde con normalidad

### Scenario 3: Botón localizado y solo con conversación (verify 3)

Given la pantalla Pinche
When la conversación está vacía / tiene algo que limpiar
Then el botón no se muestra / se muestra con la content-description «Limpiar conversación» en español y «Clear conversation» en inglés (según el idioma de la app)

### Scenario 4: Limpiar con una petición en curso (step 3, D5)

Given una petición en curso con la burbuja de streaming visible
When pulso el botón
Then la petición se cancela, la pantalla vuelve al estado inicial y ningún fragmento ni respuesta tardía vuelve a pintarse ni a añadir mensajes

### Scenario 5: La limpieza no toca «Mi perfil» (step 3, D6)

Given un perfil con partes guardadas desde el chat y otras pendientes
When limpio la conversación y en la nueva pido una receta
Then «Mi perfil» conserva lo guardado y el Pinche retoma la entrevista solo por las partes que siguen pendientes

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — `PincheUiState` (messages, input, isSending, notice, lastUserText, streamingText), `runRequest` con `viewModelScope.launch` y `onContent` desde hilo IO; no hay `clearConversation` ni cancelación explícita de la petición.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheRoute.kt` — `viewModel: PincheViewModel = koinViewModel()` (por defecto anclado al `NavBackStackEntry`).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `openTab` con `popUpTo(my_recipes)` + `launchSingleTop`: al cambiar de pestaña la entrada `pinche` se destruye y con ella su ViewModel → hoy la conversación **se pierde** al cambiar de pestaña (también al pulsar «Abrir Mi perfil», que usa `openTab(AppTab.MY_PROFILE)`); con destinos apilados (Ajustes) la entrada sobrevive.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — `PincheTestTags`, `TopAppBar` solo con título (sin `actions`), saludo + chips derivados de `messages.isEmpty()`, `sendQuickSuggestion` operativo solo con conversación vacía (coherente con D7).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `viewModel { PincheViewModel(get(), get(), get(), get(), get()) }` (DSL `org.koin.core.module.dsl.viewModel`, Koin 4.2).
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — streaming SSE; la `Call` de OkHttp se cancela al cancelar el coroutine (base de D5).
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — patrón «Limpiar comprados»: `IconButton` en `TopAppBar.actions` con `Icons.Outlined.DeleteSweep`, `contentDescription` localizado y testTag propio.
- `app/src/main/res/values/strings.xml` + `values-en/strings.xml` — claves `pinche_*`; se mantiene la paridad es/en.
- `app/src/test/java/com/whatsforlunch/app/ui/pinche/PincheViewModelTest.kt` — unit del ViewModel con `runTest` y clientes falsos; `ShoppingListFlowTest` también construye un `PincheViewModel` (firma inalterada).
- `app/src/androidTest/java/com/whatsforlunch/app/PincheE2ETest.kt` — 17 tests que montan `PincheScreen` directamente con `PincheViewModel` propio y `FakePincheChatClient` (con gates `CompletableDeferred` para el streaming, S6).
- `app/src/androidTest/java/com/whatsforlunch/app/BottomBarE2ETest.kt` y `CreateRecipeE2ETest.kt` (`CreateRecipeAppE2ETest`) — patrón de app real con `createAndroidComposeRule<MainActivity>()` e inyección de fakes en Koin vía `GlobalContext.get().loadModules(..., allowOverride = true)`.
- `docs/build-brief.md:60`, `docs/domain-model.md` (RD17, RD26/RD27), `DESIGN.md` (fila Pinche), `docs/risks-and-open-questions.md` (`Q-resolver-dudas-con-pinche-5`), `CONTEXT.md` — vida de la conversación y enmiendas previas.

### Existing Patterns To Follow

- Acción de limpiar en la top bar estilo «Limpiar comprados» (icono + content-description + testTag).
- E2E de app real con MainActivity + fake del cliente de IA inyectado en Koin (`CreateRecipeAppE2ETest`, `MyProfileE2ETest` verify 4).
- Localización verificada cambiando el idioma con `AppLocaleController` (patrón `MyRecipesLocalizationE2ETest`) o con `localized(...)` vía Koin.
- Enmienda de comportamiento aceptado sin reescribir specs ni evidencias históricas (patrón `rellenar-perfil-con-el-pinche` → `crear-mi-perfil`).

### Current Gaps

- No existe hoy ningún mecanismo de ámbito superior al back stack para el estado del Pinche (ni holder ni VM anclado).
- No hay comando automatizado de paridad de claves es/en: se verifica comparando los `name` de `values/strings.xml` y `values-en/strings.xml` (práctica de las sesiones previas).

## Technical Approach

1. **Ámbito (D8)**: en `PincheRoute`, un helper composable resuelve el ViewModel con `koinViewModel(viewModelStoreOwner = <actividad>)` cuando `LocalActivity.current as? ComponentActivity` da la Activity (la app real: `LocalContext` es `MainActivity`), y con `koinViewModel()` (owner por defecto, como antes) cuando no la hay —tests que proveen un `LocalContext` de app—. El ViewModel vive en el `ViewModelStore` de la Activity: sobrevive a `openTab` y a destinos apilados; se destruye al morir el proceso (D1). El contenido de `composable(AppRoutes.PINCHE)` en `AppNavHost` no cambia.
2. **Limpieza (D9)**: `PincheViewModel.clearConversation()` cancela el `Job` guardado de la petición en curso, incrementa `conversationGeneration` y pone `_uiState.value = PincheUiState()`. `send`/`retry` guardan su `Job` y capturan la generación al arrancar `runRequest`.
3. **Descartes tardíos (D10)**: el lambda `onContent` y los `onSuccess`/`onUnavailable` comparan la generación capturada con la vigente antes de escribir `_uiState`; si no coincide, no hacen nada.
4. **UI (D3/D4/D7/D11)**: `PincheScreen` recibe `onClearConversation: () -> Unit` (parámetro con defecto `={}` para no romper llamadas existentes). La `TopAppBar` gana `actions` con un `IconButton` (`Icons.Outlined.DeleteSweep`, `contentDescription = stringResource(R.string.pinche_clear_conversation)`, `testTag(PincheTestTags.CLEAR_CONVERSATION)`) visible solo con `uiState.messages.isNotEmpty()`. Al pulsar: limpieza directa. El estado inicial muestra de nuevo saludo + chips (derivado de `messages.isEmpty()`) y las chips vuelven a ser operativas.
5. **Strings (D11)**: clave `pinche_clear_conversation` en `values/strings.xml` («Limpiar conversación») y `values-en/strings.xml` ("Clear conversation"), con paridad de claves.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — modify; `Job` de la petición + generación + `clearConversation()` (D9/D10).
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheRoute.kt` — modify; VM anclado a la Activity (D8) y cableado de `onClearConversation`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — modify; acción en la top bar, testTag y parámetro `onClearConversation`.
- `app/src/main/res/values/strings.xml` + `app/src/main/res/values-en/strings.xml` — modify; clave `pinche_clear_conversation`.
- `app/src/test/java/com/whatsforlunch/app/ui/pinche/PincheViewModelTest.kt` — modify; unit de limpiado, cancelación y descartes tardíos.
- `app/src/androidTest/java/com/whatsforlunch/app/PincheConversationE2ETest.kt` — create; E2E con app real (S1, S2, S3, S5) con `createAndroidComposeRule<MainActivity>()` y `FakePincheChatClient` en Koin.
- `app/src/androidTest/java/com/whatsforlunch/app/PincheE2ETest.kt` — modify solo si la firma de `PincheScreen` lo exige (no debería: parámetro con defecto); sin reescribir sus 17 tests.

## Visual Design Impact

- UI involved: yes
- Design source: `../../../../DESIGN.md` (fila «Pinche» y patrón «Limpiar comprados» de «Lista de la Compra»)
- Screens or states affected: top bar de la pantalla Pinche (acción nueva); estado inicial tras limpiar (saludo + chips, ya existente); sin cambios en el resto de estados (burbujas, Ficha, avisos, streaming).
- New design artifact required: no — se reutiliza el patrón de acción de top bar de la Lista de la Compra.

## Durable Documentation Impact

- `../../../../ARCHITECTURE.md` / `../../../../CONSTRAINTS.md`: not needed — no existen en este repo; los docs durables son `CONTEXT.md` y `docs/` (el alcance no introduce dominios ni reglas MUST nuevas).
- `../../../../AGENTS.md`: not needed — no cambia el flujo de trabajo, comandos ni convenciones.
- `docs/build-brief.md`: update — línea 60 «Historial de conversación entre sesiones»: precisar «mientras dura la pantalla» → «mientras la app está abierta» (D1) y añadir el botón de limpiar la conversación.
- `docs/domain-model.md`: update — RD17 (la conversación se mantiene en memoria mientras la app está abierta y se puede limpiar desde la top bar) y casos límite nuevos (limpiar con streaming, limpiar con Ficha guardada, perfil no tocado).
- `DESIGN.md`: update — fila «Pinche»: botón «Limpiar conversación» en la top bar (solo con conversación).
- `docs/risks-and-open-questions.md`: update — registrar `Q-mantener-y-limpiar-conversacion-del-pinche-1..5` con las decisiones D3-D7 (convención del repo: las Q se registran al implementar).
- `docs/technical-discovery.md`: update mínimo — nota sobre el ámbito del `PincheViewModel` (Activity-scoped) en la sección de arquitectura/UI.
- `docs/specs/resolver-dudas-con-pinche.md` y `docs/specs/rellenar-perfil-con-el-pinche.md`: not needed — sus specs y evidencias históricas no se reescriben; esta feature enmienda su comportamiento («al salir del Pinche se pierde la conversación») y queda documentada aquí y en los docs durables (patrón de `rellenar-perfil-con-el-pinche`).

## Implementation Plan

1. `PincheViewModel`: `Job` + generación + `clearConversation()` + unit tests (D9/D10).
2. `PincheRoute`: anclar el ViewModel a la Activity (D8) y cablear `onClearConversation`.
3. `PincheScreen`: acción `IconButton` en la top bar + testTag + clave es/en (D3/D4/D7/D11).
4. E2E: crear `PincheConversationE2ETest` (app real, S1-S3 y S5) y comprobar que `PincheE2ETest` sigue compilando y en verde.
5. Docs durables (build-brief, domain-model, DESIGN, risks Q-entries, technical-discovery) y paridad de claves es/en.
6. Gates completos y batería E2E.

## Implementation Tasks

- [x] Añadir `clearConversation()` con cancelación del `Job` en curso y reset total a `PincheUiState()` (D9).
- [x] Guardia de generación en `onContent`, `onSuccess` y `onUnavailable` (D10).
- [x] Unit: limpiar restablece el estado inicial (mensajes, entrada, avisos, streaming, lastUserText).
- [x] Unit: limpiar con petición en curso cancela y descarta fragmentos/resultados tardíos (S4).
- [x] Anclar `PincheViewModel` a la Activity en `PincheRoute` (D8).
- [x] `PincheScreen`: `IconButton` (`Icons.Outlined.DeleteSweep`) en `TopAppBar.actions`, visible solo con `messages.isNotEmpty()` (D7), con `pinche_clear_conversation` es/en y tag `pinche_clear_conversation` (D11).
- [x] `PincheConversationE2ETest` con app real + `FakePincheChatClient` en Koin: S1 (navegación), S2 (limpiar y conversar), S3 (es/en + visibilidad) y S5 (perfil intacto y entrevista retomada).
- [x] Actualizar docs durables y registrar `Q-mantener-y-limpiar-conversacion-del-pinche-1..5`.
- [x] Gates: `init.ps1`, `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin`, paridad es/en y batería E2E.

## Verification Plan

- `./init.ps1` → «Entorno listo» (exit 0; ejecuta comprobaciones no bloqueantes de entorno, no arranca procesos largos).
- `./gradlew :app:lintDebug` → `No issues found` (línea base actual: 0 errores / 0 warnings).
- `./gradlew :app:testDebugUnitTest` → verde (línea base 47 suites / 375 tests; los nuevos se suman).
- `./gradlew :app:assembleDebug` y `:app:compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL.
- Paridad es/en: los `name` de `values/strings.xml` y `values-en/strings.xml` coinciden (diff vacío) y `pinche_clear_conversation` existe en ambos.
- E2E focalizada (emulador, `connectedDebugAndroidTest`):
  - `PincheConversationE2ETest` — verify 1 (S1), verify 2 (S2), verify 3 (S3, content-description en es y en con `AppLocaleController`) y S5 (D6).
  - `PincheE2ETest` (regresión, 17/17) y `BottomBarE2ETest` + `MyProfileE2ETest` (regresión de navegación y perfil).
  - S4 (limpiar con streaming) queda cubierta por unit con cliente falso y gates; su cobertura E2E opcional reutilizaría los `CompletableDeferred` de `FakePincheChatClient` (S6 de `PincheE2ETest`).
- Batería completa `./e2e.ps1` sobre el árbol final = inventario `@Test` (251 hoy + los nuevos), sin regresiones.
- E2E justificada porque la feature cambia comportamiento visible y navegación (los tests con app real son el único modo de comprobar la supervivencia de la conversación al cambiar de pestaña).

## Evidence To Capture

- Salidas de `lintDebug`, `testDebugUnitTest` y `assembleDebug` en `docs/evidence/mantener-y-limpiar-conversacion-del-pinche/`.
- Resultados de la E2E focalizada y de la batería (logs/XML con recuentos por clase).
- Capturas PNG 1280×2856: top bar con el botón visible en una conversación, pantalla tras limpiar (saludo + chips) y el botón en español e inglés.
- Resumen en `evidence` de `feature_list.json` y entrada en `PROGRESS.md`/`docs/progress-history.md` al cerrar la implementación (solo `status`/`evidence` en el JSON).

## Validator Checklist

- [ ] Implementation stays within this feature's scope.
- [ ] Acceptance scenarios pass.
- [ ] Verification evidence is present.
- [ ] Persistent E2E coverage was added/updated (`PincheConversationE2ETest`) and `PincheE2ETest`/`BottomBarE2ETest`/`MyProfileE2ETest` regressions are green.
- [ ] `feature_list.json` and `../../../../PROGRESS.md` were updated correctly (solo `status`/`evidence`).
- [ ] No unrelated product behavior or extra feature work was added (sin tocar tools/prompts, «Mi perfil», Room ni persistencia de la conversación).
