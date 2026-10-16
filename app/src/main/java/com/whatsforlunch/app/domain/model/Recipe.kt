package com.whatsforlunch.app.domain.model

// Receta: plato con ingredientes, pasos y metadatos (docs/domain-model.md).
// Modelo mínimo para la base de la app; se amplía en features posteriores.
data class Recipe(
    val id: String,
    val title: String,
)
