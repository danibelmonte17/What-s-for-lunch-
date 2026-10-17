# Fase A · Contrato

Reglas de redacción de la Parte A. Cárgala antes de redactar el contrato (paso 4 de la Fase A de la skill).

## Patrones EARS

Cada requisito va en **EARS**, numerado `RF-1`, `RF-2`, …, y es **verificable**: si no se te ocurre cómo comprobarlo, está mal escrito. **Un requisito, una frase** (si necesitas un «y» para unir dos comportamientos, son dos requisitos). **Sin adjetivos no medibles**: «rápido», «intuitivo», «robusto» no son requisitos; escribe el umbral o no lo escribas. Elige un patrón por requisito; no los mezcles.

| Patrón | Forma | Cuándo |
|---|---|---|
| Ubicuo | `EL SISTEMA <hará>` | siempre cierto |
| Dirigido por evento | `CUANDO <disparador>, EL SISTEMA <hará>` | responde a algo |
| Estado | `MIENTRAS <estado>, EL SISTEMA <hará>` | durante una condición |
| Opcional | `DONDE <característica>, EL SISTEMA <hará>` | solo si está presente |
| No deseado | `SI <condición>, ENTONCES EL SISTEMA <hará>` | errores y casos límite |

Ejemplo: `RF-4: SI el nombre ya existe (comparación ignorando mayúsculas y espacios exteriores), ENTONCES EL SISTEMA no creará un duplicado e informará del conflicto (código de error 409)`. Mal escrito: «manejar bien los duplicados y ser rápido» (sin patrón EARS, dos ideas en una frase, adjetivo no medible).

Los casos límite, los errores y las condiciones de concurrencia también son RF (patrón «No deseado» o «Evento»), no notas sueltas. La sección «Casos límite» los enumera y apunta al RF que los cubre.

## Trazabilidad

La tabla enlaza cada RF con su origen (campo de la entrada de `feature_list.json` —`description` o `steps`—, o «derivado: caso límite»), con cómo se verifica (una de las `verify` de la feature o `ver Plan de verificación`) y con la evidencia esperada. Ningún RF sin verificación; ningún comportamiento de `description` ni de `steps` sin RF. Celdas cortas (≤15 palabras): apunta, no describas la aserción completa.

## Registro de aclaraciones en `docs/risks-and-open-questions.md`

Toda duda `[NECESITA ACLARACIÓN: …]` se copia a «Preguntas abiertas» de `docs/risks-and-open-questions.md` (fuente única de lo que falta por confirmar), una fila por duda. Añádela al final de la tabla; si no existe, créala con su cabecera. No borres ni reordenes filas ni texto existentes.

| ID | Pregunta | Origen | Bloqueante | Estado |
|---|---|---|---|---|
| Q-<feature-id>-1 | <pregunta concreta> | `docs/specs/<feature-id>.md` | sí/no | abierta |
| Q-<feature-id>-2 | <pregunta concreta> | `docs/specs/<feature-id>.md` | no | abierta (se asume: <valor por defecto>) |

- `ID`: `Q-<feature-id>-<n>` secuencial dentro de la feature; nunca reutilices un ID.
- `Bloqueante`: `sí` si su respuesta cambia lo que hay que construir y no hay valor por defecto razonable; `no` en caso contrario.
- `Estado`: `abierta`; en las no bloqueantes añade `(se asume: …)`. Si se responde, no borres la fila: cámbiala a `resuelta` con la respuesta. Si una duda registrada cambia, actualiza su fila sin crear duplicados.
- Actualiza los recuentos del `Resumen` («Preguntas abiertas» y, si la fila es bloqueante, «Bloquean la construcción») al añadir o cerrar filas.
