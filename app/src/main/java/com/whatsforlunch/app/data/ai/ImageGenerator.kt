package com.whatsforlunch.app.data.ai

// Resultado de generar una imagen con IA: distingue el éxito de la falta de key
// (RF-21) y del fallo de generación (RF-22, RD05).
sealed interface ImageGenerationResult {
    class Success(val bytes: ByteArray) : ImageGenerationResult
    data object MissingApiKey : ImageGenerationResult
    data object Failed : ImageGenerationResult
}

// Puerto de generación de imágenes con IA. El prompt se construye con la información
// del plato (título e historia) en la capa de presentación (Q-crear-receta-7).
interface ImageGenerator {
    suspend fun generate(prompt: String): ImageGenerationResult
}
