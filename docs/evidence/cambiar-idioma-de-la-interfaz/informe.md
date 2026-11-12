# Evidencia de `cambiar-idioma-de-la-interfaz` — implementación autoverificada

- **Sesión**: 103 (`feature-implementer`, 2026-11-06/07; la spec se commiteó en `63dbb10`).
- **Rama**: `feature/cambiar-idioma-de-la-interfaz`.
- **Entorno**: AVD `Pixel_10_Pro` (API 37) en `emulator-5554`, Windows 11, Gradle 8.14.3 / JDK 17 (Corretto). Sistema del emulador en **es-ES** (`settings get system system_locales` = `es-ES`; el `persist.sys.locale` = `en-US` no manda sobre la configuración real).
- **Estado**: `passing` (implementada y autoverificada; **no es aceptación** — pendiente de `feature-validator`).

## 1. Gates del harness

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `init.ps1` (con `ANDROID_SERIAL=emulator-5554`) | App instalada y en ejecución (`pidof` 30355, `topResumedActivity` = `MainActivity`). El script **sale 1** por el falso fallo conocido de `adb shell monkey`/stderr en PowerShell (documentado en `PROGRESS.md`); verificado aparte, no bloqueante. |
| Lint | `lint.ps1` / `lintDebug` | **0 errores / 49 warnings** = línea base histórica. Los 2 warnings nuevos que trajo la feature (`UseKtx` en el store, `AppBundleLocaleChanges` por el cambio de locale dinámico) se **corrigieron**: `androidx.core.content.edit { }` y `bundle { language { enableSplit = false } }` en `app/build.gradle.kts`. |
| Unit | `unit.ps1` + `testDebugUnitTest --rerun-tasks` | **42 suites / 299 tests / 0 fallos** (antes: 40/291; +2 suites `AppLanguageTest` (3) y `AppLocaleControllerTest` (3) y +2 tests en `SettingsViewModelTest` (8→10): 291+3+3+2 = 299). Recuento reconciliado desde los XML de `app/build/test-results/`. |
| Ensamblado | `assembleDebug` | **BUILD SUCCESSFUL** (y `assembleDebugAndroidTest` OK). |
| Paridad es/en | recuento de claves `<string>`/`<plurals>` | **330/330** (3 claves nuevas `settings_language_title/spanish/english`; endónimos idénticos en ambos locales). |

## 2. E2E — `LanguageSwitchE2ETest` (8 tests, 8/8 verde en `emulator-5554`)

Focalizada: `gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.LanguageSwitchE2ETest"` → **8/8** (2026-11-06). Fakes por constructor (`InMemory*`, `CapturingRecipeSuggester`, `CapturingStepSpeaker`), fixtures locales y cliente de IA simulado.

| Escenario | Test | Cubre |
|---|---|---|
| S1 (verify 1) | `switchingToEnglishUpdatesAllScreensWithoutRestart` | Ajustes + Mis Recetas cambian al momento sin reinicio; back stack intacto («Back» → «My Recipes»); endónimos «Español»/«English»; opción marcada con `assertIsSelected`. |
| S2 | `switchingBackToSpanishRestoresAllTexts` | Reversión a español en todas las pantallas del back stack. |
| S3 | `selectionSurvivesAppRestart` | Persistencia con SharedPreferences reales: un `AppLocaleController` nuevo sobre el mismo store (equivalente a reabrir tras `force-stop`) arranca en inglés con sistema en español. |
| S4 | `firstLaunchFollowsEnglishSystemLanguage` / `firstLaunchFallsBackToSpanishForOtherSystemLanguages` | Sin selección guardada, preselección por sistema: `en` → English; cualquier otro (`Locale.FRANCE`) → Español (D2). |
| S5 (RF-IDI4) | `aiSuggestionRequestUsesEnglishWhenAppIsInEnglish` / `aiSuggestionRequestUsesSpanishWhenAppIsInSpanish` | La petición de Sugerencias lleva al cliente de IA el idioma de la app (`"en"`/`"es"`) derivado de `LocalConfiguration`, como en producción. |
| S5 (RF-IDI4) | `voiceLocaleFollowsAppLanguage` | El locale de voz del Modo Cocina sigue al idioma de la app (`Locale.ENGLISH` → `es-ES` al volver a español y reactivar la voz), con la `CookModeRoute` **real** y TTS/reconocimiento simulados. El permiso `RECORD_AUDIO` se concede con `pm grant` vía `uiAutomation` desde el propio test (sin dependencias nuevas; `androidx.test:rules` no está en el proyecto). |

## 3. Batería E2E completa (`e2e.ps1`, 2026-11-06) — regresión

- **221 tests del runner** (la consola AGP muestra «Finished 223» por doble cuenta de los omitidos): **218 verdes + 1 fallo + 2 omitidos**.
- **1 fallo**: `ScanPantryE2ETest#successivePhotosAccumulateWithoutOverwritingEditedChips`, firma «Activity never becomes requested state "[DESTROYED]"» — **flake de teardown de `ActivityScenario`** documentado en `PROGRESS.md`, ajeno a esta feature (la suite no toca el idioma). Re-ejecución focalizada del solo test: **1/1 verde** (29 s) → flake confirmado.
- **2 omitidos**: `SettingsE2ETest` por `Assume` sin key de build (preexistentes).
- La suite nueva está incluida en la batería y pasó dentro de ella.

## 4. Comprobaciones manuales (adb + uiautomator, emulador `emulator-5554`)

Técnica: `adb shell uiautomator dump` + `adb pull` + parseo del XML (reintentar si el primer dump devuelve «null root node»); capturas con `cmd /c "adb exec-out screencap -p > <file>.png"` (redirección binaria de `cmd`, nunca la `>` de PowerShell que escribe UTF-16). Ambas capturas verificadas: cabecera PNG `89 50 4E 47`, 1280×2856.

1. **S4 manual (preselección por sistema)**: `pm clear com.whatsforlunch.app` + arranque → la app muestra «Mis Recetas» / «Todavía no tienes recetas» (español, el idioma del sistema es-ES) y en Ajustes la fila «Español» está `checked=true` (nodo `[96,1365][1184,1509]`). Captura: **`ajustes-idioma-es.png`** (247.511 bytes).
2. **Verify 1 manual (cambio sin reinicio)**: tap en la fila «English» → toda la pantalla cambia al momento a «Settings» / «Language» / «Save» / «Privacy and data» / «Back» y la fila «English» queda `checked=true`, **sin reiniciar la app** (mismo proceso, misma Activity). Captura: **`interfaz-idioma-en.png`** (250.207 bytes).
3. **S3 manual (persistencia entre reinicios)**: `adb shell am force-stop com.whatsforlunch.app` + reapertura → la app arranca en inglés («My Recipes» / «No recipes yet» / «Create recipe» / «Scan your Pantry») pese al sistema en es-ES, y Ajustes abre con «Language» y la fila «English» `checked=true`. La elección persiste (D1).

## 5. Hallazgos de implementación (registrados también en la spec)

- **RF-IDI4 exigía tocar un consumidor**: `AndroidStepSpeaker` resolvía los textos de la locución («Paso N. {texto}», «Temporizador de N minutos») con el contexto inyectado (idioma del sistema); ahora los localiza con un contexto localizado con el **locale de voz** preparado. Sin tocar `StepSpeaker`, `VoiceCommandParser` ni los prompts.
- **El proveedor de contexto localizado vive en `MainActivity`** (`ui/locale/AppLocaleHost`), no dentro de `WhatsForLunchApp`, para no romper los E2E de localización existentes (que montan `WhatsForLunchApp()` con su propio `CompositionLocalProvider`). Reprovidea `LocalConfiguration`/`LocalResources` (no `LocalContext`, que sigue siendo la Activity real por `PhotoAcquirer`).
- **Dos warnings de lint nuevos resueltos** para mantener la línea base de 49 (ver gate de lint).

## 6. Seguridad

Sin hallazgos: sin secretos ni PII en código, logs, capturas ni docs (la clave de DevExpert Inference no aparece); SharedPreferences del idioma sin cifrar solo guarda el nombre del enum (`SPANISH`/`ENGLISH`), no sensible; **sin permisos nuevos** (el manifiesto sigue con `CAMERA` e `INTERNET`; `RECORD_AUDIO` ya existía de `controlar-cocina-con-voz` y solo se concede en el test), **sin dependencias nuevas**, sin red. Prompts de IA (`assets/prompts/*_{es,en}.txt`) y contenido del usuario **intactos** (D5/D6). `feature_list.json` solo cambia `status`/`evidence`.

## 7. Qué queda para la validación independiente

`feature-validator` sobre `docs/specs/cambiar-idioma-de-la-interfaz.md` (RF-IDI1–4, verify 1 = S1, D1–D6): comprobar selector en Ajustes, cambio sin reinicio, persistencia, preselección por sistema, prompts/voz/formatos con el idioma activo, non-goals (solo es/en, sin «Seguir el sistema») y que los 2 flaky/omitidos de la batería son los documentados. Evidencia histórica de la feature en esta misma carpeta; la de features cerradas vive en `docs/evidence/<feature-id>/`.
