package com.whatsforlunch.app.ui.myrecipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado.
@Composable
fun MyRecipesRoute(
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    onOpenPantryRecipes: () -> Unit = {},
    onOpenPantry: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: MyRecipesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyRecipesScreen(
        uiState = uiState,
        onCreateRecipe = onCreateRecipe,
        onScanPantry = onScanPantry,
        onEditRecipe = onEditRecipe,
        onOpenRecipe = onOpenRecipe,
        onOpenPantryRecipes = onOpenPantryRecipes,
        onOpenPantry = onOpenPantry,
        onOpenSettings = onOpenSettings,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onMealMomentSelected = viewModel::onMealMomentSelected,
        onResetFilters = viewModel::onResetFilters,
        onDeleteRecipe = viewModel::deleteRecipe,
        onDeleteErrorConsumed = viewModel::consumeDeleteError,
    )
}
