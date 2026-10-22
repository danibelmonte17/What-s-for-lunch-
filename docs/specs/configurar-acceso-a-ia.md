# Feature Implementation Spec: Configurar acceso a IA

## Source Feature

- `id`: configurar-acceso-a-ia
- `area`: seguridad
- `depends_on`: arrancar-app-en-mis-recetas (`accepted`)
- `status`: not_started
- `source`: `feature_list.json`

## Goal

El usuario registra, sustituye y borra su API key de OpenRouter desde una pantalla **Ajustes**, con
aviso de privacidad visible. La key queda **cifrada en el dispositivo** (nunca en claro en disco, en
el repositorio, en logs ni re-mostrada en pantalla — `⚠ S05`, `RK01`) y las funciones de IA existentes
(«Generar con IA» de `crear-receta`) pasan a consumirla sin cambiar su comportamiento: sin key, aviso
que pide configurarla y el formulario sigue usable (RF-21, `Q-crear-receta-6`).

## Non-Goals

- Selector de idioma en Ajustes: es `cambiar-idioma-de-la-interfaz` (esta pantalla solo se estructura para acogerlo después).
- Llamadas de red de ningún tipo: ni validar la key contra OpenRouter (decisión del usuario, 2026-10-22), ni chat, ni sugerencias, ni generación de imágenes nuevas.
- Medición de latencia y coste reales de OpenRouter (tarea de investigación de `RK01`): **aplazada** por decisión del usuario (requiere key real y llamadas de pago confirmadas).
- Configuración «sin retención» en el panel de OpenRouter (`RK03`): se documenta en el aviso, pero su verificación efectiva queda para `sugerir-recetas-con-ia`.
- Cambiar el flujo de «Generar con IA» ni su copy existente (`create_recipe_notice_ai_key_missing`).
- Proxy/backend para ocultar la key (`P34`, fuera del MVP).

## Job Story

When quiero usar las funciones con IA de la app (hoy, generar la foto de una Receta),
I want to registrar mi API key de OpenRouter en Ajustes con un aviso claro de privacidad,
so I can que la app llame al proveedor desde mi dispositivo sin que mi key quede expuesta ni almacenada en claro.

## Users And Permissions

- **Usuario** (único rol; dueño del dispositivo, sin cuentas ni login — `P31`): registra, sustituye y borra su propia API key. Sin roles ni permisos adicionales; la key es credencial del usuario, no de la app (`docs/domain-model.md`, «Roles y acceso»).

## Decisiones fijadas con el usuario (2026-10-22)

- **D1 — Acceso a Ajustes**: acción (icono) en la barra superior de Mis Recetas, junto al icono de Despensa existente; patrón `IconButton` + `testTag` ya usado.
- **D2 — Almacenamiento cifrado**: **Android Keystore directo** (AES/GCM/NoPadding, clave 256 bits del Keystore) con el ciphertext (IV + dato, Base64) en `SharedPreferences` propias. **No** se usa `androidx.security:security-crypto` (deprecado). Cierra la tarea de investigación «Keystore vs EncryptedSharedPreferences».
- **D3 — Validación local únicamente**: recorte de espacios y rechazo de campo vacío; sin comprobaciones de formato ni llamadas de red (el fallo de key real lo cubre RD05 cuando llegue el consumo de IA).
- **D4 — La key guardada nunca se re-muestra**: la UI muestra el estado «Configurada/No configurada» y el campo queda enmascarado (`PasswordVisualTransformation`); sustituir = guardar encima; borrar = diálogo de confirmación (patrón `eliminar-receta`) → limpiar.
- **D5 — Aviso de privacidad (`P27`) siempre visible en Ajustes**: la key solo se usa para llamar a OpenRouter desde el dispositivo; el proveedor se configura sin retención de datos para entrenamiento (`RK03`, verificación pendiente); las fotos del Escaneo nunca salen del dispositivo (RD06); los límites de gasto se controlan en el panel de OpenRouter (`RK01`).
- **D6 — Sin key nada se rompe**: las funciones de IA siguen degradando con aviso y entrada manual (RD05, RF-21); tras borrar la key, «Generar con IA» vuelve a mostrar el aviso de configurarla.

## Acceptance Scenarios

### Scenario 1: Registrar la API key

Given Ajustes sin key configurada
When el usuario escribe una key, la guarda
Then el estado pasa a «Configurada», el campo queda enmascarado y la key persiste cifrada (visible para `AiKeyProvider`, nunca en claro en disco — Scenario 2)

### Scenario 2: La key no queda en claro en disco (S05)

Given una key guardada desde Ajustes
When se inspeccionan los ficheros de datos de la app (`shared_prefs/`, Room, logs)
Then no aparece la key en claro en ninguno: el valor persistido es ciphertext AES-GCM y el texto plano solo existe en memoria al consumirla

### Scenario 3: Borrar la key pide configurarla (verify 2)

Given una key configurada
When el usuario pulsa «Eliminar», confirma y después usa «Generar con IA» en el Formulario
Then la key queda eliminada (estado «No configurada») y «Generar con IA» muestra el aviso «Configura tu API key de IA en Ajustes…» sin bloquear el formulario

### Scenario 4: Aviso de privacidad (P27)

Given la pantalla Ajustes en español o en inglés
When se abre en cualquier estado (con o sin key)
Then se muestra el aviso de privacidad (D5) con los textos `settings_privacy_*` de `values/` y `values-en/`

### Scenario 5: Persistencia y campo protegido

Given una key guardada
When se cierra y reabre la actividad (reinicio de la app) o se vuelve a Ajustes
Then el estado sigue siendo «Configurada» y el campo nunca muestra la key en claro (enmascarado); el campo vacío no permite guardar (D3/D4)

## Repository Research

### Files Inspected

- `feature_list.json`, `PROGRESS.md`, `AGENTS.md` — feature, estado y reglas de trabajo.
- `docs/build-brief.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/domain-model.md`, `CONTEXT.md`, `DESIGN.md` — `⚠ S05`, `RK01`/`RK03`/`RK04`, `P27`, tareas de investigación, roles, tokens y pantallas.
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyProvider.kt` — interfaz actual (`apiKey(): String?`) y `UnconfiguredAiKeyProvider` (siempre `null`), marcada para sustituir aquí.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleImageGenerator.kt` — consumidor de `AiKeyProvider` (Bearer header, `MissingApiKey` sin key, `Failed` con error — RD05); no debe cambiar.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin: `single<AiKeyProvider> { UnconfiguredAiKeyProvider() }` a sustituir.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` y `ui/nav/ProvisionalScreen.kt` — patrón de rutas y destinos; no existe ruta de Ajustes.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` / `MyRecipesRoute.kt` — barra superior con `IconButton` de Despensa (`MyRecipesTestTags.OPEN_PANTRY`): patrón del nuevo acceso a Ajustes.
- `app/src/main/java/com/whatsforlunch/app/ui/createrecipe/CreateRecipeViewModel.kt` y `res/values*/strings.xml` — aviso `create_recipe_notice_ai_key_missing` (RF-21) que debe seguir apareciendo sin key.
- `app/build.gradle.kts` — dependencias actuales (sin `security-crypto`; no hace falta añadir ninguna: Keystore es API del sistema).
- `app/src/androidTest/.../E2eDatabase.kt`, `CreateRecipeE2ETest.kt`, `ScanPantryE2ETest.kt` — patrones de E2E: fakes por constructor, `GlobalContext.get()` para resolver singletons reales, `E2eDatabase.clear()`.

### Existing Patterns To Follow

- MVVM + StateFlow + Koin; UI sin reglas de negocio; `*TestTags` object por pantalla; textos es/en con las mismas claves en `values/` y `values-en/`.
- Diálogo de confirmación destructiva con «Cancelar»/«Eliminar» (tinte error) y descarte = cancelar (`eliminar-receta`, `Q-eliminar-receta-1/2`).
- Accesibilidad: objetivos táctiles ≥ 48 dp, `contentDescription` en iconos (`DESIGN.md`).
- E2E instrumentado Compose sobre `Pixel_10_Pro` (API 37) con `connectedDebugAndroidTest`; verificación manual con `adb`/uiautomator y capturas en español.

### Current Gaps

- No existe pantalla, ruta ni punto de acceso de Ajustes; no hay textos `settings_*`.
- No hay persistencia de la key ni mecanismo de cifrado; `AiKeyProvider` solo tiene la implementación provisional.
- La interfaz `AiKeyProvider` no contempla guardado/borrado (falta un puerto de escritura).
- Sin cobertura de «cifrado en reposo»: nadie comprueba hoy que la key no acabe en claro en disco.

## Technical Approach

1. **Puerto y almacén (data/ai)**: nuevo `interface AiKeyStore : AiKeyProvider { fun save(key: String); fun clear() }` y su implementación `KeystoreAiKeyStore(context)`:
   - clave simétrica AES-256 generada en `AndroidKeyStore` (`KeyGenParameterSpec`, `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `GCM`), alias `whatsforlunch_ai_key`;
   - cada guardado genera IV nuevo (GCM 12 bytes) y persiste `Base64(iv || ciphertext)` en `SharedPreferences` `ai_key_prefs`, clave `api_key` (D2);
   - `apiKey()` descifra en memoria y devuelve `null` si no hay nada o si el descifrado falla (borrado/corrupción → estado «No configurada», sin crash);
   - `clear()` borra la entrada; nunca se registra la key en `Log` ni en excepciones.
2. **DI (AppModule)**: `single<AiKeyStore> { KeystoreAiKeyStore(androidContext()) }` y `single<AiKeyProvider> { get<AiKeyStore>() }` (sustituye `UnconfiguredAiKeyProvider`, que se elimina); `ImageGenerator` sigue igual.
3. **Pantalla Ajustes (ui/settings/)**: `SettingsRoute` → `SettingsViewModel` (StateFlow con `isConfigured`, `fieldValue`, `canSave`, evento `saved`/`cleared`) → `SettingsScreen`. Campo de key enmascarado (D4), botones «Guardar» (deshabilitado con campo vacío tras recorte, D3) y «Eliminar» con confirmación, estado «Configurada/No configurada», aviso de privacidad (D5) y `SettingsTestTags`. La UI no contiene reglas: trim y validación mínima en el ViewModel sobre el puerto.
4. **Navegación y acceso (D1)**: `AppRoutes.SETTINGS = "settings"` en `AppNavHost` con `SettingsRoute(onBack = popBackStack)`; nuevo `onOpenSettings` en `MyRecipesRoute`/`MyRecipesScreen` como `IconButton` en la barra superior junto a Despensa (mismo patrón y ≥ 48 dp).
5. **Textos es/en**: claves `settings_title`, `settings_api_key_label`, `settings_api_key_hint`, `settings_api_key_save`, `settings_api_key_delete`, `settings_api_key_delete_title`, `settings_api_key_delete_message`, `settings_api_key_delete_confirm`, `settings_api_key_delete_cancel`, `settings_api_key_state_configured`, `settings_api_key_state_missing`, `settings_api_key_saved`, `settings_open_accessibility`, `settings_privacy_title`, `settings_privacy_notice` (copy de D5 propuesto por la spec; se valida como el resto de copy, `Q-arrancar-app-en-mis-recetas-5`).
6. **Degradación intacta (D6)**: sin cambios en `CreateRecipeViewModel` ni en sus avisos; el E2E comprueba que el aviso reaparece tras borrar la key.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyProvider.kt` — modify: añadir `AiKeyStore` (o crear `data/ai/AiKeyStore.kt`) y retirar `UnconfiguredAiKeyProvider`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/KeystoreAiKeyStore.kt` — create: almacén cifrado Android Keystore (D2).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify: bindings de `AiKeyStore`/`AiKeyProvider`.
- `app/src/main/java/com/whatsforlunch/app/ui/settings/SettingsScreen.kt`, `SettingsRoute.kt`, `SettingsViewModel.kt` — create: pantalla Ajustes.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modify: ruta `settings`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt`, `MyRecipesRoute.kt` — modify: acción `onOpenSettings` en la barra superior.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify: claves `settings_*` (paridad es/en).
- `app/src/test/java/com/whatsforlunch/app/ui/settings/SettingsViewModelTest.kt` — create: unit del ViewModel con `AiKeyStore` falso.
- `app/src/androidTest/java/com/whatsforlunch/app/SettingsE2ETest.kt` — create: flujo de Ajustes (escenarios 1, 3, 4, 5).
- `app/src/androidTest/java/com/whatsforlunch/app/KeystoreAiKeyStoreTest.kt` — create: cifrado en reposo y round-trip real en dispositivo (escenario 2).
- `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` — modify (ver Durable Documentation Impact).

## Visual Design Impact

- UI involved: **yes** (nueva pantalla Ajustes + acción en la barra de Mis Recetas).
- Design source: `DESIGN.md` (tokens del front-matter de `uxdocs/DESIGN.md`, `P17`); **sin referencia visual** de Ajustes (`RK04`): se diseña siguiendo los tokens, como declara `DESIGN.md` («Preguntas de diseño abiertas»).
- Screens or states affected: Ajustes (nueva: sin key / con key / diálogo de borrado / aviso de privacidad), Mis Recetas (solo el icono de acceso en la barra; nada más cambia).
- New design artifact required: no — basta el copy de la spec validado en revisión; sin conceptos generados.
- Pautas: superficies cálidas y `surface-container`, botón primario terracota (52 dp) para «Guardar», «Eliminar» con tinte error, inputs con anillo de foco terracota y estado deshabilitado distinguible, objetivos ≥ 48 dp, WCAG AA.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — no existe en este repo; las fronteras de capas y adapters viven en `docs/technical-discovery.md` (que sí se actualiza).
- `CONSTRAINTS.md`: **not needed** — no existe; la regla duradera «la API key nunca en claro ni en el repositorio» ya está en `docs/technical-discovery.md` y `docs/build-brief.md` (`⚠ S05`).
- `AGENTS.md`: **not needed** — no cambia el flujo del agente, los comandos ni las convenciones.
- `docs/technical-discovery.md`: **update** — sustituir la entrada «Credenciales: Keystore/EncryptedSharedPreferences — Candidato» por la decisión D2 (Android Keystore AES-GCM + SharedPreferences, sin dependencias nuevas) y anotar que la latencia/coste de OpenRouter queda aplazada.
- `docs/risks-and-open-questions.md`: **update** — cerrar la tarea «Almacenamiento cifrado de la API key (Keystore vs EncryptedSharedPreferences)» (resuelta: Keystore directo, 2026-10-22) y marcar «Latencia y coste reales de OpenRouter» como aplazada a petición del usuario (requiere key real y llamadas de pago); `RK01` pasa a tener su mitigación «almacenamiento cifrado + aviso en Ajustes» implementada.
- `DESIGN.md`: **update (menor)** — opcional: reflejar en «Preguntas de diseño abiertas» que Ajustes ya tiene diseño propio con los tokens (se resuelve en la revisión visual).
- Otros (`CONTEXT.md`, `docs/domain-model.md`): **not needed** — sin cambios de términos ni de reglas de negocio.

## Implementation Plan

1. Implementar `AiKeyStore` + `KeystoreAiKeyStore` (D2) con su test instrumentado de cifrado en reposo y round-trip.
2. Sustituir el binding de Koin y comprobar que `OpenAiCompatibleImageGenerator` consume la key real sin cambios (`MissingApiKey` sin key).
3. Construir `ui/settings/` (ViewModel + Screen + Route) con textos es/en, y añadir la ruta en `AppNavHost`.
4. Añadir el icono de Ajustes a la barra de Mis Recetas (D1) y sus testTags.
5. Escribir unit (`SettingsViewModelTest`) y E2E (`SettingsE2ETest`) de los escenarios 1, 3, 4 y 5.
6. Actualizar la documentación duradera (`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`) y capturar evidencia.

## Implementation Tasks

- [ ] T1: `AiKeyStore` (save/clear/apiKey) + `KeystoreAiKeyStore` con alias `whatsforlunch_ai_key`, AES-GCM y `SharedPreferences` `ai_key_prefs` (D2), sin logs de la key.
- [ ] T2: `KeystoreAiKeyStoreTest` (androidTest): guardado → la key en claro NO aparece en `shared_prefs/ai_key_prefs.xml` (lectura literal del fichero); `apiKey()` devuelve la key original; `clear()` → `null`; app con datos corruptos → `null` sin crash.
- [ ] T3: `AppModule`: `AiKeyStore` real en Koin, `AiKeyProvider = get<AiKeyStore>()`; retirar `UnconfiguredAiKeyProvider`.
- [ ] T4: `SettingsViewModel` + `SettingsScreen` + `SettingsRoute`: campo enmascarado, «Guardar» (deshabilitado vacío), «Eliminar» con confirmación, estados «Configurada/No configurada», aviso de privacidad (D5) y `SettingsTestTags`.
- [ ] T5: claves `settings_*` en `values/strings.xml` y `values-en/strings.xml` (paridad exacta de claves).
- [ ] T6: ruta `settings` en `AppNavHost` + `onOpenSettings` en la barra de Mis Recetas junto a Despensa (D1, ≥ 48 dp, `contentDescription`).
- [ ] T7: `SettingsViewModelTest` (unit): trim, guardar vacío rechazado, guardar → configurada, eliminar → no configurada, persistencia del estado entre cargas.
- [ ] T8: `SettingsE2ETest`: escenarios 1, 3 (incluido el aviso de `create_recipe_notice_ai_key_missing` tras borrar), 4 (es/en) y 5 (reinicio de actividad + campo enmascarado + guardar vacío deshabilitado).
- [ ] T9: actualizar `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` (decisiones D2/D3 y tarea aplazada) y recopilar evidencia.

## Verification Plan

- `./init.sh` (o `./init.ps1`): gate de arranque **sin cambios** por esta feature — solo comprobaciones no bloqueantes, sin arrancar servicios persistentes.
- `./gradlew lintDebug` → 0 errores.
- `./gradlew testDebugUnitTest` (o `./gradlew testDebugUnitTest --rerun-tasks`) → en verde, incluido `SettingsViewModelTest`.
- `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew connectedDebugAndroidTest` → suite completa en verde en `Pixel_10_Pro` (API 37) + focalizadas `SettingsE2ETest` y `KeystoreAiKeyStoreTest`. Notas de entorno: `e2e.ps1` desinstala la app al terminar (usar `connectedDebugAndroidTest` y reinstalar con `./gradlew installDebug`); suites largas pueden abortar por el emulador compartido (reintentar tras reiniciar el AVD).
- **E2E obligatorio** (feature con comportamiento visible y flujo de credenciales): `SettingsE2ETest` cubre los dos `verify` de `feature_list.json` — (1) guardar una key de prueba y comprobar que queda guardada **cifrada** (aserción de ciphertext en reposo en `KeystoreAiKeyStoreTest` + estado «Configurada» en UI) y (2) borrarla y comprobar que «Generar con IA» pide configurarla (aviso `create_recipe_notice_ai_key_missing`, sin llamadas de red).
- Comprobación manual con `adb`/uiautomator: apertura de Ajustes desde Mis Recetas, aviso de privacidad visible en es y en, campo enmascarado, y ausencia de la key en claro en los ficheros de la app (barrido de `shared_prefs/`, `databases/` y logcat).
- Sin llamadas de pago a la IA en ninguna comprobación (D3); si se necesitaran, exigir confirmación humana previa (AGENTS.md).

## Evidence To Capture

- Salida de `init.ps1`, `lintDebug`, `testDebugUnitTest --rerun-tasks`, `assembleDebug` y `connectedDebugAndroidTest` (recuento y XML) en `PROGRESS.md`/`feature_list.json`.
- Resultado del barrido «sin key en claro en disco» (rutas inspeccionadas y resultado) en `docs/evidence/configurar-acceso-a-ia/`.
- 2 capturas en español (`docs/evidence/configurar-acceso-a-ia/`): Ajustes con key configurada y aviso de privacidad; Ajustes sin key (o el diálogo de confirmación de borrado).
- Nombres de los tests nuevos en verde: `SettingsViewModelTest`, `KeystoreAiKeyStoreTest`, `SettingsE2ETest` (escenarios 1-5).

## Validator Checklist

- [ ] Implementation stays within this feature's scope (sin selector de idioma, sin llamadas a la IA, sin cambios en el flujo de «Generar con IA»).
- [ ] Acceptance scenarios pass (1-5), incluido el escenario 2 de cifrado en reposo.
- [ ] Verification evidence is present (salidas de comandos, barrido de disco y capturas es).
- [ ] Persistent E2E coverage added (`SettingsE2ETest` + `KeystoreAiKeyStoreTest`) covering both `verify` of `feature_list.json`.
- [ ] `feature_list.json` and `PROGRESS.md` were updated correctly (solo `status` y `evidence`).
- [ ] No unrelated product behavior or extra feature work was added.
- [ ] Seguridad: la API key no aparece en claro en disco, en logs, en capturas ni en el repositorio (`⚠ S05`, `RK01`); sin dependencias ni permisos nuevos; sin secretos escritos en el repo.
- [ ] La documentación duradera marcada «update» quedó actualizada en el mismo cambio.
