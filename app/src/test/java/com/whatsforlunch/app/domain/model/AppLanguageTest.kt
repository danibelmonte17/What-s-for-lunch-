package com.whatsforlunch.app.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

// Preselección desde el idioma del sistema (D2 de la spec de
// cambiar-idioma-de-la-interfaz): inglés solo si el sistema es inglés; cualquier
// otro idioma cae a español. languageTag es lo que consumen prompts y voz.
class AppLanguageTest {

    @Test
    fun fromSystemSelectsEnglishOnlyForEnglishSystem() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystem(Locale.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystem(Locale.US))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromSystem(Locale.forLanguageTag("es-ES")))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromSystem(Locale.FRANCE))
    }

    @Test
    fun languageTagIsSimpleCodeForPromptsAndVoice() {
        assertEquals("es", AppLanguage.SPANISH.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
    }

    @Test
    fun spanishLocaleIsSpanishAndEnglishIsEnglish() {
        assertEquals("es", AppLanguage.SPANISH.locale.language)
        assertEquals("en", AppLanguage.ENGLISH.locale.language)
    }
}
