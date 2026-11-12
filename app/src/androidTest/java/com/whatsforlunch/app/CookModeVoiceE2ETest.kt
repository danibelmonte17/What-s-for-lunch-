package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.audio.SystemRingtoneTimerAlarm
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.voice.VoiceSourceEvent
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.ui.cookmode.CookModeEvent
import com.whatsforlunch.app.ui.cookmode.CookModeScreen
import com.whatsforlunch.app.ui.cookmode.CookModeTestTags
import com.whatsforlunch.app.ui.cookmode.CookModeViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S9 de controlar-cocina-con-voz (RD24): voz opcional con comandos exactos,
// activación con interruptor, avisos de degradación e idioma de la app. Los
// motores son fakes (D15): el reconocimiento se simula emitiendo transcripciones
// y la precisión real se mide en T0 y en la comprobación manual, nunca aquí.
@RunWith(AndroidJUnit4::class)
class CookModeVoiceE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var speaker: FakeStepSpeaker
    private lateinit var source: FakeVoiceCommandSource
    private lateinit var viewModel: CookModeViewModel

    private val eventsScope = CoroutineScope(Dispatchers.Main + Job())
    private val finishCount = AtomicInteger(0)

    @After
    fun cancelEventsCollection() {
        eventsScope.coroutineContext[Job]?.cancel()
    }

    // S2: activar «Voz» cambia el chip a «Voz activa», muestra la barra de
    // comandos y locuta el Paso activo con su temporizador (D6).
    @Test
    fun enablingVoiceShowsActiveChipCommandsBarAndAnnouncesTheStep() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.VOICE_CUE).assertCountEquals(0)

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()

        composeRule.onNodeWithText("Voz activa").assertIsDisplayed()
        composeRule.onNodeWithText("Comandos de voz activos").assertIsDisplayed()
        composeRule.onNodeWithText("Di «Siguiente», «Repetir» o «Pausar tiempo»")
            .assertIsDisplayed()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }
        val announced = speaker.announcedSnapshot()
        assertEquals(1, announced[0].stepNumber)
        assertEquals("Corta las patatas", announced[0].text)
        assertEquals(5, announced[0].timerMinutes)
    }

    // S1: la transcripción «Siguiente» avanza al Paso 2 y locuta el nuevo Paso.
    @Test
    fun nextTranscriptAdvancesTheStepAndAnnouncesIt() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }

        source.emit(VoiceSourceEvent.Heard("Siguiente"))

        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 2 }
        val announced = speaker.announcedSnapshot()
        assertEquals(2, announced[1].stepNumber)
        assertEquals("Cuece la tortilla", announced[1].text)
    }

    // S3: «Repetir» relocuta el Paso activo sin cambiar de Paso; una frase que no
    // es comando exacto se ignora (D7).
    @Test
    fun repeatTranscriptReannouncesWithoutAdvancingAndPhrasesAreIgnored() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }

        source.emit(VoiceSourceEvent.Heard("Siguiente, por favor"))
        source.emit(VoiceSourceEvent.Heard("Repetir"))

        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 2 }
        assertEquals(1, speaker.announcedSnapshot()[1].stepNumber)
    }

    // S4: «Pausar tiempo» pausa la cuenta en marcha y no la reanuda al repetirse.
    @Test
    fun pauseTimerTranscriptPausesTheCountdownAndNeverResumes() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()
        composeRule.onNodeWithText("En marcha").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }

        source.emit(VoiceSourceEvent.Heard("Pausar tiempo"))
        composeRule.onNodeWithText("Pausado").assertIsDisplayed()

        // S3 (D2): repetir el comando no reanuda; la cuenta sigue pausada.
        source.emit(VoiceSourceEvent.Heard("Pausar tiempo"))
        composeRule.onNodeWithText("Pausado").assertIsDisplayed()
        composeRule.onAllNodesWithText("En marcha").assertCountEquals(0)
    }

    // S5: «Siguiente» en el último Paso termina como «Terminar» (D3): se emite el
    // evento de fin y la voz vuelve a apagarse.
    @Test
    fun nextTranscriptOnTheLastStepFinishesCookingMode() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        source.emit(VoiceSourceEvent.Heard("Siguiente"))

        composeRule.waitUntil(5_000) { finishCount.get() == 1 }
        composeRule.onNodeWithText("Voz").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.VOICE_CUE).assertCountEquals(0)
    }

    // S6: «Escuchar» locuta el Paso con la voz apagada y sin iniciar la escucha.
    @Test
    fun listenButtonSpeaksWithVoiceOffWithoutStartingTheRecognizer() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_LISTEN).performClick()

        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }
        assertEquals(0, source.startCount)
        composeRule.onAllNodesWithTag(CookModeTestTags.VOICE_CUE).assertCountEquals(0)
    }

    // S7: permiso denegado muestra el aviso, la voz sigue apagada y «Escuchar» y
    // los botones siguen operativos; al reintentar con permiso concedido (lo que
    // la Route hace al volver el resultado del diálogo), la voz se activa (D4).
    @Test
    fun deniedMicrophoneShowsNoticeKeepsButtonsUsableAndRetrySucceeds() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }
        var microphoneGranted = false

        launchVoiceCookMode(recipes) {
            if (microphoneGranted) {
                viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            } else {
                viewModel.onVoicePermissionDenied()
            }
        }

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Sin permiso de micrófono. Toca «Voz» para volver a intentarlo.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Voz activa").assertCountEquals(0)
        // «Escuchar» y los botones siguen operativos sin permiso (D4/S6).
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_LISTEN).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        // Reintento: el permiso ya está concedido y «Voz» activa la voz.
        microphoneGranted = true
        composeRule.onNodeWithTag(CookModeTestTags.PREVIOUS_STEP).performClick()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()

        composeRule.onNodeWithText("Voz activa").assertIsDisplayed()
        composeRule.onNodeWithText("Comandos de voz activos").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.VOICE_NOTICE).assertCountEquals(0)
    }

    // S8: en inglés el comando exacto es «next»; «siguiente» se ignora y los
    // textos visibles son los ingleses (D5).
    @Test
    fun englishAppUsesEnglishCommandsAndVisibleTexts() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes, locale = Locale.ENGLISH)

        composeRule.onNodeWithText("Voice").assertIsDisplayed()
        composeRule.onNodeWithText("Listen").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()
        composeRule.onNodeWithText("Voice on").assertIsDisplayed()
        composeRule.onNodeWithText("Voice commands active").assertIsDisplayed()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }

        // El español se ignora con la app en inglés (S8/D5).
        source.emit(VoiceSourceEvent.Heard("Siguiente"))
        composeRule.onNodeWithText("Step 1 of 2").assertIsDisplayed()

        source.emit(VoiceSourceEvent.Heard("next"))
        composeRule.onNodeWithText("Step 2 of 2").assertIsDisplayed()
    }

    // S9a: sin motor TTS la activación muestra el aviso, no hay crash y los
    // botones siguen funcionando (D4).
    @Test
    fun missingTextToSpeechShowsNoticeAndKeepsTheModeUsable() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        speaker.available = false

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()

        composeRule.onNodeWithText("La voz no está disponible en este dispositivo.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Voz activa").assertCountEquals(0)
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()
    }

    // S9b: sin servicio de reconocimiento la activación muestra su aviso y
    // «Escuchar» sigue locutando (D4).
    @Test
    fun missingRecognitionServiceShowsNoticeAndListenStillWorks() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(voiceRecipe()) }

        launchVoiceCookMode(recipes)
        source.available = false

        composeRule.onNodeWithTag(CookModeTestTags.VOICE_TOGGLE).performClick()

        composeRule.onNodeWithText(
            "No se pudo iniciar el reconocimiento de voz. " +
                "Puedes seguir con «Escuchar» y los botones.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Voz activa").assertCountEquals(0)
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_LISTEN).assertIsEnabled()
        composeRule.onNodeWithTag(CookModeTestTags.VOICE_LISTEN).performClick()
        composeRule.waitUntil(5_000) { speaker.announcedSnapshot().size == 1 }
    }

    // Compone el Modo Cocina con sus fakes de voz (D15). El toggle por defecto
    // simula la concesión del permiso; los tests de permiso pasan su propio
    // handler para simular la denegación y el reintento (S7).
    private fun launchVoiceCookMode(
        recipes: InMemoryRecipeRepository,
        locale: Locale = Locale.forLanguageTag("es-ES"),
        onVoiceToggle: (() -> Unit)? = null,
    ) {
        val context = contextFor(locale)
        speaker = FakeStepSpeaker()
        source = FakeVoiceCommandSource()
        // Fuera del lambda de composición: lint prohíbe construir ViewModels dentro
        // de un composable (patrón de CookModeE2ETest).
        viewModel = CookModeViewModel(
            recipes,
            SystemRingtoneTimerAlarm(context),
            speaker,
            source,
            "tortilla",
            servings = null,
        )
        finishCount.set(0)
        eventsScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    CookModeEvent.Finish -> finishCount.incrementAndGet()
                }
            }
        }
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    CookModeScreen(
                        uiState = state,
                        onBack = {},
                        onNext = viewModel::next,
                        onPrevious = viewModel::previous,
                        onFinish = { finishCount.incrementAndGet() },
                        onTimerStart = viewModel::onTimerStart,
                        onTimerPause = viewModel::onTimerPause,
                        onTimerResume = viewModel::onTimerResume,
                        onTimerReset = viewModel::onTimerReset,
                        onTimerAddMinute = viewModel::onTimerAddMinute,
                        onTimerStop = viewModel::onTimerStop,
                        onVoiceToggle = onVoiceToggle
                            ?: { viewModel.onVoiceEnabled(locale) },
                        onSpeakStep = { viewModel.onSpeakStep(locale) },
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CookModeTestTags.SCREEN)
                .fetchSemanticsNodes().size == 1
        }
    }

    private fun contextFor(locale: Locale) =
        InstrumentationRegistry.getInstrumentation().targetContext
            .let { baseContext ->
                baseContext.createConfigurationContext(
                    Configuration(baseContext.resources.configuration).apply {
                        setLocale(locale)
                    },
                )
            }

    // Receta de 2 Pasos con temporizador solo en el Paso 1 (S4) e Ingrediente
    // vinculado para no saltar el drawer.
    private fun voiceRecipe() = Recipe(
        id = "tortilla",
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = emptyList(),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", timerMinutes = 5),
            RecipeStep(id = "s2", text = "Cuece la tortilla"),
        ),
    )
}
