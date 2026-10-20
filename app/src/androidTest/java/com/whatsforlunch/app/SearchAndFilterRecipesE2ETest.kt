package com.whatsforlunch.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchAndFilterRecipesE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var screenContext: Context

    private fun text(resId: Int): String = screenContext.getString(resId)

    private fun launchScreen(
        repository: InMemoryRecipeRepository = repositoryWithRecipes(),
        locale: Locale = Locale("es", "ES"),
    ): MyRecipesViewModel {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        screenContext = baseContext.createConfigurationContext(configuration)
        val viewModel = MyRecipesViewModel(repository, InMemoryPantryRepository())
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides screenContext) {
                WhatsForLunchTheme {
                    MyRecipesScreen(
                        uiState = state,
                        onCreateRecipe = {},
                        onScanPantry = {},
                        onEditRecipe = {},
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onMealMomentSelected = viewModel::onMealMomentSelected,
                        onResetFilters = viewModel::onResetFilters,
                    )
                }
            }
        }
        return viewModel
    }

    @Test
    fun filtersByDinnerAndOffersAllMomentsWithAllSelectedByDefault() {
        launchScreen()
        waitForRecipeCount(3)

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.FILTER_CHIP_ALL).assertIsSelected()
        MealMoment.entries.forEach { moment ->
            composeRule.onAllNodesWithTag(MyRecipesTestTags.filterChip(moment)).assertCountEquals(1)
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).performClick()

        waitForRecipeCount(1)
        composeRule.onNodeWithText("Pollo asado").assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).assertIsSelected()
    }

    @Test
    fun searchAndMomentControlsExposeTheirVisualStructure() {
        launchScreen()
        waitForRecipeCount(3)

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onAllNodesWithTag(
            MyRecipesTestTags.SEARCH_ICON,
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            MyRecipesTestTags.FILTER_CHIP_ALL_ICON,
            useUnmergedTree = true,
        ).assertCountEquals(1)
        MealMoment.entries.forEach { moment ->
            composeRule.onAllNodesWithTag(MyRecipesTestTags.filterChip(moment)).assertCountEquals(1)
            composeRule.onAllNodesWithTag(
                MyRecipesTestTags.filterChipIcon(moment),
                useUnmergedTree = true,
            ).assertCountEquals(1)
        }

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("sin coincidencia")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.NO_RESULTS_CARD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.FILTER_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.NO_RESULTS_CARD).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.NO_RESULTS_ICON_CONTAINER).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.NO_RESULTS_ICON).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RESET_FILTERS).assertIsDisplayed()
    }

    @Test
    fun searchesIngredientsCombinesCriteriaShowsNoResultsAndResets() {
        launchScreen()
        waitForRecipeCount(3)
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("Cebolla")

        waitForRecipeCount(2)
        composeRule.onNodeWithText("Pollo asado").assertIsDisplayed()
        composeRule.onNodeWithText("Arroz con pollo").assertIsDisplayed()

        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).performClick()
        waitForRecipeCount(1)
        composeRule.onNodeWithText("Pollo asado").assertIsDisplayed()
        composeRule.onAllNodesWithText("Arroz con pollo").assertCountEquals(0)

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("no existe")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.NO_RESULTS).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text(R.string.my_recipes_no_results_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RESET_FILTERS).assertIsDisplayed().performClick()
        waitForRecipeCount(3)
        assertEquals("", composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        composeRule.onNodeWithTag(MyRecipesTestTags.FILTER_CHIP_ALL).assertIsSelected()
    }

    @Test
    fun clearingSearchKeepsMomentFilter() {
        val viewModel = launchScreen()
        waitForRecipeCount(3)
        viewModel.onMealMomentSelected(MealMoment.DINNER)
        waitForRecipeCount(1)
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("pollo")
        waitForRecipeCount(1)
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("")
        waitForRecipeCount(1)
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).assertIsSelected()
    }

    @Test
    fun emptyCollectionHidesSearchAndFilterControls() {
        launchScreen(repository = InMemoryRecipeRepository())
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MyRecipesTestTags.SEARCH_FIELD).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MyRecipesTestTags.FILTER_ROW).assertCountEquals(0)
    }

    @Test
    fun searchTextsFollowTheEnglishLocale() {
        launchScreen(locale = Locale("en", "US"))
        waitForRecipeCount(3)
        composeRule.onNodeWithText(text(R.string.my_recipes_search_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.my_recipes_filter_all)).assertIsDisplayed()
    }

    @Test
    fun emptyStateTextFollowsTheSpanishLocale() {
        launchScreen(repository = InMemoryRecipeRepository(), locale = Locale("es", "ES"))
        composeRule.onNodeWithText(text(R.string.my_recipes_empty_title)).assertIsDisplayed()
    }

    @Test
    fun keepsSearchAndMomentFilterWhenReturningWithinTheSession() {
        val repository = repositoryWithRecipes()
        val viewModel = MyRecipesViewModel(repository, InMemoryPantryRepository())
        val showingRecipes = mutableStateOf(true)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides InstrumentationRegistry.getInstrumentation().targetContext) {
                WhatsForLunchTheme {
                    if (showingRecipes.value) {
                        MyRecipesScreen(
                            uiState = state,
                            onCreateRecipe = { showingRecipes.value = false },
                            onScanPantry = {},
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onMealMomentSelected = viewModel::onMealMomentSelected,
                            onResetFilters = viewModel::onResetFilters,
                        )
                    } else {
                        Button(onClick = { showingRecipes.value = true }) {
                            Text(text = "Back to My Recipes")
                        }
                    }
                }
            }
        }
        waitForRecipeCount(3)
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD)
            .performTextReplacement("pollo")
        waitForRecipeCount(2)
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).performClick()
        waitForRecipeCount(1)

        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Back to My Recipes").performClick()

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).assertTextContains("pollo")
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.DINNER)).assertIsSelected()
        waitForRecipeCount(1)
    }

    private fun waitForRecipeCount(count: Int) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == count
        }
    }

    private fun repositoryWithRecipes(): InMemoryRecipeRepository {
        val repository = InMemoryRecipeRepository()
        runBlocking {
            repository.saveRecipe(recipe("1", "Pollo asado", MealMoment.DINNER, "Cebolla"))
            repository.saveRecipe(recipe("2", "Arroz con pollo", MealMoment.LUNCH, "Cebolla"))
            repository.saveRecipe(recipe("3", "Tostada", MealMoment.BREAKFAST, "Pan"))
        }
        return repository
    }

    private fun recipe(id: String, title: String, moment: MealMoment, ingredient: String) = Recipe(
        id = id,
        title = title,
        mealMoment = moment,
        ingredients = listOf(RecipeIngredient(id = "ingredient-$id", name = ingredient)),
    )
}
