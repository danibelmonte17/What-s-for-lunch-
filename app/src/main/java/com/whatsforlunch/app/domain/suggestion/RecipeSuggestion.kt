package com.whatsforlunch.app.domain.suggestion

import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep

// Propuesta de la IA con forma de Receta (D1); aún no es una Receta (RD07 se aplica
// al aceptarla). Vive solo en memoria mientras dura el destino de Sugerencias (D4).
data class RecipeSuggestion(
    val title: String,
    val story: String,
    val mealMoment: MealMoment,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: Difficulty,
    val baseServings: Int,
    val caloriesPerServing: Int?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
)

// Sugerencia con su Coincidencia calculada en el cliente (D2, RD01/RD10).
data class SuggestionWithMatch(
    val suggestion: RecipeSuggestion,
    val match: RecipeMatch?,
)

// Alimento que el usuario eligió enviar a la IA (D3): nombre visible y cantidad
// opcional (null = sin cantidad).
data class SuggestableFood(
    val name: String,
    val quantity: Int? = null,
)

// Valores crudos de una propuesta tal y como llegan del JSON compacto de D1. El
// parser de `data/ai` los rellena y la normalización de dominio (D8) los valida.
data class RawRecipeSuggestion(
    val title: String? = null,
    val story: String? = null,
    val mealMoment: String? = null,
    val prepMinutes: Double? = null,
    val cookMinutes: Double? = null,
    val difficulty: String? = null,
    val baseServings: Double? = null,
    val calories: Double? = null,
    val ingredients: List<RawRecipeIngredient> = emptyList(),
    val steps: List<RawRecipeStep> = emptyList(),
)

data class RawRecipeIngredient(
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val optional: Boolean? = null,
)

data class RawRecipeStep(
    val text: String? = null,
    val timerMinutes: Double? = null,
)
