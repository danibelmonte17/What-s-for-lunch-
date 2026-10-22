package com.whatsforlunch.app.data.ai

// Composicion del endpoint a partir de la base configurable (D9): AI_BASE_URL es la
// base compatible con OpenAI y el generador anade la ruta de imagenes, sin duplicar
// la barra final. Sin base, se usa DevExpert Inference. Tambien se acepta la URL
// completa del endpoint para no duplicar /images/generations si se pega tal cual.
object AiEndpoints {

    const val DEFAULT_BASE_URL = "https://inference.devexpert.io/v1"

    fun imageGeneration(baseUrl: String): String {
        val base = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }.trimEnd('/')
        return if (base.endsWith(IMAGE_GENERATIONS_PATH)) base else "$base$IMAGE_GENERATIONS_PATH"
    }

    private const val IMAGE_GENERATIONS_PATH = "/images/generations"
}
