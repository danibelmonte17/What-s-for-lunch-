package com.whatsforlunch.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whatsforlunch.app.data.ai.OpenAiCompatiblePincheChatClient
import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.domain.model.AppLanguage
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.ui.myprofile.MyProfileTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

// S1-S3 y D6 de `mantener-y-limpiar-conversacion-del-pinche` sobre la app real
// (MainActivity + navegación por pestañas): la conversación sobrevive a los
// cambios de pestaña (verify 1), el botón de la top bar limpia y devuelve la
// pantalla al estado inicial para conversar de nuevo (verify 2), su
// content-description está en español e inglés (verify 3) y la limpieza no toca
// «Mi perfil» (D6). El cliente de IA se sustituye por uno simulado inyectado en
// Koin: sin red ni llamadas de pago.
@RunWith(AndroidJUnit4::class)
class PincheConversationE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val fakeClient = ReplyingPincheChatClient()
    private var originalLanguage: AppLanguage = AppLanguage.SPANISH

    @Before
    fun setUp() {
        E2eDatabase.clear()
        originalLanguage = appLocaleController().language.value
        GlobalContext.get().loadModules(
            listOf(module { single<PincheChatClient> { fakeClient } }),
            allowOverride = true,
        )
        // Determinismo: los textos de este test se afirman en español (el idioma
        // original se restaura en @After porque `select` persiste la elección).
        appLocaleController().select(AppLanguage.SPANISH)
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        appLocaleController().select(originalLanguage)
        // El binding por defecto vuelve a ser el real para el resto de la batería.
        GlobalContext.get().loadModules(
            listOf(module {
                single<PincheChatClient> {
                    OpenAiCompatiblePincheChatClient(get(), get(), get(), get(), get(), get(), BuildConfig.AI_BASE_URL)
                }
            }),
            allowOverride = true,
        )
    }

    // verify 1: cambiar de pestaña destruye la entrada `pinche` del back stack;
    // con el ViewModel anclado a la Activity la conversación debe seguir intacta.
    @Test
    fun conversationSurvivesSwitchingTabsAndComingBack() {
        openPincheTab()
        sendAndWaitForAssistant("¿Cómo evito que el arroz se pegue?", assistantIndex = 1)

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        composeRule.onNodeWithTag(PincheTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(PincheTestTags.userMessage(0)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.assistantMessage(1)).performScrollTo().assertIsDisplayed()

        // Y se puede continuar la conversación desde donde estaba.
        sendAndWaitForAssistant("¿Y si añado un poco de caldo?", assistantIndex = 3)
    }

    // verify 2: el botón limpia sin confirmación, la pantalla vuelve al estado
    // inicial (saludo + chips, sin mensajes ni botón —D7—) y se puede conversar
    // de nuevo.
    @Test
    fun clearButtonResetsToTheInitialScreenAndTheChatWorksAgain() {
        openPincheTab()
        sendAndWaitForAssistant("¿Cómo evito que el arroz se pegue?", assistantIndex = 1)

        composeRule.onNodeWithTag(PincheTestTags.CLEAR_CONVERSATION).performClick()

        composeRule.onNodeWithText("Hola, soy el Pinche. Pregúntame lo que quieras de cocina.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.QUICK_SUGGESTION).assertIsDisplayed()
        composeRule.onAllNodesWithTag(PincheTestTags.userMessage(0)).assertCountEquals(0)
        composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1)).assertCountEquals(0)
        composeRule.onAllNodesWithTag(PincheTestTags.CLEAR_CONVERSATION).assertCountEquals(0)

        // Las chips de sugerencia vuelven a estar operativas (RF-PCH2).
        composeRule.onNodeWithTag(PincheTestTags.quickSuggestion(0)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.userMessage(0)).performScrollTo().assertIsDisplayed()
    }

    // verify 3 (es): sin conversación no hay botón (D7); con ella, su
    // content-description es la clave es.
    @Test
    fun clearButtonIsHiddenWhenEmptyAndShowsItsSpanishDescription() {
        openPincheTab()

        composeRule.onAllNodesWithTag(PincheTestTags.CLEAR_CONVERSATION).assertCountEquals(0)

        sendAndWaitForAssistant("¿Cómo evito que el arroz se pegue?", assistantIndex = 1)

        composeRule.onNodeWithContentDescription("Limpiar conversación").assertIsDisplayed()
    }

    // verify 3 (en): el mismo botón con su content-description en inglés.
    @Test
    fun clearButtonShowsItsEnglishDescription() {
        appLocaleController().select(AppLanguage.ENGLISH)
        composeRule.waitForIdle()
        openPincheTab()

        sendAndWaitForAssistant("How do I keep the rice from sticking?", assistantIndex = 1)

        composeRule.onNodeWithContentDescription("Clear conversation").assertIsDisplayed()
    }

    // D6: la limpieza no toca «Mi perfil» —lo guardado se conserva, la siguiente
    // conversación sigue llevándolo en el puerto y la chip sigue en su pantalla.
    @Test
    fun clearingKeepsTheProfileAndTheNextChatCarriesIt() {
        runBlocking {
            GlobalContext.get().get<UserProfileRepository>()
                .save(UserProfile(allergies = listOf("Frutos secos"), intolerancesNone = true))
        }
        openPincheTab()

        sendAndWaitForAssistant("Dame una receta", assistantIndex = 1)
        assertEquals(listOf("Frutos secos"), fakeClient.lastProfile().allergies)

        composeRule.onNodeWithTag(PincheTestTags.CLEAR_CONVERSATION).performClick()
        composeRule.onAllNodesWithTag(PincheTestTags.userMessage(0)).assertCountEquals(0)

        sendAndWaitForAssistant("Ahora otra distinta", assistantIndex = 1)
        val profileAfterClear = fakeClient.lastProfile()
        assertEquals(listOf("Frutos secos"), profileAfterClear.allergies)
        assertNull(profileAfterClear.dietType)

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PROFILE).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun openPincheTab() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        composeRule.onNodeWithTag(PincheTestTags.SCREEN).assertIsDisplayed()
    }

    private fun sendAndWaitForAssistant(text: String, assistantIndex: Int) {
        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput(text)
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(assistantIndex))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun dismissKeyboard() {
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
    }

    private fun appLocaleController(): AppLocaleController = GlobalContext.get().get<AppLocaleController>()

    // Cliente simulado: responde siempre con texto, sin Propuesta y sin red.
    private class ReplyingPincheChatClient : PincheChatClient {
        private val profiles = mutableListOf<UserProfile>()

        fun lastProfile(): UserProfile = profiles.last()

        override suspend fun reply(
            turns: List<PincheTurn>,
            language: String,
            profile: UserProfile,
            onContent: (String) -> Unit,
        ): PincheReplyResult {
            profiles += profile
            return PincheReplyResult.Success(PincheReply(text = "Respuesta simulada del Pinche."))
        }
    }
}
