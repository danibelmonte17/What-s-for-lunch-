package com.whatsforlunch.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.pantry.filterPantryItems
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PantrySaveError { InvalidName, DuplicateName, SaveFailed, DeleteFailed }

data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val searchQuery: String = "",
    val visibleItems: List<PantryItem> = emptyList(),
    val saveError: PantrySaveError? = null,
    val saveCompleted: Boolean = false,
)

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {
    private val saveError = MutableStateFlow<PantrySaveError?>(null)
    private val saveCompleted = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PantryUiState> = combine(
        repository.observePantryItems(),
        searchQuery,
        saveError,
        saveCompleted,
    ) { items, query, error, completed ->
        // `items` alimenta los contadores de cabecera/resumen y `visibleItems`
        // la lista filtrada por la búsqueda en vivo (D18).
        PantryUiState(
            items = items,
            searchQuery = query,
            visibleItems = filterPantryItems(items, query),
            saveError = error,
            saveCompleted = completed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), PantryUiState())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun saveItem(id: String?, name: String, quantity: Int, expiryDate: LocalDate?) {
        saveCompleted.value = false
        saveError.value = when {
            !PantryItemValidation.isNameValid(name) -> PantrySaveError.InvalidName
            else -> null
        }
        if (saveError.value != null) return

        val item = PantryItem(
            id = id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            quantity = quantity.takeUnless { it == PantryItemValidation.MIN_QUANTITY },
            expiryDate = expiryDate,
        )
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

    // Stepper inline de la tarjeta (D15): cada toque valida con
    // `PantryItemValidation` (0..99) y persiste al momento, sin diálogo; 0 se
    // guarda como «sin cantidad».
    fun setQuantity(id: String, quantity: Int) {
        if (!PantryItemValidation.isQuantityValid(quantity)) return
        viewModelScope.launch {
            try {
                val item = repository.observePantryItems().first().find { it.id == id } ?: return@launch
                repository.savePantryItem(
                    item.copy(quantity = quantity.takeUnless { it == PantryItemValidation.MIN_QUANTITY }),
                )
                saveError.value = null
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
