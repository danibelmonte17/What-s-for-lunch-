---
feature: sincronizar-spec-de-crear-receta
category: tecnica
depends_on: [afinar-fidelidad-visual-del-formulario]
status: ready
source: feature_list.json
---

# Spec: Sincronizar la spec de crear-receta con el «Publicar Receta» deshabilitado y la lista de requisitos

# PARTE A · CONTRATO

## Contexto y objetivo

El producto entregado muestra «Publicar Receta» deshabilitado mientras el formulario no cumple los requisitos de Publicar y enumera los incumplidos junto al dock (`afinar-fidelidad-visual-del-formulario` RF-14 a RF-16, validado en la sesión 19 con la regresión de `crear-receta` en verde en la 23). `docs/specs/crear-receta.md` describe todavía el flujo anterior (pulsar «Publicar Receta» y recibir el requisito incumplido), que la UI ya no permite porque el botón está deshabilitado. El desfase quedó registrado en `PROGRESS.md` como pendiente por la restricción de permisos del implementador sobre `docs/specs/**`.

Esta feature sincroniza las cinco secciones afectadas de esa spec (RF-15, RF-16, Casos límite, Plan de verificación y Trazabilidad) con el comportamiento real y marca la tarea T6 de la spec de la fidelidad visual, cerrando el desfase sin tocar código ni tests (fuente de verdad de solo lectura).

## Actores y permisos

- Agente de documentación: escribe solo bajo `docs/specs/**`; `app/src`, `strings.xml` y los tests son de solo lectura.

## Historia

Cuando mantengo o verifico el formulario de alta desde su spec,
quiero que describa el «Publicar Receta» deshabilitado con la lista de requisitos junto al dock,
para poder trabajar sin contradicción con el producto ya entregado.

## Requisitos funcionales

- RF-1: EL SISTEMA fija en RF-15 de `docs/specs/crear-receta.md` que MIENTRAS el formulario no cumple los requisitos de Publicar, «Publicar Receta» se muestra deshabilitado con un estilo distinguible.
- RF-2: EL SISTEMA fija en RF-16 de `docs/specs/crear-receta.md` que MIENTRAS el formulario no cumple los requisitos de Publicar, se enumeran junto al dock los requisitos incumplidos que impiden Publicar (título, foto, al menos un Ingrediente y un Paso válidos y ninguna fila de Ingrediente ni Paso inválidos).
- RF-3: EL SISTEMA describe en los Casos límite de `docs/specs/crear-receta.md` el bloqueo por requisitos de Publicar incumplidos («Publicar Receta» deshabilitado con la lista junto al dock).
- RF-4: EL SISTEMA describe en el Plan de verificación de `docs/specs/crear-receta.md` los escenarios E2E del botón deshabilitado y de la lista de requisitos junto al dock.
- RF-5: EL SISTEMA fija en la Trazabilidad de `docs/specs/crear-receta.md` la verificación E2E y la evidencia de sus RF-15 y RF-16.
- RF-6: EL SISTEMA marca como completada la tarea T6 de `docs/specs/afinar-fidelidad-visual-del-formulario.md`.
- RF-7: EL SISTEMA limita los cambios de esta feature a ficheros bajo `docs/specs/**`.

## Requisitos no funcionales

- NFR-1: La documentación queda redactada en español, con los identificadores de código citados en inglés.

## Casos límite

- La enumeración de requisitos usa la fuente única `⚠ S07` / `RecipeValidation.validateForPublish`; no se crea una redacción alternativa → RF-2.
- La habilitación al cumplir los requisitos no gana RF propia: los Casos límite la apuntan a RF-17 de `crear-receta.md` → RF-3.
- El resto de ítems de Casos límite y puntos del Plan de verificación de `crear-receta.md` se conserva intacto → RF-3, RF-4.
- T6 cubría spec + tests E2E + regresión: tests y regresión ya están completos y en verde (informe de evidencia de la fidelidad); esta feature completa solo la parte de spec → RF-6.
- Ante cualquier matiz de redacción, manda `CrearRecetaE2ETest` como fuente del comportamiento → RF-1, RF-2.

## Fuera de alcance

- Cambios en `app/src`, `strings.xml` o tests: son la fuente de comportamiento que fija el texto objetivo (RF-7).
- `docs/specs/editar-receta.md` (RF-7/RF-8/RF-12/RF-14 ya usan este patrón), `docs/domain-model.md` y `⚠ S07`: ya reflejan la regla de Publicar (Q-crear-receta-1).
- Las demás secciones de `docs/specs/crear-receta.md` (otros RF, NFR, Fuera de alcance, Criterios, Enfoque técnico, «Registro de iteraciones» y «## Clarificación»).
- Re-ejecutar `lint`/`unit`/`e2e`: la regresión está medida (44 tests, sesión 23) y la feature es de solo documentación.
- Añadir una línea de registro del cambio en `crear-receta.md` — descartado por Q-sincronizar-spec-de-crear-receta-2 (resuelta: no).

## Criterios de finalización

- verify 1 y verify 2 de la entrada en `feature_list.json` pasan (revisión de las cinco secciones sin contradicción con `CrearRecetaE2ETest` y T6 marcado).
- `git diff --name-only` muestra solo rutas bajo `docs/specs/` y NFR-1 se cumple.

## Dudas abiertas

- Ninguna abierta. Resueltas con el usuario el 2026-10-19 (registro conservado):
  - Q-sincronizar-spec-de-crear-receta-1 [resuelta]: ¿RF-15 de `crear-receta.md` queda el botón deshabilitado y RF-16 la lista junto al dock, o al revés? — no bloqueante. Resuelta: RF-15 deshabilitado y RF-16 lista (orden de la `description`, `editar-receta` RF-7/RF-8 y `afinar-fidelidad-visual-del-formulario` RF-14/RF-15).
  - Q-sincronizar-spec-de-crear-receta-2 [resuelta]: ¿debe quedar también una línea en «Registro de iteraciones» de `crear-receta.md` que registre esta sincronización? — no bloqueante. Resuelta: no; el rastro vive en esta spec y en el Historial de `docs/progress-history.md`.

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `steps`: actualizar RF-15/RF-16 | verify 1 (revisión de RF-15) | revisión de la spec |
| RF-2 | `steps`: actualizar RF-15/RF-16 | verify 1 (revisión de RF-16) | revisión de la spec |
| RF-3 | `steps`: alinear Casos límite | verify 1 (revisión de Casos límite) | revisión de la spec |
| RF-4 | `steps`: alinear Plan de verificación | verify 1 (revisión del Plan) | revisión de la spec |
| RF-5 | `steps`: alinear Trazabilidad | verify 1 (revisión de Trazabilidad) | revisión de la spec |
| RF-6 | `steps`: marcar T6 | verify 2 | revisión del checkbox |
| RF-7 | `steps`: rutas de los ficheros citados | ver Plan de verificación (`git diff`) | salida de `git diff --name-only` |

## Registro de iteraciones

- Sesión de respuestas (2026-10-19): Q-sincronizar-spec-de-crear-receta-1 y Q-sincronizar-spec-de-crear-receta-2 resueltas con el usuario, confirmando los valores asumidos; la Parte A no cambia de contenido y queda sin dudas abiertas.

# PARTE B · PLAN

## Investigación del repositorio

### Archivos inspeccionados

- `docs/specs/crear-receta.md` — spec a sincronizar: RF-15/RF-16 aún describen pulsar «Publicar Receta» y recibir el requisito incumplido; Casos límite, Plan de verificación y Trazabilidad heredan esa semántica.
- `docs/specs/afinar-fidelidad-visual-del-formulario.md` — RF-14 a RF-16 fijan el comportamiento entregado; T6 es la única tarea sin marcar.
- `docs/evidence/afinar-fidelidad-visual-del-formulario/informe-validacion.md` — T6 queda sin marcar solo por la parte de spec (permisos sobre `docs/specs/**`); tests y regresión completos.
- `app/src/androidTest/.../CrearRecetaE2ETest.kt` — escenarios `publishIsDisabledUntilRequirementsAreMet`, `publishWithoutPhotoIsBlocked`, `publishWithoutIngredientIsBlocked`, `invalidIngredientRowsAreListedAsBlockingRequirement`, `blockingErrorsAreLocalizedInEnglish`, `publishWithPhotoSavesPublishedRecipe`.
- `app/src/main/.../crearreceta/CrearRecetaScreen.kt`, `CrearRecetaViewModel.kt`, `domain/crearreceta/RecipeValidation.kt` — `canPublish`, botón `enabled`, `PublishErrors` junto al dock y `validateForPublish` (fuente única de requisitos).
- `docs/specs/editar-receta.md` — RF-7/RF-8/RF-12/RF-14: patrón de redacción a imitar.
- `PROGRESS.md`, `docs/risks-and-open-questions.md` — pendiente documentado, `⚠ S07` y Q-crear-receta-1 (orígenes de RF-15/RF-16).

### Patrones existentes a seguir

- EARS «MIENTRAS … deshabilitado» / «MIENTRAS … enumera junto al dock» de `editar-receta` RF-7/RF-8 y `afinar-fidelidad-visual-del-formulario` RF-14/RF-15 (un requisito, una frase).
- Enumeración única de requisitos de Publicar: `⚠ S07` / `RecipeValidation.validateForPublish`, ya citada por `afinar-fidelidad-visual-del-formulario` RF-15.
- Trazabilidad con celdas cortas y verificación apuntando a `verify` o `ver Plan de verificación`.

### Carencias actuales

- Ninguna: Q-sincronizar-spec-de-crear-receta-1 y Q-sincronizar-spec-de-crear-receta-2 quedaron resueltas con el usuario (ver §Dudas abiertas).

## Enfoque técnico

Parche mínimo sobre dos ficheros de spec. El texto objetivo es canónico en la tabla siguiente (el reparto RF-15/RF-16 quedó confirmado por Q-sincronizar-spec-de-crear-receta-1) y no se copia a otras secciones; se admite redacción equivalente solo en conectores, sin cambiar semántica ni citas de origen, y `CrearRecetaE2ETest` arbitra cualquier matiz:

| Sección de `crear-receta.md` | Contenido objetivo |
|---|---|
| RF-15 | «MIENTRAS el formulario no cumple los requisitos de Publicar, EL SISTEMA muestra «Publicar Receta» deshabilitado con un estilo distinguible (`⚠ S07`).» |
| RF-16 | «MIENTRAS el formulario no cumple los requisitos de Publicar, EL SISTEMA enumera junto al dock los requisitos incumplidos que impiden Publicar (título, foto, al menos un Ingrediente y un Paso válidos y ninguna fila de Ingrediente ni Paso inválidos) (Q-crear-receta-1, que revisa `⚠ S07`).» |
| Casos límite · «Guardado incompleto» | «sin título → RF-14; requisitos de Publicar incumplidos → «Publicar Receta» deshabilitado y requisitos enumerados junto al dock → RF-15, RF-16; cumplidos → habilitado y publicable → RF-17»; el resto del punto y los demás ítems intactos |
| Plan de verificación · «E2E propios» | sustituir «publicar sin ingrediente/paso o sin foto → error visible y sin guardar (RF-15, RF-16)» por «Publicar deshabilitado y lista junto al dock mientras falta algún requisito y habilitado al cumplirlos, sin guardar nada antes de publicar (RF-15, RF-16)», citando `publishIsDisabledUntilRequirementsAreMet`, `publishWithoutPhotoIsBlocked`, `publishWithoutIngredientIsBlocked` e `invalidIngredientRowsAreListedAsBlockingRequirement`; los demás puntos quedan intactos |
| Trazabilidad · filas RF-15 y RF-16 | RF-15 → origen «`steps` S07 + `afinar-fidelidad-visual-del-formulario` RF-14», verificación «escenario E2E: Publicar deshabilitado», evidencia «informe E2E»; RF-16 → origen «`steps` S07 + Q-crear-receta-1 + `afinar-fidelidad-visual-del-formulario` RF-15», verificación «escenario E2E: lista junto al dock», evidencia «informe E2E» |

En `afinar-fidelidad-visual-del-formulario.md` solo se cambia `[ ]` por `[x]` en T6 (cuyo resto —adaptación de tests E2E y regresión— ya está hecho).

## Archivos esperados

- `docs/specs/crear-receta.md` — modificar; solo las cinco secciones del texto objetivo.
- `docs/specs/afinar-fidelidad-visual-del-formulario.md` — modificar; solo el checkbox T6.

## Impacto en documentación duradera

- `ARCHITECTURE.md`/`CONSTRAINTS.md`: no necesario — no existen (la arquitectura vive en `CONTEXT.md` y `docs/technical-discovery.md`).
- `AGENTS.md`: no necesario — no cambian flujo, comandos ni convenciones.
- Otros: `docs/domain-model.md` y `⚠ S07` no cambian — ya reflejan la regla de Publicar (Q-crear-receta-1).

## Plan y tareas

- [x] T1: reescribir RF-15 y RF-16 de `crear-receta.md` con el texto objetivo. (cubre RF-1, RF-2)
- [x] T2: alinear Casos límite, Plan de verificación y Trazabilidad de `crear-receta.md`. (cubre RF-3, RF-4, RF-5)
- [x] T3: marcar el checkbox T6 de `afinar-fidelidad-visual-del-formulario.md`. (cubre RF-6)
- [x] T4: revisar el diff y el idioma de lo redactado. (cubre RF-7, NFR-1)

## Plan de verificación

- verify 1 (revisión): RF-15, RF-16, Casos límite, Plan de verificación y Trazabilidad de `crear-receta.md` describen el deshabilitado + la lista junto al dock y no contradicen `CrearRecetaE2ETest` → sin rastro de la redacción previa «pulsa … e indica el requisito incumplido».
- verify 2 (revisión): el checkbox T6 de `afinar-fidelidad-visual-del-formulario.md` figura `[x]` → marcado.
- `git diff --name-only` → solo rutas bajo `docs/specs/`.
- E2E: no aplica — feature de solo documentación; la regresión medida (44 tests, sesión 23) es la fuente de comportamiento citada.
- Scripts de verificación (`init`/`lint`/`unit`/`e2e`): no se tocan.

## Evidencia a capturar

- `docs/evidence/sincronizar-spec-de-crear-receta/informe-validacion.md` con la salida de `git diff --name-only` y la verificación de verify 1 y verify 2 (1 artefacto; sin capturas: no hay UI).

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec (solo `docs/specs/**`).
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤ 4) y solo de esta feature.
- [x] UI: no aplica — sin cambios visibles (feature de documentación).
- [x] Cobertura E2E: no aplica — justificada por feature de solo documentación sin cambios de comportamiento.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-19 — veredicto: APTA (intento 1 de 3)

### Hallazgos
1. [bloque 5 · trazabilidad, menor] §Enfoque técnico, fila «Guardado incompleto»: «cumplidos → habilitado y publicable → RF-17» apunta la habilitación a RF-17, que no la fija (vive en `afinar-fidelidad-visual-del-formulario` RF-16; decisión «sin RF propia» ya explícita en §Casos límite) → observación: acotar la flecha a «publicable → RF-17» o citar la fuente de la habilitación.
2. [bloque 5 · formato, menor] Gate B (cada duda en §Carencias actuales, §Enfoque técnico y §Fuera de alcance): Q-sincronizar-spec-de-crear-receta-1 solo se cita en §Enfoque técnico y Q-…-2 solo en §Fuera de alcance → observación formal; ambas están resueltas y sus efectos quedan registrados en §Dudas abiertas y §Registro de iteraciones.

### Siguiente acción
Continuar (APTA).
