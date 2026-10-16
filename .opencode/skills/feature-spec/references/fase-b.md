# Fase B · Plan

Reglas específicas de la Parte B. Cárgala antes de redactar el plan (paso 2 de la Fase B de la skill).

- **Verificación visual (obligatoria si hay UI).** Si la feature cambia una pantalla, estado o estilo visible, el plan de verificación debe exigir evidencia visual (no `curl` ni afirmaciones verbales), y «Impacto visual» debe citar el prototipo de la pantalla (`uxdocs/<pantalla>/code.html`) además de `DESIGN.md`, nombrando pantallas/estados y rutas de evidencia. La evidencia visual se acota a **hasta 2 capturas** de los estados clave (presupuesto total ≤4 artefactos, solo de esta feature). La ejecución es exclusiva del validador: ni planner ni implementador la ejecutan. En Android nativo (este proyecto) la fidelidad se comprueba con capturas en emulador API 30 o superior comparadas con el prototipo; en web, con Chrome DevTools MCP (`navigate_page`, `take_snapshot`, `take_screenshot` en escritorio y móvil y `list_console_messages` sin errores nuevos).
- **E2E persistente.** Si existe un comando E2E y la feature cambia comportamiento visible, autenticación, autorización, rutas o flujos de API, el plan debe añadir o actualizar cobertura E2E acotada e incluir el comando, o justificar por qué bastan unit/integración.
- **Scripts de verificación.** Si la feature toca los scripts del harness, especifica el efecto en `init` (arranque: compila e instala/abre la app), `lint` (estático), `unit` (unitarios) y `e2e` (extremo a extremo). Regla por defecto: `./init.sh` solo arranca (no corre tests ni lint) y no arranca procesos largos; las comprobaciones de calidad viven en `lint`/`unit`/`e2e`.
- **Si falta `DESIGN.md` y la UI lo necesita,** márcalo como carencia de planificación (planning gap) en «Impacto visual»; no dejes que el implementador invente un estilo.
- **Incógnitas y plan provisional.** Una duda de dominio nunca bloquea la Parte B. Plasmas cada incógnita en tres sitios: «Carencias actuales» (qué falta por confirmar), el «Enfoque técnico» marcado como provisional en los puntos que de ella dependen, y «Fuera de alcance» (lo que queda condicionado a la respuesta). Las incógnitas deben ser explícitas y no esconder ambigüedad bloqueante.
- **Impacto en documentación duradera.** Marca cada documento como crear, actualizar o no necesario, con motivo:
  - `AGENTS.md`: solo si cambian el flujo de agentes, el arranque o las reglas operativas globales.
  - `ARCHITECTURE.md`: si la feature establece o cambia dominios, capas, superficies de ejecución, dirección de dependencias, adaptadores o proveedores.
  - `CONSTRAINTS.md`: si introduce una regla duradera DEBE / NO DEBE para futuros agentes.
  - No dupliques comportamiento de producto ya recogido en los docs de descubrimiento, salvo que la implementación cambie la fuente de verdad.
