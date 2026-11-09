package com.whatsforlunch.app.domain.suggestion

import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.IngredientUnit
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.text.normalizeName

/**
 * Normalización (D8) de las propuestas de la IA y cálculo de su Coincidencia (D2).
 *
 * Las propuestas inválidas se descartan individualmente sin tumbar la lista; los
 * valores fuera de rango se corrigen con los defectos acordados. La Coincidencia
 * reutiliza `calculateMatch` (RD01/RD10) contra los Alimentos que el usuario envió,
 * sin tocar la regla compartida.
 */
object SuggestionRules {

    /** Máximo de Sugerencias mostradas (D7). */
    const val MAX_SUGGESTIONS = 3

    const val DEFAULT_BASE_SERVINGS = 4
    const val MIN_BASE_SERVINGS = 1
    const val MAX_BASE_SERVINGS = 12
    const val DEFAULT_INGREDIENT_QUANTITY = 1.0

    fun normalize(rawSuggestions: List<RawRecipeSuggestion>): List<RecipeSuggestion> {
        val normalized = mutableListOf<RecipeSuggestion>()
        val seenTitles = mutableSetOf<String>()
        rawSuggestions.forEach { raw ->
            val suggestion = normalizeOne(raw) ?: return@forEach
            // D8: las propuestas inválidas se descartan sin tumbar la lista.
            if (!seenTitles.add(normalizeName(suggestion.title))) return@forEach
            normalized += suggestion
            if (normalized.size == MAX_SUGGESTIONS) return normalized
        }
        return normalized
    }

    /**
     * D2: la Coincidencia se calcula con `calculateMatch` sobre los ingredientes
     * obligatorios de la propuesta contra los Alimentos seleccionados (RD01/RD10).
     * Se conserva el orden de la respuesta de la IA (D7).
     */
    fun suggestionMatches(
        suggestions: List<RecipeSuggestion>,
        availableFoods: List<SuggestableFood>,
    ): List<SuggestionWithMatch> {
        val pantryNames = availableFoods.associateBy { it.name.trim() }
        val pantryItems = pantryNames.values.map { food ->
            PantryItem(id = food.name, name = food.name, quantity = food.quantity)
        }
        return suggestions.map { suggestion ->
            val recipe = Recipe(
                id = "",
                title = suggestion.title,
                story = suggestion.story,
                mealMoment = suggestion.mealMoment,
                prepMinutes = suggestion.prepMinutes,
                cookMinutes = suggestion.cookMinutes,
                difficulty = suggestion.difficulty,
                baseServings = suggestion.baseServings,
                caloriesPerServing = suggestion.caloriesPerServing,
                ingredients = suggestion.ingredients,
                steps = suggestion.steps,
            )
            SuggestionWithMatch(suggestion = suggestion, match = calculateMatch(recipe, pantryItems))
        }
    }

    private fun normalizeOne(raw: RawRecipeSuggestion): RecipeSuggestion? {
        val title = cleanText(raw.title)
        if (title.isEmpty()) return null

        val ingredients = raw.ingredients.mapNotNull { ingredient ->
            val name = cleanText(ingredient.name)
            if (name.isEmpty()) return@mapNotNull null
            RecipeIngredient(
                name = name,
                // D8: una cantidad no numérica o ≤ 0 queda en 1; el ingrediente nunca
                // se descarta por su cantidad.
                quantity = positiveOrNull(ingredient.quantity) ?: DEFAULT_INGREDIENT_QUANTITY,
                // D2 (unificar-unidades): la unidad de la IA se normaliza al enum
                // cerrado; la vacía o desconocida cae a «ud».
                unit = IngredientUnit.normalize(ingredient.unit),
                optional = ingredient.optional ?: false,
            )
        }
        val steps = raw.steps.mapNotNull { step ->
            val text = cleanText(step.text)
            if (text.isEmpty()) return@mapNotNull null
            RecipeStep(text = text, timerMinutes = nonNegativeMinutes(step.timerMinutes))
        }
        if (ingredients.isEmpty() || steps.isEmpty()) return null

        return RecipeSuggestion(
            title = title,
            story = cleanText(raw.story),
            mealMoment = parseMealMoment(raw.mealMoment),
            prepMinutes = nonNegativeMinutes(raw.prepMinutes) ?: 0,
            cookMinutes = nonNegativeMinutes(raw.cookMinutes) ?: 0,
            difficulty = parseDifficulty(raw.difficulty),
            baseServings = baseServings(raw.baseServings),
            caloriesPerServing = calories(raw.calories),
            ingredients = ingredients,
            steps = steps,
        )
    }

    private fun cleanText(value: String?): String =
        value.orEmpty().trim().replace(WHITESPACE, " ")

    private fun positiveOrNull(value: Double?): Double? =
        value?.takeIf { it.isFinite() && it > 0.0 }

    private fun nonNegativeMinutes(value: Double?): Int? =
        value?.takeIf { it.isFinite() && it >= 0.0 }?.toInt()

    private fun baseServings(value: Double?): Int {
        if (value == null || !value.isFinite() || value % 1.0 != 0.0) return DEFAULT_BASE_SERVINGS
        val servings = value.toInt()
        return if (servings in MIN_BASE_SERVINGS..MAX_BASE_SERVINGS) servings else DEFAULT_BASE_SERVINGS
    }

    private fun calories(value: Double?): Int? =
        value?.takeIf { it.isFinite() && it > 0.0 }?.toInt()

    private fun parseMealMoment(value: String?): MealMoment =
        MealMoment.entries.firstOrNull { it.name == value?.trim()?.uppercase() } ?: MealMoment.LUNCH

    private fun parseDifficulty(value: String?): Difficulty =
        Difficulty.entries.firstOrNull { it.name == value?.trim()?.uppercase() } ?: Difficulty.MEDIUM

    private val WHITESPACE = Regex("\\s+")
}
