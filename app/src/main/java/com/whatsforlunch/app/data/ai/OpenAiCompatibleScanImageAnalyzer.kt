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
// Missing key and any network/provider/parse failure are reported separately (RD05).
class OpenAiCompatibleScanImageAnalyzer(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    private val promptSource: PromptSource,
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : ScanImageAnalyzer {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult {
        if (image.isEmpty()) return ScanAnalysisResult.Failed
        val key = aiKeyProvider.apiKey() ?: return ScanAnalysisResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            val startedAt = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $key")
                    .post(buildBody(image, language))
                    .build()
                client.newCall(request).execute().use { response ->
                    val elapsed = elapsedMs(startedAt)
                    if (!response.isSuccessful) {
                        Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms")
                        return@withContext ScanAnalysisResult.Failed
                    }
                    val body = response.body?.string()
                    if (body == null) {
                        Log.w(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms without body")
                        return@withContext ScanAnalysisResult.Failed
                    }
                    val content = messageContent(body)
                    if (content == null) {
                        Log.w(
                            TAG,
                            "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                "without message content (body ${body.length} chars)",
                        )
                        return@withContext ScanAnalysisResult.Failed
                    }
                    val foods = ScanAnalysisParser.parse(content)
                    if (foods == null) {
                        Log.w(
                            TAG,
                            "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms " +
                                "with unparseable content (${content.length} chars)",
                        )
                        return@withContext ScanAnalysisResult.Failed
                    }
                    Log.i(TAG, "POST $endpoint -> HTTP ${response.code} in ${elapsed}ms, ${foods.size} foods")
                    ScanAnalysisResult.Success(foods)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                ScanAnalysisResult.Failed
            } catch (exception: Exception) {
                Log.w(TAG, "POST $endpoint failed after ${elapsedMs(startedAt)}ms: ${exception.describe()}")
                ScanAnalysisResult.Failed
            }
        }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun Exception.describe(): String = "${javaClass.simpleName}: ${message.orEmpty()}"

    private fun buildBody(image: ByteArray, language: String): okhttp3.RequestBody {
        val encoded = Base64.getEncoder().encodeToString(image)
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", promptSource.pantryScanPrompt(language)))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$encoded")),
            )
        val message = JSONObject().put("role", "user").put("content", content)
        val json = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0)
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

    private companion object {
        const val TAG = "ScanImageAnalyzer"
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}
