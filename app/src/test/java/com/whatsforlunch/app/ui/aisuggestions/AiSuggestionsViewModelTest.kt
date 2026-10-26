package com.whatsforlunch.app.ui.aisuggestions

import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.RecipeAvoidance
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.RecipeSuggestionsResult
import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.domain.suggestion.SuggestionAcceptance
import com.whatsforlunch.app.domain.suggestion.SuggestionState
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AiSuggestionsViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectionMergesScanChipsAndPantryWithEverythingPreselected() = runTest {
        val pantry = pantryWith(PantryItem("p1", "manzana", 1), PantryItem("p2", "Tomate"))
        val holder = ScanDraftHolder().apply {
            update(listOf(ScanDraftItem(name = "Manzana", quantity = 3), ScanDraftItem(name = "Nata")))
        }
        val viewModel = viewModel(pantry = pantry, holder = holder)
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Selection
        assertEquals(listOf("Manzana", "Nata", "Tomate"), phase.foods.map { it.name })
        assertEquals(3, phase.foods.first().quantity)
        assertEquals(phase.foods.mapTo(mutableSetOf()) { it.name }, phase.selected)
        assertTrue(phase.canRequest)
    }

    @Test
    fun deselectingEveryFoodDisablesTheRequestAndAvoidsCallingTheAi() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.Success(emptyList())))
        val viewModel = viewModel(holder = holder, suggester = suggester)
        advanceUntilIdle()

        viewModel.toggleFood("Manzana")
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Selection
        assertFalse(phase.canRequest)
        assertTrue(phase.selected.isEmpty())

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        assertEquals(0, suggester.calls)
        assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Selection)
    }

    @Test
    fun missingApiKeyReturnsToSelectionWithKeyNotice() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val viewModel = viewModel(
            holder = holder,
            suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.MissingApiKey)),
        )
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()

        assertEquals(AiSuggestionsNotice.AI_KEY_MISSING, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Selection)
    }

    @Test
    fun failureShowsNoticeAndRetryRequestsAgain() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Failed(AiFailureCause.NO_CONNECTION),
                RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana"))),
            ),
        )
        val viewModel = viewModel(holder = holder, suggester = suggester)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        assertEquals(AiSuggestionsNotice.FAILED_NO_CONNECTION, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Selection)

        viewModel.retry()
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals("Tarta de manzana", phase.items.single().suggestion.title)
        assertEquals(2, suggester.calls)
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun eachFailureCauseMapsToItsOwnSuggestionsNoticeAndCanBeCleared() = runTest {
        val cases = mapOf(
            AiFailureCause.NO_CONNECTION to AiSuggestionsNotice.FAILED_NO_CONNECTION,
            AiFailureCause.PROVIDER_ERROR to AiSuggestionsNotice.FAILED_PROVIDER_ERROR,
            AiFailureCause.INVALID_RESPONSE to AiSuggestionsNotice.FAILED_INVALID_RESPONSE,
        )
        cases.forEach { (cause, expectedNotice) ->
            val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
            val viewModel = viewModel(
                holder = holder,
                suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.Failed(cause))),
            )
            advanceUntilIdle()

            viewModel.requestSuggestions("es")
            advanceUntilIdle()

            assertEquals(expectedNotice, viewModel.uiState.value.notice)
            assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Selection)

            viewModel.clearNotice()
            assertNull(viewModel.uiState.value.notice)
        }
    }

    @Test
    fun successListsSuggestionsWithClientMatchAgainstSelectedFoods() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(suggestion(title = "Tarta de manzana", required = listOf("Manzana", "Nata"))),
                ),
            ),
        )
        val viewModel = viewModel(holder = holder, suggester = suggester)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals(1, phase.items.size)
        assertEquals(50, phase.items.single().match?.percentage)
        assertEquals(1, suggester.calls)
        assertEquals(listOf("manzana"), suggester.lastFoods.map { it.name })
    }

    @Test
    fun successfulEmptyAnswerShowsEmptyState() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val viewModel = viewModel(
            holder = holder,
            suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.Success(emptyList()))),
        )
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Empty)
    }

    @Test
    fun scannerStillExposesItsChipsThroughTheSharedHolder() = runTest {
        val holder = ScanDraftHolder()
        val scanner = ScannerViewModel(
            pantryRepository = InMemoryPantryRepository(),
            scanImageAnalyzer = FailingScanAnalyzer,
            scanDraftHolder = holder,
        )

        scanner.addManualItem("Chicle")

        assertEquals(listOf("Chicle"), holder.items.value.map { it.name })
    }

    @Test
    fun acceptingSavesTheSuggestionAsADraftWithSuggestionOrigin() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(suggestion(title = "Tarta de manzana", required = listOf("Manzana", "Harina"))),
                ),
            ),
        )
        val recipes = InMemoryRecipeRepository()
        val viewModel = viewModel(holder = holder, suggester = suggester, recipes = recipes)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        viewModel.acceptSuggestion(id)
        advanceUntilIdle()

        assertEquals(AiSuggestionsNotice.SAVED, viewModel.uiState.value.notice)
        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals(SuggestionState.ACCEPTED, phase.states[id])

        val saved = recipes.observed().single()
        assertEquals(id, saved.id)
        assertEquals(RecipeStatus.DRAFT, saved.status)
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertNull(saved.photoPath)
        assertFalse(saved.favorite)
        assertEquals("Tarta de manzana", saved.title)
        assertEquals(listOf("$id-ingredient-0", "$id-ingredient-1"), saved.ingredients.map { it.id })
        assertEquals(listOf("$id-step-0"), saved.steps.map { it.id })
    }

    @Test
    fun acceptingTheSameSuggestionTwiceKeepsASingleRecipe() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana")))),
        )
        val recipes = InMemoryRecipeRepository()
        val viewModel = viewModel(holder = holder, suggester = suggester, recipes = recipes)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        viewModel.acceptSuggestion(id)
        advanceUntilIdle()
        viewModel.acceptSuggestion(id)
        advanceUntilIdle()

        assertEquals(1, recipes.observed().size)
    }

    @Test
    fun discardingKeepsTheCardVisibleWithoutSavingAnything() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana")))),
        )
        val recipes = InMemoryRecipeRepository()
        val viewModel = viewModel(holder = holder, suggester = suggester, recipes = recipes)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        viewModel.discardSuggestion(id)
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals(SuggestionState.DISCARDED, phase.states[id])
        assertEquals(1, phase.items.size)
        assertTrue(recipes.observed().isEmpty())
    }

    @Test
    fun requestSendsOnlyExistingRecipesAboveSixtyPercentToThePrompt() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de pera")))),
        )
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(storedRecipe(title = "Tarta de manzana", required = listOf("Manzana")))
            recipes.saveRecipe(storedRecipe(title = "Ensalada", required = listOf("Lechuga", "Harina")))
        }
        val viewModel = viewModel(holder = holder, suggester = suggester, recipes = recipes)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()

        assertEquals(1, suggester.lastAvoidance.size)
        assertEquals("Tarta de manzana", suggester.lastAvoidance.single().title)
        assertEquals(listOf("Manzana"), suggester.lastAvoidance.single().ingredientNames)
    }

    @Test
    fun aNewRequestStartsEverySuggestionAsProposed() = runTest {
        val holder = ScanDraftHolder().apply { update(listOf(ScanDraftItem(name = "Manzana"))) }
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(suggestion(title = "Tarta de manzana"), suggestion(title = "Tarta de pera")),
                ),
            ),
        )
        val viewModel = viewModel(holder = holder, suggester = suggester)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        viewModel.acceptSuggestion(SuggestionAcceptance.suggestionRecipeId("Tarta de manzana"))
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals(2, phase.states.size)
        assertTrue(phase.states.values.all { it == SuggestionState.PROPOSED })
    }

    private fun viewModel(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        holder: ScanDraftHolder = ScanDraftHolder(),
        suggester: FakeRecipeSuggester = FakeRecipeSuggester(emptyList()),
        recipes: InMemoryRecipeRepository = InMemoryRecipeRepository(),
    ) = AiSuggestionsViewModel(pantry, holder, suggester, recipes)

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            kotlinx.coroutines.runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun InMemoryRecipeRepository.observed(): List<Recipe> =
        kotlinx.coroutines.runBlocking { observeRecipes().first() }

    private fun storedRecipe(title: String, required: List<String>): Recipe = Recipe(
        id = title,
        title = title,
        ingredients = required.map { RecipeIngredient(name = it, quantity = 1.0) },
        steps = listOf(RecipeStep(text = "Cocina")),
    )

    private fun suggestion(
        title: String,
        required: List<String> = listOf("Manzana"),
    ) = RecipeSuggestion(
        title = title,
        story = "",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = null,
        ingredients = required.mapIndexed { index, name -> RecipeIngredient(id = "$index", name = name) },
        steps = listOf(RecipeStep(text = "Cocina")),
    )

    private class FakeRecipeSuggester(
        private val results: List<RecipeSuggestionsResult>,
    ) : RecipeSuggester {
        var calls: Int = 0
            private set
        var lastFoods: List<SuggestableFood> = emptyList()
            private set
        var lastAvoidance: List<RecipeAvoidance> = emptyList()
            private set

        override suspend fun suggest(
            foods: List<SuggestableFood>,
            language: String,
            existingRecipes: List<RecipeAvoidance>,
        ): RecipeSuggestionsResult {
            lastFoods = foods
            lastAvoidance = existingRecipes
            return results[minOf(calls++, results.lastIndex.coerceAtLeast(0))]
        }
    }

    private object FailingScanAnalyzer : ScanImageAnalyzer {
        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult =
            ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)
    }
}
