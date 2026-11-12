package com.whatsforlunch.app.domain.model

// Ingrediente de una Receta (IngredienteReceta en docs/domain-model.md):
// nombre, cantidad y unidad. Desde `redisenar-formulario-de-receta` todos los
// Ingredientes cuentan en la Coincidencia y en los Faltantes: ya no hay
// distinción obligatorio/opcional. `id` es estable (RD21): se persiste como
// `ingredients.domainId` y lo referencian los vínculos de los Pasos
// (`RecipeStep.ingredientIds`), por lo que sobrevive a los guardados y a los
// cambios de nombre, cantidad o unidad.
data class RecipeIngredient(
    val id: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
)
