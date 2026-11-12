package com.whatsforlunch.app.ui.myrecipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.myrecipes.filterRecipes
import com.whatsforlunch.app.domain.myrecipes.myRecipesStateOf
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.PantryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado visible de Mi recetario: el estado de la colección, las Recetas guardadas,
// la búsqueda/filtro activos (momento y Favoritas), el aviso de error de borrado y
// si la Despensa tiene datos (RF-MOB3, enmienda 2026-11-11: condiciona el FAB
// «Recetas con mi Despensa»).
data class MyRecipesUiState(
    val state: MyRecipesState = MyRecipesState.Empty,
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val mealMomentFilter: MealMoment? = null,
    val favoritesOnly: Boolean = false,
    val visibleRecipes: List<Recipe> = recipes,
    val deleteError: Boolean = false,
    val matches: Map<String, RecipeMatch> = emptyMap(),
    val hasFoods: Boolean = false,
)

// MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1): reglas fuera de la UI.
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MyRecipesViewModel(
    private val repository: RecipeRepository,
    private val pantryRepository: PantryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val appliedSearchQuery = MutableStateFlow("")
    private val mealMomentFilter = MutableStateFlow<MealMoment?>(null)
    private val favoritesOnly = MutableStateFlow(false)
    private val deleteError = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .collect { appliedSearchQuery.value = it }
        }
    }

    // Al volver a Mi recetario (ON_RESUME del route → `refresh()`) cada emisión
    // cancela la colección anterior y vuelve a leer Recetas y Despensa: la vista
    // se reconstruye con los datos actuales si ha habido cambios.
    private val refreshTick = MutableStateFlow(0)

    private val recipesAndPantry = refreshTick.flatMapLatest {
        combine(
            repository.observeRecipes(),
            pantryRepository.observePantryItems(),
        ) { recipes, pantryItems -> recipes to pantryItems }
    }

    // D3/RF-EBF7: el chip «Favoritas» se combina por AND con la búsqueda y el momento.
    private val activeFilters = combine(
        searchQuery,
        appliedSearchQuery,
        mealMomentFilter,
        favoritesOnly,
    ) { currentQuery, appliedQuery, moment, favorites ->
        FilterState(currentQuery, appliedQuery, moment, favorites)
    }

    val uiState: StateFlow<MyRecipesUiState> = combine(
        recipesAndPantry,
        activeFilters,
        deleteError,
    ) { (recipes, pantryItems), filters, hasDeleteError ->
        val visibleRecipes = filterRecipes(
            recipes,
            filters.appliedQuery,
            filters.moment,
            filters.favorites,
        )
        MyRecipesUiState(
            state = myRecipesStateOf(recipes, visibleRecipes),
            recipes = recipes,
            searchQuery = filters.currentQuery,
            mealMomentFilter = filters.moment,
            favoritesOnly = filters.favorites,
            visibleRecipes = visibleRecipes,
            deleteError = hasDeleteError,
            matches = recipes.mapNotNull { recipe ->
                calculateMatch(recipe, pantryItems)?.let { recipe.id to it }
            }.toMap(),
            hasFoods = pantryItems.isNotEmpty(),
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

    fun onFavoriteFilterToggle() {
        favoritesOnly.value = !favoritesOnly.value
    }

    fun onResetFilters() {
        searchQuery.value = ""
        appliedSearchQuery.value = ""
        mealMomentFilter.value = null
        favoritesOnly.value = false
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

    // Refresco al volver a la vista: fuerza una consulta nueva a los repositorios
    // para que la lista, las Coincidencias, el estado vacío y los FABs se
    // actualicen con la información vigente (aunque los flujos de Room ya son
    // reactivos, esto garantiza la lectura en cada ON_RESUME).
    fun refresh() {
        refreshTick.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

// Filtros activos de Mi recetario agrupados para combinarlos en un solo flujo.
private data class FilterState(
    val currentQuery: String,
    val appliedQuery: String,
    val moment: MealMoment?,
    val favorites: Boolean,
)
