package com.whatsforlunch.app.data.voice

import java.util.Locale

// Datos mínimos de la locución de un Paso (D6): número, texto y, si lo tiene,
// minutos del temporizador. No incluye ingredientes.
data class StepAnnouncement(
    val stepNumber: Int,
    val text: String,
    val timerMinutes: Int?,
)

// Puerto de síntesis de voz (D8): el ViewModel no conoce Android ni recursos;
// el adaptador compone la frase y habla con el motor del sistema.
interface StepSpeaker {
    suspend fun prepare(locale: Locale): Boolean

    fun speak(announcement: StepAnnouncement)

    fun stop()

    fun release()
}
