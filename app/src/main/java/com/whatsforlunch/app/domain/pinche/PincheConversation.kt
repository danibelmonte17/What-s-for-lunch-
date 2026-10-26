package com.whatsforlunch.app.domain.pinche

import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion

// Conversación del Pinche (RD17, D2): todo vive solo en memoria mientras dura el
// ViewModel. Un turno es cada mensaje de texto que viaja al modelo; las Propuestas
// y los avisos de la UI no son turnos.
enum class PincheAuthor {
    USER,
    ASSISTANT,
}

data class PincheTurn(
    val author: PincheAuthor,
    val text: String,
)

// Estado de la Ficha de una Propuesta (D9): guardada o aún pendiente. Sobrevive a
// la rotación con el resto de la conversación y se pierde al salir del Pinche.
enum class PincheProposalState {
    PROPOSED,
    SAVED,
}

// Propuesta del Pinche con sus Faltantes calculados una sola vez al recibir la
// respuesta (D8, RD15).
data class PincheProposal(
    val recipe: RecipeSuggestion,
    val missingIngredients: List<String>,
)

// Respuesta final del turno: texto conversacional y, como máximo, una Propuesta
// válida (D5, D7). Sin Propuesta, solo se muestra el texto.
data class PincheReply(
    val text: String,
    val proposal: RecipeSuggestion? = null,
)
