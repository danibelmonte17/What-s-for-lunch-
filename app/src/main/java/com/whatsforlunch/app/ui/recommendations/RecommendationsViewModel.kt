package com.whatsforlunch.app.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.CompatibleRecipe
import com.whatsforlunch.app.domain.match.compatibleRecipes
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Estado de «Recetas con mi Despensa»: Recetas Compatibles (RD02, RF-SDR2) y si la
// Despensa persistida tiene Alimentos (D2, RF-SDR4). La regla de negocio vive en
// dominio; la pantalla solo la muestra.
data class RecommendationsUiState(
    val compatibleRecipes: List<CompatibleRecipe> = emptyList(),
    val hasFoods: Boolean = false,
)

class RecommendationsViewModel(
    pantryRepository: PantryRepository,
    recipeRepository: RecipeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        // RF-SDR3: al cambiar Alimentos o Recetas la lista se recalcula sin reiniciar.
        viewModelScope.launch {
            combine(
                recipeRepository.observeRecipes(),
                pantryRepository.observePantryItems(),
            ) { recipes, pantryItems ->
                RecommendationsUiState(
                    compatibleRecipes = compatibleRecipes(recipes, pantryItems),
                    hasFoods = pantryItems.isNotEmpty(),
                )
            }.collect { state -> _uiState.value = state }
        }
    }
}
