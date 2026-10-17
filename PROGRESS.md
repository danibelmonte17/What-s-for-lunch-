# Progreso — What's for Lunch?

## Estado verificado
Solo lo comprobado, no lo que se cree.
- `init`: pasa con `./init.sh` (exit 0: compila, instala y abre). Ojo: en dos ejecuciones de la sesión 11 devolvió exit 1 con `ERROR: la app no esta en ejecucion tras el arranque` por una carrera del `pidof` inmediato con la creación del proceso (el pid aparece ~0,2-1 s después de `monkey`); en la sesión 12 (validación de `crear-receta`) pasó a la primera y no depende de la funcionalidad. Los scripts del harness no se tocan
- `crear-receta` (emulador API 37 `Pixel_10a`, locale `en-US`): `lint` pasa (exit 0, 0 errores) · `unit` pasa (exit 0, 10 tests: `RecipeValidationTest` 8 + `MisRecetasStateTest` 2) · `e2e` **pasa** (exit 0, 22 tests, 0 fallos, 0 saltados). Es lo último medido (sesión 12)
- `arrancar-app-en-mis-recetas` (mismo arnés): `lint` exit 0 · `unit` exit 0 (2 tests) · `e2e` exit 0 (7/7 tests)
- Funcionalidades `validated`: 2 — `arrancar-app-en-mis-recetas` y `crear-receta` (2 de 23)
- Funcionalidades `passing`: ninguna
- Funcionalidades `in_progress`: ninguna
- `arrancar-app-en-mis-recetas`: B-1, B-2 y B-3 resueltos y verificados; los 11 RF pasan su verificación (e2e + `unit` + `uiautomator` + capturas es/en). Evidencia en `docs/evidence/arrancar-app-en-mis-recetas/`
- `crear-receta` **validada** (sesión 12, ciclo 2): los 26 RF pasan su verificación (22 E2E + 10 unit + comprobaciones manuales con `adb`/`uiautomator`), incluidos los manuales RF-19 (el selector del sistema abre, se elige una imagen de MediaStore y el formulario pasa a vista previa «Quitar foto») y RF-23 (`pm revoke CAMERA` → diálogo → «Don't allow» → aviso «Permiso de cámara denegado…» con el formulario utilizable); RF-18 reconfirmado con `force-stop` + relanzado (la tarjeta y su miniatura persisten en Room). B-1 y B-2 de la 1ª validación resueltos: el fix de B-1 inyecta el locale en `CrearRecetaE2ETest.launchForm` (`createConfigurationContext` + `CompositionLocalProvider(LocalContext …)`, patrón de `MisRecetasLocalizationE2ETest`) y las aserciones leen la cadena del recurso; B-2 añade `editAndRemoveStepRows` (RF-10/RF-11), `generatedPhotoCanBeAcceptedOrDiscarded` (RF-20) y `generationFailureShowsNoticeAndKeepsManualOptions` (RF-22). Solo cambiaron tests; el producto quedó intacto respecto a la 1ª validación. Evidencia: `docs/evidence/crear-receta/informe-validacion.md` + 2 capturas es (API 37). Fidelidad: jerarquía y tamaños conformes; 2 divergencias de rol de token (tarjetas/inputs/chips en `surfaceContainerHighest` y chip seleccionado en `secondaryContainer`) consideradas no bloqueantes (prototipo «orientativo»; colores son tokens de la paleta)
- Roto o pendiente de arreglar: nada conocido

## Siguiente paso
Elegir la siguiente funcionalidad `ready` o `not_started` cuyas dependencias estén `validated` (según `feature_list.json`) y seguir el flujo (spec → auditoría → implementación → validación). `crear-receta` ya está cerrada en `validated`. Recordar el escenario de checkout fresco: `init` regenera `local.properties` sin escapar (`lint.xml` ya lo excluye de `PropertyEscape`). Ojo con `./e2e.sh`: desinstala la app al terminar (`connectedAndroidTest`); reinstalar con `./gradlew installDebug` antes de cualquier comprobación manual con `adb`.

## Riesgos y preguntas abiertas
El detalle está en `docs/risks-and-open-questions.md`. Afectan a la implementación: RK02 (el modelo de detección MediaPipe está por validar), S05 (la API key vive en el dispositivo: la implementación provisional usa `UnconfiguredAiKeyProvider`, que siempre devuelve `null`, así que «Generar con IA» avisa sin bloquear hasta que llegue `configurar-acceso-a-ia`), S07 (validaciones del formulario de receta, ya revisado con la foto obligatoria para Publicar).
- `crear-receta` — riesgos cerrados: el build con Room 2.8.5 y KSP 2.3.12 sobre AGP 8.13.2 compila y empaqueta (`init`, `lint`, `unit` y `e2e` han ejecutado `gradlew` sin problemas de plugin). La dependencia del idioma del dispositivo en la suite E2E (B-1) se corrigió en la sesión 12 inyectando el locale en el propio test y quedó **confirmada en la validación del ciclo 2** (22/22 en `en-US`).
- Entorno de validación: el AVD reutilizado es `Pixel_10a` (API 37, 420 dpi, locale `en-US`). Para las capturas en español se usó el locale por aplicación (`adb shell cmd locale set-app-locales com.whatsforlunch.app --locales es-ES`) y se revirtió al terminar; el locale del sistema no se tocó.
- S06 queda confirmado con el scaffold real (Q-arrancar-app-en-mis-recetas-1).
- Nota de entorno: el build fija `org.gradle.java.home` a un JDK 17 (`gradle.properties`, con la ruta escapada para `lint`: `C\:/...`) porque AGP 8.13.2 no arranca con el JBR de Android Studio (JDK 25), que es el que seleccionan los scripts del harness cuando `JAVA_HOME` no está definido.
- Nota de lint: la regla `PropertyEscape` ignora `local.properties` (generado por `init.ps1`, harness intocable, sin escapar el `:` de la unidad) mediante `lint.xml` en la raíz, referenciado desde `app/build.gradle.kts`.

## Historial
Las entradas de sesión viven en `docs/progress-history.md` (solo se añaden; no se leen enteras salvo la última si hace falta contexto).
