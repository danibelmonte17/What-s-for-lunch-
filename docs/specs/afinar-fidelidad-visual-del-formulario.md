---
feature: afinar-fidelidad-visual-del-formulario
category: funcional
depends_on: [crear-receta]
status: ready
source: feature_list.json
---
# Spec: Fidelidad visual del formulario de alta y las tarjetas de Mis Recetas

# PARTE A · CONTRATO

## Contexto y objetivo

El formulario de alta y las tarjetas de Mis Recetas funcionan desde `crear-receta`, pero su composición y varios roles de token divergen del prototipo (`uxdocs/chefencasa_registrar_y_editar_receta/code.html`, `uxdocs/chefencasa_mis_recetas_y_crud/code.html`): secciones, campos y chips caen en `surfaceContainerHighest` y el chip seleccionado usa `secondaryContainer`. Esta feature rediseña la composición del formulario para calcar los bloques del prototipo, alinea las tarjetas (estilo y metadatos ya disponibles) y corrige esos roles. Nota de impacto cruzado: deshabilitar «Publicar Receta» (RF-14/RF-16) cambia la interacción validada de `crear-receta` (sus RF-15/RF-16 exigen el aviso al pulsar); se actualizan su spec y sus tests E2E y su verificación se re-ejecuta como regresión.

## Actores y permisos

- **Usuario** (único rol del MVP, propietario del dispositivo): usa el formulario y ve las tarjetas de Mis Recetas. Sin cuentas ni roles adicionales (`P31`).

## Historia

Cuando quiero dar de alta y revisar mis platos,
quiero que el formulario y las tarjetas se vean y se comporten como el prototipo, con las acciones claras a simple vista,
para poder usarlos sin fricción y percibir una sola app coherente.

## Requisitos funcionales

- RF-1: CUANDO el usuario abre el formulario, EL SISTEMA muestra, en este orden, la cabecera, la portada de foto, la información básica, la rejilla de métricas, los ingredientes, los pasos, el favorito y el dock de acciones.
- RF-2: EL SISTEMA presenta la foto en una portada a ancho completo con los controles de foto (galería, cámara y generar con IA) al pie de la portada.
- RF-3: EL SISTEMA muestra en la cabecera el título del formulario y el distintivo de estado de la Receta (Borrador/Publicada).
- RF-4: EL SISTEMA presenta el momento culinario como una fila de chips con los cuatro momentos (Desayuno, Almuerzo, Cena, Postre).
- RF-5: EL SISTEMA presenta tiempo, dificultad y raciones como una rejilla de tres bloques, cada uno con su icono y su control de ajuste.
- RF-6: EL SISTEMA presenta cada fila de Ingrediente en línea dentro de la sección, con nombre, cantidad, unidad y marca de opcional.
- RF-7: EL SISTEMA presenta cada Paso con su número dentro de un círculo destacado, su texto y su temporizador.
- RF-8: EL SISTEMA presenta «Guardar Borrador» y «Publicar Receta» en un dock siempre visible al pie del formulario.
- RF-9: EL SISTEMA usa el blanco de superficie `surface-container-lowest` (#FFFFFF) como fondo de las secciones del formulario.
- RF-10: EL SISTEMA usa el fondo claro `surface-container-low` (#F5F3EF) en los campos de entrada y en los chips no seleccionados.
- RF-11: EL SISTEMA usa el terracota `primary` (#A83212) con texto `on-primary` (#FFFFFF) en el chip seleccionado de momento culinario y de dificultad.
- RF-12: CUANDO un campo de entrada recibe el foco, EL SISTEMA lo marca con un anillo `primary`.
- RF-13: EL SISTEMA usa esquinas redondeadas de 1rem y sombra cálida ámbar-carbón (`uxdocs/DESIGN.md` §Ambient Shadow Character) en las secciones, y forma de píldora en chips y badges.
- RF-14: MIENTRAS el formulario no cumple los requisitos de Publicar, EL SISTEMA muestra «Publicar Receta» deshabilitado con un estilo distinguible.
- RF-15: MIENTRAS el formulario no cumple los requisitos de Publicar, EL SISTEMA enumera junto al dock los requisitos incumplidos que impiden Publicar (título, foto, al menos un Ingrediente y un Paso válidos y ninguna fila de Ingrediente ni Paso inválidos).
- RF-16: CUANDO el formulario cumple los requisitos de Publicar, EL SISTEMA habilita «Publicar Receta».
- RF-17: CUANDO existen Recetas guardadas, EL SISTEMA muestra cada tarjeta con su foto a ancho completo y el badge del momento culinario superpuesto sobre la imagen.
- RF-18: EL SISTEMA muestra en cada tarjeta el título y una fila de metadatos con el tiempo total (preparación + cocción), la dificultad y las raciones, cada uno con su icono.
- RF-19: EL SISTEMA muestra en cada tarjeta el distintivo de estado (Borrador/Publicada) como una píldora.
- RF-20: EL SISTEMA usa `surface-container-lowest` (#FFFFFF), esquinas de 0.75rem y sombra cálida ámbar-carbón (`uxdocs/DESIGN.md` §Ambient Shadow Character) en las tarjetas de Mis Recetas.
- RF-21: EL SISTEMA no muestra «Guardar» en la cabecera del formulario: el guardado se realiza solo desde el dock («Guardar Borrador»/«Publicar Receta» en el alta y en un Borrador, «Guardar cambios» en una Publicada).
- RF-22: SI una Receta no tiene foto, ENTONCES EL SISTEMA muestra el badge del momento culinario junto al título de la tarjeta.

## Requisitos no funcionales

- NFR-1: Interfaz en español e inglés vía `strings.xml` (`P19`); los textos nuevos del formulario y de las tarjetas se afirman en ambas lenguas.
- NFR-2: Objetivos táctiles ≥ 48×48 px y contraste WCAG 2.1 AA; el botón primario del dock mide ≥ 52 px de alto (`DESIGN.md`).
- NFR-3: Android 11 (API 30) o superior, solo móvil (`P20`).
- NFR-4: Los colores del formulario y de Mis Recetas coinciden con los roles del prototipo (muestreo de píxeles, verify 1).

## Casos límite

- Formulario sin foto → la portada muestra sus controles sin imagen → RF-2.
- Requisitos de Publicar incumplidos → Publicar deshabilitado y lista junto al dock → RF-14, RF-15; cumplidos → habilitado → RF-16.
- Tarjeta sin foto → arranca por el título y el badge del momento va junto a él → RF-17, RF-22.
- Tarjeta con tiempo total 0 o Borrador sin metadatos → muestra los valores por defecto sin ocultar la fila → RF-18.
- Cabecera sin «Guardar»: el guardado queda solo en el dock, que exige título para «Guardar Borrador» → RF-21.

## Fuera de alcance

- Buscador, filtro, favorito, editar/borrar, «Cocinar Ahora» y badge de Coincidencia en Mis Recetas (`buscar-y-filtrar-recetas`, `editar-receta`, `eliminar-receta`, `mostrar-badge-de-coincidencia`).
- Etiquetas y Dieta, «Escanear con IA», «Reconocimiento Activo» y reordenado drag & drop (fuera en `crear-receta`); el asa se dibuja decorativa (Q-afinar-fidelidad-visual-del-formulario-1).
- Pantalla de Ajustes y posteriores (`configurar-acceso-a-ia` y siguientes).

## Criterios de finalización

- Los 22 RF pasan su verificación (E2E en emulador API 30+, unit si aplica y comparación visual) y `lint` + `unit` están en verde.
- Evidencia capturada dentro del presupuesto (≤ 4 artefactos, solo de esta feature).
- La spec de `crear-receta` (RF-15/RF-16) y sus tests E2E reflejan Publicar deshabilitado y su verificación se re-ejecuta como regresión.

## Dudas abiertas

- Ninguna. Resueltas Q-afinar-fidelidad-visual-del-formulario-1 (asa decorativa, sin reordenado; RF-6) y Q-afinar-fidelidad-visual-del-formulario-2 (botón «Guardar» de cabecera como Borrador; RF-21), esta última revisada el 2026-10-19 por Q-editar-receta-5 (se elimina el botón de cabecera en todos los modos), en `docs/risks-and-open-questions.md`.

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 a RF-8 | `steps`: comparar atributo a atributo el formulario | verify 2 (uiautomator: bloques, textos, iconos y controles) | informe de validación + captura |
| RF-9 a RF-13 | `steps`: corregir divergencias de rol de token | verify 1 (muestreo de píxeles) | captura del formulario |
| RF-14 a RF-16 | `verify` 2; impacto en `crear-receta` | verify 2 + escenario E2E de estado | informe E2E + captura |
| RF-17 a RF-20, RF-22 | `steps`: alinear las tarjetas de Mis Recetas | verify 1 (muestreo de píxeles) | captura de Mis Recetas |
| RF-21 | Q-afinar-fidelidad-visual-del-formulario-2, revisada por Q-editar-receta-5 | escenario E2E: la cabecera no dibuja «Guardar» y el dock guarda el Borrador | informe E2E |

## Registro de iteraciones

- Parte A nacida con las respuestas del usuario (rediseño estructural completo, tarjetas con metadatos, Publicar deshabilitado) y las dudas resueltas (asa decorativa, botón «Guardar» de cabecera como Borrador → RF-21).
- Retrabajo tras auditoría (A RETRABAJAR intento 1): RF-22 fija el badge cuando la Receta no tiene foto (hallazgo 1) con caso límite y verificación; RF-13/RF-20 citan la sombra de `uxdocs/DESIGN.md` §Ambient Shadow Character; RF-15 concreta requisitos y ubicación junto al dock; se comprime la spec (verbosidad) y se adelanta la tarea de iconos y textos (T2, prerrequisito).
- Revisión 2026-10-19 (Q-editar-receta-5, pedido del usuario): se elimina «Guardar» de la cabecera en todos los modos para no duplicar el guardado del dock; RF-3 y RF-21 se reescriben, el caso límite y la trazabilidad se alinean y `CrearRecetaE2ETest` pasa a afirmar su ausencia. Regresión re-ejecutada en la sesión 23 (`lint`/`unit`/`e2e` en verde, 44 tests) y recogida en el informe de validación.

# PARTE B · PLAN

## Investigación del repositorio

### Archivos inspeccionados

- `ui/crearreceta/CrearRecetaScreen.kt` y `CrearRecetaViewModel.kt` — composición y validación a rediseñar; añadir estado «puede publicar».
- `ui/misrecetas/MisRecetasScreen.kt`, `ui/theme/*`, `domain/model/Recipe.kt` — tarjeta a alinear, tokens/radios existentes (sin token de sombra) y metadatos de las tarjetas.
- `androidTest/.../CrearRecetaE2ETest.kt` — `publishWithoutPhotoIsBlocked`, `publishWithoutIngredientIsBlocked` y `blockingErrorsAreLocalizedInEnglish` a adaptar.
- `uxdocs/chefencasa_registrar_y_editar_receta/code.html`, `uxdocs/chefencasa_mis_recetas_y_crud/code.html`, `uxdocs/DESIGN.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml` — referencia visual y ausencia de `material-icons-extended`.

### Patrones existentes a seguir

- Tokens en `ui/theme/`, testTags en objetos `*TestTags` y textos en `values/` y `values-en/`.
- MVVM + StateFlow (`Route` → `ViewModel` → `Screen`) con Koin; scripts `init`/`lint`/`unit`/`e2e` y `lint.xml` en la raíz.

### Carencias actuales

- Sin `material-icons-extended` ni token de sombra cálida (secciones y tarjetas).
- Sin estado calculado «puede publicar» en el ViewModel.

## Enfoque técnico

- **Estilo compartido**: contenedor de sección (`surfaceContainerLowest`, radio 1rem, sombra de `uxdocs/DESIGN.md`), chip seleccionado en `primary`/`onPrimary` y campos rellenos (`surfaceContainerLow`, foco `primary`) corrigen RF-9 a RF-13 en un solo sitio.
- **Formulario**: reordenar `CrearRecetaScreen` en cabecera (título y distintivo, sin «Guardar»: el guardado vive en el dock, RF-21), portada, información básica, rejilla de métricas, ingredientes (asa decorativa, Q-afinar-fidelidad-visual-del-formulario-1), pasos con número circular, favorito y dock fijo en el `bottomBar` (RF-1 a RF-8).
- **Publicar**: exponer en `CrearRecetaUiState` si el formulario cumple `RecipeValidation.validateForPublish`; `Button(enabled = uiState.canPublish)` y lista de requisitos junto al dock mientras esté deshabilitado (RF-14 a RF-16).
- **Tarjetas**: `RecipeCard` con portada y badge de momento superpuesto (o junto al título sin foto, RF-22), título, metadatos con iconos (tiempo `prepMinutes + cookMinutes`, dificultad, raciones) y píldora de estado sobre `surfaceContainerLowest` (RF-17 a RF-20).
- **Impacto cruzado en `crear-receta`**: actualizar RF-15/RF-16 de `docs/specs/crear-receta.md` y los tres tests E2E citados, y re-ejecutar `unit`/`e2e` como regresión.

## Archivos esperados

- `ui/crearreceta/CrearRecetaScreen.kt` y `CrearRecetaViewModel.kt` — modificar; rediseño y estado «puede publicar».
- `ui/misrecetas/MisRecetasScreen.kt` y `ui/theme/` — modificar; tarjeta y token de sombra.
- `res/values/strings.xml`, `values-en/strings.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml` — ampliar; textos y `material-icons-extended`.
- `androidTest/.../CrearRecetaE2ETest.kt` — modificar; tests de Publicar, cabecera y fidelidad.
- `docs/specs/crear-receta.md` — actualizar; RF-15/RF-16.

## Impacto visual

- Fuente de diseño: `DESIGN.md` y los prototipos `uxdocs/chefencasa_registrar_y_editar_receta/` y `uxdocs/chefencasa_mis_recetas_y_crud/`.
- Pantallas o estados afectados: Formulario de Receta (rediseño con Publicar deshabilitado) y Mis Recetas (tarjetas con metadatos).
- Fidelidad (NFR-4): colores por rol del prototipo (muestreo de píxeles) y presencia de sus bloques, textos, iconos y controles, excluidos los de «Fuera de alcance». No hace falta artefacto de diseño nuevo.

## Impacto en documentación duradera

- `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`: no necesario (no cambian capas, reglas ni flujo). Otros: `docs/specs/crear-receta.md` actualizar RF-15/RF-16; `docs/domain-model.md` no cambia.

## Plan y tareas

- [x] T1: contenedor de sección, chip de selección y campos rellenos con los roles de token correctos. (cubre RF-9 a RF-13)
- [x] T2: dependencia de iconos (`material-icons-extended`) y textos es/en; prerrequisito de T3 y T5. (cubre NFR-1)
- [x] T3: rediseño estructural del formulario (cabecera con título y distintivo, portada, métricas, ingredientes, pasos, favorito y dock). (cubre RF-1 a RF-8, RF-21)
- [x] T4: estado «puede publicar», botón deshabilitado y lista junto al dock. (cubre RF-14 a RF-16)
- [x] T5: tarjetas de Mis Recetas con foto, metadatos, píldora y badge sin foto. (cubre RF-17 a RF-20, RF-22)
- [x] T6: adaptar la spec y los tests E2E de `crear-receta` y re-ejecutar su regresión. (cubre el criterio de finalización)
- [x] T7: tests E2E de fidelidad, estado deshabilitado y tarjeta sin foto. (cubre el plan de verificación)

## Plan de verificación

- `./lint.sh` y `./unit.sh` → exit 0.
- E2E de `CrearRecetaE2ETest`: Publicar deshabilitado sin requisitos y habilitado con ellos (RF-14, RF-16), lista junto al dock (RF-15), presencia de bloques/iconos (RF-1 a RF-8) y cabecera sin «Guardar» con guardado desde el dock (RF-21).
- E2E de tarjetas: foto/badge, metadatos, píldora de estado y badge junto al título sin foto (RF-17 a RF-20, RF-22).
- UI (solo el validador): 2 capturas en emulador API 30+ comparadas por muestreo de píxeles con los prototipos y `DESIGN.md` (NFR-4); regresión de `crear-receta` (`unit`/`e2e`) en verde. No se tocan los scripts del harness.

## Evidencia a capturar

- `docs/evidence/afinar-fidelidad-visual-del-formulario/informe-validacion.md` con `lint`, `unit`, `e2e`, la verificación por RF y la comparación visual.
- UI: `.../formulario-receta.png` y `.../mis-recetas-tarjetas.png` (emulador API 30+, es).

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec.
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤ 4) y solo de esta feature.
- [x] UI: 2 capturas de estados clave y fidelidad a los prototipos y a `DESIGN.md`.
- [x] Cobertura E2E añadida o actualizada y regresión de `crear-receta` en verde.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-17 — veredicto: A RETRABAJAR (intento 1 de 3)

### Hallazgos

1. [bloque 3 · casos límite, bloqueante] RF-17 vs Casos límite «Tarjeta sin foto»: RF-17 sitúa el badge de momento culinario «superpuesto sobre la imagen», pero el caso límite de tarjeta sin foto solo dice que no se dibuja la portada y la tarjeta arranca por el título, sin fijar el destino del badge (¿se omite, se reubica en línea con el título u otra regla?); con Borradores sin foto (posibles por `crear-receta` RF-13/RF-14) el implementador tendría que inventar y el validador no podría juzgar → resolver en redacción: fijar la regla del badge cuando no hay portada, reflejarla en el caso límite y en el E2E de tarjetas (RF-17 a RF-19).
2. [bloque 1 · ambigüedad, menor] RF-13 y RF-20: «sombra cálida» no es medible y la spec no cita su valor (existe en `uxdocs/DESIGN.md` §«Ambient Shadow Character», sombras ámbar-carbón) → observación: citar esa sección o fijar el token de sombra (elevación + tinte) en la spec.
3. [bloque 3 · casos límite, menor] RF-15 y Casos límite «Requisitos de Publicar»: la enumeración (título, foto, ingredientes o pasos) omite las filas/Pasos inválidos que `RecipeValidation.validateForPublish` (heredado de `crear-receta` RF-8/RF-12/RF-17) también bloquea, y «en línea» no concreta dónde se muestra la lista → observación: citar `validateForPublish` como fuente única de los requisitos incumplidos y fijar la localización de la lista.
4. [bloque 5 · formato, menor] Verbosidad: la spec mide 196 líneas (189 sin frontmatter), por encima del umbral de 170 de `gates.md` («retrabaja por verbosidad») → aprovechar el retrabajo para comprimir §Investigación del repositorio y §Enfoque técnico sin perder trazabilidad.
5. [bloque 5 · formato, menor] §Plan y tareas: T5 (dependencia `material-icons-extended` y textos es/en) es prerrequisito de los iconos de T2 y T4 pero figura después → observación: adelantar T5 o explicitar la dependencia entre tareas.

### Siguiente acción

Retrabajar la spec (A RETRABAJAR): resolver el hallazgo 1 y, de paso, los menores 2 a 5.

## Clarificación — 2026-10-17 — veredicto: APTA (intento 2 de 3)

### Hallazgos

1. [bloque 2 · contradicción, menor] RF-17 y RF-22: RF-17 enuncia «cada tarjeta con su foto a ancho completo y el badge … superpuesto» sin acotar el supuesto de foto; RF-22 (SI/ENTONCES) y el caso límite «Tarjeta sin foto» fijan la excepción, así que el implementador no debe inventar → observación: acotar RF-17 a «CUANDO la Receta tiene foto» en una futura revisión.
2. [bloque 5 · trazabilidad, menor] §Trazabilidad, fila «RF-17 a RF-20, RF-22»: cita solo verify 1 (muestreo de píxeles) aunque §Plan de verificación añade el E2E de tarjetas para esos mismos RF → observación: alinear la celda con ambas verificaciones.
3. [bloque 5 · verificabilidad, menor] NFR-2 (objetivos ≥ 48×48 px y botón del dock ≥ 52 px) no tiene comprobación explícita en §Plan de verificación → observación: añadir medición por uiautomator o declararla cubierta por la comparación visual.

Hallazgos del intento 1 resueltos: badge sin foto fijado en RF-22, caso límite y E2E (ver §Requisitos funcionales); sombra citada de `uxdocs/DESIGN.md` §Ambient Shadow Character en RF-13/RF-20 (ver §Requisitos funcionales); RF-15 enumera requisitos y localización junto al dock (ver §Requisitos funcionales); cuerpo comprimido a 168 líneas (ver §Investigación del repositorio); T2 adelantada como prerrequisito de T3/T5 (ver §Plan y tareas).

### Siguiente acción

Continuar (APTA): la spec pasa a `ready` y queda lista para implementación.
