package com.whatsforlunch.app.domain.model

// Origen de una Receta (docs/domain-model.md, P6/P32, RD07):
// creada a mano por el Usuario o aceptada desde una Sugerencia de la IA.
enum class RecipeOrigin {
    USER,
    SUGGESTION,
}
