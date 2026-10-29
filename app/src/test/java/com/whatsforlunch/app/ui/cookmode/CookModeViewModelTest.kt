package com.whatsforlunch.app.ui.cookmode

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.audio.TimerAlarm
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.cookmode.StepTimerPhase
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = 2)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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
            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = 13)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = 2)
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
            val viewModel = CookModeViewModel(InMemoryRecipeRepository(), FakeTimerAlarm(), "missing", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "draft", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, FakeTimerAlarm(), "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, alarm, "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, alarm, "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, alarm, "tortilla", servings = null)
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
            assertEquals(120, secondStep.timer!!.remainingSeconds)
            assertFalse(alarm.isRinging)

            viewModel.previous()
            runCurrent()
            val firstStep = (viewModel.uiState.value as CookModeUiState.Cooking)
            assertEquals(StepTimerPhase.READY, firstStep.timer!!.phase)
            assertEquals(300, firstStep.timer!!.remainingSeconds)

            // Volver al Paso 2 también entra «Listo» (sin cuenta ni alarma heredadas).
            val startsBeforeReentering = alarm.startCount
            viewModel.next()
            runCurrent()
            val backToSecond = (viewModel.uiState.value as CookModeUiState.Cooking)
            assertEquals(StepTimerPhase.READY, backToSecond.timer!!.phase)
            assertEquals(120, backToSecond.timer!!.remainingSeconds)
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

            val viewModel = CookModeViewModel(recipes, alarm, "tortilla", servings = null)
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

            val viewModel = CookModeViewModel(recipes, alarm, "tortilla", servings = null)
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
// llamadas y mantiene si está sonando, sin tocar el audio del dispositivo.
private class FakeTimerAlarm : TimerAlarm {
    var startCount = 0
        private set
    var isRinging = false
        private set

    override fun start() {
        startCount += 1
        isRinging = true
    }

    override fun stop() {
        isRinging = false
    }
}
