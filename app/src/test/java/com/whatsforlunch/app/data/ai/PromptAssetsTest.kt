package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAssetsTest {

    private val source = TestPromptSource()
    private val languages = listOf("es", "en")
    private val names = listOf(
        "pinche_system",
        "suggestion",
        "suggestion_avoid",
        "pantry_scan",
        "dish_photo",
    )

    @Test
    fun everyPromptExistsForSpanishAndEnglish() {
        names.forEach { name ->
            languages.forEach { language ->
                assertTrue("$name ($language) is empty", source.template(name, language).isNotBlank())
            }
        }
    }

    @Test
    fun renderedPromptsNeverKeepTheContractPlaceholder() {
        names.forEach { name ->
            languages.forEach { language ->
                val rendered = renderPrompt(source.template(name, language))
                assertFalse("$name ($language) keeps the placeholder", rendered.contains(CONTRACT_PLACEHOLDER))
            }
        }
    }

    @Test
    fun suggestionPromptsKeepTheirTokensAndTheSharedContract() {
        assertTrue(source.suggestionPrompt("es").contains("{FOODS}"))
        assertTrue(source.suggestionPrompt("en").contains("{FOODS}"))
        assertTrue(source.suggestionPrompt("es").contains(RECIPE_CONTRACT))
        assertTrue(source.suggestionPrompt("en").contains(RECIPE_CONTRACT))
        assertTrue(source.suggestionAvoidPrompt("es").contains("{RECIPES}"))
        assertTrue(source.suggestionAvoidPrompt("en").contains("{RECIPES}"))
        assertTrue(source.suggestionPrompt("es").contains("cocinero experto"))
        assertTrue(source.suggestionPrompt("en").contains("expert cook"))
        assertTrue(source.suggestionAvoidPrompt("es").contains("No propongas recetas iguales"))
        assertTrue(source.suggestionAvoidPrompt("en").contains("Do not propose recipes"))
    }

    @Test
    fun scanPromptsAreLocalized() {
        assertTrue(source.pantryScanPrompt("es").contains("Analiza la foto"))
        assertTrue(source.pantryScanPrompt("en").contains("Analyze the photo"))
    }

    @Test
    fun dishPhotoPromptsFillDishAndStory() {
        assertEquals(
            "Fotografía gastronómica apetitosa del plato \"Tortilla\". Con cebolla.",
            renderDishPhotoPrompt("Tortilla", "Con cebolla", "es", source),
        )
        assertEquals(
            "Appetizing food photography of the dish \"Omelette\". With onion.",
            renderDishPhotoPrompt("Omelette", "With onion", "en", source),
        )
        assertEquals(
            "Fotografía gastronómica apetitosa del plato \"Tortilla\".",
            renderDishPhotoPrompt("Tortilla", " ", "es", source),
        )
    }
}
