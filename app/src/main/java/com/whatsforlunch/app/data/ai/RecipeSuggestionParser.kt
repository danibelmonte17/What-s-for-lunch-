package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.RawRecipeIngredient
import com.whatsforlunch.app.domain.suggestion.RawRecipeStep
import com.whatsforlunch.app.domain.suggestion.RawRecipeSuggestion
import org.json.JSONArray
import org.json.JSONObject

// Tolerant parser for the compact recipe JSON of D1. It recovers the first JSON
// object from the message content (accepting ```json fences and surrounding text)
// and reads `s` as the proposal array. Malformed values are kept as null so the
// domain normalization (D8) can discard or fix them proposal by proposal.
// A response without a JSON object is not parseable and therefore a failure.
internal object RecipeSuggestionParser {

    fun parse(content: String): List<RawRecipeSuggestion>? {
        val json = firstJsonObject(content) ?: return null
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val proposals = root.opt("s") as? JSONArray ?: return emptyList()
        return (0 until proposals.length()).mapNotNull { index ->
            proposals.optJSONObject(index)?.let(::readSuggestion)
        }
    }

    private fun readSuggestion(value: JSONObject): RawRecipeSuggestion = RawRecipeSuggestion(
        title = text(value, "t"),
        story = text(value, "st"),
        mealMoment = text(value, "mm"),
        prepMinutes = number(value, "pm"),
        difficulty = text(value, "df"),
        baseServings = number(value, "bs"),
        calories = number(value, "kcal"),
        ingredients = readIngredients(value.opt("i") as? JSONArray),
        steps = readSteps(value.opt("p") as? JSONArray),
    )

    private fun readIngredients(array: JSONArray?): List<RawRecipeIngredient> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            RawRecipeIngredient(
                name = text(item, "n"),
                quantity = number(item, "q"),
                unit = text(item, "u"),
            )
        }
    }

    private fun readSteps(array: JSONArray?): List<RawRecipeStep> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            RawRecipeStep(
                text = text(item, "t"),
                timerMinutes = number(item, "m"),
            )
        }
    }

    private fun text(value: JSONObject, key: String): String? = value.opt(key) as? String

    private fun number(value: JSONObject, key: String): Double? = when (val raw = value.opt(key)) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDoubleOrNull()
        else -> null
    }

    private fun firstJsonObject(value: String): String? {
        val start = value.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until value.length) {
            val char = value[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return value.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
