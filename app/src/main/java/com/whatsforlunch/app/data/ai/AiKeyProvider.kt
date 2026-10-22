package com.whatsforlunch.app.data.ai

// Proveedor de la API key de IA. La key puede venir del build (AI_API_KEY en
// local.properties, con prioridad, D7-D8) o del almacén cifrado de Ajustes
// (Android Keystore, D2); aquí solo se consume (⚠ S05). Sin key, «Generar con IA»
// avisa y no bloquea el resto (RF-21).
interface AiKeyProvider {
    fun apiKey(): String?
}
