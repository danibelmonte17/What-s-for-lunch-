package com.whatsforlunch.app.domain.model

// Receta: plato con ingredientes, pasos y metadatos (docs/domain-model.md).
// Modelo de dominio puro: sin dependencias de Android ni de la UI.
data class Recipe(
    val id: String,
    val title: String,
    val story: String = "",
    val mealMoment: MealMoment = MealMoment.LUNCH,
    val prepMinutes: Int = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val baseServings: Int = 4,
    val caloriesPerServing: Int? = null,
    val photoPath: String? = null,
    val favorite: Boolean = false,
    val origin: RecipeOrigin = RecipeOrigin.USER,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
)
