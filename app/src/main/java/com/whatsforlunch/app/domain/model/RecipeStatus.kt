package com.whatsforlunch.app.domain.model

// Estado de una Receta (docs/domain-model.md, ciclo de vida):
// Borrador mientras no está lista para cocinar, Publicada cuando lo está.
enum class RecipeStatus {
    DRAFT,
    PUBLISHED,
}
