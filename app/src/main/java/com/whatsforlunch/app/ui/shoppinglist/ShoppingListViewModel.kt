package com.whatsforlunch.app.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val items: List<ShoppingListItem> = emptyList(),
    // Ítem pendiente de «Deshacer» tras un borrado; null si no hay aviso activo.
    val pendingUndo: ShoppingListItem? = null,
)

// MVVM + StateFlow: la pantalla solo observa, borra y deshace. La fusión de los
// Faltantes vive en dominio/Room (RD18).
class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
) : ViewModel() {

    private val pendingUndo = MutableStateFlow<ShoppingListItem?>(null)

    val uiState: StateFlow<ShoppingListUiState> = combine(
        repository.observeItems(),
        pendingUndo,
    ) { items, pending ->
        ShoppingListUiState(
            items = items.sortedWith(
                compareBy({ normalizeName(it.name) }, { it.id }),
            ),
            pendingUndo = pending,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ShoppingListUiState(),
    )

    fun deleteItem(id: String) {
        val item = uiState.value.items.firstOrNull { it.id == id } ?: return
        pendingUndo.value = item
        viewModelScope.launch { repository.deleteItem(id) }
    }

    fun undoDelete() {
        val item = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { repository.restoreItem(item) }
    }

    fun dismissUndo() {
        pendingUndo.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
