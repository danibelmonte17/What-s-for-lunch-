package com.whatsforlunch.app.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// RD24 (D5, D7): el parser es exacto, por frase completa y por idioma de la app.
// Español e inglés son el vocabulario; el idioma inactivo se ignora (S8).
class VoiceCommandParserTest {

    @Test
    fun spanishCommandsMatchTheExactPhrase() {
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("Siguiente", "es"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("Repetir", "es"))
        assertEquals(VoiceCommand.PAUSE_TIMER, VoiceCommandParser.parse("Pausar tiempo", "es"))
    }

    @Test
    fun englishCommandsMatchTheExactPhrase() {
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("Next", "en"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("Repeat", "en"))
        assertEquals(VoiceCommand.PAUSE_TIMER, VoiceCommandParser.parse("Pause timer", "en"))
    }

    @Test
    fun normalizationIgnoresCaseAccentsAndPunctuation() {
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("  SIGUIENTE! ", "es"))
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("Siguiente.", "es"))
        assertEquals(VoiceCommand.PAUSE_TIMER, VoiceCommandParser.parse("pausar tiempo", "es"))
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("next,", "en"))
    }

    @Test
    fun substringsAndExtraWordsAreNotCommands() {
        assertNull(VoiceCommandParser.parse("Dale a siguiente", "es"))
        assertNull(VoiceCommandParser.parse("Siguiente paso por favor", "es"))
        assertNull(VoiceCommandParser.parse("please next", "en"))
        assertNull(VoiceCommandParser.parse("pausar", "es"))
        assertNull(VoiceCommandParser.parse("tiempo", "es"))
    }

    @Test
    fun theInactiveLanguageIsIgnored() {
        assertNull(VoiceCommandParser.parse("Next", "es"))
        assertNull(VoiceCommandParser.parse("Siguiente", "en"))
        assertNull(VoiceCommandParser.parse("Pause timer", "es"))
    }

    @Test
    fun otherLanguagesFallBackToSpanish() {
        assertEquals(VoiceCommand.NEXT, VoiceCommandParser.parse("Siguiente", "fr"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommandParser.parse("Repetir", "de-DE"))
    }

    @Test
    fun blankOrPunctuationOnlyTranscriptsAreNotCommands() {
        assertNull(VoiceCommandParser.parse("", "es"))
        assertNull(VoiceCommandParser.parse("   ", "es"))
        assertNull(VoiceCommandParser.parse("...", "es"))
    }
}
