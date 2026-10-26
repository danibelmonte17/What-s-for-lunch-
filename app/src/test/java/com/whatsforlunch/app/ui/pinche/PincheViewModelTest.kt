package com.whatsforlunch.app.ui.pinche

import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheProposalState
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PincheViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendAddsTheUserBubbleAndTheAssistantAnswer() = runTest {
        val client = FakePincheChatClient(listOf(success("Remueve el arroz al principio.")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("  ¿Cómo evito que se pegue?  ")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(PincheAuthor.USER, state.messages[0].author)
        assertEquals("¿Cómo evito que se pegue?", state.messages[0].text)
        assertEquals(PincheAuthor.ASSISTANT, state.messages[1].author)
        assertEquals("Remueve el arroz al principio.", state.messages[1].text)
        assertEquals("", state.input)
        assertFalse(state.isSending)
        assertEquals(1, client.calls)
        assertEquals(listOf("¿Cómo evito que se pegue?"), client.lastTurns.map { it.text })
    }

    @Test
    fun blankInputIsIgnoredAndDoesNotCallTheClient() = runTest {
        val client = FakePincheChatClient(listOf(success("hola")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("   ")
        viewModel.send("es")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(0, client.calls)
    }

    @Test
    fun inputAndSendAreTruncatedToFiveHundredCharacters() = runTest {
        val client = FakePincheChatClient(listOf(success("ok")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("a".repeat(600))
        assertEquals(500, viewModel.uiState.value.input.length)

        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(500, client.lastTurns.single().text.length)
    }

    @Test
    fun missingApiKeyShowsTheKeyNotice() = runTest {
        val viewModel = viewModel(client = FakePincheChatClient(listOf(PincheReplyResult.MissingApiKey)))

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(PincheNotice.AI_KEY_MISSING, viewModel.uiState.value.notice)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun failedTurnShowsNoticeAndRetryDoesNotDuplicateTheUserBubble() = runTest {
        val client = FakePincheChatClient(
            listOf(PincheReplyResult.Failed, success("Ahora sí.")),
        )
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()
        assertEquals(PincheNotice.FAILED, viewModel.uiState.value.notice)

        viewModel.retry("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.notice)
        assertEquals(2, state.messages.size)
        assertEquals(1, state.messages.count { it.author == PincheAuthor.USER })
        assertEquals("Ahora sí.", state.messages.last().text)
        assertEquals(2, client.calls)
    }

    @Test
    fun successWithProposalComputesMissingRequiredIngredientsFromThePantry() = runTest {
        val pantry = pantryWith(PantryItem("1", "Tomate", 3))
        val viewModel = viewModel(
            pantry = pantry,
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        val proposal = viewModel.uiState.value.messages.last().proposal
        assertEquals(listOf("Harina"), proposal?.missingIngredients)
    }

    @Test
    fun savingTheProposalStoresADraftAndMarksTheCardAsSaved() = runTest {
        val recipes = InMemoryRecipeRepository()
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            recipes = recipes,
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()
        viewModel.saveProposal(message.id)
        advanceUntilIdle()

        assertEquals(PincheProposalState.SAVED, viewModel.uiState.value.messages.last().proposalState)
        val saved = runBlocking { recipes.observeRecipes().first() }.single()
        assertEquals(PincheProposalRules.proposalRecipeId("Arroz con tomate"), saved.id)
        assertEquals(RecipeStatus.DRAFT, saved.status)
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertNull(saved.photoPath)

        viewModel.saveProposal(message.id)
        advanceUntilIdle()
        assertEquals(1, runBlocking { recipes.observeRecipes().first() }.size)
    }

    @Test
    fun failedSaveRevertsTheCardAndShowsTheSaveNotice() = runTest {
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            recipes = ThrowingRecipeRepository(),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()
        viewModel.saveProposal(message.id)
        advanceUntilIdle()

        assertEquals(PincheProposalState.PROPOSED, viewModel.uiState.value.messages.last().proposalState)
        assertEquals(PincheNotice.SAVE_FAILED, viewModel.uiState.value.notice)
    }

    @Test
    fun missingIngredientsAreNotRecalculatedWhenThePantryChanges() {
        val pantry = InMemoryPantryRepository()
        runBlocking { pantry.savePantryItem(PantryItem("1", "Tomate", 1)) }
        val viewModel = PincheViewModel(
            pantryRepository = pantry,
            recipeRepository = InMemoryRecipeRepository(),
            pincheChatClient = FakePincheChatClient(
                listOf(success("Prueba esto.", proposal())),
            ),
            shoppingListRepository = InMemoryShoppingListRepository(),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        runBlocking { pantry.savePantryItem(PantryItem("2", "Harina", 1)) }

        assertEquals(listOf("Harina"), viewModel.uiState.value.messages.last().proposal?.missingIngredients)
    }

    // RF-AFL4/D8/D9: la Ficha aporta las cantidades base con la clave de fuente de
    // la Propuesta (RD16) y queda marcada como añadida sin duplicar.
    @Test
    fun addingMissingUsesBaseQuantitiesAndMarksTheProposalAsAdded() = runTest {
        val shoppingList = InMemoryShoppingListRepository()
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
            shoppingList = shoppingList,
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.last().addedToList)
        val item = shoppingList.observeItems().first().single()
        assertEquals("Harina", item.name)
        assertEquals(1.0, item.quantity!!, 0.0001)
        assertEquals("ud", item.unit)
        assertEquals(
            listOf(PincheProposalRules.proposalRecipeId("Arroz con tomate")),
            item.sources.map { it.sourceKey },
        )

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()
        assertEquals(1, shoppingList.observeItems().first().size)
    }

    @Test
    fun failedShoppingListAddRevertsTheCardAndShowsTheNotice() = runTest {
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
            shoppingList = ThrowingShoppingListRepository(),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.messages.last().addedToList)
        assertEquals(PincheNotice.SHOPPING_LIST_FAILED, viewModel.uiState.value.notice)
    }

    @Test
    fun quickSuggestionsOnlyWorkWithAnEmptyConversation() = runTest {
        val client = FakePincheChatClient(listOf(success("Hola"), success("Otra vez")))
        val viewModel = viewModel(client = client)

        viewModel.sendQuickSuggestion("¿Qué ceno?", "es")
        advanceUntilIdle()
        assertEquals(listOf("¿Qué ceno?"), client.lastTurns.map { it.text })

        viewModel.sendQuickSuggestion("Ignorada", "es")
        advanceUntilIdle()
        assertEquals(1, client.calls)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun successWithoutTextAndProposalIsTreatedAsFailure() = runTest {
        val viewModel = viewModel(client = FakePincheChatClient(listOf(success("   "))))

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(PincheNotice.FAILED, viewModel.uiState.value.notice)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    private fun viewModel(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        recipes: RecipeRepository = InMemoryRecipeRepository(),
        client: FakePincheChatClient = FakePincheChatClient(emptyList()),
        shoppingList: ShoppingListRepository = InMemoryShoppingListRepository(),
    ) = PincheViewModel(pantry, recipes, client, shoppingList)

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun success(text: String, proposal: RecipeSuggestion? = null): PincheReplyResult.Success =
        PincheReplyResult.Success(PincheReply(text = text, proposal = proposal))

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
            RecipeIngredient(id = "2", name = "Albahaca", quantity = 1.0, unit = "ud", optional = true),
        ),
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )

    private class FakePincheChatClient(
        private val results: List<PincheReplyResult>,
    ) : PincheChatClient {
        var calls: Int = 0
            private set
        var lastTurns: List<PincheTurn> = emptyList()
            private set

        override suspend fun reply(turns: List<PincheTurn>, language: String): PincheReplyResult {
            lastTurns = turns
            if (results.isEmpty()) return PincheReplyResult.Success(PincheReply(text = "ok"))
            return results[minOf(calls++, results.lastIndex)]
        }
    }

    private class ThrowingRecipeRepository : RecipeRepository {
        private val recipes = MutableStateFlow<List<Recipe>>(emptyList())

        override fun observeRecipes(): Flow<List<Recipe>> = recipes.asStateFlow()

        override suspend fun getRecipe(id: String): Recipe? = null

        override suspend fun saveRecipe(recipe: Recipe) {
            throw IllegalStateException("save failed")
        }

        override suspend fun deleteRecipe(id: String) = Unit
    }

    private class ThrowingShoppingListRepository : ShoppingListRepository {
        override fun observeItems(): Flow<List<ShoppingListItem>> = emptyFlow()

        override suspend fun addMissing(
            sourceKey: String,
            sourceTitle: String,
            ingredients: List<RecipeIngredient>,
        ) {
            throw IllegalStateException("shopping list failed")
        }

        override suspend fun deleteItem(id: String) = Unit

        override suspend fun restoreItem(item: ShoppingListItem) = Unit
    }
}
