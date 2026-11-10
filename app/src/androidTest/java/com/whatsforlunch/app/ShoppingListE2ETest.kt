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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
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
import androidx.compose.ui.unit.Dp
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
import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.data.repository.RoomPantryRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.data.repository.RoomShoppingListRepository
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.ui.components.optionTag
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.pinche.PincheScreen
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import com.whatsforlunch.app.ui.pinche.PincheViewModel
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.recipedetail.formatQuantity
import com.whatsforlunch.app.ui.recommendations.RecommendationsTestTags
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
// migración 2→3 de Room. S1–S10 de agrupar-y-completar-la-lista: agrupación por
// Pasillo/Receta, checkbox y progreso, «Limpiar comprados» con «Deshacer» y el
// contenido de fila según el modo.
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
            // Perfil completo: sin partes pendientes la IA sí responde (RD26).
            InMemoryUserProfileRepository(
                UserProfile(
                    allergies = listOf("Frutos secos"),
                    intolerancesNone = true,
                    dietType = DietType.OMNIVORE,
                ),
            ),
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

    // S7 (mover-escaneo-a-la-despensa, D2): la Lista vacía ya no ofrece escanear —
    // sin «Escanear la Despensa» ni ningún acceso al Escáner— y queda solo
    // «Añadir ítem» como CTA primario.
    @Test
    fun emptyListShowsNoScannerAccessAndOnlyTheAddItemCta() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Tu Lista de la Compra está vacía").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY_CTA).assertCountEquals(0)
        composeRule.onAllNodesWithText("Escanear la Despensa").assertCountEquals(0)
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_ADD_CTA).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).assertIsDisplayed()
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
            val reopened = RoomShoppingListRepository(room.shoppingListDao(), room)
            val item = runBlocking { reopened.observeItems().first().single() }
            assertEquals("Azafrán", item.name)
            assertEquals(1.0, item.quantity!!, 0.0001)
            assertEquals("g", item.unit)
            assertEquals(listOf("paella"), item.sources.map { it.sourceKey })
            assertEquals(listOf("Paella"), item.sources.map { it.sourceTitle })

            // anadir-item-a-la-lista (punto 6): editar a mano un ítem con fuentes sobre
            // una BD v3 previa conserva nombre, fuentes y Origen y sobrevive al reabrir.
            runBlocking {
                reopened.saveManualItem(
                    id = item.id,
                    name = "Azafrán",
                    quantity = 2.0,
                    unit = "g",
                    aisle = Aisle.DAIRY,
                )
            }
            room.close()

            room = Room.databaseBuilder(context, RecipeDatabase::class.java, name)
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
            val reopenedAfterEdit = RoomShoppingListRepository(room.shoppingListDao(), room)
            val edited = runBlocking { reopenedAfterEdit.observeItems().first().single() }
            assertEquals("Azafrán", edited.name)
            assertEquals(2.0, edited.quantity!!, 0.0001)
            assertEquals("g", edited.unit)
            assertEquals(Aisle.DAIRY, edited.aisle)
            assertEquals(listOf("paella"), edited.sources.map { it.sourceKey })
            assertEquals(listOf("Paella"), edited.sources.map { it.sourceTitle })

            runBlocking { reopenedAfterEdit.deleteItem(edited.id) }
            assertTrue(runBlocking { reopenedAfterEdit.observeItems().first() }.isEmpty())

            runBlocking { reopenedAfterEdit.restoreItem(edited) }
            assertEquals(edited, runBlocking { reopenedAfterEdit.observeItems().first().single() })
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    // --- anadir-item-a-la-lista (S1–S6) -------------------------------------

    // S1 (verify 1): el alta manual con cantidad, unidad y Pasillo deja la fila
    // con la etiqueta del Pasillo y el badge «Despensa baja».
    @Test
    fun manualAddShowsTheItemWithAisleLabelAndManualBadge() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("2")
        // D1 (unificar-unidades): el ítem nuevo nace con la unidad «ud» del enum.
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).assertTextContains("ud")
        composeRule.onNodeWithTag(ShoppingListTestTags.aisleOption("PANTRY_OILS"))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        val itemId = ShoppingListRules.itemIdFor("Pan")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Pan (2 ud)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pan (2 ud)").assertIsDisplayed()
        // D6: la etiqueta de Pasillo de la fila se muestra en «Por Receta»; allí
        // el badge «Despensa baja» (sin fuentes) se conserva.
        selectRecipeGrouping()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.aisle(itemId)) and hasText("Despensa & Aceites"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Despensa baja").assertIsDisplayed()
    }

    // S2: añadir a mano un nombre que ya existe lo edita en lugar de duplicarlo y
    // conserva su Origen (D3).
    @Test
    fun manualAddOfAnExistingNameEditsItWithoutCreatingANewRow() {
        seedRecipe(
            Recipe(
                id = "tortilla",
                title = "Tortilla",
                ingredients = listOf(
                    RecipeIngredient(id = "flour", name = "Harina", quantity = 200.0, unit = "g"),
                ),
            ),
        )
        launchFullApp()
        openRecipeByTitle("Tortilla")
        addMissingFromDetail()
        backToMyRecipes()
        openShoppingList()
        composeRule.onNodeWithText("Harina (200 g)").assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("harina")
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("500")
        selectUnit("g")
        composeRule.onNodeWithTag(ShoppingListTestTags.aisleOption("PANTRY_OILS"))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        val itemId = ShoppingListRules.itemIdFor("Harina")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Harina (500 g)").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size)
        composeRule.onNodeWithTag(ShoppingListTestTags.item(itemId)).assertIsDisplayed()
        // D6: en «Pasillo» la etiqueta de Pasillo la lleva la cabecera del grupo.
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("PANTRY_OILS")) and hasText("Despensa & Aceites"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Para: Tortilla").assertIsDisplayed()
    }

    // S3: el icono de edición por fila solo cambia cantidad, unidad y Pasillo;
    // el nombre y el badge quedan intactos (D7).
    @Test
    fun editingAnItemOnlyChangesQuantityUnitAndAisle() {
        seedRecipe(
            Recipe(
                id = "paella",
                title = "Paella",
                ingredients = listOf(
                    RecipeIngredient(id = "saffron", name = "Azafrán", quantity = 1.0, unit = "g"),
                ),
            ),
        )
        launchFullApp()
        openRecipeByTitle("Paella")
        addMissingFromDetail()
        backToMyRecipes()
        openShoppingList()

        val itemId = ShoppingListRules.itemIdFor("Azafrán")
        composeRule.onNodeWithTag(ShoppingListTestTags.edit(itemId)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.aisleOption("DAIRY"))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EDIT_DIALOG).fetchSemanticsNodes().isEmpty() &&
                // D6: en «Pasillo» el cambio de Pasillo se ve en la cabecera del grupo.
                composeRule.onAllNodes(
                    hasTestTag(ShoppingListTestTags.groupHeader("DAIRY")) and hasText("Lácteos"),
                ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Para: Paella").assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size)
    }

    // S4/RF-AIL5: nombre vacío o cantidad inválida muestran el error, mantienen
    // el diálogo abierto y no guardan nada.
    @Test
    fun invalidNameOrQuantityShowsAnErrorAndKeepsTheDialogOpen() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.onNodeWithText("Escribe un nombre").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.EDIT_DIALOG).assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Leche")
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("0")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.onNodeWithText("Escribe una cantidad válida").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.EDIT_DIALOG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).assertCountEquals(0)
    }

    // S5: cantidad y Pasillo son opcionales; el ítem queda «Sin clasificar». La
    // unidad es la «ud» por defecto de las filas nuevas (D1 de unificar-unidades),
    // por lo que la fila la muestra junto al nombre.
    @Test
    fun manualAddWithoutQuantityOrAisleUsesTheDefaults() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Leche")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        val itemId = ShoppingListRules.itemIdFor("Leche")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Leche (ud)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Leche (ud)").assertIsDisplayed()
        // D6: la etiqueta de Pasillo de la fila se muestra en «Por Receta».
        selectRecipeGrouping()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.aisle(itemId)) and hasText("Sin clasificar"),
        ).assertIsDisplayed()
    }

    // S6: el estado vacío también abre el diálogo con su «Añadir ítem».
    @Test
    fun emptyStateAddCtaOpensTheDialogAndSavesTheItem() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.EMPTY_ADD_CTA).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.EDIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        // D1 (unificar-unidades): el ítem nace con la unidad «ud», que la fila
        // muestra junto al nombre.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Pan (ud)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pan (ud)").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).assertCountEquals(1)
        composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY).assertCountEquals(0)
    }

    // Repair H1 (validación sesión 88): editar sin tocar la cantidad la conserva
    // intacta aunque sea ≥ 1000 (el prellenado del diálogo hace el ida-y-vuelta
    // formatEditableQuantity → parseManualQuantity sin millares ni redondeo).
    @Test
    fun editingKeepsALargeQuantityUnchangedThroughTheDialog() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Leche")
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("1500")
        selectUnit("ml")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        val expectedLine = "Leche (${formatQuantity(1500.0, Locale("es", "ES"))} ml)"
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(expectedLine).fetchSemanticsNodes().isNotEmpty()
        }

        val itemId = ShoppingListRules.itemIdFor("Leche")
        composeRule.onNodeWithTag(ShoppingListTestTags.edit(itemId)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EDIT_DIALOG).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText(expectedLine).assertIsDisplayed()
    }

    // --- agrupar-y-completar-la-lista (S1–S10) -------------------------------

    // S1: agrupación por defecto por Pasillo con cabecera y contador,
    // «Sin clasificar» al final y el selector en «Pasillo de Super».
    @Test
    fun itemsAreGroupedByAisleByDefaultWithUncategorizedLast() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.saveManualItem(id = null, name = "Manzana", quantity = 1.0, unit = "kg", aisle = Aisle.PRODUCE)
            repository.saveManualItem(id = null, name = "Leche", quantity = 1.0, unit = "l", aisle = Aisle.DAIRY)
            repository.saveManualItem(id = null, name = "Toallitas", quantity = null, unit = "", aisle = null)
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
            )
        }
        launchShoppingList(ShoppingListViewModel(repository))

        // D1: el modo por defecto es «Pasillo de Super».
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_AISLE).assertIsOn()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).assertIsOff()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("PRODUCE")) and hasText("Verduras & Frescos"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("PRODUCE")) and hasText("1 ítem"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("DAIRY")) and hasText("Lácteos"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("")) and hasText("Sin clasificar"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("")) and hasText("2 ítems"),
        ).assertIsDisplayed()
        // D8: orden del catálogo de Pasillos y «Sin clasificar» al final.
        assertTrue(
            topOf(ShoppingListTestTags.groupHeader("PRODUCE")) <
                topOf(ShoppingListTestTags.groupHeader("DAIRY")),
        )
        assertTrue(
            topOf(ShoppingListTestTags.groupHeader("DAIRY")) <
                topOf(ShoppingListTestTags.groupHeader("")),
        )
    }

    // S2 (verify 1): con ítems de dos recetas, «Por Receta» deja cada ítem bajo
    // su cabecera y los contadores cuadran.
    @Test
    fun itemsFromTwoRecipesAreGroupedUnderTheirRecipe() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(
                    RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                    RecipeIngredient(name = "Arroz", quantity = 200.0, unit = "g"),
                ),
            )
            repository.addMissing(
                sourceKey = "gazpacho",
                sourceTitle = "Gazpacho",
                ingredients = listOf(
                    RecipeIngredient(name = "Tomate", quantity = 2.0, unit = "ud"),
                    RecipeIngredient(name = "Pan", quantity = 1.0, unit = "ud"),
                ),
            )
        }
        launchShoppingList(ShoppingListViewModel(repository))
        selectRecipeGrouping()

        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("gazpacho")) and hasText("Gazpacho"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("gazpacho")) and hasText("2 ítems"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupHeader("paella")) and hasText("Paella"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("paella")) and hasText("2 ítems"),
        ).assertIsDisplayed()
        // Cada fila queda tras la cabecera de su Receta y antes de la siguiente.
        val pan = ShoppingListRules.itemIdFor("Pan")
        val tomate = ShoppingListRules.itemIdFor("Tomate")
        val arroz = ShoppingListRules.itemIdFor("Arroz")
        val azafran = ShoppingListRules.itemIdFor("Azafrán")
        assertTrue(
            topOf(ShoppingListTestTags.groupHeader("gazpacho")) <
                topOf(ShoppingListTestTags.item(pan)),
        )
        assertTrue(topOf(ShoppingListTestTags.item(pan)) < topOf(ShoppingListTestTags.item(tomate)))
        assertTrue(
            topOf(ShoppingListTestTags.item(tomate)) <
                topOf(ShoppingListTestTags.groupHeader("paella")),
        )
        assertTrue(
            topOf(ShoppingListTestTags.groupHeader("paella")) <
                topOf(ShoppingListTestTags.item(arroz)),
        )
        assertTrue(topOf(ShoppingListTestTags.item(arroz)) < topOf(ShoppingListTestTags.item(azafran)))
    }

    // S3 (verify 2): al marcar 2 de 4, la tarjeta dice «2 de 4 artículos en la
    // cesta» y la barra muestra el 50 %.
    @Test
    fun theProgressBarReflectsHalfOfTheItemsCompleted() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            listOf("Leche", "Pan", "Queso", "Tomate").forEach { name ->
                repository.saveManualItem(id = null, name = name, quantity = 1.0, unit = "ud", aisle = null)
            }
        }
        launchShoppingList(ShoppingListViewModel(repository))

        composeRule.onNodeWithTag(ShoppingListTestTags.PROGRESS_TEXT)
            .assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("0 de 4 artículos en la cesta"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_PERCENT) and hasText("0%"),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.PROGRESS_BAR)
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f)))

        val pan = ShoppingListRules.itemIdFor("Pan")
        val leche = ShoppingListRules.itemIdFor("Leche")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(leche)).performClick()

        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("2 de 4 artículos en la cesta"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_PERCENT) and hasText("50%"),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.PROGRESS_BAR)
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.5f, 0f..1f)))
    }

    // S4: el ítem multi-fuente aparece en todos sus grupos; marcar una copia
    // marca todas y el progreso lo cuenta una sola vez.
    @Test
    fun multiSourceItemsAppearInEveryGroupAndCountOnce() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
            )
            repository.addMissing(
                sourceKey = "gazpacho",
                sourceTitle = "Gazpacho",
                ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 500.0, unit = "g")),
            )
        }
        launchShoppingList(ShoppingListViewModel(repository))
        selectRecipeGrouping()

        val harina = ShoppingListRules.itemIdFor("Harina")
        composeRule.onAllNodesWithTag(ShoppingListTestTags.item(harina)).assertCountEquals(2)
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("gazpacho")) and hasText("1 ítem"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.groupCount("paella")) and hasText("1 ítem"),
        ).assertIsDisplayed()

        // Marcar la copia del grupo «Paella» marca también la de «Gazpacho».
        composeRule.onAllNodesWithTag(ShoppingListTestTags.check(harina))[1].performClick()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.check(harina))[0].assertIsOn()
        composeRule.onAllNodesWithTag(ShoppingListTestTags.check(harina))[1].assertIsOn()
        // Una sola fila lógica: el progreso la cuenta una vez.
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("1 de 1 artículo en la cesta"),
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_PERCENT) and hasText("100%"),
        ).assertIsDisplayed()
    }

    // S5: desmarcar devuelve la fila a su estado normal y baja el progreso.
    @Test
    fun uncheckingAnItemRevertsTheProgress() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.saveManualItem(id = null, name = "Pan", quantity = 1.0, unit = "ud", aisle = null)
            repository.saveManualItem(id = null, name = "Leche", quantity = 1.0, unit = "l", aisle = null)
        }
        launchShoppingList(ShoppingListViewModel(repository))

        val pan = ShoppingListRules.itemIdFor("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).assertIsOn()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("1 de 2 artículos en la cesta"),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).assertIsOff()
        composeRule.onNodeWithText("Pan (1 ud)").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("0 de 2 artículos en la cesta"),
        ).assertIsDisplayed()
    }

    // S6: la fila completada conserva su nombre (tachado y atenuado) con el
    // checkbox relleno y «Limpiar comprados» aparece solo con ≥1 completado.
    @Test
    fun completedRowsKeepTheirContentAndShowTheClearAction() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.saveManualItem(id = null, name = "Pan", quantity = 2.0, unit = "uds", aisle = null)
        }
        launchShoppingList(ShoppingListViewModel(repository))

        composeRule.onAllNodesWithTag(ShoppingListTestTags.CLEAR_COMPLETED).assertCountEquals(0)
        val pan = ShoppingListRules.itemIdFor("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()

        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).assertIsOn()
        // D5: el nombre se conserva; no se sustituye por «En la cesta».
        composeRule.onNodeWithText("Pan (2 uds)").assertIsDisplayed()
        composeRule.onAllNodesWithText("En la cesta").assertCountEquals(0)
        composeRule.onAllNodesWithTag(ShoppingListTestTags.CLEAR_COMPLETED).assertCountEquals(1)
    }

    // S7: «Limpiar comprados» borra los marcados y «Deshacer» los restaura a
    // todos con su estado, cantidades y fuentes.
    @Test
    fun clearCompletedRemovesThemAndUndoRestoresThemAll() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(
                    RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                    RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g"),
                    RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g"),
                    RecipeIngredient(name = "Sal", quantity = 1.0, unit = "g"),
                ),
            )
        }
        launchShoppingList(ShoppingListViewModel(repository))

        val azafran = ShoppingListRules.itemIdFor("Azafrán")
        val harina = ShoppingListRules.itemIdFor("Harina")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(azafran)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(harina)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.CLEAR_COMPLETED).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size == 2 &&
                composeRule.onAllNodesWithTag(ShoppingListTestTags.UNDO)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ítems comprados eliminados").assertIsDisplayed()
        composeRule.onNodeWithText("Mariscos (300 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Sal (1 g)").assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingListTestTags.UNDO).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.ITEM).fetchSemanticsNodes().size == 4
        }
        composeRule.onNodeWithText("Azafrán (1 g)").assertIsDisplayed()
        composeRule.onNodeWithText("Harina (200 g)").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(azafran)).assertIsOn()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(harina)).assertIsOn()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("2 de 4 artículos en la cesta"),
        ).assertIsDisplayed()
        // Las fuentes vuelven con los ítems restaurados.
        composeRule.onAllNodesWithText("Para: Paella").assertCountEquals(4)
    }

    // S8: el contenido de la fila cambia con el modo (D6): «Pasillo» lleva
    // badge y «Por Receta» lleva etiqueta de Pasillo; el multi-fuente conserva
    // «Para: N recetas» en los dos modos.
    @Test
    fun rowContentChangesWithTheGroupingMode() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.addMissing(
                sourceKey = "paella",
                sourceTitle = "Paella",
                ingredients = listOf(
                    RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                    RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g"),
                ),
            )
            repository.addMissing(
                sourceKey = "gazpacho",
                sourceTitle = "Gazpacho",
                ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 500.0, unit = "g")),
            )
            repository.saveManualItem(
                id = null,
                name = "Azafrán",
                quantity = 1.0,
                unit = "g",
                aisle = Aisle.DAIRY,
            )
        }
        launchShoppingList(ShoppingListViewModel(repository))

        val azafran = ShoppingListRules.itemIdFor("Azafrán")
        // «Pasillo»: sin etiqueta de Pasillo, con badge de una fuente y multi.
        composeRule.onAllNodesWithTag(ShoppingListTestTags.aisle(azafran)).assertCountEquals(0)
        composeRule.onNodeWithText("Para: Paella").assertIsDisplayed()
        composeRule.onAllNodesWithText("Para: 2 recetas").assertCountEquals(1)

        // «Por Receta»: etiqueta de Pasillo y sin badge de una fuente; el
        // multi-fuente se replica con su badge en cada grupo.
        selectRecipeGrouping()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.aisle(azafran)) and hasText("Lácteos"),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Para: Paella").assertCountEquals(0)
        composeRule.onAllNodesWithText("Para: 2 recetas").assertCountEquals(2)
    }

    // S9: los completados persisten al salir y volver a entrar (Room) y el
    // selector vuelve a «Pasillo de Super» (D1).
    @Test
    fun completedItemsSurviveTheRoundTripAndTheModeResets() {
        launchFullApp()
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.NAME_FIELD).performTextReplacement("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("2")
        // D1 (unificar-unidades): sin tocar la unidad se conserva el «ud» por defecto.
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Pan (2 ud)").fetchSemanticsNodes().isNotEmpty()
        }

        val pan = ShoppingListRules.itemIdFor("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).assertIsOn()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        openShoppingList()

        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).assertIsOn()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_AISLE).assertIsOn()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).assertIsOff()
    }

    // S10: editar conserva la marca del ítem y la lista vacía no muestra la
    // tarjeta de progreso ni el selector.
    @Test
    fun editingKeepsTheCompletedStateAndTheEmptyListHasNoProgressCard() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.saveManualItem(id = null, name = "Pan", quantity = 2.0, unit = "uds", aisle = null)
        }
        launchShoppingList(ShoppingListViewModel(repository))

        val pan = ShoppingListRules.itemIdFor("Pan")
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.edit(pan)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.QUANTITY_FIELD).performTextReplacement("3")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EDIT_DIALOG).fetchSemanticsNodes().isEmpty()
        }

        // D10: la edición no cambia el estado de completado.
        composeRule.onNodeWithText("Pan (3 uds)").assertIsDisplayed()
        composeRule.onNodeWithTag(ShoppingListTestTags.check(pan)).assertIsOn()
        composeRule.onNode(
            hasTestTag(ShoppingListTestTags.PROGRESS_TEXT) and hasText("1 de 1 artículo en la cesta"),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag(ShoppingListTestTags.delete(pan)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EMPTY).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithTag(ShoppingListTestTags.PROGRESS_CARD).assertCountEquals(0)
        composeRule.onAllNodesWithTag(ShoppingListTestTags.GROUP_BY_LABEL).assertCountEquals(0)
        composeRule.onAllNodesWithTag(ShoppingListTestTags.GROUP_MODE_AISLE).assertCountEquals(0)
    }

    // unificar-unidades (verify 2, S3): el diálogo ofrece los cinco valores del enum
    // con «ud» preseleccionado en los ítems nuevos; al editar, una unidad legada
    // entra como opción extra seleccionada y la vacía se rotula «(sin unidad)», de
    // modo que conservarla no exige tocar el desplegable (D1/D5).
    @Test
    fun unitDropdownOffersTheClosedListAndKeepsLegacyUnitsAsExtraOption() {
        val repository = InMemoryShoppingListRepository()
        runBlocking {
            repository.saveManualItem(id = null, name = "Café", quantity = 1.0, unit = "cucharadas", aisle = null)
            repository.saveManualItem(id = null, name = "Toallitas", quantity = null, unit = "", aisle = null)
        }
        launchShoppingList(ShoppingListViewModel(repository))

        // Ítem nuevo: «ud» preseleccionado y las cinco opciones del enum (D1).
        composeRule.onNodeWithTag(ShoppingListTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).assertTextContains("ud")
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).performClick()
        listOf("ud", "g", "kg", "ml", "l").forEach { code ->
            composeRule.onNodeWithTag(optionTag(ShoppingListTestTags.UNIT_FIELD, code)).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(optionTag(ShoppingListTestTags.UNIT_FIELD, "kg")).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).assertTextContains("kg")
        composeRule.onNodeWithTag(ShoppingListTestTags.CANCEL_EDIT).performClick()

        // Ítem legado con unidad fuera del enum: opción extra seleccionada (D5).
        val cafe = ShoppingListRules.itemIdFor("Café")
        composeRule.onNodeWithTag(ShoppingListTestTags.edit(cafe)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).assertTextContains("cucharadas")
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).performClick()
        composeRule.onNodeWithTag(optionTag(ShoppingListTestTags.UNIT_FIELD, "cucharadas")).assertIsDisplayed()
        composeRule.onNodeWithTag(optionTag(ShoppingListTestTags.UNIT_FIELD, "cucharadas")).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EDIT_DIALOG).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Café (1 cucharadas)").assertIsDisplayed()

        // Ítem legado sin unidad: rotulado «(sin unidad)» y conservado al guardar.
        val toallitas = ShoppingListRules.itemIdFor("Toallitas")
        composeRule.onNodeWithTag(ShoppingListTestTags.edit(toallitas)).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).assertTextContains("(sin unidad)")
        composeRule.onNodeWithTag(ShoppingListTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.EDIT_DIALOG).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Toallitas").assertIsDisplayed()
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
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ShoppingListTestTags.SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Pasa el selector «Agrupar por» a «Por Receta» (RF-AGL2).
    private fun selectRecipeGrouping() {
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.GROUP_MODE_RECIPE).assertIsOn()
    }

    // Abre el desplegable de unidad del diálogo y elige una opción (unificar-unidades).
    private fun selectUnit(option: String) {
        composeRule.onNodeWithTag(ShoppingListTestTags.UNIT_FIELD).performClick()
        composeRule.onNodeWithTag(optionTag(ShoppingListTestTags.UNIT_FIELD, option)).performClick()
    }

    // Posición vertical de un nodo, para comprobar el orden de grupos y filas.
    private fun topOf(tag: String): Dp = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top

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
                        onDeleteItem = viewModel::deleteItem,
                        onUndoDelete = viewModel::undoDelete,
                        onUndoExpired = viewModel::dismissUndo,
                        onAddItem = viewModel::addItem,
                        onEditItem = viewModel::editItem,
                        onDismissEditor = viewModel::dismissEditor,
                        onSaveItem = viewModel::saveManualItem,
                        onToggleCompleted = viewModel::toggleCompleted,
                        onSetGroupMode = viewModel::setGroupMode,
                        onClearCompleted = viewModel::clearCompleted,
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
        override suspend fun reply(
            turns: List<PincheTurn>,
            language: String,
            profile: UserProfile,
        ): PincheReplyResult {
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
