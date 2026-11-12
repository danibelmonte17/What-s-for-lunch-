package com.whatsforlunch.app.data.ai

// Composicion del endpoint a partir de la base configurable (D9): AI_BASE_URL es la
// base compatible con OpenAI y cada cliente anade su ruta, sin duplicar la barra final.
// Sin base, se usa DevExpert Inference. Tambien se acepta la URL completa del endpoint
// para no duplicar la ruta si se pega tal cual.
object AiEndpoints {

    const val DEFAULT_BASE_URL = "https://inference.devexpert.io/v1"

    fun imageGeneration(baseUrl: String): String = endpoint(baseUrl, IMAGE_GENERATIONS_PATH)

    fun chatCompletions(baseUrl: String): String = endpoint(baseUrl, CHAT_COMPLETIONS_PATH)

    private fun endpoint(baseUrl: String, path: String): String {
        val base = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }.trimEnd('/')
        return if (base.endsWith(path)) base else "$base$path"
    }

    private const val IMAGE_GENERATIONS_PATH = "/images/generations"
    private const val CHAT_COMPLETIONS_PATH = "/chat/completions"
}
