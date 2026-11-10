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
import com.whatsforlunch.app.domain.profile.SavedProfilePart
import com.whatsforlunch.app.domain.profile.UserProfileRules
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import com.whatsforlunch.app.domain.text.normalizeName
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
// `profileRequest` el asistente pide las partes del perfil que faltan: es el
// mensaje estático de `crear-mi-perfil` (RD26/D8) que hoy actúa de **guarda** de
// las Propuestas y de **degradación** sin IA (`rellenar-perfil-con-el-pinche`, D7).
// Con `profileSaved` la app resume lo que `save_profile` persistió en el turno
// (RD27/D2). Ninguno de los dos viaja al modelo (texto en blanco).
data class PincheMessage(
    val id: String = UUID.randomUUID().toString(),
    val author: PincheAuthor,
    val text: String,
    val proposal: PincheProposal? = null,
    val proposalState: PincheProposalState = PincheProposalState.PROPOSED,
    // RF-AFL4: estado en memoria de la Ficha, como «Guardada en Mis Recetas».
    val addedToList: Boolean = false,
    // Partes del perfil pendientes de este mensaje estático (vacío en el resto).
    val profileRequest: List<PendingProfilePart> = emptyList(),
    // Partes del perfil guardadas en este turno (resumen «He guardado…», D2).
    val profileSaved: List<SavedProfilePart> = emptyList(),
)

data class PincheUiState(
    val messages: List<PincheMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val notice: PincheNotice? = null,
    val lastUserText: String = "",
    // Enmienda D9 (`rellenar-perfil-con-el-pinche`): texto visible de la ronda en
    // curso; `null` = sin burbuja transitoria. Se limpia al terminar el turno.
    val streamingText: String? = null,
)

/**
 * Estado de la conversación del Pinche (RD17, D2-D10).
 *
 * Todo vive en memoria: el saludo es solo UI y no viaja al modelo; cada envío hace
 * una tanda al puerto con los últimos 20 turnos. La Ficha se calcula al recibir la
 * respuesta con un snapshot de la Despensa (D8) y guardar crea un Borrador con id
 * estable mediante upsert (RD16). Los fallos nunca congelan la pantalla.
 *
 * `rellenar-perfil-con-el-pinche` (RD26 reescrita / RD27): **el gate previo se
 * retiró** —siempre que haya texto se llama al cliente— y la entrevista de perfil
 * vive en el system prompt. La app añade el resumen «He guardado…» con lo que
 * `save_profile` persistió en el turno (D2) y aplica la **guarda determinista**
 * (D1): una respuesta con Propuesta y partes pendientes no pinta la Ficha y añade
 * el mensaje estático con «Abrir Mi perfil»; sin key o con fallo, además del aviso,
 * se añade ese mismo mensaje (degradación, S5).
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
            streamingText = null,
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
        _uiState.value = _uiState.value.copy(isSending = true, notice = null, streamingText = null)
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
            // RD26/RD27 (`rellenar-perfil-con-el-pinche`): el gate previo se retiró;
            // la entrevista de perfil vive en el system prompt y la guarda de la
            // Propuesta en `onSuccess`. Siempre que haya texto se llama al cliente.
            val profile = currentProfile()
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
                // D9: el texto visible de la ronda llega por `onContent` (hilo IO);
                // en blanco = sin burbuja transitoria.
                pincheChatClient.reply(turns, language, profile) { fragment ->
                    _uiState.update { state ->
                        state.copy(streamingText = fragment.ifBlank { null })
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                PincheReplyResult.Failed
            }
            when (result) {
                is PincheReplyResult.Success -> onSuccess(result)
                PincheReplyResult.MissingApiKey -> onUnavailable(PincheNotice.AI_KEY_MISSING, profile)
                PincheReplyResult.Failed -> onUnavailable(PincheNotice.FAILED, profile)
            }
        }
    }

    /**
     * Aviso de siempre (key ausente o fallo del turno, RF-PCH9) y, con partes del
     * perfil pendientes, además el mensaje estático con «Abrir Mi perfil»
     * (degradación sin IA, S5/`rellenar-perfil-con-el-pinche` D7).
     *
     * D11 (enmienda): si el stream se cortó a mitad, el texto recibido se
     * **conserva** como burbuja del asistente (ya cortado con `streamingText`,
     * sin el JSON de la Propuesta) y viaja en el contexto de un reintento.
     */
    private fun onUnavailable(notice: PincheNotice, profile: UserProfile) {
        val pending = UserProfileRules.pendingParts(profile)
        val partial = _uiState.value.streamingText
        var messages = _uiState.value.messages
        if (!partial.isNullOrBlank()) {
            messages = messages + PincheMessage(
                author = PincheAuthor.ASSISTANT,
                text = partial,
            )
        }
        _uiState.value = _uiState.value.copy(
            messages = if (pending.isEmpty()) {
                messages
            } else {
                messages + PincheMessage(
                    author = PincheAuthor.ASSISTANT,
                    text = "",
                    profileRequest = pending,
                )
            },
            isSending = false,
            notice = notice,
            streamingText = null,
        )
    }

    /** Perfil vigente; si la lectura falla se asume vacío (todo pendiente). */
    private suspend fun currentProfile(): UserProfile =
        runCatching { userProfileRepository.observeProfile().first() }.getOrDefault(UserProfile.empty)

    private suspend fun onSuccess(result: PincheReplyResult.Success) {
        val text = result.reply.text
        val proposal = result.reply.proposal
        if (text.isBlank() && proposal == null) {
            _uiState.value = _uiState.value.copy(
                isSending = false,
                notice = PincheNotice.FAILED,
                streamingText = null,
            )
            return
        }
        var messages = _uiState.value.messages
        // D2: resumen fiel con lo realmente persistido en el turno, antes de la
        // respuesta (texto en blanco: nunca viaja al modelo).
        result.profileSaved?.let { saved ->
            messages = messages + PincheMessage(
                author = PincheAuthor.ASSISTANT,
                text = "",
                profileSaved = saved,
            )
        }
        // D1 (guarda determinista): con partes pendientes la Propuesta **no se
        // pinta**; se conserva el texto y se añade el mensaje estático con la
        // acción «Abrir Mi perfil».
        val pending = UserProfileRules.pendingParts(currentProfile())
        val guardedProposal = if (proposal != null && pending.isEmpty()) {
            // D8: los Faltantes se calculan una sola vez con la Despensa del momento.
            val pantry = runCatching { pantryRepository.observePantryItems().first() }.getOrDefault(emptyList())
            PincheProposal(
                recipe = proposal,
                missingIngredients = PincheProposalRules.missingRequired(proposal, pantry),
            )
        } else {
            null
        }
        messages = messages + PincheMessage(
            author = PincheAuthor.ASSISTANT,
            text = text,
            proposal = guardedProposal,
            profileRequest = if (proposal != null && pending.isNotEmpty()) pending else emptyList(),
        )
        _uiState.value = _uiState.value.copy(
            messages = messages,
            isSending = false,
            notice = null,
            // D9: la burbuja transitoria desaparece al llegar el mensaje final
            // (el resumen «He guardado…» queda antes de la respuesta, D2 intacto).
            streamingText = null,
        )
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 20
    }
}
