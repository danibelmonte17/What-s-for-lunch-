package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.filterPantryItems
import com.whatsforlunch.app.ui.pantry.PantryScreen
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.pantry.PantryUiState
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PantryLocalizationE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pantryCopyIsLocalizedAndRowActionsMeetTouchTargetRequirement() {
        setPantry(Locale.ENGLISH, listOf(PantryItem("test-id", "Tomato", 2)))
        composeRule.onNodeWithTag(PantryTestTags.TOP_BAR_TITLE).assertTextEquals("My Pantry")
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()

        // Textos nuevos del rediseño en inglés (S6 / verify 6); con 1 Alimento
        // los contadores son «1».
        composeRule.onNodeWithTag(PantryTestTags.SECTION_SUBTITLE)
            .assertTextEquals("Ingredients registered at home")
        composeRule.onNodeWithTag(PantryTestTags.FRESH_BADGE).assertTextEquals("1 fresh food")
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_COUNT).assertTextEquals("1 food")
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_CAPACITY)
            .assertTextEquals("Estimated capacity at 3%")
        composeRule.onNodeWithTag(PantryTestTags.itemFreshness("test-id"))
            .assertTextEquals("In good shape")
        composeRule.onNodeWithTag(PantryTestTags.itemQuantity("test-id"))
            .assertTextEquals("Quantity: 2")
        composeRule.onNodeWithTag(PantryTestTags.itemExpiry("test-id"))
            .assertTextEquals("Long shelf life")
        composeRule.onNodeWithText("Search tomatoes, eggs, flour…").assertIsDisplayed()

        // Menú ⋮: «Editar»/«Eliminar» viven en el menú con sus tags y ≥48 dp.
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        composeRule.onNodeWithTag(PantryTestTags.itemMenu("test-id")).performClick()
        val editAction = composeRule.onNodeWithTag(PantryTestTags.editItem("test-id"))
        val deleteAction = composeRule.onNodeWithTag(PantryTestTags.deleteItem("test-id"))
        editAction.assertContentDescriptionEquals("Edit Tomato")
        deleteAction.assertContentDescriptionEquals("Delete Tomato")
        assert(editAction.fetchSemanticsNode().size.width / density >= 48f)
        assert(editAction.fetchSemanticsNode().size.height / density >= 48f)
        assert(deleteAction.fetchSemanticsNode().size.width / density >= 48f)
        assert(deleteAction.fetchSemanticsNode().size.height / density >= 48f)

        // Stepper inline ≥48 dp (S6 / verify 6).
        listOf(PantryTestTags.itemQuantityDecrease("test-id"), PantryTestTags.itemQuantityIncrease("test-id"))
            .forEach { tag ->
                val button = composeRule.onNodeWithTag(tag)
                assert(button.fetchSemanticsNode().size.width / density >= 48f)
                assert(button.fetchSemanticsNode().size.height / density >= 48f)
            }

        editAction.performClick()
        composeRule.onNodeWithText("Edit food").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Quantity (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Expiry date (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("No expiry date").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Decrease quantity").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase quantity").assertIsDisplayed()
        assertStepperTouchTargets()
    }

    @Test
    fun pantryStepperCopyIsLocalizedInSpanishAndButtonsMeetTouchTargetRequirement() {
        setPantry(Locale.forLanguageTag("es-ES"), listOf(PantryItem("test-id", "Tomate", 2)))

        // Cabecera, resumen y tarjeta en español (S6 / verify 6); con 1 Alimento
        // los contadores son «1».
        composeRule.onNodeWithTag(PantryTestTags.TOP_BAR_TITLE).assertTextEquals("Mi Despensa")
        composeRule.onNodeWithTag(PantryTestTags.SECTION_SUBTITLE)
            .assertTextEquals("Ingredientes registrados en casa")
        composeRule.onNodeWithTag(PantryTestTags.FRESH_BADGE).assertTextEquals("1 fresco")
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_COUNT).assertTextEquals("1 Alimento")
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_CAPACITY)
            .assertTextEquals("Capacidad estimada al 3%")
        composeRule.onNodeWithTag(PantryTestTags.itemFreshness("test-id"))
            .assertTextEquals("En buen estado")
        composeRule.onNodeWithTag(PantryTestTags.itemQuantity("test-id"))
            .assertTextEquals("Cantidad: 2")
        composeRule.onNodeWithTag(PantryTestTags.itemExpiry("test-id"))
            .assertTextEquals("Larga duración")

        composeRule.onNodeWithTag(PantryTestTags.itemMenu("test-id")).performClick()
        composeRule.onNodeWithTag(PantryTestTags.editItem("test-id")).performClick()
        composeRule.onNodeWithText("Cantidad (opcional)").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha de caducidad (opcional)").assertIsDisplayed()
        composeRule.onNodeWithText("Sin fecha de caducidad").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Disminuir cantidad").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Aumentar cantidad").assertIsDisplayed()
        assertStepperTouchTargets()
    }

    // S3 / verify 3: el estado «sin resultados» con su copy en el idioma activo.
    @Test
    fun noResultsCopyIsLocalizedWhenTheSearchHasNoMatches() {
        setPantry(Locale.ENGLISH, listOf(PantryItem("test-id", "Tomato", 2)))
        composeRule.onNodeWithTag(PantryTestTags.SEARCH_FIELD).performTextInput("zzz")
        composeRule.onNodeWithTag(PantryTestTags.NO_RESULTS).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.NO_RESULTS_TITLE)
            .assertTextEquals("No ingredients found")
        composeRule.onNodeWithTag(PantryTestTags.NO_RESULTS_ADD)
            .assertTextEquals("+ Register new food")
    }

    private fun setPantry(locale: Locale, items: List<PantryItem>) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        val localizedConfiguration = Configuration(localizedContext.resources.configuration)
        composeRule.setContent {
            // Réplica del flujo de producción: la query vive en estado y el
            // ViewModel deriva `visibleItems` (aquí, en el propio composition).
            var query by remember { mutableStateOf("") }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalResources provides localizedContext.resources,
            ) {
                WhatsForLunchTheme {
                    PantryScreen(
                        uiState = PantryUiState(
                            items = items,
                            searchQuery = query,
                            visibleItems = filterPantryItems(items, query),
                        ),
                        onScanFood = {},
                        onSaveItem = { _, _, _, _ -> },
                        onQuantityChange = { _, _ -> },
                        onSearchChange = { query = it },
                        onDeleteItem = {},
                        onErrorConsumed = {},
                        onSaveCompletedConsumed = {},
                    )
                }
            }
        }
    }

    private fun assertStepperTouchTargets() {
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        listOf(PantryTestTags.QUANTITY_DECREMENT, PantryTestTags.QUANTITY_INCREMENT).forEach { tag ->
            val button = composeRule.onNodeWithTag(tag)
            assert(button.fetchSemanticsNode().size.width / density >= 48f)
            assert(button.fetchSemanticsNode().size.height / density >= 48f)
        }
    }
}
