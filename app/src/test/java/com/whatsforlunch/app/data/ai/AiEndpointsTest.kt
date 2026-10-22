package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

// D9: AI_BASE_URL es una base compatible con OpenAI; el endpoint de imagenes se
// compone sin duplicar la barra final y sin base se mantiene OpenRouter.
class AiEndpointsTest {

    @Test
    fun blankBaseUsesOpenRouterDefault() {
        assertEquals(
            "https://openrouter.ai/api/v1/images/generations",
            AiEndpoints.imageGeneration(""),
        )
    }

    @Test
    fun customBaseBuildsImageEndpoint() {
        assertEquals(
            "https://proxy.local/v1/images/generations",
            AiEndpoints.imageGeneration("https://proxy.local/v1"),
        )
    }

    @Test
    fun trailingSlashIsNormalized() {
        assertEquals(
            "https://proxy.local/v1/images/generations",
            AiEndpoints.imageGeneration("https://proxy.local/v1/"),
        )
    }
}
