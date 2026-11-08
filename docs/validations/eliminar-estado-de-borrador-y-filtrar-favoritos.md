# Validación independiente — `eliminar-estado-de-borrador-y-filtrar-favoritos`

- **Veredicto: `accept`** (2026-11-07, sesión `feature-validator` independiente del implementador).
- Spec: `docs/specs/eliminar-estado-de-borrador-y-filtrar-favoritos.md` (RF-EBF1–8, S1–S7 con verify 1–5, D1–D11).
- Rama `feature/eliminar-estado-de-borrador-y-filtrar-favoritos`, todo sin commitear. Sin cambios de código hechos por el validador.
- Entorno de re-ejecución: AVD `Pixel_10_Pro(AVD) - 17` (API 37, 1280×2856, es-ES), `emulator-5554`, Windows 11, Gradle 8.14.3 / JDK corretto-17.0.20.

## 1. Contrato de validación aplicado

D11 (usuario, 2026-11-07, **supera D4**): la foto es SIEMPRE opcional en alta y edición; `RecipeValidation.validateForSave(recipe)` solo exige título, ≥1 Ingrediente y ≥1 Paso válidos y ninguna fila inválida; `RecipeRequirement.PHOTO` y `create_recipe_save_error_photo` retirados; «Sin foto (opcional)» en el hueco del formulario. El alineado de los textos de `feature_list.json` con D11 fue autorizado expresamente por el usuario el 2026-11-07 y **no se considera hallazgo**.

## 2. Checks ejecutados

| Check | Método | Resultado |
|---|---|---|
| `init.ps1` | **Re-ejecutado** | exit 0 («Entorno listo»); app en ejecución verificada con `pidof` (1159); sin procesos largos lanzados |
| `lintDebug --rerun-tasks` | **Re-ejecutado** | **0 errores / 49 warnings** (línea base; XML del reporte: 49 Warning) |
| `testDebugUnitTest --rerun-tasks` | **Re-ejecutado** | **42 suites / 305 tests / 0 fallos / 0 omitidos** (reconciliado desde los XML de `app/build/test-results`) |
| `assembleDebug` (misma tanda `--rerun-tasks`) | **Re-ejecutado** | BUILD SUCCESSFUL |
| Paridad es/en | **Re-ejecutado** (recuento de claves) | **322/322 strings + 5/5 plurals**, sin duplicados |
| Batería E2E `connectedDebugAndroidTest` | **Solo inspeccionado** (evidencia exacta y consistente, sin drift) | 220 tests / 0 fallos / 0 errores / 2 omitidos |
| Migración Room v4→v5 | Inspeccionado (dentro de la batería) | `RecipeMigrationTest` 2/2 en el XML |
| Capturas 3× PNG | **Inspeccionadas visualmente** | 1280×2856, cabecera PNG, conformes a lo descrito, datos ficticios |

### 2.1 Reconciliación del tier caro (base de la inspección)

- XML conservado en `docs/evidence/eliminar-estado-de-borrador-y-filtrar-favoritos/TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`: cabecera `tests="220" failures="0" errors="0" skipped="2"`, timestamp `2026-11-07T20:11:02`, `time=905.913` (≈15m 18s, coincide con lo declarado; la consola AGP «Finished 222» es la doble cuenta conocida de los omitidos).
- **Recuento por clase reconciliado**: los 27 `classname` del XML suman 220 y casan 1:1 con la tabla de `informe.md` (CreateRecipeE2ETest 23 + CreateRecipeApp 1 + MyRecipesCards 2 = 26 `@Test` de su fichero; EditRecipeE2ETest 12 + EditRecipeApp 1 + EditRecipeRoute 1 = 14 de su fichero; etc.).
- **Inventario de código**: 220 `@Test` en `app/src/androidTest` (23 ficheros) = 220 del XML. Los 2 omitidos son exactamente los `Assume` de `SettingsE2ETest` (`savedKeyMarksConfigured…`, `deletingTheKeyAsks…`).
- **Sin drift**: ninguna fuente de `app/src` tiene mtime posterior al timestamp de la batería; el change set validado es el que se ejecutó.
- Las dos baterías previas descartadas quedan documentadas en `informe.md` §3 con firmas de fallo atribuibles al AVD (`gradlew --stop` externo; `INSTRUMENTATION_ABORTED: System has crashed` con 2 fallos de teardown/test muerto que pasan en verde en la limpia) — coherente con los flakes conocidos del AVD; no requieren re-ejecución.

### 2.2 Cobertura del `verification` de `feature_list.json`

| Item | Cobertura en evidencia |
|---|---|
| 1. Alta: bloqueada sin obligatorios/filas inválidas; foto siempre opcional; guardado único; sin distintivo | `CreateRecipeE2ETest` (`saveIsBlockedWithoutTitle`, `saveWithoutIngredientIsBlocked`, `invalidIngredientRowsAreListedAsBlockingRequirement`, `saveWithoutPhotoSavesRecipe` con `photoPath == null`, `saveWithPhotoSavesRecipe`, `saveIsDisabledUntilRequirementsAreMet`, `blockingErrorsAreLocalizedInEnglish`) + `CreateRecipeAppE2ETest.savedRecipeAppearsInMyRecipesWithoutStatusBadges` (app real, BD real) |
| 2. Edición: «Guardar cambios» exige obligatorios; sin distintivo | `EditRecipeE2ETest` (`editingWithoutRequirementsBlocksSavingChanges`, `editingPreloadsRecipeFieldsAndSingleSaveDock`, `savingChangesKeepsIdAndOrigin`, `editingHasNoHeaderSaveActionAndDockSaves`) |
| 3. Sugerencia y Propuesta del Pinche: sin foto y sin distintivo | `AiSuggestionsE2ETest.acceptingSavesTheSuggestionAsARecipeVisibleInMyRecipes` y `PincheE2ETest.savingTheProposalCreatesARecipeInMyRecipesWithoutDuplicates` (ambas: `photoPath == null`, textos/tags de estado ausentes) |
| 4. Chip «Favoritas»: solo favoritas, combinación, «sin resultados», restablecer | `SearchAndFilterRecipesE2ETest` (`favoritesChipShowsOnlyFavoritesAndCombinesWithOtherFilters`, `resetClearsSearchMomentAndFavoritesFilter`, `togglingFavoritesOffShowsAllRecipesAgain`, posición al final de la fila + icono) |
| 5. Sin textos «Borrador»/«Publicada»; textos nuevos es/en | Aserciones de ausencia en `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `MyRecipesCardsE2ETest`, `AiSuggestionsE2ETest`, `PincheE2ETest`; inglés en `blockingErrorsAreLocalizedInEnglish`, `editEnglishLabels` («Save changes») y chip «Favorites»; paridad 322/322 + 5/5 |
| 6. Unit: `validateForSave` (título, ≥1 Ingrediente, ≥1 Paso, sin filas inválidas; foto opcional; alta y edición) | `RecipeValidationTest` (7 tests de guardado, incl. `photoIsAlwaysOptional`, `saveIsBlockedByAnInvalidIngredient/Step`) + `CreateRecipeViewModelTest` (`createSavesWithoutPhoto`, `editingARecipeWithoutPhotoSavesWithoutPhoto`) |

**Conclusión**: el `verification` queda **cubierto** por la evidencia.

## 3. Spec, alcance y arquitectura

- RF-EBF1–8 implementadas: `RecipeStatus.kt` eliminado, `Recipe.status` fuera del modelo y de `RecipeEntity`; `MIGRATION_4_5` recrea `recipes` sin `status` (Room v5, registrada en `AppModule` y en los 4 builders legados de tests); dock con botón único `create_recipe_save` («Guardar»/«Guardar cambios»); `StatusBadge`/`StatusPill` retirados; `filterRecipes(..., favoritesOnly)` combinado en AND; chip «Favoritas» al final de la fila (`Role.Checkbox`, tags `my_recipes_filter_favorites(_icon)`) y reset ampliado (RF-EBF8).
- Non-goals respetados: Detalle intacto (D7, `RecipeDetailScreen` sin cambios), sin marcado de favoritos nuevo en tarjetas/Detalle, IA/prompts intactos salvo el destino sin estado, filtros sin persistir, specs/evidencias históricas sin reescribir (solo `notes` de enmienda), Despensa/Lista/navegación sin tocar.
- Tags retirados (`create_recipe_status_badge`, `my_recipes_card_status`, `create_recipe_save_draft`, `create_recipe_publish`) solo aparecen como literales en aserciones de ausencia (`assertCountEquals(0)`) — exactamente el uso permitido. Cero referencias a `RecipeStatus`, `validateForDraft`, `validateForPublish`, `editingStatus`, `onSaveDraft/onPublish/onSaveChanges`, `canPublish/publishErrors` en `app/src`.
- Arquitectura: sin reglas de negocio en la UI — `RecipeValidation` y `filterRecipes` viven en `domain/` puros con tests unitarios; el `FilterState` intermedio de `MyRecipesViewModel` está documentado en la spec («Hallazgos de implementación»). Strings en `values/`/`values-en/`, convenciones del repo respetadas.
- `feature_list.json`: el diff de la entrada solo toca `status` (`not_started`→`passing`), `evidence` (5 items verificados) y el alineado de textos con D11 **autorizado por el usuario** — sin cambios en otras features más allá de sus `notes` de enmienda ya declarados.

## 4. Seguridad (checklist acotado a la feature)

Sin hallazgos. Sin secretos ni PII en el diff ni en la evidencia (capturas con datos ficticios: «Receta de evidencia»); sin dependencias ni permisos nuevos (ni `build.gradle.kts` ni `AndroidManifest.xml` en el diff); migración SQL estática sin superficie de inyección; la inyección de `ImageGenerator` falso es solo de tests vía Koin; logs sin datos sensibles; la API key no se toca.

## 5. Documentación durable

Cumple el `Durable Documentation Impact` de la spec: `docs/domain-model.md` (ciclo de vida sin estados, obligatorios sin foto D11, RD07/RD16/RD18, casos límites y nota de implementación), `docs/risks-and-open-questions.md` (fila `⚠ S07  revisada + `Q-eliminar-estado-de-borrador-y-filtrar-favoritos-1..5`), `CONTEXT.md` (alta de «Favorita», «Propuesta del Pinche» reescrita, «Borrador» retirado del glosario y términos retirados listados al final) y `DESIGN.md` (tabla de pantallas y estados). `AGENTS.md` sin cambios (correcto). `PROGRESS.md` y `docs/progress-history.md` al día. Ver hallazgos F1–F2 (Low) por redacción obsoleta residual.

## 6. Hallazgos

### F1: término «se publica» residual en RD21
- Severidad: Low.
- Evidencia: `docs/domain-model.md` línea 79 (RD21): «una Receta sin vínculos se guarda, **se publica** y se cocina igual» — línea no tocada por el diff pese a que el fichero estaba en alcance.
- Por qué importa: contradice la retirada del término «publicar» (`CONTEXT.md`, glosario de términos retirados) y RF-EBF2; lectura errónea para sesiones futuras.
- Cambio requerido: reescribir como «se guarda y se cocina igual».
- Verificación tras el fix: ninguna mención a «publicar» como transición vigente en `docs/domain-model.md`.

### F2: `docs/build-brief.md` conserva el guardado dual
- Severidad: Low.
- Evidencia: `docs/build-brief.md` línea 39 («desde ella se guarda como Borrador o se descarta») y línea 40 («guardado dual Borrador/Publicar») — brief de producto que `AGENTS.md` manda leer al inicio de cada sesión.
- Por qué importa: es el único documento de lectura obligatoria que describe el comportamiento antiguo como vigente (mitigado porque `CONTEXT.md` —que se lee antes— da los términos como retirados).
- Cambio requerido: línea 39 → «desde ella se guarda o se descarta»; línea 40 → «guardado único con obligatorios (sin estado de borrador)».
- Verificación tras el fix: ninguna mención a Borrador/Publicar como comportamiento vigente en `build-brief.md`.

## 7. Notas (sin cambio requerido)

- **N1**: `create_recipe_save_error_photo` aparece solo en la documentación que registra su baja (spec D11, `informe.md`, `PROGRESS.md`, historial) y **cero veces en `app/src`**. Interpretación del contrato «no debe referenciarse en ningún sitio» = sin referencias en código/strings (a diferencia de los tags, permitidos en aserciones de ausencia); nombrar la clave retirada en el registro de la decisión es necesario para documentarla.
- **N2**: el bloqueo por fila de Paso inválida se cubre en unit (`saveIsBlockedByAnInvalidStep`); en E2E solo la fila de Ingrediente inválida tiene aserción propia — el mismo `SaveErrors` renderiza ambos requisitos desde el mismo set. Suficiente por composición.
- **N3**: el verify 4 siembra `favorite = true` en el repositorio para los tests del chip; «marcar favorita» por UI está cubierto aparte (`favoriteToggleFlipsState`, pre-carga en edición con toggle `IsOn`).
- **N4**: `DESIGN.md` línea 100 describe el material `uxdocs/` («guardado Borrador/Publicar») — descripción fiel del prototipo de referencia (solo lectura, `P16`), no del comportamiento de la app.
- **N5**: el XML de la batería es un `testsuite` fusionado de AGP con el nombre de la primera clase; el desglose real va por `classname` de cada `testcase` (reconciliado en §2.1).
- **N6**: `Q-eliminar-estado-de-borrador-y-filtrar-favoritos-1` registra la resolución con D4 (superada por D11); la superación queda documentada en Q-5 y en `⚠ S07`. Coherente como registro histórico.

## 8. Veredicto y estado recomendado

- **`accept`**: la implementación cumple la spec (RF-EBF1–8, S1–S7, D1–D11), la evidencia de verificación es exacta, consistente y reconciliada, el `verification` de `feature_list.json` queda cubierto, y alcance/arquitectura/seguridad/docs durables están conformes. Hallazgos solo 2 Low documentales (F1, F2) + notas N1–N6.
- Recomendación de persistencia (a cargo del orquestador): `feature_list.json` → `accepted` (solo `status`/`evidence`), aplicar F1–F2 al persistir y registrar esta validación en `PROGRESS.md`/historial.
