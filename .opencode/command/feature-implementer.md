---
description: Implementa una feature desde su spec y la deja en `passing`.
agent: build
---

Carga la skill `feature-implementer` con la herramienta de skills y sigue sus instrucciones al pie de la letra para toda esta sesión.

Petición del usuario: $ARGUMENTS

Si el usuario no ha indicado un id de feature, retoma la `in_progress` si la hay, si no elige la primera `ready` lista para implementar según el criterio de selección de la skill. Sin spec en `docs/specs/<feature-id>.md` no hay implementación: para y pide ejecutar primero la skill `feature-spec`.
