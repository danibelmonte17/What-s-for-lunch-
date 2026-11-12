# Informe de validación — `buscar-y-filtrar-recetas`

- **Fecha**: 2026-10-20 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: `accept`
- **Spec**: `docs/specs/buscar-y-filtrar-recetas.md` (reapertura de fidelidad visual V1-V3; Verificación Plan y Evidence To Capture vigentes).
- **Entorno**: AVD `Pixel_10_Pro` API 37 (density 480 → 3 px/dp), `adb`/`emulator` de `%LOCALAPPDATA%\Android\Sdk`, JDK 17.
- **Diff revisado**: `MisRecetasScreen.kt` (buscador V1, chips V2, estado «sin resultados» V3), `BuscarYFiltrarRecetasE2ETest.kt` (test de estructura visual) y docs de estado (`feature_list.json`, `PROGRESS.md`, `docs/progress-history.md`, spec). Sin cambios en `res/`, `domain/`, Gradle ni scripts del harness.

## 1. Gate reranqueado

| Comando | Resultado | Detalle |
|---|---|---|
| `./init.ps1` | **exit 1** (artefacto conocido) | `installDebug` BUILD SUCCESSFUL, APK instalado, `monkey` inyecta el launch y la app queda viva verificada a mano (`pidof`). El exit 1 es la carrera conocida de `pidof` documentada en PROGRESS.md, no una regresión. |
| `./lint.ps1` (`lintDebug`) | **exit 0** | BUILD SUCCESSFUL, 0 errores (`.harness/validator-lint.log`). |
| `./unit.ps1` (`testDebugUnitTest`) | **exit 0** | **19/19**, 0 fallos (recuento desde los XML de `app/build/test-results`; `.harness/validator-unit.log`). |
| `./e2e.ps1` (`connectedDebugAndroidTest`) | **exit 0** | **61/61, 0 fallos, 0 skipped, 170,6 s** (`TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`; `.harness/validator-e2e.log`). Incluye `BuscarYFiltrarRecetasE2ETest` completo con el test nuevo `searchAndMomentControlsExposeTheirVisualStructure`. |

Nota de entorno: la primera ejecución de `e2e.ps1` murió por contención de `adb` (clientes colgados y emulador en `offline`); se reinició el AVD y se repitió completa en verde. Sin impacto en el código.

## 2. Escenarios S1-S12

| Escenario | Resultado | Evidencia |
|---|---|---|
| S1 filtrar por Cena | pasa | E2E `filtersByDinnerAndOffersAllMomentsWithAllSelectedByDefault` + manual: 3 Borradores sembrados por la UI real (Desayuno/Almuerzo/Cena) → chip «Cena» → solo «Sopa de cebolla» (captura) |
| S2 buscar por ingrediente | pasa | E2E `searchesIngredientsCombinesCriteriaShowsNoResultsAndResets` |
| S3 combinado | pasa | mismo E2E + unit `combinesTextAndMomentAndKeepsRepositoryOrderIncludingDrafts` |
| S4 debounce 300 ms | pasa | unit `searchAppliesOnceThreeHundredMillisecondsAfterTheLastKeystroke` |
| S5 comparación normalizada | pasa | unit `ignoresCaseAccentsAndOuterWhitespace` |
| S6 borrar buscador con filtro | pasa | E2E `clearingSearchKeepsMomentFilter` |
| S7 sin resultados y restablecer | pasa | E2E + manual: «zzzz» → tarjeta con controles visibles (captura) → «Ver todas las recetas» → buscador vacío, «Todas» activo y las 3 recetas de nuevo |
| S8 colección vacía | pasa | E2E `emptyCollectionHidesSearchAndFilterControls` |
| S9 valores iniciales y selección única | pasa | E2E + manual: tras relanzar la app, buscador vacío y «Todas» activo |
| S10 estado en memoria | pasa | E2E `keepsSearchAndMomentFilterWhenReturningWithinTheSession` + manual (relanzado ⇒ valores iniciales; recetas persistidas en Room) |
| S11 orden y estados conservados | pasa | unit `combinesTextAndMomentAndKeepsRepositoryOrderIncludingDrafts` |
| S12 fidelidad visual | pasa | §3 de este informe |

## 3. Fidelidad V1-V3 (S12)

Capturas nuevas (es; locale por aplicación `es-ES` con `cmd locale set-app-locales`, revertido al terminar):

- `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-filtro-cena.png` (1280×2856) — reemplaza a la del ciclo 1/6.
- `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-sin-resultados.png` (1280×2856) — ídem.

Muestreo de píxeles sobre las capturas (3 px/dp):

| Rol | Esperado | Muestreado |
|---|---|---|
| Fondo `surface` | #FBF9F5 | `FBF9F5` |
| Contenedor del buscador `surface-container-lowest` | #FFFFFF | `FFFFFF` |
| Chip activo `primary` | #A83212 | `A83212` |
| Chip inactivo `surface-container-lowest` | #FFFFFF | `FFFFFF` (Almuerzo y Postre) |
| Texto del chip activo `on-primary` | #FFFFFF | `FFFFFF` (píxel más claro del glifo) |
| Tarjeta «sin resultados» | #FFFFFF | `FFFFFF` |
| Círculo del icono `surface-container` | #EFEEEA | `EFEEEA` |
| Icono `search_off` `primary` | #A83212 | `A83212` (distancia 0) |
| Botón «Ver todas las recetas» `secondary` | #006C4A | `006C4A` |

`uiautomator` (estructura, iconos, textos y objetivos):

- **V1 buscador**: contenedor 1184×144 px → alto 48 dp; icono 60×60 px = 20 dp a 12 dp del borde; el texto arranca a 40 dp; hint y placeholder en su sitio. ✓
- **V2 chips**: caja táctil 144 px = 48 dp; píldora 114 px ≈ 38 dp; icono 54×54 px = 18 dp por chip; separación 24 px = 8 dp; margen 16 dp; rol `RadioButton` (selección única) y sin checkmark. ✓
- **V3 sin resultados**: círculo 168×168 px = 56 dp; icono 84×84 px = 28 dp; botón visual 108 px = 36 dp con caja táctil 144 px = 48 dp; buscador y chips visibles sobre la tarjeta. ✓

## 4. Seguridad

Checklist por feature sin hallazgos: el diff no introduce secretos, red, ficheros ni permisos; la búsqueda es `contains` en memoria sobre la lista ya cargada (el DAO usa solo `@Query` parametrizadas de Room, sin consultas construidas con la entrada); las capturas contienen solo recetas de prueba ficticias.

## 5. Alcance, docs y evidencia

- El diff coincide con `Expected File Changes`; sin copy nuevo, sin tocar `strings.xml`, `domain/`, Gradle ni harness; sin trabajo de otras features.
- Presupuesto de evidencia respetado: 1 informe (este) + 2 capturas que reemplazan al set anterior.
- `feature_list.json` (`passing` con evidence) y `PROGRESS.md` coherentes; el traspaso a `accepted` lo gestiona el orquestador.
- Incidente de esta validación (transparencia): al marcar el `Validator Checklist`, PowerShell 5.1 dobló y después vació `docs/specs/buscar-y-filtrar-recetas.md`; se reconstruyó íntegro (250 líneas, verificado contra la lectura de la sesión) con el checklist ya marcado. Ningún fichero de producto tocado.

## 6. Veredicto

**`accept`** — S1-S12 en verde, V1-V3 con fidelidad comprobada por muestreo de píxeles y `uiautomator`, sin hallazgos de seguridad, alcance correcto y harness consistente. Recomendación al orquestador: `status` → `accepted` en `feature_list.json` resumiendo esta evidencia.