# Gates de la spec

## Gate A · Contrato

- [ ] Cada RF es una frase EARS, atómica y verificable; sin adjetivos no medibles ni detalles de implementación.
- [ ] Todo comportamiento de `description`/`steps` tiene RF y origen trazable; errores y casos límite cubiertos.
- [ ] «Fuera de alcance» tiene contenido; no hay contradicciones entre RF ni con constitución, ADR o specs previas.
- [ ] Las incógnitas son explícitas; cada `[NECESITA ACLARACIÓN: …]` está también en `docs/risks-and-open-questions.md`.

## Gate B · Plan

- [ ] Implementador puede ejecutar sin este chat y validador puede juzgar el resultado.
- [ ] Archivos concretos (provisionales marcados si corresponde); tareas ordenadas cubren todos los RF.
- [ ] Verificación usa comandos existentes; E2E está añadido o justificado; verificación visual completa si hay UI (`fase-b.md`).
- [ ] Documentación duradera explícita; el plan no añade comportamiento fuera de Parte A.
- [ ] Cada duda aparece en «Carencias actuales», «Enfoque técnico» (provisional donde aplique) y «Fuera de alcance». Si UI necesita `DESIGN.md` ausente, declara planning gap; no inventes estilo.
- [ ] Sin duplicados (menor): ninguna aserción copiada en 3+ secciones; Trazabilidad con celdas ≤15 palabras; spec >170 líneas o con párrafos repetidos literales retrabaja por verbosidad.

## Gates para auditoría

Al auditar, comprueba Gate A y Gate B además de los seis bloques de `modo-clarify.md`. Los gates son criterios de salida: cada fallo bloqueante debe citar RF/sección y qué debe resolver planner.
