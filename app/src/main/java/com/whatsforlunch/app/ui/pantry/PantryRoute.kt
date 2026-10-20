package com.whatsforlunch.app.ui.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantryRoute(
    onBack: () -> Unit,
    viewModel: PantryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PantryScreen(
        uiState = uiState,
        onBack = onBack,
        onSaveItem = viewModel::saveItem,
        onDeleteItem = viewModel::deleteItem,
        onErrorConsumed = viewModel::clearError,
        onSaveCompletedConsumed = viewModel::clearSaveCompleted,
    )
}
