package com.whatsforlunch.app.ui.aisuggestions

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
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                RecipeSuggestionsResult.Failed,
                RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana"))),
            ),
        )
        val viewModel = viewModel(holder = holder, suggester = suggester)
        advanceUntilIdle()

        viewModel.requestSuggestions("es")
        advanceUntilIdle()
        assertEquals(AiSuggestionsNotice.FAILED, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.phase is AiSuggestionsPhase.Selection)

        viewModel.retry()
        advanceUntilIdle()

        val phase = viewModel.uiState.value.phase as AiSuggestionsPhase.Results
        assertEquals("Tarta de manzana", phase.items.single().suggestion.title)
        assertEquals(2, suggester.calls)
        assertNull(viewModel.uiState.value.notice)
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
            recipeRepository = InMemoryRecipeRepository(),
            scanImageAnalyzer = FailingScanAnalyzer,
            scanDraftHolder = holder,
        )

        scanner.addManualItem("Chicle")

        assertEquals(listOf("Chicle"), holder.items.value.map { it.name })
    }

    private fun viewModel(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        holder: ScanDraftHolder = ScanDraftHolder(),
        suggester: FakeRecipeSuggester = FakeRecipeSuggester(emptyList()),
    ) = AiSuggestionsViewModel(pantry, holder, suggester)

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            kotlinx.coroutines.runBlocking { items.forEach { savePantryItem(it) } }
        }

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

        override suspend fun suggest(
            foods: List<SuggestableFood>,
            language: String,
        ): RecipeSuggestionsResult {
            lastFoods = foods
            return results[minOf(calls++, results.lastIndex.coerceAtLeast(0))]
        }
    }

    private object FailingScanAnalyzer : ScanImageAnalyzer {
        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult =
            ScanAnalysisResult.Failed
    }
}
