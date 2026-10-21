package com.whatsforlunch.app.ui.recipedetail

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import java.util.Locale
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-DR2, RF-DR4 y RF-DR8: estado del Detalle, Coincidencia reactiva y formato D7.
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    @Test
    fun unknownRecipeIdShowsNotFoundState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val viewModel = RecipeDetailViewModel(
                recipeRepository = InMemoryRecipeRepository(),
                pantryRepository = InMemoryPantryRepository(),
                recipeId = "missing",
            )
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertEquals(RecipeDetailUiState.NotFound, viewModel.uiState.value)
        } finally {
           viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun detailShowsMatchPercentageFromRequiredIngredients() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            val pantry = InMemoryPantryRepository()
            recipes.saveRecipe(
                recipe(
                    id = "pollo-arroz",
                    ingredients = listOf(
                        RecipeIngredient(id = "chicken", name = "Pollo"),
                        RecipeIngredient(id = "rice", name = "Arroz"),
                        RecipeIngredient(id = "lemon", name = "Limón", optional = true),
                    ),
                ),
            )
            pantry.savePantryItem(PantryItem("chicken", "Pollo"))

            val viewModel = RecipeDetailViewModel(recipes, pantry, "pollo-arroz")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is RecipeDetailUiState.Detail)
            state as RecipeDetailUiState.Detail
            assertEquals("pollo-arroz", state.recipe.id)
            assertEquals(50, state.match?.percentage)
        } finally {
           viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun matchReactsToPantryChangesWithoutLeavingTheScreen() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            val pantry = InMemoryPantryRepository()
            recipes.saveRecipe(
                recipe(
                    id = "partial",
                    ingredients = listOf(
                        RecipeIngredient(id = "chicken", name = "Pollo"),
                        RecipeIngredient(id = "rice", name = "Arroz"),
                    ),
                ),
            )
            pantry.savePantryItem(PantryItem("chicken", "Pollo"))

            val viewModel = RecipeDetailViewModel(recipes, pantry, "partial")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            assertEquals(50, (viewModel.uiState.value as RecipeDetailUiState.Detail).match?.percentage)

            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("rice", "Arroz")))
            runCurrent()

            val updated = viewModel.uiState.value as RecipeDetailUiState.Detail
            assertEquals(100, updated.match?.percentage)
            assertTrue(updated.match?.isFullMatch == true)
        } finally {
           viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun recipeWithoutRequiredIngredientsHasNoMatch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(
                recipe(
                    id = "optional-only",
                    ingredients = listOf(RecipeIngredient(id = "salt", name = "Sal", optional = true)),
                ),
            )

            val viewModel = RecipeDetailViewModel(recipes, InMemoryPantryRepository(), "optional-only")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as RecipeDetailUiState.Detail
            assertNull(state.match)
        } finally {
           viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyPantryShowsZeroPercentBadge() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(
                recipe(
                    id = "empty-pantry",
                    ingredients = listOf(RecipeIngredient(id = "tomato", name = "Tomate")),
                ),
            )

            val viewModel = RecipeDetailViewModel(recipes, InMemoryPantryRepository(), "empty-pantry")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as RecipeDetailUiState.Detail
            assertEquals(0, state.match?.percentage)
        } finally {
           viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun quantityFormatUsesActiveLocaleWithoutTrailingZeros() {
        // D7: separador decimal del idioma activo, sin ceros sobrantes.
        assertEquals("0,5", formatQuantity(0.5, Locale("es", "ES")))
        assertEquals("0.5", formatQuantity(0.5, Locale.ENGLISH))
        assertEquals("2", formatQuantity(2.0, Locale("es", "ES")))
        assertEquals("0,25", formatQuantity(0.25, Locale("es", "ES")))
    }

    private fun recipe(id: String, ingredients: List<RecipeIngredient>) = Recipe(
        id = id,
        title = "Receta $id",
        ingredients = ingredients,
    )
}
