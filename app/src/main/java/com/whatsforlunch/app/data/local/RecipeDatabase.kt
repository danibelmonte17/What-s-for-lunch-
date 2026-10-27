package com.whatsforlunch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Base de datos local de las Recetas (Parte B, T2). Room persiste en el dispositivo,
// de modo que las Recetas siguen ahí tras reiniciar la app (RF-18). La versión 3
// añade la Lista de la Compra (RD18) sin tocar las tablas anteriores y la versión 4
// añade los ids estables de Ingrediente (`domainId`) y los vínculos
// Paso→Ingrediente (`ingredientIds`, RD21) sin tocar los datos existentes.
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        PantryItemEntity::class,
        ShoppingListItemEntity::class,
        ShoppingItemSourceEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun pantryItemDao(): PantryItemDao

    abstract fun shoppingListDao(): ShoppingListDao

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

        /** Adds the shopping list tables (items and their sources) on top of v2. */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` " +
                        "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, " +
                        "`quantity` REAL, `unit` TEXT NOT NULL, `aisle` TEXT, " +
                        "`completed` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_shopping_items_normalizedName` " +
                        "ON `shopping_items` (`normalizedName`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_item_sources` " +
                        "(`itemId` TEXT NOT NULL, `sourceKey` TEXT NOT NULL, " +
                        "`sourceTitle` TEXT NOT NULL, PRIMARY KEY(`itemId`, `sourceKey`), " +
                        "FOREIGN KEY(`itemId`) REFERENCES `shopping_items`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shopping_item_sources_itemId` " +
                        "ON `shopping_item_sources` (`itemId`)",
                )
            }
        }

        /**
         * Adds the stable ingredient ids (`domainId`, backfilled with the historical
         * `{recipeId}-ingredient-{position}` convention) and the step→ingredient links
         * (`ingredientIds`, CSV) on top of v3 (RD21).
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ingredients` ADD COLUMN `domainId` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE `ingredients` SET `domainId` = `recipeId` || '-ingredient-' || `position`",
                )
                db.execSQL("ALTER TABLE `steps` ADD COLUMN `ingredientIds` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
