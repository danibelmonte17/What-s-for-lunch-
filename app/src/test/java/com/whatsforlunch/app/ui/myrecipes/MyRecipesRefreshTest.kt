package com.whatsforlunch.app.ui.myrecipes

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// ON_RESUME del route → `refresh()`: cada vuelta a Mi recetario reinicia la
// colección de los repositorios (consulta nueva) y la vista se reconstruye con
// los datos vigentes, incluido `hasFoods` de la Despensa.
@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipesRefreshTest {

    @Test
    fun refreshStartsANewCollectionOfTheRepositories() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = CountingRecipeRepository(InMemoryRecipeRepository())
            val pantry = InMemoryPantryRepository()
            val viewModel = MyRecipesViewModel(recipes, pantry)
            viewModelStore.put("refresh", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            val collectionsAfterStart = recipes.collections
            assertTrue("La vista debe suscribirse a los repositorios al arrancar", collectionsAfterStart >= 1)

            viewModel.refresh()
            runCurrent()

            assertTrue(
                "refresh() debe volver a leer los repositorios",
                recipes.collections > collectionsAfterStart,
            )
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshRebuildsTheViewWithTheLatestData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipeRepository = InMemoryRecipeRepository()
            val pantry = InMemoryPantryRepository()
            val viewModel = MyRecipesViewModel(recipeRepository, pantry)
            viewModelStore.put("refresh", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.refresh()
            runCurrent()
            assertFalse(viewModel.uiState.value.hasFoods)
            assertEquals(0, viewModel.uiState.value.recipes.size)

            recipeRepository.saveRecipe(
                Recipe(
                    id = "tortilla",
                    title = "Tortilla de patata",
                    mealMoment = MealMoment.LUNCH,
                ),
            )
            pantry.savePantryItem(PantryItem(id = "pantry-tomate", name = "Tomate fresco"))
            viewModel.refresh()
            runCurrent()

            assertEquals(1, viewModel.uiState.value.recipes.size)
            assertTrue(viewModel.uiState.value.hasFoods)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    // Cuenta las colecciones de `observeRecipes` (onStart en cada suscripción).
    private class CountingRecipeRepository(
        private val delegate: RecipeRepository,
    ) : RecipeRepository {
        var collections = 0
            private set

        override fun observeRecipes(): Flow<List<Recipe>> =
            delegate.observeRecipes().onStart { collections++ }

        override suspend fun getRecipe(id: String): Recipe? = delegate.getRecipe(id)

        override suspend fun saveRecipe(recipe: Recipe) = delegate.saveRecipe(recipe)

        override suspend fun deleteRecipe(id: String) = delegate.deleteRecipe(id)
    }
}
