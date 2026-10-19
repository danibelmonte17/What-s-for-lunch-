package com.whatsforlunch.app.ui.misrecetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.misrecetas.misRecetasStateOf
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado visible de Mis Recetas: el estado de la colección y las Recetas guardadas.
data class MisRecetasUiState(
    val state: MisRecetasState = MisRecetasState.Empty,
    val recipes: List<Recipe> = emptyList(),
    val deleteError: Boolean = false,
)

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): reglas fuera de la UI.
class MisRecetasViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val deleteError = MutableStateFlow(false)

    val uiState: StateFlow<MisRecetasUiState> = combine(repository.observeRecipes(), deleteError) { recipes, hasDeleteError ->
        MisRecetasUiState(
            state = misRecetasStateOf(recipes),
            recipes = recipes,
            deleteError = hasDeleteError,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MisRecetasUiState(),
        )

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(id)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                deleteError.value = true
            }
        }
    }

    fun consumeDeleteError() {
        deleteError.value = false
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
