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

    // RF-ER1/RF-ER2 y RD03: el Detalle arranca en las Raciones base y las filas
    // del checklist se recalculan al cambiar de raciones conservando su identidad.
    @Test
    fun detailStartsAtBaseServingsAndScalesIngredientRows() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(
                Recipe(
                    id = "scaled",
                    title = "Bizcocho",
                    baseServings = 4,
                    ingredients = listOf(
                        RecipeIngredient(id = "flour", name = "Harina", quantity = 200.0, unit = "g"),
                        RecipeIngredient(id = "egg", name = "Huevos", quantity = 2.0, unit = "uds"),
                    ),
                ),
            )

            val viewModel = RecipeDetailViewModel(recipes, InMemoryPantryRepository(), "scaled")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val initial = viewModel.uiState.value as RecipeDetailUiState.Detail
            assertEquals(4, initial.servings)
            assertEquals(200.0, initial.ingredientRows.first { it.id == "flour" }.quantity, 0.0001)

            viewModel.setServings(2)
            runCurrent()

            val half = viewModel.uiState.value as RecipeDetailUiState.Detail
            assertEquals(2, half.servings)
            assertEquals(100.0, half.ingredientRows.first { it.id == "flour" }.quantity, 0.0001)
            assertEquals(1.0, half.ingredientRows.first { it.id == "egg" }.quantity, 0.0001)
            // D5: nombre, unidad y opcionalidad intactos y misma identidad de fila.
            assertEquals(listOf("flour", "egg"), half.ingredientRows.map { it.id })
            assertEquals("Harina", half.ingredientRows.first().name)
            assertEquals("g", half.ingredientRows.first().unit)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    // RF-ER3 (verify 2): la selección nunca sale de 1-12.
    @Test
    fun servingsSelectionNeverLeavesAllowedRange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(Recipe(id = "range", title = "Sopa", baseServings = 4))

            val viewModel = RecipeDetailViewModel(recipes, InMemoryPantryRepository(), "range")
            viewModelStore.put("detail", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.setServings(0)
            runCurrent()
            assertEquals(1, (viewModel.uiState.value as RecipeDetailUiState.Detail).servings)

            viewModel.setServings(13)
            runCurrent()
            assertEquals(12, (viewModel.uiState.value as RecipeDetailUiState.Detail).servings)

            viewModel.setServings(5)
            runCurrent()
            assertEquals(5, (viewModel.uiState.value as RecipeDetailUiState.Detail).servings)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun recipe(id: String, ingredients: List<RecipeIngredient>) = Recipe(
        id = id,
        title = "Receta $id",
        ingredients = ingredients,
    )
}
