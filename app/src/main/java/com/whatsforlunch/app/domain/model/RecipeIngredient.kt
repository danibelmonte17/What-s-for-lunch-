package com.whatsforlunch.app.domain.model

// Ingrediente de una Receta (IngredienteReceta en docs/domain-model.md):
// nombre, cantidad, unidad en texto libre y si es opcional. `id` es estable
// (RD21): se persiste como `ingredients.domainId` y lo referencian los vínculos
// de los Pasos (`RecipeStep.ingredientIds`), por lo que sobrevive a los guardados
// y a los cambios de nombre, cantidad, unidad u opcionalidad.
data class RecipeIngredient(
    val id: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val optional: Boolean = false,
)
