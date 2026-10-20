package com.whatsforlunch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Base de datos local de las Recetas (Parte B, T2). Room persiste en el dispositivo,
// de modo que las Recetas siguen ahí tras reiniciar la app (RF-18).
@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class, PantryItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun pantryItemDao(): PantryItemDao

    companion object {
        /** Adds pantry storage without touching existing recipe tables or data. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pantry_items` " +
                        "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, " +
                        "`quantity` REAL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_items_normalizedName` " +
                        "ON `pantry_items` (`normalizedName`)",
                )
            }
        }
    }
}
