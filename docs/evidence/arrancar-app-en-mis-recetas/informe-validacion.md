# Informe de validación — `arrancar-app-en-mis-recetas`

Fecha: 2026-10-17 · Cuarta revalidación (tras el arreglo de B-3) · Veredicto: **VALIDADA** · Estado final: `validated`

Entorno: Windows 11 · emulador `Pixel_10_Pro` (API 37, 480 dpi) headless · JDK corretto-17.0.20 · AGP 8.13.2 / Gradle 8.14.3.

## Resumen de ejecuciones

| Check | Comando | Resultado | Exit |
|---|---|---|---|
| init | `./init.sh` (y `./init.ps1` como referencia) | **pasa** (compila, instala y abre) | 0 (init.sh) / 1 (init.ps1, ver nota) |
| lint | `./lint.ps1` | **pasa** (`BUILD SUCCESSFUL`, 0 errores) | 0 |
| unit | `./unit.ps1` | **pasa** (2 tests) | 0 |
| e2e | `./e2e.ps1` | **pasa** (7 tests, 0 fallos) | 0 |

Escenario de checkout fresco reproducido: se **eliminó `local.properties`** y se ejecutó `./init.ps1`, que lo regeneró sin escapar (`sdk.dir=C:/Users/Daniel/AppData/Local/Android/Sdk`). Aun así `lint` queda verde gracias al arreglo de B-3.

## B-1, B-2 y B-3 (anteriores) — RESUELTOS ✅

- **B-1** (grafo de dependencias con `compileSdk 35` + JBR JDK 25): `koin` 4.1.1, `lifecycle` 2.9.3 y `org.gradle.java.home` a JDK 17. `:app:checkDebugAarMetadata` pasa.
- **B-2** (compilación Kotlin): `MisRecetasRoute.kt` en `package com.whatsforlunch.app.ui.misrecetas` y `@OptIn(ExperimentalTextApi::class)` en `Type.kt`. `:app:compileDebugKotlin` compila.
- **B-3** (lint): `gradle.properties:4` con `:` escapado (`C\:/...`) y `lint.xml` que ignora `local.properties` en `PropertyEscape` (referenciado desde `app/build.gradle.kts` con `lint { lintConfig = rootProject.file("lint.xml") }`). Reproducido con el `local.properties` sin escapar regenerado por `init.ps1`: **`lint` en verde, 0 errores**.

## Nota de entorno (no afecta al veredicto)

`./init.ps1` devuelve exit 1 como **falso negativo**: `adb shell monkey` escribe su salida en stderr y `$ErrorActionPreference = "Stop"` lo convierte en `NativeCommandError` en Windows PowerShell 5.1 (el script del harness no se toca). El objetivo real de `init` (compila, instala y abre) se cumple y se verificó con el script equivalente `./init.sh` (exit 0):

```
Installing APK 'app-debug.apk' on 'Pixel_10_Pro(AVD) - 17' for :app:debug
Installed on 1 device.
BUILD SUCCESSFUL
==> Entorno listo
App com.whatsforlunch.app en ejecucion (pid 4356) sobre el dispositivo de adb
```
Independientemente: `pm path com.whatsforlunch.app` → instalado y `dumpsys` → `MainActivity` en primer plano.

## Fidelidad visual (tokens de `uxdocs/DESIGN.md`)

El prototipo (`uxdocs/chefencasa_mis_recetas_y_crud/code.html`) no dibuja el estado vacío (laguna `RK04` declarada); se compara contra los tokens y pautas de `DESIGN.md`. Muestreo de píxeles sobre las capturas (API 37, 480 dpi):

| Elemento | Token | Implementación (muestreo) | Veredicto |
|---|---|---|---|
| Fondo / superficie / app bar | `surface #fbf9f5` | `#FBF9F5` (px 20,500 · 640,250) | igual |
| Botón primario «Crear receta» | `primary #a83212` | `#A83212` | igual |
| Botón tonal «Escanear la Despensa» | `secondary-container #82f5c1` | `#82F5C1` | igual |
| Tipografías | Plus Jakarta Sans / Inter | `Type.kt` (fuentes en `res/font/`) | igual |
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

Los 11 RF pasan su verificación.

## Alcance

`git diff` frente al commit del scaffold: `gradle.properties` (ruta de JDK escapada), `app/build.gradle.kts` (`lint { lintConfig = ... }`) y nuevo `lint.xml`; más `PROGRESS.md` y `docs/progress-history.md`. Coherente con la Parte B; el andamiaje del harness no se ha tocado; sin secretos ni fotos reales.

## Veredicto

**`validated`** — init/lint/unit/e2e en verde y los 11 RF verificados en emulador API 37 con evidencia capturada. Checkboxes de la spec marcados.
