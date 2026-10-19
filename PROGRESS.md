# Progreso — What's for Lunch?

## Estado verificado
Solo lo comprobado, no lo que se cree. El detalle por feature vive en `docs/evidence/<feature-id>/` y la crónica de sesiones en `docs/progress-history.md` (solo se añade; no se lee entera salvo la última entrada).
- **Harness**: `init` pasa (⚠ carrera conocida del `pidof`: puede devolver exit 1 a la primera y pasar al reintentar) · `lint` 0 errores · `unit` 12 tests · `e2e` **53/53**. Último medido: sesión 38 (ciclo 7) sobre `Pixel_10_Pro`.
- **Funcionalidades `accepted`: 6 de 26** — `arrancar-app-en-mis-recetas`, `crear-receta`, `afinar-fidelidad-visual-del-formulario`, `sincronizar-spec-de-crear-receta`, `editar-receta` y `eliminar-receta`. Sin `passing` ni `in_progress`. (Las sesiones anteriores llamaban `validated` a `accepted`.)
- **Últimos cambios (sesión 40)**: build-brief reestructurado a las plantillas del harness nuevo, `feature_list.json`/`AGENTS.md` migrados al vocabulario `passing`/`accepted`, comandos de `.opencode/` alineados y `README.md` reescrito. Sin cambios en `app/`: `lint`/`unit`/`e2e` no se repitieron.
- **Entorno**: AVD único `Pixel_10_Pro` (API 37; el `Pixel_10a` ya no existe); `adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools` (y `...\emulator` para el AVD). Build verde con AGP 8.13.2 + Room 2.8.5 + KSP 2.3.12 y JDK 17 fijado en `gradle.properties` (AGP no arranca con el JBR de Android Studio, JDK 25). `local.properties` se genera sin escapar el `:` y `lint.xml` lo excluye de `PropertyEscape`. Capturas en español con locale por aplicación (`cmd locale set-app-locales … --locales es-ES`, revertido al terminar).
- **Incidencias abiertas del entorno**: el picker del MediaStore devuelve un ítem inexistente (`FileNotFoundException: No item at content://media/external/file/178`), así que no se pueden crear recetas con foto desde la UI (RF-8 se verificó con `PhotoStore` falso); `./e2e.ps1` desinstala la app al terminar (reinstalar con `./gradlew installDebug` antes de comprobar a mano con `adb`).

## Siguiente paso
1. PRs **#1-#6 mergeados** en `develop`; **PR #7** (`buscar-y-filtrar-recetas`) abierto, con la rama por actualizar tras el merge de `eliminar-receta`.
2. Siguiente funcionalidad dependency-ready: `revisar-identificadores-en-ingles` (sin spec). Flujo de `AGENTS.md`: `feature-spec` → `feature-implementer` → `feature-validator` → `accepted`.
3. Nota de shell: en PowerShell 5.1 ejecutar los scripts con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File <script>.ps1 > .harness\<x>.log 2>&1"` para evitar el falso fallo de `init` por el stderr de `adb shell monkey`.

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: RK02 (el modelo de detección MediaPipe está por validar: spike antes del Escáner), S05 (la API key vive en el dispositivo: hoy `UnconfiguredAiKeyProvider` devuelve `null` y «Generar con IA» avisa sin bloquear hasta `configurar-acceso-a-ia`) y S07 (publicar exige título, foto, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila inválida). Riesgos y supuestos ya cerrados: en `docs/progress-history.md`.

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
