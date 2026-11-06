package com.whatsforlunch.app.data.settings

import com.whatsforlunch.app.domain.model.AppLanguage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

// D1/D2 de la spec de cambiar-idioma-de-la-interfaz: la elección persiste en el
// store y solo el store vacío cae al idioma del sistema.
class AppLocaleControllerTest {

    @Test
    fun withoutStoredSelectionUsesSystemLanguage() {
        assertEquals(
            AppLanguage.ENGLISH,
            AppLocaleController(InMemoryLanguagePreferenceStore(), Locale.ENGLISH).language.value,
        )
        assertEquals(
            AppLanguage.SPANISH,
            AppLocaleController(InMemoryLanguagePreferenceStore(), Locale("es", "ES")).language.value,
        )
    }

    @Test
    fun storedSelectionWinsOverSystemLanguage() {
        val store = InMemoryLanguagePreferenceStore()
        store.select(AppLanguage.ENGLISH)

        assertEquals(
            AppLanguage.ENGLISH,
            AppLocaleController(store, Locale("es", "ES")).language.value,
        )
    }

    @Test
    fun selectPersistsAndEmitsImmediately() {
        val store = InMemoryLanguagePreferenceStore()
        val controller = AppLocaleController(store, Locale("es", "ES"))

        controller.select(AppLanguage.ENGLISH)

        assertEquals(AppLanguage.ENGLISH, controller.language.value)
        assertEquals(AppLanguage.ENGLISH, store.selected())
    }
}

private class InMemoryLanguagePreferenceStore : LanguagePreferenceStore {
    private var selected: AppLanguage? = null

    override fun selected(): AppLanguage? = selected

    override fun select(language: AppLanguage) {
        selected = language
    }
}
