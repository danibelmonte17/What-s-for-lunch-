# Feature Implementation Spec: Controlar la cocina con voz

## Source Feature

- `id`: controlar-cocina-con-voz
- `area`: funcional
- `depends_on`: cocinar-paso-a-paso (accepted); «Pausar tiempo» reutiliza `gestionar-temporizador-de-paso` (accepted)
- `status`: not_started (a la hora de planificar; la spec no cambia `feature_list.json`)
- `source`: `feature_list.json` (decisiones de producto del usuario 2026-10-29, registradas como D1-D6)

## Goal

Añadir al **Modo Cocina** el control **manos libres por voz** con las APIs nativas de Android (`TextToSpeech` y `SpeechRecognizer`, `⚠ S04`): un interruptor **«Voz» apagado por defecto** que, al activarse, **locuta el Paso activo** y los cambios de Paso, y mantiene **escucha continua** de los comandos exactos **«Siguiente»**, **«Repetir»** y **«Pausar tiempo»**. El botón **«Escuchar»** sigue locutando el Paso puntualmente aunque la voz esté apagada.

La feature incluye el **permiso `RECORD_AUDIO`** (se pide al activar «Voz» por primera vez), la **degradación visible** si se deniega o si el dispositivo no tiene motor de voz/reconocimiento, el **idioma de la app (es/en)** y el **ciclo de vida**: la voz vive solo en memoria, se detiene al salir o al pasar a segundo plano y nunca escucha en background. La alarma del temporizador conserva la prioridad.

## Non-Goals

- **Dictado en el Pinche** y **alta por voz en la Lista de la Compra**: siguen diferidos (`Q-...`, `⚠ S04`).
- **Wake word**, palabra de activación, escucha en segundo plano, `ForegroundService`, notificaciones o `AlarmManager` para la voz.
- **Más comandos** que los tres acordados (sin «Anterior», «Reanudar tiempo», «Detener alarma» ni control por voz de «+1 min»).
- **Reconocimiento parcial** o fuzzy matching: solo resultados finales y frase completa exacta.
- **Persistencia** de audio, transcripciones, preferencia de voz o estado del interruptor; sin cambios en Room, IA, prompts, validaciones ni semántica del temporizador.
- **Audio focus, ducking, vibración, Bluetooth ni selección manual de motor/voz**; los gestiona el sistema.
- **Nuevas dependencias**: `material-icons-extended` ya está en el proyecto y `TextToSpeech`/`SpeechRecognizer` son APIs de plataforma.

## Job Story

Cuando estoy cocinando con las manos ocupadas o sucias,
quiero activar la voz para que me lea el Paso y avanzar o pausar el tiempo hablando,
para no tener que tocar la pantalla y no perder el hilo de la receta.

## Users And Permissions

- **Usuario local (único rol)**: activa/desactiva «Voz», usa «Escuchar» y dicta los tres comandos. Sin cuentas ni backend.
- **Permiso `RECORD_AUDIO`**: runtime, se solicita **solo al activar «Voz»** la primera vez. Denegarlo no bloquea el Modo Cocina: «Escuchar» y los botones siguen funcionando; el aviso permite reintentar tocando «Voz».
- **Servicio de voz del sistema**: procesa el audio del micrófono; la app no lo almacena ni lo envía a DevExpert Inference. El aviso de privacidad de Ajustes debe mencionarlo.
- **Implementador/validador**: los tests usan fakes; la precisión real y el diálogo de permiso se validan en `T0` y en la comprobación manual (dispositivo físico solo con confirmación del usuario).

## Acceptance Scenarios

### Scenario 1: «Siguiente» avanza de Paso (verify 1 de `feature_list.json`)

Given el Modo Cocina en el Paso 1 de 2 con la voz activada y el micrófono escuchando
When el reconocimiento entrega la frase «Siguiente»
Then el Modo Cocina muestra el Paso 2, actualiza «Paso 2 de 2» y locuta el Paso 2.

### Scenario 2: activar la voz locuta el Paso activo

Given el Modo Cocina en el Paso 1 con la voz apagada
When el usuario toca «Voz» y concede el micrófono
Then el chip queda como «Voz activa», aparece la barra «Comandos de voz activos» y suena «Paso 1. {texto}» (más el temporizador si lo tiene).

### Scenario 3: «Repetir» no cambia de Paso

Given la voz activa en el Paso 2
When se reconoce «Repetir»
Then vuelve a locutarse el Paso 2 y la pantalla sigue en el Paso 2.

### Scenario 4: «Pausar tiempo» pausa la cuenta en marcha

Given el temporizador del Paso en «En marcha»
When se reconoce «Pausar tiempo»
Then la píldora pasa a «Pausado» y el reloj deja de decrementar; no se reanuda solo.

### Scenario 5: «Siguiente» en el último Paso termina

Given el Modo Cocina en el último Paso con la voz activa
When se reconoce «Siguiente»
Then se detienen locución y escucha, se emite el evento de fin y la app vuelve al Detalle (mismo destino que «Terminar»).

### Scenario 6: «Escuchar» funciona con la voz apagada

Given la voz apagada y el permiso de micrófono sin conceder
When el usuario toca «Escuchar»
Then se locuta el Paso activo sin pedir micrófono y sin iniciar la escucha.

### Scenario 7: permiso denegado con reintento

Given la voz apagada y el permiso `RECORD_AUDIO` denegado
When el usuario toca «Voz»
Then la voz sigue apagada, se muestra el aviso de permiso y «Escuchar»/los botones siguen operativos; al volver a tocar «Voz» con el permiso ya concedido, la voz se activa.

### Scenario 8: idioma inglés

Given la app en inglés y la voz activa
When se reconoce «next»
Then avanza de Paso; «siguiente» se ignora y los textos visibles son «Voice on», «Listen» y «Voice commands active».

### Scenario 9: motor de voz no disponible

Given un dispositivo sin motor TTS o sin servicio de reconocimiento
When el usuario intenta activar «Voz» o toca «Escuchar»
Then no hay crash, la voz queda apagada y se muestra el aviso correspondiente; los botones siguen funcionando.

## Repository Research

### Files Inspected

- `CookModeScreen.kt` (tags 60-84, `CookingContent` 212-254, dock 623-671), `CookModeViewModel.kt` (`next`/`previous` 110-122, timer/alarma 168-180, `onCleared` 140-143) y `CookModeRoute.kt` (15-38) — pantalla, estado y Route sin permisos.
- `StepTimer.kt` (`StepTimerRules.pause`, 37-38) y `RecipeStep.kt` — pausa reutilizable y datos del Paso.
- `AppModule.kt` (120-130) y `AppNavHost.kt` (118-123) — Koin y `onFinish`.
- `AndroidManifest.xml` (solo `CAMERA`/`INTERNET`), strings es/en (316/316; privacidad 203-204) y `SettingsScreen.kt` (188-195) — permisos, paridad y aviso.
- `CookModeE2ETest.kt` (297-337), `CookModeTimerE2ETest.kt` y `CookModeViewModelTest.kt` — ViewModel fuera de composición, fakes y ~20 construcciones.
- Specs de cocina/temporizador, `docs/domain-model.md` (RD22/RD23), `DESIGN.md`, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` (`⚠ S04`).

### Existing Patterns To Follow

- MVVM + `StateFlow`; `Screen` sin estado y `Route` que une ViewModel, permisos y navegación (patrón de `ScannerRoute`); reglas puras en `domain/` (P25).
- Puertos + adaptadores en `data/` (`TimerAlarm`/`SystemRingtoneTimerAlarm`) con fakes en tests; Koin con `viewModel { parameters -> ... }`.
- Tests instrumentados con `createComposeRule`, ViewModel fuera de composición, `CookModeTestTags` y repositorios en memoria.
- Textos es/en con paridad exacta; documentación y specs en español.

### Current Gaps

- No existe código de voz, ni permiso `RECORD_AUDIO`, ni `<queries>` de reconocimiento/TTS.
- `next()` no emite evento de fin; no hay estado de voz, barra, avisos ni strings de locución/plurals.
- `CookModeE2ETest`/`CookModeTimerE2ETest` construyen el ViewModel y habrá que pasarles fakes; el aviso de privacidad no menciona micrófono.
- La disponibilidad real de `SpeechRecognizer`/TTS en español en el AVD es desconocida: la resuelve `T0`.

## Technical Approach

**Capas y contratos**

- `domain/voice/VoiceCommand.kt` y `VoiceCommandParser.kt`: enum `VoiceCommand { NEXT, REPEAT, PAUSE_TIMER }` y `parse(transcript, language)`. Normaliza minúsculas, tildes, signos y espacios, y compara la **frase completa**: es `siguiente`/`repetir`/`pausar tiempo`; en `next`/`repeat`/`pause timer`; otro idioma cae a español. Sin subcadenas ni fuzzy matching.
- `data/voice/StepSpeaker.kt` y `AndroidStepSpeaker.kt`: `StepAnnouncement(stepNumber, text, timerMinutes)`, puerto `prepare(locale)/speak/stop/release` y adaptador `TextToSpeech` con `QUEUE_FLUSH`; `prepare` (idempotente) espera `onInit`, valida `setLanguage` y devuelve `false` sin motor/idioma; compone la locución con `cook_mode_voice_announce_step` + plurals si `timerMinutes > 0`; `release()` hace `shutdown()`.
- `data/voice/VoiceCommandSource.kt` y `AndroidVoiceCommandSource.kt`: `VoiceSourceEvent { Heard(text), PermissionDenied, Unavailable }`, puerto `events/start(locale)/stop/release` (start idempotente) y adaptador `SpeechRecognizer` en el hilo principal con `EXTRA_LANGUAGE`, `EXTRA_PREFER_OFFLINE = true`, `EXTRA_MAX_RESULTS = 1` y sin parciales. Bucle: `onResults` emite `Heard` y relanza; `NO_MATCH`/`SPEECH_TIMEOUT` reintentan a 250 ms; `BUSY`/`CLIENT`/`SERVER`/`NETWORK` reintentan a 1 s hasta 3 veces; `INSUFFICIENT_PERMISSIONS` emite `PermissionDenied`; idioma no soportado o 3 fallos emiten `Unavailable`. Los puertos se invocan en el hilo principal (`Dispatchers.Main.immediate`); `prepare` devuelve en Main.

**ViewModel**

- `CookModeUiState.Cooking` añade `voice: VoiceUiState(enabled: Boolean = false, notice: VoiceNotice? = null)` con `VoiceNotice { PERMISSION_DENIED, VOICE_UNAVAILABLE, COMMANDS_UNAVAILABLE }`; el constructor añade `stepSpeaker` y `voiceCommandSource` (sin valores por defecto; se actualizan Koin, unit y E2E).
- `onVoiceEnabled(locale)` (en `viewModelScope`, con guarda contra pulsaciones concurrentes): guarda `voiceLocale = locale`, `prepare(locale)`; si falla → `VOICE_UNAVAILABLE` y voz apagada; si `start(locale)` falla → `COMMANDS_UNAVAILABLE` y voz apagada (TTS queda listo para «Escuchar»); si todo va bien → `enabled = true`, limpia aviso y locuta el Paso activo.
- `onVoiceDisabled()` / `onVoicePermissionDenied()`: detienen source y speaker, apagan la voz y (el segundo) muestran `PERMISSION_DENIED`.
- `onSpeakStep(locale)`: prepara el TTS y locuta el Paso activo aunque la voz esté apagada; si falla → `VOICE_UNAVAILABLE` y, si estaba activa, apaga la voz.
- `onHostStopped()`: cancela la activación en curso, `source.stop()` + `speaker.stop()` y conserva `enabled`; `onHostStarted()`: si `enabled`, `source.start(voiceLocale)` y, si falla, apaga con `COMMANDS_UNAVAILABLE`. No se relanza la locución al volver.
- Colección de `voiceCommandSource.events`: `Heard` solo se parsea con la voz activa y `voiceLocale.language`; `PermissionDenied` delega en `onVoicePermissionDenied()`; `Unavailable` apaga con `COMMANDS_UNAVAILABLE`.
- `NEXT`: si es el último Paso, `source.stop()` + `speaker.stop()` y emite `CookModeEvent.Finish`; si no, cambia de Paso (descarta el temporizador/alarma como `next()`). `REPEAT`: locuta el Paso actual. `PAUSE_TIMER`: llama a `onTimerPause()`. `next()`/`previous()` locutan el nuevo Paso si la voz está activa y `commitTimer` llama a `speaker.stop()` al entrar en `FINISHED`, antes de `timerAlarm.start()`.
- `val events: SharedFlow<CookModeEvent>` con `Finish`; `onCleared()` cancela jobs, hace `release()` de ambos puertos y mantiene `discardTimer()`. La voz no se persiste: sobrevive a la rotación y nace apagada al reabrir el Modo Cocina.

**Route, permiso y ciclo de vida**

- `CookModeRoute` obtiene `locale = LocalConfiguration.current.locales[0]` y lo normaliza (inglés si `language == "en"`, si no español), usa `rememberLauncherForActivityResult(RequestPermission())` y decide en `onVoiceToggle`: si la voz está activa → `onVoiceDisabled()`; si el permiso está concedido → `onVoiceEnabled(locale)`; si no → lanza `Manifest.permission.RECORD_AUDIO`. El resultado concede → `onVoiceEnabled(locale)`; deniega → `onVoicePermissionDenied()`.
- Un observador de ciclo de vida llama `onHostStopped()` en `ON_STOP` y `onHostStarted()` en `ON_START`; al reanudar comprueba el permiso y, si se revocó con la voz activa, llama `onVoicePermissionDenied()` sin arrancar la fuente. `LaunchedEffect(viewModel)` recoge `events` y traduce `Finish` a `onFinish()`.
- La lógica de permiso no entra en el ViewModel; los E2E pueden invocar `onVoiceEnabled`/`onVoicePermissionDenied` con fakes.

**UI**

- En `CookModeScreen` (firma + `CookingContent`) se añaden `onVoiceToggle` y `onSpeakStep`. Bajo `StepProgress`, una fila con `FilterChip` «Voz»/«Voz activa» (icono `Icons.Outlined.Mic`, seleccionado = `voice.enabled`) y `OutlinedButton` «Escuchar» (icono `Icons.Outlined.VolumeUp`). Debajo: aviso (`errorContainer`, `liveRegion = Polite`) si `notice != null` y barra de comandos (`secondaryContainer`) solo si `enabled`.
- Tags nuevos: `VOICE_TOGGLE`, `VOICE_LISTEN`, `VOICE_CUE`, `VOICE_NOTICE`; targets ≥ 48 dp, cuerpo ≥ 18 sp, iconos decorativos con `contentDescription = null`. Sin cambios en el dock, el temporizador, el drawer ni el orden del Paso.

**DI, manifest y textos**

- `AppModule`: `factory<StepSpeaker> { AndroidStepSpeaker(androidContext()) }` y `factory<VoiceCommandSource> { AndroidVoiceCommandSource(androidContext()) }`; el `CookModeViewModel` recibe `get()` para ambos. `factory` evita que `release()` de una sesión inutilice el motor de la siguiente.
- `AndroidManifest.xml`: `<uses-permission android:name="android.permission.RECORD_AUDIO" />`, `<uses-feature android:name="android.hardware.microphone" android:required="false" />` y `<queries>` con `android.speech.RecognitionService` y `android.intent.action.TTS_SERVICE`.
- Strings es/en (mismas claves; 316/316 actuales + las nuevas): `cook_mode_voice_toggle` («Voz»/«Voice»), `cook_mode_voice_toggle_active` («Voz activa»/«Voice on»), `cook_mode_voice_listen` («Escuchar»/«Listen»), `cook_mode_voice_commands_title` («Comandos de voz activos»/«Voice commands active»), `cook_mode_voice_commands_hint` («Di: «Siguiente», «Repetir» o «Pausar tiempo»»/«Say: “Next”, “Repeat”, or “Pause timer”»), `cook_mode_voice_permission_denied`, `cook_mode_voice_unavailable`, `cook_mode_voice_commands_unavailable`, `cook_mode_voice_announce_step` («Paso %1$d. %2$s»/«Step %1$d. %2$s») y plurals `cook_mode_voice_announce_timer` («Temporizador de %1$d minuto(s)»/«Timer: %1$d minute(s)»).
- `settings_privacy_notice` (es/en): añadir que, con «Voz» activa, el micrófono capta comandos y el servicio de voz del sistema los procesa (puede usar la red según el proveedor del dispositivo), y que la app no guarda audio ni transcripciones.

**Estrategia de test**

- Unit: parser puro (es/en, tildes, mayúsculas, puntuación, frases no exactas, idioma inactivo) y ViewModel con `FakeStepSpeaker`/`FakeVoiceCommandSource` (activación, locución, comandos, fin, pausa, avisos, ciclo de vida, alarma, release).
- E2E: `CookModeVoiceE2ETest` con fakes que emiten transcripciones; se adaptan los constructores de `CookModeE2ETest`/`CookModeTimerE2ETest`. La precisión real se mide en `T0` y en la comprobación manual, no con el E2E.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/voice/VoiceCommand.kt`, `VoiceCommandParser.kt` — create; enum, normalización y coincidencia exacta.
- `app/src/main/java/com/whatsforlunch/app/data/voice/StepSpeaker.kt`, `AndroidStepSpeaker.kt` — create; `StepAnnouncement`, puerto y adaptador TTS.
- `app/src/main/java/com/whatsforlunch/app/data/voice/VoiceCommandSource.kt`, `AndroidVoiceCommandSource.kt` — create; eventos, puerto y adaptador `SpeechRecognizer`.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModel.kt` — modify; estado de voz, eventos, locución, comandos, lifecycle y prioridad de alarma.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeRoute.kt` — modify; permiso, locale, lifecycle y `Finish`.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt` — modify; controles, barra, aviso, tags y callbacks.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; factories y constructor del ViewModel.
- `app/src/main/AndroidManifest.xml` — modify; `RECORD_AUDIO`, feature de micrófono y `<queries>`.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — modify; claves nuevas, plurals y aviso de privacidad (paridad).
- `app/src/test/java/com/whatsforlunch/app/domain/voice/VoiceCommandParserTest.kt` — create; normalización y comandos. `CookModeViewModelTest.kt` — modify; fakes y escenarios de voz.
- `app/src/androidTest/java/com/whatsforlunch/app/CookModeVoiceE2ETest.kt` — create; S1-S9. `CookModeE2ETest.kt`, `CookModeTimerE2ETest.kt` — modify; fakes y callbacks.
- Docs: `docs/domain-model.md` (RD24), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `DESIGN.md`, `CONTEXT.md`, notas en las specs de cocina/temporizador, `PROGRESS.md` y `feature_list.json` (solo `status`/`evidence` al cerrar).

## Visual Design Impact

- UI involved: **sí**. Design source: `DESIGN.md` (tokens Warm Culinary Craft) + `uxdocs/chefencasa_modo_cocina_paso_a_paso/` (píldora «Voz Activa», botón «Escuchar», barra «Comandos de voz activos»; inspiración `P16`, mandan los tokens `P17`).
- Screens/states affected: Modo Cocina con voz apagada, activa (barra de comandos), aviso de permiso, aviso de voz no disponible y variante inglesa.
- El chip activo usa `secondaryContainer`/`onSecondaryContainer`; la barra de comandos, el mismo contenedor; el aviso, `errorContainer`/`onErrorContainer`. Targets ≥ 48 dp, contraste AAA y cuerpo ≥ 18 sp.
- New design artifact required: **no**; no hay imágenes ni assets nuevos y los iconos ya existen en `material-icons-extended`.

## Durable Documentation Impact

- `ARCHITECTURE.md`, `CONSTRAINTS.md` y `AGENTS.md`: **not needed** — no existen / no cambian el flujo, los comandos ni las convenciones; los puertos siguen el patrón `data/` de `TimerAlarm`.
- `docs/domain-model.md`: **update** — nueva **RD24** (voz opcional, comandos exactos, permiso, sin persistencia, prioridad de alarma) y casos límite.
- `DESIGN.md`: **update** — la fila «Modo Cocina» incluye los controles de voz y la barra de comandos; la nota del prototipo deja de estar pendiente.
- `docs/technical-discovery.md`: **update** — fila de voz: `TextToSpeech` + `SpeechRecognizer`, puertos, sin dependencias, permiso y privacidad; estado confirmado tras `T0` (o bloqueado con el motivo).
- `docs/risks-and-open-questions.md`: **update** — resolver la tarea pendiente de precisión de voz con el resultado de `T0`; actualizar `⚠ S04`; registrar `Q-controlar-cocina-con-voz-1..6` (D1-D6) y pasarlas al apéndice.
- `CONTEXT.md`: **update** — añadir **Voz** y **Comando de voz** al glosario.
- `docs/specs/cocinar-paso-a-paso.md` y `docs/specs/gestionar-temporizador-de-paso.md`: **update** — nota de que la voz diferida llega aquí y de que «Pausar tiempo» reutiliza `StepTimerRules.pause` sin cambiar RD23.

## Decisions

- **D1** (usuario 2026-10-29): **interruptor manual «Voz» apagado por defecto**; al activarlo, locución automática al activar/cambiar de Paso y escucha continua. «Escuchar» locuta puntualmente aunque la voz esté apagada.
- **D2** (usuario 2026-10-29): comandos **exactos** «Siguiente», «Repetir» y «Pausar tiempo»; «Pausar tiempo» pausa la cuenta en marcha y **no reanuda** (reanudar sigue siendo táctil).
- **D3** (usuario 2026-10-29): «Siguiente» por voz en el **último Paso termina** y vuelve al Detalle, igual que «Terminar».
- **D4** (usuario 2026-10-29): `RECORD_AUDIO` se pide **al activar «Voz» por primera vez**; si se deniega, aviso visible y el Modo Cocina sigue con «Escuchar» y botones, con reintento al tocar «Voz».
- **D5** (usuario 2026-10-29): locución y reconocimiento usan el **idioma de la app (es/en)**; cualquier otro idioma cae a español.
- **D6** (usuario 2026-10-29): la locución es **«Paso N. {texto}» + «Temporizador de N minutos»** si el Paso lo tiene; **no lee ingredientes**.
- **D7** (derivada): el parser es **puro en `domain/`**, normaliza y exige coincidencia de **frase completa**; solo resultados finales, sin parciales ni fuzzy matching.
- **D8** (derivada): puertos en `data/voice/` y adaptadores Android; el ViewModel no usa recursos ni `Context`; los textos de locución los compone el adaptador TTS.
- **D9** (derivada): la voz vive **solo en memoria**; nace apagada al entrar, se detiene al salir/segundo plano y `onCleared` libera los motores; sin Room ni preferencias.
- **D10** (derivada): el **Route** gestiona permiso, locale y lifecycle; el ViewModel recibe `onVoiceEnabled(locale)`/`onVoicePermissionDenied()` y no conoce el diálogo del sistema.
- **D11** (derivada): el fin por voz se comunica con `SharedFlow<CookModeEvent.Finish>`; el Route lo traduce a `onFinish`, sin meter navegación en el ViewModel.
- **D12** (derivada): TTS con `QUEUE_FLUSH` y parada al entrar en «Tiempo cumplido»; la escucha sigue activa durante la locución y el parser exacto evita la autoactivación. **Plan B si `T0` detecta autoactivación**: suspender la escucha mientras el TTS habla y reanudarla al terminar, sin cambiar el contrato de comandos.
- **D13** (derivada): sin dependencias nuevas; se usan `material-icons-extended` y APIs de plataforma; sin wake word, audio focus ni servicios.
- **D14** (derivada): el aviso de privacidad de Ajustes menciona micrófono, servicio de voz del sistema (puede usar la red) y que no se guarda audio ni transcripciones.
- **D15** (derivada): el E2E usa fakes de `StepSpeaker`/`VoiceCommandSource`; la precisión real se demuestra en `T0` y en la comprobación manual, nunca con un E2E que simule el micrófono.

## Casos límite

| Caso | Comportamiento |
| --- | --- |
| Paso sin `timerMinutes` o `timerMinutes = 0` | Locución solo con «Paso N. {texto}»; el widget sigue existiendo si el dato está |
| Permiso denegado / «No volver a preguntar» | Voz apagada, aviso de permiso; «Escuchar» y botones funcionan; reintento al tocar «Voz» |
| Permiso revocado en Ajustes con la voz activa | Al reanudar se comprueba, se apaga la voz y se muestra el aviso |
| Sin TTS/idioma, sin reconocimiento o micrófono ocupado | Aviso `VOICE_UNAVAILABLE`/`COMMANDS_UNAVAILABLE`; tras 3 fallos de micrófono, voz apagada; sin crash |
| «Pausar tiempo» en Listo/Pausado/Tiempo cumplido o sin temporizador | No-op; no reanuda ni detiene la alarma |
| «Siguiente» en el último Paso | Detiene voz y escucha, emite `Finish` y vuelve al Detalle |
| «Repetir» o «Siguiente» mientras el TTS habla | `QUEUE_FLUSH`: reinicia la locución o cambia y locuta el nuevo Paso |
| Frase no exacta («siguiente, por favor») | Se ignora; no hay aviso ni comando |
| Comando del idioma inactivo o app en otro idioma | Se ignora; es/en usan su vocabulario y otro idioma cae a español |
| Rotación o segundo plano | El estado sobrevive; se cortan TTS y micrófono y al volver se reanuda la escucha sin relocutar |
| Alarma sonando mientras el TTS habla | El TTS se detiene; la alarma sigue hasta «Detener»/«+1 min» o hasta que «Siguiente» cambie de Paso (descarta el temporizador, como la navegación táctil) |
| Salir con «Volver», atrás o «Terminar» | Se detienen fuente y locutor, se liberan motores y no queda escucha |
| Pulsaciones repetidas en «Voz» durante la activación | Se ignoran; no se crean dos reconocedores |

## Implementation Plan

1. **T0 (bloqueante)**: medir la precisión real de `SpeechRecognizer`/TTS en español e inglés y el flujo de permiso; si falla, parar y documentar el bloqueo sin implementar.
2. **Dominio**: `VoiceCommand` + `VoiceCommandParser` con unit de normalización y comandos.
3. **Puertos/adaptadores**: `StepSpeaker`/`AndroidStepSpeaker` y `VoiceCommandSource`/`AndroidVoiceCommandSource`, con fakes de test.
4. **ViewModel**: estado de voz, activación/desactivación, avisos, locución, comandos, `Finish`, lifecycle y prioridad de alarma + unit.
5. **Route/DI/manifest**: permiso, locale, lifecycle, `Finish`, factories, `RECORD_AUDIO` y `<queries>`.
6. **UI/textos**: controles, barra, aviso, tags y strings es/en con paridad; aviso de privacidad.
7. **E2E**: `CookModeVoiceE2ETest` (S1-S9) y adaptación de los E2E existentes.
8. **Docs y cierre**: RD24, `DESIGN.md`, `docs/technical-discovery.md`, riesgos/`S04`, `CONTEXT.md`, notas de specs, `PROGRESS.md` y `feature_list.json`.

## Implementation Tasks

- [ ] T0: spike de voz real (emulador con servicios de voz o dispositivo con confirmación): 10 repeticiones por comando, ≥ 8/10; TTS audible es/en; sin autoactivación; diálogo de permiso con concesión/denegación/reintento; registrar dispositivo, versión y resultados en `docs/evidence/controlar-cocina-con-voz/t0-voice-spike.md`. Si falla, `blocked`.
- [ ] T1: `VoiceCommand` + `VoiceCommandParser` + `VoiceCommandParserTest` (es/en, tildes, mayúsculas, puntuación, espacios, frases no exactas, idioma inactivo).
- [ ] T2: `StepSpeaker`/`AndroidStepSpeaker` (prepare, language, QUEUE_FLUSH, anuncio con plurals, release) y `VoiceCommandSource`/`AndroidVoiceCommandSource` (bucle, offline, errores, permisos, release); fakes en test.
- [ ] T3: `CookModeViewModel` con `VoiceUiState`, activación/desactivación, avisos, locución en activación/cambio, comandos, `CookModeEvent.Finish`, lifecycle, parada en alarma y release + tests.
- [ ] T4: `CookModeRoute` con `RequestPermission`, locale, lifecycle, comprobación de permiso revocado y colección de `Finish`; `AppModule` con factories.
- [ ] T5: `CookModeScreen` con chip «Voz», «Escuchar», barra de comandos, aviso, tags `VOICE_*` y callbacks; strings es/en y aviso de privacidad.
- [ ] T6: `AndroidManifest.xml` con `RECORD_AUDIO`, feature de micrófono opcional y `<queries>` de reconocimiento/TTS.
- [ ] T7: `CookModeVoiceE2ETest` (S1-S9) + adaptar `CookModeE2ETest` y `CookModeTimerE2ETest` a fakes y nuevos callbacks.
- [ ] T8: docs durables (RD24, `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `CONTEXT.md`, notas en specs de cocina/temporizador).
- [ ] T9: gates, E2E focalizada y batería, comprobación manual real (permiso, ciclo de vida, alarma, inglés) y 2 capturas es.
- [ ] T10: `PROGRESS.md` (Estado verificado/Siguiente paso/Historial) y `feature_list.json` solo `status`/`evidence`.

## Verification Plan

- `./init.ps1` — «Entorno listo», exit 0 (no arranca procesos largos).
- `./lint.ps1` / `./gradlew :app:lintDebug` — **0 errores** y sin warnings nuevos sobre la línea base de 49.
- `./unit.ps1` / `./gradlew :app:testDebugUnitTest --rerun-tasks` — todo verde (hoy 39 suites / 270 tests); entran `VoiceCommandParserTest` y los nuevos casos de `CookModeViewModelTest`.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismas claves en `values/strings.xml` y `values-en/strings.xml` (hoy 316/316 + las nuevas, incluidos los plurals).
- **T0 (antes de implementar)**: 10 repeticiones por comando en español y las correspondientes en inglés; **≥ 8/10** por comando, TTS inteligible, sin autoactivación durante 10 locuciones y diálogo de permiso correcto. AVD con servicios de voz o dispositivo físico con confirmación; si no hay servicios o no se alcanza el umbral, la feature queda `blocked` y se actualiza `⚠ S04`. No se commitean grabaciones de audio.
- **E2E focalizada**: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.CookModeVoiceE2ETest,com.whatsforlunch.app.CookModeE2ETest,com.whatsforlunch.app.CookModeTimerE2ETest` — S1-S9 verdes y regresión de Modo Cocina/Temporizador. Fixtures locales, fakes, sin IA ni red.
- **E2E batería**: `./e2e.ps1` (`connectedDebugAndroidTest` completo) — sin regresiones sobre el inventario de 203 `@Test` (crecerá con los nuevos); documentar y reejecutar flakes del AVD.
- **Manual real (post-implementación)**: conceder/denegar/reintentar permiso; revocarlo en Ajustes y volver; rotar y pasar a segundo plano; comprobar que el micrófono se apaga al salir y al apagar «Voz»; alarma mientras el TTS habla; comandos en inglés y en español; que «Repetir»/«Siguiente»/«Pausar tiempo» funcionan a ~30 cm en una cocina silenciosa.
- **Manual de degradación**: desactivar/limitar el servicio de voz del sistema si el dispositivo lo permite, o validar los avisos con fakes; «Escuchar» y los botones deben seguir operativos.
- **Evidencia visual**: 2 PNG es de 1280×2856 (cabecera PNG válida): (1) voz activa con barra de comandos; (2) aviso de permiso denegado o voz activa en inglés.

## Evidence To Capture

- `docs/evidence/controlar-cocina-con-voz/`: informe, `t0-voice-spike.md`, 2 capturas es 1280×2856 y salidas de gates/E2E con nombres de tests.
- `PROGRESS.md` (Estado verificado, Siguiente paso y entrada en `docs/progress-history.md`) y `evidence` de `feature_list.json`.
- `docs/validations/controlar-cocina-con-voz.md` lo redacta el `feature-validator`, no el implementador.
- Sin API keys, secretos, PII ni grabaciones de audio en el repositorio o los logs (`⚠ S05`).

## Validator Checklist

- [ ] El alcance se respeta: sin dictado en Pinche/Lista, sin wake word, sin background, sin comandos extra, sin cambios en Room/IA/prompts/validaciones/semántica del temporizador.
- [ ] S1-S9 pasan (S1 = verify 1 de `feature_list.json`) y los E2E de `cocinar-paso-a-paso`/`gestionar-temporizador-de-paso` siguen verdes.
- [ ] D1-D15 se cumplen, en especial: voz apagada por defecto, tres comandos exactos, «Pausar tiempo» no reanuda, último «Siguiente» termina, idioma de la app, permiso al activar y avisos con reintento.
- [ ] `T0` tiene evidencia real (≥ 8/10 por comando, sin autoactivación) o la feature está `blocked`; el E2E con fakes no se presenta como prueba de precisión.
- [ ] El micrófono se detiene al apagar «Voz», salir, pasar a segundo plano y terminar; `onCleared` libera TTS y reconocedor; no hay escucha en background.
- [ ] La alarma tiene prioridad y detiene el TTS; «Pausar tiempo» reutiliza `StepTimerRules.pause` y no altera RD23.
- [ ] Permiso `RECORD_AUDIO` solo al activar «Voz»; «Escuchar» no lo pide; denegación/revocación dejan aviso y el Modo Cocina sigue usable.
- [ ] Aviso de privacidad actualizado en es/en; manifest con `RECORD_AUDIO`, feature opcional y `<queries>`; sin dependencias nuevas.
- [ ] Evidencia presente (gates, paridad, E2E focalizada y batería, T0, manual, 2 PNG e informe) y `feature_list.json`/`PROGRESS.md`/docs durables al día (RD24, `DESIGN.md`, `docs/technical-discovery.md`, `S04`, `CONTEXT.md`, notas de specs).
- [ ] Seguridad: sin secretos, sin audio/transcripciones persistidos, sin PII; sin trabajo de otras features.
