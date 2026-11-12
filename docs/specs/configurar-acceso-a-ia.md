# Feature Implementation Spec: Configurar acceso a IA

## Source Feature

- `id`: configurar-acceso-a-ia
- `area`: seguridad
- `depends_on`: arrancar-app-en-mis-recetas (`accepted`)
- `status`: `accepted` (delta D7-D11 implementado, repair del `revise` aplicado y revalidación independiente con veredicto `accept` el 2026-10-22 — ver `docs/validations/configurar-acceso-a-ia.md` §9)
- `source`: `feature_list.json`

## Goal

El usuario registra, sustituye y borra su API key de OpenRouter desde una pantalla **Ajustes**, con
aviso de privacidad visible. La key queda **cifrada en el dispositivo** (nunca en claro en disco, en
el repositorio, en logs ni re-mostrada en pantalla — `⚠ S05`, `RK01`) y las funciones de IA existentes
(«Generar con IA» de `crear-receta`) pasan a consumirla sin cambiar su comportamiento: sin key, aviso
que pide configurarla y el formulario sigue usable (RF-21, `Q-crear-receta-6`).

**Cambio de esta revisión (2026-10-22)**: además del almacén cifrado de Ajustes, la **baseURL** y la
**API key** de la inferencia de IA se pueden aportar desde `local.properties` en tiempo de compilación
(decisiones D7-D11). Si existen, **tienen prioridad**; si no, se mantiene el flujo ya implementado de
Ajustes + Android Keystore. Es una vía provisional para el MVP, aceptada por el usuario sabiendo que la
key queda incrustada en el APK de debug **y de release**.

## Non-Goals

- Selector de idioma en Ajustes: es `cambiar-idioma-de-la-interfaz` (esta pantalla solo se estructura para acogerlo después).
- Llamadas de red de ningún tipo en Ajustes: ni validar la key contra OpenRouter, ni chat, ni sugerencias, ni generación de imágenes nuevas.
- Medición de latencia y coste reales de OpenRouter (tarea de investigación de `RK01`): **aplazada** por decisión del usuario.
- Configuración «sin retención» en el panel de OpenRouter (`RK03`): se documenta en el aviso, su verificación efectiva queda para `sugerir-recetas-con-ia`.
- Cambiar el flujo de «Generar con IA» ni su copy existente (`create_recipe_notice_ai_key_missing`).
- Proxy/backend para ocultar la key (`P34`, fuera del MVP) y cualquier mecanismo para ocultarla dentro del APK (ofuscación, NDK, cifrado del binario).
- UI para editar la baseURL, la fuente de la key o el endpoint: la configuración de build es solo fichero + `BuildConfig`.
- Modificar `init.ps1` para generar o conservar `AI_*`: el fichero `local.properties` es del desarrollador y está en `.gitignore`; `init.ps1` ya solo lo crea si no existe.

## Job Story

When quiero usar las funciones con IA de la app (hoy, generar la foto de una Receta),
I want to registrar mi API key de OpenRouter en Ajustes —o inyectarla desde `local.properties` mientras desarrollo—
so I can que la app llame al proveedor desde mi dispositivo sin que mi key quede expuesta ni almacenada en claro.

## Users And Permissions

- **Usuario** (único rol; dueño del dispositivo, sin cuentas ni login — `P31`): registra, sustituye y borra su propia API key desde Ajustes. La key es credencial del usuario, no de la app (`docs/domain-model.md`, «Roles y acceso»).
- **Desarrollador/autor del build**: define `AI_BASE_URL` y `AI_API_KEY` en `local.properties` (fichero gitignored) para compilar con una key de desarrollo. No hay rol de aplicación asociado: son valores de compilación.

## Decisiones fijadas con el usuario

- **D1 — Acceso a Ajustes**: acción (icono) en la barra superior de Mis Recetas, junto al icono de Despensa existente; patrón `IconButton` + `testTag` ya usado.
- **D2 — Almacenamiento cifrado (fallback)**: Android Keystore directo (AES/GCM/NoPadding, clave 256 bits) con `Base64(IV || ciphertext)` en `SharedPreferences` `ai_key_prefs`. Sin `androidx.security:security-crypto`.
- **D3 — Validación local únicamente** en Ajustes: recorte de espacios y rechazo de campo vacío; sin comprobaciones de formato ni llamadas de red.
- **D4 — La key guardada nunca se re-muestra**: estado «Configurada/No configurada», campo enmascarado; sustituir = guardar encima; borrar = diálogo de confirmación.
- **D5 — Aviso de privacidad (`P27`) siempre visible en Ajustes** con las afirmaciones ya aprobadas (uso solo para llamar a OpenRouter, proveedor sin retención —`RK03` pendiente—, fotos del Escáner que no salen del dispositivo, límites de gasto en el panel de OpenRouter).
- **D6 — Sin key nada se rompe**: las funciones de IA degradan con aviso y entrada manual (RD05, RF-21).
- **D7 — Fuente de build (`local.properties`)**: claves `AI_BASE_URL` y `AI_API_KEY` leídas en tiempo de compilación desde el `local.properties` de la raíz (gitignored) e inyectadas en `BuildConfig.AI_BASE_URL` / `BuildConfig.AI_API_KEY` para **debug y release** (decisión del usuario, 2026-10-22, para el MVP). Si el fichero o las claves faltan, los campos valen `""` y el build no falla. Los valores no se imprimen en la salida de Gradle.
- **D8 — Precedencia**: si `BuildConfig.AI_API_KEY` no está vacía (tras `trim()`), la usa la inferencia; si está vacía, se cae a `AiKeyStore.apiKey()` (Ajustes). Misma regla para la baseURL: `AI_BASE_URL` no vacía gana; si no, `https://openrouter.ai/api/v1`.
- **D9 — Semántica de baseURL**: `AI_BASE_URL` es la base compatible con OpenAI, terminada en `/api/v1` por convención. El generador compone el endpoint como `<base sin barra final>/images/generations`. La base por defecto es la de OpenRouter, de modo que sin configuración el comportamiento es idéntico al actual.
- **D10 — Ajustes refleja la fuente efectiva**: «Configurada» si **cualquiera** de las dos fuentes aporta key. Cuando la key de build está activa, Ajustes muestra una nota `settings_api_key_build_source` (es/en) y **oculta** «Eliminar API key», porque borrar la del dispositivo no cambiaría la efectiva. Guardar en Ajustes sigue permitido (queda como fallback para builds sin key).
- **D11 — Riesgo de seguridad asumido**: con D7 la key de `local.properties` queda **incrustada en el APK** (debug y release) y es extraíble del binario. Se acepta para el MVP con estas condiciones: `local.properties` nunca se commitea ni se copia a documentos/capturas, no se imprime en logs, se usa una key de desarrollo con límite de gasto y el APK release con key real no se distribuye fuera del equipo. Queda registrado en `docs/risks-and-open-questions.md` (`S05`, `RK01`) como pendiente de revertir antes de publicar.

## Acceptance Scenarios

### Scenario 1: Registrar la API key (flujo Ajustes, sin key de build)

Given Ajustes sin key configurada y `BuildConfig.AI_API_KEY` vacía
When el usuario escribe una key, la guarda
Then el estado pasa a «Configurada», el campo queda enmascarado y la key persiste cifrada (visible para `AiKeyProvider`, nunca en claro en disco)

### Scenario 2: La key no queda en claro en disco (S05)

Given una key guardada desde Ajustes
When se inspeccionan los ficheros de datos de la app (`shared_prefs/`, Room, logs)
Then no aparece la key en claro en ninguno: el valor persistido es ciphertext AES-GCM y el texto plano solo existe en memoria al consumirla

### Scenario 3: Borrar la key pide configurarla (verify 2)

Given una key configurada en Ajustes y `BuildConfig.AI_API_KEY` vacía
When el usuario pulsa «Eliminar», confirma y después usa «Generar con IA» en el Formulario
Then la key queda eliminada (estado «No configurada») y «Generar con IA» muestra el aviso de configurarla sin bloquear el formulario

### Scenario 4: Aviso de privacidad (P27)

Given la pantalla Ajustes en español o en inglés
When se abre en cualquier estado (con o sin key)
Then se muestra el aviso de privacidad (D5) con los textos `settings_privacy_*` de `values/` y `values-en/`

### Scenario 5: Persistencia y campo protegido

Given una key guardada en Ajustes
When se cierra y reabre la aplicación o se vuelve a Ajustes
Then el estado sigue siendo «Configurada» y el campo nunca muestra la key en claro; el campo vacío no permite guardar (D3/D4)

### Scenario 6: La key de `local.properties` tiene prioridad (D7/D8)

Given un `local.properties` con `AI_API_KEY` y `AI_BASE_URL` no vacías y un build instalado
When se abre Ajustes
Then el estado es «Configurada», se muestra la nota de que la clave proviene de la compilación, no aparece «Eliminar API key» y la inferencia usa la key y la baseURL del build (no la del almacén del dispositivo)

### Scenario 7: Fallback sin configuración de build (D8/D9)

Given un `local.properties` sin `AI_BASE_URL` ni `AI_API_KEY` (o sin fichero)
When se compila y se abre la app
Then `BuildConfig` contiene cadenas vacías, el build no falla, el endpoint es `https://openrouter.ai/api/v1/images/generations` y Ajustes se comporta exactamente como antes (S1-S5)

### Scenario 8: La baseURL se compone sin duplicar barras (D9)

Given `AI_BASE_URL` con o sin barra final (`https://proxy.local/v1` o `https://proxy.local/v1/`)
When el generador construye el endpoint
Then solicita `https://proxy.local/v1/images/generations` en ambos casos

## Repository Research

### Files Inspected

- `app/build.gradle.kts` — hoy no activa `buildConfig` (`buildFeatures { compose = true }`) y no lee `local.properties`; hay que añadir la generación de campos.
- `local.properties` — solo `sdk.dir`; está en `.gitignore:5` y `init.ps1` solo lo crea si no existe (no lo sobrescribe).
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleImageGenerator.kt` — recibe `endpoint: String = DEFAULT_ENDPOINT` (`https://openrouter.ai/api/v1/images/generations`), obtiene la key de `AiKeyProvider` y devuelve `MissingApiKey`/`Failed`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyProvider.kt`, `AiKeyStore.kt`, `KeystoreAiKeyStore.kt` — puerto de solo lectura, puerto de escritura y almacén cifrado ya implementados en T1-T9.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `single<AiKeyStore>`, `single<AiKeyProvider> { get<AiKeyStore>() }`, `single<ImageGenerator>`, `viewModel { SettingsViewModel(get()) }`.
- `app/src/main/java/com/whatsforlunch/app/ui/settings/{SettingsViewModel,SettingsScreen,SettingsRoute}.kt` — estado `isConfigured/fieldValue/canSave/saved/cleared`, campo enmascarado, borrado con diálogo; el ViewModel consulta `AiKeyStore` directamente.
- `app/src/androidTest/java/com/whatsforlunch/app/SettingsE2ETest.kt` — 4 casos que asumen que el almacén es la única fuente (estado «No configurada» tras `clear()`), por lo que hoy fallarían si el build llevara key.
- `app/src/androidTest/java/com/whatsforlunch/app/KeystoreAiKeyStoreTest.kt`, `app/src/test/java/com/whatsforlunch/app/ui/settings/SettingsViewModelTest.kt` — patrones de test con fake del puerto y lectura literal del XML.
- `docs/technical-discovery.md` (sección de credenciales), `docs/risks-and-open-questions.md` (`S05`, `RK01`), `docs/specs/crear-receta.md` (RF-21), `AGENTS.md` — documentación duradera y líneas rojas.

### Existing Patterns To Follow

- MVVM + StateFlow + Koin; UI sin reglas; `*TestTags`; textos es/en con paridad exacta.
- Adaptadores y puertos en `data/ai` (`AiKeyProvider`, `AiKeyStore`), consumidos desde DI.
- Tests unitarios JUnit con fakes por constructor; instrumentados Compose con `BuildConfig`/Koin reales.
- Gradle con JDK 17 fijado; `local.properties` ya excluido de lint (`lint.xml`).

### Current Gaps

- No hay ningún puente entre ficheros de build y `BuildConfig`; `buildConfig` está desactivado.
- El generador no acepta una baseURL configurable y no existe un resolutor de endpoint.
- Ajustes no conoce la fuente efectiva: mostraría «No configurada» aunque el build aporte key.
- Los E2E de Ajustes dependen de que no haya key de build.

## Technical Approach

1. **Gradle (`app/build.gradle.kts`)**: activar `buildFeatures.buildConfig = true`. Leer la raíz `local.properties` con `java.util.Properties` (si no existe, objeto vacío) y exponer en `defaultConfig`:
   - `buildConfigField("String", "AI_BASE_URL", "\"${escape(props.getProperty("AI_BASE_URL").orEmpty().trim())}\"")`
   - `buildConfigField("String", "AI_API_KEY", "\"${escape(props.getProperty("AI_API_KEY").orEmpty().trim())}\"")`
   donde `escape` sustituye `\` y `"`. Nunca imprimir los valores. Cambiar `local.properties` exige recompilar (`assembleDebug`/`installDebug`), no basta con relanzar la app.
2. **Proveedor con prioridad (`data/ai/PriorityAiKeyProvider.kt`, nuevo)**: `class PriorityAiKeyProvider(private val buildKey: String, private val fallback: AiKeyProvider) : AiKeyProvider` con `apiKey() = buildKey.trim().takeIf { it.isNotEmpty() } ?: fallback.apiKey()`. La key de build nunca se persiste ni se muestra.
3. **Resolutor de endpoint (`data/ai/AiEndpoints.kt`, nuevo)**: `fun imageGeneration(baseUrl: String): String` que usa `DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"` si la base está en blanco y compone `"${base.trimEnd('/')}/images/generations"`. `OpenAiCompatibleImageGenerator` pasa a recibir `baseUrl: String = DEFAULT_BASE_URL` y usa el resolutor; su contrato (`MissingApiKey`/`Failed`/`Success`) no cambia.
4. **DI (`AppModule.kt`)**: `single<AiKeyProvider> { PriorityAiKeyProvider(BuildConfig.AI_API_KEY, get<AiKeyStore>()) }`; `single<ImageGenerator> { OpenAiCompatibleImageGenerator(get(), get(), BuildConfig.AI_BASE_URL) }`; `viewModel { SettingsViewModel(get(), get(), BuildConfig.AI_API_KEY.isNotBlank()) }`.
5. **Ajustes**: `SettingsViewModel(effectiveKeyProvider: AiKeyProvider, aiKeyStore: AiKeyStore, buildKeyActive: Boolean)`. `SettingsUiState` añade `buildKeyActive` y `canDelete` (`!buildKeyActive && aiKeyStore.apiKey() != null`); `isConfigured` se calcula con el proveedor efectivo tras cargar/guardar/borrar. `SettingsScreen` muestra `settings_api_key_build_source` cuando `buildKeyActive` y el botón «Eliminar API key» solo si `canDelete`. El campo, el diálogo y el aviso no cambian.
6. **Tests**: unit de `PriorityAiKeyProvider` (prioridad, vacío, trim), de `AiEndpoints` (defecto, barra final, base personalizada) y de `SettingsViewModel` (key de build activa → configurada, sin borrado; fallback tras borrar). `SettingsE2ETest` debe ser independiente de la configuración local: calcula el estado esperado a partir de `BuildConfig.AI_API_KEY` y usa `Assume.assumeTrue` en los casos que exigen que el almacén sea la única fuente, más un caso específico para la ruta de build.
7. **Documentación duradera**: actualizar `docs/technical-discovery.md` (prioridad y riesgo de APK) y `docs/risks-and-open-questions.md` (`S05`/`RK01` con el riesgo aceptado y la tarea de revertir antes de publicar).

## Expected File Changes

- `app/build.gradle.kts` — modify: `buildConfig`, lectura de `local.properties` y campos `AI_BASE_URL`/`AI_API_KEY`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PriorityAiKeyProvider.kt` — create: prioridad build → almacén.
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiEndpoints.kt` — create: baseURL → endpoint de imágenes.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleImageGenerator.kt` — modify: `baseUrl` en lugar de `endpoint` fijo.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify: bindings con `BuildConfig` y ViewModel con `buildKeyActive`.
- `app/src/main/java/com/whatsforlunch/app/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt` — modify: estado de la fuente efectiva, nota y visibilidad de «Eliminar».
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify: `settings_api_key_build_source` (paridad es/en).
- `app/src/test/java/com/whatsforlunch/app/data/ai/PriorityAiKeyProviderTest.kt`, `AiEndpointsTest.kt` — create.
- `app/src/test/java/com/whatsforlunch/app/ui/settings/SettingsViewModelTest.kt` — modify: casos de build-key activa y `canDelete`.
- `app/src/androidTest/java/com/whatsforlunch/app/SettingsE2ETest.kt` — modify: independencia de `local.properties` y caso de key de build.
- `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` — modify (ver Durable Documentation Impact).

## Visual Design Impact

- UI involved: **yes (mínimo)** — una línea de nota y la visibilidad condicional del botón de borrado en Ajustes.
- Design source: `DESIGN.md` (tokens; Ajustes ya diseñada en T1-T9 con `surface-container`, tipografía y ≥ 48 dp).
- Screens or states affected: Ajustes con clave de build activa (nuevo estado informativo); el resto no cambia.
- New design artifact required: no — la nota usa `bodySmall`/`onSurfaceVariant` como el aviso existente.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — no existe; las fronteras viven en `docs/technical-discovery.md`.
- `CONSTRAINTS.md`: **not needed** — no existe.
- `AGENTS.md`: **not needed** — no cambian comandos, flujo ni reglas del agente.
- `docs/technical-discovery.md`: **update** — documentar `AI_BASE_URL`/`AI_API_KEY` en `local.properties`, la prioridad sobre Ajustes, que se inyectan en debug y release y que la key queda en el APK (riesgo asumido).
- `docs/risks-and-open-questions.md`: **update** — `S05`/`RK01`: nueva fuente de key en build, riesgo de extracción del APK y tarea de retirar la inyección antes de distribuir.
- `DESIGN.md`: **not needed** — la nota reutiliza estilos existentes; sin cambio de sistema visual.
- Otros (`CONTEXT.md`, `docs/domain-model.md`): **not needed** — sin cambios de términos ni reglas de negocio.

## Implementation Plan

1. Añadir la lectura de `local.properties` y los `buildConfigField` en `app/build.gradle.kts`, verificando builds con y sin claves.
2. Crear `PriorityAiKeyProvider` y `AiEndpoints` con sus unit tests.
3. Adaptar `OpenAiCompatibleImageGenerator` a `baseUrl` y actualizar los bindings de Koin.
4. Extender `SettingsViewModel`/`SettingsScreen` (fuente efectiva, nota, borrado condicional) y los textos es/en.
5. Actualizar `SettingsE2ETest` para que no dependa de `local.properties` y cubra la ruta de key de build.
6. Actualizar `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`.
7. Autoverificar con los gates y dejar la feature en `passing` para `feature-validator`.

## Implementation Tasks

- [x] T1-T9: base ya implementada y validada (almacén cifrado, Ajustes, ruta, textos, tests y documentación; ver sesión 54).
- [x] T10: `app/build.gradle.kts` lee `local.properties` (opcional) y expone `BuildConfig.AI_BASE_URL`/`AI_API_KEY` con `""` por defecto, sin imprimir valores y con `buildConfig = true`.
- [x] T11: `PriorityAiKeyProvider` (build con prioridad, trim, fallback) + unit tests.
- [x] T12: `AiEndpoints.imageGeneration` + unit tests (defecto, barra final, base personalizada) y `OpenAiCompatibleImageGenerator` con `baseUrl`.
- [x] T13: DI: proveedor compuesto, generador con `BuildConfig.AI_BASE_URL` y `SettingsViewModel(get(), get(), BuildConfig.AI_API_KEY.isNotBlank())`.
- [x] T14: Ajustes muestra `settings_api_key_build_source` y oculta «Eliminar API key» con key de build; `canDelete` en el estado; unit tests nuevos.
- [x] T15: `SettingsE2ETest` independiente de `local.properties` (estado esperado según `BuildConfig.AI_API_KEY`, `Assume` en los casos que requieren almacén único, caso de key de build).
- [x] T16: actualizar `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` con D7-D11 y el riesgo asumido.

## Verification Plan

- `./init.ps1` → gate de arranque sin cambios (no bloqueante, sin servicios persistentes).
- `./gradlew lintDebug` → 0 errores (los 4 warnings intencionales de `commit()` siguen permitidos).
- `./gradlew testDebugUnitTest --rerun-tasks` → en verde, con los nuevos unit tests y `SettingsViewModelTest`.
- `./gradlew assembleDebug` con `local.properties` **sin** `AI_*` → BUILD SUCCESSFUL; `BuildConfig.AI_BASE_URL`/`AI_API_KEY` vacíos.
- `./gradlew assembleDebug` con `AI_BASE_URL`/`AI_API_KEY` sintéticas y no vacías → BUILD SUCCESSFUL; los valores aparecen en el `BuildConfig` generado (comprobación de que la inyección funciona; la key sintética se retira después).
- `./gradlew connectedDebugAndroidTest` → suite completa en verde en `Pixel_10_Pro` (API 37) **con `local.properties` sin `AI_*`** (ruta normal) y, si se prueba la ruta de build, con `AI_*` sintéticas: en ese caso los casos marcados con `Assume` se omiten de forma explícita y el caso de key de build pasa.
- Comprobación manual con `adb`/uiautomator: con `AI_API_KEY` sintética y `AI_BASE_URL` personalizada, Ajustes muestra «Configurada» + nota de compilación y **no** muestra «Eliminar API key»; sin `AI_*`, Ajustes se comporta como en T1-T9.
- Sin llamadas de pago a la IA en ninguna comprobación (D3); cualquier llamada real exigiría confirmación humana previa (AGENTS.md).
- Comprobar que `git status` no incluye `local.properties` ni claves reales en ningún fichero versionado (`git grep` de la key sintética → 0 coincidencias).

## Evidence To Capture

- Salida de `init.ps1`, `lintDebug`, `testDebugUnitTest --rerun-tasks`, `assembleDebug` (con y sin `AI_*`) y `connectedDebugAndroidTest` (recuento y XML) en `PROGRESS.md`/`feature_list.json`.
- Extracto del `BuildConfig` generado con los campos presentes (sin volcar la key completa en documentos versionados).
- Resultado manual: Ajustes con key de build (nota + sin borrado) y sin key de build (flujo T1-T9).
- `git grep` de la key sintética → 0 coincidencias en el repositorio.
- Nombres de los tests nuevos: `PriorityAiKeyProviderTest`, `AiEndpointsTest`, casos nuevos de `SettingsViewModelTest` y `SettingsE2ETest`.

## Validator Checklist

- [ ] La implementación se limita al alcance (sin UI de baseURL, sin proxy, sin llamadas de red nuevas, sin tocar `init.ps1`).
- [ ] Escenarios 1-8: Ajustes sigue funcionando y la key de build tiene prioridad, con fallback correcto.
- [ ] `local.properties` ausente o sin `AI_*` no rompe el build y deja el comportamiento idéntico al actual.
- [ ] Debug y release reciben los mismos campos (D7) y esto queda documentado junto con el riesgo D11.
- [ ] La key de build no se persiste en `SharedPreferences`, no se muestra en pantalla ni en logs, y no aparece en ficheros versionados.
- [ ] `SettingsE2ETest` pasa con y sin `AI_*` en `local.properties`, sin dependencias ocultas del entorno del desarrollador.
- [ ] Evidencia de gates presente y `feature_list.json`/`PROGRESS.md` actualizados (solo `status`/`evidence`).
- [ ] Documentación duradera actualizada (`technical-discovery`, `risks-and-open-questions`) y sin contradicciones con `⚠ S05`.
