# Informe de autoverificación — `redisenar-vista-de-la-despensa`

- **Fecha**:2026-11-11 (sesión de implementación, rol `feature-implementer`).
- **Spec**: `docs/specs/redisenar-vista-de-la-despensa.md` (T1-T10 completados).
- **Dispositivo de validación**: **teléfono físico V2124 (Android13, API33)** — por instrucción explícita del usuario en esta sesión («utiliza el dispositivo físico para hacer la validación, siempre»). Resolución de capturas1080×2408.
- **Estado**: `feature_list.json` → `passing` (pendiente de validación independiente con `feature-validator`).

##1. Gates de calidad

| Check | Comando | Resultado |
|---|---|---|
| Arranque | `./init.ps1` (con `ANDROID_SERIAL=<físico>`) | **exit0 «Entorno listo»**; `installDebug` solo en `V2124 - 13`; app en ejecución |
| Estático | `./gradlew :app:lintDebug` | **0 errores /10 warnings** — todos los conocidos de versiones (`GradleDependency`×5, `NewerVersionAvailable`×3, `OldTargetApi`×1, `AndroidGradlePluginVersion`×1); **0 warnings nuevos** |
| Unit | `./gradlew :app:testDebugUnitTest` | **49 suites /384 tests /0 fallos /0 omitidos** (base366 +12: `FreshnessRulesTest`6, `PantrySearchFilterTest`3, `InMemoryPantryRepositoryTest`+1 y tests existentes) |
| Build | `./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin` | **BUILD SUCCESSFUL** |
| Paridad es/en | recuento de claves | **368/368 strings +10/10 plurals, diff=0** |

Durante la autoverificación se corrigieron2 warnings nuevos `PluralsCandidate` (`pantry_tip_many` pasó de `<string>` a `<plurals>`, con lo que además mejora la gramática con «1 alimento listo»).

##2. E2E focalizada (dispositivo físico)

`adb shell am instrument -w -e class PantryManagementE2ETest, PantryLocalizationE2ETest, RecipeMigrationTest …`

- **OK (22 tests) en41,9 s** — log: `e2e-focalizada-fisico.txt`.
- Cobertura: S1-S8 de la spec (verify1-6 y8) + los tests nuevos S10-S14 (resumen/consejo/contadores, stepper inline con límites y persistencia, búsqueda en vivo + «sin resultados», alta/edición de caducidad con persistencia, FABs intactos), localización es/en con ≥48 dp y migración v7→v8 (`migrationFromV7AddsExpiryDateAndKeepsPantryData`).

##3. Batería completa (dispositivo físico)

`adb shell am instrument -w …` (mismo patrón que `e2e.ps1`, log propio) — log: `e2e-bateria-fisico.txt`.

- **258 tests →256 en verde +2 fallos, ambos clasificados y sin relación con la feature**:

| Test | Fallo | Clasificación y prueba |
|---|---|---|
| `BottomBarE2ETest#activeTabIsHighlightedBackIsRemovedAndTheBackStackStaysCoherent` | `expected:<DESTROYED> but was:<CREATED>` | **Ambiental del dispositivo** (decisión del usuario2026-11-11: documentar, no tocar el test). El back en la raíz de la app en este teléfono **no invoca `finish()`**: reproducido manualmente sin tests (`dumpsys activity activities`: `state=STOPPED finished=false` minutos después, con Launcher en primer plano; el comportamiento observable de RF-MOB4 —«sale de la app»— sí se cumple). **Control de regresión**: el mismo test pasa en emulador con **HEAD (a8d6fb1)** y con **la feature** — ver §4. |
| `SettingsE2ETest#deletingTheKeyAsksToConfigureItAgainForAiFeatures` | `ComposeTimeoutException`5 s | **Flake documentado** (sesiones115/116/119/124/125); **OK en aislamiento esta sesión** (5/5 con `SettingsE2ETest` completo). |

Log de la primera batería (con6 fallos, antes de las correcciones de §5): `e2e-bateria-fisico-intento1.txt`.

##4. Experimento de control del fallo de BottomBar (regresión vs dispositivo)

| Código | Entorno | Resultado |
|---|---|---|
| HEAD `a8d6fb1` (worktree `wfl-head-check`) | emulador `Pixel_9_Pro_Validator` | **OK (6,9 s)** |
| Feature (este trabajo) | mismo emulador | **OK (7,5 s)** |
| Feature | físico V2124 | **FAIL ×3** (batería +2 aislados) |
| Feature | físico, back manual sin tests | app sale a Launcher pero la actividad queda `finishing=false` |

Conclusión: **no es regresión de la feature** (el diff no toca navegación/back; no hay `BackHandler` en raíz ni flags de manifiesto); es comportamiento del dispositivo. Decisión del usuario: **documentar como ambiental** y dejar el test intacto.

##5. Correcciones de autoverificación (hallazgos propios, dentro del alcance)

1. **`PantryUiState.visibleItems` en tests que montan el estado a mano** (`PantryLocalizationE2ETest`, `ScanPantryE2ETest`): sin él se renderizaba «No se encontraron ingredientes» en vez de las tarjetas (en producción lo deriva el ViewModel). Corregido en ambos → `ScanPantryE2ETest` **25/25** en aislamiento.
2. **Ambigüedad de `contentDescription`**: «Disminuir/Aumentar cantidad» existía en el diálogo y en el stepper inline → los botones inline pasan a descripción por alimento («Disminuir cantidad de X», claves `pantry_item_qty_*` es/en).
3. **testTags en nodos hoja** (`FRESH_BADGE`, píldora `FreshnessPill`, `TIP_MESSAGE`): movidos al `Text` para poder asertar contenido con `assertTextEquals`.
4. **`pantry_tip_many` → `<plurals>`** (lint `PluralsCandidate`).

##6. Verificación manual (físico, `adb` + `uiautomator dump`)

Todas las comprobaciones en el dispositivo físico, con la app en español salvo la captura EN:

- Estado vacío: cabecera con badge «0 frescos», «Tu despensa está vacía», CTAs **en orden D16** («Añadir Alimento» primario primero, «Escanear la despensa» tonal debajo) y los dos FABs apilados — *capture01*.
- Resumen/consejo/píldoras: «2 Alimentos», «Capacidad estimada al5%», «1 por caducar», consejo «Tienes Aguacates listo para consumir pronto.», píldoras «Consumir pronto»/«En buen estado», «Caduca hoy»/«Larga duración» — *capture02*.
- Stepper inline: «+» → «Cantidad:1» guardado al momento; menú ⋮ con «Editar»/«Eliminar» — *capture03*.
- Búsqueda: «zzz» → «No se encontraron ingredientes» con «+ Registrar nuevo alimento»; limpiar vuelve la lista — *capture04*.
- Inglés: «My Pantry», «2 fresh foods», «Cooking tip: You have Aguacates ready to use soon.», «Use soon», «Expires today», «Long shelf life», pestañas «Recipe book/Pantry/…» — *capture05*.
- **Persistencia**: `force-stop` + relanzar → fecha («Caduca hoy»), cantidad («Cantidad:1») y contadores intactos.
- Idioma devuelto a **español** al terminar; pref `language_prefs/app_language=SPANISH` verificado.

Capturas: `01-despensa-vacia-es.png`, `02-resumen-consejo-es.png`, `03-stepper-menu-es.png`, `04-sin-resultados-es.png`, `05-despensa-en.png` — todas PNG con firma `89 50 4E 47` y **revisadas visualmente una a una**.

##7. Incidencias de entorno (para el validador)

1. **Interferencia de sesiones paralelas**: hay otros worktrees activos (`C:/Proyectos/whats-for-lunch` [develop], `…/develop` [feature/control-conversacion-pinche], `…/develop-2` [feature/add_photo_without_camera-2]). Sus `installDebug`/`init.ps1` instalan en **todos** los dispositivos conectados: a las **20:15:40** y **20:33:58** (hora del sistema) sobrescribieron el APK del físico con código anterior a Room v8 → crashes `A migration from8 to7 was required` y proceso muerto. Restaurado con reinstalación del APK v8 (20:25:49) y `init.ps1` final. **Riesgo para el validador**: re-verificar `dumpsys package … lastUpdateTime`/arranque antes de dar por buena una corrida, o instalar con `ANDROID_SERIAL` scoping justo antes de ejecutar.
2. **Conexión adb-TLS intermitente** (respuestas `cat` vacías) y **bug de `uiautomator dump` en el Vivo** (warning `EACCES /sys/board_info/user_cpu_freq`) → todos los accesos usaron reintentos.
3. **Primera tanda de capturas corrupta**: PowerShell5.1 reescribe `adb exec-out … > archivo` como UTF-16 (BOM `FF FE`). Regeneradas con `adb shell screencap` + `adb pull` (binario seguro).
4. `BottomBarE2ETest` y el flake de `SettingsE2ETest` son los únicos rojos de batería (§3); el resto de la suite completa pasa en el físico.
5. Worktree de control `wfl-head-check` (HEAD `a8d6fb1`) creado y **eliminado** al cerrar la sesión.

##8. Archivos de evidencia

```
docs/evidence/redisenar-vista-de-la-despensa/
├── informe.md                       (este informe)
├── lint-final.txt                   (salida de lintDebug final)
├── e2e-focalizada-fisico.txt        (22/22 OK)
├── e2e-bateria-fisico.txt           (258 tests:256 OK +2 clasificados)
├── e2e-bateria-fisico-intento1.txt  (primera batería,6 fallos → corregidos/clasificados)
├── 01-despensa-vacia-es.png         (1080×2408)
├── 02-resumen-consejo-es.png        (1080×2408)
├── 03-stepper-menu-es.png           (1080×2408)
├── 04-sin-resultados-es.png         (1080×2408)
└── 05-despensa-en.png               (1080×2408)
```
