# Informe de validación — Mostrar recetas compatibles

- **Feature**: `mostrar-recetas-compatibles`
- **Spec**: `docs/specs/mostrar-recetas-compatibles.md`
- **Rama/estado**: `feature/mostrar-recetas-compatibles`, diff sin commitear sobre `90e2a2a`; `feature_list.json` decía `passing` al iniciar la validación
- **Validador**: `feature-validator` (sesión 54, 2026-10-22)
- **Veredicto**: **accept** → recomendado `accepted`

## Resumen

La sección «Recetas Compatibles» del Escáner cumple la spec: selecciona las Recetas con
Coincidencia ≥ 85 % sobre el porcentaje redondeado (RD02/D2), las ordena por Coincidencia
descendente → título normalizado → `id` (D6), incluye Borradores y Publicadas (D4), excluye las
Recetas sin obligatorios (D3) y se recalcula de forma reactiva contra la Despensa persistida
(D1/RF-MRC9). La tarjeta reutiliza `MatchBadge`, muestra miniatura o placeholder, título a una
línea y «X de Y ingredientes en la Despensa»; el tap abre el Detalle y el estado vacío ofrece
«Sugerir receta con IA» hacia el destino provisional. Los non-goals se respetan.

## Checks baratos reranqueados por el validador

Ejecutados el 2026-10-22 en el árbol actual, con
`JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20` y el emulador `Pixel_9_Pro` (serial
`emulator-5562`, API 37) arrancado.

| Check | Comando | Resultado |
| --- | --- | --- |
| Arranque | `cmd /c "set JAVA_HOME=...&& powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1"` | **exit 0**; instaló la APK actual en `Pixel_9_Pro`, `adb shell monkey` inyectó el evento y terminó con «Entorno listo» / «App … en ejecucion». No arrancó procesos duraderos. |
| Lint | `gradlew.bat lintDebug` | **BUILD SUCCESSFUL**, 0 errores (1 task ejecutada; warnings preexistentes de `viewModel` de Koin). |
| Unit | `gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**, 26 tasks ejecutadas; **67/67 tests, 0 fallos, 0 errores, 0 omitidos** (12 suites desde XML). `CompatibleRecipesTest` **6/6**, `ScannerViewModelTest` **9/9**, `RecipeMatchTest` **5/5**. |
| Build | `gradlew.bat assembleDebug --rerun-tasks` | **BUILD SUCCESSFUL**, 38 tasks ejecutadas; APK de debug empaquetada. |

## Evidencia cara inspeccionada (no se repitió la suite completa)

- **Suite completa `Pixel_9_Pro` vía `am instrument`: 103/103 tests, 0 fallos**,
  `.harness/e2e-pixel9-full.log` (timestamp 2026-10-22 13:07, `Time: 647.974`, `OK (103 tests)`).
  Incluye los 7 casos de `CompatibleRecipesE2ETest` y la regresión completa; es coherente con el
  diff actual (mismo árbol desde el último cambio de código).
- **Feature focalizada `Pixel_9_Pro`: 7/7**, `.harness/e2e-pixel9-focused.log`
  (`Time: 21.918`, `OK (7 tests)`).
- **Feature focalizada `Pixel_8_Pro` vía Gradle: 7/7**, `.harness/e2e-pixel8c.log`
  (`BUILD SUCCESSFUL`, `Finished 7 tests`).
- **`Pixel_8_Pro` completa: 102/103**, `.harness/e2e-pixel8-full.log`: único fallo
  `PantryManagementE2ETest.deleteCanBeCancelledOrConfirmed` (ajeno a esta feature; la spec y el
  historial lo registran como flake y pasó en solitario). El log de `.harness/e2e-pixel8-full2.log`
  contiene además interrupciones/crashes de teardown y reinstalaciones concurrentes de otra worktree;
  no afectan al veredicto porque el pase completo limpio es el de `Pixel_9_Pro`.
- No quedan XML de `connectedDebugAndroidTest` en `app/build/outputs/androidTest-results/connected/debug/`
  (el árbol de build se limpió/recompiló después); la evidencia disponible son los logs anteriores,
  con recuentos y comandos exactos. Esto se registra como nota, no como bloqueo.
- **Reranqueo focalizado del validador** (además, para capturar las 2 imágenes):
  `am instrument -e class com.whatsforlunch.app.CompatibleRecipesE2ETest#compatibleRecipesAreOrderedByMatchWithBadgesAndIngredientBreakdown`
  → **1/1 OK (3.803 s)** y `…#recipesBelowThresholdAreNotListed` → **1/1 OK (3.856 s)**.
  No hubo `killDueToPackageUpdate` durante estas ejecuciones.

## Verificación visual/UI (S1 y S4, español)

- **Estado con tarjetas** (`docs/evidence/mostrar-recetas-compatibles/escaner-compatibles-100-85.png`):
  render real de `ScannerScreen` en español con el `CompatibleRecipesE2ETest` (repos in-memory,
  permiso de cámara denegado) mostrando, en este orden vertical:
  1. «Borrador compatible» — `100% Match` — «2 de 2 ingredientes en la Despensa» (D4, empate a 100 % resuelto por título);
  2. «Tortilla de patatas» — `100% Match` — «4 de 4 ingredientes en la Despensa»;
  3. «Pechuga al horno» — `85%` — «17 de 20 ingredientes en la Despensa».
  La cabecera «Recetas Compatibles» está presente, no hay icono `auto_awesome` ni «Motor SQL Inteligente»,
  y las miniaturas sin foto usan el placeholder `SkilletIcon`. La captura se tomó de la pantalla Compose
  real del test porque el Escáner solo es alcanzable desde Mis Recetas cuando esta está vacía (comportamiento
  existente); el orden, badges y textos también están asertados en el E2E (bounds + `assertTextEquals`).
- **Estado vacío** (`docs/evidence/mostrar-recetas-compatibles/escaner-compatibles-vacio.png`):
  «Recetas Compatibles», «Aún no hay recetas compatibles con tu Despensa.» y botón
  «Sugerir receta con IA» (altura ≥ 52 dp), en español y sin recetas compatibles.
- **Comprobación con `uiautomator` en la app real** (`Pixel_9_Pro`, locale `es-ES`):
  desde Mis Recetas vacía → «Escanear la Despensa» → `uiautomator dump` devolvió
  `Recetas Compatibles`, `Aún no hay recetas compatibles con tu Despensa.`, `Sugerir receta con IA`,
  `Nombre del alimento`, `Añadir alimento`, `Volver`, `Escáner de la Despensa`; al pulsar el botón se
  abrió el destino con título `Sugerir receta con IA` y `Volver`, y al volver se regresó al Escáner.
  Esto valida el cableado real de `AppNavHost`/`ScannerRoute` para RF-MRC8 y la vuelta atrás.
- **Tags**: los testTags nuevos (`COMPATIBLE_SECTION`, `COMPATIBLE_TITLE`, `compatibleCard(id)`,
  `compatibleMatch(id)`, `compatibleIngredients(id)`, `COMPATIBLE_EMPTY`, `COMPATIBLE_SUGGEST_AI`)
  existen en `ScannerTestTags` y los usa la UI; el E2E los consulta y pasa. `uiautomator` no expone
  testTags de Compose (solo texto/content-desc), por eso la comprobación de tags se apoya en el E2E.

## Reproducción de la spec

| Requisito / decisión | Cómo se cumple / evidencia |
| --- | --- |
| S1 / RF-MRC1, RF-MRC2, RF-MRC6 / D2, D6 | Unit `keepsRecipesAtOrAboveRoundedThresholdOrderedByMatchThenTitleThenId` (100/85, empate y título); E2E S1 (bounds y badges) + captura. |
| S2 / RF-MRC1 | Unit `ignoresRecipesBelowThresholdIncluding84` y `usesRoundedPercentageForThresholdSoElevenOfThirteenEnters` (84 fuera, 11/13 → 85 dentro); E2E `recipesBelowThresholdAreNotListed` (84 % y 50 %). |
| S3 / RF-MRC9 / D1 | E2E `sectionUpdatesReactivelyWhenPantryReachesThreshold` (entra al añadir el Alimento, sin relanzar la pantalla); `ScannerViewModel` combina `observeRecipes` + `observePantryItems`. |
| S4 / RF-MRC8 / D8 | E2E `emptyStateOpensAiSuggestionsProvisionalScreen` + verificación real con `uiautomator` (botón → provisional → Volver); captura del estado vacío. |
| S5 / RF-MRC7 / D7 | E2E `tappingCompatibleCardOpensRecipeDetailAndBackReturnsToScanner` (callback con el `id` correcto, Detalle real y vuelta); cableado `AppNavHost` inspeccionado. |
| S6 / RF-MRC3 / D4 | Unit `draftsAndPublishedRecipesEnterEqually`; E2E S1 incluye un Borrador al 100 % visible con una Publicada. |
| S7 / RF-MRC4 / D3 | Unit `recipesWithoutRequiredIngredientsNeverEnter`; E2E `recipeWithoutRequiredIngredientsNeverAppears`. |
| S8 | E2E `compatibleSectionIsLocalizedInEnglish` (cabecera, vacío, botón y línea de ingredientes en inglés). |
| RF-MRC5 / D5 | Lista vertical de tarjetas bajo los chips, dentro del único scroll del Escáner; captura y E2E. |
| RF-MRC10 / RD05 | E2E monta el Escáner con `onCameraPermissionDenied()` y la sección sigue visible y operativa; en la app real también se ve sin cámara. |
| Casos límite | 85 exacto entra (unit); porcentaje redondeado 85 entra (unit 11/13); sin obligatorios nunca entra (unit + E2E); cantidad 0 cuenta como disponible (unit); chips sin guardar no alteran el cálculo (el cálculo usa `pantryItems` persistidos); sin foto → `SkilletIcon`; sin límite/paginación. |

## Non-goals y disciplina de alcance

- No se tocaron `calculateMatch`, `RecipeMatch`, `MatchBadge`, el badge de Mis Recetas ni el Detalle;
  tampoco Room, el esquema, la Despensa, el Escaneo ni el formulario de Recetas (`git diff --stat`).
- Sin IA real, sin OpenRouter/API key ni llamadas de red: el botón solo abre `ProvisionalScreen`.
- Sin Lista de la Compra, ingredientes faltantes, Modo Cocina ni botón de carrito.
- Sin iconos ni dependencias nuevas. Sin cambios en `uxdocs/`, `.opencode/` ni scripts del harness.
- El diff se limita a los ficheros de `Expected File Changes` de la spec (más los tests y docs previstos).

## Revisión de calidad y riesgo (rúbrica)

- **Correctitud**: RF-MRC1..10 y D1..D9 implementados y verificados; S1-S8 cubiertos.
- **Verificación**: checks baratos reranqueados en el árbol actual y evidencia cara inspeccionada con
  recuentos exactos; el E2E de la feature se ejecutó en dos AVD (7/7) y la suite completa 103/103.
- **E2E**: cobertura persistente añadida (`CompatibleRecipesE2ETest`, 7 casos) además de unit de la
  regla pura. El E2E monta `ScannerScreen`/`RecipeDetailScreen`/`ProvisionalScreen` directamente con
  repos in-memory, siguiendo el patrón existente; el cableado real de navegación se verificó a mano
  para el camino de IA. Ver nota L3.
- **Arquitectura**: regla de selección/orden pura en `domain/match/`, ViewModel MVVM+StateFlow, UI sin
  reglas de negocio, Koin actualizado; cumple los patrones del repo.
- **Mantenibilidad**: código pequeño y legible, comentarios con RF/RD; tests deterministas.
- **Seguridad**: ver abajo.

## Seguridad (checklist por feature)

**Sin hallazgos.** El diff no añade secretos, credenciales, PII ni llamadas de red; no introduce
permisos ni dependencias; no escribe datos nuevos en disco (la miniatura solo lee `photoPath`, patrón
ya existente en Mis Recetas); no expone datos privados en logs ni capturas (las capturas usan recetas
y despensa ficticias del harness). No hay superficie de autenticación/autorización en esta feature
(usuario único local, P31). La decodificación de la foto es local y tolera errores (`runCatching`).

## Documentación durable y handoff

- `docs/risks-and-open-questions.md`: `Q-mostrar-recetas-compatibles-1..4` registradas como
  **resueltas por el usuario el 2026-10-22** (D4, D5, D7, D8), con la spec como origen.
- `feature_list.json`: `status: passing` con evidencia de gates y E2E; no declara aceptación. Es
  coherente con lo reranqueado e inspeccionado. Recomendación: pasar a `accepted` con el resumen de
  este informe.
- `PROGRESS.md` y `docs/progress-history.md`: estado de implementación y siguiente paso
  (`feature-validator`) actualizados; el detalle de gates coincide con los logs. No se detectan
  afirmaciones falsas. Matiz: el historial cita el pase Gradle completo de `Pixel_8_Pro` con el flake
  de Pantry, pero no menciona los crashes/reintentos de `e2e-pixel8-full2.log`; quedan cubiertos por
  la nota de reinstalaciones concurrentes y por el pase limpio 103/103 de `Pixel_9_Pro`.
- `ARCHITECTURE.md` / `CONSTRAINTS.md` / `AGENTS.md`: no requeridos por la spec (no existen o no
  cambian capas/convenciones). `docs/domain-model.md`: no requerido; RD02 ya definía umbral y badge.

## Findings bloqueantes

Ninguno. **0 Critical, 0 High, 0 Medium.**

### Notas no bloqueantes (follow-up)

1. **(L1) Flash del estado vacío al cargar**: `ScannerScreen` pinta la sección con
   `compatibleRecipes = emptyList()` antes de que `pantryItems` y Recetas hayan emitido, así que puede
   verse «Aún no hay recetas compatibles…» durante un instante al abrir el Escáner con datos. No rompe
   ningún requisito (el estado converge solo) pero sería más pulido gatear la sección con
   `pantryLoaded`/carga de Recetas. No bloquea la aceptación.
2. **(L2) Virtualización**: las tarjetas se componen en un `Column` dentro de un único `item` del
   `LazyColumn`, no como `items`; con muchas Recetas se componen todas a la vez. El MVP no exige
   paginación y la lista hace scroll correctamente; mejora futura.
3. **(L3) Cobertura del NavHost**: el E2E de la feature simula la navegación con estado local
   (`showDetail` / `showAiSuggestions`) en lugar de montar `AppNavHost`; el callback del `id` y las
   pantallas reales sí se verifican, y el camino real de IA se comprobó con `uiautomator` en la app.
   El camino real tarjeta→Detalle queda verificado por inspección del cableado (`ScannerRoute` →
   `AppNavHost.recipeDetail(id)`), no por E2E de navegación. No bloquea, pero es la mejora natural de
   cobertura si se quiere cerrar del todo.
4. **(L4) Evidencia de la suite completa**: los XML de `connectedDebugAndroidTest` ya no están en el
   árbol de build; la evidencia son los logs `.harness/e2e-pixel9-full.log` (103/103) y los
   focalizados. Los comandos y recuentos son exactos y consistentes con el diff.

## Persistencia recomendada

- `feature_list.json`: `mostrar-recetas-compatibles` → `accepted`, con evidencia resumida:
  `init/lint/unit(67/67)/assemble` en verde; `CompatibleRecipesE2ETest` 7/7 en Pixel_9_Pro y 7/7 en
  Pixel_8_Pro; suite completa 103/103 en Pixel_9_Pro (648 s); 2 capturas e informe de validación.
- `PROGRESS.md`: mover la feature a aceptada y dejar como siguiente paso `sumar-cantidades-al-guardar`
  (spec ya redactada) o la prioridad que decida el usuario.

## Enmienda posterior (2026-10-22, implementer)

- **Origen**: petición del usuario — aunque haya Recetas compatibles, debe seguir ofreciéndose
  «Sugerir receta con IA». Ajuste de producto sobre `RF-MRC8`/`D8`; no cambia la regla de dominio.
- **Cambios**: `CompatibleRecipesSection` mantiene el botón siempre visible (debajo de las tarjetas o
  del estado vacío); la spec añade S9 y actualiza RF-MRC8/D8; nuevo E2E
  `aiSuggestionIsAvailableWhenCompatibleRecipesExist`. Se corrigió además la firma obsoleta de
  `ScannerScreen` en `CompatibleRecipesE2ETest` (parámetros del diálogo de duplicados retirados por
  `sumar-cantidades-al-guardar`), que impedía compilar el `androidTest`.
- **Verificación de la enmienda**: `lintDebug` 0 errores; `testDebugUnitTest --rerun-tasks` 69/69;
  `assembleDebug` BUILD SUCCESSFUL; `CompatibleRecipesE2ETest` **8/8** en `Pixel_9_Pro` (API 37),
  incluido el caso nuevo. No se repitió la suite completa.
- **Estado**: la feature volvió a `passing` hasta la validación independiente de la enmienda; el
  veredicto `accept` anterior cubría S1-S8 y la enmienda añade S9 sobre RF-MRC8.

### Validación independiente de la enmienda (2026-10-22, `feature-validator`)

- **Veredicto: `accept`** → `feature_list.json` a `accepted` con la evidencia de esta validación.
- **Alcance revisado**: diff de 5 ficheros (`ScannerScreen`, `CompatibleRecipesE2ETest`, spec, este
  informe y `feature_list.json`). El botón `COMPATIBLE_SUGGEST_AI` sale de la rama del estado vacío y
  queda al final de la `Column` de la sección, así que se pinta bajo el texto vacío y también bajo las
  tarjetas; el callback `onSuggestWithAi` y el cableado `AppNavHost → ai_suggestions_provisional` no
  cambian. La corrección de la firma de `ScannerScreen` en el E2E retira parámetros del diálogo de
  duplicados ya inexistentes (grep de `onOpenDuplicate`/`onResolveDuplicate`/`onDismissDuplicate` en
  `app/src` = 0 hits). Sin cambios de dominio, textos, Room, permisos ni red. S9/RF-MRC8/D8 cumplidos.
- **Checks baratos reranqueados** (árbol actual, `JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20`):
  `lintDebug` **0 errores** (43 warnings preexistentes) · `testDebugUnitTest --rerun-tasks` **69/69**
  (12 suites; `CompatibleRecipesTest` 6/6) · `assembleDebug` BUILD SUCCESSFUL.
- **Evidencia cara reejecutada** en `Pixel_9_Pro` API 37 (`emulator-5554`): `CompatibleRecipesE2ETest`
  **8/8**, 0 fallos/omitidos (19,9 s), incluido `aiSuggestionIsAvailableWhenCompatibleRecipesExist`;
  regresión del Escáner `ScanPantryE2ETest` **18/18** (1m 19s). Un primer pase de `ScanPantryE2ETest`
  abortó con `Process crashed` durante `liveFramesShowFoodOnceWithBestConfidenceAndHudLabel`; el caso
  pasó en solitario (1/1) y la clase completa repitió 18/18, patrón del crash intermitente del emulador
  ya documentado.
- **Findings**: 0 Critical/High/Medium. **1 Low**: el E2E de S9 no asierta la vuelta atrás al Escáner
  que menciona el escenario; el retorno está cubierto por S4 sobre el mismo `ProvisionalScreen` y el
  cableado real se validó en la aceptación anterior. Nota de proceso: el implementer escribió en `notes`
  de `feature_list.json` (campo reservado a `status`/`evidence`); esta validación lo restauró a su valor
  aceptado.
- **Documentación**: spec (Goal, RF-MRC8, S4, S9, D8, impacto visual, trazabilidad y checklist) e informe
  son coherentes entre sí y con el código. `PROGRESS.md` todavía cita la aceptación anterior; el
  orquestador debe actualizar su cabecera en el cierre de sesión.
