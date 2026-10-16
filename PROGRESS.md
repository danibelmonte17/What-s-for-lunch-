# Progreso — What's for Lunch?

## Estado verificado
Solo lo comprobado, no lo que se cree.
- Último `init`: exit 1 (falso negativo: `adb shell monkey` escribe en stderr y `$ErrorActionPreference="Stop"` lo convierte en error en Windows PowerShell 5.1); el objetivo real se cumple (APK instalado y `MainActivity` resumed)
- `lint`: **falla** (exit 1; B-3) · `unit`: pasa (exit 0, 2 tests) · `e2e`: pasa (exit 0, 7/7 tests)
- Funcionalidades `validated`: ninguna (0 de 23)
- Funcionalidades `passing`: 1 — `arrancar-app-en-mis-recetas` (cerrada por el flujo `feature-flow` como `passing` al agotar 3 ciclos de validación; requiere intervención humana para B-3)
- Funcionalidades `in_progress`: ninguna
- Los 11 RF de la feature pasan su verificación (e2e + `unit` + `uiautomator` + capturas); el único freno a `validated` es `lint`
- Roto o pendiente de arreglar: B-1 y B-2 **resueltos**; B-3 — `lint` da 2 errores `PropertyEscape`: `gradle.properties:4` (ruta de `org.gradle.java.home` sin escapar, del scaffold) y `local.properties:1` (generado sin escapar por `init.ps1`). Evidencia en `docs/evidence/arrancar-app-en-mis-recetas/informe-validacion.md`

## Siguiente paso
El flujo `feature-flow` paró `arrancar-app-en-mis-recetas` en `passing` tras agotar los 3 ciclos de validación (B-3 sin resolver: `lint` en rojo). Intervención humana: arreglar B-3 con `feature-implementer` (escapar la ruta en `gradle.properties` y evitar el error del `local.properties` generado por `init.ps1`, que los scripts del harness no se tocan) y re-ejecutar `feature-validate` en emulador API 30+. Reproducción: `./gradlew.bat --no-daemon lintDebug` o `./lint.ps1`. El worktree y la rama `feature/arrancar-app-en-mis-recetas` se conservan. Después, implementar `crear-receta` (depende de esta).

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: RK02 (el modelo de detección MediaPipe está por validar), S05 (la API key vive en el dispositivo), S07 (validaciones del formulario de receta). S06 queda confirmado con el scaffold real (Q-arrancar-app-en-mis-recetas-1).
- Nota de entorno: el build fija `org.gradle.java.home` a un JDK 17 (`gradle.properties`) porque AGP 8.13.2 no arranca con el JBR de Android Studio (JDK 25), que es el que seleccionan los scripts del harness cuando `JAVA_HOME` no está definido.

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
