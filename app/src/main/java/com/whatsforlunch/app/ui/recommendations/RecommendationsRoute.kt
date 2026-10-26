package com.whatsforlunch.app.ui.recommendations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado (RF-SDR1).
@Composable
fun RecommendationsRoute(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSuggestWithAi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecommendationsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendationsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenRecipe = onOpenRecipe,
        onSuggestWithAi = onSuggestWithAi,
        onAddMissingToShoppingList = viewModel::addMissingToShoppingList,
        modifier = modifier,
    )
}
