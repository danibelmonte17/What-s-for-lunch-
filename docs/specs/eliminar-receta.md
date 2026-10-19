---
feature: eliminar-receta
category: funcional
depends_on: [crear-receta]
status: ready            # draft | needs-clarification | ready (lo escribe la auditoría)
source: feature_list.json
---

# Spec: Eliminar receta

# PARTE A · CONTRATO

> QUÉ y POR QUÉ. Sin stack, arquitectura, nombres de archivos, esquemas, algoritmos ni firmas.

## Contexto y objetivo

Mis Recetas ya permite crear y editar Recetas, pero no retirar las que el usuario ya no quiere en su recetario. Como el borrado es irreversible (RD04: sin papelera ni deshacer), la acción exige una confirmación que muestre qué Receta se va a eliminar, para que el usuario no la pierda por un toque accidental ni dude de cuál borra.

## Actores y permisos

- Usuario (único rol del MVP, propietario del dispositivo; `P7`, `P31`): puede crear, editar y eliminar sus Recetas (`docs/domain-model.md`). No hay otros roles ni permisos.

## Historia

Cuando quiero retirar de mi recetario una Receta que ya no voy a cocinar,
quiero confirmar el borrado viendo el nombre de la Receta,
para poder eliminarla sin riesgo de perderla por error.

## Requisitos funcionales

> Un patrón EARS por requisito, una sola frase, verificable. Incluye aquí los casos límite y errores.

- RF-1: CUANDO el usuario pulsa la acción «Eliminar receta» del pie de la tarjeta de una Receta en Mis Recetas, EL SISTEMA mostrará un diálogo de confirmación que incluye el nombre de esa Receta.
- RF-2: CUANDO el usuario confirma el borrado en el diálogo, EL SISTEMA eliminará la Receta y dejará de mostrarla en Mis Recetas.
- RF-3: EL SISTEMA eliminará las Recetas de forma definitiva, sin papelera ni deshacer (RD04).
- RF-4: CUANDO el usuario pulsa «Cancelar» en el diálogo de confirmación, EL SISTEMA cerrará el diálogo y conservará la Receta en Mis Recetas.
- RF-5: SI el usuario descarta el diálogo sin confirmar (atrás o toque fuera), ENTONCES EL SISTEMA lo tratará como cancelación.
- RF-6: SI el borrado falla en el almacenamiento, ENTONCES EL SISTEMA mantendrá la Receta en Mis Recetas e informará del error.
- RF-7: SI la Receta ya no existe cuando se confirma el borrado, ENTONCES EL SISTEMA cerrará el diálogo sin error ni aviso.
- RF-8: CUANDO se borra una Receta con foto, EL SISTEMA borrará también su fichero de foto del dispositivo.

## Requisitos no funcionales

- NFR-1: EL SISTEMA mostrará todos los textos de este flujo en español e inglés según el locale del dispositivo.
- NFR-2: EL SISTEMA dará a la acción «Eliminar receta» la misma altura visual y tamaño de icono que «Editar» (36 dp / 16 dp), un área táctil de al menos 48 dp y el nombre accesible «Eliminar receta».

## Casos límite

- Doble confirmación del borrado (la Receta ya no existe) → cubierto por RF-7.
- Descartar el diálogo con atrás o toque fuera → cubierto por RF-5.
- Fallo del almacenamiento al borrar → cubierto por RF-6.
- Receta en Borrador o Publicada → mismo borrado en cualquier estado (ciclo de vida `docs/domain-model.md`); cubierto por RF-2 y RF-3.
- Borrar la última Receta de la lista → Mis Recetas pasa a su estado vacío; cubierto por RF-2.
- Receta con foto asociada → se borra también el fichero; cubierto por RF-8 (Q-eliminar-receta-3).

## Fuera de alcance

> Incluye lo que queda condicionado a una duda abierta, no solo lo que se descarta por completo.

- Papelera, deshacer y confirmación diferida: RD04 los prohíbe; esta feature no los implementa.
- Borrado desde el Detalle de Receta (`ver-detalle-de-receta`, aún sin implementar).
- Borrado múltiple o por selección de varias Recetas.
- Snackbar o aviso con «Deshacer» tras borrar.
- «Cocinar Ahora» y el favorito del pie de la tarjeta (otras features; Q-editar-receta-4).
- Copy del diálogo distinto del aprobado en Q-eliminar-receta-1.
- Estilo de la acción «Eliminar» distinto del aprobado en Q-eliminar-receta-4.
- Trato del descarte del diálogo distinto de cancelar (aprobado en Q-eliminar-receta-2).
- Retención del fichero de foto tras el borrado (aprobado en Q-eliminar-receta-3: se borra).

## Criterios de finalización

- `lint`, `unit` y `e2e` en verde con los tests de esta feature añadidos y sin regresiones.
- Los 8 RF y los 2 NFR pasan su verificación (2 E2E de `verify` + escenarios propios) en el emulador.
- Evidencia capturada (≤4 artefactos) en `docs/evidence/eliminar-receta/`.

## Dudas abiertas

> Cada duda de aquí se copia a `docs/risks-and-open-questions.md` como fila `Q-<feature-id>-<n>` (ver «Registro de aclaraciones» en la skill).

- Q-eliminar-receta-1 — resuelta (2026-10-19): ¿copy exacto del diálogo? Confirmado: título «Eliminar receta», mensaje «¿Eliminar «{nombre}»? Esta acción es definitiva y no se puede deshacer.», botones «Cancelar» y «Eliminar» (confirmación con tinte error).
- Q-eliminar-receta-2 — resuelta (2026-10-19): ¿el descarte (atrás o toque fuera) cuenta como cancelación? Confirmado: sí; cierra el diálogo sin borrar (RF-5).
- Q-eliminar-receta-3 — resuelta (2026-10-19): ¿se borra también el fichero de la foto asociada a la Receta? Confirmado: sí; al confirmar se borra el fichero para no dejar huérfanos (RF-8).
- Q-eliminar-receta-4 — resuelta (2026-10-19; valor actualizado por cambio de alcance del usuario y alineación posterior con «Editar»): ¿estilo de la acción «Eliminar» del pie de la tarjeta? Confirmado: `Button` de Material 3 con relleno `#A83212` (mismo color primario que «Editar») y texto blanco, icono `Icons.Outlined.Delete` de 16 dp + etiqueta visible «Eliminar», altura visual 36 dp y ancho al contenido (misma estructura compacta y misma altura/icono que «Editar»; Q-editar-receta-6), área táctil ≥48 dp, nombre accesible «Eliminar receta» y `testTag(MisRecetasTestTags.RECIPE_DELETE)`, a la derecha de «Editar» (sustituye al icono discreto tonal error de 36 dp; el tamaño grande de 18 dp/52 dp de la primera iteración quedó obsoleto al compactarse «Editar»).

## Trazabilidad

> Apunta, no describe: ≤15 palabras por celda; la aserción completa vive una sola vez en «Plan de verificación» o «Enfoque técnico».

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `steps`: «Mostrar una confirmación con el nombre…» | verify: E2E confirmar y cancelar | nombre visible en el diálogo (captura) |
| RF-2 | `description`: «El usuario borra una receta…» | verify: «E2E: eliminar… desaparece de Mis Recetas» | test E2E en verde (informe) |
| RF-3 | `steps`: «Borrar… definitiva sin papelera según RD04» | escenario propio: aserción de repositorio | informe de validación |
| RF-4 | `verify`: «E2E: cancelar… sigue en Mis Recetas» | verify: «E2E: cancelar la confirmación…» | test E2E en verde (informe) |
| RF-5 | derivado: caso límite | escenario propio: E2E descartar con atrás | test E2E en verde (informe) |
| RF-6 | derivado: caso límite | escenario propio: E2E con almacenamiento que falla | test E2E en verde (informe) |
| RF-7 | derivado: caso límite | escenario propio: unit con id inexistente | test unit en verde (informe) |
| RF-8 | derivado: caso límite | escenario propio: E2E con almacén de fotos falso | llamada de borrado en informe |
| NFR-1 | restricción: interfaz es/en (`docs/build-brief.md`) | comprobación en `en-US` | textos en-US en informe |
| NFR-2 | derivado: accesibilidad | comprobación con uiautomator | medidas y nombre accesible en informe |

## Registro de iteraciones

- Iteración 1 (2026-10-19): resueltas Q-eliminar-receta-1..4 con confirmación del usuario; se fijan copy del diálogo, descarte = cancelar, borrado del fichero de foto y estilo del botón «Eliminar», y se retiran los puntos provisionales de la Parte B.
- Iteración 2 (2026-10-19): cambio de alcance del usuario tras agotar la validación (3/3 ciclos): la acción «Eliminar» del pie pasa de icono discreto tonal error de 36 dp a botón primario relleno calcado de «Editar» (Q-eliminar-receta-4 reabierta y resuelta con el nuevo valor); se acota la comparación con el prototipo excluyendo las desviaciones aprobadas (observación 3 de la auditoría) y Q-eliminar-receta-4 remite a Q-editar-receta-6 (observación 4).
- Iteración 3 (2026-10-19): alineación con el tamaño compacto vigente de «Editar» (36 dp de altura visual y 16 dp de icono, área táctil ≥48 dp) tras la reducción de ese botón; el pie vuelve a ser homogéneo y se actualizan NFR-2, Q-eliminar-receta-4, el enfoque técnico, el plan de verificación y el E2E de tamaño.

# PARTE B · PLAN

> CÓMO. No puede contradecir la Parte A ni añadir comportamiento nuevo.

## Investigación del repositorio

### Archivos inspeccionados

- `feature_list.json` — entrada `eliminar-receta` (description, steps, verify, depends_on).
- `docs/domain-model.md` — RD04, ciclo «cualquier estado → eliminada», cascada Receta → Ingredientes/Pasos.
- `DESIGN.md` y `uxdocs/chefencasa_mis_recetas_y_crud/code.html` — pie de tarjeta («Editar», «Eliminar», «Cocinar Ahora») y confirmación de borrado.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — `CardActions`, `EditRecipeButton`, `MisRecetasTestTags`, `Scaffold`.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasRoute.kt`, `MisRecetasViewModel.kt` — wiring MVVM + StateFlow.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — contrato actual sin borrado.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDao.kt`, `IngredientEntity.kt`, `StepEntity.kt` — `upsertRecipe` transaccional; FKs `ON DELETE CASCADE`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — implementaciones existentes.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoStore.kt` — `delete(path)` ya existe.
- `app/src/androidTest/java/com/whatsforlunch/app/EditarRecetaE2ETest.kt`, `MisRecetasLocalizationE2ETest.kt` — patrones de repositorio en memoria, fakes de foto y locale inyectado.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — convención de claves por pantalla.

### Patrones existentes a seguir

- MVVM + StateFlow con reglas fuera de la UI e inyección Koin (Q-arrancar-app-en-mis-recetas-1).
- testTags centralizados en `MisRecetasTestTags`; nombre accesible vía `semantics { contentDescription }` (como `EditRecipeButton`).
- E2E sobre `InMemoryRecipeRepository` sembrado con `runBlocking`, fakes de `PhotoStore`/`PhotoAcquirer` y locale inyectado con `createConfigurationContext` + `CompositionLocalProvider(LocalContext …)`; las aserciones leen del recurso (patrón del fix B-1 de `crear-receta`).
- Cascada Room: `ingredients` y `steps` declaran FK a `recipes` con `ON DELETE CASCADE`; el borrado de la fila `recipes` arrastra sus filas.
- Cadenas con prefijo de pantalla (`mis_recetas_*`, `crear_receta_*`) duplicadas en `values/` y `values-en/`.
- Identificadores de código en inglés; textos visibles en español/inglés por recursos.

### Carencias actuales

- `RecipeRepository` no tiene operación de borrado (hay que añadirla en el contrato y en las dos implementaciones).
- `MisRecetasScreen` no tiene infraestructura de avisos (snackbar/evento de error) para RF-6.
- El prototipo no cubre la pantalla de confirmación de borrado (usa `confirm()` nativo; ver `DESIGN.md` «qué no cubre»); el copy está fijado en Q-eliminar-receta-1.
- Sin `kotlinx-coroutines-test`: los unit se apoyan en `runBlocking` y repositorios en memoria.

## Enfoque técnico

> Estrategia suficiente para que otro agente ejecute sin replanificar. Marca como provisional lo que dependa de una duda abierta y di de qué duda depende.

1. **Dominio**: añadir `suspend fun deleteRecipe(id: String)` a `RecipeRepository`. El borrado es físico (sin estado de papelera, RD04): la Receta desaparece del flujo `observeRecipes()` y no puede recuperarse.
2. **Datos (Room)**: nueva consulta `DELETE FROM recipes WHERE id = :id` en `RecipeDao`; `RoomRecipeRepository.deleteRecipe` la invoca —`ingredients` y `steps` caen por `ON DELETE CASCADE`— y borra también el fichero de la foto vía `PhotoStore.delete(path)` si la Receta tiene foto (Q-eliminar-receta-3). `InMemoryRecipeRepository.deleteRecipe` filtra su `MutableStateFlow` (id inexistente = no-op, RF-7).
3. **ViewModel**: `MisRecetasViewModel` expone `deleteRecipe(id)` (corrutina sobre `viewModelScope`) y un evento de un solo uso para RF-6 (p. ej. contador/flag `deleteError` en `MisRecetasUiState` que la UI consume y limpia); la forma concreta del evento queda a criterio del implementador mientras sea un solo uso.
4. **UI (Mis Recetas)**: `CardActions` añade `DeleteRecipeButton` a la derecha de `EditRecipeButton`, con la misma estructura compacta que «Editar» (Q-eliminar-receta-4, misma excepción de diseño que Q-editar-receta-6): `Button` de Material 3 con relleno `#A83212` (color primario) y texto blanco, icono `Icons.Outlined.Delete` de 16 dp + etiqueta visible «Eliminar», altura visual 36 dp y ancho al contenido (M3 `minimumInteractiveComponentSize` mantiene el área táctil ≥48 dp por NFR-2), nombre accesible «Eliminar receta» y `testTag(MisRecetasTestTags.RECIPE_DELETE)`. Al pulsarlo se abre un `AlertDialog` de Material 3 con el nombre de la Receta en el mensaje y el copy de Q-eliminar-receta-1 (confirmación con tinte `error`), con testTags `mis_recetas_delete_dialog`, `mis_recetas_delete_confirm` y `mis_recetas_delete_cancel`. «Cancelar» y cualquier descarte (atrás/toque fuera; Q-eliminar-receta-2) cierran sin borrar (RF-4, RF-5); confirmar llama a `deleteRecipe` y cierra (RF-2). Si el borrado falla, la lista queda intacta y la pantalla muestra un snackbar de error consumido una sola vez (RF-6).
5. **Recursos**: cadenas nuevas con prefijo `mis_recetas_delete_*` (acción, título y mensaje del diálogo, botones, aviso de error) en `values/strings.xml` y `values-en/strings.xml` (traducción `en` con el mismo sentido; NFR-1).
6. **Tests**: unit en `app/src/test` sobre `InMemoryRecipeRepository` (borrado definitivo del flujo, RF-2/RF-3; id inexistente = no-op, RF-7). E2E nuevo `EliminarRecetaE2ETest` con repositorio en memoria sembrado y `MisRecetasScreen` (patrón de `EditarRecetaE2ETest`): confirmar (desaparece, RF-1/RF-2/RF-3), cancelar (sigue, RF-4), descartar con atrás (RF-5) y almacenamiento que falla mediante un repositorio delegado que lanza en `deleteRecipe` (RF-6); el fake de `PhotoStore` registra `delete` para RF-8. NFR-1 con locale `es-ES` + comprobación `en-US`; NFR-2 con uiautomator.

## Archivos esperados

- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt` — modificar; añadir `deleteRecipe(id)`.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDao.kt` — modificar; consulta de borrado de `recipes`.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomRecipeRepository.kt` — modificar; borrado físico + foto asociada (Q-eliminar-receta-3).
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryRecipeRepository.kt` — modificar; borrado en memoria.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasViewModel.kt` — modificar; acción de borrado y evento de error.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasRoute.kt` — modificar; cablear `onDeleteRecipe`.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — modificar; botón, diálogo, snackbar y testTags.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; cadenas es/en.
- `app/src/test/java/com/whatsforlunch/app/data/repository/InMemoryRecipeRepositoryDeleteTest.kt` — crear; unit de RF-2/RF-3/RF-7 (nombre en inglés a criterio del implementador).
- `app/src/androidTest/java/com/whatsforlunch/app/EliminarRecetaE2ETest.kt` — crear; E2E del flujo (sigue la convención de `CrearRecetaE2ETest`/`EditarRecetaE2ETest`).

## Impacto visual

- Fuente de diseño: `DESIGN.md` + prototipo de la pantalla `uxdocs/chefencasa_mis_recetas_y_crud/code.html` («Orientativo»; la confirmación de borrado no está cubierta por el prototipo, que usa `confirm()` nativo).
- Pantallas o estados afectados: pie de la tarjeta de Mis Recetas (nueva acción «Eliminar receta»), diálogo de confirmación (nuevo) y aviso de error de borrado.
- Fidelidad visual (NFR): desviación aprobada — el pie usa dos botones primarios idénticos en estilo y tamaño («Editar» y «Eliminar», 36 dp de altura visual y 16 dp de icono), estructura no literal del prototipo, aprobada por el usuario (Q-eliminar-receta-4, alineada con Q-editar-receta-6); conserva la posición del prototipo («Eliminar» a la derecha de «Editar»). El diálogo usa componentes M3 con los tokens «Warm Culinary Craft» y no está cubierto por el prototipo (usa `confirm()` nativo). La comparación con `screen.png`/`code.html` y `DESIGN.md` se acota a los bloques implementados y excluye las desviaciones aprobadas (pie de dos botones primarios y diálogo); se verifica con captura en emulador API 30+.
- ¿Nuevo artefacto de diseño necesario?: no — el diálogo se resuelve con el componente `AlertDialog` de M3 y los tokens existentes; el copy está fijado en Q-eliminar-receta-1.

## Impacto en documentación duradera

- `ARCHITECTURE.md`: crear/actualizar/no necesario — no existe y el patrón MVVM + Room no cambia.
- `CONSTRAINTS.md`: crear/actualizar/no necesario — no existe y no se toman restricciones nuevas.
- `AGENTS.md`: actualizar/no necesario — sin cambios de proceso ni de harness.
- Otros: ninguno — `docs/domain-model.md` ya documenta RD04 y el ciclo «cualquier estado → eliminada».

## Plan y tareas

> Solo qué + `(cubre RF-n)`; el cómo vive en «Enfoque técnico», no lo re-expliques aquí.

- [x] T1: Añadir `deleteRecipe(id)` a `RecipeRepository` e `InMemoryRecipeRepository`, con unit de borrado definitivo y no-op con id inexistente. (cubre RF-2, RF-3, RF-7)
- [x] T2: Implementar el borrado en Room (`RecipeDao` + `RoomRecipeRepository`) con cascada y borrado del fichero de foto. (cubre RF-2, RF-3, RF-8)
- [x] T3: Acción `deleteRecipe(id)` en `MisRecetasViewModel` con evento de error de un solo uso. (cubre RF-2, RF-6)
- [x] T4: Botón «Eliminar receta» en `CardActions` y diálogo de confirmación con nombre, «Cancelar»/«Eliminar» y descarte = cancelar. (cubre RF-1, RF-4, RF-5)
- [x] T5: Cadenas es/en, testTags y nombre accesible/área táctil ≥ 48 dp. (cubre NFR-1, NFR-2)
- [x] T6: `EliminarRecetaE2ETest` con los escenarios definidos en «Enfoque técnico». (cubre RF-1…RF-6, NFR-1)
- [x] T7: Ejecutar `./init.ps1`, `./lint.ps1`, `./unit.ps1` y `./e2e.ps1` en verde sin regresiones.

## Plan de verificación

- `./lint.ps1` → exit 0 sin errores nuevos.
- `./unit.ps1` → exit 0 con los tests nuevos de borrado en verde.
- `./e2e.ps1` → exit 0 con `EliminarRecetaE2ETest` en verde y sin regresiones del resto de la suite.
- E2E: añadir `EliminarRecetaE2ETest.kt` con los escenarios de «Enfoque técnico»; ejecutar con `./e2e.ps1`.
- UI *(solo el validador)*:
  - Android nativo: hasta 2 capturas en emulador API 30 o superior de los estados clave (diálogo de confirmación con el nombre; Mis Recetas tras confirmar) y comparación con `uxdocs/chefencasa_mis_recetas_y_crud/code.html` y `DESIGN.md`, acotada a los bloques implementados y excluidas las desviaciones aprobadas (pie con dos botones primarios; diálogo no cubierto por el prototipo), guardadas según «Evidencia a capturar».
- NFR-1: aserciones de textos con locale `en-US` (patrón de `MisRecetasLocalizationE2ETest`).
- NFR-2: con `adb` + uiautomator, nombre accesible «Eliminar receta», área táctil ≥ 48 dp y misma altura visual/icono que «Editar» (36 dp / 16 dp; el E2E de tamaño compara ambas alturas).
- Scripts de verificación (`init`/`lint`/`unit`/`e2e`): no se tocan.

## Evidencia a capturar

> Presupuesto: **≤4 artefactos y solo de esta feature** (1 informe + ≤2 capturas + ≤1 E2E). Al revalidar se reemplaza el set anterior.

- `docs/evidence/eliminar-receta/informe-validacion.md` — resumen de lint/unit/e2e, recorrido de los 8 RF y 2 NFR y fidelidad visual.
- UI: `docs/evidence/eliminar-receta/dialogo-confirmar-borrado.png` y `docs/evidence/eliminar-receta/mis-recetas-tras-borrar.png` (capturas del validador).
- E2E: resultado de `./e2e.ps1` con `EliminarRecetaE2ETest`, recogido en el informe.

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec.
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤4) y solo de esta feature.
- [x] UI: evidencia visual (hasta 2 capturas de estados clave) y fidelidad al prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (capturas de emulador), acotada a los bloques implementados y excluidas las desviaciones aprobadas (pie con dos botones primarios idénticos «Editar»/«Eliminar», estructura no literal del prototipo aprobada por el usuario; diálogo no cubierto por el prototipo).
- [x] Cobertura E2E añadida o actualizada cuando hay flujo observable y existe harness, o justificada su ausencia.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-19 — veredicto: APTA (intento 2 de 3)

### Hallazgos
1. [contradicción, menor] resuelto: la comparación con el prototipo queda acotada excluyendo las desviaciones aprobadas → ver §Impacto visual y §Plan de verificación/§Checklist del validador.
2. [formato/trazabilidad, menor] resuelto: Q-eliminar-receta-4 remite ahora a `Q-editar-receta-6` → ver §Dudas abiertas y fila `Q-eliminar-receta-4` del registro.

Sin hallazgos nuevos en el delta (Q-4 reescrita, Iteración 2, paso 4 «UI (Mis Recetas)», fidelidad, verificación y checklist): valores medibles (#A83212, 18 dp, ~114×52 dp, `RECIPE_DELETE`) consistentes con `DESIGN.md` y `editar-receta` RF-4/Q-editar-receta-6; sin referencias residuales al estilo antiguo fuera de contextos históricos.

### Siguiente acción
Continuar (APTA): la spec queda `ready`; sin cambio de `status` en la feature (`passing`).

## Clarificación — 2026-10-19 — cambio de alcance: alineación compacta de «Eliminar» con «Editar»

### Hallazgos
1. [cambio pedido por el usuario, resuelto] El botón «Eliminar» conservaba el estilo grande de la primera iteración (icono 18 dp, altura visual 52 dp) mientras «Editar» ya se había compactado a 16 dp y 36 dp de altura visual. El usuario pide igualarlos para que el pie de la tarjeta sea homogéneo → ver NFR-2, Q-eliminar-receta-4, §Enfoque técnico (punto 4), §Impacto visual, §Plan de verificación y el E2E `deleteActionHasAccessibleNameAndMeetsFooterSizeRequirements`.

### Siguiente acción
Implementar la alineación (icono 16 dp, altura visual 36 dp, área táctil ≥ 48 dp), ajustar el E2E de tamaño y revalidar la feature.
