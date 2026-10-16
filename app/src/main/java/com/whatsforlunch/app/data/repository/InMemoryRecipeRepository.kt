package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Implementación sin persistencia: la app nace sin Recetas. La persistencia
// real llega con la feature crear-receta.
class InMemoryRecipeRepository : RecipeRepository {
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())

    override fun observeRecipes(): Flow<List<Recipe>> = recipes.asStateFlow()
}
