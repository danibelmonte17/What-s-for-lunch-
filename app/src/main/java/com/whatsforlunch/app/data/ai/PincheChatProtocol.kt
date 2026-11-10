package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.domain.profile.ProfileUpdate
import com.whatsforlunch.app.domain.profile.ProfileUpdateError
import com.whatsforlunch.app.domain.profile.SavedProfilePart
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestionRules
import org.json.JSONArray
import org.json.JSONObject

// Llamada a función pedida por el modelo: id, nombre y sus argumentos en crudo.
// Las dos funciones de solo lectura no tienen parámetros y los ignoran;
// `save_profile` los parsea con `parseSaveProfileArgs` (D8).
internal data class PincheToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

// Protocolo puro del Pinche (D4-D7, D11-D12): definiciones de las tres
// funciones —dos de solo lectura y `save_profile`, de escritura acotada al
// perfil (RD27)—, lectura de `tool_calls`, resultados acotados y separación entre
// el texto conversacional y la única Propuesta del bloque cercado `receta`. Sin
// dependencias de Android, red ni UI para poder probarlo con JVM.
internal object PincheChatProtocol {

    const val MAX_PANTRY_ITEMS = 100
    const val MAX_RECIPES = 50
    const val MAX_RECIPE_INGREDIENTS = 20
    const val MAX_USER_MESSAGE_CHARS = 500

    const val RECIPE_BLOCK_TAG = "receta"

    private const val PANTRY_FUNCTION = "get_pantry"
    private const val RECIPES_FUNCTION = "get_my_recipes"
    private const val SAVE_PROFILE_FUNCTION = "save_profile"

    // Campos de los argumentos de `save_profile` (RD27).
    private const val ALLERGIES_FIELD = "allergies"
    private const val ALLERGIES_NONE_FIELD = "allergies_none"
    private const val INTOLERANCES_FIELD = "intolerances"
    private const val INTOLERANCES_NONE_FIELD = "intolerances_none"
    private const val DIET_TYPE_FIELD = "diet_type"

    /** Marca de «sin corte» en `streamingText` (ningún candidato a ocultar). */
    private const val NO_CUT = Int.MAX_VALUE

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
        .put(
            JSONObject()
                .put("type", "function")
                .put(
                    "function",
                    JSONObject()
                        .put("name", SAVE_PROFILE_FUNCTION)
                        .put(
                            "description",
                            "Write the user's Mi perfil sections from what the user just said; " +
                                "only call it when the user states their data.",
                        )
                        .put("parameters", saveProfileParameters()),
                ),
        )

    private fun emptyParameters(): JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", JSONObject())
        .put("additionalProperties", false)

    // RD27/D5: cinco campos opcionales; el tipo de alimentación con el vocabulario
    // cerrado es/en del bloque de perfil (D8) y sin campos extra (D5).
    private fun saveProfileParameters(): JSONObject = JSONObject()
        .put("type", "object")
        .put(
            "properties",
            JSONObject()
                .put("allergies", stringArrayParameter())
                .put("allergies_none", JSONObject().put("type", "boolean"))
                .put("intolerances", stringArrayParameter())
                .put("intolerances_none", JSONObject().put("type", "boolean"))
                .put(
                    "diet_type",
                    JSONObject()
                        .put("type", "string")
                        .put("enum", JSONArray(DIET_TYPE_ENUM_VALUES)),
                ),
        )
        .put("additionalProperties", false)

    private fun stringArrayParameter(): JSONObject = JSONObject()
        .put("type", "array")
        .put("items", JSONObject().put("type", "string"))

    /**
     * `tool_calls[].id`, nombre de la función y sus argumentos en crudo (los de
     * lectura no tienen parámetros y se ignoran).
     */
    fun toolCalls(message: JSONObject): List<PincheToolCall> {
        val calls = message.optJSONArray("tool_calls") ?: return emptyList()
        return (0 until calls.length()).mapNotNull { index ->
            val call = calls.optJSONObject(index) ?: return@mapNotNull null
            val function = call.optJSONObject("function") ?: return@mapNotNull null
            val id = call.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = function.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PincheToolCall(id = id, name = name, arguments = function.optString("arguments"))
        }
    }

    /**
     * Argumentos de `save_profile`: `null` cuando no son usables (JSON que no es
     * objeto, tipos incorrectos o `diet_type` fuera del vocabulario cerrado, D8).
     */
    fun parseSaveProfileArgs(json: String): ProfileUpdate? = parseArgs(json).update

    /**
     * Código de error para `saveProfileToolError` cuando `parseSaveProfileArgs`
     * devuelve `null`: `unknown_diet_type` si `diet_type` está presente pero no se
     * puede mapear al enum (D8), `invalid_arguments` en el resto de casos.
     */
    fun saveProfileArgsError(json: String): String =
        parseArgs(json).error ?: ProfileUpdateError.INVALID_ARGUMENTS

    // Resultado interno del parseo: `update` si es usable, `error` si hay que
    // rechazarlo (solo tiene sentido cuando `update` es null).
    private data class ParsedArgs(val update: ProfileUpdate?, val error: String?)

    /** Un campo presente con un tipo que no corresponde (→ `invalid_arguments`). */
    private class FieldTypeError : Exception()

    private fun parseArgs(json: String): ParsedArgs {
        val invalid = ParsedArgs(null, ProfileUpdateError.INVALID_ARGUMENTS)
        val object_ = try {
            JSONObject(json)
        } catch (_: Exception) {
            return invalid
        }
        // `diet_type` presente pero fuera del vocabulario cerrado (D8).
        val dietType = if (object_.has(DIET_TYPE_FIELD)) {
            val raw = object_.opt(DIET_TYPE_FIELD) as? String
                ?: return ParsedArgs(null, ProfileUpdateError.UNKNOWN_DIET_TYPE)
            DIET_TYPE_VOCABULARY[raw.trim().lowercase()]
                ?: return ParsedArgs(null, ProfileUpdateError.UNKNOWN_DIET_TYPE)
        } else {
            null
        }
        val update = try {
            ProfileUpdate(
                allergies = optionalStringList(object_, ALLERGIES_FIELD),
                allergiesNone = optionalBoolean(object_, ALLERGIES_NONE_FIELD),
                intolerances = optionalStringList(object_, INTOLERANCES_FIELD),
                intolerancesNone = optionalBoolean(object_, INTOLERANCES_NONE_FIELD),
                dietType = dietType,
            )
        } catch (_: FieldTypeError) {
            return invalid
        }
        return ParsedArgs(update = update, error = null)
    }

    /** Valor ausente → `null` (campo omitido, sin tocar); tipo incorrecto → error. */
    private fun optionalStringList(`object`: JSONObject, key: String): List<String>? {
        if (!`object`.has(key)) return null
        val array = `object`.optJSONArray(key) ?: throw FieldTypeError()
        val values = mutableListOf<String>()
        for (index in 0 until array.length()) {
            if (array.opt(index) !is String) throw FieldTypeError()
            values += array.getString(index)
        }
        return values
    }

    private fun optionalBoolean(`object`: JSONObject, key: String): Boolean? {
        if (!`object`.has(key)) return null
        return `object`.opt(key) as? Boolean ?: throw FieldTypeError()
    }

    // RD27/D2: lo realmente persistido (valores finales) y lo que sigue pendiente,
    // para que el modelo sepa qué preguntar después.
    fun saveProfileToolResult(saved: List<SavedProfilePart>, pending: List<PendingProfilePart>): String {
        val savedJson = JSONObject()
        saved.forEach { part ->
            when (part.part) {
                PendingProfilePart.ALLERGIES -> {
                    savedJson.put(ALLERGIES_FIELD, JSONArray(part.chips))
                    savedJson.put(ALLERGIES_NONE_FIELD, part.none)
                }

                PendingProfilePart.INTOLERANCES -> {
                    savedJson.put(INTOLERANCES_FIELD, JSONArray(part.chips))
                    savedJson.put(INTOLERANCES_NONE_FIELD, part.none)
                }

                PendingProfilePart.DIET_TYPE -> part.dietType?.let { dietType ->
                    savedJson.put(DIET_TYPE_FIELD, dietType.name)
                }
            }
        }
        return JSONObject()
            .put("saved", savedJson)
            .put("pending", JSONArray(pending.map { it.name }))
            .toString()
    }

    fun saveProfileToolError(code: String): String =
        JSONObject().put("error", code).toString()

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

    /**
     * Texto visible durante el streaming (`rellenar-perfil-con-el-pinche`, D9):
     * devuelve el contenido **sin el JSON de la Propuesta**, cortando en el
     * inicio del bloque cercado `receta` o en el primer `{` seguido de comillas
     * (inicio de objeto JSON, equilibrado o aún a medio formar). Sin candidatos,
     * devuelve el contenido entero.
     *
     * Caso límite aceptado en la spec: un `{"` en prosa que no llegue a ser JSON
     * se corta aquí y `parseReply` puede devolverlo después como texto.
     */
    fun streamingText(content: String): String {
        val fence = Regex("```\\s*receta\\b", RegexOption.IGNORE_CASE)
            .find(content)?.range?.first ?: NO_CUT
        val json = jsonStart(content) ?: NO_CUT
        val cut = minOf(fence, json)
        val visible = if (cut == NO_CUT) content else content.substring(0, cut)
        return visible.trim()
    }

    /** Posición del primer `{` seguido de comillas (posible inicio de JSON). */
    private fun jsonStart(content: String): Int? {
        var index = content.indexOf('{')
        while (index >= 0) {
            if (content.substring(index + 1).trimStart().startsWith("\"")) return index
            index = content.indexOf('{', index + 1)
        }
        return null
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
