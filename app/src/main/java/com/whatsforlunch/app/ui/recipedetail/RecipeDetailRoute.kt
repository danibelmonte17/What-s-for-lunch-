package com.whatsforlunch.app.ui.recipedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El id de la
// Receta llega por ruta (RF-DR1) y se inyecta en el ViewModel vía Koin.
@Composable
fun RecipeDetailRoute(
    recipeId: String,
    onBack: () -> Unit,
    onStartCooking: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = koinViewModel { parametersOf(recipeId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onStartCooking = onStartCooking,
        onServingsChange = viewModel::setServings,
        modifier = modifier,
    )
}
