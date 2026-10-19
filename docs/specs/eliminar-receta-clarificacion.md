# Clarificaciones anteriores · eliminar-receta

## Clarificación — 2026-10-19 — veredicto: APTA (intento 1 de 3)

### Hallazgos
1. [casos límite, menor] RF-6/RF-8: no se cubre el fallo parcial del borrado del fichero de foto tras eliminar la fila (foto huérfana o Receta ya sin fichero); residual porque `FilePhotoStore.delete` no lanza excepciones → observación: tratar el borrado de foto como best-effort.
2. [ambigüedad, menor] RF-6 «informará del error»: el copy del aviso no está fijado (el del diálogo sí, Q-eliminar-receta-1) → observación: a criterio del implementador, con cobertura NFR-1 es/en.
3. [contradicción, menor] §Impacto visual y §Checklist del validador: la comparación con el prototipo no excluye las desviaciones aprobadas («Eliminar» tonal error de Q-eliminar-receta-4, excepción de «Editar» de Q-editar-receta-6, diálogo no cubierto por el prototipo) → observación: acotar la comparación como en `editar-receta` NFR-4.
4. [formato/trazabilidad, menor] Q-eliminar-receta-4: cita la excepción de «Editar» sin remitir a `Q-editar-receta-6` (el registro sí lo hace) → observación de trazabilidad.

### Siguiente acción
Continuar (APTA): la spec queda `ready`; los menores son observaciones para implementación y validación.
