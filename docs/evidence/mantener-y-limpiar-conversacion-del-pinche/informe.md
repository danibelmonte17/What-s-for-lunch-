# Informe de autoverificación — `mantener-y-limpiar-conversacion-del-pinche`

- **Fecha**: 2026-11-11 · **Rama**: `feature/control-conversacion-pinche`
- **Spec**: `docs/specs/mantener-y-limpiar-conversacion-del-pinche.md`
- **Emuladores de verificación**: focalizada y primeros gates en `Pixel_8_Pro` (`emulator-5558`, API 37) e `init.ps1` + batería final en `Pixel_10_Pro` (`emulator-5554`, API 37, AVD relanzado limpio) — indicación del usuario en esta sesión («usa siempre Pixel_8_pro» → después «usa el Pixel_10_pro» para la batería)
- **Rol**: autoverificación del `feature-implementer`; la aceptación independiente corresponde a `feature-validator`.

## Qué se implementó (D1-D11 de la spec)

- **D8/ámbito**: `PincheRoute` resuelve el `PincheViewModel` anclado al `ViewModelStore` de la Activity (`koinViewModel(viewModelStoreOwner = LocalActivity.current as? ComponentActivity)`), con **fallback** al owner por defecto cuando la composición no tiene Activity (hallazgo de implementación: `LocalActivity` deriva de `LocalContext` y en el patrón `showApp` de los E2E el contexto es de app). Así la conversación sobrevive a `openTab`, que destruía la entrada `pinche` del back stack.
- **D9/D10/limpieza**: `PincheViewModel.clearConversation()` incrementa la generación, cancela el `Job` de la petición en curso y resetea el estado a `PincheUiState()` inicial. La generación descarta `onContent` y los handlers `onSuccess`/`onUnavailable` tardíos (y los fallos de guardar/añadir a la Lista).
- **D3/D4/D7/UI**: `IconButton` con `Icons.Outlined.DeleteSweep` en la top bar de `PincheScreen`, visible solo con `messages.isNotEmpty()`, limpieza directa, tag `pinche_clear_conversation` y clave `pinche_clear_conversation` es/en («Limpiar conversación» / «Clear conversation»).
- **D6**: la limpieza no toca «Mi perfil» ni ningún dato persistido (verificado por E2E: el perfil sembrado viaja en el puerto a la conversación nueva y la chip sigue en su pantalla).

## Gates baratos (todos en verde)

| Comando | Resultado |
| --- | --- |
| `./init.ps1` (`ANDROID_SERIAL=emulator-5558`) | **exit 0 — «Entorno listo»** |
| `./gradlew :app:lintDebug` | **No issues found** (0 errores / 0 warnings = línea base) |
| `./gradlew :app:testDebugUnitTest` | **47 suites / 377 tests / 0 fallos** (línea base 375 + 2 nuevos) |
| `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| Paridad es/en (`values/` vs `values-en/`) | **342/342 claves, diff 0** (incluye `pinche_clear_conversation`) |

Evidencia: `lint-2026-11-11.txt` («No issues found.»), `unit-PincheViewModelTest-2026-11-11.xml` (27 tests, 0 fallos).

## E2E focalizada (2026-11-11, ~19:06, `emulator-5558`)

**34/34 tests, 0 fallos, 0 omitidos** — «Running tests on devices: Pixel_8_Pro(AVD) - 17» (AGP con `ANDROID_SERIAL`: sin instalación en el dispositivo físico conectado):

- `PincheConversationE2ETest` **5/5** (clase nueva): `conversationSurvivesSwitchingTabsAndComingBack` (verify 1), `clearButtonResetsToTheInitialScreenAndTheChatWorksAgain` (verify 2), `clearButtonIsHiddenWhenEmptyAndShowsItsSpanishDescription` + `clearButtonShowsItsEnglishDescription` (verify 3 es/en), `clearingKeepsTheProfileAndTheNextChatCarriesIt` (D6).
- `PincheE2ETest` **17/17** (regresión, incluye el S6 de streaming).
- `BottomBarE2ETest` **5/5** y `MyProfileE2ETest` **7/7** (regresión; el verify 4 con cliente «sin key» inyectado en Koin pasa tras mis cambios).

El XML de esta pasada fue sobrescrito por la batería posterior; el recuento (raíz `<testsuites tests="34" failures="0" errors="0" skipped="0">`) se observó en la sesión y la batería vuelve a cubrir las mismas clases.

## Tests nuevos

- **Unit** (`PincheViewModelTest`, 25 → 27):
  - `clearConversationResetsTheStateToTheInitialOne` — estado «sucio» (mensaje + aviso + borrador + `lastUserText`) → `clearConversation()` → igualdad exacta con `PincheUiState()`.
  - `clearConversationCancelsTheRequestAndDiscardsLateWrites` — con un «proveedor» no cooperativo que completa pese a la cancelación: se observa la cancelación y ni el fragmento tardío ni la respuesta tardía pinta nada (D5/D10).
- **E2E** (`PincheConversationE2ETest`, clase nueva con `createAndroidComposeRule<MainActivity>()`, app real y `ReplyingPincheChatClient` inyectado en Koin con `@After` que restaura el binding real): los 5 tests de arriba.

## Batería completa (`./e2e.ps1`)

- **Intento 1 — `emulator-5558` (Pixel_8_Pro), ~19:11**: falló a los **118 tests** por **interferencia externa**, no por el diff: a las **19:15:17** otra sesión ejecutó `shell:pm install -r -t "/data/local/tmp/app-debug.apk"` sobre el emulador a mitad de `MyProfileE2ETest` → `PACKAGE_FULLY_REMOVED` de `com.whatsforlunch.app` y `.test` (logcat en los resultados de esa pasada), la app murió (`signal 9`) y la instrumentación abortó con `testRunFailed` (`emptyProfileShowsTheAiNoticeAndTheActionOpensItsTab` falló por la desinstalación en vuelo; el fallo va **sin texto de aserción**, que es la firma de la instrumentación muriendo). Corrobora que no es regresión: la focalizada de las 19:06 había corrido esa misma clase 7/7 en verde.
- **Intento 2 — `emulator-5558` (Pixel_8_Pro), 12m49s**: cortado a los **77 tests** con el mismo patrón: a las **19:32:50** adbd recibió `cmd package uninstall com.whatsforlunch.app` y `… .test` de otra sesión durante `DeleteRecipeE2ETest.deleteDialogAndActionAreLocalizedInEnglish` (logcat: `PACKAGE_ADDED replacing: true` + `cmd package 'uninstall'`). Hasta ese punto, **todos los tests ejecutados pasaron salvo el coincidente con la desinstalación**.
- **Intento 3 — `emulator-5554` (Pixel_10_Pro, AVD relanzado limpio por instrucción del usuario): COMPLETADO en 22m49s** — **256 tests = inventario `@Test`** (251 de la sesión 128 + 5 nuevos): **253 verdes + 2 fallos + 1 omitido** (`Assume` de `SettingsE2ETest.buildProvidedKey…` sin `AI_API_KEY`, línea base). `init.ps1` en ese AVD: **exit 0 — «Entorno listo»**. Los 2 fallos son flakes de timing **ajenos al diff** y **ambos en verde en el rerun aislado** (`connectedDebugAndroidTest` de las dos clases → BUILD SUCCESSFUL; XML `e2e-aislada-flakes-2026-11-11.xml` = 17 tests / 0 fallos):
  - `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures` — `ComposeTimeoutException: Condition still not satisfied after 5000 ms` (flake heredado de las sesiones 115/116/119/121/122/125).
  - `RecipeDetailE2ETest.servingsStepperIsDisabledAtLowerBound` — `AssertionError: Activity never becomes requested state "[DESTROYED]" (last lifecycle transition = "PAUSED")` (carrera de back-out conocida; `RecipeDetailE2ETest` no comparte código con el diff).
  - **Clases de la feature en la batería**: `PincheConversationE2ETest` **5/5**, `PincheE2ETest` **17/17**, `MyProfileE2ETest` **7/7**, `BottomBarE2ETest` **5/5**.
  - El XML de la batería se sobrescribió con el rerun aislado (mismo patrón que el hallazgo N1 de la sesión 115): el log `e2e-bateria-pixel10-2026-11-11-2101.log` y este informe son la evidencia durable con los recuentos.

## Docs durables actualizados en este cambio

- `docs/build-brief.md` — «Historial de conversación entre sesiones»: «mientras dura la pantalla» → «mientras la app está abierta», con el botón de limpiar.
- `docs/domain-model.md` — **RD17** enmendada (memoria mientras la app está abierta + «Limpiar conversación» sin tocar datos persistidos) y 3 casos límite nuevos (cambio de pestaña, limpieza con streaming, limpieza con Ficha/Lista/perfil).
- `DESIGN.md` — fila «Pinche»: top bar con «Limpiar conversación» y conservación de la conversación.
- `docs/risks-and-open-questions.md` — `Q-mantener-y-limpiar-conversacion-del-pinche-1..5` (D3-D7, confirmadas por el usuario el 2026-11-11).
- `docs/technical-discovery.md` — estado del Pinche: anclaje al ViewModelStore de la Activity, `clearConversation()` con generación y el IconButton.
- `docs/specs/mantener-y-limpiar-conversacion-del-pinche.md` — D8 y el paso 1 del enfoque ampliados con el hallazgo del fallback de `LocalActivity`.

## Incidencias de entorno

1. **`emulator-5554` (Pixel_9_Pro_Validator, primera instancia) compartido**: su BD estaba en Room **v8** (build de otra sesión) y el build de esta rama (**v7**) crashea al abrir (`A migration from 8 to 7 was required`); `init.ps1` allí salió 1 por ese motivo (no es el falso negativo histórico de `monkey`/`pidof`). Además había otra sesión instalando/lanzando (APK `lastUpdateTime` ajeno, `uiautomator` activo). Decisión del usuario: usar **Pixel_8_Pro**.
2. **`emulator-5558` (Pixel_8_Pro)**: en los intentos 1 y 2 de la batería otra sesión instaló/desinstaló el paquete a mitad de la ejecución (detalle arriba). Decisión del usuario: usar **Pixel_10_Pro**.
3. **`emulator-5554` (Pixel_10_Pro)**: el primer arranque quedó con un photopicker zombi reteniendo el foco y el `monkey` de `init.ps1` no lograba traer la app (el `pidof` de la verificación salía vacío; `am start` explícito sí la levantaba). Tras `adb reboot` el emulador se quedó sin responder adbd → proceso eliminado y AVD relanzado de cero; el segundo arranque pasó `init.ps1` con exit 0.

## Capturas (L1 del validador, obtenidas el 2026-11-11 sobre el emulador con `adb` + `uiautomator`)

Todas PNG válidas **1280×2856** en este directorio:

- `captura-01-conversacion-con-boton-limpiar-es.png` — conversación en español con el botón «Limpiar conversación» visible en la top bar (chip de sugerencia enviada → aviso de key ausente + mensaje estático de partes pendientes).
- `captura-02-estado-inicial-tras-limpiar-es.png` — tras pulsar el botón: saludo + chips de nuevo, sin mensajes ni botón (estado inicial, D4/D7).
- `captura-03-conversacion-con-boton-limpiar-en.png` — la misma pantalla en inglés (idioma cambiado en Ajustes) con el botón en `Clear conversation`; el idioma se restauró a español después.

## Validación independiente (feature-validator con subagente, 2026-11-11): `accept`

Informe en `docs/validations/mantener-y-limpiar-conversacion-del-pinche.md`. Tier barato **re-ejecutado por el validador**: `init.ps1` exit 0, `lintDebug --rerun-tasks` 0/0, `testDebugUnitTest --rerun-tasks` 47/377/0, `assembleDebug` + `compileDebugAndroidTestKotlin` OK, paridad 342/342 + 5/5 plurals, inventario `@Test` = 256 y **E2E focalizada re-ejecutada 34/34** en `emulator-5554`. Tier caro **inspeccionado** (log de batería + recuentos del informe + XML aislado posterior sin deriva). **0 Critical/High/Medium; 3 Low**: L1 (capturas PNG — cerrado con las 3 de arriba), L2 (tareas de la spec sin marcar — cerrado: `[x]`) y L3 (XML crudos de batería/focalizada sobrescritos por reruns — patrón N1 conocido; log + informe son la evidencia). Seguridad sin hallazgos (sin secretos/permisos/dependencias, E2E con fakes y 0 llamadas de pago).

## Pendientes / huecos declarados

- **S4 (limpiar con petición en curso)**: cubierta por unit con gates (`CompletableDeferred`) y proveedor no cooperativo; sin E2E — la spec lo declara así por determinismo (los gates de streaming ya viven en el `FakePincheChatClient` de `PincheE2ETest` si el validador quiere ampliarla).
- Sin llamadas de pago a la IA en ninguna verificación (fakes en unit y E2E; `AI_API_KEY` vacía en el entorno).
- Sin tocar `uxdocs/`, `.opencode/` ni scripts del harness; `feature_list.json` solo en `status`/`evidence`.
