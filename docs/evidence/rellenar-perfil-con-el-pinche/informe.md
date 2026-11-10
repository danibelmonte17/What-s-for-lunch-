# Informe de autoverificación — `rellenar-perfil-con-el-pinche`

- **Fecha**: 2026-11-09/10 (implementación y verificación; `feature-implementer`).
- **Spec**: `docs/specs/rellenar-perfil-con-el-pinche.md` (S1–S7, D1–D8, T1–T7).
- **Entorno**: AVD `Pixel_10_Pro` API 37 (`emulator-5554`) para la focalizada, AVD dedicado `Pixel_10_Pro_MoverOpciones` API 37 (`emulator-5556`) para la batería y `Pixel_9_Pro` API 37 (`emulator-5558`) para el verify manual. Host Windows.
- **Sin llamadas de pago**: toda la verificación usa cliente simulado; las capturas usan un mock local en `127.0.0.1:8765` (`10.0.2.2` desde el emulador) con clave sintética `sk-test-*`. No se ha llamado nunca a DevExpert Inference.

## Nota de entorno (por qué la batería va con `am instrument`)

Otra sesión del mismo workspace ejecutaba `connectedDebugAndroidTest` en cadena contra **todos** los emuladores conectados (sus instalaciones de APK con el mismo `applicationId` matan la instrumentación ajena con `deletePackageX`). La primera batería sobre `emulator-5554` quedó abortada a los **131/244** por esa colisión (log con `Instrumentation run failed due to to Process crashed` y sin `data_app_crash` en dropbox: la app no falló). La batería válida se ejecutó en un **AVD dedicado vía `am instrument`** (mismo patrón que la sesión 119), donde ninguna instalación ajena entró.

## Gates baratos (código final limpio, `--rerun-tasks`)

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` | exit 0 «Entorno listo» (ver cierre de este informe) |
| Estático | `./gradlew lintDebug --rerun-tasks` | **0 errores / 48 warnings** (línea base 48; ninguno apunta al diff) — `gates-lint.txt` |
| Unit | `./gradlew testDebugUnitTest --rerun-tasks` | **45 suites / 352 tests / 0 fallos / 0 errores / 0 omitidos** (línea base 44/327 → +1 suite y +25 tests) — `gates-unit.txt` |
| Build | `./gradlew assembleDebug` + `compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** — `gates-assemble.txt` |
| Paridad es/en | recuento de claves de `values`/`values-en` | **340/340 strings + 5/5 plurals, diff = 0** (línea base 338+5 → `pinche_profile_saved` y `pinche_profile_saved_none`) |

Recuento propio desde los XML (corregido tras la validación, F1): `UserProfileRulesTest` (19), `PincheChatProtocolTest` (14), `PincheViewModelTest` (22), `PromptAssetsTest` (9), `OpenAiCompatiblePincheChatClientTest` (4, nueva).

## E2E focalizada (cliente simulado, sin red)

| Suite | Resultado |
|---|---|
| `PincheE2ETest` + `MyProfileE2ETest` (verify 4 enmendado) | **23/23** (`emulator-5554`) |
| Regresión `ShoppingListE2ETest` + `BottomBarE2ETest` | **32/32** (`emulator-5554`) |

## Batería completa (verify manual, `am instrument`, `emulator-5556` API 37)

Log íntegro: `e2e-bateria-am-instrument.txt`.

- `numtests=244` = **inventario `@Test` (27 ficheros / 244 `@Test`)**; `Tests run: 244, Failures: 1`.
- **1 fallo** = `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` (`ComposeTimeoutException` 5 s), **flake ajeno** documentado en las sesiones 115/116/119 y **fuera del diff**; **re-ejecutado en aislamiento: `OK (1 test)`**.
- **1 omitido** = `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (`Assume` sin `AI_API_KEY` en el build; línea base conocida).
- Desglose por clase (recuento propio del log): `PincheE2ETest 16/16`, `MyProfileE2ETest 7/7`, `ShoppingListE2ETest 27/27`, `BottomBarE2ETest 5/5`, `PantryManagementE2ETest 9/9`, `AiSuggestions 16`, `CreateRecipe 23+1`, `ScanPantry 25`, `RecipeDetail 12`, `Settings 3 ok + 2 no-verdes (arriba)` y el resto sin incidencias.

## Verify manual con capturas (mock local, es-ES, 1280×2856)

Flujo real de la app (sin sustituir nada: el cliente de producción ejecuta `save_profile` contra el mock): perfil vacío → «Dame una receta» → pregunta por Alergias → «Frutos secos» → **resumen «He guardado en «Mi perfil»: Alergias: Frutos secos»** + siguiente pregunta → mock parado → fallo visible + **mensaje estático con «Abrir Mi perfil»** → mock de nuevo → «Diabetes» y «Vegana» → perfil completo → pestaña «Mi perfil» con las chips guardadas.

| Captura | Contenido |
|---|---|
| `01-entrevista-resumen-es.png` | Entrevista: pregunta del Pinche + resumen «He guardado…» con lo persistido (S1) |
| `02-mi-perfil-guardado-es.png` | «Mi perfil» con las chips «Frutos secos» y «Diabetes» guardadas desde el chat |
| `03-degradacion-abrir-mi-perfil-es.png` | Aviso «El Pinche no pudo responder» + mensaje de partes pendientes con «Abrir Mi perfil» (S5) |

Las tres PNG tienen cabecera válida y tamaño **1280×2856**. El resumen del chat coincide con lo que muestra «Mi perfil» (mismos valores ficticios). Build de capturas **transitorio** (revertido después): `AI_BASE_URL=http://10.0.2.2:8765/v1` + clave sintética en `local.properties` y `android:usesCleartextTraffic="true"` en el manifiesto (el mock es HTTP local); ambos retirados y el manifiesto restaurado con `git checkout` antes de los gates finales.

## Cobertura de `verification` de `feature_list.json`

1. **E2E entrevista de una en una + resumen + Ficha al completar** → `PincheE2ETest.profileInterviewSavesOneAnswerAtATimeAndProposesOnceComplete` (S1, en verde) + captura 01 + verificación en Room de «Mi perfil» dentro del propio test.
2. **E2E perfil a medio completar** → `PincheE2ETest.halfCompleteProfileKeepsWhatWasSavedAndOnlyAddsTheNewAnswer` (S3): solo se pide lo pendiente, el resumen no repite lo guardado y Alergias no se pierde.
3. **E2E sin key/fallo → mensaje + «Abrir Mi perfil»** → `PincheE2ETest.missingApiKeyWithPendingProfileShowsNoticeAndStaticRequest` (S5), `MyProfileE2ETest.emptyProfileShowsTheAiNoticeAndTheActionOpensItsTab` (verify 4 enmendado con cliente simulado en Koin, sin red) + captura 03.
4. **Unit `save_profile` + `pendingParts`** → `UserProfileRulesTest` (chips vacía/duplicada/exclusividad/tipo desconocido vía parseo, `applyUpdate` y completitud), `OpenAiCompatiblePincheChatClientTest` (executor con repo InMemory: persiste, caché por turno y errores sin persistir), `PincheChatProtocolTest` (schema/parseo/resultados) y `PincheViewModelTest` (guarda, resumen y degradación).

## Seguridad / privacidad

- Sin secretos ni PII: clave usada solo como valor sintético `sk-test-*` en `local.properties` (gitignored, restaurado); ningún dato personal real («Frutos secos»/«Diabetes»/«Vegana» son ficticios).
- Sin permisos nuevos, sin dependencias nuevas, **Room sin cambios de esquema**.
- `RK03`/`S08`/`RK07` actualizados; sin llamadas de pago (mock local).
- Alcance: solo los ficheros de «Expected File Changes» de la spec (+ el fichero de test del executor y los hallazgos anotados en la spec).

## Enmienda (2026-11-10): respuesta en streaming y auto-scroll

**Alcance**: D9–D16 de la spec — `stream: true` + lectura SSE con `PincheSseParser`, texto visible cortado con `streamingText`, parcial conservado en fallo, burbuja transitoria y lista que baja siempre al último mensaje. Sin dependencias nuevas, sin claves nuevas de `strings.xml` (paridad **340/340 + 5/5**, diff 0), sin cambios en `MAX_HTTP_CALLS`, tools, Room ni en el resto de superficies de IA.

**Gates (sobre el change set completo)**

| Check | Resultado |
|---|---|
| `lintDebug --rerun-tasks` | **0 errores / 48 warnings** (línea base; ninguno nuevo) — `lint-enmienda-2026-11-10.log` |
| `testDebugUnitTest --rerun-tasks` | **46 suites / 366 tests / 0 fallos** (+1 suite `PincheSseParserTest`, +14 tests) — `unit-enmienda-2026-11-10.log` |
| `assembleDebug` + `compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| Paridad es/en | 345 = 340 strings + 5 plurales en cada idioma, 0 diferencias |
| `init.ps1` | exit 0 («Entorno listo») |

**E2E**

- **S6 nuevo** `PincheE2ETest.streamingAnswerGrowsInABubbleAndTheListFollowsTheLastMessage` (clase **17/17**): la burbuja `pinche_streaming_message` muestra el primer fragmento, crece con el segundo (gates `CompletableDeferred`, sin carreras), la lista vuelve abajo aunque el test la suba a índice 0 (D10) y al terminar la burbuja desaparece y el último mensaje queda **visible sin arrastrar** el `LazyColumn`.
- **Regresión focalizada**: `MyProfileE2ETest` + `ShoppingListE2ETest` + `BottomBarE2ETest` **39/39**.
- **Batería completa** (`./e2e.ps1`, log `e2e-bateria-enmienda-2026-11-10.log`): **245 tests = inventario** (244 + S6), **2 omitidos** (`SettingsE2ETest`/`Assume`) y **1 fallo flake ajeno** `ShoppingListE2ETest.twoRecipesAddUpAndTheBadgeCountsSources` (`ComposeNotIdleException`, 60 s, en una batería lenta de 59 min). Rerun aislado **1/1** (`e2e-shoppinglist-single-rerun-enmienda-2026-11-10.xml`) y clase completa **27/27 verde** (`e2e-shoppinglist-rerun-enmienda-2026-11-10.log/.xml`) tras reiniciar el AVD, que se cayó durante el primer rerun (historial conocido). El XML de la batería se sobrescribió con los reruns: el registro íntegro está en el log.

**Hueco abierto (decisión del usuario, 2026-11-10)**

- **Verify manual con mock SSE local no ejecutado**: exige cambiar `AI_BASE_URL`/`AI_API_KEY` en `local.properties` (fichero con la API key real; `AGENTS.md §5` prohíbe tocar ficheros con secretos sin autorización). **Consecuencia**: `readStream()` —la lectura SSE real por HTTP— **no está probada de extremo a extremo**; la cubren los unit de `PincheSseParser`/`streamingText` y el fallback D12 (respuesta sin `text/event-stream` → se procesa como hasta hoy). Sin capturas nuevas de la enmienda (las 3 PNG existentes son de la implementación original). Pendiente de repetir cuando el usuario autorice el mock local.
