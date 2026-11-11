# Feature Implementation Spec: Mover el escaneo de la Lista a la Despensa

## Source Feature

- `id`: mover-escaneo-a-la-despensa
- `area`: funcional
- `depends_on`: gestionar-alimentos-de-la-despensa, anadir-faltantes-a-la-lista, agrupar-y-completar-la-lista, escanear-despensa-con-ia (todas `accepted`)
- `status`: not_started (al planificar, 2026-11-08)
- `source`: `feature_list.json`

## Goal

La Lista de la Compra deja de ofrecer el Escáner: retira su CTA «Escanear la Despensa» del estado vacío y cualquier otro acceso al Escáner, para que esa pantalla se centre en lo que falta por comprar. La Despensa —donde viven los Alimentos que el Escáner guarda— gana la entrada al Escáner como segundo FAB «Escanear alimentos» junto a «Añadir alimento», más el CTA correspondiente en su estado vacío. Ambas pantallas reordenan solo los elementos necesarios para acoplar el cambio (D1–D3). Los accesos al Escáner de Mis Recetas (FAB de cámara y CTA del estado vacío) se mantienen sin cambios.

## Non-Goals

- No tocar los accesos al Escáner de Mis Recetas: FAB de cámara (`MyRecipesTestTags.SCAN_PANTRY_FAB`) y CTA del estado vacío (`MyRecipesTestTags.SCAN_PANTRY`) siguen tal cual.
- No modificar el Escáner (`ScannerScreen`/`ScannerRoute`/`ScannerViewModel`): contenido, flujo, avisos, guardado y diálogo de salida intactos.
- No cambiar la navegación de entrada a las pantallas ni la top bar de Mis Recetas (sus acciones se mueven en `mover-opciones-a-bottom-bar`, feature aparte).
- No implementar `mover-opciones-a-bottom-bar` ni `crear-mi-perfil`; solo dejar constancia de la coordinación (ver Decisiones, D6).
- Sin cambios en Room, `domain/` ni reglas de negocio: la feature es de ubicación de accesos y orden de UI.
- Sin voz, sin IA nueva, sin permisos nuevos (la cámara ya se pide dentro del Escáner).

## Job Story

When estoy comprando con la Lista de la Compra o mirando lo que tengo en la Despensa,
I want to encontrar el escaneo de Alimentos en la Despensa (que es donde se guardan) y que la Lista no me distraiga con él,
so I can llenar la Despensa sin rodeos y mantener la Lista centrada en lo que falta por comprar.

## Users And Permissions

- Usuario único (MVP de uso personal, sin cuentas): misma funcionalidad en es/en; sin permisos nuevos (el de cámara se solicita dentro del Escáner, `RD05`/`RD06` intactos).

## Decisiones

- **D1 (usuario, 2026-11-08) — orden de los FABs de la Despensa**: los dos FABs se apilan en la esquina inferior derecha; **«Escanear alimentos» encima** y el existente **«Añadir alimento» abajo** (conserva su sitio). El nuevo FAB es un `ExtendedFloatingActionButton` con icono de cámara (`Icons.Outlined.PhotoCamera`, mismo que el FAB de Mis Recetas) y texto.
- **D2 (usuario, 2026-11-08) — estado vacío de la Lista de la Compra**: se retira el CTA «Escanear la Despensa» y queda **solo «Añadir ítem» como botón primario** en su hueco; el título y el mensaje del estado vacío se conservan sin cambios.
- **D3 (usuario, 2026-11-08) — estado vacío de la Despensa**: gana **dos CTAs** que replican los FABs: «Escanear alimentos» y «Añadir alimento» (este conserva su comportamiento actual de abrir el diálogo de alta).
- **D4 (derivada)** — textos y tags nuevos: clave `pantry_scan_food` («Escanear alimentos» / «Scan foods») en `values/` y `values-en/`, con tag `PantryTestTags.SCAN_FOOD` (FAB) y `PantryTestTags.SCAN_FOOD_CTA` (estado vacío). `action_scan_pantry` («Escanear la Despensa») **se conserva** porque la usa Mis Recetas; solo desaparece de la Lista.
- **D5 (derivada)** — vuelta a la Despensa tras guardar: el comportamiento actual de `ScannerRoute.onSaved` (navegar a `PANTRY` con `popUpTo(SCAN_PANTRY) { inclusive = true }`) se conserva y se añade `launchSingleTop = true` para que, cuando el Escáner se abre desde la Despensa, no quede una entrada duplicada de la Despensa en el back stack (ver Casos límite).
- **D6 (coordinación)** — `mover-opciones-a-bottom-bar` (`not_started`, no dependencia) no se implementa aquí. Esta feature no cambia cómo se llega a las pantallas, así que el cambio de FABs/estados vacíos es compatible con esa bottom bar cuando llegue; la coordinación pendiente se reduce a que, al reordenar, no se dependa de la top bar actual de esas pantallas (solo se retira el CTA de escaneo y se apilan FABs).

## Acceptance Scenarios

### Scenario 1: la Lista vacía ya no ofrece escanear (verify 1)

Given la Lista de la Compra está vacía
When la abro desde Mis Recetas
Then se muestra el estado vacío con su título y mensaje, con un único botón primario «Añadir ítem», y no aparece «Escanear la Despensa» ni ningún otro acceso al Escáner en la pantalla.

### Scenario 2: la Despensa abre el Escáner y vuelve con los Alimentos (verify 2)

Given la Despensa tiene alimentos y se muestra su listado
When pulso el FAB «Escanear alimentos»
Then se abre el Escáner; al guardar los Alimentos del Escaneo, la app vuelve a la Despensa y el listado los muestra (con la suma RD13 ya vigente).

### Scenario 3: orden de elementos y textos es/en (verify 3)

Given abro la Lista de la Compra y la Despensa
Then los elementos siguen el orden acordado (D1–D3: en la Despensa, FAB «Escanear alimentos» encima y «Añadir alimento» abajo; en el estado vacío de la Despensa, CTA «Escanear alimentos» y después «Añadir alimento»; en el estado vacío de la Lista, solo «Añadir ítem») y todos los textos nuevos existen en español e inglés con paridad de claves.

### Scenario 4: la Despensa vacía ofrece ambos accesos

Given la Despensa está vacía
When la abro
Then el estado vacío muestra su título y mensaje con dos CTAs: «Escanear alimentos» (abre el Escáner) y «Añadir alimento» (abre el diálogo de alta).

### Scenario 5: Mis Recetas conserva sus accesos al Escáner (regresión)

Given abro Mis Recetas con Recetas y sin Recetas
Then el FAB de cámara y el CTA del estado vacío «Escanear la Despensa» siguen funcionando y abren el Escáner; al guardar, el Escáner sigue volviendo a la Despensa (comportamiento vigente).

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `mover-escaneo-a-la-despensa`, líneas 882–905) — alcance, steps, verification y puntos abiertos resueltos por D1–D3.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — `EmptyShoppingList` (líneas 653–716: `EMPTY_CTA` «Escanear la Despensa» como `Button` primario + `EMPTY_ADD_CTA` «Añadir ítem» como `OutlinedButton`), FAB «Añadir ítem» (233–240) y firma con `onScanPantry`.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListRoute.kt` — propaga `onScanPantry` a la pantalla.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — `ExtendedFloatingActionButton` «Añadir alimento» (148–157) y estado vacío con un solo `Button` (159–191); `PantryTestTags` (62–90).
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryRoute.kt` — hoy solo `onBack`; gana el callback de escaneo.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — rutas `AppRoutes` (`SCAN_PANTRY`, `PANTRY`, `SHOPPING_LIST`), `ScannerRoute.onSaved` con `popUpTo(SCAN_PANTRY) { inclusive = true }` hacia `PANTRY` (125–134) y `SHOPPING_LIST` con `onScanPantry` (160–165).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — accesos que no cambian: `SCAN_PANTRY` (top bar/estado vacío) y `SCAN_PANTRY_FAB` (`FloatingActionButton` icon-only `Icons.Outlined.PhotoCamera`).
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — `action_scan_pantry` (se conserva para Mis Recetas), `pantry_*`, `scanner_*` (incluido `scanner_capture` = «Escanear alimentos»/«Scan foods» en el Escáner; la clave nueva de la Despensa es aparte, D4) y `shopping_list_*`.
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` — S7 `emptyListShowsTheScanPantryCta` (335–344) y `launchShoppingList` con `ShoppingListScreen(onScanPantry = {} …)` (1218–1244): ambos a actualizar.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt` — 5 tests sobre `PantryTestTags`; punto de anclaje de los tests nuevos.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — ya cubre el flujo del Escáner y la vuelta a la Despensa tras guardar (`showPantry` + `PantryTestTags.SCREEN`); regresión a mantener en verde.
- `DESIGN.md` (tabla de pantallas 157/164 y «Estados a cubrir», línea 169) — descripción del estado vacío de la Lista a actualizar.
- `docs/domain-model.md:179` — casuística «Lista de la Compra vacía … CTA para escanear la Despensa y «Añadir ítem» (RD19)» a actualizar.
- `docs/technical-discovery.md` (comandos, 226–233), `docs/risks-and-open-questions.md` (sin `⚠` que toque esta feature), `CONTEXT.md` (glosario: términos intactos).

### Existing Patterns To Follow

- FAB en `Scaffold.floatingActionButton` con `testTag` y `ExtendedFloatingActionButton` (Despensa y Lista); icono de cámara `Icons.Outlined.PhotoCamera` como en el FAB de Mis Recetas.
- Navegación por callbacks Route → Screen cableados en `AppNavHost`; la ruta `SCAN_PANTRY` ya existe y no necesita argumentos (D5 resuelve el back stack con `launchSingleTop`).
- Textos visibles solo en `strings.xml` es/en con paridad 1:1 (`P19`/`P25`); identificadores en inglés.
- E2E con `testTag` + `ComposeTestRule` (`PantryManagementE2ETest`, `ShoppingListE2ETest`, `ScanPantryE2ETest`); fixtures locales y clientes de IA simulados; sin tocar `uxdocs/`.
- Comandos: `./init.ps1`, `./gradlew lintDebug`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew connectedDebugAndroidTest` (focalizado) y `./e2e.ps1` (batería; desinstala la app al terminar).

### Current Gaps

- Ninguna de infraestructura. Solo falta el cableado descrito; no hay migraciones, dependencias ni permisos nuevos.

## Technical Approach

1. **Despensa** (`PantryScreen`/`PantryRoute`): `floatingActionButton` pasa a una `Column` (alineada abajo a la derecha, `Arrangement.spacedBy(WarmSpacing.sm)`) con dos `ExtendedFloatingActionButton`: arriba el nuevo «Escanear alimentos» (`Icons.Outlined.PhotoCamera`, tag `PantryTestTags.SCAN_FOOD`, containerColor primario como el actual) y abajo el existente «Añadir alimento» (tag `ADD_ITEM` sin cambios). El estado vacío añade debajo del mensaje dos botones a ancho completo: «Escanear alimentos» (primario, tag `SCAN_FOOD_CTA`) y «Añadir alimento» (según D3; puede ser `OutlinedButton` o `Button` manteniendo el estilo actual del hueco, con tag `ADD_ITEM` o un tag dedicado si hace falta para los tests). `PantryRoute` gana `onScanFood: () -> Unit` y lo pasa a la pantalla; `AppNavHost` lo cablea a `navController.navigate(AppRoutes.SCAN_PANTRY)`.
2. **Lista de la Compra** (`ShoppingListScreen`/`ShoppingListRoute`): se retira el botón `EMPTY_CTA` («Escanear la Despensa») y el parámetro `onScanPantry` de `ShoppingListScreen` y `ShoppingListRoute`; el «Añadir ítem» del estado vacío pasa a `Button` primario en el hueco del CTA retirado (D2), conservando título y mensaje y su tag `EMPTY_ADD_CTA` salvo que los tests pidan renombrarlo. `AppNavHost` deja de pasar `onScanPantry` a `ShoppingListRoute` y actualiza el comentario de la ruta.
3. **Vuelta a la Despensa** (D5): en `AppNavHost`, el `onSaved` del Escáner añade `launchSingleTop = true` a la navegación existente a `PANTRY` con `popUpTo(SCAN_PANTRY) { inclusive = true }`. Así, con back stack `MY_RECIPES → PANTRY → SCAN_PANTRY` el guardado deja `MY_RECIPES → PANTRY` (sin duplicar), y con `MY_RECIPES → SCAN_PANTRY` (entrada desde Mis Recetas) mantiene el comportamiento actual `MY_RECIPES → PANTRY`. El `onBack` del Escáner (`popBackStack()`) ya devuelve a la Despensa cuando se abre desde allí.
4. **Textos**: clave nueva `pantry_scan_food` es/en (D4); sin retirar claves («Escanear la Despensa» sigue viva para Mis Recetas). Paridad de claves al cierre.
5. **Tests**: actualizar `ShoppingListE2ETest` (S7 pasa a verificar la ausencia de accesos al Escáner; `launchShoppingList` sin `onScanPantry`) y añadir en `PantryManagementE2ETest` los casos de S2 y S4 (FAB «Escanear alimentos» → `ScannerTestTags.SCREEN`; estado vacío con dos CTAs). `ScanPantryE2ETest` y el resto de suites no cambian (solo deben seguir en verde).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — modify: dos FABs apilados (D1), estado vacío con dos CTAs (D3) y test tags nuevos.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryRoute.kt` — modify: callback `onScanFood`.
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — modify: retirar CTA de escaneo y `onScanPantry`; «Añadir ítem» primario en el estado vacío (D2).
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListRoute.kt` — modify: retirar `onScanPantry`.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modify: cablear `onScanFood` en `PANTRY`, retirar `onScanPantry` de `SHOPPING_LIST`, `launchSingleTop = true` en `onSaved` del Escáner (D5) y comentarios al día.
- `app/src/main/res/values/strings.xml` — modify: añadir `pantry_scan_food` («Escanear alimentos»).
- `app/src/main/res/values-en/strings.xml` — modify: añadir `pantry_scan_food` («Scan foods»).
- `app/src/androidTest/java/com/whatsforlunch/app/ShoppingListE2ETest.kt` — modify: reescribir S7 (sin accesos al Escáner) y `launchShoppingList`.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt` — modify: tests nuevos de S2 y S4.
- `DESIGN.md` — modify: fila de la Lista de la Compra (estado vacío sin CTA de escaneo) y «Estados a cubrir» (vacío de la Lista y de la Despensa).
- `docs/domain-model.md` — modify: casuística «Lista de la Compra vacía» (línea 179) sin el CTA de escaneo.
- `feature_list.json` — modify (solo `status`/`evidence` al cerrar; `notes` solo con mantenimiento autorizado).

## Visual Design Impact

- UI involved: yes.
- Design source: `DESIGN.md` (tabla de pantallas, fila «Lista de la Compra»; «Estados a cubrir») y patrones existentes de `PantryScreen`/`MyRecipesScreen` (FAB con `Icons.Outlined.PhotoCamera`).
- Screens or states affected: Lista de la Compra (solo estado vacío), Despensa (FABs y estado vacío). Mis Recetas y Escáner sin cambios.
- New design artifact required: no — se reutilizan `ExtendedFloatingActionButton`, `Button`/`OutlinedButton` y el spacing del tema (`WarmSpacing`); los dos FABs se apilan en `Column` abajo a la derecha con separación `WarmSpacing.sm` y objetivos ≥ 48 dp (D1).

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existen capas, rutas ni fronteras nuevas (solo reubicación de accesos UI).
- `CONSTRAINTS.md`: not needed — no se introduce ninguna regla MUST/MUST NOT nueva.
- `AGENTS.md`: not needed — el flujo de trabajo y los comandos no cambian.
- `DESIGN.md`: update — la fila «Lista de la Compra» y «Estados a cubrir» describen hoy el CTA «Escanear la Despensa» en el estado vacío; se alinean con D2 y se documenta el estado vacío de la Despensa con dos CTAs (D3) y los FABs apilados (D1).
- `docs/domain-model.md`: update — la casuística «Lista de la Compra vacía» (línea 179) menciona el CTA de escaneo (RD19); se ajusta al final del cambio.
- `docs/technical-discovery.md`: not needed — comandos, esquema de Room e integraciones intactos.
- `CONTEXT.md`: not needed — términos del glosario (Escáner, Despensa, Lista de la Compra) intactos.

## Implementation Plan

1. Despensa: apilar los dos FABs (D1) y añadir los dos CTAs al estado vacío (D3), con tags y textos nuevos (`pantry_scan_food`).
2. Lista de la Compra: retirar el CTA «Escanear la Despensa» y `onScanPantry`; promover «Añadir ítem» a primario en el estado vacío (D2).
3. Navegación: cablear `onScanFood` en `AppNavHost` y aplicar `launchSingleTop = true` al `onSaved` del Escáner (D5).
4. Tests: actualizar `ShoppingListE2ETest` (S7 + helper) y añadir los casos de `PantryManagementE2ETest` (S2, S4).
5. Cierre: paridad es/en, gates (lint/unit/assemble), E2E focalizada y batería, docs durables (`DESIGN.md`, `docs/domain-model.md`) y `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] Añadir `pantry_scan_food` a `values/strings.xml` y `values-en/strings.xml`.
- [ ] `PantryScreen`: `Column` de FABs con «Escanear alimentos» (tag `SCAN_FOOD`) encima y «Añadir alimento» (`ADD_ITEM`) abajo (D1).
- [ ] `PantryScreen`: estado vacío con CTAs «Escanear alimentos» (`SCAN_FOOD_CTA`) y «Añadir alimento» (D3).
- [ ] `PantryRoute`: parámetro `onScanFood` y paso a la pantalla.
- [ ] `AppNavHost`: `PANTRY` pasa `onScanFood = { navigate(SCAN_PANTRY) }`.
- [ ] `ShoppingListScreen`: retirar `EMPTY_CTA`/`onScanPantry` y promover «Añadir ítem» a `Button` primario (D2).
- [ ] `ShoppingListRoute`: retirar `onScanPantry`.
- [ ] `AppNavHost`: `SHOPPING_LIST` sin `onScanPantry`; `onSaved` del Escáner con `launchSingleTop = true` (D5).
- [ ] `ShoppingListE2ETest`: reescribir S7 (sin accesos al Escáner en la Lista vacía) y `launchShoppingList`.
- [ ] `PantryManagementE2ETest`: test de S2 (FAB «Escanear alimentos» abre el Escáner) y de S4 (estado vacío con dos CTAs); si es viable con el harness actual, S2 completo con vuelta a la Despensa tras guardar (si no, se apoya en `ScanPantryE2ETest`, que ya cubre la vuelta).
- [ ] Verificar paridad de claves es/en y actualizar `DESIGN.md` y `docs/domain-model.md`.
- [ ] Ejecutar el plan de verificación y capturar la evidencia.

## Verification Plan

- `./init.ps1` — entorno listo, app instalada y en ejecución en el emulador (no arranca servicios de larga duración).
- `./gradlew lintDebug` — 0 errores (línea base actual: 0 errores / 49 warnings).
- `./gradlew testDebugUnitTest` — 0 fallos (línea base: 42 suites / 305 tests).
- `./gradlew assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismas claves en `values/strings.xml` y `values-en/strings.xml` (línea base: 322 claves + 5 plurales; +1 clave `pantry_scan_food`).
- E2E focalizada (`./gradlew connectedDebugAndroidTest` con las clases afectadas): `ShoppingListE2ETest` en verde con la S7 nueva (verify 1), `PantryManagementE2ETest` en verde con los casos nuevos (verify 2 parcial y S4) y `ScanPantryE2ETest` sin regresión (la vuelta a la Despensa tras guardar ya está cubierta por su flujo).
- E2E batería completa (`./e2e.ps1`, ~15 min): sin regresiones sobre la línea base de 220 tests / 0 fallos / 2 omitidos (`SettingsE2ETest`/`Assume`); se reconcilia el inventario `@Test` con el XML.
- Manual/adb con uiautomator (verify 3): recorrer Lista vacía, Despensa vacía y con alimentos, comprobar el orden de FABs y CTAs (D1–D3) y los textos es/en con la app en cada idioma.
- Regresión de accesos de Mis Recetas (S5): `AppLaunchE2ETest`, `SearchAndFilterRecipesE2ETest` y `MyRecipesLocalizationE2ETest` en verde (ya cubren FAB/CTA de escaneo de Mis Recetas).

## Evidence To Capture

- Salida de lint/unit/assemble con recuentos (0 errores; suites/tests) en `docs/evidence/mover-escaneo-a-la-despensa/`.
- Resultado de la E2E focalizada y de la batería (recuento por clase y XML si procede), reconciliado con el inventario `@Test`.
- 2 capturas es 1280×2856: (1) Lista de la Compra vacía sin «Escanear la Despensa» y con «Añadir ítem» primario; (2) Despensa con los dos FABs apilados («Escanear alimentos» encima) o su estado vacío con los dos CTAs.
- Comprobación manual de verify 3 (orden de elementos y textos es/en) anotada en el informe.
- Resumen en `evidence` de `feature_list.json` y entrada en `docs/progress-history.md` al cerrar.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (solo accesos/orden de UI en Lista y Despensa; Mis Recetas y Escáner intactos).
- [ ] Acceptance scenarios pass (S1–S5; verify 1 = S1, verify 2 = S2, verify 3 = S3).
- [ ] Verification evidence is present (`docs/evidence/mover-escaneo-a-la-despensa/`).
- [ ] Persistent E2E coverage was added/updated: `ShoppingListE2ETest` S7 reescrita y `PantryManagementE2ETest` ampliada (comportamiento observable de usuario), o justificación documentada de por qué basta la cobertura existente.
- [ ] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente (solo `status`/`evidence` en la lista).
- [ ] No unrelated product behavior or extra feature work was added (sin `mover-opciones-a-bottom-bar`, sin cambios en dominio/Room/Escáner).

---

**Enmienda `redisenar-vista-de-la-despensa` (2026-11-11, patrón completo)**: el orden de los CTAs del estado vacío de la Despensa se invierte (D16): **«Añadir Alimento» (primario) primero** y «Escanear la despensa» (tonal) debajo —antes «Escanear alimentos» primero (D3)—, siguiendo `uxdocs/chefencasa_despensa/code_empty.html`. Los dos FABs apilados («Escanear alimentos» encima con icono `PhotoCamera`, «Añadir alimento» abajo), las claves `pantry_scan_food`/`pantry_add_food`, `launchSingleTop` (D5) y el estado vacío de la Lista de la Compra (D2) quedan intactos; la etiqueta de la pestaña de la barra inferior pasa a la clave corta `pantry_tab_label` («Despensa»/«Pantry», patrón `my_recipes_tab_label`) para que el título «Mi Despensa» de D17 no la altere.
