package com.whatsforlunch.app.data.settings

import android.content.Context
import androidx.core.content.edit
import com.whatsforlunch.app.domain.model.AppLanguage

// Persistencia de la elección de idioma en SharedPreferences (mismo patrón que
// KeystoreAiKeyStore, sin cifrado porque el idioma no es sensible). Un valor
// almacenado desconocido se ignora como si no hubiera elección.
class SharedPrefsLanguagePreferenceStore(context: Context) : LanguagePreferenceStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun selected(): AppLanguage? =
        preferences.getString(KEY_LANGUAGE, null)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }

    override fun select(language: AppLanguage) {
        preferences.edit { putString(KEY_LANGUAGE, language.name) }
    }

    private companion object {
        const val PREFS_NAME = "language_prefs"
        const val KEY_LANGUAGE = "app_language"
    }
}
