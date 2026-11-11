package com.whatsforlunch.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.settings.SettingsTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

// Escenarios 1-7 de configurar-acceso-a-ia: registrar, borrar (con el aviso de
// «Generar con IA»), aviso de privacidad es/en, persistencia y prioridad de la key
// inyectada desde local.properties. Los casos que exigen que el almacén sea la única
// fuente se omiten explícitamente si el build lleva AI_API_KEY.
@RunWith(AndroidJUnit4::class)
class SettingsE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var appContext: Context

    private val buildKeyActive: Boolean
        get() = BuildConfig.AI_API_KEY.isNotBlank()

    @Before
    fun clearStoredKey() {
        GlobalContext.get().get<AiKeyStore>().clear()
    }

    private fun showApp(locale: Locale) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        appContext = baseContext.createConfigurationContext(configuration)
        val resources = appContext.resources
        composeRule.setContent {
            // Se localizan los textos con LocalConfiguration/LocalResources en lugar de
            // reemplazar LocalContext: CreateRecipeRoute necesita el Activity real para
            // montar el PhotoAcquirer del sistema (escenario 5).
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalResources provides resources,
            ) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
    }

    private fun localized(resourceId: Int): String = appContext.getString(resourceId)

    private fun openSettings() {
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.SCREEN).assertIsDisplayed()
    }

    private fun assertExpectedStatus() {
        val expected = if (buildKeyActive) {
            R.string.settings_api_key_state_configured
        } else {
            R.string.settings_api_key_state_missing
        }
        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(expected))
    }

    private fun saveKey(key: String = "sk-test-settings-e2e") {
        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_missing))
        composeRule.onNodeWithTag(SettingsTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(SettingsTestTags.KEY_FIELD).performTextInput(key)
        composeRule.onNodeWithTag(SettingsTestTags.SAVE).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_configured))
    }

    private fun assertFieldIsEmpty() {
        val editableText = composeRule.onNodeWithTag(SettingsTestTags.KEY_FIELD)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertEquals("", editableText)
    }

    @Test
    fun settingsOpensFromMyRecipesAndShowsPrivacyNotice() {
        showApp(Locale.forLanguageTag("es-ES"))
        openSettings()

        assertExpectedStatus()
        composeRule.onNodeWithTag(SettingsTestTags.KEY_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(SettingsTestTags.PRIVACY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.PRIVACY_NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.settings_privacy_notice)).assertIsDisplayed()
    }

    @Test
    fun savedKeyMarksConfiguredAndNeverRevealsTheKeyWhenReturning() {
        assumeFalse(buildKeyActive)
        showApp(Locale.forLanguageTag("es-ES"))
        openSettings()
        saveKey()
        assertFieldIsEmpty()

        composeRule.onNodeWithText(localized(R.string.action_back)).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        openSettings()

        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_configured))
        assertFieldIsEmpty()
        composeRule.onNodeWithTag(SettingsTestTags.DELETE).assertIsDisplayed()
    }

    @Test
    fun deletingTheKeyAsksToConfigureItAgainForAiFeatures() {
        assumeFalse(buildKeyActive)
        showApp(Locale.forLanguageTag("es-ES"))
        openSettings()
        saveKey()

        composeRule.onNodeWithTag(SettingsTestTags.DELETE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.DELETE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.DELETE_CANCEL).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_configured))

        composeRule.onNodeWithTag(SettingsTestTags.DELETE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.DELETE_CONFIRM).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_missing))

        composeRule.onNodeWithText(localized(R.string.action_back)).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(CreateRecipeTestTags.NOTICE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(
                localized(R.string.create_recipe_notice_ai_key_missing),
                substring = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            localized(R.string.create_recipe_notice_ai_key_missing),
            substring = true,
        ).assertExists()
    }

    @Test
    fun settingsAndPrivacyNoticeAreLocalizedInEnglish() {
        showApp(Locale.ENGLISH)
        openSettings()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.STATUS).assertTextEquals(
            if (buildKeyActive) "Configured" else "Not configured",
        )
        composeRule.onNodeWithText("DevExpert Inference API key").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.settings_privacy_notice)).assertIsDisplayed()
    }

    @Test
    fun buildProvidedKeyShowsConfiguredNoteAndHidesDelete() {
        assumeTrue(buildKeyActive)
        showApp(Locale.forLanguageTag("es-ES"))
        openSettings()

        composeRule.onNodeWithTag(SettingsTestTags.STATUS)
            .assertTextEquals(localized(R.string.settings_api_key_state_configured))
        composeRule.onNodeWithTag(SettingsTestTags.BUILD_SOURCE).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.settings_api_key_build_source))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.DELETE).assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.SAVE).assertIsNotEnabled()
    }
}
