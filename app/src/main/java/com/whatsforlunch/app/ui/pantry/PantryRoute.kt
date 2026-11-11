package com.whatsforlunch.app.ui.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantryRoute(
    onScanFood: () -> Unit,
    scrollToTopTick: Int = 0,
    viewModel: PantryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PantryScreen(
        uiState = uiState,
        onScanFood = onScanFood,
        onSaveItem = viewModel::saveItem,
        onQuantityChange = viewModel::setQuantity,
        onSearchChange = viewModel::setSearchQuery,
        onDeleteItem = viewModel::deleteItem,
        onErrorConsumed = viewModel::clearError,
        onSaveCompletedConsumed = viewModel::clearSaveCompleted,
        scrollToTopTick = scrollToTopTick,
    )
}
