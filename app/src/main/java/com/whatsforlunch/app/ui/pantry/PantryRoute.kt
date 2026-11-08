package com.whatsforlunch.app.ui.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsforlunch.app.ui.nav.AppTab
import org.koin.androidx.compose.koinViewModel

@Composable
fun PantryRoute(
    onTabSelected: (AppTab) -> Unit = {},
    viewModel: PantryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PantryScreen(
        uiState = uiState,
        onSaveItem = viewModel::saveItem,
        onDeleteItem = viewModel::deleteItem,
        onErrorConsumed = viewModel::clearError,
        onSaveCompletedConsumed = viewModel::clearSaveCompleted,
        onTabSelected = onTabSelected,
    )
}
