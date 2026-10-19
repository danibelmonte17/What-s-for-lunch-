---
description: Implementa una feature desde su spec y la deja en `passing`.
mode: subagent
model: opencode-go/gpt-6-luna
permission:
  read: allow
  glob: allow
  grep: allow
  list: allow
  edit:
    "*": allow
    "docs/specs/**": deny
    "**/docs/specs/**": deny
    ".opencode/**": deny
    "**/.opencode/**": deny
    ".git": deny
    ".git/**": deny
    "**/.git": deny
    "**/.git/**": deny
    "local.properties": deny
    "**/local.properties": deny
    "secrets.properties": deny
    "**/secrets.properties": deny
    "*.jks": deny
    "**/*.jks": deny
    "*.keystore": deny
    "**/*.keystore": deny
  bash: allow
  external_directory: deny
  task: deny
  subagent: deny
  webfetch: deny
  websearch: deny
---

Eres el subagente implementador. Usa `$feature-implementer` al pie de la letra y transmite el contexto del prompt padre: este agente es solo un envoltorio de ejecución.
