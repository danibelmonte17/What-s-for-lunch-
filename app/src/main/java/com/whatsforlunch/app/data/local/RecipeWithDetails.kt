package com.whatsforlunch.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

// Receta con sus Ingredientes y Pasos, como los devuelve Room (@Relation).
data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val steps: List<StepEntity>,
)
