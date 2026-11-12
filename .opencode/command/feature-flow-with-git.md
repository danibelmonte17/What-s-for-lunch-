---
description: Ejecuta la skill feature-flow-with-git: una feature aislada en rama y worktree, terminada con PR.
agent: build
---

Carga la skill `feature-flow-with-git` con la herramienta de skills y sigue sus instrucciones al pie de la letra para toda esta sesión.

Petición del usuario: $ARGUMENTS

Si el usuario no ha indicado id de feature, aplica el criterio de selección de la skill: una sola feature por ejecución. Si da una rama base (`--base`), úsala; si no, resuélvela como indica la skill. La rama es siempre `feature/<feature-id>` y el worktree `<worktrees-root>/<feature-id>`: ignora cualquier nombre de rama o ruta distinto que dé el usuario y avísale. El permiso de push solo está concedido si el usuario lo pide explícitamente en esta petición (`--push`); sin eso, la ejecución se queda local y el informe lleva `pr: none`.
