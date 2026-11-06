# Feature Implementation Spec: Cambiar el idioma de la interfaz

## Source Feature

- `id`: cambiar-idioma-de-la-interfaz
- `area`: funcional
- `depends_on`: configurar-acceso-a-ia (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

El usuario cambia el idioma de la interfaz entre **español e inglés** desde un selector en **Ajustes** y todos los textos de la app cambian **sin reiniciar** y en todas las pantallas. La elección **persiste** entre reinicios de la app (decisión del usuario, D1): el idioma del sistema solo se usa en el primer arranque o con los datos de la app borrados.

El idioma activo manda sobre todo lo localizable: textos y plurals (`strings.xml`), formato numérico de cantidades, idioma de los prompts de IA y locale de la voz del Modo Cocina (RD24). Todo eso ya se deriva hoy de `LocalConfiguration` en las Routes, así que cambiar el idioma de la app los actualiza sin tocar esos consumidores.

## Non-Goals

- **Más idiomas**: la app sigue siendo solo es/en (`P19`); sin `values-xx` nuevas ni selector de otros idiomas.
- **Opción «Seguir el idioma del sistema»** (D2): el selector tiene solo dos opciones; el sistema solo decide la preselección inicial.
- **Integración con el ajuste de idioma por aplicación del sistema** (`android:localeConfig`/Ajustes de Android): la superficie es el selector de la app; el per-app language del sistema queda fuera.
- **Traducir contenido del usuario**: Recetas, Alimentos, ítems de la Lista, fotos y conversaciones nunca se traducen (D6).
- **Cambiar prompts, vocabulario de voz o reglas de negocio**: `assets/prompts/*_{es,en}.txt`, `VoiceCommandParser` (RD24), `RecipeValidation`, RD01-RD24 y Room quedan intactos; solo cambia qué idioma se elige.
- **Reiniciar la actividad ni depender de AppCompat**: sin `Activity.recreate()` como mecanismo de cambio ni nuevas dependencias de appcompat (D4).
- **Traducciones de la Lista de la Compra, Ajustes de API key ni privacidad más allá de los textos nuevos de esta feature**.

## Job Story

When quiero usar la app en otro idioma (o compartirla con alguien que prefiere inglés),
I want to cambiar el idioma de la interfaz desde Ajustes,
so I can ver todos los textos en mi idioma al momento, sin reiniciar la app ni cambiar el idioma de mi dispositivo.

## Users And Permissions

- Usuario único sin cuentas ni roles: puede cambiar el idioma tantas veces como quiera desde Ajustes; el cambio es inmediato, reversible y persistente.

## Requisitos funcionales

- **RF-IDI1**: Ajustes muestra la sección «Idioma» con dos opciones, «Español» y «English» (endónimos, D3), y una de ellas marcada como activa.
- **RF-IDI2**: tocar una opción aplica el idioma **al momento** en toda la app visible (textos, plurals y formatos) sin reiniciar la app ni perder el estado de navegación o de los formularios abiertos.
- **RF-IDI3**: la elección persiste en el dispositivo y sobrevive a cerrar y reabrir la app; sin elección guardada se preselecciona el idioma del sistema («English» si el sistema es inglés, si no «Español», D2).
- **RF-IDI4**: el idioma activo gobierna también el idioma de los prompts de IA (Sugerencias, Escáner, foto del plato, Pinche), el locale de voz del Modo Cocina (TTS + `SpeechRecognizer`, RD24) y el formato de las cantidades, derivándose de la misma fuente que hoy (`LocalConfiguration`).
- **RF-IDI5**: todos los textos nuevos tienen paridad es/en en `values/` y `values-en/` (incluidos plurals si los hay).

## Acceptance Scenarios

### Scenario 1: cambiar a inglés sin reiniciar (verify 1)

Given la app está en español y abro Ajustes
When toco la opción «English» de la sección «Idioma»
Then los textos de Ajustes cambian al inglés al momento y, al volver a Mis Recetas y a otras pantallas, todos sus textos siguen en inglés, sin reiniciar la app.

### Scenario 2: volver a español

Given la app está en inglés
When toco la opción «Español»
Then toda la interfaz vuelve al español al momento, incluidas las pantallas del back stack.

### Scenario 3: la elección persiste

Given he elegido «English» en Ajustes
When cierro la app por completo (force-stop) y la vuelvo a abrir
Then la interfaz arranca en inglés y Ajustes muestra «English» marcado.

### Scenario 4: primer arranque sin elección guardada

Given no hay idioma guardado (primer arranque o datos de la app borrados)
When abro la app con el dispositivo en inglés (o en español)
Then la app muestra la interfaz en inglés (o en español) y Ajustes preselecciona esa misma opción.

### Scenario 5: el idioma de la app manda sobre IA y voz

Given la app está en inglés
When pido Sugerencias con IA (cliente simulado) y activo la voz en el Modo Cocina
Then la petición al cliente de IA lleva `language = "en"` y el locale de voz derivado es inglés (y al volver a español, `language = "es"` y locale es-ES), sin tocar los prompts ni el vocabulario de comandos.

## Casos límite

| Caso | Comportamiento |
|---|---|
| Sistema en un idioma distinto de es/en | Sin elección guardada se preselecciona «Español» (el fallback de `values/`) |
| Cambiar de idioma con un formulario a medio escribir | No se pierde nada: el estado vive en los ViewModels y solo cambia la resolución de textos |
| Cambiar de idioma con el Modo Cocina en el back stack | Las pantallas recomponen con el idioma nuevo; una sesión de voz ya iniciada conserva el locale capturado al activar «Voz» hasta reactivarse (en la práctica no conviven: salir del Modo detiene la voz) |
| Cantidades y decimales | Formato del idioma activo (`formatQuantity` con el locale de la app: «0,5» en es, «0.5» en en) |
| Borrar los datos de la app | Se pierde la elección: vuelve a seguir el idioma del sistema |
| Seleccionar la opción ya activa | Sin cambios ni errores (idempotente) |
| Textos con plurals y a11y (content-desc, `liveRegion`) | Cambian con el resto; verificados por paridad de claves |
| Lecturas de recursos fuera de composición | Prohibidas: los strings se leen en composables (`stringResource`) o desde el contexto localizado; los consumidores actuales (`LocalConfiguration` en Routes) ya cumplen |

## Repository Research

### Files Inspected

- `feature_list.json` (entrada: 2 steps + verify 1), `PROGRESS.md`, `CONTEXT.md` (glosario, `P19` es/en), `docs/build-brief.md` («Interfaz en español e inglés» en el MVP), `DESIGN.md` (Ajustes: «sin referencia visual → diseñar siguiendo los tokens (RK04)»), `docs/risks-and-open-questions.md` (sin `⚠` propios de idioma; `⚠ S04` y `⚠ S05` no se alteran), `docs/technical-discovery.md` (comandos, prompts en assets, voz).
- `app/src/main/java/com/whatsforlunch/app/MainActivity.kt` — `ComponentActivity` sin AppCompat: no hay `AppCompatDelegate.setApplicationLocales`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `WhatsForLunchApp` es la raíz componible con el `NavHost`: punto donde inyectar el contexto localizado.
- `app/src/main/java/com/whatsforlunch/app/ui/settings/SettingsScreen.kt` (`SettingsTestTags`, tarjetas «API key» y «Privacidad y datos»), `SettingsViewModel.kt` (MVVM + `StateFlow`), `SettingsRoute.kt` (`koinViewModel`).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — registros Koin (`single` para stores, `viewModel` para pantallas).
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiKeyStore.kt` + `KeystoreAiKeyStore.kt` — patrón de persistencia local (SharedPreferences `ai_key_prefs`) a imitar para la preferencia de idioma.
- Routes que derivan el idioma de la composición: `AiSuggestionsRoute.kt`, `CreateRecipeRoute.kt`, `ScannerRoute.kt`, `PincheRoute.kt` (`LocalConfiguration.current.locales[0].language`) y `CookModeRoute.kt` (`voiceLocale(...)`: `en` → `Locale.ENGLISH`, si no `es-ES`); `RecipeDetailScreen.kt` (`formatQuantity(quantity, locale)`), `ShoppingListScreen.kt`, `PincheScreen.kt` (mismo patrón).
- `app/src/androidTest/.../AiSuggestionsE2ETest.kt`, `CookModeE2ETest.kt`, `CookModeTimerE2ETest.kt` — patrón de contexto localizado con `createConfigurationContext` + `LocalContext`/`LocalConfiguration` (base del mecanismo de producción, D4) y textos que ya se prueban en ambos idiomas; `MyRecipesLocalizationE2ETest.kt` y `PantryLocalizationE2ETest.kt` (localización); `SettingsE2ETest.kt` (convenciones E2E de Ajustes, `E2eDatabase.clear()`, fakes sin IA ni red).
- `app/build.gradle.kts` — `minSdk 30`, `targetSdk/compileSdk 35`, sin appcompat ni datastore; Koin, Compose, Room.
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — 16 claves `settings_*` con paridad; no existe ninguna clave de idioma.

### Patterns Found

- MVVM + `StateFlow` por pantalla, puertos en `domain/`/`data/` con implementación Room/SharedPreferences/Keystore e `InMemory` para tests, cableado Koin en `AppModule`.
- TestTags estables en objetos `*TestTags` por pantalla; los E2E usan testTags para ser independientes del idioma y comprueban la localización montando la UI con un contexto de locale concreto.
- Paridad de claves es/en como gate (recuento `values/` vs `values-en/`); textos visibles solo en `strings.xml` (`P25`).
- Enum de dominio con identificadores en inglés y etiquetas visibles en `strings.xml` (`MealMoment`, `Aisle`).

### Gaps / Unknowns

- No existe ningún mecanismo de idioma in-app: la app sigue hoy el locale del sistema sin preferencia persistida.
- No hay prueba unitaria de paridad de claves: la paridad se comprueba como gate (recuento), no con test; se mantiene el gate.
- La persistencia de la preferencia se probará con SharedPreferences reales en E2E (reinicio del proceso manual) e `InMemory` en unit; no se añade Room.

## Technical Approach

**Dominio** — `domain/model/AppLanguage.kt`: enum `AppLanguage { SPANISH, ENGLISH }` (identificadores en inglés) con `locale`/`languageTag` (`es-ES`/`en`) y la función pura `fromSystem(locale)` que mapea el locale del sistema a la preselección (`en` → `ENGLISH`, cualquier otro → `SPANISH`). Es la única regla nueva y es unit-testable sin Android.

**Persistencia** — puerto `data/settings/LanguagePreferenceStore.kt` (`selected(): AppLanguage?`, `select(AppLanguage)`) con implementación `SharedPrefsLanguagePreferenceStore` sobre SharedPreferences (patrón de `KeystoreAiKeyStore`, preferencia `language_prefs`, valor = nombre del enum). Sin Room, sin migración y sin secretos: el idioma no es dato sensible.

**Estado y aplicación del locale** — `data/settings/AppLocaleController.kt`: singleton Koin con `StateFlow<AppLanguage>` inicializado con `store.selected() ?: AppLanguage.fromSystem(...)` y `select(language)` que persiste y emite. `WhatsForLunchApp` colecta ese flujo y envuelve el `NavHost` con `CompositionLocalProvider(LocalContext provides contextoLocalizado, LocalConfiguration provides configuraciónLocalizada)` construidos con `createConfigurationContext(Configuration(...).apply { setLocale(locale) })` — el mismo patrón que ya usan los E2E de localización. Así **toda** la UI recompone con el idioma nuevo sin reiniciar: `stringResource`, los `LocalConfiguration.current.locales[0].language` de las Routes (prompts de IA y locale de voz, RF-IDI4) y los `formatQuantity(…, locale)` siguen funcionando sin cambios.

**Ajustes** — `SettingsViewModel` añade `language: AppLanguage` al `UiState` y `onLanguageSelected(AppLanguage)` que delega en el controller; `SettingsScreen` añade una tarjeta «Idioma» (patrón de las tarjetas existentes) con las dos opciones como filas/chips seleccionables de ≥ 48 dp, la activa marcada, y testTags nuevos (`LANGUAGE_SECTION`, `LANGUAGE_OPTION_ES`, `LANGUAGE_OPTION_EN`) sin renombrar los existentes. Las opciones son **endónimos** (D3): «Español» y «English» con el mismo texto en ambos idiomas.

### Decisiones

- **D1** (usuario 2026-11-06): la selección **persiste** entre reinicios de la app; el idioma del sistema solo decide la preselección del primer arranque.
- **D2** (usuario 2026-11-06): el selector tiene **solo dos opciones** («Español»/«English»), según el step del `feature_list.json`; sin opción «Seguir el sistema». Sin preferencia guardada se preselecciona el idioma del sistema (`en` → «English», si no «Español»).
- **D3** (derivada): las opciones se muestran como **endónimos** idénticos en ambos idiomas, para no traducir el nombre de los idiomas.
- **D4** (derivada del verify 1 «sin reiniciar» y del stack): el cambio se aplica **reproveyendo el contexto localizado en la raíz de Compose** (patrón de los E2E de localización), sin `Activity.recreate()` ni AppCompat.
- **D5** (derivada): el idioma de la app manda sobre prompts de IA, locale de voz y formatos numéricos porque todos leen `LocalConfiguration`; **no** se cambian prompts, `VoiceCommandParser` ni su vocabulario (RD24 intacta).
- **D6** (derivada): el contenido del usuario (Recetas, Despensa, Lista, conversaciones) nunca se traduce.

### Desconocidos

- Ninguno bloqueante. La traducción literal de los textos nuevos al inglés la fija el implementer siguiendo el glosario de `CONTEXT.md` y la revisa el validador (`RK04`, mismo criterio que features previas).

### Hallazgos de implementación (2026-11-06)

- **El proveedor de contexto localizado vive en `MainActivity`, no dentro de `WhatsForLunchApp`**: los E2E de localización montan `WhatsForLunchApp()` con su propio `CompositionLocalProvider` (sin el host), de modo que el mecanismo de producción se implementa como `ui/locale/AppLocaleHost.kt`, composable público que envuelve `WhatsForLunchApp()` en `MainActivity` (decisión menor del implementer prevista más abajo). `AppLocaleHost` reprovee `LocalConfiguration` y `LocalResources` (no `LocalContext`, que sigue siendo el Activity real por `PhotoAcquirer`).
- **RF-IDI4 exigía tocar un consumidor**: `AndroidStepSpeaker` resolvía los textos de la locución («Paso N. {texto}», «Temporizador de N minutos») con el contexto inyectado, que sigue el idioma del sistema. Ahora los resuelve con un contexto localizado con el locale de voz preparado, sin tocar `StepSpeaker` ni `VoiceCommandParser`.
- **El cambio de locale en runtime exige desactivar los splits por idioma del AAB**: `app/build.gradle.kts` añade `bundle { language { enableSplit = false } }` — resuelve el warning de lint `AppBundleLocaleChanges` (para sostener la línea base de 49 warnings) y es además lo correcto para una app que cambia de idioma sin reinicio bajo AAB, donde los recursos de un locale con split no instalado no estarían disponibles.

## Expected File Changes

**Nuevos**
- `app/src/main/java/com/whatsforlunch/app/domain/model/AppLanguage.kt` (enum + `fromSystem`)
- `app/src/main/java/com/whatsforlunch/app/data/settings/LanguagePreferenceStore.kt` (puerto)
- `app/src/main/java/com/whatsforlunch/app/data/settings/SharedPrefsLanguagePreferenceStore.kt`
- `app/src/main/java/com/whatsforlunch/app/data/settings/AppLocaleController.kt`
- `app/src/main/java/com/whatsforlunch/app/ui/locale/AppLocaleHost.kt` (proveedor de contexto localizado en la raíz de Compose)
- `app/src/test/java/com/whatsforlunch/app/domain/model/AppLanguageTest.kt`, `app/src/test/java/com/whatsforlunch/app/data/settings/AppLocaleControllerTest.kt`
- `app/src/androidTest/java/com/whatsforlunch/app/LanguageSwitchE2ETest.kt` (S1-S5)

**Modificados**
- `app/src/main/java/com/whatsforlunch/app/MainActivity.kt` (envuelve `WhatsForLunchApp()` con `AppLocaleHost`; en lugar de `ui/nav/AppNavHost.kt`, por el hallazgo de los E2E de localización)
- `app/src/main/java/com/whatsforlunch/app/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `SettingsRoute.kt` (sección «Idioma»)
- `app/src/main/java/com/whatsforlunch/app/data/voice/AndroidStepSpeaker.kt` (hallazgo RF-IDI4: textos de la locución con el locale de voz)
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` (`single` del store y del controller)
- `app/build.gradle.kts` (`bundle { language { enableSplit = false } }`: desactiva los splits por idioma del AAB, por el hallazgo de `AppBundleLocaleChanges`)
- `app/src/main/res/values/strings.xml` y `app/src/main/res/values-en/strings.xml` (claves `settings_language_*`, paridad es/en)
- `app/src/test/java/com/whatsforlunch/app/ui/settings/SettingsViewModelTest.kt` (estado del idioma + `onLanguageSelected`)
- `app/src/androidTest/java/com/whatsforlunch/app/SettingsE2ETest.kt` sin cambios (la tarjeta nueva no obliga a ajustar aserciones)

`data/local/` y Room **sin cambios** (sin migración); prompts en `assets/` **sin cambios**.

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` (Ajustes sin referencia visual: seguir los tokens `Warm Culinary Craft`, RK04).
- Pantallas/estados afectados: «Ajustes» (nueva tarjeta «Idioma»); el resto de pantallas solo cambia de idioma, no de layout.
- Nuevo artefacto de diseño: no — se aplican los tokens existentes.
- **Tarjeta «Idioma»** (patrón de las tarjetas de Ajustes): título «Idioma» + dos opciones seleccionables tipo fila con marca de selección (radio) o `FilterChip`, ≥ 48 dp, seleccionable una; la activa resaltada con `secondary`/`secondaryContainer` según los tokens. Se coloca **encima** de «Privacidad y datos» y debajo de la tarjeta de la API key.
- **Contrato visual verificable**: en español, «Idioma» muestra «Español» marcado; al tocar «English», el título de la tarjeta pasa a «Language» y «English» queda marcado, sin parpadeos ni reinicio.

## Durable Documentation Impact

- **Update** `DESIGN.md`: la sección de Ajustes gana «Idioma» (selector es/en con persistencia; el resto de pantallas solo cambia de idioma).
- **Update** `docs/technical-discovery.md`: fila/mecanismo de idioma (`AppLanguage`, `LanguagePreferenceStore` en SharedPreferences, `AppLocaleController` + contexto localizado en la raíz de Compose; sin AppCompat, sin `localesConfig`) y su verificación. Comandos sin cambios.
- **Update** `docs/risks-and-open-questions.md`: registrar `Q-cambiar-idioma-de-la-interfaz-1..2` (persistencia; solo dos opciones sin «Seguir el sistema») resueltas por el usuario el 2026-11-06 (origen «planificación de la spec»).
- **Not needed** `CONTEXT.md`: sin términos nuevos («Interfaz en español e inglés» ya está en `docs/build-brief.md` y `P19`).
- **Not needed** `AGENTS.md` ni `docs/build-brief.md`: convenciones y alcance MVP intactos (esta feature ya estaba en «Qué construimos»).
- **Not needed** `docs/domain-model.md`: sin reglas de negocio ni datos nuevos.
- **Not needed** `ARCHITECTURE.md` / `CONSTRAINTS.md`: no existen en este repo y esta feature no crea dominios ni reglas MUST nuevas (el mecanismo queda en `docs/technical-discovery.md`).
- `PROGRESS.md` + `docs/progress-history.md`: al cerrar la implementación (labor del harness, no de esta planificación).

## Implementation Plan

1. **Dominio y persistencia**: `AppLanguage` (+ `fromSystem`), `LanguagePreferenceStore` y su implementación SharedPreferences, `AppLocaleController` con sus tests unitarios (T1–T2).
2. **Aplicación del locale en la raíz**: proveedor de contexto localizado en `WhatsForLunchApp` sobre el `StateFlow` del controller; verificación manual rápida de que la UI cambia forzando el estado (T3).
3. **Ajustes**: tarjeta «Idioma», `SettingsViewModel`/`SettingsRoute`, textos es/en y testTags (T4–T5).
4. **E2E y cierre**: `LanguageSwitchE2ETest` S1–S5, regresión de `SettingsE2ETest` y localización, gates y evidencia (T6–T7).

Cada fase deja `lint` y `unit` en verde antes de pasar a la siguiente.

## Implementation Tasks

- **T1**: `AppLanguage.kt` (enum, `locale`, `fromSystem`) + `AppLanguageTest`.
- **T2**: `LanguagePreferenceStore` + `SharedPrefsLanguagePreferenceStore` + `AppLocaleController` + `AppLocaleControllerTest` (con store InMemory: inicial por defecto del sistema, `select` persiste y emite).
- **T3**: el idioma activo se aplica en la raíz de Compose con `AppLocaleHost` (`LocalConfiguration`/`LocalResources` localizados según el estado del controller; registro Koin en `AppModule` y cableado en `MainActivity`, por el hallazgo de los E2E de localización).
- **T4**: `SettingsViewModel` (`language` en el `UiState`, `onLanguageSelected`) + `SettingsScreen` (tarjeta «Idioma», dos opciones ≥ 48 dp, selección marcada, testTags nuevos) + `SettingsViewModelTest`.
- **T5**: textos `settings_language_*` en `values/` y `values-en/` con paridad (título de sección; las opciones son endónimos con la misma clave en ambos).
- **T6**: `LanguageSwitchE2ETest` S1–S5 (S1 = verify 1; S5 con clientes simulados que capturan `language` y locale de voz) sin IA ni llamadas de pago.
- **T7**: gates (`init`, `lint`, `unit`, `assembleDebug`, `e2e`), regresión de `SettingsE2ETest`/E2E de localización, evidencia y `feature_list.json` (solo `status`/`evidence`).

## Verification Plan

1. `./init.ps1` (o `./init.sh`): entorno y arranque en verde antes de empezar y al cerrar; **sin cambios en el script** (comprobaciones no bloqueantes; esta feature no añade servicios, modos ni permisos).
2. `./lint.ps1` (`lintDebug`): 0 errores y sin warnings nuevos sobre el baseline.
3. `./unit.ps1` (`testDebugUnitTest --rerun-tasks`): todas las suites en verde, incluidas `AppLanguageTest`, `AppLocaleControllerTest` y `SettingsViewModelTest`.
4. `assembleDebug` sin errores.
5. `./e2e.ps1` (`connectedDebugAndroidTest` sobre el emulador, fixtures locales con `E2eDatabase.clear()`, **sin IA y sin llamadas de pago** — no requiere confirmación humana): **S1** = verify 1 del `feature_list` (cambiar a inglés → los textos cambian sin reiniciar); **S2–S5** según los escenarios; `SettingsE2ETest` y los E2E de localización previos en verde. Como la feature cambia comportamiento visible y hay harness E2E persistente, la cobertura focalizada nueva es obligatoria (T6).
6. **Persistencia en entorno real** (manual, con `adb`): elegir «English», `adb shell am force-stop com.whatsforlunch.app`, reabrir y comprobar que la app y Ajustes arrancan en inglés (S3); borrar datos de la app y comprobar que vuelve al idioma del sistema (caso límite).
7. **Paridad de claves es/en** de `values/` y `values-en/` tras añadir los textos nuevos (gate de recuento).
8. Verificación manual con `adb`/uiautomator de S1 en la app real (tap «English» → títulos, botones y textos de Mis Recetas en inglés sin reinicio) y captura para la evidencia.

## Evidence To Capture

- `docs/evidence/cambiar-idioma-de-la-interfaz/informe.md`: entorno, comandos con resultado (init, lint, unit, assemble, e2e), desglose S1–S5 y resultado del force-stop de persistencia.
- 2 capturas: (1) Ajustes en español con la tarjeta «Idioma» y «Español» marcado; (2) la app en inglés tras el cambio (p. ej. Mis Recetas o Ajustes con «Language»/«English» marcado). Nombres `ajustes-idioma-es.png` e `interfaz-idioma-en.png`.
- Resumen en el `evidence` de `feature_list.json` (lo único que se toca de ese archivo) y entrada en `docs/progress-history.md`. Informe del validador en `docs/validations/cambiar-idioma-de-la-interfaz.md`.

## Trazabilidad

| Fuente (`feature_list.json`) | Spec |
|---|---|
| «Mostrar el selector de idioma español e inglés en Ajustes» | RF-IDI1, RF-IDI3 · S3, S4 · D2, D3 |
| «Aplicar el idioma elegido en los textos de todas las pantallas» | RF-IDI2, RF-IDI4, RF-IDI5 · S1, S2, S5 · D4, D5, D6 |
| verify 1 «E2E: cambiar a inglés y comprobar que las pantallas cambian sus textos sin reiniciar» | S1 |

## Validator Checklist

- [ ] ¿El `verification` de `feature_list.json` está ejecutado como S1 en el emulador con resultado legible (sin reiniciar la app)?
- [ ] ¿Ajustes muestra la sección «Idioma» con «Español»/«English» endónimos y una opción marcada (S1, RF-IDI1, D3)?
- [ ] ¿El cambio es inmediato en todas las pantallas y reversible (S1, S2, RF-IDI2) sin perder estado de navegación ni formularios?
- [ ] ¿La elección persiste tras force-stop y volver a abrir, y sin elección guardada se preselecciona el idioma del sistema (S3, S4, RF-IDI3, D1, D2)?
- [ ] ¿El idioma activo llega a los prompts de IA, al locale de voz y al formato de cantidades sin tocar prompts ni `VoiceCommandParser` (S5, RF-IDI4, D5, RD24 intacta)?
- [ ] ¿Solo hay dos idiomas (es/en), sin `values-xx` nuevas, sin `localesConfig` y sin traducir contenido del usuario (Non-Goals, D6)?
- [ ] ¿Paridad es/en de las claves nuevas, testTags/contratos estables y los E2E previos (Ajustes, localización) en verde?
- [ ] ¿`init`, `lint` y `unit` en verde, `assembleDebug` sin errores y evidencia + `feature_list.json` actualizados?
- [ ] ¿No se ha añadido nada ajeno (Room, permisos, dependencias nuevas, AppCompat, recreate como mecanismo de cambio)?
