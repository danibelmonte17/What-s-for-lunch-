package com.whatsforlunch.app.ui.settings

import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.data.ai.PriorityAiKeyProvider
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.data.settings.LanguagePreferenceStore
import com.whatsforlunch.app.domain.model.AppLanguage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// D3/D4/D10 y escenarios 5-7: trim, rechazo de vacío, estado configurado, borrado,
// persistencia y prioridad de la key del build sobre la guardada.
class SettingsViewModelTest {

    @Test
    fun saveTrimsKeyAndMarksConfiguredWithoutRevealingIt() {
        val store = FakeAiKeyStore()
        val viewModel = viewModel(store)

        viewModel.onKeyChange("  sk-test-123  ")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals("sk-test-123", store.storedKey)
        assertTrue(state.isConfigured)
        assertTrue(state.saved)
        assertEquals("", state.fieldValue)
        assertFalse(state.canSave)
    }

    @Test
    fun blankFieldCannotBeSaved() {
        val store = FakeAiKeyStore()
        val viewModel = viewModel(store)

        viewModel.onKeyChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onSave()

        assertNull(store.storedKey)
        assertFalse(viewModel.uiState.value.isConfigured)
    }

    @Test
    fun deleteClearsConfiguredState() {
        val store = FakeAiKeyStore(storedKey = "sk-test-123")
        val viewModel = viewModel(store)
        assertTrue(viewModel.uiState.value.isConfigured)

        viewModel.onDelete()

        assertNull(store.storedKey)
        val state = viewModel.uiState.value
        assertFalse(state.isConfigured)
        assertTrue(state.cleared)
        assertEquals("", state.fieldValue)
    }

    @Test
    fun configuredStatePersistsAcrossViewModelInstances() {
        val store = FakeAiKeyStore()
        viewModel(store).apply {
            onKeyChange("sk-persisted")
            onSave()
        }

        val reloaded = viewModel(store)

        assertTrue(reloaded.uiState.value.isConfigured)
        assertEquals("", reloaded.uiState.value.fieldValue)
    }

    @Test
    fun storedKeyCanBeDeletedWhenBuildKeyIsNotActive() {
        val store = FakeAiKeyStore(storedKey = "sk-test-123")
        val viewModel = viewModel(store)

        val state = viewModel.uiState.value
        assertTrue(state.isConfigured)
        assertFalse(state.buildKeyActive)
        assertTrue(state.canDelete)
    }

    @Test
    fun buildKeyWinsAndDisablesDelete() {
        val store = FakeAiKeyStore(storedKey = "sk-test-123")
        val viewModel = SettingsViewModel(
            PriorityAiKeyProvider("sk-build", store),
            store,
            buildKeyActive = true,
            appLocaleController = appLocaleController(),
        )

        val state = viewModel.uiState.value
        assertTrue(state.isConfigured)
        assertTrue(state.buildKeyActive)
        assertFalse(state.canDelete)
    }

    @Test
    fun buildKeyConfiguresSettingsEvenWithEmptyStore() {
        val store = FakeAiKeyStore()
        val viewModel = SettingsViewModel(
            PriorityAiKeyProvider("sk-build", store),
            store,
            buildKeyActive = true,
            appLocaleController = appLocaleController(),
        )

        val state = viewModel.uiState.value
        assertTrue(state.isConfigured)
        assertTrue(state.buildKeyActive)
        assertFalse(state.canDelete)
    }

    @Test
    fun deletingStoredKeyKeepsConfiguredWhenBuildKeyIsActive() {
        val store = FakeAiKeyStore(storedKey = "sk-test-123")
        val viewModel = SettingsViewModel(
            PriorityAiKeyProvider("sk-build", store),
            store,
            buildKeyActive = true,
            appLocaleController = appLocaleController(),
        )

        viewModel.onDelete()

        assertNull(store.storedKey)
        assertTrue(viewModel.uiState.value.isConfigured)
        assertTrue(viewModel.uiState.value.cleared)
    }

    // RF-IDI2/RF-IDI3 de la spec de cambiar-idioma-de-la-interfaz: el idioma activo
    // refleja la elección y cambiarlo aplica y persiste en el controller.
    @Test
    fun languageStateReflectsActiveSelection() {
        val controller = appLocaleController(selected = AppLanguage.ENGLISH)
        val viewModel = viewModel(FakeAiKeyStore(), controller)

        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
    }

    @Test
    fun selectingLanguageAppliesAndPersistsIt() {
        val store = FakeLanguagePreferenceStore()
        val controller = appLocaleController(store = store)
        val viewModel = viewModel(FakeAiKeyStore(), controller)

        viewModel.onLanguageSelected(AppLanguage.ENGLISH)

        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
        assertEquals(AppLanguage.ENGLISH, controller.language.value)
        assertEquals(AppLanguage.ENGLISH, store.selected())
    }

    private fun viewModel(
        store: FakeAiKeyStore,
        controller: AppLocaleController = appLocaleController(),
    ): SettingsViewModel =
        SettingsViewModel(
            effectiveKeyProvider = PriorityAiKeyProvider("", store),
            aiKeyStore = store,
            buildKeyActive = false,
            appLocaleController = controller,
        )

    private fun appLocaleController(
        store: FakeLanguagePreferenceStore = FakeLanguagePreferenceStore(),
        selected: AppLanguage? = null,
    ): AppLocaleController {
        selected?.let { store.select(it) }
        return AppLocaleController(store, systemLocale = Locale("es", "ES"))
    }

    private class FakeAiKeyStore(var storedKey: String? = null) : AiKeyStore {
        override fun apiKey(): String? = storedKey

        override fun save(key: String) {
            storedKey = key
        }

        override fun clear() {
            storedKey = null
        }
    }

    private class FakeLanguagePreferenceStore : LanguagePreferenceStore {
        private var selected: AppLanguage? = null

        override fun selected(): AppLanguage? = selected

        override fun select(language: AppLanguage) {
            selected = language
        }
    }
}
