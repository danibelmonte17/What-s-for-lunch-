package com.whatsforlunch.app.ui.pinche

import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheProposalState
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.domain.profile.SavedProfilePart
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PincheViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendAddsTheUserBubbleAndTheAssistantAnswer() = runTest {
        val client = FakePincheChatClient(listOf(success("Remueve el arroz al principio.")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("  ¿Cómo evito que se pegue?  ")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(PincheAuthor.USER, state.messages[0].author)
        assertEquals("¿Cómo evito que se pegue?", state.messages[0].text)
        assertEquals(PincheAuthor.ASSISTANT, state.messages[1].author)
        assertEquals("Remueve el arroz al principio.", state.messages[1].text)
        assertEquals("", state.input)
        assertFalse(state.isSending)
        assertEquals(1, client.calls)
        assertEquals(listOf("¿Cómo evito que se pegue?"), client.lastTurns.map { it.text })
    }

    @Test
    fun blankInputIsIgnoredAndDoesNotCallTheClient() = runTest {
        val client = FakePincheChatClient(listOf(success("hola")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("   ")
        viewModel.send("es")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(0, client.calls)
    }

    @Test
    fun inputAndSendAreTruncatedToFiveHundredCharacters() = runTest {
        val client = FakePincheChatClient(listOf(success("ok")))
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("a".repeat(600))
        assertEquals(500, viewModel.uiState.value.input.length)

        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(500, client.lastTurns.single().text.length)
    }

    @Test
    fun missingApiKeyShowsTheKeyNotice() = runTest {
        val viewModel = viewModel(client = FakePincheChatClient(listOf(PincheReplyResult.MissingApiKey)))

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(PincheNotice.AI_KEY_MISSING, viewModel.uiState.value.notice)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun failedTurnShowsNoticeAndRetryDoesNotDuplicateTheUserBubble() = runTest {
        val client = FakePincheChatClient(
            listOf(PincheReplyResult.Failed, success("Ahora sí.")),
        )
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()
        assertEquals(PincheNotice.FAILED, viewModel.uiState.value.notice)

        viewModel.retry("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.notice)
        assertEquals(2, state.messages.size)
        assertEquals(1, state.messages.count { it.author == PincheAuthor.USER })
        assertEquals("Ahora sí.", state.messages.last().text)
        assertEquals(2, client.calls)
    }

    @Test
    fun successWithProposalComputesMissingRequiredIngredientsFromThePantry() = runTest {
        val pantry = pantryWith(PantryItem("1", "Tomate", 3))
        val viewModel = viewModel(
            pantry = pantry,
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        val proposal = viewModel.uiState.value.messages.last().proposal
        assertEquals(listOf("Harina"), proposal?.missingIngredients)
    }

    @Test
    fun savingTheProposalStoresADraftAndMarksTheCardAsSaved() = runTest {
        val recipes = InMemoryRecipeRepository()
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            recipes = recipes,
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()
        viewModel.saveProposal(message.id)
        advanceUntilIdle()

        assertEquals(PincheProposalState.SAVED, viewModel.uiState.value.messages.last().proposalState)
        val saved = runBlocking { recipes.observeRecipes().first() }.single()
        assertEquals(PincheProposalRules.proposalRecipeId("Arroz con tomate"), saved.id)
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertNull(saved.photoPath)

        viewModel.saveProposal(message.id)
        advanceUntilIdle()
        assertEquals(1, runBlocking { recipes.observeRecipes().first() }.size)
    }

    @Test
    fun failedSaveRevertsTheCardAndShowsTheSaveNotice() = runTest {
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            recipes = ThrowingRecipeRepository(),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()
        viewModel.saveProposal(message.id)
        advanceUntilIdle()

        assertEquals(PincheProposalState.PROPOSED, viewModel.uiState.value.messages.last().proposalState)
        assertEquals(PincheNotice.SAVE_FAILED, viewModel.uiState.value.notice)
    }

    @Test
    fun missingIngredientsAreNotRecalculatedWhenThePantryChanges() {
        val pantry = InMemoryPantryRepository()
        runBlocking { pantry.savePantryItem(PantryItem("1", "Tomate", 1)) }
        val viewModel = PincheViewModel(
            pantryRepository = pantry,
            recipeRepository = InMemoryRecipeRepository(),
            pincheChatClient = FakePincheChatClient(
                listOf(success("Prueba esto.", proposal())),
            ),
            shoppingListRepository = InMemoryShoppingListRepository(),
            userProfileRepository = completeProfileRepository(),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        runBlocking { pantry.savePantryItem(PantryItem("2", "Harina", 1)) }

        assertEquals(listOf("Harina"), viewModel.uiState.value.messages.last().proposal?.missingIngredients)
    }

    // RF-AFL4/D8/D9: la Ficha aporta las cantidades base con la clave de fuente de
    // la Propuesta (RD16) y queda marcada como añadida sin duplicar.
    @Test
    fun addingMissingUsesBaseQuantitiesAndMarksTheProposalAsAdded() = runTest {
        val shoppingList = InMemoryShoppingListRepository()
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
            shoppingList = shoppingList,
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.last().addedToList)
        val item = shoppingList.observeItems().first().single()
        assertEquals("Harina", item.name)
        assertEquals(1.0, item.quantity!!, 0.0001)
        assertEquals("ud", item.unit)
        assertEquals(
            listOf(PincheProposalRules.proposalRecipeId("Arroz con tomate")),
            item.sources.map { it.sourceKey },
        )

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()
        assertEquals(1, shoppingList.observeItems().first().size)
    }

    @Test
    fun failedShoppingListAddRevertsTheCardAndShowsTheNotice() = runTest {
        val viewModel = viewModel(
            pantry = pantryWith(PantryItem("tomate", "Tomate", 3)),
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
            shoppingList = ThrowingShoppingListRepository(),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()
        val message = viewModel.uiState.value.messages.last()

        viewModel.addMissingToShoppingList(message.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.messages.last().addedToList)
        assertEquals(PincheNotice.SHOPPING_LIST_FAILED, viewModel.uiState.value.notice)
    }

    @Test
    fun quickSuggestionsOnlyWorkWithAnEmptyConversation() = runTest {
        val client = FakePincheChatClient(listOf(success("Hola"), success("Otra vez")))
        val viewModel = viewModel(client = client)

        viewModel.sendQuickSuggestion("¿Qué ceno?", "es")
        advanceUntilIdle()
        assertEquals(listOf("¿Qué ceno?"), client.lastTurns.map { it.text })

        viewModel.sendQuickSuggestion("Ignorada", "es")
        advanceUntilIdle()
        assertEquals(1, client.calls)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun successWithoutTextAndProposalIsTreatedAsFailure() = runTest {
        val viewModel = viewModel(client = FakePincheChatClient(listOf(success("   "))))

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(PincheNotice.FAILED, viewModel.uiState.value.notice)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    // RD26/RD27 (`rellenar-perfil-con-el-pinche`): el gate previo se retiró —con
    // el perfil vacío también se llama a la IA— y la entrevista vive en el prompt.
    @Test
    fun emptyProfileStillCallsTheClientAndShowsTheAnswer() = runTest {
        val client = FakePincheChatClient(listOf(success("¿Tienes alguna alergia?")))
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(1, client.calls)
        assertEquals(UserProfile.empty, client.lastProfile)
        val state = viewModel.uiState.value
        assertFalse(state.isSending)
        assertEquals(2, state.messages.size)
        assertEquals(PincheAuthor.USER, state.messages[0].author)
        assertEquals("¿Tienes alguna alergia?", state.messages.last().text)
        assertTrue(state.messages.last().profileRequest.isEmpty())
        assertNull(state.messages.last().proposal)
    }

    // D1 (guarda determinista): una Propuesta con partes pendientes no se pinta;
    // se conserva el texto y se añade el mensaje estático con «Abrir Mi perfil».
    @Test
    fun proposalWithPendingPartsIsNotPaintedAndAddsTheStaticRequest() = runTest {
        val client = FakePincheChatClient(listOf(success("Te propongo esto.", proposal())))
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        val message = viewModel.uiState.value.messages.last()
        assertEquals("Te propongo esto.", message.text)
        assertNull(message.proposal)
        assertEquals(
            listOf(
                PendingProfilePart.ALLERGIES,
                PendingProfilePart.INTOLERANCES,
                PendingProfilePart.DIET_TYPE,
            ),
            message.profileRequest,
        )
    }

    // Con el perfil completo la Ficha sí se pinta (el resto de tests lo cubren).
    @Test
    fun proposalWithCompleteProfileShowsTheCard() = runTest {
        val viewModel = viewModel(
            client = FakePincheChatClient(listOf(success("Prueba esto.", proposal()))),
        )

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        val message = viewModel.uiState.value.messages.last()
        assertNotNull(message.proposal)
        assertTrue(message.profileRequest.isEmpty())
    }

    // D2: el resumen «He guardado…» se añade antes de la respuesta con lo
    // realmente persistido en el turno.
    @Test
    fun profileSavedAddsTheSummaryBeforeTheReply() = runTest {
        val saved = listOf(SavedProfilePart(PendingProfilePart.ALLERGIES, chips = listOf("Frutos secos")))
        val client = FakePincheChatClient(
            listOf(success("Anotado. ¿Tienes intolerancias?", saved = saved)),
        )
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Frutos secos")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        val summary = state.messages[1]
        assertEquals(PincheAuthor.ASSISTANT, summary.author)
        assertEquals("", summary.text)
        assertEquals(saved, summary.profileSaved)
        assertEquals("Anotado. ¿Tienes intolerancias?", state.messages[2].text)
        // El resumen no viaja al modelo (texto en blanco).
        assertEquals(listOf("Frutos secos"), client.lastTurns.map { it.text })
    }

    // Enmienda D9/D14: el texto del asistente llega en fragmentos que se pintan
    // en la burbuja transitoria (en blanco = sin burbuja) hasta el mensaje final.
    @Test
    fun streamingFragmentsUpdateTheTransientBubbleUntilTheFinalReply() = runTest {
        lateinit var holder: PincheViewModel
        val seen = mutableListOf<String?>()
        val client = FakePincheChatClient(
            listOf(success("Remueve el arroz.")),
            // El primer fragmento en blanco simula el reset de ronda del cliente.
            fragments = listOf("", "Remueve", " el arroz"),
            onFragment = { seen += holder.uiState.value.streamingText },
        )
        holder = viewModel(client = client)

        holder.onInputChange("¿Cómo evito que se pegue?")
        holder.send("es")
        advanceUntilIdle()

        assertEquals(listOf(null, "Remueve", "Remueve el arroz"), seen)
        val state = holder.uiState.value
        // La burbuja transitoria desaparece al llegar el mensaje final (D9).
        assertNull(state.streamingText)
        assertFalse(state.isSending)
        assertEquals(2, state.messages.size)
        assertEquals("Remueve el arroz.", state.messages[1].text)
    }

    // Enmienda D11: si el stream se corta a mitad, el texto recibido se conserva
    // como burbuja del asistente, antes del mensaje estático de partes pendientes.
    @Test
    fun failedStreamKeepsThePartialTextAsAnAssistantBubble() = runTest {
        val client = FakePincheChatClient(
            listOf(PincheReplyResult.Failed),
            fragments = listOf("Esto se corta a mitad"),
        )
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PincheNotice.FAILED, state.notice)
        assertNull(state.streamingText)
        assertEquals(3, state.messages.size)
        assertEquals(PincheAuthor.ASSISTANT, state.messages[1].author)
        assertEquals("Esto se corta a mitad", state.messages[1].text)
        assertTrue(state.messages[2].profileRequest.isNotEmpty())
    }

    // Enmienda D11: el parcial congelado viaja en el contexto del reintento.
    @Test
    fun theKeptPartialTextTravelsInTheRetryContext() = runTest {
        val client = FakePincheChatClient(
            listOf(PincheReplyResult.Failed, success("Ahora sí.")),
            fragments = listOf("Esto se corta"),
        )
        val viewModel = viewModel(client = client)

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()
        viewModel.retry("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.streamingText)
        assertNull(state.notice)
        assertEquals(listOf("Hola", "Esto se corta"), client.lastTurns.map { it.text })
        assertEquals("Ahora sí.", state.messages.last().text)
    }

    // S5/D7: sin key con partes pendientes, además del aviso se añade el mensaje
    // estático con «Abrir Mi perfil» (degradación).
    @Test
    fun missingApiKeyWithPendingPartsAddsTheStaticRequest() = runTest {
        val client = FakePincheChatClient(listOf(PincheReplyResult.MissingApiKey))
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PincheNotice.AI_KEY_MISSING, state.notice)
        assertEquals(2, state.messages.size)
        assertEquals("", state.messages.last().text)
        assertEquals(
            listOf(
                PendingProfilePart.ALLERGIES,
                PendingProfilePart.INTOLERANCES,
                PendingProfilePart.DIET_TYPE,
            ),
            state.messages.last().profileRequest,
        )
    }

    // S5/D7: fallo del proveedor con partes pendientes → aviso + mensaje estático.
    @Test
    fun failedTurnWithPendingPartsAddsTheStaticRequest() = runTest {
        val client = FakePincheChatClient(listOf(PincheReplyResult.Failed))
        val viewModel = viewModel(client = client, profile = InMemoryUserProfileRepository())

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PincheNotice.FAILED, state.notice)
        assertEquals(2, state.messages.size)
        assertTrue(state.messages.last().profileRequest.isNotEmpty())
    }

    // Con el perfil completo el aviso no añade ningún mensaje extra.
    @Test
    fun missingApiKeyWithCompleteProfileKeepsOnlyTheNotice() = runTest {
        val viewModel = viewModel(client = FakePincheChatClient(listOf(PincheReplyResult.MissingApiKey)))

        viewModel.onInputChange("Hola")
        viewModel.send("es")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PincheNotice.AI_KEY_MISSING, state.notice)
        assertEquals(1, state.messages.size)
        assertTrue(state.messages.none { it.profileRequest.isNotEmpty() })
    }

    // D5: con el perfil completo la llamada lleva el perfil en el puerto.
    @Test
    fun completeProfileIsSentToTheClient() = runTest {
        val client = FakePincheChatClient(listOf(success("Vegana y sin frutos secos.")))
        val profile = InMemoryUserProfileRepository(
            UserProfile(
                allergies = listOf("Frutos secos"),
                intolerances = listOf("Diabetes"),
                dietType = DietType.VEGAN,
            ),
        )
        val viewModel = viewModel(client = client, profile = profile)

        viewModel.onInputChange("Dame una receta")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(1, client.calls)
        assertEquals(
            UserProfile(allergies = listOf("Frutos secos"), intolerances = listOf("Diabetes"), dietType = DietType.VEGAN),
            client.lastProfile,
        )
        assertEquals("Vegana y sin frutos secos.", viewModel.uiState.value.messages.last().text)
        assertTrue(viewModel.uiState.value.messages.last().profileRequest.isEmpty())
    }

    // Los mensajes estáticos (la guarda con la Propuesta y el resumen del turno)
    // tienen texto en blanco y nunca son turnos del modelo.
    @Test
    fun staticProfileMessagesNeverTravelToTheModel() = runTest {
        val client = FakePincheChatClient(
            listOf(
                success("Te propongo esto.", proposal()),
                success("Ahora sí."),
            ),
        )
        val profile = InMemoryUserProfileRepository()
        val viewModel = viewModel(client = client, profile = profile)

        viewModel.onInputChange("Primera pregunta")
        viewModel.send("es")
        advanceUntilIdle()
        // La guarda añadió un mensaje estático (texto en blanco).
        assertTrue(viewModel.uiState.value.messages.any { it.profileRequest.isNotEmpty() })

        // Se completa el perfil y se vuelve a enviar.
        profile.save(
            UserProfile(allergies = listOf("Frutos secos"), intolerancesNone = true, dietType = DietType.OMNIVORE),
        )
        viewModel.onInputChange("Segunda pregunta")
        viewModel.send("es")
        advanceUntilIdle()

        assertEquals(2, client.calls)
        assertEquals(
            listOf("Primera pregunta", "Te propongo esto.", "Segunda pregunta"),
            client.lastTurns.map { it.text },
        )
        assertTrue(client.lastTurns.none { it.text.isBlank() })
    }

    private fun viewModel(
        pantry: InMemoryPantryRepository = InMemoryPantryRepository(),
        recipes: RecipeRepository = InMemoryRecipeRepository(),
        client: FakePincheChatClient = FakePincheChatClient(emptyList()),
        shoppingList: ShoppingListRepository = InMemoryShoppingListRepository(),
        profile: UserProfileRepository = completeProfileRepository(),
    ) = PincheViewModel(pantry, recipes, client, shoppingList, profile)

    /** Perfil completo para los tests que sí esperan respuesta de la IA (RD26). */
    private fun completeProfileRepository() = InMemoryUserProfileRepository(
        UserProfile(
            allergies = listOf("Frutos secos"),
            intolerancesNone = true,
            dietType = DietType.VEGAN,
        ),
    )

    private fun pantryWith(vararg items: PantryItem): InMemoryPantryRepository =
        InMemoryPantryRepository().apply {
            runBlocking { items.forEach { savePantryItem(it) } }
        }

    private fun success(
        text: String,
        proposal: RecipeSuggestion? = null,
        saved: List<SavedProfilePart>? = null,
    ): PincheReplyResult.Success =
        PincheReplyResult.Success(PincheReply(text = text, proposal = proposal), profileSaved = saved)

    private fun proposal(): RecipeSuggestion = RecipeSuggestion(
        title = "Arroz con tomate",
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = listOf(
            RecipeIngredient(id = "0", name = "Tomate", quantity = 1.0, unit = "ud"),
            RecipeIngredient(id = "1", name = "Harina", quantity = 1.0, unit = "ud"),
            RecipeIngredient(id = "2", name = "Albahaca", quantity = 1.0, unit = "ud", optional = true),
        ),
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )

    private class FakePincheChatClient(
        private val results: List<PincheReplyResult>,
        // Enmienda D9: fragmentos (deltas) que el «proveedor» emite en streaming,
        // **acumulados** como hace el cliente real antes de llamar a `onContent`;
        // `onFragment` deja observar el estado entre emisiones.
        private val fragments: List<String> = emptyList(),
        private val onFragment: (String) -> Unit = {},
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
            onContent: (String) -> Unit,
        ): PincheReplyResult {
            lastTurns = turns
            lastProfile = profile
            var accumulated = ""
            fragments.forEach { fragment ->
                accumulated += fragment
                // Como el cliente real: el texto visible va recortado.
                onContent(accumulated.trim())
                onFragment(accumulated.trim())
            }
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

    private class ThrowingShoppingListRepository : ShoppingListRepository {
        override fun observeItems(): Flow<List<ShoppingListItem>> = emptyFlow()

        override suspend fun addMissing(
            sourceKey: String,
            sourceTitle: String,
            ingredients: List<RecipeIngredient>,
        ) {
            throw IllegalStateException("shopping list failed")
        }

        override suspend fun saveManualItem(
            id: String?,
            name: String,
            quantity: Double?,
            unit: String,
            aisle: Aisle?,
        ) = Unit

        override suspend fun deleteItem(id: String) = Unit

        override suspend fun restoreItem(item: ShoppingListItem) = Unit

        override suspend fun setCompleted(id: String, completed: Boolean) = Unit

        override suspend fun restoreItems(items: List<ShoppingListItem>) = Unit
    }
}
