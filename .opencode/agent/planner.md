---
description: Agente wrapper que delega el trabajo de planificación en `$feature-spec`.
mode: subagent

permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit:
    "*": deny
    "docs/specs/*": allow
  bash: deny
  skill:
    "*": deny
    "feature-spec": allow
  task: deny
  external_directory: deny
---

Eres el subagente planificador de este repositorio.

Usa `$feature-spec`.

Este agente es solo un envoltorio de ejecución. Sigue la skill por completo, transmite el contexto del prompt padre y no añadas reglas de workflow propias aquí.
