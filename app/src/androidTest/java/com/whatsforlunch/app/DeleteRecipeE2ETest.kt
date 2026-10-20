package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
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

private class RecordingDeletePhotoStore : PhotoStore {
    val deletedPaths = mutableListOf<String>()

    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) {
        deletedPaths += path
    }
}

private class FailingDeleteRecipeRepository(
    private val delegate: RecipeRepository,
) : RecipeRepository by delegate {
    override suspend fun deleteRecipe(id: String) {
        error("Simulated storage failure")
    }
}

// Escenarios del borrado en Mis Recetas: confirmación, cancelación, descarte, error,
// localización y accesibilidad (Parte B, T6).
@RunWith(AndroidJUnit4::class)
class DeleteRecipeE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var localizedContext: android.content.Context

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    private fun launchScreen(
        repository: RecipeRepository,
        locale: Locale = Locale("es", "ES"),
    ): MyRecipesViewModel {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        localizedContext = baseContext.createConfigurationContext(configuration)
        val localizedConfiguration = Configuration(localizedContext.resources.configuration)
        val viewModel = MyRecipesViewModel(repository)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Also provide configuration/resources: AlertDialog uses a separate window and
            // must resolve strings from this test locale rather than the device locale.
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalResources provides localizedContext.resources,
            ) {
                WhatsForLunchTheme {
                    MyRecipesScreen(
                        uiState = state,
                        onCreateRecipe = {},
                        onScanPantry = {},
                        onDeleteRecipe = viewModel::deleteRecipe,
                        onDeleteErrorConsumed = viewModel::consumeDeleteError,
                    )
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_DELETE)
                .fetchSemanticsNodes().isNotEmpty()
        }
        return viewModel
    }

    private fun repositoryWithRecipe(): InMemoryRecipeRepository = InMemoryRecipeRepository().also { repository ->
        runBlocking { repository.saveRecipe(seededRecipe) }
    }

    @Test
    fun confirmingDeleteRemovesRecipePermanentlyAndShowsEmptyState() {
        val repository = repositoryWithRecipe()
        launchScreen(repository)

        composeRule.onNodeWithContentDescription("Eliminar receta").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.DELETE_DIALOG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("Eliminar receta").assertIsDisplayed()
        composeRule.onNodeWithText("¿Eliminar «Tortilla de patatas»? Esta acción es definitiva y no se puede deshacer.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CONFIRM).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.EMPTY_TITLE).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { repository.observeRecipes().first().isEmpty() })
    }

    @Test
    fun cancellingDeleteKeepsRecipe() {
        val repository = repositoryWithRecipe()
        launchScreen(repository)

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CANCEL).performClick()

        composeRule.onNodeWithText(seededRecipe.title).assertIsDisplayed()
        assertEquals(listOf(seededRecipe), runBlocking { repository.observeRecipes().first() })
    }

    @Test
    fun pressingBackDismissesDialogWithoutDeletingRecipe() {
        val repository = repositoryWithRecipe()
        launchScreen(repository)

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.DELETE_DIALOG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        Espresso.pressBack()

        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithText(seededRecipe.title).assertIsDisplayed()
        assertEquals(listOf(seededRecipe), runBlocking { repository.observeRecipes().first() })
    }

    @Test
    fun confirmingRecipeAlreadyRemovedClosesDialogWithoutError() {
        val repository = repositoryWithRecipe()
        launchScreen(repository)
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE).performClick()
        runBlocking { repository.deleteRecipe(seededRecipe.id) }

        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CONFIRM).performClick()

        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onAllNodesWithText("No se pudo eliminar la receta. Inténtalo de nuevo.")
            .assertCountEquals(0)
    }

    @Test
    fun storageFailureShowsErrorAndKeepsRecipeVisible() {
        val repository = FailingDeleteRecipeRepository(repositoryWithRecipe())
        launchScreen(repository)

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CONFIRM).performClick()
        composeRule.onNodeWithText("No se pudo eliminar la receta. Inténtalo de nuevo.")
            .assertIsDisplayed()
        composeRule.onNodeWithText(seededRecipe.title).assertIsDisplayed()
    }

    @Test
    fun storageFailureMessageIsLocalizedInEnglish() {
        val repository = FailingDeleteRecipeRepository(repositoryWithRecipe())
        launchScreen(repository, Locale.ENGLISH)

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CONFIRM).performClick()
        composeRule.onNodeWithText("The recipe couldn’t be deleted. Please try again.").assertIsDisplayed()
        composeRule.onNodeWithText(seededRecipe.title).assertIsDisplayed()
    }

    @Test
    fun deleteDialogAndActionAreLocalizedInEnglish() {
        launchScreen(repositoryWithRecipe(), Locale.ENGLISH)

        composeRule.onNodeWithContentDescription("Delete recipe").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.DELETE_DIALOG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Delete recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Delete “Tortilla de patatas”? This action is permanent and can’t be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.DELETE_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun recipeWithPhotoDeletesItsStoredPhotoFile() = runBlocking {
        val photoStore = RecordingDeletePhotoStore()
        val repository = RoomRecipeRepository(
            GlobalContext.get().get<RecipeDatabase>().recipeDao(),
            photoStore,
        )
        val recipeWithPhoto = seededRecipe.copy(photoPath = "/recipe-photos/tortilla.jpg")
        repository.saveRecipe(recipeWithPhoto)

        repository.deleteRecipe(recipeWithPhoto.id)

        assertEquals(listOf(recipeWithPhoto.photoPath), photoStore.deletedPaths)
        assertEquals(null, repository.getRecipe(recipeWithPhoto.id))
    }

    @Test
    fun deleteActionHasAccessibleNameAndMeetsFooterSizeRequirements() {
        launchScreen(repositoryWithRecipe())
        val action = composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DELETE)
        val editAction = composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_EDIT)
        action.assertIsDisplayed()
        editAction.assertIsDisplayed()
        action.assertContentDescriptionEquals("Eliminar receta")
        composeRule.onNodeWithText("Eliminar", useUnmergedTree = true).assertIsDisplayed()
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        // El testTag mide el botón visual: 36 dp de alto. El mínimo interactivo de
        // Material 3 reserva alrededor el área táctil de 48 dp.
        val size = action.fetchSemanticsNode().size
        val editSize = editAction.fetchSemanticsNode().size
        assertTrue(size.width / density >= 48f)
        assertTrue(size.height / density >= 36f)
        assertEquals(editSize.height, size.height)
        assertTrue(
            action.fetchSemanticsNode().boundsInRoot.left >=
                editAction.fetchSemanticsNode().boundsInRoot.right,
        )
    }

    private companion object {
        val seededRecipe = Recipe(id = "delete-recipe-1", title = "Tortilla de patatas")
    }
}
