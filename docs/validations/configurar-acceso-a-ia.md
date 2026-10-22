# Informe de validación — `configurar-acceso-a-ia`

- **Fecha**: 2026-10-22 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: **`accept` para D1-D6** · **`revise` en la revalidación del delta D7-D11** (ver §8; 1 Medium + 4 Low, sin Critical/High)
- **Spec**: `docs/specs/configurar-acceso-a-ia.md` (decisiones D1-D6, escenarios 1-5, `Validator Checklist`).
- **Entorno**: AVD **`Pixel_10_Pro` API 37** arrancado para esta validación en `emulator-5580` (`sdk_gphone16k_x86_64`, 480 dpi, 2048 MB), con `emulator-5556` (`Pixel_8_Pro`) y `emulator-5562` (`Pixel_9_Pro`) ya en marcha en el mismo host. JDK 17 (`C:\Users\Daniel\.jdks\corretto-17.0.20`), `ANDROID_SERIAL=emulator-5580`. El AVD se apagó al terminar (`adb emu kill`).
- **Diff revisado**: `data/ai/{AiKeyStore,KeystoreAiKeyStore}.kt` (nuevos), `AiKeyProvider.kt` (retira `UnconfiguredAiKeyProvider`), `di/AppModule.kt`, `ui/settings/{SettingsViewModel,SettingsScreen,SettingsRoute}.kt` (nuevos), `ui/myrecipes/{MyRecipesScreen,MyRecipesRoute}.kt`, `ui/nav/AppNavHost.kt`, `values/strings.xml` y `values-en/strings.xml`, `SettingsViewModelTest.kt`, `SettingsE2ETest.kt`, `KeystoreAiKeyStoreTest.kt` (nuevos) y documentación. Coincide con `Expected File Changes` de la spec; sin cambios en `AndroidManifest.xml`, `build.gradle.kts` ni `libs.versions.toml` (0 dependencias y 0 permisos nuevos).

## 1. Comprobaciones reranqueadas por el validador (sobre el árbol actual, sin commitear)

| Comando | Resultado | Detalle |
|---|---|---|
| `lintDebug` | **BUILD SUCCESSFUL** | **0 errores**, 44 warnings; los 4 de `KeystoreAiKeyStore.kt` son intencionales y documentados (`ApplySharedPref` ×2 y `UseKtx` ×2 por `commit()` síncrono). |
| `testDebugUnitTest --rerun-tasks` | **exit 0** | **65/65, 0 fallos/errores/omitidos** en 12 suites (recuento desde XML), incluido `SettingsViewModelTest` **4/4**. |
| `assembleDebug` | **exit 0** | `BUILD SUCCESSFUL`. |
| `connectedDebugAndroidTest` (suite completa) | **exit 0** | **103/103, 0 fallos, 0 errores, 0 omitidos** (295 s) en `Pixel_10_Pro(AVD) - 17`; `SettingsE2ETest` **4/4**, `KeystoreAiKeyStoreTest` **3/3**, sin regresión en Escáner (17), CRUD de recetas (18+14+9+1+1), Detalle (7), Mis Recetas/coincidencia (2+2+6) ni búsqueda/filtros (8). XML: `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml` (2026-10-22T11:06:43). |
| `init.ps1` | **inspeccionado, no reranqueado** | Evidencia del implementador en `PROGRESS.md`/`feature_list.json` (exit 0; gate no bloqueante). Sus comprobaciones equivalentes (lint, unit, build) se reranquearon aquí sobre el árbol actual. |
| `installDebug` + app real con `adb`/`uiautomator` | **OK** | Usado para la verificación manual de §2 (el pase de `connectedDebugAndroidTest` deja la app desinstalada en este AVD). |

## 2. Escenarios de la spec (manual en `Pixel_10_Pro` + E2E reranqueado)

| Escenario | Resultado | Evidencia |
|---|---|---|
| **S1 registrar la key** | **pasa** | Ajustes sin key: «No configurada», campo `password="true"` vacío, «Guardar» `enabled="false"`. Key sintética `sk-test-validator-2026` → Guardar → **«Configurada»**, campo vacío y «Eliminar API key» visible. |
| **S2 la key no queda en claro en disco** | **pasa** | `run-as cat shared_prefs/ai_key_prefs.xml` → solo `api_key` con `Base64(IV||ciphertext)` (`wY5WNrp3…`); `grep -rl 'sk-test-validator-2026' .` sobre el directorio de datos de la app → **0 coincidencias**; `logcat --pid` de la app → **0 coincidencias**. Cubierto además por `KeystoreAiKeyStoreTest` 3/3 (XML literal sin la key, round-trip y corrupción → `null`). |
| **S3 borrar pide configurarla** | **pasa** | «Eliminar API key» → diálogo es «¿Eliminar tu API key de IA?…» con Cancelar/Eliminar → confirmar → «No configurada» y `ai_key_prefs.xml` = `<map />`. Después, en el Formulario, «Generar con IA» muestra **«Configura tu API key de IA en Ajustes para generar imágenes.»** sin bloquear el formulario. E2E `deletingTheKeyAsksToConfigureItAgainForAiFeatures` en verde. |
| **S4 aviso de privacidad** | **pasa** | «Privacidad y datos» + aviso D5 visible en es (`uiautomator` y capturas del implementador) y en en (`SettingsE2ETest.settingsAndPrivacyNoticeAreLocalizedInEnglish`). |
| **S5 persistencia y campo protegido** | **pasa** | Tras `am force-stop` + relanzar la app y reabrir Ajustes: **«Configurada»**, campo `password="true"` vacío; guardar con campo vacío sigue deshabilitado. E2E `savedKeyMarksConfiguredAndNeverRevealsTheKeyWhenReturning` en verde. |

## 3. Decisiones y alcance

| Decisión | Resultado | Evidencia |
|---|---|---|
| **D1** acceso desde Mis Recetas | **pasa** | `IconButton` con `content-desc="Ajustes"` en la barra, junto al de Despensa; área táctil 132×144 px ≥ 48 dp; ruta `settings`. |
| **D2** Keystore directo AES-256/GCM | **pasa** | Clave AES-256 en `AndroidKeyStore` (alias `whatsforlunch_ai_key`), IV nuevo por guardado, `Base64(IV||ciphertext)` en `ai_key_prefs`; sin `security-crypto` ni dependencias nuevas. |
| **D3** validación local | **pasa** | Trim + rechazo de vacío en `SettingsViewModel`; sin red ni comprobación de formato (no hay llamadas de red en el flujo). |
| **D4** la key nunca se re-muestra | **pasa** | Campo `PasswordVisualTransformation`, vacío tras guardar y al reabrir; sin texto de la key en la UI. |
| **D5** aviso de privacidad siempre visible | **pasa** | Presente con y sin key; texto es/en con paridad exacta de claves (**198/198**). |
| **D6** sin key nada se rompe | **pasa** | Aviso `create_recipe_notice_ai_key_missing` tras borrar; `CreateRecipeViewModel` y `OpenAiCompatibleImageGenerator` sin cambios en el diff. |

**Fuera de alcance respetado**: sin selector de idioma, sin llamadas a OpenRouter, sin medición de latencia/coste, sin cambios de esquema Room ni de permisos, sin secretos reales (solo claves `sk-test-*`).

## 4. Hallazgos

Todas las observaciones son **Low y no bloqueantes**; ninguna impide la aceptación.

### Finding 1 (Low): aserción duplicada en `SettingsE2ETest`

- **Evidencia**: `app/src/androidTest/java/com/whatsforlunch/app/SettingsE2ETest.kt:158-165` repite `onNodeWithText(...create_recipe_notice_ai_key_missing...).assertExists()` dos veces.
- **Por qué importa**: ruido de mantenimiento; no altera la cobertura.
- **Cambio requerido**: eliminar el bloque duplicado.

### Finding 2 (Low): estado `cleared` muerto

- **Evidencia**: `SettingsUiState.cleared` y `SettingsViewModel.consumeCleared()` no se consumen desde `SettingsRoute`/`SettingsScreen` (solo se usa `saved`).
- **Por qué importa**: código sin uso que sugiere una confirmación de borrado que no existe.
- **Cambio requerido**: retirar `cleared`/`consumeCleared()` o consumirlos si se quiere mostrar confirmación.

### Finding 3 (Low): T8 «reinicio de actividad» no está cubierto literalmente por el E2E

- **Evidencia**: `savedKeyMarksConfiguredAndNeverRevealsTheKeyWhenReturning` vuelve y reabre Ajustes (nueva instancia del ViewModel), no recrea la actividad/proceso.
- **Por qué importa**: la persistencia real entre procesos no estaba probada por test.
- **Mitigación verificada en esta validación**: `am force-stop` + relanzar la app → sigue «Configurada» y el campo vacío (§2, S5).
- **Cambio requerido (opcional)**: añadir al E2E una recreación de actividad o mantener la comprobación manual como evidencia.

### Finding 4 (Low): línea obsoleta en `PROGRESS.md`

- **Evidencia**: `PROGRESS.md`, sección «Riesgos y preguntas abiertas», sigue diciendo que `UnconfiguredAiKeyProvider` devuelve `null` «hasta `configurar-acceso-a-ia`».
- **Por qué importa**: contradice el código actual (el proveedor provisional ya no existe).
- **Cambio requerido**: actualizar esa línea al persistir la aceptación.

### Finding 5 (Low): artefacto de renderizado en las capturas

- **Evidencia**: en `ajustes-sin-key.png` / `ajustes-configurada.png` la palabra «Volver» aparece partida verticalmente en la barra superior.
- **Por qué importa**: cosmético y del emulador; no afecta a la funcionalidad ni a la legibilidad del contenido principal.
- **Cambio requerido**: ninguno obligatorio; valorar recapturar si se reutilizan como material de diseño.

**Notas no bloqueantes**: (a) `allowBackup="true"` heredado: un backup restaurado en otro dispositivo no puede descifrar (la clave del Keystore no viaja) y degrada a «No configurada» sin crash; (b) `secretKey()` podría generar dos claves en una carrera de dos primeras lecturas simultáneas (no alcanzable desde la UI de un solo hilo); (c) el aviso D5 afirma «sin retención de datos para entrenamiento», cuya verificación efectiva sigue pendiente en `RK03` (copy exigido por la spec).

## 5. Seguridad (checklist por feature)

- **Sin hallazgos bloqueantes.** Sin secretos ni PII en el diff, tests, capturas o documentos; solo claves sintéticas `sk-test-*`.
- La key no aparece en claro en `shared_prefs/`, `databases/`, `files/` ni `logcat` (verificado a mano y por test instrumentado).
- Cifrado AES-256/GCM con clave custodiada por Android Keystore; descifrado solo en memoria, con degradación segura (`null`) ante corrupción o clave invalidada.
- Sin dependencias, permisos ni endpoints nuevos; el flujo de IA existente mantiene su degradación (RD05/RF-21) y no se hace ninguna llamada de pago durante la validación.

## 6. Documentación y estado del arnés

- `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` actualizados correctamente (decisión D2, tarea de almacenamiento cerrada, latencia/coste aplazados, RK01 con mitigación implementada).
- `AGENTS.md`/`CONTEXT.md` sin cambios necesarios (no cambian flujos ni términos).
- `feature_list.json` y `PROGRESS.md` reflejan `passing` con evidencia; la línea obsoleta de `PROGRESS.md` es el Finding 4.
- Presupuesto de evidencia respetado: informe de implementación + 3 capturas es en `docs/evidence/configurar-acceso-a-ia/`; esta validación no añade artefactos gráficos.

## 7. Veredicto

**`accept`**: la feature cumple la spec (S1-S5, D1-D6), la cobertura E2E persistente existe y pasó en `Pixel_10_Pro` (103/103, con `SettingsE2ETest` 4/4 y `KeystoreAiKeyStoreTest` 3/3), los checks baratos reranqueados están en verde y la seguridad no presenta hallazgos. Los 5 findings son Low y no bloqueantes.

**Actualización de estado recomendada para el orquestador**:

1. `feature_list.json` → `"status": "accepted"` y añadir a `evidence` el veredicto con el recuento (`lint 0 errores`, `unit 65/65`, `connected 103/103`, `Pixel_10_Pro` API 37) y la ruta de este informe.
2. `PROGRESS.md`: reescribir *Estado verificado* / *Siguiente paso* (14 de 28 `accepted`) y corregir la línea obsoleta de `UnconfiguredAiKeyProvider` (Finding 4).
3. `docs/progress-history.md`: añadir la entrada de sesión de validación.
4. Commits `feat:`/`docs:` y publicación (el validador no persiste la aceptación por sí solo).

## 8. Revalidación del delta de configuración de build (2026-10-22) — veredicto `revise`

- **Alcance validado**: decisiones **D7-D11** y escenarios **6-8** de la spec, sobre el árbol sin commitear de `feature/configurar-acceso-a-ia` (HEAD `90e2a2a`). La implementación de D1-D6 sigue amparada por el `accept` de §7; esta revalidación se centra en el delta y en que no haya regresiones.
- **Veredicto**: **`revise`** — 1 finding **Medium** (documentación duradera contradictoria) y 4 **Low**. Sin Critical/High; la funcionalidad del delta está implementada y verificada.

### Comprobaciones reranqueadas por el validador (árbol actual)

| Comando | Resultado | Detalle |
|---|---|---|
| `gradlew lintDebug testDebugUnitTest --rerun-tasks assembleDebug` | **BUILD SUCCESSFUL** (33 s) | `lintDebug` **0 errores**, 44 warnings; `testDebugUnitTest` **76/76** en 14 suites, 0 fallos/errores/omitidos (`PriorityAiKeyProviderTest` 4/4, `AiEndpointsTest` 3/3, `SettingsViewModelTest` 8/8); `assembleDebug` OK. |
| Inspección del `BuildConfig` generado sin `AI_*` en `local.properties` | **OK** | `AI_BASE_URL = ""` y `AI_API_KEY = ""`; `local.properties` solo tiene `sdk.dir` y está ignorado por `.gitignore:5`. |
| `git grep` de claves/secretos | **OK** | 0 coincidencias de la key sintética en el código; solo aparece como valor de prueba en documentación (ver L1). No hay claves reales en ficheros versionados. |

### Comprobaciones caras inspeccionadas (no reranqueadas)

- `connectedDebugAndroidTest`: XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml` → **104 tests, 0 fallos, 0 errores, 1 omitido**, 201,7 s, `2026-10-22T11:45:07`. El omitido es `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (solo aplica con `AI_*`), y `SettingsE2ETest` aporta 5 casos. Coherente con el árbol actual, sin cambios de código posteriores al pase.
- `SettingsE2ETest` con `AI_*` sintéticas (3 ejecutados en verde, 2 omitidos por `Assume`): registrado en la evidencia del implementador e informe `docs/evidence/configurar-acceso-a-ia/informe-build-config.md`; coherente con el código y con el XML.
- `init.ps1` exit 0: registrado en la sesión 55 (salida en la transcripción de la sesión). El log `.harness/init-configurar-acceso-a-ia.log` es de la sesión 54 (11:12); sus checks equivalentes (lint/unit/build) se reranquearon aquí.
- Captura manual `docs/evidence/configurar-acceso-a-ia/ajustes-clave-build-es.png` (existe, 316 KB): la nota y el ocultado de «Eliminar» también están cubiertos por la aserción instrumentada `buildProvidedKeyShowsConfiguredNoteAndHidesDelete`.

### Escenarios y decisiones

| Elemento | Resultado | Evidencia |
|---|---|---|
| **S6** prioridad de la key de build y nota en Ajustes | **pasa** | `PriorityAiKeyProvider` + DI + `SettingsViewModel`; unit 8/8 y E2E focalizado. |
| **S7** sin `AI_*` el comportamiento es el actual | **pasa** | `BuildConfig` vacío; `AiEndpoints` por defecto; suite completa 104/104 con 1 omitido esperado. |
| **S8** composición de base sin barra duplicada | **pasa** | `AiEndpointsTest` 3/3. |
| **D7** debug y release, sin imprimir valores, vacío si faltan | **pasa** (inspección) | `buildConfigField` en `defaultConfig` aplica a ambos build types; evidencia del implementador de `assembleDebug` + `assembleRelease` con `AI_*` sintéticas; el build actual sin `AI_*` deja ambos campos vacíos. |
| **D8-D10** precedencia, semántica de base y fuente efectiva en Ajustes | **pasa** | Código y unit tests; `canDelete` oculta el borrado y `isConfigured` usa el proveedor efectivo. |
| **D11** riesgo de key en el APK asumido y documentado | **pasa con M1** | Nueva sección en `docs/technical-discovery.md` y `S05`/`RK01`; contradicción con bullets antiguos del mismo documento (M1). |
| Non-goals | **respetados** | Sin UI de baseURL/fuente, sin proxy, sin llamadas de red nuevas, sin tocar `init.ps1`, sin cambiar el copy de «Generar con IA». |

### Hallazgos

#### Finding M1 (Medium): `technical-discovery.md` se contradice sobre dónde vive la API key

- **Severidad**: Medium
- **Evidencia**:
  - `docs/technical-discovery.md:152` afirma «API key: nunca en el repositorio ni hardcodeada; **solo en almacenamiento cifrado del dispositivo**».
  - `docs/technical-discovery.md:118` limita las llamadas a «la API key del usuario en Ajustes».
  - `docs/technical-discovery.md:123` dice que la credencial «la aporta el propio usuario».
  - Frente a eso, la sección nueva del mismo documento (líneas 79-86, D7-D11) y `docs/risks-and-open-questions.md` (`S05`/`RK01`) dicen que `AI_API_KEY` se inyecta en `BuildConfig` y queda **incrustada en el APK** (riesgo aceptado D11).
- **Por qué importa**: la spec exige que la documentación duradera no contradiga `⚠ S05`; un agente futuro que lea «solo en almacenamiento cifrado» puede concluir que la vía de build no existe o que está prohibida, y no ver la tarea de retirarla antes de publicar.
- **Cambio requerido**: reconciliar los tres bullets antiguos con D7-D11, dejando una única descripción coherente (Ajustes cifrado + build incrustado, con prioridad del build y riesgo aceptado).
- **Implementación sugerida**:
  1. En «Integraciones externas»: «…con la API key de Ajustes o la inyectada en el build (`local.properties`, D7-D11), con prioridad de la de build».
  2. En «Autenticación y autorización»: aclarar que la credencial puede venir del usuario (Ajustes, cifrada) o del build (desarrollo).
  3. En «Seguridad y datos sensibles»: sustituir el bullet por «nunca se commitea; en Ajustes se guarda cifrada con Keystore; con D7 la key de build queda incrustada en el APK (riesgo aceptado, usar key de desarrollo con límite de gasto y retirarla antes de distribuir); nunca se imprime ni se registra».
- **Verificación tras el arreglo**: lectura cruzada de `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y la sección D7-D11; 0 afirmaciones que digan que la key solo vive cifrada en el dispositivo.

#### Finding L1 (Low): la evidencia afirma «git grep de la key sintética → 0 coincidencias», pero el propio historial la contiene

- **Evidencia**: `feature_list.json` (evidencia del delta), `PROGRESS.md` (bullet de sesión 55) y `docs/evidence/configurar-acceso-a-ia/informe-build-config.md:31` afirman 0 coincidencias; sin embargo, `docs/progress-history.md:577` y el propio informe (línea 27) escriben literalmente `sk-test-build-config-2026`.
- **Por qué importa**: la afirmación es verificable y ahora es falsa; erosiona la confianza en la evidencia (aunque el valor es sintético y no una credencial real).
- **Cambio requerido**: reformular la comprobación como «0 claves reales en ficheros versionados; solo valores sintéticos `sk-test-*` en documentación» o dejar de escribir el valor literal en los artefactos nuevos. No borrar entradas históricas de `progress-history.md` (regla de solo añadir).
- **Verificación**: `git grep -n "sk-test-"` muestra solo valores de prueba identificables como tales; ninguna coincidencia de una clave real.

#### Finding L2 (Low): el escenario 6 no tiene una prueba automática de que el generador use la base/key del build

- **Evidencia**: `PriorityAiKeyProviderTest` cubre la precedencia y `AiEndpointsTest` la composición, pero ningún test ejerce `OpenAiCompatibleImageGenerator` con el proveedor compuesto y una base sintética para comprobar `Authorization`/URL. La cobertura E2E del camino de build solo verifica Ajustes.
- **Por qué importa**: la segunda mitad de S6 («la inferencia usa la key y la baseURL del build») queda verificada por inspección del DI, no por ejecución.
- **Cambio requerido (opcional, no bloqueante para este delta)**: añadir un unit test del generador con un `OkHttpClient` falso que capture la petición y compruebe URL y cabecera, o documentar explícitamente que se acepta la verificación por composición.
- **Verificación**: test nuevo en verde sin llamadas de red reales.

#### Finding L3 (Low): comentarios obsoletos sobre la fuente única

- **Evidencia**: `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyProvider.kt:3-4` sigue diciendo que la key «se guarda cifrada … aquí solo se consume», sin mencionar la vía de build.
- **Por qué importa**: el comentario induce a error al siguiente lector del puerto.
- **Cambio requerido**: mencionar que el proveedor puede devolver la key de build (D7-D8) y que Ajustes es el fallback.

#### Finding L4 (Low): hallazgos Low del `accept` anterior siguen abiertos

- **Evidencia**: aserción duplicada en `SettingsE2ETest` (bloque `waitUntil` + `assertExists` repetido, líneas 170-179); `SettingsUiState.cleared`/`consumeCleared()` sin consumidor; el reinicio de actividad (T8) sigue sin test automatizado.
- **Por qué importa**: ruido de mantenimiento; no afecta a la corrección del delta.
- **Cambio requerido**: opcional en un futuro ciclo de pulido.

### Evaluación de seguridad (checklist por feature)

- **Sin hallazgos Critical/High.** No hay secretos reales ni PII en el diff, tests o documentos: solo valores sintéticos `sk-test-*`.
- La fuente de build es la config del desarrollador (`local.properties`, gitignored) y el riesgo de extracción del APK está **aceptado explícitamente** en D11; queda registrado en `S05`/`RK01` con la tarea de retirarlo antes de publicar. La contradicción documental M1 es el único punto a corregir para que ese riesgo no se diluya.
- Sin dependencias, permisos ni endpoints nuevos; sin llamadas de red durante la validación. El flujo de «Generar con IA» mantiene su degradación (RD05/RF-21) y el almacén Keystore no cambia en este delta.

### Documentación y estado del arnés

- `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` recogen D7-D11, pero M1 deja una contradicción interna.
- `feature_list.json` solo movió `status`/`evidence`; `PROGRESS.md` está al día; el historial se añadió sin borrar entradas.
- Presupuesto de evidencia razonable: informe de build + 1 captura nueva; sin artefactos gráficos en esta validación.

### Veredicto y acción recomendada

**`revise`**: la funcionalidad del delta cumple S6-S8 y D7-D11 con evidencia adecuada, pero M1 impide cerrar la feature conforme al checklist de la spec («documentación duradera … sin contradicciones con `⚠ S05`»). Al ser un arreglo de documentación (más L1-L4 opcionales), el siguiente paso es el `feature-implementer` con el repair brief de abajo.

**Actualización de estado recomendada para el orquestador**:

1. `feature_list.json` → `"status": "in_progress"` y añadir a `evidence` el veredicto `revise` con este informe.
2. `PROGRESS.md` → estado verificado + siguiente paso (repair brief).
3. `docs/progress-history.md` → entrada de la revalidación.
4. Tras el arreglo, revalidar (cheap + E2E inspeccionada) antes de `accepted` y de commitear/publicar.

### Implementation Repair Brief

Goal: alinear la documentación duradera y la evidencia con D7-D11; no cambiar comportamiento ni alcance.

Do:
1. Reescribir los bullets de `docs/technical-discovery.md` en «Integraciones externas» (línea 118), «Autenticación y autorización» (línea 123) y «Seguridad y datos sensibles» (línea 152) para describir las dos fuentes (Ajustes cifrado y build incrustado), la prioridad del build y el riesgo aceptado D11.
2. Reformular en `feature_list.json`, `PROGRESS.md` y `docs/evidence/configurar-acceso-a-ia/informe-build-config.md` la comprobación «git grep → 0 coincidencias» como «0 claves reales; solo valores sintéticos `sk-test-*` en documentación»; no añadir más valores literales y no borrar entradas de historial.
3. Actualizar el comentario de `AiKeyProvider.kt` para mencionar la fuente de build.
4. Opcional (si se quiere cerrar L2): añadir un unit test del generador con cliente falso que verifique URL y cabecera `Authorization`; sin red real.

Do not:
- Cambiar la precedencia, el endpoint por defecto, la UI de Ajustes ni los textos aprobados.
- Añadir UI de baseURL/fuente, proxy, dependencias, permisos o llamadas de red.
- Tocar `init.ps1` ni otras features.

Run:
- `gradlew lintDebug testDebugUnitTest --rerun-tasks assembleDebug`
- `git grep -n "sk-test-"` (solo valores sintéticos identificables; ninguna clave real)
- Revisión cruzada de `docs/technical-discovery.md` con `S05`/`RK01`

Update:
- `feature_list.json` (mantener `in_progress` hasta revalidar; `evidence` con el arreglo)
- `PROGRESS.md` y `docs/progress-history.md`
- El informe de evidencia del build, si se reescribe la comprobación de `git grep`

## 9. Revalidación del repair (2026-10-22) — veredicto `accept`

- **Alcance validado**: repair brief de §8 sobre el árbol sin commitear de `feature/configurar-acceso-a-ia` (HEAD `90e2a2a`), centrado en D7-D11 y S6-S8. La base D1-D6 sigue amparada por el `accept` de §7 y por la revalidación de §8; aquí se comprueba que el arreglo no introdujo regresiones.
- **Veredicto**: **`accept`** — sin findings Critical/High; M1, L1 y L3 quedan verificados como resueltos y L2 queda documentado como verificación por composición aceptada, tal como permitía el brief. L4 y dos observaciones documentales quedan como notas Low no bloqueantes.

### Comprobaciones reranqueadas (cheap tier)

| Comando | Resultado | Detalle |
|---|---|---|
| `init.ps1` | **exit 0** | AVD `Pixel_9_Pro` API 17 (`emulator-5554`): `installDebug` y arranque de `com.whatsforlunch.app` OK; sin servicios persistentes. |
| `gradlew lintDebug testDebugUnitTest --rerun-tasks assembleDebug` | **BUILD SUCCESSFUL** (1 m 17 s) | `lintDebug` **0 errores**, 44 warnings; `testDebugUnitTest` **76/76** en 14 suites, 0 fallos/errores/omitidos; `assembleDebug` OK. |
| `BuildConfig` generado sin `AI_*` | **OK** | `BuildConfig.AI_BASE_URL = ""` y `BuildConfig.AI_API_KEY = ""`; `local.properties` solo contiene `sdk.dir` y está en `.gitignore:5`. |
| `git grep "sk-test-"` | **OK** | Solo valores sintéticos identificables (`progress-history.md`, `feature_list.json`, `PROGRESS.md`); ninguna clave real. Sin `AI_*` en ficheros versionados de propiedades/XML. |
| Paridad de textos `settings_*` | **OK** | 16 claves en `values/` y 16 en `values-en/`, sin diferencias. |

### Evidencia cara inspeccionada (no reranqueada)

- `connectedDebugAndroidTest`: XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml` → **104 tests, 0 fallos, 0 errores, 1 omitido**, 201,701 s, `2026-10-22T11:45:07`, AVD `Pixel_10_Pro`. El omitido es `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (solo aplica con `AI_*`); `SettingsE2ETest` aporta sus 5 casos. El repair posterior solo tocó documentación y un comentario de `AiKeyProvider.kt`, sin cambio de bytecode funcional, así que el pase sigue siendo representativo del árbol actual.
- `assembleRelease` con `AI_*` sintéticas: se inspecciona la evidencia del implementador (sesión 55), que registra ambos `BuildConfig` inyectados; no se repite por coste y porque no hay cambios de build en el repair.
- Captura manual `docs/evidence/configurar-acceso-a-ia/ajustes-clave-build-es.png`: existe y es coherente con la aserción instrumentada del caso omitido; no se recaptura.

### Findings del repair, verificados

| Finding | Estado | Evidencia de la revalidación |
|---|---|---|
| **M1** documentación contradictoria sobre dónde vive la key | **Resuelto** | `docs/technical-discovery.md:113,118,123,152` describen las dos fuentes (Ajustes cifrado + build incrustado), la prioridad del build y el riesgo aceptado `S05`/`RK01`. `git grep "solo en almacenamiento cifrado"` → 0 coincidencias. |
| **L1** afirmación falsa de `git grep` | **Resuelto** | `feature_list.json`, `PROGRESS.md` e `informe-build-config.md` usan «0 claves reales; solo valores sintéticos `sk-test-*` en documentación». La entrada histórica de `progress-history.md` se conserva por la regla de solo añadir y queda contextualizada por la entrada nueva. |
| **L2** S6 sin test del generador | **Aceptado y documentado** | `informe-build-config.md` documenta la verificación por composición (`PriorityAiKeyProviderTest`, `AiEndpointsTest` y cableado de `AppModule`) y justifica no añadir el test del generador por `android.util.Base64`/Robolectric. Era una opción explícita del brief. |
| **L3** comentario obsoleto | **Resuelto** | `AiKeyProvider.kt` menciona la fuente de build con prioridad y el fallback de Ajustes. |

### Notas no bloqueantes (Low)

- **L4 sigue abierto** (opcional): aserción duplicada en `SettingsE2ETest` (`waitUntil` + `assertExists`), `SettingsUiState.cleared`/`consumeCleared()` sin consumidor y reinicio de actividad sin test automatizado. No afecta a la corrección del delta.
- **Redacción residual en la spec**: `docs/specs/configurar-acceso-a-ia.md:214,221` aún pide «`git grep` de la key sintética → 0 coincidencias», mientras que la evidencia correcta es «0 claves reales; solo valores sintéticos». Es una instrucción del plan de verificación, no una afirmación de evidencia; conviene alinearla en el próximo pase de documentación.
- **`docs/build-brief.md:69`** describe solo la fuente de Ajustes. No contradice D7-D11 (la key sigue sin guardarse en el repositorio), pero omite la fuente de build; mejora documental para otro pase.

### Evaluación de seguridad

- **Sin hallazgos Critical/High.** No hay secretos reales ni PII en el diff, tests, capturas o documentos: solo valores sintéticos `sk-test-*` identificables. `local.properties` está gitignored, solo contiene `sdk.dir` y no hay `AI_*` en ficheros versionados.
- La key de build queda incrustada en el APK: riesgo **aceptado explícitamente** (D11) y documentado en `S05`/`RK01` con la tarea de retirar la inyección antes de distribuir. La prioridad build → Ajustes está cubierta por unit tests y el caso E2E de key de build.
- El almacén de Ajustes mantiene AES-256/GCM con IV nuevo por guardado, `Base64(IV || ciphertext)` en `SharedPreferences` y sin logs de la key; `apiKey()` degrada a `null` ante datos corruptos. No hay dependencias, permisos ni endpoints nuevos; no se hicieron llamadas de red ni de pago.

### Documentación y estado del arnés

- `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` quedan coherentes con D7-D11 y `S05`/`RK01`.
- `feature_list.json` solo ha movido `status`/`evidence`; `PROGRESS.md` refleja el repair y el siguiente paso; `docs/progress-history.md` conserva las entradas anteriores y añade la nueva.
- El árbol sigue sin commitear; no hay procesos ni artefactos temporales de esta validación.

### Veredicto y acción recomendada

**`accept`**: el repair resuelve M1 y L1-L3, L2 queda documentado conforme al brief y no hay regresiones ni riesgos de seguridad que impidan cerrar la feature. El orquestador debe persistir `accepted` en `feature_list.json` y actualizar `PROGRESS.md`/`docs/progress-history.md`; después, decidir con el usuario el commit, push y PR (sin merge).
