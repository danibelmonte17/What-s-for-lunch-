package com.whatsforlunch.app.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.whatsforlunch.app.R
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred

// Adaptador de TextToSpeech (D8, D12): prepara el motor y el idioma una sola vez,
// compone «Paso N. {texto}» y, si el Paso lo tiene, «Temporizador de N minutos»
// (D6). QUEUE_FLUSH corta la locución anterior al cambiar de Paso o repetir.
class AndroidStepSpeaker(private val context: Context) : StepSpeaker {

    private var textToSpeech: TextToSpeech? = null
    private var initialized = false
    private var language: Locale? = null
    private var initStatus: Int? = null
    private var initDeferred: CompletableDeferred<Boolean>? = null

    override suspend fun prepare(locale: Locale): Boolean {
        if (initialized && language == locale) return true
        if (initStatus != null && initStatus != TextToSpeech.SUCCESS) {
            // Un motor que no arrancó no se reutiliza: se libera para reintentar.
            releaseEngine()
            return false
        }
        val engine = textToSpeech ?: createEngine()
        val ready = if (initialized) {
            true
        } else {
            initDeferred?.await() ?: false
        }
        if (!ready) return false
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            return false
        }
        language = locale
        return true
    }

    override fun speak(announcement: StepAnnouncement) {
        val engine = textToSpeech ?: return
        if (!initialized) return
        val stepText = announcement.text.trim().trimEnd('.', '!', '?', ';', ':')
        val stepLine = context.getString(
            R.string.cook_mode_voice_announce_step,
            announcement.stepNumber,
            stepText,
        )
        val timerLine = announcement.timerMinutes
            ?.takeIf { it > 0 }
            ?.let { minutes ->
                context.resources.getQuantityString(
                    R.plurals.cook_mode_voice_announce_timer,
                    minutes,
                    minutes,
                )
            }
        val utterance = listOfNotNull(stepLine, timerLine).joinToString(". ")
        if (utterance.isNotBlank()) {
            engine.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun release() {
        releaseEngine()
        language = null
    }

    private fun createEngine(): TextToSpeech {
        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred
        return TextToSpeech(context) { status ->
            initStatus = status
            initialized = status == TextToSpeech.SUCCESS
            deferred.complete(initialized)
        }.also { textToSpeech = it }
    }

    private fun releaseEngine() {
        initDeferred?.complete(false)
        initDeferred = null
        initStatus = null
        initialized = false
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private companion object {
        const val UTTERANCE_ID = "cook_mode_step"
    }
}
