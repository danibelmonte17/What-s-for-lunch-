---
name: feature-implementer
description: Implementa o retoma una feature desde su spec y la deja en `passing`. Sin id, elige la primera `ready` lista.
---

# Feature Implementer

Convierte la spec de UNA feature (`docs/specs/<feature-id>.md`) en código, siguiendo su Parte B al pie de la letra y sin añadir comportamiento fuera de su Parte A. No planificas ni reescribes specs: ejecutas lo que `feature-spec` ya definió.

## 1. Reglas de seguridad (única fuente)

- **Solo crear y editar archivos.** Prohibido ejecutar `gradlew` (incluidos `test`, `lint`, `assemble`), los scripts del harness (`init`/`lint`/`unit`/`e2e`), tests, builds, `adb` o emulador; prohibido marcar `validated`.
- **En `feature_list.json` solo modificas el `status` de esta feature** (nada más: ni reordenar, reformatear ni renombrar) y compruebas que el JSON sigue válido.
- **Prohibido:** secretos en repo/logs/documentos; comandos destructivos fuera del proyecto; cambiar stack, añadir red/sincronización o publicar sin confirmación humana; decidir por tu cuenta sobre algo marcado `⚠` en `docs/risks-and-open-questions.md` (pregunta en vez de asumir).
- **La Parte A manda; la Parte B nunca se reescribe.** Si una tarea contradice la realidad del repo, no la reinventes: deja la feature en `in_progress`, anota en `PROGRESS.md` dónde te quedaste y pide re-ejecutar `feature-spec` (retrabajará la spec y pasará de nuevo por auditoría). Nunca auto-corrijas el plan sobre la marcha.
- **Lectura económica:** `AGENTS.md` y `PROGRESS.md` solo cabecera (Historial en `docs/progress-history.md`: como mucho la última entrada); docs de contexto solo los que la feature exija; la spec completa solo en el primer intento — **en reintento con feedback de validator, lee solo las ventanas de los RF y archivos afectados que señala el informe** (localízalos con `grep -n '^#'` + `Read offset/limit`), no la spec entera ni el repo desde cero; archivos de la app solo los que indique la Parte B, con `grep` + ventanas.
- **Idioma:** documentación y comunicación en español (el del usuario); código, identificadores y mensajes de commit en inglés.

## 2. Procedimiento

1. **Selección:** con id del usuario; si no, la primera `ready` en orden con `depends_on` satisfechas (cada id existe y está `validated`; lista antigua sin `depends_on` = `[]`); retoma antes una `in_progress` si la hay. Un id `not_started` sin spec → para y pide `feature-spec` primero. Nunca rebajes `passing` ni `validated`.
2. **Apertura:** lee la spec (completa en primer intento; ventanas afectadas en reintento, ver §1) y cambia el `status` a `in_progress`.
3. **Tareas:** ejecuta en orden T1..Tn de la Parte B, cubriendo cada RF de la tabla de trazabilidad. Respeta las líneas rojas, los términos de `CONTEXT.md` y la arquitectura vigente (MVVM + Repository). «Ejecutar» aquí es solo crear y editar archivos: si alguna tarea pide verificar, compilar, testar o ejecutar, omítela y anótala como pendiente.
4. **Tope en `passing`:** sin ninguna validación ni comando de comprobación (§1); anota en `PROGRESS.md` que la validación real (las `verify` en emulador) queda pendiente de una sesión o persona aparte.
5. **Cierre, en este orden:** comprueba que `feature_list.json` sigue siendo JSON válido; cambia el `status` a `passing` (si no pudiste terminar, déjala en `in_progress` anotando en `PROGRESS.md` dónde te quedaste); actualiza `PROGRESS.md` (*Estado verificado*, *Siguiente paso*, *Riesgos y preguntas abiertas* si cambian) y añade una entrada al Historial en `docs/progress-history.md` (sin borrar las anteriores); sin archivos temporales sin versionar.

## 3. Salida

Reporta (≤8 líneas): Feature `<id>` — spec `docs/specs/<id>.md`; Tareas ejecutadas `T1..Tn`; RF cubiertos `<lista>`; Estado en `feature_list.json`: `passing` | `in_progress` (`<motivo>`); Pendiente de validación: `verify` en emulador (fuera de esta skill); Siguiente acción: validación manual en emulador por separado.

**Checklist:** leí la spec (completa en primer intento; solo ventanas afectadas en reintento); cada RF de la trazabilidad cubierto por una tarea ejecutada; cero comandos ejecutados (`gradlew`, scripts, tests, builds, `adb`); `status` escrito correctamente y JSON válido; `PROGRESS.md` e Historial actualizados; sin temporales sin versionar.

> Desviación consciente respecto a `AGENTS.md §3`: tope en `passing` sin ejecutar ninguna validación. Solo crear y editar archivos. Queda documentada aquí y en cada reporte final.
