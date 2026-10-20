package com.whatsforlunch.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PantrySaveError { InvalidName, InvalidQuantity, DuplicateName, SaveFailed, DeleteFailed }

data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val saveError: PantrySaveError? = null,
    val saveCompleted: Boolean = false,
)

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {
    private val saveError = MutableStateFlow<PantrySaveError?>(null)
    private val saveCompleted = MutableStateFlow(false)

    val uiState: StateFlow<PantryUiState> = combine(
        repository.observePantryItems(),
        saveError,
        saveCompleted,
    ) { items, error, completed ->
        PantryUiState(items = items, saveError = error, saveCompleted = completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), PantryUiState())

    fun saveItem(id: String?, name: String, quantityText: String) {
        saveCompleted.value = false
        saveError.value = when {
            !PantryItemValidation.isNameValid(name) -> PantrySaveError.InvalidName
            !PantryItemValidation.isQuantityValid(quantityText) -> PantrySaveError.InvalidQuantity
            else -> null
        }
        if (saveError.value != null) return

        val quantity = PantryItemValidation.parseQuantity(quantityText)
        val item = PantryItem(id = id ?: UUID.randomUUID().toString(), name = name.trim(), quantity = quantity)
        viewModelScope.launch {
            try {
                when (repository.savePantryItem(item)) {
                    PantrySaveResult.Saved -> {
                        saveError.value = null
                        saveCompleted.value = true
                    }
                    PantrySaveResult.DuplicateName -> saveError.value = PantrySaveError.DuplicateName
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                saveError.value = PantrySaveError.SaveFailed
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                repository.deletePantryItem(id)
                saveError.value = null
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                saveError.value = PantrySaveError.DeleteFailed
            }
        }
    }

    fun clearError() {
        saveError.value = null
    }

    fun clearSaveCompleted() {
        saveCompleted.value = false
    }
}
