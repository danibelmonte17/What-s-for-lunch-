# Informe de validación — `ver-detalle-de-receta`

- **Fecha**: 2026-10-21 · **Rol**: `feature-validator` (validación independiente) · **Veredicto**: **`accept`**
- **Spec**: `docs/specs/ver-detalle-de-receta.md` (267 líneas, `Durable Documentation Impact` y `Validator Checklist` de la propia spec).
- **Entorno**: AVD `Pixel_10_Pro` API 37 (único AVD de trabajo), `adb`/`emulator` de `%LOCALAPPDATA%\Android\Sdk` (no en el PATH global), JDK 17 (`C:\Users\Daniel\.jdks\corretto-17.0.20` sobrescribiendo el `JAVA_HOME` inválido del entorno), rama `feature/ver-detalle-de-receta` sin commitear (todo en el working tree).
- **Diff revisado**: nuevos `ui/recipedetail/{RecipeDetailViewModel,RecipeDetailRoute,RecipeDetailScreen}.kt`, `ui/components/{MatchBadge,MomentBadge,SkilletIcon}.kt`, `RecipeDetailViewModelTest.kt`, `RecipeDetailE2ETest.kt`; modificados `MyRecipesScreen.kt`, `MyRecipesRoute.kt`, `AppNavHost.kt`, `ProvisionalScreen.kt`, `di/AppModule.kt`, `values/strings.xml` y `values-en/strings.xml` (+ `feature_list.json`/`PROGRESS.md`/`docs/progress-history.md` del implementer). Coincide con `Expected File Changes` de la spec salvo los dos extraídos `MomentBadge.kt`/`SkilletIcon.kt`, justificados por reutilización (D5 y el icono `MealMoment.LUNCH` compartido). Sin cambios en Gradle, dependencias, permisos, harness, `uxdocs/`, `docs/domain-model.md` ni esquema Room.

## 1. Gates reranqueados por el validador (sobre el árbol actual, sin commitear)

| Comando | Resultado | Detalle |
|---|---|---|
| `init.ps1` (con `JAVA_HOME` corretto-17 y `platform-tools` en `PATH`) | **exit 0** | Gate de arranque en verde (`.harness/validator-init.log`, único log conservado: registro del arranque). |
| `lintDebug` | **exit 0** | `BUILD SUCCESSFUL`, **0 errores** (solo warnings preexistentes). |
| `testDebugUnitTest --rerun-tasks` | **exit 0** | **56/56, 0 fallos/0 errores**, recuento desde los XML de `app/build/test-results` (11 suites); incluye `RecipeDetailViewModelTest` **6/6**. Lanzado directamente con `gradlew.bat` porque `unit.ps1` no reenvía argumentos. |
| `assembleDebug` | **exit 0** | `BUILD SUCCESSFUL`. |
| `connectedDebugAndroidTest -P…class=com.whatsforlunch.app.RecipeDetailE2ETest` (focalizada) | **exit 0** | **7/7, 0 fallos, 0 omitidos** en `Pixel_10_Pro(AVD) - 17` (S1-S7). |
| `connectedDebugAndroidTest` (regresión completa) | **exit 0** | **94/94, 0 fallos, 0 omitidos, 7 m 3 s** en `Pixel_10_Pro(AVD) - 17`. Sin regresión en `mostrar-badge-de-coincidencia`, `buscar-y-filtrar-recetas`, CRUD de recetas, `gestionar-alimentos-de-la-despensa` ni Escáner; tampoco los flakes ambientales documentados (esta pasada, completa, salió limpia). |

Nota: además de reranquear, el validador ejecutó a mano la app en el emulador con `adb`/`uiautomator` (§2-§4); para eso instaló con `installDebug` y sembró los datos por la propia UI (incluida una foto real vía el picker de MediaStore tras hacer visible un JPEG en `/sdcard/Pictures`, sorteo de la incidencia conocida del picker con la MediaStore vacía).

## 2. Escenarios S1-S7 (comprobación manual en el emulador + E2E reranqueado)

Se sembró: **«Tortilla de patatas»** (Publicada, foto, historia, 450 kcal, 15+30 min, Media, 4 raciones, ingredientes creados en este orden: **Aceite 0,25 l opcional**, Huevos 4 uds, Patatas 0,5 kg — el opcional **primero** en el formulario a propósito para probar D6 —, pasos «Corta las patatas» con temporizador 10 y «Cuece la tortilla» sin temporizador) y **«Borrador vacio»** (Borrador: sin foto, historia, kcal, ingredientes ni pasos). Despensa: primero solo Huevos, después Huevos + Patatas.

| Escenario | Resultado | Evidencia (manual con `adb`/`uiautomator` salvo donde se indica) |
|---|---|---|
| **S1 ficha completa** | **pasa** | Detalle: foto con badges superpuestos «Almuerzo» + «100% Match», título, historia, `45 min / Tiempo total / 15 min preparación · 30 min cocción`, `Media / Dificultad`, `450 / Calorías por porción`, `4 / Raciones base`, contador `0 de 3`, filas `4 uds Huevos`, `0,5 kg Patatas`, `0,25 l Aceite` + píldora `Opcional`, `Pasos / 2 Pasos`, pasos 1 y 2 con chip `10 min` solo en el paso 1, CTA visible. Captura: `docs/evidence/ver-detalle-de-receta/detalle-receta-completa.png`. |
| **S2 apertura y vuelta con búsqueda/filtro** | **pasa** | Búsqueda «Tortilla» → 1 tarjeta → tap en el cuerpo → Detalle → «Volver» → la lista sigue con `Tortilla` en el campo y 1 tarjeta (idA/vuelta repetido dos veces, se conservó en ambos). E2E `tappingCardBodyOpensDetailAndBackKeepsSearch` en verde (suite completa). Nota: los chips de filtro no exponen su estado `selected` a `uiautomator` (ni siquiera «Todas»), así que la conservación del filtro se apoya en E2E aceptado de `buscar-y-filtrar-recetas` + el mismo mecanismo (estado en `MyRecipesViewModel`, entrada de backstack viva) que el de la búsqueda, verificado manualmente con el texto del campo. |
| **S3 badge reactivo 50% → 100% sin salir** | **pasa** | Despensa solo «Huevos» → Detalle con badge **`50%`** (manual). Completar la Despensa sin salir de la pantalla **no tiene entrada por UI** (la Despensa vive en otra pantalla), así que la reactividad «en caliente» se verifica con los tests reranqueados: E2E `matchBadgeReactsToPantryChanges` (50% → «100% Match» con `waitUntil` sin salir) y unit `matchReactsToPantryChangesWithoutLeavingTheScreen`. Manual complementario: tras añadir «Patatas» y reabrir → **`100% Match`**. |
| **S4 borrador incompleto** | **pasa** | «Borrador vacio»: placeholder de superficie sin foto, badges junto al título («Almuerzo», **sin badge de Coincidencia** por RF-MB6), sin historia, `—` en calorías, `Sin ingredientes`, `Sin pasos`. Captura: `docs/evidence/ver-detalle-de-receta/detalle-receta-borrador.png`. |
| **S5 CTA → Modo Cocina provisional** | **pasa** | Tap «Empezar Modo Cocina» (habilitada) → pantalla provisional **«Modo Cocina»** con «Volver» → «Volver» regresa al Detalle. |
| **S6 receta inexistente** | **pasa** | Sin entrada por UI (no hay forma de navegar a un id inválido), cubierto por los tests reranqueados: E2E `unknownIdShowsNotFoundWithBackAction` («Receta no encontrada» + acción de vuelta que se activa) y unit `unknownRecipeIdShowsNotFoundState`. |
| **S7 checklist de marcado local** | **pasa** | Marca fila 1 → `1 de 3`; marca fila 2 → `2 de 3` (checkboxes `checked=true`); «Volver» a Mis Recetas; reabrir → **`0 de 3`** y todas las casillas `checked=false`. Al salir del provisional de Modo Cocina y volver (misma entrada), el marcado se mantiene (`2 de 3`) — coherente con «local en memoria»: el estado vive en la entrada de backstack y muere al salir del Detalle. |

## 3. Decisiones D1-D7 y U1

| Decisión | Resultado | Evidencia |
|---|---|---|
| **D1** tap en el cuerpo; Editar/Eliminar intactos | **pasa** | Tap sobre el cuerpo (título/historia/metadata) → Detalle ✓. Tap «Editar» (tarjeta de Tortilla) → **formulario «Editar Receta» con «Guardar cambios»**, no el Detalle ✓. Tap «Eliminar» → **diálogo «¿Eliminar «Tortilla de patatas»?…» con Cancelar/Eliminar**, no el Detalle ✓ (cancelado sin eliminar). Ver `MyRecipesScreen.kt` (capa clicable de fondo `RECIPE_BODY` detrás del contenido; `CardActions` fuera de la zona). |
| **D2** métricas 2×2 con Raciones base, sin precios | **pasa** | 2×2 en el dump y en la captura: Tiempo total (con desglose), Dificultad, Calorías por porción, **Raciones base**. Barrido de prohibidos en ambas vistas (es/en): **0 coincidencias** de `Coste`, `€`, `pers`/stepper, `opiniones`, `Valorac`. Sin selector `[-] N pers [+]` (RD03 fuera de alcance). |
| **D3** marcado LOCAL, se reinicia al salir | **pasa** | Ver S7: contador reactiva en vivo; reset total al salir y reentrar; nada persiste (no toca `domain/` ni Room). |
| **D4** CTA habilitada → destino provisional | **pasa** | Ver S5. El botón se compone siempre habilitado en `bottomBar` solo con estado `Detail` (`heightIn(min = 52.dp)`, icono sartén, `strings.xml` es/en). |
| **D5** badge reutilizado de `ui/components` | **pasa** | Componente único `ui/components/MatchBadge.kt` (mismo `testTag` por pantalla: `MyRecipesTestTags.RECIPE_MATCH` en la tarjeta, `RecipeDetailTestTags.MATCH` en el hero); el cálculo sigue en `domain/match/calculateMatch` (no reimplementado). Regresión de `mostrar-badge-de-coincidencia` dentro de la suite 94/94 en verde (`MatchBadgeE2ETest` incluido). `MomentBadge` extraído con el mismo criterio (RF-DR3 «como en la tarjeta»). |
| **D6** obligatorios primero, opcionales con píldora | **pasa** | Con el opcional creado **primero** en el formulario, el Detalle renderiza `Huevos → Patatas → Aceite(Opcional)` (`recipe.ingredients.sortedBy { it.optional }`), píldora `Opcional` visible; orden de nodos de checkbox confirmado por `uiautomator`. |
| **D7** decimal del idioma activo sin ceros sobrantes | **pasa** | es: `0,5 kg Patatas`, `0,25 l Aceite`; en: `0.5 kg Patatas`, `0.25 l Aceite` (comprobado en vivo cambiando el locale de aplicación). Unit `quantityFormatUsesActiveLocaleWithoutTrailingZeros` (0,5 / 0.5 / 2 / 0,25) en verde. |
| **U1 utensilios OMITIDOS en el MVP** | **pasa y se declara** | La sección «Utensilios necesarios» **NO aparece** en el Detalle: barrido de `uiautomator` en es y en en → 0 coincidencias de `Utensil`/`tool`; las dos capturas muestran solo hero, métricas, ingredientes, pasos y CTA. `Q-ver-detalle-de-receta-1` figura como resuelta el 2026-10-20 en `docs/risks-and-open-questions.md` (línea 109). |

**Fuera de alcance (barrido, todo ausente en el Detalle)**: escalado RD03 (sin stepper), pantalla real de Modo Cocina (solo provisional con título y «Volver»), Lista de la Compra (sin «Añadir a la lista»), disponibilidad por ingrediente (las casillas son locales y se reinician; no reflejan la Despensa), valoraciones/opiniones, precios (⚠ S02), editar/eliminar desde el Detalle.

## 4. Evidencia visual (locale por aplicación `es-ES`; presupuesto: 1 informe + 2 capturas)

- `docs/evidence/ver-detalle-de-receta/detalle-receta-completa.png` (1280×3913): ficha completa en español — hero con foto y badges «Almuerzo» + «100% Match», título e historia, métricas 2×2 (sin precios), checklist `0 de 3` con obligatorios primero y «0,25 l Aceite / Opcional», `Pasos / 2 Pasos` con chip `10 min` y CTA fija «Empezar Modo Cocina». **Nota metodológica**: es una captura larga compuesta por coser dos capturas recales consecutivas del mismo Detalle (misma composición; alineación de filas por hash de píxeles, desplazamiento 1057 px, ratio 0,886); la costura no introduce ni duplica contenido (fórmula `T[0..c) + B[c−d..H)` con la barra superior y la CTA fijas).
- `docs/evidence/ver-detalle-de-receta/detalle-receta-borrador.png` (1280×2856): borrador incompleto en español — placeholder sin foto, badge de momento junto al título sin badge de Coincidencia, `—` en calorías, `Sin ingredientes`, `Sin pasos` y CTA.
- Sin otros artefactos nuevos (presupuesto ≤ 3 respetado). Los datos de las capturas son fixtures ficticios creados por el validador.

## 5. Hallazgos

### Finding 1 (Low, no bloqueante): el tap en la portada (foto) de la tarjeta no abre el Detalle

- **Severity**: Low
- **Evidence**: `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` (`RecipeCard`, líneas 647-689): la portada con foto es un `Box`/`Image` sin `clickable`; la capa clicable `RECIPE_BODY` envuelve solo el cuerpo (título, historia, metadata). Tapping the cover therefore does nothing.
- **Why it matters**: D1 habla literalmente de «el **cuerpo** de la tarjeta» y el prototipo de `uxdocs/chefencasa_mis_recetas_y_crud` no define navegación por tap en la portada, así que **no hay incumplimiento de spec**; pero como publicar exige foto (S07), la portada (176 dp) es una superficie grande que responde al toque de forma muda y puede confundir al usuario.
- **Required change**: ninguno para aceptar esta feature. Recomendación de UX para un polish futuro (o para `cocinar-paso-a-paso` si retoca la tarjeta): extender el `clickable` también a la portada (con cuidado de no romper los `testTags` de la tarjeta, patrón «background clickable» ya usado) o, alternativamente, documentar que la portada no navega.

### Finding 2 (nota metodológica): alcance de la verificación manual de S3 y S6

- **Severity**: Low (nota, no defecto)
- **Evidence**: §2. S3 «sin salir de la pantalla» y S6 (id invalc) no tienen recorrido por la UI real: no hay entrada a la Despensa desde el Detalle ni forma de navegar a un id inexistente.
- **Why it matters**: transparencia del informe.
- **Required change**: ninguno; quedan cubiertos por los tests instrumentados/unit reranqueados por este validador (7/7 y 6/6), que ejecutan exactamente esos comportamientos en el emulador.

## 6. Seguridad

Checklist por feature **sin hallazgos**: el diff no introduce secretos, credenciales, tokens ni la API key; no añade red, permisos, dependencias ni ficheros nuevos (`AppModule.kt` solo registra el `viewModel` con `parameters`); el id de ruta llega como `NavType.StringType` y se lee con `RecipeRepository.getRecipe(id)` (consultas parametrizadas de Room, sin concatenación); la foto se decodifica con `BitmapFactory.decodeFile` + `runCatching` sobre la ruta interna del `PhotoStore` (solo lectura, sin path traversal nuevo); la UI no contiene reglas de negocio (D3/D6/D7 son presentacionales y el Coincidencia vive en `domain/`). Nota sin impacto: `rememberBitmap` decodifica el archivo a tamaño completo sin muestreo (patrón ya preexistente en `MyRecipesScreen`, no introduce riesgo nuevo; un `inSampleSize` sería mejora futura). Las capturas contienen solo datos ficticios; el dispositivo quedó limpio (`pm clear com.whatsforlunch.app`, locale de aplicación revocado, ficheros temporales de `/sdcard` borrados y emulador apagado con `adb emu kill`).

## 7. Alcance, arquitectura, docs duraderos y traspaso

- **Alcance**: correcto. Sin RD03, sin Modo Cocina real, sin Lista de la Compra, sin disponibilidad de Despensa, sin valoraciones, sin precios, sin utensilios (U1). Solo se tocaron los ficheros de `Expected File Changes` (+ este informe y las 2 capturas, dentro del presupuesto de 3 artefactos). No se modificaron scripts del harness, `uxdocs/`, `.opencode/` ni el esquema Room (sin migración).
- **Arquitectura**: correcta. MVVM + `StateFlow` (`RecipeDetailViewModel` con `combine(getRecipe + observePantryItems)` y `stateIn(WhileSubscribed)`), reglas en `domain/` (`calculateMatch` reutilizado, no duplicado), Koin en `AppModule.kt` con `recipeId` por parámetro, pantalla sin estado + `RecipeDetailRoute`, navegación por callbacks desde `AppNavHost`, `testTags` en objetos `*TestTags`, textos solo en `strings.xml` es/en (verificado en vivo en ambos idiomas), identificadores en inglés y comentarios/docs en español.
- **Docs duraderos**: según `Durable Documentation Impact` de la spec: sin cambios necesarios en `ARCHITECTURE.md`/`CONSTRAINTS.md`/`AGENTS.md`/`docs/domain-model.md` (no existen reglas nuevas ni cambio de capas). `docs/risks-and-open-questions.md` ya recoge `Q-ver-detalle-de-receta-1` como resuelta (U1). `DESIGN.md` respeta los puntos verificables: CTA ≥ 52 dp, hero ~272 dp con `rounded.xl`, checklist con contador en `secondaryContainer`, chips de temporizador `tertiary`, paleta del tema.
- **Traspaso**: no se modificó `feature_list.json` (sigue `passing` + evidence del implementer) ni `PROGRESS.md`; este informe y las 2 capturas son el input para que el orquestador lo ponga a `accepted` con la evidence resumida y actualice `PROGRESS.md` + `docs/progress-history.md`.

## 8. Veredicto

**`accept`** — gates reranqueados por el validador en verde (init exit 0; lint 0 errores; unit **56/56** con `--rerun-tasks`, de ellos `RecipeDetailViewModelTest` 6/6; `assembleDebug` OK; E2E focalizada `RecipeDetailE2ETest` **7/7** y regresión completa **94/94, 0 fallos/0 omitidos** en `Pixel_10_Pro` API 37), más una reproducción manual en el emulador de S1, S2, S4, S5 y S7, de D1-D7, de la **omisión de utensilios (U1)** y de la ausencia de todo el contenido fuera de alcance, con evidencia visual es (2 capturas, presupuesto respetado). Sin hallazgos High/Medium; los dos hallazgos Low son una nota de UX no exigida por la spec y una nota metodológica. Sin hallazgos de seguridad. Estado recomendado para el orquestador: **`accepted`** en `feature_list.json` (evidencia resumida de este informe) y actualización de `PROGRESS.md` + `docs/progress-history.md`.

Checklist de la spec (`Validator Checklist`), marcado al cerrar:

- [x] La implementación se queda dentro del alcance (sin escalado RD03, sin Modo Cocina real, sin Lista de la Compra, sin utensilios: U1 resuelta como omisión en el MVP).
- [x] Los escenarios de aceptación pasan (S1-S7) y las decisiones D1-D7 se respetan (S3/S6 con evidencia E2E+unit reranqueados; el resto reproducido manualmente).
- [x] U1 (utensilios) está resuelta como omisión: la sección «Utensilios necesarios» NO aparece en el Detalle y este informe lo declara (§3).
- [x] El badge de Coincidencia es el componente reutilizado (D5), no una reimplementación; la regresión de `mostrar-badge-de-coincidencia` pasa (suite 94/94).
- [x] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature (1 informe + 2 capturas).
- [x] Cobertura E2E persistente añadida (`RecipeDetailE2ETest`, 7 tests) y suite completa en verde (94/94).
- [x] `feature_list.json` (`passing` + evidence) y `PROGRESS.md` reflejan el estado real (el traspaso a `accepted` y la nota final los hace el orquestador).
- [x] No se añadió comportamiento de producto ni trabajo de otras features (sin selector de raciones, sin «Añadir a la lista», sin pantalla de Modo Cocina definitiva).
