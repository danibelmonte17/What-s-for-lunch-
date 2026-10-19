package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Implementación en memoria, útil para pruebas rápidas sin Room. La app usa
// RoomRecipeRepository; esta se conserva como alternativa sin persistencia (RF-18).
class InMemoryRecipeRepository : RecipeRepository {
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())

    override fun observeRecipes(): Flow<List<Recipe>> = recipes.asStateFlow()

    override suspend fun getRecipe(id: String): Recipe? = recipes.value.firstOrNull { it.id == id }

    override suspend fun saveRecipe(recipe: Recipe) {
        recipes.value = recipes.value.filterNot { it.id == recipe.id } + recipe
    }
}
