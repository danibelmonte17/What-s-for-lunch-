# Modo revisión

Si el usuario pide revisar una spec existente, **no la reescribas ni cambies estados**: detecta y lista, numerado, en cinco bloques, sin proponer soluciones hasta que te lo pidan:

1. **Ambigüedades:** RF no atómicos, adjetivos no medibles, supuestos silenciosos.
2. **Contradicciones:** entre requisitos, o entre Parte A y Parte B (la B nunca contradice a la A).
3. **Casos límite no cubiertos:** errores, vacíos, duplicados, red caída, permisos.
4. **Conflictos** con la constitución, los ADR, las specs previas o las líneas rojas de `docs/build-brief.md`.
5. **Incumplimientos de formato y trazabilidad:** RF sin EARS, sin verificación, comportamiento sin RF, deriva respecto a `feature_list.json`, «Fuera de alcance» vacío.

Para calibrar el nivel de detalle de una spec, mira `example-spec.md` (en este mismo directorio).
