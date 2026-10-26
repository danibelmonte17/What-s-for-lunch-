package com.whatsforlunch.app.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
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
    // Diálogo de alta/edición (RD19): `editing` es el ítem precargado en el
    // diálogo y null indica alta nueva.
    val editorVisible: Boolean = false,
    val editing: ShoppingListItem? = null,
)

// MVVM + StateFlow: la pantalla solo observa, edita con el diálogo y deshace
// borrados. La fusión por nombre normalizado y los Faltantes viven en
// dominio/Room (RD18/RD19).
class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
) : ViewModel() {

    private val pendingUndo = MutableStateFlow<ShoppingListItem?>(null)
    private val editorVisible = MutableStateFlow(false)
    private val editing = MutableStateFlow<ShoppingListItem?>(null)

    val uiState: StateFlow<ShoppingListUiState> = combine(
        repository.observeItems(),
        pendingUndo,
        editorVisible,
        editing,
    ) { items, pending, editorVisible, editing ->
        ShoppingListUiState(
            items = items.sortedWith(
                compareBy({ normalizeName(it.name) }, { it.id }),
            ),
            pendingUndo = pending,
            editorVisible = editorVisible,
            editing = editing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ShoppingListUiState(),
    )

    // Abre el diálogo de alta (RF-AIL1/RF-AIL2).
    fun addItem() {
        editing.value = null
        editorVisible.value = true
    }

    // Abre el diálogo precargado para editar un ítem por fila (RF-AIL4).
    fun editItem(id: String) {
        editing.value = uiState.value.items.firstOrNull { it.id == id }
        editorVisible.value = true
    }

    fun dismissEditor() {
        editorVisible.value = false
        editing.value = null
    }

    // RD19: upsert manual por nombre normalizado (D3). Los datos inválidos no
    // guardan ni cierran el diálogo (RF-AIL5).
    fun saveManualItem(name: String, quantity: Double?, unit: String, aisle: Aisle?) {
        if (!ShoppingListRules.isManualNameValid(name) ||
            !ShoppingListRules.isManualQuantityValid(quantity)
        ) {
            return
        }
        val id = editing.value?.id
        viewModelScope.launch {
            repository.saveManualItem(
                id = id,
                name = name.trim(),
                quantity = quantity,
                unit = unit,
                aisle = aisle,
            )
        }
        dismissEditor()
    }

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
