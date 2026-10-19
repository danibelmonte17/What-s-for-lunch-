package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryRecipeRepositoryDeleteTest {

    @Test
    fun deletingRecipePermanentlyRemovesItFromObservedRecipes() = runBlocking {
        val repository = InMemoryRecipeRepository()
        val recipe = Recipe(id = "recipe-1", title = "Tortilla")
        repository.saveRecipe(recipe)

        repository.deleteRecipe(recipe.id)

        assertEquals(emptyList<Recipe>(), repository.observeRecipes().first())
        assertEquals(null, repository.getRecipe(recipe.id))
    }

    @Test
    fun deletingMissingRecipeIsANoOp() = runBlocking {
        val repository = InMemoryRecipeRepository()
        val recipe = Recipe(id = "recipe-1", title = "Tortilla")
        repository.saveRecipe(recipe)

        repository.deleteRecipe("missing")

        assertEquals(listOf(recipe), repository.observeRecipes().first())
    }
}
