package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinchePromptsTest {

    private val source = TestPromptSource()

    @Test
    fun spanishPromptIsRenderedWithTheSharedContractAndCurrentInstructions() {
        val prompt = source.pincheSystemPrompt("es")

        assertTrue(prompt.contains("Eres el Pinche"))
        assertTrue(prompt.contains("get_pantry"))
        assertTrue(prompt.contains("Almuerzo"))
        assertTrue(prompt.contains("sugerencia"))
        assertTrue(prompt.contains("sin nombrar ingredientes ni pasos"))
        assertTrue(prompt.contains(RECIPE_CONTRACT))
        assertFalse(prompt.contains(CONTRACT_PLACEHOLDER))
    }

    @Test
    fun englishPromptIsRenderedWithTheSharedContractAndCurrentInstructions() {
        val prompt = source.pincheSystemPrompt("en")

        assertTrue(prompt.contains("You are the Pinche"))
        assertTrue(prompt.contains("get_my_recipes"))
        assertTrue(prompt.contains("Lunch"))
        assertTrue(prompt.contains("suggestion"))
        assertTrue(prompt.contains("without naming ingredients or steps"))
        assertTrue(prompt.contains(RECIPE_CONTRACT))
        assertFalse(prompt.contains(CONTRACT_PLACEHOLDER))
    }

    @Test
    fun unknownLanguageFallsBackToSpanish() {
        assertEquals(promptPath("pinche_system", "es"), promptPath("pinche_system", "fr"))
        assertEquals(promptPath("pinche_system", "es"), promptPath("pinche_system", "es-ES"))
        assertEquals(source.pincheSystemPrompt("es"), source.pincheSystemPrompt("fr"))
    }
}
