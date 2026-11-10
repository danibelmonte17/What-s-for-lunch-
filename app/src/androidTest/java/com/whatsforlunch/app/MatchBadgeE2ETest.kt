package com.whatsforlunch.app

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchBadgeE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsIngredientPercentageAndReactsToPantryChanges() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        runBlocking {
            recipes.saveRecipe(
                recipe(
                    id = "partial",
                    title = "Pollo con arroz",
                    ingredients = listOf(
                        RecipeIngredient(id = "chicken", name = "Pollo"),
                        RecipeIngredient(id = "rice", name = "Arroz"),
                    ),
                ),
            )
            recipes.saveRecipe(
                recipe(
                    id = "normalized",
                    title = "Tomate",
                    ingredients = listOf(RecipeIngredient(id = "tomato", name = " TOMÁTE fresco ")),
                ),
            )
            recipes.saveRecipe(
                // D12: sin Ingredientes no hay Coincidencia y no se pinta badge.
                recipe(
                    id = "empty",
                    title = "Sin ingredientes",
                    ingredients = emptyList(),
                ),
            )
            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("chicken", "Pollo")))
            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("tomato", "tomate fresco")))
        }

        val viewModel = MyRecipesViewModel(recipes, pantry)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val spanishContext = baseContext.createConfigurationContext(
            Configuration(baseContext.resources.configuration).apply { setLocale(java.util.Locale("es", "ES")) },
        )
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalContext provides spanishContext,
                LocalConfiguration provides Configuration(spanishContext.resources.configuration),
                LocalResources provides spanishContext.resources,
            ) {
                WhatsForLunchTheme {
                    MyRecipesScreen(
                        uiState = state,
                        onCreateRecipe = {},
                        onScanPantry = {},
                    )
                }
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 3
        }
        composeRule.onNodeWithText("50%").assertIsDisplayed()
        composeRule.onNodeWithText("100% Match").assertIsDisplayed()
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).assertCountEquals(2)
        val badgeNodes = composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).fetchSemanticsNodes()
        assertTrue(
            "Expected localized badge description, got ${badgeNodes.map { it.config }}",
            badgeNodes.any {
                it.config[SemanticsProperties.ContentDescription] == listOf("Coincidencia con la Despensa: 50%")
            },
        )

        runBlocking {
            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("rice", "Arroz")))
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("100% Match").fetchSemanticsNodes().size == 2
        }
        assertTrue(viewModel.uiState.value.matches.getValue("partial").isFullMatch)

        runBlocking { pantry.deletePantryItem("lemon") }
        composeRule.waitUntil(5_000) {
            viewModel.uiState.value.matches.getValue("partial").percentage == 100
        }
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).assertCountEquals(2)
    }

    @Test
    fun recipeCardWithPhotoShowsExactlyOneMatchBadge() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val photoFile = writePhoto()
        runBlocking {
            recipes.saveRecipe(
                recipe(
                    id = "with-photo",
                    title = "Tortilla con foto",
                    ingredients = listOf(RecipeIngredient(id = "egg", name = "Huevo")),
                    photoPath = photoFile.absolutePath,
                ),
            )
            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("egg", "Huevo")))
        }

        val viewModel = MyRecipesViewModel(recipes, pantry)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            WhatsForLunchTheme {
                MyRecipesScreen(
                    uiState = state,
                    onCreateRecipe = {},
                    onScanPantry = {},
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_PHOTO).fetchSemanticsNodes().size == 1 &&
                composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).fetchSemanticsNodes().size == 1
        }
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_MATCH).assertCountEquals(1)
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_MATCH).assertIsDisplayed()
    }

    private fun recipe(
        id: String,
        title: String,
        ingredients: List<RecipeIngredient>,
        photoPath: String? = null,
    ) = Recipe(
        id = id,
        title = title,
        ingredients = ingredients,
        photoPath = photoPath,
    )

    private fun writePhoto(): File {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        val photoFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "match-badge-photo-${System.nanoTime()}.png",
        )
        photoFile.writeBytes(output.toByteArray())
        return photoFile
    }
}
