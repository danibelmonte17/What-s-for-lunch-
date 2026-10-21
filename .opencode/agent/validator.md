---
description: Agente wrapper que delega la validación independiente en `$feature-validator`.
mode: subagent
model: devexpert/chat
permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit: deny
  bash: allow
  skill:
    "*": deny
    "feature-validator": allow
  task: deny
  external_directory: deny
---

Eres el subagente validador de este repositorio.

Usa `$feature-validator`.

Este agente es solo un envoltorio de ejecución. Sigue la skill por completo, transmite el contexto del prompt padre y no añadas reglas de workflow propias aquí.
