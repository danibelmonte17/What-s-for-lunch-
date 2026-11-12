package com.whatsforlunch.app.data.ai

import android.util.Log
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.domain.profile.ProfileUpdateOutcome
import com.whatsforlunch.app.domain.profile.SavedProfilePart
import com.whatsforlunch.app.domain.profile.UserProfileRules
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject

// Cliente del Pinche sobre una API compatible con OpenAI (DevExpert Inference).
// Hace una tanda de /chat/completions con el alias estable «chat», tools de
// `get_pantry`/`get_my_recipes` (solo lectura) y `save_profile` (escritura acotada
// al perfil, RD27), con `tool_choice: "auto"` y sin `response_format` (D5).
// Ejecuta en el dispositivo las `tool_calls` que devuelva el modelo y le responde
// con mensajes `role = tool`, con un máximo de 5 llamadas HTTP por turno
// (`rellenar-perfil-con-el-pinche`, D3: la entrevista de perfil necesita más de
// una ronda de tools). Sin key se informa por separado; cualquier fallo es
// `Failed` (RF-PCH9, RD17).
class OpenAiCompatiblePincheChatClient(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    private val pantryRepository: PantryRepository,
    private val recipeRepository: RecipeRepository,
    private val userProfileRepository: UserProfileRepository,
    private val promptSource: PromptSource,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : PincheChatClient {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun reply(
        turns: List<PincheTurn>,
        language: String,
        profile: UserProfile,
        onContent: (String) -> Unit,
    ): PincheReplyResult {
        if (turns.isEmpty()) return PincheReplyResult.Failed
        val key = aiKeyProvider.apiKey() ?: return PincheReplyResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            runCatching { converse(turns, language, profile, key, onContent) }
                .getOrElse { exception ->
                    if (exception is CancellationException) throw exception
                    // La lectura SSE se corta al cancelar el coroutine (`Call.cancel`);
                    // en ese caso hay que propagar la cancelación, no reportar fallo.
                    if (!isActive) throw CancellationException("Pinche turn cancelled")
                    Log.w(TAG, "Turn failed: ${exception.describe()}")
                    PincheReplyResult.Failed
                }
        }
    }

    private suspend fun converse(
        turns: List<PincheTurn>,
        language: String,
        profile: UserProfile,
        key: String,
        onContent: (String) -> Unit,
    ): PincheReplyResult {
        val startedAt = System.nanoTime()
        // D6 (`crear-mi-perfil`): el perfil viaja como bloque del system prompt,
        // localizado y con las secciones marcadas «No tengo ninguna» como
        // «ninguna»; el mismo bloque lleva las reglas de entrevista para cuando
        // hay partes pendientes (`rellenar-perfil-con-el-pinche`, D6). Sin tool
        // `get_profile` (non-goal).
        val systemPrompt = promptSource.pincheSystemPrompt(language) + "\n\n" +
            renderProfilePrompt(promptSource.pincheProfilePrompt(language), profile, language)
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt),
            )
        turns.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", if (turn.author == PincheAuthor.USER) "user" else "assistant")
                    .put("content", turn.text),
            )
        }
        val tools = PincheChatProtocol.toolsJson()
        val state = PincheToolTurnState()
        var httpCalls = 0
        var executedTools = 0

        while (true) {
            httpCalls++
            val callStartedAt = System.nanoTime()
            // D14: el texto visible se reinicia al empezar cada ronda (hoy solo
            // se pinta el contenido de la última).
            onContent("")
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $key")
                .post(buildBody(messages, tools))
                .build()
            val call = client.newCall(request)
            // D9: si el coroutine se cancela, se cancela también la `Call` para no
            // dejar una lectura bloqueante colgada del scope de la UI.
            val cancellation = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
            val message = try {
                call.execute().use { response ->
                    val elapsed = elapsedMs(callStartedAt)
                    if (!response.isSuccessful) {
                        Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms")
                        return failed("http ${response.code}", startedAt)
                    }
                    // En OkHttp 5 `Response.body` es no-nulable: el chequeo de
                    // cuerpo nulo era código muerto (un cuerpo vacío lo captura
                    // el `runCatching` de `reply`).
                    val body = response.body
                    // D9/D12: SSE si el proveedor lo devuelve; si no, el cuerpo
                    // completo se procesa como hasta hoy (sin emitir fragmentos).
                    val message = if (response.isEventStream()) {
                        readStream(body.source(), onContent)
                    } else {
                        val choices = JSONObject(body.string()).optJSONArray("choices")
                        choices?.optJSONObject(0)?.optJSONObject("message")
                    }
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
            } finally {
                cancellation?.dispose()
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
                return PincheReplyResult.Success(reply, state.savedProfileParts())
            }

            // D3 (`rellenar-perfil-con-el-pinche`): con 5 llamadas se supera el
            // presupuesto del turno.
            if (httpCalls >= MAX_HTTP_CALLS) {
                return failed("tool budget exceeded", startedAt)
            }

            messages.put(message)
            toolCalls.forEach { call ->
                executedTools++
                val result = try {
                    when (call.name) {
                        PANTRY_FUNCTION -> {
                            val snapshot = state.pantrySnapshot
                                ?: pantryRepository.observePantryItems().first()
                                    .also { state.pantrySnapshot = it }
                            PincheChatProtocol.pantryToolResult(snapshot)
                        }

                        RECIPES_FUNCTION -> {
                            val snapshot = state.recipesSnapshot
                                ?: recipeRepository.observeRecipes().first()
                                    .also { state.recipesSnapshot = it }
                            PincheChatProtocol.recipesToolResult(snapshot)
                        }

                        SAVE_PROFILE_FUNCTION -> executeSaveProfile(call.arguments, state)

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

    /**
     * RD27/D2: ejecuta `save_profile` sobre el perfil vigente del turno (leído una
     * sola vez y con caché actualizada tras cada guardado), lo persiste vía
     * `UserProfileRules` y devuelve el resultado JSON para el modelo. Un resultado
     * inválido viaja como `{"error": …}` **sin persistir nada**; una excepción al
     * persistir la captura el bucle de tools y se responde `{"error": "unavailable"}`.
     */
    internal suspend fun executeSaveProfile(
        arguments: String,
        state: PincheToolTurnState,
    ): String {
        val update = PincheChatProtocol.parseSaveProfileArgs(arguments)
            ?: return PincheChatProtocol.saveProfileToolError(
                PincheChatProtocol.saveProfileArgsError(arguments),
            )
        val current = state.profileSnapshot
            ?: userProfileRepository.observeProfile().first().also { state.profileSnapshot = it }
        return when (val outcome = UserProfileRules.applyUpdate(current, update)) {
            is ProfileUpdateOutcome.Invalid ->
                PincheChatProtocol.saveProfileToolError(outcome.code)

            is ProfileUpdateOutcome.Saved -> {
                userProfileRepository.save(outcome.updated)
                state.profileSnapshot = outcome.updated
                outcome.savedParts.forEach { state.savedParts[it.part] = it }
                PincheChatProtocol.saveProfileToolResult(
                    outcome.savedParts,
                    UserProfileRules.pendingParts(outcome.updated),
                )
            }
        }
    }

    private fun buildBody(messages: JSONArray, tools: JSONArray): okhttp3.RequestBody {
        val json = JSONObject()
            .put("model", MODEL)
            .put("temperature", 0)
            // D9 (`rellenar-perfil-con-el-pinche`): la respuesta se pide en streaming
            // y se lee como SSE; si el proveedor no lo soporta, la app cae al cuerpo
            // completo (D12) sin cambiar el resultado.
            .put("stream", true)
            .put("messages", messages)
            .put("tools", tools)
            .put("tool_choice", "auto")
        return json.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    /**
     * D9: lee el cuerpo SSE línea a línea, emite el texto visible de la ronda a
     * medida que llega y devuelve el `message` reconstruido por `PincheSseParser`
     * (o `null` si no llegó nada utilizable: sin `data:` o JSON ilegible).
     */
    private fun readStream(source: BufferedSource, onContent: (String) -> Unit): JSONObject? {
        val parser = PincheSseParser()
        var lastEmitted: String? = null
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith(DATA_PREFIX)) continue
            parser.feed(line.substring(DATA_PREFIX.length))
            val visible = PincheChatProtocol.streamingText(parser.accumulatedText)
            if (visible != lastEmitted) {
                lastEmitted = visible
                onContent(visible)
            }
            if (parser.finished || parser.invalid) break
        }
        return parser.buildMessage()
    }

    private fun Response.isEventStream(): Boolean =
        header("Content-Type").orEmpty().contains(EVENT_STREAM_MEDIA_TYPE)

    private fun messageContent(message: JSONObject): String? = when (val content = message.opt("content")) {
        null -> null
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
        // D9: cuerpo SSE de `/chat/completions` con `stream: true`.
        const val EVENT_STREAM_MEDIA_TYPE = "text/event-stream"
        const val DATA_PREFIX = "data:"
        // D3 (`rellenar-perfil-con-el-pinche`): 3 → 5 llamadas HTTP por turno.
        const val MAX_HTTP_CALLS = 5
        const val PANTRY_FUNCTION = "get_pantry"
        const val RECIPES_FUNCTION = "get_my_recipes"
        const val SAVE_PROFILE_FUNCTION = "save_profile"
        val UNKNOWN_FUNCTION_RESULT = JSONObject().put("error", "unknown_function").toString()
        val UNAVAILABLE_FUNCTION_RESULT = JSONObject().put("error", "unavailable").toString()
    }
}

// Estado de las tools de un turno (cachés por turno, patrón `pantrySnapshot`) con
// las partes del perfil guardadas, acumuladas para `profileSaved` (RD27/D2).
internal class PincheToolTurnState {
    var pantrySnapshot: List<PantryItem>? = null
    var recipesSnapshot: List<Recipe>? = null
    var profileSnapshot: UserProfile? = null
    val savedParts = LinkedHashMap<PendingProfilePart, SavedProfilePart>()

    /** Partes escritas en el turno, en el orden en que las pide el Pinche (`null` = ninguna). */
    fun savedProfileParts(): List<SavedProfilePart>? =
        PendingProfilePart.entries.mapNotNull { savedParts[it] }.takeIf { it.isNotEmpty() }
}
