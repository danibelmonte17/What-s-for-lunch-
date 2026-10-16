# Ejemplo de spec (ilustrativo)

Spec pequeña y completa para calibrar el nivel de detalle (el frontmatter lo define `spec-template.md`). Las rutas de la Parte B son inventadas. Fíjate en qué secciones se omiten por no aplicar (aquí no hay UI, ni NFR, ni E2E nuevo).

# PARTE A · CONTRATO

## Contexto y objetivo

Quien estudia necesita registrar un hábito nuevo para empezar a seguir su racha. Sin esta feature no existe nada que marcar ni contar.

## Actores y permisos

- Usuario autenticado: puede crear hábitos propios. Visitante no autenticado: no puede crear hábitos.

## Historia

Cuando quiero empezar a estudiar algo nuevo, quiero registrar un hábito con un nombre, para poder marcarlo cada día y ver mi racha.

## Requisitos funcionales

- RF-1: CUANDO un usuario autenticado envía un nombre válido, EL SISTEMA creará un hábito asociado a ese usuario y devolverá el hábito creado (código 201).
- RF-2: EL SISTEMA considerará válido un nombre de 1 a 60 caracteres una vez eliminados los espacios exteriores.
- RF-3: SI el nombre no es válido, ENTONCES EL SISTEMA no creará nada y devolverá un error de validación (código 400).
- RF-4: SI el nombre ya existe para ese usuario (ignorando mayúsculas y espacios exteriores), ENTONCES EL SISTEMA no creará un duplicado e informará del conflicto (código 409).
- RF-5: SI la petición no está autenticada, ENTONCES EL SISTEMA no creará nada y la rechazará (código 401).
- RF-6: CUANDO lleguen dos peticiones simultáneas con el mismo nombre para el mismo usuario, EL SISTEMA creará como máximo un hábito.

## Casos límite

- Solo espacios → RF-3. Distinta capitalización → RF-4. Peticiones simultáneas → RF-6.

## Fuera de alcance

- Editar, archivar o borrar hábitos; marcarlos como hechos y calcular la racha; cualquier pantalla.

## Criterios de finalización

Todos los RF con su verificación en verde y la evidencia registrada.

## Dudas abiertas

- [NECESITA ACLARACIÓN: ¿hay un máximo de hábitos por usuario?] — no bloqueante. Se asume: sin límite.

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `description`: "crear un hábito" | test de integración: alta válida | nombre del test, salida en verde |
| RF-2, RF-3 | `steps`: "nombres validados" | test unitario de validación | nombre del test |
| RF-4 | derivado: caso límite | test de integración: duplicado | nombre del test |
| RF-5 | `description`: "solo autenticados" | test de integración sin sesión | nombre del test |
| RF-6 | derivado: caso límite | test de concurrencia | nombre del test |

# PARTE B · PLAN

## Investigación del repositorio

- Inspeccionados: `src/habits/` (sin lógica de alta), `src/auth/session.ts` (patrón de sesión), `package.json` (hay `test`/`lint`; no `test:e2e`).
- Patrones a seguir: validación en capa de dominio separada del manejador HTTP; tests junto al módulo (`*.test.ts`).
- Carencias actuales: no existe la tabla de hábitos ni su migración.

## Enfoque técnico

Tabla de hábitos con índice único sobre usuario y nombre normalizado: la unicidad y la concurrencia (RF-4, RF-6) las garantiza la base de datos, no el código. La validación (RF-2, RF-3) vive en una función de dominio pura; el manejador orquesta: sesión → validación → inserción → traducción de errores a códigos.

## Archivos esperados

- `src/habits/migrations/001-create-habits.sql` — crear; tabla e índice único.
- `src/habits/validate-name.ts` — crear; validación pura.
- `src/habits/create-habit.ts` — crear; caso de uso.
- `src/habits/routes.ts` — modificar; registrar la ruta de alta.

Rutas provisionales hasta confirmar la estructura final del módulo.

## Impacto en documentación duradera

- `ARCHITECTURE.md`: no necesario. `CONSTRAINTS.md`: actualizar (unicidad en base de datos). `AGENTS.md`: no necesario. Otros: ninguno.

## Plan y tareas

- [ ] T1: migración con tabla e índice único normalizado. (RF-4, RF-6)
- [ ] T2: validación de nombre con tests unitarios. (RF-2, RF-3)
- [ ] T3: caso de uso de creación con traducción de errores de unicidad. (RF-1, RF-4, RF-6)
- [ ] T4: ruta de alta protegida por sesión. (RF-1, RF-5)
- [ ] T5: tests de integración y concurrencia. (RF-1 a RF-6)

## Plan de verificación

- `./unit.sh` (`pnpm test`) → tests de `src/habits/` en verde. `./lint.sh` (`pnpm lint`) → sin errores.
- E2E: no se añade; sin interfaz ni flujo de usuario, los tests de integración cubren la ruta.

## Evidencia a capturar

- Salida de `./unit.sh` filtrada a `habits`, registrada en el informe de la feature (≤4 artefactos, solo de esta feature).

## Checklist del validador

- [ ] Alcance de la spec respetado y cada RF de la trazabilidad pasa su verificación.
- [ ] Evidencia registrada; `feature_list.json` y `PROGRESS.md` correctos.
- [ ] No se añadió comportamiento de otras features (edición, racha, UI).
