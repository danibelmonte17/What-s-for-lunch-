# Informe de implementación — Resolver dudas con el Pinche

- **Feature**: `resolver-dudas-con-pinche`
- **Spec**: `docs/specs/resolver-dudas-con-pinche.md`
- **Rama / worktree**: `feature/resolver-dudas-con-pinche`
- **Fecha**: 2026-10-26
- **Estado**: `passing` (implementada y autoverificada)
- **Aviso**: este documento **no es una aceptación**. Lo emite el implementador y la validación independiente corresponde a `feature-validator`.

## 1. Prerrequisito bloqueante `T0` (`S08`, `RK07`)

Única llamada real autorizada y de pago de toda la feature, ejecutada en la sesión previa con la key del build:

- `POST https://inference.devexpert.io/v1/chat/completions` con el alias `chat`, `tools` (`get_pantry`, `get_my_recipes`) y `tool_choice: "auto"`.
- Resultado: **HTTP 200 en 622 ms**, `finish_reason = tool_calls`, una llamada `get_pantry` con `arguments = {}`.
- Documentado en `docs/risks-and-open-questions.md` (`S08` confirmado, `RK07` con el presupuesto de 3 llamadas) y en `docs/technical-discovery.md` (§ «Pinche con function calling»).

**No se repitió ninguna llamada real** durante la implementación ni en la verificación automática: todos los tests usan `FakePincheChatClient` y fakes en memoria.

## 2. Alcance implementado

- **Entrada (RF-PCH1)**: `Icons.AutoMirrored.Outlined.Chat` como **primera** acción de la top bar de Mis Recetas (`MyRecipesTestTags.OPEN_PINCHE = "action_open_pinche"`, `contentDescription` «Abrir el Pinche») y `AppRoutes.PINCHE = "pinche"` con `PincheRoute(onBack, onOpenSettings)`; `MyRecipesRoute`/`MyRecipesScreen` ganan `onOpenPinche: () -> Unit = {}` y `AppNavHost` cablea la navegación.
- **Pantalla (RF-PCH2, RF-PCH10)**: saludo estático (no viaja al modelo), lista de burbujas, tres chips de consulta rápida solo con la conversación vacía, compositor de una línea con `imeAction = Send`, fila «El Pinche está pensando…» con `LinearProgressIndicator` mientras `isSending` y compositor deshabilitado; botón «Enviar» deshabilitado con texto en blanco.
- **Tanda al proveedor (RF-PCH3, D5, D11)**: `messages` = prompt de sistema (es/en) + últimos **20** turnos; `tools` con las dos funciones de solo lectura, `tool_choice: "auto"`, `temperature` 0, `model = "chat"` y **sin `response_format`**.
- **Bucle de tools (RF-PCH4, D6)**: las `tool_calls` se ejecutan en el dispositivo y se devuelven como mensajes `role = tool` por `tool_call_id`; **máximo 3 llamadas HTTP por turno** (la tercera respuesta con `tool_calls` deja el turno `Failed`); sin reintentos automáticos. Tool desconocida → `{"error":"unknown_function"}`; excepción de la tool → `{"error":"unavailable"}`; argumentos no JSON se ignoran.
- **Contenido de las tools (RF-PCH5, Q-14, D11)**: `get_pantry` → `{"items":[{"name","quantity"}]}` con tope de 100 Alimentos; `get_my_recipes` → `{"recipes":[{"title","moment","ingredients":[...]}]}` con tope de 50 Recetas y 20 nombres de ingredientes por Receta (sin pasos, foto, estado ni cantidades).
- **Respuesta (RF-PCH6, D5, D7)**: `PincheChatProtocol.parseReply` separa el texto conversacional del bloque cercado `receta` (`{"s":[{…}]}`), lo normaliza con `RecipeSuggestionParser` + `SuggestionRules.normalize(...).firstOrNull()` y recorta a **una** Propuesta; sin bloque usa el primer objeto JSON del contenido y, si el JSON es inválido, deja solo el texto.
- **Ficha (RF-PCH7, RD15, D8)**: `ProposalCard` con cabecera «Propuesta del Pinche», título, Momento · tiempo total · Raciones, ingredientes (los opcionales marcados «Opcional»), pasos numerados con chip de temporizador y «Te faltan» solo si hay Faltantes. `PincheProposalRules.missingRequired` devuelve los obligatorios ausentes por `normalizeName`, sin duplicados y en orden de la receta, calculados **una sola vez** al recibir la respuesta (snapshot). Sin acción de Lista de la Compra ni de Modo Cocina.
- **Guardado (RF-PCH8, RD16, D9)**: `PincheProposalRules.proposalToRecipe` construye un **Borrador sin foto**, `favorite = false`, `origen = SUGGESTION`, id `UUID.nameUUIDFromBytes("pinche:" + normalizeName(título))` e ids de hijos `"$id-ingredient-$i"` / `"$id-step-$i"`; `RecipeRepository.saveRecipe` hace upsert, la Ficha pasa a «Guardada en Mis Recetas» deshabilitada y se permanece en el chat; un segundo guardado no duplica.
- **Fallos (RF-PCH9, D10)**: `MissingApiKey` → aviso + «Abrir Ajustes»; `Failed` → aviso + «Reintentar» (reenvía `lastUserText` sin añadir burbuja); `SAVE_FAILED` → aviso y botón de guardar reactivado; todos descartables con la ✕ y fuera de la lista.
- **Idioma (RF-PCH11, D12)**: 25 claves `pinche_*` nuevas con paridad es/en; prompt de sistema es/en y `mm`/`df` como tokens de máquina.

### Fuera de alcance respetado

Sin voz ni micrófono (`⚠ S04`), sin Lista de la Compra ni Faltantes→ÍtemLista, sin Modo Cocina, sin persistencia (ni Room ni migraciones ni historial), sin `/images/generations`, sin markdown ni streaming ni más de una Propuesta por respuesta, sin funciones de escritura, y sin tocar `calculateMatch`, `MatchBadge`, RD01/RD02/RD10/RD13, `SuggestionRules` ni `SuggestionAcceptance`. Sin permisos Android nuevos.

## 3. Ficheros

### Creados

- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheConversation.kt` — `PincheAuthor`, `PincheTurn`, `PincheProposalState`, `PincheProposal`, `PincheReply`.
- `app/src/main/java/com/whatsforlunch/app/domain/pinche/PincheProposalRules.kt` — `missingRequired`, `proposalRecipeId`, `proposalToRecipe`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — puerto + `PincheReplyResult` (`Success | MissingApiKey | Failed`).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — tools JSON, `toolCalls`, resultados acotados, `parseReply`, prompt de sistema es/en.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — bucle de tool calling, presupuesto de 3 llamadas, logging `PincheChat` (código HTTP, duración, tools y motivo; nunca key, conversación, Despensa ni Recetas).
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt`, `PincheScreen.kt`, `PincheRoute.kt`.
- `app/src/test/java/com/whatsforlunch/app/data/ai/PincheChatProtocolTest.kt` (10 tests).
- `app/src/test/java/com/whatsforlunch/app/domain/pinche/PincheProposalRulesTest.kt` (6 tests).
- `app/src/test/java/com/whatsforlunch/app/ui/pinche/PincheViewModelTest.kt` (11 tests).
- `app/src/androidTest/java/com/whatsforlunch/app/PincheE2ETest.kt` (11 tests: S1-S10 y los tres `verify`).
- `docs/evidence/resolver-dudas-con-pinche/pinche-ficha-es.png` y `pinche-guardada-es.png`.

### Modificados

- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `PincheChatClient` y `PincheViewModel`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt`, `MyRecipesRoute.kt` — `onOpenPinche`, acción de top bar y `OPEN_PINCHE`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes.PINCHE`, `composable` y cableado desde Mis Recetas.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `pinche_*` es/en.
- `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` — RD15/RD17 y notas de implementación; `Q-resolver-dudas-con-pinche-16` cerrada; `S08`/`RK07` confirmados; `RK03` con los datos que salen del dispositivo.
- `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` (`status` y `evidence`).

### Correcciones de la recuperación

- `PincheE2ETest.kt` tenía **mojibake** en 6 líneas (el bullet `•` y el apóstrofo `’` se habían corrompido a U+FFFD): corregido con la herramienta de edición UTF-8. Barrido posterior de `\uFFFD` en `*.kt`/`*.xml`/`*.md`/`*.json`: **0 coincidencias**.
- `AppNavHost.kt` no cableaba `onOpenPinche` en `MyRecipesRoute`, así que en la app real el icono no navegaba (RF-PCH1): añadido.

## 4. Verificación ejecutada

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` (con `ANDROID_SERIAL=emulator-5554`, `PKG=com.whatsforlunch.app`) | **exit 0** · «Entorno listo» (AVD `Pixel_10_Pro` API 37, `emulator-5554`) |
| Lint | `gradlew.bat :app:lintDebug` | **0 errores / 49 warnings** (ninguno nuevo de la feature) |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **173/173 en 27 suites**, 0 fallos, 0 errores, 0 omitidas (27 nuevas: 10 + 6 + 11) |
| Build | `gradlew.bat :app:assembleDebug` | **BUILD SUCCESSFUL** |
| E2E | `./e2e.ps1` → `gradlew.bat connectedDebugAndroidTest` (AVD `Pixel_10_Pro` API 37, `emulator-5554`) | **BUILD SUCCESSFUL en 6 m 57 s**: **148 ejecutados, 0 fallos, 0 errores, 1 omitido** (`SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete`, por `Assume` con key de build). `PincheE2ETest` **11/11** |

### Escenarios S1-S10

| Escenario | Cobertura | Resultado |
|---|---|---|
| S1 · Duda respondida sin Propuesta | `cookingQuestionIsAnsweredWithTextOnly` + unit `sendAddsTheUserBubbleAndTheAssistantAnswer` | verde |
| S2 · El modelo consulta la Despensa (verify 1) | `answerWithProposalShowsTheCardWithIngredientsStepsAndMissingItems` | verde |
| S3 · Ficha con Faltantes y guardado (verify 2) | `savingTheProposalCreatesADraftInMyRecipesWithoutDuplicates` + unit `savingTheProposalStoresADraftAndMarksTheCardAsSaved` | verde |
| S4 · Propone sin Despensa (RD14) | `emptyPantryShowsEveryRequiredIngredientAsMissing` | verde |
| S5 · Propuesta inválida | `answerWithoutAValidProposalShowsOnlyText` + unit `invalidProposalLeavesOnlyTheText` | verde |
| S6/S7 · Sin key y fallo con «Reintentar» (verify 3) | `missingApiKeyShowsNoticeAndOpensSettings`, `failureShowsRetryAndRetryingDoesNotDuplicateTheUserMessage` | verde |
| S8 · Entrada desde Mis Recetas y vuelta | `pincheOpensFromMyRecipesAndKeepsSearchAndFilterOnBack` | verde |
| S9 · Guardado fallido | `failedSaveShowsNoticeAndEnablesTheButtonAgain` + unit `failedSaveRevertsTheCardAndShowsTheSaveNotice` | verde |
| S10 · Textos en inglés | `pincheIsLocalizedInEnglish` | verde |
| Chips | `chipSendsItsQuestionAndHidesOnceTheConversationStarts` | verde |

La regresión de la suite completa (`AiSuggestionsE2ETest` 14/14, `PantryRecipesE2ETest` 8/8, `ScanPantryE2ETest` 22/22, `RecipeDetailE2ETest` 12/12, `MyRecipes*` 17/17, `CreateRecipe*`/`EditRecipe*`/`DeleteRecipe*` y Ajustes/Despensa) queda en verde sin cambios en sus ficheros.

### Cobertura unitaria de las reglas

- `PincheChatProtocolTest`: solo las dos tools de lectura sin parámetros; `tool_calls` con argumentos no JSON; tope de 100 Alimentos y de 50 Recetas / 20 ingredientes sin pasos ni foto; `quantity` nula conservada; bloque `receta` (se usa el primero), fallback al primer JSON, JSON inválido solo texto, texto plano, prompt es/en.
- `PincheProposalRulesTest`: Faltantes solo obligatorios en orden y sin duplicados; normalización de acentos/mayúsculas/espacios; Despensa vacía; id estable y distinto del prefijo `suggestion:`; Borrador/`SUGGESTION`/sin foto/no favorito con ids de hijos; dos guardados → mismo id.
- `PincheViewModelTest`: envío con burbuja del usuario y respuesta; texto en blanco no llama; recorte a 500; `MissingApiKey`; `Failed` + `retry` sin duplicar; Faltantes con snapshot que no se recalculan; guardado idempotente; guardado fallido revierte y avisa; chips solo con conversación vacía; `Success` vacío tratado como fallo.

## 5. Evidencia

- `docs/evidence/resolver-dudas-con-pinche/pinche-ficha-es.png` (134.651 B, cabecera PNG `89 50 4E 47 0D 0A 1A 0A`): chat en español con la burbuja del usuario, «Prueba esta receta.», la Ficha «Propuesta del Pinche» con ingredientes, pasos con temporizador, «Te faltan · Harina» y «Guardar en Mis Recetas».
- `docs/evidence/resolver-dudas-con-pinche/pinche-guardada-es.png` (137.807 B, misma cabecera PNG): Mis Recetas con «Arroz con tomate» como **Borrador** (30 min · Fácil · 4 porciones) y la acción del Pinche en la top bar.
- Ambas revisadas visualmente y con cabecera binaria válida. Presupuesto respetado: **1 informe + 2 PNG**.

## 6. Pendientes y notas para el validador

- La captura `pinche-guardada-es.png` muestra la Receta ya guardada en Mis Recetas; el flujo de la Ficha «Guardada en Mis Recetas» deshabilitada está cubierto por `PincheE2ETest` y por el unit del ViewModel.
- El AVD se comparte entre worktrees: si otra sesión instala encima, el pase de `connectedDebugAndroidTest` puede abortar (flake conocido del entorno; se resuelve reiniciando el AVD).
- `T0` es la única llamada real de pago de la feature y **no debe repetirse**; el validador no necesita key.

## 7. Validación independiente (`feature-validator`, 2026-10-26)

- **Veredicto**: `accept`. La implementación cumple la spec (`docs/specs/resolver-dudas-con-pinche.md`) en RF-PCH1-RF-PCH11, S1-S10, D1-D12 y respeta los Non-Goals. Recomendación de estado: `accepted` (la persistencia en `feature_list.json` corresponde al orquestador).
- **Alcance revisado**: diff completo (`git status`, `git diff` y ficheros nuevos), dominio (`domain/pinche/`), protocolo y cliente (`data/ai/Pinche*`, `OpenAiCompatiblePincheChatClient`), ViewModel, pantalla/ruta, navegación, Koin, textos es/en, unit, `PincheE2ETest`, docs e imágenes.
- **Checks reejecutados** (AVD `Pixel_10_Pro` API 37, `emulator-5554`, arrancado y apagado por el validador; `ANDROID_SERIAL` fijo):
  - `lintDebug` + `testDebugUnitTest --rerun-tasks` + `assembleDebug` → **BUILD SUCCESSFUL en 2 m**; **lint 0 errores / 49 warnings**; **unit 173/173 en 27 suites** (`PincheChatProtocolTest` 10/10, `PincheProposalRulesTest` 6/6, `PincheViewModelTest` 11/11), 0 fallos/errores/omitidos.
  - `connectedDebugAndroidTest` filtrado a `PincheE2ETest` → **11/11, 0 fallos/errores**, 35 s.
  - Suite E2E completa → **148 ejecutados, 0 fallos, 0 errores, 1 omitido**, 4 m 29 s (el omitido es `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete`, por `Assume` con key de build); `PincheE2ETest` 11/11 y regresión de Sugerencias/Recetas con mi Despensa/Escáner/Detalle/Mis Recetas en verde.
  - Paridad es/en: **238/238** claves, sin claves huérfanas ni duplicadas.
  - `init.ps1` (no bloqueante): **exit 1** por el falso fallo conocido de `adb shell monkey` (su stderr se convierte en error con `$ErrorActionPreference = Stop`); se comprobó que la app quedó instalada y en ejecución (`pm path` + `pidof`) y que el arranque llega a lanzar la actividad. No es un defecto de la feature.
  - Comprobación manual independiente con `adb`/`uiautomator` (sin enviar mensajes ni hacer llamadas reales): desde Mis Recetas, la acción «Abrir el Pinche» abre la pantalla con saludo, los tres chips y el compositor; atrás vuelve a Mis Recetas. Confirma en la app real el cableado de RF-PCH1 (el E2E de S8 usa navegación simulada).
- **Evidencia inspeccionada**: 2 PNG en `docs/evidence/resolver-dudas-con-pinche/`, cabecera `89 50 4E 47 0D 0A 1A 0A`, 1280×2856, abiertos y coherentes con la spec (`pinche-ficha-es.png`: Ficha con ingredientes, pasos, «Te faltan · Harina» y guardar; `pinche-guardada-es.png`: Borrador «Arroz con tomate» en Mis Recetas y la acción del Pinche en la top bar). Presupuesto respetado: 1 informe + 2 capturas. XML de E2E inspeccionado antes de la reejecución: 148/0/0/1, `PincheE2ETest` 11/11, `2026-10-26T12:22:05Z`, posterior a todos los fuentes.
- **T0**: documentado sin simulación en `docs/risks-and-open-questions.md` (`S08`, `RK07`, tarea de investigación), `docs/technical-discovery.md` y este informe (HTTP 200 en 622 ms, `finish_reason = tool_calls`, `get_pantry` con `arguments = {}`). **No se repitió ninguna llamada real**; toda la verificación usa `FakePincheChatClient` y fakes en memoria.
- **Seguridad**: sin hallazgos relevantes. Sin secretos reales en el diff, docs, evidencia ni logs; barrido de patrones de key solo encuentra el valor sintético histórico `sk-test-build-config-2026` en `docs/progress-history.md`; `local.properties` sigue gitignored y solo contiene `sdk.dir`. El cliente registra código HTTP, duración, número de llamadas/tools y clase de error, nunca key, mensajes, Despensa, Recetas ni cuerpos. `RK03` actualizado con los datos que salen del dispositivo; riesgo de retención sigue como puerta de release.
- **Documentación**: `docs/domain-model.md` (RD15 con snapshot y nota de implementación RD17), `docs/technical-discovery.md` (`S08` verificado, topes, sobre `receta`, cierre de `Q-16`), `docs/risks-and-open-questions.md` (`S08`/`RK07` confirmados, `Q-resolver-dudas-con-pinche-16` cerrada, `RK03` ampliado), `PROGRESS.md` y `docs/progress-history.md` al día. `feature_list.json` solo cambia `status` y `evidence`; no hay trabajo de otras features (sin voz, Lista de la Compra, Modo Cocina, persistencia/Room, `/images/generations`, tools de escritura ni cambios en `calculateMatch`, `MatchBadge`, RD01/RD02/RD10/RD13, `SuggestionRules` o `SuggestionAcceptance`).

### Hallazgos (todos Low, no bloqueantes)

- **L1 — Cobertura del bucle de tool calling**: RF-PCH4/D6 (ejecutar `tool_calls`, devolver `role = tool` y cortar a 3 llamadas HTTP) no tiene test automático. El `FakePincheChatClient` del E2E está en el puerto, así que no emite `tool_calls`; el bucle del cliente real solo se ha revisado por inspección y con `T0`. Además `docs/technical-discovery.md` dice que el fake «simula `tool_calls`», lo que no es exacto. No bloquea: el contrato del proveedor está verificado y el comportamiento fallido es visible con «Reintentar». Recomendación: en un ciclo de pulido, extraer el bucle a una función pura testeable (o añadir un fake de `OkHttpClient`/`MockWebServer`) y cubrir el presupuesto de 3 llamadas y los mensajes `role = tool`.
- **L2 — `init.ps1` no reproducible como exit 0 en esta consola**: el implementador registra exit 0; en la reejecución sale exit 1 por el falso fallo de `adb shell monkey` antes de «Verificando», aunque la app se instala y arranca. Se evalúa como gate no bloqueante y no afecta a la feature; conviene que el harness capture el stderr de `monkey`/`pidof` para no reportar exit 1.
- **L3 — Detalles documentales**: la cabecera de la spec conserva `status: not_started` y su «Validator Checklist» sigue sin marcar (el estado real vive en `feature_list.json`); `PROGRESS.md` dice «27 nuevas [suites]» cuando son 27 tests nuevos en 27 suites. Cosmético; puede cerrarse al persistir la aceptación.
