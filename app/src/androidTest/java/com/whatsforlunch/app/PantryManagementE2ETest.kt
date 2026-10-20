package com.whatsforlunch.app

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.R
import com.whatsforlunch.app.ui.misrecetas.MisRecetasTestTags
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PantryManagementE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun opensPantryFromMisRecetasAndShowsEmptyState() {
        composeRule.onNodeWithTag(MisRecetasTestTags.OPEN_PANTRY).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()
    }

    @Test
    fun addEditAndPersistPantryItemAcrossActivityRecreation() {
        openPantry()
        addItem("Tomate fresco")
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_edit_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextClearance()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("Tomate maduro")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_FIELD).performTextInput("3.5")
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, "3.5"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, "3.5")).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertDoesNotExist()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, "3.5")).assertIsDisplayed()
    }

    @Test
    fun normalizedDuplicateShowsInlineWarningWithoutAddingRow() {
        openPantry()
        addItem("Tomate fresco")
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("  TOMATE  fresco ")
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_duplicate_error))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_duplicate_error)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Tomate fresco").assertCountEquals(1)
        composeRule.onNodeWithTag(PantryTestTags.EDIT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun invalidQuantityShowsErrorAndBlankNameCannotBeSaved() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).assertIsNotEnabled()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("Harina")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_FIELD).performTextInput("-1")
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_error)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_error)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).assertCountEquals(0)
    }

    @Test
    fun deleteCanBeCancelledOrConfirmed() {
        openPantry()
        addItem("Tomate fresco")
        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_delete_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_delete_message, "Tomate fresco")).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_CANCEL).performClick()
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_delete_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_CONFIRM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertDoesNotExist()
    }

    private fun openPantry() {
        composeRule.onNodeWithTag(MisRecetasTestTags.OPEN_PANTRY).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
    }

    private fun localized(resourceId: Int, vararg arguments: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *arguments)

    private fun addItem(name: String) {
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput(name)
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
