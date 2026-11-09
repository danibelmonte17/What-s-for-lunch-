# Validación independiente: `mover-escaneo-a-la-despensa`

- **Fecha**: 2026-11-09.
- **Validador**: `feature-validator` (subagente independiente, sesión propia; no participó en la implementación).
- **Spec (contrato)**: `docs/specs/mover-escaneo-a-la-despensa.md` (S1-S5, D1-D6, Verification Plan, Evidence To Capture).
- **Estado en `feature_list.json` al validar**: `passing` (autoverificación del implementador; no es aceptación).
- **Veredicto**: **`accept`** -> estado recomendado para el orquestador: **`accepted`** (la persistencia del acceptance la hace el orquestador; este validador no edita `feature_list.json`, `PROGRESS.md` ni `docs/progress-history.md`).

---

## 1. Qué se re-ejecutó (tier barato) y qué se inspeccionó (tier caro)

### Re-ejecutado por el validador (sobre el árbol actual)

| Comando | Resultado | Coincide con la evidencia |
|---|---|---|
| `./init.ps1` | **exit 0: «Entorno listo», app `com.whatsforlunch.app` en ejecución** (`installDebug` + `monkey` + `pidof` verificados) | Sí («hoy dio exit 0») |
| `./gradlew.bat lintDebug --rerun-tasks` | **BUILD SUCCESSFUL, 0 errores / 49 warnings** (recuento propio desde `app/build/reports/lint-results-debug.xml`; **0 incidencias** apuntan a ficheros del diff) | Sí (línea base 0/49) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL: 42 suites / 305 tests / 0 fallos / 0 errores / 0 omitidos** (recuento propio desde `app/build/test-results/testDebugUnitTest/*.xml`) | Sí (42/305/0) |
| `./gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** | Sí |
| Paridad es/en (recuento propio) | **323/323 claves + 5/5 plurales, 0 diferencias**; `pantry_scan_food` en `values/` y `values-en/`; `action_scan_pantry` conservado en ambos | Sí (nueva línea base) |
| Inventario `@Test` en `app/src/androidTest` (recuento propio) | **224 `@Test` en 25 ficheros con tests / 27 clases** (`CreateRecipeE2ETest.kt` 26 = 3 clases; `EditRecipeE2ETest.kt` 14 = 3 clases) | Sí |
| Reconciliación propia de los 7 XML `e2e-bloque*.xml` | **224 tests / 0 fallos / 0 errores / 1 omitido**, 27 clases por `classname`, suma por bloque 44+40+35+25+44+5+31 = 224 | Sí (tabla del `informe.md`) |
| Cabeceras y dimensiones de los 3 PNG | `89 50 4E 47`, **1280x2856** en los tres | Sí |
| `feature_list.json` | JSON válido; de la entrada solo cambian `status` (`passing`) y `evidence` (6 items), un único hunk en el diff | Sí |

Nota sobre `init.ps1`: si el invocador redirige el stderr de `adb` (`2>` / `2>&1`), PowerShell eleva el `NativeCommandError` de `adb.cmd` con `$ErrorActionPreference='Stop'` y el script aborta en el paso `monkey` con exit 1 antes de «Verificando». Ejecutado sin redirección (como lo ejecuta el orquestador) sale **exit 0 «Entorno listo»**, verificado por el validador. Es artefacto de invocación (patrón histórico documentado), no un defecto del repositorio.

### Solo inspeccionado (tier caro, con evidencia exacta)

- **E2E focalizada** (`e2e-focalizada.xml`): **61 tests / 0 fallos / 0 errores / 0 omitidos**; por clase `ShoppingListE2ETest` 27 + `PantryManagementE2ETest` 9 + `ScanPantryE2ETest` 25 = 61; timestamp `2026-11-09T08:55:26Z` (09:55 local), posterior al último cambio de código (2026-11-08 12:37 local). Cubre verify 1 (S7 nueva) y verify 2 (FAB -> Escáner y vuelta tras guardar).
- **Batería completa por bloques** (7 XML): **224 / 0 / 0 / 1**, coherentes 1:1 con el `informe.md` y con el inventario. El 1 omitido es exactamente `SettingsE2ETest.buildProvidedKeyShowsConfiguredNoteAndHidesDelete` (`assumeTrue(buildKeyActive)` sin `AI_API_KEY` en `local.properties`); la explicación de por qué la línea base era de 2 omitidos (build con clave) es creíble y consistente con la familia de `Assume` de esa suite.
- **Flake de B4**: en `e2e-bloque4-cookmode.xml`, `CookModeTimerE2ETest.resetRestoresTheCurrentMetaAndLeavesItRunning` figura **en verde con `time=8.537`**, coherente con el «reintentado sin tocar el test, 25/25 en 8,5 s»; el timestamp del bloque 4 (13:46 UTC) es el **último** de la batería, coherente con el relanzado tras el primer intento; la atribución a `ComposeTimeoutException` por saturación del host es creíble (host con Android Studio y sesiones concurrentes) y el resto de bloques está en verde.
- **Primer intento de focalizada con 4 fallos por locale**: sin XML retenido, pero documentado con causa (la verificación manual previa dejó la app en español; los tests esperan cadenas en inglés), remediación (`pm clear` + reinstalación) y resultado posterior 61/61. Creíble y consistente; artefacto del entorno, no del código.
- **Manual/adb verify 3** (`informe.md` §5, tabla es/en): `verify3-despensa-fabs.xml` confirma los bounds que afirma: FAB superior `[588,2376][1232,2544]` (Escaneo) sobre `[671,2568][1232,2736]` (Añadir); `verify3-lista-compra.xml` muestra título «Tu Lista de la Compra está vacía», mensaje intacto y **un solo** botón «Añadir ítem», sin rastro de «Escanear la Despensa». Las 3 capturas PNG son legítimas y su contenido visible coincide con lo afirmado: (1) Lista vacía solo con «Añadir ítem» (primario + FAB), sin acceso al Escáner; (2) Despensa vacía con CTAs «Escanear alimentos» (primario) sobre «Añadir alimento» y los dos FABs apilados en ese orden; (3) Despensa con «Arroz» y los FABs «Escanear alimentos» (cámara) encima / «Añadir alimento» abajo.
- **Regresión S5**: en la batería, `AppLaunchE2ETest` 1/1, `SearchAndFilterRecipesE2ETest` 12/12 y `MyRecipesLocalizationE2ETest` 6/6 en verde; en el código, `MyRecipesScreen` conserva `SCAN_PANTRY`/`SCAN_PANTRY_FAB` con `action_scan_pantry`.

No se re-ejecutó ninguna prueba de emulador: la evidencia es exacta (comando, recuento, fecha, entorno) y el diff **no ha cambiado desde el último XML** (última fuente modificada 2026-11-08 12:37 local; todos los XML, gates y capturas son posteriores), que es la condición para aceptar por inspección.

---

## 2. Validación contra la spec

- **Objetivo (D2)**: la Lista de la Compra perdió todo acceso al Escáner: `EMPTY_CTA` retirado de la UI, `onScanPantry` fuera de `ShoppingListScreen`/`ShoppingListRoute`/`AppNavHost`, «Añadir ítem» como único `Button` primario con su tag `EMPTY_ADD_CTA`, título y mensaje intactos (S1/verify 1 OK).
- **D1**: Despensa con `Column` de dos `ExtendedFloatingActionButton`: «Escanear alimentos» (`SCAN_FOOD`, `Icons.Outlined.PhotoCamera`, primario) **encima** y «Añadir alimento» (`ADD_ITEM`) **abajo**; orden asegurado en test y confirmado por captura/dump.
- **D3**: estado vacío de la Despensa con `SCAN_FOOD_CTA` (primario) y `EMPTY_ADD_CTA` (`OutlinedButton`, permitido por la spec) en ese orden (S4).
- **D4**: clave `pantry_scan_food` es/en («Escanear alimentos» / «Scan foods») y tags nuevos; `action_scan_pantry` **conservado** y en uso en Mis Recetas.
- **D5**: `launchSingleTop = true` en el `onSaved` del Escáner; test que guarda desde el Escáner abierto desde la Despensa, vuelve con el alimento y comprueba que «Volver» lleva a Mis Recetas **sin PANTRY duplicado**.
- **D6 / non-goals**: sin rastro de `mover-opciones-a-bottom-bar`; sin tocar el Escáner (`ScannerScreen`/`ScannerRoute`/`ScannerViewModel` ni sus ficheros), dominio/Room, top bar de Mis Recetas, `uxdocs/`, `.opencode/` ni scripts del harness; sin permisos, dependencias ni migraciones nuevos (`AndroidManifest` y `build.gradle*` fuera del diff).
- **Escenarios S1-S5** cubiertos por evidencia (S1, S2, S3 y S4 por E2E/manual; S5 por la batería). **Verification Plan** seguido en su integridad (init, gates, paridad, focalizada, batería, manual verify 3, regresión S5).
- **`verification` de `feature_list.json` (3 items) cubiertos 3/3** por la evidencia.

## 3. Cobertura E2E persistente (rubrica)

- `ShoppingListE2ETest`: S7 reescrita como `emptyListShowsNoScannerAccessAndOnlyTheAddItemCta`: asegura **0** nodos `EMPTY_CTA`, **0** «Escanear la Despensa», `EMPTY_ADD_CTA` y `ADD_ITEM` presentes; helper `launchShoppingList` sin `onScanPantry`.
- `PantryManagementE2ETest`: **+4 tests**: `emptyStateShowsScanAndAddCtas` (S4 + orden), `scanFoodFabIsStackedAboveTheAddFoodFab` (D1), `scanFoodFabOpensTheScannerFromThePantry` (S2) y `scannerSaveReturnsToThePantryWithTheSavedFoods` (S2 + D5). La cobertura E2E exigida por la spec existe y fue ejecutada por el implementador.

## 4. Alcance, arquitectura y riesgo

- **Scope**: los 16 ficheros modificados más 2 sin trackear coinciden con «Expected File Changes» de la spec, más dos desviaciones mínimas justificadas (notas N1/N2). Sin trabajo de otras features.
- **Arquitectura**: navegación por callbacks Route -> Screen cableados en `AppNavHost` (patrón del repo); UI sin reglas de negocio (solo layout, callbacks y textos); sin dependencias nuevas.
- **Mantenibilidad**: cambios pequeños y localizados; tests con tags y aserciones de orden explícitas; comentarios D1-D5 junto al código.

---

## 5. Seguridad (`security-checklist.md`, acotado al diff)

**Sin hallazgos.** Sin credenciales, tokens, API keys ni PII en el diff, los tests, los logs, los dumps ni las capturas (los PNG muestran solo UI y datos ficticios «Arroz»/«Tomate fresco»); sin cambios en permisos de `AndroidManifest` (la cámara sigue pidiéndose dentro del Escáner, RD05/RD06 intactos); sin llamadas de red nuevas ni cambios en el manejo de la clave de IA (`local.properties` intacto y `AI_API_KEY` no escrita en ningún sitio); sin inyección ni superficie de datos nueva (Room/dominio intactos); UI sin reglas de negocio.

## 6. Hallazgos

**Sin hallazgos Critical, High ni Medium.** Sin hallazgos que impidan la aceptación. Notas (sin cambio requerido):

- **N1 (Low, desviación justificada)**: `PantryLocalizationE2ETest.kt` y `ScanPantryE2ETest.kt` aparecen modificados (+1 línea cada uno: `onScanFood = {}`) aunque la spec solo los listaba como «no cambian» o no los incluía en «Expected File Changes». Son actualizaciones mínimas obligatorias por la compilación tras el nuevo parámetro de `PantryScreen`; no alteran aserciones ni comportamiento.
- **N2 (Low, wording)**: el `informe.md` §3 enumera los 4 tests nuevos de `PantryManagementE2ETest` como «S2 con FAB, S4, duplicado normalizado y vuelta desde el Escáner», enumeración holgada que no nombra por su cuenta el test de orden de FABs (`scanFoodFabIsStackedAboveTheAddFoodFab`). El recuento (4) es correcto y cada cobertura existe; solo el desglose es impreciso.
- **N3 (nota de entorno)**: `init.ps1` sale 1 si el invocador redirige el stderr de `adb` (ver §1); sin redirección, exit 0. Artefacto de invocación, no del repositorio; el script pertenece al harness y no debe tocarse.
- **N4 (nota)**: el primer intento de la focalizada (4 fallos por locale) no conserva XML; queda documentado en el `informe.md` con causa, remediación y resultado, y es coherente con el resto de la evidencia.

---

## 7. Docs durables y harness

- **Spec**: sección `Durable Documentation Impact` respetada: `ARCHITECTURE.md`, `CONSTRAINTS.md`, `AGENTS.md`, `docs/technical-discovery.md` y `CONTEXT.md` no requerían cambios y **no se tocaron** (`AGENTS.md` intacto y corto).
- **`DESIGN.md`**: fila «Lista de la Compra» (estado vacío sin CTA de escaneo) y «Estados a cubrir» (Mis Recetas conserva accesos; Lista solo «Añadir ítem»; Despensa con dos CTAs y FABs apilados) actualizados; sin contradicciones con el código. Las menciones residuales de «Escanear la Despensa» corresponden a Mis Recetas y al flujo del Escáner, vigentes.
- **`docs/domain-model.md`**: casuística «Lista de la Compra vacía» ajustada (RD19 sin CTA de escaneo); los restos en L201/L210 corresponden a Mis Recetas y al flujo del Escáner.
- **`PROGRESS.md` y `docs/progress-history.md`**: entradas de las sesiones 113/114 coherentes con la evidencia (leídos, no editados por el validador).
- **`feature_list.json`**: solo `status`/`evidence` en esta entrada; resto del fichero intacto.
- **Harness**: `init.ps1` en verde (exit 0 «Entorno listo»); sin ficheros temporales en el repositorio; sin procesos dejados por el validador (solo daemons estándar de Gradle).

## 8. Veredicto y seguimiento

- **Veredicto: `accept`.** La implementación cumple la spec (S1-S5, D1-D6), la evidencia es exacta y consistente, el tier barato se reprodujo en verde por el validador y el tier caro queda legítimamente cubierto por inspección; alcance, arquitectura y seguridad conformes; docs durables al día.
- **Estado recomendado**: `mover-escaneo-a-la-despensa` -> **`accepted`** en `feature_list.json` (solo `status`/`evidence`), con resumen de esta validación; entrada en `PROGRESS.md`/`docs/progress-history.md` por el orquestador. Este informe es el artefacto de evidencia (`docs/validations/mover-escaneo-a-la-despensa.md`).
- **Seguimiento**: sin repair brief. Commits/push solo por petición explícita del usuario (todo sigue sin commitear en `feature/mover-escaneo-a-la-despensa`). La siguiente candidata según AGENTS §2 sigue siendo `mover-opciones-a-bottom-bar` (`not_started`, 4 dependencias `accepted`).
