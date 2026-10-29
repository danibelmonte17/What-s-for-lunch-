package com.whatsforlunch.app.domain.cookmode

// Fases del temporizador del Paso en el Modo Cocina (RF-GTP1 a RF-GTP4, RD23):
// listo para arrancar, cuenta atrás en marcha, pausado y tiempo cumplido (alarma).
enum class StepTimerPhase { READY, RUNNING, PAUSED, FINISHED }

// Estado puro del temporizador del Paso (D6): la meta vigente es la original
// (timerMinutes del Paso) más las ampliaciones de «+1 min» (D7); el reloj muestra
// `remainingSeconds` en mm:ss y el anillo `remainingSeconds / totalSeconds`.
data class StepTimerState(
    val originalSeconds: Int,
    val addedSeconds: Int = 0,
    val remainingSeconds: Int,
    val phase: StepTimerPhase,
) {
    val totalSeconds: Int get() = originalSeconds + addedSeconds
}

// Transiciones puras del temporizador (tabla de fases de la spec, D7): sin
// dependencias de Android ni de la UI (P25). Cada transición solo aplica en su
// fase de origen y en cualquier otro caso devuelve el estado sin cambios.
object StepTimerRules {

    // Estado inicial de un Paso: «Listo» con su meta; nunca arranca solo (D3).
    fun initial(timerMinutes: Int): StepTimerState {
        val seconds = timerMinutes * SECONDS_PER_MINUTE
        return StepTimerState(
            originalSeconds = seconds,
            remainingSeconds = seconds,
            phase = StepTimerPhase.READY,
        )
    }

    fun start(state: StepTimerState): StepTimerState =
        if (state.phase == StepTimerPhase.READY) state.copy(phase = StepTimerPhase.RUNNING) else state

    fun pause(state: StepTimerState): StepTimerState =
        if (state.phase == StepTimerPhase.RUNNING) state.copy(phase = StepTimerPhase.PAUSED) else state

    fun resume(state: StepTimerState): StepTimerState =
        if (state.phase == StepTimerPhase.PAUSED) state.copy(phase = StepTimerPhase.RUNNING) else state

    // «Reiniciar» restablece la cuenta a la meta vigente y la deja «En marcha».
    fun reset(state: StepTimerState): StepTimerState =
        state.copy(remainingSeconds = state.totalSeconds, phase = StepTimerPhase.RUNNING)

    // «+1 min» suma 60 s a la meta vigente y a la cuenta; en «Tiempo cumplido»
    // además reanuda la cuenta (la alarma la silencia el ViewModel).
    fun addMinute(state: StepTimerState): StepTimerState = state.copy(
        addedSeconds = state.addedSeconds + SECONDS_PER_MINUTE,
        remainingSeconds = state.remainingSeconds + SECONDS_PER_MINUTE,
        phase = if (state.phase == StepTimerPhase.FINISHED) StepTimerPhase.RUNNING else state.phase,
    )

    // Un tick en «En marcha» resta un segundo y al llegar a cero pasa a
    // «Tiempo cumplido» (la alarma la dispara el ViewModel). En las demás fases
    // no hace nada.
    fun tick(state: StepTimerState): StepTimerState {
        if (state.phase != StepTimerPhase.RUNNING) return state
        val remaining = state.remainingSeconds - 1
        return if (remaining <= 0) {
            state.copy(remainingSeconds = 0, phase = StepTimerPhase.FINISHED)
        } else {
            state.copy(remainingSeconds = remaining)
        }
    }

    // «Detener» abandona «Tiempo cumplido» y vuelve a «Listo» con la meta vigente.
    fun stopAlarm(state: StepTimerState): StepTimerState =
        if (state.phase == StepTimerPhase.FINISHED) {
            state.copy(remainingSeconds = state.totalSeconds, phase = StepTimerPhase.READY)
        } else {
            state
        }

    private const val SECONDS_PER_MINUTE = 60
}
