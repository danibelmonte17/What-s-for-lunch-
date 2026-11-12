package com.whatsforlunch.app.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

// Adaptador de SpeechRecognizer (D8, D12): escucha continua con resultados
// finales, reintentos cortos y un máximo de 3 fallos consecutivos. Prefiere el
// reconocimiento offline y usa el idioma de la app (D5). Vive en el hilo
// principal; `stop()` cancela y destruye el reconocedor.
class AndroidVoiceCommandSource(private val context: Context) : VoiceCommandSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableEvents = MutableSharedFlow<VoiceSourceEvent>(
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val events: Flow<VoiceSourceEvent> = mutableEvents

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var language: Locale? = null
    private var failures = 0
    private var restartJob: Job? = null

    override fun start(locale: Locale): Boolean {
        if (listening && language == locale) return true
        stopInternal()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return false
        return try {
            language = locale
            listening = true
            failures = 0
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(listener)
            }
            startListening()
            true
        } catch (_: SecurityException) {
            stopInternal()
            mutableEvents.tryEmit(VoiceSourceEvent.PermissionDenied)
            false
        } catch (_: RuntimeException) {
            stopInternal()
            mutableEvents.tryEmit(VoiceSourceEvent.Unavailable)
            false
        }
    }

    override fun stop() {
        stopInternal()
    }

    override fun release() {
        stopInternal()
        scope.cancel()
    }

    private fun startListening() {
        if (!listening) return
        val engine = recognizer ?: return
        val locale = language ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            engine.startListening(intent)
        } catch (_: SecurityException) {
            fail(VoiceSourceEvent.PermissionDenied)
        } catch (_: RuntimeException) {
            fail(VoiceSourceEvent.Unavailable)
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMillis)
            if (listening) startListening()
        }
    }

    private fun fail(event: VoiceSourceEvent) {
        stopInternal()
        mutableEvents.tryEmit(event)
    }

    private fun stopInternal() {
        listening = false
        restartJob?.cancel()
        restartJob = null
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            if (!listening) return
            failures = 0
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                mutableEvents.tryEmit(VoiceSourceEvent.Heard(text))
            }
            scheduleRestart(RESULT_RESTART_MILLIS)
        }

        override fun onError(error: Int) {
            if (!listening) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> scheduleRestart(NO_MATCH_RESTART_MILLIS)

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    fail(VoiceSourceEvent.PermissionDenied)

                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                -> fail(VoiceSourceEvent.LanguageUnavailable)

                else -> {
                    failures += 1
                    if (failures >= MAX_FAILURES) {
                        fail(VoiceSourceEvent.Unavailable)
                    } else {
                        scheduleRestart(ERROR_RESTART_MILLIS)
                    }
                }
            }
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val EVENT_BUFFER = 16
        const val MAX_FAILURES = 3
        const val RESULT_RESTART_MILLIS = 150L
        const val NO_MATCH_RESTART_MILLIS = 250L
        const val ERROR_RESTART_MILLIS = 1_000L
    }
}
