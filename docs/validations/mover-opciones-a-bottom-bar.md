# Validación independiente — `mover-opciones-a-bottom-bar`

- **Veredicto: `accept`** (2026-11-08, sesión `feature-validator` independiente del implementador).
- Spec: `docs/specs/mover-opciones-a-bottom-bar.md` (RF-MOB1–7, S1–S5 con verify 1–3, D1–D8; «Hallazgos de implementación (2026-11-08)»).
- Rama `feature/mover-opciones-a-bottom-bar` (base `ed1ad60`), todo sin commitear. Sin cambios de código hechos por el validador.
- Entorno: AVD dedicado `Pixel_10_Pro_MoverOpciones`, `emulator-5556`, Windows 11, Gradle 8.14.3 / JDK corretto-17.0.20. No se tocó `emulator-5554` (otra sesión activa).

## 1. Contrato de validación aplicado

Decisiones D1–D8 de la spec: barra inferior **permanente en las 4 pantallas de pestañas** (Mis Recetas, Despensa, Lista, Pinche) con pestaña activa resaltada y reselección → scroll al inicio; **4 pestañas**; **dos FABs apilados solo en Recetas**; navegación `popUpTo(my_recipes)` + `launchSingleTop`; fichero `ui/nav/AppBottomBar.kt` con enum `AppTab`; iconos `RestaurantMenu`/`Inventory2`/`ShoppingCart`/`Chat`; etiquetas con claves existentes de `strings.xml`; 3 strings retiradas (`action_open_shopping_list`, `pantry_open_accessibility`, `pinche_open_accessibility`). RF-MOB1–7 y escenarios S1–S5 (verify 1–3) de la spec.

## 2. Checks re-ejecutados (tier barato) — todos reproducidos

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `.\init.ps1` (`ANDROID_SERIAL=emulator-5556`) | **exit 0**, «Entorno listo», app en ejecución en `Pixel_10_Pro_MoverOpciones` |
| Lint | `.\gradlew.bat lintDebug --rerun-tasks` | **BUILD SUCCESSFUL, 0 errores / 48 warnings** (línea base 49; cero warnings apuntan a ficheros del diff) |
| Unit | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL**: 42 suites / 305 tests / 0 fallos / 0 errores / 0 omitidos (XML reconciliados) |
| Ensamblado | `.\gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** |
| androidTest compila | `.\gradlew.bat compileDebugAndroidTestKotlin` | **exit 0** |
| Paridad es/en | recuento `<string>`/`<plurals>` por locale | **319/319 claves + 5/5 plurals** en ambos locales, **diff = 0** |

## 3. Checks solo inspeccionados (tier caro) — sin re-ejecución

Evidencia leída en `docs/evidence/mover-opciones-a-bottom-bar/` (`informe.md`, 2 capturas PNG 1280×2856, `test-bateria-focalizada.log`, `test-bateria-completa.log`):

- **E2E focalizada (7 clases): 89/89 verde.**
- **Batería completa `e2e.ps1`: 227 tests** (= inventario `@Test` de `androidTest`), **225 verdes + 1 omitido + 1 flake**.
- **Sin drift temporal**: fuentes 13:58 < focalizada 14:09 < batería 14:43 (las ejecuciones posteriores al último cambio de código).
- **Cobertura del `verification` de la spec presente**: `BottomBarE2ETest` (nuevo) + clases de localización/navegación afectadas.
- **Flake clasificado como ajeno a la feature**: `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures` (`ComposeTimeoutException` en `waitUntil` 5 s) — su fichero **no está en el diff**, pasa en aislamiento, se reprodujo en 2 baterías con contención de CPU en logcat y es su primera vez en batería (los builds históricos llevaban `AI_API_KEY` y lo omitían).
- **Capturas inspeccionadas**: barra visible en las 4 pantallas con pestaña activa correcta.
- **Arranque de la app tras la batería verificado** (el `e2e.ps1` desinstala al terminar; se reinstaló y comprobó).

## 4. Hallazgos por severidad

- **Critical: 0 · High: 0 · Medium: 0 · Low: 3 (notas documentales, no bloqueantes).**
  - **N1**: el XML del informe de la batería fue sobrescrito por el rerun aislado de `SettingsE2ETest` (N3); la evidencia durable es `test-bateria-completa.log`. → **Cerrado**: se añade el recuento por clase (inventario `@Test`, 227 en 24 clases, cuadra 1:1 con el progreso `227/227` del log) al `informe.md`.
  - **N2**: solo 1 de los 2 logs de batería se conservó en `docs/evidence/`; el segundo se ejecutó sin redirigir a fichero. → Irrelevante: la primera batería con el resultado completo está conservada.
  - **N3**: el run aislado de las 4 clases corregidas (23/23) no guardó log propio; queda corroborado por el detalle por clase de la batería completa.

## 5. Seguridad y privacidad

Checklist sin hallazgos relevantes: **sin secretos, tokens o PII** en el repositorio ni en la evidencia (ninguna API key; la evidencia E2E usa fixtures locales y cliente IA simulado); **sin permisos nuevos**; **sin dependencias nuevas**; **sin llamadas de pago a la IA** en la verificación; `SettingsE2ETest` sin `AI_API_KEY` no toca red. No bloqueante.

## 6. Arquitectura, mantenibilidad y docs durables

- **UI sin reglas de negocio**: `AppBottomBar.kt` es pura navegación/presentación (enum `AppTab` + composable); las reglas siguen en `domain/`.
- **Alcance respetado**: solo los ficheros listados en «Expected File Changes» de la spec; no se tocó `uxdocs/` ni `.opencode/`.
- **Docs durables actualizados**: `DESIGN.md` (filas Mis Recetas/Despensa/Pinche/Lista con barra inferior) y `docs/risks-and-open-questions.md` (`Q-mover-opciones-a-bottom-bar-1..3` resueltas D1–D3 + nota de coordinación con `crear-mi-perfil` y `mover-escaneo-a-la-despensa`).
- **Handoff**: `PROGRESS.md` y `docs/progress-history.md` al día; specs con checkboxes marcados; evidencia completa.

## 7. Resultado

**`accept`** → `feature_list.json` `status` = `accepted` (persistido por el orquestador). Sin `Implementation Repair Brief`. Follow-up: ninguno bloqueante; N1 cerrado con el recuento por clase añadido al `informe.md` (N2 y N3 quedan como notas declaradas).
