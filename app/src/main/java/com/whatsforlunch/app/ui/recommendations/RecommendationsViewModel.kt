package com.whatsforlunch.app.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.CompatibleRecipe
import com.whatsforlunch.app.domain.match.compatibleRecipes
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Estado de «Recetas con mi Despensa»: Recetas Compatibles (RD02, RF-SDR2) y si la
// Despensa persistida tiene Alimentos (D2, RF-SDR4). La regla de negocio vive en
// dominio; la pantalla solo la muestra. `missingByRecipe` guarda los Faltantes con
// sus cantidades base (D8) y queda vacío para una tarjeta ya añadida (RF-AFL3).
data class RecommendationsUiState(
    val compatibleRecipes: List<CompatibleRecipe> = emptyList(),
    val hasFoods: Boolean = false,
    val missingByRecipe: Map<String, List<RecipeIngredient>> = emptyMap(),
)

class RecommendationsViewModel(
    pantryRepository: PantryRepository,
    recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    // Tarjetas a las que ya se añadieron sus Faltantes en esta visita: la línea
    // «Te falta: …» desaparece sin alterar la Coincidencia (RF-AFL3).
    private val addedRecipeIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        // RF-SDR3: al cambiar Alimentos o Recetas la lista se recalcula sin reiniciar.
        viewModelScope.launch {
            combine(
                recipeRepository.observeRecipes(),
                pantryRepository.observePantryItems(),
                addedRecipeIds,
            ) { recipes, pantryItems, added ->
                val compatible = compatibleRecipes(recipes, pantryItems)
                RecommendationsUiState(
                    compatibleRecipes = compatible,
                    hasFoods = pantryItems.isNotEmpty(),
                    missingByRecipe = compatible.associate { item ->
                        val id = item.recipe.id
                        id to if (id in added) {
                            emptyList()
                        } else {
                            ShoppingListRules.missingRequiredIngredients(
                                ingredients = item.recipe.ingredients,
                                pantryItems = pantryItems,
                            )
                        }
                    },
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    // RF-AFL3: añade los Faltantes de la tarjeta con cantidades base; repetir es un
    // no-op en el repositorio y la tarjeta oculta la línea tras la primera vez.
    fun addMissingToShoppingList(recipeId: String) {
        val recipe = _uiState.value.compatibleRecipes
            .firstOrNull { it.recipe.id == recipeId }
            ?.recipe
            ?: return
        val missing = _uiState.value.missingByRecipe[recipeId].orEmpty()
        if (missing.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                shoppingListRepository.addMissing(
                    sourceKey = recipe.id,
                    sourceTitle = recipe.title,
                    ingredients = missing,
                )
            }.onSuccess {
                addedRecipeIds.value = addedRecipeIds.value + recipe.id
            }
        }
    }
}
