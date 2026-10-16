package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

// Contrato de acceso a las Recetas. Vive en domain/ y no depende de la UI ni del
// almacenamiento concreto (RF-2).
interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
}
