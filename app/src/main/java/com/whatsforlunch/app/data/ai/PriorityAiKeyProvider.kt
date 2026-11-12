package com.whatsforlunch.app.data.ai

// Prioridad de la key (D8): la aportada por local.properties en tiempo de compilacion
// gana si no esta vacia; si no, se usa el almacen cifrado de Ajustes. La key de build
// nunca se persiste; solo se lee de BuildConfig.
class PriorityAiKeyProvider(
    private val buildKey: String,
    private val fallback: AiKeyProvider,
) : AiKeyProvider {

    override fun apiKey(): String? =
        buildKey.trim().takeIf { it.isNotEmpty() } ?: fallback.apiKey()
}
