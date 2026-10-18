---
name: feature-validate
description: Valida una feature `passing`, ejecuta sus verificaciones y la cierra o devuelve con bugs. Sin id, la primera lista.
---

# Feature Validate

Cierra el ciclo de UNA feature: convierte la implementación de `docs/specs/<feature-id>.md` en un veredicto comprobado en el entorno real. No implementas ni planificas: ejecutas la verificación que `feature-spec` definió y que `feature-implementer` dejó pendiente. **Decide con evidencia, nunca por autopercepción.**

## 1. Reglas de seguridad (única fuente)

- **Solo se valida lo implementado:** la feature está en `passing` (o retomas la `in_progress` que se estaba validando). Nunca valides `not_started` ni `ready`. Una feature por ejecución.
- **No tocas código.** Si encuentras un bug, no lo arreglas: lo demuestras, devuelves la feature a `in_progress` y derivas el arreglo a `feature-implementer`. En la spec solo puedes alternar los checkboxes (`- [ ]` ↔ `- [x]`) de los checks validados; nunca cambies su texto, contrato, plan ni añadas/quites ítems.
- **Estado congelado hasta el veredicto.** Arrancar no cambia el `status`: permanece `passing` mientras se valida y se escribe una sola vez, en el Cierre (§3.7): `validated` (todo en verde), `in_progress` (fallo) o sin cambio (no verificable). Prohibido pasarla a `in_progress` en el preflight. En `feature_list.json` solo modificas el `status` de esta feature.
- **La evidencia manda.** `validated` solo con verificaciones ejecutadas y su evidencia registrada; prohibido marcarlo «porque debería funcionar» o por leer el código. Nunca rebajes un `validated` (no revalides salvo petición explícita).
- **E2E solo si la Parte B lo pide.** El paso de emulador/`adb` (§3.3) se ejecuta únicamente cuando la Parte B incluye comprobaciones E2E en emulador; si no (p. ej. lógica de dominio verificable con tests/lint/build), omítelo por completo y **no** lo consignes como «no verificable». Ante duda, decide según la Parte B y documéntalo.
- **No verificable ≠ fallo.** Si una verificación exigida no puede ejecutarse (sin AVD, dispositivo, red o clave), deja la feature en `passing`, registra el bloqueo y da las instrucciones exactas para resolverlo. No la marques `validated` ni la rebajes.
- **Prohibido:** secretos o fotos reales en repo/logs/evidencia; comandos destructivos fuera del proyecto; cambiar stack, añadir red o publicar; decidir por tu cuenta sobre algo marcado `⚠` en `docs/risks-and-open-questions.md` (pregunta y reporta en vez de asumir).
- **Limpieza.** Mata los emuladores que hayas arrancado y deja el repo sin procesos de Gradle ni archivos temporales sin versionar.
- **Idioma:** español (el del usuario); el código, en inglés.

## 2. Entradas y lectura económica (única fuente; eres la skill que más archivos toca)

Lee por ventanas; nunca traigas a tu contexto lo que no necesites:

- `AGENTS.md` y solo la cabecera de `PROGRESS.md` (*Estado verificado*, *Siguiente paso*, *Riesgos*); el Historial vive en `docs/progress-history.md` (como mucho su última entrada).
- `feature_list.json`: solo la entrada de esta feature.
- La spec: Parte A completa —en modo delta (§3.0), solo las ventanas afectadas— y, de la Parte B, solo las secciones de verificación (*Plan de verificación*, *Evidencia a capturar*, *Checklist del validador*, *Archivos esperados*, *Impacto visual*, *Plan y tareas*). Localízalas con `grep -n '^#'` y léelas con `Read` `offset/limit`.
- Docs de contexto (`CONTEXT.md`, `DESIGN.md`, `docs/build-brief.md`, `docs/risks-and-open-questions.md` —sobre todo S06—, `docs/technical-discovery.md`) y el prototipo UX: solo si la spec o el tipo de verificación los requieren.
- `local.properties` solo si la Parte B pide E2E, y solo para resolver la ruta del SDK (nunca su contenido sensible).
- Archivos de la app: solo los que marque «Archivos esperados» (confirmar alcance).
- **Nunca `Read` de PNG, capturas ni logs enteros.** Las capturas se comprueban con `uiautomator` y muestreo de píxeles y quedan para revisión humana; la salida de `adb`/`gradlew` se filtra (§3.2).
- **En continuación de sesión, no releas lo ya leído:** lee solo el delta (informe de bugs previo, resumen de cambios del implementador y lo que citen) y las ventanas que necesites.

Rutas relativas a la raíz del repo (o al worktree del prompt padre), no a la skill.

## 3. Procedimiento

### 3.0 Modo: completa o delta

- **Completa (por defecto):** primera validación de la feature, o cualquier revalidación sin informe de bugs previo; recorre §3.1–§3.7 sin recortes.
- **Delta (reintento tras bug):** solo con informe de bugs previo y resumen de cambios del implementador. Verifica cada RF/check fallido y haz smoke de `init/lint/unit` con salida contenida; actualiza solo las filas afectadas del mismo informe y limita la fidelidad visual a las pantallas tocadas. **Válvula:** si el cambio toca archivos o comportamiento fuera de los RF fallidos, o falta el informe previo, usa la validación completa.

### 3.1 Preflight

Lee la spec por ventanas (§2) y comprueba que la feature está `passing` (sin modificar su `status` todavía). Verifica que el JSON es válido. Ejecuta `./init.sh` para confirmar que el entorno arranca y anota solo su código de salida y el resultado (§3.2).

### 3.2 Verificación estática y unitarios

Ejecuta y registra los códigos de salida: `./lint.sh` (o `lint.ps1`; Android: `./gradlew lintDebug`) y `./unit.sh` (o `unit.ps1`; Android: `testDebugUnitTest`, anota nº de tests/fallos).

**Salida contenida (regla de tokens).** No traigas la salida a tu contexto. Redirige a `.harness/` (gitignored) y lee solo lo imprescindible:

```
./lint.sh > .harness/lint.log 2>&1; echo "exit=$?"
./unit.sh > .harness/unit.log 2>&1; echo "exit=$?"
grep -nE "FAILED|error:|BUILD (SUCCESSFUL|FAILED)|tests? (completed|failed)" .harness/unit.log
```

El mismo criterio aplica a `init` y a cualquier comando de Gradle. Usa `--no-daemon` siempre que ejecutes Gradle desde la shell del agente y pre-arranca el daemon de `adb` antes de capturar su salida (detalle operativo en `references/e2e-android.md`). Si `lint` o `unit` fallan, es un fallo de validación: repórtalo como bug (§3.6).

### 3.3 E2E en emulador — solo si la Parte B lo pide

Ejecútalo **solo si la Parte B incluye comprobaciones E2E en emulador** (en *Plan de verificación*, *Evidencia a capturar* o *Checklist del validador*); si no, omite el paso completo (§1). Primero la suite instrumentada `./e2e.sh` (o `e2e.ps1`) con salida contenida (§3.2); después, cuando la Parte B lo pida, el recorrido manual guiado por `adb` siguiendo `references/e2e-android.md` (resolución de SDK, AVD válido API 30+, arranque headless, instalación y lanzamiento, fallos con `logcat` filtrado, interacción con `input` y límites de hardware no automatizable). Captura solo los estados clave (**≤2 PNG por feature**: `adb exec-out screencap -p > docs/evidence/<feature-id>/api<api>-<estado>.png`, con el API real del AVD; flujos largos: `screenrecord`, S06 acepta captura o vídeo; los PNG se versionan, los vídeos no). Sin ningún AVD válido: registra «no verificable», deja `passing` y entrega los comandos exactos para crear uno (`references/e2e-android.md`).

### 3.4 Comprobación del contrato (Parte A)

Construye la matriz de trazabilidad y rellénala con lo observado:

| RF | Verificación (de la spec) | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | … | pasa / falla / no verificable | ruta o extracto |

Un RF sin verificación en verde impide `validated`. Recorre la *Checklist del validador* y las tareas de *Plan y tareas*, anotando en la matriz qué puntos quedan en verde. **No marques los checkboxes todavía**: se hace en el Cierre (§3.7) y solo con veredicto `validated`.

**Fidelidad visual 1:1 (obligatoria si la Parte B declara UI).** Aplica `references/fidelidad-visual.md` para comparar el prototipo `uxdocs/<pantalla>/code.html` con la implementación atributo a atributo. Como el modelo no ingiere imágenes: comparación de clases HTML vs. código Compose, inventario `uiautomator` y muestreo de píxeles; las capturas quedan para revisión humana. Extensiones y estados no cubiertos por el prototipo solo se aceptan si están **declarados en la spec**. Registra la tabla de correspondencia dentro del informe único. Divergencia material (atributo ausente o visiblemente distinto, incluido el estado deshabilitado, el peso o la sombra) = **bloqueante**; matiz de acabado no exigido por la Parte A = no bloqueante.

**Presupuesto de evidencia.** `docs/evidence/<feature-id>/`: **máximo 4 artefactos y solo de esta feature** — 1 informe, ≤2 capturas PNG de estados clave y ≤1 artefacto E2E (XML de tests instrumentados o dump `uiautomator`) solo si la Parte B lo exige. Mantén el informe conciso (tablas > prosa), pero **sin límite fijo de líneas**: debe incluir los resultados de `init`/`lint`/`unit`/`e2e` aplicables, una fila por cada RF con resultado y evidencia, la tabla de fidelidad visual si aplica y el veredicto. No omitas ni agrupes requisitos de forma que impida comprobarlos para ajustarte a una longitud. Sin `logcat`, `lint-results` ni volcados sueltos (los extractos cortos van dentro del informe). Al revalidar se **reemplaza** el set anterior: sin acumular ni sufijos `-revalidation`/`-fidelityN`. Nada de recorridos por pantallas ajenas a la feature.

### 3.5 Comprobación de alcance

Con `git diff` frente al estado implementado, confirma: no hay comportamiento fuera de la Parte A ni trabajo de otras features; no hay secretos ni fotos reales en repo, logs o evidencia; la spec solo se ha modificado marcando checkboxes validados; `feature_list.json` y `PROGRESS.md` quedan coherentes.

### 3.6 Veredicto

| Resultado | `status` final | Acción |
|---|---|---|
| Todas las verificaciones en verde y alcance correcto | `validated` | Actualiza `PROGRESS.md` |
| Algún RF o check falla, o hay bug demostrable | `in_progress` | Feedback de bugs (§4) + actualiza `PROGRESS.md` |
| No verificable (sin AVD/dispositivo/red/clave) | `passing` | Registra el bloqueo y prerrequisitos en `PROGRESS.md` |

La fidelidad visual 1:1 forma parte de «todas las verificaciones»: una divergencia material no declarada impide `validated`. Con bug o fallo, entrega el feedback con el formato de §4; no arregles el bug ni propongas el parche: demuéstralo con evidencia y devuelve la feature al implementador.

### 3.7 Cierre (en este orden)

1. Comprueba que `feature_list.json` sigue siendo JSON válido.
2. Escribe el `status` final aquí, y solo aquí, una vez: `validated`, `in_progress`, o sin cambio si es no verificable (se mantiene `passing`).
3. Si el veredicto es `validated`, marca en la spec con `- [x]` los checks validados; si **no** es `validated`, no marques ninguno y desmarca (`- [ ]`) los que hubiera dejado una ejecución previa. Ese marcado/desmarcado es la única edición permitida en la spec.
4. Deja la evidencia en `docs/evidence/<feature-id>/` dentro del presupuesto (§3.4); el informe resume lint/unit/e2e y la fidelidad (los logs completos quedan en `.harness/`). Actualiza `PROGRESS.md` (*Estado verificado*, *Siguiente paso*, *Riesgos y preguntas abiertas* si cambian) y añade una entrada al Historial en `docs/progress-history.md` (sin borrar las anteriores), con la evidencia y, si hay bugs, su resumen.
5. Mata los emuladores que arrancaste y comprueba que no quedan procesos de Gradle ni temporales sin versionar. Si detienes el daemon de `adb` (`adb kill-server`), recuerda pre-arrancarlo en la próxima ejecución; si no, déjalo en marcha.

## 4. Referencia

**Formato del feedback de bugs.** Por cada bug: feature y RF afectado; esperado (Parte A) vs. observado; evidencia (ruta de captura, extracto ≤5 líneas de `logcat`, salida de comando); cómo reproducir (comandos `adb`/`gradlew` exactos, en orden); severidad (bloqueante o no); fidelidad si aplica (atributo de la rúbrica, valor esperado vs. observado); área probable (archivos de la Parte B — delimita las ventanas que el implementador debe leer en el reintento); y siguiente acción (re-ejecutar `feature-implementer`).

## 5. Salida

Reporta en forma compacta, sin repetir la matriz completa: Feature `<id>` — spec; Checks `init/lint/unit/e2e` → resultado y código de salida; APK `<ruta>` si aplica; RF `<n pasan / n fallan / n no verificables>` (detalle por RF en el informe); Fidelidad visual `<resumen y divergencias>` si aplica; Evidencia `<ruta del informe y capturas>`; Veredicto y estado en `feature_list.json`; Bugs `<severidad, RF>` si hay; bloqueo y comandos para desbloquearlo si no es verificable; Siguiente acción.

**Checklist:**

- [ ] Ejecuté `./init.sh`, `./lint.sh` y `./unit.sh` con códigos de salida registrados y salida contenida.
- [ ] Ejecuté `./e2e.sh` + recorrido `adb` solo si la Parte B lo pide (o dejé «no verificable» documentado sin rebajar).
- [ ] Evidencia dentro del presupuesto (≤4 artefactos, solo de esta feature; set anterior reemplazado).
- [ ] Modo elegido (completa o delta) y, en delta, válvula comprobada (cambio dentro de los RF fallidos e informe previo presente).
- [ ] Matriz de trazabilidad completa (cada RF con resultado); fidelidad 1:1 verificada si la Parte B declara UI.
- [ ] `git diff`: sin comportamiento fuera de alcance, sin secretos ni fotos reales.
- [ ] Estado escrito una sola vez, en el Cierre; checkboxes marcados solo con `validated`.
- [ ] Emuladores, daemon y temporales limpiados; `PROGRESS.md` e Historial actualizados.

> Desviación consciente respecto a `AGENTS.md`: esta skill elige la primera `passing` (no la primera `ready`/`not_started`) porque su trabajo es cerrar lo ya implementado. Queda documentado aquí y en cada reporte final.
