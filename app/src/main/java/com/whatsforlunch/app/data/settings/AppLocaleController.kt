package com.whatsforlunch.app.data.settings

import com.whatsforlunch.app.domain.model.AppLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Fuente única del idioma activo de la app (D1/D4 de la spec de
// cambiar-idioma-de-la-interfaz): el estado vive en memoria mientras dura el
// proceso y la elección del usuario persiste en LanguagePreferenceStore. El
// idioma del sistema solo decide el arranque de un proceso nuevo con el store
// vacío; cambiar el idioma del dispositivo en caliente no cambia la app.
class AppLocaleController(
    private val store: LanguagePreferenceStore,
    systemLocale: Locale = Locale.getDefault(),
) {

    private val _language = MutableStateFlow(store.selected() ?: AppLanguage.fromSystem(systemLocale))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun select(language: AppLanguage) {
        store.select(language)
        _language.value = language
    }
}
