package com.whatsforlunch.app.domain.model

// Paso de una Receta (docs/domain-model.md): texto y temporizador opcional
// en minutos. El orden es el de inserción.
data class RecipeStep(
    val id: String = "",
    val text: String = "",
    val timerMinutes: Int? = null,
)
