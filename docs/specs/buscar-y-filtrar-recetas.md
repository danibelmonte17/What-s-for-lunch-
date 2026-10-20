# Feature Implementation Spec: Buscar y filtrar recetas

## Source Feature

- `id`: buscar-y-filtrar-recetas
- `area`: funcional
- `depends_on`: crear-receta (`accepted`)
- `status`: `not_started` — reabierta el 2026-10-20 a petición del usuario (venía de `validated`/`accepted` del harness antiguo) para corregir la fidelidad visual de sus controles frente a `uxdocs/`
- `source`: `feature_list.json`

## Goal

Mis Recetas debe permitir localizar cualquier Receta en pocos segundos: un buscador textual sobre título e ingredientes (comparación normalizada, debounce de 300 ms) combinable con un filtro por Momento culinario de selección única (Todas, Desayuno, Almuerzo, Cena, Postre), con estado «sin resultados» y acción de restablecer. El comportamiento funcional (escenarios S1-S11) ya está implementado y se verificó en los ciclos 1-6 (`docs/evidence/buscar-y-filtrar-recetas/informe-validacion.md`); esta pasada lo re-verifica sin regresión y corrige la fidelidad visual.

Motivo de la reapertura (petición del usuario, 2026-10-20): los controles de esta feature no se ajustan visualmente al prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html`. La **fidelidad visual frente a `uxdocs/` pasa a ser un requisito verificable** (V1-V3, «Visual Design Impact»): buscador, chips de momento y estado «sin resultados» deben calcar atributo a atributo la composición, los colores por rol, las formas, la iconografía y la tipografía del prototipo, comprobable por muestreo de píxeles y `uiautomator` en el emulador.

## Non-Goals

- Filtros por tiempo y dificultad, su drawer y el botón `tune` del prototipo (el buscador va a ancho completo).
- Contador de resultados del prototipo («X recetas disponibles»).
- Ordenación por relevancia: se conserva el orden actual de la lista.
- Badge de Coincidencia (`mostrar-badge-de-coincidencia`) y acciones CRUD de la tarjeta («Editar» es `editar-receta`, «Eliminar» es `eliminar-receta`, «Cocinar Ahora» de otras features).
- Fidelidad visual de la tarjeta y del resto de la pantalla: la de las tarjetas la cubre `afinar-fidelidad-visual-del-formulario` (`accepted`); cabecera, «Crear», contador y barra inferior pertenecen a otras features. Esta feature es fiel solo en sus tres bloques (V1-V3).
- Búsqueda o filtros en otras pantallas (Escáner, Lista de la Compra, Pinche); sugerencias, autocompletado o historial del buscador.
- Persistencia de búsqueda y filtro tras cerrar la app: se restablecen a los valores iniciales (Q-buscar-y-filtrar-recetas-5).
- Filtro de momento múltiple, comparación sin normalizar y debounce distinto de 300 ms (Q-buscar-y-filtrar-recetas-1 a -3).
- Copy de la interfaz: se conservan los textos es/en ya validados en `strings.xml`; la fidelidad exigida es visual (composición, color, forma, iconografía, tipografía). El copy del prototipo queda fuera porque cada spec propone su copy (`Q-arrancar-app-en-mis-recetas-5`); si el usuario quiere calcar también el copy, será una decisión explícita aparte.

## Job Story

Cuando tengo varias Recetas guardadas en Mis Recetas,
quiero buscarlas por nombre o ingrediente y filtrarlas por momento culinario,
para encontrar rápidamente la que quiero cocinar.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P31`): busca y filtra sus Recetas, Borradores y Publicadas. Nada de esta feature exige permisos adicionales.

## Acceptance Scenarios

### Scenario 1: Filtrar por momento culinario (verify 1 de `feature_list.json`)

Given tres Recetas con momentos culinarios distintos (Cena, Almuerzo, Desayuno) en Mis Recetas
When el usuario selecciona el chip «Cena»
Then solo aparece la Receta de Cena y los demás chips quedan deseleccionados

### Scenario 2: Buscar por ingrediente (verify 2 de `feature_list.json`)

Given varias Recetas, una con el ingrediente «Pollo»
When el usuario escribe «pollo» en el buscador
Then aparecen solo las Recetas con ese texto en el título o en algún nombre de ingrediente

### Scenario 3: Combinar búsqueda y filtro

Given texto en el buscador y un momento culinario elegido
When se aplica la búsqueda tras el debounce
Then solo aparecen las Recetas que cumplen ambos criterios

### Scenario 4: Debounce de 300 ms

Given el buscador con foco
When el usuario teclea varias pulsaciones seguidas
Then el texto se aplica una sola vez, 300 ms después de la última pulsación

### Scenario 5: Comparación normalizada

Given un título o un nombre de ingrediente con mayúsculas, tildes y espacios exteriores
When el usuario escribe la variante sin tildes ni espacios
Then la Receta coincide por subcadena normalizada

### Scenario 6: Borrar el buscador con filtro activo

Given un momento elegido y texto en el buscador
When el usuario vacía el buscador
Then siguen apareciendo solo las Recetas del momento elegido

### Scenario 7: Sin resultados y restablecer

Given criterios de búsqueda y filtro sin ninguna coincidencia
When se muestra el estado «sin resultados»
Then el buscador y el filtro siguen visibles, el estado muestra su mensaje y la acción, y al pulsar «Ver todas las recetas» el buscador queda vacío y «Todas» seleccionado

### Scenario 8: Colección vacía

Given Mis Recetas sin ninguna Receta
When se abre la pantalla
Then se muestra el estado vacío de Mis Recetas con sus acciones y sin buscador ni filtro (Q-buscar-y-filtrar-recetas-4)

### Scenario 9: Valores iniciales y selección única

Given la app recién iniciada
When se abre Mis Recetas
Then el buscador está vacío y «Todas» está seleccionado, y al elegir un momento el activo previo se deselecciona (Q-buscar-y-filtrar-recetas-1)

### Scenario 10: Estado en memoria

Given búsqueda y filtro activos
When el usuario navega al formulario y vuelve a Mis Recetas
Then se conservan; al cerrar y reabrir la app vuelven a los valores iniciales (Q-buscar-y-filtrar-recetas-5)

### Scenario 11: Orden y estados de Receta conservados

Given Recetas en estado Borrador y Publicada con un orden dado
When se aplican búsqueda y filtro
Then aparecen ambos estados y se conserva el orden de la lista

### Scenario 12: Fidelidad visual de los controles (V1-V3)

Given Mis Recetas con Recetas (lista y estado «sin resultados»)
When se captura la pantalla en un emulador API 30+ y se compara con `uxdocs/chefencasa_mis_recetas_y_crud/code.html`
Then buscador, chips de momento y estado «sin resultados» cumplen V1-V3, comprobado por muestreo de píxeles de los roles de color y por `uiautomator` (bloques, iconos, textos y objetivos ≥ 48 dp)

## Repository Research

### Files Inspected

- `feature_list.json` — `user_visible_behavior`, `steps` y `verification` de la feature; `crear-receta` en `accepted`.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — UI actual: `OutlinedTextField` para el buscador, `FilterChip` sin icono para los momentos y `NoResults` con botón `primary`; `MisRecetasTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasViewModel.kt` — `MisRecetasUiState`, `debounce(300)` solo sobre el texto, filtro al instante, estado en memoria.
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/RecipeSearchFilter.kt` — filtrado y normalización (NFD, sin tildes, minúsculas, trim).
- `app/src/main/java/com/whatsforlunch/app/domain/misrecetas/MisRecetasState.kt` — `Empty`/`WithRecipes`/`NoResults`.
- `app/src/main/java/com/whatsforlunch/app/domain/model/MealMoment.kt`, `Recipe.kt` — momentos culinarios y modelo.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — textos es/en actuales (hint, «Todas», sin resultados, restablecer).
- `app/src/test/java/com/whatsforlunch/app/domain/RecipeSearchFilterTest.kt`, `.../MisRecetasStateTest.kt`, `.../ui/misrecetas/MisRecetasSearchDebounceTest.kt` — unit actuales (19 tests de suite tras el merge).
- `app/src/androidTest/java/com/whatsforlunch/app/BuscarYFiltrarRecetasE2ETest.kt` (7 tests) y `EditarRecetaE2ETest.kt` — patrón E2E con `InMemoryRecipeRepository` y pantalla real.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `kotlinx-coroutines-test` y `material-icons-extended` ya presentes.
- `app/src/main/java/com/whatsforlunch/app/ui/theme/Color.kt`, `Type.kt`, `Spacing.kt`, `WarmShadow.kt` — tokens disponibles: roles de color, `WarmTypography`, `WarmRadius.md` = 12 dp, `warmShadow`.
- `uxdocs/chefencasa_mis_recetas_y_crud/code.html` y `screen.png` — prototipo: buscador, chips de momento con icono y estado sin resultados.
- `uxdocs/DESIGN.md`, `DESIGN.md` — tokens YAML (mandan, `P17`), prosa orientativa (`P16`), sombra ámbar-carbón y excepción compacta de botones.
- `docs/evidence/buscar-y-filtrar-recetas/informe-validacion.md` — ciclo 6: funcional en verde y 3 divergencias de fidelidad declaradas «no bloqueantes» (buscador, chips sin icono, botón del estado sin resultados).
- `docs/specs/afinar-fidelidad-visual-del-formulario.md` — precedente de fidelidad verificable (muestreo de píxeles + `uiautomator`) y de la excepción compacta (Q-editar-receta-6).
- `docs/risks-and-open-questions.md` — Q-buscar-y-filtrar-recetas-1 a -5, todas resueltas.

### Existing Patterns To Follow

- MVVM + StateFlow con reglas en `domain/` sin dependencias de UI; Koin inyecta `MisRecetasViewModel(get())`.
- Fidelidad visual como requisito verificable por muestreo de píxeles y `uiautomator` sobre capturas del emulador (precedente `afinar-fidelidad-visual-del-formulario`, NFR-4).
- Acciones compactas: 36 dp de altura visual con área táctil ≥ 48 dp (excepción aprobada, Q-editar-receta-6).
- `WarmTypography` ya mapea los roles del sistema: `headline-sm` → `headlineSmall` (18 sp), `body-sm` → `bodySmall` (14 sp), `label-md` → `labelMedium` (13 sp), `label-sm` → `labelSmall` (11 sp).
- E2E Compose con `testTag` estables en `MisRecetasTestTags` y locale inyectado en el test.
- Comandos: scripts `init`/`lint`/`unit`/`e2e` sobre `./gradlew` (`docs/technical-discovery.md`).

### Current Gaps

- Divergencias visuales a cerrar (V1-V3): el buscador es un `OutlinedTextField` M3 con borde en lugar del contenedor relleno del prototipo; los chips no llevan icono ni sombra y usan `FilterChip` de M3 (dibuja su propio contorno y puede dibujar check al seleccionar); el estado «sin resultados» no es tarjeta y su acción va en `primary` en lugar de `secondary`.
- Los tests actuales no afirman la estructura visual (iconos por chip, jerarquía del estado sin resultados).
- Sin pipeline automatizado de muestreo de píxeles ni screenshot testing: la comprobación visual la ejecuta el validador con capturas + `adb`/`uiautomator` (igual que en `afinar-fidelidad-visual-del-formulario`).

## Technical Approach

- **Comportamiento (ya implementado; no se cambia)**: `RecipeSearchFilter.filterRecipes(recipes, query, moment)` normaliza (minúsculas, sin tildes, trim) y aplica «contiene en título o en nombre de ingrediente» Y «momento igual» (`null` = Todas), conservando el orden de entrada e incluyendo Borradores y Publicadas. `MisRecetasViewModel` usa `debounce(300)` solo sobre el texto (el filtro se aplica al instante al tocar un chip), expone `searchQuery`/`mealMomentFilter`/`visibleRecipes` y deriva `Empty`/`WithRecipes`/`NoResults` con `misRecetasStateOf`. El estado vive en el ViewModel (memoria: se conserva al navegar y se restablece al cerrar la app).
- **UI (retrabajo de los tres bloques de V1-V3)**: sustituir el `OutlinedTextField` por el campo relleno del prototipo; dibujar los chips de momento como píldoras con icono fijo por momento y sin checkmark (previsiblemente `Surface` + `Row` en lugar de `FilterChip`); rehacer `NoResults` como tarjeta con icono circular y botón `secondary`. Se conservan los `testTag` existentes (`SEARCH_FIELD`, `FILTER_ROW`, `FILTER_CHIP_ALL`, `FILTER_CHIP_*`, `NO_RESULTS`, `RESET_FILTERS`) y se añaden solo los necesarios para afirmar los iconos.
- **Textos**: sin cambios; el copy es/en ya validado se conserva (Non-Goals).
- **Tests**: el unit existente cubre el comportamiento (S3-S5, S9, S11) y no cambia salvo firmas. `BuscarYFiltrarRecetasE2ETest` se mantiene (S1-S11) y se amplía con la estructura de V1-V3 (presencia de los cinco chips con icono y etiqueta, buscador relleno, tarjeta de sin resultados con su acción). El muestreo de píxeles de S12 lo ejecuta el validador.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/MisRecetasScreen.kt` — modificar; buscador (V1), chips con icono (V2) y estado «sin resultados» (V3).
- `app/src/androidTest/java/com/whatsforlunch/app/BuscarYFiltrarRecetasE2ETest.kt` — modificar; afirma la estructura visual nueva sin perder los 7 tests actuales.
- `app/src/test/java/com/whatsforlunch/app/...` — no se prevén cambios (comportamiento intacto); tocar solo si cambian firmas.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — no se prevén cambios (copy ya validado).
- `app/src/main/java/com/whatsforlunch/app/ui/theme/...`, `domain/misrecetas/...`, `MisRecetasViewModel.kt`, `gradle/...` — no se prevén cambios.
- `docs/validations/buscar-y-filtrar-recetas.md` y `docs/evidence/buscar-y-filtrar-recetas/*.png` — crear/actualizar al validar (evidencia).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (fidelidad 1:1 para V1-V3, a petición del usuario 2026-10-20; ante conflicto de prosa mandan los tokens YAML de `uxdocs/DESIGN.md`, `P17`)
- Screens or states affected: Mis Recetas con Recetas (buscador + chips sobre la lista) y estado «sin resultados»; el estado vacío no cambia
- New design artifact required: no — el prototipo cubre los tres bloques

Requisitos de fidelidad verificables (roles de `uxdocs/DESIGN.md`; objetivo táctil ≥ 48 dp en todos los controles, `DESIGN.md`):

**V1 · Buscador** — fila sobre la lista, a ancho completo (el botón `tune` del prototipo abre el drawer de tiempo/dificultad, fuera de alcance):

- Contenedor relleno `surface-container-lowest` (#FFFFFF), radio 0.75 rem (12 dp), sombra cálida `shadow-sm` ámbar-carbón (`uxdocs/DESIGN.md` §Ambient Shadow Character), sin borde `outline`.
- Icono `search` de 20 dp en `on-surface-variant` (#59413B) a la izquierda (12 dp de margen); texto `body-sm` (14 sp, Inter) en `on-surface` (#1B1C1A); placeholder en `on-surface-variant` al 60 %.
- Padding interno 12 dp vertical, 40 dp a la izquierda (icono) y 16 dp a la derecha; altura mínima 48 dp.

**V2 · Chips de momento** — fila desplazable horizontalmente, separación 8 dp y margen interno 16 dp:

- Chip en píldora (radio `full`), padding 16 dp horizontal y 10 dp vertical, sombra `shadow-sm`, icono de 18 dp + etiqueta `label-md` (13 sp, Plus Jakarta Sans SemiBold).
- Icono fijo por chip (visible también en el activo, sin checkmark): Todas `restaurant_menu`, Desayuno `bakery_dining`, Almuerzo `skillet`, Cena `ramen_dining`, Postre `icecream`.
- Chip activo: fondo `primary` (#A83212) con icono y texto `on-primary` (#FFFFFF).
- Chip inactivo: fondo `surface-container-lowest` (#FFFFFF), texto `on-surface` (#1B1C1A) e icono con el tinte del momento: Desayuno `tertiary` (#8D4B00), Almuerzo `primary` (#A83212), Cena `secondary` (#006C4A), Postre `tertiary-container` (#B15F00); «Todas» inactivo en `on-surface`.
- Altura visual del prototipo (~38 dp) con área táctil ≥ 48 dp (excepción compacta, como en Q-editar-receta-6).

**V3 · Estado «sin resultados»** — los controles de búsqueda y filtro siguen visibles; solo se sustituye la lista:

- Tarjeta `surface-container-lowest` (#FFFFFF) de radio 0.75 rem (12 dp) y sombra `shadow-sm`, centrada, con padding 40 dp y separación de 8 dp entre elementos.
- Icono `search_off` de 28 dp en `primary` dentro de un círculo de 56 dp con fondo `surface-container` (#EFEEEA).
- Título `headline-sm` (18 sp, Plus Jakarta Sans SemiBold) en `on-surface`; mensaje `body-sm` (14 sp, Inter) en `on-surface-variant`, con ancho máximo 320 dp.
- Acción «Ver todas las recetas»: botón compacto relleno `secondary` (#006C4A) con texto `on-secondary` (#FFFFFF), radio 0.75 rem, etiqueta `label-md`, ancho al contenido, altura visual ≈ 36 dp y área táctil ≥ 48 dp.

Divergencias que el ciclo 6 declaró «no bloqueantes» y que V1-V3 cierran: campo con borde `outline` en lugar del contenedor relleno; chips sin icono, sin sombra y con etiqueta en `on-surface-variant`; estado sin resultados sin tarjeta, con icono suelto en `on-surface-variant` y botón `primary`.

Copy fuera de la fidelidad (se conserva el validado): hint «Buscar por receta o ingrediente», título «No hay recetas que coincidan», mensaje «Prueba con otro texto o momento culinario.» y acción «Ver todas las recetas» (es/en).

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (el filtrado sigue en `domain/misrecetas/`).
- `CONSTRAINTS.md`: not needed — no existe; la fidelidad exigida es de esta feature. Si el usuario quiere que «1:1 con `uxdocs/`» sea regla general (hoy el material es orientativo, `P16`), se decide aparte y tocaría `DESIGN.md`.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — not needed (sin reglas `RD##` nuevas: el filtrado es presentación de Mis Recetas). `docs/risks-and-open-questions.md` — not needed salvo que surjan dudas nuevas (Q-buscar-y-filtrar-recetas-1 a -5 ya resueltas).

## Implementation Plan

1. Rehacer el buscador según V1 (contenedor relleno, icono, tipografía, padding).
2. Rehacer la fila de chips según V2 (píldora con icono por momento, colores activo/inactivo, sombra, área táctil).
3. Rehacer el estado «sin resultados» según V3 (tarjeta, icono circular, tipografía, botón `secondary`) manteniendo visibles buscador y filtro.
4. Actualizar y ampliar `BuscarYFiltrarRecetasE2ETest` (S1-S11 sin regresión + estructura de V1-V3); ajustar unit solo si cambian firmas.
5. Ejecutar el harness completo, capturar la evidencia y dejar la feature en `passing` lista para `feature-validator`.

## Implementation Tasks

- [x] T1: Buscador V1 en `MisRecetasScreen`. (S2-S7, S12)
- [x] T2: Chips V2 con icono y tintes por momento. (S1, S6, S9, S12)
- [x] T3: Estado «sin resultados» V3 con acción `secondary`. (S7, S12)
- [x] T4: E2E al día: los 7 tests actuales en verde y nuevas aserciones de estructura visual. (S1-S11)
- [x] T5: `init`/`lint`/`unit`/`e2e` en verde (unit ≥ 19 y e2e ≥ 60 tras el merge de `eliminar-receta`) y evidencia capturada. (S1-S12; la comparación de píxeles y nuevas capturas las realiza el validador según el Verification Plan).

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0. Es el gate de arranque estándar no bloqueante del estado actual del repo (compila, instala y abre la app en el emulador); **no debe arrancar procesos duraderos** y no se modifica. Artefactos de entorno conocidos: la carrera del `pidof` en `init.sh` (pasa al reintentar) y el exit 1 falso de `init.ps1` por el stderr de `adb shell monkey` en PowerShell 5.1 (ejecutar con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `RecipeSearchFilterTest`, `MisRecetasSearchDebounceTest` y `MisRecetasStateTest` (≥ 19 tests).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en emulador API 30+ con `BuscarYFiltrarRecetasE2ETest`:
  - verify 1 (S1): tres Recetas de distinto momento → filtrar por Cena → solo la de Cena.
  - verify 2 (S2): escribir «Pollo» en el buscador → solo las Recetas con ese ingrediente.
  - S3 combinado; S6 borrar buscador con filtro; S7 sin resultados + restablecer; S8 colección vacía; S9 valores iniciales y selección única; S10 navegar y volver + reinicio de la app; estructura de V1-V3 dentro de la suite.
  - Unit: S4 debounce con tiempo virtual (`kotlinx-coroutines-test`), S5 normalización y S11 orden + Borradores.
  - Regresión completa en verde (≥ 60 tests tras el merge) sin romper `crear-receta`, `editar-receta`, `eliminar-receta` ni `afinar-fidelidad-visual-del-formulario`.
- UI (solo el validador, S12): 2 capturas en emulador API 30+ (es) + muestreo de píxeles de los roles de V1-V3 + `uiautomator` (bloques, iconos, textos y objetivos ≥ 48 dp). Sin screenshot testing en el repo, el muestreo de píxeles es manual con `adb` (justificado por el precedente de `afinar-fidelidad-visual-del-formulario`).
- Cobertura E2E: sí se añade/actualiza, porque la feature cambia comportamiento visible; la fidelidad de píxeles queda fuera del E2E automatizado por no existir pipeline de capturas (ver arriba).
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/buscar-y-filtrar-recetas.md` — informe con `lint`/`unit`/`e2e` (nombres de test), verificación por escenario S1-S12 y comparación de fidelidad V1-V3; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-filtro-cena.png` — Mis Recetas con el filtro Cena aplicado (es); reemplaza a la captura homónima del ciclo 1.
- `docs/evidence/buscar-y-filtrar-recetas/mis-recetas-sin-resultados.png` — estado «sin resultados» con su acción de restablecer (es); reemplaza a la del ciclo 1.
- Presupuesto ≤ 4 artefactos y solo de esta feature (1 informe + 2 capturas + opcionalmente el log de `e2e` referenciado); al revalidar se reemplaza el set anterior.

## Validator Checklist

- [x] La implementación se queda dentro del alcance de esta feature.
- [x] Los escenarios de aceptación pasan (S1-S12).
- [x] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [x] Cobertura E2E persistente añadida o actualizada (o justificada su ausencia) y suite completa en verde.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento de producto ni trabajo de otras features.