package com.whatsforlunch.app.ui.settings

import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.data.ai.PriorityAiKeyProvider
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
        )

        viewModel.onDelete()

        assertNull(store.storedKey)
        assertTrue(viewModel.uiState.value.isConfigured)
        assertTrue(viewModel.uiState.value.cleared)
    }

    private fun viewModel(store: FakeAiKeyStore): SettingsViewModel =
        SettingsViewModel(
            effectiveKeyProvider = PriorityAiKeyProvider("", store),
            aiKeyStore = store,
            buildKeyActive = false,
        )

    private class FakeAiKeyStore(var storedKey: String? = null) : AiKeyStore {
        override fun apiKey(): String? = storedKey

        override fun save(key: String) {
            storedKey = key
        }

        override fun clear() {
            storedKey = null
        }
    }
}
