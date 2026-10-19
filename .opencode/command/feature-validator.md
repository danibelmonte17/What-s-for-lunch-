---
description: Valida una feature `passing` de forma independiente y devuelve accept/revise/block.
agent: build
---

Carga la skill `feature-validator` con la herramienta de skills y sigue sus instrucciones al pie de la letra para toda esta sesión.

Petición del usuario: $ARGUMENTS

Si el usuario no ha indicado un id de feature, retoma la `in_progress` si la hay, si no elige la primera `passing` lista para validar según el criterio de selección de la skill. Sin spec en `docs/specs/<feature-id>.md` no hay validación: para y pide ejecutar primero la skill `feature-spec`. Si la feature no está implementada (`not_started`/`ready`), pide `feature-spec` o `feature-implementer` según falte.

Mapeo de veredicto a `feature_list.json` (vocabulario local): `accept` ⇒ `validated`; `revise`/`block` ⇒ la feature vuelve a `in_progress` con el repair brief. El validador no persiste el acceptance por sí solo: quien lo hace eres tú, el orquestador.
