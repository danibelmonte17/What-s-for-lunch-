package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.SuggestionRules
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Cliente de Sugerencias sobre una API compatible con OpenAI (DevExpert Inference).
// Hace una sola llamada a /chat/completions con el alias estable «chat» (D10), pide
// el JSON compacto estructurado (D1) con `response_format: json_object` y normaliza
// la respuesta con las reglas de dominio (D8). Sin key se informa por separado
// (⚠ S05); cualquier fallo de red, proveedor o parseo es `Failed`.
class OpenAiCompatibleRecipeSuggester(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : RecipeSuggester {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun suggest(foods: List<SuggestableFood>, language: String): RecipeSuggestionsResult {
        if (foods.isEmpty()) return RecipeSuggestionsResult.Failed
        val key = aiKeyProvider.apiKey() ?: return RecipeSuggestionsResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $key")
                    .post(buildBody(foods, language))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext RecipeSuggestionsResult.Failed
                    val body = response.body?.string() ?: return@withContext RecipeSuggestionsResult.Failed
                    val content = messageContent(body) ?: return@withContext RecipeSuggestionsResult.Failed
                    val raw = RecipeSuggestionParser.parse(content)
                        ?: return@withContext RecipeSuggestionsResult.Failed
                    RecipeSuggestionsResult.Success(SuggestionRules.normalize(raw))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: IOException) {
                RecipeSuggestionsResult.Failed
            } catch (_: Exception) {
                RecipeSuggestionsResult.Failed
            }
        }
    }

    private fun buildBody(foods: List<SuggestableFood>, language: String): okhttp3.RequestBody {
        val message = JSONObject()
            .put("role", "user")
            .put("content", prompt(foods, language))
        val responseFormat = JSONObject().put("type", "json_object")
        val json = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0)
            put("response_format", responseFormat)
            put("messages", JSONArray().put(message))
        }
        return json.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    private fun messageContent(body: String): String? = runCatching {
        val choices = JSONObject(body).optJSONArray("choices") ?: return@runCatching null
        val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return@runCatching null
        when (val content = message.opt("content")) {
            is String -> content
            is JSONArray -> (0 until content.length())
                .mapNotNull { index -> content.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() } }
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrNull()

    private fun prompt(foods: List<SuggestableFood>, language: String): String {
        val foodList = foods.joinToString(", ") { food ->
            if (food.quantity != null) "${food.name} (${food.quantity})" else food.name
        }
        return if (language.lowercase().startsWith("en")) {
            ENGLISH_PROMPT.replace(FOODS_TOKEN, foodList)
        } else {
            SPANISH_PROMPT.replace(FOODS_TOKEN, foodList)
        }
    }

    private companion object {
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val FOODS_TOKEN = "{FOODS}"

        const val SPANISH_PROMPT =
            "Eres un cocinero experto. Con estos alimentos disponibles: {FOODS}. " +
                "Propón hasta 3 recetas completas que los aprovechen (prioriza usarlos). " +
                "Responde EXCLUSIVAMENTE con un único objeto JSON, sin markdown ni explicaciones, " +
                "con la forma {\"s\":[{\"t\":\"título\",\"st\":\"historia breve\",\"mm\":\"BREAKFAST|LUNCH|DINNER|DESSERT\"," +
                "\"pm\":10,\"cm\":20,\"df\":\"EASY|MEDIUM|HARD\",\"bs\":4,\"kcal\":320," +
                "\"i\":[{\"n\":\"nombre\",\"q\":3,\"u\":\"ud\",\"o\":false}]," +
                "\"p\":[{\"t\":\"texto del paso\",\"m\":10}]}]}. " +
                "Escribe títulos, historias, ingredientes y pasos en español. " +
                "Usa las claves cortas indicadas y los tokens de máquina para mm y df."

        const val ENGLISH_PROMPT =
            "You are an expert cook. With these available foods: {FOODS}. " +
                "Propose up to 3 complete recipes that use them (prioritize using them). " +
                "Reply ONLY with a single JSON object, without markdown or explanations, " +
                "using the shape {\"s\":[{\"t\":\"title\",\"st\":\"short story\",\"mm\":\"BREAKFAST|LUNCH|DINNER|DESSERT\"," +
                "\"pm\":10,\"cm\":20,\"df\":\"EASY|MEDIUM|HARD\",\"bs\":4,\"kcal\":320," +
                "\"i\":[{\"n\":\"name\",\"q\":3,\"u\":\"unit\",\"o\":false}]," +
                "\"p\":[{\"t\":\"step text\",\"m\":10}]}]}. " +
                "Write titles, stories, ingredients and steps in English. " +
                "Use the short keys above and machine tokens for mm and df."
    }
}
