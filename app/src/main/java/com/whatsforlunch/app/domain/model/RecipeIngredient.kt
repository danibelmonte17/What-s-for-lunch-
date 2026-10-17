package com.whatsforlunch.app.domain.model

// Ingrediente de una Receta (IngredienteReceta en docs/domain-model.md):
// nombre, cantidad, unidad en texto libre y si es opcional.
data class RecipeIngredient(
    val id: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val optional: Boolean = false,
)
