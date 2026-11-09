# Evidencia de `mover-opciones-a-bottom-bar` — implementación autoverificada

- **Sesión**: 114 (`feature-implementer`; la spec se creó en la sesión 113, `feature-spec`, 2026-11-08).
- **Rama**: `feature/mover-opciones-a-bottom-bar` (sin commitear; diffs en el worktree).
- **Entorno**: AVD dedicado `Pixel_10_Pro_MoverOpciones` (API 37, 1280×2856 @480 dpi) en `emulator-5556`, usado con `$env:ANDROID_SERIAL = "emulator-5556"` para no pisar el `emulator-5554` de la sesión activa `mover-escaneo-a-la-despensa`. El AVD no tiene locale de sistema fijado: arranca en inglés y se cambió a español desde Ajustes para las capturas.
- **Estado**: `passing` (implementada y autoverificada; **no es aceptación** — pendiente de `feature-validator`).

## 1. Gates del harness

| Gate | Comando | Resultado |
|---|---|---|
| Lint | `lint.ps1` / `lintDebug` | **0 errores / 48 warnings** = única línea base (49). Se re-analizó sobre las fuentes finales (los tags `TOP_BAR_TITLE` de producción son de las 13:57 y el lint posterior re-ejecutó `lintAnalyzeDebug` manteniendo el recuento). |
| Unit | `unit.ps1` + `testDebugUnitTest` | **42 suites / 305 tests / 0 fallos** (recuento reconciliado desde `app/build/test-results/testDebugUnitTest/*.xml`). |
| Ensamblado | `assembleDebug` | **BUILD SUCCESSFUL**. |
| Paridad es/en | recuento de claves `<string>`/`<plurals>` | **319/319 claves + 5/5 plurals** (antes 322/322; se retiraron 3 claves de ambos locales, ver §5). |

## 2. E2E focalizada (7 clases, 89 tests — 89/89 verde en `emulator-5556`)

`BottomBarE2ETest` (S1–S5 de la spec), `PantryManagementE2ETest`, `PincheE2ETest`, `ShoppingListE2ETest`, `PantryRecipesE2ETest`, `MyRecipesLocalizationE2ETest` y `ScanPantryE2ETest`:
**BUILD SUCCESSFUL, 89/89, 0 fallos** (7 m 38 s). Log: `test-bateria-focalizada.log`.

| Escenario | Test(s) | Cubre |
|---|---|---|
| S1 (verify 1) | `bottomBarShowsFourTabsInOrderAndEachOpensItsScreen` | 4 pestañas en orden (Mis Recetas, Despensa, Lista de la Compra, Pinche), cada una abre su pantalla y marca la activa (RF-MOB1). |
| S2 (verify 2) | `topBarKeepsOnlySettingsAndPantryRecipesOpensFromTheFab` | Top bar solo «Ajustes»; «Recetas con mi Despensa» se abre desde el FAB (RF-MOB2/RF-MOB3). |
| S4 (D1) | `activeTabIsHighlightedBackIsRemovedAndTheBackStackStaysCoherent` | Pestaña activa marcada, «Volver» retirado, reselección → vuelta al inicio sin perder datos, back coherente hasta salir (RF-MOB4/5/6). |
| S5 (D3) | `emptyStateShowsNoFloatingButtons` / `recipesShowBothFloatingButtonsStacked` | Sin Recetas no hay FABs; con Recetas, los dos FABs apilados (RF-MOB3). |

Las **4 clases afectadas** por la ambigüedad título-vs-etiqueta y el ciclo de vida (`BottomBarE2ETest`, `MyRecipesLocalizationE2ETest`, `LanguageSwitchE2ETest`, `PantryLocalizationE2ETest`) también pasan **aisladas: 23/23, 0 fallos**.

## 3. Batería E2E completa (`e2e.ps1`, 2026-11-08) — regresión

- **227 tests del runner** (= inventario `@Test` actual, 220 de línea base + 5 `BottomBarE2ETest` + 2 S3 nuevos de `MyRecipesLocalizationE2ETest`): **225 verdes + 1 fallo + 1 omitido**. Log: `test-bateria-completa.log`.
- **Recuento por clase** (N1 de la validación, sesión 115): el XML/HTML de la batería en `build/` fue sobrescrito por el rerun aislado de `SettingsE2ETest` (14:47), así que el recuento por clase se declara desde el **inventario `@Test` del código**, que cuadra 1:1 con las 227 del runner (log: progreso hasta `227/227 completed. (1 skipped) (1 failed)`):

  | Clase | Tests | | Clase | Tests |
  |---|---|---|---|---|
  | `AiSuggestionsE2ETest` | 16 | | `PantryRecipesE2ETest` | 8 |
  | `AppLaunchE2ETest` | 1 | | `PincheE2ETest` | 11 |
  | `BottomBarE2ETest` (nueva) | 5 | | `RecipeDetailE2ETest` | 12 |
  | `CameraXScanImageSourceTest` | 1 | | `RecipeMigrationTest` | 2 |
  | `CookModeE2ETest` | 7 | | `ScanPantryE2ETest` | 25 |
  | `CookModeTimerE2ETest` | 8 | | `SearchAndFilterRecipesE2ETest` | 12 |
  | `CookModeVoiceE2ETest` | 10 | | `SettingsE2ETest` | 5 |
  | `CreateRecipeE2ETest` | 26 | | `ShoppingListE2ETest` | 27 |
  | `DeleteRecipeE2ETest` | 9 | | `LanguageSwitchE2ETest` | 8 |
  | `EditRecipeE2ETest` | 14 | | `MatchBadgeE2ETest` | 2 |
  | `KeystoreAiKeyStoreTest` | 3 | | `MyRecipesLocalizationE2ETest` | 8 |
  | `PantryLocalizationE2ETest` | 2 | | `PantryManagementE2ETest` | 5 |

  **Total = 227** (24 clases). El único desvío entre inventario y ejecución es el 1 omitido (`SettingsE2ETest#buildProvidedKey…`) y el 1 fallo (`SettingsE2ETest#deletingTheKey…`), ambos de `SettingsE2ETest`.
- **1 omitido**: `SettingsE2ETest#buildProvidedKeyShowsConfiguredNoteAndHidesDelete`, por `assumeTrue(buildKeyActive)` con el build **sin** `AI_API_KEY` (el `local.properties` de este entorno no la lleva).
- **1 fallo (flake ajeno a esta feature)**: `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures`, `ComposeTimeoutException` en un `waitUntil` de 5 s (línea 166) esperando `CreateRecipeTestTags.NOTICE` tras `PHOTO_GENERATE`. Análisis:
  - **Dos baterías completas consecutivas** fallaron en el mismo test y la misma línea.
  - **En aislamiento pasa**: `SettingsE2ETest` completo → 5 tests, 4 verdes + 1 omitido, 0 fallos (37 s).
  - **No está en el diff** de esta feature (`git diff HEAD --name-only -- app/src/androidTest` no incluye `SettingsE2ETest.kt`), y el flujo (Ajustes → borrar key → aviso de «Generar con IA» en el formulario) no toca la bottom bar.
  - El logcat de la ventana de la batería muestra **contención de CPU del host**: `Davey! duration=2061 ms`, `Skipped 74 frames`, pausas de GC de ~1,4 s en `system_server` — dos sesiones + dos emuladores compitiendo por la CPU; un `waitUntil` de 5 s puede vencer con un frame de 2 s de por medio.
  - **Contexto**: este test lleva `assumeFalse(buildKeyActive)` y **jamás corrió en las baterías históricas** (los builds de las sesiones 103/111 sí llevaban `AI_API_KEY` y lo omitían); ésta es la primera vez que se ejecuta en contexto de batería completa.
  - Clasificación: **flake de timing bajo carga**, no regresión de esta feature (precedentes de flakes documentados y re-ejecución aislada en verde en las sesiones 91/102c).

## 4. Comprobaciones manuales (adb + uiautomator, `emulator-5556`, es-ES) y capturas

Técnica: `adb shell uiautomator dump` + `pull` + parseo del XML para localizar nodos por `text`/`content-desc`; capturas con `cmd /c "adb exec-out screencap -p > <file>.png"` (redirección binaria de `cmd`, nunca la `>` de PowerShell). Ambas capturas verificadas PNG y **1280×2856**.

1. **Mis Recetas (S2 + S5/D3)**: tarjeta «Tortilla de patata» con badge de Coincidencia, top bar con solo «Ajustes» (sin acciones de navegación), **dos FABs apilados** «Recetas con mi Despensa» (encima) y «Escanear la Despensa» (debajo), y barra inferior con las 4 pestañas («Mis Recetas» activa). Captura: **`mis-recetas-bottom-bar-es.png`** (172 875 bytes).
2. **Despensa (destino, D1/D4)**: estado vacío, top bar con el título **«Despensa» y sin «Volver»**, pestaña «Despensa» resaltada y barra inferior con las 4 pestañas. Captura: **`despensa-bottom-bar-es.png`** (110 126 bytes).

La semilla (una receta) se creó a mano por el formulario de alta (título + 1 Ingrediente + 1 Paso + «Guardar»), sin llamadas de red.

## 5. Hallazgos de implementación (registrados también en la spec)

- `ScannerRoute.onSaved` aterriza en la pestaña Despensa con `openTab(AppTab.PANTRY)` (el Escáner no queda en el back stack).
- Se retiran 3 claves de ambos locales al desparecer sus acciones de top bar: `action_open_shopping_list`, `pantry_open_accessibility`, `pinche_open_accessibility` → paridad **319/319 + 5/5**.
- La regla de lint `ModifierParameter` exige `modifier` como primer parámetro opcional: se reordenó en `MyRecipesScreen`/`MyRecipesRoute`, `PantryScreen`/`PantryRoute`, `ShoppingListScreen`/`ShoppingListRoute` y `PincheScreen`/`PincheRoute` (incluida `MessageList`).
- Los títulos de top bar de Mis Recetas y Despensa llevan testTag `TOP_BAR_TITLE` (`MyRecipesTestTags`, `PantryTestTags`): sus textos coinciden con las etiquetas de pestaña y la tag desambigua las aserciones E2E de localización (S3). Dos tests (`MyRecipesLocalizationE2ETest:79/90`, `LanguageSwitchE2ETest` 5 sitios, `PantryLocalizationE2ETest:35`) pasaban a fallar por la «ambigüedad de 2 nodos» y quedaron escudados con `assertTextEquals` sobre la tag.
- El `assertEquals(DESTROYED)` tras el back de salida en `BottomBarE2ETest` se convirtió en espera acotada (5 s), porque la animación de salida retrasa el destroy (antes: carrera `expected:<DESTROYED> but was:<STARTED>`).

## 6. Seguridad

Sin hallazgos: sin secretos ni PII en código, logs, capturas ni docs; sin permisos ni dependencias nuevas; sin llamadas de pago a la IA (la semilla y los E2E usan fakes/fixtures locales; `SettingsE2ETest` con build sin `AI_API_KEY` no toca la red). `feature_list.json` solo cambia `status`/`evidence`.

## 7. Qué queda para la validación independiente

`feature-validator` sobre `docs/specs/mover-opciones-a-bottom-bar.md` (RF-MOB1–7, verify 1–3, D1–D8): barra permanente en las 4 pantallas de pestañas con la activa resaltada y reselección → inicio, top bar solo Ajustes, dos FABs solo con Recetas, «Volver» retirado + back coherente, 4 pestañas (la 5.ª «Mi perfil» la añade `crear-mi-perfil`), strings con claves existentes y paridad es/en (319/319 + 5/5), y que el único fallo de la batería es el flake documentado (no en el diff, pasa en aislamiento). `docs/validations/mover-opciones-a-bottom-bar.md` se creará al aceptar.