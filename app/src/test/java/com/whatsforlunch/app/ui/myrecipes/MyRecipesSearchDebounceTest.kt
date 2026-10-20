package com.whatsforlunch.app.ui.myrecipes

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipesSearchDebounceTest {

    @Test
    fun searchAppliesOnceThreeHundredMillisecondsAfterTheLastKeystroke() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val repository = InMemoryRecipeRepository()
            repository.saveRecipe(recipe("1", "Pollo al limón", "Pollo"))
            repository.saveRecipe(recipe("2", "Crema de calabaza", "Calabaza"))
            val viewModel = MyRecipesViewModel(repository)
            viewModelStore.put("search", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onSearchQueryChanged("po")
            advanceTimeBy(200)
            viewModel.onSearchQueryChanged("pollo")
            advanceTimeBy(299)
            runCurrent()
            assertEquals("pollo", viewModel.uiState.value.searchQuery)
            assertEquals(listOf("1", "2"), viewModel.uiState.value.visibleRecipes.map { it.id })

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("1"), viewModel.uiState.value.visibleRecipes.map { it.id })
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectingAnotherMomentReplacesTheActiveFilterImmediately() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val repository = InMemoryRecipeRepository()
            repository.saveRecipe(recipe("1", "Pollo al limón", "Pollo", MealMoment.DINNER))
            repository.saveRecipe(recipe("2", "Crema de calabaza", "Calabaza", MealMoment.LUNCH))
            val viewModel = MyRecipesViewModel(repository)
            viewModelStore.put("search", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onMealMomentSelected(MealMoment.DINNER)
            runCurrent()
            assertEquals(MealMoment.DINNER, viewModel.uiState.value.mealMomentFilter)
            assertEquals(listOf("1"), viewModel.uiState.value.visibleRecipes.map { it.id })

            viewModel.onMealMomentSelected(MealMoment.LUNCH)
            runCurrent()
            assertEquals(MealMoment.LUNCH, viewModel.uiState.value.mealMomentFilter)
            assertEquals(listOf("2"), viewModel.uiState.value.visibleRecipes.map { it.id })
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun recipe(
        id: String,
        title: String,
        ingredient: String,
        moment: MealMoment = MealMoment.LUNCH,
    ) = Recipe(
        id = id,
        title = title,
        mealMoment = moment,
        ingredients = listOf(RecipeIngredient(id = "ingredient-$id", name = ingredient)),
    )
}
