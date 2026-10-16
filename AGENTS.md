# Reglas para el agente de codificación

## 1. Qué leer
Al empezar cada sesión, en este orden:
1. `CONTEXT.md` y `docs/build-brief.md`: producto, MVP, qué queda fuera y líneas rojas.
2. Este archivo.
3. `PROGRESS.md`: estado verificado y siguiente paso. Lee solo su cabecera; el Historial completo vive en `docs/progress-history.md` y no se lee entero (como mucho su última entrada si necesitas contexto).
4. `feature_list.json`.
5. `git log --oneline -20`.

Consulta estos solo si la funcionalidad elegida lo requiere, para no gastar contexto en lo que no toca:
- La spec de la funcionalidad elegida en la carpeta `docs/specs/`: léela siempre antes de implementarla.
- `DESIGN.md`: si toca interfaz.
- `docs/domain-model.md`: si toca datos o reglas de negocio.
- `docs/technical-discovery.md`: si toca decisiones de stack, integraciones o herramientas.
- `docs/risks-and-open-questions.md`: ante una ambigüedad, antes de decidir por tu cuenta.

Después ejecuta `./init.sh` (Linux/macOS) o `./init.ps1` (Windows). Si la verificación falla, arregla el entorno antes de tocar ninguna funcionalidad: construir sobre un entorno roto esconde los errores. La calidad se comprueba con scripts aparte: `lint` (estático), `unit` (tests rápidos) y `e2e` (extremo a extremo).

## 2. Cómo trabajar
- Si hay una funcionalidad `in_progress`, continúa con ella. Si no, elige la primera `ready` cuyas dependencias estén `validated`, lee su spec y cambia solo su `status` a `in_progress`; si no hay, elige la primera `not_started` lista, redacta con `feature-spec` y pide una auditoría independiente con APTA/A RETRABAJAR (alterna planner → auditor hasta 3 revisiones). Trabaja en una sola.
- Avanza de forma incremental. No empieces otra funcionalidad hasta cerrar esta.
- Usa rutas absolutas al invocar herramientas.
- Respeta las líneas rojas de `docs/build-brief.md`. Ante la duda, para y pregunta.
- Si no puedes terminar la funcionalidad, déjala `in_progress` y anota en `PROGRESS.md` dónde te quedaste.

## 3. Cuándo algo está hecho
Cada funcionalidad avanza por cinco estados: `not_started`, `ready`, `in_progress`, `passing` y `validated`.
- `ready`: especificación y plan aprobados por auditoría independiente (APTA en `## Clarificación`); todavía no hay implementación.
- `passing`: está implementada y `lint` + `unit` pasan.
- `validated`: se han ejecutado todas sus comprobaciones de `verify` en el entorno real (emulador o dispositivo Android) y pasan. No vale la autopercepción del modelo: usa las pruebas instrumentadas `e2e.sh` / `e2e.ps1` y, para comprobaciones puntuales, `adb` con uiautomator.
- `status` es el único campo de `feature_list.json` que puedes modificar. Avanza en orden; si una verificación falla, vuelve a `in_progress`. Si no puedes verificar, déjala en `passing` y anota el motivo en `PROGRESS.md`. No borres, reescribas ni renombres funcionalidades, pasos o verificaciones.
- La sesión está cerrada cuando:
  1. El arranque (`init.sh` / `init.ps1`) sigue pasando y, si tocaste código, `lint` y `unit` están en verde.
  2. `PROGRESS.md` está al día: reescribe *Estado verificado* y *Siguiente paso* y añade una entrada al Historial en `docs/progress-history.md` (sin borrar las anteriores; no lo leas entero).
  3. No quedan procesos que tú arrancaras ni archivos temporales; `.harness/` guarda solo registros del arranque.
