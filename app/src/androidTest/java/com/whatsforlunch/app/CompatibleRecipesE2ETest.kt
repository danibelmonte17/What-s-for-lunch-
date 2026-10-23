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
import com.whatsforlunch.app.R
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.ui.nav.ProvisionalScreen
import com.whatsforlunch.app.ui.nav.ProvisionalTestTags
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailScreen
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailViewModel
import com.whatsforlunch.app.ui.scanner.ScannerScreen
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S8 de mostrar-recetas-compatibles: la sección «Recetas Compatibles» del
// Escáner calculada contra la Despensa persistida (D1), ordenada por Coincidencia
// (D6) y con apertura del Detalle (D7) y estado vacío hacia IA (D8).
@RunWith(AndroidJUnit4::class)
class CompatibleRecipesE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1 (verify) + S6: 100 % y 85 % ordenados, etiquetados y con su desglose;
    // un Borrador entra igual que una Publicada (D4).
    @Test
    fun compatibleRecipesAreOrderedByMatchWithBadgesAndIngredientBreakdown() {
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

        launchScanner(recipes, pantry)

        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_TITLE).assertTextEquals("Recetas Compatibles")
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_SECTION).performScrollTo()
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("full")).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("draft")).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("eighty-five")).assertIsDisplayed()

        val fullTop = composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("full"))
            .fetchSemanticsNode().boundsInRoot.top
        val draftTop = composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("draft"))
            .fetchSemanticsNode().boundsInRoot.top
        val eightyFiveTop = composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("eighty-five"))
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(draftTop < fullTop)
        assertTrue(fullTop < eightyFiveTop)

        composeRule.onAllNodesWithText("100% Match").assertCountEquals(2)
        composeRule.onAllNodesWithText("85%").assertCountEquals(1)
        composeRule.onNodeWithTag(ScannerTestTags.compatibleIngredients("full"))
            .assertTextEquals("4 de 4 ingredientes en la Despensa")
        composeRule.onNodeWithTag(ScannerTestTags.compatibleIngredients("eighty-five"))
            .assertTextEquals("17 de 20 ingredientes en la Despensa")
    }

    // S2: 84 % y 50 % quedan fuera del umbral de RD02.
    @Test
    fun recipesBelowThresholdAreNotListed() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val eightyFour = recipe(id = "eighty-four", title = "Pasta", required = 19)
        val fifty = recipe(id = "fifty", title = "Sopa", required = 2)
        runBlocking {
            recipes.saveRecipe(eightyFour)
            recipes.saveRecipe(fifty)
            addPantry(pantry, eightyFour, 16)
            addPantry(pantry, fifty, 1)
        }

        launchScanner(recipes, pantry)

        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("eighty-four")).assertDoesNotExist()
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("fifty")).assertDoesNotExist()
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_EMPTY).assertIsDisplayed()
    }

    // S3: la sección se recalcula al cambiar la Despensa sin relanzar la pantalla.
    @Test
    fun sectionUpdatesReactivelyWhenPantryReachesThreshold() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val recipe = recipe(id = "reactive", title = "Ensalada", required = 19)
        runBlocking {
            recipes.saveRecipe(recipe)
            addPantry(pantry, recipe, 16)
        }

        launchScanner(recipes, pantry)
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("reactive")).assertDoesNotExist()

        runBlocking { addPantry(pantry, recipe, 17) }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ScannerTestTags.compatibleCard("reactive"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("reactive")).assertIsDisplayed()
    }

    // S4 (D8): sin compatibles → estado vacío + «Sugerir receta con IA» → destino
    // provisional con vuelta atrás.
    @Test
    fun emptyStateOpensAiSuggestionsProvisionalScreen() {
        launchScanner(InMemoryRecipeRepository(), InMemoryPantryRepository())

        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_SUGGEST_AI).performClick()

        composeRule.onNodeWithTag(ProvisionalTestTags.AI_SUGGESTIONS_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Sugerir receta con IA").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    // RF-MRC8: la sugerencia con IA sigue disponible aunque existan Recetas compatibles.
    @Test
    fun aiSuggestionIsAvailableWhenCompatibleRecipesExist() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val compatible = recipe(id = "compatible", title = "Tortilla de patatas", required = 2)
        runBlocking {
            recipes.saveRecipe(compatible)
            addPantry(pantry, compatible, 2)
        }

        launchScanner(recipes, pantry)

        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("compatible")).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_SUGGEST_AI).performScrollTo().performClick()

        composeRule.onNodeWithTag(ProvisionalTestTags.AI_SUGGESTIONS_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Sugerir receta con IA").assertIsDisplayed()
    }

    // S5 (D7): tocar la tarjeta abre el Detalle real y la vuelta regresa al Escáner.
    @Test
    fun tappingCompatibleCardOpensRecipeDetailAndBackReturnsToScanner() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val recipe = recipe(id = "tortilla", title = "Tortilla de patatas", required = 2)
        runBlocking {
            recipes.saveRecipe(recipe)
            addPantry(pantry, recipe, 2)
        }

        launchScanner(recipes, pantry, detailRecipeId = "tortilla")

        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("tortilla")).performScrollTo().performClick()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla de patatas").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    // S7: una Receta sin obligatorios no aparece aunque la Despensa esté llena.
    @Test
    fun recipeWithoutRequiredIngredientsNeverAppears() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "optional-only",
                    title = "Solo opcionales",
                    ingredients = listOf(RecipeIngredient(id = "salt", name = "Sal", optional = true)),
                ),
            )
            pantry.savePantryItem(PantryItem("salt", "Sal"))
        }

        launchScanner(recipes, pantry)

        composeRule.onNodeWithTag(ScannerTestTags.compatibleCard("optional-only")).assertDoesNotExist()
        composeRule.onNodeWithTag(ScannerTestTags.COMPATIBLE_EMPTY).assertIsDisplayed()
    }

    // S8: cabecera, línea de ingredientes, estado vacío y botón en inglés; la sección
    // se actualiza al entrar el último Alimento.
    @Test
    fun compatibleSectionIsLocalizedInEnglish() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val recipe = recipe(id = "english", title = "Omelette", required = 2)
        runBlocking { recipes.saveRecipe(recipe) }

        launchScanner(recipes, pantry, locale = Locale.ENGLISH)

        composeRule.onNodeWithText("Compatible Recipes").assertIsDisplayed()
        composeRule.onNodeWithText("No recipes match your Pantry yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Suggest a recipe with AI").assertIsDisplayed()

        runBlocking { addPantry(pantry, recipe, 2) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(ScannerTestTags.compatibleCard("english"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ScannerTestTags.compatibleIngredients("english"))
            .assertTextEquals("2 of 2 ingredients in the Pantry")
        composeRule.onNodeWithText("100% Match").assertIsDisplayed()
    }

    private fun launchScanner(
        recipes: InMemoryRecipeRepository,
        pantry: InMemoryPantryRepository,
        locale: Locale = Locale("es", "ES"),
        detailRecipeId: String? = null,
    ) {
        val viewModel = ScannerViewModel(pantry, recipes, AlwaysFailingScanAnalyzer)
        viewModel.onCameraPermissionDenied()
        val detailViewModel = detailRecipeId?.let { RecipeDetailViewModel(recipes, pantry, it) }
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
                        showAiSuggestions.value -> ProvisionalScreen(
                            title = localeContext.getString(R.string.ai_suggestions_title),
                            testTag = ProvisionalTestTags.AI_SUGGESTIONS_SCREEN,
                            onBack = { showAiSuggestions.value = false },
                        )
                        else -> ScannerScreen(
                            uiState = state,
                            previewContent = {},
                            onBack = {},
                            onOpenScanner = {},
                            onCloseScanner = {},
                            onCapture = {},
                            onRequestExit = { true },
                            onConfirmExit = {},
                            onCancelExit = {},
                            onAddManualItem = viewModel::addManualItem,
                            onRenameItem = viewModel::renameItem,
                            onQuantityChange = viewModel::updateQuantity,
                            onRemoveItem = viewModel::removeItem,
                            onSave = viewModel::saveToPantry,
                            onClearNotice = viewModel::clearNotice,
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

    private object AlwaysFailingScanAnalyzer : ScanImageAnalyzer {
        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult = ScanAnalysisResult.Failed
    }
}
