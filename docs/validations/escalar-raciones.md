# Validación independiente: `escalar-raciones`

- **Fecha:** 2026-10-23
- **Validador:** `feature-validator` (subagente independiente)
- **Spec:** `docs/specs/escalar-raciones.md`
- **Estado declarado:** `passing` en `feature_list.json` (no modificado por el validador)
- **Árbol:** cambios sin commitear sobre la rama de trabajo de la feature
- **Veredicto:** **`revise`**

La funcionalidad cumple el contrato funcional y las verificaciones se reprodujeron en un AVD real. El `revise` se debe únicamente a defectos de evidencia y de higiene de lint: las capturas versionadas son ilegibles y el conjunto de evidencia incumple el presupuesto/nombres de la spec; además, el cambio introduce una advertencia de lint nueva.

---

## 1. Alcance y entorno

- **AVD:** `Pixel_9_Pro_Validator` (perfil `pixel_9_pro`, API 37, `emulator-5556`). **Desviación declarada:** el AVD compartido `emulator-5554` estaba siendo usado por otra sesión que reinstalaba/desinstalaba `com.whatsforlunch.app` durante los pases E2E. Para no interferir ni recibir interferencias se creó una copia del mismo perfil y se validó de forma aislada. No se tocó el dispositivo físico `dispositivo físico (serial redactado)`.
- **JDK:** `C:\Users\Daniel\.jdks\corretto-17.0.20`.
- **Instalación:** `app-debug.apk` + `app-debug-androidTest.apk` instalados con `adb install -r -t`.
- **Datos de prueba:** receta «Tortilla de patatas» (borrador, base 4, `200 g Harina`, `1 uds Huevos`) sembrada a través de la UI real; borrador «Prueba» sin ingredientes para S5.
- **Sin cambios de producción ni de estado:** el validador no modificó código ni `feature_list.json`; solo escribe este informe.

---

## 2. Checks reejecutados

| Check | Comando / método | Resultado |
|---|---|---|
| Arranque | `init.ps1` vía el wrapper documentado `cmd /c ... > .harness\init.log 2>&1` | Log termina en `==> Entorno listo`; app en ejecución sobre el AVD. **Nota:** la invocación directa desde PowerShell devuelve exit 1 por `NativeCommandError` en stderr de `adb`/`monkey`, un falso fallo ya documentado en `PROGRESS.md`; el wrapper documentado no falla. |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **105/105 tests, 18 suites, 0 fallos, 0 errores, 0 omitidas** (recuento desde `app/build/test-results/testDebugUnitTest/TEST-*.xml`). `ServingsScalingTest` 7/7 y `RecipeDetailViewModelTest` 8/8. |
| Lint | `gradlew.bat :app:lintDebug` | **0 errores / 0 fatales, 49 warnings**. Hay **1 warning nuevo en un fichero cambiado** (ver L2). |
| Paridad de textos | Comparación de claves `values/strings.xml` vs `values-en/strings.xml` | **193/193** claves en ambos. |
| E2E focalizada | `am instrument -w -r -e class com.whatsforlunch.app.RecipeDetailE2ETest com.whatsforlunch.app.test/androidx.test.runner.AndroidJUnitRunner` en `emulator-5556` | **OK (12 tests)**, 0 fallos. Cubre S1-S5 y los 7 tests previos del Detalle. |
| Verificación manual | `adb` + `uiautomator dump` sobre la app real (AVD aislado) | S1, S2, S3, S4 y S5 reproducidas (detalle en §3). |
| Capturas de validación | `cmd /c "adb exec-out screencap -p > .harness\*.png"` | PNG binarios válidos (`89 50 4E 47...`), usados solo como comprobación interna y borrados al cerrar. **No se versionan**: el implementador debe regenerar las suyas. |

### Tests E2E ejecutados (12/12)

`checklistMarksRowsAndResetsWhenReopening`, `fullRecipeShowsMetricsChecklistAndSteps`, `incompleteDraftShowsEmptyStates`, `loweringServingsHalvesIngredientQuantities`, `matchBadgeReactsToPantryChanges`, `scaledQuantitiesRoundToAtMostOneDecimal`, `servingsSelectionIsNotPersistedWhenReopeningDetail`, `servingsStepperIsDisabledAtLowerBound`, `servingsStepperIsDisabledAtUpperBound`, `startCookingCtaOpensProvisionalCookModeAndBack`, `tappingCardBodyOpensDetailAndBackKeepsSearch`, `unknownIdShowsNotFoundWithBackAction`.

### Evidencia cara inspeccionada, no reejecutada

- **Suite E2E completa:** `feature_list.json` declara 125 pruebas, 0 fallos/errores y 1 omitida. No se reejecutó completa (coste alto y AVD compartido); se reejecutó la clase focalizada 12/12, que es la que cubre esta feature, y no se observó regresión en los 7 tests previos.
- **Capturas versionadas:** inspeccionadas byte a byte (ver M1). No se pudieron abrir como imagen.

---

## 3. Verificación funcional (S1-S5, D1-D6)

### S1 — Escalado a la mitad (verify 1) — **PASA**
Base 4 → 2 raciones:
- `200 g Harina` → **`100 g Harina`**
- `1 uds Huevos` → **`1 uds Huevos`** (la cantidad base ya era 1; la reducción de 2 a 1 se comprobó con la receta de base 2 huevos: `2 uds` → `1 uds`)
- Stepper **2**, badge **0%** sin cambios, contador **`0 of 2`**, métrica **`Base servings 4`** intacta.

### S2 — Decimales (RD03 / `Q-escalar-raciones-1`) — **PASA**
Base 4 → 3 raciones: `1 uds Huevos` → **`0.8 uds Huevos`** (locale inglés; en español el formateador muestra `0,8`), `200 g Harina` → `150 g Harina`. El redondeo a 1 decimal y el formato D7 son correctos.

### S3 — Límites 1-12 (verify 2) — **PASA**
- En **1**: el nodo clickable de «Decrease servings» queda `enabled="false"`; un tap adicional no baja de 1.
- En **12**: el nodo clickable de «Increase servings» queda `enabled="false"`; un tap adicional no sube de 12.
- En 12 las cantidades escalan correctamente (`3 uds Huevos`, `600 g Harina`).
- Unit: `setServings(0)` → 1 y `setServings(13)` → 12 cubiertos por `ServingsScalingTest`/`RecipeDetailViewModelTest` (105/105 en verde).

### S4 — Solo visualización (RF-ER4, D3/D5) — **PASA**
- Con raciones en 11 y una fila marcada: la marca se conserva (`checked=true`, contador `1 of 2`) y la cantidad se actualiza (`2.8 uds Huevos`), es decir, la identidad de fila no depende de la cantidad escalada.
- Al salir y reabrir la receta: stepper vuelve a **4**, cantidades base (`1 uds Huevos`, `200 g Harina`), checklist reiniciado (`0 of 2`, sin marcas) y badge **0%** sin cambios.
- No se observó persistencia en Room ni en preferencias.

### S5 — Receta sin ingredientes (RF-ER6) — **PASA**
Borrador sin ingredientes: se muestra el bloque «Raciones a preparar» con stepper en **4**, el botón «+» es operativo (4 → 5) y el checklist conserva **`No ingredients`**.

### Decisiones y no-goals
- D1: bloque entre métricas y checklist, con subtítulo y nota en vivo. **Cumple.**
- D2: redondeo a máximo 1 decimal solo al escalar. **Cumple.**
- D3/D4: estado en el ViewModel, sin persistencia; botones deshabilitados en los extremos. **Cumple.**
- D5: identidad de fila estable. **Cumple.**
- D6: badge, «N de M», calorías, tiempos y «Raciones base» no cambian. **Cumple.**
- Non-goals respetados: sin persistencia, sin cambios en Modo Cocina, Lista de la compra, formulario, Despensa, Room ni `RecipeValidation`.

---

## 4. Hallazgos

### M1 — Evidencia versionada ilegible (Medium)
- **Evidencia:** `docs/evidence/escalar-raciones/` contiene `detalle-raciones-base-4.png`, `detalle-raciones-2.png` y `detalle-raciones-limite-12.png`. Los tres empiezan por `FF FE EB 00 50 00 4E 00` (BOM UTF-16LE), no por la firma PNG `89 50 4E 47 0D 0A 1A 0A`; no se abren como imagen.
- **Por qué importa:** la spec exige capturas válidas para auditar visualmente S1 y S3; unas capturas corruptas rompen el handoff y la trazabilidad, aunque el comportamiento se haya verificado por otra vía. Es el mismo tipo de defecto que el `M1` de la validación de `escanear-despensa-con-ia` (sesión 64).
- **Cambio requerido:** regenerar las capturas con redirección binaria segura (`cmd /c "adb exec-out screencap -p > archivo.png"`), comprobar la firma PNG antes de commitear y sustituir los tres ficheros corruptos.
- **Verificación tras el fix:** abrir las dos capturas definitivas con un visor de imágenes; deben mostrar el Detalle en español con raciones 2 (`100 g Harina`, `1 Huevo`) y raciones 12 (`600 g Harina`, «+» deshabilitado).

### L1 — Presupuesto y nombres de evidencia incumplidos (Low)
- **Evidencia:** la spec define un presupuesto de **3 artefactos** (1 informe + 2 capturas) y nombres concretos: `escalar-raciones-4-a-2-es.png` y `escalar-raciones-limite-12-es.png`. El árbol tiene **3 PNG** (más este informe = 4 artefactos) y nombres distintos (`detalle-raciones-*.png`). Además, la spec pide capturas **en español**; por estar corruptas no se puede confirmar el locale, pero los nombres no siguen el patrón `-es`.
- **Por qué importa:** incumple el contrato de evidencia de la propia spec y dificulta la revisión.
- **Cambio requerido:** dejar **exactamente 2 PNG** con los nombres de la spec, capturados con la app en español (`cmd locale set-app-locales com.whatsforlunch.app --locales es-ES`), y eliminar los actuales.
- **Verificación tras el fix:** `Get-ChildItem docs/evidence/escalar-raciones` muestra solo los 2 PNG esperados; ambos abren y muestran textos en español.

### L2 — Warning de lint nuevo en un fichero cambiado (Low)
- **Evidencia:** `lintDebug` produce 49 warnings y uno de ellos está en un fichero modificado por la feature: `ModifierParameter` en `app/src/main/java/com/whatsforlunch/app/ui/recipedetail/RecipeDetailScreen.kt:124` («Modifier parameter should be the first optional parameter»). Lo introduce el nuevo parámetro opcional `onServingsChange: (Int) -> Unit = {}` colocado antes de `modifier: Modifier = Modifier`.
- **Por qué importa:** no rompe la build (0 errores), pero ensucia el baseline de lint y contradice las convenciones de Compose del repo.
- **Cambio requerido:** reordenar la firma para que `modifier: Modifier = Modifier` sea el primer parámetro opcional (después de los obligatorios), p. ej. `onStartCooking, modifier = Modifier, onServingsChange = {}`. Ajustar las llamadas si procede.
- **Verificación tras el fix:** `lintDebug` vuelve a 49 warnings sin `ModifierParameter` en `RecipeDetailScreen.kt` (o baja a 48 si el warning desaparece), 0 errores, y `testDebugUnitTest` sigue en 105/105.

### Nota (no bloqueante) — Cifras de la suite completa no reejecutadas
`feature_list.json` declara la suite completa 125/0/0/1 y el pase en `Pixel_10_Pro`; el validador no la reejecutó (coste alto y AVD compartido) y se apoya en la clase focalizada 12/12 reejecutada. No es un hallazgo por sí mismo, pero la evidencia final debe dejar claro qué se reejecutó y qué se inspeccionó.

---

## 5. Seguridad

Revisión acotada a la feature y al diff:
- Sin secretos, tokens, PII ni credenciales en código, tests, docs ni capturas.
- Sin red, permisos nuevos, dependencias nuevas ni cambios en `AndroidManifest.xml`.
- Sin cambios en el esquema Room ni en el modelo persistido; el escalado es estado de UI y no escribe en base de datos.
- Sin entradas de usuario nuevas que puedan inyectarse en SQL o logs; los datos vienen de la receta ya validada.
- **Resultado:** sin hallazgos de seguridad. No se ejecutaron llamadas de pago a la API de IA.

---

## 6. Documentación y harness

- `docs/domain-model.md`: RD03 incorpora el redondeo a máximo 1 decimal. **Correcto.**
- `docs/risks-and-open-questions.md`: `Q-escalar-raciones-1` registrada y resuelta. **Correcto.**
- `PROGRESS.md` y `docs/progress-history.md`: entrada de implementación y estado. **Coherentes** con el diff y con la verificación realizada.
- `feature_list.json`: estado `passing`, evidencia con cifras y capturas. **Correcto en estado**, pero la evidencia de capturas deberá corregirse tras el repair de M1/L1.
- `AGENTS.md`, `ARCHITECTURE.md`, `CONSTRAINTS.md`, `technical-discovery.md`: no requerían cambios; no se introducen reglas ni comandos nuevos. **Correcto.**
- Informe de validación: este fichero.

---

## 7. Implementation Repair Brief

Entregable al implementador; ordenado y ejecutable sin este chat.

1. **Regenerar las capturas (M1 + L1).**
   - Poner la app en español: `cmd locale set-app-locales com.whatsforlunch.app --locales es-ES`; reinstalar/abrir el Detalle.
   - Capturar exactamente dos estados:
     - `docs/evidence/escalar-raciones/escalar-raciones-4-a-2-es.png`: raciones 2, `100 g Harina`, `1 Huevo` (o `1 uds Huevos` si se usa la receta sembrada), badge y «Raciones base» visibles.
     - `docs/evidence/escalar-raciones/escalar-raciones-limite-12-es.png`: raciones 12, `600 g Harina`, «+» deshabilitado.
   - Usar redirección binaria: `cmd /c "adb exec-out screencap -p > docs\evidence\escalar-raciones\<nombre>.png"`.
   - Verificar la firma `89 50 4E 47` en ambos ficheros y abrirlos con un visor.
   - Eliminar los tres PNG actuales (`detalle-raciones-base-4.png`, `detalle-raciones-2.png`, `detalle-raciones-limite-12.png`).
2. **Corregir el warning de lint (L2).**
   - En `RecipeDetailScreen`, mover `modifier: Modifier = Modifier` antes de `onServingsChange` (primer opcional) y actualizar las llamadas con argumentos nombrados si es necesario.
3. **Actualizar la evidencia declarada.**
   - En `feature_list.json` (`evidence`) y en `PROGRESS.md`, sustituir la referencia a las capturas antiguas por los dos nombres nuevos y anotar que están en español y validadas como PNG.
   - No cambiar el estado `passing` (la aceptación la persiste el orquestador tras la revalidación).
4. **Reejecutar las comprobaciones.**
   - `gradlew.bat :app:lintDebug` → 0 errores y sin `ModifierParameter` en `RecipeDetailScreen.kt`.
   - `gradlew.bat :app:testDebugUnitTest --rerun-tasks` → 105/105.
   - `RecipeDetailE2ETest` → 12/12 (o suite completa si el entorno lo permite).
5. **Revalidar.** Solicitar de nuevo `feature-validator`; el objetivo es `accept`.

---

## 8. Recomendación de estado

Mantener `escalar-raciones` en **`passing`** hasta completar el repair y la revalidación. La funcionalidad está implementada y verificada; el bloqueo es de evidencia/higiene, no de comportamiento.

---

## 9. Revalidación (sesión 70, 2026-10-23)

- **Validador:** `feature-validator` (subagente independiente)
- **Spec:** `docs/specs/escalar-raciones.md`
- **Estado declarado:** `passing` en `feature_list.json` (no modificado por el validador)
- **Árbol:** cambios sin commitear sobre la rama de trabajo de la feature
- **AVD:** `Pixel_9_Pro_Validator` (perfil `pixel_9_pro`, API 37, `emulator-5556`). No se tocó el dispositivo físico `dispositivo físico (serial redactado)` ni el AVD `emulator-5554` de otra sesión.
- **Veredicto:** **`accept`**

La revalidación confirma que el repair de la sesión 69 resolvió M1, L1 y L2 sin cambios funcionales adicionales. El contrato S1-S5 y las decisiones D1-D6 se reproducen en el árbol actual. Único hallazgo nuevo: una cifra de lint desactualizada en `evidence` (L3, Low), que no afecta al comportamiento ni a los gates y debe corregirse al persistir la aceptación.

### 9.1 Checks reejecutados

| Check | Comando / método | Resultado |
|---|---|---|
| Arranque | `init.ps1` vía wrapper documentado, log en `.harness/init-revalidation.log` | **Exit 0**; log termina en `==> Entorno listo`; app en ejecución en el AVD. |
| Unit | `gradlew.bat :app:testDebugUnitTest --rerun-tasks` | **105/105 tests, 18 suites, 0 fallos, 0 errores, 0 omitidas** (recuento desde `app/build/test-results/testDebugUnitTest/TEST-*.xml`). |
| Lint | `gradlew.bat :app:lintDebug --rerun-tasks` | **0 errores / 0 fatales, 48 warnings**. Sin `ModifierParameter` en `RecipeDetailScreen.kt` (los dos restantes están en `MyRecipesScreen.kt:155` y `ScannerScreen.kt:142`, ficheros no tocados por la feature). |
| E2E focalizada | `am instrument -w -r -e class com.whatsforlunch.app.RecipeDetailE2ETest ...AndroidJUnitRunner` en `emulator-5556` | **OK (12 tests), 0 fallos**. Reinstalados `app-debug.apk` y `app-debug-androidTest.apk` desde el árbol actual antes del pase. |
| Verificación manual | `adb` + `uiautomator dump` sobre la app real en español (`cmd locale set-app-locales com.whatsforlunch.app --locales es-ES`) | S1-S5 reproducidas de forma independiente (detalle en §9.3). |
| Capturas versionadas | Inspección de ficheros + apertura como imagen | Exactamente **2 PNG**; ambos abren y son coherentes (detalle en §9.2). |

**Inspeccionado, no reejecutado:** la suite E2E completa sigue declarada en `feature_list.json` como 125 pruebas, 0 fallos, 0 errores y 1 omitida (`SettingsE2ETest` por `Assume` sin key). No se reejecutó completa; se reejecutó la clase focalizada que cubre esta feature, que es el mínimo solicitado.

### 9.2 Verificación de M1/L1/L2

- **M1 resuelto.** `docs/evidence/escalar-raciones/` contiene exactamente dos ficheros: `escalar-raciones-4-a-2-es.png` (169 192 bytes) y `escalar-raciones-limite-12-es.png` (169 771 bytes). Ambos empiezan por `89 50 4E 47 0D 0A 1A 0A` y se abren correctamente como imagen. La captura de 2 raciones muestra la UI en español, `2` en el stepper, `100 g Harina`, `1 uds Huevos`, `Raciones base 4` y badge `0%`; la de 12 raciones muestra `12`, `600 g Harina`, `3 uds Huevos` y el botón `+` deshabilitado.
- **L1 resuelto.** Se eliminaron las tres capturas UTF-16LE corruptas. El presupuesto queda en **2 capturas + este informe**, con los nombres exactos de la spec y locale español.
- **L2 resuelto.** `modifier: Modifier = Modifier` es el primer parámetro opcional de `RecipeDetailScreen` (antes de `onServingsChange`). `lintDebug` ya no reporta `ModifierParameter` en ese fichero; el total baja de 49 a **48 warnings**, 0 errores.

### 9.3 Verificación funcional independiente (S1-S5)

Datos sembrados a través de la UI real: receta «Tortilla de patatas» (borrador, base 4, `2 uds Huevos` y `200 g Harina`) y borrador «Vacio» sin ingredientes. Todo en español.

- **S1 — Escalado a la mitad (verify 1): PASA.** Base 4 → 2: `2 uds Huevos` → **`1 uds Huevos`**, `200 g Harina` → **`100 g Harina`**; stepper en 2, badge **0%**, contador **`0 de 2`** y **`Raciones base 4`** intactos.
- **S2 — Redondeo y decimales: PASA.** Con base 4 y `1 uds Huevos` / `200 g Harina`, 4 → 3 muestra **`0,8 uds Huevos`** y **`150 g Harina`** (coma decimal del locale español, máximo 1 decimal).
- **S3 — Límites 1-12 (verify 2): PASA.** En 1, el nodo clickable de «Disminuir raciones» queda `enabled="false"` y un tap extra mantiene 1. En 12, «Aumentar raciones» queda `enabled="false"` y un tap extra mantiene 12; las cantidades son **`3 uds Huevos`** y **`600 g Harina`**.
- **S4 — Solo visualización (RF-ER4, D3/D5): PASA.** Con una fila marcada, al pasar de 12 a 11 la marca se conserva (`checked=true`, contador **`1 de 2`**) y la cantidad se recalcula a **`2,8 uds Huevos`**. Al salir y reabrir: stepper vuelve a **4**, cantidades base (**`1 uds Huevos`**, **`200 g Harina`**), checklist reiniciado (**`0 de 2`**) y badge **0%**; no hay persistencia de la selección.
- **S5 — Receta sin ingredientes (RF-ER6): PASA.** El borrador «Vacio» (0 ingredientes) muestra «Raciones a preparar» con stepper en **4**, nota en vivo, **`0 de 0`** y **`Sin ingredientes`**; el botón `+` es operativo (4 → 5) y el bloque sigue visible sin ingredientes.

**No-goals y métricas:** badge, «N de M», calorías, tiempos y «Raciones base» permanecen sin cambios al escalar. Sin persistencia, sin cambios en Modo Cocina, Lista de la compra, formulario, Despensa, Room ni `RecipeValidation`. Sin scope creep en el diff.

### 9.4 Hallazgos

#### L3 — Cifra de warnings desactualizada en `feature_list.json` (Low)

- **Evidencia:** la entrada de repair de `feature_list.json` dice «lint 0 errores (49 warnings, sin el nuevo `ModifierParameter`)». La revalidación con `:app:lintDebug --rerun-tasks` da **0 errores / 48 warnings**, coherente con la desaparición del warning nuevo (49 → 48). El resto de la evidencia (unit 105/105, E2E 12/12, capturas y sus contenidos) es exacto.
- **Por qué importa:** la evidencia de handoff debe ser exacta; una cifra contradictoria con su propia explicación complica la auditoría, aunque no afecta a la aceptación funcional.
- **Cambio requerido:** al persistir la aceptación, corregir esa cifra a **48 warnings** en la evidencia resumida. No requiere tocar código ni reejecutar gates.
- **Verificación tras el fix:** `feature_list.json` muestra 48 warnings y el informe de lint confirma 0 errores / 48 warnings.

No hay hallazgos Critical, High ni Medium. M1, L1 y L2 quedan cerrados.

### 9.5 Seguridad y documentación

- **Seguridad:** sin secretos, tokens, PII ni credenciales en el diff, tests, docs o capturas. Sin permisos, dependencias, red, esquema Room ni llamadas a la API de IA nuevos. Sin hallazgos.
- **Documentación:** `docs/domain-model.md` (RD03 + redondeo), `docs/risks-and-open-questions.md` (`Q-escalar-raciones-1`), `PROGRESS.md`, `docs/progress-history.md` y la spec son coherentes con el comportamiento verificado. Único ajuste pendiente: L3 en `evidence`.
- **Estado:** se recomienda persistir **`accepted`** para `escalar-raciones` y añadir a `evidence` la revalidación de esta sesión con los recuentos exactos (unit 105/105, lint 0/48, E2E focalizada 12/12, S1-S5 manuales) y la corrección de L3. El validador no modificó `feature_list.json`.

### 9.6 Limpieza

Se eliminaron los XML de `uiautomator`, capturas temporales y logs de gates/instalación/E2E generados por la validación; en `.harness/` solo quedan los logs de arranque (`init-validator.log`, `init-revalidation.log`) y el log del emulador. Se apagó únicamente `emulator-5556` (AVD del validador); `emulator-5554` y el dispositivo físico no se tocaron. Sin commits ni cambios de producción.