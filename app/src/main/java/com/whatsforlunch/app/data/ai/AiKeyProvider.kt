package com.whatsforlunch.app.data.ai

// Proveedor de la API key de IA del dispositivo. El almacenamiento cifrado real
// llega con la feature configurar-acceso-a-ia (⚠ S05, Q-crear-receta-6); aquí solo
// se consume. Sin key, «Generar con IA» avisa y no bloquea el resto (RF-21).
interface AiKeyProvider {
    fun apiKey(): String?
}

// Implementación provisional: todavía no hay key configurada, así que el formulario
// muestra el aviso de RF-21. configurar-acceso-a-ia la sustituirá por el almacén cifrado.
class UnconfiguredAiKeyProvider : AiKeyProvider {
    override fun apiKey(): String? = null
}
