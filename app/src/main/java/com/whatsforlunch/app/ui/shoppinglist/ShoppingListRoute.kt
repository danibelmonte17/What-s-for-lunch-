package com.whatsforlunch.app.ui.shoppinglist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado.
@Composable
fun ShoppingListRoute(
    modifier: Modifier = Modifier,
    scrollToTopTick: Int = 0,
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListScreen(
        uiState = uiState,
        onDeleteItem = viewModel::deleteItem,
        onUndoDelete = viewModel::undoDelete,
        onUndoExpired = viewModel::dismissUndo,
        onAddItem = viewModel::addItem,
        onEditItem = viewModel::editItem,
        onDismissEditor = viewModel::dismissEditor,
        onSaveItem = viewModel::saveManualItem,
        onToggleCompleted = viewModel::toggleCompleted,
        onSetGroupMode = viewModel::setGroupMode,
        onClearCompleted = viewModel::clearCompleted,
        scrollToTopTick = scrollToTopTick,
        modifier = modifier,
    )
}
