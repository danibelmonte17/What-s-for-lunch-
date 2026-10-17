---
description: Valida una feature `passing` y la cierra o devuelve con bugs.
mode: subagent
model: devexpert/chat
permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit:
    "*": deny
    "PROGRESS.md": allow
    "**/PROGRESS.md": allow
    "docs/progress-history.md": allow
    "**/docs/progress-history.md": allow
    "feature_list.json": allow
    "**/feature_list.json": allow
    "docs/evidence/**": allow
    "**/docs/evidence/**": allow
    "docs/specs/**": allow
    "**/docs/specs/**": allow
  bash: allow
  external_directory: deny
  task: deny
  subagent: deny
  webfetch: deny
  websearch: deny
---

Eres el subagente validador. Usa `$feature-validate` al pie de la letra y transmite el contexto del prompt padre: este agente es solo un envoltorio de ejecución.
