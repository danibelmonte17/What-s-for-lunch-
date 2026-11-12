# Validación — `eliminar-receta` (ciclo 6, modo delta)

- Fecha: 2026-10-19 · Modo: **delta** (§3.0). Válvula comprobada: el único cambio desde el ciclo 5 es `app/src/androidTest/java/com/whatsforlunch/app/EliminarRecetaE2ETest.kt` (área de test de B-6), hay informe previo (ciclo 5) y no se toca producto.
- Entorno: AVD `Pixel_10_Pro` (API 37, 480 dpi / densidad 3.0, `emulator-5554`), locale de sistema `fr-FR`; la localización de los tests ya no depende del entorno (ver delta).
- Spec: `docs/specs/eliminar-receta.md` (APTA, intento 2; 8 RF + 2 NFR) · `feature_list.json`: `verify` = 2 E2E.
- Delta verificado en el código: `launchScreen` construye el contexto localizado con `baseContext.createConfigurationContext(configuration)` (`:83`) y lo propaga a **toda** la composición con `CompositionLocalProvider(LocalContext provides …, LocalConfiguration provides …, LocalResources provides …)` (`:90-94`, patrón de `MisRecetasLocalizationE2ETest`), de modo que las aserciones de locale (`:119`, `:206`) dependen del locale inyectado y no del de la app/sistema. El matcher ambiguo `onNodeWithText("Delete")` (2 nodos en el ciclo 5) se sustituye por `onNodeWithTag(MisRecetasTestTags.DELETE_CONFIRM)` (usos en 131/175/189/201/219).

## Harness (salida contenida en `.harness/`)

| Check | Exit | Resultado | Evidencia |
|---|---|---|---|
| `init.ps1` | **0** | `installDebug` + arranque OK (1ª ejecución: carrera conocida del `pidof`, app en ejecución con pid 3609; 2ª repetición exit 0) | `.harness/init.log`, `.harness/init2.log` |
| `lint.ps1` | **0** | `lintDebug` `BUILD SUCCESSFUL in 13s`, sin errores | `.harness/lint.log` |
| `unit.ps1` | **0** | `BUILD SUCCESSFUL`, **12 tests, 0 fallos** | `.harness/unit.log` |
| `e2e.ps1` | **0** | `connectedDebugAndroidTest` ejecuta **53 tests: 53 pasan, 0 fallan, 0 saltados** (`BUILD SUCCESSFUL in 3m 9s`) | `.harness/e2e.log` + XML de instrumentación |

**B-6 cerrado:** `deleteDialogAndActionAreLocalizedInEnglish` **pasa** con el locale inyectado por `launchScreen` (sin depender del locale de la app/dispositivo) y el matcher por tag unívoco elimina la ambigüedad de «Delete». Confirmado con el XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml` (`tests=53 failures=0 errors=0 skipped=0`), que incluye los 5 tests de `EliminarRecetaE2ETest`.

## Trazabilidad

| RF/NFR | Verificación de la spec | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | E2E + nombre en el diálogo | **pasa** | E2E `confirmingDeleteRemovesRecipePermanentlyAndShowsEmptyState` (asert. de `«Eliminar receta»` y del mensaje con el nombre, ahora con contexto localizado inyectado) |
| RF-2 | verify E2E «eliminar… desaparece de Mis Recetas» | **pasa** | E2E `confirmingDeleteRemovesRecipePermanentlyAndShowsEmptyState` (receta ausente + `EMPTY_TITLE`) |
| RF-3 | unit borrado definitivo | **pasa** | `unit` exit 0 (`InMemoryRecipeRepositoryDeleteTest`); E2E `recipeWithPhotoDeletesItsStoredPhotoFile` → `getRecipe(id) == null`, sin papelera ni deshacer |
| RF-4 | verify E2E «cancelar… sigue en Mis Recetas» | **pasa** | E2E `cancellingDeleteKeepsRecipe` |
| RF-5 | E2E descartar con atrás | **pasa** | E2E `pressingBackDismissesDialogWithoutDeletingRecipe` (`Espresso.pressBack()`): diálogo inexistente y receta persistente |
| RF-6 | E2E almacenamiento que falla | **pasa** | E2E `storageFailureShowsErrorAndKeepsRecipeVisible`: aviso «No se pudo eliminar…» y receta visible |
| RF-7 | unit id inexistente + UI | **pasa** | unit no-op + E2E `confirmingRecipeAlreadyRemovedClosesDialogWithoutError` (cierra sin error ni aviso) |
| RF-8 | E2E con `PhotoStore` falso | **pasa** | E2E `recipeWithPhotoDeletesItsStoredPhotoFile`: `deletedPaths == [photoPath]` |
| NFR-1 | textos es/en | **pasa** | **E2E en verde con el delta**: `deleteDialogAndActionAreLocalizedInEnglish` (diálogo y acción en `en-US` con locale inyectado) y `storageFailureMessageIsLocalizedInEnglish`; es: resto de tests de `EliminarRecetaE2ETest` en verde. Manual del ciclo 5 (capturas `en-US`) vigente: el producto no cambió |
| NFR-2 | uiautomator ≥48 dp y nombre accesible | **pasa** | E2E `deleteActionHasAccessibleNameAndMeetsFooterSizeRequirements` (nombre «Eliminar receta», ≥48 dp de ancho, ≥52 dp de alto, a la derecha de «Editar»); manual ciclo 5: nodo `[914,572][1184,728]` = 270×156 px = **90×52 dp** |

Los 2 checks de `verify` de `feature_list.json` (E2E de borrado con confirmación y E2E de cancelación) **pasan** en esta ejecución (`e2e` exit 0, 53/53).

## Fidelidad visual

El delta no toca UI, así que la comparación de los ciclos 4/5 (5 bloques, **0 divergencias abiertas**, 2 desviaciones declaradas aceptadas) sigue vigente. Re-muestreo de píxeles sobre la captura del diálogo (en-US): contenedor `#EAE8E4` (`surface-container-high`), «Cancel» `#A83212` (`primary`) y «Delete» `#BA1A1A` (`error`) → conformes con los tokens de Q-eliminar-receta-1; el pie de la tarjeta mantiene los dos botones primarios `#A83212` con texto blanco (desviación declarada Q-eliminar-receta-4 → Q-editar-receta-6). El diálogo sigue siendo una extensión declarada (el prototipo usa `confirm()` nativo).

## Veredicto

**PASA** — `e2e` exit 0 con **53/53** y `init`/`lint`/`unit` en verde: se cumplen el criterio «`e2e` en verde sin regresiones» y los 2 `verify` de `feature_list.json`. **B-6 queda cerrado**: el fallo del ciclo 5 era del test (locale acoplado al entorno y matcher ambiguo), no del producto; el delta solo cambia `EliminarRecetaE2ETest.kt` y lo resuelve. Los 8 RF y los 2 NFR pasan su verificación → la feature queda **`validated`**.

Nota de entorno: el picker del emulador sigue sin usarse — `recipeWithPhotoDeletesItsStoredPhotoFile` (RF-8) se apoya en el `PhotoStore` falso del E2E, como en el ciclo 5.

## Evidencia

- `docs/evidence/eliminar-receta/informe-validacion.md` (este informe; reemplaza al del ciclo 5).
- `docs/evidence/eliminar-receta/dialogo-confirmar-borrado.png` — diálogo de confirmación en `en-US` con el nombre de la receta (RF-1/NFR-1; captura del recorrido manual del ciclo 5, vigente: el producto no cambió en el delta).
- `docs/evidence/eliminar-receta/mis-recetas-tras-borrar.png` — Mis Recetas tras confirmar el borrado: estado vacío (RF-2; ídem).

Presupuesto: 3 artefactos (1 informe + 2 capturas). Logs completos en `.harness/`.

### Ciclo 7 (2026-10-19) — alineación compacta de «Eliminar» con «Editar»

- **Cambio pedido por el usuario:** el botón «Eliminar» conservaba el tamaño grande de la Iteración 2 (icono 18 dp, altura visual 52 dp) mientras «Editar» ya se había compactado a 16 dp / 36 dp visuales. Se igualan para que el pie de la tarjeta sea homogéneo (Q-eliminar-receta-4, Q-editar-receta-6).
- **Alcance:** `MisRecetasScreen.kt` (`DeleteRecipeButton`: `heightIn(min = 36.dp)` e icono de 16 dp), `EliminarRecetaE2ETest.deleteActionHasAccessibleNameAndMeetsFooterSizeRequirements` (altura visual ≥ 36 dp e igual a la de «Editar»; el área táctil ≥ 48 dp la mantiene `minimumInteractiveComponentSize`), specs `eliminar-receta`/`editar-receta`, `DESIGN.md` y `risks-and-open-questions.md`. Sin cambios de flujo.
- **Verificación directa (sin scripts del harness):** `lintDebug` + `testDebugUnitTest` `BUILD SUCCESSFUL` (12 tests, 0 fallos) y `connectedDebugAndroidTest` `BUILD SUCCESSFUL` con **53 tests: 53 pasan, 0 fallan, 0 saltados** (4 m 4 s) en `Pixel_10_Pro` (API 37, 480 dpi). El primer intento del E2E dio 1 fallo en el propio test (asumía altura ≥ 52 dp); se corrigió el umbral a la altura visual real y se repitió en verde.
- **NFR-2 medido con `uiautomator` (densidad 3.0):** «Editar receta» `[96,590][345,698]` = 249×108 px = **83×36 dp** y «Eliminar receta» `[896,590][1184,698]` = 288×108 px = **96×36 dp**; ambas alturas coinciden. Nombre accesible «Eliminar receta» presente. El área táctil de 48 dp la garantiza el mínimo interactivo de Material 3.
- **Veredicto:** alineación verificada; `eliminar-receta` sigue **`validated`** (los 2 `verify` de `feature_list.json` y el resto de la suite, en verde).
