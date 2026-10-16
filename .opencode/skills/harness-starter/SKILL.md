---
name: harness-starter
description: >-
  Prepara un proyecto especificado para ser implementado por sesiones: backlog
  verificable, reglas de agente, bitácora de progreso y scripts de arranque
  multiplataforma. Invocación manual únicamente: úsala solo cuando el usuario
  la pida por su nombre. No decide qué se construye ni escribe código de la
  aplicación.
disable-model-invocation: true
---

# Harness-Starter

Actúas como **arquitecto de software** centrado en el andamiaje operativo. No escribes código de la aplicación.

Un agente que implementa por sesiones no recuerda la anterior: solo persiste lo que está en disco. Por eso el harness son doce artefactos, cada uno con una función propia: el backlog, el estado y la historia, más los scripts de arranque y de verificación (dos plataformas cada uno). El estado verificado y el historial están separados para que las sesiones lean solo la cabecera y no arrastren todo el historial a su contexto. Los scripts separan responsabilidades: arrancar la app (`init`), comprobar el estilo (`lint`), correr los tests rápidos (`unit`) y la verificación de extremo a extremo (`e2e`). Si una regla vive en dos sitios acaban contradiciéndose, así que no las repitas entre archivos.

| Archivo | Función |
|---|---|
| `AGENTS.md` | Reglas: qué leer, cómo trabajar y cuándo algo está hecho |
| `feature_list.json` | Backlog: funcionalidades, dependencias y cómo verificar cada una |
| `PROGRESS.md` | Estado verificado y por dónde seguir (lo que se lee cada sesión) |
| `docs/progress-history.md` | Historial de sesiones, solo se añade; nunca se lee entero |
| `init.sh` / `init.ps1` | Arranque: instalar, preparar y dejar la app corriendo (Linux/macOS y Windows) |
| `lint.sh` / `lint.ps1` | Verificación estática (lint y, en web, tipos) |
| `unit.sh` / `unit.ps1` | Tests unitarios rápidos |
| `e2e.sh` / `e2e.ps1` | Tests instrumentados y de extremo a extremo (mobile: emulador/dispositivo; web: navegador) |

## 1. Reunir contexto

Lee los documentos de especificación del proyecto que existan: `CONTEXT.md`, `DESIGN.md`, `docs/build-brief.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md` y `docs/technical-discovery.md`. De ellos reúne tú la información que necesites para construir el harness.

No reabras el descubrimiento, no preguntes al usuario y no inventes nada: cada comando, puerto, ruta, funcionalidad y criterio de verificación del harness tiene que salir de los documentos. Un dato supuesto acabaría tratado como cierto por el agente que implementa.

Si falta algo que necesitas, o una pregunta abierta impide definir una funcionalidad o su verificación sin suponer, añádelo a `docs/risks-and-open-questions.md` (formato en `references/plantillas.md`, sección 9), sin borrar ni reescribir lo que ya contiene. Si hay faltas, no generes el harness: dile al usuario qué falta para que lo resuelva con `build-brief` y vuelva a ejecutar esta skill. Si falta `docs/build-brief.md`, no generes nada ni escribas nada y remite a `build-brief`.

## 2. Generar los artefactos

Lee `references/plantillas.md` (plantilla y criterios de cada archivo) y crea los doce artefactos: los cuatro de estado/backlog en la raíz más `docs/progress-history.md`, y los cuatro scripts de arranque y verificación en su par `.sh`/`.ps1`. Lo esencial:

- **`feature_list.json`**: es JSON y no Markdown para que un agente no pueda alterar el esquema ni borrar casos sin que se note. Antes de escribirlo lee `references/slicing.md`: cada funcionalidad es un slice del tamaño de una sesión, con un `id` que es un nombre único y legible (`registrar-cuenta`), `depends_on` hacia las anteriores de la lista, `steps` (qué implementar), `verify` (comprobaciones observables de extremo a extremo) y un `status` que nace en `not_started`. Cubre el MVP completo y nada de su lista de exclusiones.
- **`init.sh` / `init.ps1`**: arranque. Mismas etapas en ambos (instalar, preparar entorno, arrancar, verificar), mismos puertos y variables; solo cambia la sintaxis. En mobile instala y deja la app corriendo (Android: `assembleDebug` + `adb install` + `am start`); en web arranca el servidor. La etapa de verificación hace una comprobación real (health check, app instalada o artefacto generado) y falla con código distinto de 0. **No ejecuta tests ni lint.** Idempotentes, válidos desde un checkout limpio y con rutas resueltas desde la ubicación del propio script. En PowerShell, `$ErrorActionPreference` no cubre los comandos nativos: comprueba `$LASTEXITCODE` tras cada uno.
- **`lint.sh`/`lint.ps1`, `unit.sh`/`unit.ps1`, `e2e.sh`/`e2e.ps1`**: un script por tipo de verificación, con su par idéntico en `.sh` y `.ps1`. Cada uno corre el comando de su stack (Android: `lintDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`; web: eslint + tsc, vitest/jest, Playwright/Puppeteer), sale con código distinto de 0 al fallar y no arranca entorno. El comando sale de los documentos del proyecto; el `.sh` y el `.ps1` del mismo tipo usan el mismo comando.
- **`PROGRESS.md`**: la cabecera que lee cada sesión. Contiene *Estado verificado*, *Siguiente paso* y *Riesgos y preguntas abiertas*, que se reescriben al cerrar cada sesión y recogen solo lo comprobado, no lo que se cree, más un puntero corto al historial. Nunca crece sin control.
- **`docs/progress-history.md`**: el historial. Se arranca con la sesión 0 (estado inicial, riesgos y preguntas abiertas que afecten, y primera tarea recomendada) y a partir de ahí solo se añaden entradas de sesión, en orden, sin borrar las anteriores. Ninguna sesión lo lee entero.
- **`AGENTS.md`**: protocolo de trabajo, no contexto de producto (eso vive en `CONTEXT.md` y `docs/`). Tres bloques: qué leer, cómo trabajar y cuándo algo está hecho. La lectura se divide en lo que se lee siempre y lo que se consulta según la funcionalidad, para no gastar contexto en lo que no toca. Solo nombra archivos que existen en el proyecto.

## 3. Revisión post-flight

Ejecuta `python scripts/validar_harness.py <raíz-del-proyecto>`. Comprueba JSON y esquema, que todas las funcionalidades nacen en `not_started` (como mucho una `in_progress`), que los ids son nombres únicos y las dependencias válidas, que `init.sh` e `init.ps1` tienen las mismas etapas y puertos, que existen los cuatro pares de scripts (`init`/`lint`/`unit`/`e2e`) y pasan la comprobación de sintaxis, que no quedan marcadores sin rellenar ni credenciales, y que `AGENTS.md` no cita archivos inexistentes. Corrige y repite hasta que no haya errores. Los avisos de granularidad (descripciones que parecen unir varios comportamientos, menos de 10 o más de 30 funcionalidades) son para revisar: divide, fusiona o justifícalo en la sesión 0 de `docs/progress-history.md`.

Si no hay PowerShell disponible, el script avisa de que `init.ps1` no se pudo validar sintácticamente: revísalo a mano. Comprueba también a mano que `AGENTS.md` no contradice las líneas rojas de `docs/build-brief.md`.

## 4. Entrega

Resumen breve: archivos creados, cuántas funcionalidades hay en `feature_list.json` y primera tarea recomendada para la siguiente sesión.
