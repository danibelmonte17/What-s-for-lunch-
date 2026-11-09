package com.whatsforlunch.app

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
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
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun opensPantryFromMyRecipesAndShowsEmptyState() {
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).assertIsDisplayed().performClick()
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
        repeat(3) { composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).performClick() }
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 3))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertDoesNotExist()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
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
    fun stepperEnforcesIntegerBoundsAndZeroSavesWithoutQuantity() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).assertIsNotEnabled()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_STEPPER).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_VALUE).assertTextEquals("0")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_DECREMENT).assertIsNotEnabled()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)

        repeat(99) { composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).performClick() }
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_VALUE).assertTextEquals("99")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).assertIsNotEnabled()

        composeRule.onNodeWithTag(PantryTestTags.CANCEL_EDIT).performClick()
        addItem("Harina")
        composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 0)).assertCountEquals(0)
        composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).assertCountEquals(1)
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

    // S4 (mover-escaneo-a-la-despensa, D3): el estado vacío de la Despensa replica
    // los dos FABs — «Escanear alimentos» y «Añadir alimento» — en ese orden.
    @Test
    fun emptyStateShowsScanAndAddCtas() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD_CTA).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_ADD_CTA).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()

        val scanCtaTop = composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD_CTA).getUnclippedBoundsInRoot().top
        val addCtaTop = composeRule.onNodeWithTag(PantryTestTags.EMPTY_ADD_CTA).getUnclippedBoundsInRoot().top
        assertTrue(scanCtaTop < addCtaTop)
    }

    // D1 (verify 3): los dos FABs se apilan con «Escanear alimentos» encima y
    // «Añadir alimento» abajo, junto a la esquina.
    @Test
    fun scanFoodFabIsStackedAboveTheAddFoodFab() {
        openPantry()
        val scanTop = composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).getUnclippedBoundsInRoot().top
        val addTop = composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).getUnclippedBoundsInRoot().top
        assertTrue(scanTop < addTop)
    }

    // S2 (verify 2): el FAB «Escanear alimentos» de la Despensa abre el Escáner.
    @Test
    fun scanFoodFabOpensTheScannerFromThePantry() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    // S2 (verify 2): guardar en el Escáner vuelve a la Despensa con los Alimentos
    // y no deja la Despensa duplicada en el back stack (D5).
    @Test
    fun scannerSaveReturnsToThePantryWithTheSavedFoods() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Tomate fresco")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(ScannerTestTags.SAVE).and(isEnabled()))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        // D5 (adaptado a RF-MOB5 de `mover-opciones-a-bottom-bar`): la Despensa ya no
        // muestra «Volver» (es destino de primer nivel con barra inferior, sesión 117),
        // así que la vuelta a Mis Recetas se hace por su pestaña; al volver a la Despensa
        // la pantalla no está duplicada.
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        openPantry()
        assertEquals(1, composeRule.onAllNodesWithTag(PantryTestTags.SCREEN).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()
    }

    private fun openPantry() {
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).performClick()
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
