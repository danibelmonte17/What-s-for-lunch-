# Plantillas de artefactos

Contenido: 1. AGENTS.md · 2. feature_list.json · 3. PROGRESS.md y docs/progress-history.md · 4. init.sh · 5. init.ps1 · 6. lint.sh / lint.ps1 · 7. unit.sh / unit.ps1 · 8. e2e.sh / e2e.ps1 · 9. Pendientes en risks-and-open-questions.md

Adapta cada plantilla al proyecto usando solo lo que dicen los documentos de especificación: no inventes valores (ver SKILL.md, sección 1). Sustituye todo lo que está entre `<...>` y todas las líneas `# Ejemplo:`: el validador falla si quedan.

---

## 1. AGENTS.md

Protocolo de trabajo en tres bloques. No repite el contexto de producto. En *Qué leer* nombra solo los archivos que existan en el proyecto; el validador falla si cita uno inexistente. Los comandos de arranque y de verificación (`init`/`lint`/`unit`/`e2e`) salen de los documentos del proyecto.

```markdown
# Reglas para el agente de codificación

## 1. Qué leer
Al empezar cada sesión, en este orden:
1. `CONTEXT.md` y `docs/build-brief.md`: producto, MVP, qué queda fuera y líneas rojas.
2. Este archivo.
3. `PROGRESS.md`: estado verificado y siguiente paso. Lee solo su cabecera; el Historial completo vive en `docs/progress-history.md` y no se lee entero (como mucho su última entrada si necesitas contexto).
4. `feature_list.json`.
5. `git log --oneline -20`.

Consulta estos solo si la funcionalidad elegida lo requiere, para no gastar contexto en lo que no toca:
- `docs/specs/<id>.md`: la especificación y el plan de la funcionalidad elegida. Léela siempre antes de implementarla.
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
- `validated`: se han ejecutado todas sus comprobaciones de `verify` en el entorno real y pasan. No vale la autopercepción del modelo. Con interfaz web, usa automatización de navegador (<Playwright o Puppeteer vía MCP>).
- `status` es el único campo de `feature_list.json` que puedes modificar. Avanza en orden; si una verificación falla, vuelve a `in_progress`. Si no puedes verificar, déjala en `passing` y anota el motivo en `PROGRESS.md`. No borres, reescribas ni renombres funcionalidades, pasos o verificaciones.
- La sesión está cerrada cuando:
  1. El arranque (`init.sh`/`init.ps1`) sigue pasando y, si tocaste código, `lint` y `unit` están en verde.
  2. `PROGRESS.md` está al día: reescribe *Estado verificado* y *Siguiente paso* y añade una entrada al Historial en `docs/progress-history.md` (sin borrar las anteriores; no lo leas entero).
  3. Los servidores que arrancaste están detenidos (`.harness/server.pid`) y no quedan archivos temporales.
```

---

## 2. feature_list.json

Antes de escribirlo, lee `slicing.md` (en esta misma carpeta): define el tamaño de cada funcionalidad, los ids y cuántas esperar. Todas nacen `not_started`; `status` es el único campo que se modifica después.

```json
{
  "project": "<nombre del proyecto>",
  "version": 1,
  "features": [
    {
      "id": "registrar-cuenta",
      "category": "funcional",
      "description": "El usuario puede crear una cuenta con su email.",
      "depends_on": [],
      "steps": [
        "Implementar endpoint POST /api/register con validación de email",
        "Almacenar la contraseña con hash, nunca en texto plano"
      ],
      "verify": [
        "E2E: abrir /register, completar el formulario y comprobar que redirige al panel"
      ],
      "status": "not_started"
    },
    {
      "id": "proteger-rutas-privadas",
      "category": "seguridad",
      "description": "Las rutas privadas rechazan peticiones sin sesión válida.",
      "depends_on": ["registrar-cuenta"],
      "steps": [
        "Añadir middleware de autenticación a las rutas privadas"
      ],
      "verify": [
        "E2E: acceder a /panel sin sesión redirige a /login"
      ],
      "status": "not_started"
    }
  ]
}
```

Reglas del esquema:
- `id`: nombre único y legible, en minúsculas y con guiones, que resume el comportamiento. Estable: no se renombra.
- `category`: `funcional`, `tecnica` (sin tilde, para que el valor sea estable) o `seguridad`.
- `description`: una frase con un solo comportamiento.
- `depends_on`: ids de funcionalidades anteriores en la lista (vacía si no hay). Una funcionalidad solo puede depender de las que aparecen antes.
- `steps`: qué implementar. `verify`: comprobaciones observables con resultado esperado; al menos una de extremo a extremo (en proyectos sin interfaz, una llamada real a la API o al comando).
- `status`: uno de estos cinco valores.

| `status` | Significa |
|---|---|
| `not_started` | Sin empezar |
| `ready` | Especificación y plan escritos y aprobados por auditoría independiente (APTA), sin empezar a implementar |
| `in_progress` | En curso (como máximo una) |
| `passing` | Implementada y `lint` + `unit` pasan; sus `verify` aún no se han ejecutado todas |
| `validated` | Todas sus `verify` ejecutadas en el entorno real y pasan |

Una funcionalidad cuenta como cumplida para las que dependen de ella solo cuando está `validated`.

El conjunto cubre el MVP de `docs/build-brief.md` sin añadir nada de su lista de exclusiones.

---

## 3. PROGRESS.md y docs/progress-history.md

Dos archivos con ciclos de vida distintos. `PROGRESS.md` es la cabecera que lee cada sesión: se reescribe al cerrar y no crece sin control. `docs/progress-history.md` solo crece; ninguna sesión lo lee entero.

```markdown
# Progreso — <proyecto>

## Estado verificado
Solo lo comprobado, no lo que se cree.
- Último `init`: no ejecutado todavía (sin código de aplicación)
- Funcionalidades `validated`: ninguna (0 de <N>)
- Roto o pendiente de arreglar: ninguno

## Siguiente paso
`<id-de-la-funcionalidad>`: <por qué empezar por ahí y qué punto exacto retomar>

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: <lista, o "ninguno">

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
```

```markdown
# Historial de progreso — <proyecto>

Entradas de sesión en orden cronológico (solo se añaden, nunca se borran). No se lee entero: consulta como mucho la última entrada si necesitas contexto.

### Sesión 0: harness inicial (harness-starter)
- Especificación completada; andamiaje operativo generado; sin código de aplicación.
- Archivos creados: AGENTS.md, feature_list.json, PROGRESS.md, docs/progress-history.md, init.sh, init.ps1, lint.sh, lint.ps1, unit.sh, unit.ps1, e2e.sh, e2e.ps1.
- Funcionalidades: <N>.
```

---

## 4. init.sh

Arranque: cuatro etapas (instalar, preparar entorno, arrancar, verificar). Idempotente y funcional desde un checkout limpio. Equivalente a `init.ps1`. **No ejecuta tests ni lint**: eso vive en `lint`/`unit`/`e2e`.

`START_CMD`, el puerto y la ruta de salud salen de los documentos del proyecto. Si no hay servidor (p. ej. app mobile), la etapa de arranque compila e instala/abre la app (Android: `assembleDebug` + `adb install` + `am start`) y *Verificando* comprueba que el artefacto se generó o la app arrancó; nunca metas aquí la suite de tests. Añade `.harness/` al `.gitignore` (guarda el pid y el log del servidor).

```bash
#!/usr/bin/env bash
set -euo pipefail

# Rutas absolutas resueltas desde el propio script
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
mkdir -p "$ROOT/.harness"

PORT="${PORT:-<puerto>}"
HEALTH_URL="http://localhost:${PORT}/<ruta-de-salud>"
START_CMD=(<comando de arranque, p. ej. npm run dev>)

is_up() { curl -fsS -o /dev/null "$HEALTH_URL" 2>/dev/null; }

echo "==> Instalando dependencias"
# Ejemplo: npm ci   |   pip install -r requirements.txt

echo "==> Preparando entorno"
# Ejemplo: [ -f .env ] || cp .env.example .env   (sin secretos reales)

echo "==> Arrancando servidores"
if is_up; then
  echo "Ya hay un servidor respondiendo en $HEALTH_URL"
else
  nohup "${START_CMD[@]}" > "$ROOT/.harness/server.log" 2>&1 &
  echo $! > "$ROOT/.harness/server.pid"
fi

echo "==> Verificando"
for _ in $(seq 1 30); do
  if is_up; then
    echo "==> Entorno listo"
    echo "Servidor en http://localhost:${PORT}"
    exit 0
  fi
  sleep 1
done
echo "ERROR: el servidor no responde en $HEALTH_URL (ver .harness/server.log)" >&2
exit 1
```

---

## 5. init.ps1

Mismas etapas, puertos y variables que `init.sh`; solo cambia la sintaxis. **No ejecuta tests ni lint.** `$ErrorActionPreference = "Stop"` no detecta que falle un comando nativo (npm, pip...), por eso cada uno pasa por `Invoke-Step`. En Windows, `npm` se invoca como `npm.cmd`.

```powershell
$ErrorActionPreference = "Stop"

# Rutas absolutas resueltas desde el propio script
$Root = $PSScriptRoot
Set-Location $Root
New-Item -ItemType Directory -Force -Path (Join-Path $Root ".harness") | Out-Null

$Port = if ($env:PORT) { $env:PORT } else { "<puerto>" }
$HealthUrl = "http://localhost:$Port/<ruta-de-salud>"
$StartCmd = "<comando de arranque, p. ej. npm.cmd>"
$StartArgs = @(<argumentos, p. ej. "run", "dev">)

function Test-Up {
    try { Invoke-WebRequest -UseBasicParsing -Uri $HealthUrl -TimeoutSec 2 | Out-Null; return $true }
    catch { return $false }
}

function Invoke-Step {
    param([string]$Cmd, [string[]]$CmdArgs)
    & $Cmd @CmdArgs
    if ($LASTEXITCODE -ne 0) { throw "Falló: $Cmd $CmdArgs" }
}

Write-Host "==> Instalando dependencias"
# Ejemplo: Invoke-Step npm.cmd @("ci")

Write-Host "==> Preparando entorno"
# Ejemplo: if (-not (Test-Path .env)) { Copy-Item .env.example .env }   (sin secretos reales)

Write-Host "==> Arrancando servidores"
if (Test-Up) {
    Write-Host "Ya hay un servidor respondiendo en $HealthUrl"
} else {
    $proc = Start-Process -FilePath $StartCmd -ArgumentList $StartArgs -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $Root ".harness\server.log") `
        -RedirectStandardError (Join-Path $Root ".harness\server.err.log")
    $proc.Id | Set-Content (Join-Path $Root ".harness\server.pid")
}

Write-Host "==> Verificando"
for ($i = 0; $i -lt 30; $i++) {
    if (Test-Up) {
        Write-Host "==> Entorno listo"
        Write-Host "Servidor en http://localhost:$Port"
        exit 0
    }
    Start-Sleep -Seconds 1
}
Write-Error "El servidor no responde en $HealthUrl (ver .harness\server.log)"
exit 1
```

---

## 6. lint.sh / lint.ps1

Verificación estática: lint y, en web, tipos. No arranca entorno y sale con código distinto de 0 si falla. El comando sale de los documentos del proyecto; el `.sh` y el `.ps1` usan el mismo. En Android, si `JAVA_HOME` no está definido, exporta `JAVA_HOME` con el JBR de Android Studio antes de invocar Gradle (mismo criterio en `unit` y `e2e`).

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Lint"
# Ejemplo Android: ./gradlew --no-daemon lintDebug
# Ejemplo web:     npm run lint && npx tsc --noEmit
```

```powershell
$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
Set-Location $Root

Write-Host "==> Lint"
# Ejemplo Android: & .\gradlew.bat --no-daemon lintDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
```

---

## 7. unit.sh / unit.ps1

Tests rápidos de unidad. No arranca entorno y sale con código distinto de 0 si algún test falla.

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Tests unitarios"
# Ejemplo Android: ./gradlew --no-daemon testDebugUnitTest
# Ejemplo web:     npm test        (vitest/jest)
```

```powershell
$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
Set-Location $Root

Write-Host "==> Tests unitarios"
# Ejemplo Android: & .\gradlew.bat --no-daemon testDebugUnitTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
```

---

## 8. e2e.sh / e2e.ps1

Verificación de extremo a extremo. En mobile instala y recorre la app en emulador/dispositivo; en web, automatiza el navegador. Sale con código distinto de 0 si falla.

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Tests de extremo a extremo"
# Mobile (Android, emulador/dispositivo API 30+): ./gradlew --no-daemon connectedDebugAndroidTest
# Web: npx playwright test
```

```powershell
$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
Set-Location $Root

Write-Host "==> Tests de extremo a extremo"
# Mobile (Android): & .\gradlew.bat --no-daemon connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
```

---

## 9. Pendientes en risks-and-open-questions.md

Cuando falte información para generar el harness, añade este bloque al final de `docs/risks-and-open-questions.md` (créalo si no existe). No toques lo que ya hay. Si el bloque ya existe de una ejecución anterior, actualiza sus entradas en vez de duplicarlas.

```markdown
## Pendiente para el harness (añadido por harness-starter)
Resolver con `build-brief` y volver a ejecutar harness-starter.

- [ ] <Qué falta, en una frase>. Lo necesita <archivo del harness> para <qué>.
```
