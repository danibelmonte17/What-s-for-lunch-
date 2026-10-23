package com.whatsforlunch.app.ui.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.servings.ServingsScaling
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// Estado visible del Detalle de Receta (RF-DR2, RF-DR8, RF-ER1/RF-ER4):
// cargando, receta no encontrada (id inexistente o borrada) o la ficha con su
// Coincidencia calculada y las Raciones a preparar.
sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState

    data object NotFound : RecipeDetailUiState

    data class Detail(
        val recipe: Recipe,
        // Null cuando la Receta no tiene ingredientes obligatorios (RF-MB6).
        val match: RecipeMatch?,
        // Raciones a preparar elegidas (1-12, RD03); arranca en las base.
        val servings: Int,
        // Ingredientes con la cantidad escalada para pintar el checklist (D5):
        // conservan id, nombre, unidad y opcionalidad de los ingredientes base.
        val ingredientRows: List<RecipeIngredient>,
    ) : RecipeDetailUiState
}

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): la lectura por id usa
// RecipeRepository.getRecipe, la Coincidencia se recalcula de forma reactiva ante
// cambios de la Despensa (RF-DR2, RD01/RD02/RD10) y las cantidades se escalan con
// las Raciones elegidas según RD03 (RF-ER2).
class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val recipeId: String,
) : ViewModel() {

    private val recipe = flow { emit(recipeRepository.getRecipe(recipeId)) }

    // Selección de Raciones a preparar (D3): vive en memoria del ViewModel, no se
    // persiste y se reinicia al salir de la pantalla. Null = sin tocar el stepper.
    private val requestedServings = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipe,
        pantryRepository.observePantryItems(),
        requestedServings,
    ) { loaded, pantryItems, requested ->
        if (loaded == null) {
            RecipeDetailUiState.NotFound
        } else {
            val servings = ServingsScaling.clampServings(requested ?: loaded.baseServings)
            RecipeDetailUiState.Detail(
                recipe = loaded,
                match = calculateMatch(loaded, pantryItems),
                servings = servings,
                ingredientRows = loaded.ingredients.map { ingredient ->
                    ingredient.copy(
                        quantity = ServingsScaling.scaleIngredientQuantity(
                            baseQuantity = ingredient.quantity,
                            baseServings = loaded.baseServings,
                            servings = servings,
                        ),
                    )
                },
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RecipeDetailUiState.Loading,
        )

    // RF-ER3: la selección se limita a 1-12 en el dominio (verify 2).
    fun setServings(value: Int) {
        requestedServings.value = ServingsScaling.clampServings(value)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
