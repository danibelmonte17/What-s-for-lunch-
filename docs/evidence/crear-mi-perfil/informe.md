# Informe de autoverificación — `crear-mi-perfil`

- **Feature**: `crear-mi-perfil` — Crear Mi perfil con restricciones dietéticas
- **Spec**: `docs/specs/crear-mi-perfil.md` (S1–S6, verify 1–5, D1–D8, T1–T7)
- **Fecha**: 2026-11-09 · **Sesión**: `feature-implementer`
- **Entorno**: AVD `Pixel_10_Pro` (API 37, `emulator-5554`) para gates y E2E focalizada; AVD **`Pixel_9_Pro` (API 37, `emulator-5556`, arrancado para esta sesión) para la batería completa, porque otra sesión ejecutaba instrumentación en `emulator-5554` (ver §8)
- **Estado**: `passing` = implementación autoverificada y lista para `feature-validator`. **No es aceptación.**
- **Claves de la evidencia**: solo datos ficticios («Frutos secos», «Diabetes», «Vegana») y cliente de IA simulado; **sin llamadas de pago**.

## 1. Qué se implementó

| Área | Cambio |
|---|---|
| Dominio | `domain/model/DietType.kt`, `domain/model/UserProfile.kt` (chips por sección + marcas + `dietType`), `domain/profile/UserProfileRules.kt` (validación, exclusividad D3, `pendingParts`/`isComplete` D5, `setDietType`), puerto `domain/repository/UserProfileRepository.kt` |
| Persistencia | Room **v6**: `ProfileEntity` (fila única `profile`), `ProfileRestrictionEntity` (`profile_restrictions`, `id` autoincremental = orden de alta), `ProfileDao.replaceProfile` (transacción), `MIGRATION_5_6`, `RoomUserProfileRepository` + `InMemoryUserProfileRepository`, bindings en `di/AppModule.kt` |
| UI | `ui/myprofile/MyProfileScreen.kt` (con `MyProfileTestTags`), `MyProfileViewModel.kt` (mutaciones vía `UserProfileRules`), `MyProfileRoute.kt`; tarjetas al estilo de `SettingsScreen` |
| Navegación | `AppTab.MY_PROFILE` **al final** de la barra (D4, icono `Person`, tag `bottom_bar_tab_profile`), `AppRoutes.MY_PROFILE`, rama `routeTab`, bloque `composable` con `scrollToTopTick`; comentario de hand-off retirado |
| Pinche | Gating en `PincheViewModel.runRequest` (RD26/D5): con partes pendientes **no llama a la IA** y añade `PincheMessage.profileRequest` con la acción «Abrir Mi perfil» (D8); puerto `reply(turns, language, profile)`; `renderProfilePrompt` añade el bloque `pinche_profile_{es,en}.txt` al system prompt (D6, sin tool `get_profile`) |
| Textos | 18 claves nuevas es/en (`my_profile_*`, `pinche_profile_*`) + `settings_privacy_notice` ampliado con los datos de salud (D7) |
| Tests | Nuevos `UserProfileRulesTest` (11), `MyProfileViewModelTest` (6), `MyProfileE2ETest` (7); ampliados `PincheViewModelTest` (+4 → 17), `PincheE2ETest` (+1), `PromptAssetsTest` (+1 → 6), `RecipeMigrationTest` (+1 → 3); adaptados `BottomBarE2ETest` (4 → 5 pestañas), los 4 `FakePincheChatClient` y las 7 construcciones de `PincheViewModel` |

## 2. Gates (scripts del repo)

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` | exit 0 — «Entorno listo», app instalada y en ejecución (`adb shell pidof` OK) |
| Lint | `./lint.ps1` → `lintDebug` | exit 0 — **0 errores / 48 warnings** (línea base 49; **ningún warning apunta a ficheros del diff**) |
| Unit | `./unit.ps1` → `testDebugUnitTest` | exit 0 — **44 suites / 327 tests / 0 fallos / 0 errores** (línea base 42/305: +2 suites y +22 tests de esta feature) |
| Build | `assembleDebug` + `assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Test APK | `compileDebugAndroidTestKotlin` | exit 0 |
| Paridad es/en | recuento de claves de `strings.xml` | **338/338 claves + 5/5 plurales**, diff vacío (línea base 320 + 5; +18 claves nuevas) |

Salidas conservadas: `lint-debug.txt` y `unit-tests.txt`.

## 3. Migración v5 → v6

`RecipeMigrationTest.migrationFromV5CreatesProfileTablesAndKeepsRecipeData` (en verde): base v5 legada → esquema exacto de `profile` (`id`, `dietType`, `allergiesNone`, `intolerancesNone`) y `profile_restrictions` (`id`, `section`, `value`), datos de `recipes` intactos y escritura/lectura del perfil sobre la base migrada conservando el orden de alta. Los builders de `RecipeMigrationTest` y `ShoppingListE2ETest` registran `MIGRATION_5_6`.

## 4. E2E focalizada (verify 1–5)

`connectedDebugAndroidTest` con las 4 clases afectadas: **27 tests / 0 fallos / 0 omitidos**

| Clase | Tests | Cubre |
|---|---|---|
| `MyProfileE2ETest` | 7/7 | verify 1 (alta/borrado en modo edición), verify 2 («Vegana»→«Omnívora» + persistencia Room), verify 3 («No tengo ninguna» limpia y bloquea), verify 4 (Pinche pide las tres partes sin llamar a la IA y «Abrir Mi perfil» navega), verify 5 (perfil completo en el puerto del cliente), verify 6 (persistencia + textos es/en) |
| `BottomBarE2ETest` | 5/5 | 5 pestañas en orden con etiquetas y destinos (verify 1 de `mover-opciones-a-bottom-bar` ampliado) |
| `PincheE2ETest` | 12/12 | regresión del Pinche + `emptyProfileAsksForItWithoutCallingTheAiAndAnswersOnceComplete` (gating con fake que captura el perfil) |
| `RecipeMigrationTest` | 3/3 | v3→v4, v4→v5 y **v5→v6** |

## 5. Batería E2E completa

`am instrument -w -r com.whatsforlunch.app.test/androidx.test.runner.AndroidJUnitRunner` sobre `emulator-5556` (API 37, app en español), 2026-11-09:

- **`numtests=240` (inventario completo), 1 fallo, 1 omitido por `Assume`** — log completo en `e2e-bateria-am-instrument.txt`.
- **Omitido**: `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (`assumeTrue` sin `AI_API_KEY` en `local.properties`); línea base conocida.
- **Único fallo**: `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` — `ComposeTimeoutException` de 5 s esperando el aviso «Configura tu API key…» en el formulario. **Es el flake ya documentado en la sesión 116** (fuera de este diff: el test espera un aviso del formulario de Receta, que esta feature no toca) y **pasa en aislamiento**: tras el run, `-e class …#deletingTheKey…` → **`OK (1 test)`**, `INSTRUMENTATION_CODE: -1`. También pasó en aislamiento en las sesiones 115/116.
- Inventario de tests instrumentados: **240 `@Test`** (HEAD 231; +9 de esta feature: 7 en `MyProfileE2ETest`, 1 en `PincheE2ETest`, 1 en `RecipeMigrationTest`), 27 ficheros / 31 clases.
- Corrida previa (en `emulator-5554`) añadía un segundo fallo, **reparado**: ver §8.

## 6. Verificación manual (adb/uiautomator, es-ES, 1280×2856)

1. **Pestaña «Mi perfil» al final de la barra** (D4): dump de la barra con las 5 pestañas en orden — Mis Recetas, Despensa, Lista de la Compra, Pinche, **Mi perfil** (última, activa en `primary`).
2. **Alta y borrado de chips en modo edición** (verify 1): alta de «Frutos secos» en Alergias y «Diabetes» en Intolerancias; «Editar» → icono de borrado con `content-desc` «Eliminar «Frutos secos»» y botón en «Listo»; borrar la chip la elimina y la otra sección queda intacta. Captura `01-perfil-con-chips-modo-edicion.png`.
3. **Tipo de alimentación** (verify 2): «Vegana» seleccionada (única, en verde) con las 5 opciones visibles. Captura `02-tipo-alimentacion-vegana.png`.
4. **Pinche bloquea y pide solo lo pendiente** (verify 4/RD26/D5): con Alergias y tipo completos e Intolerancias pendiente, «hola» responde con «Para responder necesito tu perfil: Intolerancias o enfermedades que limitan la dieta. Complétalo en «Mi perfil».» + botón **«Abrir Mi perfil»**; **sin llamada a la IA** (sin key, sin aviso de key: nunca se llegó al cliente). Captura `03-pinche-pide-perfil.png`.
5. **Acción «Abrir Mi perfil»** (D8): al tocarla se navega a la pestaña «Mi perfil» (dump con las tres tarjetas).
6. **Re-toque de la pestaña activa = scroll al inicio** (RF-MOB6): con el contenido bajado («Alergias» fuera de pantalla) se toca «Mi perfil» activa y «Alergias» vuelve a la vista.
7. **Persistencia** (verify 6): tras `am force-stop` y reabrir, el chip «Frutos secos» seguía en Room (y el perfil completo tras re-leerlo en los tests).

## 7. Seguridad y privacidad

- Sin secretos, tokens ni PII en el diff ni en la evidencia: solo datos ficticios («Frutos secos», «Diabetes», «Vegana»).
- Sin permisos Android nuevos, sin dependencias nuevas, **sin llamadas reales de pago** (el gating se verificó con cliente simulado; en el manual el Pinche nunca llegó al cliente).
- `RK03` ampliada con las restricciones dietéticas y el tipo de alimentación (datos de salud que salen al chatear con el Pinche) y `settings_privacy_notice` actualizado en es/en (D7).
- El perfil vive en Room en el dispositivo; la conversación del Pinche sigue solo en memoria.

## 8. Notas y hallazgos

- **Reparación de un resto del merge `9dacc87` (test ajeno)**: `PantryManagementE2ETest.scannerSaveReturnsToThePantryWithTheSavedFoods` pulsaba «Volver» en la Despensa, botón retirado por `e8c5730` (`mover-opciones-a-bottom-bar`, RF-MOB5) mientras el paso se añadía en `f591dbe` (`mover-escaneo-a-la-despensa`) en la otra rama. Fallaba en aislamiento **antes de tocar esta feature** y habría bloqueado cualquier batería. Sustituido por `Espresso.pressBack()` → Mis Recetas (comportamiento vigente RF-MOB4/D1, misma intención: sin PANTRY duplicado). Solo cambia el test.
- **Helper `localized()`**: `BottomBarE2ETest` y `PantryManagementE2ETest` resolvían el texto esperado con el locale del dispositivo mientras la app pinta su idioma (`AppLocaleController`); en un AVD `en-US` las aserciones fallaban. El helper resuelve ahora con el idioma activo de la app (cambio de test, sin efecto de producto).
- **AVD compartido**: otra sesión (worktree `feature-redisenar-formulario-de-receta`) ejecutaba instrumentación en `emulator-5554` y sus desinstalaciones (`deletePackageX`) mataron dos baterías con «Process crashed». Con `e2e.ps1` (que corre en **todos** los dispositivos conectados) el choque era inevitable, así que la batería se ejecutó en un **segundo emulador** (`emulator-5556`, arrancado para esta sesión y apagado al cerrar) con `am instrument` apuntando solo a ese dispositivo.
- Evidencia en `docs/evidence/crear-mi-perfil/`: `informe.md`, `lint-debug.txt`, `unit-tests.txt`, `e2e-bateria-am-instrument.txt` y 3 capturas PNG válidas (1280×2856).
