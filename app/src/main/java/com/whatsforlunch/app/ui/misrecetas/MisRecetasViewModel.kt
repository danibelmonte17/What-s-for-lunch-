package com.whatsforlunch.app.ui.misrecetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.misrecetas.misRecetasStateOf
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Estado visible de Mis Recetas.
data class MisRecetasUiState(
    val state: MisRecetasState = MisRecetasState.Empty,
)

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): reglas fuera de la UI.
class MisRecetasViewModel(repository: RecipeRepository) : ViewModel() {

    val uiState: StateFlow<MisRecetasUiState> = repository.observeRecipes()
        .map { recipes -> MisRecetasUiState(state = misRecetasStateOf(recipes)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MisRecetasUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
