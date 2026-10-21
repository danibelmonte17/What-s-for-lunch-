package com.whatsforlunch.app.ui.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// Estado visible del Detalle de Receta (RF-DR2, RF-DR8): cargando, receta no
// encontrada (id inexistente o borrada) o la ficha con su Coincidencia calculada.
sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState

    data object NotFound : RecipeDetailUiState

    data class Detail(
        val recipe: Recipe,
        // Null cuando la Receta no tiene ingredientes obligatorios (RF-MB6).
        val match: RecipeMatch?,
    ) : RecipeDetailUiState
}

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): la lectura por id usa
// RecipeRepository.getRecipe y la Coincidencia se recalcula de forma reactiva ante
// cambios de la Despensa sin salir de la pantalla (RF-DR2, RD01/RD02/RD10).
class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val recipeId: String,
) : ViewModel() {

    private val recipe = flow { emit(recipeRepository.getRecipe(recipeId)) }

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipe,
        pantryRepository.observePantryItems(),
    ) { loaded, pantryItems ->
        if (loaded == null) {
            RecipeDetailUiState.NotFound
        } else {
            RecipeDetailUiState.Detail(
                recipe = loaded,
                match = calculateMatch(loaded, pantryItems),
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RecipeDetailUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
