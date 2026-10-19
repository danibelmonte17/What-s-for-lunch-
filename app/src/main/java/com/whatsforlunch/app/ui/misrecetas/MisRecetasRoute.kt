package com.whatsforlunch.app.ui.misrecetas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado.
@Composable
fun MisRecetasRoute(
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit,
    viewModel: MisRecetasViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MisRecetasScreen(
        uiState = uiState,
        onCreateRecipe = onCreateRecipe,
        onScanPantry = onScanPantry,
        onEditRecipe = onEditRecipe,
        onDeleteRecipe = viewModel::deleteRecipe,
        onDeleteErrorConsumed = viewModel::consumeDeleteError,
    )
}
