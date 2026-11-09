package com.whatsforlunch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Base de datos local de las Recetas (Parte B, T2). Room persiste en el dispositivo,
// de modo que las Recetas siguen ahí tras reiniciar la app (RF-18). La versión 3
// añade la Lista de la Compra (RD18) sin tocar las tablas anteriores y la versión 4
// añade los ids estables de Ingrediente (`domainId`) y los vínculos
// Paso→Ingrediente (`ingredientIds`, RD21) sin tocar los datos existentes. La
// versión 5 elimina la columna `status` de `recipes` (las Recetas ya no tienen
// estado Borrador/Publicada) recreando la tabla y conservando los datos. La
// versión 6 añade el perfil del usuario (`profile` y `profile_restrictions`,
// `crear-mi-perfil`, D1) sin tocar las tablas existentes.
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        PantryItemEntity::class,
        ShoppingListItemEntity::class,
        ShoppingItemSourceEntity::class,
        ProfileEntity::class,
        ProfileRestrictionEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun pantryItemDao(): PantryItemDao

    abstract fun shoppingListDao(): ShoppingListDao

    abstract fun profileDao(): ProfileDao

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

        /**
         * Removes the recipe `status` column (the draft/published state is gone)
         * on top of v4. `ALTER TABLE ... DROP COLUMN` is not available in every
         * Android SQLite version, so the table is recreated and the data copied.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipes_new` " +
                        "(`id` TEXT NOT NULL, `title` TEXT NOT NULL, `story` TEXT NOT NULL, " +
                        "`mealMoment` TEXT NOT NULL, `prepMinutes` INTEGER NOT NULL, " +
                        "`cookMinutes` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, " +
                        "`baseServings` INTEGER NOT NULL, `caloriesPerServing` INTEGER, " +
                        "`photoPath` TEXT, `favorite` INTEGER NOT NULL, `origin` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `recipes_new` (`id`, `title`, `story`, `mealMoment`, `prepMinutes`, " +
                        "`cookMinutes`, `difficulty`, `baseServings`, `caloriesPerServing`, " +
                        "`photoPath`, `favorite`, `origin`) " +
                        "SELECT `id`, `title`, `story`, `mealMoment`, `prepMinutes`, `cookMinutes`, " +
                        "`difficulty`, `baseServings`, `caloriesPerServing`, `photoPath`, `favorite`, " +
                        "`origin` FROM `recipes`",
                )
                db.execSQL("DROP TABLE `recipes`")
                db.execSQL("ALTER TABLE `recipes_new` RENAME TO `recipes`")
            }
        }

        /**
         * Adds the user profile tables (`profile` single row + `profile_restrictions`
         * chips in insertion order) on top of v5 (`crear-mi-perfil`, D1/RD25),
         * without touching existing recipe, pantry or shopping tables.
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` " +
                        "(`id` TEXT NOT NULL, `dietType` TEXT, `allergiesNone` INTEGER NOT NULL, " +
                        "`intolerancesNone` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile_restrictions` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `section` TEXT NOT NULL, " +
                        "`value` TEXT NOT NULL)",
                )
            }
        }
    }
}
