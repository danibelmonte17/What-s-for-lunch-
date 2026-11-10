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

---

## §11 Enmienda: streaming y auto-scroll (2026-11-10)

- **Alcance de esta sección**: enmienda de la spec - decisiones **D9-D16**, escenarios **S8-S10**, tareas **T8-T11**, «Hallazgos de implementación de la enmienda» y «Checklist de la enmienda» - revisada además por coherencia con S1-S7 (aceptados en la sesión 122), non-goals, arquitectura, alcance, docs durables, seguridad y handoff.
- **Spec/contrato**: `docs/specs/rellenar-perfil-con-el-pinche.md` (enmendada 2026-11-10).
- **Change set revisado**: `git diff 6d9448b..HEAD` - `576c925` (feat) + `a8835fe` (docs), antes `7a7cdc4` (docs): 26 ficheros, +1416/-41. Fuentes: `data/ai/PincheChatClient.kt`, `OpenAiCompatiblePincheChatClient.kt`, `PincheChatProtocol.kt`, **nuevo** `PincheSseParser.kt`, `ui/pinche/PincheViewModel.kt`, `PincheScreen.kt`; tests (unit: `PincheSseParserTest.kt` nuevo, `PincheChatProtocolTest.kt`, `PincheViewModelTest.kt`, `ShoppingListFlowTest.kt`; androidTest: `PincheE2ETest.kt`, `MyProfileE2ETest.kt`, `ShoppingListE2ETest.kt`); docs/harness (`docs/specs/`, `docs/technical-discovery.md`, `DESIGN.md`, `PROGRESS.md`, `docs/progress-history.md`, `feature_list.json`, `docs/evidence/rellenar-perfil-con-el-pinche/`).
- **Validador**: `feature-validator` en sesión independiente; no participó en la implementación y solo escribe este informe (el acceptance lo persiste el orquestador).
- **Estado en `feature_list.json` al entrar**: `passing` (autoverificación de la enmienda; no es aceptación).

### VEREDICTO: accept

**0 Critical / 0 High / 0 Medium; 3 hallazgos Low (L1-L3) y 6 notas (N7-N12)**; ninguno bloquea la aceptación, por lo que no hay Implementation Repair Brief. El hueco del verify manual con mock SSE local (decisión del usuario 2026-11-10) se valora con ese marco y no como novedad: `readStream()` se juzgó **por inspección de código** (§11.4) y el fallback D12 está bien implementado; su falta de ejecución automatizada queda como L1 (recomendación no bloqueante).

### §11.1 Checks re-ejecutados (tier barato)

| # | Check | Comando | Resultado (recuento propio) |
|---|---|---|---|
| 1 | Arranque | `./init.ps1` (con `ANDROID_SERIAL=emulator-5554`) | **exit 0**, «Entorno listo» (`installDebug` BUILD SUCCESSFUL, `monkey` inyectado, app en primer plano) |
| 2 | Lint | `.\gradlew.bat lintDebug --rerun-tasks` | **BUILD SUCCESSFUL - 0 errores / 48 warnings** (desde `app/build/reports/lint-results-debug.xml`: 48 `Warning`, 0 `Error`/`Fatal`; ficheros con incidencias: `build.gradle.kts`, `AndroidManifest.xml`, `KeystoreAiKeyStore.kt`, `CameraXScanImageSource.kt`, mipmap, `strings.xml`, `libs.versions.toml` - **ningún fichero Kotlin del diff**, sin warnings nuevos sobre la línea base) |
| 3 | Unit | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL - 46 suites / 366 tests / 0 failures / 0 errors / 0 skipped** (suma propia de los atributos `tests=`/`failures=` de `app/build/test-results/testDebugUnitTest/*.xml`). Delta coherente con la evidencia: +1 suite `PincheSseParserTest` **7/0**, `PincheChatProtocolTest` 14 a **18/0** (+4 `streamingText`), `PincheViewModelTest` 22 a **25/0** (+3) |
| 4 | Build | `.\gradlew.bat assembleDebug compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** (`EXIT=0`) |
| 5 | Paridad es/en | recuento propio de `values/strings.xml` vs `values-en/strings.xml` | **340 `<string>` + 5 `<plurals>` por idioma, diff = 0** (sin claves nuevas; la enmienda no toca `strings.xml`) |
| 6 | Inventario E2E | recuento propio de `app/src/androidTest` | **245 `@Test`** en 25 ficheros con `@Test` (27 ficheros `.kt`, 2 helpers sin `@Test`) = «Starting 245» de la batería |

### §11.2 Tier caro: una focalizada re-ejecutada + evidencia inspeccionada

- **Re-ejecutada por el validador** (único check caro ejecutado): `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.PincheE2ETest"` -> **17/17, 0 omitidos, 0 fallos, BUILD SUCCESSFUL in 53s** (`emulator-5554`, `Pixel_10_Pro(AVD) - 17`, API 37; sin llamadas de pago: la clase usa `FakePincheChatClient`). Corrobora el S6 y la regresión de S1-S5 sobre el árbol actual de HEAD.
- **Batería completa (inspeccionada)** `docs/evidence/rellenar-perfil-con-el-pinche/e2e-bateria-enmienda-2026-11-10.log`, parseada por el validador: `Starting 245 tests` = inventario (244 + S6); `Tests 245/245 completed (2 skipped) (1 failed)`; **2 omitidos** = `SettingsE2ETest.savedKeyMarksConfiguredAndNeverRevealsTheKeyWhenReturning` + `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` (ambos `assumeFalse(buildKeyActive)`; ver N7); **1 fallo** = `ShoppingListE2ETest.twoRecipesAddUpAndTheBadgeCountsSources` (`ComposeNotIdleException`, 60 s); cierre `BUILD FAILED in 59m 7s` / `EXIT=1` (esperado por el flake). `PincheE2ETest` sin ninguna línea de fallo/omisión (17/17).
- **Reruns conservados**: `e2e-shoppinglist-single-rerun-enmienda-2026-11-10.xml` -> `tests="1" failures="0"` (el test flakeado, 70 s) y `e2e-shoppinglist-rerun-enmienda-2026-11-10.xml` -> `tests="27" failures="0"` + log con `Finished 27 tests` y `BUILD SUCCESSFUL in 1m 14s`.
- **Corrección temporal (sin deriva de código)**: fuentes finales anteriores a las 18:28 (local) < batería (log a 19:33; `BUILD FAILED in 59m 7s`) < reruns (20:04 y 20:23 locales; los `timestamp` internos de los XML están en UTC) < gates del implementador (logs 20:06) < commits `576c925`/`a8835fe` (21:39) < re-ejecuciones del validador (hoy). Batería y reruns corren sobre el mismo código que HEAD y que mis re-ejecuciones.
- **No re-ejecutado (por defecto del tier caro)**: la batería completa (59 min), la regresión focalizada 39/39 y `e2e.ps1`; la evidencia es exacta, consistente y el flake está aislado con rerun 1/1 + clase 27/27.
- **Pendiente documentado y aceptado**: el verify manual con mock SSE local no se ejecutó (decisión del usuario 2026-11-10: exige cambiar `AI_BASE_URL`/`AI_API_KEY` en `local.properties` - fichero con la API key real, `AGENTS.md §5` - y `usesCleartextTraffic` transitorio); por eso `readStream()` no está probada E2E por HTTP y se juzga por inspección (§11.4). Anotado en la spec, en `informe.md` y en `evidence` de `feature_list.json`.

### §11.3 Cobertura de los escenarios de la enmienda (S8-S10) y checklist

| Escenario | Veredicto | Evidencia |
|---|---|---|
| **S8** streaming visible sin JSON de la Propuesta | **pasa** | `stream: true` en `buildBody` (`OpenAiCompatiblePincheChatClient.kt:255`); emisión por fragmento con `streamingText` (`:267`-`:281`, con dedup de emisiones) y reset por ronda `onContent("")` (`:114`, D14); corte del preview en `PincheChatProtocol.streamingText` (`PincheChatProtocol.kt:307`, con `jsonStart` en `:317`) con **4 tests que afirman el comportamiento pedido**: texto plano íntegro, corte en el bloque `receta`, JSON completo y a medio formar, y `{...}` en prosa sin comillas que **no** se corta (`PincheChatProtocolTest.kt:256`-`:291`); burbuja que crece y se sustituye por el mensaje final: unit `streamingFragmentsUpdateTheTransientBubbleUntilTheFinalReply` (`PincheViewModelTest.kt:404`, asienta el contrato de emisiones y el fin de la burbuja con el resumen intacto) + E2E S6 re-ejecutada 17/17 (§11.2). El orden «He guardado...» antes de la respuesta (D2 intacto) lo siguen asentando los tests de la sesión 121, en verde en la batería y en mi re-ejecución. |
| **S9** corte del stream + respuesta sin SSE (D11/D12) | **pasa** (con L1) | Corte: unit `failedStreamKeepsThePartialTextAsAnAssistantBubble` (`PincheViewModelTest.kt:431`: texto parcial conservado como burbuja del asistente, **después** el mensaje estático de pendientes, aviso único `FAILED`, sin burbuja transitoria) y `theKeptPartialTextTravelsInTheRetryContext` (`:453`: el parcial viaja en el contexto del reintento). En el cliente: `IOException` con el coroutine activo -> `PincheReplyResult.Failed` (`OpenAiCompatiblePincheChatClient.kt:64`-`:72`) y con el coroutine cancelado -> `CancellationException` (`:69`). Fallback D12: `isEventStream()` (`:284`-`:285`) -> cuerpo completo procesado con el código previo (`:138`-`:142`); inspección detallada en §11.4. «Sin doble aviso ni mensajes vacíos»: un único campo `notice` en el estado y el streaming en blanco se traduce en `null` (`PincheViewModel.kt:234`), por lo que no se pinta nunca una burbuja vacía. |
| **S10** la lista baja siempre al último mensaje (D10) | **pasa** | `LaunchedEffect(uiState.messages.size, hasMessages)` -> `animateScrollToItem(lastIndex)` (`PincheScreen.kt:135`-`:138`) y `LaunchedEffect(streamingText)` -> `scrollToItem(messages.size)` instantáneo con cada fragmento (`:139`-`:144`); con la lista vacía no se hace nada (`:141`); el trigger RF-MOB6 queda intacto (`:127`-`:129`) y `BottomBarE2ETest` está en verde en la batería; la regresión focalizada 39/39 no conserva artefacto raw propio pero sus tres clases están verdes en la batería (38/39) + rerun de ShoppingList (27/27). El propio S6 ascierte la lista subida a índice 0 que vuelve abajo con el segundo fragmento y el último mensaje visible **sin arrastrar** el `LazyColumn`. El borde aceptado de D10 (con texto llegando, tocar la pestaña vuelve a bajar en el siguiente fragmento) está documentado en la spec y en `technical-discovery.md`. |

**Checklist de la enmienda - cumplido**:

- Sin dependencias nuevas: `app/build.gradle.kts` **no aparece en el diff** (sin MockWebServer ni librería SSE; el parser es código propio).
- Sin claves nuevas de `strings.xml` (paridad 340/340 + 5/5, diff 0) y sin cambios en `MAX_HTTP_CALLS` (`OpenAiCompatiblePincheChatClient.kt:315`, sigue en 5), en el contrato de tools (`toolsJson` fuera del diff), en RD05/RD17 (`docs/domain-model.md` fuera del diff) ni en Room (sin ficheros de dominio/DAO en el diff).
- El resto de superficies de IA sigue sin streaming: barrido propio de `"stream"`/`text/event-stream`/`onContent` en `app/src/main` - solo aparecen en el cliente del Pinche (los matches de `AiSuggestionsScreen.kt` son `SelectionContent`, ajenos).
- Evidencia de la enmienda presente (`lint-`/`unit-enmienda-2026-11-10.log`, log de batería, reruns, `informe.md`) y **sin llamadas de pago**: toda la E2E usa `FakePincheChatClient` y esta validación no ha llamado a DevExpert Inference.
- `feature_list.json` solo con `status`/`evidence` (diff inspeccionado línea a línea: +4 evidencias y `accepted` -> `passing` en esta entrada; `steps`/`verification`/`notes` intactos) y `PROGRESS.md` + sesión 124 del historial al día.

### §11.4 `readStream()` y fallback D12 por inspección (hueco documentado)

Como el verify manual con mock SSE local no se ejecutó (decisión del usuario, §11.2), el ramo HTTP se juzga por inspección:

- **Lectura SSE**: `readStream()` (`OpenAiCompatiblePincheChatClient.kt:267`-`:281`) lee `source.readUtf8Line()` hasta EOF; solo procesa líneas con prefijo `data:` (`DATA_PREFIX`, `:313`) y el parser recorta el payload (`payload.trim()` en `PincheSseParser.kt:32`), por lo que admite `data:` con y sin espacio; las líneas de comentario (`: keep-alive`) y `event:` se ignoran.
- **`[DONE]`**: `feed()` marca `finished` (`PincheSseParser.kt:34`-`:36`) y el bucle hace `break`; `buildMessage()` devuelve lo acumulado (con `content` y/o `tool_calls`).
- **JSON ilegible**: marca `invalid` (`:38`-`:42`) y `buildMessage()` devuelve `null` (`:66`) -> el cliente devuelve `failed("missing message")` (`:146`): turno fallido con el aviso de siempre (RD05) y, si había texto visible, el ViewModel lo conserva como burbuja (D11).
- **Reensamblado**: `tool_calls` por `index` con acumulación de `id`/`name`/`arguments` (`:47`-`:62`) y `message` con la forma exacta que consumen `PincheChatProtocol.toolCalls()`/`messageContent()` (`:65`-`:91`); **el bucle de rondas, `parseReply`, `executeSaveProfile` y `MAX_HTTP_CALLS = 5` no cambian** (esos tramos no aparecen en el diff salvo el reset por ronda y la rama de lectura).
- **`Content-Type` (D12)**: `isEventStream()` (`:284`-`:285`, `contains("text/event-stream")`, insensible a mayúsculas y a parámetros como `charset`) -> si no, `JSONObject(body.string())` -> `choices[0].message` (`:138`-`:142`), que es **literalmente el código previo a la enmienda**: el resultado es idéntico al flujo sin streaming, sin emisión de fragmentos (solo el reset inicial, que deja la burbuja oculta) y sin doble aviso ni mensajes vacíos (S9). Si el proveedor ignora `stream` y devuelve JSON, la app sigue funcionando; ese es el punto de D13.
- **Cancelación de la `Call`**: `currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }` (`:123`) registrado en el Job del `withContext` (se dispara también si se cancela `viewModelScope`) y dispuesto en el `finally` (`:156`) tras el `use {}`, de modo que no queda ninguna lectura colgada ni manejadores huérfanos; una `IOException` provocada por la cancelación se propaga como `CancellationException` (`:69`) y una `IOException` real (corte del stream) con el coroutine activo se convierte en `PincheReplyResult.Failed` (`:64`-`:72`).
- **Emisión**: solo cuando el texto visible cambia (dedup `:273`-`:277`), siempre recortado con `streamingText`, de modo que por la ruta de red nunca sale el JSON de la Propuesta.

Conclusión: **ambos ramos están bien implementados**. Cobertura: el parser (7 tests) y el corte (4 tests) están ejecutados en JVM y el ViewModel está testeado para emisión/parcial/reintento, pero **ningún test ejecuta el ramo HTTP** (`readStream` ni la selección por `Content-Type`) - ver L1.

### §11.5 Determinismo del S6

`PincheE2ETest.streamingAnswerGrowsInABubbleAndTheListFollowsTheLastMessage` (`PincheE2ETest.kt:739`-`:791`) usa **dos `CompletableDeferred`** que detienen el simulador antes del último fragmento y antes del resultado: la burbuja se aserta con el proveedor congelado (aparición con el primer fragmento, crecimiento con el segundo), el scroll a índice 0 se ejecuta antes de liberar el segundo fragmento, y la espera final combina `waitUntil` con el helper `waitUntilDisplayed()` (reintenta `assertIsDisplayed()` porque el `animateScrollToItem` sigue corriendo cuando llega el mensaje). El fake emite fragmentos **solo en la última tanda** (`:960`-`:972`), de modo que los escenarios S1-S5 previos no los reproducen; sin aserciones sobre tiempos ni sleeps. Re-ejecutado por el validador: 17/17 en verde (§11.2); también en verde en la batería.

### §11.6 Hallazgos (0 Critical / 0 High / 0 Medium; 3 Low)

**L1 - los ramos HTTP (`readStream` y la selección D12) no se ejecutan en ningún test** (Low)

- Evidence: `OpenAiCompatiblePincheChatClientTest.kt` solo prueba `executeSaveProfile` (sin red); barrido de `src/test` y `src/androidTest` sin ninguna referencia a `event-stream`/`readStream`/interceptores; el único plan de verificación de la enmienda que tocaba HTTP era el verify manual con mock, pospuesto por decisión del usuario.
- Why it matters: la mitad «respuesta sin SSE» de S9 y la lectura SSE real quedan cubiertas solo por inspección (§11.4) y por el camino de parseo previamente verificado a mano en la sesión 121; una regresión futura en `isEventStream()` o en el bucle de líneas rompería todas las respuestas del Pinche sin que ningún test la detecte.
- Required change (no bloqueante, recomendación al orquestador): añadir un test JVM con **interceptor OkHttp falso** (cliente con `addInterceptor` que devuelva una `Response` sintética con cuerpo `Buffer`: sin dependencias nuevas, sin secretos, sin emulador y sin tocar `local.properties`) que cubra (a) JSON con `Content-Type: application/json` -> resultado idéntico al flujo sin streaming y sin emisión de fragmentos (D12), (b) cuerpo SSE `data: ...` + `data: [DONE]` -> `onContent` con el texto visible y `message` reconstruido, y (c) `IOException` a mitad del stream -> `Failed` con el parcial ya emitido. Alternativa: repetir el verify manual con mock SSE cuando el usuario lo autorice (pendiente ya anotado en la spec, `informe.md` y `feature_list.json`).
- Verification after fix: `.\gradlew.bat testDebugUnitTest` en verde con los tests nuevos (366 + N).

**L2 - la redacción de la evidencia sobre D12 puede leerse como cobertura de tests** (Low)

- Evidence: `feature_list.json` (evidence de la enmienda) y `docs/evidence/rellenar-perfil-con-el-pinche/informe.md:88`: «la cubren los unit de `PincheSseParser`/`streamingText` y el fallback D12 (respuesta sin `text/event-stream` -> se procesa como hasta hoy)».
- Why it matters: en realidad los unit cubren el parser y el corte; el fallback D12 queda cubierto por implementación e inspección, no por ningún test. Una validación futura podría dar por ejecutado algo que no lo está.
- Required change: matizar la frase a «los unit cubren el parser y el corte; el fallback D12 queda cubierto por implementación e inspección del validador, pendiente de ejecución (L1)» en `informe.md` y en `evidence` de `feature_list.json`.

**L3 - los fakes de test no aplican `streamingText` como afirma el hallazgo de la spec** (Low)

- Evidence: la spec («Hallazgos de implementación de la enmienda») dice «los fakes de test acumulan los deltas y **recortan como el cliente real**», pero `PincheViewModelTest.kt:663` y `PincheE2ETest.kt:972` hacen `onContent(accumulated.trim())` sin llamar a `PincheChatProtocol.streamingText`.
- Why it matters: equivalente para los inputs probados (los fragmentos no llevan JSON), pero la afirmación sobreestima la fidelidad del fake; un test futuro que emita fragmentos con `{"` no se cortará como el cliente real.
- Required change: o bien aplicar `PincheChatProtocol.streamingText(accumulated)` en los dos fakes, o ajustar la frase del hallazgo a «acumulan y normalizan los espacios; el corte lo hace el cliente real».

### §11.7 Seguridad y privacidad (checklist acotada al diff)

**Sin hallazgos.**

1. **Secretos**: barrido propio sobre `git diff 6d9448b..HEAD` (`sk-...`, `AI_API_KEY=`, `Bearer`) = **0 coincidencias**; el único valor real `sk-...` del entorno está en `local.properties` (gitignored, verificado con `git check-ignore`) y en el `BuildConfig` generado bajo `app/build/` (ignorado por git): **no se ha tocado en esta validación**, no entra en el diff ni en la evidencia, y ningún fichero con secretos queda modificado (`git status` limpio tras mis re-ejecuciones).
2. **Privacidad/PII**: la enmienda no añade capturas (documentado); la evidencia existente usa solo datos ficticios («Frutos secos», «Diabetes», «Vegana»); el streaming es presentación y no cambia qué datos salen del dispositivo (RK03 intacta).
3. **Superficie externa**: sin permisos nuevos, sin dependencias, sin cambios de manifiesto ni de Gradle (fuera del diff), sin `usesCleartextTraffic`; ninguna llamada de pago a DevExpert Inference en toda la validación (E2E con fakes).
4. **Robustez del flujo de escritura**: `save_profile` sigue acotada y validada por `UserProfileRules` antes de persistir (sin cambios en el diff); los errores siguen sin persistir nada. El log del cliente sigue sin imprimir la clave ni el contenido de la conversación (solo código HTTP, duración, nº de rondas y tools).

### §11.8 Docs durables, harness y handoff

- **`Durable Documentation Impact` de la enmienda cumplido**: `docs/technical-discovery.md` (nota «Streaming (D9-D16)» en el contrato del Pinche, con fallback, D11 y auto-scroll; sección de verificación actualizada), `DESIGN.md` (fila del Pinche con la burbuja en streaming y el auto-scroll; fila «Carga» con el indicador «Pensando...» hasta el primer fragmento), spec enmendada (Goal/Non-Goals, S8-S10, research, Technical Approach 7-11, delta de Expected File Changes, T8-T11, planes de verificación/evidencia, checklist, D9-D16 y hallazgos, incluido el hueco del verify manual). `docs/domain-model.md`, `docs/build-brief.md` y `CONTEXT.md` **no tocados**: la enmienda los declara not needed y no contradicen el comportamiento (RD17/RD26/RD27 sin cambios por la enmienda). `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md` intactos.
- **Alcance**: todos los ficheros del diff están en «Expected File Changes» (delta de la enmienda) o son docs/harness permitidos; sin trabajo adyacente (barrido de streaming solo en el Pinche) y los cambios de los 5 fakes son solo de firma.
- **`feature_list.json`**: modificado solo en `evidence` (+4 líneas) y `status` (`accepted` -> `passing`) de esta entrada; el resto de campos y features intactos; JSON válido.
- **Handoff**: `PROGRESS.md` (cabecera con la enmienda, gates, E2E y hueco) y sesión 124 de `docs/progress-history.md` coherentes con la evidencia y con este informe; evidencia localizable en `docs/evidence/rellenar-perfil-con-el-pinche/`; sin procesos ni ficheros temporales dejados por el validador (los artefactos de mis re-ejecuciones viven en `app/build/`, ignorado).

---

**Checks re-ejecutados por este validador (tier barato)**: `init.ps1` exit 0 (con `ANDROID_SERIAL=emulator-5554`) · `lintDebug --rerun-tasks` 0 errores / 48 warnings · `testDebugUnitTest --rerun-tasks` 46 suites / 366 tests / 0 fallos · `assembleDebug` + `compileDebugAndroidTestKotlin` BUILD SUCCESSFUL · paridad es/en 340/340 + 5/5 · inventario E2E 245 `@Test`.

**Re-ejecutada (tier caro, focalizada)**: `connectedDebugAndroidTest` clase `com.whatsforlunch.app.PincheE2ETest` -> **17/17, 0 omitidos, 0 fallos** (emulator-5554, Pixel_10_Pro API 37, sin llamadas de pago).

**Solo inspeccionadas**: batería `e2e-bateria-enmienda-2026-11-10.log` (245 arrancados = inventario, 2 omitidos `Assume`, 1 fallo flake ajeno con rerun aislado 1/1 y clase 27/27), regresión focalizada 39/39 (corroborada por la batería + rerun), gates del implementador (logs de la evidencia) y el verify manual con mock SSE local (pendiente documentado, decisión del usuario 2026-11-10).

**Estado recomendado**: `rellenar-perfil-con-el-pinche` -> `accepted` (orquestador); el validador no ha modificado `feature_list.json` ni `PROGRESS.md`.