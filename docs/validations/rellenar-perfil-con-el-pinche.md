# Validación independiente - `rellenar-perfil-con-el-pinche`

- **Feature**: `rellenar-perfil-con-el-pinche` - Rellenar Mi perfil conversando con el Pinche
- **Spec**: `docs/specs/rellenar-perfil-con-el-pinche.md` (S1-S7, D1-D8, T1-T7, «Expected File Changes», «Durable Documentation Impact», «Validator Checklist», «Hallazgos de implementación»)
- **Validador**: `feature-validator` (subagente en sesión independiente; no participó en la implementación y solo escribe este informe: el acceptance lo persiste el orquestador)
- **Fecha**: 2026-11-10 · **Rama**: `feature/rellenar-perfil-con-el-pinche` · **HEAD**: `b980eeb` · change set **sin commitear** (30 ficheros modificados + 3 rutas sin seguimiento: `ProfileUpdate.kt`, `OpenAiCompatiblePincheChatClientTest.kt` y `docs/evidence/rellenar-perfil-con-el-pinche/`)
- **Estado en `feature_list.json` al entrar**: `passing` (autoverificación, no aceptación)

## VEREDICTO: accept

Sin hallazgos Critical/High/Medium. 3 hallazgos Low documentales (F1-F3) y 6 notas (N1-N6); ninguno de ellos bloquea la aceptación y se corrigen al persistir. No hay Implementation Repair Brief.

## 1. Objetivo de la revisión

- **Spec/contrato**: Goal, 7 non-goals, S1-S7, `verification` 1-4 de `feature_list.json`, `Expected File Changes`, T1-T7, `Verification Plan`, `Evidence To Capture`, `Validator Checklist` y `Durable Documentation Impact`.
- **Diff revisado**: `git status` + `git diff HEAD` íntegro (fuentes, prompts, strings, tests unit y E2E, DI, docs durables, spec, `feature_list.json`, `PROGRESS.md`, historial): 1390 inserciones / 210 borrados en 30 ficheros + 2 ficheros nuevos + carpeta de evidencia.
- **Evidencia del implementador**: `docs/evidence/rellenar-perfil-con-el-pinche/` (`informe.md`, `gates-lint.txt`, `gates-unit.txt`, `gates-assemble.txt`, `e2e-bateria-am-instrument.txt`, 3 PNG) + los 4 items de `evidence` en `feature_list.json` + cabecera de `PROGRESS.md` + sesión 121 de `docs/progress-history.md`.

## 2. Checks re-ejecutados por el validador (tier barato)

| # | Check | Comando | Resultado (recuento propio) |
|---|---|---|---|
| 1 | Arranque | `./init.ps1` (con `ANDROID_SERIAL=emulator-5554`) | **exit 0**: «Entorno listo», `installDebug` BUILD SUCCESSFUL, `monkey` inyectado, `pm path` + `pidof` OK (app en primer plano) |
| 2 | Lint | `.\gradlew.bat lintDebug --rerun-tasks` | **BUILD SUCCESSFUL - 0 errores / 48 warnings** (recuento propio desde `app/build/reports/lint-results-debug.txt`: 48 incidencias, 0 Error/Fatal) |
| 3 | Unit | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL - 45 suites / 352 tests / 0 failures / 0 errors / 0 skipped** (recuento propio desde `app/build/test-results/testDebugUnitTest/*.xml`) |
| 4 | Build | `.\gradlew.bat assembleDebug compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** (ambos `EXIT=0`) |
| 5 | Paridad es/en | recuento propio de `values/strings.xml` vs `values-en/strings.xml` | **340/340 claves + 5/5 plurales**, diff vacío en ambos sentidos; `pinche_profile_saved` / `pinche_profile_saved_none` en línea 361/362 de ambos locales |
| 6 | Inventario E2E | recuento propio de `app/src/androidTest` | **244 `@Test` / 27 ficheros** (= lo esperado; el reparto por fichero cuadra con la batería, ver §3) |

Detalles de correlación:

- **`init.ps1` - nota de entorno (N3)**: mi primera invocación redirigió el stderr de `adb shell monkey` (`2>&1 | Tee-Object`), lo que con `$ErrorActionPreference = "Stop"` aborta el script en `Invoke-Step` con `NativeCommandError` y sale 1 **sin llegar a «Verificando»** (artefacto conocido, documentado en la sesión 115, nota N3). Re-ejecutado **sin redirección** → **exit 0** completo («Entorno listo»). No es un hallazgo de la feature.
- **Lint sin deriva respecto de la evidencia**: 0 errores / 48 warnings, idéntico a la línea base 48. Desglose por fichero: `strings.xml` 19 (todas en líneas preexistentes: Typos «momento», PluralsCandidate, UnusedResources `scanner_*`, TypographyEllipsis), `KeystoreAiKeyStore.kt` 4, `CameraXScanImageSource.kt` 1, `ic_launcher.xml` 1, `AndroidManifest.xml` 1. **Ninguna incidencia apunta a una línea nueva del diff** (claves nuevas en 361/362 sin incidencias) y **cero warnings en los ficheros Kotlin tocados**.
- **Unit reproducido**: recuentos exactos por suite de la feature desde XML: `UserProfileRulesTest` **19/0**, `PincheChatProtocolTest` **14/0**, `PincheViewModelTest` **22/0**, `PromptAssetsTest` **9/0**, `OpenAiCompatiblePincheChatClientTest` **4/0** (nueva). Totales 45/352/0 = línea base 44/327 +1 suite +25 tests (coincide con el delta de `PROGRESS.md` y de la sesión 121). El desglose por suite que declara `informe.md` es incorrecto (ver F1).
- **Manifest / local.properties / BuildConfig**: `git status` de `app/src/main/AndroidManifest.xml` **vacío** (el `usesCleartextTraffic` transitorio quedó revertido); `local.properties` = solo `sdk.dir=`; `BuildConfig.AI_API_KEY` y `AI_BASE_URL` **vacíos** en `debug` y `androidTest/debug`.

## 3. Checks solo inspeccionados (tier caro) y su evidencia

No se re-ejecutó ningún check caro (sin `connectedDebugAndroidTest`, sin `e2e.ps1`, **ninguna llamada de pago a DevExpert Inference**): la evidencia es exacta, internamente consistente, coherente con el diff y posterior a toda fuente funcional (última fuente funcional: `PincheE2ETest.kt` 09-11 21:10; batería 09-11 22:26; capturas 10-11 10:49-10:50; manifiesto restaurado 10-11 10:54; gates del implementador 10:58-10:59; `informe.md` 11:02; historial 11:18; gates re-ejecutados por el validador 10-11 11:30).

1. **E2E focalizada: 23/23 y regresión 32/32, 0 fallos** (verify 1-3).
   - Evidencia: `docs/evidence/rellenar-perfil-con-el-pinche/informe.md` §E2E (`emulator-5554`): `PincheE2ETest` 16/16 + `MyProfileE2ETest` 7/7 = 23; `ShoppingListE2ETest` 27/27 + `BottomBarE2ETest` 5/5 = 32.
   - **Corroboración independiente del validador**: en la batería íntegra (punto 2) esas mismas 4 clases arrancan con exactamente esos recuentos (16+7+27+5 = 55) y **ninguna falla** (el único fallo de la run es de `SettingsE2ETest`).
   - Salvedad documentada: **no se conservó un XML/log propio de la focalizada** (N1); el único XML conectado conservado en `app/build/outputs/androidTest-results/` es un run abortado por la colisión de entorno (11-09 20:47, `PantryManagementE2ETest` «Process crashed»), ajeno a la evidencia.
2. **Batería completa: `numtests=244`, 1 fallo, 1 omitido.**
   - Evidencia: `docs/evidence/rellenar-perfil-con-el-pinche/e2e-bateria-am-instrument.txt` (`am instrument`, AVD dedicado **`emulator-5556`** API 37, `Time: 1,912.912`, cierre `INSTRUMENTATION_CODE: -1`).
   - **Recuento propio del validador parseando el log**: bloques de inicio `STATUS_CODE: 1` = **244** = `numtests` = inventario 244 `@Test`; reparto por clase reconciliado **1:1 con el inventario de los 27 ficheros**: `PincheE2ETest` 16, `ShoppingListE2ETest` 27, `ScanPantryE2ETest` 25, `AiSuggestionsE2ETest` 16, `CreateRecipeE2ETest` 23 + `CreateRecipeAppE2ETest` 1 + `MyRecipesCardsE2ETest` 2 (los tres en `CreateRecipeE2ETest.kt`, 26), `EditRecipeE2ETest` 12 + `EditRecipeAppE2ETest` 1 + `EditRecipeRouteE2ETest` 1 (en `EditRecipeE2ETest.kt`, 14), `SearchAndFilterRecipesE2ETest` 12, `RecipeDetailE2ETest` 12, `CookModeVoiceE2ETest` 10, `DeleteRecipeE2ETest` 9, `PantryManagementE2ETest` 9, `CookModeTimerE2ETest` 8, `LanguageSwitchE2ETest` 8, `MyRecipesLocalizationE2ETest` 8, `PantryRecipesE2ETest` 8, `CookModeE2ETest` 7, `MyProfileE2ETest` 7, `SettingsE2ETest` 5, `BottomBarE2ETest` 5, `RecipeMigrationTest` 3, `KeystoreAiKeyStoreTest` 3, `MatchBadgeE2ETest` 2, `PantryLocalizationE2ETest` 2, `AppLaunchE2ETest` 1, `CameraXScanImageSourceTest` 1.
   - **1 fallo** = `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures`, `ComposeTimeoutException: Condition still not satisfied after 5000 ms` en `SettingsE2ETest.kt:166` (código -2) - **fuera de este diff** (`SettingsE2ETest.kt` no está modificado), flake ya registrado en las validaciones 115/116/119; el informe lo da como `OK (1 test)` en aislamiento (sin artefacto propio, N2).
   - **1 omitido** = `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` con `AssumptionViolatedException` (código -4, línea 3021 del log) - `assumeTrue` sin `AI_API_KEY` en `local.properties`; línea base conocida.
3. **Verify manual con capturas (mock local, es-ES, 1280×2856).**
   - Las 3 PNG abiertos y revisados **visualmente** por el validador: cabecera `89 50 4E 47` y `System.Drawing` los abre con **1280×2856**; sin PII ni secretos; datos ficticios.
   - `01-entrevista-resumen-es.png`: chat «Pinche» con «Dame una receta» → «Para proponerte algo necesito tu perfil. ¿Tienes alguna alergia?» → «Frutos secos» → burbuja **«He guardado en «Mi perfil»: Alergias: Frutos secos»** → «Anotado. ¿Tienes alguna intolerancia...?» → **sin Ficha** (S1) y barra inferior con las **5 pestañas** («Mi perfil» al final).
   - `02-mi-perfil-guardado-es.png`: «Mi perfil» con las chips **«Frutos secos»** (Alergias) y **«Diabetes»** (Intolerancias) persistidas, «No tengo ninguna» sin marcar en ambas → el resumen del chat **coincide con lo persistido en Room** (mismos valores ficticios).
   - `03-degradacion-abrir-mi-perfil-es.png`: aviso **«El Pinche no pudo responder. Inténtalo de nuevo.»** con «Reintentar» + mensaje estático **«Para responder necesito tu perfil: Intolerancias o enfermedades que limitan la dieta, Tipo de alimentación. Complétalo en «Mi perfil».»** con botón **«Abrir Mi perfil»** → S5 (verify 3).
   - **Sin llamadas de pago**: mock local `127.0.0.1:8765` + clave sintética; verificado por el validador que el proceso mock **no está vivo** (0 procesos `python`, puerto 8765 libre), que no queda `mock_pinche_server.py` en el repositorio, que `local.properties` solo contiene `sdk.dir` y que el manifiesto está en HEAD (`git diff` vacío).

## 4. Validación contra la spec

- **Goal**: cumplido. Con partes pendientes el Pinche entrevista en el chat (una pregunta a la vez), `save_profile` persiste cada respuesta, el resumen refleja lo realmente guardado, y al completarse se continúa con la petición original; las dudas se responden sin perfil; sin IA se conserva el mensaje estático con «Abrir Mi perfil».
- **Non-goals respetados** (verificado sobre el diff):
  - **Única tool nueva `save_profile`**: `PincheChatProtocol.toolsJson()` declara exactamente 3 funciones (test `toolsExposeTheTwoReadOnlyFunctionsWithoutParameters` renombrado afirma `["get_pantry","get_my_recipes","save_profile"]`); **sin `get_profile`**.
  - **Sin tocar Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias**: ningún fichero de esas áreas en el diff (los únicos cambios de producto son `data/ai/*` del Pinche, `domain/profile/*`, `ui/pinche/*`, `di/AppModule.kt` (1 línea), prompts y strings).
  - **Sin permisos ni dependencias nuevas**: `AndroidManifest.xml` y `app/build.gradle.kts`/`gradle/libs.versions.toml` sin cambios.
  - **Room sin cambios de esquema**: cero ficheros bajo `data/local/` en el diff; sin migraciones nuevas.
  - **`crear-mi-perfil` no reescrita**: ni `docs/specs/crear-mi-perfil.md` ni `docs/evidence/crear-mi-perfil/` aparecen en `git status`; la enmienda vive en RD26 de `docs/domain-model.md`, en la spec de esta feature y en `notes`/`evidence`.
  - La conversación y los resúmenes siguen solo en memoria (el chat no se persiste).
- **S1-S7 vs tests**:
  - **S1** (entrevista de una en una → resumen → Ficha al completar; «Mi perfil» muestra lo guardado): `PincheE2ETest.profileInterviewSavesOneAnswerAtATimeAndProposesOnceComplete` (4 turnos, persistencia en Room real, sin `OPEN_PROFILE`/`SAVE` hasta el final, y «Mi perfil» con las chips al terminar) - en verde en la batería.
  - **S2** (duda sin perfil no se bloquea): `PincheE2ETest.cookingDoubtWithEmptyProfileIsAnsweredWithoutAnInterview` (1 llamada, sin resumen, sin «Abrir Mi perfil», sin Ficha) - en verde.
  - **S3** (perfil a medio completar): `PincheE2ETest.halfCompleteProfileKeepsWhatWasSavedAndOnlyAddsTheNewAnswer` (solo pregunta Intolerancias, el resumen no repite Alergias, `Frutos secos` intacto en el repo, perfil final completo) - en verde.
  - **S4** (guarda determinista): `PincheE2ETest.proposalWithPendingProfileShowsTheStaticRequestInsteadOfTheCard` + unit `proposalWithPendingPartsIsNotPaintedAndAddsTheStaticRequest` (texto conservado, `proposal == null`, sin `SAVE`, «Abrir Mi perfil» navega) - en verde.
  - **S5** (degradación): `PincheE2ETest.missingApiKeyWithPendingProfileShowsNoticeAndStaticRequest` + `MyProfileE2ETest.emptyProfileShowsTheAiNoticeAndTheActionOpensItsTab` (verify 4 enmendado: cliente simulado «sin key» inyectado con `loadModules(..., allowOverride = true)`, patrón de `CreateRecipeAppE2ETest`, sin red) + units `missingApiKeyWithPendingPartsAddsTheStaticRequest`, `failedTurnWithPendingPartsAddsTheStaticRequest`, `missingApiKeyWithCompleteProfileKeepsOnlyTheNotice` - en verde.
  - **S6** (guardado fiel y validado): `UserProfileRulesTest` (19) con `applyUpdate` (chip vacía → `invalid_chip`, duplicada sin distinguir mayúsculas → `duplicate_chip`, chips + «No tengo ninguna» → `invalid_chip`, sin campos → `invalid_arguments`, sustitución de sección completa, `pendingParts` vacío al completar) + `OpenAiCompatiblePincheChatClientTest` (4: executor con repo InMemory, caché por turno, errores **sin persistir**) + `PincheChatProtocolTest` (14: schema, parseo, `saved`/`pending`/`error`) - en verde.
  - **S7** (textos es/en y chips literales): paridad **340/340 + 5/5** re-ejecutada + `PromptAssetsTest.profilePromptsCarryTheInterviewRulesInBothLanguages` y `dietVocabularyMatchesTheNamesOfTheProfilePrompt` (vocabulario de 10 nombres como fuente única, es/en) - en verde.
- **`verification` 1-4 de `feature_list.json`**: los cuatro cubiertos por la cadena E2E + unit anterior (coincide con el desglose del `informe.md` §Cobertura tras revisión independiente).
- **`Expected File Changes` vs diff real**: están todos los ficheros previstos (dominio `ProfileUpdate.kt` + `UserProfileRules`, protocolo, puerto `PincheChatClient`, cliente, prompts, ViewModel, Screen, strings y los 6 tests unit/E2E). Desviaciones: `OpenAiCompatiblePincheChatClientTest.kt` (fichero nuevo, **justificado** en «Hallazgos de implementación» de la spec) y `PROGRESS.md`/`docs/progress-history.md`/`feature_list.json` (harness). Dos ficheros **no listados**: `PromptSource.kt` y `di/AppModule.kt` (ver F3). **Sin deriva de producto.**
- **Tareas T1-T7**: todas `[x]` y verificadas una a una contra el código y los tests.
- **`Verification Plan`**: seguido (gates con `--rerun-tasks`, paridad, focalizada con cliente simulado, batería completa sin regresión, manual con datos ficticios y mock local, sin llamadas de pago; el opcional con pago no se ejecutó, como corresponde).
- **Arquitectura**: reglas de negocio solo en `domain/` (`UserProfileRules.applyUpdate`, `ProfileUpdate`); tools declaradas y parseadas en `PincheChatProtocol` (objeto puro, JVM); el cliente (`data/ai`) solo orquesta y persiste vía puerto; la UI (`PincheScreen.savedPartLine`) solo compone strings localizados y `PincheViewModel` delega en `UserProfileRules` para la guarda y la degradación (patrón ya aceptado en este repo). El vocabulario del tipo de alimentación es **una sola fuente** (`DIET_TYPE_VOCABULARY` en `PromptSource.kt`), con `dietTypeName` refactorizado sin cambio de comportamiento (etiquetas idénticas a los `when` previos).
- **Coherencia del contrato**: `MAX_HTTP_CALLS = 5` con comentario D3; errores `invalid_arguments`/`invalid_chip`/`duplicate_chip`/`unknown_diet_type` sin persistir y `unavailable` ante excepción de persistencia (capturada en el bucle de tools); resultado `{"saved":…,"pending":[…]}`; `PincheReplyResult.Success.profileSaved` con las partes del turno ordenadas por `PendingProfilePart`; los mensajes estáticos y el resumen tienen texto en blanco y **no viajan al modelo** (`filter { it.text.isNotBlank() }`), test `staticProfileMessagesNeverTravelToTheModel`.

## 5. Hallazgos por severidad

### Hallazgos que impiden la aceptación: ninguno (0 Critical / 0 High / 0 Medium)

### F1: `informe.md` declara un desglose por suite de unit incorrecto

- **Severity**: Low
- **Evidence**: `docs/evidence/rellenar-perfil-con-el-pinche/informe.md`, línea «Recuento propio desde los XML»: `UserProfileRulesTest` (15), `PincheChatProtocolTest` (14), `PincheViewModelTest` (18), `PromptAssetsTest` (8), `OpenAiCompatiblePincheChatClientTest` (4). Recuento real del validador desde `app/build/test-results/testDebugUnitTest/*.xml` y desde los propios ficheros (`@Test`): **19 / 14 / 22 / 9 / 4** (la línea base de `UserProfileRulesTest` en HEAD es 11 → +8 = 19, en línea con el «+8» de la sesión 121).
- **Why it matters**: es la única cifra por suite de la evidencia; una futura sesión que la use para detectar drift se lleva tres números equivocados. Los **totales sí son correctos** (45 suites / 352 tests / 0 fallos, reproducidos por el validador), por eso no sube de Low.
- **Required change**: corregir el desglose del `informe.md` a `UserProfileRulesTest` (19), `PincheChatProtocolTest` (14), `PincheViewModelTest` (22), `PromptAssetsTest` (9), `OpenAiCompatiblePincheChatClientTest` (4).
- **Suggested implementation**: editar esa única línea del `informe.md`; no tocar nada más.
- **Verification after fix**: `grep "Recuento propio desde los XML" docs/evidence/rellenar-perfil-con-el-pinche/informe.md` muestra 19/14/22/9/4 y la suma sigue cuadrando con 352.

### F2: la sesión 121 del historial cuenta «8 nuevos» en `PincheViewModelTest` (son 7)

- **Severity**: Low
- **Evidence**: `docs/progress-history.md` (sesión 121): «`PincheViewModelTest` reescrito del gate (8 nuevos: gate retirado, guarda, resumen, degradación)». Del diff: **7 tests nuevos** (`emptyProfileStillCallsTheClientAndShowsTheAnswer`, `proposalWithPendingPartsIsNotPaintedAndAddsTheStaticRequest`, `proposalWithCompleteProfileShowsTheCard`, `profileSavedAddsTheSummaryBeforeTheReply`, `missingApiKeyWithPendingPartsAddsTheStaticRequest`, `failedTurnWithPendingPartsAddsTheStaticRequest`, `missingApiKeyWithCompleteProfileKeepsOnlyTheNotice`) y **2 retirados** (`emptyProfileBlocksTheClientAndAsksForEveryPendingPart`, `onlyMissingPartsAreRequested`); `staticProfileMessagesNeverTravelToTheModel` es un renombrado, no un test nuevo. Neto 17 → 22 (+5), coherente con 327 → 352 (+25 en total: 8 + 5 + 5 + 3 + 4).
- **Why it matters**: el desglose por suite es la base para reconciliar futuras líneas base de unit; un +1 espurio rompe la cuadratura (8+5+8+3+4 = 28 ≠ +25 real).
- **Required change**: en la entrada de la sesión 121, «8 nuevos» → «7 nuevos y 2 retirados (neto +5)».
- **Verification after fix**: la suma de los deltas por suite vuelve a dar +25.

### F3: `Expected File Changes` no lista `PromptSource.kt` ni `di/AppModule.kt`

- **Severity**: Low
- **Evidence**: la spec no incluye ninguno de los dos ficheros, ambos modificados en el diff: `PromptSource.kt` (+41: `DIET_TYPE_LABELS`/`DIET_TYPE_ENUM_VALUES`/`DIET_TYPE_VOCABULARY`, la fuente única del vocabulario D8) y `di/AppModule.kt` (1 línea: sexto argumento de `OpenAiCompatiblePincheChatClient`).
- **Why it matters**: «Expected File Changes» es el contrato de alcance; una desviación no justificada en la spec dificulta detectar derivas reales en la próxima validación. Ambos cambios son mecánicos e implícitos en el Technical Approach (vocabulario compartido; dependencia nueva del cliente), por eso es Low.
- **Required change**: añadir los dos ficheros a «Expected File Changes» (o a «Hallazgos de implementación») de `docs/specs/rellenar-perfil-con-el-pinche.md`.

## 6. Seguridad y privacidad (checklist acotada al diff)

**Sin hallazgos relevantes.** Checklist `security-checklist.md` aplicada al spec + diff + archivos de soporte directos:

1. **Secretos y configuración**: barrido propio `sk-[A-Za-z0-9]{10,}` sobre todo el repositorio (md/txt/properties/json/kts/kt/xml, excluyendo `build/`, `.git/`, `uxdocs/`) = **0 coincidencias**; barrido en la carpeta de evidencia (`sk-…`, `AI_API_KEY=`, `Bearer`) = **0**. `local.properties` (gitignored) solo `sdk.dir=`; `BuildConfig.AI_API_KEY`/`AI_BASE_URL` **vacíos** en los dos BuildConfig generados. El `git diff` no contiene `AI_API_KEY=`, `AI_BASE_URL=` ni `Bearer` (solo prosa de docs).
2. **Autenticación/autorización (flujo de escritura del modelo)**: la única escritura del modelo (`save_profile`) está acotada a 5 campos del perfil, con `additionalProperties: false`, enum cerrado de 10 valores y validación previa en `UserProfileRules` antes de tocar Room; ningún dato se escribe sin pasar por `applyUpdate`.
3. **Validación de entrada**: `parseSaveProfileArgs` rechaza JSON no-objeto, tipos incorrectos y `diet_type` fuera del vocabulario; `applyUpdate` rechaza chip vacía, duplicadas (ignorando mayúsculas) y la exclusividad «No tengo ninguna» → **`{"error": …}` sin persistir nada** (unit `invalidSaveProfileReturnsTheErrorAndPersistsNothing` verifica que el repo queda `UserProfile.empty`).
4. **Exposición de datos/privacidad**: datos de salud solo en la conversación y en el perfil local; evidencia **solo con datos ficticios** («Frutos secos»/«Diabetes»/«Vegana»); los mensajes estáticos y el resumen **nunca viajan al modelo** (texto en blanco); logcat sin prompts ni conversación; `RK03` ampliada **sin cerrar** (sigue como puerta de release), `S08` y `RK07` actualizados y `Q-rellenar-perfil-con-el-pinche-1..5` resueltas.
5. **Servicios externos**: sin llamadas nuevas (mock local `10.0.2.2` solo para capturas); `MAX_HTTP_CALLS` acotado a 5; ninguna llamada de pago en toda la validación.
6. **Dependencias/tooling**: sin cambios de dependencias ni de lockfile; artefactos generados ignorados (`app/build`, `.gradle` en `!!`).
7. **Ficheros transitorios y flujo de capturas**: `usesCleartextTraffic="true"` estuvo solo en el APK de captura y quedó **revertido** (manifiesto idéntico a HEAD: `git diff` vacío y `git status` limpio); no queda el script del mock en el repositorio; sin procesos huérfanos (0 `python`, puerto 8765 libre).

## 7. Docs durables, harness y handoff

- **`Durable Documentation Impact` cumplido íntegramente**: `docs/build-brief.md` (línea roja **`⚠ S08`** ampliada en sus dos localizaciones: «tres funciones» y «`save_profile`, acotada al perfil: el único dato que el modelo puede escribir»), `docs/domain-model.md` (**RD26 enmendada** + **RD27 nueva** + 7 casos límite nuevos + `applyUpdate` en validaciones + presupuesto 5), `docs/risks-and-open-questions.md` (`S08`, `RK03`, `RK07` → 5 llamadas, `Q-rellenar-perfil-con-el-pinche-1..5`), `docs/technical-discovery.md` (contrato de 3 tools, tope 5, códigos de error, `save_profile` en «datos que salen del dispositivo», mermaid), `CONTEXT.md` (glosario «Mi perfil» y «Pinche»), `DESIGN.md` (fila Pinche con la burbuja «He guardado…»). `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md` **no tocados** (la spec los declara not needed y no cambian dominios, capas ni flujo de trabajo). `docs/specs/crear-mi-perfil.md` **sin reescribir**.
- **Docs sin contradicciones con el comportamiento**: RD26 ya no describe el gate previo; `DESIGN.md` ya no describe el mensaje de perfil como «sin respuesta de la IA»; `domain-model.md` y `technical-discovery.md` usan 5 llamadas; ningún doc conserva «dos funciones de solo lectura» como hecho actual (solo como historia: «eran 3 hasta `rellenar-perfil-con-el-pinche`»).
- **Harness**: `feature_list.json` modificado **solo** en `evidence` y `status` de esta entrada (diff verificado; `notes` y el resto de features intactos); cabecera de `PROGRESS.md` y sesión 121 del historial coherentes con la evidencia; JSON válido.
- **Handoff**: evidencia localizable en `docs/evidence/rellenar-perfil-con-el-pinche/` (informe + 3 salidas de gate + log de batería + 3 PNG); sin archivos temporales en el repo (untracked = solo los 3 esperados; `.harness/` vacío); sin procesos pendientes del implementador; rama sin commits, como exige el flujo.

## 8. Notas (sin cambio requerido)

- **N1**: no se conservó XML/log propio de la E2E focalizada (23/23 + 32/32); queda corroborada por la batería posterior con los mismos recuentos y sin fallos en esas clases.
- **N2**: el «`OK (1 test)`» del flake de `SettingsE2ETest` en aislamiento no tiene artefacto propio; el flake está documentado en las validaciones 115/116/119 y su test está fuera del diff.
- **N3**: `init.ps1` sale 1 **solo** si el invocador redirige el stderr de `adb monkey` (artefacto conocido); sin redirección → exit 0. Con un segundo AVD conectado, usar `ANDROID_SERIAL`.
- **N4**: el parser de `save_profile` ignora claves extra presentes en el JSON (el `additionalProperties: false` vive solo en el schema); sin impacto porque solo se persisten los 5 campos conocidos.
- **N5**: cada intento fallido con perfil pendiente añade un mensaje estático (los reintentos acumulan burbujas «Para responder necesito tu perfil…»); UX menor, la spec no lo prohíbe y la degradación S5 la exige.
- **N6**: la primera batería (abortada a 131/244 por la instalación de APK de otra sesión del workspace, `deletePackageX` sin `data_app_crash`) no se conservó como artefacto; queda documentada en el `informe.md`, en `PROGRESS.md` y en la sesión 121 con causa de entorno coherente con el patrón de las sesiones 118/119.

---

**Checks re-ejecutados (tier barato)**: `init.ps1` exit 0 · `lintDebug --rerun-tasks` 0 errores/48 warnings · `testDebugUnitTest --rerun-tasks` 45 suites/352 tests/0 fallos · `assembleDebug` + `compileDebugAndroidTestKotlin` BUILD SUCCESSFUL · paridad es/en 340/340 + 5/5 · inventario E2E 244 `@Test`/27 ficheros.

**Solo inspeccionados (tier caro)**: E2E focalizada 23/23 + regresión 32/32 (corroboradas por la batería), batería `am instrument` `numtests=244` / `Tests run: 244, Failures: 1` (único fallo `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures`, ajeno al diff; 1 omitido `Assume` sin `AI_API_KEY`) reconciliada 1:1 con el inventario por clase, y verify manual con 3 PNG 1280×2856 abiertos y revisados (mock local, sin llamadas de pago).
