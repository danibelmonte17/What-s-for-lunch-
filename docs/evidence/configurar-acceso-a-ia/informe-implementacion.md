# Informe de implementación — `configurar-acceso-a-ia`

- **Fecha**: 2026-10-22 · **Estado**: `passing` (pendiente de `feature-validator`; este informe **no** es aceptación).
- **Entorno**: emulador `Pixel_10_Pro` (**API 37**, 480 dpi), app en `es-ES` por locale de aplicación
  (`cmd locale set-app-localeconfig` + `set-app-locales`), revertido al terminar.
- **APK**: `app/build/outputs/apk/debug/app-debug.apk` (`com.whatsforlunch.app`, `installDebug` exit 0).

## 1. Gates del arnés

| Comando | Resultado | Detalle |
|---|---|---|
| `init.ps1` | **exit 0** | log en `.harness/init-configurar-acceso-a-ia.log` |
| `lintDebug` | **exit 0** | **0 errores**, 44 warnings (4 nuevos, intencionales, ver §4) |
| `testDebugUnitTest` | **exit 0** | **65 tests, 0 fallos, 0 saltados** (incluye `SettingsViewModelTest` 4/4) |
| `assembleDebug` | **BUILD SUCCESSFUL** | |
| `connectedDebugAndroidTest` (suite completa) | **103 tests, 0 fallos, 0 saltados** | `Pixel_10_Pro(AVD) - 17`, ~7 min |
| `connectedDebugAndroidTest` (focalizada `SettingsE2ETest` + `KeystoreAiKeyStoreTest`) | **7/7, 0 fallos** | 4 E2E de Ajustes + 3 instrumentados de cifrado |

Notas de entorno (no son fallos del producto): el AVD se comparte con otras sesiones del mismo
repositorio y la suite larga abortó varias veces por falta de memoria del host
(`INSTRUMENTATION_ABORTED`, «device not found»). El pase completo en verde se obtuvo al liberarse
memoria (`ANDROID_SERIAL=emulator-5558`); es el comportamiento ya documentado en la spec
(«suites largas pueden abortar por el emulador compartido»). Después de ese pase solo se añadió un
comentario explicativo en `KeystoreAiKeyStore.kt` (sin cambio de lógica); sobre el árbol final se
repitieron `lint` (0 errores), `unit` (65/65) y la suite focalizada (7/7) en verde.

## 2. Trazabilidad de las dos `verification` de `feature_list.json`

| Verificación | Resultado | Evidencia |
|---|---|---|
| **1. E2E: introducir una API key de prueba en Ajustes y comprobar que queda guardada cifrada** | **pasa** | `SettingsE2ETest.savedKeyMarksConfiguredAndNeverRevealsTheKeyWhenReturning` (estado «Configurada» y campo siempre vacío al volver) + `KeystoreAiKeyStoreTest.savedKeyIsCiphertextOnDiskAndRoundTrips` (el XML de `shared_prefs` no contiene la key en claro y el round-trip devuelve el valor) + comprobación manual (§3) |
| **2. E2E: borrar la API key y comprobar que las funciones de IA piden configurarla** | **pasa** | `SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures`: tras confirmar el borrado, «Generar con IA» muestra `create_recipe_notice_ai_key_missing` (sin llamada de red, porque `apiKey()` es `null`) |

Escenarios cubiertos por `SettingsE2ETest`: 1 (apertura de Ajustes desde Mis Recetas con aviso de
privacidad y sin key), 3 (guardar y borrar con confirmación), 4 (textos es/en) y 5 (persistencia al
volver y campo nunca re-mostrado).

## 3. Comprobación manual (adb + uiautomator)

1. Ajustes se abre desde el icono de la barra de Mis Recetas; sin key muestra «No configurada», el
   campo enmascarado vacío, «Guardar» deshabilitado y el aviso «Privacidad y datos»
   (`ajustes-sin-key.png`).
2. Se introduce una key sintética (`sk-test-evidence-2026`, sin valor real) y se guarda: el estado
   pasa a «Configurada», el campo vuelve a estar vacío y aparece «Eliminar API key»
   (`ajustes-configurada.png`).
3. En disco solo hay ciphertext:

   ```
   adb shell run-as com.whatsforlunch.app cat shared_prefs/ai_key_prefs.xml
   <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
   <map>
       <string name="api_key">3N1wRuATHwtdUd7pT+4rUxgxIch5M5VhgtCIHsUxzsIhy73JVXSJZR0gPzpJZ1hTSQ==</string>
   </map>
   ```

4. Barrido de datos de la app: `run-as ... grep -rl 'sk-test-evidence-2026' .` → **sin coincidencias**
   en `shared_prefs/`, `databases/` ni `files/` (solo existen `whatsforlunch.db*`, `ai_key_prefs.xml`
   y `files/profileInstalled`).
5. Logcat filtrado por el PID de la app: **0 coincidencias** de la key (las únicas apariciones en el
   log del dispositivo son el eco de `adbd` de los propios comandos `adb shell input text`).
6. «Eliminar API key» abre el diálogo de confirmación (`ajustes-eliminar-dialogo.png`); al confirmar,
   el estado vuelve a «No configurada» y `shared_prefs/ai_key_prefs.xml` queda en `<map />`.

## 4. Decisiones y notas para el validador

- **Sin dependencias nuevas**: cifrado con Android Keystore (AES-256/GCM, alias
  `whatsforlunch_ai_key`) y `Base64(IV || ciphertext)` en `SharedPreferences` privadas
  (`ai_key_prefs`), según D2/⚠ S05.
- **Escritura síncrona a propósito**: `save`/`clear` usan `commit()` para que la key quede persistida
  antes de que la UI confirme. Eso genera 4 warnings de lint (`ApplySharedPref` ×2, `UseKtx` ×2) en
  `KeystoreAiKeyStore.kt`, documentados en el propio código; **no hay errores de lint**.
- **La key guardada nunca se re-muestra** (D4): la UI solo conoce «Configurada/No configurada», el
  campo queda enmascarado (`PasswordVisualTransformation`) y se vacía tras guardar.
- **Sin cambios en el flujo de IA** (D6): `CreateRecipeViewModel`, sus avisos y
  `OpenAiCompatibleImageGenerator` no se tocan; solo se sustituyó el `AiKeyProvider` provisional por
  el almacén cifrado.
- **Fuera de alcance respetado**: sin selector de idioma, sin red en Ajustes y sin llamadas de pago.
- `SettingsE2ETest` localiza con `LocalConfiguration`/`LocalResources` en lugar de reemplazar
  `LocalContext`, porque `CreateRecipeRoute` necesita el `Activity` real para montar el
  `SystemPhotoAcquirer` del escenario 5.

## 5. Evidencia

- `docs/evidence/configurar-acceso-a-ia/informe-implementacion.md` (este informe).
- `docs/evidence/configurar-acceso-a-ia/ajustes-sin-key.png` — Ajustes en español sin key: «No
  configurada», campo enmascarado, «Guardar» deshabilitado y aviso de privacidad.
- `docs/evidence/configurar-acceso-a-ia/ajustes-configurada.png` — Ajustes en español con key
  configurada, campo vacío y aviso de privacidad.
- `docs/evidence/configurar-acceso-a-ia/ajustes-eliminar-dialogo.png` — diálogo de confirmación de
  borrado.
