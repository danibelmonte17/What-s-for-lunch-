# Plantilla de spec

Usa esta estructura para `docs/specs/<feature-id>.md`. Escríbela optimizada en tokens (regla común en `SKILL.md`): prosa mínima y tablas > párrafos. Sin objetivo de líneas: la extensión la fija el contenido. Las secciones marcadas *(solo si aplica)* se omiten por completo cuando no aplican; no las dejes vacías.

Regla de fuente única: cada dato se define una sola vez en su sección canónica y el resto referencia con `ver §X`; prohibido copiar la misma aserción en Trazabilidad, Enfoque, Tareas y Verificación. Los `Q-<feature-id>-<n>` solo se definen en «Dudas abiertas» y `docs/risks-and-open-questions.md`; el resto del doc los cita sin re-explicar la decisión.

````md
---
feature: <feature-id>
category: <category>
depends_on: [<ids de features de las que depende>]
status: draft            # draft | needs-clarification (hay incógnitas, no detiene) | ready (lo escribe el modo auditoría con APTA)
source: feature_list.json
---

# Spec: <título de la feature>

# PARTE A · CONTRATO

> QUÉ y POR QUÉ. Sin stack, arquitectura, nombres de archivos, esquemas, algoritmos ni firmas.

## Contexto y objetivo

<Qué problema resuelve y por qué merece la pena. Uno o dos párrafos.>

## Actores y permisos

- <Actor/rol>: <comportamiento permitido y restricciones.>

## Historia

Cuando <situación o contexto>,
quiero <acción o motivación>,
para poder <resultado esperado>.

## Requisitos funcionales

> Un patrón EARS por requisito, una sola frase, verificable. Incluye aquí los casos límite y errores.

- RF-1: CUANDO <disparador>, EL SISTEMA <hará> (resultado observable).
- RF-2: SI <condición no deseada>, ENTONCES EL SISTEMA <hará>.
- RF-3: MIENTRAS <estado>, EL SISTEMA <hará>.
- RF-4: EL SISTEMA <hará>.

## Requisitos no funcionales *(solo si aplica)*

<Rendimiento, seguridad, plataformas, idioma… siempre con umbral medible.>

## Casos límite

- <Caso (vacíos, duplicados, datos corruptos, límites, concurrencia…)> → cubierto por RF-n.

## Fuera de alcance

> Incluye lo que queda condicionado a una duda abierta, no solo lo que se descarta por completo.

- <Lo que explícitamente NO se hace en esta feature.>

## Criterios de finalización

<Ej.: todos los RF con verificación en verde + evidencia capturada.>

## Dudas abiertas

> Cada duda de aquí se copia a `docs/risks-and-open-questions.md` como fila `Q-<feature-id>-<n>` (ver «Registro de aclaraciones» en la skill).

- [NECESITA ACLARACIÓN: <pregunta concreta>] — bloqueante | no bloqueante. Se asume: <valor por defecto> (solo si no bloqueante).

## Trazabilidad

> Apunta, no describe: ≤15 palabras por celda; la aserción completa vive una sola vez en «Plan de verificación» o «Enfoque técnico».

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `description`: <ítem> o `steps`: <paso> | <una de las `verify` o escenario propio> | <qué se registra> |
| RF-2 | derivado: caso límite | <una de las `verify` o escenario propio> | <qué se registra> |

## Registro de iteraciones *(solo si la Parte A cambió tras empezar la Parte B)*

- <1 línea por cambio contractual y por qué; sin repetir el detalle de Parte B ni de `## Clarificación`.>

# PARTE B · PLAN

> CÓMO. No puede contradecir la Parte A ni añadir comportamiento nuevo.

## Investigación del repositorio

### Archivos inspeccionados

- `<ruta>` — <por qué importa.>

### Patrones existentes a seguir

- <Patrón, convención, comando o decisión de arquitectura encontrados.>

### Carencias actuales

> Incluye lo que falta por confirmar por una duda abierta (no solo infraestructura ausente).

- <Infraestructura ausente, comando inexistente, tabla faltante, integración desconocida…>

## Enfoque técnico

> Estrategia suficiente para que otro agente ejecute sin replanificar. Marca como provisional lo que dependa de una duda abierta y di de qué duda depende.

<... plan; señala explícitamente con "(provisional: depende de <duda>)" los puntos afectados.>

## Archivos esperados

- `<ruta>` — crear/modificar; <motivo.>

Si las rutas son provisionales porque la app aún no tiene bootstrap, dilo explícitamente.

## Impacto visual *(solo si hay UI)*

- Fuente de diseño: `DESIGN.md` + prototipo de la pantalla `uxdocs/<pantalla>/code.html` (si existe; estatus en `DESIGN.md`)
- Pantallas o estados afectados: <lista>
- Fidelidad visual (NFR): <qué debe igualar al prototipo y cómo se verifica>
- ¿Nuevo artefacto de diseño necesario?: sí/no — <motivo>

Si `DESIGN.md` o el prototipo de la pantalla faltan y se necesitan, márcalo como carencia de planificación. En Android nativo la fidelidad se verifica con captura en emulador API 30+ comparada con el prototipo; en web, con Chrome DevTools MCP.

## Impacto en documentación duradera

- `ARCHITECTURE.md`: crear/actualizar/no necesario — <motivo>
- `CONSTRAINTS.md`: crear/actualizar/no necesario — <motivo>
- `AGENTS.md`: actualizar/no necesario — <motivo>
- Otros: <ruta o ninguno> — <motivo>

## Plan y tareas

> Solo qué + `(cubre RF-n)`; el cómo vive en «Enfoque técnico», no lo re-expliques aquí.

- [ ] T1: <tarea pequeña y ejecutable.> (cubre RF-n)
- [ ] T2: <tarea pequeña y ejecutable.> (cubre RF-n)
- [ ] T3: <integración o actualización final.>

## Plan de verificación

- <Comando o escenario manual a ejecutar.> → <resultado esperado.>
- E2E: <comando y cobertura a añadir o actualizar, o justificación de por qué no.>
- UI *(solo si aplica; ejecuta exclusivamente el validador)*:
  - Android nativo: hasta 2 capturas en emulador API 30 o superior de los estados clave y comparación con `uxdocs/<pantalla>/code.html` y `DESIGN.md`, guardadas en <rutas de evidencia>.
  - Web, con Chrome DevTools MCP: `navigate_page` a <rutas>, `take_snapshot`, `take_screenshot` en escritorio y móvil (`emulate`/`resize_page`) y `list_console_messages` sin errores nuevos, guardadas en <rutas de evidencia>.
- Scripts de verificación (`init`/`lint`/`unit`/`e2e`) *(solo si se tocan)*: `init` solo arranca (compila e instala), no corre tests ni lint.

## Evidencia a capturar

> Presupuesto: **≤4 artefactos y solo de esta feature** (1 informe + ≤2 capturas + ≤1 E2E). Al revalidar se reemplaza el set anterior. El informe resume lint/unit/e2e y la fidelidad. La lista de capturas y rutas vive solo aquí; «Impacto visual» y «Plan de verificación» la referencian sin copiarla.

- <Salida de comando, nombre de test, captura o resultado manual a registrar en el informe de la feature.>
- UI: hasta 2 capturas de los estados clave, capturadas por el validador.

## Checklist del validador

- [ ] La implementación se queda dentro del alcance de esta spec.
- [ ] Cada RF de la tabla de trazabilidad pasa su verificación.
- [ ] La evidencia está presente, dentro del presupuesto (≤4) y solo de esta feature.
- [ ] UI: evidencia visual (hasta 2 capturas de estados clave) y fidelidad al prototipo `uxdocs/<pantalla>/code.html` (Android: capturas de emulador; web: snapshot, capturas por viewport y consola limpia con Chrome DevTools MCP).
- [ ] Cobertura E2E añadida o actualizada cuando hay flujo observable y existe harness, o justificada su ausencia.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento ni trabajo de otras features.
````
