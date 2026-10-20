package com.whatsforlunch.app.ui.myrecipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.myrecipes.filterRecipes
import com.whatsforlunch.app.domain.myrecipes.myRecipesStateOf
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado visible de Mis Recetas: el estado de la colección, las Recetas guardadas,
// la búsqueda/filtro activos y el aviso de error de borrado.
data class MyRecipesUiState(
    val state: MyRecipesState = MyRecipesState.Empty,
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val mealMomentFilter: MealMoment? = null,
    val visibleRecipes: List<Recipe> = recipes,
    val deleteError: Boolean = false,
)

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): reglas fuera de la UI.
@OptIn(FlowPreview::class)
class MyRecipesViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val appliedSearchQuery = MutableStateFlow("")
    private val mealMomentFilter = MutableStateFlow<MealMoment?>(null)
    private val deleteError = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .collect { appliedSearchQuery.value = it }
        }
    }

    val uiState: StateFlow<MyRecipesUiState> = combine(
        repository.observeRecipes(),
        searchQuery,
        appliedSearchQuery,
        mealMomentFilter,
        deleteError,
    ) { recipes, currentQuery, appliedQuery, moment, hasDeleteError ->
        val visibleRecipes = filterRecipes(recipes, appliedQuery, moment)
        MyRecipesUiState(
            state = myRecipesStateOf(recipes, visibleRecipes),
            recipes = recipes,
            searchQuery = currentQuery,
            mealMomentFilter = moment,
            visibleRecipes = visibleRecipes,
            deleteError = hasDeleteError,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MyRecipesUiState(),
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
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
