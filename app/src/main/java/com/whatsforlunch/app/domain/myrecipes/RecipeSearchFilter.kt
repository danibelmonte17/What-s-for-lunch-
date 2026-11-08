package com.whatsforlunch.app.domain.myrecipes

import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.text.normalizeName

/** Filters recipes by their title or ingredient names and, optionally, meal moment and favorites. */
fun filterRecipes(
    recipes: List<Recipe>,
    query: String,
    moment: MealMoment?,
    favoritesOnly: Boolean = false,
): List<Recipe> {
    val normalizedQuery = normalizeName(query)

    return recipes.filter { recipe ->
        val matchesMoment = moment == null || recipe.mealMoment == moment
        val matchesFavorite = !favoritesOnly || recipe.favorite
        val matchesText = normalizedQuery.isEmpty() ||
            normalizeName(recipe.title).contains(normalizedQuery) ||
            recipe.ingredients.any { ingredient ->
                normalizeName(ingredient.name).contains(normalizedQuery)
            }
        matchesMoment && matchesFavorite && matchesText
    }
}
