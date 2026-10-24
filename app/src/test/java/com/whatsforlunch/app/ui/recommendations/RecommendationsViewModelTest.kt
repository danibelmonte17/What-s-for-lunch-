package com.whatsforlunch.app.ui.recommendations

import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// T1: RecommendationsViewModel reutiliza compatibleRecipes() (RF-SDR2), se recalcula
// con ambos repositorios (RF-SDR3) y expone hasFoods sobre la Despensa persistida
// (RF-SDR4, D2).
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun listsOnlyCompatibleRecipesInDomainOrder() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val full = recipe(id = "full", title = "Tortilla de patatas", required = 4)
        val eightyFive = recipe(id = "eighty-five", title = "Pechuga al horno", required = 20)
        val draft = recipe(id = "draft", title = "Borrador compatible", required = 2)
        val eightyFour = recipe(id = "eighty-four", title = "Pasta", required = 19)
        recipes.saveRecipe(full)
        recipes.saveRecipe(eightyFive)
        recipes.saveRecipe(draft)
        recipes.saveRecipe(eightyFour)
        addPantry(pantry, full, 4)
        addPantry(pantry, eightyFive, 17)
        addPantry(pantry, draft, 2)
        addPantry(pantry, eightyFour, 16)

        val viewModel = RecommendationsViewModel(pantry, recipes)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("draft", "full", "eighty-five"), state.compatibleRecipes.map { it.recipe.id })
        assertEquals(listOf(100, 100, 85), state.compatibleRecipes.map { it.match.percentage })
        assertTrue(state.hasFoods)
    }

    @Test
    fun emptyOrIncompletePantryYieldsNoRecommendations() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        recipes.saveRecipe(recipe(id = "pasta", title = "Pasta", required = 19))

        val viewModel = RecommendationsViewModel(pantry, recipes)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.compatibleRecipes.isEmpty())
        assertFalse(viewModel.uiState.value.hasFoods)
    }

    @Test
    fun stateRecalculatesWhenPantryChangesWithoutRecreatingTheViewModel() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val target = recipe(id = "target", title = "Ensalada", required = 19)
        recipes.saveRecipe(target)
        addPantry(pantry, target, 16)

        val viewModel = RecommendationsViewModel(pantry, recipes)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.compatibleRecipes.isEmpty())
        assertTrue(viewModel.uiState.value.hasFoods)

        addPantry(pantry, target, 17)
        advanceUntilIdle()
        assertEquals(listOf("target"), viewModel.uiState.value.compatibleRecipes.map { it.recipe.id })

        pantry.deletePantryItem("pantry-target-1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.compatibleRecipes.isEmpty())
    }

    @Test
    fun stateRecalculatesWhenRecipesChangeAndTracksEmptyPantry() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val target = recipe(id = "target", title = "Ensalada", required = 2)
        addPantry(pantry, target, 2)

        val viewModel = RecommendationsViewModel(pantry, recipes)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.compatibleRecipes.isEmpty())

        recipes.saveRecipe(target)
        advanceUntilIdle()
        assertEquals(listOf("target"), viewModel.uiState.value.compatibleRecipes.map { it.recipe.id })

        recipes.deleteRecipe("target")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.compatibleRecipes.isEmpty())
    }

    @Test
    fun hasFoodsFollowsThePersistedPantryOnly() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val viewModel = RecommendationsViewModel(pantry, recipes)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasFoods)

        pantry.savePantryItem(PantryItem(id = "tomate", name = "Tomate"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasFoods)

        pantry.deletePantryItem("tomate")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasFoods)
    }

    private fun recipe(
        id: String,
        title: String,
        required: Int,
    ): Recipe = Recipe(
        id = id,
        title = title,
        ingredients = (1..required).map { index ->
            RecipeIngredient(id = "$id-$index", name = "$id ingredient $index")
        },
    )

    private suspend fun addPantry(pantry: InMemoryPantryRepository, recipe: Recipe, available: Int) {
        recipe.ingredients.take(available).forEach { ingredient ->
            pantry.savePantryItem(PantryItem(id = "pantry-${ingredient.id}", name = ingredient.name))
        }
    }
}
