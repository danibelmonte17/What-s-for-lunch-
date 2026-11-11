package com.whatsforlunch.app.ui.cookmode

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.audio.TimerAlarm
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.voice.StepAnnouncement
import com.whatsforlunch.app.data.voice.StepSpeaker
import com.whatsforlunch.app.data.voice.VoiceCommandSource
import com.whatsforlunch.app.data.voice.VoiceSourceEvent
import com.whatsforlunch.app.domain.cookmode.StepTimerPhase
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-CPP3 y RF-CPP4 (RD22): entrada en el Paso 1, navegación con límites, Raciones
// heredadas y estados NotFound. RF-GTP1 a RF-GTP5 (RD23): máquina del temporizador
// con tick de tiempo virtual, alarma con doble y descarte al cambiar de Paso/salir.
@OptIn(ExperimentalCoroutinesApi::class)
class CookModeViewModelTest {

    @Test
    fun cookingStartsAtFirstStepWithRowsScaledToTheInheritedServings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = 2,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(0, state.stepIndex)
            assertEquals(2, state.totalSteps)
            assertEquals(350.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun withoutServingsArgumentTheRowsUseBaseServings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(700.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun inheritedServingsOutOfRangeAreClamped() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            // 13 se recorta a 12: 700 × (12 / 4) = 2100.
            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = 13,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            val state = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(2100.0, state.ingredientRows.single { it.id == "potato" }.quantity, 0.0001)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun nextAndPreviousStayWithinTheStepRange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = 2,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.previous()
            runCurrent()
            assertEquals(0, (viewModel.uiState.value as CookModeUiState.Cooking).stepIndex)

            viewModel.next()
            runCurrent()
            val last = viewModel.uiState.value as CookModeUiState.Cooking
            // Último Paso: el índice se mantiene y el botón primario pasa a «Terminar».
            assertEquals(1, last.stepIndex)
            assertEquals(1, last.totalSteps - 1)
            assertTrue(last.ingredientRows.isEmpty())

            viewModel.next()
            runCurrent()
            assertEquals(1, (viewModel.uiState.value as CookModeUiState.Cooking).stepIndex)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unknownRecipeShowsNotFound() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val viewModel = CookModeViewModel(
                InMemoryRecipeRepository(),
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "missing",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertEquals(CookModeUiState.NotFound, viewModel.uiState.value)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun recipeWithoutStepsShowsNotFoundBecauseCookingModeDoesNotExistForIt() = runTest {
        // D3: la CTA del Detalle está deshabilitada con 0 Pasos; este estado solo
        // cubre la navegación directa a la ruta del Modo Cocina.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(Recipe(id = "draft", title = "Borrador vacío"))

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "draft",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertEquals(CookModeUiState.NotFound, viewModel.uiState.value)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    // --- Temporizador del Paso (RF-GTP1 a RF-GTP5, RD23) ---

    @Test
    fun timerStartsReadyWithItsMetaAndNeverStartsAlone() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D3: al entrar el temporizador está «Listo» con su meta y nunca arranca solo.
            val ready = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.READY, ready.phase)
            assertEquals(300, ready.remainingSeconds)
            assertEquals(300, ready.totalSeconds)

            advanceTimeBy(5_000)
            runCurrent()
            val stillReady = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.READY, stillReady.phase)
            assertEquals(300, stillReady.remainingSeconds)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startingTheTimerDecrementsOneSecondPerTick() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onTimerStart()
            runCurrent()
            assertEquals(StepTimerPhase.RUNNING, (viewModel.uiState.value as CookModeUiState.Cooking).timer!!.phase)

            // 2,5 s virtuales = 2 ticks completos (1000 y 2000 ms).
            advanceTimeBy(2_500)
            runCurrent()
            val running = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.RUNNING, running.phase)
            assertEquals(298, running.remainingSeconds)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun pausingFreezesTheCountdownAndResumingContinues() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onTimerStart()
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()

            viewModel.onTimerPause()
            runCurrent()
            val paused = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.PAUSED, paused.phase)
            assertEquals(298, paused.remainingSeconds)

            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(298, (viewModel.uiState.value as CookModeUiState.Cooking).timer!!.remainingSeconds)

            viewModel.onTimerResume()
            runCurrent()
            advanceTimeBy(1_500)
            runCurrent()
            val resumed = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.RUNNING, resumed.phase)
            assertEquals(297, resumed.remainingSeconds)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun addMinuteExtendsMetaAndCountdownWithoutInterrupting() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onTimerStart()
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()

            viewModel.onTimerAddMinute()
            runCurrent()
            val extended = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.RUNNING, extended.phase)
            assertEquals(360, extended.totalSeconds)
            assertEquals(358, extended.remainingSeconds)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun resetRestoresTheCurrentMetaAndLeavesItRunning() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            viewModel.onTimerStart()
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()
            viewModel.onTimerAddMinute()
            runCurrent()

            // «Reiniciar» usa la meta vigente (5 min + 1 min = 6 min), no la original.
            viewModel.onTimerReset()
            runCurrent()
            val restarted = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.RUNNING, restarted.phase)
            assertEquals(360, restarted.remainingSeconds)
            assertEquals(360, restarted.totalSeconds)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reachingZeroStartsTheAlarmAndAddMinuteSilencesItAndExtends() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val alarm = FakeTimerAlarm()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            goToStep(viewModel, targetStepIndex = 2)

            // S1: meta 0 min → la alarma suena al primer tick (verify 1).
            viewModel.onTimerStart()
            runCurrent()
            advanceUntilIdle()
            val finished = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.FINISHED, finished.phase)
            assertEquals(0, finished.remainingSeconds)
            assertTrue(alarm.isRinging)
            assertEquals(1, alarm.startCount)

            // S2: «+1 min» silencia la alarma, suma 60 s y reanuda la cuenta (verify 2).
            viewModel.onTimerAddMinute()
            runCurrent()
            val extended = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.RUNNING, extended.phase)
            assertEquals(60, extended.remainingSeconds)
            assertEquals(60, extended.totalSeconds)
            assertFalse(alarm.isRinging)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stopAlarmReturnsToReadyWithTheCurrentMeta() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val alarm = FakeTimerAlarm()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            goToStep(viewModel, targetStepIndex = 2)
            viewModel.onTimerStart()
            runCurrent()
            advanceUntilIdle()
            assertTrue(alarm.isRinging)

            viewModel.onTimerStop()
            runCurrent()
            val stopped = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.READY, stopped.phase)
            assertEquals(0, stopped.remainingSeconds)
            assertFalse(alarm.isRinging)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun changingStepDiscardsTheTimerAndSilencesTheAlarm() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val alarm = FakeTimerAlarm()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            goToStep(viewModel, targetStepIndex = 2)
            viewModel.onTimerStart()
            runCurrent()
            advanceUntilIdle()
            assertTrue(alarm.isRinging)

            // S6: cambiar de Paso descarta la cuenta y silencia la alarma; cada Paso
            // entra «Listo» con su propia meta.
            viewModel.previous()
            runCurrent()
            val secondStep = (viewModel.uiState.value as CookModeUiState.Cooking)
            assertEquals(1, secondStep.stepIndex)
            assertEquals(StepTimerPhase.READY, secondStep.timer!!.phase)
            assertEquals(120, secondStep.timer.remainingSeconds)
            assertFalse(alarm.isRinging)

            viewModel.previous()
            runCurrent()
            val firstStep = (viewModel.uiState.value as CookModeUiState.Cooking)
            assertEquals(StepTimerPhase.READY, firstStep.timer!!.phase)
            assertEquals(300, firstStep.timer.remainingSeconds)

            // Volver al Paso 2 también entra «Listo» (sin cuenta ni alarma heredadas).
            val startsBeforeReentering = alarm.startCount
            viewModel.next()
            runCurrent()
            val backToSecond = (viewModel.uiState.value as CookModeUiState.Cooking)
            assertEquals(StepTimerPhase.READY, backToSecond.timer!!.phase)
            assertEquals(120, backToSecond.timer.remainingSeconds)
            assertEquals(startsBeforeReentering, alarm.startCount)
            assertFalse(alarm.isRinging)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun clearingTheViewModelSilencesTheAlarmAndDiscardsTheTimer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val alarm = FakeTimerAlarm()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            goToStep(viewModel, targetStepIndex = 2)
            viewModel.onTimerStart()
            runCurrent()
            advanceUntilIdle()
            assertTrue(alarm.isRinging)

            // RF-GTP5 (D1): salir del Modo Cocina (clear del ViewModelStore) descarta
            // el temporizador y silencia la alarma.
            viewModelStore.clear()
            runCurrent()
            assertFalse(alarm.isRinging)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stepWithoutTimerMinutesHasNoTimer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val alarm = FakeTimerAlarm()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // RF-GTP6: un Paso sin temporizador no muestra widget y sus eventos no hacen nada.
            assertNull((viewModel.uiState.value as CookModeUiState.Cooking).timer)
            viewModel.onTimerStart()
            viewModel.onTimerAddMinute()
            runCurrent()
            advanceUntilIdle()
            assertNull((viewModel.uiState.value as CookModeUiState.Cooking).timer)
            assertEquals(0, alarm.startCount)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    // --- Voz del Modo Cocina (RD24, S1 a S9) ---

    @Test
    fun voiceIsOffByDefaultWithNoNotice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D1: el interruptor nace apagado y sin aviso; nada arranca solo.
            val voice = (viewModel.uiState.value as CookModeUiState.Cooking).voice
            assertFalse(voice.enabled)
            assertNull(voice.notice)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun enablingVoicePreparesTtsStartsRecognitionAndAnnouncesTheStep() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D1/D8: activar prepara el TTS con el idioma de la app, arranca la
            // escucha y locuta el Paso activo.
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            val voice = (viewModel.uiState.value as CookModeUiState.Cooking).voice
            assertTrue(voice.enabled)
            assertNull(voice.notice)
            assertEquals(listOf(Locale.forLanguageTag("es-ES")), speaker.preparedFor)
            assertEquals(1, source.startCount)
            assertEquals(1, speaker.announced.size)
            assertEquals(1, speaker.announced[0].stepNumber)
            assertEquals("Corta las patatas", speaker.announced[0].text)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun voiceUnavailableWhenTtsCannotPrepareShowsNoticeAndStaysOff() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker(prepared = false)
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D4: sin motor TTS la voz queda apagada con aviso y no se escucha.
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            val voice = (viewModel.uiState.value as CookModeUiState.Cooking).voice
            assertFalse(voice.enabled)
            assertEquals(VoiceNotice.VOICE_UNAVAILABLE, voice.notice)
            assertEquals(0, source.startCount)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun commandsUnavailableWhenRecognitionCannotStartShowsNotice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val source = FakeVoiceCommandSource(available = false)

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D4: sin servicio de reconocimiento la voz queda apagada con aviso.
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            val voice = (viewModel.uiState.value as CookModeUiState.Cooking).voice
            assertFalse(voice.enabled)
            assertEquals(VoiceNotice.COMMANDS_UNAVAILABLE, voice.notice)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun permissionDeniedShowsNoticeAndVoiceStaysOff() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D4: el permiso denegado deja aviso visible y la voz apagada; el
            // Modo Cocina sigue usable con botones.
            viewModel.onVoicePermissionDenied()
            runCurrent()

            val cooking = viewModel.uiState.value as CookModeUiState.Cooking
            assertFalse(cooking.voice.enabled)
            assertEquals(VoiceNotice.PERMISSION_DENIED, cooking.voice.notice)
            viewModel.next()
            runCurrent()
            assertEquals(1, (viewModel.uiState.value as CookModeUiState.Cooking).stepIndex)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun nextCommandAdvancesTheStepAndAnnouncesIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            // D7: «Siguiente» exacto avanza y locuta el nuevo Paso.
            source.emit(VoiceSourceEvent.Heard("Siguiente"))
            runCurrent()

            val cooking = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(1, cooking.stepIndex)
            assertTrue(cooking.voice.enabled)
            assertEquals(2, speaker.announced.size)
            assertEquals(2, speaker.announced[1].stepNumber)
            assertEquals("Cuece la tortilla", speaker.announced[1].text)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unknownTranscriptIsIgnoredAndKeepsListening() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            // D7: frase que no es comando ni en el idioma activo: nada cambia.
            source.emit(VoiceSourceEvent.Heard("Dale a siguiente"))
            runCurrent()
            source.emit(VoiceSourceEvent.Heard("Next"))
            runCurrent()

            val cooking = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(0, cooking.stepIndex)
            assertTrue(cooking.voice.enabled)
            // Solo la locución inicial del Paso 1.
            assertEquals(1, speaker.announced.size)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repeatCommandAnnouncesTheCurrentStepAgain() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            // D6: «Repetir» relocuta el mismo Paso sin avanzar.
            source.emit(VoiceSourceEvent.Heard("Repetir"))
            runCurrent()

            val cooking = viewModel.uiState.value as CookModeUiState.Cooking
            assertEquals(0, cooking.stepIndex)
            assertEquals(2, speaker.announced.size)
            assertEquals(1, speaker.announced[1].stepNumber)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun pauseTimerCommandPausesTheCountdownAndNeverResumes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onTimerStart()
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            // S3: «Pausar tiempo» pausa la cuenta en marcha.
            source.emit(VoiceSourceEvent.Heard("Pausar tiempo"))
            runCurrent()
            val paused = (viewModel.uiState.value as CookModeUiState.Cooking).timer!!
            assertEquals(StepTimerPhase.PAUSED, paused.phase)
            assertEquals(298, paused.remainingSeconds)

            // S3: repetir el comando no reanuda; la cuenta no vuelve a correr.
            source.emit(VoiceSourceEvent.Heard("Pausar tiempo"))
            runCurrent()
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(
                StepTimerPhase.PAUSED,
                (viewModel.uiState.value as CookModeUiState.Cooking).timer!!.phase,
            )
            assertEquals(
                298,
                (viewModel.uiState.value as CookModeUiState.Cooking).timer!!.remainingSeconds,
            )
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun nextCommandOnTheLastStepFinishesAndTurnsVoiceOff() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()
            val finished = mutableListOf<CookModeEvent>()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { finished += it }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()
            goToStep(viewModel, targetStepIndex = 1)

            // D3/D11: «Siguiente» en el último Paso termina como «Terminar».
            source.emit(VoiceSourceEvent.Heard("Siguiente"))
            runCurrent()

            assertEquals(listOf<CookModeEvent>(CookModeEvent.Finish), finished)
            val cooking = viewModel.uiState.value as CookModeUiState.Cooking
            assertFalse(cooking.voice.enabled)
            assertNull(cooking.voice.notice)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun hostStopCutsVoiceAndHostStartResumesListeningWithoutReannouncing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()

            // D9: segundo plano corta TTS y micrófono.
            viewModel.onHostStopped()
            runCurrent()
            assertTrue(speaker.stopCount > 0)
            assertTrue(source.stopCount > 0)

            // D1/D9: al volver se reanuda la escucha sin relocutar el Paso.
            val announcedWhenStopped = speaker.announced.size
            viewModel.onHostStarted()
            runCurrent()

            val voice = (viewModel.uiState.value as CookModeUiState.Cooking).voice
            assertTrue(voice.enabled)
            assertEquals(2, source.startCount)
            assertEquals(announcedWhenStopped, speaker.announced.size)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun clearingTheViewModelReleasesBothVoiceEngines() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val speaker = FakeStepSpeaker()
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            // D9: al salir se liberan los dos motores de voz.
            viewModelStore.clear()
            runCurrent()
            assertEquals(1, speaker.releaseCount)
            assertEquals(1, source.releaseCount)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun theAlarmCutsTheSpeechBeforeRinging() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(timerRecipe())
            val speaker = FakeStepSpeaker()
            val alarm = FakeTimerAlarm(speaker)
            val source = FakeVoiceCommandSource()

            val viewModel = CookModeViewModel(
                recipes,
                alarm,
                speaker,
                source,
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            viewModel.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()
            goToStep(viewModel, targetStepIndex = 2)
            viewModel.onTimerStart()
            runCurrent()
            advanceUntilIdle()

            // D12: la alarma tiene prioridad: el TTS se corta antes de sonar.
            assertTrue(alarm.isRinging)
            assertEquals(1, alarm.startCount)
            assertTrue(alarm.speechStopsWhenRinging >= 1)
            assertTrue(speaker.stopCount >= 1)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun voiceIsOffByDefaultWhenTheModeIsReopened() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val recipes = InMemoryRecipeRepository()
            recipes.saveRecipe(twoStepRecipe())

            val first = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", first)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                first.uiState.collect { }
            }
            runCurrent()
            first.onVoiceEnabled(Locale.forLanguageTag("es-ES"))
            runCurrent()
            assertTrue((first.uiState.value as CookModeUiState.Cooking).voice.enabled)
            viewModelStore.clear()
            runCurrent()

            // D9: la voz vive solo en memoria: una sesión nueva nace apagada.
            val second = CookModeViewModel(
                recipes,
                FakeTimerAlarm(),
                FakeStepSpeaker(),
                FakeVoiceCommandSource(),
                "tortilla",
                servings = null,
            )
            viewModelStore.put("cook", second)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                second.uiState.collect { }
            }
            runCurrent()

            val voice = (second.uiState.value as CookModeUiState.Cooking).voice
            assertFalse(voice.enabled)
            assertNull(voice.notice)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    // Navega hasta el Paso pedido con `next()` y el scheduler al día (el estado
    // combinado se recalcula entre eventos, igual que en la UI real).
    private fun TestScope.goToStep(viewModel: CookModeViewModel, targetStepIndex: Int) {
        while ((viewModel.uiState.value as CookModeUiState.Cooking).stepIndex < targetStepIndex) {
            viewModel.next()
            runCurrent()
        }
    }

    // Receta con metas de temporizador distintas por Paso: 5 min, 2 min y 0 min
    // (este último dispara la alarma al primer tick y lo usa S1).
    private fun timerRecipe() = Recipe(
        id = "tortilla",
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = listOf(
            RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
            RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
        ),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", timerMinutes = 5, ingredientIds = listOf("potato")),
            RecipeStep(id = "s2", text = "Cuece la tortilla", timerMinutes = 2),
            RecipeStep(id = "s3", text = "Deja reposar", timerMinutes = 0),
        ),
    )

    private fun twoStepRecipe() = Recipe(
        id = "tortilla",
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = listOf(
            RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
            RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
        ),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", ingredientIds = listOf("potato")),
            RecipeStep(id = "s2", text = "Cuece la tortilla"),
        ),
    )
}

// Doble de la alarma del temporizador (patrón de tests de data/): cuenta las
// llamadas y mantiene si está sonando, sin tocar el audio del dispositivo. Con el
// hablante enlazado, guarda cuántos cortes de voz había cuando empezó a sonar
// para probar la prioridad de la alarma (D12).
private class FakeTimerAlarm(
    private val speech: FakeStepSpeaker? = null,
) : TimerAlarm {
    var startCount = 0
        private set
    var isRinging = false
        private set
    var speechStopsWhenRinging = 0
        private set

    override fun start() {
        startCount += 1
        isRinging = true
        speechStopsWhenRinging = speech?.stopCount ?: 0
    }

    override fun stop() {
        isRinging = false
    }
}

// Doble del TTS (RD24, D8): registra el locale preparado y las locuciones emitidas;
// `prepared = false` simula un motor inexistente.
private class FakeStepSpeaker(
    var prepared: Boolean = true,
) : StepSpeaker {
    val announced = mutableListOf<StepAnnouncement>()
    val preparedFor = mutableListOf<Locale>()
    var stopCount = 0
        private set
    var releaseCount = 0
        private set

    override suspend fun prepare(locale: Locale): Boolean {
        preparedFor += locale
        return prepared
    }

    override fun speak(announcement: StepAnnouncement) {
        announced += announcement
    }

    override fun stop() {
        stopCount += 1
    }

    override fun release() {
        releaseCount += 1
    }
}

// Doble del reconocimiento (RD24, D8): registra arranques/paradas y deja emitir
// transcripciones o fallos al ViewModel; `available = false` simula sin servicio.
private class FakeVoiceCommandSource(
    var available: Boolean = true,
) : VoiceCommandSource {
    private val mutableEvents = MutableSharedFlow<VoiceSourceEvent>()
    override val events: Flow<VoiceSourceEvent> = mutableEvents

    var startCount = 0
        private set
    var stopCount = 0
        private set
    var releaseCount = 0
        private set

    override fun start(locale: Locale): Boolean {
        startCount += 1
        return available
    }

    override fun stop() {
        stopCount += 1
    }

    override fun release() {
        releaseCount += 1
    }

    suspend fun emit(event: VoiceSourceEvent) {
        mutableEvents.emit(event)
    }
}
