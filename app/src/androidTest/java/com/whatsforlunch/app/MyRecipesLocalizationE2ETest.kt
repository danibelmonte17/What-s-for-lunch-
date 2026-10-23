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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.app.ActivityOptionsCompat
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        showApp(Locale("es", "ES"))
        composeRule.onNodeWithText("Mis Recetas").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
        composeRule.onNodeWithText("Crea tu primera receta o escanea la Despensa para empezar.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeRule.onNodeWithText("Escanear la Despensa").assertIsDisplayed()
    }

    @Test
    fun englishEmptyStateShowsEnglishTexts() {
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithText("My Recipes").assertIsDisplayed()
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create your first recipe or scan your Pantry to get started.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Create recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Scan your Pantry").assertIsDisplayed()
    }

    @Test
    fun unsupportedLocaleFallsBackToSpanish() {
        showApp(Locale("fr", "FR"))
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
        composeRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeRule.onNodeWithText("Escanear la Despensa").assertIsDisplayed()
    }

    @Test
    fun createRecipeOpensFormAndReturns() {
        showApp(Locale("es", "ES"))
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
    }

    @Test
    fun scanPantryOpensProvisionalScannerAndReturns() {
        showApp(Locale("es", "ES"))
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
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
    }

    @Test
    fun englishDestinationsShowEnglishTexts() {
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Create recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()

        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Pantry Scanner").assertIsDisplayed()
        composeRule.onNodeWithText("Food name").assertIsDisplayed()
        composeRule.onNodeWithText("Scan with the camera").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }
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
