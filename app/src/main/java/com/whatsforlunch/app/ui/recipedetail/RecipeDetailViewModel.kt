package com.whatsforlunch.app.ui.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.servings.ServingsScaling
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        // Faltantes obligatorios (RD15) con las cantidades visibles del checklist
        // (D8); vacío cuando la Despensa los cubre todos o no hay obligatorios.
        val missingIngredients: List<RecipeIngredient>,
        // Contador de avisos de éxito: cambia con cada «Añadir a la lista» para que
        // la pantalla muestre el aviso aunque la acción sea idempotente (D5).
        val addToListCount: Int = 0,
    ) : RecipeDetailUiState
}

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): la lectura por id usa
// RecipeRepository.getRecipe, la Coincidencia se recalcula de forma reactiva ante
// cambios de la Despensa (RF-DR2, RD01/RD02/RD10) y las cantidades se escalan con
// las Raciones elegidas según RD03 (RF-ER2).
class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val recipeId: String,
) : ViewModel() {

    private val recipe = flow { emit(recipeRepository.getRecipe(recipeId)) }

    // Selección de Raciones a preparar (D3): vive en memoria del ViewModel, no se
    // persiste y se reinicia al salir de la pantalla. Null = sin tocar el stepper.
    private val requestedServings = MutableStateFlow<Int?>(null)

    // Aviso de éxito de RF-AFL2 (contador monótono, no se persiste).
    private val addToListCount = MutableStateFlow(0)

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipe,
        pantryRepository.observePantryItems(),
        requestedServings,
        addToListCount,
    ) { loaded, pantryItems, requested, addedCount ->
        if (loaded == null) {
            RecipeDetailUiState.NotFound
        } else {
            val servings = ServingsScaling.clampServings(requested ?: loaded.baseServings)
            val ingredientRows = loaded.ingredients.map { ingredient ->
                ingredient.copy(
                    quantity = ServingsScaling.scaleIngredientQuantity(
                        baseQuantity = ingredient.quantity,
                        baseServings = loaded.baseServings,
                        servings = servings,
                    ),
                )
            }
            RecipeDetailUiState.Detail(
                recipe = loaded,
                match = calculateMatch(loaded, pantryItems),
                servings = servings,
                ingredientRows = ingredientRows,
                missingIngredients = ShoppingListRules.missingRequiredIngredients(
                    ingredients = ingredientRows,
                    pantryItems = pantryItems,
                ),
                addToListCount = addedCount,
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

    /**
     * RF-AFL2/D8: crea los ítems de los Faltantes con las cantidades visibles en el
     * checklist y Origen «Para: {Receta}». Repetir la acción es un no-op (D5).
     */
    fun addMissingToShoppingList() {
        val detail = uiState.value as? RecipeDetailUiState.Detail ?: return
        val missing = detail.missingIngredients
        if (missing.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                shoppingListRepository.addMissing(
                    sourceKey = detail.recipe.id,
                    sourceTitle = detail.recipe.title,
                    ingredients = missing,
                )
            }.onSuccess {
                addToListCount.value += 1
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
