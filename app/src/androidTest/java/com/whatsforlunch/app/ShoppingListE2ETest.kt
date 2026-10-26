package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.app.ActivityOptionsCompat
import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.local.ShoppingItemSourceEntity
import com.whatsforlunch.app.data.local.ShoppingListItemEntity
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.repository.RoomPantryRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.data.repository.RoomShoppingListRepository
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.pinche.PincheScreen
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import com.whatsforlunch.app.ui.pinche.PincheViewModel
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.recommendations.RecommendationsTestTags
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.shoppinglist.ShoppingListScreen
import com.whatsforlunch.app.ui.shoppinglist.ShoppingListTestTags
import com.whatsforlunch.app.ui.shoppinglist.ShoppingListViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

private object ShoppingNoOpPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) = Unit
}

// S1–S8 de anadir-faltantes-a-la-lista: las tres superficies de alta, la Lista con
// sus badges y «Deshacer», el estado vacío, la idempotencia por fuente y la
// migración 2→3 de Room.
@RunWith(AndroidJUnit4::class)
class ShoppingListE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1 (verify 1): Detalle con Faltantes → aviso, Lista con ítems y «Para: Paella».
    @Test
    fun detailAddsMissingItemsToTheShoppingListWithOrigin() {
        seedRecipe(
            Recipe(
                id = "paella",
                title = "Paella",
                ingredients = listOf(
                    RecipeIngredient(id = "rice", name = "Arroz", quantity = 400.0, unit = "g"),
                    RecipeIngredient(id = "saffron", name = "Azafrán", quantity = 1.0, unit = "g"),
                    RecipeIngredient(id = "seafood", name = "Mariscos", quantity = 300.0, unit = "g"),
                ),
            ),
        )
        seedPantry("Arroz")
        seedPantry("Nata")
        launchFullApp()
        openRecipeByTitle("Paella")

        composeRule.onNodeWithTag(RecipeDetailTestTags.MATCH_BANNER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("¡Tienes el 33% de ingredientes en casa!")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.ADD_TO_LIST)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.ADDED_SNACKBAR)
                .fetchSemanticsNodes().isNotEmpty()
        }

        backToMyRecipes()
        openShoppingList()

        assertEquals(2, composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Mariscos (300 g)").assertIsDisplayed()
        assertEquals(
            2,
            composeRule.onAllNodesWithText("Para: Paella").fetchSemanticsNodes().size,
        )
    }

    // S2 (verify 2): repetir la acción no crea filas ni dobla las cantidades.
    @Test
    fun repeatingTheActionDoesNotDuplicateOrDoubleTheItems() {
        seedRecipe(
            Recipe(
                id = "paella",
                title = "Paella",
                ingredients = listOf(
                    RecipeIngredient(id = "rice", name = "Arroz", quantity = 400.0, unit = "g"),
                    RecipeIngredient(id = "saffron", name = "Azafrán", quantity = 1.0, unit = "g"),
                    RecipeIngredient(id = "seafood", name = "Mariscos", quantity = 300.0, unit = "g"),
                ),
            ),
        )
        seedPantry("Arroz")
        seedPantry("Nata")
        launchFullApp()
        openRecipeByTitle("Paella")
        addMissingFromDetail()
        backToMyRecipes()

        openRecipeByTitle("Paella")
        addMissingFromDetail()
        backToMyRecipes()

        openShoppingList()
        assertEquals(2, composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Mariscos (300 g)").assertIsDisplayed()
    }

    // S3: dos Recetas con «Harina» suman (200 g + 500 g) y el badge cuenta fuentes.
    @Test
    fun twoRecipesAddUpAndTheBadgeCountsSources() {
        seedRecipe(
            Recipe(
                id = "tortilla",
                title = "Tortilla",
                ingredients = listOf(
                    RecipeIngredient(id = "flour", name = "Harina", quantity = 200.0, unit = "g"),
                ),
            ),
        )
        seedRecipe(
            Recipe(
                id = "galletas",
                title = "Galletas",
                ingredients = listOf(
                    RecipeIngredient(id = "flour-2", name = "Harina", quantity = 500.0, unit = "g"),
                ),
            ),
        )
        launchFullApp()

        openRecipeByTitle("Tortilla")
        addMissingFromDetail()
        backToMyRecipes()
        openRecipeByTitle("Galletas")
        composeRule.onNodeWithText("Galletas").assertIsDisplayed()
        addMissingFromDetail()
        backToMyRecipes()
        openShoppingList()

        assertEquals(1, composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Harina (700 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Para: 2 recetas").assertIsDisplayed()
    }

    // S4: tarjeta de «Recetas con mi Despensa» (tras un escaneo) con carrito.
    @Test
    fun recommendationCardAddsItsMissingItems() {
        seedRecipe(
            Recipe(
                id = "pechuga",
                title = "Pechuga Gratinada",
                ingredients = (1..7).map { index ->
                    RecipeIngredient(id = "ingredient-$index", name = "Ingrediente $index")
                }.dropLast(1) + RecipeIngredient(
                    id = "oregano",
                    name = "Orégano seco",
                    quantity = 1.0,
                    unit = "bote",
                ),
            ),
        )
        (1..6).forEach { index -> seedPantry("Ingrediente $index") }
        launchFullApp()

        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("pechuga"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Te falta: Orégano seco").assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.addMissing("pechuga")).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.missing("pechuga"))
                .fetchSemanticsNodes().isEmpty()
        }
        assertEquals(
            0,
            composeRule.onAllNodesWithTag(RecommendationsTestTags.addMissing("pechuga"))
                .fetchSemanticsNodes().size,
        )

        composeRule.onNodeWithTag(RecommendationsTestTags.BACK).performClick()
        openShoppingList()
        composeRule.onNodeWithText("Orégano seco (1 bote)").assertIsDisplayed()
        composeRule.onNodeWithText("Para: Pechuga Gratinada").assertIsDisplayed()
    }

    // S5: Ficha del Pinche → ítems con la fuente de la Propuesta y estado «Añadido».
    @Test
    fun pincheProposalAddsMissingItemsAndBecomesDisabled() {
        val shoppingList = InMemoryShoppingListRepository()
        val pantry = InMemoryPantryRepository().apply {
            runBlocking { savePantryItem(PantryItem("tomate", "Tomate")) }
        }
        val viewModel = PincheViewModel(
            pantry,
            InMemoryRecipeRepository(),
            ShoppingFakePincheClient(listOf(PincheReplyResult.Success(PincheReply("Prueba esto.", proposal())))),
            shoppingList,
        )
        launchPinche(viewModel)

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performClick()
        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextReplacement("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.proposalCard(1))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.ADD_MISSING).performScrollTo().performClick()
        composeRule.onNodeWithTag(PincheTestTags.ADDED_MISSING_LABEL)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()

        val items = runBlocking { shoppingList.observeItems().first() }
        assertEquals(listOf("Harina"), items.map { it.name })
        assertEquals(
            listOf(PincheProposalRules.proposalRecipeId("Arroz con tomate")),
            items.single().sources.map { it.sourceKey },
        )
    }

    // S6: borrado con aviso «Ítem eliminado» y «Deshacer» que restaura el ítem.
    @Test
    fun deletingAnItemShowsUndoAndRestoresIt() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
            )
        }
        val viewModel = ShoppingListViewModel(repository)
        launchShoppingList(viewModel)

        val itemId = ShoppingListRules.itemIdFor("Azafrán")
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.delete(itemId)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.UNDO)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ShoppingListTestTags.UNDO).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Azafrán (1 g)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Para: Paella").assertIsDisplayed()
    }

    // S7: Lista vacía con CTA que abre el Escáner.
    @Test
    fun emptyListShowsTheScanPantryCta() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Tu Lista de la Compra está vacía").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_CTA).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    // S8: sin Faltantes no hay acción ni línea «Te falta: …».
    @Test
    fun fullMatchHidesTheAddActions() {
        seedRecipe(
            Recipe(
                id = "arroz-con-leche",
                title = "Arroz con leche",
                ingredients = listOf(
                    RecipeIngredient(id = "rice", name = "Arroz"),
                    RecipeIngredient(id = "milk", name = "Leche"),
                ),
            ),
        )
        seedPantry("Arroz")
        seedPantry("Leche")
        launchFullApp()
        openRecipeByTitle("Arroz con leche")

        composeRule.onNodeWithText("¡Tienes el 100% de ingredientes en casa!")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.ADD_TO_LIST).assertCountEquals(0)

        backToMyRecipes()
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("arroz-con-leche"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithTag(RecommendationsTestTags.missing("arroz-con-leche"))
            .assertCountEquals(0)
        composeRule.onAllNodesWithTag(RecommendationsTestTags.addMissing("arroz-con-leche"))
            .assertCountEquals(0)
    }

    // Migración 2→3: una BD v2 con Receta y Despensa conserva sus datos y gana las
    // tablas de la Lista de la Compra.
    @Test
    fun migrationFromV2KeepsRecipesAndPantryAndCreatesShoppingTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "shopping-migration-2-3.db"
        context.deleteDatabase(name)
        createLegacyV2Database(context, name)

        val room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(RecipeDatabase.MIGRATION_1_2, RecipeDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val recipeCursor = room.query("SELECT title FROM recipes", null)
            assertTrue(recipeCursor.moveToFirst())
            assertEquals("Paella", recipeCursor.getString(0))
            recipeCursor.close()

            val pantryCursor = room.query("SELECT name FROM pantry_items", null)
            assertTrue(pantryCursor.moveToFirst())
            assertEquals("Arroz", pantryCursor.getString(0))
            pantryCursor.close()

            val tableCursor = room.query(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                    "AND name IN ('shopping_items', 'shopping_item_sources') ORDER BY name",
                null,
            )
            val tables = buildList {
                while (tableCursor.moveToNext()) add(tableCursor.getString(0))
            }
            tableCursor.close()
            assertEquals(listOf("shopping_item_sources", "shopping_items"), tables)

            runBlocking {
                room.shoppingListDao().insertItem(
                    ShoppingListItemEntity(
                        id = "migrated-item",
                        name = "Azafrán",
                        normalizedName = "azafran",
                        quantity = 1.0,
                        unit = "g",
                        aisle = null,
                        completed = false,
                    ),
                )
                room.shoppingListDao().insertSources(
                    listOf(
                        ShoppingItemSourceEntity(
                            itemId = "migrated-item",
                            sourceKey = "paella",
                            sourceTitle = "Paella",
                        ),
                    ),
                )
                val migrated = room.shoppingListDao().getAllOnce().single()
                assertEquals("Azafrán", migrated.item.name)
                assertEquals(1.0, migrated.item.quantity!!, 0.0001)
                assertEquals("Paella", migrated.sources.single().sourceTitle)
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // Persistencia Room (RF-AFL7): el ítem y sus fuentes sobreviven a cerrar y reabrir
    // la base, y la ruta real de borrado/restauración conserva cantidad, unidad y Origen.
    @Test
    fun roomRepositoryKeepsItemsAcrossReopenAndRestoresDeletedSources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "shopping-repository.db"
        context.deleteDatabase(name)
        var room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
            .addMigrations(RecipeDatabase.MIGRATION_1_2, RecipeDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RoomShoppingListRepository(room.shoppingListDao(), room)
            runBlocking {
                repository.addMissing(
                    sourceKey = "paella",
                    sourceTitle = "Paella",
                    ingredients = listOf(
                        RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                    ),
                )
            }
            room.close()

            room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
                .addMigrations(RecipeDatabase.MIGRATION_1_2, RecipeDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()
            val reopened = RoomShoppingListRepository(room.shoppingListDao(), room)
            val item = runBlocking { reopened.observeItems().first().single() }
            assertEquals("Azafrán", item.name)
            assertEquals(1.0, item.quantity!!, 0.0001)
            assertEquals("g", item.unit)
            assertEquals(listOf("paella"), item.sources.map { it.sourceKey })
            assertEquals(listOf("Paella"), item.sources.map { it.sourceTitle })

            runBlocking { reopened.deleteItem(item.id) }
            assertTrue(runBlocking { reopened.observeItems().first() }.isEmpty())

            runBlocking { reopened.restoreItem(item) }
            assertEquals(item, runBlocking { reopened.observeItems().first().single() })
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // --- Helpers -----------------------------------------------------------------

    private fun launchFullApp() {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val context = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalActivityResultRegistryOwner provides ShoppingTestActivityResultRegistryOwner,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun openRecipeByTitle(title: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).performTextReplacement(title)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD)
                .fetchSemanticsNodes().size == 1 &&
                composeRule.onAllNodes(
                    hasTestTag(MyRecipesTestTags.RECIPE_TITLE) and hasText(title),
                ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addMissingFromDetail() {
        composeRule.onNodeWithTag(RecipeDetailTestTags.ADD_TO_LIST)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.ADDED_SNACKBAR)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun backToMyRecipes() {
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openShoppingList() {
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SHOPPING_LIST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun launchShoppingList(viewModel: ShoppingListViewModel) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val context = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    ShoppingListScreen(
                        uiState = state,
                        onBack = {},
                        onScanPantry = {},
                        onDeleteItem = viewModel::deleteItem,
                        onUndoDelete = viewModel::undoDelete,
                        onUndoExpired = viewModel::dismissUndo,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun launchPinche(viewModel: PincheViewModel) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val context = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    PincheScreen(
                        uiState = state,
                        onBack = {},
                        onOpenSettings = {},
                        onInputChange = viewModel::onInputChange,
                        onSend = { viewModel.send("es") },
                        onQuickSuggestion = { text -> viewModel.sendQuickSuggestion(text, "es") },
                        onRetry = { viewModel.retry("es") },
                        onSaveProposal = viewModel::saveProposal,
                        onAddMissingToShoppingList = viewModel::addMissingToShoppingList,
                        onClearNotice = viewModel::clearNotice,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun seedRecipe(recipe: Recipe) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomRecipeRepository(database.recipeDao(), ShoppingNoOpPhotoStore)
        runBlocking { repository.saveRecipe(recipe) }
    }

    private fun seedPantry(name: String) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomPantryRepository(database.pantryItemDao())
        runBlocking {
            repository.savePantryItem(PantryItem(id = "pantry-$name", name = name))
        }
    }

    private fun createLegacyV2Database(
        context: android.content.Context,
        name: String,
    ) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV2Schema(db)
                        db.execSQL("PRAGMA user_version = 2")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO recipes VALUES " +
                "('paella', 'Paella', '', 'LUNCH', 10, 20, 'EASY', 4, NULL, NULL, 0, 'DRAFT', 'MANUAL')",
        )
        db.execSQL("INSERT INTO pantry_items VALUES ('pantry-rice', 'Arroz', 'arroz', NULL)")
        db.close()
        helper.close()
    }

    private fun createV2Schema(db: SupportSQLiteDatabase) {
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
    }

    private fun proposal(): RecipeSuggestion = RecipeSuggestion(
        title = "Arroz con tomate",
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = listOf(
            RecipeIngredient(id = "0", name = "Tomate", quantity = 1.0, unit = "ud"),
            RecipeIngredient(id = "1", name = "Harina", quantity = 1.0, unit = "ud"),
        ),
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )

    private class ShoppingFakePincheClient(
        private val results: List<PincheReplyResult>,
    ) : PincheChatClient {
        private var calls = 0
        override suspend fun reply(turns: List<PincheTurn>, language: String): PincheReplyResult {
            if (results.isEmpty()) return PincheReplyResult.Success(PincheReply(text = "ok"))
            return results[minOf(calls++, results.lastIndex)]
        }
    }
}

// El contexto localizado no es una Activity: el dueño del registro de resultados se
// aporta explícitamente para que el Escáner pueda componerse.
private object ShoppingTestActivityResultRegistryOwner : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) = Unit
    }
}
