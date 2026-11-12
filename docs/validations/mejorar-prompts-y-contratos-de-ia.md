# Validacion independiente: `mejorar-prompts-y-contratos-de-ia`

- **Fecha**: 2026-11-12.
- **Validador**: `feature-validator` (subagente independiente, sesion propia; no participo en la implementacion de la sesion 133).
- **Spec (contrato)**: `docs/specs/mejorar-prompts-y-contratos-de-ia.md` (RF-MPC1-10, escenarios S1-S7, tareas T1-T10 marcadas, Expected File Changes, Durable Documentation Impact, Verification Plan, Validator Checklist).
- **Estado en `feature_list.json` al validar**: `passing` con 4 `evidence` (autoverificacion del implementador; no es aceptacion).
- **Commits validados**: ninguno - rama `feature/mejorar-prompts-y-contratos-de-ia`, **todo sin commitear** (30 entradas en `git status`: 12 assets + 6 fuentes `data/ai` + 5 tests + `AiContractPayloadTest.kt` nueva + spec + evidencia + 4 docs + `feature_list.json` + `PROGRESS.md`).
- **Veredicto final**: **`revise`** -> estado recomendado para el orquestador: **mantener `passing`** (no persistir `accepted`) hasta cerrar los 2 hallazgos Medium de la seccion 4; son arreglos de ~10 minutos y no tocan comportamiento de usuario. Este validador no edita `feature_list.json`, `PROGRESS.md` ni `docs/progress-history.md`; su unico fichero de salida es este informe.
- **Entorno del validador**: dispositivo fisico **V2124 (Android 13)** via adb, autorizado por el usuario para instalar/E2E. `adb devices`: 1 dispositivo (`adb-34303312630018I-NShbkJ._adb-tls-connect._tcp`). Incidentes de entorno documentados en la seccion 1.3.

---

## 1. Que se re-ejecuto (tier barato) y que se inspecciono (tier caro)

### 1.1 Re-ejecutado por el validador (tier barato, sobre el arbol validado)

| Comando | Resultado | Coincide con la evidencia |
|---|---|---|
| `.\init.ps1` | **exit 0, «Entorno listo»** - `installDebug` en `V2124 - 13`, `Events injected: 1`, «App com.whatsforlunch.app en ejecucion sobre el dispositivo de adb». **Sin** el falso negativo conocido de la linea 36 | Si (el implementador registro exit 0) |
| `.\gradlew.bat :app:lintDebug --rerun-tasks` | **BUILD SUCCESSFUL, 0 errores / 7 warnings** (recuento propio desde `app/build/reports/lint-results-debug.xml`: 7 `Warning`, 0 `Error`) | Si (0/7, identicos) |
| `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL: 52 suites / 405 tests / 0 failures / 0 errors / 0 skipped** (recuento propio desde `app/build/test-results/testDebugUnitTest/*.xml`) | Si (52/405/0) |
| `.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** (50 tareas) | Si |

Desglose propio de las suites afectadas por la feature (todas en verde): `AiContractPayloadTest` **7**, `PincheChatProtocolTest` **19**, `PinchePromptsTest` **5**, `PromptAssetsTest` **10**, `SuggestionPromptTest` **5**.

Los 7 warnings de lint apuntan a ficheros **fuera del diff**: `GalleryScanImage.kt` (`ExifInterface` x2), `ScannerScreen.kt` (`ModifierParameter`), `mipmap-anydpi-v26` (`ObsoleteSdkInt`), `ic_launcher_foreground.xml` / `ic_launcher_round.webp` (`UnusedResources` x3) y `colors.xml`. **0 warnings** sobre `data/ai/*`, `assets/prompts/*` o los tests del diff.

### 1.2 Tier caro - re-ejecutado por el validador (E2E focalizada en V2124)

La E2E del implementador era **previa a los ultimos cambios de codigo** (XML de 13:17:03 frente a `RecipeContract.kt` 13:30:59, `OpenAiCompatibleRecipeSuggester.kt` y `OpenAiCompatibleScanImageAnalyzer.kt` 13:31:29, `AiContractPayloadTest.kt` 13:32:25), asi que se re-ejecuto (regla del skill: *the diff changed after the evidence was captured*). Antes de re-ejecutar se parseo y respaldo el XML del implementador (67/0/0/0, ver 1.3).

| Corrida | Comando | Resultado |
|---|---|---|
| 1.ª (14:01-14:22) | `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.AiSuggestionsE2ETest,com.whatsforlunch.app.ScanPantryE2ETest,com.whatsforlunch.app.LanguageSwitchE2ETest,com.whatsforlunch.app.PincheE2ETest"` | **BUILD FAILED: 67/67 fallos, ambientales** - ver 1.3. **No se usa como evidencia** |
| 2.ª (15:47-15:49, tras desbloquear el movil a peticion del usuario y reinstalar) | identico | **BUILD SUCCESSFUL in 2m32s, exit 0**; XML propio: **67 tests / 0 failures / 0 errors / 0 skipped** (`AiSuggestionsE2ETest` 17, `ScanPantryE2ETest` 25, `LanguageSwitchE2ETest` 8, `PincheE2ETest` 17), V2124, 15:49:41, `test-result-exit-code.txt = 0` |

### 1.3 Incidentes de entorno (declarados, sin cambio de codigo)

1. **1.ª corrida de E2E 100 % fallida por entorno, no por la feature**: el movil estaba **bloqueado con candado seguro** (`dumpsys trust`: `deviceLocked=1`, `dumpsys window policy`: `secure=true`, foco en `NotificationShade`) y en **salvapantallas** (`mWakefulness=Dreaming`, foco `com.android.deskclock/.DreamActivity`). Ninguna Activity llego a RESUMED -> `NoActivityResumedException` / `No compose hierarchies found in the app` en los 67 tests. Ademas el **dueno del movil estaba usando Play Store** (instala `com.google.android.apps.maps`, desinstala `com.google.android.apps.photos` ...) y a las **14:22:51 desinstalo `com.whatsforlunch.app`** (`pm path` -> vacio). El validador **no intento saltarse el candado**.
2. El usuario respondio a la pregunta del validador con **«desbloqueo el movil y avisas»**; con el movil desbloqueado se reinstalo (`.\gradlew.bat :app:installDebug` -> `Installed on 1 device`, `Events injected: 1`, `MainActivity` en primer plano, pid 1423) y la 2.ª corrida salio verde (1.2).
3. **Ajustes del dispositivo tocados y restaurados**: `screen_off_timeout` 1800000 -> 3600000 -> **1800000** y `screensaver_enabled` 1 -> 0 -> **1** (verificado con `adb shell settings get` tras la corrida). Nada mas se modifico en el dispositivo; la app queda instalada y en primer plano, como la deja `init.ps1`.
4. **XML respaldados fuera del repo** (`C:\Users\Daniel\AppData\Local\Temp\opencode\e2e-backup-mejorar-prompts\`): `TEST-V2124-implementador-1317.xml` (67/0/0/0 del implementador) y `TEST-V2124-validador-intento-fallido-1422.xml` (la corrida ambiental). El XML del directorio de resultados quedo con la corrida valida del validador (patron N1: log + informe como evidencia).

### 1.4 Tier caro - evidencia inspeccionada (sin re-ejecucion)

- **E2E del implementador** (XML de **13:17:03**, respaldado): **67 / 0 fallos / 0 errores / 0 omitidos** con el mismo desglose 17/25/8/17. **Sin deriva sobre lo que la E2E toca**: todos los ficheros que ejercitan las 4 suites (12 assets, `PromptSource.kt` 12:49, `PincheChatProtocol.kt` y `OpenAiCompatiblePincheChatClient.kt` 12:58, tests 13:04-13:10) eran **previos a 13:15**; el delta posterior (13:30-13:32) son `RecipeContract.kt` + los dos `buildBody` + su test, que la E2E **no ejercita** (usa `FakeRecipeSuggester`, `CapturingRecipeSuggester`, `AlwaysFailingScanAnalyzer` - grep de `app/src/androidTest` sin ninguna referencia a `OpenAiCompatible*`, `recipeJsonSchema`, `buildBody` ni `response_format`) y que si cubren los unit re-ejecutados en verde.
- **Smoke test** (`docs/evidence/mejorar-prompts-y-contratos-de-ia/`): `informe.md` leido integro; `smoke-test.log` (intento 3, 13:33) y `smoke-test-intento1.log` (intento 1, 13:26) leidos integros; **no se re-ejecuto ninguna llamada de pago** (prohibido).
- **Imagen** `smoke-imagen-generada.png`: **abierta y revisada por el validador** - tortilla de patatas con cebolla, luz natural, primer plano, fondo neutro, sin texto, sin marcas de agua, sin personas ni manos y **sin `<<<`/`>>>` visibles** -> RF-MPC9 cumplido. Dimensiones **1024x1024**. Firma de archivo: ver hallazgo **L2** (es JPEG, no PNG).

---
## 2. Validacion contra la spec

### 2.1 RF-MPC1 ... RF-MPC10

| RF | Cumplimiento | Evidencia |
|---|---|---|
| **RF-MPC1** roles + delimitadores + nota | **Cumplido con el defecto M1** | `recipeSuggestionSystemPrompt`/`recipeSuggestionUserPrompt` y `OpenAiCompatibleScanImageAnalyzer.buildBody` emiten `system`/`user`; `wrapUserData` delimita `{FOODS}`, `{RECIPES}`, chips de perfil y `{DISH}`/`{STORY}`; nota «dato del usuario»/«user data» en `suggestion`, `pantry_scan` y `pinche_profile` (verificado a mano en los assets). **Pero** el mensaje `system` del Escaner arranca con el literal `@@system@@` -> **M1** |
| **RF-MPC2** `json_schema` + limites | **Cumplido** | `recipeJsonSchema()`/`scanJsonSchema()` en `RecipeContract.kt` con `maxItems` 3/10/12, enums `mm` (4) y `df` (3), `required`, `bs` 1..12, `pm`/`m` 1..1439 (`type: ["integer","null"]`), Escaner 1..99; sobre OpenAI `{"type":"json_schema","json_schema":{name,schema}}` (hallazgo D2 enmendado). `AiContractPayloadTest` (7) lo fija; smoke intento 3 -> HTTP 200 sin fallback |
| **RF-MPC3** reintento unico + log | **Cumplido** | `shouldFallbackToJsonObject(400..499)` testeado (400/404/429 si; 200/500 no); `executeOnce` devuelve `null` solo con `useJsonSchema && 4xx` -> un unico reintento con `json_object`; el 2.º fallo cae en `Failed(PROVIDER_ERROR)` (rutas visibles existentes); log `"json_schema rejected, falling back to json_object"` con codigo HTTP. Evidencia de proveedor: intento 1 (log crudo: 400 -> fallback disparado en ambas superficies) + intento 2 narrado en el informe -> ver **L1** |
| **RF-MPC4** robustez JSON | **Cumplido** | `suggestion_*`: «JSON valido: sin comentarios, sin texto antes ni despues y sin comas finales» + fallback `{"s":[]}`; `pantry_scan_*`: identico + `{}`; `pinche_system_*`: identico en «Formato de la propuesta». Cubierto por `SuggestionPromptTest` (`sin comas finales` / `no trailing commas`) |
| **RF-MPC5** Sugerencias | **Cumplido** | `suggestion_*`: respeta cantidades `nombre (n)`, prioriza las de mas cantidad, «solo los alimentos que indique el usuario mas la despensa basica (sal, aceite, agua, azucar y especias basicas)»; en `system`. Test `systemPromptCarriesTheContractAndTheRulesWithoutUserData`. Respuesta real del intento 3 lo confirma (arroz/tomate/queso + aceite/agua/sal/pimienta/oregano) |
| **RF-MPC6** anti-duplicados | **Cumplido** | `suggestion_avoid_*`: «Tampoco variantes menores ni renombradas del mismo plato; ante la duda, propone otra cosa distinta»; seccion solo con Recetas que evitar (RF-ASR6 intacto). Tests `spanishUserPrompt...`/`englishUserPrompt...` |
| **RF-MPC7** Escaner | **Cumplido** | `pantry_scan_*`: un solo nombre por alimento, singular, sin marcas, sinonimos fusionados, sin platos preparados ni objetos no alimentos, «si no puedes estimar la cantidad, pon 1», `@@user@@` con `<<<imagen>>>` |
| **RF-MPC8** Pinche y perfil | **Cumplido** | `pinche_system_*` en 5 secciones (Rol y tono - Herramientas - Cuando proponer receta - Formato de la propuesta - Seguridad culinaria) con alergias por encima de todo, coccion segura de carne/pescado/huevo, sin consejos medicos y «si necesitas un dato que no tienes, pregunalo»; `pinche_profile_*` con «No preguntes por partes ya declaradas» y «vuelve a llamar a save_profile con la lista completa: sustituye a la anterior». Tests `systemPromptsAreOrganizedInSectionsInTheTwoLanguages` y `profilePromptAddsCorrectionAndNoRepeatedQuestionsRules` |
| **RF-MPC9** foto del plato | **Cumplido** | `dish_photo_*` con estilo completo es/en; sin nota «dato del usuario» (decision documentada: contaminaria la imagen). Imagen del smoke revisada visualmente (seccion 1.4) |
| **RF-MPC10** rol `tool` | **Cumplido** | `PincheChatProtocol.toolResultMessage(callId, content)` extraido y usado por el bucle (`OpenAiCompatiblePincheChatClient.kt`); regresion `toolResultMessageKeepsTheToolRoleAndItsCallId` (rol `tool`, `tool_call_id`, `assertNotEquals("user")`) |

### 2.2 Escenarios S1-S7

- **S1 (paridad y marcadores)**: **OK** - recuento propio sobre los 12 assets: **0 BOM** en los 12; en los 6 pares es/en los placeholders `{...}` son **identicos** y la presencia de marcadores tambien (`@@user@@` solo en `suggestion` y `pantry_scan`, `@@system@@` solo en `pantry_scan`, ocurrencias de `<<<` es/en = 0/0, 1/1, 1/1, 0/0, 2/2, 0/0). `PromptAssetsTest.promptPairsKeepTheSameMarkersSectionsInBothLanguages` reproduce lo mismo.
- **S2 (payload con `json_schema` y limites)**: **OK** - `AiContractPayloadTest.recipeSchemaCarriesTheEnumsAndLimitsOfTheContract`, `...ResponseFormatUsesTheOpenAiShapeWithTheSchemaWrapper`, `suggestionPayloadSeparatesSystemFromDelimitedUserData` (2 mensajes, `system` con contrato y sin `{FOODS}`/datos, `user` con `<<<Manzana (3)>>>`), `scanPayloadPutsTheRulesInSystemAndThePhotoInUser`.
- **S3 (fallback)**: **OK estructural + evidencia de proveedor parcial** - decision y builders puros testeados; el reintento en si no tiene test JVM (sin MockWebServer, decision de la spec) y su verificacion real descarga del smoke -> **L1**.
- **S4 (resultados de tools con rol `tool`)**: **OK** - test en `PincheChatProtocolTest`.
- **S5 (textos de Sugerencias)**: **OK** - `SuggestionPromptTest` (cantidades, despensa basica, variantes menores, robustez JSON, seccion ausente sin Recetas).
- **S6 (Escaner, Pinche, foto)**: **OK** - `PinchePromptsTest` + `PromptAssetsTest` (paridad, notas, composicion con delimitadores y `dish_photo` con/sin historia).
- **S7 (smoke de pago)**: **OK con reservas documentadas** - Sugerencias y Escaner `json_schema` -> **HTTP 200** con JSON valido (intento 3, `smoke-test.log`), imagen -> **HTTP 200** con imagen 1024x1024 revisada; fallback 400 -> `json_object` -> 200 narrado sin log crudo (**L1**) y archivo **JPEG etiquetado PNG** (**L2**).

### 2.3 Non-goals y alcance

- **Contrato de tools del Pinche y parsers intactos**: `PincheChatProtocol.kt` solo gana `toolResultMessage` (9 lineas); `PincheSseParser`, `RecipeSuggestionParser`, `ScanAnalysisParser` **no aparecen** en `git status`.
- **Sin `response_format` en el Pinche**: grep del diff de `OpenAiCompatiblePincheChatClient.kt` -> 0 coincidencias de `response_format`/`json_schema`; `Q-resolver-dudas-con-pinche-16` intacta.
- **Payload de imagen intacto**: `OpenAiCompatibleImageGenerator.kt` no esta en el diff.
- **Sin UI, dominio, Room, topes de contexto ni permisos**: `git status` limitado a `data/ai/`, `assets/prompts/`, tests JVM, docs y ficheros de estado; **cero** cambios en `AndroidManifest.xml`, `*.gradle.kts`, `libs.versions.toml`, `strings.xml`, `domain/`, room.
- **Sin dependencias nuevas** (MockWebServer sigue descartado; builders puros).
- **Tareas T1-T10** marcadas y verificadas una a una; **Validator Checklist** de la spec cumplida salvo lo que cubren M1/M2.

---

## 3. Paridad, BOM y seguridad

- **BOM**: los 12 assets leidos byte a byte -> `BOM=False` en los 12 (coincide con la evidencia).
- **Paridad es/en**: 6/6 pares con placeholders identicos y marcadores identicos (seccion 2.2 S1).
- **Secretos**: valor de `AI_API_KEY` (25 caracteres) extraido de `local.properties` en memoria y **buscado sin imprimirlo**: **ausente** del diff completo y de los 3 ficheros de texto de la evidencia. Barridos extra del diff por `sk-...`, `Bearer <...>`, `eyJ...`, `AI_API_KEY=` -> **0 coincidencias**. Logs del smoke sin key ni PII (datos sinteticos: «platano», arroz/tomate/queso, «banana.jpg»).
- **`local.properties`**: en `.gitignore:5` y **no trackeado** (`git ls-files local.properties` -> vacio; `git check-ignore -v` -> `.gitignore:5`). Solo `AI_API_KEY` + `AI_BASE_URL` + `sdk.dir` (acceso al fichero solo para la comprobacion anterior, sin volcar su contenido).
- **Permisos/dependencias/manifiesto**: sin cambios (seccion 2.3).
- **Llamadas de pago**: el validador **no ejecuto ninguna**; solo inspecciono la evidencia existente, como indica el encargo.

---
## 4. Hallazgos

### M1 - El marcador `@@system@@` se envia literal en el mensaje `system` del Escaner (es y en)

- **Severidad**: Medium
- **Evidencia**: `app/src/main/assets/prompts/pantry_scan_es.txt` y `pantry_scan_en.txt` **linea 1** = `@@system@@`; `PromptSource.kt:52-62` define solo `USER_SECTION_MARKER = "@@user@@"` y `splitSystemUser` hace `template.take(index).trim()` (el `trim()` solo quita espacios, no `@@system@@`), y luego `OpenAiCompatibleScanImageAnalyzer.kt:138` envia ese trozo como mensaje `system`. Grep en `app/src` -> **ningun sitio** elimina `@@system@@`. `AiContractPayloadTest.scanPayloadPutsTheRulesInSystemAndThePhotoInUser` solo asserta `contains("Analiza la foto")`, asi que el literal pasa inadvertido.
- **Por que importa**: es el entregable central de la feature -el payload que sale del dispositivo- y lleva un token interno de seccion que el propio mecanismo de split no reconoce. `suggestion` no lo lleva (inconsistencia entre prompts); un lector futuro dara por hecho que `@@system@@` se maneja igual que `@@user@@`. Es exactamente la «polucion de prompt» que RF-MPC1 busca evitar. No se observo dano funcional (el intento 3 del Escaner respondio 200), pero nada lo impide ni lo cubre.
- **Cambio requerido**: que el mensaje `system` del Escaner no contenga `@@system@@`, y que un test lo fije.
- **Pasos sugeridos**:
  1. Decidir una sola de: (a) eliminar la linea `@@system@@` de los dos assets, o (b) tratarla como marca simetrica -constante `SYSTEM_SECTION_MARKER` y quitarla en `splitSystemUser` (p. ej. `template.take(index).removePrefix(SYSTEM_SECTION_MARKER).trim()`).
  2. Anadir en `AiContractPayloadTest` (o `PromptAssetsTest`) un assert de contenido del `system` del Escaner: `assertFalse(system.contains("@@"))` y, en su caso, `assertTrue(system.startsWith("Analiza la foto"))` / `startsWith("Analyze the photo")`.
  3. Mantener la paridad es/en (el marcador esta en ambos pares).
- **Verificacion tras el arreglo**:
  - `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` -> 0 fallos con el assert nuevo.
  - `.\gradlew.bat :app:lintDebug` -> 0 errores.
  - Recuento propio: busqueda de `@@` en `pantry_scan_*.txt` coherente con la decision tomada.

### M2 - `docs/technical-discovery.md` documenta el sobre `json_schema` plano (que el smoke probo que da HTTP 400) y dice que el soporte sigue sin confirmar

- **Severidad**: Medium
- **Evidencia**:
  - Linea **90** (Escaner): «el payload lleva `response_format = {"type":"json_schema","schema":...}`» - **la forma plana**, que el smoke devolvio **HTTP 400** «`response_format.json_schema` is required».
  - Linea **100** (Sugerencias): identico, **ademas** «el soporte de `json_schema` sigue sin confirmar, ver `⚠ S08`».
  - Linea **143** (Timeouts y diagnostico): «**`{"type":"json_schema"}` no esta confirmado** ... verificar con el smoke test de pago».
  - Linea **108**: sigue citando la funcion **`recipeSuggestionPrompt`**, que **ya no existe** (renombrada a `recipeSuggestionSystemPrompt`/`recipeSuggestionUserPrompt`; grep en `app/src` -> 0 resultados).
  - Contradice a `docs/risks-and-open-questions.md:43` (S08: «`json_schema` **confirmado** ... forma plana -> HTTP 400, forma OpenAI -> HTTP 200») y a `Q-mejorar-prompts-y-contratos-de-ia-1`, y a la propia spec RF-MPC2 enmendada y al codigo (`RecipeContract.kt:126-131`, `responseFormatJsonSchema`).
- **Por que importa**: `AGENTS.md` designa `docs/technical-discovery.md` como hogar de las decisiones de stack/llamada; la spec lo lista en *Durable Documentation Impact* como «update - contratos de llamada a la IA, fallback y prompts». Hoy un agente que implemente la proxima llamada siguiendo ese documento **reintroduciria el sobre que devuelve 400** y creeria que el soporte esta sin confirmar pese a que S08 ya lo confirma. Es la contradiccion «docs vs. comportamiento implementado» del rubrico.
- **Cambio requerido**: que `technical-discovery.md` describa el sobre real, el estado real del soporte y las funciones reales.
- **Pasos sugeridos**:
  1. Lineas 90 y 100: sustituir `{"type":"json_schema","schema":...}` por `{"type":"json_schema","json_schema":{"name":...,"schema":...}}` (forma estandar de OpenAI, hallazgo del smoke 2026-11-12) y **quitar/actualizar** «sigue sin confirmar» / «verificar con el smoke test de pago» -> «confirmado en el smoke test: forma plana -> 400, forma OpenAI -> 200; con reintento unico a `json_object` en 4xx (RF-MPC3)».
  2. Linea 143: misma correccion («`json_schema` **confirmado** con la forma OpenAI» en lugar de «no esta confirmado»).
  3. Linea 108: `recipeSuggestionPrompt` -> `recipeSuggestionSystemPrompt` / `recipeSuggestionUserPrompt`.
  4. Releer el parrafo completo para que no quede ninguna frase que contradiga a `docs/risks-and-open-questions.md` S08/Q-1 ni a la spec.
- **Verificacion tras el arreglo**:
  - Busqueda de `json_schema` en `docs/technical-discovery.md` -> ninguna forma plana ni frase «sin confirmar».
  - Busqueda de `recipeSuggestionPrompt` en `docs/technical-discovery.md` -> 0 resultados.

### L1 - El log crudo del intento 2 del smoke (el que verifico el fallback de extremo a extremo y genero la imagen) no se conservo

- **Severidad**: Low
- **Evidencia**: `docs/evidence/.../` contiene solo `smoke-test-intento1.log` (intento 1) y `smoke-test.log` (intento 3); el propio `informe.md:25` lo declara («Tres intentos ... Logs: intento1 e intento 3»). El intento 2 es el que afirma «400 -> `json_object` -> **HTTP 200**» en ambas superficies y la imagen 200; su respaldo crudo no existe en el repo ni en el temporal del validador. `feature_list.json` lo resume como «3 intentos, logs e informe».
- **Por que importa**: es la unica afirmacion de RF-MPC3 contra el proveedor real que no tiene artefacto crudo. Esta corroborada de forma indirecta (intento 1 loguea el 400 real y el fallback disparado en ambas superficies; el informe cita el cuerpo del 400 con letra calva y timings concretos; el PNG existe con timestamp 13:29:28 = intento 2), y las 4 `verification` de `feature_list.json` tienen evidencia propia. Por eso es Low y no bloquea.
- **Cambio requerido**: o bien adjuntar el log del intento 2 si sigue disponible, o bien ajustar la redaccion («intentos 1 y 3 con log; intento 2 documentado en el informe») para que `feature_list.evidence` no prometa mas logs de los que hay. **No** re-ejecutar llamadas de pago para regenerarlo.
- **Verificacion tras el arreglo**: listado de `docs/evidence/mejorar-prompts-y-contratos-de-ia/` coherente con lo que afirman `informe.md` y `feature_list.json`.

### L2 - `smoke-imagen-generada.png` es un JPEG, no un PNG

- **Severidad**: Low
- **Evidencia**: firma de archivo `FF D8 FF E0 00 10 4A 46 49 46` (JFIF) y `System.Drawing.Image.FromFile(...).RawFormat` = codec **JPEG**; dimensiones **1024x1024** correctas. En cambio `informe.md:37`, `feature_list.json` (evidence 3), `PROGRESS.md` y `docs/progress-history.md` lo llaman «**PNG** 1024x1024».
- **Por que importa**: la evidencia no coincide literalmente con lo afirmado en cuatro artefactos duraderos; alguien que intente validar la firma IHDR esperara `89 50 4E 47` y la dara por falsa. El contenido sustancial (imagen valida 1024x1024 generada con `model`/`prompt`/`size`, RF-MPC9 cumplido - revisada por el validador) **se sostiene**.
- **Cambio requerido**: corregir el formato en los textos («JPEG 1024x1024») o renombrar el fichero a `.jpg`; actualizar `informe.md`, la entrada de `evidence` de `feature_list.json`, `PROGRESS.md` y `docs/progress-history.md` de forma consistente.
- **Verificacion tras el arreglo**: busqueda de «PNG» en la evidencia, `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` sin referencias erroneas a este fichero.

### Notas sin cambio requerido

- **N1**: `shouldFallbackToJsonObject` devuelve `true` para **cualquier** 4xx, incluidos 401/403 - una key invalida provocara dos peticiones en lugar de una (mismo resultado final `Failed`). Es lo que pide RF-MPC3 literalmente («HTTP 4xx»); se anota por coste, no como defecto.
- **N2**: `elapsedMs(startedAt)` comparte el `startedAt` entre los dos intentos, asi que la linea de log del reintento incluye la duracion del primer intento. Cosmetico.
- **N3**: `PromptSource.kt:63-64` doble linea en blanco tras `splitSystemUser`. Cosmetico.
- **N4**: el esquema no fija `strict: true` ni `additionalProperties: false`, asi que la validacion en el proveedor es *best effort* (la normalizacion sigue en `RecipeSuggestionParser`/`ScanAnalysisParser`, como prevé la spec). RF-MPC2 no pide `strict`; conforme a contrato.
- **N5**: `<<<imagen>>>` viaja como texto del mensaje `user` junto a la imagen (marcador breve previsto en el Technical Approach paso 4); sin datos de usuario reales que delimitar.
- **N6**: `recipeJsonSchema` deja `st`, `pm`, `bs`, `kcal`, `q`, `u`, `m` fuera de `required` (opcionales), coherente con el parser tolerante.

---
## 5. Calidad del enfoque y arquitectura

- **Builders puros**: `buildBody(...)` paso de `private` a `internal` y devuelve `JSONObject`; probado en JVM sin red ni MockWebServer, patron ya establecido en el repo (`recipeSuggestionPrompt` lo era). Buena extraccion: el split, el sobre y el `response_format` quedan verificables.
- **Decision de fallback pura** (`shouldFallbackToJsonObject`) + orquestacion minima en `executeOnce` (devuelve `null` solo cuando toca reintentar) -> el reintento unico es facil de leer y de probar. Los caminos de error (red, 5xx, parseo) **no** reintentan, como pide la spec.
- **Esquema vs. contrato**: los limites de RF-MPC2 viven solo en `RecipeContract.kt` y el prompt solo los recuerda en texto (paso 1 del Technical Approach, respetado). El esquema refleja enums, `required` y todos los topes pedidos.
- **Ambiguedades resueltas**: la forma del sobre (D2 enmendado con el hallazgo del smoke), el ingrediente fuera de la lista (Q-...-2), la nota ausente en `dish_photo` (justificada) y el bloque de perfil que sigue en `system` (decision aceptada de `crear-mi-perfil` D6, con delimitadores) quedan documentados. **Quedan abiertas solo M1 y M2.**

---

## 6. Documentacion duradera y handoff

- **Spec**: completa, RF-MPC1-10 alineadas con lo implementado, D2 enmendada con el hallazgo del smoke, T1-T10 marcadas, checklist de validador cumplida.
- **`docs/risks-and-open-questions.md`**: `⚠ S08` ampliada y **correcta** (forma OpenAI confirmada) + `Q-mejorar-prompts-y-contratos-de-ia-1` y `-2` resueltas. Bien.
- **`docs/technical-discovery.md`**: actualizado en el sitio correcto pero con el contenido erroneo de **M2**.
- **`docs/progress-history.md` / `PROGRESS.md` / `feature_list.json`**: entrada de sesion 133 y bullet de estado consistentes con la evidencia; en `feature_list.json` solo se anadio la entrada nueva de esta feature (status `passing`, 4 `evidence`) - **ninguna otra feature tocada**.
- **`DESIGN.md`, `CONTEXT.md`, `AGENTS.md`, `docs/domain-model.md`**: sin tocar, correcto (sin UI, sin terminos nuevos, sin cambios de flujo).
- **Harness**: `init.ps1` **no se modifico**; `.\init.ps1` paso en verde en la primera invocacion del validador.

---

## 7. Resumen de veredicto

- **Veredicto**: **`revise`** - 0 Critical, 0 High, **2 Medium** (M1, M2), **2 Low** (L1, L2) + 6 notas.
- **Checks re-ejecutados (verde)**: `init.ps1` exit 0 · `lintDebug --rerun-tasks` 0 errores / 7 warnings (0 en el diff) · `testDebugUnitTest --rerun-tasks` **52 suites / 405 tests / 0 fallos** · `assembleDebug` + `compileDebugAndroidTestKotlin` OK · **E2E focalizada re-ejecutada 67/67** en V2124 (tras incidente ambiental declarado en 1.3).
- **Inspeccionado sin re-ejecucion**: E2E del implementador 67/67 (XML previo, sin deriva sobre lo que ejercita), smoke test completo (informe + 2 logs + imagen revisada), paridad es/en y BOM de los 12 assets, barrido de secretos, alcance del diff.
- **Seguridad**: sin hallazgos (sin secretos ni PII en diff ni evidencia; `local.properties` gitignored y no trackeado; sin permisos ni dependencias nuevos; E2E con fakes; ninguna llamada de pago re-ejecutada por el validador).
- **Seguimiento requerido**: cerrar **M1** y **M2** (reparacion rapida de la seccion 8) y, opcionalmente, **L1**/**L2** en el mismo pase; despues re-validar (tier barato + E2E focalizada) y solo entonces persistir `accepted`.

---

## 8. Implementation Repair Brief

**Goal**: eliminar el marcador interno `@@system@@` del payload real del Escaner y alinear `docs/technical-discovery.md` con el sobre `json_schema` y el estado real del soporte.

**Do**:
1. Quitar `@@system@@` de `app/src/main/assets/prompts/pantry_scan_es.txt` y `pantry_scan_en.txt` (o, si se prefiere simetria, anadir `SYSTEM_SECTION_MARKER` y eliminarla en `splitSystemUser`); mantener la paridad es/en.
2. Anadir un assert que fije el contenido del mensaje `system` del Escaner sin `@@` (en `AiContractPayloadTest` o `PromptAssetsTest`), en es y en.
3. Corregir `docs/technical-discovery.md`: lineas 90 y 100 con la forma OpenAI `{"type":"json_schema","json_schema":{name,schema}}` y sin «sigue sin confirmar»; linea 143 sin «no esta confirmado» (S08 ya lo confirma); linea 108 `recipeSuggestionPrompt` -> `recipeSuggestionSystemPrompt`/`recipeSuggestionUserPrompt`.
4. (Opcional, mismo pase) L1: adjuntar el log del intento 2 o suavizar la redaccion de `informe.md`/`feature_list.evidence`; L2: renombrar/corregir «PNG» -> «JPEG» en `informe.md`, `feature_list.json`, `PROGRESS.md` y `docs/progress-history.md`.

**Do not**:
- No tocar `response_format` del Pinche, parsers, contrato de tools, dominio, Room, UI, permisos ni dependencias.
- No re-ejecutar llamadas de pago a la API de IA.
- No editar scripts del harness (`init.ps1`/`e2e.ps1`), `uxdocs/` ni `.opencode/`.
- No cambiar el status en `feature_list.json` (lo persiste el orquestador tras la re-validacion).

**Run**:
- `.\gradlew.bat :app:lintDebug`
- `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks`
- `.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin`
- E2E focalizada en el fisico V2124 desbloqueado: `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.AiSuggestionsE2ETest,com.whatsforlunch.app.ScanPantryE2ETest,com.whatsforlunch.app.LanguageSwitchE2ETest,com.whatsforlunch.app.PincheE2ETest"` (esperado 67/67).
- Verificacion documental: busquedas de M2 de la seccion 4.

**Update**:
- `docs/evidence/mejorar-prompts-y-contratos-de-ia/informe.md` (y, si aplica, `feature_list.json` solo en `evidence`) para que reflejen M1/L1/L2.
- `PROGRESS.md` + `docs/progress-history.md` con la entrada de la sesion de reparacion.
---

# 9. Re-verificacion acotada tras el repair (2026-11-12)

- **Alcance**: SOLO los hallazgos M1, M2, L1 y L2 del revise y su efecto. No se rehace la validacion completa; los apartados 1-8 se mantienen como registro de la primera pasada.
- **Repair aplicado por el orquestador** (entrada `feature_list.json` L1187): `splitSystemUser` retira `@@system@@`, aserciones nuevas en `AiContractPayloadTest`, `technical-discovery.md` corregido, L1 documentado, imagen renombrada a `.jpg` con referencias actualizadas.
- **Estado al re-validar**: `feature_list.json` sigue en `passing`; este informe no lo ha tocado.

## 9.1 Estado final de los hallazgos

### M1 - `@@system@@` en el system del Escaner: **CERRADO**

- **Codigo** (`PromptSource.kt:49-66`): nueva constante `SYSTEM_SECTION_MARKER = "@@system@@"` con comentario explicito («documenta la seccion en el asset pero se retira al componer: nunca debe llegar literal al modelo») y `splitSystemUser` que devuelve `system.removePrefix(SYSTEM_SECTION_MARKER).trim() to user.trim()`; el caso `index < 0` (sin `@@user@@`) sigue devolviendo el template entero en `system`.
- **Tests** (`AiContractPayloadTest`, sigue en 7 tests, todos en verde): L105 `assertFalse(system...contains("@@"))` en el system de **Sugerencias**; L129-130 comentario «M1 del validador» + `assertFalse(system...contains("@@"))` en el system del **Escaner**; L135 `assertFalse(content.getJSONObject(0).getString("text").contains("@@"))` en el **user** del Escaner (`<<<imagen>>>`).
- **Verificacion independiente del validador** (no se apoya solo en el test): replica exacta de `splitSystemUser` en PowerShell sobre los 12 assets reales - `suggestion` es/en y `pantry_scan` es/en: **system sin `@@` = True, user sin `@@` = True**, y el system del Escaner empieza ya por «Analiza la foto y enumera...» / «Analyze the photo and list...»; los otros 4 prompts (`pinche_system`, `pinche_profile`, `suggestion_avoid`, `dish_photo`) **no contienen `@@` en absoluto**. Ningun `@@` llega al modelo en ninguna de las 12 superficies.
- **Coherencia con `suggestion`**: `suggestion` nunca tuvo `@@system@@` (solo linea de instrucciones + `@@user@@`); ahora ambos prompts pasan por el mismo `splitSystemUser` y producen la misma forma de salida, sin ramas distintas. Los 6 pares siguen con placeholders y marcadores identicos (S1 intacto).

### M2 - `docs/technical-discovery.md`: **CERRADO**

- **L90 (Escaner)**: `response_format = {"type":"json_schema","json_schema":{"name":"scan_analysis","schema":...}}` con la nota «forma estandar de OpenAI; la forma plana `{"type":"json_schema","schema":...}` devuelve HTTP 400, `Q-mejorar-prompts-y-contratos-de-ia-1`». La forma plana solo aparece como **la que falla**, no como vigente.
- **L100 (Sugerencias)**: sobre OpenAI `{"name":"recipe_suggestions","schema":...}` + «el soporte de `json_schema` quedo **confirmado** en el smoke test de pago de 2026-11-12, ver `⚠ S08`».
- **L108**: `recipeSuggestionSystemPrompt`/`recipeSuggestionUserPrompt` (con `AiContractPayloadTest` para payloads y esquemas). Grep de `recipeSuggestionPrompt` en `docs/technical-discovery.md` -> **0 resultados**.
- **L143**: «`{"type":"json_schema"}` ... con la forma estandar de OpenAI ..., confirmado en el smoke test de pago de 2026-11-12 (`Q-...-1`, `⚠ S08`); la forma plana ... devuelve HTTP 400».
- **Barrido de contradicciones** sobre todo `docs/`: ninguna frase «sin confirmar»/«no esta confirmado» referida a `json_schema` (los unicos matches son los supuestos del Escaner `Q-escanear-despensa-con-camara-1..3` y el texto genericamente introductorio de `risks`); las 3 restantes apariciones de la forma plana son `docs/risks-and-open-questions.md:215` (Q-1, documentando que devuelve 400) y `docs/progress-history.md:1556` (historial, idem) - **contexto negativo correcto**. Sin contradiccion con `⚠ S08` ni con la spec RF-MPC2.

### L1 - log del intento 2: **CERRADO**

- `docs/evidence/.../informe.md:25`: «Logs: `smoke-test-intento1.log` (intento 1) y `smoke-test.log` (intento 3, definitivo); **el log crudo del intento 2 se sobrescribio con el intento 3** (patron N1 de la sesion 115) -sus resultados quedan citados integros en este apartado y el intento 1 conserva el rastro del primer rechazo de `json_schema`-».
- `docs/progress-history.md:1556` (Historial, sesion 133): «Evidencia: ... (`informe` + `smoke-test.log` + `smoke-test-intento1.log` + JPEG; **el log crudo del intento 2 se sobrescribio con el 3 -patron N1- y sus resultados quedan citados en el informe**)». Coincide con lo que afirma `feature_list.json` L1187. Ambos sitios, como pedia el repair.

### L2 - imagen JPEG: **CERRADO salvo un residuo (L2-r)**

- Fichero: `docs/evidence/mejorar-prompts-y-contratos-de-ia/smoke-imagen-generada.jpg` (727.202 B, mtime 13:29:28 = intento 2; el `.png` ya no existe en el directorio). JPEG real confirmado por firma `FF D8 FF E0` y por `System.Drawing` (codec JPEG), 1024x1024.
- Referencias corregidas y verificadas: `informe.md:37` (`.jpg` + «el proveedor devuelve bytes JPEG en `b64_json` -hallazgo L2 del validador, cerrado renombrando-»), `feature_list.json` evidence L1185 («JPEG 1024x1024 (smoke-imagen-generada.jpg...)»), `PROGRESS.md:5` y `docs/progress-history.md:1556` («JPEG 1024x1024 ... el proveedor devuelve bytes JPEG en `b64_json`»).
- Barrido de `smoke-imagen-generada.png` y de «PNG 1024» en esos 4 ficheros: **0 resultados**.
- **Residuo L2-r (Low)**: `docs/evidence/mejorar-prompts-y-contratos-de-ia/informe.md:54`, tabla «Cobertura del smoke test», fila Imagen: **«`✅ 200 + PNG válido`»**. Unica referencia vieja que queda (el resto del fichero ya dice JPEG); no es `.png` ni «PNG 1024» literal, pero describe mal el formato del mismo archivo. Cambio de una palabra al persistir la aceptacion: `PNG valido` -> `JPEG valido`.
- Unica otra mencion de `.png` en los 4 ficheros: `feature_list.json:1186`, que es **mi propia entrada** describiendo el hallazgo («L2 la imagen era JPEG con extension `.png`») - nota historica, correcta.

## 9.2 Checks re-ejecutados por el validador tras el repair (tier barato + E2E)

| Comando | Resultado |
|---|---|
| `.\init.ps1` | **exit 0, «Entorno listo»** (reinstala en `V2124 - 13` -el dueno del movil lo habia vuelto a desinstalar- y arranca la app) |
| `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **52 suites / 405 tests / 0 failures / 0 errors / 0 skipped** (recuento propio del XML; `AiContractPayloadTest` 7, `PincheChatProtocolTest` 19, `PinchePromptsTest` 5, `PromptAssetsTest` 10, `SuggestionPromptTest` 5) - incluye las 3 aserciones nuevas de M1 |
| `.\gradlew.bat :app:lintDebug --rerun-tasks` | **0 errores / 7 warnings**; ficheros: `colors.xml`, `GalleryScanImage.kt`, `ic_launcher_foreground.xml`, `ic_launcher_round.webp`, `mipmap-anydpi-v26`, `ScannerScreen.kt` - **ninguno del diff** |
| `.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** (50 tareas) |
| `.\gradlew.bat :app:connectedDebugAndroidTest` (4 suites focalizadas) | **BUILD SUCCESSFUL in 2m 31s, exit 0**; XML propio: **67 tests / 0 failures / 0 errors / 0 skipped** (`AiSuggestions` 17, `ScanPantry` 25, `LanguageSwitch` 8, `Pinche` 17), V2124, 16:29:22 |

Coinciden con los gates que el orquestador reejecuto (unit 52/405/0, lint 0/7, assemble OK, E2E 67/67 a las 16:15:31 -XML respaldado en el temporal del validador-). Ningun fichero del arbol es posterior a esa evidencia salvo `feature_list.json` (16:16:43, entrada de evidencia, no codigo). **Ninguna llamada de pago re-ejecutada.**

## 9.3 Veredicto final

**`accept`** -> estado recomendado para el orquestador: **`accepted`** (la persistencia la hace el orquestador; este informe no toca `feature_list.json`).

- **0 Critical, 0 High, 0 Medium** - M1 y M2, los dos hallazgos que motivaron el `revise`, estan **cerrados y verificados** por el validador (codigo + tests + comprobacion independiente sobre los assets reales; documentos sin contradicciones).
- **L1 cerrado** (informe + Historial). **L2 cerrado salvo 1 Low residual (L2-r)**: `informe.md:54` dice «PNG valido» donde deberia decir «JPEG valido».
- **L2-r sin cambio requerido para aceptar**: es una palabra en una tabla de un fichero de evidencia, el contenido sustancial (archivo JPEG correcto con nombre correcto y referencias en los otros 3 ficheros) esta verificado. Sugerencia: corregirla en la misma sesion en que se persista la aceptacion (patron «corregido al persistir» de sesiones previas).
- **Observacion de cierre (sin hallazgo)**: el ciclo `revise` -> repair de esta feature todavia no tiene entrada propia en `docs/progress-history.md` ni en `PROGRESS.md` (la entrada de la sesion 133 sigue diciendo «Estado: passing ... siguiente: validacion independiente»). Conviene anadirlo al persistir la aceptacion, junto con L2-r.
- **Seguridad**: sin hallazgos en la re-verificacion (el diff de repair solo toca `PromptSource.kt`, un test, 4 documentos y la evidencia; sin secretos, permisos ni dependencias).