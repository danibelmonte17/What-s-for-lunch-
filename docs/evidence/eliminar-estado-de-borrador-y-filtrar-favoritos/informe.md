# Evidencia de implementación — `eliminar-estado-de-borrador-y-filtrar-favoritos`

Fecha de la verificación: 2026-11-07.
Entorno: emulador `Pixel_10_Pro` (AVD, API 37, 1280×2856, 480 dpi), locale `es-ES`, rama `feature/eliminar-estado-de-borrador-y-filtrar-favoritos`.
Este informe recoge la evidencia de la autoverificación del implementador. La validación independiente corresponde a `feature-validator` y su informe se guardará en `docs/validations/eliminar-estado-de-borrador-y-filtrar-favoritos.md`; esta carpeta queda como insumo para esa validación.

## 1. Alcance implementado

Requisitos funcionales del brief (RF-EBF1..8) y decisiones cerradas en la spec (`docs/specs/eliminar-estado-de-borrador-y-filtrar-favoritos.md`):

- **D1 — Roles en alta sin foto**: «Sugerencia» y «Pinche» admiten alta sin foto.
- **D2 — Sin distintivos de estado**: retirados los badges de estado de las tarjetas de Mis recetas.
- **D3 — Chip combinable**: «Favoritas» se combina con el resto de filtros, no los sustituye.
- **D5 — Room v5**: `Recipe` sin campo `status`; migración `MIGRATION_4_5` (v4→v5) que elimina la columna.
- **D6 — Guardado único**: un único botón «Guardar» (alta) / «Guardar cambios» (edición) con tag `create_recipe_save`; retirados los botones de borrador y publicar.
- **D7 — Detalle intacto**: la pantalla de detalle de receta no cambia.
- **D8 — Chip «Favoritas»**: al final de la fila de filtros de Mis recetas, semántica `Role.Checkbox`, tags `my_recipes_filter_favorites` / `my_recipes_filter_favorites_icon`.
- **D9 — Validación de guardado**: `validateForSave(recipe)` (título no vacío, ≥1 ingrediente válido, ≥1 paso válido, sin filas inválidas).
- **D10 — Renombrado de tags**: `create_recipe_publish*` → `create_recipe_save*`.
- **D11 — Foto siempre opcional (usuario, 2026-11-07; SUPERA D4)**: en alta y edición la foto nunca es requisito de guardado. `RecipeRequirement.PHOTO` y la cadena `create_recipe_save_error_photo` quedan retirados; el hueco del formulario muestra «Sin foto (opcional)», que es coherente con el comportamiento real. La decisión D4 (foto obligatoria en el rol Sugerencia) queda obsoleta.

## 2. Método de captura de la evidencia visual

Las tres capturas de `docs/evidence/eliminar-estado-de-borrador-y-filtrar-favoritos/` se tomaron con un test instrumentado **temporal** (`EvidenceCaptureE2ETest.kt`, ya eliminado del repositorio tras extraerlas) que:

1. sembraba la base de datos real de Room con recetas de prueba;
2. recorría la UI real de la app en el emulador;
3. capturaba la pantalla con `uiAutomation.takeScreenshot()` (PNG a MediaStore);
4. recuperaba los PNG vía `adb pull`.

Formato: 1280×2856 px, locale `es-ES`:

- `mis-recetas-favoritas-es.png` — Mis recetas con el chip «Favoritas» activo y el filtrado aplicado (D3, D8).
- `mis-recetas-sin-estados-es.png` — tarjetas de Mis recetas sin distintivos de estado (D2).
- `formulario-guardado-unico-es.png` — formulario con el hueco «Sin foto (opcional)» y el único botón «Guardar» (D6, D10, D11).

## 3. Batería E2E completa

Ejecución `./gradlew :app:connectedDebugAndroidTest` sobre el emulador, 2026-11-07, **BUILD SUCCESSFUL en 15m 18s**.

- Inventario de `@Test` en `app/src/androidTest`: **220**.
- Resultado: **220 tests, 0 fallos, 0 errores, 2 omitidos** (cabecera del XML: `tests="220" failures="0" errors="0" skipped="2"`).
- Los 2 omitidos son los `Assume` de `SettingsE2ETest` (pruebas que exigen condiciones de dispositivo concretas). Nota: la consola de AGP reporta «Finished 222 tests» por doble cuenta de los omitidos; la cifra válida es la del XML: 220.

Recuento por clase (fuente: `TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`, copiado en esta carpeta):

| Clase | Tests | Fallos | Omitidos |
|---|---|---|---|
| AiSuggestionsE2ETest | 16 | 0 | 0 |
| AppLaunchE2ETest | 1 | 0 | 0 |
| CameraXScanImageSourceTest | 1 | 0 | 0 |
| CookModeE2ETest | 7 | 0 | 0 |
| CookModeTimerE2ETest | 8 | 0 | 0 |
| CookModeVoiceE2ETest | 10 | 0 | 0 |
| CreateRecipeAppE2ETest | 1 | 0 | 0 |
| CreateRecipeE2ETest | 23 | 0 | 0 |
| DeleteRecipeE2ETest | 9 | 0 | 0 |
| EditRecipeAppE2ETest | 1 | 0 | 0 |
| EditRecipeE2ETest | 12 | 0 | 0 |
| EditRecipeRouteE2ETest | 1 | 0 | 0 |
| KeystoreAiKeyStoreTest | 3 | 0 | 0 |
| LanguageSwitchE2ETest | 8 | 0 | 0 |
| MatchBadgeE2ETest | 2 | 0 | 0 |
| MyRecipesCardsE2ETest | 2 | 0 | 0 |
| MyRecipesLocalizationE2ETest | 6 | 0 | 0 |
| PantryLocalizationE2ETest | 2 | 0 | 0 |
| PantryManagementE2ETest | 5 | 0 | 0 |
| PantryRecipesE2ETest | 8 | 0 | 0 |
| PincheE2ETest | 11 | 0 | 0 |
| RecipeDetailE2ETest | 12 | 0 | 0 |
| RecipeMigrationTest | 2 | 0 | 0 |
| ScanPantryE2ETest | 25 | 0 | 0 |
| SearchAndFilterRecipesE2ETest | 12 | 0 | 0 |
| SettingsE2ETest | 5 | 0 | 2 |
| ShoppingListE2ETest | 27 | 0 | 0 |
| **Total** | **220** | **0** | **2** |

### Ejecuciones previas descartadas (artefactos del AVD, no regresiones)

Dos baterías anteriores de esta misma sesión de cierre no sirven como evidencia y quedan documentadas aquí por transparencia:

1. Interrumpida en 140/220 (0 fallos hasta ese punto) por un `gradlew --stop` externo («Gradle build daemon has been stopped: stop command received»).
2. Abortada en 83/220 con `INSTRUMENTATION_ABORTED: System has crashed` (crash del `system_server` del AVD). Registró 2 fallos igualmente atribuibles al crash: `EditRecipeE2ETest.photolessRecipeEditsAndSavesWithoutPhoto` (flake de teardown conocido: «Activity never becomes requested state [DESTROYED]» tras pasar el cuerpo) y `EditRecipeE2ETest.editingWithoutRequirementsBlocksSavingChanges` (`<failure></failure>` vacío, test muerto con el crash). Ambos tests pasan en la batería limpia de la sección anterior.

## 4. Migración de base de datos v4→v5

`RecipeMigrationTest` (2 tests, en verde en la batería): la migración `MIGRATION_4_5` elimina la columna `status` de la tabla de recetas y los datos existentes sobreviven sin pérdida. Todo test que construye la BD registra `MIGRATION_4_5`.

## 5. Paridad de cadenas es/en

Verificación de `app/src/main/res/values/strings.xml` frente a `values-en/`: **322/322 claves + 5/5 plurales** (nueva línea base tras D11, que retiró `create_recipe_save_error_photo` y añadió las cadenas del guardado único y el chip de favoritos).

## 6. Gates estáticos y unitarios (tras D11)

- `lintDebug`: **0 errores / 49 warnings** (línea base, sin regresión).
- `testDebugUnitTest`: **305 tests / 0 fallos**.
- `assembleDebug`: correcto, sin errores.
- E2E focalizada de las áreas de la feature (CreateRecipeE2ETest, CreateRecipeAppE2ETest, MyRecipesCardsE2ETest, EditRecipeE2ETest, EditRecipeRouteE2ETest, EditRecipeAppE2ETest, RecipeMigrationTest): **40/40 en verde** antes de la batería completa.
