---
name: feature-spec
description: Planifica, redacta, retrabaja, audita o revisa la spec de UNA feature de feature_list.json. Sin id, elige la primera lista según el modo. Úsala para preparar trabajo de implementación o aprobar una spec.
---

# Feature Spec

Una feature por ejecución. Despacha por petición/contexto: **redactar/retrabajar** → `references/redaccion.md`; **auditar y emitir APTA/A RETRABAJAR** → `references/modo-clarify.md`; **revisar sin escribir** → `references/modo-revision.md`. Si el modo no es obvio, infiérelo del prompt padre; no ejecutes una auditoría al terminar de redactar.

## Reglas comunes

- Sin `feature_list.json`: para y pide ejecutar primero la skill de arranque del harness.
- Una feature por ejecución; no implementes ni modifiques código de aplicación.
- Documentación y comunicación en el idioma de `docs/constitution.md` si existe; si no, el del usuario. Código e identificadores en inglés.
- **Lectura económica:** usa `grep` + ventanas de hasta 80 líneas; `AGENTS.md`/`PROGRESS.md`, solo cabecera; contexto solo si la feature lo requiere.
- Escrituras según modo: redactar solo spec + dudas en `docs/risks-and-open-questions.md`; auditar solo anexa `## Clarificación` y, con APTA, cambia estado de spec y feature; revisar no escribe.

Las rutas son relativas a la raíz del repo/worktree, no a la carpeta de la skill. Si se pide modo redacción, carga su referencia y solo las referencias de fase necesarias; modo auditoría usa sus referencias específicas. El detalle de cada modo vive fuera de esta skill para evitar cargar instrucciones ajenas a la tarea.
