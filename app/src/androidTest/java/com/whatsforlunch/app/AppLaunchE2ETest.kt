package com.whatsforlunch.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whatsforlunch.app.ui.misrecetas.MisRecetasTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// verify 1 · RF-1, RF-4, RF-5: la app real abre en Mis Recetas con su estado vacío.
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
    fun appOpensInMisRecetasWithEmptyState() {
        composeRule.onNodeWithTag(MisRecetasTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.EMPTY_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.CREATE_RECIPE).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.SCAN_PANTRY).assertIsDisplayed()
    }
}
