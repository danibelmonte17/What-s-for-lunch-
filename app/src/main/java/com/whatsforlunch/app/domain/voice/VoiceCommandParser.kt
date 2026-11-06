package com.whatsforlunch.app.domain.voice

import java.text.Normalizer
import java.util.Locale

// Parser puro de los comandos de voz (D7): normaliza la transcripción y exige
// la frase completa del idioma activo; no hay subcadenas ni fuzzy matching. El
// vocabulario es el de la app (D5): español o inglés; cualquier otro idioma cae
// a español, y el idioma inactivo se ignora (S8).
object VoiceCommandParser {

    fun parse(transcript: String, language: String): VoiceCommand? {
        val normalized = normalize(transcript)
        if (normalized.isEmpty()) return null
        return when (language.lowercase(Locale.ROOT).substringBefore('-')) {
            "en" -> when (normalized) {
                "next" -> VoiceCommand.NEXT
                "repeat" -> VoiceCommand.REPEAT
                "pause timer" -> VoiceCommand.PAUSE_TIMER
                else -> null
            }

            else -> when (normalized) {
                "siguiente" -> VoiceCommand.NEXT
                "repetir" -> VoiceCommand.REPEAT
                "pausar tiempo" -> VoiceCommand.PAUSE_TIMER
                else -> null
            }
        }
    }

    // Minúsculas, sin tildes, solo letras/dígitos separados por un espacio simple
    // (las mayúsculas y la puntuación no cambian el comando).
    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(NON_ALPHANUMERIC, " ")
            .trim()
            .replace(WHITESPACE, " ")

    private val DIACRITICS = Regex("\\p{M}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
    private val WHITESPACE = Regex("\\s+")
}
