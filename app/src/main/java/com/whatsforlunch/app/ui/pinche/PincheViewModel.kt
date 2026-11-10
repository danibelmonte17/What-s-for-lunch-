package com.whatsforlunch.app.ui.pinche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.ai.PincheChatClient
import com.whatsforlunch.app.data.ai.PincheChatProtocol
import com.whatsforlunch.app.data.ai.PincheReplyResult
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheProposal
import com.whatsforlunch.app.domain.pinche.PincheProposalRules
import com.whatsforlunch.app.domain.pinche.PincheProposalState
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.domain.profile.UserProfileRules
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.text.normalizeName
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Avisos con acción del Pinche (RF-PCH9): key ausente, fallo del turno y fallo al
// guardar. Se muestran fuera de la lista de mensajes.
enum class PincheNotice {
    AI_KEY_MISSING,
    FAILED,
    SAVE_FAILED,
    SHOPPING_LIST_FAILED,
}

// Mensaje en memoria de la conversación (D2). Una respuesta del asistente puede
// llevar como máximo una Propuesta válida (D7) y su estado de guardado (D9). Con
// `profileRequest` el asistente pide las partes del perfil que faltan antes de
// responder (`crear-mi-perfil`, RD26/D5): no viaja al modelo (texto en blanco).
data class PincheMessage(
    val id: String = UUID.randomUUID().toString(),
    val author: PincheAuthor,
    val text: String,
    val proposal: PincheProposal? = null,
    val proposalState: PincheProposalState = PincheProposalState.PROPOSED,
    // RF-AFL4: estado en memoria de la Ficha, como «Guardada en Mis Recetas».
    val addedToList: Boolean = false,
    // Partes del perfil pendientes de este mensaje de petición (vacío en el resto).
    val profileRequest: List<PendingProfilePart> = emptyList(),
)

data class PincheUiState(
    val messages: List<PincheMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val notice: PincheNotice? = null,
    val lastUserText: String = "",
)

/**
 * Estado de la conversación del Pinche (RD17, D2-D10).
 *
 * Todo vive en memoria: el saludo es solo UI y no viaja al modelo; cada envío hace
 * una tanda al puerto con los últimos 20 turnos. La Ficha se calcula al recibir la
 * respuesta con un snapshot de la Despensa (D8) y guardar crea un Borrador con id
 * estable mediante upsert (RD16). Los fallos nunca congelan la pantalla.
 *
 * Con el perfil del usuario incompleto el envío no llega al modelo: se añade un
 * mensaje del asistente que pide solo las partes pendientes (RD26/D5) y la
 * llamada se retoma cuando el perfil está completo.
 */
class PincheViewModel(
    private val pantryRepository: PantryRepository,
    private val recipeRepository: RecipeRepository,
    private val pincheChatClient: PincheChatClient,
    private val shoppingListRepository: ShoppingListRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PincheUiState())
    val uiState: StateFlow<PincheUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(input = text.take(PincheChatProtocol.MAX_USER_MESSAGE_CHARS))
    }

    fun send(language: String) {
        val state = _uiState.value
        val text = _uiState.value.input.trim().take(PincheChatProtocol.MAX_USER_MESSAGE_CHARS)
        if (text.isBlank() || state.isSending) return
        val userMessage = PincheMessage(author = PincheAuthor.USER, text = text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            input = "",
            isSending = true,
            notice = null,
            lastUserText = text,
        )
        runRequest(language)
    }

    /** Los chips solo existen con la conversación vacía y envían su texto (RF-PCH2). */
    fun sendQuickSuggestion(text: String, language: String) {
        if (_uiState.value.messages.isNotEmpty()) return
        _uiState.value = _uiState.value.copy(input = text.take(PincheChatProtocol.MAX_USER_MESSAGE_CHARS))
        send(language)
    }

    // RF-PCH9: reenvía el último mensaje del usuario sin añadir otra burbuja.
    fun retry(language: String) {
        val state = _uiState.value
        if (state.isSending || state.lastUserText.isBlank()) return
        _uiState.value = _uiState.value.copy(isSending = true, notice = null)
        runRequest(language)
    }

    /**
     * RF-PCH8/D9: marca «Guardada» antes de persistir y revierte con aviso si el
     * repositorio falla, de modo que el botón vuelve a estar habilitado.
     */
    fun saveProposal(messageId: String) {
        val state = _uiState.value
        val message = state.messages.firstOrNull { it.id == messageId } ?: return
        val proposal = message.proposal ?: return
        if (message.proposalState == PincheProposalState.SAVED) return
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.map { message ->
                if (message.id == messageId) message.copy(proposalState = PincheProposalState.SAVED) else message
            },
            notice = null,
        )
        viewModelScope.launch {
            val saved = runCatching {
                recipeRepository.saveRecipe(PincheProposalRules.proposalToRecipe(proposal.recipe))
            }.isSuccess
            if (!saved) {
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(proposalState = PincheProposalState.PROPOSED)
                        } else {
                            message
                        }
                    },
                    notice = PincheNotice.SAVE_FAILED,
                )
            }
        }
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    /**
     * RF-AFL4/D8/D9: añade los Faltantes del snapshot RD15 con cantidades base y la
     * clave de fuente de la Propuesta (RD16), de modo que la Ficha y el Detalle del
     * Borrador guardado no dupliquen. La Ficha queda «Añadido…» deshabilitada.
     */
    fun addMissingToShoppingList(messageId: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val proposal = message.proposal ?: return
        if (message.addedToList || proposal.missingIngredients.isEmpty()) return
        val missingNames = proposal.missingIngredients.mapTo(mutableSetOf(), ::normalizeName)
        val seen = mutableSetOf<String>()
        val ingredients = proposal.recipe.ingredients
            .filter { ingredient ->
                val normalized = normalizeName(ingredient.name)
                normalized in missingNames && seen.add(normalized)
            }
        if (ingredients.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.map { current ->
                if (current.id == messageId) current.copy(addedToList = true) else current
            },
        )
        viewModelScope.launch {
            val added = runCatching {
                shoppingListRepository.addMissing(
                    sourceKey = PincheProposalRules.proposalRecipeId(proposal.recipe.title),
                    sourceTitle = proposal.recipe.title,
                    ingredients = ingredients,
                )
            }.isSuccess
            if (!added) {
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.map { current ->
                        if (current.id == messageId) current.copy(addedToList = false) else current
                    },
                    notice = PincheNotice.SHOPPING_LIST_FAILED,
                )
            }
        }
    }

    private fun runRequest(language: String) {
        viewModelScope.launch {
            val profile = currentProfile()
            // RD26/D5 (`crear-mi-perfil`): con partes del perfil pendientes el
            // Pinche **no llama a la IA**: pide solo lo que falta y no responde a
            // la pregunta hasta que el perfil esté completo.
            val pending = UserProfileRules.pendingParts(profile)
            if (pending.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + PincheMessage(
                        author = PincheAuthor.ASSISTANT,
                        text = "",
                        profileRequest = pending,
                    ),
                    isSending = false,
                    notice = null,
                )
                return@launch
            }
            val turns = _uiState.value.messages
                .filter { it.text.isNotBlank() }
                .map { message ->
                    PincheTurn(
                        author = message.author,
                        text = message.text,
                    )
                }
                .takeLast(MAX_CONTEXT_MESSAGES)
            val result = try {
                pincheChatClient.reply(turns, language, profile)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                PincheReplyResult.Failed
            }
            when (result) {
                is PincheReplyResult.Success -> onSuccess(result.reply.text, result.reply.proposal)
                PincheReplyResult.MissingApiKey -> _uiState.value = _uiState.value.copy(
                    isSending = false,
                    notice = PincheNotice.AI_KEY_MISSING,
                )

                PincheReplyResult.Failed -> _uiState.value = _uiState.value.copy(
                    isSending = false,
                    notice = PincheNotice.FAILED,
                )
            }
        }
    }

    /** Perfil vigente; si la lectura falla se asume vacío (todo pendiente). */
    private suspend fun currentProfile(): UserProfile =
        runCatching { userProfileRepository.observeProfile().first() }.getOrDefault(UserProfile.empty)

    private suspend fun onSuccess(text: String, proposal: RecipeSuggestion?) {
        if (text.isBlank() && proposal == null) {
            _uiState.value = _uiState.value.copy(isSending = false, notice = PincheNotice.FAILED)
            return
        }
        // D8: los Faltantes se calculan una sola vez con la Despensa del momento.
        val pantry = runCatching { pantryRepository.observePantryItems().first() }.getOrDefault(emptyList())
        val pincheProposal = proposal?.let { suggestion ->
            PincheProposal(
                recipe = suggestion,
                missingIngredients = PincheProposalRules.missingRequired(suggestion, pantry),
            )
        }
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + PincheMessage(
                author = PincheAuthor.ASSISTANT,
                text = text,
                proposal = pincheProposal,
            ),
            isSending = false,
            notice = null,
        )
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 20
    }
}
