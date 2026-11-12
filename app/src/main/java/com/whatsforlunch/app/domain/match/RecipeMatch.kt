package com.whatsforlunch.app.domain.match

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.text.normalizeName
import kotlin.math.roundToInt

// Coincidencia de una Receta con la Despensa (RD01, RD10; badge según RD02).
// Desde `redisenar-formulario-de-receta` todos los Ingredientes cuentan: ya no
// hay distinción obligatorio/opcional.
data class RecipeMatch(
    val percentage: Int,
    val availableRequired: Int,
    val totalRequired: Int,
) {
    val isFullMatch: Boolean get() = percentage == 100
}

// Returns null when the recipe has no ingredients at all (RF-MB6).
fun calculateMatch(recipe: Recipe, pantryItems: List<PantryItem>): RecipeMatch? {
    val requiredIngredients = recipe.ingredients
    if (requiredIngredients.isEmpty()) return null

    val pantryNames = pantryItems.mapTo(mutableSetOf()) { normalizeName(it.name) }
    val availableRequired = requiredIngredients.count { ingredient ->
        normalizeName(ingredient.name) in pantryNames
    }
    val totalRequired = requiredIngredients.size
    return RecipeMatch(
        percentage = (availableRequired * 100f / totalRequired).roundToInt(),
        availableRequired = availableRequired,
        totalRequired = totalRequired,
    )
}
