# Validación independiente — `cocinar-paso-a-paso`

> Informe emitido por el validador independiente (agente `feature-validator`, sesión 96, 2026-10-28) y guardado por el orquestador sin ediciones de contenido. Veredicto: **`revise`** (solo documental, hallazgos Low F1–F3).

## 1. Cabecera

| Campo | Valor |
|---|---|
| **Feature** | `cocinar-paso-a-paso` |
| **Spec (contrato)** | `docs/specs/cocinar-paso-a-paso.md` (RF-CPP1..6, S1–S7 con verify 1 = S1 y verify 2 = S2, D1–D9, T1–T8) |
| **Fecha** | 2026-10-28 |
| **Entorno** | Windows 11, JDK Corretto 17.0.20, Gradle 8.14.3; AVD `emulator-5554` (Pixel_10_Pro, API 37); rama `feature/cocinar-paso-a-paso`, trabajo **sin commitear** |
| **Estado reclamado** | `passing` en `feature_list.json` (autoverificación; no es aceptación) |
| **Veredicto** | **`revise`** — el código, los tests y el comportamiento cumplen la spec de forma independientemente verificada; el `revise` es **solo documental** (hallazgos Low F1–F3: recuentos que no reconcilian y una contradicción de redacción en PROGRESS/historial que, según el criterio de esta validación, deben corregirse antes de que la evidencia persistida sea fiable) |

## 2. Resumen de la revisión

- **Objetivo cumplido**: la pantalla real «Modo Cocina» sustituye al destino provisional con guía paso a paso, barra «Paso X de Y», tipografía grande (`bodyLarge` = **18 sp** en `WarmTypography`, verificado en `ui/theme/Type.kt`) y drawer colapsable «Ingredientes para este paso» con los ingredientes **vinculados del Paso activo** (RD21) en el orden de la Receta y cantidades **escaladas a las Raciones heredadas del Detalle** (RD03). La CTA «Empezar Modo Cocina» queda deshabilitada con 0 Pasos (RF-CPP5). S1–S7 pasan en el emulador real (ver §3/§4).
- **Non-goals respetados**: sin temporizador funcional (solo el hint pasivo «⏱ N min», verificado en E2E y capturas), sin voz ni `SpeechRecognizer`/`TextToSpeech`, sin permisos Android nuevos (`AndroidManifest.xml` intacto), sin cambios en Room (sigue v4), `RecipeValidation` ni prompts/IA (diff sin `data/`, `assets/prompts`, `build.gradle`), sin persistencia del progreso de cocción (índice en `MutableStateFlow`, S6 verifica reinicio en Paso 1), sin stepper de Raciones en el Modo Cocina, drawer de solo lectura sin casillas ni precios.
- **Alcance**: el diff contiene exactamente los ficheros de «Expected File Changes» (con una desviación justificada, N2) más los docs durables previstos. Sin trabajo ajeno, sin refactorizaciones espurias, sin restos de artefactos temporales (`ScreenshotCaptureTest` eliminado, cero ficheros `ack`, sin referencias residuales a `ProvisionalTestTags`).

## 3. Checks re-ejecutados vs solo inspeccionados

### Re-ejecutados por el validador (tier barato + tier caro justificado)

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `.\init.ps1` | Instalación + arranque OK; exit 1 por el **falso negativo conocido** de `pidof`/`monkey` (nota N1). App confirmada en ejecución: `adb shell pidof` → 15490; `topResumedActivity = com.whatsforlunch.app/.MainActivity` |
| Lint | `.\gradlew.bat :app:lintDebug` | **0 errores / 49 warnings** (línea base intacta; 0 issues en ficheros `cookmode`/`CookMode`, verificado sobre `lint-results-debug.xml`) |
| Unit | `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **38 suites / 251 tests / 0 fallos, 0 errores, 0 omitidos** (inventario XML reconciliado; `CookModeRulesTest` 6/6 y `CookModeViewModelTest` 6/6) — coincide exactamente con lo declarado |
| Compilación | `.\gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL |
| Paridad es/en | conteo y comparación de claves (`values/` vs `values-en/`) | **305/305 claves idénticas** (301 strings + 4 plurals); keysets sin huecos; +8 sobre la baseline 297/297 |
| **Batería E2E completa** | `.\e2e.ps1` (`connectedDebugAndroidTest`) — re-ejecutada porque la evidencia era internamente inconsistente en el recuento (F1) | **125/195 ejecutados, 5 fallos, y abort del run**: `INSTRUMENTATION_ABORTED: System has crashed` (crash del `system_server` del AVD, inestabilidad conocida; 27 m 11 s). El propio AGP confirma el inventario: **«Expected 195 tests»**. Verdes entre otros: `CookModeE2ETest` **7/7**, `AiSuggestionsE2ETest` 16/16, `CreateRecipeE2ETest` 24/24, `EditRecipeE2ETest` 16/16, `PantryRecipesE2ETest` 8/8, `PincheE2ETest` 11/11, `DeleteRecipeE2ETest` 9/9 |
| **Clases afectadas por los fallos** | `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.PantryManagementE2ETest,com.whatsforlunch.app.RecipeDetailE2ETest` | **17/17 verde** (16 m 48 s). Los 5 fallos quedan confirmados como flakes del AVD y del abort: 3 × `PantryManagementE2ETest` con firmas «Failed to inject touch input»/`ComposeTimeoutException` (idénticas a las documentadas por el implementador y en sesiones 87/89/92), 1 × `RecipeDetailE2ETest#startCookingCtaOpensCookModeAndBack` con fallo de **teardown** («Activity never becomes [DESTROYED] … PAUSED» en `ActivityScenarioRule.after` — la firma de flake ya documentada para `SettingsE2ETest`; el cuerpo del test pasa) y 1 × `RecipeDetailE2ETest#unknownIdShowsNotFoundWithBackAction` con nodo de fallo **sin stack**, coincidente con el momento del crash del sistema |

### Solo inspeccionados (evidencia del implementador)

- **E2E focalizada 19/19** (`CookModeE2ETest` 7 + `RecipeDetailE2ETest` 12): respaldada por `docs/evidence/cocinar-paso-a-paso/informe.md` (nombres de tests, fecha 2026-10-28, AVD `emulator-5554`) y **además confirmada de forma independiente** por mis re-ejecuciones (7/7 y 12/12 en verde).
- **Batería del implementador («197 tests sin regresión», 4 flakes re-ejecutados «12/12»)**: inspeccionada en `informe.md` + `PROGRESS.md` + `feature_list.json`. La sustancia (sin regresiones, flakes del AVD re-ejecutados en verde) es coherente con mi re-ejecución parcial + focalizada; **las cifras no se sostienen** (F1: inventario real 195; las clases re-ejecutadas suman 10, no 12) y no se conserva el XML de la batería.
- **4 capturas** `docs/evidence/cocinar-paso-a-paso/*.png`: inspeccionadas visualmente; **PNG binarios válidos, 1280×2856** (cabecera `89 50 4E 47 0D 0A 1A 0A` verificada byte a byte) y coherentes con lo descrito: (01) Paso 1 de 2, drawer desplegado con «350 g Patatas» escalado de 700 g base, «Paso Anterior» deshabilitado; (02) último Paso con «Paso 2 de 2», barra completa, «⏱ 5 min», «4 uds Huevos» y botón «Terminar»; (03) Detalle conservando «Raciones a preparar: 2 pers.» tras «Terminar»; (04) Borrador sin Pasos con «Empezar Modo Cocina» deshabilitada.

## 4. Resultados por apartado de la spec

### RF-CPP1..RF-CPP6

| RF | Resultado | Evidencia |
|---|---|---|
| RF-CPP1 (pantalla real, ruta, barra «Paso X de Y», texto ≥ 18 px AAA) | ✅ | `AppRoutes.COOK_MODE = "cook_mode/{recipeId}?servings={servings}"`; `CookModeScreen` con `LinearProgressIndicator` + «Paso X de Y»; `bodyLarge = 18.sp` en `WarmTypography`; contraste con tokens del tema (`onSurface`/`primary`/`secondary` sobre `surface*`); capturas 01/02 |
| RF-CPP2 (drawer colapsable, vínculos del Paso en orden de la Receta, cantidad escalada, solo lectura, «Sin ingredientes») | ✅ | `CookModeRules.stepIngredientRows` (orden de `recipe.ingredients`, escalado RD03) + `StepIngredientsDrawer` colapsable (expandido por defecto, `rememberSaveable`); E2E S1 («350 g Patatas», sin «2 uds Huevos») y S4 («Sin ingredientes» sin fallback); unit `rowsFollowRecipeIngredientOrderAndScaleQuantities`, `stepWithoutLinksShowsNoRowsInsteadOfAllIngredients` |
| RF-CPP3 («Paso Anterior»/«Siguiente», «Terminar» → Detalle, progreso en memoria) | ✅ | `CookingDock` con `enabled = !isFirstStep`; `next`/`previous` con clamp `0..totalSteps-1` (unit `nextAndPreviousStayWithinTheStepRange`); E2E S2/S3/S6; rotación preservada por ViewModel (patrón, sin test de rotación — nota) |
| RF-CPP4 (herencia de Raciones, `clampServings`, sin stepper) | ✅ | Ruta con `servings` String nullable + `toIntOrNull()`; `requestedServings = servings?.let(ServingsScaling::clampServings)`; unit `cookingStartsAtFirstStepWithRowsScaledToTheInheritedServings`, `withoutServingsArgumentTheRowsUseBaseServings`, `inheritedServingsOutOfRangeAreClamped`; E2E S1 (4→2 → 350 g) y S3 (Raciones «2» conservadas); capturas 01/03 |
| RF-CPP5 (CTA deshabilitada con 0 Pasos) | ✅ | `StartCookingBar(enabled = uiState.recipe.steps.isNotEmpty(), …)`; E2E S5 (`assertIsNotEnabled` + click sin navegación) y `RecipeDetailE2ETest#incompleteDraftShowsEmptyStates` (aserción añadida); captura 04 |
| RF-CPP6 (hint pasivo «⏱ N min») | ✅ | `StepCard` con `timerMinutes?.let { … }`; E2E S2 («⏱ 5 min»); sin cuenta atrás/alarma/+1 min en el diff; capturas 01 («⏱ 10 min»)/02 |

### Escenarios de aceptación (verify 1 = S1, verify 2 = S2)

| Escenario | Resultado | Evidencia |
|---|---|---|
| S1 (verify 1) | ✅ | `CookModeE2ETest#startingCookingModeShowsFirstStepWithScaledIngredients` (7/7 verde en mi re-ejecución; «Paso 1 de 2», «350 g Patatas», ausencia de «2 uds Huevos») + captura 01 |
| S2 (verify 2) | ✅ | `CookModeE2ETest#advancingStepUpdatesProgressAndShowsFinishButton` («Paso 2 de 2», «Terminar», sin «Siguiente», drawer colapsable, hint «⏱ 5 min») + captura 02 |
| S3 | ✅ | `finishingReturnsToDetailKeepingTheChosenServings` + captura 03 |
| S4 | ✅ | `stepWithoutLinksShowsNoIngredientsMessage` («Sin ingredientes» en ambos Pasos, 0 filas, sin fallback) |
| S5 | ✅ | `detailCtaIsDisabledWithoutSteps` (+ aserción reforzada en `RecipeDetailE2ETest`) + captura 04 |
| S6 | ✅ | `leavingMidCookRestartsAtFirstStepWhenReopening` («Volver» sin confirmación; al reabrir, Paso 1) |
| S7 | ✅ | `cookModeTextsChangeWithTheLanguage` (es/en; paridad 305/305 verificada por mí) |

### Decisiones D1–D9

| Decisión | Resultado | Dónde se verifica |
|---|---|---|
| D1 drawer solo con vínculos del Paso | ✅ | `CookModeRules` (filtra por `step.ingredientIds`); S1/S4 |
| D2 «Terminar» → Detalle | ✅ | `CookingDock` (`if (isLastStep) onFinish() else onNext()`); S2/S3 |
| D3 CTA deshabilitada con 0 Pasos | ✅ | `StartCookingBar(enabled=…)`; S5 |
| D4 Raciones heredadas + «Sin ingredientes» sin fallback | ✅ | ViewModel + regla; S1/S4; unit |
| D5 drawer solo lectura | ✅ | `StepIngredientsDrawer` solo `Text` (sin casillas/contador/precios) |
| D6 hint pasivo «⏱ N min» | ✅ | `StepCard`; S2; non-goal de temporizador intacto |
| D7 progreso en memoria (rotación sí, reabrir reinicia) | ✅ | `MutableStateFlow` en ViewModel; S6; sin persistencia |
| D8 sin stepper de Raciones en Modo Cocina | ✅ | `CookModeScreen` sin stepper |
| D9 regla pura en `domain/` (P25) | ✅ | `domain/cookmode/CookModeRules.kt` sin dependencias Android; UI sin reglas (solo presentación + copy) |

### Validator Checklist de la spec

- [x] **Alcance**: sin temporizador funcional, sin voz, sin permisos, sin Room/IA/prompts — verificado sobre el diff completo.
- [x] **S1–S7 pasan** (verify 1 = S1, verify 2 = S2) y los E2E previos siguen en verde, incluida la adaptación de `ver-detalle-de-receta` (17/17 en mi re-ejecución; adaptaciones que **refuerzan** aserciones).
- [x] **D1–D9 cumplidas**.
- [~] **Evidencia**: gates ✅, E2E focalizada ✅, 4 capturas es válidas ✅ (superset de las 2 presupuestadas, N3), informe ✅ — pero con **recuentos de batería y claves incorrectos (F1/F2)**; la batería declarada no reconcilia y su XML no se conserva.
- [x] **Cobertura E2E persistente** para el cambio de comportamiento visible: `CookModeE2ETest` S1–S7 + adaptaciones sin debilitar (comparadas con HEAD vía `git diff`: `startCookingCtaOpensCookModeAndBack` gana la pantalla real + «Paso 1 de 2»; el borrador gana `assertIsNotEnabled`; `PantryRecipesE2ETest` solo ajusta la firma del callback).
- [~] **`feature_list.json` solo `status`/`evidence`** ✅ (verificado con `git diff`), `PROGRESS.md` al día ✅, docs durables ✅ (RD22 + casos límite, `DESIGN.md`, `docs/technical-discovery.md`, `Q-cocinar-paso-a-paso-1..3`) — salvo las imprecisiones de F2/F3.
- [x] **Sin comportamiento ajeno** ni trabajo de `gestionar-temporizador-de-paso`/`controlar-cocina-con-voz`.
- [x] **Seguridad**: sin secretos, sin permisos nuevos, sin red, sin datos nuevos persistidos.

## 5. Hallazgos por severidad

### Finding F1: el recuento de la batería E2E no reconcilia con el inventario y no se conserva el XML

- **Severidad**: Low (documental) — pero es corrección **previa** para que la evidencia persistida sea fiable (criterio de esta validación).
- **Evidencia**: `docs/evidence/cocinar-paso-a-paso/informe.md` (§Gates), `feature_list.json` (`evidence`) y `PROGRESS.md`/`docs/progress-history.md` declaran «**197 tests** sin regresión» y re-ejecución de «**12/12**» de 2 clases. El inventario real es **195 `@Test`** (conteo sobre `app/src/androidTest`: sin tests parametrizados ni heredados) y el propio AGP lo confirma en mi re-ejecución («Expected 195 tests»); las clases re-ejecutadas (`PantryManagementE2ETest` 5 + `SettingsE2ETest` 5) suman **10**. 197 = 195 + 2 omitidos por `Assume` contados dos veces por la consola de AGP (12 = 10 + 2) — exactamente la convención ya corregida en la sesión 89 («consola = XML + 2»). La baseline previa («188 tests», sesión 94) sí era inventario (195 − 7 = 188), por lo que las cifras mezclan convenciones. Además no se conserva el XML de la batería (el único XML en `app/build/outputs/androidTest-results` era el del test temporal `ScreenshotCaptureTest`).
- **Por qué importa**: rompe la cadena de comparación con la baseline (sugiere +9 tests cuando son +7), reincumple una convención ya corregida y deja un claim de batería sin artefacto verificable; futuras sesiones compararían cifras incompatibles.
- **Cambio requerido**: cuadrar las cifras a la convención de inventario en informe, `feature_list.json`, `PROGRESS.md` e historial: **195 tests (193 ejecutados + 2 omitidos por `Assume`)** para la batería y **10/10** para la re-ejecución de las dos clases; añadir la nota de que la consola muestra 197/12 por doble cuenta de los omitidos.
- **Pasos sugeridos**:
  1. En `docs/evidence/cocinar-paso-a-paso/informe.md` (§Gates, fila de batería) sustituir «197 tests» por «195 tests (193 ejecutados + 2 omitidos por `Assume`); la consola de AGP muestra 197 por doble cuenta de los omitidos (convención corregida en la sesión 89)» y «reejecución de ambas clases 12/12» por «10/10».
  2. Igual corrección en la entrada `evidence` de `feature_list.json`, en `PROGRESS.md` (Estado verificado) y en la entrada de la sesión 95 de `docs/progress-history.md`.
  3. (Recomendado) si se re-ejecuta la batería para regenerar el artefacto, conservar el XML en `docs/evidence/cocinar-paso-a-paso/e2e-bateria-2026-10-28.xml` (patrón de la sesión 89).
- **Verificación tras el fix**: reconciliación aritmética documental (195 = 188 baseline + 7 de `CookModeE2ETest`; coincide con «Expected 195 tests» de AGP y con el conteo de `@Test`) y grep de «197» sin resultados en los tres ficheros.

### Finding F2: reclamaciones inexactas sobre claves de strings y sobre `formatQuantity` en la evidencia

- **Severidad**: Low (documental).
- **Evidencia**: `informe.md` (§Gates) dice «8 strings `cook_mode_*` nuevos en ambos idiomas y **`cook_mode_title` completada en `values-en`**»; `PROGRESS.md` dice «8 claves nuevas + `cook_mode_title` en en»; `docs/progress-history.md` dice «**9 claves** es/en nuevas (8 + `cook_mode_title` en `values-en`)». Verificado con `git show HEAD:…strings.xml`: `cook_mode_title` **ya existía en ambos idiomas** («Modo Cocina»/«Cooking Mode»); el diff añade exactamente **8 claves** (297 → 305). Además informe e historial afirman «`formatQuantity`/`ingredientLine` del Detalle pasan a `internal`», pero solo `ingredientLine` cambió (`formatQuantity` ya era `internal` en HEAD).
- **Por qué importa**: son las cifras de inventario de claves que usarán las próximas features como baseline; el error «9»/«+ `cook_mode_title`» ya indujo a correcciones idénticas en la sesión 94 (F2).
- **Cambio requerido**: corregir a «8 claves nuevas, todas `cook_mode_*` (`cook_mode_title` ya existía en ambos idiomas)» y a «`ingredientLine` pasa a `internal` (`formatQuantity` ya lo era)» en `informe.md`, `PROGRESS.md` y `docs/progress-history.md`.
- **Pasos sugeridos**: edición textual de las tres frases citadas; no tocar código ni `feature_list.json` (su cifra «305 claves» es correcta).
- **Verificación tras el fix**: grep de «9 claves» y «completada en `values-en`» sin resultados; recuento 305/305 intacto.

### Finding F3: `PROGRESS.md` y el historial describen el drawer como `ModalNavigationDrawer`, que no existe en el código

- **Severidad**: Low (documental).
- **Evidencia**: `PROGRESS.md` (Estado verificado) y la entrada de la sesión 95 de `docs/progress-history.md` dicen «drawer … (`ModalNavigationDrawer` **expandido por defecto**)». El código implementa `StepIngredientsDrawer` como tarjeta `Surface` **colapsable** con fila-conmutador (icono `ExpandLess`/`ExpandMore`); `git grep ModalNavigationDrawer` no tiene resultados. La spec pide «drawer colapsable» (RF-CPP2) y el código la cumple; `docs/technical-discovery.md`, `DESIGN.md` y RD22 sí describen «drawer colapsable» correctamente. Mismo bloque: el paréntesis sobre `Q-cocinar-paso-a-paso-1..3` («opciones de accesibilidad, IA y temporizador diferidas») no corresponde con el contenido real de las Q (herencia de Raciones/«Sin ingredientes», drawer solo lectura, temporizador pasivo).
- **Por qué importa**: es un doc que contradice el código; llevaría a una sesión futura a buscar un `ModalNavigationDrawer` inexistente o a «arreglar» una UI que cumple la spec.
- **Cambio requerido**: sustituir `ModalNavigationDrawer` por «drawer colapsable (tarjeta con conmutador) expandido por defecto» y corregir el paréntesis de las Q en ambos ficheros.
- **Pasos sugeridos**: edición textual en `PROGRESS.md` y `docs/progress-history.md`; `docs/technical-discovery.md` ya es preciso (no tocar).
- **Verificación tras el fix**: grep de `ModalNavigationDrawer` en el repo → 0 resultados.

### Notas (sin cambio requerido)

- **N1**: `init.ps1` sale hoy con exit 1 y «la app no esta en ejecucion tras el arranque» pese a dejar la app instalada y en ejecución (falso negativo conocido de `pidof`/`monkey`, documentado en sesiones 79/80/92). La evidencia lo registra como «verde»; coexistible porque el fallo es de comprobación, no del entorno. Deuda heredada del script (fuera del alcance de esta feature).
- **N2**: «Expected File Changes» lista `RecipeDetailViewModel.kt` como *modify* y no se tocó: justificado — el estado `Detail` ya exponía `recipe.id` y `servings`, suficientes para el nuevo callback. Sin hueco funcional.
- **N3**: la spec presupuesta «2 capturas»; se entregan **4** (superset válido que cubre además S3/S5, todas 1280×2856 binarias y verificadas). Desviación benigna, ya reflejada en `feature_list.json`.
- **N4**: mi re-ejecución de la batería completa no pudo terminar por el crash del `system_server` del AVD (125/195, «System has crashed»); el «sin regresión» global se apoya en mis 125 ejecuciones parciales (120 verdes, todas las suites previas relevantes en verde) + la focalizada 17/17 verde + la evidencia del implementador. No se ha lanzado una segunda batería completa (instrucción explícita).
- **N5**: la supervivencia del Paso activo ante rotación (D7) se garantiza por el patrón ViewModel y está documentada, pero no tiene test de rotación propio (consistente con features previas).

### Implementation Repair Brief

> **Goal**: corregir los recuentos y redacciones inexactos de la evidencia/estado (F1–F3) para que la documentación persistida reconcilie con el código y los inventarios. **Solo documentación: cero cambios de código, tests o recursos.**
>
> **Do:**
> 1. `docs/evidence/cocinar-paso-a-paso/informe.md`:
>    - §Gates/batería: «197 tests» → «**195 tests (193 ejecutados + 2 omitidos por `Assume`)**; la consola de AGP muestra 197 por doble cuenta de los omitidos (convención corregida en la sesión 89)»; «reejecución … 12/12 verde» → «**10/10**».
>    - §Gates/paridad: quitar «y `cook_mode_title` completada en `values-en`» → «8 strings `cook_mode_*` nuevos en ambos idiomas (`cook_mode_title` ya existía)».
>    - §Micro-decisiones: «`formatQuantity`/`ingredientLine` … pasan a `internal`» → «`ingredientLine` pasa a `internal` (`formatQuantity` ya lo era)».
> 2. `feature_list.json` (solo el string `evidence`, sin tocar `status`): «batería 197 tests» → «batería 195 tests (2 omitidos por `Assume`)».
> 3. `PROGRESS.md` (Estado verificado): «197 tests» → «195 tests (2 omitidos)»; «12/12» → «10/10»; «8 claves `cook_mode_*` nuevas + `cook_mode_title` en en» → «8 claves `cook_mode_*` nuevas (`cook_mode_title` ya existía en ambos idiomas)»; «(`ModalNavigationDrawer` **expandido por defecto**)» → «(drawer colapsable con conmutador, **expandido por defecto**)»; paréntesis de `Q-cocinar-paso-a-paso-1..3` → «(herencia de Raciones y «Sin ingredientes», drawer solo lectura, temporizador solo como dato pasivo)».
> 4. `docs/progress-history.md` (entrada sesión 95): mismas correcciones que en 3 (incluido «9 claves es/en nuevas (8 + `cook_mode_title`)» → «8 claves es/en nuevas; `cook_mode_title` ya existía» y la frase de `formatQuantity`).
> 5. (Opcional, recomendado) si se re-ejecuta `./e2e.ps1`, conservar el XML resultante en `docs/evidence/cocinar-paso-a-paso/e2e-bateria-2026-10-28.xml`.
>
> **Do not:** tocar código, tests, `strings.xml`, `docs/specs/`, ni reescribir `PROGRESS.md` entero (solo las frases citadas); no cambiar `status` de `feature_list.json` (lo hará el orquestador al aceptar).
>
> **Run:** no requiere re-ejecutar gates (el código no cambia). Verificación documental: grep de «197», «12/12», «9 claves», «completada en `values-en`», `ModalNavigationDrawer` → 0 resultados; 195 = 188 + 7 y 305 = 297 + 8 cuadran.
>
> **Update:** `docs/evidence/cocinar-paso-a-paso/informe.md`, `feature_list.json` (solo `evidence`), `PROGRESS.md`, `docs/progress-history.md`.

## 6. Evaluación de seguridad (security checklist, acotada a la feature y al diff)

| Punto | Resultado |
|---|---|
| 1. Secretos y configuración | Sin hallazgos — diff y evidencia sin API keys/tokens/credenciales; capturas con datos de fixture; `KeystoreAiKeyStore` no tocado |
| 2. Autenticación/autorización | No aplica — app local sin identidad ni roles en esta feature |
| 3. Validación de entrada / inyección | Sin hallazgos — único input externo: argumentos de ruta (`recipeId` texto, `servings` con `toIntOrNull()` + `clampServings` 1–12); índice de Paso acotado con `coerceIn`; sin SQL dinámico (Room y DAOs intactos) |
| 4. Exposición de datos / privacidad | Sin hallazgos — solo datos locales de la Receta; sin PII en evidencia ni logs |
| 5. Servicios externos / SSRF | No aplica — sin red ni IA en la feature |
| 6. Dependencias y tooling | Sin hallazgos — `build.gradle`/lockfiles sin cambios |
| 7. Frontera servidor/cliente | No aplica |
| 8. Subidas y media | No aplica (las fotos solo se leen en el Detalle, sin cambios) |
| 9. Auditoría y abuso | No aplica |

**Resultado global: sin hallazgos** (ni bloqueantes ni notas). Sin permisos Android nuevos (`AndroidManifest.xml` sin tocar), sin datos nuevos persistidos (paso activo y expansión del drawer solo en memoria/`rememberSaveable`), sin inyección ni SQL dinámico.

## 7. Documentación durable y harness

- **`docs/domain-model.md`**: ✅ **RD22** completo (herencia de Raciones, drawer con vínculos del Paso, navegación/«Terminar», CTA ≥ 1 Paso, progreso en memoria, hint pasivo, `CookModeRules` en `domain/`) + 5 casos límite nuevos coherentes con el código.
- **`DESIGN.md`**: ✅ la fila «Modo Cocina» describe la composición real (sin temporizador funcional ni voz, referidos a sus features); tokens y targets ≥ 48/52 dp respetados en el código (verificado: `heightIn(min = 48.dp)`, `52.dp`, `bodyLarge` 18 sp).
- **`docs/technical-discovery.md`**: ✅ sección «Modo Cocina paso a paso» precisa (ruta, ViewModel, regla, UI, flujo, verificación); sin cambios de comandos ni Room.
- **`docs/risks-and-open-questions.md`**: ✅ `Q-cocinar-paso-a-paso-1..3` registradas con origen y resolución (D4/D5/D6).
- **`ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`/`CONTEXT.md`**: ✅ sin cambios, como justifica la spec (sin capas, MUST nuevos ni términos nuevos).
- **`feature_list.json`**: ✅ `git diff` confina los cambios a `status`/`evidence` de la entrada.
- **Harness / restos temporales**: ✅ sin `ScreenshotCaptureTest`, sin ficheros `ack`, sin referencias a `ProvisionalScreen`/`ProvisionalTestTags`, `init.sh`/`init.ps1` como gate no bloqueante (N1). Salvedades: las imprecisiones documentales F1–F3 (§5) que este informe exige corregir.

## 8. Veredicto final y actualización de estado recomendada

**Veredicto: `revise`.**

Justificación: el comportamiento, los tests y el código cumplen la spec — RF-CPP1..6, S1–S7 (verify 1 = S1, verify 2 = S2) y D1–D9 verificados de forma independiente (tier barato re-ejecutado y reproducido; `CookModeE2ETest` 7/7 y `RecipeDetailE2ETest` 12/12 en verde en el AVD; non-goals y seguridad sin objeciones). No hay ningún hallazgo por encima de Low. El `revise` se debe **exclusivamente** a que la evidencia persistida contiene cifras que no reconcilian con los inventarios verificables (F1: «197 tests»/«12/12» frente a 195/10, sin XML conservado, reincumbiendo la convención de recuento corregida en la sesión 89; F2: «9 claves»/«+ `cook_mode_title`» frente a 8, y `formatQuantity`) y una redacción que contradice el código (F3: `ModalNavigationDrawer`). Según el criterio de esta validación, esas correcciones son **requisito previo** para que la evidencia de la feature sea fiable como registro durable, y su corrección es previa a que el orquestador persista el `accepted`.

**Actualización de estado recomendada**:

1. `feature_list.json`: `cocinar-paso-a-paso` → **`in_progress`** (de `passing`) hasta aplicar el repair brief.
2. Aplicar el **Implementation Repair Brief** (§5) con `feature-implementer`: es **solo documental** (4 ficheros de texto: `docs/evidence/cocinar-paso-a-paso/informe.md`, `feature_list.json` solo `evidence`, `PROGRESS.md`, `docs/progress-history.md`); sin tocar código ni re-ejecutar gates.
3. Revalidación acotada: comprobar únicamente las correcciones documentales (reconciliación 195 = 188 + 7 y 305 = 297 + 8; grep de los términos erróneos a cero). Con eso, la feature está en condiciones de **`accept` → `accepted`** con la evidencia de esta sesión (tier barato reproducido, `CookModeE2ETest` 7/7, `RecipeDetailE2ETest` 12/12 y batería parcial 120/125 verdes con AVD confirmado como causa de los fallos).

---

## Revalidación tras el repair (2026-10-28)

> Informe del mismo validador independiente (agente `feature-validator`) tras aplicar el orquestador el Implementation Repair Brief §5. Alcance: **solo** las correcciones documentales. Código no re-ejecutado por acuerdo (el diff es idéntico al auditado) y ningún fichero editado por el validador.

### (a) Resultado por punto

**1. F1 — recuentos de la batería: ✅ CORREGIDO**
- `docs/evidence/cocinar-paso-a-paso/informe.md` (§Gates): «**195 tests (193 ejecutados + 2 omitidos por `Assume`)** … **188 baseline + 7 de `CookModeE2ETest`**», re-ejecución de ambas clases «**10/10** verde», con la nota «la consola de AGP muestra 197/12 por doble cuenta de los omitidos por `Assume` —convención corregida en la sesión 89—».
- `PROGRESS.md` (entrada sesión 95) y `docs/progress-history.md` (entrada sesión 95, línea 1006): misma forma completa «195 tests (193 ejecutados + 2 omitidos por `Assume`)» + «10/10» + nota de la consola 197/12.
- `feature_list.json` (`evidence`): «batería 195 tests (2 omitidos por Assume) sin regresión (4 flakes del AVD re-ejecutados 10/10 verde)». Es la forma abreviada **exacta que el brief prescribía para este campo** (punto 2); aritméticamente consistente (195 total, 2 omitidos → 193 ejecutados) y cumple el brief al pie de la letra.
- **Reconciliación**: 195 = 188 baseline + 7 `CookModeE2ETest`, expresamente escrita en informe e historial; cuadra con el conteo independiente de `@Test` (195) y con el «Expected 195 tests» de AGP. ✔

**2. F2 — claves y `internal`: ✅ CORREGIDO**
- Los cuatro documentos afirman ahora **8 claves** nuevas (`cook_mode_title` ya existía en ambos idiomas). Sin rastro de «9 claves» ni de «completada en `values-en`» como reclamación.
- `internal`: «solo `ingredientLine` pasa a `internal`, `formatQuantity` ya lo era».
- **Reconciliación**: 305 = 297 + 8 (paridad 305/305 intacta). ✔

**3. F3 — redacción del drawer: ✅ CORREGIDO**
- Las entradas de la sesión 95 dicen ahora «drawer … **colapsable con conmutador, expandido por defecto** y de solo lectura».
- Paréntesis de `Q-cocinar-paso-a-paso-1..3` corregido en ambos: «(herencia de Raciones y «Sin ingredientes», drawer solo lectura, temporizador solo como dato pasivo)» — coincide con el contenido real de las Q.
- `ModalNavigationDrawer` solo pervive en: (i) las **citas del hallazgo F3** en la entrada de la sesión 96 (legítimas por definición), y (ii) **un KDoc en `StepIngredientsDrawer.kt:18`** que explica que la implementación *evita* deliberadamente `ModalNavigationDrawer` para no introducir Material3 experimental. Criterio propio: **no es una reclamación** (describe lo que el código no usa y por qué, con exactitud), está en código cuya modificación el brief prohibía y no contradice la spec. No cuenta como fallo. ✔

**4. Greps de verificación: ✅ LIMPIOS como reclamaciones**
- «197»: solo notas explicativas de la consola AGP (197/12) y citas F1 de la sesión 96.
- «completada en `values-en`»: 0 resultados (la única «completada en» es la ajena de `ver-detalle-de-receta` «completada en `develop`», legítima).
- «12/12»: solo usos legítimos para `RecipeDetailE2ETest` (12 tests reales, verificados 12/12 en verde) y citas de la sesión 96; la reclamación errónea «reejecución de ambas clases 12/12» ya no existe (sustituida por 10/10).
- «9 claves»: solo falsos positivos por subcadena en features ajenas («79/79 claves»).
- Ninguna reclamación errónea restante. ✔

**5. Sin sorpresas de código: ✅**
- `git status` / `git diff --stat -- app/`: idénticos a lo auditado en la validación. Cero cambios de código, tests o recursos desde la auditoría: el repair fue estrictamente documental. ✔

**6. Estado: ✅**
- `feature_list.json`: `status: "in_progress"` (correcto hasta este `accept`; el paso a `accepted` es potestad del orquestador) y `evidence` de 3 ítems que referencia este informe y las cifras ya corregidas. ✔

### (b) Cierre de hallazgos

| Hallazgo | Estado |
|---|---|
| **F1** (recuentos batería 197/12 vs inventario 195/10, sin XML) | **Cerrado** — cifras a convención de inventario en los 4 ficheros + nota de la consola; reconciliación 195 = 188 + 7 explícita y verificada contra AGP. El XML (punto 5 del brief, opcional y condicionado a re-ejecutar la batería) no era exigible: no hubo re-ejecución en el repair. |
| **F2** («9 claves»/«+ `cook_mode_title`»/`formatQuantity`) | **Cerrado** — 8 claves con `cook_mode_title` preexistente; solo `ingredientLine` como `internal` nuevo. |
| **F3** (`ModalNavigationDrawer` + paréntesis de las Q) | **Cerrado** — sustituido por «colapsable con conmutador» y paréntesis corregido; restos solo como citas de F3 y un KDoc explicativo no reclamativo. |

### (c) Veredicto final: **`accept`**

El repair se ajusta fielmente al Implementation Repair Brief §5 (puntos 1–4 aplicados tal cual; el punto 5 era opcional y no era exigible) y **no queda ninguna reclamación errónea** en evidencia, estado ni documentación durable. Las únicas apariciones residuales de los términos corregidos son citas legítimas de los propios hallazgos y notas explicativas, sin valor de reclamación. Con ello queda satisfecha la condición que motivó el `revise`: la evidencia persistida ya reconcilia con los inventarios verificables (195 = 188 + 7; 305 = 297 + 8) y no contradice el código. Subyace —ya verificado de forma independiente en la validación original— que código, tests y comportamiento cumplen la spec (RF-CPP1..6, S1–S7 con verify 1 = S1 y verify 2 = S2, D1–D9, non-goals y seguridad sin hallazgos), con gates re-ejecutados en verde, `CookModeE2ETest` 7/7 y `RecipeDetailE2ETest` 12/12 en el AVD y los fallos de batería acreditados como flakes/crash del `system_server`. Ningún hallazgo por encima de Low.

### (d) Actualización de estado recomendada

1. El orquestador persiste `cocinar-paso-a-paso` → **`accepted`** en `feature_list.json` (solo `status`/`evidence`), con evidencia que combine la validación original y esta revalidación.
2. Actualizar `PROGRESS.md` (Estado verificado / Siguiente paso) y añadir la entrada de cierre al historial, citando esta revalidación.
3. Pendientes del usuario que quedan **fuera** de esta feature (no bloquean la aceptación): mantenimiento de `controlar-cocina-con-voz` en `feature_list.json` según D1, merge del PR #29, y deuda opcional `restored` → `items` (sesión 92, Low).
