package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestionRules
import org.json.JSONArray
import org.json.JSONObject

// Llamada a función pedida por el modelo; los argumentos se ignoran porque las dos
// funciones del Pinche no tienen parámetros (D4).
internal data class PincheToolCall(
    val id: String,
    val name: String,
)

// Protocolo puro del Pinche (D4-D7, D11-D12): definiciones de las dos funciones de
// solo lectura, lectura de `tool_calls`, resultados acotados y separación entre el
// texto conversacional y la única Propuesta del bloque cercado `receta`. Sin
// dependencias de Android, red ni UI para poder probarlo con JVM.
internal object PincheChatProtocol {

    const val MAX_PANTRY_ITEMS = 100
    const val MAX_RECIPES = 50
    const val MAX_RECIPE_INGREDIENTS = 20
    const val MAX_USER_MESSAGE_CHARS = 500

    const val RECIPE_BLOCK_TAG = "receta"

    private const val PANTRY_FUNCTION = "get_pantry"
    private const val RECIPES_FUNCTION = "get_my_recipes"

    fun toolsJson(): JSONArray = JSONArray()
        .put(
            JSONObject()
                .put("type", "function")
                .put(
                    "function",
                    JSONObject()
                        .put("name", PANTRY_FUNCTION)
                        .put(
                            "description",
                            "Read the pantry: the food names and quantities the user has at home. " +
                                "Call it only when you need to know what is available.",
                        )
                        .put("parameters", emptyParameters()),
                ),
        )
        .put(
            JSONObject()
                .put("type", "function")
                .put(
                    "function",
                    JSONObject()
                        .put("name", RECIPES_FUNCTION)
                        .put(
                            "description",
                            "Read My Recipes: titles, meal moments and ingredient names. " +
                                "Call it only when you need to know what the user already cooks.",
                        )
                        .put("parameters", emptyParameters()),
                ),
        )

    private fun emptyParameters(): JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject())
        .put("additionalProperties", false)

    /**
     * D4: `tool_calls[].id` junto al nombre de la función. Los argumentos se ignoran
     * porque las dos funciones no tienen parámetros.
     */
    fun toolCalls(message: JSONObject): List<PincheToolCall> {
        val calls = message.optJSONArray("tool_calls") ?: return emptyList()
        return (0 until calls.length()).mapNotNull { index ->
            val call = calls.optJSONObject(index) ?: return@mapNotNull null
            val function = call.optJSONObject("function") ?: return@mapNotNull null
            val id = call.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = function.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PincheToolCall(id = id, name = name)
        }
    }

    // RF-PCH5/D11: solo nombre y cantidad, como máximo los primeros 100 Alimentos.
    fun pantryToolResult(items: List<PantryItem>): String {
        val array = JSONArray()
        items.take(MAX_PANTRY_ITEMS).forEach { item ->
            array.put(
                JSONObject()
                    .put("name", item.name)
                    .put("quantity", item.quantity ?: JSONObject.NULL),
            )
        }
        return JSONObject().put("items", array).toString()
    }

    // RF-PCH5/Q-14: título, Momento culinario e ingredientes; sin pasos, foto,
    // estado ni cantidades. Topes: 50 Recetas y 20 nombres por Receta (D11).
    fun recipesToolResult(recipes: List<Recipe>): String {
        val array = JSONArray()
        recipes.take(MAX_RECIPES).forEach { recipe ->
            val ingredients = JSONArray()
            recipe.ingredients.take(MAX_RECIPE_INGREDIENTS).forEach { ingredient ->
                ingredients.put(ingredient.name)
            }
            array.put(
                JSONObject()
                    .put("title", recipe.title)
                    .put("moment", recipe.mealMoment.name)
                    .put("ingredients", ingredients),
            )
        }
        return JSONObject().put("recipes", array).toString()
    }

    /**
     * D5/D7: separa el texto de la única Propuesta. Busca el bloque cercado `receta`,
     * lo normaliza con `SuggestionRules` y toma la primera propuesta; si no hay
     * bloque, intenta con el primer objeto JSON del contenido. Un bloque o JSON
     * inválido deja solo el texto.
     */
    fun parseReply(content: String): PincheReply {
        val block = findRecipeBlock(content)
        if (block != null) {
            val text = content.substring(0, block.startIndex).trim()
            return PincheReply(text = text, proposal = parseProposal(block.json))
        }
        val firstJson = firstJsonObject(content)
            ?: return PincheReply(text = content.trim(), proposal = null)
        val text = (content.substring(0, firstJson.startIndex) + content.substring(firstJson.endIndex))
            .trim()
        return PincheReply(text = text, proposal = parseProposal(firstJson.json))
    }

    private fun parseProposal(json: String): RecipeSuggestion? {
        val raw = RecipeSuggestionParser.parse(json) ?: return null
        return SuggestionRules.normalize(raw).firstOrNull()
    }

    private fun findRecipeBlock(content: String): RecipeBlock? {
        val open = Regex("```\\s*receta\\b", RegexOption.IGNORE_CASE).find(content) ?: return null
        val jsonStart = content.indexOf('\n', open.range.last + 1)
            .takeIf { it >= 0 }
            ?: (open.range.last + 1)
        val close = content.indexOf("```", jsonStart)
        val endIndex = close.takeIf { it >= 0 } ?: content.length
        return RecipeBlock(
            startIndex = open.range.first,
            json = content.substring(jsonStart, endIndex).trim(),
        )
    }

    private fun firstJsonObject(value: String): JsonSlice? {
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
                    if (depth == 0) {
                        return JsonSlice(
                            startIndex = start,
                            endIndex = index + 1,
                            json = value.substring(start, index + 1),
                        )
                    }
                }
            }
        }
        return null
    }

    private data class RecipeBlock(
        val startIndex: Int,
        val json: String,
    )

    private data class JsonSlice(
        val startIndex: Int,
        val endIndex: Int,
        val json: String,
    )
}
