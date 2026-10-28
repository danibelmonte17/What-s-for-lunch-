package com.whatsforlunch.app.ui.cookmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El id de la
// Receta y las Raciones heredadas del Detalle llegan por ruta
// (cook_mode/{recipeId}?servings={servings}, RF-CPP4) y se inyectan en el
// ViewModel vía Koin.
@Composable
fun CookModeRoute(
    recipeId: String,
    servings: Int?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CookModeViewModel = koinViewModel { parametersOf(recipeId, servings) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CookModeScreen(
        uiState = uiState,
        onBack = onBack,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onFinish = onFinish,
        modifier = modifier,
    )
}
