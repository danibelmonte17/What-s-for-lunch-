package com.whatsforlunch.app.data.ai

import android.util.Log
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Multimodal vision client over an OpenAI-compatible API (DevExpert Inference).
// It sends one photo per call to /chat/completions with the stable «chat» alias (D4),
// using the build key with priority over the encrypted Settings key (⚠ S05, D7-D11).
// The analysis rules travel as a `system` message and the photo as `user` data
// delimited with `<<<`/`>>>` (RF-MPC1); `response_format` carries `json_schema`
// (RF-MPC2) with an automatic single retry as `json_object` when the provider
// rejects it (RF-MPC3). Missing key and any network/provider/parse failure are
// reported separately (RD05).
class OpenAiCompatibleScanImageAnalyzer(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    private val promptSource: PromptSource,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : ScanImageAnalyzer {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult {
        if (image.isEmpty()) return ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)
        val key = aiKeyProvider.apiKey() ?: return ScanAnalysisResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            val startedAt = System.nanoTime()
            try {
                var useJsonSchema = true
                var result = executeOnce(image, language, key, useJsonSchema, startedAt)
                if (result == null) {
                    // RF-MPC3: el proveedor rechazó `json_schema`; un solo reintento
                    // con `json_object`, con la misma validación tras la respuesta.
                    useJsonSchema = false
                    result = executeOnce(image, language, key, useJsonSchema, startedAt)
                }
                result ?: ScanAnalysisResult.Failed(AiFailureCause.PROVIDER_ERROR)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                ScanAnalysisResult.Failed(AiFailure.fromException(exception))
            } catch (exception: Exception) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                ScanAnalysisResult.Failed(AiFailure.fromException(exception))
            }
        }
    }

    // Devuelve `null` solo cuando el proveedor rechaza `json_schema` con un 4xx y
    // toca reintentar una vez con `json_object` (RF-MPC3).
    private fun executeOnce(
        image: ByteArray,
        language: String,
        key: String,
        useJsonSchema: Boolean,
        startedAt: Long,
    ): ScanAnalysisResult? = try {
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $key")
            .post(buildBody(image, language, useJsonSchema).toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
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
                    ScanAnalysisResult.Failed(AiFailureCause.PROVIDER_ERROR)
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
                        ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)
                    } else {
                        val foods = ScanAnalysisParser.parse(content)
                        if (foods == null) {
                            Log.w(
                                TAG,
                                "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                    "with unparseable content (${content.length} chars)",
                            )
                            ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)
                        } else {
                            Log.i(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms, ${foods.size} foods")
                            ScanAnalysisResult.Success(foods)
                        }
                    }
                }
            }
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IOException) {
        Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
        ScanAnalysisResult.Failed(AiFailure.fromException(exception))
    } catch (exception: Exception) {
        Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
        ScanAnalysisResult.Failed(AiFailure.fromException(exception))
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun Exception.describe(): String = "${javaClass.simpleName}: ${message.orEmpty()}"

    // Cuerpo de la petición en forma pura (testeable sin red): mensajes `system`
    // (instrucciones) y `user` (foto delimitada, RF-MPC1) y `response_format`
    // `json_schema` o `json_object` (RF-MPC2/3). La imagen solo viaja en `user`,
    // límite de las APIs multimodales.
    internal fun buildBody(image: ByteArray, language: String, useJsonSchema: Boolean): JSONObject {
        val encoded = Base64.getEncoder().encodeToString(image)
        val (system, user) = splitSystemUser(promptSource.pantryScanPrompt(language))
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", user))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$encoded")),
            )
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", content))
        return JSONObject()
            .put("model", MODEL)
            .put("temperature", 0)
            .put(
                "response_format",
                if (useJsonSchema) {
                    responseFormatJsonSchema(SCAN_SCHEMA_NAME, scanJsonSchema())
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
        const val TAG = "ScanImageAnalyzer"
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val SCAN_SCHEMA_NAME = "scan_analysis"
    }
}
