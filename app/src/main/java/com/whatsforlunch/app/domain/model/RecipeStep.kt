package com.whatsforlunch.app.domain.model

// Paso de una Receta (docs/domain-model.md): texto y temporizador opcional
// en minutos. El orden es el de inserción. `ingredientIds` (RD21) son los ids de
// los Ingredientes de la Receta que usa el Paso: vínculo N:M y opcional (un
// Ingrediente puede usarse en varios Pasos), guardado en el orden de ingredientes
// de la Receta; vacío = sin vínculos (recetas de IA o legadas, D4).
data class RecipeStep(
    val id: String = "",
    val text: String = "",
    val timerMinutes: Int? = null,
    val ingredientIds: List<String> = emptyList(),
)
