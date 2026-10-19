package com.whatsforlunch.app.ui.misrecetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.misrecetas.misRecetasStateOf
import com.whatsforlunch.app.domain.misrecetas.filterRecipes
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado visible de Mis Recetas: el estado de la colección y las Recetas guardadas.
data class MisRecetasUiState(
    val state: MisRecetasState = MisRecetasState.Empty,
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val mealMomentFilter: MealMoment? = null,
    val visibleRecipes: List<Recipe> = recipes,
)

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): reglas fuera de la UI.
@OptIn(FlowPreview::class)
class MisRecetasViewModel(repository: RecipeRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val appliedSearchQuery = MutableStateFlow("")
    private val mealMomentFilter = MutableStateFlow<MealMoment?>(null)

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .collect { appliedSearchQuery.value = it }
        }
    }

    val uiState: StateFlow<MisRecetasUiState> = combine(
        repository.observeRecipes(),
        searchQuery,
        appliedSearchQuery,
        mealMomentFilter,
    ) { recipes, currentQuery, appliedQuery, moment ->
        val visibleRecipes = filterRecipes(recipes, appliedQuery, moment)
        MisRecetasUiState(
            state = misRecetasStateOf(recipes, visibleRecipes),
            recipes = recipes,
            searchQuery = currentQuery,
            mealMomentFilter = moment,
            visibleRecipes = visibleRecipes,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MisRecetasUiState(),
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onMealMomentSelected(moment: MealMoment?) {
        mealMomentFilter.value = moment
    }

    fun onResetFilters() {
        searchQuery.value = ""
        appliedSearchQuery.value = ""
        mealMomentFilter.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
