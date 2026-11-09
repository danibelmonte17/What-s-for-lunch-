package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAssetsTest {

    private val source = TestPromptSource()
    private val languages = listOf("es", "en")
    private val names = listOf(
        "pinche_system",
        "pinche_profile",
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

    // `crear-mi-perfil` (D6): el bloque de perfil rellena tipo de alimentación y
    // restricciones, con «No tengo ninguna» como «ninguna»/«none» y el texto libre
    // del usuario literal en ambos idiomas.
    @Test
    fun profilePromptFillsDietAndRestrictions() {
        val profile = UserProfile(
            allergies = listOf("Frutos secos"),
            intolerances = listOf("Diabetes"),
            dietType = DietType.VEGAN,
        )
        val es = renderProfilePrompt(source.template("pinche_profile", "es"), profile, "es")
        assertTrue(es.contains("Tipo de alimentación: Vegana"))
        assertTrue(es.contains("Alergias: Frutos secos"))
        assertTrue(es.contains("Intolerancias o enfermedades que limitan la dieta: Diabetes"))
        assertFalse(es.contains(DIET_PLACEHOLDER))
        assertFalse(es.contains(ALLERGIES_PLACEHOLDER))
        assertFalse(es.contains(INTOLERANCES_PLACEHOLDER))

        val en = renderProfilePrompt(source.template("pinche_profile", "en"), profile, "en")
        assertTrue(en.contains("Diet type: Vegan"))
        // El texto libre del usuario no se traduce.
        assertTrue(en.contains("Allergies: Frutos secos"))
        assertTrue(en.contains("Intolerances or medical conditions that limit the diet: Diabetes"))

        val noneProfile = UserProfile(allergiesNone = true, intolerancesNone = true, dietType = DietType.OMNIVORE)
        val noneEs = renderProfilePrompt(source.template("pinche_profile", "es"), noneProfile, "es")
        assertTrue(noneEs.contains("Tipo de alimentación: Omnívora"))
        assertTrue(noneEs.contains("Alergias: ninguna"))
        assertTrue(noneEs.contains("Intolerancias o enfermedades que limitan la dieta: ninguna"))
        val noneEn = renderProfilePrompt(source.template("pinche_profile", "en"), noneProfile, "en")
        assertTrue(noneEn.contains("Allergies: none"))
        assertTrue(noneEn.contains("Diet type: Omnivore"))
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
