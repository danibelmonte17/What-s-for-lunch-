# Feature Implementation Spec: Gestionar temporizador de paso

## Source Feature

- `id`: gestionar-temporizador-de-paso
- `area`: funcional
- `depends_on`: cocinar-paso-a-paso (accepted)
- `status`: not_started (a la hora de planificar; la spec no cambia `feature_list.json`)
- `source`: `feature_list.json` (decisiones del usuario 2026-10-28, registradas como D1-D4)

## Goal

Dotar al Modo Cocina del **temporizador funcional del Paso activo**: widget con **cuenta atrás circular** (anillo + reloj digital `mm:ss` + meta), **arranque manual** con «Iniciar», controles «Pausar/Reanudar», «Reiniciar» y **«+1 min»**, y **alarma sonora al llegar a cero** (tono de alarma del sistema en bucle) con la ampliación de un minuto y «Detener». Sustituye al hint pasivo «⏱ N min» de `cocinar-paso-a-paso` (RF-CPP6, D6), que quedó diferido a esta feature.

El temporizador vive **solo en el Modo Cocina** (memoria): al cambiar de Paso o salir se descarta y silencia; sin servicios, notificaciones ni AlarmManager (D1).

## Non-Goals

- **Alarma en segundo plano**: sin notificaciones, servicios, `AlarmManager` ni permisos exactos (D1). La cuenta atrás solo corre con el Modo Cocina visible.
- **Vibración**: la alarma es sonora y visual; sin permiso `VIBRATE` (D4).
- **Voz** (locución, «Pausar tiempo» por voz): es `controlar-cocina-con-voz` (`⚠ S04`), **ya implementada** (RD24: «Pausar tiempo» pausa la cuenta en marcha y nunca la reanuda; reanudar sigue siendo táctil).
- **Editar `timerMinutes` desde el Modo Cocina**: la meta viene del formulario («Temporizador (min, opcional)»); el Modo Cocina solo la usa.
- **Persistencia del temporizador** entre Pasos, salidas o sesiones (coherente con RD22: progreso en memoria).
- **Cambios en Room (v4), `RecipeValidation`, prompts de IA ni en el formulario de Receta**.
- **Cambios en el Detalle**: su listado de Pasos conserva el rótulo informativo «⏱ N min».

## Job Story

When estoy cocinando un paso con un tiempo de cocción y no puedo vigilar el reloj,
I want to lanzar un temporizador con cuenta atrás y que me avise con una alarma al llegar a cero,
so I can controlar la cocción a distancia y ampliar un minuto si hace falta sin tocar nada más.

## Users And Permissions

- Usuario personal (1-5 usuarios, MVP): única interacción; sin roles, sin cuentas y **sin permisos Android nuevos** (el tono de alarma del sistema no los necesita).

## Requisitos funcionales

- **RF-GTP1**: el Modo Cocina muestra, para un Paso con `timerMinutes`, el **widget del temporizador**: título «Temporizador», píldora de estado, reloj digital `mm:ss` con la cuenta restante, **anillo circular** de progreso (restante / meta vigente) y línea «Meta: N min». **Sustituye al hint pasivo «⏱ N min»** (cambia RF-CPP6 de `cocinar-paso-a-paso`; D5).
- **RF-GTP2**: **arranque manual** (D3): al entrar en un Paso el temporizador está **«Listo»** con su meta y nunca arranca solo; «Iniciar» pone la cuenta a decrecer **un segundo por segundo**.
- **RF-GTP3**: controles siempre visibles con targets ≥ 48 dp (D2, como el prototipo): **«Pausar»/«Reanudar»** (botón primario según el estado), **«Reiniciar»** (restablece la cuenta a la meta vigente y la deja «En marcha») y **«+1 min»** (suma 60 s a la meta vigente y a la cuenta; en «Tiempo cumplido» silencia la alarma, suma 60 s y reanuda la cuenta).
- **RF-GTP4**: **alarma al llegar a cero** (D4/D8): reloj 00:00, píldora **«¡Tiempo cumplido!»** y el tono de alarma del sistema **en bucle** hasta «Detener» o «+1 min»; «Detener» silencia y devuelve el temporizador a «Listo» con la meta vigente.
- **RF-GTP5**: **ciclo de vida en memoria** (D1): cambiar de Paso («Siguiente»/«Paso Anterior») o salir («Volver»/atrás/«Terminar») descarta la cuenta y silencia la alarma; cada Paso entra «Listo» con su propia meta. El estado sobrevive a la rotación (ViewModel) y se reinicia al reabrir el Modo Cocina.
- **RF-GTP6**: un Paso **sin `timerMinutes`** no muestra widget (sin cambios respecto a hoy).

## Acceptance Scenarios

### Scenario 1 (verify 1): la alarma suena al llegar a cero

Given un Paso con temporizador de 0 min (el paso más corto: cualquier meta llega a cero igual) abierto en el Modo Cocina
When pulso «Iniciar»
Then la cuenta llega a 00:00 y suena la alarma: píldora «¡Tiempo cumplido!», reloj 00:00 y controles «+1 min» y «Detener» (el sonido se verifica por unit con fake y a mano; ver Verification Plan)

### Scenario 2 (verify 2): «+1 min» amplía la cuenta

Given la alarma del Scenario 1 sonando
When pulso «+1 min»
Then la alarma se silencia, la meta queda en 1 min y la cuenta se extiende (reloj ≈ 01:00) en «En marcha»

### Scenario 3: arranque manual y cuenta decreciente

Given un Paso con temporizador de 5 min al entrar en el Modo Cocina
Then el temporizador está «Listo» con reloj 05:00, «Meta: 5 min» y el botón «Iniciar» (no arranca solo)
When pulso «Iniciar»
Then pasa a «En marcha» y el reloj decrece (tras unos segundos ya no muestra 05:00)

### Scenario 4: pausar y reanudar

Given el temporizador «En marcha» con la cuenta decreciendo
When pulso «Pausar»
Then la píldora pasa a «Pausado» y el reloj se congela; al pulsar «Reanudar» la cuenta continúa desde donde estaba

### Scenario 5: «Reiniciar» restablece la meta vigente

Given el temporizador con «+1 min» ya aplicado (meta 6 min sobre un Paso de 5 min) y la cuenta a mitad
When pulso «Reiniciar»
Then el reloj vuelve a 06:00 y el temporizador queda «En marcha»

### Scenario 6: cambiar de Paso o salir descarta el temporizador

Given la alarma sonando en el Paso 1
When pulso «Siguiente» (o «Volver»)
Then la alarma se silencia; el Paso 2 entra «Listo» con su propia meta y, al volver al Paso 1 o reabrir el Modo Cocina, también está «Listo» (sin cuenta ni alarma heredadas)

### Scenario 7: Paso sin temporizador

Given un Paso sin `timerMinutes`
Then no se muestra el widget del temporizador

### Scenario 8: textos es/en

Given el idioma de la app en español o en inglés
Then los textos del widget (título, estados, botones, meta) cambian con el idioma (paridad de claves)

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `gestionar-temporizador-de-paso` (steps/verification) y dependencia `cocinar-paso-a-paso` = `accepted`.
- `docs/specs/cocinar-paso-a-paso.md` — RF-CPP6/D6 (hint pasivo diferido a esta feature) y non-goals que la remiten aquí.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt` — `StepCard` con el hint (`cook_mode_step_timer_hint`, tag `CookModeTestTags.STEP_TIMER`), drawer y dock: punto de inserción del widget.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModel.kt` — `Cooking(recipe, stepIndex, totalSteps, ingredientRows)`, índice en memoria, `next`/`previous`: dónde enganchar la máquina del temporizador y el tick.
- `app/src/main/java/com/whatsforlunch/app/domain/cookmode/CookModeRules.kt` — patrón de reglas puras en `domain/` (P25).
- `app/src/main/java/com/whatsforlunch/app/domain/model/RecipeStep.kt` y `domain/createrecipe/RecipeValidation.kt` — `timerMinutes: Int?` (opcional; `0` es válido y hoy muestra «⏱ 0 min»).
- `app/src/main/java/com/whatsforlunch/app/data/photo/PhotoStore.kt` (+ `data/ai/AiKeyProvider.kt`) — patrón puerto/impl en `data/<area>/` con dobles en tests.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — Koin `single`/`viewModel { parameters -> … }` (`CookModeViewModel` ya registrado con parámetros).
- `app/src/main/AndroidManifest.xml` — permisos actuales: `CAMERA` e `INTERNET` (ninguno de audio; no se añade ninguno).
- `app/src/main/res/values/strings.xml` y `values-en/strings.xml` — `cook_mode_step_timer_hint` (a retirar); paridad actual 305/305.
- `app/src/test/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModelTest.kt` — `runTest` + `StandardTestDispatcher(testScheduler)` + `setMain` (tiempo virtual disponible); `kotlinx-coroutines-test` ya es dependencia de test.
- `app/src/androidTest/java/com/whatsforlunch/app/CookModeE2ETest.kt` — fixtures (Room + `E2eDatabase.clear()`, `CookModeNoOpPhotoStore`, ViewModel construido directo) y aserciones del hint en `advancingStepUpdatesProgressAndShowsFinishButton` (líneas 126-128), que hay que adaptar.
- `uxdocs/chefencasa_modo_cocina_paso_a_paso/code.html` — widget «Built-in Kitchen Timer» (anillo + reloj digital + «Meta original», píldora «En Marcha»/«Pausado», botones Pausar/Reiniciar/+1 min).
- `DESIGN.md` (fila Modo Cocina; tertiary `#8d4b00` para temporizadores; AAA, cuerpo ≥ 18 px, targets ≥ 48 dp; «la alarma sonora es propuesta libre»), `docs/domain-model.md` (RD22 + casos límite), `docs/build-brief.md` («temporizador con alarma y `+1 min`»), `docs/risks-and-open-questions.md` (Q-cocinar-paso-a-paso-3), `docs/technical-discovery.md` (comandos/gates).

### Existing Patterns To Follow

- MVVM + StateFlow + Koin; `*Route` → `*Screen` con callbacks; **reglas puras en `domain/`** (P25) y puertos con implementación Android en `data/<area>/` con dobles (fakes) en tests.
- Test tags por pantalla en objetos `*TestTags`; textos en `values/` + `values-en/` con paridad de claves.
- Tests: unit JVM con `runTest` y tiempo virtual (`advanceTimeBy`); E2E instrumentados con fixtures locales, sin IA ni red; E2E focalizada por clases.

### Current Gaps

- No existe ninguna lógica de cuenta atrás ni de sonido: cero usos de `Ringtone`/`MediaPlayer`/`ToneGenerator` en la app.
- El hint pasivo es el único rastro del temporizador en el Modo Cocina; no hay estados, controles ni alarma.
- El sonido de la alarma solo está definido como «propuesta libre» en `DESIGN.md` (queda fijado en D4).

## Technical Approach

**Máquina de estados pura (D6)** — `domain/cookmode/StepTimer.kt`: `StepTimerPhase { READY, RUNNING, PAUSED, FINISHED }` y `StepTimerState(originalMinutes, extendedSeconds, remainingSeconds, phase)` con **meta vigente = original + ampliaciones** y transiciones puras (`initial`, `start`, `pause`, `resume`, `reset`, `addMinute`, `tick`, `stopAlarm`…), sin dependencias de Android. Contrato por fases (reloj = `mm:ss`; píldora; controles):

| Fase | Reloj | Píldora | Controles (primario primero) |
| --- | --- | --- | --- |
| READY | meta vigente | «Listo» | «Iniciar», «Reiniciar», «+1 min» |
| RUNNING | decreciente | «En marcha» | «Pausar», «Reiniciar», «+1 min» |
| PAUSED | congelado | «Pausado» | «Reanudar», «Reiniciar», «+1 min» |
| FINISHED | 00:00 | «¡Tiempo cumplido!» | «+1 min», «Detener» |

Reglas: «Reiniciar» → cuenta = meta vigente y `RUNNING`; «+1 min» → meta y cuenta +60 s (en `FINISHED`: silencia, cuenta = 60 s, `RUNNING`); «Detener» → silencia y `READY` con la meta vigente; `tick` en 0 → `FINISHED` + alarma.

**Alarma (D4/D8)** — puerto `data/audio/TimerAlarm.kt` (`start()`/`stop()`) con implementación `SystemRingtoneTimerAlarm` (Context): `RingtoneManager.getDefaultUri(TYPE_ALARM)` (fallback `TYPE_NOTIFICATION`), `Ringtone` con `isLooping = true` (API 28+; minSdk 30), `stop()` silencia y libera. Sin permisos ni assets. Registrada en `AppModule` (`single`); los tests usan `FakeTimerAlarm` que cuenta `start`/`stop`. *Hallazgo de implementación*: `Ringtone.setAudioStreamType` ya no está en los stubs de compileSdk 35, así que el stream de alarma se fija con `AudioAttributes(USAGE_ALARM + CONTENT_TYPE_SONIFICATION)` (equivalente y moderno).

**ViewModel** — `CookModeViewModel` gana `timerAlarm: TimerAlarm` y expone el temporizador dentro del estado (`Cooking.timer: StepTimerState`, inicial `READY` con la meta del Paso activo). Bucle de tick: `viewModelScope.launch { while (isActive) { delay(1000); … } }` **solo con `RUNNING`** (D6: `delay`, no timestamps del sistema → tiempo virtualizable con `kotlinx-coroutines-test`); al llegar a cero llama `timerAlarm.start()` una vez. Cambio de Paso (`next`/`previous`), salida (`onCleared`) → `timerAlarm.stop()` + temporizador del nuevo Paso en `READY`.

**UI** — `StepTimerCard` en `CookModeScreen` bajo la tarjeta del Paso (composición del prototipo `P16`, mandan los tokens `P17`): título, píldora de estado, anillo (`CircularProgressIndicator` circular con progreso restante/meta) y reloj digital centrado dentro, línea «Meta: N min» y fila de controles (≥ 48 dp; primario `Button`, resto `OutlinedButton`/`TextButton`). `StepCard` pierde el hint y su parámetro `timerMinutes`. `CookModeTestTags` gana `TIMER_*` y retira `STEP_TIMER`. **Pantalla encendida** en el Modo Cocina (`LocalView.current.keepScreenOn`, D9). Sin reglas de negocio en la UI (P25).

**Sin cambios** en Room (v4), `RecipeValidation`, prompts, formulario, Detalle, Despensa ni Lista de la Compra.

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/cookmode/StepTimer.kt` — create; fase/estado y transiciones puras del temporizador.
- `app/src/main/java/com/whatsforlunch/app/data/audio/TimerAlarm.kt` — create; puerto `start()`/`stop()`.
- `app/src/main/java/com/whatsforlunch/app/data/audio/SystemRingtoneTimerAlarm.kt` — create; alarma con tono del sistema en bucle.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModel.kt` — modify; estado del temporizador, tick, eventos y `timerAlarm`.
- `app/src/main/java/com/whatsforlunch/app/ui/cookmode/CookModeScreen.kt` — modify; `StepTimerCard`, retirada del hint (`STEP_TIMER`), tags `TIMER_*` y pantalla encendida.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modify; `single { SystemRingtoneTimerAlarm(androidContext()) }` y paso a `CookModeViewModel`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modify; ~12 claves nuevas (`cook_mode_timer_*`: título, 4 estados, meta, Iniciar/Pausar/Reanudar/Reiniciar/+1 min/Detener) y retirada de `cook_mode_step_timer_hint`, con paridad.
- `app/src/test/java/com/whatsforlunch/app/domain/cookmode/StepTimerTest.kt` — create; transiciones y casos límite.
- `app/src/test/java/com/whatsforlunch/app/ui/cookmode/CookModeViewModelTest.kt` — modify; tick con tiempo virtual, alarma con `FakeTimerAlarm`, descarte al cambiar de Paso/salir.
- `app/src/androidTest/java/com/whatsforlunch/app/CookModeTimerE2ETest.kt` — create; S1-S8.
- `app/src/androidTest/java/com/whatsforlunch/app/CookModeE2ETest.kt` — modify; adaptar la aserción del hint RF-CPP6 (líneas 126-128) al widget.
- Docs: `docs/domain-model.md`, `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, nota en `docs/specs/cocinar-paso-a-paso.md` (ver más abajo). `feature_list.json`: al cerrar, solo `status`/`evidence`.

## Visual Design Impact

- UI involved: **sí**. Design source: `DESIGN.md` (tokens Warm Culinary Craft) + `uxdocs/chefencasa_modo_cocina_paso_a_paso/` (widget «Built-in Kitchen Timer», inspiración `P16`; mandan los tokens, `P17`).
- Screens/states afectados: Modo Cocina (widget en sus 4 fases + controles; Paso sin temporizador) y su cabecera (pantalla encendida).
- Del prototipo se toma la composición del widget (anillo + reloj digital + meta + píldora + 3 controles); el color de temporizadores es el tertiary `#8d4b00` (ámbar) de `DESIGN.md`; contraste AAA y targets ≥ 48 dp.
- New design artifact required: **no**; el sonido de la alarma se resuelve con el tono del sistema (D4) y las animaciones se limitan al anillo del prototipo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** (no existe y no hace falta crearlo): sin capas ni dependencias nuevas.
- `CONSTRAINTS.md`: **not needed** (no existe y no hace falta crearlo): sin MUST/MUST NOT nuevos.
- `AGENTS.md`: **not needed** — sin cambios de flujo, comandos ni convenciones.
- `docs/domain-model.md`: **update** — nueva regla **RD23** (temporizador del Paso en el Modo Cocina: máquina de estados, arranque manual, controles, alarma y ciclo de vida en memoria) + casos límite nuevos; ajustar la frase final de RD22 y la fila de casos límite «Paso con temporizador en el Modo Cocina» (el hint pasivo se sustituye por el widget).
- `DESIGN.md`: **update** — la fila «Modo Cocina» describe el widget del temporizador (y que el hint pasivo desaparece); la nota «alarma sonora … propuesta libre» queda resuelta con D4.
- `docs/technical-discovery.md`: **update** — `TimerAlarm`/`SystemRingtoneTimerAlarm` (tono del sistema, sin permisos); sin cambios de Room ni de comandos.
- `docs/risks-and-open-questions.md`: **update** — registrar `Q-gestionar-temporizador-de-paso-1..4` (D1-D4) al implementar.
- `docs/specs/cocinar-paso-a-paso.md`: **update** — nota en RF-CPP6/D6 y Non-Goals: el hint pasivo «⏱ N min» queda sustituido por el widget de esta feature (cambio de comportamiento aceptado, al estilo de RF-CPP5 → `ver-detalle-de-receta`).
- `CONTEXT.md`: **not needed** — «Paso», «Modo Cocina» y «temporizador» ya están definidos; sin términos nuevos.

## Decisions

- **D1** (usuario 2026-10-28): la cuenta atrás y la alarma viven **solo en el Modo Cocina** (memoria): al salir o cambiar de Paso se descartan y silencian; sin notificaciones, servicios ni `AlarmManager`.
- **D2** (usuario 2026-10-28): controles **como el prototipo**: «Pausar/Reanudar», «Reiniciar» y «+1 min» siempre visibles; en la alarma, «+1 min» y «Detener».
- **D3** (usuario 2026-10-28): arranque **manual** con «Iniciar»; la cuenta nunca arranca sola al entrar en el Paso.
- **D4** (usuario 2026-10-28): la alarma suena con el **tono de alarma del sistema** (`RingtoneManager.getDefaultUri(TYPE_ALARM)` + `Ringtone`), sin assets, sin permisos nuevos ni dependencias; **sin vibración**.
- **D5** (derivada): el widget **sustituye al hint pasivo «⏱ N min»** del Paso (cambia RF-CPP6 de `cocinar-paso-a-paso`, que quedó diferido a esta feature).
- **D6** (derivada/técnica): la máquina de estados es **pura en `domain/cookmode/`** (P25) y el tick es `delay(1000)` en el ViewModel (tiempo virtualizable en unit), no timestamps del sistema.
- **D7** (derivada): la meta vigente es **original + ampliaciones** («+1 min» suma a meta y cuenta; «Reiniciar» restablece la meta vigente y deja «En marcha» —como el prototipo—; «Detener» silencia y vuelve a «Listo» con la meta vigente).
- **D8** (derivada): la alarma suena **en bucle** (`Ringtone.setLooping`) hasta «Detener» o «+1 min» y se silencia siempre que se descarta el temporizador (D1).
- **D9** (derivada): el Modo Cocina mantiene la **pantalla encendida** mientras está visible, para que la cuenta sea legible a distancia con el alcance de D1.

## Casos límite

| Caso | Comportamiento |
| --- | --- |
| Paso sin `timerMinutes` | Sin widget (RF-GTP6) |
| `timerMinutes = 0` | Widget con meta 0:00; «Iniciar» llega a cero al primer tick → alarma inmediata (lo usa S1) |
| Cambiar de Paso con la alarma sonando | Se silencia y descarta; el nuevo Paso entra «Listo» con su meta |
| Salir («Volver»/atrás/«Terminar») con cuenta o alarma | Se silencia y descarta; al reabrir, Paso 1 «Listo» |
| Rotación de la pantalla | El estado del temporizador (y la alarma sonando) se conserva (ViewModel) |
| Meta ≥ 60 min | Reloj `mm:ss` sin tope (p. ej. «120:00»); «Meta: N min» con N entero |
| «+1 min» con la cuenta en marcha | Suma 60 s sin interrumpir la cuenta ni la fase |
| «+1 min» acumulados y «Reiniciar»/«Detener» | Usan la meta vigente (original + ampliaciones) |
| «Iniciar» con meta 0:00 tras «Detener» | Vuelve a sonar al primer tick (coherente con la meta del Paso) |
| Pantalla apagada / app en segundo plano | Fuera del alcance (D1); D9 mantiene la pantalla activa en el Modo Cocina |
| Paso con `timerMinutes` negativo | Imposible: `RecipeValidation.isValidTimerMinutes` y el formulario lo descartan |

## Implementation Plan

1. **Dominio**: `StepTimer` (fase/estado + transiciones puras) + unit con los casos de la tabla.
2. **Alarma**: puerto `TimerAlarm` + `SystemRingtoneTimerAlarm` (tono del sistema en bucle) y `FakeTimerAlarm` para tests.
3. **ViewModel**: estado `Cooking.timer`, tick por `delay(1000)` con tiempo virtual, eventos de los controles y descarte al cambiar de Paso/salir + unit.
4. **UI**: `StepTimerCard` (anillo + reloj + meta + píldora + controles), retirada del hint, tags y pantalla encendida; textos es/en con paridad.
5. **E2E**: `CookModeTimerE2ETest` (S1-S8) y adaptación del hint en `CookModeE2ETest`.
6. **Docs y cierre**: `domain-model.md` (RD23), `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (Q-1..4), nota en la spec de `cocinar-paso-a-paso`, `PROGRESS.md` y `feature_list.json`.

## Implementation Tasks

- [x] T1: `domain/cookmode/StepTimer.kt` + tests de transiciones (READY/RUNNING/PAUSED/FINISHED, +1 min, Reiniciar, Detener, tick a cero).
- [x] T2: `data/audio/TimerAlarm.kt` + `SystemRingtoneTimerAlarm.kt` (bucle, fallback `TYPE_NOTIFICATION`, `stop()`) y registro Koin.
- [x] T3: `CookModeViewModel` con `timerState`, tick virtualizable, `timerAlarm` al llegar a cero y descarte al cambiar de Paso/salir + `FakeTimerAlarm` en `CookModeViewModelTest`.
- [x] T4: `StepTimerCard` en `CookModeScreen` + `CookModeTestTags.TIMER_*`, retirada del hint/`STEP_TIMER`, pantalla encendida y textos es/en (retirar `cook_mode_step_timer_hint`).
- [x] T5: E2E `CookModeTimerE2ETest` (S1-S8; verify 1 = S1, verify 2 = S2) + adaptar las líneas 126-128 de `CookModeE2ETest`.
- [x] T6: docs (RD23 + casos límite, `DESIGN.md`, `docs/technical-discovery.md`, `Q-gestionar-temporizador-de-paso-1..4`, nota en `docs/specs/cocinar-paso-a-paso.md`).
- [x] T7: gates completos y cierre (`PROGRESS.md`, `feature_list.json` solo `status`/`evidence`).

## Verification Plan

- `./init.ps1` (Windows) o `./init.sh` — «Entorno listo», exit 0 (comprobaciones no bloqueantes; **no** arranca procesos largos).
- `./lint.ps1` / `./gradlew :app:lintDebug` — **0 errores** y sin warnings nuevos sobre la línea base (49).
- `./unit.ps1` / `./gradlew :app:testDebugUnitTest --rerun-tasks` — todo verde (hoy 38 suites / 251 tests); entran los tests de `StepTimer` y los nuevos de `CookModeViewModelTest`.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- Paridad es/en: mismo conjunto de claves en `values/strings.xml` y `values-en/strings.xml` (hoy 305/305; tras la feature, mismas claves en ambos idiomas).
- **E2E focalizada** (la feature cambia comportamiento visible): `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whatsforlunch.app.CookModeTimerE2ETest,com.whatsforlunch.app.CookModeE2ETest` — verde con S1-S8 y la regresión de `cocinar-paso-a-paso` (S1-S7, con la aserción del hint adaptada al widget). Fixtures locales, sin IA ni red. La cobertura E2E persistente es obligatoria: la feature cambia la UI y el flujo del Modo Cocina.
- **E2E batería**: `./e2e.ps1` (`connectedDebugAndroidTest` completo) — sin regresiones sobre los 195 tests de inventario (los flakes del AVD se documentan y reejecutan como en sesiones previas).
- **Sonido de la alarma (verify 1)**: el E2E no puede asertar salida de audio, por lo que «la alarma suena» se verifica en tres niveles: (1) **unit** con `FakeTimerAlarm` (`start()` al llegar a cero, `stop()` al descartar), (2) **E2E** con el estado de alarma en UI (píldora «¡Tiempo cumplido!», «+1 min»/«Detener») y (3) **manual** en el AVD (oído / `adb shell dumpsys audio` mientras suena).
- Manual (apoyo, con `adb`/uiautomator): rotación conserva la cuenta y la alarma; al salir del Modo Cocina la alarma se silencia; pantalla encendida con el Modo Cocina visible.

## Evidence To Capture

- `docs/evidence/gestionar-temporizador-de-paso/`: informe + **2 capturas es 1280×2856** (PNG binario): (1) temporizador «En marcha» con anillo, reloj y meta; (2) alarma «¡Tiempo cumplido!» con «+1 min» y «Detener».
- Salida de los gates (`init`, `lint`, `unit`, `assembleDebug`, paridad), de la E2E focalizada (nombres de los tests) y recuento de la batería.
- `PROGRESS.md` (Estado verificado + Siguiente paso + entrada en `docs/progress-history.md`) y `evidence` de `feature_list.json`.
- Sin API keys, sin llamadas a la IA ni PII en la evidencia (`⚠ S05`).

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance (sin segundo plano, notificaciones, servicios, `AlarmManager`, vibración, voz, permisos nuevos ni cambios en Room/validaciones/prompts/formulario/Detalle).
- [ ] S1-S8 pasan (verify 1 = S1, verify 2 = S2) y los E2E de `cocinar-paso-a-paso` siguen en verde con la aserción del hint adaptada.
- [ ] D1-D9 cumplidas (solo Modo Cocina, controles del prototipo, arranque manual, tono del sistema, widget sustituye al hint, máquina pura + tick virtual, meta vigente, alarma en bucle, pantalla encendida).
- [ ] La alarma queda verificada en los tres niveles (unit con fake, E2E de estado, manual del sonido).
- [ ] Evidencia presente: gates, E2E focalizada y batería, 2 capturas es válidas (cabecera PNG, 1280×2856) e informe en `docs/evidence/gestionar-temporizador-de-paso/`.
- [ ] Cobertura E2E añadida para el cambio de comportamiento visible (nuevo widget + alarma).
- [ ] `feature_list.json` solo cambia `status`/`evidence`; `PROGRESS.md` al día; docs durables actualizadas (RD23 + casos límite, `DESIGN.md`, `docs/technical-discovery.md`, `Q-gestionar-temporizador-de-paso-1..4`, nota en `docs/specs/cocinar-paso-a-paso.md`).
- [ ] Sin comportamiento ajeno ni trabajo de `controlar-cocina-con-voz` añadido; `AndroidManifest.xml` sin permisos nuevos.
- [ ] Seguridad: sin secretos, sin red, sin datos nuevos persistidos (Room sigue v4).
