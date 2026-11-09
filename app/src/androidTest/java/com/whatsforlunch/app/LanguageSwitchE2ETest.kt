package com.whatsforlunch.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.data.ai.PriorityAiKeyProvider
import com.whatsforlunch.app.data.ai.RecipeAvoidance
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.RecipeSuggestionsResult
import com.whatsforlunch.app.data.audio.SystemRingtoneTimerAlarm
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.data.settings.LanguagePreferenceStore
import com.whatsforlunch.app.data.settings.SharedPrefsLanguagePreferenceStore
import com.whatsforlunch.app.data.voice.StepAnnouncement
import com.whatsforlunch.app.data.voice.StepSpeaker
import com.whatsforlunch.app.domain.model.AppLanguage
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsRoute
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsTestTags
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsViewModel
import com.whatsforlunch.app.ui.cookmode.CookModeRoute
import com.whatsforlunch.app.ui.cookmode.CookModeTestTags
import com.whatsforlunch.app.ui.cookmode.CookModeViewModel
import com.whatsforlunch.app.ui.locale.AppLocaleHost
import com.whatsforlunch.app.ui.myrecipes.MyRecipesRoute
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.settings.SettingsRoute
import com.whatsforlunch.app.ui.settings.SettingsTestTags
import com.whatsforlunch.app.ui.settings.SettingsViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S5 de cambiar-idioma-de-la-interfaz: el selector de Ajustes cambia el idioma de
// toda la interfaz sin reiniciar (S1 = verify 1), la elección persiste entre procesos,
// el primer arranque sigue al idioma del sistema y el idioma activo gobierna los
// prompts de IA y el locale de voz (RF-IDI4, D5). Fakes por constructor y fixtures
// locales (InMemory*): sin IA, sin red y con TTS/reconocimiento simulados (D15).
@RunWith(AndroidJUnit4::class)
class LanguageSwitchE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // S1 (verify 1): cambiar a inglés actualiza Ajustes y el resto de pantallas al
    // momento, sin reiniciar la app ni perder el estado de navegación.
    @Test
    fun switchingToEnglishUpdatesAllScreensWithoutRestart() {
        showAppWithNavigation(appController(selected = AppLanguage.SPANISH))

        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Idioma").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_ES)
            .performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN)
            .performScrollTo().assertIsNotSelected()
        // Endónimos (D3): cada idioma se nombra a sí mismo en ambos idiomas.
        composeRule.onNodeWithText("Español").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN).performClick()

        composeRule.onNodeWithText("Language").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN)
            .performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_ES)
            .performScrollTo().assertIsNotSelected()
        composeRule.onNodeWithText("Privacy and data").performScrollTo().assertIsDisplayed()

        // El back stack sobrevive al cambio: Mis Recetas vuelve en inglés.
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("My Recipes")
    }

    // S2: volver a español restaura todos los textos al momento, incluidos los de
    // las pantallas del back stack.
    @Test
    fun switchingBackToSpanishRestoresAllTexts() {
        showAppWithNavigation(appController(selected = AppLanguage.ENGLISH))

        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN)
            .performScrollTo().assertIsSelected()

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_ES).performClick()

        composeRule.onNodeWithText("Idioma").assertIsDisplayed()
        composeRule.onNodeWithText("Privacidad y datos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_ES)
            .performScrollTo().assertIsSelected()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("Mis Recetas")
    }

    // S3: la elección persiste en el dispositivo (D1): un controller nuevo sobre las
    // mismas SharedPreferences —equivalente a reabrir la app tras un force-stop—
    // arranca en inglés aunque el sistema esté en español. El force-stop real se
    // verifica con adb en la comprobación manual (Verification Plan de la spec).
    @Test
    fun selectionSurvivesAppRestart() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        AppLocaleController(SharedPrefsLanguagePreferenceStore(targetContext), SPANISH_SYSTEM)
            .select(AppLanguage.ENGLISH)

        val restarted = AppLocaleController(
            SharedPrefsLanguagePreferenceStore(targetContext),
            SPANISH_SYSTEM,
        )
        assertEquals(AppLanguage.ENGLISH, restarted.language.value)

        showAppWithNavigation(restarted)
        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("My Recipes")
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN)
            .performScrollTo().assertIsSelected()
    }

    // S4: sin elección guardada, el primer arranque preselecciona el idioma del
    // sistema (D2): inglés si el sistema es inglés.
    @Test
    fun firstLaunchFollowsEnglishSystemLanguage() {
        showAppWithNavigation(appController(systemLocale = Locale.US))

        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("My Recipes")
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_EN)
            .performScrollTo().assertIsSelected()
    }

    // S4 (caso límite): cualquier otro idioma del sistema cae a español.
    @Test
    fun firstLaunchFallsBackToSpanishForOtherSystemLanguages() {
        showAppWithNavigation(appController(systemLocale = Locale.FRANCE))

        composeRule.onNodeWithTag(MyRecipesTestTags.TOP_BAR_TITLE).assertTextEquals("Mis Recetas")
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithText("Idioma").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_OPTION_ES)
            .performScrollTo().assertIsSelected()
    }

    // S5 (RF-IDI4): la petición de Sugerencias lleva al cliente de IA el idioma de
    // la app derivado de LocalConfiguration (como AiSuggestionsRoute).
    @Test
    fun aiSuggestionRequestUsesEnglishWhenAppIsInEnglish() {
        assertAiRequestLanguage(AppLanguage.ENGLISH, expected = "en")
    }

    // S5 (RF-IDI4): y al volver a español la petición lleva «es».
    @Test
    fun aiSuggestionRequestUsesSpanishWhenAppIsInSpanish() {
        assertAiRequestLanguage(AppLanguage.SPANISH, expected = "es")
    }

    // S5 (RF-IDI4): el locale de voz del Modo Cocina sigue el idioma de la app y, al
    // volver a español y reactivar la voz, vuelve a es-ES. Los prompts y el
    // vocabulario de comandos no cambian (RD24 intacta, D5): solo cambia la fuente.
    @Test
    fun voiceLocaleFollowsAppLanguage() {
        // CookModeRoute comprueba RECORD_AUDIO antes de activar «Voz» (RD24): se
        // concede con `pm grant` desde el propio test para recorrer la ruta real sin
        // diálogos del sistema ni dependencias nuevas. Los motores siguen siendo fakes.
        grantMicrophonePermission()
        val speaker = CapturingStepSpeaker()
        val controller = appController(selected = AppLanguage.ENGLISH)
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }
        // Fuera del lambda de composición: lint prohíbe construir ViewModels dentro de
        // un composable (patrón de CookModeVoiceE2ETest).
        val viewModel = CookModeViewModel(
            recipes,
            SystemRingtoneTimerAlarm(InstrumentationRegistry.getInstrumentation().targetContext),
            speaker,
            FakeVoiceCommandSource(),
            voiceRecipe().id,
            2,
        )
        composeRule.setContent {
            WhatsForLunchTheme {
                AppLocaleHost(controller) {
                    CookModeRoute(
                        recipeId = voiceRecipe().id,
                        servings = 2,
                        onBack = {},
                        onFinish = {},
                        viewModel = viewModel,
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CookModeTestTags.VOICE_TOGGLE)
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) {
            speaker.preparedSnapshot().lastOrNull() == Locale.ENGLISH
        }

        // Al volver a español, la voz reactivada captura es-ES (una sesión ya iniciada
        // conserva el locale capturado hasta reactivarse: caso límite de la spec).
        controller.select(AppLanguage.SPANISH)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) {
            speaker.preparedSnapshot().lastOrNull()?.language == "es"
        }
    }

    private fun assertAiRequestLanguage(language: AppLanguage, expected: String) {
        val suggester = CapturingRecipeSuggester()
        val viewModel = AiSuggestionsViewModel(
            pantryWith(PantryItem("apple", "Apple", 3)),
            ScanDraftHolder(),
            suggester,
            InMemoryRecipeRepository(),
        )
        composeRule.setContent {
            WhatsForLunchTheme {
                AppLocaleHost(appController(selected = language)) {
                    AiSuggestionsRoute(
                        onBack = {},
                        onOpenSettings = {},
                        viewModel = viewModel,
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.REQUEST)
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()

        composeRule.waitUntil(5_000) { suggester.lastLanguage != null }
        assertEquals(expected, suggester.lastLanguage)
    }

    // App real con navegación mínima (Mis Recetas ↔ Ajustes) bajo el host de idioma:
    // las Routes reales derivan textos, prompts y formatos de LocalConfiguration.
    private fun showAppWithNavigation(controller: AppLocaleController) {
        val myRecipesViewModel =
            MyRecipesViewModel(InMemoryRecipeRepository(), InMemoryPantryRepository())
        val settingsViewModel = settingsViewModel(controller)
        composeRule.setContent {
            WhatsForLunchTheme {
                AppLocaleHost(controller) {
                    var screen by remember { mutableStateOf(AppScreen.MY_RECIPES) }
                    when (screen) {
                        AppScreen.MY_RECIPES -> MyRecipesRoute(
                            onCreateRecipe = {},
                            onScanPantry = {},
                            onEditRecipe = {},
                            onOpenSettings = { screen = AppScreen.SETTINGS },
                            viewModel = myRecipesViewModel,
                        )

                        AppScreen.SETTINGS -> SettingsRoute(
                            onBack = { screen = AppScreen.MY_RECIPES },
                            viewModel = settingsViewModel,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun settingsViewModel(controller: AppLocaleController): SettingsViewModel {
        val keyStore = InMemoryAiKeyStore()
        return SettingsViewModel(
            effectiveKeyProvider = PriorityAiKeyProvider("", keyStore),
            aiKeyStore = keyStore,
            buildKeyActive = false,
            appLocaleController = controller,
        )
    }

    private fun grantMicrophonePermission() {
        val targetPackage = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val command = "pm grant $targetPackage ${Manifest.permission.RECORD_AUDIO}"
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .use { descriptor ->
                // Leer hasta EOF espera a que el comando termine (patrón de la API de
                // UiAutomation); después el permiso ya está concedido.
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        val granted = ContextCompat.checkSelfPermission(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Manifest.permission.RECORD_AUDIO,
        )
        assertTrue(
            "RECORD_AUDIO should be granted before enabling voice",
            granted == PackageManager.PERMISSION_GRANTED,
        )
    }

    private fun appController(
        store: LanguagePreferenceStore = InMemoryLanguagePreferenceStore(),
        selected: AppLanguage? = null,
        systemLocale: Locale = SPANISH_SYSTEM,
    ): AppLocaleController {
        selected?.let { store.select(it) }
        return AppLocaleController(store, systemLocale)
    }

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun voiceRecipe() = Recipe(
        id = VOICE_RECIPE_ID,
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = listOf(
            RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
        ),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", ingredientIds = listOf("potato")),
        ),
    )

    private enum class AppScreen { MY_RECIPES, SETTINGS }

    private class InMemoryAiKeyStore(var storedKey: String? = null) : AiKeyStore {
        override fun apiKey(): String? = storedKey

        override fun save(key: String) {
            storedKey = key
        }

        override fun clear() {
            storedKey = null
        }
    }

    private class InMemoryLanguagePreferenceStore : LanguagePreferenceStore {
        private var selected: AppLanguage? = null

        override fun selected(): AppLanguage? = selected

        override fun select(language: AppLanguage) {
            selected = language
        }
    }

    // Doble del TTS que registra el locale con el que CookModeViewModel prepara la
    // voz (RF-IDI4): el locale de voz derivado es el observable de este escenario.
    private class CapturingStepSpeaker : StepSpeaker {
        private val preparedLocales = mutableListOf<Locale>()

        fun preparedSnapshot(): List<Locale> = synchronized(preparedLocales) {
            preparedLocales.toList()
        }

        override suspend fun prepare(locale: Locale): Boolean {
            synchronized(preparedLocales) { preparedLocales += locale }
            return true
        }

        override fun speak(announcement: StepAnnouncement) = Unit

        override fun stop() = Unit

        override fun release() = Unit
    }

    private class CapturingRecipeSuggester : RecipeSuggester {
        @Volatile
        var lastLanguage: String? = null
            private set

        override suspend fun suggest(
            foods: List<SuggestableFood>,
            language: String,
            existingRecipes: List<RecipeAvoidance>,
        ): RecipeSuggestionsResult {
            lastLanguage = language
            return RecipeSuggestionsResult.Success(emptyList())
        }
    }

    private companion object {
        val SPANISH_SYSTEM: Locale = Locale("es", "ES")
        const val VOICE_RECIPE_ID = "tortilla"
    }
}
