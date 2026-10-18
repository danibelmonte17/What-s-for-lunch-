---
name: feature-flow
description: Orquesta spec, implementación y validación de features en worktrees, hasta abrir PR.
---

# Feature Flow

Delega cada etapa en su subagente; el orquestador solo controla transiciones y git. **Fuente de estado:** `feature_list.json` del worktree, nunca el texto del subagente. La spec alterna planner y auditor independiente (máx. 3 revisiones); auditoría no anidada en planner.

```
feature-flow (agente primario build)
  └─ por cada feature, en orden:
       subagent → planner (feature-spec/redacción) → spec draft (JSON sigue not_started)
          │  ¿dudas abiertas y sin --auto? → 1 ronda de preguntas al humano → planner (retrabajo: las resuelve)
       subagent → clarify (feature-spec/auditoría) → ready | A RETRABAJAR
                  ↺ planner ⇄ clarify (máx 3; reintentos en delta; clarify cierra en ready)
       subagent → implementer (feature-implementer) → passing
       subagent → validator (feature-validate) → validated | in_progress
                  ↺ reintento implement→validate (máx 3; revalidación en delta)
       si validated → commit + push + gh pr create → borra worktree
```

## 1. Activación y reglas (única fuente)

- La lanza el usuario para ejecutar el ciclo de **una o varias** features, cada una aislada en un git worktree bajo `.worktrees/` sobre la rama `feature/<feature-id>` (id exacto de `feature_list.json`).
- **No se inicia con el entorno sucio**, con rebase/merge en curso ni con ramas o worktrees previos del flujo. Si las features elegidas tienen `depends_on` entre ellas, para y advierte (deben ir en flujos separados o tras mergear el PR anterior).
- **El orquestador no planifica, no implementa ni valida.** Delega en los subagentes con la herramienta `subagent`; no reescribe specs, no arregla código, no marca estados por su cuenta. Sí pregunta al humano las dudas de la spec (gate de §3.3.c); las respuestas las incorpora planner, nunca el orquestador.
- **Escrituras del orquestador:** solo git (worktree, commit, push, PR), `PROGRESS.md` y `docs/progress-history.md` dentro del worktree. El `status` de `feature_list.json` lo escriben los subagentes; **únicas excepciones**: los cierres en fallo de §3.4 (deja `passing`/`not_started`).
- **Prohibido:** force-push, borrar trabajo ajeno, tocar secretos, commitear archivos sensibles (`.jks`, `*.keystore`, `secrets.properties`, `local.properties`).
- **Una feature termina `validated` (con PR, salvo `--no-pr` o `gh` sin auth, donde no hay PR pero el flujo continúa) o el flujo para.** Nunca se abre un PR de una feature sin validar.
- **Máximo 3 ciclos de validación por feature** (y hasta 3 revisiones de spec). Al agotarlos: la feature queda en `passing` (o `not_started` si falló la spec), se conservan su worktree y su rama, se anota el bloqueo en `PROGRESS.md` y el flujo **para** pidiendo intervención humana.
- **Idioma:** documentación y comunicación en español (el del usuario); el código, en inglés.

## 2. Entradas (una vez por corrida; no releeas por feature lo ya leído)

1. `AGENTS.md` y la cabecera de `PROGRESS.md` (el Historial vive en `docs/progress-history.md`; no lo leas entero)
2. `feature_list.json` (raíz del repo)
3. `docs/build-brief.md` (líneas rojas) y `docs/risks-and-open-questions.md`
4. Por cada feature elegida: su spec `docs/specs/<feature-id>.md`, solo las secciones necesarias (cabecera de estado, dudas y resumen), localizadas con `grep -n '^#'` + `Read offset/limit`
5. `CONTEXT.md`, `docs/domain-model.md`, `docs/technical-discovery.md`, `DESIGN.md`: solo si una spec o un subagente los cita

Rutas relativas a la raíz del repo. El estado de cada feature durante la corrida se consulta con grep de su entrada en `.worktrees/<feature-id>/feature_list.json`.

## 3. Procedimiento

### 3.1 Argumentos y selección

`$ARGUMENTS`: uno o varios `feature-id` separados por espacios (opcional); `--base <rama>` (rama de partida de los PR; por defecto, la actual); `--no-pr` (sin push ni PR; deja rama y worktree listos); `--auto` (no interrumpe por dudas de la spec; sin humano en la sesión, el flujo actúa como con `--auto`).

**Rama:** se compone **siempre** automáticamente como `feature/<feature-id>`. Si `$ARGUMENTS` trae `--branch`, un prefijo u otro nombre de rama, ignóralo y avisa.

Selección: **con ids**, comprueba que existen y no están `validated`, sin dependencias mutuas entre las elegidas (si las hay, para y advierte), ordenadas respetando `depends_on` y el orden de la lista. **Sin ids**, la primera feature `not_started` o `ready` con todas sus `depends_on` `validated`; si no hay, reporta y para.

### 3.2 Preflight (entorno limpio)

1. `git status --porcelain` vacío (si no, para y pide limpiar o commitear). Sin rebase/merge en curso (`.git/rebase-merge` o `rebase-apply`).
2. `git fetch origin --prune`. Resuelve `<base>` en local o `origin/<base>`.
3. Comprueba que no existen `.worktrees/<feature-id>` ni la rama `feature/<feature-id>` de una corrida previa (si existen, para y pide resolverlas).
4. Si habrá PR (`--no-pr` ausente): `gh auth status` en verde (si no, avisa que no habrá PR y no abortes).
5. Resuelve el root absoluto del repo: `git rev-parse --show-toplevel`.

### 3.3 Por cada feature, en secuencia

**a. Rama y worktree.** `git worktree add -b feature/<feature-id> .worktrees/<feature-id> origin/<base>` — siempre desde `origin/<base>` recién fetcheado.

**b. Sembrar los locales gitignored.** Copia al worktree `local.properties` y `secrets.properties` si existen en el checkout principal (Gradle y `feature-validate` los necesitan). No los versiones ni los edites.

**c. Spec (planner ⇄ auditoría).** Solo si no existe `docs/specs/<feature-id>.md` o el estado es `not_started`. `MAX_CLARIFY = 3` (entradas `veredicto: A RETRABAJAR` en los bloques `## Clarificación` de la spec + `docs/specs/<feature-id>-clarificacion.md`):

1. Invoca `planner` con `$feature-spec` modo redacción; deja la spec en borrador. Primer pase en sesión limpia. En retrabajo: antes de invocar, copia la spec a `.harness/spec-<feature-id>-<intento>.md` (gitignored); continúa su sesión con `sessionID` (fallback §4) y pasa solo el delta. Al volver, calcula el delta con salida contenida (`git diff --no-index <snapshot> <spec>` —el exit 1 solo indica diferencias, no es un fallo— o comparación de cabeceras de sección) y guárdalo para la auditoría.
2. **Preguntas (solo dudas abiertas):** localiza en spec y registro las dudas abiertas de esta feature con grep. Si hay humano y no `--auto`, pregúntalas en **una sola ronda** (bloqueantes primero; en las no bloqueantes, valor asumido primero) y re-invoca planner en modo continuar con las respuestas `Q-<id>: <respuesta>`; segunda ronda solo si una respuesta abre una duda bloqueante nueva. Continúa su sesión. Nunca edites tú la spec. Con `--auto`/sin humano, anota en `PROGRESS.md` y continúa.
3. Invoca `clarify` con `$feature-spec` modo auditoría (id, ruta y, en intentos 2+, el delta + hallazgos previos pendientes en el prompt; continúa su sesión). Sin delta, indica auditoría completa. APTA cierra en `ready`.
4. Lee el `status` de la entrada en `.worktrees/<feature-id>/feature_list.json`: `ready` → continúa con la implementación; `not_started` con menos de 3 `A RETRABAJAR` → vuelve al paso 1 (retrabajo); `not_started` con 3 → «Cierre en fallo de spec» (§3.4).

**d. Implementación.** Invoca a `implementer` con `$feature-implementer` (sesión limpia la primera vez). Debe dejar la feature en `passing`. Si no está en `ready` al llegar aquí, para y reporta la incoherencia.

**e. Validación con reintentos.** `MAX_VALIDACIONES = 3`:

1. Invoca a `validator` con `$feature-validate` (sesión limpia la primera vez). En revalidación, continúa su sesión con `sessionID` y pásale el informe de bugs previo y el delta de la corrección —`git -C .worktrees/<feature-id> status --porcelain` + `git diff` contenido, con las rutas nuevas—; el validator elige completa o delta (válvula en §3.0 de su skill).
2. Lee el `status` de la entrada en `.worktrees/<feature-id>/feature_list.json`:
   - `validated` → salir del bucle.
   - `passing` (no verificable: sin AVD, red o clave) → **no reintentes**; para el flujo y reporta los prerrequisitos exactos.
   - `in_progress` (bug demostrado) → si quedan intentos, re-invoca a `implementer` con el informe de bugs, **continuando su sesión con `sessionID`** (fallback §4); al volver, revalida en delta (paso 1). Agotados los 3 ciclos → «Cierre en fallo» (§3.4).

**f. Cierre en éxito** (solo con `validated`): actualiza en el worktree `PROGRESS.md` y, si falta, la entrada del Historial en `docs/progress-history.md` (el subagente ya lo hace; añade solo el resumen del flujo). Commitea con Conventional Commits (`feat(<feature-id>): <resumen>`, `docs(<feature-id>): spec y estado`, `test(<feature-id>): evidencia de validación`). `git -C .worktrees/<feature-id> push -u origin feature/<feature-id>`. Crea el PR:

```
gh pr create --base <base> --head feature/<feature-id> \
  --title "feat(<feature-id>): <descripción>" \
  --body "<cuerpo>"
```

El cuerpo resume: feature id y descripción, RF validados, enlaces a la evidencia en `docs/evidence/<feature-id>/`, resultado de `lint`/`unit`/`e2e` y nº de validaciones. Devuelve la URL. Después `git worktree remove --force .worktrees/<feature-id>` (la rama se conserva mientras el PR esté abierto). Con `--no-pr`: omite push y PR, deja el commit en la rama, reporta rama y worktree y **continúa con la siguiente feature**.

### 3.4 Cierres en fallo (no crees PR; conserva worktree y rama; para el flujo)

**Validación agotada (3 ciclos):** deja la feature en `passing` en `feature_list.json` (desviación consciente: `feature-validate` la deja en `in_progress`; se promueve a `passing` para señalizar «implementada pero requiere intervención»); anota en `PROGRESS.md` (*Siguiente paso*) y en `docs/progress-history.md` del worktree: feature id, RF afectado, resumen de bugs, rutas de evidencia y comando de reproducción; reporta la ruta y el siguiente paso (corregir con `feature-implementer` y re-ejecutar la validación).

**Spec agotada (3 auditorías):** deja la feature en `not_started` (única excepción de escritura del orquestador); anota bloqueo, intentos y hallazgos pendientes en `PROGRESS.md` e Historial; conserva worktree/rama y pide intervención humana.

## 4. Invocación de subagentes

`subagent` hereda el root del checkout principal; cada prompt declara el worktree:

```
Ejecuta la feature <feature-id> tratando el directorio
<root-absoluto>/.worktrees/<feature-id> como la raíz del repositorio.

- Todas las lecturas, búsquedas y ediciones deben usar rutas bajo ese directorio.
- Cualquier comando (./init.sh, ./gradlew, adb, git) se ejecuta con workdir =
  <root-absoluto>/.worktrees/<feature-id>.
- Las rutas relativas de tu skill (docs/specs/, feature_list.json, PROGRESS.md)
  se resuelven dentro de ese worktree.

Sigue tu skill al pie de la letra. Devuelve el resumen final que define tu skill.
```

Para `clarify`, indica id, ruta, modo auditoría y, en intentos 2+, el delta + hallazgos previos. APTA cierra en `ready`.

Sesión nueva en el primer pase. En retrabajo o revalidación, continúa el mismo subagente con su `sessionID` y pasa solo el delta; indica que no relea lo ya leído (solo el delta y lo que este cite). Usa sesión nueva únicamente si no hay sesión utilizable (fallback), pasando el delta y las rutas necesarias. Ordena el prompt para cache: preámbulo fijo del worktree primero; delta variable al final.

Al volver de cada subagente, comprueba la transición con grep de la entrada en `.worktrees/<feature-id>/feature_list.json` (nunca el texto del subagente). Si el `status` no es el esperado, para y reporta la incoherencia.

**Fallos y paradas.**

| Situación | Acción |
|---|---|
| Entorno sucio, rebase/merge en curso | Parar sin tocar nada |
| Rama o worktree previo del flujo | Parar y pedir resolverlo |
| Features con dependencia mutua en la corrida | Parar y advertir |
| Dudas abiertas | §3.3.c: preguntar y pasar respuestas al planner; con `--auto`/sin humano, anotar y continuar |
| Dudas siguen tras la ronda de preguntas | Continuar a auditoría y reportar |
| 3 auditorías sin APTA | Dejar `not_started`, conservar worktree y parar |
| Validación falla 3 veces | Deja `passing`, conserva worktree, parar |
| No verificable (sin AVD/red/clave) | No reintenta; deja `passing`, reporta desbloqueo |
| `gh` sin auth | No crea PR; deja rama y worktree, explica el desbloqueo |
| `subagent` devuelve estado incoherente | Parar y reportar |

## 5. Salida

Reporta (≤10 líneas): Features procesadas (en orden) y estado final `<id: validated|passing|not_started>`; Preguntas al humano por feature `<n preguntadas / n resueltas>` y rondas; Revisiones de clarify y validaciones por feature (`completa|delta` por intento) con RF validados; PR de cada feature validada `<URL>` (o por qué no se creó); Worktrees `<eliminados>` / `<conservados → ruta>`; Bloqueos y prerrequisitos para desbloquear; Procesos de Gradle o emuladores pendientes y temporales sin versionar.

**Checklist:**

- [ ] Preflight limpio: sin cambios, rebase/merge, worktrees ni ramas previas.
- [ ] Cada transición leída de `feature_list.json` del worktree (grep de la entrada), no del texto del subagente.
- [ ] En reintentos y retrabajos continué la sesión anterior del subagente con `sessionID`, pasé solo el delta (a auditoría/validación incluido) e inicié una sesión limpia únicamente cuando aplica el fallback de §4.
- [ ] Cada feature termina `validated` (con PR) o el flujo se detiene con el bloqueo anotado.
- [ ] Ningún PR abierto sin `validated`; worktrees limpiados o conservados con justificación.
- [ ] `PROGRESS.md` y `docs/progress-history.md` actualizados y sin secretos ni temporales sin versionar.
