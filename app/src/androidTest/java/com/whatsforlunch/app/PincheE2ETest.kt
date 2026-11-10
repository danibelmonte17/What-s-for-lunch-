package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.nav.AppBottomBar
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.nav.AppTab
import com.whatsforlunch.app.ui.pinche.PincheScreen
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import com.whatsforlunch.app.ui.pinche.PincheViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// S1-S10 del Pinche: texto conversacional, Ficha con Faltantes, guardado como
// Receta en Mis Recetas (sin foto y sin distintivo de estado), fallos visibles,
// inglés y entrada desde Mis Recetas.
// Todo usa un cliente simulado; nunca se llama a la IA real.
@RunWith(AndroidJUnit4::class)
class PincheE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // S1 (verify 1): una duda de cocina se responde solo con texto, sin Ficha.
    @Test
    fun cookingQuestionIsAnsweredWithTextOnly() {
        val viewModel = launchPinche(
            client = FakePincheChatClient(listOf(success("Remueve el arroz al principio."))),
        )

        composeRule.onNodeWithText("Hola, soy el Pinche. Pregúntame lo que quieras de cocina.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("¿Cómo evito que el arroz se pegue?")
        composeRule.onNodeWithTag(PincheTestTags.SEND).assertIsEnabled().performClick()
        dismissKeyboard()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.userMessage(0)).assertIsDisplayed()
        composeRule.onNodeWithText("Remueve el arroz al principio.").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithTag(PincheTestTags.proposalCard(1))
                .fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(1, viewModel.uiState.value.messages.count { it.text.isNotBlank() && it.text.startsWith("Remueve") })
    }

    // S2 (verify 1): respuesta con una Propuesta, ingredientes, pasos y Faltantes.
    @Test
    fun answerWithProposalShowsTheCardWithIngredientsStepsAndMissingItems() {
        launchPinche(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            client = FakePincheChatClient(listOf(success("Prueba esta receta.", proposal()))),
        )

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta con lo que tengo")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.proposalCard(1))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.PROPOSAL_TITLE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Arroz con tomate").assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.PROPOSAL_INGREDIENTS).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.PROPOSAL_STEPS).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.PROPOSAL_MISSING).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("• Harina").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("• Tomate").fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithText("• Albahaca").fetchSemanticsNodes().isEmpty(),
        )
    }

    // S3 (verify 2): guardar crea una Receta visible en Mis Recetas y un segundo
    // guardado no duplica la Receta.
    @Test
    fun savingTheProposalCreatesARecipeInMyRecipesWithoutDuplicates() {
        val recipes = InMemoryRecipeRepository()
        val pantry = pantryWith(PantryItem("tomate", "Tomate", 3))
        val pincheViewModel = PincheViewModel(
            pantry,
            recipes,
            FakePincheChatClient(listOf(success("Prueba esta receta.", proposal()))),
            InMemoryShoppingListRepository(),
            completeProfileRepository(),
        )
        val myRecipesViewModel = MyRecipesViewModel(recipes, pantry)
        val showPinche = mutableStateOf(true)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val pincheState by pincheViewModel.uiState.collectAsState()
            val myRecipesState by myRecipesViewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    if (showPinche.value) {
                        PincheScreen(
                            uiState = pincheState,
                            onOpenSettings = {},
                            onInputChange = pincheViewModel::onInputChange,
                            onSend = { pincheViewModel.send("es") },
                            onQuickSuggestion = { pincheViewModel.sendQuickSuggestion(it, "es") },
                            onRetry = { pincheViewModel.retry("es") },
                            onSaveProposal = pincheViewModel::saveProposal,
                            onClearNotice = pincheViewModel::clearNotice,
                        )
                    } else {
                        MyRecipesScreen(
                            uiState = myRecipesState,
                            onCreateRecipe = {},
                            onScanPantry = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        dismissKeyboard()

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.SAVE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.SAVE).performScrollTo().assertIsEnabled().performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.SAVED_LABEL).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.SAVED_LABEL).assertIsNotEnabled()

        val id = PincheProposalRules.proposalRecipeId("Arroz con tomate")
        composeRule.waitUntil(5_000) { runBlocking { recipes.getRecipe(id) } != null }
        val saved = runBlocking { recipes.getRecipe(id) }!!
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertNull(saved.photoPath)

        composeRule.runOnIdle { pincheViewModel.saveProposal(pincheViewModel.uiState.value.messages.last().id) }
        composeRule.waitForIdle()
        assertEquals(1, runBlocking { recipes.observeRecipes().first() }.size)

        showPinche.value = false
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Arroz con tomate").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Arroz con tomate").assertIsDisplayed()
        // S5/RF-EBF6: sin ningún distintivo de estado.
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
        composeRule.onAllNodesWithTag("my_recipes_card_status").assertCountEquals(0)
    }

    // S4: con la Despensa vacía todos los obligatorios son Faltantes y guardar sigue habilitado.
    @Test
    fun emptyPantryShowsEveryRequiredIngredientAsMissing() {
        launchPinche(
            client = FakePincheChatClient(
                listOf(success("Prueba esta receta.", proposal(required = listOf("Tomate", "Harina")))),
            ),
        )

        composeRule.onNodeWithTag(PincheTestTags.QUICK_SUGGESTION).assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.quickSuggestion(2)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.PROPOSAL_MISSING).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("• Tomate").assertIsDisplayed()
        composeRule.onNodeWithText("• Harina").assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.SAVE).performScrollTo().assertIsEnabled()
    }

    @Test
    fun chipSendsItsQuestionAndHidesOnceTheConversationStarts() {
        val client = FakePincheChatClient(listOf(success("Con lo que tienes, haz una tortilla.")))
        val viewModel = viewModel(client = client)

        launch(viewModel = viewModel)
        composeRule.onNodeWithTag(PincheTestTags.quickSuggestion(0)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1)).fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(listOf("¿Qué ceno con lo que tengo?"), client.lastTurns.map { it.text })
        assertTrue(
            composeRule.onAllNodesWithTag(PincheTestTags.QUICK_SUGGESTION).fetchSemanticsNodes().isEmpty(),
        )
    }

    // S5: una Propuesta inválida deja solo el texto, sin Ficha ni guardar.
    @Test
    fun answerWithoutAValidProposalShowsOnlyText() {
        launchPinche(
            client = FakePincheChatClient(listOf(success("No tengo una receta válida para ti."))),
        )

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("No tengo una receta válida para ti.").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag(PincheTestTags.SAVE).fetchSemanticsNodes().isEmpty())
    }

    // S6 (verify 3): sin key se muestra el aviso y «Abrir Ajustes» navega.
    @Test
    fun missingApiKeyShowsNoticeAndOpensSettings() {
        var settingsOpened = false
        launchPinche(
            client = FakePincheChatClient(listOf(PincheReplyResult.MissingApiKey)),
            onOpenSettings = { settingsOpened = true },
        )

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Hola")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.NOTICE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Configura tu API key en Ajustes para hablar con el Pinche.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.OPEN_SETTINGS).performClick()
        assertTrue(settingsOpened)
    }

    // S7 (verify 3): fallo del proveedor y «Reintentar» con éxito sin duplicar la burbuja.
    @Test
    fun failureShowsRetryAndRetryingDoesNotDuplicateTheUserMessage() {
        val client = FakePincheChatClient(
            listOf(PincheReplyResult.Failed, success("Ahora sí puedo ayudarte.")),
        )
        launchPinche(client = client)

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Hola")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.RETRY).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.RETRY).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(1)).fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(
            composeRule.onAllNodesWithTag(PincheTestTags.NOTICE).fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithTag(PincheTestTags.userMessage(1)).fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithText("Ahora sí puedo ayudarte.").assertIsDisplayed()
        assertEquals(2, client.calls)
    }

    // S8 (entrada): desde Mis Recetas se abre el Pinche y al volver se conservan
    // búsqueda y filtro.
    @Test
    fun pincheOpensFromMyRecipesAndKeepsSearchAndFilterOnBack() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "1",
                    title = "Arroz con tomate",
                    mealMoment = MealMoment.LUNCH,
                    ingredients = listOf(RecipeIngredient(name = "Arroz")),
                    steps = listOf(RecipeStep(text = "Cocina")),
                ),
            )
        }
        val myRecipesViewModel = MyRecipesViewModel(recipes, pantry)
        val pincheViewModel = PincheViewModel(
            pantry,
            recipes,
            FakePincheChatClient(listOf(success("Hola"))),
            InMemoryShoppingListRepository(),
            completeProfileRepository(),
        )
        val showPinche = mutableStateOf(false)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val localeContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val myRecipesState by myRecipesViewModel.uiState.collectAsState()
            val pincheState by pincheViewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    // La barra vive fuera de las pantallas (extraída a
                    // WhatsForLunchApp); el test la monta aquí para seguir
                    // navegando por pestañas como lo hace la app.
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            if (showPinche.value) {
                                PincheScreen(
                                    uiState = pincheState,
                                    onOpenSettings = {},
                                    onInputChange = pincheViewModel::onInputChange,
                                    onSend = { pincheViewModel.send("es") },
                                    onQuickSuggestion = { pincheViewModel.sendQuickSuggestion(it, "es") },
                                    onRetry = { pincheViewModel.retry("es") },
                                    onSaveProposal = pincheViewModel::saveProposal,
                                    onClearNotice = pincheViewModel::clearNotice,
                                )
                            } else {
                                MyRecipesScreen(
                                    uiState = myRecipesState,
                                    onCreateRecipe = {},
                                    onScanPantry = {},
                                    onSearchQueryChanged = myRecipesViewModel::onSearchQueryChanged,
                                    onMealMomentSelected = myRecipesViewModel::onMealMomentSelected,
                                )
                            }
                        }
                        AppBottomBar(
                            selectedTab = if (showPinche.value) AppTab.PINCHE else AppTab.MY_RECIPES,
                            onTabSelected = { tab -> showPinche.value = tab == AppTab.PINCHE },
                            onReselect = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).performTextInput("Arroz")
        composeRule.onNodeWithTag(MyRecipesTestTags.filterChip(MealMoment.LUNCH)).performClick()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        dismissKeyboard()

        composeRule.onNodeWithTag(PincheTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Hola, soy el Pinche. Pregúntame lo que quieras de cocina.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        assertEquals("Arroz", myRecipesViewModel.uiState.value.searchQuery)
        assertEquals(MealMoment.LUNCH, myRecipesViewModel.uiState.value.mealMomentFilter)
    }

    // S9: fallo de guardado con aviso y botón otra vez habilitado.
    @Test
    fun failedSaveShowsNoticeAndEnablesTheButtonAgain() {
        launchPinche(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            recipes = ThrowingRecipeRepository(),
            client = FakePincheChatClient(listOf(success("Prueba esta receta.", proposal()))),
        )

        composeRule.onNodeWithTag(PincheTestTags.QUICK_SUGGESTION).assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.quickSuggestion(0)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.SAVE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PincheTestTags.SAVE).performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.NOTICE).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("No se pudo guardar la receta. Inténtalo de nuevo.").assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.SAVE).performScrollTo().assertIsEnabled()
    }

    // S10: saludo, chip, compositor, avisos y Ficha en inglés.
    @Test
    fun pincheIsLocalizedInEnglish() {
        launchPinche(
            pantry = pantryWith(PantryItem("tomato", "Tomato", 3)),
            client = FakePincheChatClient(listOf(success("Try this recipe.", proposal(title = "Tomato rice")))),
            locale = Locale.ENGLISH,
        )

        composeRule.onNodeWithText("Hi, I’m the Pinche. Ask me anything about cooking.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("What can I cook with what I have?").assertIsDisplayed()
        composeRule.onNodeWithText("Send").assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.quickSuggestion(0)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.proposalCard(1)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pinche proposal").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ingredients").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save to My Recipes").performScrollTo().assertIsDisplayed()
    }

    // verify 4 (`crear-mi-perfil`, S4/RD26): con el perfil vacío el Pinche pide
    // las tres partes pendientes sin llamar a la IA y, con el perfil completo,
    // responde; el mensaje lleva la acción «Abrir Mi perfil» (D8).
    @Test
    fun emptyProfileAsksForItWithoutCallingTheAiAndAnswersOnceComplete() {
        val client = FakePincheChatClient(listOf(success("Vegana y sin frutos secos.")))
        val profile = InMemoryUserProfileRepository()
        launchPinche(client = client, profile = profile)

        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.OPEN_PROFILE).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, client.calls)
        composeRule.onNodeWithText(
            "Para responder necesito tu perfil: Alergias, " +
                "Intolerancias o enfermedades que limitan la dieta, Tipo de alimentación. " +
                "Complétalo en «Mi perfil».",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(PincheTestTags.OPEN_PROFILE).assertIsDisplayed()
        // La respuesta de la IA todavía no llegó.
        assertTrue(
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(3)).fetchSemanticsNodes().isEmpty(),
        )

        // Se completa el perfil y se vuelve a enviar: ahora sí responde la IA.
        runBlocking {
            profile.save(
                UserProfile(
                    allergies = listOf("Frutos secos"),
                    intolerances = listOf("Diabetes"),
                    dietType = DietType.VEGAN,
                ),
            )
        }
        composeRule.onNodeWithTag(PincheTestTags.COMPOSER).performTextInput("Dame una receta")
        composeRule.onNodeWithTag(PincheTestTags.SEND).performClick()
        dismissKeyboard()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(PincheTestTags.assistantMessage(3)).fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(1, client.calls)
        assertEquals(DietType.VEGAN, client.lastProfile?.dietType)
        composeRule.onNodeWithText("Vegana y sin frutos secos.").assertIsDisplayed()
    }

    private fun dismissKeyboard() {
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
    }

    private fun launchPinche(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        recipes: RecipeRepository = InMemoryRecipeRepository(),
        client: FakePincheChatClient = FakePincheChatClient(emptyList()),
        locale: Locale = Locale("es", "ES"),
        onOpenSettings: () -> Unit = {},
        profile: UserProfileRepository = completeProfileRepository(),
    ): PincheViewModel {
        val viewModel = viewModel(pantry = pantry, recipes = recipes, client = client, profile = profile)
        launch(viewModel = viewModel, locale = locale, onOpenSettings = onOpenSettings)
        return viewModel
    }

    private fun viewModel(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        recipes: RecipeRepository = InMemoryRecipeRepository(),
        client: FakePincheChatClient = FakePincheChatClient(emptyList()),
        profile: UserProfileRepository = completeProfileRepository(),
    ) = PincheViewModel(pantry, recipes, client, InMemoryShoppingListRepository(), profile)

    // Perfil completo para los escenarios que esperan respuesta de la IA (RD26).
    private fun completeProfileRepository() = InMemoryUserProfileRepository(
        UserProfile(
            allergies = listOf("Frutos secos"),
            intolerancesNone = true,
            dietType = DietType.VEGAN,
        ),
    )

    private fun launch(
        viewModel: PincheViewModel,
        locale: Locale = Locale("es", "ES"),
        onOpenSettings: () -> Unit = {},
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        val localeContext = baseContext.createConfigurationContext(configuration)
        val language = localeContext.resources.configuration.locales[0].language

        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    PincheScreen(
                        uiState = state,
                        onOpenSettings = onOpenSettings,
                        onInputChange = viewModel::onInputChange,
                        onSend = { viewModel.send(language) },
                        onQuickSuggestion = { text -> viewModel.sendQuickSuggestion(text, language) },
                        onRetry = { viewModel.retry(language) },
                        onSaveProposal = viewModel::saveProposal,
                        onClearNotice = viewModel::clearNotice,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        dismissKeyboard()
    }

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun success(text: String, proposal: RecipeSuggestion? = null): PincheReplyResult.Success =
        PincheReplyResult.Success(PincheReply(text = text, proposal = proposal))

    private fun proposal(
        title: String = "Arroz con tomate",
        required: List<String> = listOf("Tomate", "Harina"),
    ): RecipeSuggestion = RecipeSuggestion(
        title = title,
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = required.mapIndexed { index, name ->
            RecipeIngredient(id = "$index", name = name, quantity = 1.0, unit = "ud")
        },
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )

    private class FakePincheChatClient(
        private val results: List<PincheReplyResult>,
    ) : PincheChatClient {
        var calls: Int = 0
            private set
        var lastTurns: List<PincheTurn> = emptyList()
            private set
        var lastProfile: UserProfile? = null
            private set

        override suspend fun reply(
            turns: List<PincheTurn>,
            language: String,
            profile: UserProfile,
        ): PincheReplyResult {
            lastTurns = turns
            lastProfile = profile
            if (results.isEmpty()) return PincheReplyResult.Success(PincheReply(text = "ok"))
            return results[minOf(calls++, results.lastIndex)]
        }
    }

    private class ThrowingRecipeRepository : RecipeRepository {
        private val recipes = MutableStateFlow<List<Recipe>>(emptyList())

        override fun observeRecipes(): Flow<List<Recipe>> = recipes.asStateFlow()

        override suspend fun getRecipe(id: String): Recipe? = null

        override suspend fun saveRecipe(recipe: Recipe) {
            throw IllegalStateException("save failed")
        }

        override suspend fun deleteRecipe(id: String) = Unit
    }
}
