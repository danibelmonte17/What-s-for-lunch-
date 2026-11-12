package com.whatsforlunch.app

import com.whatsforlunch.app.data.voice.StepAnnouncement
import com.whatsforlunch.app.data.voice.StepSpeaker
import com.whatsforlunch.app.data.voice.VoiceCommandSource
import com.whatsforlunch.app.data.voice.VoiceSourceEvent
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// Doble del TTS para los E2E del Modo Cocina (D15): registra las locuciones y la
// parada, sin tocar el motor del sistema. `available = false` simula S9 (sin TTS).
internal class FakeStepSpeaker(
    @Volatile var available: Boolean = true,
) : StepSpeaker {
    val announced = mutableListOf<StepAnnouncement>()
    var stopCount = 0
        private set
    var releaseCount = 0
        private set

    @Synchronized
    fun announcedSnapshot(): List<StepAnnouncement> = announced.toList()

    override suspend fun prepare(locale: Locale): Boolean = available

    @Synchronized
    override fun speak(announcement: StepAnnouncement) {
        announced += announcement
    }

    @Synchronized
    override fun stop() {
        stopCount += 1
    }

    @Synchronized
    override fun release() {
        releaseCount += 1
    }
}

// Doble del reconocimiento para los E2E del Modo Cocina (D15): el test emite
// transcripciones o fallos como si los entregara el servicio del sistema.
// `available = false` simula S9 (sin servicio de reconocimiento).
internal class FakeVoiceCommandSource(
    @Volatile var available: Boolean = true,
) : VoiceCommandSource {
    private val mutableEvents = MutableSharedFlow<VoiceSourceEvent>(
        extraBufferCapacity = 16,
    )
    override val events: Flow<VoiceSourceEvent> = mutableEvents

    var startCount = 0
        private set
    var stopCount = 0
        private set
    var releaseCount = 0
        private set

    override fun start(locale: Locale): Boolean {
        startCount += 1
        return available
    }

    override fun stop() {
        stopCount += 1
    }

    override fun release() {
        releaseCount += 1
    }

    fun emit(event: VoiceSourceEvent) {
        mutableEvents.tryEmit(event)
    }
}
