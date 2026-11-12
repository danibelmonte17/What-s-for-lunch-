package com.whatsforlunch.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// verify 1 · RF-1, RF-4, RF-5: la app real abre en Mi recetario con su estado vacío.
// Las aserciones usan testTags para ser independientes del idioma del dispositivo.
@RunWith(AndroidJUnit4::class)
class AppLaunchE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun appOpensInMyRecipesWithEmptyState() {
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB).assertCountEquals(0)
    }
}
