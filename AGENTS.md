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
- Si hay una funcionalidad `in_progress`, continúa con ella. Si no, elige la primera de `feature_list.json` que no esté ni `passing` ni `accepted` y cuyas dependencias estén `accepted` (una dependencia `passing` solo cuenta si hay evidencia de aceptación independiente en `docs/validations/`, `feature_list.json` o `PROGRESS.md`). Trabaja en una sola.
- El flujo por funcionalidad es: `feature-spec` (spec en `docs/specs/<feature-id>.md`) → `feature-implementer` (implementa y autoverifica: queda en `passing`) → `feature-validator` (validación independiente: accept/revise/block) → `accepted`. Sin spec, empieza por `feature-spec`; con spec ya redactada, salta a `feature-implementer`.
- Avanza de forma incremental. No empieces otra funcionalidad hasta cerrar esta.
- Usa rutas absolutas al invocar herramientas.
- Respeta las líneas rojas de `docs/build-brief.md`. Ante la duda, para y pregunta.
- Si no puedes terminar la funcionalidad, déjala `in_progress` y anota en `PROGRESS.md` dónde te quedaste.

## 3. Cuándo algo está hecho
Cada funcionalidad avanza por cuatro estados — `not_started`, `in_progress`, `passing` y `accepted` — más `blocked` si algo la impide. (Vocabulario del harness anterior: `ready` = hoy `not_started` con spec redactada; `validated` = hoy `accepted`.)
- `not_started`: sin implementar; puede o no tener spec en `docs/specs/`.
- `in_progress`: en implementación; máximo una a la vez.
- `passing`: está implementada y `lint` + `unit` pasan. **No es aceptación**: solo indica que queda lista para la validación independiente.
- `accepted`: una validación independiente (`feature-validator`) la ha aceptado y se han ejecutado todas sus comprobaciones de `verification` en el entorno real (emulador o dispositivo Android) y pasan. No vale la autopercepción del modelo: usa las pruebas instrumentadas `e2e.sh` / `e2e.ps1` y, para comprobaciones puntuales, `adb` con uiautomator.
- `status` y `evidence` son los únicos campos de `feature_list.json` que puedes modificar (el resto solo si el usuario pide mantenimiento de la lista). Avanza en orden; si una verificación falla, vuelve a `in_progress`. Si no puedes verificar, déjala en `passing` y anota el motivo en `PROGRESS.md`. No borres, reescribas ni renombres funcionalidades, pasos o verificaciones.
- La evidencia nueva de aceptación se guarda en `docs/validations/<feature-id>.md` y se resume en `evidence` de `feature_list.json`; la evidencia histórica de las features ya cerradas vive en `docs/evidence/<feature-id>/`.
- La sesión está cerrada cuando:
  1. El arranque (`init.sh` / `init.ps1`) sigue pasando y, si tocaste código, `lint` y `unit` están en verde.
  2. `PROGRESS.md` está al día: reescribe *Estado verificado* y *Siguiente paso* y añade una entrada al Historial en `docs/progress-history.md` (sin borrar las anteriores; no lo leas entero).
  3. No quedan procesos que tú arrancaras ni archivos temporales; `.harness/` guarda solo registros del arranque.

## 4. Convenciones

- Documentación, specs y textos de producto en español; código, identificadores y mensajes técnicos en inglés (`P19`, `P25`).
- Los textos de la interfaz viven en `app/src/main/res/values/` y `values-en/` (`strings.xml`, es/en); los términos de `CONTEXT.md` se usan tal cual en docs, UI y código.
- Commits en inglés con **Conventional Commits** (`feat:`, `fix:`, `docs:`…).
- La UI no contiene reglas de negocio: viven en `domain/` y en `docs/domain-model.md` (IDs `RD##`).
- Los comandos del proyecto (Gradle y scripts) están en `docs/technical-discovery.md`.

## 5. Reglas de trabajo

- Prohibido: comandos destructivos fuera del directorio del proyecto; escribir credenciales, tokens o PII en el repositorio, logs o documentos (nunca la API key de OpenRouter); commits sin Conventional Commits; modificar `uxdocs/` (solo lectura), `.opencode/` o los scripts del harness; tocar ficheros con secretos.
- Pedir confirmación humana antes de: instalar en un dispositivo físico, distribuir la APK, o ejecutar llamadas de pago a la API de IA.
- Si la tarea toca algo marcado `⚠` en `docs/risks-and-open-questions.md`, preguntar en vez de asumir.
- Si un cambio altera estructura, comandos, convenciones o términos, actualiza en el mismo cambio `CONTEXT.md`, `docs/technical-discovery.md` o este archivo, según corresponda.
- Antes de dar por terminada una tarea de código: `lintDebug` y `testDebugUnitTest` en verde y `assembleDebug` sin errores; si la feature lo exige, también la E2E (`connectedDebugAndroidTest`, con fixtures locales y cliente de IA simulado). El `status` de `feature_list.json` solo se mueve después de todo eso.
