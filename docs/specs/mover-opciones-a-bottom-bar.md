# Feature Implementation Spec: Mover las opciones de la TopBar a una BottomBar

## Source Feature

- `id`: mover-opciones-a-bottom-bar
- `area`: funcional
- `depends_on`: gestionar-alimentos-de-la-despensa, anadir-faltantes-a-la-lista, resolver-dudas-con-pinche, separar-despensa-y-busqueda-de-recetas (todas `accepted`)
- `status`: not_started (al planificar)
- `source`: `feature_list.json` (entrada creada en la sesión 105, 2026-11-07)

## Goal

Mis Recetas deja de tener la top bar saturada: sus accesos a «Lista de la Compra», «Pinche», «Recetas con mi Despensa» y «Despensa» bajan a una **barra inferior de navegación** con las pestañas Mis Recetas, Despensa, Lista de la Compra y Pinche (en ese orden), y Ajustes se queda como única acción de la top bar. La barra es **permanente en las cuatro pantallas de las pestañas** (D1) con la pestaña activa resaltada; «Recetas con mi Despensa» pasa a un **segundo botón flotante** junto al de cámara conservando su icono y su destino. Los destinos y su comportamiento no cambian: solo cambia cómo se llega a ellos.

Esta spec resuelve los puntos que `feature_list.json` dejó abiertos en `notes` («la permanencia de la bottom bar en los demás destinos y el comportamiento de la pestaña activa») con las decisiones del usuario D1–D3 del 2026-11-08.

## Non-Goals

- No se añade la pestaña «Mi perfil» ni se deja placeholder de quinta pestaña: la ampliación a 5 pestañas corresponde a `crear-mi-perfil` (D2).
- No se cambia el contenido ni el comportamiento de los destinos (Despensa, Lista de la Compra, Pinche, «Recetas con mi Despensa»): solo su forma de navegación y su top bar.
- No se mueve el escaneo a la Despensa ni se reordenan los FABs de otras pantallas (`mover-escaneo-a-la-despensa`, futura): los accesos al Escáner se mantienen como hoy (FAB de cámara y botón del estado vacío de la Lista).
- No se toca Ajustes, el Detalle, el formulario, el Modo Cocina, el Escáner ni Sugerencias más allá de no mostrar la barra (ya no la muestran).
- No se persiste el estado de las pestañas entre reinicios de la app ni se añade restauración de scroll compleja (D5).
- No se reescriben specs ni evidencias históricas: la enmienda al non-goal «bottom navigation» de `separar-despensa-y-busqueda-de-recetas` ya está en sus `notes`.
- No se tocan Room, las reglas de dominio ni los flujos de IA.

## Job Story

When uso la app a diario y salto entre Mis Recetas, la Despensa, la Lista de la Compra y el Pinche,
I want to tener esas secciones siempre a un toque en una barra inferior y la top bar de Mis Recetas solo para Ajustes,
so I can navegar más rápido sin perder el hilo de lo que estoy haciendo.

## Users And Permissions

- Usuario único sin roles: mismo comportamiento en todas las pestañas. Sin permisos ni autenticación nuevos.

## Requisitos funcionales

- RF-MOB1: Mis Recetas, Despensa, Lista de la Compra y Pinche muestran una barra inferior de navegación común con 4 pestañas en el orden Mis Recetas, Despensa, Lista de la Compra, Pinche (icono + texto localizado) y la pestaña de la pantalla actual seleccionada (D1, D6).
- RF-MOB2: La top bar de Mis Recetas solo conserva «Ajustes»; se retiran «Lista de la Compra», «Pinche», «Recetas con mi Despensa» y «Despensa».
- RF-MOB3: «Recetas con mi Despensa» pasa a un segundo FAB en Mis Recetas, apilado encima del FAB de cámara (que conserva su posición), con el mismo icono (`MenuBook`) y el mismo destino (`pantry_recipes`); ambos FABs solo se muestran cuando hay Recetas (D3, mismo criterio que hoy el de cámara).
- RF-MOB4: La navegación por pestañas mantiene el back stack coherente (D5): el back del sistema desde Despensa, Lista de la Compra o Pinche vuelve a Mis Recetas y desde Mis Recetas sale de la app; tocar pestañas no crea entradas duplicadas.
- RF-MOB5: Despensa, Lista de la Compra y Pinche dejan de mostrar «Volver» en su top bar (son destinos de primer nivel, D1) y conservan título, contenido y acciones propias («Limpiar comprados» en la Lista, el acceso a Ajustes dentro del Pinche, etc.).
- RF-MOB6: La pestaña activa está resaltada en el color primary del tema (estilo del prototipo) y tocar la pestaña activa vuelve al inicio de esa pantalla (el contenido se desplaza a su primera posición), sin crear una entrada nueva en el back stack ni resetear su estado.
- RF-MOB7: Los destinos no cambian: cada pestaña abre la pantalla de hoy con sus datos, sus textos es/en y objetivos táctiles de al menos 48 dp; las pantallas empujadas (Escáner, Detalle, Modo Cocina, formulario, Ajustes, «Recetas con mi Despensa», Sugerencias) no muestran la barra y conservan su «Volver» (D4).

## Acceptance Scenarios

### Scenario 1: barra inferior con las 4 pestañas y sus destinos (verify 1)

Given Mis Recetas mostrando la barra inferior
When leo las pestañas y toco cada una
Then aparecen «Mis Recetas», «Despensa», «Lista de la Compra» y «Pinche» en ese orden y cada pestaña abre su pantalla actual (Mis Recetas, Despensa, Lista de la Compra y el chat del Pinche)

### Scenario 2: top bar solo Ajustes y «Recetas con mi Despensa» en el FAB (verify 2)

Given Mis Recetas con al menos una Receta
When examino la top bar y los botones flotantes
Then la top bar solo muestra «Ajustes» y «Recetas con mi Despensa» se abre desde el segundo botón flotante, que conserva su icono actual y lleva a su pantalla

### Scenario 3: textos en español e inglés (verify 3)

Given la app en español y en inglés
When recorro la barra inferior y los botones flotantes
Then las etiquetas muestran «Mis Recetas»/«My Recipes», «Despensa»/«Pantry», «Lista de la Compra»/«Shopping List», «Pinche»/«Pinche» y los textos de los FABs en el idioma correspondiente

### Scenario 4: pestaña activa, «Volver» retirado y back coherente (D1)

Given Despensa, Lista de la Compra o Pinche abiertas desde la barra
When toco la pestaña activa y uso el back del sistema
Then la pestaña de la pantalla actual aparece resaltada, la top bar no muestra «Volver», tocar la pestaña activa vuelve al inicio de la pantalla y el back del sistema vuelve a Mis Recetas; desde Mis Recetas el back sale de la app

### Scenario 5: los dos FABs solo con Recetas (D3)

Given Mis Recetas sin Recetas y después con Recetas
When comparo los botones flotantes de ambos estados
Then con la lista vacía no aparece ningún FAB (siguen las acciones del estado vacío) y con Recetas aparecen juntos los dos FABs, «Recetas con mi Despensa» encima del de cámara

## Casos límite

- Estado «sin resultados» de Mis Recetas: hay Recetas (solo filtradas), así que los dos FABs siguen visibles; en el estado vacío (0 Recetas) no hay FABs (D3).
- Cambio directo de pestaña (Despensa → Lista de la Compra): la pestaña origen se descarta y su estado transitorio se reinicia al volver (igual que hoy al salir y volver a entrar); Mis Recetas conserva búsqueda, momento y «Favoritas» porque su entrada nunca se descarta (D5).
- Pinche con conversación en curso y Lista con agrupación «Por Receta»: al cambiar de pestaña y volver, ambos arrancan en su estado inicial (comportamiento actual al salir de esas pantallas).
- Escáner abierto desde el FAB de cámara o desde el estado vacío de la Lista: tras guardar se aterriza en Despensa como pestaña y el back vuelve a Mis Recetas, sin dejar el Escáner en el back stack (patrón actual de `ScannerRoute.onSaved`).
- Back repetido desde cualquier pestaña: Mis Recetas nunca se duplica en el back stack (`launchSingleTop`/`popUpTo`), así que un segundo back sale de la app.
- Rotación de pantalla en cualquier pestaña: la barra y la pestaña seleccionada se conservan.
- Etiqueta larga («Lista de la Compra»): el ítem de la barra admite dos líneas y no se trunca el texto (la accesibilidad usa la etiqueta).

## Repository Research

### Files Inspected

- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — rutas (`my_recipes`, `pantry`, `shopping_list`, `pinche`, `pantry_recipes`, `scan_pantry`, …), callbacks por pantalla y `ScannerRoute.onSaved` (navega a `pantry` con `popUpTo(scan_pantry)`).
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `TopAppBar` con 5 `IconButton` (`action_open_shopping_list`, `action_open_pinche`, `action_open_pantry_recipes`, `action_open_pantry`, `action_open_settings`) y FAB `action_scan_pantry_fab` solo con Recetas; `MyRecipesTestTags`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesRoute.kt` — firma con `onOpenPantry`/`onOpenPinche`/`onOpenShoppingList`/`onOpenPantryRecipes`/`onOpenSettings`.
- `app/src/main/java/com/whatsforlunch/app/ui/pantry/PantryScreen.kt` — `TopAppBar` con `navigationIcon` TextButton «Volver» (`action_back`, ≥ 48 dp) y FAB «Añadir alimento».
- `app/src/main/java/com/whatsforlunch/app/ui/shoppinglist/ShoppingListScreen.kt` — `TopAppBar` con `navigationIcon` «Volver» y acción «Limpiar comprados»; FAB «Añadir ítem».
- `app/src/main/java/com/whatsforlunch/app/ui/pinche/PincheScreen.kt` — `TopAppBar` con `navigationIcon` «Volver» y botón `pinche_open_settings` dentro del chat.
- `app/src/main/java/com/whatsforlunch/app/ui/recommendations/RecommendationsScreen.kt` — destino no-pestaña: conserva su «Volver».
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — claves `my_recipes_title` («Mis Recetas»/«My Recipes»), `pantry_title` («Despensa»/«Pantry»), `shopping_list_title`, `pinche_title`, `action_back`, `action_scan_pantry`, `recommendations_open_accessibility`; paridad actual 322/322 claves + 5/5 plurals.
- Tests E2E (`app/src/androidTest`): `PantryManagementE2ETest` (`openPantry` vía `action_open_pantry`), `PincheE2ETest` (`action_open_pinche` + «Volver» por contentDescription), `ShoppingListE2ETest` (`action_open_shopping_list`, `action_open_pantry_recipes` ×2), `MyRecipesLocalizationE2ETest` (el «Volver» que usa es del formulario/Escáner → intacto; candidato a cubrir S3), `AppLaunchE2ETest`/`SearchAndFilterRecipesE2ETest`/`CreateRecipeE2ETest`/`ScanPantryE2ETest` (usan `action_scan_pantry_fab` o aterrizan en Despensa) y `PantryRecipesE2ETest` («Volver» de `RecommendationsScreen`, intacto). Tests unitarios: ninguno toca la navegación (sin cambios esperados).
- `uxdocs/*/code.html` — nav inferior fijo (`fixed bottom-0`, icono 24 px + label, activo en `text-primary` + negrita) en las pantallas del prototipo: inspiración visual (`P16`).
- `DESIGN.md` — tabla de pantallas (fila Mis Recetas con las acciones de top bar de hoy) a actualizar.
- `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `CONTEXT.md` — sin reglas de navegación ni `⚠` ligados a esta feature.

### Patterns Found

- `Scaffold` + `TopAppBar` por pantalla, con callbacks cableados en `AppNavHost` (sin ViewModel de navegación).
- testTags centralizados por pantalla (`XxxTestTags`) y E2E por pantalla en `app/src/androidTest`.
- Textos de UI en `values/` (es) y `values-en/` (en) con claves snake_case; paridad por recuento (sin test automático).
- Objetivos táctiles ≥ 48 dp en las acciones (`heightIn(min = 48.dp)`).
- Comandos: `.\init.ps1`, `.\lint.ps1`, `.\unit.ps1`, `.\e2e.ps1` (`connectedDebugAndroidTest`), `.\gradlew.bat assembleDebug`.

### Current Gaps

- No existe navegación inferior previa: `AppBottomBar` es un componente nuevo (Material3 `NavigationBar`).
- No hay tests unitarios de navegación: la navegación se verifica con E2E.
- La paridad de claves es/en es por recuento, sin test automático (convención del repo).

## Technical Approach

### Decisiones

- D1 (usuario, 2026-11-08): la barra es **permanente en las cuatro pantallas de las pestañas** (Mis Recetas, Despensa, Lista de la Compra y Pinche) con la pestaña activa resaltada; tocar la pestaña activa vuelve al inicio de esa pantalla; se retira el «Volver» de Despensa, Lista de la Compra y Pinche; el back del sistema desde esas pantallas vuelve a Mis Recetas y desde Mis Recetas sale de la app.
- D2 (usuario, 2026-11-08): la barra se construye con **4 pestañas**; la quinta («Mi perfil», al final tras Pinche) la añadirá `crear-mi-perfil` (fuera de alcance).
- D3 (usuario, 2026-11-08): los dos FABs de Mis Recetas se muestran **juntos y solo cuando hay Recetas** (mismo criterio que el FAB de cámara actual).
- D4 (derivada): la barra solo aparece en las cuatro pantallas de las pestañas; las pantallas empujadas (Escáner, Detalle, Modo Cocina, formulario, Ajustes, «Recetas con mi Despensa», Sugerencias) no la muestran y conservan su «Volver».
- D5 (derivada): navegación por pestañas con `popUpTo(my_recipes) { inclusive = false }` + `launchSingleTop` para Despensa/Lista/Pinche y vuelta a la entrada existente de `my_recipes` para la pestaña Mis Recetas (sin apilar una segunda entrada). El back stack queda [my_recipes, pestaña actual]; al cambiar de pestaña el destino se recrea con su estado inicial (igual que hoy al salir y volver) y Mis Recetas conserva búsqueda y filtros.
- D6 (derivada): componente compartido `ui/nav/AppBottomBar.kt` (Material3 `NavigationBar` con 4 `NavigationBarItem`) y enum `AppTab`; cada pantalla de pestaña lo usa en su `Scaffold(bottomBar = …)` (patrón actual de Scaffold por pantalla).
- D7 (derivada): iconos — Mis Recetas `Icons.Outlined.RestaurantMenu` (distinto de `MenuBook`), Despensa `Icons.Outlined.Inventory2`, Lista de la Compra `Icons.Outlined.ShoppingCart` y Pinche `Icons.AutoMirrored.Outlined.Chat` (los tres últimos, los mismos de hoy); «Recetas con mi Despensa» conserva `Icons.AutoMirrored.Outlined.MenuBook` en su FAB (sesión 105) y el FAB de cámara `Icons.Outlined.PhotoCamera` abajo, con «Recetas con mi Despensa» apilado encima.
- D8 (derivada): etiquetas de pestaña con claves existentes (`my_recipes_title`, `pantry_title`, `shopping_list_title`, `pinche_title`) → sin altas de strings y paridad intacta. testTags: nuevos `AppBottomBarTestTags` (`bottom_bar`, `bottom_bar_tab_my_recipes|pantry|shopping_list|pinche`); el FAB reutiliza `action_open_pantry_recipes` (misma acción y destino) y se retiran `action_open_shopping_list`, `action_open_pinche` y `action_open_pantry`; `action_open_settings` se mantiene.

### Desconocidos

- Ninguno bloqueante. El indicador exacto de pestaña activa usa `NavigationBarItem` por defecto con `primary` en la seleccionada sobre los tokens de `DESIGN.md`; si el implementador necesita claves de accesibilidad nuevas para la barra, mantiene la paridad es/en.

## Expected File Changes

- `ui/nav/AppBottomBar.kt` — create; enum `AppTab`, `NavigationBar` común (D6/D7) y `AppBottomBarTestTags`.
- `ui/nav/AppNavHost.kt` — modify; navegación por pestañas (D5), callbacks de pestañas en las 4 rutas y `ScannerRoute.onSaved` hacia la pestaña Despensa.
- `ui/myrecipes/MyRecipesScreen.kt` — modify; top bar solo Ajustes (D8), `bottomBar = AppBottomBar(...)` y FAB apilado «Recetas con mi Despensa» + cámara solo con Recetas (D3).
- `ui/myrecipes/MyRecipesRoute.kt` — modify; firma de callbacks de pestañas y FAB.
- `ui/pantry/PantryScreen.kt`, `ui/pantry/PantryRoute.kt` — modify; quitar «Volver», añadir `bottomBar` y callbacks de pestañas.
- `ui/shoppinglist/ShoppingListScreen.kt`, `ui/shoppinglist/ShoppingListRoute.kt` — modify; quitar «Volver», añadir `bottomBar` («Limpiar comprados» intacto).
- `ui/pinche/PincheScreen.kt`, `ui/pinche/PincheRoute.kt` — modify; quitar «Volver», añadir `bottomBar` (acceso interno a Ajustes intacto).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — sin cambios esperados (reutilización de claves, D8); cualquier alta mantiene la paridad es/en.
- Tests unitarios — sin cambios esperados (no hay reglas de dominio en juego).
- `app/src/androidTest/.../BottomBarE2ETest.kt` — create; S1–S5.
- `app/src/androidTest/.../PantryManagementE2ETest.kt`, `PincheE2ETest.kt`, `ShoppingListE2ETest.kt` — modify; aperturas por pestaña/FAB y retirada del «Volver» del Pinche.
- `app/src/androidTest/.../MyRecipesLocalizationE2ETest.kt` — modify/ampliar; textos de la barra y los FABs es/en (S3).
- `app/src/androidTest/.../AppLaunchE2ETest.kt`, `SearchAndFilterRecipesE2ETest.kt`, `CreateRecipeE2ETest.kt`, `ScanPantryE2ETest.kt`, `PantryRecipesE2ETest.kt` — revisar y adaptar tags/back si procede; el resto de E2E queda intacto.
- `DESIGN.md`, `docs/risks-and-open-questions.md` — modify; ver Durable Documentation Impact.

## Visual Design Impact

- UI involved: sí.
- Design source: `DESIGN.md` y el nav inferior de `uxdocs/*/code.html` (inspiración `P16`: barra fija con icono 24 px + label y pestaña activa en primary/negrita).
- Screens or states affected: Mis Recetas (top bar solo Ajustes; barra inferior; dos FABs apilados; estados vacío/con Recetas/sin resultados), Despensa, Lista de la Compra y Pinche (barra inferior; sin «Volver»).
- New design artifact required: no — Material3 `NavigationBar` con los tokens existentes (seleccionado en `primary`, sin seleccionar en `onSurfaceVariant`, superficie `surface`).

## Durable Documentation Impact

- `ARCHITECTURE.md` / `CONSTRAINTS.md`: not needed — no existen en el repo y el cambio no introduce fronteras arquitectónicas nuevas (sigue el patrón `ui/nav`).
- `AGENTS.md`: not needed — no cambia el flujo de trabajo, los comandos ni las convenciones.
- `DESIGN.md`: update — Mis Recetas (acciones de top bar → solo Ajustes; barra inferior de 4 pestañas; dos FABs) y Despensa/Lista de la Compra/Pinche (sin «Volver», con barra inferior).
- `docs/domain-model.md`: not needed — la navegación es UI y no toca reglas de negocio.
- `docs/risks-and-open-questions.md`: update — añadir `Q-mover-opciones-a-bottom-bar-1..3` resueltas (D1–D3) y la nota de coordinación con `crear-mi-perfil` (5.ª pestaña) y `mover-escaneo-a-la-despensa`.
- `CONTEXT.md`: not needed — la terminología no cambia.
- Specs históricas: no se reescriben (la enmienda al non-goal «bottom navigation» de `separar-despensa-y-busqueda-de-recetas` ya vive en sus `notes`).

## Implementation Plan

1. Componente y navegación: `AppTab` + `AppBottomBar` (D6/D7/D8) y navegación por pestañas en `AppNavHost` con el back stack de D5 (incluido `ScannerRoute.onSaved`).
2. Mis Recetas: top bar solo Ajustes, FAB apilado «Recetas con mi Despensa» (D3) y `bottomBar`; ajustar `MyRecipesRoute`.
3. Destinos: Despensa, Lista de la Compra y Pinche sin «Volver» y con `bottomBar`; cablear sus rutas a las pestañas.
4. E2E: `BottomBarE2ETest` (S1–S5) y adaptación de los ficheros afectados (aperturas por pestaña/FAB, back coherente).
5. Cierre: `DESIGN.md`, `docs/risks-and-open-questions.md`, paridad es/en por recuento y gates.

## Implementation Tasks

- [x] `ui/nav/AppBottomBar.kt`: `AppTab`, `NavigationBar` con las 4 `NavigationBarItem` (D7), etiquetas con claves existentes (D8) y `AppBottomBarTestTags`.
- [x] `AppNavHost`: helper de navegación por pestañas (D5), callbacks en las 4 rutas y `onSaved` del Escáner hacia la pestaña Despensa.
- [x] `MyRecipesScreen`/`MyRecipesRoute`: top bar solo Ajustes, FAB apilado («Recetas con mi Despensa» encima del de cámara, solo con Recetas) y `bottomBar`.
- [x] `PantryScreen`/`PantryRoute`, `ShoppingListScreen`/`ShoppingListRoute`, `PincheScreen`/`PincheRoute`: retirar «Volver», añadir `bottomBar` y cablear las pestañas.
- [x] Strings: sin altas esperadas; comprobar la paridad es/en por recuento (319/319 claves + 5/5 plurals; se retiraron 3 claves de ambos locales, ver Hallazgos).
- [x] `BottomBarE2ETest.kt` con S1–S5 (verify 1–3 cubiertos).
- [x] E2E afectados: `PantryManagementE2ETest`, `PincheE2ETest`, `ShoppingListE2ETest`, `MyRecipesLocalizationE2ETest` y revisión de `AppLaunchE2ETest`, `SearchAndFilterRecipesE2ETest`, `CreateRecipeE2ETest`, `ScanPantryE2ETest`, `PantryRecipesE2ETest`.
- [x] Docs durables (`DESIGN.md`, `docs/risks-and-open-questions.md`) y gates finales (lint 0/48, unit 305/0, assembleDebug OK, paridad 319/319 + 5/5).

### Hallazgos de implementación (2026-11-08)

- `ScannerRoute.onSaved` aterriza en la pestaña Despensa con `openTab(AppTab.PANTRY)` (el Escáner no queda en el back stack, como preveía el patrón previo).
- Se retiran 3 claves de strings de ambos locales (`values/strings.xml` y `values-en/strings.xml`) al desaparecer sus acciones de top bar: `action_open_shopping_list`, `pantry_open_accessibility` y `pinche_open_accessibility`. La paridad se mantiene: 319/319 claves + 5/5 plurals (antes 322/322).
- La regla de lint `ModifierParameter` exige `modifier` como primer parámetro opcional: se reordenó en `MyRecipesScreen`/`MyRecipesRoute`, `PantryScreen`/`PantryRoute`, `ShoppingListScreen`/`ShoppingListRoute` y `PincheScreen`/`PincheRoute` (incluida `MessageList`).
- Los títulos de top bar de Mis Recetas y Despensa llevan testTag `TOP_BAR_TITLE` (`MyRecipesTestTags`, `PantryTestTags`): sus textos coinciden con las etiquetas de pestaña de la barra inferior y la tag desambigua las aserciones E2E de localización (S3).

## Verification Plan

- `.\init.ps1` (o `./init.sh`): instala y arranca la app sin lanzar procesos de larga duración (comportamiento actual del script). Nota: un exit 1 con la app en ejecución es el falso negativo conocido de `monkey`/`pidof`; verificar con `adb shell pidof com.whatsforlunch.app`.
- `.\lint.ps1` (`gradlew lintDebug`): 0 errores y warnings por debajo de la línea base de 49.
- `.\unit.ps1` (`gradlew testDebugUnitTest`): todos los tests unitarios en verde (sin cambios de dominio esperados).
- `.\gradlew.bat assembleDebug`: BUILD SUCCESSFUL.
- Paridad es/en: recuento de claves de `values/strings.xml` vs `values-en/strings.xml` idéntico (convención del repo).
- E2E focalizada con `.\gradlew.bat connectedDebugAndroidTest` (emulador disponible): `BottomBarE2ETest` (S1–S5), `PantryManagementE2ETest`, `PincheE2ETest`, `ShoppingListE2ETest`, `PantryRecipesE2ETest` y `MyRecipesLocalizationE2ETest` (S3).
- Batería completa `.\e2e.ps1`: sin regresiones; los conteos deben cuadrar con el inventario `@Test`.
- Cobertura E2E obligatoria: la feature cambia navegación y comportamiento visible de usuario. El back del sistema (D1) se verifica con `Espresso.pressBack()` en E2E y, si hiciera falta, con `adb shell input keyevent KEYCODE_BACK` a mano.

## Evidence To Capture

- Salidas de `lint.ps1`, `unit.ps1` (suites/tests/0 fallos), `assembleDebug` y de la batería E2E (conteos por clase, con XML conservado en `docs/evidence/mover-opciones-a-bottom-bar/`).
- 2 capturas es 1280×2856: Mis Recetas con la barra («Mis Recetas» activa), la top bar solo Ajustes y los dos FABs; una pestaña destino (p. ej. Despensa) con la barra, su pestaña activa y sin «Volver».
- Recuento de paridad de claves es/en tras el cambio.
- Resumen en `evidence` de `feature_list.json` y entrada en `docs/progress-history.md` al cerrar.

## Trazabilidad

| RF | Escenario | Verificación |
| --- | --- | --- |
| RF-MOB1 | S1 | E2E `BottomBarE2ETest` (verify 1) |
| RF-MOB2 | S2 | E2E `BottomBarE2ETest` (verify 2) |
| RF-MOB3 | S2, S5 | E2E `BottomBarE2ETest` (verify 2) |
| RF-MOB4 | S4 | E2E `BottomBarE2ETest` (back con `pressBack`) |
| RF-MOB5 | S4 | E2E + ausencia de textos «Volver» en las 3 pantallas |
| RF-MOB6 | S4 | E2E `BottomBarE2ETest` (scroll + tap en la pestaña activa) |
| RF-MOB7 | S1, S3 | E2E de destinos + `MyRecipesLocalizationE2ETest` (verify 3) |

## Validator Checklist

- [ ] Implementation stays within this feature's scope.
- [ ] Acceptance scenarios pass.
- [ ] Verification evidence is present.
- [ ] Persistent E2E coverage was added/updated (obligatorio: cambia navegación y comportamiento visible).
- [ ] `feature_list.json` and `../../../../PROGRESS.md` were updated correctly.
- [ ] No unrelated product behavior or extra feature work was added.
