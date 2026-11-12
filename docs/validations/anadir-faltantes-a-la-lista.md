# Validación independiente — `anadir-faltantes-a-la-lista`

- **Fecha**: 2026-10-26
- **Rol**: `feature-validator` (subagente `validator`)
- **Spec**: `docs/specs/anadir-faltantes-a-la-lista.md`
- **Estado declarado al validar**: `passing` (autoverificación); reparación posterior sobre `in_progress`
- **Veredicto inicial (sesión 84)**: **`revise`**
- **Veredicto vigente (revalidación, sesión 85)**: **`accept`**

## 1. Objetivo de revisión

- Feature seleccionada: `anadir-faltantes-a-la-lista` (primera `passing` lista para validar; sus dependencias `crear-receta` y `gestionar-alimentos-de-la-despensa` están `accepted`).
- Diff revisado: árbol de trabajo sin commitear (31 ficheros modificados y ficheros nuevos sin trackear).
- `feature_list.json` solo cambia `status` y `evidence` respecto a la base.
- Evidencia inspeccionada: `docs/evidence/anadir-faltantes-a-la-lista/informe.md`, 2 PNG y el XML de `connectedDebugAndroidTest`.

## 2. Checks reejecutados e inspeccionados

### Reejecutados sobre el árbol actual

| Check | Resultado |
|---|---|
| `gradlew :app:lintDebug :app:testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (46 s) |
| `unit` | **203/203 en 33 suites**, 0 fallos, 0 errores, 0 omitidos |
| `lintDebug` | **0 errores / 53 warnings** |
| `assembleDebug` | **BUILD SUCCESSFUL** |
| Paridad es/en | **265/265 claves**, sin claves solo-es, solo-en ni duplicadas |
| PNG de evidencia | Cabecera `89 50 4E 47 0D 0A 1A 0A`, 1280×2856; revisados visualmente |

Suites relevantes: `ShoppingListRulesTest` 10/10, `InMemoryShoppingListRepositoryTest` 3/3, `ShoppingListViewModelTest` 3/3, `PincheViewModelTest` 12/12, `RecipeDetailViewModelTest` 9/9, `RecommendationsViewModelTest` 6/6 y `PincheProposalRulesTest` 6/6.

Nota: `lint.ps1` y `unit.ps1` devolvieron exit 0 pero Gradle los marcó `UP-TO-DATE`; la evidencia válida es la ejecución con `--rerun-tasks`.

### Inspeccionados, no reejecutados

| Check | Evidencia |
|---|---|
| `e2e.ps1` / `connectedDebugAndroidTest` | XML `TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`, 2026-10-26 20:16:15: **163 tests, 0 fallos, 0 errores, 2 omitidas**, 1.713,442 s; `ShoppingListE2ETest` 9/9. Coherente con el informe y el diff; emulador apagado, por lo que no se reejecutó. |
| `init.ps1` | No reproducible en esta sesión: `adb devices` no muestra dispositivo y el script sale en `adb get-state`. El `exit 0` del implementador queda como evidencia inspected-only. No es un hallazgo contra la feature. |
| Migración 2→3 | Test E2E que crea una BD v2 con Receta y Despensa, abre v3, conserva los datos y comprueba las dos tablas nuevas. |
| Alcance, arquitectura, seguridad y documentación | Inspección del diff completo, textos, testTags, manifest, dependencias y docs. |

## 3. Evaluación contra la spec

| Escenario | Estado |
|---|---|
| S1 · Detalle con Faltantes → Lista con Origen | ✅ `detailAddsMissingItemsToTheShoppingListWithOrigin` |
| S2 · repetir no duplica ni dobla | ✅ `repeatingTheActionDoesNotDuplicateOrDoubleTheItems` (Room real en proceso) |
| S3 · dos Recetas suman y badge cuenta fuentes | ✅ `twoRecipesAddUpAndTheBadgeCountsSources` |
| S4 · tarjeta de «Recetas con mi Despensa» | ✅ `recommendationCardAddsItsMissingItems` |
| S5 · Ficha del Pinche + guardar + repetir desde el Detalle del Borrador | ⚠️ **Parcial**: falta la cláusula final (guardar y repetir desde el Detalle) |
| S6 · borrado con «Deshacer» | ✅ UI in-memory; ruta Room de borrado/restauración solo inspeccionada |
| S7 · Lista vacía + CTA al Escáner | ✅ `emptyListShowsTheScanPantryCta` |
| S8 · sin Faltantes no hay acción | ✅ `fullMatchHidesTheAddActions` |
| Migración 2→3 | ✅ existencia y conservación de datos |

RF-AFL1..8: cumplidos salvo el matiz de cobertura de RF-AFL7 (persistencia verificada en proceso, sin prueba de reinicio). Alcance respetado: sin alta manual/voz, Pasillo, «Despensa baja», tabs, checkboxes, progreso, IA ni cambios en RD01/RD02/RD10/RD13. Arquitectura conforme: reglas puras en `domain/shoppinglist`, repositorio en `domain/repository`, Room en `data` y MVVM en `ui`.

## 4. Hallazgos

### H1 — S5 no está verificado de extremo a extremo

- **Severidad**: High
- **Evidencia**: `ShoppingListE2ETest.kt:255-288` pulsa «Añadir faltantes», comprueba «Añadido…» deshabilitado y consulta el repositorio in-memory, pero no guarda la Propuesta, no abre la Lista y no repite desde el Detalle del Borrador. El `informe.md:37` marca S5 como ✅. La spec (`docs/specs/anadir-faltantes-a-la-lista.md:72-76`) exige ese flujo completo.
- **Por qué importa**: es el punto donde se cruzan RD16 (id estable de la Propuesta/Borrador) y RD18 (idempotencia por fuente). La lógica es correcta por composición (`PincheProposalRules.proposalToRecipe` usa `proposalRecipeId`, y el Detalle usa `recipe.id`), pero la evidencia no demuestra el flujo en la app.
- **Cambio requerido**: añadir cobertura E2E o de integración de ViewModels que ejecute Ficha → Añadir faltantes → Guardar → Detalle del Borrador → Añadir a la lista → Lista, y demuestre un solo ítem, cantidad sin doblar, una sola fuente y badge «Para: {título}».
- **Verificación tras el arreglo**: `ShoppingListE2ETest` focalizado (o unit de integración) + `e2e.ps1` completo; actualizar informe y recuentos.

### L1 — Warnings nuevos de lint y afirmación de evidencia inexacta

- **Severidad**: Low
- **Evidencia**: 53 warnings frente a 49 del baseline. Nuevos: `PluralsCandidate` en `shopping_list_origin_multiple` (es/en) y `ModifierParameter` en `PincheScreen.kt:109` y `RecommendationsScreen.kt:88`; el de `MyRecipesScreen.kt:164` es preexistente. `informe.md:20` y `PROGRESS.md` afirman «sin warnings nuevos».
- **Cambio requerido**: usar `<plurals>` + `pluralStringResource` para «Para: N recetas», poner `modifier` como primer parámetro opcional en las dos pantallas, o actualizar la evidencia con el recuento exacto y la justificación.
- **Verificación**: `lintDebug` sin los 4 warnings nuevos (ideal ≤49) y `unit` en verde.

### L2 — «Añadido» del Pinche no revierte si falla la persistencia

- **Severidad**: Low
- **Evidencia**: `PincheViewModel.addMissingToShoppingList` marca `addedToList = true` antes de persistir y no revierte en `onFailure`, a diferencia de `saveProposal`.
- **Cambio requerido**: revertir el estado y mostrar un aviso, o documentar la aceptación explícita de ese comportamiento.
- **Verificación**: test con repositorio que lanza excepción.

### L3 — Cobertura Room incompleta

- **Severidad**: Low
- **Evidencia**: S6 usa `InMemoryShoppingListRepository`; `RoomShoppingListRepository.deleteItem`/`restoreItem` no se ejercitan; no hay prueba de reinicio de la app; la migración no inserta/lee un ítem tras migrar.
- **Cambio requerido**: añadir una prueba Room de add → delete → restore y, en la migración, insertar y leer un ÍtemLista con su fuente.
- **Verificación**: tests focalizados y actualización del informe.

### L4 — Metadatos y cifras documentales desactualizados

- **Severidad**: Low
- **Evidencia**: la spec mantiene `status: not_started` y `PROGRESS.md`/historial dicen «206 líneas» frente a 281. La afirmación de que `PincheProposalRules.missingRequired` conserva el comportamiento intacto no menciona que la delegación excluye nombres normalizados vacíos.
- **Cambio requerido**: actualizar el estado de la spec, la cifra de líneas y matizar la nota de comportamiento.

**Nota Low no bloqueante**: la top bar de Mis Recetas pasa a 5 acciones; la navegación se verifica, pero no hay captura que permita valorar densidad visual.

## 5. Seguridad

**Sin hallazgos de seguridad** en el diff:

- sin secretos, API keys ni `.env` en código, tests, docs o capturas; `local.properties` no aparece en `git status`;
- sin nuevas rutas autenticadas ni cambios de autorización (app local de usuario único);
- consultas Room parametrizadas y SQL de migración constante;
- sin llamadas de red ni IA; sin dependencias, permisos ni manifest nuevos;
- capturas con datos de demostración, sin PII.

La BD local sin cifrar es un riesgo preexistente aceptado, no introducido por esta feature.

## 6. Documentación y handoff

Bien actualizados: `docs/domain-model.md` (RD18, ÍtemLista y casos límite), `docs/risks-and-open-questions.md` (`Q-anadir-faltantes-a-la-lista-1..7`), `DESIGN.md`, `docs/technical-discovery.md` (Room v3), `PROGRESS.md`, `docs/progress-history.md`, `feature_list.json` (solo `status`/`evidence`) y la evidencia (informe + 2 PNG). `AGENTS.md` y `CONTEXT.md` no cambian, correcto.

Pendiente: cobertura de S5 (H1), recuento real de warnings (L1), metadatos de la spec y cifra de líneas (L4), y reproducibilidad de `init.ps1` (inspected-only).

## 7. Implementation Repair Brief

```md
Goal: Cerrar la verificación de S5 y alinear evidencia/documentación con el estado real.

Do:
1. Ampliar ShoppingListE2ETest (o test de integración de ViewModels con el mismo
   InMemoryShoppingListRepository) para cubrir S5 completo:
   - Ficha: enviar mensaje, ADD_MISSING y comprobar ADDED_MISSING_LABEL;
   - SAVE y esperar SAVED_LABEL;
   - cargar la Receta guardada con proposalRecipeId("<título>");
   - renderizar la Lista sobre el mismo repositorio y comprobar el ítem y
     «Para: <título>»;
   - renderizar el Detalle con RecipeDetailViewModel(..., saved.id), pulsar
     ADD_TO_LIST y assertar 1 ítem, misma cantidad y una sola fuente.
2. Corregir o documentar los 4 warnings nuevos:
   - shopping_list_origin_multiple -> <plurals> + pluralStringResource;
   - modifier como primer opcional en PincheScreen y RecommendationsScreen;
   - si no se corrigen, actualizar la evidencia a «0 errores / 53 warnings (4 nuevos)».
3. Opcional (L2): revertir addedToList y avisar si el repositorio falla; test con fallo.
4. Opcional (L3): prueba Room add -> delete -> restore e inserción/lectura de un
   ÍtemLista tras la migración 2->3.
5. Actualizar docs: status de la spec, «206 líneas» -> 281, matiz de
   missingRequired (excluye nombres normalizados vacíos) y recuentos reales.

Do not:
- No añadir alta manual/voz, Pasillo, «Despensa baja», tabs, checkboxes, progreso ni IA.
- No tocar RD01/RD02/RD10/RD13, calculateMatch, SuggestionRules, SuggestionAcceptance
  ni ServingsScaling.
- No commitear sin petición explícita.

Run:
- .\gradlew.bat :app:lintDebug :app:testDebugUnitTest --rerun-tasks
- .\gradlew.bat :app:assembleDebug
- .\e2e.ps1 (o ShoppingListE2ETest focalizado) en el AVD Pixel_10_Pro API 37

Update:
- docs/evidence/anadir-faltantes-a-la-lista/informe.md
- feature_list.json (evidence; status solo según el flujo del orquestador)
- PROGRESS.md y docs/progress-history.md
- docs/specs/anadir-faltantes-a-la-lista.md (metadatos)
```

## 8. Seguimiento

1. Aplicar H1 y las correcciones L1/L4 (L2/L3 recomendables).
2. Reejecutar `lint` + `unit` + `assemble` y la E2E focalizada (y la suite completa si es posible).
3. Revalidar con `feature-validator`; solo entonces persistir `accepted`.

## 9. Revalidación independiente (sesión 85, 2026-10-26)

- **Veredicto**: **`accept`**. La reparación cierra H1 y L1-L4; no hay hallazgos Critical/High/Medium ni de seguridad.
- **Alcance revisado**: diff completo sin commitear (31 ficheros modificados + nuevos), spec de 281 líneas, `feature_list.json`, evidencia, `PROGRESS.md` e historial. Solo se actualizan `status` y `evidence` en `feature_list.json`.

### Checks reejecutados sobre el árbol actual

| Check | Resultado |
|---|---|
| `gradlew :app:lintDebug :app:testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (38 s) |
| `unit` | **205/205 en 34 suites**, 0 fallos, 0 errores, 0 omitidos |
| `lintDebug` | **0 errores / 49 warnings** (sin warnings nuevos sobre el baseline) |
| `assembleDebug` | **BUILD SUCCESSFUL** |
| `init.ps1` | **exit 0** («Entorno listo»; AVD `Pixel_10_Pro` API 37, `emulator-5554`; app instalada y en ejecución) |
| Paridad es/en | **266/266** (265 strings + 1 plural por idioma), sin claves solo-es o solo-en |
| PNG de evidencia | 2 válidos, 1280×2856 (`detalle-faltantes-es.png` 203.287 B; `lista-compra-es.png` 157.050 B) |

### Evidencia cara ejecutada e inspeccionada

| Check | Evidencia |
|---|---|
| `e2e.ps1` / `connectedDebugAndroidTest` | **BUILD SUCCESSFUL** en 7m 37s; XML más reciente: **164 tests, 0 fallos, 0 errores, 2 omitidas**, 446,233 s, `2026-10-26T19:44:45`. Las 2 omitidas siguen siendo `SettingsE2ETest` por `Assume` sin key. |
| `ShoppingListE2ETest` | **10/10**, 0 fallos, 0 omitidas. Incluye los 8 escenarios, la migración 2→3 con escritura/lectura y la persistencia Room con cierre/reapertura + borrado/restauración. |
| `ShoppingListFlowTest` (S5) | **1/1**. Ficha → Añadir faltantes → Guardar Propuesta → cargar Borrador por `proposalRecipeId` → Detalle → Añadir a la lista: 1 ítem, misma cantidad, una sola fuente y mismo `sourceKey`/título. |

### Cierre de hallazgos

- **H1 — cerrado**. La cláusula que faltaba (guardar la Propuesta y repetir desde el Detalle del Borrador) queda cubierta por `ShoppingListFlowTest`. La UI del badge no se vuelve a renderizar en ese test JVM, pero S1/S3/S6 ya demuestran que `ShoppingListScreen` pinta el `sourceTitle` como «Para: …», y el test de integración verifica el dato exacto que consume el badge. Cobertura conjunta suficiente; no se repite el flujo UI completo en un solo test.
- **L1 — cerrado**. `shopping_list_origin_multiple` es un `<plurals>` usado con `pluralStringResource`; `modifier` va antes de los callbacks en `PincheScreen`/`RecommendationsScreen`. Lint baja de 53 a **49** warnings.
- **L2 — cerrado**. El fallo de persistencia revierte `addedToList` y muestra `PincheNotice.SHOPPING_LIST_FAILED`; `PincheViewModelTest` lo cubre.
- **L3 — cerrado**. Room se ejercita con cierre/reapertura, `deleteItem`/`restoreItem` y migración 2→3 con inserción/lectura de un ÍtemLista y su fuente.
- **L4 — cerrado**. Spec con `status: accepted` y 281 líneas; `docs/domain-model.md` y `docs/technical-discovery.md` documentan la exclusión de nombres normalizados vacíos y los recuentos reales.

### Escenarios

| Escenario | Estado final |
|---|---|
| S1 · Detalle con Faltantes → Lista con Origen | ✅ E2E |
| S2 · repetir no duplica ni dobla | ✅ E2E con Room real |
| S3 · dos Recetas suman y badge cuenta fuentes | ✅ E2E |
| S4 · tarjeta de «Recetas con mi Despensa» | ✅ E2E |
| S5 · Ficha + guardar + repetir desde el Borrador | ✅ E2E de Ficha + integración `ShoppingListFlowTest` |
| S6 · borrado con «Deshacer» | ✅ E2E in-memory |
| S7 · Lista vacía + CTA al Escáner | ✅ E2E |
| S8 · sin Faltantes no hay acción | ✅ E2E |
| Migración 2→3 y persistencia Room | ✅ E2E |

### Seguridad y notas no bloqueantes

- **Seguridad**: sin hallazgos. Sin secretos, API keys, llamadas de red/IA, permisos, dependencias ni manifest nuevos; consultas Room parametrizadas y SQL de migración constante; capturas con datos de demostración y sin PII.
- **Notas Low no bloqueantes**: la top bar de Mis Recetas acumula 5 acciones y no hay captura de densidad; `restoreItem` podría sobrescribir una fuente añadida durante la ventana de «Deshacer» y el borrado/undo no está serializado en el ViewModel. Son riesgos de borde preexistentes al alcance de esta feature y no afectan a S1-S8.
- **Handoff**: evidencia, `feature_list.json`, `PROGRESS.md` e historial actualizados por el orquestador. Sin commit ni push; el worktree queda sin commitear a la espera de decisión del usuario.
