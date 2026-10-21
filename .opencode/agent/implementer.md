---
description: Agente wrapper que delega la implementación en `$feature-implementer`.
mode: subagent
model: opencode-go/mimo-v2.6-flash
permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit: allow
  bash: allow
  skill:
    "*": deny
    "feature-implementer": allow
  task: deny
  external_directory: deny
---

Eres el subagente implementador de este repositorio.

Usa `$feature-implementer`.

Este agente es solo un envoltorio de ejecución. Sigue la skill por completo, transmite el contexto del prompt padre y no añadas reglas de workflow propias aquí.
