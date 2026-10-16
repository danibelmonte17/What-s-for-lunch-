# Modo redacción · feature-spec

Usa para crear, actualizar, continuar o retrabajar la spec de UNA feature. Modos: crear/actualizar (A → B), continuar una `draft`/`needs-clarification` o responder hallazgos de auditoría. No ejecutes la auditoría al terminar; el flujo la delega a `agent/clarify` en una sesión independiente.

## Selección y entradas

Con id explícito, acepta `not_started` o una `in_progress` bloqueada por implementer; `ready` solo se rehace con petición explícita. Sin id, selecciona la primera `not_started` en orden con campos `id`, `category`, `description`, `depends_on`, `steps` y `verify` completos y dependencias satisfechas (cada id existe y está `validated`; si falta `depends_on`, usa `[]`). Prefiere una con aclaraciones pendientes. Salta y reporta las bloqueadas; si la entrada seleccionada está incompleta, para y enumera los campos.

Lee `AGENTS.md` y cabecera de `PROGRESS.md`; `feature_list.json`; `docs/constitution.md` y solo las specs previas relevantes; además de los documentos de contexto que la feature exija (`CONTEXT.md`, `docs/build-brief.md`, `docs/domain-model.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/adr/*.md`, `DESIGN.md`). Lee la spec existente en retrabajo. Archivos de app solo durante Fase B. Usa `grep`/`glob` y ventanas de hasta 80 líneas.

## Fase A · Contrato

Carga `fase-a.md` y `spec-template.md` antes de redactar.

1. Deriva RF EARS de `description` y `steps`, con trazabilidad de origen.
2. Identifica casos límite, errores, permisos y alcance. Clasifica cada duda como bloqueante (cambia lo que se construye, sin valor por defecto razonable) o no bloqueante (asume explícitamente un valor obvio).
3. Descarta dudas obvias. Con humano, pregunta las restantes de una en una o en una llamada interactiva (máximo 6 por ronda; una segunda ronda si quedan). Recomienda como primera opción el valor asumido en las no bloqueantes. No propongas soluciones técnicas. En ejecución autónoma, no preguntes: deja dudas marcadas y enuméralas en el reporte.
4. Redacta todas las secciones de la Parte A, incluyendo «Fuera de alcance» y trazabilidad. Registra cada `[NECESITA ACLARACIÓN: …]` tanto en la spec como en `docs/risks-and-open-questions.md`, siguiendo `fase-a.md`.
5. Pasa el Gate A de `gates.md`; corrige hasta 3 iteraciones. Si aún falla, conserva las incógnitas explícitas, continúa con la spec y reporta el gap.

Las dudas de dominio no detienen la spec: continúa con la Parte B provisional. Si hay dudas bloqueantes, usa `status: needs-clarification` y refléjalas en «Carencias actuales», «Enfoque técnico» (provisional) y «Fuera de alcance». Solo detente por entrada de backlog incompleta.

Al recibir respuestas, conserva la fila y márcala `resuelta` con respuesta; actualiza el resumen, Parte A y puntos provisionales de Parte B. Sin bloqueantes abiertas, el estado vuelve a `draft`.

## Fase B · Plan

Solo tras Gate A. Carga `fase-b.md` antes de redactar.

1. Investiga lo necesario para que el plan sea ejecutable sin replanificar: framework/estado, rutas, componentes, módulos, tests, scripts, convenciones, modelos/migraciones, límites API, auth, comandos de verificación y E2E persistente si aplica. Inspecciona tantos archivos como requiera el trabajo, sin lecturas innecesarias. No registres como carencia lo que puedas resolver leyendo el repo. Anota archivos inspeccionados y carencias reales de infraestructura.
2. Redacta enfoque técnico, archivos esperados (marca provisionales en apps sin bootstrap), tareas ejecutables y ordenadas, verificación, evidencia y checklist del validador.
3. Si la investigación contradice la Parte A (inviable o realidad existente distinta), actualízala, repite Gate A y registra la iteración. La Parte B nunca contradice la A.
4. Pasa Gate B de `gates.md` y corrige hasta aprobar.

## Cierre, seguridad y salida

Escribe dudas en `docs/risks-and-open-questions.md` y deja `status: draft` (o `needs-clarification` con dudas bloqueantes). Nunca cambies `feature_list.json` ni escribas `ready`. En retrabajo, resuelve cada hallazgo bloqueante con cambio trazado o réplica justificada y conserva las entradas anteriores de `## Clarificación`. Solo escribe `docs/specs/<id>.md` y las filas de dudas; nunca borres ni reordenes contenido existente del registro.

Reporte en ≤8 líneas: feature/spec; nº RF e iteraciones Gate A; dudas bloqueantes/no bloqueantes y supuestos; preguntas humanas pendientes; riesgos clave; estado sin cambios en `feature_list.json`; siguiente acción: auditoría independiente.

`example-spec.md` es opcional y se carga solo si hace falta calibrar el nivel de detalle.
