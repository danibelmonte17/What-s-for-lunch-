package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.ui.misrecetas.MisRecetasTestTags
import com.whatsforlunch.app.ui.nav.ProvisionalTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// verify 2 · RF-9, RF-10, RF-11, RF-6, RF-7, RF-8: textos visibles localizados es/en,
// navegación a los destinos provisionales con vuelta atrás y fallback a español.
@RunWith(AndroidJUnit4::class)
class MisRecetasLocalizationE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showApp(locale: Locale) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
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
    fun createRecipeOpensProvisionalFormAndReturns() {
        showApp(Locale("es", "ES"))
        composeRule.onNodeWithTag(MisRecetasTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(ProvisionalTestTags.FORM_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MisRecetasTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
    }

    @Test
    fun scanPantryOpensProvisionalScannerAndReturns() {
        showApp(Locale("es", "ES"))
        composeRule.onNodeWithTag(MisRecetasTestTags.SCAN_PANTRY).performClick()
        composeRule.onNodeWithTag(ProvisionalTestTags.SCANNER_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Escanear la Despensa").assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MisRecetasTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no tienes recetas").assertIsDisplayed()
    }

    @Test
    fun englishDestinationsShowEnglishTexts() {
        showApp(Locale.ENGLISH)
        composeRule.onNodeWithTag(MisRecetasTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(ProvisionalTestTags.FORM_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Create recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()

        composeRule.onNodeWithTag(MisRecetasTestTags.SCAN_PANTRY).performClick()
        composeRule.onNodeWithTag(ProvisionalTestTags.SCANNER_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Scan your Pantry").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("No recipes yet").assertIsDisplayed()
    }
}
