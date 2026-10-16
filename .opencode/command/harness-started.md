---
description: Ejecuta la skill harness-starter: genera el harness del proyecto (feature_list.json, PROGRESS.md, docs/progress-history.md, AGENTS.md y los scripts init/lint/unit/e2e en .sh y .ps1) a partir de la documentación de especificación.
agent: build
---

Carga la skill `harness-starter` con la herramienta de skills y sigue sus instrucciones al pie de la letra para toda esta sesión.

Petición del usuario: $ARGUMENTS

Si el usuario no ha indicado nada, trabaja sobre el directorio de trabajo actual. Si indica una raíz de proyecto distinta, genera y valida el harness ahí.
