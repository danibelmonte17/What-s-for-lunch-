package com.whatsforlunch.app.domain.model

import java.util.Locale

// Idioma de la interfaz (P19): solo español e inglés (D2 de la spec de
// cambiar-idioma-de-la-interfaz). `locale` es el que se aplica a los recursos
// (values/values-es no existe: español es el fallback de values/). `fromSystem`
// fija la preselección del primer arranque: inglés solo si el sistema es inglés;
// cualquier otro idioma cae a español (D2).
enum class AppLanguage(val locale: Locale) {
    SPANISH(Locale.forLanguageTag("es-ES")),
    ENGLISH(Locale.ENGLISH),
    ;

    /** Código de idioma simple (`es` / `en`), el que consumen prompts y voz. */
    val languageTag: String
        get() = locale.language

    companion object {
        fun fromSystem(locale: Locale): AppLanguage =
            if (locale.language.lowercase(Locale.ROOT) == "en") ENGLISH else SPANISH
    }
}
