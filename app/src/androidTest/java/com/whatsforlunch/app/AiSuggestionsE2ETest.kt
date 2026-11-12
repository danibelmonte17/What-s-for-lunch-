package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.RecipeAvoidance
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.RecipeSuggestionParser
import com.whatsforlunch.app.data.ai.RecipeSuggestionsResult
import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.domain.suggestion.SuggestionAcceptance
import com.whatsforlunch.app.domain.suggestion.SuggestionRules
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsScreen
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsTestTags
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsViewModel
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.recommendations.RecommendationsScreen
import com.whatsforlunch.app.ui.recommendations.RecommendationsTestTags
import com.whatsforlunch.app.ui.recommendations.RecommendationsViewModel
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S8 de sugerir-recetas-con-ia: selección de Alimentos (D3), llamada simulada
// única (D10), Coincidencia en el cliente (D2/RD01/RD10), estados vacío/error y
// navegación real desde el Escáner (RF-SRC1). Nunca se llama a la IA real.
@RunWith(AndroidJUnit4::class)
class AiSuggestionsE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // S1 (verify 1): las propuestas se listan con título, badge calculado en el
    // cliente, «X de Y ingredientes disponibles», ingredientes y pasos.
    @Test
    fun suggestionsAreListedWithClientMatchIngredientsAndSteps() {
        val pantry = pantryWith(
            PantryItem("manzana", "Manzana", 3),
            PantryItem("nata", "Nata", 1),
        )
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(
                        suggestion(
                            title = "Tarta de manzana",
                            required = listOf("Manzana", "Harina"),
                            steps = listOf(RecipeStep(text = "Pela las manzanas", timerMinutes = 10)),
                        ),
                        suggestion(title = "Sopa de nata", required = listOf("Nata")),
                    ),
                ),
            ),
        )

        launchSuggestions(pantry = pantry, suggester = suggester)

        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionItem("Manzana")).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionCard(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionMatch(0)).assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()
        composeRule.onNodeWithText("1 de 2 ingredientes disponibles").assertIsDisplayed()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionCard(1)).performScrollTo()
        composeRule.onNodeWithText("1 de 1 ingredientes disponibles").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionCard(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionMatch(1)).assertIsDisplayed()
        composeRule.onNodeWithText("100% Match").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.SUGGESTION_INGREDIENTS)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.SUGGESTION_STEPS)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        composeRule.onNodeWithText("10 min").assertIsDisplayed()
        assertEquals(1, suggester.calls)
    }

    // S2 (verify 2): sin API key se pide configurarla y «Abrir Ajustes» navega.
    @Test
    fun missingApiKeyAsksToConfigureItAndOpensSettings() {
        val settingsOpened = mutableStateOf(false)
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.MissingApiKey)),
            onOpenSettings = { settingsOpened.value = true },
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText("Configura tu API key en Ajustes para pedir sugerencias con IA.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.OPEN_SETTINGS).performClick()
        assertTrue(settingsOpened.value)
        composeRule.onNodeWithTag(AiSuggestionsTestTags.SELECTION_LIST).assertIsDisplayed()
    }

    // S3: sin ningún Alimento seleccionado no se puede pedir.
    @Test
    fun requestIsDisabledWhenEveryFoodIsDeselected() {
        launchSuggestions(
            pantry = pantryWith(
                PantryItem("manzana", "Manzana", 3),
                PantryItem("nata", "Nata", 1),
            ),
            suggester = FakeRecipeSuggester(emptyList()),
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Manzana")).assertIsOn()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Manzana")).performClick()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Manzana")).assertIsOff()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Nata")).performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).assertIsNotEnabled()
    }

    // S4: fallo con aviso y «Reintentar»; el segundo intento lista resultados.
    @Test
    fun failureShowsNoticeAndRetryListsSuggestions() {
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Failed(AiFailureCause.NO_CONNECTION),
                RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana"))),
            ),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText("Sin conexión. Comprueba tu red y vuelve a intentarlo.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.RETRY).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Tarta de manzana").assertIsDisplayed()
        assertEquals(2, suggester.calls)
    }

    // S5: el aviso de fallo es descartable con un control de 48 dp.
    @Test
    fun failureNoticeCanBeDismissed() {
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = FakeRecipeSuggester(
                listOf(RecipeSuggestionsResult.Failed(AiFailureCause.PROVIDER_ERROR)),
            ),
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.NOTICE).assertIsDisplayed()
        composeRule.onNodeWithText("El proveedor de IA no está disponible ahora mismo. Inténtalo más tarde.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.NOTICE_DISMISS).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.NOTICE).assertDoesNotExist()
    }

    // S7: los avisos de fallo también se localizan en inglés.
    @Test
    fun failureNoticeIsLocalizedInEnglish() {
        launchSuggestions(
            pantry = pantryWith(PantryItem("apple", "Apple", 3)),
            suggester = FakeRecipeSuggester(
                listOf(RecipeSuggestionsResult.Failed(AiFailureCause.NO_CONNECTION)),
            ),
            locale = Locale.ENGLISH,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.onNodeWithText("No connection. Check your network and try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.RETRY).assertIsDisplayed()
    }

    // S5: sin propuestas válidas se muestra el estado vacío con «Reintentar».
    @Test
    fun emptyAnswerShowsEmptyStateWithRetry() {
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = FakeRecipeSuggester(listOf(RecipeSuggestionsResult.Success(emptyList()))),
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.RETRY).assertIsDisplayed()
    }

    // S6: textos nuevos de selección, botón, estados y cabeceras en inglés.
    @Test
    fun suggestionsScreenIsLocalizedInEnglish() {
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Apple tart")))),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("apple", "Apple", 3)),
            suggester = suggester,
            locale = Locale.ENGLISH,
        )

        composeRule.onNodeWithText("Choose the foods you want to send").assertIsDisplayed()
        composeRule.onNodeWithText("Ask for suggestions").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Apple tart").assertIsDisplayed()
        composeRule.onNodeWithText("Ingredients").assertIsDisplayed()
        composeRule.onNodeWithText("Steps").assertIsDisplayed()
    }

    // S7: desde «Recetas con mi Despensa», el botón abre la selección con chips del
    // Escaneo y la Despensa preseleccionados; al volver, los chips sin guardar siguen
    // intactos (RF-SDR4, D7).
    @Test
    fun pantryRecipesButtonOpensSelectionWithChipsAndPantryPreselected() {
        val pantry = pantryWith(PantryItem("tomate", "Tomate", 1))
        val holder = ScanDraftHolder()
        val scannerViewModel = ScannerViewModel(
            pantryRepository = pantry,
            scanImageAnalyzer = AlwaysFailingScanAnalyzer,
            scanDraftHolder = holder,
        )
        val recommendationsViewModel = RecommendationsViewModel(
            pantry,
            InMemoryRecipeRepository(),
            InMemoryShoppingListRepository(),
        )
        val suggestionsViewModel = AiSuggestionsViewModel(
            pantry,
            holder,
            FakeRecipeSuggester(emptyList()),
            InMemoryRecipeRepository(),
        )
        val showSuggestions = mutableStateOf(false)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es-ES"))
        }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val recommendationsState by recommendationsViewModel.uiState.collectAsState()
            val suggestionsState by suggestionsViewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    if (showSuggestions.value) {
                        AiSuggestionsScreen(
                            uiState = suggestionsState,
                            onBack = { showSuggestions.value = false },
                            onOpenSettings = {},
                            onToggleFood = suggestionsViewModel::toggleFood,
                            onRequest = { suggestionsViewModel.requestSuggestions("es") },
                            onRetry = suggestionsViewModel::retry,
                            onAccept = suggestionsViewModel::acceptSuggestion,
                            onDiscard = suggestionsViewModel::discardSuggestion,
                            onClearNotice = suggestionsViewModel::clearNotice,
                        )
                    } else {
                        RecommendationsScreen(
                            uiState = recommendationsState,
                            onBack = {},
                            onOpenRecipe = {},
                            onSuggestWithAi = { showSuggestions.value = true },
                            onAddMissingToShoppingList = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        scannerViewModel.addManualItem("Chicle")
        scannerViewModel.addManualItem("Mint")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(RecommendationsTestTags.SUGGEST_AI).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionItem("Chicle")).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionItem("Mint")).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionItem("Tomate")).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Chicle")).assertIsOn()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Mint")).assertIsOn()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.selectionToggle("Tomate")).assertIsOn()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(RecommendationsTestTags.SCREEN).assertIsDisplayed()
        assertEquals(listOf("Chicle", "Mint"), holder.items.value.map { it.name })
    }

    // S8: todos los Ingredientes cuentan en la Coincidencia (D5: sin opcionalidad) y
    // no aparece ninguna píldora «Opcional».
    @Test
    fun allIngredientsCountForTheMatchAndNoOptionalPillIsShown() {
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(
                        suggestion(
                            title = "Tarta de manzana",
                            required = listOf("Manzana", "Harina", "Canela"),
                        ),
                    ),
                ),
            ),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionMatch(0)).assertIsDisplayed()
        composeRule.onNodeWithText("33%").assertIsDisplayed()
        composeRule.onNodeWithText("1 de 3 ingredientes disponibles").assertIsDisplayed()
        composeRule.onNodeWithText("Opcional").assertDoesNotExist()
    }

    // S1 (verify 1 de `aceptar-sugerencia-de-receta`): aceptar guarda la propuesta como
    // Receta con origen Sugerencia (RD07), muestra el aviso y la Receta aparece en
    // Mi recetario sin foto y sin distintivo de estado (S5).
    @Test
    fun acceptingSavesTheSuggestionAsARecipeVisibleInMyRecipes() {
        val pantry = pantryWith(
            PantryItem("manzana", "Manzana", 3),
            PantryItem("nata", "Nata", 1),
        )
        val recipes = InMemoryRecipeRepository()
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(
                        suggestion(title = "Tarta de manzana", required = listOf("Manzana", "Harina")),
                        suggestion(title = "Sopa de nata", required = listOf("Nata")),
                    ),
                ),
            ),
        )
        val viewModel = AiSuggestionsViewModel(pantry, ScanDraftHolder(), suggester, recipes)
        val myRecipesViewModel = MyRecipesViewModel(recipes, pantry)
        val showMyRecipes = mutableStateOf(false)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es-ES"))
        }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val suggestionsState by viewModel.uiState.collectAsState()
            val myRecipesState by myRecipesViewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    if (showMyRecipes.value) {
                        MyRecipesScreen(
                            uiState = myRecipesState,
                            onCreateRecipe = {},
                            onScanPantry = {},
                        )
                    } else {
                        AiSuggestionsScreen(
                            uiState = suggestionsState,
                            onBack = {},
                            onOpenSettings = {},
                            onToggleFood = viewModel::toggleFood,
                            onRequest = { viewModel.requestSuggestions("es") },
                            onRetry = viewModel::retry,
                            onAccept = viewModel::acceptSuggestion,
                            onDiscard = viewModel::discardSuggestion,
                            onClearNotice = viewModel::clearNotice,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0))
            .assertIsEnabled()
            .performClick()

        composeRule.onNodeWithText("Guardada en Mi recetario").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionState(0)).assertIsDisplayed()
        composeRule.onNodeWithText("Aceptada").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).assertIsNotEnabled()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionDiscard(0)).assertIsNotEnabled()

        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        composeRule.waitUntil(5_000) { runBlocking { recipes.getRecipe(id) } != null }
        val saved = runBlocking { recipes.getRecipe(id) }!!
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertNull(saved.photoPath)
        assertEquals(listOf("$id-ingredient-0", "$id-ingredient-1"), saved.ingredients.map { it.id })
        assertEquals(listOf("$id-step-0"), saved.steps.map { it.id })

        showMyRecipes.value = true
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Tarta de manzana").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Tarta de manzana").assertIsDisplayed()
        // S5/RF-EBF6: sin foto y sin ningún distintivo de estado.
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
        composeRule.onAllNodesWithTag("my_recipes_card_status").assertCountEquals(0)
    }

    // unificar-unidades · verify 3 (S4, D2): el cliente simulado devuelve unidades
    // desconocidas («gramos»), con espacios (« ML »), vacía y ausente; el parser y
    // la normalización reales las convierten en unidades del enum cerrado («ud» para
    // lo desconocido y lo vacío) y así se guarda la Receta al aceptar.
    @Test
    fun acceptingNormalizesAiUnitsToTheClosedEnum() {
        val rawResponse = """{"s":[{"t":"Tarta de manzana","st":"Con unidades raras","mm":"DESSERT",
            "pm":10,"cm":20,"df":"EASY","bs":4,"kcal":320,
            "i":[{"n":"Harina","q":200,"u":"gramos"},{"n":"Leche","q":150,"u":" ML "},
                 {"n":"Huevo","q":2},{"n":"Sal","q":1,"u":""}],
            "p":[{"t":"Mezcla todo","m":5}]}]}"""
        val parsed = requireNotNull(RecipeSuggestionParser.parse(rawResponse))
        val normalized = SuggestionRules.normalize(parsed).single()
        val recipes = InMemoryRecipeRepository()
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(normalized))),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
            recipes = recipes,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).performClick()

        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        composeRule.waitUntil(5_000) { runBlocking { recipes.getRecipe(id) } != null }
        val saved = runBlocking { recipes.getRecipe(id) }!!
        assertEquals(listOf("ud", "ml", "ud", "ud"), saved.ingredients.map { it.unit })
    }

    // S2 (verify 2 de `aceptar-sugerencia-de-receta`): aceptar dos veces la misma
    // Sugerencia reescribe la misma Receta (id estable + upsert) y no duplica.
    @Test
    fun acceptingTheSameSuggestionTwiceDoesNotDuplicateTheRecipe() {
        val recipes = InMemoryRecipeRepository()
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana")))),
        )
        val viewModel = launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
            recipes = recipes,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).performClick()

        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        composeRule.runOnIdle { viewModel.acceptSuggestion(id) }
        composeRule.waitForIdle()

        assertEquals(1, runBlocking { recipes.observeRecipes().first() }.size)
    }

    // S3: descartar no guarda nada y deja la tarjeta «Descartada» con las acciones
    // deshabilitadas, sin retirarla de la lista.
    @Test
    fun discardingKeepsTheCardWithDisabledActionsAndSavesNothing() {
        val recipes = InMemoryRecipeRepository()
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de manzana")))),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
            recipes = recipes,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionDiscard(0)).performClick()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionCard(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionState(0)).assertIsDisplayed()
        composeRule.onNodeWithText("Descartada").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).assertIsNotEnabled()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionDiscard(0)).assertIsNotEnabled()
        assertTrue(runBlocking { recipes.observeRecipes().first() }.isEmpty())
    }

    // S4: solo las Recetas existentes con Coincidencia > 60 % viajan al prompt.
    @Test
    fun onlyExistingRecipesAboveSixtyPercentAreSentToThePrompt() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(storedRecipe("Tarta de manzana", listOf("Manzana")))
            recipes.saveRecipe(storedRecipe("Ensalada", listOf("Lechuga", "Harina")))
        }
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Tarta de pera")))),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
            recipes = recipes,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(1, suggester.lastAvoidance.size)
        assertEquals("Tarta de manzana", suggester.lastAvoidance.single().title)
        assertEquals(listOf("Manzana"), suggester.lastAvoidance.single().ingredientNames)
    }

    // S5: aceptar y descartar conviven en la misma lista; una lista nueva vuelve a
    // empezar con todas las propuestas en estado propuesta (RF-ASR7).
    @Test
    fun mixedStatesCoexistAndANewListStartsEverySuggestionAsProposed() {
        val suggester = FakeRecipeSuggester(
            listOf(
                RecipeSuggestionsResult.Success(
                    listOf(
                        suggestion(title = "Tarta de manzana"),
                        suggestion(title = "Tarta de pera"),
                        suggestion(title = "Tarta de nata"),
                    ),
                ),
            ),
        )
        val viewModel = launchSuggestions(
            pantry = pantryWith(PantryItem("manzana", "Manzana", 3)),
            suggester = suggester,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).performClick()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionDiscard(1)).performScrollTo().performClick()

        composeRule.onNodeWithText("Aceptada").assertIsDisplayed()
        composeRule.onNodeWithText("Descartada").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(2))
            .performScrollTo()
            .assertIsEnabled()

        composeRule.runOnIdle { viewModel.retry() }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.suggestionState(0))
                .fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.suggestionState(1))
                .fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).assertIsEnabled()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionDiscard(1))
            .performScrollTo()
            .assertIsEnabled()
    }

    // S6: acciones, estados y aviso en inglés.
    @Test
    fun acceptingAndDiscardingAreLocalizedInEnglish() {
        val suggester = FakeRecipeSuggester(
            listOf(RecipeSuggestionsResult.Success(listOf(suggestion(title = "Apple tart")))),
        )
        launchSuggestions(
            pantry = pantryWith(PantryItem("apple", "Apple", 3)),
            suggester = suggester,
            locale = Locale.ENGLISH,
        )

        composeRule.onNodeWithTag(AiSuggestionsTestTags.REQUEST).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(AiSuggestionsTestTags.RESULT_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Accept").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").assertIsDisplayed()

        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).performClick()

        composeRule.onNodeWithText("Saved to My recipe book").assertIsDisplayed()
        composeRule.onNodeWithText("Accepted").assertIsDisplayed()
        composeRule.onNodeWithTag(AiSuggestionsTestTags.suggestionAccept(0)).assertIsNotEnabled()
    }

    private fun launchSuggestions(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        holder: ScanDraftHolder = ScanDraftHolder(),
        suggester: RecipeSuggester = FakeRecipeSuggester(emptyList()),
        recipes: InMemoryRecipeRepository = InMemoryRecipeRepository(),
        locale: Locale = Locale.forLanguageTag("es-ES"),
        onOpenSettings: () -> Unit = {},
    ): AiSuggestionsViewModel {
        val viewModel = AiSuggestionsViewModel(pantry, holder, suggester, recipes)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    val language = localeContext.resources.configuration.locales[0].language
                    AiSuggestionsScreen(
                        uiState = state,
                        onBack = {},
                        onOpenSettings = onOpenSettings,
                        onToggleFood = viewModel::toggleFood,
                        onRequest = { viewModel.requestSuggestions(language) },
                        onRetry = viewModel::retry,
                        onAccept = viewModel::acceptSuggestion,
                        onDiscard = viewModel::discardSuggestion,
                        onClearNotice = viewModel::clearNotice,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun storedRecipe(title: String, required: List<String>): Recipe = Recipe(
        id = title,
        title = title,
        ingredients = required.map { RecipeIngredient(name = it, quantity = 1.0) },
        steps = listOf(RecipeStep(text = "Cocina")),
    )

    private fun suggestion(
        title: String,
        required: List<String> = listOf("Manzana"),
        steps: List<RecipeStep> = listOf(RecipeStep(text = "Cocina la receta")),
    ) = RecipeSuggestion(
        title = title,
        story = "Una receta propuesta por la IA",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = required.mapIndexed { index, name ->
            RecipeIngredient(
                id = "$index",
                name = name,
                quantity = 1.0,
                unit = "ud",
            )
        },
        steps = steps,
    )

    private class FakeRecipeSuggester(
        private val results: List<RecipeSuggestionsResult>,
    ) : RecipeSuggester {
        var calls: Int = 0
            private set
        var lastFoods: List<SuggestableFood> = emptyList()
            private set
        var lastAvoidance: List<RecipeAvoidance> = emptyList()
            private set

        override suspend fun suggest(
            foods: List<SuggestableFood>,
            language: String,
            existingRecipes: List<RecipeAvoidance>,
        ): RecipeSuggestionsResult {
            lastFoods = foods
            lastAvoidance = existingRecipes
            if (results.isEmpty()) return RecipeSuggestionsResult.Success(emptyList())
            return results[minOf(calls++, results.lastIndex)]
        }
    }

    private object AlwaysFailingScanAnalyzer : ScanImageAnalyzer {
        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult =
            ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)
    }
}
