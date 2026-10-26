# Evidencia de implementación — `anadir-faltantes-a-la-lista`

- **Fecha**: 2026-10-26
- **Rol**: `feature-implementer` (autoverificación; **no** es aceptación)
- **Spec**: `docs/specs/anadir-faltantes-a-la-lista.md`
- **Estado resultante**: `accepted` tras la revalidación independiente de la sesión 85 (la autoverificación inicial quedó en `passing`)

## Entorno

- Windows 10/11, JDK 17 fijado en `gradle.properties`, Gradle Kotlin DSL.
- Emulador **AVD `Pixel_10_Pro`, API 37**, serial `emulator-5554` (arrancado para esta sesión).
- App instalada con `installDebug` para las capturas; locale de la app `es-ES` (`cmd locale set-app-locales`).
- Sin llamadas reales a DevExpert Inference: la feature no usa IA y las E2E usan fixtures locales y `FakePincheChatClient`. Sin coste y sin API keys en ficheros, logs ni evidencia.

## Comandos ejecutados y resultado

| Gate | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` | **exit 0** («Entorno listo»; AVD `Pixel_10_Pro` API 37, `emulator-5554`, app instalada y en ejecución) |
| Estático | `./lint.ps1` (`lintDebug`, `--rerun-tasks` en la revalidación) | **BUILD SUCCESSFUL**, **0 errores / 49 warnings** (sin warnings nuevos de la feature) |
| Unitarios | `./unit.ps1` (`testDebugUnitTest --rerun-tasks`) | **205/205 en 34 suites**, 0 fallos, 0 errores, 0 omitidos |
| APK | `assembleDebug` | **BUILD SUCCESSFUL** |
| E2E | `./e2e.ps1` (`connectedDebugAndroidTest`) | **164 pruebas, 0 fallos, 0 errores, 2 omitidas** (`SettingsE2ETest` por `Assume`), 446,233 s |
| Integración S5 | `ShoppingListFlowTest` | **1/1** (Ficha → guardar → Detalle del Borrador sin duplicar) |

Nota de entorno: `init.ps1`/`e2e.ps1` deben ejecutarse **sin canalizar stdout** en PowerShell; con la salida canalizada, el falso fallo conocido de `adb shell monkey` (`args: [...]` por stderr con `$ErrorActionPreference = "Stop"`) aborta el wrapper. La ejecución directa termina en verde.

## Desglose de escenarios

`ShoppingListE2ETest` ejecutada completa: **10/10 en verde** (S1-S8 + migración 2→3 + persistencia Room).

| Escenario | Test | Resultado |
|---|---|---|
| S1 · Detalle con faltantes → Lista con Origen (verify 1) | `detailAddsMissingItemsToTheShoppingListWithOrigin` | ✅ |
| S2 · repetir la acción no duplica ni dobla (verify 2) | `repeatingTheActionDoesNotDuplicateOrDoubleTheItems` | ✅ |
| S3 · dos Recetas suman y el badge cuenta fuentes | `twoRecipesAddUpAndTheBadgeCountsSources` | ✅ |
| S4 · tarjeta de «Recetas con mi Despensa» («Te falta: …» + carrito) | `recommendationCardAddsItsMissingItems` | ✅ |
| S5 · Ficha del Pinche (UI y aviso deshabilitado) | `pincheProposalAddsMissingItemsAndBecomesDisabled` | ✅ |
| S5 · Ficha → guardar → Detalle del Borrador (integración) | `ShoppingListFlowTest.savingTheProposalAndRepeatingFromTheDraftDoesNotDuplicateTheItem` | ✅ 1 ítem, cantidad sin doblar y una sola fuente |
| S6 · borrado con «Deshacer» | `deletingAnItemShowsUndoAndRestoresIt` | ✅ |
| S7 · Lista vacía con CTA «Escanear la Despensa» | `emptyListShowsTheScanPantryCta` | ✅ |
| S8 · sin Faltantes no hay acción | `fullMatchHidesTheAddActions` | ✅ |
| Migración 2→3 + persistencia Room | `migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables` + `roomRepositoryKeepsItemsAcrossReopenAndRestoresDeletedSources` | ✅ |

Cobertura unitaria añadida: `ShoppingListRulesTest` (obligatorios, orden, sin duplicados, suma por fuente, idempotencia, unidades distintas, cantidad 0), `InMemoryShoppingListRepositoryTest`, `ShoppingListViewModelTest`, la integración `ShoppingListFlowTest` y ampliaciones de `RecipeDetailViewModelTest`, `RecommendationsViewModelTest` y `PincheViewModelTest`. `PincheProposalRulesTest` sigue pasando sin cambios (la delegación en `ShoppingListRules.missingRequired` conserva el comportamiento salvo la exclusión de nombres normalizados vacíos).

## Cierre de la reparación y revalidación (sesión 85)

- **H1 resuelto**: `ShoppingListFlowTest` ejecuta Ficha → Añadir faltantes → Guardar → Detalle del Borrador → Añadir a la lista y demuestra un único ítem, cantidad sin doblar y una sola fuente con la clave de RD16. El E2E de S5 conserva la comprobación del botón «Añadido…» deshabilitado; la UI del badge ya está cubierta por S1/S3/S6.
- **L1 resuelto**: «Para: N recetas» usa `<plurals>` + `pluralStringResource`; `PincheScreen` y `RecommendationsScreen` colocan `modifier` como primer opcional. Lint queda en **0 errores / 49 warnings**, sin warnings nuevos.
- **L2 resuelto**: `PincheViewModel.addMissingToShoppingList` revierte `addedToList` y muestra `SHOPPING_LIST_FAILED` si falla la persistencia, con test dedicado.
- **L3 resuelto**: `ShoppingListE2ETest` cubre Room con cierre/reapertura, borrado/restauración y la migración 2→3 con inserción/lectura de un ÍtemLista y su fuente.
- **L4 resuelto**: spec en `status: accepted` (281 líneas), recuentos y matiz de nombres normalizados vacíos actualizados en `docs/domain-model.md` y `docs/technical-discovery.md`.
- **Paridad es/en**: **266/266** (265 strings + 1 plural por idioma), sin claves solo-es o solo-en. Las 2 PNG de evidencia siguen siendo válidas (1280×2856).
- **Veredicto independiente**: `accept`; sin hallazgos Critical/High/Medium ni de seguridad. Informe completo en `docs/validations/anadir-faltantes-a-la-lista.md` §9.

## Migración 2→3

- `RecipeDatabase` pasa a `version = 3` con `MIGRATION_2_3`, que crea `shopping_items` (índice único por `normalizedName`) y `shopping_item_sources` (PK `itemId` + `sourceKey`, FK `ON DELETE CASCADE`).
- El test de migración crea una base **v2** con Recetas, Ingredientes y Despensa, abre la app y comprueba que los datos previos siguen ahí y que las dos tablas nuevas existen y son operativas.
- El repositorio Room guarda ítem y fuentes en una transacción (`Mutex` + `withTransaction`) y reinscribe el padre con `INSERT OR REPLACE`, de modo que la cascada no deja fuentes huérfanas.

## Contrato visual verificado

- Detalle con Faltantes: banner «¡Tienes el X% de ingredientes en casa!» y acción «Añadir a la lista».
- Un ítem por nombre normalizado con badge «Para: {Receta}» o «Para: N recetas» (títulos en la descripción accesible).
- Con dos Recetas que comparten «Harina»: una única fila `Harina (700 g)` con «Para: 2 recetas».
- Lista plana ordenada por nombre, cantidad entre paréntesis y borrado con «Deshacer»; estado vacío con CTA al Escáner.

## Capturas

Ambas en español, PNG binarias válidas (cabecera `89 50 4E 47 0D 0A 1A 0A`), 1280×2856:

1. `detalle-faltantes-es.png` (203.287 B): Detalle de Receta con el banner de Coincidencia y «Añadir a la lista».
2. `lista-compra-es.png` (157.050 B): Lista de la Compra con «Harina (700 g)» y badge «Para: 2 recetas».

Datos de demostración sembrados en una copia de la base Room (Tortilla de patatas, Bizcocho de yogur y Galletas de avena; Despensa con Huevos y Patatas), con la app relanzada para leer la base actualizada. No se modificó ningún dato del repositorio ni se usaron keys reales.

## Alcance respetado

- Sin alta manual ni por voz, sin Pasillo, sin «Despensa baja» (`anadir-item-a-la-lista`).
- Sin tabs, checkboxes, barra de progreso ni agrupación (`agrupar-y-completar-la-lista`); `completed` existe pero siempre es `false`.
- Sin cambios en `calculateMatch`/RD01/RD02/RD10/RD13, `SuggestionRules`, `SuggestionAcceptance`, `ServingsScaling` ni en el esquema de Recetas/Despensa.
- Sin IA, sin llamadas a la API y sin commits.
- `feature_list.json`: solo se actualizan `status` y `evidence`.
