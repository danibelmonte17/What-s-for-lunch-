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

    // RF-MPC8: el prompt del Pinche queda en secciones (rol y tono, herramientas,
    // cuándo proponer receta, formato, límites y seguridad culinaria) en ambos idiomas.
    @Test
    fun systemPromptsAreOrganizedInSectionsInTheTwoLanguages() {
        val es = source.pincheSystemPrompt("es")
        listOf(
            "Rol y tono.",
            "Herramientas.",
            "Cuándo proponer receta.",
            "Formato de la propuesta.",
            "Seguridad culinaria.",
        ).forEach { section -> assertTrue("falta «$section» en es", es.contains(section)) }
        assertTrue(es.contains("pregúntalo en vez de asumirlo"))
        assertTrue(es.contains("cocción segura de carne, pescado y huevo"))
        assertTrue(es.contains("no des consejos médicos"))
        assertTrue(es.contains("10 ingredientes y 12 pasos"))

        val en = source.pincheSystemPrompt("en")
        listOf(
            "Role and tone.",
            "Tools.",
            "When to propose a recipe.",
            "Recipe format.",
            "Food safety.",
        ).forEach { section -> assertTrue("missing «$section» in en", en.contains(section)) }
        assertTrue(en.contains("ask for it instead of assuming it"))
        assertTrue(en.contains("safe cooking of meat, fish and egg"))
        assertTrue(en.contains("do not give medical advice"))
        assertTrue(en.contains("10 ingredients and 12 steps"))
    }

    // RF-MPC8: el bloque de perfil añade la corrección de datos guardados y que no
    // se vuelva a preguntar por lo ya declarado.
    @Test
    fun profilePromptAddsCorrectionAndNoRepeatedQuestionsRules() {
        val es = source.pincheProfilePrompt("es")
        assertTrue(es.contains("No preguntes por partes ya declaradas"))
        assertTrue(es.contains("vuelve a llamar a save_profile con la lista completa"))
        assertTrue(es.contains("dato del usuario"))

        val en = source.pincheProfilePrompt("en")
        assertTrue(en.contains("Do not ask about parts already declared"))
        assertTrue(en.contains("call save_profile again with the full list"))
        assertTrue(en.contains("user data"))
    }
}
