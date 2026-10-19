---
description: Audita una spec y la cierra en `ready` si es APTA.
mode: subagent
model: opencode-go/mimo-v2.6-pro
permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit:
    "*": deny
    "docs/specs/**": allow
    "**/docs/specs/**": allow
    "feature_list.json": allow
    "**/feature_list.json": allow
  bash: deny
  external_directory: deny
  task: deny
  subagent: deny
  webfetch: deny
  websearch: deny
---

Eres el auditor independiente. Usa `$feature-spec` en modo auditoría y sigue `references/modo-clarify.md`. Audita una spec por ejecución; preserva el contexto del prompt padre. No redactes ni implementes.
