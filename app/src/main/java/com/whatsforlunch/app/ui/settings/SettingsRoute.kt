package com.whatsforlunch.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel de Ajustes con la pantalla sin estado.
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onKeyChange = viewModel::onKeyChange,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        onSavedConsumed = viewModel::consumeSaved,
    )
}
