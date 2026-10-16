# Progreso — What's for Lunch?

## Estado verificado
Solo lo comprobado, no lo que se cree.
- `init`: pasa con `./init.sh` (exit 0: compila, instala y abre). `./init.ps1` devuelve exit 1 por un falso negativo de Windows PowerShell 5.1 (el stderr de `adb shell monkey` + `$ErrorActionPreference="Stop"`), con el mismo objetivo cumplido
- `lint`: pasa (exit 0, 0 errores) · `unit`: pasa (exit 0, 2 tests) · `e2e`: pasa (exit 0, 7/7 tests), en emulador API 37
- Funcionalidades `validated`: 1 — `arrancar-app-en-mis-recetas` (1 de 23)
- Funcionalidades `passing`: ninguna
- Funcionalidades `in_progress`: ninguna
- `arrancar-app-en-mis-recetas`: B-1, B-2 y B-3 resueltos y verificados; los 11 RF pasan su verificación (e2e + `unit` + `uiautomator` + capturas es/en). Evidencia en `docs/evidence/arrancar-app-en-mis-recetas/`
- Roto o pendiente de arreglar: nada conocido

## Siguiente paso
Implementar `crear-receta` (depende de `arrancar-app-en-mis-recetas`, ya `validated`): redactar/auditar su spec y luego implementarla. Recordar el escenario de checkout fresco: `init` regenera `local.properties` sin escapar (`lint.xml` ya lo excluye de `PropertyEscape`).

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: RK02 (el modelo de detección MediaPipe está por validar), S05 (la API key vive en el dispositivo), S07 (validaciones del formulario de receta). S06 queda confirmado con el scaffold real (Q-arrancar-app-en-mis-recetas-1).
- Nota de entorno: el build fija `org.gradle.java.home` a un JDK 17 (`gradle.properties`, con la ruta escapada para `lint`: `C\:/...`) porque AGP 8.13.2 no arranca con el JBR de Android Studio (JDK 25), que es el que seleccionan los scripts del harness cuando `JAVA_HOME` no está definido.
- Nota de lint: la regla `PropertyEscape` ignora `local.properties` (generado por `init.ps1`, harness intocable, sin escapar el `:` de la unidad) mediante `lint.xml` en la raíz, referenciado desde `app/build.gradle.kts`.

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
