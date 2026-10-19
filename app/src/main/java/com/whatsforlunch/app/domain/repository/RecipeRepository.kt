package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

// Contrato de acceso a las Recetas. Vive en domain/ y no depende de la UI ni del
// almacenamiento concreto (RF-2).
interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>

    // Lee una Receta concreta con sus Ingredientes y Pasos para abrir el formulario en
    // modo edición; null si no existe (RF-2, RF-18).
    suspend fun getRecipe(id: String): Recipe?

    // Guarda (crea o actualiza) una Receta y sus Ingredientes y Pasos.
    suspend fun saveRecipe(recipe: Recipe)
}
