package com.whatsforlunch.app.data.ai

// Composicion del endpoint a partir de la base configurable (D9): AI_BASE_URL es la
// base compatible con OpenAI y el generador anade la ruta de imagenes, sin duplicar
// la barra final. Sin base, se usa OpenRouter como hasta ahora.
object AiEndpoints {

    const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"

    fun imageGeneration(baseUrl: String): String {
        val base = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }
        return "${base.trimEnd('/')}/images/generations"
    }
}
