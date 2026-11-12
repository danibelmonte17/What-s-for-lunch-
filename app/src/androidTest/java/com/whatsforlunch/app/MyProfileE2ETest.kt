package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.ui.myprofile.MyProfileTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

// S1-S6 de «Mi perfil» (`crear-mi-perfil`): alta y borrado de chips en modo
// edición (verify 1), tipo de alimentación de selección única con persistencia
// (verify 2), «No tengo ninguna» exclusiva (verify 3), el Pinche con el aviso de IA
// y el mensaje estático de partes pendientes (verify 4 enmendado,
// `rellenar-perfil-con-el-pinche`, RD26 reescrita) y textos es/en (verify 6).
// Sin llamadas a la IA: el verify 4 sustituye el cliente de Koin por uno simulado
// en modo «sin key», sin red.
@RunWith(AndroidJUnit4::class)
class MyProfileE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // verify 1 (S1): «Frutos secos» en Alergias y «Diabetes» en Intolerancias
    // quedan como chips; «Editar» habilita el borrado y borrar la hace desaparecer.
    @Test
    fun chipsAreAddedAndDeletedOnlyInEditMode() {
        showApp(Locale.forLanguageTag("es-ES"))
        openProfileTab()

        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_ALLERGIES).performTextInput("Frutos secos")
        composeRule.onNodeWithTag(MyProfileTestTags.ADD_ALLERGIES).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_INTOLERANCES).performTextInput("Diabetes")
        composeRule.onNodeWithTag(MyProfileTestTags.ADD_INTOLERANCES).performClick()
        dismissKeyboard()

        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.INTOLERANCE, 0))
            .performScrollTo()
            .assertIsDisplayed()
        // Sin modo edición no hay borrado.
        composeRule.onAllNodesWithTag(MyProfileTestTags.removeChip(ProfileSection.ALLERGY, 0))
            .assertCountEquals(0)

        composeRule.onNodeWithTag(MyProfileTestTags.EDIT_ALLERGIES).performScrollTo().performClick()
        // El icono vive dentro de la chip (semántica fusionada en la chip madre).
        composeRule.onNodeWithTag(MyProfileTestTags.removeChip(ProfileSection.ALLERGY, 0), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onAllNodesWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0))
            .assertCountEquals(0)
        // La otra sección no se toca.
        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.INTOLERANCE, 0))
            .performScrollTo()
            .assertIsDisplayed()
        // Se guarda en Room, no solo en la UI.
        val saved = awaitStoredProfile { it.allergies.isEmpty() && it.intolerances == listOf("Diabetes") }
        assertEquals(emptyList<String>(), saved.allergies)
        assertEquals(listOf("Diabetes"), saved.intolerances)
    }

    // verify 2 (S2): «Vegana» y después «Omnívora» deja solo la última marcada y
    // el valor persistido es ese.
    @Test
    fun dietTypeIsSingleSelectionAndPersisted() {
        showApp(Locale.forLanguageTag("es-ES"))
        openProfileTab()

        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.VEGAN)).performScrollTo().performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.VEGAN)).assertIsSelected()
        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.OMNIVORE)).performScrollTo().performClick()

        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.OMNIVORE)).assertIsSelected()
        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.VEGAN)).assertIsNotSelected()
        val saved = awaitStoredProfile { it.dietType == DietType.OMNIVORE }
        assertEquals(DietType.OMNIVORE, saved.dietType)
    }

    // verify 3 (S3/RD25): «No tengo ninguna» limpia las chips y bloquea el alta;
    // desmarcarla devuelve el alta.
    @Test
    fun noneOptionClearsTheSectionAndBlocksAdding() {
        showApp(Locale.forLanguageTag("es-ES"))
        openProfileTab()

        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_ALLERGIES).performTextInput("Frutos secos")
        composeRule.onNodeWithTag(MyProfileTestTags.ADD_ALLERGIES).performClick()
        dismissKeyboard()
        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(MyProfileTestTags.NONE_ALLERGIES).performScrollTo().performClick()

        composeRule.onAllNodesWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0)).assertCountEquals(0)
        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_ALLERGIES).assertIsNotEnabled()
        composeRule.onNodeWithTag(MyProfileTestTags.ADD_ALLERGIES).assertIsNotEnabled()
        val saved = awaitStoredProfile { it.allergiesNone }
        assertTrue(saved.allergiesNone)
        assertEquals(emptyList<String>(), saved.allergies)

        composeRule.onNodeWithTag(MyProfileTestTags.NONE_ALLERGIES).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_ALLERGIES).assertIsEnabled()
        val unmarked = awaitStoredProfile { !it.allergiesNone }
        assertFalse(unmarked.allergiesNone)
    }

    // verify 4 enmendado (`rellenar-perfil-con-el-pinche`, RD26 reescrita): con el
    // perfil vacío el Pinche **sí llama a la IA** —aquí con el cliente de Koin
    // sustituido por uno simulado en modo «sin key», sin red ni llamadas de pago—
    // y, además del aviso de siempre, muestra el mensaje estático de las tres
    // partes pendientes con «Abrir Mi perfil» que salta a la pestaña (S5 a nivel
    // de app completa; el bloqueo previo desapareció).
    @Test
    fun emptyProfileShowsTheAiNoticeAndTheActionOpensItsTab() {
        GlobalContext.get().loadModules(
            listOf(module { single<PincheChatClient> { MissingKeyPincheChatClient() } }),
            allowOverride = true,
        )
        showApp(Locale.forLanguageTag("es-ES"))

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()

        // Aviso de siempre por la key ausente (S5)…
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.NOTICE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Configura tu API key en Ajustes para hablar con el Pinche.")
            .assertIsDisplayed()
        // …y el mensaje estático de las partes pendientes (degradación, D7).
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.OPEN_PROFILE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            "Para responder necesito tu perfil: Alergias, " +
                "Intolerancias o enfermedades que limitan la dieta, Tipo de alimentación. " +
                "Complétalo en «Mi perfil».",
        ).assertIsDisplayed()

        composeRule.onNodeWithTag(PincheTestTags.OPEN_PROFILE).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.SCREEN).assertIsDisplayed()
    }

    // verify 6 (S6): los textos nuevos existen en español e inglés con paridad.
    @Test
    fun screenTextsAreLocalizedInSpanish() {
        showApp(Locale.forLanguageTag("es-ES"))
        openProfileTab()

        // «Mi perfil» y «No tengo ninguna» aparecen más de una vez (pestaña/título
        // y sección); se comprueba que existen y están en pantalla.
        assertAnyTextPresent("Mi perfil")
        assertAnyTextPresent("Alergias")
        assertAnyTextPresent("Intolerancias o enfermedades que limitan la dieta")
        assertAnyTextPresent("Tipo de alimentación")
        assertAnyTextPresent("No tengo ninguna")
        assertAnyTextPresent("Omnívora")
        assertAnyTextPresent("Vegana")
    }

    @Test
    fun screenTextsAreLocalizedInEnglish() {
        showApp(Locale.ENGLISH)
        openProfileTab()

        assertAnyTextPresent("My profile")
        assertAnyTextPresent("Allergies")
        assertAnyTextPresent("Intolerances or medical conditions that limit your diet")
        assertAnyTextPresent("Diet type")
        assertAnyTextPresent("I don't have any")
        assertAnyTextPresent("Ketogenic")
    }

    // verify 6 (S6): lo guardado en la UI sigue en Room (persistencia real) y el
    // estado vuelve a leerse desde el repositorio.
    @Test
    fun profilePersistsInRoomAfterReopeningTheScreen() {
        showApp(Locale.forLanguageTag("es-ES"))
        openProfileTab()

        composeRule.onNodeWithTag(MyProfileTestTags.INPUT_ALLERGIES).performTextInput("Frutos secos")
        composeRule.onNodeWithTag(MyProfileTestTags.ADD_ALLERGIES).performClick()
        dismissKeyboard()
        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.VEGAN))
            .performScrollTo()
            .performClick()

        val saved = awaitStoredProfile { it.dietType == DietType.VEGAN && it.allergies == listOf("Frutos secos") }
        assertEquals(listOf("Frutos secos"), saved.allergies)
        assertEquals(DietType.VEGAN, saved.dietType)

        // Cambiar de pestaña y volver repinta la pantalla desde Room.
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PROFILE).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.chip(ProfileSection.ALLERGY, 0))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MyProfileTestTags.dietOption(DietType.VEGAN))
            .performScrollTo()
            .assertIsSelected()
    }

    private fun openProfileTab() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PROFILE).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.SCREEN).assertIsDisplayed()
    }

    private fun dismissKeyboard() {
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
    }

    /**
     * Espera a que el guardado asíncrono en Room refleje el estado esperado:
     * los guards del ViewModel se lanzan en `viewModelScope` y leer antes puede
     * encontrar el valor anterior.
     */
    private fun awaitStoredProfile(predicate: (UserProfile) -> Boolean): UserProfile {
        var stored: UserProfile? = null
        composeRule.waitUntil(5_000) {
            stored = runBlocking { profileRepository().observeProfile().first() }
            stored.let(predicate) == true
        }
        return requireNotNull(stored)
    }

    private fun assertAnyTextPresent(text: String) {
        assertTrue(
            "Texto ausente en pantalla: $text",
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun profileRepository(): UserProfileRepository =
        GlobalContext.get().get<UserProfileRepository>()

    private fun showApp(locale: Locale) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides localizedContext) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
        composeRule.waitForIdle()
    }

    // Cliente simulado «sin key» para la app completa: produce el mismo aviso que
    // una instalación sin configurar la IA, sin red ni llamadas de pago.
    private class MissingKeyPincheChatClient : PincheChatClient {
        override suspend fun reply(
            turns: List<PincheTurn>,
            language: String,
            profile: UserProfile,
            onContent: (String) -> Unit,
        ): PincheReplyResult = PincheReplyResult.MissingApiKey
    }
}
