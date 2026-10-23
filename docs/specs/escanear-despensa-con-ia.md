# Feature Implementation Spec: Escanear la Despensa con IA

## Source Feature

- `id`: escanear-despensa-con-ia
- `area`: funcional
- `depends_on`: detectar-alimentos-en-vivo (`accepted`), sumar-cantidades-al-guardar (`accepted`), configurar-acceso-a-ia (`accepted`)
- `status`: not_started
- `source`: `feature_list.json`

> **Decisiones de producto cerradas antes de la spec** (2026-10-22, usuario): fotos sucesivas acumulan fusionando por nombre, cantidades inválidas de la IA → «sin cantidad» + recorte a 99, y se retiran confianza y HUD. Registradas como `Q-escanear-despensa-con-ia-1..3` en `docs/risks-and-open-questions.md` y formalizadas como D1-D3 más abajo.
>
> **Nota de revisión (2026-10-23, sesión 63)**: se revisa la jerarquía visual del Escáner. La entrada manual pasa a ser el contenido principal y el visor CameraX se despliega bajo demanda con «Escanear con la cámara»; el permiso de cámara se solicita solo al desplegar y el panel se pliega con animación al terminar **cualquier** análisis (éxito, fallo, sin API key o sin alimentos). Se retira el botón «Re-escanear» y cada chip conserva su borrado individual con icono terracota. Decisiones registradas como `Q-escanear-despensa-con-ia-4..5`; el resto de D1-D8 no cambia.

## Goal

El Escáner sustituye la detección MediaPipe en vivo por un **Escaneo con IA**: el usuario hace una foto de los alimentos desde el visor CameraX, la app la envía a `/chat/completions` de DevExpert Inference (alias `chat`) y vuelca la respuesta como **chips editables de Alimento→cantidad** que se guardan en la Despensa. La **entrada manual es el contenido principal** de la pantalla; el visor se despliega bajo demanda y se pliega con animación al terminar el análisis. Se conservan intactos la edición manual de chips, el stepper 0..99, la suma de cantidades al guardar (RD13), la marca «Ya está en la Despensa» y el Modo de degradación RD05. La foto nunca se escribe en disco (RD06, reformulado) y se retiran MediaPipe, su asset y sus dependencias.

## Non-Goals

- No se implementan Sugerencias de recetas (`sugerir-recetas-con-ia`), el Pinche ni la Lista de la Compra; el cliente `chat/completions` creado aquí las desbloqueará, pero sin consumidores nuevos.
- No se toca el esquema Room v2 ni `MIGRATION_1_2`; ni la pantalla Despensa, ni Coincidencia/badges, ni la sección «Recetas Compatibles».
- No cambia RD13 (la suma al guardar), ni el máximo 99, ni la confirmación al salir, ni la entrada manual de chips. El botón «Re-escanear» y su confirmación se retiran; para empezar de cero se borran los chips uno a uno o se sale del Escáner.
- No hay chat, historial de conversación, reintentos automáticos ni cola de llamadas: **una foto = una llamada**.
- No se mide latencia ni coste reales de DevExpert Inference (tarea aplazada por el usuario) ni se configura la política «sin retención» del proveedor (tarea propia de `sugerir-recetas-con-ia`, `RK03`).
- No se cambia «Generar con IA» del formulario ni el endpoint `/images/generations`.
- La verificación manual con foto real y llamada de pago **no se ejecuta sin confirmación humana** (regla de `AGENTS.md`).

## Job Story

When tengo alimentos por anotar y no quiero teclearlos uno a uno,
I want to hacer una foto y que la IA proponga los Alimentos con su cantidad,
so I can revisarlos, corregirlos y guardarlos en la Despensa en segundos.

## Users And Permissions

- **Usuario** (único rol del MVP, `P31`): escanea, edita chips y guarda en su Despensa. Usa su propia API key (`⚠ S05`); sin cuentas ni permisos nuevos.

## Acceptance Scenarios

### Scenario 1: Foto → chips → Despensa (verify 1)

Given el Escáner abierto con la API key configurada y el cliente de IA simulado devolviendo `{"manzana":3,"nata":1}`
When el usuario pulsa «Escanear con la cámara», hace una foto y termina el análisis
Then el panel del visor se pliega con animación, aparecen los chips «Manzana» cantidad 3 y «Nata» cantidad 1 debajo del alta manual y, al guardar, la Despensa muestra esos Alimentos con esas cantidades

### Scenario 2: Sin API key (verify 2)

Given el Escáner sin API key configurada (ni de build ni de Ajustes)
When el usuario pulsa «Escanear con la cámara» y hace una foto
Then el visor se pliega con animación, se muestra un aviso que invita a configurar la API key en Ajustes y la entrada manual sigue operativa
And el usuario puede añadir Alimentos a mano y guardarlos sin bloqueos

### Scenario 3: Fallo del cliente de IA (verify 2)

Given el cliente de IA simulado en modo fallo (red, proveedor o respuesta ilegible)
When el usuario pulsa «Escanear con la cámara» y hace una foto
Then el visor se pliega con animación, se muestra un aviso de fallo del análisis y la lista queda editable con entrada manual (RD05)
And se puede reintentar desplegando otra vez el visor y haciendo otra foto, sin reiniciar nada

### Scenario 4: Respuesta con entradas inválidas

Given una respuesta JSON con nombres vacíos, cantidades no enteras, ≤ 0 o > 99 y nombres repetidos tras normalizar
When se vuelca la respuesta
Then los nombres vacíos se descartan, las cantidades no enteras o ≤ 0 quedan «sin cantidad» (0), las superiores se recortan a 99 y los duplicados se fusionan en un solo chip
And una respuesta sin ningún objeto JSON se trata como fallo (Scenario 3)

### Scenario 5: Fotos sucesivas y duplicados

Given una lista con un chip «Manzana» cantidad 2 editada por el usuario y una segunda foto que devuelve `{"manzana":5,"pera":1}`
When el usuario vuelve a desplegar el visor y se analiza la segunda foto
Then «Manzana» conserva cantidad 2 y «Pera» entra con cantidad 1 (acumulación sin pisar ediciones) y el visor se pliega de nuevo
And dentro de una misma respuesta, dos entradas con el mismo nombre normalizado acaban en un solo chip

### Scenario 6: Edición y guardado sin cambios (regresión)

Given chips procedentes de la IA
When el usuario los renombra, ajusta la cantidad con el stepper 0..99, los quita con el icono de borrado, añade alguno a mano y guarda
Then todo funciona igual que hoy: un solo Alimento por nombre normalizado, marca «Ya está en la Despensa» informativa y suma RD13 al guardar
And el alta manual sigue siendo el primer contenido del listado

### Scenario 7: Salir con chips sin guardar (regresión)

Given chips sin guardar
When el usuario sale del Escáner
Then se pide confirmación y, al confirmar, la lista se descarta; sin chips, sale sin preguntar
And no existe ningún botón «Re-escanear» en la pantalla

### Scenario 8: Textos es/en

Given la interfaz en español o en inglés
When se usan captura, avisos, hint y chips
Then todos los textos visibles están en el idioma activo y el prompt pide los nombres de alimento en ese mismo idioma

### Scenario 9: La foto no se persiste (RD06)

Given una captura analizada con éxito o con fallo
When se comprueba el almacenamiento
Then la imagen solo existió en memoria: no hay imágenes nuevas en `cacheDir`/`filesDir` ni en MediaStore

### Scenario 10: Alta manual primero y cámara bajo demanda

Given el Escáner abierto
When el usuario entra en la pantalla
Then ve el alta manual como primer contenido y no se pide permiso de cámara ni se compone el visor
When el usuario pulsa «Escanear con la cámara»
Then el visor se despliega con animación, se solicita permiso si hace falta y la captura queda disponible

### Scenario 11: El visor se pliega tras cualquier análisis

Given el visor desplegado
When termina el análisis con éxito, fallo, sin API key o sin alimentos
Then el visor se pliega con animación y el aviso (si lo hay) queda visible junto al alta manual

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `escanear-despensa-con-ia` (steps, verification, notes: la foto sale del dispositivo; sin umbral de confianza; RD06/RD12 y aviso de privacidad a actualizar); las 3 dependencias en `accepted`.
- `PROGRESS.md` (cabecera) — entorno (AVD `Pixel_10_Pro` API 37, `cmd /c` + `JAVA_HOME`, flakes conocidos), reglas de evidencia y cierre.
- `CONTEXT.md`, `docs/build-brief.md` — términos Alimento/Escaneo/Escáner/Modo de degradación; RGPD `P27` y `⚠ S05`.
- `docs/domain-model.md` — Alimento (0..99, 0 = sin cantidad), filas «Escaneo» y «Relaciones» con la detección continua (a reescribir), RD05/RD06/RD12/RD13, validación de confianza (a retirar).
- `docs/technical-discovery.md` — stack MediaPipe/CameraX/Retrofit-OkHttp, secciones del spike RK02 y de detección en vivo, `AiEndpoints`/`AiKeyProvider` D7-D11, comandos Gradle.
- `docs/risks-and-open-questions.md` — `RK02` (MediaPipe), `RK03` («las fotos nunca salen del dispositivo», afirmación que esta feature deroga), `S05`, tareas de investigación.
- `DESIGN.md` — fila del Escáner («HUD con etiquetas y confianza», a actualizar).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt` — `captureAndAnalyze`, `persistItems` con `sumInto` (RD13), avisos `ScannerNotice`, salida con confirmación y `requestRescan` (a retirar en esta revisión).
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerScreen.kt` (visor fijo arriba, botón de captura, chips, alta manual al final, `ScannerActions` con «Re-escanear») y `ScannerRoute.kt` (permiso de cámara al entrar y preview CameraX).
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — `mergeDraftItems`, `rename`, `updateQuantity`, `matchesWithPantry`, `sumInto`, `toPantryItem`; `ScanAnalysis.mergeProposals`.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt`, `CameraXScanImageSource.kt` (Preview + `ImageCapture`, JPEG en memoria) y `data/ai/ScanImageAnalyzer.kt`, `OpenAiCompatibleScanImageAnalyzer.kt`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/` — `AiEndpoints` (`/images/generations`, alias `image`), `ImageGenerator`/`OpenAiCompatibleImageGenerator` (patrón `Success`/`MissingApiKey`/`Failed`, OkHttp + `org.json`), `AiKeyProvider`/`PriorityAiKeyProvider`/`AiKeyStore`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin: `single<FoodDetector>`, `single<AiKeyProvider>`, `ScannerViewModel(get(), get(), get())`.
- `app/src/main/java/com/whatsforlunch/app/domain/model/DetectedFood.kt`, `ScanDraftItem.kt`, `Scan.kt` — modelo actual con `confidence` y `boundingBox` (a retirar).
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `scanner_*`, `food_label_*` (10 etiquetas COCO) y `settings_privacy_notice` («Las fotos del Escáner no salen del dispositivo», a reescribir).
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `mediapipe-tasks-vision 0.10.35`, CameraX 1.5.3, OkHttp 4.12.0.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` (harness con `FakeLiveFrameSource`/`FakeFoodDetector`, locale inyectado, caso RD06 `liveFramesAreNeverWrittenToCacheOrFiles`), `CameraXLiveFrameSourceTest.kt`, `MediaPipeFoodDetectorTest.kt`.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt`, `ui/scanner/ScannerViewModelTest.kt`, `data/ai/AiEndpointsTest.kt`.

### Existing Patterns To Follow

- MVVM + StateFlow (`*Route` → `*Screen` sin estado → `*ViewModel`); reglas puras en `domain/`; UI sin lógica de negocio.
- Cliente IA tipo `OpenAiCompatibleImageGenerator`: OkHttp + `org.json`, resultados `Success`/`MissingApiKey`/`Failed` (RD05), key vía `AiKeyProvider` (build con prioridad sobre Ajustes).
- `AiEndpoints` compone `<base sin barra final>/<ruta>` y acepta la URL completa del endpoint; alias estables (`image` → aquí `chat`).
- Captura CameraX **en memoria** (`ImageProxy` → `Bitmap` → JPEG, sin ficheros) — RD06; el puerto `ScanImageSource.capture()` ya existió y se retira/restaura con ese patrón.
- E2E Compose con fakes deterministas e idioma inyectado; unit de dominio sin Android; testTags por pantalla; cadenas `scanner_*` duplicadas es/en; objetivos táctiles ≥ 48 dp.

### Current Gaps

- No existe cliente de chat ni de visión: solo `/images/generations`.
- No hay puerto de captura hoy (se retiró con el obturador en `detectar-alimentos-en-vivo`); el bucle en vivo, el HUD y la confianza sobran tras el cambio.
- `org.json` no está disponible en unit tests locales (es stub de Android): el parser necesita `testImplementation` de `org.json:json` (solo test) o tests instrumentados.
- La documentación y el aviso de privacidad afirman que las fotos no salen del dispositivo: deja de ser cierto.

## Technical Approach

1. **Puerto de análisis** (`data/ai/ScanImageAnalyzer.kt`, nombre provisional): `suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult` con `Success(foods: List<AnalyzedFood>)`, `MissingApiKey`, `Failed` (mismo patrón que `ImageGenerator`). `AnalyzedFood(name, quantity)` es el DTO de la respuesta.
2. **Cliente** (`data/ai/OpenAiCompatibleScanImageAnalyzer.kt`): `POST <base>/chat/completions` (D4) con la key de `AiKeyProvider`; extrae `choices[0].message.content`, recupera el primer objeto JSON (se toleran vallas ```` ```json ````) y lo convierte en `AnalyzedFood`; cualquier `IOException`, HTTP != 2xx o cuerpo ilegible → `Failed`.
3. **Normalización en dominio** (`domain/scanner/ScanDetections.kt` o `domain/scanner/ScanAnalysis.kt`): función pura que recibe las propuestas y la lista actual y aplica D1 (acumulación + fusión por `normalizeName` sin pisar ediciones) y D2 (cantidades). Unit-testable sin Android; la UI no valida nada.
4. **Captura** (`data/scanner/ScanImageSource.kt` + `CameraXScanImageSource.kt`): `suspend fun capture(): ByteArray?` con `ImageCapture` de CameraX; JPEG en memoria redimensionado al lado largo ≤ 1024 px (q85) para el payload base64; nunca en disco (D6). Sustituye a `LiveFrameSource`/`CameraXLiveFrameSource`; `CameraPreviewController` se conserva para el visor.
5. **ViewModel** (`ScannerViewModel`): `captureAndAnalyze(source)` en lugar del bucle — estado `isAnalyzing` (antes `isDetecting`), `analyze()` con éxito → fusión D1 en `items`; `MissingApiKey` → aviso `AI_KEY_MISSING`; `Failed` → aviso `ANALYSIS_FAILED` + `manualMode`. Se retiran `startLiveDetection`/`stopLiveDetection`/`detectFrame`/`liveDetections`/`liveStopped` y `ScanDetections.MIN_CONFIDENCE/eligible/mergeLiveDetections/toDraftItems/LIVE_SAMPLE_INTERVAL_MS`. `persistItems` (RD13), `matches`, salida con confirmación y avisos de guardado **no cambian**. Sin reintentos automáticos (D5).
6. **UI** (`ScannerScreen`/`ScannerRoute`): el alta manual es el primer ítem del `LazyColumn` y el borrador del nombre se iza a la pantalla (`rememberSaveable`) para no perderlo al hacer scroll. Debajo, botón «Escanear con la cámara» (`ScannerTestTags.SCANNER_TOGGLE`) que despliega un panel `AnimatedVisibility` con el visor CameraX, el obturador (`CAPTURE`, ≥ 48 dp, deshabilitado mientras `isAnalyzing`) y el progreso (`CAPTURE_PROGRESS`). El permiso de cámara se solicita solo al desplegar y el preview se bindea/desbindea con la composición del panel. `isScannerExpanded` vive en `ScannerUiState`; cualquier resultado del análisis (éxito, fallo, sin key o sin alimentos) lo pliega con animación. Los chips se insertan debajo del alta manual con `Modifier.animateItem()` y cada uno conserva su borrado individual con icono terracota. Se retira el botón «Re-escanear» y su confirmación. El resto del Escáner (chips, stepper, marca, guardar, compatibles) no cambia.
7. **Llamada**: `POST <AI_BASE_URL>/chat/completions` con `model = "chat"`, `messages[0].content = [ {type:"text", text:prompt}, {type:"image_url", image_url:{url:"data:image/jpeg;base64,..."}} ]`, `temperature = 0`; prompt en el idioma de la app que exige **un único objeto JSON** `{"nombre del alimento": cantidad}` con cantidades enteras 1..99 y nombres en ese idioma, sin markdown ni explicaciones (D4, D8).
8. **Retirada de MediaPipe**: dependencia `mediapipe-tasks-vision` (`libs.versions.toml` + `app/build.gradle.kts`), asset `app/src/main/assets/efficientdet_lite2_detection_metadata_1.tflite`, `MediaPipeFoodDetector`, `FoodDetector`/`FoodDetectionResult`, `DetectedFood`/`DetectionBounds`, `Scan.detections` y `ScanDraftItem.confidence` (verificar con `grep` usos restantes antes de borrar cada símbolo).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/data/ai/ScanImageAnalyzer.kt` (o `FoodImageAnalyzer.kt`) — crear; puerto + `AnalyzedFood` + `ScanAnalysisResult`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/OpenAiCompatibleScanImageAnalyzer.kt` — crear; cliente `/chat/completions` con OkHttp + `org.json`.
- `app/src/main/java/com/whatsforlunch/app/data/ai/AiEndpoints.kt` — modificar; `chatCompletions(baseUrl)` con el mismo criterio que `imageGeneration`.
- `app/src/main/java/com/whatsforlunch/app/data/scanner/ScanImageSource.kt`, `CameraXScanImageSource.kt` — crear; captura JPEG en memoria + visor (sustituyen a `LiveFrameSource.kt`/`CameraXLiveFrameSource.kt`, que se borran junto con `FoodDetector.kt` y `MediaPipeFoodDetector.kt`).
- `app/src/main/java/com/whatsforlunch/app/domain/scanner/ScanDetections.kt` — modificar; retirar rastro on-device (`MIN_CONFIDENCE`, `eligible`, `mergeLiveDetections`, `toDraftItems`, `LIVE_SAMPLE_INTERVAL_MS`, `confidence`) y añadir la normalización/fusión de propuestas de IA (D1, D2).
- `app/src/main/java/com/whatsforlunch/app/domain/model/DetectedFood.kt`, `Scan.kt`, `ScanDraftItem.kt` — modificar/borrar según usos; sin `confidence` ni `boundingBox`.
- `app/src/main/java/com/whatsforlunch/app/ui/scanner/ScannerViewModel.kt`, `ScannerScreen.kt`, `ScannerRoute.kt` — modificar; `isScannerExpanded`, alta manual primera en el `LazyColumn`, panel animado del visor, permiso de cámara bajo demanda, plegado tras cualquier análisis, borrado por chip terracota y retirada de «Re-escanear».
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `single<ScanImageAnalyzer>`, `single<ScanImageSource>` (o por uso), `ScannerViewModel` sin `FoodDetector`.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — modificar; nuevas `scanner_open_camera`, `scanner_close_camera`, `scanner_no_detections` y `scanner_capture_hint` reescrito; retirar `scanner_rescan`, `scanner_rescan_title`, `scanner_rescan_message` y `scanner_rescan_confirm`; conservar `settings_privacy_notice` (la foto **se envía** a DevExpert Inference para análisis y no se retiene — `P27`/`RK03`). Paridad claves es/en.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — modificar; retirar `mediapipe` (CameraX y OkHttp se quedan); `testImplementation` de `org.json:json` si el parser se testea en unit.
- `app/src/test/java/com/whatsforlunch/app/domain/scanner/ScanDetectionsTest.kt` (+ `ScanAnalysisTest` si se separa), `ui/scanner/ScannerViewModelTest.kt`, `data/ai/AiEndpointsTest.kt` — modificar/crear; normalización D1/D2, flujo `captureAndAnalyze` (éxito, sin key, fallo), `openScanner`/`closeScanner`, plegado tras análisis y retirada del re-escaneo.
- `app/src/androidTest/java/com/whatsforlunch/app/ScanPantryE2ETest.kt` — modificar; `FakeScanImageSource`/`FakeScanImageAnalyzer`, alta manual primero, despliegue/plegado animado, borrado por chip y los casos S1-S11 (verify 1 y 2 incluidos).
- `app/src/androidTest/java/com/whatsforlunch/app/MyRecipesLocalizationE2ETest.kt` — modificar; pulsar `SCANNER_TOGGLE` antes de esperar el preview y comprobar el alta manual al entrar.
- `app/src/androidTest/java/com/whatsforlunch/app/CompatibleRecipesE2ETest.kt` — modificar; quitar `onRescan` y ajustar desplazamientos si hace falta.
- `docs/domain-model.md` — modificar; fila «Escaneo» (foto analizada por IA), Relaciones (acumulación D1 + RD13), **RD06** reformulado («no se almacenan: se procesan en memoria, se envían al proveedor para análisis y se descartan»), **RD12** marcada como derogada (sin umbral de confianza), validaciones de cantidad propuesta (D2) y casos límite.
- `docs/technical-discovery.md` — modificar; stack de detección → chat multimodal alias `chat`; integraciones con `/chat/completions`; secciones del spike RK02 y de detección en vivo anotadas como **retiradas** (se conserva el historial); diagrama de flujos.
- `docs/risks-and-open-questions.md` — modificar; `RK02` cerrada (MediaPipe retirado), `RK03` reescrita (la foto **sí** sale del dispositivo; sin retención por verificar), `Q-escanear-despensa-con-ia-1..3` y las nuevas `Q-escanear-despensa-con-ia-4..5` (plegado en fallo y retirada del re-escaneo).
- `docs/specs/escanear-despensa-con-camara.md` — modificar; nota de revisión: su jerarquía visual (visor primero) y su «Re-escanear» quedan revisados por esta feature.
- `DESIGN.md` — modificar; fila del Escáner (alta manual primero, visor bajo demanda y plegado, chips con borrado terracota).

## Visual Design Impact

- UI involved: yes (solo la pantalla Escáner y el aviso de privacidad de Ajustes).
- Design source: `DESIGN.md` + tokens de `uxdocs/DESIGN.md` (`P17`); `uxdocs/chefencasa_esc_ner_de_nevera_ia/` como inspiración. El material no define el botón de captura ni el estado «analizando» (laguna RK04 conocida).
- Screens or states affected: Escáner — alta manual como primer contenido, botón secundario «Escanear con la cámara» que despliega el visor, progreso «Analizando la foto…», plegado animado tras el análisis, avisos de sin-key/fallo/sin alimentos, chips **sin** porcentaje de confianza y borrado por chip con icono terracota; Ajustes — copy del aviso de privacidad sin cambios. Sin cambios en Despensa, Mis Recetas ni Detalle.
- New design artifact required: no — componentes M3 y tokens existentes; `AnimatedVisibility` y `Modifier.animateItem()` para el despliegue/plegado y la inserción de chips.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y esta feature no cambia capas ni dirección de dependencias (solo cambia el proveedor de detección dentro de `data/`).
- `CONSTRAINTS.md`: not needed — no existe; las reglas nuevas viven como D1-D8 aquí y como RD06/RD12 en `docs/domain-model.md`.
- `AGENTS.md`: not needed — sin cambios de flujo de agentes, comandos ni convenciones.
- Other docs: `docs/domain-model.md` (RD06/RD12 + Escaneo), `docs/technical-discovery.md` (stack e integraciones), `docs/risks-and-open-questions.md` (RK02/RK03 + Qs), `DESIGN.md` (fila Escáner), notas de revisión en las dos specs del Escáner.

## Dudas abiertas y desconocidos (Unknowns)

Decisiones de producto confirmadas por el usuario el 2026-10-22 (`Q-escanear-despensa-con-ia-1..3`); no queda ambigüedad bloqueante:

- **D1 · Fotos sucesivas acumulan** (Q-…-1): cada foto se fusiona en la lista actual por nombre normalizado; los alimentos nuevos entran con su cantidad propuesta y los chips existentes conservan nombre y cantidad editados. Duplicados dentro de una misma respuesta → un solo chip. Cada foto requiere volver a desplegar el visor; no hay re-escaneo global.
- **D2 · Cantidades inválidas** (Q-…-2): valor no entero o ≤ 0 → chip «sin cantidad» (0); valor > 99 → 99. El alimento nunca se descarta por su cantidad; los nombres vacíos sí se descartan.
- **D3 · Sin confianza ni HUD** (Q-…-3): los chips dejan de mostrar «NN%» (como los manuales) y desaparecen las cajas/píldoras del visor; RD12 queda derogada. Sin marca «IA» en los chips.
- **D4 · Contrato de la llamada**: `POST <base>/chat/completions`, modelo alias `chat` (mismo criterio que `image`), payload multimodal OpenAI-compatible con la foto como `data:image/jpeg;base64`, `temperature = 0`; el prompt exige un único objeto JSON `{"alimento": cantidad}` en el idioma de la app, sin markdown; se toleran vallas ```` ```json ```` al extraer el objeto. JSON ilegible = fallo (RD05).
- **D5 · Degradación RD05**: sin key → aviso a Ajustes; fallo de red/proveedor/parseo → aviso + modo manual; captura reintentable a mano, sin reintentos automáticos ni bloqueo del guardado.
- **D6 · Captura solo en memoria**: JPEG comprimido en memoria (lado largo ≤ 1024 px, q85) y descartado tras el análisis; sin ficheros, sin MediaStore, sin tabla de Escaneos (RD06 reformulado).
- **D7 · Idioma**: el prompt pide los nombres en el idioma actual de la app (`Locale`/configuración activa) y los chips quedan en ese idioma; no hay traducción posterior.
- **D8 · Coste/latencia**: fuera de alcance medirlos (tarea ya aplazada); el timeout es el del `OkHttpClient` compartido, sin configuración nueva.
- **D9 · Alta manual primero y visor bajo demanda** (Q-…-4, 2026-10-23): la entrada manual es el primer ítem del `LazyColumn`; el visor CameraX no se compone ni se pide permiso hasta pulsar «Escanear con la cámara». El panel se despliega con `AnimatedVisibility` y se pliega con animación al terminar **cualquier** análisis (éxito, fallo, sin key o sin alimentos); el aviso queda fuera del panel, visible junto al alta manual.
- **D10 · Sin re-escaneo global** (Q-…-5, 2026-10-23): se retira el botón «Re-escanear» y su confirmación; cada chip conserva su borrado individual (icono de papelera en terracota, área táctil ≥ 48 dp). Salir con chips sin guardar sigue pidiendo confirmación.

No inspeccionado: `PantryScreen.kt`/`SettingsScreen.kt` completos (solo sus contratos), el resto de la suite E2E más allá de `ScanPantryE2ETest` (regresión a cargo del implementador) y `uxdocs/` (solo lectura). El comportamiento real de DevExpert Inference en `/chat/completions` con imágenes **no se ha probado** (requiere llamada de pago): si el proveedor difiere del contrato OpenAI-compatible, se documenta como desviación y se ajusta el parser sin cambiar la UX.

## Implementation Plan

1. Cliente de IA + endpoints + normalización de dominio con unit (D2, D4).
2. Captura CameraX en memoria + retirada del bucle en vivo/HUD/confianza/MediaPipe (D1, D3, D6).
3. UI: alta manual primero, visor bajo demanda y plegado animado, borrado por chip, avisos, hint y cadenas es/en (D5, D7, D9, D10).
4. E2E con fakes (S1-S11, verify 1 y 2), gates completos, docs y cierre en `passing`.

## Implementation Tasks

- [x] T1: `AiEndpoints.chatCompletions` + unit; `ScanImageAnalyzer`/`OpenAiCompatibleScanImageAnalyzer` con parser tolerante a vallas y unit del parser (con `org.json:json` en `testImplementation`) y de error → `Failed`/`MissingApiKey`. (D4, D5)
- [x] T2: normalización en `domain/scanner` (D1, D2) + unit: fusión por nombre normalizado sin pisar ediciones, duplicados en una respuesta, cantidades inválidas → 0, > 99 → 99, nombres vacíos descartados.
- [x] T3: `ScanImageSource`/`CameraXScanImageSource` (JPEG en memoria, ≤ 1024 px) + `CameraXScanImageSourceTest`; borrar `LiveFrameSource`/`CameraXLiveFrameSource`/`FoodDetector`/`MediaPipeFoodDetector`, asset `.tflite` y dependencia MediaPipe. (D6)
- [x] T4: `ScannerViewModel` (`isScannerExpanded`, `openScanner`/`closeScanner`, plegado en todos los finales, `NO_DETECTIONS`, retirada de `requestRescan`) + `ScannerScreen`/`ScannerRoute` (alta manual primera, panel animado, permiso bajo demanda, borrado por chip terracota) + unit del ViewModel. (S1-S7, S10-S11)
- [x] T5: cadenas es/en nuevas y retiradas (`scanner_open_camera`, `scanner_close_camera`, `scanner_no_detections`; fuera `scanner_rescan*`), paridad claves; barrido `grep` de `RESCAN`, `requestRescan`, `rescanAfterConfirmation`, `scanner_rescan` → 0 usos vivos. (S8)
- [x] T6: `ScanPantryE2ETest` reescrito sobre fakes de captura/análisis (S1-S11 con verify 1 y 2), actualizar `MyRecipesLocalizationE2ETest`/`CompatibleRecipesE2ETest` y regresión completa; `docs/risks-and-open-questions.md`, `DESIGN.md`, nota en `docs/specs/escanear-despensa-con-camara.md`; gates y feature en `passing`.

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate no bloqueante; no arranca procesos duraderos; los scripts no se tocan). Nota de `PROGRESS.md`: ejecutar con `cmd /c "set JAVA_HOME=C:\Users\Daniel\.jdks\corretto-17.0.20&& set PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%LOCALAPPDATA%\Android\Sdk\emulator;%PATH%&& set PKG=com.whatsforlunch.app&& powershell -NoProfile -ExecutionPolicy Bypass -File <script>.ps1 > .harness\<x>.log 2>&1"`; el `init` tiene un falso fallo de `pidof` tras `installDebug` (reintentar).
- `gradlew lintDebug` → 0 errores · `gradlew testDebugUnitTest --rerun-tasks` → verde con los unit nuevos · `gradlew assembleDebug` → BUILD SUCCESSFUL.
- E2E persistente: `connectedDebugAndroidTest` en el AVD `Pixel_10_Pro` (API 37) — `./e2e.ps1` vale pero desinstala la app al terminar (reinstalar con `./gradlew installDebug` para comprobaciones manuales):
  - verify 1 (S1): cliente simulado `{"manzana":3,"nata":1}` → chips «Manzana» 3 y «Nata» 1 → guardar → en la Despensa con esas cantidades.
  - verify 2 (S2, S3): sin key y en modo fallo → aviso + alta manual y guardado sin bloqueos.
  - S4-S7: normalización de respuestas inválidas, acumulación de fotos sin pisar ediciones, edición/guardado RD13 sin cambios, confirmación de salida y ausencia del botón «Re-escanear».
  - S8: textos en es/en (locale inyectado, mismo patrón que hoy).
  - S9/RD06: `captureIsNeverWrittenToCacheOrFiles` (sin imágenes nuevas en `cacheDir`/`filesDir`).
  - S10: al abrir, alta manual visible y `CAMERA_PREVIEW`/`CAPTURE` inexistentes; tras `SCANNER_TOGGLE`, preview visible y captura disponible.
  - S11: tras éxito, fallo, sin key o cero alimentos, `isScannerExpanded == false` y el aviso (si procede) visible junto al alta manual; el borrado por chip elimina solo ese chip.
  - Regresión: `AppLaunchE2ETest`, `CreateRecipeE2ETest`, `EditRecipeE2ETest`, `DeleteRecipeE2ETest`, `SearchAndFilterRecipesE2ETest`, `PantryManagementE2ETest`, `PantryLocalizationE2ETest`, `MatchBadgeE2ETest`, `RecipeDetailE2ETest`, `MyRecipesLocalizationE2ETest`, `CompatibleRecipesE2ETest`, `SettingsE2ETest`, `KeystoreAiKeyStoreTest` (los tests de MediaPipe se retiran con la feature).
  - Barrido de restos: `grep` de `MediaPipe`, `mediapipe`, `efficientdet`, `FoodDetector`, `mergeLiveDetections`, `LIVE_SAMPLE_INTERVAL_MS`, `MIN_CONFIDENCE`, `food_label_`, `scanner_confidence`, `scanner_hud_` en `app/src` y `gradle/` → 0 hits vivos.
- verify 3 (manual, **con confirmación previa del usuario por ser llamada de pago**): una foto real en es y otra en en con API key real → JSON válido y nombres/cantidades razonables; resultado registrado en la evidencia. Si el usuario no autoriza la llamada, la feature queda en `passing` con la verificación anotada como pendiente en `PROGRESS.md`.
- Cobertura E2E: **sí** (flujo de usuario y llamada de API nuevos); los casos usan cliente y captura simulados, sin red real ni coste.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se modifican.

## Evidence To Capture

- `docs/validations/escanear-despensa-con-ia.md` — informe con gates (nombres de test y recuentos), recorrido S1-S11, los dos `verify` del emulador, el verify 3 (o su pendiente), el barrido de restos de MediaPipe y la revisión de RD06/RD12; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/escanear-despensa-con-ia/` — hasta 2 capturas nuevas en español: estado inicial con alta manual y visor plegado, y resultado del análisis con el visor plegado o el aviso de degradación. La captura histórica `scanner-degradacion-es.png` se conserva y queda **fuera** de este presupuesto.
- Presupuesto del rediseño ≤ 3 artefactos (1 informe + 2 capturas nuevas); la captura histórica no cuenta.

## Validator Checklist

- [ ] La implementación se queda dentro del alcance (sin tocar Room v2, Despensa, Coincidencia, compatibles, RD13, máximo 99 ni `/images/generations`).
- [ ] Las decisiones D1-D10 se respetan o quedan documentadas como desviación acordada.
- [ ] Los escenarios S1-S11 pasan y los verify 1 y 2 se ejecutan en el emulador; el verify 3 está ejecutado con confirmación humana o declarado pendiente.
- [ ] Al abrir el Escáner no se pide permiso de cámara, el alta manual es el primer contenido y el visor solo se compone tras «Escanear con la cámara».
- [ ] El visor se pliega con animación tras éxito, fallo, sin key y cero alimentos; el aviso queda visible fuera del panel.
- [ ] No queda ningún botón «Re-escanear»; cada chip tiene borrado individual con icono terracota y área táctil ≥ 48 dp.
- [ ] RD06 reformulada y RD12 marcada como derogada en `docs/domain-model.md`; `RK02`/`RK03` y aviso de privacidad coherentes con «la foto se envía al proveedor».
- [ ] Notas de revisión presentes en `docs/specs/escanear-despensa-con-camara.md` y `docs/specs/detectar-alimentos-en-vivo.md`.
- [ ] Barrido de restos de MediaPipe/confianza/HUD en `app/src` → 0 hits; asset `.tflite` y dependencia MediaPipe fuera del build.
- [ ] La entrada manual conserva el texto al hacer scroll (borrador izado) y el teclado no tapa el campo ni el visor.
- [ ] La captura no se escribe nunca en disco (RD06) verificado por test y a mano.
- [ ] La evidencia está presente, dentro del presupuesto y solo de esta feature.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente; la feature pasa a `passing` solo tras `lint` + `unit` + `assembleDebug` + `e2e`.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular, sin Sugerencias, Pinche ni Lista de la Compra).
