package com.whatsforlunch.app.domain.misrecetas

import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import java.text.Normalizer
import java.util.Locale

/** Filters recipes by their title or ingredient names and, optionally, meal moment. */
fun filterRecipes(
    recipes: List<Recipe>,
    query: String,
    moment: MealMoment?,
): List<Recipe> {
    val normalizedQuery = normalizeRecipeSearchText(query)

    return recipes.filter { recipe ->
        val matchesMoment = moment == null || recipe.mealMoment == moment
        val matchesText = normalizedQuery.isEmpty() ||
            normalizeRecipeSearchText(recipe.title).contains(normalizedQuery) ||
            recipe.ingredients.any { ingredient ->
                normalizeRecipeSearchText(ingredient.name).contains(normalizedQuery)
            }
        matchesMoment && matchesText
    }
}

private fun normalizeRecipeSearchText(value: String): String = Normalizer
    .normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(ACCENT_MARKS, "")

private val ACCENT_MARKS = Regex("\\p{Mn}+")
