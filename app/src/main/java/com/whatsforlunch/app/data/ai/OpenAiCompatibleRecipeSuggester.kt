package com.whatsforlunch.app.data.ai

import android.util.Log
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
    private val promptSource: PromptSource,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : RecipeSuggester {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun suggest(
        foods: List<SuggestableFood>,
        language: String,
        existingRecipes: List<RecipeAvoidance>,
    ): RecipeSuggestionsResult {
        if (foods.isEmpty()) return RecipeSuggestionsResult.Failed(AiFailureCause.INVALID_RESPONSE)
        val key = aiKeyProvider.apiKey() ?: return RecipeSuggestionsResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            val startedAt = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $key")
                    .post(buildBody(foods, language, existingRecipes))
                    .build()
                client.newCall(request).execute().use { response ->
                    val elapsed = elapsedMs(startedAt)
                    if (!response.isSuccessful) {
                        Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms")
                        return@withContext RecipeSuggestionsResult.Failed(AiFailureCause.PROVIDER_ERROR)
                    }
                    // En OkHttp 5 `Response.body` es no-nulable: el chequeo de
                    // cuerpo nulo era código muerto.
                    val body = response.body.string()
                    val content = messageContent(body)
                    if (content == null) {
                        Log.w(
                            TAG,
                            "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                "without message content (body ${body.length} chars)",
                        )
                        return@withContext RecipeSuggestionsResult.Failed(AiFailureCause.INVALID_RESPONSE)
                    }
                    val raw = RecipeSuggestionParser.parse(content)
                    if (raw == null) {
                        Log.w(
                            TAG,
                            "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                "with unparseable content (${content.length} chars)",
                        )
                        return@withContext RecipeSuggestionsResult.Failed(AiFailureCause.INVALID_RESPONSE)
                    }
                    Log.i(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms, ${raw.size} proposals")
                    RecipeSuggestionsResult.Success(SuggestionRules.normalize(raw))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                RecipeSuggestionsResult.Failed(AiFailure.fromException(exception))
            } catch (exception: Exception) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                RecipeSuggestionsResult.Failed(AiFailure.fromException(exception))
            }
        }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun Exception.describe(): String = "${javaClass.simpleName}: ${message.orEmpty()}"

    private fun buildBody(
        foods: List<SuggestableFood>,
        language: String,
        existingRecipes: List<RecipeAvoidance>,
    ): okhttp3.RequestBody {
        val message = JSONObject()
            .put("role", "user")
            .put("content", recipeSuggestionPrompt(foods, language, promptSource, existingRecipes))
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
            null -> null
            is String -> content
            is JSONArray -> (0 until content.length())
                .mapNotNull { index -> content.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() } }
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrNull()

    private companion object {
        const val TAG = "RecipeSuggester"
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}

/**
 * Prompt de Sugerencias (D1 y D2 de `aceptar-sugerencia-de-receta`). Es determinista
 * para una fuente de prompts dada, así que se puede verificar sin red que las Recetas
 * existentes > 60 % solo entran cuando corresponde y con qué forma.
 */
internal fun recipeSuggestionPrompt(
    foods: List<SuggestableFood>,
    language: String,
    source: PromptSource,
    existingRecipes: List<RecipeAvoidance> = emptyList(),
): String {
    val foodList = foods.joinToString(", ") { food ->
        if (food.quantity != null) "${food.name} (${food.quantity})" else food.name
    }
    val base = source.suggestionPrompt(language).replace(FOODS_TOKEN, foodList)
    if (existingRecipes.isEmpty()) return base
    val avoidList = existingRecipes.joinToString("; ") { recipe ->
        "${recipe.title} (${recipe.ingredientNames.joinToString(", ")})"
    }
    val section = source.suggestionAvoidPrompt(language).replace(AVOID_TOKEN, avoidList)
    return "$base $section"
}

private const val FOODS_TOKEN = "{FOODS}"
private const val AVOID_TOKEN = "{RECIPES}"
