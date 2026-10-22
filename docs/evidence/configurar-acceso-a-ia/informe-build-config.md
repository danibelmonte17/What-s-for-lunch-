# Evidencia — delta `configurar-acceso-a-ia` (configuración de build, D7-D11)

Fecha: 2026-10-22 · Rama `feature/configurar-acceso-a-ia` · Emulador `Pixel_10_Pro` API 37 (`emulator-5580`, arrancado y apagado por esta sesión).

## Qué se implementó

- `local.properties` (gitignored) aporta `AI_BASE_URL` y `AI_API_KEY`; `app/build.gradle.kts` los inyecta en `BuildConfig` para **debug y release**, con `""` si faltan y sin imprimir valores.
- `PriorityAiKeyProvider`: la key de build (recortada) gana si no está vacía; si no, se usa `AiKeyStore` (Ajustes).
- `AiEndpoints.imageGeneration`: `AI_BASE_URL` es la base (`https://openrouter.ai/api/v1` por defecto) y compone `<base sin barra final>/images/generations`.
- Ajustes: estado «Configurada» con la fuente efectiva, nota `settings_api_key_build_source` (es/en) y «Eliminar API key» oculto cuando manda la key de build (`canDelete`).
- Tests: `PriorityAiKeyProviderTest` (4), `AiEndpointsTest` (3), `SettingsViewModelTest` (8, 4 nuevos) y `SettingsE2ETest` (5, independiente de `local.properties`).

## Gates

| Check | Resultado |
|---|---|
| `init.ps1` | exit 0, entorno listo y app en ejecución |
| `lintDebug` | BUILD SUCCESSFUL · **0 errores**, 44 warnings (4 intencionales de `commit()`) |
| `testDebugUnitTest --rerun-tasks` | **76/76** en 14 suites, 0 fallos/errores/omitidos |
| `assembleDebug` sin `AI_*` | BUILD SUCCESSFUL · `BuildConfig.AI_BASE_URL = ""` y `AI_API_KEY = ""` |
| `assembleDebug` + `assembleRelease` con `AI_*` sintéticas | BUILD SUCCESSFUL · ambos `BuildConfig` con `https://proxy.local/v1` y la key sintética |
| `connectedDebugAndroidTest` (sin `AI_*`) | **104/104**, 0 fallos/errores, **1 omitido**: `buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (solo aplica con key de build) |
| `SettingsE2ETest` focalizado (con `AI_*` sintéticas) | 5 tests: 3 ejecutados y en verde (incluido el de key de build), 2 omitidos con `Assume` (flujos que exigen almacén único) |

## Verificación manual (adb + uiautomator)

Con `AI_BASE_URL=https://proxy.local/v1` y una `AI_API_KEY` sintética de prueba (`sk-test-*`, sin valor real) en `local.properties`, build e `installDebug`:

- Ajustes muestra **«Configurada»**, la nota «La clave de IA proviene de la compilación (local.properties) y tiene prioridad sobre la guardada aquí.» y **no** muestra «Eliminar API key».
- Captura: `ajustes-clave-build-es.png`.
- No hay claves reales en ficheros versionados: el valor usado es sintético y queda identificado como `sk-test-*`; `local.properties` quedó restaurado a solo `sdk.dir` y el `BuildConfig` final se regeneró vacío.

## Notas

- El riesgo de key incrustada en el APK queda asumido (D11) y documentado en `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` (`S05`/`RK01`, tarea de retirar la inyección antes de publicar).
- La mitad de S6 relativa a la inferencia se verifica **por composición**: `PriorityAiKeyProviderTest` cubre prioridad/trim/fallback, `AiEndpointsTest` cubre base→endpoint y el cableado de `AppModule` conecta `BuildConfig.AI_API_KEY` con el proveedor y `BuildConfig.AI_BASE_URL` con el generador. No se añade un test del generador con cliente falso porque `android.util.Base64` no está disponible en la JVM y exigiría Robolectric o un refactor fuera del alcance del repair; se acepta explícitamente esta verificación por composición.
- No se hicieron llamadas de red ni de pago a la IA.
