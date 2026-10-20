---
feature: buscar-y-filtrar-recetas
category: funcional
depends_on: [crear-receta]
status: draft            # draft | needs-clarification (hay incógnitas, no detiene) | ready (lo escribe el modo auditoría con APTA)
source: feature_list.json
---

# Spec: Buscar y filtrar recetas

# PARTE A · CONTRATO

> QUÉ y POR QUÉ. Sin stack, arquitectura, nombres de archivos, esquemas, algoritmos ni firmas.

## Contexto y objetivo

Mis Recetas ya lista las Recetas creadas (`crear-receta`, `editar-receta`) ordenadas por título, pero a medida que la colección crece encontrar una concreta obliga a desplazarse por toda la lista. Esta feature añade a Mis Recetas un buscador textual sobre título e ingredientes con debounce y un filtro por momento culinario, combinables entre sí, para que el usuario localice cualquier Receta en pocos segundos. Sigue el prototipo de Mis Recetas (`uxdocs/chefencasa_mis_recetas_y_crud/code.html`).

## Actores y permisos

- **Usuario** (único rol del MVP, propietario del dispositivo): busca y filtra sus Recetas (Borradores y Publicadas). Sin cuentas ni roles adicionales (`P31`).

## Historia

Cuando tengo varias recetas guardadas en Mis Recetas,
quiero buscarlas por nombre o ingrediente y filtrarlas por momento culinario,
para encontrar rápidamente la que quiero cocinar.

## Requisitos funcionales

> Un patrón EARS por requisito, una sola frase, verificable. Incluye aquí los casos límite y errores.

**Búsqueda textual**

- RF-1: CUANDO el usuario escribe texto en el buscador de Mis Recetas, EL SISTEMA muestra solo las Recetas cuyo título o alguno de sus nombres de ingrediente contiene el texto escrito.
- RF-2: CUANDO el usuario teclea varias pulsaciones seguidas en el buscador, EL SISTEMA aplica el texto una sola vez, 300 ms después de la última pulsación.
- RF-3: SI el texto escrito difiere de un título o de un nombre de ingrediente solo en mayúsculas, tildes o espacios exteriores, ENTONCES EL SISTEMA lo considera coincidencia.
- RF-4: SI el buscador queda vacío, ENTONCES EL SISTEMA muestra todas las Recetas que cumplan el filtro de momento culinario activo.

**Filtro por momento culinario**

- RF-5: EL SISTEMA ofrece como filtro por momento culinario exactamente Desayuno, Almuerzo, Cena y Postre, además de la opción Todas.
- RF-6: EL SISTEMA usa como valores iniciales del buscador y del filtro el texto vacío y el filtro Todas.
- RF-7: CUANDO el usuario elige un momento culinario en el filtro, EL SISTEMA muestra solo las Recetas con ese momento culinario.
- RF-8: CUANDO el usuario elige un momento culinario, EL SISTEMA deselecciona el filtro previamente activo (selección única).
- RF-9: MIENTRAS el filtro Todas está seleccionado, EL SISTEMA muestra Recetas de todos los momentos culinarios.

**Combinación y resultados**

- RF-10: MIENTRAS hay texto en el buscador y un momento culinario elegido, EL SISTEMA muestra solo las Recetas que cumplen ambos criterios.
- RF-11: SI ninguna Receta coincide con el texto del buscador y el filtro activos, ENTONCES EL SISTEMA muestra el estado «sin resultados» con una acción para restablecer la búsqueda y el filtro.
- RF-12: CUANDO el usuario pulsa la acción de restablecer del estado «sin resultados», EL SISTEMA vacía el buscador y selecciona el filtro Todas.
- RF-13: SI la colección de Recetas está vacía, ENTONCES EL SISTEMA muestra el estado vacío de Mis Recetas con sus acciones y sin buscador ni filtro.
- RF-14: EL SISTEMA conserva el orden de la lista de Mis Recetas al aplicar búsqueda y filtro.
- RF-15: MIENTRAS la app sigue en memoria, EL SISTEMA conserva el texto del buscador y el filtro de momento activos al volver a Mis Recetas.

## Requisitos no funcionales

- NFR-1: EL SISTEMA muestra los textos del buscador, del filtro y del estado «sin resultados» en español e inglés vía `strings.xml` (`P19`).
- NFR-2: EL SISTEMA ofrece objetivos táctiles de al menos 48 dp en el buscador, los chips de filtro y la acción de restablecer (`DESIGN.md`).

## Casos límite

- Buscador vacío con filtro activo → solo aplica el filtro → RF-4.
- Texto sin coincidencias, con o sin filtro activo → estado «sin resultados» → RF-11.
- Restablecer desde «sin resultados» → vuelve a la lista completa → RF-12.
- Mayúsculas, tildes y espacios exteriores en texto o títulos → coinciden → RF-3.
- Ráfaga de escritura rápida → una sola actualización tras la pausa → RF-2.
- Colección de Recetas vacía → estado vacío existente, sin buscador ni filtro → RF-13.
- Borradores: la búsqueda y el filtro incluyen Recetas en estado Borrador y Publicada → RF-1, RF-7.
- Dos Recetas del mismo momento o con títulos parecidos → ambas aparecen, en el orden actual de la lista → RF-7, RF-14.
- Navegar a otra pantalla y volver a Mis Recetas dentro de la sesión → se conservan búsqueda y filtro → RF-15.
- Cerrar y reabrir la app → buscador vacío y filtro Todas → RF-6.

## Fuera de alcance

> Incluye lo que queda condicionado a una duda abierta, no solo lo que se descarta por completo.

- Filtros por tiempo y dificultad y su drawer del prototipo: esta feature solo filtra por momento culinario.
- Contador de resultados del prototipo («X recetas disponibles»).
- Ordenación por relevancia de los resultados (se conserva el orden actual, RF-14).
- Badge de Coincidencia en las tarjetas (`mostrar-badge-de-coincidencia`).
- Acciones CRUD de las tarjetas: «Editar receta» ya existe (`editar-receta`); «Eliminar» es `eliminar-receta`; «Cocinar Ahora» es de otras features.
- Búsqueda o filtros en otras pantallas (Escáner, Lista de la Compra, Pinche).
- Sugerencias, autocompletado o historial del buscador.
- Persistencia de la búsqueda y del filtro tras cerrar la app: se restablecen a los valores iniciales (RF-6; confirmado en Q-buscar-y-filtrar-recetas-5).
- Filtro de momento múltiple, comparación sin normalizar y retardo de debounce distinto de 300 ms: descartados por las respuestas confirmadas (Q-buscar-y-filtrar-recetas-1 a -3).

## Criterios de finalización

Todos los RF pasan su verificación (`Plan de verificación`) en el emulador, con `lint`, `unit` y `e2e` en verde y la evidencia capturada según «Evidencia a capturar».

## Dudas abiertas

> Cada duda de aquí se copia a `docs/risks-and-open-questions.md` como fila `Q-<feature-id>-<n>` (ver «Registro de aclaraciones» en la skill).

- [RESUELTA: ¿el filtro por momento culinario es de selección única con «Todas» por defecto (como el prototipo) o de selección múltiple?] — no bloqueante. Confirmada por el humano (2026-10-19): selección única con opción «Todas» (RF-5, RF-6, RF-8).
- [RESUELTA: ¿la comparación del buscador ignora tildes y espacios exteriores además de mayúsculas?] — no bloqueante. Confirmada por el humano (2026-10-19): comparación normalizada completa, insensible a mayúsculas y tildes y con espacios recortados, en título e ingredientes (RF-3).
- [RESUELTA: ¿qué retardo de debounce usa el buscador?] — no bloqueante. Confirmada por el humano (2026-10-19): debounce de 300 ms (RF-2).
- [RESUELTA: ¿se muestran el buscador y el filtro cuando la colección de Recetas está vacía?] — no bloqueante. Confirmada por el humano (2026-10-19): se ocultan; solo queda el estado vacío (RF-13).
- [RESUELTA: ¿la búsqueda y el filtro se conservan al navegar y volver a Mis Recetas y tras cerrar la app?] — no bloqueante. Confirmada por el humano (2026-10-19): estado en memoria, se conserva al navegar dentro de la sesión y se restablece al cerrar la app (RF-15, RF-6).

## Trazabilidad

> Apunta, no describe: ≤15 palabras por celda; la aserción completa vive una sola vez en «Plan de verificación» o «Enfoque técnico».

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `steps`: buscador sobre título e ingredientes | verify 2 (ingrediente en el buscador) | informe E2E |
| RF-2 | `steps`: buscador con debounce | unit: tiempo virtual (ver Plan de verificación) | resultado unit |
| RF-3 | derivado: caso límite (comparación) | escenario unit: normalización | resultado unit |
| RF-4 | derivado: caso límite (buscador vacío) | escenario E2E: borrar el buscador | informe E2E |
| RF-5 | `steps`: filtro por momento culinario | escenario E2E: opciones del filtro visibles | informe E2E |
| RF-6 | derivado: valor por defecto | escenario E2E: valores iniciales | informe E2E |
| RF-7 | `steps`: filtro por momento culinario | verify 1 (filtrar por Cena) | informe E2E |
| RF-8 | derivado: selección única | escenario unit: cambiar de momento | resultado unit |
| RF-9 | derivado: caso límite («Todas») | escenario E2E: «Todas» muestra todos | informe E2E |
| RF-10 | `description`: combinando búsqueda y filtro | escenario E2E combinado | informe E2E |
| RF-11 | derivado: caso límite (sin coincidencias) | escenario E2E: sin resultados | informe E2E + captura |
| RF-12 | derivado: caso límite (restablecer) | escenario E2E: pulsar restablecer | informe E2E |
| RF-13 | derivado: caso límite (colección vacía) | escenario E2E: estado vacío intacto | informe E2E |
| RF-14 | derivado: caso límite (orden) | escenario unit: orden conservado | resultado unit |
| RF-15 | duda resuelta Q-buscar-y-filtrar-recetas-5 | escenario E2E: navegar y volver | informe E2E |
| NFR-1 | restricción `P19` | escenario E2E en es/en | informe E2E |
| NFR-2 | restricción `DESIGN.md` | UI: capturas y uiautomator | capturas |

## Registro de iteraciones

- 2026-10-19: el humano resuelve las 5 dudas confirmando los valores asumidos; se añade RF-15 (retención en memoria) con sus casos límite y trazabilidad, y se retiran las marcas provisionales de la Parte B.

# PARTE B · PLAN

> CÓMO. No puede contradecir la Parte A ni añadir comportamiento nuevo.

## Investigación del repositorio

### Archivos inspeccionados

- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — pantalla, `MisRecetasTestTags` y etiquetas de momento (`momento_*`).
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasViewModel.kt` — `MisRecetasUiState` y StateFlow desde `observeRecipes()`.
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/MisRecetasState.kt` — estado Empty/WithRecipes derivado de la colección.
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeIngredient.kt`, `MealMoment.kt` — modelo, ingredientes y momentos.
- `app/src/main/java/com/whatsforlunch/app/data/local/RecipeDao.kt` — Recetas observadas ordenadas por título.
- `app/src/main/java/com/whatsforlunch/app/data/repository/InMemoryRecipeRepository.kt` y `app/src/androidTest/java/com/whatsforlunch/app/EditarRecetaE2ETest.kt` — patrón de siembra en E2E.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — dependencias de test (solo JUnit 4).
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — claves existentes es/en.
- `uxdocs/chefencasa_mis_recetas_y_crud/code.html` — prototipo: buscador, chips de momento, estado sin resultados.
- `docs/domain-model.md`, `DESIGN.md` — reglas y pautas de interfaz.

### Patrones existentes a seguir

- MVVM + StateFlow con reglas en `domain/` sin dependencias de UI (`MisRecetasState`); Koin inyecta `MisRecetasViewModel(get())`.
- `MisRecetasUiState` se expone como `StateFlow` con `stateIn(WhileSubscribed(5_000))`.
- E2E Compose con `testTag` en `MisRecetasTestTags`; siembra con `InMemoryRecipeRepository` y composables reales (`EditarRecetaE2ETest`); locale inyectado en el test (`MisRecetasLocalizationE2ETest`).
- Etiquetas de momento culinario ya localizadas (`momento_desayuno`… `momento_postre`, `MealMoment.labelRes()`).
- Comentarios en español; código e identificadores en inglés para lo nuevo (los identificadores antiguos en español los resuelve `revisar-identificadores-en-ingles`).

### Carencias actuales

> Incluye lo que falta por confirmar por una duda abierta (no solo infraestructura ausente).

- Sin `kotlinx-coroutines-test`: hace falta para el test de debounce con tiempo virtual (se añade a `testImplementation`).
- Sin `testTag` del buscador, los chips y el estado «sin resultados» (se añaden en `MisRecetasTestTags`).
- Sin provisionales pendientes: las 5 dudas de la spec quedaron confirmadas por el humano el 2026-10-19 (ver «Dudas abiertas»).

## Enfoque técnico

> Estrategia suficiente para que otro agente ejecute sin replanificar. Marca como provisional lo que dependa de una duda abierta y di de qué duda depende.

- **Regla de filtrado** en `domain/misrecetas/RecipeSearchFilter.kt` (nuevo, puro): `filterRecipes(recipes, query, moment)` con `moment: MealMoment?` (`null` = Todas). Normaliza query, títulos y nombres de ingrediente (minúsculas, sin tildes vía `java.text.Normalizer`, sin espacios exteriores; confirmado en Q-buscar-y-filtrar-recetas-2) y aplica: contiene en título o en cualquier nombre de ingrediente (RF-1, RF-3) Y momento (RF-7, RF-9, RF-10). Vacío = sin criterio de texto (RF-4). Devuelve la lista filtrada conservando el orden de entrada (RF-14); incluye Borradores y Publicadas (RF-1).
- **Estado de pantalla**: `MisRecetasState` gana `NoResults` y `misRecetasStateOf(recipes, visible)` (compatibles con la firma actual): `Empty` si la colección está vacía (RF-13), `NoResults` si hay Recetas pero ninguna visible (RF-11), `WithRecipes` en caso contrario.
- **ViewModel** (`MisRecetasViewModel`): `searchQuery: MutableStateFlow<String>` y `mealMomentFilter: MutableStateFlow<MealMoment?>`; `debounce(SEARCH_DEBOUNCE_MS = 300)` (fija RF-2) solo sobre el flujo de texto, de modo que el filtro se aplica al instante al tocar un chip. Acciones `onSearchQueryChanged`, `onMealMomentSelected` (selección única, RF-8), `onResetFilters` (RF-12). `MisRecetasUiState` gana `searchQuery`, `mealMomentFilter` y `visibleRecipes`. El estado vive en el ViewModel, conservándose al navegar dentro de la sesión (RF-15) y sin persistir tras cerrar la app.
- **UI** (`MisRecetasScreen`, ramas `Empty`/`WithRecipes` más la nueva `NoResults`): fila de buscador (icono `search` + campo, hint localizado) sobre la lista; fila de chips con «Todas» + 4 momentos (chip activo en `primary`, resto en `surface-container-lowest`, como el prototipo, orientativo `P16`); `NoResults` con icono `search_off`, mensaje y botón de restablecer. Con colección vacía se mantiene el estado vacío actual sin buscador ni filtro (RF-13). Nuevos `testTag`: `SEARCH_FIELD`, `FILTER_CHIP_ALL`, `FILTER_CHIP`, `NO_RESULTS`, `RESET_FILTERS`.
- **Textos** es/en nuevos en `strings.xml` y `values-en/strings.xml`: hint del buscador, «Todas», título y mensaje de «sin resultados», acción «Ver todas las recetas» y etiqueta accesible del filtro; se reutilizan `momento_*` existentes (NFR-1).
- **Unit** (`app/src/test`): `RecipeSearchFilterTest` (título, ingrediente, normalización, combinación AND, momento, orden, borradores) y `MisRecetasSearchDebounceTest` con `runTest` + `advanceTimeBy` (ráfaga → una única actualización 300 ms tras la última pulsación; selección única del filtro).
- **E2E** (`app/src/androidTest/.../BuscarYFiltrarRecetasE2ETest.kt`): patrón de `EditarRecetaE2ETest` (repositorio en memoria sembrado con 3 Recetas de distinto momento, una con ingrediente «Pollo») sobre el ViewModel y la pantalla reales: verify 1, verify 2, combinado, sin resultados + restablecer, borrado del buscador, «Todas» por defecto, selección única, colección vacía intacta y textos es/en.

## Archivos esperados

- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/RecipeSearchFilter.kt` — crear; filtrado y normalización.
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/MisRecetasState.kt` — modificar; estado `NoResults` y derivación con lista visible.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasViewModel.kt` — modificar; query, filtro, debounce y UiState ampliado.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — modificar; buscador, chips, estado «sin resultados» y TestTags.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; textos nuevos es/en.
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — modificar; `kotlinx-coroutines-test` en `testImplementation`.
- `app/src/test/java/com/whatsforlunch/app/domain/RecipeSearchFilterTest.kt` — crear; unit de filtrado.
- `app/src/test/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasSearchDebounceTest.kt` — crear; unit de debounce y selección única.
- `app/src/androidTest/java/com/whatsforlunch/app/BuscarYFiltrarRecetasE2ETest.kt` — crear; E2E de la feature.

## Impacto visual

- Fuente de diseño: `DESIGN.md` + prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (orientativo, `P16`).
- Pantallas o estados afectados: Mis Recetas (buscador + chips sobre la lista; nuevo estado «sin resultados»); estado vacío sin cambios.
- Fidelidad visual (NFR-2): buscador con icono, chips redondeados con activo en `primary` y estado «sin resultados» con icono `search_off` siguiendo los tokens de `DESIGN.md`; objetivos táctiles ≥ 48 dp; comparación por captura en emulador API 30+ (lista en «Evidencia a capturar»).
- ¿Nuevo artefacto de diseño necesario?: no — el prototipo cubre buscador, chips y estado sin resultados.

## Impacto en documentación duradera

- `ARCHITECTURE.md`: no necesario — no existe y la estructura de capas no cambia.
- `CONSTRAINTS.md`: no necesario — no existe.
- `AGENTS.md`: no necesario — sin cambios de estructura, comandos ni convenciones.
- Otros: `docs/domain-model.md` — no necesario; el filtrado es regla de presentación de Mis Recetas que vive en `domain/misrecetas/`, sin reglas `RD##` nuevas.

## Plan y tareas

> Solo qué + `(cubre RF-n)`; el cómo vive en «Enfoque técnico», no lo re-expliques aquí.

- [x] T1: `RecipeSearchFilter` en domain con normalización y combinación búsqueda+momento. (cubre RF-1, RF-3, RF-4, RF-7, RF-9, RF-10, RF-14)
- [x] T2: `MisRecetasState` con `NoResults` y derivación desde la lista visible. (cubre RF-11, RF-13)
- [x] T3: `MisRecetasViewModel` con query, filtro, debounce y acciones. (cubre RF-2, RF-6, RF-8, RF-12, RF-15)
- [x] T4: UI de buscador, chips y estado «sin resultados» con TestTags y textos es/en. (cubre RF-5, RF-6, RF-11, RF-12, RF-13, NFR-1, NFR-2)
- [x] T5: unit de filtrado y debounce con `kotlinx-coroutines-test`. (cubre RF-1, RF-2, RF-3, RF-4, RF-7, RF-8, RF-9, RF-10, RF-14)
- [x] T6: E2E con repositorio sembrado y regresión de las features previas. (cubre RF-1, RF-4, RF-5, RF-6, RF-7, RF-9, RF-10, RF-11, RF-12, RF-13, RF-15, NFR-1)
- [x] T7: harness `init`/`lint`/`unit`/`e2e` en verde y captura de evidencia.

## Plan de verificación

- `./init.ps1` (o `./init.sh`) → exit 0 (compila, instala y abre).
- `./lint.ps1` y `./unit.ps1` (o `.sh`) → exit 0; unit incluye `RecipeSearchFilterTest` y `MisRecetasSearchDebounceTest`.
- `./e2e.ps1` (o `./e2e.sh`) en emulador API 30+ con `BuscarYFiltrarRecetasE2ETest`:
  - verify 1: tres Recetas de distinto momento (Cena, Almuerzo, Desayuno) → filtrar por Cena → solo la de Cena.
  - verify 2: escribir «Pollo» en el buscador → solo las Recetas con un ingrediente que lo contiene.
  - Combinado: texto + momento → intersección de ambos criterios (RF-10).
  - Sin coincidencias → estado «sin resultados» y restablecer vuelve a la lista completa (RF-11, RF-12).
  - Borrar el buscador con filtro activo → solo el filtro (RF-4); valores iniciales: buscador vacío y «Todas» (RF-6); elegir un momento deselecciona el anterior (RF-8); «Todas» muestra todos (RF-9); colección vacía → estado vacío sin buscador ni filtro (RF-13); textos en es/en (NFR-1).
  - Navegar al formulario y volver a Mis Recetas → búsqueda y filtro intactos (RF-15); cerrar y reabrir la app → valores iniciales (RF-6).
- Unit: normalización (mayúsculas/tildes/espacios, RF-3), conservación de orden (RF-14) y debounce con tiempo virtual (RF-2).
- UI *(solo el validador)*: hasta 2 capturas de los estados clave de «Evidencia a capturar» en emulador API 30+, comparadas con `uxdocs/chefencasa_mis_recetas_y_crud/code.html` y `DESIGN.md`.
- Scripts de verificación (`init`/`lint`/`unit`/`e2e`): no se tocan.

## Evidencia a capturar

> Presupuesto: **≤4 artefactos y solo de esta feature** (1 informe + ≤2 capturas + ≤1 E2E). Al revalidar se reemplaza el set anterior. El informe resume lint/unit/e2e y la fidelidad. La lista de capturas y rutas vive solo aquí; «Impacto visual» y «Plan de verificación» la referencian sin copiarla.

- `docs/evidence/buscar-y-filtrar-recetas/informe-validacion.md` — resumen de `lint`/`unit`/`e2e` (con los tests de `BuscarYFiltrarRecetasE2ETest`) y de la fidelidad.
- UI: `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-filtro-cena.png` — Mis Recetas con el filtro Cena aplicado (es).
- UI: `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-sin-resultados.png` — estado «sin resultados» con su acción de restablecer (es).

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec.
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤4) y solo de esta feature.
- [x] UI: evidencia visual (hasta 2 capturas de estados clave) y fidelidad al prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (capturas de emulador).
- [x] Cobertura E2E añadida o actualizada cuando hay flujo observable y existe harness, o justificada su ausencia.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-19 — veredicto: APTA (intento 1 de 3)

### Hallazgos
1. [ambigüedad, menor] RF-1/RF-3: RF-3 enuncia la normalización para coincidencia completa («difiere solo en mayúsculas, tildes o espacios exteriores»); la coincidencia por subcadena normalizada (p. ej. «cancion» vs «La canción de la cena») queda resuelta por Q-2 (comparación normalizada completa) y §Enfoque técnico (normalizar ambos lados y aplicar «contiene») → observación; RF-1 podría enunciarlo explícitamente en una reescritura futura.
2. [caso límite, menor] Crear o eliminar una Receta con búsqueda y filtro activos no está enunciado; el comportamiento se deriva (la lista visible se recalcula sobre la colección observada) → observación.

### Siguiente acción
Continuar.
