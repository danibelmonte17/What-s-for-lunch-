# Validación independiente - `crear-mi-perfil`

- **Feature**: `crear-mi-perfil` - Crear Mi perfil con restricciones dietéticas
- **Spec**: `docs/specs/crear-mi-perfil.md` (S1-S6, verify 1-5, D1-D8, T1-T7)
- **Validador**: `feature-validator` (subagente en sesión independiente; no participó en la implementación y solo escribe este informe: el acceptance lo persiste el orquestador)
- **Fecha**: 2026-11-09 · **Rama**: `feature/crear-mi-perfil` · **HEAD**: `a290aa1` · change set **sin commitear**
- **Estado en `feature_list.json` al entrar**: `passing` (autoverificación, no aceptación)

## VEREDICTO: accept

Sin hallazgos Critical/High/Medium. 1 hallazgo Low documental (D1) y 5 notas (N1-N5) sin cambio requerido, detallados más abajo. No hay Implementation Repair Brief.

## 1. Objetivo de la revisión

- **Spec/contrato**: `docs/specs/crear-mi-perfil.md` - Goal, 6 non-goals, S1-S6, `verification` 1-5 de `feature_list.json`, `Expected File Changes`, T1-T7 y `Verification Plan`.
- **Diff revisado**: 29 ficheros modificados + 15 rutas sin seguimiento (fuentes, tests, 2 assets de prompt, spec y carpeta de evidencia); `git diff HEAD --stat` = 791 inserciones / 46 borrados.
- **Evidencia del implementador**: `docs/evidence/crear-mi-perfil/` (`informe.md`, `lint-debug.txt`, `unit-tests.txt`, `e2e-bateria-am-instrument.txt`, 3 PNG) + los 5 items de `evidence` en `feature_list.json`.

## 2. Checks re-ejecutados por el validador (tier barato)

| # | Check | Comando | Resultado (recuento propio) |
|---|---|---|---|
| 1 | Arranque | `./init.ps1` (con `ANDROID_SERIAL=emulator-5554`) | **exit 0**: «Entorno listo», `installDebug` BUILD SUCCESSFUL, `monkey` inyectado, `pm path` + `pidof` OK |
| 2 | Lint | `.\gradlew.bat lintDebug --rerun-tasks` | **BUILD SUCCESSFUL - 0 errores / 48 warnings** (recuento desde `app/build/reports/lint-results-debug.txt`: 48 incidencias, 0 Error/Fatal) |
| 3 | Unit | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL - 44 suites / 327 tests / 0 failures / 0 errors / 0 skipped** (recuento propio desde `app/build/test-results/testDebugUnitTest/*.xml`) |
| 4 | Build | `.\gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** |
| 5 | Test APK | `.\gradlew.bat compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** (tareas UP-TO-DATE: insumos idénticos a la última compilación, el árbol actual compila) |
| 6 | Paridad es/en | recuento propio de `values/strings.xml` vs `values-en/strings.xml` | **338/338 claves + 5/5 plurales**, sin claves solo en un locale (diff vacío en ambos sentidos) |
| 7 | Inventario E2E | `git grep -c "@Test" HEAD` vs recuento del árbol | HEAD **231 `@Test` / 24 ficheros** -> árbol **240 `@Test` / 27 ficheros** (+9: `MyProfileE2ETest` 7, `PincheE2ETest` +1, `RecipeMigrationTest` +1) |

Detalles de correlación:

- **`init.ps1` - nota de entorno (no es el falso negativo histórico)**: con **dos AVDs conectados** (`emulator-5554` Pixel_10_Pro API 37 y `emulator-5558` Pixel_8_Pro) el script aborta en `adb get-state` con el error `more than one device/emulator` (verificado por el validador: `adb get-state` -> exit 1; `adb -s emulator-5554 get-state` -> `device`). Re-ejecutado fijando `ANDROID_SERIAL=emulator-5554` -> exit 0 completo. No hizo falta invocar el falso negativo conocido de `monkey`/`pidof`.
- **Lint sin deriva respecto de la evidencia**: las 48 incidencias de mi `--rerun-tasks` forman un **conjunto idéntico** al de `docs/evidence/crear-mi-perfil/lint-debug.txt` (0 solo en evidencia, 0 solo en mi run), luego **ningún warning nuevo del diff**. Las 19 incidencias que caen en `values/strings.xml` / `values-en/strings.xml` (Typos «momento», PluralsCandidate, UnusedResources `scanner_*`, TypographyEllipsis) están en líneas preexistentes: los hunos del diff en esos ficheros son `@@ -215,7 +215,7 @@` (solo `settings_privacy_notice`) y `@@ -356,4 +356,24 @@` (claves nuevas al final) y ninguna incidencia apunta a esas líneas. Línea base 48-49 según `PROGRESS.md`; aquí 48.
- **Unit reproducido**: suites de la feature con recuentos exactos: `UserProfileRulesTest` 11/0, `MyProfileViewModelTest` 6/0, `PincheViewModelTest` 17/0, `PromptAssetsTest` 6/0 (coincide con `unit-tests.txt`).

## 3. Checks solo inspeccionados (tier caro) y su evidencia

No se re-ejecutó ningún check caro: la evidencia es exacta, internamente consistente, coherente con el diff y **posterior a toda fuente del change set** (última fuente modificada: `PantryManagementE2ETest.kt` 09-11 14:34; lint 12:50 y unit 14:10 reproducidos arriba; batería 15:18; capturas 15:38-15:48; `informe.md` 15:51).

1. **E2E focalizada: 27/27, 0 fallos, 0 omitidos** (verify 1-5).
   - Evidencia: `docs/evidence/crear-mi-perfil/informe.md` §4 - `connectedDebugAndroidTest` sobre `Pixel_10_Pro` API 37, desglose `MyProfileE2ETest` 7/7, `BottomBarE2ETest` 5/5, `PincheE2ETest` 12/12, `RecipeMigrationTest` 3/3.
   - **Corroboración independiente del validador**: en la batería posterior (punto 2) esas mismas 4 clases arrancan con exactamente esos recuentos (7+5+12+3 = 27) y **ninguna falla** (el único fallo de la run es de `SettingsE2ETest`).
   - Salvedad documentada: **no se conservó un XML/log propio de la focalizada** (N1, Low).
2. **Batería completa: `numtests=240`, 1 fallo, 1 omitido.**
   - Evidencia: `docs/evidence/crear-mi-perfil/e2e-bateria-am-instrument.txt` (`am instrument -w -r com.whatsforlunch.app.test/androidx.test.runner.AndroidJUnitRunner`, AVD **`emulator-5556`** Pixel_9_Pro API 37, app en español, 09-11 15:18, `Time: 1,316.632`).
   - **Recuento propio del validador parseando el log**: bloques de inicio `STATUS_CODE: 1` = **240** (igual que `numtests` y que el inventario 240 `@Test`); reparto por clase = 29 clases con tests, entre ellas `MyProfileE2ETest` 7, `BottomBarE2ETest` 5, `PincheE2ETest` 12, `RecipeMigrationTest` 3, `ShoppingListE2ETest` 27, `ScanPantryE2ETest` 25; **1 código -2 (fallo)** y **1 código -4 (Assume/skip)**; cierre `Tests run: 240, Failures: 1`.
   - Fallo único: `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` (`ComposeTimeoutException` de 5 s) - **fuera de este diff** (`SettingsE2ETest.kt` no está modificado), flake ya registrado en las validaciones 115/116, informado como `OK (1 test)` en aislamiento tras el run.
   - Omitido único: `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` con `AssumptionViolatedException` en `assumeTrue` (sin `AI_API_KEY` en `local.properties`) - línea base conocida.
3. **Verificación manual adb/uiautomator (es-ES, 1280×2856) + 3 capturas.**
   - PNG inspeccionados **visualmente** por el validador: legítimos, sin PII ni secretos, datos ficticios.
   - `01-perfil-con-chips-modo-edicion.png`: chip «Frutos secos» con icono de borrado y botón en «Listo» (sección en modo edición) mientras Intolerancias muestra «Editar» (sin borrado) -> verify 1 con borrado por sección; barra inferior con **5 pestañas en orden** (Mis Recetas, Despensa, Lista de la Compra, Pinche, **Mi perfil** última y activa) -> D4.
   - `02-tipo-alimentacion-vegana.png`: «Vegana» como **única** marcada (verde) entre las 5 opciones -> verify 2 / D2.
   - `03-pinche-pide-perfil.png`: el Pinche pide **solo** «Intolerancias o enfermedades que limitan la dieta» con el botón «Abrir Mi perfil» y **sin respuesta de la IA** -> verify 4 / RD26 / D8.
   - Los puntos manuales sin captura (la acción navega a la pestaña, re-toque con scroll al inicio, persistencia tras `force-stop`) están cubiertos por E2E (`MyProfileE2ETest.pincheAsksForTheEmptyProfileAndTheActionOpensItsTab`, `MyProfileE2ETest.profilePersistsInRoomAfterReopeningTheScreen`, `BottomBarE2ETest`) y por el `informe.md` §6.

## 4. Validación contra la spec

- **Goal**: cumplido: pestaña «Mi perfil» con Alergias e Intolerancias como chips (alta por texto + «Añadir», borrado solo en modo edición), «No tengo ninguna» exclusiva por sección, «Tipo de alimentación» de selección única sin preselección, persistencia Room v6 y Pinche que bloquea con perfil incompleto y consume el perfil en el prompt.
- **Non-goals respetados**: el perfil solo lo consumen `ui/myprofile/*`, `ui/pinche/*`, DI y persistencia (búsqueda de `UserProfile|DietType|PendingProfilePart` en `app/src/main` devuelve 17 ficheros, todos de esta feature más `di/AppModule.kt` y los de `data/`); Sugerencias, Recetas con mi Despensa, búsquedas, Escáner y Coincidencias intactos. **Sin tool `get_profile`** (`PincheChatProtocol.kt` sin cambios, `pinche_system_*` intactos). Sin permisos nuevos (manifest sin cambios: CAMERA/INTERNET/RECORD_AUDIO preexistentes). **Sin dependencias nuevas** (sin diff en `app/build.gradle.kts` ni `gradle/libs.versions.toml`). Conversación del Pinche sigue solo en memoria. `RK03` ampliada pero **no cerrada** (sigue como puerta de release). Aviso de privacidad solo extendido.
- **`verification` 1-5 de `feature_list.json`**:
  1. chips + borrado en modo edición -> `MyProfileE2ETest.chipsAreAddedAndDeletedOnlyInEditMode` (7/7 en evidencia y en batería) + captura 01.
  2. «Vegana»->«Omnívora» con persistencia -> `MyProfileE2ETest.dietTypeIsSingleSelectionAndPersisted` + captura 02; el reflejo en el prompt/propuesta se cubre por la cadena `PromptAssetsTest.profilePromptFillsDietAndRestrictions` (el bloque renderiza «Vegana»/«Omnívora»), `PincheViewModelTest.completeProfileIsSentToTheClient` (el perfil viaja al puerto) y `PincheE2ETest.emptyProfileAsks...` (dieta capturada en el cliente). Ver nota N5.
  3. «No tengo ninguna» -> `MyProfileE2ETest.noneOptionClearsTheSectionAndBlocksAdding` + unidades `UserProfileRulesTest.eachRestrictionSectionCompletesWithAChipOrWithNone`, `profileIsCompleteOnlyWhenNoPartIsPending` y `PincheViewModelTest.onlyMissingPartsAreRequested`.
  4. flujo bloqueante con perfil vacío -> `MyProfileE2ETest.pincheAsksForTheEmptyProfileAndTheActionOpensItsTab`, `PincheE2ETest.emptyProfileAsksForItWithoutCallingTheAiAndAnswersOnceComplete` (aserción `client.calls == 0` y luego 1) y unidades `emptyProfileBlocksTheClientAndAsksForEveryPendingPart`, `profileRequestMessagesNeverTravelToTheModel`.
  5. unit: prompt con perfil y gating solo con vacío -> `PromptAssetsTest` (6/6) y `PincheViewModelTest` (17/17).
- **`Expected File Changes` vs diff real**: están todos los ficheros previstos (dominio, Room/DAO/migración/repositorios, `ui/myprofile/`, navegación, Pinche, prompts, strings, tests). Desviaciones **justificadas**: `ShoppingListE2ETest`, `ShoppingListFlowTest` y `TestPromptSource` (adaptación de los fakes a la firma `reply(turns, language, profile)`, prevista en la spec); `PantryManagementE2ETest` y `BottomBarE2ETest` (helper `localized()` al idioma activo de la app y reparación del resto del merge `9dacc87`: «Volver» retirado en la Despensa; solo test, documentado en «Hallazgos de implementación» de la spec y en `evidence`); `PROGRESS.md`, `docs/progress-history.md` y `feature_list.json` (estado del harness). **Sin deriva de producto.** La spec preveía tocar `PinchePromptsTest` y en su vez se tocaron `PromptAssetsTest` y `TestPromptSource` (la interfaz `PromptSource` ganó `pincheProfilePrompt`); `PinchePromptsTest` pasa 3/3 sin cambios.
- **Tareas T1-T7**: todas marcadas `[x]` y verificadas una a una contra el código y los tests.
- **`Verification Plan`**: seguido (gates, paridad, migración v5→v6 en verde, E2E focalizada, batería, manual adb, solo datos ficticios y cliente IA simulado, sin llamadas de pago).

## 5. Hallazgos por severidad

### Hallazgos que impiden la aceptación: ninguno (0 Critical / 0 High / 0 Medium)

### D1: `docs/technical-discovery.md` no incluye el perfil en «Datos que salen del dispositivo» del Pinche

- **Severity**: Low
- **Evidence**: `docs/technical-discovery.md:121` sigue diciendo «**Datos que salen del dispositivo**: el hilo de la conversación y, cuando el modelo invoca las funciones, la Despensa y las Recetas descritas (`RK03`)...», sin mencionar las restricciones dietéticas y el tipo de alimentación. La spec lo pedía en `Durable Documentation Impact`: «`docs/technical-discovery.md` - update: Room v6 y tablas nuevas, **datos del perfil en el system prompt y en los datos que salen del dispositivo**, assets de prompt».
- **Why it matters**: es el inventario canónico de qué sale del dispositivo; omitir los datos de salud deja una lista incompleta para futuras sesiones y auditorías. La información **sí** existe en `docs/risks-and-open-questions.md` (RK03 ampliada), `CONTEXT.md` (glosario «Mi perfil»), `docs/domain-model.md` (RD26) y en el aviso de privacidad de la app, por eso no sube de Low.
- **Required change**: añadir una cláusula a ese bullet (o a la línea nueva «Perfil en el system prompt») indicando que el system prompt envía además las restricciones dietéticas y el tipo de alimentación (datos de salud, `RK03`).
- **Suggested implementation**:
  1. Editar `docs/technical-discovery.md:121` para que diga: «...la Despensa y las Recetas descritas, y las restricciones dietéticas y el tipo de alimentación del perfil (`RK03`)...».
  2. No tocar ningún fichero de código.
- **Verification after fix**:
  - `Select-String -Path docs\technical-discovery.md -Pattern "Datos que salen"` muestra el perfil citado en el bullet del Pinche.
  - Ningún gate de código afectado.

### Notas (Low, sin cambio requerido)

- **N1**: la E2E focalizada 27/27 no conserva XML/log propio en `docs/evidence/crear-mi-perfil/` (la spec pedía «XML/resultado de la focalizada»). Queda corroborada por la batería posterior, que arranca exactamente esos 27 tests y solo falla `SettingsE2ETest`. Sugerencia futura: copiar el XML de `app/build/outputs/androidTest-results/connected/` tras la focalizada.
- **N2**: la evidencia dice «27 ficheros / 31 clases»: el validador confirma **27 ficheros** con `@Test` y **240 `@Test`**; hay 31 declaraciones de clase en `androidTest` pero **29 clases con tests** (las otras 2 son helpers). Las cifras de ejecución (240) cuadran; solo la palabra «clases» es ambigua.
- **N3**: `app/build/outputs/androidTest-results/connected/` conserva un XML del 09-11 13:37 de una run aislada de `AiSuggestionsE2ETest` con 1 fallo; es un artefacto de `build/` **anterior** a la batería (15:18), donde `AiSuggestionsE2ETest` pasa 16/16. Sin impacto.
- **N4**: con dos AVDs conectados, `init.ps1` exige dispositivo único (`adb get-state` falla con `more than one device/emulator`); se resuelve con `ANDROID_SERIAL`. No es el falso negativo histórico de `monkey`/`pidof`. Conviene documentarlo en `docs/technical-discovery.md` si el entorno multi-AVD se vuelve habitual.
- **N5**: la verify 2 («el Pinche propone recetas veganas... la propuesta usa el nuevo tipo») no se afirma con un E2E que compare propuestas (el fake no evalúa la dieta y las llamadas de pago están prohibidas); se cubre por la cadena mecanismo unit + E2E descrita en §4, según el propio `Verification Plan` de la spec («verify 5 ... verificado con el fake que captura el perfil»).

**Implementation Repair Brief**: no aplica (veredicto `accept`).

## 6. Resumen de seguridad (checklist acotada al diff)

**Sin hallazgos relevantes; nada bloqueante.**

1. **Secretos/config**: ningún `sk-...`, token, credencial ni `AI_API_KEY=` en el diff, en la evidencia (`*.txt`/`*.md`) ni en las 3 capturas; `AI_API_KEY` solo aparece como referencia a `BuildConfig`/docs preexistentes. Sin cambios en `.env*` ni `local.properties`.
2. **Auth/authz**: sin cuentas ni roles (MVP de usuario único, `P31`); gestión de la API key y `KeystoreAiKeyStore` intactos.
3. **Validación de entrada**: chips validadas en el dominio (`UserProfileRules.normalizeChip`/`isDuplicate`: trim, no vacío, no duplicado) antes de persistir; Room 100 % parametrizado (`@Query("... WHERE id = :id")`, `@Insert`, sin SQL concatenado); el texto libre viaja literal al prompt sin interpretación.
4. **Exposición de datos/privacidad (RK03)**: los datos de salud salen **solo** en el system prompt al chatear con el Pinche; `settings_privacy_notice` es/en ampliado («datos de salud ... se envían al proveedor de IA cuando chateas con el Pinche»); `RK03` ampliada y **sin cerrar**; el mensaje de petición del perfil **nunca viaja al modelo** (texto en blanco; unit `profileRequestMessagesNeverTravelToTheModel`); el logcat del cliente solo registra endpoint, códigos HTTP y duraciones (sin prompts ni conversación); evidencia **solo con datos ficticios** («Frutos secos», «Diabetes», «Vegana»).
5. **Servicios externos/SSRF**: sin URLs ni endpoints nuevos; el cliente reutiliza `AiEndpoints.chatCompletions` y `aiKeyProvider` sin cambios; el gating se verificó sin llamadas de pago.
6. **Dependencias/tooling**: sin diff en `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
7. **Server/client**: N/A (app Android local, sin backend nuevo).
8. **Subidas/media**: N/A (sin uploads ni ficheros nuevos de usuario).
9. **Auditoría/abuso**: N/A (cambios locales, sin endpoints públicos); sin permisos nuevos en el manifest (CAMERA/INTERNET/RECORD_AUDIO preexistentes).

## 7. Docs durables y harness

- **`Durable Documentation Impact` de la spec**: `docs/domain-model.md` OK (RD25, RD26, bloque de implementación, obligatoriedad de chip y 6 casos límite nuevos); `docs/technical-discovery.md` parcial (Room v6 + migración `5->6`, bloque `pinche_profile_*`, assets: falta la cláusula de D1); `docs/risks-and-open-questions.md` OK (RK03 ampliada, `Q-crear-mi-perfil-1..5` resueltas); `DESIGN.md` OK (fila «Mi perfil», barra de 5 pestañas, mensaje de petición en Pinche, aviso de privacidad en Ajustes); `CONTEXT.md` OK (glosario «Mi perfil» con RD25/RD26 y `RK03`). `AGENTS.md`, `ARCHITECTURE.md` y `CONSTRAINTS.md` **sin tocar, conforme a la spec** («not needed»). Sin docs innecesarios creados.
- **UI conforme a `DESIGN.md`**: tarjetas por sección sobre `Surface` con tokens `Warm*` (patrón de Ajustes), chips píldora con borrado terracota en modo edición, `FilterChip` de selección única, top bar y barra inferior con 5 pestañas y test tags `bottom_bar_tab_*`.
- **Estado del harness**: `feature_list.json` modificado **solo** en `evidence` y `status` de esta feature (diff revisado línea a línea); `PROGRESS.md` refleja `passing` y el siguiente paso (validación); `docs/progress-history.md` solo añade la entrada de la sesión 118. Nada fuera de alcance en los ficheros de estado.
- **Higiene de esta validación**: solo se escribió este informe; no se tocaron `feature_list.json`, `PROGRESS.md` ni `docs/progress-history.md`; no hay commits; los temporales `.harness/validator-*.txt` creados para los gates fueron eliminados (los informes de lint y unit siguen en `app/build/reports/` y `app/build/test-results/`). Queda la app instalada y en ejecución en `emulator-5554` (arranque del propio `init.ps1`); no se inició ni detuvo ningún otro dispositivo.

## 8. Follow-up para el orquestador

1. Persistir el acceptance: `feature_list.json` -> `status: accepted` con resumen de este informe en `evidence`; `PROGRESS.md` (Estado verificado / Siguiente paso) y entrada en `docs/progress-history.md`.
2. Opcional y recomendado al persistir: cerrar **D1** con la frase de `docs/technical-discovery.md:121` (una línea; también N4 si se documenta el entorno multi-AVD).
3. Nada más: sin cambios de código requeridos, sin re-ejecuciones pendientes, sin llamadas de pago ni dispositivos por tocar.
