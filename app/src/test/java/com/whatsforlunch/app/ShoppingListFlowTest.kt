package com.whatsforlunch.app

import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.ui.pinche.PincheViewModel
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Integración de S5 (RD16 + RD18): añadir los Faltantes desde la Ficha del Pinche,
 * guardar la Propuesta y repetir desde el Detalle del Borrador no duplica el ÍtemLista
 * ni dobla su cantidad, porque ambas superficies comparten la clave de fuente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListFlowTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun savingTheProposalAndRepeatingFromTheDraftDoesNotDuplicateTheItem() = runTest {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository().apply {
            runBlocking { savePantryItem(PantryItem("tomate", "Tomate", 3)) }
        }
        val shoppingList = InMemoryShoppingListRepository()
        val pinche = PincheViewModel(
            pantryRepository = pantry,
            recipeRepository = recipes,
            pincheChatClient = FakePincheChatClient(
                PincheReplyResult.Success(PincheReply("Prueba esto.", proposal())),
            ),
            shoppingListRepository = shoppingList,
            // Perfil completo: sin partes pendientes la IA sí responde (RD26).
            userProfileRepository = InMemoryUserProfileRepository(
                UserProfile(allergies = listOf("Frutos secos"), intolerancesNone = true, dietType = DietType.OMNIVORE),
            ),
        )

        pinche.onInputChange("Dame una receta")
        pinche.send("es")
        advanceUntilIdle()
        val message = pinche.uiState.value.messages.last()

        pinche.addMissingToShoppingList(message.id)
        advanceUntilIdle()
        val afterFicha = shoppingList.observeItems().first().single()
        assertEquals("Harina", afterFicha.name)
        assertEquals(1.0, afterFicha.quantity!!, 0.0001)
        assertEquals("ud", afterFicha.unit)
        assertEquals(listOf("Arroz con tomate"), afterFicha.sources.map { it.sourceTitle })

        pinche.saveProposal(message.id)
        advanceUntilIdle()
        val savedId = PincheProposalRules.proposalRecipeId("Arroz con tomate")
        val saved = recipes.getRecipe(savedId)
        assertNotNull(saved)
        assertEquals(savedId, saved?.id)

        val detail = RecipeDetailViewModel(recipes, pantry, shoppingList, savedId)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            detail.uiState.collect { }
        }
        advanceUntilIdle()

        detail.addMissingToShoppingList()
        advanceUntilIdle()

        val afterDraft = shoppingList.observeItems().first()
        assertEquals(1, afterDraft.size)
        assertEquals(afterFicha.quantity, afterDraft.single().quantity)
        assertEquals(1, afterDraft.single().sources.size)
        assertEquals(savedId, afterDraft.single().sources.single().sourceKey)
        assertEquals("Arroz con tomate", afterDraft.single().sources.single().sourceTitle)
    }

    private fun proposal(): RecipeSuggestion = RecipeSuggestion(
        title = "Arroz con tomate",
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 30,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = listOf(
            RecipeIngredient(id = "0", name = "Tomate", quantity = 1.0, unit = "ud"),
            RecipeIngredient(id = "1", name = "Harina", quantity = 1.0, unit = "ud"),
        ),
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )

    private class FakePincheChatClient(
        private val result: PincheReplyResult,
    ) : PincheChatClient {
        override suspend fun reply(
            turns: List<PincheTurn>,
            language: String,
            profile: UserProfile,
        ): PincheReplyResult = result
    }
}
