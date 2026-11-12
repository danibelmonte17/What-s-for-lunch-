# Validación independiente: `mantener-y-limpiar-conversacion-del-pinche`

- **Fecha**: 2026-11-11.
- **Validador**: `feature-validator` (subagente independiente; **no participó en la implementación** de la sesión 130 y solo escribe este informe; el acceptance lo persiste el orquestador).
- **Spec (contrato)**: `docs/specs/mantener-y-limpiar-conversacion-del-pinche.md` (D1-D11, escenarios S1-S5, Expected File Changes, Durable Documentation Impact, Verification Plan, Validator Checklist).
- **Estado en `feature_list.json` al validar**: `passing` (autoverificación del implementador; no es aceptación) con 5 entradas de `evidence`.
- **Diff revisado**: `git diff f30a78e..HEAD` (3 commits: `816eba5` docs spec, `f2a3889` feat, `1d5d24d` docs/evidencia), **árbol limpio** al empezar y al terminar la validación (23 ficheros, +759/-18).
- **Veredicto**: **`accept`** -> estado recomendado para el orquestador: **`accepted`**.
- **Entorno del validador**: `ANDROID_SERIAL=emulator-5554` (`Pixel_10_Pro`, API 37, AVD) fijado en `init.ps1` y en toda prueba con dispositivo; el **dispositivo físico TLS nunca se usó** (`installDebug` y `connectedDebugAndroidTest` informaron «Installed on 1 device» y «Running tests on devices: Pixel_10_Pro(AVD) - 17»). `local.properties` solo contiene `sdk.dir`: **sin `AI_API_KEY`**, toda la E2E usa clientes simulados -> **ninguna llamada de pago a la IA**.

---

## 1. Checks re-ejecutados por el validador (tier barato)

Todos sobre el árbol actual (`git status` limpio también tras las ejecuciones):

| Comando | Resultado | Coincide con la evidencia |
|---|---|---|
| `./init.ps1` (`ANDROID_SERIAL=emulator-5554`) | **exit 0, «Entorno listo»**; `installDebug` -> `Installed on 1 device` (solo el emulador) + `monkey` + «App ... en ejecucion sobre el dispositivo de adb» | Sí (informe/`feature_list`: exit 0) |
| `.\gradlew.bat :app:lintDebug` | **BUILD SUCCESSFUL**; `app/build/reports/lint-results-debug.txt` = **«No issues found.»** -> **0 errores / 0 warnings** (línea base) | Sí |
| `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**; recuento propio desde `app/build/test-results/testDebugUnitTest/*.xml` -> **47 suites / 377 tests / 0 fallos / 0 errores / 0 omitidos**; `PincheViewModelTest` **27/27** con los 2 nuevos `clearConversationResetsTheStateToTheInitialOne` y `clearConversationCancelsTheRequestAndDiscardsLateWrites` | Sí (47/377 = base 375 + 2) |
| `.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** | Sí |
| Paridad es/en (recuento propio sobre `values/strings.xml` y `values-en/strings.xml`) | **342/342 claves `<string>`, diff = 0**, más **5/5 `<plurals>`**; `pinche_clear_conversation` presente en ambos («Limpiar conversación» / «Clear conversation») | Sí (342/342, diff 0) |
| Inventario `@Test` en `app/src/androidTest` (recuento propio) | **256 `@Test` en 28 ficheros** | Sí («256 = inventario» de la batería) |
| `git status --porcelain` (al inicio y al final) | vacío | Árbol = diff revisado |

## 2. Tier caro

### 2.1 E2E focalizada: **re-ejecutada por el validador** (la evidencia cruda se había sobrescrito)

El `informe.md` declara que el XML de la focalizada (19:06, `emulator-5558`) «fue sobrescrito por la batería posterior», es decir **el artefacto crudo de esa pasada ya no existe**: evidencia ausente para ese chequeo, lo que justifica re-ejecutarlo bajo la skill.

- Comando: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.PincheConversationE2ETest,com.whatsforlunch.app.PincheE2ETest,com.whatsforlunch.app.BottomBarE2ETest,com.whatsforlunch.app.MyProfileE2ETest"` con `ANDROID_SERIAL=emulator-5554`.
- Resultado: **BUILD SUCCESSFUL in 1m 56s**; XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17.xml` -> `<testsuites tests="34" failures="0" errors="0" skipped="0">`:
  - `PincheConversationE2ETest` **5/5** (clase nueva): `conversationSurvivesSwitchingTabsAndComingBack` (verify 1), `clearButtonResetsToTheInitialScreenAndTheChatWorksAgain` (verify 2), `clearButtonIsHiddenWhenEmptyAndShowsItsSpanishDescription` y `clearButtonShowsItsEnglishDescription` (verify 3 es/en), `clearingKeepsTheProfileAndTheNextChatCarriesIt` (D6).
  - `PincheE2ETest` **17/17**, `BottomBarE2ETest` **5/5**, `MyProfileE2ETest` **7/7** (regresión).
- **Reproducido**: la focalizada del implementador (34/34) queda confirmada con artefacto propio del validador.

### 2.2 Batería completa `./e2e.ps1`: **solo inspeccionada** (no re-ejecutada)

Evidencia exacta, consistente y sin deriva respecto al diff (specs, código, evidencia y commits del feature, todos el mismo día 2026-11-11; árbol limpio = árbol validado), así que se inspecciona en lugar de re-ejecutarse (coste de 13 a 25 min más el riesgo documentado de que otra sesión instale/desinstale APKs a mitad de ejecución, cosa que ya abortó 2 intentos):

- `docs/evidence/mantener-y-limpiar-conversacion-del-pinche/e2e-bateria-pixel10-2026-11-11-2101.log`: `Running tests on devices: Pixel_10_Pro(AVD) - 17`, `> Task :app:connectedDebugAndroidTest FAILED` con «There were failing tests», **`BUILD FAILED in 22m 49s`** (coherente con los 2 fallos declarados).
- Recuentos en `informe.md`: **256 = inventario propio del validador** -> **253 verdes + 2 flakes + 1 omitido** (aritmética consistente: 253+2+1 = 256).
- **Flakes reconciliados con artefacto crudo**: `e2e-aislada-flakes-2026-11-11.xml` -> `<testsuites tests="17" failures="0" errors="0" skipped="1">` en `emulator-5554` (timestamp `2026-11-11T20:28:45` UTC, 21:28 local, posterior al cierre de la batería a las 21:24 local), con **los dos tests nombrados en verde**: `RecipeDetailE2ETest.servingsStepperIsDisabledAtLowerBound` (7,0 s) y `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` (11,8 s); el único `skipped` es `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (`AssumptionViolatedException` sin `AI_API_KEY`), la línea base conocida.
- **Clases de la feature en la batería** (5/5, 17/17, 7/7, 5/5) corroboradas además por la re-ejecución de la focalizada del validador sobre el mismo árbol.
- Los 2 intentos previos abortados (`e2e-bateria-2026-11-11.log`, `e2e-bateria-reintento-2026-11-11-2020.log`) y su causa (`pm install` y `cmd package uninstall` de otra sesión -> `PACKAGE_FULLY_REMOVED`, `testRunFailed` sin texto de aserción) están descritos en el informe; **no se dieron por fallos de la feature**: la firma del fallo (sin aserción, paquete desinstalado en vuelo) corresponde a instrumentación muerta, no a una regresión.

## 3. Evaluación contra la spec

### 3.1 Decisiones D1-D11

| Decisión | Veredicto | Evidencia |
|---|---|---|
| D1 conversación en memoria mientras la app está abierta (sin persistencia) | Cumplida | Diff sin Room ni ficheros; E2E verify 1 en app real |
| D2 botón en la top bar que limpia y empieza de nuevo | Cumplida | `PincheScreen` `TopAppBar.actions` + `onClearConversation`; E2E verify 2 |
| D3 `IconButton` con icono y content-description localizada es/en | Cumplida | `Icons.Outlined.DeleteSweep` + `stringResource(R.string.pinche_clear_conversation)`; E2E verify 3 en ambos idiomas |
| D4 limpieza directa, sin confirmación ni «Deshacer» | Cumplida | `onClick = onClearConversation` sin diálogo; el E2E pulsa y verifica el estado inicial |
| D5 con petición en curso se cancela y lo tardío se descarta | Cumplida | `requestJob?.cancel()` más guardia de generación; unit `clearConversationCancelsTheRequestAndDiscardsLateWrites` con proveedor no cooperativo que completa pese a la cancelación. **Sin E2E**: justificado en la spec (Verification Plan: S4 por unit con gates, por determinismo) |
| D6 la limpieza no toca «Mi perfil» | Cumplida | `clearConversation` solo resetea `PincheUiState()`; E2E `clearingKeepsTheProfileAndTheNextChatCarriesIt` (el perfil sembrado viaja en el puerto tras limpiar y la chip sigue visible) |
| D7 botón solo con mensajes | Cumplida | `if (uiState.messages.isNotEmpty())`; el E2E aserta `assertCountEquals(0)` con la pantalla vacía |
| D8 `PincheViewModel` anclado a la Activity con fallback | Cumplida y documentada | `koinViewModel(viewModelStoreOwner = LocalActivity.current as? ComponentActivity)` con rama `else koinViewModel()`; hallazgo de implementación anotado en la spec (D8) y en `technical-discovery.md` |
| D9 `clearConversation()` cancela, incrementa generación y resetea a `PincheUiState()` | Cumplida | Unit `clearConversationResetsTheStateToTheInitialOne` (igualdad exacta con `PincheUiState()` tras un estado «sucio») |
| D10 guardia de generación en `onContent`, `onSuccess` y `onUnavailable` (más los fallos de guardar y de añadir a la Lista) | Cumplida | Diff de `PincheViewModel.kt`: los 5 puntos de escritura comparan `generation == conversationGeneration` |
| D11 icono `DeleteSweep`, tag `pinche_clear_conversation`, clave es/en | Cumplida | `PincheTestTags.CLEAR_CONVERSATION = "pinche_clear_conversation"`; claves en ambos locales; paridad 342/342 |

### 3.2 Escenarios de aceptación y `verification` de `feature_list.json`

- **verify 1** (conversación intacta tras cambiar de pestaña): E2E `conversationSurvivesSwitchingTabsAndComingBack`, **verde en la re-ejecución del validador**.
- **verify 2** (limpiar -> estado inicial y volver a conversar): E2E `clearButtonResetsToTheInitialScreenAndTheChatWorksAgain` (saludo + chips, 0 mensajes, 0 botón, chip operativa), **verde**.
- **verify 3** (content-description es/en): E2E `clearButtonIsHiddenWhenEmptyAndShowsItsSpanishDescription` y `clearButtonShowsItsEnglishDescription`, **verdes**.
- **verification 4 (unit)** (limpiar restablece el estado del ViewModel): **verde** en re-ejecución propia (`PincheViewModelTest` 27/27).

### 3.3 Non-goals respetados

Sin persistencia de la conversación (sin Room, ficheros ni migración); sin dictado; **contrato con el proveedor intacto** (el diff no toca `PromptSource.kt`, `OpenAiCompatiblePincheChatClient.kt`, prompts ni `MAX_HTTP_CALLS`); **sin tocar «Mi perfil»** (`UserProfileRules` y RD25-RD27 fuera del diff); una sola conversación viva (el botón limpia y no crea historiales).

### 3.4 Alcance (`Expected File Changes`)

Coincide exactamente: `PincheViewModel.kt`, `PincheRoute.kt`, `PincheScreen.kt`, `strings.xml` ×2, `PincheViewModelTest.kt`, `PincheConversationE2ETest.kt` (creado), `PincheE2ETest.kt` **sin tocar** (el parámetro lleva defecto, como preveía la spec), más los 5 docs durables declarados en *Durable Documentation Impact*, la spec, la evidencia, `feature_list.json`, `PROGRESS.md` y `docs/progress-history.md`. **Sin scope creep**: nada de `uxdocs/`, `.opencode/`, scripts del harness, dominio, Room, manifiesto, permisos ni dependencias.

## 4. Hallazgos por severidad

**0 Critical / 0 High / 0 Medium.** 3 Low no bloqueantes:

### Finding L1: faltan las 3 capturas PNG que pide *Evidence To Capture*

- Severity: Low
- Evidence: la spec (§ Evidence To Capture) pide «Capturas PNG 1280×2856: top bar con el botón visible en una conversación, pantalla tras limpiar (saludo + chips) y el botón en español e inglés»; el barrido de `docs/evidence/mantener-y-limpiar-conversacion-del-pinche` con filtro `*.png` devuelve **0 ficheros** (la carpeta solo tiene 3 logs, 1 XML, `informe.md`, `lint-2026-11-11.txt` y `unit-PincheViewModelTest-2026-11-11.xml`).
- Why it matters: es el único hueco entre lo declarado en la spec y la evidencia entregada; la parte visual del cambio (icono en la top bar) queda cubierta solo por aserciones automáticas y no por un artefacto revisable sin ejecutar nada.
- Required change (no bloqueante; cabe en un ciclo de pulido): capturar los 3 PNG (conversación con el botón visible, estado inicial tras limpiar, botón en es y en) en `docs/evidence/mantener-y-limpiar-conversacion-del-pinche/`.
- Verification after fix: 3 PNG con cabecera `89 50 4E 47` e `IHDR` 1280×2856 que muestren el `IconButton` de la top bar, el saludo + chips y la content-description en ambos idiomas.

### Finding L2: los *Implementation Tasks* de la spec quedaron sin marcar

- Severity: Low
- Evidence: en `docs/specs/mantener-y-limpiar-conversacion-del-pinche.md` las 9 tareas de implementación siguen `- [ ]`, mientras que la convención de las specs aceptadas es marcarlas al implementar (`rellenar-perfil-con-el-pinche.md` T1-T11 en `[x]`, `unificar-unidades.md` 11 en `[x]`, `redisenar-formulario-de-receta.md` T1-T7 en `[x]`; el *Validator Checklist* se deja sin marcar, como corresponde).
- Why it matters: un agente futuro que lea la spec no distingue de un vistazo qué tareas quedaron hechas y qué queda pendiente (handoff).
- Required change (no bloqueante): marcar las 9 tareas como `[x]` al persistir el acceptance.
- Verification after fix: contar 9 `- [x]` de tareas en la spec.

### Finding L3: los XML crudos de la focalizada y de la batería se sobrescribieron; los recuentos por clase de la batería solo viven en `informe.md`

- Severity: Low
- Evidence: el propio informe lo declara (patrón N1 de la sesión 115); el log `e2e-bateria-pixel10-2026-11-11-2101.log` confirma dispositivo, `BUILD FAILED in 22m 49s` y «There were failing tests», pero **no** incluye recuentos por clase; el XML aislado (17/0/1) es el único artefacto crudo conservado, y la spec pedía «logs/XML con recuentos por clase».
- Why it matters: el desglose 253+2+1 de la batería se apoya en la redacción del informe y no en un artefacto parseable; se mitiga con el recuento propio de inventario del validador (256) y con la re-ejecución de la focalizada.
- Required change (no bloqueante): si se vuelve a pasar la batería, conservar su XML sin sobrescribirlo con el rerun aislado (o guardar un log con los recuentos por clase).

**Notas sin cambio requerido**: N1 la cabecera de la spec declara `status: not_started` (convención: estado al planificar, igual que las specs vecinas); N2 la entrada de `docs/progress-history.md` cita «Spec (145 líneas)» mientras el fichero final tiene 193 tras ampliar D8 con el hallazgo; N3 el `tearDown` de `PincheConversationE2ETest` re-registra el binding real del cliente duplicando el cableado del módulo DI (se rompe en compilación si cambia el constructor del cliente; patrón aceptado en este repo).

## 5. Seguridad (`security-checklist.md` acotado al diff)

**Sin hallazgos relevantes y sin bloqueantes.**

- **Secretos y configuración**: el diff no contiene API keys, tokens ni URLs sensibles; no toca `local.properties` (solo `sdk.dir`, sin `AI_API_KEY`); el único valor de configuración que aparece es `BuildConfig.AI_BASE_URL` en el `tearDown` del test (URL base, no secreto). Ningún log ni fichero de evidencia lleva PII ni texto de conversación.
- **Autenticación y autorización**: no aplica (sin cuentas ni sesión; funcionalidad local).
- **Validación de entrada e inyección**: sin nuevos puntos de entrada; el texto de entrada conserva el tope `MAX_USER_MESSAGE_CHARS` existente.
- **Exposición de datos y privacidad**: la conversación sigue **solo en memoria** (sin persistir y sin nuevos canales de salida); limpiar no toca datos persistidos; RD17 y el aviso de privacidad siguen vigentes.
- **Servicios externos**: sin llamadas nuevas; toda la E2E usa un cliente simulado inyectado en Koin y `AI_API_KEY` ausente -> **0 llamadas de pago** (AGENTS.md exige confirmación humana para llamadas de pago: no se pidió ni se ejecutó ninguna).
- **Dependencias y tooling**: 0 cambios de dependencias, 0 cambios de manifiesto, 0 permisos nuevos.
- **Límite cliente/servidor, subidas de fichero y abuso**: no aplican a esta slice.

## 6. Docs durables, harness y handoff

- **Docs durables al día y sin contradicciones**: `docs/build-brief.md` (línea 60 precisada a «mientras la app está abierta» más el botón), `docs/domain-model.md` (RD17 enmendada y 3 casos límite nuevos coherentes con el código), `DESIGN.md` (fila «Pinche» con el botón de la top bar), `docs/risks-and-open-questions.md` (`Q-mantener-y-limpiar-conversacion-del-pinche-1..5` resueltas con D3-D7 del usuario) y `docs/technical-discovery.md` (ámbito Activity-scoped, fallback y generación). `AGENTS.md` intacto; `ARCHITECTURE.md` y `CONSTRAINTS.md` no existen en este repo (la spec lo declara); specs vecinas sin reescribir (patrón de enmienda correcto).
- **UI sin reglas de negocio**: la lógica de limpieza, cancelación y generación vive en `PincheViewModel`; `PincheScreen` solo delega (`onClearConversation`) y decide la visibilidad (D7), coherente con `CONTEXT.md` y `docs/domain-model.md`.
- **Harness**: `init.ps1` termina «Entorno listo» con exit 0, pertenece al tier barato y **no** arranca procesos largos ni ejecuta pruebas de dispositivo; los scripts `lint`, `unit` y `e2e` no se tocaron.
- **Handoff**: `PROGRESS.md` refleja `passing` con el detalle del estado y, como *Siguiente paso*, la validación independiente (esta); `feature_list.json` solo cambió `evidence` (5 entradas) y `status` (`not_started` -> `passing`), verificado en el diff, sin tocar otros campos ni otras features.

## 7. Validator Checklist de la spec (punto por punto)

- [x] **Scope**: dentro de la feature, sin trabajo ajeno (§3.4).
- [x] **Escenarios de aceptación**: S1-S5 / verify 1-4 en verde (E2E re-ejecutada y unit re-ejecutado; S4 por unit con justificación explícita en la spec).
- [x] **Evidencia de verificación presente**: gates, focalizada, batería y aislado en `docs/evidence/mantener-y-limpiar-conversacion-del-pinche/`, con las salvedades L1 y L3.
- [x] **Cobertura E2E persistente añadida**: `PincheConversationE2ETest` (5 tests) más las regresiones `PincheE2ETest`, `BottomBarE2ETest` y `MyProfileE2ETest` en verde.
- [x] **`feature_list.json` y `PROGRESS.md` actualizados correctamente** (solo `status` y `evidence` en el JSON).
- [x] **Sin comportamiento aislado ni feature extra** (tools/prompts, «Mi perfil», Room y persistencia intactos).

## 8. Veredicto

**`accept`** -> estado recomendado para el orquestador: **`accepted`** en `feature_list.json` (solo `status`/`evidence`).

- **Checks re-ejecutados por el validador**: `init.ps1` exit 0 («Entorno listo»), `lintDebug` 0/0 («No issues found.»), `testDebugUnitTest --rerun-tasks` 47 suites / 377 tests / 0 fallos / 0 omitidos, `assembleDebug` + `compileDebugAndroidTestKotlin` BUILD SUCCESSFUL, paridad es/en 342/342 más 5/5 plurales, inventario 256 `@Test`, y **E2E focalizada 34/34 en `emulator-5554`** con artefacto propio (la del implementador se había sobrescrito).
- **Solo inspeccionado**: batería completa `e2e.ps1` (256 = 253 verdes + 2 flakes de timing ajenos + 1 omitido `Assume`) con sus artefactos (log de 22m49s en Pixel_10_Pro, recuentos del `informe.md` y XML aislado 17 tests / 0 fallos / 1 omitido con los 2 flakes en verde), por ser evidencia exacta, consistente y sin deriva respecto al diff. Si el orquestador quiere máxima holgura, puede re-ejecutarla en una ventana tranquila (otra sesión ha abortado 2 intentos instalando APKs), pero no es requisito para este veredicto.
- **Hallazgos**: 0 Critical, 0 High, 0 Medium; **3 Low** (L1 capturas PNG ausentes, L2 tareas de la spec sin marcar, L3 XML de batería sobrescrito). Ninguno exige *Implementation Repair Brief*; pueden resolverse como pulido o al persistir el acceptance.
- **Seguridad**: sin hallazgos (checklist acotada al diff: sin secretos, sin permisos, sin dependencias y sin llamadas de pago).
- **Docs durables y handoff**: coherentes con el comportamiento implementado.
