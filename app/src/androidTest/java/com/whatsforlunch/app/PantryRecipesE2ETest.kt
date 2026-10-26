package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.RecipeAvoidance
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.RecipeSuggestionsResult
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsScreen
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsTestTags
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsViewModel
import com.whatsforlunch.app.ui.recommendations.RecommendationsScreen
import com.whatsforlunch.app.ui.recommendations.RecommendationsTestTags
import com.whatsforlunch.app.ui.recommendations.RecommendationsViewModel
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailScreen
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S8 de separar-despensa-y-busqueda-de-recetas: la pantalla «Recetas con mi
// Despensa» (pantry_recipes) es donde viven las Recetas Compatibles (RD02) y la
// sugerencia con IA desde que el Escáner solo escanea (RF-SDR1 a RF-SDR8).
@RunWith(AndroidJUnit4::class)
class PantryRecipesE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1 (verify 1): título propio, tarjetas ordenadas por Coincidencia, badge y
    // desglose; un Borrador entra igual que una Publicada (RF-SDR2).
    @Test
    fun recommendationsListShowsOrderedCardsWithBadgesAndIngredientBreakdown() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val full = recipe(id = "full", title = "Tortilla de patatas", required = 4, status = RecipeStatus.PUBLISHED)
        val eightyFive = recipe(id = "eighty-five", title = "Pechuga al horno", required = 20, status = RecipeStatus.PUBLISHED)
        val draft = recipe(id = "draft", title = "Borrador compatible", required = 2, status = RecipeStatus.DRAFT)
        runBlocking {
            recipes.saveRecipe(full)
            recipes.saveRecipe(eightyFive)
            recipes.saveRecipe(draft)
            addPantry(pantry, full, 4)
            addPantry(pantry, eightyFive, 17)
            addPantry(pantry, draft, 2)
        }

        launchRecommendations(recipes, pantry)

        composeRule.onNodeWithTag(RecommendationsTestTags.TITLE).assertTextEquals("Recetas con mi Despensa")
        composeRule.onNodeWithTag(RecommendationsTestTags.SECTION_TITLE).assertTextEquals("Recetas Compatibles")
        composeRule.onNodeWithTag(RecommendationsTestTags.card("full")).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.card("draft")).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.card("eighty-five")).assertIsDisplayed()

        val fullTop = composeRule.onNodeWithTag(RecommendationsTestTags.card("full"))
            .fetchSemanticsNode().boundsInRoot.top
        val draftTop = composeRule.onNodeWithTag(RecommendationsTestTags.card("draft"))
            .fetchSemanticsNode().boundsInRoot.top
        val eightyFiveTop = composeRule.onNodeWithTag(RecommendationsTestTags.card("eighty-five"))
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(draftTop < fullTop)
        assertTrue(fullTop < eightyFiveTop)

        composeRule.onAllNodesWithText("100% Match").assertCountEquals(2)
        composeRule.onAllNodesWithText("85%").assertCountEquals(1)
        composeRule.onNodeWithTag(RecommendationsTestTags.ingredients("full"))
            .assertTextEquals("4 de 4 ingredientes en la Despensa")
        composeRule.onNodeWithTag(RecommendationsTestTags.ingredients("eighty-five"))
            .assertTextEquals("17 de 20 ingredientes en la Despensa")
    }

    // S8: 84 % y las Recetas sin obligatorios quedan fuera (RF-MRC1, RF-MRC4 intactos).
    @Test
    fun recipesBelowThresholdAndWithoutRequiredIngredientsAreNotListed() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val eightyFour = recipe(id = "eighty-four", title = "Pasta", required = 19)
        runBlocking {
            recipes.saveRecipe(eightyFour)
            recipes.saveRecipe(
                Recipe(
                    id = "optional-only",
                    title = "Solo opcionales",
                    ingredients = listOf(RecipeIngredient(id = "salt", name = "Sal", optional = true)),
                ),
            )
            addPantry(pantry, eightyFour, 16)
            pantry.savePantryItem(PantryItem("salt", "Sal"))
        }

        launchRecommendations(recipes, pantry)

        composeRule.onNodeWithTag(RecommendationsTestTags.card("eighty-four")).assertDoesNotExist()
        composeRule.onNodeWithTag(RecommendationsTestTags.card("optional-only")).assertDoesNotExist()
        composeRule.onNodeWithTag(RecommendationsTestTags.EMPTY).assertIsDisplayed()
    }

    // S6: la lista se recalcula en caliente al añadir/eliminar Alimentos y al crear o
    // eliminar Recetas, sin reabrir la pantalla (RF-SDR3).
    @Test
    fun listUpdatesReactivelyWhenPantryAndRecipesChange() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val target = recipe(id = "reactive", title = "Ensalada", required = 19)
        runBlocking {
            recipes.saveRecipe(target)
            addPantry(pantry, target, 16)
        }

        launchRecommendations(recipes, pantry)
        composeRule.onNodeWithTag(RecommendationsTestTags.card("reactive")).assertDoesNotExist()

        runBlocking { addPantry(pantry, target, 17) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("reactive"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(RecommendationsTestTags.card("reactive")).assertIsDisplayed()

        runBlocking { pantry.deletePantryItem("pantry-reactive-1") }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("reactive"))
                .fetchSemanticsNodes().isEmpty()
        }

        runBlocking {
            addPantry(pantry, target, 17)
            recipes.deleteRecipe("reactive")
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("reactive"))
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(RecommendationsTestTags.EMPTY).assertIsDisplayed()
    }

    // S4: sin Despensa el estado vacío se ve y «Sugerir receta con IA» queda
    // deshabilitado y no navega (RF-SDR4, D2).
    @Test
    fun aiSuggestionIsDisabledWhenPantryIsEmpty() {
        launchRecommendations(InMemoryRecipeRepository(), InMemoryPantryRepository())

        composeRule.onNodeWithTag(RecommendationsTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.SUGGEST_AI).assertIsNotEnabled()
    }

    // S3 (verify 3): con Despensa, el botón abre Sugerencias y la vuelta regresa a
    // «Recetas con mi Despensa».
    @Test
    fun aiSuggestionOpensSuggestionsAndBackReturnsToRecommendations() {
        val pantry = InMemoryPantryRepository()
        runBlocking { pantry.savePantryItem(PantryItem(id = "pantry-tomate", name = "Tomate")) }

        launchRecommendations(InMemoryRecipeRepository(), pantry)

        composeRule.onNodeWithTag(RecommendationsTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.SUGGEST_AI)
            .assertIsEnabled()
            .performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Elige los alimentos que quieres enviar").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionItem("Tomate")).assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(RecommendationsTestTags.SCREEN).assertIsDisplayed()
    }

    // RF-MRC8 conservada (S3): la sugerencia sigue disponible con Recetas compatibles.
    @Test
    fun aiSuggestionStaysAvailableWhenCompatibleRecipesExist() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val compatible = recipe(id = "compatible", title = "Tortilla de patatas", required = 2)
        runBlocking {
            recipes.saveRecipe(compatible)
            addPantry(pantry, compatible, 2)
        }

        launchRecommendations(recipes, pantry)

        composeRule.onNodeWithTag(RecommendationsTestTags.card("compatible")).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(RecommendationsTestTags.SUGGEST_AI).assertIsEnabled().performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.SELECTION_LIST).assertIsDisplayed()
    }

    // S2 (verify 2, D7): tocar la tarjeta abre el Detalle y al volver se regresa a
    // «Recetas con mi Despensa» con la lista intacta.
    @Test
    fun tappingRecommendationOpensDetailAndBackReturnsToList() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val tortilla = recipe(id = "tortilla", title = "Tortilla de patatas", required = 2)
        runBlocking {
            recipes.saveRecipe(tortilla)
            addPantry(pantry, tortilla, 2)
        }

        launchRecommendations(recipes, pantry, detailRecipeId = "tortilla")

        composeRule.onNodeWithTag(RecommendationsTestTags.card("tortilla")).performScrollTo().performClick()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla de patatas").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(RecommendationsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(RecommendationsTestTags.card("tortilla")).assertIsDisplayed()
    }

    // S7: título de pantalla, sección, desglose y botón en inglés (RF-SDR8).
    @Test
    fun recommendationsScreenIsLocalizedInEnglish() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val english = recipe(id = "english", title = "Omelette", required = 2)
        runBlocking { recipes.saveRecipe(english) }

        launchRecommendations(recipes, pantry, locale = Locale.ENGLISH)

        composeRule.onNodeWithTag(RecommendationsTestTags.TITLE).assertTextEquals("Recipes with my Pantry")
        composeRule.onNodeWithText("Compatible Recipes").assertIsDisplayed()
        composeRule.onNodeWithText("No recipes match your Pantry yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Suggest a recipe with AI").assertIsDisplayed()

        runBlocking { addPantry(pantry, english, 2) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecommendationsTestTags.card("english"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(RecommendationsTestTags.ingredients("english"))
            .assertTextEquals("2 of 2 ingredients in the Pantry")
        composeRule.onNodeWithText("100% Match").assertIsDisplayed()
    }

    private fun launchRecommendations(
        recipes: InMemoryRecipeRepository,
        pantry: InMemoryPantryRepository,
        locale: Locale = Locale("es", "ES"),
        detailRecipeId: String? = null,
    ) {
        val viewModel = RecommendationsViewModel(pantry, recipes, InMemoryShoppingListRepository())
        val detailViewModel = detailRecipeId?.let {
            RecipeDetailViewModel(recipes, pantry, InMemoryShoppingListRepository(), it)
        }
        val aiSuggestionsViewModel = AiSuggestionsViewModel(
            pantry,
            ScanDraftHolder(),
            AlwaysFailingRecipeSuggester,
            recipes,
        )
        val showDetail = mutableStateOf(false)
        val showAiSuggestions = mutableStateOf(false)

        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    when {
                        showDetail.value && detailViewModel != null -> {
                            val detailState by detailViewModel.uiState.collectAsState()
                            RecipeDetailScreen(
                                uiState = detailState,
                                onBack = { showDetail.value = false },
                                onStartCooking = {},
                            )
                        }
                        showAiSuggestions.value -> {
                            val suggestionsState by aiSuggestionsViewModel.uiState.collectAsState()
                            AiSuggestionsScreen(
                                uiState = suggestionsState,
                                onBack = { showAiSuggestions.value = false },
                                onOpenSettings = {},
                                onToggleFood = aiSuggestionsViewModel::toggleFood,
                                onRequest = { aiSuggestionsViewModel.requestSuggestions("es") },
                                onRetry = aiSuggestionsViewModel::retry,
                                onAccept = aiSuggestionsViewModel::acceptSuggestion,
                                onDiscard = aiSuggestionsViewModel::discardSuggestion,
                                onClearNotice = aiSuggestionsViewModel::clearNotice,
                            )
                        }
                        else -> RecommendationsScreen(
                            uiState = state,
                            onBack = {},
                            onOpenRecipe = {
                                assertEquals(detailRecipeId, it)
                                showDetail.value = true
                            },
                            onSuggestWithAi = { showAiSuggestions.value = true },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun recipe(
        id: String,
        title: String,
        required: Int,
        status: RecipeStatus = RecipeStatus.PUBLISHED,
    ): Recipe = Recipe(
        id = id,
        title = title,
        status = status,
        ingredients = (1..required).map { index ->
            RecipeIngredient(id = "$id-$index", name = "$id ingredient $index")
        },
    )

    private suspend fun addPantry(pantry: InMemoryPantryRepository, recipe: Recipe, available: Int) {
        recipe.ingredients.take(available).forEach { ingredient ->
            pantry.savePantryItem(PantryItem(id = "pantry-${ingredient.id}", name = ingredient.name))
        }
    }

    private object AlwaysFailingRecipeSuggester : RecipeSuggester {
        override suspend fun suggest(
            foods: List<SuggestableFood>,
            language: String,
            existingRecipes: List<RecipeAvoidance>,
        ): RecipeSuggestionsResult = RecipeSuggestionsResult.Failed(AiFailureCause.PROVIDER_ERROR)
    }
}
