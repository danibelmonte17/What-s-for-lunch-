package com.whatsforlunch.app.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroup
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroupMode
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.shoppinglist.ShoppingProgress
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Aviso de «Deshacer» activo (patrón de la pantalla): `items` son los ítems
// borrados — una fila o el bloque de «Limpiar comprados» (RF-AGL7) — y `bulk`
// distingue los dos mensajes de aviso.
data class PendingUndo(
    val items: List<ShoppingListItem>,
    val bulk: Boolean,
)

data class ShoppingListUiState(
    val items: List<ShoppingListItem> = emptyList(),
    // Lista agrupada según `mode` (RD20); los ítems multi-fuente se replican en
    // cada grupo de sus fuentes (D2).
    val groups: List<ShoppingGroup> = emptyList(),
    val progress: ShoppingProgress = ShoppingProgress(0, 0, 0),
    // Modo de agrupación en memoria (D1): se reinicia al salir de la pantalla.
    val mode: ShoppingGroupMode = ShoppingGroupMode.BY_AISLE,
    // Ítems pendientes de «Deshacer» tras un borrado; null si no hay aviso activo.
    val pendingUndo: PendingUndo? = null,
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

    private val pendingUndo = MutableStateFlow<PendingUndo?>(null)
    private val editorVisible = MutableStateFlow(false)
    private val editing = MutableStateFlow<ShoppingListItem?>(null)
    private val groupMode = MutableStateFlow(ShoppingGroupMode.BY_AISLE)

    val uiState: StateFlow<ShoppingListUiState> = combine(
        repository.observeItems(),
        pendingUndo,
        editorVisible,
        editing,
        groupMode,
    ) { items, pending, editorVisible, editing, mode ->
        val sorted = items.sortedWith(
            compareBy({ normalizeName(it.name) }, { it.id }),
        )
        ShoppingListUiState(
            items = sorted,
            groups = ShoppingListRules.groupItems(sorted, mode),
            progress = ShoppingListRules.progress(sorted),
            mode = mode,
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
    // guardan ni cierran el diálogo (RF-AIL5). Editar no cambia `completed` (D10).
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
        pendingUndo.value = PendingUndo(items = listOf(item), bulk = false)
        viewModelScope.launch { repository.deleteItem(id) }
    }

    // RD20/RF-AGL2: cambia el modo de agrupación (solo presentación, D1).
    fun setGroupMode(mode: ShoppingGroupMode) {
        groupMode.value = mode
    }

    // RD20/RF-AGL5: alterna el estado de completado (D4: sin snackbar; el
    // «Deshacer» es el propio checkbox reversible).
    fun toggleCompleted(id: String) {
        val item = uiState.value.items.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.setCompleted(id, !item.completed) }
    }

    // RD20/RF-AGL7: borra en bloque los ítems completados con aviso «Deshacer»
    // que los restaura todos con su estado (D7).
    fun clearCompleted() {
        val completedItems = uiState.value.items.filter { it.completed }
        if (completedItems.isEmpty()) return
        pendingUndo.value = PendingUndo(items = completedItems, bulk = true)
        viewModelScope.launch {
            completedItems.forEach { repository.deleteItem(it.id) }
        }
    }

    fun undoDelete() {
        val pending = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { repository.restoreItems(pending.items) }
    }

    fun dismissUndo() {
        pendingUndo.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
