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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.audio.SystemRingtoneTimerAlarm
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.ui.cookmode.CookModeScreen
import com.whatsforlunch.app.ui.cookmode.CookModeTestTags
import com.whatsforlunch.app.ui.cookmode.CookModeViewModel
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

// Almacén de fotos sin uso: los tests solo guardan Recetas sin archivo real.
private object CookModeNoOpPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) = Unit
}

// S1-S7 de cocinar-paso-a-paso (RD22): Modo Cocina paso a paso con drawer de los
// Ingredientes vinculados del Paso activo, Raciones heredadas del Detalle y
// navegación «Paso Anterior»/«Siguiente»/«Terminar». Fixtures locales, sin IA ni red.
@RunWith(AndroidJUnit4::class)
class CookModeE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1 (verify 1): Receta con 2 Pasos, el Paso 1 con «Patatas» vinculada (700 g en
    // 4 Raciones base) y Raciones a preparar = 2 → «Paso 1 de 2», el texto del Paso
    // y el drawer con «Patatas» y su cantidad escalada (350 g).
    @Test
    fun startingCookingModeShowsFirstStepWithScaledIngredients() {
        seedRoomRecipe(twoStepRecipe())
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        // Raciones a preparar = 2 sobre base 4: el Modo Cocina hereda la selección (RF-CPP4).
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_SCALER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("2")

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithText("Corta las patatas").assertIsDisplayed()
        // Drawer con el Ingrediente vinculado del Paso y su cantidad escalada.
        composeRule.onNodeWithTag(CookModeTestTags.INGREDIENT_ROW).assertIsDisplayed()
        composeRule.onNodeWithText("350 g Patatas").assertIsDisplayed()
        // D1: solo los Ingredientes vinculados del Paso; Huevos pertenece al Paso 2.
        composeRule.onAllNodesWithText("2 uds Huevos").assertCountEquals(0)
    }

    // S2 (verify 2): dado el Modo Cocina en el Paso 1 de 2, «Siguiente» actualiza la
    // barra de avance a «Paso 2 de 2» y el botón primario pasa a «Terminar» (D2).
    // También cubre el widget del temporizador del Paso (RF-GTP1, D5) y el drawer
    // colapsable.
    @Test
    fun advancingStepUpdatesProgressAndShowsFinishButton() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(twoStepRecipe()) }

        launchCookMode(recipes, "tortilla", servings = 2)

        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithText("Siguiente").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.PREVIOUS_STEP).assertIsNotEnabled()

        // RF-CPP2: el drawer colapsa y vuelve a desplegar sus filas.
        composeRule.onNodeWithTag(CookModeTestTags.INGREDIENT_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.INGREDIENTS_TOGGLE).performClick()
        composeRule.onAllNodesWithTag(CookModeTestTags.INGREDIENT_ROW).assertCountEquals(0)
        composeRule.onNodeWithTag(CookModeTestTags.INGREDIENTS_TOGGLE).performClick()
        composeRule.onNodeWithTag(CookModeTestTags.INGREDIENT_ROW).assertIsDisplayed()

        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()

        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.PREVIOUS_STEP).assertIsEnabled()
        // RF-GTP1 (D5): el hint pasivo «⏱ N min» es ahora el widget del temporizador,
        // «Listo» con su meta y arranque manual (D3).
        composeRule.onNodeWithTag(CookModeTestTags.TIMER).assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.TIMER_CLOCK).assertTextEquals("05:00")
        composeRule.onNodeWithText("Meta: 5 min").assertIsDisplayed()
        composeRule.onNodeWithText("Terminar").assertIsDisplayed()
        composeRule.onAllNodesWithText("Siguiente").assertCountEquals(0)
    }

    // S3: dado el Modo Cocina en el último Paso, «Terminar» vuelve al Detalle de la
    // misma Receta con las Raciones que había elegido (D2).
    @Test
    fun finishingReturnsToDetailKeepingTheChosenServings() {
        seedRoomRecipe(twoStepRecipe())
        launchFullApp()
        openCookModeFromDetail(servingsDecrements = 2)

        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        composeRule.onNodeWithText("Terminar").performClick()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("2")
        composeRule.onNodeWithText("350 g Patatas").performScrollTo().assertIsDisplayed()
    }

    // S4: un Paso sin Ingredientes vinculados (Receta de IA o legada) muestra
    // «Sin ingredientes» y nunca «todos» los Ingredientes de la Receta (D1/D4).
    @Test
    fun stepWithoutLinksShowsNoIngredientsMessage() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "legacy",
                    title = "Receta legada",
                    baseServings = 4,
                    ingredients = listOf(
                        RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
                        RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
                    ),
                    steps = listOf(
                        RecipeStep(id = "s1", text = "Mezcla todo"),
                        RecipeStep(id = "s2", text = "Cuece"),
                    ),
                ),
            )
        }

        launchCookMode(recipes, "legacy", servings = null)

        composeRule.onNodeWithText("Sin ingredientes").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.INGREDIENT_ROW).assertCountEquals(0)
        composeRule.onAllNodesWithText("4 uds Huevos").assertCountEquals(0)
        composeRule.onAllNodesWithText("700 g Patatas").assertCountEquals(0)

        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()

        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()
        composeRule.onNodeWithText("Sin ingredientes").assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.INGREDIENT_ROW).assertCountEquals(0)
    }

    // S5: un Borrador sin Pasos abierto en el Detalle tiene «Empezar Modo Cocina»
    // deshabilitada y no navega (D3, RF-CPP5).
    @Test
    fun detailCtaIsDisabledWithoutSteps() {
        seedRoomRecipe(Recipe(id = "draft", title = "Borrador vacío"))
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).assertIsNotEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CookModeTestTags.SCREEN).assertCountEquals(0)
    }

    // S6: salir a mitad de la cocción con «Volver» regresa al Detalle sin
    // confirmación y al reabrir el Modo Cocina empieza en el Paso 1 (D7).
    @Test
    fun leavingMidCookRestartsAtFirstStepWhenReopening() {
        seedRoomRecipe(twoStepRecipe())
        launchFullApp()
        openCookModeFromDetail()

        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithTag(CookModeTestTags.NEXT_STEP).performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()
        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
    }

    // S7: los textos del Modo Cocina, el drawer y los botones cambian con el idioma
    // (paridad de claves es/en).
    @Test
    fun cookModeTextsChangeWithTheLanguage() {
        val recipes = InMemoryRecipeRepository()
        runBlocking { recipes.saveRecipe(twoStepRecipe()) }

        launchCookMode(recipes, "tortilla", servings = 2, locale = Locale.ENGLISH)

        composeRule.onNodeWithText("Cooking Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("STEP 1").assertIsDisplayed()
        composeRule.onNodeWithText("Ingredients for this step").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()
        composeRule.onNodeWithText("Previous step").assertIsDisplayed()
    }

    // Receta base de los escenarios: 4 Raciones base, «Patatas» (700 g) vinculada al
    // Paso 1 y «Huevos» (4 uds) al Paso 2, que además tiene temporizador (D6).
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
            RecipeStep(id = "s2", text = "Cuece la tortilla", timerMinutes = 5, ingredientIds = listOf("egg")),
        ),
    )

    // Flujo común de los escenarios con la app real: abre el Detalle de la única
    // Receta, ajusta las Raciones con el stepper y pulsa la CTA hacia el Modo Cocina.
    private fun openCookModeFromDetail(servingsDecrements: Int = 0) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        if (servingsDecrements > 0) {
            composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_SCALER)
                .performScrollTo()
                .assertIsDisplayed()
            repeat(servingsDecrements) {
                composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
            }
        }

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()
        composeRule.onNodeWithTag(CookModeTestTags.SCREEN).assertIsDisplayed()
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

    private fun launchCookMode(
        recipes: InMemoryRecipeRepository,
        recipeId: String,
        servings: Int?,
        locale: Locale = Locale("es", "ES"),
        onBack: () -> Unit = {},
        onFinish: () -> Unit = {},
    ) {
        val context = contextFor(locale)
        // Fuera del lambda de composición: lint prohíbe construir ViewModels dentro
        // de un composable (patrón de RecipeDetailE2ETest). La alarma real (tono del
        // sistema) solo suena si el temporizador llega a cero (RF-GTP4).
        val viewModel = CookModeViewModel(recipes, SystemRingtoneTimerAlarm(context), recipeId, servings)
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
                        onBack = onBack,
                        onNext = viewModel::next,
                        onPrevious = viewModel::previous,
                        onFinish = onFinish,
                        onTimerStart = viewModel::onTimerStart,
                        onTimerPause = viewModel::onTimerPause,
                        onTimerResume = viewModel::onTimerResume,
                        onTimerReset = viewModel::onTimerReset,
                        onTimerAddMinute = viewModel::onTimerAddMinute,
                        onTimerStop = viewModel::onTimerStop,
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CookModeTestTags.SCREEN).fetchSemanticsNodes().size == 1
        }
    }

    private fun launchFullApp() {
        val context = contextFor(Locale("es", "ES"))
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun seedRoomRecipe(recipe: Recipe) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomRecipeRepository(database.recipeDao(), CookModeNoOpPhotoStore)
        runBlocking { repository.saveRecipe(recipe) }
    }
}
