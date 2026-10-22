package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

// D9: AI_BASE_URL es una base compatible con OpenAI; el endpoint de imagenes se
// compone sin duplicar la barra final y sin base se usa DevExpert Inference.
class AiEndpointsTest {

    @Test
    fun blankBaseUsesDevExpertDefault() {
        assertEquals(
            "https://inference.devexpert.io/v1/images/generations",
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

    @Test
    fun fullEndpointIsNotDuplicated() {
        assertEquals(
            "https://inference.devexpert.io/v1/images/generations",
            AiEndpoints.imageGeneration("https://inference.devexpert.io/v1/images/generations"),
        )
    }

    @Test
    fun blankBaseBuildsDefaultChatEndpoint() {
        assertEquals(
            "https://inference.devexpert.io/v1/chat/completions",
            AiEndpoints.chatCompletions(""),
        )
    }

    @Test
    fun customBaseBuildsChatEndpointWithoutDuplicatingSlashOrPath() {
        assertEquals(
            "https://proxy.local/v1/chat/completions",
            AiEndpoints.chatCompletions("https://proxy.local/v1/"),
        )
        assertEquals(
            "https://proxy.local/v1/chat/completions",
            AiEndpoints.chatCompletions("https://proxy.local/v1/chat/completions"),
        )
    }
}
