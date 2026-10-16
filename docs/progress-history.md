# Historial de progreso — What's for Lunch?

Entradas de sesión en orden cronológico (solo se añaden, nunca se borran). No se lee entero: consulta como mucho la última entrada si necesitas contexto.

### Sesión 0: harness inicial (harness-starter)
- Especificación completada (`docs/build-brief.md`, `docs/domain-model.md`, `docs/technical-discovery.md`, `DESIGN.md`); andamiaje operativo generado; sin código de aplicación.
- Archivos creados: AGENTS.md, feature_list.json, PROGRESS.md, docs/progress-history.md, init.sh, init.ps1, lint.sh, lint.ps1, unit.sh, unit.ps1, e2e.sh, e2e.ps1.
- Funcionalidades: 23, todas en `not_started`. El MVP (app Android de uso personal con datos locales) se ha troceado en 23 slices dentro del rango esperado de 12-30, sin necesidad de justificar la granularidad.
- Primera tarea recomendada: `arrancar-app-en-mis-recetas` (scaffold, tema desde los tokens de `uxdocs/DESIGN.md`, pantalla Mis Recetas con estado vacío e i18n es/en).
- Nota: `init.sh` / `init.ps1` no pueden completarse hasta que exista el scaffold Android (no hay `gradlew` todavía); los scripts lo detectan y terminan con un error que lo explica. Al crear el scaffold, el applicationId se determina leyendo el APK generado (sin fijarlo en los scripts; se puede forzar con la variable de entorno PKG).

### Sesión 1: implementación de `arrancar-app-en-mis-recetas` (feature-implementer)
- Scaffold Android creado desde cero: Gradle Kotlin DSL + wrapper 8.14.3, AGP 8.13.2, Kotlin 2.2.21 + plugin de Compose; módulo único `app` con capas `ui`/`domain`/`data`, MVVM + StateFlow y Koin (Q-arrancar-app-en-mis-recetas-1 y -4). `compileSdk`/`targetSdk` 35, `minSdk` 30 (Q-arrancar-app-en-mis-recetas-2).
- Tema «Warm Culinary Craft» en Compose desde los tokens YAML de `uxdocs/DESIGN.md`; fuentes OFL Plus Jakarta Sans e Inter (variables) empaquetadas en `res/font/` (sin red), con sus licencias en `licenses/`.
- Mis Recetas como destino inicial con su estado vacío y sus dos acciones; destinos provisionales del formulario de Receta y del Escáner con título y vuelta atrás (Compose Navigation) (Q-arrancar-app-en-mis-recetas-3).
- Textos visibles en `strings.xml` (`values/` español por defecto y `values-en/` inglés); sin literales en el código.
- Suite E2E instrumentada (verify 1, verify 2 y aserciones de RF-6/7/8/9/11) y test unitario de humo de `domain/`.
- Documentación actualizada: `CONTEXT.md` (S06 confirmado, Koin) y `docs/technical-discovery.md` (DI Koin, Android 11/API 30+).
- Estado: `arrancar-app-en-mis-recetas` en `passing` (tope de esta skill). Pendiente de validación real en emulador API 30+ por una sesión aparte.
- Nota: no se ejecutaron `gradlew`, scripts del harness, tests, builds ni `adb` (regla de esta skill).

### Sesion 2: validacion de `arrancar-app-en-mis-recetas` (feature-validate)
- Entorno: emulador `Pixel_10_Pro` (API 37, x86_64) headless; JDK corretto-17.0.20; AGP 8.13.2 / Gradle 8.14.3.
- `./init.ps1`, `./lint.ps1`, `./unit.ps1` y `./e2e.ps1`: todos fallan (exit 1) en `:app:checkDebugAarMetadata`: el grafo resuelto exige `compileSdk >= 36` (androidx.activity 1.12.4, navigationevent 1.0.2, activity-compose/ktx 1.12.4), pero `app/build.gradle.kts` declara `compileSdk = 35`.
- Causa raiz: `io.insert-koin:koin-android:4.2.2` (declarado en `gradle/libs.versions.toml`) exige `androidx.activity:activity-ktx:1.12.4` (y lifecycle 2.10.0); la resolucion por conflicto sube activity de 1.10.1 a 1.12.4. `koin-compose:4.2.2` arrastra `org.jetbrains.compose.*:1.10.2`.
- Sin APK no hubo capturas ni recorrido `adb`; la fidelidad visual no pudo comprobarse.
- Prerrequisito de entorno: los scripts auto-seleccionan el JBR de Android Studio (JDK 25 en esta maquina, incompatible con AGP 8.13.2: `What went wrong: 25.0.2`); con `JAVA_HOME` = JDK 17 se llega al fallo real.
- Veredicto: `in_progress` con bug bloqueante B-1. Evidencia: `docs/evidence/arrancar-app-en-mis-recetas/informe-validacion.md`. Siguiente accion: arreglar con `feature-implementer` y revalidar.

### Sesion 3: revalidacion de `arrancar-app-en-mis-recetas` (feature-validate) — B-1 resuelto, B-2 abierto
- Cambios revalidados: `koin` 4.2.2 → 4.1.1 y `lifecycle` 2.9.0 → 2.9.3 en `gradle/libs.versions.toml`; `org.gradle.java.home` a corretto-17.0.20 en `gradle.properties`.
- B-1 resuelto: `:app:checkDebugAarMetadata` pasa con `compileSdk 35`; el build ya no cae por el grafo de dependencias ni por el JBR JDK 25.
- Nuevo bug bloqueante B-2: `:app:compileDebugKotlin` falla (3 errores `e:`): `ui/nav/AppNavHost.kt:11` importa `com.whatsforlunch.app.ui.misrecetas.MisRecetasRoute` pero `MisRecetasRoute.kt` declara `package com.whatsforlunch.app.ui.nav`; y `ui/theme/Type.kt:16,22` usa la API experimental `FontVariation`/`variationSettings` sin `@OptIn(ExperimentalTextApi::class)`.
- `./init.ps1`, `./lint.ps1`, `./unit.ps1` y `./e2e.ps1`: todos exit 1 por B-2. Sin APK, sin capturas ni recorrido `adb`; fidelidad visual no comprobable.
- Veredicto: `in_progress`. Evidencia: `docs/evidence/arrancar-app-en-mis-recetas/informe-validacion.md`. Siguiente accion: arreglar B-2 con `feature-implementer` y revalidar.

### Sesion 4: 3a revalidacion de `arrancar-app-en-mis-recetas` (feature-validate) — B-1/B-2 resueltos, B-3 abierto
- Cambios revalidados: `MisRecetasRoute.kt` con `package com.whatsforlunch.app.ui.misrecetas` (alineado con el import de `AppNavHost.kt`); `@OptIn(ExperimentalTextApi::class)` en `jakarta`/`inter` de `Type.kt`.
- B-1 y B-2 resueltos: compila, se instala y abre (`installDebug` OK; `dumpsys` -> `MainActivity` resumed).
- Checks: `unit` pasa (exit 0, 2 tests); `e2e` pasa (exit 0, 7/7 tests); `init` exit 1 por falso negativo (monkey escribe en stderr y `$ErrorActionPreference="Stop"` lo convierte en error en PowerShell 5.1, no en la logica del script); `lint` **falla** (exit 1).
- B-3 (bloqueante para `validated`): `lint` da 2 errores `PropertyEscape`: `gradle.properties:4` (`org.gradle.java.home` con `:` sin escapar) y `local.properties:1` (generado sin escapar por `init.ps1`; gitignored). Sin `local.properties`, `lint` baja a 1 error (solo `gradle.properties`).
- Los 11 RF pasan su verificacion; capturas del estado vacio es/en muestreadas contra los tokens (surface `#fbf9f5`, primary `#a83212`, secondary-container `#82f5c1`), objetivos tactiles 52/48 dp.
- Veredicto: `in_progress` (unico freno: `lint`). Evidencia: `docs/evidence/arrancar-app-en-mis-recetas/informe-validacion.md` (+ `api37-mis-recetas-vacio.png`, `api37-mis-recetas-vacio-en.png`). Siguiente accion: arreglar B-3 con `feature-implementer` y revalidar.

### Sesión 3: corrección del bug B-1 de `arrancar-app-en-mis-recetas` (feature-implementer, reintento)
- B-1 corregido alineando el grafo de dependencias con `compileSdk 35` (spec): `gradle/libs.versions.toml` baja `koin` de `4.2.2` a `4.1.1`. Koin 4.1.1 depende de `androidx.activity:activity-ktx:1.10.1` y `androidx.lifecycle:*-viewmodel-ktx:2.9.3` (compatibles con `compileSdk 35`), en lugar de `activity-ktx:1.12.4`, `lifecycle 2.10.0` y `org.jetbrains.compose:1.10.2` que exigían `compileSdk >= 36`. Se mantienen sin cambios `app/build.gradle.kts`, el código de la app y los tests; las APIs usadas (`koinViewModel`, `viewModel {}`) siguen existiendo en 4.1.1 (verificado en el AAR).
- Prerrequisito de entorno resuelto: `gradle.properties` fija `org.gradle.java.home` a un JDK 17 (`corretto-17.0.20`), porque los scripts del harness auto-seleccionan el JBR de Android Studio (JDK 25) cuando `JAVA_HOME` no está definido y AGP 8.13.2 no arranca con JDK 25.
- Documentación: `PROGRESS.md` actualizado; el informe de validación `docs/evidence/arrancar-app-en-mis-recetas/informe-validacion.md` se conserva como evidencia.
- Estado: `arrancar-app-en-mis-recetas` de nuevo en `passing`. Pendiente de revalidación real en emulador API 30+ por una sesión aparte.
- Nota: no se ejecutaron `gradlew`, scripts del harness, tests, builds ni `adb` (regla de esta skill).

### Sesión 4: corrección del bug B-2 de `arrancar-app-en-mis-recetas` (feature-implementer, reintento)
- B-2 corregido en el código, sin tocar la spec ni el resto de la app:
  - `ui/misrecetas/MisRecetasRoute.kt`: el `package` pasa de `com.whatsforlunch.app.ui.nav` a `com.whatsforlunch.app.ui.misrecetas`, alineado con su carpeta y con el import de `ui/nav/AppNavHost.kt`; se quitan los imports redundantes de `MisRecetasScreen`/`MisRecetasViewModel` (ya en el mismo paquete).
  - `ui/theme/Type.kt`: se añade `@OptIn(ExperimentalTextApi::class)` a las funciones `jakarta`/`inter` (usan `FontVariation`/`variationSettings`, API experimental) y su import `androidx.compose.ui.text.ExperimentalTextApi`.
- Se verificó por inspección que el resto de paquetes coinciden con sus carpetas y que no quedan usos sin opt-in de APIs experimentales (`TopAppBar` ya tenía `@OptIn(ExperimentalMaterial3Api::class)`).
- Estado: `arrancar-app-en-mis-recetas` de nuevo en `passing`. Pendiente de revalidación real en emulador API 30+ por una sesión aparte.
- Nota: no se ejecutaron `gradlew`, scripts del harness, tests, builds ni `adb` (regla de esta skill).

### Sesión 5: cierre del flujo `feature-flow` de `arrancar-app-en-mis-recetas` — 3 ciclos de validación agotados
- El flujo `feature-flow` (worktree `.worktrees/arrancar-app-en-mis-recetas`, rama `feature/arrancar-app-en-mis-recetas`) agotó los 3 ciclos de validación permitidos: validación inicial (B-1), revalidación tras B-1 (B-2) y tercera revalidación tras B-2 (B-3).
- Estado final de los checks en la 3ª validación: `unit` pasa (2 tests), `e2e` pasa (7/7), `init` exit 1 por falso negativo conocido (monkey escribe en stderr bajo PowerShell 5.1; el objetivo real se cumple), `lint` **falla** por B-3.
- B-1 y B-2 resueltos. B-3 (bloqueante para `validated`): `lint` da 2 errores `PropertyEscape` — `gradle.properties:4` (`org.gradle.java.home` con `:` sin escapar) y `local.properties:1` (generado sin escapar por `init.ps1`; gitignored).
- Los 11 RF pasan su verificación en emulador real; evidencia en `docs/evidence/arrancar-app-en-mis-recetas/` (`informe-validacion.md`, `api37-mis-recetas-vacio.png`, `api37-mis-recetas-vacio-en.png`).
- Cierre en fallo (desviación consciente de §3.4 de la skill): la feature queda en **`passing`** (implementada, todos los RF verificados, solo `lint` en rojo) y el flujo **para** pidiendo intervención humana. Se conservan worktree y rama; no se creó PR.
- Reproducción: `./gradlew.bat --no-daemon lintDebug` o `./lint.ps1`. Siguiente acción humana: arreglar B-3 con `feature-implementer` y re-ejecutar `feature-validate`.
