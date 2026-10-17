# Informe de validación — `crear-receta`

Fecha: 2026-10-17 · Ciclo 2 (tras el arreglo de B-1 y B-2) · Veredicto: **VALIDADA** · Estado final: `validated`

Entorno: macOS (Intel i5-8279U) · AVD `Pixel_10a` (API 37, 1080×2424, 420 dpi, x86_64) ya arrancado por el orquestador (`emulator-5554`), locale del sistema `en-US` · JDK 17 por override en `~/.gradle/gradle.properties` · AGP 8.13.2 / Gradle 8.14.3. Sin worktree: rama `feature/crear-receta` in situ, cambios sin comitear. El implementador solo tocó `CrearRecetaE2ETest.kt` (más los archivos nuevos de test); ninguna fuente de producto cambió respecto al ciclo 1.

## Resumen de ejecuciones

| Check | Comando | Resultado | Exit |
|---|---|---|---|
| init | `./init.sh` | **pasa** (compila, instala y abre; pid 32418) | 0 |
| lint | `./lint.sh` (`lintDebug`) | **pasa** (`BUILD SUCCESSFUL`, 0 errores) | 0 |
| unit | `./unit.sh` (`testDebugUnitTest`) | **pasa** (10 tests, 0 fallos: `RecipeValidationTest` 8 + `MisRecetasStateTest` 2) | 0 |
| e2e | `./e2e.sh` (`connectedDebugAndroidTest`) | **pasa** (22 tests, 0 fallos, 0 saltados) | 0 |

La suite pasó de 18 a 22 tests con los escenarios que faltaban. Todos los tests corren en el AVD con locale `en-US`, lo que confirma que la suite ya es independiente del idioma del dispositivo.

## Bugs de ciclo 1 — RESUELTOS ✅

- **B-1 (bloqueante, RF-14/RF-15)**: las aserciones de texto ya no dependen del idioma. `launchForm` inyecta el locale con `createConfigurationContext` + `CompositionLocalProvider(LocalContext provides formContext)` (mismo patrón que `MisRecetasLocalizationE2ETest`) y las cadenas se leen del contexto localizado con `formText(R.string.…)`. `saveDraftIsBlockedWithoutTitle` y `publishWithoutIngredientIsBlocked` pasan en `en-US`; además se añade `blockingErrorsAreLocalizedInEnglish`, que afirma los tres errores de bloqueo en inglés (NFR-1).
- **B-2 (cobertura)**: añadidos `editAndRemoveStepRows` (RF-10/RF-11: reemplaza el texto del Paso, le pone temporizador, lo elimina y comprueba el reordenado), `generatedPhotoCanBeAcceptedOrDiscarded` (RF-20: la imagen generada se ofrece en vista previa y se puede descartar) y `generationFailureShowsNoticeAndKeepsManualOptions` (RF-22: aviso de fallo sin bloquear galería/cámara ni el guardado).

## Trazabilidad (Parte A) — 26 RF

| RF | Verificación (spec) | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | verify 1 (formulario completo) | pasa | `uiautomator`: foto, título, historia, momento, tiempos, dificultad, raciones, calorías, favorito |
| RF-2 | E2E: las cuatro opciones | pasa | `newFormPreselectsLunch` + `uiautomator`: Desayuno/Almuerzo/Cena/Postre |
| RF-3 | unit (límite 1-12) | pasa | `servingsAreLimitedTo1To12` |
| RF-4 | unit (minutos ≥ 0) | pasa | `timesMustBeNonNegativeMinutes` |
| RF-5 | verify 1 (ingredientes) | pasa | `addAndRemoveIngredientRows`, `publishWithPhotoSavesPublishedRecipe` |
| RF-6 | E2E editar fila | pasa | `addAndRemoveIngredientRows` (nombre y cantidad editables) |
| RF-7 | E2E eliminar fila | pasa | `addAndRemoveIngredientRows` |
| RF-8 | unit (bloqueo Publicar) | pasa | `publishIsBlockedByAnInvalidIngredient` |
| RF-9 | verify 1 (pasos con temporizador) | pasa | `editAndRemoveStepRows` (texto + temporizador) |
| RF-10 | E2E editar Paso | pasa | `editAndRemoveStepRows` (`performTextReplacement` + temporizador) |
| RF-11 | E2E eliminar Paso | pasa | `editAndRemoveStepRows` (elimina y comprueba reordenado) |
| RF-12 | unit (bloqueo por Paso inválido) | pasa | `publishIsBlockedByAnInvalidStep` |
| RF-13 | verify 2 | pasa | `saveDraftWithoutPhotoSavesDraft` + `savedDraftAppearsInMisRecetas`; manual: tarjeta «Tortilla de patatas · Almuerzo · Borrador» |
| RF-14 | E2E error de título | pasa | `saveDraftIsBlockedWithoutTitle` (+ manual en dispositivo) |
| RF-15 | verify 3 (unit) + E2E de bloqueo | pasa | `publishIsBlockedWithoutIngredientsOrSteps` + `publishWithoutIngredientIsBlocked` |
| RF-16 | E2E publicar sin foto/título | pasa | `publishWithoutPhotoIsBlocked` (bloque de errores) + `publishIsBlockedWithoutPhotoOrTitle` (unit) |
| RF-17 | verify 1 | pasa | `publishWithPhotoSavesPublishedRecipe` |
| RF-18 | reinicio real | pasa | manual: `force-stop` + relanzado mantiene la tarjeta y su miniatura (Room) |
| RF-19 | manual `adb`/uiautomator | pasa | manual: «Galería» abre `com.google.android.photopicker`, se elige una imagen de MediaStore y el formulario pasa a «Quitar foto» (vista previa); + `publishWithPhotoSavesPublishedRecipe` |
| RF-20 | E2E con generador simulado | pasa | `generatedPhotoCanBeAcceptedOrDiscarded` (vista previa + «Quitar foto» → vuelven las opciones) |
| RF-21 | E2E sin API key | pasa | `generatePhotoWithoutApiKeyShowsNotice` |
| RF-22 | E2E con generador en fallo | pasa | `generationFailureShowsNoticeAndKeepsManualOptions` |
| RF-23 | manual: permiso denegado | pasa | manual: `pm revoke CAMERA` → diálogo → «Don't allow» → aviso «Permiso de cámara denegado…» con el formulario utilizable; + `takePhotoWithDeniedPermissionShowsNotice` |
| RF-24 | E2E marcar favorito | pasa | `favoriteToggleFlipsState` |
| RF-25 | verify 1 y 2 | pasa | `savedDraftAppearsInMisRecetas` + captura (título, momento, distintivo y miniatura) |
| RF-26 | E2E momento por defecto | pasa | `newFormPreselectsLunch` |

**Los 26 RF pasan su verificación** (22 E2E + 10 unit + comprobaciones manuales con `adb`/`uiautomator`). NFR-1 cubierto con `blockingErrorsAreLocalizedInEnglish` + inyección de locale; NFR-2 (targets ≥ 48 dp) comprobado por muestreo; NFR-3 (API 30+) cubierto al ejecutar en API 37.

## Fidelidad visual (prototipo `uxdocs/chefencasa_registrar_y_editar_receta/code.html` + `DESIGN.md`)

Muestreo de píxeles sobre la captura del formulario (API 37, 420 dpi) + inventario `uiautomator`. Jerarquía y tamaños conformes; dos diferencias de rol de token, **no bloqueantes** (el prototipo está declarado «orientativo» en la Parte B y el NFR de fidelidad pide «misma jerarquía … con los tokens Warm Culinary Craft», que se cumple: todos los colores son tokens de la paleta).

| Elemento del prototipo | Atributo | Implementación | Veredicto |
|---|---|---|---|
| `bg-surface` (fondo, app bar) | color `#fbf9f5` | `#FBF9F5` (px 500,200 · 20,850) | igual |
| Botón primario `bg-primary` | color `#a83212` | `#A83212` («Generar con IA», px 150,714) | igual |
| `bg-surface-container-lowest` tarjetas (form y tarjeta de receta) | color `#ffffff` + `shadow-sm` | `Card` M3 → `surfaceContainerHighest` `#E4E2DE`, sin sombra | divergencia (no bloqueante · elección de rol) |
| Inputs `bg-surface-container-low` | color `#f5f3ef` | `#E4E2DE` (mismo valor que la tarjeta → sin contraste tarjeta/input) | divergencia (no bloqueante) |
| Chip «Almuerzo» seleccionado `bg-primary text-on-primary` + check | color | `FilterChip` M3 → `secondaryContainer` `#82F5C1` / `#00714E` | divergencia (no bloqueante · elección de rol) |
| Chips no seleccionados `bg-surface-container-low` | color | `#E4E2DE` | divergencia (no bloqueante) |
| Botones primarios ≥ 52 px | altura | 137 px / 2,625 (densidad 420 dpi) = **52,2 dp** (`heightIn(min = 52.dp)`) | igual |
| Objetivos táctiles ≥ 48 px | altura | 126 px / 2,625 = **48,0 dp** (`heightIn(min = 48.dp)`) | igual |
| Foto arriba · metadatos · ingredientes · pasos · «Guardar Borrador»/«Publicar Receta» | estructura | Secciones y orden equivalentes; `Galería`/`Cámara` son extensión declarada (RF-19) | igual |

Sin divergencias materiales bajo el NFR de fidelidad de la Parte A. Idioma es/en verificado en las dos lenguas (E2E + capturas en español).

## Alcance

`git diff` frente al estado anterior: dominio, `data/local` (Room), `data/photo`, `data/ai`, `ui/crearreceta`, tarjetas en `ui/misrecetas`, `AppNavHost`, `di/AppModule`, `AndroidManifest.xml`, `strings.xml` (es/en), `build.gradle.kts` + `libs.versions.toml`, tests (unit + E2E) y docs (`domain-model`, `risks`, `progress`). Todo dentro de «Archivos esperados» de la Parte B; los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se han tocado; sin secretos ni fotos reales en repo, logs ni evidencia. En la spec solo se marcan los checkboxes validados.

## Veredicto

**`validated`** — `init`, `lint`, `unit` y `e2e` en verde (22/22) y los 26 RF verificados en emulador API 37, incluidos los escenarios manuales de RF-19 (galería) y RF-23 (permiso de cámara denegado). B-1 y B-2 resueltos.

Nota de entorno (no afecta al veredicto): en el ciclo 1 `./init.sh` devolvió exit 1 en dos intentos por una carrera de su `pidof` inmediato con la creación del proceso; en el ciclo 2 pasó a la primera. Los scripts del harness no se tocan. `./e2e.sh` desinstala la app al terminar (`connectedAndroidTest`): reinstalar con `./gradlew installDebug` antes de cualquier comprobación manual.
