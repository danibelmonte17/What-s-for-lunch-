# Validación independiente — `agrupar-y-completar-la-lista`

- **Fecha**: 2026-10-27
- **Rol**: `feature-validator` (subagente `validator`, sesión independiente sin contexto del implementador)
- **Spec**: `docs/specs/agrupar-y-completar-la-lista.md` (RF-AGL1–RF-AGL8, S1–S10, D1–D10, T1–T10)
- **Estado declarado al validar**: `passing` (autoverificación del implementador, sesión 91)
- **Veredicto**: **`accept`** — sin hallazgos Critical/High/Medium; 1 hallazgo **Low** (cosmético, no bloqueante) y 3 notas. Persistido por el orquestador como `accepted` (sesión 92).

## 1. Objetivo de revisión

- Feature seleccionada: `agrupar-y-completar-la-lista` (única `passing` de `feature_list.json`, la más recientemente evidenciada; su dependencia `anadir-faltantes-a-la-lista` está `accepted`). Estado tratado como «listo para validar», no aceptado.
- Spec/contrato: `docs/specs/agrupar-y-completar-la-lista.md` (RF-AGL1–8, escenarios S1–S10 con verify 1 = S2 y verify 2 = S3, decisiones D1–D10, tareas T1–T10, plan de verificación y validator checklist — evaluada punto por punto por el validador, checkboxes sin marcar a propósito).
- Diff revisado: árbol de trabajo sin commitear en la rama `feaure/agrupar-y-completar-la-lista` (errata intencional del usuario, respetada). 17 ficheros modificados + `ShoppingGroup.kt` nuevo + spec + carpeta de evidencia; coinciden **exactamente** con «Expected File Changes» de la spec. Sin commits ni push.
- `feature_list.json` auditado con `git diff`: solo cambia `status` (`not_started` → `passing`) y `evidence` de esta entrada (7 líneas cambiadas), conforme a AGENTS.md.
- Evidencia inspeccionada: `docs/evidence/agrupar-y-completar-la-lista/informe.md`, `lista-pasillo-es.png` y `lista-receta-es.png`, y las entradas de `PROGRESS.md` / `docs/progress-history.md` (sesión 91).

## 2. Checks re-ejecutados vs. solo inspección

### Re-ejecutados por el validador (tier barato, sobre el árbol de trabajo actual)

| Check | Comando | Resultado |
|---|---|---|
| Gate de arranque | `$env:JAVA_HOME="C:\Users\Daniel\.jdks\corretto-17.0.20"; ./init.ps1` | **exit 1** en `adb get-state`: «no hay emulador ni dispositivo conectado» (emulador apagado). Gate **no bloqueante** y dependiente de dispositivo, patrón ya aceptado en sesiones 69/84. El script sí ejecuta Gradle (no solo imprime comandos); no arranca procesos largos. |
| Lint | `./gradlew :app:lintDebug --rerun-tasks` (tras `./lint.ps1` que quedó `UP-TO-DATE`) | **BUILD SUCCESSFUL, 0 errores / 49 warnings** — idéntico a la línea base. Los 49 son preexistentes (líneas 28–231 de `strings.xml`: `Typos` «momento», `PluralsCandidate`, `UnusedResources`, `TypographyEllipsis`, etc.); **ninguno apunta a las claves nuevas (297–311) ni a ficheros del diff**. |
| Unit | `./gradlew :app:testDebugUnitTest --rerun-tasks` (tras `./unit.ps1` `UP-TO-DATE`) | **35 suites / 232 tests / 0 fallos / 0 errores / 0 omitidos** (inventario de XML de `test-results`), reconcilia con 219 base + 13 nuevos. |
| Build | `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** (exit 0). |
| Paridad es/en | recuento de claves `<string>`/`<plurals>` | **291 / 291, claves idénticas** (9 nuevas por archivo). |
| Temporales | `grep`/`git status -uall` | Sin restos del test instrumentado temporal de captura (`ShoppingEvidenceCaptureTest` no existe; 0 hits de `takeScreenshot`/`MediaStore` en `app/src`). |
| Ficheros cambiados | `git status` + diffs | Coinciden **exactamente** con «Expected File Changes» de la spec. |

Nota: los primeros `lint.ps1`/`unit.ps1` terminaron `UP-TO-DATE` (caché del implementador); por eso se re-ejecutaron con `--rerun-tasks`, que es la ejecución real citada arriba.

### Tier caro — SOLO INSPECCIÓN (sin arrancar el AVD `Pixel_9_Pro`, que sigue apagado)

| Check | Evidencia que lo respalda | Auditoría |
|---|---|---|
| E2E focalizada `ShoppingListE2ETest` — **27/27, 0 omitidos** | `docs/evidence/agrupar-y-completar-la-lista/informe.md` §Gates y §Desglose (comando exacto con propiedad entrecomillada, fecha 2026-10-27, AVD `Pixel_9_Pro` API 37 `emulator-5554`, desglose S1–S10 con nombre de método y resultado por escenario) | Los 10 nombres de test citados **existen literalmente** en el fichero; el inventario `@Test` del fichero es **27** = 17 previos + 10 nuevos ✓; sin `@Ignore`/`Assume` ocultos ✓; fechas coherentes (última modificación de `app/src` 13:13 < capturas 13:18 < informe 13:21 → la evidencia se capturó sobre el árbol final) ✓. |
| Batería `e2e.ps1` — **181 tests, 0 fallos, 2 omitidos** (`SettingsE2ETest` por `Assume`, preexistentes) | Mismo informe + entrada sesión 91 de `docs/progress-history.md` | Reconciliación aritmética: inventario previo 171 + 10 nuevos = **181** ✓; la nota «consola 183/181» = 181 + 2 omitidos dobles por AGP, patrón ya documentado en evidencias previas ✓. |
| Capturas es (2) | `lista-pasillo-es.png`, `lista-receta-es.png` | Cabecera PNG `89 50 4E 47 0D 0A 1A 0A` ✓, **1280×2856** ✓, **inspeccionadas visualmente**: (1) modo «Pasillo de Super», progreso «2 de 3 artículos en la cesta»/67 %/barra, selector segmentado, cabeceras «Lácteos»/«Despensa & Aceites»/«Sin clasificar» con contador, 2 filas completadas tachadas/atenuadas con checkbox relleno y nombre intacto, badges conservados, «Limpiar comprados» en la top bar; (2) modo «Por Receta», «Harina» replicada en «Gazpacho» y «Paella» con «Para: 2 recetas» y checkbox compartido, «Sin receta» al final con etiqueta de Pasillo por fila. Sin PII ni secretos. |

**Decisión de no re-ejecutar el tier caro**: la evidencia es exacta (comando, resultado, fecha/entorno, conteo), internamente consistente (conteos cuadran con el inventario del código), no contradice el diff y fue capturada tras el último cambio de árbol (mtimes). Cumple los criterios de la skill para inspección; no se ha arrancado ningún proceso de emulador.

## 3. Hallazgos por severidad

### Finding: parámetro renombrado en el override de `InMemoryShoppingListRepository.restoreItems`

- **Severidad**: Low (no bloqueante).
- **Evidencia**: warning del compilador en `compileDebugKotlin`: `InMemoryShoppingListRepository.kt:74:39 The corresponding parameter in the supertype 'ShoppingListRepository' is named 'items'`. El override usa `restored` mientras la interfaz usa `items` (Room usa `items`).
- **Por qué importa**: ruido de compilación nuevo introducido por este diff y ligera trampa de argumentos con nombre.
- **Cambio requerido**: renombrar el parámetro del override a `items`.
- **Pasos sugeridos**: 1) en `InMemoryShoppingListRepository.restoreItems`, cambiar `restored` → `items` (y sus usos en el cuerpo); 2) nada más.
- **Verificación tras el fix**: `./gradlew :app:compileDebugUnitTestKotlin` sin ese warning; `unit.ps1` sigue 232/232.

### Notas (no bloqueantes, sin cambio requerido)

- **N1 — `clearCompleted()` borra en bucle sin transacción única** (`ShoppingListViewModel.clearCompleted` → N× `repository.deleteItem`). Un corte a mitad dejaría borrado parcial sin deshacer posible (el `pendingUndo` vive en memoria). El undo masivo sí es transaccional (`restoreItems`). Riesgo mínimo en app local; candidato a endurecimiento futuro.
- **N2 — título de grupo por primera fuente vista** en `groupItems` BY_RECIPE: si dos ítems comparten `sourceKey` con `sourceTitle` divergente (receta renombrada entre aportaciones), el grupo muestra el título del primero. Caso borde del modelo, sin impacto en los escenarios.
- **N3 — `init.ps1` no reproducible en verde sin dispositivo** (exit 1 en `adb get-state`); patrón conocido y no bloqueante, ya aceptado en sesiones previas; no exige fix.

### Sobre lo pedido específicamente por el orquestador

- **4 adaptaciones de tests E2E previos por D6** (`manualAddShowsTheItemWithAisleLabelAndManualBadge`, `manualAddWithoutQuantityOrAisleUsesTheDefaults`, `manualAddOfAnExistingNameEditsItWithoutCreatingANewRow`, `editingAnItemOnlyChangesQuantityUnitAndAisle`): **adaptaciones legítimas de aserción, sin debilitar lo verificado**. D6 movió literalmente la etiqueta de Pasillo de la fila (visible solo en «Por Receta») y ocultó el badge de fuente única en «Por Receta»; las adaptaciones conservan la misma cobertura (etiqueta de Pasillo verificada en el modo que la muestra; cabecera de grupo como verificador del Pasillo en modo «Pasillo», donde la fila ya no lo muestra; badges conservados siguen verificados). Ninguna aserción ajena fue eliminada (comprobado en el contexto del diff) y S8 cubre además el comportamiento D6 explícitamente.
- **Refinamiento `PendingUndo(items, bulk)` sobre la spec**: **documentado** en la propia spec (§Technical Approach, nota «Refinamiento de implementación (2026-10-27, sin cambio de alcance)»), en el informe §Refinamiento y en el historial. **No cambia el alcance**: mismos mensajes («Ítem eliminado» por fila / «Ítems comprados eliminados» masivo, D7) y mismos alcances de deshacer que fija la spec, sin reabrir el diálogo de confirmación descartado en D3.
- **`feature_list.json`**: `git diff feature_list.json` = **solo `status` y `evidence`** — 7 líneas cambiadas ✓, el resto del objeto intacto.

## 4. Implementation Repair Brief

No procede: el veredicto es `accept`. El único hallazgo Low (renombrar `restored` → `items`) puede aplicarse como mantenimiento menor en la siguiente sesión de código sin bloquear la aceptación.

## 5. Seguridad (aplicando `security-checklist.md` acotado al diff)

**Sin hallazgos relevantes.** Detalle:

1. **Secretos/configuración**: ninguno en el diff ni en la evidencia (capturas con solo ítems de compra); sin cambios en `local.properties` ni claves; `⚠ S05` respetado.
2. **Autenticación/autorización**: N/A (app local, sin cuentas ni roles; sin cambios de acceso).
3. **Validación de entrada / inyección**: el único SQL nuevo es **parametrizado** (`UPDATE shopping_items SET completed = :completed WHERE id = :id`, `ShoppingListDao.updateCompleted`); la validación del diálogo manual (RD19) no se toca.
4. **Exposición de datos**: sin PII en capturas, informe ni logs; la feature no añade logging.
5. **Servicios externos / SSRF**: ninguno — la feature es solo presentación + estado; sin red ni IA (no se toca `data/ai`).
6. **Dependencias/herramientas**: sin cambios en `app/build.gradle.kts` ni `libs.versions.toml`; sin artefactos generados versionados.
7. **Frontera servidor/cliente**: N/A. 8. **Uploads/media**: N/A. 9. **Auditoría/abuso**: N/A.

Sin permisos nuevos (`AndroidManifest.xml` sin cambios). Nada que reprobar.

## 6. Docs durables / harness

- **Spec cumplida**: RF-AGL1–8 implementadas y verificadas (unit + E2E + capturas); T1–T10 completadas; «Verification Plan» seguido (todos los gates; el de arranque con la salvedad de dispositivo ya anotada).
- **Non-goals respetados** (verificados en el diff y en los tests): sin compartir/WhatsApp, sin snackbar al completar (D4: `toggleCompleted` no crea `pendingUndo`, test unitario con `assertNull`), sin diálogo de confirmación al limpiar, sin voz/micrófono/permisos, sin chips de sugerencia, sin renombrar ítems ni conversiones ni tope 99, sin persistencia del modo (en memoria, S9 verifica el reinicio), **Room sigue en v3** (solo `MIGRATION_1_2`/`MIGRATION_2_3`), RD18/RD19 intactos (el diff de `ShoppingListRules` solo **añade** `groupItems`/`progress`).
- **Escenarios/evidencia**: S1–S10 con nombres de método verificados contra el fichero y resultados desglosados; 2 capturas es válidas con los nombres de `feature_list.json`; informe completo y reproducible (procedimiento de captura documentado, sin temporales residuales).
- **Docs durables exactamente como declara la spec** (`Durable Documentation Impact`): `docs/domain-model.md` con **RD20** + nota de implementación + casos límite + retirada de la nota «la agrupación llega con…»; `DESIGN.md` actualizado (fila de la Lista sin «lista plana», con progreso/selector/checkbox/«Limpiar comprados» — coherente con la implementación y los tokens `secondary`/`surfaceContainer`); `docs/risks-and-open-questions.md` con `Q-agrupar-y-completar-la-lista-1..6` resueltas. `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`/`technical-discovery.md`/`CONTEXT.md` **sin tocar**, como declara la spec ✓. Convenciones: docs en español, código/identificadores en inglés, textos en `values/`+`values-en/`, reglas de negocio en `domain/`, `uxdocs/`/`.opencode/`/harness sin modificar.
- **Handoff**: `PROGRESS.md` (Estado verificado + Siguiente paso) y `docs/progress-history.md` (sesión 91) al día y coherentes con el diff; `feature_list.json` limitado a `status`/`evidence`.
- **`DESIGN.md` respetado** (feature con UI): checkbox `secondary`/`onSecondary`, barra `secondary` sobre pista `surfaceContainer`, selector segmentado como el prototipo, fila completada tachada/atenuada (D5 resuelve la inconsistencia del prototipo a favor de su JS), targets de 48 dp.

## 7. Follow-up requerido

1. **Orquestador**: persistir el acceptance — `feature_list.json` `status` → `accepted` + `evidence` resumida, entrada de aceptación en `PROGRESS.md`/`docs/progress-history.md` e informe en `docs/validations/agrupar-y-completar-la-lista.md` (este validador no editó ficheros). → **Hecho en la sesión 92 por el orquestador** (este propio informe).
2. **Opcional (Low)**: aplicar el rename `restored` → `items` en `InMemoryShoppingListRepository.restoreItems` en la próxima sesión de código.
3. **Pendiente del usuario** (arrastrado): decidir el mantenimiento del step «alta por dictado de voz» en `feature_list.json` y si se commitea/publica esta rama (hoy todo sin commitear; commit/push solo por petición explícita).

## 8. Validator Checklist de la spec (punto por punto)

| # | Ítem | Resultado | Evidencia breve |
|---|---|---|---|
| 1 | Alcance (sin voz, compartir, chips, ni cambios en RD18/RD19/Room v3) | **Verde** | Diff sin voz/share/chips; `ShoppingListRules` solo añade `groupItems`/`progress`; Room v3 sin migraciones nuevas; sin permisos ni dependencias. |
| 2 | S1–S10 pasan (verify 1 = S2, verify 2 = S3) y los 17 E2E previos en verde | **Verde** | E2E focalizada 27/27 por evidencia (informe con nombres de método verificados contra el fichero; inventario 27 = 17 + 10); las 4 adaptaciones D6 auditadas como legítimas. |
| 3 | D1–D10 cumplidas | **Verde** | D1 (S9 + código en memoria) · D2 (S4 + captura 2) · D3 (S7, sin share) · D4 (unit `assertNull(pendingUndo)`) · D5 (S6 + capturas) · D6 (S8 + adaptaciones) · D7 (S7) · D8 (S1/S2 + unit) · D9 (S3/S4/S10 + unit de redondeo) · D10 (S10 + unit edición/aportaciones). |
| 4 | Evidencia: gates, E2E, 2 capturas es válidas (PNG, 1280×2856) e informe | **Verde** | PNG verificados binarios y 1280×2856 e inspeccionados; informe con comandos/resultados/fechas; gates re-ejecutados por el validador y reproducidos (lint 0/49, unit 232/232, assemble OK, paridad 291/291). |
| 5 | Cobertura E2E añadida/actualizada para el cambio visible | **Verde** | 10 tests nuevos S1–S10 + 4 adaptaciones justificadas por D6 + batería completa sin regresión (181/0). |
| 6 | `feature_list.json` solo `status`/`evidence`; `PROGRESS.md` al día; docs durables (RD20, DESIGN, Q-1..6) | **Verde** | `git diff feature_list.json` auditado; PROGRESS + historial sesión 91; RD20/DESIGN/Q-1..6 presentes y coherentes. |
| 7 | Sin comportamiento ajeno ni trabajo de features futuras | **Verde** | Los 21 ficheros del diff coinciden con «Expected File Changes»; sin features adyacentes ni refactors amplios (`PincheViewModelTest` solo añade los no-ops obligados por la interfaz). |
| 8 | Seguridad: sin secretos, sin permisos nuevos, sin red; logcat y evidencia sin datos sensibles | **Verde** | Ver §5: SQL parametrizado, sin red/IA, sin permisos ni dependencias, capturas e informe sin PII ni claves. |
