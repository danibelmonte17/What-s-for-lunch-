# Informe de validación — `arrancar-app-en-mis-recetas`

Fecha: 2026-10-16 · Tercera revalidación tras el arreglo de B-2 · Veredicto: **A RETRABAJAR (bug B-3: `lint`)** · Estado final: `in_progress`

Entorno: Windows 11 (Windows PowerShell 5.1) · emulador `Pixel_10_Pro` (API 37, 480 dpi) headless · JDK corretto-17.0.20 · AGP 8.13.2 / Gradle 8.14.3.

## Resumen de ejecuciones

| Check | Comando | Resultado | Exit |
|---|---|---|---|
| init | `./init.ps1` | **falla** (falso negativo: `monkey` escribe en stderr) | 1 |
| lint | `./lint.ps1` | **falla** (2 `PropertyEscape`) | 1 |
| unit | `./unit.ps1` | pasa (`BUILD SUCCESSFUL`, 2 tests) | 0 |
| e2e | `./e2e.ps1` | pasa (7 tests, 0 fallos) | 0 |

La app compila, se instala y abre; los dos `verify` pasan. El único bloqueo real es `lint`.

## B-1 y B-2 (anteriores) — RESUELTOS ✅

- B-1 (compileSdk/grafo y JDK 25): `koin` 4.1.1 y `lifecycle` 2.9.3 permiten compilar con `compileSdk 35`; `:app:checkDebugAarMetadata` pasa.
- B-2 (compilación Kotlin): `MisRecetasRoute.kt` ya declara `package com.whatsforlunch.app.ui.misrecetas` (coincide con `AppNavHost.kt`) y `Type.kt` usa `@OptIn(ExperimentalTextApi::class)`. `:app:compileDebugKotlin` compila.

## Bug bloqueante (B-3): `lint` no pasa (`./lint.ps1` exit 1)

- Check afectado: `lint`. No afecta a ningún RF concreto (es la puerta de calidad general).
- Esperado (Parte B, «Plan de verificación»): `./lint.ps1` verde.
- Observado: **2 errores** `PropertyEscape`, 18 warnings:

  ```
  gradle.properties:4: Error: Windows file separators (\) and drive letter separators (':')
    must be escaped (\\) in property files; use C\:/Users/Daniel/.jdks/corretto-17.0.20 [PropertyEscape]
    org.gradle.java.home=C:/Users/Daniel/.jdks/corretto-17.0.20
  local.properties:1: Error: ... use C\:/Users/Daniel/AppData/Local/Android/Sdk\r [PropertyEscape]
    sdk.dir=C:/Users/Daniel/AppData/Local/Android/Sdk
  > Task :app:lintDebug FAILED
  ```

- Causas:
  1. `gradle.properties:4` (archivo del scaffold, fijado en el arreglo de B-1): la ruta de `org.gradle.java.home` no escapa el `:` del disco.
  2. `local.properties:1` (generado, gitignored): `init.ps1` lo escribe con la ruta sin escapar (`$SdkDir -replace "\\","/"`), así que cualquier ejecución de `init.ps1` en un checkout sin `local.properties` produce este error de lint.
- Verificación del alcance: con `local.properties` ausente y `ANDROID_HOME` definido, `lint` baja a **1 error** (solo `gradle.properties`). Es decir, el error de `gradle.properties` es responsabilidad del scaffold; el de `local.properties` lo genera el harness.
- Cómo reproducir (workdir = worktree):
  ```
  ./init.ps1          # crea local.properties sin escapar
  ./lint.ps1          # exit 1, 2 errores PropertyEscape
  ```
- Severidad: **bloqueante** para `lint`. Área probable: `gradle.properties`, y (para el segundo error) config de `lint` o el generador de `local.properties`.
- Siguiente acción: re-ejecutar `feature-implementer` para escapar la ruta en `gradle.properties` (`C\:/Users/...`) y, para que `lint` sea verde en un checkout fresco, evitar el error del `local.properties` generado (p. ej. ajustando la verificación de lint sobre ese archivo generado).

## Observación (no bloqueo de código): `./init.ps1` exit 1 por un falso negativo

`init.ps1` aborta en el paso de lanzamiento porque `adb shell monkey` escribe su salida en **stderr** y el script usa `$ErrorActionPreference = "Stop"`, que en Windows PowerShell 5.1 convierte ese stderr en `NativeCommandError` (en PowerShell 7 no ocurre). El objetivo real de `init` **sí** se cumple (verificado de forma independiente):

```
> Task :app:installDebug
Installing APK 'app-debug.apk' on 'Pixel_10_Pro(AVD) - 17' for :app:debug
Installed on 1 device.
BUILD SUCCESSFUL
```
`adb shell monkey ...` → exit 0; `pm path com.whatsforlunch.app` → instalado; `dumpsys` → `topResumedActivity=.../.MainActivity`.

## Fidelidad visual (tokens de `uxdocs/DESIGN.md`)

Prototipo (`uxdocs/chefencasa_mis_recetas_y_crud/code.html`) no dibuja el estado vacío (laguna `RK04` declarada); se compara contra los tokens y pautas de `DESIGN.md`. Muestreo de píxeles sobre las capturas (API 37):

| Elemento | Token | Implementación (muestreo) | Veredicto |
|---|---|---|---|
| Fondo / superficie / app bar | `surface #fbf9f5` | `#FBF9F5` (px 20,500 · 640,250) | igual |
| Botón primario «Crear receta» | `primary #a83212` | `#A83212` | igual |
| Botón tonal «Escanear la Despensa» | `secondary-container #82f5c1` | `#82F5C1` | igual |
| Tipografías | Plus Jakarta Sans / Inter | `Type.kt` (fuentes empaquetadas en `res/font/`) | igual |
| Objetivo táctil primario | ≥48 dp | 156 px / 3.0 = 52 dp | igual |
| Objetivo táctil secundario | ≥48 dp | 144 px / 3.0 = 48 dp | igual |

Sin divergencias materiales. Inventario `uiautomator` (es): «Mis Recetas», «Todavía no tienes recetas», «Crea tu primera receta o escanea la Despensa para empezar.», «Crear receta», «Escanear la Despensa». (en): «My Recipes», «No recipes yet», «Create your first recipe or scan your Pantry to get started.», «Create recipe», «Scan your Pantry».

## RF / trazabilidad (Parte A)

| RF | Verificación | Resultado | Evidencia |
|---|---|---|---|
| RF-1 | verify 1 (instalar y abrir) | pasa | `installDebug` + `dumpsys` MainActivity resumed |
| RF-2 | capas + unit sin pantalla | pasa | `unit` 2 tests verdes |
| RF-3 | captura vs tokens | pasa | `api37-mis-recetas-vacio(-en).png` + muestreo |
| RF-4 | verify 1 | pasa | e2e `appOpensInMisRecetasWithEmptyState` |
| RF-5 | verify 1 | pasa | e2e + `uiautomator` |
| RF-6 | E2E acción → destino provisional | pasa | e2e `createRecipeOpensProvisionalFormAndReturns` |
| RF-7 | E2E acción → destino provisional | pasa | e2e `scanPantryOpensProvisionalScannerAndReturns` |
| RF-8 | E2E vuelta atrás | pasa | incluida en los dos tests anteriores |
| RF-9 | E2E es/en de todos los textos | pasa | e2e `MisRecetasLocalizationE2ETest` (5 tests) |
| RF-10 | verify 2 | pasa | e2e `englishEmptyStateShowsEnglishTexts` + `uiautomator` en-US |
| RF-11 | E2E idioma no soportado | pasa | e2e `unsupportedLocaleFallsBackToSpanish` |

Los 11 RF pasan su verificación. El freno a `validated` es el check `lint` (B-3).

## Alcance

`git diff`: scaffold Android (Gradle, módulo `app`, tema, Mis Recetas, tests) más `CONTEXT.md`, `docs/technical-discovery.md`, `PROGRESS.md`, `feature_list.json`, `gradle/libs.versions.toml` y `gradle.properties`. Coherente con la Parte B; sin secretos ni fotos reales. Spec intacta (ningún checkbox marcado).

## Veredicto

`in_progress` — B-1 y B-2 resueltos; `unit` y `e2e` verdes y los 11 RF pasan, pero `lint` falla por B-3 (2 errores `PropertyEscape`). Pendiente de arreglo por `feature-implementer` antes de poder marcar `validated`.
