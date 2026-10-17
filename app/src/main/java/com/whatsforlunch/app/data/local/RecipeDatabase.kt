package com.whatsforlunch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Base de datos local de las Recetas (Parte B, T2). Room persiste en el dispositivo,
// de modo que las Recetas siguen ahí tras reiniciar la app (RF-18).
@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
