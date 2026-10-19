# Informe de validación — `editar-receta`

- **Fecha**: 2026-10-19 · **Modo**: completa (ciclo 1) + ciclo 2 tras la retirada de «Guardar» de la cabecera.
- **Entorno real**: emulador Android `Pixel_10_Pro` (**API 37.1**, densidad 480 → 3 px/dp), locale de sistema `en-US`; app en `es-ES` por locale de aplicación (`cmd locale set-app-locales`, revertido al terminar). AVD válido (≥ API 30).
- **APK validado**: `app/build/outputs/apk/debug/app-debug.apk` (`com.whatsforlunch.app`, `installDebug` exit 0).

## 1. Arranque y calidad

| Comando | Resultado | Detalle |
|---|---|---|
| `init.ps1` | **exit 0** | compila, instala y abre la app (salida en `.harness/init-validation.log`) |
| `lint.ps1` (`gradew lintDebug`) | **exit 0** | **0 errores**, 28 warnings (ninguno nuevo de esta feature) |
| `unit.ps1` (`testDebugUnitTest`) | **exit 0** | **10 tests, 0 fallos** (`RecipeValidationTest` 8 + `MisRecetasStateTest` 2) |
| `e2e.ps1` (`connectedDebugAndroidTest`) | **exit 0** | **45 tests, 0 fallos, 0 saltados** en `Pixel_10_Pro(AVD) - 17` (3 m 34 s) |

Notas de entorno (no son fallos del producto): `init.ps1` requiere `ANDROID_HOME` en este worktree porque `local.properties` trae el `sdk.dir` escapado (`C\:\\…`) y el script lo lee sin desescapar; además, mezclar stderr (`2>&1`) al invocarlo bajo PowerShell 5.1 hace que su `ErrorActionPreference=Stop` aborte en la salida de `adb shell monkey`. Ejecutado como manda el arnés (sin fusión de flujos) devuelve exit 0.

### Regresión (misma suite instrumentada)

| Suite | Tests | Resultado |
|---|---|---|
| `EditarRecetaE2ETest` (pantalla) | 15 | 0 fallos |
| `EditarRecetaRouteE2ETest` (Route real + Koin/Room) | 1 | 0 fallos |
| `EditarRecetaAppE2ETest` (app real, `verify` 1) | 1 | 0 fallos |
| `CrearRecetaE2ETest` + `CrearRecetaAppE2ETest` (regresión `crear-receta`) | 22 | 0 fallos |
| `MisRecetasCardsE2ETest` + `MisRecetasLocalizationE2ETest` + `AppLaunchE2ETest` (regresión `afinar-fidelidad-visual-del-formulario` / arranque) | 6 | 0 fallos |

## 2. Matriz de trazabilidad (Parte A)

Recorrido manual con `adb` + `uiautomator` sobre el APK real, además de la suite.

| RF | Verificación | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | pulsar «Editar receta» abre el formulario en modo edición | **pasa** | manual: tap (168,1172) → cabecera «Editar Receta»; `EditarRecetaAppE2ETest` |
| RF-2 | precarga de todos los campos, foto, favorito, ingredientes y pasos | **pasa** | manual: «Tortilla de patatas» + «Quitar foto» + «Patata/700» + «Pochar la cebolla»; `editingPreloadsRecipeFieldsAndPublishedDock` (título, historia, momento, tiempos, dificultad, raciones, calorías, foto, filas y favorito) |
| RF-3 | cabecera «Editar Receta» + distintivo de estado | **pasa** | manual: «Editar Receta» + píldora «Publicada» (y «Editar Recipe»/«Published» en inglés) |
| RF-4 | icono `edit` 18 dp sobre recuadro 36 dp con área ≥ 48 dp | **pasa** | `uiautomator`: nodo `Editar receta` = `[96,1100][240,1244]` → **144×144 px = 48×48 dp**; tinta del icono 40×40 px centrada (glifo 18 dp) y color `#59413B` |
| RF-5 | dock de Publicada = única acción «Guardar cambios» | **pasa** | manual + `editingPreloadsRecipeFieldsAndPublishedDock` (no existen `SAVE_DRAFT` ni `PUBLISH`) |
| RF-6 | guardar cambios mantiene Publicada | **pasa** | manual: título editado → tarjeta «…/Publicada»; `saveChangesKeepsPublishedStatusAndOrigin` |
| RF-7 | «Guardar cambios» deshabilitado si no cumple requisitos | **pasa** | manual: sin título → nodo dock `enabled=false`; `publishedEditingWithoutRequirementsBlocksSavingChanges` |
| RF-8 | requisitos incumplidos enumerados junto al dock | **pasa** | manual: «No se puede publicar todavía: Falta el título» junto al dock |
| RF-9 | Borrador en edición → dock del alta | **pasa** | manual: dock «Guardar Borrador» + «Publicar Receta»; `draftEditingShowsCreationDock` |
| RF-10 | «Guardar Borrador» mantiene Borrador | **pasa** | manual: «Ensalada» → «Ensalada cesar» con píldora «Borrador»; `saveDraftFromEditingKeepsDraftStatus` |
| RF-11 | publicar desde edición deja Publicada | **pasa** | `publishFromEditingSetsPublishedStatus` |
| RF-12 | reglas de Publicar del Borrador en edición | **pasa** | manual: sin foto/ingrediente/paso → «Publicar Receta» deshabilitado + lista; `draftEditingWithoutRequirementsBlocksPublish` |
| RF-13 | en la cabecera no hay «Guardar» en ningún modo; el único guardado es el dock | **pasa** | manual: cabecera sin acción de guardado en alta y en edición (`uiautomator` no contiene «Guardar» fuera del dock); `editingHasNoHeaderSaveAction`, `publishedEditingHasNoHeaderSaveAction` |
| RF-14 | sin acción de cabecera que se salte el bloqueo del dock | **pasa** | manual: dock `enabled=false` sin nodo de guardado en la cabecera; `headerSaveIsAbsentWhilePublishedSaveIsDisabled` |
| RF-15 | cerrar sin guardar descarta cambios | **pasa** | manual: título vaciado → «Volver» → la tarjeta conserva su título anterior; `closingWithoutSavingDiscardsChanges` |
| RF-16 | conserva el origen (Usuario/Sugerencia) | **pasa** | `saveChangesKeepsPublishedStatusAndOrigin` (origen `SUGGESTION` intacto, id y estado iguales) |
| RF-17 | al guardar vuelve a Mis Recetas con la Receta actualizada | **pasa** | manual (2 veces, Publicada y Borrador) + `EditarRecetaAppE2ETest` |
| RF-18 | id inexistente → vuelve sin abrir el formulario | **pasa** | `missingRecipeReturnsWithoutOpeningForm` (Route real sobre Room vacío) |

**NFR**

| NFR | Comprobación | Resultado |
|---|---|---|
| NFR-1 | textos de edición en es/en | **pasa**: «Editar Receta»/«Edit Recipe», «Editar receta»/«Edit recipe», «Guardar cambios»/«Save changes» comprobados en el emulador en ambos idiomas; 82/82 claves en `values/` y `values-en/`; `editTextsAreLocalizedInEnglish`, `cardEditActionIsLocalizedInEnglish` |
| NFR-2 | objetivos ≥ 48 dp y contraste AA | **pasa**: icono «Editar receta» 48×48 dp y botón de dock 156 px = **52 dp** (≥ 52 dp); botón primario `#A83212` con texto blanco → contraste ≈ 6,7:1 (AA) |
| NFR-3 | API 30+ | **pasa**: ejecutado en API 37.1 (y la suite corre en el mismo AVD) |
| NFR-4 | fidelidad 1:1 por muestreo de píxeles | **pasa**: ver §3 |

## 3. Fidelidad visual 1:1 (NFR-4)

Prototipos: `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (pie de la tarjeta) y `uxdocs/chefencasa_registrar_y_editar_receta/code.html` (cabecera y dock). Muestreo sobre `mis-recetas-accion-editar.png` y `formulario-editar-receta.png` (densidad 480 → 3 px/dp).

| Elemento del prototipo | Atributo | Implementación (medido) | Veredicto |
|---|---|---|---|
| `article.recipe-card` | fondo `bg-surface-container-lowest` | `#FFFFFF` en (640,950) | igual |
| `body.bg-surface` | fondo de pantalla | `#FBF9F5` en (640,1330) | igual |
| `button[aria-label="Editar receta"]` `w-9 h-9 rounded-lg` | recuadro 36 px + área táctil ≥ 48 px (`NFR-2`) | nodo 144×144 px = **48×48 dp** (recuadro 36 dp en el código), clickable `[96,1100][240,1244]` | igual (RF-4 declara la precedencia `DESIGN.md`) |
| `span.material-symbols-outlined.text-\[18px\]` `edit` | icono `edit` 18 px centrado | icono `Icons.Outlined.Edit` a 18 dp; tinta 40×40 px centrada en el recuadro | igual |
| `text-on-surface-variant` | color del icono | `#59413B` (muestreado sobre la tinta) | igual |
| `span` píldora de estado (`bg-primary/10 text-primary rounded-full`) | fondo primary @10 % + texto primary | píldora «Publicada» `#F2E5DE` sobre `#FBF9F5` (= primary 10 % exacto) y `#F6EAE7` sobre blanco | igual |
| `div.sticky.bottom-4.bg-surface-container-lowest/95` (dock) | superficie del dock | `#FFFFFF` en (640,2640) | igual |
| dock Publicada (extensión declarada en RF-5) | acción primaria `bg-primary text-on-primary` ≥ 52 px | «Guardar cambios» `#A83212`, alto 156 px = **52 dp** | igual (extensión declarada) |
| cabecera `span.text-on-surface` («Crear Receta» en el prototipo) | título + subtítulo «Paso a paso con IA» | «Editar Receta» (título de edición declarado en RF-3) + subtítulo intacto | igual (título de edición declarado) |
| botón «Guardar» (cabecera) | primario habilitado / deshabilitado distinguible | **no se dibuja** (Q-editar-receta-5): el prototipo lo muestra, desviación declarada y aceptada por el usuario | desviación declarada |
| «Guardar cambios» deshabilitado (`RF-7`) | estado deshabilitado visible y distinguible | `#E4E2DE` (`surface-container-highest`) frente a `#A83212` habilitado | igual |

Atributos comparados: **11**. Desviaciones declaradas aceptadas: 4 (área táctil 48 dp frente a los 36 px del prototipo —RF-4/NFR-2; dock de Publicada con «Guardar cambios» —RF-5; título «Editar Receta» —RF-3, tomado del modal del prototipo de Mis Recetas; botón «Guardar» de la cabecera omitido —Q-editar-receta-5). «Eliminar receta» y «Cocinar Ahora» quedan excluidos del muestreo por «Fuera de alcance» (Q-editar-receta-4). **Divergencias abiertas: 0.**

## 4. Alcance

`git diff` frente al estado implementado: solo los archivos de «Archivos esperados» (repositorio/DAO/Room/InMemory, `CrearReceta*`, `MisRecetas*`, `AppNavHost`, `strings.xml` es/en, `EditarRecetaE2ETest.kt` nuevo) más `PROGRESS.md`, `feature_list.json` y `docs/progress-history.md`. Sin cambios en `init`/`lint`/`unit`/`e2e`; sin secretos ni fotos reales (las capturas son del emulador); la spec solo se toca marcando checkboxes. Sin comportamiento de otras features. `logcat`: sin `FATAL EXCEPTION` ni `AndroidRuntime` de la app en todo el recorrido.

## 5. Evidencia

- `docs/evidence/editar-receta/informe-validacion.md` (este informe).
- `docs/evidence/editar-receta/mis-recetas-accion-editar.png` — Mis Recetas (es) con la tarjeta Publicada y su pie «Editar receta».
- `docs/evidence/editar-receta/formulario-editar-receta.png` — formulario en modo edición (es): cabecera «Editar Receta»/«Borrador» sin «Guardar», datos precargados y dock del alta («Guardar Borrador»/«Publicar Receta»).

Total: 3 artefactos (≤ 4), solo de esta feature.

## 6. Veredicto

**`validated`** — los 18 RF y los 4 NFR pasan su verificación en el entorno real, `lint`/`unit`/`e2e` en verde (44 tests instrumentados, 0 fallos, con la regresión de `crear-receta` y `afinar-fidelidad-visual-del-formulario`), fidelidad 1:1 con las desviaciones declaradas y alcance correcto. Sin bugs.

### Ciclo 2 (2026-10-19) — retirada de «Guardar» de la cabecera

- **Cambio pedido por el usuario (Q-editar-receta-5)**: el botón de guardado de la cabecera era redundante en los tres estados (mismo handler y mismo `enabled` que el dock). Se elimina de `FormHeader` en todos los modos; el único guardado son las acciones del dock.
- **Alcance**: `CrearRecetaScreen.kt` (`FormHeader` sin `actions`), `strings.xml` es/en (se retira `action_save`, ya sin uso), `CrearRecetaE2ETest` y `EditarRecetaE2ETest` (afirman la ausencia del tag `HEADER_SAVE` y guardan desde el dock), `docs/specs/editar-receta.md` (RF-13/RF-14) y `docs/specs/afinar-fidelidad-visual-del-formulario.md` (RF-3/RF-21).
- **Harness**: `lint` exit 0 (0 errores), `unit` exit 0 (10 tests), `e2e` exit 0 (**44 tests, 0 fallos**, 0 saltados) en `Pixel_10_Pro(AVD) - 17`.
- **Manual (adb + uiautomator)**: en alta y en edición la cabecera solo muestra «Volver», título, distintivo y «Paso a paso con IA»; `uiautomator` no encuentra ningún nodo «Guardar» fuera del dock. Guardado de Borrador desde el dock verificado de extremo a extremo (tarjeta «Tortilla de patatas»/«Borrador» en Mis Recetas).
- **Evidencia actualizada**: `formulario-editar-receta.png` recapturada (cabecera sin «Guardar», dock del alta).
- **Veredicto del ciclo 2**: `validated`.
