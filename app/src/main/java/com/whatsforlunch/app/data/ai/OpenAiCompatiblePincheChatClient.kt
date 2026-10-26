package com.whatsforlunch.app.data.ai

import android.util.Log
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Cliente del Pinche sobre una API compatible con OpenAI (DevExpert Inference).
// Hace una tanda de /chat/completions con el alias estable «chat», tools de solo
// lectura y `tool_choice: "auto"`, sin `response_format` (D5). Ejecuta en el
// dispositivo las `tool_calls` que devuelva el modelo y le responde con mensajes
// `role = tool`, con un máximo de 3 llamadas HTTP por turno (D6). Sin key se informa
// por separado; cualquier fallo es `Failed` (RF-PCH9, RD17).
class OpenAiCompatiblePincheChatClient(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    private val pantryRepository: PantryRepository,
    private val recipeRepository: RecipeRepository,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : PincheChatClient {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun reply(turns: List<PincheTurn>, language: String): PincheReplyResult {
        if (turns.isEmpty()) return PincheReplyResult.Failed
        val key = aiKeyProvider.apiKey() ?: return PincheReplyResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            runCatching { converse(turns, language, key) }
                .getOrElse { exception ->
                    if (exception is CancellationException) throw exception
                    Log.w(TAG, "Turn failed: ${exception.describe()}")
                    PincheReplyResult.Failed
                }
        }
    }

    private suspend fun converse(
        turns: List<PincheTurn>,
        language: String,
        key: String,
    ): PincheReplyResult {
        val startedAt = System.nanoTime()
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", PincheChatProtocol.systemPrompt(language)))
        turns.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", if (turn.author == PincheAuthor.USER) "user" else "assistant")
                    .put("content", turn.text),
            )
        }
        val tools = PincheChatProtocol.toolsJson()
        var pantrySnapshot: List<PantryItem>? = null
        var recipesSnapshot: List<Recipe>? = null
        var httpCalls = 0
        var executedTools = 0

        while (true) {
            httpCalls++
            val callStartedAt = System.nanoTime()
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $key")
                .post(buildBody(messages, tools))
                .build()
            val message = client.newCall(request).execute().use { response ->
                val elapsed = elapsedMs(callStartedAt)
                if (!response.isSuccessful) {
                    Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms")
                    return failed("http ${response.code}", startedAt)
                }
                val body = response.body?.string()
                if (body == null) {
                    Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms without body")
                    return failed("empty body", startedAt)
                }
                val choices = JSONObject(body).optJSONArray("choices")
                val message = choices?.optJSONObject(0)?.optJSONObject("message")
                if (message == null) {
                    Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms without message")
                    return failed("missing message", startedAt)
                }
                Log.i(
                    TAG,
                    "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms, " +
                        "$httpCalls calls, $executedTools tools",
                )
                message
            }

            val toolCalls = PincheChatProtocol.toolCalls(message)
            if (toolCalls.isEmpty()) {
                val content = messageContent(message)
                if (content == null) return failed("missing content", startedAt)
                val reply = PincheChatProtocol.parseReply(content)
                if (reply.text.isBlank() && reply.proposal == null) {
                    return failed("empty reply", startedAt)
                }
                Log.i(TAG, "Turn finished in ${elapsedMs(startedAt)}ms with $httpCalls HTTP calls")
                return PincheReplyResult.Success(reply)
            }

            // D6: la tercera respuesta con tool_calls supera el presupuesto.
            if (httpCalls >= MAX_HTTP_CALLS) {
                return failed("tool budget exceeded", startedAt)
            }

            messages.put(message)
            toolCalls.forEach { call ->
                executedTools++
                val result = try {
                    when (call.name) {
                        PANTRY_FUNCTION -> {
                            val snapshot = pantrySnapshot
                                ?: pantryRepository.observePantryItems().first().also { pantrySnapshot = it }
                            PincheChatProtocol.pantryToolResult(snapshot)
                        }

                        RECIPES_FUNCTION -> {
                            val snapshot = recipesSnapshot
                                ?: recipeRepository.observeRecipes().first().also { recipesSnapshot = it }
                            PincheChatProtocol.recipesToolResult(snapshot)
                        }

                        else -> UNKNOWN_FUNCTION_RESULT
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    UNAVAILABLE_FUNCTION_RESULT
                }
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", result),
                )
            }
        }
    }

    private fun buildBody(messages: JSONArray, tools: JSONArray): okhttp3.RequestBody {
        val json = JSONObject()
            .put("model", MODEL)
            .put("temperature", 0)
            .put("messages", messages)
            .put("tools", tools)
            .put("tool_choice", "auto")
        return json.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    private fun messageContent(message: JSONObject): String? = when (val content = message.opt("content")) {
        is String -> content
        is JSONArray -> (0 until content.length())
            .mapNotNull { index -> content.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() } }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        else -> null
    }

    private fun failed(reason: String, startedAt: Long): PincheReplyResult {
        Log.w(TAG, "Turn failed after ${elapsedMs(startedAt)}ms: $reason")
        return PincheReplyResult.Failed
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun Throwable.describe(): String = "${javaClass.simpleName}: ${message.orEmpty()}"

    private companion object {
        const val TAG = "PincheChat"
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val MAX_HTTP_CALLS = 3
        const val PANTRY_FUNCTION = "get_pantry"
        const val RECIPES_FUNCTION = "get_my_recipes"
        val UNKNOWN_FUNCTION_RESULT = JSONObject().put("error", "unknown_function").toString()
        val UNAVAILABLE_FUNCTION_RESULT = JSONObject().put("error", "unavailable").toString()
    }
}
