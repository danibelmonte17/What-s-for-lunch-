# Validación — Gestionar alimentos de la Despensa

- **Feature**: `gestionar-alimentos-de-la-despensa` (estado en `feature_list.json` al validar: `passing`)
- **Spec**: `docs/specs/gestionar-alimentos-de-la-despensa.md`
- **Fecha**: 2026-10-20
- **Validador**: `feature-validator` independiente (posterior a la sesión 42 del implementer)
- **Veredicto**: **accept**

## Comprobaciones ejecutadas por el validador

| Comando | Resultado |
|---|---|
| `unit.ps1` (`testDebugUnitTest`, reejecución forzada con `--rerun`) | exit 0 — **27/27**, 0 fallos (7 suites: `PantryItemValidationTest` 4, `InMemoryPantryRepositoryTest` 4, `RecipeValidationTest` 8, `RecipeSearchFilterTest` 4, `MisRecetasStateTest` 3, `InMemoryRecipeRepositoryDeleteTest` 2, `MisRecetasSearchDebounceTest` 2) |
| `lintDebug` | exit 0 — BUILD SUCCESSFUL, 0 errores |
| `connectedDebugAndroidTest` (intento 1) | **abortado**: 2 fallos en `EliminarRecetaE2ETest` y `INSTRUMENTATION_ABORTED: System has crashed` (`TimeoutException: ReferenceQueueDaemon ... HardwareRenderer$DestroyContextRunnable`) — inestabilidad del emulador/ART, del mismo tipo que los flakes ya documentados; no imputable al código de la feature |
| `connectedDebugAndroidTest` (intento 2, tras reinicio en frío del AVD) | exit 0 — **67/67, 0 fallos, 0 omitidos** (log `.harness/validator-e2e-retry2.log`; incluye `PantryManagementE2ETest` 5/5 y `PantryLocalizationE2ETest` 1/1) |
| `init.ps1` | no reejecutado por el validador; el implementer lo registró en exit 0 y `lint`, `unit` y E2E se reejecutaron de forma independiente en esta validación |

## Los dos `verification` de `feature_list.json` (adb/uiautomator, `Pixel_10_Pro` API 37, locale es-ES)

1. **Añadir «Tomate fresco»** ✓ — desde Mis Recetas (acción de la barra superior con `contentDescription` «Despensa») → Despensa → FAB «Añadir alimento» → diálogo de alta → campo Nombre → «Añadir»: el listado muestra «Tomate fresco». Repetido con un segundo alimento «Leche entera» con cantidad 2 (se muestra «Cantidad: 2»; orden alfabético Leche → Tomate).
2. **Eliminar un alimento** ✓ — «Eliminar Tomate fresco» (nombre accesible completo) → diálogo «Eliminar alimento» con «¿Eliminar «Tomate fresco»? Esta acción es definitiva y no se puede deshacer.» → «Eliminar»: el alimento desaparece y se muestra el estado vacío. Antes se comprobó el escenario S3: «Cancelar» cierra el diálogo y el alimento sigue en la lista.

Al terminar, el validador dejó el dispositivo limpio (Despensa vacía, receta de prueba de migración eliminada, locale de la app revertido, ficheros temporales borrados); la app queda instalada con la DB en v2.

## Migración Room 1→2 sin pérdida

- **Estática**: `RecipeDatabase` pasa a `version = 2` con `MIGRATION_1_2` explícita (`CREATE TABLE IF NOT EXISTS pantry_items (...)` + `CREATE UNIQUE INDEX IF NOT EXISTS index_pantry_items_normalizedName`); **sin `fallbackToDestructiveMigration`** en todo el diff; `AppModule` registra `.addMigrations(RecipeDatabase.MIGRATION_1_2)`.
- **En el emulador**: a partir del esquema real se construyó un fixture v1 (se eliminaron `pantry_items` y su índice, `user_version = 1`, receta sintética «Gazpacho andaluz» PUBLICADA), se colocó como `databases/whatsforlunch.db` de la app vía `run-as` y se lanzó la app. Resultado: arranque sin crash, `user_version = 2`, tabla `pantry_items` creada y la receta **visible en Mis Recetas** con todos sus campos (título, historia, momento culinario, tiempos, dificultad, raciones, estado). Comprobación repetida de forma independiente a la del implementer.

## UI: capturas y `uiautomator`

- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-con-alimentos.png` — lista en español con «Leche entera / Cantidad: 2» y «Tomate fresco», tarjetas blancas sobre fondo cálido, iconos Editar/Eliminar y FAB «Añadir alimento».
- `docs/evidence/gestionar-alimentos-de-la-despensa/despensa-estado-vacio.png` — estado vacío con icono en círculo, título, mensaje y CTA a ancho completo.
- Presupuesto: 1 informe + 2 capturas = 3 ≤ 4 ✓.
- Tokens (sin prototipo, laguna `RK04`; referencia `DESIGN.md`/`uxdocs/DESIGN.md`): fondo `surface` cálido, tarjetas `surface-container-lowest` con radio grande y sombra, texto `on-surface`/`on-surface-variant`, acciones primarias en terracota con texto blanco, error rojo reservado a «Eliminar». Coherente con lo observado en captura.
- Objetivos táctiles (density 480; ≥48 dp = ≥144 px): Editar/Eliminar por fila 144×144 px ✓, acción de la barra superior de Mis Recetas 144×144 px ✓, botones del diálogo de borrado y de alta ≥144 px de alto ✓, CTA del estado vacío 1136×156 px ✓, «Volver» 213×144 px ✓.
- Nombres accesibles: «Despensa», «Editar <nombre>», «Eliminar <nombre>» verificados con uiautomator en español; `PantryLocalizationE2ETest` cubre «Edit <name>/Delete <name>» y los textos en inglés.

## Recorrido S1–S10

- `PantryManagementE2ETest` cubre S1-S9 en el emulador con la DB real (S3 vía «Cancelar», S4 edición a «Tomate maduro» con cantidad, S5 duplicado normalizado con aviso y sin fila nueva, S6 guardado deshabilitado con nombre vacío, S7 cantidad negativa con error inline, S8 estado vacío con CTA, S9 persistencia con `recreate()`); `PantryLocalizationE2ETest` cubre S10 en `en-US` y ≥48 dp.
- El validador añadió en el emulador: alta de dos alimentos (S1), borrado con cancelar y con confirmar (S2/S3), persistencia tras reinicio completo del proceso (S9) y locale español de la pantalla y sus diálogos (S10). Todos en verde.

## Alcance, arquitectura y seguridad

- **Alcance** ✓: sin Escáner, sin Coincidencia, sin desglose por ubicaciones físicas (RD11 respetado y citado en `notes`), sin búsqueda/filtro/orden del listado. De Mis Recetas solo se añadió la acción `action_open_pantry`; el resto de su comportamiento quedó intacto (regresión completa en verde).
- **Arquitectura** ✓: MVVM + StateFlow, reglas de negocio en `domain/` (`PantryItemValidation`; `normalizeName` extraída a `domain/text/` y compartida con `RecipeSearchFilter`, cuyos tests siguen verdes), Room en `data/local/` con DAO, entidad e índice único, doble `InMemoryPantryRepository` para unit, Koin en `AppModule`, testTags centralizados en `PantryTestTags`. Control de duplicados con triple salvaguarda: comprobación previa, índice único y traducción de `SQLiteConstraintException` a `DuplicateName`.
- **Seguridad/privacidad** ✓: sin permisos nuevos, sin red, sin secrets; solo datos locales en la DB de la app.
- **Docs duraderos** ✓: spec completa y concordante; `docs/domain-model.md` (duplicado rechazado con aviso en alta/edición manual, Q-…-3) y `docs/risks-and-open-questions.md` (Q-gestionar-alimentos-de-la-despensa-1..4 en el apéndice de resueltas) actualizados como preveía la spec; `AGENTS.md`, `ARCHITECTURITY.md` y `CONSTRAINTS.md` sin cambios, justificados en la spec.

## Hallazgos no bloqueantes (mejoras sugeridas)

1. **Menor — estilo de los campos del formulario**: los `OutlinedTextField` del diálogo de alta/edición usan los colores por defecto de M3, no el estilo relleno validado en `afinar-fidelidad-visual-del-formulario` (`warmFieldColors()` es privado de `CrearRecetaScreen`). El resultado visual es coherente y aceptable; para paridad exacta, extraer `warmFieldColors` a un componente compartido y reutilizarlo en `PantryItemDialog`.
2. **Menor — a11y de la acción de la barra superior**: el `contentDescription` «Despensa» está en el `Icon` (nodo accesible de 24 dp) y no en el `IconButton` (48 dp). Moverlo al botón lo alinea con el patrón de las filas de la Despensa.
3. **Nota de entorno**: la suite E2E sufre abortos intermitentes del emulador (intento 1 abortado en `EliminarRecetaE2ETest` con crash del proceso de tests); con el AVD reiniciado en frío pasa 67/67. Además, `uiautomator dump` omite el nodo del FAB en esta configuración (se verificó por captura).

## Seguimiento del orquestador

- Resumir este informe y las 2 capturas en `evidence` de `feature_list.json` y persistir el estado `accepted` (el validador no modifica `feature_list.json`).
- Actualizar `PROGRESS.md`: la línea «permanece `in_progress` hasta que el orquestador la pase a `passing`» está superada; reflejar la aceptación y la validación visual completada.

## Veredicto

**accept** — La implementación cumple la spec y sus dos `verification` ejecutados en el emulador, la migración 1→2 se comprobó en dispositivo sin pérdida de datos, la suite completa está en verde de forma independiente (unit 27/27, lint 0 errores, E2E 67/67) y no hay hallazgos bloqueantes en alcance, arquitectura, seguridad ni documentación.

---

# Revalidación del delta stepper 2026-10-20 (sesión 44)

- **Estado en `feature_list.json` al validar**: `passing` (delta R1-R6 de la «Revisión 2026-10-20» de la spec: cantidad por stepper entero).
- **Validador**: `feature-validator` independiente (posterior a la sesión 44 del implementer).
- **Veredicto**: **accept**

## Comprobaciones reraneadas por el validador

| Comando | Resultado |
|---|---|
| `init.ps1` | exit 0 — «Entorno listo» |
| `lintDebug` | exit 0 — BUILD SUCCESSFUL, 0 errores (31 avisos preexistentes ajenos a la feature) |
| `testDebugUnitTest --rerun-tasks` | exit 0 — **28/28**, 0 fallos (`PantryItemValidationTest` 4/4, `InMemoryPantryRepositoryTest` 5/5) |
| `assembleDebug` | BUILD SUCCESSFUL |
| E2E focalizadas | `PantryManagementE2ETest` **5/5** + `PantryLocalizationE2ETest` **2/2** |
| `connectedDebugAndroidTest` completo | exit 0 — **68/68, 0 fallos y 0 omitidos** (`Pixel_10_Pro` API 37, 6m54s; incluye regresión de Crear/Editar/Eliminar/Buscar/MisRecetas) |
| Migración 1→2 | no reraneada, justificada: `RecipeDatabase.kt`, `PantryItemEntity.kt` y `PantryItemDao.kt` no aparecen en el diff (esquema v2 intacto, sin `fallbackToDestructiveMigration`) |

## Verificación manual del delta (adb/uiautomator, es-ES y en-US)

- **S4**: tres toques en «+» → tarjeta «Tomate fresco / **Cantidad: 3**» (entero, sin decimal).
- **S7**: el diálogo tiene **solo 1 `EditText`** (el nombre); «−» `enabled=false` en 0 y «+» `enabled=false` tras 999 toques (contenedores 144×144 px = **48 dp**); guardar con 0 → tarjeta **sin línea de cantidad** y edición que reabre en 0 (ida y vuelta `0 ↔ null` correcta).
- **S10**: locale por aplicación `en-US` → «Pantry», «Quantity (optional)», «Decrease/Increase quantity», «Delete “X”?»; locale revertido al terminar.
- Estado dejado: Despensa vacía (alimento de prueba borrado con confirmación, revalidando S2 en inglés), app instalada, sin datos de prueba.

## Hallazgos no bloqueantes del delta

1. **Medio** — este informe estaba desactualizado respecto al delta (cifras de la sesión 42); se corrigió con esta sección al persistir la aceptación.
2. **Bajo** — el descarte del diálogo de borrado por «atrás/toque fuera» (S3) no está cubierto por E2E (solo el botón «Cancelar»); preexiste al delta.
3. **Bajo** — el protector de decimales heredados (`roundToInt` + clamp al leer de Room, p. ej. 3,5 → 4) no tiene test automatizado (Room sin Robolectric); es una guarda defensiva documentada en la spec.
4. **Bajo** — `stepperEnforcesIntegerBoundsAndZeroSavesWithoutQuantity` tarda ~104 s (999 `performClick()`); candidato a abaratar en el futuro.
5. **Observación** — el working tree contiene una modificación ajena a la feature: `.opencode/skills/feature-validator/SKILL.md`; debe quedar fuera del commit de la feature.

## Veredicto del delta

**accept** — el stepper entero 0..999 con 0 = sin cantidad cumple la revisión de la spec: gate completo reraneado en verde (unit 28/28, lint 0 errores, E2E 68/68), verificación manual S4/S7/S10 conforme, sin restos del campo de texto (grep), Room v2 intacto, docs duraderas (`docs/domain-model.md`, `docs/risks-and-open-questions.md`) actualizadas y sin hallazgos de seguridad.