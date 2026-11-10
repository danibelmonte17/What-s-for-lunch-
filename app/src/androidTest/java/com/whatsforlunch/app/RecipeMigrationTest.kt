package com.whatsforlunch.app

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.data.repository.RoomUserProfileRepository
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private class MigrationPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "/tmp/$recipeId.jpg"

    override suspend fun delete(path: String) = Unit
}

// Migración v3→v4 (RF-VPI4, verify 3 = Scenario 4): una base v3 con Recetas migra a v4
// sin perder datos, cada Ingrediente recibe su `domainId` backfilled con la convención
// histórica `{recipeId}-ingredient-{position}` y los Pasos quedan sin vínculos.
// Migración v4→v5 (RF-EBF1, D5): la columna `status` desaparece de `recipes`
// recreando la tabla y conservando el resto de datos (incluido `favorite`).
@RunWith(AndroidJUnit4::class)
class RecipeMigrationTest {

    @Test
    fun migrationFromV3KeepsRecipeDataAndBackfillsStableIngredientIds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recipe-migration-3-4.db"
        context.deleteDatabase(name)
        createLegacyV3Database(context, name)

        val room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(
                RecipeDatabase.MIGRATION_1_2,
                RecipeDatabase.MIGRATION_2_3,
                RecipeDatabase.MIGRATION_3_4,
                RecipeDatabase.MIGRATION_4_5,
                RecipeDatabase.MIGRATION_5_6,
                RecipeDatabase.MIGRATION_6_7,
            )
            .allowMainThreadQueries()
            .build()
        try {
            val recipeCursor = room.query("SELECT title FROM recipes", null)
            assertTrue(recipeCursor.moveToFirst())
            assertEquals("Paella", recipeCursor.getString(0))
            recipeCursor.close()

            val ingredientsCursor = room.query(
                "SELECT name, domainId FROM ingredients ORDER BY position",
                null,
            )
            val ingredients = buildList {
                while (ingredientsCursor.moveToNext()) {
                    add(ingredientsCursor.getString(0) to ingredientsCursor.getString(1))
                }
            }
            ingredientsCursor.close()
            assertEquals(
                listOf(
                    "Arroz" to "mig-1-ingredient-0",
                    "Azafrán" to "mig-1-ingredient-1",
                ),
                ingredients,
            )

            val stepsCursor = room.query("SELECT text, ingredientIds FROM steps ORDER BY position", null)
            val steps = buildList {
                while (stepsCursor.moveToNext()) {
                    add(stepsCursor.getString(0) to stepsCursor.getString(1))
                }
            }
            stepsCursor.close()
            assertEquals(listOf("Sofreír el arroz" to "", "Añadir el caldo" to ""), steps)

            // Escritura y lectura tras la migración: el dominio usa los ids backfilled y
            // los vínculos nuevos se persisten sobre la base migrada.
            val repository = RoomRecipeRepository(room.recipeDao(), MigrationPhotoStore())
            runBlocking {
                val migrated = repository.getRecipe("mig-1")!!
                assertEquals(listOf("mig-1-ingredient-0", "mig-1-ingredient-1"), migrated.ingredients.map { it.id })
                assertTrue(migrated.steps.all { it.ingredientIds.isEmpty() })

                repository.saveRecipe(
                    migrated.copy(
                        steps = migrated.steps.mapIndexed { index, step ->
                            if (index == 0) step.copy(ingredientIds = listOf("mig-1-ingredient-0")) else step
                        },
                    ),
                )
                val reloaded = repository.getRecipe("mig-1")!!
                assertEquals(listOf("mig-1-ingredient-0"), reloaded.steps[0].ingredientIds)
                assertTrue(reloaded.steps[1].ingredientIds.isEmpty())
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // RF-EBF1/D5: una base v4 con valores legados de `status` migra a v5 sin la
    // columna: no queda columna residual ni fallback y los datos (incluido `favorite`)
    // se conservan.
    @Test
    fun migrationFromV4RemovesStatusColumnAndKeepsRecipeData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recipe-migration-4-5.db"
        context.deleteDatabase(name)
        createLegacyV4Database(context, name)

        val room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(
                RecipeDatabase.MIGRATION_1_2,
                RecipeDatabase.MIGRATION_2_3,
                RecipeDatabase.MIGRATION_3_4,
                RecipeDatabase.MIGRATION_4_5,
                RecipeDatabase.MIGRATION_5_6,
                RecipeDatabase.MIGRATION_6_7,
            )
            .allowMainThreadQueries()
            .build()
        try {
            // Sin columna `status` residual y con `favorite` conservado.
            val columns = buildList {
                val cursor = room.query("PRAGMA table_info(`recipes`)", null)
                while (cursor.moveToNext()) add(cursor.getString(1))
                cursor.close()
            }
            assertTrue("status" !in columns)
            assertTrue("favorite" in columns)

            val repository = RoomRecipeRepository(room.recipeDao(), MigrationPhotoStore())
            runBlocking {
                val migrated = repository.getRecipe("mig4-1")!!
                assertEquals("Tortilla", migrated.title)
                assertTrue(migrated.favorite)
                assertEquals("mig4-1-ingredient-0", migrated.ingredients.single().id)
                assertEquals(listOf("mig4-1-ingredient-0"), migrated.steps.single().ingredientIds)

                // Lectura y escritura tras la migración sobre la tabla recreada.
                repository.saveRecipe(migrated.copy(title = "Tortilla mejorada"))
                assertEquals("Tortilla mejorada", repository.getRecipe("mig4-1")!!.title)
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // `crear-mi-perfil` (D1/RD25): una base v5 gana las tablas del perfil
    // (`profile` y `profile_restrictions`) sin tocar los datos existentes, y el
    // perfil se escribe y se lee con su orden de alta tras la migración.
    @Test
    fun migrationFromV5CreatesProfileTablesAndKeepsRecipeData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recipe-migration-5-6.db"
        context.deleteDatabase(name)
        createLegacyV5Database(context, name)

        val room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(
                RecipeDatabase.MIGRATION_1_2,
                RecipeDatabase.MIGRATION_2_3,
                RecipeDatabase.MIGRATION_3_4,
                RecipeDatabase.MIGRATION_4_5,
                RecipeDatabase.MIGRATION_5_6,
                RecipeDatabase.MIGRATION_6_7,
            )
            .allowMainThreadQueries()
            .build()
        try {
            // Esquema exacto de las tablas nuevas.
            val profileColumns = buildList {
                val cursor = room.query("PRAGMA table_info(`profile`)", null)
                while (cursor.moveToNext()) add(cursor.getString(1))
                cursor.close()
            }
            assertEquals(listOf("id", "dietType", "allergiesNone", "intolerancesNone"), profileColumns)
            val restrictionColumns = buildList {
                val cursor = room.query("PRAGMA table_info(`profile_restrictions`)", null)
                while (cursor.moveToNext()) add(cursor.getString(1))
                cursor.close()
            }
            assertEquals(listOf("id", "section", "value"), restrictionColumns)

            // Los datos de la v5 siguen intactos.
            val recipeCursor = room.query("SELECT title FROM recipes", null)
            assertTrue(recipeCursor.moveToFirst())
            assertEquals("Tortilla", recipeCursor.getString(0))
            recipeCursor.close()

            // Escritura y lectura del perfil sobre la base migrada (RD25/D1).
            val repository = RoomUserProfileRepository(room.profileDao())
            runBlocking {
                assertEquals(UserProfile.empty, repository.observeProfile().first())

                repository.save(
                    UserProfile(
                        allergies = listOf("Frutos secos", "Marisco"),
                        intolerances = listOf("Diabetes"),
                        dietType = DietType.VEGAN,
                    ),
                )
                val loaded = repository.observeProfile().first()
                assertEquals(listOf("Frutos secos", "Marisco"), loaded.allergies)
                assertEquals(listOf("Diabetes"), loaded.intolerances)
                assertEquals(DietType.VEGAN, loaded.dietType)
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // `redisenar-formulario-de-receta` (D3/D5/D6/D9/D10): una base v6 migra a v7
    // sin `recipes.cookMinutes` (sumado a `prepMinutes`, con tope 23:59 h) ni
    // `ingredients.optional` (todos los Ingredientes cuentan) y con los
    // temporizadores de Paso 0 normalizados a NULL («Sin temporizador»).
    @Test
    fun migrationFromV6FoldsCookMinutesAndDropsOptionalColumn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recipe-migration-6-7.db"
        context.deleteDatabase(name)
        createLegacyV6Database(context, name)

        val room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(
                RecipeDatabase.MIGRATION_1_2,
                RecipeDatabase.MIGRATION_2_3,
                RecipeDatabase.MIGRATION_3_4,
                RecipeDatabase.MIGRATION_4_5,
                RecipeDatabase.MIGRATION_5_6,
                RecipeDatabase.MIGRATION_6_7,
            )
            .allowMainThreadQueries()
            .build()
        try {
            val recipeColumns = buildList {
                val cursor = room.query("PRAGMA table_info(`recipes`)", null)
                while (cursor.moveToNext()) add(cursor.getString(1))
                cursor.close()
            }
            assertTrue("cookMinutes" !in recipeColumns)
            assertTrue("prepMinutes" in recipeColumns)
            val ingredientColumns = buildList {
                val cursor = room.query("PRAGMA table_info(`ingredients`)", null)
                while (cursor.moveToNext()) add(cursor.getString(1))
                cursor.close()
            }
            assertTrue("optional" !in ingredientColumns)

            // D6: la cocción se suma a la preparación; D9: el tope es 23:59 h (1439).
            val prepCursor = room.query(
                "SELECT id, prepMinutes FROM recipes ORDER BY id",
                null,
            )
            val prep = buildList {
                while (prepCursor.moveToNext()) add(prepCursor.getString(0) to prepCursor.getInt(1))
            }
            prepCursor.close()
            assertEquals(listOf("mig6-1" to 75, "mig6-2" to 1439), prep)

            // D10: los temporizadores 0 pasan a NULL; los positivos se conservan.
            val timerCursor = room.query(
                "SELECT timerMinutes FROM steps ORDER BY id",
                null,
            )
            val timers = buildList {
                while (timerCursor.moveToNext()) {
                    add(if (timerCursor.isNull(0)) null else timerCursor.getInt(0))
                }
            }
            timerCursor.close()
            assertEquals(listOf<Int?>(null, 5), timers)

            // Lectura y escritura tras la migración sobre las tablas recreadas.
            val repository = RoomRecipeRepository(room.recipeDao(), MigrationPhotoStore())
            runBlocking {
                val migrated = repository.getRecipe("mig6-1")!!
                assertEquals(75, migrated.prepMinutes)
                assertEquals(listOf("Patata"), migrated.ingredients.map { it.name })
                assertEquals(listOf<Int?>(null), migrated.steps.map { it.timerMinutes })

                repository.saveRecipe(migrated.copy(prepMinutes = 80))
                assertEquals(80, repository.getRecipe("mig6-1")!!.prepMinutes)
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }
    // Esquema exacto de Room v6: como v5 más `profile` y `profile_restrictions`
    // (MIGRATION_5_6), con `recipes.cookMinutes` e `ingredients.optional` aún.
    private fun createLegacyV6Database(context: android.content.Context, name: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV6Schema(db)
                        db.execSQL("PRAGMA user_version = 6")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        // prep 30 + cocción 45 = 75 (D6); un Ingrediente «opcional» que pasa a
        // obligatorio (D5) y un Paso con temporizador 0 que queda «Sin temporizador».
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('mig6-1', 'Tortilla', '', 'LUNCH', 30, 45, 'EASY', 4, NULL, NULL, 1, 'USER')",
        )
        db.execSQL("INSERT INTO ingredients VALUES (1, 'mig6-1', 'Patata', 700.0, 'g', 1, 0, 'mig6-1-ingredient-0')")
        db.execSQL("INSERT INTO steps VALUES (1, 'mig6-1', 'Pochar', 0, 0, 'mig6-1-ingredient-0')")
        // Suma por encima de 23:59 h: se recorta a 1439 min (D9).
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('mig6-2', 'Estofado', '', 'DINNER', 1000, 900, 'HARD', 6, NULL, NULL, 0, 'USER')",
        )
        db.execSQL("INSERT INTO ingredients VALUES (2, 'mig6-2', 'Carne', 500.0, 'g', 0, 0, 'mig6-2-ingredient-0')")
        db.execSQL("INSERT INTO steps VALUES (2, 'mig6-2', 'Estofar', 5, 0, '')")
        db.close()
        helper.close()
    }

    private fun createV6Schema(db: SupportSQLiteDatabase) {
        createV5Schema(db)
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

    // Esquema exacto de Room v5: como v4 pero `recipes` sin `status` (MIGRATION_4_5).
    private fun createLegacyV5Database(context: android.content.Context, name: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV5Schema(db)
                        db.execSQL("PRAGMA user_version = 5")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('mig5-1', 'Tortilla', '', 'LUNCH', 10, 15, 'EASY', 4, NULL, NULL, 1, 'USER')",
        )
        db.execSQL("INSERT INTO ingredients VALUES (1, 'mig5-1', 'Patata', 700.0, 'g', 0, 0, 'mig5-1-ingredient-0')")
        db.execSQL("INSERT INTO steps VALUES (1, 'mig5-1', 'Pochar', 5, 0, 'mig5-1-ingredient-0')")
        db.close()
        helper.close()
    }

    private fun createV5Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`story` TEXT NOT NULL, `mealMoment` TEXT NOT NULL, `prepMinutes` INTEGER NOT NULL, " +
                "`cookMinutes` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, " +
                "`baseServings` INTEGER NOT NULL, `caloriesPerServing` INTEGER, `photoPath` TEXT, " +
                "`favorite` INTEGER NOT NULL, `origin` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, `optional` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "`domainId` TEXT NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId` ON `ingredients` (`recipeId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `text` TEXT NOT NULL, `timerMinutes` INTEGER, " +
                "`position` INTEGER NOT NULL, `ingredientIds` TEXT NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pantry_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`normalizedName` TEXT NOT NULL, `quantity` REAL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_items_normalizedName` " +
                "ON `pantry_items` (`normalizedName`)",
        )
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
                "(`itemId` TEXT NOT NULL, `sourceKey` TEXT NOT NULL, `sourceTitle` TEXT NOT NULL, " +
                "PRIMARY KEY(`itemId`, `sourceKey`), " +
                "FOREIGN KEY(`itemId`) REFERENCES `shopping_items`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_shopping_item_sources_itemId` " +
                "ON `shopping_item_sources` (`itemId`)",
        )
    }

    // Esquema exacto de Room v4: como v3 pero con `domainId` en Ingredientes y
    // `ingredientIds` en Pasos (MIGRATION_3_4); `recipes` aún tiene `status`.
    private fun createLegacyV4Database(context: android.content.Context, name: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV4Schema(db)
                        db.execSQL("PRAGMA user_version = 4")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('mig4-1', 'Tortilla', '', 'LUNCH', 10, 15, 'EASY', 4, NULL, NULL, 1, 'DRAFT', 'USER')",
        )
        db.execSQL("INSERT INTO ingredients VALUES (1, 'mig4-1', 'Patata', 700.0, 'g', 0, 0, 'mig4-1-ingredient-0')")
        db.execSQL("INSERT INTO steps VALUES (1, 'mig4-1', 'Pochar', 5, 0, 'mig4-1-ingredient-0')")
        db.close()
        helper.close()
    }

    private fun createV4Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`story` TEXT NOT NULL, `mealMoment` TEXT NOT NULL, `prepMinutes` INTEGER NOT NULL, " +
                "`cookMinutes` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, " +
                "`baseServings` INTEGER NOT NULL, `caloriesPerServing` INTEGER, `photoPath` TEXT, " +
                "`favorite` INTEGER NOT NULL, `status` TEXT NOT NULL, `origin` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, `optional` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "`domainId` TEXT NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId` ON `ingredients` (`recipeId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `text` TEXT NOT NULL, `timerMinutes` INTEGER, " +
                "`position` INTEGER NOT NULL, `ingredientIds` TEXT NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pantry_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`normalizedName` TEXT NOT NULL, `quantity` REAL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_items_normalizedName` " +
                "ON `pantry_items` (`normalizedName`)",
        )
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

    private fun createLegacyV3Database(context: android.content.Context, name: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV3Schema(db)
                        db.execSQL("PRAGMA user_version = 3")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('mig-1', 'Paella', '', 'LUNCH', 10, 20, 'EASY', 4, NULL, NULL, 0, 'DRAFT', 'MANUAL')",
        )
        db.execSQL("INSERT INTO ingredients VALUES (1, 'mig-1', 'Arroz', 300.0, 'g', 0, 0)")
        db.execSQL("INSERT INTO ingredients VALUES (2, 'mig-1', 'Azafrán', 1.0, 'g', 1, 1)")
        db.execSQL("INSERT INTO steps VALUES (1, 'mig-1', 'Sofreír el arroz', 5, 0)")
        db.execSQL("INSERT INTO steps VALUES (2, 'mig-1', 'Añadir el caldo', NULL, 1)")
        db.close()
        helper.close()
    }

    // Esquema exacto de Room v3: Recetas, Ingredientes (sin `domainId`), Pasos (sin
    // `ingredientIds`), Despensa y Lista de la Compra (RD18).
    private fun createV3Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`story` TEXT NOT NULL, `mealMoment` TEXT NOT NULL, `prepMinutes` INTEGER NOT NULL, " +
                "`cookMinutes` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, " +
                "`baseServings` INTEGER NOT NULL, `caloriesPerServing` INTEGER, `photoPath` TEXT, " +
                "`favorite` INTEGER NOT NULL, `status` TEXT NOT NULL, `origin` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, `optional` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId` ON `ingredients` (`recipeId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` TEXT NOT NULL, `text` TEXT NOT NULL, `timerMinutes` INTEGER, " +
                "`position` INTEGER NOT NULL, FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pantry_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`normalizedName` TEXT NOT NULL, `quantity` REAL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_items_normalizedName` " +
                "ON `pantry_items` (`normalizedName`)",
        )
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
