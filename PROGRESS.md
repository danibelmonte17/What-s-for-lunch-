# Progreso — What's for Lunch?

## Estado verificado
Solo lo comprobado, no lo que se cree. El detalle por feature vive en `docs/evidence/<feature-id>/` y la crónica de sesiones en `docs/progress-history.md` (solo se añade; no se lee entera salvo la última entrada).
- **Harness**: `init.ps1` exit 0 tras reintentar la carrera conocida de `pidof` · `lintDebug` 0 errores · `testDebugUnitTest` **19/19** · `connectedDebugAndroidTest` **61/61**. Último medido: sesión 41 sobre `Pixel_10_Pro` (API 37).
- **Funcionalidades `accepted`: 7 de 26** — `arrancar-app-en-mis-recetas`, `crear-receta`, `afinar-fidelidad-visual-del-formulario`, `sincronizar-spec-de-crear-receta`, `editar-receta`, `eliminar-receta` y `buscar-y-filtrar-recetas` (reabierta y re-aceptada en la sesión 41). Sin `passing` ni `in_progress`. (Las sesiones anteriores llamaban `validated` a `accepted`.)
- **Últimos cambios (sesión 41)**: retrabajo visual V1-V3 de Mis Recetas: buscador relleno, chips con iconos/tintes/sombra y tarjeta «sin resultados» con acción `secondary`; E2E amplía la estructura visual sin regresión funcional. `assembleDebug` también pasa.
- **Validación visual completada (sesión 41)**: S12 en verde con el `feature-validator` — dos capturas es renovadas en `docs/evidence/buscar-y-filtrar-recetas/`, muestreo de píxeles con todos los roles exactos y `uiautomator` (buscador 48 dp, chips 48 dp táctiles/píldora 38 dp, tarjeta con círculo 56 dp y botón 36/48 dp). Veredicto `accept`: `docs/validations/buscar-y-filtrar-recetas.md`.
- **Entorno**: AVD único `Pixel_10_Pro` (API 37; el `Pixel_10a` ya no existe); `adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools` (y `...\emulator` para el AVD). Build verde con AGP 8.13.2 + Room 2.8.5 + KSP 2.3.12 y JDK 17 fijado en `gradle.properties` (AGP no arranca con el JBR de Android Studio, JDK 25). `local.properties` se genera sin escapar el `:` y `lint.xml` lo excluye de `PropertyEscape`. Capturas en español con locale por aplicación (`cmd locale set-app-locales … --locales es-ES`, revertido al terminar).
- **Incidencias abiertas del entorno**: el picker del MediaStore devuelve un ítem inexistente (`FileNotFoundException: No item at content://media/external/file/178`), así que no se pueden crear recetas con foto desde la UI (RF-8 se verificó con `PhotoStore` falso); `./e2e.ps1` desinstala la app al terminar (reinstalar con `./gradlew installDebug` antes de comprobar a mano con `adb`).

## Siguiente paso
1. `buscar-y-filtrar-recetas` **cerrada** (`accepted`, sesión 41): PR #7 fusionado en `develop` (`c9084e7`), retrabajo visual V1-V3 con validación independiente `accept` (lint 0 errores, unit 19/19, e2e 61/61, S12 en emulador); el commit único de la feature lo hace el orquestador al cerrar el flujo.
2. Tras cerrar esa validación, la siguiente feature dependency-ready es `revisar-identificadores-en-ingles` (sin spec).
3. Nota de shell: en PowerShell 5.1 ejecutar los scripts con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File <script>.ps1 > .harness\<x>.log 2>&1"` para evitar el falso fallo de `init` por el stderr de `adb shell monkey`.

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: RK02 (el modelo de detección MediaPipe está por validar: spike antes del Escáner), S05 (la API key vive en el dispositivo: hoy `UnconfiguredAiKeyProvider` devuelve `null` y «Generar con IA» avisa sin bloquear hasta `configurar-acceso-a-ia`) y S07 (publicar exige título, foto, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila inválida). Riesgos y supuestos ya cerrados: en `docs/progress-history.md`.

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
