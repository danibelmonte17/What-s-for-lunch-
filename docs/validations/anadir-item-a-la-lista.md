# Validación independiente — `anadir-item-a-la-lista`

- **Fecha**: 2026-10-27
- **Rol**: `feature-validator` (subagente `validator`)
- **Spec**: `docs/specs/anadir-item-a-la-lista.md` (RF-AIL1–RF-AIL7, S1–S6, D1–D7)
- **Estado declarado al validar**: `passing` (autoverificación del implementador, sesión 87)
- **Veredicto**: **`revise`** (un hallazgo High de ida-y-vuelta de la cantidad al editar; el resto de la feature es de calidad aceptación)

## 1. Objetivo de revisión

- Feature seleccionada: `anadir-item-a-la-lista` (`passing` en `feature_list.json`; su dependencia `anadir-faltantes-a-la-lista` está `accepted` con validación independiente previa).
- Diff revisado: árbol de trabajo sin commitear en `feature/anadir-item-a-la-lista` (20 ficheros modificados + 4 nuevos sin trackear: `Aisle.kt`, `ShoppingListManualItemTest.kt`, `docs/evidence/anadir-item-a-la-lista/`, `docs/specs/anadir-item-a-la-lista.md`).
- `feature_list.json` solo cambia `status` (`not_started` → `passing`) y `evidence` de esta entrada: conforme a AGENTS.md.
- Evidencia inspeccionada: `docs/evidence/anadir-item-a-la-lista/informe.md`, 2 PNG (1280×2856) y el XML superviviente de `connectedDebugAndroidTest`.

## 2. Checks reejecutados e inspeccionados

### Reejecutados sobre el árbol actual (tier barato)

| Check | Resultado |
|---|---|
| `.\init.ps1` (sin canalizar stdout) | **Verde** («Entorno listo»; app instalada y en ejecución en `emulator-5554`, AVD `Pixel_10_Pro` API 37) |
| `.\lint.ps1` (`lintDebug`) | **BUILD SUCCESSFUL**; `lint-results-debug`: **0 errores / 49 warnings** (línea base, sin nuevos) |
| `.\gradlew testDebugUnitTest --rerun-tasks` (punto 3 del plan) | **BUILD SUCCESSFUL**; XML: **35 suites / 216 tests / 0 fallos / 0 errores / 0 omitidos** — coincide exactamente con lo reclamado (+11 tests: `ShoppingListManualItemTest` 7, `InMemoryShoppingListRepositoryTest` +2, `ShoppingListViewModelTest` +2) |
| `.\gradlew assembleDebug` | **BUILD SUCCESSFUL** |
| Paridad es/en (`<string>`) | **281/281 claves idénticas** (16 nuevas por archivo) |
| Recuento de `@Test` en `app/src/androidTest` | **170** (164 de la sesión 85 + 6 de S1–S6; `ShoppingListE2ETest` = 16) — base del hallazgo L1 |
| Reproducción del formato/parsing de cantidades (jshell, Corretto 17) | Confirma el hallazgo H1: `es-ES` 1200 → «1.200» → `parse` = 1.2; 1234.5 → «1.234,5» → sin parsear; 0.1234 → «0,123» |

Nota: `lintDebug` en ejecución normal queda `UP-TO-DATE`; la cifra 0/49 se leyó del informe `lint-results-debug.{xml,txt}` del árbol actual (sin drift: los gates baratos reproducen exactamente las cifras de la evidencia).

### Inspeccionados, no reejecutados (tier caro)

| Check | Evidencia |
|---|---|
| `e2e.ps1` batería completa | Informe §Gates/§Inestabilidad + PROGRESS + historial: **172 tests, 2 fallos** clasificados como inestabilidad del entorno (teardown de `createComposeRule` en `AiSuggestionsE2ETest#mixedStates…`; crash de `system_server` → `INSTRUMENTATION_ABORTED`). La cifra global no reconcilia con el inventario (**170**): ver L1. No se reejecutó (la discrepancia es de recuento, no de cobertura de la feature). |
| `ShoppingListE2ETest` completo | **16/16 verde, 10m07s** (10 previos sin regresión + S1–S6). Cuadra exactamente con el diff: 6 tests nuevos + 10 previos = 16. |
| `AiSuggestionsE2ETest#mixedStates…` aislado | **1/1 verde (49 s)**. Corroborado en disco: XML `TEST-Pixel_10_Pro(AVD) - 17-_app-.xml` (2026-10-26 22:23:13), 1 test, 0 fallos. |
| Punto 6 del plan (BD v3 + edición sin cascada + reinicio) | Extensión de `roomRepositoryKeepsItemsAcrossReopenAndRestoresDeletedSources` en el diff: edita «Azafrán» (1→2 g, `DAIRY`) sobre BD v3 con fuente «Paella», cierra y reabre: conserva nombre, `sources` y solo cambia cantidad/unidad/Pasillo; delete/restore intactos. |
| 2 capturas | Revisadas visualmente: `dialogo-alta-es.png` (diálogo «Añadir ítem» con cantidad y Pasillo «Despensa & Aceites») y `lista-es.png` («Pan (2 uds)» con «Despensa baja» + «Harina (500 g)» editada con badge «Para: Tortilla» intacto). 1280×2856, cumplen el contrato visual de la spec. |
| Alcance, arquitectura, seguridad y docs | Inspección del diff completo, strings, testTags, manifest, dependencias y docs durables. |

## 3. Evaluación contra la spec

| Escenario / requisito | Estado |
|---|---|
| S1 · alta manual con cantidad y Pasillo (verify 1) | ✅ `manualAddShowsTheItemWithAisleLabelAndManualBadge` + captura |
| S2 · duplicado fusiona sin fila nueva y conserva fuentes | ✅ `manualAddOfAnExistingNameEditsItWithoutCreatingANewRow` (1 fila, «Para: Tortilla» intacto) |
| S3 · edición por icono solo cambia cantidad/unidad/Pasillo | ⚠️ **Correcto en el caso probado** (1 g) **pero falla con cantidades ≥ 1000 o >3 decimales**: ver H1 |
| S4 · nombre obligatorio, error sin cerrar | ✅ `invalidNameOrQuantityShowsAnErrorAndKeepsTheDialogOpen` |
| S5 · cantidad y Pasillo opcionales | ✅ `manualAddWithoutQuantityOrAisleUsesTheDefaults` («Leche», «Sin clasificar») |
| S6 · el estado vacío también añade | ✅ `emptyStateAddCtaOpensTheDialogAndSavesTheItem` |
| RF-AIL1..RF-AIL3, RF-AIL5..RF-AIL7 | ✅ cumplidos (FAB + CTA vacío; diálogo 5 opciones + badge «Despensa baja»; upsert por nombre normalizado; validación en `ShoppingListRules`; Room v3 sin migración; paridad 281/281) |
| RF-AIL4 (fila con Pasillo + edición con nombre de solo lectura) | ✅ salvo el matiz de H1 en el prellenado de la cantidad |
| Non-goals | ✅ respetados: sin voz (sin `RECORD_AUDIO`, `SpeechRecognizer` ni permisos nuevos), sin barra rápida ni chips, sin agrupación/tabs/checkboxes/progreso, sin renombrar (nombre `readOnly` en edición), sin conversiones ni tope 99, RD01/RD02/RD10/RD13/RD15/RD18 intactas (`addContributions`/`missingRequiredIngredients`/`calculateMatch` sin tocar) y esquema Room sin cambios (v3, `ShoppingListItemEntity` solo comentario) |
| Plan de verificación §208–216 (puntos 1–7) | ✅ seguido, incluido el punto 3 con `--rerun-tasks` y el punto 6 sobre BD v3 previa con Faltantes |
| Tareas T1–T7 | ✅ completadas |

Arquitectura conforme: reglas puras en `domain/shoppinglist`, contrato en `domain/repository`, Room en `data` (upsert con reinyección de fuentes en la misma transacción, difiriendo el `REPLACE`+cascada), MVVM en `ui` sin reglas de negocio (la validación delega en `ShoppingListRules`). TestTags nuevos sin renombrar los existentes. Fuera del diff esperado solo aparecen dos cambios mínimos y justificados: el fake de `PincheViewModelTest` gana el `saveManualItem` vacío (ripple obligatorio de la interfaz) y `ShoppingListItemEntity` solo actualiza comentarios.

## 4. Hallazgos

### H1 — La edición corrompe o bloquea cantidades ≥ 1000 (y pierde precisión >3 decimales) por el ida-y-vuelta del prellenado

- Severity: **High**
- Evidence:
  - `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` (`ShoppingItemDialog`): el campo Cantidad se prellena con `formatQuantity(item.quantity, locale)` y se reinterpreta con `ShoppingListRules.parseManualQuantity` (`replace(',', '.')` + `toDoubleOrNull`) al guardar.
  - `formatQuantity` (`ui/recipedetail/RecipeDetailScreen.kt:121`) usa `NumberFormat.getNumberInstance(locale)`, que **agrupa millares por defecto** y limita a 3 decimales.
  - Reproducción con el JDK del proyecto (jshell, Corretto 17.0.20): `es-ES` 1200.0 → «1.200» → `Double.parseDouble("1.200")` = **1.2**; 1234.5 → «1.234,5» → tras `replace(',', '.')` = «1.234.5» → `NumberFormatException` → `parseManualQuantity` = null; 0.1234 → «0,123» (redondeo silencioso).
  - Ruta del fallo: «Harina» con 1200 g en la Lista (vía diálogo o Faltantes de Receta) → icono de edición → cambiar solo el Pasillo (S3) → «Guardar» → la cantidad se guarda como 1.2, o el guardado queda bloqueado con «Escribe una cantidad válida» fantasma y obliga a reescribir la cantidad.
- Why it matters: S3 exige que al editar «la cantidad… quede intacta» y la tabla de casos límite declara cantidades > 99 sin tope (1200 g, 1500 ml son cantidades normales de compra; RD18). El fallo es corrupción silenciosa de datos o un bloqueo de guardado, y ninguna prueba actual (unit ni E2E) usa cantidades de 4 cifras ni >3 decimales, por lo que pasó desapercibido.
- Required change: el prellenado del campo Cantidad debe ser estable en ida-y-vuelta con `parseManualQuantity` (sin separador de millares y sin redondear a 3 decimales), manteniendo D6 (coma o punto como separador decimal; vacío = sin cantidad).
- Suggested implementation:
  1. Añadir un formateador de edición `formatEditableQuantity(quantity: Double, locale: Locale)` (p. ej. junto a `formatQuantity` o privado en `ShoppingListScreen.kt`): `NumberFormat.getNumberInstance(locale).apply { isGroupingUsed = false; maximumFractionDigits = 12 }` (alternativa: `quantity.toBigDecimal().stripTrailingZeros().toPlainString()`), y usarlo **solo** para el prellenado del diálogo; la fila sigue mostrando `formatQuantity`.
  2. (Defensa opcional) Hacer `parseManualQuantity` tolerante a los separadores de millares del locale antes de normalizar coma→punto, sin cambiar D6.
  3. Tests de ida-y-vuelta en `ShoppingListManualItemTest` (p. ej. `parseManualQuantity(formatEditableQuantity(1200.0, es)) == 1200.0`, y casos 1234.5 y 0.1234) y una comprobación de edición con cantidad > 999 (extender el test Room del punto 6 con 1500.5, o un caso E2E breve).
- Verification after fix:
  - `.\gradlew testDebugUnitTest --rerun-tasks` → suites en verde con los tests nuevos (35 suites / 219+ tests).
  - `.\lint.ps1` → 0 errores / 49 warnings.
  - Reejecución focalizada de `ShoppingListE2ETest` (16/16+) y/o comprobación manual con `adb`: crear «Agua» con «1500» ml, editar solo el Pasillo y verificar que la fila sigue con 1500 (captura o log en la evidencia).

### L1 — La cifra «172 tests» de la batería completa no reconcilia con el inventario (170)

- Severity: Low
- Evidence: el inventario de `@Test` de `app/src/androidTest` es **170** (164 de la sesión 85 + 6 de S1–S6; `ShoppingListE2ETest` = 16 exactos). La evidencia afirma «172 tests, 2 fallos» en la primera pasada de `e2e.ps1` y no queda XML de esa pasada (sobrevive solo el de la reejecución aislada de `AiSuggestionsE2ETest`, 1/1 a las 22:23).
- Why it matters: la evidencia inspeccionada debe cuadrar con el diff para aceptar sin reejecutar; una cifra que no reconcilia obliga a reconstruir la trazabilidad a mano y ensucia el handoff. No afecta a la cobertura (S1–S6 = 6 tests presentes y ejecutados, 16/16 cuadra), solo al recuento global.
- Required change: corregir la cifra en `docs/evidence/anadir-item-a-la-lista/informe.md` y en las entradas de `PROGRESS.md`/`docs/progress-history.md` que la repiten (o documentar de dónde salen los +2), y conservar el XML de la batería completa junto a la evidencia en futuras carreras.
- Suggested implementation: 1) determinar la cifra real de esa pasada (posible doble conteo de los 2 omitidos de `SettingsE2ETest` o de una carrera abortada+reiniciada); 2) reescribir la línea con la cifra correcta; 3) copiar `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` a `docs/evidence/anadir-item-a-la-lista/` antes de que otra carrera lo sobrescriba.
- Verification after fix: el recuento de `@Test` de `androidTest` coincide con la cifra documentada y el XML guardado suma lo mismo.

### Notas no bloqueantes

- **N1 (RK04)**: la traducción EN de «Despensa baja» («Low Pantry») es literal y algo forzada; si el usuario prefiere otra voz (p. ej. «From the pantry»), basta cambiar `shopping_list_manual_origin` en `values-en/`. Las traducciones de Pasillos («Produce & Fresh», «Meat & Fish», «Dairy», «Pantry & Oils») son razonables y los términos es coinciden literalmente con `CONTEXT.md`.
- **N2**: el prellenado usa `formatQuantity`, compartido con la fila; el fix de H1 debe tocar solo el prellenado para no alterar el formato de lectura de las filas.

## 5. Seguridad

Revisión acotada al diff según `references/security-checklist.md`:

- **Secretos y configuración**: sin API keys, tokens, credenciales ni PII en código, tests ni evidencia (la evidencia cita solo rutas locales y nombres de AVD). Sin cambios en `local.properties`/`BuildConfig`/gradle.
- **Autenticación/autorización**: no aplica (app local, usuario único).
- **Validación de entrada**: nombre y cantidad se validan en `ShoppingListRules` en el límite (nombre no vacío tras normalizar; cantidad finita > 0; `toDoubleOrNull` sin excepciones); unidad recortada como texto libre. La escritura usa DAO de Room (sin SQL crudo): sin vector de inyección. Nota teórica: longitud de nombre/unidad sin acotar (riesgo nulo en este slice local).
- **Exposición de datos**: todo local; sin red, sin logs de datos de usuario y las capturas muestran solo datos de demo.
- **Servicios externos / dependencias / permisos**: la feature no llama a servicios, no añade dependencias y no toca el manifest (sin `RECORD_AUDIO`: D1 respetada).

**Sin hallazgos de seguridad bloqueantes ni notas que requieran acción.**

## 6. Documentación y handoff

- `docs/domain-model.md`: **RD19** añadida, validaciones y casos límites nuevos (duplicado, edición con fuentes, manual→Faltantes, Pasillo desconocido → «Sin clasificar») y escenarios 3/6 actualizados; coherente con el comportamiento implementado.
- `docs/risks-and-open-questions.md`: `Q-anadir-item-a-la-lista-1..4` registradas como resueltas (2026-10-26) y el spike de `SpeechRecognizer` anota que cubre también el dictado de la Lista (diferido): correcto.
- `DESIGN.md`: fila de la Lista (diálogo, edición, etiqueta de Pasillo, «Despensa baja», voz diferida) y estado vacío con dos CTAs: correcto.
- `CONTEXT.md`, `AGENTS.md`, `docs/build-brief.md`, `docs/technical-discovery.md` y scripts del harness: **sin cambios** (justificado; `uxdocs/` y `.opencode/` intactos). `AGENTS.md` sigue corto y tipo router.
- `PROGRESS.md` + `docs/progress-history.md`: al día (sesiones 86–87), salvo la cifra de L1.
- Harness: `init.ps1` verde y sin cambios; harness E2E existente con S1–S6 ejecutados (16/16). Estado de `feature_list.json` (`passing`) coherente con lo verificado; la persistencia de la aceptación corresponde al orquestador.

## 7. Implementation Repair Brief

Goal: eliminar la corrupción/bloqueo de cantidades al editar (H1) y cuadrar la cifra de la batería E2E (L1).

Do:
1. Añadir `formatEditableQuantity(quantity, locale)` sin agrupación de millares y con precisión suficiente (`isGroupingUsed = false`, `maximumFractionDigits = 12`, o `BigDecimal.stripTrailingZeros().toPlainString()`) y usarlo en el prellenado del campo Cantidad de `ShoppingItemDialog` (la fila de la lista sigue con `formatQuantity`).
2. (Opcional) Reforzar `ShoppingListRules.parseManualQuantity` eliminando separadores de millares del locale antes de normalizar coma→punto, sin cambiar D6.
3. Añadir tests de ida-y-vuelta en `ShoppingListManualItemTest` (1200.0, 1234.5 y 0.1234 con locale es y en) y una comprobación de edición con cantidad > 999 (extender el test Room del punto 6 o añadir aserción en un test de ViewModel/repositorio).
4. Corregir la cifra «172 tests» en `docs/evidence/anadir-item-a-la-lista/informe.md` (+ `PROGRESS.md`/`docs/progress-history.md` cuando el orquestador lo persista) y guardar el XML de la batería en `docs/evidence/anadir-item-a-la-lista/`.

Do not:
- No cambies el formato de las filas de la lista ni D6 (coma o punto decimal; vacío = sin cantidad).
- No toques `formatQuantity` compartido con el Detalle de Receta, ni amplíes el alcance (voz, agrupación, migración Room).

Run:
- `$env:JAVA_HOME='C:\Users\Daniel\.jdks\corretto-17.0.20'; & .\gradlew testDebugUnitTest --rerun-tasks` → todo verde con los tests nuevos.
- `$env:JAVA_HOME='C:\Users\Daniel\.jdks\corretto-17.0.20'; & .\lint.ps1` → 0 errores / 49 warnings.
- Reejecución focalizada `ShoppingListE2ETest` (o la batería completa `e2e.ps1` si se prefiere) → en verde; comprobación manual de edición con 1500 ml si no se automatiza.

Update:
- `docs/evidence/anadir-item-a-la-lista/informe.md` (nueva pasada de unit/E2E + cifra corregida).
- Entrada de historial/PROGRESS con el fix (labor del orquestador/implementer).

## 8. Seguimiento

- Veredicto: **`revise`** → la feature vuelve a `in_progress` para aplicar H1 (y L1); después revalidación independiente.
- Tras el fix, revalidar: ida-y-vuelta de cantidades ≥ 1000 y >3 decimales, regresión de S1–S6 y cifras de gates baratos.
- No es aceptación: `feature_list.json` no se toca desde este informe (lo persiste el orquestador).
