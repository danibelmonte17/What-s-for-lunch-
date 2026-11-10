package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidad Room de una Receta. Los enums se guardan como texto para no necesitar
// convertidores. Los Ingredientes y Pasos viven en sus propias tablas con clave foránea.
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val story: String,
    val mealMoment: String,
    val prepMinutes: Int,
    val difficulty: String,
    val baseServings: Int,
    val caloriesPerServing: Int?,
    val photoPath: String?,
    val favorite: Boolean,
    val origin: String,
)
