package com.whatsforlunch.app.ui.cookmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.cookmode.CookModeRules
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.servings.ServingsScaling
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// Estado visible del Modo Cocina (RF-CPP1 a RF-CPP4, RD22): cargando, Receta no
// encontrada o la cocción con el Paso activo y sus Ingredientes ya escalados.
sealed interface CookModeUiState {
    data object Loading : CookModeUiState

    data object NotFound : CookModeUiState

    data class Cooking(
        val recipe: Recipe,
        // Índice del Paso activo (0-based; la UI muestra «Paso X de Y» con X+1).
        val stepIndex: Int,
        val totalSteps: Int,
        // Ingredientes vinculados del Paso activo (RD21) con la cantidad escalada a
        // las Raciones heredadas del Detalle (RD03); vacío = «Sin ingredientes» (D4).
        val ingredientRows: List<RecipeIngredient>,
    ) : CookModeUiState
}

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1). El Paso activo vive solo en
// memoria (D7): sobrevive a la rotación (mismo ViewModel) y se reinicia al crear el
// ViewModel al reabrir el Modo Cocina. Solo lee la Receta: no escribe en ningún
// repositorio ni contiene reglas de negocio (D9).
class CookModeViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeId: String,
    // Raciones heredadas del Detalle (RF-CPP4): null = usar las Raciones base.
    servings: Int?,
) : ViewModel() {

    private val recipe = flow { emit(recipeRepository.getRecipe(recipeId)) }

    // RF-CPP4 y caso límite: las Raciones heredadas fuera de 1-12 se recortan.
    private val requestedServings: Int? = servings?.let(ServingsScaling::clampServings)

    private val stepIndex = MutableStateFlow(0)

    val uiState: StateFlow<CookModeUiState> = combine(recipe, stepIndex) { loaded, index ->
        if (loaded == null || loaded.steps.isEmpty()) {
            // Sin Pasos el Modo Cocina no existe (D3): la CTA del Detalle está
            // deshabilitada y este estado solo cubre la navegación directa.
            CookModeUiState.NotFound
        } else {
            val activeIndex = index.coerceIn(0, loaded.steps.lastIndex)
            CookModeUiState.Cooking(
                recipe = loaded,
                stepIndex = activeIndex,
                totalSteps = loaded.steps.size,
                ingredientRows = CookModeRules.stepIngredientRows(
                    recipe = loaded,
                    stepIndex = activeIndex,
                    servings = requestedServings ?: loaded.baseServings,
                ),
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CookModeUiState.Loading,
        )

    // RF-CPP3: la navegación se mantiene dentro de 1..totalSteps (el primer Paso
    // deshabilita «Paso Anterior» y el último pasa el botón primario a «Terminar»).
    fun next() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        stepIndex.value = (current.stepIndex + 1).coerceAtMost(current.totalSteps - 1)
    }

    fun previous() {
        val current = uiState.value as? CookModeUiState.Cooking ?: return
        stepIndex.value = (current.stepIndex - 1).coerceAtLeast(0)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
