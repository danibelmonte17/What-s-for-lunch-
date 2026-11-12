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
// Hace una llamada a /chat/completions con el alias estable «chat» (D10): las
// instrucciones y el contrato viajan en rol `system` y los datos del usuario en
// rol `user` delimitados con `<<<`/`>>>` (RF-MPC1, guía de IA del usuario
// 2026-11-12); `response_format` lleva `json_schema` con el esquema formal del
// contrato (RF-MPC2) y, si el proveedor lo rechaza con un 4xx, se reintenta una
// sola vez con `json_object` (RF-MPC3). Normaliza la respuesta con las reglas de
// dominio (D8). Sin key se informa por separado (⚠ S05); cualquier fallo de red,
// proveedor o parseo es `Failed`.
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
                var useJsonSchema = true
                var result = executeOnce(foods, language, existingRecipes, key, useJsonSchema, startedAt)
                if (result == null) {
                    // RF-MPC3: el proveedor rechazó `json_schema`; un solo reintento
                    // con `json_object`, con la misma validación tras la respuesta.
                    useJsonSchema = false
                    result = executeOnce(foods, language, existingRecipes, key, useJsonSchema, startedAt)
                }
                result ?: RecipeSuggestionsResult.Failed(AiFailureCause.PROVIDER_ERROR)
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

    // Devuelve `null` solo cuando el proveedor rechaza `json_schema` con un 4xx y
    // toca reintentar una vez con `json_object` (RF-MPC3).
    private fun executeOnce(
        foods: List<SuggestableFood>,
        language: String,
        existingRecipes: List<RecipeAvoidance>,
        key: String,
        useJsonSchema: Boolean,
        startedAt: Long,
    ): RecipeSuggestionsResult? = try {
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $key")
            .post(buildBody(foods, language, existingRecipes, useJsonSchema).toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val elapsed = elapsedMs(startedAt)
            when {
                !response.isSuccessful && useJsonSchema && shouldFallbackToJsonObject(response.code) -> {
                    Log.w(
                        TAG,
                        "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms: " +
                            "json_schema rejected, falling back to json_object",
                    )
                    null
                }

                !response.isSuccessful -> {
                    Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms")
                    RecipeSuggestionsResult.Failed(AiFailureCause.PROVIDER_ERROR)
                }

                else -> {
                    val body = response.body.string()
                    val content = messageContent(body)
                    if (content == null) {
                        Log.w(
                            TAG,
                            "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                "without message content (body ${body.length} chars)",
                        )
                        RecipeSuggestionsResult.Failed(AiFailureCause.INVALID_RESPONSE)
                    } else {
                        val raw = RecipeSuggestionParser.parse(content)
                        if (raw == null) {
                            Log.w(
                                TAG,
                                "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                    "with unparseable content (${content.length} chars)",
                            )
                            RecipeSuggestionsResult.Failed(AiFailureCause.INVALID_RESPONSE)
                        } else {
                            Log.i(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms, ${raw.size} proposals")
                            RecipeSuggestionsResult.Success(SuggestionRules.normalize(raw))
                        }
                    }
                }
            }
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

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun Exception.describe(): String = "${javaClass.simpleName}: ${message.orEmpty()}"

    // Cuerpo de la petición en forma pura (testeable sin red): mensajes `system` y
    // `user` (RF-MPC1) y `response_format` `json_schema` o `json_object` (RF-MPC2/3).
    internal fun buildBody(
        foods: List<SuggestableFood>,
        language: String,
        existingRecipes: List<RecipeAvoidance>,
        useJsonSchema: Boolean,
    ): JSONObject {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", recipeSuggestionSystemPrompt(language, promptSource)),
            )
            .put(
                JSONObject()
                    .put("role", "user")
                    .put("content", recipeSuggestionUserPrompt(foods, language, promptSource, existingRecipes)),
            )
        return JSONObject()
            .put("model", MODEL)
            .put("temperature", 0)
            .put(
                "response_format",
                if (useJsonSchema) {
                    responseFormatJsonSchema(RECIPE_SCHEMA_NAME, recipeJsonSchema())
                } else {
                    responseFormatJsonObject()
                },
            )
            .put("messages", messages)
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
        const val RECIPE_SCHEMA_NAME = "recipe_suggestions"
    }
}

// Instrucciones (rol `system`) del prompt de Sugerencias: rol del modelo, contrato
// compartido, robustez JSON y reglas de contenido; sin datos del usuario (RF-MPC1).
internal fun recipeSuggestionSystemPrompt(language: String, source: PromptSource): String =
    splitSystemUser(source.suggestionPrompt(language)).first

// Datos (rol `user`) del prompt de Sugerencias: alimentos y Recetas existentes,
// delimitados con `<<<`/`>>>` (RF-MPC1). La sección anti-duplicados solo aparece
// cuando hay Recetas que evitar (RF-ASR6). Determinista para una fuente de prompts
// dada, así que se verifica sin red.
internal fun recipeSuggestionUserPrompt(
    foods: List<SuggestableFood>,
    language: String,
    source: PromptSource,
    existingRecipes: List<RecipeAvoidance>,
): String {
    val foodList = foods.joinToString(", ") { food ->
        if (food.quantity != null) "${food.name} (${food.quantity})" else food.name
    }
    val base = splitSystemUser(source.suggestionPrompt(language)).second
        .replace(FOODS_TOKEN, wrapUserData(foodList))
    if (existingRecipes.isEmpty()) return base
    val avoidList = existingRecipes.joinToString("; ") { recipe ->
        "${recipe.title} (${recipe.ingredientNames.joinToString(", ")})"
    }
    val section = source.suggestionAvoidPrompt(language).replace(AVOID_TOKEN, wrapUserData(avoidList))
    return "$base $section"
}

private const val FOODS_TOKEN = "{FOODS}"
private const val AVOID_TOKEN = "{RECIPES}"
