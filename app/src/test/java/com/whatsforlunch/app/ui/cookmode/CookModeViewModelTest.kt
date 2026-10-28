package com.whatsforlunch.app.ui.cookmode

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-CPP3 y RF-CPP4 (RD22): entrada en el Paso 1, navegación con límites, Raciones
// heredadas y estados NotFound.
@OptIn(ExperimentalCoroutinesApi::class)
class CookModeViewModelTest {

    @Test
    fun cookingStartsAtFirstStepWithRowsScaledToTheInheritedServings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(recipes, "tortilla", servings = 2)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            assertEquals(350.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun withoutServingsArgumentTheRowsUseBaseServings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(recipes, "tortilla", servings = null)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(700.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun inheritedServingsOutOfRangeAreClamped() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            // 13 se recorta a 12: 700 × (12 / 4) = 2100.
            val viewModel = CookModeViewModel(recipes, "tortilla", servings = 13)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(2100.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun nextAndPreviousStayWithinTheStepRange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(recipes, "tortilla", servings = 2)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.previous()
            runCurrent()
            assertEquals(0, (viewModel.uiState.value as CookModeUiState.Cooking).stepIndex)

            viewModel.next()
            runCurrent()
            val last = viewModel.uiState.value as CookModeUiState.Cooking
            // Último Paso: el índice se mantiene y el botón primario pasa a «Terminar».
            assertEquals(1, last.stepIndex)
            assertEquals(1, last.totalSteps - 1)
            assertTrue(last.ingredientRows.isEmpty())

            viewModel.next()
            runCurrent()
            assertEquals(1, (viewModel.uiState.value as CookModeUiState.Cooking).stepIndex)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unknownRecipeShowsNotFound() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val viewModel = CookModeViewModel(InMemoryRecipeRepository(), "missing", servings = null)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertEquals(CookModeUiState.NotFound, viewModel.uiState.value)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun recipeWithoutStepsShowsNotFoundBecauseCookingModeDoesNotExistForIt() = runTest {
        // D3: la CTA del Detalle está deshabilitada con 0 Pasos; este estado solo
        // cubre la navegación directa a la ruta del Modo Cocina.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(Recipe(id = "draft", title = "Borrador vacío"))

            val viewModel = CookModeViewModel(recipes, "draft", servings = null)
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertEquals(CookModeUiState.NotFound, viewModel.uiState.value)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun twoStepRecipe() = Recipe(
        id = "tortilla",
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = listOf(
            RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
            RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
        ),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", ingredientIds = listOf("potato")),
            RecipeStep(id = "s2", text = "Cuece la tortilla"),
        ),
    )
}
