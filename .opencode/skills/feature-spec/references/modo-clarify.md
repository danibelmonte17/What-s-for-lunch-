# Modo auditoría · clarify

Audita estáticamente una spec en borrador, escrita por el modo redacción. No planifiques ni implementes. El contador es el número de entradas resueltas `veredicto: A RETRABAJAR` en `## Clarificación` (excluye ejemplos).

## Límites y preflight

- Con id explícito, audita esa feature. Sin id, recorre `feature_list.json` en orden y elige la primera feature elegible (`not_started`, o `in_progress` si el prompt confirma que está bloqueada por implementer) que tenga spec en `draft`/`needs-clarification`, dependencias satisfechas y menos de 3 `A RETRABAJAR`. Si no hay candidata, repórtalo al padre.
- Lee Parte A completa, las partes de la Parte B necesarias para auditar gates, solo la entrada de la feature en `feature_list.json` y las filas `Q-<feature-id>` del registro. En intentos 2+, compara los cambios con hallazgos previos; usa un diff si el padre lo proporciona, sin requerir comandos de shell.
- Para conflictos externos, consulta `docs/build-brief.md` (líneas rojas) y, solo cuando puedan contradecir la spec, `docs/constitution.md`, `docs/adr/*.md`, specs previas y `docs/risks-and-open-questions.md`; localiza secciones con grep y lee ventanas pertinentes.
- Con 3 o más `A RETRABAJAR`, no audites ni escribas: reporta bloqueo para intervención humana. Sin spec, reporta al padre. Una feature por ejecución.
- No ejecutes verificaciones, scripts, emulador, `adb` ni builds. No toques código ni `PROGRESS.md`.

## Revisión y veredicto

Audita Gate A y Gate B (`gates.md`) y clasifica hallazgos en estos seis bloques:

1. Ambigüedades: RF no atómicos, adjetivos no medibles o supuestos silenciosos.
2. Contradicciones: entre requisitos o entre Parte A y B.
3. Casos límite no cubiertos: errores, vacíos, duplicados, red, permisos.
4. Conflictos externos: constitución, ADR, specs previas y líneas rojas del build brief.
5. Formato/trazabilidad: EARS, verificabilidad, cobertura, deriva respecto al backlog y «Fuera de alcance» vacío.
6. Dudas bloqueantes sin resolver: una aclaración marcada bloqueante que sigue `abierta` en spec/registro bloquea por falta de respuesta humana; no la presentes como defecto de redacción. Dudas no bloqueantes con valor asumido no bloquean.

Numera cada hallazgo y clasifícalo `bloqueante` (el implementador tendría que inventar o el validador no podría juzgar) o `menor`. Sin bloqueantes: `APTA`. Con alguno: `A RETRABAJAR` y feedback accionable. Con el tercer intento consumido: bloqueo para intervención humana. En el encabezado, N = entradas previas `A RETRABAJAR` + 1.

## Escritura y cierre

No reescribas la spec. Solo crea/anexa `## Clarificación` al final; nunca alteres Parte A/B, dudas, trazabilidad, checkboxes ni entradas anteriores. Formato:

```md
## Clarificación — AAAA-MM-DD — veredicto: APTA | A RETRABAJAR (intento N de 3)

### Hallazgos
1. [bloque/contradicción, bloqueante] RF-X, sección …: … → resolver en redacción.
2. [bloque, menor] RF-Y: … → observación.

### Siguiente acción
Retrabajar la spec (A RETRABAJAR) o continuar (APTA).
```

Con APTA, la lista puede estar vacía o contener menores. Solo entonces cambia `status: ready` en la cabecera y el `status` de la feature a `ready`, si su estado era `not_started` o una `in_progress` bloqueada por implementer. Nunca modifiques otros campos, ni rebajes `ready`, `passing` o `validated`; valida que `feature_list.json` sigue siendo JSON válido. Con `A RETRABAJAR`, no cambies estados.

Reporte al padre en ≤8 líneas: feature/spec; intento; recuento de hallazgos por severidad/bloque; veredicto; dudas bloqueantes abiertas; cierre `ready` si APTA; hallazgos bloqueantes con RF/sección si retrabajar.

Checklist: intentos contados; seis bloques y gates revisados; dudas bloqueantes abiertas bloquean; solo se anexó Clarificación; estado cambiado solo con APTA y desde estado válido; JSON válido; sin código/PROGRESS; una feature.
