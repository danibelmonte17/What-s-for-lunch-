package com.whatsforlunch.app.data.voice

import java.util.Locale
import kotlinx.coroutines.flow.Flow

// Eventos del reconocimiento de voz (D8): una transcripción final, permiso
// insuficiente o servicio no disponible. El texto se parsea en el ViewModel.
sealed interface VoiceSourceEvent {
    data class Heard(val text: String) : VoiceSourceEvent

    data object PermissionDenied : VoiceSourceEvent

    data object Unavailable : VoiceSourceEvent

    /** El motor offline del idioma no está descargado en el dispositivo. */
    data object LanguageUnavailable : VoiceSourceEvent
}

// Puerto de reconocimiento (D8): el ViewModel decide cuándo escuchar y qué
// hacer con cada transcripción; el adaptador encapsula SpeechRecognizer.
interface VoiceCommandSource {
    val events: Flow<VoiceSourceEvent>

    fun start(locale: Locale): Boolean

    fun stop()

    fun release()
}
