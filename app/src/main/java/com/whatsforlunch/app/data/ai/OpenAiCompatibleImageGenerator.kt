package com.whatsforlunch.app.data.ai

import android.util.Base64
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// Implementación provisional de ImageGenerator sobre una API compatible con OpenAI
// (OpenRouter). Depende de la API key del dispositivo (⚠ S05, Q-crear-receta-6).
// Devuelve MissingApiKey sin key y Failed ante cualquier fallo (RD05).
class OpenAiCompatibleImageGenerator(
    private val client: OkHttpClient,
    private val aiKeyProvider: AiKeyProvider,
    private val endpoint: String = DEFAULT_ENDPOINT,
) : ImageGenerator {

    override suspend fun generate(prompt: String): ImageGenerationResult {
        val key = aiKeyProvider.apiKey() ?: return ImageGenerationResult.MissingApiKey
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $key")
                    .post(buildBody(prompt))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext ImageGenerationResult.Failed
                    val body = response.body?.string() ?: return@withContext ImageGenerationResult.Failed
                    val bytes = parseImage(body) ?: return@withContext ImageGenerationResult.Failed
                    ImageGenerationResult.Success(bytes)
                }
            } catch (_: IOException) {
                ImageGenerationResult.Failed
            }
        }
    }

    private fun buildBody(prompt: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", MODEL)
            put("prompt", prompt)
            put("n", 1)
        }
        return json.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    private fun parseImage(body: String): ByteArray? = runCatching {
        val data = JSONObject(body).optJSONArray("data") ?: return@runCatching null
        val first = data.optJSONObject(0) ?: return@runCatching null
        first.optString("b64_json").takeIf { it.isNotBlank() }?.let { encoded ->
            return@runCatching Base64.decode(encoded, Base64.DEFAULT)
        }
        null
    }.getOrNull()

    private companion object {
        const val DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/images/generations"
        const val MODEL = "openai/dall-e-3"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}
