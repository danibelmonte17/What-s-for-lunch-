# Informe de validación — `sincronizar-spec-de-crear-receta`

Fecha: 2026-10-19 · Modo: validación completa (primer pase, sesión limpia) · Categoría: `tecnica` (solo documentación)

Entorno: worktree `C:\Proyectos\whats-for-lunch\.worktrees\sincronizar-spec-de-crear-receta` · Windows 11 · AVD ya arrancado `emulator-5554` (API 37, `sdk_gphone16k_x86_64`) · JDK de Android Studio (JBR) · Gradle 8.14.3 / AGP 8.13.2.

## Checks del arnés

| Check | Comando | Resultado | Exit |
|---|---|---|---|
| init | `./init.sh` (Git Bash, `adb` en PATH) | pasa: instala y arranca la app (`com.whatsforlunch.app`, pid activo) | 0 |
| lint | `./lint.sh` | pasa (`BUILD SUCCESSFUL`, 0 errores) | 0 |
| unit | `./unit.sh` | pasa: 10 tests, 0 fallos, 0 errores, 0 saltados (2 suites) | 0 |
| e2e | no ejecutado | **no aplica**: la Parte B lo excluye expresamente («E2E: no aplica — feature de solo documentación; la regresión medida (44 tests, sesión 23) es la fuente de comportamiento citada») | — |

Nota de entorno (no afecta al veredicto): `init.ps1` devolvió exit 1 bajo PowerShell 5.1 porque `adb shell monkey` escribe su banner `args: [...]` en stderr y el script usa `$ErrorActionPreference = "Stop"` (NativeCommandError); la app sí quedó instalada y en ejecución. El mismo arranque con `./init.sh` (el script equivalente) devolvió exit 0. Los scripts del arnés no se han tocado. Logs en `.harness/` (`init.log`, `lint.log`, `unit.log`).

## Matriz de trazabilidad

| RF | Verificación | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | RF-15 de `crear-receta.md` fija «Publicar Receta» deshabilitado | pasa | `docs/specs/crear-receta.md:34` |
| RF-2 | RF-16 fija la lista de requisitos incumplidos junto al dock | pasa | `docs/specs/crear-receta.md:35` |
| RF-3 | Casos límite describe el bloqueo por requisitos | pasa | `docs/specs/crear-receta.md:51` |
| RF-4 | Plan de verificación describe los escenarios E2E del deshabilitado y de la lista | pasa | `docs/specs/crear-receta.md:163` |
| RF-5 | Trazabilidad fija la verificación E2E de RF-15/RF-16 | pasa | `docs/specs/crear-receta.md:85-86` |
| RF-6 | T6 de `afinar-fidelidad-visual-del-formulario.md` marcada | pasa | `docs/specs/afinar-fidelidad-visual-del-formulario.md:152` → `- [x] T6` |
| RF-7 | Cambios de contenido limitados a `docs/specs/**` | pasa | `git diff --name-only HEAD`: ver «Alcance» |
| NFR-1 | Redacción en español, identificadores en inglés | pasa | Texto revisado: `publishIsDisabledUntilRequirementsAreMet`, `RecipeValidation.validateForPublish`, `dock`, `⚠ S07` se citan en inglés/símbolo; prosa en español |

## verify 1 — coherencia de las cinco secciones con `CrearRecetaE2ETest`

| Sección | Observado |
|---|---|
| RF-15 | «MIENTRAS el formulario no cumple los requisitos de Publicar, EL SISTEMA muestra «Publicar Receta» deshabilitado con un estilo distinguible (`⚠ S07`).» |
| RF-16 | «… enumera junto al dock los requisitos incumplidos que impiden Publicar (título, foto, al menos un Ingrediente y un Paso válidos y ninguna fila de Ingrediente ni Paso inválidos) (Q-crear-receta-1 …).» |
| Casos límite | «… requisitos de Publicar incumplidos → «Publicar Receta» deshabilitado y requisitos enumerados junto al dock → RF-15, RF-16; cumplidos → habilitado y publicable → RF-17.» |
| Plan de verificación | «E2E propios: Publicar deshabilitado y lista junto al dock mientras falta algún requisito y habilitado al cumplirlos, sin guardar nada antes de publicar (RF-15, RF-16; `publishIsDisabledUntilRequirementsAreMet`, `publishWithoutPhotoIsBlocked`, `publishWithoutIngredientIsBlocked`, `invalidIngredientRowsAreListedAsBlockingRequirement`) …» |
| Trazabilidad | RF-15 → «escenario E2E: Publicar deshabilitado»; RF-16 → «escenario E2E: lista junto al dock» |

Sin rastro de la redacción previa: `Select-String "requisito incumplido|pulsa(n)? «Publicar Receta»"` en `crear-receta.md` solo devuelve RF-17 (pulsar con todos los requisitos para publicar), Casos límite y la línea de fidelidad del NFR.

Contraste con el código entregado (sin cambios en esta feature):

- `app/src/main/java/com/whatsforlunch/app/domain/crearreceta/RecipeValidation.kt:48-55` — `validateForPublish` añade `TITLE`, `PHOTO`, `INGREDIENT`, `STEP`, `INVALID_INGREDIENT`, `INVALID_STEP`: coincide con la enumeración de RF-16.
- `app/src/main/java/com/whatsforlunch/app/ui/crearreceta/CrearRecetaScreen.kt:1080-1082` — `PublishErrors` se dibuja dentro del `ActionDock` (junto al dock); `:1137` `enabled = uiState.canPublish`.
- `app/src/main/java/com/whatsforlunch/app/ui/crearreceta/CrearRecetaViewModel.kt:361-362` — `publishErrors = unmet; canPublish = unmet.isEmpty()`.
- `app/src/androidTest/java/com/whatsforlunch/app/CrearRecetaE2ETest.kt:258` `publishIsDisabledUntilRequirementsAreMet`, `:217` `publishWithoutPhotoIsBlocked`, `:230` `publishWithoutIngredientIsBlocked`, `:275` `invalidIngredientRowsAreListedAsBlockingRequirement` — los cuatro escenarios citados existen y afirman el deshabilitado y la lista.
- `docs/evidence/crear-receta/informe-validacion.md` — 22 E2E en verde en la validación de `crear-receta` (ciclo 2); `PROGRESS.md` registra la regresión de 44 E2E en verde de las sesiones 23-24, que incluye esa suite.

## verify 2 — T6

`docs/specs/afinar-fidelidad-visual-del-formulario.md:152`:

```
- [x] T6: adaptar la spec y los tests E2E de `crear-receta` y re-ejecutar su regresión. (cubre el criterio de finalización)
```

Marcado, sin otros cambios en el fichero (`git diff`: 1 línea).

## Alcance

`git diff --name-only HEAD` (previo al cierre):

```
PROGRESS.md
docs/progress-history.md
docs/risks-and-open-questions.md
docs/specs/afinar-fidelidad-visual-del-formulario.md
docs/specs/crear-receta.md
feature_list.json
```

más el fichero nuevo `docs/specs/sincronizar-spec-de-crear-receta.md` (untracked). `git diff --name-only HEAD -- app/` está vacío: **sin cambios de código, `strings.xml`, tests ni harness**. Los cuatro ficheros de estado (`PROGRESS.md`, `docs/progress-history.md`, `docs/risks-and-open-questions.md`, `feature_list.json`) son artefactos de proceso exigidos por `AGENTS.md`/la skill; los cambios de contenido de la feature quedan en `docs/specs/**` (RF-7). Sin secretos, fotos reales ni logs en la evidencia.

Revisión del diff de contenido:

- `docs/specs/crear-receta.md` (±12): solo las cinco secciones objetivo (RF-15, RF-16, Casos límite «Guardado incompleto», Plan de verificación «E2E propios» y dos filas de Trazabilidad); el texto coincide literalmente con la tabla de §Enfoque técnico de la spec.
- `docs/specs/afinar-fidelidad-visual-del-formulario.md` (±2): solo `[ ]` → `[x]` en T6.

## Fidelidad visual

No aplica: la Parte B declara «UI: no aplica — sin cambios visibles» y no hay cambios de código.

## Veredicto

**`validated`** — verify 1 y verify 2 en verde, `init`/`lint`/`unit` en verde (exit 0) y alcance correcto. Sin bugs. Sin capturas (no hay UI); 1 artefacto de evidencia.
