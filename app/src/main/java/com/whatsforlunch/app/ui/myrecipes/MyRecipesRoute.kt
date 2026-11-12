package com.whatsforlunch.app.ui.myrecipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado.
@Composable
fun MyRecipesRoute(
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    onScanPantryFromGallery: () -> Unit = {},
    onOpenPantryRecipes: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    scrollToTopTick: Int = 0,
    viewModel: MyRecipesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresco al volver a la vista: el ciclo ON_RESUME del destino de
    // navegación (cambio de pestaña o vuelta de una pantalla empujada) dispara
    // `refresh()`, que relee Recetas y Despensa y reconstruye la vista completa.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MyRecipesScreen(
        uiState = uiState,
        onCreateRecipe = onCreateRecipe,
        onScanPantry = onScanPantry,
        onScanPantryFromGallery = onScanPantryFromGallery,
        onEditRecipe = onEditRecipe,
        onOpenRecipe = onOpenRecipe,
        onOpenPantryRecipes = onOpenPantryRecipes,
        onOpenSettings = onOpenSettings,
        scrollToTopTick = scrollToTopTick,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onMealMomentSelected = viewModel::onMealMomentSelected,
        onFavoriteFilterToggle = viewModel::onFavoriteFilterToggle,
        onResetFilters = viewModel::onResetFilters,
        onDeleteRecipe = viewModel::deleteRecipe,
        onDeleteErrorConsumed = viewModel::consumeDeleteError,
    )
}
