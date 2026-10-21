# Feature Implementation Spec: Escanear la Despensa con la cámara

> **Revisión 2026-10-21 (delta sobre las dudas D2/D3/D5, respuestas del usuario)**: (1) un Alimento escaneado cuyo nombre normalizado coincide con uno ya presente en la Despensa **se pregunta en pantalla antes de guardar** (diálogo por coincidencia con «Mantener cantidad»/«Actualizar cantidad»): ya no hay fusión automática silenciosa con aviso resumido (Q-escanear-despensa-con-camara-1); la regla de dominio «un Alimento por nombre normalizado» se conserva; (2) salir del Escáner con chips sin guardar **pide confirmación** (supuesto D3 confirmado; S10 se mantiene; Q-escanear-despensa-con-camara-2); (3) los chips del Escáner **editan la cantidad** con un stepper entero 0..999 (0 = sin cantidad), igual que el formulario de la Despensa (Q-escanear-despensa-con-camara-3), y se guarda con la cantidad elegida.

## Source Feature

- `id`: escanear-despensa-con-camara
- `area`: funcional
- `depends_on`: gestionar-alimentos-de-la-despensa (`accepted`)
- `status`: `not_started` en el momento de planificar
- `source`: `feature_list.json`

## Goal

Sustituir el destino provisional `scan_pantry` por el **Escáner** real que llena la **Despensa** desde la cámara: el usuario ve el visor, captura una imagen, la detección de Alimentos ocurre **en el dispositivo** (MediaPipe, `P23`, sin red) y la app propone **chips editables** (nombre, cantidad con stepper entero 0..999 y confianza) que se corrigen, quitan o amplían antes de **confirmar y guardar** en la Despensa (Room), resolviendo **en pantalla** —antes de guardar— las coincidencias por nombre normalizado con Alimentos ya presentes (Q-escanear-despensa-con-camara-1). Es el tramo `Camara → MediaPipe → Chips → Despensa` de `docs/technical-discovery.md`.

Lo duradero son solo los **Alimentos** de la Despensa (Room vía `PantryRepository`, ya existente). El **Escaneo** es temporal (detecciones con confianza, en memoria, hasta confirmar los chips, `docs/domain-model.md`) y **la foto se descarta tras la inferencia** (RD06, RGPD): no se escribe a disco ni queda rastro de imagen.

## Non-Goals

- Carrusel de Recetas compatibles ni Sugerencias (`mostrar-recetas-compatibles`, `sugerir-recetas-con-ia`).
- Faltantes hacia la Lista de la Compra desde el Escaneo (`anadir-faltantes-a-la-lista`).
- Calibración del umbral de confianza, cambio de cámara o galería desde el Escáner (el prototipo los dibuja; `P16`).
- Unidades, decimales o fracciones en la cantidad de los chips: es siempre un entero 0..999 sin unidad, igual que en Despensa (0 = sin cantidad), sin entrada de texto.
- Sumar cantidades al resolver una coincidencia con la Despensa: solo se ofrece «mantener» la cantidad existente o «actualizar» con la del chip.
- Cambiar `PantryRepository`, el esquema Room (v2, `MIGRATION_1_2`) ni las pantallas Despensa/Mis Recetas.
- Entrenar o afinar modelos: elegir el modelo concreto es el spike previo (RK02), no esta feature.

## Job Story

Cuando quiero tener la Despensa al día sin teclear cada alimento,
quiero escanear con la cámara lo que tengo delante y corregir lo detectado antes de guardarlo,
para que la app sepa lo que hay en casa en segundos.

## Users And Permissions

- **Usuario** (único rol, propietario del dispositivo, `P31`): usa el Escáner. Requiere permiso `CAMERA` en tiempo de ejecución (ya declarado en el manifest); sin él el flujo degrada a entrada manual sin bloquear (RD05; precedente RF-23 de `crear-receta`).

## Acceptance Scenarios

### Scenario 1: Detección con chips y confianza (verify 1 de `feature_list.json`)

Given el Escáner abierto con permiso concedido y una imagen de prueba inyectada por el puerto de captura
When el usuario pulsa el obturador y la inferencia termina
Then se muestran los chips de los Alimentos detectados con su nombre y su confianza (p. ej. «Tomate fresco · 96%»), y las etiquetas de detección sobre la imagen capturada

### Scenario 2: Confirmar y guardar en la Despensa (verify 2 de `feature_list.json`)

Given los chips de detección visibles con las cantidades elegidas por el usuario (p. ej. «Tomate fresco» con 3 y «Leche» en 0 = sin cantidad)
When el usuario confirma («Guardar en la Despensa») y no quedan coincidencias pendientes
Then los Alimentos aparecen en la Despensa con la cantidad elegida en cada chip (persistidos en Room; 0 = sin cantidad → Alimento sin cantidad, Q-escanear-despensa-con-camara-3) y el Escaneo queda descartado

### Scenario 3: Quitar, renombrar, ajustar cantidad y añadir chips (RD05)

Given los chips de detección visibles
When el usuario quita un chip con «×», renombra otro, ajusta la cantidad de un tercero con el stepper «− valor +» y añade uno manualmente («Añadir alimento»)
Then la bandeja refleja los cambios —la cantidad es un entero 0..999 (0 = sin cantidad), «−» deshabilitado en 0 y «+» en 999, sin campo de texto— y solo eso se guarda al confirmar

### Scenario 4: Duplicados dentro del Escaneo, un solo chip (regla de dominio)

Given dos detecciones con el mismo nombre normalizado («TOMATE FRESCO» y «  Tomate  fresco »)
When se construye la bandeja de chips
Then queda un solo chip por nombre normalizado (nombre más legible y confianza máxima) con un único stepper de cantidad; se conserva la regla de dominio «un Alimento por nombre normalizado» (`docs/domain-model.md`). Las coincidencias con Alimentos **ya presentes en la Despensa** no se fusionan a escondidas: se preguntan en pantalla (S11 y S12, Q-escanear-despensa-con-camara-1)

### Scenario 5: Permiso de cámara denegado (RD05)

Given el permiso `CAMERA` denegado o revocado
When el usuario abre el Escáner
Then se muestra un aviso (mismo criterio que RF-23) y el flujo continúa en modo manual: chips «Añadir alimento» sin visor, sin bloquear al usuario

### Scenario 6: Escaneo sin detecciones (caso límite de `docs/domain-model.md`)

Given una captura en la que el detector no devuelve ningún Alimento
When termina la inferencia
Then se muestra un mensaje claro y la alta manual de Alimentos por chips

### Scenario 7: Fallo de la inferencia (RD05)

Given el detector en error (modelo no cargado, imagen ilegible…)
When la inferencia falla
Then se muestra un aviso y el flujo degrada a entrada manual, sin bloquear

### Scenario 8: La captura no persiste (RD06)

Given una captura procesada y confirmada
When se revisa el almacenamiento tras la inferencia y el guardado
Then no existe ninguna imagen de Escaneo en disco ni tabla de Escaneos; solo los Alimentos en Room

### Scenario 9: Textos es/en y objetivos táctiles

Given la app en español o en inglés
When se abre el Escáner (visor, chips, avisos)
Then todos los textos visibles cambian con el locale y las acciones tienen área táctil ≥ 48 dp

### Scenario 10: Salir con chips sin guardar (Q-escanear-despensa-con-camara-2, confirmado)

Given chips de Alimentos presentes en la bandeja y sin guardar
When el usuario pulsa «atrás» o navega fuera del Escáner
Then se muestra un diálogo de confirmación (mismo patrón que `eliminar-receta`: «Cancelar» conserva el Escáner y sus chips; confirmar descarta el Escaneo temporal y sale)

### Scenario 11: Coincidencia con la Despensa: preguntar en pantalla (Q-escanear-despensa-con-camara-1)

Given un chip «Tomate fresco» con cantidad 3 y un Alimento «  TOMATE  fresco » ya en la Despensa con cantidad 1
When se detecta la coincidencia (al generarse, renombrarse o añadirse el chip) y el usuario la resuelve
Then el chip queda marcado («Ya está en la Despensa») y el Escáner pregunta con un diálogo por coincidencia (patrón `eliminar-receta`, con el nombre y la cantidad existente): «Mantener cantidad» deja el Alimento existente intacto (cantidad 1; sigue sin cantidad si no la tiene) y el chip no crea fila; «Actualizar cantidad» deja el Alimento existente con la cantidad del chip (3; si el chip está en 0, queda sin cantidad). En ambos casos queda un solo Alimento por nombre normalizado, sin filas nuevas

### Scenario 12: Varias coincidencias y confirmación con pendientes

Given varios chips que coinciden con Alimentos de la Despensa, alguno sin resolver
When el usuario pulsa «Guardar en la Despensa»
Then no se guarda nada todavía: el Escáner pregunta por cada coincidencia pendiente una a una (orden de la bandeja) y solo guarda cuando todas están resueltas (junto al resto de chips); descartar un diálogo (atrás o toque fuera) aborta el guardado y deja la bandeja intacta —chips y decisiones ya tomadas—, sin filas parciales en la Despensa

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `escanear-despensa-con-camara` (steps, verification, notes con RK02/RD06); dependencia `gestionar-alimentos-de-la-despensa` en `accepted`.
- `PROGRESS.md` — estado del harness, entorno (AVD `Pixel_10_Pro` API 37), convención de scripts y capturas en español.
- `docs/risks-and-open-questions.md` — **RK02** (el modelo MediaPipe puede no alcanzar > 88 % de confianza; mitigación = spike previo + degradación RD05) y su **tarea de spike** «antes de `escanear-despensa-con-camara`» (valida también RK05); alternativa YOLOv8 ONNX (`P23`) solo si el spike falla.
- `docs/domain-model.md` — entidades Alimento (nombre, cantidad entera 0..999; 0 = sin cantidad) y Escaneo (fecha, Alimentos detectados con confianza; temporal: «sus resultados viven solo hasta confirmar los chips»); fusión de duplicados por nombre normalizado (a plasmar con la decisión 2026-10-21: dentro del Escaneo un solo chip y frente a la Despensa preguntar en pantalla, siempre un Alimento por nombre normalizado); casos límite «Escaneo sin detecciones», «Fallo de la IA» (RD05), «Ingrediente detectado duplicado»; RD05 y RD06.
- `docs/technical-discovery.md` — MediaPipe on-device (`P23`, modelo concreto «Por validar», RK02), flujo `Camara → MediaPipe → Chips → Despensa`, capas `ui`/`domain`/`data`, datos temporales («resultados de Escaneo (confianzas)», «fotos… no se persisten las de Escaneo — RD06»), comandos Gradle, E2E instrumentadas Compose con fixtures locales y IA falsa.
- `docs/build-brief.md`, `CONTEXT.md` — líneas rojas (RGPD: fotos de escaneo sin almacenar), términos «Escaneo» vs «Escáner» (no sinónimos), «Despensa».
- `DESIGN.md` — pantalla «Escáner de la Despensa» (visor, HUD con etiquetas y confianza, chips editables; carrusel fuera de alcance), chips confirmados en verde salvia, estados vacío/carga/error, accesibilidad.
- `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html`, `screen.png` — inspiración (`P16`): etiquetas píldora «nombre · 96%» sobre el visor, bandeja «Detectados» con chips y «×», «Añadir extra», obturador; los tokens de `uxdocs/DESIGN.md` mandan (`P17`).
- `app/src/main/AndroidManifest.xml` — permiso `CAMERA` y `uses-feature camera required=false` ya declarados (desde `crear-receta`); `FileProvider` ya existe para fotos de Receta.
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — **sin CameraX ni MediaPipe** hoy; BOM Compose, Koin, Room 2.8.5, minSdk 30, JUnit4 + `kotlinx-coroutines-test`.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoAcquirer.kt`, `SystemPhotoAcquirer.kt` — patrón de puerto con `PhotoResult` (incluye `CameraPermissionDenied`), solicitud de `CAMERA` en runtime con `ActivityResultRegistry`, y fake que inyecta bytes de prueba en E2E.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt`, `domain/model/PantryItem.kt`, `domain/pantry/PantryItemValidation.kt`, `domain/text/NameNormalization.kt` — contrato (`savePantryItem` con `PantrySaveResult.Saved | DuplicateName`; cubre alta y edición, así que «Actualizar cantidad» regraba el Alimento existente con su mismo `id`), cantidad opcional, rango 0..999 (reutilizado en los chips del Escáner) y normalización de nombres ya en uso.
- `app/src/main/java/com/whatsforlunch/app/data/repository/RoomPantryRepository.kt`, `InMemoryPantryRepository.kt`, `data/local/PantryItemEntity.kt`, `PantryItemDao.kt`, `RecipeDatabase.kt` — persistencia Room v2 de la Despensa (índice único por `normalizedName`); doble en memoria para unit.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — módulos Koin (Room, repositorios, `viewModel {}`).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt`, `ui/nav/ProvisionalScreen.kt` — ruta `scan_pantry` hoy en `ProvisionalScreen` con `ProvisionalTestTags.SCANNER_SCREEN`; entradas al Escáner desde Mis Recetas (barra/estado vacío).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — convención de claves por pantalla (`pantry_*`, `action_*`), i18n es/en obligatorio.
- `app/src/androidTest/java/com/whatsforlunch/app/CreateRecipeE2ETest.kt` — patrón E2E: pantalla montada sobre `InMemory*Repository` con fakes inyectados (`FakePhotoAcquirer`), locale inyectado y `testTag`s.
- `app/src/androidTest/java/com/whatsforlunch/app/PantryManagementE2ETest.kt`, `MatchBadgeE2ETest.kt`, `MyRecipesLocalizationE2ETest.kt`, `E2eDatabase.kt` — E2E sobre la app/DB real con `E2eDatabase.clear()`; `MyRecipesLocalizationE2ETest` apunta hoy a `ProvisionalTestTags.SCANNER_SCREEN` (hay que actualizarlo).
- `docs/specs/gestionar-alimentos-de-la-despensa.md`, `docs/specs/crear-receta.md` — estilo de spec aceptado; RF-23 (permiso denegado) y patrón de fakes de `crear-receta`.
- `init.sh`/`init.ps1`, `lint.*`, `unit.*`, `e2e.*` — scripts del harness (no se tocan); `e2e.ps1` lanza `gradlew.bat connectedDebugAndroidTest` completo.

### Existing Patterns To Follow

- MVVM + StateFlow: `*Route` (cablea ViewModel y adaptadores del sistema) → `*Screen` (sin estado) → `*ViewModel` con `uiState`; reglas de negocio en `domain/` sin Compose; Koin `viewModel {}`.
- Puertos en `data/` con implementación real y fake para tests (`PhotoAcquirer`/`SystemPhotoAcquirer`/`FakePhotoAcquirer`): la UI depende de la abstracción; el permiso `CAMERA` se pide en runtime y se degrada con aviso (RF-23).
- Persistencia de la Despensa reutilizable tal cual: `PantryRepository.savePantryItem` ya resuelve el resultado por nombre normalizado.
- testTags por pantalla (`PantryTestTags`, `MyRecipesTestTags`…); diálogos con tags propios; `contentDescription` para accesibilidad.
- Cadenas con prefijo de pantalla, duplicadas en `values/` y `values-en/`; identificadores de código en inglés (`P19`, `P25`).
- E2E Compose con locale inyectado y fakes; regresión completa obligatoria.

### Current Gaps

- No existe nada del Escáner salvo la ruta provisional: ni CameraX, ni MediaPipe, ni modelo, ni ViewModel/pantalla.
- **El modelo MediaPipe concreto no está elegido ni validado (RK02; spike pendiente)**: no hay dependencia de `tasks-vision`, ni `.tflite` en `assets/`, ni vocabulario de etiquetas confirmado.
- No hay pruebas de cámara ni de visión en el repo; el emulador no garantiza una escena real: la E2E debe inyectar imagen por puerto (y el detector real se valida en el spike y con una fixture local).
- `MyRecipesLocalizationE2ETest` depende del testTag provisional del Escáner.

## Technical Approach

0. **Spike previo (gate bloqueante, RK02/RK05)**: antes de tocar la feature, validar en el emulador (API 30+) un modelo concreto de MediaPipe (p. ej. `tasks-vision` Object Detector con `.tflite` en `assets/`) contra imágenes de alimentos: objetivo **> 88 % de confianza** en las detecciones correctas. Registrar el resultado y el modelo elegido en `docs/technical-discovery.md` y RK02 en `docs/risks-and-open-questions.md`. **Si el modelo no alcanza el objetivo o no soporta API 30: PARAR y preguntar al usuario** (opciones: aceptar el modelo con umbral menor —los chips editables y RD05 son red de seguridad—, probar la alternativa YOLOv8 ONNX (`P23`) o degradar el Escáner a modo manual y dejar la detección fuera del MVP). No decidir por cuenta propia.
1. **Dominio** (`domain/`): `DetectedFood(name, confidence)` (detección cruda), el chip de bandeja `ScanDraftItem(name, confidence: Double?, quantity: Int)` —cantidad entera 0..999 (0 = sin cantidad), `confidence = null` en los añadidos a mano— y el Escaneo temporal `Scan(createdAt, detections)` en `domain/model/`; reglas puras en `domain/scanner/` (patrón `RecipeValidation`): filtrar por umbral mínimo de confianza, **fusionar los duplicados del Escaneo por `normalizeName`** (un chip por nombre normalizado; se conserva el nombre más legible y la confianza máxima), validar nombre no vacío, calcular las coincidencias de la bandeja con la Despensa (mismo `normalizeName`) y su resolución (mantener/actualizar cantidad) y convertir los chips confirmados a `PantryItem` con `quantity = chip.quantity.takeIf { it > 0 }` (0 = sin cantidad → `null`). Sin Compose ni Android.
2. **Puertos y adaptadores** (`data/scanner/`), siguiendo el patrón de `PhotoAcquirer`:
   - `ScanImageSource` (puerto de captura): `suspend fun capture(): ScanCaptureResult` con `Success(bytes) | CameraPermissionDenied | Cancelled | Failed` (misma semántica que `PhotoResult`). Implementación real `CameraXScanImageSource` (Preview + `ImageCapture` con `OnImageCapturedCallback` → bytes **en memoria**, nunca `OutputFileOptions` a disco — RD06); se construye en la Route con el `ActivityResultRegistry` para pedir `CAMERA` en runtime, igual que `SystemPhotoAcquirer`.
   - `FoodDetector` (puerto de inferencia): `suspend fun detect(image: ByteArray): FoodDetectionResult` (`Success(List<DetectedFood>) | Failed`). Implementación `MediaPipeFoodDetector` (on-device, modelo del spike; no conserva la imagen). Fakes `FakeScanImageSource`/`FakeFoodDetector` en `androidTest` para inyectar imagen y detecciones deterministas.
3. **UI** (`ui/scanner/`): `ScannerRoute` (Koin, cablea ViewModel + adaptadores), `ScannerViewModel(pantryRepository, foodDetector)` con `uiState` (estado del visor/captura/chips/avisos) y `ScannerScreen`. Estados: **visor en vivo** (CameraX `AndroidView(PreviewView)`) con obturador → **inferencia en curso** (progreso; la app nunca se congela) → **resultado**: imagen capturada congelada con etiquetas píldora «nombre · NN%» (HUD) y bandeja de **chips editables** (renombrar, «×» quitar, «Añadir alimento» manual y **stepper «− valor +» de cantidad** —entera 0..999, mismo patrón que el diálogo de Despensa: «−» deshabilitado en 0, «+» deshabilitado en 999, sin campo de texto ni teclado, 0 = sin cantidad—) + acción primaria «Guardar en la Despensa» + «Re-escanear». Los chips con coincidencia frente a la Despensa muestran la marca «Ya está en la Despensa» y su estado de decisión (pendiente / mantener / actualizar); resolver abre el diálogo descrito en el punto 4. El `ScannerViewModel` observa `PantryRepository.observePantryItems()` para calcular y mantener al día las coincidencias. Con imagen falsa en tests, el área de visor es un placeholder inerte (los tests no dependen de la preview).
4. **Persistencia y coincidencias (revisión 2026-10-21, Q-escanear-despensa-con-camara-1)**: al confirmar, cada chip pasa por `PantryRepository.savePantryItem` con la cantidad elegida en su stepper (0 = sin cantidad → `null`); los chips sin coincidencia se guardan como Alimentos nuevos. Un chip cuyo `normalizeName` coincide con un Alimento de la Despensa **no se fusiona automáticamente: se pregunta en pantalla antes de guardar**, con un diálogo `AlertDialog` por coincidencia (mismo patrón que `eliminar-receta`, con el nombre y la cantidad existente) y dos resoluciones: **«Mantener cantidad»** —el Alimento existente queda intacto (conserva su cantidad, o sigue sin cantidad) y el chip no crea fila— o **«Actualizar cantidad»** —el Alimento existente adopta la cantidad del chip (incluido 0 = sin cantidad) regrabándose con su mismo `id`—; en ambos casos un solo Alimento por nombre normalizado, sin filas nuevas. Las coincidencias se recalculan al cambiar la bandeja (detectar, renombrar, añadir, quitar) y se resuelven de forma independiente; si hay varias, se preguntan una a una en el orden de la bandeja. «Guardar en la Despensa» con coincidencias pendientes **no guarda nada**: primero pregunta por cada pendiente y solo guarda cuando todas están resueltas; descartar un diálogo (atrás o toque fuera) aborta el guardado y deja la bandeja intacta (chips y decisiones ya tomadas). Tras guardar, el Escaneo temporal se descarta y se navega a la Despensa (`AppRoutes.PANTRY`) donde aparecen los Alimentos. Si el usuario sale con chips sin guardar (atrás/navegación fuera), diálogo de confirmación antes de descartar (Q-escanear-despensa-con-camara-2, confirmado el 2026-10-21; mismo patrón que `eliminar-receta`, S10).
5. **Degradación (RD05)**: `CameraPermissionDenied` → aviso + modo manual (chips a mano); 0 detecciones → mensaje + alta manual; `FoodDetector` en fallo → aviso + modo manual. Nunca se bloquea el flujo (RF-23 como precedente).
6. **Recursos**: claves `scanner_*` en `values/strings.xml` y `values-en/strings.xml`, incluidas las de coincidencias y salida (`scanner_duplicate_*`, `scanner_exit_*`) y los `contentDescription` del stepper de cantidad («Disminuir cantidad»/«Aumentar cantidad», mismo criterio que Despensa). Copy propuesto (es/en), patrón de `eliminar-receta` (título + mensaje con el nombre + acciones claras + descarte definido):
   - Marca del chip: «Ya está en la Despensa» / “Already in the Pantry”.
   - Diálogo de coincidencia (la cantidad existente se muestra como «Cantidad: N»/“Quantity: N” o «sin cantidad»/“no quantity”): título «Alimento ya en la Despensa» / “Food already in the Pantry”; mensaje ««{nombre}» ya está en la Despensa ({cantidad}). ¿Qué cantidad conservamos?» / “"{name}" is already in your Pantry ({quantity}). Which quantity should we keep?”; botones «Mantener cantidad» / “Keep quantity” y «Actualizar cantidad» / “Update quantity”; descartar (atrás/toque fuera) no decide nada (Q-eliminar-receta-2 como precedente).
   - Diálogo de salida con chips sin guardar (S10): título «¿Salir del Escáner?» / “Leave the scanner?”; mensaje «Tienes alimentos sin guardar. Si sales, se descartan.» / “You have unsaved foods. If you leave, they will be discarded.”; botones «Cancelar» / “Cancel” y «Salir» / “Leave” (tinte `error`); descartar = cancelar.
   Los nombres de Alimento los aporta el detector: las etiquetas del modelo se muestran con su nombre canónico en el idioma de la UI mediante un mapa etiqueta→cadena (p. ej. `food_label_*` es/en) con la etiqueta cruda como fallback; el alcance del vocabulario lo fija el spike (ver D4).
7. **Tests**: unit (`app/src/test`) sobre las reglas de `domain/scanner/` (umbral, fusión del Escaneo por nombre normalizado, nombre vacío, conversión a `PantryItem` con cantidad —0 = sin cantidad, límites 0/999— y resolución de coincidencias mantener/actualizar) y sobre `ScannerViewModel` con `FakeFoodDetector`/`InMemoryPantryRepository` + `coroutines-test` (guardado con cantidades, resolución de coincidencias, confirmar con pendientes, abort por descarte, degradación). E2E (`app/src/androidTest`) `ScanPantryE2ETest` montando `ScannerScreen` con fakes (patrón `CreateRecipeE2ETest`): inyección de imagen → chips con confianza y stepper (verify 1) → confirmar → Alimentos en `InMemoryPantryRepository` con las cantidades de los chips y visibles en `PantryScreen` (verify 2); más límites del stepper (0..999, 0 = sin cantidad), coincidencias con la Despensa (S11/S12), permiso denegado, sin detecciones, fallo del detector y RD06 (sin ficheros de imagen tras el flujo). `MediaPipeFoodDetectorTest` (androidTest, fixture local de imagen) valida el adaptador real en el dispositivo sin cámara. `MyRecipesLocalizationE2ETest` se actualiza al nuevo testTag; se añade localización es/en del Escáner.

## Expected File Changes

- `gradle/libs.versions.toml`, `app/build.gradle.kts` — modificar; CameraX (`camera-core`/`camera2`/`camera-lifecycle`/`camera-view`), MediaPipe `tasks-vision`, modelo `.tflite` (`noCompress` si procede) y, si hace falta, `androidx-test-rules`; versiones exactas según el spike.
- `app/src/main/assets/` — crear; modelo `.tflite` validado por el spike (nombre y origen documentados).
- `app/src/main/AndroidManifest.xml` — sin cambios previstos (permiso `CAMERA`, `uses-feature` y `FileProvider` ya existen y se reutilizan; la captura del Escáner no usa `FileProvider` por RD06).
- `app/src/main/java/com/whatsforlunch/app/domain/model/DetectedFood.kt`, `domain/model/ScanDraftItem.kt`/`Scan.kt` — crear; detección cruda, chip de bandeja con cantidad entera (0..999; 0 = sin cantidad) y Escaneo temporal.
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` (u equivalente) — crear; reglas puras (umbral, fusión del Escaneo por `normalizeName`, validación, conversión con cantidad y resolución de coincidencias mantener/actualizar).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt`, `CameraXScanImageSource.kt` — crear; puerto de captura + adaptador CameraX (bytes en memoria).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/FoodDetector.kt`, `MediaPipeFoodDetector.kt` — crear; puerto de inferencia + adaptador MediaPipe on-device.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt`, `ScannerScreen.kt`, `ScannerViewModel.kt` — crear; pantalla, cableado y estado.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; `scan_pantry` → `ScannerRoute` (fin del `ProvisionalScreen` del Escáner).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `FoodDetector`, `ScanImageSource` (o registro desde la Route) y `ScannerViewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; cadenas `scanner_*` (incluidas `scanner_duplicate_*`, `scanner_exit_*` y los `contentDescription` del stepper) y mapa `food_label_*` (según D4).
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/*Test.kt`, `ui/scanner/ScannerViewModelTest.kt` — crear; unit (nombres en inglés, a criterio del implementador) con los casos de cantidad y de resolución de coincidencias.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt`, `MediaPipeFoodDetectorTest.kt` — crear; E2E con fakes e instrumentado del adaptador real con fixture.
- `app/src/androidTest/java/com/whatsforlunch/app/MyRecipesLocalizationE2ETest.kt` — modificar; `ProvisionalTestTags.SCANNER_SCREEN` → `ScannerTestTags.SCREEN`.
- `docs/technical-discovery.md` — modificar; modelo MediaPipe concreto, dependencias CameraX/MediaPipe y resultado del spike.
- `docs/risks-and-open-questions.md` — modificar; cerrar/anotar RK02 con el resultado del spike y mantener en el apéndice Q-escanear-despensa-con-camara-1..3 como **resueltas el 2026-10-21** (respuestas del usuario: «Preguntar en pantalla», «Pedir confirmación», «Con stepper en los chips»), con el resumen en 0 supuestos sin confirmar.
- `docs/domain-model.md` — modificar; cantidad editable del Escaneo (chips con stepper 0..999; 0 = sin cantidad) y coincidencias con la Despensa resueltas preguntando en pantalla, manteniendo un Alimento por nombre normalizado (Q-escanear-despensa-con-camara-1 y -3).

Los paths de `ui/scanner/`, `data/scanner/` y `domain/scanner/` son los previstos; el implementador puede ajustar nombres dentro del patrón de capas existente.

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); `uxdocs/chefencasa_esc_ner_de_nevera_ia/` como inspiración (`P16`), con lagunas explícitas (prototipo sin estados de permiso denegado, sin detecciones, de carga ni de fallo).
- Screens or states affected: «Escáner» (nueva: visor en vivo, inferencia en curso, resultado con HUD + chips con stepper de cantidad, marcas de coincidencia con la Despensa y sus diálogos de resolución, sin detecciones, permiso denegado / modo manual, fallo del detector, diálogo de salida con chips sin guardar); Despensa (solo como destino tras guardar; sin cambios).
- New design artifact required: no — componentes M3 y tokens existentes.
- Criterios: fondo/superficie `#FBF9F5`; chips de detección píldora (confirmados con fondo verde salvia `#006c4a`/secundario según `DESIGN.md`) con stepper de cantidad «− valor +» igual que el diálogo de Despensa («−» deshabilitado en 0, «+» en 999, estado deshabilitado distinguible, sin teclado); etiquetas HUD píldora con nombre y confianza sobre la imagen; diálogos de coincidencia y de salida con `AlertDialog` M3 y los tokens existentes (no cubiertos por el prototipo, como el de `eliminar-receta`); acción primaria «Guardar en la Despensa» terracota `#A83212` (altura mínima 52 dp); estados de carga con progreso claro; avisos en `tertiary`/`error` según caso; todo objetivo táctil ≥ 48 dp; contraste WCAG AA sobre la imagen del visor (velo oscuro tras las etiquetas, como el prototipo).
- Copy propuesto (es/en) en el punto 6 del enfoque técnico (incluidos los diálogos de coincidencia y de salida); la revisión visual puede ajustarlo sin cambiar comportamiento.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe; las capas `ui`/`domain`/`data` y la dirección de dependencias no cambian (adapters nuevos del patrón ya usado por foto/IA).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD05/RD06 ya viven en `docs/domain-model.md`).
- `AGENTS.md`: not needed — sin cambios de flujo de agentes, comandos ni convenciones.
- Other docs: `docs/technical-discovery.md` — update (modelo concreto y dependencias tras el spike; sin cambios por esta revisión); `docs/risks-and-open-questions.md` — update (RK02 al cerrar el spike; Q-escanear-despensa-con-camara-1..3 en el apéndice como **resueltas el 2026-10-21** con las decisiones del usuario y resumen en 0 supuestos sin confirmar); `docs/domain-model.md` — update (cantidad editable en el Escaneo y «preguntar en pantalla» para las coincidencias con la Despensa, sin cambiar «un Alimento por nombre normalizado»).

## Dudas abiertas y desconocidos (Unknowns)

- **D1 · RK02 (bloqueante previo)**: el modelo MediaPipe concreto está por validar (> 88 % de confianza, API 30). El spike es paso 0 obligatorio; **si no cumple, se PARA y se pregunta al usuario** (aceptar umbral menor / YOLOv8 ONNX / degradar a manual). Mientras no esté validado, nada de la detección se da por bueno. El umbral mínimo de confianza para proponer chip es parámetro del spike (candidato 0.80, el del prototipo).
- **D2 · resuelta el 2026-10-21** (Q-escanear-despensa-con-camara-1): respuesta del usuario **«Preguntar en pantalla»** — cuando un Alimento escaneado coincide (por nombre normalizado) con uno ya presente en la Despensa, el Escáner **pregunta en el momento qué hacer, antes de guardar** (diálogo por coincidencia, patrón `eliminar-receta`, con «Mantener cantidad»/«Actualizar cantidad»; S11 y S12). Sustituye a la fusión automática silenciosa con aviso resumido; se conserva la regla de dominio «un Alimento por nombre normalizado».
- **D3 · confirmada el 2026-10-21** (Q-escanear-despensa-con-camara-2): respuesta del usuario **«Pedir confirmación»** (supuesto D3 confirmado) — al salir del Escáner con chips sin guardar (atrás o navegación fuera) se muestra un diálogo de confirmación con el patrón de `eliminar-receta`; «Cancelar» conserva el Escáner y sus chips, confirmar descarta el Escaneo y sale. S10 se mantiene tal cual.
- **D4 (depende del spike)**: vocabulario del modelo y traducción de etiquetas (mapa `food_label_*` es/en con fallback a etiqueta cruda). Si el vocabulario es muy amplio, priorizar los alimentos del MVP y confirmar el criterio con el usuario.
- **D5 · resuelta el 2026-10-21** (Q-escanear-despensa-con-camara-3): respuesta del usuario **«Con stepper en los chips»** — los chips del Escáner **editan la cantidad** con un stepper entero 0..999 (0 = sin cantidad), igual que el formulario de la Despensa, y se guarda con la cantidad elegida (S2, S3). Se retira el Non-Goal «los chips no editan cantidad»; se conservan el límite 0..999 y «0 = sin cantidad» de `gestionar-alimentos-de-la-despensa`.
- Decisiones de interacción fijadas por esta spec por estar el prototipo incompleto (visor → captura congelada con etiquetas → chips → guardar; no hay detección en vivo sobre el preview): se revisan en la validación visual sin cambiar el comportamiento.

## Implementation Plan

1. **Spike RK02** (gate): modelo, confianza > 88 % y API 30 en emulador; documentar resultado y decidir con el usuario si no cumple.
2. Dominio (`DetectedFood`, `ScanDraftItem`, `Scan`, reglas de fusión del Escaneo/umbral/conversión con cantidad/resolución de coincidencias) con unit.
3. Puertos `ScanImageSource`/`FoodDetector` y adaptadores CameraX/MediaPipe; dependencias Gradle; wiring Koin.
4. UI del Escáner (`ScannerViewModel`/`ScannerScreen`/`ScannerRoute`), navegación y cadenas es/en; chips con stepper de cantidad, marcas y diálogos de coincidencia «preguntar en pantalla», guardado en Despensa y degradaciones RD05/RD06.
5. Tests (unit + E2E + adaptador real con fixture), harness completo, evidencia y cierre en `PROGRESS.md`/`feature_list.json` (`passing`).

## Implementation Tasks

- [ ] T0: spike del modelo MediaPipe (RK02): confianza > 88 %, API 30, `.tflite` elegido; resultado en `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`. Si no cumple: parar y preguntar. (gate)
- [ ] T1: `DetectedFood`, `ScanDraftItem`, `Scan` y reglas puras (`domain/scanner/`) con unit (umbral, fusión del Escaneo por nombre normalizado, nombre vacío, conversión a `PantryItem` con cantidad 0..999 —0 = sin cantidad— y resolución de coincidencias mantener/actualizar). (S1, S2, S4, S11)
- [ ] T2: dependencias Gradle + `FoodDetector`/`MediaPipeFoodDetector` y `ScanImageSource`/`CameraXScanImageSource` (captura en memoria, RD06) + Koin. (S1, S8)
- [ ] T3: `ScannerViewModel`/`ScannerScreen`/`ScannerRoute`: visor, obturador, inferencia en curso, HUD con etiquetas y confianza, chips editables (renombrar/quitar/añadir y stepper de cantidad 0..999, «−» deshabilitado en 0 y «+» en 999, 0 = sin cantidad). (S1, S3, S6)
- [ ] T4: «Guardar en la Despensa» con la cantidad de cada chip y resolución en pantalla de las coincidencias (diálogo por coincidencia con «Mantener cantidad»/«Actualizar cantidad», varias una a una, confirmar con pendientes pregunta antes de guardar y descartar aborta) y navegación a Despensa; descarte del Escaneo; diálogo de confirmación al salir con chips sin guardar. (S2, S10, S11, S12)
- [ ] T5: degradaciones RD05 (permiso denegado, 0 detecciones, fallo del detector) y comprobación RD06. (S5, S6, S7, S8)
- [ ] T6: cadenas `scanner_*` (incluidas `scanner_duplicate_*`/`scanner_exit_*`)/`food_label_*` es/en y testTags; actualizar `MyRecipesLocalizationE2ETest` y añadir localización del Escáner. (S9)
- [ ] T7: `ScanPantryE2ETest` (fakes con imagen inyectada), `MediaPipeFoodDetectorTest` (fixture) y unit de ViewModel sin regresiones. (S1-S12)
- [ ] T8: `./init.ps1`, `./lint.ps1`, `./unit.ps1`, `./e2e.ps1`, `./gradlew assembleDebug` en verde; evidencia capturada; feature en `passing`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; **no arranca procesos duraderos**; los scripts no se modifican). Nota de entorno de `PROGRESS.md`: ejecutar con `cmd /c "set PATH=...&& powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`.
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `./gradlew assembleDebug` → BUILD SUCCESSFUL; `./gradlew testDebugUnitTest` incluye los unit nuevos (fusión del Escaneo por nombre normalizado, umbral, conversión con cantidad 0..999/0 = sin cantidad, resolución de coincidencias mantener/actualizar, guardado con cantidades).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en emulador API 30+ (AVD `Pixel_10_Pro`), con `ScanPantryE2ETest`:
  - verify 1 (S1): imagen inyectada por `FakeScanImageSource` + `FakeFoodDetector` → chips con nombre y confianza.
  - verify 2 (S2): confirmar → Alimentos en la Despensa con las cantidades elegidas en los chips (0 = sin cantidad; repositorio y `PantryScreen`).
  - S3 quitar/renombrar/añadir chips y stepper de cantidad («−» deshabilitado en 0, «+» en 999, sin campo de texto, 0 = sin cantidad); S4 duplicados del Escaneo fusionados en un chip; S5 permiso denegado → aviso + modo manual; S6 cero detecciones → mensaje + alta manual; S7 detector en fallo → aviso + manual; S8 sin ficheros de imagen tras el flujo (RD06: comprobar `cacheDir`/`filesDir`); S9 textos con locale `en-US` y ≥ 48 dp (incluidos los diálogos de coincidencia y de salida); S10 salir con chips sin guardar → diálogo de confirmación (cancelar conserva los chips, confirmar descarta y sale); S11 coincidencia con la Despensa → diálogo con «Mantener cantidad»/«Actualizar cantidad» y cantidades resultantes correctas, sin fila nueva; S12 varias coincidencias → preguntas una a una, nada se guarda con pendientes y descartar un diálogo aborta el guardado dejando la bandeja intacta.
  - `MediaPipeFoodDetectorTest`: fixture local de imagen con alimentos → ≥ 1 detección con confianza ≥ umbral sobre el adaptador real (resultado dependiente del spike; ajustar expectativas al modelo elegido).
  - Regresión completa en verde sin romper `AppLaunchE2ETest`, `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `DeleteRecipeE2ETest`, `SearchAndFilterRecipesE2ETest`, `PantryManagementE2ETest`, `PantryLocalizationE2ETest`, `MatchBadgeE2ETest`, `MyRecipesLocalizationE2ETest` (esta última con el testTag actualizado).
- Comprobación manual del validador (con cámara real del emulador/dispositivo): visor en vivo, captura con etiquetas reales del modelo del spike y latencia de inferencia razonable (objetivo PRD < 800 ms, «Por validar»); no exige más precisión que la fijada en el spike.
- Cobertura E2E: sí se añade, porque la feature introduce flujo de usuario nuevo (captura → chips → Despensa) y depende de permisos; el detector real no es determinista para una E2E y por eso la cadena completa se prueba con fakes (mismo criterio que las fotos de `crear-receta`), dejando al adaptador real su test instrumentado con fixture y el spike como validación de precisión.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- Resultado del spike (T0) en `docs/technical-discovery.md`/`docs/risks-and-open-questions.md`: modelo elegido, confianza observada, compatibilidad API 30.
- `docs/validations/escanear-despensa-con-camara.md` — informe con `lint`/`unit`/`e2e` (nombres de test), recorrido de S1-S12 (incluidas las resoluciones de coincidencias y el stepper de cantidad) y comprobación RD06 (sin imágenes persistidas); se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/escanear-despensa-con-camara/` — hasta 2 capturas en español (visor con etiquetas y chips con stepper de cantidad; diálogo de coincidencia con la Despensa, o Despensa con los Alimentos guardados y sus cantidades).
- Presupuesto ≤ 4 artefactos y solo de esta feature (1 informe + 2 capturas + el log de `e2e` referenciado si aporta).

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (sin carrusel de Recetas, sin Sugerencias, sin Lista de la Compra, sin unidades ni decimales en la cantidad, sin sumar cantidades al resolver coincidencias, sin tocar Room v2 ni `PantryRepository`).
- [ ] El spike RK02 está documentado y su veredicto se respeta (si no cumplía, hay decisión explícita del usuario registrada).
- [ ] Los escenarios de aceptación pasan (S1-S12) y los dos `verify` de `feature_list.json` se ejecutan en el emulador.
- [ ] Coincidencias verificadas: ninguna fusión automática silenciosa con la Despensa —se pregunta en pantalla por cada coincidencia («Mantener cantidad»/«Actualizar cantidad»)— y se conserva un solo Alimento por nombre normalizado (S11/S12).
- [ ] Cantidad de los chips verificada: stepper entero 0..999 con 0 = sin cantidad («−» deshabilitado en 0, «+» en 999, sin campo de texto) y persistencia con la cantidad elegida.
- [ ] RD06 verificado: ninguna imagen de Escaneo persistida (sin `OutputFileOptions`/ficheros; sin tabla de Escaneos).
- [ ] RD05 verificado: permiso denegado, cero detecciones y fallo del detector degradan a entrada manual con aviso, sin bloquear.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E actualizada (incluido `MyRecipesLocalizationE2ETest`) y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature pasa a `passing` solo tras `lint` + `unit` + `assembleDebug` (y E2E si procede).
- [ ] No se añadió comportamiento de producto ni trabajo de otras features.
