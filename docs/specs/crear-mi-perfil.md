# Feature Implementation Spec: Crear Mi perfil con restricciones dietéticas

## Source Feature

- `id`: crear-mi-perfil
- `area`: funcional
- `depends_on`: `mover-opciones-a-bottom-bar` (accepted), `resolver-dudas-con-pinche` (accepted)
- `status`: not_started (a la hora de planificar; 2026-11-09)
- `source`: `feature_list.json`

## Goal

El usuario mantiene en una nueva pestaña «Mi perfil» sus alergias y sus intolerancias o enfermedades que limitan la dieta (p. ej. diabetes) como listas de chips con alta por texto y borrado tras habilitar un botón de editar, su tipo de alimentación (Omnívora, Pescetariana, Vegetariana, Vegana o Cetogénica) como selección única sin preselección inicial, y la opción «No tengo ninguna» en cada sección de restricciones. El perfil se persiste en el dispositivo (Room) y el Pinche lo usa: propone recetas que lo respetan y, mientras haya secciones pendientes, pide solo esas secciones antes de responder y no contesta hasta completarlas.

## Non-Goals

- Aplicar el perfil a Sugerencias, Recetas con mi Despensa, búsquedas, Escáner ni Coincidencias (solo el Pinche; decisión del usuario 2026-11-07).
- Añadir un tool de function calling nuevo (`get_profile`): el perfil viaja en el system prompt (los tools siguen siendo `get_pantry` y `get_my_recipes` de solo lectura, `⚠ S08`).
- Cuentas de usuario, sincronización, exportación/borrado del perfil, importación desde otros servicios.
- Voz, fotos o campos de texto libre fuera de las chips; sin permisos Android nuevos ni dependencias nuevas.
- Reescribir la conversación del Pinche más allá del gating y del bloque del prompt; la conversación sigue solo en memoria.
- Cambiar el aviso de privacidad de otros flujos ni cerrar `RK03` (sigue como puerta de release).

## Job Story

When tengo alergias, intolerancias o un tipo de alimentación que quiero respetar al cocinar,
I want to mantenerlos en «Mi perfil» y que el Pinche los conozca antes de responderme,
so I can recibir propuestas seguras para mí sin repetir mis restricciones en cada mensaje.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P7`/`P31`): edita su propio perfil en local. Las restricciones son **datos de salud** que salen del dispositivo al chatear con el Pinche (`RK03`): actualizar el aviso de privacidad y usar solo datos ficticios en la evidencia. Sin permisos Android nuevos.

## Acceptance Scenarios

### Scenario 1: Alta y borrado de chips de restricciones

Given un perfil nuevo en la pestaña «Mi perfil»
When el usuario escribe «Frutos secos» en Alergias y «Diabetes» en Intolerancias o enfermedades y las añade
Then ambas aparecen como chips en su sección; al habilitar el botón de editar de una sección, las chips muestran borrado y borrar una la hace desaparecer (verify 1).

### Scenario 2: Tipo de alimentación de selección única

Given el perfil con el tipo de alimentación sin elegir
When el usuario toca «Vegana» y después «Omnívora»
Then solo queda marcada la última opción tocada y el valor persistido es ese (verify 2).

### Scenario 3: «No tengo ninguna» exclusiva con las chips

Given la sección Alergias con la chip «Frutos secos»
When el usuario marca «No tengo ninguna»
Then las chips de esa sección se limpian y el alta queda bloqueada; al escribir y añadir una chip nueva, «No tengo ninguna» se desmarca (verify 3, D3).

### Scenario 4: El Pinche bloquea y pide solo lo pendiente

Given el perfil vacío (sin chips, sin «No tengo ninguna» y sin tipo de alimentación)
When el usuario envía un mensaje al Pinche
Then el Pinche responde pidiendo las tres secciones pendientes (Alergias, Intolerancias o enfermedades y Tipo de alimentación) con acceso a «Mi perfil», no llama a la IA y no responde a la pregunta; si el usuario completa solo Alergias y vuelve a enviar, pide únicamente lo que siga pendiente (verify 4, D5).

### Scenario 5: El Pinche responde respetando el perfil completo

Given un perfil completo (Alergias: «Frutos secos»; Intolerancias o enfermedades: «Diabetes»; Tipo de alimentación: «Vegana»)
When el usuario envía un mensaje al Pinche
Then la llamada a la IA se hace con el perfil en el system prompt y la respuesta/proposición respeta el perfil (p. ej. vegana y sin frutos secos); si el usuario cambia el tipo a «Omnívora», la siguiente propuesta usa el nuevo tipo (verify 2 y 4).

### Scenario 6: Persistencia y textos es/en

Given un perfil con chips, «No tengo ninguna» en alguna sección y tipo de alimentación elegido
When se cierra y se vuelve a abrir la app
Then el perfil se muestra tal como se dejó (Room) y todos los textos nuevos existen en español e inglés (verify 1–5, paridad de claves).

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppBottomBar.kt` — `AppBottomBarTestTags` y enum `AppTab` (route, `@StringRes`, icono, testTag; orden = orden de pestañas); comentario explícito: «La quinta pestaña («Mi perfil») la añadirá `crear-mi-perfil`; aquí no se deja placeholder» (líneas 36–39).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — `AppRoutes` (constantes de ruta), `openTab()` genérico (sin cambios), `routeTab()` (añadir la nueva ruta) y `WhatsForLunchApp` con `scrollToTopTick` y el `AppBottomBar` en el `Scaffold`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt` — `send()`/`runRequest()` (punto de anclaje del gating), `PincheNotice`, constructor con repositorios + `PincheChatClient`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — `PincheTestTags`, `NoticeBanner` (patrón de aviso con acción), burbujas y chips de sugerencia.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt` — puerto `reply(turns, language)`; hay que ampliarlo con el perfil.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatiblePincheChatClient.kt` — construcción del system prompt (líneas 59–64) y bucle de tools.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatProtocol.kt` — tools JSON y parsing de respuesta (sin cambios previstos).
- `app/src/main/java/com/whatsforlunch/app/data/ai/PromptSource.kt` + `app/src/main/assets/prompts/pinche_system_{es,en}.txt` — prompts por idioma con placeholders (`{RECIPE_CONTRACT}`); patrón a reutilizar para el bloque de perfil.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — Room v5, 6 entidades, `MIGRATION_1_2..MIGRATION_4_5` registradas en `di/AppModule.kt`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — bindings Koin (BD/migraciones/repos/ViewModels).
- `app/src/main/java/com/whatsforlunch/app/data/settings/LanguagePreferenceStore.kt` + `SharedPrefsLanguagePreferenceStore.kt` — patrón de puerto + adaptador (referencia de estilo; el almacenamiento aquí es Room por D1).
- `app/src/main/java/com/whatsforlunch/app/ui/settings/SettingsScreen.kt` — patrón de pantalla con tarjetas por sección y filas seleccionables (`Role.RadioButton`).
- Tests: `ui/pinche/PincheViewModelTest.kt`, `data/ai/PinchePromptsTest.kt`, `data/ai/PincheChatProtocolTest.kt` (unit); `androidTest/.../BottomBarE2ETest.kt` (afirma **exactamente 4 pestañas**: hay que actualizarla a 5), `PincheE2ETest.kt` (monta su propio `AppBottomBar`), `RecipeMigrationTest.kt`, `E2eDatabase.kt`.
- `app/src/main/res/values/strings.xml` / `values-en/strings.xml` — 320 claves + 5 plurales por locale; prefijos por feature; etiquetas de pestaña reutilizan las claves `*_title`.
- `DESIGN.md` (filas de pantalla, chips «píldora», tokens), `docs/domain-model.md` (RD##, máx. RD24), `docs/risks-and-open-questions.md` (`RK03`, `⚠ S05/S07/S08`), `docs/technical-discovery.md`.

### Existing Patterns To Follow

- Pestaña = entrada en `AppTab` (route + clave `*_title` + icono Material + testTag `bottom_bar_tab_*`) + constante en `AppRoutes` + rama de `routeTab` + bloque `composable` con `scrollToTopTick`.
- Reglas de negocio puras en `domain/<feature>/` (`object XxxRules`, sin Android) con IDs `RD##` en `docs/domain-model.md`; la UI no contiene reglas de negocio.
- Persistencia relational en Room con entidad + DAO + puerto `domain/repository/*` + `Room*Repository` e `InMemory*Repository` para tests; migraciones manuales registradas en `AppModule` y verificadas por `RecipeMigrationTest`.
- Textos de UI en `values`/`values-en` (paridad exacta de claves); prompts de IA en assets por idioma.
- E2E: patrón por pantalla (`XxxTestTags`, `E2eDatabase.clear()`, fakes locales de `PincheChatClient`); unit de ViewModel con `InMemory*Repository` + `runTest`.

### Current Gaps

- No existe nada de perfil: ni modelo, ni tablas, ni UI, ni prompts, ni tests (búsqueda confirmada: solo está el comentario de hand-off de `AppBottomBar.kt`).
- El puerto `PincheChatClient.reply(turns, language)` no admite contexto de perfil; los 3 `FakePincheChatClient` de tests (unit y E2E) implementan ese puerto y deberán adaptarse al cambio de firma.

## Technical Approach

1. **Dominio** (`domain/model/` + `domain/profile/`): enum `DietType` (OMNIVORE, PESCETARIAN, VEGETARIAN, VEGAN, KETOGENIC) y data class `UserProfile(allergies: List<String>, allergiesNone: Boolean, intolerances: List<String>, intolerancesNone: Boolean, dietType: DietType?)`. Reglas puras en `UserProfileRules` (D3/D5): validación de chip (trim, no vacío, sin duplicados ignorando mayúsculas), exclusividad «No tengo ninguna» ↔ chips (marcarla limpia y bloquea el alta de la sección; añadir chip la desmarca), `pendingSections(profile)` y `isComplete(profile)` (cada sección de restricciones: ≥1 chip o «No tengo ninguna»; tipo: elegido — **sin preselección**, D2).
2. **Persistencia (Room v6, D1)**: entidades `ProfileEntity` (tabla `profile`, fila única: `dietType: String?`, `allergiesNone: Boolean`, `intolerancesNone: Boolean`) y `ProfileRestrictionEntity` (tabla `profile_restrictions`: `id` autoincremental, `section` ALLERGY|INTOLERANCE, `value`; orden = `id`). `MIGRATION_5_6` crea ambas tablas (`RecipeDatabase` → versión 6). Puerto `domain/repository/UserProfileRepository` (`Flow<UserProfile>` + `suspend save(UserProfile)`), implementación `RoomUserProfileRepository` e `InMemoryUserProfileRepository` para tests; bindings en `di/AppModule.kt`.
3. **UI** (`ui/myprofile/`): `MyProfileRoute` + `MyProfileViewModel` (StateFlow de estado, mutaciones vía `UserProfileRules`) + `MyProfileScreen` con `MyProfileTestTags`. Layout estilo `SettingsScreen` (tarjetas por sección, tokens `WarmSpacing`/`WarmRadius`): sección «Alergias» e «Intolerancias o enfermedades que limitan la dieta» con chips píldora, campo de texto + «Añadir» (acción IME), botón «Editar» por sección que habilita el borrado por chip (icono terracota, patrón Escáner) y fila «No tengo ninguna»; sección «Tipo de alimentación» con 5 chips `FilterChip` de selección única. Pestaña `AppTab.MY_PROFILE` **al final tras Pinche** (D4), icono `Icons.Outlined.Person`, etiqueta `my_profile_title`, testTag `bottom_bar_tab_profile`; `AppRoutes.MY_PROFILE = "my_profile"` + rama de `routeTab` + bloque `composable` con `scrollToTopTick` (scroll al inicio al re-tocar la pestaña).
4. **Pinche**: `PincheViewModel` gana `UserProfileRepository`. En `runRequest()`, antes de llamar al cliente: si `pendingSections` no está vacío → **no se llama a la IA**; se añade un mensaje del asistente que pide solo las secciones pendientes (texto localizado con acción «Abrir Mi perfil» que navega a la pestaña). Con perfil completo, se llama a la IA. El puerto pasa a `reply(turns, language, profile: UserProfile)`; `OpenAiCompatiblePincheChatClient` añade al system prompt un bloque de perfil localizado (assets nuevos `pinche_profile_{es,en}.txt` con placeholders `{DIET_TYPE}`/`{ALLERGIES}`/`{INTOLERANCES}`, «ninguna» cuando la sección está marcada «No tengo ninguna») e instrucción de respetarlo en las propuestas. `get_profile` no se crea (non-goal).
5. **Aviso de privacidad (D7)**: `settings_privacy_notice` (es/en) ampliado indicando que las restricciones dietéticas y el tipo de alimentación (datos de salud) se envían al proveedor de IA al chatear con el Pinche (`RK03`).
6. **Casos límite**: perfil que vuelve a quedar incompleto al borrar chips en edición → el Pinche vuelve a pedir lo pendiente; app recién instalada → todo pendiente; chips vacías o duplicadas → rechazadas por `UserProfileRules`; cambios de idioma no traducen las chips del usuario (texto libre se conserva literal).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/model/DietType.kt`, `domain/model/UserProfile.kt` — create; modelo.
- `app/src/main/java/com/whatsforlunch/app/domain/profile/UserProfileRules.kt` — create; reglas puras (D3/D5).
- `app/src/main/java/com/whatsforlunch/app/domain/repository/UserProfileRepository.kt` — create; puerto.
- `app/src/main/java/com/whatsforlunch/app/data/local/ProfileEntity.kt`, `ProfileRestrictionEntity.kt`, `ProfileDao.kt` — create; tablas `profile` y `profile_restrictions`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDatabase.kt` — modify; versión 6, entidades y `MIGRATION_5_6`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomUserProfileRepository.kt`, `InMemoryUserProfileRepository.kt` — create; adaptadores.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; DAO, migración, repositorio y `MyProfileViewModel`.
- `app/src/main/java/com/whatsforlunch/app/ui/myprofile/MyProfileRoute.kt`, `MyProfileScreen.kt`, `MyProfileViewModel.kt` — create; pantalla, VM y `MyProfileTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppBottomBar.kt` — modify; `AppTab.MY_PROFILE` + `TAB_PROFILE` (retirar el comentario de hand-off).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modify; `AppRoutes.MY_PROFILE`, `routeTab`, bloque `composable`.
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheViewModel.kt`, `PincheScreen.kt`, `PincheRoute.kt` — modify; gating por secciones pendientes, mensaje de petición + acción «Abrir Mi perfil», dependencia del repositorio del perfil.
- `app/src/main/java/com/whatsforlunch/app/data/ai/PincheChatClient.kt`, `OpenAiCompatiblePincheChatClient.kt`, `PromptSource.kt` — modify; perfil en la firma y bloque `{PROFILE}` en el system prompt.
- `app/src/main/assets/prompts/pinche_profile_es.txt`, `pinche_profile_en.txt` — create; bloque de perfil localizado (o sección equivalente dentro de `pinche_system_*.txt`).
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify; claves `my_profile_*` (título, secciones, hint, «Añadir», «Editar», «No tengo ninguna», 5 tipos de alimentación, descripciones de borrado), mensaje de petición del Pinche (`pinche_profile_*`) y `settings_privacy_notice`; paridad exacta es/en.
- Tests unit: `domain/profile/UserProfileRulesTest.kt`, `ui/myprofile/MyProfileViewModelTest.kt` — create; `ui/pinche/PincheViewModelTest.kt`, `data/ai/PinchePromptsTest.kt` — modify (firma de los fakes incluida).
- Tests instrumentados: `androidTest/.../MyProfileE2ETest.kt` — create (S1–S6 + localización es/en); `BottomBarE2ETest.kt` (4 → 5 pestañas), `PincheE2ETest.kt` (gating + perfil) y `RecipeMigrationTest.kt` (v5→v6) — modify.

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` (tokens `Warm*`, chips píldora, tarjetas de sección) y patrón de `SettingsScreen.kt`.
- Screens or states affected: **nueva pantalla «Mi perfil»** (tarjetas «Alergias», «Intolerancias o enfermedades que limitan la dieta», «Tipo de alimentación»; chips con borrado solo en modo edición; «No tengo ninguna»; estado por defecto sin nada marcado); **barra inferior** (5 pestañas, «Mi perfil» al final, activa resaltada y re-toque = scroll al inicio); **Pinche** (mensaje de petición de secciones pendientes + acción «Abrir Mi perfil»; el resto del chat sin cambios); **Ajustes** (copy del aviso de privacidad).
- New design artifact required: no — la pantalla sigue los patrones existentes (Settings por tarjetas + chips del Escáner/filtros). La fila de pantallas de `DESIGN.md` se actualiza en la implementación.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no cambian dominios, capas ni dependencias (patrón existente).
- `CONSTRAINTS.md`: not needed — sin reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — no cambia el flujo de trabajo ni los comandos.
- Other docs:
  - `docs/domain-model.md` — **update**: entidad Perfil, reglas nuevas `RD25` (exclusividad «No tengo ninguna» ↔ chips) y `RD26` (flujo bloqueante por secciones pendientes del Pinche) y casos límite.
  - `docs/technical-discovery.md` — **update**: Room v6 y tablas nuevas, datos del perfil en el system prompt y en los datos que salen del dispositivo, assets de prompt.
  - `docs/risks-and-open-questions.md` — **update**: `RK03` ampliada (restricciones dietéticas y tipo de alimentación = datos de salud que salen al chatear con el Pinche) y `Q-crear-mi-perfil-1..5` (decisiones D1–D5 de esta spec).
  - `DESIGN.md` — **update**: fila «Mi perfil» y barra inferior de 5 pestañas.
  - `CONTEXT.md` — **update** si procede (glosario «Mi perfil»); revisar al implementar.

## Implementation Plan

1. Dominio: `DietType`, `UserProfile`, `UserProfileRules` + tests unitarios puros.
2. Persistencia: entidades/DAO, `MIGRATION_5_6`, `RoomUserProfileRepository`/`InMemoryUserProfileRepository`, bindings Koin y test de migración v5→v6.
3. UI: `ui/myprofile/` (VM + pantalla + tags), pestaña en `AppBottomBar`/`AppNavHost` y strings es/en con paridad.
4. Pinche: gating en `PincheViewModel` (mensaje de petición + acción «Abrir Mi perfil»), ampliación del puerto y bloque de perfil en el system prompt + tests unitarios.
5. E2E: `MyProfileE2ETest` nuevo, `PincheE2ETest` (gating) y `BottomBarE2ETest` (5 pestañas); gates completos y docs durables al día.

## Implementation Tasks

- [x] T1: `DietType`, `UserProfile` y `UserProfileRules` (validación, exclusividad D3, `pendingSections` D5) con `UserProfileRulesTest`.
- [x] T2: `ProfileEntity`/`ProfileRestrictionEntity`/`ProfileDao`, `RecipeDatabase` v6 con `MIGRATION_5_6`, repositorios Room/InMemory, Koin y test de migración v5→v6.
- [x] T3: `ui/myprofile/` (Route/ViewModel/Screen con `MyProfileTestTags`) y `MyProfileViewModelTest` con repositorio InMemory.
- [x] T4: quinta pestaña en `AppBottomBar`/`AppNavHost` (orden tras Pinche, `scrollToTopTick`) y strings es/en (paridad 1:1).
- [x] T5: gating del Pinche en `PincheViewModel` + mensaje de petición con «Abrir Mi perfil»; firma `reply(turns, language, profile)` y bloque de perfil en el system prompt (`pinche_profile_{es,en}.txt`); adaptar los fakes y `PincheViewModelTest`/`PinchePromptsTest`.
- [x] T6: `settings_privacy_notice` ampliado (es/en) y docs durables (`domain-model.md` RD25/RD26, `technical-discovery.md`, `risks-and-open-questions.md` con `Q-crear-mi-perfil-1..5`, `DESIGN.md`).
- [x] T7: `MyProfileE2ETest` (S1–S6 + es/en), `PincheE2ETest` (S4/verify 4) y `BottomBarE2ETest` a 5 pestañas; gates en verde.

## Verification Plan

- `./init.sh` / `./init.ps1` — entorno listo (ejecuta comprobaciones no bloqueantes; no arranca servidores largos; el falso negativo conocido de `monkey`/`pidof` se verifica aparte con `adb shell pidof`).
- `./gradlew lintDebug` — 0 errores (línea base de warnings: 49; sin warnings nuevos en el diff).
- `./gradlew testDebugUnitTest` — todo en verde; `UserProfileRulesTest`, `MyProfileViewModelTest` y los Pinche ampliados incluidos.
- `./gradlew assembleDebug` y `compileDebugAndroidTestKotlin` — OK.
- Paridad es/en de `strings.xml` — mismas claves en `values` y `values-en` (contar claves; hoy 320 + 5 plurales).
- `RecipeMigrationTest` (v5→v6) — 1/1 en verde (migración con escritura/lectura, sin datos residuales).
- E2E focalizada (`connectedDebugAndroidTest`, fixtures locales y cliente de IA simulado): `MyProfileE2ETest` (verify 1: alta/borrado de chips con modo edición; verify 2: selección única «Vegana»→«Omnívora» y persistencia; verify 3: «No tengo ninguna» limpia y bloquea; verify 4: perfil vacío → el Pinche pide las tres secciones sin llamar a la IA y, tras completar Alergias, pide solo lo pendiente; verify 5: con perfil completo la llamada lleva el perfil en el prompt y la propuesta lo respeta —verificado con el fake que captura el perfil—; verify 6: persistencia tras reinicio y textos es/en) + `PincheE2ETest` y `BottomBarE2ETest` actualizadas.
- Batería E2E completa (`./e2e.sh` / `e2e.ps1`, `connectedDebugAndroidTest`) sin regresión sobre el inventario actual (~231 `@Test` de androidTest + los nuevos).
- Comprobación manual con `adb`/uiautomator: pestaña «Mi perfil» al final de la barra, re-toque con scroll al inicio y mensaje del Pinche con acceso a «Mi perfil».
- Toda la verificación usa **solo datos ficticios** («Frutos secos», «Diabetes», «Vegana») y el cliente de IA simulado; **sin llamadas reales de pago** (regla de `AGENTS.md`).

## Evidence To Capture

- Salidas de `lintDebug`, `testDebugUnitTest`, `assembleDebug` y recuento de paridad es/en en `docs/evidence/crear-mi-perfil/`.
- XML/resultado de `MyProfileE2ETest`, `PincheE2ETest` y `BottomBarE2ETest` (focalizada) y de la batería, con recuento por clase.
- 2–3 capturas es-ES 1280×2856: «Mi perfil» con chips y modo edición, tipo de alimentación seleccionado y el mensaje del Pinche pidiendo el perfil con su acción.
- Test de migración v5→v6 en verde; resumen en `docs/evidence/crear-mi-perfil/informe.md` y `evidence` de `feature_list.json`.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (solo el Pinche consume el perfil; sin tools nuevos, sin permisos ni dependencias nuevas).
- [ ] Acceptance scenarios pass (S1–S6, verify 1–5 de `feature_list.json`).
- [ ] Verification evidence is present (`docs/evidence/crear-mi-perfil/` con gates, E2E, capturas y migración).
- [ ] Persistent E2E coverage added/updated (`MyProfileE2ETest` nuevo; `PincheE2ETest` y `BottomBarE2ETest` actualizadas) con cliente de IA simulado y fixtures locales.
- [ ] `feature_list.json` y `PROGRESS.md` updated correctly (solo `status`/`evidence` en la lista).
- [ ] No unrelated product behavior or extra feature work was added (Sugerencias, Recetas con mi Despensa y búsquedas sin cambios).
- [ ] Seguridad/privacidad: sin secretos ni PII reales en diff/evidencia; `RK03` ampliada y aviso de privacidad actualizado; evidencia solo con datos ficticios.

## Decisiones del usuario (2026-11-09)

- **D1 — Almacenamiento en Room con `MIGRATION_5_6`** (tabla(s) nuevas en `whatsforlunch.db`), no SharedPreferences.
- **D2 — Tipo de alimentación sin preselección** en un perfil nuevo; elegirlo cuenta como pendiente en el flujo bloqueante del Pinche.
- **D3 — «No tengo ninguna» exclusiva con limpieza**: al marcarla se limpian las chips de la sección y se bloquea su alta; añadir una chip la desmarca.
- **D4 — Pestaña «Mi perfil» al final de la barra, tras Pinche** (5 pestañas).
- **D5 — Completitud por sección** (refina «perfil vacío» de la verify 5): el Pinche pide solo lo que falte —cada sección de restricciones con ≥1 chip o «No tengo ninguna», y tipo de alimentación elegido— y responde en cuanto el perfil esté completo.

Derivadas: **D6** el perfil viaja en el system prompt (sin tool `get_profile`); **D7** el aviso de privacidad de Ajustes se amplía con los datos de salud que salen al chatear con el Pinche (`RK03`); **D8** el mensaje de petición del Pinche lista las secciones pendientes y ofrece la acción «Abrir Mi perfil».

## Hallazgos de implementación (2026-11-09)

- **S3 leída como bloqueo visible en la UI + regla**: con «No tengo ninguna» marcada el campo y «Añadir» quedan **deshabilitados** (el alta está bloqueada en pantalla) y, a nivel de regla, `UserProfileRules.addChip` **desmarca** «No tengo ninguna» de la sección; ambas caras de D3 están cubiertas (`UserProfileRulesTest.addingAChipUnmarksNoneOnThatSection` y el E2E `noneOptionClearsTheSectionAndBlocksAdding`).
- **Mensaje de petición localizado en la UI**: `PincheMessage` gana `profileRequest: List<PendingProfilePart>` (vacío en el resto de mensajes) y `PincheScreen` compone el texto con `stringResource` y la acción «Abrir Mi perfil»; el texto va en blanco, así que nunca entra en los turnos que viajan al modelo. `PincheViewModel` no necesita resources.
- **Firma del puerto**: `PincheChatClient.reply(turns, language, profile)` obligó a adaptar los 4 `FakePincheChatClient` (unit y E2E) y las 7 construcciones de `PincheViewModel` en tests; los escenarios que esperan respuesta de la IA seedean un **perfil completo**, porque el gating es real también en tests.
- **Helper `localized()` de los E2E con `MainActivity`**: `BottomBarE2ETest` y `PantryManagementE2ETest` resolvían el texto esperado con el **locale del dispositivo**, mientras la app pinta su propio idioma (`AppLocaleController`, en Ajustes); en un AVD en `en-US` las aserciones fallaban. El helper ahora resuelve con el idioma activo de la app, igual que hace la propia app (sin cambios de producto).
- **Reparación de un resto del merge `9dacc87` (test ajeno)**: `PantryManagementE2ETest.scannerSaveReturnsToThePantryWithTheSavedFoods` pulsaba «Volver» en la Despensa, botón retirado por `e8c5730` (`mover-opciones-a-bottom-bar`, RF-MOB5) mientras el paso se añadía en `f591dbe` (`mover-escaneo-a-la-despensa`) en la otra rama; el merge los juntó y el test fallaba en cualquier entorno (verificado en aislamiento). Se sustituyó por `Espresso.pressBack()` → Mis Recetas, que es el comportamiento vigente (RF-MOB4/D1) y conserva la intención del paso (sin PANTRY duplicado). Solo cambia el test.
- **`SettingsE2ETest.deletingTheKeyAsksToConfigureItAgainForAiFeatures`**: `ComposeTimeoutException` de 5 s en la batería; **pasa en aislamiento** y es el flake ya documentado en la sesión 116 (fuera de este diff).
- **Sin cambios de producto fuera de alcance**: Sugerencias, Recetas con mi Despensa, búsquedas, Escáner y Coincidencias siguen sin consumir el perfil; `pinche_system_*` intacto; sin permisos ni dependencias nuevas.
