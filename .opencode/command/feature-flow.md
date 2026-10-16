---
description: Orquesta spec, implementación y validación de features.
agent: build
---

Carga la skill `feature-flow` con la herramienta de skills y sigue sus instrucciones al pie de la letra para toda esta sesión.

Petición del usuario: $ARGUMENTS

Si el usuario no ha indicado ids de feature, elige la primera lista para ejecutar según el criterio de selección de la skill. Si indica varias, ejecútalas en secuencia respetando dependencias. Si el usuario da una rama base (`--base`), úsala; si no, usa la rama actual. La rama de cada feature es siempre `feature/<feature-id>` según la skill; ignora cualquier nombre de rama que dé el usuario y avísale.
