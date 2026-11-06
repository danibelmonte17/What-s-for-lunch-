package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.audio.SystemRingtoneTimerAlarm
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.ui.cookmode.CookModeScreen
import com.whatsforlunch.app.ui.cookmode.CookModeTestTags
import com.whatsforlunch.app.ui.cookmode.CookModeViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E del temporizador del Paso (RD23, RF-GTP1 a RF-GTP6): escenarios S1-S8 de
// docs/specs/gestionar-temporizador-de-paso.md (verify 1 = S1, verify 2 = S2).
// La alarma se verifica en tres niveles (Verification Plan de la spec): aquí el
// estado de alarma en la UI, con el sonido REAL del tono del sistema (D4; se
// comprueba a mano durante la ejecución con `adb shell dumpsys audio`) y con
// `FakeTimerAlarm` en los tests unitarios del ViewModel.
@RunWith(AndroidJUnit4::class)
class CookModeTimerE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private var timerAlarm: SystemRingtoneTimerAlarm? = null

    // Cada escenario termina con la alarma silenciada y el temporizador descartado
    // (el ViewModel sale del store y ejecuta su limpieza), aunque falle a mitad.
    @After
    fun tearDown() {
        timerAlarm?.stop()
        viewModelStore.clear()
    }

    // S1 (verify 1): dado un Paso con temporizador de 0 min (el paso más corto:
    // cualquier meta llega a cero igual) abierto en el Modo Cocina, «Iniciar» lleva
    // la cuenta a 00:00 y suena la alarma.
    @Test
    fun alarmRingsWhenTheCountdownReachesZero() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)

        // D3: el temporizador está «Listo» con su meta y nunca arranca solo.
        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Temporizador").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("00:00")
        composeRule.onNodeWithText("Meta: 0 min").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Listo")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).assertIsDisplayed()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()

        // verify 1: la alarma suena al llegar a cero. En la UI se observa el estado
        // «¡Tiempo cumplido!» con «+1 min» y «Detener»; el sonido es el tono real del
        // sistema y se comprueba a mano (Verification Plan, nivel 3).
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("¡Tiempo cumplido!").fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("¡Tiempo cumplido!")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("00:00")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_ADD_MINUTE).assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STOP).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER_START).assertCountEquals(0)
    }

    // S2 (verify 2): con la alarma sonando (S1), «+1 min» la silencia, amplía la meta
    // a 1 min y reanuda la cuenta (reloj ≈ 01:00).
    @Test
    fun addMinuteSilencesTheAlarmAndExtendsTheCountdown() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        reachAlarmWithZeroMinuteStep()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_ADD_MINUTE).performClick()

        // verify 2: la cuenta se extiende (01:00 y bajando) con la meta ya ampliada.
        composeRule.waitUntil(5_000) { clockText().matches(Regex("00:5[0-9]|01:0[0-9]")) }
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("En marcha")
        composeRule.onNodeWithText("Meta: 1 min").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER_STOP).assertCountEquals(0)
    }

    // S3: dado un Paso con temporizador de 5 min al entrar en el Modo Cocina, el
    // temporizador está «Listo» con 05:00 y «Iniciar» (no arranca solo); «Iniciar»
    // lo pone «En marcha» y el reloj decrece.
    @Test
    fun startingIsManualAndTheCountdownDecrements() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        openSecondStep()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("05:00")
        composeRule.onNodeWithText("Meta: 5 min").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Listo")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER_PAUSE).assertCountEquals(0)

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("En marcha")
        composeRule.waitUntil(5_000) { clockText() != "05:00" }
    }

    // S4: «Pausar» congela el reloj (píldora «Pausado») y «Reanudar» retoma la cuenta
    // desde donde estaba.
    @Test
    fun pausingFreezesTheCountdownAndResumingContinues() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        openSecondStep()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()
        composeRule.waitUntil(5_000) { clockText() != "05:00" }

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_PAUSE).performClick()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Pausado")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_RESUME).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER_PAUSE).assertCountEquals(0)

        // Pausado: el reloj se congela (se observa el mismo valor tras esperar).
        val frozen = clockText()
        Thread.sleep(2_000)
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals(frozen)

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_RESUME).performClick()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("En marcha")
        composeRule.waitUntil(5_000) { clockText() != frozen }
    }

    // S5: «+1 min» amplía la meta y «Reiniciar» restablece la cuenta a la meta
    // vigente (5 min + 1 min = 6 min) dejándola «En marcha».
    @Test
    fun resetRestoresTheCurrentMetaAndLeavesItRunning() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        openSecondStep()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()
        composeRule.waitUntil(5_000) { clockText() != "05:00" }

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_ADD_MINUTE).performClick()
        composeRule.onNodeWithText("Meta: 6 min").assertIsDisplayed()

        // «Reiniciar» usa la meta vigente (6 min), no la original del Paso.
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_RESET).performClick()
        composeRule.waitUntil(5_000) { clockText().matches(Regex("06:0[0-2]")) }
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("En marcha")
    }

    // S6: cambiar de Paso con la alarma sonando silencia la alarma y descarta la
    // cuenta (el sonido se comprueba a mano; el descarte con `FakeTimerAlarm` en
    // unit): el nuevo Paso entra «Listo» con su propia meta y, al volver al Paso 1,
    // también entra «Listo» (sin cuenta ni alarma heredadas).
    @Test
    fun changingStepDiscardsTheTimerAndTheAlarm() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        reachAlarmWithZeroMinuteStep()

        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Listo")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("05:00")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER_STOP).assertCountEquals(0)

        composeRule.onNodeWithTag(CookModeTestTags.PREVIOUS_STEP).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Listo")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("00:00")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).assertIsDisplayed()
        composeRule.onAllNodesWithText("¡Tiempo cumplido!").assertCountEquals(0)
    }

    // S7 (RF-GTP6): un Paso sin `timerMinutes` no muestra el widget del temporizador.
    @Test
    fun stepWithoutTimerShowsNoWidget() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }
        launchTimerCookMode(recipes)
        openSecondStep()

        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()

        composeRule.onNodeWithText("Paso 3 de 3").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.TIMER).assertCountEquals(0)
    }

    // S8: los textos del widget (título, estados, botones y meta) cambian con el
    // idioma (paridad de claves es/en); los textos en español los cubren S1-S7.
    @Test
    fun timerTextsChangeWithTheLanguage() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(timerRecipe()) }

        launchTimerCookMode(recipes, locale = Locale.ENGLISH)

        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Timer").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_STATUS).assertTextEquals("Ready")
        composeRule.onNodeWithText("Goal: 0 min").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).assertTextEquals("Start")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_RESET).assertTextEquals("Reset")
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_ADD_MINUTE).assertTextEquals("+1 min")
    }

    // Lleva el temporizador del Paso 1 (meta 0 min) hasta «¡Tiempo cumplido!» con la
    // alarma sonando (misma preparación que S1, verify 1).
    private fun reachAlarmWithZeroMinuteStep() {
        composeRule.onNodeWithTag(CookModeTestTags.TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_START).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("¡Tiempo cumplido!").fetchSemanticsNodes().size == 1
        }
    }

    // Paso 1 → Paso 2 (el de 5 min) con la navegación del dock.
    private fun openSecondStep() {
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 3").assertIsDisplayed()
    }

    // Texto actual del reloj digital del widget (mm:ss).
    private fun clockText(): String =
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK)
            .fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }

    // Receta de los escenarios: 3 Pasos con metas distintas — 0 min (S1/S2, alarma
    // inmediata), 5 min (S3-S5) y sin temporizador (S7).
    private fun timerRecipe() = Recipe(
        id = "tortilla",
        title = "Tortilla de patatas",
        baseServings = 4,
        ingredients = listOf(
            RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
        ),
        steps = listOf(
            RecipeStep(id = "s1", text = "Corta las patatas", timerMinutes = 0, ingredientIds = listOf("potato")),
            RecipeStep(id = "s2", text = "Cuece la tortilla", timerMinutes = 5),
            RecipeStep(id = "s3", text = "Deja reposar"),
        ),
    )

    private fun contextFor(locale: Locale) =
        InstrumentationRegistry.getInstrumentation().targetContext
            .let { baseContext ->
                baseContext.createConfigurationContext(
                    Configuration(baseContext.resources.configuration).apply {
                        setLocale(locale)
                    },
                )
            }

    // Compone el Modo Cocina con su ViewModel (en un ViewModelStore, para poder
    // descartarlo en @After) y la alarma REAL del tono del sistema (D4).
    private fun launchTimerCookMode(
        recipes: InMemoryRecipeRepository,
        recipeId: String = "tortilla",
        locale: Locale = Locale("es", "ES"),
    ) {
        val context = contextFor(locale)
        val alarm = SystemRingtoneTimerAlarm(context)
        timerAlarm = alarm
        // Fuera del lambda de composición: lint prohíbe construir ViewModels dentro
        // de un composable (patrón de CookModeE2ETest). Los motores de voz son fakes
        // (RD24, D15): este test no toca micrófono ni TTS.
        val viewModel = CookModeViewModel(
            recipes,
            alarm,
            FakeStepSpeaker(),
            FakeVoiceCommandSource(),
            recipeId,
            servings = null,
        )
        viewModelStore.put("cook", viewModel)
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
                        onFinish = {},
                        onTimerStart = viewModel::onTimerStart,
                        onTimerPause = viewModel::onTimerPause,
                        onTimerResume = viewModel::onTimerResume,
                        onTimerReset = viewModel::onTimerReset,
                        onTimerAddMinute = viewModel::onTimerAddMinute,
                        onTimerStop = viewModel::onTimerStop,
                        onVoiceToggle = {},
                        onSpeakStep = {},
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CookModeTestTags.SCREEN).fetchSemanticsNodes().size == 1
        }
    }
}
