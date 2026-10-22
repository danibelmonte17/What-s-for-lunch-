package com.whatsforlunch.app.data.ai

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
    baseUrl: String = AiEndpoints.DEFAULT_BASE_URL,
) : ScanImageAnalyzer {

    private val endpoint = AiEndpoints.chatCompletions(baseUrl)

    override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult {
        if (image.isEmpty()) return ScanAnalysisResult.Failed
        val key = aiKeyProvider.apiKey() ?: return ScanAnalysisResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $key")
                    .post(buildBody(image, language))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext ScanAnalysisResult.Failed
                    val body = response.body?.string() ?: return@withContext ScanAnalysisResult.Failed
                    val content = messageContent(body) ?: return@withContext ScanAnalysisResult.Failed
                    val foods = ScanAnalysisParser.parse(content) ?: return@withContext ScanAnalysisResult.Failed
                    ScanAnalysisResult.Success(foods)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: IOException) {
                ScanAnalysisResult.Failed
            } catch (_: Exception) {
                ScanAnalysisResult.Failed
            }
        }
    }

    private fun buildBody(image: ByteArray, language: String): okhttp3.RequestBody {
        val encoded = Base64.getEncoder().encodeToString(image)
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt(language)))
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

    private fun prompt(language: String): String =
        if (language.lowercase().startsWith("en")) ENGLISH_PROMPT else SPANISH_PROMPT

    private companion object {
        // Stable DevExpert Inference alias for /chat/completions.
        const val MODEL = "chat"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        const val SPANISH_PROMPT =
            "Analiza la foto y enumera los alimentos visibles. Responde EXCLUSIVAMENTE con un único " +
                "objeto JSON, sin markdown ni explicaciones, con la forma {\"nombre del alimento\": cantidad}. " +
                "Usa nombres de alimento en español, en singular y sin marcas, y cantidades enteras entre 1 y 99. " +
                "Si no reconoces ningún alimento, responde {}."

        const val ENGLISH_PROMPT =
            "Analyze the photo and list the visible foods. Reply ONLY with a single JSON object, " +
                "without markdown or explanations, using the shape {\"food name\": quantity}. " +
                "Use English food names, singular and without brand names, and integer quantities between 1 and 99. " +
                "If you recognize no food, reply {}."
    }
}
