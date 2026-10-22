# Informe de validación — `sumar-cantidades-al-guardar`

- **Fecha**: 2026-10-22 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: **`accept`**
- **Feature**: `sumar-cantidades-al-guardar` (`feature_list.json`, status `passing` → validada; **no se ha modificado `feature_list.json`**: el orquestador persiste el veredicto).
- **Spec**: `docs/specs/sumar-cantidades-al-guardar.md`.
- **Diff revisado** (working tree sin commit, base `90e2a2a`): `domain/scanner/ScanDetections.kt`, `domain/repository/PantryRepository.kt`, `ui/scanner/ScannerViewModel.kt`, `ui/scanner/ScannerScreen.kt`, `ui/scanner/ScannerRoute.kt`, `values/strings.xml`, `values-en/strings.xml`, `ScanDetectionsTest.kt`, `ScannerViewModelTest.kt`, `ScanPantryE2ETest.kt`, `docs/domain-model.md`, `docs/specs/detectar-alimentos-en-vivo.md`, `docs/specs/escanear-despensa-con-camara.md`, `docs/specs/sumar-cantidades-al-guardar.md`, `PROGRESS.md`, `docs/progress-history.md`, `feature_list.json` y 2 PNG en `docs/evidence/sumar-cantidades-al-guardar/`. Coincide con `Expected File Changes` de la spec: **sin** cambios en Room/`MIGRATION_1_2`/`PantryItemEntity`/`PantryItemDao`, ni en `RoomPantryRepository`/`InMemoryPantryRepository`, ni en la pantalla Despensa, Coincidencia/badge, detección en vivo/HUD, Gradle, harness ni `uxdocs/`.
- **Entorno de validación**: JDK 17 (`C:\Users\Daniel\.jdks\corretto-17.0.20`), Gradle del repo. Evidencia cara capturada por el implementador en `Pixel_9_Pro(AVD) - 17` (`emulator-5564`, apagado durante la validación).

## 1. Checks: reranqueados vs inspeccionados

| Check | Modo | Resultado |
|---|---|---|
| `gradlew lintDebug` | **reranqueado** (validador, 12:25-12:31) | **BUILD SUCCESSFUL**, **0 errores** (solo warnings preexistentes: versiones, `Typos` de "momento", `UnusedResources` previos, `MonochromeLauncherIcon`…) — `.harness/validator-lint-unit.log` |
| `gradlew testDebugUnitTest --rerun-tasks` | **reranqueado** (validador) | **63/63, 0 fallos, 0 errores, 0 omitidos**, 11 suites (recuento desde `app/build/test-results/testDebugUnitTest/*.xml`, 12:29). Incluye `ScanDetectionsTest` **15/15** y `ScannerViewModelTest` **9/9** con `sumIntoAddsChipQuantityToExistingQuantity`, `sumIntoTreatsNoQuantityAsAdditiveNeutral`, `sumIntoCapsTotalAtMaximumWithoutWarning`, `saveSumsMatchedChipQuantityIntoExistingPantryItem` y `saveAppliesAllSumsAndCreatesOnlyUnmatchedItems` |
| `init.ps1` | **inspeccionado (no reranqueado)** — ver §6 | Evidencia del implementador: exit 0 |
| `assembleDebug` | **inspeccionado** | Evidencia del implementador: BUILD SUCCESSFUL; corroborado porque `lintDebug`+`testDebugUnitTest --rerun-tasks` compilan fuentes debug en el árbol actual y el pase de `connectedDebugAndroidTest` de las 12:15 requirió compilar app + androidTest |
| `connectedDebugAndroidTest` (suite completa) | **inspeccionado** — suite cara, prohibido tocar otros AVD | XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_9_Pro(AVD) - 17-_app-.xml`: `tests="97" failures="0" errors="0" skipped="0" time="482.699"`, 17 clases, `ScanPantryE2ETest` **18/18** (incluye los 3 casos nuevos: `saveSumsChipQuantityIntoExistingPantryItem`, `saveAppliesAgreedQuantityEdgeCasesWithoutNewRows`, `saveWithPantryMatchesNeverShowsDecisionDialog`). Coherente con el recuento previo (96 + 1 test neto = 97) y **posterior** a todos los cambios de código (fuentes con mtime ≤ 11:11:32; XML 12:15) |
| Barrido de restos | **reranqueado** (grep) | `PantryDuplicateResolution`, `resolveDuplicate`, `DUPLICATE_DIALOG/TITLE/MESSAGE/KEEP/UPDATE`, `scanner_duplicate_{title,message,existing_quantity,keep,update}`, `scanner_no_quantity`, `openDuplicate`, `dismissDuplicate`, `pendingSave`, `suppressedAutoPrompts`, `duplicatePromptQueue`, `activeDuplicateItemId`, `duplicateDecisions` en `app/src` → **0 hits** (solo sobreviven `scanner_duplicate_error` y el prefijo de tag `scanner_duplicate_mark`, ambos previstos por la spec) |
| Paridad de cadenas es/en | **reranqueado** | 177 cadenas en cada locale; `scanner_already_in_pantry` presente en ambos, cadenas del diálogo ausentes en ambos |

## 2. Ítems de verificación de `feature_list.json`

| Ítem | Resultado | Evidencia |
|---|---|---|
| **Verify 1 — E2E: Despensa «Tomate» 1 + chip «Tomate» 2 ⇒ 3** | **pasa** | `ScanPantryE2ETest.saveSumsChipQuantityIntoExistingPantryItem` (en la suite 97/97): Despensa `PantryItem("tomato-id","TOMATE fresco",1)`, chip «Tomate fresco» incrementado a 2, guardado sin diálogo ⇒ 1 sola fila, `id = tomato-id`, `name = "TOMATE fresco"` (D4) y `quantity = 3`. Refuerzo visual: `docs/evidence/sumar-cantidades-al-guardar/despensa-suma-es.png` (Despensa en español con una única fila «Tomate / Cantidad: 3»). |
| **Verify 2 — E2E: casos límite sin filas nuevas** | **pasa** | `ScanPantryE2ETest.saveAppliesAgreedQuantityEdgeCasesWithoutNewRows` (97/97): sin cantidad + 2 ⇒ **2**; 3 + 0 ⇒ **3**; 60 + 50 ⇒ **99**; sin cantidad + 0 ⇒ **sin cantidad**; tamaño final 4 (ninguna fila nueva). Cubre los 3 casos límite acordados en un único guardado (S7). Unit de respaldo: `sumIntoTreatsNoQuantityAsAdditiveNeutral` y `sumIntoCapsTotalAtMaximumWithoutWarning` (63/63). |

**Limitación declarada**: los dos `verify` no se han reejecutado a mano por el validador. `init.ps1` y los E2E exigen un dispositivo, y en el entorno solo hay conectados `emulator-5556` (Pixel_8_Pro) y `emulator-5558` (Pixel_10_Pro), **en uso por otras sesiones**; tocar cualquiera de ellos habría instalado/actualizado la app y roto sus suites. El AVD de la feature (`Pixel_9_Pro`, `emulator-5564`) está apagado. Se acepta como base la evidencia cara **inspeccionada**, que es exacta, completa y consistente con el diff actual (comando, resultado, recuento, dispositivo, fecha y postdatado respecto a los fuentes), tal y como permite la skill para el tier caro.

## 3. Escenarios S1-S8

| Escenario | Resultado | Evidencia |
|---|---|---|
| S1 suma al guardar, sin diálogo, sin fila nueva | pasa | Verify 1 (E2E + captura) |
| S2 existente sin cantidad + chip 2 ⇒ 2 | pasa | E2E `saveAppliesAgreedQuantityEdgeCasesWithoutNewRows` (tomate null + 2 → 2) |
| S3 chip 0 conserva la cantidad / 0+0 ⇒ sin cantidad | pasa | E2E (leche 3 + 0 → 3; sal null + 0 → null) + unit `sumIntoTreatsNoQuantityAsAdditiveNeutral` |
| S4 60 + 50 ⇒ 99 sin aviso | pasa | E2E (arroz 60 + 50 → 99) + unit `sumIntoCapsTotalAtMaximumWithoutWarning` |
| S5 ninguna pregunta en el flujo; marca informativa | pasa | `saveWithPantryMatchesNeverShowsDecisionDialog` (marca visible, `assertHasNoClickAction`, sin diálogo) + retirada total del estado/UI del diálogo (0 hits) |
| S6 chips sin coincidencia se crean como antes | pasa | `manualFoodCanBeAddedWithQuantityAndSaved` y `savePersistsChosenQuantityAndShowsPantryScreen` verdes en la suite 97/97; `toPantryItem` intacto |
| S7 varias coincidencias en un único guardado | pasa | E2E con 4 coincidencias en un solo «Guardar en la Despensa» + unit `saveAppliesAllSumsAndCreatesOnlyUnmatchedItems` (2 coincidencias + 1 alta) |
| S8 textos es/en sin restos del diálogo | pasa (con L1) | Marca en español asertada con `locale(R.string.scanner_already_in_pantry)`; paridad 177/177 y barrido 0 hits; el lado inglés de la marca no tiene aserción E2E específica (ver **L1**) |

## 4. Decisiones D1-D5

| Decisión | Cumplimiento |
|---|---|
| D1 «sin cantidad» = neutro aditivo | **Cumplida**: `(existing.quantity ?: 0) + draft.quantity`, `0 → null` en `ScanDetections.sumInto` (líneas 96-105), con unit de las tres combinaciones |
| D2 tope 99 sin aviso | **Cumplida**: `total.coerceAtMost(PantryItemValidation.MAX_QUANTITY)`; sin aviso en UI; máximo global intacto (`PantryItemValidation` sin cambios) |
| D3 sin diálogo | **Cumplida**: retirados `DuplicateResolutionDialog`, tags de decisión, callbacks y toda la cola/estado de decisiones; la marca pasa de `TextButton` (48 dp) a `Text` con color `secondary` (sin acción táctil) |
| D4 id y nombre existentes | **Cumplida**: `existing.copy(quantity = …)`; E2E comprueba `id = tomato-id` y `name = "TOMATE fresco"` |
| D5 salvaguarda `DuplicateName` | **Cumplida**: `persistItems` conserva el aviso `DUPLICATE_NAME` y aborta el resto; unit `duplicateRepositoryResultIsNotReportedAsSuccessfulSave` verde |

## 5. Alcance, arquitectura, seguridad y documentación

- **Alcance**: sin trabajo de otras features. No se toca Room v2 ni migraciones, Despensa, Coincidencia/badge, detección en vivo, HUD, muestreo de 300 ms, RD05/RD06/RD12 ni el máximo 99. La retirada del enum `PantryDuplicateResolution` es la prevista en `Expected File Changes` y su único consumidor era el flujo eliminado.
- **Arquitectura**: la regla vive en `domain/scanner/ScanDetections.kt` como función pura (`sumInto`) y la UI queda sin lógica de negocio; MVVM/StateFlow intacto; el ViewModel solo orquesta `matches` → `sumInto` → `savePantryItem`. `matches` se conserva para la marca y se recalcula en alta/renombrado/borrado y en cada emisión de la Despensa. Sin dependencias nuevas.
- **Seguridad (checklist feature-scoped)**: **sin hallazgos**. Sin secretos ni PII en el diff, tests, capturas (datos ficticios «Tomate») ni docs; sin red, permisos, ficheros, dependencias ni scripts nuevos; entrada de cantidad validada en el dominio (`coerceIn(0..99)` en el chip, recorte de la suma, `require` de rango en el repositorio); guardia de duplicado conservada; sin cambios en autenticación (no aplica, P31) ni en exposición de datos; sin logs nuevos.
- **Documentación durable**: `docs/domain-model.md` incorpora **RD13** y actualiza Relaciones, «Cantidad propuesta» y el caso límite de duplicado (ya no queda «se pregunta en pantalla»); notas de revisión de una línea en `docs/specs/escanear-despensa-con-camara.md` y `docs/specs/detectar-alimentos-en-vivo.md`; `Q-sumar-cantidades-al-guardar-1..4` figuran resueltas en `docs/risks-and-open-questions.md`; `AGENTS.md`, `CONTEXT.md` y `DESIGN.md` no requerían cambios (y no se tocaron). Sin `ARCHITECTURE.md`/`CONSTRAINTS.md` en el repo, según declara la spec.
- **Handoff**: `PROGRESS.md` (Estado verificado + Siguiente paso) y `feature_list.json` (`passing` + `evidence`) reflejan el estado real; `docs/progress-history.md` tiene la entrada de la sesión 54. Evidencia dentro del presupuesto (1 informe + 2 PNG) y solo de esta feature.

## 6. Hallazgos y notas

### L1 · El lado inglés de la marca «Already in the Pantry» no tiene aserción E2E

- **Severidad**: Low (no bloqueante)
- **Evidencia**: `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt:251` — `englishLabelsAndScannerActionsAreLocalized()` llama a `launchScanner(...)` sin `existing`, así que no hay coincidencia y la marca no se renderiza; ningún otro E2E comprueba la marca en inglés.
- **Por qué importa**: S8 pide que la marca se vea «en el idioma activo». Hoy la garantía es indirecta (paridad 177/177 de `strings.xml` y `stringResource`), suficiente para aceptar, pero una regresión de traducción o de visibilidad de la marca en inglés no la detectaría la suite.
- **Cambio requerido**: ninguno para aceptar. Recomendado: ampliar el caso inglés con una coincidencia y aserción de texto.
- **Implementación sugerida**: (1) en `englishLabelsAndScannerActionsAreLocalized`, añadir `existing = listOf(PantryItem("tomato-id", "Tomato", 1))` y una detección «Tomato»; (2) `onNodeWithTag(ScannerTestTags.duplicateMark(id)).assertTextEquals("Already in the Pantry").assertHasNoClickAction()`.
- **Verificación tras el arreglo**: `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.ScanPantryE2ETest` en verde (19/19).

### L2 · La segunda captura no documenta visualmente la cantidad conservada tras guardar

- **Severidad**: Low (nota de evidencia)
- **Evidencia**: `docs/evidence/sumar-cantidades-al-guardar/scanner-caso-limite-es.png` muestra el Escáner antes de guardar (chip «Tomate», cantidad 0, marca «Ya está en la Despensa»); no hay captura de la Despensa posterior con la cantidad 3 conservada.
- **Por qué importa**: el caso «chip 0 conserva la cantidad» queda evidenciado por el E2E (`saveAppliesAgreedQuantityEdgeCasesWithoutNewRows`: leche 3 + 0 → 3) y por la unidad de `sumInto`, no por la imagen; la afirmación de `PROGRESS.md` («chip 0 conserva la cantidad 3») se apoya en el E2E.
- **Cambio requerido**: ninguno. Recomendado en un futuro repaso: recapturar la Despensa tras ese guardado o ajustar el pie de foto.

### Notas no bloqueantes

- **N1**: `docs/specs/detectar-alimentos-en-vivo.md` líneas 23 y 155 siguen afirmando que el diálogo se conserva «tal cual»; la nota de revisión de cabecera (línea 11) aclara la sustitución, que es exactamente lo que pedía el `Durable Documentation Impact` («nota de revisión de una línea, sin reescribir su historia»). Opcional: añadir «(ver nota de revisión)» junto a esas frases.
- **N2**: `docs/progress-history.md` termina sin salto de línea final (cosmético).
- **N3**: `.kotlin/sessions` (artefacto del build de Kotlin, vacío, sin ignorar) sigue en la raíz y ya era untracked antes de este delta; cualquier compilación lo regenera. Conviene añadir `.kotlin/` a `.gitignore` en un commit `chore`/`docs` fuera del alcance de esta feature (AGENTS pide no dejar temporales al cerrar la sesión).
- **N4 (limitación de la validación)**: `init.ps1` **no se ha reranqueado**. No es un gate de calidad (ejecuta `gradlew --version` + `installDebug` + `monkey` + `pidof`), instala y lanza la app en el **primer** dispositivo de `adb`, y en este entorno solo hay AVD de otras sesiones (`emulator-5556`, `emulator-5558`): ejecutarlo habría interferido con ellas. El AVD de la feature está apagado. Se inspecciona el script (no solo imprime comandos: compila, instala y verifica; no arranca procesos duraderos) y se toma la evidencia declarada del implementador (exit 0). El falso fallo conocido de `pidof` tras `installDebug` sigue documentado en `PROGRESS.md`.

## 7. Veredicto

**`accept`** — La implementación cumple Goal, S1-S8 y D1-D5, respeta los Non-Goals, sustituye por completo el flujo de decisión sin dejar restos, añade la cobertura E2E/unit esperada y deja la documentación durable y el handoff coherentes. Los checks baratos se han **reranqueado en verde** sobre el árbol actual (`lintDebug` 0 errores, `testDebugUnitTest --rerun-tasks` 63/63) y la evidencia cara se ha **inspeccionado** (`connectedDebugAndroidTest` 97/97, 0 fallos/errores/omitidos; `ScanPantryE2ETest` 18/18 con los dos `verify`; capturas en español revisadas). Únicos hallazgos: 3 Low no bloqueantes.

**Estado recomendado** (lo persiste el orquestador, no este informe): `feature_list.json` → `status: "accepted"` con un resumen de evidencia que cite el informe `docs/validations/sumar-cantidades-al-guardar.md`, `lintDebug` 0 errores, `testDebugUnitTest` 63/63, `connectedDebugAndroidTest` 97/97 (0 fallos) y los dos `verify` cubiertos por E2E; y actualizar *Siguiente paso* de `PROGRESS.md`.