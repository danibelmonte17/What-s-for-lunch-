# Validación independiente — `cambiar-idioma-de-la-interfaz`

**Fecha:** 2026-11-07
**Validador:** agente `feature-validator` (sesión independiente, sesión 104)
**Veredicto: `accept`**
**Spec:** `docs/specs/cambiar-idioma-de-la-interfaz.md` (RF-IDI1–5, S1–S5 donde verify 1 = S1, D1–D6)
**Árbol:** rama `feature/cambiar-idioma-de-la-interfaz`, HEAD `413e0c5` (`63dbb10` spec · `b1f536c` feat · `413e0c5` docs sobre `develop`=`c6aa60e`), working tree limpio

---

## 1. Checks re-ejecutados vs. solo inspeccionados

**Re-ejecutados por el validador (tier barato, árbol actual):**

| Check | Comando | Resultado |
|---|---|---|
| Lint | `.\gradlew.bat lintDebug --rerun-tasks` | BUILD SUCCESSFUL; `lint-results-debug.xml` reconciliado por severidad: **0 errores / 49 warnings** = línea base exacta, sin warnings nuevos |
| Unit | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL; XML `app/build/test-results/testDebugUnitTest/`: **42 suites / 299 tests / 0 fallos / 0 errores / 0 omitidos** = cifra registrada |
| Ensamblado | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL sin errores |
| Paridad es/en | recuento de claves `<string>`+`<plurals>` | **330/330** (325 strings + 5 plurals), claves idénticas entre `values/` y `values-en/` (0 diferencias) |
| Capturas PNG | lectura de bytes + inspección visual | `ajustes-idioma-es.png` (247.511 B) e `interfaz-idioma-en.png` (250.207 B): cabecera `89 50 4E 47`, **1280×2856**, coherentes con S4/verify 1 y **sin secretos ni PII** |
| Auditoría del diff | `git diff c6aa60e..HEAD`, `git show` | Sin secretos; sin `recreate()`/AppCompat/`localeConfig`; sin `values-xx` nuevas; `AndroidManifest.xml`, `assets/prompts/` y `data/local/` intactos; `feature_list.json` solo toca `status`/`evidence`; sin mojibake en `app/src` |

**Solo inspeccionados (tier caro, emulador apagado):**

- **E2E focalizada `LanguageSwitchE2ETest` 8/8** (2026-11-06, `emulator-5554`): sin XML/HTML conservado de esa pasada (sobrescritos por la batería); la evidencia del informe §2 cumple el estándar (comando exacto, fecha, entorno, recuento) y **el inventario del código cuadra**: 8 `@Test` con desglose exacto S1 (= verify 1, con back stack), S2, S3 (SharedPreferences reales + controller nuevo), S4 (2 tests: `en`→English, `Locale.FRANCE`→Español), S5 (2 de prompts `"en"`/`"es"` vía `CapturingRecipeSuggester` + `voiceLocaleFollowsAppLanguage` con `CookModeRoute` real, `CapturingStepSpeaker` y `pm grant` de `RECORD_AUDIO` sin dependencias nuevas).
- **Batería `e2e.ps1` 221 tests** (2026-11-06): 218 verdes + 1 flaky + 2 omitidos = 221 ✓ (el «Finished 223» de consola = doble cuenta de omitidos, convención histórica). El flaky `ScanPantryE2ETest#successivePhotosAccumulateWithoutOverwritingEditedChips` («Activity never becomes requested state "[DESTROYED]"») pertenece a la familia histórica de teardown de `ActivityScenario` (misma firma en `EditRecipeE2ETest`, sesión 60; «incidencias abiertas» de `PROGRESS.md`), es ajeno al idioma y su **rerun focalizado 1/1 está conservado como XML** (`TEST-Pixel_10_Pro(AVD) - 17-_app-.xml`, 2026-11-06T22:55:26). Los 2 omitidos (`SettingsE2ETest`/`Assume` sin key de build) son preexistentes. Delta coherente: 213 (sesión 102d) + 8 = 221.
- **Comprobaciones manuales §4** (adb/uiautomator): S4 con `pm clear` («Español» marcado, sistema es-ES), verify 1 sin reinicio (UI en inglés, mismo proceso/Activity), S3 con `am force-stop` (reapertura en inglés con «English» marcada). **Cubren el `verification`** de `feature_list.json`.
- **`init.ps1`**: no re-ejecutado (falso fallo `monkey`/stderr documentado, no tratado como finding).

## 2. Hallazgos

### F1 (Low, documental) — desglose «+5 tests en SettingsViewModelTest» no reconciliaba
- **Evidencia**: `informe.md` §1 y la entrada de Sesión 103 decían «+5»; real verificado por el validador (y reconfirmado al persistir): `SettingsViewModelTest` pasó de **8** a **10** `@Test` (+2: `languageStateReflectsActiveSelection`, `selectingLanguageAppliesAndPersistsIt`); delta total 291→299 = 3+3+2.
- **Corregido al persistir (2026-11-07)**: desglose ajustado a «+2 (8→10)» en `docs/evidence/cambiar-idioma-de-la-interfaz/informe.md` y en la Sesión 103 de `docs/progress-history.md`. Verificación: 291+3+3+2 = 299 cuadra con el XML re-ejecutado.

### F2 (Low, trazabilidad) — `app/build.gradle.kts` no estaba en «Expected File Changes» de la spec
- **Evidencia**: el diff modifica `app/build.gradle.kts` (`bundle { language { enableSplit = false } }`) y el fichero no aparecía en la spec.
- **Corregido al persistir (2026-11-07)**: añadido a «Expected File Changes» y a «Hallazgos de implementación» de la spec con su justificación (warning `AppBundleLocaleChanges` + recursos disponibles al cambiar de locale bajo AAB).

**Notas (sin cambio requerido):** N1 formato de cantidades (RF-IDI4) verificado por composición sin aserción dedicada, aceptable según la spec · N2 la focalizada 8/8 y la batería no conservan XML/HTML propios (sobrescritos); conservarlos en `docs/evidence/` reforzaría evidencia futura · N3 `successivePhotosAccumulateWithoutOverwritingEditedChips` es primera ocurrencia como flaky (la familia teardown sí es histórica) · N4 `status: not_started` en la cabecera de la spec refleja el momento de planificación (convención previa).

## 3. Security assessment (checklist aplicado, alcance acotado a la feature)

**Sin hallazgos.** Sin secretos/PII en diff ni capturas (la API key de DevExpert Inference no aparece; las capturas solo muestran placeholders); selección = enum cerrado `AppLanguage` y el store ignora valores desconocidos (`firstOrNull { it.name == stored }`); SharedPreferences guarda solo `SPANISH`/`ENGLISH` sin cifrar (no sensible, razonado); sin servicios externos ni prompts tocados (`assets/` intacto); **cero dependencias nuevas** y **cero permisos nuevos** (`AndroidManifest.xml` sin cambios; `RECORD_AUDIO` solo con `pm grant` dentro del test); UI sin reglas de negocio (`AppLanguage.fromSystem` es pura en `domain/model/`).

## 4. Documentación / harness

`DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (Q-…-1..2 resueltas por el usuario), spec con hallazgos, `PROGRESS.md` y `docs/progress-history.md` (Sesión 103) consistentes con el comportamiento implementado. `CONTEXT.md`/`AGENTS.md`/`docs/build-brief.md` intactos, como prevé la spec. Commits Conventional Commits en inglés; docs de producto en español. `SettingsE2ETest` sin cambios; `feature_list.json` solo `status`/`evidence`.

## 5. Conclusión

La implementación cumple la spec (RF-IDI1–5, S1–S5, D1–D6, non-goals), el tier barato se reproduce exactamente re-ejecutado, la evidencia del tier caro es exacta y consistente, la seguridad no arroja hallazgos y la documentación durable está al día. **Veredicto: `accept`** → la orquestadora persiste `cambiar-idioma-de-la-interfaz` como **`accepted`** (2026-11-07).
