---
feature: editar-receta
category: funcional
depends_on: [crear-receta]
status: ready
source: feature_list.json
---
# Spec: Editar una receta existente sin perder su estado

# PARTE A — CONTRATO

## Contexto y objetivo

Mis Recetas ya crea Recetas, pero una Receta guardada no se puede modificar: cualquier error obliga a borrarla y recrearla. Esta feature abre el formulario existente en modo edición, precargado con los datos de la Receta, y guarda los cambios sin perder su estado (Borrador o Publicada), según el ciclo de vida de `docs/domain-model.md`. El usuario pidió ajustarse visualmente a `uxdocs/`: el acceso es el icono «Editar receta» del pie de la tarjeta y la cabecera del formulario pasa a «Editar Receta», tal como los dibuja el prototipo.

## Actores y permisos

- **Usuario** (único rol del MVP, propietario del dispositivo): edita cualquier Receta de Mis Recetas, de origen Usuario o Sugerencia. Sin cuentas (`P31`); solo edición local, sin sincronización (`P33`).

## Historia

Cuando una Receta guardada tiene un error o quiero mejorarla,
quiero abrirla en el formulario con sus datos ya cargados y corregirla,
para poder modificarla sin recrearla ni perder su estado.

## Requisitos funcionales

- RF-1: CUANDO el usuario pulsa «Editar receta» en la tarjeta de una Receta de Mis Recetas, EL SISTEMA abre el formulario de Receta en modo edición.
- RF-2: EL SISTEMA precarga en el formulario el título, la historia, el momento culinario, los tiempos de preparación y cocción, la dificultad, las raciones base, las calorías, la foto, el favorito, los ingredientes y los pasos de la Receta editada.
- RF-3: CUANDO el formulario abre en modo edición, EL SISTEMA titula la cabecera del alta «Editar Receta» con el distintivo de estado de la Receta (Borrador/Publicada), según `uxdocs/chefencasa_registrar_y_editar_receta/code.html` (cabecera) y el modal de edición de `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (título).
- RF-4: EL SISTEMA presenta «Editar receta» como un icono `edit` de 18 dp sobre un recuadro visual de 36 dp y un área táctil ≥ 48×48 dp, en el pie de acciones de la tarjeta según `uxdocs/chefencasa_mis_recetas_y_crud/code.html`.
- RF-5: MIENTRAS se edita una Receta en estado Publicada, EL SISTEMA muestra en el dock una única acción primaria «Guardar cambios».
- RF-6: CUANDO el usuario pulsa «Guardar cambios» sobre una Receta Publicada que cumple los requisitos de Publicar, EL SISTEMA guarda los cambios manteniendo el estado Publicada.
- RF-7: MIENTRAS una Receta Publicada en edición no cumple los requisitos de Publicar (`⚠ S07`), EL SISTEMA mantiene «Guardar cambios» deshabilitado.
- RF-8: MIENTRAS una Receta Publicada en edición no cumple los requisitos de Publicar (`⚠ S07`), EL SISTEMA enumera junto al dock los requisitos incumplidos.
- RF-9: MIENTRAS se edita una Receta en estado Borrador, EL SISTEMA muestra el dock del alta («Guardar Borrador» y «Publicar Receta»).
- RF-10: CUANDO el usuario pulsa «Guardar Borrador» sobre un Borrador en edición con título, EL SISTEMA guarda los cambios manteniendo el estado Borrador.
- RF-11: CUANDO el usuario pulsa «Publicar Receta» sobre un Borrador en edición que cumple los requisitos de Publicar, EL SISTEMA guarda la Receta con estado Publicada.
- RF-12: MIENTRAS se edita un Borrador, EL SISTEMA aplica a «Publicar Receta» las mismas reglas de habilitado y de requisitos incumplidos junto al dock que en el alta (`⚠ S07`).
- RF-13: EL SISTEMA no muestra «Guardar» en la cabecera del formulario, ni en el alta ni en modo edición: el único punto de guardado es la acción del dock del estado (RF-5, RF-9).
- RF-14: MIENTRAS se edita una Receta Publicada cuyos requisitos de Publicar no se cumplen, EL SISTEMA mantiene el único guardado en el dock deshabilitado, sin ofrecer en la cabecera ninguna acción alternativa que permita saltarse el bloqueo (RF-7).
- RF-15: SI el usuario cierra el formulario en modo edición sin guardar, ENTONCES EL SISTEMA descarta los cambios (la Receta conserva sus datos anteriores).
- RF-16: EL SISTEMA conserva el origen de la Receta (Usuario/Sugerencia) al guardar la edición.
- RF-17: CUANDO se guardan cambios en modo edición, EL SISTEMA vuelve a Mis Recetas mostrando la Receta actualizada.
- RF-18: SI la Receta a editar no existe, ENTONCES EL SISTEMA vuelve a Mis Recetas sin abrir el formulario.

## Requisitos no funcionales

- NFR-1: Interfaz en español e inglés vía `strings.xml` (`P19`); los textos de edición («Editar Receta», «Editar receta», «Guardar cambios») se afirman en ambas lenguas.
- NFR-2: Objetivos táctiles ≥ 48×48 dp y contraste WCAG 2.1 AA; el botón primario del dock mide ≥ 52 dp de alto (`DESIGN.md`).
- NFR-3: Android 11 (API 30) o superior, solo móvil (`P20`); el dato vive en el dispositivo (`P33`).
- NFR-4: Los estados de edición reproducen el prototipo 1:1 (bloques, textos, iconos, controles y roles de token) por muestreo de píxeles en emulador API 30+, excluidos los controles de «Fuera de alcance»; si el prototipo contradice `DESIGN.md`, prima `DESIGN.md` en interacción y accesibilidad y el prototipo en apariencia.

## Casos límite

- Publicada en edición sin foto, sin ingredientes o sin pasos, o con filas de Ingrediente/Paso inválidas → «Guardar cambios» deshabilitado y lista junto al dock → RF-7, RF-8.
- Borrador en edición incompleto → las filas se guardan tal cual (`crear-receta` RF-13); sin título → no guarda y señala el título obligatorio junto al campo (`crear-receta` RF-14) → RF-10.
- Cabecera en edición → el subtítulo «Paso a paso con IA» se mantiene, pero «Guardar» no se dibuja en ningún modo (alta incluida) → RF-3, RF-13.
- Cambiar, generar o quitar la foto o usar un título duplicado durante la edición → mismas reglas que el alta → RF-2 (hereda `crear-receta`).
- Cerrar sin guardar → cambios descartados sin confirmación → RF-15; Receta de origen Sugerencia → edición idéntica y origen intacto → RF-2, RF-16; Receta inexistente → se vuelve sin abrir el formulario → RF-18.

## Fuera de alcance

- «Eliminar receta», «Cocinar Ahora» y el favorito del pie de la tarjeta (`eliminar-receta`, `ver-detalle-de-receta`/`cocinar-paso-a-paso`): solo se implementa «Editar receta» (Q-editar-receta-4); el favorito del formulario sí se edita.
- Vuelta de Publicada a Borrador: no existe en el MVP; la edición solo puede publicar un Borrador (RF-11).
- Confirmación al cerrar con cambios sin guardar: se asume sin confirmación, igual que el alta (RF-15).
- Buscador, filtro por momento culinario y badge de Coincidencia (`buscar-y-filtrar-recetas`, `mostrar-badge-de-coincidencia`); Detalle, escalado de Raciones y Modo Cocina; reordenado drag & drop, etiquetas y dieta, y entrada por IA del prototipo (fuera ya en `crear-receta`).

## Criterios de finalización

- Los 18 RF pasan su verificación (E2E en emulador API 30+ y comparación visual) y `lint` + `unit` están en verde; evidencia ≤ 4 artefactos, solo de esta feature, y regresión de `crear-receta` y `afinar-fidelidad-visual-del-formulario` en verde (comparten el formulario).

## Dudas abiertas

- Ninguna abierta. Q-editar-receta-1 a Q-editar-receta-5 se resolvieron con el usuario (ver `docs/risks-and-open-questions.md`): dock con «Guardar cambios» en Publicada (RF-5), «Publicar Receta» al editar un Borrador (RF-9, RF-11), guardado bloqueado si la Publicada deja de cumplir `⚠ S07` (RF-7, RF-8), solo «Editar receta» en el pie de la tarjeta (RF-4) y sin «Guardar» de cabecera en ningún modo (RF-13, RF-14). Decisión asumida sin consulta: cerrar en modo edición descarta los cambios sin confirmación, igual que el alta (RF-15).

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1, RF-2 | `steps`: precargar el formulario | verify 1 + escenario E2E de precarga | informe E2E |
| RF-3, RF-4 | uxdocs: cabecera y pie de la tarjeta | verify UI + uiautomator: cabecera e icono | captura |
| RF-5, RF-9, RF-11 | Q-editar-receta-1, Q-editar-receta-2 | escenario E2E: dock según estado y publicar desde edición | informe E2E |
| RF-6, RF-10 | `steps`: guardar manteniendo estado | verify 1 (título nuevo en Mis Recetas) | informe E2E + captura |
| RF-7, RF-8, RF-12 | Q-editar-receta-3 + `⚠ S07` | escenario E2E: deshabilitado y lista junto al dock | informe E2E |
| RF-13, RF-14 | derivado: guardado único en el dock; sin «Guardar» de cabecera en ningún modo | escenario E2E: ausencia de «Guardar» y guardado desde el dock | informe E2E |
| RF-15, RF-17 | derivado: caso límite + `verify` | verify 1 + escenario de cierre sin guardar | informe E2E |
| RF-16 | derivado: caso límite (RD07) | escenario de origen intacto | informe E2E |
| RF-18 | derivado: caso límite (id inexistente) | escenario E2E: id inexistente | informe E2E |

## Registro de iteraciones

- Retrabajo tras auditoría (A RETRABAJAR intento 1): RF-4 fija icono 18 dp, recuadro 36 dp y área táctil ≥ 48 dp con la precedencia `DESIGN.md`/prototipo en NFR-4 (hallazgo 1); RF-13/RF-14 nombran el guardado de la cabecera por estado y su estado deshabilitado (2); RF-3 cita el prototipo correcto y los casos límites fijan el subtítulo (3); NFR-4 excluye del muestreo los controles de «Fuera de alcance» (4); RF-18 cubre el id inexistente (5); el caso límite del Borrador cita `crear-receta` RF-13/RF-14 (6); RF-7/RF-8 separados y RF-15 acotado (7); verificación es/en añadida (8). Post-APTA (intento 2), corregidos de oficio a petición del usuario: Trazabilidad precisa el origen de RF-11 y de RF-15 a RF-18 y NFR-2 unifica la medida en dp (menores 9 y 10).

# PARTE B — PLAN

## Investigación del repositorio

### Archivos inspeccionados

- `ui/crearreceta/CrearRecetaScreen.kt` — `FormHeader`, `StatusBadge`, `PublishErrors` y `ActionDock` (cabecera y dock a condicionar por estado).
- `ui/crearreceta/CrearRecetaViewModel.kt` y `CrearRecetaRoute.kt` — `CrearRecetaUiState`, `onSaveDraft`/`onPublish`, `buildRecipe` (id nuevo y `origin = USER` forzados) y `saved` → `popBackStack`.
- `ui/nav/AppNavHost.kt`, `ui/misrecetas/MisRecetasScreen.kt`, `MisRecetasRoute.kt` — ruta `crear_receta` sin argumentos; `RecipeCard` sin pie de acciones.
- `domain/repository/RecipeRepository.kt`, `data/local/RecipeDao.kt`, `data/repository/RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — `saveRecipe` ya es upsert (crea y actualiza); falta lectura por id.
- `domain/crearreceta/RecipeValidation.kt` — `validateForDraft`/`validateForPublish` y `RecipeRequirement`, fuente única de requisitos.
- `res/values/strings.xml`, `values-en/strings.xml`, `androidTest/.../CrearRecetaE2ETest.kt`, `E2eDatabase.kt` — textos y patrón E2E con testTags y fakes.

### Patrones existentes a seguir

- MVVM + StateFlow (`Route` → `ViewModel` → `Screen`) con Koin; validaciones puras en `domain/`; testTags en objetos `*TestTags` y textos en `values/` y `values-en/`. El formulario ya habilita «Publicar Receta» con `validateForPublish`, muestra `publishErrors` junto al dock y `E2eDatabase.clear()` parte de Mis Recetas vacía.

### Carencias actuales

- `RecipeRepository` no lee una Receta por id (solo `observeRecipes`). La ruta `crear_receta` no admite Receta ni modo edición (el ViewModel crea id nuevo y fuerza `origin = USER`) y `RecipeCard` no tiene pie de acciones ni textos/testTags de «Editar receta» y «Guardar cambios».

## Enfoque técnico

- **Modo edición**: `CrearRecetaRoute`/`CrearRecetaViewModel` aceptan un `recipeId` opcional (null = alta); con id cargan la Receta con `RecipeRepository.getRecipe(id)` (nuevo, sobre `RecipeDao` con `RecipeWithDetails`), precargan `CrearRecetaUiState` (campos, filas con sus ids, foto y favorito) y `buildRecipe` conserva `id`, `status` y `origin` originales (RF-2, RF-16); si `getRecipe(id)` no devuelve Receta, se vuelve a Mis Recetas sin abrir el formulario (RF-18).
- **Navegación**: `AppRoutes.CREAR_RECETA` pasa a `"crear_receta?recipeId={recipeId}"` y Mis Recetas navega con el id de la tarjeta; al guardar, `popBackStack` y `observeRecipes` refrescan la lista (RF-1, RF-17).
- **Dock por estado**: `ActionDock` se rama por el estado de la Receta editada — Publicada → único «Guardar cambios» habilitado solo si `canPublish`, con `PublishErrors` junto al dock (RF-5 a RF-8); Borrador → dock del alta con sus mismas reglas (RF-9 a RF-12). `FormHeader` no dibuja «Guardar» en ningún modo: el dock es el único guardado (RF-13, RF-14).
- **Guardado**: la acción de guardado persiste con el estado original (RF-6, RF-10); «Publicar Receta» solo ejecuta la transición Borrador → Publicada (RF-11) y nada persiste si se cierra sin guardar (RF-15).
- **Tarjeta y copy**: `RecipeCard` añade el pie del prototipo solo con «Editar receta» (icono `Icons.Outlined.Edit` de 18 dp sobre recuadro visual de 36 dp, `onSurfaceVariant`, área táctil ≥ 48 dp por NFR-2) en la posición izquierda y `RecipeList` recibe `onEditRecipe(id)` (RF-1, RF-4); «Eliminar» y «Cocinar Ahora» no se dibujan (Q-editar-receta-4). Textos de edición en `strings.xml` es/en (NFR-1).

## Archivos esperados

- `ui/crearreceta/CrearRecetaViewModel.kt`, `CrearRecetaScreen.kt`, `CrearRecetaRoute.kt` — modificar; modo edición, dock por estado y cabecera; `res/values/strings.xml`, `values-en/strings.xml` — textos de edición.
- `ui/misrecetas/MisRecetasScreen.kt`, `MisRecetasRoute.kt`, `ui/nav/AppNavHost.kt` — modificar; acción «Editar receta» y navegación con id.
- `domain/repository/RecipeRepository.kt`, `data/local/RecipeDao.kt`, `data/repository/RoomRecipeRepository.kt`, `InMemoryRecipeRepository.kt` — modificar; `getRecipe(id)`.
- `androidTest/.../EditarRecetaE2ETest.kt` — crear; `CrearRecetaE2ETest.kt` y `MisRecetasLocalizationE2ETest.kt` — regresión sin cambios de producto.

## Impacto visual

- Fuente de diseño: `DESIGN.md` y los prototipos `uxdocs/chefencasa_registrar_y_editar_receta/code.html` (formulario, cabecera y dock) y `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (pie de acciones y modal «Editar Receta»); pantallas y estados afectados: Formulario de Receta en modo edición (cabecera y dock según estado) y Mis Recetas (pie de acciones de la tarjeta).
- Fidelidad visual (NFR-4): 1:1 con el prototipo por muestreo de píxeles (bloques, textos, iconos, controles y roles de token) en emulador API 30+, acotado a los bloques implementados (excluidos «Eliminar receta» y «Cocinar Ahora», en «Fuera de alcance»); el resto del formulario hereda sin cambios los RF de `afinar-fidelidad-visual-del-formulario`. No hace falta artefacto de diseño nuevo: los prototipos cubren ambos elementos.

## Impacto en documentación duradera

- `ARCHITECTURE.md` / `CONSTRAINTS.md` / `AGENTS.md`: no necesario — no cambian capas, reglas ni flujo; `docs/domain-model.md`: no necesario (la edición ya está en su ciclo de vida); `docs/risks-and-open-questions.md`: filas Q-editar-receta-1 a Q-editar-receta-4.

## Plan y tareas

- [x] T1: `getRecipe(id)` en `RecipeRepository`, `RecipeDao`, `RoomRecipeRepository` e `InMemoryRecipeRepository`. (cubre RF-2, RF-18)
- [x] T2: modo edición en ViewModel y Route (`recipeId` opcional, precarga y `buildRecipe` con id/estado/origen). (cubre RF-1, RF-2, RF-16)
- [x] T3: dock por estado y cabecera de edición sin «Guardar». (cubre RF-5 a RF-14)
- [x] T4: acción «Editar receta» en el pie de la tarjeta y navegación con id desde Mis Recetas. (cubre RF-1, RF-4, RF-17)
- [x] T5: cabecera «Editar Receta» con distintivo de estado y textos es/en. (cubre RF-3, NFR-1)
- [x] T6: cierre sin guardar. (cubre RF-15)
- [x] T7: tests E2E de edición y regresión de `crear-receta`. (cubre el plan de verificación)

## Plan de verificación

- `./lint.sh` y `./unit.sh` → exit 0; unit: requisitos de Publicar en modo edición (RF-7, RF-8, RF-12) y conservación de estado y origen (RF-6, RF-10, RF-16).
- E2E verify 1: editar el título de una receta publicada y comprobar que Mis Recetas muestra el nuevo título (RF-6, RF-17).
- E2E propios: precarga (RF-2), cabecera e icono (RF-3, RF-4), dock según estado (RF-5, RF-9), deshabilitado y lista en Publicada incompleta (RF-7, RF-8), guardar borrador manteniendo estado (RF-10), publicar desde edición (RF-11, RF-12), ausencia de «Guardar» de cabecera y guardado desde el dock en ambos estados (RF-13, RF-14), cierre sin guardar (RF-15), id inexistente (RF-18) y localización es/en de los textos de edición (NFR-1).
- UI *(solo el validador)*: 2 capturas en emulador API 30+ (formulario en modo edición y tarjeta con el pie de acciones) comparadas por muestreo de píxeles acotado a los bloques implementados (NFR-4) con los prototipos y `DESIGN.md`, y medición por uiautomator (informa en px, convertidos a dp según la densidad del emulador) de área táctil ≥ 48 dp y botón del dock ≥ 52 dp (NFR-2). Regresión: `./e2e.sh` deja en verde `crear-receta` y `afinar-fidelidad-visual-del-formulario`; no se tocan `init`/`lint`/`unit`/`e2e`.

## Evidencia a capturar

- `docs/evidence/editar-receta/informe-validacion.md` con `lint`, `unit`, `e2e`, la verificación por RF y la comparación visual; UI: `docs/evidence/editar-receta/formulario-editar-receta.png` y `docs/evidence/editar-receta/mis-recetas-accion-editar.png` (emulador API 30+, es).

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec.
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤ 4) y solo de esta feature.
- [x] UI: 2 capturas de estados clave y fidelidad 1:1 a los prototipos y a `DESIGN.md`, acotada a los bloques implementados.
- [x] Cobertura E2E añadida y regresión de `crear-receta` y `afinar-fidelidad-visual-del-formulario` en verde.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-18 — veredicto: A RETRABAJAR (intento 1 de 3)

### Hallazgos

1. [contradicción, bloqueante] RF-4, NFR-2, NFR-4, §Enfoque técnico «Tarjeta y copy» y §Plan de verificación: el plan fija el icono «Editar receta» en 36 dp, pero el prototipo lo dibuja de 18 px dentro de un botón de 36×36 px (`w-9 h-9` y `text-[18px]` en `uxdocs/chefencasa_mis_recetas_y_crud/code.html`), `DESIGN.md` y `uxdocs/DESIGN.md` exigen objetivos ≥ 48×48 px y la verificación mide ≥ 48 dp con uiautomator; ningún valor cumple a la vez NFR-2, la fidelidad 1:1 por muestreo de píxeles y la medición → resolver en redacción: fijar el tamaño del icono y del área táctil (y qué prevalece cuando el prototipo contradice `DESIGN.md`), y alinear con ello el texto de §Enfoque técnico y §Plan de verificación.
2. [ambigüedad/contradicción, bloqueante] RF-12 y §Enfoque técnico «Dock por estado»: «el mismo guardado que la acción primaria del dock» no define cuál es la acción primaria cuando el dock del Borrador tiene dos («Guardar Borrador» y «Publicar Receta», que es la primaria visual del prototipo) y choca con el comportamiento ya validado en `afinar-fidelidad-visual-del-formulario` RF-21 («Guardar» de cabecera guarda como Borrador); además no fija el estado de «Guardar» de cabecera cuando RF-7/RF-11 dejan la acción del dock deshabilitada (¿se deshabilita también o guardaría saltándose el bloqueo de RF-7?) → resolver en redacción: nombrar la acción concreta que dispara «Guardar» en cada estado y su comportamiento con la acción del dock deshabilitada.
3. [trazabilidad, menor] RF-3: la cabecera con título y distintivo de estado se dibuja en `uxdocs/chefencasa_registrar_y_editar_receta/code.html` (título + badge + «Guardar»), no en `uxdocs/chefencasa_mis_recetas_y_crud/code.html`, donde «Editar Receta» es el título de un modal CRUD sin badge; tampoco se fija qué ocurre con el subtítulo «Paso a paso con IA» y el resto de la cabecera en modo edición → observación: corregir la cita (coherente con §Impacto visual) y concretar los elementos de la cabecera en edición.
4. [contradicción, menor] NFR-4, §Impacto visual y §Plan de verificación: se exige comparación 1:1 de la tarjeta con el prototipo, pero su pie de acciones incluye «Eliminar receta» y «Cocinar Ahora», que están en «Fuera de alcance» (Q-editar-receta-4); falta la exclusión de los elementos fuera de alcance del criterio de comparación, que `afinar-fidelidad-visual-del-formulario` sí declara → observación: acotar el muestreo de la tarjeta al pie con «Editar receta».
5. [casos límite, menor] RF-2 y §Enfoque técnico «Modo edición»: no se cubre un `recipeId` que no corresponde a ninguna Receta (el nuevo `getRecipe(id)` puede no encontrar nada) → observación: fijar si se abre como alta, se muestra un error o se vuelve a Mis Recetas.
6. [ambigüedad, menor] Casos límite «Borrador en edición incompleto o sin título»: la frase «se guarda tal cual con título (hereda crear-receta); sin título no guarda y lo señala» es confusa y «lo señala» no concreta ubicación ni texto → observación: reescribir la frase citando `crear-receta` RF-13/RF-14 como fuente.
7. [formato/EARS, menor] RF-7 (y en menor grado RF-13) une con «y» varios comportamientos (deshabilitar, enumerar y no guardar), frente a la regla «un requisito, una frase» de EARS → observación: separar en requisitos o acotar la enumeración.
8. [verificabilidad, menor] NFR-1 exige afirmar los textos de edición («Editar Receta», «Editar receta», «Guardar cambios») en español e inglés, pero §Plan de verificación no incluye ningún paso que lo compruebe → observación: añadir la afirmación es/en (p. ej. en el E2E de localización) o acotar NFR-1.

### Siguiente acción
Retrabajar la spec: resolver los hallazgos 1 y 2 en Parte A y Parte B, y aclarar los menores 3 a 8.

## Clarificación — 2026-10-18 — veredicto: APTA (intento 2 de 3)

### Hallazgos

1. [intento 1 · contradicción, resuelto] Icono/área táctil del pie de tarjeta y precedencia `DESIGN.md`/prototipo → ver RF-4, NFR-4, §Enfoque técnico «Tarjeta y copy» y §Plan de verificación.
2. [intento 1 · ambigüedad/contradicción, resuelto] Guardado de cabecera por estado y con la acción del dock deshabilitada → ver RF-13 y RF-14 (coherente con `afinar-fidelidad-visual-del-formulario` RF-21).
3. [intento 1 · trazabilidad, resuelto] Cita del prototipo de la cabecera y composición en modo edición → ver RF-3 y §Casos límite «Cabecera en edición».
4. [intento 1 · contradicción, resuelto] Exclusión de «Eliminar receta» y «Cocinar Ahora» del muestreo 1:1 → ver NFR-4, §Impacto visual y §Checklist del validador.
5. [intento 1 · casos límite, resuelto] `recipeId` sin Receta → ver RF-18, §Casos límite, §Enfoque técnico «Modo edición» y §Plan de verificación.
6. [intento 1 · ambigüedad, resuelto] Caso límite del Borrador reescrito con fuente citada → ver §Casos límite (`crear-receta` RF-13/RF-14).
7. [intento 1 · formato/EARS, resuelto] Atomicidad de RF-7/RF-8 y acotación de RF-15 → ver RF-7, RF-8 y RF-15.
8. [intento 1 · verificabilidad, resuelto] Afirmación es/en de los textos de edición → ver §Plan de verificación (NFR-1).
9. [trazabilidad, menor] §Trazabilidad, fila «RF-6, RF-10, RF-11»: RF-11 se atribuye a `steps` («guardar manteniendo estado») pero su origen es Q-editar-receta-2 según §Dudas abiertas, y la fila «RF-15 a RF-18» agrupa el RF-18 bajo «derivado: caso límite (RD07)» pese a nacer del hallazgo 5 → observación: precisar el origen de RF-11 y RF-18.
10. [verificabilidad, menor] NFR-2 fija el objetivo táctil en ≥ 48×48 px (copia de `DESIGN.md`) mientras RF-4 y §Plan de verificación fijan y miden ≥ 48×48 dp; uiautomator informa bounds en px y la conversión depende de la densidad del emulador → observación: unificar la unidad (dp) o fijar la conversión esperada.

### Siguiente acción
Continuar: la spec queda `ready` y puede pasar a implementación; los menores 9 y 10 son observaciones no bloqueantes que pueden corregirse en el siguiente redactado o aceptarse tal cual.

## Clarificación — 2026-10-19 — veredicto: APTA (intento 3 de 3, cambio de alcance)

### Hallazgos

1. [cambio de alcance pedido por el usuario, resuelto] El «Guardar» de la cabecera era redundante: mismo handler y mismo `enabled` que la acción del dock en los tres estados (alta, Borrador y Publicada). Se elimina en todos los modos, también en el alta (revisa `afinar-fidelidad-visual-del-formulario` RF-3/RF-21) → ver RF-13, RF-14, §Casos límite, §Enfoque técnico, §Plan de verificación y §Plan y tareas (T3).

### Siguiente acción
Implementar el ajuste y re-ejecutar la validación de la feature.
