package com.whatsforlunch.app.ui.cookmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.audio.TimerAlarm
import com.whatsforlunch.app.data.voice.StepAnnouncement
import com.whatsforlunch.app.data.voice.StepSpeaker
import com.whatsforlunch.app.data.voice.VoiceCommandSource
import com.whatsforlunch.app.data.voice.VoiceSourceEvent
import com.whatsforlunch.app.domain.cookmode.CookModeRules
import com.whatsforlunch.app.domain.cookmode.StepTimerPhase
import com.whatsforlunch.app.domain.cookmode.StepTimerRules
import com.whatsforlunch.app.domain.cookmode.StepTimerState
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.servings.ServingsScaling
import com.whatsforlunch.app.domain.voice.VoiceCommand
import com.whatsforlunch.app.domain.voice.VoiceCommandParser
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Estado visible del Modo Cocina (RF-CPP1 a RF-CPP4, RD22): cargando, Receta no
// encontrada o la cocción con el Paso activo, sus Ingredientes ya escalados, el
// temporizador del Paso (RD23; null = Paso sin temporizador, RF-GTP6) y la voz
// opcional (RD24, D1).
sealed interface CookModeUiState {
    data object Loading : CookModeUiState

    data object NotFound : CookModeUiState

    data class Cooking(
        val recipe: Recipe,
        // Índice del Paso activo (0-based; la UI muestra «Paso X de Y» con X+1).
        val stepIndex: Int,
        val totalSteps: Int,
        // Ingredientes vinculados del Paso activo (RD21) con la cantidad escalada a
        // las Raciones heredadas del Detalle (RD03); vacío = «Sin ingredientes» (D4).
        val ingredientRows: List<RecipeIngredient>,
        // Temporizador del Paso activo (RF-GTP1, RD23); su estado efectivo es el del
        // snapshot si coincide el Paso o, si no, el inicial «Listo» del Paso (D3).
        val timer: StepTimerState?,
        // Voz del Modo Cocina (RD24): apagada por defecto (D1) y aviso visible si
        // falta permiso, motor o servicio de reconocimiento (D4).
        val voice: VoiceUiState = VoiceUiState(),
    ) : CookModeUiState
}

// Estado de la voz (D1, D4): el interruptor y el aviso de degradación. La voz
// vive solo en memoria (D9) y nace apagada al reabrir el Modo Cocina.
data class VoiceUiState(
    val enabled: Boolean = false,
    val notice: VoiceNotice? = null,
)

enum class VoiceNotice {
    PERMISSION_DENIED,
    VOICE_UNAVAILABLE,
    COMMANDS_UNAVAILABLE,
    LANGUAGE_NOT_DOWNLOADED,
}

// Evento de salida del ViewModel (D11): el fin por voz en el último Paso se
// traduce en la Route a `onFinish`, sin meter navegación en el ViewModel.
sealed interface CookModeEvent {
    data object Finish : CookModeEvent
}

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1). El Paso activo vive solo en
// memoria (D7): sobrevive a la rotación (mismo ViewModel) y se reinicia al crear el
// ViewModel al reabrir el Modo Cocina. Solo lee la Receta: no escribe en ningún
// repositorio ni contiene reglas de negocio (D9).
//
// El temporizador del Paso (RD23) también vive solo en memoria (D1): la máquina de
// estados es pura (D6) y el tick es un `delay(1000)` por segundo solo con la cuenta
// «En marcha» (tiempo virtualizable en los tests). La alarma (TimerAlarm, D4) suena
// al llegar a cero y se silencia al descartar el temporizador (cambio de Paso o
// salida del Modo Cocina, RF-GTP5).
//
// La voz (RD24) es opcional y solo en memoria (D9): el ViewModel no conoce Android
// ni recursos, solo los puertos StepSpeaker y VoiceCommandSource (D8). El parser es
// puro y exacto (D7); el permiso, el locale y el ciclo de vida se resuelven en la
// Route (D10).
class CookModeViewModel(
    private val recipeRepository: RecipeRepository,
    private val timerAlarm: TimerAlarm,
    private val stepSpeaker: StepSpeaker,
    private val voiceCommandSource: VoiceCommandSource,
    private val recipeId: String,
    // Raciones heredadas del Detalle (RF-CPP4): null = usar las Raciones base.
    servings: Int?,
) : ViewModel() {

    private val recipe = flow { emit(recipeRepository.getRecipe(recipeId)) }

    // RF-CPP4 y caso límite: las Raciones heredadas fuera de 1-12 se recortan.
    private val requestedServings: Int? = servings?.let(ServingsScaling::clampServings)

    private val stepIndex = MutableStateFlow(0)

    // Último estado del temporizador comprometido por un evento, con el Paso al que
    // pertenece. Al cambiar de Paso se descarta (RF-GTP5) y el estado efectivo vuelve
    // a ser el inicial «Listo» del nuevo Paso.
    private data class StepTimerSnapshot(val stepIndex: Int, val state: StepTimerState)

    private val timerSnapshot = MutableStateFlow<StepTimerSnapshot?>(null)

    private val voiceState = MutableStateFlow(VoiceUiState())

    private val mutableEvents = MutableSharedFlow<CookModeEvent>(extraBufferCapacity = 1)

    val events: SharedFlow<CookModeEvent> = mutableEvents.asSharedFlow()

    private var tickJob: Job? = null

    private var voiceLocale: Locale? = null

    private var voiceJob: Job? = null

    private var speakJob: Job? = null

    init {
        viewModelScope.launch {
            voiceCommandSource.events.collect { event ->
                when (event) {
                    is VoiceSourceEvent.Heard -> handleHeard(event.text)
                    VoiceSourceEvent.PermissionDenied -> onVoicePermissionDenied()
                    VoiceSourceEvent.Unavailable -> onVoiceUnavailable()
                    VoiceSourceEvent.LanguageUnavailable -> onVoiceLanguageUnavailable()
                }
            }
        }
    }

    val uiState: StateFlow<CookModeUiState> = combine(
        recipe,
        stepIndex,
        timerSnapshot,
        voiceState,
    ) { loaded, index, _, voice ->
        if (loaded == null || loaded.steps.isEmpty()) {
            // Sin Pasos el Modo Cocina no existe (D3): la CTA del Detalle está
            // deshabilitada y este estado solo cubre la navegación directa.
            CookModeUiState.NotFound
        } else {
            val activeIndex = index.coerceIn(0, loaded.steps.lastIndex)
            CookModeUiState.Cooking(
                recipe = loaded,
                stepIndex = activeIndex,
                totalSteps = loaded.steps.size,
                ingredientRows = CookModeRules.stepIngredientRows(
                    recipe = loaded,
                    stepIndex = activeIndex,
                    servings = requestedServings ?: loaded.baseServings,
                ),
                timer = effectiveTimer(loaded, activeIndex),
                voice = voice,
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CookModeUiState.Loading,
        )

    // RF-CPP3: la navegación se mantiene dentro de 1..totalSteps (el primer Paso
    // deshabilita «Paso Anterior» y el último pasa el botón primario a «Terminar»).
    // Cambiar de Paso descarta el temporizador y silencia la alarma (RF-GTP5).
    fun next() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        val newIndex = (current.stepIndex + 1).coerceAtMost(current.totalSteps - 1)
        if (newIndex != current.stepIndex) {
            discardTimer()
            stepIndex.value = newIndex
            announceIfEnabled(current.recipe, newIndex)
        }
    }

    fun previous() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        val newIndex = (current.stepIndex - 1).coerceAtLeast(0)
        if (newIndex != current.stepIndex) {
            discardTimer()
            stepIndex.value = newIndex
            announceIfEnabled(current.recipe, newIndex)
        }
    }

    // Activa la voz (D1): primero prepara el TTS y luego arranca la escucha. Si el
    // motor o el reconocimiento no están disponibles, la voz queda apagada con un
    // aviso visible y el Modo Cocina sigue usable (D4).
    fun onVoiceEnabled(locale: Locale) {
        if (voiceState.value.enabled || voiceJob?.isActive == true) return
        voiceLocale = locale
        voiceJob = viewModelScope.launch {
            val prepared = stepSpeaker.prepare(locale)
            if (!isActive) return@launch
            if (!prepared) {
                voiceState.value = VoiceUiState(notice = VoiceNotice.VOICE_UNAVAILABLE)
                return@launch
            }
            val started = voiceCommandSource.start(locale)
            if (!isActive) return@launch
            if (!started) {
                voiceState.value = VoiceUiState(notice = VoiceNotice.COMMANDS_UNAVAILABLE)
                return@launch
            }
            voiceState.value = VoiceUiState(enabled = true)
            announceCurrent()
        }
    }

    fun onVoiceDisabled() {
        voiceJob?.cancel()
        voiceJob = null
        voiceCommandSource.stop()
        stepSpeaker.stop()
        voiceState.value = VoiceUiState()
    }

    fun onVoicePermissionDenied() {
        voiceJob?.cancel()
        voiceJob = null
        voiceCommandSource.stop()
        stepSpeaker.stop()
        voiceState.value = VoiceUiState(notice = VoiceNotice.PERMISSION_DENIED)
    }

    // «Escuchar» (D1): locuta el Paso activo aunque la voz esté apagada; no pide
    // micrófono ni inicia la escucha. Si el TTS falla, avisa y apaga la voz.
    fun onSpeakStep(locale: Locale) {
        if (speakJob?.isActive == true) return
        speakJob = viewModelScope.launch {
            val prepared = stepSpeaker.prepare(locale)
            if (!isActive) return@launch
            if (!prepared) {
                voiceCommandSource.stop()
                stepSpeaker.stop()
                voiceState.value = VoiceUiState(notice = VoiceNotice.VOICE_UNAVAILABLE)
                return@launch
            }
            announceCurrent()
        }
    }

    // Ciclo de vida (D9): al pasar a segundo plano se cortan TTS y micrófono; al
    // volver se reanuda la escucha sin relocutar (D1). El permiso lo comprueba la
    // Route antes de llamar aquí (D10).
    fun onHostStopped() {
        voiceJob?.cancel()
        voiceJob = null
        speakJob?.cancel()
        speakJob = null
        voiceCommandSource.stop()
        stepSpeaker.stop()
    }

    fun onHostStarted() {
        val state = voiceState.value
        if (!state.enabled) return
        val locale = voiceLocale ?: return
        if (!voiceCommandSource.start(locale)) {
            stepSpeaker.stop()
            voiceState.value = state.copy(enabled = false, notice = VoiceNotice.COMMANDS_UNAVAILABLE)
        }
    }

    // Controles del temporizador (RF-GTP3, D2/D7): cada evento transforma el estado
    // efectivo con su transición pura y compromete el resultado.
    fun onTimerStart() = applyTimer(StepTimerRules::start)

    fun onTimerPause() = applyTimer(StepTimerRules::pause)

    fun onTimerResume() = applyTimer(StepTimerRules::resume)

    fun onTimerReset() = applyTimer(StepTimerRules::reset)

    fun onTimerAddMinute() = applyTimer(StepTimerRules::addMinute)

    fun onTimerStop() = applyTimer(StepTimerRules::stopAlarm)

    // Al salir del Modo Cocina el temporizador se descarta y la alarma se silencia
    // (RF-GTP5, D1): la cuenta nunca sigue en segundo plano. Los motores de voz se
    // liberan (D9) y no queda ninguna escucha activa.
    override fun onCleared() {
        voiceJob?.cancel()
        speakJob?.cancel()
        voiceCommandSource.release()
        stepSpeaker.release()
        discardTimer()
        super.onCleared()
    }

    private fun handleHeard(text: String) {
        val state = voiceState.value
        if (!state.enabled) return
        val locale = voiceLocale ?: return
        when (VoiceCommandParser.parse(text, locale.language)) {
            VoiceCommand.NEXT -> onVoiceNext()
            VoiceCommand.REPEAT -> announceCurrent()
            VoiceCommand.PAUSE_TIMER -> onTimerPause()
            null -> Unit
        }
    }

    // «Siguiente» por voz en el último Paso termina (D3), igual que «Terminar»:
    // detiene voz y escucha, apaga el interruptor y emite Finish.
    private fun onVoiceNext() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        if (current.stepIndex >= current.totalSteps - 1) {
            voiceJob?.cancel()
            voiceJob = null
            voiceCommandSource.stop()
            stepSpeaker.stop()
            voiceState.value = VoiceUiState()
            mutableEvents.tryEmit(CookModeEvent.Finish)
        } else {
            next()
        }
    }

    private fun onVoiceUnavailable() {
        voiceJob?.cancel()
        voiceJob = null
        voiceCommandSource.stop()
        stepSpeaker.stop()
        voiceState.value = VoiceUiState(notice = VoiceNotice.COMMANDS_UNAVAILABLE)
    }

    private fun onVoiceLanguageUnavailable() {
        voiceJob?.cancel()
        voiceJob = null
        voiceCommandSource.stop()
        stepSpeaker.stop()
        voiceState.value = VoiceUiState(notice = VoiceNotice.LANGUAGE_NOT_DOWNLOADED)
    }

    private fun announceCurrent() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        announce(current.recipe, current.stepIndex)
    }

    private fun announceIfEnabled(recipe: Recipe, index: Int) {
        if (voiceState.value.enabled) announce(recipe, index)
    }

    private fun announce(recipe: Recipe, index: Int) {
        val step = recipe.steps.getOrNull(index) ?: return
        stepSpeaker.speak(
            StepAnnouncement(
                stepNumber = index + 1,
                text = step.text,
                timerMinutes = step.timerMinutes,
            ),
        )
    }

    // Estado efectivo del temporizador de un Paso: el snapshot si pertenece a ese
    // Paso o, si no, el inicial «Listo» con su meta (D3); null = Paso sin
    // `timerMinutes` (RF-GTP6).
    private fun effectiveTimer(loaded: Recipe, index: Int): StepTimerState? {
        val timerMinutes = loaded.steps[index].timerMinutes ?: return null
        return timerSnapshot.value?.takeIf { it.stepIndex == index }?.state
            ?: StepTimerRules.initial(timerMinutes)
    }

    private fun applyTimer(transform: (StepTimerState) -> StepTimerState) {
        val loaded = (uiState.value as? CookModeUiState.Cooking)?.recipe ?: return
        val index = stepIndex.value.coerceIn(0, loaded.steps.lastIndex)
        val before = effectiveTimer(loaded, index) ?: return
        commitTimer(index, transform(before))
    }

    // Tick de un segundo (D6): solo la cuenta «En marcha» decrece; al llegar a cero
    // la transición pura pasa a «Tiempo cumplido» y aquí suena la alarma (RF-GTP4).
    private fun tickOnce() {
        val snapshot = timerSnapshot.value ?: return
        commitTimer(snapshot.stepIndex, StepTimerRules.tick(snapshot.state))
    }

    private fun commitTimer(index: Int, after: StepTimerState) {
        val before = timerSnapshot.value?.takeIf { it.stepIndex == index }?.state
        timerSnapshot.value = StepTimerSnapshot(index, after)
        // La alarma suena en bucle al entrar en «Tiempo cumplido» y se silencia al
        // salir de él (D8): «Detener», «+1 min» o el descarte del temporizador. La
        // locución se corta antes para que la alarma tenga prioridad (D12).
        if (before?.phase != StepTimerPhase.FINISHED && after.phase == StepTimerPhase.FINISHED) {
            stepSpeaker.stop()
            timerAlarm.start()
        }
        if (before?.phase == StepTimerPhase.FINISHED && after.phase != StepTimerPhase.FINISHED) {
            timerAlarm.stop()
        }
        syncTicker(after.phase)
    }

    // El tick solo corre con la cuenta «En marcha» (D6): un `delay(1000)` por segundo
    // en el viewModelScope, que el scheduler de los tests virtualiza.
    private fun syncTicker(phase: StepTimerPhase) {
        if (phase == StepTimerPhase.RUNNING) {
            if (tickJob == null) {
                tickJob = viewModelScope.launch {
                    while (isActive) {
                        delay(TICK_MILLIS)
                        tickOnce()
                    }
                }
            }
        } else {
            tickJob?.cancel()
            tickJob = null
        }
    }

    private fun discardTimer() {
        timerSnapshot.value = null
        tickJob?.cancel()
        tickJob = null
        timerAlarm.stop()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        const val TICK_MILLIS = 1_000L
    }
}
