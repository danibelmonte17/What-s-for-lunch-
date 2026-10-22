# Feature Implementation Spec: Sumar cantidades al guardar

## Source Feature

- `id`: sumar-cantidades-al-guardar
- `area`: funcional
- `depends_on`: escanear-despensa-con-camara (`accepted`)
- `status`: passing (implementada el 2026-10-22; pendiente de validación independiente)
- `source`: `feature_list.json`

> **Decisiones de producto cerradas antes de la spec** (2026-10-22, usuario): los tres casos límites que `feature_list.json` y `PROGRESS.md` exigen decidir antes de planificar (Alimento existente sin cantidad, chip en 0 y suma superior a 99) más el destino del diálogo de coincidencia. Registradas como `Q-sumar-cantidades-al-guardar-1..4` en `docs/risks-and-open-questions.md` y formalizadas como D1-D3 más abajo.

## Goal

Al confirmar un Escaneo con «Guardar en la Despensa», la cantidad de cada chip **se suma a la del Alimento que ya existe** en la Despensa con el mismo nombre normalizado, en lugar de preguntar «Mantener cantidad»/«Actualizar cantidad». El resultado sigue siendo **un solo Alimento por nombre normalizado**: la coincidencia actualiza la fila existente (misma id, mismo nombre) y nunca crea una fila nueva; solo los chips sin coincidencia crean Alimentos nuevos.

## Non-Goals

- No cambia el máximo de cantidad: sigue siendo **99** en toda la app (decisión D6 de 2026-10-21) y siguen prohibidos decimales y unidades.
- No se toca el esquema Room v2 ni `MIGRATION_1_2` (`RecipeDatabase`, `PantryItemEntity`, `PantryItemDao` intactos).
- No cambia la pantalla Despensa: stepper 0..99, alta/edición/borrado y rechazo de duplicados con aviso (Q-gestionar-alimentos-de-la-despensa-3) se quedan como están.
- No cambia la Coincidencia ni el badge de Mis Recetas, ni la detección en vivo, el HUD, el muestreo de 300 ms, RD05, RD06 ni RD12.
- El chip no previsualiza el resultado de la suma: muestra su propia cantidad (0..99), como hoy.
- No hay deshacer, historial ni auditoría de las sumas (solo importa el estado actual).
- No se toca el carrusel de Recetas compatibles, las Sugerencias ni la Lista de la Compra.

## Job Story

When repongo la Despensa escaneando productos que ya tengo,
I want to que las cantidades detectadas se sumen solas a las que ya figuran,
so I can actualizar la Despensa sin responder preguntas y sin perder las cantidades que ya había anotado.

## Users And Permissions

- **Usuario** (único rol del MVP, `P31`): guarda Escaneos en su Despensa. Sin cuentas, sin permisos ni roles nuevos.

## Acceptance Scenarios

### Scenario 1: Suma de cantidades (verify 1 de `feature_list.json`)

Given la Despensa con «Tomate» cantidad 1 y un chip «Tomate» cantidad 2 en el Escáner
When el usuario pulsa «Guardar en la Despensa»
Then la Despensa muestra «Tomate» cantidad 3, sin ningún diálogo y sin crear una fila nueva

### Scenario 2: Alimento existente sin cantidad (verify 2, caso límite 1)

Given la Despensa con «Tomate» sin cantidad y un chip «Tomate» cantidad 2
When el usuario guarda el Escaneo
Then «Tomate» queda con cantidad 2 («sin cantidad» cuenta como 0 en la suma)

### Scenario 3: Chip en 0, sin cantidad (verify 2, caso límite 2)

Given la Despensa con «Tomate» cantidad 3 y un chip «Tomate» en 0 (sin cantidad)
When el usuario guarda el Escaneo
Then «Tomate» conserva la cantidad 3 y no se crea fila nueva
And, si el Alimento existente también está sin cantidad, sigue sin cantidad (0 + 0 = sin cantidad)

### Scenario 4: Suma superior al máximo (verify 2, caso límite 3)

Given la Despensa con «Tomate» cantidad 60 y un chip «Tomate» cantidad 50
When el usuario guarda el Escaneo
Then «Tomate» queda con cantidad 99 (la suma se recorta al máximo 99, sin aviso)

### Scenario 5: Ninguna pregunta en todo el flujo

Given un chip cuyo nombre normalizado coincide con un Alimento de la Despensa (detectado o añadido a mano)
When el usuario edita el listado y guarda
Then no aparece en ningún momento el diálogo «Alimento ya en la Despensa» ni ninguna otra pregunta
And el chip conserva la marca informativa «Ya está en la Despensa» (no accionable)

### Scenario 6: Chips sin coincidencia (regresión)

Given un chip que no coincide con ningún Alimento de la Despensa
When el usuario guarda el Escaneo
Then se crea el Alimento con la cantidad del chip (0 = sin cantidad), como hasta ahora

### Scenario 7: Varios chips que coinciden en un mismo guardado

Given dos chips que coinciden con dos Alimentos existentes más un chip sin coincidencia
When el usuario pulsa «Guardar en la Despensa»
Then se aplican todas las sumas en un único guardado, sin secuencia de preguntas y sin abortar el guardado por decisiones pendientes

### Scenario 8: Textos es/en

Given la interfaz en español o en inglés
When se guarda un Escaneo con coincidencias
Then solo se ve la marca «Ya está en la Despensa» en el idioma activo y no queda ningún resto visible del diálogo retirado

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `sumar-cantidades-al-guardar` (steps, verification, notes: cambia el comportamiento aceptado de `escanear-despensa-con-camara` S11/S12 y su Non-Goal de no sumar; los casos límites ya decididos; máximo 99 vigente); dependencia `escanear-despensa-con-camara` en `accepted`.
- `PROGRESS.md` — entorno (AVD `Pixel_10_Pro` API 37, ejecución de scripts con `cmd /c` + `JAVA_HOME`), reglas de evidencia y cierre; aviso de que esta feature exige decidir los casos límites antes de la spec (hecho el 2026-10-22).
- `CONTEXT.md` — términos Alimento, Despensa, Escaneo/Escáner (no sinónimos).
- `docs/domain-model.md` — Alimento (cantidad entera 0..99; 0 = sin cantidad), fusión por nombre normalizado, «se pregunta en pantalla antes de guardar («Mantener cantidad» o «Actualizar cantidad»)» en Relaciones y en Casos límite (líneas que esta feature sustituye), reglas RD01-RD12.
- `docs/risks-and-open-questions.md` — sin riesgos nuevos; Q-escanear-despensa-con-camara-1 («¿qué pasa cuando un Alimento escaneado coincide con uno ya presente?») queda **superada** por esta feature.
- `docs/technical-discovery.md` — comandos Gradle (`./gradlew installDebug|assembleDebug|testDebugUnitTest|lintDebug|connectedDebugAndroidTest`), stack Room v2 / CameraX / MediaPipe.
- `docs/specs/escanear-despensa-con-camara.md`, `docs/specs/detectar-alimentos-en-vivo.md` — comportamiento aceptado que esta feature sustituye (diálogo «Mantener/Actualizar» como S11/S12 y como Non-Goal «sin sumar cantidades» de la detección en vivo).
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — reglas puras del Escaneo: `matchesWithPantry` (coincidencia por `normalizeName`), `resolveDuplicate(existing, draft, resolution)` (hoy `KeepExisting` → `null` / `UpdateQuantity` → `existing.copy(quantity = draft.quantity.takeUnless { it == 0 })`) y `toPantryItem(draft)` (0 → `null`). **Aquí vive la regla de suma**.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt` — contrato `savePantryItem(item): PantrySaveResult` (`Saved` | `DuplicateName`) y el `enum PantryDuplicateResolution { KeepExisting, UpdateQuantity }` (solo lo usa el flujo que se retira).
- `app/src/main/java/com/whatsforlunch/app/domain/model/ScanDraftItem.kt`, `PantryItem.kt` — `ScanDraftItem.quantity: Int` (0..99, 0 = sin cantidad) y `PantryItem.quantity: Int?` (`null` = sin cantidad).
- `app/src/main/java/com/whatsforlunch/app/domain/pantry/PantryItemValidation.kt` — `MIN_QUANTITY = 0`, `MAX_QUANTITY = 99` (máximo único de la app, D6).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — `saveToPantry`/`persistItems` con el flujo de decisiones: `duplicateDecisions`, `activeDuplicateItemId`, `duplicatePromptQueue`, `suppressedAutoPrompts`, `pendingSave`, `openDuplicateResolution`, `resolveDuplicate`, `dismissDuplicateResolution`, `queueNewAutomaticMatches`, `promptKey`; salvaguarda `PantrySaveResult.DuplicateName` → aviso `DUPLICATE_NAME`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — `ScannerFoodCard` con la marca `duplicateMark(id)` (hoy `TextButton` que abre el diálogo), `DuplicateResolutionDialog` y tags `DUPLICATE_DIALOG/TITLE/MESSAGE/KEEP/UPDATE`; `NoticeBanner` con `scanner_duplicate_error`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — cablea `onOpenDuplicate`/`onResolveDuplicate`/`onDismissDuplicate` al ViewModel.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt`, `InMemoryPantryRepository.kt` — `savePantryItem` actualiza cuando el id coincide (`Saved`) y devuelve `DuplicateName` solo ante otro id con el mismo nombre normalizado; guardia `require` de rango.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `scanner_duplicate_title/message/existing_quantity/keep/update`, `scanner_no_quantity` (solo usado por el diálogo), `scanner_already_in_pantry`, `scanner_duplicate_error`.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt`, `ui/scanner/ScannerViewModelTest.kt` — unit de `resolveDuplicate`, cola de prompts y decisiones.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — 16 casos con `FakeScanImageSource`/`FakeFoodDetector`; los de diálogo son `pantryMatchOpensDecisionDialogAndKeepOrUpdateReusesExistingRow` y `multiplePendingMatchesAskSequentiallyAndDismissAbortsWholeSave`; el harness construye `ScannerScreen(...)` a mano con los callbacks del diálogo.
- `DESIGN.md` — Escáner de la Despensa (chips editables, marca de coincidencia); no define el diálogo de decisión ni su ausencia.

### Existing Patterns To Follow

- MVVM + StateFlow (`*Route` → `*Screen` sin estado → `*ViewModel` con `uiState`); **reglas de negocio puras en `domain/`** (`ScanDetections`) y la UI sin lógica de negocio.
- Un solo Alimento por nombre normalizado (`normalizeName`: trim, minúsculas, sin marcas, espacios simples); la coincidencia con la Despensa ya se calcula en `matchesWithPantry`.
- Cantidades derivadas de `PantryItemValidation.MIN_QUANTITY/MAX_QUANTITY`; `toPantryItem` mapea 0 → `null`.
- testTags por pantalla (`ScannerTestTags.*`), objetivos táctiles ≥ 48 dp para lo accionable, cadenas `scanner_*` duplicadas en `values/` y `values-en/`, identificadores en inglés (`P19`, `P25`).
- E2E Compose sobre DB/`InMemoryPantryRepository` con fakes deterministas y locale inyectado; los scripts del harness no se modifican.

### Current Gaps

- No existe la suma: `resolveDuplicate` solo mantiene o sustituye la cantidad (la sustitución era «Actualizar cantidad»).
- No hay regla de dominio para el «sin cantidad» como neutro aditivo ni para el recorte de la suma a 99 (hoy el recorte solo vive en `ScanDetections.updateQuantity`).
- El flujo de decisiones (diálogo, cola de preguntas, `pendingSave`, supresión de prompts automáticos) queda huérfano y debe retirarse por completo.

## Technical Approach

1. **Regla de suma en dominio** (`domain/scanner/ScanDetections.kt`): nueva función pura `sumInto(existing: PantryItem, draft: ScanDraftItem): PantryItem` que **sustituye** a `resolveDuplicate` y devuelve `existing.copy(quantity = merged)` (conserva id y nombre del Alimento existente — D4). Fórmula única (D1 + D2): `total = (existing.quantity ?: 0) + draft.quantity` · `merged = total.coerceAtMost(PantryItemValidation.MAX_QUANTITY)` · `quantity = merged.takeIf { it != 0 }` (0 → `null`, «sin cantidad»). Cubre los tres casos límites: null+N → N (E1), M+0 → M (E2), 60+50 → 99 (E3) y 0+0 → sin cantidad. Unit-testable sin Android.
2. **Persistencia** (`ui/scanner/ScannerViewModel.kt` → `persistItems`): para cada chip, `matches[draft.id]` == null → `savePantryItem(ScanDetections.toPantryItem(draft))`; si hay coincidencia → `savePantryItem(ScanDetections.sumInto(existing, draft))`. Guardado único e inmediato al pulsar «Guardar en la Despensa» (S7): sin `pendingSave`, sin cola de preguntas ni decisiones. Se conserva la salvaguarda `PantrySaveResult.DuplicateName` → aviso `DUPLICATE_NAME` y abort del resto (D5).
3. **Retirada del flujo de decisiones**: `PantryDuplicateResolution` (enum), `openDuplicateResolution`, `resolveDuplicate`, `dismissDuplicateResolution` y los campos `duplicateDecisions`, `activeDuplicateItemId`, `duplicatePromptQueue`, `suppressedAutoPrompts`, `pendingSave`, más los helpers `queueNewAutomaticMatches` y `promptKey` y su recorte en `recalculateItems`. `matches` **se conserva** (alimenta la marca del chip).
4. **UI** (`ScannerScreen.kt` / `ScannerRoute.kt`): se retira `DuplicateResolutionDialog`, los tags `DUPLICATE_DIALOG/TITLE/MESSAGE/KEEP/UPDATE` y los parámetros `onOpenDuplicate`/`onResolveDuplicate`/`onDismissDuplicate`. La marca `scanner_already_in_pantry` del chip **se conserva** como texto informativo no accionable (D3; mismo testTag `duplicateMark(id)` para no romper la localización de tests, con nombre accesible = texto visible).
5. **Cadenas** (`values/`, `values-en/`): se retiran `scanner_duplicate_title`, `scanner_duplicate_message`, `scanner_duplicate_existing_quantity`, `scanner_duplicate_keep`, `scanner_duplicate_update` y `scanner_no_quantity` (confirmar con `grep` que no tengan más usos antes de borrar). Se conservan `scanner_already_in_pantry` y `scanner_duplicate_error`. Mismo juego de claves en ambos locales.
6. **Tests**: unit de `sumInto` (todas las combinaciones de E1/E2/E3 + 0+0 + conservación de id/nombre) y de `ScannerViewModel` (guardado con una y varias coincidencias, sin diálogos ni estado de decisiones, con `InMemoryPantryRepository`); E2E `ScanPantryE2ETest` sustituye los dos casos de diálogo por los de suma (mismos fakes y patrón de locale).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — modificar; `sumInto` (o `mergedQuantity` equivalente) sustituye `resolveDuplicate`.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt` — modificar; retirar `enum PantryDuplicateResolution` (se conservan `PantryRepository` y `PantrySaveResult`).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — modificar; `persistItems` con suma; retirar estado y acciones del flujo de decisiones.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` — modificar; retirar `DuplicateResolutionDialog` y los tags de decisión; marca no accionable.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt` — modificar; retirar el cableado del diálogo.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; retirar las cadenas del diálogo en ambos locales.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt`, `app/src/test/java/com/whatsforlunch/app/ui/scanner/ScannerViewModelTest.kt` — modificar; unit de suma y de guardado sin decisiones.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — modificar; sustituir `pantryMatchOpensDecisionDialogAndKeepOrUpdateReusesExistingRow` y `multiplePendingMatchesAskSequentiallyAndDismissAbortsWholeSave` por los escenarios de suma y actualizar la firma de `ScannerScreen(...)` del harness.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt`, `InMemoryPantryRepository.kt` — sin cambios previstos (la actualización del mismo id ya devuelve `Saved`).
- `app/src/main/java/com/whatsforlunch/app/data/local/*` — sin cambios (Room v2 intacto).
- `docs/domain-model.md` — modificar; regla de suma al guardar (nueva **RD13**), Relaciones y Casos límite del Escaneo (desaparece «se pregunta en pantalla»), «Validaciones de datos» de la cantidad propuesta (al guardar **se suma** en lugar de conservarse).
- `docs/specs/escanear-despensa-con-camara.md`, `docs/specs/detectar-alimentos-en-vivo.md` — modificar; nota de revisión de una línea: el diálogo «Mantener/Actualizar» y el Non-Goal de no sumar quedan **sustituidos** por `sumar-cantidades-al-guardar` (evita que un lector futuro confíe en el comportamiento retirado).
- `docs/risks-and-open-questions.md` — not needed en implementación; ya contiene `Q-sumar-cantidades-al-guardar-1..4` (registradas al redactar esta spec).

## Visual Design Impact

- UI involved: yes (solo la pantalla Escáner)
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); `uxdocs/chefencasa_esc_ner_de_nevera_ia/` como inspiración. El material no define el diálogo de decisión (laguna RK04 conocida).
- Screens or states affected: Escáner — desaparece el diálogo «Alimento ya en la Despensa» («Mantener cantidad»/«Actualizar cantidad»); el chip conserva la marca «Ya está en la Despensa», ahora informativa. Sin cambios en Despensa, Mis Recetas ni el Detalle.
- New design artifact required: no — se reutiliza el texto de marca existente y los tokens actuales; no hay color, tipografía ni layout nuevos.
- Criterios: la marca conserva el color `secondary` actual y los mismos márgenes; al dejar de ser accionable no necesita área táctil de 48 dp (solo se exige a lo que siga siendo interactivo); textos es/en sin claves nuevas.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe; no cambian capas, dominios ni dirección de dependencias (solo se sustituye una regla pura en `domain/` y se retira estado de UI).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (la suma se documenta como RD13 en `docs/domain-model.md`, que sigue siendo la fuente de reglas).
- `AGENTS.md`: not needed — sin cambios de flujo de agentes, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — update (RD13 + Relaciones + Casos límite + Validaciones); `docs/specs/escanear-despensa-con-camara.md` y `docs/specs/detectar-alimentos-en-vivo.md` — update (nota de revisión, sin reescribir su historia); `docs/risks-and-open-questions.md` — ya actualizado en planificación (`Q-sumar-cantidades-al-guardar-1..4`).

## Dudas abiertas y desconocidos (Unknowns)

Decisiones de producto confirmadas por el usuario el 2026-10-22 (origen `Q-sumar-cantidades-al-guardar-1..4`); no queda ambigüedad bloqueante:

- **D1 · «Sin cantidad» = neutro aditivo** (casos límites 1 y 2): `null`/0 se trata como 0 al sumar y si el resultado es 0 se guarda «sin cantidad» (`null`). Ej.: existente sin cantidad + chip 2 → 2; existente 3 + chip 0 → 3; ambos sin cantidad → sigue sin cantidad.
- **D2 · Tope 99** (caso límite 3): la suma se recorta a `MAX_QUANTITY` (99) **sin aviso** (tope duro). El máximo de la app no cambia (D6 de 2026-10-21).
- **D3 · Sin diálogo**: desaparece por completo el flujo «Mantener cantidad»/«Actualizar cantidad» (diálogo, prompts automáticos, cola secuencial y abort del guardado); la marca «Ya está en la Despensa» del chip se conserva como información **no accionable**.
- **D4 · El Alimento existente conserva id y nombre**: la suma solo cambia la cantidad (igual que hacía «Actualizar cantidad»). Detalle técnico derivado, revisable en validación sin cambiar el comportamiento visible.
- **D5 · Salvaguarda de duplicado**: si al guardar aparece un duplicado con otra id (carrera improbable: un solo dispositivo), se mantiene el aviso `DUPLICATE_NAME` y se aborta el guardado restante, como hoy.

No inspeccionado: `PantryScreen.kt` completo (solo su contrato y su ViewModel, que no cambia), `uxdocs/` (solo lectura) y la suite E2E completa más allá de `ScanPantryE2ETest` (regresión a cargo del implementador).

## Implementation Plan

1. Dominio: `ScanDetections.sumInto` + unit (S1-S4).
2. `ScannerViewModel`: suma en `persistItems` y retirada del flujo de decisiones + unit (S1, S3-S7).
3. UI y cadenas: sin diálogo ni tags de decisión, marca informativa, `PantryDuplicateResolution` eliminado (S5, S8).
4. E2E `ScanPantryE2ETest` con los dos `verify`, gates completos, docs (`domain-model.md` + notas de revisión) y cierre en `passing`.

## Implementation Tasks

- [x] T1: `ScanDetections.sumInto` con la fórmula de D1/D2 y unit (null+N→N, M+0→M, 0+0→sin cantidad, 60+50→99, 99+1→99, conservación de id/nombre). (S1-S4)
- [x] T2: `ScannerViewModel.persistItems` con suma; retirar `duplicateDecisions`/`activeDuplicateItemId`/`duplicatePromptQueue`/`suppressedAutoPrompts`/`pendingSave`, `openDuplicateResolution`/`resolveDuplicate`/`dismissDuplicateResolution`, `queueNewAutomaticMatches`/`promptKey`; unit de guardado con una y varias coincidencias sin diálogos. (S1, S3, S4, S7)
- [x] T3: `ScannerScreen`/`ScannerRoute`: retirar `DuplicateResolutionDialog`, tags `DUPLICATE_*` de decisión y sus callbacks; marca `scanner_already_in_pantry` no accionable; borrar `PantryDuplicateResolution` y las cadenas es/en del diálogo (con `grep` previo de usos). (S5, S8)
- [x] T4: `ScanPantryE2ETest`: nuevos casos `saveSumsChipQuantityIntoExistingPantryItem` (verify 1), `saveAppliesAgreedQuantityEdgeCasesWithoutNewRows` (verify 2) y `saveWithPantryMatchesNeverShowsDecisionDialog` (S5); actualizar el harness (`ScannerScreen(...)` sin callbacks del diálogo) sin perder cobertura previa. (S1-S8)
- [x] T5: gates (`init`, `lint`, `unit`, `assembleDebug`, E2E focalizada + regresión completa), `docs/domain-model.md` (RD13) y notas de revisión en las specs vecinas; feature en `passing` con evidencia.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; no arranca procesos duraderos; los scripts no se modifican). Nota de `PROGRESS.md`: `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& set PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%LOCALAPPDATA%\Android\Sdk\emulator;%PATH%&& set PKG=com.whatsforlunch.app&& powershell -NoProfile -ExecutionPolicy Bypass -File <script>.ps1 > .harness\<x>.log 2>&1"` (el `init` tiene un falso fallo conocido de `pidof` tras un `installDebug`: reintentar).
- `./lint.ps1` / `gradlew lintDebug` → 0 errores; `gradlew testDebugUnitTest --rerun-tasks` → verde con los unit nuevos; `gradlew assembleDebug` → BUILD SUCCESSFUL.
- E2E persistente: `connectedDebugAndroidTest` en el AVD `Pixel_10_Pro` (API 37) — `./e2e.ps1` vale pero desinstala la app al terminar (reinstalar con `./gradlew installDebug` si se comprueba a mano):
  - verify 1 (S1): Despensa «Tomate» 1 + chip «Tomate» 2 → guardar → «Tomate» **3**, una sola fila, sin diálogo.
  - verify 2 (S2-S4): existente sin cantidad + chip 2 → 2 · existente 3 + chip 0 → 3 · existente 60 + chip 50 → 99 · ambos sin cantidad → sigue sin cantidad; **en ningún caso se crea fila nueva**.
  - S5: `DUPLICATE_DIALOG` no existe en ningún momento del flujo (ni con prompts automáticos ni al guardar).
  - S6-S8: alta sin coincidencia sin cambios, varias coincidencias en un único guardado y marca localizada en es/en.
  - Regresión completa sin romper `AppLaunchE2ETest`, `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `DeleteRecipeE2ETest`, `SearchAndFilterRecipesE2ETest`, `PantryManagementE2ETest`, `PantryLocalizationE2ETest`, `MatchBadgeE2ETest`, `MyRecipesCardsE2ETest`, `RecipeDetailE2ETest`, `MyRecipesLocalizationE2ETest`, `CameraXLiveFrameSourceTest`, `MediaPipeFoodDetectorTest`.
  - Barrido de restos: `grep` de `PantryDuplicateResolution`, `resolveDuplicate`, `DUPLICATE_DIALOG`, `scanner_duplicate_keep`, `scanner_duplicate_update`, `scanner_duplicate_title`, `scanner_duplicate_message`, `scanner_duplicate_existing_quantity` en `app/src` → 0 hits.
- Comprobación manual del validador (emulador, `adb` + uiautomator): los dos `verify` sobre la app real, en español, con el stepper del chip y la Despensa visible.
- Cobertura E2E: **sí**, porque la feature cambia un flujo de usuario observable (el guardado del Escaneo); los casos van con fakes deterministas, igual que el resto del Escáner.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/sumar-cantidades-al-guardar.md` — informe con los gates (nombres de test y recuentos), recorrido de S1-S8, los dos `verify` ejecutados en el emulador, el barrido de restos del diálogo y la revisión de RD13 en `docs/domain-model.md`; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/sumar-cantidades-al-guardar/` — hasta 2 capturas en español: Despensa con «Tomate» 3 tras la suma (verify 1) y un caso límite (99 recortado o chip en 0 conservando la cantidad).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (sin tocar Room v2 ni migraciones, la pantalla Despensa, Coincidencia/badge, detección/HUD, RD05/RD06/RD12, ni el máximo 99).
- [ ] Las decisiones D1-D5 se respetan o quedan documentadas como desviación acordada.
- [ ] Los escenarios de aceptación pasan (S1-S8) y los dos `verify` de `feature_list.json` se ejecutan en el emulador.
- [ ] Regla de suma documentada como RD13 en `docs/domain-model.md` y Relaciones/Casos límite/Validaciones coherentes (sin rastro de «se pregunta en pantalla»).
- [ ] Notas de revisión presentes en `docs/specs/escanear-despensa-con-camara.md` y `docs/specs/detectar-alimentos-en-vivo.md`.
- [ ] Barrido de restos del flujo de decisiones en `app/src` → 0 hits.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E adaptada (nuevos casos de suma, sin perder cobertura previa) y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature pasa a `passing` solo tras `lint` + `unit` + `assembleDebug` + `e2e`.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular, no se retoca `detectar-alimentos-en-vivo` ni la Despensa).
