package com.whatsforlunch.app.domain.cookmode

import org.junit.Assert.assertEquals
import org.junit.Test

// Transiciones de la máquina de estados del temporizador (RD23, D3/D7): tabla de
// fases de la spec y casos límite (meta 0, ampliaciones acumuladas, ticks fuera
// de «En marcha»).
class StepTimerTest {

    @Test
    fun initialIsReadyWithItsMetaAndNeverStartsAlone() {
        val state = StepTimerRules.initial(5)
        assertEquals(StepTimerPhase.READY, state.phase)
        assertEquals(300, state.originalSeconds)
        assertEquals(300, state.remainingSeconds)
        assertEquals(300, state.totalSeconds)
    }

    @Test
    fun startPauseResumeMoveOnlyBetweenTheirPhases() {
        val running = StepTimerRules.start(StepTimerRules.initial(1))
        assertEquals(StepTimerPhase.RUNNING, running.phase)
        // start solo aplica desde «Listo».
        assertEquals(running, StepTimerRules.start(running))

        val paused = StepTimerRules.pause(running)
        assertEquals(StepTimerPhase.PAUSED, paused.phase)
        assertEquals(paused, StepTimerRules.pause(paused))

        val resumed = StepTimerRules.resume(paused)
        assertEquals(StepTimerPhase.RUNNING, resumed.phase)
        assertEquals(resumed, StepTimerRules.resume(resumed))
    }

    @Test
    fun tickDecrementsOneSecondAndReachesFinishedAtZero() {
        val running = StepTimerRules.start(StepTimerRules.initial(1))
        val afterOne = StepTimerRules.tick(running)
        assertEquals(59, afterOne.remainingSeconds)
        assertEquals(StepTimerPhase.RUNNING, afterOne.phase)

        var state = afterOne
        repeat(58) { state = StepTimerRules.tick(state) }
        assertEquals(1, state.remainingSeconds)

        val finished = StepTimerRules.tick(state)
        assertEquals(0, finished.remainingSeconds)
        assertEquals(StepTimerPhase.FINISHED, finished.phase)
        // El tick en «Tiempo cumplido» no cambia nada.
        assertEquals(finished, StepTimerRules.tick(finished))
    }

    @Test
    fun tickOutsideRunningIsANoOp() {
        val ready = StepTimerRules.initial(1)
        assertEquals(ready, StepTimerRules.tick(ready))
        val paused = StepTimerRules.pause(StepTimerRules.start(ready))
        assertEquals(paused, StepTimerRules.tick(paused))
    }

    @Test
    fun zeroMinuteMetaReachesFinishedAtTheFirstTick() {
        val running = StepTimerRules.start(StepTimerRules.initial(0))
        assertEquals(StepTimerPhase.RUNNING, running.phase)
        assertEquals(0, running.remainingSeconds)

        val finished = StepTimerRules.tick(running)
        assertEquals(StepTimerPhase.FINISHED, finished.phase)
        assertEquals(0, finished.remainingSeconds)
    }

    @Test
    fun addMinuteExtendsBothMetaAndCountdownWithoutChangingThePhase() {
        val ready = StepTimerRules.initial(5)
        val extendedReady = StepTimerRules.addMinute(ready)
        assertEquals(360, extendedReady.totalSeconds)
        assertEquals(360, extendedReady.remainingSeconds)
        assertEquals(StepTimerPhase.READY, extendedReady.phase)

        val running = StepTimerRules.start(ready)
        val extendedRunning = StepTimerRules.addMinute(StepTimerRules.tick(running))
        assertEquals(360, extendedRunning.totalSeconds)
        assertEquals(359, extendedRunning.remainingSeconds)
        assertEquals(StepTimerPhase.RUNNING, extendedRunning.phase)
    }

    @Test
    fun addMinuteFromFinishedResumesTheCountdownAtSixtySeconds() {
        val finished = StepTimerRules.tick(StepTimerRules.start(StepTimerRules.initial(0)))
        assertEquals(StepTimerPhase.FINISHED, finished.phase)

        val resumed = StepTimerRules.addMinute(finished)
        assertEquals(StepTimerPhase.RUNNING, resumed.phase)
        assertEquals(60, resumed.remainingSeconds)
        assertEquals(60, resumed.totalSeconds)
    }

    @Test
    fun resetRestoresTheCurrentMetaAndLeavesItRunning() {
        // Meta 5 min + dos ampliaciones = 7 min: «Reiniciar» usa la meta vigente.
        val extended = StepTimerRules.addMinute(
            StepTimerRules.addMinute(StepTimerRules.tick(StepTimerRules.start(StepTimerRules.initial(5)))),
        )
        val restarted = StepTimerRules.reset(extended)
        assertEquals(StepTimerPhase.RUNNING, restarted.phase)
        assertEquals(420, restarted.remainingSeconds)
        assertEquals(420, restarted.totalSeconds)
    }

    @Test
    fun stopAlarmReturnsToReadyWithTheCurrentMeta() {
        val finished = StepTimerRules.tick(StepTimerRules.start(StepTimerRules.initial(0)))
        val stopped = StepTimerRules.stopAlarm(finished)
        assertEquals(StepTimerPhase.READY, stopped.phase)
        assertEquals(0, stopped.remainingSeconds)
        assertEquals(0, stopped.totalSeconds)
        // Fuera de «Tiempo cumplido» no hace nada.
        assertEquals(stopped, StepTimerRules.stopAlarm(stopped))
    }
}
