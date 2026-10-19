package com.whatsforlunch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Acceso a Recetas, Ingredientes y Pasos. Las escrituras de una Receta se agrupan
// en una transacción para no dejar filas huérfanas ni duplicadas.
@Dao
abstract class RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY title COLLATE NOCASE ASC")
    abstract fun observeRecipesWithDetails(): Flow<List<RecipeWithDetails>>

    // Lectura puntual por id para el modo edición (RF-2, RF-18).
    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getRecipeWithDetails(id: String): RecipeWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert
    abstract suspend fun insertSteps(steps: List<StepEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    abstract suspend fun deleteSteps(recipeId: String)

    // Reemplaza por completo la Receta y sus filas (idempotente para crear y editar).
    @Transaction
    open suspend fun upsertRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>,
    ) {
        deleteIngredients(recipe.id)
        deleteSteps(recipe.id)
        insertRecipe(recipe)
        if (ingredients.isNotEmpty()) insertIngredients(ingredients)
        if (steps.isNotEmpty()) insertSteps(steps)
    }
}
