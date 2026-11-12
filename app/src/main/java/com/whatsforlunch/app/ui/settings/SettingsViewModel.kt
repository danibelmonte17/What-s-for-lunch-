package com.whatsforlunch.app.ui.settings

import androidx.lifecycle.ViewModel
import com.whatsforlunch.app.data.ai.AiKeyProvider
import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Estado visible de Ajustes (D3/D4/D10): la key nunca se re-muestra; solo se conoce
// si hay una key efectiva (build o almacén), si manda la del build y el campo.
// `language` es el idioma activo de la interfaz (cambiar-idioma-de-la-interfaz).
data class SettingsUiState(
    val isConfigured: Boolean = false,
    val buildKeyActive: Boolean = false,
    val canDelete: Boolean = false,
    val fieldValue: String = "",
    val canSave: Boolean = false,
    val saved: Boolean = false,
    val cleared: Boolean = false,
    val language: AppLanguage = AppLanguage.SPANISH,
)

// MVVM + StateFlow: la key efectiva la resuelve AiKeyProvider (build con prioridad,
// D8); AiKeyStore sigue siendo el almacén de escritura de Ajustes. La validación es
// local y mínima (recorte y rechazo de vacío, D3); sin comprobaciones de formato ni red.
// El idioma delega en AppLocaleController (aplica y persiste, D1/D2).
class SettingsViewModel(
    private val effectiveKeyProvider: AiKeyProvider,
    private val aiKeyStore: AiKeyStore,
    private val buildKeyActive: Boolean,
    private val appLocaleController: AppLocaleController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onKeyChange(value: String) {
        _uiState.update {
            it.copy(
                fieldValue = value,
                canSave = value.trim().isNotEmpty(),
                saved = false,
                cleared = false,
            )
        }
    }

    fun onSave() {
        val key = _uiState.value.fieldValue.trim()
        if (key.isEmpty()) {
            _uiState.update { it.copy(fieldValue = key, canSave = false) }
            return
        }
        aiKeyStore.save(key)
        _uiState.value = loadState(saved = true)
    }

    fun onDelete() {
        aiKeyStore.clear()
        _uiState.value = loadState(cleared = true)
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    fun consumeCleared() {
        _uiState.update { it.copy(cleared = false) }
    }

    // RF-IDI2/RF-IDI3 (cambiar-idioma-de-la-interfaz): el controller aplica el
    // idioma a toda la composición y lo persiste; aquí solo se refleja en el estado.
    fun onLanguageSelected(language: AppLanguage) {
        appLocaleController.select(language)
        _uiState.update { it.copy(language = language) }
    }

    private fun loadState(saved: Boolean = false, cleared: Boolean = false): SettingsUiState =
        SettingsUiState(
            isConfigured = effectiveKeyProvider.apiKey() != null,
            buildKeyActive = buildKeyActive,
            canDelete = !buildKeyActive && aiKeyStore.apiKey() != null,
            saved = saved,
            cleared = cleared,
            language = appLocaleController.language.value,
        )
}
