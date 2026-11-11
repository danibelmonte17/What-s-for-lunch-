package com.whatsforlunch.app

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.app.ActivityOptionsCompat
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

// verify 2 · RF-9, RF-10, RF-11, RF-6, RF-7, RF-8: textos visibles localizados es/en,
// navegación a los destinos provisionales con vuelta atrás y fallback a español.
@RunWith(AndroidJUnit4::class)
class MyRecipesLocalizationE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            Manifest.permission.CAMERA,
        )
    }

    private fun showApp(locale: Locale) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides TestActivityResultRegistryOwner,
            ) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
    }

    @Test
    fun spanishEmptyStateShowsSpanishTexts() {
        showApp(Locale.forLanguageTag("es-ES"))
        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("Mi recetario")
        composeRule.onNodeWithText("Tu recetario está vacío").assertIsDisplayed()
        composeRule.onNodeWithText("Comienza a guardar tus recetas favoritas o descubre qué cocinar hoy según lo que tengas en casa.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Crear una receta").assertIsDisplayed()
        composeRule.onNodeWithText("Escanear despensa con IA").assertIsDisplayed()
    }

    @Test
    fun englishEmptyStateShowsEnglishTexts() {
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("My recipe book")
        composeRule.onNodeWithText("Your recipe book is empty").assertIsDisplayed()
        composeRule.onNodeWithText("Start saving your favorite recipes or discover what to cook today with what you have at home.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Create a recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Scan your Pantry with AI").assertIsDisplayed()
    }

    @Test
    fun unsupportedLocaleFallsBackToSpanish() {
        showApp(Locale.forLanguageTag("fr-FR"))
        composeRule.onNodeWithText("Tu recetario está vacío").assertIsDisplayed()
        composeRule.onNodeWithText("Crear una receta").assertIsDisplayed()
        composeRule.onNodeWithText("Escanear despensa con IA").assertIsDisplayed()
    }

    @Test
    fun createRecipeOpensFormAndReturns() {
        showApp(Locale.forLanguageTag("es-ES"))
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Tu recetario está vacío").assertIsDisplayed()
    }

    @Test
    fun scanPantryOpensProvisionalScannerAndReturns() {
        showApp(Locale.forLanguageTag("es-ES"))
        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Escáner de la Despensa").assertIsDisplayed()
        composeRule.onNodeWithText("Nombre del alimento").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_PREVIEW).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_PREVIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ScannerTestTags.CAMERA_PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Tu recetario está vacío").assertIsDisplayed()
    }

    @Test
    fun englishDestinationsShowEnglishTexts() {
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Create recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Your recipe book is empty").assertIsDisplayed()

        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Pantry Scanner").assertIsDisplayed()
        composeRule.onNodeWithText("Food name").assertIsDisplayed()
        composeRule.onNodeWithText("Scan with the camera").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Your recipe book is empty").assertIsDisplayed()
    }

    // S3 (RF-MOB1, RF-MOB3, verify 3): las etiquetas de la barra inferior y los
    // textos accesibles de los dos FABs siguen el idioma de la app.
    @Test
    fun spanishBottomBarAndFabsShowSpanishTexts() {
        seedRecipe()
        showApp(Locale.forLanguageTag("es-ES"))
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).assertTextEquals("Recetario")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).assertTextEquals("Despensa")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).assertTextEquals("Lista de la Compra")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).assertTextEquals("Pinche")
        composeRule.onNodeWithContentDescription("Recetas con mi Despensa").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Escanear despensa con IA").assertIsDisplayed()
    }

    @Test
    fun englishBottomBarAndFabsShowEnglishTexts() {
        seedRecipe()
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).assertTextEquals("Recipe book")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).assertTextEquals("Pantry")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).assertTextEquals("Shopping List")
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).assertTextEquals("Pinche")
        composeRule.onNodeWithContentDescription("Recipes with my Pantry").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Scan your Pantry with AI").assertIsDisplayed()
    }

    // Los FABs solo se muestran con Recetas (D3), así que S3 siembra una antes de
    // mostrar la app.
    private fun seedRecipe() {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomRecipeRepository(database.recipeDao(), LocalizationNoOpPhotoStore)
        runBlocking { repository.saveRecipe(Recipe(id = "tortilla", title = "Tortilla")) }
    }
}

private object LocalizationNoOpPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) = Unit
}

private object TestActivityResultRegistryOwner : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            error("The test grants CAMERA before composing the scanner route")
        }
    }
}
