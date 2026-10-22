# Feature Implementation Spec: Detectar alimentos en vivo

## Source Feature

- `id`: detectar-alimentos-en-vivo
- `area`: funcional
- `depends_on`: escanear-despensa-con-camara (`accepted`)
- `status`: `not_started` en el momento de planificar
- `source`: `feature_list.json`

## Goal

Sustituir el flujo de captura congelada del Escáner por **detección continua sobre el visor**: mientras el usuario enfoca con la cámara, MediaPipe va detectando Alimentos **en el dispositivo** (sin red, sin persistir fotogramas — RD06) y la app va construyendo **bajo el visor un listado sin repetidos**: cada Alimento entra **una sola vez por nombre normalizado** y su confianza se actualiza con cada nueva detección. El listado sigue siendo editable (renombrar, quitar, cantidad con stepper **0..99**, alta manual) y se guarda en la Despensa igual que hoy.

Lo duradero son solo los **Alimentos** de la Despensa (Room, `PantryRepository` sin cambios). El **Escaneo** sigue siendo temporal (detecciones con confianza, en memoria, hasta confirmar el listado) y **ningún fotograma se escribe a disco** (RD06).

## Non-Goals

- Carrusel de Recetas compatibles ni Sugerencias (`mostrar-recetas-compatibles`, `sugerir-recetas-con-ia`).
- Faltantes hacia la Lista de la Compra (`anadir-faltantes-a-la-lista`).
- **Sumar cantidades al guardar**: el diálogo de coincidencia con la Despensa («Mantener cantidad»/«Actualizar cantidad») se conserva **tal cual**; la suma es `sumar-cantidades-al-guardar` y no se toca aquí.
- Cambiar el modelo, el vocabulario (10 clases COCO) ni el umbral de confianza (RD12, 0.60): EfficientDet-Lite2 queda fijado.
- Cambiar `PantryRepository`, el esquema Room (v2) ni las pantallas Despensa/Mis Recetas.
- Grabar vídeo, guardar fotogramas, detección en la nube o historial de Escaneos (RD06, RGPD).
- Cambiar de cámara, usar la galería, calibrar el umbral o editar cajas de detección.

## Job Story

When tengo la despensa delante y quiero cargarla sin teclear ni pulsar el obturador,
I want to ir enfocando con la cámara mientras la app detecta y acumula los alimentos que veo,
so I can tener el listado listo para corregir y guardar en segundos.

## Users And Permissions

- **Usuario** (único rol, propietario del dispositivo, `P31`): usa el Escáner. Requiere permiso `CAMERA` en runtime (ya declarado); sin él, degrada a entrada manual sin bloquear (RD05, mismo criterio que RF-23 de `crear-receta` y S5 de `escanear-despensa-con-camara`).

## Acceptance Scenarios

### Scenario 1: Detección en vivo sin repetidos (verify 1 de `feature_list.json`)

Given el Escáner abierto con permiso concedido y una fuente de fotogramas inyectada por fakes que entrega dos fotogramas consecutivos con el mismo Alimento (p. ej. «banana» 0.72 y «banana» 0.83)
When el flujo en vivo procesa ambos fotogramas
Then el listado muestra **un solo** ítem «banana» con confianza 0.83 (la mejor observada), sin duplicados

### Scenario 2: Alta manual con cantidad y guardado (verify 2 de `feature_list.json`)

Given el listado en vivo con detecciones en curso
When el usuario añade a mano «Tomate», ajusta su cantidad con el stepper a 3 y pulsa «Guardar en la Despensa»
Then la Despensa contiene «Tomate» con cantidad 3 (y los demás Alimentos con la cantidad elegida; 0 = sin cantidad) y el Escaneo queda descartado

### Scenario 3: El listado se construye acumulando (no se vacía solo)

Given un Alimento ya detectado y presente en el listado
When el Alimento desaparece de la escena (deja de detectarse en fotogramas posteriores)
Then el ítem **permanece** en el listado con su nombre, su confianza y su cantidad; solo el usuario lo quita («×») o vacía el listado

### Scenario 4: Edición del listado durante la detección (RD05 parcial)

Given detecciones en vivo añadiendo ítems
When el usuario renombra un ítem hacia otro existente, lo quita o cambia su cantidad (stepper 0..99)
Then la lista refleja los cambios al instante, los renombrados hacia duplicado se fusionan (gana el editado, confianza máxima, `docs/domain-model.md`) y la detección en vivo sigue corriendo sin pisar las ediciones del usuario (nombre y cantidad nunca se sobrescriben; solo la confianza)

### Scenario 5: Confianza actualizada al volver a detectar

Given un ítem ya en el listado con confianza 0.70
When fotogramas posteriores lo detectan con 0.66 y después con 0.81
Then su confianza queda en 0.81 (máximo observado; regla de fusión existente «confianza máxima»)

### Scenario 6: Permiso de cámara denegado (RD05)

Given el permiso `CAMERA` denegado o revocado
When el usuario abre el Escáner
Then se muestra un aviso y el flujo continúa en modo manual (alta por chips) sin visor y sin bloquear

### Scenario 7: Fallo del detector en vivo (RD05)

Given el detector en error (modelo no cargado, fotograma ilegible…)
When una inferencia falla
Then se muestra un aviso, se detiene el muestreo (sin reintentos en bucle) y el usuario puede seguir añadiendo a mano; «Re-escanear» reinicia la detección en vivo

### Scenario 8: Ningún fotograma persiste (RD06)

Given un flujo en vivo con detecciones y un guardado completado
When se revisa `cacheDir`/`filesDir` y el esquema Room tras el flujo
Then no existe ninguna imagen de Escaneo en disco ni tabla de Escaneos: solo los Alimentos en Room

### Scenario 9: Salir o vaciar con Alimentos sin guardar

Given ítems en el listado sin guardar
When el usuario pulsa «atrás» (o «Re-escanear» para empezar otro Escaneo)
Then se muestra el diálogo de confirmación existente («Cancelar» conserva el listado; confirmar descarta el Escaneo temporal y sale o vacía, según el disparador)

### Scenario 10: Textos es/en y objetivos táctiles

Given la app en español o en inglés
When se usa el Escáner en vivo (visor, HUD, listado, avisos, diálogos)
Then todos los textos visibles cambian con el locale y las acciones tienen área táctil ≥ 48 dp

### Scenario 11: El visor no se congela (rendimiento)

Given la detección en vivo corriendo
When el usuario mueve la cámara y se producen inferencias
Then el visor sigue fluido (la inferencia nunca bloquea el hilo de UI) y el listado refleja una detección nueva en ≤ 1 s desde que el Alimento entra en escena (objetivo de la decisión D2)

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `detectar-alimentos-en-vivo` (steps, verification, notes: sustituye el flujo congelado; RD06 vigente; el muestreo, la conflación y el objetivo de rendimiento los fija esta spec); dependencia `escanear-despensa-con-camara` en `accepted` (PR #12).
- `PROGRESS.md` — entorno (AVD `Pixel_10_Pro` API 37, scripts con `cmd /c` + `JAVA_HOME` corregido), reglas de evidencia y cierre.
- `docs/domain-model.md` — Alimento (cantidad entera 0..99; 0 = sin cantidad), Escaneo temporal («sus resultados viven solo hasta confirmar los chips»), fusión por nombre normalizado con «confianza máxima» y «gana el chip editado», cantidad propuesta 0..99 con stepper, RD05, RD06, RD12 (umbral 0.60).
- `docs/technical-discovery.md` — MediaPipe `tasks-vision:0.10.35` + EfficientDet-Lite2 (7.557.887 bytes, SHA-256 verificado), umbral 0.60, diez clases COCO `food_label_*`, CameraX 1.5.3, flujo `Camara → MediaPipe → Chips → Despensa`, latencia objetivo PRD < 800 ms.
- `docs/risks-and-open-questions.md` — RK02 mitigado (aceptación de Lite2 + 0.60); sin riesgos nuevos previstos para detección en vivo salvo rendimiento (se registra en D2).
- `docs/build-brief.md`, `CONTEXT.md` — línea roja RGPD (fotos de escaneo sin almacenar), términos «Escáner»/«Escaneo»/«Despensa».
- `DESIGN.md` — «Escáner de la Despensa»: visor, HUD con etiquetas y confianza, chips editables; chips confirmados verde salvia; accesibilidad es/en.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt` — puerto `ScanImageSource.capture()` (`ScanCaptureResult.Success(bytes) | CameraPermissionDenied | Cancelled | Failed`) y `CameraPreviewController` (bind/unbind del visor).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/CameraXScanImageSource.kt` — adaptador CameraX: `Preview` + `ImageCapture` (`CAPTURE_MODE_MINIMIZE_LATENCY`), JPEG copiado a memoria con `ImageProxy.close()` garantizado, **sin `OutputFileOptions`** (RD06).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/FoodDetector.kt`, `MediaPipeFoodDetector.kt` — puerto `FoodDetector.detect(ByteArray)` y adaptador MediaPipe (`RunningMode.IMAGE`, `setScoreThreshold(0f)`, filtro `FOOD_LABELS` de 10 clases, caja normalizada `DetectionBounds`, `bitmap.recycle()` en `finally`).
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — reglas puras: `MIN_CONFIDENCE = 0.60`, `eligible`, `toDraftItems` (fusión por `normalizeName`, confianza máxima), `mergeDraftItems` («confidence is always the best available score»), `rename`, `updateQuantity` (0..99), `matchesWithPantry`, `resolveDuplicate`, `toPantryItem` (0 → `null`).
- `app/src/main/java/com/whatsforlunch/app/domain/model/ScanDraftItem.kt`, `Scan.kt`, `DetectedFood.kt` — `ScanDraftItem(name, confidence, quantity=0, id)`, `Scan(createdAt, detections)`, `DetectedFood(name, confidence, boundingBox)`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerRoute.kt`, `ScannerScreen.kt`, `ScannerViewModel.kt` — Route construye `CameraXScanImageSource(context, lifecycleOwner)` y cablea `viewModel.capture(imageSource)`; `ScannerTestTags` (`CAMERA_PREVIEW`, `CAPTURED_IMAGE`, `HUD_LABEL_PREFIX`, `CAPTURE`, `CAPTURE_PROGRESS`, `FOOD_LIST`, `FOOD_ITEM`, `ITEM_*`, `MANUAL_*`, `SAVE`, `RESCAN`, `EXIT_*`, `DUPLICATE_*`); estado `capturedImage`, `isCapturing`, `isDetecting`, `manualMode`; diálogos de salida y de duplicado ya implementados.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `single<FoodDetector> { MediaPipeFoodDetector(androidContext()) }`; el `ScanImageSource` se construye en la Route.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `scanner_*` (incluidas `scanner_rescan*`, `scanner_detection_loading`, `scanner_camera_unavailable`) y `food_label_*` es/en.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt`, `ui/scanner/ScannerViewModelTest.kt` — unit existentes de umbral, fusión, stepper y ViewModel (con `FakeFoodDetector`/`InMemoryPantryRepository` + `coroutines-test`).
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — E2E actual (15 casos) con `FakeScanImageSource`/`FakeFoodDetector` privados; cubre captura → chips → Despensa, stepper 0..99, duplicados, diálogos, RD06 (`capturedImageIsNeverWrittenToCacheOrFiles`), localización y 48 dp.
- `app/src/androidTest/java/com/whatsforlunch/app/CameraXScanImageSourceTest.kt` — instrumentado de la captura JPEG en memoria (sin ficheros).
- `app/src/androidTest/java/com/whatsforlunch/app/MediaPipeFoodDetectorTest.kt` — adaptador real Lite2 con fixture local (1/1 en API 30 y 37); **no cambia**.
- `docs/specs/escanear-despensa-con-camara.md` — spec aceptada previa (estilo, D2/D3/D5, patrón de fakes E2E); su Non-Goal «no hay detección en vivo sobre el preview» es justamente lo que esta feature sustituye.

### Existing Patterns To Follow

- MVVM + StateFlow: `*Route` (cablea adaptadores del sistema) → `*Screen` (sin estado) → `*ViewModel` con `uiState`; reglas de negocio puras en `domain/`; Koin `viewModel {}`.
- Puertos en `data/` con implementación real y fake inyectable (`FoodDetector`, `ScanImageSource`): la UI no conoce MediaPipe ni CameraX.
- testTags por pantalla con prefijos por ítem (`itemQuantity(id)`…); diálogos con tags propios; `contentDescription`; objetivos ≥ 48 dp.
- Cadenas con prefijo `scanner_*`/`food_label_*` duplicadas en `values/` y `values-en/`; identificadores de código en inglés (`P19`, `P25`).
- E2E Compose con locale inyectado, `InMemory*Repository` y fakes deterministas (`CreateRecipeE2ETest`, `ScanPantryE2ETest`); el adaptador real se valida aparte (`MediaPipeFoodDetectorTest`, `CameraXScanImageSourceTest`).
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se modifican.

### Current Gaps

- No existe fuente de fotogramas en vivo: `ScanImageSource` solo captura una imagen a petición; CameraX hoy no tiene caso de uso `ImageAnalysis`.
- No hay muestreo ni conflación: el flujo actual dispara **una** inferencia por pulsación de obturador.
- `ScanDetections` fusiona lotes de detecciones (`toDraftItems`) pero no acumula detecciones nuevas sobre una lista ya editada por el usuario (sin pisar nombre/cantidad).
- El emulador no permite inyectar una escena de comida real en la cámara: la E2E debe inyectar fotogramas/detecciones por fakes (limitación ya registrada en la validación anterior).

## Technical Approach

1. **Puerto de fotogramas** (`data/scanner/`): nuevo `LiveFrameSource` con `fun frames(): Flow<ByteArray>` (fotogramas JPEG **en memoria**, nunca a disco — RD06) más el `CameraPreviewController` existente para bind/unbind del visor. Implementación `CameraXScanImageSource` **modificada**: añade `ImageAnalysis` con `STRATEGY_KEEP_ONLY_LATEST` (conflación a nivel de cámara: solo el último fotograma; los intermedios se descartan), resolución de análisis modesta (~640×480) y conversión `ImageProxy` (YUV) → JPEG en memoria (`bitmap.recycle()` siempre; sin `OutputFileOptions`). El `ImageCapture` y `ScanImageSource.capture()` **se retiran** (el obturador desaparece con el flujo congelado). En tests, `FakeLiveFrameSource` emite una lista de fotogramas determinista.
2. **Detector**: `FoodDetector.detect(ByteArray)` y `MediaPipeFoodDetector` **sin cambios** (`RunningMode.IMAGE`: cada fotograma muestreado es una inferencia independiente; no se necesita `VIDEO`/`LIVE_STREAM` ni timestamps — decisión D4). El umbral RD12 (0.60) y el filtro de 10 clases siguen igual.
3. **Dominio** (`domain/scanner/ScanDetections.kt`): nueva regla pura `mergeLiveDetections(items: List<ScanDraftItem>, detections: List<DetectedFood>)` que aplica `eligible` (umbral 0.60) y para cada detección: si ya existe un ítem con el mismo `normalizeName` → actualiza **solo la confianza** al máximo observado (nombre, cantidad y ediciones del usuario intactos); si no → añade `ScanDraftItem(name, confidence, quantity = 0)`. Constante `LIVE_SAMPLE_INTERVAL_MS = 300` en el mismo objeto (muestreo). Unit-testable sin Android.
4. **ViewModel** (`ui/scanner/ScannerViewModel.kt`): `capture(imageSource)` se sustituye por `startLiveDetection(frameSource)` / `stopLiveDetection()`. Bucle: recolectar `frames()` con conflación (`conflate()` + bandera «inferencia en curso») y **muestreo mínimo de 300 ms** entre inferencias (D2); cada fotograma procesado → `foodDetector.detect` → `ScanDetections.mergeLiveDetections` sobre los ítems actuales. **El bucle vive en corrutinas**: `startLiveDetection` lanza la recolección con `viewModelScope.launch`, la inferencia es la `suspend fun` existente de `FoodDetector` ejecutada en `Dispatchers.Default` (ya lo hace `MediaPipeFoodDetector`, fuera del hilo de UI) y `stopLiveDetection()` (y el `onCleared` del ViewModel) cancela la corrutina al salir del Escáner; el muestreo se implementa con `delay(300.ms)` entre inferencias o `sample(300.ms)`, a criterio del implementador (D1). El estado guarda además `liveDetections` (detecciones del último fotograma procesado, con `boundingBox` normalizado) para el HUD. Las ediciones del usuario (`renameItem`, `updateQuantity`, `removeItem`, `addManualItem`) conservan su semántica actual y nunca se sobrescriben con detecciones. `FoodDetectionResult.Failed` → aviso `DETECTION_FAILED`, se detiene el muestreo y se activa el modo manual (D5); «Re-escanear» reinicia el Escaneo (vacía el listado y reanuda el muestreo) con la confirmación existente si hay ítems sin guardar.
5. **UI** (`ui/scanner/ScannerScreen.kt`): el visor vivo (`CAMERA_PREVIEW`, `previewContent` existente) es siempre visible con el **HUD de detecciones en vivo** encima (cajas + etiquetas píldora «nombre · NN%» en coordenadas normalizadas del fotograma analizado, reutilizando `HUD_LABEL_PREFIX`); desaparecen `CAPTURE`, `CAPTURED_IMAGE` y el flujo de captura congelada; `CAPTURE_PROGRESS` se conserva como indicador discreto de «detectando…» mientras hay inferencia en curso. Debajo del visor, el listado existente (`FOOD_LIST`/`FOOD_ITEM` con stepper 0..99, «×», renombrar, marca de coincidencia), el alta manual (`MANUAL_*`) y las acciones «Guardar en la Despensa» (`SAVE`) y «Re-escanear» (`RESCAN`, ahora = vaciar listado/reiniciar Escaneo). Diálogos `EXIT_*` y `DUPLICATE_*` sin cambios. Con fakes, el área de visor sigue siendo un placeholder inerte.
6. **Persistencia**: sin cambios — al confirmar, cada ítem pasa por `PantryRepository.savePantryItem` con la cantidad elegida (0 → `null`), el diálogo de coincidencia «Mantener cantidad»/«Actualizar cantidad» se conserva tal cual (no sumar: Non-Goal) y el Escaneo temporal se descarta.
7. **Degradación (RD05)**: permiso denegado → aviso + modo manual; fallo del detector → aviso + modo manual + muestreo detenido; listado vacío → alta manual siempre disponible. Nunca se bloquea el flujo.
8. **Tests**: unit sobre `mergeLiveDetections` (sin repetidos, confianza máxima, no pisar ediciones, umbral, alta de nuevos) y sobre `ScannerViewModel` en vivo con `FakeLiveFrameSource` + `FakeFoodDetector` por secuencia + `coroutines-test` (verify 1 y 2 a nivel de ViewModel, degradaciones, confirmación al salir). E2E: `ScanPantryE2ETest` **modificado** (los fakes y los casos del flujo congelado se adaptan al flujo en vivo) con los dos `verify` como casos nuevos; `CameraXScanImageSourceTest` **adaptado** a la fuente de fotogramas (JPEG en memoria, sin ficheros); `MediaPipeFoodDetectorTest` sin cambios; regresión completa obligatoria.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/data/scanner/LiveFrameSource.kt` — crear; puerto de fotogramas en vivo (`frames(): Flow<ByteArray>`) con la semántica RD06 documentada.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt` — modificar; retirar `ScanImageSource.capture()`/`ScanCaptureResult` (flujo congelado sustituido) y mantener `CameraPreviewController` (o moverlo a `LiveFrameSource.kt`).
- `app/src/main/java/com/whatsforlunch/app/data/scanner/CameraXScanImageSource.kt` — modificar; `ImageAnalysis` (KEEP_ONLY_LATEST, ~640×480, JPEG en memoria) en lugar de `ImageCapture`; conservar permisos, cierre de `ImageProxy` y ausencia total de ficheros.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/FoodDetector.kt`, `MediaPipeFoodDetector.kt` — sin cambios previstos.
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — modificar; `mergeLiveDetections` + `LIVE_SAMPLE_INTERVAL_MS = 300`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — modificar; bucle de detección en vivo (muestreo + conflación), `liveDetections`, «Re-escanear» = reiniciar Escaneo; retirar `capture()`/`isCapturing`/`capturedImage`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt`, `ScannerRoute.kt` — modificar; HUD en vivo sobre el visor, sin obturador ni imagen congelada, indicador «detectando…», cableado `startLiveDetection`/`stopLiveDetection`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar solo si el cableado del ViewModel cambia (hoy el adaptador se construye en la Route).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — modificar; cadenas nuevas (`scanner_live_*` para el estado de detección/hint si procede) y ajuste de `scanner_rescan*` al nuevo significado (vaciar/reiniciar); `food_label_*` sin cambios.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt`, `ui/scanner/ScannerViewModelTest.kt` — modificar; unit del flujo en vivo (nombres de test en inglés).
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — modificar; fakes `FakeLiveFrameSource`/`FakeFoodDetector` por secuencia, casos del flujo congelado adaptados y los dos `verify` en vivo.
- `app/src/androidTest/java/com/whatsforlunch/app/CameraXScanImageSourceTest.kt` — modificar; valida la fuente de fotogramas (JPEG en memoria, sin ficheros) en lugar del obturador.
- `app/src/main/java/com/whatsforlunch/app/ui/nav/AppNavHost.kt` — sin cambios previstos (la ruta `scan_pantry` → `ScannerRoute` ya existe).
- `docs/domain-model.md` — modificar; el Escaneo pasa a «detección continua sobre el visor, acumulada en el listado (un Alimento por nombre normalizado, confianza = máximo observado)» y se fija el matiz de que las detecciones no sobrescriben ediciones del usuario.
- `docs/technical-discovery.md` — modificar; muestreo (300 ms), conflación (KEEP_ONLY_LATEST + conflate), `RunningMode.IMAGE` por fotograma, objetivo de rendimiento (D2) y retirada del obturador.
- `docs/risks-and-open-questions.md` — modificar solo para registrar las preguntas `Q-detectar-alimentos-en-vivo-N` que se abran durante la implementación (D1-D8 son decisiones de esta spec, revisables en validación).

Los paths de `ui/scanner/`, `data/scanner/` y `domain/scanner/` son los previstos; el implementador puede ajustar nombres dentro del patrón de capas existente (p. ej. renombrar `CameraXScanImageSource` a `CameraXLiveFrameSource`).

## Visual Design Impact

- UI involved: yes
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); `uxdocs/chefencasa_esc_ner_de_nevera_ia/` como inspiración (`P16`), con la misma laguna conocida (el material no cubre HUD en vivo ni estados de fallo).
- Screens or states affected: «Escáner» — visor en vivo con HUD de detecciones (cajas + etiquetas «nombre · NN%»), listado acumulado bajo el visor, indicador «detectando…», alta manual, modo manual (permiso denegado / fallo del detector), diálogos de salida y duplicado (sin cambios); Despensa como destino tras guardar (sin cambios).
- New design artifact required: no — componentes M3 y tokens existentes; el HUD sobre el visor ya está descrito en `DESIGN.md` («HUD con etiquetas y confianza»).
- Criterios: fondos y roles de color iguales que en `escanear-despensa-con-camara` (superficie `#FBF9F5`, acción primaria terracota `#A83212`, chips confirmados verde salvia, avisos `tertiary`/`error`); etiquetas HUD píldora con velo para contraste WCAG AA sobre el vídeo; objetivos táctiles ≥ 48 dp; todo es/en.
- Laguna explícita (RK04): las cajas del HUD se dibujan en coordenadas normalizadas del fotograma analizado y se escalan al área del visor; con `PreviewView` de tipo `FIT_CENTER` puede haber desajuste si la relación de aspecto del análisis difiere de la del visor — el implementador debe escalar respecto al área real del preview y la validación visual revisa el resultado sin cambiar comportamiento.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe; las capas `ui`/`domain`/`data` y la dirección de dependencias no cambian (solo cambia el adaptador CameraX de captura puntual a flujo de fotogramas, dentro del patrón de puertos ya usado).
- `CONSTRAINTS.md`: not needed — no existe y no se introducen reglas MUST/MUST NOT nuevas (RD05/RD06/RD12 siguen en `docs/domain-model.md`).
- `AGENTS.md`: not needed — sin cambios de flujo de agentes, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — update (Escaneo en vivo acumulado; detecciones no pisan ediciones); `docs/technical-discovery.md` — update (muestreo, conflación, modo IMAGE, objetivo de rendimiento); `docs/risks-and-open-questions.md` — update solo si se abren `Q-detectar-alimentos-en-vivo-N`.

## Dudas abiertas y desconocidos (Unknowns)

Decisiones fijadas por esta spec porque `feature_list.json` las delega explícitamente («se fijan en la spec») o porque el prototipo no las cubre; son revisables en la validación sin cambiar el comportamiento de las features vecinas:

- **D1 · Muestreo**: máximo **una inferencia cada 300 ms** (`ScanDetections.LIVE_SAMPLE_INTERVAL_MS`); constante única, fácil de retocar tras medir en el emulador.
- **D2 · Conflación y objetivo de rendimiento**: doble conflación (CameraX `STRATEGY_KEEP_ONLY_LATEST` + `conflate()`/bandera en el ViewModel): nunca más de un fotograma pendiente y siempre el más reciente; la inferencia corre fuera del hilo de UI. Objetivo declarado: inferencia por fotograma ≤ 800 ms (objetivo PRD), visor nunca bloqueado y listado actualizado en ≤ 1 s desde que el Alimento entra en escena. Si en el emulador no se alcanza, **parar y preguntar al usuario** (subir el intervalo / bajar resolución de análisis / revisar el modelo) antes de tocar el umbral RD12.
- **D3 · Actualización de confianza**: **máximo observado** (coherente con la regla de fusión existente «confianza siempre la mejor disponible» de `ScanDetections.mergeDraftItems` y «confianza máxima» de `docs/domain-model.md`); una detección posterior peor no la reduce.
- **D4 · Modo MediaPipe**: se mantiene `RunningMode.IMAGE` (cada fotograma muestreado = una llamada `detect`); no se usa `VIDEO` ni `LIVE_STREAM` porque el muestreo de D1 no aprovecha más y el puerto `FoodDetector` sigue intacto y fakeable. Si el rendimiento lo exige, el cambio a `VIDEO` es interno al adaptador.
- **D5 · Fallo del detector en vivo**: aviso + modo manual + **muestreo detenido** (sin reintentos automáticos en bucle); «Re-escanear» reinicia la detección.
- **D6 · Acumulación permanente**: los ítems no se eliminan solos cuando el Alimento desaparece de la escena (S3); el listado es la memoria del Escaneo y solo el usuario lo edita.
- **D7 · «Re-escanear»**: pasa de «volver a capturar» a «vaciar el listado y reiniciar el Escaneo» (mantiene el diálogo de confirmación con ítems sin guardar, S9). Retirada del obturador: el flujo congelado desaparece por completo (la nota de `feature_list.json` lo declara «sustituido»).
- **D8 · Renombrar y volver a detectar**: la fusión es por nombre normalizado **del momento**; si el usuario renombra «banana» a «Plátano» y el detector vuelve a proponer «banana», se crea un ítem nuevo (el renombrado ya no comparte nombre normalizado) y puede fusionarse manualmente. La alternativa (memorizar la etiqueta de origen del detector) queda fuera de alcance.

## Implementation Plan

1. Dominio: `mergeLiveDetections` + `LIVE_SAMPLE_INTERVAL_MS` con unit (S1, S3, S5).
2. Puerto `LiveFrameSource` + adaptador CameraX `ImageAnalysis` (fotogramas JPEG en memoria, KEEP_ONLY_LATEST); retirar el obturador; adaptar `CameraXScanImageSourceTest` (S8).
3. `ScannerViewModel` en vivo (muestreo, conflación, `liveDetections`, «Re-escanear» = reiniciar) + unit con fakes por secuencia (S1-S7, S9).
4. `ScannerScreen`/`ScannerRoute`: HUD en vivo sobre el visor, listado bajo el visor, indicador «detectando…», cadenas es/en (S10, S11).
5. E2E (`ScanPantryE2ETest` adaptado con los dos `verify`), harness completo, evidencia y cierre en `PROGRESS.md`/`feature_list.json` (`passing`).

## Implementation Tasks

- [ ] T1: `ScanDetections.mergeLiveDetections` + `LIVE_SAMPLE_INTERVAL_MS = 300` con unit (sin repetidos, confianza máxima, no pisar ediciones, umbral 0.60, ítem nuevo con cantidad 0). (S1, S3, S5)
- [ ] T2: `LiveFrameSource` + `CameraXScanImageSource` con `ImageAnalysis` (JPEG en memoria, KEEP_ONLY_LATEST, ~640×480, RD06) y retirada de `capture()`/`ImageCapture`; adaptar `CameraXScanImageSourceTest`. (S8)
- [ ] T3: `ScannerViewModel`: `startLiveDetection`/`stopLiveDetection` con muestreo 300 ms y conflación, `liveDetections` para el HUD, fallo → aviso + modo manual + muestreo detenido, «Re-escanear» reinicia el Escaneo con confirmación. (S3, S6, S7, S9)
- [ ] T4: `ScannerScreen`/`ScannerRoute`: HUD en vivo (cajas + etiquetas) sobre `CAMERA_PREVIEW`, sin obturador ni imagen congelada, indicador «detectando…», listado/stepper/manual intactos; cadenas `scanner_*` es/en. (S1, S2, S4, S10, S11)
- [ ] T5: unit de `ScannerViewModel` en vivo con `FakeLiveFrameSource`/`FakeFoodDetector` por secuencia (verify 1 y 2 a nivel de ViewModel, ediciones no pisadas, degradaciones). (S1-S7)
- [ ] T6: `ScanPantryE2ETest` adaptado al flujo en vivo con los dos `verify` de `feature_list.json` y los casos existentes re-mapeados (permiso, fallo, RD06, diálogos, localización, 48 dp). (S1-S10)
- [ ] T7: `./init.ps1`, `./lint.ps1`, `./unit.ps1`, `./e2e.ps1`, `./gradlew assembleDebug` en verde, regresión completa sin romper suites ajenas; evidencia capturada; feature en `passing`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate estándar no bloqueante; **no arranca procesos duraderos**; los scripts no se modifican). Nota de `PROGRESS.md`: ejecutar con `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& set PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%LOCALAPPDATA%\Android\Sdk\emulator;%PATH%&& set PKG=com.whatsforlunch.app&& powershell -NoProfile -ExecutionPolicy Bypass -File <script>.ps1 > .harness\<x>.log 2>&1"`.
- `./lint.ps1` / `./unit.ps1` → exit 0; `./gradlew assembleDebug` → BUILD SUCCESSFUL; `./gradlew testDebugUnitTest --rerun-tasks` incluye los unit nuevos de `mergeLiveDetections` y del ViewModel en vivo.
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`) en el AVD `Pixel_10_Pro` (API 37), con `ScanPantryE2ETest` (fakes `FakeLiveFrameSource`/`FakeFoodDetector` por secuencia):
  - verify 1 (S1): dos fotogramas con el mismo Alimento → **un solo** ítem en el listado con la mejor confianza.
  - verify 2 (S2): alta manual «Tomate» + stepper a 3 → «Guardar en la Despensa» → Despensa con «Tomate» cantidad 3.
  - S3 el ítem permanece al dejar de detectarse; S4 renombrar/quitar/stepper en caliente sin que la detección pise ediciones; S5 confianza = máximo observado; S6 permiso denegado → aviso + modo manual; S7 fallo del detector → aviso + manual + muestreo detenido (y «Re-escanear» lo reanuda); S8 tras el flujo no hay imágenes en `cacheDir`/`filesDir` ni tabla de Escaneos (RD06); S9 diálogos de salida/reinicio con ítems sin guardar (cancelar conserva, confirmar descarta); S10 locale `en-US` y objetivos ≥ 48 dp.
  - `CameraXScanImageSourceTest` (adaptado): la fuente de fotogramas entrega JPEG en memoria y no escribe ficheros.
  - `MediaPipeFoodDetectorTest` (sin cambios): adaptador real Lite2 con fixture → ≥ 1 detección ≥ umbral.
  - Regresión completa en verde sin romper `AppLaunchE2ETest`, `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `DeleteRecipeE2ETest`, `SearchAndFilterRecipesE2ETest`, `PantryManagementE2ETest`, `PantryLocalizationE2ETest`, `MatchBadgeE2ETest`, `RecipeDetailE2ETest`, `MyRecipesLocalizationE2ETest`.
- Comprobación manual del validador (emulador/dispositivo con escena virtual de cámara): visor fluido durante la detección, HUD en vivo coherente con lo enfocado, listado que acumula sin repetidos y latencia por inferencia ≤ 800 ms medida o estimada con el log de tiempos (objetivo D2; si no se alcanza: revisión con el usuario, no se toca RD12 por cuenta propia). Misma limitación conocida que en la validación anterior: el emulador no permite inyectar comida de forma determinista por la cámara real, así que las etiquetas HUD se prueban con fakes y se comprueban a mano con escena virtual.
- Cobertura E2E: sí se mantiene y se adapta, porque la feature cambia un flujo de usuario observable (captura congelada → detección en vivo) y depende de permisos; la cadena completa se prueba con fakes (mismo criterio que `crear-receta` y `escanear-despensa-con-camara`), dejando al adaptador real sus tests instrumentados.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/detectar-alimentos-en-vivo.md` — informe con `lint`/`unit`/`e2e` (nombres de test), recorrido de S1-S11, los dos `verify` ejecutados en el emulador, comprobación RD06 (sin fotogramas persistidos) y medida/estimación de la latencia (D2); se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/detectar-alimentos-en-vivo/` — hasta 2 capturas en español (visor con HUD en vivo y listado acumulado; Despensa con los Alimentos guardados, o estado de degradación).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas; el log de `e2e` referenciado si aporta).

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (sin carrusel, sin Sugerencias, sin Lista de la Compra, **sin sumar cantidades al guardar**, sin tocar Room v2, `PantryRepository` ni el modelo/umbral RD12).
- [ ] El flujo de captura congelada está sustituido (sin obturador, sin imagen congelada) y las decisiones D1-D8 se respetan o quedan documentadas como desviación acordada.
- [ ] Los escenarios de aceptación pasan (S1-S11) y los dos `verify` de `feature_list.json` se ejecutan en el emulador.
- [ ] RD06 verificado: ningún fotograma persistido (sin `OutputFileOptions`/ficheros; sin tabla de Escaneos).
- [ ] RD05 verificado: permiso denegado y fallo del detector degradan a entrada manual con aviso, sin bloquear.
- [ ] Rendimiento (D1/D2): muestreo 300 ms y conflación aplicados; el visor no se congela; si el objetivo de latencia no se cumple, hay decisión explícita del usuario registrada.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E adaptada (incluidos los fakes por secuencia y `CameraXScanImageSourceTest`) y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature pasa a `passing` solo tras `lint` + `unit` + `assembleDebug` + `e2e`.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular `sumar-cantidades-al-guardar`).
