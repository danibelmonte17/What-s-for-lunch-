package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.domain.model.PantryItem
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
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        val localizedConfiguration = Configuration(localizedContext.resources.configuration)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalResources provides localizedContext.resources,
            ) {
                WhatsForLunchTheme {
                    PantryScreen(
                        uiState = PantryUiState(items = listOf(PantryItem("test-id", "Tomato", 2.0))),
                        onBack = {},
                        onSaveItem = { _, _, _ -> },
                        onDeleteItem = {},
                        onErrorConsumed = {},
                        onSaveCompletedConsumed = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("Pantry").assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()
        val density = baseContext.resources.displayMetrics.density
        val editAction = composeRule.onNodeWithTag(PantryTestTags.editItem("test-id"))
        val deleteAction = composeRule.onNodeWithTag(PantryTestTags.deleteItem("test-id"))
        editAction.assertContentDescriptionEquals("Edit Tomato")
        deleteAction.assertContentDescriptionEquals("Delete Tomato")
        assert(editAction.fetchSemanticsNode().size.width / density >= 48f)
        assert(editAction.fetchSemanticsNode().size.height / density >= 48f)
        assert(deleteAction.fetchSemanticsNode().size.width / density >= 48f)
        assert(deleteAction.fetchSemanticsNode().size.height / density >= 48f)
        editAction.performClick()
        composeRule.onNodeWithText("Edit food").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Quantity (optional)").assertIsDisplayed()
    }
}
