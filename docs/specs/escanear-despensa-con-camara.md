# Feature Implementation Spec: Escanear la Despensa con la cámara

> **Revisión 2026-10-21 (delta sobre la cantidad máxima, decisión del usuario)**: la cantidad de un Alimento admite como máximo **99** (rango entero **0..99**; 0 = sin cantidad) en **toda la app**, no solo en los chips del Escáner: `PantryItemValidation.MAX_QUANTITY` pasa a 99 y arrastra Despensa, Escáner, repositorios y sus tests (Q-escanear-despensa-con-camara-4). Los chips del Escáner dejan de decir «igual que en Despensa» como excusa para heredar 999: ambos comparten el nuevo límite 0..99. El resto de la spec se actualiza a 0..99 (`−` deshabilitado en 0, `+` deshabilitado en 99).

> **Revisión 2026-10-21 (delta sobre las dudas D2/D3/D5, respuestas del usuario)**: (1) un Alimento escaneado cuyo nombre normalizado coincide con uno ya presente en la Despensa **se pregunta en pantalla antes de guardar** (diálogo por coincidencia con «Mantener cantidad»/«Actualizar cantidad»): ya no hay fusión automática silenciosa con aviso resumido (Q-escanear-despensa-con-camara-1); la regla de dominio «un Alimento por nombre normalizado» se conserva; (2) salir del Escáner con chips sin guardar **pide confirmación** (supuesto D3 confirmado; S10 se mantiene; Q-escanear-despensa-con-camara-2); (3) los chips del Escáner **editan la cantidad** con un stepper entero 0..99 (0 = sin cantidad), igual que el formulario de la Despensa (Q-escanear-despensa-con-camara-3), y se guarda con la cantidad elegida.

## Source Feature

- `id`: escanear-despensa-con-camara
- `area`: funcional
- `depends_on`: gestionar-alimentos-de-la-despensa (`accepted`)
- `status`: `not_started` en el momento de planificar
- `source`: `feature_list.json`

## Goal

Sustituir el destino provisional `scan_pantry` por el **Escáner** real que llena la **Despensa** desde la cámara: el usuario ve el visor, captura una imagen, la detección de Alimentos ocurre **en el dispositivo** (MediaPipe, `P23`, sin red) y la app propone **chips editables** (nombre, cantidad con stepper entero 0..99 y confianza) que se corrigen, quitan o amplían antes de **confirmar y guardar** en la Despensa (Room), resolviendo **en pantalla** —antes de guardar— las coincidencias por nombre normalizado con Alimentos ya presentes (Q-escanear-despensa-con-camara-1). Es el tramo `Camara → MediaPipe → Chips → Despensa` de `docs/technical-discovery.md`.

Lo duradero son solo los **Alimentos** de la Despensa (Room vía `PantryRepository`, ya existente). El **Escaneo** es temporal (detecciones con confianza, en memoria, hasta confirmar los chips, `docs/domain-model.md`). La foto capturada vive solo en memoria para mostrar la vista congelada con HUD y se libera al guardar, re-escanear o salir; **nunca se escribe a disco** (RD06, RGPD).

## Non-Goals

- Carrusel de Recetas compatibles ni Sugerencias (`mostrar-recetas-compatibles`, `sugerir-recetas-con-ia`).
- Faltantes hacia la Lista de la Compra desde el Escaneo (`anadir-faltantes-a-la-lista`).
- Calibración del umbral de confianza, cambio de cámara o galería desde el Escáner (el prototipo los dibuja; `P16`).
- Unidades, decimales o fracciones en la cantidad de los chips: es siempre un entero 0..99 sin unidad, igual que en Despensa (0 = sin cantidad), sin entrada de texto.
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
Then solo las detecciones con confianza ≥ 60% proponen chips, que muestran el nombre y la confianza (p. ej. «Pizza · 74%»), junto con las etiquetas de detección sobre la imagen capturada; esta foto permanece únicamente en memoria mientras se revisa el resultado

### Scenario 2: Confirmar y guardar en la Despensa (verify 2 de `feature_list.json`)

Given los chips de detección visibles con las cantidades elegidas por el usuario (p. ej. «Tomate fresco» con 3 y «Leche» en 0 = sin cantidad)
When el usuario confirma («Guardar en la Despensa») y no quedan coincidencias pendientes
Then los Alimentos aparecen en la Despensa con la cantidad elegida en cada chip (persistidos en Room; 0 = sin cantidad → Alimento sin cantidad, Q-escanear-despensa-con-camara-3) y el Escaneo queda descartado

### Scenario 3: Quitar, renombrar, ajustar cantidad y añadir chips (RD05)

Given los chips de detección visibles
When el usuario quita un chip con «×», renombra otro, ajusta la cantidad de un tercero con el stepper «− valor +» y añade uno manualmente («Añadir alimento»)
Then la bandeja refleja los cambios —la cantidad es un entero 0..99 (0 = sin cantidad), «−» deshabilitado en 0 y «+» en 99, sin campo de texto— y solo eso se guarda al confirmar. Si el nombre editado colisiona por normalización, se fusiona con el chip editado como ganador (conserva nombre/cantidad) y confianza máxima; «Añadir alimento» con un nombre ya presente conserva el chip existente y no crea otro

### Scenario 4: Duplicados dentro del Escaneo, un solo chip (regla de dominio)

Given dos detecciones con el mismo nombre normalizado («TOMATE FRESCO» y «  Tomate  fresco »)
When se construye la bandeja de chips
Then queda un solo chip por nombre normalizado (en detecciones: nombre más legible, confianza máxima y cantidad inicial 0; al renombrar: el chip editado conserva nombre/cantidad y gana, con confianza máxima; al añadir manualmente: se conserva el chip existente) y un único stepper; se mantiene «un Alimento por nombre normalizado» (`docs/domain-model.md`). Una coincidencia con Alimentos **ya presentes en la Despensa** no se fusiona a escondidas: se pregunta en pantalla (S11 y S12).

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
Then se muestra un diálogo de confirmación (mismo patrón que `eliminar-receta`: «Cancelar» conserva el Escáner y sus chips; confirmar descarta el Escaneo temporal y sale). «Re-escanear» con chips pendientes abre el mismo diálogo: cancelar conserva la bandeja; confirmar la descarta y comienza una nueva captura.

### Scenario 11: Coincidencia con la Despensa: preguntar en pantalla (Q-escanear-despensa-con-camara-1)

Given un chip «Tomate fresco» con cantidad 3 y un Alimento «  TOMATE  fresco » ya en la Despensa con cantidad 1
When se detecta la coincidencia (al generarse, renombrarse o añadirse el chip)
Then el chip queda marcado («Ya está en la Despensa») y el Escáner abre automáticamente un diálogo por coincidencia, con nombre y cantidad existente (patrón `eliminar-receta`): «Mantener cantidad» deja el existente intacto y el chip no crea fila; «Actualizar cantidad» deja el existente con la cantidad del chip (0 = sin cantidad). La decisión se puede cambiar tocando la marca del chip, que reabre el diálogo. En ambos casos queda un solo Alimento por nombre normalizado, sin filas nuevas.

### Scenario 12: Varias coincidencias y confirmación con pendientes

Given varios chips que coinciden con Alimentos de la Despensa, alguno sin resolver
When el usuario pulsa «Guardar en la Despensa»
Then no se guarda nada todavía: el Escáner pregunta por cada coincidencia pendiente una a una (orden de la bandeja) y solo guarda cuando todas están resueltas (junto al resto de chips); descartar un diálogo (atrás o toque fuera) aborta el guardado y deja la bandeja intacta —chips y decisiones ya tomadas—, sin filas parciales en la Despensa

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `escanear-despensa-con-camara` (steps, verification, notes con RK02/RD06); dependencia `gestionar-alimentos-de-la-despensa` en `accepted`.
- `PROGRESS.md` — estado del harness, entorno (AVD `Pixel_10_Pro` API 37), convención de scripts y capturas en español.
- `docs/risks-and-open-questions.md` — RK02 aceptado: EfficientDet-Lite2 con umbral 0.60, chips editables y RD05, decisión del usuario del 2026-10-21.
- `docs/domain-model.md` — entidades Alimento (nombre, cantidad entera 0..99; 0 = sin cantidad) y Escaneo (fecha, Alimentos detectados con confianza; temporal: «sus resultados viven solo hasta confirmar los chips»); fusión de duplicados por nombre normalizado (a plasmar con la decisión 2026-10-21: dentro del Escaneo un solo chip y frente a la Despensa preguntar en pantalla, siempre un Alimento por nombre normalizado); casos límite «Escaneo sin detecciones», «Fallo de la IA» (RD05), «Ingrediente detectado duplicado»; RD05 y RD06.
- `docs/technical-discovery.md` — MediaPipe on-device (`P23`), modelo EfficientDet-Lite2 y umbral 0.60 adoptados, dependencias CameraX/MediaPipe definitivas, flujo `Camara → MediaPipe → Chips → Despensa`, datos temporales/RD06 y comandos Gradle.
- `docs/build-brief.md`, `CONTEXT.md` — líneas rojas (RGPD: fotos de escaneo sin almacenar), términos «Escaneo» vs «Escáner» (no sinónimos), «Despensa».
- `DESIGN.md` — pantalla «Escáner de la Despensa» (visor, HUD con etiquetas y confianza, chips editables; carrusel fuera de alcance), chips confirmados en verde salvia, estados vacío/carga/error, accesibilidad.
- `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html`, `screen.png` — inspiración (`P16`): etiquetas píldora «nombre · 96%» sobre el visor, bandeja «Detectados» con chips y «×», «Añadir extra», obturador; los tokens de `uxdocs/DESIGN.md` mandan (`P17`).
- `app/src/main/AndroidManifest.xml` — permiso `CAMERA` y `uses-feature camera required=false` ya declarados (desde `crear-receta`); `FileProvider` ya existe para fotos de Receta.
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — **sin CameraX ni MediaPipe** hoy; BOM Compose, Koin, Room 2.8.5, minSdk 30, JUnit4 + `kotlinx-coroutines-test`.
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoAcquirer.kt`, `SystemPhotoAcquirer.kt` — patrón de puerto con `PhotoResult` (incluye `CameraPermissionDenied`), solicitud de `CAMERA` en runtime con `ActivityResultRegistry`, y fake que inyecta bytes de prueba en E2E.
- `app/src/main/java/com/whatsforlunch/app/domain/repository/PantryRepository.kt`, `domain/model/PantryItem.kt`, `domain/pantry/PantryItemValidation.kt`, `domain/text/NameNormalization.kt` — contrato (`savePantryItem` con `PantrySaveResult.Saved | DuplicateName`; cubre alta y edición, así que «Actualizar cantidad» regraba el Alimento existente con su mismo `id`), cantidad opcional, rango 0..99 (revisión 2026-10-21: `MAX_QUANTITY` pasa de 999 a 99 y se reutiliza en los chips del Escáner) y normalización de nombres ya en uso.
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
- Modelo final fijado por decisión del usuario: EfficientDet-Lite2, `tasks-vision:0.10.21`, threshold 0.60 y diez etiquetas alimentarias COCO con nombres localizados (ver D1/D4 y `docs/technical-discovery.md`).
- No hay pruebas de cámara ni de visión en el repo; el emulador no garantiza una escena real: la E2E debe inyectar imagen por puerto (y el detector real se valida en el spike y con una fixture local).
- `MyRecipesLocalizationE2ETest` depende del testTag provisional del Escáner.

## Technical Approach

0. **Spike RK02 completado**: cuatro Object Detectors se ejecutaron en API 30. El 2026-10-21 el usuario aceptó EfficientDet-Lite2 con umbral mínimo **0.60**, chips editables y degradación RD05. Hash, procedencia, dependencias y vocabulario están en `docs/technical-discovery.md`; RK02 registra la mitigación aceptada.
1. **Dominio** (`domain/`): `DetectedFood(name, confidence, boundingBox?)`, `ScanDraftItem(name, confidence: Double?, quantity: Int)` y `Scan`; reglas puras para filtrar `confidence >= 0.60` (RD12), fusionar duplicados por `normalizeName` (detecciones: nombre legible/confianza máxima; renombrado: chip editado conserva nombre/cantidad; alta manual duplicada: conserva chip existente), validar nombre, calcular/resolver coincidencias y convertir cantidad 0 a `null`. Sin Compose ni Android.
2. **Puertos y adaptadores** (`data/scanner/`), siguiendo el patrón de `PhotoAcquirer`:
    - `ScanImageSource` (puerto de captura): `suspend fun capture(): ScanCaptureResult` con `Success(bytes) | CameraPermissionDenied | Cancelled | Failed`. `CameraXScanImageSource` implementa además `CameraPreviewController`; CameraX Preview + `ImageCapture.OnImageCapturedCallback` entrega/copia bytes **en memoria**, siempre cierra `ImageProxy` y nunca usa `OutputFileOptions` (RD06). `ScannerRoute` solicita `CAMERA` con `rememberLauncherForActivityResult` y enlaza/desenlaza Preview al lifecycle.
    - `FoodDetector` (puerto de inferencia): `suspend fun detect(image: ByteArray): FoodDetectionResult` (`Success(List<DetectedFood>) | Failed`). `MediaPipeFoodDetector` usa el asset EfficientDet-Lite2, mantiene inferencia on-device, filtra el vocabulario COCO alimentario y no conserva la imagen. Fakes `FakeScanImageSource`/`FakeFoodDetector` en `androidTest` inyectan bytes y detecciones deterministas.
3. **UI** (`ui/scanner/`): `ScannerRoute` (Koin, cablea ViewModel + adaptadores), `ScannerViewModel` con estado de visor/captura/chips/avisos y `ScannerScreen`. Estados: visor CameraX en vivo + obturador → inferencia con progreso → resultado con imagen congelada **en memoria** y HUD con confianza/etiquetas; chips editables con renombrado, borrado, alta manual y stepper 0..99. Los chips con coincidencia abren automáticamente el diálogo; tocar la marca lo reabre para cambiar una decisión. Con fakes, visor placeholder inerte; `ScanPantryE2ETest` afirma etiquetas HUD por testTag/texto.
4. **Persistencia y coincidencias**: al confirmar, primero se resuelven en serie todas las coincidencias pendientes y no se persiste nada hasta completar todas. «Mantener» conserva el Alimento existente; «Actualizar» lo guarda con el `id` existente y cantidad del chip (0 → `null`). Las coincidencias se recalculan al editar bandeja, aparecen automáticamente y se pueden reabrir/cambiar tocando la marca. Si se descarta un diálogo durante el guardado, se aborta todo y bandeja/decisiones quedan intactas. Navegar atrás/salir con chips sin guardar pide confirmación; re-escanear con chips usa la misma confirmación y al aceptar descarta el resultado anterior e inicia nueva captura.
5. **Degradación (RD05)**: `CameraPermissionDenied` → aviso + modo manual (chips a mano); 0 detecciones → mensaje + alta manual; `FoodDetector` en fallo → aviso + modo manual. Nunca se bloquea el flujo (RF-23 como precedente).
6. **Recursos**: claves `scanner_*` en `values/strings.xml` y `values-en/strings.xml`, incluidas las de coincidencias y salida (`scanner_duplicate_*`, `scanner_exit_*`), `food_label_*` para el vocabulario alimentario real del Lite2 y `contentDescription` del stepper.
   - Marca del chip: «Ya está en la Despensa» / “Already in the Pantry”.
   - Diálogo de coincidencia (la cantidad existente se muestra como «Cantidad: N»/“Quantity: N” o «sin cantidad»/“no quantity”): título «Alimento ya en la Despensa» / “Food already in the Pantry”; mensaje ««{nombre}» ya está en la Despensa ({cantidad}). ¿Qué cantidad conservamos?» / “"{name}" is already in your Pantry ({quantity}). Which quantity should we keep?”; botones «Mantener cantidad» / “Keep quantity” y «Actualizar cantidad» / “Update quantity”; descartar (atrás/toque fuera) no decide nada (Q-eliminar-receta-2 como precedente).
   - Diálogo de salida con chips sin guardar (S10): título «¿Salir del Escáner?» / “Leave the scanner?”; mensaje «Tienes alimentos sin guardar. Si sales, se descartan.» / “You have unsaved foods. If you leave, they will be discarded.”; botones «Cancelar» / “Cancel” y «Salir» / “Leave” (tinte `error`); descartar = cancelar.
    EfficientDet-Lite2 usa etiquetas COCO; el Escáner solo acepta las diez clases alimentarias `apple`, `banana`, `broccoli`, `cake`, `carrot`, `donut`, `hot dog`, `orange`, `pizza`, `sandwich`. Se localizan con `food_label_*` (es/en); otras etiquetas del modelo se descartan y nunca se proponen como Alimentos.
7. **Tests**: unit (`app/src/test`) sobre las reglas de `domain/scanner/` (umbral, fusión del Escaneo por nombre normalizado, nombre vacío, conversión a `PantryItem` con cantidad —0 = sin cantidad, límites 0/99— y resolución de coincidencias mantener/actualizar) y sobre `ScannerViewModel` con `FakeFoodDetector`/`InMemoryPantryRepository` + `coroutines-test` (guardado con cantidades, resolución de coincidencias, confirmar con pendientes, abort por descarte, degradación). E2E (`app/src/androidTest`) `ScanPantryE2ETest` montando `ScannerScreen` con fakes (patrón `CreateRecipeE2ETest`): inyección de imagen → chips con confianza y stepper (verify 1) → confirmar → Alimentos en `InMemoryPantryRepository` con las cantidades de los chips y visibles en `PantryScreen` (verify 2); más límites del stepper (0..99, 0 = sin cantidad), coincidencias con la Despensa (S11/S12), permiso denegado, sin detecciones, fallo del detector y RD06 (sin ficheros de imagen tras el flujo). `MediaPipeFoodDetectorTest` (androidTest, fixture local de imagen) valida el adaptador real en el dispositivo sin cámara. `MyRecipesLocalizationE2ETest` se actualiza al nuevo testTag; se añade localización es/en del Escáner.

## Expected File Changes

- `gradle/libs.versions.toml`, `app/build.gradle.kts` — CameraX 1.5.3 (`camera-core`/`camera-camera2`/`camera-lifecycle`/`camera-view`) y `com.google.mediapipe:tasks-vision:0.10.35` (el spike de scores usó 0.10.21; la versión final resuelve ABI x86_64/páginas 16 KiB).
- `app/src/main/assets/efficientdet_lite2_detection_metadata_1.tflite` — modelo elegido (TensorFlow Task Library, SHA-256 en `docs/technical-discovery.md`).
- `app/src/main/AndroidManifest.xml` — sin cambios previstos (permiso `CAMERA`, `uses-feature` y `FileProvider` ya existen y se reutilizan; la captura del Escáner no usa `FileProvider` por RD06).
- `app/src/main/java/com/whatsforlunch/app/domain/model/DetectedFood.kt`, `domain/model/ScanDraftItem.kt`/`Scan.kt` — crear; detección cruda, chip de bandeja con cantidad entera (0..99; 0 = sin cantidad) y Escaneo temporal.
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` (u equivalente) — crear; reglas puras (umbral, fusión del Escaneo por `normalizeName`, validación, conversión con cantidad y resolución de coincidencias mantener/actualizar).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt`, `CameraXScanImageSource.kt` — crear; puerto de captura + adaptador CameraX (bytes en memoria).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/FoodDetector.kt`, `MediaPipeFoodDetector.kt` — crear; puerto de inferencia + adaptador MediaPipe on-device.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt`, `ScannerScreen.kt`, `ScannerViewModel.kt` — crear; pantalla, cableado y estado.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — modificar; `scan_pantry` → `ScannerRoute` (fin del `ProvisionalScreen` del Escáner).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `FoodDetector`, `ScanImageSource` (o registro desde la Route) y `ScannerViewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; cadenas `scanner_*` (incluidas `scanner_duplicate_*`, `scanner_exit_*` y los `contentDescription` del stepper) y mapa `food_label_*` (según D4).
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/*Test.kt`, `ui/scanner/ScannerViewModelTest.kt` — crear; unit (nombres en inglés, a criterio del implementador) con los casos de cantidad y de resolución de coincidencias.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt`, `MediaPipeFoodDetectorTest.kt`, `CameraXScanImageSourceTest.kt` — E2E S1-S12 con fakes, detector real Lite2 con fixture local en API 30 y captura CameraX JPEG en memoria sin archivos.
- `app/src/androidTest/java/com/whatsforlunch/app/MyRecipesLocalizationE2ETest.kt` — modificar; `ProvisionalTestTags.SCANNER_SCREEN` → `ScannerTestTags.SCREEN`.
- `docs/technical-discovery.md` — modelo final, dependencias CameraX/MediaPipe, hash y decisión del spike.
- `docs/risks-and-open-questions.md` — RK02 como riesgo aceptado; las Q-escanear 1..3 siguen resueltas el 2026-10-21.
- `docs/domain-model.md` — RD12 (threshold 0.60), cantidad editable, política de fusión y coincidencias preguntadas en pantalla.

Los paths de `ui/scanner/`, `data/scanner/` y `domain/scanner/` son los previstos; el implementador puede ajustar nombres dentro del patrón de capas existente.

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); `uxdocs/chefencasa_esc_ner_de_nevera_ia/` como inspiración (`P16`), con lagunas explícitas (prototipo sin estados de permiso denegado, sin detecciones, de carga ni de fallo).
- Screens or states affected: «Escáner» (nueva: visor en vivo, inferencia en curso, resultado con HUD + chips con stepper de cantidad, marcas de coincidencia con la Despensa y sus diálogos de resolución, sin detecciones, permiso denegado / modo manual, fallo del detector, diálogo de salida con chips sin guardar); Despensa (solo como destino tras guardar; sin cambios).
- New design artifact required: no — componentes M3 y tokens existentes.
- Criterios: fondo/superficie `#FBF9F5`; chips de detección píldora (confirmados con fondo verde salvia `#006c4a`/secundario según `DESIGN.md`) con stepper de cantidad «− valor +» igual que el diálogo de Despensa («−» deshabilitado en 0, «+» en 99, estado deshabilitado distinguible, sin teclado); etiquetas HUD píldora con nombre y confianza sobre la imagen; diálogos de coincidencia y de salida con `AlertDialog` M3 y los tokens existentes (no cubiertos por el prototipo, como el de `eliminar-receta`); acción primaria «Guardar en la Despensa» terracota `#A83212` (altura mínima 52 dp); estados de carga con progreso claro; avisos en `tertiary`/`error` según caso; todo objetivo táctil ≥ 48 dp; contraste WCAG AA sobre la imagen del visor (velo oscuro tras las etiquetas, como el prototipo).
- Copy propuesto (es/en) en el punto 6 del enfoque técnico (incluidos los diálogos de coincidencia y de salida); la revisión visual puede ajustarlo sin cambiar comportamiento.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe; las capas `ui`/`domain`/`data` y la dirección de dependencias no cambian (adapters nuevos del patrón ya usado por foto/IA).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD05/RD06 ya viven en `docs/domain-model.md`).
- `AGENTS.md`: not needed — sin cambios de flujo de agentes, comandos ni convenciones.
- Other docs: `docs/technical-discovery.md` (Lite2, threshold, hashes y CameraX/MediaPipe), `docs/risks-and-open-questions.md` (RK02 aceptado), `docs/domain-model.md` (RD12, stepper y duplicados).

## Dudas abiertas y desconocidos (Unknowns)

- **D1 · RK02 · resuelta por el usuario el 2026-10-21**: aunque ningún candidato llegó a >88%, el usuario acepta EfficientDet-Lite2 (`lite-model_efficientdet_lite2_detection_metadata_1.tflite`, SHA-256 `6FD32C84AB1EB0F7E7F3A7A20A20D7DF1530DAA8378728F7C79571096286BD52`) con umbral de propuesta **0.60**. La decisión cierra el gate T0; la protección son los chips editables y RD05 (modo manual con aviso ante fallo, cero resultados o permiso denegado). No probar YOLOv8 ni cambiar a modo manual como modo principal.
- **D2 · resuelta el 2026-10-21** (Q-escanear-despensa-con-camara-1): respuesta del usuario **«Preguntar en pantalla»** — cuando un Alimento escaneado coincide (por nombre normalizado) con uno ya presente en la Despensa, el Escáner **pregunta en el momento qué hacer, antes de guardar** (diálogo por coincidencia, patrón `eliminar-receta`, con «Mantener cantidad»/«Actualizar cantidad»; S11 y S12). Sustituye a la fusión automática silenciosa con aviso resumido; se conserva la regla de dominio «un Alimento por nombre normalizado».
- **D3 · confirmada el 2026-10-21** (Q-escanear-despensa-con-camara-2): respuesta del usuario **«Pedir confirmación»** (supuesto D3 confirmado) — al salir del Escáner con chips sin guardar (atrás o navegación fuera) se muestra un diálogo de confirmación con el patrón de `eliminar-receta`; «Cancelar» conserva el Escáner y sus chips, confirmar descarta el Escaneo y sale. S10 se mantiene tal cual.
- **D4 · resuelta durante la implementación**: el modelo Lite2 usa las etiquetas COCO. Se incluyen solo las diez clases de alimentos detectables pertinentes al dominio: `apple`, `banana`, `broccoli`, `cake`, `carrot`, `donut`, `hot dog`, `orange`, `pizza`, `sandwich`; traducción canónica en `food_label_*` es/en. Las otras etiquetas COCO (objetos, animales, personas) se filtran y no crean chips. No se abre otra pregunta.
- **D5 · resuelta el 2026-10-21** (Q-escanear-despensa-con-camara-3): respuesta del usuario **«Con stepper en los chips»** — los chips del Escáner **editan la cantidad** con un stepper entero 0..99 (0 = sin cantidad), igual que el formulario de la Despensa, y se guarda con la cantidad elegida (S2, S3). Se retira el Non-Goal «los chips no editan cantidad»; se conservan el «0 = sin cantidad» de `gestionar-alimentos-de-la-despensa` y su stepper, ahora con máximo **99** (D6).
- **D6 · resuelta el 2026-10-21** (Q-escanear-despensa-con-camara-4): respuesta del usuario — **la cantidad máxima de un Alimento es 99 en toda la app**. El cambio no es solo del Escáner: `PantryItemValidation.MAX_QUANTITY` pasa de 999 a 99, y con ello la Despensa, sus diálogos de alta/edición, los repositorios (`require` de rango), la conversión de chips a `PantryItem` y los tests de límites. Consecuencias: el rango válido es un entero **0..99** (0 = sin cantidad); los datos ya guardados por encima de 99 (si existe alguno) se recortan al leer/escribir según la guardia de dominio, sin migración de esquema Room; `docs/domain-model.md` y la spec de `gestionar-alimentos-de-la-despensa` deben reflejar el nuevo máximo.
- Decisiones de interacción fijadas por esta spec por estar el prototipo incompleto (visor → captura congelada con etiquetas → chips → guardar; no hay detección en vivo sobre el preview): se revisan en la validación visual sin cambiar el comportamiento.

## Implementation Plan

1. **Spike RK02** (gate completado): Lite2, threshold 0.60, API 30; decisión de aceptación registrada en technical discovery / riesgos.
2. Dominio (`DetectedFood`, `ScanDraftItem`, `Scan`, reglas de fusión del Escaneo/umbral/conversión con cantidad/resolución de coincidencias) con unit.
3. Puertos `ScanImageSource`/`FoodDetector` y adaptadores CameraX/MediaPipe; dependencias Gradle; wiring Koin.
4. UI del Escáner (`ScannerViewModel`/`ScannerScreen`/`ScannerRoute`), navegación y cadenas es/en; chips con stepper de cantidad, marcas y diálogos de coincidencia «preguntar en pantalla», guardado en Despensa y degradaciones RD05/RD06.
5. Tests (unit + E2E + adaptador real con fixture), harness completo, evidencia y cierre en `PROGRESS.md`/`feature_list.json` (`passing`).

## Implementation Tasks

- [x] T0: spike en API 30 y decisión del usuario registrada: EfficientDet-Lite2, threshold 0.60, riesgo aceptado con chips editables + RD05.
- [x] T1: `DetectedFood`, `ScanDraftItem`, `Scan` y reglas puras (`domain/scanner/`) con unit (umbral 0.60, fusión del Escaneo por nombre normalizado, nombre vacío, conversión a `PantryItem` con cantidad 0..99 —0 = sin cantidad— y resolución de coincidencias mantener/actualizar). (S1, S2, S4, S11)
- [x] T1b: límite de cantidad a 99 en toda la app (`PantryItemValidation.MAX_QUANTITY = 99`, guardias de los repositorios, stepper de la Despensa y unit/E2E de límites 0/99). (S3, D6)
- [x] T2: dependencias Gradle + `FoodDetector`/`MediaPipeFoodDetector` y `ScanImageSource`/`CameraXScanImageSource` (captura en memoria, RD06) + Koin. (S1, S8)
- [x] T3: `ScannerViewModel`/`ScannerScreen`/`ScannerRoute`: visor, obturador, inferencia en curso, HUD con etiquetas y confianza, chips editables (renombrar/quitar/añadir y stepper de cantidad 0..99, «−» deshabilitado en 0 y «+» en 99, 0 = sin cantidad). (S1, S3, S6)
- [x] T4: «Guardar en la Despensa» con cantidad de cada chip y resolución en pantalla de coincidencias (diálogo por coincidencia con «Mantener cantidad»/«Actualizar cantidad», secuencial, no persiste hasta resolver; descartar aborta); navegación a Despensa, descarte del Escaneo y confirmación al salir/re-escanear con chips. (S2, S10, S11, S12)
- [x] T5: degradaciones RD05 (permiso denegado, cero detecciones, fallo del detector) y comprobación RD06. (S5, S6, S7, S8)
- [x] T6: cadenas `scanner_*`/`food_label_*` es/en y testTags; actualizar `MyRecipesLocalizationE2ETest`. (S9)
- [x] T7: `ScanPantryE2ETest` (fakes con HUD y S1-S12), `MediaPipeFoodDetectorTest` (fixture real Lite2 y threshold 0.60), `CameraXScanImageSourceTest` (JPEG en memoria, sin archivos) y unit de ViewModel.
- [x] T8: `init.ps1`, `lintDebug`, `testDebugUnitTest`, `assembleDebug` y `connectedDebugAndroidTest` en verde; evidencia registrada; feature en `passing`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; **no arranca procesos duraderos**; los scripts no se modifican). Nota de entorno de `PROGRESS.md`: ejecutar con `cmd /c "set PATH=...&& powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`.
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `./gradlew assembleDebug` → BUILD SUCCESSFUL; `./gradlew testDebugUnitTest` incluye los unit nuevos (fusión del Escaneo por nombre normalizado, umbral 0.60, conversión con cantidad 0..99/0 = sin cantidad, resolución de coincidencias mantener/actualizar, guardado con cantidades).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en emulador API 30+ (AVD `Pixel_10_Pro`), con `ScanPantryE2ETest`:
  - verify 1 (S1): imagen inyectada por `FakeScanImageSource` + `FakeFoodDetector` → chips con nombre y confianza.
  - verify 2 (S2): confirmar → Alimentos en la Despensa con las cantidades elegidas en los chips (0 = sin cantidad; repositorio y `PantryScreen`).
  - S3 quitar/renombrar/añadir chips y stepper de cantidad («−» deshabilitado en 0, «+» en 99, sin campo de texto, 0 = sin cantidad); S4 duplicados del Escaneo fusionados en un chip; S5 permiso denegado → aviso + modo manual; S6 cero detecciones → mensaje + alta manual; S7 detector en fallo → aviso + manual; S8 sin ficheros de imagen tras el flujo (RD06: comprobar `cacheDir`/`filesDir`); S9 textos con locale `en-US` y ≥ 48 dp (incluidos los diálogos de coincidencia y de salida); S10 salir con chips sin guardar → diálogo de confirmación (cancelar conserva los chips, confirmar descarta y sale); S11 coincidencia con la Despensa → diálogo con «Mantener cantidad»/«Actualizar cantidad» y cantidades resultantes correctas, sin fila nueva; S12 varias coincidencias → preguntas una a una, nada se guarda con pendientes y descartar un diálogo aborta el guardado dejando la bandeja intacta.
  - `MediaPipeFoodDetectorTest`: fixture local banana → etiqueta alimentaria Lite2 con confianza ≥ 0.60 en API 30 y Pixel_10_Pro API 37; `CameraXScanImageSourceTest`: captura JPEG en memoria sin archivos en API 37. La versión final `tasks-vision:0.10.35` aporta ABI x86_64 y compatibilidad con páginas de 16 KiB.
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
- [ ] Los escenarios de aceptación pasan (S1-S12) y los dos `verify` de `feature_list.json` se ejecutan en el emulador; T0 registra decisión explícita de Lite2/0.60.
- [ ] Coincidencias verificadas: ninguna fusión automática silenciosa con la Despensa —se pregunta en pantalla por cada coincidencia («Mantener cantidad»/«Actualizar cantidad»)— y se conserva un solo Alimento por nombre normalizado (S11/S12).
- [ ] Cantidad de los chips verificada: stepper entero 0..99 con 0 = sin cantidad («−» deshabilitado en 0, «+» en 99, sin campo de texto) y persistencia con la cantidad elegida.
- [ ] Límite 99 aplicado en toda la app (D6): `PantryItemValidation.MAX_QUANTITY = 99`, guardias de repositorios, stepper de la Despensa («+» deshabilitado en 99) y tests de límites 0/99 actualizados; sin migración de esquema Room.
- [ ] RD06 verificado: ninguna imagen de Escaneo persistida (sin `OutputFileOptions`/ficheros; sin tabla de Escaneos).
- [ ] RD05 verificado: permiso denegado, cero detecciones y fallo del detector degradan a entrada manual con aviso, sin bloquear.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E actualizada (incluido `MyRecipesLocalizationE2ETest`) y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature pasa a `passing` solo tras `lint` + `unit` + `assembleDebug` (y E2E si procede).
- [ ] No se añadió comportamiento de producto ni trabajo de otras features.

## Clarificación — 2026-10-21 — veredicto: APTA (intento 1 de 3)

Auditoría independiente (primera pasada, sesión limpia) contra `feature_list.json`, `AGENTS.md`, `docs/build-brief.md` (líneas rojas), `docs/domain-model.md`, `docs/technical-discovery.md`, `DESIGN.md` y el código actual. Comprobado con lectura directa: `RoomPantryRepository.savePantryItem` upserta por `id` y solo rechaza `DuplicateName` cuando el `matchingItem.id` difiere (`RoomPantryRepository.kt:29-44`), así que «Actualizar cantidad» regrabando el Alimento existente con su mismo `id` es viable tal como afirma la spec; `PhotoResult` (`PhotoAcquirer.kt:5-10`) y `SystemPhotoAcquirer` (permiso `CAMERA` en runtime, `CameraPermissionDenied`) son el patrón que propone `ScanImageSource`; `PantryItemValidation` (0..99 tras la revisión 2026-10-21), `normalizeName`, manifest (`CAMERA`, `uses-feature`, `FileProvider`), `ProvisionalTestTags.SCANNER_SCREEN` en `MyRecipesLocalizationE2ETest.kt:97,114` y el patrón `FakePhotoAcquirer` de `CreateRecipeE2ETest.kt:92` coinciden con la investigación. Trazabilidad completa: los 4 `steps` y los 2 `verify` de `feature_list.json` se cubren en S1/S2/S8 y T0-T8, y cada escenario S1-S12 tiene tarea y aserción en el plan de verificación. Consistencia tras el delta 2026-10-21: `docs/domain-model.md` (entidad Escaneo con cantidad propuesta 0..99, Relaciones, Validaciones de datos, Casos límite) y el apéndice de `docs/risks-and-open-questions.md` (Q-escanear-despensa-con-camara-1..3 resueltas) coinciden con S10-S12 y con los Non-Goals. Sin trabajo de otras features (carrusel, Sugerencias, Lista de la Compra, unidades/decimales explícitamente fuera). D1 (spike RK02) y D4 quedan como gates explícitos con parada y pregunta al usuario, sin ambigüedad bloqueante.

### Hallazgos

Sin hallazgos bloqueantes. Quedan 5 observaciones sin bloqueo (no impiden implementar ni validar; se resuelven dentro de las decisiones ya tomadas por el usuario):

1. [menor · consistencia interna] Goal vs S1/Enfoque 3: el Goal dice que «la foto se descarta tras la inferencia (RD06): no se escribe a disco ni queda rastro de imagen», pero S1 y el punto 3 mantienen la imagen capturada congelada con las etiquetas HUD visibles durante todo el estado resultado, hasta «Guardar»/«Re-escanear»/salir. RD06 (`docs/domain-model.md`) es una regla de no persistencia → precisar que la imagen vive solo en memoria durante el estado resultado y se libera al confirmar/re-escanear/salir, sin persistirse nunca, para que un implementador no descarte el bitmap antes de dibujar el HUD ni un validador lo tome por rastro.
2. [menor · verificabilidad] El Then de S1 incluye «las etiquetas de detección sobre la imagen capturada», pero la verify 1 del plan de verificación y T7 solo afirman «chips con nombre y confianza» (y el stepper); las píldoras HUD solo quedan cubiertas por la captura y la revisión manual → añadir la aserción E2E de las etiquetas HUD (testTag/texto de las píldoras) a la verify 1/T7, o citar explícitamente qué evidencia las cubre.
3. [menor · caso límite] Duplicados dentro de la bandeja creados por el usuario (renombrar un chip a un nombre cuyo `normalizeName` coincide con otro chip, o «Añadir alimento» con un nombre ya presente): S4 solo cubre duplicados de detección y `docs/domain-model.md` (Validaciones de datos) manda fusionar «en un solo chip» dentro de un Escaneo mientras que en la edición manual de la Despensa se rechaza con aviso → fijar cuál de las dos reglas aplica en la bandeja (y qué cantidad/confianza gana la fusión si se fusiona) en S3/S4 y reflejarlo en T1/T7.
4. [menor · interacción] El disparador del diálogo de coincidencia queda implícito («resolver abre el diálogo»): no se dice si se abre automáticamente al detectarse la coincidencia (S11: «al generarse, renombrarse o añadirse el chip») o al tocar la marca del chip, ni si una decisión ya tomada (mantener/actualizar) se puede cambiar y cómo → explicitar el gesto y la reversibilidad, con su aserción E2E en S11/S12.
5. [menor · interacción] «Re-escanear» con chips sin guardar en la bandeja no tiene comportamiento definido: si descarta los chips iría en contra de la decisión Q-escanear-despensa-con-camara-2 (confirmar antes de descartar) → acotar en S10/Enfoque 3 (p. ej. mismo diálogo de confirmación si hay chips) y cubrirlo en T4/T7.

### Resolución de las 5 observaciones durante la implementación

1. La imagen congelada y bytes del Escaneo viven solo en memoria; se liberan al guardar, re-escanear o salir. RD06 no permite escribir `OutputFileOptions` ni crear archivos.
2. `ScanPantryE2ETest` afirma las etiquetas HUD con tags/textos (además del nombre/confianza de los chips).
3. La fusión aplica también a ediciones y altas manuales: al renombrar una colisión gana el chip editado y conserva nombre/cantidad con confianza máxima; al añadir un duplicado manual se conserva el chip existente. Queda fijado en S3/S4 y las reglas puras.
4. Una coincidencia abre el diálogo automáticamente; tocar la marca lo reabre para cambiar la decisión. E2E cubre la apertura y el cambio Keep/Update.
5. «Re-escanear» con chips pendientes usa la confirmación de S10; cancelar conserva bandeja, confirmar la descarta y captura otra imagen. E2E cubre ambos pasos.

### Siguiente acción

Continuar con T1-T8: T0 quedó cerrado por decisión explícita del usuario (EfficientDet-Lite2, umbral 0.60). Las cinco observaciones de redacción se resuelven de oficio durante esta implementación, sin abrir un nuevo ciclo de preguntas.

### Resolución de las 5 observaciones (feature-implementer, 2026-10-21)

1. **Resuelta**: la imagen congelada y los bytes del Escaneo viven solo en memoria hasta guardar, re-escanear o salir; nunca se serializan ni escriben a disco (RD06).
2. **Resuelta**: `ScanPantryE2ETest` afirma las etiquetas HUD por testTag/texto, además del nombre y confianza de los chips.
3. **Resuelta**: S3/S4 y RD12 fijan las colisiones de bandeja. Al renombrar gana el chip editado (conserva nombre/cantidad y toma confianza máxima); al añadir manualmente un duplicado se conserva el chip existente; detecciones repetidas usan nombre más legible/confianza máxima.
4. **Resuelta**: una coincidencia abre el diálogo automáticamente; tocar la marca lo reabre para cambiar «Mantener»/«Actualizar». S11/S12 verifican resolución y reversibilidad.
5. **Resuelta**: «Re-escanear» con chips pendientes reutiliza la confirmación de S10; cancelar conserva la bandeja y confirmar la descarta y captura otra imagen. Cubierto por `ScanPantryE2ETest`.
